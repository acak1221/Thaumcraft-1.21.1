package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.fx.particles.BlockRuneParticle;
import thaumcraft.common.lib.network.ScanRuneFxPayload;
import thaumcraft.common.lib.research.ScanRunes;

/**
 * The client half of the rune particles of the original thaumometer scan.
 *
 * <p>Oracle {@code thaumcraft/client/ClientProxy.java:918-922} creates exactly ONE
 * {@code FXBlockRunes} per call, at {@code (x + 0.5, y + 0.5, z + 0.5)} of the point of the
 * caller. The colour of the original was drawn from the client world random (oracle
 * {@code thaumcraft/common/items/relics/ItemThaumometer.java:92,103,128}), so the native client
 * draws it from its own random and the server world random stays untouched.</p>
 */
public final class ScanRuneClientEffects {
    private ScanRuneClientEffects() {}

    /** The visual answer of one payload; nothing here changes the world. */
    public static void accept(ScanRuneFxPayload payload) {
        var client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        float[] colour = ScanRunes.colour(level.getRandom());
        client.particleEngine.add(new BlockRuneParticle(level,
                payload.x() + 0.5D, payload.y() + 0.5D, payload.z() + 0.5D,
                colour[0], colour[1], colour[2], payload.duration(), payload.gravity()));
    }
}
