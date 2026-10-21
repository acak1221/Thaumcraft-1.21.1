package thaumcraft.common.lib.entities;

/**
 * Pure decision rules of the TC4 projectiles and their supporting entities.
 * <p>
 * Every value below is taken from the TC4 4.2.3.5 oracle:
 * <ul>
 *   <li>{@code thaumcraft/common/entities/projectile/EntityBottleTaint.java}</li>
 *   <li>{@code thaumcraft/common/entities/projectile/EntityGolemOrb.java}</li>
 *   <li>{@code thaumcraft/common/entities/projectile/EntityPrimalArrow.java}</li>
 *   <li>{@code thaumcraft/common/entities/EntityFollowingItem.java}</li>
 *   <li>{@code thaumcraft/common/entities/InventoryMob.java} and {@code InventoryPech.java}</li>
 * </ul>
 * The 1.7.10 semantics behind two of the srg names were recovered from the
 * official 1.7.10 {@code joined.srg}: {@code func_70182_d} is
 * {@code net/minecraft/entity/projectile/EntityThrowable/func_70182_d} (the
 * inherited launch speed) and {@code func_70183_g} is
 * {@code net/minecraft/entity/projectile/EntityThrowable/func_70183_g} (the
 * inherited launch pitch offset, {@code -20} for the vanilla potion and
 * experience bottles, which TC4 copies for the bottle of taint).
 */
public final class ProjectileRules {
    private ProjectileRules() {}

    // ------------------------------------------------------------------
    // Bottle of taint - EntityBottleTaint
    // ------------------------------------------------------------------

    /** Original {@code func_70185_h} gravity of the thrown bottle. */
    public static final float BOTTLE_TAINT_GRAVITY = 0.05F;
    /** Original {@code func_70182_d} launch speed of the thrown bottle. */
    public static final float BOTTLE_TAINT_LAUNCH_SPEED = 0.5F;
    /** Original {@code func_70183_g} launch pitch offset: the bottle leaves the hand 20 degrees above the aim. */
    public static final float BOTTLE_TAINT_PITCH_OFFSET = -20F;
    /** Original splash box: a zero size box at the impact point inflated by five on every axis. */
    public static final double BOTTLE_TAINT_SPLASH_RADIUS = 5D;
    /** Original taint poison duration in ticks. */
    public static final int BOTTLE_TAINT_POISON_TICKS = 100;
    /** Original number of taint fibre placement attempts. */
    public static final int BOTTLE_TAINT_FIBRE_ATTEMPTS = 10;
    /** Original random spread of a taint fibre attempt on the x and z axes. */
    public static final float BOTTLE_TAINT_FIBRE_SPREAD = 5F;
    /** Original taint fibre metadata (the plain fibre state). */
    public static final int BOTTLE_TAINT_FIBRE_METADATA = 0;

    /**
     * The original poisons every living entity in the splash box that is
     * neither a tainted mob nor undead.
     */
    public static boolean bottleTaintAffects(boolean taintedMob, boolean undead) {
        return !taintedMob && !undead;
    }

    /** Original {@code (int)((first - second) * 5.0F)} fibre offset. */
    public static int bottleTaintFibreOffset(float first, float second) {
        return (int) ((first - second) * BOTTLE_TAINT_FIBRE_SPREAD);
    }

    /**
     * The original only grows fibres on a replaceable block that rests on a
     * full cube. The biome is converted before this test, never after.
     */
    public static boolean bottleTaintFibreFits(boolean solidBelow, boolean replaceableAbove) {
        return solidBelow && replaceableAbove;
    }

    // ------------------------------------------------------------------
    // Golem orb - EntityGolemOrb
    // ------------------------------------------------------------------

    /** Original lifetime of the red (cultist) orb in ticks. */
    public static final int GOLEM_ORB_LIFETIME_RED = 240;
    /** Original lifetime of the blue (golem) orb in ticks. */
    public static final int GOLEM_ORB_LIFETIME_BLUE = 160;
    /** Original homing acceleration per tick. */
    public static final double GOLEM_ORB_ACCELERATION = 0.2D;
    /** Original homing speed clamp on every axis. */
    public static final double GOLEM_ORB_MAX_SPEED = 0.25D;
    /** Original damage factor of the red orb. */
    public static final float GOLEM_ORB_DAMAGE_RED = 1F;
    /** Original damage factor of the blue orb. */
    public static final float GOLEM_ORB_DAMAGE_BLUE = 0.6F;

    /**
     * Original damage: the thrower's attack damage attribute times the orb
     * factor. The oracle reads {@code SharedMonsterAttributes.attackDamage}
     * ({@code field_111264_e}) of the thrower, not of the victim.
     */
    public static float golemOrbDamage(float throwerAttackDamage, boolean red) {
        return throwerAttackDamage * (red ? GOLEM_ORB_DAMAGE_RED : GOLEM_ORB_DAMAGE_BLUE);
    }

    /** Original lifetime in ticks; the orb dies once it is older than this. */
    public static int golemOrbLifetime(boolean red) {
        return red ? GOLEM_ORB_LIFETIME_RED : GOLEM_ORB_LIFETIME_BLUE;
    }

