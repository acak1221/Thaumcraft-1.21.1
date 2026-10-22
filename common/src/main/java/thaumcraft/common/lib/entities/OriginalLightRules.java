package thaumcraft.common.lib.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;

/** TC4/1.7 light gates. World.getBlockLightValue (func_72957_l) is NOT
 * the BLOCK channel: World delegates to Chunk.getBlockLightValue with
 * skylightSubtracted, which returns max(block, savedSky - subtraction).
 * EntityMob and EntityWisp first roll SAVED sky, then this combined light;
 * their thunder branch temporarily uses subtraction10. */
public final class OriginalLightRules {
    private OriginalLightRules() {}
    public static final int SKY_LIGHT_ROLL_BOUND = 32;
    public static final int BLOCK_LIGHT_ROLL_BOUND = 8;
    public static final int CLASS_LIGHT_ROLL_BOUND = 7;

    /** Firebat/swarm class gates have no separate thunder subtraction override. */
    public static boolean validClassLightLevel(int combinedLight, int roll) {
        return combinedLight <= roll;
    }

    public static boolean validMobLightLevel(LevelReader level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(SKY_LIGHT_ROLL_BOUND)) return false;
        boolean thunder = level instanceof ServerLevelAccessor server && server.getLevel().isThundering();
        int combined = level.getMaxLocalRawBrightness(pos, thunder ? 10 : level.getSkyDarken());
        return combined <= random.nextInt(BLOCK_LIGHT_ROLL_BOUND);
    }

    /** Original EntityWisp.isValidLightLevel uses the same saved-sky/combined pair. */
    public static boolean validWispLightLevel(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        return validMobLightLevel(level, pos, random);
    }

    /** Pure gate with pre-read saved sky and combined light, not two channels. */
    public static boolean validMobLightLevel(int savedSkyLight, int combinedLight, int skyRoll, int lightRoll) {
        return savedSkyLight <= skyRoll && combinedLight <= lightRoll;
    }

    public static boolean validWispLightLevel(int savedSkyLight, int combinedLight, int skyRoll, int lightRoll) {
        return validMobLightLevel(savedSkyLight, combinedLight, skyRoll, lightRoll);
    }
}
