package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumcraft.common.lib.network.ClientFxPayload;

/**
 * The client half of the server senders of {@code thaumcraft.api.ThaumcraftFX} (sparkle, burst, runes, smoke spiral): the
 * payload becomes the call of the public facade it stands for, so what a server asks for and what a client mod draws by itself
 * are the same effect.
 */
public final class ClientFxReceiver {
    private ClientFxReceiver() {}

    /** The visual answer of one payload; nothing here changes the world. */
    public static void accept(ClientFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        switch (payload.kind()) {
            case ClientFxPayload.SPARKLE -> ThaumcraftClientFX.sparkle(level, payload.x(), payload.y(), payload.z(), payload.a(), payload.i(), payload.b());
            case ClientFxPayload.BURST -> ThaumcraftClientFX.burst(level, payload.x(), payload.y(), payload.z(), payload.a());
            case ClientFxPayload.RUNES -> ThaumcraftClientFX.blockRunes(level, payload.x(), payload.y(), payload.z(),
                    payload.a(), payload.b(), payload.c(), payload.i(), payload.d());
            case ClientFxPayload.SMOKE_SPIRAL -> ThaumcraftClientFX.smokeSpiral(level, payload.x(), payload.y(), payload.z(),
                    payload.a(), payload.i(), payload.j(), payload.k());
            default -> { /* The payload codec rejects other kinds. */ }
        }
    }
}
