package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
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
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ThaumcraftGogglesClient;
import thaumcraft.client.fx.NodeDrainClientEffects;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileNode;

/** Fresh finite native fixtures; real crosshair goggles HUD and packet-driven first-person node use. */
public final class WandCrucibleClientProbe implements ClientModInitializer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("thaumcraft-wand-crucible-probe");
    private static final BlockPos CRUCIBLE = new BlockPos(0, 100, 1), NODE = new BlockPos(4, 101, 1);
    private final String runId = "wand-crucible-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> captures = new ArrayList<>(), samples = new ArrayList<>(), hudChecks = new ArrayList<>();
    private CompletableFuture<Void> operation;
    private CompletableFuture<Map<String, Object>> serverVerification;
    private Map<String, Object> serverFacts = Map.of();
    private Path directory;
    private boolean created, stopped, pending, dispatched, holding;
    private int stage, ticks, total, chargingTicks, capturesDuringCharge, previousUse = -1;
    private int nextAfterCapture;
    private String pendingName;
    private long initialReceived, initialRendered;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.wandCrucibleClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!directory.getFileName().toString().equals("run-wand-crucible-client"))
            throw new IllegalStateException("Wand/crucible probe requires its isolated run directory");
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (!pending || dispatched || stopped || client.screen != null || client.getOverlay() != null) return;
            dispatched = true;
            try {
                graphics.flush();
                String fileName = runId + "-" + pendingName + ".png";
                Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), message -> client.execute(() -> {
                    if (stopped) return;
                    try {
                        Path path = directory.resolve("screenshots").resolve(fileName);
                        if (!Files.isRegularFile(path) || Files.size(path) < 1000) throw new IllegalStateException("Missing real screenshot " + path);
                        captures.add(Map.of("path", path.toString(), "language", client.getLanguageManager().getSelected(),
                                "name", pendingName, "bytes", Files.size(path), "screen", "native first-person world"));
                        pending = dispatched = false;
                        if (nextAfterCapture >= 0) advance(nextAfterCapture);
                        write("RUNNING", null);
                    } catch (Throwable error) { fail(client, error); }
                }));
            } catch (Throwable error) { fail(client, error); }
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 7200 || ++ticks > 1800) throw new IllegalStateException("Wand/crucible timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Wand and crucible finite supplied fixture", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(71338964L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (holding) {
                aim(client, Vec3.atCenterOf(NODE)); client.options.keyUse.setDown(true);
                sampleCharge(client);
            } else if (stage > 0 && stage < 12 && client.screen == null) aim(client, Vec3.atCenterOf(CRUCIBLE).add(0, -.1, 0));
            if (pending || client.getOverlay() != null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    operation = client.getSingleplayerServer().submit(() -> {
                        ServerPlayer actor = actor(client); var level = actor.serverLevel();
                        for (int x = -3; x <= 7; x++) for (int z = -2; z <= 6; z++) level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(CRUCIBLE.below(), Blocks.LAVA.defaultBlockState(), 3);
                        level.setBlock(CRUCIBLE, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), 3);
                        TileCrucible crucible = (TileCrucible) level.getBlockEntity(CRUCIBLE);
                        if (crucible.fillWater(1000, true) != 1000) throw new IllegalStateException("Native water fixture refused");
                        actor.teleportTo(.5, 100, 3.5); actor.setGameMode(GameType.SURVIVAL);
                        actor.getInventory().clearContent(); actor.getInventory().selected = 8;
                        actor.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
                        KnowledgeAccess.get(actor).reset(); KnowledgeEvents.sync(actor);
                        actor.inventoryMenu.sendAllDataToRemote();
                    });
                    advance(1);
                }
                case 1 -> {
                    if (!complete()) return;
                    if (!(client.level.getBlockEntity(CRUCIBLE) instanceof TileCrucible crucible) || crucible.getHeat() <= 150) return;
                    operation = client.getSingleplayerServer().submit(() -> {
                        ServerPlayer actor = actor(client); var level = actor.serverLevel();
                        TileCrucible tile = (TileCrucible) level.getBlockEntity(CRUCIBLE);
                        ItemEntity coal = new ItemEntity(level, CRUCIBLE.getX() + .5, CRUCIBLE.getY() + .4,
                                CRUCIBLE.getZ() + .5, new ItemStack(Items.COAL));
                        coal.setDeltaMovement(Vec3.ZERO); coal.setPickUpDelay(1000);
                        if (!level.addFreshEntity(coal)) throw new IllegalStateException("Native coal fixture was not spawned");
                        level.getBlockState(CRUCIBLE).entityInside(level, CRUCIBLE, coal);
                        if (!coal.isRemoved() || tile.getAspects().getAmount(Aspect.FIRE) != 2 || tile.getAspects().getAmount(Aspect.ENERGY) != 2)
                            throw new IllegalStateException("Actual native coal dissolution did not create ignis2/potentia2");
                        // Finite observation pool stays cold after genuine dissolution, without altering its aspects.
                        level.setBlock(CRUCIBLE.below(), Blocks.STONE.defaultBlockState(), 3);
                    });
                    advance(2);
                }
                case 2 -> { if (complete()) reload(client, "en_us", 3); }
                case 3 -> {
                    if (!complete() || ticks < 20 || !goggles(client)) return;
                    requireHud(client, false); capture("en-goggles-known-ignis-unknown-potentia", 4);
                }
                case 4 -> {
                    operation = client.getSingleplayerServer().submit(() -> {
                        ServerPlayer actor = actor(client); actor.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); actor.inventoryMenu.sendAllDataToRemote();
                    });
                    advance(5);
                }
                case 5 -> {
                    if (!complete() || ticks < 20 || !client.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
                    requireCrucibleTarget(client);
                    if (ThaumcraftGogglesClient.targetView(client) != null) throw new IllegalStateException("Aspects leaked without worn goggles");
                    hudChecks.add(Map.of("goggles", false, "target", CRUCIBLE.toShortString(), "aspect_popup_absent", true));
                    capture("en-unaided-no-aspect-popup", 6);
                }
                case 6 -> {
                    operation = client.getSingleplayerServer().submit(() -> {
                        ServerPlayer actor = actor(client); actor.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get())); actor.inventoryMenu.sendAllDataToRemote();
                    });
                    advance(7);
                }
                case 7 -> { if (complete()) reload(client, "ru_ru", 8); }
                case 8 -> {
                    if (!complete() || ticks < 20 || !goggles(client)) return;
                    requireHud(client, false); capture("ru-goggles-unknown-after-resource-reload", 9);
                }
                case 9 -> {
                    operation = client.getSingleplayerServer().submit(() -> { var actor = actor(client); KnowledgeAccess.get(actor).addDiscoveredAspect("potentia"); KnowledgeEvents.sync(actor); });
                    advance(10);
                }
                case 10 -> {
                    if (!complete() || ticks < 20 || !KnowledgeAccess.get(client.player).hasDiscoveredAspect("potentia")) return;
                    requireHud(client, true); capture("ru-goggles-both-aspects-known", 11);
                }
                case 11 -> {
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client); var level = actor.serverLevel();
                        level.setBlock(NODE, ThaumcraftContent.AURA_NODE.get().defaultBlockState(), 3);
                        TileNode node = (TileNode) level.getBlockEntity(NODE);
                        node.initializeNatural(new AspectList().add(Aspect.AIR, 30), null);
                        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
                        wand.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY);
                        wand.set(DataComponents.CUSTOM_NAME, Component.literal("Finite first-person charge fixture"));
                        actor.getInventory().clearContent(); actor.getInventory().setItem(0, wand); actor.getInventory().selected = 0;
                        actor.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); actor.teleportTo(4.5, 100, 4.5);
                        actor.inventoryMenu.sendAllDataToRemote();
                    });
                    advance(12);
                }
                case 12 -> {
                    if (!complete() || ticks < 30 || !(client.player.getMainHandItem().getItem() instanceof ItemWandCasting)
                            || !client.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
                    client.setScreen(null); aim(client, Vec3.atCenterOf(NODE));
                    if (!(client.player.pick(client.player.blockInteractionRange(), 1, true) instanceof BlockHitResult hit) || !hit.getBlockPos().equals(NODE)) return;
                    capture("first-person-wand-before-charge-no-goggles", 13);
                }
                case 13 -> {
                    aim(client, Vec3.atCenterOf(NODE));
                    var before = NodeDrainClientEffects.diagnostics(); initialReceived = before.received(); initialRendered = before.rendered();
                    holding = true; chargingTicks = 0; previousUse = -1;
                    client.options.keyUse.setDown(true);
                    if (!client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction()) throw new IllegalStateException("Actual wand use packet was refused");
                    advance(14);
                }
                case 14 -> {
                    if (chargingTicks >= 80) {
                        release(client);
                        serverVerification = client.getSingleplayerServer().submit(() -> {
                            var actor = actor(client); var node = (TileNode) actor.serverLevel().getBlockEntity(NODE);
                            int vis = ItemWandCasting.getVis(actor.getMainHandItem(), "aer"), left = node.getAspects().getAmount(Aspect.AIR);
                            if (vis < 800 || left >= 30) throw new IllegalStateException("Native server did not transfer enough finite node aura");
                            return Map.of("wand_aer_centivis", vis, "node_aer_remaining", left, "node_aer_initial", 30,
                                    "head_empty", actor.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "server_use_packet_session", true);
                        });
                        advance(15); return;
                    }
                    int target = capturesDuringCharge == 0 ? 15 : capturesDuringCharge == 1 ? 35 : 60;
                    if (capturesDuringCharge < 3 && chargingTicks >= target) {
                        var fx = NodeDrainClientEffects.diagnostics();
                        if (fx.received() <= initialReceived || fx.rendered() <= initialRendered || fx.active() == 0)
                            throw new IllegalStateException("Paid siphon did not reach actual client FloatyLine rendering");
                        capturesDuringCharge++; capture("first-person-active-charge-" + chargingTicks, -1);
                    }
                }
                case 15 -> {
                    if (!serverVerification.isDone() || ticks < 20) return;
                    serverFacts = serverVerification.join();
                    if (client.player.isUsingItem() || NodeDrainClientEffects.diagnostics().active() != 0)
                        throw new IllegalStateException("Released use kept its node siphon alive");
                    long visValues = samples.stream().map(s -> s.get("aer_centivis")).distinct().count();
                    if (samples.size() < 60 || visValues < 8) throw new IllegalStateException("Insufficient observed synchronized vis updates");
                    capture("first-person-released-charge-effect-stopped", 16);
                }
                case 16 -> { write("SCRIPT_PASSED", null); stopped = true; client.stop(); }
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable error) { fail(client, error); }
    }

    private void sampleCharge(Minecraft client) throws ReflectiveOperationException {
        chargingTicks++;
        int used = client.player.getTicksUsingItem();
        Object renderer = field(client.gameRenderer, "itemInHandRenderer");
        float height = ((Number) field(renderer, "mainHandHeight")).floatValue();
        if (chargingTicks > 8 && (!client.player.isUsingItem() || used <= previousUse || height < .95F))
            throw new IllegalStateException("First-person wand use/equip reset during synchronized charge: tick=" + chargingTicks + ", used=" + used + ", previous=" + previousUse + ", equip=" + height);
        previousUse = used;
        var fx = NodeDrainClientEffects.diagnostics();
        samples.add(Map.of("charge_tick", chargingTicks, "using_item", client.player.isUsingItem(), "use_ticks", used,
                "main_hand_height", height, "legacy_equipped_progress", UtilsFX.getEquippedProgress(1),
                "aer_centivis", ItemWandCasting.getVis(client.player.getMainHandItem(), "aer"),
                "fx_received", fx.received(), "fx_rendered", fx.rendered(), "fx_active", fx.active()));
    }

    private void requireHud(Minecraft client, boolean energyKnown) {
        requireCrucibleTarget(client);
        var view = ThaumcraftGogglesClient.targetView(client);
        if (view == null || !view.position().equals(CRUCIBLE) || view.entries().size() != 2)
            throw new IllegalStateException("Normally targeted native crucible did not expose two aspect popups");
        var fire = view.entries().stream().filter(a -> a.tag().equals("ignis")).findFirst().orElseThrow();
        var energy = view.entries().stream().filter(a -> a.tag().equals("potentia")).findFirst().orElseThrow();
        if (fire.amount() != 2 || !fire.known() || energy.amount() != 2 || energy.known() != energyKnown
                || energy.image().equals(fire.image())) throw new IllegalStateException("Native known/unknown aspect visibility or quantities differ");
        hudChecks.add(Map.of("goggles", true, "locale", client.getLanguageManager().getSelected(),
                "entries", view.entries().stream().map(a -> Map.of("tag", a.tag(), "amount", a.amount(), "known", a.known(), "image", a.image().toString())).toList()));
    }

    private static void requireCrucibleTarget(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(CRUCIBLE))
            throw new IllegalStateException("Actual crosshair must target crucible");
    }
    private static boolean goggles(Minecraft client) { return client.player.getItemBySlot(EquipmentSlot.HEAD).is(ThaumcraftWearables.GOGGLES.get()); }
    private void reload(Minecraft client, String locale, int next) {
        client.getLanguageManager().setSelected(locale); client.options.languageCode = locale; operation = client.reloadResourcePacks(); advance(next);
    }
    private boolean complete() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private void capture(String name, int next) { pendingName = name; nextAfterCapture = next; pending = true; dispatched = false; }
    private void advance(int next) { LOGGER.info("Wand/crucible probe stage {} -> {}", stage, next); stage = next; ticks = 0; }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
    }
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException absent) { /* parent class */ }
        }
        throw new NoSuchFieldException(name);
    }
    private void release(Minecraft client) {
        holding = false; client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player);
    }
    private void write(String status, Throwable error) {
        try {
            var report = new LinkedHashMap<String, Object>();
            report.put("status", status); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "fresh1.21.1 flat world; lava-heated water1000 and actual one-coal dissolution; finite natural aer30 node and empty wood/iron wand; supplied, not survival acquisition");
            report.put("hud_checks", hudChecks); report.put("charge_samples", samples); report.put("server_verification", serverFacts); report.put("captures", captures);
            report.put("particle_diagnostics", NodeDrainClientEffects.diagnostics());
            if (error != null) report.put("error", error.toString());
            Files.writeString(directory.resolve("wand-crucible-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException("Cannot write wand/crucible report", failure); }
    }
    private void fail(Minecraft client, Throwable error) {
        if (stopped) return;
        error.printStackTrace(); if (holding) release(client); write("FAILED", error); stopped = true; client.stop();
    }
}
