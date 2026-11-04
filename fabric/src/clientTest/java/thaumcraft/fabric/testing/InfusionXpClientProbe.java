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
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.utils.ExperiencePoints;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Native wand packets and natural ritual ticks; all starting resources are declared supplied fixtures. */
public final class InfusionXpClientProbe implements ClientModInitializer {
    private static final BlockPos CENTER = new BlockPos(0, 101, 0), MATRIX = CENTER.above(2);
    private static final BlockPos MAGIC = CENTER.offset(-8, 0, 9), WEAPON = CENTER.offset(-7, 0, 9);
    private static final int PRICE = 16;
    private final String runId = "infusion-xp-" + Instant.now().toString().replace(':', '-');
    private final List<Map<String, Object>> checks = new ArrayList<>(), captures = new ArrayList<>();
    private final long lowBefore = ExperiencePoints.atLevel(3) + 5, highBefore = ExperiencePoints.atLevel(30) + 17;
    private CompletableFuture<?> operation;
    private CompletableFuture<State> sample;
    private Path directory;
    private String pendingCapture;
    private int stage, ticks, total, captureFrames, bookDescriptor;
    private boolean created, stopped, dispatched, high;

    private record State(long points, int level, int total, float progress, boolean active, boolean crafting,
                         int debt, int sharpness, int damage, int magic, int weapon, boolean componentsEmpty, int instability) {
        Map<String, Object> report() {
            var value = new LinkedHashMap<String, Object>();
            value.put("points", points); value.put("level", level); value.put("total", total); value.put("progress", progress);
            value.put("active", active); value.put("crafting", crafting); value.put("remaining_xp_points", debt);
            value.put("sharpness", sharpness); value.put("damage", damage); value.put("praecantatio", magic);
            value.put("telum", weapon); value.put("components_empty", componentsEmpty); value.put("instability", instability);
            return value;
        }
    }

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.infusionXpClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        require(directory.getFileName().toString().equals("run-infusion-xp-client"), "isolated infusion client directory");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
            if (pendingCapture == null || dispatched || stopped || !(current instanceof GuiResearchRecipe)) return;
            try {
                // The real renderer uses this wall-clock cycle for the enchantment and its price.
                if (Math.floorMod(Util.getMillis() / 1000L, 5) != 1 || !visibleBookDescriptor(current)) { captureFrames = 0; return; }
                if (++captureFrames >= 3) capture(client, graphics, Map.of("target_sharpness", 2, "price_points", PRICE,
                        "translated_price", Component.translatable("tc.port.book_recipe.xp_points", PRICE).getString()));
            } catch (Throwable error) { finish(client, error); }
        }));
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            var client = Minecraft.getInstance();
            if (pendingCapture != null && !dispatched && !stopped && client.screen == null && client.getOverlay() == null
                    && ++captureFrames >= 3) capture(client, graphics, Map.of("client_points", ExperiencePoints.available(client.player),
                    "client_level", client.player.experienceLevel, "client_progress", client.player.experienceProgress));
        });
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 5000 || ++ticks > 1200) throw new IllegalStateException("Infusion XP client timeout at stage " + stage);
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    if (!client.getLanguageManager().getSelected().equals("ru_ru")) { reload(client, "ru_ru", stage); return; }
                    created = true; client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.options.renderDistance().set(2); client.options.simulationDistance().set(5);
                    client.createWorldOpenFlows().createFreshLevel(runId,
                            new LevelSettings("Infusion XP supplied fixtures", GameType.SURVIVAL, false, Difficulty.PEACEFUL,
                                    true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(73042027L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null || pendingCapture != null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    operation = client.getSingleplayerServer().submit(() -> setup(client)); advance(1);
                }
                case 1 -> {
                    if (!completed() || ticks < 30 || !clientPoints(client, lowBefore)
                            || !KnowledgeAccess.get(client.player).isResearchComplete("INFUSIONENCHANTMENT")
                            || !(client.level.getBlockEntity(MATRIX) instanceof TileInfusionMatrix)) return;
                    require(client.player.experienceLevel == 3, "low payer has actual native level3 plus5 points");
                    openBook(client); requestCapture("book-ru-sharpness-II-cost16-points");
                }
                case 2 -> reload(client, "en_us", 3);
                case 3 -> {
                    if (!completed() || ticks < 5) return;
                    openBook(client); requestCapture("book-en-sharpness-II-cost16-points");
                }
                case 4 -> { client.setScreen(null); reload(client, "ru_ru", 5); }
                case 5 -> {
                    if (!completed() || ticks < 10) return;
                    useWand(client); advance(6);
                }
                case 6 -> {
                    if (ticks < 5 || !clientTile(client).isActive()) return;
                    sample = client.getSingleplayerServer().submit(() -> snapshot(client)); advance(7);
                }
                case 7 -> {
                    if (!sample.isDone()) return;
                    State activated = sample.join(); requireGauge(client, activated);
                    require(activated.active() && !activated.crafting() && activated.points() == lowBefore
                                    && activated.magic() == 8 && activated.weapon() == 16 && !activated.componentsEmpty(),
                            "native wand activation supplies no payment or reward");
                    checks.add(Map.of("check", "actual native wand packet activates supplied altar without payment", "passed", true, "server", activated.report()));
                    useWand(client); advance(8);
                }
                case 8 -> {
                    if (!clientPoints(client, before() - PRICE) || !clientTile(client).isCrafting() || clientTile(client).getRemainingXPPoints() != 0) return;
                    sample = client.getSingleplayerServer().submit(() -> verifyPaid(client)); advance(9);
                }
                case 9 -> {
                    if (!sample.isDone()) return;
                    State paid = sample.join(); requireGauge(client, paid);
                    checks.add(Map.of("check", "actual wand-started ritual pays exactly16 fixed points at " + (high ? "high" : "low") + " level",
                            "passed", true, "before_points", before(), "after_server", paid.report(),
                            "after_client_points", ExperiencePoints.available(client.player)));
                    aim(client, Vec3.atCenterOf(MATRIX)); requestCapture((high ? "level30" : "level3") + "-paid16-native-level-and-bar");
                }
                case 10 -> {
                    if (ticks < 10) return;
                    sample = client.getSingleplayerServer().submit(() -> snapshot(client)); advance(11);
                }
                case 11 -> {
                    if (!sample.isDone()) return;
                    State result = sample.join();
                    if (result.crafting()) { advance(10); return; }
                    verifyCompleted(result); requireGauge(client, result);
                    if (clientSharpness(client) != 2) return;
                    checks.add(Map.of("check", "natural " + (high ? "high" : "low") + " payer ritual completes with exact finite resources", "passed", true, "server", result.report()));
                    aim(client, Vec3.atCenterOf(CENTER)); requestCapture((high ? "level30" : "level3") + "-completed-sharpness-II-finite-resources");
                }
                case 12 -> {
                    if (!high) {
                        high = true;
                        operation = client.getSingleplayerServer().submit(() -> {
                            var player = actor(client);
                            player.getInventory().setItem(2, pedestal(client, CENTER).getItem(0).copy());
                            fixtureRitual(client, Math.toIntExact(highBefore));
                            player.inventoryMenu.sendAllDataToRemote();
                        }); advance(13);
                    } else {
                        if (ticks < 40) return;
                        sample = client.getSingleplayerServer().submit(() -> snapshot(client)); advance(14);
                    }
                }
                case 13 -> {
                    if (!completed() || ticks < 25 || !clientPoints(client, highBefore) || clientSharpness(client) != 1) return;
                    require(client.player.experienceLevel == 30, "high payer has actual native level30 plus17 points");
                    useWand(client); advance(8);
                }
                case 14 -> {
                    if (!sample.isDone()) return;
                    State stable = sample.join(); verifyCompleted(stable); requireGauge(client, stable);
                    require(client.player.getInventory().getItem(2).getEnchantments().getLevel(client.level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)) == 2,
                            "first genuinely completed reward is retained during the second supplied ritual");
                    checks.add(Map.of("check", "completed ritual remains stable40 ticks with no repeated reward or debit", "passed", true, "server", stable.report()));
                    finish(client, null);
                }
                default -> throw new IllegalStateException("Unknown infusion XP stage " + stage);
            }
        } catch (Throwable failure) { finish(client, failure); }
    }

    private void setup(Minecraft client) {
        var player = actor(client); var level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL); level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        for (BlockPos pos : BlockPos.betweenClosed(-12, 100, -12, 12, 100, 12)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            level.setBlock(CENTER.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState(), 3);
        level.setBlock(CENTER, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState(), 3);
        level.setBlock(MATRIX, ThaumcraftInfusion.MATRIX.get().defaultBlockState(), 3);
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            level.setBlock(CENTER.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER), 3);
            level.setBlock(CENTER.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER), 3);
        }
        level.setBlock(CENTER.west(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState(), 3);
        level.setBlock(CENTER.east(3), ThaumcraftInfusion.PEDESTAL.get().defaultBlockState(), 3);
        level.setBlock(MAGIC, ThaumcraftContent.JAR.get().defaultBlockState(), 3);
        level.setBlock(WEAPON, ThaumcraftContent.JAR.get().defaultBlockState(), 3);
        player.getInventory().clearContent(); player.getInventory().setItem(0, new ItemStack(ThaumcraftContent.WAND.get()));
        player.getInventory().selected = 0; player.teleportTo(.5, 101, 3.5);
        var knowledge = KnowledgeAccess.get(player); knowledge.reset(); knowledge.addDiscoveredPrimalAspects();
        knowledge.addResearch("INFUSION"); knowledge.addResearch("INFUSIONENCHANTMENT"); KnowledgeEvents.sync(player);
        fixtureRitual(client, Math.toIntExact(lowBefore)); player.inventoryMenu.sendAllDataToRemote();
    }

    private void fixtureRitual(Minecraft client, int points) {
        var player = actor(client); var level = player.serverLevel();
        require(!serverTile(client).isCrafting(), "new supplied fixture is admitted only after preceding ritual completion");
        ItemStack input = new ItemStack(Items.IRON_SWORD);
        input.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 1);
        input.setDamageValue(7);
        InfusionEnchantmentRecipe recipe = recipe(client);
        require(recipe.xpPoints(input, level.registryAccess()) == PRICE && recipe.aspectCost(input, level.registryAccess()).getAmount(Aspect.MAGIC) == 8
                        && recipe.aspectCost(input, level.registryAccess()).getAmount(Aspect.WEAPON) == 16,
                "native Sharpness I toII recipe quotes16 XP points and finite8/16 essentia");
        pedestal(client, CENTER).setItem(0, input); pedestal(client, CENTER.west(3)).setItem(0, new ItemStack(Items.IRON_SWORD));
        pedestal(client, CENTER.east(3)).setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus"))));
        require(jar(client, MAGIC).getAmount() == 0 && jar(client, WEAPON).getAmount() == 0
                        && jar(client, MAGIC).addToContainer(Aspect.MAGIC, 8) == 0 && jar(client, WEAPON).addToContainer(Aspect.WEAPON, 16) == 0,
                "declared finite sources contain exactly this ritual's essentia");
        // Fixture baseline only. Every tested debit uses natural live matrix cycles and native XP packets.
        player.experienceLevel = 0; player.experienceProgress = 0; player.totalExperience = 0;
        player.giveExperiencePoints(points);
        require(ExperiencePoints.available(player) == points && player.totalExperience == points, "native grant sets the declared fixture XP baseline");
    }

    private void openBook(Minecraft client) throws ReflectiveOperationException {
        var research = ResearchCatalog.get("INFUSIONENCHANTMENT");
        bookDescriptor = -1;
        for (int i = 0; i < research.pages().size(); i++) if ("InfEnch8".equals(research.pages().get(i).recipeKey())) bookDescriptor = i;
        require(bookDescriptor >= 0, "canonical book contains Sharpness recipe descriptor");
        var screen = new GuiResearchRecipe(null, research);
        Field requested = GuiResearchRecipe.class.getDeclaredField("requestedDescriptor"); requested.setAccessible(true); requested.setInt(screen, bookDescriptor);
        client.setScreen(screen);
        // Keep pointer away from item tooltips; focus and wiggle produce a real MouseHandler update.
        GLFW.glfwFocusWindow(client.getWindow().getWindow());
        GLFW.glfwSetCursorPos(client.getWindow().getWindow(), 8, 8); GLFW.glfwSetCursorPos(client.getWindow().getWindow(), 10, 8);
    }

    private boolean visibleBookDescriptor(Object screen) throws ReflectiveOperationException {
        int page = (int) field(screen, "page"); var pages = (List<?>) field(screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) if ((int) field(pages.get(i), "descriptor") == bookDescriptor) {
            var client = Minecraft.getInstance(); Object value = field(pages.get(i), "recipe");
            require(value instanceof ThaumcraftRecipeDisplay, "actual canonical book page resolves native recipe display");
            var display = (ThaumcraftRecipeDisplay) value;
            require(display.recipe() == client.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion_enchantment/sharpness")).orElseThrow(),
                    "actual book page retains the synced native Sharpness recipe holder");
            require(!Component.translatable("tc.port.book_recipe.xp_points", PRICE).getString().contains("tc.port."), "point price translation is loaded");
            return true;
        }
        return false;
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static InfusionEnchantmentRecipe recipe(Minecraft client) {
        return actor(client).serverLevel().getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(recipe -> recipe.enchantment().equals(ResourceLocation.withDefaultNamespace("sharpness"))).findFirst().orElseThrow();
    }
    private static State snapshot(Minecraft client) {
        var player = actor(client); var tile = serverTile(client); ItemStack result = pedestal(client, CENTER).getItem(0);
        return new State(ExperiencePoints.available(player), player.experienceLevel, player.totalExperience, player.experienceProgress,
                tile.isActive(), tile.isCrafting(), tile.getRemainingXPPoints(), result.getEnchantments().getLevel(player.serverLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)), result.getDamageValue(),
                jar(client, MAGIC).getAmount(), jar(client, WEAPON).getAmount(), pedestal(client, CENTER.west(3)).isEmpty()
                && pedestal(client, CENTER.east(3)).isEmpty(), tile.getInstability());
    }
    private State verifyPaid(Minecraft client) {
        State state = snapshot(client);
        require(state.points() == before() - PRICE && state.total() == before() - PRICE && state.debt() == 0
                        && state.crafting() && state.sharpness() == 1 && state.instability() < 0,
                "real ritual has paid exactly16 native XP points before its resulting enchantment");
        return state;
    }
    private void verifyCompleted(State state) {
        require(state.points() == before() - PRICE && state.total() == before() - PRICE && !state.crafting() && state.debt() == 0
                        && state.sharpness() == 2 && state.damage() == 7 && state.magic() == 0 && state.weapon() == 0 && state.componentsEmpty(),
                "natural ritual consumes16 XP points, all24 finite essentia units and both actual components for one preserved Sharpness II result");
    }
    private static void requireGauge(Minecraft client, State state) {
        require(client.player.experienceLevel == state.level() && client.player.totalExperience == state.total()
                        && Float.compare(client.player.experienceProgress, state.progress()) == 0 && ExperiencePoints.available(client.player) == state.points(),
                "native XP packets synchronize actual client level/progress/total with authoritative server points");
    }
    private static void useWand(Minecraft client) {
        require(client.screen == null && client.player.getMainHandItem().is(ThaumcraftContent.WAND.get()), "actual client survival actor holds native wand");
        Vec3 point = Vec3.atCenterOf(MATRIX).add(0, 0, .5);
        aim(client, point);
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(point, Direction.SOUTH, MATRIX, false)).consumesAction(), "native wand block-use sends real interaction packet");
        client.player.swing(InteractionHand.MAIN_HAND);
    }
    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
    }
    private long before() { return high ? highBefore : lowBefore; }
    private static boolean clientPoints(Minecraft client, long points) { return ExperiencePoints.available(client.player) == points; }
    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static TileInfusionMatrix serverTile(Minecraft client) { return (TileInfusionMatrix) actor(client).serverLevel().getBlockEntity(MATRIX); }
    private static TileInfusionMatrix clientTile(Minecraft client) { return (TileInfusionMatrix) client.level.getBlockEntity(MATRIX); }
    private static TilePedestal pedestal(Minecraft client, BlockPos pos) { return (TilePedestal) actor(client).serverLevel().getBlockEntity(pos); }
    private static TileJar jar(Minecraft client, BlockPos pos) { return (TileJar) actor(client).serverLevel().getBlockEntity(pos); }
    private static int clientSharpness(Minecraft client) {
        return ((TilePedestal) client.level.getBlockEntity(CENTER)).getItem(0).getEnchantments().getLevel(client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS));
    }
    private boolean completed() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private void reload(Minecraft client, String language, int next) {
        client.getLanguageManager().setSelected(language); client.options.languageCode = language; operation = client.reloadResourcePacks(); advance(next);
    }
    private void requestCapture(String label) { pendingCapture = label; captureFrames = 0; }
    private void capture(Minecraft client, GuiGraphics graphics, Map<String, Object> extra) {
        dispatched = true; graphics.flush(); String label = pendingCapture, file = runId + '-' + label + ".png";
        Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), ignored -> client.execute(() -> {
            try {
                Path path = directory.resolve("screenshots").resolve(file);
                require(Files.isRegularFile(path) && Files.size(path) > 1000, "actual native infusion screenshot exists");
                var receipt = new LinkedHashMap<String, Object>(extra);
                receipt.put("label", label); receipt.put("file", path.toString()); receipt.put("bytes", Files.size(path));
                receipt.put("language", client.getLanguageManager().getSelected()); captures.add(receipt);
                pendingCapture = null; dispatched = false; captureFrames = 0; advance(stage + 1);
            } catch (Throwable error) { finish(client, error); }
        }));
    }
    private void advance(int next) { System.out.println("Infusion XP client stage " + stage + " -> " + next); stage = next; ticks = 0; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void finish(Minecraft client, Throwable error) {
        if (stopped) return; stopped = true;
        try {
            var report = new LinkedHashMap<String, Object>();
            report.put("status", error == null ? "PASS" : "FAILED"); report.put("run_id", runId); report.put("stage", stage);
            report.put("fixture", "Fresh 1.21.1 flat world; supplied complete altar with formed pillars, symmetric skull stabilizers, INFUSION and INFUSIONENCHANTMENT research, native empty wood/iron wand, two Sharpness I iron swords with damage7, two iron sword/salis component sets, exactly8 Praecantatio and16 Telum per ritual, and server XP baselines level3+5points and level30+17points through native giveExperiencePoints after explicit fixture reset. Survival actor. All tested activation/start actions use real client wand useItemOn packets; XP, essentia, component payment and result use natural server ticks. Actual RU/EN canonical research book captures. No survival acquisition, pillar formation or old-world migration claim.");
            report.put("price_points", PRICE); report.put("low_before_points", lowBefore); report.put("high_before_points", highBefore);
            report.put("checks", checks); report.put("captures", captures);
            if (error != null) { report.put("error", error.toString()); error.printStackTrace(); }
            Files.writeString(directory.resolve("infusion-xp-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { failure.printStackTrace(); }
        client.stop();
    }
}
