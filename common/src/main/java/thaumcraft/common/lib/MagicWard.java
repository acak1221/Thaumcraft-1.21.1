package thaumcraft.common.lib;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.armor.MagicWardGear;

/**
 * The first pass of the original {@code ISpecialArmor} armor (void robes, Thaumium Fortress), run by Forge's
 * {@code ArmorProperties.ApplyArmor}: the damage is scaled by 25, every worn ward piece (priority 1) absorbs its share of it
 * ({@code armor / 35} for a magic source, {@code armor / 20} for fire or an explosion where the piece guards against those, times the
 * piece's factor), the total share comes off the damage in one step and each piece is worn {@code max(1, absorbed / 25)} points
 * (truncated). A source is magic when it is in {@link #MAGIC_DAMAGE} (the original {@code isMagicDamage}: potions and instant harm, the
 * taint, indirect magic; thorns, magic in 1.7, is left to the vanilla armor value, which already counts), fire or explosive when it is
 * in the vanilla fire and explosion tags.
 */
public final class MagicWard {
    public static final TagKey<DamageType> MAGIC_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "is_magic"));
    /** Original getProperties: ratio = damageReduceAmount / 35 for a magic source. */
    public static final double DIVISOR = 35.0;
    /** The Fortress armor: ratio = damageReduceAmount / 20 for fire and explosions. */
    public static final double FIRE_DIVISOR = 20.0;
    private static final double SCALE = 25.0;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** What the original asked a ward piece about a source: magic first, then fire or explosion, anything else is the ordinary armor. */
    public enum Kind { NONE, MAGIC, FIRE }

    private MagicWard() {}

    public static Kind kind(DamageSource source) {
        if (source.is(MAGIC_DAMAGE)) return Kind.MAGIC;
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) return Kind.FIRE;
        return Kind.NONE;
    }

    /** The share one piece of that armor value absorbs of magic damage. */
    public static double ratio(int armor) { return armor / DIVISOR; }

    /** The share one piece of that armor value absorbs of fire and explosions where it guards against them. */
    public static double fireRatio(int armor) { return armor / FIRE_DIVISOR; }

    /** Forge ApplyArmor, one priority group: the damage left after the wards of the given armor values (a total above 1 is not reachable by robes: 16 / 35). */
    public static float reduce(float damage, int... armors) {
        double[] ratios = new double[armors.length];
        for (int index = 0; index < armors.length; index++) ratios[index] = ratio(armors[index]);
        return reduceByRatios(damage, ratios);
    }

    /** The same for shares already worked out; a total above 1 is the whole damage (Forge divides the group down to exactly 1). */
    public static float reduceByRatios(float damage, double... ratios) {
        double total = 0;
        for (double ratio : ratios) total += ratio;
        total = Math.min(total, 1.0);
        double scaled = damage * SCALE;
        scaled -= scaled * total;
        return (float) (scaled / SCALE);
    }

    /** Points of wear for a piece of that armor value: Forge's {@code (int)(absorb / 25 < 1 ? 1 : absorb / 25)} on the scaled absorbed amount. */
    public static int wear(float damage, int armor) { return wearByRatio(damage, ratio(armor)); }

    public static int wearByRatio(float damage, double ratio) {
        double points = damage * SCALE * ratio / SCALE;
        return (int) (points < 1 ? 1 : points);
    }

    private static double share(MagicWardGear gear, ItemStack stack, LivingEntity entity, Kind kind) {
        int armor = gear.wardArmor(stack);
        if (armor <= 0) return 0;
        return switch (kind) {
            case MAGIC -> ratio(armor) * gear.wardFactor(stack, entity);
            case FIRE -> gear.wardsFire() ? fireRatio(armor) * gear.wardFactor(stack, entity) : 0;
            default -> 0;
        };
    }

    /** The damage after the worn wards of the entity; the pieces are worn here. Neither magic nor fire, nothing worn or no damage: unchanged. */
    public static float apply(LivingEntity entity, DamageSource source, float damage) {
        Kind kind = kind(source);
        if (damage <= 0 || kind == Kind.NONE) return damage;
        double[] ratios = new double[ARMOR.length];
        int count = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof MagicWardGear gear && share(gear, stack, entity, kind) > 0) ratios[count++] = share(gear, stack, entity, kind);
        }
        if (count == 0) return damage;
        float after = reduceByRatios(damage, java.util.Arrays.copyOf(ratios, count));
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof MagicWardGear gear && share(gear, stack, entity, kind) > 0) {
                stack.hurtAndBreak(wearByRatio(damage, share(gear, stack, entity, kind)), entity, slot);
            }
        }
        return after;
    }

    /**
     * What to add to the armor value that vanilla's own formula sees for this source. A piece that took a share in the first pass was not in
     * the original's ordinary pass, so its armor is taken out; for ordinary damage a piece with a factor above 1 (the Fortress set bonus)
     * counts that much more. Vanilla's formula is not the original's linear one, only the armor it is given follows the original.
     */
    public static float armorDelta(LivingEntity entity, DamageSource source) {
        Kind kind = kind(source);
        float delta = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof MagicWardGear gear) || gear.wardArmor(stack) <= 0) continue;
            if (share(gear, stack, entity, kind) > 0) delta -= gear.wardArmor(stack);
            else if (kind == Kind.NONE) delta += (float) (gear.wardArmor(stack) * (gear.wardFactor(stack, entity) - 1.0));
        }
        return delta;
    }
}
