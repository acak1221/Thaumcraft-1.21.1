package thaumcraft.common.lib.golems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemDartRules;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUpgrades;

/**
 * The decision rules of TC4's golem goal set (M11 golemancy), ported to native 1.21.1.
 *
 * <p>Every constant and every predicate the original {@code thaumcraft.common.entities.ai} goals used lives here as a
 * static, world-free function, so the whole decision layer is unit testable without a level. The native {@code Goal}
 * classes in {@code thaumcraft.common.entities.ai} only move, look and act; they ask this class what to do.</p>
 *
 * <p>Sources: the TC4 4.2.3.5 oracle under {@code thaumcraft/common/entities/ai/} and
 * {@code thaumcraft/common/entities/golems/GolemHelper.java}. Numbers are copied verbatim, including the original's
 * oddities (a five-tick search gate, a 200-tick path budget, a one-in-120 wander, a 1.25 speed factor).</p>
 */
public final class GolemAiRules {

    /* ------------------------------------------------------------------ shared cadence */
    /** Original {@code Config.golemDelay}: a goal may only look for work every fifth tick. */
    public static final int GOLEM_DELAY = 5;
    /** Original {@code count = 200}: the tick budget of a "go to" goal. */
    public static final int PATH_BUDGET = 200;
    /** Original {@code count = 20}: the tick budget of the little detour after a failed path. */
    public static final int DETOUR_BUDGET = 20;
    /** Original {@code GolemHelper.ADJACENT_RANGE}: the original compares the SQUARED distance against four. */
    public static final double ADJACENT_RANGE_SQ = 4.0;
    /** Original home container limit of the inventory goals: the SQUARED distance against five. */
    public static final double HOME_RANGE_SQ = 5.0;
    /** Original {@code AIReturnHome}: the golem is "home" when the SQUARED distance is below three. */
    public static final double HOME_RETURN_RANGE_SQ = 3.0;
    /** Original {@code countChest = 5}: the chest-lid animation budget after a container interaction. */
    public static final int CHEST_INTERACT_TICKS = 5;

    /* ------------------------------------------------------------------ combat */
    /** Original {@code AIAttackOnCollide}: ten ticks between two swings. */
    public static final int MELEE_ATTACK_TICKS = 10;
    /** Original {@code 4 + random.nextInt(7)}: the path refresh interval. */
    public static final int REPATH_BASE = 4, REPATH_SPAN = 7;
    /** Original {@code failedPathFindingPenalty += 10}. */
    public static final int FAILED_PATH_PENALTY = 10;
    /** Original: a target further away than 1024 blocks squared adds ten ticks of delay. */
    public static final double DISTANCE_PENALTY_FAR = 1024.0;
    /** Original: a target further away than 256 blocks squared adds five ticks of delay. */
    public static final double DISTANCE_PENALTY_NEAR = 256.0;
    /** Original {@code AIGolemAttackOnCollide}: the butcher rule needs more than two animals of the kind. */
    public static final int BUTCHER_POPULATION = 2;
    /** Original {@code AIAvoidCreeperSwell}: the search box around the golem. */
    public static final double CREEPER_AVOID_RANGE = 5.0, CREEPER_AVOID_HEIGHT = 3.0;
    /** Original {@code nearSpeed}: the golem flees faster when the creeper is close. */
    public static final float CREEPER_NEAR_SPEED = 1.25F, CREEPER_FAR_SPEED = 1.125F;
    /** Original: the creeper is "near" inside seven blocks (49 squared). */
    public static final double CREEPER_NEAR_RANGE_SQ = 49.0;
    /** Original {@code AICreeperSwell}: the creeper starts swelling inside nine blocks. */
    public static final double CREEPER_SWELL_RANGE = 9.0;
    /** Original {@code AICreeperSwell}: the swell is given up beyond seven blocks (49 squared). */
    public static final double CREEPER_SWELL_CANCEL_RANGE_SQ = 49.0;
    /** Original {@code AIDartAttack}: inside three blocks the dart goal gives up. */
    public static final double DART_MIN_DISTANCE_SQ = 9.0;
    /** Original {@code AIDartAttack}: a dart flies at most 80% of the golem's range. */
    public static final float DART_REACH_FACTOR = 0.8F;

