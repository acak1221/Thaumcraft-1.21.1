package thaumcraft.common.testing;

import dev.architectury.event.events.common.LifecycleEvent;
import java.util.Map;
import java.util.Properties;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.lib.events.ChunkRegeneration;

/** Real bootstrap callbacks, not manual tick calls or a second event registration. */
public final class EventLifecycleRuntimeChecks {
    private EventLifecycleRuntimeChecks() {}

    public static void regenerationQueueLifecycle(GameTestHelper test) {
        var level = test.getLevel();
        test.assertTrue(!ThaumcraftBiomeSettings.regen().anyRegeneration(), "fixture requires default regeneration disabled");
        try {
            enqueue(test, 1);
            var field = ChunkRegeneration.class.getDeclaredField("QUEUE");
            field.setAccessible(true);
            test.assertTrue(((Map<?, ?>) field.get(null)).containsKey(level),
                    "queue belongs to this world instance, not its reusable dimension key");
            LifecycleEvent.SERVER_LEVEL_UNLOAD.invoker().act(level);
            test.assertTrue(ChunkRegeneration.queued(level) == 0, "registered unload callback discards pending work");
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
        try {
            ThaumcraftBiomeSettings.configure(settings);
            for (int i = 0; i < count; i++)
                ChunkRegeneration.readMarker(test.getLevel(), new ChunkPos(i, 0), new CompoundTag());
        } finally {
            ThaumcraftBiomeSettings.defaults();
        }
    }
}
