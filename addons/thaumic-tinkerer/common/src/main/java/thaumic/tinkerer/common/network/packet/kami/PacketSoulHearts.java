/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 29, 2013, 9:41:05 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet.kami;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Server to client: how many Soul Hearts (half hearts, 0 to 20) the receiving player has. The client draws them above the
 * food bar ({@code SoulHeartClientHandler}); the client handler is registered by the client registration.
 */
public record PacketSoulHearts(int hearts) implements CustomPacketPayload {
    public static final Type<PacketSoulHearts> TYPE = new Type<>(LibMisc.id("soul_hearts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSoulHearts> CODEC = StreamCodec
            .composite(ByteBufCodecs.INT, PacketSoulHearts::hearts, PacketSoulHearts::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
