package thaumcraft.client;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusShock;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.foci.ItemFocusFrost;

/** Native component-driven model selection, shared by crafted, held, dropped and inventory wands. */
public final class ThaumcraftWandClient {
    public static final ResourceLocation ROD_PROPERTY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_rod");
    public static final ResourceLocation CAP_PROPERTY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_cap");
    public static final ResourceLocation FOCUS_PROPERTY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_focus");
    private static boolean initialized;

    private ThaumcraftWandClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        // Native ClampedItemPropertyFunction.call clamps to[0,1]. These must
        // match wand.json; integer2 would silently become greatwood's old1.
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), ROD_PROPERTY,
                (stack, level, entity, seed) -> switch (ItemWandCasting.data(stack).rod()) {
                    case "silverwood" -> 1F;
                    case "greatwood" -> .5F;
                    default -> 0F;
                });
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), CAP_PROPERTY,
                (stack, level, entity, seed) -> switch (ItemWandCasting.data(stack).cap()) {
                    case "thaumium" -> 1F;
                    case "gold" -> .5F;
                    default -> 0F;
                });
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), FOCUS_PROPERTY,
                (stack, level, entity, seed) -> {
                    var focus = FocusData.get(stack);
                    return focus.getItem() instanceof ItemFocusShock ? 1F : focus.getItem() instanceof ItemFocusFrost ? .75F
                            : focus.getItem() instanceof ItemFocusExcavation ? .5F : focus.isEmpty() ? 0F : .25F;
                });
        ColorHandlerRegistry.registerItemColors((stack, tint) -> {
            var focus = FocusData.get(stack);
            return tint == 0 && focus.getItem() instanceof ItemFocusBasic item ? 0xff000000 | item.getFocusColor(focus) : -1;
        }, ThaumcraftContent.WAND);
    }
}
