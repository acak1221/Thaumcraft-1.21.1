package thaumcraft.common.testing;

import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Saves a data storage and waits until its files are really on disk.
 *
 * <p>Vanilla and Fabric write saved data synchronously inside {@code DimensionDataStorage.save()}.
 * NeoForge 21.1 hands the write to its IO worker ({@code SavedData.save(File)} →
 * {@code IOUtilities.withIOWorker}), so a fixture that reads the file right after saving races
 * the writer. The loader harness installs its own wait; the shared side never names a loader class.</p>
 */
public final class SavedDataFlush {
    private static volatile Runnable awaitWrites = () -> {};

    private SavedDataFlush() {}

    public static void install(Runnable waiter) { awaitWrites = waiter; }

    public static void save(DimensionDataStorage storage) {
        storage.save();
        awaitWrites.run();
    }
}
