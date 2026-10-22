package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;

/**
 * The one entry point of the searches of the golem.
 *
 * <p>TC4 4.2.3.5 had <b>one</b> helper class with the searches of every core
 * (oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:48-721}). The native tree had
 * split them into pure rule classes ({@code GolemHomeInventory}, {@code GolemMarker},
 * {@code GolemRouteRules}, {@code GolemItemMatch}, {@code GolemGuardTargets}) and the goals called
 * them one by one. This class is the single entry point of the original again: every search of the
 * original is here, with the original's name and the original's rule, and it delegates to the pure
 * rule classes of the native tree. The rules themselves are unchanged.</p>
 *
 * <p>The searches of the <b>fluid</b> half of the original ({@code findJarWithRoom},
 * {@code getMarkedFluidHandlers}, {@code getMarkedFluidBlocks}, {@code getMissingLiquids},
 * {@code findPossibleLiquid} and {@code getReggedLiquids}) are still owned by the Decanting and the
 * Alchemy goals of the native tree, because the 1.21.1 common module has no loader independent fluid
 * handler API yet; see the report of the wave for the exact blocker.</p>
 */
public final class GolemHelper {
    /**
     * Original {@code GolemHelper.ADJACENT_RANGE} — the <b>squared</b> distance of a marked block
     * that still counts as "next to the golem".
     */
    public static final double ADJACENT_RANGE = 4.0;

    private GolemHelper() {}

    /**
     * The marked container of one marker, with the rules of the native golem.
     *
     * <p>This is the per-marker form of the original's {@code getMarkedContainers}:54-64. The
     * original adds the double chest of the marker as a second result; the native double chest is
     * one storage owner already, so the native returns it once.</p>
     */
    public static GolemHomeInventory.Destination markedContainer(EntityGolemBase golem, GolemMarker marker) {
        if (golem == null || marker == null) return null;
        if (!(golem.level() instanceof ServerLevel server) || !golem.liveMarkers().contains(marker)
                || !marker.dimension().equals(golem.level().dimension().location()) || marker.pos().equals(golem.homeInventoryPosition())) return null;
        double range = golem.rangeSqr();
        // Original GolemHelper.getMarkedContainers:54-64 has NO range at all; the range of the
        // original is the golem's own (AIFillGoto:100-104, AIEmptyGoto:56; the adjacent goals
        // are bound by the squared distance of four instead). The distance of the HOME is not a
        // bound of the original, so it is not one here.
        if (golem.distanceToSqr(marker.pos().getX() + .5, marker.pos().getY() + .5, marker.pos().getZ() + .5) > range) return null;
        GolemHomeInventory.Destination result = GolemHomeInventory.find(server, marker.pos());
        // A native double chest is one storage owner, not a remote source just
        // because the marker happens to name its other physical half.
        return result == null || result.sharesStorage(golem.homeInventory()) ? null : result;
    }

    /**
     * The marked container of one marker for the Sorting core, with the seed rule of the original.
     *
     * <p>Original {@code GolemHelper.getContainersWithGoods}:148-164 — the Sorting core only uses a
     * container that already holds the item.</p>
     */
    public static GolemHomeInventory.Destination markedSortingContainer(EntityGolemBase golem, GolemMarker marker) {
        if (golem == null || marker == null) return null;
        if (golem.getGolemCore() != GolemData.SORTING || !(golem.level() instanceof ServerLevel server)
                || !golem.liveMarkers().contains(marker)
                || !marker.dimension().equals(server.dimension().location()) || marker.pos().equals(golem.homeInventoryPosition())
                || golem.distanceToMarkerSqr(marker) > golem.rangeSqr()) return null;
        // Sorting's original bound is relative to the GOLEM (AISortingGoto:58-59), like Fill's
        // and Empty's now.
        GolemHomeInventory.Destination result = GolemHomeInventory.find(server, marker.pos());
        return result == null || result.sharesStorage(golem.homeInventory()) ? null : result;
    }

