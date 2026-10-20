package thaumcraft.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.client.renderers.tile.TileTableRenderer;
import thaumcraft.client.renderers.tile.TileResearchTableRenderer;
import thaumcraft.client.renderers.tile.TileJarRenderer;
import thaumcraft.client.renderers.tile.TileNodeStabilizerRenderer;
import thaumcraft.client.renderers.tile.TileNodeConverterRenderer;
import thaumcraft.client.renderers.tile.TileNodeEnergizedRenderer;
import thaumcraft.client.renderers.tile.TileVisRelayRenderer;
import thaumcraft.common.lib.network.VisNetFxNetworking;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.lib.network.OpenThaumonomiconPayload;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.api.aspects.Aspect;

/** Invoked only by the loader client initializer. */
public final class ClientSetup {
    private static boolean initialized;
    private ClientSetup() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        KnowledgeEvents.initClient();
        ThaumcraftArcaneClient.init();
        ThaumcraftNodeClient.init();
        ThaumcraftFluxClient.init();
        ThaumcraftResourceClient.init();
        ThaumcraftAlchemyClient.init();
        ThaumcraftNaturalClient.init();
        ThaumcraftPlantClient.init();
        ThaumcraftDistillationClient.init();
        ThaumcraftAlembicClient.init();
        ThaumcraftWandClient.init();
        ThaumcraftBellowsClient.init();
        ThaumcraftFocusClient.init();
        ThaumcraftMiscKeyClient.init();
        ThaumcraftFocalClient.init();
        ThaumcraftBoreClient.init();
        ThaumcraftHungryChestClient.init();
        ThaumcraftGolemClient.init();
        ThaumcraftEmberClient.init();
        ThaumcraftFrostClient.init();
        ThaumcraftTubeClient.init();
        ThaumcraftInfusionClient.init();
        ThaumcraftGogglesClient.init();
        ThaumcraftShockClient.init();
        ThaumcraftExcavationClient.init();
        ItemWispEssence.setTooltipViewer(() -> Minecraft.getInstance().player);
        ItemJar.setTooltipViewer(() -> Minecraft.getInstance().player);
        thaumcraft.common.items.ItemCrystalEssence.setTooltipViewer(() -> Minecraft.getInstance().player);
        RenderTypeRegistry.register(RenderType.cutout(), thaumcraft.common.config.ThaumcraftInfusion.TALLOW_CANDLE.get());
        MenuRegistry.registerScreenFactory(ThaumcraftContent.RESEARCH_TABLE_MENU.get(), GuiResearchTable::new);
        BlockEntityRendererRegistry.register(ThaumcraftContent.TABLE_BLOCK_ENTITY.get(), TileTableRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftContent.RESEARCH_TABLE_BLOCK_ENTITY.get(), TileResearchTableRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), TileJarRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.NODE_STABILIZER_BLOCK_ENTITY.get(), TileNodeStabilizerRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.NODE_TRANSDUCER_BLOCK_ENTITY.get(), TileNodeConverterRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get(), TileNodeEnergizedRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), TileVisRelayRenderer::new);
        TileNodeConverter.setClientEffects(VisNetClientEffects.CONVERTER_EFFECTS);
        VisNetFxNetworking.initClient(payload -> {
            var client = Minecraft.getInstance();
            if (client.level == null || !client.level.dimension().location().equals(payload.dimension())) return;
            // Payload carries validated RGB, never an unchecked registry index.
            // Supplier FIRST, consumer SECOND; no client source/vis query.
            VisNetClientEffects.drain(client.level, payload.target(), payload.origin(), payload.rgb());
        });
        EntityRendererRegistry.register(ThaumcraftContent.SPECIAL_ITEM, ItemEntityRenderer::new);
        EntityRendererRegistry.register(ThaumcraftContent.INVENTORY_ITEM, ItemEntityRenderer::new);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenThaumonomiconPayload.TYPE,
                OpenThaumonomiconPayload.STREAM_CODEC, (payload, context) -> context.queue(() -> {
                    var minecraft = Minecraft.getInstance();
                    if (minecraft.player == null) return;
                    minecraft.player.playSound(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "page")), 1, 1);
                    minecraft.setScreen(new GuiResearchBrowser());
                }));
        ColorHandlerRegistry.registerItemColors((stack, tint) -> {
            var data = ItemEssence.data(stack);
            return tint == 1 && data != null ? opaque(Aspect.getAspect(data.tag()).getColor()) : -1;
        }, ThaumcraftContent.FILLED_PHIAL);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(ItemWispEssence.color(stack)) : -1,
                ThaumcraftEssences.WISP_ESSENCE);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(thaumcraft.common.items.ItemCrystalEssence.color(stack)) : -1,
                ThaumcraftContent.CRYSTAL_ESSENCE);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(ItemRobeArmor.color(stack)) : -1,
                ThaumcraftWearables.ROBE_CHESTPLATE, ThaumcraftWearables.ROBE_LEGGINGS, ThaumcraftWearables.ROBE_BOOTS);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> {
            var data = ItemResearchNotes.data(stack);
            return tint == 1 ? opaque(data == null ? 0x999999 : data.color()) : -1;
        }, ThaumcraftContent.RESEARCH_NOTE, ThaumcraftContent.DISCOVERY);
        // Original jar_side carries fractional glass alpha (centre84/255).
        // Cutout discards blending and writes an opaque blue depth shell,
        // hiding real essentia. TC4 jars use the translucent world render pass.
        RenderTypeRegistry.register(RenderType.translucent(), ThaumcraftContent.JAR.get(), ThaumcraftContent.VOID_JAR.get());
        // These are original shard colors, which differ from aspect HUD colors.
        String[] tags = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};
        int[] colors = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};
        for (int i = 0; i < tags.length; i++) {
            int color = colors[i];
            var shard = ThaumcraftContent.SHARDS.get(tags[i]);
            var crystal = ThaumcraftContent.CRYSTALS.get(tags[i]);
            ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(color) : -1, shard);
            ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(color) : -1, () -> crystal.get().asItem());
            ColorHandlerRegistry.registerBlockColors((state, level, pos, tint) -> color, crystal);
            RenderTypeRegistry.register(RenderType.cutout(), crystal.get());
        }
        for (var ore : ThaumcraftOres.INFUSED.values()) {
            var block = ore.get();
            ColorHandlerRegistry.registerBlockColors((state, level, pos, tint) -> tint == 0 ? block.tintColor() : -1, ore);
            ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(block.tintColor()) : -1, () -> block.asItem());
            RenderTypeRegistry.register(RenderType.cutout(), block);
        }
    }

    private static int opaque(int rgb) {
        // Native 1.21.1 ItemRenderer.renderQuadList now reads ARGB alpha too.
        // TC4's 24-bit RGB would make every tinted item layer fully invisible.
        return 0xff000000 | rgb;
    }
}
