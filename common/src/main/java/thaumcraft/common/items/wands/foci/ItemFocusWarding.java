package thaumcraft.common.items.wands.foci;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.IArchitect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.lib.world.WardingCasts;

/** TC4 ownership warding: an exact remembered block, quarter-vis earth/order and tenth-vis water; owner undo is free. */
public final class ItemFocusWarding extends ItemFocusBasic implements IArchitect {
    public ItemFocusWarding(Properties properties) { super(properties); }
    @Override public AspectList getVisCost(ItemStack focus) {
        return new AspectList().add(Aspect.EARTH, WardingRules.EARTH).add(Aspect.ORDER, WardingRules.ORDER).add(Aspect.WATER, WardingRules.WATER);
    }
    @Override public int getFocusColor(ItemStack focus) { return 16771535; }
    @Override public String getSortingHelper(ItemStack focus) { return "BWA" + super.getSortingHelper(focus); }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> new FocusUpgradeType[]{FocusUpgradeType.frugal};
            case 2 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.architect};
            case 3, 4, 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.enlarge};
            default -> new FocusUpgradeType[0];
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgradeType type, int rank) {
        return (type != FocusUpgradeType.enlarge || FocusUpgradeData.read(focus).level(FocusUpgradeType.architect) > 0)
                && super.canApplyUpgrade(focus, player, type, rank);
    }
    @Override public int getMaxAreaSize(ItemStack focus) { return 3 + FocusUpgradeData.level(focus, FocusUpgradeType.enlarge); }
    public static Map<String, Integer> activationCost(ItemStack wand, Player actor) {
        return FocusVis.price(new AspectList().add(Aspect.EARTH, WardingRules.EARTH).add(Aspect.ORDER, WardingRules.ORDER)
                .add(Aspect.WATER, WardingRules.WATER), wand, actor);
    }
    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player actor, InteractionHand hand) {
        if (!level.isClientSide && actor instanceof ServerPlayer player) WardingCasts.use(player, wand, hand);
        return wand;
    }
}