    /**
     * Original {@code getMarkedContainers}:54-64 — every marked container of the dimension.
     *
     * <p>The original has no range at all; the range of the original is the golem's own
     * ({@code AIFillGoto}:100-104, {@code AIEmptyGoto}:56), and the adjacent goals are bound by the
     * squared distance of four instead.</p>
     */
    public static List<GolemHomeInventory.Destination> getMarkedContainers(EntityGolemBase golem) {
        List<GolemHomeInventory.Destination> results = new ArrayList<>();
        if (golem == null) return results;
        for (GolemMarker marker : golem.getMarkers()) {
            GolemHomeInventory.Destination destination = markedContainer(golem, marker);
            if (destination != null) results.add(destination);
        }
        return results;
    }

    /**
     * Original {@code getMarkedContainersAdjacentToGolem}:66-76 — the marked containers that stand
     * next to the golem.
     */
    public static List<GolemHomeInventory.Destination> getMarkedContainersAdjacentToGolem(EntityGolemBase golem) {
        List<GolemHomeInventory.Destination> results = new ArrayList<>();
        if (golem == null) return results;
        for (GolemHomeInventory.Destination destination : getMarkedContainers(golem)) {
            if (!adjacent(golem, destination.pos())) continue;
            results.add(destination);
        }
        return results;
    }

