package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;

/** Ordinary Fill/Empty/Sorting routes; only Fill/Empty have explicit safe world targets. */
final class InventoryGolemGoals {
    private final EntityGolemBase golem;
    private final Route route;
    private final List<FailedItem> failedItems = new ArrayList<>();
    private GolemMarker target;
    private ItemStack watched = ItemStack.EMPTY; // Ghost identity only, never owned cargo.
    private int interactionPause;
    private boolean travelStarted;
    private boolean interactionReady;

    private record FailedItem(ItemStack exemplar, long until) {}

    InventoryGolemGoals(EntityGolemBase golem) {
        this.golem = golem;
        route = new Route();
    }

    void install() { golem.addWorkGoal(golem.getGolemCore() == GolemData.FILL ? 4 : 3, route); }
    boolean hasRoute() {
        return target != null || route.running || interactionPause > 0 || !watched.isEmpty() || interactionReady;
    }
    void configurationChanged() {
        target = null;
        watched = ItemStack.EMPTY;
        interactionPause = 0;
        travelStarted = false;
        interactionReady = false;
        if (route.running) golem.getNavigation().stop();
    }
    void reset() {
        configurationChanged();
        route.running = false;
        route.pursued = null;
        route.budget = 0;
        failedItems.clear();
    }
    private void clearRoute() {
        target = null;
        watched = ItemStack.EMPTY;
        travelStarted = false;
        interactionReady = false;
        if (route.running) golem.getNavigation().stop();
    }
    private void progressed() { clearRoute(); interactionPause = 5; }

    void serverTick() {
        if (!golem.canWork()) return;
        sweepTimeouts();
        try {
            // Original Drop has no golemDelay gate. A container-interaction hold
            // must not add one, but adjacent insertion still has higher priority.
            if (tryDirectedThrow()) { progressed(); return; }
            if (interactionPause > 0 && --interactionPause > 0) return;
            if (golem.getGolemCore() == GolemData.FILL) fillTick();
            else if (golem.getGolemCore() == GolemData.EMPTY_CORE) emptyTick();
            else if (golem.getGolemCore() == GolemData.SORTING) sortingTick();
        } finally { interactionReady = false; }
    }

    private boolean tryDirectedThrow() {
        if (!golem.hasCargo() || !golem.getNavigation().isDone()) return false;
        if (golem.getGolemCore() == GolemData.FILL) return golem.throwCarriedAtHome();
        if (golem.getGolemCore() != GolemData.EMPTY_CORE) return false;
        ItemStack cargo = golem.getCarriedItem();
        for (GolemMarker marker : golem.getMarkers()) {
            if (golem.distanceToMarkerSqr(marker) < 4 && inventoryAccepts(marker, cargo)) return false;
        }
        return golem.throwCarriedAtMarker();
    }

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

    private void fillTick() {
        if (golem.hasCargo()) {
            clearRoute();
            if (golem.tickCount % 5 == 0 && golem.getNavigation().isDone() && golem.distanceToHomeSqr() <= 5
                    && golem.placeCarriedAtHome() > 0) progressed();
            return;
        }
        // FillTake is not independently cadence-gated: re-read ANY adjacent
        // marked source/face after navigation ends, not an exclusive reservation.
        if (golem.getNavigation().isDone() && !watched.isEmpty()) {
            int wanted = fillRequest(watched);
            if (wanted > 0) for (GolemMarker marker : golem.getMarkers()) {
                if (golem.distanceToMarkerSqr(marker) >= 4 || !golem.markerAccepts(marker, watched)) continue;
                GolemHomeInventory.Destination source = golem.markedInventory(marker);
                if (source != null && golem.takeFromInventory(source, marker.face(), watched, wanted) > 0) {
                    progressed();
                    return;
                }
            }
            // A decision may precede the native goal-start tick. Do not cancel
            // an unstarted remote route merely because navigation is still idle.
            if (wanted <= 0 || target == null || travelStarted) clearRoute();
        }
        if (golem.tickCount % 5 != 0) return;
        if (target != null && !watched.isEmpty() && fillRequest(watched) > 0) {
            GolemHomeInventory.Destination source = golem.markedInventory(target);
            if (source != null && source.firstExtractable(watched, target.face(), golem.itemMatchFlags()) >= 0) return;
        }
        clearRoute();
        for (int row = 0; row < golem.getSlotCount(); row++) {
            ItemStack filter = golem.getFilter(row);
            if (filter.isEmpty() || fillRequest(filter) <= 0) continue;
            boolean duplicate = false;
            for (int earlier = 0; earlier < row; earlier++) {
                ItemStack previous = golem.getFilter(earlier);
                if (!previous.isEmpty() && ItemStack.isSameItemSameComponents(previous, filter)) { duplicate = true; break; }
            }
            if (duplicate) continue;
            GolemMarker nearest = nearestSupply(filter);
            if (nearest == null) continue;
            watched = filter.copyWithCount(1);
            target = nearest;
            return; // First missing exemplar having supply, then its nearest source.
        }
    }