    /* ------------------------------------------------------------------ interact */
    /** Original {@code AIHarvestCrops}: the SQUARED distance to the crop may not exceed four. */
    public static final double HARVEST_REACH_SQ = 4.0;
    /** Original {@code AIHarvestCrops}: the vertical scan of a column is plus or minus three. */
    public static final int CROP_SCAN_VERTICAL = 3;
    /** Original {@code AIHarvestCrops}: the adjacent-crop scan is five by five by three. */
    public static final int CROP_ADJACENT_HORIZONTAL = 2, CROP_ADJACENT_VERTICAL = 1;
    /** Original {@code AIHarvestLogs}: the adjacent-log scan is three by three by three. */
    public static final int LOG_ADJACENT = 1;
    /** Original {@code AIHarvestLogs}: the column scan of a log is plus or minus three. */
    public static final int LOG_SCAN_VERTICAL = 3;
    /** Original {@code AIUseItem}: the SQUARED distance to the marked block may not exceed four. */
    public static final double USE_ITEM_REACH_SQ = 4.0;
    /** Original {@code AIUseItem}: the next attempt is three times the remaining tick budget away. */
    public static final int USE_ITEM_TICK_BASE = 5, USE_ITEM_TICK_SPAN = 3;
    /** Original {@code AIFish}: the quality bonus of every direction with water beside the pond. */
    public static final float FISH_QUALITY_ADJACENT = 3.0E-5F;
    /** Original {@code AIFish}: the extra quality of a sky-lit pond. */
    public static final float FISH_QUALITY_SKY = 3.0E-5F;
    /** Original {@code AIFish}: the quality of every deeper water block below the pond. */
    public static final float FISH_QUALITY_DEPTH = 1.5E-5F;
    /** Original {@code AIFish}: every point of golem strength adds this much catch chance. */
    public static final float FISH_STRENGTH_BONUS = 1.5E-4F;
    /** Original {@code AIFish}: the junk chance drops and the treasure chance rises by this much per good direction. */
    public static final float FISH_JUNK_STEP = 0.005F, FISH_TREASURE_STEP = 0.00125F;
    /** Original {@code AIFish}: every deeper water block adds this much treasure chance. */
    public static final float FISH_DEPTH_TREASURE = 0.001F;
    /** Original {@code AIFish}: the cast lasts 300 to 499 ticks. */
    public static final int FISH_DELAY_BASE = 300, FISH_DELAY_SPAN = 200;
    /** Original {@code AIFish}: four horizontal directions are inspected around the pond. */
    public static final int FISH_DIRECTIONS = 4;
    /** Original {@code AIFish}: three blocks of depth are inspected below the pond. */
    public static final int FISH_DEPTH = 3;

    /* ------------------------------------------------------------------ fluid */
    /** Original {@code AIEssentiaGather}: one second between two gather attempts. */
    public static final long ESSENTIA_GATHER_DELAY_MS = 1000L;
    /** Original {@code AIEssentiaGather}: a tenth of a second before the next pulse. */
    public static final long ESSENTIA_GATHER_REPEAT_MS = 100L;
    /** Original {@code AIEssentiaEmpty}: the SQUARED distance to the jar may not exceed four. */
    public static final double ESSENTIA_EMPTY_REACH_SQ = 4.0;
    /** Original {@code AILiquidEmpty}: the SQUARED distance to the home may not exceed five. */
    public static final double LIQUID_EMPTY_REACH_SQ = 5.0;
    /** Original {@code AILiquidGather}: the first pulse happens after ten ticks. */
    public static final int LIQUID_GATHER_FIRST_PULSE = 10;
    /** Original {@code AILiquidGather}: the gather goal gives up after twenty ticks. */
    public static final int LIQUID_GATHER_MAX_TICKS = 20;
    /** Original: a bucket is a thousand millibuckets. */
    public static final int BUCKET_MB = 1000;
    /** Original {@code AILiquidGather}: a golem stops one bucket short of its limit. */
    public static final int FLUID_NEAR_FULL_MARGIN = 1000;
    /** Original {@code AILiquidGather}: the connected-fluid search is a three by three by three neighbourhood. */
    public static final int CONNECTED_FLUID_RADIUS = 1;

    /* ------------------------------------------------------------------ inventory */
    /** Original {@code AIItemPickup}: an item younger than five ticks is never picked up. */
    public static final int ITEM_PICKUP_DELAY = 5;
    /** Original {@code AIItemPickup}: the SQUARED distance to the item may not exceed two. */
    public static final double ITEM_PICKUP_REACH_SQ = 2.0;
    /** Original {@code AIEmptyDrop}/{@code AIHomeDrop}: a dropped item waits ten ticks before it may be picked up. */
    public static final int DROPPED_ITEM_DELAY = 10;

