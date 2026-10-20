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
     * <p>Forced 1.21.1 change: the original took a Forge ore name; the native port takes a namespaced
     * item tag, because 1.21.1 has no ore dictionary.</p>
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
     * <p>Forced 1.21.1 change: the original took a Forge ore name; the native port takes a namespaced
     * item tag, because 1.21.1 has no ore dictionary.</p>
     */
    public void registerComplexObjectTag(String nativeItemTag, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(nativeItemTag, aspects);
    }
}
