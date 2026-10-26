package thaumcraft.client;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandModels;
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
    /** One for a wand with an add-on rod or cap: selects {@link WandModels#ADDON_MODEL}. */
    public static final ResourceLocation ADDON_PROPERTY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_addon");
    private static boolean initialized;

    private ThaumcraftWandClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        // Native ClampedItemPropertyFunction.call clamps to [0,1]. The values come from WandModels, which the
        // wand.json overrides are generated and checked against (WandModelAssetsTest).
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), ROD_PROPERTY,
                (stack, level, entity, seed) -> WandModels.rodValue(ItemWandCasting.data(stack).rod()));
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), CAP_PROPERTY,
                (stack, level, entity, seed) -> WandModels.capValue(ItemWandCasting.data(stack).cap()));
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), ADDON_PROPERTY,
                (stack, level, entity, seed) -> {
                    var data = ItemWandCasting.data(stack);
                    return WandModels.addonValue(data.rod(), data.cap());
                });
        ItemPropertiesRegistry.register(ThaumcraftContent.WAND.get(), FOCUS_PROPERTY,
                (stack, level, entity, seed) -> {
                    var focus = FocusData.get(stack);
                    return (focus.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusHellbat ? WandModels.HELLBAT
                            : focus.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusPech ? WandModels.PECH
                            : focus.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusPrimal ? WandModels.PRIMAL
                            : focus.getItem() instanceof ItemFocusShock ? WandModels.SHOCK : focus.getItem() instanceof ItemFocusFrost ? WandModels.FROST
                            : focus.getItem() instanceof ItemFocusExcavation ? WandModels.EXCAVATION
                            : focus.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusWarding ? WandModels.WARDING
                            : focus.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusTrade ? WandModels.TRADE
                            : focus.getItem() instanceof thaumcraft.common.items.wands.foci.ItemFocusPortableHole ? WandModels.PORTABLE_HOLE
                            : focus.isEmpty() ? WandModels.NONE : WandModels.OTHER).value();
                });
        ColorHandlerRegistry.registerItemColors((stack, tint) -> {
            var focus = FocusData.get(stack);
            return tint == 0 && focus.getItem() instanceof ItemFocusBasic item ? 0xff000000 | item.getFocusColor(focus) : -1;
        }, ThaumcraftContent.WAND);
    }
}