    /* ------------------------------------------------------------------ misc */
    /** Original {@code AIDoorInteract}: the SQUARED distance to the door may not exceed 2.25. */
    public static final double DOOR_REACH_SQ = 2.25;
    /** Original {@code AIDoorInteract}: the door interaction budget. */
    public static final int DOOR_BUDGET = 200;
    /** Original {@code AIDoorInteract}: the budget restarts with half its value. */
    public static final int DOOR_START_BUDGET = 100;
    /** Original {@code AIOpenDoor}: the open state lasts twenty ticks. */
    public static final int OPEN_DOOR_TICKS = 20;
    /** Original {@code AIWander}: one wander in a hundred and twenty. */
    public static final int WANDER_CHANCE = 120;
    /** Original {@code AIWander}: a wandering mob gives up after a hundred idle ticks. */
    public static final int WANDER_IDLE_LIMIT = 100;
    /** Original {@code AIWander}: the wander search box. */
    public static final int WANDER_HORIZONTAL = 10, WANDER_VERTICAL = 7;
    /** Original {@code AIConvertGrass}: one conversion in two hundred and fifty. */
    public static final int CONVERT_GRASS_CHANCE = 250;
    /** Original {@code AIConvertGrass}: the conversion takes forty ticks. */
    public static final int CONVERT_GRASS_TICKS = 40;
    /** Original {@code AIAltarFocus}: the SQUARED distance to the altar may not exceed sixteen. */
    public static final double ALTAR_FOCUS_DISTANCE_SQ = 16.0;
    /** Original {@code AIAltarFocus}: the focus is checked every fortieth tick. */
    public static final int ALTAR_FOCUS_INTERVAL = 40;
    /** Original {@code AIReturnHome}: every failed path adds five ticks of delay, up to fifty. */
    public static final int RETURN_HOME_DELAY_INCREMENT = 5, RETURN_HOME_DELAY_MAX = 50;

    /* ------------------------------------------------------------------ pech */
    /** Original {@code AIPechItemEntityGoto}: the pech looks for dropped items within sixteen blocks. */
    public static final double PECH_ITEM_RANGE = 16.0;
    /** Original {@code AIPechItemEntityGoto}: the squared search radius. */
    public static final double PECH_ITEM_RANGE_SQ = PECH_ITEM_RANGE * PECH_ITEM_RANGE;
    /** Original {@code AIPechItemEntityGoto}: the retry penalty grows by ten. */
    public static final int PECH_FAILED_PATH_PENALTY = 10;
    /** Original {@code AIPechItemEntityGoto}: the retry interval is four plus a random number below four. */
    public static final int PECH_RETRY_BASE = 4, PECH_RETRY_SPAN = 4;

    private GolemAiRules() {}

    /* ------------------------------------------------------------------ shared rules */

    /** Original {@code tickCount % Config.golemDelay}: the fifth-tick search gate. */
    public static boolean searchTick(int tickCount) { return tickCount % GOLEM_DELAY == 0; }

    /** Original {@code count = 200}: the path budget of a "go to" goal. */
    public static int pathBudget() { return PATH_BUDGET; }

    /** Original {@code count = 20}: the detour budget after a failed path. */
    public static int detourBudget() { return DETOUR_BUDGET; }

    /** Original {@code ADJACENT_RANGE}: a marker or container is adjacent when the SQUARED distance is below four. */
    public static boolean adjacent(double distanceSq) { return distanceSq < ADJACENT_RANGE_SQ; }

    /** Original home container limit of the inventory goals (the SQUARED distance against five). */
    public static boolean atHome(double distanceSq) { return distanceSq <= HOME_RANGE_SQ; }

    /** Original {@code AIReturnHome}: the golem counts as home when the SQUARED distance is below three. */
    public static boolean homeReached(double distanceSq) { return distanceSq < HOME_RETURN_RANGE_SQ; }

    /** Original {@code EntityGolemBase.getCarrySpace}: the room left in the golem's hands. */
    public static int carrySpace(int carryLimit, ItemStack carried) {
        if (carried == null || carried.isEmpty()) return carryLimit;
        return Math.min(carryLimit - carried.getCount(), carried.getMaxStackSize() - carried.getCount());
    }

    /* ------------------------------------------------------------------ combat rules */

    /**
     * Original {@code AIAttackOnCollide}/{@code AIGolemAttackOnCollide}: the reach is the attacker's width squared
     * four times over, plus the target's width.
     */
    public static double meleeReach(float attackerWidth, float targetWidth) {
        return (double) (attackerWidth * 2.0F * attackerWidth * 2.0F) + targetWidth;
    }

