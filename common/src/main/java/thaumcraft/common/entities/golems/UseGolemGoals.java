package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.golems.GolemMarker;

/** Ordinary DEFAULT Use8 only: supplied wheat planting or genuinely bare-hand redstone. */
final class UseGolemGoals {
    record Target(GolemMarker marker, BlockState target, BlockState adjacent, BlockPos home, Direction facing) {}
    private record HomeSeed(GolemHomeInventory.Destination source, ItemStack actual) {}
    private record Timeout(ItemStack identity, long until) {}

    private final EntityGolemBase golem;
    private final Replace replace = new Replace();
    private final Click click = new Click();
    private final Take take = new Take();
    private final List<Timeout> timeouts = new ArrayList<>(); // Identity exemplars, NEVER paid items.
    private long nextTick;

    UseGolemGoals(EntityGolemBase golem) { this.golem = golem; }

    void install() {
        replace.delay = take.delay = 0;
        timeouts.clear();
        nextTick = (long) golem.tickCount + golem.getRandom().nextInt(6);
        golem.addWorkGoal(0, replace);
        golem.addWorkGoal(0, click);
        golem.addWorkGoal(4, take);
    }
    void reset() {
        click.stop();
        take.seed = null;
    }
    private boolean working() { return golem.useReady(); }
    boolean hasRoute() {
        return working() && (replace.delay > 0 || take.delay > 0
                || click.running && click.selected != null && !golem.getNavigation().isDone());
    }
    boolean ownsAttempt(Target target) { return click.attempting && click.running && click.selected == target; }
    boolean ownsTake(GolemHomeInventory.Destination source, ItemStack actual) {
        return take.attempting && take.seed != null && take.seed.source() == source && take.seed.actual() == actual;
    }
    boolean ownsReturn() { return replace.attempting; }

