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
 * it with {@code PacketHandler.INSTANCE.sendToAllAround(..., 32)}. {@link #blockSparkle}, {@link #blockArc}, {@link #zap},
 * {@link #bubbles}, {@link #essentiaTrail} and {@link #visDrain} are those packets, with the original look. {@link #sparkle},
 * {@link #burst}, {@link #blockRunes} and {@link #smokeSpiral} are not: the 1.7.10 proxy drew them on the client only, so
 * they are new senders, the server twins of the matching methods of {@link thaumcraft.api.client.ThaumcraftClientFX}, with no
 * original packet. They reach the players within 32 blocks of the point, except {@link #visDrain}, which reaches the players
 * within 64 blocks of the consumer (and is thinned by the original's half second throttle per consumer). Call them on the
 * server thread of the level; on another thread they throw {@link IllegalStateException}. Colours are 0xRRGGBB.</p>
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

    /**
     * The server twin of {@link thaumcraft.api.client.ThaumcraftClientFX#sparkle(net.minecraft.world.level.Level, double, double,
     * double, float, int, float) ThaumcraftClientFX.sparkle}: every player within 32 blocks of the point is sent the sparkle and draws it with their own
     * particle setting (so the sparkle is drawn with the chance of that call, not always). Not a packet of the original: the
     * 1.7.10 proxy drew sparkles on the client only, and an add-on whose logic runs on the server had to send its own.
     *
     * @param type 0 to 7, see the client method
     * @throws IllegalArgumentException for the ranges of the client method
     * @throws IllegalStateException    when not called on the server thread of the level
     */
    public static void sparkle(ServerLevel level, double x, double y, double z, float size, int type, float gravity) {
        thaumcraft.common.lib.network.ClientFxNetworking.send(level, thaumcraft.common.lib.network.ClientFxPayload.sparkle(
                level.dimension().location(), x, y, z, size, type, gravity));
    }

    /**
     * The server twin of {@link thaumcraft.api.client.ThaumcraftClientFX#burst}: the white puff of the wisp, drawn by every
     * player within 32 blocks of the point.
     *
     * @throws IllegalArgumentException for the ranges of the client method
     * @throws IllegalStateException    when not called on the server thread of the level
     */
    public static void burst(ServerLevel level, double x, double y, double z, float size) {
        thaumcraft.common.lib.network.ClientFxNetworking.send(level, thaumcraft.common.lib.network.ClientFxPayload.burst(
                level.dimension().location(), x, y, z, size));
    }

    /**
     * The server twin of {@link thaumcraft.api.client.ThaumcraftClientFX#blockRunes}: one rune glyph, drawn by every player
     * within 32 blocks of the point. As in the client method, the position is the corner of a block and the rune appears half
     * a block above and beside it on every axis.
     *
     * @throws IllegalArgumentException for the ranges of the client method
     * @throws IllegalStateException    when not called on the server thread of the level
     */
    public static void blockRunes(ServerLevel level, double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
        thaumcraft.common.lib.network.ClientFxNetworking.send(level, thaumcraft.common.lib.network.ClientFxPayload.runes(
                level.dimension().location(), x, y, z, red, green, blue, duration, gravity));
    }

    /**
     * The server twin of {@link thaumcraft.api.client.ThaumcraftClientFX#smokeSpiral}: <b>one</b> puff of smoke that travels along
     * a spiral, drawn by every player within 32 blocks of the point. One call is one puff, not a ring; the originals make the
     * ring by calling it 5 to 33 times with random {@code start} angles, and so does a server caller that wants one.
     *
     * @throws IllegalArgumentException for the ranges of the client method
     * @throws IllegalStateException    when not called on the server thread of the level
     */
    public static void smokeSpiral(ServerLevel level, double x, double y, double z, float radius, int start, int minY, int rgb) {
        thaumcraft.common.lib.network.ClientFxNetworking.send(level, thaumcraft.common.lib.network.ClientFxPayload.smokeSpiral(
                level.dimension().location(), x, y, z, radius, start, minY, rgb));
    }

    /** Original {@code PacketFXVisDrain}: vis drawn from a node to the block that consumed it. */
    public static void visDrain(ServerLevel level, BlockPos consumer, BlockPos node, Aspect aspect) {
        if (consumer == null || node == null || aspect == null) return;
        thaumcraft.api.visnet.VisNetHandler.generateVisEffect(level, consumer, node, aspect);
    }
}
