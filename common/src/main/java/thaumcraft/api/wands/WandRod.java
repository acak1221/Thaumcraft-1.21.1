package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;

/**
 * The original TC4 wand rod.
 *
 * <p>The native port owns the real rod catalogue in {@code thaumcraft.common.items.wands.WandMaterials}; this
     * class keeps mutable compatibility registrations. Addon constructors register only in {@link #rods},
     * leave nativeRod null, and do not extend the native WandMaterials catalogue used by native wand operations.</p>
 *
 * <p>Forced 1.21.1 change: the original metadata-free rod is unchanged, but the item is a native
 * {@link ItemStack} and the texture a {@link ResourceLocation}, as before.</p>
 */
public class WandRod {
    /** The original public registry; it stays mutable so an add-on may add its own rod. */
    public static LinkedHashMap<String, WandRod> rods = new LinkedHashMap<>();
    private String tag;
    private int craftCost;
    int capacity;
    protected ResourceLocation texture;
    ItemStack item;
    IWandRodOnUpdate onUpdate;
    boolean glow;
    private final thaumcraft.common.items.wands.WandMaterials.Rod nativeRod;

    /** Registers/replaces this tag in rods, retaining nullable item/texture references; capacity is raw centivis and craftCost a dimensionless assembly factor, neither validated. */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        this.nativeRod = null;
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = texture;
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
    }

    /** Registers/replaces this tag in rods, retaining nullable item/callback/texture; capacity is raw centivis, craftCost a dimensionless assembly factor, with no native catalogue binding. */
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

    /** Registers a compatibility rod with a derived texture and no callback; nullable tag/item are retained, capacity is centivis and craftCost a dimensionless assembly factor. */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost) {
        this(tag, capacity, item, craftCost, (IWandRodOnUpdate) null,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png"));
    }

    /** Registers a compatibility rod with a derived texture, retaining nullable tag/item/callback; capacity is centivis and craftCost a dimensionless assembly factor. */
    public WandRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
        this(tag, capacity, item, craftCost, onUpdate,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png"));
    }

    /** A rod of the native catalogue. */
    public WandRod(thaumcraft.common.items.wands.WandMaterials.Rod rod, ItemStack item) {
        this.nativeRod = rod;
        this.setTag(rod.tag());
        this.capacity = rod.capacity();
        this.craftCost = rod.craftCost();
        this.item = item;
        // StaffRod uses the base material sheet, not a nonexistent *_staff PNG.
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + rod.baseTag() + ".png");
        rods.put(rod.tag(), this);
    }

    /** The native rod behind this view, or null for an add-on rod. */
    public thaumcraft.common.items.wands.WandMaterials.Rod nativeRod() { return nativeRod; }

    /** Returns the stored nullable tag without changing the registry. */
    public String getTag() { return this.tag; }

    /** Stores a nullable tag but does not re-key rods or modify the native material. */
    public void setTag(String tag) { this.tag = tag; }

    /** Returns raw centivis capacity; unlike the 1.7.10 whole-vis rod value, native catalogue views already include the factor of 100. */
    public int getCapacity() { return this.capacity; }

    /** Stores unvalidated centivis capacity on this view only; does not modify the native material. */
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /** Returns the stored texture identifier, possibly null, without changing state. */
    public ResourceLocation getTexture() { return this.texture; }

    /** Stores the nullable texture identifier on this view without changing the native material. */
    public void setTexture(ResourceLocation texture) { this.texture = texture; }

    /** Returns the live nullable material stack reference; no copy is made. */
    public ItemStack getItem() { return this.item; }

    /** Retains the nullable stack reference on this view without copying it or changing native registration. */
    public void setItem(ItemStack item) { this.item = item; }

    /** Returns the dimensionless assembly cost factor, not a centivis debit. */
    public int getCraftCost() { return this.craftCost; }

    /** Stores an unvalidated dimensionless assembly cost factor on this view only. */
    public void setCraftCost(int craftCost) { this.craftCost = craftCost; }

    /** Returns the nullable retained callback reference without invoking it. */
    public IWandRodOnUpdate getOnUpdate() { return this.onUpdate; }

    /** Retains a nullable callback on this compatibility view; does not install it in native wand dispatch. */
    public void setOnUpdate(IWandRodOnUpdate onUpdate) { this.onUpdate = onUpdate; }

    /** Returns this view's glow flag without changing state. */
    public boolean isGlowing() { return this.glow; }

    /** Stores this view's glow flag without modifying a native material. */
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
}
