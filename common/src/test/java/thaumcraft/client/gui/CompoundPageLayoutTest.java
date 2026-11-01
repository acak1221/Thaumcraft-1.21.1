package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.AdvancedFurnaceRules;
import thaumcraft.common.lib.ArcaneFurnaceFormation;
import thaumcraft.common.lib.ThaumatoriumRules;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

/**
 * The COMPOUND_CRAFTING page of the original: where the diagram of a wand construct is drawn, how its list is read and
 * the nine constructs of the original book.
 *
 * <p>Original 4.2.3.5 {@code GuiResearchRecipe.drawCompoundCraftingPage} and
 * {@code ConfigRecipes.initializeCompoundRecipes}; the numbers below are worked out from the formulas of the original
 * quoted in {@link CompoundLayout}. Nothing here needs a running game: the drawing itself is checked in a client.</p>
 */
class CompoundPageLayoutTest {
    /** The constructs of the original book and the Levitational Locomotive of Thaumic Tinkerer (5 by 1 by 1). */
    private static final List<CompoundLayout> SHAPES = List.of(new CompoundLayout(1, 2, 1), new CompoundLayout(1, 2, 2),
            new CompoundLayout(3, 3, 3), new CompoundLayout(3, 4, 3), new CompoundLayout(1, 3, 1), new CompoundLayout(3, 2, 3),
            new CompoundLayout(5, 1, 1));

    @Test void theAdvancedFurnaceIsDrawnWhereTheOriginalDrewIt() {
        CompoundLayout furnace = new CompoundLayout(3, 2, 3);
        assertEquals(16, furnace.xoff(), "64 - (3 * 16 + 3 * 16) / 2");
        assertEquals(-50, furnace.yoff(), "two layers raise the structure by 50");
        assertEquals(0, furnace.shrink());
        assertEquals(16, furnace.originX());
        assertEquals(58, furnace.originY(), "108 + yoff");
        assertEquals(-24, furnace.groundX(), "-8 - xoff: the plate starts at x -8 of the page");
        assertEquals(5, furnace.groundY(), "-119 + 0 + 12 + 12 + 100");
        assertEquals(63, furnace.originY() + furnace.groundY(), "the ground plate at y + 63 of the page, as audit A-026-F03 measured");
    }

    @Test void theCellsAreReadLayerByLayerFromTheBackRowAndTheBackColumn() {
        List<CompoundLayout.Cell> cells = new CompoundLayout(3, 2, 3).cellsInOrder();
        assertEquals(18, cells.size());
        // The first entry of the list: top layer, row 2, column 2, the right corner of the plan.
        assertEquals(new CompoundLayout.Cell(0, 0, 2, 2, 64, 0), cells.get(0));
        // The fifth entry is the centre of the top layer (the "Empty Space" of the advanced furnace).
        assertEquals(new CompoundLayout.Cell(4, 0, 1, 1, 32, 0), cells.get(4));
        // The ninth entry: row 0, column 0, the left corner.
        assertEquals(new CompoundLayout.Cell(8, 0, 0, 0, 0, 0), cells.get(8));
        // The centre of the lower layer, fifty pixels lower (the alchemical furnace).
        assertEquals(new CompoundLayout.Cell(13, 1, 1, 1, 32, 50), cells.get(13));
        // Row 2, column 0 lies right and below of the left corner; row 0, column 2 right and above.
        assertEquals(new CompoundLayout.Cell(2, 0, 2, 0, 32, 16), cells.get(2));
        assertEquals(new CompoundLayout.Cell(6, 0, 0, 2, 32, -16), cells.get(6));
        assertEquals(60, cells.get(0).depth(), "the original depth of the top layer");
        assertEquals(50, cells.get(13).depth(), "ten less for the layer below");
    }

    @Test void aConstructOfMoreThanThreeLayersIsDrawnSmaller() {
        CompoundLayout jar = new CompoundLayout(3, 4, 3);
        assertEquals(.2F, jar.shrink(), 1e-6);
        assertEquals(.8F, jar.scale(), 1e-6);
        assertEquals(19.2F, jar.originX(), 1e-4, "xoff * (1 + sz)");
        assertEquals(28F, jar.originY(), 1e-4, "108 + yoff * (1 - sz)");
        CompoundLayout.Cell node = jar.cellsInOrder().get(22);
        assertEquals(2, node.layer(), "the node of the jar is the centre of the third layer");
        assertEquals(19.2F + 32 * .8F, jar.pageX(node), 1e-4);
        assertEquals(28F + 100 * .8F, jar.pageY(node), 1e-4);
        assertEquals(.2F, new CompoundLayout(1, 7, 1).scale(), 1e-6);
        assertEquals(CompoundLayout.MIN_SCALE, new CompoundLayout(1, 12, 1).scale(), 1e-6,
                "the original reached size zero at eight layers and mirrored above; the port stops at 0.2");
    }

