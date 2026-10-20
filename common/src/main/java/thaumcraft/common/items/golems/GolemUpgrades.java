package thaumcraft.common.items.golems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;

/**
 * Pure slot rules, serialization and effect arithmetic of the golem upgrades, without
 * any game object. Original EntityGolemBase:188-196 (slot array), 255 (damage), 372-383
 * (speed), 530-558 (counting), 991-1006 (installation), 1139-1141 (thorns),
 * ItemGolemBell:292-316 (dismantling), ItemGolemPlacer:202-231 (placer array).
 */
public final class GolemUpgrades {
    /** Ordinary materials hold 1 or 2; an advanced golem holds one more (original E:149). */
    public static final int MAX_SLOTS = 3;
    /** Original E:1148: the golem's melee/attack delay in ticks, shortened by 2 when advanced. */
    public static final int BASE_ATTACK_SPEED = 20;
    /** Original installation refuses a third upgrade of one type. */
    public static final int MAX_SAME_KIND = 2;
    private static final int BITS = 4;

    private GolemUpgrades() {}

    public static int[] empty(int slots) {
        int[] result = new int[slots];
        Arrays.fill(result, GolemUpgrade.EMPTY);
        return result;
    }

    public static int count(int[] slots, int kind) {
        int amount = 0;
        for (int slot : slots) if (slot == kind) amount++;
        return amount;
    }

    public static int count(int[] slots, GolemUpgrade kind) { return count(slots, kind.id); }

