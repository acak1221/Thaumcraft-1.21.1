package thaumcraft.client.renderers.item;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.foci.ItemFocusPech;
import thaumcraft.common.items.wands.foci.ItemFocusPortableHole;
import thaumcraft.common.items.wands.foci.ItemFocusPrimal;
import thaumcraft.common.items.wands.foci.ItemFocusWarding;

/**
 * Constructs and registers real focus items before the native registry freezes, like the existing
 * EldritchTestRegistry and BlockVisibilityTestRegistry fixtures. No frozen registry is reopened,
 * and every constructed intrusive holder is bound to a test-only registry ID.
 */
public final class FocusRenderingTestRegistry implements LauncherSessionListener {
    private static List<ItemFocusBasic> depthFoci;
    private static ItemFocusBasic ordinary, addon, ornamented;

    @Override public void launcherSessionOpened(LauncherSession session) {
        if (depthFoci != null) return;
        try {
            SharedConstants.tryDetectVersion();
            Field flag = Bootstrap.class.getDeclaredField("isBootstrapped");
            flag.setAccessible(true);
            boolean previous = flag.getBoolean(null);
            flag.setBoolean(null, true);
            try {
                depthFoci = List.of(register("primal", new ItemFocusPrimal(new Item.Properties())),
                        register("pech", new ItemFocusPech(new Item.Properties())),
                        register("warding", new ItemFocusWarding(new Item.Properties())),
                        register("portablehole", new ItemFocusPortableHole(new Item.Properties())));
                ordinary = register("ordinary", new ItemFocusBasic(new Item.Properties()));
                addon = register("addon", new ItemFocusBasic(new Item.Properties()) {
                    @Override public ResourceLocation getFocusDepthLayerIcon(ItemStack stack) {
                        return ResourceLocation.fromNamespaceAndPath("addon", "textures/focus.png");
                    }
                });
                ornamented = register("ornamented", new ItemFocusBasic(new Item.Properties()) {
                    @Override public int getFocusColor(ItemStack stack) { return 0x33CCAA; }
                    @Override public ResourceLocation getOrnament(ItemStack stack) {
                        return ResourceLocation.fromNamespaceAndPath("addon", "textures/items/focus_orn.png");
                    }
                });
            } finally {
                flag.setBoolean(null, previous);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not enter the native focus test registration lifecycle", failure);
        }
    }

    private static <T extends ItemFocusBasic> T register(String name, T item) {
        return Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_test", "render_focus_" + name), item);
    }

    static List<ItemFocusBasic> depthFoci() { return depthFoci; }
    static ItemFocusBasic ordinary() { return ordinary; }
    static ItemFocusBasic addon() { return addon; }
    static ItemFocusBasic ornamented() { return ornamented; }
}
