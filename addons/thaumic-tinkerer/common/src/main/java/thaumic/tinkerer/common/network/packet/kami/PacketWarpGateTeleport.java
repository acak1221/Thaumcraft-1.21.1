/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 7:21:05 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet.kami;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Client to server: a click on a destination of the gateway's map (1.7.10 {@code PacketWarpGateTeleport}) teleports the
 * sender to the pearl in that slot.
 *
 * <p>The original accepted a packet for any gateway of the world from any player. The destinations screen is opened by
 * standing on the gateway, so the sender has to be within eight blocks of the gateway it names (the reach of the
 * gateway's own menu); a client that is not near cannot teleport through a gateway it never visited.</p>
 */
public record PacketWarpGateTeleport(BlockPos pos, int index) implements CustomPacketPayload {
    public static final Type<PacketWarpGateTeleport> TYPE = new Type<>(LibMisc.id("warp_gate_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketWarpGateTeleport> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketWarpGateTeleport::pos,
            ByteBufCodecs.VAR_INT, PacketWarpGateTeleport::index,
            PacketWarpGateTeleport::new);

    public PacketWarpGateTeleport(TileWarpGate tile, int index) {
        this(tile.getBlockPos(), index);
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

    /** {@code onMessage} on the server. Returns whether a teleport happened. */
    public boolean apply(ServerPlayer sender) {
        ServerLevel level = sender.serverLevel();
        if (!level.isLoaded(pos)) return false;
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof TileWarpGate gate)) return false;
        if (sender.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64) return false;
        return gate.teleportPlayer(sender, index);
    }
}
