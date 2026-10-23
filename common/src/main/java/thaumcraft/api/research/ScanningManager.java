package thaumcraft.api.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

/**
 * The registry of scannable things.
 *
 * <p>The native port keeps the original registration order: the first registered handler that accepts the
 * scanned target wins.</p>
 */
public class ScanningManager {
    private static final List<IScanThing> things = new ArrayList<>();

    private ScanningManager() {}

    /** Ignores null; other handlers are registered without validation and their failures propagate from {@link #find}. */
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
