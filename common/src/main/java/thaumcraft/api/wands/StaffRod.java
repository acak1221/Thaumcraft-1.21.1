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

    /** Registers a compatibility rod under tag plus _staff with a derived texture; capacity is centivis, craftCost a dimensionless assembly factor, nullable item is retained. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost) {
        super(tag + "_staff", capacity, item, craftCost);
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png");
    }

    /** Registers tag plus _staff, retaining nullable item/callback/texture; capacity is centivis and craftCost a dimensionless assembly factor, without native catalogue extension. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate, texture);
    }

    /** Registers tag plus _staff with a derived texture and retained nullable item/callback; capacity is centivis and craftCost a dimensionless assembly factor. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate);
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + tag + ".png");
    }

    /** Registers tag plus _staff, retaining nullable item/texture; capacity is centivis and craftCost a dimensionless assembly factor, without native catalogue extension. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, texture);
    }

    /** Returns this compatibility view's rune flag without mutation. */
    public boolean hasRunes() { return this.runes; }

    /** Stores this view's rune flag only; does not modify the native material catalogue. */
    public void setRunes(boolean hasRunes) { this.runes = hasRunes; }
}
