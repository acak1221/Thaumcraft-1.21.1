package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.ConstructBlueprintDisplay.Part;

/** An add-on's original ResearchPage(List) compound page: [AspectList, width, height, depth, cells]. */
class AddonConstructBlueprintTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void aRowOfFiveWithTwoGapsKeepsItsStacksAndShowsTheGapsAsEmptySpace() {
        // Thaumic Tinkerer's Levitational Locomotive: relay, empty space, locomotive, empty space, relay; no vis.
        ItemStack relay = new ItemStack(Items.STONE), locomotive = new ItemStack(Items.IRON_BLOCK);
        var blueprint = ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 5, 1, 1,
                Arrays.asList(relay, ItemStack.EMPTY, locomotive, ItemStack.EMPTY, relay)));
        assertNotNull(blueprint);
        assertEquals(5, blueprint.width()); assertEquals(1, blueprint.height()); assertEquals(1, blueprint.depth());
        assertTrue(blueprint.vis().isEmpty(), "an empty AspectList draws no aspect row");
        // The first entry of the list is the highest x of the row.
        assertEquals(List.of(Part.ITEM, Part.AIR, Part.ITEM, Part.AIR, Part.ITEM), blueprint.cells());
        assertSame(relay, blueprint.entry(0, 4, 0));
        assertSame(locomotive, blueprint.entry(0, 2, 0));
        assertEquals(Part.AIR, blueprint.at(0, 3, 0));
        // Source drawCompoundCraftingPage: xoff 16, yoff -25; the row climbs to the right.
        assertEquals(new ConstructBlueprintDisplay.CellPosition(80, 51, 1, 60), ConstructBlueprintDisplay.position(blueprint, 0, 4, 0));
        assertEquals(new ConstructBlueprintDisplay.CellPosition(16, 83, 1, 60), ConstructBlueprintDisplay.position(blueprint, 0, 0, 0));
    }

    @Test void theCellsOfTheListMapToTheBundledParts() {
        var blueprint = ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList().add(Aspect.FIRE, 50).add(Aspect.WATER, 25), 3, 2, 1,
                Arrays.asList(null, Blocks.AIR, Items.AIR, "slabWood", Blocks.GLASS)));
        assertNotNull(blueprint);
        assertEquals(Map.of("ignis", 50, "aqua", 25), blueprint.vis());
        assertEquals(List.of("ignis", "aqua"), List.copyOf(blueprint.vis().keySet()), "the order of the AspectList");
        assertEquals(List.of(Part.NONE, Part.AIR, Part.AIR, Part.ITEM, Part.ITEM, Part.NONE), blueprint.cells(),
                "null draws nothing, air is the empty space, and a list shorter than the construct leaves the rest empty");
        assertEquals("slabWood", blueprint.entry(1, 2, 0));
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList(null, 1, 1, 1, List.of())).entry(0, 0, 0));
        assertTrue(ConstructBlueprintDisplay.addon(Arrays.asList(null, 1, 1, 1, List.of())).vis().isEmpty());
    }

    @Test void anAddOnConstructUsesTheGeometryOfTheBundledPages() {
        var furnace = ConstructBlueprintDisplay.blueprint("InfernalFurnace");
        var addon = ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 3, 3, 3, List.of()));
        var jar = ConstructBlueprintDisplay.blueprint("NodeJar");
        var tall = ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 3, 4, 3, List.of()));
        for (int layer = 0; layer < 3; layer++) for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            assertEquals(ConstructBlueprintDisplay.position(furnace, layer, x, z), ConstructBlueprintDisplay.position(addon, layer, x, z));
            assertEquals(ConstructBlueprintDisplay.position(jar, layer, x, z), ConstructBlueprintDisplay.position(tall, layer, x, z));
        }
        assertTrue(furnace.entries().isEmpty(), "a bundled page has no list entries");
        assertNull(furnace.entry(0, 0, 0));
    }

    @Test void aListOfAnotherShapeIsNotAPage() {
        assertNull(ConstructBlueprintDisplay.addon(null));
        assertNull(ConstructBlueprintDisplay.addon("a list"));
        assertNull(ConstructBlueprintDisplay.addon(List.of(new AspectList(), 1, 2, 1)), "four elements");
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList("vis", 1, 2, 1, List.of())), "not an AspectList");
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 1L, 2, 1, List.of())), "the original cast (Integer)");
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 1, 2, 1, "cells")), "the cells are a list");
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 0, 2, 1, List.of())), "no width");
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 33, 1, 1, List.of())), "too wide to draw");
        assertNull(ConstructBlueprintDisplay.addon(Arrays.asList(new AspectList(), 32, 32, 32, List.of())), "too many cells");
    }
}
