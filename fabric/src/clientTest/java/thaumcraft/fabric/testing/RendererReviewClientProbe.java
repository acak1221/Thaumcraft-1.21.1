package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.client.renderers.entity.RenderEldritchWarden;
import thaumcraft.client.renderers.models.ModelThaumatorium;
import thaumcraft.client.renderers.tile.TileEldritchCapRenderer;
import thaumcraft.client.renderers.tile.TileEldritchCrabSpawnerRenderer;
import thaumcraft.client.renderers.tile.TileEldritchCrystalRenderer;
import thaumcraft.client.renderers.tile.TileFluxScrubberRenderer;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftVillageProfessions;
import thaumcraft.common.blocks.BlockThaumatorium;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.entities.monster.EntityEldritchWarden;
import thaumcraft.common.entities.monster.EntityTaintChicken;
import thaumcraft.common.tiles.TileEldritchCrabSpawner;
import thaumcraft.common.tiles.TileEldritchCrystal;
import thaumcraft.common.lib.world.FamilyVariant;
import thaumcraft.common.lib.world.EldritchBlockRules;

/** Isolated actual native renderer/model/buffer regressions; no replacement vertex consumer. */
public final class RendererReviewClientProbe implements ClientModInitializer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("thaumcraft-renderer-review");
    private static final float EPSILON = 1.0E-5F;
    private final String runId = "renderer-review-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private final List<Map<String, Object>> captures = new ArrayList<>();
    private final List<Map<String, Object>> villagerMaterials = new ArrayList<>();
    private final List<Map<String, Object>> villagerGeometry = new ArrayList<>();
    private final List<UUID> visualEntities = new ArrayList<>();
    private Path directory;
    private CompletableFuture<Void> operation;
    private int ticks, settle, visualPhase, visualTicks;
    private String captureName;
    private boolean captureDispatched;
    private boolean created, stopped;
    private CompletableFuture<TailImageCacheReviewChecks.Result> imageCacheReview;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.rendererReview")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!directory.getFileName().toString().equals("run-review-renderers"))
            throw new IllegalStateException("Renderer review requires its isolated run directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (captureName == null || captureDispatched || stopped || client.screen != null || client.getOverlay() != null) return;
            captureDispatched = true;
            graphics.flush();
            String name = captureName, fileName = runId + '-' + name + ".png";
            Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), ignored -> client.execute(() -> {
                try {
                    var file = directory.resolve("screenshots").resolve(fileName);
                    require(Files.isRegularFile(file) && Files.size(file) > 1000, "native screenshot was not written: " + file);
                    captures.add(Map.of("name", name, "path", file.toString(), "bytes", Files.size(file),
                            "game_time", client.level.getGameTime(), "visual_status", "captured; human inspection required"));
                    captureName = null; captureDispatched = false; visualPhase++; visualTicks = 0;
                } catch (Exception failure) {
                    failures.add("native screenshot: " + failure); finish(client);
                }
            }));
        });
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++ticks > 3000) throw new IllegalStateException("Renderer review timed out entering its fresh world");
            if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen first && client.getOverlay() == null) first.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.tutorialStep = TutorialSteps.NONE;
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Renderer review supplied fixtures", GameType.CREATIVE, false,
                                    Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(73471008L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || ++settle < 30) return;
            if (visualPhase == 0) {
                if (imageCacheReview == null) {
                    imageCacheReview = TailImageCacheReviewChecks.run(client);
                    return;
                }
                if (!imageCacheReview.isDone()) return;
                var images = imageCacheReview.join();
                checks.addAll(images.checks());
                failures.addAll(images.failures());
                runChecks(client);
                if (!failures.isEmpty()) { finish(client); return; }
                setupVisualFixtures(client);
                visualPhase = 1; visualTicks = 0;
            } else {
                visualFixtures(client);
            }
        } catch (Throwable failure) {
            failures.add("probe setup: " + failure);
            LOG.error("Renderer review setup failed", failure);
            finish(client);
        }
    }

    /** Supplied blocks and mobs in the actual integrated-server world, rendered by the ordinary world pipeline. */
    private void setupVisualFixtures(Minecraft client) {
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.options.fov().set(65);
        client.setScreen(null);
        operation = client.getSingleplayerServer().submit(() -> {
            var player = actor(client);
            var level = player.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.setDayTime(6000);
            for (int x = -9; x <= 10; x++) for (int z = -19; z <= 14; z++)
                level.setBlock(new BlockPos(x, 99, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            var machine = new BlockPos(-5, 101, 0);
            level.setBlock(machine.below(), ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), 2);
            level.setBlock(machine, ThaumcraftThaumatorium.THAUMATORIUM.get().defaultBlockState()
                    .setValue(BlockThaumatorium.FACING, Direction.SOUTH), 2);
            level.setBlock(machine.above(), ThaumcraftThaumatorium.THAUMATORIUM_TOP.get().defaultBlockState()
                    .setValue(BlockThaumatorium.FACING, Direction.SOUTH), 2);
            level.setBlock(new BlockPos(-1, 100, 0), FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(),
                    EldritchBlockRules.ELDRITCH_CAPSTONE), 3);
            level.setBlock(new BlockPos(2, 100, 0), ThaumcraftEldritchBlocks.ELDRITCH_CRYSTAL.get().defaultBlockState(), 3);
            level.setBlock(new BlockPos(5, 100, 0), ThaumcraftEldritchBlocks.ELDRITCH_SPAWNER.get().defaultBlockState(), 3);
            if (level.getBlockEntity(new BlockPos(5, 100, 0)) instanceof TileEldritchCrabSpawner vent) {
                vent.count = Integer.MAX_VALUE;
                vent.setFacing((byte) Direction.SOUTH.get3DDataValue());
            }
            level.setBlock(new BlockPos(7, 100, 0), ThaumcraftEldritchBlocks.STONE_DEVICE.get().defaultBlockState()
                    .setValue(BlockStoneDevice.VARIANT, EldritchBlockRules.DEVICE_STONE_FLUX_SCRUBBER), 3);
            var warden = java.util.Objects.requireNonNull(ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get().create(level));
            warden.setNoAi(true); warden.setNoGravity(true); warden.setPersistenceRequired();
            warden.setPos(-3, 100, -6); warden.setYRot(30); warden.setYHeadRot(65); warden.yBodyRot = 30;
            // Skip only the supplied server fixture's protected spawn period; client constructor still animates its normal entry.
            try {
                var timer = EntityEldritchWarden.class.getSuperclass().getDeclaredField("spawnTimer");
                timer.setAccessible(true); timer.setInt(warden, 0);
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            level.addFreshEntity(warden); visualEntities.add(warden.getUUID());
            for (int i = 0; i < 2; i++) {
                var chicken = java.util.Objects.requireNonNull(ThaumcraftTaintAnimals.TAINT_CHICKEN.get().create(level));
                chicken.setNoAi(true); chicken.setNoGravity(i != 0); chicken.setPersistenceRequired();
                chicken.setPos(1 + i * 3, 100 + i * 1.5, -6); chicken.setYRot(20); chicken.setYHeadRot(20);
                level.addFreshEntity(chicken); visualEntities.add(chicken.getUUID());
            }
            var professions = List.of(ThaumcraftVillageProfessions.BANKER.get(),
                    ThaumcraftVillageProfessions.WIZARD.get(), VillagerProfession.FARMER);
            var labels = List.of("TC4 banker", "TC4 wizard", "Vanilla farmer control");
            for (int i = 0; i < professions.size(); i++) {
                var villager = java.util.Objects.requireNonNull(EntityType.VILLAGER.create(level));
                villager.setVillagerData(new VillagerData(VillagerType.PLAINS, professions.get(i), 3));
                villager.setNoAi(true); villager.setPersistenceRequired();
                villager.setPos(-3 + i * 3, 100, -12);
                villager.setYRot(0); villager.setYHeadRot(0); villager.yBodyRot = 0;
                villager.setCustomName(Component.literal(labels.get(i))); villager.setCustomNameVisible(true);
                level.addFreshEntity(villager); visualEntities.add(villager.getUUID());
            }
            player.teleportTo(0, 100, 10);
        });
    }

    private void visualFixtures(Minecraft client) {
        if (captureName != null || operation == null || !operation.isDone()) return;
        operation.join();
        if (visualPhase == 1) aim(client, new Vec3(0, 101.25, 0));
        else if (visualPhase == 3 || visualPhase == 4) aim(client, new Vec3(0, 101.5, -6));
        else if (visualPhase == 6 || visualPhase == 8 || visualPhase == 10) aim(client, new Vec3(0, 101.25, -12));
        if (++visualTicks < (visualPhase == 1 ? 220 : 35)) return;
        if (visualPhase == 1) {
            require(client.level.getBlockEntity(new BlockPos(-5, 101, 0)) instanceof thaumcraft.common.tiles.TileThaumatorium,
                    "actual thaumatorium fixture did not synchronize");
            for (var uuid : visualEntities)
                require(client.level.entitiesForRendering().iterator().hasNext()
                                && java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
                                        .anyMatch(entity -> entity.getUUID().equals(uuid)), "native entity fixture did not synchronize: " + uuid);
            captureName = "world-thaumatorium-cap-crystal-vent-scrubber";
        } else if (visualPhase == 2) {
            operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(-1, 100, 1));
            visualPhase = 3; visualTicks = 0;
        } else if (visualPhase == 3) {
            captureName = "world-warden-and-grounded-flying-chickens";
        } else if (visualPhase == 4) {
            captureName = "world-warden-and-chickens-later-animation";
        } else if (visualPhase == 5) {
            operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(0, 100, -7));
            visualPhase = 6; visualTicks = 0;
        } else if (visualPhase == 6) {
            captureName = "world-original-banker-wizard-and-farmer-front";
        } else if (visualPhase == 7) {
            operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(4.5, 100, -8));
            visualPhase = 8; visualTicks = 0;
        } else if (visualPhase == 8) {
            captureName = "world-original-banker-wizard-and-farmer-oblique";
        } else if (visualPhase == 9) {
            operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(0, 100, -17));
            visualPhase = 10; visualTicks = 0;
        } else if (visualPhase == 10) {
            captureName = "world-original-banker-wizard-and-farmer-back";
        } else {
            finish(client);
        }
    }

    private static ServerPlayer actor(Minecraft client) {
        return java.util.Objects.requireNonNull(client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()));
    }

    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
    }

    private void runChecks(Minecraft client) {
        check("thaumatorium fills actual NEW_ENTITY overlay element", () -> {
            var model = new ModelThaumatorium();
            try (var storage = new ByteBufferBuilder(65536)) {
                var vertices = new BufferBuilder(storage, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
                model.render(new PoseStack(), vertices, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                try (var mesh = vertices.buildOrThrow()) {
                    require(mesh.drawState().vertexCount() > 0, "the retained thaumatorium mesh emitted no native vertices");
                }
            }
        });
        check("crab vent preserves every retained triangle through its actual renderer", () -> {
            var tile = new TileEldritchCrabSpawner(BlockPos.ZERO, ThaumcraftEldritchBlocks.ELDRITCH_SPAWNER.get().defaultBlockState());
            try (var batches = new NativeBatches()) {
                new TileEldritchCrabSpawnerRenderer(null).render(tile, 0, new PoseStack(), batches, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                requireTriangles(batches.snapshot().getFirst(), originalFaces(client, "crabvent", "Vent"));
            }
        });
        check("eldritch crystal preserves both retained triangle groups through its actual renderer", () -> {
            var tile = new TileEldritchCrystal(BlockPos.ZERO, ThaumcraftEldritchBlocks.ELDRITCH_CRYSTAL.get().defaultBlockState());
            try (var batches = new NativeBatches()) {
                new TileEldritchCrystalRenderer(null).render(tile, 0, new PoseStack(), batches, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                var meshes = batches.snapshot();
                require(meshes.size() == 2, "crystal must retain independently textured base and crystal groups");
                requireTriangles(meshes.get(0), originalFaces(client, "vcrystal", "Base"));
                requireTriangles(meshes.get(1), originalFaces(client, "vcrystal", "Crystal"));
            }
        });
        check("eldritch cap preserves its retained triangles through the cap renderer", () -> {
            try (var batches = new NativeBatches()) {
                new TileEldritchCapRenderer().render(new PoseStack(), batches, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, TileEldritchCapRenderer.CAP, false);
                requireTriangles(batches.snapshot().getFirst(), originalFaces(client, "obelisk_cap", "Cap"));
            }
        });
        check("flux scrubber retains its existing TRIANGLES buffer and both cap/tip groups", () -> {
            try (var batches = new NativeBatches()) {
                TileFluxScrubberRenderer.renderParts(Direction.DOWN, 13, new PoseStack(), batches,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                var mesh = batches.snapshot().getFirst();
                require(mesh.mode() == VertexFormat.Mode.TRIANGLES, "the shared cap model must preserve the scrubber TRIANGLES caller");
                requireTriangles(mesh, originalFaces(client, "obelisk_cap", "Cap") + originalFaces(client, "obelisk_cap", "Tip"));
            }
        });
        check("warden cloth and arms advance with whole entity ticks", () -> {
            var warden = warden(client);
            var renderer = (RenderEldritchWarden) client.getEntityRenderDispatcher().getRenderer(warden);
            warden.tickCount = 0;
            renderWarden(renderer, warden, .25F);
            float first = wardenRoot(renderer).getChild("ArmR1").xRot;
            warden.tickCount = 40;
            renderWarden(renderer, warden, .25F);
            float next = wardenRoot(renderer).getChild("ArmR1").xRot;
            require(Math.abs(first - next) > .01F, "whole tick advance left the actual arm pose unchanged: " + next);
            close(-1 - warden.armLiftR + Mth.sin(40.25F / 10F) * .08F, next, "original arm age");
        });
        check("warden additive hood eye follows the actual hood pose", () -> {
            var warden = warden(client);
            warden.setXRot(20); warden.xRotO = 20;
            warden.setYHeadRot(35); warden.yHeadRotO = 35;
            var renderer = (RenderEldritchWarden) client.getEntityRenderDispatcher().getRenderer(warden);
            renderWarden(renderer, warden, .5F);
            var root = wardenRoot(renderer);
            close(root.getChild("Hood1").xRot, root.getChild("HoodEye").xRot, "eye pitch follows hood");
            close(root.getChild("Hood1").yRot, root.getChild("HoodEye").yRot, "eye yaw follows hood");
            require(!root.getChild("HoodEye").visible, "the eye remains hidden from the next ordinary body pass");
        });
        check("warden body turns with native interpolated body yaw", () -> {
            var warden = warden(client);
            var renderer = (RenderEldritchWarden) client.getEntityRenderDispatcher().getRenderer(warden);
            var first = renderWarden(renderer, warden, .5F).getFirst().positions();
            warden.yBodyRot = warden.yBodyRotO = 90;
            warden.setYHeadRot(90); warden.yHeadRotO = 90;
            var turned = renderWarden(renderer, warden, .5F).getFirst().positions();
            require(first.size() == turned.size(), "body yaw changed the number of native vertices");
            // A head-only turn must not satisfy a body rotation check, regardless of root child iteration order.
            int changed = 0;
            for (int i = 0; i < first.size(); i++) if (first.get(i).distance(turned.get(i)) > .01F) changed++;
            require(changed > first.size() / 2, "ninety-degree body turn changed only " + changed + '/' + first.size() + " native body vertices");
            close(0, wardenRoot(renderer).getChild("Hood1").yRot, "head remains relative to the turned body");
        });
        check("grounded taint chicken folds native model wings", () -> chicken(client, 0, 0, 0, 0, 200));
        check("flying taint chicken feeds interpolated flap into native model wings", () -> chicken(client, .4F, 1.4F, .3F, .9F, 1337));
        legacyVillagerChecks(client, "banker", ThaumcraftVillageProfessions.BANKER.get(), "moneychanger");
        legacyVillagerChecks(client, "wizard", ThaumcraftVillageProfessions.WIZARD.get(), "wizard");
        check("native farmer retains its base, biome, profession and level materials after legacy roles", () -> {
            var rendered = renderVillager(client, "farmer control", VillagerProfession.FARMER);
            var expected = List.of(vanilla("textures/entity/villager/villager.png"),
                    vanilla("textures/entity/villager/type/plains.png"),
                    vanilla("textures/entity/villager/profession/farmer.png"),
                    vanilla("textures/entity/villager/profession_level/gold.png"));
            require(rendered.base().equals(expected.getFirst()), "native farmer base changed: " + rendered.base());
            require(rendered.textures().equals(expected), "native farmer materials changed: " + rendered.textures());
            require(rendered.missing().isEmpty(), "farmer requests unavailable materials: " + rendered.missing());
            var farmer = java.util.Objects.requireNonNull(EntityType.VILLAGER.create(client.level));
            var renderer = (VillagerRenderer) client.getEntityRenderDispatcher().getRenderer(farmer);
            var head = renderer.getModel().root().getChild("head");
            require(head.visible && head.getChild("hat").visible && head.getChild("hat").getChild("hat_rim").visible,
                    "legacy rendering leaked visibility into the shared native farmer model");
        });
        check("legacy heads remain visible without modern hat and rim during actual native vertex emission", () -> {
            var rendered = List.of(observeVillagerHead(client, "banker", ThaumcraftVillageProfessions.BANKER.get()),
                    observeVillagerHead(client, "wizard", ThaumcraftVillageProfessions.WIZARD.get()));
            for (var emission : rendered) {
                require(!emission.states().isEmpty() && emission.states().keySet().stream()
                                .allMatch(state -> state.head() && !state.hat() && !state.rim()),
                        emission.role() + " native emission visibility=" + emission.states());
                // Original 1.7.10 ModelVillager (bik): head, nose, body+robe, three arm boxes and two legs = nine cubes.
                require(emission.vertices() == 9 * 6 * 4, emission.role() + " native geometry includes added head layers: " + emission.vertices());
                require(emission.before().equals(emission.after()), emission.role() + " render leaked shared model visibility");
            }
        });
        check("native farmer head hat and rim remain visible during base emission after legacy renders", () -> {
            var emission = observeVillagerHead(client, "farmer after legacy geometry", VillagerProfession.FARMER);
            require(!emission.states().isEmpty() && emission.states().keySet().stream()
                            .allMatch(state -> state.head() && state.hat() && state.rim()),
                    "native farmer base emission inherited hidden head parts: " + emission.states());
            require(emission.vertices() == 11 * 6 * 4, "native farmer base lost geometry: " + emission.vertices());
            require(emission.before().equals(emission.after()), "farmer render leaked shared model visibility");
        });
    }

    private record HeadVisibility(boolean head, boolean hat, boolean rim) {}
    private record HeadEmission(String role, HeadVisibility before, HeadVisibility after,
            Map<HeadVisibility, Integer> states, int vertices) {}

    private HeadEmission observeVillagerHead(Minecraft client, String role, VillagerProfession profession) throws Exception {
        var villager = java.util.Objects.requireNonNull(EntityType.VILLAGER.create(client.level));
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, profession, 3));
        var renderer = (VillagerRenderer) client.getEntityRenderDispatcher().getRenderer(villager);
        var head = renderer.getModel().root().getChild("head");
        var hat = head.getChild("hat");
        var rim = hat.getChild("hat_rim");
        var before = new HeadVisibility(head.visible, hat.visible, rim.visible);
        var emitted = new LinkedHashMap<RenderType, Map<HeadVisibility, Integer>>();
        try (var batches = new NativeBatches(type -> emitted.computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                .merge(new HeadVisibility(head.visible, hat.visible, rim.visible), 1, Integer::sum))) {
            renderer.render(villager, 0, .5F, new PoseStack(), batches, LightTexture.FULL_BRIGHT);
            var base = renderer.getTextureLocation(villager);
            var baseStates = new LinkedHashMap<HeadVisibility, Integer>();
            for (var entry : emitted.entrySet()) if (NativeBatches.texture(entry.getKey()).equals(base))
                entry.getValue().forEach((state, count) -> baseStates.merge(state, count, Integer::sum));
            var meshes = batches.snapshot();
            require(!meshes.isEmpty(), "observed native renderer emitted no real buffer geometry");
            int vertices = baseStates.values().stream().mapToInt(Integer::intValue).sum();
            var after = new HeadVisibility(head.visible, hat.visible, rim.visible);
            villagerGeometry.add(Map.of("role", role, "actual_base_texture", base.toString(),
                    "before", before, "after", after, "base_vertices", vertices,
                    "during_base_emission", baseStates.entrySet().stream()
                            .map(entry -> Map.of("visibility", entry.getKey(), "vertices", entry.getValue())).toList()));
            return new HeadEmission(role, before, after, Map.copyOf(baseStates), vertices);
        }
    }

    private void legacyVillagerChecks(Minecraft client, String role, VillagerProfession profession, String skin) {
        var expected = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + skin + ".png");
        check("original " + role + " full skin is selected by the actual registered villager renderer", () -> {
            var rendered = renderVillager(client, role, profession);
            require(client.getResourceManager().getResource(expected).isPresent(), "original full skin is unavailable: " + expected);
            require(rendered.base().equals(expected) && rendered.textures().contains(expected),
                    "registered renderer selected " + rendered.base() + "; actual materials=" + rendered.textures() + "; expected=" + expected);
        });
        check("original " + role + " full skin has no incompatible modern villager overlays", () -> {
            var rendered = renderVillager(client, role, profession);
            require(rendered.missing().isEmpty(), "actual renderer requests unavailable materials: " + rendered.missing()
                    + "; all requested=" + rendered.textures());
            require(rendered.textures().equals(List.of(expected)), "modern biome/profession/rank materials cover original skin: " + rendered.textures());
        });
    }

    private record VillagerMaterials(ResourceLocation base, List<ResourceLocation> textures, List<ResourceLocation> missing) {}

    private VillagerMaterials renderVillager(Minecraft client, String role, VillagerProfession profession) throws Exception {
        Villager villager = java.util.Objects.requireNonNull(EntityType.VILLAGER.create(client.level));
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, profession, 3));
        villager.yBodyRot = villager.yBodyRotO = 0;
        villager.setYHeadRot(0); villager.yHeadRotO = 0;
        var registered = client.getEntityRenderDispatcher().getRenderer(villager);
        require(registered instanceof VillagerRenderer, "fixture did not use the registered native VillagerRenderer: " + registered);
        var renderer = (VillagerRenderer) registered;
        try (var batches = new NativeBatches()) {
            renderer.render(villager, 0, .5F, new PoseStack(), batches, LightTexture.FULL_BRIGHT);
            var textures = batches.textures();
            var meshes = batches.snapshot();
            require(!meshes.isEmpty() && meshes.stream().allMatch(mesh -> !mesh.positions().isEmpty()),
                    "actual villager material batches contain no native geometry");
            var missing = textures.stream().filter(texture -> client.getResourceManager().getResource(texture).isEmpty()).toList();
            var base = renderer.getTextureLocation(villager);
            var hat = renderer.getModel().root().getChild("head").getChild("hat");
            villagerMaterials.add(Map.of("role", role, "renderer", renderer.getClass().getName(),
                    "base_texture", base.toString(), "actual_render_type_textures", textures.stream().map(Object::toString).toList(),
                    "missing_resources", missing.stream().map(Object::toString).toList(),
                    "native_vertices", meshes.stream().map(mesh -> mesh.positions().size()).toList(),
                    "hat_visible_after_render", hat.visible, "hat_rim_visible_after_render", hat.getChild("hat_rim").visible));
            LOG.info("Native {} villager materials: base={}, batches={}, missing={}", role, base, textures, missing);
            return new VillagerMaterials(base, textures, missing);
        }
    }

    private static ResourceLocation vanilla(String path) { return ResourceLocation.withDefaultNamespace(path); }

    private static EntityEldritchWarden warden(Minecraft client) {
        var warden = java.util.Objects.requireNonNull(ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get().create(client.level));
        warden.yBodyRot = warden.yBodyRotO = 0;
        warden.setYHeadRot(0); warden.yHeadRotO = 0;
        warden.setXRot(0); warden.xRotO = 0;
        return warden;
    }

    private static List<NativeMesh> renderWarden(RenderEldritchWarden renderer, EntityEldritchWarden warden, float partial) {
        try (var batches = new NativeBatches()) {
            renderer.render(warden, warden.yBodyRot, partial, new PoseStack(), batches, LightTexture.FULL_BRIGHT);
            return batches.snapshot();
        }
    }

    private static ModelPart wardenRoot(RenderEldritchWarden renderer) throws ReflectiveOperationException {
        var root = RenderEldritchWarden.class.getDeclaredField("root");
        root.setAccessible(true);
        return (ModelPart) root.get(renderer);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void chicken(Minecraft client, float oldFlap, float flap, float oldSpeed, float speed, int age) throws Exception {
        EntityTaintChicken chicken = java.util.Objects.requireNonNull(ThaumcraftTaintAnimals.TAINT_CHICKEN.get().create(client.level));
        chicken.oFlap = oldFlap; chicken.flap = flap;
        chicken.oFlapSpeed = oldSpeed; chicken.flapSpeed = speed;
        chicken.tickCount = age;
        var renderer = (LivingEntityRenderer) client.getEntityRenderDispatcher().getRenderer(chicken);
        try (var batches = new NativeBatches()) {
            renderer.render(chicken, 0, .5F, new PoseStack(), batches, LightTexture.FULL_BRIGHT);
            require(!batches.snapshot().isEmpty(), "actual chicken renderer emitted no geometry");
        }
        var wing = renderer.getModel().getClass().getDeclaredField("rightWing");
        wing.setAccessible(true);
        float expected = (Mth.sin(Mth.lerp(.5F, oldFlap, flap)) + 1) * Mth.lerp(.5F, oldSpeed, speed);
        close(expected, ((ModelPart) wing.get(renderer.getModel())).zRot, "native wing roll follows flap rather than age " + age);
    }

    private static int originalFaces(Minecraft client, String asset, String group) throws Exception {
        int count = 0;
        String current = "";
        try (var reader = client.getResourceManager().getResourceOrThrow(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + asset + ".obj")).openAsReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                var words = line.strip().split("\\s+");
                if (words[0].equals("g")) current = words[1];
                if (words[0].equals("f") && current.equals(group)) {
                    require(words.length == 4, "retained oracle asset is not triangle-only: " + asset);
                    count++;
                }
            }
        }
        require(count > 0, "retained original group is missing: " + asset + '/' + group);
        return count;
    }

    private static void requireTriangles(NativeMesh mesh, int expected) {
        int primitive = mesh.mode() == VertexFormat.Mode.TRIANGLES ? 3 : mesh.mode() == VertexFormat.Mode.QUADS ? 4 : 0;
        require(primitive > 0, "unexpected native mode " + mesh.mode());
        require(mesh.positions().size() % primitive == 0, "native " + mesh.mode() + " batch has an incomplete primitive: " + mesh.positions().size());
        int triangles = 0;
        for (int i = 0; i < mesh.positions().size(); i += primitive) {
            if (area(mesh.positions().get(i), mesh.positions().get(i + 1), mesh.positions().get(i + 2)) > 1.0E-12F) triangles++;
            if (primitive == 4 && area(mesh.positions().get(i + 2), mesh.positions().get(i + 3), mesh.positions().get(i)) > 1.0E-12F) triangles++;
        }
        require(triangles == expected, "native primitive assembly drew " + triangles + " nondegenerate triangles; retained original has " + expected
                + " (mode=" + mesh.mode() + ", vertices=" + mesh.positions().size() + ", indices=" + mesh.indexCount() + ')');
    }

    private static float area(Vector3f a, Vector3f b, Vector3f c) {
        return new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).lengthSquared();
    }

    private record NativeMesh(VertexFormat.Mode mode, int indexCount, List<Vector3f> positions) {}

    /** Owns real native buffers; it never draws or installs an alternate element validator. */
    private static final class NativeBatches implements MultiBufferSource, AutoCloseable {
        private final Map<RenderType, ByteBufferBuilder> storage = new LinkedHashMap<>();
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        private final Consumer<RenderType> emissionObserver;
        NativeBatches() { this(null); }
        NativeBatches(Consumer<RenderType> emissionObserver) { this.emissionObserver = emissionObserver; }
        @Override public VertexConsumer getBuffer(RenderType type) {
            return builders.computeIfAbsent(type, key -> {
                var bytes = new ByteBufferBuilder(Math.max(65536, key.bufferSize()));
                storage.put(key, bytes);
                return emissionObserver == null ? new BufferBuilder(bytes, key.mode(), key.format())
                        : new ObservedBufferBuilder(bytes, key.mode(), key.format(), () -> emissionObserver.accept(key));
            });
        }
        /** Read the actual texture state supplied by each native renderer batch, rather than reconstructing paths. */
        List<ResourceLocation> textures() throws ReflectiveOperationException {
            var textures = new ArrayList<ResourceLocation>();
            for (var type : builders.keySet()) textures.add(texture(type));
            return List.copyOf(textures);
        }
        private static ResourceLocation texture(RenderType type) throws ReflectiveOperationException {
            var stateField = type.getClass().getDeclaredField("state");
            stateField.setAccessible(true);
            var state = stateField.get(type);
            var textureField = state.getClass().getDeclaredField("textureState");
            textureField.setAccessible(true);
            var cutout = RenderStateShard.EmptyTextureStateShard.class.getDeclaredMethod("cutoutTexture");
            cutout.setAccessible(true);
            @SuppressWarnings("unchecked") var texture = (Optional<ResourceLocation>) cutout.invoke(textureField.get(state));
            require(texture.isPresent(), "actual villager render batch has no texture: " + type);
            return texture.orElseThrow();
        }
        List<NativeMesh> snapshot() {
            var result = new ArrayList<NativeMesh>();
            for (var builder : builders.values()) {
                try (MeshData mesh = builder.buildOrThrow()) {
                    var data = mesh.vertexBuffer().order(ByteOrder.nativeOrder());
                    var state = mesh.drawState();
                    int stride = state.format().getVertexSize(), offset = state.format().getOffset(VertexFormatElement.POSITION);
                    var positions = new ArrayList<Vector3f>();
                    for (int i = 0; i < state.vertexCount(); i++) {
                        int at = i * stride + offset;
                        positions.add(new Vector3f(data.getFloat(at), data.getFloat(at + 4), data.getFloat(at + 8)));
                    }
                    result.add(new NativeMesh(state.mode(), state.indexCount(), List.copyOf(positions)));
                }
            }
            builders.clear();
            return List.copyOf(result);
        }
        @Override public void close() { storage.values().forEach(ByteBufferBuilder::close); storage.clear(); builders.clear(); }
    }

    /** Observes the native ModelPart.Cube packed-vertex call, then writes through the actual BufferBuilder implementation. */
    private static final class ObservedBufferBuilder extends BufferBuilder {
        private final Runnable observer;
        ObservedBufferBuilder(ByteBufferBuilder bytes, VertexFormat.Mode mode, VertexFormat format, Runnable observer) {
            super(bytes, mode, format);
            this.observer = observer;
        }
        @Override public void addVertex(float x, float y, float z, int color, float u, float v,
                int overlay, int light, float normalX, float normalY, float normalZ) {
            observer.run();
            super.addVertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }

    private interface Checked { void run() throws Exception; }
    private void check(String name, Checked test) {
        try {
            test.run(); checks.add(Map.of("check", name, "passed", true));
        } catch (Throwable failure) {
            checks.add(Map.of("check", name, "passed", false, "detail", failure.toString()));
            failures.add(name + ": " + failure);
            LOG.error("Renderer regression failed: {}", name, failure);
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void close(float expected, float actual, String message) {
        require(Float.isFinite(actual) && Math.abs(expected - actual) <= EPSILON, message + ": expected=" + expected + ", actual=" + actual);
    }

    private void finish(Minecraft client) {
        if (stopped) return;
        stopped = true;
        try {
            var receipt = Map.of("run_id", runId, "status", failures.isEmpty() ? "PASS" : "FAIL",
                    "fixture", "fresh isolated client world; actual registered entities/renderers, ModelPart and native BufferBuilder/vertex formats; supplied server-world blocks and frozen mob fixtures, not survival acquisition",
                    "checks", checks, "failures", failures, "captures", captures, "villager_materials", villagerMaterials,
                    "villager_geometry", villagerGeometry,
                    "visual_status", "screenshots require separate inspection; native buffer checks do not establish visual acceptance");
            Files.writeString(directory.resolve("renderer-review.json"), new GsonBuilder().setPrettyPrinting().create().toJson(receipt) + "\n");
            LOG.info("Renderer review {}: {} checks, {} failures", failures.isEmpty() ? "PASS" : "FAIL", checks.size(), failures.size());
        } catch (Exception failure) { LOG.error("Cannot write renderer review receipt", failure); }
        client.stop();
    }
}
