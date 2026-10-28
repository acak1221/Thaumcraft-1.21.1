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
package thaumic.tinkerer.client.core.handler.kami;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import thaumic.tinkerer.common.core.handler.kami.KamiArmorHandler;
import thaumic.tinkerer.common.network.packet.kami.PacketArmorStatus;
import thaumic.tinkerer.common.network.packet.kami.PacketToggleArmor;

/** The client's side of the awakened armour's switch: the key sets it and tells the server, the server's answer on login sets it. */
public final class KamiArmorClientHandler {

    private KamiArmorClientHandler() {}

    /** 1.7.10 {@code SetStatus}: only while no screen is open. */
    public static void setStatus(boolean status) {
        if (Minecraft.getInstance().screen == null) {
            ToolModeHUDHandler.setTooltip(
                    Component.translatable(status ? "ttmisc.enableAllArmor" : "ttmisc.disableAllArmor"));
            KamiArmorHandler.setClientStatus(status);
            NetworkManager.sendToServer(new PacketToggleArmor(status));
        }
    }

    /** The server tells the status of this player (1.7.10: {@code PacketToggleArmor} from {@code PlayerTracker}). */
    public static void onStatus(PacketArmorStatus packet) {
        KamiArmorHandler.setClientStatus(packet.armorStatus());
    }
}
