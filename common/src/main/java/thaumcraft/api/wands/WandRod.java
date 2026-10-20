package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;

/**
 * The original TC4 wand rod.
 *
 * <p>The native port owns the real rod catalogue in {@code thaumcraft.common.items.wands.WandMaterials}; this
 * class keeps the original public shape and answers from the native catalogue.</p>
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

    public WandRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        this.nativeRod = null;
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = texture;
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
    }

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

    public WandRod(String tag, int capacity, ItemStack item, int craftCost) {
        this(tag, capacity, item, craftCost, (IWandRodOnUpdate) null,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png"));
    }

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
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + rod.tag() + ".png");
        rods.put(rod.tag(), this);
    }

    /** The native rod behind this view, or null for an add-on rod. */
    public thaumcraft.common.items.wands.WandMaterials.Rod nativeRod() { return nativeRod; }

    public String getTag() { return this.tag; }

    public void setTag(String tag) { this.tag = tag; }

    public int getCapacity() { return this.capacity; }

    public void setCapacity(int capacity) { this.capacity = capacity; }

    public ResourceLocation getTexture() { return this.texture; }

    public void setTexture(ResourceLocation texture) { this.texture = texture; }

    public ItemStack getItem() { return this.item; }

    public void setItem(ItemStack item) { this.item = item; }

    public int getCraftCost() { return this.craftCost; }

    public void setCraftCost(int craftCost) { this.craftCost = craftCost; }

    public IWandRodOnUpdate getOnUpdate() { return this.onUpdate; }

    public void setOnUpdate(IWandRodOnUpdate onUpdate) { this.onUpdate = onUpdate; }

    public boolean isGlowing() { return this.glow; }

    public void setGlowing(boolean hasGlow) { this.glow = hasGlow; }

    /** The original research key of a rod. */
    public String getResearch() { return "ROD_" + this.getTag(); }

    /** The rod of the native catalogue with this tag, or null. */
    public static WandRod byTag(String tag) {
        for (WandRod rod : rods.values()) {
            if (rod.tag.equals(tag)) return rod;
        }
        return null;
    }
}
