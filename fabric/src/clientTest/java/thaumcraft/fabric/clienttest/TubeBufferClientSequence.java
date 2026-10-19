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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.blocks.BlockAlchemyFurnace;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.blocks.BlockBellows;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftBellows;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileBellows;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Buffer-only continuation in a NEW private copy of the accepted M06b save. */
public final class TubeBufferClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_CAPTURE, CHARGE_TRAVEL, CHARGE_AIM, CHARGE_READY, CHARGE_START,
        CHARGE_USE, CHARGE_STOP, CHARGE_ACK, BENCH_TRAVEL, BENCH_OPEN, FUND, FUNDED,
        GRID, PREVIEW, PREVIEW_ACK, TAKE, STORE, STORED, REMAINDER_ACK, WAND_PICK, WAND_RETURN, CRAFT_DONE,
        SCENE_TRAVEL, HELD_CAPTURE, BUFFER_PLACE, BUFFER_ACK, CONNECTOR_PLACE, CONNECTOR_ACK,
        LEVER_PLACE, LEVER_ACK, BELLOWS_WAIT, WAND_HAND, WAND_SHIFT, WAND_USE, WAND_ACK,
        INSPECT_HAND, INSPECT_SHIFT, INSPECT_READY, INSPECT_USE, INSPECT_ACK, INSPECT_CAPTURE,
        FURNACE_TRAVEL, FURNACE_OPEN, FUEL_PICK, FUEL_PUT, INPUT_PICK, INPUT_PUT, BURNING, MIXED_WAIT,
        LABEL_HAND, LABEL_ACK, POWER_TRAVEL, POWER_USE, POWER_ACK, OUTPUT_TRAVEL,
        OPEN_DOWN, DOWN_ACK, OPEN_NORTH, DRAIN, OFFHAND_STORE, OFFHAND_ACK, REMOVE_HAND,
        REMOVE_SHIFT, REMOVE_READY, REMOVE_USE, REMOVE_ACK, PICKUP, OFFHAND_RETURN, OFFHAND_RETURNED,
        GOLD_RETURN, FINAL_CHECK, FINAL_ACK, FINAL_CAPTURE, DONE
    }
    private static final List<String> CAPTURES = List.of(
            "122-buffer-original-PVP-T-T-PRP-native-CENTRIFUGE-book-en.png",
            "123-four-real-empty-phials-earned-valve-restrict-native-buffer-preview-en.png",
            "124-genuinely-crafted-held-buffer-original-core-and-shaft-torch-lit-world-en.png",
            "125-native-indexed-buffer-north-choke-one-blue-pressure-one-chat-en.png",
            "126-native-indexed-buffer-north-choke-two-red-pressure-zero-chat-en.png",
            "127-native-full-eight-mixed-buffer-four-ignis-four-potentia-sorted-chat-en.png",
            "128-native-buffer-typed-64-versus-untyped-32-arbitrated-two-jars-chat-en.png",
            "129-native-blank-label-physical-return-conserved-buffer-workshop-en.png");
    private static final Map<String, Integer> SAVED_VIS = Map.of("aer", 2950, "aqua", 2750, "ignis", 4300, "ordo", 90, "perditio", 3230, "terra", 3560);
    private static final Map<String, Integer> FROST_VIS = Map.of("aer", 1951, "aqua", 2436, "ignis", 2846, "ordo", 933, "perditio", 431, "terra", 464);
    private static final Map<String, Integer> TOTAL_PRICE = Map.of("aqua", 1350, "ordo", 900, "terra", 1440);
    private static final Map<String, Integer> BUDGET = Map.of("ignis", 4, "potentia", 4);
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final List<BlockPos> OLD_CLOCKS = List.of(new BlockPos(1722, 216, 377), new BlockPos(1754, 216, 377), new BlockPos(1786, 216, 377));
    private static final int HAND_SLOT = 3;

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final Minecraft owningClient;
    private final Consumer<String> capture;
    private final BlockPos workbench, center, bufferPos, connectorPos, jarA, jarB, bellowsPos, leverPos, chargingNode;
    private final List<BlockPos> furnaces, torches;
    private final long startedNanos = System.nanoTime();
    private final Map<BlockPos, BlockState> oldBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> oldBEs = new LinkedHashMap<>(), oldNodes = new LinkedHashMap<>();
    private final List<ItemStack> savedStacks = new ArrayList<>(), savedArmor = new ArrayList<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), crafts = new ArrayList<>(), balances = new ArrayList<>(), transfers = new ArrayList<>(), chargeRows = new ArrayList<>(), wandRows = new ArrayList<>(), inspections = new ArrayList<>();
    private final int[] rawCoalSlots = new int[2], fuelCoalSlots = new int[2];
    private final ClientTickEvent.Client chargeGuard = this::guardCharge;
    private Step step = Step.HANDOFF;
    private CompletableFuture<Void> serverAction;
    private TickEvent.Server nativePost;
    private EntityEvent.Add nativeAdds;
    private LifecycleEvent.ServerState serverStopped;
    private CameraType previousCamera;
    private volatile Throwable observationFailure;
    private volatile Frame observed;
    private volatile ChargeProbe charge;
    private volatile boolean awaitingBlank;
    private volatile UUID blankDropId;
    private volatile ItemStack blankDrop;
    private Ledger ledger;
    private ItemStack savedGold, chargedGold, paidGold, savedPrimary, savedOffhand, savedPhial, savedResonator, earnedBuffer;
    private CompoundTag expectedKnowledge;
    private List<int[]> gridClicks = List.of();
    private List<GuiMessage> chatBefore = List.of();
    private List<String> expectedChat = List.of();
    private int ticks, totalTicks, captures, turns, craftIndex, gridIndex, destination, wandAction, inspectMode, furnaceIndex, appleSlot;
    private int initialXp, handChangedAt = Integer.MIN_VALUE / 2, chargeTicks;
    private BlockPos useAimPos;
    private Direction useAimFace;
    private Vec3 useAimPoint;
    private int useAimStarted;
    private boolean savedInvulnerability, finalVerified, chargingHeld, aimCharge, releaseSent, primeNorth;

    public TubeBufferClientSequence(Minecraft client, Consumer<String> capture, BlockPos bench, BlockPos oldCenter) {
        owningClient = client; this.capture = capture; workbench = bench.immutable(); center = oldCenter.immutable();
        bufferPos = workbench.offset(9, 1, 0); connectorPos = bufferPos.north(); jarA = bufferPos.below(); jarB = connectorPos.below(); bellowsPos = bufferPos.south(); leverPos = bellowsPos.below();
        furnaces = List.of(workbench.east(8), workbench.east(10)); chargingNode = new BlockPos(1623, 218, 374);
        torches = List.of(workbench.offset(7, 0, -2), workbench.offset(12, 0, -2), workbench.offset(7, 0, 3), workbench.offset(12, 0, 3));
        report.put("scope", "buffer ONLY: NEW buffer-smoke-UUID private copy of ACCEPTED STOPPED tubefilter-smoke-cf7a6c49-0845-4cd6-a544-6352e8779cad; neither failed M06b world is an input; construct/centrifuge are NOT implemented here");
        report.put("boundary", "Actual saved gold7/basic5 in4+1 stacks/emptyphials3/POTENTIA8phial1/blank1 are inputs. Native saved-node use funds three paid crafts1350AQ/900ORDO/1440TERRA. New RAW glass3/clayball1 produce eight ordinary empty phials; saved3+earned8 minus buffer4 leaves7. Four ACTUAL saved basics pay new valve1/restrict1/buffer2; the fifth becomes the sole connector. RAW lever1(RECIPE)+stone1, plus separately declared lever1(EXTERNAL bellows power control), two RAW coals(one/furnace) and two separate FUEL coals(one/furnace). Empty table/two furnaces/two single alembics/two jars/one inward bellows, bounded floor/camera and FOUR static vanilla torches are fixtures. Only CENTRIFUGE research completion is new. No vis/essentia/XP/output grants or clock/weather/RNG writes.");
        report.put("native_story", "Real ordinary phials, real valve/restrict/buffer menu crafts and native saved-label/phial binding with inventory-FIRST unchanged phial remainder. Native placement and wand raw DOWN/NORTH close, paired NORTH/SOUTH reopen, then real sneak choke0->1->2->0 with one unpowered inward bellows:32/1/0/32. Close outputs and let two real coal sources fill mixed buffer8(4IGNIS+4POTENTIA). Real buffer-only sorted resonator inspection. Bind jarA to actual saved POTENTIA label. Power the declared bellows with the separate real lever; wait for native20tick cache refresh to suction1. Open DOWN then NORTH with overlapped native pre-aim, allowing simultaneous stronger typed64 versus connector31 arbitration:4POTENTIA to A,4IGNIS to B. Empty-hand native label removal, physical same-UUID pickup, exact apple restored.");
        report.put("coordinates", Map.of("bench", workbench.toShortString(), "old_center", center.toShortString(), "buffer", bufferPos.toShortString(), "furnaces", furnaces.stream().map(BlockPos::toShortString).toList(), "connector", connectorPos.toShortString(), "jarA", jarA.toShortString(), "jarB", jarB.toShortString(), "bellows", bellowsPos.toShortString(), "EXTERNAL_lever", leverPos.toShortString(), "saved_node", chargingNode.toShortString()));
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size()); report.put("declared_raw_inputs_and_fixtures", inputs); report.put("actual_native_crafts", crafts); report.put("passive_exact_balances", balances); report.put("passive_per_aspect_per_hop", transfers); report.put("actual_node_charge_transitions", chargeRows); report.put("actual_indexed_wand_actions", wandRows); report.put("actual_resonator_inspections", inspections);
        report.put("limits", Map.of("wall_seconds", 600, "client_ticks", 14000, "ordinary_step_ticks", 400, "node_charge_ticks", 2400, "native_mixed_fill_ticks", 6000, "native_drain_ticks", 1600, "preview_and_action_ack_fail_fast", 40));
        report.put("visual_boundary", "No GLFW/OS hover or inventory tooltip claim.124 is the real crafted buffer held outside GUI under declared mundane torch lighting.125/126 show native blue/red choke rods,127 real sorted4+4 chat,128 real jar chats. All exact122-129 pixels await root inspection. Root shared ChatScreen capture allowlist/10second deadline retained; no unsupported custom screen.");
        report.put("not_covered", List.of("old acquisition replay", "buffer closed-input fill/first-eligible failed-take/transaction/refusal/reload matrices: separate native server cases", "construct/centrifuge or other automation", "full legacy lighting/GUI/FX/audio parity or audio listening", "ordinary packaged install, remote server or NeoForge"));
    }

    public boolean tick(Minecraft client) throws ReflectiveOperationException {
        try { return advance(client); }
        catch (ReflectiveOperationException | RuntimeException | Error error) { releaseInputs(client); if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation); if (error instanceof RuntimeException) throw new IllegalStateException("Buffer story failed at " + step + "; craft=" + craftIndex + "; wand=" + wandAction + "; inspect=" + inspectMode + "; observed=" + observed, error); throw error; }
    }

    private boolean advance(Minecraft client) throws ReflectiveOperationException {
        require(client == owningClient, "same root-owned client"); ++ticks; ++totalTicks;
        int limit = step == Step.CHARGE_USE ? 2400 : step == Step.MIXED_WAIT ? 6000 : step == Step.DRAIN ? 1600 : 400;
        require(ticks <= limit && totalTicks <= 14000 && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded buffer step " + step + "; ticks=" + ticks + "/" + limit);
        if (observationFailure != null) throw new IllegalStateException("Passive native buffer observation failed", observationFailure);
        if (aimCharge && client.screen == null) sendRotation(client, Vec3.atCenterOf(chargingNode));
        if (primeNorth && (step == Step.DOWN_ACK || step == Step.OPEN_NORTH)) sendRotation(client, armPoint(Direction.NORTH));
        switch (step) {
            case HANDOFF -> { var path = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(); String name = path.getFileName().toString(); require(name.startsWith("buffer-smoke-"), "only NEW private buffer save"); UUID.fromString(name.substring("buffer-smoke-".length())); require(path.getParent().getFileName().toString().equals("saves") && path.getParent().getParent().getFileName().toString().equals("run-clienttest"), "isolated client-test save only"); report.put("actual_owned_world", path.toString()); previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON); client.setScreen(null); releaseKeys(client); require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "empty inherited native menu/cursor"); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP); }
            case SETUP -> { if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("CENTRIFUGE")) return false; client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("CENTRIFUGE"))); next(Step.BOOK); }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_CAPTURE); }
            case BOOK_CAPTURE -> { report.put("native_book", Map.of("actual_recipe", id("arcane/essentia_buffer").toString(), "research", "CENTRIFUGE", "pattern", List.of("PVP", "T T", "PRP"), "base_whole_vis", Map.of("aqua", 5, "ordo", 5))); next(Step.CHARGE_TRAVEL); capture(0); }
            case CHARGE_TRAVEL -> { client.setScreen(null); aimCharge = true; onServer(client, () -> stand(serverPlayer(client), chargingNode.getX() + .5, workbench.getY(), chargingNode.getZ() + 3.5)); next(Step.CHARGE_AIM); }
            case CHARGE_AIM -> { if (!serverDone() || ticks < 16 || !hand(client, value -> ItemStack.matches(value, savedGold))) return false; onServer(client, () -> charge = new ChargeProbe(serverPlayer(client))); next(Step.CHARGE_READY); }
            case CHARGE_READY -> { if (!serverDone()) return false; next(Step.CHARGE_START); }
            case CHARGE_START -> { if (!client.isWindowActive()) return false; sendRotation(client, Vec3.atCenterOf(chargingNode)); releaseSent = false; chargingHeld = true; chargeTicks = 0; client.options.keyUse.setDown(true); require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "NEW native use of actual saved NORMAL node"); next(Step.CHARGE_USE); }
            case CHARGE_USE -> { require(ticks < 40 || charge.used, "native charge session must start within40ticks"); if (!charge.funded) return false; releaseUse(client); next(Step.CHARGE_STOP); }
            case CHARGE_STOP -> { releaseUse(client); if (ticks < 6 || client.player.isUsingItem() || charge.stopped < 2) return false; onServer(client, () -> { report.put("actual_finite_node_recharge", charge.finish()); chargedGold = paidGold = serverPlayer(client).getMainHandItem().copy(); }); next(Step.CHARGE_ACK); }
            case CHARGE_ACK -> { if (!serverDone() || !ItemStack.matches(client.player.getMainHandItem(), chargedGold)) return false; aimCharge = false; next(Step.BENCH_TRAVEL); }
            case BENCH_TRAVEL -> { if (!ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) { click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, paidGold))), 7, ClickType.SWAP); return false; } onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.BENCH_OPEN); }
            case BENCH_OPEN -> { if (!serverDone() || ticks < 12) return false; select(client, 7); if (!use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5))) return false; next(Step.FUND); }
            case FUND -> { if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false; for (int slot = 0; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "genuinely empty new native workbench"); click(client, playerSlot(client, 7), 0, ClickType.QUICK_MOVE); next(Step.FUNDED); }
            case FUNDED -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getSlot(1).getItem(), paidGold)) return false; beginGrid(client); }
            case GRID -> { if (!serverDone() || ticks < 2) return false; if (gridIndex == gridClicks.size()) { next(Step.PREVIEW); return false; } int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0; }
            case PREVIEW -> { if (ticks < 8) return false; if (!arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craftOutput())) { require(ticks < 40, "native preview missing within40ticks: " + craftRecipe() + "; grid=" + java.util.stream.IntStream.range(2, 11).mapToObj(slot -> arcane(client).getSlot(slot).getItem().toString()).toList()); return false; } require(arcane(client).getCarried().isEmpty() && arcane(client).getSlot(0).getItem().getCount() == outputCount(), "real finite native preview"); verifyGrid(arcane(client), false); onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.PREVIEW_ACK); }
            case PREVIEW_ACK -> { if (!serverDone()) return false; for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == craftPrice().getOrDefault(tag, 0), "actual native worn-gear price " + tag); next(Step.TAKE); if (craftIndex == 3) capture(1); }
            case TAKE -> { click(client, 0, 0, ClickType.PICKUP); next(Step.STORE); }
            case STORE -> { if (ticks < 6 || !arcane(client).getCarried().is(craftOutput())) return false; require(arcane(client).getCarried().getCount() == outputCount() && (craftIndex != 4 || ItemJarLabel.aspect(arcane(client).getCarried()) == Aspect.ENERGY), "only genuine native result, actual label bound to saved POTENTIA phial"); verifyGrid(arcane(client), true); destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.STORED); }
            case STORED -> { if (ticks < 6 || !arcane(client).getCarried().isEmpty()) return false; verifyDebit(paidGold, arcane(client).getSlot(1).getItem(), craftPrice()); paidGold = arcane(client).getSlot(1).getItem().copy(); if (craftIndex == 4) next(Step.REMAINDER_ACK); else { ++craftIndex; beginGrid(client); } }
            case REMAINDER_ACK -> { if (ticks < 6 || count(client.player, filledPhial()) != 1) return false; verifyGrid(arcane(client), true); require(ItemStack.matches(stack(client.player, value -> value.is(filledPhial())), savedPhial), "actual original inventory-FIRST unchanged phial remainder; NEVER expect/reconstruct it in matrix"); click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK); }
            case WAND_PICK -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), paidGold)) return false; require(client.player.getInventory().getItem(7).isEmpty(), "actual payer returns to saved7"); click(client, playerSlot(client, 7), 0, ClickType.PICKUP); next(Step.WAND_RETURN); }
            case WAND_RETURN -> { if (ticks < 6 || !arcane(client).getCarried().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) return false; client.player.closeContainer(); onServer(client, () -> verifyCrafts(serverPlayer(client))); next(Step.CRAFT_DONE); }
            case CRAFT_DONE -> { if (!serverDone() || ticks < 8) return false; onServer(client, () -> northCamera(serverPlayer(client))); next(Step.SCENE_TRAVEL); }
            case SCENE_TRAVEL -> { if (!serverDone() || ticks < 12 || !hand(client, value -> value.is(bufferItem()))) return false; require(ItemStack.matches(client.player.getMainHandItem(), earnedBuffer), "actual crafted plain buffer held"); lookAt(client, Vec3.atCenterOf(bufferPos).add(0, -.35, 0)); next(Step.HELD_CAPTURE); }
            case HELD_CAPTURE -> { if (ticks < 8) return false; next(Step.BUFFER_PLACE); capture(2); }
            case BUFFER_PLACE -> { if (!use(client, furnaces.getFirst().above(), Direction.EAST, Vec3.atCenterOf(furnaces.getFirst().above()).add(.5, 0, 0))) return false; next(Step.BUFFER_ACK); }
            case BUFFER_ACK -> { if (ticks < 8 || !(client.level.getBlockEntity(bufferPos) instanceof TileTubeBuffer)) return false; require(count(client.player, bufferItem()) == 0, "one actual crafted buffer placed"); next(Step.CONNECTOR_PLACE); }
            case CONNECTOR_PLACE -> { if (!hand(client, value -> value.is(tubeItem()))) return false; if (!use(client, bufferPos, Direction.NORTH, Vec3.atCenterOf(bufferPos).add(0, 0, -.25))) return false; next(Step.CONNECTOR_ACK); }
            case CONNECTOR_ACK -> { if (ticks < 8 || !(client.level.getBlockEntity(connectorPos) instanceof TileTube)) return false; require(count(client.player, tubeItem()) == 0, "fifth ACTUAL saved tube is the only connector"); onServer(client, () -> powerCamera(serverPlayer(client))); next(Step.LEVER_PLACE); }
            case LEVER_PLACE -> { if (!serverDone() || ticks < 12 || !hand(client, value -> value.is(Items.LEVER))) return false; if (!use(client, leverPos.below(), Direction.UP, Vec3.atCenterOf(leverPos.below()).add(0, .5, 0))) return false; next(Step.LEVER_ACK); }
            case LEVER_ACK -> { if (ticks < 8 || !client.level.getBlockState(leverPos).is(Blocks.LEVER)) return false; require(!client.level.getBlockState(leverPos).getValue(LeverBlock.POWERED) && count(client.player, Items.LEVER) == 0, "separate external lever really placed, not the consumed recipe lever"); onServer(client, () -> northCamera(serverPlayer(client))); next(Step.BELLOWS_WAIT); }
            case BELLOWS_WAIT -> { if (!serverDone() || ticks < 12) return false; Frame frame = observed; if (frame == null || frame.bellows() != 1 || frame.northPressure() != 32) return false; require(frame.stored().isEmpty(), "all new devices dry before native choke demo"); next(Step.WAND_HAND); }
            case WAND_HAND -> { client.setScreen(null); if (!serverDone() || !hand(client, value -> ItemStack.matches(value, paidGold))) return false; client.options.keyShift.setDown(wandAction >= 3 && wandAction <= 5); next(Step.WAND_SHIFT); }
            case WAND_SHIFT -> { if (ticks < 6 || client.player.isShiftKeyDown() != (wandAction >= 3 && wandAction <= 5)) return false; next(Step.WAND_USE); }
            case WAND_USE -> { Direction side = wandAction == 0 ? Direction.DOWN : Direction.NORTH; if (!use(client, bufferPos, side, armPoint(side))) return false; next(Step.WAND_ACK); }
            case WAND_ACK -> { if (ticks < 4) return false; if (!wandAcknowledged(client.level)) { require(ticks < 40, "native indexed buffer action must acknowledge within40ticks; action=" + wandAction + "; state=" + observed); return false; } int done = wandAction; onServer(client, () -> verifyWand(serverPlayer(client), done)); client.options.keyShift.setDown(false); if (done == 3) startInspect(0); else if (done == 4) startInspect(1); else if (done == 6) { furnaceIndex = 0; next(Step.FURNACE_TRAVEL); } else { ++wandAction; next(Step.WAND_HAND); } }
            case INSPECT_HAND -> { if (!serverDone() || !hand(client, value -> value.is(ThaumcraftContent.RESONATOR.get()))) return false; client.options.keyShift.setDown(false); next(Step.INSPECT_SHIFT); }
            case INSPECT_SHIFT -> { if (ticks < 6 || client.player.isShiftKeyDown()) return false; chatBefore = chat(client); onServer(client, () -> expectedChat = inspectionLines(serverPlayer(client))); next(Step.INSPECT_READY); }
            case INSPECT_READY -> { if (!serverDone()) return false; next(Step.INSPECT_USE); }
            case INSPECT_USE -> { if (!use(client, inspectionPos(), inspectionFace(client.level), inspectionPoint(client.level))) return false; next(Step.INSPECT_ACK); }
            case INSPECT_ACK -> { if (ticks < 8) return false; var actual = chat(client).stream().filter(value -> !chatBefore.contains(value)).map(value -> value.content().getString()).toList(); if (actual.size() < expectedChat.size()) return false; require(actual.reversed().equals(expectedChat), "actual native resonator chat exactly once and TAG-sorted in send order; expected=" + expectedChat + "; actual_newest_first=" + actual); int mode = inspectMode; List<String> expected = expectedChat; onServer(client, () -> { require(ItemStack.matches(serverPlayer(client).getMainHandItem(), savedResonator), "real resonator unchanged"); inspections.add(Map.of("mode", mode, "actual_chat_newest_first", actual, "actual_chat_send_order", actual.reversed(), "expected_from_actual_raw_state", expected, "native_tick", serverPlayer(client).level().getGameTime(), "synthetic_messages", 0)); }); if (mode == 3) startInspect(4); else next(Step.INSPECT_CAPTURE); }
            case INSPECT_CAPTURE -> { if (!serverDone() || ticks < 8) return false; lookAt(client, Vec3.atCenterOf(bufferPos).add(0, -.15, 0)); client.setScreen(new ChatScreen("")); if (inspectMode == 0) { wandAction = 4; next(Step.WAND_HAND); capture(3); } else if (inspectMode == 1) { wandAction = 5; next(Step.WAND_HAND); capture(4); } else if (inspectMode == 2) { next(Step.LABEL_HAND); capture(5); } else { next(Step.OFFHAND_STORE); capture(6); } }
            case FURNACE_TRAVEL -> { client.setScreen(null); if (!serverDone()) return false; onServer(client, () -> furnaceCamera(serverPlayer(client), furnaceIndex)); next(Step.FURNACE_OPEN); }
            case FURNACE_OPEN -> { if (!serverDone() || ticks < 12 || !emptyHand(client)) return false; if (!use(client, furnaces.get(furnaceIndex), Direction.NORTH, Vec3.atCenterOf(furnaces.get(furnaceIndex)).add(0, 0, -.5))) return false; next(Step.FUEL_PICK); }
            case FUEL_PICK -> { if (ticks < 8 || !(client.screen instanceof GuiAlchemyFurnace)) return false; require(furnaceMenu(client).getSlot(0).getItem().isEmpty() && furnaceMenu(client).getSlot(1).getItem().isEmpty(), "both NEW furnace slots genuinely empty"); click(client, playerSlot(client, fuelCoalSlots[furnaceIndex]), 0, ClickType.PICKUP); next(Step.FUEL_PUT); }
            case FUEL_PUT -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == 1, "one separate FUEL coal for this furnace"); click(client, 1, 0, ClickType.PICKUP); next(Step.INPUT_PICK); }
            case INPUT_PICK -> { if (ticks < 5 || !furnaceMenu(client).getCarried().isEmpty()) return false; click(client, playerSlot(client, rawCoalSlots[furnaceIndex]), 0, ClickType.PICKUP); next(Step.INPUT_PUT); }
            case INPUT_PUT -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == 1, "one ACTUAL RAW coal for this furnace"); click(client, 0, 0, ClickType.PICKUP); next(Step.BURNING); }
            case BURNING -> { if (ticks < 8 || !furnaceMenu(client).isBurning()) return false; require(!furnaceMenu(client).isSpeedBoost() && furnaceMenu(client).getCurrentItemBurnTime() == 1600 && furnaceMenu(client).getBellowsCount() == 0 && furnaceMenu(client).getSmeltTime() == 40, "real40tick raw coal,1600 ordinary fuel, no bellows clock boost"); client.player.closeContainer(); if (++furnaceIndex < 2) next(Step.FURNACE_TRAVEL); else { onServer(client, () -> northCamera(serverPlayer(client))); next(Step.MIXED_WAIT); } }
            case MIXED_WAIT -> { if (!serverDone()) return false; Frame frame = observed; if (frame == null || frame.rawSmelted() != 2 || !frame.buffer().equals(BUDGET) || frame.sourceUnits() != 0) return false; require(!frame.downOpen() && !frame.northOpen() && frame.jarA() == 0 && frame.jarB() == 0 && frame.connector() == 0 && frame.bellows() == 1 && frame.northPressure() == 32, "ACTUAL mixed8 holds4+4 with both real outputs closed and native bellows32"); onServer(client, () -> recordFullMixedBuffer(serverPlayer(client))); startInspect(2); }
            case LABEL_HAND -> { client.setScreen(null); if (!hand(client, value -> value.is(labelItem()) && ItemJarLabel.aspect(value) == Aspect.ENERGY)) return false; if (!use(client, jarA, Direction.NORTH, jarPoint(jarA, Direction.NORTH))) return false; next(Step.LABEL_ACK); }
            case LABEL_ACK -> { require(ticks < 40 || jar(client.level, 0).getFilter() == Aspect.ENERGY && count(client.player, labelItem()) == 0, "paid jar label must acknowledge within40ticks"); if (ticks < 8 || jar(client.level, 0).getFilter() != Aspect.ENERGY || count(client.player, labelItem()) != 0) return false; require(observed.buffer().equals(BUDGET) && jar(client.level, 0).getFacing() == Direction.NORTH, "label creates demand, never essentia, while outputs remain closed; actual paid NORTH face remains visible around jarB"); onServer(client, () -> powerCamera(serverPlayer(client))); next(Step.POWER_TRAVEL); }
            case POWER_TRAVEL -> { if (!serverDone() || ticks < 12 || !emptyHand(client)) return false; next(Step.POWER_USE); }
            case POWER_USE -> { if (!use(client, leverPos, Direction.SOUTH, Vec3.atCenterOf(leverPos).add(0, -.25, .15))) return false; next(Step.POWER_ACK); }
            case POWER_ACK -> { Frame frame = observed; if (ticks < 8 || frame.bellows() != 0 || frame.northPressure() != 1 || !client.level.getBlockState(leverPos).getValue(LeverBlock.POWERED)) return false; require(frame.buffer().equals(BUDGET) && !frame.downOpen() && !frame.northOpen(), "real powered bellows and original cached refresh reduce pressure to1 without moving mixed8"); report.put("actual_bellows_power_transition", Map.of("actual_native_tick", frame.tick(), "actual_count", frame.count(), "actual_bellows", frame.bellows(), "actual_suction", frame.northPressure(), "cache_refreshed_by_native_tick_only", true)); onServer(client, () -> northCamera(serverPlayer(client))); next(Step.OUTPUT_TRAVEL); }
            case OUTPUT_TRAVEL -> { if (!serverDone() || ticks < 12 || !hand(client, value -> ItemStack.matches(value, paidGold))) return false; next(Step.OPEN_DOWN); }
            case OPEN_DOWN -> { if (!use(client, bufferPos, Direction.DOWN, armPoint(Direction.DOWN))) return false; primeNorth = true; beginAim(client, bufferPos, Direction.NORTH, armPoint(Direction.NORTH)); next(Step.DOWN_ACK); }
            case DOWN_ACK -> { require(ticks < 40 || observed.downOpen(), "native DOWN reopen must acknowledge within40ticks"); if (ticks < 2 || !observed.downOpen() || !buffer(client.level).isSideOpen(Direction.DOWN)) return false; next(Step.OPEN_NORTH); }
            case OPEN_NORTH -> { if (!use(client, bufferPos, Direction.NORTH, armPoint(Direction.NORTH))) return false; primeNorth = false; next(Step.DRAIN); }
            case DRAIN -> { Frame frame = observed; if (frame.jarA() != 4 || frame.jarB() != 4 || !frame.buffer().isEmpty() || frame.connector() != 0) return false; require(frame.locations().get("jarA").equals(Map.of("potentia", 4)) && frame.locations().get("jarB").equals(Map.of("ignis", 4)) && ledger.contestedMixed, "actual stronger typed64 demand wins POTENTIA while weaker31 connector routes IGNIS with both types still held"); onServer(client, () -> { require(buffer(serverPlayer(client).level()).isSideOpen(Direction.DOWN) && buffer(serverPlayer(client).level()).isSideOpen(Direction.NORTH) && connector(serverPlayer(client).level()).isSideOpen(Direction.SOUTH) && ItemStack.matches(serverPlayer(client).getMainHandItem(), paidGold), "real indexed outputs reopen/pair with zero wand vis debit"); }); startInspect(3); }
            case OFFHAND_STORE -> { client.setScreen(null); appleSlot = emptyMain(client.player); click(client, playerSlot(client, appleSlot), 40, ClickType.SWAP); next(Step.OFFHAND_ACK); }
            case OFFHAND_ACK -> { if (ticks < 6 || !client.player.getOffhandItem().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(appleSlot), savedOffhand)) return false; next(Step.REMOVE_HAND); }
            case REMOVE_HAND -> { if (!emptyHand(client)) return false; client.options.keyShift.setDown(true); next(Step.REMOVE_SHIFT); }
            case REMOVE_SHIFT -> { if (ticks < 6 || !client.player.isShiftKeyDown()) return false; onServer(client, () -> { require(serverPlayer(client).getMainHandItem().isEmpty() && serverPlayer(client).getOffhandItem().isEmpty() && serverPlayer(client).isShiftKeyDown(), "native BOTH-hands-empty label removal, apple safely retained in inventory"); awaitingBlank = true; }); next(Step.REMOVE_READY); }
            case REMOVE_READY -> { if (!serverDone()) return false; next(Step.REMOVE_USE); }
            case REMOVE_USE -> { Direction face = jar(client.level, 0).getFacing(); if (!use(client, jarA, face, jarPoint(jarA, face))) return false; client.options.keyShift.setDown(false); next(Step.REMOVE_ACK); }
            case REMOVE_ACK -> { require(ticks < 40 || jar(client.level, 0).getFilter() == null, "native label removal must acknowledge within40ticks"); if (ticks < 8 || client.player.isShiftKeyDown() || jar(client.level, 0).getFilter() != null) return false; next(Step.PICKUP); }
            case PICKUP -> { if (!serverDone()) return false; if (blankDropId == null || count(client.player, labelItem()) != 1) { if (ticks % 8 == 0) onServer(client, () -> approachBlank(serverPlayer(client))); return false; } onServer(client, () -> { var player = serverPlayer(client); require(blankDrop != null && ItemStack.matches(stack(player, value -> value.is(labelItem())), blankDrop) && player.serverLevel().getEntity(blankDropId) == null, "same real native blank UUID physically picked up, no fabricated refund"); report.put("actual_blank_return", Map.of("UUID", blankDropId.toString(), "actual_RAW_snbt", snbt(blankDrop, player), "physical_returns", 1, "item_relocations_or_output_grants", 0)); northCamera(player); }); next(Step.OFFHAND_RETURN); }
            case OFFHAND_RETURN -> { if (!serverDone() || ticks < 8) return false; click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, savedOffhand))), 40, ClickType.SWAP); next(Step.OFFHAND_RETURNED); }
            case OFFHAND_RETURNED -> { if (ticks < 8 || !ItemStack.matches(client.player.getOffhandItem(), savedOffhand)) return false; next(Step.GOLD_RETURN); }
            case GOLD_RETURN -> { if (!ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) { click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, paidGold))), 7, ClickType.SWAP); return false; } if (ticks < 8 || !hand(client, value -> value.is(labelItem()) && ItemJarLabel.data(value) == null)) return false; next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> { if (!serverDone() || ticks < 8) return false; require(finalVerified && nativePost == null && nativeAdds == null && serverStopped == null && captures == 7, "all native hooks removed and seven exact preceding frames"); lookAt(client, Vec3.atCenterOf(bufferPos).add(0, -.25, -.25)); next(Step.FINAL_CAPTURE); }
            case FINAL_CAPTURE -> { if (ticks < 8) return false; report.put("native_client_ticks", totalTicks); report.put("elapsed_wall_seconds", (System.nanoTime() - startedNanos) / 1_000_000_000D); next(Step.DONE); capture(7); }
            case DONE -> { require(captures == CAPTURES.size(), "exact eight122-129 captures"); client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(workbench.equals(new BlockPos(1810, 216, 377)) && center.equals(new BlockPos(1474, 216, 377)), "root passes actual new1810 bench and old1474 center"); require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && player.isAlive(), "actual saved Survival"); initialXp = player.totalExperience; savedInvulnerability = player.isInvulnerable(); require(initialXp == 11 && savedInvulnerability, "actual XP11/Entity Invulnerable1 unchanged");
        savedGold = player.getInventory().getItem(7).copy(); savedPrimary = stack(player, TubeBufferClientSequence::primary).copy(); savedOffhand = player.getOffhandItem().copy(); savedPhial = stack(player, value -> value.is(filledPhial())).copy(); savedResonator = stack(player, value -> value.is(ThaumcraftContent.RESONATOR.get())).copy();
        require(gold(savedGold) && ItemWandCasting.data(savedGold).vis().equals(SAVED_VIS) && FocusData.get(savedGold).isEmpty(), "EXACT accepted M06b gold7, including only90ORDO, no topup"); require(ItemWandCasting.data(savedPrimary).vis().equals(FROST_VIS) && FocusData.get(savedPrimary).is(ThaumcraftFoci.FROST_FOCUS.get()), "EXACT main Frost payer, never used");
        require(count(player, tubeItem()) == 5 && player.getInventory().items.stream().filter(value -> value.is(tubeItem())).map(ItemStack::getCount).sorted().toList().equals(List.of(1, 4)) && count(player, emptyPhial()) == 3 && count(player, filledPhial()) == 1 && savedPhial.getCount() == 1 && ItemEssence.data(savedPhial) != null && ItemEssence.data(savedPhial).tag().equals("potentia") && count(player, labelItem()) == 1 && ItemJarLabel.data(stack(player, value -> value.is(labelItem()))) == null && count(player, material("vis_filter")) == 1 && count(player, material("quicksilver_drop")) == 7, "ACTUAL basic4+1/emptyphials3/filledP8/blank1/VisFilter1/DROP7");
        require(count(player, valveItem()) == 0 && count(player, restrictItem()) == 0 && count(player, bufferItem()) == 0 && count(player, ThaumcraftContent.WAND.get()) == 3 && savedOffhand.is(Items.APPLE) && savedOffhand.getCount() == 1 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.SHOCK_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.EXCAVATION_FOCUS.get()) == 1, "no finished buffer prerequisites supplied; all old tools/foci/apple present");
        var worn = List.of(ThaumcraftWearables.GOGGLES.get(), ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get()); for (int index = 0; index < ARMOR.size(); index++) { ItemStack value = player.getItemBySlot(ARMOR.get(index)); require(value.is(worn.get(index)) && value.getDamageValue() == (index >= 2 ? 1 : 0), "actual worn gear0/0/1/1"); savedArmor.add(value.copy()); } for (ItemStack value : player.getInventory().items) if (!value.isEmpty()) savedStacks.add(value.copy());
        var level = player.serverLevel(); scanOld(level); TileNode source = node(player); require(source.getNodeType() == TileNode.NodeType.NORMAL && source.getScanId().equals("9bbeaa03-a122-4968-ac6c-131f416e1365") && aspects(source.getAspectsBase()).equals(Map.of("aer", 80, "aqua", 80, "ignis", 80, "ordo", 80, "perditio", 80, "terra", 80)), "actual saved NORMAL node UUID/base80, current never reset"); report.put("actual_saved_node_before_RAW_snbt", beTag(source).toString()); patch(level);
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        for (BlockPos at : furnaces) { level.setBlockAndUpdate(at, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState()); level.setBlockAndUpdate(at.above(), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.NORTH)); }
        level.setBlockAndUpdate(jarA, ThaumcraftContent.JAR.get().defaultBlockState()); level.setBlockAndUpdate(jarB, ThaumcraftContent.JAR.get().defaultBlockState()); level.setBlockAndUpdate(bellowsPos, ThaumcraftBellows.BELLOWS.get().defaultBlockState().setValue(BlockBellows.FACING, Direction.NORTH)); for (BlockPos at : torches) level.setBlockAndUpdate(at, Blocks.TORCH.defaultBlockState());
        for (int index = 0; index < 2; index++) require(oven(level, index).isEmpty() && oven(level, index).getVis() == 0 && alembic(level, index).getAmount() == 0 && alembic(level, index).getFilter() == null && jar(level, index).getAmount() == 0 && jar(level, index).getFilter() == null, "only genuinely empty new devices"); require(!((TileBellows) level.getBlockEntity(bellowsPos)).isPowered(), "one actually unpowered inward bellows fixture");
        var knowledge = KnowledgeAccess.get(player); CompoundTag before = knowledge.save().copy(); require(knowledge.isResearchComplete("TUBEFILTER") && knowledge.isResearchComplete("NODEPRESERVE"), "reuse actual prior research, never replay it"); boolean already = knowledge.isResearchComplete("CENTRIFUGE"); knowledge.addResearch("CENTRIFUGE"); expectedKnowledge = knowledge.save().copy(); KnowledgeEvents.sync(player); report.put("declared_research_fixture", Map.of("ONLY_key", "CENTRIFUGE", "already_complete", already, "earned_here", false, "before_RAW_snbt", before.toString(), "after_RAW_snbt", expectedKnowledge.toString()));
        require(aspects(ObjectAspectResolver.get(new ItemStack(Items.COAL), level)).equals(Map.of("ignis", 2, "potentia", 2)), "native actual coal2IGNIS+2POTENTIA"); for (Item raw : List.of(Items.GLASS, Items.CLAY_BALL, Items.STONE, Items.LEVER, Items.COAL)) require(count(player, raw) == 0, "new RAW fixture cannot conflate saved stock " + raw);
        supply(player, Items.GLASS, 3, "ordinary eight-phial RAW"); supply(player, Items.CLAY_BALL, 1, "ordinary eight-phial RAW"); supply(player, Items.STONE, 1, "native restrict RAW, NOT saved cobblestone"); supply(player, Items.LEVER, 2, "RECIPE lever1 plus separate EXTERNAL bellows-control lever1"); for (int index = 0; index < 2; index++) { rawCoalSlots[index] = supply(player, Items.COAL, 1, "RAW coal for furnace" + index); fuelCoalSlots[index] = supply(player, Items.COAL, 1, "separate FUEL coal for furnace" + index); }
        report.put("actual_saved_inputs", Map.of("gold7_RAW_snbt", snbt(savedGold, player), "main_Frost_RAW_snbt", snbt(savedPrimary, player), "main_stacks_RAW_snbt", savedStacks.stream().map(value -> snbt(value, player)).toList(), "worn_RAW_snbt", savedArmor.stream().map(value -> snbt(value, player)).toList(), "offhand_RAW_snbt", snbt(savedOffhand, player), "actual_catalyst_RAW_snbt", snbt(savedPhial, player), "saved_total_XP", initialXp)); inputs.add(Map.of("fixtures", "empty table/two furnaces/two single alembics/two jars/one inward bellows, bounded floor/camera, FOUR static ordinary vanilla torches", "torch_positions", torches.stream().map(BlockPos::toShortString).toList(), "actual_unmodified_day_time", level.getDayTime(), "clock_weather_RNG_writes", 0)); player.inventoryMenu.broadcastChanges(); ledger = new Ledger(player); startObservation(player);
    }

    private Item ingredient(int cell, int craft) { return switch (craft) {
        case 0 -> cell == 1 ? Items.CLAY_BALL : cell == 3 || cell == 5 || cell == 7 ? Items.GLASS : Items.AIR;
        case 1 -> cell == 2 ? tubeItem() : cell == 6 ? Items.LEVER : Items.AIR;
        case 2 -> cell == 1 ? tubeItem() : cell == 7 ? Items.STONE : Items.AIR;
        case 3 -> switch (cell) { case 0, 2, 6, 8 -> emptyPhial(); case 1 -> valveItem(); case 3, 5 -> tubeItem(); case 7 -> restrictItem(); default -> Items.AIR; };
        default -> cell == 0 ? labelItem() : cell == 8 ? filledPhial() : Items.AIR;
    }; }
    private Item craftOutput() { return switch (craftIndex) { case 0 -> emptyPhial(); case 1 -> valveItem(); case 2 -> restrictItem(); case 3 -> bufferItem(); default -> labelItem(); }; }
    private int outputCount() { return craftIndex == 0 ? 8 : 1; }
    private ResourceLocation craftRecipe() { return id(switch (craftIndex) { case 0 -> "phial_empty"; case 1 -> "arcane/essentia_valve"; case 2 -> "arcane/essentia_restrict"; case 3 -> "arcane/essentia_buffer"; default -> "jar_label_bind"; }); }
    private Map<String, Integer> craftPrice() { return switch (craftIndex) { case 1, 3 -> Map.of("aqua", 450, "ordo", 450); case 2 -> Map.of("aqua", 450, "terra", 1440); default -> Map.of(); }; }
    private void beginGrid(Minecraft client) {
        // The accepted basic tubes and the eleven phials are REAL split stacks.
        // Populate each cell once across as many actual source slots as needed.
        var clicks = new ArrayList<int[]>(); var types = new LinkedHashSet<Item>(); for (int cell = 0; cell < 9; cell++) if (ingredient(cell, craftIndex) != Items.AIR) types.add(ingredient(cell, craftIndex));
        for (Item item : types) { var cells = new ArrayList<Integer>(); for (int cell = 0; cell < 9; cell++) if (ingredient(cell, craftIndex) == item) cells.add(cell); int used = 0; for (int slot = 0; slot < 36 && used < cells.size(); slot++) { ItemStack actual = client.player.getInventory().getItem(slot); if (!actual.is(item)) continue; int from = playerSlot(client, slot), take = Math.min(actual.getCount(), cells.size() - used); clicks.add(new int[]{from, 0}); for (int amount = 0; amount < take; amount++) clicks.add(new int[]{cells.get(used++) + 2, 1}); clicks.add(new int[]{from, 0}); } require(used == cells.size(), "actual split input stock covers native recipe cells " + item); }
        gridClicks = List.copyOf(clicks); gridIndex = 0; onServer(client, () -> preflight(serverPlayer(client))); next(Step.GRID);
    }
    private void preflight(ServerPlayer player) { var projection = new SimpleContainer(11); var needed = new LinkedHashMap<Item, Integer>(); for (int cell = 0; cell < 9; cell++) { Item wanted = ingredient(cell, craftIndex); if (wanted != Items.AIR) { needed.merge(wanted, 1, Integer::sum); projection.setItem(cell, stack(player, value -> value.is(wanted)).copyWithCount(1)); } } for (var entry : needed.entrySet()) require(count(player, entry.getKey()) >= entry.getValue(), "actual split stacks, not copied shortage, cover recipe " + entry.getKey()); projection.setItem(TileArcaneWorkbench.WAND_SLOT, paidGold.copy()); var match = ArcaneCrafting.find(player, projection); require(match != null && match.recipe().id().equals(craftRecipe()) && match.arcane() == (craftIndex >= 1 && craftIndex <= 3) && match.prices().equals(craftPrice()) && match.output().is(craftOutput()) && match.output().getCount() == outputCount(), "read-only actual-input recipe preflight before clicks " + craftRecipe()); }
    private void verifyGrid(ContainerArcaneWorkbench menu, boolean consumed) { for (int cell = 0; cell < 9; cell++) { ItemStack actual = menu.getSlot(cell + 2).getItem(); Item wanted = ingredient(cell, craftIndex); require(consumed || wanted == Items.AIR ? actual.isEmpty() : actual.is(wanted) && actual.getCount() == 1, "native one-per-cell consumption; inventory-FIRST remainder means matrix empty " + cell); } }
    private void inspectCraft(ServerPlayer player) { var match = ArcaneCrafting.find(player, table(player.level())); require(match != null && match.recipe().id().equals(craftRecipe()) && match.prices().equals(craftPrice()) && match.output().getCount() == outputCount(), "actual native menu recipe and worn-gear price"); crafts.add(Map.of("stage", craftIndex, "actual_recipe", match.recipe().id().toString(), "actual_matrix", match.input().items().stream().map(value -> value.isEmpty() ? "EMPTY" : snbt(value, player)).toList(), "actual_output_preview", snbt(match.output(), player), "actual_centivis", match.prices(), "actual_payer_before_RAW_snbt", snbt(table(player.level()).getItem(10), player), "direct_craft_calls", 0)); }
    private void verifyCrafts(ServerPlayer player) { verifyDebit(chargedGold, player.getInventory().getItem(7), TOTAL_PRICE); require(crafts.size() == 5 && table(player.level()).isEmpty() && player.containerMenu.getCarried().isEmpty() && count(player, tubeItem()) == 1 && count(player, emptyPhial()) == 7 && count(player, bufferItem()) == 1 && count(player, valveItem()) == 0 && count(player, restrictItem()) == 0 && count(player, Items.LEVER) == 1 && count(player, labelItem()) == 1 && ItemJarLabel.aspect(stack(player, value -> value.is(labelItem()))) == Aspect.ENERGY && ItemStack.matches(stack(player, value -> value.is(filledPhial())), savedPhial), "three real paid crafts, real eight phials, saved8phial inventory-FIRST remainder, four actual basics/four empty phials consumed"); for (Item raw : List.of(Items.GLASS, Items.CLAY_BALL, Items.STONE)) require(count(player, raw) == 0, "declared RAW recipe stock consumed " + raw); earnedBuffer = stack(player, value -> value.is(bufferItem())).copy(); report.put("native_craft_totals", Map.of("paid_crafts", 3, "ordinary_crafts", 2, "actual_price", TOTAL_PRICE, "actual_empty_phials_earned", 8, "actual_empty_phials_consumed", 4, "actual_empty_phials_remaining", 7, "actual_saved_basics_consumed_in_recipes", 4, "actual_buffer_RAW_snbt", snbt(earnedBuffer, player), "actual_phial_remainder_RAW_snbt", snbt(stack(player, value -> value.is(filledPhial())), player), "actual_gold_after_RAW_snbt", snbt(player.getInventory().getItem(7), player))); }

    private boolean wandAcknowledged(Level level) { TileTubeBuffer actual = buffer(level); return switch (wandAction) { case 0 -> !actual.isSideOpen(Direction.DOWN); case 1, 6 -> !actual.isSideOpen(Direction.NORTH) && !connector(level).isSideOpen(Direction.SOUTH); case 2 -> actual.isSideOpen(Direction.NORTH) && connector(level).isSideOpen(Direction.SOUTH); case 3 -> actual.getChoke(Direction.NORTH) == 1; case 4 -> actual.getChoke(Direction.NORTH) == 2; default -> actual.getChoke(Direction.NORTH) == 0; }; }
    private void verifyWand(ServerPlayer player, int action) { var level = player.level(); require(ItemStack.matches(player.getMainHandItem(), paidGold), "all actual wand port/choke operations debit ZERO vis"); if (action == 0) require(!buffer(level).isSideOpen(Direction.DOWN), "native indexed DOWN closure"); else if (action == 1 || action == 6) require(!buffer(level).isSideOpen(Direction.NORTH) && !connector(level).isSideOpen(Direction.SOUTH), "native NORTH closure assigns matching SOUTH raw flag"); else if (action == 2 || action == 8) require(buffer(level).isSideOpen(Direction.NORTH) && connector(level).isSideOpen(Direction.SOUTH), "native closed indexed NORTH arm reopens paired SOUTH flag"); else if (action == 7) require(buffer(level).isSideOpen(Direction.DOWN), "native indexed DOWN output reopens"); else { int expected = action == 3 ? 1 : action == 4 ? 2 : 0; require(buffer(level).getChoke(Direction.NORTH) == expected && buffer(level).getSuctionAmount(Direction.NORTH) == (expected == 1 ? 1 : expected == 2 ? 0 : 32) && buffer(level).getBellowsCount() == 1 && buffer(level).isSideOpen(Direction.NORTH), "actual sneak choke0->1->2->0 and original32/1/0/32 pressure"); } wandRows.add(Map.of("action", action, "native_tick", level.getGameTime(), "actual_count", buffer(level).getTickCount(), "DOWN_open", buffer(level).isSideOpen(Direction.DOWN), "NORTH_open", buffer(level).isSideOpen(Direction.NORTH), "paired_SOUTH_open", connector(level).isSideOpen(Direction.SOUTH), "actual_NORTH_choke", buffer(level).getChoke(Direction.NORTH), "actual_NORTH_suction", buffer(level).getSuctionAmount(Direction.NORTH), "actual_bellows", buffer(level).getBellowsCount())); }
    private void startInspect(int mode) { inspectMode = mode; next(Step.INSPECT_HAND); }
    private void recordFullMixedBuffer(ServerPlayer player) {
        // Never combine a SERVER_POST stock snapshot with a delayed client BE
        // packet. Read the actual public total and actual stock on ONE server
        // thread, with both native outputs still closed and the pool stable.
        Frame frame = ledger.snapshot(); TileTubeBuffer actual = buffer(player.level());
        require(frame.rawSmelted() == 2 && frame.buffer().equals(BUDGET) && frame.sourceUnits() == 0
                && !frame.downOpen() && !frame.northOpen() && frame.jarA() == 0 && frame.jarB() == 0
                && frame.connector() == 0 && actual.getAmount() == 8 && actual.getEssentiaAmount(Direction.NORTH) == 8,
                "same-server-thread original mixed8 public amount matches actual4IGNIS+4POTENTIA, not a stale client packet");
        report.put("actual_full_mixed_buffer", Map.of("actual_native_tick", frame.tick(), "actual_count", frame.count(),
                "actual_aspects", frame.buffer(), "actual_locations", frame.locations(),
                "public_total", actual.getEssentiaAmount(Direction.NORTH), "observation_side", "single server-thread snapshot",
                "RNG_or_essentia_writes", 0));
    }
    private BlockPos inspectionPos() { return inspectMode < 3 ? bufferPos : inspectMode == 3 ? jarA : jarB; }
    private Direction inspectionFace(Level level) { return inspectMode < 3 ? Direction.NORTH : inspectMode == 3 ? jar(level, 0).getFacing() : Direction.NORTH; }
    private Vec3 inspectionPoint(Level level) { return inspectMode < 3 ? armPoint(Direction.NORTH) : jarPoint(inspectionPos(), inspectionFace(level)); }
    private List<String> inspectionLines(ServerPlayer player) { var result = new ArrayList<String>(); Direction face = inspectionFace(player.level()); IEssentiaTransport transport; if (inspectMode < 3) { var actual = buffer(player.level()); AspectList stock = actual.getAspects(); for (Aspect type : stock.getAspectsSorted()) result.add(Component.translatable("tc.resonator1", Integer.toString(stock.getAmount(type)), type.getName()).getString()); transport = actual; int expected = inspectMode == 0 ? 1 : inspectMode == 1 ? 0 : 32; require(actual.getSuctionAmount(face) == expected && (inspectMode == 2 ? aspects(stock).equals(BUDGET) : stock.visSize() == 0), "actual buffer-only sorted multi-type branch/pressure, without random public type query"); } else { TileJar actual = jar(player.level(), inspectMode - 3); transport = actual; require(actual.getAmount() == 4 && actual.getAspect() == (inspectMode == 3 ? Aspect.ENERGY : Aspect.FIRE), "actual conserved four-unit typed jar before native inspection"); result.add(Component.translatable("tc.resonator1", Integer.toString(actual.getEssentiaAmount(face)), actual.getAspect().getName()).getString()); } Aspect suction = transport.getSuctionType(face); result.add(Component.translatable("tc.resonator2", Integer.toString(transport.getSuctionAmount(face)), suction == null ? Component.translatable("tc.resonator3").getString() : suction.getName()).getString()); return List.copyOf(result); }

    private final class ChargeProbe {
        final ServerPlayer player; final TileNode source; final ItemStack wand, original; final WandData before;
        final Map<String, Integer> sourceBefore, base, gained = new LinkedHashMap<>(), spent = new LinkedHashMap<>(), regenerated = new LinkedHashMap<>();
        final long started; WandData previous; Map<String, Integer> previousSource; boolean active = true; volatile boolean used, funded; volatile int stopped;
        ChargeProbe(ServerPlayer player) { this.player = player; source = node(player); wand = player.getMainHandItem(); original = wand.copy(); before = previous = ItemWandCasting.data(wand); sourceBefore = previousSource = aspects(source.getAspects()); base = aspects(source.getAspectsBase()); started = player.level().getGameTime(); require(ItemStack.matches(wand, savedGold) && !player.isUsingItem() && !ItemWandCasting.isUsingNode(player) && !player.isShiftKeyDown() && KnowledgeAccess.get(player).isResearchComplete("NODEPRESERVE"), "new exact saved-gold native preservation session"); require(sourceBefore.getOrDefault("ordo", 0) - 1 >= (900 - before.amount("ordo") + 99) / 100, "actual finite node ORDO budget, never reset"); }
        void observe() { require(player.getMainHandItem() == wand && sameExceptVis(wand, original) && player.level().getBlockEntity(chargingNode) == source && aspects(source.getAspectsBase()).equals(base) && player.level().getGameTime() - started <= 2600, "bounded same real wand/node/base charge"); WandData now = ItemWandCasting.data(wand); var current = aspects(source.getAspects()); int moved = 0, regenTotal = 0; for (String tag : WandData.PRIMALS) { int delta = now.amount(tag) - previous.amount(tag); require(delta >= 0 && delta <= 100 && (delta == 0 || delta == 100 || now.amount(tag) == now.capacity()), "only native whole/capped-partial charge " + tag); int units = (delta + 99) / 100, regen = current.getOrDefault(tag, 0) - previousSource.getOrDefault(tag, 0) + units; require(regen >= 0 && regen <= 1, "finite source plus actual native regeneration " + tag); if (delta > 0) gained.merge(tag, delta, Integer::sum); if (units > 0) spent.merge(tag, units, Integer::sum); if (regen > 0) regenerated.merge(tag, regen, Integer::sum); moved += units; regenTotal += regen; } require(moved <= 1 && regenTotal <= 1, "one native tap/at most one regen per real tick"); String session = "released"; if (player.isUsingItem()) { require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND && ItemWandCasting.isUsingNode(player) && serverSessionNode(player, wand).equals(chargingNode), "actual server-owned node session"); used = true; session = chargingNode.toShortString(); } if (moved > 0 || regenTotal > 0) chargeRows.add(Map.of("native_tick", player.level().getGameTime(), "wand_before", previous.vis(), "wand_after", now.vis(), "source_before", previousSource, "source_after", current, "actual_server_session", session)); funded = now.amount("aqua") >= 1350 && now.amount("ordo") >= 900 && now.amount("terra") >= 1440; stopped = used && !player.isUsingItem() ? stopped + 1 : 0; require(funded || stopped < 2, "native charge session stopped before funding; wand=" + now.vis() + "; source=" + current); previous = now; previousSource = current; }
        Map<String, Object> finish() { require(used && funded && stopped >= 2 && !ItemWandCasting.isUsingNode(player) && !gained.isEmpty(), "actual funded native recharge released"); var rounding = new LinkedHashMap<String, Integer>(); for (String tag : WandData.PRIMALS) { require(source.getAspects().getAmount(Aspect.getAspect(tag)) == sourceBefore.getOrDefault(tag, 0) - spent.getOrDefault(tag, 0) + regenerated.getOrDefault(tag, 0) && ItemWandCasting.data(wand).amount(tag) - before.amount(tag) == gained.getOrDefault(tag, 0), "ALL actual source units and primal centivis reconcile " + tag); rounding.put(tag, spent.getOrDefault(tag, 0) * 100 - gained.getOrDefault(tag, 0)); } active = false; return Map.of("actual_node_UUID", source.getScanId(), "source_before", sourceBefore, "source_after", aspects(source.getAspects()), "base_unchanged", base, "actual_centivis_gains", Map.copyOf(gained), "whole_source_units_spent", Map.copyOf(spent), "native_regeneration", Map.copyOf(regenerated), "native_cap_rounding", rounding, "wand_before", before.vis(), "wand_after", ItemWandCasting.data(wand).vis()); }
    }

    private record Frame(long tick, int count, int coal, int rawSmelted, int fuelSpent, List<Integer> rawSlots,
                         List<Integer> burnTimes, int bellows, int northChoke, int northPressure, boolean downOpen,
                         boolean northOpen, int sourceUnits, int connector, int jarA, int jarB,
                         Map<String, Integer> buffer, Map<String, Integer> stored, Map<String, Map<String, Integer>> locations) {}

    /** One passive sample per real SERVER_POST. Never queries the mixed buffer's
     * RNG-bearing public type getter or invokes a transfer/tick/storage backend.
     * Detached accounting maps are NEVER installed back into the ordered pool. */
    private final class Ledger {
        final ServerPlayer player;
        Frame previous;
        int rawSmelted, fuelSpent, checks, intoBuffer, toA, toB;
        boolean contestedMixed;
        final int[] rawPerFurnace = new int[2], fuelPerFurnace = new int[2];
        long lastFill = Long.MIN_VALUE / 2, lastA = Long.MIN_VALUE / 2, lastB = Long.MIN_VALUE / 2;
        Ledger(ServerPlayer player) { this.player = player; previous = snapshot(); checkBalance(previous); observed = previous; }
        void observe() {
            require(player.isAlive() && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded actual live buffer story, including screenshot waits");
            Frame now = snapshot(); int[] consumed = new int[2]; int fuelThisTick = 0, rawThisTick = 0;
            for (int index = 0; index < 2; index++) {
                if (now.burnTimes().get(index) > previous.burnTimes().get(index)) { require(fuelPerFurnace[index] == 0 && oven(player.level(), index).getCurrentItemBurnTime() == 1600 && !oven(player.level(), index).isSpeedBoost(), "ONE real normal-coal ignition per furnace"); ++fuelPerFurnace[index]; ++fuelThisTick; transfers.add(Map.of("native_tick", now.tick(), "event", "actual separate FUEL coal ignition", "furnace", index, "actual_coal_consumed", 1, "native_duration", 1600, "essentia_created", 0)); }
                consumed[index] = Math.max(0, previous.rawSlots().get(index) - now.rawSlots().get(index)); require(consumed[index] <= 1 && rawPerFurnace[index] + consumed[index] <= 1, "one physical RAW input consumption per furnace"); rawPerFurnace[index] += consumed[index]; rawThisTick += consumed[index];
            }
            int coalDebit = previous.coal() - now.coal(); require(coalDebit == fuelThisTick + rawThisTick && coalDebit >= 0 && coalDebit <= 4, "EVERY actual coal debit is independently witnessed native ignition or raw input-slot consumption"); rawSmelted += rawThisTick; fuelSpent += fuelThisTick;
            var emitted = List.of(new LinkedHashMap<String, Integer>(), new LinkedHashMap<String, Integer>()); int intake = 0;
            for (String tag : BUDGET.keySet()) {
                require(now.stored().getOrDefault(tag, 0) - previous.stored().getOrDefault(tag, 0) == rawThisTick * 2, "real raw coal creates EXACT2each; all later per-aspect transitions conserve units " + tag);
                int sourceOut = 0;
                for (int index = 0; index < 2; index++) {
                    String from = "furnace:" + index, source = "alembic:" + index;
                    int out = amount(previous, from, tag) + consumed[index] * 2 - amount(now, from, tag); require(out >= 0 && out <= 1, "each native furnace emits at most one of this type"); emitted.get(index).put(tag, out);
                    int toBuffer = amount(previous, source, tag) + out - amount(now, source, tag); require(toBuffer >= 0 && toBuffer <= 1, "actual alembic output is one native buffer intake"); sourceOut += toBuffer;
                    if (consumed[index] > 0) transfer(now, "actual RAW coal:" + index, from, tag, 2);
                    if (out > 0) transfer(now, from, source, tag, out);
                    if (toBuffer > 0) transfer(now, source, "buffer", tag, toBuffer);
                }
                intake += sourceOut;
                int aAdded = amount(now, "jarA", tag) - amount(previous, "jarA", tag);
                int bAdded = amount(now, "jarB", tag) - amount(previous, "jarB", tag);
                int toConnector = amount(now, "connector", tag) - amount(previous, "connector", tag) + bAdded;
                int bufferOut = amount(previous, "buffer", tag) + sourceOut - amount(now, "buffer", tag);
                require(aAdded >= 0 && aAdded <= 1 && bAdded >= 0 && bAdded <= 1 && toConnector >= 0 && toConnector <= 1 && bufferOut == aAdded + toConnector, "EACH real mixed-buffer output equals its stored destination delta, including same-tick connector pass-through " + tag);
                if (aAdded > 0) transfer(now, "buffer", "jarA", tag, aAdded);
                if (toConnector > 0) transfer(now, "buffer", "connector", tag, toConnector);
                if (bAdded > 0) transfer(now, "connector", "jarB", tag, bAdded);
            }
            for (int index = 0; index < 2; index++) require(emitted.get(index).values().stream().mapToInt(Integer::intValue).sum() <= 1, "one actual single-alembic emission per furnace tick");
            require(intake >= 0 && intake <= 1, "buffer native first eligible attempt stops: at most one successful fill per tick");
            if (intake > 0) { require(now.count() % 5 == 0 && now.tick() - lastFill >= 5, "actual buffer fills ONLY on its native local5tick phase"); lastFill = now.tick(); intoBuffer += intake; }
            int aAdded = now.jarA() - previous.jarA(), bAdded = now.jarB() - previous.jarB();
            if (rawSmelted == 2 && !previous.downOpen() && now.downOpen()) verifyWand(player, 7);
            if (rawSmelted == 2 && !previous.northOpen() && now.northOpen()) verifyWand(player, 8);
            if (aAdded > 0) { require(now.tick() - lastA >= 5, "jarA actual local5tick cadence"); lastA = now.tick(); toA += aAdded; }
            if (bAdded > 0) { require(now.tick() - lastB >= 5, "jarB actual local5tick cadence"); lastB = now.tick(); toB += bAdded; }
            if (now.bellows() != previous.bellows() && previous.count() > 0 && previous.bellows() >= 0) require(now.count() % 20 == 0, "bellows cache refresh happens on original native20tick count, never a query/fixture setter");
            if (now.downOpen() && now.northOpen() && now.bellows() == 0 && now.buffer().getOrDefault("potentia", 0) > 0 && now.buffer().getOrDefault("ignis", 0) > 0 && jar(player.level(), 0).getFilter() == Aspect.ENERGY && jar(player.level(), 0).getSuctionAmount(Direction.UP) == 64 && connector(player.level()).getSuctionAmount(Direction.SOUTH) == 31) {
                if (!contestedMixed) report.put("actual_competing_mixed_demands", Map.of("native_tick", now.tick(), "actual_buffer", now.buffer(), "actual_typed_jarA_demand", 64, "actual_untyped_jarB_demand", jar(player.level(), 1).getSuctionAmount(Direction.UP), "actual_connector_demand", 31, "actual_buffer_suction", now.northPressure(), "both_outputs_actually_open", true)); contestedMixed = true;
            }
            now = new Frame(now.tick(), now.count(), now.coal(), rawSmelted, fuelSpent, now.rawSlots(), now.burnTimes(), now.bellows(), now.northChoke(), now.northPressure(), now.downOpen(), now.northOpen(), now.sourceUnits(), now.connector(), now.jarA(), now.jarB(), now.buffer(), now.stored(), now.locations()); checkBalance(now);
            if (coalDebit > 0 || !now.locations().equals(previous.locations()) || now.bellows() != previous.bellows() || now.northChoke() != previous.northChoke() || now.downOpen() != previous.downOpen() || now.northOpen() != previous.northOpen()) balances.add(Map.of("native_tick", now.tick(), "actual_count", now.count(), "actual_coal", now.coal(), "actual_raw_consumed", rawSmelted, "actual_fuel_ignitions", fuelSpent, "actual_stored", now.stored(), "actual_locations", now.locations(), "actual_bellows", now.bellows(), "actual_NORTH_choke", now.northChoke(), "actual_NORTH_suction", now.northPressure()));
            ++checks; previous = now; observed = now;
        }
        Frame snapshot() {
            var level = player.serverLevel(); var locations = new LinkedHashMap<String, Map<String, Integer>>(); var raw = new ArrayList<Integer>(); var burns = new ArrayList<Integer>(); int sourceUnits = 0;
            for (int index = 0; index < 2; index++) { var oven = oven(level, index); var source = alembic(level, index); locations.put("furnace:" + index, aspects(oven.getAspects())); locations.put("alembic:" + index, singleton(source.getAspect(), source.getAmount())); sourceUnits += oven.getVis() + source.getAmount(); ItemStack input = oven.getItem(0); require(input.isEmpty() || input.is(Items.COAL) && input.getCount() == 1, "only the declared native RAW input in each new furnace"); raw.add(countStack(input, Items.COAL)); burns.add(oven.getBurnTime()); }
            TileTubeBuffer buffer = level.getBlockEntity(bufferPos) instanceof TileTubeBuffer value ? value : null; Map<String, Integer> pool = buffer == null ? Map.of() : aspects(buffer.getAspects()); int total = pool.values().stream().mapToInt(Integer::intValue).sum(); require(total >= 0 && total <= 8 && pool.keySet().stream().allMatch(BUDGET::containsKey), "actual mixed pool has8TOTAL capacity and real raw output types");
            if (buffer != null) { require(buffer.getAmount() == total, "EVERY native server snapshot has exact mixed total"); for (Direction face : Direction.values()) require(buffer.getEssentiaAmount(face) == total, "public sided amount reports SAME native mixed total even on closed ports " + face); }
            locations.put("buffer", pool);
            TileTube connector = level.getBlockEntity(connectorPos) instanceof TileTube value ? value : null; int connectorAmount = connector == null ? 0 : connector.getStoredEssentiaAmount(); require(connectorAmount >= 0 && connectorAmount <= 1, "actual capacity1 connector"); locations.put("connector", connector == null ? Map.of() : singleton(connector.getStoredEssentiaType(), connectorAmount)); locations.put("jarA", singleton(jar(level, 0).getAspect(), jar(level, 0).getAmount())); locations.put("jarB", singleton(jar(level, 1).getAspect(), jar(level, 1).getAmount()));
            var physical = physicalStacks(player); var physicalAspects = new LinkedHashMap<>(itemAspects(physical)); int afterBaseline = physicalAspects.getOrDefault("potentia", 0) - 8; require(afterBaseline >= 0, "saved POTENTIA8 catalyst physically retained across actual matrix/cursor/inventory/drop/pending ownership"); if (afterBaseline == 0) physicalAspects.remove("potentia"); else physicalAspects.put("potentia", afterBaseline); locations.put("new physical items", Map.copyOf(physicalAspects));
            int labels = physical.stream().mapToInt(value -> countStack(value, labelItem())).sum() + (jar(level, 0).getFilter() == null ? 0 : 1); require(labels == 1, "ONE actual saved label ownership: blank/bound physical OR paid jar filter OR real durable blank return"); int coal = physical.stream().mapToInt(value -> countStack(value, Items.COAL)).sum(); for (int index = 0; index < 2; index++) coal += countStack(oven(level, index).getItem(0), Items.COAL) + countStack(oven(level, index).getItem(1), Items.COAL);
            var stored = new LinkedHashMap<String, Integer>(); for (var map : locations.values()) map.forEach((tag, units) -> stored.merge(tag, units, Integer::sum)); return new Frame(level.getGameTime(), buffer == null ? 0 : buffer.getTickCount(), coal, rawSmelted, fuelSpent, List.copyOf(raw), List.copyOf(burns), buffer == null ? -1 : buffer.getBellowsCount(), buffer == null ? 0 : buffer.getChoke(Direction.NORTH), buffer == null ? 0 : buffer.getSuctionAmount(Direction.NORTH), buffer == null || buffer.isSideOpen(Direction.DOWN), buffer == null || buffer.isSideOpen(Direction.NORTH), sourceUnits, connectorAmount, jar(level, 0).getAmount(), jar(level, 1).getAmount(), pool, Map.copyOf(stored), Map.copyOf(locations));
        }
        void checkBalance(Frame frame) { require(frame.coal() == 4 - frame.rawSmelted() - frame.fuelSpent() && frame.rawSmelted() >= 0 && frame.rawSmelted() <= 2 && frame.fuelSpent() >= 0 && frame.fuelSpent() <= 2, "all four physical coals are retained or separately observed RAW/FUEL debits"); for (String tag : BUDGET.keySet()) require(BUDGET.get(tag) == 2 * (2 - frame.rawSmelted()) + frame.stored().getOrDefault(tag, 0), "EXACT supplied = unprocessed actual RAW potential + all stored, no loss/void/output credit " + tag); for (String tag : frame.stored().keySet()) require(BUDGET.containsKey(tag), "unexpected new stored aspect " + tag); }
        void transfer(Frame frame, String from, String to, String tag, int units) { transfers.add(Map.of("native_tick", frame.tick(), "from", from, "to", to, "aspect", tag, "supplied", units, "stored", units, "voided", 0, "read_only_observation", true)); }
    }

    private void startObservation(ServerPlayer player) {
        nativePost = server -> { if (server != player.server || observationFailure != null) return; try { if (charge != null && charge.active) charge.observe(); ledger.observe(); } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); } };
        nativeAdds = (entity, level) -> { if (level == player.level() && awaitingBlank && entity instanceof ItemEntity item && item.getItem().is(labelItem()) && patchBounds().contains(item.position())) try { require(blankDropId == null && item.getItem().getCount() == 1 && ItemJarLabel.data(item.getItem()) == null, "ONE real native blank label return"); blankDropId = item.getUUID(); blankDrop = item.getItem().copy(); } catch (RuntimeException | Error error) { observationFailure = error; } return EventResult.pass(); };
        serverStopped = server -> { if (server == player.server) { stopObservation(); owningClient.execute(() -> releaseInputs(owningClient)); } }; TickEvent.SERVER_POST.register(nativePost); EntityEvent.ADD.register(nativeAdds); LifecycleEvent.SERVER_STOPPED.register(serverStopped); ClientTickEvent.CLIENT_PRE.register(chargeGuard);
    }
    private void stopObservation() { if (nativePost != null) { TickEvent.SERVER_POST.unregister(nativePost); nativePost = null; } if (nativeAdds != null) { EntityEvent.ADD.unregister(nativeAdds); nativeAdds = null; } if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; } if (charge != null) charge.active = false; owningClient.execute(() -> ClientTickEvent.CLIENT_PRE.unregister(chargeGuard)); }
    private void guardCharge(Minecraft client) { try { if (client.player == null || client.level == null || client.gameMode == null) return; if (aimCharge && client.screen == null) sendRotation(client, Vec3.atCenterOf(chargingNode)); if (chargingHeld && !releaseSent && (charge != null && charge.funded || observationFailure != null || !client.isWindowActive() || client.screen != null || ++chargeTicks >= 2400)) releaseUse(client); } catch (RuntimeException | Error error) { observationFailure = error; releaseUse(client); } }
    private void releaseUse(Minecraft client) { chargingHeld = false; client.options.keyUse.setDown(false); if (!releaseSent && client.player != null && client.gameMode != null) { client.gameMode.releaseUsingItem(client.player); releaseSent = true; } }

    private void scanOld(ServerLevel level) {
        BlockPos min = center.offset(-12, -2, -18), max = center.offset(326, 10, 14);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        int jars = 0, units = 0, originalJars = 0, originalUnits = 0, candles = 0;
        for (BlockPos at : BlockPos.betweenClosed(min, max)) {
            oldBlocks.put(at.immutable(), level.getBlockState(at)); BlockEntity tile = level.getBlockEntity(at);
            if (tile instanceof TileNode) oldNodes.put(at.immutable(), stableNodeTag(tile)); else if (tile != null) oldBEs.put(at.immutable(), beTag(tile));
            if (tile instanceof TileJar jar) { ++jars; units += jar.getAmount(); if (at.getX() <= 1704) { ++originalJars; originalUnits += jar.getAmount(); } }
            if (level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get())) ++candles;
        }
        require(oldBEs.size() == 62 && jars == 16 && units == 317 && originalJars == 12 && originalUnits == 173 && candles == 72, "independent ACTUAL accepted M06b scan62non-nodeBE/16jars317/original12jars173/72candles");
        for (BlockPos at : OLD_CLOCKS) require(level.getBlockEntity(at) instanceof TileAlchemyFurnace old && old.getItem(0).isEmpty() && old.getBurnTime() >= 0 && old.getBurnTime() <= (at.getX() == 1786 ? 740 : 0), "ONLY actual old paid furnace clock may have naturally decayed since accepted stop; no old raw input " + at);
        require(level.getBlockEntity(new BlockPos(1465, 216, 377)) instanceof TileAlchemyFurnace original && original.getItem(0).isEmpty() && original.getItem(1).is(Items.COAL) && original.getItem(1).getCount() == 3 && original.getBurnTime() == 0, "original1465 furnace empty input/fuel3/B0 is NOT a clock exception");
        require(level.getBlockEntity(new BlockPos(1724, 216, 377)) instanceof TileJar oldVoid && oldVoid.isVoid() && oldVoid.getAmount() == 64 && oldVoid.getFilter() == Aspect.ENERGY, "accepted labelled void64, not any failed world");
        BlockPos valvePos = new BlockPos(1755, 217, 377);
        require(level.getBlockEntity(valvePos) instanceof TileTube valve && valve.isValve() && valve.isFlowAllowed() && !valve.hadPower() && valve.getFacing() == Direction.SOUTH && !level.hasNeighborSignal(valvePos), "accepted final unpowered/open old valve");
        require(level.getBlockEntity(new BlockPos(1756, 216, 377)) instanceof TileJar oldJar && oldJar.getAmount() == 8 && oldJar.getAspect() == Aspect.ENERGY, "prior native valve8POTENTIA jar retained");
        require(level.getBlockEntity(new BlockPos(1787, 218, 377)) instanceof TileTube oldFilter && oldFilter.isFilter() && oldFilter.getFilter() == null && oldFilter.getStoredEssentiaAmount() == 0 && oldFilter instanceof IAspectContainer marker && marker.getAspects() == null, "accepted old empty WHITE filter, never a hidden marker/storage fixture");
        require(level.getBlockEntity(new BlockPos(1786, 217, 377)) instanceof TileAlembic oldIgnis && oldIgnis.getAmount() == 8 && oldIgnis.getAspect() == Aspect.FIRE && level.getBlockEntity(new BlockPos(1789, 216, 377)) instanceof TileJar oldPotentia && oldPotentia.getAmount() == 8 && oldPotentia.getAspect() == Aspect.ENERGY && oldPotentia.getFilter() == null && level.getBlockEntity(new BlockPos(1789, 218, 377)) instanceof TileTube oldOneWay && oldOneWay.isOneWay() && oldOneWay.getFacing() == Direction.UP, "accepted actual lowerIGNIS8/upper-routed POTENTIA8/staticUP one-way, not either failed M06b client");
        report.put("actual_old_world_scan", Map.of("min", min.toShortString(), "max", max.toShortString(), "actual_non_node_BEs", oldBEs.size(), "actual_jars", jars, "actual_jar_units", units, "original_jars", originalJars, "original_units", originalUnits, "actual_candles", candles, "actual_three_old_clocks_RAW_snbt", OLD_CLOCKS.stream().map(at -> beTag(level.getBlockEntity(at)).toString()).toList()));
    }

    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) {
            if (ItemStack.isSameItemSameComponents(saved, savedGold)) continue;
            int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            int delta = saved.is(tubeItem()) ? -before : saved.is(emptyPhial()) ? 4 : 0;
            require(after == before + delta, "all saved main item components/counts EXACT except documented five real basics used and net+4 emptyphials " + saved);
        }
        for (int index = 0; index < ARMOR.size(); index++) require(ItemStack.matches(player.getItemBySlot(ARMOR.get(index)), savedArmor.get(index)), "exact old worn0/0/1/1");
        require(ItemStack.matches(player.getInventory().getItem(7), paidGold) && ItemStack.matches(stack(player, TubeBufferClientSequence::primary), savedPrimary) && ItemStack.matches(player.getOffhandItem(), savedOffhand) && ItemStack.matches(stack(player, value -> value.is(filledPhial())), savedPhial) && ItemStack.matches(stack(player, value -> value.is(ThaumcraftContent.RESONATOR.get())), savedResonator) && count(player, ThaumcraftContent.WAND.get()) == 3, "all actual old wands/foci/phial/resonator/apple retained, actual paid gold back7");
        require(player.totalExperience == initialXp && initialXp == 11 && player.isInvulnerable() == savedInvulnerability && player.isAlive() && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && expectedKnowledge.equals(KnowledgeAccess.get(player).save()), "XP11/Entity flag/Survival/all previous knowledge/pools/scans/warp exact; ONLY CENTRIFUGE completion added");
        for (var entry : oldBlocks.entrySet()) {
            BlockPos at = entry.getKey(); BlockState before = entry.getValue(), after = player.level().getBlockState(at); BlockPos cactus = center.offset(149, 0, 5);
            boolean column = at.getX() == cactus.getX() && at.getZ() == cactus.getZ() && at.getY() >= cactus.getY() && at.getY() <= cactus.getY() + 2;
            boolean growth = column && (before.is(Blocks.CACTUS) && after.is(Blocks.CACTUS) || before.isAir() && after.is(Blocks.CACTUS) && player.level().getBlockState(at.below()).is(Blocks.CACTUS));
            boolean clock = OLD_CLOCKS.contains(at) && before.getBlock() == after.getBlock() && before.setValue(BlockAlchemyFurnace.LIT, false) == after.setValue(BlockAlchemyFurnace.LIT, false);
            require(before == after || growth || clock, "all old blocks exact, only native cactus/LIT transients allowed " + at);
        }
        for (var entry : oldBEs.entrySet()) {
            BlockEntity actual = player.level().getBlockEntity(entry.getKey()); require(actual != null, "actual old BE remains " + entry.getKey()); CompoundTag after = beTag(actual);
            if (OLD_CLOCKS.contains(entry.getKey())) {
                CompoundTag beforeStable = entry.getValue().copy(), afterStable = after.copy(); beforeStable.remove("BurnTime"); afterStable.remove("BurnTime");
                require(beforeStable.equals(afterStable) && after.getShort("BurnTime") >= 0 && after.getShort("BurnTime") <= entry.getValue().getShort("BurnTime") && ((TileAlchemyFurnace) actual).getItem(0).isEmpty() && player.level().getBlockState(entry.getKey()).getValue(BlockAlchemyFurnace.LIT) == (((TileAlchemyFurnace) actual).getBurnTime() > 0), "ONLY old BurnTime/matching LIT may decay; exact fuel/items/essentia/identity, never another ignition");
                report.put("retained_old_clock_" + entry.getKey().toShortString(), Map.of("before_RAW_snbt", entry.getValue().toString(), "after_RAW_snbt", after.toString(), "only_whitelisted_fields", List.of("BurnTime", "matching block LIT")));
            } else require(after.equals(entry.getValue()), "all other accepted old BE fields EXACT " + entry.getKey());
        }
        for (var entry : oldNodes.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileNode && stableNodeTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "actual old node UUID/base/type retained; charge/regen separately accounted");
    }

    private void finalState(ServerPlayer player) {
        retained(player); Frame frame = observed; ledger.checkBalance(frame); verifyDebit(chargedGold, player.getInventory().getItem(7), TOTAL_PRICE);
        require(frame.rawSmelted() == 2 && frame.fuelSpent() == 2 && frame.coal() == 0 && frame.stored().equals(BUDGET) && ledger.intoBuffer == 8 && ledger.toA == 4 && ledger.toB == 4 && ledger.checks >= 500 && ledger.contestedMixed && charge != null && charge.funded && !charge.active && charge.stopped >= 2 && crafts.size() == 5, "all three paid crafts/two ordinary crafts/two real RAW/two real FUEL/all8 mixed intake/all8 arbitrated output independently observed");
        var level = player.level(); for (int index = 0; index < 2; index++) require(oven(level, index).isEmpty() && oven(level, index).getVis() == 0 && alembic(level, index).getAmount() == 0 && alembic(level, index).getFilter() == null && ledger.rawPerFurnace[index] == 1 && ledger.fuelPerFurnace[index] == 1, "both actual sources emptied, exactly one RAW and one FUEL coal per furnace");
        require(buffer(level).getAmount() == 0 && buffer(level).getAspects().visSize() == 0 && !buffer(level).isQuarantined() && connector(level).getStoredEssentiaAmount() == 0 && buffer(level).isSideOpen(Direction.DOWN) && buffer(level).isSideOpen(Direction.NORTH) && connector(level).isSideOpen(Direction.SOUTH), "actual mixed pool/connector empty, raw outputs genuinely reopened");
        for (Direction face : Direction.values()) require(buffer(level).getChoke(face) == 0 && buffer(level).isSideOpen(face), "final original default raw ports/chokes restored");
        require(jar(level, 0).getAmount() == 4 && jar(level, 0).getAspect() == Aspect.ENERGY && jar(level, 0).getFilter() == null && jar(level, 1).getAmount() == 4 && jar(level, 1).getAspect() == Aspect.FIRE && jar(level, 1).getFilter() == null && buffer(level).getBellowsCount() == 0 && buffer(level).getSuctionAmount(Direction.NORTH) == 1 && level.getBlockState(leverPos).getValue(LeverBlock.POWERED) && ((TileBellows) level.getBlockEntity(bellowsPos)).isPowered(), "actual fourPOTENTIA/fourIGNIS jars, blank returned, external lever remains visibly powered, native buffer suction1");
        require(count(player, tubeItem()) == 0 && count(player, emptyPhial()) == 7 && count(player, bufferItem()) == 0 && count(player, valveItem()) == 0 && count(player, restrictItem()) == 0 && count(player, labelItem()) == 1 && ItemJarLabel.data(stack(player, value -> value.is(labelItem()))) == null && count(player, material("vis_filter")) == 1 && count(player, material("quicksilver_drop")) == 7, "actual finite phials7, saved basics all five paid/placed, DROP7/VisFilter1 and ONE physical blank retained");
        for (Item raw : List.of(Items.GLASS, Items.CLAY_BALL, Items.STONE, Items.LEVER, Items.COAL)) require(count(player, raw) == 0, "no unaccounted declared RAW left in main inventory " + raw);
        require(blankDropId != null && blankDrop != null && inspectMode == 4 && inspections.size() == 5 && wandRows.size() == 9 && table(level).isEmpty() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty() && !player.isUsingItem() && !player.isShiftKeyDown() && !ItemWandCasting.isUsingNode(player), "all five real inspections/nine native indexed operations/physical blank recorded, menu/cursor/use/Shift/session released");
        require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && pendingStacks(player.serverLevel()).isEmpty(), "no lost, duplicated, pending physical outputs or XP grants");
        for (BlockPos at : torches) require(level.getBlockState(at).is(Blocks.TORCH), "all FOUR declared mundane static torch fixtures remain");
        report.put("final_exact_unit_accounting", Map.of("finite_RAW_potential", BUDGET, "actual_stored", frame.stored(), "actual_locations", frame.locations(), "actual_RAW_coal_consumed", frame.rawSmelted(), "actual_separate_FUEL_coal_ignited", frame.fuelSpent(), "actual_coal_remaining", frame.coal(), "native_POST_checks", ledger.checks, "actual_buffer_intake", ledger.intoBuffer, "actual_jar_delivery", Map.of("POTENTIA_A", ledger.toA, "IGNIS_B", ledger.toB), "voided_lost_or_granted", 0));
        report.put("final_inventory_and_price_ledger", Map.of("actual_gold7_RAW_snbt", snbt(player.getInventory().getItem(7), player), "actual_gold_vis", ItemWandCasting.data(player.getInventory().getItem(7)).vis(), "actual_charge_then_price", TOTAL_PRICE, "actual_saved_basics_consumed_recipes", 4, "actual_saved_basic_placed", 1, "actual_empty_phials_remaining", 7, "saved_DROP7_VisFilter1_exact", true, "old_POTENTIA8_phial_EXACT", snbt(stack(player, value -> value.is(filledPhial())), player), "actual_returned_blank_RAW_snbt", snbt(stack(player, value -> value.is(labelItem())), player), "saved_total_XP", player.totalExperience));
        report.put("new_devices_at_finish_RAW_snbt", Map.of("furnaces", furnaces.stream().map(at -> beTag(level.getBlockEntity(at)).toString()).toList(), "alembics", furnaces.stream().map(at -> beTag(level.getBlockEntity(at.above())).toString()).toList(), "buffer", beTag(buffer(level)).toString(), "connector", beTag(connector(level)).toString(), "jarA", beTag(jar(level, 0)).toString(), "jarB", beTag(jar(level, 1)).toString(), "bellows", beTag(level.getBlockEntity(bellowsPos)).toString(), "external_lever", level.getBlockState(leverPos).toString(), "future_save_caveat", "New furnaces1818/1820 may still burn their one already-paid ordinary FUEL coal each. Next continuation must allow native BurnTime/matching LIT decay, never freeze/reset clocks or permit extra fuel ignition. Their inputs are empty and all8 output is actually in the two jars."));
        report.put("retained_prior_outputs", Map.of("actual_old_non_node_BEs", oldBEs.size(), "actual_old16jars317_original12jars173", true, "actual_old72candles", true, "old_empty_WHITE_filter_lowerIGNIS8_POTENTIA8jar_UP_oneway", true, "all_old_wands_foci_armor_phial_resonator_apple", true, "ONLY_three_old_native_clock_exceptions", OLD_CLOCKS.stream().map(BlockPos::toShortString).toList(), "original1465_fuel3_B0_EXACT", true, "all_prior_knowledge_pools_scans_warp", true));
        report.put("actual_saved_node_after_story_RAW_snbt", beTag(node(player)).toString()); finalVerified = true;
    }

    private void approachBlank(ServerPlayer player) {
        if (blankDropId == null || count(player, labelItem()) == 1) return;
        require(player.serverLevel().getEntity(blankDropId) instanceof ItemEntity item && ItemStack.matches(item.getItem(), blankDrop), "same real blank UUID until physical pickup, never a replacement refund"); ItemEntity item = (ItemEntity) player.serverLevel().getEntity(blankDropId);
        // Both jars retain original FULL entity collision; the neighbouring
        // furnaces/bellows can obstruct all four cardinal approaches. Search
        // diagonals too, within the player's ordinary item-contact envelope.
        // The physical item/UUID/velocity/pickup delay are NEVER moved or reset.
        double[] offsets = {0, .8, -.8, 1.05, -1.05, 1.2, -1.2};
        for (int rise = 0; rise <= 1; rise++) for (double x : offsets) for (double z : offsets) {
            Vec3 at = new Vec3(item.getX() + x, workbench.getY() + rise, item.getZ() + z);
            AABB body = player.getBoundingBox().move(at.subtract(player.position()));
            BlockPos floor = BlockPos.containing(at.x, at.y - .01, at.z);
            if (!patchBounds().contains(at) || !player.level().noCollision(player, body)
                    || player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty()
                    || !body.inflate(1, .5, 1).intersects(item.getBoundingBox())) continue;
            report.put("actual_blank_pickup_approach", Map.of("UUID", item.getUUID().toString(),
                    "unmoved_item_at", item.position().toString(), "collision_checked_player_at", at.toString(),
                    "existing_support", floor.toShortString(), "within_native_contact_envelope", true,
                    "item_relocations_or_output_grants", 0));
            stand(player, at.x, at.y, at.z); return;
        }
        throw new IllegalStateException("No collision-free native approach to actual blank at " + item.position());
    }
    private BlockPos patchMin() { return workbench.offset(-4, -2, -7); }
    private BlockPos patchMax() { return workbench.offset(17, 7, 8); }
    private AABB patchBounds() { return new AABB(Vec3.atLowerCornerOf(patchMin()), Vec3.atLowerCornerOf(patchMax().offset(1, 1, 1))); }
    private void patch(ServerLevel level) {
        for (int x = patchMin().getX() >> 4; x <= patchMax().getX() >> 4; x++) for (int z = patchMin().getZ() >> 4; z <= patchMax().getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, patchBounds()).isEmpty() && pendingStacks(level).isEmpty(), "new fixture patch refuses old physical/pending items/XP/entities");
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft") && !oldBlocks.containsKey(at), "never overwrite any old BE/TC/accepted scanned cell " + at);
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
    private int supply(ServerPlayer player, Item item, int amount, String purpose) {
        require(item == Items.GLASS && amount == 3 || (item == Items.CLAY_BALL || item == Items.STONE || item == Items.COAL) && amount == 1 || item == Items.LEVER && amount == 2, "ONLY declared finite RAW fixtures, NEVER finished device/output/vis/essentia/XP");
        int slot = emptyMain(player); player.getInventory().setItem(slot, new ItemStack(item, amount)); inputs.add(Map.of("raw_item", BuiltInRegistries.ITEM.getKey(item).toString(), "count", amount, "purpose", purpose, "genuinely_empty_main_slot", slot)); return slot;
    }
    private List<ItemStack> physicalStacks(ServerPlayer player) {
        var result = new ArrayList<ItemStack>(); result.addAll(player.getInventory().items); result.addAll(player.getInventory().offhand); result.add(player.containerMenu.getCarried());
        // Actual table matrix0..8 and payer10 are storage. Preview9 is NOT.
        var table = table(player.level()); for (int cell = 0; cell < 9; cell++) result.add(table.getItem(cell)); result.add(table.getItem(TileArcaneWorkbench.WAND_SLOT));
        for (ItemEntity item : player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds())) result.add(item.getItem()); result.addAll(pendingStacks(player.serverLevel())); return result;
    }
    private static Map<String, Integer> itemAspects(List<ItemStack> stacks) {
        var result = new LinkedHashMap<String, Integer>();
        for (ItemStack value : stacks) { if (ItemJar.isJar(value)) merge(result, ItemJar.getAspect(value), ItemJar.getAmount(value) * value.getCount()); if (value.is(filledPhial())) { var data = ItemEssence.data(value); require(data != null && Aspect.getAspect(data.tag()) != null, "actual native filled-phial component"); merge(result, Aspect.getAspect(data.tag()), PhialData.AMOUNT * value.getCount()); } }
        return Map.copyOf(result);
    }
    @SuppressWarnings("unchecked") private List<ItemStack> pendingStacks(ServerLevel level) {
        try {
            var field = DeferredItemDrops.class.getDeclaredField("FACTORY"); field.setAccessible(true); var factory = (SavedData.Factory<DeferredItemDrops>) field.get(null); var queue = level.getDataStorage().get(factory, "thaumcraft_pending_inventory_drops"); if (queue == null) return List.of();
            var result = new ArrayList<ItemStack>(); for (Tag raw : queue.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) { CompoundTag row = (CompoundTag) raw; if (!patchBounds().contains(Vec3.atCenterOf(BlockPos.of(row.getLong("Source"))))) continue; ItemStack value = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElseThrow(); value.setCount(row.getInt("Count")); result.add(value); } return result;
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only pending-output view needs root API reconciliation", error); }
    }
    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages"); ResourceLocation wanted = id("arcane/essentia_buffer");
        for (int index = page; index < Math.min(page + 2, pages.size()); index++) if (BookClientSmoke.field(pages.get(index), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(wanted)) { require(display.recipe() == client.level.getRecipeManager().byKey(wanted).orElseThrow(), "native book uses the actual synced buffer recipe holder"); return true; }
        require(++turns <= 18, "bounded native CENTRIFUGE buffer-recipe navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow(); client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false;
    }
    private void capture(int index) { require(captures == index, "exact contiguous eight122-129 capture order"); ++captures; report.put("actual_captures_requested", captures); capture.accept(CAPTURES.get(index)); }
    @SuppressWarnings("unchecked") private static List<GuiMessage> chat(Minecraft client) throws ReflectiveOperationException { return List.copyOf((List<GuiMessage>) BookClientSmoke.field(client.gui.getChat(), "allMessages")); }
    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside menus"); if (!wanted.test(client.player.getInventory().getItem(HAND_SLOT))) { click(client, playerSlot(client, itemSlot(client.player, wanted)), HAND_SLOT, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, HAND_SLOT); return true;
    }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException { return hand(client, ItemStack::isEmpty); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static int playerSlot(Minecraft client, int inventory) { return client.player.containerMenu.slots.stream().filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void sendRotation(Minecraft client, Vec3 point) { lookAt(client, point); client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround())); }
    private void beginAim(Minecraft client, BlockPos pos, Direction face, Vec3 point) { sendRotation(client, point); useAimPos = pos.immutable(); useAimFace = face; useAimPoint = point; useAimStarted = client.player.tickCount; }
    private boolean use(Minecraft client, BlockPos pos, Direction face, Vec3 point) {
        require(client.player.canInteractWithBlock(pos, 0), "actual native Survival reach " + pos); sendRotation(client, point);
        if (!pos.equals(useAimPos) || face != useAimFace || !point.equals(useAimPoint)) { beginAim(client, pos, face, point); return false; }
        // Applies to EVERY action: real C2S body rotation must settle through
        // six ordinary client ticks before strict native head/view retracing.
        if (client.player.tickCount - useAimStarted < 6) return false;
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)).consumesAction(), "native useItemOn accepted " + pos + "/" + face);
        useAimPos = null; useAimFace = null; useAimPoint = null; return true;
    }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 delta = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void stand(ServerPlayer player, double x, double y, double z) { var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z); require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "collision-checked camera above existing declared floor"); player.teleportTo(x, y, z); }
    // Slightly east of the nearer jar's tangent: jarB must not occlude jarA,
    // but the label-use yaw still faces SOUTH, making jarA's paid face NORTH.
    private void northCamera(ServerPlayer player) { stand(player, bufferPos.getX() + 2.85, workbench.getY(), bufferPos.getZ() - 2.4); }
    private void powerCamera(ServerPlayer player) { stand(player, bellowsPos.getX() + .5, workbench.getY(), bellowsPos.getZ() + 3); }
    private void furnaceCamera(ServerPlayer player, int index) { BlockPos at = furnaces.get(index); stand(player, at.getX() + .5, workbench.getY(), at.getZ() - 2.5); }
    private Vec3 armPoint(Direction side) { return Vec3.atCenterOf(bufferPos).add(side.getStepX() * .35, side.getStepY() * .35, side.getStepZ() * .35); }
    private static Vec3 jarPoint(BlockPos at, Direction face) { return Vec3.atCenterOf(at).add(face.getStepX() * .3125, -.1, face.getStepZ() * .3125); }
    private static void releaseKeys(Minecraft client) { client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); }
    private void releaseInputs(Minecraft client) { releaseUse(client); aimCharge = false; primeNorth = false; ClientTickEvent.CLIENT_PRE.unregister(chargeGuard); releaseKeys(client); if (client.player != null && client.gameMode != null) client.gameMode.stopDestroyBlock(); if (previousCamera != null) client.options.setCameraType(previousCamera); }
    private static BlockPos serverSessionNode(ServerPlayer player, ItemStack wand) { try { var field = ItemWandCasting.class.getDeclaredField("SERVER_NODE_USES"); field.setAccessible(true); Object session = ((Map<?, ?>) field.get(null)).get(player); require(session != null && BookClientSmoke.field(session, "hand") == InteractionHand.MAIN_HAND && BookClientSmoke.field(session, "stack") == wand, "actual SERVER-owned node session exact hand/stack identity"); return ((BlockPos) BookClientSmoke.field(session, "pos")).immutable(); } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only saved node-session view needs reconciliation", error); } }
    private static boolean sameExceptVis(ItemStack first, ItemStack second) { WandData firstData = ItemWandCasting.data(first), secondData = ItemWandCasting.data(second); if (!firstData.rod().equals(secondData.rod()) || !firstData.cap().equals(secondData.cap())) return false; ItemStack a = first.copy(), b = second.copy(); a.remove(ThaumcraftContent.WAND_DATA.get()); b.remove(ThaumcraftContent.WAND_DATA.get()); return ItemStack.matches(a, b); }
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price) { require(sameExceptVis(before, after), "all non-vis/focus/craft-cost components exact"); WandData first = ItemWandCasting.data(before), last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same actual saved payer materials"); for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0), "exact actual finite centivis debit " + tag); }
    private static CompoundTag beTag(BlockEntity tile) { return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy(); }
    private static CompoundTag stableNodeTag(BlockEntity tile) { CompoundTag tag = beTag(tile); tag.remove("Aspects"); tag.remove("lastActive"); return tag; }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect type : list.getAspects()) merge(result, type, list.getAmount(type)); return Map.copyOf(result); }
    private static Map<String, Integer> singleton(Aspect type, int amount) { var result = new LinkedHashMap<String, Integer>(); merge(result, type, amount); return Map.copyOf(result); }
    private static int amount(Frame frame, String location, String tag) { return frame.locations().getOrDefault(location, Map.of()).getOrDefault(tag, 0); }
    private static void merge(Map<String, Integer> result, Aspect type, int amount) { require(amount >= 0 && (amount == 0 || type != null), "real storage never contains negative filter-marker/untyped amounts"); if (amount > 0) result.merge(type.getTag(), amount, Integer::sum); }
    private static boolean gold(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("greatwood") && ItemWandCasting.data(value).cap().equals("gold"); }
    private static boolean primary(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int slot = 0; slot < 36; slot++) if (wanted.test(player.getInventory().getItem(slot))) return slot; throw new IllegalStateException("Missing actual finite buffer input/output"); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().mapToInt(value -> countStack(value, item)).sum(); }
    private static int countStack(ItemStack value, Item item) { return value.is(item) ? value.getCount() : 0; }
    private static int emptyMain(Player player) { for (int slot = 9; slot < 36; slot++) if (player.getInventory().getItem(slot).isEmpty()) return slot; throw new IllegalStateException("No genuinely empty main inventory slot"); }
    private static Item tubeItem() { return ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(); }
    private static Item valveItem() { return ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(); }
    private static Item restrictItem() { return ThaumcraftTubes.ESSENTIA_RESTRICT_ITEM.get(); }
    private static Item bufferItem() { return ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get(); }
    private static Item material(String name) { return ThaumcraftContent.MATERIALS.get(name).get(); }
    private static Item labelItem() { return material("jar_label"); }
    private static Item filledPhial() { return ThaumcraftContent.FILLED_PHIAL.get(); }
    private static Item emptyPhial() { return ThaumcraftContent.EMPTY_PHIAL.get(); }
    private TileTubeBuffer buffer(Level level) { return (TileTubeBuffer) level.getBlockEntity(bufferPos); }
    private TileTube connector(Level level) { return (TileTube) level.getBlockEntity(connectorPos); }
    private TileAlchemyFurnace oven(Level level, int index) { return (TileAlchemyFurnace) level.getBlockEntity(furnaces.get(index)); }
    private TileAlembic alembic(Level level, int index) { return (TileAlembic) level.getBlockEntity(furnaces.get(index).above()); }
    private TileJar jar(Level level, int index) { return (TileJar) level.getBlockEntity(index == 0 ? jarA : jarB); }
    private TileArcaneWorkbench table(Level level) { return (TileArcaneWorkbench) level.getBlockEntity(workbench); }
    private TileNode node(ServerPlayer player) { return (TileNode) player.level().getBlockEntity(chargingNode); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ContainerAlchemyFurnace furnaceMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String snbt(ItemStack value, ServerPlayer player) { return value.save(player.registryAccess()).toString(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "no overlapping fixture/read-only server actions"); if (serverAction != null) serverAction.join(); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; thaumcraft.common.Thaumcraft.LOG.info("Tube-buffer client step={} craft={} wand={} inspect={} furnace={}", step, craftIndex, wandAction, inspectMode, furnaceIndex); }
}
