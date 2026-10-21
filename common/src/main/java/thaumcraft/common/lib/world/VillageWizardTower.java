package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import thaumcraft.common.config.ThaumcraftVillages;

/**
 * Oracle thaumcraft.common.lib.world.ComponentWizardTower, the TC4 wizard
 * tower.
 *
 * <p>The original was a {@code StructureVillagePieces.Village} piece: the
 * village handed it a bounding box, a ground level and a facing, and the
 * piece wrote its own blocks into the world. 1.21.1 has no village piece
 * system, so the component became a deterministic per chunk feature; the
 * tower shape, the block composition, the ladder, the trapdoor, the glowstone
 * and the chest content are the original ones and live in
 * {@link VillageWorldGenRules}.
 *
 * <p>Original geometry: six wide, six deep and twelve high, with the door on
 * the local z 0 side, so the component faces north before it is rotated.
 */
public final class VillageWizardTower {
    /** The original bounding box of the piece, oracle ComponentWizardTower:47. */
    public static final int WIDTH = VillageWorldGenRules.WIZARD_TOWER_WIDTH;
    public static final int HEIGHT = VillageWorldGenRules.WIZARD_TOWER_HEIGHT;
    public static final int DEPTH = VillageWorldGenRules.WIZARD_TOWER_DEPTH;

    private VillageWizardTower() {}

    /**
     * Writes the wizard tower into the world.
     *
     * @param level the world being generated
     * @param random the chunk random, used for the chest content
     * @param origin the world position of the local block 0,0,0
     * @param rotation the quarter turns the component is turned by
     */
    public static boolean build(WorldGenLevel level, RandomSource random, BlockPos origin, int rotation) {
        // Oracle ComponentWizardTower:48 - the piece refuses a place where it
        // would overlap another piece of the village. The ground of the piece
        // is levelled first, like the village generator of the original did.
        ThaumcraftVillages.levelGround(level, origin, rotation, WIDTH, DEPTH);
        if (!ThaumcraftVillages.spaceIsFree(level, origin, rotation, WIDTH, DEPTH, HEIGHT)) return false;
        boolean placed = false;
        List<VillageWorldGenRules.Placement> placements = VillageWorldGenRules.wizardTower();
        for (VillageWorldGenRules.Placement placement : placements) {
            int x = VillageWorldGenRules.rotateX(placement.x(), placement.z(), WIDTH, DEPTH, rotation);
            int z = VillageWorldGenRules.rotateZ(placement.x(), placement.z(), WIDTH, DEPTH, rotation);
            placed |= VillageBankerHome.set(level, origin.offset(x, placement.y(), z), placement.material(), rotation);
        }
        // Oracle ComponentWizardTower:97-99 - the stone step in front of the door.
        int stepX = VillageWorldGenRules.rotateX(3, 0, WIDTH, DEPTH, rotation);
        int stepZ = VillageWorldGenRules.rotateZ(3, 0, WIDTH, DEPTH, rotation);
        BlockPos step = origin.offset(stepX, 0, stepZ);
        if (level.getBlockState(step).isAir() && !level.getBlockState(step.below()).isAir()) {
            placed |= VillageBankerHome.set(level, step, VillageWorldGenRules.Material.STONE_STAIRS, rotation);
        }
        // Oracle ComponentWizardTower:92-93 - the chest with its loot.
        int chestX = VillageWorldGenRules.rotateX(2, 2, WIDTH, DEPTH, rotation);
        int chestZ = VillageWorldGenRules.rotateZ(2, 2, WIDTH, DEPTH, rotation);
        fillChest(level, origin.offset(chestX, 6, chestZ), random);
        // Oracle ComponentWizardTower:100-105 - clear the space above the
        // tower and lay a cobble footing under it.
        ThaumcraftVillages.clearAboveAndFillBelow(level, origin, rotation, WIDTH, DEPTH, HEIGHT);
        // Oracle ComponentWizardTower:106 - the wizard of the tower. The
        // original spawns it at x 7, which is outside the six wide piece, so
        // the original never spawns it either.
        int villagerX = VillageWorldGenRules.rotateX(VillageWorldGenRules.WIZARD_TOWER_VILLAGER_X,
                VillageWorldGenRules.WIZARD_TOWER_VILLAGER_Z, WIDTH, DEPTH, rotation);
        int villagerZ = VillageWorldGenRules.rotateZ(VillageWorldGenRules.WIZARD_TOWER_VILLAGER_X,
                VillageWorldGenRules.WIZARD_TOWER_VILLAGER_Z, WIDTH, DEPTH, rotation);
        if (villagerX >= 0 && villagerX < WIDTH && villagerZ >= 0 && villagerZ < DEPTH) {
            VillageBankerHome.spawnVillager(level,
                    origin.offset(villagerX, VillageWorldGenRules.WIZARD_TOWER_VILLAGER_Y, villagerZ));
        }
        return placed;
    }

    /**
     * The tower chest, oracle ComponentWizardTower:92. The original rolls the
     * count from four to nine and fills the chest with that many weighted
     * items.
     */
    private static void fillChest(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        List<VillageWorldGenRules.ChestContent> contents = VillageWorldGenRules.towerChestContents();
        int total = VillageWorldGenRules.towerChestTotalWeight();
        int count = VillageWorldGenRules.towerChestItemCount(random.nextInt());
        int slot = 0;
        for (int i = 0; i < count && slot < chest.getContainerSize(); i++) {
            int roll = random.nextInt(total);
            VillageWorldGenRules.ChestContent chosen = contents.get(contents.size() - 1);
            for (VillageWorldGenRules.ChestContent content : contents) {
                roll -= content.weight();
                if (roll < 0) { chosen = content; break; }
            }
            int amount = chosen.min();
            if (chosen.max() > chosen.min()) amount += random.nextInt(chosen.max() - chosen.min() + 1);
            ItemStack stack = stack(chosen.item(), amount);
            if (stack.isEmpty()) continue;
            chest.setItem(slot++, stack);
        }
    }

    private static ItemStack stack(String id, int amount) {
        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
        return item.map(value -> new ItemStack(value, amount)).orElse(ItemStack.EMPTY);
    }
}
