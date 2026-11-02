package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.client.renderers.models.gear.FortressArmorTrims;
import thaumcraft.client.renderers.models.gear.RobeArmorTrims;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ScanManager;

/** Supplied fresh-world visual fixtures; actual native sign, armour, book and timed scanner paths. */
public final class ModernAdaptationsClientProbe implements ClientModInitializer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("thaumcraft-modern-adaptations-probe");
    private static final List<String> SPECIES = List.of("greatwood", "silverwood");
    private static final List<String> WOOD_ITEMS = List.of("door", "trapdoor", "fence", "fence_gate", "sign", "hanging_sign", "button", "pressure_plate");
    private static final String FROG_SCAN = "@entity:minecraft:frog";
    private final String runId = "modern-adaptations-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private final List<UUID> stands = new ArrayList<>();
    private final List<UUID> robeStands = new ArrayList<>();
    private CompletableFuture<Void> operation;
    private CompletableFuture<Map<String, Object>> scanVerification;
    private CompletableFuture<Map<String, Object>> doorVerification;
    private CompletableFuture<Map<String, Object>> signVerification;
    private UUID frogId;
    private Path directory;
    private String captureName;
    private int stage, ticks, total, following;
    private boolean created, stopped, pending, dispatched, captureScreen, holding, activeScanCaptured;
    private boolean editingHanging;
    private long playerTrimBaseline;
    private long robeTrimBaseline;
    private Vec3 target = new Vec3(0, 101.3, 1);

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.modernAdaptationsClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!directory.getFileName().toString().equals("run-modern-adaptations-client"))
            throw new IllegalStateException("Modern adaptation probe requires its isolated run directory");
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.screen == null) captureFrame(client, graphics, false);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register(
                (rendered, graphics, mx, my, delta) -> {
                    if (rendered == client.screen) captureFrame(client, graphics, true);
                }));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void captureFrame(Minecraft client, GuiGraphics graphics, boolean screen) {
        if (!pending || dispatched || stopped || captureScreen != screen || client.getOverlay() != null) return;
        dispatched = true;
        try {
            graphics.flush();
            String name = captureName, fileName = runId + "-" + name + ".png";
            Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), result -> client.execute(() -> {
                if (stopped) return;
                try {
                    Path file = directory.resolve("screenshots").resolve(fileName);
                    if (!Files.isRegularFile(file) || Files.size(file) < 1000) throw new IllegalStateException("Missing native frame " + file);
                    captures.add(Map.of("name", name, "path", file.toString(), "bytes", Files.size(file),
                            "locale", client.getLanguageManager().getSelected(), "screen", client.screen == null
                                    ? "native world" : client.screen.getClass().getName()));
                    pending = dispatched = false;
                    if (following >= 0) advance(following);
                    write("RUNNING", null);
                } catch (Throwable error) { fail(client, error); }
            }));
        } catch (Throwable error) { fail(client, error); }
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 12000 || ++ticks > 1800) throw new IllegalStateException("Modern probe timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen first && client.getOverlay() == null) first.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.fov().set(78); client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Modern Thaumcraft finite supplied fixtures", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(71338967L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.screen == null && client.options.getCameraType() == CameraType.FIRST_PERSON) aim(client, target);
            if (holding) client.options.keyUse.setDown(true);
            if (pending || client.getOverlay() != null) return;
            switch (stage) {
                case 0 -> { if (ticks >= 30) { operation = client.getSingleplayerServer().submit(() -> setup(client)); advance(1); } }
                case 1 -> { if (complete()) reload(client, "en_us", 2); }
                case 2 -> {
                    if (!complete() || ticks < 30 || !woodLoaded(client)) return;
                    verifyWood(client); verifySigns(client); capture("en-woodwork-both-families", false, 3);
                }
                case 3 -> { move(client, -8, 101, 9, new Vec3(-7, 101, 1), 4); }
                case 4 -> { if (complete() && ticks > 20) capture("en-greatwood-door-trapdoor-fence-signs", false, 5); }
                case 5 -> { move(client, 8, 101, 9, new Vec3(7, 101, 1), 6); }
                case 6 -> { if (complete() && ticks > 20) capture("en-silverwood-door-trapdoor-fence-signs", false, 44); }
                case 7 -> { move(client, -10.5, 100, 3.5, Vec3.atCenterOf(door(0)), 8); }
                case 8 -> {
                    if (!complete() || ticks < 20) return;
                    BlockPos pos = door(0); aim(client, Vec3.atCenterOf(pos));
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0, .5), Direction.SOUTH, pos, false));
                    advance(9);
                }
                case 9 -> {
                    if (ticks < 15 || !client.level.getBlockState(door(0)).getValue(DoorBlock.OPEN)) return;
                    if (doorVerification == null) {
                        doorVerification = client.getSingleplayerServer().submit(() -> {
                            if (!actor(client).serverLevel().getBlockState(door(0)).getValue(DoorBlock.OPEN))
                                throw new IllegalStateException("Client door request did not commit on the real server");
                            return Map.of("real_client_door_use", "greatwood_door", "opened_on_server", true);
                        });
                        return;
                    }
                    if (!doorVerification.isDone()) return;
                    checks.add(doorVerification.join());
                    capture("en-greatwood-door-after-native-use", false, 10);
                }
                case 10 -> { client.setScreen(new InventoryScreen(client.player)); parkMouse(client); advance(11); }
                case 11 -> {
                    if (ticks < 15 || !(client.screen instanceof InventoryScreen)) return;
                    parkMouse(client); verifyWoodInventory(client); capture("en-native-woodwork-inventory", true, 12);
                }
                case 12 -> { client.setScreen(null); reload(client, "ru_ru", 13); }
                case 13 -> {
                    if (!complete() || ticks < 30) return;
                    verifyWood(client); verifySigns(client); client.setScreen(new InventoryScreen(client.player)); parkMouse(client); advance(14);
                }
                case 14 -> {
                    if (ticks < 15 || !(client.screen instanceof InventoryScreen)) return;
                    parkMouse(client); verifyWoodInventory(client); capture("ru-native-woodwork-inventory-after-reload", true, 15);
                }
                case 15 -> { client.setScreen(null); move(client, 0, 101, 16, new Vec3(0, 101.2, 1), 16); }
                case 16 -> { if (complete() && ticks > 20) capture("ru-woodwork-text-and-models-after-reload", false, 17); }
                case 17 -> { move(client, 0, 100, 32, new Vec3(0, 101.2, 20), 18); }
                case 18 -> {
                    if (!complete() || ticks < 30 || !armorLoaded(client)) return;
                    verifyArmor(client); capture("ru-native-fortress-void-trim-controls", false, 19);
                }
                case 19 -> { move(client, -4, 100, 25.5, new Vec3(-4, 101.2, 20), 20); }
                case 20 -> {
                    if (!complete() || ticks < 20) return;
                    var diagnostics = FortressArmorTrims.diagnostics();
                    if (diagnostics.rendered() <= 0 || diagnostics.lastSprite() == null || diagnostics.lastSprite().getPath().contains("missing"))
                        throw new IllegalStateException("Actual fortress trim pass did not draw a valid vanilla atlas sprite");
                    requireTrimSprite(client, diagnostics.lastSprite());
                    checks.add(Map.of("fortress_trim_rendered", diagnostics.rendered(), "last_sprite", diagnostics.lastSprite().toString()));
                    capture("ru-fortress-trim-diamond-sentry-gold-dune-close", false, 21);
                }
                case 21 -> {
                    playerTrimBaseline = FortressArmorTrims.diagnostics().rendered();
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client); equip(actor, true, 1);
                        var helm = actor.getItemBySlot(EquipmentSlot.HEAD);
                        helm.set(ThaumcraftEquipment.FORTRESS_GOGGLES.get(), true);
                        helm.set(ThaumcraftEquipment.FORTRESS_MASK.get(), 0);
                        actor.teleportTo(0, 100, 32); actor.inventoryMenu.sendAllDataToRemote();
                    });
                    client.options.setCameraType(CameraType.THIRD_PERSON_FRONT); advance(22);
                }
                case 22 -> {
                    if (!complete() || ticks <= 30) return;
                    var helmet = client.player.getItemBySlot(EquipmentSlot.HEAD);
                    if (!helmet.has(DataComponents.TRIM) || !Boolean.TRUE.equals(helmet.get(ThaumcraftEquipment.FORTRESS_GOGGLES.get()))
                            || helmet.getOrDefault(ThaumcraftEquipment.FORTRESS_MASK.get(), -1) != 0
                            || FortressArmorTrims.diagnostics().rendered() <= playerTrimBaseline)
                        throw new IllegalStateException("Actual worn fortress trim or existing mask/goggles did not synchronize/render");
                    checks.add(Map.of("actual_player_fortress_trim_rendered", true, "existing_mask_and_goggles_retained", true));
                    capture("ru-player-worn-fortress-trim-mask-goggles", false, 40);
                }
                case 23 -> {
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.player.getInventory().selected = 8;
                    client.player.connection.send(new ServerboundSetCarriedItemPacket(8));
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client);
                        for (EquipmentSlot slot : EquipmentSlot.values()) if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) actor.setItemSlot(slot, ItemStack.EMPTY);
                        actor.getInventory().setItem(8, new ItemStack(ThaumcraftContent.THAUMOMETER.get()));
                        actor.getInventory().selected = 8; actor.teleportTo(3.5, 100, 48.5); actor.inventoryMenu.sendAllDataToRemote();
                    });
                    target = new Vec3(3.5, 100.3, 44.5); advance(24);
                }
                case 24 -> {
                    if (!complete() || ticks < 25) return;
                    if (!(client.player.getMainHandItem().getItem() instanceof ItemThaumometer))
                        throw new IllegalStateException("Native selected hotbar slot did not expose the supplied thaumometer: selected="
                                + client.player.getInventory().selected + ", held=" + client.player.getMainHandItem());
                    var readout = ItemThaumometer.readout(client.player, client.player.getMainHandItem());
                    if (readout == null || !readout.signature().equals(FROG_SCAN) || readout.aspects().size() == 0) {
                        if (ticks > 100) throw new IllegalStateException("Native frog ray target unavailable: readout=" + readout
                                + ", frog=" + client.level.getEntity(findEntityId(client, frogId)) + ", position=" + client.player.position());
                        return;
                    }
                    if (ScanManager.hasBeenScanned(KnowledgeAccess.get(client.player), FROG_SCAN, ScanManager.ScanKind.ENTITY))
                        throw new IllegalStateException("Fresh frog fixture was already scanned");
                    capture("ru-new-frog-native-thaumometer-before-scan", false, 25);
                }
                case 25 -> {
                    holding = true; client.options.keyUse.setDown(true);
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND); advance(26);
                }
                case 26 -> {
                    if (!activeScanCaptured && client.player.isUsingItem() && client.player.getTicksUsingItem() >= 8) {
                        activeScanCaptured = true; capture("ru-new-frog-real-timed-scan-runes", false, -1); return;
                    }
                    if (!ScanManager.hasBeenScanned(KnowledgeAccess.get(client.player), FROG_SCAN, ScanManager.ScanKind.ENTITY)) return;
                    release(client);
                    scanVerification = client.getSingleplayerServer().submit(() -> {
                        var knowledge = KnowledgeAccess.get(actor(client));
                        if (!ScanManager.hasBeenScanned(knowledge, FROG_SCAN, ScanManager.ScanKind.ENTITY))
                            throw new IllegalStateException("Client scan did not commit real server knowledge");
                        return Map.of("real_native_entity_scan", FROG_SCAN, "server_knowledge_committed", true,
                                "frog_uuid", frogId.toString());
                    });
                    advance(27);
                }
                case 27 -> {
                    if (!scanVerification.isDone() || ticks < 12) return;
                    checks.add(scanVerification.join()); capture("ru-new-frog-native-thaumometer-known", false, 28);
                }
                case 28 -> {
                    target = new Vec3(0, 100.7, 60);
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client); actor.getInventory().setItem(8, ItemStack.EMPTY);
                        actor.teleportTo(0, 101, 68.5); actor.inventoryMenu.sendAllDataToRemote();
                    });
                    advance(29);
                }
                case 29 -> { if (complete() && ticks > 20) capture("ru-modern-crop-native-world-fixtures", false, 30); }
                case 30 -> { showBook(client, "MODERNINFUSION", false); advance(31); }
                case 31 -> { if (ticks > 15) { verifyBook(client, "MODERNINFUSION"); verifyBookSpread(client, "loyalty"); capture("ru-modern-infusion-native-book-first", true, 55); } }
                case 32 -> { showBook(client, "MODERNINFUSION", true); advance(33); }
                case 33 -> { if (ticks > 15) { verifyBookSpread(client, "density", "breach"); capture("ru-modern-infusion-native-book-last", true, 34); } }
                case 34 -> { showBook(client, "DEEPINFUSION", false); advance(35); }
                case 35 -> { if (ticks > 15) { verifyBook(client, "DEEPINFUSION"); verifyBookSpread(client, "wind_burst"); capture("ru-deep-infusion-native-book-first", true, 36); } }
                case 36 -> { showBook(client, "DEEPINFUSION", true); advance(37); }
                case 37 -> { if (ticks > 15) { verifyBookSpread(client, "soul_speed", "swift_sneak"); capture("ru-deep-infusion-native-book-last", true, 38); } }
                case 38 -> { write("SCRIPT_PASSED", null); stopped = true; client.stop(); }
                case 40 -> {
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    robeTrimBaseline = RobeArmorTrims.diagnostics().rendered();
                    move(client, 0, 100, 43, new Vec3(0, 101.2, 37), 41);
                }
                case 41 -> {
                    if (!complete() || ticks < 25) return;
                    verifyRobes(client);
                    var diagnostics = RobeArmorTrims.diagnostics();
                    if (diagnostics.rendered() <= robeTrimBaseline || diagnostics.lastSprite() == null)
                        throw new IllegalStateException("Actual custom robe trim pass did not render");
                    requireTrimSprite(client, diagnostics.lastSprite());
                    checks.add(Map.of("robe_trim_rendered", diagnostics.rendered(), "last_sprite", diagnostics.lastSprite().toString()));
                    capture("ru-dyed-void-robes-native-trim-control", false, 42);
                }
                case 42 -> { move(client, 3, 100, 40.5, new Vec3(3, 101.1, 37), 43); }
                case 43 -> { if (complete() && ticks > 20) capture("ru-dyed-void-robe-gold-sentry-trim-close", false, 23); }
                case 44 -> { prepareSignEditor(client, false); advance(45); }
                case 45 -> {
                    if (!complete() || ticks < 20) return;
                    BlockPos pos = editingSign();
                    if (!(client.level.getBlockEntity(pos) instanceof SignBlockEntity sign) || sign.isWaxed()) return;
                    aim(client, Vec3.atCenterOf(pos));
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0, .4), Direction.SOUTH, pos, false));
                    advance(46);
                }
                case 46 -> {
                    if (ticks < 15) return;
                    if (!(client.screen instanceof AbstractSignEditScreen editor)) {
                        if (ticks > 100) throw new IllegalStateException("Native sign interaction did not open its editor: " + editingSign());
                        return;
                    }
                    if (editingHanging ? !(editor instanceof HangingSignEditScreen) : !(editor instanceof SignEditScreen))
                        throw new IllegalStateException("Wrong native sign editor type: " + editor.getClass());
                    String[] messages = (String[]) field(editor, "messages");
                    if (!messages[0].equals(editingHanging ? "silverwood" : "greatwood")
                            || !messages[1].equals(editingHanging ? "Hanging" : "Standing")
                            || !Boolean.TRUE.equals(field(editor, "isFrontText")))
                        throw new IllegalStateException("Native editor did not preserve the supplied front text");
                    editor.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0); editor.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                    for (char letter : "Synced".toCharArray()) editor.charTyped(letter, 0);
                    if (!messages[2].equals("Synced")) throw new IllegalStateException("Native text field rejected typed sign text");
                    capture(editingHanging ? "en-silverwood-native-hanging-sign-editor" : "en-greatwood-native-sign-editor", true, 47);
                }
                case 47 -> { client.screen.onClose(); signVerification = null; advance(48); }
                case 48 -> {
                    if (ticks < 15) return;
                    if (signVerification == null) {
                        BlockPos pos = editingSign(); boolean hanging = editingHanging;
                        signVerification = client.getSingleplayerServer().submit(() -> {
                            var level = actor(client).serverLevel();
                            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)
                                    || !sign.getFrontText().getMessage(0, false).getString().equals(hanging ? "silverwood" : "greatwood")
                                    || !sign.getFrontText().getMessage(2, false).getString().equals("Synced"))
                                throw new IllegalStateException("Native sign editor close did not commit typed text on the server: " + pos);
                            sign.setWaxed(true); sign.setChanged(); level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                            return Map.of("native_sign_editor", hanging ? "hanging" : "standing", "existing_text_preserved", true,
                                    "typed_text_committed_on_server", "Synced");
                        });
                        return;
                    }
                    if (!signVerification.isDone()) return;
                    checks.add(signVerification.join());
                    if (editingHanging) {
                        client.player.getInventory().selected = 0;
                        client.player.connection.send(new ServerboundSetCarriedItemPacket(0));
                        advance(7);
                    }
                    else { prepareSignEditor(client, true); advance(45); }
                }
                case 55 -> { showBookSpread(client, 2); advance(56); }
                case 56 -> { if (ticks > 15) { verifyBookSpread(client, "impaling", "riptide"); capture("ru-modern-infusion-native-book-impaling-riptide", true, 57); } }
                case 57 -> { showBookSpread(client, 4); advance(58); }
                case 58 -> { if (ticks > 15) { verifyBookSpread(client, "channeling", "multishot"); capture("ru-modern-infusion-native-book-channeling-multishot", true, 59); } }
                case 59 -> { showBookSpread(client, 6); advance(60); }
                case 60 -> { if (ticks > 15) { verifyBookSpread(client, "piercing", "quick_charge"); capture("ru-modern-infusion-native-book-piercing-quick-charge", true, 32); } }
                default -> throw new IllegalStateException("Unknown modern probe stage " + stage);
            }
        } catch (Throwable error) { fail(client, error); }
    }

    private void setup(Minecraft client) {
        ServerPlayer actor = actor(client); var level = actor.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
        for (int x = -17; x <= 18; x++) for (int z = -4; z <= 70; z++) level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
        actor.setGameMode(GameType.SURVIVAL); actor.getInventory().clearContent(); actor.getInventory().selected = 8;
        actor.getAbilities().mayfly = true; actor.getAbilities().flying = true; actor.onUpdateAbilities();
        actor.teleportTo(0, 101, 16);
        for (int family = 0; family < SPECIES.size(); family++) {
            String species = SPECIES.get(family); int base = family == 0 ? -11 : 3;
            BlockState lower = block(species, "door").defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            level.setBlock(door(family), lower, 3); level.setBlock(door(family).above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
            level.setBlock(new BlockPos(base + 2, 100, 0), block(species, "trapdoor").defaultBlockState(), 3);
            level.setBlock(new BlockPos(base + 4, 100, 0), block(species, "fence").defaultBlockState(), 3);
            level.setBlock(new BlockPos(base + 5, 100, 0), block(species, "fence").defaultBlockState(), 3);
            level.setBlock(new BlockPos(base + 6, 100, 0), block(species, "fence_gate").defaultBlockState(), 3);
            BlockPos sign = new BlockPos(base, 100, 3), hanging = new BlockPos(base + 3, 102, 3);
            level.setBlock(sign, block(species, "sign").defaultBlockState(), 3);
            level.setBlock(hanging.above(), woodPlanks(species).defaultBlockState(), 3);
            level.setBlock(hanging, block(species, "hanging_sign").defaultBlockState(), 3);
            textSign(level.getBlockEntity(sign), species, "Standing"); textSign(level.getBlockEntity(hanging), species, "Hanging");
            for (BlockPos pos : List.of(sign, hanging)) level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            level.setBlock(new BlockPos(base + 6, 100, 3), block(species, "pressure_plate").defaultBlockState(), 3);
            level.setBlock(new BlockPos(base + 7, 100, 3), block(species, "button").defaultBlockState()
                    .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR), 3);
            for (int i = 0; i < WOOD_ITEMS.size(); i++) actor.getInventory().setItem(family * 9 + i, new ItemStack(block(species, WOOD_ITEMS.get(i))));
        }
        for (int i = 0; i < 5; i++) {
            double x = new double[]{-8, -4, 0, 5, 9}[i];
            ArmorStand stand = new ArmorStand(level, x, 100, 20);
            stand.setYRot(0); stand.setYBodyRot(0); stand.setYHeadRot(0); stand.setNoGravity(true); stand.setInvulnerable(true);
            stand.setShowArms(true); stand.setNoBasePlate(true); equip(stand, i < 3, i == 0 || i == 3 ? 0 : i == 2 ? 2 : 1);
            if (!level.addFreshEntity(stand)) throw new IllegalStateException("Native armour stand spawn failed"); stands.add(stand.getUUID());
        }
        for (int i = 0; i < 2; i++) {
            ArmorStand stand = new ArmorStand(level, i == 0 ? -3 : 3, 100, 37);
            stand.setYRot(0); stand.setYBodyRot(0); stand.setYHeadRot(0); stand.setNoGravity(true); stand.setInvulnerable(true);
            stand.setShowArms(true); stand.setNoBasePlate(true); equipRobe(stand, i > 0);
            if (!level.addFreshEntity(stand)) throw new IllegalStateException("Native robe stand spawn failed"); robeStands.add(stand.getUUID());
        }
        Frog frog = EntityType.FROG.create(level);
        if (frog == null) throw new IllegalStateException("No native frog entity");
        frog.moveTo(3.5, 100, 44.5, 0, 0); frog.setNoAi(true); frog.setNoGravity(true); frog.setInvulnerable(true);
        if (!level.addFreshEntity(frog)) throw new IllegalStateException("Native frog spawn failed"); frogId = frog.getUUID();
        var knowledge = KnowledgeAccess.get(actor);
        for (String tag : Aspect.aspects.keySet()) knowledge.addDiscoveredAspect(tag);
        for (var research : ResearchCatalog.all()) knowledge.addResearch(research.key());
        KnowledgeEvents.sync(actor); actor.inventoryMenu.sendAllDataToRemote();
        setupCrops(level);
    }

    private static void textSign(net.minecraft.world.level.block.entity.BlockEntity entity, String species, String kind) {
        if (!(entity instanceof SignBlockEntity sign)) throw new IllegalStateException("Native sign entity missing for " + species + " " + kind);
        var text = sign.getFrontText().setMessage(0, Component.literal(species)).setMessage(1, Component.literal(kind))
                .setColor(DyeColor.WHITE).setHasGlowingText(true);
        sign.setText(text, true); sign.setWaxed(true); sign.setChanged();
    }

    private BlockPos editingSign() { return editingHanging ? new BlockPos(6, 102, 3) : new BlockPos(-11, 100, 3); }

    private void prepareSignEditor(Minecraft client, boolean hanging) {
        editingHanging = hanging; BlockPos pos = editingSign(); target = Vec3.atCenterOf(pos);
        client.player.getInventory().selected = 8;
        client.player.connection.send(new ServerboundSetCarriedItemPacket(8));
        operation = client.getSingleplayerServer().submit(() -> {
            var actor = actor(client); var level = actor.serverLevel();
            actor.getInventory().setItem(8, ItemStack.EMPTY); actor.getInventory().selected = 8;
            actor.teleportTo(pos.getX() + .5, hanging ? 101 : 100, pos.getZ() + 2.5); actor.inventoryMenu.sendAllDataToRemote();
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) throw new IllegalStateException("Native sign editor fixture missing");
            sign.setWaxed(false); sign.setChanged(); level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        });
    }

    private static void setupCrops(net.minecraft.server.level.ServerLevel level) {
        for (int x = -5; x <= 5; x++) { level.setBlock(new BlockPos(x, 99, 60), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), 3); }
        level.setBlock(new BlockPos(-5, 100, 60), Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3), 3);
        level.setBlock(new BlockPos(-3, 100, 60), Blocks.TORCHFLOWER.defaultBlockState(), 3);
        var pitcher = Blocks.PITCHER_CROP.defaultBlockState().setValue(BlockStateProperties.AGE_4, 4);
        level.setBlock(new BlockPos(-1, 100, 60), pitcher.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(new BlockPos(-1, 101, 60), pitcher.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
        level.setBlock(new BlockPos(1, 99, 60), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(new BlockPos(1, 100, 60), Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3), 3);
        level.setBlock(new BlockPos(3, 102, 60), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(new BlockPos(3, 101, 60), Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.BERRIES, true), 3);
        level.setBlock(new BlockPos(0, 99, 61), Blocks.WATER.defaultBlockState(), 3);
    }

    private static void equip(LivingEntity entity, boolean fortress, int trimVariant) {
        Item[] items = fortress ? new Item[]{ThaumcraftEquipment.FORTRESS_HELMET.get(), ThaumcraftEquipment.FORTRESS_CHESTPLATE.get(), ThaumcraftEquipment.FORTRESS_LEGGINGS.get(), ThaumcraftEquipment.THAUMIUM_BOOTS.get()}
                : new Item[]{ThaumcraftEquipment.VOID_HELMET.get(), ThaumcraftEquipment.VOID_CHESTPLATE.get(), ThaumcraftEquipment.VOID_LEGGINGS.get(), ThaumcraftEquipment.VOID_BOOTS.get()};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = new ItemStack(items[i]);
            if (trimVariant > 0) {
                var materials = entity.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL);
                var patterns = entity.registryAccess().registryOrThrow(Registries.TRIM_PATTERN);
                stack.set(DataComponents.TRIM, new ArmorTrim(materials.getHolderOrThrow(trimVariant == 2 ? TrimMaterials.GOLD : fortress ? TrimMaterials.DIAMOND : TrimMaterials.EMERALD),
                        patterns.getHolderOrThrow(trimVariant == 2 ? TrimPatterns.DUNE : TrimPatterns.SENTRY)));
            }
            entity.setItemSlot(slots[i], stack);
        }
    }

    private static void equipRobe(LivingEntity entity, boolean trimmed) {
        Item[] items = {ThaumcraftEquipment.VOID_ROBE_HELMET.get(), ThaumcraftEquipment.VOID_ROBE_CHESTPLATE.get(),
                ThaumcraftEquipment.VOID_ROBE_LEGGINGS.get(), ThaumcraftEquipment.VOID_BOOTS.get()};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = new ItemStack(items[i]);
            if (i < 3) stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0x335599, true));
            if (trimmed) stack.set(DataComponents.TRIM, new ArmorTrim(
                    entity.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.GOLD),
                    entity.registryAccess().registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.SENTRY)));
            entity.setItemSlot(slots[i], stack);
        }
    }

    private void verifyWood(Minecraft client) {
        for (String species : SPECIES) for (String kind : WOOD_ITEMS) {
            ItemStack stack = new ItemStack(block(species, kind));
            var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
            if (model == client.getModelManager().getMissingModel()) throw new IllegalStateException("Missing actual woodwork item model " + species + "_" + kind);
            String name = stack.getHoverName().getString();
            if (name.startsWith("block.") || name.startsWith("item.")) throw new IllegalStateException("Untranslated woodwork item " + name);
        }
        checks.add(Map.of("woodwork_models_and_names", 16, "locale", client.getLanguageManager().getSelected()));
    }

    private void verifyWoodInventory(Minecraft client) {
        Map<String, String> names = new LinkedHashMap<>();
        for (int family = 0; family < SPECIES.size(); family++) for (int i = 0; i < WOOD_ITEMS.size(); i++) {
            ItemStack actual = client.player.getInventory().getItem(family * 9 + i);
            String id = SPECIES.get(family) + "_" + WOOD_ITEMS.get(i);
            if (!actual.is(block(SPECIES.get(family), WOOD_ITEMS.get(i)).asItem())) throw new IllegalStateException("Wrong real inventory woodwork " + id);
            names.put(id, actual.getHoverName().getString());
        }
        checks.add(Map.of("native_inventory_names", names, "locale", client.getLanguageManager().getSelected()));
    }

    private void verifySigns(Minecraft client) {
        for (int family = 0; family < SPECIES.size(); family++) {
            int base = family == 0 ? -11 : 3;
            for (BlockPos pos : List.of(new BlockPos(base, 100, 3), new BlockPos(base + 3, 102, 3))) {
                if (!(client.level.getBlockEntity(pos) instanceof SignBlockEntity sign)
                        || !sign.getFrontText().getMessage(0, false).getString().equals(SPECIES.get(family))
                        || !sign.getFrontText().hasGlowingText() || !sign.isWaxed())
                    throw new IllegalStateException("Native sign text/colour/wax did not synchronize " + pos);
            }
        }
        checks.add(Map.of("native_sign_text_glow_wax", true, "locale", client.getLanguageManager().getSelected()));
    }

    private void verifyArmor(Minecraft client) {
        for (int i = 0; i < stands.size(); i++) {
            var entity = client.level.getEntity(findEntityId(client, stands.get(i)));
            if (!(entity instanceof ArmorStand stand)) throw new IllegalStateException("Native armour gallery entity absent");
            boolean trimmed = i != 0 && i != 3;
            if (stand.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.TRIM) != trimmed) throw new IllegalStateException("Trim/control equipment lost native component");
        }
        checks.add(Map.of("native_armor_stands", 5, "fortress_patterns", List.of("sentry/diamond", "dune/gold"), "void_pattern", "sentry/emerald"));
    }

    private void verifyRobes(Minecraft client) {
        for (int i = 0; i < robeStands.size(); i++) {
            var entity = client.level.getEntity(findEntityId(client, robeStands.get(i)));
            if (!(entity instanceof ArmorStand stand)) throw new IllegalStateException("Native robe gallery entity absent");
            var chest = stand.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.has(DataComponents.TRIM) != (i > 0) || chest.get(DataComponents.DYED_COLOR) == null
                    || chest.get(DataComponents.DYED_COLOR).rgb() != 0x335599)
                throw new IllegalStateException("Native dyed robe trim/control components differ from the fixture");
        }
        checks.add(Map.of("native_dyed_void_robe_control", true, "cloth_rgb", "335599", "trim_pattern", "sentry/gold"));
    }

    private static void requireTrimSprite(Minecraft client, ResourceLocation id) {
        var sprite = client.getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET).getSprite(id);
        if (sprite.contents().name().getPath().contains("missing")) throw new IllegalStateException("Missing actual native trim atlas sprite " + id);
    }

    private void showBook(Minecraft client, String key, boolean last) throws Exception {
        var research = ResearchCatalog.get(key);
        if (research == null) throw new IllegalStateException("Modern research is missing: " + key);
        GuiResearchRecipe book = new GuiResearchRecipe(new GuiResearchBrowser(), research);
        client.setScreen(book);
        if (last) {
            List<?> pages = (List<?>) field(book, "pages");
            int index = Math.max(0, pages.size() - 1) / 2 * 2;
            Field page = GuiResearchRecipe.class.getDeclaredField("page"); page.setAccessible(true); page.setInt(book, index);
            Method update = GuiResearchRecipe.class.getDeclaredMethod("updateButtons"); update.setAccessible(true); update.invoke(book);
        }
    }

    private void verifyBook(Minecraft client, String key) throws Exception {
        if (!(client.screen instanceof GuiResearchRecipe book)) throw new IllegalStateException("No native modern research page");
        Map<?, ?> displays = (Map<?, ?>) field(book, "recipes");
        List<String> recipes = new ArrayList<>();
        for (Object value : displays.values()) if (value instanceof ThaumcraftRecipeDisplay display && display.recipe() != null) recipes.add(display.recipe().id().toString());
        if (recipes.isEmpty()) throw new IllegalStateException("Modern research resolves no actual recipe display: " + key);
        checks.add(Map.of("native_research", key, "resolved_recipe_ids", recipes));
    }

    private void showBookSpread(Minecraft client, int page) {
        client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("MODERNINFUSION"), page));
    }

    private void verifyBookSpread(Minecraft client, String... expected) throws Exception {
        if (!(client.screen instanceof GuiResearchRecipe book)) throw new IllegalStateException("No native research spread");
        List<?> pages = (List<?>) field(book, "pages"); int page = (Integer) field(book, "page");
        List<String> visible = new ArrayList<>();
        for (int index = page; index < Math.min(page + 2, pages.size()); index++) {
            Object value = field(pages.get(index), "recipe");
            if (value instanceof ThaumcraftRecipeDisplay display && display.recipe() != null)
                visible.add(display.recipe().id().getPath().replace("infusion_enchantment/modern/", ""));
        }
        if (!visible.equals(List.of(expected))) throw new IllegalStateException("Unexpected actual native book spread " + page + ": " + visible);
        checks.add(Map.of("native_book_spread", page, "visible_recipe_icons", visible));
    }

    private boolean woodLoaded(Minecraft client) { return client.level.getBlockState(door(0)).is(block("greatwood", "door")); }
    private boolean armorLoaded(Minecraft client) { return !stands.isEmpty() && findEntityId(client, stands.getFirst()) >= 0; }
    private static int findEntityId(Minecraft client, UUID uuid) {
        for (var entity : client.level.entitiesForRendering()) if (entity.getUUID().equals(uuid)) return entity.getId();
        return -1;
    }
    private static Block block(String species, String kind) {
        var id = ResourceLocation.fromNamespaceAndPath("thaumcraft", species + "_" + kind);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalStateException("Missing native woodwork block " + id);
        return BuiltInRegistries.BLOCK.get(id);
    }
    private static Block woodPlanks(String species) { return block(species, "planks"); }
    private static BlockPos door(int family) { return new BlockPos(family == 0 ? -11 : 3, 100, 0); }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static Object field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException absent) { }
        }
        throw new NoSuchFieldException(name);
    }
    private void move(Minecraft client, double x, double y, double z, Vec3 look, int next) {
        target = look; operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(x, y, z)); advance(next);
    }
    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
    }
    private void reload(Minecraft client, String locale, int next) {
        client.getLanguageManager().setSelected(locale); client.options.languageCode = locale; operation = client.reloadResourcePacks(); advance(next);
    }
    private static void parkMouse(Minecraft client) throws ReflectiveOperationException {
        long window = client.getWindow().getWindow();
        GLFW.glfwSetCursorPos(window, 4, 4);
        Method move = client.mouseHandler.getClass().getDeclaredMethod("onMove", long.class, double.class, double.class);
        move.setAccessible(true); move.invoke(client.mouseHandler, window, 4.0, 4.0);
        if (client.mouseHandler.xpos() != 4.0 || client.mouseHandler.ypos() != 4.0)
            throw new IllegalStateException("Native cursor move did not park outside inventory slots");
    }
    private boolean complete() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private void capture(String name, boolean screen, int next) { captureName = name; captureScreen = screen; following = next; pending = true; dispatched = false; }
    private void advance(int next) { LOG.info("Modern probe stage {} -> {}", stage, next); stage = next; ticks = 0; }
    private void release(Minecraft client) { holding = false; client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); }
    private void write(String status, Throwable error) {
        try {
            var report = new LinkedHashMap<String, Object>();
            report.put("status", status); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "fresh1.21.1 flat world; supplied modern woodwork, labelled native sign text, native armour stands/components, all-aspect/research knowledge and frog; not survival acquisition");
            report.put("checks", checks); report.put("captures", captures);
            if (error != null) report.put("error", error.toString());
            Files.writeString(directory.resolve("modern-adaptations-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException("Cannot write modern probe result", failure); }
    }
    private void fail(Minecraft client, Throwable error) {
        if (stopped) return;
        error.printStackTrace(); if (holding && client.player != null) release(client); write("FAILED", error); stopped = true; client.stop();
    }
}
