package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.lib.world.VillageWorldGenRules;

/**
 * Checks that the two TC4 village components really reach a generated chunk.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/VillageBankerManager:58-68} and
 * {@code VillageWizardManager:55-65} give the vanilla village two extra pieces, the
 * banker home (weight 25) and the wizard tower (weight 15). 1.21.1 has no village piece
 * system, so both became deterministic per chunk features: the village plan of a chunk
 * decides whether the chunk carries a banker home or a wizard tower.
 *
 * <p>This check finds the chunks the village plan gives a component to, generates them
 * and looks for the blocks of the original component (the banker home is a cobble floor
 * with a log frame, the wizard tower is a six by six cobble tower with a plank floor).
 */
public final class VillageComponentPlacementRuntimeChecks {
    /** The chunk coordinates the scan of the village cells covers. */
    private static final int SCAN_RADIUS = 96;
    /** The number of component chunks that are generated at most. */
    private static final int MAX_GENERATED = 12;

    private VillageComponentPlacementRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        ServerLevel level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null, "test-only normal terrain dimension is loaded");
        long seed = level.getSeed();

        List<int[]> bankers = componentChunks(seed, VillageWorldGenRules.HouseType.BANKER_HOME);
        List<int[]> wizards = componentChunks(seed, VillageWorldGenRules.HouseType.WIZARD_TOWER);
        test.assertTrue(!bankers.isEmpty(), "the village plan gives chunks to the banker home");
        test.assertTrue(!wizards.isEmpty(), "the village plan gives chunks to the wizard tower");

        boolean bankerBuilt = false;
        int bankerTried = 0;
        for (int[] chunk : bankers) {
            if (bankerTried >= MAX_GENERATED) break;
            ++bankerTried;
            level.getChunk(chunk[0], chunk[1]);
            if (hasBankerHome(level, chunk[0], chunk[1])) {
                bankerBuilt = true;
                break;
            }
        }
        test.assertTrue(bankerBuilt, "a chunk the village plan gave to the banker home carries the house, "
                + bankerTried + " chunks were generated");

        boolean wizardBuilt = false;
        int wizardTried = 0;
        for (int[] chunk : wizards) {
            if (wizardTried >= MAX_GENERATED) break;
            ++wizardTried;
            level.getChunk(chunk[0], chunk[1]);
            if (hasWizardTower(level, chunk[0], chunk[1])) {
                wizardBuilt = true;
                break;
            }
        }
        test.assertTrue(wizardBuilt, "a chunk the village plan gave to the wizard tower carries the tower, "
                + wizardTried + " chunks were generated");
        test.succeed();
    }

    /**
     * The chunks the village plan gives to a component, the same decision the feature
     * of the component makes for every chunk of the world.
     */
    private static List<int[]> componentChunks(long seed, VillageWorldGenRules.HouseType type) {
        List<int[]> chunks = new ArrayList<>();
        for (int chunkX = -SCAN_RADIUS; chunkX <= SCAN_RADIUS; ++chunkX) {
            for (int chunkZ = -SCAN_RADIUS; chunkZ <= SCAN_RADIUS; ++chunkZ) {
                VillageWorldGenRules.VillagePlan plan = VillageWorldGenRules.villagePlan(seed, chunkX, chunkZ);
                int house = VillageWorldGenRules.houseIndex(plan, chunkX, chunkZ);
                if (house < 0) continue;
                if (plan.houses().get(house) != type) continue;
                chunks.add(new int[] {chunkX, chunkZ});
            }
        }
        return chunks;
    }

    /** The banker home of the original is a cobble floor with a log frame and a plank wall. */
    private static boolean hasBankerHome(ServerLevel level, int chunkX, int chunkZ) {
        int base = chunkX * 16 + 8;
        int baseZ = chunkZ * 16 + 8;
        for (int x = base - 3; x <= base + 3; ++x) {
            for (int z = baseZ - 3; z <= baseZ + 3; ++z) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); ++y) {
                    // Only the village components place an oak door, the house
                    // has one at its local 1,1,0 before the rotation.
                    if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.OAK_DOOR)) return true;
                }
            }
        }
        return false;
    }

    /** The wizard tower of the original is a six by six cobble tower with a plank floor. */
    private static boolean hasWizardTower(ServerLevel level, int chunkX, int chunkZ) {
        int base = chunkX * 16 + 8;
        int baseZ = chunkZ * 16 + 8;
        for (int x = base - 3; x <= base + 3; ++x) {
            for (int z = baseZ - 3; z <= baseZ + 3; ++z) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); ++y) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.OAK_DOOR)) return true;
                }
            }
        }
        return false;
    }
}
