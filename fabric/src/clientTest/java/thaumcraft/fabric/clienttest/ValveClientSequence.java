package thaumcraft.fabric.clienttest;

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
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.blocks.BlockAlchemyFurnace;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
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
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.TubeGeometry;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileTube;

/** Seven focused scenes in root's NEW private copy of the accepted void-jar save. */
public final class ValveClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_CAPTURE, BENCH_TRAVEL, BENCH_OPEN, FUND, FUNDED,
        GRID, PREVIEW, PREVIEW_ACK, TAKE, STORE, STORED, WAND_PICK, WAND_RETURN, CRAFT_DONE,
        NETWORK_TRAVEL, VALVE_PLACE, VALVE_ACK, TUBE_PLACE, TUBE_ACK, LEVER_PLACE, LEVER_ACK,
        WAND_AIM, WAND_USE, WAND_ACK, OPEN_CAPTURE, INITIAL_CLOSE, INITIAL_CLOSED,
        FURNACE_TRAVEL, FURNACE_OPEN, FUEL_PICK, FUEL_PUT, INPUT_PICK, INPUT_PUT, BURNING, DISTILL,
        INSPECT_HAND, INSPECT_SHIFT, INSPECT_READY, INSPECT_USE, INSPECT_ACK,
        FIRST_OPEN, FIRST_FLOW, ACTIVE_CLOSE, CLOSED_SETTLE, CLOSED_CAPTURE,
        EDGE_REOPEN, EDGE_REOPENED, LEVER_ON, RISING_ACK, POWERED_REOPEN, POWERED_ACK, POWERED_STABLE,
        POWERED_CAPTURE, POWERED_CLOSE, POWERED_CLOSED, LEVER_OFF, FALLING_ACK, DRAIN,
        GOLD_RETURN, FINAL_CHECK, FINAL_ACK, FINAL_CAPTURE, DONE
    }

    private static final List<String> CAPTURES = List.of(
            "107-valve-original-shapeless-tube-lever-native-book-recipe-en.png",
            "108-saved-tube-arbitrary-cells-native-shapeless-valve-450aqua-450ordo-en.png",
            "109-native-iron-quartz-stick-resonator-450aqua-450aer-en.png",
            "110-earned-valve-native-stem-wheel-wand-core-and-paired-arms-en.png",
            "111-native-hand-closed-valve-settled-zero-suction-residual-buffer-drain-en.png",
            "112-native-powered-manual-reopen-resonator-secondary-jar-chat-en.png",
            "113-native-valve-resumed-eight-unit-transfer-retained-voidjar-world-en.png");
    private static final Map<String, Integer> GOLD_VIS = Map.of("aer", 500, "aqua", 1760, "ignis", 500, "ordo", 500, "perditio", 950, "terra", 2300);
    private static final Map<String, Integer> FROST_VIS = Map.of("aer", 1951, "aqua", 2436, "ignis", 2846, "ordo", 933, "perditio", 431, "terra", 464);
    private static final Map<String, Integer> BUDGET = Map.of("ignis", 8, "potentia", 8);
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final int HAND_SLOT = 3;

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final Minecraft owningClient;
    private final Consumer<String> capture;
    private final BlockPos workbench, center, furnace, valvePos, tubePos, jarPos, leverPos, oldHotFurnace;
    private final long startedNanos = System.nanoTime();
    private final Map<BlockPos, BlockState> oldBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> oldBEs = new LinkedHashMap<>(), oldNodes = new LinkedHashMap<>();
    private final List<ItemStack> savedStacks = new ArrayList<>(), savedArmor = new ArrayList<>();
    private final List<Map<String, Object>> supplied = new ArrayList<>(), balances = new ArrayList<>(), transfers = new ArrayList<>(), samples = new ArrayList<>(), inspections = new ArrayList<>(), animations = new ArrayList<>();
    private Step step = Step.HANDOFF, afterInspect;
    private CompletableFuture<Void> serverAction;
    private TickEvent.Server nativePre, nativePost;
    private LifecycleEvent.ServerState serverStopped;
    private CameraType previousCamera;
    private volatile Throwable observationFailure;
    private volatile Frame observed;
    private volatile PreFrame beforeTick;
    private volatile boolean manualMayChange;
    private volatile int nativeSamples, risingCloses, fallingOpens;
    private Ledger ledger;
    private ItemStack savedGold, paidGold, savedPrimary, savedOffhand, earnedResonator;
    private CompoundTag savedKnowledge;
    private Map<String, Integer> baselinePhysical;
    private List<int[]> gridClicks = List.of();
    private List<GuiMessage> chatBefore = List.of();
    private List<String> expectedChat = List.of();
    private int ticks, totalTicks, captures, turns, craftIndex, gridIndex, destination, wandAction, inspectIndex;
    private int rawCoalSlot, fuelCoalSlot, initialXp, sampleBaseline, closedJar, closedSource, closedTotal, closedAtCount;
    private int handChangedAt = Integer.MIN_VALUE / 2;
    private int lastClientRotation = -1, lastClientTick = -1;
    private long closedTick;
    private boolean savedInvulnerability, finalVerified, sawOpening, sawClosing;

    public ValveClientSequence(Minecraft client, Consumer<String> capture, BlockPos bench, BlockPos oldCenter) {
        owningClient = client; this.capture = capture; workbench = bench.immutable(); center = oldCenter.immutable();
        furnace = workbench.east(8); valvePos = furnace.above().east(); tubePos = valvePos.east(); jarPos = tubePos.below(); leverPos = valvePos.below();
        oldHotFurnace = new BlockPos(1722, 216, 377);
        report.put("scope", "valve: ONLY a NEW valve-smoke-UUID private copy of accepted STOPPED voidjar-smoke-3eb55479-602a-4902-853b-58f93c50c093; prior acquisition is NOT replayed");
        report.put("accepted_input_record", "docs/porting/voidjar-client.json; accepted source save is never opened/edited");
        report.put("boundary", "ACTUAL saved gold payer7/3 loose tubes/3 wands/all foci/worn robes+goggles/XP11/knowledge and complete accepted voidjar patch are inputs. Consume one saved tube in a real shapeless valve craft; place one more saved basic tube and retain the third. NEW RAW fixtures: iron4, nether quartz1, stick1, RECIPE lever1, EXTERNAL redstone lever1, four raw coal and one separate ordinary fuel coal. Empty new table/furnace/two alembics/one ordinary jar, bounded stone floor/air patch and collision-checked camera are declared fixtures. No valve/resonator/vis/essentia/XP/output grants, world/day/fuel clock writes, weather overrides, manual ticks or RNG control.");
        report.put("native_actions", "Native C2S menu clicks and BlockItem placements; actual client rotation sent via native movement packet BEFORE each scripted use packet (no server angle setter); wand core cycling and closed indexed arm reopening with paired raw-side assignment; empty-hand valve toggles; real lever edges observed against PRE tube count%5; stable-powered manual reopen over20 actual samples; item-first resonator on alembic/tube/valve/jar including secondary-use. ArcaneCrafting.find/ObjectAspectResolver and SERVER_PRE/POST observations are read-only, never craft or repair state.");
        report.put("coordinates", Map.of("bench", workbench.toShortString(), "furnace", furnace.toShortString(), "valve", valvePos.toShortString(), "basic_tube", tubePos.toShortString(), "jar", jarPos.toShortString(), "external_lever", leverPos.toShortString(), "old_center", center.toShortString()));
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size()); report.put("supplied_raw_inputs_and_fixtures", supplied);
        report.put("passive_exact_balances", balances); report.put("passive_native_transfers", transfers); report.put("native_PREcount_power_samples", samples); report.put("actual_resonator_chat", inspections); report.put("native_client_rotation_observations", animations);
        report.put("limits", Map.of("wall_seconds", 600, "client_ticks", 10000, "ordinary_step_ticks", 400, "distillation_ticks", 6000, "pipe_wait_ticks", 1200, "preview_fail_fast_ticks", 40, "powered_manual_reopen_samples", 20));
        report.put("retention_caveat", "Old furnace1722,216,377 has EMPTY input, one coal fuel and8IGNIS, below full32IGNIS alembic. TileAlchemyFurnace95-113 legitimately decrements BurnTime/LIT after reload; ONLY that BurnTime and matching LIT are whitelisted. Empty input makes canSmelt=false, so ignite cannot spend its retained coal. All items/aspects/filters/coordinates/BE identity fields remain exact. Nodes may naturally regenerate; saved cactus AGE/at-most-three-block native growth allowed. No freeze/reset/restoration writes.");
        report.put("visual_inspection", "Pending root inspection of exact107-113 PNGs. Retained original pipe_valve.png (actual original registration), valve.png stem, tick-stepped wheel rotation/travel. No full GUI/lighting/FX/audio parity claim before inspection.");
        report.put("not_covered", List.of("earlier research/material/survival acquisition", "reload/callback/vent/malformed/creative matrices covered separately by server cases", "filter/restricted/oneway/buffer tubes", "ordinary packaged install or remote server", "independently heard audio or full legacy art parity", "NeoForge"));
    }

    public boolean tick(Minecraft client) throws ReflectiveOperationException {
        try { return advance(client); }
        catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client);
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Valve story failed at " + step + "; craft=" + craftIndex + "; wand=" + wandAction + "; inspect=" + inspectIndex + "; observed=" + observed, error);
            throw error;
        }
    }

    private boolean advance(Minecraft client) throws ReflectiveOperationException {
        require(client == owningClient, "same root-owned client"); ++ticks; ++totalTicks;
        int limit = step == Step.DISTILL ? 6000 : step == Step.DRAIN ? 1200 : 400;
        require(ticks <= limit && totalTicks <= 10000 && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded native valve story " + step + "; ticks=" + ticks + "/" + limit);
        if (observationFailure != null) throw new IllegalStateException("Passive valve accounting failed", observationFailure);
        observeAnimation(client);
        if (step == Step.WAND_AIM || step == Step.WAND_USE || step == Step.WAND_ACK) lookAt(client, wandPoint());
        switch (step) {
            case HANDOFF -> {
                var path = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(); String name = path.getFileName().toString();
                require(name.startsWith("valve-smoke-"), "only NEW root-owned private valve world, never accepted voidjar source"); UUID.fromString(name.substring("valve-smoke-".length()));
                require(path.getParent().getFileName().toString().equals("saves") && path.getParent().getParent().getFileName().toString().equals("run-clienttest"), "isolated client-test save"); report.put("actual_owned_world", path.toString());
                previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON); client.setScreen(null); releaseKeys(client);
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "empty inherited native menu/cursor"); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> { if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("TUBES")) return false; client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("TUBES"))); next(Step.BOOK); }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_CAPTURE); }
            case BOOK_CAPTURE -> { report.put("native_book", Map.of("research", "TUBES", "actual_synced_recipe", id("arcane/essentia_valve").toString(), "shapeless", List.of("one ACTUAL saved basic tube", "one declared RECIPE lever"), "base_whole_vis", Map.of("aqua", 5, "ordo", 5))); next(Step.BENCH_TRAVEL); capture(0); }
            case BENCH_TRAVEL -> { client.setScreen(null); onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.BENCH_OPEN); }
            case BENCH_OPEN -> { if (!serverDone() || ticks < 12) return false; select(client, 7); use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5)); next(Step.FUND); }
            case FUND -> { if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false; for (int slot = 0; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "genuinely empty new workbench"); click(client, playerSlot(client, 7), 0, ClickType.QUICK_MOVE); next(Step.FUNDED); }
            case FUNDED -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getSlot(1).getItem(), savedGold)) return false; beginGrid(client); }
            case GRID -> { if (ticks < 2) return false; if (gridIndex == gridClicks.size()) { next(Step.PREVIEW); return false; } int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0; }
            case PREVIEW -> {
                if (ticks < 8) return false;
                if (!arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craftOutput())) { require(ticks < 40, "native preview missing after40ticks: " + craftRecipe() + "; grid=" + java.util.stream.IntStream.range(2, 11).mapToObj(slot -> arcane(client).getSlot(slot).getItem().toString()).toList()); return false; }
                require(arcane(client).getCarried().isEmpty() && arcane(client).getSlot(0).getItem().getCount() == 1, "one genuine result, empty cursor"); verifyGrid(arcane(client), false); onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.PREVIEW_ACK);
            }
            case PREVIEW_ACK -> { if (!serverDone()) return false; for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == craftPrice().getOrDefault(tag, 0), "actual worn-gear actor price " + tag); next(Step.TAKE); capture(craftIndex + 1); }
            case TAKE -> { click(client, 0, 0, ClickType.PICKUP); next(Step.STORE); }
            case STORE -> { if (ticks < 6 || !arcane(client).getCarried().is(craftOutput())) return false; require(arcane(client).getCarried().getCount() == 1, "one native result extraction"); verifyGrid(arcane(client), true); destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.STORED); }
            case STORED -> { if (ticks < 6 || !arcane(client).getCarried().isEmpty() || !client.player.getInventory().getItem(destination).is(craftOutput())) return false; verifyDebit(paidGold, arcane(client).getSlot(1).getItem(), craftPrice()); paidGold = arcane(client).getSlot(1).getItem().copy(); if (++craftIndex < 2) beginGrid(client); else { click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK); } }
            case WAND_PICK -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), paidGold)) return false; require(client.player.getInventory().getItem(7).isEmpty(), "payer returns to saved slot7"); click(client, playerSlot(client, 7), 0, ClickType.PICKUP); next(Step.WAND_RETURN); }
            case WAND_RETURN -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) return false; client.player.closeContainer();
                onServer(client, () -> { var player = serverPlayer(client); require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "both actual crafts leave empty table/cursor"); verifyDebit(savedGold, player.getInventory().getItem(7), Map.of("aqua", 900, "ordo", 450, "aer", 450)); require(count(player, valveItem()) == 1 && count(player, resonator()) == 1 && count(player, tubeItem()) == 2 && count(player, Items.LEVER) == 1 && count(player, Items.IRON_INGOT) == 0 && count(player, Items.QUARTZ) == 0 && count(player, Items.STICK) == 0, "saved tube1/RECIPE lever1/raw resonator ingredients actually consumed once"); earnedResonator = stack(player, value -> value.is(resonator())).copy(); report.put("native_crafts", Map.of("paid_total_centivis", Map.of("aqua", 900, "ordo", 450, "aer", 450), "saved_tubes_after_recipe", 2, "external_lever_retained", 1, "actual_resonator", snbt(earnedResonator, player), "actual_gold_after", snbt(player.getInventory().getItem(7), player), "output_grants", 0)); }); next(Step.CRAFT_DONE);
            }
            case CRAFT_DONE -> { if (!serverDone() || ticks < 8) return false; onServer(client, () -> machineCamera(serverPlayer(client))); next(Step.NETWORK_TRAVEL); }
            case NETWORK_TRAVEL -> { if (!serverDone() || ticks < 12 || !hand(client, value -> value.is(valveItem()))) return false; next(Step.VALVE_PLACE); }
            case VALVE_PLACE -> { use(client, furnace.above(), Direction.EAST, Vec3.atCenterOf(furnace.above()).add(.5, 0, 0)); next(Step.VALVE_ACK); }
            case VALVE_ACK -> { if (ticks < 6 || !(client.level.getBlockEntity(valvePos) instanceof TileTube valve)) return false; require(valve.isValve() && valve.getFacing() == Direction.EAST && valve.isFlowAllowed() && !valve.hadPower() && !valve.isConnectable(Direction.EAST) && valve.isSideOpen(Direction.EAST), "actual valve placement clicked EAST; stem exclusion is NOT raw-side closure"); next(Step.TUBE_PLACE); }
            case TUBE_PLACE -> { if (!hand(client, value -> value.is(tubeItem()))) return false; use(client, valvePos, Direction.EAST, Vec3.atCenterOf(valvePos).add(.125, 0, 0)); next(Step.TUBE_ACK); }
            case TUBE_ACK -> { if (ticks < 6 || !(client.level.getBlockEntity(tubePos) instanceof TileTube tube)) return false; require(!tube.isValve() && tube.getFacing() == Direction.EAST && count(client.player, tubeItem()) == 1, "one more ACTUAL saved tube placed; third retained, never assume six inputs"); next(Step.LEVER_PLACE); }
            case LEVER_PLACE -> { if (!hand(client, value -> value.is(Items.LEVER))) return false; use(client, leverPos.below(), Direction.UP, Vec3.atCenterOf(leverPos.below()).add(0, .5, 0)); next(Step.LEVER_ACK); }
            case LEVER_ACK -> { if (ticks < 6 || !client.level.getBlockState(leverPos).is(Blocks.LEVER)) return false; require(!client.level.getBlockState(leverPos).getValue(LeverBlock.POWERED) && count(client.player, Items.LEVER) == 0, "only declared EXTERNAL lever really placed, starts unpowered"); next(Step.WAND_AIM); }
            case WAND_AIM -> { if (!serverDone() || !hand(client, value -> ItemStack.matches(value, paidGold)) || ticks < 8) return false; var hit = TubeGeometry.retrace(client.player, valve(client.level)); require(hit != null && hit.index() == (wandAction < 4 ? 6 : Direction.EAST.get3DDataValue()), "real indexed wand ray, not supplied clicked SOUTH face; action=" + wandAction + "; hit=" + hit); next(Step.WAND_USE); }
            case WAND_USE -> { manualMayChange = true; use(client, valvePos, Direction.SOUTH, wandPoint()); next(Step.WAND_ACK); }
            case WAND_ACK -> {
                if (ticks < 6) return false; var valve = valve(client.level); Direction wanted = switch (wandAction) { case 0 -> Direction.DOWN; case 1 -> Direction.UP; case 2 -> Direction.NORTH; default -> Direction.SOUTH; };
                if (valve.getFacing() != wanted || wandAction >= 4 && (valve.isSideOpen(Direction.EAST) != (wandAction == 5) || tube(client.level).isSideOpen(Direction.WEST) != (wandAction == 5))) return false;
                int completedAction = wandAction;
                onServer(client, () -> { var player = serverPlayer(client); var actual = valve(player.level()); require(actual.getFacing() == wanted && actual.isSideOpen(wanted) && !actual.isConnectable(wanted) && actual.isFlowAllowed(), "wand core chooses unoccupied wheel face; raw stem remains independently open"); if (completedAction >= 4) require(actual.isSideOpen(Direction.EAST) == (completedAction == 5) && tube(player.level()).isSideOpen(Direction.WEST) == (completedAction == 5), "indexed EAST close AND reopen assign identical opposite raw flags"); require(ItemStack.matches(player.getMainHandItem(), paidGold), "wand orientation/paired arms charge NO vis"); manualMayChange = false; report.put("native_wand_action_" + completedAction, Map.of("facing", wanted.getName(), "raw_EAST", actual.isSideOpen(Direction.EAST), "neighbor_raw_WEST", tube(player.level()).isSideOpen(Direction.WEST), "actual_count", actual.getTickCount(), "real_indexed_ray", true)); });
                next(++wandAction < 6 ? Step.WAND_AIM : Step.OPEN_CAPTURE);
            }
            case OPEN_CAPTURE -> { if (!serverDone() || ticks < 8) return false; require(valve(client.level).getRotation() == 0 && valve(client.level).getFacing() == Direction.SOUTH, "open wheel at native zero rotation"); lookAt(client, Vec3.atCenterOf(valvePos)); next(Step.INITIAL_CLOSE); capture(3); }
            case INITIAL_CLOSE -> { if (!serverDone() || !emptyHand(client)) return false; toggle(client); next(Step.INITIAL_CLOSED); }
            case INITIAL_CLOSED -> { if (ticks < 8 || valve(client.level).isFlowAllowed() || observed == null || observed.flow() || observed.suction() != 0) return false; manualMayChange = false; onServer(client, () -> stand(serverPlayer(client), furnace.getX() + .5, workbench.getY(), furnace.getZ() + 3.2)); next(Step.FURNACE_TRAVEL); }
            case FURNACE_TRAVEL -> { if (!serverDone() || ticks < 12 || !emptyHand(client)) return false; use(client, furnace, Direction.SOUTH, Vec3.atCenterOf(furnace).add(0, 0, .5)); next(Step.FURNACE_OPEN); }
            case FURNACE_OPEN -> { if (ticks < 8 || !(client.screen instanceof GuiAlchemyFurnace)) return false; require(furnaceMenu(client).getSlot(0).getItem().isEmpty() && furnaceMenu(client).getSlot(1).getItem().isEmpty(), "genuinely empty NEW furnace slots"); click(client, playerSlot(client, fuelCoalSlot), 0, ClickType.PICKUP); next(Step.FUEL_PICK); }
            case FUEL_PICK -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == 1, "declared single FUEL coal, not raw stack"); click(client, 1, 0, ClickType.PICKUP); next(Step.FUEL_PUT); }
            case FUEL_PUT -> { if (ticks < 5 || !furnaceMenu(client).getCarried().isEmpty()) return false; click(client, playerSlot(client, rawCoalSlot), 0, ClickType.PICKUP); next(Step.INPUT_PICK); }
            case INPUT_PICK -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == 4, "all and only four declared RAW coal"); click(client, 0, 0, ClickType.PICKUP); next(Step.INPUT_PUT); }
            case INPUT_PUT -> { if (ticks < 8 || !furnaceMenu(client).isBurning()) return false; require(!furnaceMenu(client).isSpeedBoost() && furnaceMenu(client).getCurrentItemBurnTime() == 1600 && furnaceMenu(client).getBellowsCount() == 0 && furnaceMenu(client).getSmeltTime() == 40, "native coal2IGNIS+2POTENTIA,40ticks, ordinary1600fuel, no clock shortcut"); client.player.closeContainer(); onServer(client, () -> machineCamera(serverPlayer(client))); next(Step.BURNING); }
            case BURNING -> { if (!serverDone() || ticks < 12) return false; next(Step.DISTILL); }
            case DISTILL -> { Frame frame = observed; if (frame == null || frame.rawSmelted() != 4 || !frame.locations().getOrDefault("furnace", Map.of()).isEmpty() || alembic(client.level, 1).getAmount() != 8 || alembic(client.level, 2).getAmount() != 8) return false; require(!frame.flow() && frame.suction() == 0 && jar(client.level).getAmount() == 0 && alembic(client.level, 1).getAspect() != alembic(client.level, 2).getAspect(), "native unseeded type order; closed valve leaves both actual8 sources intact"); report.put("native_distillation", Map.of("actual_lower", alembic(client.level, 1).getAspect().getTag(), "actual_upper", alembic(client.level, 2).getAspect().getTag(), "each_actual_amount", 8, "supplied_budget", BUDGET, "RNG_changes", 0)); startInspect(0, Step.FIRST_OPEN); }
            case INSPECT_HAND -> { if (!serverDone() || !hand(client, value -> value.is(resonator()))) return false; client.options.keyShift.setDown(inspectIndex >= 2); next(Step.INSPECT_SHIFT); }
            case INSPECT_SHIFT -> { if (ticks < 6 || client.player.isShiftKeyDown() != (inspectIndex >= 2)) return false; lookAt(client, inspectionPoint()); chatBefore = chat(client); onServer(client, () -> expectInspection(serverPlayer(client))); next(Step.INSPECT_READY); }
            case INSPECT_READY -> { if (!serverDone() || ticks < 6) return false; lookAt(client, inspectionPoint()); next(Step.INSPECT_USE); }
            case INSPECT_USE -> { use(client, inspectionPos(), Direction.SOUTH, inspectionPoint()); client.options.keyShift.setDown(false); next(Step.INSPECT_ACK); }
            case INSPECT_ACK -> {
                if (ticks < 8 || client.player.isShiftKeyDown()) return false; List<GuiMessage> actual = chat(client).stream().filter(value -> !chatBefore.contains(value)).toList();
                if (actual.size() < expectedChat.size()) return false;
                List<String> lines = actual.stream().map(value -> value.content().getString()).toList(); require(lines.size() == expectedChat.size() && lines.containsAll(expectedChat), "real server resonator chat exactly once, no synthetic/duplicate lines: expected=" + expectedChat + "; actual=" + lines);
                int completedInspection = inspectIndex; BlockPos target = inspectionPos(); List<String> expected = expectedChat;
                onServer(client, () -> { var player = serverPlayer(client); require(ItemStack.matches(player.getMainHandItem(), earnedResonator) && valve(player.level()).isFlowAllowed() == (completedInspection == 3), "real item-first inspection neither damages resonator nor toggles valve"); inspections.add(Map.of("target", target.toShortString(), "kind", completedInspection == 0 ? "alembic" : completedInspection == 1 ? "basic tube" : completedInspection == 2 ? "valve secondary-use" : "jar secondary-use", "secondary", completedInspection >= 2, "actual_chat", lines, "server_expected_from_actual_sided_storage", expected, "native_tick", player.level().getGameTime(), "messages_injected", 0)); });
                if (inspectIndex < 2) startInspect(inspectIndex + 1, afterInspect); else next(afterInspect);
            }
            case FIRST_OPEN -> { if (!serverDone() || !emptyHand(client)) return false; toggle(client); next(Step.FIRST_FLOW); }
            case FIRST_FLOW -> { Frame frame = observed; require(ticks < 40 || frame != null && frame.flow(), "native empty-hand opening must reach the server within40ticks; last=" + frame); if (frame == null || !frame.flow() || frame.jar() < 1 || !valve(client.level).isFlowAllowed()) return false; require(frame.source() > 0, "first actual flow leaves finite upstream material for closure/resume"); manualMayChange = false; next(Step.ACTIVE_CLOSE); }
            case ACTIVE_CLOSE -> { if (!emptyHand(client)) return false; toggle(client); next(Step.CLOSED_SETTLE); }
            case CLOSED_SETTLE -> {
                Frame frame = observed; if (frame == null || frame.flow() || frame.suction() != 0 || frame.settledChecks() < 20 || valve(client.level).getRotation() != 360) return false;
                require(frame.source() > 0 && frame.jar() > 0 && frame.jar() < 8, "actual closed cut retains upstream units, after allowing old valve/downstream buffers to drain");
                closedJar = frame.jar(); closedSource = frame.source(); closedTotal = frame.source() + frame.valve() + frame.tube() + frame.jar(); closedTick = frame.tick(); closedAtCount = frame.count(); manualMayChange = false;
                report.put("native_closed_after_settle", Map.of("actual_native_tick", closedTick, "actual_count", closedAtCount, "effective_suction", frame.suction(), "source_units", closedSource, "jar_units", closedJar, "valve_buffer", frame.valve(), "downstream_buffer", frame.tube(), "consecutive_native_zero_checks", frame.settledChecks(), "buffer_drain_is_allowed", true)); next(Step.CLOSED_CAPTURE);
            }
            case CLOSED_CAPTURE -> { lookAt(client, Vec3.atCenterOf(valvePos)); next(Step.EDGE_REOPEN); capture(4); }
            case EDGE_REOPEN -> { if (!emptyHand(client)) return false; toggle(client); next(Step.EDGE_REOPENED); }
            case EDGE_REOPENED -> { if (observed == null || !observed.flow() || !valve(client.level).isFlowAllowed()) return false; manualMayChange = false; sampleBaseline = nativeSamples; next(Step.LEVER_ON); }
            case LEVER_ON -> { use(client, leverPos, Direction.SOUTH, Vec3.atCenterOf(leverPos).add(0, -.25, .15)); next(Step.RISING_ACK); }
            case RISING_ACK -> { Frame frame = observed; if (frame == null || nativeSamples <= sampleBaseline || !frame.power() || !frame.hadPower() || frame.flow() || valve(client.level).isFlowAllowed()) return false; require(risingCloses > 0, "real rising edge closes OPEN valve on original PREcount%5 sample"); report.put("native_rising_edge", Map.of("actual_count", frame.count(), "native_samples", nativeSamples - sampleBaseline, "hadpower", frame.hadPower(), "flow", frame.flow(), "suction_after_sample", frame.suction())); next(Step.POWERED_REOPEN); }
            case POWERED_REOPEN -> { if (!emptyHand(client)) return false; toggle(client); next(Step.POWERED_ACK); }
            case POWERED_ACK -> { Frame frame = observed; if (frame == null || !frame.flow() || !frame.power() || !frame.hadPower() || !valve(client.level).isFlowAllowed()) return false; manualMayChange = false; sampleBaseline = nativeSamples; next(Step.POWERED_STABLE); }
            case POWERED_STABLE -> { Frame frame = observed; require(frame != null && frame.power() && frame.hadPower() && frame.flow(), "stable power does NOT continuously force closure after a real manual reopen"); if (nativeSamples - sampleBaseline < 20 || valve(client.level).getRotation() != 0 || jar(client.level).getAmount() != 8) return false; report.put("native_stable_power_manual_override", Map.of("actual_samples", nativeSamples - sampleBaseline, "actual_count", frame.count(), "flow", frame.flow(), "hadpower", frame.hadPower(), "power", frame.power(), "actual_jar_after_resumed_transfer", frame.jar())); startInspect(3, Step.POWERED_CAPTURE); }
            case POWERED_CAPTURE -> { if (!serverDone() || ticks < 8) return false; require(valve(client.level).isFlowAllowed() && client.level.getBlockState(leverPos).getValue(LeverBlock.POWERED), "actual powered manual reopen remains visible"); lookAt(client, Vec3.atCenterOf(valvePos)); client.setScreen(new ChatScreen("")); next(Step.POWERED_CLOSE); capture(5); }
            case POWERED_CLOSE -> { client.setScreen(null); if (!emptyHand(client)) return false; toggle(client); next(Step.POWERED_CLOSED); }
            case POWERED_CLOSED -> { if (ticks < 6 || observed == null || observed.flow() || !observed.power() || !observed.hadPower() || valve(client.level).isFlowAllowed()) return false; manualMayChange = false; sampleBaseline = nativeSamples; next(Step.LEVER_OFF); }
            case LEVER_OFF -> { use(client, leverPos, Direction.SOUTH, Vec3.atCenterOf(leverPos).add(0, -.25, .15)); next(Step.FALLING_ACK); }
            case FALLING_ACK -> { Frame frame = observed; if (frame == null || nativeSamples <= sampleBaseline || frame.power() || frame.hadPower() || !frame.flow() || !valve(client.level).isFlowAllowed()) return false; require(fallingOpens > 0, "real falling edge reopens CLOSED valve on original PREcount%5 sample"); report.put("native_falling_edge", Map.of("actual_count", frame.count(), "actual_samples", nativeSamples - sampleBaseline, "flow", frame.flow(), "hadpower", frame.hadPower())); next(Step.DRAIN); }
            case DRAIN -> { Frame frame = observed; if (frame == null || frame.jar() != 8 || frame.source() != 0 || frame.valve() != 0 || frame.tube() != 0 || valve(client.level).getRotation() != 0) return false; require(frame.jar() > closedJar && closedSource > frame.source(), "real transfer resumed from finite saved closure baseline, not a supplied output"); next(Step.GOLD_RETURN); }
            case GOLD_RETURN -> { if (!ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) { click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, paidGold))), 7, ClickType.SWAP); return false; } if (ticks < 8 || !emptyHand(client)) return false; next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> { if (!serverDone() || ticks < 8) return false; require(finalVerified && nativePre == null && nativePost == null && serverStopped == null && captures == 6, "all passive hooks removed; six preceding captures"); lookAt(client, Vec3.atCenterOf(valvePos).add(-.3, .1, 0)); next(Step.FINAL_CAPTURE); }
            case FINAL_CAPTURE -> { if (ticks < 8) return false; report.put("native_client_ticks", totalTicks); report.put("elapsed_wall_seconds", (System.nanoTime() - startedNanos) / 1_000_000_000D); next(Step.DONE); capture(6); }
            case DONE -> { require(captures == CAPTURES.size(), "exact seven captures107-113"); client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(center.equals(new BlockPos(1474, 216, 377)) && workbench.equals(new BlockPos(1746, 216, 377)), "root passes NEW bench1746,216,377 and accepted center1474,216,377");
        var level = player.serverLevel(); require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && player.isAlive(), "actual accepted Survival, not creative conversion");
        initialXp = player.totalExperience; savedInvulnerability = player.isInvulnerable(); require(initialXp == 11 && savedInvulnerability, "accepted XP11/Entity Invulnerable1 retained");
        savedGold = player.getInventory().getItem(7).copy(); paidGold = savedGold.copy(); savedPrimary = stack(player, ValveClientSequence::primary).copy(); savedOffhand = player.getOffhandItem().copy();
        require(gold(savedGold) && ItemWandCasting.data(savedGold).vis().equals(GOLD_VIS) && FocusData.get(savedGold).isEmpty(), "EXACT accepted gold7 finite vis, no topup");
        require(ItemWandCasting.data(savedPrimary).vis().equals(FROST_VIS) && FocusData.get(savedPrimary).is(ThaumcraftFoci.FROST_FOCUS.get()), "EXACT accepted primary Frost payer, never used");
        require(count(player, tubeItem()) == 3 && count(player, valveItem()) == 0 && count(player, resonator()) == 0 && count(player, ThaumcraftContent.WAND.get()) == 3 && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 3 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 1 && savedOffhand.is(Items.APPLE) && savedOffhand.getCount() == 1, "actual3 loose tubes/3wands/3empty+one filled phial/apple; NOT six tubes");
        require(count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.SHOCK_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.EXCAVATION_FOCUS.get()) == 1, "all earlier earned foci retained");
        var worn = List.of(ThaumcraftWearables.GOGGLES.get(), ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get());
        for (int index = 0; index < ARMOR.size(); index++) { ItemStack value = player.getItemBySlot(ARMOR.get(index)); require(value.is(worn.get(index)) && value.getDamageValue() == (index >= 2 ? 1 : 0), "accepted worn gear0/0/1/1"); savedArmor.add(value.copy()); }
        require(WandVisDiscount.percentagePoints(player, Aspect.WATER) == 10 && (int) (500 * WandVisDiscount.modifier(savedGold, player, Aspect.WATER)) == 450 && (int) (500 * WandVisDiscount.modifier(savedGold, player, Aspect.AIR)) == 450 && (int) (500 * WandVisDiscount.modifier(savedGold, player, Aspect.ORDER)) == 450, "actual saved worn gear charges4.5 each, no invented cost");
        for (ItemStack value : player.getInventory().items) if (!value.isEmpty()) savedStacks.add(value.copy());
        savedKnowledge = KnowledgeAccess.get(player).save().copy(); require(KnowledgeAccess.get(player).isResearchComplete("TUBES") && KnowledgeAccess.get(player).isResearchComplete("JARVOID"), "reuse actually accepted TUBES/JARVOID, no research replay"); KnowledgeEvents.sync(player);
        scanOld(level); patch(level);
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState()); level.setBlockAndUpdate(furnace, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
        for (int depth = 1; depth <= 2; depth++) level.setBlockAndUpdate(furnace.above(depth), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(jarPos, ThaumcraftContent.JAR.get().defaultBlockState());
        require(oven(level).isEmpty() && oven(level).getVis() == 0 && alembic(level, 1).getAmount() == 0 && alembic(level, 2).getAmount() == 0 && alembic(level, 1).getFilter() == null && alembic(level, 2).getFilter() == null && jar(level).getAmount() == 0 && jar(level).getFilter() == null, "ONLY empty new devices, never contents/filter setters");
        require(aspects(ObjectAspectResolver.get(new ItemStack(Items.COAL), level)).equals(Map.of("ignis", 2, "potentia", 2)), "native actual coal tags2IGNIS+2POTENTIA, not a coal-block assumption");
        for (Item item : List.of(Items.IRON_INGOT, Items.QUARTZ, Items.STICK, Items.LEVER, Items.COAL)) require(count(player, item) == 0, "fixture cannot conflate existing saved stack " + item);
        supply(player, Items.IRON_INGOT, 4, "resonator RAW"); supply(player, Items.QUARTZ, 1, "resonator RAW nether quartz"); supply(player, Items.STICK, 1, "resonator RAW"); supply(player, Items.LEVER, 2, "one RECIPE lever consumed; one EXTERNAL redstone lever retained/placed"); rawCoalSlot = supply(player, Items.COAL, 4, "RAW distillation inputs"); fuelCoalSlot = supply(player, Items.COAL, 1, "separate normal furnace FUEL");
        verifyRecipes(player); baselinePhysical = itemAspects(physicalStacks(player));
        machineCamera(player); player.inventoryMenu.broadcastChanges();
        supplied.add(Map.of("fixture", "empty table/furnace/two alembics/one ordinary jar; separate stone floor/air patch; collision-checked camera", "actual_unmodified_day_time", level.getDayTime(), "day_time_writes", 0, "game_time_writes", 0, "weather_writes", 0, "RNG_writes", 0, "research_grants", 0));
        report.put("actual_saved_inputs", Map.of("gold7", snbt(savedGold, player), "main_Frost", snbt(savedPrimary, player), "main_stacks", savedStacks.stream().map(value -> snbt(value, player)).toList(), "worn", savedArmor.stream().map(value -> snbt(value, player)).toList(), "offhand", snbt(savedOffhand, player), "XP", initialXp, "knowledge_snbt", savedKnowledge.toString()));
        ledger = new Ledger(player); startObservation(player);
    }

    private Item ingredient(int cell, int craft) { if (craft == 0) return cell == 0 ? Items.LEVER : cell == 8 ? tubeItem() : Items.AIR; return switch (cell) { case 0, 2, 3, 5 -> Items.IRON_INGOT; case 4 -> Items.QUARTZ; case 7 -> Items.STICK; default -> Items.AIR; }; }
    private Item craftOutput() { return craftIndex == 0 ? valveItem() : resonator(); }
    private ResourceLocation craftRecipe() { return id(craftIndex == 0 ? "arcane/essentia_valve" : "arcane/resonator"); }
    private Map<String, Integer> craftPrice() { return craftIndex == 0 ? Map.of("aqua", 450, "ordo", 450) : Map.of("aqua", 450, "aer", 450); }
    private void beginGrid(Minecraft client) {
        var clicks = new ArrayList<int[]>(); var types = new LinkedHashSet<Item>(); for (int cell = 0; cell < 9; cell++) if (ingredient(cell, craftIndex) != Items.AIR) types.add(ingredient(cell, craftIndex));
        for (Item item : types) { int from = playerSlot(client, itemSlot(client.player, value -> value.is(item))); clicks.add(new int[]{from, 0}); for (int cell = 0; cell < 9; cell++) if (ingredient(cell, craftIndex) == item) clicks.add(new int[]{cell + 2, 1}); clicks.add(new int[]{from, 0}); }
        gridClicks = List.copyOf(clicks); gridIndex = 0; next(Step.GRID);
    }
    private void verifyGrid(ContainerArcaneWorkbench menu, boolean empty) { for (int cell = 0; cell < 9; cell++) { ItemStack actual = menu.getSlot(cell + 2).getItem(); Item wanted = ingredient(cell, craftIndex); require(empty || wanted == Items.AIR ? actual.isEmpty() : actual.is(wanted) && actual.getCount() == 1, "real native grid/consumption cell " + cell); } }
    private void verifyRecipes(ServerPlayer player) {
        var checks = new ArrayList<Map<String, Object>>();
        for (int craft = 0; craft < 2; craft++) {
            var projection = new SimpleContainer(11); for (int cell = 0; cell < 9; cell++) { Item item = ingredient(cell, craft); if (item != Items.AIR) projection.setItem(cell, stack(player, value -> value.is(item)).copyWithCount(1)); } projection.setItem(TileArcaneWorkbench.WAND_SLOT, savedGold.copy());
            var match = ArcaneCrafting.find(player, projection); ResourceLocation wanted = id(craft == 0 ? "arcane/essentia_valve" : "arcane/resonator");
            require(match != null && match.arcane() && match.recipe().id().equals(wanted) && match.output().is(craft == 0 ? valveItem() : resonator()) && match.output().getCount() == 1 && match.prices().equals(craft == 0 ? Map.of("aqua", 450, "ordo", 450) : Map.of("aqua", 450, "aer", 450)), "read-only actual recipe preflight before clicks " + wanted);
            if (craft == 0) require(match.recipe().value().getSerializer() == BuiltInRegistries.RECIPE_SERIALIZER.get(id("arcane_shapeless")), "genuine registered shapeless serializer, not shaped diagonal permutation");
            checks.add(Map.of("actual_recipe", wanted.toString(), "detached_actual_input_copies", match.input().items().stream().map(ItemStack::toString).toList(), "native_prices", match.prices(), "read_only", true));
        }
        report.put("read_only_recipe_preflight", checks);
    }
    private void inspectCraft(ServerPlayer player) {
        var match = ArcaneCrafting.find(player, (TileArcaneWorkbench) player.level().getBlockEntity(workbench));
        require(match != null && match.arcane() && match.recipe().id().equals(craftRecipe()) && match.prices().equals(craftPrice()) && match.output().is(craftOutput()), "actual native menu recipe/prices");
        report.put("native_preview_" + craftIndex, Map.of("actual_recipe", match.recipe().id().toString(), "centivis", match.prices(), "actual_input", match.input().items().stream().map(ItemStack::toString).toList(), "actual_output", snbt(match.output(), player), "payer_before", snbt(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).getItem(10), player), "shapeless_arbitrary_diagonal_cells", craftIndex == 0, "backend_craft_calls", 0));
    }

    private void startInspect(int index, Step after) { inspectIndex = index; afterInspect = after; next(Step.INSPECT_HAND); }
    private BlockPos inspectionPos() { return switch (inspectIndex) { case 0 -> furnace.above(); case 1 -> tubePos; case 2 -> valvePos; default -> jarPos; }; }
    private Vec3 inspectionPoint() { return Vec3.atCenterOf(inspectionPos()).add(0, inspectIndex == 3 ? -.1 : 0, inspectIndex == 3 ? .3125 : .125); }
    private void expectInspection(ServerPlayer player) {
        var tile = player.level().getBlockEntity(inspectionPos()); require(tile instanceof IEssentiaTransport, "actual resonator transport target"); IEssentiaTransport transport = (IEssentiaTransport) tile; Direction side = Direction.SOUTH;
        if (tile instanceof TileTube tube) { var hit = TubeGeometry.retrace(player, tube); if (hit != null && hit.index() >= 0 && hit.index() < 6) side = Direction.from3DDataValue(hit.index()); }
        var expected = new ArrayList<String>(); Aspect type = transport.getEssentiaType(side), suctionType = transport.getSuctionType(side);
        if (type != null) expected.add(Component.translatable("tc.resonator1", "" + transport.getEssentiaAmount(side), type.getName()).getString());
        expected.add(Component.translatable("tc.resonator2", "" + transport.getSuctionAmount(side), suctionType == null ? Component.translatable("tc.resonator3").getString() : suctionType.getName()).getString());
        expectedChat = List.copyOf(expected);
    }

    private record PreFrame(long tick, int count, boolean power, boolean flow, boolean hadPower, boolean manual) {}
    private record Frame(long tick, int count, int coal, int rawSmelted, int fuelSpent, int burnTime, boolean power,
                         boolean flow, boolean hadPower, int suction, int source, int valve, int tube, int jar,
                         int settledChecks, Map<String, Integer> stored, Map<String, Map<String, Integer>> locations) {}

    /** Every tick counts physical coal in inventory/cursor/furnace/drops/pending,
     * and separates a real 1600-tick ignition from real raw-input consumption.
     * No expected value is installed in a raw snapshot or gameplay state. */
    private final class Ledger {
        final ServerPlayer player;
        Frame previous;
        int rawSmelted, fuelSpent, checks, settledChecks, jarUnits;
        long lastJarPull = Long.MIN_VALUE / 2;
        Ledger(ServerPlayer player) { this.player = player; previous = snapshot(); checkBalance(previous); observed = previous; }
        void observe() {
            require(player.isAlive() && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded live owned story server, including capture waits");
            Frame now = snapshot(); int coalDebit = previous.coal() - now.coal(); require(coalDebit >= 0 && coalDebit <= 2, "only real ignition/raw-smelt coal debits, no new grants/loss");
            int ignition = now.burnTime() > previous.burnTime() ? 1 : 0;
            if (ignition == 1) { require(oven(player.level()).getCurrentItemBurnTime() == 1600 && !oven(player.level()).isSpeedBoost() && fuelSpent == 0 && coalDebit >= 1, "ONE native ordinary coal ignition accounts actual physical debit"); ++fuelSpent; transfers.add(Map.of("native_tick", now.tick(), "from", "separate FUEL coal", "to", "native BurnTime1600", "coal_consumed", 1, "essentia_produced", 0)); }
            int smelted = coalDebit - ignition; require(smelted >= 0 && smelted <= 1, "at most one native raw smelt per tick"); rawSmelted += smelted; require(rawSmelted <= 4 && fuelSpent <= 1, "finite four RAW + one FUEL only");
            for (String tag : BUDGET.keySet()) require(now.stored().getOrDefault(tag, 0) - previous.stored().getOrDefault(tag, 0) == smelted * 2, "each actual RAW coal creates EXACT2 of each; transfers preserve all physical essentia " + tag);
            if (smelted > 0) transfers.add(Map.of("native_tick", now.tick(), "from", "actual raw coal input", "to", "furnace buffer", "raw_consumed", 1, "actual_native_tags", Map.of("ignis", 2, "potentia", 2)));
            if (player.level().getBlockEntity(valvePos) instanceof TileTube valve) {
                PreFrame pre = beforeTick;
                if (pre != null && pre.tick() == player.server.getTickCount() && pre.power() == now.power() && !pre.manual() && !manualMayChange) {
                    boolean sample = pre.count() % 5 == 0; boolean wantedFlow = pre.flow();
                    if (sample) { ++nativeSamples; if (pre.hadPower() && !pre.power() && !wantedFlow) { wantedFlow = true; ++fallingOpens; } if (!pre.hadPower() && pre.power() && wantedFlow) { wantedFlow = false; ++risingCloses; } require(now.hadPower() == pre.power(), "hadpower changes on real PREcount%5 sample"); if (pre.hadPower() != pre.power() || samples.size() < 40) samples.add(Map.of("native_tick", now.tick(), "PREcount", pre.count(), "power", pre.power(), "hadpower_before", pre.hadPower(), "hadpower_after", now.hadPower(), "flow_before", pre.flow(), "flow_after", now.flow())); }
                    else require(now.hadPower() == pre.hadPower(), "no continuous/second redstone timer between original count samples");
                    require(now.flow() == wantedFlow, "native edge/sample rule preserves stable-powered manual override");
                } else if (pre != null && pre.tick() == player.server.getTickCount() && pre.power() != now.power() && !pre.manual() && !manualMayChange) {
                    // A real lever packet can run between SERVER_PRE and the
                    // block ticker. Do not silently discard a genuine edge just
                    // because input changed within this observed native tick.
                    // If the latch changed, it proves the ticker sampled the new
                    // input; otherwise leave the pending edge for its next sample.
                    boolean latchChanged = pre.hadPower() != now.hadPower();
                    require(!latchChanged || pre.count() % 5 == 0 && now.hadPower() == now.power(),
                            "within-tick lever edge changes latch ONLY at the real PREcount sample");
                    boolean wantedFlow = pre.flow();
                    if (latchChanged) {
                        ++nativeSamples;
                        if (pre.hadPower() && !now.hadPower() && !wantedFlow) { wantedFlow = true; ++fallingOpens; }
                        if (!pre.hadPower() && now.hadPower() && wantedFlow) { wantedFlow = false; ++risingCloses; }
                        samples.add(Map.of("native_tick", now.tick(), "PREcount", pre.count(), "power_before", pre.power(),
                                "power_after", now.power(), "hadpower_before", pre.hadPower(), "hadpower_after", now.hadPower(),
                                "flow_before", pre.flow(), "flow_after", now.flow(), "input_changed_within_native_tick", true));
                    }
                    require(now.flow() == wantedFlow, "within-tick lever action respects sampled edges without rewriting clock/input/state");
                }
                if (!now.flow() && now.suction() == 0) {
                    ++settledChecks;
                    if (previous.settledChecks() > 0 && previous.rawSmelted() == 4 && now.rawSmelted() == 4 && previous.locations().getOrDefault("furnace", Map.of()).isEmpty() && now.locations().getOrDefault("furnace", Map.of()).isEmpty()) require(now.source() == previous.source() && now.source() + now.valve() + now.tube() + now.jar() == previous.source() + previous.valve() + previous.tube() + previous.jar(), "settled closed zero suction stops source intake but ALLOWS residual-buffer downstream drainage");
                } else settledChecks = 0;
                require(valve.getVenting() == 0 && valve.getStoredEssentiaAmount() <= 1 && (!valve.isConnectable(valve.getFacing())), "single-source native valve remains capacity1/unvented; stem excluded");
            }
            if (previous.rawSmelted() == 4 && now.rawSmelted() == 4 && previous.locations().getOrDefault("furnace", Map.of()).isEmpty() && now.locations().getOrDefault("furnace", Map.of()).isEmpty()) {
                // A same-tick pass-through cannot hide in a net buffer delta:
                // reconstruct each real hop independently for BOTH aspects.
                for (String tag : BUDGET.keySet()) {
                    int sourceOut = amount(previous, "alembic:1", tag) - amount(now, "alembic:1", tag);
                    int valveOut = amount(previous, "valve", tag) + sourceOut - amount(now, "valve", tag);
                    int tubeOut = amount(previous, "tube", tag) + valveOut - amount(now, "tube", tag);
                    int jarAdded = amount(now, "jar", tag) - amount(previous, "jar", tag);
                    require(sourceOut >= 0 && sourceOut <= 1 && valveOut >= 0 && valveOut <= 1 && tubeOut >= 0 && tubeOut <= 1 && jarAdded == tubeOut, "EACH real source->valve->tube->jar hop supplies exactly its stored delta, including same-tick pass-through " + tag);
                    require(amount(now, "alembic:2", tag) == amount(previous, "alembic:2", tag), "upper other output untouched by the real lower route " + tag);
                    if (sourceOut + valveOut + tubeOut > 0) transfers.add(Map.of("native_tick", now.tick(), "aspect", tag, "actual_source_to_valve", sourceOut, "actual_valve_to_basic_tube", valveOut, "actual_basic_tube_to_jar", tubeOut, "actual_stored_in_jar", jarAdded, "voided", 0, "read_only", true));
                }
            }
            int added = now.jar() - previous.jar(); require(added >= 0 && added <= 1, "new ordinary jar gains at most one native unit per tick"); jarUnits += added;
            if (added > 0) { require(now.tick() - lastJarPull >= 5, "actual jar pulls follow its native local5tick cadence"); lastJarPull = now.tick(); transfers.add(Map.of("native_tick", now.tick(), "from", "actual downstream basic tube", "to", "new ordinary jar", "supplied", added, "stored", added, "voided", 0, "actual_jar", now.jar())); }
            now = new Frame(now.tick(), now.count(), now.coal(), rawSmelted, fuelSpent, now.burnTime(), now.power(), now.flow(), now.hadPower(), now.suction(), now.source(), now.valve(), now.tube(), now.jar(), settledChecks, now.stored(), now.locations()); checkBalance(now);
            if (coalDebit > 0 || !now.locations().equals(previous.locations()) || now.flow() != previous.flow() || now.hadPower() != previous.hadPower() || now.power() != previous.power()) balances.add(Map.of("native_tick", now.tick(), "actual_physical_coal_remaining", now.coal(), "actual_raw_smelt_events", rawSmelted, "actual_fuel_ignitions", fuelSpent, "stored", now.stored(), "locations", now.locations(), "flow", now.flow(), "suction", now.suction(), "hadpower", now.hadPower()));
            ++checks; previous = now; observed = now;
        }
        Frame snapshot() {
            var level = player.serverLevel(); var locations = new LinkedHashMap<String, Map<String, Integer>>(); locations.put("furnace", aspects(oven(level).getAspects()));
            for (int depth = 1; depth <= 2; depth++) locations.put("alembic:" + depth, singleton(alembic(level, depth).getAspect(), alembic(level, depth).getAmount()));
            TileTube valve = level.getBlockEntity(valvePos) instanceof TileTube valveTile ? valveTile : null;
            TileTube tube = level.getBlockEntity(tubePos) instanceof TileTube tubeTile ? tubeTile : null;
            if (valve != null) locations.put("valve", singleton(valve.getStoredEssentiaType(), valve.getStoredEssentiaAmount())); if (tube != null) locations.put("tube", singleton(tube.getStoredEssentiaType(), tube.getStoredEssentiaAmount())); locations.put("jar", singleton(jar(level).getAspect(), jar(level).getAmount()));
            var physical = physicalStacks(player); var physicalAspects = new LinkedHashMap<>(itemAspects(physical)); baselinePhysical.forEach((tag, amount) -> { int remaining = physicalAspects.getOrDefault(tag, 0) - amount; require(remaining >= 0, "old item-held essentia never disappears or becomes new budget " + tag); if (remaining == 0) physicalAspects.remove(tag); else physicalAspects.put(tag, remaining); }); locations.put("new physical items/cursor/drops/pending", Map.copyOf(physicalAspects));
            int coal = physical.stream().mapToInt(value -> countStack(value, Items.COAL)).sum() + countStack(oven(level).getItem(0), Items.COAL) + countStack(oven(level).getItem(1), Items.COAL);
            var total = new LinkedHashMap<String, Integer>(); for (var contents : locations.values()) contents.forEach((tag, amount) -> total.merge(tag, amount, Integer::sum));
            return new Frame(level.getGameTime(), valve == null ? 0 : valve.getTickCount(), coal, rawSmelted, fuelSpent, oven(level).getBurnTime(), valve != null && level.hasNeighborSignal(valvePos), valve == null || valve.isFlowAllowed(), valve != null && valve.hadPower(), valve == null ? 0 : valve.getSuctionAmount(Direction.WEST), alembic(level, 1).getAmount(), valve == null ? 0 : valve.getStoredEssentiaAmount(), tube == null ? 0 : tube.getStoredEssentiaAmount(), jar(level).getAmount(), settledChecks, Map.copyOf(total), Map.copyOf(locations));
        }
        void checkBalance(Frame frame) {
            require(frame.coal() == 5 - frame.rawSmelted() - frame.fuelSpent() && frame.rawSmelted() >= 0 && frame.rawSmelted() <= 4 && frame.fuelSpent() >= 0 && frame.fuelSpent() <= 1, "all five actual physical coals are retained or independently observed native RAW/FUEL debits");
            for (String tag : BUDGET.keySet()) require(BUDGET.get(tag) == 2 * (4 - frame.rawSmelted()) + frame.stored().getOrDefault(tag, 0), "EXACT finite supplied potential = actually unprocessed RAW potential + ALL stored, no void/loss/topup " + tag);
            for (String tag : frame.stored().keySet()) require(BUDGET.containsKey(tag), "no invented output aspect " + tag);
        }
    }

    private void startObservation(ServerPlayer player) {
        nativePre = server -> { if (server != player.server || observationFailure != null) return; if (player.level().getBlockEntity(valvePos) instanceof TileTube valve) beforeTick = new PreFrame(server.getTickCount(), valve.getTickCount(), player.level().hasNeighborSignal(valvePos), valve.isFlowAllowed(), valve.hadPower(), manualMayChange); };
        nativePost = server -> { if (server != player.server || observationFailure != null) return; try { ledger.observe(); } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); } };
        serverStopped = server -> { if (server == player.server) stopObservation(); }; TickEvent.SERVER_PRE.register(nativePre); TickEvent.SERVER_POST.register(nativePost); LifecycleEvent.SERVER_STOPPED.register(serverStopped);
    }
    private void stopObservation() { if (nativePre != null) { TickEvent.SERVER_PRE.unregister(nativePre); nativePre = null; } if (nativePost != null) { TickEvent.SERVER_POST.unregister(nativePost); nativePost = null; } if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; } }
    private void observeAnimation(Minecraft client) {
        if (client.player == null || !(client.level.getBlockEntity(valvePos) instanceof TileTube valve) || lastClientTick == client.player.tickCount) return;
        int rotation = Math.round(valve.getRotation()); require(rotation >= 0 && rotation <= 360 && rotation % 20 == 0, "native client +/-20-degree rotation in0..360, no partial interpolation");
        if (lastClientRotation >= 0 && client.player.tickCount == lastClientTick + 1 && rotation != lastClientRotation) { require(Math.abs(rotation - lastClientRotation) == 20, "native single client tick wheel step20"); sawClosing |= rotation > lastClientRotation; sawOpening |= rotation < lastClientRotation; }
        if (rotation != lastClientRotation) animations.add(Map.of("actual_client_tick", client.player.tickCount, "actual_rotation", rotation, "flow", valve.isFlowAllowed(), "facing", valve.getFacing().getName(), "render_degrees", -1.5F * rotation, "travel", -.12F * rotation / 360F)); lastClientRotation = rotation; lastClientTick = client.player.tickCount;
    }

    private void scanOld(ServerLevel level) {
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 258) >> 4; x++) for (int z = (center.getZ() - 18) >> 4; z <= (center.getZ() + 14) >> 4; z++) level.getChunk(x, z);
        int jars = 0, units = 0, originalJars = 0, originalUnits = 0, candles = 0;
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-12, -2, -18), center.offset(258, 10, 14))) { oldBlocks.put(at.immutable(), level.getBlockState(at)); BlockEntity tile = level.getBlockEntity(at); if (tile instanceof TileNode) oldNodes.put(at.immutable(), stableNodeTag(tile)); else if (tile != null) oldBEs.put(at.immutable(), beTag(tile)); if (tile instanceof TileJar jar) { ++jars; units += jar.getAmount(); if (at.getX() <= 1704) { ++originalJars; originalUnits += jar.getAmount(); } } if (level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get())) ++candles; }
        require(originalJars == 12 && originalUnits == 173 && jars == 14 && units == 301 && candles == 72 && oldBEs.size() >= 46, "whole accepted voidjar world: original12jars173 + two actual64 jars,72candles and ALL prior BEs");
        var hot = (TileAlchemyFurnace) level.getBlockEntity(oldHotFurnace); require(hot != null && hot.getItem(0).isEmpty() && hot.getItem(1).is(Items.COAL) && hot.getItem(1).getCount() == 1 && aspects(hot.getAspects()).equals(Map.of("ignis", 8)), "actual old finite empty-input furnace1fuel/8IGNIS, no freeze/reset");
        require(level.getBlockEntity(oldHotFurnace.above()) instanceof TileAlembic low && low.getAmount() == 32 && low.getAspect() == Aspect.FIRE && low.getFilter() == Aspect.FIRE, "accepted full32IGNIS alembic prevents old8buffer drainage");
        require(level.getBlockEntity(new BlockPos(1724, 216, 377)) instanceof TileJar oldVoid && oldVoid.isVoid() && oldVoid.getAmount() == 64 && oldVoid.getFilter() == Aspect.ENERGY, "accepted actual labelled void64 retained, never new fixture");
        report.put("old_world_scan", Map.of("min", center.offset(-12, -2, -18).toShortString(), "max", center.offset(258, 10, 14).toShortString(), "original_jars", originalJars, "original_units", originalUnits, "accepted_all_jars", jars, "accepted_all_jar_units", units, "candles", candles, "non_node_BEs", oldBEs.size(), "old_hot_furnace_before_RAW_snbt", beTag(hot).toString()));
    }
    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) { if (ItemStack.isSameItemSameComponents(saved, savedGold)) continue; int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum(), after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum(); require(after == before - (saved.is(tubeItem()) ? 2 : 0), "every saved main item/component/count retained EXCEPT one crafted+one placed saved tube " + saved); }
        for (int index = 0; index < ARMOR.size(); index++) require(ItemStack.matches(player.getItemBySlot(ARMOR.get(index)), savedArmor.get(index)), "exact saved worn gear");
        require(ItemStack.matches(player.getInventory().getItem(7), paidGold) && ItemStack.matches(stack(player, ValveClientSequence::primary), savedPrimary) && ItemStack.matches(player.getOffhandItem(), savedOffhand) && count(player, ThaumcraftContent.WAND.get()) == 3, "exact other wands/foci/offhand, genuinely paid gold returns7");
        require(player.totalExperience == initialXp && player.totalExperience == 11 && player.isInvulnerable() == savedInvulnerability && player.isAlive() && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && savedKnowledge.equals(KnowledgeAccess.get(player).save()), "XP11/knowledge/pools/scans/warp/Entity flag/Survival exact, no acquisition replay");
        for (var entry : oldBlocks.entrySet()) { BlockPos at = entry.getKey(); BlockState before = entry.getValue(), after = player.level().getBlockState(at); BlockPos cactus = center.offset(149, 0, 5); boolean column = at.getX() == cactus.getX() && at.getZ() == cactus.getZ() && at.getY() >= cactus.getY() && at.getY() <= cactus.getY() + 2; boolean growth = column && (before.is(Blocks.CACTUS) && after.is(Blocks.CACTUS) || before.isAir() && after.is(Blocks.CACTUS) && player.level().getBlockState(at.below()).is(Blocks.CACTUS)); boolean clockOnly = at.equals(oldHotFurnace) && before.getBlock() == after.getBlock() && before.setValue(BlockAlchemyFurnace.LIT, false) == after.setValue(BlockAlchemyFurnace.LIT, false); require(before == after || growth || clockOnly, "all old blocks retained; only declared native cactus/LIT transient " + at); }
        for (var entry : oldBEs.entrySet()) { BlockEntity actual = player.level().getBlockEntity(entry.getKey()); require(actual != null, "old BE identity exists " + entry.getKey()); CompoundTag after = beTag(actual); if (entry.getKey().equals(oldHotFurnace)) { CompoundTag beforeStable = entry.getValue().copy(), afterStable = after.copy(); beforeStable.remove("BurnTime"); afterStable.remove("BurnTime"); require(beforeStable.equals(afterStable) && after.getShort("BurnTime") >= 0 && after.getShort("BurnTime") <= entry.getValue().getShort("BurnTime") && player.level().getBlockState(oldHotFurnace).getValue(BlockAlchemyFurnace.LIT) == (((TileAlchemyFurnace) actual).getBurnTime() > 0), "only old native BurnTime/LIT can evolve; empty input cannot consume old coal, all items/aspects/identity exact"); report.put("old_hot_furnace_after_RAW_snbt", after.toString()); } else require(after.equals(entry.getValue()), "all other old BE items/aspects/filters/identity EXACT " + entry.getKey()); }
        for (var entry : oldNodes.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileNode && stableNodeTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "old node identity/base/type retained, only native current regeneration allowed");
    }
    private void finalState(ServerPlayer player) {
        // Do not manually call the observer/ticker here: use the latest real POST
        // sample, then independently inspect the actual live final state.
        Frame frame = observed; retained(player); ledger.checkBalance(frame);
        require(frame.rawSmelted() == 4 && frame.fuelSpent() == 1 && frame.coal() == 0 && frame.stored().equals(BUDGET) && ledger.jarUnits == 8 && ledger.checks >= 300 && risingCloses > 0 && fallingOpens > 0 && sawOpening && sawClosing && inspections.size() == 4, "finite native RAW/FUEL/transfer/edge/chat/animation story actually observed");
        require(oven(player.level()).isEmpty() && oven(player.level()).getVis() == 0 && alembic(player.level(), 1).getAmount() == 0 && alembic(player.level(), 2).getAmount() == 8 && alembic(player.level(), 2).getAspect() != jar(player.level()).getAspect() && jar(player.level()).getAmount() == 8 && jar(player.level()).getFilter() == null, "new jar holds all8 lower output; upper actual8 other output remains, no label/essentia grant");
        require(valve(player.level()).isValve() && valve(player.level()).isFlowAllowed() && !valve(player.level()).hadPower() && valve(player.level()).getFacing() == Direction.SOUTH && valve(player.level()).isSideOpen(Direction.SOUTH) && !valve(player.level()).isConnectable(Direction.SOUTH) && valve(player.level()).isSideOpen(Direction.EAST) && tube(player.level()).isSideOpen(Direction.WEST) && valve(player.level()).getStoredEssentiaAmount() == 0 && tube(player.level()).getStoredEssentiaAmount() == 0 && !player.level().getBlockState(leverPos).getValue(LeverBlock.POWERED), "actual open final valve/paired arms/stem/raw ports, no trapped/lost buffer, external lever off");
        require(count(player, tubeItem()) == 1 && count(player, valveItem()) == 0 && count(player, resonator()) == 1 && ItemStack.matches(stack(player, value -> value.is(resonator())), earnedResonator) && count(player, Items.LEVER) == 0 && ((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty() && !player.isUsingItem() && !player.isShiftKeyDown(), "actual crafted valve placed, resonator retained unchanged, one saved tube retained, menus/cursor released");
        require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && pendingStacks(player.serverLevel()).isEmpty(), "no physical/pending output debt or XP grants");
        report.put("final_exact_unit_accounting", Map.of("finite_RAW_potential", BUDGET, "actual_stored", frame.stored(), "actual_locations", frame.locations(), "raw_coals_natively_smelted", frame.rawSmelted(), "separate_fuel_coals_natively_ignited", frame.fuelSpent(), "actual_physical_coals_remaining", frame.coal(), "native_POST_checks", ledger.checks, "native_jar_units", ledger.jarUnits, "voided_or_lost", 0));
        report.put("resumed_transfer", Map.of("closed_native_tick", closedTick, "closed_actual_count", closedAtCount, "closed_source", closedSource, "closed_jar", closedJar, "closed_route_total", closedTotal, "final_source", alembic(player.level(), 1).getAmount(), "final_jar", jar(player.level()).getAmount(), "real_resumed_jar_units", jar(player.level()).getAmount() - closedJar));
        report.put("retained_prior_outputs", Map.of("old_non_node_BEs", oldBEs.size(), "original12jars173units", true, "accepted_void64_ordinary64_phial8_IGNIS32alembic8furnace", true, "old_hot_furnace_only_native_BurnTime_LIT_allowed", true, "all_other_old_BEs_exact", true, "all_wands_foci_worn_gear_knowledge_exact_except_paid_gold", true, "saved_tubes_retained", 1, "saved_total_XP", 11, "prior_acquisition_replayed", false));
        report.put("native_final_valve_RAW_snbt", beTag(valve(player.level())).toString()); report.put("native_final_gold_RAW_snbt", snbt(player.getInventory().getItem(7), player)); finalVerified = true;
    }

    private BlockPos patchMin() { return workbench.offset(-4, -2, -7); }
    private BlockPos patchMax() { return workbench.offset(15, 7, 8); }
    private AABB patchBounds() { return new AABB(Vec3.atLowerCornerOf(patchMin()), Vec3.atLowerCornerOf(patchMax().offset(1, 1, 1))); }
    private void patch(ServerLevel level) { for (int x = patchMin().getX() >> 4; x <= patchMax().getX() >> 4; x++) for (int z = patchMin().getZ() >> 4; z <= patchMax().getZ() >> 4; z++) level.getChunk(x, z); require(level.getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, patchBounds()).isEmpty() && pendingStacks(level).isEmpty(), "new patch refuses old entities/items/XP/pending outputs"); for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft") && !oldBlocks.containsKey(at), "new patch cannot overwrite old BE/TC/scanned cells " + at); for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState()); }
    private int supply(ServerPlayer player, Item item, int amount, String purpose) { require(item == Items.IRON_INGOT && amount == 4 || item == Items.QUARTZ && amount == 1 || item == Items.STICK && amount == 1 || item == Items.LEVER && amount == 2 || item == Items.COAL && (amount == 4 || amount == 1), "ONLY declared finite raw/lever fixtures, never output/vis/essentia/XP"); int slot = emptyMain(player); player.getInventory().setItem(slot, new ItemStack(item, amount)); supplied.add(Map.of("raw_item", BuiltInRegistries.ITEM.getKey(item).toString(), "count", amount, "purpose", purpose, "genuinely_empty_main_slot", slot)); return slot; }
    private List<ItemStack> physicalStacks(ServerPlayer player) { var result = new ArrayList<ItemStack>(); result.addAll(player.getInventory().items); result.add(player.containerMenu.getCarried()); for (ItemEntity entity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds())) result.add(entity.getItem()); result.addAll(pendingStacks(player.serverLevel())); return result; }
    private static Map<String, Integer> itemAspects(List<ItemStack> stacks) { var result = new LinkedHashMap<String, Integer>(); for (ItemStack value : stacks) { if (ItemJar.isJar(value)) merge(result, ItemJar.getAspect(value), ItemJar.getAmount(value) * value.getCount()); if (value.is(ThaumcraftContent.FILLED_PHIAL.get())) { var data = ItemEssence.data(value); require(data != null && Aspect.getAspect(data.tag()) != null, "physical filled phial carries actual aspect"); merge(result, Aspect.getAspect(data.tag()), PhialData.AMOUNT * value.getCount()); } } return Map.copyOf(result); }
    @SuppressWarnings("unchecked") private List<ItemStack> pendingStacks(ServerLevel level) { try { var field = DeferredItemDrops.class.getDeclaredField("FACTORY"); field.setAccessible(true); var factory = (SavedData.Factory<DeferredItemDrops>) field.get(null); var queue = level.getDataStorage().get(factory, "thaumcraft_pending_inventory_drops"); if (queue == null) return List.of(); var result = new ArrayList<ItemStack>(); for (Tag raw : queue.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) { CompoundTag row = (CompoundTag) raw; if (!patchBounds().contains(Vec3.atCenterOf(BlockPos.of(row.getLong("Source"))))) continue; ItemStack value = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElseThrow(); value.setCount(row.getInt("Count")); result.add(value); } return result; } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only pending-output view needs root API reconciliation", error); } }
    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException { int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages"); ResourceLocation wanted = id("arcane/essentia_valve"); for (int index = page; index < Math.min(page + 2, pages.size()); index++) if (BookClientSmoke.field(pages.get(index), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(wanted)) { require(display.recipe() == client.level.getRecipeManager().byKey(wanted).orElseThrow(), "book shows actual synced native shapeless holder"); return true; } require(++turns <= 18, "bounded native TUBES book navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow(); client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false; }
    @SuppressWarnings("unchecked") private static List<GuiMessage> chat(Minecraft client) throws ReflectiveOperationException { return List.copyOf((List<GuiMessage>) BookClientSmoke.field(client.gui.getChat(), "allMessages")); }
    private Vec3 wandPoint() { return Vec3.atCenterOf(valvePos).add(wandAction < 4 ? 0 : .35, 0, wandAction < 4 ? .125 : 0); }
    private void toggle(Minecraft client) { require(client.player.getMainHandItem().isEmpty(), "genuine empty-hand valve toggle"); manualMayChange = true; use(client, valvePos, Direction.SOUTH, Vec3.atCenterOf(valvePos).add(0, 0, .125)); }
    private void capture(int index) { require(captures == index, "exact contiguous capture order107-113"); ++captures; report.put("actual_captures_requested", captures); capture.accept(CAPTURES.get(index)); }
    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException { require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside menus"); if (!wanted.test(client.player.getInventory().getItem(HAND_SLOT))) { click(client, playerSlot(client, itemSlot(client.player, wanted)), HAND_SLOT, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; } if (client.player.tickCount - handChangedAt < 6) return false; select(client, HAND_SLOT); return true; }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException { return hand(client, ItemStack::isEmpty); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static int playerSlot(Minecraft client, int inventory) { return client.player.containerMenu.slots.stream().filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void use(Minecraft client, BlockPos pos, Direction face, Vec3 point) {
        require(client.player.canInteractWithBlock(pos, 0), "actual native Survival reach " + pos);
        lookAt(client, point);
        // A scripted turn and click in CLIENT_POST otherwise send the use packet
        // before LocalPlayer's NEXT movement update. In particular standing up
        // after inspection changes eye height, so the old crouched pitch misses
        // the valid target on the authoritative server ray. Send the ACTUAL
        // current native client angles first, not a direct server pose mutation
        // or a weakened production reach/occlusion check.
        client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround()));
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)).consumesAction(), "native useItemOn accepted " + pos + "/" + face);
    }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 delta = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void stand(ServerPlayer player, double x, double y, double z) { var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z); require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "collision-checked camera on declared floor"); player.teleportTo(x, y, z); }
    private void machineCamera(ServerPlayer player) { stand(player, furnace.getX() + 2.4, workbench.getY(), furnace.getZ() + 2.6); }
    private static void releaseKeys(Minecraft client) { client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); }
    private void releaseInputs(Minecraft client) { releaseKeys(client); if (client.player != null && client.gameMode != null) { client.gameMode.stopDestroyBlock(); if (client.player.isUsingItem()) client.gameMode.releaseUsingItem(client.player); } if (previousCamera != null) client.options.setCameraType(previousCamera); }
    /** Detached comparison only, never installed into the actual saved wand. */
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price) { ItemStack a = before.copy(), b = after.copy(); a.remove(ThaumcraftContent.WAND_DATA.get()); b.remove(ThaumcraftContent.WAND_DATA.get()); require(ItemStack.matches(a, b), "all non-vis/focus/craft-cost components exact"); WandData first = ItemWandCasting.data(before), last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same actual saved payer materials"); for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0), "exact native finite centivis debit " + tag); }
    private static CompoundTag beTag(BlockEntity tile) { return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy(); }
    private static CompoundTag stableNodeTag(BlockEntity tile) { CompoundTag tag = beTag(tile); tag.remove("Aspects"); tag.remove("lastActive"); return tag; }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect type : list.getAspects()) merge(result, type, list.getAmount(type)); return Map.copyOf(result); }
    private static Map<String, Integer> singleton(Aspect type, int amount) { var result = new LinkedHashMap<String, Integer>(); merge(result, type, amount); return Map.copyOf(result); }
    private static int amount(Frame frame, String location, String tag) { return frame.locations().getOrDefault(location, Map.of()).getOrDefault(tag, 0); }
    private static void merge(Map<String, Integer> result, Aspect type, int amount) { require(amount >= 0 && (amount == 0 || type != null), "native stored amount/type valid"); if (amount > 0) result.merge(type.getTag(), amount, Integer::sum); }
    private static boolean gold(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("greatwood") && ItemWandCasting.data(value).cap().equals("gold"); }
    private static boolean primary(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int slot = 0; slot < 36; slot++) if (wanted.test(player.getInventory().getItem(slot))) return slot; throw new IllegalStateException("Missing actual finite valve-story input/output"); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().mapToInt(value -> countStack(value, item)).sum(); }
    private static int countStack(ItemStack value, Item item) { return value.is(item) ? value.getCount() : 0; }
    private static int emptyMain(Player player) { for (int slot = 9; slot < 36; slot++) if (player.getInventory().getItem(slot).isEmpty()) return slot; throw new IllegalStateException("No genuinely empty main inventory slot"); }
    private static Item tubeItem() { return ThaumcraftTubes.ESSENTIA_TUBE.get().asItem(); }
    private static Item valveItem() { return ThaumcraftTubes.ESSENTIA_VALVE.get().asItem(); }
    private static Item resonator() { return ThaumcraftContent.RESONATOR.get(); }
    private TileTube valve(Level level) { return (TileTube) level.getBlockEntity(valvePos); }
    private TileTube tube(Level level) { return (TileTube) level.getBlockEntity(tubePos); }
    private TileAlchemyFurnace oven(Level level) { return (TileAlchemyFurnace) level.getBlockEntity(furnace); }
    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(furnace.above(depth)); }
    private TileJar jar(Level level) { return (TileJar) level.getBlockEntity(jarPos); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ContainerAlchemyFurnace furnaceMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String snbt(ItemStack value, ServerPlayer player) { return value.save(player.registryAccess()).toString(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "no overlapping fixture/read-only server actions"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; thaumcraft.common.Thaumcraft.LOG.info("Valve client step={} craft={} wand={} inspect={}", step, craftIndex, wandAction, inspectIndex); }
}