    /** Original {@code AIAttackOnCollide}: the path refresh interval, including the failed-path penalty. */
    public static int repathDelay(int failedPathFindingPenalty, int roll) {
        return failedPathFindingPenalty + REPATH_BASE + Math.floorMod(roll, REPATH_SPAN);
    }

    /** Original {@code AIAttackOnCollide}: ten extra ticks beyond 1024, five beyond 256. */
    public static int distancePenalty(double distanceSq) {
        if (distanceSq > DISTANCE_PENALTY_FAR) return 10;
        if (distanceSq > DISTANCE_PENALTY_NEAR) return 5;
        return 0;
    }

    /** Original {@code AINearestAttackableTargetSorter}: the nearest target comes first. */
    public static int compareDistanceSq(double first, double second) {
        return first < second ? -1 : first > second ? 1 : 0;
    }

    /** Original {@code AIOldestAttackableTargetSorter}: the oldest target comes first. */
    public static int compareAge(int first, int second) {
        return first > second ? -1 : first < second ? 1 : 0;
    }

    /** Original {@code AINearestButcherTarget}: the herd must hold more than two animals of the kind. */
    public static boolean butcherPopulationSufficient(int count) { return count > BUTCHER_POPULATION; }

    /** Original {@code targetChance}: a zero chance always passes, otherwise the roll has to be zero. */
    public static boolean targetChancePassed(int chance, int roll) {
        return chance <= 0 || Math.floorMod(roll, chance) == 0;
    }

    /** Original {@code farSpeed}: the golem's own speed times 1.125. */
    public static float avoidCreeperFarSpeed(float speed) { return speed * CREEPER_FAR_SPEED; }

    /** Original {@code nearSpeed}: the golem's own speed times 1.25. */
    public static float avoidCreeperNearSpeed(float speed) { return speed * CREEPER_NEAR_SPEED; }

    /** Original {@code AIAvoidCreeperSwell}: the creeper is near inside seven blocks. */
    public static boolean creeperIsNear(double distanceSq) { return distanceSq < CREEPER_NEAR_RANGE_SQ; }

    /** Original {@code AICreeperSwell}: the swell starts inside nine blocks. */
    public static boolean creeperSwells(double distanceSq) { return distanceSq < CREEPER_SWELL_RANGE; }

    /** Original {@code AICreeperSwell}: the swell is given up beyond seven blocks. */
    public static boolean creeperSwellEnds(double distanceSq) { return distanceSq > CREEPER_SWELL_CANCEL_RANGE_SQ; }

    /** Original {@code AILongRangeAttack}: the wielder keeps its distance. */
    public static boolean beyondLongRange(double distanceSq, double minDistance) {
        return distanceSq >= minDistance * minDistance;
    }

    /** Original {@code AIDartAttack}: the shot interval shrinks by eight ticks per Air upgrade. */
    public static int dartInterval(int airUpgrades) { return GolemDartRules.interval(airUpgrades); }

    /** Original {@code AIDartAttack}: the first shot waits half an interval. */
    public static int dartFirstDelay(int interval) { return GolemDartRules.firstDelay(interval); }

    /** Original {@code AIDartAttack}: nearer than three blocks the dart goal gives up. */
    public static boolean dartTooClose(double distanceSq) { return distanceSq < DART_MIN_DISTANCE_SQ; }

    /** Original {@code AIDartAttack}: the dart reaches 80% of the golem's range. */
    public static boolean dartInReach(double distanceSq, float range) { return GolemDartRules.inReach(distanceSq, range); }

    /**
     * Original {@code EntityGolemBase.isValidTarget} dispatch: a Guard may only attack what its target switches allow,
     * a Butcher only an animal that belongs to a herd of more than two.
     */
    public static boolean isValidTarget(EntityGolemBase golem, net.minecraft.world.entity.LivingEntity target) {
        if (golem == null || target == null) return false;
        int core = golem.getGolemCore();
        if (core == GolemData.GUARD) return golem.isGuardTarget(target);
        if (core == GolemData.BUTCHER) return golem.hasButcherSurplus(target);
        return false;
    }

    /* ------------------------------------------------------------------ interact rules */

    /** Original {@code AIHarvestCrops}: ten ticks per hardness point, never below ten ticks. */
    public static int cropBreakDelay(int golemStrength, float destroySpeed) {
        return (int) Math.max(10.0F, (20.0F - (float) golemStrength * 2.0F) * destroySpeed);
    }

    /** Original {@code AIHarvestLogs}: five ticks per hardness point, never below five ticks. */
    public static int logBreakDelay(int golemStrength, float destroySpeed) {
        return (int) Math.max(5.0F, (20.0F - (float) golemStrength * 3.0F) * destroySpeed);
    }

