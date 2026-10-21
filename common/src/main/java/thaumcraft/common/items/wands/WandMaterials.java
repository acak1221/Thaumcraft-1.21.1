package thaumcraft.common.items.wands;

import java.util.List;

/**
 * Bounded TC4 material catalogue. Original ConfigItems:292-313: iron/gold/thaumium caps, the wooden, greatwood, silverwood and the
 * six elemental rods, and the nine staff rods ({@code StaffRod}: tag {@code <rod>_staff}, a bigger capacity and price, the
 * {@code isStaff} behaviour), and the void cap (the copper and silver caps exist only when another mod supplies those metals). An elemental rod carries a primal aspect (original
 * {@code WandRodPrimalOnUpdate}) that it slowly refills in the wand it sits in, the primal staff all six; see {@link WandRodPassive}.
 */
public final class WandMaterials {
    /** The {@link Rod#passive} of the primal staff: any of the six primals, picked at random (original {@code WandRodPrimalOnUpdate()}). */
    public static final String ANY_PRIMAL = "*";
    private static final String STAFF = "_staff";
    /**
     * Original ItemWandCasting:119-121: the capacity of a wand is the capacity of its rod, and the capacity
     * of a SCEPTRE is one and a half times the capacity of its rod.
     */
    public static final int SCEPTRE_CAPACITY_FACTOR = 150;
    /** The largest amount a wand can hold: the primal staff as a sceptre, 375 whole vis. */
    public static final int MAX_CAPACITY = 37500;

    public record Rod(String tag, int capacity, int craftCost, String passive, boolean runes) {
        public Rod(String tag, int capacity, int craftCost) { this(tag, capacity, craftCost, null, false); }
        public Rod(String tag, int capacity, int craftCost, String passive) { this(tag, capacity, craftCost, passive, false); }
        public String research() { return "ROD_" + tag; }
        /** Original {@code rod instanceof StaffRod}. */
        public boolean staff() { return tag.endsWith(STAFF); }
        /** The tag of the plain rod a staff is made from (the name and texture of a staff use it); the tag itself for a wand rod. */
        public String baseTag() { return staff() ? tag.substring(0, tag.length() - STAFF.length()) : tag; }
        /** Original {@code StaffRod.hasRunes}: only the primal staff carries the runes. */
        public boolean hasRunes() { return runes; }
    }

    public record Cap(String tag, float modifier, int craftCost) {
        public String research() { return "CAP_" + tag; }
    }

