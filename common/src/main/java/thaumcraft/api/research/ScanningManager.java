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

    public static void addScannableThing(IScanThing thing) {
        if (thing != null) things.add(thing);
    }

    /** The registered handlers, in registration order. */
    public static List<IScanThing> scannableThings() { return things; }

    /** The first registered handler that accepts the target, or null. */
    public static IScanThing find(Player player, Object thing) {
        for (IScanThing candidate : things) {
            if (candidate.checkThing(player, thing)) return candidate;
        }
        return null;
    }
}
