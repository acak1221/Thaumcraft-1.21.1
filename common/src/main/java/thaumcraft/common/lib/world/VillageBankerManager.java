package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftVillages;

/**
 * Oracle thaumcraft.common.lib.world.VillageBankerManager, the TC4 banker
 * home village piece.
 *
 * <p>In 1.7.10 the manager was a Forge
 * {@code VillagerRegistry.IVillageCreationHandler}: it gave the vanilla
 * village a piece weight for the banker home, built the piece and added the
 * banker trades. 1.21.1 has no village piece system and no villager
 * registry, so the manager became the deterministic per chunk feature that
 * places the banker homes. The piece weight (25), the minimum village size
 * the original computed and the trade table are unchanged and live in
 * {@link VillageWorldGenRules}.
 */
public final class VillageBankerManager extends Feature<NoneFeatureConfiguration> {
    /** Oracle VillageBankerManager:59 - the piece weight of the banker home. */
    public static final int PIECE_WEIGHT = VillageWorldGenRules.BANKER_HOME_WEIGHT;
    /** Oracle ConfigEntities:83 - the profession id of the banker. */
    public static final int PROFESSION_ID = VillageWorldGenRules.BANKER_PROFESSION_ID;

    public VillageBankerManager() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /**
     * The banker trades, oracle VillageBankerManager:42-55. The native tree
     * has no villager profession yet, so the table is kept as data until the
     * villager unit binds it.
     */
    public static List<VillageWorldGenRules.TradeSpec> trades() {
        return VillageWorldGenRules.bankerTrades();
    }

    /**
     * The minimum village size of the piece, oracle VillageBankerManager:59.
     * The original draws it with {@code MathHelper.getIntWithMinMax(random, 0 + i, 1 + i)},
     * a range one wide, so the draw is always the village size itself.
     */
    public static int pieceWeightMinSize(int villageSize) {
        return VillageWorldGenRules.pieceWeightMinSize(villageSize);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!ThaumcraftOreFeature.isSurfaceDimension(level)) return false;
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        VillageWorldGenRules.VillagePlan plan = VillageWorldGenRules.villagePlan(level.getLevel().getSeed(), chunkX, chunkZ);
        int house = VillageWorldGenRules.houseIndex(plan, chunkX, chunkZ);
        if (house < 0) return false;
        if (plan.houses().get(house) != VillageWorldGenRules.HouseType.BANKER_HOME) return false;
        int rotation = VillageWorldGenRules.houseRotation(plan, house);
        RandomSource random = RandomSource.create(VillageWorldGenRules.houseSeed(
                level.getLevel().getSeed(), chunkX, chunkZ, house));
        // Oracle ComponentBankerHome:39-40 - the roof variant and the table slot.
        boolean tallHouse = random.nextBoolean();
        int tablePosition = random.nextInt(VillageWorldGenRules.BANKER_HOME_TABLE_SLOTS);
        int width = VillageWorldGenRules.rotatedWidth(VillageBankerHome.WIDTH, VillageBankerHome.DEPTH, rotation);
        int depth = VillageWorldGenRules.rotatedDepth(VillageBankerHome.WIDTH, VillageBankerHome.DEPTH, rotation);
        // The house stands in the middle of the chunk the village gave it.
        ChunkLoc chunk = new ChunkLoc(chunkX, chunkZ);
        int originX = chunk.getCenterXPos() - (width - 1) / 2;
        int originZ = chunk.getCenterZPosition() - (depth - 1) / 2;
        int ground = ThaumcraftVillages.averageGroundLevel(level, originX, originZ, width, depth);
        return VillageBankerHome.build(level, random, new BlockPos(originX, ground, originZ), rotation, tallHouse, tablePosition);
    }
}
