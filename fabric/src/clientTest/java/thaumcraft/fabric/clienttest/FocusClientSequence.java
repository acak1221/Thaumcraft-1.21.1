package thaumcraft.fabric.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.impl.NetworkAggregator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.client.ThaumcraftFocusClient;
import thaumcraft.client.fx.particles.ResourceParticle;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiFocusSelect;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftPlants;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.FocusSelectionPayload;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Supplied inputs/patch/cow only. Focus output, selection, combat and flower resources are native. */
final class FocusClientSequence {
    private enum Step {
        SETUP, BOOK_OPEN, BOOK_CATEGORY, BOOK_PAN, BOOK_ENTRY, BOOK_RECIPE,
        WORKBENCH_OPEN, FUND, SHARDS_PICKUP, SHARDS_PLACE, QUARTZ_PICKUP, QUARTZ_PLACE, CENTER,
        ASSEMBLY_READY, ASSEMBLY_CAPTURE, CRAFT, CRAFTED, CRAFT_STORED, WAND_RETURN, CRAFT_VERIFY, CRAFT_ACK,
        CANCEL_PRESS, CANCEL_WAIT, CANCEL_ACK, FOCUS_PRESS, RADIAL_WAIT, RADIAL_SELECT, EQUIPPED,
        CAST_POSITION, CAST_AIM, CAST_START, CAST_LIVE, CAST_STOP, CAST_ACK,
        REMOVE_PREPARE, REMOVE_PRESS, REMOVE_WAIT, REMOVE_VERIFY, REMOVE_SCENE,
        GARDEN_PREPARE, BREAK_SHIMMER, PICKUP_SHIMMER, PICKUP_CINDER,
        PLANT_CRAFT_OPEN, PLANT_CRAFT_INSERT, PLANT_CRAFT_GRID, PLANT_CRAFT_READY,
        PLANT_CRAFT_TAKE, PLANT_CRAFT_OUTPUT, PLANT_CRAFT_STORED, PLANTS_VERIFY, PLANTS_POSITION, PLANTS_WORLD,
        NORMAL_SWAP_PRESS, NORMAL_SWAP_ACK, NORMAL_SWAP_RETURN, NORMAL_SWAP_RESTORED, DONE
    }

    private static final int[] CORNERS = {0, 2, 6, 8}, EDGES = {1, 3, 5, 7};
    private static final String WIRE_OBSERVER = "thaumcraft-focus-smoke";
    private static final String TARGET_TAG = "thaumcraft.focus_client_target";
    final Map<String, Object> evidence = new LinkedHashMap<>();
    int returnedFocusSlot = -1;
    private final BlockPos workbench, arena, garden, shimmerHarvest, cinderHarvest;
    private final List<Integer> selectionPackets = new CopyOnWriteArrayList<>();
    private final AtomicInteger offhandSwapPackets = new AtomicInteger(), usePackets = new AtomicInteger(), releasePackets = new AtomicInteger();
    private final AtomicInteger plantBreakPackets = new AtomicInteger();
    private final ClientTickEvent.Client castGuard = this::guardCast;
    private Step step = Step.SETUP;
    private int ticks, bookTurns, matrixIndex, plantRecipe, plantSourceSlot, targetId, castClientTicks;
    private int castUseBefore, castReleaseBefore;
    private boolean isolatedDuplicateKeyDispatch, castActive, releaseSent;
    private CompletableFuture<Void> serverAction;
    private Map<String, Object> serverSnapshot = Map.of();
    private ItemStack craftingWandBefore, selectedServerWand, craftedFocus, offhandBefore;
    private ItemStack ordinaryHandBefore;
    private WandData craftingVisBefore, selectedVis, stoppedVis;
    private CastProbe castProbe;
    private Channel wireChannel;
    private volatile Throwable wireObservationFailure;
    private final Map<String, Integer> flowerSeen = new LinkedHashMap<>();
    private int inactiveInputTicks;

    FocusClientSequence(BlockPos research) {
        workbench = research.offset(88, 0, 8);
        arena = workbench.east(7);
        garden = workbench.south(12);
        shimmerHarvest = garden.offset(-1, 0, 2);
        cinderHarvest = garden.offset(1, 0, 2);
        evidence.put("boundary", "The actual scene39 greatwood/gold wand and all four returned phials are retained. Supplied INPUT top-up sets that wand to4000 ignis/3000 perditio centivis; other primals/components are preserved. Supplied four fire shards, four quartz, one fire charge, FOCUSFIRE research, empty workbench, offhand apple, bounded flat patch, four planted block fixtures and one ordinary no-AI named cow. Actor effects are cleared. No focus item/output/component, ember, target damage/fire, flower item drop, quicksilver or blaze powder is injected.");
        evidence.put("input_boundary", "No OS input. Native KeyMapping held states/clicks, native screen mouse/key handlers, native useItem/releaseUsingItem, survival destroy-block packets and native inventory-slot clicks. Shared-F dispatch fixtures may briefly unbind either competing mapping to exercise one-mapping delivery and a competing vanilla click, restoring both F bindings before any tick; they never edit click counters or focus state.");
        evidence.put("plant_boundary", "Two decorative species are supplied as real native blocks: one surviving display and one survival-harvest input per species. Placement/generation, biome frequency and full survival/research acquisition are NOT claimed. Particles must arise from ordinary client animate ticks, never an effect callback invoked by this harness.");
        evidence.put("not_covered", List.of("focus upgrades", "other foci", "focus pouches", "natural plant distribution", "survival material/research acquisition", "visual parity before PNG inspection"));
    }

    boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ticks++;
        if (ticks > 1800) throw new IllegalStateException("Focus client step timed out: " + step
                + "; screen=" + (client.screen == null ? "none" : client.screen.getClass().getName())
                + "; selected=" + client.player.getInventory().selected + "; hand=" + client.player.getMainHandItem()
                + "; hand_components=" + client.player.getMainHandItem().getComponents()
                + "; slot7=" + client.player.getInventory().getItem(7).getComponents()
                + "; server_hand=" + (client.getSingleplayerServer() == null ? "none"
                        : client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()).getMainHandItem().getComponents())
                + "; using=" + client.player.isUsingItem() + "; selections=" + selectionPackets + "; swaps=" + offhandSwapPackets.get());
        if (castProbe != null && castProbe.failure != null) throw new IllegalStateException("Native cast observation failed", castProbe.failure);
        if (wireObservationFailure != null) throw new IllegalStateException("Native packet observation failed", wireObservationFailure);
        boolean inputStep = switch (step) {
            case CANCEL_PRESS, CANCEL_WAIT, FOCUS_PRESS, RADIAL_WAIT, RADIAL_SELECT, CAST_START,
                    REMOVE_PRESS, NORMAL_SWAP_PRESS, NORMAL_SWAP_RETURN -> true;
            default -> false;
        };
        if (inputStep && !client.isWindowActive()) {
            // Focus loss is an environment interruption, not evidence that F
            // failed. Never fake Window's active flag or send input off-window.
            evidence.put("inactive_input_wait_ticks", ++inactiveInputTicks);
            releaseFocusKey(client);
            if (step == Step.CANCEL_WAIT) next(Step.CANCEL_PRESS);
            else if (step == Step.RADIAL_WAIT || step == Step.RADIAL_SELECT) next(Step.FOCUS_PRESS);
            return false;
        }
        // Opening a Screen releases native mappings. Reassert the scripted held
        // key after that transition; this is input, not a call into UI internals.
        if ((step == Step.CANCEL_WAIT || step == Step.RADIAL_WAIT || step == Step.RADIAL_SELECT)
                && client.screen instanceof GuiFocusSelect) ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true);
        switch (step) {
            case SETUP -> {
                client.setScreen(null);
                // Keep the native last-sent hotbar cache in agreement with the
                // supplied server fixture. The previous book scene sent slot6;
                // changing ONLY the server to8 makes useItem(slot6) omit the
                // otherwise-required C2S carried-slot packet as "unchanged".
                client.player.getInventory().selected = 8;
                releaseFocusKey(client);
                client.options.keyShift.setDown(false);
                client.options.keyUse.setDown(false);
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.options.particles().set(ParticleStatus.ALL);
                onServer(client, () -> prepare(serverPlayer(client)));
                next(Step.BOOK_OPEN);
            }
            case BOOK_OPEN -> {
                if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("FOCUSFIRE")
                        || !client.player.getInventory().getItem(3).is(fireShard())
                        || ItemWandCasting.getVis(client.player.getInventory().getItem(7), "ignis") != 4000) return false;
                client.player.getInventory().selected = 6;
                require(client.player.getMainHandItem().is(ThaumcraftContent.THAUMONOMICON.get()), "focus research opens through the retained real book");
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                next(Step.BOOK_CATEGORY);
            }
            case BOOK_CATEGORY -> {
                if (!(client.screen instanceof GuiResearchBrowser) || ticks < 10) return false;
                BookClientSmoke.clickCategoryTab(client, "THAUMATURGY");
                next(Step.BOOK_PAN);
            }
            case BOOK_PAN -> {
                if (ticks < 10) return false;
                var research = ResearchCatalog.get("FOCUSFIRE");
                int x = (int) BookClientSmoke.field(client.screen, "graphLeft") + (int) BookClientSmoke.field(client.screen, "graphWidth") / 2;
                int y = (int) BookClientSmoke.field(client.screen, "graphTop") + (int) BookClientSmoke.field(client.screen, "graphHeight") / 2;
                require(client.screen.mouseDragged(x, y, 0, -research.column() * 24, -research.row() * 24), "native map drag centres FOCUSFIRE");
                next(Step.BOOK_ENTRY);
            }
            case BOOK_ENTRY -> {
                if (ticks < 10) return false;
                int[] point = researchPoint(client, "FOCUSFIRE");
                Screen browser = client.screen;
                browser.mouseClicked(point[0] + 8, point[1] + 8, 0);
                browser.mouseReleased(point[0] + 8, point[1] + 8, 0);
                require(client.screen instanceof GuiResearchRecipe, "real browser focus icon opens its native research entry");
                next(Step.BOOK_RECIPE);
            }
            case BOOK_RECIPE -> {
                if (ticks < 10 || !showRecipe(client, "arcane/focus_fire")) return false;
                evidence.put("native_focus_book", Map.of("research", "FOCUSFIRE", "recipe", "thaumcraft:arcane/focus_fire", "opened_by_book_and_browser_click", true,
                        "page_index", BookClientSmoke.field(client.screen, "page"), "pattern", List.of("CQC", "Q#Q", "CQC"), "base_vis", Map.of("ignis", 20, "perditio", 10)));
                next(Step.WORKBENCH_OPEN);
                capture.accept("41-native-fire-focus-recipe-book-en.png");
            }
            case WORKBENCH_OPEN -> {
                client.setScreen(null);
                client.player.getInventory().selected = 8;
                useBlock(client, workbench);
                next(Step.FUND);
            }
            case FUND -> {
                if (!(client.screen instanceof GuiArcaneWorkbench) || ticks < 10) return false;
                click(client, 45, 0, ClickType.QUICK_MOVE); // Retained crafted wand: hotbar7 -> actual wand slot.
                next(Step.SHARDS_PICKUP);
            }
            case SHARDS_PICKUP -> {
                if (ticks < 6 || !isGreatwoodGold(arcane(client).getSlot(1).getItem()) || !client.player.getInventory().getItem(7).isEmpty()) return false;
                click(client, 41, 0, ClickType.PICKUP); // Arcane hotbar3: four supplied fire shards.
                matrixIndex = 0;
                next(Step.SHARDS_PLACE);
            }
            case SHARDS_PLACE -> {
                if (ticks < 5 || !arcane(client).getCarried().is(fireShard()) || arcane(client).getCarried().getCount() != 4 - matrixIndex) return false;
                click(client, 2 + CORNERS[matrixIndex], 1, ClickType.PICKUP);
                next(++matrixIndex == 4 ? Step.QUARTZ_PICKUP : Step.SHARDS_PLACE);
            }
            case QUARTZ_PICKUP -> {
                if (ticks < 5 || !arcane(client).getCarried().isEmpty()) return false;
                for (int cell : CORNERS) require(arcane(client).getSlot(2 + cell).getItem().is(fireShard()), "four native clicks placed the actual corner shards");
                click(client, 42, 0, ClickType.PICKUP); // Arcane hotbar4: four quartz.
                matrixIndex = 0;
                next(Step.QUARTZ_PLACE);
            }
            case QUARTZ_PLACE -> {
                if (ticks < 5 || !arcane(client).getCarried().is(Items.QUARTZ) || arcane(client).getCarried().getCount() != 4 - matrixIndex) return false;
                click(client, 2 + EDGES[matrixIndex], 1, ClickType.PICKUP);
                next(++matrixIndex == 4 ? Step.CENTER : Step.QUARTZ_PLACE);
            }
            case CENTER -> {
                if (ticks < 5 || !arcane(client).getCarried().isEmpty()) return false;
                click(client, 6, 5, ClickType.SWAP); // Real fire charge from hotbar5 -> matrix centre4.
                next(Step.ASSEMBLY_READY);
            }
            case ASSEMBLY_READY -> {
                var menu = arcane(client);
                if (ticks < 12 || !menu.craftable() || !menu.getSlot(0).getItem().is(ThaumcraftFoci.FIRE_FOCUS.get())) return false;
                require(menu.getSlot(0).getItem().getCount() == 1 && menu.requiredVis("ignis") == 2000 && menu.requiredVis("perditio") == 1000,
                        "real gold-capped workbench preview costs exactly20ignis/10perditio");
                for (int cell = 0; cell < 9; cell++) {
                    Item expected = cell == 4 ? Items.FIRE_CHARGE : cell % 2 == 0 ? fireShard() : Items.QUARTZ;
                    require(menu.getSlot(2 + cell).getItem().is(expected) && menu.getSlot(2 + cell).getItem().getCount() == 1, "native C2S clicks populated exactly CQC/Q#Q/CQC");
                }
                require(count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) == 0 && menu.getCarried().isEmpty(), "result is only a preview; no focus input/output was supplied");
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var tile = (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench);
                    var match = ArcaneCrafting.find(player, tile);
                    require(match != null && match.arcane() && match.recipe().id().equals(id("arcane/focus_fire"))
                            && match.output().is(ThaumcraftFoci.FIRE_FOCUS.get()) && match.output().getCount() == 1
                            && match.prices().equals(Map.of("ignis", 2000, "perditio", 1000)), "authoritative native recipe manager matches and prices Fire Focus");
                    require(ItemWandCasting.data(tile.getItem(10)).equals(craftingVisBefore), "preview does not debit any supplied input vis");
                    serverSnapshot = Map.of("server_verified", true, "recipe", match.recipe().id().toString(), "matrix", List.of("CQC", "Q#Q", "CQC"),
                            "shards", 4, "quartz", 4, "fire_charge", 1, "prices_centivis", match.prices(), "wand", "retained scene39 greatwood/gold", "result_is_preview_only", true);
                });
                next(Step.ASSEMBLY_CAPTURE);
            }
            case ASSEMBLY_CAPTURE -> {
                if (!serverDone() || ticks < 6) return false;
                evidence.put("native_focus_assembly_menu", serverSnapshot);
                next(Step.CRAFT);
                capture.accept("42-native-fire-focus-arcane-assembly-menu-en.png");
            }
            case CRAFT -> {
                require(client.player.getInventory().getItem(8).isEmpty(), "physical focus output destination starts empty");
                click(client, 0, 0, ClickType.PICKUP);
                require(arcane(client).getCarried().isEmpty(), "result click does not optimistically inject a focus");
                next(Step.CRAFTED);
            }
            case CRAFTED -> {
                var menu = arcane(client);
                if (ticks < 8 || !menu.getCarried().is(ThaumcraftFoci.FIRE_FOCUS.get())) return false;
                require(menu.getCarried().getCount() == 1 && ItemWandCasting.getVis(menu.getSlot(1).getItem(), "ignis") == 2000
                        && ItemWandCasting.getVis(menu.getSlot(1).getItem(), "perditio") == 2000, "one actual focus craft spends exactly2000/1000centivis");
                for (int slot = 2; slot <= 10; slot++) require(menu.getSlot(slot).getItem().isEmpty(), "all nine physical ingredients were consumed once");
                click(client, 46, 0, ClickType.PICKUP); // Store the native cursor output in hotbar8.
                next(Step.CRAFT_STORED);
            }
            case CRAFT_STORED -> {
                if (ticks < 8 || !client.player.getInventory().getItem(8).is(ThaumcraftFoci.FIRE_FOCUS.get()) || !arcane(client).getCarried().isEmpty()) return false;
                // Original wand-slot button normalisation makes hotbar SWAP7
                // inappropriate here. Use native pickup/cursor storage instead.
                click(client, 1, 0, ClickType.PICKUP);
                next(Step.WAND_RETURN);
            }
            case WAND_RETURN -> {
                if (ticks < 8 || !isGreatwoodGold(arcane(client).getCarried()) || !arcane(client).getSlot(1).getItem().isEmpty()) return false;
                require(client.player.getInventory().getItem(7).isEmpty(), "paid wand returns to its original physically freed hotbar slot");
                click(client, 45, 0, ClickType.PICKUP);
                next(Step.CRAFT_VERIFY);
            }
            case CRAFT_VERIFY -> {
                if (ticks < 8 || !isGreatwoodGold(client.player.getInventory().getItem(7)) || !arcane(client).getSlot(1).getItem().isEmpty()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var tile = (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench);
                    require(tile.isEmpty() && player.containerMenu.getCarried().isEmpty(), "native craft/retrieval leaves the entire persistent table and cursor empty");
                    selectedServerWand = player.getInventory().getItem(7);
                    selectedVis = ItemWandCasting.data(selectedServerWand);
                    craftedFocus = player.getInventory().getItem(8).copy();
                    ItemStack expectedWand = craftingWandBefore.copy();
                    expectedWand.set(ThaumcraftContent.WAND_DATA.get(), craftingVisBefore.withAmount("ignis", 2000).withAmount("perditio", 2000));
                    require(isGreatwoodGold(selectedServerWand) && FocusData.get(selectedServerWand).isEmpty()
                            && ItemStack.matches(selectedServerWand, expectedWand), "retrieved original wand retains all non-price components/vis");
                    require(FocusData.isSupportedFocus(craftedFocus) && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1
                            && count(player, fireShard()) == 0 && count(player, Items.QUARTZ) == 0 && count(player, Items.FIRE_CHARGE) == 0,
                            "one real focus output conserves the four shards/four quartz/one fire-charge inputs");
                    require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && ItemStack.matches(player.getOffhandItem(), offhandBefore), "crafting preserves earlier phial outputs and the competing offhand fixture");
                    serverSnapshot = Map.of("server_verified", true, "focus_outputs", 1, "inputs_remaining", 0, "ignis_debit_centivis", 2000,
                            "perditio_debit_centivis", 1000, "wand_vis_after", selectedVis.vis(), "empty_phials_retained", 4, "table_and_cursor_empty", true);
                });
                next(Step.CRAFT_ACK);
            }
            case CRAFT_ACK -> {
                if (!serverDone()) return false;
                evidence.put("native_focus_craft", serverSnapshot);
                client.player.closeContainer();
                client.player.getInventory().selected = 7;
                observePackets(client);
                next(Step.CANCEL_PRESS);
            }
            case CANCEL_PRESS -> {
                if (ticks < 10 || !isGreatwoodGold(client.player.getMainHandItem())) return false;
                pressFocusKey(client);
                next(Step.CANCEL_WAIT);
            }
            case CANCEL_WAIT -> {
                if (!(client.screen instanceof GuiFocusSelect) || ticks < 5) return false;
                require(!client.screen.isPauseScreen() && selectionPackets.isEmpty(), "held F opens the actual non-pausing radial without selecting anything");
                require(client.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0), "native Escape handler cancels radial selection");
                releaseFocusKey(client);
                next(Step.CANCEL_ACK);
            }
            case CANCEL_ACK -> {
                if (ticks < 8) return false;
                require(client.screen == null && selectionPackets.isEmpty() && offhandSwapPackets.get() == 0
                        && FocusData.get(client.player.getMainHandItem()).isEmpty() && count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1,
                        "F open/Escape close sends no selection or competing offhand swap");
                evidence.put("native_radial_cancel", Map.of("opened_by_held_F", true, "closed_by_native_escape", true, "selection_packets", 0, "offhand_swap_packets", 0));
                next(Step.FOCUS_PRESS);
            }
            case FOCUS_PRESS -> {
                pressFocusKey(client);
                next(Step.RADIAL_WAIT);
            }
            case RADIAL_WAIT -> {
                if (!(client.screen instanceof GuiFocusSelect) || ticks < 10) return false;
                require(FocusData.get(client.player.getMainHandItem()).isEmpty() && count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1
                        && selectionPackets.isEmpty() && offhandSwapPackets.get() == 0, "held-F UI shows the actual crafted inventory focus without changing either hand");
                int[] point = hoverPoint(client.screen);
                client.screen.mouseMoved(point[0], point[1]);
                evidence.put("native_radial_input", Map.of("focus_binding", ThaumcraftFocusClient.CHANGE_FOCUS.saveString(), "offhand_binding", client.options.keySwapOffhand.saveString(),
                        "competing_native_click_queued", true, "isolated_duplicate_key_dispatch_fixture", isolatedDuplicateKeyDispatch,
                        "focus_inventory_slot", 8, "button_x", point[0], "button_y", point[1], "pauses_world", client.screen.isPauseScreen(), "offhand_swap_packets", offhandSwapPackets.get()));
                next(Step.RADIAL_SELECT);
                capture.accept("43-native-held-F-focus-selection-radial-en.png");
            }
            case RADIAL_SELECT -> {
                require(client.screen instanceof GuiFocusSelect, "capture retains the native held-F selection screen");
                Screen radial = client.screen;
                int[] point = hoverPoint(radial);
                radial.mouseMoved(point[0], point[1]);
                require(radial.mouseClicked(point[0], point[1], 0), "real radial button mouse handler accepts the crafted focus");
                radial.mouseReleased(point[0], point[1], 0);
                releaseFocusKey(client);
                require(FocusData.get(client.player.getMainHandItem()).isEmpty() && count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1,
                        "native radial sends intent, not an optimistic local component/inventory write");
                next(Step.EQUIPPED);
            }
            case EQUIPPED -> {
                if (ticks < 10 || !FocusData.get(client.player.getMainHandItem()).is(ThaumcraftFoci.FIRE_FOCUS.get()) || count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) != 0) return false;
                require(selectionPackets.equals(List.of(8)) && offhandSwapPackets.get() == 0,
                        "actual UI emits exactly one source-slot intent and no vanilla swap packet; selections=" + selectionPackets + "; swaps=" + offhandSwapPackets.get());
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    requireHands(player);
                    require(ItemStack.matches(FocusData.get(selectedServerWand), craftedFocus) && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 0
                            && ItemWandCasting.data(selectedServerWand).equals(selectedVis), "server equips precisely the crafted focus, preserving wand identity/materials/vis and the apple");
                    var cow = EntityType.COW.create(player.serverLevel());
                    require(cow != null, "native cow fixture can be constructed");
                    cow.setNoAi(true);
                    cow.setPersistenceRequired();
                    cow.addTag(TARGET_TAG);
                    cow.setCustomName(Component.literal("Native Fire Focus target"));
                    cow.setCustomNameVisible(true);
                    cow.moveTo(arena.getX() + .5, arena.getY(), arena.getZ() + .5, 180, 0);
                    require(player.serverLevel().addFreshEntity(cow), "only the ordinary living target is spawned as a combat fixture");
                    require(cow.getHealth() == cow.getMaxHealth() && !cow.isOnFire() && !cow.fireImmune(), "target starts at native maximum health without injected damage, fire or immunity");
                    targetId = cow.getId();
                    player.teleportTo(arena.getX() + .5, arena.getY(), arena.getZ() + 3.5);
                    serverSnapshot = Map.of("server_verified", true, "source_slot", 8, "loose_focus_after", 0, "same_server_wand_instance", true,
                            "wand_vis_unchanged", true, "offhand_apple_unchanged", true, "selection_packets", List.copyOf(selectionPackets), "offhand_swap_packets", offhandSwapPackets.get());
                });
                next(Step.CAST_POSITION);
            }
            case CAST_POSITION -> {
                if (!serverDone() || ticks < 15 || !(client.level.getEntity(targetId) instanceof Cow cow)) return false;
                evidence.put("native_focus_equip", serverSnapshot);
                lookAt(client, cow.getBoundingBox().getCenter());
                next(Step.CAST_AIM);
            }
            case CAST_AIM -> {
                lookAt(client, client.level.getEntity(targetId).getBoundingBox().getCenter());
                if (ticks < 10) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    requireHands(player);
                    require(!player.isUsingItem() && ItemFocusFire.tickCost(selectedServerWand, player) == 10, "native gold-cap/no-effect actor costs exactly10centivis per paid tick");
                    castProbe = new CastProbe(player, (Cow) player.serverLevel().getEntity(targetId));
                });
                next(Step.CAST_START);
            }
            case CAST_START -> {
                if (!serverDone()) return false;
                lookAt(client, client.level.getEntity(targetId).getBoundingBox().getCenter());
                require(client.player.getMainHandItem().getUseAnimation() == UseAnim.BOW, "equipped native wand exposes the original charged/BOW use animation");
                castUseBefore = usePackets.get();
                castReleaseBefore = releasePackets.get();
                castActive = true;
                ClientTickEvent.CLIENT_PRE.register(castGuard);
                client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "real native RMB/useItem starts focused air use");
                next(Step.CAST_LIVE);
            }
            case CAST_LIVE -> {
                var frame = castProbe.frame;
                var cow = (Cow) client.level.getEntity(targetId);
                lookAt(client, cow.getBoundingBox().getCenter());
                var embers = client.level.getEntitiesOfClass(EntityEmber.class, client.player.getBoundingBox().inflate(8));
                if (frame.paidTicks() < 5 || frame.health() >= castProbe.healthBefore || !frame.onFire()
                        || cow.getHealth() >= castProbe.healthBefore || !cow.isOnFire() || embers.isEmpty()) {
                    require(!releaseSent, "no native live damage/fire/tracked-ember capture before the fourteen-client-tick safety release");
                    return false;
                }
                require(client.screen == null && client.player.isUsingItem() && cow.isAlive() && frame.liveEmbers() > 0,
                        "world capture contains a genuinely using wand, tracked live embers and a live damaged/burning cow");
                require(usePackets.get() - castUseBefore == 1 && offhandSwapPackets.get() == 0, "one actual native use packet started combat; F never swapped hands");
                evidence.put("native_live_fire_capture", Map.of("paid_ticks_at_observation", frame.paidTicks(), "ignis_debit_centivis", frame.debit(),
                        "server_live_embers", frame.liveEmbers(), "client_live_embers", embers.size(), "target_health_before", castProbe.healthBefore,
                        "target_health_server", frame.health(), "target_health_client", cow.getHealth(), "target_burning", cow.isOnFire(),
                        "ember_renderer", client.getEntityRenderDispatcher().getRenderer(embers.getFirst()).getClass().getName(), "native_use_animation", "BOW"));
                next(Step.CAST_STOP);
                capture.accept("44-native-fire-focus-live-embers-damage-and-ignition-world-en.png");
            }
            case CAST_STOP -> {
                releaseCast(client);
                castActive = false;
                ClientTickEvent.CLIENT_PRE.unregister(castGuard);
                next(Step.CAST_ACK);
            }
            case CAST_ACK -> {
                var frame = castProbe.frame;
                if (frame.using() || frame.stableStoppedTicks() < 2 || client.player.isUsingItem()) return false;
                require(usePackets.get() - castUseBefore == 1 && releasePackets.get() - castReleaseBefore == 1, "native cast uses one real useItem and one RELEASE_USE_ITEM packet");
                onServer(client, () -> {
                    requireHands(serverPlayer(client));
                    serverSnapshot = castProbe.finish();
                    stoppedVis = ItemWandCasting.data(selectedServerWand);
                });
                next(Step.REMOVE_PREPARE);
            }
            case REMOVE_PREPARE -> {
                if (!serverDone()) return false;
                evidence.put("native_fire_cast", serverSnapshot);
                client.options.keyShift.setDown(true);
                next(Step.REMOVE_PRESS);
            }
            case REMOVE_PRESS -> {
                if (ticks < 4 || !client.player.isShiftKeyDown()) return false;
                pressFocusKey(client);
                next(Step.REMOVE_WAIT);
            }
            case REMOVE_WAIT -> {
                if (ticks < 10 || !FocusData.get(client.player.getMainHandItem()).isEmpty() || count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) != 1) return false;
                require(client.screen == null && selectionPackets.equals(List.of(8, -1)) && offhandSwapPackets.get() == 0,
                        "actual held Shift+F removes once without opening a radial, repeated intent or offhand swap");
                returnedFocusSlot = itemSlot(client.player, ThaumcraftFoci.FIRE_FOCUS.get());
                require(ItemStack.matches(client.player.getInventory().getItem(returnedFocusSlot), craftedFocus)
                        && !client.player.getMainHandItem().has(ThaumcraftFoci.WAND_FOCUS.get())
                        && ItemWandCasting.data(client.player.getMainHandItem()).equals(stoppedVis), "S2C returns the actual crafted focus, clears the installed component and retains exactly the server-paid vis");
                releaseFocusKey(client);
                client.options.keyShift.setDown(false);
                next(Step.REMOVE_VERIFY);
            }
            case REMOVE_VERIFY -> {
                if (ticks < 10 || client.player.isShiftKeyDown()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    requireHands(player);
                    require(FocusData.get(selectedServerWand).isEmpty() && !selectedServerWand.has(ThaumcraftFoci.WAND_FOCUS.get())
                            && ItemWandCasting.data(selectedServerWand).equals(stoppedVis), "server removal clears only focus, preserving the same paid wand and final vis");
                    require(count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && ItemStack.matches(player.getInventory().getItem(returnedFocusSlot), craftedFocus), "server returns precisely one complete native crafted focus stack");
                    require(selectionPackets.equals(List.of(8, -1)) && offhandSwapPackets.get() == 0, "observed real C2S selection/removal and zero competing swap packets");
                    serverSnapshot = Map.of("server_verified", true, "key_path", "held Shift+F", "selection_slots_on_wire", List.copyOf(selectionPackets),
                            "returned_focus_count", 1, "returned_inventory_slot", returnedFocusSlot, "focus_component_absent", true,
                            "same_server_wand_instance", true, "offhand_apple_unchanged", true, "offhand_swap_packets", offhandSwapPackets.get(), "final_vis", stoppedVis.vis());
                });
                client.setScreen(new InventoryScreen(client.player));
                next(Step.REMOVE_SCENE);
            }
            case REMOVE_SCENE -> {
                if (!serverDone() || ticks < 10 || !(client.screen instanceof InventoryScreen)) return false;
                evidence.put("native_focus_removal", serverSnapshot);
                next(Step.GARDEN_PREPARE);
                capture.accept("45-native-Shift-F-removal-returned-focus-inventory-en.png");
            }
            case GARDEN_PREPARE -> {
                client.player.closeContainer();
                client.player.getInventory().selected = 8;
                require(client.player.getMainHandItem().isEmpty(), "harvest uses the now-empty native focus source hotbar slot, not a fabricated plant item");
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, shimmerItem()) == 0 && count(player, cinderItem()) == 0 && count(player, quicksilver()) == 0 && count(player, Items.BLAZE_POWDER) == 0,
                            "no flower drop or resource output has been supplied");
                    player.teleportTo(garden.getX() + .5, garden.getY(), garden.getZ() + 3.5);
                });
                next(Step.BREAK_SHIMMER);
            }
            case BREAK_SHIMMER -> {
                if (!serverDone() || ticks < 15 || !client.level.getBlockState(shimmerHarvest).is(ThaumcraftPlants.SHIMMERLEAF.get())) return false;
                breakPlant(client, shimmerHarvest);
                onServer(client, () -> serverPlayer(client).teleportTo(shimmerHarvest.getX() + .5, shimmerHarvest.getY(), shimmerHarvest.getZ() + .5));
                next(Step.PICKUP_SHIMMER);
            }
            case PICKUP_SHIMMER -> {
                if (!serverDone() || ticks < 20 || !client.level.isEmptyBlock(shimmerHarvest) || count(client.player, shimmerItem()) != 1) return false;
                breakPlant(client, cinderHarvest);
                onServer(client, () -> serverPlayer(client).teleportTo(cinderHarvest.getX() + .5, cinderHarvest.getY(), cinderHarvest.getZ() + .5));
                next(Step.PICKUP_CINDER);
            }
            case PICKUP_CINDER -> {
                if (!serverDone() || ticks < 20 || !client.level.isEmptyBlock(cinderHarvest) || count(client.player, cinderItem()) != 1) return false;
                require(count(client.player, shimmerItem()) == 1 && plantBreakPackets.get() == 2, "two observed native survival block-break packets produce both physical drops without supplying item fixtures");
                evidence.put("native_flower_harvest", Map.of("action", "native survival block destruction and physical item pickup", "start_destroy_block_packets", plantBreakPackets.get(), "shimmerleaf", 1, "cinderpearl", 1));
                next(Step.PLANT_CRAFT_OPEN);
            }
            case PLANT_CRAFT_OPEN -> {
                client.setScreen(new InventoryScreen(client.player));
                next(Step.PLANT_CRAFT_INSERT);
            }
            case PLANT_CRAFT_INSERT -> {
                if (ticks < 8 || !(client.screen instanceof InventoryScreen)) return false;
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.inventoryMenu.getCarried().isEmpty(), "flower crafting uses the actual survival inventory's2x2 menu");
                plantSourceSlot = itemSlot(client.player, plantInput());
                click(client, inventorySlot(plantSourceSlot), 0, ClickType.PICKUP);
                next(Step.PLANT_CRAFT_GRID);
            }
            case PLANT_CRAFT_GRID -> {
                if (ticks < 6 || !client.player.inventoryMenu.getCarried().is(plantInput())) return false;
                require(client.player.inventoryMenu.getCarried().getCount() == 1, "2x2 crafting input is exactly the one harvested plant");
                click(client, 1, 0, ClickType.PICKUP);
                next(Step.PLANT_CRAFT_READY);
            }
            case PLANT_CRAFT_READY -> {
                var menu = client.player.inventoryMenu;
                if (ticks < 8 || !menu.getSlot(0).getItem().is(plantOutput()) || !menu.getCarried().isEmpty()) return false;
                require(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().is(plantInput()), "native inventory result previews exactly one original flower resource");
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var nativeMenu = player.inventoryMenu;
                    require(player.containerMenu == nativeMenu, "authoritative2x2 fixture remains the real inventory session");
                    var input = CraftingInput.of(2, 2, List.of(nativeMenu.getSlot(1).getItem().copy(), nativeMenu.getSlot(2).getItem().copy(),
                            nativeMenu.getSlot(3).getItem().copy(), nativeMenu.getSlot(4).getItem().copy()));
                    var recipe = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level()).orElseThrow();
                    var output = recipe.value().assemble(input, player.registryAccess());
                    require(recipe.id().equals(id(plantRecipe == 0 ? "quick_silver_from_shimmerleaf" : "blaze_powder_from_cinderpearl"))
                            && output.is(plantOutput()) && output.getCount() == 1, "actual native server RecipeHolder maps the harvested flower to exactly one canonical resource");
                });
                next(Step.PLANT_CRAFT_TAKE);
            }
            case PLANT_CRAFT_TAKE -> {
                if (!serverDone()) return false;
                click(client, 0, 0, ClickType.PICKUP);
                next(Step.PLANT_CRAFT_OUTPUT);
            }
            case PLANT_CRAFT_OUTPUT -> {
                var menu = client.player.inventoryMenu;
                if (ticks < 8 || !menu.getCarried().is(plantOutput())) return false;
                require(menu.getCarried().getCount() == 1 && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "one native result extraction consumes the flower and leaves no second output");
                require(client.player.getInventory().getItem(plantSourceSlot).isEmpty(), "store destination is the physically freed harvest slot");
                click(client, inventorySlot(plantSourceSlot), 0, ClickType.PICKUP);
                next(Step.PLANT_CRAFT_STORED);
            }
            case PLANT_CRAFT_STORED -> {
                if (ticks < 8 || !client.player.inventoryMenu.getCarried().isEmpty() || count(client.player, plantOutput()) != 1) return false;
                if (++plantRecipe < 2) next(Step.PLANT_CRAFT_INSERT);
                else next(Step.PLANTS_VERIFY);
            }
            case PLANTS_VERIFY -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, shimmerItem()) == 0 && count(player, cinderItem()) == 0 && count(player, quicksilver()) == 1 && count(player, Items.BLAZE_POWDER) == 1,
                            "native survival harvest and two real2x2 transactions conserve two plants as exactly one quicksilver and one blaze powder");
                    for (int slot = 0; slot <= 4; slot++) require(player.inventoryMenu.getSlot(slot).getItem().isEmpty(), "native2x2 grid and result are empty after both crafts");
                    require(player.inventoryMenu.getCarried().isEmpty() && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4
                            && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && ItemStack.matches(player.getOffhandItem(), offhandBefore)
                            && ItemWandCasting.data(player.getInventory().getItem(7)).equals(stoppedVis), "flower crafts preserve the actual focus, paid wand, offhand apple and all previous phials");
                    var level = player.serverLevel();
                    require(level.getBlockState(garden.west()).is(ThaumcraftPlants.SHIMMERLEAF.get()) && level.getBlockState(garden.east()).is(ThaumcraftPlants.CINDERPEARL.get())
                            && level.isEmptyBlock(shimmerHarvest) && level.isEmptyBlock(cinderHarvest),
                            "two display plants survive while the two physically harvested fixtures are absent:"
                                    + " displayShimmer=" + level.getBlockState(garden.west()) + "@" + garden.west().toShortString()
                                    + " displayCinder=" + level.getBlockState(garden.east()) + "@" + garden.east().toShortString()
                                    + " harvestShimmer=" + level.getBlockState(shimmerHarvest) + "@" + shimmerHarvest.toShortString()
                                    + " harvestCinder=" + level.getBlockState(cinderHarvest) + "@" + cinderHarvest.toShortString()
                                    + " player=" + player.blockPosition().toShortString());
                    serverSnapshot = Map.of("server_verified", true, "menu", "native survival InventoryMenu2x2", "harvested_plants_consumed", 2,
                            "outputs", Map.of("thaumcraft:quick_silver", 1, "minecraft:blaze_powder", 1), "remaining_display_plants", 2,
                            "recipes", List.of("thaumcraft:quick_silver_from_shimmerleaf", "thaumcraft:blaze_powder_from_cinderpearl"), "empty_phials_retained", 4,
                            "natural_generation_claimed", false);
                });
                next(Step.PLANTS_POSITION);
            }
            case PLANTS_POSITION -> {
                if (!serverDone()) return false;
                evidence.put("native_flower_resource_crafting", serverSnapshot);
                client.player.closeContainer();
                client.player.getInventory().selected = 7;
                onServer(client, () -> serverPlayer(client).teleportTo(garden.getX() + .5, garden.getY(), garden.getZ() + 4.2));
                next(Step.PLANTS_WORLD);
            }
            case PLANTS_WORLD -> {
                if (!serverDone() || ticks < 20) return false;
                lookAt(client, Vec3.atCenterOf(garden).add(0, -.1, 0));
                Map<String, Integer> particles = flowerParticles(client);
                // Vanilla animateTick picks ONE random block per call from a well over a
                // thousand around the player, and the shimmerleaf wisp lives only three
                // ticks. Demanding all three channels in the same instant was therefore a
                // lottery, not an observation: the previous 1800-tick wait proved nothing
                // about the emitters. Track what each channel has delivered over the whole
                // window instead, and record the live counts at the instant of capture.
                for (String channel : List.of("shimmerleaf_wisps", "cinderpearl_flame", "cinderpearl_smoke")) {
                    if (particles.get(channel) > 0) flowerSeen.merge(channel, particles.get(channel), Integer::sum);
                }
                boolean allSeen = flowerSeen.keySet().containsAll(
                        List.of("shimmerleaf_wisps", "cinderpearl_flame", "cinderpearl_smoke"));
                if (ticks >= 900 && !allSeen) {
                    throw new IllegalStateException("flower particles never reached the client: accumulated="
                            + flowerSeen + " live=" + particles
                            + "; player=" + client.player.blockPosition().toShortString()
                            + " displayShimmer=" + client.level.getBlockState(garden.west())
                            + "@" + garden.west().toShortString()
                            + " displayCinder=" + client.level.getBlockState(garden.east())
                            + "@" + garden.east().toShortString()
                            + " distanceShimmer=" + Math.sqrt(client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(garden.west())))
                            + " paused=" + client.isPaused() + " windowActive=" + client.isWindowActive()
                            + " particleStatus=" + client.options.particles().get()
                            + " rndShimmer=" + client.level.getBlockState(garden.west()).isRandomlyTicking()
                            + " rndCinder=" + client.level.getBlockState(garden.east()).isRandomlyTicking());
                }
                // Capture on a frame that actually carries something rather than on a
                // statistically empty instant.
                if (!allSeen || particles.get("shimmerleaf_wisps") == 0 || particles.get("cinderpearl_flame") == 0) return false;
                evidence.put("native_flower_world", Map.of("models", flowerModels(client),
                        "live_particles_at_observation", particles, "particles_seen_over_window", Map.copyOf(flowerSeen),
                        "shimmerleaf", garden.west().toShortString(), "cinderpearl", garden.east().toShortString(), "held_item", "retained unfocused greatwood/gold wand", "particle_callbacks_invoked_by_harness", false));
                next(Step.NORMAL_SWAP_PRESS);
                capture.accept("46-native-shimmerleaf-cinderpearl-models-and-particles-world-en.png");
            }
            case NORMAL_SWAP_PRESS -> {
                if (ticks == 1) client.player.getInventory().selected = 8;
                if (ticks < 10) return false;
                require(ThaumcraftFocusClient.wandHand(client.player) == null && client.player.getOffhandItem().is(Items.APPLE),
                        "ordinary-hand key check has no native wand in either hand");
                ordinaryHandBefore = client.player.getMainHandItem().copy();
                pressOrdinarySwapKey(client);
                next(Step.NORMAL_SWAP_ACK);
            }
            case NORMAL_SWAP_ACK -> {
                if (ticks < 10 || !client.player.getMainHandItem().is(Items.APPLE)
                        || !ItemStack.matches(client.player.getOffhandItem(), ordinaryHandBefore)) return false;
                require(offhandSwapPackets.get() == 1 && selectionPackets.equals(List.of(8, -1)),
                        "F with ordinary hands forwards precisely one vanilla swap and no focus intent");
                releaseFocusKey(client);
                next(Step.NORMAL_SWAP_RETURN);
            }
            case NORMAL_SWAP_RETURN -> {
                if (ticks < 4) return false;
                pressOrdinarySwapKey(client);
                next(Step.NORMAL_SWAP_RESTORED);
            }
            case NORMAL_SWAP_RESTORED -> {
                if (ticks < 10 || !ItemStack.matches(client.player.getMainHandItem(), ordinaryHandBefore)
                        || !ItemStack.matches(client.player.getOffhandItem(), offhandBefore)) return false;
                require(offhandSwapPackets.get() == 2 && selectionPackets.equals(List.of(8, -1)) && client.screen == null,
                        "second ordinary F restores both real hands without opening focus UI");
                releaseFocusKey(client);
                evidence.put("native_non_wand_offhand_passthrough", Map.of("binding", "F", "native_swap_packets", 2,
                        "one_mapping_dispatch_exercised", true, "focus_intents_unchanged", true, "both_hands_restored", true));
                client.player.getInventory().selected = 7;
                next(Step.DONE);
            }
            case DONE -> {
                if (wireChannel != null && wireChannel.pipeline().get(WIRE_OBSERVER) != null) wireChannel.pipeline().remove(WIRE_OBSERVER);
                return true;
            }
            default -> throw new IllegalStateException("Unimplemented focus sequence step " + step);
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        var level = player.serverLevel();
        // Only a prior invocation's explicitly owned target, inside this patch.
        for (Cow old : level.getEntitiesOfClass(Cow.class, new AABB(Vec3.atLowerCornerOf(workbench.offset(-4, 0, -4)), Vec3.atLowerCornerOf(workbench.offset(12, 6, 18))),
                cow -> cow.getTags().contains(TARGET_TAG))) old.discard();
        for (int x = -4; x <= 11; x++) for (int z = -4; z <= 17; z++) {
            level.setBlockAndUpdate(workbench.offset(x, -2, z), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(workbench.offset(x, -1, z), x >= -3 && x <= 3 && z >= 10 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState());
            for (int y = 0; y <= 5; y++) level.setBlockAndUpdate(workbench.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        for (BlockPos pos : List.of(garden.west(), shimmerHarvest)) level.setBlockAndUpdate(pos, ThaumcraftPlants.SHIMMERLEAF.get().defaultBlockState());
        for (BlockPos pos : List.of(garden.east(), cinderHarvest)) {
            level.setBlockAndUpdate(pos.below(), Blocks.SAND.defaultBlockState());
            level.setBlockAndUpdate(pos, ThaumcraftPlants.CINDERPEARL.get().defaultBlockState());
        }
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.removeAllEffects();
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 0,
                "scene40 hands off all four native phials and no injected focus");
        for (int slot : new int[] {3, 4, 5, 8}) require(player.getInventory().getItem(slot).isEmpty(), "new supplied inputs never overwrite earlier native outputs");
        require(player.getOffhandItem().isEmpty(), "offhand clash fixture does not overwrite an earlier item");
        ItemStack wand = player.getInventory().getItem(7);
        require(isGreatwoodGold(wand) && FocusData.get(wand).isEmpty(), "focus sequence retains the actual earlier crafted wand");
        craftingVisBefore = ItemWandCasting.data(wand).withAmount("ignis", 4000).withAmount("perditio", 3000);
        wand.set(ThaumcraftContent.WAND_DATA.get(), craftingVisBefore); // Explicit supplied INPUT vis, never focus/craft output.
        craftingWandBefore = wand.copy();
        player.getInventory().setItem(3, new ItemStack(fireShard(), 4));
        player.getInventory().setItem(4, new ItemStack(Items.QUARTZ, 4));
        player.getInventory().setItem(5, new ItemStack(Items.FIRE_CHARGE));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE));
        offhandBefore = player.getOffhandItem().copy();
        KnowledgeAccess.get(player).addResearch("FOCUSFIRE");
        KnowledgeEvents.sync(player);
        player.getInventory().selected = 8;
        player.inventoryMenu.broadcastChanges();
        player.teleportTo(workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6);
        require(((TileArcaneWorkbench) level.getBlockEntity(workbench)).isEmpty(), "supplied real workbench starts wholly empty");
    }

    /** Read actual widget coordinates only; selection still goes through mouseClicked. */
    static int[] hoverPoint(Screen screen) {
        require(screen instanceof GuiFocusSelect, "radial hover requires the real native focus screen");
        var buttons = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(widget -> widget.getWidth() == 20 && widget.getHeight() == 20).toList();
        require(buttons.size() == 1, "radial contains exactly the one actually crafted loose focus's20x20 native widget");
        var button = buttons.getFirst();
        return new int[] {button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
    }

    private void pressFocusKey(Minecraft client) throws ReflectiveOperationException {
        var focus = ThaumcraftFocusClient.CHANGE_FOCUS;
        var swap = client.options.keySwapOffhand;
        var key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F);
        require(client.isWindowActive(), "native focus input requires the isolated game window to be active");
        require(focus.matches(GLFW.GLFW_KEY_F, 0) && focus.same(swap), "isolated native input fixture tests the actual default F/offhand conflict");
        KeyMapping.click(key);
        if ((int) BookClientSmoke.field(swap, "clickCount") == 0) {
            // Vanilla MAP can dispatch a duplicate binding to just one mapping.
            // Queue the competitor through the public native click API, not a
            // private counter write, then restore both F bindings BEFORE ticking.
            focus.setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            KeyMapping.click(key);
            focus.setKey(key);
            KeyMapping.resetMapping();
            isolatedDuplicateKeyDispatch = true;
        }
        require((int) BookClientSmoke.field(swap, "clickCount") > 0 && focus.same(swap), "real competing vanilla swap click is queued under restored identical F bindings");
        focus.setDown(true);
        swap.setDown(true);
    }

    private static void releaseFocusKey(Minecraft client) {
        ThaumcraftFocusClient.CHANGE_FOCUS.setDown(false);
        client.options.keySwapOffhand.setDown(false);
    }

    private void pressOrdinarySwapKey(Minecraft client) throws ReflectiveOperationException {
        require(client.isWindowActive(), "ordinary-key input also requires the real test window to be active");
        var focus = ThaumcraftFocusClient.CHANGE_FOCUS;
        var swap = client.options.keySwapOffhand;
        var key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F);
        require((int) BookClientSmoke.field(focus, "clickCount") == 0 && (int) BookClientSmoke.field(swap, "clickCount") == 0,
                "ordinary F fixture begins without stale mapping clicks");
        // Public native input only: exercise Fabric's single-mapping case even
        // if resetMapping's iteration order happens to make vanilla F the winner.
        swap.setKey(InputConstants.UNKNOWN);
        KeyMapping.resetMapping();
        KeyMapping.click(key);
        swap.setKey(key);
        KeyMapping.resetMapping();
        require((int) BookClientSmoke.field(focus, "clickCount") == 1 && (int) BookClientSmoke.field(swap, "clickCount") == 0 && focus.same(swap),
                "only the shared focus mapping received the real click, with both default F bindings restored before ticking");
        focus.setDown(true);
        swap.setDown(true);
    }

    private void observePackets(Minecraft client) throws ReflectiveOperationException {
        wireChannel = (Channel) BookClientSmoke.field(client.getConnection().getConnection(), "channel");
        wireChannel.pipeline().addLast(WIRE_OBSERVER, new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                if (message instanceof ServerboundCustomPayloadPacket packet && packet.payload().type().id().equals(FocusSelectionPayload.TYPE.id())) {
                    try {
                        FocusSelectionPayload payload;
                        if (packet.payload() instanceof FocusSelectionPayload direct) payload = direct;
                        else if (packet.payload() instanceof NetworkAggregator.BufCustomPacketPayload wrapped) {
                            // Architectury's real outbound packet contains its
                            // byte-array envelope, NOT the original Java record.
                            // Decode a private copy for observation; never write
                            // the packet/body/counters or dispatch a second intent.
                            var body = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(wrapped.payload().clone()), client.level.registryAccess());
                            try { payload = FocusSelectionPayload.STREAM_CODEC.decode(body); }
                            finally { body.release(); }
                        } else throw new IllegalStateException("Unknown real focus payload envelope: " + packet.payload().getClass().getName());
                        if (payload.hand() == InteractionHand.MAIN_HAND) selectionPackets.add(payload.inventorySlot());
                        else selectionPackets.add(Integer.MIN_VALUE);
                    } catch (RuntimeException | Error error) { wireObservationFailure = error; }
                } else if (message instanceof ServerboundUseItemPacket) usePackets.incrementAndGet();
                else if (message instanceof ServerboundPlayerActionPacket packet) {
                    if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) offhandSwapPackets.incrementAndGet();
                    if (packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) releasePackets.incrementAndGet();
                    if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                            && (packet.getPos().equals(shimmerHarvest) || packet.getPos().equals(cinderHarvest))) plantBreakPackets.incrementAndGet();
                }
                context.write(message, promise);
            }
        });
    }

    private void guardCast(Minecraft client) {
        if (!castActive || releaseSent || client.player == null) return;
        // Capture writing suspends the enclosing sequence, not this native
        // client-tick safety release. It never ticks a focus or stops server use
        // directly; the real RELEASE_USE_ITEM packet owns the server transition.
        if (++castClientTicks >= 14 || castProbe.frame.paidTicks() >= 12) releaseCast(client);
    }

    private void releaseCast(Minecraft client) {
        if (releaseSent) return;
        client.options.keyUse.setDown(false);
        client.gameMode.releaseUsingItem(client.player);
        releaseSent = true;
    }

    private void requireHands(ServerPlayer player) {
        require(player.getInventory().selected == 7 && player.getMainHandItem() == selectedServerWand && isGreatwoodGold(selectedServerWand)
                && ItemStack.matches(player.getOffhandItem(), offhandBefore), "F/native casting retains the same main-hand server wand and untouched offhand apple");
    }

    private record CastFrame(int paidTicks, int debit, int liveEmbers, float health, boolean onFire,
                             boolean using, int stableStoppedTicks) {}

    /** Passive scoped observations of real ticks/spawn attempts, never a simulated focus tick. */
    private static final class CastProbe implements AutoCloseable {
        final ServerPlayer player;
        final Cow target;
        final ItemStack wand;
        final WandData before;
        final float healthBefore;
        final int duration;
        final TickEvent.Server tick;
        final EntityEvent.Add add;
        final List<Map<String, Object>> payments = new ArrayList<>();
        volatile CastFrame frame;
        volatile Throwable failure;
        private boolean active = true, started;
        private int previousVis, paidTicks, spawnAttempts, nativeElapsed, stableStoppedTicks, peakLive;

        CastProbe(ServerPlayer player, Cow target) {
            this.player = player;
            this.target = target;
            wand = player.getMainHandItem();
            before = ItemWandCasting.data(wand);
            healthBefore = target.getHealth();
            duration = wand.getUseDuration(player);
            previousVis = before.amount("ignis");
            require(target.isAlive() && !target.isOnFire() && healthBefore == target.getMaxHealth(), "cast baseline reads the ordinary unmodified living target");
            frame = new CastFrame(0, 0, 0, healthBefore, false, false, 0);
            add = (entity, level) -> {
                if (active && failure == null && level == player.level() && entity instanceof EntityEmber ember && ember.getOwner() == player) {
                    try {
                        require(ember.getDamage() == 2F && ember.getDuration() == 20 && ember.getFireSeconds() == 3 && ember.getScatter() == 15,
                                "actual focus-created native ember carries original basic damage2/scatter15/life20/fire3");
                        spawnAttempts++;
                    } catch (RuntimeException | Error error) { failure = error; }
                }
                return EventResult.pass();
            };
            tick = server -> {
                if (active && failure == null && server == player.server) {
                    try { observeTick(); } catch (RuntimeException | Error error) { failure = error; }
                }
            };
            EntityEvent.ADD.register(add);
            TickEvent.SERVER_POST.register(tick);
        }

        private void observeTick() {
            require(player.getMainHandItem() == wand && ItemFocusFire.tickCost(wand, player) == 10 && FocusData.get(wand).is(ThaumcraftFoci.FIRE_FOCUS.get()),
                    "actual native use keeps its same focused gold wand at the canonical10centivis price");
            WandData current = ItemWandCasting.data(wand);
            for (String primal : WandData.PRIMALS) if (!primal.equals("ignis")) require(current.amount(primal) == before.amount(primal), "basic fire never debits another primal");
            int delta = previousVis - current.amount("ignis");
            require(delta == 0 || delta == 10, "server vis is monotonic and each real tick pays either zero or exactly10centivis");
            boolean using = player.isUsingItem();
            if (using) {
                require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND, "native active-item state owns the original main-hand stack");
                started = true;
                nativeElapsed = duration - player.getUseItemRemainingTicks();
                require(nativeElapsed == paidTicks + (delta == 10 ? 1 : 0), "independent native item-use counter matches the number of paid server ticks");
            }
            if (delta == 10) {
                paidTicks++;
                require(using, "vis payment occurs inside native active item use");
                payments.add(Map.of("paid_tick", paidTicks, "native_elapsed_use_ticks", nativeElapsed, "before", previousVis,
                        "after", current.amount("ignis"), "server_game_time", player.serverLevel().getGameTime()));
            }
            require(spawnAttempts == paidTicks * 2, "two actual native ember ADD callbacks follow each single paid tick");
            int live = player.serverLevel().getEntitiesOfClass(EntityEmber.class, player.getBoundingBox().inflate(8), ember -> ember.getOwner() == player).size();
            peakLive = Math.max(peakLive, live);
            stableStoppedTicks = !using && started && paidTicks > 0 ? stableStoppedTicks + 1 : 0;
            previousVis = current.amount("ignis");
            frame = new CastFrame(paidTicks, before.amount("ignis") - previousVis, live, target.getHealth(), target.isOnFire(), using, stableStoppedTicks);
        }

        Map<String, Object> finish() {
            require(failure == null && !player.isUsingItem() && stableStoppedTicks >= 2 && paidTicks >= 1, "authoritative release has been observed, with at least one paid tick and two stable post-release ticks");
            int delta = before.amount("ignis") - ItemWandCasting.getVis(wand, "ignis");
            int paidFromVis = delta / 10;
            require(delta % 10 == 0 && paidFromVis == paidTicks && delta == paidTicks * 10 && nativeElapsed == paidTicks && spawnAttempts == paidTicks * 2 && peakLive > 0,
                    "final native counter, exact vis delta and observed spawn attempts agree after real release ACK");
            require(target.getHealth() < healthBefore && target.isOnFire(), "only native projectile collisions damaged and ignited the target");
            var result = new LinkedHashMap<String, Object>();
            result.put("server_verified", true);
            result.put("entry", "native C2S useItem -> ItemWandCasting.onUseTick -> ItemFocusFire");
            result.put("release", "native C2S RELEASE_USE_ITEM; two observed stable server ticks afterwards");
            result.put("paid_ticks", paidTicks);
            result.put("paid_ticks_from_vis_delta", paidFromVis);
            result.put("independent_native_elapsed_use_ticks", nativeElapsed);
            result.put("per_tick_centivis", 10);
            result.put("ignis_before", before.amount("ignis"));
            result.put("ignis_after", ItemWandCasting.getVis(wand, "ignis"));
            result.put("exact_ignis_debit", delta);
            result.put("other_primals_unchanged", true);
            result.put("observed_native_ember_add_callbacks", spawnAttempts);
            result.put("add_callback_boundary", "passive real ADD/spawn-attempt observer; not a claim that cancelled spawns would be refunded");
            result.put("peak_live_server_embers", peakLive);
            result.put("actual_payments", List.copyOf(payments));
            result.put("target", Map.of("type", "minecraft:cow", "uuid", target.getUUID().toString(), "no_ai_fixture", true,
                    "health_before", healthBefore, "health_after", target.getHealth(), "remaining_fire_ticks", target.getRemainingFireTicks(), "damage_or_fire_injected", false));
            close();
            return result;
        }

        @Override public void close() {
            active = false;
            TickEvent.SERVER_POST.unregister(tick);
            EntityEvent.ADD.unregister(add);
            require(!TickEvent.SERVER_POST.isRegistered(tick) && !EntityEvent.ADD.isRegistered(add), "scoped native cast observers are removed");
        }
    }

    private Map<String, Integer> flowerParticles(Minecraft client) throws ReflectiveOperationException {
        int shimmer = 0, flame = 0, smoke = 0, nearGarden = 0, resourceAnywhere = 0, resourceNear = 0, smallWisps = 0;
        var tc4 = (thaumcraft.client.fx.ParticleLayers<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, net.minecraft.client.particle.Particle>)
                BookClientSmoke.staticField(thaumcraft.client.fx.ParticleEngine.class, "PARTICLES");
        var queues = new java.util.ArrayList<Object>();
        for (Object queue : ((Map<?, ?>) BookClientSmoke.field(client.particleEngine, "particles")).values()) queues.add(queue);
        // TC4 particles never reach the vanilla map: Tc4ParticleEngineMixin intercepts
        // ParticleEngine.add and diverts every Tc4Particle into Thaumcraft's own owner,
        // which keeps its own dimension/layer store. Reading only the vanilla map made a
        // live, randomly-ticking shimmerleaf look silent for 900 ticks.
        for (int layer = 0; layer < thaumcraft.client.fx.ParticleLayers.LAYERS; layer++) {
            queues.add(tc4.layer(client.level.dimension(), layer));
        }
        for (Object queue : queues) for (Object particle : (Iterable<?>) queue) {
            double x = (double) BookClientSmoke.field(particle, "x"), y = (double) BookClientSmoke.field(particle, "y"), z = (double) BookClientSmoke.field(particle, "z");
            Vec3 position = new Vec3(x, y, z);
            if (particle instanceof ResourceParticle resource) {
                resourceAnywhere++;
                var near = position.distanceToSqr(Vec3.atCenterOf(garden.west())) < 2.25;
                if (near) resourceNear++;
                if (resource.getQuadSize(0) > .025F) { if (near) shimmer++; }
                else if (near) smallWisps++;
            }
            if (position.distanceToSqr(Vec3.atCenterOf(garden)) < 64) nearGarden++;
            if (position.distanceToSqr(Vec3.atCenterOf(garden.east())) < 4) {
                if (particle instanceof FlameParticle) flame++;
                if (particle instanceof SmokeParticle) smoke++;
            }
        }
        return Map.of("shimmerleaf_wisps", shimmer, "cinderpearl_flame", flame, "cinderpearl_smoke", smoke,
                "any_near_garden", nearGarden, "resource_particles_anywhere", resourceAnywhere,
                "resource_particles_near_shimmer", resourceNear, "resource_near_below_quad_threshold", smallWisps);
    }

    private Map<String, Object> flowerModels(Minecraft client) {
        var result = new LinkedHashMap<String, Object>();
        for (BlockPos pos : List.of(garden.west(), garden.east())) {
            var state = client.level.getBlockState(pos);
            var model = client.getBlockRenderer().getBlockModel(state);
            require(model != client.getModelManager().getMissingModel(), "decorative plant resolves a real native block model");
            var quads = model.getQuads(state, null, RandomSource.create(0));
            var textures = new LinkedHashSet<String>();
            quads.forEach(quad -> textures.add(quad.getSprite().contents().name().toString()));
            String name = pos.equals(garden.west()) ? "shimmerleaf" : "cinderpearl";
            require(quads.size() == 4 && textures.equals(java.util.Set.of("thaumcraft:blocks/" + name)), "native crossed-quad model uses its original plant texture, not a catalogue placeholder");
            result.put(name, Map.of("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), "cross_quads", quads.size(), "textures", List.copyOf(textures), "light", state.getLightEmission()));
        }
        return result;
    }

    private boolean showRecipe(Minecraft client, String path) throws ReflectiveOperationException {
        require(client.screen instanceof GuiResearchRecipe, "focus recipe uses the actual native book");
        int page = (int) BookClientSmoke.field(client.screen, "page");
        var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) {
            Object recipe = BookClientSmoke.field(pages.get(i), "recipe");
            if (recipe instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id(path))) {
                require(display.recipe() == client.level.getRecipeManager().byKey(id(path)).orElseThrow(), "book diagram is backed by the actual current native recipe holder");
                return true;
            }
        }
        require(++bookTurns <= 24, "native focus recipe is reachable by bounded page turns");
        clickButton(client, "→");
        ticks = 0;
        return false;
    }

    private static int[] researchPoint(Minecraft client, String key) throws ReflectiveOperationException {
        for (Object node : (List<?>) BookClientSmoke.field(client.screen, "nodes")) {
            if (((ResearchDefinition) BookClientSmoke.field(node, "research")).key().equals(key)) return new int[] {(int) BookClientSmoke.field(node, "x"), (int) BookClientSmoke.field(node, "y")};
        }
        throw new IllegalStateException("Native browser did not draw " + key);
    }

    private static void clickButton(Minecraft client, String label) {
        Screen screen = client.screen;
        var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(widget -> widget.active && widget.getMessage().getString().equals(label)).findFirst().orElseThrow();
        screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
        screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
    }

    private static void useBlock(Minecraft client, BlockPos pos) {
        Vec3 point = Vec3.atCenterOf(pos).add(0, 0, .5);
        require(client.player.canInteractWithBlock(pos, 0), "native workbench interaction remains within survival reach");
        lookAt(client, point);
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, Direction.SOUTH, pos, false)).consumesAction(), "native useItemOn opens the real workbench");
    }

    private static void breakPlant(Minecraft client, BlockPos pos) {
        require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(pos, 0), "real plant harvest retains survival costs and native reach");
        lookAt(client, Vec3.atCenterOf(pos));
        require(client.gameMode.startDestroyBlock(pos, Direction.UP), "native survival startDestroyBlock accepts the instant-break plant");
        client.gameMode.stopDestroyBlock();
    }

    private static void lookAt(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.yRotO = client.player.getYRot();
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        client.player.xRotO = client.player.getXRot();
    }

    private Item plantInput() { return plantRecipe == 0 ? shimmerItem() : cinderItem(); }
    private Item plantOutput() { return plantRecipe == 0 ? quicksilver() : Items.BLAZE_POWDER; }
    private static Item fireShard() { return ThaumcraftContent.SHARDS.get("ignis").get(); }
    private static Item shimmerItem() { return ThaumcraftPlants.SHIMMERLEAF.get().asItem(); }
    private static Item cinderItem() { return ThaumcraftPlants.CINDERPEARL.get().asItem(); }
    private static Item quicksilver() { return ThaumcraftContent.MATERIALS.get("quick_silver").get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static int inventorySlot(int slot) { return slot < 9 ? slot + 36 : slot; }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int itemSlot(Player player, Item item) {
        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(item)) return slot;
        throw new IllegalStateException("Native inventory lacks " + BuiltInRegistries.ITEM.getKey(item));
    }
    private static boolean isGreatwoodGold(ItemStack stack) {
        return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals("greatwood") && ItemWandCasting.data(stack).cap().equals("gold");
    }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void onServer(Minecraft client, Runnable action) {
        require(serverAction == null || serverAction.isDone(), "fixture/server observations never overlap");
        serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer());
    }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
