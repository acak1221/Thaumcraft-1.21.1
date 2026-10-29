package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumcraft.common.lib.network.ClientFxPayload;

/**
 * The client half of the server senders of {@code thaumcraft.api.ThaumcraftFX} (sparkle, burst, runes, smoke spiral): the
 * payload becomes the call of the public facade it stands for, so what a server asks for and what a client mod draws by itself
 * are the same effect.
 *
 * <p>The mapping of the payload's generic slots (see {@link ClientFxPayload}) to the arguments of those calls is
 * {@link #dispatch}, a pure function of a payload and a {@link Sink}, so a unit test can record the calls and pin every slot
 * without a client; {@link #accept} only supplies the sink that calls {@link ThaumcraftClientFX} with the client level.</p>
 */
public final class ClientFxReceiver {
    private ClientFxReceiver() {}

    /**
     * The calls of the public facade a payload can stand for, with the level left out. {@link #accept} gives them to
     * {@code ThaumcraftClientFX}; a test gives them to a recorder.
     */
    public interface Sink {
        /** {@code ThaumcraftClientFX.sparkle(level, x, y, z, size, type, gravity)}. */
        void sparkle(double x, double y, double z, float size, int type, float gravity);

        /** {@code ThaumcraftClientFX.burst(level, x, y, z, size)}. */
        void burst(double x, double y, double z, float size);

        /** {@code ThaumcraftClientFX.blockRunes(level, x, y, z, red, green, blue, duration, gravity)}. */
        void blockRunes(double x, double y, double z, float red, float green, float blue, int duration, float gravity);

        /** {@code ThaumcraftClientFX.smokeSpiral(level, x, y, z, radius, start, minY, rgb)}. */
        void smokeSpiral(double x, double y, double z, float radius, int start, int minY, int rgb);
    }

    /** The facade of a client level as a sink: each call is the public {@code ThaumcraftClientFX} call of the same name. */
    private record FacadeSink(Level level) implements Sink {
        @Override public void sparkle(double x, double y, double z, float size, int type, float gravity) {
            ThaumcraftClientFX.sparkle(level, x, y, z, size, type, gravity);
        }

        @Override public void burst(double x, double y, double z, float size) {
            ThaumcraftClientFX.burst(level, x, y, z, size);
        }

        @Override public void blockRunes(double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
            ThaumcraftClientFX.blockRunes(level, x, y, z, red, green, blue, duration, gravity);
        }

        @Override public void smokeSpiral(double x, double y, double z, float radius, int start, int minY, int rgb) {
            ThaumcraftClientFX.smokeSpiral(level, x, y, z, radius, start, minY, rgb);
        }
    }

    /** The visual answer of one payload; nothing here changes the world. */
    public static void accept(ClientFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        dispatch(payload, new FacadeSink(level));
    }

    /**
     * Turns a payload into the call it stands for: the generic slots of the payload are the arguments of that call, in the
     * order {@link ClientFxPayload} documents. An unknown kind does nothing (the payload codec rejects it before it gets here).
     */
    public static void dispatch(ClientFxPayload payload, Sink sink) {
        switch (payload.kind()) {
            case ClientFxPayload.SPARKLE -> sink.sparkle(payload.x(), payload.y(), payload.z(), payload.a(), payload.i(), payload.b());
            case ClientFxPayload.BURST -> sink.burst(payload.x(), payload.y(), payload.z(), payload.a());
            case ClientFxPayload.RUNES -> sink.blockRunes(payload.x(), payload.y(), payload.z(),
                    payload.a(), payload.b(), payload.c(), payload.i(), payload.d());
            case ClientFxPayload.SMOKE_SPIRAL -> sink.smokeSpiral(payload.x(), payload.y(), payload.z(),
                    payload.a(), payload.i(), payload.j(), payload.k());
            default -> { /* The payload codec rejects other kinds. */ }
        }
    }
}
