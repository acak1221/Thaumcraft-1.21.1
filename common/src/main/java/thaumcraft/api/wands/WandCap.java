package thaumcraft.api.wands;

import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * The original TC4 wand cap.
 *
 * <h2>Add-on contract</h2>
 * <p>An add-on registers a cap exactly like it did on 1.7.10, by constructing it; the constructor puts it into {@link #caps}
 * under its tag:</p>
 * <pre>{@code
 * WandCap alchemical = new WandCap("alchemical", 0.9F, List.of(Aspect.WATER), 0.8F, new ItemStack(MyItems.ALCHEMICAL_CAP), 7);
 * alchemical.setTexture(ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_cap_alchemical.png"));
 * }</pre>
 * <ul>
 *   <li><b>Discount</b> is the vis cost multiplier of the wand (1 is unchanged, 0.9 is a tenth cheaper); an aspect of the special list
 *       uses the special multiplier instead (original ItemWandCasting.getConsumptionModifier:243-248). The equipment discount, the
 *       frugal upgrade and the sceptre are subtracted from it as usual, never below a tenth.</li>
 *   <li><b>Craft cost</b> is the dimensionless assembly factor, multiplied with the rod's (see {@link WandRod}).</li>
 *   <li><b>Item</b> is the stack the arcane workbench accepts in the two (sceptre: three) cap cells, compared like the original.</li>
 *   <li><b>Research</b> is {@link #getResearch()}, {@code CAP_<tag>}; the add-on registers it itself.</li>
 *   <li><b>Texture</b> is an entity-style sheet in the layout of the original {@code ModelWand} cap (the 1.7.10 PNG works unchanged);
 *       the constructors derive {@code thaumcraft:textures/models/wand_cap_<tag>.png}, so an add-on calls {@link #setTexture}.</li>
 *   <li><b>Name</b>: the wand name uses the translation key {@code item.Wand.<tag>.cap}; the add-on ships it in its own lang file.</li>
 * </ul>
 * <p>Registration may happen at any time of mod initialisation; gameplay looks the registry up whenever it needs a cap, so later
 * setter calls are seen. The registry is a plain map: register during initialisation only.</p>
 *
 * <h2>Native caps</h2>
 * <p>The native caps of the port's catalogue are present in {@link #caps} as views ({@link #nativeCap()} is non-null) with the
 * original values. Gameplay reads native caps from the catalogue: changing a native view, or registering another cap under a
 * native tag, does not change the native cap (forced deviation that keeps the native values bit-identical).</p>
 */
public class WandCap {
    /** The original public registry; it stays mutable so an add-on may add its own cap. Native views are registered first. */
    public static LinkedHashMap<String, WandCap> caps = new LinkedHashMap<>();
    private String tag;
    private int craftCost;
    float baseCostModifier;
    List<Aspect> specialCostModifierAspects;
    float specialCostModifier;
    ResourceLocation texture;
    ItemStack item;
    private final thaumcraft.common.items.wands.WandMaterials.Cap nativeCap;
    /** The views registered below, kept even if an add-on replaces their registry entries. */
    private static final java.util.Map<String, WandCap> NATIVE_VIEWS = new java.util.HashMap<>();

    static {
        // Original ConfigItems:292-297 and :331-334 register the native caps during Thaumcraft's own start.
        for (var cap : thaumcraft.common.items.wands.WandMaterials.CAPS) NATIVE_VIEWS.put(cap.tag(), new WandCap(cap, null));
    }

    /** The cap gameplay uses for this tag: the native view of a native tag, else the registry entry (null when unknown). */
    static WandCap forGameplay(String tag) {
        WandCap view = NATIVE_VIEWS.get(tag);
        return view != null ? view : caps.get(tag);
    }

    /**
     * Registers (or replaces) a cap without special aspects; retains a nullable item.
     *
     * @param discount  the vis cost multiplier (1 means unchanged)
     * @param craftCost the dimensionless assembly factor
     */
    public WandCap(String tag, float discount, ItemStack item, int craftCost) {
        this.nativeCap = null;
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = null;
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + lowerTag(this.getTag()) + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    /**
     * Registers (or replaces) a cap whose special aspects use their own multiplier; retains the nullable item/aspect list.
     *
     * @param discount        the vis cost multiplier of every other aspect (1 means unchanged)
     * @param specialAspects  the aspects that use {@code discountSpecial}; may be null
     * @param discountSpecial the vis cost multiplier of the special aspects
     * @param craftCost       the dimensionless assembly factor
     */
    public WandCap(String tag, float discount, List<Aspect> specialAspects, float discountSpecial, ItemStack item, int craftCost) {
        this.nativeCap = null;
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = specialAspects;
        this.specialCostModifier = discountSpecial;
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + lowerTag(this.getTag()) + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    /**
     * A view of a cap of the native catalogue, registered under its tag.
     *
     * @param item the cap item, or null to resolve the native cap item when it is first asked for
     */
    public WandCap(thaumcraft.common.items.wands.WandMaterials.Cap cap, ItemStack item) {
        this.nativeCap = cap;
        this.setTag(cap.tag());
        this.baseCostModifier = cap.modifier();
        this.craftCost = cap.craftCost();
        if (!cap.specialAspects().isEmpty()) {
            List<Aspect> special = new java.util.ArrayList<>();
            for (String aspect : cap.specialAspects()) {
                Aspect resolved = Aspect.getAspect(aspect);
                if (resolved != null) special.add(resolved);
            }
            this.specialCostModifierAspects = special;
            this.specialCostModifier = cap.specialModifier();
        }
        this.item = item;
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + lowerTag(cap.tag()) + ".png");
        caps.put(cap.tag(), this);
    }

    /** The native cap behind this view, or null for an add-on cap. */
    public thaumcraft.common.items.wands.WandMaterials.Cap nativeCap() { return nativeCap; }

    /** Returns the dimensionless base cost multiplier (1 means unchanged), without mutation. */
    public float getBaseCostModifier() { return this.baseCostModifier; }

    /** Returns the live nullable special-aspect list; no copy is made. */
    public List<Aspect> getSpecialCostModifierAspects() { return this.specialCostModifierAspects; }

    /** Returns the dimensionless special-aspect cost multiplier without mutation. */
    public float getSpecialCostModifier() { return this.specialCostModifier; }

    /** Returns the stored nullable texture identifier without mutation. */
    public ResourceLocation getTexture() { return this.texture; }

    /** Stores a nullable texture; the renderer uses it for an add-on cap (null falls back to the derived sheet), never for a native one. */
    public void setTexture(ResourceLocation texture) { this.texture = texture; }

    /** Returns the stored nullable tag without mutation. */
    public String getTag() { return this.tag; }

    /** Stores a nullable tag without re-keying caps or changing the native material; gameplay looks caps up by their registry key. */
    public void setTag(String tag) { this.tag = tag; }

    /**
     * Returns the live material stack reference; no copy is made. A native view built without an item answers the native cap
     * item once the items are registered (an empty stack before that).
     */
    public ItemStack getItem() {
        if (this.item == null && this.nativeCap != null) {
            ItemStack resolved = nativeItem(this.nativeCap.tag());
            if (resolved == null) return ItemStack.EMPTY;
            this.item = resolved;
        }
        return this.item;
    }

    private static ItemStack nativeItem(String tag) {
        try {
            return thaumcraft.common.config.ThaumcraftWands.capItem(tag);
        } catch (RuntimeException | LinkageError notRegisteredYet) {
            // Before the native items exist (NeoForge registers them late, unit tests never do).
            return null;
        }
    }

    /** Retains the nullable stack reference; seen by the arcane workbench for an add-on cap, never for a native one. */
    public void setItem(ItemStack item) { this.item = item; }

    /** Returns the dimensionless assembly cost factor, not a centivis debit. */
    public int getCraftCost() { return this.craftCost; }

    /** Stores the dimensionless assembly cost factor; seen by later assemblies with an add-on cap, never a native one. */
    public void setCraftCost(int craftCost) { this.craftCost = craftCost; }

    /** The original research key of a cap. */
    public String getResearch() { return "CAP_" + this.getTag(); }

    /** Null/unknown/malformed queries return null unless a stored null cap/tag causes NPE while scanning the mutable registry. */
    public static WandCap byTag(String tag) {
        for (WandCap cap : caps.values()) {
            if (cap.tag.equals(tag)) return cap;
        }
        return null;
    }

    /**
     * The tag as it appears in a resource path. Resource paths are lower case; tags are not (a tag is also a research key
     * suffix, {@code CAP_ICHOR}), so the derived default texture of a part tagged {@code ICHOR} is {@code ..._ichor.png}.
     */
    static String lowerTag(String tag) {
        return tag.toLowerCase(java.util.Locale.ROOT);
    }
}
