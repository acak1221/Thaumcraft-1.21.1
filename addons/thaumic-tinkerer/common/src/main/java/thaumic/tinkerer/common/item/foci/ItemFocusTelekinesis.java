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

import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.core.helper.MiscHelper;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Wand Focus: Telekinesis. Held, it pulls every dropped item within six blocks of a point five blocks ahead of the player
 * (on the player when he sneaks) to that point; costs 0.05 Aer and 0.05 Perditio per tick while there is something to pull.
 */
public class ItemFocusTelekinesis extends ItemModFocus {
    /** The cost of a cast (or of a tick), in centivis. */
    public static final AspectList VIS_USAGE = new AspectList().add(Aspect.AIR, 5).add(Aspect.ENTROPY, 5);

    /** The reach of the pull around its target, in blocks (1.7.10 {@code range}). */
    public static final int RANGE = 6;
    /** How far ahead of the player the target is (1.7.10 {@code range - 1}). */
    public static final double DISTANCE = RANGE - 1;
    /** The speed of a pulled item, in blocks per tick. */
    public static final float PULL_SPEED = 0.3333F;
    /** The sparkles of a pulled item are the violet ones of the API ({@code sparkle(x, y, z, 0)}). */
    private static final int SPARKLE_TYPE = FociFx.SPARKLE_VIOLET;

    public ItemFocusTelekinesis(Item.Properties properties) {
        super(properties);
    }

    /** The point the items are pulled to: the centre of the player, five blocks along his sight unless he sneaks, half a block up. */
    public static Vec3 target(Player player) {
        Vec3 target = player.getBoundingBox().getCenter();
        if (!player.isShiftKeyDown()) target = target.add(player.getLookAngle().scale(DISTANCE));
        return target.add(0.0, 0.5, 0.0);
    }

    /** The box in which items are pulled to the target. */
    public static AABB pullBox(Vec3 target) {
        return new AABB(target.x - RANGE, target.y - RANGE, target.z - RANGE, target.x + RANGE, target.y + RANGE,
                target.z + RANGE);
    }

    @Override
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {
        Vec3 target = target(player);
        List<ItemEntity> entities = player.level().getEntitiesOfClass(ItemEntity.class, pullBox(target));

        if (!entities.isEmpty()
                && WandHelper.consumeAllVis(wandstack, player, getVisCost(WandHelper.getFocusItem(wandstack)), true, false)) {
            for (ItemEntity item : entities) {
                MiscHelper.setEntityMotionFromVector(item, target, PULL_SPEED);
                FociFx.sparkle(player.level(), item.getX(), item.getY(), item.getZ(), SPARKLE_TYPE);
            }
        }
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTTE" + super.getSortingHelper(focusstack);
    }

    @Override
    protected boolean hasOrnament() {
        return true;
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0x9C00BE;
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return VIS_USAGE.copy();
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focusstack) {
        return true;
    }

    // TODO (1.7.10) add range upgrades
    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[] { FocusUpgradeType.frugal } : null;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        TTResearchItem research = new TTResearchItem(LibResearch.KEY_FOCUS_TELEKINESIS,
                new AspectList().add(Aspect.ELDRITCH, 2).add(Aspect.MAGIC, 1).add(Aspect.MOTION, 1), -4, -6, 2,
                new ItemStack(this));
        research.setParents(LibResearch.KEY_FOCUS_FLIGHT).setConcealed().setPages(new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_FOCUS_TELEKINESIS)).setSecondary();
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_FOCUS_TELEKINESIS, new ItemStack(this), 5,
                new AspectList().add(Aspect.MOTION, 10).add(Aspect.AIR, 20).add(Aspect.ENTROPY, 20).add(Aspect.MIND, 10),
                new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.QUARTZ), new ItemStack(Items.QUARTZ),
                new ItemStack(Items.QUARTZ), new ItemStack(Items.QUARTZ), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.GOLD_INGOT), tc("itemShard", 0));
    }
}
