package thaumcraft.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** TC4 queue ownership; native effects retain their shaders, atlases and geometry. */
public final class ParticleEngine {
    private static final ParticleLayers<ResourceKey<Level>, Particle> PARTICLES = new ParticleLayers<>();

    private ParticleEngine() {}

    public static boolean add(ClientLevel level, Particle particle) {
        if (level == null || !(particle instanceof Tc4Particle effect) || effect.tc4Layer() < 0) return false;
        ClientLevel source = ((thaumcraft.common.mixin.client.ParticleLevelAccess) particle).thaumcraft$level();
        PARTICLES.add(source.dimension(), effect.tc4Layer(), particle);
        return true;
    }

    public static void tick(ClientLevel level) {
        if (level != null) PARTICLES.tick(level.dimension(), Particle::tick, Particle::isAlive);
    }

    public static void render(ClientLevel level, Camera camera, float partialTick) {
        if (level != null) PARTICLES.forEach(level.dimension(), particle -> particle.render(null, camera, partialTick));
    }

    /** Level replacement, disconnect and resource reload release world-owned particles. */
    public static void clear() { PARTICLES.clear(); }
}
