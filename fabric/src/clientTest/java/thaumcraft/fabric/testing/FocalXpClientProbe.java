package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import dev.architectury.networking.NetworkManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.gui.GuiFocalManipulator;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;
import thaumcraft.common.lib.network.FocalManipulatorStartPayload;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.utils.ExperiencePoints;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Focused native GUI/payment regression with supplied research, focus and XP fixtures. */
public final class FocalXpClientProbe implements ClientModInitializer {
    private static final BlockPos TABLE = new BlockPos(0, 101, 0);
    private static final int PRICE = 112;
    private final String runId = "focal-xp-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private final long highBefore = ExperiencePoints.atLevel(30) + 17;
    private final long lowBefore = ExperiencePoints.atLevel(8) + 17;
    private CompletableFuture<?> operation;
    private CompletableFuture<State> sample;
    private FocalManipulatorStartPayload replay;
    private Path directory;
    private int stage, ticks, total, captureFrames;
    private boolean created, stopped, dispatched, reportedCaptureMismatch;
    private String pendingCapture;
    private java.util.UUID highSession;

    private record State(long points, int level, int total, float progress, int jobSize, int rank,
                         long revision, List<Short> upgrades) {
        Map<String, Object> report() {
            return Map.of("points", points, "level", level, "total", total, "progress", progress,
                    "job_size", jobSize, "rank", rank, "revision", revision, "upgrades", upgrades);
        }
    }

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.focalXpClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-focal-xp-client"), "isolated client directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
            if (pendingCapture == null || dispatched || stopped || !(current instanceof GuiFocalManipulator)) return;
            int localMouseX = mouseX - (current.width - 192) / 2;
            int localMouseY = mouseY - (current.height - 233) / 2;
            if (localMouseX < 108 || localMouseX >= 156 || localMouseY < 58 || localMouseY >= 74) {
                if (!reportedCaptureMismatch) {
                    reportedCaptureMismatch = true;
                    double[] cursorX = {0}, cursorY = {0};
                    GLFW.glfwGetCursorPos(client.getWindow().getWindow(), cursorX, cursorY);
                    System.out.println("Focal XP capture mouse mismatch " + pendingCapture + ": render=" + mouseX + "," + mouseY
                            + " screen=" + current.width + "x" + current.height + " local=" + localMouseX + "," + localMouseY
                            + " handler=" + client.mouseHandler.xpos() + "," + client.mouseHandler.ypos()
                            + " active=" + client.isWindowActive() + " glfw=" + cursorX[0] + "," + cursorY[0]);
                }
                captureFrames = 0; hoverXp(client); return;
            }
            if (++captureFrames < 3) return;
            dispatched = true; graphics.flush();
            String label = pendingCapture, file = runId + '-' + label + ".png";
            Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), ignored -> client.execute(() -> {
                try {
                    Path image = directory.resolve("screenshots").resolve(file);
                    require(Files.isRegularFile(image) && Files.size(image) > 1000, "native focal GUI screenshot exists");
                    captures.add(Map.of("label", label, "file", image.toString(), "bytes", Files.size(image),
                            "render_mouse_local", List.of(localMouseX, localMouseY),
                            "window_active", client.isWindowActive(), "language", client.getLanguageManager().getSelected()));
                    pendingCapture = null; dispatched = false; reportedCaptureMismatch = false; captureFrames = 0; advance(stage + 1);
                } catch (Throwable failure) { finish(client, failure); }
            }));
        }));
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 3600 || ++ticks > 1000) throw new IllegalStateException("Focal XP client timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    if (!client.getLanguageManager().getSelected().equals("ru_ru")) {
                        client.getLanguageManager().setSelected("ru_ru"); client.options.languageCode = "ru_ru";
                        client.reloadResourcePacks(); return;
                    }
                    created = true; client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.renderDistance().set(2); client.options.simulationDistance().set(5);
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Focal XP supplied fixtures", GameType.SURVIVAL, false, Difficulty.PEACEFUL,
                                    true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(73042026L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || pendingCapture != null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    require(FocalUpgradeCosts.xpPoints(1) == PRICE, "rank one costs exactly112 XP points");
                    operation = client.getSingleplayerServer().submit(() -> setup(client));
                    advance(1);
                }
                case 1 -> {
                    if (!completed() || ticks < 30 || !(client.level.getBlockEntity(TABLE) instanceof TileFocalManipulator)
                            || !KnowledgeAccess.get(client.player).isResearchComplete("FOCALMANIPULATION") || !clientPoints(client, 111)) return;
                    operation = client.getSingleplayerServer().submit(() -> serverTile(client).openMenu(actor(client)));
                    advance(2);
                }
                case 2 -> {
                    if (!completed() || ticks < 20 || !ready(client)) return;
                    guiClick(client, 72, 112); // Actual rank-one Potency icon, second in the native Fire focus list.
                    selectedQuote(client);
                    require(!startEnabled(client), "actual GUI START refuses111 points against112 cost");
                    hoverXp(client); pendingCapture = "rank1-112-points-disabled-at111";
                }
                case 3 -> {
                    guiClick(client, 80, 92); guiClick(client, 80, 92);
                    require(field(client.screen, "pending") == null, "unaffordable GUI clicks dispatch no pending START");
                    advance(4);
                }
                case 4 -> {
                    if (ticks < 25) return;
                    require(clientPoints(client, 111), "refused GUI clicks preserve native client XP gauge");
                    operation = client.getSingleplayerServer().submit(() -> {
                        State refused = snapshot(client);
                        require(refused.points() == 111 && refused.total() == 111 && refused.jobSize() == 0,
                                "unaffordable selected GUI START preserves server XP and creates no job");
                        checks.add(Map.of("check", "111 points refuse112 price without debit", "passed", true, "server", refused.report()));
                        fixturePoints(actor(client), Math.toIntExact(highBefore));
                    });
                    advance(5);
                }
                case 5 -> {
                    if (!completed() || ticks < 20 || !ready(client) || !clientPoints(client, highBefore)) return;
                    selectedQuote(client);
                    require(startEnabled(client) && client.player.experienceLevel == 30,
                            "level30 plus17 points enables the same112-point GUI quote");
                    hoverXp(client); pendingCapture = "level30-plus17-ready-112-point-tooltip";
                }
                case 6 -> {
                    ContainerFocalManipulator menu = menu(client);
                    highSession = menu.sessionId();
                    replay = new FocalManipulatorStartPayload(menu.containerId, menu.sessionId(), menu.selectionRevision(), FocusUpgradeType.potency.id);
                    clickStartTwice(client);
                    advance(7);
                }
                case 7 -> {
                    if (ticks < 20 || !clientPoints(client, highBefore - PRICE) || clientTile(client).getSize() <= 0) return;
                    sample = client.getSingleplayerServer().submit(() -> verifyPaid(client, highBefore - PRICE));
                    advance(8);
                }
                case 8 -> {
                    if (!sample.isDone()) return;
                    State paid = sample.join(); requireGauge(client, paid);
                    checks.add(Map.of("check", "actual GUI START at level30 pays exactly112 native points", "passed", true,
                            "before_points", highBefore, "after_server", paid.report(), "after_client_points", ExperiencePoints.available(client.player)));
                    hoverXp(client); pendingCapture = "level30-paid112-pending-vis-job";
                }
                case 9 -> {
                    // Real wire replay of the original session/revision, after the genuine GUI admission.
                    NetworkManager.sendToServer(replay);
                    guiClick(client, 80, 92);
                    advance(10);
                }
                case 10 -> {
                    if (ticks < 25) return;
                    sample = client.getSingleplayerServer().submit(() -> verifyPaid(client, highBefore - PRICE));
                    advance(11);
                }
                case 11 -> {
                    if (!sample.isDone()) return;
                    State stable = sample.join(); requireGauge(client, stable);
                    checks.add(Map.of("check", "pending clicks and replayed original wire intent cannot debit again", "passed", true,
                            "server", stable.report()));
                    guiClick(client, 96, 68); // Actual native physical focus-slot pickup cancels the paid job.
                    advance(12);
                }
                case 12 -> {
                    if (ticks < 25 || !menu(client).getSlot(0).getItem().isEmpty() || clientTile(client).getSize() != 0) return;
                    operation = client.getSingleplayerServer().submit(() -> {
                        State cancelled = snapshot(client);
                        require(cancelled.points() == highBefore - PRICE && cancelled.total() == highBefore - PRICE
                                        && cancelled.jobSize() == 0 && serverTile(client).getItem(0).isEmpty(),
                                "native focus pickup cancels without another XP charge or a refund");
                        checks.add(Map.of("check", "physical slot pickup cancels paid job without extra XP changes", "passed", true,
                                "server", cancelled.report()));
                        var player = actor(client); player.closeContainer(); player.getInventory().clearContent();
                        serverTile(client).setItem(0, new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()));
                        fixturePoints(player, Math.toIntExact(lowBefore));
                        serverTile(client).openMenu(player);
                    });
                    advance(13);
                }
                case 13 -> {
                    if (!completed() || ticks < 25 || !ready(client) || menu(client).sessionId().equals(highSession)
                            || !clientPoints(client, lowBefore)) return;
                    guiClick(client, 72, 112); selectedQuote(client);
                    require(startEnabled(client) && client.player.experienceLevel == 8,
                            "level8 plus17 points enables the identical112-point quote");
                    hoverXp(client); pendingCapture = "level8-plus17-ready-same112-point-tooltip";
                }
                case 14 -> { clickStartTwice(client); advance(15); }
                case 15 -> {
                    if (ticks < 20 || !clientPoints(client, lowBefore - PRICE) || clientTile(client).getSize() <= 0) return;
                    sample = client.getSingleplayerServer().submit(() -> verifyPaid(client, lowBefore - PRICE));
                    advance(16);
                }
                case 16 -> {
                    if (!sample.isDone()) return;
                    State paid = sample.join(); requireGauge(client, paid);
                    checks.add(Map.of("check", "actual GUI START at level8 also pays exactly112 points", "passed", true,
                            "before_points", lowBefore, "after_server", paid.report(), "after_client_points", ExperiencePoints.available(client.player)));
                    hoverXp(client); pendingCapture = "level8-paid112-leaves17-points";
                }
                case 17 -> {
                    operation = client.getSingleplayerServer().submit(() -> {
                        var player = actor(client); player.closeContainer(); player.getInventory().clearContent();
                        ItemStack rankFour = new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get());
                        var focus = (ItemFocusBasic) rankFour.getItem();
                        for (int rank = 1; rank <= 4; rank++)
                            require(focus.applyUpgrade(rankFour, FocusUpgradeType.frugal, rank), "declared rank-four GUI fixture applies valid upgrade history");
                        serverTile(client).setItem(0, rankFour);
                        fixturePoints(player, Math.toIntExact(ExperiencePoints.atLevel(40) + 17));
                        serverTile(client).openMenu(player);
                    });
                    advance(18);
                }
                case 18 -> {
                    if (!completed() || ticks < 25 || !ready(client)
                            || !clientPoints(client, ExperiencePoints.atLevel(40) + 17)) return;
                    guiClick(client, 72, 112);
                    var preview = (FocalUpgradeCosts.Plan) field(client.screen, "preview");
                    require(preview != null && preview.rank() == 5 && preview.xpPoints() == 2920 && startEnabled(client),
                            "actual rank-five GUI quotes2920 points for the supplied upgraded focus");
                    checks.add(Map.of("check", "actual rank-five preview quotes2920 points for visual price/tooltip inspection", "passed", true,
                            "rank", preview.rank(), "price_points", preview.xpPoints(), "available_points", ExperiencePoints.available(client.player),
                            "fixture", "supplied Fire focus with four valid Frugal upgrades; no rank-five payment or acquisition claim"));
                    hoverXp(client); pendingCapture = "rank5-2920-point-price-and-tooltip";
                }
                case 19 -> finish(client, null);
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable failure) { finish(client, failure); }
    }

    private void setup(Minecraft client) {
        var player = actor(client); var level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL);
        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        for (BlockPos pos : BlockPos.betweenClosed(-3, 100, -3, 3, 100, 4)) level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(TABLE, ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get().defaultBlockState(), 3);
        require(level.getBlockEntity(TABLE) instanceof TileFocalManipulator, "native focal block creates its registered tile");
        serverTile(client).setItem(0, new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()));
        player.getInventory().clearContent(); player.teleportTo(.5, 101, 3.5);
        var knowledge = KnowledgeAccess.get(player); knowledge.reset(); knowledge.addDiscoveredPrimalAspects();
        knowledge.addResearch("FOCALMANIPULATION"); KnowledgeEvents.sync(player);
        fixturePoints(player, 111);
        player.inventoryMenu.sendAllDataToRemote();
    }

    /** Declared fixture baseline only; all tested debits go through actual GUI/network admission. */
    private static void fixturePoints(ServerPlayer player, int points) {
        player.experienceLevel = 0; player.experienceProgress = 0; player.totalExperience = 0;
        player.giveExperiencePoints(points);
        require(ExperiencePoints.available(player) == points && player.totalExperience == points, "native grant sets exact declared fixture points");
    }

    private static State snapshot(Minecraft client) {
        var player = actor(client); var tile = serverTile(client);
        var upgrades = new ArrayList<Short>();
        ItemStack stack = tile.getItem(0);
        if (stack.getItem() instanceof ItemFocusBasic focus) for (short id : focus.getAppliedUpgrades(stack)) upgrades.add(id);
        return new State(ExperiencePoints.available(player), player.experienceLevel, player.totalExperience,
                player.experienceProgress, tile.getSize(), tile.getRank(), tile.getSelectionRevision(), List.copyOf(upgrades));
    }

    private static State verifyPaid(Minecraft client, long expected) {
        State state = snapshot(client);
        require(state.points() == expected && state.total() == expected, "server spends exactly112 XP points in native gauge and total");
        require(state.jobSize() > 0 && state.rank() == 1 && state.upgrades().size() == 5 && state.upgrades().stream().allMatch(id -> id == -1),
                "without supplied vis the XP-paid job remains pending and applies no free upgrade");
        return state;
    }

    private static void requireGauge(Minecraft client, State server) {
        require(ExperiencePoints.available(client.player) == server.points() && client.player.experienceLevel == server.level()
                        && client.player.totalExperience == server.total() && Float.compare(client.player.experienceProgress, server.progress()) == 0,
                "actual XP packets synchronize client level/progress/total with authoritative server gauge");
    }

    private static boolean clientPoints(Minecraft client, long points) { return ExperiencePoints.available(client.player) == points; }
    private boolean completed() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static TileFocalManipulator serverTile(Minecraft client) { return (TileFocalManipulator) actor(client).serverLevel().getBlockEntity(TABLE); }
    private static TileFocalManipulator clientTile(Minecraft client) { return menu(client).tile(); }
    private static ContainerFocalManipulator menu(Minecraft client) { return (ContainerFocalManipulator) client.player.containerMenu; }

    private static boolean ready(Minecraft client) throws ReflectiveOperationException {
        return client.screen instanceof GuiFocalManipulator && client.player.containerMenu instanceof ContainerFocalManipulator
                && (boolean) field(client.screen, "ready") && clientTile(client) != null;
    }

    private static void selectedQuote(Minecraft client) throws ReflectiveOperationException {
        require((int) field(client.screen, "selected") == FocusUpgradeType.potency.id, "actual GUI Potency choice is selected");
        var preview = (FocalUpgradeCosts.Plan) field(client.screen, "preview");
        require(preview != null && preview.rank() == 1 && preview.xpPoints() == PRICE, "actual selected GUI preview is112 XP points");
    }

    private static boolean startEnabled(Minecraft client) throws ReflectiveOperationException {
        Method method = GuiFocalManipulator.class.getDeclaredMethod("startEnabled"); method.setAccessible(true);
        return (boolean) method.invoke(client.screen);
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = GuiFocalManipulator.class.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }

    private static void clickStartTwice(Minecraft client) throws ReflectiveOperationException {
        require(startEnabled(client), "real GUI is ready for a paid START");
        guiClick(client, 80, 92);
        Object pending = field(client.screen, "pending");
        require(pending != null, "real GUI creates one pending intent before server acknowledgement");
        guiClick(client, 80, 92);
        require(field(client.screen, "pending") == pending, "second click preserves the same pending intent");
    }

    private static void guiClick(Minecraft client, int x, int y) {
        require(client.screen instanceof GuiFocalManipulator, "native focal screen receives actual input");
        int px = (client.screen.width - 192) / 2 + x, py = (client.screen.height - 233) / 2 + y;
        client.screen.mouseMoved(px, py);
        require(client.screen.mouseClicked(px, py, 0), "actual focal GUI hitbox consumes the click");
        client.screen.mouseReleased(px, py, 0);
    }

    private static void hoverXp(Minecraft client) {
        GLFW.glfwFocusWindow(client.getWindow().getWindow());
        int x = (client.screen.width - 192) / 2 + 135, y = (client.screen.height - 233) / 2 + 65;
        double nativeX = x * client.getWindow().getScreenWidth() / (double) client.screen.width;
        double nativeY = y * client.getWindow().getScreenHeight() / (double) client.screen.height;
        // A repeated warp to the current native position emits no motion callback.
        GLFW.glfwSetCursorPos(client.getWindow().getWindow(), nativeX - 2, nativeY);
        GLFW.glfwSetCursorPos(client.getWindow().getWindow(), nativeX, nativeY);
        client.screen.mouseMoved(x, y);
    }

    private void advance(int next) { stage = next; ticks = 0; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    private void finish(Minecraft client, Throwable error) {
        if (stopped) return; stopped = true;
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", error == null ? "PASS" : "FAILED"); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "fresh 1.21.1 flat world; supplied native focal manipulator, two plain Fire foci and one Fire focus with four valid Frugal upgrades for rank-five layout, FOCALMANIPULATION research and explicit server XP baselines 111, level 30 + 17 points, level 8 + 17 points and level 40 + 17 points through native giveExperiencePoints after declared fixture reset. Survival actor, no supplied vis. All tested START/debit/replay/pickup actions use actual GUI or wire; not survival acquisition or full upgrade completion.");
            report.put("rank1_price_points", PRICE); report.put("checks", checks); report.put("captures", captures);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("focal-xp-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
