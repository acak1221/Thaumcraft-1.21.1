package thaumcraft.api.aspects;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;

/**
 * The original TC4 aspect helper.
 *
 * <p>Forced 1.21.1 change: the original read the live object-tag registry directly; the native port reads
 * the same registry through {@link ThaumcraftApiHelper}, which answers from the native state.</p>
 */
public class AspectHelper {
    private AspectHelper() {}

    /** The aspects of one item stack, without a world. */
    public static AspectList getObjectAspects(ItemStack stack) {
        return ThaumcraftApiHelper.getObjectAspects(stack);
    }
}
