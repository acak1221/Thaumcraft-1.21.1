package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Entry point of the Thaumic Tinkerer client smoke. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0
 * like the add-on, not distributed.
 *
 * <p>Armed only by {@code -Dthaumictinkerer.clientSmoke=true}, which the {@code tinkererClientSmoke} run sets; in any other
 * launch the class does nothing. See {@code addons/thaumic-tinkerer/docs/CLIENT_VERIFICATION.md}.</p>
 */
public final class TinkererClientSmoke implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumictinkerer.clientSmoke")) return;
        Path outDir = Path.of(System.getProperty("thaumictinkerer.clientSmoke.outDir", "build/tinkerer-client-smoke")).toAbsolutePath();
        Path sourceRoot = Path.of(System.getProperty("thaumictinkerer.clientSmoke.sourceRoot", "addons/thaumic-tinkerer")).toAbsolutePath();
        int timeoutSeconds = Integer.getInteger("thaumictinkerer.clientSmoke.timeoutSeconds", 1500);
        try {
            Files.createDirectories(outDir.resolve("screenshots"));
        } catch (IOException e) {
            throw new IllegalStateException("cannot create " + outDir, e);
        }
        SmokeRun run = new SmokeRun(outDir, sourceRoot);
        run.report().write();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (run.client() == null) run.install(client);
            run.tick(client);
        });
        Watchdog.start(run, timeoutSeconds);
        SmokeRun.LOG.info("Thaumic Tinkerer client smoke armed: out={} sources={} timeout={}s", outDir, sourceRoot, timeoutSeconds);
    }
}
