package thaumcraft.tiles;

import org.junit.jupiter.api.Test;
import thaumcraft.common.tiles.JarContents;
import static org.junit.jupiter.api.Assertions.*;

class JarContentsTest {
    @Test void capacityRejectsOverflowAndPreservesAspect() {
        JarContents jar = new JarContents();
        for (int i = 0; i < 8; i++) assertEquals(0, jar.add("aer", 8));
        assertEquals(64, jar.amount());
        assertEquals(8, jar.add("aer", 8));
        assertEquals(8, jar.add("ignis", 8));
        assertEquals("aer", jar.tag());
        assertEquals(64, jar.amount());
    }
    @Test void eightUnitTransferConservesSeparateResourceTypes() {
        JarContents source = new JarContents();
        JarContents destination = new JarContents();
        source.add("terra", 64);
        assertTrue(source.take("terra", 8));
        assertEquals(0, destination.add("terra", 8));
        assertEquals(64, source.amount() + destination.amount());
        assertFalse(source.take("aqua", 8));
        assertEquals(56, source.amount());
        assertEquals(8, destination.add("aqua", 8));
        assertEquals(8, destination.amount());
    }
    @Test void emptiedJarCanChangeTypeAndMalformedSaveCannotCreateOverflow() {
        JarContents jar = new JarContents();
        jar.add("aer", 8);
        assertFalse(jar.take("aer", 9));
        assertTrue(jar.take("aer", 8));
        assertNull(jar.tag());
        assertEquals(0, jar.add("ignis", 8));
        jar.restore("ignis", 999);
        assertEquals(64, jar.amount());
        jar.restore("unknown", 8);
        assertEquals(0, jar.amount());
        assertNull(jar.tag());
        assertFalse(jar.take("ignis", -8));
        assertEquals(-8, jar.add("ignis", -8));
        assertEquals(0, jar.amount());
    }
    @Test void voidOffersConsumeExcessButNeverStoreItOrEraseAnotherType() {
        JarContents jar = new JarContents();
        assertEquals(0, jar.add("aqua", 60, true));
        assertEquals(0, jar.add("aqua", 8, true));
        assertEquals(64, jar.amount());
        assertEquals(0, jar.add("aqua", Integer.MAX_VALUE, true));
        assertEquals(64, jar.amount());
        assertEquals(8, jar.add("ignis", 8, true));
        assertEquals("aqua", jar.tag());
        assertEquals(64, jar.amount());
        assertTrue(jar.take("aqua", 64));
        assertEquals(0, jar.add("ignis", 0, true));
        assertNull(jar.tag());
        assertEquals(-8, jar.add("ignis", -8, true));
        assertEquals(8, jar.add("unknown", 8, true));
        assertEquals(0, jar.amount());
        assertEquals(0, jar.add("ignis", 8, true));
        assertEquals("ignis", jar.tag());
    }
}
