package thaumcraft.api.machines;

/**
 * The Brain in a Jar (block {@code thaumcraft:jar_brain}, the original {@code TileJarBrain}): the experience it holds.
 *
 * <p>The jar pulls experience orbs within six blocks towards itself and swallows those inside its block, up to
 * {@link #getXpMax()}. The original peripheral {@code tt_braininajar} read the public field {@code xp}.</p>
 */
public interface IBrainJar {
    /**
     * The stored experience points (not levels). Saved with the block (key {@code XP}) and synced to clients when it
     * changes. It can exceed {@link #getXpMax()} for the rest of the tick in which the jar swallowed several orbs at
     * once; the next server tick cuts it back to the maximum, as the original did. Either side.
     *
     * @return the stored experience, at least zero
     */
    int getXp();

    /**
     * The most experience the jar keeps: 2000 (original {@code xpMax}). Either side.
     *
     * @return the capacity in experience points
     */
    int getXpMax();
}
