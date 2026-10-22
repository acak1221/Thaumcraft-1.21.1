package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.Test;

/**
 * The soil rule of the item of the plant of TC4 4.2.3.5 against the 1.21.1 port.
 *
 * <p>The rule is the Forge 1.7.10 table of {@code Block.canSustainPlant}, the platform the
 * original ran on; the table is quoted in the comment of the check. No block is constructed
 * here: a constructed block leaves an unregistered intrusive holder behind and breaks the
 * registry freeze of the whole test suite, so every rule is a pure function over the states of
 * the blocks the vanilla already registered.</p>
 */
class PlantSoilRuleParityTest {

    private static boolean caveSoil(BlockState soil) {
        return BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.CAVE, soil,
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /**
     * The 1.7.10 {@code BlockBush.func_149854_a} - the MCP name of the method is
     * {@code canPlaceBlockOn} and the MCP mapping describes it as "is the block grass, dirt or
     * farmland". The port had accepted the podzol and the coarse dirt as well; the coarse dirt
     * does not exist in 1.7.10 at all and the podzol is not in the table of the original.
     */
    @Test void theInheritedSoilIsGrassDirtAndFarmland() {
        assertTrue(BlockCustomPlant.bushSoil(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(BlockCustomPlant.bushSoil(Blocks.DIRT.defaultBlockState()));
        assertTrue(BlockCustomPlant.bushSoil(Blocks.FARMLAND.defaultBlockState()));
        assertFalse(BlockCustomPlant.bushSoil(Blocks.PODZOL.defaultBlockState()),
                "the podzol is not in the table of the 1.7.10 BlockBush");
        assertFalse(BlockCustomPlant.bushSoil(Blocks.COARSE_DIRT.defaultBlockState()),
                "the coarse dirt does not exist in 1.7.10");
        assertFalse(BlockCustomPlant.bushSoil(Blocks.SAND.defaultBlockState()));
        assertFalse(BlockCustomPlant.bushSoil(Blocks.STONE.defaultBlockState()));
    }

    /**
     * The Cave case of the Forge 1.7.10 table is
     * {@code isSideSolid(world, x, y, z, UP)}: the soil must present a solid face on its top
     * side. The default of that method is a full opaque cube.
     */
    @Test void theCaveSoilIsTheSolidTopSideOfTheOriginal() {
        assertTrue(caveSoil(Blocks.STONE.defaultBlockState()), "a full opaque cube");
        assertTrue(caveSoil(Blocks.COBBLESTONE.defaultBlockState()));
        assertTrue(caveSoil(Blocks.SAND.defaultBlockState()));
        assertTrue(caveSoil(Blocks.DIRT.defaultBlockState()));
        assertFalse(caveSoil(Blocks.GLASS.defaultBlockState()),
                "the glass of 1.21.1 carries noOcclusion, the glass of 1.7.10 was not opaque");
        assertFalse(caveSoil(Blocks.OAK_LEAVES.defaultBlockState()),
                "the leaves of 1.21.1 carry noOcclusion, the leaves of 1.7.10 were not opaque");
    }

    /**
     * The special cases of the 1.7.10 {@code Block.isSideSolid}: the upper half of a slab, the
     * upper half of the stairs, the full snow layer of eight and the hopper are solid on their
     * top side, the lower half of a slab and the farmland are not.
     */
    @Test void theSpecialCasesOfTheOriginalSolidSideAreKept() {
        assertTrue(caveSoil(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)),
                "the upper half of a slab is solid on its top side");
        assertFalse(caveSoil(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)),
                "the lower half of a slab is not");
        assertTrue(caveSoil(Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP)),
                "the flipped stairs are solid on their top side");
        assertFalse(caveSoil(Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM)),
                "the stairs are not");
        assertTrue(caveSoil(Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8)),
                "the full snow layer of eight is solid on its top side");
        assertFalse(caveSoil(Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1)),
                "a thin snow layer is not");
        assertTrue(caveSoil(Blocks.HOPPER.defaultBlockState()), "the hopper is solid on its top side");
    }

    /**
     * The soil of the item of the plant is the table of the plant type: the BlockBush soil for
     * every type, the sand for the Desert type and the solid top side for the Cave type.
     */
    @Test void theSoilOfTheItemIsTheTableOfItsPlantType() {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        // The Plains type: the soil of the inherited BlockBush alone.
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, grass, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, sand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, stone, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        // The Desert type: the inherited soil plus the sand.
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.DESERT, sand, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.DESERT, grass, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertFalse(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.DESERT, stone, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        // The Cave type: the inherited soil plus the solid top side.
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.CAVE, grass, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.CAVE, stone, EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
}
