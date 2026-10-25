package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.SpaRules;
import thaumcraft.common.lib.fluid.PureBucketActions;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * The death fluid and the purifying fluid of TC4 4.2.3.5 against the 1.21.1 port.
 *
 * <p>Every number below is the value of the decompiled original; the file and the line of the
 * 1.7.10 oracle are named in the comment of the check. No block is constructed here: a
 * constructed block leaves an unregistered intrusive holder behind and breaks the registry
 * freeze of the whole test suite, so every rule is a pure function.</p>
 */
class FluidFamilyParityTest {

    // ------------------------------------------------------------- the death fluid

    /**
     * Oracle BlockFluidDeath:41 - the death fluid holds four quanta in a source cell.
     * Oracle ConfigBlocks:420 - its luminosity is eight.
     * Oracle BlockFluidDeath:55-59 - the damage of a cell is its metadata plus one, and the
     * metadata of a cell is the quanta of the cell minus one, so the damage is the quanta.
     */
    @Test void theDeathFluidKeepsTheOriginalQuantaLightAndDamage() {
        assertEquals(4, BlockFluidDeath.QUANTA_PER_BLOCK, "BlockFluidDeath:41 - setQuantaPerBlock(4)");
        assertEquals(8, BlockFluidDeath.LIGHT, "ConfigBlocks:420 - the luminosity of the death fluid");
        for (int level = 0; level < 4; level++) {
            assertEquals(4 - level, BlockFluidDeath.quanta(level), "the quanta of the cell");
            assertEquals(4 - level, BlockFluidDeath.damage(level), "the metadata of the cell plus one");
            assertEquals(3 - level, LegacyTileRules.deathFluidOriginalMetadata(level), "the original metadata of the cell");
        }
    }

    /**
     * Oracle ItemBucketDeath:103 and EventHandlerWorld:216-223 - the bucket of death pours the
     * cell of the original metadata three, which is the cell that still holds all four quanta.
     * The native liquid level is the inverse of the original metadata, so the source cell of the
     * native liquid is the level zero.
     */
    @Test void theBucketOfDeathFillsTheSourceCellOfTheOriginalMetadataThree() {
        assertEquals(3, BlockFluidDeath.BUCKET_CELL_METADATA, "ItemBucketDeath:103 - the metadata three");
        assertEquals(0, BlockFluidDeath.bucketCellLevel(), "the native level of the original metadata three");
        assertEquals(0, LegacyTileRules.deathFluidLevelForOriginalMetadata(3), "the mirror of the original metadata");
        assertEquals(3, LegacyTileRules.deathFluidOriginalMetadata(0), "the original metadata of the source cell");
    }

    /** The original finite parent shares a sum and retains its asymmetric remainder/RNG convention. */
    @Test void theDeathFluidSharesExistingStockInsteadOfFeedingNeighbors() {
        int[] shared = LegacyTileRules.deathFluidDistribution(4, new int[] {0, 0, 0, 0}, bound -> 0);
        assertEquals(4, java.util.Arrays.stream(shared).sum(), "one bucket is still four quanta across five cells");
        assertEquals(List.of(1, 0, 1, 1, 1), java.util.Arrays.stream(shared).boxed().toList(),
                "Forge finite N/S/W/E remainder allocation, with the zero draw");
        assertEquals(List.of(1, 0, 0, 0, 0), java.util.Arrays.stream(
                LegacyTileRules.deathFluidDistribution(1, new int[] {0, 0, 0, 0}, bound -> 0)).boxed().toList(),
                "an isolated final quantum does not evaporate or copy itself");
        assertEquals(7, BlockFluidDeath.TICK_DELAY, "Forge viscosity1500 divided by200 ticks");
        // The level of a cell never leaves the range of the four quanta.
        assertEquals(3, LegacyTileRules.deathFluidCellLevel(true, 9));
        assertEquals(0, LegacyTileRules.deathFluidCellLevel(true, -3));
        // A state that owns no level property never receives one.
        assertEquals(-1, LegacyTileRules.deathFluidCellLevel(false, 1));
    }