    /** Original {@code this.mod = this.delay / Math.round(this.delay / 6.0f)}: one pulse every sixth of the work. */
    public static int pulseMod(int delay) {
        if (delay <= 0) return 1;
        return delay / Math.round((float) delay / 6.0F);
    }

    /** Original {@code AIHarvestCrops}: the search radius is the square root of a quarter of the golem's range. */
    public static int cropSearchRadius(float range) { return (int) Math.floor(Math.sqrt(range / 4.0F)); }

    /** Original {@code AIHarvestLogs}: the search radius is the square root of a third of the golem's range. */
    public static int logSearchRadius(float range) { return (int) Math.floor(Math.sqrt(range / 3.0F)); }

    /** Original {@code AIFish}: the search radius is the square root of half the golem's range. */
    public static int fishSearchRadius(float range) { return (int) Math.floor(Math.sqrt(range / 2.0F)); }

    /** Original {@code AIHarvestLogs}: the log search tries four times as many columns as the radius is wide. */
    public static int logSearchAttempts(float range) { return (int) (logSearchRadius(range) * 4.0F); }

    /** Original {@code AIFish}: the quality of a pond with the given numbers of good directions, sky and depth. */
    public static float fishingQuality(int adjacentWater, int skyWater, int depthWater) {
        return FISH_QUALITY_ADJACENT * (adjacentWater + skyWater) + FISH_QUALITY_DEPTH * depthWater;
    }

    /** Original {@code AIFish}: the chance of any catch at all. */
    public static float fishingCatchChance(int adjacentWater, int skyWater, int depthWater, int golemStrength) {
        return fishingQuality(adjacentWater, skyWater, depthWater) + (float) golemStrength * FISH_STRENGTH_BONUS;
    }

    /** Original {@code AIFish}: the junk chance of a pond, Entropy lowering and good water lowering it further. */
    public static float fishingJunkChance(int adjacentWater, int skyWater, int entropyUpgrades) {
        return Mth.clamp(GolemUpgrades.fishJunkBase(entropyUpgrades) - FISH_JUNK_STEP * (adjacentWater + skyWater), 0.0F, 1.0F);
    }

    /** Original {@code AIFish}: the treasure chance of a pond, Order raising and good water raising it further. */
    public static float fishingTreasureChance(int adjacentWater, int skyWater, int depthWater, int orderUpgrades) {
        return Mth.clamp(GolemUpgrades.fishTreasureBase(orderUpgrades) + FISH_TREASURE_STEP * (adjacentWater + skyWater)
                + FISH_DEPTH_TREASURE * depthWater, 0.0F, 1.0F);
    }

    /** Original {@code AIFish}: the cast lasts 300 to 499 ticks. */
    public static int fishingDelay(int roll) { return FISH_DELAY_BASE + Math.floorMod(roll, FISH_DELAY_SPAN); }

    /** Original {@code AIUseItem}: the next attempt happens three times the remaining budget away. */
    public static int useItemNextTick(int tickCount, int remaining) {
        int budget = Math.max(1, USE_ITEM_TICK_BASE - remaining);
        return tickCount + budget * USE_ITEM_TICK_SPAN;
    }

    /* ------------------------------------------------------------------ fluid rules */

    /** Original {@code AILiquidGather}: the golem stops one bucket short of its limit. */
    public static boolean fluidNearFull(int carried, int fluidCarryLimit) {
        return carried > fluidCarryLimit - FLUID_NEAR_FULL_MARGIN;
    }

    /** Original {@code AILiquidGather}: the room left in the golem's fluid tank. */
    public static int fluidRoom(int carried, int fluidCarryLimit) {
        return Math.max(0, fluidCarryLimit - carried);
    }

    /** Original {@code AIEssentiaGather}: the number of essentia units a gather takes. */
    public static int essentiaGatherAmount(int available, int carried, int carryLimit) {
        return Math.min(available, carryLimit - carried);
    }

    /** Original {@code AIEssentiaGather}: the golem may only take essentia of the aspect it already carries. */
    public static boolean essentiaAspectMatches(Aspect carried, Aspect offered) {
        return carried == null || offered == null || carried == offered;
    }

    /** Original {@code AILiquidGather}: the connected-fluid search radius of a marked block. */
    public static float pumpDistance(float range) { return range * range; }

    /* ------------------------------------------------------------------ inventory rules */

    /** Original {@code AIItemPickup}: an item younger than five ticks is never picked up. */
    public static boolean pickupAllowed(int pickupDelay) { return pickupDelay < ITEM_PICKUP_DELAY; }

