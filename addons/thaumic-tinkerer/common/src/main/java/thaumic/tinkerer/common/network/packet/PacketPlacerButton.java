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
package thaumic.tinkerer.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import thaumic.tinkerer.common.block.tile.TileRPlacer;
import thaumic.tinkerer.common.block.tile.container.ContainerRemotePlacer;
import thaumic.tinkerer.common.lib.LibMisc;

/** Client to server: the distance (1 to 4 blocks) chosen in the Remote Placer's screen (1.7.10 {@code PacketPlacerButton}). */
public record PacketPlacerButton(BlockPos pos, int blocks) implements CustomPacketPayload {
    public static final Type<PacketPlacerButton> TYPE = new Type<>(LibMisc.id("placer_button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPlacerButton> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketPlacerButton::pos,
            ByteBufCodecs.VAR_INT, PacketPlacerButton::blocks,
            PacketPlacerButton::new);

    public PacketPlacerButton(TileRPlacer tile) {
        this(tile.getBlockPos(), tile.blocks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Registers the receiver (both sides, from the mod initialisation). */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TYPE, STREAM_CODEC, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender) payload.apply(sender);
        }));
    }

    /** {@code onMessage} on the server: the placer takes over the distance. Returns whether it did. */
    public boolean apply(ServerPlayer sender) {
        TileRPlacer tile = PacketTile.resolve(sender, pos, TileRPlacer.class, ContainerRemotePlacer.class, menu -> menu.placer);
        if (tile == null) return false;
        tile.setBlocks(blocks);
        return true;
    }
}
