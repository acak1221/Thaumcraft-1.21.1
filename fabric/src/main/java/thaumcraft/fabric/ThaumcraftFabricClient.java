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
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_stabilizer_bubble"),
                DefaultVertexFormat.POSITION_TEX_COLOR, shader -> {
                    TileNodeStabilizerRenderer.setBubbleShader(shader);
                    VisNetClientEffects.setShader(shader);
                }));
        ClientSetup.init();
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, ticks) ->
                thaumcraft.client.ThaumcraftWarpClient.renderHud(graphics));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            thaumcraft.client.ThaumcraftWarpClient.reset();
            thaumcraft.common.config.ThaumcraftSettings.resetClient();
        });
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override public net.minecraft.resources.ResourceLocation getFabricId() {
                        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "warp_effects");
                    }
                    @Override public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager resources) {
                        thaumcraft.client.ThaumcraftWarpClient.reset();
                    }
                });
        // The retained shell is shared with blocks, but actual item contents
        // and labels must come from this stack, never a synthetic world tile.
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftContent.JAR.get(), JarItemRenderer::render);
        BuiltinItemRendererRegistry.INSTANCE.register(ThaumcraftContent.VOID_JAR.get(), JarItemRenderer::render);
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
            thaumcraft.client.ThaumcraftGolemClient.renderMarkers(context.matrixStack(), buffers, partialTick, context.camera());
            buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());
        });
    }
}
