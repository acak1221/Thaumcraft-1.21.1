package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemLadderRules;
import thaumcraft.common.lib.golems.GolemRouteRules;

/**
 * The goal ladder of the Fill, the Empty and the Sorting golem.
 *
 * <p><b>The original's structure.</b> TC4 4.2.3.5 registered <b>five</b> selector entries per core
 * (oracle {@code thaumcraft/common/entities/golems/EntityGolemBase.java:270-277,278-285,335-341}):</p>
 *
 * <ul>
 * <li>Fill: {@code AIHomeReplace 0}, {@code AIHomePlace 1}, {@code AIHomeDrop 2},
 * {@code AIFillTake 3}, {@code AIFillGoto 4};</li>
 * <li>Empty: {@code AIHomeReplace 0}, {@code AIEmptyPlace 1}, {@code AIEmptyDrop 2},
 * {@code AIEmptyGoto 3}, {@code AIHomeTake 4};</li>
 * <li>Sorting: {@code AIHomeReplace 0}, {@code AISortingPlace 1}, {@code AISortingGoto 3},
 * {@code AIHomeTakeSorting 4}.</li>
 * </ul>
 *
 * <p>Every one of them took the mutex bits three, so the entries of a core exclude each other and
 * the lowest number wins (see {@link GolemLadderRules} for the exact 1.7.10 semantics). The native
 * installs the same five entries with the same numbers and gives them the two exclusive flags
 * {@code MOVE} and {@code LOOK}, which are the bits three of the original.</p>
 *
 * <p><b>What the entries do.</b> The condition of an entry is the original's {@code shouldExecute},
 * the action is the original's {@code startExecuting} and the continuation is the original's
 * {@code continueExecuting}, including its two counters: the {@code count} of two hundred that
 * holds the whole ladder of the core for ten seconds when a transfer placed only part of the cargo,
 * and the {@code countChest} of five ticks that keeps the chest of the transfer open. The rules
 * themselves are the ones of {@link GolemRouteRules} and of the native helpers of
 * {@link EntityGolemBase}; this class only distributes them over the entries of the original.</p>
 */
final class InventoryGolemGoals {
    private final EntityGolemBase golem;
    private final List<FailedItem> failedItems = new ArrayList<>();
    private GolemMarker target;
    private ItemStack watched = ItemStack.EMPTY; // Ghost identity only, never owned cargo.
    private boolean travelStarted;
    private boolean interactionReady;

    private record FailedItem(ItemStack exemplar, long until) {}

    InventoryGolemGoals(EntityGolemBase golem) {
        this.golem = golem;
    }

    // ---- the installation of the ladder -----------------------------------------------------------------------

    /**
     * The five selector entries of the core, in the order of the original's registration.
     *
     * <p>The 1.21.1 selector walks its goals in the order of the registration and gives a running
     * goal's flags only to a goal with a <b>lower</b> number, so the registration order and the
     * numbers together reproduce the original's ladder: the lowest entry that wants to run wins and
     * the other four are blocked by its exclusive flags.</p>
     */
    void install() {
        switch (golem.getGolemCore()) {
            case GolemData.FILL -> {
                golem.addWorkGoal(GoalPriority.HOME_REPLACE, new HomeReplace());
                golem.addWorkGoal(GoalPriority.HOME_PLACE, new HomePlace());
                golem.addWorkGoal(GoalPriority.HOME_DROP, new HomeDrop());
                golem.addWorkGoal(GoalPriority.FILL_TAKE, new FillTake());
                golem.addWorkGoal(GoalPriority.FILL_GOTO, new Route());
            }
            case GolemData.EMPTY_CORE -> {
                golem.addWorkGoal(GoalPriority.HOME_REPLACE, new HomeReplace());
                golem.addWorkGoal(GoalPriority.EMPTY_PLACE, new EmptyPlace());
                golem.addWorkGoal(GoalPriority.EMPTY_DROP, new EmptyDrop());
                golem.addWorkGoal(GoalPriority.EMPTY_GOTO, new Route());
                golem.addWorkGoal(GoalPriority.HOME_TAKE, new HomeTake());
            }
            case GolemData.SORTING -> {
                golem.addWorkGoal(GoalPriority.HOME_REPLACE, new HomeReplace());
                golem.addWorkGoal(GoalPriority.SORTING_PLACE, new SortingPlace());
                golem.addWorkGoal(GoalPriority.SORTING_GOTO, new Route());
                golem.addWorkGoal(GoalPriority.HOME_TAKE_SORTING, new HomeTakeSorting());
            }
            default -> { }
        }
    }

