package thaumcraft.common.lib.network;

import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.fx.ClientFxRules;

/**
 * The server-to-client route of the client effect facade ({@code thaumcraft.api.client.ThaumcraftClientFX}): one S2C payload
 * with an explicit kind, in the manner of {@link BlockFxPayload}, for the effects a server wants every nearby client to draw
 * (the sparkle, the burst, the runes and the smoke spiral of the 1.7.10 proxy).
 *
 * <p>The numbers are the arguments of the facade call that the client makes when the payload arrives, so the meaning of the
 * generic slots depends on the kind:</p>
 * <ul>
 *   <li>{@link #SPARKLE}: {@code a} size, {@code b} gravity, {@code i} type;</li>
 *   <li>{@link #BURST}: {@code a} size;</li>
 *   <li>{@link #RUNES}: {@code a}, {@code b}, {@code c} red, green, blue, {@code d} gravity, {@code i} duration;</li>
 *   <li>{@link #SMOKE_SPIRAL}: {@code a} radius, {@code i} start angle, {@code j} lowest y, {@code k} colour (0xRRGGBB).</li>
 * </ul>
 * <p>The constructor applies the same rules as the facade ({@link ClientFxRules}), so a payload that decodes is one the client
 * can draw; a malformed one is rejected with {@link IllegalArgumentException} while it is read.</p>
 */
public record ClientFxPayload(ResourceLocation dimension, int kind, double x, double y, double z,
                              float a, float b, float c, float d, int i, int j, int k) implements CustomPacketPayload {
    /** {@code ThaumcraftClientFX.sparkle}. */
    public static final int SPARKLE = 0;
    /** {@code ThaumcraftClientFX.burst}. */
    public static final int BURST = 1;
    /** {@code ThaumcraftClientFX.blockRunes}. */
    public static final int RUNES = 2;
    /** {@code ThaumcraftClientFX.smokeSpiral}. */
    public static final int SMOKE_SPIRAL = 3;

    public static final Type<ClientFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "client_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientFxPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeVarInt(payload.kind);
                buffer.writeDouble(payload.x); buffer.writeDouble(payload.y); buffer.writeDouble(payload.z);
                buffer.writeFloat(payload.a); buffer.writeFloat(payload.b); buffer.writeFloat(payload.c); buffer.writeFloat(payload.d);
                buffer.writeInt(payload.i); buffer.writeInt(payload.j); buffer.writeInt(payload.k);
            },
            buffer -> new ClientFxPayload(buffer.readResourceLocation(), buffer.readVarInt(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt()));

    public ClientFxPayload {
        Objects.requireNonNull(dimension, "dimension");
        switch (kind) {
            case SPARKLE -> ClientFxRules.sparkle(x, y, z, a, i, b);
            case BURST -> ClientFxRules.burst(x, y, z, a);
            case RUNES -> ClientFxRules.blockRunes(x, y, z, a, b, c, i, d);
            case SMOKE_SPIRAL -> ClientFxRules.smokeSpiral(x, y, z, a);
            default -> throw new IllegalArgumentException("Unknown client effect kind " + kind);
        }
    }

    /** {@code ThaumcraftClientFX.sparkle} as a payload. */
    public static ClientFxPayload sparkle(ResourceLocation dimension, double x, double y, double z, float size, int type, float gravity) {
        return new ClientFxPayload(dimension, SPARKLE, x, y, z, size, gravity, 0, 0, type, 0, 0);
    }

    /** {@code ThaumcraftClientFX.burst} as a payload. */
    public static ClientFxPayload burst(ResourceLocation dimension, double x, double y, double z, float size) {
        return new ClientFxPayload(dimension, BURST, x, y, z, size, 0, 0, 0, 0, 0, 0);
    }

    /** {@code ThaumcraftClientFX.blockRunes} as a payload. */
    public static ClientFxPayload runes(ResourceLocation dimension, double x, double y, double z,
                                        float red, float green, float blue, int duration, float gravity) {
        return new ClientFxPayload(dimension, RUNES, x, y, z, red, green, blue, gravity, duration, 0, 0);
    }

    /** {@code ThaumcraftClientFX.smokeSpiral} as a payload. */
    public static ClientFxPayload smokeSpiral(ResourceLocation dimension, double x, double y, double z,
                                              float radius, int start, int minY, int rgb) {
        return new ClientFxPayload(dimension, SMOKE_SPIRAL, x, y, z, radius, 0, 0, 0, start, minY, ClientFxRules.rgb(rgb));
    }

    @Override public Type<ClientFxPayload> type() { return TYPE; }
}
