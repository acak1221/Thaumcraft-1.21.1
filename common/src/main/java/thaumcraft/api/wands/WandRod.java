package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;

/**
 * The original TC4 wand rod.
 *
 * <h2>Add-on contract</h2>
 * <p>An add-on registers a rod exactly like it did on 1.7.10, by constructing it; the constructor puts it into {@link #rods}
 * under its tag:</p>
 * <pre>{@code
 * WandRod infernal = new WandRod("infernal", 150, new ItemStack(MyItems.INFERNAL_ROD), 12, new MyRodOnUpdate(),
 *         ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_rod_infernal.png"));
 * }</pre>
 * <ul>
 *   <li><b>Capacity</b> is in <b>whole vis</b>, the unit of the original constructors ({@code new WandRod("silverwood", 100, ...)}).
 *       The wand stores centivis: a wand holds {@code capacity * 100} centivis of every primal, a sceptre
 *       {@code capacity * 150} (original ItemWandCasting.getMaxVis:91). {@link #getCapacity()} answers whole vis for every rod,
 *       native ones included ({@code WandRod.rods.get("greatwood").getCapacity() == 50}).</li>
 *   <li><b>Craft cost</b> is the dimensionless assembly factor: the arcane workbench asks {@code rodCost * capCost} vis of each
 *       primal for a wand, and {@code (int) (rodCost * capCost * 1.5F)} for a sceptre (original ArcaneWandRecipe:59,
 *       ArcaneSceptreRecipe:64); the paying wand's cap and the player's gear discount that price as usual.</li>
 *   <li><b>Item</b> is the stack the arcane workbench accepts in the rod cell; it is compared by item and, when the grid stack carries
 *       its own components, by those components (original ArcaneWandRecipe.checkItemEquals).</li>
 *   <li><b>Research</b> is {@link #getResearch()}, {@code ROD_<tag>}; the add-on registers that research itself. A subclass may
 *       override it.</li>
 *   <li><b>{@link IWandRodOnUpdate}</b> is called on the server every tick the wand sits in a player's inventory (original
 *       ItemWandCasting.onUpdate:350-355). Use {@code ItemWandCasting.addVis/getVis/getMaxVis} from it.</li>
 *   <li><b>Texture</b> is an entity-style sheet in the layout of the original {@code ModelWand} (the 1.7.10 PNG works unchanged);
 *       a texture of null falls back to {@code thaumcraft:textures/models/wand_rod_<tag>.png}. {@link #setGlowing(boolean)} lights
 *       the rod like the original ModelWand:87-101.</li>
 *   <li><b>Name</b>: the wand name uses the translation key {@code item.Wand.<tag>.rod} (for a {@link StaffRod}, the tag without
 *       {@code _staff}); the add-on ships it in its own {@code lang/*.json}.</li>
 * </ul>
 * <p>Registration may happen at any time of mod initialisation, before or after Thaumcraft, on Fabric or NeoForge: gameplay looks
 * the registry up whenever it needs a part, nothing is copied at Thaumcraft's initialisation, and later setter calls on the
 * registered object are seen. Register during initialisation only: the registry is a plain map, not thread-safe, and a wand whose
 * rod is no longer registered can neither be loaded nor used.</p>
 *
 * <h2>Native rods</h2>
 * <p>The native rods of the port's catalogue ({@code thaumcraft.common.items.wands.WandMaterials}) are present in {@link #rods} as
 * views ({@link #nativeRod()} is non-null; the nine staff cores are {@link StaffRod}s), as in the original. Their values are those
 * of the original rods. Gameplay reads native rods from the catalogue, not from these views: changing a native view, or
 * registering another rod under a native tag, does not change the native rod (forced deviation that keeps the native values
 * bit-identical).</p>
 *
 * <p>Forced 1.21.1 change: the original metadata-free rod is unchanged, but the item is a native
 * {@link ItemStack} and the texture a {@link ResourceLocation}, as before.</p>
 */
public class WandRod {
    /** The original public registry; it stays mutable so an add-on may add its own rod. Native views are registered first. */
    public static LinkedHashMap<String, WandRod> rods = new LinkedHashMap<>();
    private String tag;
    private int craftCost;
    int capacity;
    protected ResourceLocation texture;
    ItemStack item;
    IWandRodOnUpdate onUpdate;
    boolean glow;
    private final thaumcraft.common.items.wands.WandMaterials.Rod nativeRod;
    /** The views registered below, kept even if an add-on replaces their registry entries. */
    private static final java.util.Map<String, WandRod> NATIVE_VIEWS = new java.util.HashMap<>();

    static {
        // Original ConfigItems:292-325 registers the native rods during Thaumcraft's own start; here they exist as soon as the
        // registry is touched, so an add-on that starts first still finds them.
        for (var rod : thaumcraft.common.items.wands.WandMaterials.RODS) {
            NATIVE_VIEWS.put(rod.tag(), rod.staff() ? new StaffRod(rod, null) : new WandRod(rod, null));
        }
    }

    /** The rod gameplay uses for this tag: the native view of a native tag, else the registry entry (null when unknown). */
    static WandRod forGameplay(String tag) {
        WandRod view = NATIVE_VIEWS.get(tag);
        return view != null ? view : rods.get(tag);
    }

