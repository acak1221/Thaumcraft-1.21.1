package thaumcraft.crystals;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.common.lib.crystals.CrystalClusterGeometry;

class CrystalClusterGeometryTest {
    @Test void mixedContainsOriginalCentralOrderThenAirFireWaterEarthEntropy() {
        var mixed = CrystalClusterGeometry.pieces(6, 3, 5, 7, 0);
        assertEquals(List.of(0xeeccff, 0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0x555577), mixed.stream().map(CrystalClusterGeometry.Piece::color).toList());
        for (int metadata = 0; metadata < 6; metadata++) {
            var primal = CrystalClusterGeometry.pieces(metadata, 3, 5, 7, 0);
            assertEquals(6, primal.size()); for (var piece : primal) assertEquals(CrystalClusterGeometry.color(metadata), piece.color());
        }
    }
    @Test void originalJavaRandomScheduleKeepsPinnedAnglesAndScales() {
        // Oracle TileCrystalRenderer Java Random seed6+3+5*7=44; explicit reference evaluation retained in .tmp/OracleCrystalVector.java.
        var pieces = CrystalClusterGeometry.pieces(6, 3, 5, 7, 0); var first = pieces.getFirst();
        assertEquals(.29083818F, first.yaw()); assertEquals(-.77008486F, first.pitch());
        assertEquals(.2450876F, first.scaleX()); assertEquals(.65547615F, first.scaleY()); assertEquals(.21377471F, first.scaleZ());
        assertEquals(List.of(92F, 158F, 234F, 301F, 367F), pieces.subList(1, 6).stream().map(CrystalClusterGeometry.Piece::yaw).toList());
        assertEquals(List.of(28F, 19F, 26F, 18F, 29F), pieces.subList(1, 6).stream().map(CrystalClusterGeometry.Piece::pitch).toList());
        assertEquals(.13293153F, pieces.getLast().scaleX()); assertEquals(.4383208F, pieces.getLast().scaleY()); assertEquals(.15613317F, pieces.getLast().scaleZ());
    }
    @Test void originalSeedCollisionAndViewerPulsingDoNotChangeTheGeometry() {
        var first = CrystalClusterGeometry.pieces(6, 3, 5, 7, 0);
        assertEquals(first, CrystalClusterGeometry.pieces(6, 38, 0, 0, 0));
        assertEquals(first, CrystalClusterGeometry.pieces(6, 3, 7, 5, 0));
        var later = CrystalClusterGeometry.pieces(6, 3, 5, 7, 100);
        for (int index = 0; index < 6; index++) {
            var before = first.get(index); var after = later.get(index);
            assertEquals(before.color(), after.color()); assertEquals(before.yaw(), after.yaw()); assertEquals(before.pitch(), after.pitch());
            assertEquals(before.scaleX(), after.scaleX()); assertEquals(before.scaleY(), after.scaleY()); assertEquals(before.scaleZ(), after.scaleZ());
        }
        assertNotEquals(first.stream().map(CrystalClusterGeometry.Piece::light).toList(), later.stream().map(CrystalClusterGeometry.Piece::light).toList());
    }
    @Test void attachmentTransformsKeepOriginalSixFacesAndDoNotClampInvalidSignedShorts() {
        assertEquals(new CrystalClusterGeometry.Attachment(.5F, 1.3F, .5F, 'x', 180), CrystalClusterGeometry.attachment((short) 0));
        assertEquals(new CrystalClusterGeometry.Attachment(.5F, -.3F, .5F, 'x', 0), CrystalClusterGeometry.attachment((short) 1));
        assertEquals(new CrystalClusterGeometry.Attachment(.5F, .5F, 1.3F, 'x', -90), CrystalClusterGeometry.attachment((short) 2));
        assertEquals(new CrystalClusterGeometry.Attachment(.5F, .5F, -.3F, 'x', 90), CrystalClusterGeometry.attachment((short) 3));
        assertEquals(new CrystalClusterGeometry.Attachment(1.3F, .5F, .5F, 'z', 90), CrystalClusterGeometry.attachment((short) 4));
        assertEquals(new CrystalClusterGeometry.Attachment(-.3F, .5F, .5F, 'z', -90), CrystalClusterGeometry.attachment((short) 5));
        assertEquals(new CrystalClusterGeometry.Attachment(0, 0, 0, 'x', 0), CrystalClusterGeometry.attachment(Short.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> CrystalClusterGeometry.pieces(7, 0, 0, 0, 0));
    }
}
