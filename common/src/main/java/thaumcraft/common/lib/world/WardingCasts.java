package thaumcraft.common.lib.world;

import dev.architectury.event.events.common.BlockEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockWarded;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWards;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusWarding;
import thaumcraft.common.items.wands.foci.WardingRules;
import thaumcraft.common.tiles.TileWarded;

/** Server-authoritative ward/restore attempts. No copied block inventories and no owner name hashes. */
public final class WardingCasts {
    private static final SoundEvent ZAP = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "zap"));
    private static final Map<ServerLevel, Map<BlockPos, Long>> RETRIES = new WeakHashMap<>();
    private static final ThreadLocal<Creation> CREATING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private record Creation(ServerLevel level, BlockPos pos, BlockState original, ServerPlayer player) {}
    private WardingCasts() {}

    public static void clear(ServerLevel level) { RETRIES.remove(level); }
    public static boolean isInitializing(TileWarded tile, BlockState original, UUID owner) {
        Creation attempt = CREATING.get();
        return attempt != null && tile.getLevel() == attempt.level && tile.getBlockPos().equals(attempt.pos)
                && attempt.level.getBlockEntity(attempt.pos) == tile && attempt.player.server.isSameThread()
                && original == attempt.original && attempt.player.getUUID().equals(owner);
    }
    private static boolean actor(ServerPlayer player, ItemStack wand, InteractionHand hand, ServerLevel level) {
        return player.server.isSameThread() && player.serverLevel() == level && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && !player.hasDisconnected() && player.connection != null && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && player.getItemInHand(hand) == wand && wand.getCount() == 1 && ArcaneVis.isCraftingWand(wand)
                && FocusData.get(wand).getItem() instanceof ItemFocusWarding;
    }
    private static boolean position(ServerLevel level, ServerPlayer player, BlockPos pos, Direction face, ItemStack wand) {
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, wand);
    }
    public static boolean wardable(ServerLevel level, BlockPos pos, BlockState state) {
        // The old isBlockNormalCubeDefault(..., true) path: a solid normal block without a tile, not plants/fluids/containers.
        return !state.isAir() && !state.hasBlockEntity() && level.getBlockEntity(pos) == null && !state.canBeReplaced()
                && state.getFluidState().isEmpty() && state.isSolidRender(level, pos) && state.isCollisionShapeFullBlock(level, pos);
    }

    public static boolean use(ServerPlayer player, ItemStack wand, InteractionHand hand) {
        if (player == null || !player.server.isSameThread()) return false;
        ServerLevel level = player.serverLevel();
        if (ACTIVE.get() != null || !actor(player, wand, hand, level)) return false;
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        if (!Double.isFinite(eye.x + eye.y + eye.z + look.x + look.y + look.z)) return false;
        Vec3 end = eye.add(look.scale(player.blockInteractionRange()));
        if (!level.hasChunksAt(BlockPos.containing(eye), BlockPos.containing(end))) return false;
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        player.swing(hand);
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        Map<BlockPos, Long> delays = RETRIES.computeIfAbsent(level, ignored -> new HashMap<>());
        long now = level.getGameTime();
        delays.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (delays.getOrDefault(pos, Long.MIN_VALUE) > now) return false;
        delays.put(pos.immutable(), now + WardingRules.RETRY_TICKS);
        ACTIVE.set(Boolean.TRUE);
        try {
            if (!position(level, player, pos, face, wand) || !actor(player, wand, hand, level)) return false;
            boolean restoring = level.getBlockEntity(pos) instanceof TileWarded;
            var focus = FocusData.get(wand);
            WandArchitect.Area area = WandArchitect.enabled(wand) ? WandArchitect.area(wand, focus) : new WandArchitect.Area(0, 0, 0, 0);
            List<BlockPos> cells = WandArchitect.connected(pos, face, area, false, cell -> {
                if (!level.isInWorldBounds(cell) || !level.hasChunkAt(cell)) return false;
                return restoring ? level.getBlockEntity(cell) instanceof TileWarded tile && tile.ownedBy(player.getUUID())
                        : wardable(level, cell, level.getBlockState(cell));
            });
            boolean changed = false;
            for (BlockPos cell : cells) {
                if (!actor(player, wand, hand, level) || !ItemStack.matches(focus, FocusData.get(wand))
                        || !player.getEyePosition().equals(eye) || !player.getLookAngle().equals(look)) break;
                if (!restoring && !FocusVis.canPay(wand, ItemFocusWarding.activationCost(wand, player))) break;
                changed |= one(player, level, wand, hand, cell, face, restoring, eye, look);
            }
            if (changed) sound(level, pos);
            return changed;
        } finally {
            ACTIVE.remove();
            player.inventoryMenu.broadcastChanges();
        }
    }
    private static boolean stillCasting(ServerPlayer player, ServerLevel level, ItemStack wand, InteractionHand hand, ItemStack binding,
                                        BlockPos pos, Direction face, Vec3 eye, Vec3 look) {
        return actor(player, wand, hand, level) && ItemStack.matches(binding, wand) && position(level, player, pos, face, wand)
                && player.getEyePosition().equals(eye) && player.getLookAngle().equals(look);
    }
    private static boolean one(ServerPlayer player, ServerLevel level, ItemStack wand, InteractionHand hand, BlockPos pos,
                               Direction face, boolean restoring, Vec3 eye, Vec3 look) {
        ItemStack binding = wand.copy();
        if (!stillCasting(player, level, wand, hand, binding, pos, face, eye, look)) return false;
        if (restoring) {
            if (!(level.getBlockEntity(pos) instanceof TileWarded ward) || !ward.ownedBy(player.getUUID())) return false;
            BlockState expected = level.getBlockState(pos), back = ward.originalState();
            if (BlockEvent.PLACE.invoker().placeBlock(level, pos, back, player).isFalse()) return false;
            if (!stillCasting(player, level, wand, hand, binding, pos, face, eye, look) || level.getBlockEntity(pos) != ward
                    || level.getBlockState(pos) != expected || !ward.ownedBy(player.getUUID())) return false;
            boolean restored = level.setBlock(pos, back, Block.UPDATE_ALL);
            if (restored) level.scheduleTick(pos, back.getBlock(), 2);
            return restored;
        }
        BlockState original = level.getBlockState(pos);
        if (!wardable(level, pos, original)) return false;
        BlockState replacement = ThaumcraftWards.WARDED.get().defaultBlockState().setValue(BlockWarded.LIGHT, original.getLightEmission());
        if (BlockEvent.PLACE.invoker().placeBlock(level, pos, replacement, player).isFalse()) return false;
        if (!stillCasting(player, level, wand, hand, binding, pos, face, eye, look)
                || level.getBlockState(pos) != original || !wardable(level, pos, original)) return false;
        // Protection callbacks may change equipment discounts too: price only after they have finished.
        Map<String, Integer> costs = ItemFocusWarding.activationCost(wand, player);
        ItemStack paid = wand.copy();
        if (!FocusVis.debit(paid, costs)) return false;
        // Original pays before replacing the eligible block. A post-payment native failure is never refunded or replayed.
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        player.getInventory().setChanged();
        CREATING.set(new Creation(level, pos.immutable(), original, player));
        try {
            if (!level.setBlock(pos, replacement, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) return false;
            if (!(level.getBlockEntity(pos) instanceof TileWarded ward)) {
                if (level.getBlockState(pos).is(ThaumcraftWards.WARDED.get())) level.setBlock(pos, original, Block.UPDATE_ALL);
                return false;
            }
            ward.initialize(original, player.getUUID());
            level.sendBlockUpdated(pos, replacement, replacement, Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(pos, replacement.getBlock());
            return true;
        } finally { CREATING.remove(); }
    }
    private static void sound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, ZAP, SoundSource.PLAYERS, .25F, 1.0F);
    }
}
