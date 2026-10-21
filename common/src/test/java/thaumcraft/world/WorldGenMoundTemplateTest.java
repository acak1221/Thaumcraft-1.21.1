package thaumcraft.world;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.WorldGenMound;
import thaumcraft.common.lib.world.WorldGenMoundTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The block template of the TC4 mound against the oracle.
 *
 * <p>The oracle {@code thaumcraft/common/lib/world/WorldGenMound.java:63-2549} writes the
 * mound with 2441 explicit block calls. The native port keeps that shape in the layer table
 * of {@link WorldGenMoundTemplate}. The checksum below is the sha256 of the canonical form of
 * the table as {@code scripts/porting/extract_mound_template.py} rebuilds it from the oracle,
 * so the equality of the two can be checked at any time:
 *
 * <pre>
 * python3 scripts/porting/extract_mound_template.py
 * oracle  : /home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/lib/world/WorldGenMound.java
 * layers  : 16
 * blocks  : 2441
 * sha256  : 93a2b6f8b8b1722733d5a005d02e25b92d0e2ba28d612b7f69409c5463a65433
 * </pre>
 */
class WorldGenMoundTemplateTest {
    /** The checksum of the table of the oracle, see scripts/porting/extract_mound_template.py. */
    private static final String ORACLE_SHA256 =
            "93a2b6f8b8b1722733d5a005d02e25b92d0e2ba28d612b7f69409c5463a65433";
    /** The number of blocks the oracle writes into the template. */
    private static final int ORACLE_BLOCKS = 2441;

    @Test void theTemplateIsTheTableOfTheOracle() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder canonical = new StringBuilder();
        for (int layer = 0; layer < WorldGenMoundTemplate.LAYERS; layer++) {
            for (int z = 0; z < WorldGenMoundTemplate.SIZE; z++) {
                for (int x = 0; x < WorldGenMoundTemplate.SIZE; x++) {
                    canonical.append(WorldGenMoundTemplate.cell(layer, z, x));
                }
            }
            canonical.append('\n');
        }
        String sha256 = HexFormat.of().formatHex(
                digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(ORACLE_SHA256, sha256,
                "the native table must be the table of the oracle, rebuild it with "
                        + "scripts/porting/extract_mound_template.py");
    }

    @Test void theTemplateHasTheShapeOfTheOriginal() {
        assertEquals(19, WorldGenMoundTemplate.SIZE);
        assertEquals(16, WorldGenMoundTemplate.LAYERS);
        assertEquals(ORACLE_BLOCKS, WorldGenMoundTemplate.blockCount());
    }

    @Test void theCellsCarryTheBlocksOfTheOriginal() {
        // Oracle WorldGenMound:67 - the dirt of the shell.
        assertEquals(Blocks.DIRT, WorldGenMoundTemplate.block(8, 4, 0));
        // Oracle WorldGenMound:71 - the cobblestone of the walls.
        assertEquals(Blocks.COBBLESTONE, WorldGenMoundTemplate.block(8, 8, 0));
        // Oracle WorldGenMound:141 - the mossy cobblestone of the walls.
        assertEquals(Blocks.MOSSY_COBBLESTONE, WorldGenMoundTemplate.block(8, 9, 2));
        // Oracle WorldGenMound:78 - the grass of the shell.
        assertEquals(Blocks.GRASS_BLOCK, WorldGenMoundTemplate.block(9, 4, 0));
        // Oracle WorldGenMound:271 - the iron bars of the windows.
        assertEquals(Blocks.IRON_BARS, WorldGenMoundTemplate.block(9, 9, 3));
        // Oracle WorldGenMound:182 - the chiselled stone bricks of the altar.
        assertEquals(Blocks.CHISELED_STONE_BRICKS, WorldGenMoundTemplate.block(11, 9, 2));
        // Oracle WorldGenMound:1860 - the ladder of the shaft.
        assertEquals(Blocks.LADDER, WorldGenMoundTemplate.block(1, 9, 7));
        // Oracle WorldGenMound:454 - the stone stairs keep their facing and their half.
        assertEquals(Blocks.STONE_STAIRS, WorldGenMoundTemplate.block(11, 6, 5));
        // Oracle WorldGenMound:1649 - the tall grass of the shell.
        assertEquals(Blocks.SHORT_GRASS, WorldGenMoundTemplate.block(10, 4, 0));
        // The cells the original does not write stay empty.
        assertNull(WorldGenMoundTemplate.block(0, 0, 0));
        assertNull(WorldGenMoundTemplate.block(15, 0, 0));
    }

    @Test void theStairsKeepTheirOriginalMetadata() {
        // Oracle WorldGenMound:516 - the metadata of the stone stairs is the facing and
        // the half of the 1.7.10 metadata.
        var east = WorldGenMoundTemplate.stateFor('0');
        var west = WorldGenMoundTemplate.stateFor('1');
        var south = WorldGenMoundTemplate.stateFor('2');
        var north = WorldGenMoundTemplate.stateFor('3');
        assertEquals(net.minecraft.core.Direction.EAST, east.getValue(net.minecraft.world.level.block.StairBlock.FACING));
        assertEquals(net.minecraft.core.Direction.WEST, west.getValue(net.minecraft.world.level.block.StairBlock.FACING));
        assertEquals(net.minecraft.core.Direction.SOUTH, south.getValue(net.minecraft.world.level.block.StairBlock.FACING));
        assertEquals(net.minecraft.core.Direction.NORTH, north.getValue(net.minecraft.world.level.block.StairBlock.FACING));
        assertEquals(net.minecraft.world.level.block.state.properties.Half.BOTTOM,
                east.getValue(net.minecraft.world.level.block.StairBlock.HALF));
        assertEquals(net.minecraft.world.level.block.state.properties.Half.TOP,
                WorldGenMoundTemplate.stateFor('4').getValue(net.minecraft.world.level.block.StairBlock.HALF));
    }

    @Test void theLadderKeepsItsOriginalFacing() {
        // Oracle WorldGenMound:1860 - the ladder of the original has the metadata five,
        // which is the ladder that is bound to the block in the west.
        var ladder = WorldGenMoundTemplate.stateFor('L');
        assertEquals(net.minecraft.core.Direction.EAST,
                ladder.getValue(net.minecraft.world.level.block.LadderBlock.FACING));
    }

    @Test void theLootRoomKeepsTheRollsOfTheOriginal() {
        // Oracle WorldGenMound:2520 - the rarity of a container: rare below a tenth,
        // ordinary below a third and common above that.
        boolean[] seen = new boolean[3];
        var random = net.minecraft.util.RandomSource.create(2024L);
        for (int roll = 0; roll < 3000; ++roll) {
            int rarity = WorldGenMound.lootRarity(random);
            assertTrue(rarity >= 0 && rarity <= 2, "the rarity of a container: " + rarity);
            seen[rarity] = true;
        }
        assertTrue(seen[0] && seen[1] && seen[2], "all three rarities of the original are reachable");
    }
}