    /**
     * Original homing step: the delta is divided by the SQUARED distance to the target and
     * then scaled by 0.2. The original read {@code getDistanceSqToEntity}, so the pull of the
     * orb weakens with the square of the distance, and the port used the plain distance,
     * which made the orb home far stronger than the original.
     */
    public static double golemOrbHoming(double velocity, double delta, double squaredDistance) {
        if (!(squaredDistance > 1E-6D)) return velocity;
        return velocity + (delta / squaredDistance) * GOLEM_ORB_ACCELERATION;
    }

    /** Original {@code MathHelper.clamp(value, -0.25F, 0.25F)} on every motion axis. */
    public static double clampGolemOrbSpeed(double value) {
        if (value < -GOLEM_ORB_MAX_SPEED) return -GOLEM_ORB_MAX_SPEED;
        return value > GOLEM_ORB_MAX_SPEED ? GOLEM_ORB_MAX_SPEED : value;
    }

    // ------------------------------------------------------------------
    // Primal arrow - EntityPrimalArrow
    // ------------------------------------------------------------------

    /** Original base damage of the primal arrow before the type factor. */
    public static final double PRIMAL_ARROW_BASE_DAMAGE = 2.1D;
    /** Original in ground lifetime of the primal arrow in ticks. */
    public static final int PRIMAL_ARROW_IN_GROUND_LIMIT = 100;
    /** Original weakness duration of the order arrow. */
    public static final int PRIMAL_ARROW_ORDER_TICKS = 200;
    /** Original weakness amplifier of the order arrow. */
    public static final int PRIMAL_ARROW_ORDER_AMPLIFIER = 4;
    /** Original slowness duration of the second arrow type. */
    public static final int PRIMAL_ARROW_SLOWNESS_TICKS = 200;
    /** Original slowness amplifier of the second arrow type. */
    public static final int PRIMAL_ARROW_SLOWNESS_AMPLIFIER = 4;
    /** Original wither duration of the fifth arrow type. */
    public static final int PRIMAL_ARROW_WITHER_TICKS = 100;
    /** Original fire seconds of a burning arrow that is not of the second type. */
    public static final int PRIMAL_ARROW_FIRE_SECONDS = 5;
    /** Original extra fire seconds of the fire arrow. */
    public static final int PRIMAL_ARROW_FIRE_TYPE_SECONDS = 5;

    /** Arrow type 0: the magic air arrow that ignores armour. */
    public static final int PRIMAL_ARROW_TYPE_AIR = 0;
    /** Arrow type 1: the fire arrow. */
    public static final int PRIMAL_ARROW_TYPE_FIRE = 1;
    /** Arrow type 2: the arrow that slows its victim. */
    public static final int PRIMAL_ARROW_TYPE_SLOW = 2;
    /** Arrow type 3: the heavy arrow, one and a half times the base damage. */
    public static final int PRIMAL_ARROW_TYPE_HEAVY = 3;
    /** Arrow type 4: the order arrow that weakens its victim. */
    public static final int PRIMAL_ARROW_TYPE_ORDER = 4;
    /** Arrow type 5: the withering arrow. */
    public static final int PRIMAL_ARROW_TYPE_WITHER = 5;

    /**
     * Original {@code func_70242_d} damage of the arrow type: three halves for
     * the heavy arrow, four fifths for the order and wither arrows, the plain
     * base damage otherwise.
     */
    public static double primalArrowDamage(int type) {
        if (type == PRIMAL_ARROW_TYPE_HEAVY) return PRIMAL_ARROW_BASE_DAMAGE * 1.5D;
        if (type == PRIMAL_ARROW_TYPE_ORDER || type == PRIMAL_ARROW_TYPE_WITHER) return PRIMAL_ARROW_BASE_DAMAGE * 0.8D;
        return PRIMAL_ARROW_BASE_DAMAGE;
    }

    /** Original {@code MathHelper.ceil_double(speed * damage)} hit damage. */
    public static int primalArrowHitDamage(double speed, double damage) {
        return (int) Math.ceil(speed * damage);
    }

    /** Original critical bonus roll bound: {@code damage / 2 + 2}. */
    public static int primalArrowCritBound(int damage) {
        return damage / 2 + 2;
    }

    /** Original critical bonus: the roll is added to the already rolled damage. */
    public static int primalArrowCritDamage(int damage, int roll) {
        return damage + roll;
    }

    /** Original fire seconds of a burning arrow; the second type never burns. */
    public static int primalArrowFireSeconds(boolean burning, int type) {
        return burning && type != PRIMAL_ARROW_TYPE_SLOW ? PRIMAL_ARROW_FIRE_SECONDS : 0;
    }

    /** Original extra fire seconds of the fire arrow. */
    public static int primalArrowFireTypeSeconds(int type) {
        return type == PRIMAL_ARROW_TYPE_FIRE ? PRIMAL_ARROW_FIRE_TYPE_SECONDS : 0;
    }

    /** Original launch speed: the bow charge times the arrow damage times one and a half. */
    public static float primalArrowLaunchSpeed(float velocity) {
        return velocity * 1.5F;
    }

