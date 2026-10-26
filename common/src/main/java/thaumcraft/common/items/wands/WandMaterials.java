package thaumcraft.common.items.wands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;

/**
 * Bounded TC4 material catalogue. Original ConfigItems:292-313: iron/gold/thaumium caps, the wooden, greatwood, silverwood and the
 * six elemental rods, and the nine staff rods ({@code StaffRod}: tag {@code <rod>_staff}, a bigger capacity and price, the
 * {@code isStaff} behaviour), and the void cap (the copper and silver caps exist only when another mod supplies those metals). An elemental rod carries a primal aspect (original
 * {@code WandRodPrimalOnUpdate}) that it slowly refills in the wand it sits in, the primal staff all six; see {@link WandRodPassive}.
 *
 * <p><b>Add-on parts.</b> The original wand read every part from the public registries {@link WandRod#rods} and
 * {@link WandCap#caps} (original {@code ItemWandCasting.getRod/getCap}: {@code WandRod.rods.get(nbt "rod")}), so a part an
 * add-on registered there was a wand part like any other. Here the native catalogue keeps its constants, and a tag that is
 * not native is looked up in those registries <em>on every call</em> ({@link #rod(String)}, {@link #cap(String)}): an add-on may
 * register at any time of its initialisation, before or after Thaumcraft, on either loader, and nothing is frozen at
 * Thaumcraft's own initialisation. The answer for an add-on part is a fresh {@link Rod} / {@link Cap} built from the live
 * registry object ({@link Rod#addon()} / {@link Cap#addon()} points at it), so later setter calls of the add-on are seen.</p>
 *
 * <p>Native tags always resolve to the native constants, even when a registry entry of the same tag was replaced: the native
 * values stay bit-identical (a deliberate deviation from the original, which let a re-registration replace a native part).</p>
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
    /** The largest amount a wand of the NATIVE catalogue can hold: the primal staff as a sceptre, 375 whole vis. */
    public static final int MAX_CAPACITY = 37500;
    /**
     * The largest whole-vis capacity an add-on rod may declare: the sceptre capacity in centivis
     * ({@code capacity * 150}) must still be an {@code int}. A larger or negative declaration is clamped.
     */
    public static final int MAX_ADDON_CAPACITY = Integer.MAX_VALUE / 15000;
    /**
     * The largest craft cost of an assembled wand: the original stored it as the item metadata, a short in NBT
     * (ArcaneWandRecipe:59-61, ArcaneSceptreRecipe:64-66). Add-on parts may cost more than the native maximum of 432.
     */
    public static final int MAX_ASSEMBLY_COST = Short.MAX_VALUE;

    /**
     * One wand rod: the tag, the capacity in centivis, the craft cost, the passive aspect and the rune flag.
     *
     * @param addon the live add-on registration this rod was resolved from, or null for a rod of the native catalogue
     */
    public record Rod(String tag, int capacity, int craftCost, String passive, boolean runes, WandRod addon) {
        public Rod(String tag, int capacity, int craftCost) { this(tag, capacity, craftCost, null, false, null); }
        public Rod(String tag, int capacity, int craftCost, String passive) { this(tag, capacity, craftCost, passive, false, null); }
        public Rod(String tag, int capacity, int craftCost, String passive, boolean runes) { this(tag, capacity, craftCost, passive, runes, null); }

        /** Original {@code WandRod.getResearch()}: {@code ROD_<tag>}; an add-on's own override of it is honoured. */
        public String research() { return addon != null ? addon.getResearch() : "ROD_" + tag; }
        /** Original {@code rod instanceof StaffRod}; a native staff is a {@code <rod>_staff} tag. */
        public boolean staff() { return addon != null ? addon instanceof StaffRod : tag.endsWith(STAFF); }
        /**
         * The tag of the plain rod a staff is made from (the name and texture of a staff use it); the tag itself for a wand rod.
         *
         * <p>For an add-on rod this is the original name rule (ItemWandCasting:136-137): the part of the tag before the first
         * {@code _staff}.</p>
         */
        public String baseTag() {
            if (addon != null) {
                int staff = tag.indexOf(STAFF);
                return staff >= 0 ? tag.substring(0, staff) : tag;
            }
            return staff() ? tag.substring(0, tag.length() - STAFF.length()) : tag;
        }
        /** Original {@code StaffRod.hasRunes}: only the primal staff carries the runes. */
        public boolean hasRunes() { return runes; }
        /** True for a rod an add-on registered through {@link WandRod}. */
        public boolean isAddon() { return addon != null; }
        /** Original {@code WandRod.getOnUpdate()} of an add-on rod; native rods refill through {@link WandRodPassive} instead. */
        public IWandRodOnUpdate onUpdate() { return addon == null ? null : addon.getOnUpdate(); }
        /** Original {@code WandRod.isGlowing()} of an add-on rod (ModelWand:87-101 lights the rod). */
        public boolean glowing() { return addon != null && addon.isGlowing(); }
        /** The model sheet of an add-on rod, or null for a native rod (whose sheet is {@code wand_rod_<baseTag>.png}). */
        public ResourceLocation texture() { return addon == null ? null : addon.getTexture(); }

        static Rod of(String tag, WandRod rod) {
            int whole = Math.max(0, Math.min(rod.getCapacity(), MAX_ADDON_CAPACITY));
            return new Rod(tag, whole * 100, Math.max(0, rod.getCraftCost()), null,
                    rod instanceof StaffRod staff && staff.hasRunes(), rod);
        }
    }

    /**
     * One original wand cap: the tag, the base cost modifier, the craft cost and the special aspects with their modifier.
     *
     * <p>Original WandCap: the four ordinary caps have no special aspects, the copper cap (ConfigItems:331) has order and entropy
     * with the special modifier one, and the silver cap (ConfigItems:334) has air, earth, fire and water with the special
     * modifier 0.95.
     *
     * @param addon the live add-on registration this cap was resolved from, or null for a cap of the native catalogue
     */
    public record Cap(String tag, float modifier, int craftCost, List<String> specialAspects, float specialModifier, WandCap addon) {
        public Cap(String tag, float modifier, int craftCost) { this(tag, modifier, craftCost, List.of(), 0F, null); }
        public Cap(String tag, float modifier, int craftCost, List<String> specialAspects, float specialModifier) {
            this(tag, modifier, craftCost, specialAspects, specialModifier, null);
        }
        /** Original {@code WandCap.getResearch()}: {@code CAP_<tag>}; an add-on's own override of it is honoured. */
        public String research() { return addon != null ? addon.getResearch() : "CAP_" + tag; }
        /**
         * Original ItemWandCasting.getConsumptionModifier:243-248: the cost modifier of one aspect, the special one when the cap
         * lists the aspect, the base one otherwise.
         */
        public float modifierFor(String aspectTag) {
            return aspectTag != null && specialAspects.contains(aspectTag) ? specialModifier : modifier;
        }
        /** True for a cap an add-on registered through {@link WandCap}. */
        public boolean isAddon() { return addon != null; }
        /** The model sheet of an add-on cap, or null for a native cap (whose sheet is {@code wand_cap_<tag>.png}). */
        public ResourceLocation texture() { return addon == null ? null : addon.getTexture(); }

        static Cap of(String tag, WandCap cap) {
            List<String> special = new ArrayList<>();
            List<Aspect> aspects = cap.getSpecialCostModifierAspects();
            if (aspects != null) for (Aspect aspect : aspects) if (aspect != null && aspect.getTag() != null) special.add(aspect.getTag());
            return new Cap(tag, cap.getBaseCostModifier(), Math.max(0, cap.getCraftCost()), List.copyOf(special),
                    cap.getSpecialCostModifier(), cap);
        }
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
    /**
     * Original {@code WandCap("copper", 1.1f, Arrays.asList(Aspect.ORDER, Aspect.ENTROPY), 1.0f, ..., 2)}
     * (ConfigItems:331): the base cost modifier is the one of the iron cap, the special aspects are order
     * and entropy with the special modifier one, and the craft cost is two. The original registered the cap
     * only when another mod supplied a copper ingot; copper is vanilla in 1.21.1, so the cap is carried.
     */
    public static final Cap COPPER = new Cap("copper", 1.1F, 2, List.of("ordo", "perditio"), 1.0F);
    /** Original {@code WandCap("void", 0.8f, ..., 9)} (ConfigItems:297): the dearest cap and the cheapest wand to use. */
    public static final Cap VOID = new Cap("void", .8F, 9);
    /**
     * The NATIVE rods only. The order is part of the client model selector: appending keeps the earlier rods' values.
     * Add-on rods are not listed here; see {@link #allRods()}.
     */
    public static final List<Rod> RODS = List.of(WOOD, GREATWOOD, SILVERWOOD, OBSIDIAN, BLAZE, ICE, QUARTZ, BONE, REED,
            GREATWOOD_STAFF, OBSIDIAN_STAFF, BLAZE_STAFF, ICE_STAFF, QUARTZ_STAFF, BONE_STAFF, REED_STAFF, SILVERWOOD_STAFF, PRIMAL_STAFF);
    /** The NATIVE caps only, in the order of the client model selector; see {@link #allCaps()} for add-on caps too. */
    public static final List<Cap> CAPS = List.of(IRON, GOLD, THAUMIUM, VOID, COPPER);

    private WandMaterials() {}

    /** The native rod of this tag, or null. */
    public static Rod nativeRod(String tag) {
        for (Rod rod : RODS) if (rod.tag().equals(tag)) return rod;
        return null;
    }

    /** The native cap of this tag, or null. */
    public static Cap nativeCap(String tag) {
        for (Cap cap : CAPS) if (cap.tag().equals(tag)) return cap;
        return null;
    }

    /**
     * The add-on rod registered under this tag in {@link WandRod#rods}, resolved now, or null when there is none.
     * A native tag never resolves here, and neither does a native API view.
     */
    public static Rod addonRod(String tag) {
        if (tag == null || nativeRod(tag) != null) return null;
        Map<String, WandRod> registry = WandRod.rods;
        WandRod rod = registry == null ? null : registry.get(tag);
        return rod == null || rod.nativeRod() != null ? null : Rod.of(tag, rod);
    }

    /** The add-on cap registered under this tag in {@link WandCap#caps}, resolved now, or null when there is none. */
    public static Cap addonCap(String tag) {
        if (tag == null || nativeCap(tag) != null) return null;
        Map<String, WandCap> registry = WandCap.caps;
        WandCap cap = registry == null ? null : registry.get(tag);
        return cap == null || cap.nativeCap() != null ? null : Cap.of(tag, cap);
    }

    /** The native rod of the tag, else the add-on rod registered under it now; an unknown tag throws. */
    public static Rod rod(String tag) {
        Rod rod = nativeRod(tag);
        if (rod == null) rod = addonRod(tag);
        if (rod == null) throw new IllegalArgumentException("Unsupported wand rod: " + tag);
        return rod;
    }

    /** The native cap of the tag, else the add-on cap registered under it now; an unknown tag throws. */
    public static Cap cap(String tag) {
        Cap cap = nativeCap(tag);
        if (cap == null) cap = addonCap(tag);
        if (cap == null) throw new IllegalArgumentException("Unsupported wand cap: " + tag);
        return cap;
    }

    public static boolean supportsRod(String tag) { return nativeRod(tag) != null || addonRod(tag) != null; }
    public static boolean supportsCap(String tag) { return nativeCap(tag) != null || addonCap(tag) != null; }

    /** The native rods, then every add-on rod registered now, in registry order. */
    public static List<Rod> allRods() {
        List<Rod> rods = new ArrayList<>(RODS);
        Map<String, WandRod> registry = WandRod.rods;
        if (registry != null) for (String tag : List.copyOf(registry.keySet())) {
            Rod addon = addonRod(tag);
            if (addon != null) rods.add(addon);
        }
        return Collections.unmodifiableList(rods);
    }

    /** The native caps, then every add-on cap registered now, in registry order. */
    public static List<Cap> allCaps() {
        List<Cap> caps = new ArrayList<>(CAPS);
        Map<String, WandCap> registry = WandCap.caps;
        if (registry != null) for (String tag : List.copyOf(registry.keySet())) {
            Cap addon = addonCap(tag);
            if (addon != null) caps.add(addon);
        }
        return Collections.unmodifiableList(caps);
    }

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
     * The rod capacities are whole vis, so the original multiplication is exact in centivis.</p>
     */
    public static int capacity(String rod, boolean sceptre) {
        return rod(rod).capacity() * (sceptre ? SCEPTRE_CAPACITY_FACTOR : 100) / 100;
    }
    public static int rodCraftCost(String rod) { return rod(rod).craftCost(); }
    public static int capCraftCost(String cap) { return cap(cap).craftCost(); }
    public static float capModifier(String cap) { return cap(cap).modifier(); }
    public static int assemblyCost(String rod, String cap) { return assemblyCost(rod(rod), cap(cap)); }
    /** Original ArcaneWandRecipe:59 and :104: the cap cost times the rod cost, per primal and as the item metadata. */
    public static int assemblyCost(Rod rod, Cap cap) {
        return (int) Math.min((long) rod.craftCost() * cap.craftCost(), MAX_ASSEMBLY_COST);
    }
}
