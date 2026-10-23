package thaumcraft.api.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

/**
 * The registry of scannable things.
 *
 * <p>The native port keeps the original registration order: the first registered handler that accepts the
     * scanned target wins when find is explicitly called; the native thaumometer never calls this registry.</p>
     * <p>This is a later compatibility shape, not a TC4 1.7.10 generic target-dispatch service.
     * In 1.21.1 registrations only populate isolated data, not gameplay scan callbacks.</p>
     * @deprecated Use ThaumcraftApi.registerScanEventhandler for phenomena callbacks; no equivalent
     * generic-target dispatcher is currently wired into native gameplay.
 */
@Deprecated
public class ScanningManager {
    private static final List<IScanThing> things = new ArrayList<>();

    private ScanningManager() {}

    /** Appends a non-null handler to isolated compatibility storage; null is ignored and native scanning is unaffected.
     * @deprecated Use ThaumcraftApi.registerScanEventhandler for phenomena; generic-target gameplay dispatch has no replacement. */
    @Deprecated
    public static void addScannableThing(IScanThing thing) {
        if (thing != null) things.add(thing);
    }

    /** The registered handlers, in registration order. The registry itself stays private. */
    public static List<IScanThing> scannableThings() { return List.copyOf(things); }

    /** Returns the first accepting handler or null; passes null/unsupported arguments through and propagates callback failures. */
    public static IScanThing find(Player player, Object thing) {
        for (IScanThing candidate : things) {
            if (candidate.checkThing(player, thing)) return candidate;
        }
        return null;
    }
}
