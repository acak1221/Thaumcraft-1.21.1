package thaumcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.BlockSparkleClientEffects;
import thaumcraft.client.fx.particles.FXArc;
import thaumcraft.common.entities.monster.EntityEldritchGolem;
import thaumcraft.common.lib.network.BlockSparkleFxPayload;
import thaumcraft.common.tiles.TileJarNode;

/**
 * Client registration for the eldritch, cultist and taint animal renderers.
 * The entity types come from the eldritch, cultist and taint animal content classes;
 * every renderer here is the original TC4 client presentation of its mob.
 */
public final class ThaumcraftEldritchClient {
    private static boolean initialized;
    private ThaumcraftEldritchClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        // Original Thaumcraft.proxy.arcLightning: the golem's lightning arc and its target sparkle.
        EntityEldritchGolem.setClientEffects(FXArc::arc);
        // Original TileJarNode:204-219 - the client event nine of the node jar: the burst of
        // block sparkles with the random grey colour over the whole jar.
        TileJarNode.setClientEffects(ThaumcraftEldritchClient::nodeJarEvent);
    }

    /** The original burst of the node jar: four by three by three block sparkles. */
    private static void nodeJarEvent(Level level, BlockPos pos, int event, int data) {
        if (event != TileJarNode.ANIMATE_EVENT || !(level instanceof ClientLevel clientLevel)) return;
        for (int yy = -1; yy < TileJarNode.SPARKLE_Y - 1; ++yy) {
            for (int xx = -1; xx < TileJarNode.SPARKLE_X - 1; ++xx) {
                for (int zz = -1; zz < TileJarNode.SPARKLE_Z - 1; ++zz) {
                    BlockSparkleClientEffects.burst(clientLevel, pos.offset(xx, yy, zz),
                            BlockSparkleFxPayload.RANDOM_GREY, TileJarNode.SPARKLE_COUNT);
                }
            }
        }
    }
}