    boolean hasRoute() {
        return target != null || !watched.isEmpty() || interactionReady;
    }

    void configurationChanged() {
        clearRoute();
        watched = ItemStack.EMPTY;
    }

    void reset() {
        configurationChanged();
        failedItems.clear();
    }

    /** The timeout list of the original is swept on every tick of the golem. */
    void serverTick() {
        sweepTimeouts();
        // The idle arrival of a route gives the adjacent take/place one turn before
        // the return home may start a fresh path.
        interactionReady = false;
    }

    private void clearRoute() {
        boolean running = travelStarted;
        target = null;
        travelStarted = false;
        interactionReady = false;
        if (running) golem.getNavigation().stop();
    }

    // ---- the shared decision rules ------------------------------------------------------------------------------

    /** Duplicate exemplar rows SUM first; only then subtract visible stock and zero-clamp. */
    int fillRequest(ItemStack exemplar) {
        if (exemplar.isEmpty()) return 0;
        GolemHomeInventory.Destination home = golem.homeInventory();
        if (home == null ? !golem.hasHomeWorldThrowTarget() : !home.current()) return 0;
        int wanted = 0;
        for (int row = 0; row < golem.getSlotCount(); row++) {
            ItemStack filter = golem.getFilter(row);
            if (!filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, exemplar)) wanted += golem.getWantedCount(row);
        }
        if (wanted <= 0) return 0; // A populated exemplar is mandatory, even in Any mode.
        if (golem.isFillAnyAmount()) return golem.getCarryLimit();
        // A positively eligible world home has zero logical inventory stock;
        // null caused by unloading/unknown providers never authorizes a fetch.
        long present = home == null ? 0 : home.countVisible(exemplar, golem.getHomeFacing(), golem.itemMatchFlags());
        return (int) Math.max(0L, (long) wanted - present);
    }

    private GolemMarker nearestSupply(ItemStack exemplar) {
        GolemMarker result = null;
        double nearest = Double.MAX_VALUE;
        // The single entry point of the original's helper keeps the order of the markers.
        for (GolemMarker marker : GolemHelper.getMarkedContainersWithGoods(golem, exemplar)) {
            double distance = golem.distanceToMarkerSqr(marker);
            if (distance < nearest) { nearest = distance; result = marker; }
        }
        return result;
    }

    boolean shouldReturnSortingCargo(ItemStack cargo) {
        return golem.getGolemCore() == GolemData.SORTING && !cargo.isEmpty() && !canDeliver(cargo);
    }

    // ---- the single entry point of the original's helper --------------------------------------------------------

    /** Original {@code GolemHelper.isOnTimeOut}:568-579 — the ten second ignore of an item. */
    boolean isItemOnTimeout(ItemStack stack) { return onTimeout(stack); }

    /** Original {@code GolemHelper.findSomethingEmptyCore}:515-543 — a destination for the item. */
    boolean hasEmptyCoreDestination(ItemStack stack) { return destinationExists(stack); }

    /** Original {@code GolemHelper.findSomethingSortCore}:545-566 — a destination for the item. */
    boolean hasSortingCoreDestination(ItemStack stack) { return destinationExists(stack); }

    /** Original {@code GolemHelper.validTargetForItem}:581-611 — the item may be delivered. */
    boolean hasValidTargetForItem(ItemStack stack) { return canDeliver(stack); }

    boolean canDeliver(ItemStack exemplar) {
        if (exemplar.isEmpty() || onTimeout(exemplar)) return false;
        if (findDestination(exemplar) != null) return true;
        fail(exemplar);
        return false;
    }

    /** The original's search of a destination, with its timeout side effect. */
    private boolean destinationExists(ItemStack stack) {
        if (stack == null || stack.isEmpty() || onTimeout(stack)) return false;
        if (findDestination(stack) != null) return true;
        fail(stack);
        return false;
    }

    private boolean inventoryAccepts(GolemMarker marker, ItemStack stack) {
        if (!golem.markerAccepts(marker, stack)) return false; // Order: the stack's slot colour must accept the marker's colour.
        GolemHomeInventory.Destination destination = GolemHelper.markedContainer(golem, marker);
        return destination != null && destination.current() && destination.roomFor(stack, marker.face()) > 0;
    }

    /** The markers whose container would take the stack, in the order of the markers. */
    private List<GolemMarker> containersWithRoom(ItemStack stack) {
        return GolemHelper.getMarkedContainersWithRoom(golem, stack);
    }

    private boolean sortingInventoryAccepts(GolemMarker marker, ItemStack stack) {
        GolemHomeInventory.Destination destination = GolemHelper.markedSortingContainer(golem, marker);
        return destination != null && destination.hasSeedAndRoom(stack, marker.face(), golem.itemMatchFlags());
    }

    private GolemMarker findDestination(ItemStack stack) {
        GolemMarker result = null;
        double nearest = Double.MAX_VALUE;
        boolean sorting = golem.getGolemCore() == GolemData.SORTING;
        for (GolemMarker marker : golem.getMarkers()) {
            if (sorting ? !sortingInventoryAccepts(marker, stack) : !inventoryAccepts(marker, stack)) continue;
            double distance = golem.distanceToMarkerSqr(marker);
            if (distance < nearest) { nearest = distance; result = marker; }
        }
        if (result != null || sorting) return result; // Sorting has NO world-target fallback.
        // Original Empty preflight/Goto: inventories first, then the FIRST world
        // marker in list order, without the inventory range16/home exclusions.
        for (GolemMarker marker : golem.getMarkers()) if (golem.markedWorldThrowTarget(marker)) return marker;
        return null;
    }

    private void sweepTimeouts() {
        long now = System.currentTimeMillis();
        failedItems.removeIf(failed -> now >= failed.until());
    }

    private boolean onTimeout(ItemStack stack) {
        sweepTimeouts();
        for (FailedItem failed : failedItems) if (ItemStack.isSameItemSameComponents(failed.exemplar(), stack)) return true;
        return false;
    }

    private void fail(ItemStack stack) {
        if (!stack.isEmpty() && !onTimeout(stack)) {
            // Original golemIgnoreDelay is wall-clock 10000 ms, not game ticks.
            failedItems.add(new FailedItem(stack.copyWithCount(1), System.currentTimeMillis() + GolemRouteRules.IGNORE_DELAY_MS));
        }
    }

    // ---- the base of the entries -------------------------------------------------------------------------------

    /**
     * One selector entry of the ladder of a core.
     *
     * <p>Every entry of the original took the mutex bits three, so the native gives it the two
     * exclusive flags {@code MOVE} and {@code LOOK}. The two counters are the original's
     * {@code count} (the budget of the entry) and {@code countChest} (the ticks the chest of a
     * transfer stays open).</p>
     */
    private abstract class LadderGoal extends Goal {
        /** The original's {@code count}: the budget of the entry. */
        int count;
        /** The original's {@code countChest}: the ticks the chest of the transfer stays open. */
        int countChest;
        /** The container the original opened and closes again when the entry stops. */
        private BlockPos chest;

        LadderGoal() { setFlags(GolemLadderRules.ladderFlags()); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public boolean canUse() { return golem.canWork() && ladderCanUse(); }

        @Override public void tick() {
            if (countChest > 0) --countChest;
            if (count > 0) --count;
        }

        @Override public void stop() {
            count = 0;
            countChest = 0;
            closeChest();
        }

        /** The original's {@code shouldExecute}. */
        abstract boolean ladderCanUse();

        /** The original's {@code startExecuting}. */
        abstract void ladderStart();

        void openChest(BlockPos pos) {
            chest = pos;
            GolemChestInteraction.open(golem.level(), pos);
        }

        private void closeChest() {
            if (chest == null) return;
            GolemChestInteraction.close(golem.level(), chest);
            chest = null;
        }
    }

    // ---- the original AIHomeReplace (the number zero) ----------------------------------------------------------

    /**
     * The original {@code AIHomeReplace}, the number zero of the Empty and the Sorting core.
     *
     * <p>The Fill core has no entry of its own: the original's {@code getItemsNeeded} returns
     * {@code null} for it (oracle {@code GolemHelper.java:448-471}), so the goal of the Fill core
     * never fires and the Fill never replaces its cargo.</p>
     */
    private final class HomeReplace extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AIHomeReplace.func_75250_a:37-45.
            if (!golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return false;
            if (!golem.getNavigation().isDone() || !GolemRouteRules.atHome(golem.distanceToHomeSqr())) return false;
            ItemStack cargo = golem.getCarriedItem();
            // Original AIHomeReplace.func_75250_a:46-48 — an item without a destination goes home at once.
            if (onTimeout(cargo)) return true;
            // Original AIHomeReplace.func_75250_a:49-59 — the core decides what "no destination" means.
            return switch (golem.getGolemCore()) {
                case GolemData.EMPTY_CORE, GolemData.SORTING -> !canDeliver(cargo);
                default -> false;
            };
        }

        @Override void ladderStart() {
            // Original AIHomeReplace.func_75249_e:99-119 — the cargo goes into the home inventory.
            if (golem.placeCarriedAtHome() <= 0) return;
            countChest = GolemRouteRules.CHEST_WINDOW;
            openChest(golem.homeInventoryPosition());
        }

        @Override public boolean canContinueToUse() {
            // Original AIHomeReplace.func_75253_b:72-74 — no count at all, only the chest window.
            return countChest > 0 || ladderCanUse();
        }
    }

    // ---- the original AIHomePlace (the number one of the Fill core) --------------------------------------------

    /**
     * The original {@code AIHomePlace}: the cargo of the Fill golem goes into the home inventory.
     */
    private final class HomePlace extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AIHomePlace.func_75250_a:35-63.
            if (!golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return false;
            if (!golem.getNavigation().isDone() || !GolemRouteRules.atHome(golem.distanceToHomeSqr())) return false;
            GolemHomeInventory.Destination home = golem.homeInventory();
            if (home == null || !home.current()) return false;
            return home.roomFor(golem.getCarriedItem(), golem.getHomeFacing()) > 0;
        }

        @Override void ladderStart() {
            if (golem.placeCarriedAtHome() <= 0) return;
            // Original AIHomePlace.func_75249_e:109 — only the chest window; the goal of the
            // original has NO counter of two hundred (oracle AIHomePlace.java:27-28).
            countChest = GolemRouteRules.CHEST_WINDOW;
            openChest(golem.homeInventoryPosition());
        }

        @Override public boolean canContinueToUse() {
            // Original AIHomePlace.func_75253_b:65-67 — no count at all.
            return countChest > 0 || ladderCanUse();
        }
    }

    // ---- the original AIHomeDrop (the number two of the Fill core) ---------------------------------------------

    /**
     * The original {@code AIHomeDrop}: the cargo of the Fill golem is dropped at its home, because
     * the home is no inventory.
     */
    private final class HomeDrop extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AIHomeDrop.func_75250_a:37-48 — no delay gate, and the home is no inventory.
            if (!golem.hasCargo() || !golem.getNavigation().isDone()) return false;
            if (!GolemRouteRules.atHome(golem.distanceToHomeSqr())) return false;
            return golem.hasHomeWorldThrowTarget();
        }

        @Override void ladderStart() {
            // Original AIHomeDrop.func_75249_e:72 — the counter of the ladder.
            if (golem.throwCarriedAtHome()) count = GolemLadderRules.PARTIAL_HOLD;
        }

        @Override public boolean canContinueToUse() {
            return count > 0 && (ladderCanUse() || countChest > 0);
        }
    }

    // ---- the original AIFillTake (the number three of the Fill core) -------------------------------------------

    /**
     * The original {@code AIFillTake}: the Fill golem takes the goods out of a marked container
     * that stands next to it.
     */
    private final class FillTake extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AIFillTake.func_75250_a:38-41 — no delay gate, the navigator must be idle.
            if (golem.hasCargo() || watched.isEmpty() || !golem.getNavigation().isDone()) return false;
            int wanted = fillRequest(watched);
            if (wanted <= 0) return false;
            return takeSource(watched, wanted) != null;
        }

        @Override void ladderStart() {
            int wanted = fillRequest(watched);
            if (wanted <= 0) return;
            GolemMarker marker = takeSource(watched, wanted);
            if (marker == null) return;
            GolemHomeInventory.Destination source = golem.markedInventory(marker);
            if (source == null || golem.takeFromInventory(source, marker.face(), watched, wanted) <= 0) return;
            // Original AIFillTake.func_75250_a:70-72 — the counters and the cleared watch.
            count = GolemLadderRules.PARTIAL_HOLD;
            countChest = GolemRouteRules.CHEST_WINDOW;
            watched = ItemStack.EMPTY;
            openChest(marker.pos());
        }

        @Override public boolean canContinueToUse() {
            // Original AIFillTake.func_75253_b:81-83.
            return count > 0 && (!golem.getNavigation().isDone() || countChest > 0);
        }

        /** The first adjacent marked container that holds the watched goods (original AIFillTake:47-59). */
        private GolemMarker takeSource(ItemStack exemplar, int wanted) {
            for (GolemMarker marker : golem.getMarkers()) {
                if (!GolemRouteRules.adjacent(golem.distanceToMarkerSqr(marker))) continue;
                if (!golem.markerAccepts(marker, exemplar)) continue;
                GolemHomeInventory.Destination source = GolemHelper.markedContainer(golem, marker);
                if (source != null && source.firstExtractable(exemplar, marker.face(), golem.itemMatchFlags()) >= 0) return marker;
            }
            return null;
        }
    }

    // ---- the original AIEmptyPlace (the number one of the Empty core) -------------------------------------------

    /**
     * The original {@code AIEmptyPlace}: the cargo of the Empty golem goes into a marked container
     * that stands next to it.
     */
    private final class EmptyPlace extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AIEmptyPlace.func_75250_a:41-77 — no delay gate, the navigator must be idle.
            if (!golem.hasCargo() || !golem.getNavigation().isDone()) return false;
            ItemStack cargo = golem.getCarriedItem();
            for (GolemMarker marker : containersWithRoom(cargo)) {
                if (!GolemRouteRules.adjacent(golem.distanceToMarkerSqr(marker))) continue;
                return true;
            }
            return false;
        }

        @Override void ladderStart() {
            ItemStack cargo = golem.getCarriedItem();
            for (GolemMarker marker : containersWithRoom(cargo)) {
                if (!GolemRouteRules.adjacent(golem.distanceToMarkerSqr(marker))) continue;
                if (golem.placeCarriedAtMarker(marker) <= 0) continue;
                // Original AIEmptyPlace.func_75249_e:114 — the counters of the transfer.
                count = GolemLadderRules.PARTIAL_HOLD;
                countChest = GolemRouteRules.CHEST_WINDOW;
                openChest(marker.pos());
                return;
            }
        }

        @Override public boolean canContinueToUse() {
            // Original AIEmptyPlace.func_75253_b:79-81.
            return count > 0 && (ladderCanUse() || countChest > 0);
        }
    }

    // ---- the original AIEmptyDrop (the number two of the Empty core) -------------------------------------------

    /**
     * The original {@code AIEmptyDrop}: the cargo of the Empty golem is dropped at a marked block
     * that stands next to it.
     */
    private final class EmptyDrop extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AIEmptyDrop.func_75250_a:30-44 — no delay gate, the navigator must be idle.
            if (!golem.hasCargo() || !golem.getNavigation().isDone()) return false;
            return golem.firstAdjacentWorldThrowMarker() != null;
        }

        @Override void ladderStart() {
            // Original AIEmptyDrop.func_75249_e:60 — the counter of the ladder.
            if (golem.throwCarriedAtMarker()) count = GolemLadderRules.PARTIAL_HOLD;
        }

        @Override public boolean canContinueToUse() {
            // Original AIEmptyDrop.func_75253_b:46-48 — the counter of two hundred and the
            // condition of the goal; the drop has no chest window.
            return count > 0 && ladderCanUse();
        }
    }

    // ---- the original AIHomeTake (the number four of the Empty core) --------------------------------------------

    /**
     * The original {@code AIHomeTake}: the Empty golem takes goods out of its home.
     */
    private final class HomeTake extends LadderGoal {
        @Override boolean ladderCanUse() { return candidate() != null; }

        @Override void ladderStart() {
            Take take = candidate();
            if (take == null) return;
            GolemHomeInventory.Destination home = golem.homeInventory();
            if (home == null) return;
            // Original AIHomeTake.func_75250_a:59-74 — the extraction of the home.
            if (golem.takeFromInventory(home, golem.getHomeFacing(), take.exemplar(), golem.carrySpace()) <= 0) return;
            countChest = GolemRouteRules.CHEST_WINDOW;
            openChest(home.pos());
        }

        @Override public boolean canContinueToUse() {
            // Original AIHomeTake.func_75253_b:108-110 — only the chest window, no count.
            return countChest > 0;
        }

        /** The first slot of the home the original would take (AIHomeTake.func_75250_a:37-106). */
        private Take candidate() {
            if (golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return null;
            if (!golem.getNavigation().isDone() || !GolemRouteRules.atHome(golem.distanceToHomeSqr())) return null;
            GolemHomeInventory.Destination home = golem.homeInventory();
            if (home == null || !home.current()) return null;
            boolean filtered = false;
            for (int row = 0; row < golem.getSlotCount(); row++) {
                ItemStack exemplar = golem.getFilter(row);
                if (exemplar.isEmpty()) continue;
                filtered = true;
                int slot = home.firstExtractable(exemplar, golem.getHomeFacing(), golem.itemMatchFlags());
                if (slot < 0 || !canDeliver(home.container().getItem(slot))) continue;
                return new Take(slot, exemplar);
            }
            if (filtered) return null;
            int[] exposed = home.slots(golem.getHomeFacing());
            if (exposed == null) return null;
            for (int slot : exposed) {
                if (slot < 0 || slot >= home.container().getContainerSize()) continue;
                ItemStack actual = home.container().getItem(slot);
                if (!home.extracts(slot, actual, golem.getHomeFacing()) || !canDeliver(actual)) continue;
                return new Take(slot, actual.copyWithCount(1));
            }
            return null;
        }
    }

    // ---- the original AISortingPlace (the number one of the Sorting core) --------------------------------------

    /**
     * The original {@code AISortingPlace}: the cargo of the Sorting golem goes into a marked
     * container that already holds it.
     */
    private final class SortingPlace extends LadderGoal {
        @Override boolean ladderCanUse() {
            // Original AISortingPlace.func_75250_a:41-74 — no delay gate, the navigator must be idle.
            if (!golem.hasCargo() || !golem.getNavigation().isDone()) return false;
            ItemStack cargo = golem.getCarriedItem();
            for (GolemMarker marker : golem.getMarkers()) {
                if (!GolemRouteRules.adjacent(golem.distanceToMarkerSqr(marker))) continue;
                if (sortingInventoryAccepts(marker, cargo)) return true;
            }
            return false;
        }

        @Override void ladderStart() {
            ItemStack cargo = golem.getCarriedItem();
            for (GolemMarker marker : golem.getMarkers()) {
                if (!GolemRouteRules.adjacent(golem.distanceToMarkerSqr(marker))) continue;
                if (!sortingInventoryAccepts(marker, cargo)) continue;
                if (golem.placeCarriedAtMarker(marker) <= 0) continue;
                // Original AISortingPlace.func_75249_e:114 — the counters of the transfer.
                count = GolemLadderRules.PARTIAL_HOLD;
                countChest = GolemRouteRules.CHEST_WINDOW;
                openChest(marker.pos());
                return;
            }
        }

        @Override public boolean canContinueToUse() {
            // Original AISortingPlace.func_75253_b:76-78.
            return count > 0 && (ladderCanUse() || countChest > 0);
        }
    }

    // ---- the original AIHomeTakeSorting (the number four of the Sorting core) ---------------------------------

    /**
     * The original {@code AIHomeTakeSorting}: the Sorting golem takes goods out of its home.
     */
    private final class HomeTakeSorting extends LadderGoal {
        @Override boolean ladderCanUse() { return candidate() != null; }

        @Override void ladderStart() {
            Take take = candidate();
            if (take == null) return;
            GolemHomeInventory.Destination home = golem.homeInventory();
            if (home == null) return;
            // Original AIHomeTakeSorting.func_75249_e:114-126 — the extraction of the home.
            if (golem.takeFromInventory(home, golem.getHomeFacing(), take.exemplar(), golem.carrySpace()) <= 0) return;
            count = GolemLadderRules.PARTIAL_HOLD;
            countChest = GolemRouteRules.CHEST_WINDOW;
            openChest(home.pos());
        }

        @Override public boolean canContinueToUse() {
            // Original AIHomeTakeSorting.func_75253_b:73-75.
            return count > 0 && (ladderCanUse() || countChest > 0);
        }

        /**
         * The first slot of the home the original would take.
         *
         * <p>The original reads the face visible order of the home (oracle
         * {@code GolemHelper.getItemsInHomeContainer}:658-686) and not the ghost rows.</p>
         */
        private Take candidate() {
            if (golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return null;
            if (!golem.getNavigation().isDone() || !GolemRouteRules.atHome(golem.distanceToHomeSqr())) return null;
            GolemHomeInventory.Destination home = golem.homeInventory();
            if (home == null || !home.current()) return null;
            int[] exposed = home.slots(golem.getHomeFacing());
            if (exposed == null) return null;
            for (int slot : exposed) {
                if (slot < 0 || slot >= home.container().getContainerSize()) continue;
                ItemStack candidate = home.container().getItem(slot);
                if (candidate.isEmpty() || !canDeliver(candidate)) continue;
                if (home.firstExtractable(candidate, golem.getHomeFacing()) < 0) continue;
                return new Take(slot, candidate.copyWithCount(1));
            }
            return null;
        }
    }

    // ---- the routes of the three cores -------------------------------------------------------------------------

    /**
     * The route of the core: the original's {@code AIFillGoto}, {@code AIEmptyGoto} and
     * {@code AISortingGoto}.
     *
     * <p>The three routes share their numbers and their rules except for the destination they look
     * for, so the native uses one goal and lets the core decide.</p>
     */
    private final class Route extends LadderGoal {
        private GolemMarker pursued;
        private BlockPos previous;

        @Override boolean ladderCanUse() {
            if (interactionReady) return false;
            return switch (golem.getGolemCore()) {
                case GolemData.FILL -> decideFillRoute();
                case GolemData.EMPTY_CORE -> decideEmptyRoute();
                case GolemData.SORTING -> decideSortingRoute();
                default -> false;
            };
        }

        @Override void ladderStart() {
            // Original AIFillGoto.func_75249_e:139-144 and AIEmptyGoto.func_75249_e:114-120.
            count = GolemLadderRules.PARTIAL_HOLD;
            travelStarted = true;
            pursued = target;
            previous = golem.blockPosition();
            if (pursued == null) { count = 0; return; }
            if (golem.distanceToMarkerSqr(pursued) < 4) {
                count = 0;
                interactionReady = true;
                golem.getNavigation().stop();
                return;
            }
            if (!golem.loadedNavigationArea(pursued.pos())) { count = 0; return; }
            golem.getNavigation().moveTo(pursued.pos().getX(), pursued.pos().getY(), pursued.pos().getZ(), 1);
        }

        @Override public boolean canContinueToUse() {
            // Original AIFillGoto.func_75253_b:120-122 — the budget and the unfinished path.
            return count > 0 && pursued != null && target != null && target.equals(pursued)
                    && !golem.getNavigation().isDone();
        }

        @Override public void tick() {
            super.tick();
            if (pursued == null) return;
            golem.getLookControl().setLookAt(pursued.pos().getX() + .5, pursued.pos().getY() + .5, pursued.pos().getZ() + .5);
            // Original AIFillGoto.func_75246_d:124-131 — the random detour of an exhausted route.
            if (count != 0 || !golem.blockPosition().equals(previous) || !golem.loadedNavigationArea(golem.blockPosition())) return;
            Vec3 detour = DefaultRandomPos.getPos(golem, GolemRouteRules.DETOUR_XZ, GolemRouteRules.DETOUR_Y);
            if (detour == null) return;
            count = GolemRouteRules.DETOUR_BUDGET;
            golem.getNavigation().moveTo(detour.x, detour.y, detour.z, 1);
        }

        @Override public void stop() {
            // Give the adjacent take/place phase a turn before the lower-priority
            // native ReturnHome goal can start a fresh path and mask idle arrival.
            if (travelStarted && golem.getNavigation().isDone()) interactionReady = true;
            if (travelStarted && target != null && target.equals(pursued)) target = null;
            pursued = null;
            travelStarted = false;
            super.stop();
            golem.getNavigation().stop();
            // Fill's watched identity survives Goto.stop for adjacent FillTake.
        }

        /** The original's {@code AIFillGoto.func_75250_a}:52-118 — the nearest supply of a missing good. */
        private boolean decideFillRoute() {
            if (golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return false;
            for (int row = 0; row < golem.getSlotCount(); row++) {
                ItemStack filter = golem.getFilter(row);
                if (filter.isEmpty() || fillRequest(filter) <= 0) continue;
                boolean duplicate = false;
                for (int earlier = 0; earlier < row; earlier++) {
                    ItemStack previousFilter = golem.getFilter(earlier);
                    if (!previousFilter.isEmpty() && ItemStack.isSameItemSameComponents(previousFilter, filter)) { duplicate = true; break; }
                }
                if (duplicate) continue;
                GolemMarker nearest = nearestSupply(filter);
                if (nearest == null) continue;
                watched = filter.copyWithCount(1);
                target = nearest;
                return true; // First missing exemplar having supply, then its nearest source.
            }
            target = null;
            return false;
        }

        /** The original's {@code AIEmptyGoto.func_75250_a}:49-93 — the nearest container with room. */
        private boolean decideEmptyRoute() {
            if (!golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return false;
            ItemStack cargo = golem.getCarriedItem();
            GolemMarker destination = onTimeout(cargo) ? null : findDestination(cargo);
            if (destination == null) {
                if (!onTimeout(cargo)) fail(cargo);
                target = null;
                return false;
            }
            target = destination;
            return true;
        }

        /** The original's {@code AISortingGoto.func_75250_a}:49-87 — the nearest seeded container. */
        private boolean decideSortingRoute() {
            if (!golem.hasCargo() || !GolemRouteRules.decisionTick(golem.tickCount)) return false;
            ItemStack cargo = golem.getCarriedItem();
            GolemMarker destination = onTimeout(cargo) ? null : findDestination(cargo);
            if (destination == null) {
                if (!onTimeout(cargo)) fail(cargo);
                target = null;
                return false;
            }
            target = destination;
            return true;
        }
    }

    /** The slot of the home the original would take and the stack it would take it with. */
    private record Take(int slot, ItemStack exemplar) {}
}
