package thaumcraft.compat.jei;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Actual JEI lookups and rendered recipes after synchronized per-player scan changes. */
public final class AspectSourcesClientProbe implements ClientModInitializer {
    private static IJeiRuntime runtime;
    public static final class JeiCapture implements IModPlugin {
        @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("thaumcraft_client_smoke", "aspect_sources"); }
        @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
        @Override public void onRuntimeUnavailable() { runtime = null; }
    }
    private final String runId = "jei-aspect-sources-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private Path directory;
    private CompletableFuture<?> operation;
    private int stage, ticks, total;
    private boolean created, stopped, dispatched;
    private String pending;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.jeiAspectSourcesClient")) return;
        require(FabricLoader.getInstance().isModLoaded("jei"), "real JEI is loaded");
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-jei-aspect-sources-client"), "isolated client directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
            if (pending == null || dispatched || stopped || !current.getClass().getName().startsWith("mezz.jei.")) return;
            dispatched = true; graphics.flush(); String label = pending, file = runId + '-' + label + ".png";
            Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), ignored -> client.execute(() -> {
                try {
                    Path image = directory.resolve("screenshots").resolve(file);
                    require(Files.isRegularFile(image) && Files.size(image) > 1000, "native JEI screenshot exists");
                    captures.add(Map.of("label", label, "file", image.toString(), "bytes", Files.size(image)));
                    pending = null; dispatched = false; advance(stage + 1);
                } catch (Throwable failure) { finish(client, failure); }
            }));
        }));
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 3600 || ++ticks > 1200) throw new IllegalStateException("JEI sources timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    if (!client.getLanguageManager().getSelected().equals("ru_ru")) {
                        client.getLanguageManager().setSelected("ru_ru"); client.options.languageCode = "ru_ru";
                        client.reloadResourcePacks(); return;
                    }
                    created = true; client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("JEI supplied scan fixtures", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                    true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(73032026L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || pending != null || runtime == null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    operation = client.getSingleplayerServer().submit(() -> {
                        var player = actor(client); KnowledgeAccess.get(player).reset();
                        KnowledgeAccess.get(player).addDiscoveredPrimalAspects(); KnowledgeEvents.sync(player);
                    });
                    advance(1);
                }
                case 1 -> {
                    if (!complete() || ticks < 25 || !KnowledgeAccess.get(client.player).getScannedItems().isEmpty()) return;
                    require(aqua().isEmpty(), "fresh knowledge exposes no item sources for Aqua"); record("empty scans hide every source", List.of());
                    operation = client.getSingleplayerServer().submit(() -> ThaumcraftApiHelper.addScans(actor(client),
                            new ThaumcraftApiHelper.Scans(Set.of("@minecraft:clay_ball", "@minecraft:dirt"), Set.of(), Set.of())));
                    advance(2);
                }
                case 2 -> {
                    if (!complete() || ticks < 25 || !ThaumcraftApiHelper.hasScannedItem(client.player, new ItemStack(Items.CLAY_BALL))) return;
                    var sources = aqua();
                    require(sources.size() == 1 && sources.getFirst().item().is(Items.CLAY_BALL), "only the scanned Aqua-bearing clay ball is exposed");
                    require(sources.getFirst().amount() == ObjectAspectResolver.get(new ItemStack(Items.CLAY_BALL), client.level).getAmount(Aspect.WATER), "actual per-item Aqua count");
                    var unknown = runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT,
                            mezz.jei.api.constants.VanillaTypes.ITEM_STACK, new ItemStack(Items.CLAY));
                    require(runtime.getRecipeManager().createRecipeLookup(Categories.SOURCES).limitFocus(List.of(unknown)).get().findAny().isEmpty(),
                            "unscanned clay block cannot leak through item-usage lookup");
                    record("Aqua lookup filters unscanned and non-Aqua items", sources);
                    showAqua(); pending = "aqua-only-scanned-clay-ball";
                }
                case 3 -> {
                    client.setScreen(null);
                    operation = client.getSingleplayerServer().submit(() -> ThaumcraftApiHelper.addScans(actor(client),
                            new ThaumcraftApiHelper.Scans(Set.of("#minecraft:clay"), Set.of(), Set.of())));
                    advance(4);
                }
                case 4 -> {
                    if (!complete() || ticks < 25 || !ThaumcraftApiHelper.hasScannedItem(client.player, new ItemStack(Items.CLAY))) return;
                    var sources = aqua();
                    require(sources.size() == 2 && sources.stream().anyMatch(source -> source.item().is(Items.CLAY))
                            && sources.stream().anyMatch(source -> source.item().is(Items.CLAY_BALL)), "advanced scan updates actual JEI lookup without reload/relogin");
                    record("new advanced scan refreshes Aqua sources", sources); showAqua(); pending = "aqua-after-studying-clay-block";
                }
                case 5 -> {
                    client.setScreen(null);
                    operation = client.getSingleplayerServer().submit(() -> { KnowledgeAccess.get(actor(client)).reset(); KnowledgeEvents.sync(actor(client)); });
                    advance(6);
                }
                case 6 -> {
                    if (!complete() || ticks < 25 || !KnowledgeAccess.get(client.player).getScannedItems().isEmpty()) return;
                    require(aqua().isEmpty(), "forgetting scanned items removes cached sources"); record("forgotten scans hide sources again", List.of());
                    finish(client, null);
                }
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable failure) { finish(client, failure); }
    }

    private static IFocus<AspectIngredient.Stack> focus() {
        return runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, AspectIngredient.TYPE,
                new AspectIngredient.Stack(Aspect.WATER, 1));
    }
    private static List<Pages.Source> aqua() {
        return runtime.getRecipeManager().createRecipeLookup(Categories.SOURCES).limitFocus(List.of(focus())).get().toList();
    }
    private static void showAqua() {
        runtime.getRecipesGui().show(List.of(focus()));
        require(Minecraft.getInstance().screen != null && Minecraft.getInstance().screen.getClass().getName().startsWith("mezz.jei."), "native Aqua focus opens real JEI recipes UI");
    }
    private boolean complete() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void advance(int next) { stage = next; ticks = 0; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void record(String label, List<Pages.Source> sources) {
        checks.add(Map.of("check", label, "passed", true, "sources", sources.stream().map(source ->
                Map.of("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.item().getItem()).toString(),
                        "aspect", source.aspect().getTag(), "amount", source.amount())).toList()));
    }
    private void finish(Minecraft client, Throwable error) {
        if (stopped) return; stopped = true;
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", error == null ? "PASS" : "FAILED"); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "fresh1.21.1 world, supplied @clay_ball/@dirt and #clay scan markers through the server API, actual JEI and knowledge packets; not survival acquisition");
            report.put("checks", checks); report.put("captures", captures);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("jei-aspect-sources-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
