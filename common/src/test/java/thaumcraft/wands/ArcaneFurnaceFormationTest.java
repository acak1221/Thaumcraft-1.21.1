package thaumcraft.wands;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.ArcaneFurnaceFormation;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure rules of the original event two wand construct.
 *
 * <p>The oracle is the byte exact 4.2.3.5 decompilation:
 * {@code thaumcraft/common/items/wands/WandManager.java:332-407} builds the
 * arcane furnace, {@code thaumcraft/common/config/ConfigRecipes.java:90-92}
 * registers the trigger blocks and the research page of the construct, and
 * {@code thaumcraft/common/config/ConfigRecipes.java:89} names the fifty vis of
 * fire and fifty vis of earth the wand pays.</p>
 *
 * <p>The test never constructs a block of the port: it only reads the vanilla
 * blueprint of the original and the vis price of the construct.</p>
 */
class ArcaneFurnaceFormationTest {
    @Test void theConstructPaysFiftyVisOfFireAndFiftyVisOfEarth() {
        // Oracle WandManager.java:341 and ConfigRecipes.java:89.
        assertEquals(50, ArcaneFurnaceFormation.FORM_VIS.get("ignis"));
        assertEquals(50, ArcaneFurnaceFormation.FORM_VIS.get("terra"));
        assertEquals(2, ArcaneFurnaceFormation.FORM_VIS.size());
    }

    @Test void theShellOfTheOriginalIsNetherBricksObsidianLavaAndAir() {
        // Oracle WandManager.java:351-356.
        for (int layer = 0; layer < 3; layer++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    boolean corner = (dx == 0 || dx == 2) && (dz == 0 || dz == 2);
                    if (dx == 1 && dz == 1) {
                        assertSame(Blocks.AIR, ArcaneFurnaceFormation.expected(layer, dx, dz),
                                "the middle of the top layer of the original shell is empty");
                        assertSame(Blocks.LAVA, ArcaneFurnaceFormation.expected(1, dx, dz),
                                "the middle of the middle layer of the original shell is lava");
                        assertSame(Blocks.OBSIDIAN, ArcaneFurnaceFormation.expected(2, dx, dz),
                                "the middle of the bottom layer of the original shell is obsidian");
                        continue;
                    }
                    assertSame(corner ? Blocks.NETHER_BRICKS : Blocks.OBSIDIAN,
                            ArcaneFurnaceFormation.expected(layer, dx, dz),
                            "the corners of the original shell are nether bricks and the edges are obsidian");
                }
            }
        }
    }

    @Test void theThreeTriggerBlocksOfTheEventTwoAreTheOriginalOnes() {
        // Oracle ConfigRecipes.java:91-92.
        assertTrue(ArcaneFurnaceFormation.isTriggerBlock(Blocks.NETHER_BRICKS.defaultBlockState()));
        assertTrue(ArcaneFurnaceFormation.isTriggerBlock(Blocks.OBSIDIAN.defaultBlockState()));
        assertTrue(ArcaneFurnaceFormation.isTriggerBlock(Blocks.IRON_BARS.defaultBlockState()));
        assertFalse(ArcaneFurnaceFormation.isTriggerBlock(Blocks.LAVA.defaultBlockState()));
        assertFalse(ArcaneFurnaceFormation.isTriggerBlock(Blocks.STONE.defaultBlockState()));
    }
}