    /** Original {@code AIItemPickup}: the SQUARED distance to the item may not exceed two. */
    public static boolean pickupInReach(double distanceSq) { return distanceSq <= ITEM_PICKUP_REACH_SQ; }

    /**
     * Original {@code AIEmptyPlace}/{@code AISortingPlace}: a container accepts the stack when the placement changed
     * it, that is when the container did not already hold the very same stack.
     */
    public static boolean placementChanged(ItemStack result, ItemStack original) {
        return !ItemStack.isSameItemSameComponents(result == null ? ItemStack.EMPTY : result,
                original == null ? ItemStack.EMPTY : original)
                || result == null || original == null || result.getCount() != original.getCount();
    }

    /** Original {@code AIEmptyGoto}/{@code AIFillGoto}: the nearest container inside the golem's range wins. */
    public static boolean nearerThan(double candidate, double best) { return candidate < best; }

    /** Original {@code AIEmptyGoto}: the container has to be inside the squared range of the golem. */
    public static boolean withinRange(double distanceSq, float range) { return distanceSq <= range * range; }

    /* ------------------------------------------------------------------ misc rules */

    /** Original {@code AIDoorInteract}: the door is close enough inside 1.5 blocks. */
    public static boolean doorInReach(double distanceSq) { return distanceSq <= DOOR_REACH_SQ; }

    /** Original {@code AIWander}: the mob may only wander while it has been idle for less than a hundred ticks. */
    public static boolean mayWander(int idleTicks) { return idleTicks < WANDER_IDLE_LIMIT; }

    /** Original {@code AIWander}: one wander in a hundred and twenty. */
    public static boolean wanderRoll(int roll) { return Math.floorMod(roll, WANDER_CHANCE) == 0; }

    /** Original {@code AIConvertGrass}: one conversion in two hundred and fifty. */
    public static boolean convertGrassRoll(int roll) { return Math.floorMod(roll, CONVERT_GRASS_CHANCE) == 0; }

    /** Original {@code AIAltarFocus}: the altar is out of reach when the SQUARED distance exceeds sixteen. */
    public static boolean altarInReach(double distanceSq) { return distanceSq <= ALTAR_FOCUS_DISTANCE_SQ; }

    /** Original {@code AIReturnHome}: every failed path adds five ticks of delay, up to fifty. */
    public static int returnHomeDelay(int increment) { return Math.min(RETURN_HOME_DELAY_MAX, increment); }

    /** Original {@code AIReturnHome}: the delay increment grows by five per failure. */
    public static int returnHomeDelayIncrement(int increment) { return increment + RETURN_HOME_DELAY_INCREMENT; }

    /* ------------------------------------------------------------------ the golem cargo */

    /**
     * The cargo the ported goals move.
     *
     * <p>The 1.7 original kept the carried item in the public field {@code EntityGolemBase.itemCarried} and the
     * goals wrote that very field, so the goal set and the golem always agreed on one stack. The native golem keeps
     * the field private, so the goal set reaches it through the public cargo setters of {@link EntityGolemBase}.</p>
     */
    public interface Cargo {
        /** The stack the golem carries, never null. */
        ItemStack carried();
        /** Replaces the carried stack; false when the change was refused. */
        boolean setCarried(ItemStack stack);
        /** The golem's overall carrying capacity. */
        int carryLimit();
        /** The room left in the golem's hands. */
        int carrySpace();
        /** The golem's fluid tank in millibuckets. */
        int fluidCarryLimit();
        /** The essentia aspect the golem carries, or null. */
        Aspect aspect();
        /** The number of essentia units the golem carries. */
        int essentiaAmount();
        /** Replaces the essentia stock; false when the change was refused. */
        boolean setEssentia(Aspect aspect, int amount);
        /** The water stock in millibuckets. */
        int waterMb();
        /** Replaces the water stock in millibuckets; false when the change was refused. */
        boolean setWaterMb(int amount);
    }

    /**
     * The cargo of one golem: the golem's own carried stack, its essentia stock and its water tank.
     *
     * <p>The stock is not a side table any more: every value is read from and written to the golem itself, so the
     * Fill, Empty, Sorting and Use goals move the ORIGINAL item.</p>
     */
    public static final class NativeCargo implements Cargo {
        private final EntityGolemBase golem;

        NativeCargo(EntityGolemBase golem) {
            this.golem = golem;
        }

