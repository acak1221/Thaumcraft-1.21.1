package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * The cosmetic stairs and the custom plant of TC4 4.2.3.5 against the 1.21.1 port.
 *
 * <p>Every number below is the value of the decompiled original; the file and the line of the
 * 1.7.10 oracle are named in the comment of the check. No block is constructed here: a
 * constructed block leaves an unregistered intrusive holder behind and breaks the registry
 * freeze of the whole test suite, so every rule is a pure function.</p>
 */
class CosmeticAndPlantParityTest {

    // ------------------------------------------------------------- the cosmetic stairs

    /**
     * Oracle BlockCosmeticSolid:66-67 - the cosmetic solid calls
     * {@code func_149752_b(10.0f)} (the blast resistance) and {@code func_149711_c(2.0f)} (the
     * hardness). The 1.7.10 setter stores the resistance three times and its getter divides it
     * by five, so the <em>effective</em> blast resistance of the family is {@code 10 * 3 / 5} = 6.
     */
    @Test void theCosmeticFamilyKeepsTheOriginalHardnessAndEffectiveBlastResistance() {
        assertEquals(2.0F, EldritchBlockRules.COSMETIC_FAMILY_HARDNESS, "BlockCosmeticSolid:67 - setHardness(2.0f)");
        assertEquals(6.0F, EldritchBlockRules.COSMETIC_FAMILY_NATIVE_RESISTANCE,
                "BlockCosmeticSolid:66 - setResistance(10.0f) is stored three times and divided by five");
    }

    /**
     * The 1.7.10 {@code BlockStairs} copies the hardness and the blast resistance of the block
     * whose texture it copies, so the four stairs of the original are:
     *
     * <ul>
     * <li>the eldritch stairs: the cosmetic solid, the metadata eleven - hardness 2.0, resistance 6.0</li>
     * <li>the arcane stone stairs: the cosmetic solid, the metadata seven - hardness 2.0, resistance 6.0</li>
     * <li>the greatwood and the silverwood stairs: the wooden device, the metadata six and seven -
     * hardness 2.5, resistance 6.0</li>
     * </ul>
     *
     * <p>The metadata seven of the cosmetic solid has the hardness four and the blast resistance
     * twenty of its own (oracle BlockCosmeticSolid:221-223 and :249-251), but the stair takes
     * the <em>field</em> of the block and not the per metadata getter, so the stair of that
     * metadata keeps the hardness two and the resistance six of the family.</p>
     */
    @Test void theStairsOfTheCosmeticFamilyCopyTheHardnessAndTheResistanceOfTheirBlock() {
        // The eldritch stairs and the arcane stone stairs copy the cosmetic solid.
        assertEquals(2.0F, EldritchBlockRules.COSMETIC_FAMILY_HARDNESS);
        assertEquals(6.0F, EldritchBlockRules.COSMETIC_FAMILY_NATIVE_RESISTANCE);
        // The greatwood and the silverwood stairs copy the wooden device.
        assertEquals(2.5F, EldritchBlockRules.DEVICE_WOOD_HARDNESS, "BlockWoodenDevice:83 - setHardness(2.5f)");
        assertEquals(6.0F, EldritchBlockRules.DEVICE_WOOD_RESISTANCE, "BlockWoodenDevice:82 - setResistance(10.0f) is 10 * 3 / 5");
        // The eldritch stairs are the only seamless ones of the family.
        assertTrue(EldritchBlockRules.stairsAreSeamless(11), "the eldritch stairs copy the metadata eleven");
        assertFalse(EldritchBlockRules.stairsAreSeamless(7), "the arcane stone stairs copy the metadata seven");
    }

    /**
     * Oracle BlockCosmeticStairs:32 - the original stairs of the cosmetic family set the light
     * opacity to zero, so a dry stair lets the light through.
     */
    @Test void theCosmeticStairsKeepTheOriginalLightOpacity() {
        assertEquals(0, BlockCosmeticStairs.lightOpacity(false, 15), "a dry cosmetic stair lets the light through");
        assertEquals(15, BlockCosmeticStairs.lightOpacity(true, 15), "a waterlogged cosmetic stair keeps the vanilla opacity");
    }

    // ------------------------------------------------------------- the custom plant

    /**
     * Oracle BlockCustomPlantItem:85-101 - the plant type the original <em>item</em> of the
     * metadata asks the soil for: the metadata three (the cinderpearl) is Desert, the metadata
     * four (the purifier seed) and the metadata five (the manashroom) are Cave and every other
     * metadata is Plains.
     */
    @Test void theOriginalItemAsksTheSoilForItsOwnPlantType() {
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.itemPlantType(0), "the greatwood sapling");
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.itemPlantType(1), "the silverwood sapling");
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.itemPlantType(2), "the shimmerleaf");
        assertEquals(BlockCustomPlant.PlantType.DESERT, BlockCustomPlant.itemPlantType(3), "the cinderpearl");
        assertEquals(BlockCustomPlant.PlantType.CAVE, BlockCustomPlant.itemPlantType(4), "the purifier seed");
        assertEquals(BlockCustomPlant.PlantType.CAVE, BlockCustomPlant.itemPlantType(5), "the manashroom");
    }

    /**
     * Oracle BlockCustomPlant:119-128 - the plant type the original <em>block</em> of the
     * metadata answers: only the metadata five differs from the table of the item, the manashroom
     * is a Plains plant for its block and a Cave plant for its item.
     */
    @Test void theOriginalBlockAnswersItsOwnPlantType() {
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.blockPlantType(0));
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.blockPlantType(1));
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.blockPlantType(2));
        assertEquals(BlockCustomPlant.PlantType.DESERT, BlockCustomPlant.blockPlantType(3));
        assertEquals(BlockCustomPlant.PlantType.CAVE, BlockCustomPlant.blockPlantType(4));
        assertEquals(BlockCustomPlant.PlantType.PLAINS, BlockCustomPlant.blockPlantType(5),
                "the original block of the manashroom is a Plains plant, its item asks for a Cave soil");
    }

    /**
     * Oracle BlockCustomPlantItem:50-65 - the item only places the plant on the top face of a
     * block whose soil supports its plant type. The soil rule of the port is a pure function, so
     * no plant and no item is constructed here.
     */
    @Test void thePlantSoilRuleIsTheOriginalOne() {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState farmland = Blocks.FARMLAND.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        // The Plains type: the soils of the inherited BlockBush.
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, grass));
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, dirt));
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, farmland));
        assertFalse(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, sand),
                "the sand carries the Desert plant type only");
        assertFalse(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.PLAINS, stone));
        // The Desert type: the sand of the cinderpearl.
        assertTrue(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.DESERT, sand));
        assertFalse(BlockCustomPlant.soilSupports(BlockCustomPlant.PlantType.DESERT, grass));
        // The convenience rule of the port that the production code calls.
        assertTrue(BlockCustomPlant.soilSupports(false, grass));
        assertFalse(BlockCustomPlant.soilSupports(false, sand));
        assertTrue(BlockCustomPlant.soilSupports(true, sand));
    }
}
