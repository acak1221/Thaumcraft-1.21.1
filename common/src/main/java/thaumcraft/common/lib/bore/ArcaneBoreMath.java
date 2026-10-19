package thaumcraft.common.lib.bore;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Pure TC4 4.2.3.5 bore arithmetic, not a world interaction or resource permit.
 * Original authority: TileArcaneBore151-184,255-286,387-392,506-643 and
 * TCVec3135-165. No item/world lookup, callbacks, RNG, ticking or mutable state. */
public final class ArcaneBoreMath {
    public static final int BASE_RADIUS = 2;
    public static final int CANDIDATE_READS = 64;
    public static final int MAX_RAW_PERDITIO_REQUEST = 100;
    public static final float REFILL_THRESHOLD = 20.0F;
    private static final float QUARTER_TURN = 1.5707964F;

    private ArcaneBoreMath() {}

    public record Angles(int x, int z) {}

    public record Rotation(int x, int z, int velocityX, int velocityZ, int targetX, int targetZ) {
        public boolean aligned() { return x == targetX && z == targetZ; }
    }

    /** The four currently supported focus levels, not an item eligibility check. */
    public record FocusLevels(int potency, int frugal, int treasure, int enlarge) {}

    public record Stats(int fortune, int speed, int area) {
        public int radius() { return BASE_RADIUS + area; }
        public int width() { return 1 + radius() * 2; }
    }

    /** Only scan geometry is represented. Target/count/beam state belongs to the
     * future tile. lastPlane cannot retain a MutableBlockPos alias. */
    public record ScanCursor(int spiral, float radius, float radialIncrement, BlockPos lastPlane) {
        public ScanCursor {
            if (spiral < 0 || spiral >= 360 || !Float.isFinite(radius) || !Float.isFinite(radialIncrement))
                throw new IllegalArgumentException("Invalid bore scan geometry");
            lastPlane = Objects.requireNonNull(lastPlane, "lastPlane").immutable();
        }
        public static ScanCursor initial() { return new ScanCursor(0, 0.0F, 0.0F, BlockPos.ZERO); }
    }

    /** One mathematical sample, not one server tick. Repeat in the SAME target
     * selection until changedPlane, then examine that plane's column. */
    public record PlaneStep(ScanCursor cursor, boolean changedPlane) {}

    /** Tile579-612: logical targets do not depend on above/below mounting. */
    public static Angles targetAngles(Direction miningDirection) {
        return switch (Objects.requireNonNull(miningDirection, "miningDirection")) {
            case DOWN -> new Angles(0, 180);
            case UP -> new Angles(0, 0);
            case NORTH -> new Angles(270, 90);
            case SOUTH -> new Angles(90, 90);
            case WEST -> new Angles(0, 90);
            case EAST -> new Angles(180, 90);
        };
    }

    /** Tile614-617 snaps positions only on initial load/client initialization;
     * existing velocities are NOT reset by setOrientation. Initial item placement
     * assigns the direction directly, so must not be mistaken for a wand turn. */
    public static Rotation reorient(Rotation before, Direction miningDirection, boolean initial) {
        Angles target = targetAngles(miningDirection);
        return new Rotation(initial ? target.x() : before.x(), initial ? target.z() : before.z(),
                before.velocityX(), before.velocityZ(), target.x(), target.z());
    }

    /** Tile167-184: use the OLD velocity, then accelerate or damp after crossing.
     * No shortest-angle wrap, position clamp or tolerance-based alignment. */
    public static Rotation stepRotation(Rotation before) {
        Axis x = stepAxis(before.x(), before.velocityX(), before.targetX());
        Axis z = stepAxis(before.z(), before.velocityZ(), before.targetZ());
        return new Rotation(x.position(), z.position(), x.velocity(), z.velocity(), before.targetX(), before.targetZ());
    }

    private record Axis(int position, int velocity) {}
    private static Axis stepAxis(int position, int velocity, int target) {
        if (position < target) {
            position += velocity;
            velocity = position < target ? velocity + 1 : (int) ((float) velocity / 3.0F);
        } else if (position > target) {
            position += velocity;
            velocity = position > target ? velocity - 1 : (int) ((float) velocity / 3.0F);
        } else velocity = 0;
        return new Axis(position, velocity);
    }

    /** Valid installation faces only. Mining direction is deliberately absent. */
    public static BlockPos basePosition(BlockPos top, Direction attachment) {
        if (attachment != Direction.UP && attachment != Direction.DOWN)
            throw new IllegalArgumentException("Bore attachment must be UP or DOWN");
        return top.relative(attachment.getOpposite());
    }

    /** Tile263-282: Frugal is intentionally not a contributor. Focus rank and
     * item/enchantment validation must happen before these numeric inputs. */
    public static Stats stats(FocusLevels focus, int pickFortune, int efficiency) {
        return new Stats(Math.max(focus.treasure(), pickFortune), focus.potency() + efficiency, focus.enlarge());
    }

    /** Tile255-260. No tier, pick mining-speed or research requirement is added. */
    public static boolean itemsValid(boolean hasFocus, boolean hasPickaxe, boolean damageablePick, int damage, int maxDamage) {
        return hasFocus && hasPickaxe && damageablePick && damage + 1 < maxDamage;
    }