    // Capacities are centivis, like WandData and the native wand's addRealVis path.
    public static final Rod WOOD = new Rod("wood", 2500, 1);
    public static final Rod GREATWOOD = new Rod("greatwood", 5000, 3);
    public static final Rod SILVERWOOD = new Rod("silverwood", 10000, 9);
    public static final Rod OBSIDIAN = new Rod("obsidian", 7500, 6, "terra");
    public static final Rod BLAZE = new Rod("blaze", 7500, 6, "ignis");
    public static final Rod ICE = new Rod("ice", 7500, 6, "aqua");
    public static final Rod QUARTZ = new Rod("quartz", 7500, 6, "ordo");
    public static final Rod BONE = new Rod("bone", 7500, 6, "perditio");
    public static final Rod REED = new Rod("reed", 7500, 6, "aer");
    // StaffRod: capacity 125 / 175 / 250 whole vis, price 8 / 14 / 24; the primal one 250 and 32 (ConfigItems:304-313).
    public static final Rod GREATWOOD_STAFF = new Rod("greatwood_staff", 12500, 8);
    public static final Rod OBSIDIAN_STAFF = new Rod("obsidian_staff", 17500, 14, "terra");
    public static final Rod BLAZE_STAFF = new Rod("blaze_staff", 17500, 14, "ignis");
    public static final Rod ICE_STAFF = new Rod("ice_staff", 17500, 14, "aqua");
    public static final Rod QUARTZ_STAFF = new Rod("quartz_staff", 17500, 14, "ordo");
    public static final Rod BONE_STAFF = new Rod("bone_staff", 17500, 14, "perditio");
    public static final Rod REED_STAFF = new Rod("reed_staff", 17500, 14, "aer");
    public static final Rod SILVERWOOD_STAFF = new Rod("silverwood_staff", 25000, 24);
    /** Original {@code STAFF_ROD_PRIMAL.setRunes(true)} (ConfigItems:325): the primal staff adds one focus potency. */
    public static final Rod PRIMAL_STAFF = new Rod("primal_staff", 25000, 32, ANY_PRIMAL, true);
    public static final Cap IRON = new Cap("iron", 1.1F, 1);
    public static final Cap GOLD = new Cap("gold", 1.0F, 3);
    public static final Cap THAUMIUM = new Cap("thaumium", .9F, 6);
    /** Original {@code WandCap("void", 0.8f, ..., 9)} (ConfigItems:297): the dearest cap and the cheapest wand to use. */
    public static final Cap VOID = new Cap("void", .8F, 9);
    /** The order is part of the client model selector: appending keeps the earlier rods' values. */
    public static final List<Rod> RODS = List.of(WOOD, GREATWOOD, SILVERWOOD, OBSIDIAN, BLAZE, ICE, QUARTZ, BONE, REED,
            GREATWOOD_STAFF, OBSIDIAN_STAFF, BLAZE_STAFF, ICE_STAFF, QUARTZ_STAFF, BONE_STAFF, REED_STAFF, SILVERWOOD_STAFF, PRIMAL_STAFF);
    public static final List<Cap> CAPS = List.of(IRON, GOLD, THAUMIUM, VOID);

    private WandMaterials() {}

    public static Rod rod(String tag) {
        for (Rod rod : RODS) if (rod.tag().equals(tag)) return rod;
        throw new IllegalArgumentException("Unsupported wand rod: " + tag);
    }

    public static Cap cap(String tag) {
        if (IRON.tag().equals(tag)) return IRON;
        if (GOLD.tag().equals(tag)) return GOLD;
        if (THAUMIUM.tag().equals(tag)) return THAUMIUM;
        if (VOID.tag().equals(tag)) return VOID;
        throw new IllegalArgumentException("Unsupported wand cap: " + tag);
    }

    public static boolean supportsRod(String tag) { return RODS.stream().anyMatch(rod -> rod.tag().equals(tag)); }
    public static boolean supportsCap(String tag) { return CAPS.stream().anyMatch(cap -> cap.tag().equals(tag)); }
    /**
     * Original {@code ItemWandCasting.getFocusPotency}: the potency upgrade of the
     * installed focus plus one when the wand carries a staff rod with runes.
     */
    public static int focusPotency(int upgradeLevel, Rod rod) { return upgradeLevel + (rod != null && rod.hasRunes() ? 1 : 0); }
    public static int capacity(String rod) { return rod(rod).capacity(); }
    /**
     * The capacity of a wand of this rod in centivis, and the capacity of the same wand as a sceptre.
     *
     * <p>Original ItemWandCasting:119-121: {@code getRod(stack).getCapacity() * (isSceptre(stack) ? 150 : 100)}.
     * The rod capacities are whole multiples of fifty vis, so the original multiplication is exact in centivis.</p>
     */
    public static int capacity(String rod, boolean sceptre) {
        return rod(rod).capacity() * (sceptre ? SCEPTRE_CAPACITY_FACTOR : 100) / 100;
    }
    public static int rodCraftCost(String rod) { return rod(rod).craftCost(); }
    public static int capCraftCost(String cap) { return cap(cap).craftCost(); }
    public static float capModifier(String cap) { return cap(cap).modifier(); }
    public static int assemblyCost(String rod, String cap) { return rodCraftCost(rod) * capCraftCost(cap); }
}
