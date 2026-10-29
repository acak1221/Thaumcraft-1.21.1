/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 5:59:27 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet.kami;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerWarpGate;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.network.packet.PacketTile;

/**
 * Client to server: the button of the gateway's screen (1.7.10 {@code PacketWarpGateButton}) sets the gate's
 * {@code locked} flag. The sender must have that gateway's menu open.
 */
public record PacketWarpGateButton(BlockPos pos, boolean locked) implements CustomPacketPayload {
    public static final Type<PacketWarpGateButton> TYPE = new Type<>(LibMisc.id("warp_gate_button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketWarpGateButton> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketWarpGateButton::pos,
            ByteBufCodecs.BOOL, PacketWarpGateButton::locked,
            PacketWarpGateButton::new);

    public PacketWarpGateButton(TileWarpGate tile) {
        this(tile.getBlockPos(), tile.locked);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Registers the receiver (both sides, from the mod initialisation). */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TYPE, CODEC, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender) payload.apply(sender);
        }));
    }

    /** {@code onMessage} on the server: the gate takes over the flag. Returns whether it did. */
    public boolean apply(ServerPlayer sender) {
        TileWarpGate tile = PacketTile.resolve(sender, pos, TileWarpGate.class, ContainerWarpGate.class, menu -> menu.gate);
        if (tile == null) return false;
        tile.setLocked(locked);
        return true;
    }
}
