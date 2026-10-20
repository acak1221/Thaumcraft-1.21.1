package thaumcraft.api.wands;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Bounded native catalogue. Stable TC4 IDs, not enum ordinals or enchantments.
 * Unimplemented specializations are deliberately absent, never paid placeholders. */
public final class FocusUpgradeType {
    public static final FocusUpgradeType potency = new FocusUpgradeType(0, "potency", Aspect.WEAPON);
    public static final FocusUpgradeType frugal = new FocusUpgradeType(1, "frugal", Aspect.HUNGER);
    public static final FocusUpgradeType treasure = new FocusUpgradeType(2, "treasure", Aspect.GREED);
    public static final FocusUpgradeType enlarge = new FocusUpgradeType(3, "enlarge", Aspect.TRAVEL);
    public static final FocusUpgradeType extend = new FocusUpgradeType(7, "extend", Aspect.EXCHANGE);
    private static final Map<Short, FocusUpgradeType> TYPES = catalogue(potency, frugal, treasure, enlarge, extend);

    public final short id;
    public final String name;
    public final String text;
    public final ResourceLocation icon;
    private final AspectList aspects;

    private FocusUpgradeType(int id, String key, Aspect aspect) {
        if (id < 0 || id > Short.MAX_VALUE) throw new IllegalArgumentException("Invalid focus upgrade ID");
        this.id = (short) id;
        name = "focus.upgrade." + key + ".name";
        text = "focus.upgrade." + key + ".text";
        icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/" + key + ".png");
        aspects = new AspectList().add(aspect, 1);
    }

    private static Map<Short, FocusUpgradeType> catalogue(FocusUpgradeType... entries) {
        Map<Short, FocusUpgradeType> result = new LinkedHashMap<>();
        for (FocusUpgradeType entry : entries) {
            if (entry == null || entry.id < 0 || result.putIfAbsent(entry.id, entry) != null)
                throw new IllegalArgumentException("Invalid or duplicate focus upgrade ID");
        }
        return Map.copyOf(result);
    }

    public AspectList getAspects() { return aspects.copy(); }
    public static FocusUpgradeType get(short id) { return TYPES.get(id); }
    public static FocusUpgradeType get(int id) { return id < 0 || id > Short.MAX_VALUE ? null : get((short) id); }
    @Override public boolean equals(Object other) { return this == other || other instanceof FocusUpgradeType type && id == type.id; }
    @Override public int hashCode() { return Short.hashCode(id); }
}
