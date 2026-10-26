package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import thaumcraft.api.IThaumcraftAddon;

/**
 * Runs the {@link IThaumcraftAddon} hooks and the {@code ThaumcraftApi.whenReady} callbacks once the loader has
 * initialized every mod. The loader modules call {@link #fireReady(List)} with the hooks they discovered.
 */
public final class AddonLifecycle {
    /** One discovered hook and the mod that declared it, for the error message. */
    public record Hook(String modId, IThaumcraftAddon addon) {}

    private static final List<Runnable> PENDING = new ArrayList<>();
    private static boolean ready;

    private AddonLifecycle() {}

    /** Runs the callback now when the game is ready, later otherwise. */
    public static void whenReady(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        synchronized (AddonLifecycle.class) {
            if (!ready) {
                PENDING.add(callback);
                return;
            }
        }
        callback.run();
    }

    public static synchronized boolean isReady() { return ready; }

    /** Calls every discovered hook, then every queued callback; a second call does nothing. */
    public static void fireReady(List<Hook> hooks) {
        List<Runnable> queued;
        synchronized (AddonLifecycle.class) {
            if (ready) return;
            ready = true;
            queued = List.copyOf(PENDING);
            PENDING.clear();
        }
        for (Hook hook : hooks) {
            try {
                hook.addon().onThaumcraftReady();
            } catch (RuntimeException | LinkageError failure) {
                throw new IllegalStateException("Thaumcraft add-on hook of mod '" + hook.modId() + "' ("
                        + hook.addon().getClass().getName() + ") failed", failure);
            }
        }
        for (Runnable callback : queued) callback.run();
        // Research registered by the hooks joins the live catalogue at once.
        thaumcraft.common.lib.research.AddonResearch.refresh();
    }

    /** Test seam: forgets the state, as a fresh game would have it. */
    static synchronized void resetForTest() {
        ready = false;
        PENDING.clear();
    }
}
