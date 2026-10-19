package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-to-client native knowledge snapshot; never accepted as a client request. */
public record KnowledgeSyncPayload(CompoundTag state) implements CustomPacketPayload {
    public static final Type<KnowledgeSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "knowledge_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeNbt(payload.state),
            buffer -> {
                CompoundTag tag = buffer.readNbt();
                if (tag == null) throw new IllegalArgumentException("Missing Thaumcraft knowledge snapshot");
                return new KnowledgeSyncPayload(tag);
            });

    public KnowledgeSyncPayload {
        state = Objects.requireNonNull(state, "state").copy();
    }

    @Override
    public CompoundTag state() { return state.copy(); }

    @Override
    public Type<KnowledgeSyncPayload> type() { return TYPE; }
}
