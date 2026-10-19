package thaumcraft.common.items.wands;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;

/** Casting-only CENTIVIS pricing. Recipe pricing/positive-price validation stays
 * in ArcaneVis. Original per-aspect FLOAT truncation may legitimately yield zero. */
public final class FocusVis {
    private FocusVis() {}

    public static Map<String, Integer> price(AspectList baseCentivis, ItemStack wand, Player actor) {
        if (baseCentivis == null || baseCentivis.size() == 0 || baseCentivis.size() > 6)
            throw new IllegalArgumentException("Missing focus casting price");
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Aspect aspect : baseCentivis.getAspects()) {
            int amount = baseCentivis.getAmount(aspect);
            if (aspect == null || !aspect.isPrimal() || !WandData.PRIMALS.contains(aspect.getTag()) || amount <= 0)
                throw new IllegalArgumentException("Invalid focus casting aspect/amount");
            float derived = amount * WandVisDiscount.focusModifier(wand, actor, aspect);
            if (!Float.isFinite(derived) || derived >= Integer.MAX_VALUE) throw new IllegalArgumentException("Focus price overflow");
            result.put(aspect.getTag(), (int) derived);
        }
        return Map.copyOf(result);
    }

    public static boolean canPay(ItemStack wand, Map<String, Integer> costs) {
        if (wand == null || !ArcaneVis.isCraftingWand(wand) || costs == null || costs.isEmpty() || costs.size() > 6) return false;
        WandData data = ItemWandCasting.data(wand);
        return costs.entrySet().stream().allMatch(entry -> entry.getKey() != null && WandData.PRIMALS.contains(entry.getKey())
                && entry.getValue() != null && entry.getValue() >= 0 && data.amount(entry.getKey()) >= entry.getValue());
    }

    /** Mutate only the supplied stack's vis component; callers own hand/binding proof. */
    public static boolean debit(ItemStack wand, Map<String, Integer> costs) {
        if (!canPay(wand, costs)) return false;
        WandData data = ItemWandCasting.data(wand);
        for (String tag : WandData.PRIMALS) if (costs.getOrDefault(tag, 0) > 0)
            data = data.withAmount(tag, data.amount(tag) - costs.get(tag));
        wand.set(ThaumcraftContent.WAND_DATA.get(), data);
        return true;
    }
}
