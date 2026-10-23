package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.architectury.event.events.client.ClientTickEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.client.gui.GuiFocusSelect;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.ResearchNoteRequestPayload;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.research.ResearchNoteGenerator;
import thaumcraft.common.lib.research.ResearchNotePuzzle;
import thaumcraft.common.lib.research.ResearchNoteIssuance;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.common.tiles.TileTable;
import thaumcraft.common.tiles.TileResearchTable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/** No OS input: invokes the same client item and screen handlers as a player. */
public final class BookClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("thaumcraft-client-smoke");
    private static final long CAPTURE_TIMEOUT_NANOS = 10_000_000_000L;
    /** How long a stage may wait for a vanilla screen button that is not built yet. */
    private static final int SCREEN_RETRY_TICKS = 200;
    private enum Stage {
        RESEARCH_HANDS, WAIT_WORLD, WAIT_RESTART_PROBE, ENGLISH_RELOAD, WAIT_ITEM, BROWSER, OPEN_BOOK, BOOK, BACK_TO_BROWSER, OPEN_RESEARCH,
        RESEARCH, NEXT_RESEARCH, ILLUSTRATED, PREVIOUS_RESEARCH, PREVIOUS_CAPTURE,
        RUSSIAN_RELOAD, RUSSIAN_SIZE, RUSSIAN_BROWSER, OPEN_RUSSIAN_BOOK, RUSSIAN_BOOK,
        RUSSIAN_NEXT, RUSSIAN_PAGINATION, PURCHASE_RELOAD, PURCHASE_SIZE,
        PURCHASE_SETUP, PURCHASE_LOW_SYNC, PURCHASE_LOW_CAPTURE, PURCHASE_LOW_CLICK,
        PURCHASE_LOW_VERIFY, PURCHASE_FUNDED_SYNC, PURCHASE_SEND, PURCHASE_COMPLETE,
        PURCHASE_READ, PURCHASE_PAGE, PURCHASE_REOPEN, PURCHASE_REPLAY_VERIFY,
        NOTE_SETUP, NOTE_SYNC, NOTE_INVENTORY, DISCOVERY_CAPTURE, NOTE_USE,
        NOTE_LEARNED, NOTE_REPLAY_SYNC, NOTE_REPLAY_USE, NOTE_REPLAY_VERIFY,
        ISSUANCE_SETUP, ISSUANCE_BOOK_SYNC, ISSUANCE_BROWSER, ISSUANCE_BASELINE,
        ISSUANCE_MISSING_CAPTURE, ISSUANCE_MISSING_CLICK, ISSUANCE_FUND,
        ISSUANCE_SEND, ISSUANCE_RECEIVE, ISSUANCE_DUPLICATE, ISSUANCE_VERIFY,
        ISSUANCE_INVENTORY, ISSUANCE_CAPTURE,
        TABLE_SETUP, TABLE_SYNC, TABLE_PLACE_SECOND, TABLE_BASE_VERIFY, TABLE_BASE_CAPTURE,
        TABLE_CONVERT, TABLE_CONVERT_VERIFY, TABLE_OPEN_MAIN, TABLE_MAIN_MENU,
        TABLE_TOOL_OUT, TABLE_TOOL_BACK, TABLE_NOTE_IN, TABLE_NOTE_SYNC,
        TABLE_CLOSE_WORLD, TABLE_WORLD_CAPTURE, TABLE_OPEN_SECONDARY, TABLE_SECONDARY_MENU,
        TABLE_MENU_CAPTURE, TABLE_SAVE_VERIFY,
        TABLE_PREVIEW_SETUP, TABLE_PREVIEW_SYNC, TABLE_PREVIEW_INVENTORY,
        TABLE_PREVIEW_DROP, TABLE_PREVIEW_DROP_SYNC, TABLE_PREVIEW_WORLD,
        EDIT_SETUP, EDIT_OPEN, EDIT_READY, EDIT_ACTION, EDIT_WAIT, EDIT_SOLVED_CAPTURE,
        EDIT_TAKE, EDIT_LEARN, EDIT_LEARNED, EARLY_CONTENT, ALCHEMY, DISTILLATION, FOCUS, TUBES, INFUSION, EQUIPMENT, CAPS, GOGGLES, ROBE_SHOCK, EXCAVATION, FROST, VOID_JAR, VALVE, TUBE_FILTER, BUFFER, CENTRIFUGE, CRYSTALLIZER, STABILIZER, VISNET, FOCAL, BORE, FINISH
    }

    private final List<Map<String, Object>> actions = new ArrayList<>();
    private final List<Map<String, Object>> screenshots = new ArrayList<>();
    private final Map<String, Object> purchaseEvidence = new LinkedHashMap<>();
    private final Map<String, Object> noteEvidence = new LinkedHashMap<>();
    private final Map<String, Object> issuanceEvidence = new LinkedHashMap<>();
    private final Map<String, Object> tableEvidence = new LinkedHashMap<>();
    private final Map<String, Object> worldOpenEvidence = new LinkedHashMap<>();
    private final AtomicInteger noteRequestPackets = new AtomicInteger();
    private final long started = System.nanoTime();
    private final String runId = System.getProperty("thaumcraft.clientSmokeRunId", "client-smoke-" + UUID.randomUUID());
    private final boolean equipmentOnly = "equipment".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean researchHandsOnly = "researchhands".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private ResearchHandsClientSequence researchHands;
    private final boolean capsOnly = "caps".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean gogglesOnly = "goggles".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean clothingOnly = "clothing".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean excavationOnly = "excavation".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean frostOnly = "frost".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean voidJarOnly = "voidjar".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean valveOnly = "valve".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean tubeFilterOnly = "tubefilter".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean bufferOnly = "buffer".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean centrifugeOnly = "centrifuge".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean crystallizerOnly = "crystallizer".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean stabilizerOnly = "stabilizer".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean visnetOnly = "visnet".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean focalOnly = "focal".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private final boolean boreOnly = "bore".equals(System.getProperty("thaumcraft.clientSmokeScope", "full"));
    private BlockPos equipmentOrigin;
    private BlockPos capsCenter;
    private Stage stage = Stage.WAIT_WORLD;
    private int stageTicks, illustrationTurns;
    private int editStep;
    private EarlyContentClientSequence earlyContent;
    private AlchemyClientSequence alchemy;
    private DistillationClientSequence distillation;
    private FocusClientSequence focus;
    private TubeClientSequence tubes;
    private InfusionClientSequence infusion;
    private ThaumiumEquipmentClientSequence equipment;
    private ThaumiumCapsClientSequence caps;
    private GogglesClientSequence goggles;
    private RobeShockClientSequence robeShock;
    private ExcavationClientSequence excavation;
    private FrostClientSequence frost;
    private VoidJarClientSequence voidJar;
    private ValveClientSequence valve;
    private TubeFilterClientSequence tubeFilter;
    private TubeBufferClientSequence buffer;
    private CentrifugeClientSequence centrifuge;
    private CrystallizerClientSequence crystallizer;
    private NodeStabilizerClientSequence stabilizer;
    private VisNetClientSequence visnet;
    private VisNetTailObserver visnetTail;
    private FocalManipulatorClientSequence focal;
    private BoreClientSequence bore;
    private boolean capturePending, captureDispatched, finished;
    private boolean worldConfirmationCapturePending, worldConfirmationCaptured;
    private long captureStartedNanos;
    private String pendingScreenshotName;
    private Stage pendingScreenshotStage;
    private Screen pendingScreenshotScreen;
    private String pendingHoverKey;
    private int pendingInventorySlot = -1;
    private CompletableFuture<Void> serverSetup, languageReload;
    private CompletableFuture<NoteFixture> noteSetup;
    private NoteFixture noteFixture;
    private CompletableFuture<IssuanceBaseline> issuanceSetup;
    private IssuanceBaseline issuanceBaseline;
    private CompletableFuture<TableFixture> tableSetup;
    private TableFixture tableFixture;
    private CompletableFuture<PreviewDrop> previewDrop;
    private PreviewDrop previewDropped;
    private CompletableFuture<String> restartProbe;
    private boolean restartProbeChecked;
    private Path restartExpectation;
    private Path runDirectory;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.clientSmoke")) return;
        runDirectory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!runDirectory.getFileName().toString().equals("run-clienttest")) {
            throw new IllegalStateException("Smoke harness is restricted to the isolated run-clienttest directory");
        }
        if (visnetOnly || focalOnly || boreOnly) ClientTestServerLifecycle.init();
        if (boreOnly) {
            try {
                var bindings=BoreNativeBinding.installed();
                BoreClientSequence.nativeHooksInstalled(2,bindings.fingerprint(),bindings.resourceSha256());
            } catch (Exception error) {
                throw new IllegalStateException("Cannot bind the private Bore observer manifest", error);
            }
        }
        writeResult("RUNNING", null);
        // The vanilla world confirmation is captured from its own rendered frame
        // so the run keeps an honest picture of what the player was asked.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen confirmation)) return;
            worldConfirmationCapturePending = true;
            worldOpenEvidence.put("vanilla_confirmation_title", confirmation.getTitle().getString());
            ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, delta) -> {
                if (!worldConfirmationCapturePending || worldConfirmationCaptured || finished) return;
                worldConfirmationCapturePending = false;
                worldConfirmationCaptured = true;
                try {
                    graphics.flush();
                    String name = runId + "-00-vanilla-world-confirmation.png";
                    worldOpenEvidence.put("dialog_screenshot", runDirectory.resolve("screenshots").resolve(name).toString());
                    Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
                            message -> LOGGER.info("Client smoke: captured the vanilla world confirmation dialog ({})", message.getString()));
                } catch (Throwable error) {
                    fail(client, error);
                    return;
                }
                fail(client, new IllegalStateException("The vanilla client asked to confirm the experimental world settings; "
                        + "the TC4 port must open its own world without that question (the captured frame is the evidence)"));
            });
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!supportsCapture(screen)) return;
            ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, delta) -> {
                if (!capturePending || captureDispatched || pendingScreenshotScreen != rendered
                        || client.getOverlay() != null || finished) return;
                captureDispatched = true;
                try {
                    if (pendingHoverKey != null) {
                        var point = researchPoint((GuiResearchBrowser) rendered, pendingHoverKey);
                        // Exercise the existing native hover renderer with a
                        // scripted point, without moving the user's OS cursor.
                        graphics.flush();
                        rendered.render(graphics, point[0], point[1], delta);
                    } else if (pendingInventorySlot >= 0) {
                        var point = inventoryPoint(client, (InventoryScreen) rendered, pendingInventorySlot);
                        // Run vanilla slot hover, item models and item tooltip rendering.
                        graphics.flush();
                        rendered.render(graphics, point[0], point[1], delta);
                    } else if (rendered instanceof thaumcraft.client.gui.GuiFocalManipulator) {
                        // Use the real screen's icon-hover renderer, not an
                        // invented tooltip or a mutation of client tile state.
                        boolean potencyHover = pendingScreenshotName.startsWith("167-");
                        graphics.flush();
                        rendered.render(graphics, potencyHover ? (rendered.width - 192) / 2 + 72 : -1,
                                potencyHover ? (rendered.height - 233) / 2 + 112 : -1, delta);
                    } else if (focalOnly && rendered instanceof InventoryScreen) {
                        var point = inventoryPoint(client, (InventoryScreen) rendered,
                                client.player.getInventory().selected);
                        graphics.flush();
                        rendered.render(graphics, point[0], point[1], delta);
                    } else if (researchHandsOnly && rendered instanceof GuiResearchTable && pendingScreenshotName.startsWith("203-")) {
                        // Show the actual copy widget's native cost tooltip before payment.
                        graphics.flush();
                        rendered.render(graphics, (rendered.width - 255) / 2 + 48,
                                (rendered.height - 255) / 2 + 16, delta);
                    } else if (rendered instanceof GuiResearchTable || rendered instanceof GuiArcaneWorkbench || rendered instanceof GuiAlchemyFurnace
                             || rendered instanceof thaumcraft.client.gui.GuiArcaneBore
                             || rendered instanceof net.minecraft.client.gui.screens.inventory.FurnaceScreen
                             || rendered instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen
                            || rendered instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen) {
                        graphics.flush();
                        rendered.render(graphics, -1, -1, delta);
                    } else if (rendered instanceof GuiFocusSelect && !clothingOnly && !excavationOnly && !frostOnly && !focalOnly) {
                        var point = FocusClientSequence.hoverPoint(rendered);
                        graphics.flush();
                        rendered.render(graphics, point[0], point[1], delta);
                    }
                    // Text/widgets are buffered. Read back only this completed screen render.
                    graphics.flush();
                    grabRenderedFrame(client, pendingScreenshotName, pendingScreenshotStage);
                } catch (Throwable error) {
                    fail(client, error);
                }
            });
        });
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (!capturePending || captureDispatched || pendingScreenshotScreen != null
                    || client.screen != null || client.getOverlay() != null || finished) return;
            captureDispatched = true;
            try {
                graphics.flush();
                grabRenderedFrame(client, pendingScreenshotName, pendingScreenshotStage);
            } catch (Throwable error) {
                fail(client, error);
            }
        });
        ClientTickEvent.CLIENT_POST.register(this::tick);
        LOGGER.info("Client smoke armed in {}", runDirectory);
    }

    private void tick(Minecraft client) {
        if (finished) return;
        try {
            client.options.pauseOnLostFocus = false;
            // The focused check starts with the real saved inventory; it does
            // not replay the unrelated research/distillation/infusion story.
            if ((System.nanoTime() - started) / 1_000_000_000L > (boreOnly ? 1080 : equipmentOnly || capsOnly || gogglesOnly || clothingOnly || excavationOnly || frostOnly || voidJarOnly || valveOnly || tubeFilterOnly || bufferOnly || centrifugeOnly || crystallizerOnly || stabilizerOnly || visnetOnly || focalOnly ? 600 : 1200)) {
                throw new IllegalStateException("Client smoke timed out at " + stage + "; screen=" + screenName(client));
            }
            if (client.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen) {
                throw new IllegalStateException("Client disconnected during native login/play at " + stage + "; inspect the network error in this run's log");
            }
            if (client.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen confirmation) {
                // Vanilla 1.21.1 asks the player to confirm a world whose level stems
                // are not all vanilla-like (WorldOpenFlows.openWorldCheckWorldStem-
                // Compatibility / WorldDimensions.bake). The TC4 port supports its
                // own Outer Lands dimension completely, so the port reports the level
                // stem registry as stable and this dialog must never appear.
                // The dialog frame itself is captured by the screen hook, and the
                // captured frame ends the run as a failure.
                worldOpenEvidence.put("vanilla_confirmation_title", confirmation.getTitle().getString());
                if (!worldConfirmationCaptured) {
                    worldConfirmationCapturePending = true;
                    return;
                }
                throw new IllegalStateException("The vanilla client asked to confirm the experimental world settings; "
                        + "the TC4 port must open its own world without that question (the captured frame is the evidence)");
            }
            if (capturePending) {
                if (focal != null) focal.check();
                if (bore != null) bore.check();
                if (pendingScreenshotScreen != client.screen) {
                    throw new IllegalStateException("Screen changed while awaiting native screenshot " + pendingScreenshotName
                            + "; expected=" + (pendingScreenshotScreen == null ? "HUD" : pendingScreenshotScreen.getClass().getName())
                            + "; actual=" + screenName(client));
                }
                if (System.nanoTime() - captureStartedNanos > CAPTURE_TIMEOUT_NANOS) {
                    throw new IllegalStateException("Native screenshot did not finish within10seconds: " + pendingScreenshotName
                            + "; screen=" + screenName(client) + "; render_dispatched=" + captureDispatched);
                }
                return;
            }
            stageTicks++;
            switch (stage) {
                case RESEARCH_HANDS -> {
                    if (researchHands == null) researchHands = new ResearchHandsClientSequence();
                    if (researchHands.tick(client, name -> capture(client, name, Stage.RESEARCH_HANDS))) {
                        advance(client, Stage.FINISH, "Native table insert, combine, solve and paid copy cross-checked on client and server");
                    }
                }
                case WAIT_WORLD -> {
                    if (client.player == null || client.level == null || client.gameMode == null
                            || client.getSingleplayerServer() == null || stageTicks < 20) return;
                    if (researchHandsOnly) {
                        client.options.guiScale().set(2);
                        client.resizeDisplay();
                        advance(client, Stage.RESEARCH_HANDS, "Focused fresh-world research-table input fixture");
                        return;
                    }
                    if (boreOnly) {
                        if (!client.getLanguageManager().getSelected().equals("en_us")
                                || !client.player.getUUID().equals(UUID.fromString("13848433-7be7-350d-8b4a-c038819c2260"))
                                || !client.player.getGameProfile().getName().equals("Player653")) {
                            throw new IllegalStateException("Bore requires English and the actual accepted Player653 profile BEFORE fixtures");
                        }
                        client.options.guiScale().set(2);
                        client.resizeDisplay();
                        advance(client, Stage.BORE, "Fresh private accepted focal copy; genuine primary research and finite paid Bore acquisition, no earlier replay or resource initialization");
                        return;
                    }
                    if (focalOnly) {
                        if (!client.getLanguageManager().getSelected().equals("en_us")) {
                            throw new IllegalStateException("Focused focal launch requires the prepared English locale");
                        }
                        client.options.guiScale().set(2);
                        client.resizeDisplay();
                        advance(client, Stage.FOCAL, "Fresh private accepted M09b copy; no earlier content replay, XP or vis initialization");
                        return;
                    }
                    if (equipmentOnly || capsOnly || gogglesOnly || clothingOnly || excavationOnly || frostOnly || voidJarOnly || valveOnly || tubeFilterOnly || bufferOnly || centrifugeOnly || crystallizerOnly || stabilizerOnly || visnetOnly) {
                        if (!client.getLanguageManager().getSelected().equals("en_us")) {
                            throw new IllegalStateException("Focused equipment launch requires the prepared English locale");
                        }
                        client.options.guiScale().set(2);
                        client.resizeDisplay();
                        String[] coordinates = System.getProperty("thaumcraft.clientSmokeEquipmentOrigin", "").split(",");
                        if (coordinates.length != 3) throw new IllegalStateException("Focused equipment requires the accepted save's actual infusion workbench anchor");
                        equipmentOrigin = new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
                        if (capsOnly || gogglesOnly || clothingOnly || excavationOnly || frostOnly || voidJarOnly || valveOnly || tubeFilterOnly || bufferOnly || centrifugeOnly || crystallizerOnly || stabilizerOnly || visnetOnly) {
                            String[] center = System.getProperty("thaumcraft.clientSmokeInfusionCenter", "").split(",");
                            if (center.length != 3) throw new IllegalStateException("Focused caps require the existing formed altar anchor");
                            capsCenter = new BlockPos(Integer.parseInt(center[0]), Integer.parseInt(center[1]), Integer.parseInt(center[2]));
                        }
                        advance(client, visnetOnly ? Stage.VISNET : stabilizerOnly ? Stage.STABILIZER : crystallizerOnly ? Stage.CRYSTALLIZER : centrifugeOnly ? Stage.CENTRIFUGE : bufferOnly ? Stage.BUFFER : tubeFilterOnly ? Stage.TUBE_FILTER : valveOnly ? Stage.VALVE : voidJarOnly ? Stage.VOID_JAR : frostOnly ? Stage.FROST : excavationOnly ? Stage.EXCAVATION : clothingOnly ? Stage.ROBE_SHOCK : gogglesOnly ? Stage.GOGGLES : capsOnly ? Stage.CAPS : Stage.EQUIPMENT, "Focused content check: use a private copy of the existing saved inventory/altar; prior acquisition is not replayed or claimed in this run");
                        return;
                    }
                    if (!restartProbeChecked) {
                        restartExpectation = runDirectory.resolve("table-restart-expectation.json");
                        if (Files.isRegularFile(restartExpectation)) {
                            var expectation = JsonParser.parseString(Files.readString(restartExpectation, StandardCharsets.UTF_8)).getAsJsonObject();
                            String sourceRunId = expectation.get("source_run_id").getAsString();
                            if (sourceRunId.isBlank() || sourceRunId.equals(runId)) throw new IllegalStateException("Restart probe requires a different source process run id");
                            BlockPos main = restartPosition(expectation.getAsJsonArray("main"));
                            BlockPos secondary = restartPosition(expectation.getAsJsonArray("secondary"));
                            var variants = expectation.getAsJsonArray("variants");
                            if (variants.size() != 2) throw new IllegalStateException("Restart variants must contain main/secondary");
                            int mainVariant = variants.get(0).getAsInt(), secondaryVariant = variants.get(1).getAsInt();
                            var expected = TagParser.parseTag(expectation.get("expected_inventory_snbt").getAsString());
                            if (!expected.contains("Inventory", Tag.TAG_LIST) || expected.getList("Inventory", Tag.TAG_COMPOUND).size() != 2) {
                                throw new IllegalStateException("Restart expectation must contain both full inventory entries");
                            }
                            var expectedInventory = expected.getList("Inventory", Tag.TAG_COMPOUND).copy();
                            var server = client.getSingleplayerServer(); var playerId = client.player.getUUID();
                            restartProbe = CompletableFuture.supplyAsync(() -> {
                                var player = server.getPlayerList().getPlayer(playerId);
                                if (player == null) throw new IllegalStateException("Restart probe has no native server player");
                                var level = player.serverLevel();
                                // Native chunk loading reads the prior process save.
                                // Do not recreate the table or manually load its NBT.
                                level.getChunkAt(main); level.getChunkAt(secondary);
                                if (!level.getBlockState(main).is(ThaumcraftContent.TABLE.get())
                                        || !level.getBlockState(secondary).is(ThaumcraftContent.TABLE.get())
                                        || level.getBlockState(main).getValue(BlockTable.VARIANT) != mainVariant
                                        || level.getBlockState(secondary).getValue(BlockTable.VARIANT) != secondaryVariant
                                        || !(level.getBlockEntity(main) instanceof TileResearchTable tile)
                                        || !(level.getBlockEntity(secondary) instanceof TileTable)
                                        || BlockTable.findOpeningMain(level, secondary) != tile) {
                                    throw new IllegalStateException("Prior process research table state/shared target did not survive restart");
                                }
                                var actualInventory = tile.saveWithFullMetadata(player.registryAccess()).getList("Inventory", Tag.TAG_COMPOUND);
                                if (!expectedInventory.equals(actualInventory)) {
                                    throw new IllegalStateException("Prior process full table inventory changed across restart: expected="
                                            + expectedInventory + "; actual=" + actualInventory);
                                }
                                return sourceRunId;
                            }, server);
                            advance(client, Stage.WAIT_RESTART_PROBE, "Read-only startup probe checks the prior process table before any book/knowledge/table setup");
                            return;
                        }
                        restartProbeChecked = true;
                    }
                    if (!client.getLanguageManager().getSelected().equals("en_us")) {
                        client.getLanguageManager().setSelected("en_us");
                        client.options.languageCode = "en_us";
                        languageReload = client.reloadResourcePacks();
                        advance(client, Stage.ENGLISH_RELOAD, "Reset English locale for a reused smoke run");
                        return;
                    }
                    client.options.guiScale().set(2);
                    client.resizeDisplay();
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        if (player == null) throw new IllegalStateException("Integrated server has no local player");
                        player.setGameMode(GameType.CREATIVE);
                        var knowledge = KnowledgeAccess.get(player);
                        knowledge.addResearch("THAUMONOMICON");
                        knowledge.addResearch("RESEARCH");
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.THAUMONOMICON.get()));
                        player.inventoryMenu.broadcastChanges();
                        KnowledgeEvents.sync(player);
                    }, server);
                    advance(client, Stage.WAIT_ITEM, "Set up isolated server player and synchronize book/knowledge");
                }
                case WAIT_RESTART_PROBE -> {
                    if (!restartProbe.isDone()) return;
                    String sourceRunId = restartProbe.join();
                    var verified = runDirectory.resolve("table-restart-expectation." + runId + ".verified.json");
                    Files.move(restartExpectation, verified);
                    restartProbeChecked = true;
                    tableEvidence.put("previous_process_restart_verified", true);
                    tableEvidence.put("previous_process_restart_source_run_id", sourceRunId);
                    tableEvidence.put("previous_process_restart_inventory_comparison", "exact full native Inventory NBT list");
                    tableEvidence.put("previous_process_restart_expectation", verified.toString());
                    advance(client, Stage.WAIT_WORLD, "Verified persisted table/contents from the previous process; consumed the owned startup expectation");
                }
                case ENGLISH_RELOAD -> {
                    if (!languageReload.isDone()) return;
                    languageReload.join();
                    if (client.getOverlay() == null && stageTicks >= 20) {
                        advance(client, Stage.WAIT_WORLD, "English resources have reloaded");
                    }
                }
                case WAIT_ITEM -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    if (!client.player.getMainHandItem().is(ThaumcraftContent.THAUMONOMICON.get())
                            || !KnowledgeAccess.get(client.player).isResearchComplete("RESEARCH")) return;
                    client.setScreen(null);
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(client, Stage.BROWSER, "Normal client useItem sends the book use packet");
                }
                case BROWSER -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    capture(client, "01-browser-en.png", Stage.OPEN_BOOK);
                }
                case OPEN_BOOK -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickEntry(client, "THAUMONOMICON");
                    advance(client, Stage.BOOK, "Clicked mapped THAUMONOMICON entry");
                }
                case BOOK -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    capture(client, "02-thaumonomicon-en.png", Stage.BACK_TO_BROWSER);
                }
                case BACK_TO_BROWSER -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    clickButton(client, Component.translatable("gui.back").getString());
                    advance(client, Stage.OPEN_RESEARCH, "Clicked Book Back button to return to browser");
                }
                case OPEN_RESEARCH -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickEntry(client, "RESEARCH");
                    advance(client, Stage.RESEARCH, "Clicked mapped RESEARCH entry");
                }
                case RESEARCH -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    capture(client, "03-research-en.png", Stage.NEXT_RESEARCH);
                }
                case NEXT_RESEARCH -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    turnPage(client, "→", 2);
                    advance(client, Stage.ILLUSTRATED, "Clicked Next and verified page advances by two");
                }
                case ILLUSTRATED -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    if (!visibleIllustration(client.screen)) {
                        if (++illustrationTurns > 20) throw new IllegalStateException("No reachable illustrated RESEARCH spread");
                        turnPage(client, "→", 2);
                        stageTicks = 0;
                        return;
                    }
                    capture(client, "04-research-illustrated-en.png", Stage.PREVIOUS_RESEARCH);
                }
                case PREVIOUS_RESEARCH -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    turnPage(client, "←", -2);
                    advance(client, Stage.PREVIOUS_CAPTURE, "Clicked Previous and verified page decreases by two");
                }
                case PREVIOUS_CAPTURE -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    capture(client, "05-research-previous-en.png", Stage.RUSSIAN_RELOAD);
                }
                case RUSSIAN_RELOAD -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    clickButton(client, Component.translatable("gui.back").getString());
                    client.getLanguageManager().setSelected("ru_ru");
                    client.options.languageCode = "ru_ru";
                    client.options.guiScale().set(3);
                    languageReload = client.reloadResourcePacks();
                    advance(client, Stage.RUSSIAN_SIZE, "Reload Russian locale and select small GUI viewport");
                }
                case RUSSIAN_SIZE -> {
                    if (!languageReload.isDone()) return;
                    languageReload.join();
                    if (client.getOverlay() != null || stageTicks < 20) return;
                    client.resizeDisplay();
                    advance(client, Stage.RUSSIAN_BROWSER, "Resize and allow new small GUI frames to render");
                }
                case RUSSIAN_BROWSER -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    capture(client, "06-browser-ru-small.png", Stage.OPEN_RUSSIAN_BOOK);
                }
                case OPEN_RUSSIAN_BOOK -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickEntry(client, "THAUMONOMICON");
                    advance(client, Stage.RUSSIAN_BOOK, "Open Russian book at small GUI size");
                }
                case RUSSIAN_BOOK -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    capture(client, "07-thaumonomicon-ru-small.png", Stage.RUSSIAN_NEXT);
                }
                case RUSSIAN_NEXT -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    turnPage(client, "→", 2);
                    advance(client, Stage.RUSSIAN_PAGINATION, "Navigate overflow text on small Russian pages");
                }
                case RUSSIAN_PAGINATION -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    capture(client, "08-thaumonomicon-ru-small-next.png", Stage.PURCHASE_RELOAD);
                }
                case PURCHASE_RELOAD -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    clickButton(client, Component.translatable("gui.back").getString());
                    client.getLanguageManager().setSelected("en_us");
                    client.options.languageCode = "en_us";
                    client.options.guiScale().set(2);
                    languageReload = client.reloadResourcePacks();
                    advance(client, Stage.PURCHASE_SIZE, "Restore English/default GUI for native Secondary purchase");
                }
                case PURCHASE_SIZE -> {
                    if (!languageReload.isDone()) return;
                    languageReload.join();
                    if (client.getOverlay() != null || stageTicks < 20) return;
                    client.resizeDisplay();
                    advance(client, Stage.PURCHASE_SETUP, "Allow restored English browser frames to settle");
                }
                case PURCHASE_SETUP -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        var knowledge = KnowledgeAccess.get(player);
                        knowledge.removeResearch("ENCHFABRIC");
                        knowledge.setAspectPool("pannus", 3);
                        knowledge.setAspectPool("praecantatio", 2);
                        KnowledgeEvents.sync(player);
                    }, server);
                    purchaseEvidence.put("key", "ENCHFABRIC");
                    purchaseEvidence.put("canonical_costs", Map.of("pannus", 3, "praecantatio", 3));
                    advance(client, Stage.PURCHASE_LOW_SYNC, "Server supplies unfinished ENCHFABRIC and unaffordable pools 3/2");
                }
                case PURCHASE_LOW_SYNC -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    if (!purchaseMatches(client, false, 3, 2) || !ready(client, GuiResearchBrowser.class)) return;
                    clickCategoryTab(client, "ARTIFICE");
                    var browser = (GuiResearchBrowser) client.screen;
                    int graphX = (int) field(browser, "graphLeft") + (int) field(browser, "graphWidth") / 2;
                    int graphY = (int) field(browser, "graphTop") + (int) field(browser, "graphHeight") / 2;
                    var entry = ResearchCatalog.get("ENCHFABRIC");
                    if (!browser.mouseDragged(graphX, graphY, 0, -entry.column() * 24, -entry.row() * 24)) {
                        throw new IllegalStateException("Native graph pan did not center ENCHFABRIC");
                    }
                    advance(client, Stage.PURCHASE_LOW_CAPTURE, "Select Artifice and pan the native map to ENCHFABRIC");
                }
                case PURCHASE_LOW_CAPTURE -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    capture(client, "09-secondary-unaffordable-en.png", Stage.PURCHASE_LOW_CLICK);
                    pendingHoverKey = "ENCHFABRIC";
                }
                case PURCHASE_LOW_CLICK -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickUnfinished(client, "ENCHFABRIC");
                    requirePurchase(client, false, 3, 2);
                    advance(client, Stage.PURCHASE_LOW_VERIFY, "Unaffordable node click leaves client completion and pools unchanged");
                }
                case PURCHASE_LOW_VERIFY -> {
                    if (!ready(client, GuiResearchBrowser.class) || stageTicks < 20) return;
                    requirePurchase(client, false, 3, 2);
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        var knowledge = KnowledgeAccess.get(player);
                        if (knowledge.isResearchComplete("ENCHFABRIC") || knowledge.getAspectPoolFor("pannus") != 3
                                || knowledge.getAspectPoolFor("praecantatio") != 2) {
                            throw new IllegalStateException("Unaffordable UI click changed authoritative knowledge");
                        }
                        knowledge.setAspectPool("pannus", 6);
                        knowledge.setAspectPool("praecantatio", 6);
                        KnowledgeEvents.sync(player);
                    }, server);
                    purchaseEvidence.put("unaffordable_click_unchanged", true);
                    advance(client, Stage.PURCHASE_FUNDED_SYNC, "Verify server rejection state before funding authoritative pools 6/6");
                }
                case PURCHASE_FUNDED_SYNC -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    if (!purchaseMatches(client, false, 6, 6) || !ready(client, GuiResearchBrowser.class)) return;
                    advance(client, Stage.PURCHASE_SEND, "Client receives authoritative 6/6 pools and unfinished research");
                }
                case PURCHASE_SEND -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickUnfinished(client, "ENCHFABRIC");
                    requirePurchase(client, false, 6, 6);
                    purchaseEvidence.put("no_optimistic_completion_or_debit", true);
                    advance(client, Stage.PURCHASE_COMPLETE, "Native browser click sends intent; immediate client state stays unfinished with 6/6");
                }
                case PURCHASE_COMPLETE -> {
                    if (!purchaseMatches(client, true, 3, 3) || !ready(client, GuiResearchBrowser.class)) return;
                    purchaseEvidence.put("authoritative_client_snapshot", purchaseSnapshot(client));
                    advance(client, Stage.PURCHASE_READ, "Server sync completes ENCHFABRIC and debits exactly 3/3");
                }
                case PURCHASE_READ -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickEntry(client, "ENCHFABRIC");
                    requirePurchase(client, true, 3, 3);
                    advance(client, Stage.PURCHASE_PAGE, "Completed purchased entry opens its native text pages");
                }
                case PURCHASE_PAGE -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    capture(client, "10-secondary-purchased-entry-en.png", Stage.PURCHASE_REOPEN);
                }
                case PURCHASE_REOPEN -> {
                    if (!ready(client, GuiResearchRecipe.class)) return;
                    clickButton(client, Component.translatable("gui.back").getString());
                    advance(client, Stage.PURCHASE_REPLAY_VERIFY, "Return from purchased page to test completed-entry navigation");
                }
                case PURCHASE_REPLAY_VERIFY -> {
                    if (client.screen instanceof GuiResearchBrowser) {
                        if (!ready(client, GuiResearchBrowser.class)) return;
                        clickEntry(client, "ENCHFABRIC");
                        stageTicks = 0;
                        return;
                    }
                    if (!ready(client, GuiResearchRecipe.class) || stageTicks < 20) return;
                    requirePurchase(client, true, 3, 3);
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var knowledge = KnowledgeAccess.get(server.getPlayerList().getPlayer(playerId));
                        if (!knowledge.isResearchComplete("ENCHFABRIC") || knowledge.getAspectPoolFor("pannus") != 3
                                || knowledge.getAspectPoolFor("praecantatio") != 3) {
                            throw new IllegalStateException("Completed entry navigation spent research points again");
                        }
                    }, server);
                    purchaseEvidence.put("completed_entry_reopen_no_extra_debit", true);
                    advance(client, Stage.NOTE_SETUP, "Reopened completed entry; authoritative pools remain 3/3");
                }
                case NOTE_SETUP -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    client.setScreen(null);
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    noteSetup = CompletableFuture.supplyAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        if (player == null) throw new IllegalStateException("No server player for note fixture");
                        // Vanilla InventoryScreen redirects creative players to a different screen.
                        player.setGameMode(GameType.SURVIVAL);
                        // Protect only this isolated fixture player; survival paper/ink costs still apply.
                        player.setInvulnerable(true);
                        var knowledge = KnowledgeAccess.get(player);
                        knowledge.removeResearch("NITOR");
                        knowledge.addResearch("CRUCIBLE");
                        var generated = ResearchNoteGenerator.generate("NITOR", new Random(42));
                        if (generated.cells().size() != 19 || !new Cell(-2, 2, 1, "lux").equals(generated.cellAt(-2, 2))
                                || !new Cell(2, -2, 1, "ignis").equals(generated.cellAt(2, -2))) {
                            throw new IllegalStateException("NITOR fixture does not match original generated endpoints");
                        }
                        Map<String, String> path = Map.of("-1:2", "ignis", "0:1", "lux", "0:0", "ignis", "1:-1", "lux");
                        var cells = generated.cells().stream().map(cell -> path.containsKey(cell.coordinate())
                                ? new Cell(cell.q(), cell.r(), 2, path.get(cell.coordinate())) : cell).toList();
                        // A different researcher solved this transferable note. Do not grant
                        // its puzzle aspects or charge research points to the reader.
                        var solved = ResearchNotePuzzle.solve(generated.withCells(cells, false), Set.of("lux", "ignis"));
                        if (!solved.complete() || solved.cells().size() != 6) {
                            throw new IllegalStateException("NITOR fixture path was not solved and pruned");
                        }
                        player.getInventory().setItem(0, ItemResearchNotes.create(generated, false));
                        player.getInventory().setItem(1, ItemResearchNotes.create(solved, true));
                        var ink = new ItemStack(ThaumcraftContent.INKWELL.get());
                        ink.setDamageValue(99);
                        player.getInventory().setItem(2, ink);
                        player.inventoryMenu.broadcastChanges();
                        KnowledgeEvents.sync(player);
                        return new NoteFixture(generated, solved, knowledge.getAspectsDiscovered(), warpSnapshot(knowledge));
                    }, server);
                    advance(client, Stage.NOTE_SYNC, "Server supplies generated note, transferred solved discovery and 99/100 used ink");
                }
                case NOTE_SYNC -> {
                    if (!noteSetup.isDone()) return;
                    noteFixture = noteSetup.join();
                    if (client.gameMode.hasInfiniteItems() || !noteInventoryMatches(client)
                            || KnowledgeAccess.get(client.player).isResearchComplete("NITOR")
                            || !KnowledgeAccess.get(client.player).getAspectsDiscovered().equals(noteFixture.pools)) return;
                    noteEvidence.put("key", "NITOR");
                    noteEvidence.put("generated_cells", noteFixture.generated.cells().size());
                    noteEvidence.put("solved_cells", noteFixture.solved.cells().size());
                    noteEvidence.put("solver_aspects_are_separate_from_reader", true);
                    noteEvidence.put("pools_before_learning", noteFixture.pools);
                    noteEvidence.put("warp_before_learning", noteFixture.warp);
                    noteEvidence.put("reader_has_lux_before", KnowledgeAccess.get(client.player).hasDiscoveredAspect("lux"));
                    client.setScreen(new InventoryScreen(client.player));
                    advance(client, Stage.NOTE_INVENTORY, "Open actual vanilla survival inventory after authoritative component sync");
                }
                case NOTE_INVENTORY -> {
                    if (!ready(client, InventoryScreen.class)) return;
                    requireNotePresentation(client);
                    capture(client, "11-note-tooltip-en.png", Stage.DISCOVERY_CAPTURE);
                    pendingInventorySlot = 0;
                }
                case DISCOVERY_CAPTURE -> {
                    if (!ready(client, InventoryScreen.class)) return;
                    requireNotePresentation(client);
                    capture(client, "12-discovery-tooltip-en.png", Stage.NOTE_USE);
                    pendingInventorySlot = 1;
                }
                case NOTE_USE -> {
                    if (!ready(client, InventoryScreen.class)) return;
                    requireNotePresentation(client);
                    client.setScreen(null);
                    client.player.getInventory().selected = 1;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    if (KnowledgeAccess.get(client.player).isResearchComplete("NITOR")
                            || !client.player.getMainHandItem().is(ThaumcraftContent.DISCOVERY.get())
                            || client.player.getMainHandItem().getCount() != 1) {
                        throw new IllegalStateException("Discovery use optimistically granted research or consumed the client item");
                    }
                    noteEvidence.put("no_optimistic_learning_or_consumption", true);
                    advance(client, Stage.NOTE_LEARNED, "Normal useItem sends discovery use; immediate client still holds one discovery and NITOR is unknown");
                }
                case NOTE_LEARNED -> {
                    if (!KnowledgeAccess.get(client.player).isResearchComplete("NITOR")
                            || !client.player.getInventory().getItem(1).isEmpty()) return;
                    requireNotePools(KnowledgeAccess.get(client.player));
                    noteEvidence.put("authoritative_client_after_learning", noteSnapshot(client));
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        var knowledge = KnowledgeAccess.get(player);
                        if (!knowledge.isResearchComplete("NITOR") || !player.getInventory().getItem(1).isEmpty()) {
                            throw new IllegalStateException("Server did not learn NITOR and consume exactly one discovery");
                        }
                        requireNotePools(knowledge);
                        // A second copy of the same transferred solved note must survive replay.
                        player.getInventory().setItem(1, ItemResearchNotes.create(noteFixture.solved, true));
                        player.inventoryMenu.broadcastChanges();
                    }, server);
                    advance(client, Stage.NOTE_REPLAY_SYNC, "S2C proves NITOR learned and discovery consumed; server supplies another identical solved copy");
                }
                case NOTE_REPLAY_SYNC -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    var held = client.player.getInventory().getItem(1);
                    if (!held.is(ThaumcraftContent.DISCOVERY.get()) || held.getCount() != 1
                            || !noteFixture.solved.equals(ItemResearchNotes.data(held))) return;
                    advance(client, Stage.NOTE_REPLAY_USE, "Receive second solved transfer copy without changing completed research");
                }
                case NOTE_REPLAY_USE -> {
                    if (stageTicks < 10) return;
                    client.player.getInventory().selected = 1;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    requireLearnedCopy(client);
                    advance(client, Stage.NOTE_REPLAY_VERIFY, "Use an already learned discovery; immediate client retains one copy");
                }
                case NOTE_REPLAY_VERIFY -> {
                    if (stageTicks < 20) return;
                    requireLearnedCopy(client);
                    noteEvidence.put("client_after_already_complete_replay", noteSnapshot(client));
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        var knowledge = KnowledgeAccess.get(player);
                        var copy = player.getInventory().getItem(1);
                        if (!knowledge.isResearchComplete("NITOR") || !copy.is(ThaumcraftContent.DISCOVERY.get())
                                || copy.getCount() != 1 || !noteFixture.solved.equals(ItemResearchNotes.data(copy))) {
                            throw new IllegalStateException("Already completed discovery was consumed or changed on the server");
                        }
                        requireNotePools(knowledge);
                        if (!noteFixture.generated.equals(ItemResearchNotes.data(player.getInventory().getItem(0)))
                                || player.getInventory().getItem(2).getDamageValue() != 99) {
                            throw new IllegalStateException("Learning unexpectedly consumed the normal note or ink");
                        }
                    }, server);
                    advance(client, Stage.ISSUANCE_SETUP, "Verify server replay retains discovery, note, ink, aspect pools and warp");
                }
                case ISSUANCE_SETUP -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    noteEvidence.put("server_learning_and_replay_verified", true);
                    observeNoteRequests(client);
                    client.setScreen(null);
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || player.getAbilities().instabuild) {
                            throw new IllegalStateException("Note issuance fixture must retain survival resource costs");
                        }
                        if (player.getInventory().selected != 1) throw new IllegalStateException("Unexpected carried slot before note issuance");
                        // The earlier twelve scenes are complete; make a clean resource fixture
                        // only in this isolated world, with the normal book in the carried slot.
                        player.getInventory().clearContent();
                        player.getInventory().setItem(1, new ItemStack(ThaumcraftContent.THAUMONOMICON.get()));
                        var knowledge = KnowledgeAccess.get(player);
                        knowledge.removeResearch("NITOR");
                        knowledge.addResearch("CRUCIBLE");
                        player.inventoryMenu.broadcastChanges();
                        KnowledgeEvents.sync(player);
                    }, server);
                    advance(client, Stage.ISSUANCE_BOOK_SYNC, "Prepare unfinished NITOR with no note, paper or ink and a normally usable book");
                }
                case ISSUANCE_BOOK_SYNC -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    if (!client.player.getMainHandItem().is(ThaumcraftContent.THAUMONOMICON.get())
                            || KnowledgeAccess.get(client.player).isResearchComplete("NITOR")
                            || nitorNoteCount(client.player) != 0 || paperCount(client.player) != 0
                            || !client.player.getInventory().getItem(2).isEmpty()) return;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(client, Stage.ISSUANCE_BROWSER, "Open the research browser through normal book use before recording the knowledge baseline");
                }
                case ISSUANCE_BROWSER -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    issuanceSetup = CompletableFuture.supplyAsync(() -> {
                        var knowledge = KnowledgeAccess.get(server.getPlayerList().getPlayer(playerId));
                        if (knowledge.isResearchComplete("NITOR")) throw new IllegalStateException("Book use unexpectedly completed NITOR");
                        return new IssuanceBaseline(knowledge.save());
                    }, server);
                    advance(client, Stage.ISSUANCE_BASELINE, "Capture authoritative knowledge after normal book sibling side effects");
                }
                case ISSUANCE_BASELINE -> {
                    if (!issuanceSetup.isDone() || !ready(client, GuiResearchBrowser.class)) return;
                    issuanceBaseline = issuanceSetup.join();
                    if (!KnowledgeAccess.get(client.player).save().equals(issuanceBaseline.state)) return;
                    issuanceEvidence.put("key", "NITOR");
                    issuanceEvidence.put("baseline_after_normal_book_use", true);
                    issuanceEvidence.put("knowledge_before_request", issuanceBaseline.state.toString());
                    clickCategoryTab(client, "ALCHEMY");
                    var browser = (GuiResearchBrowser) client.screen;
                    int graphX = (int) field(browser, "graphLeft") + (int) field(browser, "graphWidth") / 2;
                    int graphY = (int) field(browser, "graphTop") + (int) field(browser, "graphHeight") / 2;
                    var entry = ResearchCatalog.get("NITOR");
                    if (!browser.mouseDragged(graphX, graphY, 0, -entry.column() * 24, -entry.row() * 24)) {
                        throw new IllegalStateException("Native graph pan did not center NITOR");
                    }
                    advance(client, Stage.ISSUANCE_MISSING_CAPTURE, "Select Alchemy and center the primary NITOR node");
                }
                case ISSUANCE_MISSING_CAPTURE -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    requireIssuanceKnowledge(KnowledgeAccess.get(client.player));
                    var preview = ResearchNoteIssuance.preview(client.player, "NITOR");
                    if ((preview.failure() != ResearchNoteIssuance.Failure.MISSING_PAPER
                            && preview.failure() != ResearchNoteIssuance.Failure.MISSING_INK) || preview.reused()) {
                        throw new IllegalStateException("Missing-material primary hover did not reject paper/ink availability");
                    }
                    capture(client, "13-primary-note-missing-materials-en.png", Stage.ISSUANCE_MISSING_CLICK);
                    pendingHoverKey = "NITOR";
                }
                case ISSUANCE_MISSING_CLICK -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    clickUnfinished(client, "NITOR");
                    requireMissingIssuanceMaterials(client.player);
                    if (noteRequestPackets.get() != 0) throw new IllegalStateException("Missing-material GUI sent a note request");
                    advance(client, Stage.ISSUANCE_FUND, "Missing-material click sends no intent and changes no inventory or knowledge");
                }
                case ISSUANCE_FUND -> {
                    if (!ready(client, GuiResearchBrowser.class) || stageTicks < 20) return;
                    requireMissingIssuanceMaterials(client.player);
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        requireMissingIssuanceMaterials(player);
                        player.getInventory().setItem(3, new ItemStack(Items.PAPER, 2));
                        var ink = new ItemStack(ThaumcraftContent.INKWELL.get());
                        ink.setDamageValue(99);
                        player.getInventory().setItem(2, ink);
                        player.inventoryMenu.broadcastChanges();
                    }, server);
                    issuanceEvidence.put("missing_material_click_no_intent_or_changes", true);
                    advance(client, Stage.ISSUANCE_SEND, "Server supplies two paper and an inkwell with one use remaining");
                }
                case ISSUANCE_SEND -> {
                    if (!serverSetup.isDone() || !ready(client, GuiResearchBrowser.class)) return;
                    serverSetup.join();
                    if (paperCount(client.player) != 2 || !inkMatches(client.player, 99) || nitorNoteCount(client.player) != 0) return;
                    requireIssuanceKnowledge(KnowledgeAccess.get(client.player));
                    var preview = ResearchNoteIssuance.preview(client.player, "NITOR");
                    if (!preview.success() || preview.reused()) throw new IllegalStateException("Funded primary note preview is unavailable");
                    issuanceEvidence.put("funded_client_before_request", issuanceSnapshot(client));
                    clickUnfinished(client, "NITOR");
                    if (paperCount(client.player) != 2 || !inkMatches(client.player, 99) || nitorNoteCount(client.player) != 0) {
                        throw new IllegalStateException("Primary click optimistically consumed paper/ink or granted an item");
                    }
                    requireIssuanceKnowledge(KnowledgeAccess.get(client.player));
                    issuanceEvidence.put("no_optimistic_item_costs_or_research", true);
                    advance(client, Stage.ISSUANCE_RECEIVE, "Actual primary browser click sends only the key; client resources remain untouched");
                }
                case ISSUANCE_RECEIVE -> {
                    if (!ready(client, GuiResearchBrowser.class)) return;
                    if (nitorNoteCount(client.player) != 1 || paperCount(client.player) != 1 || !inkMatches(client.player, 100)) return;
                    requireIssuedNote(client.player);
                    if (noteRequestPackets.get() != 1) throw new IllegalStateException("Actual primary issuance did not send exactly one C2S intent");
                    issuanceEvidence.put("authoritative_client_after_request", issuanceSnapshot(client));
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> requireIssuedNote(server.getPlayerList().getPlayer(playerId)), server);
                    advance(client, Stage.ISSUANCE_DUPLICATE, "Authoritative sync delivers one generated note, paper 1 and exhausted ink 100/100");
                }
                case ISSUANCE_DUPLICATE -> {
                    if (!serverSetup.isDone() || !ready(client, GuiResearchBrowser.class)) return;
                    serverSetup.join();
                    var preview = ResearchNoteIssuance.preview(client.player, "NITOR");
                    if (!preview.success() || !preview.reused()) throw new IllegalStateException("Existing note did not bypass exhausted ink preflight");
                    clickUnfinished(client, "NITOR");
                    requireIssuedNote(client.player);
                    advance(client, Stage.ISSUANCE_VERIFY, "Click existing primary note; original GUI suppresses another request despite exhausted ink");
                }
                case ISSUANCE_VERIFY -> {
                    if (stageTicks < 20 || !ready(client, GuiResearchBrowser.class)) return;
                    requireIssuedNote(client.player);
                    if (noteRequestPackets.get() != 1) throw new IllegalStateException("Existing-note GUI sent an extra C2S request");
                    issuanceEvidence.put("duplicate_gui_no_extra_intent_or_expense", true);
                    issuanceEvidence.put("request_packets", noteRequestPackets.get());
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> requireIssuedNote(server.getPlayerList().getPlayer(playerId)), server);
                    advance(client, Stage.ISSUANCE_INVENTORY, "Server and client retain the same one note, one paper and exhausted inkwell");
                }
                case ISSUANCE_INVENTORY -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    requireIssuedNote(client.player);
                    client.setScreen(new InventoryScreen(client.player));
                    advance(client, Stage.ISSUANCE_CAPTURE, "Show the actually issued native research note in the vanilla inventory");
                }
                case ISSUANCE_CAPTURE -> {
                    if (!ready(client, InventoryScreen.class)) return;
                    requireIssuedNote(client.player);
                    int slot = nitorNoteSlot(client.player);
                    var stack = client.player.getInventory().getItem(slot);
                    var tooltip = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
                            .stream().map(Component::getString).toList();
                    var color = (ItemColors) field(client, "itemColors");
                    if (client.getItemRenderer().getModel(stack, client.level, client.player, 0) == client.getModelManager().getMissingModel()
                            || color.getColor(stack, 1) != (0xff000000 | noteFixture.generated.color())
                            || !tooltip.contains(Component.translatable(ResearchCatalog.get("NITOR").nameKey()).getString())) {
                        throw new IllegalStateException("Issued primary note lost its native model, tint or tooltip");
                    }
                    issuanceEvidence.put("issued_native_tooltip", tooltip);
                    capture(client, "14-primary-note-issued-inventory-en.png", Stage.TABLE_SETUP);
                    pendingInventorySlot = slot;
                }
                case TABLE_SETUP -> {
                    issuanceEvidence.put("server_issuance_and_duplicate_verified", true);
                    client.player.closeContainer();
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    tableSetup = CompletableFuture.supplyAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        if (player == null || player.getAbilities().instabuild) throw new IllegalStateException("Table fixture requires survival");
                        var level = player.serverLevel();
                        var main = player.blockPosition().offset(0, 12, 6).immutable();
                        level.setDayTime(1000);
                        // This platform belongs only to the private copied smoke world.
                        for (int x = -1; x <= 4; x++) for (int z = -1; z <= 4; z++) {
                            level.setBlockAndUpdate(main.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(main.offset(x, y, z), Blocks.AIR.defaultBlockState());
                        }
                        player.getInventory().clearContent();
                        player.getInventory().setItem(0, ItemResearchNotes.create(noteFixture.generated, false));
                        player.getInventory().setItem(4, new ItemStack(ThaumcraftContent.TABLE_ITEM.get(), 2));
                        var tool = new ItemStack(ThaumcraftContent.INKWELL.get()); tool.setDamageValue(100);
                        player.getInventory().setItem(5, tool);
                        player.getInventory().selected = 4;
                        player.teleportTo(level, main.getX() + 3, main.getY(), main.getZ() + 3.4, 145, 14);
                        player.inventoryMenu.broadcastChanges();
                        KnowledgeEvents.sync(player);
                        return new TableFixture(main, main.east(), KnowledgeAccess.get(player).save());
                    }, server);
                    advance(client, Stage.TABLE_SYNC, "Prepare private survival placement fixture and two native table items");
                }
                case TABLE_SYNC -> {
                    if (!tableSetup.isDone()) return;
                    tableFixture = tableSetup.join();
                    if (stageTicks < 20 || !client.player.getInventory().getItem(4).is(ThaumcraftContent.TABLE_ITEM.get())
                            || client.player.distanceToSqr(tableFixture.main.getX() + 3, tableFixture.main.getY(), tableFixture.main.getZ() + 3.4) > .1) return;
                    for (var item : List.of(ThaumcraftContent.TABLE_ITEM.get(), ThaumcraftContent.RESEARCH_TABLE_ITEM.get())) {
                        if (client.getItemRenderer().getModel(new ItemStack(item), client.level, client.player, 0) == client.getModelManager().getMissingModel()) {
                            throw new IllegalStateException("Native table item model is missing");
                        }
                    }
                    client.player.getInventory().selected = 4;
                    lookAtTable(client);
                    useBlock(client, tableFixture.main.below());
                    advance(client, Stage.TABLE_PLACE_SECOND, "Place the first native table through normal client useItemOn");
                }
                case TABLE_PLACE_SECOND -> {
                    if (stageTicks < 10 || !client.level.getBlockState(tableFixture.main).is(ThaumcraftContent.TABLE.get())
                            || client.player.getInventory().getItem(4).getCount() != 1) return;
                    useBlock(client, tableFixture.secondary.below());
                    advance(client, Stage.TABLE_BASE_VERIFY, "Place the adjacent second table through native BlockItem handling");
                }
                case TABLE_BASE_VERIFY -> {
                    if (stageTicks < 15 || !client.level.getBlockState(tableFixture.secondary).is(ThaumcraftContent.TABLE.get())
                            || !client.player.getInventory().getItem(4).isEmpty()) return;
                    var server = client.getSingleplayerServer(); var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId); var level = player.serverLevel();
                        for (BlockPos pos : List.of(tableFixture.main, tableFixture.secondary)) {
                            if (!(level.getBlockEntity(pos) instanceof TileTable)
                                    || level.getBlockState(pos).getValue(BlockTable.VARIANT) > 1) throw new IllegalStateException("Native base placement state differs");
                        }
                        if (!player.getInventory().getItem(4).isEmpty()) throw new IllegalStateException("Survival table placement did not consume two items");
                    }, server);
                    advance(client, Stage.TABLE_BASE_CAPTURE, "Verify two physical base tile entities and survival item consumption");
                }
                case TABLE_BASE_CAPTURE -> {
                    if (!serverSetup.isDone() || stageTicks < 15) return; serverSetup.join();
                    if (!(client.level.getBlockEntity(tableFixture.main) instanceof TileTable)
                            || !(client.level.getBlockEntity(tableFixture.secondary) instanceof TileTable)) return;
                    lookAtTable(client);
                    capture(client, "15-two-base-tables-world-en.png", Stage.TABLE_CONVERT);
                }
                case TABLE_CONVERT -> {
                    client.player.getInventory().selected = 5;
                    if (client.player.getMainHandItem().getDamageValue() != 100) throw new IllegalStateException("Conversion fixture lost exhausted ink");
                    useBlock(client, tableFixture.main);
                    advance(client, Stage.TABLE_CONVERT_VERIFY, "Use exhausted native inkwell on the base pair through normal client interaction");
                }
                case TABLE_CONVERT_VERIFY -> {
                    if (stageTicks < 15 || !(client.level.getBlockEntity(tableFixture.main) instanceof TileResearchTable tile)
                            || !(client.level.getBlockEntity(tableFixture.secondary) instanceof TileTable)
                            || !tile.getItem(0).is(ThaumcraftContent.INKWELL.get()) || !client.player.getInventory().getItem(5).isEmpty()) return;
                    if (client.level.getBlockState(tableFixture.main).getValue(BlockTable.VARIANT) != 5
                            || client.level.getBlockState(tableFixture.secondary).getValue(BlockTable.VARIANT) != 8
                            || tile.getItem(0).getDamageValue() != 100 || tile.getItem(0).getCount() != 1) {
                        throw new IllegalStateException("Converted main/secondary state or exhausted tool differs");
                    }
                    tableEvidence.put("native_placement_and_conversion", true);
                    tableEvidence.put("main", tableFixture.main.toShortString());
                    tableEvidence.put("secondary", tableFixture.secondary.toShortString());
                    tableEvidence.put("variants", List.of(5, 8));
                    advance(client, Stage.TABLE_OPEN_MAIN, "Client receives replacement main BE, invisible secondary and unchanged exhausted tool");
                }
                case TABLE_OPEN_MAIN -> {
                    useBlock(client, tableFixture.main);
                    advance(client, Stage.TABLE_MAIN_MENU, "Open research table from the main half using native block interaction");
                }
                case TABLE_MAIN_MENU -> {
                    if (!ready(client, GuiResearchTable.class)) return;
                    var menu = tableMenu(client);
                    if (!menu.getSlot(0).getItem().is(ThaumcraftContent.INKWELL.get()) || menu.getSlot(0).getItem().getDamageValue() != 100) return;
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, ClickType.QUICK_MOVE, client.player);
                    advance(client, Stage.TABLE_TOOL_OUT, "Shift-click exhausted tool out through the actual container packet");
                }
                case TABLE_TOOL_OUT -> {
                    if (stageTicks < 15) return;
                    var menu = tableMenu(client);
                    if (!menu.getSlot(0).getItem().isEmpty()) return;
                    int slot = tablePlayerSlot(menu, client, ThaumcraftContent.INKWELL.get());
                    client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, client.player);
                    advance(client, Stage.TABLE_TOOL_BACK, "Shift-click the same exhausted tool back into the native tool slot");
                }
                case TABLE_TOOL_BACK -> {
                    if (stageTicks < 15) return;
                    var menu = tableMenu(client);
                    if (!menu.getSlot(0).getItem().is(ThaumcraftContent.INKWELL.get())) return;
                    int slot = tablePlayerSlot(menu, client, ThaumcraftContent.RESEARCH_NOTE.get());
                    client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, client.player);
                    advance(client, Stage.TABLE_NOTE_IN, "Shift-click generated NITOR notes into the native note slot");
                }
                case TABLE_NOTE_IN -> {
                    if (stageTicks < 15) return;
                    var menu = tableMenu(client);
                    if (!noteFixture.generated.equals(ItemResearchNotes.data(menu.getSlot(1).getItem()))) return;
                    var server = client.getSingleplayerServer(); var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> verifyTableOnServer(server.getPlayerList().getPlayer(playerId)), server);
                    advance(client, Stage.TABLE_NOTE_SYNC, "Verify authoritative menu transfers preserve both item components and knowledge");
                }
                case TABLE_NOTE_SYNC -> {
                    if (!serverSetup.isDone() || stageTicks < 15) return; serverSetup.join();
                    if (!noteFixture.generated.equals(ItemResearchNotes.data(tableMenu(client).getSlot(1).getItem()))) throw new IllegalStateException("Menu lost note data");
                    tableEvidence.put("main_half_opened", true);
                    tableEvidence.put("actual_container_transfers_verified", true);
                    client.player.closeContainer();
                    advance(client, Stage.TABLE_CLOSE_WORLD, "Close normal menu after server-confirmed tool round trip and note insertion");
                }
                case TABLE_CLOSE_WORLD -> {
                    if (stageTicks < 20 || client.screen != null || !(client.level.getBlockEntity(tableFixture.main) instanceof TileResearchTable tile)
                            || !noteFixture.generated.equals(ItemResearchNotes.data(tile.getItem(1)))) return;
                    lookAtTable(client);
                    advance(client, Stage.TABLE_WORLD_CAPTURE, "Wait for block-entity update: exhausted quill and tinted note are world-rendered");
                }
                case TABLE_WORLD_CAPTURE -> {
                    if (stageTicks < 20) return;
                    lookAtTable(client);
                    capture(client, "16-research-table-filled-world-en.png", Stage.TABLE_OPEN_SECONDARY);
                }
                case TABLE_OPEN_SECONDARY -> {
                    useBlock(client, tableFixture.secondary);
                    advance(client, Stage.TABLE_SECONDARY_MENU, "Open the same physical research inventory from its secondary half");
                }
                case TABLE_SECONDARY_MENU -> {
                    if (!ready(client, GuiResearchTable.class)) return;
                    var menu = tableMenu(client);
                    if (!noteFixture.generated.equals(ItemResearchNotes.data(menu.getSlot(1).getItem()))
                            || menu.getSlot(0).getItem().getDamageValue() != 100) return;
                    tableEvidence.put("secondary_half_opened_same_main", true);
                    advance(client, Stage.TABLE_MENU_CAPTURE, "Secondary interaction opens all38 original slots with the authoritative generated note");
                }
                case TABLE_MENU_CAPTURE -> {
                    if (stageTicks < 15) return;
                    capture(client, "17-research-table-menu-note-en.png", Stage.TABLE_SAVE_VERIFY);
                }
                case TABLE_SAVE_VERIFY -> {
                    if (stageTicks == 1) {
                        client.player.closeContainer();
                        var server = client.getSingleplayerServer(); var playerId = client.player.getUUID();
                        serverSetup = CompletableFuture.runAsync(() -> {
                            var player = server.getPlayerList().getPlayer(playerId);
                            verifyTableOnServer(player);
                            var tile = (TileResearchTable) player.serverLevel().getBlockEntity(tableFixture.main);
                            var saved = tile.saveWithFullMetadata(player.registryAccess());
                            var reloaded = new TileResearchTable(tableFixture.main, tile.getBlockState());
                            reloaded.loadWithComponents(saved, player.registryAccess());
                            if (!ItemStack.matches(tile.getItem(0), reloaded.getItem(0)) || !ItemStack.matches(tile.getItem(1), reloaded.getItem(1))) {
                                throw new IllegalStateException("Native table save/load changed item components");
                            }
                            server.saveEverything(false, true, false);
                        }, server);
                    }
                    if (!serverSetup.isDone() || stageTicks < 15) return; serverSetup.join();
                    tableEvidence.put("native_save_load_components_verified", true);
                    tableEvidence.put("private_world_saved", true);
                    tableEvidence.put("knowledge_pools_warp_unchanged", true);
                    advance(client, Stage.TABLE_PREVIEW_SETUP, "Save only the copied smoke world and verify a native table NBT/component round trip");
                }
                case TABLE_PREVIEW_SETUP -> {
                    var server = client.getSingleplayerServer(); var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        // Additional preview items exist only after the placement,
                        // conversion, transfer and persistence assertions have passed.
                        player.getInventory().setItem(4, new ItemStack(ThaumcraftContent.TABLE_ITEM.get()));
                        player.getInventory().setItem(5, new ItemStack(ThaumcraftContent.RESEARCH_TABLE_ITEM.get()));
                        player.getInventory().selected = 5;
                        player.inventoryMenu.broadcastChanges();
                    }, server);
                    advance(client, Stage.TABLE_PREVIEW_SYNC, "Supply two separate preview items only in the owned smoke world");
                }
                case TABLE_PREVIEW_SYNC -> {
                    if (!serverSetup.isDone() || stageTicks < 15) return; serverSetup.join();
                    if (!client.player.getInventory().getItem(4).is(ThaumcraftContent.TABLE_ITEM.get())
                            || !client.player.getInventory().getItem(5).is(ThaumcraftContent.RESEARCH_TABLE_ITEM.get())) return;
                    client.player.getInventory().selected = 5;
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.setScreen(new InventoryScreen(client.player));
                    advance(client, Stage.TABLE_PREVIEW_INVENTORY, "Open native inventory with both baked item previews and held research table");
                }
                case TABLE_PREVIEW_INVENTORY -> {
                    if (!ready(client, InventoryScreen.class)) return;
                    for (int slot : List.of(4, 5)) {
                        var stack = client.player.getInventory().getItem(slot);
                        if (stack.getCount() != 1 || client.getItemRenderer().getModel(stack, client.level, client.player, 0) == client.getModelManager().getMissingModel()) {
                            throw new IllegalStateException("Table preview item model or count is invalid");
                        }
                    }
                    tableEvidence.put("native_inventory_item_previews", List.of("thaumcraft:table", "thaumcraft:research_table"));
                    tableEvidence.put("research_table_dropped_preview_tested", false);
                    capture(client, "18-table-item-previews-inventory-en.png", Stage.TABLE_PREVIEW_DROP);
                    pendingInventorySlot = 5;
                }
                case TABLE_PREVIEW_DROP -> {
                    if (stageTicks == 1) {
                        client.player.closeContainer();
                        lookAtTable(client);
                        client.player.getInventory().selected = 4;
                        return;
                    }
                    // Let vanilla gameMode.tick synchronize the selected hotbar
                    // slot first: LocalPlayer.drop itself sends only DROP_ITEM.
                    if (stageTicks < 3) return;
                    if (!client.player.getMainHandItem().is(ThaumcraftContent.TABLE_ITEM.get())) throw new IllegalStateException("Native drop selected the wrong preview item");
                    if (!client.player.drop(false)) throw new IllegalStateException("Native client refused the preview table drop");
                    client.player.getInventory().selected = 5;
                    advance(client, Stage.TABLE_PREVIEW_DROP_SYNC, "Native LocalPlayer.drop sends the normal item-drop packet; await the server entity");
                }
                case TABLE_PREVIEW_DROP_SYNC -> {
                    if (stageTicks < 12) return;
                    if (previewDrop == null) {
                        var server = client.getSingleplayerServer(); var playerId = client.player.getUUID();
                        previewDrop = CompletableFuture.supplyAsync(() -> {
                            var player = server.getPlayerList().getPlayer(playerId);
                            var drops = player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8),
                                    entity -> !entity.isRemoved() && entity.getItem().is(ThaumcraftContent.TABLE_ITEM.get()));
                            if (drops.size() != 1 || drops.getFirst().getItem().getCount() != 1
                                    || !player.getInventory().getItem(4).isEmpty()
                                    || !player.getInventory().getItem(5).is(ThaumcraftContent.RESEARCH_TABLE_ITEM.get())
                                    || player.getInventory().getItem(5).getCount() != 1
                                    || !KnowledgeAccess.get(player).save().equals(tableFixture.knowledge)) {
                                throw new IllegalStateException("Actual native table drop did not preserve authoritative items/knowledge");
                            }
                            var dropped = drops.getFirst();
                            // Move only the fixture player back before vanilla's
                            // pickup delay expires; keep native item physics intact.
                            player.teleportTo(player.serverLevel(), tableFixture.main.getX() + 3, tableFixture.main.getY(),
                                    tableFixture.main.getZ() + 4.2, 145, 14);
                            return new PreviewDrop(dropped.getId(), dropped.getUUID());
                        }, server);
                    }
                    if (!previewDrop.isDone()) return;
                    previewDropped = previewDrop.join();
                    if (!(client.level.getEntity(previewDropped.entityId) instanceof ItemEntity dropped)
                            || !dropped.getUUID().equals(previewDropped.uuid)
                            || !client.player.getInventory().getItem(4).isEmpty()
                            || client.player.distanceToSqr(tableFixture.main.getX() + 3, tableFixture.main.getY(), tableFixture.main.getZ() + 4.2) > .1) return;
                    tableEvidence.put("native_c2s_base_item_drop_verified", true);
                    tableEvidence.put("dropped_base_entity_uuid", previewDropped.uuid.toString());
                    advance(client, Stage.TABLE_PREVIEW_WORLD, "Authoritative vanilla dropped base table is synchronized; research table remains held");
                }
                case TABLE_PREVIEW_WORLD -> {
                    if (stageTicks < 30) return;
                    if (client.screen != null || !(client.level.getEntity(previewDropped.entityId) instanceof ItemEntity dropped)
                            || !dropped.getUUID().equals(previewDropped.uuid) || dropped.getItem().getCount() != 1
                            || !dropped.getItem().is(ThaumcraftContent.TABLE_ITEM.get())
                            || !client.player.getMainHandItem().is(ThaumcraftContent.RESEARCH_TABLE_ITEM.get())) {
                        throw new IllegalStateException("Native dropped/held table previews are no longer visible");
                    }
                    lookAtTable(client);
                    tableEvidence.put("first_person_research_table_held", true);
                    capture(client, "19-table-research-hand-base-dropped-world-en.png", Stage.EDIT_SETUP);
                }
                case EDIT_SETUP -> {
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        var tile = (TileResearchTable) player.serverLevel().getBlockEntity(tableFixture.main);
                        var knowledge = KnowledgeAccess.get(player);
                        knowledge.removeResearch("NITOR");
                        knowledge.removeResearch("RESEARCHER1");
                        knowledge.removeResearch("RESEARCHER2");
                        knowledge.setAspectPool("lux", 2);
                        knowledge.setAspectPool("ignis", 3);
                        knowledge.setAspectPool("potentia", 0);
                        // Small hand-authored geometry exercises actual GUI actions,
                        // not the separately verified original note generator.
                        var data = new ResearchNoteData("NITOR", 0xFFF563, false, 0, List.of(
                                new Cell(0, 0, 1, "aer"), new Cell(1, 0, 0, null),
                                new Cell(2, 0, 0, null), new Cell(3, 0, 1, "potentia")));
                        var note = ItemResearchNotes.create(data, false);
                        note.set(DataComponents.CUSTOM_NAME, Component.literal("Research table result"));
                        tile.setItem(0, new ItemStack(ThaumcraftContent.INKWELL.get()));
                        tile.setItem(1, note);
                        player.getInventory().clearContent();
                        player.inventoryMenu.broadcastChanges();
                        KnowledgeEvents.sync(player);
                    }, server);
                    advance(client, Stage.EDIT_OPEN, "Prepare a solvable native table puzzle and four literal ink uses");
                }
                case EDIT_OPEN -> {
                    if (!serverSetup.isDone() || stageTicks < 15) return;
                    serverSetup.join();
                    useBlock(client, tableFixture.main);
                    advance(client, Stage.EDIT_READY, "Open the physical table for palette drag and erase");
                }
                case EDIT_READY -> {
                    if (!ready(client, GuiResearchTable.class) || !tableMenu(client).editReady()) return;
                    if (tableMenu(client).getSlot(0).getItem().getDamageValue() != 0) return;
                    advance(client, Stage.EDIT_ACTION, "Authoritative menu session and palette are ready");
                }
                case EDIT_ACTION -> {
                    var menu = tableMenu(client);
                    if (!menu.editReady() || stageTicks < 5) return;
                    var before = ItemResearchNotes.data(menu.getSlot(1).getItem());
                    var pools = KnowledgeAccess.get(client.player).getAspectsDiscovered();
                    if (editStep == 1) {
                        var point = editHexPoint(client, 1, 0);
                        client.screen.mouseClicked(point[0], point[1], 0);
                        client.screen.mouseReleased(point[0], point[1], 0);
                    } else dragTableAspect(client, editStep == 3 ? "ignis" : "lux", editStep == 3 ? 2 : 1, 0);
                    if (!before.equals(ItemResearchNotes.data(menu.getSlot(1).getItem()))
                            || menu.getSlot(0).getItem().getDamageValue() != editStep
                            || !pools.equals(KnowledgeAccess.get(client.player).getAspectsDiscovered())) {
                        throw new IllegalStateException("Table GUI changed note, ink or pools before authoritative sync");
                    }
                    advance(client, Stage.EDIT_WAIT, "Native GUI sends " + (editStep == 1 ? "erase" : "placement") + " intent");
                }
                case EDIT_WAIT -> {
                    var menu = tableMenu(client);
                    if (!menu.editReady() || stageTicks < 5 || menu.getSlot(0).getItem().getDamageValue() != editStep + 1) return;
                    var data = ItemResearchNotes.data(menu.getSlot(1).getItem());
                    int expectedLux = editStep < 2 ? 1 : 0;
                    if (KnowledgeAccess.get(client.player).getAspectPoolFor("lux") != expectedLux
                            || KnowledgeAccess.get(client.player).getAspectPoolFor("ignis") != (editStep == 3 ? 2 : 3)) return;
                    if (data.cellAt(1, 0).type() != (editStep == 1 ? 0 : 2)) throw new IllegalStateException("Native table edit changed wrong cell");
                    if (++editStep < 4) advance(client, Stage.EDIT_ACTION, "Authoritative cell, pool and ink update received");
                    else {
                        if (!data.complete() || !menu.getSlot(1).getItem().is(ThaumcraftContent.DISCOVERY.get())
                                || !menu.getSlot(1).getItem().getHoverName().getString().equals("Research table result")
                                || KnowledgeAccess.get(client.player).isResearchComplete("NITOR")) {
                            throw new IllegalStateException("Solving must preserve components and create discovery without learning research");
                        }
                        tableEvidence.put("gui_place_erase_solve", true);
                        tableEvidence.put("editing_ink_damage", 4);
                        tableEvidence.put("editing_lux_pool", 0);
                        tableEvidence.put("editing_ignis_pool", 2);
                        advance(client, Stage.EDIT_SOLVED_CAPTURE, "Player solved the table puzzle through native GUI/C2S");
                    }
                }
                case EDIT_SOLVED_CAPTURE -> {
                    if (!ready(client, GuiResearchTable.class)) return;
                    capture(client, "20-research-table-player-solved-en.png", Stage.EDIT_TAKE);
                }
                case EDIT_TAKE -> {
                    var menu = tableMenu(client);
                    client.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.QUICK_MOVE, client.player);
                    advance(client, Stage.EDIT_LEARN, "Take the actually solved discovery out of the table");
                }
                case EDIT_LEARN -> {
                    if (!tableMenu(client).getSlot(1).getItem().isEmpty() || stageTicks < 10) return;
                    int slot = -1;
                    for (int index = 0; index < 9; index++) if (client.player.getInventory().getItem(index).is(ThaumcraftContent.DISCOVERY.get())) slot = index;
                    if (slot < 0) throw new IllegalStateException("Solved discovery did not reach native hotbar");
                    client.player.closeContainer();
                    client.player.getInventory().selected = slot;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(client, Stage.EDIT_LEARNED, "Use the discovery solved in the table, not a server-pre-solved copy");
                }
                case EDIT_LEARNED -> {
                    if (!KnowledgeAccess.get(client.player).isResearchComplete("NITOR") || !client.player.getMainHandItem().isEmpty()) return;
                    var server = client.getSingleplayerServer();
                    var playerId = client.player.getUUID();
                    serverSetup = CompletableFuture.runAsync(() -> {
                        var player = server.getPlayerList().getPlayer(playerId);
                        var tile = (TileResearchTable) player.serverLevel().getBlockEntity(tableFixture.main);
                        var knowledge = KnowledgeAccess.get(player);
                        if (!knowledge.isResearchComplete("NITOR") || knowledge.getAspectPoolFor("lux") != 0
                                || knowledge.getAspectPoolFor("ignis") != 2 || tile.getItem(0).getDamageValue() != 4
                                || !tile.getItem(1).isEmpty()) throw new IllegalStateException("Server did not retain the player solve→learn result");
                        server.saveEverything(false, true, true);
                    }, server);
                    tableEvidence.put("player_solved_discovery_learned", true);
                    advance(client, Stage.EARLY_CONTENT, "Player solve→take→learn completed with exact costs");
                }
                case EARLY_CONTENT -> {
                    if (!serverSetup.isDone()) return;
                    serverSetup.join();
                    if (earlyContent == null) earlyContent = new EarlyContentClientSequence(tableFixture.main);
                    if (earlyContent.tick(client, name -> capture(client, name, Stage.EARLY_CONTENT))) {
                        advance(client, Stage.ALCHEMY, "Native combine→node drain→arcane craft sequence completed");
                    }
                }
                case ALCHEMY -> {
                    if (alchemy == null) alchemy = new AlchemyClientSequence(tableFixture.main);
                    if (alchemy.tick(client, name -> capture(client, name, Stage.ALCHEMY))) {
                        advance(client, Stage.DISTILLATION, "Native crucible conversion→bucket→physical ingredients→Nitor craft→pickup→placement completed");
                    }
                }
                case DISTILLATION -> {
                    if (distillation == null) distillation = new DistillationClientSequence(tableFixture.main);
                    if (distillation.tick(client, name -> capture(client, name, Stage.DISTILLATION))) {
                        advance(client, Stage.FOCUS, "Native furnace→alembics→four phials→two jars and actual gold/greatwood wand assembly/charging completed; seeded trees/masonry are visual fixtures");
                    }
                }
                case FOCUS -> {
                    if (focus == null) focus = new FocusClientSequence(tableFixture.main);
                    if (focus.tick(client, name -> {
                        capture(client, name, Stage.FOCUS);
                        if (name.startsWith("45-")) pendingInventorySlot = focus.returnedFocusSlot;
                    })) {
                        advance(client, Stage.TUBES, "Native Fire Focus craft→held-F radial equip→RMB ember combat→Shift-F removal and survival flower harvest→2x2 resource crafts completed; planted displays remain fixtures");
                    }
                }
                case TUBES -> {
                    if (tubes == null) tubes = new TubeClientSequence(tableFixture.main.offset(112, 0, 8), tableFixture.main.offset(124, 0, 8));
                    if (tubes.tick(client, name -> capture(client, name, Stage.TUBES))) {
                        advance(client, Stage.INFUSION, "Native quicksilver→drops→eight paid arcane tubes→placement→wand close/reopen→finite coal distillation→labelled jar pull→survival jar break/pickup/replacement completed");
                    }
                }
                case INFUSION -> {
                    if (infusion == null) infusion = new InfusionClientSequence(tableFixture.main.offset(160, 0, 8), tableFixture.main.offset(176, 0, 8));
                    if (infusion.tick(client, name -> capture(client, name, Stage.INFUSION))) {
                        advance(client, Stage.EQUIPMENT, "Native altar crafting/formation→finite shard distillation→paid STACK infusion→Silverwood wand assembly and node charging completed");
                    }
                }
                case EQUIPMENT -> {
                    if (equipment == null) {
                        BlockPos workbench = equipmentOnly ? equipmentOrigin : tableFixture.main.offset(224, 0, 8);
                        equipment = new ThaumiumEquipmentClientSequence(workbench, workbench.east(16));
                    }
                    if (equipment.tick(client, name -> capture(client, name, Stage.EQUIPMENT))) {
                        advance(client, Stage.FINISH, "Native Thaumium metallurgy, ordinary equipment crafting, mining, repair and armor continuation completed");
                    }
                }
                case CAPS -> {
                    if (caps == null) caps = new ThaumiumCapsClientSequence(capsOnly ? equipmentOrigin : tableFixture.main.offset(256, 0, 8),
                            capsOnly ? capsCenter : tableFixture.main.offset(176, 0, 8));
                    if (caps.tick(client, name -> capture(client, name, Stage.CAPS))) {
                        advance(client, Stage.FINISH, "Native node harvest/essentia→Thaumium cap infusion→wand assembly/efficient Fire continuation completed");
                    }
                }
                case GOGGLES -> {
                    if (goggles == null) goggles = new GogglesClientSequence(equipmentOrigin, capsCenter);
                    if (goggles.tick(client, name -> capture(client, name, Stage.GOGGLES))) {
                        advance(client, Stage.FINISH, "Native goggles crafting/equipping→hands-free node/container optics→finite discounted crafting and Fire completed");
                    }
                }
                case ROBE_SHOCK -> {
                    if (robeShock == null) robeShock = new RobeShockClientSequence(equipmentOrigin, capsCenter);
                    if (robeShock.tick(client, name -> capture(client, name, Stage.ROBE_SHOCK))) {
                        advance(client, Stage.FINISH, "Native cloth/robes→dye/wash/wear/repair→Shock crafting/selection and actual paid ranged pulses completed");
                    }
                }
                case EXCAVATION -> {
                    if (excavation == null) excavation = new ExcavationClientSequence(equipmentOrigin, capsCenter);
                    if (excavation.tick(client, name -> capture(client, name, Stage.EXCAVATION))) {
                        advance(client, Stage.FINISH, "Native Excavation craft/selection, genuine protected player harvesting, loot/XP and paid refusal completed");
                    }
                }
                case FROST -> {
                    if (frost == null) frost = new FrostClientSequence(equipmentOrigin, capsCenter);
                    if (frost.tick(client, name -> capture(client, name, Stage.FROST))) {
                        advance(client, Stage.FINISH, "Native Frost craft/selection, paid tracked ricochet projectile and ordinary hit completed");
                    }
                }
                case VOID_JAR -> {
                    if (voidJar == null) voidJar = new VoidJarClientSequence(client, name -> capture(client, name, Stage.VOID_JAR), equipmentOrigin, capsCenter);
                    if (voidJar.tick(client)) {
                        advance(client, Stage.FINISH, "Native void jar craft, real distillation/held-jar drain, full overflow and preserved component drop/place completed");
                    }
                }
                case VALVE -> {
                    if (valve == null) valve = new ValveClientSequence(client, name -> capture(client, name, Stage.VALVE), equipmentOrigin, capsCenter);
                    if (valve.tick(client)) {
                        advance(client, Stage.FINISH, "Native shapeless valve and resonator craft, real flow, hand/wand/redstone edges and inspection completed");
                    }
                }
                case TUBE_FILTER -> {
                    if (tubeFilter == null) tubeFilter = new TubeFilterClientSequence(client, name -> capture(client, name, Stage.TUBE_FILTER), equipmentOrigin, capsCenter);
                    if (tubeFilter.tick(client)) {
                        advance(client, Stage.FINISH, "Native finite node recharge, real tube/filter crafts, paid label and filtered/restricted/directional transport completed");
                    }
                }
                case BUFFER -> {
                    if (buffer == null) buffer = new TubeBufferClientSequence(client, name -> capture(client, name, Stage.BUFFER), equipmentOrigin, capsCenter);
                    if (buffer.tick(client)) {
                        advance(client, Stage.FINISH, "Native buffer craft, finite mixed essentia, indexed raw ports/chokes and conserved delivery completed");
                    }
                }
                case CENTRIFUGE -> {
                    if (centrifuge == null) centrifuge = new CentrifugeClientSequence(client, name -> capture(client, name, Stage.CENTRIFUGE), equipmentOrigin, capsCenter);
                    if (centrifuge.tick(client)) {
                        advance(client, Stage.FINISH, "Native construct/centrifuge crafting, finite compound processing, redstone pause and conserved component output completed");
                    }
                }
                case CRYSTALLIZER -> {
                    if (crystallizer == null) crystallizer = new CrystallizerClientSequence(client, name -> capture(client, name, Stage.CRYSTALLIZER), equipmentOrigin, capsCenter);
                    if (crystallizer.tick(client)) {
                        advance(client, Stage.FINISH, "Native unassisted crystallization, typed crystals, inventory and physical paid outputs completed; VisNet acceleration remains deferred");
                    }
                }
                case STABILIZER -> {
                    if (stabilizer == null) stabilizer = new NodeStabilizerClientSequence(client, name -> capture(client, name, Stage.STABILIZER), equipmentOrigin, capsCenter);
                    if (stabilizer.tick(client)) {
                        advance(client, Stage.FINISH, "Native basic stabilizer, ordinary-node cached lock/recharge, redstone and physical recovery completed; real VisNet and special-node behaviours remain deferred");
                    }
                }
                case VISNET -> {
                    if (visnet == null) {
                        visnetTail = new VisNetTailObserver(client.getSingleplayerServer(), runDirectory, runId);
                        visnet = new VisNetClientSequence(client, name -> capture(client, name, Stage.VISNET), equipmentOrigin, capsCenter);
                    }
                    visnetTail.check();
                    if (visnet.tick(client)) {
                        advance(client, Stage.FINISH, "Native transduction, real relay-routed Terra CV acceleration and safe depleted reversion completed; special nodes and the remaining network consumers remain deferred");
                    }
                }
                case FOCAL -> {
                    if (focal == null) focal = new FocalManipulatorClientSequence(client,
                            name -> capture(client, name, Stage.FOCAL), runDirectory, runId);
                    focal.check();
                    if (focal.tick(client)) {
                        focal.close(); // Keep its independent SERVER_POST normal-stop tail.
                        advance(client, Stage.FINISH, "Native furnace XP, real recipes and bound paid Potency upgrade completed; exact PNG and stopped-save inspection still required");
                    }
                }
                case BORE -> {
                    if (bore == null) bore = new BoreClientSequence(client,
                            name -> capture(client, name, Stage.BORE), runDirectory, runId);
                    bore.check();
                    if (bore.tick(client)) {
                        bore.close(); // Input released; independent observations remain through native stop.
                        advance(client, Stage.FINISH, "Genuine research, paid recipes/distillation/infusion and two native Bore harvests completed; exact PNG and stopped-save audit remain required");
                    }
                }
                case FINISH -> {
                    if (visnetTail != null) visnetTail.check();
                    if (focal != null) focal.check();
                    if (bore != null) bore.check();
                    if (serverSetup != null && !serverSetup.isDone()) return;
                    if (serverSetup != null) serverSetup.join();
                    finished = true;
                    writeResult("SCRIPT_PASSED", null);
                    LOGGER.info("CLIENT_SMOKE_SCRIPT_PASSED: {} screenshots; visual inspection remains required", screenshots.size());
                    client.stop();
                }
            }
        } catch (ScreenNotReadyException notReady) {
            // A vanilla screen that has not built its button yet is not a defect of
            // the port: the stage waits for the real widget and only reports when
            // the screen never offers it.
            if (stageTicks > SCREEN_RETRY_TICKS) {
                fail(client, notReady);
                return;
            }
            LOGGER.info("Client smoke: {} waits for its screen button ({})", stage, notReady.getMessage());
        } catch (Throwable error) {
            fail(client, error);
        }
    }

    private boolean ready(Minecraft client, Class<? extends Screen> expected) {
        return stageTicks >= 10 && client.getOverlay() == null && expected.isInstance(client.screen);
    }

    private int[] editHexPoint(Minecraft client, int q, int r) {
        var point = new HexUtils.Hex(q, r).toPixel(9);
        return new int[]{(client.getWindow().getGuiScaledWidth() - 255) / 2 + 169 + (int) Math.round(point.x),
                (client.getWindow().getGuiScaledHeight() - 255) / 2 + 83 + (int) Math.round(point.y)};
    }

    private void dragTableAspect(Minecraft client, String tag, int q, int r) {
        int left = (client.getWindow().getGuiScaledWidth() - 255) / 2;
        int top = (client.getWindow().getGuiScaledHeight() - 255) / 2;
        var tags = KnowledgeAccess.get(client.player).getAspectsDiscovered().keySet().stream().sorted().toList();
        int index = tags.indexOf(tag);
        if (index < 0) throw new IllegalStateException("Missing palette aspect " + tag);
        for (int reset = 0; reset < 10; reset++) client.screen.mouseClicked(left + 39, top + 125, 0);
        while (index >= 25) { client.screen.mouseClicked(left + 63, top + 125, 0); index -= 5; }
        int x = left + 18 + index / 5 * 16, y = top + 48 + index % 5 * 16;
        var target = editHexPoint(client, q, r);
        client.screen.mouseClicked(x, y, 0);
        client.screen.mouseDragged(target[0], target[1], 0, target[0] - x, target[1] - y);
        client.screen.mouseReleased(target[0], target[1], 0);
    }

    private static BlockPos restartPosition(JsonArray coordinates) {
        if (coordinates == null || coordinates.size() != 3) throw new IllegalStateException("Restart position requires3 coordinates");
        return new BlockPos(coordinates.get(0).getAsInt(), coordinates.get(1).getAsInt(), coordinates.get(2).getAsInt());
    }

    private void lookAtTable(Minecraft client) {
        var target = Vec3.atCenterOf(tableFixture.main).add(.5, .25, 0);
        var eye = client.player.getEyePosition();
        var delta = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        client.player.setYRot(yaw); client.player.yRotO = yaw;
        client.player.setXRot(pitch); client.player.xRotO = pitch;
    }

    private void useBlock(Minecraft client, BlockPos pos) {
        if (client.screen != null) throw new IllegalStateException("Native table interaction requires the world view");
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
    }

    private ContainerResearchTable tableMenu(Minecraft client) {
        if (!(client.screen instanceof GuiResearchTable screen) || !(client.player.containerMenu instanceof ContainerResearchTable menu)
                || screen.getMenu() != menu || !menu.mainPos().equals(tableFixture.main) || menu.slots.size() != 38) {
            throw new IllegalStateException("Research menu does not address the same physical38-slot table");
        }
        return menu;
    }

    private int tablePlayerSlot(ContainerResearchTable menu, Minecraft client, Item item) {
        for (int index = 2; index < menu.slots.size(); index++) {
            var slot = menu.getSlot(index);
            if (slot.container == client.player.getInventory() && slot.getItem().is(item)) return index;
        }
        throw new IllegalStateException("No player container slot for " + BuiltInRegistries.ITEM.getKey(item));
    }

    private void verifyTableOnServer(ServerPlayer player) {
        if (player == null || !(player.serverLevel().getBlockEntity(tableFixture.main) instanceof TileResearchTable tile)
                || !(player.serverLevel().getBlockEntity(tableFixture.secondary) instanceof TileTable)
                || player.serverLevel().getBlockState(tableFixture.main).getValue(BlockTable.VARIANT) != 5
                || player.serverLevel().getBlockState(tableFixture.secondary).getValue(BlockTable.VARIANT) != 8) {
            throw new IllegalStateException("Authoritative converted table pair is missing");
        }
        if (!tile.getItem(0).is(ThaumcraftContent.INKWELL.get()) || tile.getItem(0).getCount() != 1 || tile.getItem(0).getDamageValue() != 100
                || !tile.getItem(1).is(ThaumcraftContent.RESEARCH_NOTE.get()) || tile.getItem(1).getCount() != 1
                || !noteFixture.generated.equals(ItemResearchNotes.data(tile.getItem(1)))) {
            throw new IllegalStateException("Table transfers changed or lost note/tool components");
        }
        for (var stack : player.getInventory().items) {
            if (stack.is(ThaumcraftContent.TABLE_ITEM.get()) || stack.is(ThaumcraftContent.INKWELL.get())
                    || stack.is(ThaumcraftContent.RESEARCH_NOTE.get())) throw new IllegalStateException("Table placement/transfer duplicated an item");
        }
        if (!KnowledgeAccess.get(player).save().equals(tableFixture.knowledge)) throw new IllegalStateException("Read-only table changed knowledge, pools or warp");
    }

    private Map<String, Object> tableSnapshot(Minecraft client) {
        if (tableFixture == null) return Map.of();
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("main", tableFixture.main.toShortString());
        snapshot.put("secondary", tableFixture.secondary.toShortString());
        snapshot.put("main_variant", client.level.getBlockState(tableFixture.main).getValue(BlockTable.VARIANT));
        snapshot.put("secondary_variant", client.level.getBlockState(tableFixture.secondary).getValue(BlockTable.VARIANT));
        if (client.level.getBlockEntity(tableFixture.main) instanceof TileResearchTable tile) {
            snapshot.put("tool_count", tile.getItem(0).getCount()); snapshot.put("ink_damage", tile.getItem(0).getDamageValue());
            var data = ItemResearchNotes.data(tile.getItem(1));
            snapshot.put("note_key", data == null ? "" : data.key()); snapshot.put("note_cells", data == null ? 0 : data.cells().size());
            snapshot.put("note_color", data == null ? 0 : data.color()); snapshot.put("note_complete", data != null && data.complete());
        }
        if (client.player.containerMenu instanceof ContainerResearchTable menu) {
            snapshot.put("menu_main", menu.mainPos().toShortString()); snapshot.put("menu_slots", menu.slots.size());
            snapshot.put("menu_tool_count", menu.getSlot(0).getItem().getCount());
            snapshot.put("menu_note_data_matches", noteFixture.generated.equals(ItemResearchNotes.data(menu.getSlot(1).getItem())));
        }
        snapshot.put("held_item", BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem()).toString());
        snapshot.put("preview_base_inventory_count", client.player.getInventory().getItem(4).is(ThaumcraftContent.TABLE_ITEM.get())
                ? client.player.getInventory().getItem(4).getCount() : 0);
        snapshot.put("preview_research_inventory_count", client.player.getInventory().getItem(5).is(ThaumcraftContent.RESEARCH_TABLE_ITEM.get())
                ? client.player.getInventory().getItem(5).getCount() : 0);
        if (previewDropped != null && client.level.getEntity(previewDropped.entityId) instanceof ItemEntity dropped) {
            snapshot.put("base_item_entity", Map.of("uuid", dropped.getUUID().toString(), "count", dropped.getItem().getCount(),
                    "x", dropped.getX(), "y", dropped.getY(), "z", dropped.getZ()));
        }
        return snapshot;
    }

    private void clickEntry(Minecraft client, String key) throws ReflectiveOperationException {
        if (!(client.screen instanceof GuiResearchBrowser browser)) throw new IllegalStateException("Expected browser");
        var point = researchPoint(browser, key);
        if (!browser.mouseClicked(point[0], point[1], 0) || !(client.screen instanceof GuiResearchRecipe)) {
            throw new IllegalStateException("Mapped entry click did not open " + key + " at " + point[0] + "," + point[1]);
        }
    }

    private void clickUnfinished(Minecraft client, String key) throws ReflectiveOperationException {
        if (!(client.screen instanceof GuiResearchBrowser browser)) throw new IllegalStateException("Expected browser");
        var point = researchPoint(browser, key);
        if (!browser.mouseClicked(point[0], point[1], 0) || client.screen != browser) {
            throw new IllegalStateException("Unfinished entry click did not remain in native browser: " + key);
        }
    }

    private int[] researchPoint(GuiResearchBrowser browser, String key) throws ReflectiveOperationException {
        var research = ResearchCatalog.get(key);
        if (!research.category().equals(field(browser, "category"))) {
            throw new IllegalStateException("Wrong native category selected for " + key);
        }
        int x = (int) field(browser, "graphLeft") + (int) field(browser, "graphWidth") / 2
                + (int) (double) field(browser, "panX") + research.column() * 24 + 11;
        int y = (int) field(browser, "graphTop") + (int) field(browser, "graphHeight") / 2
                + (int) (double) field(browser, "panY") + research.row() * 24 + 11;
        return new int[]{x, y};
    }

    private boolean purchaseMatches(Minecraft client, boolean complete, int pannus, int magic) {
        var knowledge = KnowledgeAccess.get(client.player);
        return knowledge.isResearchComplete("ENCHFABRIC") == complete
                && knowledge.getAspectPoolFor("pannus") == pannus && knowledge.getAspectPoolFor("praecantatio") == magic;
    }

    private void requirePurchase(Minecraft client, boolean complete, int pannus, int magic) {
        if (!purchaseMatches(client, complete, pannus, magic)) {
            throw new IllegalStateException("Unexpected authoritative purchase state: " + purchaseSnapshot(client));
        }
    }

    private Map<String, Object> purchaseSnapshot(Minecraft client) {
        var knowledge = KnowledgeAccess.get(client.player);
        return Map.of("key", "ENCHFABRIC", "complete", knowledge.isResearchComplete("ENCHFABRIC"),
                "pannus", knowledge.getAspectPoolFor("pannus"), "praecantatio", knowledge.getAspectPoolFor("praecantatio"));
    }

    private boolean noteInventoryMatches(Minecraft client) {
        var inventory = client.player.getInventory();
        var normal = inventory.getItem(0);
        var discovery = inventory.getItem(1);
        var ink = inventory.getItem(2);
        return normal.is(ThaumcraftContent.RESEARCH_NOTE.get()) && normal.getCount() == 1
                && noteFixture.generated.equals(ItemResearchNotes.data(normal))
                && discovery.is(ThaumcraftContent.DISCOVERY.get()) && discovery.getCount() == 1
                && noteFixture.solved.equals(ItemResearchNotes.data(discovery))
                && ink.is(ThaumcraftContent.INKWELL.get()) && ink.getDamageValue() == 99 && ink.getMaxDamage() == 100;
    }

    private void requireNotePresentation(Minecraft client) throws ReflectiveOperationException {
        if (!noteInventoryMatches(client)) throw new IllegalStateException("Native inventory lost note fixture components");
        if (!client.getLanguageManager().getSelected().equals("en_us")) throw new IllegalStateException("Note scene is not English");
        var colors = (ItemColors) field(client, "itemColors");
        List<Map<String, Object>> presentation = new ArrayList<>();
        String researchName = Component.translatable(ResearchCatalog.get("NITOR").nameKey()).getString();
        for (int slot = 0; slot <= 2; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
            if (model == client.getModelManager().getMissingModel()) {
                throw new IllegalStateException("Missing native inventory item model for slot " + slot);
            }
            var tooltip = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
                    .stream().map(Component::getString).toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("slot", slot);
            item.put("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            item.put("missing_model", false);
            item.put("tooltip", tooltip);
            if (slot < 2) {
                var data = ItemResearchNotes.data(stack);
                if (!tooltip.contains(researchName) || colors.getColor(stack, 1) != (0xff000000 | data.color())
                        || colors.getColor(stack, 0) != -1) {
                    throw new IllegalStateException("Native note tooltip or layer tint is wrong in slot " + slot);
                }
                item.put("component_color", data.color());
                item.put("layer0_tint", colors.getColor(stack, 0));
                item.put("layer1_tint", colors.getColor(stack, 1));
            } else {
                item.put("damage", stack.getDamageValue());
                item.put("max_damage", stack.getMaxDamage());
            }
            presentation.add(item);
        }
        noteEvidence.put("native_inventory_presentation", presentation);
    }

    private int[] inventoryPoint(Minecraft client, InventoryScreen screen, int inventorySlot) throws ReflectiveOperationException {
        var slot = screen.getMenu().slots.stream()
                .filter(candidate -> candidate.container == client.player.getInventory() && candidate.getContainerSlot() == inventorySlot)
                .findFirst().orElseThrow(() -> new IllegalStateException("No native player slot " + inventorySlot));
        return new int[]{(int) field(screen, "leftPos") + slot.x + 8, (int) field(screen, "topPos") + slot.y + 8};
    }

    private void requireNotePools(PlayerKnowledge knowledge) {
        if (!knowledge.getAspectsDiscovered().equals(noteFixture.pools) || !warpSnapshot(knowledge).equals(noteFixture.warp)) {
            throw new IllegalStateException("Transferred NITOR learning/replay changed research points, discovery or warp");
        }
    }

    private void requireLearnedCopy(Minecraft client) {
        ItemStack held = client.player.getMainHandItem();
        if (!KnowledgeAccess.get(client.player).isResearchComplete("NITOR")
                || !held.is(ThaumcraftContent.DISCOVERY.get()) || held.getCount() != 1
                || !noteFixture.solved.equals(ItemResearchNotes.data(held))) {
            throw new IllegalStateException("Already completed client use consumed or changed the transferred note");
        }
        requireNotePools(KnowledgeAccess.get(client.player));
    }

    private Map<String, Object> noteSnapshot(Minecraft client) {
        var knowledge = KnowledgeAccess.get(client.player);
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int index = 0; index <= 2; index++) {
            var stack = client.player.getInventory().getItem(index);
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("slot", index);
            slot.put("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            slot.put("count", stack.getCount());
            slot.put("damage", stack.getDamageValue());
            var data = ItemResearchNotes.data(stack);
            if (data != null) {
                slot.put("note_key", data.key());
                slot.put("note_complete", data.complete());
                slot.put("note_cells", data.cells().size());
                slot.put("note_color", data.color());
            }
            slots.add(slot);
        }
        return Map.of("key", "NITOR", "complete", knowledge.isResearchComplete("NITOR"),
                "pools", knowledge.getAspectsDiscovered(), "warp", warpSnapshot(knowledge), "inventory", slots);
    }

    private static List<Integer> warpSnapshot(PlayerKnowledge knowledge) {
        return List.of(knowledge.getWarpPerm(), knowledge.getWarpSticky(), knowledge.getWarpTemp(), knowledge.getWarpCounter());
    }

    private void observeNoteRequests(Minecraft client) throws ReflectiveOperationException {
        Channel channel = (Channel) field(client.getConnection().getConnection(), "channel");
        channel.pipeline().addLast("thaumcraft-note-smoke", new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                if (message instanceof ServerboundCustomPayloadPacket packet
                        && packet.payload().type().id().equals(ResearchNoteRequestPayload.TYPE.id())) {
                    LOGGER.info("Client smoke observed primary note request {}", noteRequestPackets.incrementAndGet());
                }
                context.write(message, promise);
            }
        });
    }

    private static int paperCount(Player player) {
        return player.getInventory().items.stream().filter(stack -> stack.is(Items.PAPER)).mapToInt(ItemStack::getCount).sum();
    }

    private static int nitorNoteCount(Player player) {
        return player.getInventory().items.stream().filter(stack -> {
            var data = ItemResearchNotes.data(stack);
            return data != null && data.key().equals("NITOR");
        }).mapToInt(ItemStack::getCount).sum();
    }

    private static int nitorNoteSlot(Player player) {
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            var data = ItemResearchNotes.data(player.getInventory().getItem(index));
            if (data != null && data.key().equals("NITOR")) return index;
        }
        return -1;
    }

    private static boolean inkMatches(Player player, int damage) {
        var ink = player.getInventory().getItem(2);
        return ink.is(ThaumcraftContent.INKWELL.get()) && ink.getCount() == 1
                && ink.getDamageValue() == damage && ink.getMaxDamage() == 100;
    }

    private void requireIssuanceKnowledge(PlayerKnowledge knowledge) {
        if (!knowledge.save().equals(issuanceBaseline.state)) {
            throw new IllegalStateException("Note issuance changed research, discovered aspects, research points or warp");
        }
    }

    private void requireMissingIssuanceMaterials(Player player) {
        requireIssuanceKnowledge(KnowledgeAccess.get(player));
        if (nitorNoteCount(player) != 0 || paperCount(player) != 0 || !player.getInventory().getItem(2).isEmpty()) {
            throw new IllegalStateException("Missing-material primary click changed inventory");
        }
    }

    private void requireIssuedNote(Player player) {
        requireIssuanceKnowledge(KnowledgeAccess.get(player));
        int slot = nitorNoteSlot(player);
        if (slot < 0 || nitorNoteCount(player) != 1 || paperCount(player) != 1 || !inkMatches(player, 100)) {
            throw new IllegalStateException("Primary issuance/replay did not retain one note, one paper and exhausted ink");
        }
        var note = player.getInventory().getItem(slot);
        if (!note.is(ThaumcraftContent.RESEARCH_NOTE.get()) || !noteFixture.generated.equals(ItemResearchNotes.data(note))) {
            throw new IllegalStateException("Issued primary note does not contain the canonical unfinished NITOR grid");
        }
    }

    private Map<String, Object> issuanceSnapshot(Minecraft client) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        var knowledge = KnowledgeAccess.get(client.player);
        snapshot.put("key", "NITOR");
        snapshot.put("complete", knowledge.isResearchComplete("NITOR"));
        snapshot.put("pools", knowledge.getAspectsDiscovered());
        snapshot.put("warp", warpSnapshot(knowledge));
        snapshot.put("paper", paperCount(client.player));
        snapshot.put("ink_count", client.player.getInventory().getItem(2).getCount());
        snapshot.put("ink_damage", client.player.getInventory().getItem(2).getDamageValue());
        snapshot.put("note_count", nitorNoteCount(client.player));
        snapshot.put("request_packets", noteRequestPackets.get());
        int noteSlot = nitorNoteSlot(client.player);
        snapshot.put("note_slot", noteSlot);
        if (noteSlot >= 0) {
            var data = ItemResearchNotes.data(client.player.getInventory().getItem(noteSlot));
            snapshot.put("note_complete", data.complete());
            snapshot.put("note_cells", data.cells().size());
            snapshot.put("note_copies", data.copies());
            snapshot.put("note_color", data.color());
        }
        return snapshot;
    }

    private void turnPage(Minecraft client, String label, int change) throws ReflectiveOperationException {
        int previous = (int) field(client.screen, "page");
        clickButton(client, label);
        if ((int) field(client.screen, "page") != previous + change) {
            throw new IllegalStateException("Pagination handler did not apply expected change " + change);
        }
    }

    private void clickButton(Minecraft client, String label) {
        Screen screen = client.screen;
        var buttons = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
        Button button = buttons.stream().filter(widget -> widget.getMessage().getString().equals(label)).findFirst().orElse(null);
        if (button == null) {
            // The vanilla button of the screen is built while the screen initialises
            // and the language of the label can change between two ticks, so a
            // missing button is a "not ready yet" answer, not a defect of the port.
            // The stage retries until SCREEN_RETRY_TICKS and only then reports.
            throw new ScreenNotReadyException("Screen button is not present yet: " + label
                    + "; available=" + buttons.stream().map(widget -> widget.getMessage().getString()).toList());
        }
        if (!button.active || !button.visible
                || !client.screen.mouseClicked(button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, 0)) {
            throw new IllegalStateException("Screen button is unavailable: " + label);
        }
        screen.mouseReleased(button.getX() + 1, button.getY() + 1, 0);
    }

    /** The screen has not built the wanted button yet; the stage may retry. */
    private static final class ScreenNotReadyException extends RuntimeException {
        ScreenNotReadyException(String message) {
            super(message, null, false, false);
        }
    }

    /**
     * The original research browser has no category buttons: the categories are
     * drawn icons in the pane margin and picked by their own hit test
     * (oracle GuiResearchBrowser:668-687). The harness therefore clicks the real
     * tab icon instead of inventing a Button widget.
     */
    static void clickCategoryTab(Minecraft client, String key) throws ReflectiveOperationException {
        if (!(client.screen instanceof GuiResearchBrowser browser)) throw new IllegalStateException("Expected browser");
        Object tab = null;
        for (Object candidate : (List<?>) field(browser, "tabs")) {
            if (key.equals(field(candidate, "key"))) { tab = candidate; break; }
        }
        if (tab == null) throw new IllegalStateException("The native browser has no category tab " + key);
        int x = (int) field(tab, "x") + 12;
        int y = (int) field(tab, "y") + 12;
        if (!browser.mouseClicked(x, y, 0)) {
            throw new IllegalStateException("The native category tab did not accept the click: " + key);
        }
        if (!key.equals(field(browser, "category"))) {
            throw new IllegalStateException("The native category tab did not select " + key);
        }
        browser.mouseReleased(x, y, 0);
    }

    private boolean visibleIllustration(Screen screen) throws ReflectiveOperationException {
        int page = (int) field(screen, "page");
        var pages = (List<?>) field(screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) {
            for (Object draw : (List<?>) field(pages.get(i), "text")) if (field(draw, "image") != null) return true;
        }
        return false;
    }

    static Object field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                // Native inventory layout fields are inherited from AbstractContainerScreen.
            }
        }
        throw new NoSuchFieldException(owner.getClass().getName() + "." + name);
    }

    /** One allowlist for BOTH registration and request validation. New scenes
     * cannot silently request a screen whose afterRender handler never exists. */
    private static boolean supportsCapture(Screen screen) {
        return screen == null || screen instanceof GuiResearchBrowser || screen instanceof GuiResearchRecipe
                || screen instanceof InventoryScreen || screen instanceof GuiResearchTable
                || screen instanceof GuiArcaneWorkbench || screen instanceof GuiAlchemyFurnace
                || screen instanceof GuiFocusSelect || screen instanceof ChatScreen
                || screen instanceof thaumcraft.client.gui.GuiFocalManipulator
                || screen instanceof thaumcraft.client.gui.GuiArcaneBore
                || screen instanceof net.minecraft.client.gui.screens.inventory.FurnaceScreen
                || screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen
                || screen instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen;
    }

    private void capture(Minecraft client, String name, Stage following) {
        if (visnetTail != null && following == Stage.VISNET) visnetTail.capture("requested", name);
        if (capturePending) throw new IllegalStateException("Another screenshot is still pending: " + pendingScreenshotName);
        if (!supportsCapture(client.screen)) throw new IllegalStateException("No native afterRender capture handler for " + screenName(client));
        capturePending = true;
        captureDispatched = false;
        captureStartedNanos = System.nanoTime();
        pendingScreenshotName = name;
        pendingScreenshotStage = following;
        pendingScreenshotScreen = client.screen;
        pendingHoverKey = null;
        pendingInventorySlot = -1;
        // Preserve the actual pending screen/name and latest story observations,
        // rather than leave a stale report from the previous successful frame.
        writeResult("RUNNING", null);
    }

    private void grabRenderedFrame(Minecraft client, String name, Stage following) {
        if (visnetTail != null && following == Stage.VISNET) visnetTail.capture("after_render_before_native_PNG_grab", name);
        if (focal != null && following == Stage.FOCAL) focal.captureEvent("after_render_before_native_PNG_grab", name);
        if (bore != null && following == Stage.BORE) bore.captureEvent("after_render_before_native_PNG_grab", name);
        String filename = runId + "-" + name;
        var expected = runDirectory.resolve("screenshots").resolve(filename);
        var purchaseState = name.startsWith("09-") || name.startsWith("10-") ? purchaseSnapshot(client) : Map.of();
        var noteState = name.startsWith("11-") || name.startsWith("12-") ? noteSnapshot(client) : Map.of();
        var issuanceState = name.startsWith("13-") || name.startsWith("14-") ? issuanceSnapshot(client) : Map.of();
        var tableState = name.startsWith("15-") || name.startsWith("16-") || name.startsWith("17-")
                || name.startsWith("18-") || name.startsWith("19-") ? tableSnapshot(client) : Map.of();
        String capturePhase = pendingScreenshotScreen == null ? "HudRenderCallback.afterHud + GuiGraphics.flush" : "ScreenEvents.afterRender + GuiGraphics.flush";
        int inventoryHover = pendingInventorySlot;
        Screenshot.grab(client.gameDirectory, filename, client.getMainRenderTarget(), message -> client.execute(() -> {
            if (finished) return; // A late I/O callback cannot overwrite a timeout failure.
            try {
                if (!Files.isRegularFile(expected) || Files.size(expected) < 1000) {
                    throw new IllegalStateException("Native screenshot was not written: " + message.getString());
                }
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("path", expected.toString());
                evidence.put("bytes", Files.size(expected));
                evidence.put("screen", screenName(client));
                evidence.put("gui_width", client.screen == null ? 0 : client.screen.width);
                evidence.put("gui_height", client.screen == null ? 0 : client.screen.height);
                evidence.put("language", client.getLanguageManager().getSelected());
                evidence.put("capture_phase", capturePhase);
                if (!purchaseState.isEmpty()) evidence.put("research_purchase", purchaseState);
                if (!noteState.isEmpty()) {
                    evidence.put("research_note", noteState);
                    evidence.put("hovered_inventory_slot", inventoryHover);
                }
                if (!issuanceState.isEmpty()) {
                    evidence.put("research_note_issuance", issuanceState);
                    if (inventoryHover >= 0) evidence.put("hovered_inventory_slot", inventoryHover);
                }
                if (client.screen instanceof GuiResearchRecipe) evidence.put("first_page_index", field(client.screen, "page"));
                if (!tableState.isEmpty()) evidence.put("research_table", tableState);
                screenshots.add(evidence);
                if (visnetTail != null && following == Stage.VISNET) visnetTail.capture("native_PNG_written", name);
                if (focal != null && following == Stage.FOCAL) focal.captureEvent("native_PNG_written", name);
                if (bore != null && following == Stage.BORE) bore.captureEvent("native_PNG_written", name);
                capturePending = false;
                captureStartedNanos = 0;
                advance(client, following, "Captured " + name);
            } catch (Throwable error) {
                fail(client, error);
            }
        }));
    }

    private void advance(Minecraft client, Stage following, String action) {
        actions.add(Map.of("at", Instant.now().toString(), "action", action, "screen", screenName(client)));
        LOGGER.info("Client smoke: {} -> {} ({})", stage, following, action);
        stage = following;
        stageTicks = 0;
        writeResult("RUNNING", null);
    }

    private static String screenName(Minecraft client) {
        return client.screen == null ? "none" : client.screen.getClass().getName();
    }

    private void fail(Minecraft client, Throwable error) {
        if (finished) return;
        finished = true;
        LOGGER.error("CLIENT_SMOKE_SCRIPT_FAILED at " + stage, error);
        if (focal != null) {
            try { focal.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
        }
        if (bore != null) {
            try { bore.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
        }
        writeResult("SCRIPT_FAILED", error);
        client.stop();
    }

    private void writeResult(String status, Throwable error) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", status);
        report.put("stage", stage.name());
        report.put("captured_utc", Instant.now().toString());
        report.put("run_directory", runDirectory.toString());
        report.put("run_id", runId);
        report.put("scope", researchHandsOnly ? "researchhands" : boreOnly ? "bore" : focalOnly ? "focal" : visnetOnly ? "visnet" : stabilizerOnly ? "stabilizer" : crystallizerOnly ? "crystallizer" : centrifugeOnly ? "centrifuge" : bufferOnly ? "buffer" : tubeFilterOnly ? "tubefilter" : valveOnly ? "valve" : voidJarOnly ? "voidjar" : frostOnly ? "frost" : excavationOnly ? "excavation" : clothingOnly ? "clothing" : gogglesOnly ? "goggles" : capsOnly ? "caps" : equipmentOnly ? "equipment" : "full");
        report.put("research_hands", researchHands == null ? Map.of() : researchHands.evidence);
        report.put("previous_content_replayed", !researchHandsOnly && !equipmentOnly && !capsOnly && !gogglesOnly && !clothingOnly && !excavationOnly && !frostOnly && !voidJarOnly && !valveOnly && !tubeFilterOnly && !bufferOnly && !centrifugeOnly && !crystallizerOnly && !stabilizerOnly && !visnetOnly && !focalOnly && !boreOnly);
        report.put("original_world_modified", false);
        report.put("visual_inspection", "pending; script completion is not visual parity evidence");
        report.put("actions", actions);
        report.put("screenshots", screenshots);
        report.put("screenshot_capture", Map.of("pending", capturePending, "render_dispatched", captureDispatched,
                "name", pendingScreenshotName == null ? "" : pendingScreenshotName,
                "screen", pendingScreenshotScreen == null ? "HUD" : pendingScreenshotScreen.getClass().getName(),
                "timeout_seconds", 10, "pending_elapsed_millis", capturePending && captureStartedNanos > 0
                        ? (System.nanoTime() - captureStartedNanos) / 1_000_000L : 0));
        report.put("research_purchase", purchaseEvidence);
        report.put("research_note_learning", noteEvidence);
        report.put("research_note_issuance", issuanceEvidence);
        report.put("research_table", tableEvidence);
        report.put("world_open", worldOpenEvidence);
        report.put("early_content", earlyContent == null ? Map.of() : earlyContent.evidence);
        report.put("alchemy", alchemy == null ? Map.of() : alchemy.evidence);
        report.put("distillation", distillation == null ? Map.of() : distillation.evidence);
        report.put("focus", focus == null ? Map.of() : focus.evidence);
        report.put("tubes", tubes == null ? Map.of() : tubes.report);
        report.put("infusion", infusion == null ? Map.of() : infusion.report);
        report.put("equipment", equipment == null ? Map.of() : equipment.report);
        report.put("caps", caps == null ? Map.of() : caps.report);
        report.put("goggles", goggles == null ? Map.of() : goggles.report);
        report.put("clothing", robeShock == null ? Map.of() : robeShock.report);
        report.put("excavation", excavation == null ? Map.of() : excavation.report);
        report.put("frost", frost == null ? Map.of() : frost.report);
        report.put("voidjar", voidJar == null ? Map.of() : voidJar.report);
        report.put("valve", valve == null ? Map.of() : valve.report);
        report.put("tubefilter", tubeFilter == null ? Map.of() : tubeFilter.report);
        report.put("buffer", buffer == null ? Map.of() : buffer.report);
        report.put("centrifuge", centrifuge == null ? Map.of() : centrifuge.report);
        report.put("crystallizer", crystallizer == null ? Map.of() : crystallizer.report);
        report.put("stabilizer", stabilizer == null ? Map.of() : stabilizer.report);
        report.put("visnet", visnet == null ? Map.of() : visnet.report);
        report.put("focal", focal == null ? Map.of() : focal.report);
        report.put("bore", bore == null ? Map.of() : bore.progressSnapshot());
        if (error != null) {
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            report.put("error", stack.toString());
        }
        try {
            Files.createDirectories(runDirectory);
            try (var output = Files.newBufferedWriter(runDirectory.resolve("client-smoke-result.json"), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(report, output);
                output.write('\n');
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot record client smoke result", failure);
        }
    }

    private record NoteFixture(ResearchNoteData generated, ResearchNoteData solved,
                               Map<String, Integer> pools, List<Integer> warp) {}
    private record TableFixture(BlockPos main, BlockPos secondary, CompoundTag knowledge) {}
    private record PreviewDrop(int entityId, UUID uuid) {}
    private record IssuanceBaseline(CompoundTag state) {}
}
