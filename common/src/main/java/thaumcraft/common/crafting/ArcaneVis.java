package thaumcraft.common.crafting;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;

/** Original cap/armor/exhaustion pricing for the bounded native materials. */
public final class ArcaneVis {
    private ArcaneVis() {}

    public static boolean isCraftingWand(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemWandCasting)) return false;
        WandData data = ItemWandCasting.data(stack);
        return WandMaterials.supportsRod(data.rod()) && WandMaterials.supportsCap(data.cap());
    }

    public static Map<String, Integer> price(Map<String, Integer> vis, ItemStack wand) {
        return price(vis, wand, null);
    }

    public static Map<String, Integer> price(Map<String, Integer> vis, ItemStack wand, Player actor) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        // ItemWandCasting:306-335 multiplies whole vis by100, then the FLOAT
        // modifier, then truncates. Addon gear may discount each aspect differently.
        for (String tag : WandData.PRIMALS) {
            int amount = vis.getOrDefault(tag, 0);
            if (amount > 0) costs.put(tag, (int) (amount * 100 * WandVisDiscount.modifier(wand, actor, Aspect.getAspect(tag))));
        }
        return Map.copyOf(costs);
    }

    public static boolean canPay(ItemStack wand, Map<String, Integer> costs) {
        if (!isCraftingWand(wand) || costs.isEmpty()) return false;
        WandData state = ItemWandCasting.data(wand);
        return costs.entrySet().stream().allMatch(entry -> WandData.PRIMALS.contains(entry.getKey())
                && entry.getValue() > 0 && state.amount(entry.getKey()) >= entry.getValue());
    }

    /** Called on a private wand copy inside the server menu's validated transaction. */
    public static boolean debit(ItemStack wand, Map<String, Integer> costs) {
        if (!canPay(wand, costs)) return false;
        WandData state = ItemWandCasting.data(wand);
        for (var entry : costs.entrySet()) state = state.withAmount(entry.getKey(), state.amount(entry.getKey()) - entry.getValue());
        wand.set(ThaumcraftContent.WAND_DATA.get(), state);
        return true;
    }
}
