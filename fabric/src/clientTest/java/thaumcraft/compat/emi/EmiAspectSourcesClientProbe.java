package thaumcraft.compat.emi;

import com.google.gson.GsonBuilder;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

/** Actual EMI lookups and rendered recipes after synchronized per-player scan changes. */
public final class EmiAspectSourcesClientProbe implements ClientModInitializer {
    private final String runId = "emi-aspect-sources-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private Path directory;
    private CompletableFuture<?> operation;
    private final List<SlotWidget> retainedSlots = new ArrayList<>();
    private int stage, ticks, total;
    private boolean created, stopped, dispatched;
    private String pending;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.emiAspectSourcesClient")) return;
        require(FabricLoader.getInstance().isModLoaded("emi"), "real EMI is loaded");
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-emi-aspect-sources-client"), "isolated client directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
            if (pending == null || dispatched || stopped || !current.getClass().getName().startsWith("dev.emi.emi.")) return;
            dispatched = true; graphics.flush(); String label = pending, file = runId + '-' + label + ".png";
            Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), ignored -> client.execute(() -> {
                try {
                    Path image = directory.resolve("screenshots").resolve(file);
                    require(Files.isRegularFile(image) && Files.size(image) > 1000, "native EMI screenshot exists");
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
            if (++total > 3600 || ++ticks > 1200) throw new IllegalStateException("EMI sources timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    if (!client.getLanguageManager().getSelected().equals("ru_ru")) {
                        client.getLanguageManager().setSelected("ru_ru"); client.options.languageCode = "ru_ru";
                        client.reloadResourcePacks(); return;
                    }
                    created = true; client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("EMI supplied scan fixtures", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                    true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(73032026L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || pending != null || !emiReady()) return;
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
                    require(aqua().sources().isEmpty(), "fresh knowledge exposes no item sources for Aqua");
                    retainActualSlots();
                    require(displayedItems().isEmpty(), "native EMI slots contain no unscanned item before scanning");
                    record("empty scans hide every source");
                    operation = client.getSingleplayerServer().submit(() -> ThaumcraftApiHelper.addScans(actor(client),
                            new ThaumcraftApiHelper.Scans(Set.of("@minecraft:clay_ball", "@minecraft:dirt"), Set.of(), Set.of())));
                    advance(2);
                }
                case 2 -> {
                    if (!complete() || ticks < 25 || !ThaumcraftApiHelper.hasScannedItem(client.player, new ItemStack(Items.CLAY_BALL))) return;
                    var sources = aqua().sources();
                    require(sources.size() == 1 && sources.getFirst().item().is(Items.CLAY_BALL), "only the scanned Aqua-bearing clay ball is exposed");
                    require(sources.getFirst().amount() == ObjectAspectResolver.get(new ItemStack(Items.CLAY_BALL), client.level).getAmount(Aspect.WATER), "actual per-item Aqua count");
                    require(displayedItems().size() == 1 && displayedItems().getFirst().is(Items.CLAY_BALL),
                            "retained native EMI slots immediately expose only scanned Aqua-bearing clay ball");
                    require(aqua().getInputs().isEmpty(), "source recipe index contains no unscanned item inputs");
                    record("Aqua lookup filters unscanned and non-Aqua items");
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
                    var sources = aqua().sources();
                    require(sources.size() == 2 && sources.stream().anyMatch(source -> source.item().is(Items.CLAY))
                            && sources.stream().anyMatch(source -> source.item().is(Items.CLAY_BALL)), "advanced scan updates actual EMI lookup without reload/relogin");
                    require(displayedItems().size() == 2 && displayedItems().stream().anyMatch(stack -> stack.is(Items.CLAY))
                            && displayedItems().stream().anyMatch(stack -> stack.is(Items.CLAY_BALL)),
                            "retained native EMI slots refresh after advanced scan without rebuilding widgets");
                    record("new advanced scan refreshes Aqua sources"); showAqua(); pending = "aqua-after-studying-clay-block";
                }
                case 5 -> {
                    client.setScreen(null);
                    operation = client.getSingleplayerServer().submit(() -> { KnowledgeAccess.get(actor(client)).reset(); KnowledgeEvents.sync(actor(client)); });
                    advance(6);
                }
                case 6 -> {
                    if (!complete() || ticks < 25 || !KnowledgeAccess.get(client.player).getScannedItems().isEmpty()) return;
                    require(aqua().sources().isEmpty(), "forgetting scanned items removes cached sources");
                    require(displayedItems().isEmpty(), "retained native EMI slots remove all forgotten item sources");
                    record("forgotten scans hide sources again");
                    showAqua(); pending = "aqua-no-scanned-sources";
                }
                case 7 -> finish(client, null);
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable failure) { finish(client, failure); }
    }

    private static boolean emiReady() {
        var manager = EmiApi.getRecipeManager();
        return manager != null && manager.getRecipes().stream().anyMatch(recipe -> recipe instanceof EmiAspectSourcesRecipe);
    }
    private static EmiAspectSourcesRecipe aqua() {
        return EmiApi.getRecipeManager().getRecipesByOutput(new AspectEmiStack(Aspect.WATER, 1)).stream()
                .filter(recipe -> recipe instanceof EmiAspectSourcesRecipe)
                .map(recipe -> (EmiAspectSourcesRecipe) recipe).filter(recipe -> recipe.aspect() == Aspect.WATER)
                .findFirst().orElseThrow(() -> new IllegalStateException("Actual EMI aspect-output lookup has no Aqua sources page"));
    }
    private static void showAqua() {
        EmiApi.displayRecipes(new AspectEmiStack(Aspect.WATER, 1));
        require(Minecraft.getInstance().screen != null && Minecraft.getInstance().screen.getClass().getName().startsWith("dev.emi.emi."), "native Aqua focus opens real EMI recipes UI");
    }
    private void retainActualSlots() {
        var recipe = aqua();
        recipe.addWidgets(new WidgetHolder() {
            @Override public int getWidth() { return recipe.getDisplayWidth(); }
            @Override public int getHeight() { return recipe.getDisplayHeight(); }
            @Override public <T extends Widget> T add(T widget) {
                if (widget instanceof SlotWidget slot) retainedSlots.add(slot);
                return widget;
            }
        });
        require(!retainedSlots.isEmpty(), "source recipe builds actual EMI slot widgets");
    }
    private List<ItemStack> displayedItems() {
        return retainedSlots.stream().flatMap(slot -> slot.getStack().getEmiStacks().stream())
                .map(stack -> stack.getItemStack()).filter(stack -> !stack.isEmpty()).toList();
    }
    private boolean complete() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void advance(int next) { stage = next; ticks = 0; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void record(String label) {
        checks.add(Map.of("check", label, "passed", true, "sources", aqua().sources().stream().map(source ->
                Map.of("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.item().getItem()).toString(),
                        "aspect", Aspect.WATER.getTag(), "amount", source.amount())).toList()));
    }
    private void finish(Minecraft client, Throwable error) {
        if (stopped) return; stopped = true;
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", error == null ? "PASS" : "FAILED"); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "fresh1.21.1 world, supplied @clay_ball/@dirt and #clay scan markers through the server API, actual EMI and knowledge packets; not survival acquisition");
            report.put("checks", checks); report.put("captures", captures);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("emi-aspect-sources-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
