/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the Fabric glue of the Kami gear's client side.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import thaumic.tinkerer.client.core.handler.kami.PlacementMirrorPredictionRenderer;
import thaumic.tinkerer.client.render.kami.WingsLayer;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * The two client hooks of the Kami gear that Architectury has no event for:
 *
 * <ul>
 *   <li>the wings of the Robes of the Stratosphere are a render layer, added to every player renderer
 *       ({@link LivingEntityFeatureRendererRegistrationCallback});</li>
 *   <li>the phantom blocks of the Placement Mirror are drawn after the translucent terrain
 *       ({@link WorldRenderEvents#AFTER_TRANSLUCENT}), 1.7.10's {@code RenderWorldLastEvent}, when
 *       {@code ConfigHandler.showPlacementMirrorBlocks} is on.</li>
 * </ul>
 *
 * <p>Either list this class as a {@code client} entrypoint of {@code fabric.mod.json} or call {@link #register()} once from
 * {@code ThaumicTinkererFabricClient}; doing both registers everything twice.</p>
 */
public final class KamiGearFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        register();
    }

    /** Registers the two hooks; does nothing while the Kami content is off. */
    public static void register() {
        if (!ConfigHandler.enableKami) return;

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (renderer instanceof PlayerRenderer players) {
                        helper.register(new WingsLayer(players, context.getModelSet()));
                    }
                });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (!ConfigHandler.showPlacementMirrorBlocks) return;
            if (context.matrixStack() == null || !(context.consumers() instanceof MultiBufferSource.BufferSource buffers)) return;
            PlacementMirrorPredictionRenderer.render(context.matrixStack(), buffers, context.camera());
        });
    }
}