    /** Tile579-582,618-627: preserve lastY/spiral/radius. This resets ONLY the
     * represented scan fields; it is not the tile's whole orientation reset. */
    public static ScanCursor resetScanForOrientation(ScanCursor before) {
        return new ScanCursor(before.spiral(), before.radius(), 0.0F, new BlockPos(0, before.lastPlane().getY(), 0));
    }

    /** Tile507-534, one iteration of its while-loop. Area0..2 is the current
     * bounded Enlarge catalogue. There is no world scan or internal retry loop. */
    public static PlaneStep stepPlane(ScanCursor before, BlockPos top, Direction miningDirection, int area) {
        if (area < 0 || area > 2) throw new IllegalArgumentException("Unsupported bore Enlarge level");
        int limit = BASE_RADIUS + area;
        float increment = before.radialIncrement();
        if (increment == 0.0F) increment = (float) limit / 360.0F;
        int spiral = before.spiral() + 2;
        if (spiral >= 360) spiral -= 360;
        float radius = before.radius() + increment;
        if (radius > (float) limit || radius < (float) -limit) increment *= -1.0F;
        var advanced = new ScanCursor(spiral, radius, increment, before.lastPlane());
        BlockPos plane = projectPlane(top, miningDirection, advanced);
        return new PlaneStep(new ScanCursor(spiral, radius, increment, plane), !plane.equals(before.lastPlane()));
    }

    /** Original TCVec3 Z, then Y, then X signs/order match native Vec3.zRot/
     * yRot/xRot (float table trig, double vector products). Keep the two float
     * angle operations: spiral/180F * (float)PI is NOT spiral*DEG_TO_RAD.
     * Do not round a radius, idealize quarter-turn trig, or substitute a square. */
    public static BlockPos projectPlane(BlockPos top, Direction miningDirection, ScanCursor sample) {
        Vec3 offset = new Vec3(0.0, sample.radius(), 0.0)
                .zRot((float) sample.spiral() / 180.0F * (float) Math.PI)
                .yRot(QUARTER_TURN * (float) miningDirection.getStepX())
                .xRot(QUARTER_TURN * (float) miningDirection.getStepY());
        Vec3 source = new Vec3((double) (top.getX() + miningDirection.getStepX()) + 0.5,
                (double) (top.getY() + miningDirection.getStepY()) + 0.5,
                (double) (top.getZ() + miningDirection.getStepZ()) + 0.5);
        return BlockPos.containing(source.add(offset));
    }

    /** Tile535-540 advances once before the loop AND before its first read.
     * The plane is already at top+direction: indexes0..63 mean top+3..66.
     * The future caller stops on its original world predicates; this is not a
     * list of targets, a chunk load request or permission to mine any candidate. */
    public static BlockPos candidate(BlockPos plane, Direction miningDirection, int index) {
        if (index < 0 || index >= CANDIDATE_READS) throw new IllegalArgumentException("Bore candidate index outside0..63");
        return plane.relative(miningDirection, index + 2);
    }

    /** Tile555-568's ray can hit nearer than the candidate, even the first two
     * forward cells. End at Vec3.atCenterOf(candidate). Real ray/protection and
     * its hardness>-1/AABB retarget check belong to the world boundary. */
    public static Vec3 occlusionRayStart(BlockPos top, Direction miningDirection) {
        return Vec3.atCenterOf(top).add(miningDirection.getStepX(), miningDirection.getStepY(), miningDirection.getStepZ());
    }

    /** Tile550-563. Arithmetic only: the caller separately checks harvestability.
     * Zero/negative results and the int cast/multiplication are not clamped. */
    public static int delayForTarget(float hardness, int speed, float creditBeforeDigDebit) {
        int delay = Math.max(10 - speed, (int) (hardness * 2.0F) - speed * 2);
        if (creditBeforeDigDebit < 1.0F) delay *= 4;
        return delay;
    }

    /** Tile153-158: caller asks this BEFORE requesting raw vis, and AGAIN after
     * applying its actual grant before attempting one essentia. This top-runtime
     * prefill is independent of redstone, focus and pick validity. */
    public static boolean needsAcceleration(float credit) { return credit < REFILL_THRESHOLD; }

    /** Arithmetic for an already committed actual grant, NEVER a debit/permit.
     * Caller owns the preceding <20 gate, installed-identity and callback checks. */
    public static float addAcceptedRawPerditio(float credit, int actualRawGrant) {
        if (actualRawGrant < 0 || actualRawGrant > MAX_RAW_PERDITIO_REQUEST)
            throw new IllegalArgumentException("Invalid actual raw Perditio grant");
        return credit + (float) actualRawGrant / 5.0F;
    }

    /** Only after an actual one-unit consumption while still <20. Do not clamp
     * the result: partial raw vis followed by essentia can overshoot20. */
    public static float addConsumedEssentia(float credit) { return credit + 20.0F; }

    /** Tile389-392: FIRST select next target/delay using the old credit, THEN
     * call this. A completed pending dig decrements only when >0, including
     * fractional credit which becomes negative. This is not a success permit. */
    public static float creditAfterDigAndNextSelection(float credit, boolean dug) {
        return dug && credit > 0.0F ? credit - 1.0F : credit;
    }

    /** Tile636,643: Java short cast truncates fractions toward zero, including
     * negative fractional debt. No implicit exact-float persistence. */
    public static short persistedCredit(float credit) { return (short) credit; }
    public static float loadedCredit(short credit) { return credit; }
}
