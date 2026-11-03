package thaumcraft.compat.jei;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import thaumcraft.client.lib.ScannedAspectSources;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ItemScanIdentity;

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
    private int stage, ticks, total, warmTicks, coldReadyTicks, coldRenderedFrames;
    private boolean created, stopped, dispatched, secondWorld, transitioning;
    private Object firstPlayer, firstLevel;
    private IJeiRuntime firstRuntime;
    private AspectSources coldSources;
    private Pages.Source coldPlaceholder;
    private String pending;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.jeiAspectSourcesClient")) return;
        require(FabricLoader.getInstance().isModLoaded("jei"), "real JEI is loaded");
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-jei-aspect-sources-client"), "isolated client directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
            if (current.getClass().getName().startsWith("mezz.jei.") && coldPlaceholder != null
                    && coldPlaceholder.pending().refreshCompleted) coldRenderedFrames++;
            if (pending == null || dispatched || stopped || !current.getClass().getName().startsWith("mezz.jei.")) return;
            if (pending.equals("aqua-after-cold-loading") && coldRenderedFrames < 10) return;
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
        if (stopped || transitioning) return;
        try {
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            if (++total > 3600 || ++ticks > 1200) throw new IllegalStateException("JEI sources timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    if (!client.getLanguageManager().getSelected().equals("ru_ru")) {
                        client.getLanguageManager().setSelected("ru_ru"); client.options.languageCode = "ru_ru";
                        client.reloadResourcePacks(); return;
                    }
                    created = true; client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.createWorldOpenFlows().createFreshLevel(runId + (secondWorld ? "-second" : ""),
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
                    if (!ScannedAspectSources.isIdle()) return;
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
                    if (!ScannedAspectSources.isIdle()) return;
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
                    advance(7);
                }
                case 7 -> {
                    Set<String> scans = new LinkedHashSet<>();
                    for (ItemStack item : runtime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)) {
                        if (!item.isEmpty()) scans.add(ItemScanIdentity.signature(item));
                        if (scans.size() >= 600) break;
                    }
                    scans.remove("@minecraft:clay");
                    scans.add("@minecraft:clay_ball"); scans.add("#minecraft:clay");
                    require(scans.size() >= 500, "broad supplied fixture includes at least 500 indexed scan identities");
                    operation = client.getSingleplayerServer().submit(() -> ThaumcraftApiHelper.addScans(actor(client),
                            new ThaumcraftApiHelper.Scans(scans, Set.of(), Set.of())));
                    advance(8);
                }
                case 8 -> {
                    if (!complete() || ticks < 25 || KnowledgeAccess.get(client.player).getScannedItems().size() < 500) return;
                    List<Aspect> aspects = List.copyOf(Aspect.aspects.values());
                    for (Aspect aspect : aspects) query(aspect);
                    if (!ScannedAspectSources.isIdle()) { warmTicks = 0; return; }
                    if (++warmTicks < 40) return;
                    require(!aqua().isEmpty(), "first world has known Aqua sources before switching worlds");
                    long before = ScannedAspectSources.resolvedItemCount(), started = System.nanoTime();
                    long rows = 0;
                    for (int i = 0; i < 1000; i++) rows += query(aspects.get(i % aspects.size())).size();
                    long elapsed = System.nanoTime() - started, after = ScannedAspectSources.resolvedItemCount();
                    require(after == before, "1000 actual JEI output queries and aspect switches perform no new item aspect resolutions");
                    Map<String, Object> benchmark = new LinkedHashMap<>();
                    benchmark.put("check", "warm JEI lookups reuse resolved source index"); benchmark.put("passed", true);
                    benchmark.put("supplied_scan_count", KnowledgeAccess.get(client.player).getScannedItems().size());
                    benchmark.put("lookups", 1000); benchmark.put("aspects", aspects.size()); benchmark.put("returned_rows", rows);
                    benchmark.put("elapsed_ns", elapsed); benchmark.put("warm_ticks", warmTicks);
                    benchmark.put("item_resolutions_before", before); benchmark.put("item_resolutions_after", after);
                    benchmark.put("additional_item_resolutions", after - before); checks.add(benchmark);
                    showAqua(); pending = "aqua-large-scan-index";
                }
                case 9 -> {
                    coldSources = new AspectSources(runtime.getIngredientManager(), runtime.getJeiHelpers().getFocusFactory());
                    long before = ScannedAspectSources.resolvedItemCount();
                    var placeholders = coldSources.getRecipesForOutput(focus().getTypedValue());
                    require(placeholders.size() == 1 && placeholders.getFirst().pending() != null
                            && placeholders.getFirst().item().isEmpty(), "cold JEI lookup returns only a safe loading placeholder");
                    require(ScannedAspectSources.resolvedItemCount() == before, "cold JEI lookup performs no synchronous item aspect resolutions");
                    coldPlaceholder = placeholders.getFirst();
                    coldRenderedFrames = 0;
                    runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(Categories.SOURCES),
                            placeholders, List.of(focus()));
                    advance(10);
                }
                case 10 -> {
                    if (!coldPlaceholder.pending().sources.isReady() || !coldPlaceholder.pending().refreshCompleted) {
                        coldReadyTicks = 0; return;
                    }
                    if (++coldReadyTicks < 5) return;
                    var loaded = coldSources.getRecipesForOutput(focus().getTypedValue());
                    require(!loaded.isEmpty() && loaded.stream().allMatch(source -> source.pending() == null && !source.item().isEmpty()),
                            "completed cold JEI index supplies actual scanned item source rows");
                    require(client.screen instanceof mezz.jei.api.runtime.IRecipesGui, "queued placeholder refresh keeps the real JEI recipe screen open");
                    checks.add(Map.of("check", "cold JEI placeholder completes its source-page refresh after background loading",
                            "passed", true, "loaded_source_rows", loaded.size(), "settled_refresh_ticks", coldReadyTicks));
                    pending = "aqua-after-cold-loading";
                }
                case 11 -> {
                    firstPlayer = client.player; firstLevel = client.level; firstRuntime = runtime;
                    secondWorld = true; created = false;
                    transitioning = true;
                    try {
                        client.level.disconnect();
                        client.disconnect(new TitleScreen());
                        client.setScreen(new TitleScreen());
                    } finally { transitioning = false; }
                    advance(12);
                }
                case 12 -> {
                    if (ticks < 40 || runtime == firstRuntime) return;
                    require(client.player != firstPlayer && client.level != firstLevel, "second fresh world has new native player and level objects");
                    require(KnowledgeAccess.get(client.player).getScannedItems().isEmpty(), "second fresh world has no first-world scan knowledge");
                    for (Aspect aspect : Aspect.aspects.values()) {
                        require(query(aspect).isEmpty(), "second fresh world exposes no cached first-world sources for " + aspect.getTag());
                    }
                    require(coldSources.getRecipesForOutput(focus().getTypedValue()).isEmpty(),
                            "retained first-world JEI provider cannot rebind to the second world's player or recipes");
                    if (!ScannedAspectSources.isIdle()) return;
                    record("disconnect and fresh world in same client hide all first-world sources", List.of());
                    finish(client, null);
                }
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable failure) { finish(client, failure); }
    }

    private static IFocus<AspectIngredient.Stack> focus() {
        return focus(Aspect.WATER);
    }
    private static IFocus<AspectIngredient.Stack> focus(Aspect aspect) {
        return runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, AspectIngredient.TYPE,
                new AspectIngredient.Stack(aspect, 1));
    }
    private static List<Pages.Source> aqua() {
        return query(Aspect.WATER);
    }
    private static List<Pages.Source> query(Aspect aspect) {
        return runtime.getRecipeManager().createRecipeLookup(Categories.SOURCES).limitFocus(List.of(focus(aspect))).get().toList();
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
            report.put("fixture", "two fresh1.21.1 worlds in one client; first receives supplied @clay_ball/@dirt, #clay and at least500 indexed scan identities through the server API; second receives no supplied scans; actual JEI lookups and knowledge packets; not survival acquisition or a measured FPS claim");
            report.put("checks", checks); report.put("captures", captures);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("jei-aspect-sources-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
