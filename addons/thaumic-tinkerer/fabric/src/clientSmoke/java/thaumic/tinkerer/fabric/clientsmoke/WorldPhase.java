package thaumic.tinkerer.fabric.clientsmoke;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Makes the world the rest of the run happens in: a fresh creative flat singleplayer world with cheats on, no mobs, no
 * weather, fixed noon. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The harness creates the world itself ({@code WorldOpenFlows.createFreshLevel}) from the title screen, so no prepared save
 * is needed and the run is the same every time: the same seed, the vanilla flat preset without structures, peaceful, the daylight
 * cycle, the weather cycle, mob spawning, fire ticks and random ticks off.</p>
 */
final class WorldPhase implements Phase {
    static final String LEVEL = "tt-clientsmoke";

    private enum State { LAUNCH, WAIT_WORLD, SETUP, WAIT_SETUP }

    private State state = State.LAUNCH;
    private long since = System.nanoTime();
    private java.util.concurrent.CompletableFuture<?> setup;

    @Override
    public String name() {
        return "world";
    }

    @Override
    public boolean tick(SmokeRun run) throws Exception {
        Minecraft client = run.client();
        switch (state) {
            case LAUNCH -> {
                if (client.getOverlay() != null || !(client.screen instanceof TitleScreen)) return limit("title screen to create the world from");
                Stage.reset();
                LevelSettings settings = new LevelSettings(LEVEL, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
                        WorldDataConfiguration.DEFAULT);
                client.createWorldOpenFlows().createFreshLevel(LEVEL, settings, new WorldOptions(20261030L, false, false),
                        WorldPhase::flat, client.screen);
                state = State.WAIT_WORLD;
                since = System.nanoTime();
            }
            case WAIT_WORLD -> {
                ServerPlayer player = run.serverPlayer();
                if (client.level == null || client.player == null || client.screen != null || player == null || client.player.tickCount < 40) {
                    return limit("the player in the new world");
                }
                run.report().info("world", "world_created", "singleplayer world '" + LEVEL + "' joined; client level " + client.level.dimension().location()
                        + ", player " + client.player.getName().getString() + " at " + client.player.blockPosition());
                state = State.SETUP;
            }
            case SETUP -> {
                MinecraftServer server = run.server();
                setup = java.util.concurrent.CompletableFuture.runAsync(() -> configure(server, run.serverPlayer()), server);
                state = State.WAIT_SETUP;
            }
            case WAIT_SETUP -> {
                if (!setup.isDone()) return limit("the server-side set-up");
                setup.join();
                return true;
            }
        }
        return false;
    }

    /** True never; fails the run's world when the wait took more than two minutes. */
    private boolean limit(String waitingFor) {
        if (System.nanoTime() - since > 120_000_000_000L) throw new IllegalStateException("timed out waiting for " + waitingFor + " (state " + state + ")");
        return false;
    }

    /** The vanilla flat preset with the structure sets removed, so that no village ever stands on the grid. */
    private static WorldDimensions flat(net.minecraft.core.RegistryAccess access) {
        WorldDimensions flat = access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
        FlatLevelGeneratorSettings settings = ((FlatLevelSource) flat.overworld()).settings();
        FlatLevelGeneratorSettings bare = settings.withBiomeAndLayers(settings.getLayersInfo(), Optional.of(HolderSet.direct()), settings.getBiome());
        return flat.replaceOverworldGenerator(access, new FlatLevelSource(bare));
    }

    private static void configure(MinecraftServer server, ServerPlayer player) {
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        rules.getRule(GameRules.RULE_DISABLE_RAIDS).set(true, server);
        rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        ServerLevel level = player.serverLevel();
        level.setDayTime(6000L);
        level.setWeatherParameters(24000, 0, false, false);
        // The player hangs in the air at the camera spots: no gravity (a creative player on the ground stops flying by itself).
        player.setNoGravity(true);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }
}
