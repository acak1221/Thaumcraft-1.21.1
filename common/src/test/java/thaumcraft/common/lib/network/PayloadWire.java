package thaumcraft.common.lib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Wire-level helpers for the payload codec tests: encode to bytes, decode from literal bytes, always releasing the buffer. */
final class PayloadWire {
    private PayloadWire() { }

    static <T> byte[] bytes(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) { return bytes(codec, value, RegistryAccess.EMPTY); }

    static <T> byte[] bytes(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess access) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        try {
            codec.encode(buffer, value);
            byte[] out = new byte[buffer.readableBytes()];
            buffer.readBytes(out);
            return out;
        } finally {
            buffer.release();
        }
    }

    /** Decodes the literal bytes (each 0..255) and returns the value; the caller learns of trailing bytes only through the codec itself. */
    static <T> T decode(StreamCodec<RegistryFriendlyByteBuf, T> codec, int... literal) { return decode(codec, RegistryAccess.EMPTY, literal); }

    static <T> T decode(StreamCodec<RegistryFriendlyByteBuf, T> codec, RegistryAccess access, int... literal) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        try {
            for (int value : literal) buffer.writeByte(value);
            return codec.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    /** The bytes as unsigned ints, so that an expectation can be written as {@code ints(0, 127, 0x03, 0xE8)}. */
    static int[] unsigned(byte[] bytes) {
        int[] out = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) out[i] = bytes[i] & 0xFF;
        return out;
    }
}
