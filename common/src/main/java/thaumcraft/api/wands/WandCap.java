package thaumcraft.api.wands;

import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * The original TC4 wand cap.
 *
 * <p>The native port owns the real cap catalogue in {@code thaumcraft.common.items.wands.WandMaterials}; this
     * class keeps mutable compatibility registrations. Addon constructors register only in {@link #caps},
     * leave nativeCap null, and do not extend the native catalogue used by native wand operations.</p>
 */
public class WandCap {
    /** The original public registry; it stays mutable so an add-on may add its own cap. */
    public static LinkedHashMap<String, WandCap> caps = new LinkedHashMap<>();
    private String tag;
    private int craftCost;
    float baseCostModifier;
    List<Aspect> specialCostModifierAspects;
    float specialCostModifier;
    ResourceLocation texture;
    ItemStack item;
    private final thaumcraft.common.items.wands.WandMaterials.Cap nativeCap;

    /** Registers/replaces a compatibility cap, retaining nullable tag/item; discount is a cost multiplier (1 means unchanged), craftCost a dimensionless assembly factor, neither validated. */
    public WandCap(String tag, float discount, ItemStack item, int craftCost) {
        this.nativeCap = null;
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = null;
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + this.getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    /** Registers/replaces a compatibility cap retaining nullable item/special-aspect list; discounts are cost multipliers, craftCost a dimensionless assembly factor, with no native catalogue binding. */
    public WandCap(String tag, float discount, List<Aspect> specialAspects, float discountSpecial, ItemStack item, int craftCost) {
        this.nativeCap = null;
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = specialAspects;
        this.specialCostModifier = discountSpecial;
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + this.getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    /** A cap of the native catalogue. */
    public WandCap(thaumcraft.common.items.wands.WandMaterials.Cap cap, ItemStack item) {
        this.nativeCap = cap;
        this.setTag(cap.tag());
        this.baseCostModifier = cap.modifier();
        this.craftCost = cap.craftCost();
        this.item = item;
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + cap.tag() + ".png");
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

    /** Stores a nullable texture on this view only; no native registration is changed. */
    public void setTexture(ResourceLocation texture) { this.texture = texture; }

    /** Returns the stored nullable tag without mutation. */
    public String getTag() { return this.tag; }

    /** Stores a nullable tag without re-keying caps or changing the native material. */
    public void setTag(String tag) { this.tag = tag; }

    /** Returns the live nullable material stack reference; no copy is made. */
    public ItemStack getItem() { return this.item; }

    /** Retains the nullable stack reference on this view without copying it or changing native registration. */
    public void setItem(ItemStack item) { this.item = item; }

    /** Returns the dimensionless assembly cost factor, not a centivis debit. */
    public int getCraftCost() { return this.craftCost; }

    /** Stores an unvalidated dimensionless assembly cost factor on this view only. */
    public void setCraftCost(int craftCost) { this.craftCost = craftCost; }

    /** The original research key of a cap. */
    public String getResearch() { return "CAP_" + this.getTag(); }

    /** The cap of the native catalogue with this tag, or null. */
    public static WandCap byTag(String tag) {
        for (WandCap cap : caps.values()) {
            if (cap.tag.equals(tag)) return cap;
        }
        return null;
    }
}
