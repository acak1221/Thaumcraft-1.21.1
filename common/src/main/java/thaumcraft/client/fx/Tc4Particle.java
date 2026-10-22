package thaumcraft.client.fx;

/** Opt in only effects originally submitted to TC4's engine, not vanilla's. */
public interface Tc4Particle {
    /** Original getFXLayer; -1 leaves a vanilla-owned variant in vanilla's engine. */
    default int tc4Layer() { return 0; }
}
