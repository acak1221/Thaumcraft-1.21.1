package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.ThaumcraftGogglesClient;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.tiles.TileCrystal;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Real synchronized ritual demand, native crosshair goggles and daylight crystal terrain. Supplied fixtures, not survival. */
public final class MatrixCrystalClientProbe implements ClientModInitializer {
    private static final BlockPos CENTER = new BlockPos(0, 101, 0), MATRIX = CENTER.above(2), JAR = CENTER.offset(-4, 0, 0);
    private final String runId = "matrix-crystal-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private CompletableFuture<Map<String, Integer>> setup;
    private CompletableFuture<?> operation;
    private Map<String, Integer> expected;
    private String paidTag, pendingCapture;
    private Path directory;
    private int stage, ticks, total;
    private boolean created, stopped, dispatched;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.matrixCrystalClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-matrix-crystal-client"), "isolated client directory");
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
            client.options.pauseOnLostFocus = false;
            if (++total > 3600 || ++ticks > 1200) throw new IllegalStateException("Client scene timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Matrix goggles and crystal supplied fixtures", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(73032026L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || pendingCapture != null) return;
            if (stage < 4) aim(client, Vec3.atCenterOf(MATRIX));
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    setup = client.getSingleplayerServer().submit(() -> setup(client));
                    advance(1);
                }
                case 1 -> {
                    if (!setup.isDone() || ticks < 35) return;
                    expected = setup.join();
                    if (!readyMatrix(client)) return;
                    checkDemand(client, expected, "initial remaining demand");
                    pendingCapture = "matrix-needed-aspects";
                }
                case 2 -> {
                    paidTag = expected.containsKey("aer") ? "aer" : expected.keySet().iterator().next();
                    operation = client.getSingleplayerServer().submit(() -> {
                        var actor = actor(client); var level = actor.serverLevel();
                        level.setBlock(JAR, ThaumcraftContent.JAR.get().defaultBlockState(), 3);
                        TileJar jar = (TileJar) level.getBlockEntity(JAR);
                        require(jar.addToContainer(Aspect.getAspect(paidTag), 1) == 0 && jar.getAmount() == 1,
                                "one physical supplied source unit, no direct matrix debt edit");
                    });
                    advance(3);
                }
                case 3 -> {
                    if (!operation.isDone() || ticks < 35 || !readyMatrix(client)) return;
                    operation.join();
                    var matrix = (TileInfusionMatrix) client.level.getBlockEntity(MATRIX);
                    if (matrix.getDemand().getAmount(Aspect.getAspect(paidTag)) != expected.get(paidTag) - 1) return;
                    var remaining = new LinkedHashMap<>(expected); remaining.compute(paidTag, (tag, amount) -> amount - 1);
                    checkDemand(client, remaining, "remaining demand after one genuine source debit");
                    operation = client.getSingleplayerServer().submit(() -> {
                        var level = actor(client).serverLevel();
                        require(((TileJar) level.getBlockEntity(JAR)).getAmount() == 0, "paid source is empty");
                        require(((TileInfusionMatrix) level.getBlockEntity(MATRIX)).getRecipeCost().getAmount(Aspect.getAspect(paidTag)) == expected.get(paidTag),
                                "recipe total is unchanged while live remaining demand decreases");
                    });
                    pendingCapture = "matrix-after-one-essentia";
                }
                case 4 -> {
                    operation.join();
                    operation = client.getSingleplayerServer().submit(() -> actor(client).teleportTo(16.5, 103, 8.5));
                    advance(5);
                }
                case 5 -> {
                    if (!operation.isDone() || ticks < 35 || client.player.getX() < 15) return;
                    operation.join(); aim(client, new Vec3(16.5, 101.25, .5));
                    if (ticks < 50) return;
                    for (int i = 0; i < 7; i++) {
                        BlockPos pos = new BlockPos(10 + i * 2, 101, 0); var state = client.level.getBlockState(pos);
                        require(state.getShadeBrightness(client.level, pos) == 1 && state.getLightBlock(client.level, pos) == 0
                                        && state.propagatesSkylightDown(client.level, pos) && state.getOcclusionShape(client.level, pos).isEmpty(),
                                "cluster " + i + " transmits sky without cube AO");
                        require(state.getCollisionShape(client.level, pos).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                                && state.getLightEmission() == 7, "original collision and emitted light remain intact");
                    }
                    checks.add(Map.of("check", "all seven native crystal lighting states", "passed", true));
                    pendingCapture = "crystal-clusters-daylight";
                }
                default -> finish(client, null);
            }
        } catch (Throwable error) { finish(client, error); }
    }

    private Map<String, Integer> setup(Minecraft client) {
        ServerPlayer player = actor(client); var level = player.serverLevel();
        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        for (BlockPos pos : BlockPos.betweenClosed(-9, 100, -9, 25, 100, 9)) level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        var recipe = level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(value -> value.research().equals("ROD_silverwood")).findFirst().orElseThrow();
        List<BlockPos> pedestals = new ArrayList<>();
        for (int x : new int[]{-3, 0, 3}) for (int z : new int[]{-3, 0, 3}) if (x != 0 || z != 0) pedestals.add(CENTER.offset(x, 0, z));
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
            BlockPos pos = CENTER.offset(x, 0, z);
            if ((Math.abs(x) > 1 || Math.abs(z) > 1) && !pedestals.contains(pos)) level.setBlock(pos, Blocks.SKELETON_SKULL.defaultBlockState(), 3);
        }
        level.setBlock(CENTER, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState(), 3);
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            BlockPos pos = CENTER.offset(x, 0, z);
            level.setBlock(pos, ThaumcraftInfusion.PILLAR.get().defaultBlockState(), 3);
            level.setBlock(pos.above(), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER), 3);
        }
        for (int i = 0; i < pedestals.size(); i++) {
            level.setBlock(pedestals.get(i), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState(), 3);
            if (i < recipe.components().size()) ((TilePedestal) level.getBlockEntity(pedestals.get(i))).setItem(0, recipe.components().get(i).getItems()[0].copy());
        }
        ((TilePedestal) level.getBlockEntity(CENTER)).setItem(0, recipe.input().getItems()[0].copy());
        level.setBlock(MATRIX, ThaumcraftInfusion.MATRIX.get().defaultBlockState(), 3);
        player.getInventory().clearContent(); player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumcraftWearables.GOGGLES.get()));
        player.teleportTo(.5, 101, 4.5); player.inventoryMenu.sendAllDataToRemote();
        var knowledge = KnowledgeAccess.get(player); knowledge.reset(); knowledge.addDiscoveredPrimalAspects();
        knowledge.addResearch("INFUSION"); knowledge.addResearch(recipe.research()); KnowledgeEvents.sync(player);
        TileInfusionMatrix matrix = (TileInfusionMatrix) level.getBlockEntity(MATRIX);
        matrix.setActive(true); require(matrix.startCrafting(player), "registered Silverwood ritual starts against physical inputs");
        for (int i = 0; i < 7; i++) {
            BlockPos pos = new BlockPos(10 + i * 2, 101, 0);
            level.setBlock(pos, ThaumcraftCrystalClusters.blocks().get(i).defaultBlockState(), 3);
            ((TileCrystal) level.getBlockEntity(pos)).orientation = 1;
        }
        return Map.copyOf(recipe.aspects());
    }

    private boolean readyMatrix(Minecraft client) {
        return client.screen == null && client.player.getItemBySlot(EquipmentSlot.HEAD).is(ThaumcraftWearables.GOGGLES.get())
                && client.level.getBlockEntity(MATRIX) instanceof TileInfusionMatrix matrix && matrix.isCrafting()
                && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(MATRIX);
    }
    private void checkDemand(Minecraft client, Map<String, Integer> expected, String label) {
        var view = ThaumcraftGogglesClient.targetView(client);
        require(view != null && view.position().equals(MATRIX), "normal crosshair exposes matrix demand with goggles");
        Map<String, Integer> shown = new LinkedHashMap<>();
        view.entries().forEach(entry -> {
            shown.put(entry.tag(), entry.amount());
            require(entry.known() == KnowledgeAccess.get(client.player).hasDiscoveredAspect(entry.tag()), "normal known/unknown aspect visibility");
        });
        require(shown.equals(expected), label + ": " + shown + " expected " + expected);
        checks.add(Map.of("check", label, "passed", true, "shown", shown));
    }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
    }
    private void advance(int next) { stage = next; ticks = 0; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void finish(Minecraft client, Throwable error) {
        if (stopped) return;
        stopped = true;
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", error == null ? "PASS" : "FAILED"); report.put("run_id", runId);
            report.put("fixture", "fresh1.21.1 flat world, supplied Silverwood ritual ingredients, skull stabilisers and one real jar unit; no survival acquisition claim");
            report.put("checks", checks); report.put("captures", captures); report.put("stage", stage);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("matrix-crystal-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
