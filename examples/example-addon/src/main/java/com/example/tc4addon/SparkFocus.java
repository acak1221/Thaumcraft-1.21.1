package com.example.tc4addon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        // All or nothing; the cost is discounted by the wand's cap, worn gear and the frugal upgrade.
        if (!WandHelper.consumeAllVis(wand, player, getVisCost(WandHelper.getFocusItem(wand)), true, false)) return null;
        ThaumcraftFX.zap((ServerLevel) level, player.getEyePosition().add(0, -0.2, 0), hit.getLocation());
        BlockPos fire = block.getBlockPos().relative(block.getDirection());
        if (level.isEmptyBlock(fire) && BaseFireBlock.canBePlacedAt(level, fire, block.getDirection())) {
            level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
        }
        return wand;
    }
}
// [/doc]
