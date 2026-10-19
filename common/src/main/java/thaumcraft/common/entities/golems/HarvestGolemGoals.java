package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrades;

/** One ordinary farmer goal: private columns, one exact target, no cargo or marker work. */
final class HarvestGolemGoals extends Goal {
    private final EntityGolemBase golem;
    private final List<BlockPos> columns = new ArrayList<>();
    private int columnsRadius;
    private BlockPos scanHome;
    private BlockPos target;
    private BlockState expected;
    private int delay = -1;
    private int maxDelay = 1;
    private int pulseMod = 1;
    private int budget;
    private boolean running;
    private boolean attempting;

    HarvestGolemGoals(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Original AIHarvestCrops:84 {@code ceil(range / 4)}, read live: a Water upgrade changes it without a new goal. */
    private int radius() { return GolemUpgrades.workRadius(golem.getRange(), 4); }

    void install() { golem.addWorkGoal(2, this); }
    boolean hasTarget() { return target != null || running; }
    void reset() {
        try { stop(); }
        finally { columns.clear(); scanHome = null; }
    }

    /** Actual server-tick admission, independent of native new-goal throttling. */
    void serverTick() {
        if (!(golem.level() instanceof ServerLevel server) || !working()) { reset(); return; }
        BlockPos home = golem.getHomePosition();
        if (scanHome == null) scanHome = home.immutable();
        else if (!scanHome.equals(home)) { reset(); scanHome = home.immutable(); }
        if (target != null && !currentTarget(server)) clearTarget();
        if (running || target != null || delay >= 0 || golem.tickCount % 5 != 0 || !golem.getNavigation().isDone()) return;
        int radius = radius();
        if (columns.isEmpty() || columnsRadius != radius) { // A Water upgrade mid-scan restarts the column shuffle at the new radius.
            columns.clear();
            columnsRadius = radius;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                columns.add(home.offset(x, 0, z));
            }
            // Native RandomSource, with the original shuffled-without-replacement order.
            for (int i = columns.size(); i > 1; --i) Collections.swap(columns, i - 1, golem.getRandom().nextInt(i));
        }
        BlockPos column = columns.remove(0); // Exactly ONE column per eligible five-tick scan.
        for (int dy = -3; dy <= 3; dy++) {
            BlockPos pos = new BlockPos(column.getX(), home.getY() + dy, column.getZ());
            if (select(server, pos)) return;
        }
    }

    private boolean working() { return golem.getGolemCore() == GolemData.HARVEST && golem.canWork(); }
    private boolean inHomeBounds(BlockPos pos) {
        BlockPos home = golem.getHomePosition();
        int radius = radius();
        return Math.abs((long) pos.getX() - home.getX()) <= radius
                && Math.abs((long) pos.getY() - home.getY()) <= radius
                && Math.abs((long) pos.getZ() - home.getZ()) <= radius;
    }
    private boolean loadedTarget(ServerLevel server, BlockPos pos) {
        return inHomeBounds(pos) && !server.isOutsideBuildHeight(pos)
                && server.getWorldBorder().isWithinBounds(pos) && server.hasChunkAt(pos);
    }
    private boolean select(ServerLevel server, BlockPos pos) {
        if (!loadedTarget(server, pos)) return false;
        BlockState state = server.getBlockState(pos);
        if (!GolemCrops.ripe(server, pos, state)) return false;
        target = pos.immutable();
        expected = state;
        delay = -1;
        return true;
    }
    private boolean currentTarget(ServerLevel server) {
        return working() && target != null && expected != null && loadedTarget(server, target)
                && GolemCrops.ripe(server, target, expected);
    }
    boolean ownsAttempt(BlockPos pos, BlockState state) {
        return running && attempting && delay == 0 && target != null && target.equals(pos) && expected == state;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        return !running && golem.level() instanceof ServerLevel server && currentTarget(server);
    }
    @Override public boolean canContinueToUse() {
        return running && budget > 0 && golem.level() instanceof ServerLevel server && currentTarget(server)
                && (delay > 0 || !golem.getNavigation().isDone());
    }
    @Override public void start() { running = true; startPath(); }
    private void startPath() {
        budget = 200;
        if (!(golem.level() instanceof ServerLevel server) || !currentTarget(server) || !golem.loadedNavigationArea(target)) {
            clearTarget();
            return;
        }
        golem.getNavigation().moveTo(target.getX() + .5, target.getY() + .5, target.getZ() + .5, 1);
    }

    @Override public void tick() {
        if (!running || !(golem.level() instanceof ServerLevel server)) return;
        if (budget <= 0 || !currentTarget(server)) { clearTarget(); return; }
        --budget;
        golem.getLookControl().setLookAt(target.getX() + .5, target.getY() + .5, target.getZ() + .5, 30, 30);
        if (golem.distanceToSqr(target.getX() + .5, target.getY() + .5, target.getZ() + .5) > 4) return;
        if (delay < 0) {
            maxDelay = delay = (int) Math.max(10.0F, (20.0F - 2.0F * golem.getGolemStrength()) * expected.getDestroySpeed(server, target));
            pulseMod = delay / Math.round(delay / 6.0F);
        }
        if (delay <= 0) return;
        --delay; // Only work time in reach; travel still consumes the pursuit budget.
        if (delay > 0 && delay % pulseMod == 0 && golem.getNavigation().isDone()) {
            golem.startActionTimer();
            SoundType sound = expected.getSoundType();
            server.playSound(null, target.getX() + .5, target.getY() + .5, target.getZ() + .5,
                    sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + .7F) / 8.0F, sound.getPitch() * .5F);
            server.destroyBlockProgress(golem.getId(), target, (int) (9.0F * (1.0F - (float) delay / maxDelay)));
        }
        if (delay != 0) return;
        BlockPos completed = target;
        boolean harvested;
        try {
            attempting = true;
            harvested = golem.harvestCrop(completed, expected); // ONE guarded native commit attempt.
        } finally {
            attempting = false;
            clearTarget(); // Refusal/exception also retires this old state; never retick delay0.
        }
        if (!harvested || golem.level() != server || !working()) return;
        golem.startActionTimer();
        // Original traversal order, including homeY±4 for chained neighbors.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 1; dy++) {
            if (select(server, completed.offset(dx, dy, dz))) { startPath(); return; }
        }
    }

    private void clearTarget() {
        BlockPos old = target;
        target = null;
        expected = null;
        delay = -1;
        maxDelay = pulseMod = 1;
        budget = 0;
        attempting = false;
        try {
            if (old != null && golem.level() instanceof ServerLevel server) server.destroyBlockProgress(golem.getId(), old, -1);
        } finally {
            if (running) golem.getNavigation().stop();
        }
    }
    @Override public void stop() {
        try { clearTarget(); }
        finally { running = false; }
    }
}
