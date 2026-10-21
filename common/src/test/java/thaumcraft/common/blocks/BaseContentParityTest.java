package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.SpaRules;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * The parity of the base content of TC4 4.2.3.5 with the 1.21.1 port.
 *
 * <p>Every number below is the value of the decompiled original; the file and the line of the
 * 1.7.10 oracle are named in the comment of the check.</p>
 */
class BaseContentParityTest {
    static {
        // The registration classes touch the vanilla registries while they fill their entries.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ------------------------------------------------------------ the finite flux

    /**
     * {@code BlockFluxGasItem:9,17-19} and {@code BlockFluxGooItem:9,17-19} - the original
     * registers an item for both finite fluids and always places the block with the original
     * metadata eight, the exceptional nine unit birth of a Forge {@code BlockFluidFinite}.
     */
    @Test void theFiniteFluxItemsCarryTheOriginalIdsAndPlaceTheOriginalMetadataEight() {
        assertEquals("thaumcraft:flux_gas", ThaumcraftFlux.FLUX_GAS_ITEM.getId().toString());
        assertEquals("thaumcraft:flux_goo", ThaumcraftFlux.FLUX_GOO_ITEM.getId().toString());
        assertEquals(8, FluxFiniteBlock.PLACED_METADATA);
        assertEquals(8, BlockFluxGasItem.placedMetadata());
        assertEquals(8, BlockFluxGooItem.placedMetadata());
        assertTrue(BlockItem.class.isAssignableFrom(BlockFluxGasItem.class));
        assertTrue(BlockItem.class.isAssignableFrom(BlockFluxGooItem.class));
    }

    /**
     * {@code BlockFluxBase} of Forge 1.7.10 - the metadata of a finite fluid cell runs from zero
     * to eight, the eighth being the exceptional birth of nine units in a cell of eight.
     */
    @Test void theFiniteFluxLevelCarriesTheOriginalMetadataRange() {
        assertEquals("level", FluxFiniteBlock.LEVEL.getName());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8), new ArrayList<>(FluxFiniteBlock.LEVEL.getPossibleValues()));
        assertEquals(8, FluxFiniteBlock.QUANTA);
    }

    /**
     * {@code BlockFluxGoo:88-122} - a cell of the metadata two up to five breeds a size one
     * slime in one of twenty five, a cell of the metadata six and above breeds a size two slime
     * or turns into the taint in one of fifty, and every other cell decays in one of thirty.
     */
    @Test void theFluxGooKeepsTheOriginalBreedingNumbers() {
        assertEquals(2, BlockFluxGoo.SLIME_METADATA_MIN);
        assertEquals(6, BlockFluxGoo.SLIME_METADATA_MAX);
        assertEquals(6, BlockFluxGoo.TAINT_METADATA);
        assertEquals(25, BlockFluxGoo.BREED_ROLL);
        assertEquals(50, BlockFluxGoo.TAINT_ROLL);
        assertEquals(30, BlockFluxGoo.DECAY_ROLL);
        assertEquals(1, BlockFluxGoo.SLIME_SIZE_THIN);
        assertEquals(2, BlockFluxGoo.SLIME_SIZE_THICK);
    }

    // ------------------------------------------------------------ the custom plants

    /**
     * {@code BlockCustomPlantItem:50-65} - the original item only places a plant on the top
     * face of a block whose soil supports its plant type, so the item of the port has to refuse
     * the placement that the original refuses instead of losing the item to a later tick.
     */
    @Test void thePlantItemRefusesThePlacementTheOriginalRefuses() throws Exception {
        // BlockCustomPlantItem:50-65 - the item only places the plant on the top face of a
        // block whose soil supports its plant type. The rule itself is the pure soil function
        // of the block, so no block is constructed here: a constructed block would leave an
        // unregistered intrusive holder behind and break the registry freeze of the suite.
        Method mustSurvive = BlockCustomPlant.PlantItem.class.getDeclaredMethod("mustSurvive");
        assertEquals(BlockCustomPlant.PlantItem.class, mustSurvive.getDeclaringClass(),
                "the original item refuses a plant without a supporting soil (BlockCustomPlantItem:54)");
        Method placement = BlockCustomPlant.PlantItem.class.getDeclaredMethod("getPlacementState", BlockPlaceContext.class);
        assertEquals(BlockCustomPlant.PlantItem.class, placement.getDeclaringClass(),
                "the original item only places the plant on the top face (BlockCustomPlantItem:51)");
        // BlockCustomPlantItem:54 - the soil has to carry the plant type of the metadata.
        assertTrue(BlockCustomPlant.soilSupports(false, Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(BlockCustomPlant.soilSupports(false, Blocks.DIRT.defaultBlockState()));
        assertTrue(BlockCustomPlant.soilSupports(false, Blocks.FARMLAND.defaultBlockState()));
        assertFalse(BlockCustomPlant.soilSupports(false, Blocks.SAND.defaultBlockState()),
                "the sand carries the Desert plant type only");
        assertFalse(BlockCustomPlant.soilSupports(false, Blocks.STONE.defaultBlockState()));
        assertTrue(BlockCustomPlant.soilSupports(true, Blocks.SAND.defaultBlockState()),
                "the cinderpearl is the Desert plant type of the original metadata three");
    }

    // ------------------------------------------------------------ the magical leaves

    /**
     * {@code BlockMagicalLeaves:246-255} - a leaf that is flagged for the decay and that was
     * not placed by a player drops a sapling in one of two hundred (greatwood) or in one of two
     * hundred and fifty (silverwood) rolls.
     */
    @Test void theMagicalLeavesSaplingRollsAreTheOriginalOnes() throws Exception {
        assertEquals(200, BlockMagicalLeaves.SAPLING_ROLL_GREATWOOD);
        assertEquals(250, BlockMagicalLeaves.SAPLING_ROLL_SILVERWOOD);
        assertEquals(BlockMagicalLeaves.SAPLING_ROLL_GREATWOOD, saplingBound("greatwood_leaves"));
        assertEquals(BlockMagicalLeaves.SAPLING_ROLL_SILVERWOOD, saplingBound("silverwood_leaves"));
    }

    /**
     * {@code BlockMagicalLeaves:247} - the original drops a sapling only when the metadata has
     * the bit eight (the leaf is flagged for the decay) and has not the bit four (the leaf was
     * placed by a player). The native port carries the bit eight in the check_decay state and
     * the bit four in the persistent state.
     */
    @Test void theMagicalLeavesSaplingNeedsTheOriginalDecayFlagAndNoPlayerFlag() throws Exception {
        for (String leaves : List.of("greatwood_leaves", "silverwood_leaves")) {
            JsonObject table = lootTable(leaves);
            JsonObject condition = saplingCondition(table);
            assertEquals("minecraft:block_state_property", condition.get("condition").getAsString());
            JsonObject properties = condition.getAsJsonObject("properties");
            assertEquals("true", properties.get("check_decay").getAsString(), leaves + ": the bit eight of the original");
            assertEquals("false", properties.get("persistent").getAsString(), leaves + ": the bit four of the original");
        }
    }

    // ------------------------------------------------------------ the fluids

    /**
     * {@code BlockFluidDeath:27} - the original death fluid holds four quanta in a source cell.
     * {@code ConfigBlocks:420} - its luminosity is eight.
     * {@code BlockFluidDeath:43} - the damage of a cell is its metadata plus one.
     *
     * <p>The native level of the death fluid is the inverse of the original metadata: the source
     * cell of the original, the metadata three, is the native level zero.</p>
     */
    @Test void theDeathFluidKeepsTheOriginalQuantaLightAndDamage() {
        assertEquals(4, BlockFluidDeath.QUANTA_PER_BLOCK);
        assertEquals(8, BlockFluidDeath.LIGHT);
        assertEquals(3, BlockFluidDeath.BUCKET_CELL_METADATA);
        for (int level = 0; level < 4; level++) {
            assertEquals(4 - level, BlockFluidDeath.quanta(level), "the quanta of the cell");
            assertEquals(4 - level, BlockFluidDeath.damage(level), "the metadata of the cell plus one");
            assertEquals(3 - level, LegacyTileRules.deathFluidOriginalMetadata(level));
        }
    }

    /**
     * {@code ConfigBlocks:419} - the purifying fluid has the luminosity ten, and the Forge
     * {@code BlockFluidClassic} subtracts one quantum from the scaled light, so the source cell
     * emits the light eight and the flowing cells seven, six, five, three, two, one and none.
     */
    @Test void thePurifyingFluidKeepsTheOriginalScaledLight() {
        assertEquals(List.of(8, 7, 6, 5, 3, 2, 1, 0),
                List.of(SpaRules.pureLight(0), SpaRules.pureLight(1), SpaRules.pureLight(2), SpaRules.pureLight(3),
                        SpaRules.pureLight(4), SpaRules.pureLight(5), SpaRules.pureLight(6), SpaRules.pureLight(7)));
    }

    // ------------------------------------------------------------ the cosmetic stairs

    /**
     * {@code BlockCosmeticStairs:20} - the original stairs of the cosmetic family have the light
     * opacity zero, so a dry stair lets the light through.
     */
    @Test void theCosmeticStairsKeepTheOriginalLightOpacity() {
        // BlockCosmeticStairs:32 - the original stairs of the cosmetic family have the light
        // opacity zero, so a dry stair lets the light through. The vanilla stair of a solid
        // block would have the opacity fifteen, so the override is a real difference.
        assertEquals(0, BlockCosmeticStairs.lightOpacity(false, 15),
                "a dry cosmetic stair lets the light through");
        assertEquals(15, BlockCosmeticStairs.lightOpacity(true, 15),
                "a waterlogged cosmetic stair keeps the vanilla opacity");
    }

    // ------------------------------------------------------------ the helpers

    private static JsonObject lootTable(String name) throws Exception {
        var stream = BaseContentParityTest.class.getClassLoader()
                .getResourceAsStream("data/thaumcraft/loot_table/blocks/" + name + ".json");
        assertNotNull(stream, "missing loot table " + name);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /** The bound of the sapling chance of a leaves loot table. */
    private static int saplingBound(String name) throws Exception {
        JsonObject table = lootTable(name);
        for (var pool : table.getAsJsonArray("pools")) {
            for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject item = entry.getAsJsonObject();
                if (!"minecraft:alternatives".equals(item.get("type").getAsString())) continue;
                for (var child : item.getAsJsonArray("children")) {
                    JsonObject sapling = child.getAsJsonObject();
                    if (!"thaumcraft:greatwood_sapling".equals(sapling.get("name").getAsString())
                            && !"thaumcraft:silverwood_sapling".equals(sapling.get("name").getAsString())) continue;
                    JsonArray functions = sapling.getAsJsonArray("functions");
                    assertEquals(1, functions.size());
                    return functions.get(0).getAsJsonObject().get("bound").getAsInt();
                }
            }
        }
        throw new AssertionError("no sapling entry in " + name);
    }

    /** The condition of the sapling entry of a leaves loot table. */
    private static JsonObject saplingCondition(JsonObject table) {
        for (var pool : table.getAsJsonArray("pools")) {
            for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject item = entry.getAsJsonObject();
                if (!"minecraft:alternatives".equals(item.get("type").getAsString())) continue;
                for (var child : item.getAsJsonArray("children")) {
                    JsonObject sapling = child.getAsJsonObject();
                    if (!"thaumcraft:greatwood_sapling".equals(sapling.get("name").getAsString())
                            && !"thaumcraft:silverwood_sapling".equals(sapling.get("name").getAsString())) continue;
                    JsonArray conditions = sapling.getAsJsonArray("conditions");
                    assertEquals(1, conditions.size());
                    return conditions.get(0).getAsJsonObject();
                }
            }
        }
        throw new AssertionError("no sapling entry in the loot table");
    }
}
