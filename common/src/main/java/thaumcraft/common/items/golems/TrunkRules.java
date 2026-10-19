package thaumcraft.common.items.golems;

/**
 * Pure rules of the Traveling Trunk (original EntityTravelingTrunk, InventoryTrunk, ContainerTravelingTrunk, ItemTrunkSpawner).
 * The trunk's upgrade is the id of a golem upgrade (0 Air, 1 Earth, 2 Fire, 3 Water, 4 Order, 5 Entropy), -1 for none, fitted once.
 */
public final class TrunkRules {
    public static final int NO_UPGRADE = -1, AIR = 0, EARTH = 1, FIRE = 2, WATER = 3, ORDER = 4, ENTROPY = 5;
    /** The original inventory array always holds 36 stacks; the rows decide how many of them are used. */
    public static final int MAX_SLOTS = 36;
    public static final float MAX_HEALTH = 75F, ATTACK_DAMAGE = 4F;
    /** Original getDistanceToEntity thresholds. */
    public static final float TELEPORT_DISTANCE = 20F, FOLLOW_DISTANCE = 5F;
    public static final int ANGER_TICKS = 600;
    /** Item pull: radius 3 for the pull, 0.5 to be swallowed, strength 0.075 per tick. */
    public static final double PULL_RADIUS = 3.0, SWALLOW_RADIUS = 0.5, PULL_STRENGTH = 0.075;

    private TrunkRules() {}

    public static boolean validUpgrade(int upgrade) { return upgrade >= NO_UPGRADE && upgrade <= ENTROPY; }

    /** setInvSize: four rows with the Earth upgrade, three otherwise. */
    public static int rows(int upgrade) { return upgrade == EARTH ? 4 : 3; }

    public static int slots(int upgrade) { return rows(upgrade) * 9; }

    /** Only an empty trunk takes an upgrade (original func_70085_c: getUpgrade() == -1). */
    public static boolean canFit(int current, int offered) { return current == NO_UPGRADE && offered >= AIR && offered <= ENTROPY; }

    /** Water makes the trunk invulnerable (original attackEntityFrom). */
    public static boolean invulnerable(int upgrade) { return upgrade == WATER; }

    /** Original func_70071_h_: +1 every tick with Water, otherwise every 50th tick of its age. */
    public static boolean healsNow(int upgrade, int age) { return upgrade == WATER || age % 50 == 0; }

    /** A Water trunk belongs to its owner alone: nobody else opens it, and a bell does not select it for anyone else. */
    public static boolean mayOpen(int upgrade, boolean isOwner) { return upgrade != WATER || isOwner; }

    public static boolean fast(int upgrade) { return upgrade == AIR; }

    /** Horizontal push while airborne after a hop (original jumpMovementFactor). */
    public static float airAcceleration(int upgrade) { return fast(upgrade) ? 0.04F : 0.03F; }

    public static boolean pullsItems(int upgrade) { return upgrade == ENTROPY; }

    public static boolean defendsOwner(int upgrade) { return upgrade == FIRE; }

    /** Hop delay in ticks: {@code (nextInt(10) + 5) / 3}, integer division, so 1..4. */
    public static int hopDelay(int roll) { return (roll + 5) / 3; }

    /** Original attack cooldown: {@code 10 + nextInt(5)}. */
    public static int attackCooldown(int roll) { return 10 + roll; }
}
