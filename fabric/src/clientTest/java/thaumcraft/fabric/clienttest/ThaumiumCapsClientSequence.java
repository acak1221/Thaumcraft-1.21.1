package thaumcraft.fabric.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ThaumcraftFocusClient;
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiFocusSelect;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.infusion.InfusionStructure;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TilePedestal;

/** Focused continuation of the REAL accepted M07 save, not a replay of its acquisition. */
public final class ThaumiumCapsClientSequence {
    private enum Step {
        HANDOFF, HANDOFF_WAIT, SETUP, HARVEST_POSITION, HARVEST_AIM, HARVEST_START, HARVEST_HELD,
        HARVEST_COMMIT, HARVEST_WORLD, HARVEST_APPROACH, HARVEST_PICKUP, HARVEST_VERIFIED,
        SALT_TRAVEL, SALT_OPEN, SALT_FUEL_PICK, SALT_FUEL_PUT, SALT_INPUT_PICK, SALT_INPUT_PUT, SALT_STARTED,
        OVEN_TRAVEL, OVEN_OPEN, OVEN_INPUT_PICK, OVEN_INPUT_PUT, DISTILL_WAIT, DISTILL_VERIFY,
        PHIAL_POSITION, PHIAL_HAND, PHIAL_USE, PHIAL_WITHDRAWN, JAR_POSITION, JAR_USE, JAR_FILLED, BATCH_VERIFY,
        SALT_RETURN, SALT_RESULT_OPEN, SALT_READY, SALT_PREVIEW, SALT_TAKE, SALT_TAKEN, SALT_XP, SALT_VERIFIED,
        NUGGET_CRAFT, INERT_CRAFT, CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_FUND, CRAFT_GRID, CRAFT_READY,
        CRAFT_PREVIEW, CRAFT_TAKE, CRAFT_OUTPUT, CRAFT_STORE, CRAFT_STORED, CRAFT_WAND_PICK,
        CRAFT_WAND_STORE, CRAFT_RETURNED, CRAFT_VERIFIED,
        OFFER_PREP, OFFER_POSITION, OFFER_HAND, OFFER_USE, OFFER_WAIT, START_PREP, START_AIM,
        START_USE, START_WAIT, PAID_WORLD, INFUSION_WAIT, CAP_WORLD, CAP_EMPTY_HAND, CAP_EJECT, CAP_PICKUP, CAP_VERIFIED,
        PAYER_PREP, CHARGE_POSITION, CHARGE_AIM, CHARGE_ARM, CHARGE_START, CHARGE_HELD, CHARGE_STOPPED,
        ASSEMBLY_CRAFT, REWARD_PREP, REWARD_HAND, FIRE_NODE_PREP,
        FOCUS_PRESS, FOCUS_WAIT, FOCUS_SELECT, FOCUS_EQUIPPED, CAST_POSITION, CAST_AIM, CAST_START,
        CAST_LIVE, CAST_STOP, CAST_STOPPED, FINAL_VERIFY, DONE
    }

