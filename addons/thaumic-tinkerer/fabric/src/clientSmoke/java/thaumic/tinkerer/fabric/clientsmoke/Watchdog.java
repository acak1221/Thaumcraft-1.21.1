package thaumic.tinkerer.fabric.clientsmoke;

/**
 * Ends a run that hangs. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not
 * distributed.
 *
 * <p>After the time limit the partial report is written with the status {@code TIMED_OUT} and the game is asked to stop; if it
 * is still running ten seconds later the JVM is halted with exit code 3, so that a hang never leaves a Gradle task running. The
 * Gradle task fails whenever the report does not say {@code COMPLETED}, whatever the exit code of the JVM was. A JVM that ends
 * before the harness reached {@link FinishPhase} (a crash, a kill) is marked {@code ABORTED} by the shutdown hook.</p>
 */
final class Watchdog {
    private Watchdog() {}

    static void start(SmokeRun run, int timeoutSeconds) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!"RUNNING".equals(run.report().harnessStatusText())) return;
            FinishPhase.collectLog(run);
            run.report().harnessStatus("ABORTED", "the JVM ended before the harness finished (crash or kill); see the client log and crash-reports");
            run.report().write();
        }, "tt-clientsmoke-shutdown"));
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(timeoutSeconds * 1000L);
            } catch (InterruptedException e) {
                return;
            }
            if (!"RUNNING".equals(run.report().harnessStatusText())) return;
            SmokeRun.LOG.error("client smoke watchdog: {} s elapsed, stopping", timeoutSeconds);
            FinishPhase.collectLog(run);
            run.report().harnessStatus("TIMED_OUT", "the harness did not finish within " + timeoutSeconds + " s");
            run.report().write();
            try {
                run.client().execute(run.client()::stop);
                Thread.sleep(10_000L);
            } catch (InterruptedException | RuntimeException ignored) {
                // halted below either way
            }
            Runtime.getRuntime().halt(3);
        }, "tt-clientsmoke-watchdog");
        thread.setDaemon(true);
        thread.start();
    }
}
