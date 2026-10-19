package thaumcraft.api.wands;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;

/** Native supported focus contracts; upgrade candidates never advertise inert branches. */
public class ItemFocusBasic extends Item {
    public ItemFocusBasic(Properties properties) { super(properties.stacksTo(1).rarity(Rarity.RARE)); }

    /** Like original ItemFocusBasic:65-72, these amounts are CENTIVIS, not recipe vis. */
    public AspectList getVisCost(ItemStack focusstack) { return null; }
    public boolean isVisCostPerTick(ItemStack focusstack) { return false; }
    public int getFocusColor(ItemStack focusstack) { return 0; }
    public String getSortingHelper(ItemStack focusstack) {
        FocusUpgradeData data = FocusUpgradeData.read(focusstack);
        StringBuilder result = new StringBuilder();
        for (short id : data.applied()) result.append(id);
        return result.toString();
    }
    public int getActivationCooldown(ItemStack focusstack) { return 0; }
    public WandFocusAnimation getAnimation(ItemStack focusstack) { return WandFocusAnimation.WAVE; }

    public ItemStack onFocusRightClick(ItemStack wandstack, Level level, Player player, InteractionHand hand) { return null; }
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {}
    public void onPlayerStoppedUsingFocus(ItemStack wandstack, Level level, Player player, int count) {}

    public short[] getAppliedUpgrades(ItemStack focusstack) { return FocusUpgradeData.read(focusstack).applied(); }
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) { return new FocusUpgradeType[0]; }
    public int getUpgradeLevel(ItemStack focusstack, FocusUpgradeType type) { return FocusUpgradeData.level(focusstack, type); }
    public boolean isUpgradedWith(ItemStack focusstack, FocusUpgradeType type) { return getUpgradeLevel(focusstack, type) > 0; }

    public boolean canApplyUpgrade(ItemStack focusstack, Player player, FocusUpgradeType type, int rank) {
        if (rank < 1 || rank > 5 || type == null || FocusUpgradeType.get(type.id) != type
                || focusstack == null || focusstack.getItem() != this || !FocusUpgradeData.supportedForFocus(focusstack)
                || FocusUpgradeData.read(focusstack).ranks().get(rank - 1) != -1) return false;
        FocusUpgradeType[] choices = getPossibleUpgradesByRank(focusstack, rank);
        if (choices != null) for (FocusUpgradeType choice : choices) if (type.equals(choice)) return true;
        return false;
    }

    /** Bounds-before-index fixes the original crash; machine owns sequential rank
     * progression. Direct API holes survive; unsupported branches cannot be paid. */
    public boolean applyUpgrade(ItemStack focusstack, FocusUpgradeType type, int rank) {
        if (!canApplyUpgrade(focusstack, null, type, rank)) return false;
        focusstack.set(ThaumcraftFoci.FOCUS_UPGRADES.get(), FocusUpgradeData.read(focusstack).withUpgrade(type, rank));
        return true;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        AspectList cost = getVisCost(stack);
        if (cost != null && cost.size() > 0) {
            tooltip.add(Component.translatable(isVisCostPerTick(stack) ? "item.Focus.cost2" : "item.Focus.cost1"));
            for (Aspect aspect : cost.getAspectsSorted()) {
                String amount = BigDecimal.valueOf(cost.getAmount(aspect), 2).stripTrailingZeros().toPlainString();
                tooltip.add(Component.literal(" " + aspect.getName())
                        .withStyle(ChatFormatting.getByCode(aspect.getChatcolor().charAt(0)))
                        .append(Component.literal(" x " + amount).withStyle(ChatFormatting.RESET)));
            }
        }
        if (!FocusUpgradeData.supportedForFocus(stack)) {
            tooltip.add(Component.translatable("tc.port.focus.upgrades.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        LinkedHashMap<Short, Integer> levels = new LinkedHashMap<>();
        for (short id : getAppliedUpgrades(stack)) if (id >= 0) levels.merge(id, 1, Integer::sum);
        levels.forEach((id, level) -> {
            var line = Component.translatable(FocusUpgradeType.get(id).name).withStyle(ChatFormatting.DARK_PURPLE);
            if (level > 1) line.append(" ").append(Component.translatable("enchantment.level." + level));
            tooltip.add(line);
        });
        tooltip.add(Component.translatable("tc.port.focus.upgrades.partial").withStyle(ChatFormatting.DARK_GRAY));
    }

    public enum WandFocusAnimation { WAVE, CHARGE }
}
