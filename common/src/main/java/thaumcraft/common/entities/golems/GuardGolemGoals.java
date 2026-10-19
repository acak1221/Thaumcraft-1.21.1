package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.items.golems.GolemData;

/** Shared ordinary melee, with SEPARATE Guard4 hostile and Butcher9 surplus policies. */
final class GuardGolemGoals {
    private final EntityGolemBase golem;
    private final Melee melee = new Melee();
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
        golem.addWorkGoal(3, melee);
        if (core == GolemData.GUARD) {
            golem.addTargetGoal(1, retaliate);
            golem.addTargetGoal(2, nearest);
        } else golem.addTargetGoal(1, oldest); // NO retaliation/nearest-hostile goal for Butcher.
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
                || melee.running || retaliate.chosen != null || nearest.chosen != null || oldest.chosen != null;
        attempting = false;
        strikeTarget = null;
        strikeCore = GolemData.NO_CORE;
        melee.stop();
        retaliate.chosen = nearest.chosen = oldest.chosen = null;
        if (ours && golem.level() instanceof ServerLevel && golem.getTarget() != null) golem.setTarget(null);
    }
    void serverTick() {
        if (!working()) { reset(); return; }
        LivingEntity target = golem.getTarget();
        if (target != null && !targetPolicy(target)) {
            golem.setTarget(null);
            melee.stop();
        }
    }
    boolean hasCombatRoute() {
        LivingEntity target = golem.getTarget();
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
        private int unseen;

        Nearest() { setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (golem.getGolemCore() != GolemData.GUARD || !working() || !(golem.level() instanceof ServerLevel server)) return false;
            chosen = null;
            double distance = Double.MAX_VALUE;
            // Original actor BOX, not an actor-distance sphere, random gate or five-tick scan.
            for (LivingEntity candidate : server.getEntitiesOfClass(LivingEntity.class,
                    golem.getBoundingBox().inflate(golem.getRange(), 4, golem.getRange()), golem::isGuardTarget)) {
                double next = golem.distanceToSqr(candidate);
                if (next < distance) { distance = next; chosen = candidate; }
            }
            return chosen != null;
        }
        @Override public boolean canContinueToUse() {
            double follow = golem.getAttributeValue(Attributes.FOLLOW_RANGE);
            return golem.getTarget() == chosen && golem.isGuardTarget(chosen)
                    && golem.distanceToSqr(chosen) <= follow * follow && unseen <= 60;
        }
        @Override public void start() {
            unseen = 0;
            if (golem.isGuardTarget(chosen)) golem.setTarget(chosen);
        }
        @Override public void tick() {
            if (!golem.isGuardTarget(chosen)) return;
            if (golem.loadedNavigationArea(chosen.blockPosition()) && golem.getSensing().hasLineOfSight(chosen)) unseen = 0;
            else ++unseen;
        }
        @Override public void stop() {
            if (chosen != null && golem.getTarget() == chosen) golem.setTarget(null);
            chosen = null;
            unseen = 0;
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
