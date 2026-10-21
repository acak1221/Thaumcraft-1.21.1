package thaumcraft.client.fx.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The original push-out-of-blocks routine of the TC4 1.7.10 client particles.
 *
 * <p>The original implements it three times, with the same body: the oracle
 * {@code thaumcraft/client/fx/particles/FXWisp.java:225-294}, {@code FXWispEG.java:110-139} and
 * {@code FXEssentiaTrail.java:145-197}. A mote that ends up inside a solid block is pushed out through the
 * nearest open face with the small random speed {@code rand.nextFloat() * 0.05f + 0.025f} and the random
 * sideways jitter {@code (rand.nextFloat() - rand.nextFloat()) * 0.1f}.</p>
 *
 * <p>The original predicate is {@code !isAirBlock(pos) && isBlockNormalCubeDefault(pos, true) &&
 * !isAnyLiquid(boundingBox)}: the mote is inside a block that is a normal cube, that is a full solid
 * opaque cube, and its own box does not touch a liquid. The neighbouring blocks that are <em>not</em> a
 * normal cube are the faces the mote can escape through.</p>
 *
 * <p>The rule itself is the pure function {@link #deflect}, so that it can be asserted without a level and
 * without constructing a block. The 1.21.1 port of a normal cube is
 * {@code state.isSolidRender(level, pos) && state.isCollisionShapeFullBlock(level, pos)}, the same
 * predicate the port already uses for the 1.7.10 {@code isBlockNormalCubeDefault} path in
 * {@code thaumcraft/common/lib/world/WardingCasts.java:62-66}.</p>
 */
public final class ParticleDeflection {
    private ParticleDeflection() {}

    /** The redirected motion of the mote. */
    public record Motion(double x, double y, double z) {}

    /**
     * The original {@code pushOutOfBlocks(double, double, double)} of the oracle particles.
     *
     * @param level the level of the mote
     * @param random the random of the mote, the original uses the own {@code rand} of the particle
     * @param box the box of the mote, the original tests it against the liquids
     * @param x the position of the mote
     * @param y the position of the mote
     * @param z the position of the mote
     * @return the new motion when the mote was inside a solid block, null when it was not
     */
    public static Motion pushOutOfBlocks(Level level, RandomSource random, AABB box, double x, double y, double z) {
        int blockX = Mth.floor(x);
        int blockY = Mth.floor(y);
        int blockZ = Mth.floor(z);
        // The original condition, in the original order: the block is a normal cube and the box of the
        // mote does not touch a liquid. Only then does the original draw its two random numbers.
        if (!isNormalCube(level, blockX, blockY, blockZ)) return null;
        if (isAnyLiquid(level, box)) return null;
        return deflect(random,
                isOpen(level, blockX - 1, blockY, blockZ),
                isOpen(level, blockX + 1, blockY, blockZ),
                isOpen(level, blockX, blockY - 1, blockZ),
                isOpen(level, blockX, blockY + 1, blockZ),
                isOpen(level, blockX, blockY, blockZ - 1),
                isOpen(level, blockX, blockY, blockZ + 1),
                x - blockX, y - blockY, z - blockZ);
    }

    /**
     * The pure rule of the original: the nearest open face of the block and the original motion.
     *
     * <p>The face order, the comparisons and the random draws are the ones of the oracle
     * {@code FXWisp.java:233-290}: the west, east, down, up, north and south faces are tested in that order,
     * each one with a strict {@code <} against the distance that is still the best one, and the two random
     * numbers are drawn before the face is applied.</p>
     *
     * @param random the random of the mote
     * @param westOpen the west neighbour is not a normal cube
     * @param eastOpen the east neighbour is not a normal cube
     * @param downOpen the down neighbour is not a normal cube
     * @param upOpen the up neighbour is not a normal cube
     * @param northOpen the north neighbour is not a normal cube
     * @param southOpen the south neighbour is not a normal cube
     * @param localX the position of the mote inside its block
     * @param localY the position of the mote inside its block
     * @param localZ the position of the mote inside its block
     * @return the new motion, null when every face is closed
     */
    public static Motion deflect(RandomSource random, boolean westOpen, boolean eastOpen, boolean downOpen,
                                 boolean upOpen, boolean northOpen, boolean southOpen,
                                 double localX, double localY, double localZ) {
        float speed = random.nextFloat() * 0.05F + 0.025F;
        float jitter = (random.nextFloat() - random.nextFloat()) * 0.1F;
        int face = -1;
        double best = 9999.0D;
        if (westOpen && localX < best) { best = localX; face = 0; }
        if (eastOpen && 1.0D - localX < best) { best = 1.0D - localX; face = 1; }
        if (downOpen && localY < best) { best = localY; face = 2; }
        if (upOpen && 1.0D - localY < best) { best = 1.0D - localY; face = 3; }
        if (northOpen && localZ < best) { best = localZ; face = 4; }
        if (southOpen && 1.0D - localZ < best) { face = 5; }
        if (face == 0) return new Motion(-speed, jitter, jitter);
        if (face == 1) return new Motion(speed, jitter, jitter);
        if (face == 2) return new Motion(jitter, -speed, jitter);
        if (face == 3) return new Motion(jitter, speed, jitter);
        if (face == 4) return new Motion(jitter, jitter, -speed);
        if (face == 5) return new Motion(jitter, jitter, speed);
        return null;
    }

    /** The original open neighbour: a block that is not a normal cube. */
    private static boolean isOpen(Level level, int x, int y, int z) {
        return !isNormalCube(level, x, y, z);
    }

    /** The 1.21.1 normal cube: a full solid opaque cube. */
    private static boolean isNormalCube(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos) && state.isCollisionShapeFullBlock(level, pos);
    }

    /** The original {@code isAnyLiquid(AABB)} of the world. */
    private static boolean isAnyLiquid(Level level, AABB box) {
        if (box == null) return false;
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!level.getFluidState(new BlockPos(x, y, z)).isEmpty()) return true;
                }
            }
        }
        return false;
    }
}
