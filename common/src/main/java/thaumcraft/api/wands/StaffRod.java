package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The original TC4 staff rod: a rod with the staff tag and its own texture.
 *
 * <p>Forced 1.21.1 change: the original staff texture name is kept, but the native staff rods are the
 * {@code <rod>_staff} entries of the native rod catalogue.</p>
 */
public class StaffRod extends WandRod {
    boolean runes = false;

    public StaffRod(String tag, int capacity, ItemStack item, int craftCost) {
        super(tag + "_staff", capacity, item, craftCost);
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png");
    }

    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate, texture);
    }

    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate);
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png");
    }

    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, texture);
    }

    public boolean hasRunes() { return this.runes; }

    public void setRunes(boolean hasRunes) { this.runes = hasRunes; }
}
