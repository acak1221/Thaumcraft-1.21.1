/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the client registrations of the wand foci, called
 * from {@code Foci.registerClient()}. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see
 * addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import thaumic.tinkerer.client.core.handler.DislocationHUDHandler;
import thaumic.tinkerer.common.item.foci.ItemFocusHeal;
import thaumic.tinkerer.common.item.foci.ItemFocusSmelt;

/**
 * Client side of the wand foci: the readout of the Focus of Dislocation beside the wand's dial, and the clean-up of the
 * client's own progress maps (the Mending and Efreet's Flame foci count on both sides) when the client leaves a world.
 *
 * <p>The item models, the ornaments and the depth layers are resources; the wand draws them from the focus's textures.</p>
 */
public final class FociClient {
    private FociClient() {}

    public static void register() {
        DislocationHUDHandler.register();
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            ItemFocusHeal.forgetClient();
            ItemFocusSmelt.forgetClient();
        });
    }
}
