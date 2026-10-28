/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.core.handler.kami;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumic.tinkerer.common.network.packet.kami.PacketShadowBeam;

/**
 * Draws the path of a Shadowbeam. 1.7.10 spawned a black {@code FXSparkle} (scale 2, rising slowly, five ticks to live) at
 * every step of the beam, 0.15 blocks apart; here a black dust particle is spawned at the same spacing along the segments
 * the server sent.
 */
public final class ShadowBeamClientHandler {
    private static final double SPACING = 0.15;
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.04F, 0.0F, 0.06F), 1.4F);

    private ShadowBeamClientHandler() {}

    public static void show(PacketShadowBeam packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        List<Vec3> points = packet.points();
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 from = points.get(i);
            Vec3 to = points.get(i + 1);
            Vec3 line = to.subtract(from);
            int steps = (int) (line.length() / SPACING);
            for (int step = 0; step <= steps; step++) {
                Vec3 at = from.add(line.scale(steps == 0 ? 0 : (double) step / steps));
                level.addParticle(DUST, at.x, at.y, at.z, 0, 0.01, 0);
            }
        }
    }
}
