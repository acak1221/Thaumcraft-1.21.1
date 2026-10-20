package thaumcraft.common.items.wands;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;

/** Original WandManager:75-101 / ItemWandCasting:247-259, for native caps, four accessory slots and armor. */
public final class WandVisDiscount {
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    private WandVisDiscount() {}

    /** Net INTEGER percentage points, including the strongest exhaustion variant. */
    public static int percentagePoints(Player player, Aspect aspect) {
        if (player == null) return 0;
        int total = 0;
        var accessories=thaumcraft.common.lib.baubles.PlayerAccessories.get(player);
        for(int index=0;index<4;index++) {
            ItemStack stack=accessories.getItem(index);
            if(!stack.isEmpty() && stack.getItem() instanceof IVisDiscountGear gear)total+=gear.getVisDiscount(stack,player,aspect);
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getItem() instanceof IVisDiscountGear gear) total += gear.getVisDiscount(armor, player, aspect);
        }
        MobEffectInstance normal = player.getEffect(ThaumcraftFlux.visExhaust());
        MobEffectInstance infectious = player.getEffect(ThaumcraftFlux.infectiousVisExhaust());
        if (normal != null || infectious != null) {
            int level1 = normal == null ? 0 : normal.getAmplifier();
            int level2 = infectious == null ? 0 : infectious.getAmplifier();
            total -= (Math.max(level1, level2) + 1) * 10;
        }
        return total;
    }

    /** Shared native arcane/focus pricing; callers must supply the actual aspect. */
    public static float modifier(ItemStack wand, Player player, Aspect aspect) {
        float cap = !wand.isEmpty() && wand.getItem() instanceof ItemWandCasting
                ? WandMaterials.capModifier(ItemWandCasting.data(wand).cap()) : 1.0F;
        return modifier(cap, percentagePoints(player, aspect));
    }

    /** Original arithmetic reducer: convert the integer net discount only once. */
    public static float modifier(float capModifier, int netPercentagePoints) {
        // Do not subtract a gear float and add a separate exhaustion float:
        // .9F - .05F is below .85F, giving424/254, not rounded425/255.
        return Math.max(capModifier - netPercentagePoints / 100.0F, .1F);
    }

    /** Separate casting path: Frugal must NEVER discount arcane recipes. */
    public static float focusModifier(ItemStack wand, Player player, Aspect aspect) {
        float cap = !wand.isEmpty() && wand.getItem() instanceof ItemWandCasting
                ? WandMaterials.capModifier(ItemWandCasting.data(wand).cap()) : 1F;
        return focusModifier(cap, percentagePoints(player, aspect), FocusUpgradeData.level(FocusData.get(wand), FocusUpgradeType.frugal));
    }

    public static float focusModifier(float capModifier, int netPercentagePoints, int frugalLevel) {
        if (!Float.isFinite(capModifier) || frugalLevel < 0 || frugalLevel > 5) throw new IllegalArgumentException("Invalid focus modifier");
        // Original two FLOAT subtractions, not one combined integer percentage.
        return Math.max(capModifier - netPercentagePoints / 100F - frugalLevel / 10F, .1F);
    }
}
