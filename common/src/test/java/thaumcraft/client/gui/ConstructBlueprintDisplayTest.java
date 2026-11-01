package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.client.gui.ConstructBlueprintDisplay.Part;
import thaumcraft.common.lib.ArcaneFurnaceFormation;
import thaumcraft.common.lib.NodeJarFormation;

/** Original ConfigRecipes:83-95, cross-checked against the native formation contracts. */
class ConstructBlueprintDisplayTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void theSmallConstructsRetainTheirOriginalInstructionsAndEmptyCell() {
        var book = ConstructBlueprintDisplay.blueprint("Thaumonomicon");
        var arcane = ConstructBlueprintDisplay.blueprint("ArcTable");
        var crucible = ConstructBlueprintDisplay.blueprint("Crucible");
        for (var blueprint : List.of(book, arcane, crucible)) {
            assertEquals(1, blueprint.width()); assertEquals(2, blueprint.height()); assertEquals(1, blueprint.depth());
            assertEquals(Part.WAND, blueprint.at(0, 0, 0));
            assertTrue(blueprint.vis().isEmpty(), "the original three click conversions are free");
        }
        assertEquals(Part.BOOKSHELF, book.at(1, 0, 0));
        assertEquals(Part.TABLE, arcane.at(1, 0, 0));
        assertEquals(Part.CAULDRON, crucible.at(1, 0, 0), "the source Items.field_151066_bu is the cauldron, never a water bucket");
        var research = ConstructBlueprintDisplay.blueprint("ResTable");
        assertEquals(1, research.width()); assertEquals(2, research.height()); assertEquals(2, research.depth());
        assertEquals(Part.NONE, research.at(0, 0, 1), "the source leading null is omitted, not a required-air item");
        assertEquals(Part.INKWELL, research.at(0, 0, 0));
        assertEquals(Part.TABLE, research.at(1, 0, 1)); assertEquals(Part.TABLE, research.at(1, 0, 0));
    }

    @Test void theInfernalExampleIsAnActuallyBuildableNativeShellWithTheOriginalSingleBar() {
        var blueprint = ConstructBlueprintDisplay.blueprint("InfernalFurnace");
        assertEquals(Map.of("ignis", 50, "terra", 50), blueprint.vis());
        Map<Part, Block> blocks = Map.of(Part.NETHER_BRICKS, Blocks.NETHER_BRICKS, Part.OBSIDIAN, Blocks.OBSIDIAN,
                Part.AIR, Blocks.AIR, Part.LAVA, Blocks.LAVA, Part.IRON_BARS, Blocks.IRON_BARS);
        int bars = 0;
        for (int layer = 0; layer < 3; layer++) for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            Part part = blueprint.at(layer, x, z);
            if (part == Part.IRON_BARS) {
                bars++;
                assertEquals(1, layer); assertEquals(0, x); assertEquals(1, z, "the original west-facing example slot");
            } else assertSame(ArcaneFurnaceFormation.expected(layer, x, z), blocks.get(part),
                    "every other displayed cell must satisfy the actual wand formation: " + layer + ":" + x + ":" + z);
        }
        assertEquals(1, bars);
    }

    @Test void theNodeJarShowsExactlyTheShellAndPriceConsumedByNativeCapture() {
        var blueprint = ConstructBlueprintDisplay.blueprint("NodeJar");
        assertEquals(3, blueprint.width()); assertEquals(4, blueprint.height()); assertEquals(3, blueprint.depth());
        assertEquals(NodeJarFormation.FORM_VIS, blueprint.vis());
        assertEquals(9, blueprint.cells().stream().filter(part -> part == Part.WOODEN_SLAB).count());
        assertEquals(26, blueprint.cells().stream().filter(part -> part == Part.GLASS).count());
        assertEquals(1, blueprint.cells().stream().filter(part -> part == Part.NODE).count());
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) assertEquals(Part.WOODEN_SLAB, blueprint.at(0, x, z));
        assertEquals(Part.NODE, blueprint.at(2, 1, 1), "source index22 is the node in the third layer down, not the geometric four-layer midpoint");
    }

    @Test void theIsometricCellCoordinatesPreserveOriginalLayerOrderAndFourLayerCompression() {
        var small = ConstructBlueprintDisplay.blueprint("Thaumonomicon");
        assertEquals(new ConstructBlueprintDisplay.CellPosition(48, 58, 1, 60), ConstructBlueprintDisplay.position(small, 0, 0, 0));
        assertEquals(new ConstructBlueprintDisplay.CellPosition(48, 108, 1, 50), ConstructBlueprintDisplay.position(small, 1, 0, 0));
        var furnace = ConstructBlueprintDisplay.blueprint("InfernalFurnace");
        assertEquals(new ConstructBlueprintDisplay.CellPosition(80, 33, 1, 60), ConstructBlueprintDisplay.position(furnace, 0, 2, 2));
        assertEquals(new ConstructBlueprintDisplay.CellPosition(16, 133, 1, 40), ConstructBlueprintDisplay.position(furnace, 2, 0, 0));
        var jar = ConstructBlueprintDisplay.blueprint("NodeJar");
        var center = ConstructBlueprintDisplay.position(jar, 2, 1, 1);
        assertEquals(44.8F, center.x(), .00001F); assertEquals(108, center.y(), .00001F);
        assertEquals(.8F, center.scale(), .00001F); assertEquals(40, center.depth());
        assertFalse(ConstructBlueprintDisplay.supports("not-an-original-blueprint"));
    }
}