    static boolean loaded(ServerLevel level, BlockPos pos) {
        return pos != null && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
    static boolean seed(ItemStack actual) {
        return actual != null && !actual.isEmpty() && actual.is(Items.WHEAT_SEEDS)
                && actual.getCount() > 0 && actual.getCount() <= actual.getMaxStackSize()
                && ItemStack.isSameItemSameComponents(actual, new ItemStack(Items.WHEAT_SEEDS));
    }
    private boolean homePermits(ItemStack actual) {
        if (!(golem.level() instanceof ServerLevel level) || !loaded(level, golem.getHomePosition())
                || !loaded(level, golem.homeInventoryPosition())) return false;
        if (actual.isEmpty()) return golem.hasHomeWorldThrowTarget(); // Positive known non-BE/provider classification.
        GolemHomeInventory.Destination home = golem.homeInventory();
        return seed(actual) && home != null && home.current(); // An EMPTY inventory is still an inventory.
    }
    private static boolean applicable(ServerLevel level, GolemMarker marker, BlockState target, BlockState adjacent, ItemStack actual) {
        if (target.isAir() || !adjacent.isAir() || target.hasBlockEntity() || level.getBlockEntity(marker.pos()) != null) return false;
        if (!actual.isEmpty()) return seed(actual) && target.is(Blocks.FARMLAND) && marker.face() == Direction.UP;
        Block block = target.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && id.getNamespace().equals("minecraft")
                && (block.getClass() == LeverBlock.class || block.getClass() == ButtonBlock.class);
    }
    private boolean markerLoaded(ServerLevel level, GolemMarker marker) {
        // Reject dimensions BEFORE ANY block/BE reads. Original unused ceil(range/3) was NOT a bound.
        if (marker == null || !marker.dimension().equals(level.dimension().location())) return false;
        BlockPos home = golem.getHomePosition(), pos = marker.pos();
        double dx = (double) pos.getX() - home.getX(), dy = (double) pos.getY() - home.getY(), dz = (double) pos.getZ() - home.getZ();
        // The original computes ceil(range / 3) here and never uses it; the port's own home bound is the live range.
        return dx * dx + dy * dy + dz * dz <= golem.rangeSqr() && loaded(level, pos) && loaded(level, pos.relative(marker.face()));
    }
    Target findTarget(ItemStack actual) {
        if (!working() || actual == null || !golem.useItemAllowed(actual) || !homePermits(actual)
                || !(golem.level() instanceof ServerLevel level)) return null;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!markerLoaded(level, marker) || !golem.markerAccepts(marker, actual)) continue;
            BlockState target = level.getBlockState(marker.pos()), adjacent = level.getBlockState(marker.pos().relative(marker.face()));
            if (applicable(level, marker, target, adjacent, actual)) {
                return new Target(marker, target, adjacent, golem.getHomePosition().immutable(), golem.getHomeFacing());
            }
        }
        return null; // First applicable marker in stored order, never nearest or a world scan.
    }
    boolean current(Target job, ItemStack actual) {
        if (job == null || !working() || actual == null || !golem.useItemAllowed(actual) || !homePermits(actual)
                || !job.home().equals(golem.getHomePosition()) || job.facing() != golem.getHomeFacing()
                || !(golem.level() instanceof ServerLevel level) || !markerLoaded(level, job.marker())
                || !golem.markerAccepts(job.marker(), actual) || !golem.getMarkers().contains(job.marker())) return false;
        return level.getBlockState(job.marker().pos()) == job.target()
                && level.getBlockState(job.marker().pos().relative(job.marker().face())) == job.adjacent()
                && applicable(level, job.marker(), job.target(), job.adjacent(), actual);
    }
    private boolean timedOut(ItemStack actual) {
        long now = System.currentTimeMillis();
        timeouts.removeIf(timeout -> timeout.until() <= now);
        for (Timeout timeout : timeouts) if (ItemStack.isSameItemSameComponents(timeout.identity(), actual)) return true;
        return false;
    }
    private void timeout(ItemStack actual) {
        if (actual.isEmpty() || timedOut(actual)) return;
        if (timeouts.size() == 64) timeouts.remove(0);
        timeouts.add(new Timeout(actual.copyWithCount(1), System.currentTimeMillis() + 10000L));
    }
    boolean shouldReturn(ItemStack actual) {
        if (actual.isEmpty()) return false;
        if (timedOut(actual)) return true;
        if (findTarget(actual) != null) return false;
        timeout(actual);
        return true;
    }
    private HomeSeed findHomeSeed() {
        if (!(golem.level() instanceof ServerLevel level) || !loaded(level, golem.getHomePosition())
                || !loaded(level, golem.homeInventoryPosition())) return null;
        GolemHomeInventory.Destination home = golem.homeInventory();
        if (home == null || !home.current()) return null;
        int[] slots = home.slots(golem.getHomeFacing());
        if (slots == null) return null;
        for (int slot : slots) {
            if (!working() || !home.current()) return null;
            if (slot < 0 || slot >= home.container().getContainerSize()) continue;
            ItemStack actual = home.container().getItem(slot);
            if (!seed(actual) || !golem.useItemAllowed(actual) || !home.extracts(slot, actual, golem.getHomeFacing()) || timedOut(actual)) continue;
            if (findTarget(actual) != null) return new HomeSeed(home, actual);
            timeout(actual);
        }
        return null;
    }
    private boolean homeTransferReady() {
        return working() && golem.tickCount % 5 == 0 && golem.getNavigation().isDone() && golem.distanceToHomeSqr() <= 5;
    }

    private abstract class HomeTransfer extends Goal {
        int delay;
        boolean attempting;
        HomeTransfer() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canContinueToUse() { return working() && delay > 0; }
        @Override public void tick() { if (working() && delay > 0) --delay; }
        @Override public void stop() { /* A pause cannot turn the paid transfer's five active updates into zero. */ }
    }
    private final class Replace extends HomeTransfer {
        @Override public boolean canUse() {
            return working() && (delay > 0 || take.delay == 0 && homeTransferReady() && golem.hasCargo() && shouldReturn(golem.getCarriedItem()));
        }
        @Override public void start() {
            if (delay > 0) return;
            int before = golem.getCarriedItem().getCount();
            attempting = true;
            try { golem.placeCarriedAtHome(); }
            catch (RuntimeException failure) { Thaumcraft.LOG.warn("Golem Use home return failed; keeping actual remainder", failure); }
            finally {
                attempting = false;
                delay = golem.getCarriedItem().getCount() < before ? 5 : 0;
            }
        }
    }
    private final class Take extends HomeTransfer {
        private HomeSeed seed;
        @Override public boolean canUse() {
            if (delay > 0) return working();
            if (replace.delay > 0 || !homeTransferReady() || golem.hasCargo()) return false;
            seed = findHomeSeed();
            return seed != null;
        }
        @Override public void start() {
            if (delay > 0) return;
            attempting = true;
            try {
                if (seed != null) golem.takeFromInventory(seed.source(), golem.getHomeFacing(), seed.actual(), golem.getCarryLimit());
            } catch (RuntimeException failure) { Thaumcraft.LOG.warn("Golem Use home take failed; never reconstructing input stock", failure); }
            finally { attempting = false; seed = null; delay = golem.hasCargo() ? 5 : 0; }
        }
    }
    private final class Click extends Goal {
        private Target selected;
        private boolean running, attempting;
        private int budget;
        Click() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (!working() || replace.delay > 0 || take.delay > 0 || golem.tickCount < nextTick || !golem.getNavigation().isDone()) return false;
            ItemStack actual = golem.getCarriedItem();
            if (!actual.isEmpty() && timedOut(actual)) return false;
            // Authentic age bug, NOT the Air-upgrade count. Usually three ticks once age >= 4.
            nextTick = (long) golem.tickCount + 3L * Math.max(1L, 5L - golem.tickCount);
            selected = findTarget(actual);
            if (selected == null) { timeout(actual); return false; }
            return golem.loadedNavigationArea(selected.marker().pos());
        }
        @Override public void start() {
            running = true;
            budget = 200;
            if (!current(selected, golem.getCarriedItem()) || !golem.loadedNavigationArea(selected.marker().pos())) { stop(); return; }
            BlockPos pos = selected.marker().pos();
            golem.getNavigation().moveTo(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1);
        }
        @Override public boolean canContinueToUse() {
            return running && budget > 0 && current(selected, golem.getCarriedItem())
                    && golem.loadedNavigationArea(selected.marker().pos()) && !golem.getNavigation().isDone();
        }
        @Override public void tick() {
            if (!running || budget-- <= 0 || !current(selected, golem.getCarriedItem())
                    || !golem.loadedNavigationArea(selected.marker().pos())) { stop(); return; }
            BlockPos pos = selected.marker().pos();
            golem.getLookControl().setLookAt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 30, 30);
            if (golem.distanceToMarkerSqr(selected.marker()) > 4) return;
            attempting = true;
            try { golem.useMarkedBlock(selected); }
            finally { attempting = false; stop(); } // ONE attempt regardless of native result/publication failure.
        }
        @Override public void stop() {
            if (running) golem.getNavigation().stop();
            running = false;
            budget = 0;
            selected = null;
        }
    }
}
