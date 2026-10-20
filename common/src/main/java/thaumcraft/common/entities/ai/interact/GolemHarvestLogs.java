package thaumcraft.common.entities.ai.interact;

import java.util.EnumSet;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHarvestLogs} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/interact/AIHarvestLogs.java}).
 *
 * <p>The golem looks for a log, walks to it and breaks it: the breaking takes {@code max(5, (20 - 3 * strength) * hardness)}
 * ticks and pulses every sixth of that time. A trunk that stands on other logs is cut at its lowest block, and afterwards
 * the golem looks for the next log around the one it just cut.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemHarvestLogs extends Goal {
    /** The original 1.7 goal priority of {@code AIHarvestLogs}. */
    public static final int PRIORITY = 2;

    private final EntityGolemBase golem;
    private BlockPos target;
    private BlockState expected;
    private int delay = -1;
    private int maxDelay = 1;
    private int pulseMod = 1;
    private int count;

    public GolemHarvestLogs(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (delay >= 0) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!golem.getNavigation().isDone()) return false;
        BlockPos found = findLog();
        if (found == null) return false;
        target = found;
        expected = golem.level().getBlockState(found);
        return true;
    }

    @Override public boolean canContinueToUse() {
        if (target == null || expected == null) return false;
        if (!golem.level().getBlockState(target).equals(expected)) return false;
        if (count-- <= 0) return false;
        return delay > 0 || isLog(target) || !golem.getNavigation().isDone();
    }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        golem.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() {
        if (target != null && golem.level() instanceof ServerLevel server) {
            server.destroyBlockProgress(golem.getId(), target, -1);
        }
        delay = -1;
    }

    @Override public void tick() {
        if (target == null || expected == null) return;
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 30, 30);
        double distance = golem.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distance > GolemAiRules.HARVEST_REACH_SQ) return;
        if (delay < 0) {
            maxDelay = delay = GolemAiRules.logBreakDelay(golem.getGolemStrength(),
                    expected.getDestroySpeed(golem.level(), target));
            pulseMod = GolemAiRules.pulseMod(delay);
        }
        if (delay <= 0) return;
        if (--delay > 0) {
            if (delay % pulseMod != 0 || !golem.getNavigation().isDone()) return;
            if (golem.level() instanceof ServerLevel server) {
                server.playSound(null, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                        expected.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                        (expected.getSoundType().getVolume() + 0.7F) / 8.0F, expected.getSoundType().getPitch() * 0.5F);
                server.destroyBlockProgress(golem.getId(), target, (int) (9.0F * (1.0F - (float) delay / (float) maxDelay)));
            }
            return;
        }
        harvest();
        if (isLog(target)) {
            delay = -1;
            expected = golem.level().getBlockState(target);
            start();
        } else {
            checkAdjacent();
        }
    }

    /** Original {@code harvest}: the log is broken and its drops are left on the ground. */
    private void harvest() {
        count = GolemAiRules.pathBudget();
        if (!(golem.level() instanceof ServerLevel server)) return;
        if (!server.getBlockState(target).equals(expected)) return;
        server.destroyBlock(target, true, golem);
    }

    /** Original {@code checkAdjacent}: the next log around the one just cut. */
    private void checkAdjacent() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        int radius = GolemAiRules.logSearchRadius(golem.getRange());
        BlockPos home = golem.getHomePosition();
        for (int dx = -GolemAiRules.LOG_ADJACENT; dx <= GolemAiRules.LOG_ADJACENT; dx++) {
            for (int dz = -GolemAiRules.LOG_ADJACENT; dz <= GolemAiRules.LOG_ADJACENT; dz++) {
                for (int dy = -GolemAiRules.LOG_ADJACENT; dy <= GolemAiRules.LOG_ADJACENT; dy++) {
                    BlockPos pos = target.offset(dx, dy, dz);
                    if (Math.abs(home.getX() - pos.getX()) > radius || Math.abs(home.getY() - pos.getY()) > radius
                            || Math.abs(home.getZ() - pos.getZ()) > radius) continue;
                    if (!isLog(pos)) continue;
                    target = pos.immutable();
                    expected = server.getBlockState(pos);
                    delay = -1;
                    start();
                    return;
                }
            }
        }
    }

    /**
     * Original {@code findLog}: a random column is tried four times the radius in attempts, and the lowest block of a trunk
     * is the one that is cut.
     */
    private BlockPos findLog() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        net.minecraft.util.RandomSource random = golem.getRandom();
        int radius = GolemAiRules.logSearchRadius(golem.getRange());
        int attempts = GolemAiRules.logSearchAttempts(golem.getRange());
        BlockPos home = golem.getHomePosition();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = home.getX() + random.nextInt(1 + radius * 2) - radius;
            int y = home.getY() + random.nextInt(1 + radius) - radius / 2;
            int z = home.getZ() + random.nextInt(1 + radius * 2) - radius;
            BlockPos pos = new BlockPos(x, y, z);
            if (!isLog(pos)) continue;
            double distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            int down = 1;
            while (isLog(pos.below(down)) && golem.distanceToSqr(pos.getX() + 0.5, pos.getY() - down + 0.5, pos.getZ() + 0.5) < distance) {
                pos = pos.below(down);
                distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                ++down;
            }
            return pos.immutable();
        }
        return null;
    }

    /** Original {@code Utils.isWoodLog}: an ordinary log, and nothing else. */
    boolean isLog(BlockPos pos) {
        if (!(golem.level() instanceof ServerLevel server) || !server.hasChunkAt(pos)) return false;
        return server.getBlockState(pos).is(BlockTags.LOGS);
    }
}
