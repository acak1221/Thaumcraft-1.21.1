package thaumcraft.client.fx.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/**
 * ClientProxy.blockSparkle/FXGeneric: green 0x40ff40, original112..120 atlas frames, delay/size/drag/fullbright.
 *
 * <p>The original growth lamp sent the very same block sparkle packet as the other five senders
 * (oracle {@code thaumcraft/common/tiles/TileArcaneLampGrowth.java:106}), so the native green
 * sparkle is only the fixed colour of the shared particle.</p>
 */
public final class GrowthLampParticle extends BlockSparkleParticle {
    /** The original colour of the growth lamp block sparkle. */
    public static final int GREEN = 0x40FF40;
    public GrowthLampParticle(ClientLevel level, BlockPos pos) { super(level, pos, GREEN); }
}
