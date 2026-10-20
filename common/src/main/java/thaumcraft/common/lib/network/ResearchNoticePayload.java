package thaumcraft.common.lib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Original PacketResearchComplete: @KEY clue versus actual key popup/highlight; never an authority request. */
public record ResearchNoticePayload(String key) implements CustomPacketPayload {
    public static final Type<ResearchNoticePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","research_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ResearchNoticePayload> STREAM_CODEC=StreamCodec.of(
            (buffer,value)->buffer.writeUtf(value.key,256),buffer->new ResearchNoticePayload(buffer.readUtf(256)));
    public ResearchNoticePayload { if(key==null||key.isEmpty()||key.length()>256) throw new IllegalArgumentException("Invalid research notice key"); }
    @Override public Type<ResearchNoticePayload> type(){return TYPE;}
}
