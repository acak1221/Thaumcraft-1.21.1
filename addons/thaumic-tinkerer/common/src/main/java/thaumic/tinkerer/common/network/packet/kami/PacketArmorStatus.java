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
package thaumic.tinkerer.common.network.packet.kami;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import thaumic.tinkerer.common.lib.LibMisc;

/** The server tells a client that joins whether its awakened armour is on (server to client; 1.7.10 sent {@code PacketToggleArmor} from {@code PlayerTracker}). */
public record PacketArmorStatus(boolean armorStatus) implements CustomPacketPayload {
    public static final Type<PacketArmorStatus> TYPE = new Type<>(LibMisc.id("armor_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArmorStatus> CODEC = StreamCodec
            .composite(ByteBufCodecs.BOOL, PacketArmorStatus::armorStatus, PacketArmorStatus::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
