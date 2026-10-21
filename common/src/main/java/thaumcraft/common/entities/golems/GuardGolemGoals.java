package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.items.golems.GolemDartRules;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemDecorations;
import thaumcraft.common.items.golems.GolemUpgrade;

/** Shared ordinary melee (and the dart launcher's ranged attack), with SEPARATE Guard4 hostile and Butcher9 surplus policies. */
final class GuardGolemGoals {
    private final EntityGolemBase golem;
    private final Melee melee = new Melee();
    private final Dart dart = new Dart();
    private final Retaliate retaliate = new Retaliate();
    private final Nearest nearest = new Nearest();
    private final OldestButcher oldest = new OldestButcher();
    private int attackCooldown; // Active melee updates only; NEVER reset by goal start/stop/pause.
    private boolean attempting;
    private LivingEntity strikeTarget;
    private int strikeCore = GolemData.NO_CORE;

    GuardGolemGoals(EntityGolemBase golem) { this.golem = golem; }

    void install() {
        int core = golem.getGolemCore();
        if (core != GolemData.GUARD && core != GolemData.BUTCHER) return;
        if (GolemDecorations.has(golem.getDecoration(), GolemDecorations.DART)) golem.addWorkGoal(GoalPriority.DART_ATTACK, dart); // Original AIDartAttack, priority 2.
        golem.addWorkGoal(GoalPriority.MELEE, melee);
        if (core == GolemData.GUARD) {
            golem.addTargetGoal(GoalPriority.HURT_BY_TARGET, retaliate);
            golem.addTargetGoal(GoalPriority.NEAREST_TARGET, nearest);
        } else golem.addTargetGoal(GoalPriority.OLDEST_TARGET, oldest); // NO retaliation/nearest-hostile goal for Butcher.
    }
    private boolean working() {
        int core = golem.getGolemCore();
        return (core == GolemData.GUARD || core == GolemData.BUTCHER) && !golem.isNoAi() && golem.canWork();
    }
    private boolean targetPolicy(LivingEntity target) {
        boolean valid = golem.isCombatTarget(target); // Explicit Entity dispatch; never Guard OR Animal.
        if (!valid && golem.getGolemCore() == GolemData.BUTCHER) {
            if (target != null && golem.getTarget() == target) golem.setTarget(null);
            if (melee.running && melee.victim == target) melee.stop();
        }
        return valid;
    }
    void reset() {
        boolean ours = golem.getGolemCore() == GolemData.GUARD || golem.getGolemCore() == GolemData.BUTCHER
                || melee.running || dart.running || retaliate.chosen != null || nearest.chosen != null || oldest.chosen != null;
        attempting = false;
        strikeTarget = null;
        strikeCore = GolemData.NO_CORE;
        melee.stop();
        dart.stop();
        retaliate.chosen = nearest.chosen = oldest.chosen = null;
        if (ours && golem.level() instanceof ServerLevel && golem.getTarget() != null) golem.setTarget(null);
    }
    void serverTick() {
        if (!working()) { reset(); return; }
        LivingEntity target = golem.getTarget();
        if (target != null && !targetPolicy(target)) {
            golem.setTarget(null);
            melee.stop();
            dart.stop();
        }
    }
    boolean hasCombatRoute() {
        LivingEntity target = golem.getTarget();
        if (working() && dart.running && dart.chosen == target && targetPolicy(target) && !golem.getNavigation().isDone()) return true;
        return working() && melee.running && targetPolicy(target) && melee.running && melee.victim == target
                && golem.getTarget() == target && !golem.getNavigation().isDone();
    }
    boolean ownsAttempt(LivingEntity target) {
        return attempting && working() && strikeCore == golem.getGolemCore() && strikeTarget == target
                && melee.running && melee.victim == target && golem.getTarget() == target;
    }
    private void strike(LivingEntity target) {
        if (attackCooldown > 0 || attempting || !melee.running || golem.getTarget() != target) return;
        attackCooldown = golem.getAttackSpeed(); // Pay the attempt, including native hurt(false).
        strikeCore = golem.getGolemCore();
        strikeTarget = target;
        attempting = true;
        try { golem.doHurtTarget(target); }
        finally { attempting = false; strikeTarget = null; strikeCore = GolemData.NO_CORE; }
    }

    private final class Melee extends Goal {
        private LivingEntity victim;
        private Path path;
        private int repath;
        private boolean running;

