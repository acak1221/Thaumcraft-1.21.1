package thaumcraft.common.lib.enchantment;

import java.util.LinkedHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRepairableExtended;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Original EventHandlerEntity:245-269,295-309 / WandManager:104-120. */
public final class EnchantmentRepair {
    private EnchantmentRepair() {}
    public static void tick(Player player) {
        // Original func_70451_h is getHotbarSize()==9, not the whole main inventory.
        for (int slot = 0; slot < 9; slot++) repair(player.getInventory().items.get(slot), player);
        for (ItemStack armor : player.getInventory().armor) repair(armor, player);
    }
    public static AspectList cost(ItemStack stack, Player player, int repairLevel) {
        AspectList base = ObjectAspectResolver.baseTags(stack, player.level());
        if (base.size() == 0 || repairLevel <= 0) return new AspectList();
        AspectList primals = FocalUpgradeCosts.reduceToPrimals(base), result = new AspectList();
        for (Aspect aspect : primals.getAspects()) result.add(aspect,
                EquipmentEnchantmentRules.repairCentivis(primals.getAmount(aspect), repairLevel));
        return result;
    }
    public static boolean repair(ItemStack stack, Player player) {
        if (player.level().isClientSide || player.getAbilities().instabuild || !stack.isDamaged()
                || !(stack.getItem() instanceof IRepairable)) return false;
        int level = Math.min(2, EquipmentEnchantments.level(stack, player.level(), EquipmentEnchantments.REPAIR));
        if (level <= 0) return false;
        AspectList cost = cost(stack, player, level);
        if (cost.size() == 0) return false;
        if (stack.getItem() instanceof IRepairableExtended extended && !extended.doRepair(stack, player, level)) return false;
        if (!consumeVisFromInventory(player, cost)) return false;
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - level));
        player.getInventory().setChanged();
        return true;
    }
    private static boolean consumeVisFromInventory(Player player, AspectList rawCost) {
        // Vis amulets/baubles have not been ported; their original priority is a visible pending integration.
        // Each wand is tested independently, last main-inventory slot first. Costs never split across wands.
        for (int slot = player.getInventory().items.size() - 1; slot >= 0; slot--) {
            ItemStack wand = player.getInventory().items.get(slot);
            if (!ArcaneVis.isCraftingWand(wand)) continue;
            var current = ItemWandCasting.data(wand);
            var prices = new LinkedHashMap<String, Integer>();
            boolean canPay = true;
            for (Aspect aspect : rawCost.getAspects()) {
                int amount = EquipmentEnchantmentRules.repairPrice(rawCost.getAmount(aspect), WandVisDiscount.modifier(wand, player, aspect));
                if (current.amount(aspect.getTag()) < amount) { canPay = false; break; }
                prices.put(aspect.getTag(), amount);
            }
            if (!canPay) continue;
            for (var entry : prices.entrySet()) current = current.withAmount(entry.getKey(), current.amount(entry.getKey()) - entry.getValue());
            wand.set(ThaumcraftContent.WAND_DATA.get(), current);
            player.getInventory().setChanged();
            return true;
        }
        return false;
    }
}
