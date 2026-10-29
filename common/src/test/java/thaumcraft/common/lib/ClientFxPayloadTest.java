package thaumcraft.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.ClientFxNetworking;
import thaumcraft.common.lib.network.ClientFxPayload;

/**
 * The server-to-client payload of the client effect facade: each kind survives the wire with its numbers, and a payload the
 * client could not draw is refused while it is read.
 */
class ClientFxPayloadTest {
    private static final ResourceLocation DIMENSION = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    private static ClientFxPayload roundTrip(ClientFxPayload payload) {
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ClientFxPayload.STREAM_CODEC.encode(wire, payload);
            ClientFxPayload read = ClientFxPayload.STREAM_CODEC.decode(wire);
            assertEquals(0, wire.readableBytes(), "the whole payload was read");
            return read;
        } finally {
            wire.release();
        }
    }

    @Test void aSparkleSurvivesTheWire() {
        var sent = ClientFxPayload.sparkle(DIMENSION, 12.5, 64.25, -3.75, 1.5F, 4, -0.02F);
        var read = roundTrip(sent);
        assertEquals(sent, read);
        assertEquals(ClientFxPayload.SPARKLE, read.kind());
        assertEquals(1.5F, read.a(), "size");
        assertEquals(-0.02F, read.b(), "gravity");
        assertEquals(4, read.i(), "type");
    }

    @Test void aBurstSurvivesTheWire() {
        var read = roundTrip(ClientFxPayload.burst(DIMENSION, 1, 2, 3, 0.5F));
        assertEquals(ClientFxPayload.BURST, read.kind());
        assertEquals(0.5F, read.a());
        assertEquals(2.0D, read.y());
    }

    @Test void runesSurviveTheWire() {
        var read = roundTrip(ClientFxPayload.runes(DIMENSION, 10, 70.7, 10, 0.2F, 0.1F, 0.9F, 20, -0.02F));
        assertEquals(ClientFxPayload.RUNES, read.kind());
        assertEquals(0.2F, read.a(), "red");
        assertEquals(0.1F, read.b(), "green");
        assertEquals(0.9F, read.c(), "blue");
        assertEquals(-0.02F, read.d(), "gravity");
        assertEquals(20, read.i(), "duration");
    }

    @Test void aSmokeSpiralSurvivesTheWireAndKeepsOnlyTwentyFourBitsOfColour() {
        var read = roundTrip(ClientFxPayload.smokeSpiral(DIMENSION, 0, 64, 0, 1.5F, 270, 63, 0xFF9E2FF0));
        assertEquals(ClientFxPayload.SMOKE_SPIRAL, read.kind());
        assertEquals(1.5F, read.a(), "radius");
        assertEquals(270, read.i(), "start");
        assertEquals(63, read.j(), "lowest y");
        assertEquals(0x9E2FF0, read.k(), "colour");
    }

    @Test void thePayloadAppliesTheRulesOfTheFacade() {
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.sparkle(DIMENSION, Double.NaN, 0, 0, 1.0F, 2, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.sparkle(DIMENSION, 0, 0, 0, 1.0F, 8, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.sparkle(DIMENSION, 0, 0, 0, 0.0F, 2, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.burst(DIMENSION, 0, 0, 0, -1.0F));
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.runes(DIMENSION, 0, 0, 0, 1, 1, 1, 0, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.runes(DIMENSION, 0, 0, 0, Float.POSITIVE_INFINITY, 1, 1, 10, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.smokeSpiral(DIMENSION, 0, 0, 0, 100.0F, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClientFxPayload(DIMENSION, 4, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0), "an unknown kind");
        assertThrows(IllegalArgumentException.class, () -> new ClientFxPayload(DIMENSION, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> ClientFxPayload.burst(null, 0, 0, 0, 1.0F));
    }

    @Test void aMalformedPayloadIsRefusedWhileItIsRead() {
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            wire.writeResourceLocation(DIMENSION);
            wire.writeVarInt(ClientFxPayload.BURST);
            wire.writeDouble(0); wire.writeDouble(0); wire.writeDouble(0);
            wire.writeFloat(Float.NaN); wire.writeFloat(0); wire.writeFloat(0); wire.writeFloat(0);
            wire.writeInt(0); wire.writeInt(0); wire.writeInt(0);
            assertThrows(IllegalArgumentException.class, () -> ClientFxPayload.STREAM_CODEC.decode(wire));
        } finally {
            wire.release();
        }
    }

    @Test void theServerSendsToThirtyTwoBlocksAsTheBlockEffects() {
        assertEquals(32.0D, ClientFxNetworking.RANGE);
        assertEquals("thaumcraft:client_fx", ClientFxPayload.TYPE.id().toString());
    }
}
