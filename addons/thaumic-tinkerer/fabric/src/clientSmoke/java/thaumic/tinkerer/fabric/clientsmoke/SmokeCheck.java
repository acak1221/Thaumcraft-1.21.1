package thaumic.tinkerer.fabric.clientsmoke;

/**
 * One named check of the resource phase. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the
 * add-on, not distributed.
 *
 * <p>A check records its results in {@link SmokeRun#report()}. If it throws, the runner records a harness error under the
 * check's name and goes on with the next check, so one broken check never hides the others.</p>
 */
interface SmokeCheck {
    String PHASE = "resources";

    String name();

    void run(SmokeRun run) throws Exception;
}
