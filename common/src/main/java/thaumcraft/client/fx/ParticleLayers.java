package thaumcraft.client.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Oracle ParticleEngine:57,138-196: four independent dimension maps, FIFO cap. */
public final class ParticleLayers<K, P> {
    public static final int LAYERS = 4;
    public static final int CAPACITY = 2000;
    private final List<Map<K, List<P>>> layers = new ArrayList<>();

    public ParticleLayers() {
        for (int i = 0; i < LAYERS; i++) layers.add(new HashMap<>());
    }

    public void add(K dimension, int layer, P particle) {
        List<P> particles = layers.get(layer).computeIfAbsent(dimension, key -> new ArrayList<>());
        // Do not kill the evicted object: the original only removes its reference.
        if (particles.size() >= CAPACITY) particles.removeFirst();
        particles.add(particle);
    }

    public void tick(K dimension, Consumer<P> tick, Predicate<P> alive) {
        for (Map<K, List<P>> layer : layers) {
            List<P> particles = layer.get(dimension);
            if (particles == null) continue;
            for (int i = 0; i < particles.size(); i++) {
                P particle = particles.get(i);
                tick.accept(particle);
                if (!alive.test(particle)) particles.remove(i--);
            }
        }
    }

    public void forEach(K dimension, Consumer<P> action) {
        for (Map<K, List<P>> layer : layers) {
            List<P> particles = layer.get(dimension);
            if (particles != null) for (P particle : particles) action.accept(particle);
        }
    }

    public void clear() { layers.forEach(Map::clear); }
}
