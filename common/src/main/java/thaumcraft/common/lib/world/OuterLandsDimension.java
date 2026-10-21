package thaumcraft.common.lib.world;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The runtime handle of the TC4 Outer Lands dimension.
 *
 * <p>The dimension itself is registered by the data pack, not by code: the level stem
 * {@code data/thaumcraft/dimension/outer_lands.json} is read into the {@code minecraft:level_stem}
 * registry of Minecraft 1.21.1 and the server creates a level for every entry of that registry.
 * This class only looks the level up and reports it, so that the running server proves that the
 * Outer Lands exists with the original values.
 *
 * @see OuterLandsDimensionRules
 */
public final class OuterLandsDimension {
    private static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");

    private OuterLandsDimension() {}

    /** The level of the Outer Lands, or null when the server did not register the dimension. */
    public static ServerLevel level(MinecraftServer server) {
        return server == null ? null : server.getLevel(OuterLandsProvider.DIMENSION);
    }

    /** True when the running server registered the Outer Lands. */
    public static boolean isRegistered(MinecraftServer server) {
        return level(server) != null;
    }

    /**
     * The report of the registered Outer Lands.
     *
     * <p>It names the generator and the biome of the level and the original values of the
     * dimension type, so that the server log shows that the dimension is a real dimension.
     */
    public static String describe(ServerLevel level) {
        var type = level.dimensionType();
        var generator = level.getChunkSource().getGenerator();
        String biome = "unknown";
        if (generator instanceof FlatLevelSource flat) {
            biome = flat.settings().getBiome().unwrapKey()
                    .map(key -> key.location().toString()).orElse("unknown");
        }
        return "TC4 Outer Lands dimension registered: " + OuterLandsDimensionRules.LEVEL_STEM_ID
                + " (" + generator.getClass().getSimpleName() + " generator, " + biome + " biome, height "
                + type.height() + ", min y " + type.minY() + ", sky light " + type.hasSkyLight()
                + ", ambient light " + type.ambientLight() + ", fixed time " + type.fixedTime().orElse(-1L) + ")";
    }

    /** Reports the Outer Lands when the server is ready. */
    public static void registerLogging() {
        LifecycleEvent.SERVER_STARTED.register(server -> {
            ServerLevel outer = level(server);
            if (outer == null) {
                LOG.error("TC4 Outer Lands dimension is NOT registered: {}", OuterLandsDimensionRules.LEVEL_STEM_ID);
                return;
            }
            LOG.info(describe(outer));
        });
    }
}
