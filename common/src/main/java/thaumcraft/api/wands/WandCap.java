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
 * class keeps the original public shape and answers from the native catalogue.</p>
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

    public float getBaseCostModifier() { return this.baseCostModifier; }

    public List<Aspect> getSpecialCostModifierAspects() { return this.specialCostModifierAspects; }

    public float getSpecialCostModifier() { return this.specialCostModifier; }

    public ResourceLocation getTexture() { return this.texture; }

    public void setTexture(ResourceLocation texture) { this.texture = texture; }

    public String getTag() { return this.tag; }

    public void setTag(String tag) { this.tag = tag; }

    public ItemStack getItem() { return this.item; }

    public void setItem(ItemStack item) { this.item = item; }

    public int getCraftCost() { return this.craftCost; }

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
