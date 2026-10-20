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
    public static final FocusUpgradeType alchemistsfire = new FocusUpgradeType(4, "alchemistsfire", Aspect.ENERGY, Aspect.SLIME);
    public static final FocusUpgradeType alchemistsfrost = new FocusUpgradeType(5, "alchemistsfrost", Aspect.COLD, Aspect.TRAP);
    public static final FocusUpgradeType architect = new FocusUpgradeType(6, "architect", Aspect.CRAFT);
    public static final FocusUpgradeType extend = new FocusUpgradeType(7, "extend", Aspect.EXCHANGE);
    public static final FocusUpgradeType silktouch = new FocusUpgradeType(8, "silktouch", Aspect.GREED);
    public static final FocusUpgradeType fireball = new FocusUpgradeType(9, "fireball", Aspect.DARKNESS);
    public static final FocusUpgradeType firebeam = new FocusUpgradeType(10, "firebeam", Aspect.ENERGY, Aspect.AIR);
    public static final FocusUpgradeType scattershot = new FocusUpgradeType(11, "scattershot", Aspect.COLD, Aspect.WEAPON);
    public static final FocusUpgradeType iceboulder = new FocusUpgradeType(12, "iceboulder", Aspect.COLD, Aspect.CRYSTAL);
    public static final FocusUpgradeType batbombs = new FocusUpgradeType(13, "batbombs", Aspect.ENERGY, Aspect.TRAP);
    public static final FocusUpgradeType devilbats = new FocusUpgradeType(14, "devilbats", Aspect.ARMOR);
    public static final FocusUpgradeType nightshade = new FocusUpgradeType(15, "nightshade", Aspect.LIFE, Aspect.POISON, Aspect.MAGIC);
    public static final FocusUpgradeType chainlightning = new FocusUpgradeType(17, "chainlightning", Aspect.WEATHER);
    public static final FocusUpgradeType earthshock = new FocusUpgradeType(18, "earthshock", Aspect.WEATHER);
    public static final FocusUpgradeType vampirebats = new FocusUpgradeType(19, "vampirebats", Aspect.HUNGER, Aspect.LIFE);
    public static final FocusUpgradeType dowsing = new FocusUpgradeType(20, "dowsing", Aspect.MINE);
    private static final Map<Short, FocusUpgradeType> TYPES = catalogue(potency, frugal, treasure, enlarge,
            alchemistsfire, alchemistsfrost, architect, extend, silktouch, fireball, firebeam,
            scattershot, iceboulder, batbombs, devilbats, nightshade, chainlightning, earthshock, vampirebats, dowsing);

    public final short id;
    public final String name;
    public final String text;
    public final ResourceLocation icon;
    private final AspectList aspects;

    private FocusUpgradeType(int id, String key, Aspect... components) {
        if (id < 0 || id > Short.MAX_VALUE) throw new IllegalArgumentException("Invalid focus upgrade ID");
        this.id = (short) id;
        name = "focus.upgrade." + key + ".name";
        text = "focus.upgrade." + key + ".text";
        icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/" + key + ".png");
        aspects = new AspectList();
        for (Aspect aspect : components) aspects.add(aspect, 1);
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
