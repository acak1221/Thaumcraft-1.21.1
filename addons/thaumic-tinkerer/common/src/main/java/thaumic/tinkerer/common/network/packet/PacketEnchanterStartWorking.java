/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [16 Sep 2013, 21:49:24 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.ContainerEnchanter;
import thaumic.tinkerer.common.lib.LibMisc;

/** Client to server: the player pressed the button that starts the Osmotic Enchanter (1.7.10 {@code PacketEnchanterStartWorking}). */
public record PacketEnchanterStartWorking(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PacketEnchanterStartWorking> TYPE = new Type<>(LibMisc.id("enchanter_start_working"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketEnchanterStartWorking> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketEnchanterStartWorking::pos,
            PacketEnchanterStartWorking::new);

    public PacketEnchanterStartWorking(TileEnchanter tile) {
        this(tile.getBlockPos());
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

    /** {@code onMessage} on the server: the enchanter starts when it has a tool and chosen enchantments. Returns whether it did. */
    public boolean apply(ServerPlayer sender) {
        TileEnchanter tile = PacketTile.resolve(sender, pos, TileEnchanter.class, ContainerEnchanter.class, menu -> menu.enchanter);
        if (tile == null) return false;

        if (!tile.working && !tile.enchantments.isEmpty() && !tile.levels.isEmpty()
                && !tile.getItem(TileEnchanter.SLOT_TOOL).isEmpty()) {
            tile.updateAspectList();
            tile.currentAspects = new AspectList();
            tile.working = true;
            tile.sync();
            return true;
        }
        return false;
    }
}
