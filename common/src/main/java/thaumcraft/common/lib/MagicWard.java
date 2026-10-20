package thaumcraft.common.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.armor.MagicWardGear;

/**
 * The magic pass of the original {@code ISpecialArmor} void robes ({@code ItemVoidRobeArmor.getProperties}), run by Forge's
 * {@code ArmorProperties.ApplyArmor}: the damage is scaled by 25, every worn ward piece (priority 1) absorbs {@code armor / 35} of it, the
 * total share comes off the damage in one step and each piece is worn {@code max(1, absorbed / 25)} points (truncated). A source is magic
 * when it is in {@link #MAGIC_DAMAGE} (the original {@code isMagicDamage}: potions and instant harm, the taint, indirect magic; thorns,
 * magic in 1.7, is left to the vanilla armor value, which already counts).
 */
public final class MagicWard {
    public static final TagKey<DamageType> MAGIC_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "is_magic"));
    /** Original getProperties: ratio = damageReduceAmount / 35 for a magic source. */
    public static final double DIVISOR = 35.0;
    private static final double SCALE = 25.0;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private MagicWard() {}

    /** The share one piece of that armor value absorbs. */
    public static double ratio(int armor) { return armor / DIVISOR; }

    /** Forge ApplyArmor, one priority group: the damage left after the wards of the given armor values (a total above 1 is not reachable by robes: 16 / 35). */
    public static float reduce(float damage, int... armors) {
        double total = 0;
        for (int armor : armors) total += ratio(armor);
        double scaled = damage * SCALE;
        scaled -= scaled * total;
        return (float) (scaled / SCALE);
    }

    /** Points of wear for a piece of that armor value: Forge's {@code (int)(absorb / 25 < 1 ? 1 : absorb / 25)} on the scaled absorbed amount. */
    public static int wear(float damage, int armor) {
        double points = damage * SCALE * ratio(armor) / SCALE;
        return (int) (points < 1 ? 1 : points);
    }

    /** The damage after the worn wards of the entity; the pieces are worn here. Not magic, nothing worn or no damage: unchanged. */
    public static float apply(LivingEntity entity, DamageSource source, float damage) {
        if (damage <= 0 || !source.is(MAGIC_DAMAGE)) return damage;
        int[] armors = new int[ARMOR.length];
        int count = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof MagicWardGear gear && gear.wardArmor(stack) > 0) armors[count++] = gear.wardArmor(stack);
        }
        if (count == 0) return damage;
        int[] worn = java.util.Arrays.copyOf(armors, count);
        float after = reduce(damage, worn);
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof MagicWardGear gear && gear.wardArmor(stack) > 0) {
                stack.hurtAndBreak(wear(damage, gear.wardArmor(stack)), entity, slot);
            }
        }
        return after;
    }
}
