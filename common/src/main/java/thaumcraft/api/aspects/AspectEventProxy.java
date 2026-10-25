package thaumcraft.api.aspects;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;

/**
 * The original TC4 aspect registration facade.
 *
 * <p>The native port keeps the TC4 registry in {@link ThaumcraftApi}; this proxy only forwards to it so that
 * an add-on written against the TC6 shape keeps compiling.</p>
 */
public class AspectEventProxy {
    /** Registers the aspects of one item identity. */
    public void registerObjectTag(ItemStack item, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(item, aspects);
    }

    /** Registers one item identity for a group of metadata values. */
    public void registerObjectTag(ItemStack item, int[] meta, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(item, meta, aspects);
    }

    /**
     * Registers every member of a native item tag.
     *
     * <p>Native namespaced item tags replace Forge ore names; legacy names require an explicit mapping.</p>
     */
    public void registerObjectTag(String nativeItemTag, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(nativeItemTag, aspects);
    }

    /** Registers the aspects of one item identity, including its recipe-derived base. */
    public void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        ThaumcraftApi.registerComplexObjectTag(item, aspects);
    }

    /**
     * Registers every member of a native item tag, including its recipe-derived base.
     *
     * <p>This TC6-shaped convenience overload applies TC4's complex-item registration to each native
     * tag member. It requires the loaded server recipe lifecycle, preserves the original existing-tag
     * branch, and copies additions per member. Legacy Forge ore names require an explicit mapping.</p>
     */
    public void registerComplexObjectTag(String nativeItemTag, AspectList aspects) {
        ThaumcraftApi.registerComplexObjectTag(nativeItemTag, aspects);
    }
}
