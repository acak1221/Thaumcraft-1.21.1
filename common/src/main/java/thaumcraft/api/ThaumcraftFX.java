package thaumcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;

/**
 * The visual effects a 1.7.10 add-on sent to the players around a block with Thaumcraft's own packets.
 *
 * <p>Not a 1.7.10 class: an add-on of 1.7.10 built {@code thaumcraft.common.lib.network.fx.PacketFX*} itself and sent
 * it with {@code PacketHandler.INSTANCE.sendToAllAround(..., 32)}. Each method here is one of those packets, with the
 * original look, sent to the players within 32 blocks. Call them on the server thread of the level; on another thread
 * they throw {@link IllegalStateException}. Colours are 0xRRGGBB.</p>
 */
public final class ThaumcraftFX {
    /** The original random grey of {@link #blockSparkle}. */
    public static final int RANDOM_GREY = thaumcraft.common.lib.network.BlockSparkleFxPayload.RANDOM_GREY;

    private ThaumcraftFX() {}

    /** Original {@code PacketFXBlockSparkle}: a burst of sparkles over a block; {@link #RANDOM_GREY} for the grey of the original. */
    public static void blockSparkle(ServerLevel level, BlockPos pos, int rgb) {
        thaumcraft.common.lib.network.BlockSparkleFxNetworking.sendBlockSparkle(level, pos, rgb);
    }

    /** Original {@code PacketFXBlockArc}: an arc of lightning from an entity to a block. */
    public static void blockArc(ServerLevel level, Entity source, BlockPos target) {
        thaumcraft.common.lib.network.BlockFxNetworking.sendArc(level, source, target);
    }

    /** Original {@code PacketFXBlockZap}: the bolt of a node between two points. */
    public static void zap(ServerLevel level, Vec3 from, Vec3 to) {
        thaumcraft.common.lib.network.BlockFxNetworking.sendZap(level, from, to);
    }

    /** Original {@code PacketFXBlockBubble}: the bubbles of a crucible around a block. */
    public static void bubbles(ServerLevel level, BlockPos pos, int rgb) {
        thaumcraft.common.lib.network.BlockFxNetworking.sendBubble(level, pos, rgb);
    }

    /** Original {@code PacketFXEssentiaSource}: essentia flowing from {@code source} to the block that drew it. */
    public static void essentiaTrail(ServerLevel level, BlockPos consumer, BlockPos source, Aspect aspect) {
        if (aspect == null) return;
        thaumcraft.common.lib.network.EssentiaFlowFxNetworking.sendEssentiaFlow(level, consumer, source, aspect.getColor());
    }

    /** Original {@code PacketFXVisDrain}: vis drawn from a node to the block that consumed it. */
    public static void visDrain(ServerLevel level, BlockPos consumer, BlockPos node, Aspect aspect) {
        if (consumer == null || node == null || aspect == null) return;
        thaumcraft.api.visnet.VisNetHandler.generateVisEffect(level, consumer, node, aspect);
    }
}
