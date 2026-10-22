package thaumcraft.common.testing;

import dev.architectury.event.events.common.LifecycleEvent;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.lib.events.ChunkRegeneration;

/** Real bootstrap callbacks, not manual tick calls or a second event registration. */
public final class EventLifecycleRuntimeChecks {
    private EventLifecycleRuntimeChecks() {}

    public static void regenerationQueueLifecycle(GameTestHelper test) {
        var level = test.getLevel();
        test.assertTrue(!ThaumcraftBiomeSettings.regen().anyRegeneration(), "fixture requires default regeneration disabled");
        try {
            // A fixture must preserve its caller's nondefault configuration too.
            try (var settings = new BiomeConfiguration()) {
                var nondefault = new Properties();
                nondefault.setProperty("blue_magical_forest", "true");
                ThaumcraftBiomeSettings.configure(nondefault);
                var before = ThaumcraftBiomeSettings.current();
                enqueue(test, 1);
                test.assertTrue(ThaumcraftBiomeSettings.current() == before,
                        "regeneration fixture restores the EXACT prior nondefault biome configuration, not defaults");
            }
            var field = ChunkRegeneration.class.getDeclaredField("QUEUE");
            field.setAccessible(true);
            test.assertTrue(((Map<?, ?>) field.get(null)).containsKey(level),
                    "queue belongs to this world instance, not its reusable dimension key");
            boolean liveBefore = BoreNativeContext.isLiveLevel(level);
            test.assertTrue(liveBefore, "lifecycle fixture begins with a live shared test level");
            try (var lifetime = new BoreLevelLifetime(level)) {
                LifecycleEvent.SERVER_LEVEL_UNLOAD.invoker().act(level);
                thaumcraft.common.Thaumcraft.LOG.info("GLOBAL_LIFETIME regenerationQueueLifecycle before={} afterUnload={}",
                        liveBefore, BoreNativeContext.isLiveLevel(level));
                test.assertTrue(!BoreNativeContext.isLiveLevel(level), "real registered unload callback revokes the old lifetime");
                test.assertTrue(ChunkRegeneration.queued(level) == 0, "registered unload callback discards pending work");
            }
            thaumcraft.common.Thaumcraft.LOG.info("GLOBAL_LIFETIME regenerationQueueLifecycle restored={}",
                    BoreNativeContext.isLiveLevel(level));
            test.assertTrue(BoreNativeContext.isLiveLevel(level) == liveBefore,
                    "unload fixture must restore the shared level lifetime before later Bore scenarios");
            enqueue(test, 21);
            // Disabled regeneration cannot alter terrain. Only the real world-post
            // callback consumes this queue: 21 -> 11 -> 1 -> 0.
            test.runAfterDelay(1, () -> test.assertTrue(ChunkRegeneration.queued(level) == 11, "one world tick drains ten, not twenty"));
            test.runAfterDelay(2, () -> test.assertTrue(ChunkRegeneration.queued(level) == 1, "two world ticks drain twenty"));
            test.runAfterDelay(3, () -> {
                test.assertTrue(ChunkRegeneration.queued(level) == 0, "third tick drains remaining work");
                enqueue(test, 1);
                ChunkRegeneration.clearServer(level.getServer());
                test.assertTrue(ChunkRegeneration.queued(level) == 0, "shutdown fallback discards pending work");
                test.succeed();
            });
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void enqueue(GameTestHelper test, int count) {
        Properties settings = new Properties();
        settings.setProperty("aura_nodes", "true");
        try (var previous = new BiomeConfiguration()) {
            ThaumcraftBiomeSettings.configure(settings);
            for (int i = 0; i < count; i++)
                ChunkRegeneration.readMarker(test.getLevel(), new ChunkPos(i, 0), new CompoundTag());
        }
    }

    /** Test-only access to the immutable startup snapshot; no production API for test overrides. */
    private static final class BiomeConfiguration implements AutoCloseable {
        private final Field field;
        private final Object original;

        BiomeConfiguration() {
            try {
                field = ThaumcraftBiomeSettings.class.getDeclaredField("current");
                field.setAccessible(true);
                original = field.get(null);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }

        @Override public void close() {
            try { field.set(null, original); }
            catch (IllegalAccessException failure) { throw new AssertionError(failure); }
        }
    }

    /** The unload is simulated: do not permanently revoke the server's still-live shared level.
     * Restore only this level's exact old marker, never clear another owner's denial. */
    private static final class BoreLevelLifetime implements AutoCloseable {
        private final Map<ServerLevel, Boolean> revoked;
        private final ServerLevel level;
        private final boolean present;
        private final Boolean original;

        @SuppressWarnings("unchecked")
        BoreLevelLifetime(ServerLevel level) {
            this.level = level;
            try {
                var field = BoreNativeContext.class.getDeclaredField("REVOKED_LEVELS");
                field.setAccessible(true);
                revoked = (Map<ServerLevel, Boolean>) field.get(null);
                present = revoked.containsKey(level);
                original = revoked.get(level);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }

        @Override public void close() {
            if (present) revoked.put(level, original);
            else revoked.remove(level);
        }
    }
}
