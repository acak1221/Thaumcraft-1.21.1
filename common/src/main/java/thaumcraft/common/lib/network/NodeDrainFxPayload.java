package thaumcraft.common.lib.network;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Ephemeral S2C appearance of an actual node-to-wand transfer, never vis authority. */
public record NodeDrainFxPayload(ResourceLocation dimension, BlockPos node, int actorId, UUID actorUuid,
        int rgb, boolean active) implements CustomPacketPayload {
    public static final Type<NodeDrainFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_drain_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeDrainFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeInt(payload.node.getX()); buffer.writeInt(payload.node.getY()); buffer.writeInt(payload.node.getZ());
                buffer.writeVarInt(payload.actorId); buffer.writeUUID(payload.actorUuid);
                buffer.writeInt(payload.rgb); buffer.writeBoolean(payload.active);
            }, buffer -> new NodeDrainFxPayload(buffer.readResourceLocation(), new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()),
                    buffer.readVarInt(), buffer.readUUID(), buffer.readInt(), buffer.readBoolean()));

    public NodeDrainFxPayload {
        Objects.requireNonNull(dimension, "dimension"); Objects.requireNonNull(node, "node");
        Objects.requireNonNull(actorUuid, "actorUuid"); node = node.immutable();
        if (actorId < 0 || Math.abs((long) node.getX()) > 30_000_000 || Math.abs((long) node.getZ()) > 30_000_000
                || Math.abs((long) node.getY()) > 20_000_000 || rgb < 0 || rgb > 0xffffff)
            throw new IllegalArgumentException("Invalid node drain appearance");
    }

    @Override public Type<NodeDrainFxPayload> type() { return TYPE; }
}
