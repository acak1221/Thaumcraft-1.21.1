package thaumcraft.api.aspects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The aspect registration event of the original add-on surface.
 *
 * <p>Forced 1.21.1 change: Forge's {@code net.minecraftforge.fml.common.eventhandler.Event} does not exist in
 * 1.21.1, so the event is a plain object that add-ons subscribe to with
 * {@link #addListener(Consumer)} and that the mod raises with {@link #post()}. The published
 * {@link #register} proxy keeps the original registration surface.</p>
 */
public class AspectRegistryEvent {
    /** The registration proxy the original event exposed. */
    public final AspectEventProxy register = new AspectEventProxy();

    private static final List<Consumer<AspectRegistryEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    /** Subscribes to the event; listeners run in registration order. */
    public static void addListener(Consumer<AspectRegistryEvent> listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    /** Unsubscribes a previously added listener. */
    public static void removeListener(Consumer<AspectRegistryEvent> listener) {
        if (listener != null) LISTENERS.remove(listener);
    }

    /** The number of registered listeners; a listener may unregister itself while the event runs. */
    public static int listenerCount() { return LISTENERS.size(); }

    /** Raises the event. A listener that throws does not stop the remaining listeners. */
    public static void post() {
        AspectRegistryEvent event = new AspectRegistryEvent();
        for (Consumer<AspectRegistryEvent> listener : LISTENERS) {
            try {
                listener.accept(event);
            } catch (RuntimeException failure) {
                org.slf4j.LoggerFactory.getLogger("Thaumcraft").warn("An aspect registry listener failed", failure);
            }
        }
    }
}