    @Test void theLocomotiveIsADiagonalOfFiveCells() {
        CompoundLayout locomotive = new CompoundLayout(5, 1, 1);
        assertEquals(16, locomotive.xoff());
        assertEquals(83, locomotive.originY());
        List<CompoundLayout.Cell> cells = locomotive.cellsInOrder();
        assertEquals(List.of(64, 48, 32, 16, 0), cells.stream().map(CompoundLayout.Cell::x).toList());
        assertEquals(List.of(-32, -24, -16, -8, 0), cells.stream().map(CompoundLayout.Cell::y).toList());
        assertEquals(54, locomotive.originY() + locomotive.groundY(), "-119 + 2 * 8 + 20 + 4 + 50 below the origin");
    }

    @Test void everyConstructIsCentredOnTheTitleAndFitsTheFrame() {
        for (CompoundLayout layout : SHAPES) {
            float left = Float.MAX_VALUE, right = -Float.MAX_VALUE, top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
            for (CompoundLayout.Cell cell : layout.cellsInOrder()) {
                left = Math.min(left, layout.pageX(cell));
                right = Math.max(right, layout.pageX(cell) + 16 * layout.scale());
                top = Math.min(top, layout.pageY(cell));
                bottom = Math.max(bottom, layout.pageY(cell) + 16 * layout.scale());
            }
            if (layout.shrink() == 0) assertEquals(CompoundLayout.TITLE_CENTER, (left + right) / 2, 1e-4, layout + " centred on x 56");
            float groundLeft = layout.originX() + layout.groundX() * layout.scale();
            float groundTop = layout.originY() + layout.groundY() * layout.scale();
            float groundRight = groundLeft + CompoundLayout.GROUND_WIDTH * layout.scale();
            float groundBottom = groundTop + CompoundLayout.GROUND_HEIGHT * layout.scale();
            assertTrue(Math.min(left, groundLeft) >= CompoundLayout.FRAME_LEFT, layout + " left edge");
            assertTrue(Math.max(right, groundRight) <= CompoundLayout.FRAME_LEFT + CompoundLayout.FRAME_WIDTH, layout + " right edge");
            assertTrue(top >= 10, layout + " below the title");
            assertTrue(Math.max(bottom, groundBottom) <= CompoundLayout.FRAME_HEIGHT, layout + " bottom edge");
            assertTrue(bottom <= CompoundLayout.ASPECT_Y, layout + " above the aspect row");
        }
        for (int count = 1; count <= 6; count++) {
            assertTrue(CompoundLayout.aspectX(0, count) >= CompoundLayout.FRAME_LEFT);
            assertTrue(CompoundLayout.aspectX(count - 1, count) + 16 <= CompoundLayout.FRAME_LEFT + CompoundLayout.FRAME_WIDTH);
        }
        assertTrue(CompoundLayout.ASPECT_Y + 16 <= CompoundLayout.FRAME_HEIGHT);
    }

    @Test void theAspectRowIsTheOriginalOne() {
        // 14 + 18 * n + (5 - count) * 8
        assertEquals(List.of(6, 24, 42, 60, 78, 96), List.of(0, 1, 2, 3, 4, 5).stream().map(n -> CompoundLayout.aspectX(n, 6)).toList());
        assertEquals(List.of(30, 48, 66), List.of(0, 1, 2).stream().map(n -> CompoundLayout.aspectX(n, 3)).toList());
        assertEquals(182, CompoundLayout.ASPECT_Y);
    }

    @Test void theHoveredCellIsTheLastShownCellUnderThePoint() {
        CompoundLayout furnace = new CompoundLayout(3, 2, 3);
        // The centre of the top layer: x 16 + 32, y 58 + 0.
        assertEquals(4, furnace.hit(48 + 8, 58 + 8, index -> true));
        assertEquals(-1, furnace.hit(48 + 8, 58 + 8, index -> index != 4), "a null entry has no tooltip");
        assertEquals(-1, furnace.hit(-100, -100, index -> true));
        assertEquals(13, furnace.hit(48 + 8, 108 + 8, index -> true), "the centre of the lower layer");
        // The cells of one layer never overlap, but the layers of a construct four blocks wide and deep do: the front corner
        // of the top layer (entry 3) and the back corner of the layer below (entry 28) share pixels, and the last one wins.
        CompoundLayout wide = new CompoundLayout(4, 2, 4);
        List<CompoundLayout.Cell> cells = wide.cellsInOrder();
        assertEquals(new CompoundLayout.Cell(3, 0, 3, 0, 48, 24), cells.get(3));
        assertEquals(new CompoundLayout.Cell(28, 1, 0, 3, 48, 26), cells.get(28));
        assertEquals(28, wide.hit(50, 58 + 32, index -> true));
        assertEquals(3, wide.hit(50, 58 + 32, index -> index != 28));
    }