    /**
     * Oracle BlockFluidDeath:39 - the death fluid is a {@code BlockFluidFinite} over
     * {@code Material.water} (the readable 1.7.10 reference names {@code Material.field_151586_h}
     * as {@code Material.water}, RenderEventHandler:367). A 1.7.10 entity that stood in it was
     * therefore an entity in water. The 1.21.1 counterpart of that material is the vanilla
     * {@code minecraft:water} tag of the fluids.
     */
    @Test void theDeathFluidIsInTheVanillaWaterTagLikeTheOriginalMaterialWater() throws Exception {
        JsonArray values = waterTag();
        assertTrue(values.contains(new JsonPrimitive("thaumcraft:death_fluid")), "the death fluid is a water of the original");
        assertTrue(values.contains(new JsonPrimitive("thaumcraft:death_fluid_flowing")), "the flowing death fluid is a water of the original");
        assertTrue(values.contains(new JsonPrimitive("thaumcraft:purifying_fluid")), "the purifying fluid is a water of the original");
        assertTrue(values.contains(new JsonPrimitive("thaumcraft:flowing_purifying_fluid")), "the flowing purifying fluid is a water of the original");
    }

    // ------------------------------------------------------------- the purifying fluid

    /**
     * Oracle ConfigBlocks:419 - the purifying fluid has the luminosity ten, and the Forge
     * {@code BlockFluidClassic} subtracts one quantum from the scaled light, so the source cell
     * emits the light eight and the flowing cells seven, six, five, three, two, one and none.
     */
    @Test void thePurifyingFluidKeepsTheOriginalScaledLight() {
        assertEquals(List.of(8, 7, 6, 5, 3, 2, 1, 0),
                List.of(SpaRules.pureLight(0), SpaRules.pureLight(1), SpaRules.pureLight(2), SpaRules.pureLight(3),
                        SpaRules.pureLight(4), SpaRules.pureLight(5), SpaRules.pureLight(6), SpaRules.pureLight(7)));
    }

    /**
     * Oracle BlockFluidPure:41-56 - the Ward of a player that touches a source of the purifying
     * fluid lasts {@code min(32000, 200000 / max(1, floor(sqrt(permanentWarp))))} ticks.
     */
    @Test void thePurifyingFluidKeepsTheOriginalWardDuration() {
        assertEquals(32000, SpaRules.warpWardDuration(0), "without a permanent warp the Ward lasts the maximum");
        assertEquals(32000, SpaRules.warpWardDuration(1));
        assertEquals(32000, SpaRules.warpWardDuration(4), "the divisor of two still asks for more than the maximum");
        assertEquals(200000 / 7, SpaRules.warpWardDuration(49), "the divisor is the square root of the permanent warp");
        assertEquals(20000, SpaRules.warpWardDuration(100));
        assertEquals(200, SpaRules.warpWardDuration(1000000), "the Ward shrinks with the permanent warp");
    }

    /**
     * Oracle ItemBucketPure:103 and the original FillBucketEvent - the bucket of the pure fluid
     * captures the cell of the metadata zero, the source cell only.
     */
    @Test void theBucketOfThePureFluidCapturesTheSourceCellOnly() {
        assertTrue(PureBucketActions.isSourceLevel(0), "the source cell");
        for (int level = 1; level <= 7; level++) {
            assertFalse(PureBucketActions.isSourceLevel(level), "the flowing cell " + level + " stays in the world");
        }
    }

    // ------------------------------------------------------------- the helpers

    /** The vanilla water tag of the port. */
    private static JsonArray waterTag() throws Exception {
        var stream = FluidFamilyParityTest.class.getClassLoader()
                .getResourceAsStream("data/minecraft/tags/fluid/water.json");
        assertNotNull(stream, "the vanilla water tag is missing");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject tag = JsonParser.parseReader(reader).getAsJsonObject();
            return tag.getAsJsonArray("values");
        }
    }
}
