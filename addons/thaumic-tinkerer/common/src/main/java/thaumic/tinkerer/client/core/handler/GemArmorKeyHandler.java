/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.core.handler;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import thaumic.tinkerer.client.core.handler.kami.KamiArmorClientHandler;
import thaumic.tinkerer.common.core.handler.kami.KamiArmorHandler;

/**
 * The key that switches the awakened armour on and off ("Toggle Awakened Armor", category "Thaumic Tinkerer"). The pinned
 * 1.7.10 source binds it to no key by default (the Thaumonomicon pages still say U); it is bound to nothing here too.
 */
public final class GemArmorKeyHandler {
    private static final KeyMapping SPECIAL_ABILITY = new KeyMapping("ttmisc.toggleArmor", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "ttmisc.keyCategory");

    private GemArmorKeyHandler() {}

    public static void register() {
        KeyMappingRegistry.register(SPECIAL_ABILITY);
        ClientTickEvent.CLIENT_POST.register(GemArmorKeyHandler::onClientTick);
        // A new world starts with the armour on until the server says otherwise.
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> KamiArmorHandler.setClientStatus(true));
    }

    private static void onClientTick(Minecraft client) {
        while (SPECIAL_ABILITY.consumeClick()) {
            if (client.player != null && KamiArmorHandler.wearsAwakenedArmor(client.player)) {
                KamiArmorClientHandler.setStatus(!KamiArmorHandler.clientStatus());
            }
        }
    }
}
