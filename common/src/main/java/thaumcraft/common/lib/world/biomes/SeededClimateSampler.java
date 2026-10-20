package thaumcraft.common.lib.world.biomes;

/** Native RandomState seed attached to its immutable per-world sampler. */
public interface SeededClimateSampler {
    long thaumcraft$worldSeed();
    void thaumcraft$setWorldSeed(long seed);
}