    /** Original bow event damage: the draw strength times the arrow damage. */
    public static float primalArrowChargeDamage(float charge, float damage) {
        return charge * damage;
    }

    /** The original slowness effect belongs to the second arrow type only. */
    public static boolean primalArrowAppliesSlowness(int type) {
        return type == PRIMAL_ARROW_TYPE_SLOW;
    }

    /** The original weakness effect belongs to the order arrow only. */
    public static boolean primalArrowAppliesOrder(int type) {
        return type == PRIMAL_ARROW_TYPE_ORDER;
    }

    /** The original wither effect belongs to the fifth arrow type only. */
    public static boolean primalArrowAppliesWither(int type) {
        return type == PRIMAL_ARROW_TYPE_WITHER;
    }

    /** The original magic damage sources ignore armour. */
    public static boolean primalArrowIgnoresArmour(int type) {
        return type == PRIMAL_ARROW_TYPE_AIR || type == PRIMAL_ARROW_TYPE_ORDER;
    }

    /** The original fire arrow uses a fire damage source. */
    public static boolean primalArrowIsFireDamage(int type) {
        return type == PRIMAL_ARROW_TYPE_FIRE;
    }

    // ------------------------------------------------------------------
    // Following item - EntityFollowingItem
    // ------------------------------------------------------------------

    /** Original follow divisor: the item accelerates while this counter falls. */
    public static final int FOLLOW_ITEM_SLOWDOWN = 20;
    /** Original gravity of an item that has no target. */
    public static final double FOLLOW_ITEM_GRAVITY = 0.04D;
    /** Original distance under which the item stops following. */
    public static final double FOLLOW_ITEM_STOP_DISTANCE = 0.5D;

    /** Original decrement of the follow divisor; it never falls below one. */
    public static int followItemSlowdown(int current) {
        return current > 1 ? current - 1 : current;
    }

    /**
     * Original follow speed: the motion is the delta divided by the distance
     * and the follow divisor, so the item always moves one block per divisor
     * ticks towards its target.
     */
    public static double followItemSpeed(int slowdown) {
        return 1D / slowdown;
    }

    /** The original stops following inside half a block. */
    public static boolean followItemArrived(double distance) {
        return distance <= FOLLOW_ITEM_STOP_DISTANCE;
    }

    /** Original target height: the feet of the target plus half of its height. */
    public static double followItemTargetY(double targetY, double targetHeight) {
        return targetY + targetHeight / 2D;
    }

    // ------------------------------------------------------------------
    // Permanent item - EntityPermanentItem
    // ------------------------------------------------------------------

    /** Original vanilla item lifetime in ticks. */
    public static final int ITEM_LIFETIME = 6000;
    /** The original keeps the item alive by restarting its age this many ticks early. */
    public static final int ITEM_LIFETIME_MARGIN = 5;

    /** The original never lets the item entity die of old age. */
    public static boolean itemNeverDespawns(int age) {
        return age + ITEM_LIFETIME_MARGIN >= ITEM_LIFETIME;
    }

    // ------------------------------------------------------------------
    // Inventories - InventoryMob and InventoryPech
    // ------------------------------------------------------------------

    /** Original default stack limit of a mob inventory. */
    public static final int INVENTORY_MOB_STACK_LIMIT = 64;
    /** Original number of merchant slots of the Pech trade inventory. */
    public static final int INVENTORY_PECH_SLOTS = 5;
    /** Original distance under which a player may open a mob inventory. */
    public static final double INVENTORY_MOB_REACH = 64D;

    /**
     * The original reports one slot more than it owns, so the last slot of a
     * mob inventory is never reachable through the container.
     */
    public static int inventoryMobContainerSize(int slots) {
        return slots + 1;
    }

    /**
     * Original {@code storeItemStack}: a slot may receive the offered stack
     * when it holds the same item, is stackable, is not full, stays below the
     * inventory limit and carries the same metadata.
     */
    public static boolean inventoryMobAccepts(boolean sameItem, boolean stackable, boolean atStackLimit,
                                              boolean atInventoryLimit, boolean differentMetadata) {
        return sameItem && stackable && !atStackLimit && !atInventoryLimit && !differentMetadata;
    }

    /** Original {@code getFirstEmptyStack}: the first slot without a stack. */
    public static int inventoryMobFirstEmpty(boolean[] occupied) {
        for (int slot = 0; slot < occupied.length; slot++) if (!occupied[slot]) return slot;
        return -1;
    }

    /**
     * Original {@code storePartialItemStack}: the amount that actually fits is
     * limited by the stack size of the item and by the inventory limit.
     */
    public static int inventoryMobStoreAmount(int offered, int stackRoom, int limitRoom) {
        int amount = offered;
        if (amount > stackRoom) amount = stackRoom;
        if (amount > limitRoom) amount = limitRoom;
        return amount;
    }

    /** The original Pech trade only accepts the offered item in the first slot. */
    public static boolean inventoryPechAccepts(int slot) {
        return slot == 0;
    }

    /** The original Pech trade stack limit. */
    public static int inventoryPechStackLimit() {
        return INVENTORY_MOB_STACK_LIMIT;
    }
}
