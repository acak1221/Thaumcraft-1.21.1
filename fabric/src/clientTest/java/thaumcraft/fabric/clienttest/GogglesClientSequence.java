package thaumcraft.fabric.clienttest;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.client.ThaumcraftGogglesClient;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.client.renderers.tile.TileNodeRenderer;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TilePedestal;

/** Seven-scene continuation of the stopped, accepted caps save; no acquisition replay. */
public final class GogglesClientSequence {
    private enum Step {
        HANDOFF, HANDOFF_WAIT, APPLE_STORE, SETUP, BOOK, BOOK_PAGE, METERS,
        CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_FUND, CRAFT_GRID, CRAFT_READY, CRAFT_PREVIEW,
        CRAFT_TAKE, CRAFT_OUTPUT, CRAFT_STORE, CRAFT_STORED, WAND_PICK, WAND_RETURN, CRAFT_FINISH, CRAFT_VERIFIED,
        NODE_TRAVEL, NAKED, WAND_ONLY, METER, OFFHAND_METER, OFFHAND_CHECK, METER_RETURN,
        CHARGE_ARM, CHARGE_START, CHARGE_HELD, CHARGE_STOPPED, GOGGLES,
        HELD_GOGGLES, HELD_CHECK, EQUIP, EQUIPPED, WORN_TRAVEL, WORN, WALL_TRAVEL, WALL,
        JAR_TRAVEL, JAR_BEFORE, PHIAL_HAND, PHIAL_USE, PHIAL_TAKEN, JAR_AFTER,
        POUR_BACK, POURED_BACK, DISCOUNT_CRAFT, FIRE_TRAVEL, FIRE_AIM, FIRE_START, FIRE_USE, FIRE_STOPPED,
        APPLE_RETURN, FINAL_CHECK, INVENTORY, WORN_ITEM, DONE
    }

