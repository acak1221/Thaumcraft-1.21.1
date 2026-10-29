package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The original TC4 staff rod: a rod with the staff tag and its own texture.
 *
 * <p>Add-on contract: everything of {@link WandRod} applies; the rod is registered under {@code <tag>_staff}, and a wand built
 * from it is a staff (original {@code isStaff}: {@code rod instanceof StaffRod}) - the long staff model, +6 attack damage, no
 * arcane workbench slot. {@link #setRunes(boolean)} adds the primal staff's runes and one focus potency. The capacity is in
 * whole vis (the original staves declare 125 / 175 / 250). The derived texture is the sheet of the plain rod,
 * {@code thaumcraft:textures/models/wand_rod_<tag>.png}, and the wand name uses {@code item.Wand.<tag>.rod}.</p>
 *
 * <p>Forced 1.21.1 change: the original staff texture name is kept, but the native staff rods are the
 * {@code <rod>_staff} entries of the native rod catalogue, present in {@link WandRod#rods} as {@code StaffRod} views.</p>
 */
public class StaffRod extends WandRod {
    boolean runes = false;

    /** Registers a staff rod under tag plus _staff with the derived texture; capacity in whole vis, craftCost a dimensionless factor. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost) {
        super(tag + "_staff", capacity, item, craftCost);
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + WandRod.lowerTag(tag) + ".png");
    }

    /** Registers tag plus _staff, retaining nullable item/callback/texture; capacity in whole vis, craftCost a dimensionless factor. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate, texture);
    }

    /** Registers tag plus _staff with the derived texture and a nullable callback; capacity in whole vis. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate);
        this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + WandRod.lowerTag(tag) + ".png");
    }

    /** Registers tag plus _staff, retaining nullable item/texture; capacity in whole vis, craftCost a dimensionless factor. */
    public StaffRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, texture);
    }

    /**
     * A view of a staff rod of the native catalogue ({@code <rod>_staff}), registered under its tag.
     *
     * @param item the staff core item, or null to resolve the native item when it is first asked for
     */
    public StaffRod(thaumcraft.common.items.wands.WandMaterials.Rod rod, ItemStack item) {
        super(rod, item);
        this.runes = rod.hasRunes();
    }

    /** Returns this rod's rune flag without mutation. */
    public boolean hasRunes() { return this.runes; }

    /** Stores the rune flag; an add-on staff then adds one focus potency and draws the runes. A native view's flag is ignored. */
    public void setRunes(boolean hasRunes) { this.runes = hasRunes; }
}