        Melee() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            LivingEntity target = golem.getTarget();
            if (!working() || !targetPolicy(target) || golem.getTarget() != target) return false;
            if (!golem.loadedNavigationArea(target.blockPosition())) return false;
            Path candidate = golem.getNavigation().createPath(target, 0);
            if (candidate == null || !working() || golem.getTarget() != target) return false; // No pathless in-reach fallback.
            victim = target;
            path = candidate;
            return true;
        }
        @Override public boolean canContinueToUse() {
            LivingEntity target = victim;
            return running && working() && golem.getTarget() == target && targetPolicy(target) && running
                    && victim == target && golem.getTarget() == target && !golem.getNavigation().isDone();
        }
        @Override public void start() {
            running = true;
            repath = 0;
            LivingEntity target = victim;
            if (!working() || !targetPolicy(target) || !running || victim != target || golem.getTarget() != target || path == null
                    || !golem.loadedNavigationArea(target.blockPosition())) { golem.getNavigation().stop(); return; }
            golem.getNavigation().moveTo(path, 1); // Native material attribute supplies speed.
        }
        @Override public void stop() {
            boolean wasRunning = running;
            running = false;
            victim = null;
            path = null;
            repath = 0;
            if (wasRunning) golem.getNavigation().stop();
        }
        @Override public void tick() {
            if (!running || !working()) return;
            if (attackCooldown > 0) --attackCooldown;
            LivingEntity target = victim;
            if (golem.getTarget() != target || !targetPolicy(target) || !running || victim != target || golem.getTarget() != target
                    || !golem.loadedNavigationArea(target.blockPosition())) {
                golem.getNavigation().stop();
                return;
            }
            golem.getLookControl().setLookAt(target, 30, 30);
            if (golem.getSensing().hasLineOfSight(target) && --repath <= 0) {
                repath = 4 + golem.getRandom().nextInt(7);
                golem.getNavigation().moveTo(target, 1);
            }
            if (attackCooldown == 0 && running && victim == target && golem.getTarget() == target
                    && golem.isWithinGuardReach(target) && golem.hasLineOfSight(target)) strike(target);
        }
    }

    /**
     * Original AIDartAttack: a target at least three blocks away is chased and shot at, one dart per interval (30 ticks, 8 fewer for every Air
     * upgrade), only while it is in sight and within 80% of the golem's range. The wait survives a stop only as half an interval. Nearer than
     * three blocks the melee goal (lower priority) takes over. Like the original it continues only while the golem has a path.
     */
    private final class Dart extends Goal {
        private LivingEntity chosen;
        private boolean running;
        private int wait;

        Dart() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private int interval() { return GolemDartRules.interval(golem.getUpgradeAmount(GolemUpgrade.AIR)); }
        private double distanceSq(LivingEntity target) { return golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()); }

        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            LivingEntity target = golem.getTarget();
            if (target == null || !working()) return false;
            if (!targetPolicy(target)) {
                golem.setTarget(null); // Original: not a valid target any more, forget it.
                return false;
            }
            if (GolemDartRules.tooClose(distanceSq(target))) return false;
            chosen = target;
            return true;
        }
        @Override public boolean canContinueToUse() { return running && canUse() && !golem.getNavigation().isDone(); }
        @Override public void start() {
            running = true;
            wait = GolemDartRules.firstDelay(interval());
        }
        @Override public void stop() {
            boolean wasRunning = running;
            running = false;
            chosen = null;
            wait = GolemDartRules.firstDelay(interval());
            if (wasRunning) golem.getNavigation().stop();
        }
        @Override public void tick() {
            LivingEntity target = chosen;
            if (!running || target == null || !working() || golem.getTarget() != target || !golem.loadedNavigationArea(target.blockPosition())) return;
            double distance = distanceSq(target);
            boolean sees = golem.getSensing().hasLineOfSight(target);
            golem.getNavigation().moveTo(target, 1);
            if (!sees) return;
            golem.getLookControl().setLookAt(target, 30, 30);
            wait = GolemDartRules.wait(wait);
            if (wait <= 0 && GolemDartRules.inReach(distance, golem.getRange())) {
                golem.shootDart(target);
                wait = interval();
            }
        }
    }

    private final class Retaliate extends Goal {
        private LivingEntity chosen;
        private int lastHurtTimestamp = -1;

        Retaliate() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            if (golem.getGolemCore() != GolemData.GUARD || !working() || golem.getLastHurtByMobTimestamp() == lastHurtTimestamp) return false;
            LivingEntity attacker = golem.getLastHurtByMob(); // Self hurt, NEVER owner's hurt target.
            if (!golem.isGuardTarget(attacker)) return false;
            chosen = attacker;
            return true;
        }
        @Override public boolean canContinueToUse() {
            return golem.getTarget() == chosen && golem.getLastHurtByMob() == chosen && golem.isGuardTarget(chosen);
        }
        @Override public void start() {
            lastHurtTimestamp = golem.getLastHurtByMobTimestamp();
            if (golem.isGuardTarget(chosen)) golem.setTarget(chosen);
        }
        @Override public void stop() {
            if (chosen != null && golem.getTarget() == chosen) golem.setTarget(null);
            chosen = null;
        }
    }

    private final class Nearest extends Goal {
        private LivingEntity chosen;

        Nearest() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (golem.getGolemCore() != GolemData.GUARD || !working() || !(golem.level() instanceof ServerLevel server)) return false;
            chosen = nearestGuardTarget(server);
            return chosen != null;
        }
        @Override public boolean canContinueToUse() {
            // Original AINearestAttackableTarget: the class extends the VANILLA EntityAITarget and never overrides its
            // continuation rule; it only writes its own PRIVATE targetDistance, so the parent's distance stays 0 and
            // the inherited rule (the squared distance against the parent's distance) never holds. The goal is therefore
            // re-evaluated on every tick and the golem's attack target is the nearest valid target at all times.
            // The port keeps the goal alive and re-picks the target in tick() instead - the same observable rule.
            // The 60 tick unseen counter and the extra golem distance of the previous port version belong to no
            // original rule of this goal: TC4's own AITarget has them, but both of its golem subclasses
            // (AIHurtByTarget, AINearestAttackableTarget) override the continuation rule.
            return chosen != null && golem.getTarget() == chosen && golem.isGuardTarget(chosen);
        }
        @Override public void start() {
            if (golem.isGuardTarget(chosen)) golem.setTarget(chosen);
        }
        @Override public void tick() {
            if (!working() || !(golem.level() instanceof ServerLevel server)) return;
            LivingEntity nearest = nearestGuardTarget(server);
            if (nearest == null) return; // The original keeps the old target when nothing valid is left.
            if (nearest != chosen) { chosen = nearest; golem.setTarget(nearest); }
        }
        @Override public void stop() {
            if (chosen != null && golem.getTarget() == chosen) golem.setTarget(null);
            chosen = null;
        }
        /** Original AINearestAttackableTarget.shouldExecute:47-60 - the nearest valid target of the inflated box. */
        private LivingEntity nearestGuardTarget(ServerLevel server) {
            LivingEntity result = null;
            double distance = Double.MAX_VALUE;
            // Original actor BOX, not an actor-distance sphere, random gate or five-tick scan.
            for (LivingEntity candidate : server.getEntitiesOfClass(LivingEntity.class,
                    golem.getBoundingBox().inflate(golem.getRange(), 4, golem.getRange()), golem::isGuardTarget)) {
                double next = golem.distanceToSqr(candidate);
                if (next < distance) { distance = next; result = candidate; }
            }
            return result;
        }
    }

    private final class OldestButcher extends Goal {
        private LivingEntity chosen;

        OldestButcher() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (golem.getGolemCore() != GolemData.BUTCHER || !working() || !(golem.level() instanceof ServerLevel server)
                    || !server.getServer().isSameThread()) return false;
            chosen = null;
            // Original B/S: lifetime descending, stable query-order ties, NO random/delay/nearest gate.
            // Permission callbacks happen AFTER the native section traversal has made its list.
            List<LivingEntity> candidates = new ArrayList<>(server.getEntitiesOfClass(LivingEntity.class,
                    golem.getBoundingBox().inflate(golem.getRange(), 4, golem.getRange()), LivingEntity::isAlive));
            candidates.sort(Comparator.comparingInt((LivingEntity animal) -> animal.tickCount).reversed());
            for (LivingEntity candidate : candidates) {
                if (!working() || golem.level() != server || golem.getGolemCore() != GolemData.BUTCHER) return false;
                if (golem.hasButcherSurplus(candidate)) { chosen = candidate; return true; }
            }
            return false;
        }
        @Override public boolean canContinueToUse() {
            LivingEntity target = chosen;
            return working() && golem.getGolemCore() == GolemData.BUTCHER && golem.getTarget() == target
                    && targetPolicy(target) && chosen == target && golem.getTarget() == target;
        }
        @Override public void start() {
            LivingEntity target = chosen;
            if (working() && golem.getGolemCore() == GolemData.BUTCHER && golem.hasButcherSurplus(target) && chosen == target) golem.setTarget(target);
        }
        @Override public void tick() {
            LivingEntity target = chosen;
            if (target != null) targetPolicy(target); // Invalid/protected or population2 stops the hunt immediately.
        }
        @Override public void stop() {
            if (chosen != null && golem.getTarget() == chosen) golem.setTarget(null);
            chosen = null;
        }
    }
}
