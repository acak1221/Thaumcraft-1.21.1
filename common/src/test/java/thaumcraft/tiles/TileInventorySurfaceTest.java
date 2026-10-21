package thaumcraft.tiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.WorldlyContainer;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.lib.tiles.TileParityRules;

/**
 * The inventory surfaces of the block entities that the 4.2.3.5 oracle declares.
 *
 * <p>The surfaces are checked on the class, so the test needs no registry and no world; the
 * numbers of the surfaces are the ones of the oracle.
 */
class TileInventorySurfaceTest {

    /**
     * Original TileMirror:346-348 and :387-389 - the item mirror has one inventory slot with
     * a stack limit of sixty four, and it hands an inserted item to the twin mirror.
     */
    @Test void theItemMirrorHasTheOriginalSingleSlotInventory() throws Exception {
        Class<?> mirror = Class.forName("thaumcraft.common.tiles.TileMirror");
        assertTrue(WorldlyContainer.class.isAssignableFrom(mirror),
                "the original TileMirror implements IInventory, so a hopper may push an item into it");
        assertNotNull(mirror.getMethod("getContainerSize"));
        assertNotNull(mirror.getMethod("getMaxStackSize"));
        assertNotNull(mirror.getMethod("setItem", int.class, net.minecraft.world.item.ItemStack.class));
        assertNotNull(mirror.getMethod("isItemValidForSlot", int.class, net.minecraft.world.item.ItemStack.class));
        assertNotNull(mirror.getMethod("canPlaceItemThroughFace", int.class, net.minecraft.world.item.ItemStack.class,
                net.minecraft.core.Direction.class));
        assertEquals(1, TileParityRules.MIRROR_INVENTORY_SLOTS);
        assertEquals(64, TileParityRules.MIRROR_STACK_LIMIT);
    }

    /**
     * Original TileJarNode:110-131 - the node jar is an aspect container whose stored aspects
     * never grow past the stored base of the node.
     */
    @Test void theNodeJarIsAnAspectContainer() throws Exception {
        Class<?> jar = Class.forName("thaumcraft.common.tiles.TileJarNode");
        assertTrue(IAspectContainer.class.isAssignableFrom(jar),
                "the original TileJarNode implements IAspectContainer");
        assertNotNull(jar.getMethod("addToContainer", thaumcraft.api.aspects.Aspect.class, int.class));
        assertNotNull(jar.getMethod("takeFromContainer", thaumcraft.api.aspects.Aspect.class, int.class));
        assertNotNull(jar.getMethod("containerContains", thaumcraft.api.aspects.Aspect.class));
        assertNotNull(jar.getMethod("doesContainerContainAmount", thaumcraft.api.aspects.Aspect.class, int.class));
    }

    /** Original TileJarNode:118-127 - the jar only takes what fits above the base. */
    @Test void theNodeJarOnlyTakesWhatFitsAboveItsBase() {
        assertEquals(5, TileParityRules.jarNodeAccepted(0, 8, 5));
        assertEquals(8, TileParityRules.jarNodeAccepted(0, 8, 20));
        assertEquals(3, TileParityRules.jarNodeAccepted(5, 8, 20));
        assertEquals(0, TileParityRules.jarNodeAccepted(8, 8, 20));
        assertEquals(0, TileParityRules.jarNodeAccepted(12, 8, 20));
        assertEquals(0, TileParityRules.jarNodeAccepted(0, 8, 0));
        assertEquals(0, TileParityRules.jarNodeAccepted(0, 8, -3));
    }

    /** Original TileJarNode:129-138 - the jar gives an aspect back only when it holds enough. */
    @Test void theNodeJarGivesAnAspectBackOnlyWhenItHoldsEnough() {
        assertTrue(TileParityRules.jarNodeGives(8, 8));
        assertTrue(TileParityRules.jarNodeGives(9, 8));
        assertFalse(TileParityRules.jarNodeGives(7, 8));
        assertFalse(TileParityRules.jarNodeGives(8, 0));
        assertFalse(TileParityRules.jarNodeGives(8, -1));
    }

    /**
     * Original TileEldritchPortal - the tile stores nothing: the open counter and the tick
     * counter are runtime state and never reach the saved data.
     */
    @Test void theEldritchPortalKeepsItsCountersOutOfTheSavedData() throws Exception {
        String source = readSource("thaumcraft/common/tiles/TileEldritchPortal.java");
        assertFalse(source.contains("putInt(\"opencount\""), "the original portal never writes its open counter");
        assertFalse(source.contains("putInt(\"count\""), "the original portal never writes its tick counter");
        assertFalse(source.contains("getInt(\"opencount\""), "the original portal never reads an open counter");
        assertFalse(source.contains("getInt(\"count\""), "the original portal never reads a tick counter");
    }

    private static String readSource(String path) throws Exception {
        Path file = Path.of("src/main/java", path);
        assertTrue(Files.exists(file), "the source of the tile is missing: " + file);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