    @Test void onlyTheSizesThePortCanDrawAreAccepted() {
        assertTrue(CompoundLayout.valid(1, 1, 1));
        assertTrue(CompoundLayout.valid(16, 16, 16));
        assertFalse(CompoundLayout.valid(0, 1, 1));
        assertFalse(CompoundLayout.valid(1, -1, 1));
        assertFalse(CompoundLayout.valid(33, 1, 1));
        assertFalse(CompoundLayout.valid(32, 32, 32), "more than 4096 cells");
        assertThrows(IllegalArgumentException.class, () -> new CompoundLayout(0, 0, 0));
    }

    @Test void theListOfTheOriginalIsReadAsTheOriginalReadIt() {
        AspectList aspects = new AspectList().add(Aspect.FIRE, 50);
        List<Object> cells = new ArrayList<>(Arrays.asList("a", null, "c", null, "e"));
        List<Object> recipe = Arrays.asList(aspects, 5, 1, 1, cells);
        CompoundPage page = CompoundPage.parse(recipe);
        assertNotNull(page);
        assertSame(aspects, page.aspects());
        assertEquals(new CompoundLayout(5, 1, 1), page.layout());
        assertSame(cells, page.entries(), "kept by reference, like the original");
        assertSame(recipe, page.source());
        assertEquals("e", page.entry(4));
        assertNull(page.entry(5), "a list shorter than the construct leaves the rest empty");
        assertNull(page.entry(-1));
        // An empty AspectList and a null one draw no aspect row; both are accepted.
        assertNotNull(CompoundPage.parse(Arrays.asList(new AspectList(), 1, 2, 1, List.of())));
        assertNotNull(CompoundPage.parse(Arrays.asList(null, 1, 2, 1, List.of())));
    }

    @Test void aListOfAnotherShapeIsNotAPage() {
        assertNull(CompoundPage.parse(null));
        assertNull(CompoundPage.parse("not a list"));
        assertNull(CompoundPage.parse(List.of(new AspectList(), 1, 2, 1)), "four elements");
        assertNull(CompoundPage.parse(Arrays.asList("aspects", 1, 2, 1, List.of())), "not an AspectList");
        assertNull(CompoundPage.parse(Arrays.asList(new AspectList(), 1L, 2, 1, List.of())), "the original cast (Integer)");
        assertNull(CompoundPage.parse(Arrays.asList(new AspectList(), 1, 2, 1, "cells")), "not a list of cells");
        assertNull(CompoundPage.parse(Arrays.asList(new AspectList(), 0, 2, 1, List.of())), "no width");
        assertNull(CompoundPage.parse(Arrays.asList(new AspectList(), 64, 64, 64, List.of())), "too large to draw");
    }

    @Test void theBookHasTheNineConstructsOfTheOriginal() {
        Map<String, CompoundConstructs.Definition> all = CompoundConstructs.all();
        assertEquals(List.of("Thaumonomicon", "ArcTable", "ResTable", "Crucible", "InfernalFurnace", "InfusionAltar", "NodeJar",
                "Thaumatorium", "AdvAlchemyFurnace"), List.copyOf(all.keySet()), "ConfigRecipes.initializeCompoundRecipes, in its order");
        assertEquals(ResearchRecipeCoverage.constructBlueprints().keySet(), Set.copyOf(all.keySet()),
                "every construct key of the research corpus is drawn");
        for (var entry : all.entrySet()) {
            var definition = entry.getValue();
            assertEquals(definition.layout().cells(), definition.cells().size(), entry.getKey() + " has one entry per cell");
        }
    }

    @Test void theSizesOfTheConstructsAreTheOriginalOnes() {
        Map<String, CompoundConstructs.Definition> all = CompoundConstructs.all();
        assertEquals(new CompoundLayout(1, 2, 1), all.get("Thaumonomicon").layout());
        assertEquals(new CompoundLayout(1, 2, 1), all.get("ArcTable").layout());
        assertEquals(new CompoundLayout(1, 2, 2), all.get("ResTable").layout());
        assertEquals(new CompoundLayout(1, 2, 1), all.get("Crucible").layout());
        assertEquals(new CompoundLayout(3, 3, 3), all.get("InfernalFurnace").layout());
        assertEquals(new CompoundLayout(3, 3, 3), all.get("InfusionAltar").layout());
        assertEquals(new CompoundLayout(3, 4, 3), all.get("NodeJar").layout());
        assertEquals(new CompoundLayout(1, 3, 1), all.get("Thaumatorium").layout());
        assertEquals(new CompoundLayout(3, 2, 3), all.get("AdvAlchemyFurnace").layout());
    }

