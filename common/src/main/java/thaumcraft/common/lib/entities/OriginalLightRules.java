package thaumcraft.common.lib.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The 1.7.10 light level gates of the original mobs.
 *
 * <p>Sources: the original {@code net/minecraft/entity/monster/EntityMob.java} (the
 * {@code isValidLightLevel} of every hostile mob that does not override it) and the original
 * {@code thaumcraft/common/entities/monster/EntityWisp.java} (the wisp overrides it with the
 * darkened sky light).</p>
 *
 * <p>The original mob rule reads the SAVED sky light of the cell and the BLOCK light of the
 * cell:
 * <pre>
 * if (world.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) &gt; rand.nextInt(32)) return false;
 * int l = world.getBlockLightValue(x, y, z);
 * if (world.isThundering()) { int keep = world.skylightSubtracted; world.skylightSubtracted = 10;
 *     l = world.getBlockLightValue(x, y, z); world.skylightSubtracted = keep; }
 * return l &lt;= rand.nextInt(8);
 * </pre>
 * The thundering branch only moves the sky subtraction, so the block light it re-reads is the
 * same value: it is a vanilla no-op and is kept here as a comment.</p>
 *
 * <p>The port used the maximum raw brightness of the cell instead of the block light. The
 * maximum raw brightness is {@code max(blockLight, skyLight - skyDarken)}, so in the open during
 * the day it is the full sky light of fifteen and the original mobs could never spawn there,
 * while the original spawns them in every dark place of the day as well.</p>
 */
public final class OriginalLightRules {
    private OriginalLightRules() {}

    /** The original sky light roll of the mob rule: {@code rand.nextInt(32)}. */
    public static final int SKY_LIGHT_ROLL_BOUND = 32;
    /** The original block light roll of the mob rule: {@code rand.nextInt(8)}. */
    public static final int BLOCK_LIGHT_ROLL_BOUND = 8;
    /**
     * The original class gate of the fire bat and the taint swarm: the BLOCK light of the cell
     * against a {@code nextInt(7)} roll.
     *
     * <p>Oracle {@code EntityFireBat.getCanSpawnHere:333-340} and
     * {@code EntityTaintSwarm.getCanSpawnHere:226-233} read
     * {@code world.getBlockLightValue(x, minY, z)} and compare it with
     * {@code rand.nextInt(7)}. Both classes override {@code isValidLightLevel} with a plain
     * {@code true}, so the generic mob darkness gate does not apply to them, but their own class
     * gate does.</p>
     */
    public static final int CLASS_LIGHT_ROLL_BOUND = 7;

    /**
     * The original class gate: the block light of the cell against the {@code nextInt(7)} roll.
     *
     * <p>The port used the maximum raw brightness of the cell instead of the block light. The
     * maximum raw brightness is {@code max(blockLight, skyLight - skyDarken)}, so under the open
     * sky during the day it is the full sky light of fifteen and the gate refused every cell,
     * while the original reads the block light, which is zero in the open air and therefore always
     * passes the roll.</p>
     */
    public static boolean validClassLightLevel(int blockLight, int roll) {
        return blockLight <= roll;
    }

    /**
     * The original {@code EntityMob.isValidLightLevel}: the saved sky light of the cell against
     * a {@code nextInt(32)} roll, then the block light of the cell against a {@code nextInt(8)}
     * roll. The two rolls keep the original order, so a cell that fails the sky roll never
     * draws the block roll.
     */
    public static boolean validMobLightLevel(LevelReader level, BlockPos pos, RandomSource random) {
        int skyRoll = random.nextInt(SKY_LIGHT_ROLL_BOUND);
        if (level.getBrightness(LightLayer.SKY, pos) > skyRoll) return false;
        int blockRoll = random.nextInt(BLOCK_LIGHT_ROLL_BOUND);
        return level.getBrightness(LightLayer.BLOCK, pos) <= blockRoll;
    }

    /**
     * The original {@code EntityWisp.isValidLightLevel}: the wisp reads the DARKENED sky light
     * ({@code getLightValue} multiplies the saved sky light with {@code 1 - skyDarken}) against
     * a {@code nextInt(32)} roll, then the block light against a {@code nextInt(8)} roll.
     */
    public static boolean validWispLightLevel(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        int skyRoll = random.nextInt(SKY_LIGHT_ROLL_BOUND);
        if (darkenedSkyLight(level, pos) > skyRoll) return false;
        int blockRoll = random.nextInt(BLOCK_LIGHT_ROLL_BOUND);
        return level.getBrightness(LightLayer.BLOCK, pos) <= blockRoll;
    }

    /**
     * The original mob rule with the two rolls already drawn: the saved sky light of the cell
     * against the {@code nextInt(32)} roll and the block light of the cell against the
     * {@code nextInt(8)} roll.
     */
    public static boolean validMobLightLevel(int savedSkyLight, int blockLight, int skyRoll, int blockRoll) {
        return savedSkyLight <= skyRoll && blockLight <= blockRoll;
    }

    /**
     * The original wisp rule with the two rolls already drawn: the darkened sky light of the
     * cell against the {@code nextInt(32)} roll and the block light against the
     * {@code nextInt(8)} roll.
     */
    public static boolean validWispLightLevel(int darkenedSkyLight, int blockLight, int skyRoll, int blockRoll) {
        return darkenedSkyLight <= skyRoll && blockLight <= blockRoll;
    }

    /**
     * The 1.21.1 reading of the original darkened sky light: the saved sky light of the cell
     * minus the sky darken of the dimension, never below zero. The 1.7.10 formula multiplied
     * the saved sky light with {@code 1 - skyDarken} where the darken was a factor between zero
     * and one; the 1.21.1 sky darken is the number of light levels the night takes off, so the
     * two agree on the day and on the full night.
     */
    public static int darkenedSkyLight(ServerLevelAccessor level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int darken = level.getLevel().getSkyDarken();
        return Math.max(0, sky - darken);
    }
}