    private static final List<String> CAPTURES = List.of(
            "74-native-goggles-of-revealing-arcane-recipe-book-en.png",
            "75-normal-nodes-naked-eyes-depth-tested-faint-core-world-en.png",
            "76-earned-mainhand-thaumometer-reveals-normal-nodes-world-en.png",
            "77-earned-head-goggles-empty-hands-live-seven-aspect-node-world-en.png",
            "78-goggles-live-saved-jar-eight-after-native-phial-withdrawal-world-en.png",
            "79-goggles-five-percent-discount-native-jar-85-centivis-menu-en.png",
            "80-earned-goggles-worn-native-inventory-after-eight-centivis-fire-en.png");
    private static final Map<String, Integer> GOGGLES_VIS = Map.of("aer", 5, "ignis", 5, "aqua", 5, "terra", 5, "ordo", 3, "perditio", 3);
    private static final ResourceLocation UNKNOWN_IMAGE = id("textures/aspects/_unknown.png");

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final BlockPos workbench, center, visibleNode, wallNode, wall, savedAirJar, arena;
    private final List<ItemStack> savedStacks = new ArrayList<>(), armorBefore = new ArrayList<>();
    private final Map<BlockPos, BlockState> protectedBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, Map<String, Object>> savedJars = new LinkedHashMap<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), crafts = new ArrayList<>(), views = new ArrayList<>();
    private final ClientTickEvent.Client castGuard = this::guardCast;
    private Step step = Step.HANDOFF, afterCraft;
    private CompletableFuture<Void> serverAction;
    private CameraType previousCamera;
    private ItemStack expectedWand, savedFocus, apple, craftedGoggles, craftWandBefore, output;
    private CompoundTag knowledgeBefore;
    private Aspect unknown;
    private CraftPlan craft;
    private List<int[]> gridClicks;
    private Map<String, Integer> prices;
    private Map<String, Object> craftPreview;
    private int ticks, totalTicks, handoffSlot, appleSlot, turns, gridIndex, crafted, destination, outputBefore, targetId, castClientTicks;
    private int handChangedAt = Integer.MIN_VALUE / 2, xpBefore, jarDelta;
    private boolean castActive, releaseSent;
    private ChargeProbe charge;
    private FireProbe fire;
    private TickEvent.Server nativeTicks;
    private EntityEvent.Add nativeAdds;
    private LifecycleEvent.ServerState serverStopped;
    private volatile Throwable observationFailure;

    private record CraftPlan(String recipe, Item result, Item[] grid, int repetitions, Map<String, Integer> vis, String capture) {}

    /** Root passes accepted caps workbench+32X and the actual retained altar center. */
    public GogglesClientSequence(BlockPos workbench, BlockPos existingCenter) {
        this.workbench = workbench.immutable(); center = existingCenter.immutable();
        visibleNode = workbench.offset(5, 2, 0); wallNode = workbench.offset(7, 2, -6); wall = workbench.offset(7, 2, -5);
        savedAirJar = center.offset(6, 4, 6); arena = workbench.offset(10, 0, 4);
        report.put("scope", "goggles: NEW private copy of accepted STOPPED caps-smoke-1fd3fe4e-9a70-4861-8b82-b1551b2418b3; root owns copying; source world never opened/modified");
        report.put("progression_not_replayed", List.of("research acquisition", "metallurgy/equipment", "distillation/tubes", "altar/72 candles/infusion", "Salis/charged caps/three wand assemblies", "saved Fire Focus acquisition/equipping"));
        report.put("boundary", "All three saved wands (including focused Thaumium/Silverwood:146ignis/300perditio), book, four phials, apple, eight drops/nuggets, six tubes, spare pedestal and173jar essentia are saved INPUTS, not earned here. New RAW INPUTS only:6gold ingots,4leather,2glass,4air shards,7glass panes,1oak slab. Research/parents are declared fixtures, not earned research. Empty workbench, bounded fresh floor, two finite NORMAL nodes, opaque wall, ordinary no-AI cow, camera, daytime/clear weather are fixtures. No goggles/thaumometers/jar/filled-phial outputs, vis top-up, focus component, essentia, damage, fire, ember, scan or discovery is injected.");
        report.put("native_action_boundary", "Native inventory SWAP/QUICK_MOVE/PICKUP, two actual ordinary thaumometer crafts consumed by one real arcane goggles craft, finite node held-use, native head equip, targeted renderer read-only views, real eight-unit phial withdrawal/pour-back, discounted native jar extraction, actual RMB Fire/native server ticks. Only input fixtures/camera and read-only observations run in server futures; no backend craft/drain/focus/tick calls or native key rebinding.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "existing_center", center.toShortString(), "visible_charging_node", visibleNode.toShortString(),
                "wall_node", wallNode.toShortString(), "opaque_wall", wall.toShortString(), "saved_air_jar", savedAirJar.toShortString(), "cow", arena.toShortString()));
        report.put("fresh_patch", Map.of("min", workbench.offset(-2, -2, -8).toShortString(), "max", workbench.offset(12, 5, 8).toShortString(),
                "refuse_existing_BE_TC_block_or_item_drop", true, "altar_or_old_machine_clearing", false));
        report.put("supplied_inputs", inputs); report.put("native_crafts", crafts); report.put("optical_views", views);
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size());
        report.put("visual_inspection", "Pending root inspection of exact74-80 PNGs; semantic/model/resource probes are not a seen-texture or full visual/audio parity claim.");
        report.put("not_covered", List.of("full survival/natural node frequency/research acquisition", "inventory aspect HUD/Arcane Ear/vis drain beams", "Repair enchantment/runic shielding/other wearables", "all foci/upgrades", "NeoForge", "full visual/audio parity"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try {
            return advance(client, capture);
        } catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client);
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Goggles sequence failed at " + step + "; recipe=" + (craft == null ? "none" : craft.recipe()), error);
            throw error;
        }
    }

    private boolean advance(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks; ++totalTicks;
        require(ticks <= 1600 && totalTicks <= 7000, "bounded focused goggles sequence timeout: " + step);
        if (observationFailure != null) throw new IllegalStateException("Native goggles observer failed", observationFailure);
        switch (step) {
            case HANDOFF -> {
                previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON);
                client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false); client.setScreen(null);
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "accepted native menu/cursor intact");
                require(reward(client.player.getInventory().getItem(8)), "actual saved focused Thaumium/Silverwood wand in hotbar8");
                handoffSlot = emptyMain(client.player); click(client, inventorySlot(handoffSlot), 8, ClickType.SWAP); next(Step.HANDOFF_WAIT);
            }
            case HANDOFF_WAIT -> {
                if (ticks < 8 || !client.player.getInventory().getItem(8).isEmpty() || !reward(client.player.getInventory().getItem(handoffSlot))) return false;
                apple = client.player.getOffhandItem().copy(); require(apple.is(Items.APPLE) && apple.getCount() == 1, "saved offhand apple");
                appleSlot = emptyMain(client.player); click(client, inventorySlot(appleSlot), 40, ClickType.SWAP); next(Step.APPLE_STORE);
            }
            case APPLE_STORE -> {
                if (ticks < 8 || !client.player.getOffhandItem().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(appleSlot), apple)) return false;
                select(client, 8); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> {
                if (!serverDone() || ticks < 12 || !KnowledgeAccess.get(client.player).isResearchComplete("GOGGLES")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("GOGGLES"))); next(Step.BOOK);
            }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_PAGE); }
            case BOOK_PAGE -> {
                report.put("native_book", Map.of("research", "GOGGLES", "recipe", id("arcane/goggles").toString(), "actual_synced_recipe_holder", true, "screen_and_research_fixture", true));
                next(Step.METERS); capture.accept(CAPTURES.get(0));
            }
            case METERS -> beginCraft(client, new CraftPlan("thaumometer", meter(), new Item[] {null, shard(), null, Items.GOLD_INGOT, Items.GLASS, Items.GOLD_INGOT, null, shard(), null}, 2, Map.of(), null), Step.NODE_TRAVEL);
            case CRAFT_TRAVEL -> {
                if (!serverDone() || ticks < 10 || !(craft.vis().isEmpty() ? emptyHand(client) : hand(client, GogglesClientSequence::reward))) return false;
                use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5)); next(Step.CRAFT_OPEN);
            }
            case CRAFT_OPEN -> {
                if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false;
                for (int slot = 0; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "truly empty new native table/preview");
                require(arcane(client).getCarried().isEmpty(), "empty crafting cursor");
                craftWandBefore = expectedWand.copy();
                if (!craft.vis().isEmpty()) click(client, arcaneSlot(8), 0, ClickType.QUICK_MOVE);
                next(Step.CRAFT_FUND);
            }
            case CRAFT_FUND -> {
                if (ticks < 8 || !craft.vis().isEmpty() && !ItemStack.matches(arcane(client).getSlot(1).getItem(), expectedWand)) return false;
                require(craft.vis().isEmpty() ? arcane(client).getSlot(1).getItem().isEmpty() : client.player.getInventory().getItem(8).isEmpty(), "only the actual payer moves into the wand slot");
                gridClicks = recipeClicks(client); gridIndex = 0; next(Step.CRAFT_GRID);
            }
            case CRAFT_GRID -> {
                if (ticks < 2) return false;
                if (gridIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; }
                int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0;
            }
            case CRAFT_READY -> {
                if (ticks < 8 || !arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craft.result())) return false;
                require(arcane(client).getSlot(0).getItem().getCount() == 1 && arcane(client).getCarried().isEmpty(), "one genuine native result preview"); verifyGrid(arcane(client), craft.repetitions());
                onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.CRAFT_PREVIEW);
            }
            case CRAFT_PREVIEW -> {
                if (!serverDone()) return false;
                for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == prices.getOrDefault(tag, 0), "native client preview equals server price: " + tag);
                next(Step.CRAFT_TAKE); if (craft.capture() != null) capture.accept(craft.capture());
            }
            case CRAFT_TAKE -> { outputBefore = count(client.player, craft.result()); click(client, 0, 0, ClickType.PICKUP); next(Step.CRAFT_OUTPUT); }
            case CRAFT_OUTPUT -> {
                if (ticks < 6 || !arcane(client).getCarried().is(craft.result())) return false;
                output = arcane(client).getCarried().copy(); require(output.getCount() == 1, "one native extraction, never a supplied output");
                verifyGrid(arcane(client), craft.repetitions() - crafted - 1);
                if (!craft.vis().isEmpty()) {
                    ItemStack expected = expectedWand.copy(); var data = ItemWandCasting.data(expected);
                    for (var cost : prices.entrySet()) data = data.withAmount(cost.getKey(), data.amount(cost.getKey()) - cost.getValue());
                    expected.set(ThaumcraftContent.WAND_DATA.get(), data); // Assertion copy ONLY; never installed in inventory/world.
                    require(ItemStack.matches(arcane(client).getSlot(1).getItem(), expected), "native debit equals preview exactly, with all other components preserved"); expectedWand = expected;
                }
                destination = emptyMain(client.player); next(Step.CRAFT_STORE);
            }
            case CRAFT_STORE -> { click(client, arcaneSlot(destination), 0, ClickType.PICKUP); next(Step.CRAFT_STORED); }
            case CRAFT_STORED -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, craft.result()) != outputBefore + 1) return false;
                if (++crafted < craft.repetitions()) { next(Step.CRAFT_TAKE); return false; }
                if (craft.vis().isEmpty()) next(Step.CRAFT_FINISH);
                else { click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK); }
            }
            case WAND_PICK -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), expectedWand)) return false;
                require(client.player.getInventory().getItem(8).isEmpty(), "original payer return cell empty"); click(client, arcaneSlot(8), 0, ClickType.PICKUP); next(Step.WAND_RETURN);
            }
            case WAND_RETURN -> {
                if (ticks < 6 || !ItemStack.matches(client.player.getInventory().getItem(8), expectedWand) || !arcane(client).getCarried().isEmpty()) return false;
                next(Step.CRAFT_FINISH);
            }
            case CRAFT_FINISH -> {
                onServer(client, () -> finishCraft(serverPlayer(client))); client.player.closeContainer(); next(Step.CRAFT_VERIFIED);
            }
            case CRAFT_VERIFIED -> { if (!serverDone() || ticks < 8) return false; next(afterCraft); }
            case NODE_TRAVEL -> {
                onServer(client, () -> nodeCamera(serverPlayer(client))); next(Step.NAKED);
            }
            case NAKED -> {
                if (!serverDone() || !emptyHand(client)) return false; lookAt(client, Vec3.atCenterOf(visibleNode)); if (ticks < 12) return false;
                opticalView(client, "naked eyes", false); next(Step.WAND_ONLY); capture.accept(CAPTURES.get(1));
            }
            case WAND_ONLY -> {
                if (!hand(client, GogglesClientSequence::reward)) return false; lookAt(client, Vec3.atCenterOf(visibleNode));
                opticalView(client, "focused wand held alone; no use", false); next(Step.METER);
            }
            case METER -> {
                if (!hand(client, stack -> stack.is(meter()))) return false; lookAt(client, Vec3.atCenterOf(visibleNode)); if (ticks < 10) return false;
                opticalView(client, "earned MAIN_HAND thaumometer", true); next(Step.OFFHAND_METER); capture.accept(CAPTURES.get(2));
            }
            case OFFHAND_METER -> {
                require(client.player.getOffhandItem().isEmpty(), "saved apple is safely stored, not overwritten"); click(client, inventorySlot(8), 40, ClickType.SWAP); next(Step.OFFHAND_CHECK);
            }
            case OFFHAND_CHECK -> {
                lookAt(client, Vec3.atCenterOf(visibleNode));
                if (ticks < 8 || !client.player.getMainHandItem().isEmpty() || !client.player.getOffhandItem().is(meter())) return false;
                opticalView(client, "OFF_HAND thaumometer alone", false); click(client, inventorySlot(8), 40, ClickType.SWAP); next(Step.METER_RETURN);
            }
            case METER_RETURN -> {
                if (ticks < 8 || !client.player.getOffhandItem().isEmpty() || !hand(client, GogglesClientSequence::reward)) return false;
                lookAt(client, Vec3.atCenterOf(visibleNode)); if (ticks < 14) return false;
                onServer(client, () -> charge = new ChargeProbe(serverPlayer(client), visibleNode)); next(Step.CHARGE_ARM);
            }
            case CHARGE_ARM -> { if (!serverDone()) return false; next(Step.CHARGE_START); }
            case CHARGE_START -> {
                lookAt(client, Vec3.atCenterOf(visibleNode)); client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "native held-use starts finite node charging, not Fire"); next(Step.CHARGE_HELD);
            }
            case CHARGE_HELD -> {
                lookAt(client, Vec3.atCenterOf(visibleNode));
                if (!WandData.PRIMALS.stream().allMatch(tag -> ItemWandCasting.getVis(client.player.getMainHandItem(), tag) >= 800)) return false;
                client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); next(Step.CHARGE_STOPPED);
            }
            case CHARGE_STOPPED -> {
                if (ticks < 12 || client.player.isUsingItem() || charge.stoppedTicks < 2) return false;
                onServer(client, () -> { report.put("native_finite_charge", charge.finish()); expectedWand = serverPlayer(client).getMainHandItem().copy(); }); next(Step.GOGGLES);
            }
            case GOGGLES -> {
                if (!serverDone()) return false;
                beginCraft(client, new CraftPlan("arcane/goggles", goggles(), new Item[] {Items.LEATHER, Items.GOLD_INGOT, Items.LEATHER, Items.LEATHER, null, Items.LEATHER, meter(), Items.GOLD_INGOT, meter()}, 1, GOGGLES_VIS, null), Step.HELD_GOGGLES);
            }
            case HELD_GOGGLES -> {
                if (!hand(client, stack -> stack.is(goggles()))) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); require(player.getMainHandItem().is(goggles()) && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "real earned goggles held but UNWORN");
                    require(WandVisDiscount.percentagePoints(player, Aspect.AIR) == 0 && ItemFocusFire.tickCost(expectedWand, player) == 9
                            && ArcaneVis.price(Map.of("aer", 5), expectedWand, player).equals(Map.of("aer", 450)), "held goggles grant NO armor discount");
                    report.put("held_unworn_discount", Map.of("actual_percentage_points", 0, "five_vis_thaumium", 450, "fire_centivis", 9));
                }); next(Step.HELD_CHECK);
            }
            case HELD_CHECK -> { if (!serverDone()) return false; next(Step.EQUIP); }
            case EQUIP -> {
                require(client.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "never overwrite saved head armor"); click(client, inventorySlot(8), 0, ClickType.QUICK_MOVE); next(Step.EQUIPPED);
            }
            case EQUIPPED -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getItemBySlot(EquipmentSlot.HEAD), craftedGoggles) || !emptyHand(client)) return false;
                onServer(client, () -> { inspectWearer(serverPlayer(client)); nodeCamera(serverPlayer(client)); }); next(Step.WORN_TRAVEL);
            }
            case WORN_TRAVEL -> { if (!serverDone() || ticks < 12) return false; next(Step.WORN); }
            case WORN -> {
                lookAt(client, Vec3.atCenterOf(visibleNode)); if (ticks < 8) return false;
                require(client.player.getMainHandItem().isEmpty() && client.player.getOffhandItem().isEmpty(), "goggles reveal with BOTH actual hands empty");
                opticalView(client, "earned HEAD goggles, both hands empty", true); inspectTarget(client, visibleNode, node(client.level, visibleNode).getAspects(), true);
                next(Step.WALL_TRAVEL); capture.accept(CAPTURES.get(3));
            }
            case WALL_TRAVEL -> {
                onServer(client, () -> stand(serverPlayer(client), wall.getX() + .5, workbench.getY(), wall.getZ() + 2.6)); next(Step.WALL);
            }
            case WALL -> {
                if (!serverDone()) return false; lookAt(client, Vec3.atCenterOf(wallNode)); if (ticks < 12) return false;
                require(client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && client.level.getBlockState(hit.getBlockPos()).is(Blocks.STONE_BRICKS)
                        && TileNodeRenderer.revealedTo(node(client.level, wallNode), client), "normal crosshair stops on opaque wall while equipped goggles still reveal the node behind it");
                var popup = ThaumcraftGogglesClient.targetView(client);
                require(popup == null || popup.entries().isEmpty() && popup.note() < 0, "node visibility is NOT container x-ray; wall target supplies no popup behind it");
                report.put("opaque_wall", Map.of("actual_hit", ((BlockHitResult) client.hitResult).getBlockPos().toShortString(), "behind_node_revealed", true, "target_popup_entries", 0, "normal_hit_result_not_overridden", true));
                onServer(client, () -> stand(serverPlayer(client), savedAirJar.getX() + .5, savedAirJar.getY(), savedAirJar.getZ() + 3.6)); next(Step.JAR_TRAVEL);
            }
            case JAR_TRAVEL -> {
                if (!serverDone() || !emptyHand(client)) return false; lookAt(client, jarPoint()); if (ticks < 12) return false; next(Step.JAR_BEFORE);
            }
            case JAR_BEFORE -> {
                require(jar(client.level, savedAirJar).getAspect() == Aspect.AIR && jar(client.level, savedAirJar).getAmount() == 16, "actual retained caps AIR16 jar, not injected contents");
                inspectTarget(client, savedAirJar, new AspectList().add(Aspect.AIR, 16), false); next(Step.PHIAL_HAND);
            }
            case PHIAL_HAND -> { if (!hand(client, stack -> stack.is(ThaumcraftContent.EMPTY_PHIAL.get()))) return false; next(Step.PHIAL_USE); }
            case PHIAL_USE -> { use(client, savedAirJar, Direction.SOUTH, jarPoint()); next(Step.PHIAL_TAKEN); }
            case PHIAL_TAKEN -> {
                if (ticks < 8 || jar(client.level, savedAirJar).getAmount() != 8 || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 3
                        || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 1 || !hand(client, GogglesClientSequence::airPhial)) return false;
                onServer(client, () -> { jarDelta = -8; retained(serverPlayer(client)); }); next(Step.JAR_AFTER);
            }
            case JAR_AFTER -> {
                if (!serverDone()) return false; lookAt(client, jarPoint()); if (ticks < 8) return false;
                require(airPhial(client.player.getMainHandItem()), "capture holds the genuine withdrawn AIR8 phial"); inspectTarget(client, savedAirJar, new AspectList().add(Aspect.AIR, 8), false);
                report.put("native_jar_withdrawal", Map.of("position", savedAirJar.toShortString(), "jar_before", 16, "jar_after", 8, "physical_filled_phial_units", 8,
                        "empty_phials", 3, "combined_saved_essentia", 173, "direct_essentia_writes", 0, "server_verified", true)); next(Step.POUR_BACK); capture.accept(CAPTURES.get(4));
            }
            case POUR_BACK -> { use(client, savedAirJar, Direction.SOUTH, jarPoint()); next(Step.POURED_BACK); }
            case POURED_BACK -> {
                if (ticks < 8 || jar(client.level, savedAirJar).getAmount() != 16 || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 4 || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 0) return false;
                lookAt(client, jarPoint()); inspectTarget(client, savedAirJar, new AspectList().add(Aspect.AIR, 16), false);
                onServer(client, () -> { jarDelta = 0; retained(serverPlayer(client)); report.put("native_pour_back", Map.of("jar_after", 16, "same_saved_empty_phials_restored", 4, "combined_saved_essentia", 173, "server_verified", true)); }); next(Step.DISCOUNT_CRAFT);
            }
            case DISCOUNT_CRAFT -> {
                if (!serverDone()) return false;
                beginCraft(client, new CraftPlan("arcane/warded_jar", ThaumcraftContent.JAR.get().asItem(), new Item[] {Items.GLASS_PANE, Items.OAK_SLAB, Items.GLASS_PANE, Items.GLASS_PANE, null, Items.GLASS_PANE, Items.GLASS_PANE, Items.GLASS_PANE, Items.GLASS_PANE}, 1, Map.of("aqua", 1), CAPTURES.get(5)), Step.FIRE_TRAVEL);
            }
            case FIRE_TRAVEL -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var cow = EntityType.COW.create(player.serverLevel()); require(cow != null, "ordinary no-AI target INPUT");
                    cow.setNoAi(true); cow.setPersistenceRequired(); cow.setCustomName(Component.literal("Goggles: native 8 centivis Fire")); cow.setCustomNameVisible(true);
                    cow.moveTo(arena.getX() + .5, arena.getY(), arena.getZ() + .5, 180, 0);
                    require(player.serverLevel().addFreshEntity(cow) && cow.getHealth() == cow.getMaxHealth() && !cow.isOnFire(), "unharmed/unignited target, no injected Fire effect"); targetId = cow.getId();
                    inputs.add(Map.of("fixture", "ordinary no-AI cow INPUT", "uuid", cow.getUUID().toString(), "position", arena.toShortString()));
                    stand(player, arena.getX() + .5, arena.getY(), arena.getZ() + 3.3);
                }); next(Step.FIRE_AIM);
            }
            case FIRE_AIM -> {
                if (!serverDone() || ticks < 12 || !hand(client, GogglesClientSequence::reward) || !(client.level.getEntity(targetId) instanceof Cow cow)) return false;
                lookAt(client, cow.getBoundingBox().getCenter()); if (ticks < 18) return false;
                onServer(client, () -> fire = new FireProbe(serverPlayer(client), (Cow) serverPlayer(client).serverLevel().getEntity(targetId))); next(Step.FIRE_START);
            }
            case FIRE_START -> {
                if (!serverDone() || !client.isWindowActive()) return false; lookAt(client, client.level.getEntity(targetId).getBoundingBox().getCenter());
                castActive = true; releaseSent = false; castClientTicks = 0; ClientTickEvent.CLIENT_PRE.register(castGuard); client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "one actual native RMB starts existing equipped Fire"); next(Step.FIRE_USE);
            }
            case FIRE_USE -> {
                lookAt(client, client.level.getEntity(targetId).getBoundingBox().getCenter());
                if (fire.frame.paid() < 6 && !releaseSent) return false;
                releaseCast(client); castActive = false; ClientTickEvent.CLIENT_PRE.unregister(castGuard); next(Step.FIRE_STOPPED);
            }
            case FIRE_STOPPED -> {
                if (client.player.isUsingItem() || fire.frame.stopped() < 2) return false;
                onServer(client, () -> { report.put("native_eight_centivis_fire", fire.finish()); expectedWand = serverPlayer(client).getMainHandItem().copy(); }); next(Step.APPLE_RETURN);
            }
            case APPLE_RETURN -> {
                if (!serverDone() || ticks < 8) return false;
                require(client.player.getOffhandItem().isEmpty(), "return saved apple only into its truly empty offhand");
                click(client, inventorySlot(itemSlot(client.player, stack -> ItemStack.matches(stack, apple))), 40, ClickType.SWAP); next(Step.FINAL_CHECK);
            }
            case FINAL_CHECK -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getOffhandItem(), apple)) return false;
                onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); stand(serverPlayer(client), workbench.getX() + 3.5, workbench.getY(), workbench.getZ() + 3.5); }); next(Step.INVENTORY);
            }
            case INVENTORY -> {
                if (!serverDone() || ticks < 10) return false;
                require(nativeTicks == null && nativeAdds == null && serverStopped == null && !ClientTickEvent.CLIENT_PRE.isRegistered(castGuard), "all temporary observers/input guards removed");
                report.put("native_worn_presentation", presentation(client)); client.setScreen(new InventoryScreen(client.player)); next(Step.WORN_ITEM);
            }
            case WORN_ITEM -> {
                if (ticks < 10 || !(client.screen instanceof InventoryScreen)) return false;
                report.put("native_client_ticks", totalTicks); next(Step.DONE); capture.accept(CAPTURES.get(6));
            }
            case DONE -> { client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        var level = player.serverLevel(); require(workbench.equals(center.offset(112, 0, 0)), "root must use actual caps workbench+32X, not camera-derived coordinates");
        expectedWand = player.getInventory().getItem(handoffSlot).copy(); savedFocus = FocusData.get(expectedWand).copy(); xpBefore = player.totalExperience;
        require(reward(expectedWand) && ItemWandCasting.getMaxVis(expectedWand) == 10000 && ItemWandCasting.data(expectedWand).vis().equals(Map.of("ignis", 146, "perditio", 300))
                && savedFocus.is(ThaumcraftFoci.FIRE_FOCUS.get()) && count(player, ThaumcraftContent.WAND.get()) == 3, "exact accepted caps INPUT, never reconstructed/topped up");
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0
                && count(player, ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get()) == 6 && count(player, ThaumcraftWands.THAUMIUM_NUGGET.get()) == 8
                && count(player, ThaumcraftContent.MATERIALS.get("quicksilver_drop").get()) == 8 && count(player, ThaumcraftInfusion.PEDESTAL_ITEM.get()) == 1, "saved caps phials/tubes/nuggets/drops/spare pedestal retained");
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty() && !reward(stack) && !stack.is(ThaumcraftContent.EMPTY_PHIAL.get())) savedStacks.add(stack.copy());
        for (ItemStack stack : player.getArmorSlots()) armorBefore.add(stack.copy());
        require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && WandVisDiscount.percentagePoints(player, Aspect.AIR) == 0, "no initial head item/own goggles discount");
        for (Item item : List.of(goggles(), meter(), Items.GOLD_INGOT, Items.LEATHER, Items.GLASS, shard(), Items.GLASS_PANE, Items.OAK_SLAB, ThaumcraftContent.JAR.get().asItem()))
            require(count(player, item) == 0, "fresh raw inputs/outputs cannot mask an existing saved stack: " + itemId(item));
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 84) >> 4; x++) for (int z = (center.getZ() - 10) >> 4; z <= (center.getZ() + 10) >> 4; z++) level.getChunk(x, z);
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-10, 0, -9), center.offset(10, 5, 10))) {
            if (BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft")) protectedBlocks.put(at.immutable(), level.getBlockState(at));
            if (level.getBlockEntity(at) instanceof TileJar tile) savedJars.put(at.immutable(), jarState(tile));
        }
        for (BlockPos at : List.of(center.offset(80, 0, 0), center.offset(83, 0, 0), center.offset(77, 2, 0), center.offset(80, 2, 7))) protectedBlocks.put(at, level.getBlockState(at));
        require(savedJars.size() == 12 && savedJars.values().stream().mapToInt(state -> (Integer) state.get("amount")).sum() == 173
                && jar(level, savedAirJar).getAspect() == Aspect.AIR && jar(level, savedAirJar).getAmount() == 16, "actual twelve saved jars contain129old+44caps surplus, not newly supplied essence");
        var matrix = (TileInfusionMatrix) level.getBlockEntity(center.above(2)); require(matrix != null && matrix.isActive() && !matrix.isCrafting(), "actual retained formed idle altar");
        int candles = 0; for (BlockPos at : protectedBlocks.keySet()) if (level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get())) candles++;
        require(candles == 72, "all72 real saved candles retained");
        report.put("actual_saved_inputs", Map.of("main_stacks_snbt", player.getInventory().items.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.save(player.registryAccess()).toString()).toList(),
                "three_wands", 3, "focused_wand_snbt", expectedWand.save(player.registryAccess()).toString(), "essentia", 173, "candles", candles, "xp_total", xpBefore, "old_acquisition_replayed", false));
        patch(level);
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE_BRICKS.defaultBlockState());
        long oldTime = level.getDayTime(); level.setDayTime(6000); level.setWeatherParameters(0, 6000, false, false);
        inputs.add(Map.of("fixture", "daylight/clear weather", "old_day_time", oldTime, "new_day_time", 6000, "weather", "clear for6000ticks; normal ticking retained"));
        var knowledge = KnowledgeAccess.get(player); var beforeResearch = knowledge.save().copy(); var research = new LinkedHashSet<String>();
        research.addAll(ResearchCatalog.get("GOGGLES").parents()); research.addAll(ResearchCatalog.get("GOGGLES").parentsHidden()); research.add("GOGGLES"); research.add("DISTILESSENTIA");
        var researchBefore = new LinkedHashMap<String, Boolean>(); for (String key : research) { researchBefore.put(key, knowledge.isResearchComplete(key)); knowledge.addResearch(key); }
        require(knowledge.isResearchComplete("NODEPRESERVE"), "saved node-preservation research retained, not newly earned");
        unknown = List.of(Aspect.TAINT, Aspect.SENSES, Aspect.ELDRITCH).stream().filter(aspect -> !knowledge.hasDiscoveredAspect(aspect.getTag())).findFirst()
                .orElseGet(() -> Aspect.getCompoundAspects().stream().filter(aspect -> !knowledge.hasDiscoveredAspect(aspect.getTag())).findFirst().orElseThrow());
        knowledgeBefore = knowledge.save().copy(); KnowledgeEvents.sync(player);
        report.put("research_fixture", Map.of("keys", List.copyOf(research), "already_complete_before", researchBefore, "earned_here", false, "original_saved_knowledge_snbt", beforeResearch.toString()));
        report.put("view_knowledge_baseline", Map.of("unknown_compound_chosen_from_actual_undiscovered", unknown.getTag(), "knowledge_entry_clear_or_scan_writes", 0, "after_declared_research_snbt", knowledgeBefore.toString()));
        AspectList values = new AspectList(); for (String tag : WandData.PRIMALS) values.add(Aspect.getAspect(tag), 20); values.add(unknown, 13);
        freshNode(level, visibleNode, values); freshNode(level, wallNode, new AspectList().add(Aspect.AIR, 24).add(unknown, 9));
        supply(player, Items.GOLD_INGOT, 6); supply(player, Items.LEATHER, 4); supply(player, Items.GLASS, 2); supply(player, shard(), 4); supply(player, Items.GLASS_PANE, 7); supply(player, Items.OAK_SLAB, 1);
        player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false); player.inventoryMenu.broadcastChanges();
        report.put("other_fixtures", Map.of("workbench", "one genuinely empty native arcane workbench", "floor_and_camera", "bounded new stone floor/air patch and checked collision-free positions", "opaque_wall", "nine stone-brick blocks", "xp_added", 0, "direct_wand_top_ups", 0));
        retained(player); startObservation(player);
    }

    private void inspectCraft(ServerPlayer player) {
        var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench); var match = ArcaneCrafting.find(player, table);
        prices = craft.vis().isEmpty() ? Map.of() : ArcaneVis.price(craft.vis(), table.getItem(10), player);
        require(match != null && match.recipe().id().equals(id(craft.recipe())) && match.output().is(craft.result()) && match.output().getCount() == 1
                && match.arcane() == !craft.vis().isEmpty() && match.prices().equals(prices), "actual loaded native recipe and authoritative actor-priced result");
        if (craft.result() == goggles()) require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && WandVisDiscount.percentagePoints(player, Aspect.AIR) == 0
                && prices.equals(Map.of("aer", 450, "ignis", 450, "aqua", 450, "terra", 450, "ordo", 270, "perditio", 270)), "goggles cannot discount their own initial craft");
        if (craft.result() == ThaumcraftContent.JAR.get().asItem()) require(player.getItemBySlot(EquipmentSlot.HEAD).is(goggles()) && prices.equals(Map.of("aqua", 85))
                && ArcaneVis.price(craft.vis(), table.getItem(10), null).equals(Map.of("aqua", 90)), "real equipped five-point discount: native FLOAT multiplication rounds1vis to85 before integer truncation, unlike3vis254/5vis424; actual=" + prices);
        var materials = new LinkedHashMap<String, Integer>(); for (Item item : craft.grid()) if (item != null) materials.merge(itemId(item), 1, Integer::sum);
        craftPreview = Map.of("recipe", match.recipe().id().toString(), "materials_each", materials, "base_whole_vis", craft.vis(), "actual_centivis_each", prices, "repetitions", craft.repetitions(),
                "payer_before", craftWandBefore.save(player.registryAccess()).toString(), "research_fixture_not_earned", true);
    }

    private void finishCraft(ServerPlayer player) {
        require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "native table/cursor empty after actual result extraction and wand return");
        require(crafted == craft.repetitions() && count(player, craft.result()) == craft.repetitions(), "each requested physical result was really extracted once");
        if (craft.result() == goggles()) {
            require(count(player, meter()) == 0 && count(player, Items.GOLD_INGOT) == 0 && count(player, Items.LEATHER) == 0, "both actually crafted thaumometers, two additional gold and four leather consumed");
            craftedGoggles = player.getInventory().getItem(itemSlot(player, stack -> stack.is(goggles()))).copy();
        }
        var entry = new LinkedHashMap<String, Object>(craftPreview); entry.put("native_extractions", crafted); entry.put("server_verified", true);
        entry.put("actual_wand_after", expectedWand.save(player.registryAccess()).toString()); entry.put("output_grants", 0); crafts.add(entry);
        retained(player);
    }

    private void inspectWearer(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD); require(ItemStack.matches(head, craftedGoggles), "native QUICK_MOVE equipped the SAME earned goggles");
        require(head.getItem() instanceof ArmorItem armor && armor.getDefense() == 1 && armor.getEnchantmentValue() == 25 && head.getMaxDamage() == 350
                && head.getDamageValue() == 0 && head.getRarity() == Rarity.RARE && armor.isValidRepairItem(head, new ItemStack(Items.GOLD_INGOT))
                && !armor.isValidRepairItem(head, new ItemStack(Items.IRON_INGOT)), "actual HEAD1/350/rare/gold-repair goggles, not an item model alone");
        require(head.getItem() instanceof IVisDiscountGear gear && gear.getVisDiscount(head, player, Aspect.AIR) == 5
                && head.getItem() instanceof IRevealer revealer && revealer.showNodes(head, player)
                && head.getItem() instanceof IGoggles popups && popups.showIngamePopups(head, player), "three actual functional marker methods");
        require(WandVisDiscount.percentagePoints(player, Aspect.AIR) == 5 && WandVisDiscount.percentagePoints(null, Aspect.AIR) == 0
                && ItemFocusFire.tickCost(expectedWand, player) == 8 && ItemFocusFire.tickCost(expectedWand, null) == 9, "armor-only integer5, null actor has no gear discount; native Fire8 vs9");
        var probes = new ArrayList<Map<String, Object>>();
        for (String cap : List.of("iron", "gold", "thaumium")) {
            // Read-only price prototype; NEVER an additional granted/charged wand.
            ItemStack wand = cap.equals("iron") ? new ItemStack(ThaumcraftContent.WAND.get()) : cap.equals("gold") ? savedStacks.stream().filter(stack -> stack.is(ThaumcraftContent.WAND.get())).findFirst().orElseThrow() : expectedWand;
            int five = ArcaneVis.price(Map.of("aer", 5), wand, player).get("aer"), three = ArcaneVis.price(Map.of("aer", 3), wand, player).get("aer");
            require(five == (cap.equals("iron") ? 525 : cap.equals("gold") ? 475 : 424) && three == (cap.equals("iron") ? 315 : cap.equals("gold") ? 285 : 254), "original percentage-point FLOAT truncation for " + cap);
            probes.add(Map.of("cap", cap, "five_whole_vis_equipped_centivis", five, "three_whole_vis_equipped_centivis", three, "prototype_not_craft_or_charge_claim", cap.equals("iron")));
        }
        report.put("actual_equipped_goggles", Map.of("snbt", head.save(player.registryAccess()).toString(), "native_equip", "inventory QUICK_MOVE, never setItemSlot", "durability", 350,
                "head_armor", 1, "actual_armor_attribute", player.getAttributeValue(Attributes.ARMOR), "discount_integer_percentage_points", 5, "price_probes", probes, "runic_or_repair_enchantment_ability_claimed", false));
    }

    private void opticalView(Minecraft client, String label, boolean revealed) {
        require(TileNodeRenderer.revealedTo(node(client.level, visibleNode), client) == revealed, "actual renderer reveal branch: " + label);
        require(TileNodeRenderer.revealedTo(node(client.level, wallNode), client) == revealed, "same genuine optics also control the NORMAL node behind the opaque wall");
        double range = TileNodeRenderer.revealRange(node(client.level, visibleNode), client);
        require(range == (client.player.getMainHandItem().is(meter()) ? 48 : 64), "actual renderer uses main-hand meter48/head goggles64 ranges");
        require(client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(visibleNode), "normal unmodified crosshair targets actual node");
        require(!KnowledgeAccess.get(client.player).hasDiscoveredAspect(unknown.getTag()) && !KnowledgeAccess.get(client.player).hasScannedPhenomena("@NODE" + node(client.level, visibleNode).getScanId())
                && knowledgeBefore.equals(KnowledgeAccess.get(client.player).save()), "holding/viewing reveals without scanning, discovery, pools or research mutation");
        if (!client.player.getItemBySlot(EquipmentSlot.HEAD).is(goggles())) require(ThaumcraftGogglesClient.targetView(client) == null, "no goggles popup from wand/meter/naked eyes");
        views.add(Map.of("view", label, "actual_renderer_revealed", revealed, "behind_opaque_wall_renderer_revealed", revealed,
                "camera", Map.of("feet", client.player.position().toString(), "yaw", client.player.getYRot(), "pitch", client.player.getXRot()), "current_node", aspects(node(client.level, visibleNode).getAspects()),
                "base_node", aspects(node(client.level, visibleNode).getAspectsBase()), "unknown_compound", unknown.getTag(), "knowledge_unchanged", true, "actual_renderer_range", range));
    }

    /** The SAME immutable target projection used by the production HUD, not a test-owned copy. */
    private void inspectTarget(Minecraft client, BlockPos at, AspectList contents, boolean unknownExpected) {
        var target = ThaumcraftGogglesClient.targetView(client); var expected = aspects(contents); var actual = new LinkedHashMap<String, Integer>(); var entries = new ArrayList<Map<String, Object>>();
        require(target != null && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(at)
                && target.position().equals(at) && target.face() == (client.level.isEmptyBlock(at.above()) ? Direction.UP : hit.getDirection()),
                "popup follows normal current target and original free-space-above face, never a stale/x-ray inventory projection");
        int unknownCount = 0;
        for (var entry : target.entries()) {
            Aspect aspect = Aspect.getAspect(entry.tag()); require(aspect != null && entry.amount() > 0 && actual.put(entry.tag(), entry.amount()) == null, "positive unique actual current aspect entry");
            boolean known = KnowledgeAccess.get(client.player).hasDiscoveredAspect(entry.tag());
            require(entry.known() == known && entry.image().equals(known ? aspect.getImage() : UNKNOWN_IMAGE), "actual known/unknown icon decision comes from viewer knowledge");
            if (!known) unknownCount++;
            entries.add(Map.of("tag", entry.tag(), "amount", entry.amount(), "known", entry.known(), "image", entry.image().toString()));
        }
        require(actual.equals(expected) && unknownCount == (unknownExpected ? 1 : 0), "live popup retains ALL actual current compounds/quantities, no primal reduction or six-entry culling");
        if (unknownExpected) require(actual.size() == 7 && actual.get(unknown.getTag()) == 13 && !actual.equals(aspects(node(client.level, at).getAspectsBase())), "seven-entry row wrap and undiscovered compound are live CURRENT, not base/capacity");
        require(knowledgeBefore.equals(KnowledgeAccess.get(client.player).save()), "targeted popup never discovers/scans/spends research");
        views.add(Map.of("view", "live target popup", "position", at.toShortString(), "face", target.face().getName(), "entries", entries, "unknown_count", unknownCount, "note", target.note(), "knowledge_unchanged", true));
    }

    private void finalState(ServerPlayer player) {
        retained(player); require(ItemStack.matches(player.getOffhandItem(), apple) && ItemStack.matches(player.getItemBySlot(EquipmentSlot.HEAD), craftedGoggles), "same saved apple restored and actual earned goggles remain equipped");
        require(count(player, meter()) == 0 && count(player, goggles()) == 0 && count(player, ThaumcraftContent.JAR.get().asItem()) == 1, "two real meters consumed, one worn goggles and retained real discounted jar");
        for (Item item : List.of(Items.GOLD_INGOT, Items.LEATHER, Items.GLASS, shard(), Items.GLASS_PANE, Items.OAK_SLAB)) require(count(player, item) == 0, "finite raw INPUT fully accounted: " + itemId(item));
        require(player.totalExperience == xpBefore && knowledgeBefore.equals(KnowledgeAccess.get(player).save()), "no XP fixture/payment and viewing never changes saved knowledge beyond declared research fixtures");
        report.put("native_net_outputs", Map.of("ordinary_thaumometers_crafted_then_consumed", 2, "new_goggles_equipped", 1, "new_discounted_jar_retained", 1, "native_jar_debit_centivis", 85,
                "saved_wands_retained", 3, "saved_essentia_restored_in_jars", 173, "empty_saved_phials", 4, "additional_xp", 0, "actual_final_focused_wand_snbt", expectedWand.save(player.registryAccess()).toString()));
        report.put("retained_prior_outputs", Map.of("server_verified", true, "unrelated_saved_stacks_counts_components_exact", true, "two_old_gold_wands_unchanged", true,
                "same_saved_focus_still_equipped", true, "protected_old_TC_blocks", protectedBlocks.size(), "all12saved_jars_restored", true, "viewing_knowledge_unchanged", true, "old_acquisition_replayed", false));
    }

    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) {
            int before = savedStacks.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, saved)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, saved)).mapToInt(ItemStack::getCount).sum();
            if (ItemStack.isSameItemSameComponents(saved, apple) && ItemStack.isSameItemSameComponents(player.getOffhandItem(), apple)) after += player.getOffhandItem().getCount();
            require(before == after, "exact saved unrelated item count/components preserved: " + saved);
        }
        require(count(player, ThaumcraftContent.WAND.get()) + (player.containerMenu instanceof ContainerArcaneWorkbench menu && menu.getSlot(1).getItem().is(ThaumcraftContent.WAND.get()) ? 1 : 0) == 3, "all three saved wands retained");
        ItemStack actual = player.getInventory().items.stream().filter(GogglesClientSequence::reward).findFirst().orElse(ItemStack.EMPTY);
        require(ItemStack.matches(actual, expectedWand) && ItemStack.matches(FocusData.get(actual), savedFocus), "same actual saved Thaumium wand/focus, with only reported native charging/payments");
        int index = 0; for (ItemStack worn : player.getArmorSlots()) {
            if (index == 3 && craftedGoggles != null && worn.is(goggles())) require(ItemStack.matches(worn, craftedGoggles), "native equipped goggles intact");
            else require(ItemStack.matches(worn, armorBefore.get(index)), "all other saved armor slots unchanged"); index++;
        }
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == (jarDelta == 0 ? 4 : 3) && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == (jarDelta == 0 ? 0 : 1), "real whole-phial count conservation");
        if (jarDelta != 0) require(player.getInventory().items.stream().filter(stack -> stack.is(ThaumcraftContent.FILLED_PHIAL.get())).allMatch(GogglesClientSequence::airPhial), "server physical phial really contains the transferred eight AIR units");
        int stored = 0;
        for (var entry : savedJars.entrySet()) {
            var current = jarState(jar(player.level(), entry.getKey())); var expected = new LinkedHashMap<>(entry.getValue());
            if (entry.getKey().equals(savedAirJar)) expected.put("amount", (Integer) expected.get("amount") + jarDelta);
            require(current.equals(expected), "saved jar type/filter/current contents match ONLY the native AIR phial transfer: " + entry.getKey()); stored += (Integer) current.get("amount");
        }
        require(stored + count(player, ThaumcraftContent.FILLED_PHIAL.get()) * PhialData.AMOUNT == 173, "all173saved essentia conserved across jars+actual phial");
        for (var entry : protectedBlocks.entrySet()) require(player.level().getBlockState(entry.getKey()) == entry.getValue(), "old altar/jar/machine/node block never replaced: " + entry.getKey());
        var oven = (TileAlchemyFurnace) player.level().getBlockEntity(center.west(9));
        require(oven != null && oven.getItem(0).isEmpty() && oven.getVis() == 0 && oven.getBurnTime() == 0 && oven.getItem(1).is(Items.COAL) && oven.getItem(1).getCount() == 3, "actual old empty oven and three saved fuel coal untouched");
        for (int dy = 1; dy <= 3; dy++) require(player.level().getBlockEntity(center.west(9).above(dy)) instanceof TileAlembic tile && tile.getAmount() == 0, "old empty alembics untouched");
        for (int x : new int[] {-3, 0, 3}) for (int z : new int[] {-3, 0, 3}) require(player.level().getBlockEntity(center.offset(x, 0, z)) instanceof TilePedestal tile && tile.isEmpty(), "nine real old pedestals remain empty");
        require(knowledgeBefore.equals(KnowledgeAccess.get(player).save()), "server knowledge unchanged by all optical views/native resource actions");
    }

    /** Passive real server END-tick/ember-ADD measurements, removed on success/failure/shutdown. */
    private void startObservation(ServerPlayer player) {
        long started = player.serverLevel().getGameTime();
        nativeAdds = (entity, level) -> {
            if (level == player.level() && fire != null && fire.active && entity instanceof EntityEmber ember && ember.getOwner() == player) {
                try { require(ember.getDamage() == 2F && ember.getDuration() == 20 && ember.getFireSeconds() == 3 && ember.getScatter() == 15, "actual native basic Fire ember stats"); fire.adds++; }
                catch (RuntimeException | Error error) { observationFailure = error; }
            }
            return EventResult.pass();
        };
        nativeTicks = server -> {
            if (server != player.server) return;
            try {
                require(observationFailure == null && player.isAlive() && !player.isRemoved() && player.serverLevel().getGameTime() - started <= 7000, "bounded live native observer");
                if (charge != null && charge.active) charge.observe(); if (fire != null && fire.active) fire.observe();
            } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); }
        };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        EntityEvent.ADD.register(nativeAdds); TickEvent.SERVER_POST.register(nativeTicks); LifecycleEvent.SERVER_STOPPED.register(serverStopped);
    }

    private void stopObservation() {
        if (nativeTicks != null) { TickEvent.SERVER_POST.unregister(nativeTicks); nativeTicks = null; }
        if (nativeAdds != null) { EntityEvent.ADD.unregister(nativeAdds); nativeAdds = null; }
        if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; }
        ClientTickEvent.CLIENT_PRE.unregister(castGuard); if (charge != null) charge.active = false; if (fire != null) fire.active = false;
    }

    private static final class ChargeProbe {
        final ServerPlayer player; final BlockPos at; final ItemStack wand; final WandData before; final Map<String, Integer> sourceBefore, regenerated = new LinkedHashMap<>();
        final long started; WandData previous; Map<String, Integer> previousSource; boolean active = true, used; volatile int stoppedTicks; int transfers;
        ChargeProbe(ServerPlayer player, BlockPos at) {
            this.player = player; this.at = at; wand = player.getMainHandItem(); before = ItemWandCasting.data(wand); previous = before;
            sourceBefore = aspects(node(player.level(), at).getAspects()); previousSource = sourceBefore; started = player.serverLevel().getGameTime();
            require(reward(wand) && !player.isUsingItem(), "real charge baseline; no injected use session");
        }
        void observe() {
            require(player.getMainHandItem() == wand && player.serverLevel().getGameTime() - started <= 1600, "same finite native charge stack/time bound");
            var current = ItemWandCasting.data(wand); var source = aspects(node(player.level(), at).getAspects()); int restored = 0;
            for (String tag : WandData.PRIMALS) {
                int delta = current.amount(tag) - previous.amount(tag); require(delta >= 0 && delta % 100 == 0, "whole native vis transfer, never direct top-up");
                int regen = source.getOrDefault(tag, 0) - previousSource.getOrDefault(tag, 0) + delta / 100;
                require(regen >= 0 && regen <= 1, "finite source debit accounted with native regeneration only"); if (regen > 0) regenerated.merge(tag, regen, Integer::sum);
                restored += regen; transfers += delta / 100;
            }
            require(restored <= 1, "at most one native regenerated node unit per tick");
            for (String tag : sourceBefore.keySet()) if (!WandData.PRIMALS.contains(tag)) require(source.get(tag).equals(sourceBefore.get(tag)), "charging cannot consume compound node aspects");
            if (player.isUsingItem()) { require(player.getUseItem() == wand, "native use owns actual focused wand"); used = true; }
            stoppedTicks = used && !player.isUsingItem() ? stoppedTicks + 1 : 0; previous = current; previousSource = source;
        }
        Map<String, Object> finish() {
            require(used && stoppedTicks >= 2 && !player.isUsingItem() && transfers > 0, "native charge/release really occurred");
            var tile = node(player.level(), at); require(aspects(tile.getAspectsBase()).equals(sourceBefore) && !aspects(tile.getAspects()).equals(sourceBefore), "base retained but actual CURRENT depleted through native charging");
            for (String tag : WandData.PRIMALS) require(ItemWandCasting.getVis(wand, tag) >= 800 && tile.getAspects().getAmount(Aspect.getAspect(tag)) >= 1, "finite native charge funds goggles+jar+Fire and preserves source");
            active = false;
            return Map.of("server_verified", true, "source", at.toShortString(), "source_current_and_base_before", sourceBefore, "source_current_after", aspects(tile.getAspects()), "native_regenerated", Map.copyOf(regenerated),
                    "actual_vis_units_transferred", transfers, "wand_before", before.vis(), "wand_after", ItemWandCasting.data(wand).vis(), "elapsed_native_ticks", player.serverLevel().getGameTime() - started, "backend_charge_calls", 0);
        }
    }

    private record FireFrame(int paid, int stopped) {}
    private static final class FireProbe {
        final ServerPlayer player; final Cow cow; final ItemStack wand; final WandData before; final int duration; final float healthBefore;
        final List<Map<String, Object>> payments = new ArrayList<>(); volatile FireFrame frame = new FireFrame(0, 0); boolean active = true, used; int paid, previous, elapsed, stopped, adds, peakLive;
        FireProbe(ServerPlayer player, Cow cow) {
            this.player = player; this.cow = cow; wand = player.getMainHandItem(); before = ItemWandCasting.data(wand); previous = before.amount("ignis"); duration = wand.getUseDuration(player); healthBefore = cow.getHealth();
            require(reward(wand) && FocusData.get(wand).is(ThaumcraftFoci.FIRE_FOCUS.get()) && player.getItemBySlot(EquipmentSlot.HEAD).is(goggles())
                    && healthBefore == cow.getMaxHealth() && !cow.isOnFire() && ItemFocusFire.tickCost(wand, player) == 8 && ItemFocusFire.tickCost(wand, null) == 9, "actual saved focus+earned goggles/no-damage cow baseline:Fire8 versus9");
        }
        void observe() {
            require(player.getMainHandItem() == wand && ItemFocusFire.tickCost(wand, player) == 8, "same actual focused wand/armor price throughout native use");
            var current = ItemWandCasting.data(wand); int delta = previous - current.amount("ignis"); require(delta == 0 || delta == 8, "EVERY actual server tick pays zero or exactly8centivis");
            for (String tag : WandData.PRIMALS) if (!tag.equals("ignis")) require(current.amount(tag) == before.amount(tag), "Fire preserves all other primals");
            if (player.isUsingItem()) { used = true; require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND, "actual native main-hand use state"); elapsed = duration - player.getUseItemRemainingTicks(); require(elapsed == paid + (delta == 8 ? 1 : 0), "independent native use counter matches actual payments"); }
            if (delta == 8) { require(player.isUsingItem(), "payment only during actual native use"); ++paid; payments.add(Map.of("server_tick", player.serverLevel().getGameTime(), "before", previous, "after", current.amount("ignis"), "native_elapsed_use_ticks", elapsed)); }
            require(adds == paid * 2, "two genuine ember ADD attempts per paid tick, no duplicate tick/collision calls");
            peakLive = Math.max(peakLive, player.serverLevel().getEntitiesOfClass(EntityEmber.class, player.getBoundingBox().inflate(8), ember -> ember.getOwner() == player).size());
            previous = current.amount("ignis"); stopped = used && !player.isUsingItem() && paid > 0 ? stopped + 1 : 0; frame = new FireFrame(paid, stopped);
        }
        Map<String, Object> finish() {
            require(!player.isUsingItem() && stopped >= 2 && paid == 6 && elapsed == paid && before.amount("ignis") - ItemWandCasting.getVis(wand, "ignis") == 48 && adds == 12 && peakLive > 0,
                    "six native ticks x8=48centivis,12actual ember attempts and release acknowledged; never force a debit to fit");
            require(cow.isAlive() && cow.getHealth() < healthBefore && cow.isOnFire(), "genuine native ember collision damages/ignites cow; no damage/fire setter"); active = false;
            var result = new LinkedHashMap<String, Object>(); result.put("server_verified", true); result.put("entry", "native RMB/onUseTick/real ember collision; no focus equip replay");
            result.put("per_paid_tick", 8); result.put("without_goggles_reference", 9); result.put("paid_ticks", paid); result.put("exact_centivis_debit", 48); result.put("native_elapsed_use_ticks", elapsed);
            result.put("ignis_before", before.amount("ignis")); result.put("ignis_after", ItemWandCasting.getVis(wand, "ignis")); result.put("actual_payments", List.copyOf(payments));
            result.put("native_ember_add_attempts", adds); result.put("peak_live_embers", peakLive); result.put("other_primals_unchanged", true);
            result.put("cow", Map.of("uuid", cow.getUUID().toString(), "health_before", healthBefore, "actual_health_after", cow.getHealth(), "actual_fire_ticks", cow.getRemainingFireTicks(), "injected_damage_or_fire", false, "precise_health_assertion", false)); return result;
        }
    }

    private void beginCraft(Minecraft client, CraftPlan plan, Step next) {
        require(serverDone(), "native crafting cannot overlap a server action"); client.setScreen(null); craft = plan; afterCraft = next; crafted = 0;
        onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.CRAFT_TRAVEL);
    }

    /** Literal native cursor clicks; handles the two earned NONSTACKABLE thaumometers separately. */
    private List<int[]> recipeClicks(Minecraft client) {
        var result = new ArrayList<int[]>(); var groups = new LinkedHashMap<Item, List<Integer>>();
        for (int cell = 0; cell < 9; cell++) if (craft.grid()[cell] != null) for (int n = 0; n < craft.repetitions(); n++) groups.computeIfAbsent(craft.grid()[cell], ignored -> new ArrayList<>()).add(cell + 2);
        for (var group : groups.entrySet()) {
            int placed = 0;
            for (int inventory = 0; inventory < 36 && placed < group.getValue().size(); inventory++) {
                ItemStack stack = client.player.getInventory().getItem(inventory); if (!stack.is(group.getKey())) continue;
                int amount = Math.min(stack.getCount(), group.getValue().size() - placed), source = arcaneSlot(inventory); result.add(new int[] {source, 0});
                for (int n = 0; n < amount; n++) result.add(new int[] {group.getValue().get(placed++), 1});
                if (stack.getCount() > amount) result.add(new int[] {source, 0});
            }
            require(placed == group.getValue().size(), "finite real inventory supplies exact recipe inputs: " + itemId(group.getKey()));
        }
        return result;
    }

    private void verifyGrid(ContainerArcaneWorkbench menu, int amount) {
        for (int i = 0; i < 9; i++) { var actual = menu.getSlot(i + 2).getItem(); Item expected = craft.grid()[i]; require(expected == null || amount == 0 ? actual.isEmpty() : actual.is(expected) && actual.getCount() == amount, "exact native grid: " + craft.recipe() + "/" + i); }
    }

    private void patch(ServerLevel level) {
        BlockPos min = workbench.offset(-2, -2, -8), max = workbench.offset(12, 5, 8);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)))).isEmpty(), "new fixture must not erase any saved physical drops");
        for (BlockPos at : BlockPos.betweenClosed(min, max)) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft"), "refuse any old BE/node/machine/TC block in new floor patch: " + at);
        for (BlockPos at : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }

    private void freshNode(ServerLevel level, BlockPos at, AspectList values) {
        require(level.isEmptyBlock(at) && level.getBlockEntity(at) == null, "new declared node cell genuinely empty");
        for (BlockPos nearby : BlockPos.betweenClosed(at.offset(-4, -4, -4), at.offset(4, 4, 4))) require(!level.hasChunkAt(nearby) || !(level.getBlockEntity(nearby) instanceof TileNode), "finite node INPUT isolated from native bullying; no deletion of old nodes");
        level.setBlockAndUpdate(at, ThaumcraftContent.AURA_NODE.get().defaultBlockState()); node(level, at).setAspects(values); // Explicit finite NODE INPUT, never an earned resource/output/expense write.
        require(node(level, at).getNodeType() == TileNode.NodeType.NORMAL, "fresh NORMAL fixture, not a special-node gameplay claim");
        inputs.add(Map.of("fixture", "finite NORMAL node current+base INPUT", "position", at.toShortString(), "aspects", aspects(values), "naturally_found_claimed", false, "unknown_aspect_discovered_by_fixture", false));
    }

    private void supply(ServerPlayer player, Item item, int amount) {
        int slot = emptyMain(player); require(amount > 0 && amount <= item.getDefaultInstance().getMaxStackSize(), "finite raw INPUT stack");
        player.getInventory().setItem(slot, new ItemStack(item, amount)); inputs.add(Map.of("item", itemId(item), "count", amount, "genuinely_empty_main_slot", slot, "purpose", "RAW crafting INPUT, never an output grant"));
    }

    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) if (BookClientSmoke.field(pages.get(i), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id("arcane/goggles"))) {
            require(display.recipe() == client.level.getRecipeManager().byKey(id("arcane/goggles")).orElseThrow(), "book resolves actual synced native goggles recipe holder"); return true;
        }
        require(++turns <= 12, "bounded native goggles book navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(widget -> widget.active && widget.getMessage().getString().equals("→")).findFirst().orElseThrow();
        client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false;
    }

    private Map<String, Object> presentation(Minecraft client) {
        var head = client.player.getItemBySlot(EquipmentSlot.HEAD); require(ItemStack.matches(head, craftedGoggles) && head.getItem() instanceof ArmorItem, "probe only actual earned/equipped goggles");
        var armor = (ArmorItem) head.getItem(); var texture = armor.getMaterial().value().layers().getFirst().texture(false);
        require(client.getResourceManager().getResource(texture).isPresent(), "actual worn native armor-layer texture resolves");
        var model = client.getItemRenderer().getModel(head, client.level, client.player, 0); require(model != client.getModelManager().getMissingModel(), "native goggles item model not missing");
        var sprites = new LinkedHashSet<String>(); for (var quad : model.getQuads(null, null, RandomSource.create(0))) sprites.add(quad.getSprite().contents().name().toString());
        require(sprites.contains("thaumcraft:items/gogglesrevealing") && sprites.stream().noneMatch(sprite -> sprite.contains("missingno")), "actual original goggles item sprite, not missing/fallback equipment");
        return Map.of("actual_item_sprites", List.copyOf(sprites), "actual_worn_texture", texture.toString(), "tooltip", head.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList(),
                "native_screen", "InventoryScreen, same actual HEAD item", "visual_inspection", "pending exact80; resource/model resolution is not seen-texture evidence");
    }

    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside menus");
        if (!wanted.test(client.player.getInventory().getItem(8))) { click(client, inventorySlot(itemSlot(client.player, wanted)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, 8); return true;
    }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "empty hand only via real native inventory SWAP");
        if (!client.player.getInventory().getItem(8).isEmpty()) { click(client, inventorySlot(emptyMain(client.player)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, 8); return true;
    }
    private void guardCast(Minecraft client) { if (castActive && !releaseSent && client.player != null && (++castClientTicks >= 40 || fire.frame.paid() >= 6)) releaseCast(client); }
    private void releaseCast(Minecraft client) { if (!releaseSent) { client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); releaseSent = true; } }
    private void releaseInputs(Minecraft client) {
        castActive = false; ClientTickEvent.CLIENT_PRE.unregister(castGuard); client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false);
        if (client.gameMode != null && client.player != null && client.player.isUsingItem()) client.gameMode.releaseUsingItem(client.player);
        if (previousCamera != null) client.options.setCameraType(previousCamera);
    }
    private void nodeCamera(ServerPlayer player) { stand(player, visibleNode.getX() + .5, workbench.getY(), visibleNode.getZ() + 3.5); }
    private Vec3 jarPoint() { return new Vec3(savedAirJar.getX() + .5, savedAirJar.getY() + .4, savedAirJar.getZ() + .8125); }
    private static void stand(ServerPlayer player, double x, double y, double z) {
        var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z);
        require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "declared camera stands collision-free on actual floor/platform"); player.teleportTo(x, y, z);
    }
    private static boolean reward(ItemStack stack) { return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals("silverwood") && ItemWandCasting.data(stack).cap().equals("thaumium"); }
    private static boolean airPhial(ItemStack stack) { return stack.is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(stack) != null && ItemEssence.data(stack).tag().equals("aer") && stack.getCount() == 1; }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect aspect : list.getAspects()) if (list.getAmount(aspect) > 0) result.put(aspect.getTag(), list.getAmount(aspect)); return result; }
    private static Map<String, Object> jarState(TileJar jar) { return Map.of("aspect", jar.getAspect() == null ? "" : jar.getAspect().getTag(), "amount", jar.getAmount(), "filter", jar.getFilter() == null ? "" : jar.getFilter().getTag()); }
    private static int emptyMain(Player player) { for (int i = 9; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) return i; throw new IllegalStateException("No genuinely empty main inventory slot"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int i = 0; i < 36; i++) if (wanted.test(player.getInventory().getItem(i))) return i; throw new IllegalStateException("Missing actual finite goggles stage input"); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int inventorySlot(int slot) { return slot < 9 ? slot + 36 : slot; }
    private static int arcaneSlot(int slot) { return slot < 9 ? slot + 38 : slot + 2; }
    private static Item meter() { return ThaumcraftContent.THAUMOMETER.get(); }
    private static Item goggles() { return ThaumcraftWearables.GOGGLES.get(); }
    private static Item shard() { return ThaumcraftContent.SHARDS.get("aer").get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static TileNode node(Level level, BlockPos at) { return (TileNode) level.getBlockEntity(at); }
    private static TileJar jar(Level level, BlockPos at) { return (TileJar) level.getBlockEntity(at); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 d = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void use(Minecraft client, BlockPos at, Direction face, Vec3 point) { require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(at, 0), "native survival reach for " + at); lookAt(client, point); require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, at, false)).consumesAction(), "actual native useItemOn accepts " + at); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "server fixture/read-only actions cannot overlap"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
