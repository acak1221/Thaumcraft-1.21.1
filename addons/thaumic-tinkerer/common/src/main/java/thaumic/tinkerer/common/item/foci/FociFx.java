/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the sounds and particles of the wand foci, which
 * 1.7.10 spawned through Thaumcraft's client proxy. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0,
 * see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.client.ThaumcraftClientFX;

/**
 * Sounds and particles of the wand foci.
 *
 * <p>1.7.10 played {@code thaumcraft:wand}, {@code thaumcraft:wind}, {@code fire.fire}... with
 * {@code World.playSoundAtEntity} on both sides and spawned Thaumcraft's client particles ({@code sparkle},
 * {@code smokeSpiral}, {@code wispFX2}, {@code beamCont}, {@code burst}) from the focus code of the client, that is, only the
 * casting player saw them. The sounds are {@code Level.playSound(player, ...)} on both sides here (the casting player hears
 * the client's copy, the others the server's, so nobody hears it twice). The particles are the twins of those proxy calls in
 * the API, {@link ThaumcraftClientFX} (drawn on a client level, silently ignored on a server level), with the arguments the
 * original passed; the one effect that the server decides on, the burst of the Focus of Dislocation, is sent to the players
 * around with {@link ThaumcraftFX#burst}.</p>
 */
public final class FociFx {
    /** The index of the violet sparkle ({@code sparkle(x, y, z, 0)} of the Mending and Telekinesis foci). */
    public static final int SPARKLE_VIOLET = 0;
    /** The index of the white sparkle ({@code sparkle(x, y, z, 6)} of the Distortion shield). */
    public static final int SPARKLE_WHITE = 6;

    /** The type of wisp that {@code wispFX2} of Efreet's Flame asked for. */
    private static final int WISP_FLAME = 4;
    /** The texture and the end of the beam of Efreet's Flame ({@code beamCont(..., 2, 0xFF0000, true, 0F, null, 1)}). */
    private static final int BEAM_TEXTURE = 2;
    private static final int BEAM_IMPACT = 1;
    /** The smoke spiral of the Focus of Uprising: five puffs of the radius two. */
    private static final int SPIRAL_PUFFS = 5;
    private static final float SPIRAL_RADIUS = 2F;
    /** The bursts of the Focus of Dislocation (1.7.10 looped eight times with the size 0.2). */
    private static final int BURSTS = 8;
    private static final float BURST_SIZE = 0.2F;
    /** The height at which the bursts of a placed block rise above the block's base (1.7.10 added 0.65F). */
    public static final float PLACE_BURST_LIFT = 0.65F;

    private FociFx() {}

    // ---- sounds ----------------------------------------------------------------------------------------------------

    /** Plays one of Thaumcraft's sounds ({@link ThaumcraftSoundIds}) at the player; a missing sound plays nothing. */
    public static void thaumcraftSound(Level level, Player player, ResourceLocation id, float volume, float pitch) {
        SoundEvent event;
        try {
            event = ThaumcraftSoundIds.event(id);
        } catch (IllegalArgumentException missing) {
            return;
        }
        sound(level, player, event, volume, pitch);
    }

    /** {@code world.playSoundAtEntity(player, name, volume, pitch)}: on both sides, the player's own copy is the local one. */
    public static void sound(Level level, Player player, SoundEvent event, float volume, float pitch) {
        level.playSound(player, player, event, SoundSource.PLAYERS, volume, pitch);
    }

    // ---- particles (client side) -----------------------------------------------------------------------------------

    /** {@code sparkle(x, y, z, type)}: one sparkle at a point (the colour is the index of the API, 0 to 7). */
    public static void sparkle(Level level, double x, double y, double z, int type) {
        if (!level.isClientSide) return;
        ThaumcraftClientFX.sparkle(level, x, y, z, type);
    }

    /** The white sparkle of the Distortion shield on a deflected projectile ({@code sparkle(e.posX, e.posY, e.posZ, 6)}). */
    public static void whiteSparkle(Level level, Entity entity) {
        sparkle(level, entity.getX(), entity.getY(), entity.getZ(), SPARKLE_WHITE);
    }

    /**
     * The violet sparkle of the Mending focus around the player: a random point of a box one block wide and one high.
     * The height is counted from the eyes, as the {@code posY} of the local player of 1.7.10 was (its feet plus 1.62).
     */
    public static void sparkleAround(Level level, Player player, int type) {
        if (!level.isClientSide) return;
        sparkle(level, player.getX() + level.random.nextFloat() - 0.5F, player.getEyeY() + level.random.nextFloat(),
                player.getZ() + level.random.nextFloat() - 0.5F, type);
    }

    /**
     * {@code smokeSpiral(world, x, y - motionY, z, 2F, (int) (random * 360), (int) y, 0x9E2FF)} of the Focus of Uprising,
     * five times: smoke that circles the player at a radius of two blocks and sinks. The height is the eye height, as the
     * {@code posY} of the local player of 1.7.10 was.
     */
    public static void smokeSpiral(Level level, Player player, int rgb) {
        if (!level.isClientSide) return;
        double y = player.getEyeY();
        for (int i = 0; i < SPIRAL_PUFFS; i++) {
            ThaumcraftClientFX.smokeSpiral(level, player.getX(), y - player.getDeltaMovement().y, player.getZ(), SPIRAL_RADIUS,
                    (int) (level.random.nextDouble() * 360), (int) y, rgb);
        }
    }

    /** {@code wispFX2(world, x, y, z, random / 2F, 4, true, true, -random / 10F)}: a flame wisp that rises, through blocks. */
    public static void flameWisp(Level level, double x, double y, double z) {
        if (!level.isClientSide) return;
        // The size must be above zero; Math.random() can in theory be zero.
        float size = Math.max(1.0E-3F, (float) Math.random() / 2F);
        ThaumcraftClientFX.wisp(level, x, y, z, size, WISP_FLAME, true, true, (float) -Math.random() / 10F);
    }

    /**
     * {@code beamCont(world, player, x, y, z, 2, 0xFF0000, true, 0F, null, 1)}: the continuous beam from the wand to the
     * point. Called on every client tick while the focus works; the beam lets go on its own when the calls stop.
     */
    public static void beam(Level level, Player player, Vec3 target, int rgb) {
        if (!level.isClientSide) return;
        ThaumcraftClientFX.beam(level, player, target, rgb, BEAM_TEXTURE, true, 0F, BEAM_IMPACT);
    }

    /**
     * The eight bursts of the Focus of Dislocation over a block, sent by the server to the players around
     * ({@code burst(world, pos.x + random, pos.y + random + lift, pos.z + random, 0.2F)}). 1.7.10 drew them on the client
     * of the caster after the pickup (lift 0) and the placing (lift 0.65).
     */
    public static void dislocationBursts(ServerLevel level, BlockPos pos, float lift) {
        for (int i = 0; i < BURSTS; i++) {
            ThaumcraftFX.burst(level, (float) (pos.getX() + Math.random()), (float) (pos.getY() + Math.random()) + lift,
                    (float) (pos.getZ() + Math.random()), BURST_SIZE);
        }
    }
}
