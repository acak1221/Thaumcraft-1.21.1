package thaumcraft.client.fx;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleLayersTest {
    @Test void originalCapacityEvictsOldestBeforeAppend() {
        var layers = new ParticleLayers<String, Integer>();
        for (int i = 0; i <= 2000; i++) layers.add("overworld", 0, i);
        var retained = new ArrayList<Integer>();
        layers.forEach("overworld", retained::add);
        assertEquals(2000, retained.size());
        assertEquals(1, retained.getFirst());
        assertEquals(2000, retained.getLast());
    }

    @Test void allFourLayersHaveIndependentCapsAndOriginalDrawOrder() {
        var layers = new ParticleLayers<String, Integer>();
        for (int layer = 3; layer >= 0; layer--) {
            for (int i = 0; i <= 2000; i++) layers.add("a", layer, layer * 10000 + i);
        }
        var retained = new ArrayList<Integer>();
        layers.forEach("a", retained::add);
        assertEquals(8000, retained.size());
        for (int layer = 0; layer < 4; layer++) assertEquals(layer * 10000 + 1, retained.get(layer * 2000));
    }

    @Test void inactiveDimensionsRetainParticlesAndDoNotTick() {
        var layers = new ParticleLayers<String, String>();
        layers.add("a", 0, "a");
        layers.add("b", 0, "b");
        var ticked = new ArrayList<String>();
        layers.tick("a", ticked::add, p -> false);
        assertEquals(List.of("a"), ticked);
        var retained = new ArrayList<String>();
        layers.forEach("b", retained::add);
        assertEquals(List.of("b"), retained);
        layers.tick("b", ticked::add, p -> true);
        assertEquals(List.of("a", "b"), ticked);
    }

    @Test void adjacentDeadEntriesAreRemovedWithoutSkippingAndDisconnectClearsAll() {
        var layers = new ParticleLayers<String, Integer>();
        layers.add("a", 0, 1);
        layers.add("a", 0, 2);
        layers.add("a", 0, 3);
        layers.tick("a", p -> {}, p -> p == 3);
        var retained = new ArrayList<Integer>();
        layers.forEach("a", retained::add);
        assertEquals(List.of(3), retained);
        layers.clear();
        retained.clear();
        layers.forEach("a", retained::add);
        assertTrue(retained.isEmpty());
    }
}