    private static final List<String> PRIMALS = WandData.PRIMALS;
    private static final List<String> OLD_JAR_TAGS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio", "praecantatio", "vitreus");
    private static final List<String> CAPTURES = List.of(
            "67-native-survival-node-harvest-eight-air-wisp-essences-world-en.png",
            "68-native-six-balanced-shards-salis-mundus-furnace-menu-en.png",
            "69-native-thaumium-inert-cap-six-vis-arcane-menu-en.png",
            "70-native-thaumium-cap-paid-auram-potentia-infusion-beams-world-en.png",
            "71-native-second-charged-thaumium-cap-pedestal-world-en.png",
            "72-native-silverwood-thaumium54vis-wand-assembly-menu-en.png",
            "73-native-thaumium-capped-silverwood-fire-nine-centivis-world-en.png");

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final BlockPos workbench, center, matrixPos, ovenPos, saltFurnace, harvestNode, payerNode, fireNode, arena;
    private final List<BlockPos> peripheral = new ArrayList<>(), candles = new ArrayList<>(), offers;
    private final Map<String, BlockPos> newJars = new LinkedHashMap<>();
    private final Map<BlockPos, Map<String, Object>> oldJars = new LinkedHashMap<>();
    private final Map<BlockPos, BlockState> altarBlocks = new LinkedHashMap<>();
    private final List<ItemStack> retainedStacks = new ArrayList<>(), armorBefore = new ArrayList<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), crafts = new ArrayList<>(), batches = new ArrayList<>();
    private final List<Map<String, Object>> phialTransfers = new ArrayList<>(), infusions = new ArrayList<>(), charges = new ArrayList<>();
    private final List<Map<String, Object>> harvestAdds = new ArrayList<>(), payments = new ArrayList<>(), componentPayments = new ArrayList<>();
    private final List<UUID> harvestAddIds = new ArrayList<>();
    private final ClientTickEvent.Client castGuard = this::guardCast;
    private Step step = Step.HANDOFF, afterCraft;
    private CompletableFuture<Void> serverAction;
    private CameraType previousCamera;
    private ItemStack handoff, expectedGreatwood, expectedSilverwood, apple, originalFocus, cursorOutput;
    private ItemStack capDropStack;
    private CraftPlan craft;
    private List<int[]> gridClicks = List.of();
    private Map<String, Integer> prices = Map.of();
    private Map<String, Integer> lastDemandByAspect = Map.of();
    private Map<String, Object> snapshot = Map.of();
    private int ticks, totalTicks, handoffSlot, handChangedAt = Integer.MIN_VALUE / 2;
    private int mineTicks, batch, transfer, sourceDepth, sourceBefore, jarBefore, offerIndex, countBefore;
    private int gridIndex, crafted, destination, outputBefore, saltXpBefore, targetId, castClientTicks;
    private int oldCoalBefore, oldBurnBefore;
    private String transferTag;
    private long batchStarted;
    private boolean harvestCommitted, harvestVerified, saltStartedAck, saltPreviewVerified, capPickupVerified;
    private boolean chargingPayer, castActive, releaseSent;
    private ChargeProbe chargeProbe;
    private FireProbe fireProbe;
    private TickEvent.Server nativeTicks;
    private EntityEvent.Add nativeAdds;
    private LifecycleEvent.ServerState serverStopped;
    private volatile Throwable observationFailure;
    private volatile boolean harvestArmed, infusionArmed, infusionFinished, ejectionArmed;
    private volatile int paidUnits, paidComponents, completedInfusions, capDropAdds;
    private volatile String lastPaidAspect = "";
    private UUID capDropId;
    private boolean infusionStarted, saltArmed, saltObserved;
    private int lastDemand, lastComponents, saltLastInput, saltLastOutput, saltProduced;
    private long observerStarted, lastPayment, lastComponent, saltFirstInput, saltLastProduct;

    private record CraftPlan(String recipe, Item output, int amount, int repetitions, Item[] grid,
                             Map<String, Integer> baseVis, int payerHotbar, String capture) {}

    /** workbench = accepted M07 workbench+96X; existingCenter is read from accepted infusion-client.json. */
    public ThaumiumCapsClientSequence(BlockPos workbench, BlockPos existingCenter) {
        this.workbench = workbench.immutable();
        center = existingCenter.immutable();
        matrixPos = center.above(2);
        ovenPos = center.west(9);
        saltFurnace = workbench.east(3);
        harvestNode = workbench.offset(7, 1, 0);
        payerNode = workbench.offset(-3, 2, 0);
        fireNode = workbench.offset(0, 2, 7);
        arena = workbench.offset(7, 0, 9);
        for (int x : new int[] {-3, 0, 3}) for (int z : new int[] {-3, 0, 3})
            if (x != 0 || z != 0) peripheral.add(center.offset(x, 0, z));
        for (int radius : new int[] {4, 5}) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
            if (Math.max(Math.abs(x), Math.abs(z)) == radius) candles.add(center.offset(x, 0, z));
        offers = List.of(center, center.west(3), center.east(3), center.north(3));
        String[] tags = {"potentia", "ignis", "auram", "aer"};
        for (int i = 0; i < tags.length; i++) newJars.put(tags[i], center.offset(-6 + i * 4, 4, 6));
        report.put("scope", "caps: private copy of the actual accepted M07 book-smoke save; root owns save copying and runner integration");
        report.put("progression_not_replayed", List.of("M07 research/material acquisition", "earlier alchemy/tubes/Fire Focus acquisition",
                "altar/candles/pedestals crafting and formation", "63-unit Silverwood core infusion and old wand assembly", "equipment61-66"));
        report.put("boundary", "Both saved wands, book, apple, four phials, actual Fire Focus, six tubes, eight quicksilver drops, spare pedestal and all other prior stacks are real saved INPUTS, not earned again here. REUSE the actual active formed M07 altar,72 candles,nine placed pedestals,empty oven/three alembics and eight unchanged jars containing129 essentia. Five saved coal may burn natively and their exact remainder is reported. Fresh declared INPUTS: two intermediate thaumium ingots,one intermediate Silverwood rod,six intermediate balanced shards,twelve raw distillation coal,one separate vanilla-smelter fuel coal; CAP_thaumium/THAUMIUM research,empty workbench/vanilla furnace/four jars,bounded floor/supports,finite NORMAL nodes and ordinary no-AI cow. No wisp essence,Salis,inert/charged cap,wand,filled phial,essentia,focus component,beam,stability or paid output is supplied.");
        report.put("native_action_boundary", "Actual survival held mining via startDestroyBlock and Minecraft's single continueAttack path; passive physical ADD observations and real pickup; native ordinary/arcane menu clicks and exact vis payments; ordinary200-tick furnace smelting/XP; native distillation ticks and ten whole-phial withdrawal/refill pairs using the same four phials; three separate peripheral Salis per paid infusion and native pedestal ejection/pickup; original diagonal wand assembly; native finite-node held-use charging; real held-F radial/button intent and held-RMB Fire use. No backend harvest/distill/smelt/craft/start/drain/tick calls, output grants, invented disassembly, old acquisition replay or RNG/session/buffer writes.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "existing_center", center.toShortString(), "existing_matrix", matrixPos.toShortString(),
                "existing_oven", ovenPos.toShortString(), "salis_furnace", saltFurnace.toShortString(), "harvest_node", harvestNode.toShortString(),
                "payer_charging_node", payerNode.toShortString(), "fire_charging_node", fireNode.toShortString(), "new_jars", newJars.values().stream().map(BlockPos::toShortString).toList()));
        report.put("preparation_bounds", Map.of("new_patch_min", workbench.offset(-5, -2, -4).toShortString(), "new_patch_max", workbench.offset(11, 5, 13).toShortString(),
                "altar_new_empty_cells_min", center.offset(-8, 0, 6).toShortString(), "altar_new_empty_cells_max", center.offset(8, 4, 10).toShortString(),
                "altar_preparation", "NO clearing/re-flooring; only checked-empty four jar support columns and raised south phial platform; old candles/pedestals/jars/pillars/nodes never replaced"));
        report.put("supplied_inputs", inputs);
        report.put("native_crafts", crafts);
        report.put("distillation_batches", batches);
        report.put("whole_phial_transfers", phialTransfers);
        report.put("native_infusions", infusions);
        report.put("native_node_charges", charges);
        report.put("capture_names", CAPTURES);
        report.put("capture_count", CAPTURES.size());
        report.put("visual_inspection", "Pending exact PNG inspection by root; model/recipe probes and requested captures are not visual/audio parity evidence.");
        report.put("not_covered", List.of("full survival or natural node frequency", "Wisp mobs/PURE knots/special nodes", "full public API",
                "all instability outcomes", "all wand models/materials/foci/upgrades", "NeoForge", "full visual/audio parity"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try {
            return tickSequence(client, capture);
        } catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client);
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Caps sequence failed at " + step + "; batch=" + batch
                    + "; infusion=" + completedInfusions + "; paid=" + paidUnits + "/18; recipe=" + (craft == null ? "none" : craft.recipe()), error);
            throw error;
        }
    }

    private boolean tickSequence(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks; ++totalTicks;
        if (ticks > 4200 || totalTicks > 20000) throw new IllegalStateException("Caps client timeout: " + step + "; hand=" + client.player.getMainHandItem());
        if (observationFailure != null) throw new IllegalStateException("Passive native caps observation failed", observationFailure);
        if (client.screen instanceof GuiFocusSelect && (step == Step.FOCUS_WAIT || step == Step.FOCUS_SELECT))
            ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true); // Native screens release mappings on opening.
        switch (step) {
            case HANDOFF -> {
                previousCamera = client.options.getCameraType();
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false);
                releaseFocusKey(client);
                client.setScreen(null);
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "saved native inventory/cursor returned intact");
                handoff = client.player.getInventory().getItem(8).copy();
                require(isWand(handoff, "silverwood", "gold") && ItemWandCasting.getVis(handoff, "aer") >= 6000, "actual accepted M07 Silverwood/gold wand in hotbar8");
                handoffSlot = emptyMain(client.player);
                click(client, inventorySlot(handoffSlot), 8, ClickType.SWAP);
                next(Step.HANDOFF_WAIT);
            }
            case HANDOFF_WAIT -> {
                if (ticks < 8 || !client.player.getInventory().getItem(8).isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(handoffSlot), handoff)) return false;
                report.put("native_saved_wand_handoff", Map.of("action", "native inventory SWAP", "from_hotbar", 8, "to_empty_main_slot", handoffSlot));
                next(Step.SETUP);
            }
            case SETUP -> {
                select(client, 8);
                onServer(client, () -> prepare(serverPlayer(client)));
                next(Step.HARVEST_POSITION);
            }
            case HARVEST_POSITION -> {
                if (!serverDone() || ticks < 12) return false;
                require(client.player.getMainHandItem().isEmpty(), "fresh node mining starts with the genuinely empty action hand");
                onServer(client, () -> {
                    var player = serverPlayer(client); var node = node(player.level(), harvestNode);
                    require(node.getNodeType() == TileNode.NodeType.NORMAL && node.getAspects().size() == 1 && node.getAspects().getAmount(Aspect.AIR) == 70
                            && node.getNodeVisBase(Aspect.AIR) == 70, "separate fresh70current/70base AIR-only INPUT, never an already charged/drained old node");
                    harvestArmed = true;
                    // Inside genuine survival reach, outside the real pickup
                    // box even while the elevated node's drops fall/bounce.
                    stand(player, harvestNode.getX() + .5, workbench.getY(), harvestNode.getZ() + 3.8);
                });
                next(Step.HARVEST_AIM);
            }
            case HARVEST_AIM -> {
                if (!serverDone()) return false;
                lookAt(client, Vec3.atCenterOf(harvestNode));
                if (ticks < 12 || !client.player.onGround()) return false;
                var state = client.level.getBlockState(harvestNode);
                require(state.is(ThaumcraftContent.AURA_NODE.get()) && state.getRenderShape() == RenderShape.INVISIBLE
                        && !state.getShape(client.level, harvestNode).isEmpty() && state.getCollisionShape(client.level, harvestNode).isEmpty(), "actual invisible0.3..0.7 target shape, not a fabricated cube/hit");
                require(state.getDestroySpeed(client.level, harvestNode) == 2F && state.getDestroyProgress(client.player, client.level, harvestNode) > 0,
                        "original BlockAiry:115-119 NORMAL hardness2 must be genuinely survival-breakable; never bypass an unbreakable fixture");
                report.put("native_node_target", Map.of("hardness", state.getDestroySpeed(client.level, harvestNode), "outline", state.getShape(client.level, harvestNode).bounds().toString(),
                        "collision_empty", true, "render_shape", "INVISIBLE", "held_tool", "empty hand"));
                next(Step.HARVEST_START);
            }
            case HARVEST_START -> {
                lookAt(client, Vec3.atCenterOf(harvestNode));
                if (!client.isWindowActive()) return false;
                require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(harvestNode, 0), "native survival node reach");
                require(client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(harvestNode), "real first-person ray hits the node's narrow outline");
                client.mouseHandler.grabMouse();
                require(client.mouseHandler.isMouseGrabbed(), "owned native window grabbed for held attack");
                client.options.keyAttack.setDown(true);
                require(client.gameMode.startDestroyBlock(harvestNode, Direction.SOUTH), "native survival node mining started");
                mineTicks = 0;
                next(Step.HARVEST_HELD);
            }
            case HARVEST_HELD -> {
                ++mineTicks;
                lookAt(client, Vec3.atCenterOf(harvestNode));
                // ONE native Minecraft.continueAttack/continueDestroyBlock per
                // held-key tick. A second explicit call would double progress.
                if (!client.level.isEmptyBlock(harvestNode)) return false;
                client.options.keyAttack.setDown(false);
                client.gameMode.stopDestroyBlock();
                next(Step.HARVEST_COMMIT);
            }
            case HARVEST_COMMIT -> {
                if (!serverDone()) return false;
                if (!harvestCommitted) {
                    onServer(client, () -> {
                        var player = serverPlayer(client); var level = player.serverLevel();
                        if (!level.isEmptyBlock(harvestNode)) return;
                        var physical = essenceEntities(level);
                        require(harvestAdds.size() == 8 && harvestAddIds.size() == 8 && new LinkedHashSet<>(harvestAddIds).size() == 8,
                                "original floor(70/10)+1 creates EIGHT separate physical1-count native essence ADD attempts");
                        require(count(player, essence()) == 0 && physical.stream().mapToInt(item -> item.getItem().getCount()).sum() == 8,
                                "eight genuine essence units exist in the world BEFORE any pickup; native item merging is allowed");
                        for (var item : physical) { require(harvestAddIds.contains(item.getUUID()), "physical essence descends from an observed real harvest drop"); requireAirEssence(item.getItem(), level); }
                        harvestCommitted = true; harvestArmed = false;
                        report.put("native_node_harvest", Map.of("server_verified", true, "source_current_and_base_input", Map.of("aer", 70), "inclusive_original_drop_count", 8,
                                "actual_single_item_adds", List.copyOf(harvestAdds), "physical_units_before_pickup", 8,
                                "physical_entity_uuids_after_native_merging", physical.stream().map(item -> item.getUUID().toString()).toList(),
                                "client_held_mining_ticks", mineTicks, "backend_harvest_or_output_calls", 0));
                    });
                    return false;
                }
                if (ticks < 8 || essenceEntities(client.level).stream().mapToInt(item -> item.getItem().getCount()).sum() != 8) return false;
                next(Step.HARVEST_WORLD);
            }
            case HARVEST_WORLD -> {
                lookAt(client, Vec3.atCenterOf(harvestNode).add(0, -.8, 0));
                next(Step.HARVEST_APPROACH);
                capture.accept(CAPTURES.get(0));
            }
            case HARVEST_APPROACH -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var drops = essenceEntities(player.level());
                    require(!drops.isEmpty() && drops.stream().allMatch(item -> harvestAddIds.contains(item.getUUID())), "approach only actual observed harvest outputs");
                    var at = drops.getFirst();
                    stand(player, at.getX(), workbench.getY(), at.getZ());
                });
                next(Step.HARVEST_PICKUP);
            }
            case HARVEST_PICKUP -> {
                if (!serverDone() || ticks < 15) return false;
                if (count(client.player, essence()) != 8) { if (ticks >= 30) next(Step.HARVEST_APPROACH); return false; }
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, essence()) == 8 && essenceEntities(player.level()).isEmpty()
                            && harvestAddIds.stream().allMatch(uuid -> player.serverLevel().getEntity(uuid) == null), "all eight physical harvest units entered real inventory, not a replacement grant");
                    for (var stack : player.getInventory().items) if (stack.is(essence())) requireAirEssence(stack, player.level());
                    harvestVerified = true;
                    report.put("native_essence_pickup", Map.of("physical_units_picked_up", 8, "contained_component_each", Map.of("aer", 2),
                            "actual_resolver_each", Map.of("auram", 2, "aer", 2), "supplied_essences", 0));
                    retained(player);
                });
                next(Step.HARVEST_VERIFIED);
            }
            case HARVEST_VERIFIED -> { if (!serverDone()) return false; next(Step.SALT_TRAVEL); }
            case SALT_TRAVEL -> {
                onServer(client, () -> stand(serverPlayer(client), saltFurnace.getX() + .5, workbench.getY(), saltFurnace.getZ() + 2.6));
                next(Step.SALT_OPEN);
            }
            case SALT_OPEN -> {
                if (!serverDone() || ticks < 10 || !emptyHand(client)) return false;
                use(client, saltFurnace, Direction.SOUTH, south(saltFurnace));
                next(Step.SALT_FUEL_PICK);
            }
            case SALT_FUEL_PICK -> {
                if (ticks < 8 || !(client.screen instanceof FurnaceScreen)) return false;
                for (int slot = 0; slot <= 2; slot++) require(saltMenu(client).getSlot(slot).getItem().isEmpty(), "new ordinary furnace starts truly empty");
                int source = itemSlot(client.player, stack -> stack.is(Items.COAL) && stack.getCount() == 1);
                click(client, vanillaFurnaceSlot(source), 0, ClickType.PICKUP);
                next(Step.SALT_FUEL_PUT);
            }
            case SALT_FUEL_PUT -> {
                if (ticks < 6 || !saltMenu(client).getCarried().is(Items.COAL)) return false;
                require(saltMenu(client).getCarried().getCount() == 1, "one separately declared vanilla furnace fuel INPUT, not the twelve distillation coal");
                click(client, 1, 0, ClickType.PICKUP);
                next(Step.SALT_INPUT_PICK);
            }
            case SALT_INPUT_PICK -> {
                if (ticks < 6 || !saltMenu(client).getCarried().isEmpty()) return false;
                int source = itemSlot(client.player, stack -> stack.is(balanced()) && stack.getCount() == 6);
                click(client, vanillaFurnaceSlot(source), 0, ClickType.PICKUP);
                next(Step.SALT_INPUT_PUT);
            }
            case SALT_INPUT_PUT -> {
                if (ticks < 6 || !saltMenu(client).getCarried().is(balanced())) return false;
                onServer(client, () -> saltArmed = true);
                next(Step.SALT_STARTED);
            }
            case SALT_STARTED -> {
                if (!serverDone()) return false;
                if (!saltStartedAck) {
                    click(client, 0, 0, ClickType.PICKUP);
                    saltStartedAck = true; ticks = 0;
                    return false;
                }
                if (ticks < 8 || !saltMenu(client).getCarried().isEmpty() || !saltMenu(client).getSlot(0).getItem().is(balanced())) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var holder = player.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                            new SingleRecipeInput(new ItemStack(balanced())), player.level()).orElseThrow();
                    require(holder.id().equals(id("salis_mundus_from_smelting_balance_shard")) && holder.value().getCookingTime() == 200
                            && holder.value().getExperience() == 1F, "actual ordinary native smelting recipe:200ticks and XP1, no backend Salis output");
                    report.put("native_salis_recipe", Map.of("recipe", holder.id().toString(), "ticks_each", 200, "xp_each", 1, "inputs", 6,
                            "input_boundary", "separately declared balanced-shard INTERMEDIATE INPUTS; not acquired in this run", "ordinary_fuel_coal_input", 1));
                });
                client.player.closeContainer();
                next(Step.OVEN_TRAVEL);
            }
            case OVEN_TRAVEL -> {
                if (!serverDone()) return false;
                onServer(client, () -> stand(serverPlayer(client), ovenPos.getX() + .5, center.getY(), ovenPos.getZ() + 2.6));
                next(Step.OVEN_OPEN);
            }
            case OVEN_OPEN -> {
                if (!serverDone() || ticks < 10 || !emptyHand(client)) return false;
                use(client, ovenPos, Direction.SOUTH, south(ovenPos));
                next(Step.OVEN_INPUT_PICK);
            }
            case OVEN_INPUT_PICK -> {
                if (ticks < 8 || !(client.screen instanceof GuiAlchemyFurnace)) return false;
                require(ovenMenu(client).getSlot(0).getItem().isEmpty() && ovenMenu(client).getCarried().isEmpty(), "actual old oven input is empty, never cleared/replaced");
                int source = itemSlot(client.player, stack -> stack.is(batch == 0 ? Items.COAL : essence()) && stack.getCount() == (batch == 0 ? 12 : 8));
                if (batch == 1) requireAirEssence(client.player.getInventory().getItem(source), client.level);
                click(client, alchemySlot(source), 0, ClickType.PICKUP);
                next(Step.OVEN_INPUT_PUT);
            }
            case OVEN_INPUT_PUT -> {
                if (ticks < 6 || !ovenMenu(client).getCarried().is(batch == 0 ? Items.COAL : essence())) return false;
                require(ovenMenu(client).getCarried().getCount() == (batch == 0 ? 12 : 8), "finite actual raw/harvested distillation input on real cursor");
                click(client, 0, 0, ClickType.PICKUP);
                onServer(client, () -> batchStarted = serverPlayer(client).serverLevel().getGameTime());
                next(Step.DISTILL_WAIT);
            }
            case DISTILL_WAIT -> {
                if (!serverDone() || ticks < 8 || !ovenMenu(client).getSlot(0).getItem().isEmpty() || ovenMenu(client).getVis() != 0) return false;
                int amount = 0; for (int depth = 1; depth <= 3; depth++) amount += alembic(client.level, depth).getAmount();
                if (amount != (batch == 0 ? 48 : 32)) return false;
                onServer(client, () -> inspectBatch(serverPlayer(client)));
                next(Step.DISTILL_VERIFY);
            }
            case DISTILL_VERIFY -> {
                if (!serverDone()) return false;
                batches.add(snapshot);
                client.player.closeContainer();
                transfer = 0;
                next(Step.PHIAL_POSITION);
            }
            case PHIAL_POSITION -> {
                sourceDepth = 0;
                for (int depth = 1; depth <= 3; depth++) if (alembic(client.level, depth).getAmount() >= PhialData.AMOUNT) { sourceDepth = depth; break; }
                require(sourceDepth != 0 && transfer < (batch == 0 ? 6 : 4), "each phial must be paid by eight REAL typed alembic units");
                sourceBefore = alembic(client.level, sourceDepth).getAmount();
                transferTag = alembic(client.level, sourceDepth).getAspect().getTag();
                require((batch == 0 ? Map.of("potentia", 24, "ignis", 24) : Map.of("auram", 16, "aer", 16)).containsKey(transferTag), "no type clearing/substitution between the two native batches");
                onServer(client, () -> stand(serverPlayer(client), ovenPos.getX() + .5, center.getY(), ovenPos.getZ() + 2.6));
                next(Step.PHIAL_HAND);
            }
            case PHIAL_HAND -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> stack.is(ThaumcraftContent.EMPTY_PHIAL.get()))) return false;
                require(count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) == 0, "the same four saved phials return empty between transfers");
                next(Step.PHIAL_USE);
            }
            case PHIAL_USE -> {
                BlockPos at = ovenPos.above(sourceDepth);
                use(client, at, Direction.SOUTH, south(at));
                next(Step.PHIAL_WITHDRAWN);
            }
            case PHIAL_WITHDRAWN -> {
                if (ticks < 6 || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 3 || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 1
                        || alembic(client.level, sourceDepth).getAmount() != sourceBefore - 8) return false;
                require(client.player.getInventory().getItem(itemSlot(client.player, stack -> filledWith(stack, transferTag))).getCount() == 1, "actual native phial component carries its withdrawn type");
                BlockPos at = newJars.get(transferTag);
                jarBefore = jar(client.level, at).getAmount();
                require(jarBefore == 0 || jar(client.level, at).getAspect() == Aspect.getAspect(transferTag), "only a compatible truly empty/previously filled new jar");
                onServer(client, () -> stand(serverPlayer(client), at.getX() + .5, at.getY(), center.getZ() + 8.4));
                next(Step.JAR_POSITION);
            }
            case JAR_POSITION -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> filledWith(stack, transferTag))) return false;
                next(Step.JAR_USE);
            }
            case JAR_USE -> {
                BlockPos at = newJars.get(transferTag);
                use(client, at, Direction.SOUTH, new Vec3(at.getX() + .5, at.getY() + .4, at.getZ() + .8125));
                next(Step.JAR_FILLED);
            }
            case JAR_FILLED -> {
                var actual = jar(client.level, newJars.get(transferTag));
                if (ticks < 6 || actual.getAmount() != jarBefore + 8 || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 4
                        || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 0) return false;
                require(actual.getAspect() == Aspect.getAspect(transferTag) && actual.getFilter() == null, "native phial establishes the new jar's type, never a supplied label/essentia write");
                phialTransfers.add(Map.of("batch", batch + 1, "transfer", transfer + 1, "aspect", transferTag, "units", 8,
                        "alembic_before", sourceBefore, "alembic_after", sourceBefore - 8, "jar_before", jarBefore, "jar_after", jarBefore + 8,
                        "jar", newJars.get(transferTag).toShortString()));
                if (++transfer < (batch == 0 ? 6 : 4)) { next(Step.PHIAL_POSITION); return false; }
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    emptyDistillation(player.level());
                    require(storedNew(player).equals(produced(batch + 1)), "exact per-type cumulative finite distillation budget after all real phial pairs");
                    retained(player);
                });
                next(Step.BATCH_VERIFY);
            }
            case BATCH_VERIFY -> {
                if (!serverDone()) return false;
                if (++batch < 2) { next(Step.OVEN_TRAVEL); return false; }
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(harvestVerified && count(player, Items.COAL) == 0 && count(player, essence()) == 0 && phialTransfers.size() == 10
                            && storedNew(player).equals(produced(2)), "twelve raw coal plus eight genuine AIR2 essences become80real units through ten actual phial pairs");
                    var pairs = new LinkedHashMap<String, Integer>();
                    for (var entry : phialTransfers) pairs.merge((String) entry.get("aspect"), 1, Integer::sum);
                    require(pairs.equals(Map.of("potentia", 3, "ignis", 3, "auram", 2, "aer", 2)), "exact three+three+two+two native full-phial withdrawal/refill pairs");
                    report.put("native_finite_essentia_supply", Map.of("server_verified", true, "raw_coal_distilled", 12, "physically_harvested_air_essences_distilled", 8,
                            "real_phial_withdrawals", 10, "real_phial_jar_insertions", 10, "produced", produced(2), "total_produced", 80,
                            "old129_essentia_unchanged", true, "new_jars_started_empty_untyped", true, "saved_coal_fuel", fuelState(player.level())));
                    report.put("native_whole_phial_pairs_by_aspect", pairs);
                });
                next(Step.SALT_RETURN);
            }
            case SALT_RETURN -> {
                if (!serverDone()) return false;
                onServer(client, () -> stand(serverPlayer(client), saltFurnace.getX() + .5, workbench.getY(), saltFurnace.getZ() + 2.6));
                next(Step.SALT_RESULT_OPEN);
            }
            case SALT_RESULT_OPEN -> {
                if (!serverDone() || ticks < 10 || !emptyHand(client)) return false;
                use(client, saltFurnace, Direction.SOUTH, south(saltFurnace));
                next(Step.SALT_READY);
            }
            case SALT_READY -> {
                if (!serverDone() || ticks < 8 || !(client.screen instanceof FurnaceScreen) || !saltMenu(client).getSlot(0).getItem().isEmpty()
                        || !saltMenu(client).getSlot(2).getItem().is(salis()) || saltMenu(client).getSlot(2).getItem().getCount() != 6) return false;
                if (!saltPreviewVerified) {
                    onServer(client, () -> {
                        var player = serverPlayer(client); var tile = salt(player.level());
                        require(saltProduced == 6 && tile.getItem(0).isEmpty() && tile.getItem(1).isEmpty() && tile.getItem(2).is(salis())
                                && tile.getItem(2).getCount() == 6 && count(player, salis()) == 0 && count(player, balanced()) == 0,
                                "six real200-tick smelts consumed the six balanced shards and one coal; Salis exists only in native output before extraction");
                        saltXpBefore = player.totalExperience;
                        snapshot = Map.of("server_verified", true, "native_smelts_observed", saltProduced, "ticks_per_recipe", 200, "xp_per_recipe", 1,
                                "physical_result_count", 6, "elapsed_server_ticks_since_first_input_observation", player.serverLevel().getGameTime() - saltFirstInput,
                                "fuel_coal_consumed", 1, "output_grants", 0, "processing_overlapped_distillation_when_chunks_ticked", true);
                        saltPreviewVerified = true;
                    });
                    return false;
                }
                if (!serverDone()) return false;
                next(Step.SALT_PREVIEW);
            }
            case SALT_PREVIEW -> {
                report.put("native_salis_smelting", snapshot);
                next(Step.SALT_TAKE);
                capture.accept(CAPTURES.get(1));
            }
            case SALT_TAKE -> {
                click(client, 2, 0, ClickType.QUICK_MOVE);
                next(Step.SALT_TAKEN);
            }
            case SALT_TAKEN -> {
                if (ticks < 8 || count(client.player, salis()) != 6 || !saltMenu(client).getSlot(2).getItem().isEmpty()) return false;
                next(Step.SALT_XP);
            }
            case SALT_XP -> {
                if (!serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    if (player.totalExperience != saltXpBefore + 6) return; // Real furnace XP orbs must actually reach the player.
                    require(salt(player.level()).isEmpty() && count(player, salis()) == 6 && player.containerMenu.getCarried().isEmpty(), "native output extraction/XP pickup leaves empty ordinary furnace/cursor");
                    saltArmed = false;
                    report.put("native_salis_extraction", Map.of("physical_salis_in_inventory", 6, "xp_before_result", saltXpBefore, "xp_after_native_orb_pickup", player.totalExperience,
                            "actual_xp_gained", player.totalExperience - saltXpBefore, "action", "native FurnaceResultSlot QUICK_MOVE and real XP pickup"));
                });
                next(Step.SALT_VERIFIED);
            }
            case SALT_VERIFIED -> {
                if (!serverDone()) return false;
                if (!report.containsKey("native_salis_extraction")) { next(Step.SALT_XP); return false; }
                client.player.closeContainer();
                next(Step.NUGGET_CRAFT);
            }
            case NUGGET_CRAFT -> beginCraft(client, new CraftPlan("thaumium_nuggets", nugget(), 9, 2,
                    new Item[] {ingot(), null, null, null, null, null, null, null, null}, Map.of(), -1, null), Step.INERT_CRAFT);
            case INERT_CRAFT -> beginCraft(client, new CraftPlan("arcane/wand_cap_thaumium_inert", inert(), 1, 2,
                    new Item[] {nugget(), nugget(), nugget(), nugget(), null, nugget(), null, null, null},
                    Map.of("ordo", 6, "ignis", 6, "aer", 6), 7, CAPTURES.get(2)), Step.OFFER_PREP);
            case CRAFT_TRAVEL -> {
                if (!serverDone() || ticks < 10) return false;
                select(client, 8);
                use(client, workbench, Direction.SOUTH, south(workbench));
                next(Step.CRAFT_OPEN);
            }
            case CRAFT_OPEN -> {
                if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false;
                require(arcane(client).getCarried().isEmpty(), "native crafting starts with empty cursor");
                for (int slot = 1; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "new workbench input/wand slots never supplied or overwritten");
                if (craft.payerHotbar() >= 0) click(client, arcaneSlot(craft.payerHotbar()), 0, ClickType.QUICK_MOVE);
                next(Step.CRAFT_FUND);
            }
            case CRAFT_FUND -> {
                if (ticks < 8) return false;
                if (craft.payerHotbar() >= 0) {
                    if (!ItemStack.matches(arcane(client).getSlot(1).getItem(), expectedPayer())) return false;
                    require(client.player.getInventory().getItem(craft.payerHotbar()).isEmpty(), "actual retained payer moved into native wand slot");
                } else require(arcane(client).getSlot(1).getItem().isEmpty(), "ordinary nugget crafting has no inserted wand");
                gridClicks = recipeClicks(client); gridIndex = 0;
                next(Step.CRAFT_GRID);
            }
            case CRAFT_GRID -> {
                if (ticks < 2) return false;
                if (gridIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; }
                int[] at = gridClicks.get(gridIndex++);
                click(client, at[0], at[1], ClickType.PICKUP); ticks = 0;
            }
            case CRAFT_READY -> {
                var menu = arcane(client);
                if (ticks < 8 || !menu.craftable() || !menu.getSlot(0).getItem().is(craft.output())) return false;
                require(menu.getSlot(0).getItem().getCount() == craft.amount() && menu.getCarried().isEmpty(), "real recipe preview count, no supplied cursor output");
                verifyGrid(menu, craft.repetitions());
                onServer(client, () -> {
                    var player = serverPlayer(client); var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench);
                    var match = ArcaneCrafting.find(player, table);
                    prices = craft.baseVis().isEmpty() ? Map.of() : ArcaneVis.price(craft.baseVis(), table.getItem(10), player);
                    require(match != null && match.recipe().id().equals(id(craft.recipe())) && match.output().is(craft.output()) && match.output().getCount() == craft.amount()
                            && match.arcane() == !craft.baseVis().isEmpty() && match.prices().equals(prices), "actual native RecipeManager match and original modifier-priced craft");
                    var entry = new LinkedHashMap<String, Object>();
                    entry.put("recipe", match.recipe().id().toString()); entry.put("base_vis_each", craft.baseVis()); entry.put("actual_prices_each_centivis", prices);
                    entry.put("result_each", craft.amount()); entry.put("repetitions", craft.repetitions());
                    if (craft.payerHotbar() >= 0) {
                        require(ItemStack.matches(table.getItem(10), expectedPayer()), "preview owns the genuine saved payer");
                        entry.put("saved_payer_before_centivis", ItemWandCasting.data(table.getItem(10)).vis());
                    }
                    if (craft.recipe().equals("arcane/wand_assembly")) {
                        require(isReward(match.output()) && ItemWandCasting.data(match.output()).vis().isEmpty()
                                && ItemWandCasting.getMaxVis(match.output()) == 10000 && match.output().getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), -1) == 54,
                                "new Thaumium/Silverwood preview is EMPTY100vis with original craft-cost54, paid by actual old100vis GOLD wand");
                        require(ItemWandCasting.getMaxVis(table.getItem(10)) == 10000 && PRIMALS.stream().allMatch(tag -> craft.baseVis().get(tag) == 54), "greatwood50 cannot pay54; the saved Silverwood100 payer really funds this assembly");
                    }
                    snapshot = entry;
                });
                next(Step.CRAFT_PREVIEW);
            }
            case CRAFT_PREVIEW -> {
                if (!serverDone()) return false;
                for (String tag : PRIMALS) require(arcane(client).requiredVis(tag) == prices.getOrDefault(tag, 0), "client native menu matches authoritative actual price for " + tag);
                report.put("preview_" + craft.recipe(), snapshot);
                next(Step.CRAFT_TAKE);
                if (craft.capture() != null) capture.accept(craft.capture());
            }
            case CRAFT_TAKE -> {
                outputBefore = count(client.player, craft.output());
                click(client, 0, 0, ClickType.PICKUP);
                next(Step.CRAFT_OUTPUT);
            }
            case CRAFT_OUTPUT -> {
                var menu = arcane(client);
                if (ticks < 6 || !menu.getCarried().is(craft.output())) return false;
                require(menu.getCarried().getCount() == craft.amount(), "one real native result extraction at a time");
                if (craft.payerHotbar() >= 0) {
                    ItemStack expected = expectedPayer().copy();
                    var data = ItemWandCasting.data(expected);
                    for (var price : prices.entrySet()) data = data.withAmount(price.getKey(), data.amount(price.getKey()) - price.getValue());
                    expected.set(ThaumcraftContent.WAND_DATA.get(), data); // ASSERTION COPY ONLY; never installed in any inventory/world.
                    require(ItemStack.matches(menu.getSlot(1).getItem(), expected), "actual native result paid exactly the preview price once, preserving every other component");
                    if (craft.payerHotbar() == 7) expectedGreatwood = expected; else expectedSilverwood = expected;
                }
                verifyGrid(menu, craft.repetitions() - crafted - 1);
                cursorOutput = menu.getCarried().copy();
                if (craft.recipe().equals("arcane/wand_assembly")) require(isReward(cursorOutput) && ItemWandCasting.data(cursorOutput).vis().isEmpty(), "genuine assembled output starts empty, not topped up");
                destination = outputDestination(client.player, cursorOutput);
                next(Step.CRAFT_STORE);
            }
            case CRAFT_STORE -> {
                click(client, arcaneSlot(destination), 0, ClickType.PICKUP);
                next(Step.CRAFT_STORED);
            }
            case CRAFT_STORED -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, craft.output()) != outputBefore + craft.amount()) return false;
                if (++crafted < craft.repetitions()) { next(Step.CRAFT_TAKE); return false; }
                if (craft.payerHotbar() >= 0) {
                    click(client, 1, 0, ClickType.PICKUP); // Actual wand slot normalizes SWAP buttons: use its cursor path.
                    next(Step.CRAFT_WAND_PICK);
                } else next(Step.CRAFT_RETURNED);
            }
            case CRAFT_WAND_PICK -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), expectedPayer())) return false;
                require(client.player.getInventory().getItem(craft.payerHotbar()).isEmpty(), "payer's physical return slot remains empty");
                click(client, arcaneSlot(craft.payerHotbar()), 0, ClickType.PICKUP);
                next(Step.CRAFT_WAND_STORE);
            }
            case CRAFT_WAND_STORE -> {
                if (ticks < 6 || !ItemStack.matches(client.player.getInventory().getItem(craft.payerHotbar()), expectedPayer()) || !arcane(client).getCarried().isEmpty()) return false;
                next(Step.CRAFT_RETURNED);
            }
            case CRAFT_RETURNED -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "native extraction/return leaves whole persistent table and cursor empty");
                    if (craft.output() == nugget()) require(count(player, ingot()) == 0 && count(player, nugget()) == 18, "two ordinary ingot-to-nine crafts produce18 actual nuggets");
                    if (craft.output() == inert()) require(count(player, inert()) == 2 && count(player, nugget()) == 8, "two paid inert-cap crafts consume ten actual nuggets, retaining eight");
                    if (craft.recipe().equals("arcane/wand_assembly")) require(count(player, cap()) == 0 && count(player, rod()) == 0 && count(player, ThaumcraftContent.WAND.get()) == 3,
                            "diagonal assembly really consumes both charged caps and separately declared core; neither old wand was dismantled/deleted");
                    retained(player);
                    var entry = new LinkedHashMap<String, Object>(snapshot);
                    entry.put("server_verified", true); entry.put("native_result_extractions", crafted); entry.put("actual_output_units", crafted * craft.amount());
                    if (craft.payerHotbar() >= 0) entry.put("saved_payer_after_centivis", ItemWandCasting.data(expectedPayer()).vis());
                    crafts.add(entry);
                });
                client.player.closeContainer();
                next(Step.CRAFT_VERIFIED);
            }
            case CRAFT_VERIFIED -> { if (!serverDone() || ticks < 8) return false; next(afterCraft); }
            case OFFER_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(completedInfusions < 2 && count(player, inert()) == 2 - completedInfusions && count(player, salis()) == 6 - completedInfusions * 3,
                            "each real cap requires one actual inert cap and THREE separate actual smelted Salis");
                    require(storedNew(player).equals(leftovers(completedInfusions)), "finite80-unit source budget before next infusion");
                    emptyDistillation(player.level()); retained(player);
                });
                offerIndex = 0;
                next(Step.OFFER_POSITION);
            }
            case OFFER_POSITION -> {
                if (!serverDone()) return false;
                BlockPos at = offers.get(offerIndex);
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(pedestal(player.level(), at).isEmpty(), "cannot overwrite any real altar item/output");
                    stand(player, at.getX() + .5, center.getY(), at.getZ() + (offerIndex == 0 ? 2.6 : 1.8));
                });
                next(Step.OFFER_HAND);
            }
            case OFFER_HAND -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> stack.is(offerIndex == 0 ? inert() : salis()))) return false;
                countBefore = count(client.player, offerIndex == 0 ? inert() : salis());
                next(Step.OFFER_USE);
            }
            case OFFER_USE -> {
                use(client, offers.get(offerIndex), Direction.SOUTH, pedestalPoint(offers.get(offerIndex)));
                next(Step.OFFER_WAIT);
            }
            case OFFER_WAIT -> {
                var actual = pedestal(client.level, offers.get(offerIndex)).getItem(0); Item item = offerIndex == 0 ? inert() : salis();
                if (ticks < 6 || !actual.is(item) || actual.getCount() != 1 || count(client.player, item) != countBefore - 1) return false;
                if (++offerIndex < offers.size()) { next(Step.OFFER_POSITION); return false; }
                next(Step.START_PREP);
            }
            case START_PREP -> {
                onServer(client, () -> armInfusion(serverPlayer(client)));
                next(Step.START_AIM);
            }
            case START_AIM -> {
                if (!serverDone() || ticks < 10) return false;
                select(client, 7); lookAt(client, south(matrixPos));
                next(Step.START_USE);
            }
            case START_USE -> {
                use(client, matrixPos, Direction.SOUTH, south(matrixPos));
                next(Step.START_WAIT);
            }
            case START_WAIT -> {
                if (ticks < 8 || !matrix(client.level).isCrafting() || paidUnits < 1) return false;
                // A camera future must not outrun the real C2S start packet.
                onServer(client, () -> viewAltar(serverPlayer(client)));
                next(completedInfusions == 0 ? Step.PAID_WORLD : Step.INFUSION_WAIT);
            }
            case PAID_WORLD -> {
                if (!serverDone() || ticks < 10 || !matrix(client.level).isCrafting() || paidUnits < 2 || paidUnits >= 18 || !lastPaidAspect.equals("auram")) return false;
                lookAt(client, Vec3.atCenterOf(center).add(0, 1.1, 0));
                report.put("native_paid_phase_capture", Map.of("actual_paid_units_at_request", paidUnits, "remaining_demand", aspects(matrix(client.level).getDemand()),
                        "actual_symmetry", matrix(client.level).getSymmetry(), "actual_instability", matrix(client.level).getInstability(),
                        "last_actually_paid_aspect", lastPaidAspect,
                        "real_auram_source", "harvested AIR2 essence -> original baseauram2+containedaer2 resolver -> native distillation/phials/new upper jar"));
                next(Step.INFUSION_WAIT);
                capture.accept(CAPTURES.get(3));
            }
            case INFUSION_WAIT -> {
                if (!serverDone() || !infusionFinished) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(paidUnits == 18 && paidComponents == 3 && !matrix(level).isCrafting() && pedestal(level, center).getItem(0).is(cap())
                            && pedestal(level, center).getItem(0).getCount() == 1 && storedNew(player).equals(leftovers(completedInfusions + 1)),
                            "genuine18one-unit payments and three consumed Salis yield one real charged cap");
                    var entry = new LinkedHashMap<String, Object>();
                    entry.put("server_verified", true); entry.put("craft", completedInfusions + 1); entry.put("recipe", id("infusion/wand_cap_thaumium").toString());
                    entry.put("paid_essentia", capCost()); entry.put("real_one_unit_drains", paidUnits); entry.put("real_salis_consumptions", paidComponents);
                    entry.put("actual_payments", List.copyOf(payments)); entry.put("actual_component_payments", List.copyOf(componentPayments));
                    entry.put("remaining_new_essentia", leftovers(completedInfusions + 1)); entry.put("old_essentia_retained", 129);
                    entry.put("stabilization", "actual saved72paired candles,opposite occupied pedestal pair+one unpaired; native symmetry-6/I-1; no RNG/stability writes");
                    infusions.add(entry); retained(player);
                    stand(player, center.getX() + .5, center.getY(), center.getZ() + 2.6);
                });
                next(Step.CAP_WORLD);
            }
            case CAP_WORLD -> {
                if (!serverDone() || ticks < 12 || !pedestal(client.level, center).getItem(0).is(cap())) return false;
                lookAt(client, Vec3.atCenterOf(center).add(0, .8, 0));
                next(Step.CAP_EMPTY_HAND);
                if (completedInfusions == 1) {
                    require(count(client.player, cap()) == 1, "first real charged cap is retained while the second is physically displayed on its pedestal");
                    report.put("native_pair_before_second_pickup", Map.of("first_real_charged_cap_in_inventory", count(client.player, cap()), "second_real_cap_on_pedestal", 1,
                            "actual_total_paid_essentia", 36, "actual_salis_consumed", 6));
                    capture.accept(CAPTURES.get(4));
                }
            }
            case CAP_EMPTY_HAND -> {
                if (!emptyHand(client)) return false;
                onServer(client, () -> { capDropId = null; capDropStack = null; capDropAdds = 0; ejectionArmed = true; });
                next(Step.CAP_EJECT);
            }
            case CAP_EJECT -> {
                if (!serverDone() || ticks < 6) return false;
                select(client, 8);
                use(client, center, Direction.SOUTH, pedestalPoint(center));
                capPickupVerified = false;
                next(Step.CAP_PICKUP);
            }
            case CAP_PICKUP -> {
                if (!serverDone() || ticks < 12) return false;
                if (capPickupVerified) { next(Step.CAP_VERIFIED); return false; }
                if (count(client.player, cap()) != completedInfusions + 1 || !pedestal(client.level, center).isEmpty()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(capDropAdds == 1 && capDropId != null && capDropStack != null && capDropStack.getCount() == 1
                            && player.serverLevel().getEntity(capDropId) == null && count(player, cap()) == completedInfusions + 1,
                            "one observed actual pedestal ItemEntity per paid cap was physically picked up, never granted");
                    var entry = infusions.get(completedInfusions);
                    entry.put("native_output_drop_uuid", capDropId.toString()); entry.put("physical_pickup_verified", true);
                    ejectionArmed = false; ++completedInfusions; capPickupVerified = true;
                    retained(player);
                });
            }
            case CAP_VERIFIED -> {
                if (!serverDone()) return false;
                if (completedInfusions < 2) next(Step.OFFER_PREP); else next(Step.PAYER_PREP);
            }
            case PAYER_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(completedInfusions == 2 && count(player, cap()) == 2 && count(player, inert()) == 0 && count(player, salis()) == 0
                            && count(player, nugget()) == 8 && storedNew(player).equals(leftovers(2)), "both genuine cap outputs precede any new wand assembly");
                    freshNode(player.serverLevel(), payerNode, primalNode(80), "finite NORMAL six-primal charging INPUT for the actual old Silverwood/gold54vis payer");
                    report.put("retained_after_two_infusions", Map.of("new_surplus", leftovers(2), "new_surplus_total", 44, "old_jar_essentia", 129,
                            "combined_retained_essentia", 173, "retained_nuggets", 8, "old_oven_coal", fuelState(player.level())));
                });
                chargingPayer = true;
                next(Step.CHARGE_POSITION);
            }
            case CHARGE_POSITION -> {
                if (!serverDone()) return false;
                if (!hand(client, chargingPayer ? stack -> ItemStack.matches(stack, expectedSilverwood) : ThaumiumCapsClientSequence::isReward)) return false;
                BlockPos at = chargingPayer ? payerNode : fireNode;
                onServer(client, () -> stand(serverPlayer(client), at.getX() + .5, workbench.getY(), at.getZ() + 2.6));
                next(Step.CHARGE_AIM);
            }
            case CHARGE_AIM -> {
                if (!serverDone()) return false;
                BlockPos at = chargingPayer ? payerNode : fireNode;
                lookAt(client, Vec3.atCenterOf(at));
                if (ticks < 12) return false;
                onServer(client, () -> chargeProbe = new ChargeProbe(serverPlayer(client), at));
                next(Step.CHARGE_ARM);
            }
            case CHARGE_ARM -> { if (!serverDone()) return false; next(Step.CHARGE_START); }
            case CHARGE_START -> {
                lookAt(client, Vec3.atCenterOf(chargingPayer ? payerNode : fireNode));
                client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "real native held-use starts finite-node charging");
                next(Step.CHARGE_HELD);
            }
            case CHARGE_HELD -> {
                lookAt(client, Vec3.atCenterOf(chargingPayer ? payerNode : fireNode));
                ItemStack actual = client.player.getMainHandItem();
                boolean funded = chargingPayer ? PRIMALS.stream().allMatch(tag -> ItemWandCasting.getVis(actual, tag) >= 5400)
                        : ItemWandCasting.getVis(actual, "ignis") >= 200 && ItemWandCasting.getVis(actual, "perditio") >= 200;
                if (!funded) return false;
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                next(Step.CHARGE_STOPPED);
            }
            case CHARGE_STOPPED -> {
                if (!serverDone() || ticks < 12 || client.player.isUsingItem()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    charges.add(chargeProbe.finish());
                    if (chargingPayer) {
                        require(isWand(player.getMainHandItem(), "silverwood", "gold") && PRIMALS.stream().allMatch(tag -> ItemWandCasting.getVis(player.getMainHandItem(), tag) >= 5400),
                                "actual saved100vis GOLD payer natively charged enough for54each primal, not a raw greatwood top-up");
                        expectedSilverwood = player.getMainHandItem().copy();
                    } else require(isReward(player.getMainHandItem()) && ItemWandCasting.getVis(player.getMainHandItem(), "ignis") >= 200
                            && ItemWandCasting.getVis(player.getMainHandItem(), "ignis") < 1000 && ItemWandCasting.getVis(player.getMainHandItem(), "perditio") < 1000,
                            "new EMPTY reward only received a small finite genuine Fire/Entropy node charge");
                    retained(player);
                });
                next(chargingPayer ? Step.ASSEMBLY_CRAFT : Step.FOCUS_PRESS);
            }
            case ASSEMBLY_CRAFT -> {
                if (!serverDone()) return false;
                beginCraft(client, new CraftPlan("arcane/wand_assembly", ThaumcraftContent.WAND.get(), 1, 1,
                        new Item[] {null, null, cap(), null, rod(), null, cap(), null, null}, primalCost(54), 8, CAPTURES.get(5)), Step.REWARD_PREP);
            }
            case REWARD_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var reward = player.getInventory().getItem(itemSlot(player, ThaumiumCapsClientSequence::isReward));
                    require(ItemWandCasting.data(reward).vis().isEmpty() && ItemWandCasting.getMaxVis(reward) == 10000
                            && reward.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), -1) == 54 && count(player, nugget()) == 8 && count(player, rod()) == 0,
                            "physical new Thaumium/Silverwood wand is empty100vis/craftcost54 after consuming the two actual charged caps and declared core");
                    report.put("native_empty_wand_reward", Map.of("actual_snbt", reward.save(player.registryAccess()).toString(), "capacity_centivis", 10000,
                            "craft_cost_component", 54, "vis_before_charging", ItemWandCasting.data(reward).vis(), "old_wands_retained", 2,
                            "core_boundary", "one separately declared Silverwood rod INTERMEDIATE INPUT; old wand not disassembled; old63-unit story not replayed"));
                });
                next(Step.REWARD_HAND);
            }
            case REWARD_HAND -> {
                if (!serverDone() || !hand(client, ThaumiumCapsClientSequence::isReward)) return false;
                report.put("native_bare_reward_presentation", presentation(client));
                next(Step.FIRE_NODE_PREP);
            }
            case FIRE_NODE_PREP -> {
                onServer(client, () -> freshNode(serverPlayer(client).serverLevel(), fireNode, new AspectList().add(Aspect.FIRE, 4).add(Aspect.ENTROPY, 4),
                        "separate small finite NORMAL4ignis+4perditio INPUT for native new-wand charging; not a wand component top-up"));
                chargingPayer = false;
                next(Step.CHARGE_POSITION);
            }
            case FOCUS_PRESS -> {
                if (!serverDone() || ticks < 10 || !client.isWindowActive()) return false;
                require(isReward(client.player.getMainHandItem()) && FocusData.get(client.player.getMainHandItem()).isEmpty()
                        && count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && ItemStack.matches(client.player.getOffhandItem(), apple), "actual saved loose Fire Focus and apple before native held F");
                var key = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F);
                require(ThaumcraftFocusClient.CHANGE_FOCUS.matches(GLFW.GLFW_KEY_F, 0) && ThaumcraftFocusClient.CHANGE_FOCUS.same(client.options.keySwapOffhand), "real default shared F binding");
                KeyMapping.click(key);
                ThaumcraftFocusClient.CHANGE_FOCUS.setDown(true); client.options.keySwapOffhand.setDown(true);
                next(Step.FOCUS_WAIT);
            }
            case FOCUS_WAIT -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); if (client.screen != null) client.screen.onClose(); next(Step.FOCUS_PRESS); return false; }
                if (ticks < 8 || !(client.screen instanceof GuiFocusSelect)) return false;
                require(!client.screen.isPauseScreen(), "real held-F radial is native and non-pausing");
                next(Step.FOCUS_SELECT);
            }
            case FOCUS_SELECT -> {
                if (!client.isWindowActive()) { releaseFocusKey(client); if (client.screen != null) client.screen.onClose(); next(Step.FOCUS_PRESS); return false; }
                require(client.screen instanceof GuiFocusSelect, "actual native F radial owns the selection");
                var buttons = (List<?>) BookClientSmoke.field(client.screen, "focusButtons");
                require(buttons.size() == 1, "only the actual retained Fire Focus is offered");
                var button = (AbstractWidget) buttons.getFirst();
                int x = button.getX() + button.getWidth() / 2, y = button.getY() + button.getHeight() / 2;
                var radial = client.screen;
                radial.mouseMoved(x, y);
                require(radial.mouseClicked(x, y, 0), "native radial mouse handler sends real source-slot intent");
                radial.mouseReleased(x, y, 0);
                releaseFocusKey(client);
                next(Step.FOCUS_EQUIPPED);
            }
            case FOCUS_EQUIPPED -> {
                if (ticks < 10 || !FocusData.get(client.player.getMainHandItem()).is(ThaumcraftFoci.FIRE_FOCUS.get()) || count(client.player, ThaumcraftFoci.FIRE_FOCUS.get()) != 0) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(isReward(player.getMainHandItem()) && ItemStack.matches(FocusData.get(player.getMainHandItem()), originalFocus)
                            && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 0 && ItemStack.matches(player.getOffhandItem(), apple), "native server equipped the SAME saved focus, never swapped the apple or injected a component");
                    var cow = EntityType.COW.create(player.serverLevel());
                    require(cow != null, "ordinary native cow INPUT fixture");
                    cow.setNoAi(true); cow.setPersistenceRequired();
                    cow.setCustomName(Component.literal("Native Thaumium-cap Fire target")); cow.setCustomNameVisible(true);
                    cow.moveTo(arena.getX() + .5, arena.getY(), arena.getZ() + .5, 180, 0);
                    require(player.serverLevel().addFreshEntity(cow) && cow.getHealth() == cow.getMaxHealth() && !cow.isOnFire(), "only an undamaged/unignited ordinary no-AI living target is supplied");
                    targetId = cow.getId();
                    inputs.add(Map.of("fixture", "ordinary no-AI cow INPUT", "uuid", cow.getUUID().toString(), "position", arena.toShortString()));
                    stand(player, arena.getX() + .5, arena.getY(), arena.getZ() + 3.5);
                    report.put("native_focus_equip", Map.of("path", "actual held-F GuiFocusSelect -> native button -> server source-slot intent",
                            "same_saved_focus_snbt", originalFocus.save(player.registryAccess()).toString(), "loose_focus_after", 0, "offhand_apple_unchanged", true));
                    retained(player);
                });
                next(Step.CAST_POSITION);
            }
            case CAST_POSITION -> {
                if (!serverDone() || ticks < 12 || !(client.level.getEntity(targetId) instanceof Cow cow)) return false;
                lookAt(client, cow.getBoundingBox().getCenter());
                next(Step.CAST_AIM);
            }
            case CAST_AIM -> {
                lookAt(client, client.level.getEntity(targetId).getBoundingBox().getCenter());
                if (ticks < 10) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(!player.isUsingItem() && WandMaterials.capModifier("thaumium") == .9F && ItemFocusFire.tickCost(player.getMainHandItem(), player) == 9
                            && ItemFocusFire.tickCost(expectedSilverwood, player) == 10, "real no-penalty actor: new Thaumium9centivis/tick versus saved gold10, not just registered config");
                    fireProbe = new FireProbe(player, (Cow) player.serverLevel().getEntity(targetId));
                });
                next(Step.CAST_START);
            }
            case CAST_START -> {
                if (!serverDone() || !client.isWindowActive()) return false;
                lookAt(client, client.level.getEntity(targetId).getBoundingBox().getCenter());
                castActive = true; releaseSent = false; castClientTicks = 0;
                ClientTickEvent.CLIENT_PRE.register(castGuard);
                client.options.keyUse.setDown(true);
                require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "one actual native RMB/useItem starts new focused-wand use");
                next(Step.CAST_LIVE);
            }
            case CAST_LIVE -> {
                var cow = (Cow) client.level.getEntity(targetId);
                lookAt(client, cow.getBoundingBox().getCenter());
                var frame = fireProbe.frame;
                var embers = client.level.getEntitiesOfClass(EntityEmber.class, client.player.getBoundingBox().inflate(8));
                if (frame.paidTicks() < 5 || !frame.onFire() || frame.health() >= fireProbe.healthBefore || !cow.isOnFire() || cow.getHealth() >= fireProbe.healthBefore || embers.isEmpty()) {
                    require(!releaseSent, "bounded native cast ended before an actual ember-hit/ignition scene; never fake damage/fire/cast ticks");
                    return false;
                }
                require(client.screen == null && client.player.isUsingItem() && frame.using() && cow.isAlive(), "native live capture contains actually using new wand, real embers and damaged/burning live cow");
                report.put("native_live_fire_capture", Map.of("paid_ticks_at_observation", frame.paidTicks(), "ignis_debit_centivis", frame.debit(), "per_paid_tick", 9,
                        "client_live_embers", embers.size(), "server_live_embers", frame.liveEmbers(), "actual_target_health", frame.health(), "actual_target_burning", frame.onFire(),
                        "precise_target_health_asserted", false, "native_reward_presentation", presentation(client)));
                next(Step.CAST_STOP);
                capture.accept(CAPTURES.get(6));
            }
            case CAST_STOP -> {
                releaseCast(client);
                castActive = false; ClientTickEvent.CLIENT_PRE.unregister(castGuard);
                next(Step.CAST_STOPPED);
            }
            case CAST_STOPPED -> {
                if (client.player.isUsingItem() || fireProbe.frame.using() || fireProbe.frame.stoppedTicks() < 2) return false;
                onServer(client, () -> {
                    report.put("native_nine_centivis_fire", fireProbe.finish());
                    finalState(serverPlayer(client));
                    stopObservation();
                });
                next(Step.FINAL_VERIFY);
            }
            case FINAL_VERIFY -> {
                if (!serverDone()) return false;
                require(nativeTicks == null && nativeAdds == null && serverStopped == null && !ClientTickEvent.CLIENT_PRE.isRegistered(castGuard), "all temporary native observers/guard removed");
                report.put("native_client_ticks", totalTicks);
                next(Step.DONE);
            }
            case DONE -> { releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        var level = player.serverLevel();
        require(workbench.equals(center.offset(80, 0, 0)), "root must use accepted M07 workbench+96X and ACTUAL existing center, not a new/rebuilt altar");
        require(player.getInventory().getItem(8).isEmpty() && ItemStack.matches(player.getInventory().getItem(handoffSlot), handoff), "native saved-wand handoff committed");
        expectedSilverwood = handoff.copy(); expectedGreatwood = player.getInventory().getItem(7).copy(); apple = player.getOffhandItem().copy();
        require(isWand(expectedGreatwood, "greatwood", "gold") && isWand(expectedSilverwood, "silverwood", "gold") && count(player, ThaumcraftContent.WAND.get()) == 2
                && FocusData.get(expectedGreatwood).isEmpty() && FocusData.get(expectedSilverwood).isEmpty(), "both real unfocused saved wands, never reconstructed or topped up");
        require(player.getInventory().getItem(6).is(ThaumcraftContent.THAUMONOMICON.get()) && apple.is(Items.APPLE) && apple.getCount() == 1
                && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0
                && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get()) == 6
                && count(player, ThaumcraftContent.MATERIALS.get("quicksilver_drop").get()) == 8 && count(player, ThaumcraftInfusion.PEDESTAL_ITEM.get()) == 1,
                "actual M07 book/apple/phials/focus/tubes/drops/spare pedestal inputs must survive");
        originalFocus = player.getInventory().getItem(itemSlot(player, stack -> stack.is(ThaumcraftFoci.FIRE_FOCUS.get()))).copy();
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty() && !stack.is(ThaumcraftContent.WAND.get())
                && !stack.is(ThaumcraftFoci.FIRE_FOCUS.get()) && !stack.is(ThaumcraftContent.EMPTY_PHIAL.get())) retainedStacks.add(stack.copy());
        for (ItemStack stack : player.getArmorSlots()) armorBefore.add(stack.copy());
        for (Item item : List.of(Items.COAL, ingot(), nugget(), inert(), cap(), salis(), balanced(), rod(), essence()))
            require(count(player, item) == 0, "new declared inputs/outputs cannot mask or consume an existing retained stack: " + itemId(item));
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 12) >> 4; x++)
            for (int z = (center.getZ() - 12) >> 4; z <= (center.getZ() + 12) >> 4; z++) level.getChunk(x, z);
        require(candles.size() == 72 && matrix(level).isActive() && !matrix(level).isCrafting() && InfusionStructure.isValid(level, matrixPos), "reuse saved active/formed altar, never recraft/reform/inject activation");
        var protectedPositions = new ArrayList<BlockPos>(candles); protectedPositions.addAll(peripheral); protectedPositions.add(center); protectedPositions.add(matrixPos);
        for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) for (int dy = 0; dy <= 1; dy++) protectedPositions.add(center.offset(x, dy, z));
        for (BlockPos at : protectedPositions) altarBlocks.put(at, level.getBlockState(at));
        for (BlockPos at : candles) require(level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get()), "all72actual saved stabilizer candles, not replacement fixtures");
        for (BlockPos at : peripheral) require(pedestal(level, at).isEmpty(), "all eight actual peripheral pedestals empty after accepted M07");
        require(pedestal(level, center).isEmpty(), "real center is empty after accepted physical core extraction");
        for (int i = 0; i < OLD_JAR_TAGS.size(); i++) {
            BlockPos at = center.offset(-7 + i * 2, 0, -8); var jar = jar(level, at);
            require(jar != null && jar.getAspect() != Aspect.AURA && jar.getAspect() != Aspect.ENERGY, "old jars cannot supply/conflict with newly earned auram/potentia");
            oldJars.put(at, jarState(jar));
        }
        require(oldJars.values().stream().mapToInt(value -> (Integer) value.get("amount")).sum() == 129, "actual saved129essentia before new unit");
        emptyDistillation(level);
        require(oven(level).getItem(1).is(Items.COAL) && oven(level).getItem(1).getCount() == 5 && !oven(level).isSpeedBoost(), "actual five saved normal coal, no injected burn time/boost");
        oldCoalBefore = oven(level).getItem(1).getCount(); oldBurnBefore = oven(level).getBurnTime();
        var savedJars = new ArrayList<Map<String, Object>>();
        for (var entry : oldJars.entrySet()) {
            var saved = new LinkedHashMap<String, Object>(entry.getValue());
            saved.put("position", entry.getKey().toShortString()); savedJars.add(saved);
        }
        report.put("actual_saved_inputs_before_new_fixtures", Map.of("acquisition_replayed", false,
                "main_inventory_stacks_snbt", player.getInventory().items.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.save(player.registryAccess()).toString()).toList(),
                "offhand_snbt", apple.save(player.registryAccess()).toString(), "old_jars", savedJars, "old_essentia_total", 129,
                "old_oven_coal", oldCoalBefore, "actual_candles", candles.size(), "actual_placed_pedestals", peripheral.size() + 1, "actual_matrix_already_active", true));
        patch(level, workbench.offset(-5, -2, -4), workbench.offset(11, 5, 13));
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        level.setBlockAndUpdate(saltFurnace, Blocks.FURNACE.defaultBlockState());
        // Only new EMPTY cells south of both saved candle rings. Never patch the altar.
        var stoneSupports = new LinkedHashSet<BlockPos>();
        for (BlockPos at : newJars.values()) for (int dy = 1; dy <= 4; dy++) stoneSupports.add(at.below(dy));
        for (int x = -8; x <= 8; x++) for (int z = 8; z <= 10; z++) stoneSupports.add(center.offset(x, 3, z));
        for (BlockPos at : stoneSupports) require(level.isEmptyBlock(at) && level.getBlockEntity(at) == null, "new support/platform cannot replace ANY old block/BE: " + at);
        for (BlockPos at : newJars.values()) require(level.isEmptyBlock(at) && level.getBlockEntity(at) == null, "new jar cell must be genuinely empty: " + at);
        for (BlockPos at : stoneSupports) level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
        for (BlockPos at : newJars.values()) {
            level.setBlockAndUpdate(at, ThaumcraftContent.JAR.get().defaultBlockState());
            require(jar(level, at).getAmount() == 0 && jar(level, at).getAspect() == null && jar(level, at).getFilter() == null, "new jars start empty/untyped/unlabelled");
        }
        var coalTags = ObjectAspectResolver.get(new ItemStack(Items.COAL), level);
        require(coalTags.visSize() == 4 && coalTags.getAmount(Aspect.ENERGY) == 2 && coalTags.getAmount(Aspect.FIRE) == 2, "actual original rawcoal2potentia+2ignis resolver");
        player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false);
        supply(player, ingot(), 2, "DECLARED INTERMEDIATE INPUT; accepted metallurgy not replayed; two ordinary nine-nugget crafts");
        supply(player, rod(), 1, "DECLARED INTERMEDIATE INPUT Silverwood core; no invented wand disassembly/no replay of old63-unit recipe");
        supply(player, balanced(), 6, "DECLARED INTERMEDIATE INPUT; actual vanilla200-tick smelting must earn all six Salis");
        supply(player, Items.COAL, 12, "finite RAW DISTILLATION INPUT; not fuel and not supplied potentia/ignis");
        supply(player, Items.COAL, 1, "separate finite ordinary Salis furnace FUEL INPUT; saved five coal remain in actual old alchemy oven");
        var researchBefore = new LinkedHashMap<String, Boolean>();
        for (String key : List.of("THAUMIUM", "CAP_thaumium")) { researchBefore.put(key, KnowledgeAccess.get(player).isResearchComplete(key)); KnowledgeAccess.get(player).addResearch(key); }
        require(KnowledgeAccess.get(player).isResearchComplete("INFUSION") && KnowledgeAccess.get(player).isResearchComplete("NODEPRESERVE"), "actual accepted M07 research retained, not replayed");
        KnowledgeEvents.sync(player);
        freshNode(level, harvestNode, new AspectList().add(Aspect.AIR, 70), "fresh distinct NORMAL AIR-only70current/70base raw node INPUT, never charged/drained before survival harvest");
        player.inventoryMenu.broadcastChanges();
        report.put("research_input_fixture", Map.of("keys", List.of("THAUMIUM", "CAP_thaumium"), "already_complete_before_fixture", researchBefore, "old_progression_replayed", false));
        report.put("saved_wands_before", Map.of("greatwood_gold_snbt", expectedGreatwood.save(player.registryAccess()).toString(), "silverwood_gold_snbt", expectedSilverwood.save(player.registryAccess()).toString(),
                "greatwood_vis", ItemWandCasting.data(expectedGreatwood).vis(), "silverwood_vis", ItemWandCasting.data(expectedSilverwood).vis(), "top_ups", 0));
        report.put("empty_fixtures", Map.of("new_workbench", 1, "new_ordinary_furnace", 1, "new_empty_untyped_jars", 4,
                "checked_empty_stone_support_cells", stoneSupports.size(), "saved_oven_and_alembics_reused_without_clearing", true));
        retained(player);
        startObservation(player);
    }

    private void armInfusion(ServerPlayer player) {
        var level = player.serverLevel();
        var holder = level.getRecipeManager().byKey(id("infusion/wand_cap_thaumium")).orElseThrow();
        require(holder.value() instanceof InfusionStackRecipe, "actual native STACK infusion recipe loaded");
        var recipe = (InfusionStackRecipe) holder.value();
        List<ItemStack> components = peripheral.stream().map(at -> pedestal(level, at).getItem(0)).filter(stack -> !stack.isEmpty()).toList();
        require(components.size() == 3 && components.stream().allMatch(stack -> stack.is(salis()) && stack.getCount() == 1)
                && recipe.components().size() == 3 && recipe.matches(new InfusionRecipeInput(pedestal(level, center).getItem(0), components), level)
                && recipe.aspectCost().visSize() == 18 && aspects(recipe.aspectCost()).equals(capCost()) && recipe.instability() == 5
                && recipe.research().equals("CAP_thaumium"), "exact original inert center+THREE separate Salis,12potentia+6auram/I5; never a fourth ingredient");
        require(matrix(level).isActive() && !matrix(level).isCrafting() && storedNew(player).equals(leftovers(completedInfusions)), "actual formed idle altar/full finite paid source budget");
        var surroundings = InfusionStructure.scan(level, matrixPos);
        require(surroundings.pedestals().size() == 8 && surroundings.symmetry() == -6, "opposite pair+one unpaired has pedestal symmetry1; actual72candles make native total-6");
        paidUnits = 0; paidComponents = 0; lastDemand = 18; lastComponents = 3;
        lastDemandByAspect = capCost(); lastPaidAspect = "";
        lastPayment = -1; lastComponent = -1; payments.clear(); componentPayments.clear();
        infusionStarted = false; infusionFinished = false; infusionArmed = true;
        stand(player, center.getX() + .5, center.getY(), center.getZ() + 2.6);
    }

    /** Scoped passive POST/ADD/shutdown observers, not calls into any gameplay tick. */
    private void startObservation(ServerPlayer player) {
        require(nativeTicks == null && nativeAdds == null && serverStopped == null, "one native observer set, never duplicate collision/tick delivery");
        observerStarted = player.serverLevel().getGameTime();
        nativeAdds = (entity, level) -> {
            if (level != player.level() || observationFailure != null) return EventResult.pass();
            try {
                if (harvestArmed && entity instanceof ItemEntity item && item.getItem().is(essence()) && item.distanceToSqr(Vec3.atCenterOf(harvestNode)) < 16) {
                    require(item.getItem().getCount() == 1, "original harvest emits each essence as a separate physical1-count stack");
                    requireAirEssence(item.getItem(), player.level());
                    harvestAddIds.add(item.getUUID());
                    harvestAdds.add(Map.of("uuid", item.getUUID().toString(), "count_at_native_add", 1, "actual_snbt", item.getItem().save(player.registryAccess()).toString(),
                            "contained", Map.of("aer", 2), "actual_resolver", Map.of("auram", 2, "aer", 2)));
                }
                if (ejectionArmed && entity instanceof ItemEntity item && item.getItem().is(cap()) && item.distanceToSqr(player) < 16) {
                    ++capDropAdds; capDropId = item.getUUID(); capDropStack = item.getItem().copy();
                }
                if (fireProbe != null && fireProbe.active && entity instanceof EntityEmber ember && ember.getOwner() == player) fireProbe.observeAdd(ember);
            } catch (RuntimeException | Error error) { observationFailure = error; }
            return EventResult.pass(); // Observation never cancels/replays a spawn or invokes collision.
        };
        nativeTicks = server -> {
            if (server != player.server) return;
            try {
                require(observationFailure == null, "native ADD observer failed: " + observationFailure);
                require(!player.isRemoved() && player.isAlive() && player.serverLevel().getGameTime() - observerStarted <= 20000, "scoped caps observer expired/player left");
                if (saltArmed) observeSalt(player);
                if (infusionArmed) observeInfusion(player);
                if (chargeProbe != null && chargeProbe.active) chargeProbe.observe();
                if (fireProbe != null && fireProbe.active) fireProbe.observe();
            } catch (RuntimeException | Error error) {
                observationFailure = error;
                report.put("native_observer_failure", Map.of("message", String.valueOf(error.getMessage()), "paid_units", paidUnits,
                        "paid_components", paidComponents, "completed_infusions", completedInfusions, "harvest_single_adds", harvestAdds.size()));
                stopObservation();
            }
        };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        EntityEvent.ADD.register(nativeAdds); TickEvent.SERVER_POST.register(nativeTicks); LifecycleEvent.SERVER_STOPPED.register(serverStopped);
    }

    private void observeSalt(ServerPlayer player) {
        var tile = salt(player.level());
        int input = tile.getItem(0).isEmpty() ? 0 : tile.getItem(0).getCount();
        int output = tile.getItem(2).isEmpty() ? 0 : tile.getItem(2).getCount();
        if (!saltObserved) {
            if (input == 0) return;
            require(input == 6 && output == 0 && tile.getItem(0).is(balanced()), "real native first Salis input baseline");
            saltObserved = true; saltFirstInput = player.serverLevel().getGameTime(); saltLastInput = 6;
        }
        if (saltProduced == 6) return; // Native result extraction is not another smelt.
        require(input == 0 || tile.getItem(0).is(balanced()), "native ordinary smelter retains only the declared balanced input");
        require(output == 0 || tile.getItem(2).is(salis()), "native ordinary result is real Salis, never a fixture output");
        int consumed = saltLastInput - input;
        require(consumed == 0 || consumed == 1, "native furnace consumes at most one balanced shard per real tick");
        require(output - saltLastOutput == consumed && input + output == 6, "six-input physical furnace conservation before extraction");
        if (consumed == 1) {
            long now = player.serverLevel().getGameTime();
            require(now - (saltProduced == 0 ? saltFirstInput : saltLastProduct) >= (saltProduced == 0 ? 199 : 200), "each actual Salis smelt takes its native200-tick recipe duration, never accelerated/backend ticked");
            ++saltProduced; saltLastProduct = now;
        }
        saltLastInput = input; saltLastOutput = output;
    }

    private void observeInfusion(ServerPlayer player) {
        var level = player.serverLevel(); var matrix = matrix(level);
        if (!infusionStarted) {
            if (!matrix.isCrafting()) return;
            infusionStarted = true;
            require(matrix.getRecipeInstability() == 5 && matrix.getSymmetry() == -6 && matrix.getInstability() == -1 && matrix.getPedestalPositions().size() == 8,
                    "native actual72candles/three-of-eight occupancy starts I5-6=-1; no forced state/seed");
        }
        int demand = matrix.getDemand().visSize(), delta = lastDemand - demand;
        require(delta == 0 || delta == 1, "each actual native infusion tick pays zero or ONE unit");
        String paidAspect = null;
        var currentDemand = new LinkedHashMap<String, Integer>();
        for (String tag : capCost().keySet()) {
            int amount = matrix.getDemand().getAmount(Aspect.getAspect(tag));
            int difference = lastDemandByAspect.get(tag) - amount;
            require(difference == 0 || difference == 1, "per-type native demand only decreases by zero/one");
            if (difference == 1) { require(paidAspect == null, "only one aspect paid per actual tick"); paidAspect = tag; }
            currentDemand.put(tag, amount);
        }
        require((paidAspect == null) == (delta == 0), "actual typed payment agrees with total native demand delta");
        lastDemandByAspect = currentDemand;
        if (delta == 1) {
            require(matrix.getTickCount() % 10 == 0 && (lastPayment < 0 || level.getGameTime() - lastPayment == 10), "actual original ten-tick one-unit cadence");
            lastPayment = level.getGameTime(); lastPaidAspect = paidAspect; ++paidUnits;
            payments.add(Map.of("unit", paidUnits, "aspect", paidAspect, "server_game_time", lastPayment, "native_matrix_tick", matrix.getTickCount(), "remaining_demand", demand));
        }
        lastDemand = demand;
        var expected = new LinkedHashMap<>(leftovers(completedInfusions));
        for (var cost : capCost().entrySet()) expected.put(cost.getKey(), expected.get(cost.getKey()) - cost.getValue() + matrix.getDemand().getAmount(Aspect.getAspect(cost.getKey())));
        require(storedNew(player).equals(expected) && storedNew(player).values().stream().mapToInt(Integer::intValue).sum() == 80 - completedInfusions * 18 - paidUnits,
                "every real infusion tick conserves all four newly produced types; old129are separate and unchanged");
        require(ItemStack.matches(player.getInventory().getItem(7), expectedGreatwood), "native already-formed start/cycles cost no extra wand vis");
        int remaining = matrix.getRemainingIngredientCount(), componentDelta = lastComponents - remaining;
        require(componentDelta == 0 || componentDelta == 1, "native infusion consumes one real peripheral at a time");
        if (componentDelta == 1) {
            require(paidUnits == 18 && demand == 0 && matrix.getTickCount() % 10 == 0
                    && level.getGameTime() - (lastComponent < 0 ? lastPayment : lastComponent) == 60, "all18units paid before each separate Salis uses six actual ten-tick cycles");
            lastComponent = level.getGameTime(); ++paidComponents;
            componentPayments.add(Map.of("component", paidComponents, "server_game_time", lastComponent, "native_matrix_tick", matrix.getTickCount(), "remaining_components", remaining));
        }
        lastComponents = remaining;
        require(peripheral.stream().filter(at -> !pedestal(level, at).isEmpty()).count() == remaining, "actual three physical Salis and native remaining multiset agree");
        if (matrix.isCrafting()) require(matrix.getInstability() == -1 && pedestal(level, center).getItem(0).is(inert()), "fully funded genuine craft retains inert center until completion");
        else {
            require(paidUnits == 18 && paidComponents == 3 && demand == 0 && pedestal(level, center).getItem(0).is(cap()), "native paid session finishes with actual charged cap on center");
            infusionFinished = true; infusionArmed = false;
        }
        retainedWorld(level);
    }

    private void stopObservation() {
        if (nativeTicks != null) { TickEvent.SERVER_POST.unregister(nativeTicks); nativeTicks = null; }
        if (nativeAdds != null) { EntityEvent.ADD.unregister(nativeAdds); nativeAdds = null; }
        if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; }
        ClientTickEvent.CLIENT_PRE.unregister(castGuard);
        if (chargeProbe != null) chargeProbe.active = false;
        if (fireProbe != null) fireProbe.active = false;
    }

    /** Observe actual native wand/node deltas, including any ordinary600-tick source regeneration. */
    private static final class ChargeProbe {
        final ServerPlayer player;
        final BlockPos at;
        final ItemStack wand;
        final WandData before;
        final Map<String, Integer> sourceBefore, regenerated = new LinkedHashMap<>();
        final long started;
        WandData previous;
        Map<String, Integer> sourcePrevious;
        boolean active = true, usingObserved;
        int stoppedTicks, actualTransfers;

        ChargeProbe(ServerPlayer player, BlockPos at) {
            this.player = player; this.at = at;
            wand = player.getMainHandItem(); before = ItemWandCasting.data(wand); previous = before;
            sourceBefore = aspects(node(player.level(), at).getAspects()); sourcePrevious = sourceBefore;
            started = player.serverLevel().getGameTime();
            require(!player.isUsingItem() && node(player.level(), at).getNodeType() == TileNode.NodeType.NORMAL, "native charge observer baseline, no active item/session injection");
        }

        void observe() {
            require(player.getMainHandItem() == wand && player.serverLevel().getGameTime() - started <= 4200, "bounded charge keeps same actual main-hand stack");
            var current = ItemWandCasting.data(wand); var source = aspects(node(player.level(), at).getAspects());
            int regeneratedThisTick = 0;
            for (String tag : PRIMALS) {
                int delta = current.amount(tag) - previous.amount(tag);
                require(delta >= 0 && delta % 100 == 0 && (sourceBefore.containsKey(tag) || delta == 0), "real finite source funds only present primals in whole100-centivis units");
                int restored = source.getOrDefault(tag, 0) - sourcePrevious.getOrDefault(tag, 0) + delta / 100;
                require(restored >= 0 && restored <= 1, "no injected source refill; account ordinary native node regeneration separately");
                if (restored > 0) regenerated.merge(tag, restored, Integer::sum);
                regeneratedThisTick += restored; actualTransfers += delta / 100;
            }
            require(regeneratedThisTick <= 1 && current.rod().equals(before.rod()) && current.cap().equals(before.cap()) && current.capacity() == before.capacity(), "native charging preserves materials and has at most one natural restored unit per tick");
            if (player.isUsingItem()) { require(player.getUseItem() == wand, "native active node use owns actual wand"); usingObserved = true; }
            stoppedTicks = usingObserved && !player.isUsingItem() ? stoppedTicks + 1 : 0;
            previous = current; sourcePrevious = source;
        }

        Map<String, Object> finish() {
            require(usingObserved && !player.isUsingItem() && stoppedTicks >= 2 && actualTransfers > 0, "real release acknowledged after native finite node transfers");
            var source = node(player.level(), at);
            for (var entry : sourceBefore.entrySet()) require(source.getNodeVisBase(Aspect.getAspect(entry.getKey())) == entry.getValue()
                    && source.getAspects().getAmount(Aspect.getAspect(entry.getKey())) >= 1, "NODEPRESERVE retains finite node/base without a reset");
            active = false;
            return Map.of("server_verified", true, "entry", "native useItem/held node use/RELEASE_USE_ITEM", "source_input_position", at.toShortString(),
                    "source_current_and_base_before", sourceBefore, "source_actual_current_after", aspects(source.getAspects()), "native_regenerated_units_observed", Map.copyOf(regenerated),
                    "actual_vis_units_transferred", actualTransfers, "wand_before_centivis", before.vis(), "wand_after_centivis", ItemWandCasting.data(wand).vis(),
                    "elapsed_native_server_ticks", player.serverLevel().getGameTime() - started);
        }
    }

    private record FireFrame(int paidTicks, int debit, int liveEmbers, float health, boolean onFire, boolean using, int stoppedTicks) {}

    /** Read-only END_TICK/ADD measurement; never call a focus tick or an ember collision. */
    private static final class FireProbe {
        final ServerPlayer player;
        final Cow target;
        final ItemStack wand;
        final WandData before;
        final float healthBefore;
        final int duration;
        final List<Map<String, Object>> payments = new ArrayList<>();
        volatile FireFrame frame;
        boolean active = true, started;
        int previousVis, paidTicks, adds, nativeElapsed, stoppedTicks, peakLive;

        FireProbe(ServerPlayer player, Cow target) {
            this.player = player; this.target = target; wand = player.getMainHandItem();
            before = ItemWandCasting.data(wand); previousVis = before.amount("ignis"); healthBefore = target.getHealth(); duration = wand.getUseDuration(player);
            require(isReward(wand) && FocusData.get(wand).is(ThaumcraftFoci.FIRE_FOCUS.get()) && healthBefore == target.getMaxHealth() && !target.isOnFire(), "actual focused reward/unmodified ordinary target baseline");
            frame = new FireFrame(0, 0, 0, healthBefore, false, false, 0);
        }

        void observeAdd(EntityEmber ember) {
            require(ember.getDamage() == 2F && ember.getDuration() == 20 && ember.getFireSeconds() == 3 && ember.getScatter() == 15, "actual basic Fire-created ember retains original stats");
            ++adds;
        }

        void observe() {
            require(player.getMainHandItem() == wand && isReward(wand) && ItemFocusFire.tickCost(wand, player) == 9
                    && FocusData.get(wand).is(ThaumcraftFoci.FIRE_FOCUS.get()), "real using stack is same newly assembled Thaumium wand, canonical actual9centivis/tick");
            var current = ItemWandCasting.data(wand);
            for (String tag : PRIMALS) if (!tag.equals("ignis")) require(current.amount(tag) == before.amount(tag), "basic Fire cannot debit Entropy or another primal");
            int delta = previousVis - current.amount("ignis");
            require(delta == 0 || delta == 9, "EACH real native server tick debits either zero or EXACTLY9, never a recorded/injected bulk debit");
            boolean using = player.isUsingItem();
            if (using) {
                require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND, "native active-use state owns the genuine new wand");
                started = true; nativeElapsed = duration - player.getUseItemRemainingTicks();
                require(nativeElapsed == paidTicks + (delta == 9 ? 1 : 0), "independent native elapsed-use counter matches actual paid ticks");
            }
            if (delta == 9) {
                require(using, "actual payment happens within native active item use");
                ++paidTicks;
                payments.add(Map.of("paid_tick", paidTicks, "native_elapsed_use_ticks", nativeElapsed, "before", previousVis, "after", current.amount("ignis"),
                        "server_game_time", player.serverLevel().getGameTime()));
            }
            require(adds == paidTicks * 2, "two actual native ember ADD attempts per ONE paid tick; no duplicate collision delivery");
            int live = player.serverLevel().getEntitiesOfClass(EntityEmber.class, player.getBoundingBox().inflate(8), ember -> ember.getOwner() == player).size();
            peakLive = Math.max(peakLive, live); stoppedTicks = started && !using && paidTicks > 0 ? stoppedTicks + 1 : 0;
            previousVis = current.amount("ignis");
            frame = new FireFrame(paidTicks, before.amount("ignis") - previousVis, live, target.getHealth(), target.isOnFire(), using, stoppedTicks);
        }

        Map<String, Object> finish() {
            int debit = before.amount("ignis") - ItemWandCasting.getVis(wand, "ignis");
            require(!player.isUsingItem() && stoppedTicks >= 2 && paidTicks >= 5 && debit == paidTicks * 9 && debit % 9 == 0
                    && nativeElapsed == paidTicks && adds == paidTicks * 2 && peakLive > 0, "actual9-per-tick vis delta, independent native use counter and real spawn attempts agree after release");
            require(target.getHealth() < healthBefore && target.isOnFire(), "real ember hit/ignition, not injected damage/fire; exact health is deliberately not asserted across burn ticks");
            active = false;
            var result = new LinkedHashMap<String, Object>();
            result.put("server_verified", true); result.put("entry", "native RMB -> ItemWandCasting.onUseTick -> basic Fire -> real ember collision");
            result.put("release", "native RELEASE_USE_ITEM and two stable post-release server ticks"); result.put("paid_ticks", paidTicks);
            result.put("per_tick_centivis", 9); result.put("old_gold_reference_centivis", 10); result.put("independent_native_elapsed_use_ticks", nativeElapsed);
            result.put("ignis_before", before.amount("ignis")); result.put("ignis_after", ItemWandCasting.getVis(wand, "ignis")); result.put("exact_debit", debit);
            result.put("actual_payments", List.copyOf(payments)); result.put("observed_native_ember_add_attempts", adds); result.put("peak_live_embers", peakLive);
            result.put("other_primals_unchanged", true); result.put("target", Map.of("uuid", target.getUUID().toString(), "ordinary_no_ai_fixture", true,
                    "health_before", healthBefore, "actual_health_after", target.getHealth(), "actual_fire_ticks", target.getRemainingFireTicks(), "precise_health_assertion", false, "injected_damage_or_fire", false));
            return result;
        }
    }

    private void inspectBatch(ServerPlayer player) {
        var level = player.serverLevel(); var typed = new LinkedHashMap<String, Integer>();
        require(oven(level).getItem(0).isEmpty() && oven(level).getVis() == 0 && !oven(level).isSpeedBoost(), "normal native distillation fully consumed batch, no injected buffer/boost");
        for (int depth = 1; depth <= 3; depth++) {
            var tile = alembic(level, depth);
            require(tile.getFilter() == null && tile.getAmount() >= 0 && tile.getAmount() <= 32, "actual saved unlabelled32-capacity alembic");
            merge(typed, tile.getAspect(), tile.getAmount());
        }
        var expected = batch == 0 ? Map.of("potentia", 24, "ignis", 24) : Map.of("auram", 16, "aer", 16);
        require(typed.equals(expected) && storedNew(player).equals(produced(batch + 1)), "native finite batch exactly matches original resolver, no type writes/clears/loss");
        snapshot = Map.of("server_verified", true, "batch", batch + 1, "input", batch == 0 ? "twelve declared raw coal" : "eight physically harvested AIR2 wisp essences",
                "actual_typed_alembics", typed, "elapsed_native_server_ticks", level.getGameTime() - batchStarted, "saved_fuel", fuelState(level));
    }

    private void finalState(ServerPlayer player) {
        retained(player); emptyDistillation(player.level());
        require(completedInfusions == 2 && count(player, cap()) == 0 && count(player, inert()) == 0 && count(player, salis()) == 0 && count(player, rod()) == 0
                && count(player, ingot()) == 0 && count(player, balanced()) == 0 && count(player, essence()) == 0 && count(player, nugget()) == 8
                && count(player, ThaumcraftContent.WAND.get()) == 3 && isReward(player.getMainHandItem()), "exact new native cap/assembly inputs consumed, eight nuggets and three genuine wands retained");
        require(storedNew(player).equals(leftovers(2)) && storedNew(player).values().stream().mapToInt(Integer::intValue).sum() == 44, "all44new surplus plus129old essentia remain physical, jars never destroyed");
        for (BlockPos at : peripheral) require(pedestal(player.level(), at).isEmpty(), "three actual Salis consumed and five balancing pedestals still empty");
        require(pedestal(player.level(), center).isEmpty() && matrix(player.level()).isActive() && !matrix(player.level()).isCrafting() && salt(player.level()).isEmpty(), "reused altar/empty machines are retained, not reset/removed");
        report.put("native_net_outputs", Map.of("physically_harvested_essences_then_distilled", 8, "native_salis_smelts_then_consumed", 6, "native_inert_caps_then_infused", 2,
                "genuine_charged_caps_then_assembled", 2, "new_native_empty100vis_thaumium_silverwood_wands", 1, "retained_old_wands", 2,
                "retained_nuggets", 8, "new_surplus_essentia", leftovers(2), "old_essentia_unchanged", 129, "saved_oven_fuel_final", fuelState(player.level())));
        report.put("final_saved_payers", Map.of("greatwood_gold_snbt", expectedGreatwood.save(player.registryAccess()).toString(),
                "silverwood_gold_snbt", expectedSilverwood.save(player.registryAccess()).toString(), "direct_vis_top_ups", 0));
        report.put("retained_prior_outputs", Map.of("server_verified", true, "unrelated_saved_inventory_counts_and_components_exact", true,
                "old_wands_retained_with_only_actual_reported_payments_and_charging", true, "offhand_apple_unchanged", true, "empty_saved_phials", 4,
                "actual_saved_focus", "same original focus now natively equipped on the new wand", "old_jar_essentia_unchanged", 129,
                "old_altar_blocks_unchanged", altarBlocks.size(), "old_acquisition_replayed", false));
        report.put("final_native_reward_snbt", player.getMainHandItem().save(player.registryAccess()).toString());
    }

    private void retained(Player player) {
        require(findExact(player, expectedGreatwood) && findExact(player, expectedSilverwood) && ItemStack.matches(player.getOffhandItem(), apple), "both actual old wand components/paid vis and offhand apple retained");
        for (ItemStack original : retainedStacks) {
            int before = retainedStacks.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, original)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, original)).mapToInt(ItemStack::getCount).sum();
            require(after == before, "all prior unrelated item counts/components retained exactly, never consumed/replaced: " + original);
        }
        int armor = 0; for (ItemStack actual : player.getArmorSlots()) require(ItemStack.matches(actual, armorBefore.get(armor++)), "prior armor slots unchanged");
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0, "all four saved physical phials returned empty at boundaries");
        int foci = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ThaumcraftFoci.FIRE_FOCUS.get())) { require(ItemStack.matches(stack, originalFocus), "same saved loose focus components"); foci += stack.getCount(); }
            if (stack.is(ThaumcraftContent.WAND.get()) && !FocusData.get(stack).isEmpty()) {
                require(isReward(stack) && ItemStack.matches(FocusData.get(stack), originalFocus), "only the new native reward may hold the same actual saved focus"); ++foci;
            }
        }
        require(foci == 1, "one conserved actual saved Fire Focus, loose or natively equipped, never supplied again");
        retainedWorld(player.level());
    }

    private void retainedWorld(Level level) {
        for (var entry : oldJars.entrySet()) require(jarState(jar(level, entry.getKey())).equals(entry.getValue()), "all129old essentia/type/filter states unchanged: " + entry.getKey());
        for (var entry : altarBlocks.entrySet()) require(level.getBlockState(entry.getKey()) == entry.getValue(), "saved candle/pedestal/formed-pillar/matrix blocks never replaced: " + entry.getKey());
        int remaining = oven(level).getItem(1).isEmpty() ? 0 : oven(level).getItem(1).getCount();
        require(remaining >= 0 && remaining <= oldCoalBefore && (remaining == 0 || oven(level).getItem(1).is(Items.COAL)), "only actual finite saved normal coal may fuel the old oven");
    }

    private Map<String, Object> fuelState(Level level) {
        int remaining = oven(level).getItem(1).isEmpty() ? 0 : oven(level).getItem(1).getCount();
        return Map.of("saved_coal_before", oldCoalBefore, "actual_saved_coal_after", remaining, "native_coal_ignitions", oldCoalBefore - remaining,
                "actual_burn_time_before", oldBurnBefore, "actual_burn_time_after", oven(level).getBurnTime(), "supplied_extra_alchemy_fuel", 0);
    }

    private void emptyDistillation(Level level) {
        require(oven(level) != null && oven(level).getItem(0).isEmpty() && oven(level).getVis() == 0, "actual empty old oven buffer/input, never cleared by fixture");
        for (int depth = 1; depth <= 3; depth++) require(alembic(level, depth) != null && alembic(level, depth).getAmount() == 0
                && alembic(level, depth).getAspect() == null && alembic(level, depth).getFilter() == null, "native phial-empty saved alembic, no clearing/type injection");
    }

    private Map<String, Integer> storedNew(Player player) {
        var result = new LinkedHashMap<String, Integer>();
        for (String tag : List.of("potentia", "ignis", "auram", "aer")) result.put(tag, 0);
        var level = player.level(); var buffered = oven(level).getAspects();
        for (Aspect type : buffered.getAspects()) merge(result, type, buffered.getAmount(type));
        for (int depth = 1; depth <= 3; depth++) merge(result, alembic(level, depth).getAspect(), alembic(level, depth).getAmount());
        for (BlockPos at : newJars.values()) merge(result, jar(level, at).getAspect(), jar(level, at).getAmount());
        for (ItemStack stack : player.getInventory().items) if (stack.is(ThaumcraftContent.FILLED_PHIAL.get())) {
            require(ItemEssence.data(stack) != null, "actual filled-phial component required for finite accounting");
            merge(result, Aspect.getAspect(ItemEssence.data(stack).tag()), stack.getCount() * PhialData.AMOUNT);
        }
        return result;
    }

    private void freshNode(ServerLevel level, BlockPos at, AspectList aspects, String purpose) {
        require(level.isEmptyBlock(at) && level.getBlockEntity(at) == null, "declared new node cannot replace an old node/block/BE: " + at);
        for (BlockPos nearby : BlockPos.betweenClosed(at.offset(-4, -4, -4), at.offset(4, 4, 4)))
            require(!level.hasChunkAt(nearby) || !(level.getBlockEntity(nearby) instanceof TileNode), "finite input must be isolated from native node bullying; never delete/alter a nearby existing node: " + nearby);
        level.setBlockAndUpdate(at, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
        node(level, at).setAspects(aspects); // Declared finite NODE INPUT only, never a wand/essence/output write.
        require(node(level, at).getNodeType() == TileNode.NodeType.NORMAL, "only bounded NORMAL node fixtures");
        inputs.add(Map.of("fixture", "finite NORMAL node INPUT", "position", at.toShortString(), "current_and_base", aspects(aspects), "purpose", purpose,
                "naturally_found_claimed", false));
    }

    private static void patch(ServerLevel level, BlockPos min, BlockPos max) {
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)))).isEmpty(), "fresh bounded patch cannot erase physical old outputs");
        for (BlockPos at : BlockPos.betweenClosed(min, max)) require(level.getBlockEntity(at) == null
                && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft"), "refuse ANY previous BE/node/Thaumcraft block in new floor patch: " + at);
        int floorY = min.getY() + 2;
        for (BlockPos at : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(at, at.getY() < floorY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }

    private void supply(ServerPlayer player, Item item, int amount, String purpose) {
        int slot = emptyMain(player);
        require(amount > 0 && amount <= item.getDefaultInstance().getMaxStackSize(), "finite declared input stack");
        player.getInventory().setItem(slot, new ItemStack(item, amount));
        inputs.add(Map.of("item", itemId(item), "count", amount, "purpose", purpose, "genuinely_empty_main_slot", slot));
    }

    private void beginCraft(Minecraft client, CraftPlan plan, Step following) {
        require(serverDone(), "native craft cannot overlap server fixture/inspection");
        client.setScreen(null); craft = plan; afterCraft = following; crafted = 0;
        onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6));
        next(Step.CRAFT_TRAVEL);
    }

    /** Literal native menu clicks only; recipe matching/payment/output remains entirely in the actual container. */
    private List<int[]> recipeClicks(Minecraft client) {
        var result = new ArrayList<int[]>(); var groups = new LinkedHashMap<Item, List<Integer>>();
        for (int cell = 0; cell < 9; cell++) if (craft.grid()[cell] != null) groups.computeIfAbsent(craft.grid()[cell], ignored -> new ArrayList<>()).add(cell + 2);
        for (var group : groups.entrySet()) {
            int needed = group.getValue().size() * craft.repetitions();
            int inventory = itemSlot(client.player, stack -> stack.is(group.getKey()) && stack.getCount() >= needed), source = arcaneSlot(inventory);
            result.add(new int[] {source, 0});
            for (int cell : group.getValue()) for (int n = 0; n < craft.repetitions(); n++) result.add(new int[] {cell, 1});
            if (client.player.getInventory().getItem(inventory).getCount() > needed) result.add(new int[] {source, 0});
        }
        return result;
    }

    private void verifyGrid(ContainerArcaneWorkbench menu, int amount) {
        for (int cell = 0; cell < 9; cell++) {
            Item expected = craft.grid()[cell]; var actual = menu.getSlot(cell + 2).getItem();
            require(expected == null || amount == 0 ? actual.isEmpty() : actual.is(expected) && actual.getCount() == amount, "exact native recipe grid after " + crafted + " extracts: " + craft.recipe() + "/" + cell);
        }
    }

    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside machine menus");
        if (!wanted.test(client.player.getInventory().getItem(8))) {
            int source = itemSlot(client.player, wanted);
            require(source != 6 && source != 7, "action hand cannot displace saved book/greatwood payer");
            click(client, inventorySlot(source), 8, ClickType.SWAP); handChangedAt = client.player.tickCount;
            return false;
        }
        if (client.player.tickCount - handChangedAt < 6) return false;
        select(client, 8); return true;
    }

    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "empty hand created only by native inventory SWAP");
        if (!client.player.getInventory().getItem(8).isEmpty()) {
            click(client, inventorySlot(emptyMain(client.player)), 8, ClickType.SWAP); handChangedAt = client.player.tickCount;
            return false;
        }
        if (client.player.tickCount - handChangedAt < 6) return false;
        select(client, 8); return true;
    }

    private Map<String, Object> presentation(Minecraft client) {
        var actual = client.player.getMainHandItem(); require(isReward(actual), "probe only the actual newly assembled wand");
        var model = client.getItemRenderer().getModel(actual, client.level, client.player, 0);
        require(model != client.getModelManager().getMissingModel(), "native component-driven new wand model exists");
        var sprites = new LinkedHashSet<String>();
        for (var quad : model.getQuads(null, null, RandomSource.create(0))) sprites.add(quad.getSprite().contents().name().toString());
        for (Direction face : Direction.values()) for (var quad : model.getQuads(null, face, RandomSource.create(0))) sprites.add(quad.getSprite().contents().name().toString());
        report.put("latest_native_wand_model_probe", Map.of("actual_sprites", List.copyOf(sprites), "focus_equipped", !FocusData.get(actual).isEmpty()));
        require(sprites.contains("thaumcraft:models/wand_rod_silverwood") && sprites.contains("thaumcraft:models/wand_cap_thaumium"), "actual native new wand selects Silverwood rod and PURPLE Thaumium cap, not clamped gold/greatwood fallback");
        return Map.of("actual_sprites", List.copyOf(sprites), "capacity_centivis", ItemWandCasting.getMaxVis(actual), "tooltip", actual.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList(),
                "visual_inspection", "pending exact capture; model resolution is not full visual parity");
    }

    private void guardCast(Minecraft client) {
        if (!castActive || releaseSent || client.player == null) return;
        // Capture writing suspends the sequence, NOT this bounded native input
        // release. Never directly stop the server, tick focus, or insert embers.
        if (++castClientTicks >= 20 || fireProbe.frame.paidTicks() >= 14) releaseCast(client);
    }

    private void releaseCast(Minecraft client) {
        if (releaseSent) return;
        client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); releaseSent = true;
    }

    private void releaseInputs(Minecraft client) {
        castActive = false; ClientTickEvent.CLIENT_PRE.unregister(castGuard);
        client.options.keyUse.setDown(false); client.options.keyShift.setDown(false); client.options.keyAttack.setDown(false); releaseFocusKey(client);
        if (client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
            if (client.player != null && client.player.isUsingItem()) client.gameMode.releaseUsingItem(client.player);
        }
        if (previousCamera != null) client.options.setCameraType(previousCamera);
    }

    private static void releaseFocusKey(Minecraft client) { ThaumcraftFocusClient.CHANGE_FOCUS.setDown(false); client.options.keySwapOffhand.setDown(false); }
    private static void stand(ServerPlayer player, double x, double y, double z) {
        AABB body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3);
        require(player.level().noCollision(player, body), "declared camera/player position cannot collide with saved candles/pedestals/jars/terrain: " + body);
        require(!player.level().getBlockState(BlockPos.containing(x, y - .01, z)).getCollisionShape(player.level(), BlockPos.containing(x, y - .01, z)).isEmpty(), "declared camera/player stands on a real solid floor/platform");
        player.teleportTo(x, y, z);
    }
    private void viewAltar(ServerPlayer player) { stand(player, center.getX() + 9.2, center.getY(), center.getZ() - 10.2); }
    private void requireAirEssence(ItemStack stack, Level level) {
        require(stack.is(essence()) && stack.has(ThaumcraftEssences.WISP_ESSENCE_DATA.get()) && ItemWispEssence.data(stack) != null, "actual harvested item carries native WISP_ESSENCE_DATA, not a generic phial/model");
        var contained = ((ItemWispEssence) stack.getItem()).getAspects(stack);
        require(contained != null && contained.size() == 1 && contained.getAmount(Aspect.AIR) == 2, "actual essence container data is AIR2");
        var resolved = ObjectAspectResolver.get(stack, level);
        require(resolved.visSize() == 4 && resolved.getAmount(Aspect.AIR) == 2 && resolved.getAmount(Aspect.AURA) == 2, "real resolver combines base AURA2 with contained AIR2, no cull bypass/output aspects injected");
    }
    private static Map<String, Integer> produced(int batches) { return Map.of("potentia", batches > 0 ? 24 : 0, "ignis", batches > 0 ? 24 : 0, "auram", batches > 1 ? 16 : 0, "aer", batches > 1 ? 16 : 0); }
    private static Map<String, Integer> leftovers(int crafts) { var result = new LinkedHashMap<>(produced(2)); capCost().forEach((tag, amount) -> result.put(tag, result.get(tag) - amount * crafts)); return result; }
    private static Map<String, Integer> capCost() { return Map.of("potentia", 12, "auram", 6); }
    private static Map<String, Integer> primalCost(int amount) { var result = new LinkedHashMap<String, Integer>(); for (String tag : PRIMALS) result.put(tag, amount); return result; }
    private static AspectList primalNode(int amount) { var result = new AspectList(); for (String tag : PRIMALS) result.add(Aspect.getAspect(tag), amount); return result; }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect type : list.getAspects()) if (list.getAmount(type) > 0) result.put(type.getTag(), list.getAmount(type)); return result; }
    private static void merge(Map<String, Integer> target, Aspect aspect, int amount) { require(amount >= 0 && (amount == 0 || aspect != null), "nonnegative typed physical essentia"); if (amount > 0) target.merge(aspect.getTag(), amount, Integer::sum); }
    private static Map<String, Object> jarState(TileJar jar) { return Map.of("amount", jar.getAmount(), "aspect", jar.getAspect() == null ? "" : jar.getAspect().getTag(), "filter", jar.getFilter() == null ? "" : jar.getFilter().getTag()); }
    private static boolean filledWith(ItemStack stack, String tag) { return stack.is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(stack) != null && ItemEssence.data(stack).tag().equals(tag); }
    private static boolean isWand(ItemStack stack, String rod, String cap) { return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals(rod) && ItemWandCasting.data(stack).cap().equals(cap); }
    private static boolean isReward(ItemStack stack) { return isWand(stack, "silverwood", "thaumium"); }
    private static boolean findExact(Player player, ItemStack wanted) { return player.getInventory().items.stream().anyMatch(stack -> ItemStack.matches(stack, wanted)); }
    private ItemStack expectedPayer() { return craft.payerHotbar() == 7 ? expectedGreatwood : expectedSilverwood; }
    private List<ItemEntity> essenceEntities(Level level) { return level.getEntitiesOfClass(ItemEntity.class, new AABB(harvestNode).inflate(4), item -> item.getItem().is(essence())); }
    private static int outputDestination(Player player, ItemStack output) {
        for (int slot = 9; slot < 36; slot++) { var at = player.getInventory().getItem(slot); if (ItemStack.isSameItemSameComponents(at, output) && at.getCount() + output.getCount() <= at.getMaxStackSize()) return slot; }
        return emptyMain(player);
    }
    private static int emptyMain(Player player) { for (int slot = 9; slot < 36; slot++) if (player.getInventory().getItem(slot).isEmpty()) return slot; throw new IllegalStateException("No genuinely empty native main-inventory slot"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int slot = 0; slot < 36; slot++) if (wanted.test(player.getInventory().getItem(slot))) return slot; throw new IllegalStateException("Caps story lacks actual finite stage input/output"); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int inventorySlot(int slot) { return slot < 9 ? slot + 36 : slot; }
    private static int arcaneSlot(int slot) { return slot < 9 ? slot + 38 : slot + 2; }
    private static int alchemySlot(int slot) { return slot < 9 ? slot + 29 : slot - 7; }
    private static int vanillaFurnaceSlot(int slot) { return slot < 9 ? slot + 30 : slot - 6; }
    private static Item ingot() { return ThaumcraftContent.MATERIALS.get("thaumium_ingot").get(); }
    private static Item nugget() { return ThaumcraftWands.THAUMIUM_NUGGET.get(); }
    private static Item inert() { return ThaumcraftWands.THAUMIUM_WAND_CAP_INERT.get(); }
    private static Item cap() { return ThaumcraftWands.THAUMIUM_WAND_CAP.get(); }
    private static Item rod() { return ThaumcraftWands.SILVERWOOD_WAND_ROD.get(); }
    private static Item salis() { return ThaumcraftContent.MATERIALS.get("salis_mundus").get(); }
    private static Item balanced() { return ThaumcraftContent.SHARDS.get("balanced").get(); }
    private static Item essence() { return ThaumcraftEssences.WISP_ESSENCE.get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static TileJar jar(Level level, BlockPos at) { return (TileJar) level.getBlockEntity(at); }
    private static TileNode node(Level level, BlockPos at) { return (TileNode) level.getBlockEntity(at); }
    private TileAlchemyFurnace oven(Level level) { return (TileAlchemyFurnace) level.getBlockEntity(ovenPos); }
    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(ovenPos.above(depth)); }
    private FurnaceBlockEntity salt(Level level) { return (FurnaceBlockEntity) level.getBlockEntity(saltFurnace); }
    private TileInfusionMatrix matrix(Level level) { return (TileInfusionMatrix) level.getBlockEntity(matrixPos); }
    private static TilePedestal pedestal(Level level, BlockPos at) { return (TilePedestal) level.getBlockEntity(at); }
    private static FurnaceMenu saltMenu(Minecraft client) { return (FurnaceMenu) client.player.containerMenu; }
    private static ContainerAlchemyFurnace ovenMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static Vec3 south(BlockPos at) { return Vec3.atCenterOf(at).add(0, 0, .5); }
    private static Vec3 pedestalPoint(BlockPos at) { return Vec3.atCenterOf(at).add(0, 0, .25); }
    private static void use(Minecraft client, BlockPos at, Direction face, Vec3 point) { require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(at, 0), "native survival use reach: " + at); lookAt(client, point); require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, at, false)).consumesAction(), "native useItemOn accepted at " + at + "/" + face); }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 delta = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "caps server fixture/read-only actions never overlap"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
