/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 17:02:18 (GMT)]
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
import thaumic.tinkerer.common.block.tile.container.ContainerAnimationTablet;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Client to server: the click mode and the redstone mode chosen in the Animation Tablet's screen (1.7.10
 * {@code PacketTabletButton}: the position and the two flags).
 */
public record PacketTabletButton(BlockPos pos, boolean leftClick, boolean redstone) implements CustomPacketPayload {
    public static final Type<PacketTabletButton> TYPE = new Type<>(LibMisc.id("tablet_button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketTabletButton> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketTabletButton::pos,
            ByteBufCodecs.BOOL, PacketTabletButton::leftClick,
            ByteBufCodecs.BOOL, PacketTabletButton::redstone,
            PacketTabletButton::new);

    public PacketTabletButton(TileAnimationTablet tile) {
        this(tile.getBlockPos(), tile.leftClick, tile.redstone);
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

    /** {@code onMessage} on the server: the tablet takes over the two modes. Returns whether it did. */
    public boolean apply(ServerPlayer sender) {
        TileAnimationTablet tile = PacketTile.resolve(sender, pos, TileAnimationTablet.class, ContainerAnimationTablet.class,
                menu -> menu.tablet);
        if (tile == null) return false;
        tile.setLeftClick(leftClick);
        tile.setRedstone(redstone);
        // The powered mark follows the redstone mode (see BlockAnimationTablet#neighborChanged).
        tile.getLevel().neighborChanged(tile.getBlockPos(), tile.getBlockState().getBlock(), tile.getBlockPos());
        return true;
    }
}
