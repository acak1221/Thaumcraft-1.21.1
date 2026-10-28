/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami.tool;

import dev.architectury.event.EventResult;
import dev.architectury.utils.value.IntValue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumic.tinkerer.common.item.kami.KamiComponents;
import thaumic.tinkerer.common.item.kami.KamiHooks;

/**
 * The shared logic of the awakened Ichorium tools: the mode, the "materials" each tool is effective on, and the
 * breaking of the extra blocks of the square, line and column modes.
 *
 * <p>1.7.10 {@code Material} lists become predicates over block tags (see {@link #MATERIALS_PICK} and friends); the mode
 * was the damage value of the tool and is the {@link KamiComponents#TOOL_MODE} component now. The extra blocks are broken
 * with {@code ServerPlayerGameMode.destroyBlock}, which runs the normal break of the player (drops, experience, protection
 * and other mods' break events) for every one of them; a flag keeps those inner breaks from starting the mode again.</p>
 */
public final class ToolHandler {
    /**
     * {@code materialsShovel}: grass, ground, sand, snow, crafted snow, clay. Everything a shovel is effective on in 1.21.1
     * is that family (dirt, grass, sand, gravel, clay, snow, soul sand, mud...).
     */
    public static final Predicate<BlockState> MATERIALS_SHOVEL = state -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);

    /**
     * {@code materialsPick}: rock, iron, ice, glass, piston, anvil and (the 1.7.10 list is the concatenation) every shovel
     * material. Bedrock was rock; it is in 1.21.1 the only block in the list that no tag says a pickaxe mines.
     */
    public static final Predicate<BlockState> MATERIALS_PICK = state -> state.is(BlockTags.MINEABLE_WITH_PICKAXE)
            || state.is(Blocks.BEDROCK) || state.getSoundType() == SoundType.GLASS || MATERIALS_SHOVEL.test(state);

    /** {@code materialsAxe}: coral, leaves, plants, wood. */
    public static final Predicate<BlockState> MATERIALS_AXE = state -> state.is(BlockTags.MINEABLE_WITH_AXE)
            || state.is(BlockTags.LEAVES) || state.getBlock() instanceof BushBlock
            || state.getBlock() instanceof SugarCaneBlock || state.getBlock() instanceof CocoaBlock;

    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ToolHandler() {}

    // ---- mode -----------------------------------------------------------------------------------------------

    public static int getMode(ItemStack tool) {
        return tool.getOrDefault(KamiComponents.TOOL_MODE.get(), 0);
    }

    public static int getNextMode(int mode) {
        return mode == 2 ? 0 : mode + 1;
    }

    public static void changeMode(ItemStack tool) {
        tool.set(KamiComponents.TOOL_MODE.get(), getNextMode(getMode(tool)));
    }

    public static Component getToolModeStr(IAdvancedTool tool, ItemStack stack) {
        return Component.translatable("ttmisc.mode." + tool.getType() + "." + getMode(stack));
    }

    /**
     * The kind of tool a block asks for, as the Protoclay decides it: the pickaxe list is tried first and contains the
     * shovel materials, so dirt counts as {@code pick} (the same order as 1.7.10).
     */
    public static String requiredToolType(BlockState state) {
        if (MATERIALS_PICK.test(state)) return "pick";
        if (MATERIALS_SHOVEL.test(state)) return "shovel";
        if (MATERIALS_AXE.test(state)) return "axe";
        return "";
    }

    // ---- ray trace ------------------------------------------------------------------------------------------

    /** The block the player looks at (fluids stop the ray, as {@code raytraceFromEntity(..., true, range)} did), or null. */
    public static BlockHitResult raytrace(Player player, double range) {
        HitResult hit = player.pick(range, 1.0F, true);
        return hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK ? block : null;
    }

    public static BlockHitResult raytrace(Player player) {
        return raytrace(player, player.blockInteractionRange());
    }

    // ---- the break event ------------------------------------------------------------------------------------

    /**
     * Architectury {@code BlockEvent.BREAK}: the counterpart of 1.7.10's {@code Item.onBlockStartBreak}, called before the
     * block the player breaks is removed.
     */
    public static EventResult onBlockBreak(Level level, BlockPos pos, BlockState state, ServerPlayer player, IntValue xp) {
        if (BREAKING.get() || !(level instanceof ServerLevel server)) return EventResult.pass();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof IAdvancedTool tool)) return EventResult.pass();
        BREAKING.set(Boolean.TRUE);
        try {
            tool.onBlockStartBreak(held, server, pos, state, player);
        } finally {
            BREAKING.set(Boolean.FALSE);
        }
        return EventResult.pass();
    }

    /** True while an extra block is being broken by this class. */
    public static boolean isBreaking() {
        return BREAKING.get();
    }

    // ---- extra blocks ---------------------------------------------------------------------------------------

    /**
     * Breaks the blocks of the box {@code [xs, xe) x [ys, ye) x [zs, ze)} around {@code origin} that the materials list
     * allows (1.7.10 {@code removeBlocksInIteration}), and moves the items on the ground to the player. The origin
     * itself is left to the break that is in progress. {@code only} (the column mode) limits the break to that block;
     * the pinned 1.7.10 fork dropped the parameter by accident ({@code removeBlockWithDrops} still has it).
     */
    public static void removeBlocksInIteration(ServerPlayer player, ServerLevel level, BlockPos origin, int xs, int ys,
            int zs, int xe, int ye, int ze, Block only, Predicate<BlockState> materials) {
        for (int x1 = xs; x1 < xe; x1++) {
            for (int y1 = ys; y1 < ye; y1++) {
                for (int z1 = zs; z1 < ze; z1++) {
                    BlockPos pos = origin.offset(x1, y1, z1);
                    if (pos.equals(origin)) continue;
                    if (only != null && level.getBlockState(pos).getBlock() != only) continue;
                    breakExtraBlock(level, pos, player, materials);
                }
            }
        }
        AABB box = new AABB(origin.getX() + xs, origin.getY() + ys, origin.getZ() + zs, origin.getX() + xe,
                origin.getY() + ye, origin.getZ() + ze);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            item.setPos(player.getX(), player.getY() + 1, player.getZ());
        }
    }

    /**
     * 1.7.10 {@code breakExtraBlock}: only blocks the tool can harvest and the materials list contains; bedrock only in the
     * Bedrock dimension below its top two layers. Blocks that cannot be broken at all (hardness -1: end portal frames,
     * barriers...) are left alone; the pinned fork would have destroyed them.
     */
    public static void breakExtraBlock(ServerLevel level, BlockPos pos, ServerPlayer player,
            Predicate<BlockState> materials) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        boolean bedrock = state.is(Blocks.BEDROCK);
        if (!bedrock && state.getDestroySpeed(level, pos) < 0) return;
        if (!player.hasCorrectToolForDrops(state) || !materials.test(state)) return;
        if (bedrock) {
            if (level.dimension() != KamiHooks.BEDROCK_DIMENSION) return;
            if (pos.getY() > level.getMaxBuildHeight() - 3) return;
        }
        player.gameMode.destroyBlock(pos);
    }

    // ---- tree felling (Axe, tree mode) ----------------------------------------------------------------------

    private static final int TREE_LIMIT = 512;
    private static final int TREE_RADIUS = 24;

    /**
     * 1.7.10 {@code Utils.isWoodLog} and {@code BlockUtils.breakFurthestBlock} in a loop: every log of the same kind that is
     * connected to the broken one (diagonals included) is broken, the furthest first, and the items are gathered at the
     * foot of the tree. Thaumcraft's own helpers are internal (API request A6); this is a port of their behaviour with
     * limits (at most {@value #TREE_LIMIT} logs, {@value #TREE_RADIUS} blocks sideways) so that one click cannot stall
     * the server.
     */
    public static void fellTree(ServerLevel level, BlockPos origin, ServerPlayer player) {
        BlockState originState = level.getBlockState(origin);
        if (!originState.is(BlockTags.LOGS)) return;
        Block log = originState.getBlock();

        List<BlockPos> logs = connected(level, origin, log);
        logs.sort((a, b) -> Double.compare(b.distSqr(origin), a.distSqr(origin)));
        for (BlockPos pos : logs) {
            if (!pos.equals(origin) && level.getBlockState(pos).getBlock() == log) player.gameMode.destroyBlock(pos);
        }

        AABB box = new AABB(origin.getX() - 5, origin.getY() - 1, origin.getZ() - 5, origin.getX() + 5,
                origin.getY() + 64, origin.getZ() + 5);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            item.setPos(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
            item.setNoPickUpDelay();
        }
    }

    private static List<BlockPos> connected(ServerLevel level, BlockPos origin, Block log) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && found.size() < TREE_LIMIT) {
            BlockPos at = queue.poll();
            found.add(at);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = at.offset(dx, dy, dz);
                        if (Math.abs(next.getX() - origin.getX()) > TREE_RADIUS
                                || Math.abs(next.getZ() - origin.getZ()) > TREE_RADIUS || !seen.add(next)) continue;
                        if (level.getBlockState(next).getBlock() == log) queue.add(next);
                    }
                }
            }
        }
        return found;
    }
}
