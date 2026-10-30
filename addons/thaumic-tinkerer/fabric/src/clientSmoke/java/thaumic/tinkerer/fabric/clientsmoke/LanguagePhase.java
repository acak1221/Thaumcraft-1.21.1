package thaumic.tinkerer.fabric.clientsmoke;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

/**
 * Switches the game to another language through the language manager and reloads the resources, so that the pictures after it
 * show that language. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The reload costs some seconds with the software renderer and re-stitches the atlases, which is why it comes after every
 * English picture. The key-by-key comparison of the languages is made from the language files in the resource phase; this
 * phase is for the look: a name that is still English in a Russian picture is a gap in the translation.</p>
 */
final class LanguagePhase implements Phase {
    private final String code;
    private CompletableFuture<Void> reload;
    private long started;

    LanguagePhase(String code) {
        this.code = code;
    }

    @Override
    public String name() {
        return "language_" + code;
    }

    @Override
    public boolean needsWorld() {
        return true;
    }

    @Override
    public boolean tick(SmokeRun run) {
        Minecraft client = run.client();
        if (reload == null) {
            started = System.nanoTime();
            client.getLanguageManager().setSelected(code);
            client.options.languageCode = code;
            client.options.save();
            reload = client.reloadResourcePacks();
            return false;
        }
        if (!reload.isDone() || client.getOverlay() != null) {
            if (System.nanoTime() - started > 180_000_000_000L) throw new IllegalStateException("the resource reload for " + code + " took more than 180 s");
            return false;
        }
        reload.join();
        // the chunk meshes are rebuilt after a reload
        if (!client.levelRenderer.hasRenderedAllSections()) return false;
        run.report().info(name(), "language_switched", "language " + code + " selected through the LanguageManager, resources reloaded in "
                + (System.nanoTime() - started) / 1_000_000 + " ms; now selected: " + client.getLanguageManager().getSelected());
        return true;
    }
}
