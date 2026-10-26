package thaumcraft.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import thaumcraft.client.ThaumcraftArchitectClient;
import thaumcraft.client.ThaumcraftExcavationClient;
import thaumcraft.client.ThaumcraftGogglesClient;
import thaumcraft.client.ThaumcraftShockClient;
import thaumcraft.client.ThaumcraftWarpClient;
import thaumcraft.client.gui.GuiResearchPopup;
import thaumcraft.client.lib.PlayerNotifications;
import thaumcraft.client.renderers.item.JarItemRenderer;
import thaumcraft.client.renderers.tile.TileAlchemyFurnaceAdvancedRenderer;
import thaumcraft.client.renderers.tile.TileArcaneBoreBaseRenderer;
import thaumcraft.client.renderers.tile.TileArcaneBoreRenderer;
import thaumcraft.client.renderers.tile.TileCentrifugeRenderer;
import thaumcraft.client.renderers.tile.TileCrystalRenderer;
import thaumcraft.client.renderers.tile.TileEssentiaCrystalizerRenderer;
import thaumcraft.client.renderers.tile.TileMagicWorkbenchChargerRenderer;
import thaumcraft.client.renderers.tile.TileNodeConverterRenderer;
import thaumcraft.client.renderers.tile.TileNodeStabilizerRenderer;
import thaumcraft.client.renderers.tile.TileTubeBufferRenderer;
import thaumcraft.client.renderers.tile.TileTubeRenderer;
import thaumcraft.client.renderers.tile.TileVisRelayRenderer;
import thaumcraft.client.ThaumcraftGolemClient;
import thaumcraft.client.ThaumcraftRunicClient;
import thaumcraft.client.ThaumcraftTaintSporesClient;
import thaumcraft.client.ThaumcraftTravelGearClient;
import thaumcraft.client.VisNetClientEffects;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftHungryChest;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftVisCharging;

/**
 * NeoForge half of the client bootstrap of the Fabric loader.
 *
 * <p>The shared client setup of {@code thaumcraft.client.ClientSetup} uses the Architectury client
 * registries, which the Architectury NeoForge module implements. This class owns the registrations
 * that the Fabric bootstrap performs with a Fabric API and that NeoForge owns itself: the two core
 * shaders, the builtin item renderers, the HUD overlay, the late world render and the client
 * resource reload.</p>
 */
public final class ThaumcraftNeoForgeClient {
    private ThaumcraftNeoForgeClient() {}