    /**
     * Original {@code getMarkedBlocksAdjacentToGolem}:78-86 — the marked blocks next to the golem
     * that are <b>no</b> inventory.
     */
    public static List<GolemMarker> getMarkedBlocksAdjacentToGolem(EntityGolemBase golem, int color) {
        List<GolemMarker> results = new ArrayList<>();
        if (golem == null) return results;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!GolemMarker.accepts(color, marker.color())) continue;
            if (golem.markedInventory(marker) != null) continue; // An inventory is no block target.
            if (!adjacent(golem, marker.pos())) continue;
            results.add(marker);
        }
        return results;
    }

    /**
     * Original {@code getMarkedSides}:122-138 — the sides of a block that carry a marker of the
     * given colour.
     *
     * <p>The original probes every side with a marker of the given colour and keeps the sides whose
     * marker matches ({@code Marker.equalsFuzzy}:31-37, the colour rule of
     * {@code GolemMarker.accepts}). The 1.7.10 and the 1.21.1 direction orders are the same, so the
     * ordinal of a side is the number of the original.</p>
     */
    public static List<Integer> getMarkedSides(EntityGolemBase golem, BlockPos pos, int color) {
        List<Integer> sides = new ArrayList<>();
        if (golem == null || pos == null) return sides;
        List<GolemMarker> markers = golem.getMarkers();
        for (Direction face : Direction.values()) {
            for (GolemMarker marker : markers) {
                if (!marker.pos().equals(pos) || marker.face() != face) continue;
                if (!GolemMarker.accepts(color, marker.color())) continue;
                sides.add(face.ordinal());
                break;
            }
        }
        return sides;
    }

    /**
     * Original {@code getContainersWithRoom}:88-120 — the marked containers that would take the
     * given stack.
     */
    public static List<GolemHomeInventory.Destination> getContainersWithRoom(EntityGolemBase golem,
                                                                            int color, ItemStack item) {
        List<GolemHomeInventory.Destination> results = new ArrayList<>();
        if (golem == null || item == null || item.isEmpty()) return results;
        for (GolemHomeInventory.Destination destination : getMarkedContainers(golem)) {
            if (destination.roomFor(item, golem.getHomeFacing()) > 0) results.add(destination);
        }
        return results;
    }

    /**
     * Original {@code getContainersWithGoods}:148-164 — the marked containers that hold the given
     * goods, in the order of the markers.
     */
    public static List<GolemHomeInventory.Destination> getContainersWithGoods(EntityGolemBase golem,
                                                                             ItemStack goods, int color) {
        List<GolemHomeInventory.Destination> results = new ArrayList<>();
        if (golem == null || goods == null || goods.isEmpty()) return results;
        for (GolemHomeInventory.Destination destination : getMarkedContainers(golem)) {
            if (destination.firstExtractable(goods, golem.getHomeFacing(), golem.itemMatchFlags()) >= 0) {
                results.add(destination);
            }
        }
        return results;
    }

    /**
     * The markers whose container holds the given goods, in the order of the markers.
     *
     * <p>This is the per-marker form of the original's {@code getContainersWithGoods}:148-164, with
     * the colour rule of the slot that the goods belong to
     * ({@code GolemMarker.accepts}:104).</p>
     */
    public static List<GolemMarker> getMarkedContainersWithGoods(EntityGolemBase golem, ItemStack goods) {
        List<GolemMarker> results = new ArrayList<>();
        if (golem == null || goods == null || goods.isEmpty()) return results;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!golem.markerAccepts(marker, goods)) continue;
            GolemHomeInventory.Destination destination = markedContainer(golem, marker);
            if (destination == null || destination.firstExtractable(goods, marker.face(), golem.itemMatchFlags()) < 0) continue;
            results.add(marker);
        }
        return results;
    }

    /**
     * The markers whose container would take the given stack, in the order of the markers.
     *
     * <p>This is the per-marker form of the original's {@code getContainersWithRoom}:88-120.</p>
     */
    public static List<GolemMarker> getMarkedContainersWithRoom(EntityGolemBase golem, ItemStack stack) {
        List<GolemMarker> results = new ArrayList<>();
        if (golem == null || stack == null || stack.isEmpty()) return results;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!golem.markerAccepts(marker, stack)) continue;
            GolemHomeInventory.Destination destination = markedContainer(golem, marker);
            if (destination == null || !destination.current() || destination.roomFor(stack, marker.face()) <= 0) continue;
            results.add(marker);
        }
        return results;
    }

    /**
     * Original {@code getItemsInHomeContainer}:658-686 — the goods of the home in the face visible
     * order of its slots.
     */
    public static List<ItemStack> getItemsInHomeContainer(EntityGolemBase golem) {
        List<ItemStack> results = new ArrayList<>();
        if (golem == null) return results;
        GolemHomeInventory.Destination home = golem.homeInventory();
        if (home == null || !home.current()) return results;
        int[] exposed = home.slots(golem.getHomeFacing());
        if (exposed == null) return results;
        for (int slot : exposed) {
            if (slot < 0 || slot >= home.container().getContainerSize()) continue;
            ItemStack stack = home.container().getItem(slot);
            if (!stack.isEmpty()) results.add(stack.copy());
        }
        return results;
    }

    /**
     * Original {@code isOnTimeOut}:568-579 — whether the item is ignored for the next ten seconds.
     */
    public static boolean isOnTimeOut(EntityGolemBase golem, ItemStack stack) {
        return golem != null && golem.isItemOnTimeout(stack);
    }

    /**
     * Original {@code findSomethingEmptyCore}:515-543 — whether the Empty core has a destination for
     * the given item at all.
     */
    public static boolean findSomethingEmptyCore(EntityGolemBase golem, ItemStack stack) {
        return golem != null && golem.hasEmptyCoreDestination(stack);
    }

    /**
     * Original {@code findSomethingSortCore}:545-566 — whether the Sorting core has a destination
     * for the given item at all.
     */
    public static boolean findSomethingSortCore(EntityGolemBase golem, ItemStack stack) {
        return golem != null && golem.hasSortingCoreDestination(stack);
    }

    /**
     * Original {@code validTargetForItem}:581-611 — whether the item may be delivered at all.
     */
    public static boolean validTargetForItem(EntityGolemBase golem, ItemStack stack) {
        return golem != null && golem.hasValidTargetForItem(stack);
    }

    /** Whether a position stands next to the golem (the squared distance of four of the original). */
    public static boolean adjacent(EntityGolemBase golem, BlockPos pos) {
        if (golem == null || pos == null) return false;
        return golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) < ADJACENT_RANGE;
    }
}
