package thaumcraft.common.items.wands;

import java.util.List;

/** Bounded TC4 material catalogue. Original ConfigItems:302-314; staves/other rods/caps remain absent. */
public final class WandMaterials {
    public record Rod(String tag, int capacity, int craftCost) {
        public String research() { return "ROD_" + tag; }
    }

    public record Cap(String tag, float modifier, int craftCost) {
        public String research() { return "CAP_" + tag; }
    }

    // Capacities are centivis, like WandData and the native wand's addRealVis path.
    public static final Rod WOOD = new Rod("wood", 2500, 1);
    public static final Rod GREATWOOD = new Rod("greatwood", 5000, 3);
    public static final Rod SILVERWOOD = new Rod("silverwood", 10000, 9);
    public static final Cap IRON = new Cap("iron", 1.1F, 1);
    public static final Cap GOLD = new Cap("gold", 1.0F, 3);
    public static final Cap THAUMIUM = new Cap("thaumium", .9F, 6);
    public static final List<Rod> RODS = List.of(WOOD, GREATWOOD, SILVERWOOD);
    public static final List<Cap> CAPS = List.of(IRON, GOLD, THAUMIUM);

    private WandMaterials() {}

    public static Rod rod(String tag) {
        if (WOOD.tag().equals(tag)) return WOOD;
        if (GREATWOOD.tag().equals(tag)) return GREATWOOD;
        if (SILVERWOOD.tag().equals(tag)) return SILVERWOOD;
        throw new IllegalArgumentException("Unsupported wand rod: " + tag);
    }

    public static Cap cap(String tag) {
        if (IRON.tag().equals(tag)) return IRON;
        if (GOLD.tag().equals(tag)) return GOLD;
        if (THAUMIUM.tag().equals(tag)) return THAUMIUM;
        throw new IllegalArgumentException("Unsupported wand cap: " + tag);
    }

    public static boolean supportsRod(String tag) { return RODS.stream().anyMatch(rod -> rod.tag().equals(tag)); }
    public static boolean supportsCap(String tag) { return CAPS.stream().anyMatch(cap -> cap.tag().equals(tag)); }
    public static int capacity(String rod) { return rod(rod).capacity(); }
    public static int rodCraftCost(String rod) { return rod(rod).craftCost(); }
    public static int capCraftCost(String cap) { return cap(cap).craftCost(); }
    public static float capModifier(String cap) { return cap(cap).modifier(); }
    public static int assemblyCost(String rod, String cap) { return rodCraftCost(rod) * capCraftCost(cap); }
}
