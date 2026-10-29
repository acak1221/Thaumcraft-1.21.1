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
 * <p>{@code KamiGearClient.register()} calls {@link #register()} by name on Fabric, so nothing has to be listed in
 * {@code fabric.mod.json}. {@link #register()} registers only once, so listing this class as a {@code client} entrypoint
 * as well, or calling it from {@code ThaumicTinkererFabricClient}, is harmless.</p>
 */
public final class KamiGearFabricClient implements ClientModInitializer {
    private static boolean registered;

    @Override
    public void onInitializeClient() {
        register();
    }

    /** Registers the two hooks (once); does nothing while the Kami content is off. */
    public static synchronized void register() {
        if (registered || !ConfigHandler.enableKami) return;
        registered = true;

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
