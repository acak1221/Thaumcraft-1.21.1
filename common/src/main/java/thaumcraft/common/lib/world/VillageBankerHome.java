package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.common.config.ThaumcraftVillages;

/**
 * Oracle thaumcraft.common.lib.world.ComponentBankerHome, the TC4 banker home.
 *
 * <p>The original was a {@code StructureVillagePieces.Village} piece: the
 * village handed it a bounding box, a ground level and a facing, and the
 * piece wrote its own blocks into the world. 1.21.1 has no village piece
 * system, so the component became a deterministic per chunk feature; the
 * house shape, the block composition, the two roof variants, the three table
 * slots and the villager position are the original ones and live in
 * {@link VillageWorldGenRules}.
 *
 * <p>Original geometry: four wide, five deep and six high, with the door on
 * the local z 0 side, so the component faces north before it is rotated.
 */
public final class VillageBankerHome {
    /** The original bounding box of the piece, oracle ComponentBankerHome:56. */
    public static final int WIDTH = VillageWorldGenRules.BANKER_HOME_WIDTH;
    public static final int HEIGHT = VillageWorldGenRules.BANKER_HOME_HEIGHT;
    public static final int DEPTH = VillageWorldGenRules.BANKER_HOME_DEPTH;

    private VillageBankerHome() {}

    /**
     * Writes the banker home into the world.
     *
     * @param level the world being generated
     * @param random the chunk random, used for the villager
     * @param origin the world position of the local block 0,0,0
     * @param rotation the quarter turns the component is turned by
     * @param tallHouse the original roof variant
     * @param tablePosition the wall slot of the table, 0 for no table
     */
    public static boolean build(WorldGenLevel level, RandomSource random, BlockPos origin, int rotation,
                                 boolean tallHouse, int tablePosition) {
        // Oracle ComponentBankerHome:57 - the piece refuses a place where it
        // would overlap another piece of the village. The ground of the piece
        // is levelled first, like the village generator of the original did.
        ThaumcraftVillages.levelGround(level, origin, rotation, WIDTH, DEPTH);
        if (!ThaumcraftVillages.spaceIsFree(level, origin, rotation, WIDTH, DEPTH, HEIGHT)) return false;
        boolean placed = false;
        List<VillageWorldGenRules.Placement> placements = VillageWorldGenRules.bankerHome(tallHouse, tablePosition);
        for (VillageWorldGenRules.Placement placement : placements) {
            int x = VillageWorldGenRules.rotateX(placement.x(), placement.z(), WIDTH, DEPTH, rotation);
            int z = VillageWorldGenRules.rotateZ(placement.x(), placement.z(), WIDTH, DEPTH, rotation);
            placed |= set(level, origin.offset(x, placement.y(), z), placement.material(), rotation);
        }
        // Oracle ComponentBankerHome:103-105 - the stone step in front of the door.
        int stepX = VillageWorldGenRules.rotateX(1, -1, WIDTH, DEPTH, rotation);
        int stepZ = VillageWorldGenRules.rotateZ(1, -1, WIDTH, DEPTH, rotation);
        BlockPos step = origin.offset(stepX, 0, stepZ);
        if (level.getBlockState(step).isAir() && !level.getBlockState(step.below()).isAir()) {
            placed |= set(level, step, VillageWorldGenRules.Material.STONE_STAIRS, rotation);
        }
        // Oracle ComponentBankerHome:106-111 - clear the space above the house
        // and lay a cobble footing under it.
        ThaumcraftVillages.clearAboveAndFillBelow(level, origin, rotation, WIDTH, DEPTH, HEIGHT);
        // Oracle ComponentBankerHome:112 - the banker of the house.
        int villagerX = VillageWorldGenRules.rotateX(VillageWorldGenRules.BANKER_HOME_VILLAGER_X,
                VillageWorldGenRules.BANKER_HOME_VILLAGER_Z, WIDTH, DEPTH, rotation);
        int villagerZ = VillageWorldGenRules.rotateZ(VillageWorldGenRules.BANKER_HOME_VILLAGER_X,
                VillageWorldGenRules.BANKER_HOME_VILLAGER_Z, WIDTH, DEPTH, rotation);
        spawnVillager(level, origin.offset(villagerX, VillageWorldGenRules.BANKER_HOME_VILLAGER_Y, villagerZ));
        return placed;
    }

    /**
     * The villager of the house. The native tree has no TC4 villager
     * professions yet, so the villager is spawned plain; the banker trades
     * live in {@link VillageBankerManager} until the villager unit binds them.
     */
    static void spawnVillager(WorldGenLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        Entity villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (villager instanceof Mob mob) mob.setPersistenceRequired();
        level.addFreshEntity(villager);
    }

    /** Writes one block of the component. */
    static boolean set(WorldGenLevel level, BlockPos pos, VillageWorldGenRules.Material material, int rotation) {
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        return level.setBlock(pos, ThaumcraftVillages.state(material, rotation), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }
}