    /**
     * Registers (or replaces) this rod under its tag; retains nullable item/texture references.
     *
     * @param capacity  the capacity in WHOLE vis, like the original constructor
     * @param craftCost the dimensionless assembly factor
     */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        this.nativeRod = null;
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = texture;
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
    }

    /**
     * Registers (or replaces) this rod under its tag; retains nullable item/callback/texture.
     *
     * @param capacity  the capacity in WHOLE vis, like the original constructor
     * @param craftCost the dimensionless assembly factor
     * @param onUpdate  called on the server every tick the wand sits in a player's inventory; may be null
     */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
        this.nativeRod = null;
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = texture;
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
        this.onUpdate = onUpdate;
    }

    /** Registers a rod with the derived texture {@code thaumcraft:textures/models/wand_rod_<tag>.png} and no callback; capacity in whole vis. */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost) {
        this(tag, capacity, item, craftCost, (IWandRodOnUpdate) null,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + lowerTag(tag) + ".png"));
    }

    /** Registers a rod with the derived texture {@code thaumcraft:textures/models/wand_rod_<tag>.png}; capacity in whole vis. */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
        this(tag, capacity, item, craftCost, onUpdate,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + lowerTag(tag) + ".png"));
    }

    /**
     * A view of a rod of the native catalogue, registered under its tag. The capacity is converted to whole vis.
     *
     * @param item the rod item, or null to resolve the native rod item when it is first asked for
     */
    public WandRod(thaumcraft.common.items.wands.WandMaterials.Rod rod, ItemStack item) {
        this.nativeRod = rod;
        this.setTag(rod.tag());
        this.capacity = rod.capacity() / 100;
        this.craftCost = rod.craftCost();
        this.item = item;
        // StaffRod uses the base material sheet, not a nonexistent *_staff PNG.
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + lowerTag(rod.baseTag()) + ".png");
        // Original ConfigItems:305: WAND_ROD_BLAZE.setGlowing(true); the native elemental rods carry their WandRodPrimalOnUpdate.
        this.glow = "blaze".equals(rod.tag());
        if (rod.passive() != null) {
            this.onUpdate = thaumcraft.common.items.wands.WandMaterials.ANY_PRIMAL.equals(rod.passive())
                    ? thaumcraft.common.items.wands.WandRodPrimalOnUpdate.anyPrimalStaff()
                    : thaumcraft.common.items.wands.WandRodPrimalOnUpdate.of(rod.tag());
        }
        rods.put(rod.tag(), this);
    }

    /** The native rod behind this view, or null for an add-on rod. */
    public thaumcraft.common.items.wands.WandMaterials.Rod nativeRod() { return nativeRod; }

    /** Returns the stored nullable tag without changing the registry. */
    public String getTag() { return this.tag; }

    /** Stores a nullable tag but does not re-key rods or modify the native material; gameplay looks rods up by their registry key. */
    public void setTag(String tag) { this.tag = tag; }

    /** The capacity in WHOLE vis (original unit); a wand of this rod holds {@code capacity * 100} centivis per primal. */
    public int getCapacity() { return this.capacity; }

    /** Stores the whole-vis capacity; seen by later gameplay lookups of an add-on rod, never by a native one. */
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /** Returns the stored texture identifier, possibly null, without changing state. */
    public ResourceLocation getTexture() { return this.texture; }

    /** Stores the nullable texture identifier; seen by the renderer for an add-on rod, never for a native one. */
    public void setTexture(ResourceLocation texture) { this.texture = texture; }

    /**
     * Returns the live material stack reference; no copy is made. A native view built without an item answers the native rod
     * item once the items are registered (an empty stack before that).
     */
    public ItemStack getItem() {
        if (this.item == null && this.nativeRod != null) {
            ItemStack resolved = nativeItem(this.nativeRod.tag());
            if (resolved == null) return ItemStack.EMPTY;
            this.item = resolved;
        }
        return this.item;
    }

    private static ItemStack nativeItem(String tag) {
        try {
            return thaumcraft.common.config.ThaumcraftWands.rodItem(tag);
        } catch (RuntimeException | LinkageError notRegisteredYet) {
            // Before the native items exist (NeoForge registers them late, unit tests never do).
            return null;
        }
    }

    /** Retains the nullable stack reference; seen by the arcane workbench for an add-on rod, never for a native one. */
    public void setItem(ItemStack item) { this.item = item; }

    /** Returns the dimensionless assembly cost factor, not a centivis debit. */
    public int getCraftCost() { return this.craftCost; }

    /** Stores the dimensionless assembly cost factor; seen by later assemblies with an add-on rod, never a native one. */
    public void setCraftCost(int craftCost) { this.craftCost = craftCost; }

    /** Returns the nullable retained callback reference without invoking it. */
    public IWandRodOnUpdate getOnUpdate() { return this.onUpdate; }

    /** Retains a nullable callback; an add-on rod's callback runs on the server tick of a carried wand, a native view's never does. */
    public void setOnUpdate(IWandRodOnUpdate onUpdate) { this.onUpdate = onUpdate; }

    /** Returns this rod's glow flag without changing state. */
    public boolean isGlowing() { return this.glow; }

    /** Stores the glow flag; an add-on rod is then drawn lit (original ModelWand:87-101). */
    public void setGlowing(boolean hasGlow) { this.glow = hasGlow; }

    /** The original research key of a rod. */
    public String getResearch() { return "ROD_" + this.getTag(); }

    /** Null/unknown/malformed queries return null unless a stored null rod/tag causes NPE while scanning the mutable registry. */
    public static WandRod byTag(String tag) {
        for (WandRod rod : rods.values()) {
            if (rod.tag.equals(tag)) return rod;
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
