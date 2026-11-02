package thaumcraft.fabric.testing;

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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.mixin.client.MinecraftItemColorsAccess;

/** Finite supplied native ore gallery; no reused world, invented item renderer or production hook. */
public final class DeepslateOreClientProbe implements ClientModInitializer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("thaumcraft-deepslate-ore-probe");
    private static final List<String> PRIMALS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");
    private static final Vec3 GALLERY = new Vec3(1.5, 101.5, .5);
    private final String runId = "deepslate-ore-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> captures = new ArrayList<>(), checks = new ArrayList<>();
    private CompletableFuture<Void> operation;
    private Path directory;
    private String captureName;
    private boolean created, stopped, pending, dispatched, screenCapture;
    private int stage, ticks, total, nextAfterCapture;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.deepslateOreClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!directory.getFileName().toString().equals("run-deepslate-ore-client"))
            throw new IllegalStateException("Deepslate ore probe requires its isolated run directory");
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (client.screen == null) captureRendered(client, graphics, false, delta.getGameTimeDeltaPartialTick(false));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register(
                (rendered, graphics, mouseX, mouseY, delta) -> {
                    if (rendered == client.screen && rendered instanceof InventoryScreen)
                        captureRendered(client, graphics, true, delta);
                }));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void captureRendered(Minecraft client, GuiGraphics graphics, boolean inventory, float delta) {
        if (!pending || dispatched || stopped || screenCapture != inventory || client.getOverlay() != null) return;
        dispatched = true;
        try {
            if (inventory) {
                int left = (client.screen.width - 176) / 2, top = (client.screen.height - 166) / 2;
                client.screen.render(graphics, left + 16, top + 150, delta);
            } else {
                graphics.drawString(client.font, "Native fixture: upper = stone; lower = deepslate; right = vanilla", 8, 8, 0xffffffff, true);
            }
            graphics.flush();
            String name = captureName;
            String fileName = runId + "-" + name + ".png";
            Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), message -> client.execute(() -> {
                if (stopped) return;
                try {
                    Path file = directory.resolve("screenshots").resolve(fileName);
                    if (!Files.isRegularFile(file) || Files.size(file) < 1000) throw new IllegalStateException("Missing native frame " + file);
                    captures.add(Map.of("name", name, "path", file.toString(), "bytes", Files.size(file),
                            "locale", client.getLanguageManager().getSelected(), "game_time", client.level.getGameTime(),
                            "screen", inventory ? "native InventoryScreen" : "native first-person world"));
                    pending = dispatched = false; advance(nextAfterCapture); write("RUNNING", null);
                } catch (Throwable failure) { fail(client, failure); }
            }));
        } catch (Throwable failure) { fail(client, failure); }
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 7200 || ++ticks > 1800) throw new IllegalStateException("Deepslate ore timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.setCameraType(CameraType.FIRST_PERSON); client.options.fov().set(65);
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Deepslate infused ore supplied gallery", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(71338965L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.screen == null) aim(client, stage >= 4 && stage <= 5 ? new Vec3(-3, 100.7, .5)
                    : stage >= 6 && stage <= 7 ? new Vec3(3, 100.7, .5) : GALLERY);
            if (pending || client.getOverlay() != null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client); var level = actor.serverLevel();
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
                        for (int x = -9; x <= 11; x++) for (int z = -3; z <= 14; z++)
                            level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                        actor.setGameMode(GameType.SURVIVAL); actor.getInventory().clearContent();
                        for (int i = 0; i < PRIMALS.size(); i++) {
                            var normal = ThaumcraftOres.INFUSED.get(PRIMALS.get(i)).get();
                            var deep = ThaumcraftOres.DEEPSLATE_INFUSED.get(PRIMALS.get(i)).get();
                            level.setBlock(normal(i), normal.defaultBlockState(), 3);
                            level.setBlock(deep(i), deep.defaultBlockState(), 3);
                            actor.getInventory().setItem(i, new ItemStack(deep));
                            actor.getInventory().setItem(i + 9, new ItemStack(normal));
                        }
                        level.setBlock(new BlockPos(8, 102, 0), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(new BlockPos(8, 100, 0), Blocks.DEEPSLATE.defaultBlockState(), 3);
                        actor.getInventory().setItem(6, new ItemStack(Blocks.DEEPSLATE));
                        actor.getInventory().selected = 8;
                        actor.getAbilities().mayfly = true; actor.getAbilities().flying = true; actor.onUpdateAbilities();
                        actor.teleportTo(1.5, 100, 12.5); actor.inventoryMenu.sendAllDataToRemote();
                    });
                    advance(1);
                }
                case 1 -> { if (complete()) reload(client, "en_us", 2); }
                case 2 -> {
                    if (!complete() || ticks < 40 || !galleryLoaded(client)) return;
                    verifyModels(client); capture("en-day-six-deepslate-and-stone-comparison", false, 3);
                }
                case 3 -> { move(client, -5.5, 101, 5, 4); }
                case 4 -> {
                    if (!complete() || ticks < 20) return;
                    capture("en-day-air-fire-water-front-side-top", false, 5);
                }
                case 5 -> { move(client, 7.5, 101, 5, 6); }
                case 6 -> {
                    if (!complete() || ticks < 20) return;
                    capture("en-day-earth-order-entropy-front-side-top", false, 7);
                }
                case 7 -> {
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client); var level = actor.serverLevel();
                        enclosure(client, true); level.setDayTime(18000); actor.teleportTo(1.5, 100, 12.5);
                    });
                    advance(8);
                }
                case 8 -> {
                    if (!complete() || ticks < 80) return;
                    if (client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, deep(2).south()) != 0) return;
                    checks.add(Map.of("enclosed_dark_gallery", true, "sky_light", 0));
                    capture("en-dark-six-deepslate-and-stone-comparison", false, 9);
                }
                case 9 -> { client.setScreen(new InventoryScreen(client.player)); advance(10); }
                case 10 -> {
                    if (ticks < 15 || !(client.screen instanceof InventoryScreen)) return;
                    verifyInventory(client); capture("en-native-inventory-six-deepslate-item-models", true, 11);
                }
                case 11 -> { client.setScreen(null); reload(client, "ru_ru", 12); }
                case 12 -> {
                    if (!complete() || ticks < 30 || !galleryLoaded(client)) return;
                    verifyModels(client); client.setScreen(new InventoryScreen(client.player)); advance(13);
                }
                case 13 -> {
                    if (ticks < 15 || !(client.screen instanceof InventoryScreen)) return;
                    verifyInventory(client); capture("ru-native-inventory-after-resource-reload", true, 14);
                }
                case 14 -> {
                    client.setScreen(null);
                    operation = client.getSingleplayerServer().submit(() -> {
                        enclosure(client, false); actor(client).serverLevel().setDayTime(6000);
                    });
                    advance(15);
                }
                case 15 -> {
                    if (!complete() || ticks < 80 || client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, deep(2).south()) < 14) return;
                    verifyModels(client); capture("ru-day-six-deepslate-after-resource-reload", false, 16);
                }
                case 16 -> {
                    if (ticks < 13) return;
                    capture("ru-day-six-deepslate-later-animation-frame", false, 17);
                }
                case 17 -> { write("SCRIPT_PASSED", null); stopped = true; client.stop(); }
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable failure) { fail(client, failure); }
    }

    private void verifyModels(Minecraft client) {
        var entries = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < PRIMALS.size(); i++) {
            var deep = ThaumcraftOres.DEEPSLATE_INFUSED.get(PRIMALS.get(i)).get();
            var normal = ThaumcraftOres.INFUSED.get(PRIMALS.get(i)).get();
            var state = client.level.getBlockState(deep(i));
            if (!state.is(deep) || !client.level.getBlockState(normal(i)).is(normal)) throw new IllegalStateException("Gallery is not actual registered ore state");
            var model = client.getBlockRenderer().getBlockModel(state);
            if (model == client.getModelManager().getMissingModel()) throw new IllegalStateException("Missing native deepslate block model");
            Set<String> textures = textures(model, state);
            if (!textures.contains("minecraft:block/deepslate") || !textures.contains("minecraft:block/deepslate_top")
                    || !textures.contains("thaumcraft:blocks/infusedore")) throw new IllegalStateException("Incorrect native deepslate/overlay textures " + textures);
            int blockTint = client.getBlockColors().getColor(state, client.level, deep(i), 0);
            int itemTint = ((MinecraftItemColorsAccess) client).thaumcraft$itemColors().getColor(new ItemStack(deep), 0);
            if ((blockTint & 0xffffff) != normal.tintColor() || (itemTint >>> 24) != 255 || (itemTint & 0xffffff) != normal.tintColor())
                throw new IllegalStateException("Native block/item tint provider differs from original primal tint " + PRIMALS.get(i));
            var sprite = client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("thaumcraft:blocks/infusedore"));
            long frames = sprite.contents().getUniqueFrames().count();
            if (frames < 2) throw new IllegalStateException("Original animated crystal sprite is not animated after resource load");
            entries.add(Map.of("aspect", PRIMALS.get(i), "block", BuiltInRegistries.BLOCK.getKey(deep).toString(),
                    "block_tint", blockTint, "item_argb", itemTint, "textures", textures, "animation_unique_frames", frames));
        }
        checks.add(Map.of("locale", client.getLanguageManager().getSelected(), "native_model_and_tint_checks", entries));
    }

    private void verifyInventory(Minecraft client) {
        var names = new LinkedHashMap<String, String>();
        for (int i = 0; i < PRIMALS.size(); i++) {
            ItemStack actual = client.player.getInventory().getItem(i);
            if (!actual.is(ThaumcraftOres.DEEPSLATE_INFUSED.get(PRIMALS.get(i)).get().asItem()) || actual.getCount() != 1)
                throw new IllegalStateException("Native inventory did not synchronize the deepslate ore item");
            var model = client.getItemRenderer().getModel(actual, client.level, client.player, 0);
            if (model == client.getModelManager().getMissingModel()) throw new IllegalStateException("Missing native deepslate item model");
            Set<String> textures = textures(model, null);
            if (!textures.contains("minecraft:block/deepslate") || !textures.contains("thaumcraft:blocks/infusedore"))
                throw new IllegalStateException("Item uses incorrect deepslate ore textures " + textures);
            String name = actual.getHoverName().getString();
            if (name.equals(actual.getDescriptionId()) || name.startsWith("block.")) throw new IllegalStateException("Untranslated native ore item " + name);
            if (client.getLanguageManager().getSelected().equals("en_us") && !name.toLowerCase(java.util.Locale.ROOT).contains("deepslate"))
                throw new IllegalStateException("English item name does not identify its deepslate host: " + name);
            names.put(PRIMALS.get(i), name);
        }
        checks.add(Map.of("locale", client.getLanguageManager().getSelected(), "native_inventory_names", names));
    }

    private static Set<String> textures(BakedModel model, BlockState state) {
        Set<String> out = new LinkedHashSet<>();
        for (var quad : model.getQuads(state, null, RandomSource.create(0))) out.add(quad.getSprite().contents().name().toString());
        for (Direction face : Direction.values()) for (var quad : model.getQuads(state, face, RandomSource.create(0)))
            out.add(quad.getSprite().contents().name().toString());
        return out;
    }
    private boolean galleryLoaded(Minecraft client) {
        return client.level.getBlockState(deep(0)).is(ThaumcraftOres.DEEPSLATE_INFUSED.get("aer").get())
                && client.player.getInventory().getItem(5).is(ThaumcraftOres.DEEPSLATE_INFUSED.get("perditio").get().asItem());
    }
    private static void enclosure(Minecraft client, boolean build) {
        var level = actor(client).serverLevel(); var state = (build ? Blocks.DEEPSLATE : Blocks.AIR).defaultBlockState();
        for (int x = -9; x <= 11; x++) for (int z = -3; z <= 14; z++) level.setBlock(new BlockPos(x, 106, z), state, 3);
        for (int y = 100; y < 106; y++) {
            for (int x = -9; x <= 11; x++) { level.setBlock(new BlockPos(x, y, -3), state, 3); level.setBlock(new BlockPos(x, y, 14), state, 3); }
            for (int z = -2; z < 14; z++) { level.setBlock(new BlockPos(-9, y, z), state, 3); level.setBlock(new BlockPos(11, y, z), state, 3); }
        }
    }
    private static BlockPos deep(int index) { return new BlockPos(index * 2 - 5, 100, 0); }
    private static BlockPos normal(int index) { return new BlockPos(index * 2 - 5, 102, 0); }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void move(Minecraft client, double x, double y, double z, int next) {
        operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(x, y, z)); advance(next);
    }
    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
    }
    private void reload(Minecraft client, String language, int next) {
        client.getLanguageManager().setSelected(language); client.options.languageCode = language;
        operation = client.reloadResourcePacks(); advance(next);
    }
    private boolean complete() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private void capture(String name, boolean inventory, int next) { captureName = name; screenCapture = inventory; nextAfterCapture = next; pending = true; dispatched = false; }
    private void advance(int next) { LOGGER.info("Deepslate ore probe stage {} -> {}", stage, next); stage = next; ticks = 0; }
    private void write(String status, Throwable failure) {
        try {
            var report = new LinkedHashMap<String, Object>();
            report.put("status", status); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "fresh flat1.21.1 native world; supplied six deepslate and six ordinary infused ore blocks/items; no survival acquisition claim");
            report.put("gallery", "ordinary y102, deepslate y100, aspect order aer/ignis/aqua/terra/ordo/perditio at x-5/-3/-1/1/3/5,z0; vanilla stone/deepslate x8");
            report.put("checks", checks); report.put("captures", captures);
            if (failure != null) report.put("error", failure.toString());
            Files.writeString(directory.resolve("deepslate-ore-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failureWriting) { throw new IllegalStateException("Cannot write deepslate ore report", failureWriting); }
    }
    private void fail(Minecraft client, Throwable failure) {
        if (stopped) return; failure.printStackTrace(); write("FAILED", failure); stopped = true; client.stop();
    }
}
