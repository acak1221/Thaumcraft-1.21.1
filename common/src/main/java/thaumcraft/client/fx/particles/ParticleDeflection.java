package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original push-out-of-blocks routine of the TC4 1.7.10 client particles.
 *
 * <p>The original implements it three times, with the same body: the oracle
 * {@code thaumcraft/client/fx/particles/FXWisp.java:231-283}, {@code FXWispEG.java:87-139} and
 * {@code FXEssentiaTrail.java:145-197}. A mote that ends up inside a solid block is pushed out through the
 * nearest open face with the small random speed {@code rand.nextFloat() * 0.05f + 0.025f} and the random
 * sideways jitter {@code (rand.nextFloat() - rand.nextFloat()) * 0.1f}.</p>
 *
 * <p>The original predicate is {@code !isAirBlock(pos) && isSolid(pos) && !isAirBlock(entity)}: the mote is
 * inside a block that is neither air nor passable, and the neighbouring blocks that are not solid are the
 * faces the mote can escape through.</p>
 */
public final class ParticleDeflection {
    private ParticleDeflection() {}

    /**
     * The original {@code pushOutOfBlocks(double, double, double)} of the oracle particles.
     *
     * @return true when the mote was inside a solid block and its motion was redirected
     */
    public static boolean pushOutOfBlocks(Particle particle, Level level, double x, double y, double z) {
        int blockX = Mth.floor(x);
        int blockY = Mth.floor(y);
        int blockZ = Mth.floor(z);
        double localX = x - blockX;
        double localY = y - blockY;
        double localZ = z - blockZ;
        if (isOpen(level, blockX, blockY, blockZ)) return false;
        boolean west = !isOpen(level, blockX - 1, blockY, blockZ);
        boolean east = !isOpen(level, blockX + 1, blockY, blockZ);
        boolean down = !isOpen(level, blockX, blockY - 1, blockZ);
        boolean up = !isOpen(level, blockX, blockY + 1, blockZ);
        boolean north = !isOpen(level, blockX, blockY, blockZ - 1);
        boolean south = !isOpen(level, blockX, blockY, blockZ + 1);
        int face = -1;
        double best = 9999.0D;
        if (west && localX < best) { best = localX; face = 0; }
        if (east && 1.0D - localX < best) { best = 1.0D - localX; face = 1; }
        if (down && localY < best) { best = localY; face = 2; }
        if (up && 1.0D - localY < best) { best = 1.0D - localY; face = 3; }
        if (north && localZ < best) { best = localZ; face = 4; }
        if (south && 1.0D - localZ < best) { face = 5; }
        float speed = particle.random.nextFloat() * 0.05F + 0.025F;
        float jitter = (particle.random.nextFloat() - particle.random.nextFloat()) * 0.1F;
        if (face == 0) {
            particle.xd = -speed; particle.yd = jitter; particle.zd = jitter;
        } else if (face == 1) {
            particle.xd = speed; particle.yd = jitter; particle.zd = jitter;
        } else if (face == 2) {
            particle.yd = -speed; particle.xd = jitter; particle.zd = jitter;
        } else if (face == 3) {
            particle.yd = speed; particle.xd = jitter; particle.zd = jitter;
        } else if (face == 4) {
            particle.zd = -speed; particle.xd = jitter; particle.yd = jitter;
        } else if (face == 5) {
            particle.zd = speed; particle.xd = jitter; particle.yd = jitter;
        }
        return true;
    }

    /** The original open block space: air or a block that does not block the movement. */
    private static boolean isOpen(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isLoaded(pos)) return true;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.blocksMotion();
    }
}
