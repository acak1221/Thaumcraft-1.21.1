package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import thaumcraft.client.fx.particles.TaintDropParticle;
import thaumcraft.common.config.ThaumcraftTaint;

/** Reproduces the reported animateTick crash in a real client, then checks all drip frames and reload. */
public final class TaintDripClientProbe implements ClientModInitializer {
    private static final BlockPos CEILING = new BlockPos(0, 104, 0);
    private final String runId = "taint-drip-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private final RandomSource random = RandomSource.create(73032026L);
    private CompletableFuture<?> operation;
    private Path directory;
    private String pendingCapture;
    private int stage, ticks, total;
    private boolean created, stopped, dispatched;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.taintDripClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-taint-drip-client"), "isolated client directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (pendingCapture == null || dispatched || stopped || client.screen != null || client.getOverlay() != null) return;
            dispatched = true;
            graphics.flush();
            String label = pendingCapture, file = runId + '-' + label + ".png";
            Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), ignored -> client.execute(() -> {
                try {
                    Path image = directory.resolve("screenshots").resolve(file);
                    require(Files.isRegularFile(image) && Files.size(image) > 1000, "native screenshot exists");
                    captures.add(Map.of("label", label, "file", image.toString(), "bytes", Files.size(image)));
                    pendingCapture = null; dispatched = false; advance(stage + 1);
                } catch (Throwable error) { finish(client, error); }
            }));
        });
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            if (++total > 3600) throw new IllegalStateException("Taint drip client timed out at stage " + stage);
            ticks++;
            if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
                if (client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.pauseOnLostFocus = false;
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Taint drip regression supplied fixtures", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(73032026L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || pendingCapture != null) return;
            client.player.setYRot(180); client.player.setXRot(-12);
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    operation = client.getSingleplayerServer().submit(() -> {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
                        var level = player.serverLevel();
                        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
                        for (BlockPos pos : BlockPos.betweenClosed(-6, 100, -5, 6, 100, 7)) level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                        for (BlockPos pos : BlockPos.betweenClosed(-3, 104, -1, 3, 104, 1)) level.setBlock(pos, ThaumcraftTaint.CRUST.get().defaultBlockState(), 3);
                        player.teleportTo(.5, 101, 4.5);
                    });
                    advance(1);
                }
                case 1 -> {
                    if (!operation.isDone() || ticks < 40 || !client.level.getBlockState(CEILING).is(ThaumcraftTaint.CRUST.get())) return;
                    operation.join(); checkFrames(client, "initial load"); advance(2);
                }
                case 2, 5 -> {
                    // Keep real engine-owned particles visible through normal ticks and rendering.
                    for (BlockPos pos : BlockPos.betweenClosed(-3, 104, -1, 3, 104, 1)) animate(client, pos);
                    if (ticks >= 75) pendingCapture = stage == 2 ? "taint-drips" : "taint-drips-after-resource-reload";
                }
                case 3 -> { operation = client.reloadResourcePacks(); advance(4); }
                case 4 -> {
                    if (!operation.isDone() || ticks < 20) return;
                    operation.join(); checkFrames(client, "resource reload"); advance(5);
                }
                default -> finish(client, null);
            }
        } catch (Throwable error) { finish(client, error); }
    }

    private void animate(Minecraft client, BlockPos pos) {
        var state = client.level.getBlockState(pos);
        require(state.is(ThaumcraftTaint.CRUST.get()) && client.level.isEmptyBlock(pos.below()), "actual suspended taint crust");
        state.getBlock().animateTick(state, client.level, pos, random);
    }

    private void checkFrames(Minecraft client, String phase) throws ReflectiveOperationException {
        // Named dev-client fields only: inspect the actual production particle, without a production test hook.
        Field queued = ParticleEngine.class.getDeclaredField("particlesToAdd"); queued.setAccessible(true);
        Collection<?> pending = (Collection<?>) queued.get(client.particleEngine);
        TaintDropParticle drip = null;
        for (int i = 0; i < 100 && drip == null; i++) {
            animate(client, CEILING);
            for (Object candidate : pending) if (candidate instanceof TaintDropParticle particle) drip = particle;
        }
        require(drip != null, "BlockTaint.animateTick queues a real taint drip");
        checkSprite(drip, "drip_hang", phase);
        for (int i = 0; i < 40; i++) drip.tick();
        checkSprite(drip, "drip_hang", phase + " after forty bob ticks");
        drip.tick(); checkSprite(drip, "drip_fall", phase);
        Field sprite = TextureSheetParticle.class.getDeclaredField("sprite"); sprite.setAccessible(true);
        boolean landed = false;
        for (int i = 0; i < 20 && drip.isAlive(); i++) {
            drip.tick();
            if (((TextureAtlasSprite) sprite.get(drip)).contents().name().equals(ResourceLocation.withDefaultNamespace("drip_land"))) { landed = true; break; }
        }
        require(landed, "native floor collision selects the landing frame");
        checkSprite(drip, "drip_land", phase); drip.remove();
    }

    private void checkSprite(TaintDropParticle drip, String expected, String phase) throws ReflectiveOperationException {
        Field field = TextureSheetParticle.class.getDeclaredField("sprite"); field.setAccessible(true);
        ResourceLocation actual = ((TextureAtlasSprite) field.get(drip)).contents().name();
        require(actual.equals(ResourceLocation.withDefaultNamespace(expected)), "expected loaded " + expected + ", got " + actual);
        checks.add(Map.of("check", phase, "sprite", actual.toString(), "passed", true));
    }
    private void advance(int next) { stage = next; ticks = 0; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void finish(Minecraft client, Throwable error) {
        if (stopped) return;
        stopped = true;
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", error == null ? "PASS" : "FAILED"); report.put("run_id", runId);
            report.put("fixture", "fresh 1.21.1 flat world, supplied suspended taint crust and stone floor; original animateTick and particle physics; native resource reload");
            report.put("checks", checks); report.put("captures", captures); report.put("stage", stage);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("taint-drip-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
