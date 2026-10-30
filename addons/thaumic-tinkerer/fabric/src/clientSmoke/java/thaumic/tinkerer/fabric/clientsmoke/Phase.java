package thaumic.tinkerer.fabric.clientsmoke;

/**
 * A stage of the smoke run (resources, world, block rows, ...). Test-only code of the port (no 1.7.10 counterpart); CC
 * BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The run ticks the current phase once per client tick until it returns {@code true}. A phase that throws is recorded as a
 * harness error and the run goes on with the next phase.</p>
 */
interface Phase {
    String name();

    /** True for the phases that need the world of {@link WorldPhase}; they are skipped, and recorded as not run, when it failed. */
    default boolean needsWorld() {
        return false;
    }

    /** @return {@code true} when the phase is finished */
    boolean tick(SmokeRun run) throws Exception;
}
