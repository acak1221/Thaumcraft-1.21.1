package thaumcraft.common.items.wands;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

/**
 * Runs the block triggers of a wand click the way the original ran them: outside any loader capture of
 * block changes.
 *
 * <p>The original answered these clicks in Forge 1.7.10 {@code Item.onItemUseFirst}
 * ({@code ItemWandCasting.onItemUseFirst}), which ran before {@code ForgeHooks.onPlaceItemIntoWorld}
 * started capturing block snapshots. The port answers them in {@code useOn}. NeoForge runs the server half
 * of {@code ItemStack.useOn} with snapshot capture on ({@code CommonHooks.onPlaceItemIntoWorld}), and a
 * captured write defers its {@code onPlace} and every neighbour notice until the use has returned. A
 * formation that checks its own write therefore saw nothing: the Oculus gate had already charged its vis
 * and opened the altar when a notified neighbour undid the portal, and the dark node it guards was lost.
 * Fabric never captures, so its bridge is the identity.</p>
 */
public final class WandUseCapture {
    @FunctionalInterface
    public interface Bridge {
        InteractionResult outside(Level level, Supplier<InteractionResult> action);
    }

    private static volatile Bridge bridge = (level, action) -> action.get();

    private WandUseCapture() {}

    public static void install(Bridge value) { bridge = Objects.requireNonNull(value); }

    public static InteractionResult outside(Level level, Supplier<InteractionResult> action) {
        return bridge.outside(level, action);
    }
}
