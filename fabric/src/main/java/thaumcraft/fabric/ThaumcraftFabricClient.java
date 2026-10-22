package thaumcraft.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import thaumcraft.client.ClientSetup;
import thaumcraft.client.ThaumcraftGogglesClient;
import thaumcraft.client.ThaumcraftShockClient;
import thaumcraft.client.ThaumcraftExcavationClient;
import thaumcraft.client.renderers.item.JarItemRenderer;
import thaumcraft.client.renderers.tile.TileTubeRenderer;
import thaumcraft.client.renderers.tile.TileTubeBufferRenderer;
import thaumcraft.client.renderers.tile.TileCentrifugeRenderer;
import thaumcraft.client.renderers.tile.TileEssentiaCrystalizerRenderer;
import thaumcraft.client.renderers.tile.TileNodeStabilizerRenderer;
import thaumcraft.client.renderers.tile.TileNodeConverterRenderer;
import thaumcraft.client.renderers.tile.TileVisRelayRenderer;
import thaumcraft.client.VisNetClientEffects;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftNodeDevices;

public final class ThaumcraftFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "linked_mirror"),
                DefaultVertexFormat.POSITION_TEX_COLOR, thaumcraft.client.renderers.tile.LinkedMirrorField::setShader));
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "monster_lit_particle"),
                DefaultVertexFormat.PARTICLE, thaumcraft.client.fx.particles.MonsterSpriteParticle::setLitShader));
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_stabilizer_bubble"),
                DefaultVertexFormat.POSITION_TEX_COLOR, shader -> {
                    TileNodeStabilizerRenderer.setBubbleShader(shader);
                    VisNetClientEffects.setShader(shader);
                }));
        ClientSetup.init();
        // The original wand, the void robes and the fortress armour use the TC4 models, not the
        // vanilla baked item and armour models.
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftContent.WAND.get(),
                thaumcraft.client.ThaumcraftGearModelsClient::renderWand);
        // Original ItemNodeRenderer: the item of the aura node is drawn as the node itself.
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftContent.AURA_NODE.get().asItem(),
                thaumcraft.client.renderers.tile.ItemNodeRenderer::render);
        net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                thaumcraft.client.ThaumcraftGearModelsClient::renderRobe,
                thaumcraft.common.config.ThaumcraftEquipment.VOID_ROBE_HELMET.get(),
                thaumcraft.common.config.ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get(),
                thaumcraft.common.config.ThaumcraftEquipment.VOID_ROBE_LEGGINGS.get());
        net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                thaumcraft.client.ThaumcraftGearModelsClient::renderFortressArmor,
                thaumcraft.common.config.ThaumcraftEquipment.FORTRESS_HELMET.get(),
                thaumcraft.common.config.ThaumcraftEquipment.FORTRESS_CHESTPLATE.get(),
                thaumcraft.common.config.ThaumcraftEquipment.FORTRESS_LEGGINGS.get());
        thaumcraft.fabric.client.FabricSpaClient.init();
        thaumcraft.fabric.client.FabricTaintFibresModel.init();
        thaumcraft.fabric.client.FabricPositionBlockModels.init();
        thaumcraft.fabric.client.FabricTravelGearClient.init();
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftAdvancedFurnace.FURNACE_ITEM.get(),
                thaumcraft.client.renderers.tile.TileAlchemyFurnaceAdvancedRenderer::renderItem);
        for (var crystal : thaumcraft.common.config.ThaumcraftCrystalClusters.blocks())
            BuiltinItemRendererRegistry.INSTANCE.register(crystal, thaumcraft.client.renderers.tile.TileCrystalRenderer::renderItem);
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, ticks) -> {
            thaumcraft.client.ThaumcraftWarpClient.renderHud(graphics);
            thaumcraft.client.ThaumcraftRunicClient.renderHud(graphics);
            thaumcraft.client.lib.PlayerNotifications.renderHud(graphics);
            if (Minecraft.getInstance().screen == null) thaumcraft.client.gui.GuiResearchPopup.renderHud(graphics);
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // The common layer owns the original's client restore (oracle
            // EventHandlerNetwork.clientLogsOut) through the Architectury client event; this call is the
            // loader's own belt-and-braces path and is idempotent.
            thaumcraft.client.ClientStateReset.reset();
        });
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override public net.minecraft.resources.ResourceLocation getFabricId() {
                        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "warp_effects");
                    }
                    @Override public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager resources) {
                        thaumcraft.client.ThaumcraftWarpClient.reset();
                        thaumcraft.client.ThaumcraftRunicClient.reset();
                        thaumcraft.client.lib.PlayerNotifications.reset();
                        thaumcraft.client.ThaumcraftTravelGearClient.reset();
                        thaumcraft.client.ThaumcraftTaintSporesClient.reset();
                    }
                });
        // The retained shell is shared with blocks, but actual item contents
        // and labels must come from this stack, never a synthetic world tile.
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftContent.JAR.get(), JarItemRenderer::render);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftContent.VOID_JAR.get(), JarItemRenderer::render);
        // Original ItemJarNodeRenderer: the node of the jar in three crossed planes
        // inside the glass shell of the item.
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftEldritchBlocks.JAR_NODE_ITEM.get(),
                thaumcraft.client.renderers.tile.ItemJarNodeRenderer::render);
        // Original ItemThaumometerRenderer: the thaumometer is the three
        // dimensional scanner of the original, not a flat sprite.
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftContent.THAUMOMETER.get(),
                thaumcraft.client.renderers.item.ItemThaumometerRenderer::render);
        // Original ItemBannerRenderer: the banner item carries its colour and its
        // aspect and is drawn with the banner model instead of a plain cube.
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftEldritchBlocks.BANNER_ITEM.get(),
                thaumcraft.client.renderers.item.ItemBannerRenderer::render);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.ESSENTIA_VALVE.get(), TileTubeRenderer::renderValveItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.ESSENTIA_FILTER.get(), TileTubeRenderer::renderAdvancedItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.ESSENTIA_RESTRICT.get(), TileTubeRenderer::renderAdvancedItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.ESSENTIA_ONEWAY.get(), TileTubeRenderer::renderAdvancedItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.ESSENTIA_BUFFER.get(), TileTubeBufferRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.CENTRIFUGE.get(), TileCentrifugeRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get(), TileEssentiaCrystalizerRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftNodeDevices.NODE_STABILIZER.get(), TileNodeStabilizerRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftNodeDevices.NODE_TRANSDUCER.get(), TileNodeConverterRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftNodeDevices.VIS_RELAY.get(), TileVisRelayRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftVisCharging.VIS_CHARGE_RELAY_ITEM.get(),
                thaumcraft.client.renderers.tile.TileMagicWorkbenchChargerRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftArcaneBore.ARCANE_BORE.get(),
                thaumcraft.client.renderers.tile.TileArcaneBoreRenderer::renderItem);
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftArcaneBore.ARCANE_BORE_BASE.get(),
                thaumcraft.client.renderers.tile.TileArcaneBoreBaseRenderer::renderItem);
        var hungryChest = new thaumcraft.common.tiles.TileChestHungry(net.minecraft.core.BlockPos.ZERO,
                thaumcraft.common.config.ThaumcraftHungryChest.HUNGRY_CHEST.get().defaultBlockState());
        BuiltinItemRendererRegistry.INSTANCE.register(thaumcraft.common.config.ThaumcraftHungryChest.HUNGRY_CHEST_ITEM.get(),
                (stack, context, pose, buffers, light, overlay) -> Minecraft.getInstance().getBlockEntityRenderDispatcher()
                        .renderItem(hungryChest, pose, buffers, light, overlay));
        // Late rendering is required for revealed nodes behind opaque terrain.
        // Camera translation and buffer flushing belong to the shared callback.
        WorldRenderEvents.LAST.register(context -> {
            var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
            ThaumcraftGogglesClient.render(context.matrixStack(), buffers, partialTick, context.camera());
            ThaumcraftShockClient.render(context.matrixStack(), buffers, partialTick, context.camera());
            ThaumcraftExcavationClient.render(context.matrixStack(), buffers, partialTick, context.camera());
            thaumcraft.client.ThaumcraftArchitectClient.render(context.matrixStack(), buffers, partialTick, context.camera());
            thaumcraft.client.ThaumcraftGolemClient.renderMarkers(context.matrixStack(), buffers, partialTick, context.camera());
            buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());
            thaumcraft.client.lib.ScanOverlay.render(context.matrixStack(), partialTick);
        });
    }
}
