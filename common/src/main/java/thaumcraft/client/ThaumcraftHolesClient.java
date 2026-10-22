package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.client.fx.particles.TypeSparkleParticle;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.lib.world.HoleSparkleRules;
import thaumcraft.common.tiles.TileHole;
import thaumcraft.common.lib.fx.SparkleCallRules;

/**
 * The sparkles around the portable hole of the original.
 *
 * <p>Original {@code thaumcraft/common/tiles/TileHole.java:127-211} ({@code surroundwithsparkles})
 * and {@code thaumcraft/client/ClientProxy.java:658-664} (the sparkle of the proxy): the hole
 * block is surrounded by the sparkles of the type two on every edge where it touches a solid
 * block, and every sparkle is drawn with the one in six roll of the particle count of the
 * client.</p>
 */
public final class ThaumcraftHolesClient {
    private ThaumcraftHolesClient() {}

    public static void init() {
        TileHole.setClientEffects(ThaumcraftHolesClient::sparkle);
        SparkleCallRules.setClientEffects((level, position, size, type, gravity) -> {
            if (level instanceof ClientLevel client
                    && client.random.nextInt(SparkleCallRules.SPARKLE_ROLL) < TypeSparkleParticle.particleCount()) {
                Minecraft.getInstance().particleEngine.add(new TypeSparkleParticle(client,
                        position[0], position[1], position[2], size, type,
                        SparkleCallRules.SPARKLE_MULTIPLIER, gravity));
            }
        });
    }

    private static void sparkle(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel client)) return;
        HoleSparkleRules.Neighbours neighbours = new HoleSparkleRules.Neighbours(
                opaque(client, pos.above()), opaque(client, pos.east()), opaque(client, pos.south()),
                opaque(client, pos.below()), opaque(client, pos.west()), opaque(client, pos.north()),
                isHole(client, pos.above()), isHole(client, pos.below()), isHole(client, pos.south()),
                isHole(client, pos.north()), isHole(client, pos.west()), isHole(client, pos.east()));
        int count = TypeSparkleParticle.particleCount();
        for (HoleSparkleRules.Sparkle sparkle : HoleSparkleRules.sparkles(neighbours)) {
            double x = pos.getX() + sparkle.dx();
            double y = pos.getY() + sparkle.dy();
            double z = pos.getZ() + sparkle.dz();
            // The random fraction of the original is on one axis of the edge.
            switch (sparkle.random()) {
                case X -> x += client.getRandom().nextFloat();
                case Y -> y += client.getRandom().nextFloat();
                case Z -> z += client.getRandom().nextFloat();
                default -> { }
            }
            // The proxy of the original draws the particle when rand.nextInt(6) < the count.
            if (client.getRandom().nextInt(6) < count) {
                Minecraft.getInstance().particleEngine.add(new TypeSparkleParticle(client, x, y, z, 1.5F, 2, 6));
            }
        }
    }

    /**
     * The 1.7.10 {@code Block.func_149662_c} of the original, the MCP name {@code isOpaqueCube}: a
     * full cube that hides the face behind it. The 1.21.1 counterpart is a full collision shape
     * ({@code isSolidRender}) that occludes ({@code canOcclude}); the glass and the leaves of
     * 1.21.1 carry {@code noOcclusion} and the glass of the 1.7.10 was not an opaque cube.
     */
    private static boolean opaque(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos) && state.canOcclude();
    }

    /** The hole block of the original, the filter of the b1 to b6 flags. */
    private static boolean isHole(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ThaumcraftHoles.HOLE_BLOCK.get());
    }
}
