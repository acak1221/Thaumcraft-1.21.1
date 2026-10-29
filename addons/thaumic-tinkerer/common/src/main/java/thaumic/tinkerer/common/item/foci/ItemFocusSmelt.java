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
package thaumic.tinkerer.common.item.foci;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Wand Focus: Efreet's Flame. Held on a block that a furnace turns into another block (cobblestone into stone, sand into
 * glass, clay into terracotta...), it smelts it after one second at 0.45 Ignis and 0.12 Perditio; potency makes it faster.
 *
 * <p>The furnace is the recipe manager's smelting recipes of the level ({@code RecipeType.SMELTING}), and a result counts only
 * when it is a block, as 1.7.10 asked {@code result.getItem() instanceof ItemBlock}. The target is the block the player looks
 * at within ten blocks, through grass and fluids ({@code BlockUtils.getTargetBlock(world, player, false)}).</p>
 *
 * <p>The progress of each player is kept per side (see {@link ItemFocusHeal}): the client counts for its particles and
 * sounds and the server for the block.</p>
 */
public class ItemFocusSmelt extends ItemModFocus {
    /** The cost of a cast (or of a tick), in centivis. */
    public static final AspectList VIS_USAGE = new AspectList().add(Aspect.FIRE, 45).add(Aspect.ENTROPY, 12);

    /** How far the focus reaches: the fixed ten blocks of {@code BlockUtils.getTargetBlock}. */
    public static final double REACH = 10.0;
    /** Ticks between two crackles while a block heats (1.7.10 {@code soundCooldown}). */
    public static final int SOUND_COOLDOWN = 3;
    /** The red of the beam. */
    private static final int BEAM_COLOR = 0xFF0000;

    /** The block each player is smelting and the ticks that are left, by player (1.7.10 {@code playerData}), one map for each side. */
    private static final Map<UUID, SmeltData> SERVER_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, SmeltData> CLIENT_DATA = new ConcurrentHashMap<>();

    public ItemFocusSmelt(Item.Properties properties) {
        super(properties);
    }

    private static Map<UUID, SmeltData> data(Level level) {
        return level.isClientSide ? CLIENT_DATA : SERVER_DATA;
    }

    /** The ticks a block needs to melt: {@code 20 - min(3, potency) * 5}. */
    public static int startProgress(int potency) {
        return 20 - Math.min(3, potency) * 5;
    }

    /** The block the furnace makes of this one, or empty when it makes none or makes something that is not a block. */
    public static ItemStack smeltResult(Level level, BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(item));
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input,
                level);
        if (recipe.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
        return result.getItem() instanceof BlockItem ? result : ItemStack.EMPTY;
    }

    /** The block the player smelts on this side, or null. */
    @Nullable
    public static SmeltData dataOf(Level level, UUID player) {
        return data(level).get(player);
    }

    /** A player left the server: forgets his block (1.7.10 {@code PlayerTracker.onPlayerLogout}). */
    public static void forget(UUID player) {
        SERVER_DATA.remove(player);
    }

    /** The client left a world: forgets the progress of every player. */
    public static void forgetClient() {
        CLIENT_DATA.clear();
    }

    @Override
    public boolean isUseItem(ItemStack focusstack) {
        return true;
    }

    @Override
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {
        if (!WandHelper.consumeAllVis(wandstack, player, VIS_USAGE, false, false)) return;

        Level level = player.level();
        BlockHitResult hit = trace(player, REACH, false, true);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            // New: spawn protection, the world border and adventure mode keep a block out of the focus's reach.
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), wandstack)) return;
            BlockState state = level.getBlockState(pos);
            ItemStack result = smeltResult(level, state);

            if (!result.isEmpty()) {
                Map<UUID, SmeltData> players = data(level);
                boolean decremented = false;

                SmeltData data = players.get(player.getUUID());
                if (data != null && data.equalPos(pos)) {
                    data.progress--;
                    decremented = true;
                    if (data.progress <= 0) {
                        if (!level.isClientSide) {
                            level.setBlock(pos, ((BlockItem) result.getItem()).getBlock().defaultBlockState(), 3);
                            WandHelper.consumeAllVis(wandstack, player, VIS_USAGE, true, false);
                        }
                        // 1.7.10 forgot the block on the server only, and the client went on counting below zero
                        // (a sound and a burst on every tick) until the server's block arrived. A block that melts
                        // into another block that melts (stone into smooth stone) would have repeated it for a
                        // whole second, so the client starts afresh too.
                        players.remove(player.getUUID());
                        decremented = false;
                        // Sound
                        ////////////////////
                        FociFx.sound(level, player, SoundEvents.FLINTANDSTEEL_USE, 0.6F, 1F);
                        FociFx.sound(level, player, SoundEvents.FIRE_AMBIENT, 1F, 1F);
                        // Particle
                        ////////////////////
                        if (level.isClientSide) {
                            for (int i = 0; i < 25; i++) {
                                FociFx.flameWisp(level, pos.getX() + Math.random(), pos.getY() + Math.random(),
                                        pos.getZ() + Math.random());
                            }
                        }
                    }
                }

                if (!decremented) {
                    int potency = WandHelper.getFocusPotency(wandstack);
                    players.put(player.getUUID(), new SmeltData(pos, startProgress(potency)));
                } else {
                    // Sound
                    ////////////////////
                    if (count % SOUND_COOLDOWN == 0) FociFx.sound(level, player, SoundEvents.FIRE_AMBIENT, 0.2F, 1F);
                    // Particle
                    ////////////////////
                    if (level.isClientSide) {
                        for (int i = 0; i < 2; i++) {
                            FociFx.flameWisp(level, pos.getX() + Math.random(), pos.getY() + Math.random(),
                                    pos.getZ() + Math.random());
                        }
                    }
                }

                FociFx.beam(level, player, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), BEAM_COLOR);
            }
        }
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTEF" + super.getSortingHelper(focusstack);
    }

    @Override
    protected boolean hasOrnament() {
        return true;
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0xFF0000;
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return VIS_USAGE.copy();
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        // 1.7.10: { frugal /* , potency */ } at every rank.
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[] { FocusUpgradeType.frugal } : null;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        TTResearchItem research = new TTResearchItem(LibResearch.KEY_FOCUS_SMELT,
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.ENERGY, 1).add(Aspect.MAGIC, 1), -2, -2, 2,
                new ItemStack(this));
        research.setParents("FOCUSEXCAVATION").setConcealed().setPages(new ResearchPage("0"),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_FOCUS_SMELT));
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(LibResearch.KEY_FOCUS_SMELT, LibResearch.KEY_FOCUS_SMELT,
                new ItemStack(this), new AspectList().add(Aspect.FIRE, 10).add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 6),
                "FNE", 'F', tc("itemFocusFire", 0), 'E', tc("itemFocusExcavation", 0), 'N', tc("itemResource", 1));
    }

    /** The block a player is smelting and the ticks that are left. */
    public static final class SmeltData {
        public final BlockPos pos;
        public int progress;

        public SmeltData(BlockPos pos, int progress) {
            this.pos = pos.immutable();
            this.progress = progress;
        }

        public boolean equalPos(BlockPos other) {
            return other.equals(pos);
        }
    }
}
