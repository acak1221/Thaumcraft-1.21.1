package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The last phase: collects the suspicious lines of the client log, writes the report and ends the game with
 * {@code Minecraft.stop()}. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not
 * distributed.
 */
final class FinishPhase implements Phase {
    @Override
    public String name() {
        return "finish";
    }

    @Override
    public boolean tick(SmokeRun run) {
        collectLog(run);
        try {
            ScreenshotIndex.write(run.report());
        } catch (IOException e) {
            run.report().harnessError("finish", "screenshot_index", e);
        }
        run.report().harnessStatus("COMPLETED", null);
        run.report().write();
        run.markFinished();
        SmokeRun.LOG.info("client smoke finished: {} checks, {} failures, {} harness errors",
                run.report().checks().size(), run.report().count(SmokeReport.Result.FAIL), run.report().count(SmokeReport.Result.HARNESS_ERROR));
        run.client().stop();
        return true;
    }

    /** Reads the log written so far; also used by the watchdog and the shutdown hook. */
    static void collectLog(SmokeRun run) {
        Path log = SmokeRun.gameDir().resolve("logs").resolve("latest.log");
        try {
            List<Map<String, Object>> entries = LogExcerpt.collect(log);
            LogExcerpt.write(run.report().outDir().resolve("client-log-excerpt.txt"), entries);
            run.report().suspiciousLog(entries);
        } catch (IOException e) {
            run.report().harnessError("finish", "log_excerpt", e);
        }
    }
}