        @Override public ItemStack carried() { return golem.getCarriedItem(); }
        @Override public boolean setCarried(ItemStack stack) {
            if (golem.level().isClientSide) return false;
            golem.setCarriedItem(stack);
            return true;
        }
        @Override public int carryLimit() { return golem.getCarryLimit(); }
        @Override public int carrySpace() { return GolemAiRules.carrySpace(carryLimit(), carried()); }
        @Override public int fluidCarryLimit() { return golem.getFluidCarryLimit(); }
        @Override public Aspect aspect() { return golem.getEssentiaAspect(); }
        @Override public int essentiaAmount() { return golem.getEssentiaAmount(); }
        @Override public boolean setEssentia(Aspect aspect, int amount) {
            if (amount < 0 || amount > golem.getCarryLimit()) return false;
            if (amount == 0) {
                return golem.setEssentiaCargo(null, 0);
            }
            if (aspect == null) return false;
            Aspect current = golem.getEssentiaAspect();
            if (current != null && current != aspect) return false;
            return golem.setEssentiaCargo(aspect, amount);
        }
        @Override public int waterMb() { return golem.getWaterAmountMb(); }
        @Override public boolean setWaterMb(int amount) {
            if (amount < 0 || amount > golem.getFluidCarryLimit()) return false;
            return golem.setWaterCargoMb(amount);
        }
    }

    /** The cargo of the given golem; every goal of that golem reads and writes the golem's own cargo. */
    public static Cargo cargoOf(EntityGolemBase golem) {
        return new NativeCargo(golem);
    }

    /** The Air upgrade count of a golem, the original's {@code getUpgradeAmount(0)}. */
    public static int airUpgrades(EntityGolemBase golem) { return golem.getUpgradeAmount(GolemUpgrade.AIR); }
    /** The Fire upgrade count of a golem, the original's {@code getUpgradeAmount(2)}. */
    public static int fireUpgrades(EntityGolemBase golem) { return golem.getUpgradeAmount(GolemUpgrade.FIRE); }

    /** The Order upgrade count of a golem, the original's {@code getUpgradeAmount(4)}. */
    public static int orderUpgrades(EntityGolemBase golem) { return golem.getUpgradeAmount(GolemUpgrade.ORDER); }

    /** The Water upgrade count of a golem, the original's {@code getUpgradeAmount(3)}. */
    public static int waterUpgrades(EntityGolemBase golem) { return golem.getUpgradeAmount(GolemUpgrade.WATER); }

    /** The Entropy upgrade count of a golem, the original's {@code getUpgradeAmount(5)}. */
    public static int entropyUpgrades(EntityGolemBase golem) { return golem.getUpgradeAmount(GolemUpgrade.ENTROPY); }

    /**
     * The world-facing half of the inventory goals: the original's {@code GolemHelper} container lookups and its
     * {@code InventoryUtils} placement and extraction, ported to native containers. Every method here reads or writes the
     * world; the decisions that use them stay in the pure part of this class.
     */
    public static final class Containers {
        private Containers() {}

        /** The container of a block position, or null when the block is not a container. */
        public static Container container(ServerLevel server, BlockPos pos) {
            if (server == null || pos == null || !server.hasChunkAt(pos)) return null;
            BlockEntity tile = server.getBlockEntity(pos);
            return tile instanceof Container container ? container : null;
        }

        /** The other half of a double chest, or null. */
        public static Container doubleChest(ServerLevel server, Container container) {
            if (!(container instanceof ChestBlockEntity chest)) return null;
            BlockPos pos = chest.getBlockPos();
            if (!server.hasChunkAt(pos)) return null;
            BlockState state = server.getBlockState(pos);
            if (!(state.getBlock() instanceof ChestBlock block) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (!server.hasChunkAt(other)) return null;
            BlockEntity otherTile = server.getBlockEntity(other);
            return otherTile instanceof Container otherContainer ? otherContainer : null;
        }

        /** The slots of a container that a face may use. */
        public static int[] slots(Container container, Direction face) {
            if (container instanceof WorldlyContainer sided) return sided.getSlotsForFace(face);
            int[] all = new int[container.getContainerSize()];
            for (int slot = 0; slot < all.length; slot++) all[slot] = slot;
            return all;
        }

        /** The faces of the golem's markers of one block that accept the given slot colour. */
        public static List<Direction> markedSides(EntityGolemBase golem, BlockPos pos, int color) {
            List<Direction> sides = new ArrayList<>();
            for (GolemMarker marker : golem.getMarkers()) {
                if (!marker.pos().equals(pos)) continue;
                if (!GolemMarker.accepts(color, marker.color())) continue;
                if (!sides.contains(marker.face())) sides.add(marker.face());
            }
            return sides;
        }

        /** Whether the container may still take the stack through that face. */
        public static boolean hasRoom(Container container, Direction face, ItemStack stack) {
            if (container == null || stack == null || stack.isEmpty()) return false;
            for (int slot : slots(container, face)) {
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                if (!container.canPlaceItem(slot, stack)) continue;
                if (container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, stack, face)) continue;
                ItemStack actual = container.getItem(slot);
                int maximum = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(stack));
                if (actual.isEmpty() || ItemStack.isSameItemSameComponents(actual, stack)) {
                    if (maximum - actual.getCount() > 0) return true;
                }
            }
            return false;
        }

