/**
 * This class is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (no 1.7.10
 * counterpart); it is distributed under the same licence as the add-on (Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.client.ThaumcraftClientFX;

/**
 * The particles and the sound of the simple blocks, through the public facade of Thaumcraft.
 *
 * <p>1.7.10 called {@code Thaumcraft.proxy.sparkle / wispFX2 / blockRunes / burst}, which drew on the client only (the
 * proxy of a dedicated server did nothing). Here a call with a client level draws with
 * {@link ThaumcraftClientFX}; a call with a server level sends the effect to the players around with
 * {@link ThaumcraftFX} (the 1.7.10 code that ran on the server drew in the integrated client only, so a dedicated
 * server shows more than 1.7.10 did). The one effect with no server twin is the wisp: the Gaseous Tenebrae that is
 * placed on the server shows a vanilla smoke puff instead (request A8 in {@code docs/api-requests/SimpleBlocks.md}).</p>
 */
public final class SimpleBlockFx {
    /** The size of {@code Thaumcraft.proxy.sparkle(x, y, z, type)} (the facade documents it for the type-only form). */
    public static final float TYPE_ONLY_SPARKLE_SIZE = 1.5F;

    /** Sparkle type 1: yellow (the Gaseous Illuminae). */
    public static final int SPARKLE_YELLOW = 1;
    /** Sparkle type 2: blue (the dark blue {@code '?'} and {@code 255} of the 1.7.10 block sparkles are the nearest). */
    public static final int SPARKLE_BLUE = 2;
    /** Sparkle type 4: red (the Nitor Gas). */
    public static final int SPARKLE_RED = 4;
    /** Wisp type 5: the dark wisp, drawn like smoke (the Gaseous Tenebrae). */
    public static final int WISP_DARK = 5;

    private SimpleBlockFx() {}

    /** {@code sparkle(x, y, z, size, type, gravity)}: drawn here on a client level, sent to the players around on a server level. */
    public static void sparkle(Level level, double x, double y, double z, float size, int type, float gravity) {
        if (level instanceof ServerLevel server) {
            ThaumcraftFX.sparkle(server, x, y, z, size, type, gravity);
        } else {
            ThaumcraftClientFX.sparkle(level, x, y, z, size, type, gravity);
        }
    }

    /** {@code sparkle(x + 0.5, y + 0.5, z + 0.5, type)} of a block: the {@code placeParticle} of the Gaseous Illuminae. */
    public static void sparkleAt(Level level, BlockPos pos, int type) {
        sparkle(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, TYPE_ONLY_SPARKLE_SIZE, type, 0.0F);
    }

    /**
     * The wisp of the Gaseous Tenebrae ({@code wispFX2(x + 0.5, y + 0.5, z + 0.5, 0.125, 5, true, true, -0.02)}): on a client
     * level the Thaumcraft wisp; on a server level a vanilla smoke puff, since {@link ThaumcraftFX} has no wisp.
     */
    public static void shadowWispAt(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
        } else {
            ThaumcraftClientFX.wisp(level, x, y, z, 0.125F, WISP_DARK, true, true, -0.02F);
        }
    }

    /**
     * {@code blockSparkle(world, x, y, z, color, count)} on the client, as the Forcefield, the Travel Slab and the Ward Slab
     * called it: {@code count} (at the particle setting) sparkles of size 1.75 that fall slowly, at random places in the
     * block. The facade has no block sparkle for a client level, so it is the sparkle at random places.
     */
    public static void blockSparkleClient(Level level, BlockPos pos, int type, int count) {
        int n = ThaumcraftClientFX.particleCount(level, count);
        for (int i = 0; i < n; i++) {
            ThaumcraftClientFX.sparkle(level, pos.getX() + level.random.nextFloat(), pos.getY() + level.random.nextFloat(),
                    pos.getZ() + level.random.nextFloat(), 1.75F, type, 0.1F);
        }
    }

    /** {@code world.playSoundAtEntity(entity, "thaumcraft:wand", volume, pitch)} on the server; a missing sound plays nothing. */
    public static void wandSound(Level level, double x, double y, double z, float volume, float pitch) {
        SoundEvent wand;
        try {
            wand = ThaumcraftSoundIds.event(ThaumcraftSoundIds.WAND);
        } catch (IllegalArgumentException missing) {
            return;
        }
        level.playSound(null, x, y, z, wand, SoundSource.PLAYERS, volume, pitch);
    }
}
