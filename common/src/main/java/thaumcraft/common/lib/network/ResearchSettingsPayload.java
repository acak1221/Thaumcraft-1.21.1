package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.config.ResearchSettings;

/** Fixed seven-field server snapshot, at most eleven bytes; no client-authoritative configuration channel. */
public record ResearchSettingsPayload(ResearchSettings settings) implements CustomPacketPayload {
    public static final Type<ResearchSettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSettingsPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> {
        ResearchSettings value = payload.settings;
        buffer.writeByte((value.cheatSheet() ? 1 : 0) | (value.wardedStone() ? 2 : 0) | (value.mirrors() ? 4 : 0)
                | (value.hardNodes() ? 8 : 0) | (value.wuss() ? 16 : 0));
        buffer.writeVarInt(value.difficulty()); buffer.writeVarInt(value.aspectCap());
    }, buffer -> {
        int flags = buffer.readUnsignedByte();
        if ((flags & ~31) != 0) throw new IllegalArgumentException("Unknown research settings flags");
        return new ResearchSettingsPayload(new ResearchSettings((flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0,
                (flags & 8) != 0, (flags & 16) != 0, buffer.readVarInt(), buffer.readVarInt()));
    });
    public ResearchSettingsPayload { java.util.Objects.requireNonNull(settings); }
    @Override public Type<ResearchSettingsPayload> type() { return TYPE; }
}
