package com.example.tc4addon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.WandHelper;

/**
 * Wand focus: Sparks. A 1.7.10 {@code ItemFocusBasic} subclass with the 1.21.1 types: a bolt to the block the player
 * looks at, and a fire on the face it hits.
 */
// [doc:focus]
public class SparkFocus extends ItemFocusBasic {
    /** Focus costs are centivis: half a vis of Ignis and a quarter of Aer per cast. */
    private static final AspectList COST = new AspectList().add(Aspect.FIRE, 50).add(Aspect.AIR, 25);

    public SparkFocus(Properties properties) { super(properties); } // stack size one and the rare rarity are set here

    @Override public AspectList getVisCost(ItemStack focus) { return COST.copy(); }
    @Override public int getFocusColor(ItemStack focus) { return 0xFF9A2E; }
    @Override public int getActivationCooldown(ItemStack focus) { return 500; } // milliseconds, as in 1.7.10

    /** The focal manipulator offers these at every rank; frugal is applied by the wand itself. */
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency};
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        double range = 16 + 4 * WandHelper.getFocusPotency(wand);
        HitResult hit = player.pick(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return null; // not handled
        if (level.isClientSide) return wand; // both sides run this; the server pays and acts
        if (!(level instanceof ServerLevel server)) return null;
        BlockPos fire = block.getBlockPos().relative(block.getDirection());
        if (!mayIgnite(server, player, wand, hand, block, fire)) return null;
        // All or nothing; the cost is discounted by the wand's cap, worn gear and the frugal upgrade.
        if (!WandHelper.consumeAllVis(wand, player, getVisCost(WandHelper.getFocusItem(wand)), true, false)) return null;
        if (!mayIgnite(server, player, wand, hand, block, fire)) return null;
        ThaumcraftFX.zap(server, player.getEyePosition().add(0, -0.2, 0), hit.getLocation());
        if (server.isEmptyBlock(fire) && BaseFireBlock.canBePlacedAt(server, fire, block.getDirection())
                && mayIgnite(server, player, wand, hand, block, fire)) {
            server.setBlockAndUpdate(fire, BaseFireBlock.getState(server, fire));
        }
        return wand;
    }

    /** Air-use raycasts do not pass through vanilla's block-use permission checks. */
    private static boolean mayIgnite(ServerLevel level, Player player, ItemStack wand, InteractionHand hand,
                                     BlockHitResult hit, BlockPos fire) {
        BlockPos source = hit.getBlockPos();
        return player instanceof ServerPlayer actor && actor.serverLevel() == level
                && level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level
                && actor.isAlive() && !actor.isRemoved() && !actor.isSpectator()
                && actor.getItemInHand(hand) == wand
                && !level.isOutsideBuildHeight(source) && !level.isOutsideBuildHeight(fire)
                && level.getWorldBorder().isWithinBounds(source) && level.getWorldBorder().isWithinBounds(fire)
                && level.hasChunkAt(source) && level.hasChunkAt(fire)
                && level.mayInteract(actor, source) && level.mayInteract(actor, fire)
                && actor.mayUseItemAt(fire, hit.getDirection(), wand);
    }
}
// [/doc]
