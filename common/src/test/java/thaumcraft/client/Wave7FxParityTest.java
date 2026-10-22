package thaumcraft.client;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.client.gui.HoveredSlotAspectRow;
import thaumcraft.client.lib.ScanOverlay;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.entity.RenderWatcher;

import static org.junit.jupiter.api.Assertions.*;

/** Oracle values only; no registry objects, worlds, GL context or Block construction.
 * Authored for the leader's JUnit run; not executed by the w7fx agent. */
class Wave7FxParityTest {
    @Test void scanRetainsEvenTheOriginalNegativeFadeAndCornerFlash() {
        // RenderEventHandler:367,427-435; ItemElementalPickaxe:89.
        assertEquals(17, ScanOverlay.SIZE);
        assertEquals(5000, ScanOverlay.DURATION_MILLIS);
        assertEquals(-49F, ScanOverlay.alpha(5000));
        assertEquals(0F, ScanOverlay.alpha(4755));
        assertEquals(.8F, ScanOverlay.alpha(4751), 1e-6F);
        assertEquals(1F, ScanOverlay.alpha(4750));
        assertEquals(1F, ScanOverlay.alpha(1500));
        assertEquals(.5F, ScanOverlay.alpha(750));
        assertEquals(0F, ScanOverlay.alpha(0));
        assertEquals(.75F, ScanOverlay.distanceAlpha(4, 0, 0));
        assertEquals(0F, ScanOverlay.distanceAlpha(8, 0, 0));
        assertEquals(-2F, ScanOverlay.distanceAlpha(8, 8, 8));
        assertEquals(98F, ScanOverlay.cellAlpha(5000, 8, 8, 8));
    }

    @Test void scanColoursFramesScalesAndPackedLightAreOriginal() {
        // RenderEventHandler:436-451,467-470,498; UtilsFX:209.
        assertEquals(3986684, ScanOverlay.specialColour(-5));
        assertEquals(16734721, ScanOverlay.specialColour(-10));
        assertEquals(-1, ScanOverlay.specialColour(-1));
        assertEquals(0xAAAA11, ScanOverlay.INNER_COLOUR);
        assertEquals(0xAA1122, ScanOverlay.OUTER_COLOUR);
        assertEquals(.2F, ScanOverlay.innerScale(7));
        assertEquals(.5F, ScanOverlay.outerScale(7));
        assertEquals(.4F, ScanOverlay.innerScale(14));
        assertEquals(0, ScanOverlay.frame(49));
        assertEquals(1, ScanOverlay.frame(50));
        assertEquals(31, ScanOverlay.frame(1599));
        assertEquals(0, ScanOverlay.frame(1600));
        assertEquals(220, ScanOverlay.ORE_LIGHT);
        assertEquals(200, ScanOverlay.FLUID_LIGHT);
        assertTrue(ScanOverlay.isDrawn(-5));
        assertTrue(ScanOverlay.isDrawn(-10));
        assertTrue(ScanOverlay.isDrawn(0));
        assertFalse(ScanOverlay.isDrawn(-1));
    }

    @Test void hoveredRowUsesOriginalXorOffsetsAndBothScanPrefixes() {
        // ClientTickEventsFML:301-309,678-710; native ScanCallbacks:81 replaces hashes by registry IDs.
        assertFalse(HoveredSlotAspectRow.shouldShow(false, false));
        assertTrue(HoveredSlotAspectRow.shouldShow(true, false));
        assertTrue(HoveredSlotAspectRow.shouldShow(false, true));
        assertFalse(HoveredSlotAspectRow.shouldShow(true, true));
        assertEquals(109, HoveredSlotAspectRow.iconX(100, 0) + HoveredSlotAspectRow.rowShift());
        assertEquals(127, HoveredSlotAspectRow.iconX(100, 1) + HoveredSlotAspectRow.rowShift());
        assertEquals(66, HoveredSlotAspectRow.iconY(100) + HoveredSlotAspectRow.rowShift());
        assertEquals(107, HoveredSlotAspectRow.backgroundX(HoveredSlotAspectRow.iconX(100, 0)));
        assertEquals(64, HoveredSlotAspectRow.backgroundY(HoveredSlotAspectRow.iconY(100)));
        assertEquals(1.25F, HoveredSlotAspectRow.BACK_SCALE);
        assertEquals(771, HoveredSlotAspectRow.ROW_BLEND);
        assertEquals(1F, HoveredSlotAspectRow.ROW_ALPHA);
        assertTrue(HoveredSlotAspectRow.isScanned(Set.of("@minecraft:stone"), "minecraft:stone"));
        assertTrue(HoveredSlotAspectRow.isScanned(Set.of("#minecraft:stone"), "minecraft:stone"));
        assertFalse(HoveredSlotAspectRow.isScanned(Set.of("#@minecraft:stone"), "minecraft:stone"));
        assertFalse(HoveredSlotAspectRow.isScanned(Set.of(), "minecraft:stone"));
    }

    @Test void textureHeaderReadsTheSameDimensionsAsOriginalImageIo() throws IOException {
        // UtilsFX:469-507. Original retained assets: ring4096x256, wisp256x256.
        try (var ring = getClass().getResourceAsStream("/assets/thaumcraft/textures/items/lightningringv.png");
             var wisp = getClass().getResourceAsStream("/assets/thaumcraft/textures/misc/wisp.png")) {
            assertNotNull(ring);
            assertNotNull(wisp);
            int[] ringSize = UtilsFX.pngSize(ring.readNBytes(24));
            int[] wispSize = UtilsFX.pngSize(wisp.readNBytes(24));
            assertArrayEquals(new int[]{4096, 256}, ringSize);
            assertArrayEquals(new int[]{256, 256}, wispSize);
            assertEquals(16, ringSize[0] / ringSize[1]);
            assertEquals(4, wispSize[0] / 64);
        }
        assertThrows(IOException.class, () -> UtilsFX.pngSize(new byte[24]));
        assertEquals(.249375F, UtilsFX.wispUvSpan(4), 1e-7F);
        assertEquals(.2496875F, UtilsFX.wispUvSpan(8), 1e-7F);
    }

    @Test void equipUsesTheOriginalPointFourTickClampNotAttackStrength() {
        // Official Mojang 1.7.10 bly.a:518-544 (MCP updateEquippedItem).
        assertEquals(.4F, UtilsFX.equipStep(0, true));
        assertEquals(.8F, UtilsFX.equipStep(.4F, true));
        assertEquals(1F, UtilsFX.equipStep(.8F, true));
        assertEquals(.6F, UtilsFX.equipStep(1, false));
        assertEquals(.2F, UtilsFX.equipStep(.6F, false), 1e-6F);
        assertEquals(0F, UtilsFX.equipStep(.2F, false));
        assertEquals(.1F, UtilsFX.EQUIP_RESET);
    }

    @Test void watcherIsStillUnreachableVisually() {
        // No RenderWatcher registration in oracle ClientProxy:464-517.
        assertFalse(RenderWatcher.drawsWatcher());
    }
}
