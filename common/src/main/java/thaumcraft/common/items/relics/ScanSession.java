package thaumcraft.common.items.relics;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Transient server-owned use session; intentionally not written into item saves. */
public record ScanSession(String signature, long startTick) {
    public static final StreamCodec<ByteBuf, ScanSession> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ScanSession::signature,
            ByteBufCodecs.VAR_LONG, ScanSession::startTick,
            ScanSession::new);
}
