/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md); its rule is the {@code ISpecialArmor} part of Vazkii's {@code ItemIchorclothArmor}.
 * Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.item.kami.armor;

import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * The armour absorption of the Ichorcloth pieces, 1.7.10 {@code ItemIchorclothArmor implements ISpecialArmor}:
 *
 * <pre>
 * getProperties(...)  = new ArmorProperties(0, getArmorMaterial().getDamageReductionAmount(slot) * 0.0425, Integer.MAX_VALUE)
 * damageArmor(...)    = no-op
 * </pre>
 *
 * Every piece takes {@code 0.0425 x its armour points} of every blow (priority 0, no cap), the four together
 * {@code 20 x 0.0425 = 85%}, and never wears (the pieces have no durability here, so the game's own wear call does nothing).
 * Forge's {@code ArmorProperties.ApplyArmor} added the ratios of all pieces of one priority and took the sum off the blow:
 * a plain {@code ItemArmor} contributed {@code damageReduceAmount / 25} (4% per point), an {@code ISpecialArmor} what it
 * returned. This is the same sum here: the Ichorcloth ratio is added to the share the other armour takes under the 1.21.1
 * formula, and the sum never takes more than the blow.
 *
 * <p>Registered on {@code TinkererEvents.LIVING_ARMOR_ABSORB} by {@code KamiArmorHandler.register()}. The armour points that
 * the pieces give the entity's attributes (3/8/6/3, the 1.7.10 {@code getArmorDisplay} of the HUD) are a display only: the
 * listener <i>replaces</i> the vanilla amount, so they never stack with the 85%.</p>
 */
public final class IchorAbsorption {
    /** 1.7.10 {@code ItemIchorclothArmor.getProperties}: the share of a blow that one armour point of a piece takes. */
    public static final double RATIO_PER_POINT = 0.0425;

    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET };

    private IchorAbsorption() {}

    /** The share of a blow that a piece of {@code armourPoints} takes (1.7.10 {@code damageReductionAmount * 0.0425}). */
    public static double ratio(int armourPoints) {
        return armourPoints * RATIO_PER_POINT;
    }

    /**
     * The amount of a blow that the armour takes off: {@code damage x ichorRatio} for the Ichorcloth, plus
     * {@code otherAbsorbed} (what the rest of the armour takes under the 1.21.1 formula), never more than the blow itself
     * (Forge normalised ratios above 1 for the same effect).
     *
     * @param ichorRatio the summed ratios of the worn Ichorcloth pieces; with none (0) the other armour's amount is returned as it is
     */
    public static float absorbed(float damage, double ichorRatio, float otherAbsorbed) {
        if (!(ichorRatio > 0.0)) return otherAbsorbed;
        return (float) Math.min(damage * ichorRatio + otherAbsorbed, damage);
    }

    /**
     * The listener of {@code TinkererEvents.LIVING_ARMOR_ABSORB}. For an entity that wears no Ichorcloth piece it returns
     * {@code absorbed} untouched (the vanilla armour, and whatever the other listeners did). For one that does, it replaces the
     * amount: the Ichorcloth share as above plus the vanilla share of the armour that is <i>not</i> Ichorcloth (the armour value of
     * the entity less the points of the Ichorcloth pieces, with the toughness), so a mixed set keeps the other armour's worth.
     */
    public static float onLivingArmorAbsorb(LivingEntity victim, DamageSource source, float damage, float absorbed) {
        int ichorPoints = 0;
        double ichorRatio = 0.0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = victim.getItemBySlot(slot);
            // a piece counts only in the slot it is made for (the game gives it no attributes anywhere else)
            if (stack.getItem() instanceof ItemIchorclothArmor piece && piece.getEquipmentSlot() == slot) {
                ichorPoints += piece.getDefense();
                ichorRatio += ratio(piece.getDefense());
            }
        }
        if (ichorPoints == 0) return absorbed;
        return absorbed(damage, ichorRatio, otherArmourAbsorbed(victim, source, damage, ichorPoints));
    }

    /** What vanilla's formula takes with the armour value of everything but the Ichorcloth pieces. */
    private static float otherArmourAbsorbed(LivingEntity victim, DamageSource source, float damage, int ichorPoints) {
        int otherArmour = Math.max(0, victim.getArmorValue() - ichorPoints);
        if (otherArmour == 0) return 0.0F;
        float left = CombatRules.getDamageAfterAbsorb(victim, damage, source, (float) otherArmour,
                (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        return damage - left;
    }
}