    private GolemMarker nearestSupply(ItemStack exemplar) {
        GolemMarker result = null;
        double nearest = Double.MAX_VALUE;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!golem.markerAccepts(marker, exemplar)) continue;
            GolemHomeInventory.Destination source = golem.markedInventory(marker);
            if (source == null || source.firstExtractable(exemplar, marker.face(), golem.itemMatchFlags()) < 0) continue;
            double distance = golem.distanceToMarkerSqr(marker);
            if (distance < nearest) { nearest = distance; result = marker; }
        }
        return result;
    }

    private void emptyTick() {
        if (golem.hasCargo()) {
            ItemStack cargo = golem.getCarriedItem();
            if (!onTimeout(cargo) && golem.getNavigation().isDone()) {
                // EmptyPlace is adjacent/idle, not independently five-tick gated.
                for (GolemMarker marker : golem.getMarkers()) {
                    if (golem.distanceToMarkerSqr(marker) < 4 && golem.placeCarriedAtMarker(marker) > 0) {
                        progressed();
                        return;
                    }
                }
            }
            if (golem.tickCount % 5 != 0) return;
            GolemMarker destination = onTimeout(cargo) ? null : findDestination(cargo);
            if (destination == null) {
                if (!onTimeout(cargo)) fail(cargo);
                clearRoute();
                // HomeReplace: retain cargo if home is missing/full/side-denied.
                if (golem.getNavigation().isDone() && golem.distanceToHomeSqr() <= 5 && golem.placeCarriedAtHome() > 0) progressed();
            } else if (target == null || !destinationAccepts(target, cargo)
                    || golem.markedWorldThrowTarget(target) && !golem.markedWorldThrowTarget(destination)) target = destination;
            return;
        }
        clearRoute();
        if (golem.tickCount % 5 != 0 || !golem.getNavigation().isDone() || golem.distanceToHomeSqr() > 5) return;
        GolemHomeInventory.Destination home = golem.homeInventory();
        if (home == null || !home.current()) return;
        boolean filtered = false;
        for (int row = 0; row < golem.getSlotCount(); row++) {
            ItemStack exemplar = golem.getFilter(row);
            if (exemplar.isEmpty()) continue;
            filtered = true;
            int slot = home.firstExtractable(exemplar, golem.getHomeFacing(), golem.itemMatchFlags());
            if (slot < 0 || !canDeliver(home.container().getItem(slot))) continue;
            if (golem.takeFromInventory(home, golem.getHomeFacing(), exemplar, golem.carrySpace()) > 0) progressed();
            return;
        }
        if (filtered) return;
        int[] exposed = home.slots(golem.getHomeFacing());
        if (exposed == null) return;
        for (int slot : exposed) {
            if (slot < 0 || slot >= home.container().getContainerSize()) continue;
            ItemStack actual = home.container().getItem(slot);
            if (!home.extracts(slot, actual, golem.getHomeFacing()) || !canDeliver(actual)) continue;
            if (golem.takeFromInventory(home, golem.getHomeFacing(), actual.copyWithCount(1), golem.carrySpace()) > 0) progressed();
            return; // First matching extractable slot; no assembled multi-slot batch.
        }
    }

    private void sortingTick() {
        if (golem.hasCargo()) {
            ItemStack cargo = golem.getCarriedItem(); // Probe only; the entity pays from its actual private field.
            boolean decisionTick = golem.tickCount % 5 == 0;
            // HomeReplace0 precedes Place1/Goto3, but only a timeout or the loss
            // of all seeded destinations authorizes returning just-taken cargo.
            if (decisionTick && shouldReturnSortingCargo(cargo)) {
                clearRoute();
                if (golem.getNavigation().isDone() && golem.distanceToHomeSqr() <= 5 && golem.placeCarriedAtHome() > 0) progressed();
                return;
            }
            if (!onTimeout(cargo) && golem.getNavigation().isDone()) {
                for (GolemMarker marker : golem.getMarkers()) {
                    if (golem.distanceToMarkerSqr(marker) < 4 && sortingInventoryAccepts(marker, cargo)
                            && golem.placeCarriedAtMarker(marker) > 0) {
                        progressed();
                        return;
                    }
                }
            }
            if (!decisionTick) return;
            GolemMarker destination = findDestination(cargo);
            if (destination == null) {
                fail(cargo);
                clearRoute(); // Shared ReturnHome can now walk the real remainder back.
            } else if (target == null || !sortingInventoryAccepts(target, cargo)) target = destination;
            return;
        }
        clearRoute();
        // HomeTakeSorting4: actual face-visible home slot order, NOT ghost rows
        // or stock totals. A candidate may match a later extractable source slot.
        if (golem.tickCount % 5 != 0 || !golem.getNavigation().isDone() || golem.distanceToHomeSqr() > 5) return;
        GolemHomeInventory.Destination home = golem.homeInventory();
        if (home == null || !home.current()) return;
        int[] exposed = home.slots(golem.getHomeFacing());
        if (exposed == null) return;
        for (int slot : exposed) {
            if (!home.current()) return;
            if (slot < 0 || slot >= home.container().getContainerSize()) continue;
            ItemStack candidate = home.container().getItem(slot);
            if (candidate.isEmpty() || !canDeliver(candidate) || home.firstExtractable(candidate, golem.getHomeFacing()) < 0) continue;
            if (golem.takeFromInventory(home, golem.getHomeFacing(), candidate.copyWithCount(1), golem.carrySpace()) > 0) {
                progressed();
                return;
            }
        }
    }

    boolean shouldReturnSortingCargo(ItemStack cargo) {
        return golem.getGolemCore() == GolemData.SORTING && !cargo.isEmpty() && !canDeliver(cargo);
    }
    boolean canDeliver(ItemStack exemplar) {
        if (exemplar.isEmpty() || onTimeout(exemplar)) return false;
        if (findDestination(exemplar) != null) return true;
        fail(exemplar);
        return false;
    }
    private boolean destinationAccepts(GolemMarker marker, ItemStack stack) {
        if (golem.getGolemCore() == GolemData.SORTING) return sortingInventoryAccepts(marker, stack);
        return inventoryAccepts(marker, stack) || golem.markedWorldThrowTarget(marker);
    }
    private boolean inventoryAccepts(GolemMarker marker, ItemStack stack) {
        if (!golem.markerAccepts(marker, stack)) return false; // Order: the stack's slot colour must accept the marker's colour.
        GolemHomeInventory.Destination destination = golem.markedInventory(marker);
        return destination != null && destination.current() && destination.roomFor(stack, marker.face()) > 0;
    }
    private boolean sortingInventoryAccepts(GolemMarker marker, ItemStack stack) {
        GolemHomeInventory.Destination destination = golem.markedSortingInventory(marker);
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
            failedItems.add(new FailedItem(stack.copyWithCount(1), System.currentTimeMillis() + 10_000L));
        }
    }

    private final class Route extends Goal {
        private GolemMarker pursued;
        private int budget;
        private BlockPos previous;
        private boolean running;

        Route() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            return golem.canWork() && interactionPause == 0 && !interactionReady && target != null
                    && (!golem.getNavigation().isDone() || golem.distanceToMarkerSqr(target) >= 4);
        }
        @Override public boolean canContinueToUse() {
            return golem.canWork() && interactionPause == 0 && target != null && target.equals(pursued)
                    && budget > 0 && !golem.getNavigation().isDone();
        }
        @Override public void start() {
            running = true;
            travelStarted = true;
            pursued = target;
            budget = 200;
            previous = golem.blockPosition();
            if (pursued != null && golem.distanceToMarkerSqr(pursued) < 4) {
                budget = 0;
                interactionReady = true;
                golem.getNavigation().stop();
                return;
            }
            if (pursued == null || !golem.loadedNavigationArea(pursued.pos())) { budget = 0; return; }
            golem.getNavigation().moveTo(pursued.pos().getX(), pursued.pos().getY(), pursued.pos().getZ(), 1);
        }
        @Override public void tick() {
            if (pursued == null) return;
            golem.getLookControl().setLookAt(pursued.pos().getX() + .5, pursued.pos().getY() + .5, pursued.pos().getZ() + .5);
            if (--budget != 0 || !golem.blockPosition().equals(previous) || !golem.loadedNavigationArea(golem.blockPosition())) return;
            Vec3 detour = DefaultRandomPos.getPos(golem, 2, 1);
            if (detour != null) {
                budget = 20;
                golem.getNavigation().moveTo(detour.x, detour.y, detour.z, 1);
            }
        }
        @Override public void stop() {
            // Give the adjacent take/place phase a turn before the lower-priority
            // native ReturnHome goal can start a fresh path and mask idle arrival.
            if (travelStarted && golem.getNavigation().isDone()) interactionReady = true;
            if (travelStarted && target != null && target.equals(pursued)) target = null;
            pursued = null;
            budget = 0;
            running = false;
            golem.getNavigation().stop();
            // Fill's watched identity survives Goto.stop for adjacent FillTake.
        }
    }
}
