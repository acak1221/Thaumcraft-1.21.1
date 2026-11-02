package thaumcraft.fabric.testing;

import com.google.gson.GsonBuilder;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.RecipeTransferPayload;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Opt-in finite supplied fixture. All tested transfers originate in real rendered GUI handlers. */
public final class RecipeTransferClientProbe implements ClientModInitializer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("thaumcraft-recipe-transfer-probe");
    private static final BlockPos BENCH = new BlockPos(0, 100, 1);
    private static final String[] LOCALES = {"en_us", "ru_ru", "en_us"};
    private final String world = "recipe-transfer-" + Instant.now().toString().replace(':', '-');
    private final AtomicInteger packets = new AtomicInteger();
    private final List<Map<String, Object>> captures = new ArrayList<>(), checks = new ArrayList<>();
    private CompletableFuture<Void> operation;
    private CompletableFuture<Map<String, Object>> verification;
    private ItemStack expectedWand;
    private boolean created, stopped, pending, dispatched;
    private int stage, phase, recipeCase, ticks, total, packetBaseline, nextAfterCapture;
    private int clickX, clickY;
    private String captureName;
    private Path directory;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.recipeTransferClient")) return;
        directory = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        if (!directory.getFileName().toString().equals("run-recipe-transfer-client"))
            throw new IllegalStateException("Recipe transfer probe requires its isolated run directory");
        if (!FabricLoader.getInstance().isModLoaded("emi")) throw new IllegalStateException("Run with -PwithEmi");
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterRender(screen).register(
                (rendered, graphics, mouseX, mouseY, delta) -> {
                    if (!pending || dispatched || stopped || client.screen != rendered || client.getOverlay() != null) return;
                    dispatched = true;
                    try {
                        // Render the actual hover, with the same coordinates used for the next input.
                        if (nextAfterCapture == 6) rendered.render(graphics, clickX, clickY, delta);
                        graphics.flush();
                        String fileName = captureName + ".png";
                        Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), message -> client.execute(() -> {
                            if (stopped) return;
                            try {
                                Path path = directory.resolve("screenshots").resolve(fileName);
                                if (!Files.isRegularFile(path) || Files.size(path) < 1000) throw new IllegalStateException("Missing capture " + path);
                                captures.add(Map.of("path", path.toString(), "language", LOCALES[phase], "phase", phase,
                                        "case", caseName(), "screen", rendered.getClass().getName(), "bytes", Files.size(path)));
                                pending = dispatched = false; advance(nextAfterCapture); write("RUNNING", null);
                            } catch (Throwable error) { fail(client, error); }
                        }));
                    } catch (Throwable error) { fail(client, error); }
                }));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (stopped) return;
        try {
            client.options.pauseOnLostFocus = false;
            if (++total > 7200 || ++ticks > 1800) throw new IllegalStateException("Probe timeout: stage=" + stage + ", phase=" + phase + ", case=" + caseName());
            if (pending) return;
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                if (!created && client.screen instanceof AccessibilityOnboardingScreen onboarding && client.getOverlay() == null) onboarding.onClose();
                if (!created && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                    created = true;
                    client.options.guiScale().set(2); client.options.tutorialStep = TutorialSteps.NONE;
                    client.createWorldOpenFlows().createFreshLevel(world,
                            new LevelSettings("Recipe transfer supplied visual fixture", GameType.CREATIVE, false,
                                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(71338963L, false, false),
                            access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(), client.screen);
                }
                return;
            }
            if (client.getOverlay() != null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 30) return;
                    observe(client);
                    operation = client.getSingleplayerServer().submit(() -> {
                        ServerPlayer actor = actor(client);
                        var level = actor.serverLevel();
                        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 5; z++) level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(BENCH, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState(), 3);
                        actor.teleportTo(.5, 100, 3.5); actor.setGameMode(GameType.SURVIVAL);
                        KnowledgeAccess.get(actor).addResearch("ARCANESTONE"); KnowledgeEvents.sync(actor);
                    });
                    advance(1);
                }
                case 1 -> { if (complete()) startLocale(client); }
                case 2 -> {
                    if (!complete() || !KnowledgeAccess.get(client.player).isResearchComplete("ARCANESTONE")) return;
                    prepare(client);
                }
                case 3 -> {
                    if (!complete() || ticks < 12) return;
                    if (recipeCase == 0) {
                        client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("ARCANESTONE")));
                        advance(4);
                    } else {
                        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(BENCH).add(0, 0, .5), Direction.SOUTH, BENCH, false));
                        advance(4);
                    }
                }
                case 4 -> {
                    if (ticks < 8) return;
                    if (recipeCase == 0) {
                        if (!(client.screen instanceof GuiResearchRecipe)) throw new IllegalStateException("Book did not open");
                        Button button = bookButton(client.screen);
                        if (button == null) {
                            // Text pagination depends on the actual selected language.
                            Button next = client.screen.children().stream().filter(v -> v instanceof Button b && b.active
                                    && b.getMessage().getString().equals("→")).map(v -> (Button) v).findFirst().orElseThrow();
                            next.onPress(); ticks = 0; return;
                        }
                        clickX = button.getX() + button.getWidth() / 2; clickY = button.getY() + button.getHeight() / 2;
                        advance(5);
                    } else {
                        if (!(client.screen instanceof GuiArcaneWorkbench)) return;
                        EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(ResourceLocation.parse(recipeCase == 1
                                ? "thaumcraft:arcane/arcane_stone" : "minecraft:oak_planks"));
                        if (recipe == null) return;
                        EmiApi.displayRecipe(recipe); advance(5);
                    }
                }
                case 5 -> {
                    if (ticks < 10) return;
                    if (recipeCase > 0) {
                        int[] point = emiFillPoint(client.screen);
                        if (point == null) throw new IllegalStateException("EMI did not display its real fill button");
                        clickX = point[0]; clickY = point[1];
                    }
                    capture("before", 6);
                }
                case 6 -> {
                    packetBaseline = packets.get();
                    if (!client.screen.mouseClicked(clickX, clickY, 0)) throw new IllegalStateException("Real fill-button mouse click was refused");
                    advance(7);
                }
                case 7 -> {
                    if (ticks < 15 || !(client.screen instanceof GuiArcaneWorkbench)
                            || !(client.player.containerMenu instanceof ContainerArcaneWorkbench menu) || !matchesClient(menu)) return;
                    verification = client.getSingleplayerServer().submit(() -> verifyServer(client));
                    advance(8);
                }
                case 8 -> {
                    if (!verification.isDone() || ticks < 10) return;
                    if (!(client.screen instanceof GuiArcaneWorkbench screen)
                            || screen.getMenu() != client.player.containerMenu || !matchesClient(screen.getMenu()))
                        throw new IllegalStateException("The rendered workbench must own the verified current menu");
                    Map<String, Object> facts = verification.join();
                    if (packets.get() != packetBaseline + 1) throw new IllegalStateException("GUI must emit exactly one actual C2S packet");
                    var row = new LinkedHashMap<>(facts); row.put("phase", phase); row.put("locale", LOCALES[phase]);
                    row.put("case", caseName()); row.put("actual_c2s_packets", packets.get() - packetBaseline); checks.add(row);
                    capture("filled", 9);
                }
                case 9 -> {
                    client.player.closeContainer(); client.setScreen(null);
                    recipeCase++;
                    if (recipeCase < 3) { prepare(client); return; }
                    recipeCase = 0; phase++;
                    if (phase < LOCALES.length) startLocale(client);
                    else { write("SCRIPT_PASSED", null); stopped = true; client.stop(); }
                }
                default -> throw new IllegalStateException("Unknown stage " + stage);
            }
        } catch (Throwable error) { fail(client, error); }
    }

    private void prepare(Minecraft client) {
        // Setup is deliberately supplied, not presented as survival acquisition. Only GUI inputs move it afterwards.
        operation = client.getSingleplayerServer().submit(() -> {
            ServerPlayer actor = actor(client);
            if (actor.containerMenu != actor.inventoryMenu) actor.closeContainer();
            TileArcaneWorkbench table = (TileArcaneWorkbench) actor.serverLevel().getBlockEntity(BENCH);
            table.clearContent(); actor.getInventory().clearContent();
            expectedWand = new ItemStack(ThaumcraftContent.WAND.get());
            WandData data = WandData.EMPTY;
            for (String primal : WandData.PRIMALS) data = data.withAmount(primal, 1000);
            expectedWand.set(ThaumcraftContent.WAND_DATA.get(), data); table.setItem(10, expectedWand.copy());
            if (recipeCase == 2) actor.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 4));
            else {
                actor.getInventory().setItem(0, new ItemStack(Items.STONE, 16));
                actor.getInventory().setItem(1, new ItemStack(ThaumcraftContent.SHARDS.get("aer").get(), 2));
            }
            actor.getInventory().selected = 8;
            actor.getInventory().setChanged(); actor.inventoryMenu.sendAllDataToRemote();
        });
        advance(3);
    }

    private void startLocale(Minecraft client) {
        client.getLanguageManager().setSelected(LOCALES[phase]); client.options.languageCode = LOCALES[phase];
        operation = client.reloadResourcePacks(); advance(2);
    }

    private boolean complete() { if (operation == null || !operation.isDone()) return false; operation.join(); return true; }
    private void advance(int following) {
        LOGGER.info("Recipe transfer probe: stage {} -> {}, phase {}, case {}", stage, following, phase, caseName());
        stage = following; ticks = 0;
    }
    private String caseName() { return switch (recipeCase) { case 0 -> "book_arcane"; case 1 -> "emi_arcane"; default -> "emi_vanilla"; }; }
    private void capture(String suffix, int following) {
        captureName = "recipe-transfer-" + phase + "-" + LOCALES[phase] + "-" + caseName() + "-" + suffix;
        nextAfterCapture = following; pending = true; dispatched = false;
    }

    private static Button bookButton(Screen screen) {
        return screen.children().stream().filter(v -> v instanceof Button b && b.active && b.visible
                && b.getClass().getSimpleName().equals("TransferButton")).map(v -> (Button) v).findFirst().orElse(null);
    }

    private static int[] emiFillPoint(Screen screen) throws ReflectiveOperationException {
        if (!screen.getClass().getName().equals("dev.emi.emi.screen.RecipeScreen")) return null;
        for (Object group : (List<?>) field(screen, "currentPage")) {
            int x = (Integer) field(group, "x"), y = (Integer) field(group, "y");
            for (Object entry : (List<?>) field(group, "widgets")) {
                if (entry.getClass().getName().equals("dev.emi.emi.api.widget.RecipeFillButtonWidget")) {
                    var bounds = ((Widget) entry).getBounds();
                    return new int[]{x + bounds.x() + bounds.width() / 2, y + bounds.y() + bounds.height() / 2};
                }
            }
        }
        return null;
    }

    private boolean matchesClient(ContainerArcaneWorkbench menu) {
        for (int cell = 0; cell < 9; cell++) if (!expectedCell(menu.getSlot(cell + 2).getItem(), cell)) return false;
        if (!ItemStack.matches(menu.getSlot(1).getItem(), expectedWand) || !menu.craftable()) return false;
        for (String primal : WandData.PRIMALS) {
            int expected = recipeCase < 2 && (primal.equals("terra") || primal.equals("ignis")) ? 110 : 0;
            if (menu.requiredVis(primal) != expected) return false;
        }
        return true;
    }

    private boolean expectedCell(ItemStack stack, int cell) {
        if (recipeCase == 2) return cell == 0 ? stack.is(Items.OAK_LOG) && stack.getCount() == 1 : stack.isEmpty();
        return stack.getCount() == 1 && (cell == 4 ? stack.is(ThaumcraftContent.SHARDS.get("aer").get()) : stack.is(Items.STONE));
    }

    private Map<String, Object> verifyServer(Minecraft client) {
        ServerPlayer actor = actor(client);
        TileArcaneWorkbench table = (TileArcaneWorkbench) actor.serverLevel().getBlockEntity(BENCH);
        if (!(actor.containerMenu instanceof ContainerArcaneWorkbench menu)) throw new IllegalStateException("Server did not open the native workbench");
        for (int cell = 0; cell < 9; cell++) if (!expectedCell(table.getItem(cell), cell)) throw new IllegalStateException("Wrong server matrix cell " + cell);
        if (!menu.craftable()) throw new IllegalStateException("Native server preview is not craftable after transfer");
        var prices = new LinkedHashMap<String, Integer>();
        for (String primal : WandData.PRIMALS) {
            int expected = recipeCase < 2 && (primal.equals("terra") || primal.equals("ignis")) ? 110 : 0;
            if (menu.requiredVis(primal) != expected) throw new IllegalStateException("Wrong server primal price: " + primal);
            prices.put(primal, menu.requiredVis(primal));
        }
        if (!ItemStack.matches(expectedWand, table.getItem(10)) || !table.getItem(9).isEmpty()
                || !actor.containerMenu.getCarried().isEmpty()) throw new IllegalStateException("Transfer changed wand/reserved slot/cursor");
        if (recipeCase == 2) {
            if (actor.getInventory().countItem(Items.OAK_LOG) != 3 || actor.getInventory().countItem(Items.OAK_PLANKS) != 0)
                throw new IllegalStateException("Vanilla transfer did not preserve paid input/output counts");
        } else if (actor.getInventory().countItem(Items.STONE) != 8
                || actor.getInventory().countItem(ThaumcraftContent.SHARDS.get("aer").get()) != 1)
            throw new IllegalStateException("Arcane transfer did not debit exactly eight stone and one shard");
        List<String> matrix = new ArrayList<>();
        for (int cell = 0; cell < 9; cell++) matrix.add(table.getItem(cell).toString());
        return Map.of("matrix", matrix, "primal_prices_centivis", prices, "craftable", true,
                "wand_unchanged", true, "output_not_taken", true,
                "source_inventory_verified", true, "native_menu", actor.containerMenu.containerId);
    }

    private static ServerPlayer actor(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void observe(Minecraft client) throws ReflectiveOperationException {
        Channel channel = (Channel) field(client.getConnection().getConnection(), "channel");
        channel.pipeline().addLast("thaumcraft-recipe-transfer-probe", new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                if (message instanceof ServerboundCustomPayloadPacket packet && packet.payload().type().id().equals(RecipeTransferPayload.TYPE.id())) packets.incrementAndGet();
                context.write(message, promise);
            }
        });
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(object); }
            catch (NoSuchFieldException absent) { /* superclass */ }
        }
        throw new NoSuchFieldException(name);
    }

    private void write(String status, Throwable error) {
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", status); report.put("world", world); report.put("seed", 71338963L);
            report.put("fixture", "fresh flat1.21.1 world; finite supplied ingredients; no survival acquisition claim");
            report.put("stage", stage); report.put("phase", phase); report.put("case", caseName());
            report.put("actual_c2s_packets", packets.get()); report.put("checks", checks); report.put("captures", captures);
            report.put("addon_loaded", FabricLoader.getInstance().isModLoaded("refinedthaumaturgy"));
            if (error != null) report.put("error", error.toString());
            Files.writeString(directory.resolve("recipe-transfer-client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException("Cannot write recipe-transfer client result", failure); }
    }
    private void fail(Minecraft client, Throwable error) {
        if (stopped) return;
        error.printStackTrace(); write("FAILED", error); stopped = true; client.stop();
    }
}