    /**
     * First free slot for a supported type, or -1. The original loop skips a slot that is
     * occupied OR whenever two upgrades of the type are already fitted; no core check.
     */
    public static int installSlot(int[] slots, int kind) {
        if (!GolemUpgrade.isSupported(kind)) return -1;
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot] != GolemUpgrade.EMPTY || count(slots, kind) >= MAX_SAME_KIND) continue;
            return slot;
        }
        return -1;
    }

    /** An empty slot or a supported type. Unsupported-but-original ids are data this port cannot honour. */
    public static boolean validSlot(int value) { return value == GolemUpgrade.EMPTY || GolemUpgrade.isSupported(value); }

    /** Canonical placer form: slot order is kept, only trailing empty slots are dropped. */
    public static List<Integer> canonical(List<Integer> slots) {
        int end = slots.size();
        while (end > 0 && slots.get(end - 1) == GolemUpgrade.EMPTY) end--;
        return List.copyOf(slots.subList(0, end));
    }

    public static List<Integer> toList(int[] slots) {
        List<Integer> list = new ArrayList<>(slots.length);
        for (int slot : slots) list.add(slot);
        return canonical(list);
    }

    /**
     * Pads a canonical list with empties to the material's slot count. Null when the list holds
     * an unsupported id or more slots than the material has: never truncated like the original.
     */
    public static int[] fit(List<Integer> slots, int slotCount) {
        if (slots == null || slotCount < 0 || slotCount > MAX_SLOTS || slots.size() > slotCount) return null;
        int[] result = empty(slotCount);
        for (int slot = 0; slot < slots.size(); slot++) {
            Integer value = slots.get(slot);
            if (value == null || !validSlot(value)) return null;
            result[slot] = value;
        }
        return result;
    }

    /** Entity NBT {@code upgrades}: a byte array, -1 for an empty slot, one byte per material slot. */
    public static byte[] toBytes(int[] slots) {
        byte[] bytes = new byte[slots.length];
        for (int slot = 0; slot < slots.length; slot++) bytes[slot] = (byte) slots[slot];
        return bytes;
    }

    /** Shorter arrays are padded like the original; unsupported ids and over-long arrays return null, not data loss. */
    public static int[] fromBytes(byte[] raw, int slotCount) {
        if (raw == null || slotCount < 0 || slotCount > MAX_SLOTS || raw.length > slotCount) return null;
        int[] result = empty(slotCount);
        for (int slot = 0; slot < raw.length; slot++) {
            if (!validSlot(raw[slot])) return null;
            result[slot] = raw[slot];
        }
        return result;
    }

    /** Synchronised form: 4 bits per slot, 0 = empty, otherwise type + 1. Replaces the original's hex string. */
    public static int pack(int[] slots) {
        int packed = 0;
        for (int slot = 0; slot < slots.length && slot < MAX_SLOTS; slot++) {
            packed |= (slots[slot] + 1) << (slot * BITS);
        }
        return packed;
    }

    public static int[] unpack(int packed, int slotCount) {
        int count = Math.max(0, Math.min(MAX_SLOTS, slotCount));
        int[] result = new int[count];
        for (int slot = 0; slot < count; slot++) result[slot] = ((packed >> (slot * BITS)) & 0xF) - 1;
        return result;
    }

    /** Original E:376: a float product on the material speed. */
    public static float speed(float materialSpeed, int air) { return speed(materialSpeed, air, false); }

    /** Original E:376-383: Air first, then {@code ×1.1f} for an advanced golem (float products, in this order). */
    public static float speed(float materialSpeed, int air, boolean advanced) { return speed(materialSpeed, air, advanced, ""); }

    /**
     * Original E:329-335: the material speed, then the bowtie's 1.1 and the plating's 0.88 (E:329-331), then Air, then the
     * advanced golem's 1.1 (float products, in this order). An empty decoration multiplies by exactly 1.0f, so the result
     * is bit-for-bit the undecorated one.
     */
    public static float speed(float materialSpeed, int air, boolean advanced, String decoration) {
        float speed = materialSpeed * GolemDecorations.speedFactor(decoration);
        speed *= 1.0f + (float) air * 0.15f;
        if (advanced) speed *= 1.1f;
        return speed;
    }

    /** Upgrade slots of a material: its own plus the advanced golem's extra one (original E:149, 625). */
    public static int slotCount(int materialSlots, boolean advanced) { return materialSlots + (advanced ? 1 : 0); }

    /** Original E:1148: {@code 20 - (advanced ? 2 : 0)} ticks between attacks. */
    public static int attackSpeed(boolean advanced) { return BASE_ATTACK_SPEED - (advanced ? 2 : 0); }

    /** Original E:851: Earth adds one strength each. */
    public static int strength(int materialStrength, int earth) { return materialStrength + earth; }

    /**
     * Original E:255 counts Earth twice: {@code 2 + getGolemStrength() + Earth}, and getGolemStrength
     * already includes Earth. Preserved, not "fixed".
     */
    public static float meleeDamage(int materialStrength, int earth) { return 2 + strength(materialStrength, earth) + earth; }

    /** Original E:210-216: the mace adds two on top of the Earth-doubled strength damage. */
    public static float meleeDamage(int materialStrength, int earth, String decoration) {
        return meleeDamage(materialStrength, earth) + GolemDecorations.meleeDamageBonus(decoration);
    }

    /**
     * Original E:355-362: {@code carry + min(16, max(4, carry)) * Earth}, with {@code carry} the material's own
     * capacity (Straw 1 -> 5, Tallow 8 -> 16/24, Iron 32 -> 48, Thaumium 32 -> 48/64).
     */
    public static int carry(int materialCarry, int earth) {
        return materialCarry + Math.min(16, Math.max(4, materialCarry)) * earth;
    }

    /** Original E:364-366: {@code floor(sqrt(carry)) * 1000} millibuckets of water. */
    public static int fluidLimitMb(int carry) {
        return (int) Math.floor(Math.sqrt(carry)) * 1000;
    }

    /**
     * Original E:460-469 without the decoration and advanced additions: {@code 16 + 4n},
     * a float, with {@code n} the number of Water upgrades. Every work radius and
     * home-distance check of the golem is derived from this one value.
     */
    public static float range(int water) { return range(water, false); }

    /**
     * Original E:460-469 with the advanced golem's addition and without the spectacles' (see the overload): after {@code 16 + 4n}
     * an advanced golem adds {@code max(range * 0.2f, 2.0f)} (float arithmetic, 16 -> 19.2, 24 -> 28.8).
     */
    public static float range(int water, boolean advanced) { return range(water, advanced, ""); }

    /**
     * Original E:417-427 complete: {@code 16 + 4n}, plus the spectacles' {@code max(range * 0.1f, 1.0f)}, then the advanced
     * golem's {@code max(range * 0.2f, 2.0f)} on the already enlarged value (float arithmetic, in this order).
     */
    public static float range(int water, boolean advanced, String decoration) {
        float range = 16 + water * 4;
        range += GolemDecorations.rangeBonus(range, decoration);
        if (advanced) range += Math.max(range * 0.2f, 2.0f);
        return range;
    }

    /**
     * Work radius of a goal, computed in the original from the live range: {@code ceil(range / divisor)}
     * with divisor 4 for Harvest (AIHarvestCrops:84) and 2 for Fishing (AIFish:71). Lumber's 3 is
     * {@code LumberGeometry.radius}. Float arithmetic like the original MathHelper.ceiling_float_int.
     */
    public static int workRadius(float range, int divisor) { return (int) Math.ceil(range / (float) divisor); }

    /**
     * Original E:1120-1122: after a successful melee hit the target burns for {@code 4j} seconds, with
     * {@code j} the Fire Aspect enchantment level plus the number of Fire upgrades. A golem here carries
     * no enchanted gear, so {@code j} is the Fire count; zero means the target is not set alight.
     */
    public static int igniteSeconds(int fire) { return fire <= 0 ? 0 : fire * 4; }

    /** The three result pools of a Fishing golem's successful cast (original AIFish:getFishingResult). */
    public enum FishCategory { JUNK, TREASURE, FISH }

    /** Original AIFish:183: base junk chance {@code 0.1 - 0.025 n} for n Entropy upgrades (float); the pond only lowers it. */
    public static float fishJunkBase(int entropy) { return 0.1f - (float) entropy * 0.025f; }

    /** Original AIFish:184: base treasure chance {@code 0.05 + 0.0125 n} for n Order upgrades (float); the pond only raises it. */
    public static float fishTreasureBase(int order) { return 0.05f + (float) order * 0.0125f; }

    /**
     * Original AIFish:187-193: one uniform roll picks junk below {@code junk}, treasure in the next {@code treasure}
     * and fish above (float subtraction like {@code f -= f1}).
     */
    public static FishCategory fishCategory(float roll, float junk, float treasure) {
        if (roll < junk) return FishCategory.JUNK;
        return roll - junk < treasure ? FishCategory.TREASURE : FishCategory.FISH;
    }

    /**
     * Original AIFish:118-120: one catch per success, a second one when {@code nextInt(10) < Air}. The draw happens
     * only with at least one Air upgrade, so a golem without Air consumes no extra random number.
     */
    public static int fishCatches(int air, IntUnaryOperator nextInt) {
        return air > 0 && nextInt.applyAsInt(10) < air ? 2 : 1;
    }

    /** Original E:1140: {@code 2n + nextInt(2n)}; zero when there is no Entropy upgrade. */
    public static int thornsDamage(int entropy, IntUnaryOperator nextInt) {
        if (entropy <= 0) return 0;
        return entropy * 2 + nextInt.applyAsInt(2 * entropy);
    }

    /**
     * Bell dismantling (ItemGolemBell:302-305): a coin is tossed only for an occupied slot, in slot order,
     * and a returned upgrade comes back as its item.
     */
    public static List<GolemUpgrade> dismantleReturns(int[] slots, BooleanSupplier coin) {
        List<GolemUpgrade> returned = new ArrayList<>();
        for (int slot : slots) {
            if (slot <= GolemUpgrade.EMPTY || !coin.getAsBoolean()) continue;
            GolemUpgrade upgrade = GolemUpgrade.byId(slot);
            if (upgrade != null) returned.add(upgrade);
        }
        return returned;
    }
}
