package thaumcraft.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemJarLabel;

/** ItemResource:108-126: only a bound label draws the aspect-colored second pass. */
public final class ThaumcraftJarLabelClient {
    public static final ResourceLocation BOUND_PROPERTY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "label_bound");

    private ThaumcraftJarLabelClient() {}

    public static void init() {
        var label = ThaumcraftContent.MATERIALS.get("jar_label");
        ItemPropertiesRegistry.register(label.get(), BOUND_PROPERTY,
                (stack, level, entity, seed) -> bound(ItemJarLabel.aspect(stack)));
        ColorHandlerRegistry.registerItemColors((stack, tint) -> color(ItemJarLabel.aspect(stack), tint), label);
    }

    static float bound(Aspect aspect) { return aspect == null ? 0 : 1; }

    static int color(Aspect aspect, int tint) {
        return tint == 1 && aspect != null ? 0xFF000000 | aspect.getColor() : -1;
    }
}