    public static void register(IEventBus bus) {
        NeoForgeSpaClient.init();
        bus.addListener(ThaumcraftNeoForgeClient::onRegisterShaders);
        bus.addListener(ThaumcraftNeoForgeClient::onRegisterClientExtensions);
        bus.addListener(ThaumcraftNeoForgeClient::onRegisterReloadListeners);
        NeoForge.EVENT_BUS.addListener(ThaumcraftNeoForgeClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ThaumcraftNeoForgeClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(ThaumcraftNeoForgeClient::onLoggingOut);
    }

    /** The core shaders of the Fabric CoreShaderRegistrationCallback. */
    private static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            var mirror = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "linked_mirror"),
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            event.registerShader(mirror, thaumcraft.client.renderers.tile.LinkedMirrorField::setShader);
            var monster = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "monster_lit_particle"),
                    DefaultVertexFormat.PARTICLE);
            event.registerShader(monster, thaumcraft.client.fx.particles.MonsterSpriteParticle::setLitShader);
            var bubble = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_stabilizer_bubble"),
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            event.registerShader(bubble, shader -> {
                TileNodeStabilizerRenderer.setBubbleShader(shader);
                VisNetClientEffects.setShader(shader);
            });
            var architect = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "architect_overlay"),
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            event.registerShader(architect, ThaumcraftArchitectClient::setShader);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("TC4 core shaders could not be created", failure);
        }
    }

    /** The builtin item renderers of the Fabric BuiltinItemRendererRegistry. */
    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // The shared client setup must run before NeoForge collects menu screens, model layers
        // and entity renderers. ClientHooks.initClientHooks posts those events from the Minecraft
        // constructor, long before FMLClientSetupEvent, and the Architectury registries apply
        // these three only inside the events. Run from client setup, every Thaumcraft GUI, gear
        // model layer and entity renderer was lost, and the first Thaumcraft entity drawn crashed
        // the client. This event is the first initClientHooks posts; registries are filled by then.
        thaumcraft.client.ClientSetup.init();
        // The original robe dye in the native armour layer. It is not an item
        // renderer but the NeoForge native colour hook of the robe items.
        NeoForgeRobeDye.register(event);
        // The original wand uses the TC4 model, not the vanilla baked item model.
        event.registerItem(renderer(thaumcraft.client.ThaumcraftGearModelsClient::renderWand), ThaumcraftContent.WAND.get());
        // Original ItemNodeRenderer: the item of the aura node is drawn as the node itself.
        event.registerItem(renderer(thaumcraft.client.renderers.tile.ItemNodeRenderer::render),
                ThaumcraftContent.AURA_NODE.get().asItem());
        // Original ItemThaumometerRenderer: the thaumometer is the three dimensional
        // scanner of the original, not a flat sprite.
        event.registerItem(renderer(thaumcraft.client.renderers.item.ItemThaumometerRenderer::render),
                ThaumcraftContent.THAUMOMETER.get());
        // Original ItemBannerRenderer: the banner item carries its colour and its
        // aspect and is drawn with the banner model instead of a plain cube.
        event.registerItem(renderer(thaumcraft.client.renderers.item.ItemBannerRenderer::render),
                thaumcraft.common.config.ThaumcraftEldritchBlocks.BANNER_ITEM.get());
        event.registerItem(renderer(TileAlchemyFurnaceAdvancedRenderer::renderItem),
                ThaumcraftAdvancedFurnace.FURNACE_ITEM.get());
        for (var crystal : ThaumcraftCrystalClusters.blocks())
            event.registerItem(renderer(TileCrystalRenderer::renderItem), crystal.asItem());
        // The retained shell is shared with blocks, but actual item contents
        // and labels must come from this stack, never a synthetic world tile.
        event.registerItem(renderer(JarItemRenderer::render), ThaumcraftContent.JAR.get().asItem(), ThaumcraftContent.VOID_JAR.get().asItem());
        // Original ItemJarNodeRenderer: the node of the jar in three crossed planes
        // inside the glass shell of the item.
        event.registerItem(renderer(thaumcraft.client.renderers.tile.ItemJarNodeRenderer::render),
                thaumcraft.common.config.ThaumcraftEldritchBlocks.JAR_NODE_ITEM.get());
        event.registerItem(renderer(TileTubeRenderer::renderValveItem), ThaumcraftTubes.ESSENTIA_VALVE.get().asItem());
        event.registerItem(renderer(TileTubeRenderer::renderAdvancedItem), ThaumcraftTubes.ESSENTIA_FILTER.get().asItem(),
                ThaumcraftTubes.ESSENTIA_RESTRICT.get().asItem(), ThaumcraftTubes.ESSENTIA_ONEWAY.get().asItem());
        event.registerItem(renderer(TileTubeBufferRenderer::renderItem), ThaumcraftTubes.ESSENTIA_BUFFER.get().asItem());
        event.registerItem(renderer(TileCentrifugeRenderer::renderItem), ThaumcraftTubes.CENTRIFUGE.get().asItem());
        event.registerItem(renderer(TileEssentiaCrystalizerRenderer::renderItem), ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().asItem());
        event.registerItem(renderer(TileNodeStabilizerRenderer::renderItem), ThaumcraftNodeDevices.NODE_STABILIZER.get().asItem());
        event.registerItem(renderer(TileNodeStabilizerRenderer::renderItem), ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER_ITEM.get());
        event.registerItem(renderer(TileNodeConverterRenderer::renderItem), ThaumcraftNodeDevices.NODE_TRANSDUCER.get().asItem());
        event.registerItem(renderer(TileVisRelayRenderer::renderItem), ThaumcraftNodeDevices.VIS_RELAY.get().asItem());
        event.registerItem(renderer(TileMagicWorkbenchChargerRenderer::renderItem),
                ThaumcraftVisCharging.VIS_CHARGE_RELAY_ITEM.get());
        event.registerItem(renderer(TileArcaneBoreRenderer::renderItem), ThaumcraftArcaneBore.ARCANE_BORE.get().asItem());
        event.registerItem(renderer(TileArcaneBoreBaseRenderer::renderItem), ThaumcraftArcaneBore.ARCANE_BORE_BASE.get().asItem());
        var hungryChest = new thaumcraft.common.tiles.TileChestHungry(net.minecraft.core.BlockPos.ZERO,
                ThaumcraftHungryChest.HUNGRY_CHEST.get().defaultBlockState());
        event.registerItem(renderer((stack, context, pose, buffers, light, overlay) ->
                        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                                .renderItem(hungryChest, pose, buffers, light, overlay)),
                ThaumcraftHungryChest.HUNGRY_CHEST_ITEM.get());
    }

    /**
     * The builtin item renderer of the Fabric BuiltinItemRendererRegistry. NeoForge publishes it
     * through the client item extensions, whose custom renderer is a vanilla
     * {@code BlockEntityWithoutLevelRenderer}; the adapter therefore forwards the very same
     * {@code renderByItem} call of the shared renderer.
     */
    private interface ItemRenderer {
        void renderByItem(net.minecraft.world.item.ItemStack stack,
                          net.minecraft.world.item.ItemDisplayContext displayContext,
                          com.mojang.blaze3d.vertex.PoseStack pose,
                          net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay);
    }

    private static IClientItemExtensions renderer(ItemRenderer delegate) {
        return new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer adapter;

            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (adapter == null) {
                    var minecraft = Minecraft.getInstance();
                    adapter = new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {
                        @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,
                                net.minecraft.world.item.ItemDisplayContext displayContext,
                                com.mojang.blaze3d.vertex.PoseStack pose,
                                net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
                            delegate.renderByItem(stack, displayContext, pose, buffers, light, overlay);
                        }
                    };
                }
                return adapter;
            }
        };
    }

    /** The client resource reload of the Fabric ResourceManagerHelper. */
    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PreparableReloadListener() {
            @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                                            ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                                            Executor prepareExecutor, Executor applyExecutor) {
                // Even a listener with no preparation work must signal the barrier:
                // SimpleReloadInstance removes preparingListeners only in wait().
                return CompletableFuture.<Void>completedFuture(null)
                        .thenCompose(barrier::wait)
                        .thenRunAsync(ThaumcraftNeoForgeClient::resetClientState, applyExecutor);
            }

            @Override public String getName() {
                return "thaumcraft:warp_effects";
            }
        });
    }

    private static void resetClientState() {
        thaumcraft.client.renderers.item.PrimalWandFocusRenderer.reset();
        ThaumcraftWarpClient.reset();
        ThaumcraftRunicClient.reset();
        PlayerNotifications.reset();
        ThaumcraftTravelGearClient.reset();
        ThaumcraftTaintSporesClient.reset();
    }

    /** The HUD overlay of the Fabric HudRenderCallback. */
    private static void onRenderGui(RenderGuiEvent.Post event) {
        var graphics = event.getGuiGraphics();
        thaumcraft.client.lib.CastingWandHud.render(graphics);
        ThaumcraftWarpClient.renderHud(graphics);
        ThaumcraftRunicClient.renderHud(graphics);
        PlayerNotifications.renderHud(graphics);
        if (Minecraft.getInstance().screen == null) GuiResearchPopup.renderHud(graphics);
    }

    /** The late world render of the Fabric WorldRenderEvents.LAST. */
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var pose = event.getPoseStack();
        if (pose == null) return;
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ThaumcraftGogglesClient.render(pose, buffers, partialTick, event.getCamera());
        ThaumcraftShockClient.render(pose, buffers, partialTick, event.getCamera());
        ThaumcraftExcavationClient.render(pose, buffers, partialTick, event.getCamera());
        ThaumcraftArchitectClient.render(pose, buffers, partialTick, event.getCamera());
        ThaumcraftGolemClient.renderMarkers(pose, buffers, partialTick, event.getCamera());
        buffers.endBatch(RenderType.lines());
        thaumcraft.client.lib.ScanOverlay.render(pose, partialTick);
    }

    /** The disconnect reset of the Fabric ClientPlayConnectionEvents.DISCONNECT. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientState();
        // The common network layer owns the original's client-config restore
        // (oracle EventHandlerNetwork.clientLogsOut) through the Architectury
        // client event; this call is the loader's own belt-and-braces path
        // and is idempotent.
        thaumcraft.common.config.ThaumcraftSettings.resetClient();
    }
}
