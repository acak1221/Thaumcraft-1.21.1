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
import thaumcraft.common.config.ThaumcraftEquipment;
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
        // The public recipe API answers from the recipe manager of the loaded world.
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level ->
                thaumcraft.api.ThaumcraftApi.installRecipeManager(level.getRecipeManager(), level.registryAccess()));
        thaumcraft.common.lib.network.ResearchSettingsNetworking.initClient();
        // The original block visual channels: the arc lightning, the node bolt and the crucible bubbles.
        thaumcraft.common.lib.network.BlockFxNetworking.initClient(thaumcraft.client.fx.BlockFxClientEffects::accept);
        // The original EventHandlerNetwork.clientLogsOut restores the client's own state when the connection
        // ends (oracle thaumcraft/common/lib/network/EventHandlerNetwork.java:69-81). The common layer owns
        // that restore, so every loader behaves like the original.
        thaumcraft.client.ClientStateReset.init();
        thaumcraft.client.lib.PlayerNotifications.init();
        thaumcraft.client.ThaumcraftMonsterClient.init();
        thaumcraft.client.ThaumcraftRunicClient.init();
        thaumcraft.client.ThaumcraftBaublesClient.init();
        thaumcraft.client.ThaumcraftTravelGearClient.init();
        thaumcraft.common.items.ItemPrimalCharm.setTooltipViewer(() -> Minecraft.getInstance().player);
        thaumcraft.client.ThaumcraftFocusPouchesClient.init();
        thaumcraft.client.ThaumcraftColoredCandlesClient.init();
        thaumcraft.client.ThaumcraftHungryNodeClient.init();
        thaumcraft.client.ThaumcraftTaintBlocksClient.init();
        thaumcraft.client.ThaumcraftTaintSporesClient.init();
        thaumcraft.client.ThaumcraftBiomeClient.init();
        thaumcraft.client.ThaumcraftPrimalFociClient.init();
        thaumcraft.client.ThaumcraftEldritchItemsClient.init();
        thaumcraft.client.ThaumcraftSpaClient.init();
        EntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftTaint.TAINT_SWARM,
                thaumcraft.client.renderers.entity.RenderTaintSwarm::new);
        EntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftTaint.TAINTACLE,
                context -> new thaumcraft.client.renderers.entity.RenderTaintacle(context, .6F, 10));
        EntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftTaint.TAINTACLE_SMALL,
                context -> new thaumcraft.client.renderers.entity.RenderTaintacle(context, .2F, 6));
        thaumcraft.common.entities.monster.EntityTaintacle.setClientEffects(thaumcraft.client.fx.particles.TaintacleAriseParticle::arise);
        // Original ClientProxy.smokeSpiral: the smoke of the Sword of the Zephyr and of the
        // spawning eldritch warden (oracle ItemElementalSword.onUsingTick:128-141 and
        // EntityEldritchWarden.onUpdate:186-194).
        thaumcraft.common.items.equipment.ItemElementalSword.setClientEffects((player, floor, count) -> {
            if (!(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) return;
            for (int a = 0; a < count; ++a)
                thaumcraft.client.fx.particles.FXSmokeSpiral.spiral(level, player.getX(),
                        player.getY() + player.getBbHeight() / 2F, player.getZ(), 1.5F,
                        level.random.nextInt(360), floor, 0xDDDDDD);
        });
        thaumcraft.common.entities.monster.EntityEldritchWarden.setClientEffects((warden, height, count) -> {
            if (!(warden.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) return;
            for (int a = 0; a < count; ++a)
                thaumcraft.client.fx.particles.FXSmokeSpiral.spiral(level, warden.getX(),
                        warden.getY() + height / 2F, warden.getZ(), height,
                        level.random.nextInt(360), (int) Math.floor(warden.getY()) - 1, 0x22112F);
        });
        thaumcraft.common.items.ItemManaBean.setTooltipViewer(() -> Minecraft.getInstance().player);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? 0xff000000 | thaumcraft.common.items.ItemManaBean.color(stack) : -1,
                thaumcraft.common.config.ThaumcraftManaBeans.MANA_BEAN);
        RenderTypeRegistry.register(RenderType.cutout(), thaumcraft.common.config.ThaumcraftManaBeans.MANA_POD.get());
        thaumcraft.client.ThaumcraftFireSpecialClient.init();
        thaumcraft.client.ThaumcraftShockOrbsClient.init();
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                thaumcraft.common.config.ThaumcraftAspectOrbs.ORB, thaumcraft.client.renderers.entity.RenderAspectOrb::new);
        KnowledgeEvents.initClient();
        ThaumcraftArcaneClient.init();
        ThaumcraftNodeClient.init();
        ThaumcraftFluxClient.init();
        ThaumcraftResourceClient.init();
        ThaumcraftAlchemyClient.init();
        ThaumcraftNaturalClient.init();
        ThaumcraftPlantClient.init();
        ThaumcraftBiologicalLampClient.init();
        ThaumcraftDistillationClient.init();
        ThaumcraftAdvancedFurnaceClient.init();
        ThaumcraftCrystalClustersClient.init();
        ThaumcraftPechClient.init();
        ThaumcraftAlembicClient.init();
        ThaumcraftWandClient.init();
        ThaumcraftBellowsClient.init();
        ThaumcraftFocusClient.init();
        ThaumcraftMiscKeyClient.init();
        ThaumcraftConfigClient.init();
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
        ThaumcraftWarpClient.init();
        ItemWispEssence.setTooltipViewer(() -> Minecraft.getInstance().player);
        ItemJar.setTooltipViewer(() -> Minecraft.getInstance().player);
        thaumcraft.common.items.ItemCrystalEssence.setTooltipViewer(() -> Minecraft.getInstance().player);
        RenderTypeRegistry.register(RenderType.cutout(), thaumcraft.common.config.ThaumcraftInfusion.TALLOW_CANDLE.get());
        RenderTypeRegistry.register(RenderType.cutout(), thaumcraft.common.config.ThaumcraftGrate.ITEM_GRATE.get());
        MenuRegistry.registerScreenFactory(ThaumcraftContent.RESEARCH_TABLE_MENU.get(), GuiResearchTable::new);
        MenuRegistry.registerScreenFactory(thaumcraft.common.config.ThaumcraftDeconstruction.MENU.get(), thaumcraft.client.gui.GuiDeconstructionTable::new);
        BlockEntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftDeconstruction.TILE.get(), thaumcraft.client.renderers.tile.TileDeconstructionTableRenderer::new);
        MenuRegistry.registerScreenFactory(thaumcraft.common.config.ThaumcraftThaumatorium.MENU.get(), thaumcraft.client.gui.GuiThaumatorium::new);
        BlockEntityRendererRegistry.register(ThaumcraftContent.TABLE_BLOCK_ENTITY.get(), TileTableRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftContent.RESEARCH_TABLE_BLOCK_ENTITY.get(), TileResearchTableRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftContent.JAR_BLOCK_ENTITY.get(), TileJarRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.NODE_STABILIZER_BLOCK_ENTITY.get(), TileNodeStabilizerRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.NODE_TRANSDUCER_BLOCK_ENTITY.get(), TileNodeConverterRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get(), TileNodeEnergizedRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), TileVisRelayRenderer::new);
        BlockEntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftLamps.ARCANE_LAMP_TILE.get(), thaumcraft.client.renderers.tile.TileArcaneLampRenderer::new);
        BlockEntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftVisCharging.VIS_CHARGE_RELAY_TILE.get(), thaumcraft.client.renderers.tile.TileMagicWorkbenchChargerRenderer::new);
        BlockEntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftWards.WARDED_TILE.get(), thaumcraft.client.renderers.tile.TileWardedRenderer::new);
        // Original BlockLifterRenderer:66-92 - the glowing faces of the levitator.
        BlockEntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftLevitator.LEVITATOR_BLOCK_ENTITY.get(), thaumcraft.client.renderers.tile.TileLevitatorRenderer::new);
        TileNodeConverter.setClientEffects(VisNetClientEffects.CONVERTER_EFFECTS);
        VisNetFxNetworking.initClient(payload -> {
            var client = Minecraft.getInstance();
            if (client.level == null || !client.level.dimension().location().equals(payload.dimension())) return;
            // Payload carries validated RGB, never an unchecked registry index.
            // Supplier FIRST, consumer SECOND; no client source/vis query.
            VisNetClientEffects.drain(client.level, payload.target(), payload.origin(), payload.rgb());
        });
        EntityRendererRegistry.register(ThaumcraftContent.SPECIAL_ITEM, thaumcraft.client.renderers.entity.RenderSpecialItem::new);
        EntityRendererRegistry.register(ThaumcraftContent.INVENTORY_ITEM, ItemEntityRenderer::new);
        thaumcraft.client.ThaumcraftEldritchClient.init();
        thaumcraft.client.ThaumcraftDeviceGuiClient.init();
        thaumcraft.client.ThaumcraftGearModelsClient.init();
        thaumcraft.client.ThaumcraftChampionClient.init();
        thaumcraft.client.ThaumcraftReservoirClient.init();
        EntityRendererRegistry.register(thaumcraft.common.config.ThaumcraftTaintCreeper.TAINT_CREEPER,
                thaumcraft.client.renderers.entity.RenderTaintCreeper::new);
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
        // The void thaumaturge robes tint the plain pass (tint 0) with the same dye, the metal pass is untinted.
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? opaque(ItemRobeArmor.color(stack)) : -1,
                ThaumcraftEquipment.VOID_ROBE_HELMET, ThaumcraftEquipment.VOID_ROBE_CHESTPLATE, ThaumcraftEquipment.VOID_ROBE_LEGGINGS);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> {
            var data = ItemResearchNotes.data(stack);
            return tint == 1 ? opaque(data == null ? 0x999999 : data.color()) : -1;
        }, ThaumcraftContent.RESEARCH_NOTE, ThaumcraftContent.DISCOVERY,
                thaumcraft.common.config.ThaumcraftUnknownKnowledge.UNKNOWN_NOTE,
                thaumcraft.common.config.ThaumcraftUnknownKnowledge.UNKNOWN_NOTE_24);
        // Original jar_side carries fractional glass alpha (centre84/255).
        // Cutout discards blending and writes an opaque blue depth shell,
        // hiding real essentia. TC4 jars use the translucent world render pass.
        RenderTypeRegistry.register(RenderType.translucent(), ThaumcraftContent.JAR.get(), ThaumcraftContent.VOID_JAR.get());
        RenderTypeRegistry.register(RenderType.translucent(), thaumcraft.common.config.ThaumcraftTubes.ESSENTIA_RESERVOIR.get());
        RenderTypeRegistry.register(RenderType.translucent(), thaumcraft.common.config.ThaumcraftLamps.AMBER_BLOCK.get(), thaumcraft.common.config.ThaumcraftLamps.AMBER_BRICKS.get());
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
