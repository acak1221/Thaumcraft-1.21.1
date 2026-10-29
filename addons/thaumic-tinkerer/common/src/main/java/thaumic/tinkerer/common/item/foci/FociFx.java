/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the sounds and particles of the wand foci, which
 * 1.7.10 spawned through Thaumcraft's client proxy. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0,
 * see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.foci;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftSoundIds;

/**
 * Sounds and particles of the wand foci.
 *
 * <p>1.7.10 played {@code thaumcraft:wand}, {@code thaumcraft:wind}, {@code fire.fire}... with
 * {@code World.playSoundAtEntity} on both sides and spawned Thaumcraft's client particles ({@code sparkle},
 * {@code smokeSpiral}, {@code wispFX2}, {@code beamCont}) from the focus code of the client. The sounds are
 * {@code Level.playSound(player, ...)} on both sides here (the casting player hears the client's copy, the others the
 * server's, so nobody hears it twice). Thaumcraft's particles have no public facade in the 1.21.1 API
 * ({@code docs/api-requests/Foci.md}); until they do, the effects are vanilla particles added on the client with
 * {@link Level#addParticle}, which does nothing on a server.</p>
 */
public final class FociFx {
    /** The sparkle of the Distortion and Telekinesis foci: a white star ({@code sparkle} of 1.7.10). */
    public static final ParticleOptions SPARKLE = ParticleTypes.END_ROD;

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

    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(Vec3.fromRGB24(rgb & 0xFFFFFF).toVector3f(), scale);
    }

    /** {@code sparkle(x, y, z, type)}: one sparkle at a point, tinted. */
    public static void sparkle(Level level, double x, double y, double z, int rgb) {
        if (!level.isClientSide) return;
        RandomSource random = level.random;
        level.addParticle(dust(rgb, 0.9F), x, y, z, (random.nextDouble() - 0.5) * 0.02, 0.02, (random.nextDouble() - 0.5) * 0.02);
    }

    /** The white sparkle of the Distortion shield on a deflected projectile. */
    public static void whiteSparkle(Level level, Entity entity) {
        if (!level.isClientSide) return;
        level.addParticle(SPARKLE, entity.getX(), entity.getY(), entity.getZ(), 0.0, 0.0, 0.0);
    }

    /**
     * {@code smokeSpiral(world, x, y - motionY, z, 2F, angle, (int) y, 0x9E2FF)} of the Focus of Uprising: smoke that
     * circles the player at a radius of two blocks. Five of them per cast, as in 1.7.10.
     */
    public static void smokeSpiral(Level level, Player player, int rgb) {
        if (!level.isClientSide) return;
        RandomSource random = level.random;
        ParticleOptions smoke = dust(rgb, 1.2F);
        for (int i = 0; i < 5; i++) {
            double angle = random.nextInt(360) * Mth.DEG_TO_RAD;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            level.addParticle(smoke, player.getX() + cos * 2.0, player.getY() - player.getDeltaMovement().y,
                    player.getZ() + sin * 2.0, -sin * 0.12, 0.06, cos * 0.12);
        }
    }

    /** {@code wispFX2(..., type 4, true, true, -random / 10)}: a flame that rises, inside the block. */
    public static void flameWisp(Level level, double x, double y, double z) {
        if (!level.isClientSide) return;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, level.random.nextDouble() / 10.0, 0.0);
    }

    /**
     * {@code beamCont(world, player, x, y, z, 2, 0xFF0000, true, 0F, null, 1)}: the continuous beam from the wand to
     * the block, as dust along the line (a few motes per tick).
     */
    public static void beam(Level level, Player player, Vec3 target, int rgb) {
        if (!level.isClientSide) return;
        Vec3 look = player.getViewVector(1.0F);
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-6 ? Vec3.ZERO : right.normalize();
        Vec3 from = player.getEyePosition().add(look.scale(0.5)).add(right.scale(0.25)).add(0.0, -0.25, 0.0);
        Vec3 line = target.subtract(from);
        double length = line.length();
        if (length < 0.1) return;
        ParticleOptions mote = dust(rgb, 0.7F);
        RandomSource random = level.random;
        int motes = Math.max(2, (int) (length * 1.5));
        for (int i = 0; i < motes; i++) {
            Vec3 at = from.add(line.scale(random.nextDouble()));
            level.addParticle(mote, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    /** The pink and purple motes of {@code sparkle(..., 0)} around a healing player or a pulled item. */
    public static void sparkleAround(Level level, Entity entity, int rgb) {
        if (!level.isClientSide) return;
        RandomSource random = level.random;
        sparkle(level, entity.getX() + random.nextFloat() - 0.5F, entity.getY() + random.nextFloat(),
                entity.getZ() + random.nextFloat() - 0.5F, rgb);
    }
}