    @Test void theBlocksOfTheConstructsAreTheOriginalOnes() {
        Map<String, CompoundConstructs.Definition> all = CompoundConstructs.all();
        String wand = CompoundConstructs.WAND, empty = CompoundConstructs.EMPTY;
        assertEquals(List.of(wand, "minecraft:bookshelf"), all.get("Thaumonomicon").cells());
        assertEquals(List.of(wand, "thaumcraft:table"), all.get("ArcTable").cells());
        assertEquals(Arrays.asList(null, "thaumcraft:inkwell", "thaumcraft:table", "thaumcraft:table"), all.get("ResTable").cells());
        assertEquals(List.of(wand, "minecraft:cauldron"), all.get("Crucible").cells());
        String alembic = "thaumcraft:alembic", construct = "thaumcraft:alchemical_construct", advanced = "thaumcraft:advanced_alchemical_construct";
        assertEquals(List.of(alembic, construct, alembic, construct, empty, construct, alembic, construct, alembic,
                advanced, advanced, advanced, advanced, "thaumcraft:alchemy_furnace", advanced, advanced, advanced, advanced),
                all.get("AdvAlchemyFurnace").cells());
        assertEquals(List.of(construct, construct, "thaumcraft:crucible"), all.get("Thaumatorium").cells());
        List<String> altar = all.get("InfusionAltar").cells();
        assertEquals("thaumcraft:infusion_matrix", altar.get(4), "the matrix floats in the middle of the top layer");
        assertEquals("thaumcraft:arcane_pedestal", altar.get(22), "the pedestal in the middle of the ground layer");
        for (int corner : new int[] {0, 2, 6, 8}) {
            assertEquals(empty, altar.get(corner), "the space above the pillars stays empty");
            assertEquals("thaumcraft:arcane_stone", altar.get(9 + corner));
            assertEquals("thaumcraft:arcane_stone_bricks", altar.get(18 + corner));
        }
        assertEquals(13, altar.stream().filter(java.util.Objects::isNull).count(), "the other cells are null");
        List<String> furnace = all.get("InfernalFurnace").cells();
        assertEquals(empty, furnace.get(4), "the top of the furnace is open");
        assertEquals("minecraft:lava_bucket", furnace.get(13), "the lava source, which 1.21.1 has no item for");
        assertEquals("minecraft:iron_bars", furnace.get(14));
        assertEquals(12, furnace.stream().filter("minecraft:nether_bricks"::equals).count(), "the corners and edges of every layer");
        assertEquals(12, furnace.stream().filter("minecraft:obsidian"::equals).count());
        List<String> jar = all.get("NodeJar").cells();
        assertEquals(List.of("ore:slabWood"), jar.subList(0, 9).stream().distinct().toList(), "a lid of wooden slabs");
        assertEquals("thaumcraft:aura_node", jar.get(22), "the node in the middle of the jar");
        assertEquals(26, jar.stream().filter("minecraft:glass"::equals).count());
    }

    @Test void theVisOfTheConstructsIsWhatTheWandSpends() {
        Map<String, CompoundConstructs.Definition> all = CompoundConstructs.all();
        assertEquals(ArcaneFurnaceFormation.FORM_VIS, all.get("InfernalFurnace").aspects());
        assertEquals(ThaumatoriumRules.FORM_VIS, all.get("Thaumatorium").aspects());
        assertEquals(AdvancedFurnaceRules.FORM_VIS, all.get("AdvAlchemyFurnace").aspects());
        assertEquals(List.of("ignis", "terra", "ordo", "aer", "perditio", "aqua"), List.copyOf(all.get("InfusionAltar").aspects().keySet()),
                "the order of the original AspectList");
        assertEquals(Set.of(25), Set.copyOf(all.get("InfusionAltar").aspects().values()));
        assertEquals(Set.of(70), Set.copyOf(all.get("NodeJar").aspects().values()));
        assertEquals(6, all.get("NodeJar").aspects().size());
        for (String free : List.of("Thaumonomicon", "ArcTable", "ResTable", "Crucible")) {
            assertTrue(all.get(free).aspects().isEmpty(), free + " costs no vis and draws no aspect row");
        }
        for (var definition : all.values()) {
            for (String tag : definition.aspects().keySet()) assertNotNull(Aspect.getAspect(tag), tag);
        }
    }

    @Test void everyThaumcraftBlockOfTheConstructsIsAnItemOfThePort() throws Exception {
        Path models = Path.of("src/main/resources/assets/thaumcraft/models/item");
        if (!Files.isDirectory(models)) models = Path.of("common").resolve(models);
        for (var definition : CompoundConstructs.all().values()) {
            for (String cell : definition.cells()) {
                if (cell == null || !cell.startsWith("thaumcraft:")) continue;
                assertTrue(Files.isRegularFile(models.resolve(cell.substring("thaumcraft:".length()) + ".json")), cell + " has an item model");
            }
        }
    }
}