        /** Whether the container holds a matching stack that may be taken through that face. */
        public static boolean holds(Container container, Direction face, ItemStack exemplar) {
            if (container == null || exemplar == null || exemplar.isEmpty()) return false;
            for (int slot : slots(container, face)) {
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                ItemStack actual = container.getItem(slot);
                if (actual.isEmpty() || !ItemStack.isSameItemSameComponents(actual, exemplar)) continue;
                if (container instanceof WorldlyContainer sided && !sided.canTakeItemThroughFace(slot, actual, face)) continue;
                return true;
            }
            return false;
        }

        /**
         * Original {@code InventoryUtils.placeItemStackIntoInventory}: the stack is merged into matching stacks first and
         * then into empty ones. With {@code commit} false nothing is written and the untouched remainder is returned.
         */
        public static ItemStack place(Container container, Direction face, ItemStack stack, boolean commit) {
            if (container == null || stack == null || stack.isEmpty()) return stack == null ? ItemStack.EMPTY : stack.copy();
            ItemStack remaining = stack.copy();
            int[] slots = slots(container, face);
            for (int slot : slots) {
                if (remaining.isEmpty()) break;
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                ItemStack actual = container.getItem(slot);
                if (actual.isEmpty() || !ItemStack.isSameItemSameComponents(actual, remaining)) continue;
                if (!container.canPlaceItem(slot, remaining)) continue;
                if (container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, remaining, face)) continue;
                int maximum = Math.min(remaining.getMaxStackSize(), container.getMaxStackSize(remaining));
                int room = maximum - actual.getCount();
                if (room <= 0) continue;
                int moved = Math.min(room, remaining.getCount());
                if (commit) {
                    actual.grow(moved);
                    container.setItem(slot, actual);
                }
                remaining.shrink(moved);
            }
            for (int slot : slots) {
                if (remaining.isEmpty()) break;
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                if (!container.getItem(slot).isEmpty()) continue;
                if (!container.canPlaceItem(slot, remaining)) continue;
                if (container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, remaining, face)) continue;
                int maximum = Math.min(remaining.getMaxStackSize(), container.getMaxStackSize(remaining));
                int moved = Math.min(maximum, remaining.getCount());
                if (commit) container.setItem(slot, remaining.split(moved));
                else remaining.shrink(moved);
            }
            return remaining;
        }

        /**
         * Original {@code InventoryUtils.extractStack}: the first matching stack of the exposed slots is taken. With
         * {@code commit} false nothing is written and the amount that would be taken is returned.
         */
        public static ItemStack extract(Container container, Direction face, ItemStack exemplar, int requested, boolean commit) {
            if (container == null || requested <= 0) return null;
            for (int slot : slots(container, face)) {
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                ItemStack actual = container.getItem(slot);
                if (actual.isEmpty()) continue;
                if (exemplar != null && !exemplar.isEmpty() && !ItemStack.isSameItemSameComponents(actual, exemplar)) continue;
                if (container instanceof WorldlyContainer sided && !sided.canTakeItemThroughFace(slot, actual, face)) continue;
                int moved = Math.min(actual.getCount(), requested);
                if (moved <= 0) continue;
                if (!commit) return actual.copyWithCount(moved);
                ItemStack taken = actual.split(moved);
                if (actual.isEmpty()) container.setItem(slot, ItemStack.EMPTY);
                else container.setChanged();
                return taken;
            }
            return null;
        }

        /** The number of matching items the container shows through that face. */
        public static long visibleCount(Container container, Direction face, ItemStack exemplar) {
            if (container == null || exemplar == null || exemplar.isEmpty()) return 0;
            long count = 0;
            for (int slot : slots(container, face)) {
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                ItemStack actual = container.getItem(slot);
                if (!actual.isEmpty() && ItemStack.isSameItemSameComponents(actual, exemplar)) count += actual.getCount();
            }
            return count;
        }
    }
}
