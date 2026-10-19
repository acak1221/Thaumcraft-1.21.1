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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
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
import thaumcraft.common.config.ThaumcraftNaturals;
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

/** A finite M06b continuation, never a replay or a resource-restoring fixture. */
public final class TubeFilterClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_CAPTURE, CHARGE_TRAVEL, CHARGE_AIM, CHARGE_READY,
        CHARGE_START, CHARGE_USE, CHARGE_STOP, CHARGE_ACK, CHARGE_SCREEN, CHARGE_CAPTURE,
        BENCH_TRAVEL, BENCH_OPEN, FUND, FUNDED, GRID, PREVIEW, PREVIEW_ACK, TAKE, STORE, STORED,
        REMAINDER_ACK, WAND_PICK, WAND_RETURN, CRAFT_DONE,
        FURNACE_TRAVEL, FURNACE_OPEN, FUEL_PICK, FUEL_PUT, INPUT_PICK, INPUT_PUT, BURNING, DISTILL,
        ROUTE_TRAVEL, PLACE, PLACE_ACK, LABEL_HAND, LABEL_ACK, WRONG_WAIT, WRONG_CAPTURE,
        WAND_AIM, WAND_USE, WAND_ACK, FLOW_WAIT, INSPECT_HAND, INSPECT_SHIFT, INSPECT_READY,
        INSPECT_USE, INSPECT_ACK, FLOW_CAPTURE, OFFHAND_STORE, OFFHAND_ACK, REMOVE_HAND,
        REMOVE_SHIFT, REMOVE_READY, REMOVE_USE, REMOVE_ACK, PICKUP, OFFHAND_RETURN,
        OFFHAND_RETURNED, GOLD_RETURN, FINAL_CHECK, FINAL_ACK, FINAL_CAPTURE, DONE
    }
    private static final List<String> CAPTURES = List.of(
            "114-filter-restrict-oneway-original-native-TUBEFILTER-book-en.png",
            "115-actual-saved-node-native-gold-recharge-finite-all-primal-ledger-en.png",
            "116-earned-vis-filter-native-shapeless-filter-450aqua-1440ordo-en.png",
            "117-native-lapis-oneway-shapeless-450aqua-720ordo-720perditio-en.png",
            "118-saved-blank-label-potentia-phial-native-zero-vis-binding-en.png",
            "119-paid-potentia-filter-restrict-oneway-wrong-facing-zero-upstream-pressure-en.png",
            "120-native-wand-corrected-direction-half-pressure-eight-potentia-resonator-chat-en.png",
            "121-native-filter-white-unset-real-blank-label-pickup-retained-valve-world-en.png");
    private static final Map<String, Integer> SAVED_VIS = Map.of("aer", 50, "aqua", 860, "ignis", 500, "ordo", 50, "perditio", 950, "terra", 2300);
    private static final Map<String, Integer> FROST_VIS = Map.of("aer", 1951, "aqua", 2436, "ignis", 2846, "ordo", 933, "perditio", 431, "terra", 464);
    private static final Map<String, Integer> TOTAL_PRICE = Map.of("aqua", 2250, "ordo", 3060, "terra", 1440, "perditio", 720);
    private static final Map<String, Integer> BUDGET = Map.of("ignis", 8, "potentia", 8);
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final List<BlockPos> OLD_FURNACES = List.of(new BlockPos(1722, 216, 377), new BlockPos(1754, 216, 377));
    private static final int HAND_SLOT = 3;

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final Minecraft owningClient;
    private final Consumer<String> capture;
    private final BlockPos workbench, center, furnace, jarPos, chargingNode;
    private final long startedNanos = System.nanoTime();
    private final Map<BlockPos, BlockState> oldBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> oldBEs = new LinkedHashMap<>(), oldNodes = new LinkedHashMap<>();
    private final List<ItemStack> savedStacks = new ArrayList<>(), savedArmor = new ArrayList<>();
    private final List<Map<String, Object>> inputs = new ArrayList<>(), crafts = new ArrayList<>(), balances = new ArrayList<>(), transfers = new ArrayList<>(), chargeRows = new ArrayList<>();
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
    private volatile int potentiaDepth;
    private volatile List<BlockPos> route = List.of();
    private volatile boolean awaitingBlank;
    private volatile UUID blankDropId;
    private volatile ItemStack blankDrop;
    private Ledger ledger;
    private ItemStack savedGold, chargedGold, paidGold, savedPrimary, savedOffhand, savedPhial, savedResonator;
    private CompoundTag expectedKnowledge;
    private List<int[]> gridClicks = List.of();
    private List<GuiMessage> chatBefore = List.of();
    private List<String> expectedChat = List.of();
    private int ticks, totalTicks, captures, turns, craftIndex, gridIndex, destination, placement, rawCoalSlot, fuelCoalSlot, appleSlot, inspectIndex;
    private int initialXp, handChangedAt = Integer.MIN_VALUE / 2, chargeTicks;
    private BlockPos useAimPos;
    private Direction useAimFace;
    private Vec3 useAimPoint;
    private int useAimStarted;
    private long wrongTick = -1;
    private boolean savedInvulnerability, finalVerified, chargingHeld, aimCharge, releaseSent;

    public TubeFilterClientSequence(Minecraft client, Consumer<String> capture, BlockPos bench, BlockPos oldCenter) {
        owningClient = client; this.capture = capture; workbench = bench.immutable(); center = oldCenter.immutable();
        furnace = workbench.east(8); jarPos = furnace.east(3); chargingNode = new BlockPos(1623, 218, 374);
        report.put("scope", "tubefilter: ONLY root's NEW tubefilter-smoke-UUID private copy of ACCEPTED STOPPED valve-smoke-39c7d715-ac80-4b86-97a6-7fcafb0c3caa; never reuse either failed valve run or replay acquisition");
        report.put("boundary", "Actual saved gold7, quicksilverDROP8, basicTube1, blankLabel1 and actual POTENTIA8phial are inputs. Native node use recharges only the saved gold payer; all other wands/foci/worn armor/XP11/apple/knowledge/complete old world retained. NEW RAW: iron2/glass1/goldnugget1 for eight tubes, goldingot2/silverwoodplank1 for two VisFilters, STONE1/LAPIS1, four raw coal/one separate fuel coal. One TUBEFILTER research completion, empty table/furnace/two alembics/one ordinary jar, separate stone floor and collision-checked camera are declared fixtures. No output/vis/essentia/XP grants, clock/weather/RNG changes or gameplay backend calls.");
        report.put("story", "Five actual arcane menu crafts, then native zero-vis binding of saved blank+saved POTENTIA8phial with exact unchanged real phial remainder. Attach paid label to real filter. Distill four coal to8each and choose the actual unseeded POTENTIA alembic. Place filter->restrict->oneway->jar (one extra earned basic tube only if upper source). Wrong EAST facing leaves upstream pressure0; actual wand-core click selects UP, then native source->filter->restrict->oneway->jar transfers all8. Secondary resonator inspects real raw storage/suction, never the filter marker -1. Temporarily move exact offhand apple with native clicks for genuine empty-hand sneak removal, observe ONE physical blank label UUID, approach/pick it up normally and restore apple.");
        report.put("coordinates", Map.of("bench", workbench.toShortString(), "furnace", furnace.toShortString(), "jar", jarPos.toShortString(), "ACTUAL_saved_node", chargingNode.toShortString(), "old_center", center.toShortString()));
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size()); report.put("supplied_raw_inputs_and_fixtures", inputs); report.put("actual_native_crafts", crafts); report.put("passive_exact_balances", balances); report.put("passive_per_hop_transfers", transfers); report.put("actual_node_charge_transitions", chargeRows);
        report.put("limits", Map.of("wall_seconds", 600, "client_ticks", 14000, "ordinary_step_ticks", 400, "node_charge_ticks", 2400, "native_distillation_ticks", 6000, "route_wait_ticks", 1600, "preview_fail_fast_ticks", 40));
        report.put("retention_caveats", "Only BurnTime and matching LIT at old1722 AND1754 may naturally decrease. Both actual old inputs must be EMPTY: no further ignition/fuel consumption allowed, all other old item/aspect/filter/identity fields exact. Old node current/lastActive may naturally evolve, but saved UUID/base/type retained; this actual source has a complete finite charge/regen ledger. Saved cactus AGE/at-most-three-block native growth allowed, never reset.");
        report.put("visual_inspection", "Pending root exact114-121 inspection. Native tint/retained textures/three source-model blue rods, no synthetic pressure colours or particles. ChatScreen and InventoryScreen use root's shared capture allowlist/deadline, no custom unsupported screen.");
        report.put("not_covered", List.of("prior acquisition replay", "64-then35 sequential restrict edge/cancellation/creative/reentrancy/reload matrices: separate native server cases", "buffer/centrifuge/other automation", "full GUI/lighting/FX/audio parity", "ordinary packaged install, remote server or NeoForge"));
    }

    public boolean tick(Minecraft client) throws ReflectiveOperationException {
        try { return advance(client); }
        catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client); if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Tube-filter story failed at " + step + "; craft=" + craftIndex + "; placement=" + placement + "; observed=" + observed, error); throw error;
        }
    }

    private boolean advance(Minecraft client) throws ReflectiveOperationException {
        require(client == owningClient, "same root-owned client"); ++ticks; ++totalTicks;
        int limit = step == Step.CHARGE_USE ? 2400 : step == Step.DISTILL ? 6000 : step == Step.FLOW_WAIT ? 1600 : 400;
        require(ticks <= limit && totalTicks <= 14000 && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded tube-filter step " + step + "; ticks=" + ticks + "/" + limit);
        if (observationFailure != null) throw new IllegalStateException("Passive tube-filter observer failed", observationFailure);
        if (aimCharge && client.screen == null) lookAt(client, Vec3.atCenterOf(chargingNode));
        switch (step) {
            case HANDOFF -> {
                var path = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(); String name = path.getFileName().toString(); require(name.startsWith("tubefilter-smoke-"), "ONLY NEW private tubefilter world, never accepted input"); UUID.fromString(name.substring("tubefilter-smoke-".length()));
                require(path.getParent().getFileName().toString().equals("saves") && path.getParent().getParent().getFileName().toString().equals("run-clienttest"), "isolated client-test save"); report.put("actual_owned_world", path.toString()); previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON); client.setScreen(null); releaseKeys(client);
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "no inherited menu/cursor"); onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> { if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("TUBEFILTER")) return false; client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("TUBEFILTER"))); next(Step.BOOK); }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_CAPTURE); }
            case BOOK_CAPTURE -> { report.put("native_book", Map.of("research", "TUBEFILTER", "actual_synced_recipe", id("arcane/essentia_filter").toString(), "base_whole_vis", Map.of("aqua", 5, "ordo", 16))); next(Step.CHARGE_TRAVEL); capture(0); }
            case CHARGE_TRAVEL -> { client.setScreen(null); aimCharge = true; onServer(client, () -> stand(serverPlayer(client), chargingNode.getX() + .5, workbench.getY(), chargingNode.getZ() + 3.5)); next(Step.CHARGE_AIM); }
            case CHARGE_AIM -> { if (!serverDone() || ticks < 16 || !hand(client, value -> ItemStack.matches(value, savedGold))) return false; onServer(client, () -> charge = new ChargeProbe(serverPlayer(client))); next(Step.CHARGE_READY); }
            case CHARGE_READY -> { if (!serverDone()) return false; next(Step.CHARGE_START); }
            case CHARGE_START -> { if (!client.isWindowActive()) return false; sendRotation(client, Vec3.atCenterOf(chargingNode)); releaseSent = false; chargingHeld = true; chargeTicks = 0; client.options.keyUse.setDown(true); require(client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).consumesAction(), "NEW native held-RMB use of ACTUAL saved node"); next(Step.CHARGE_USE); }
            case CHARGE_USE -> { require(ticks < 40 || charge.used, "actual native saved-node session never started within40ticks"); if (!charge.funded) return false; releaseUse(client); next(Step.CHARGE_STOP); }
            case CHARGE_STOP -> { releaseUse(client); if (ticks < 6 || client.player.isUsingItem() || charge.stopped < 2) return false; onServer(client, () -> { report.put("native_finite_recharge", charge.finish()); chargedGold = paidGold = serverPlayer(client).getMainHandItem().copy(); }); next(Step.CHARGE_ACK); }
            case CHARGE_ACK -> { if (!serverDone() || !ItemStack.matches(client.player.getMainHandItem(), chargedGold)) return false; aimCharge = false; client.setScreen(new InventoryScreen(client.player)); next(Step.CHARGE_SCREEN); }
            case CHARGE_SCREEN -> { if (ticks < 8 || !client.isWindowActive()) return false; hover(client, HAND_SLOT); next(Step.CHARGE_CAPTURE); }
            case CHARGE_CAPTURE -> { if (ticks < 3) return false; next(Step.BENCH_TRAVEL); capture(1); }
            case BENCH_TRAVEL -> { client.setScreen(null); if (!ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) { click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, paidGold))), 7, ClickType.SWAP); return false; } onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.BENCH_OPEN); }
            case BENCH_OPEN -> { if (!serverDone() || ticks < 12) return false; select(client, 7); if (!use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5))) return false; next(Step.FUND); }
            case FUND -> { if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false; for (int slot = 0; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "genuinely empty new workbench"); click(client, playerSlot(client, 7), 0, ClickType.QUICK_MOVE); next(Step.FUNDED); }
            case FUNDED -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getSlot(1).getItem(), paidGold)) return false; beginGrid(client); }
            case GRID -> { if (!serverDone() || ticks < 2) return false; if (gridIndex == gridClicks.size()) { next(Step.PREVIEW); return false; } int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0; }
            case PREVIEW -> {
                if (ticks < 8) return false; if (!arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craftOutput())) { require(ticks < 40, "native recipe preview missing within40ticks " + craftRecipe() + "; grid=" + java.util.stream.IntStream.range(2, 11).mapToObj(slot -> arcane(client).getSlot(slot).getItem().toString()).toList()); return false; }
                require(arcane(client).getCarried().isEmpty() && arcane(client).getSlot(0).getItem().getCount() == outputCount(), "finite actual result preview"); verifyGrid(arcane(client), false); onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.PREVIEW_ACK);
            }
            case PREVIEW_ACK -> { if (!serverDone()) return false; for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == craftPrice().getOrDefault(tag, 0), "native actor price " + tag); next(Step.TAKE); if (craftIndex == 2) capture(2); else if (craftIndex == 4) capture(3); else if (craftIndex == 5) capture(4); }
            case TAKE -> { click(client, 0, 0, ClickType.PICKUP); next(Step.STORE); }
            case STORE -> { if (ticks < 6 || !arcane(client).getCarried().is(craftOutput())) return false; require(arcane(client).getCarried().getCount() == outputCount() && (craftIndex != 5 || ItemJarLabel.aspect(arcane(client).getCarried()) == Aspect.ENERGY), "only genuine finite output; native label really bound to POTENTIA"); verifyGrid(arcane(client), true); destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.STORED); }
            case STORED -> { if (ticks < 6 || !arcane(client).getCarried().isEmpty()) return false; verifyDebit(paidGold, arcane(client).getSlot(1).getItem(), craftPrice()); paidGold = arcane(client).getSlot(1).getItem().copy(); if (craftIndex == 5) next(Step.REMAINDER_ACK); else { ++craftIndex; beginGrid(client); } }
            case REMAINDER_ACK -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, filledPhial()) != 1) return false;
                // Original SlotCraftingArcaneWorkbench:49-63 returns container
                // remainders to player inventory FIRST. This actor has room;
                // expecting a phial in cell8 would contradict the real menu.
                verifyGrid(arcane(client), true);
                require(ItemStack.matches(stack(client.player, value -> value.is(filledPhial())), savedPhial), "actual native inventory-first remainder retains the exact saved8POTENTIA phial, not a reconstruction");
                report.put("native_inventory_first_phial_remainder", Map.of("exact_saved_components_matched", true, "actual_count", count(client.player, filledPhial()), "matrix_empty", true, "output_or_remainder_grants", 0));
                click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK);
            }
            case WAND_PICK -> { if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), paidGold)) return false; require(client.player.getInventory().getItem(7).isEmpty(), "actual payer returns7 after native craft funding moved it"); click(client, playerSlot(client, 7), 0, ClickType.PICKUP); next(Step.WAND_RETURN); }
            case WAND_RETURN -> { if (ticks < 6 || !arcane(client).getCarried().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) return false; client.player.closeContainer(); onServer(client, () -> verifyCrafts(serverPlayer(client))); next(Step.CRAFT_DONE); }
            case CRAFT_DONE -> { if (!serverDone() || ticks < 8) return false; onServer(client, () -> stand(serverPlayer(client), furnace.getX() + .5, workbench.getY(), furnace.getZ() + 3.2)); next(Step.FURNACE_TRAVEL); }
            case FURNACE_TRAVEL -> { if (!serverDone() || ticks < 12 || !emptyHand(client)) return false; if (!use(client, furnace, Direction.SOUTH, Vec3.atCenterOf(furnace).add(0, 0, .5))) return false; next(Step.FURNACE_OPEN); }
            case FURNACE_OPEN -> { if (ticks < 8 || !(client.screen instanceof GuiAlchemyFurnace)) return false; require(furnaceMenu(client).getSlot(0).getItem().isEmpty() && furnaceMenu(client).getSlot(1).getItem().isEmpty(), "empty new furnace slots"); click(client, playerSlot(client, fuelCoalSlot), 0, ClickType.PICKUP); next(Step.FUEL_PICK); }
            case FUEL_PICK -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == 1, "single separate normal FUEL coal"); click(client, 1, 0, ClickType.PICKUP); next(Step.FUEL_PUT); }
            case FUEL_PUT -> { if (ticks < 5 || !furnaceMenu(client).getCarried().isEmpty()) return false; click(client, playerSlot(client, rawCoalSlot), 0, ClickType.PICKUP); next(Step.INPUT_PICK); }
            case INPUT_PICK -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == 4, "exact four RAW coal, not fuel"); click(client, 0, 0, ClickType.PICKUP); next(Step.INPUT_PUT); }
            case INPUT_PUT -> { if (ticks < 8 || !furnaceMenu(client).isBurning()) return false; require(!furnaceMenu(client).isSpeedBoost() && furnaceMenu(client).getCurrentItemBurnTime() == 1600 && furnaceMenu(client).getBellowsCount() == 0 && furnaceMenu(client).getSmeltTime() == 40, "native40tick coal distillation and1600 ordinary fuel, no clock shortcut"); client.player.closeContainer(); next(Step.BURNING); }
            case BURNING -> { if (ticks < 8) return false; next(Step.DISTILL); }
            case DISTILL -> { Frame frame = observed; if (frame == null || frame.rawSmelted() != 4 || !frame.locations().getOrDefault("furnace", Map.of()).isEmpty() || alembic(client.level, 1).getAmount() != 8 || alembic(client.level, 2).getAmount() != 8) return false; onServer(client, () -> identifySource(serverPlayer(client))); next(Step.ROUTE_TRAVEL); }
            case ROUTE_TRAVEL -> { if (!serverDone() || ticks < 12 || route.isEmpty()) return false; next(Step.PLACE); }
            case PLACE -> { Item item = placement == 0 ? filterItem() : placement == 1 ? restrictItem() : placement == 2 ? oneWayItem() : tubeItem(); if (!hand(client, value -> value.is(item))) return false; BlockPos anchor = placement == 0 ? furnace.above(potentiaDepth) : route.get(placement - 1); Direction face = placement == 3 ? Direction.DOWN : Direction.EAST; if (!use(client, anchor, face, Vec3.atCenterOf(anchor).add(face.getStepX() * .125, face.getStepY() * .125, face.getStepZ() * .125))) return false; next(Step.PLACE_ACK); }
            case PLACE_ACK -> { if (ticks < 6 || !(client.level.getBlockEntity(route.get(placement)) instanceof TileTube)) return false; if (++placement < route.size()) next(Step.PLACE); else next(Step.LABEL_HAND); }
            case LABEL_HAND -> { if (!hand(client, value -> value.is(labelItem()) && ItemJarLabel.aspect(value) == Aspect.ENERGY)) return false; require(count(client.player, labelItem()) == 1, "only actual bound saved label available"); if (!use(client, filterPos(), Direction.SOUTH, filterPoint())) return false; next(Step.LABEL_ACK); }
            case LABEL_ACK -> { require(ticks < 40 || tube(client.level, 0).getFilter() == Aspect.ENERGY && count(client.player, labelItem()) == 0, "actual paid label attachment must acknowledge within40ticks, filter=" + tube(client.level, 0).getFilter() + "; labels=" + count(client.player, labelItem())); if (ticks < 8 || tube(client.level, 0).getFilter() != Aspect.ENERGY || count(client.player, labelItem()) != 0) return false; onServer(client, () -> { inspectMarker(serverPlayer(client), true); report.put("paid_filter_attachment", Map.of("actual_filter_RAW_snbt", beTag(tube(serverPlayer(client).level(), 0)).toString(), "actual_paid_bound_label", 1, "bound_label_supplied", 0)); }); next(Step.WRONG_WAIT); }
            case WRONG_WAIT -> { if (!serverDone()) return false; Frame frame = observed; if (frame.pressure().size() < 3) return false; if (wrongTick < 0) wrongTick = frame.tick(); if (frame.tick() - wrongTick < 50) return false; require(frame.jar() == 0 && frame.source() == 8 && frame.pressure().get("filter") == 0 && frame.pressure().get("restrict") == 0 && frame.pressure().get("oneway") == 0 && tube(client.level, 2).getFacing() == Direction.EAST, "real wrong EAST facing supplies NO upstream pressure despite actual lower jar suction"); report.put("native_wrong_direction", Map.of("actual_ticks", frame.tick() - wrongTick, "actual_pressure", frame.pressure(), "source", frame.source(), "jar", frame.jar(), "facing", "east", "actual_count", tube(client.level, 2).getTickCount())); next(Step.WRONG_CAPTURE); }
            case WRONG_CAPTURE -> { lookAt(client, Vec3.atCenterOf(route.get(1))); next(Step.WAND_AIM); capture(5); }
            case WAND_AIM -> { if (!hand(client, value -> ItemStack.matches(value, paidGold)) || ticks < 8) return false; lookAt(client, Vec3.atCenterOf(route.get(2))); var hit = TubeGeometry.retrace(client.player, tube(client.level, 2)); require(hit != null && hit.index() == 6, "actual indexed core ray for one-way correction"); next(Step.WAND_USE); }
            case WAND_USE -> { if (!use(client, route.get(2), Direction.SOUTH, Vec3.atCenterOf(route.get(2)))) return false; next(Step.WAND_ACK); }
            case WAND_ACK -> { if (ticks < 8 || tube(client.level, 2).getFacing() != Direction.UP) return false; onServer(client, () -> { require(tube(serverPlayer(client).level(), 2).getFacing() == Direction.UP && ItemStack.matches(serverPlayer(client).getMainHandItem(), paidGold), "native wand chooses connected DOWN pressure source by facingUP, zero vis debit"); }); next(Step.FLOW_WAIT); }
            case FLOW_WAIT -> { if (!serverDone()) return false; Frame frame = observed; if (frame.jar() != 8 || frame.source() != 0 || frame.routeUnits() != 0 || frame.pressureChecks() < 20) return false; require(frame.locations().get("jar").equals(Map.of("potentia", 8)), "all eight ACTUAL source POTENTIA reached jar; upper IGNIS retained"); report.put("native_resumed_filter_route", Map.of("actual_pressure", frame.pressure(), "expected_integer_half", frame.pressure().get("oneway") / 2, "actual_locations", frame.locations(), "actual_stable_pressure_checks", frame.pressureChecks(), "actual_oneway_facing", "up", "actual_filter", "potentia")); next(Step.INSPECT_HAND); }
            case INSPECT_HAND -> { if (!serverDone() || !hand(client, value -> value.is(ThaumcraftContent.RESONATOR.get()))) return false; client.options.keyShift.setDown(inspectIndex == 1); next(Step.INSPECT_SHIFT); }
            case INSPECT_SHIFT -> { if (ticks < 6 || client.player.isShiftKeyDown() != (inspectIndex == 1)) return false; chatBefore = chat(client); onServer(client, () -> expectedChat = resonatorLines(serverPlayer(client))); next(Step.INSPECT_READY); }
            case INSPECT_READY -> { if (!serverDone()) return false; next(Step.INSPECT_USE); }
            case INSPECT_USE -> { if (!use(client, inspectionPos(), Direction.SOUTH, Vec3.atCenterOf(inspectionPos()).add(0, 0, .16))) return false; client.options.keyShift.setDown(false); next(Step.INSPECT_ACK); }
            case INSPECT_ACK -> {
                if (ticks < 8 || client.player.isShiftKeyDown()) return false; var actual = chat(client).stream().filter(value -> !chatBefore.contains(value)).map(value -> value.content().getString()).toList(); if (actual.size() < expectedChat.size()) return false; require(actual.size() == expectedChat.size() && actual.containsAll(expectedChat) && actual.stream().noneMatch(value -> value.contains("-1")), "actual resonator reports raw storage/suction exactly once, NOT filter marker " + actual);
                int completed = inspectIndex; List<String> expected = expectedChat;
                onServer(client, () -> { inspectMarker(serverPlayer(client), true); require(ItemStack.matches(serverPlayer(client).getMainHandItem(), savedResonator), "native resonator unchanged and paid filter not removed"); report.put(completed == 0 ? "actual_restrict_resonator_chat" : "actual_secondary_resonator_chat", Map.of("actual_chat", actual, "server_expected_from_raw_sided_storage", expected, "secondary_use", completed == 1, "filter_still", "potentia", "public_filter_marker", -1, "filter_raw_stored_amount", tube(serverPlayer(client).level(), 0).getStoredEssentiaAmount(), "synthetic_messages", 0)); });
                next(++inspectIndex < 2 ? Step.INSPECT_HAND : Step.FLOW_CAPTURE);
            }
            case FLOW_CAPTURE -> { if (!serverDone() || ticks < 8) return false; lookAt(client, Vec3.atCenterOf(route.get(1))); client.setScreen(new ChatScreen("")); next(Step.OFFHAND_STORE); capture(6); }
            case OFFHAND_STORE -> { client.setScreen(null); appleSlot = emptyMain(client.player); click(client, playerSlot(client, appleSlot), 40, ClickType.SWAP); next(Step.OFFHAND_ACK); }
            case OFFHAND_ACK -> { if (ticks < 6 || !client.player.getOffhandItem().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(appleSlot), savedOffhand)) return false; next(Step.REMOVE_HAND); }
            case REMOVE_HAND -> { if (!emptyHand(client)) return false; client.options.keyShift.setDown(true); next(Step.REMOVE_SHIFT); }
            case REMOVE_SHIFT -> { if (ticks < 6 || !client.player.isShiftKeyDown()) return false; onServer(client, () -> { var player = serverPlayer(client); require(player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty() && player.isShiftKeyDown(), "genuine native BOTH-hands-empty secondary-use, actual apple temporarily retained in inventory"); awaitingBlank = true; }); next(Step.REMOVE_READY); }
            case REMOVE_READY -> { if (!serverDone()) return false; next(Step.REMOVE_USE); }
            case REMOVE_USE -> { if (!use(client, filterPos(), Direction.SOUTH, filterPoint())) return false; client.options.keyShift.setDown(false); next(Step.REMOVE_ACK); }
            case REMOVE_ACK -> { if (ticks < 8 || client.player.isShiftKeyDown() || tube(client.level, 0).getFilter() != null) return false; next(Step.PICKUP); }
            case PICKUP -> { if (!serverDone()) return false; if (blankDropId == null || count(client.player, labelItem()) != 1) { if (ticks % 8 == 0) onServer(client, () -> approachBlank(serverPlayer(client))); return false; } onServer(client, () -> { var player = serverPlayer(client); require(blankDrop != null && ItemStack.matches(stack(player, value -> value.is(labelItem())), blankDrop) && player.serverLevel().getEntity(blankDropId) == null, "physical pickup owns SAME real native blank UUID/components, no injected output"); inspectMarker(player, false); report.put("native_blank_return_and_pickup", Map.of("actual_UUID", blankDropId.toString(), "actual_drop_RAW_snbt", snbt(blankDrop, player), "actual_filter_now_unset", true, "native_physical_returns", 1, "item_relocation_or_output_setters", 0)); machineCamera(player); }); next(Step.OFFHAND_RETURN); }
            case OFFHAND_RETURN -> { if (!serverDone() || ticks < 8) return false; click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, savedOffhand))), 40, ClickType.SWAP); next(Step.OFFHAND_RETURNED); }
            case OFFHAND_RETURNED -> { if (ticks < 8 || !ItemStack.matches(client.player.getOffhandItem(), savedOffhand)) return false; next(Step.GOLD_RETURN); }
            case GOLD_RETURN -> { if (!ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) { click(client, playerSlot(client, itemSlot(client.player, value -> ItemStack.matches(value, paidGold))), 7, ClickType.SWAP); return false; } if (ticks < 8 || !hand(client, value -> value.is(labelItem()) && ItemJarLabel.data(value) == null)) return false; next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> { if (!serverDone() || ticks < 8) return false; require(finalVerified && nativePost == null && nativeAdds == null && serverStopped == null && captures == 7, "native hooks removed, seven exact preceding captures"); lookAt(client, Vec3.atCenterOf(filterPos()).add(.3, 0, 0)); next(Step.FINAL_CAPTURE); }
            case FINAL_CAPTURE -> { if (ticks < 8) return false; report.put("native_client_ticks", totalTicks); report.put("elapsed_wall_seconds", (System.nanoTime() - startedNanos) / 1_000_000_000D); next(Step.DONE); capture(7); }
            case DONE -> { require(captures == CAPTURES.size(), "exact eight captures114-121"); client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(workbench.equals(new BlockPos(1778, 216, 377)) && center.equals(new BlockPos(1474, 216, 377)), "root passes ONLY NEW bench1778 and accepted center1474");
        require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && player.isAlive(), "actual saved Survival, never creative conversion"); initialXp = player.totalExperience; savedInvulnerability = player.isInvulnerable(); require(initialXp == 11 && savedInvulnerability, "actual XP11/Entity Invulnerable1 unchanged");
        savedGold = player.getInventory().getItem(7).copy(); savedPrimary = stack(player, TubeFilterClientSequence::primary).copy(); savedOffhand = player.getOffhandItem().copy(); savedPhial = stack(player, value -> value.is(filledPhial())).copy(); savedResonator = stack(player, value -> value.is(ThaumcraftContent.RESONATOR.get())).copy();
        require(gold(savedGold) && ItemWandCasting.data(savedGold).vis().equals(SAVED_VIS) && FocusData.get(savedGold).isEmpty(), "EXACT accepted gold7 finite50AIR/860AQ/50ORDO, no topups");
        require(ItemWandCasting.data(savedPrimary).vis().equals(FROST_VIS) && FocusData.get(savedPrimary).is(ThaumcraftFoci.FROST_FOCUS.get()), "EXACT saved main Frost payer, not used in this story");
        require(count(player, tubeItem()) == 1 && count(player, drops()) == 8 && count(player, labelItem()) == 1 && ItemJarLabel.data(stack(player, value -> value.is(labelItem()))) == null && count(player, filledPhial()) == 1 && ItemEssence.data(savedPhial).tag().equals("potentia") && count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 3 && count(player, visFilter()) == 0, "ACTUAL saved remaining tube1/DROP8/blank1/POTENTIA8phial/empty3 and no VisFilter stock");
        require(savedPhial.getCount() == 1 && savedOffhand.is(Items.APPLE) && savedOffhand.getCount() == 1 && count(player, ThaumcraftContent.WAND.get()) == 3 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.SHOCK_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.EXCAVATION_FOCUS.get()) == 1, "three wands/all old foci/apple retained");
        var worn = List.of(ThaumcraftWearables.GOGGLES.get(), ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get());
        for (int index = 0; index < ARMOR.size(); index++) { ItemStack value = player.getItemBySlot(ARMOR.get(index)); require(value.is(worn.get(index)) && value.getDamageValue() == (index >= 2 ? 1 : 0), "actual worn gear0/0/1/1"); savedArmor.add(value.copy()); }
        for (ItemStack value : player.getInventory().items) if (!value.isEmpty()) savedStacks.add(value.copy());
        var level = player.serverLevel(); scanOld(level); var source = node(player); require(source.getNodeType() == TileNode.NodeType.NORMAL && source.getScanId().equals("9bbeaa03-a122-4968-ac6c-131f416e1365") && aspects(source.getAspectsBase()).equals(Map.of("aer", 80, "aqua", 80, "ignis", 80, "ordo", 80, "perditio", 80, "terra", 80)), "ACTUAL existing saved NORMAL node UUID/base80, current is read and NEVER reset");
        report.put("actual_node_before_RAW_snbt", beTag(source).toString()); patch(level);
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState()); level.setBlockAndUpdate(furnace, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState()); for (int depth = 1; depth <= 2; depth++) level.setBlockAndUpdate(furnace.above(depth), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.SOUTH)); level.setBlockAndUpdate(jarPos, ThaumcraftContent.JAR.get().defaultBlockState());
        require(oven(level).isEmpty() && oven(level).getVis() == 0 && alembic(level, 1).getAmount() == 0 && alembic(level, 2).getAmount() == 0 && alembic(level, 1).getFilter() == null && alembic(level, 2).getFilter() == null && jar(level).getAmount() == 0 && jar(level).getFilter() == null, "all NEW devices genuinely empty/unlabelled");
        var knowledge = KnowledgeAccess.get(player); CompoundTag before = knowledge.save().copy(); require(knowledge.isResearchComplete("TUBES") && knowledge.isResearchComplete("JARVOID"), "actual prior research reused, never replayed"); boolean already = knowledge.isResearchComplete("TUBEFILTER"); knowledge.addResearch("TUBEFILTER"); expectedKnowledge = knowledge.save().copy(); KnowledgeEvents.sync(player);
        report.put("research_fixture", Map.of("ONLY_new_key", "TUBEFILTER", "already_complete", already, "earned_here", false, "before_RAW_snbt", before.toString(), "after_RAW_snbt", expectedKnowledge.toString()));
        require(aspects(ObjectAspectResolver.get(new ItemStack(Items.COAL), level)).equals(Map.of("ignis", 2, "potentia", 2)), "native coal tags are exactly2each, not guessed fuel potential");
        for (Item item : List.of(Items.IRON_INGOT, Items.GLASS, Items.GOLD_NUGGET, Items.GOLD_INGOT, silverwood(), Items.STONE, Items.LAPIS_LAZULI, Items.COAL)) require(count(player, item) == 0, "new RAW fixtures never conflate saved stacks " + item);
        supply(player, Items.IRON_INGOT, 2); supply(player, Items.GLASS, 1); supply(player, Items.GOLD_NUGGET, 1); supply(player, Items.GOLD_INGOT, 2); supply(player, silverwood(), 1); supply(player, Items.STONE, 1); supply(player, Items.LAPIS_LAZULI, 1); rawCoalSlot = supply(player, Items.COAL, 4); fuelCoalSlot = supply(player, Items.COAL, 1);
        report.put("actual_saved_inputs", Map.of("gold7_RAW_snbt", snbt(savedGold, player), "main_Frost_RAW_snbt", snbt(savedPrimary, player), "main_stacks_RAW_snbt", savedStacks.stream().map(value -> snbt(value, player)).toList(), "worn_RAW_snbt", savedArmor.stream().map(value -> snbt(value, player)).toList(), "offhand_RAW_snbt", snbt(savedOffhand, player), "retained_phial_RAW_snbt", snbt(savedPhial, player), "total_XP", initialXp));
        inputs.add(Map.of("fixture", "empty table/furnace/two alembics/ordinary jar; bounded separate floor/air and camera", "actual_unmodified_day_time", level.getDayTime(), "world_day_fuel_clock_writes", 0, "weather_writes", 0, "RNG_writes", 0)); player.inventoryMenu.broadcastChanges(); ledger = new Ledger(player); startObservation(player);
    }

    private Item ingredient(int cell, int craft) {
        return switch (craft) {
            case 0 -> switch (cell) { case 1 -> drops(); case 3, 5 -> Items.IRON_INGOT; case 4 -> Items.GLASS; case 7 -> Items.GOLD_NUGGET; default -> Items.AIR; };
            case 1 -> cell == 0 || cell == 2 ? Items.GOLD_INGOT : cell == 1 ? silverwood() : Items.AIR;
            case 2 -> cell == 2 ? tubeItem() : cell == 6 ? visFilter() : Items.AIR;
            case 3 -> cell == 0 ? Items.STONE : cell == 8 ? tubeItem() : Items.AIR;
            case 4 -> cell == 1 ? Items.LAPIS_LAZULI : cell == 7 ? tubeItem() : Items.AIR;
            default -> cell == 0 ? labelItem() : cell == 8 ? filledPhial() : Items.AIR;
        };
    }
    private Item craftOutput() { return switch (craftIndex) { case 0 -> tubeItem(); case 1 -> visFilter(); case 2 -> filterItem(); case 3 -> restrictItem(); case 4 -> oneWayItem(); default -> labelItem(); }; }
    private int outputCount() { return craftIndex == 0 ? 8 : craftIndex == 1 ? 2 : 1; }
    private ResourceLocation craftRecipe() { return id(switch (craftIndex) { case 0 -> "arcane/essentia_tube"; case 1 -> "arcane/vis_filter"; case 2 -> "arcane/essentia_filter"; case 3 -> "arcane/essentia_restrict"; case 4 -> "arcane/essentia_oneway"; default -> "jar_label_bind"; }); }
    private Map<String, Integer> craftPrice() { return switch (craftIndex) { case 0, 1 -> Map.of("aqua", 450, "ordo", 450); case 2 -> Map.of("aqua", 450, "ordo", 1440); case 3 -> Map.of("aqua", 450, "terra", 1440); case 4 -> Map.of("aqua", 450, "ordo", 720, "perditio", 720); default -> Map.of(); }; }
    private void beginGrid(Minecraft client) {
        var clicks = new ArrayList<int[]>(); var types = new LinkedHashSet<Item>(); for (int cell = 0; cell < 9; cell++) if (ingredient(cell, craftIndex) != Items.AIR) types.add(ingredient(cell, craftIndex));
        for (Item item : types) { int from = playerSlot(client, itemSlot(client.player, value -> value.is(item))); clicks.add(new int[]{from, 0}); for (int cell = 0; cell < 9; cell++) if (ingredient(cell, craftIndex) == item) clicks.add(new int[]{cell + 2, 1}); clicks.add(new int[]{from, 0}); }
        gridClicks = List.copyOf(clicks); gridIndex = 0; onServer(client, () -> preflight(serverPlayer(client))); next(Step.GRID);
    }
    private void preflight(ServerPlayer player) {
        // Each stage copies ONLY actually present earned/saved inputs. In
        // particular no VisFilter or tube output is invented to preflight it.
        var projection = new SimpleContainer(11); for (int cell = 0; cell < 9; cell++) { Item wanted = ingredient(cell, craftIndex); if (wanted != Items.AIR) projection.setItem(cell, stack(player, value -> value.is(wanted)).copyWithCount(1)); } projection.setItem(TileArcaneWorkbench.WAND_SLOT, paidGold.copy());
        var match = ArcaneCrafting.find(player, projection); require(match != null && match.recipe().id().equals(craftRecipe()) && match.arcane() == (craftIndex < 5) && match.prices().equals(craftPrice()) && match.output().is(craftOutput()) && match.output().getCount() == outputCount(), "read-only actual-input recipe preflight BEFORE clicks " + craftRecipe());
        if (craftIndex >= 2 && craftIndex < 5) require(match.recipe().value().getSerializer() == BuiltInRegistries.RECIPE_SERIALIZER.get(id("arcane_shapeless")), "genuine native shapeless serializer for arbitrary-position upgrade, not shaped permutations");
    }
    private void verifyGrid(ContainerArcaneWorkbench menu, boolean consumed) { for (int cell = 0; cell < 9; cell++) { ItemStack actual = menu.getSlot(cell + 2).getItem(); Item wanted = ingredient(cell, craftIndex); require(consumed || wanted == Items.AIR ? actual.isEmpty() : actual.is(wanted) && actual.getCount() == 1, "actual one-per-cell grid consumption (native remainders prefer inventory), cell " + cell); } }
    private void inspectCraft(ServerPlayer player) { var match = ArcaneCrafting.find(player, table(player.level())); require(match != null && match.recipe().id().equals(craftRecipe()) && match.arcane() == (craftIndex < 5) && match.prices().equals(craftPrice()) && match.output().getCount() == outputCount(), "actual native menu preview/gate/cost"); crafts.add(Map.of("stage", craftIndex, "actual_recipe", match.recipe().id().toString(), "actual_input_copies", match.input().items().stream().map(value -> value.isEmpty() ? "EMPTY" : snbt(value, player)).toList(), "actual_output_preview", snbt(match.output(), player), "actual_price_centivis", match.prices(), "actual_payer_before", snbt(table(player.level()).getItem(10), player), "backend_craft_calls", 0)); }
    private void verifyCrafts(ServerPlayer player) { verifyDebit(chargedGold, player.getInventory().getItem(7), TOTAL_PRICE); require(crafts.size() == 6 && table(player.level()).isEmpty() && player.containerMenu.getCarried().isEmpty() && count(player, tubeItem()) == 6 && count(player, visFilter()) == 1 && count(player, filterItem()) == 1 && count(player, restrictItem()) == 1 && count(player, oneWayItem()) == 1 && count(player, drops()) == 7 && count(player, labelItem()) == 1 && ItemJarLabel.aspect(stack(player, value -> value.is(labelItem()))) == Aspect.ENERGY && ItemStack.matches(stack(player, value -> value.is(filledPhial())), savedPhial), "five real arcane crafts + zero-vis label binding; actual phial unchanged; all outputs/remaining inputs exact"); for (Item raw : List.of(Items.IRON_INGOT, Items.GLASS, Items.GOLD_NUGGET, Items.GOLD_INGOT, silverwood(), Items.STONE, Items.LAPIS_LAZULI)) require(count(player, raw) == 0, "declared real recipe raw fully consumed " + raw); report.put("native_craft_totals", Map.of("arcane_crafts", 5, "ordinary_bindings", 1, "actual_total_price", TOTAL_PRICE, "basic_tubes_before_any_route_placement", 6, "remaining_earned_VisFilter", 1, "retained_actual_quicksilver_DROPs", 7, "actual_phial_remainder_RAW_snbt", snbt(stack(player, value -> value.is(filledPhial())), player), "actual_gold_after_RAW_snbt", snbt(player.getInventory().getItem(7), player), "output_grants", 0)); }

    /** Passive accounting of ALL actual primal gains, including native capped
     * partial-unit rounding. One source unit can yield fewer than100 centivis;
     * the original consumes that whole source unit and we report the difference. */
    private final class ChargeProbe {
        final ServerPlayer player; final TileNode source; final ItemStack wand, original; final WandData before;
        final Map<String, Integer> sourceBefore, base, gained = new LinkedHashMap<>(), sourceSpent = new LinkedHashMap<>(), regenerated = new LinkedHashMap<>();
        final long started; WandData previous; Map<String, Integer> previousSource; boolean active = true; volatile boolean used, funded; volatile int stopped;
        ChargeProbe(ServerPlayer player) { this.player = player; source = node(player); wand = player.getMainHandItem(); original = wand.copy(); before = previous = ItemWandCasting.data(wand); sourceBefore = previousSource = aspects(source.getAspects()); base = aspects(source.getAspectsBase()); started = player.level().getGameTime(); require(ItemStack.matches(wand, savedGold) && !player.isUsingItem() && !ItemWandCasting.isUsingNode(player) && !player.isShiftKeyDown() && KnowledgeAccess.get(player).isResearchComplete("NODEPRESERVE"), "NEW exact saved gold charge session with actual earned native preservation, not inherited or crouched"); for (String tag : List.of("aqua", "ordo")) require(sourceBefore.getOrDefault(tag, 0) - 1 >= (TOTAL_PRICE.get(tag) - before.amount(tag) + 99) / 100, "actual finite saved node budget can fund " + tag + "; current=" + sourceBefore); }
        void observe() {
            require(player.getMainHandItem() == wand && sameExceptVis(wand, original) && player.level().getBlockEntity(chargingNode) == source && aspects(source.getAspectsBase()).equals(base) && player.level().getGameTime() - started <= 2600, "same actual saved source/base/wand identity, bounded native charge");
            WandData now = ItemWandCasting.data(wand); Map<String, Integer> current = aspects(source.getAspects()); int moved = 0, regenTotal = 0;
            for (String tag : WandData.PRIMALS) { int delta = now.amount(tag) - previous.amount(tag); require(delta >= 0 && delta <= 100 && (delta == 0 || delta == 100 || now.amount(tag) == now.capacity()), "only real native single-unit/capped-partial charge, no expenses or topups " + tag); int spent = (delta + 99) / 100; int regen = current.getOrDefault(tag, 0) - previousSource.getOrDefault(tag, 0) + spent; require(regen >= 0 && regen <= 1, "actual node current equals previous minus whole units plus native regen " + tag); if (delta > 0) gained.merge(tag, delta, Integer::sum); if (spent > 0) sourceSpent.merge(tag, spent, Integer::sum); if (regen > 0) regenerated.merge(tag, regen, Integer::sum); moved += spent; regenTotal += regen; }
            require(moved <= 1 && regenTotal <= 1, "one original tap and at most one natural normal-node regeneration per real tick"); String session = "released";
            if (player.isUsingItem()) { require(player.getUseItem() == wand && player.getUsedItemHand() == InteractionHand.MAIN_HAND && ItemWandCasting.isUsingNode(player) && serverSessionNode(player, wand).equals(chargingNode), "actual SERVER-side held node session owns this exact wand/node"); used = true; session = chargingNode.toShortString(); }
            if (moved > 0 || regenTotal > 0) chargeRows.add(Map.of("native_tick", player.level().getGameTime(), "wand_before", previous.vis(), "wand_after", now.vis(), "source_before", previousSource, "source_after", current, "actual_server_session_node", session, "using", player.isUsingItem()));
            funded = now.amount("aqua") >= 2250 && now.amount("ordo") >= 3060; stopped = used && !player.isUsingItem() ? stopped + 1 : 0; require(funded || stopped < 2, "native saved-node session stopped before funding; current=" + now.vis() + "; source=" + current); previous = now; previousSource = current;
        }
        Map<String, Object> finish() { require(used && funded && stopped >= 2 && !ItemWandCasting.isUsingNode(player) && !gained.isEmpty(), "native funded charge and released own-side session completed"); var rounded = new LinkedHashMap<String, Integer>(); for (String tag : WandData.PRIMALS) { require(source.getAspects().getAmount(Aspect.getAspect(tag)) == sourceBefore.getOrDefault(tag, 0) - sourceSpent.getOrDefault(tag, 0) + regenerated.getOrDefault(tag, 0), "actual saved source budget reconciled " + tag); require(ItemWandCasting.data(wand).amount(tag) - before.amount(tag) == gained.getOrDefault(tag, 0), "ALL actual primal gains reconciled " + tag); rounded.put(tag, 100 * sourceSpent.getOrDefault(tag, 0) - gained.getOrDefault(tag, 0)); } active = false; return Map.of("source", chargingNode.toShortString(), "actual_node_UUID", source.getScanId(), "actual_source_before", sourceBefore, "actual_source_after", aspects(source.getAspects()), "base_unchanged", base, "actual_centivis_gained_by_aspect", Map.copyOf(gained), "whole_source_units_spent_by_aspect", Map.copyOf(sourceSpent), "native_regeneration_by_aspect", Map.copyOf(regenerated), "original_cap_rounding_centivis", rounded, "actual_wand_after", ItemWandCasting.data(wand).vis()); }
    }

    private void identifySource(ServerPlayer player) {
        var low = alembic(player.level(), 1); var high = alembic(player.level(), 2); require(low.getAmount() == 8 && high.getAmount() == 8 && low.getAspect() != high.getAspect() && (low.getAspect() == Aspect.ENERGY || high.getAspect() == Aspect.ENERGY) && (low.getAspect() == Aspect.FIRE || high.getAspect() == Aspect.FIRE), "actual unseeded native output order, exactly8each");
        potentiaDepth = low.getAspect() == Aspect.ENERGY ? 1 : 2; var path = new ArrayList<BlockPos>(); for (int x = 1; x <= 3; x++) path.add(furnace.above(potentiaDepth).east(x)); if (potentiaDepth == 2) path.add(jarPos.above()); route = List.copyOf(path); machineCamera(player);
        report.put("actual_unseeded_source_and_route", Map.of("lower", low.getAspect().getTag(), "upper", high.getAspect().getTag(), "POTENTIA_depth", potentiaDepth, "actual_route", route.stream().map(BlockPos::toShortString).toList(), "extra_earned_basic_tube_required", potentiaDepth == 2 ? 1 : 0, "RNG_or_aspect_seed_writes", 0));
    }
    private void inspectMarker(ServerPlayer player, boolean configured) {
        TileTube filter = tube(player.level(), 0); require(filter.isFilter() && filter instanceof IAspectContainer, "actual registered thin filter API type"); AspectList marker = ((IAspectContainer) filter).getAspects();
        require(configured ? filter.getFilter() == Aspect.ENERGY && marker != null && marker.size() == 1 && marker.getAmount(Aspect.ENERGY) == -1 : filter.getFilter() == null && marker == null, "public filter marker is configured -1 or null, NOT stored essentia"); require(filter.getStoredEssentiaAmount() == 0 && filter.getEssentiaAmount(Direction.SOUTH) == 0, "marker must NOT fabricate a buffered essentia unit");
    }
    private List<String> resonatorLines(ServerPlayer player) {
        inspectMarker(player, true); TileTube target = tube(player.level(), inspectIndex == 0 ? 1 : 0); Direction side = Direction.SOUTH; var hit = TubeGeometry.retrace(player, target); if (hit != null && hit.index() >= 0 && hit.index() < 6) side = Direction.from3DDataValue(hit.index());
        var result = new ArrayList<String>(); Aspect type = target.getEssentiaType(side), suction = target.getSuctionType(side); if (type != null) result.add(Component.translatable("tc.resonator1", "" + target.getEssentiaAmount(side), type.getName()).getString()); result.add(Component.translatable("tc.resonator2", "" + target.getSuctionAmount(side), suction == null ? Component.translatable("tc.resonator3").getString() : suction.getName()).getString()); return List.copyOf(result);
    }

    private record Frame(long tick, int coal, int rawSmelted, int fuelSpent, int burnTime, int source, int routeUnits,
                         int jar, int pressureChecks, Map<String, Integer> pressure, Map<String, Integer> stored,
                         Map<String, Map<String, Integer>> locations) {}

    /** All real physical storage is counted, never IAspectContainer's filter
     * marker. Native coal debits are classified by actual ignition/essentia
     * changes, not by replacing a raw snapshot with an expected fixture value. */
    private final class Ledger {
        final ServerPlayer player;
        Frame previous;
        int rawSmelted, fuelSpent, checks, jarUnits, pressureChecks;
        long lastJarPull = Long.MIN_VALUE / 2;
        Ledger(ServerPlayer player) { this.player = player; previous = snapshot(); checkBalance(previous); observed = previous; }
        void observe() {
            require(player.isAlive() && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded live native player/story, including capture I/O");
            Frame now = snapshot(); int debit = previous.coal() - now.coal(); require(debit >= 0 && debit <= 2, "ONLY real raw/fuel coal debits, no silent loss/grants"); int ignition = now.burnTime() > previous.burnTime() ? 1 : 0;
            if (ignition > 0) { require(fuelSpent == 0 && debit >= 1 && oven(player.level()).getCurrentItemBurnTime() == 1600 && !oven(player.level()).isSpeedBoost(), "ONE actual normal coal1600 ignition, separate from raw distillation"); ++fuelSpent; transfers.add(Map.of("native_tick", now.tick(), "event", "actual separate FUEL coal ignition", "coal_consumed", 1, "native_duration", 1600, "essentia_produced", 0)); }
            int smelted = debit - ignition; require(smelted >= 0 && smelted <= 1, "at most one actual raw smelt per tick"); rawSmelted += smelted; require(rawSmelted <= 4, "only four actual RAW coals supplied");
            for (String tag : BUDGET.keySet()) require(now.stored().getOrDefault(tag, 0) - previous.stored().getOrDefault(tag, 0) == smelted * 2, "native RAW2each plus exact global transfer conservation " + tag);
            if (smelted > 0) transfers.add(Map.of("native_tick", now.tick(), "event", "actual RAW coal consumed", "coal_consumed", 1, "actual_essentia_created", Map.of("ignis", 2, "potentia", 2)));
            if (potentiaDepth > 0 && previous.rawSmelted() == 4 && now.rawSmelted() == 4 && previous.locations().get("furnace").isEmpty() && now.locations().get("furnace").isEmpty()) {
                for (String tag : BUDGET.keySet()) {
                    int incoming = amount(previous, "alembic:" + potentiaDepth, tag) - amount(now, "alembic:" + potentiaDepth, tag); require(incoming >= 0 && incoming <= 1, "actual source emits at most one route unit");
                    for (int index = 0; index < route.size(); index++) { int outgoing = amount(previous, "route:" + index, tag) + incoming - amount(now, "route:" + index, tag); require(outgoing >= 0 && outgoing <= 1, "EACH native route hop preserves capacity1, including same-tick pass-through"); if (incoming > 0) transfers.add(Map.of("native_tick", now.tick(), "from", index == 0 ? "actual POTENTIA alembic" : "route:" + (index - 1), "to", "route:" + index, "aspect", tag, "supplied", incoming, "stored", incoming, "voided", 0)); incoming = outgoing; }
                    require(amount(now, "jar", tag) - amount(previous, "jar", tag) == incoming && amount(now, "alembic:" + (3 - potentiaDepth), tag) == amount(previous, "alembic:" + (3 - potentiaDepth), tag), "last native tube debit equals exact jar increase; other8 output untouched");
                }
            }
            int added = now.jar() - previous.jar(); require(added >= 0 && added <= 1, "one native jar unit at most"); if (added > 0) { require(now.tick() - lastJarPull >= 5, "real jar local5tick pull cadence"); lastJarPull = now.tick(); jarUnits += added; transfers.add(Map.of("native_tick", now.tick(), "from", "last actual route tube", "to", "new ordinary jar", "supplied", added, "stored", added, "voided", 0)); }
            boolean stable = route.size() >= 3 && route.stream().allMatch(pos -> player.level().getBlockEntity(pos) instanceof TileTube value && value.getVenting() == 0) && tube(player.level(), 0).getFilter() == Aspect.ENERGY && tube(player.level(), 2).getFacing() == Direction.UP && now.pressure().get("oneway") == (potentiaDepth == 2 ? 30 : 31) && now.pressure().get("restrict") == now.pressure().get("oneway") / 2 && now.pressure().get("filter") == now.pressure().get("restrict") - 1 && tube(player.level(), 0).getSuctionType(Direction.WEST) == Aspect.ENERGY && tube(player.level(), 1).getSuctionType(Direction.WEST) == Aspect.ENERGY && tube(player.level(), 2).getSuctionType(Direction.WEST) == Aspect.ENERGY;
            pressureChecks = stable ? pressureChecks + 1 : 0;
            now = new Frame(now.tick(), now.coal(), rawSmelted, fuelSpent, now.burnTime(), now.source(), now.routeUnits(), now.jar(), pressureChecks, now.pressure(), now.stored(), now.locations()); checkBalance(now);
            if (debit > 0 || !now.locations().equals(previous.locations()) || !now.pressure().equals(previous.pressure())) balances.add(Map.of("native_tick", now.tick(), "actual_physical_coals", now.coal(), "actual_raw_smelt_events", rawSmelted, "actual_fuel_ignitions", fuelSpent, "actual_stored", now.stored(), "actual_locations", now.locations(), "actual_pressure", now.pressure())); ++checks; previous = now; observed = now;
        }
        Frame snapshot() {
            var level = player.serverLevel(); var locations = new LinkedHashMap<String, Map<String, Integer>>(); locations.put("furnace", aspects(oven(level).getAspects())); for (int depth = 1; depth <= 2; depth++) locations.put("alembic:" + depth, singleton(alembic(level, depth).getAspect(), alembic(level, depth).getAmount()));
            int routeUnits = 0; var pressure = new LinkedHashMap<String, Integer>(); for (int index = 0; index < route.size(); index++) { BlockEntity tile = level.getBlockEntity(route.get(index)); if (tile instanceof TileTube tube) { int actual = tube.getStoredEssentiaAmount(); require(actual >= 0 && actual <= 1 && (actual == 0 || tube.getStoredEssentiaType() == Aspect.ENERGY), "actual route buffer remains capacity1/POTENTIA only, marker ignored"); routeUnits += actual; locations.put("route:" + index, singleton(tube.getStoredEssentiaType(), actual)); if (index < 3) pressure.put(index == 0 ? "filter" : index == 1 ? "restrict" : "oneway", tube.getSuctionAmount(Direction.WEST)); } }
            locations.put("jar", singleton(jar(level).getAspect(), jar(level).getAmount())); var physical = physicalStacks(player); var physicalAspects = new LinkedHashMap<>(itemAspects(physical)); int oldPhial = physicalAspects.getOrDefault("potentia", 0) - 8; require(oldPhial >= 0, "actual old8phial conserved across inventory/cursor/native recipe remainder/table/drops/pending"); if (oldPhial == 0) physicalAspects.remove("potentia"); else physicalAspects.put("potentia", oldPhial); locations.put("new physical items", Map.copyOf(physicalAspects));
            int labelOwners = physical.stream().mapToInt(value -> countStack(value, labelItem())).sum(); if (!route.isEmpty() && level.getBlockEntity(filterPos()) instanceof TileTube filter && filter.getFilter() != null) ++labelOwners; require(labelOwners == 1, "EXACT saved label ownership: blank/bound physical stack OR configured filter OR durable real blank return, never duplication/loss");
            int coal = physical.stream().mapToInt(value -> countStack(value, Items.COAL)).sum() + countStack(oven(level).getItem(0), Items.COAL) + countStack(oven(level).getItem(1), Items.COAL); var total = new LinkedHashMap<String, Integer>(); for (var contents : locations.values()) contents.forEach((tag, amount) -> total.merge(tag, amount, Integer::sum));
            return new Frame(level.getGameTime(), coal, rawSmelted, fuelSpent, oven(level).getBurnTime(), potentiaDepth == 0 ? 0 : alembic(level, potentiaDepth).getAmount(), routeUnits, jar(level).getAmount(), pressureChecks, Map.copyOf(pressure), Map.copyOf(total), Map.copyOf(locations));
        }
        void checkBalance(Frame frame) { require(frame.coal() == 5 - frame.rawSmelted() - frame.fuelSpent() && frame.rawSmelted() >= 0 && frame.rawSmelted() <= 4 && frame.fuelSpent() >= 0 && frame.fuelSpent() <= 1, "all five actual coals are physical or separately observed RAW/FUEL debits"); for (String tag : BUDGET.keySet()) require(BUDGET.get(tag) == 2 * (4 - frame.rawSmelted()) + frame.stored().getOrDefault(tag, 0), "EXACT finite supplied = unprocessed RAW potential + ALL actually stored, no void/grants/loss " + tag); for (String tag : frame.stored().keySet()) require(BUDGET.containsKey(tag), "unexpected new stored aspect " + tag); }
    }

    private void startObservation(ServerPlayer player) {
        nativePost = server -> { if (server != player.server || observationFailure != null) return; try { if (charge != null && charge.active) charge.observe(); ledger.observe(); } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); } };
        nativeAdds = (entity, level) -> { if (level == player.level() && awaitingBlank && entity instanceof ItemEntity item && item.getItem().is(labelItem()) && patchBounds().contains(item.position())) try { require(blankDropId == null && item.getItem().getCount() == 1 && ItemJarLabel.data(item.getItem()) == null, "ONE actual blank native label return, not bound/duplicated output"); blankDropId = item.getUUID(); blankDrop = item.getItem().copy(); } catch (RuntimeException | Error error) { observationFailure = error; } return EventResult.pass(); };
        serverStopped = server -> { if (server == player.server) { stopObservation(); owningClient.execute(() -> releaseInputs(owningClient)); } }; TickEvent.SERVER_POST.register(nativePost); EntityEvent.ADD.register(nativeAdds); LifecycleEvent.SERVER_STOPPED.register(serverStopped); ClientTickEvent.CLIENT_PRE.register(chargeGuard);
    }
    private void stopObservation() { if (nativePost != null) { TickEvent.SERVER_POST.unregister(nativePost); nativePost = null; } if (nativeAdds != null) { EntityEvent.ADD.unregister(nativeAdds); nativeAdds = null; } if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; } if (charge != null) charge.active = false; owningClient.execute(() -> ClientTickEvent.CLIENT_PRE.unregister(chargeGuard)); }
    private void guardCharge(Minecraft client) { try { if (client.player == null || client.level == null || client.gameMode == null) return; if (aimCharge && client.screen == null) sendRotation(client, Vec3.atCenterOf(chargingNode)); if (chargingHeld && !releaseSent && (charge != null && charge.funded || observationFailure != null || !client.isWindowActive() || client.screen != null || ++chargeTicks >= 2400)) releaseUse(client); } catch (RuntimeException | Error error) { observationFailure = error; releaseUse(client); } }
    private void releaseUse(Minecraft client) { chargingHeld = false; client.options.keyUse.setDown(false); if (!releaseSent && client.player != null && client.gameMode != null) { client.gameMode.releaseUsingItem(client.player); releaseSent = true; } }

    private void scanOld(ServerLevel level) {
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 290) >> 4; x++) for (int z = (center.getZ() - 18) >> 4; z <= (center.getZ() + 14) >> 4; z++) level.getChunk(x, z);
        int jars = 0, units = 0, originalJars = 0, originalUnits = 0, candles = 0;
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-12, -2, -18), center.offset(290, 10, 14))) { oldBlocks.put(at.immutable(), level.getBlockState(at)); BlockEntity tile = level.getBlockEntity(at); if (tile instanceof TileNode) oldNodes.put(at.immutable(), stableNodeTag(tile)); else if (tile != null) oldBEs.put(at.immutable(), beTag(tile)); if (tile instanceof TileJar jar) { ++jars; units += jar.getAmount(); if (at.getX() <= 1704) { ++originalJars; originalUnits += jar.getAmount(); } } if (level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get())) ++candles; }
        require(oldBEs.size() == 53 && jars == 15 && units == 309 && originalJars == 12 && originalUnits == 173 && candles == 72, "ACTUAL whole accepted valve-world scan:53non-nodeBE/15jars309/original12jars173/72candles, not blindly assumed"); for (BlockPos at : OLD_FURNACES) require(level.getBlockEntity(at) instanceof TileAlchemyFurnace hot && hot.getItem(0).isEmpty(), "actual old furnace has EMPTY input, so no extra ignition/fuel debit " + at);
        require(level.getBlockEntity(new BlockPos(1724, 216, 377)) instanceof TileJar oldVoid && oldVoid.isVoid() && oldVoid.getAmount() == 64 && oldVoid.getFilter() == Aspect.ENERGY, "accepted old labelled void64 retained");
        BlockPos acceptedValve = new BlockPos(1755, 217, 377);
        require(level.getBlockEntity(acceptedValve) instanceof TileTube valve && valve.isValve() && valve.isFlowAllowed() && !valve.hadPower() && valve.getFacing() == Direction.SOUTH && !level.hasNeighborSignal(acceptedValve), "accepted FINAL unpowered/open valve, not failed client2's powered manual-reopen stop");
        require(level.getBlockEntity(new BlockPos(1756, 216, 377)) instanceof TileJar priorJar && !priorJar.isVoid() && priorJar.getAmount() == 8 && priorJar.getAspect() == Aspect.ENERGY, "accepted actual final8POTENTIA jar, not failed FIRST_FLOW world");
        report.put("actual_old_world_scan", Map.of("min", center.offset(-12, -2, -18).toShortString(), "max", center.offset(290, 10, 14).toShortString(), "actual_non_node_BEs", oldBEs.size(), "actual_jars", jars, "actual_jar_units", units, "original_jars", originalJars, "original_units", originalUnits, "actual_candles", candles, "actual_old_furnaces_RAW_snbt", OLD_FURNACES.stream().map(at -> beTag(level.getBlockEntity(at)).toString()).toList()));
    }
    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) { if (ItemStack.isSameItemSameComponents(saved, savedGold)) continue; int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum(); int after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum(); int delta = saved.is(tubeItem()) ? 5 - (potentiaDepth == 2 ? 1 : 0) : saved.is(drops()) ? -1 : 0; require(after == before + delta, "all old main components/counts exact EXCEPT one savedDROP and actual new/consumed/placed tube ledger " + saved); }
        for (int index = 0; index < ARMOR.size(); index++) require(ItemStack.matches(player.getItemBySlot(ARMOR.get(index)), savedArmor.get(index)), "exact saved worn gear"); require(ItemStack.matches(player.getInventory().getItem(7), paidGold) && ItemStack.matches(stack(player, TubeFilterClientSequence::primary), savedPrimary) && ItemStack.matches(player.getOffhandItem(), savedOffhand) && ItemStack.matches(stack(player, value -> value.is(filledPhial())), savedPhial) && ItemStack.matches(stack(player, value -> value.is(ThaumcraftContent.RESONATOR.get())), savedResonator) && count(player, ThaumcraftContent.WAND.get()) == 3, "all other wands/foci/phial/resonator/apple exact; real paid gold returned7");
        require(player.totalExperience == initialXp && initialXp == 11 && player.isInvulnerable() == savedInvulnerability && player.isAlive() && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && expectedKnowledge.equals(KnowledgeAccess.get(player).save()), "XP11/Entity flag/Survival/all prior knowledge exact, ONLY declared TUBEFILTER fixture added");
        for (var entry : oldBlocks.entrySet()) { BlockPos at = entry.getKey(); BlockState before = entry.getValue(), after = player.level().getBlockState(at); BlockPos cactus = center.offset(149, 0, 5); boolean column = at.getX() == cactus.getX() && at.getZ() == cactus.getZ() && at.getY() >= cactus.getY() && at.getY() <= cactus.getY() + 2; boolean growth = column && (before.is(Blocks.CACTUS) && after.is(Blocks.CACTUS) || before.isAir() && after.is(Blocks.CACTUS) && player.level().getBlockState(at.below()).is(Blocks.CACTUS)); boolean clock = OLD_FURNACES.contains(at) && before.getBlock() == after.getBlock() && before.setValue(BlockAlchemyFurnace.LIT, false) == after.setValue(BlockAlchemyFurnace.LIT, false); require(before == after || growth || clock, "all accepted old blocks retained; only native cactus/LIT transients " + at); }
        for (var entry : oldBEs.entrySet()) { BlockEntity actual = player.level().getBlockEntity(entry.getKey()); require(actual != null, "old BE identity remains " + entry.getKey()); CompoundTag after = beTag(actual); if (OLD_FURNACES.contains(entry.getKey())) { CompoundTag beforeStable = entry.getValue().copy(), afterStable = after.copy(); beforeStable.remove("BurnTime"); afterStable.remove("BurnTime"); require(beforeStable.equals(afterStable) && after.getShort("BurnTime") >= 0 && after.getShort("BurnTime") <= entry.getValue().getShort("BurnTime") && ((TileAlchemyFurnace) actual).getItem(0).isEmpty() && player.level().getBlockState(entry.getKey()).getValue(BlockAlchemyFurnace.LIT) == (((TileAlchemyFurnace) actual).getBurnTime() > 0), "ONLY native old BurnTime/LIT may decrease, all old items/essentia/filters/identity exact, no new ignition"); report.put("retained_old_furnace_" + entry.getKey().toShortString(), Map.of("before_RAW_snbt", entry.getValue().toString(), "after_RAW_snbt", after.toString(), "whitelisted_fields", List.of("BurnTime", "matching block LIT"))); } else require(after.equals(entry.getValue()), "all other old BE data EXACT " + entry.getKey()); }
        for (var entry : oldNodes.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileNode && stableNodeTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "saved node identity/base/type retained; current charge/regen explicitly accounted");
    }
    private void finalState(ServerPlayer player) {
        retained(player); Frame frame = observed; ledger.checkBalance(frame); verifyDebit(chargedGold, player.getInventory().getItem(7), TOTAL_PRICE);
        require(frame.rawSmelted() == 4 && frame.fuelSpent() == 1 && frame.coal() == 0 && frame.stored().equals(BUDGET) && ledger.jarUnits == 8 && ledger.checks >= 500 && charge != null && charge.funded && !charge.active && charge.stopped >= 2 && crafts.size() == 6, "genuine funded five crafts/binding/four raw coals/one fuel/all8 transfer observed, no seed/grant/loss");
        require(oven(player.level()).isEmpty() && oven(player.level()).getVis() == 0 && alembic(player.level(), potentiaDepth).getAmount() == 0 && alembic(player.level(), 3 - potentiaDepth).getAmount() == 8 && alembic(player.level(), 3 - potentiaDepth).getAspect() == Aspect.FIRE && jar(player.level()).getAmount() == 8 && jar(player.level()).getAspect() == Aspect.ENERGY && jar(player.level()).getFilter() == null && frame.routeUnits() == 0, "actual new POTENTIA8jar + untouched other IGNIS8, all real buffers empty");
        inspectMarker(player, false); require(tube(player.level(), 1).isRestrict() && tube(player.level(), 2).isOneWay() && tube(player.level(), 2).getFacing() == Direction.UP && count(player, tubeItem()) == 6 - (potentiaDepth == 2 ? 1 : 0) && count(player, visFilter()) == 1 && count(player, filterItem()) == 0 && count(player, restrictItem()) == 0 && count(player, oneWayItem()) == 0 && count(player, labelItem()) == 1 && ItemJarLabel.data(stack(player, value -> value.is(labelItem()))) == null && count(player, drops()) == 7, "all genuinely earned variants placed, one VisFilter retained, exact free tube/DROP count and ONE physical returned blank");
        require(blankDropId != null && blankDrop != null && inspectIndex == 2 && table(player.level()).isEmpty() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty() && !player.isUsingItem() && !player.isShiftKeyDown() && !ItemWandCasting.isUsingNode(player), "native sessions/menu/cursor/Shift released, both actual inspections and blank return witnessed"); require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && pendingStacks(player.serverLevel()).isEmpty(), "no lost/duplicated physical/pending outputs or XP grants");
        report.put("final_exact_unit_accounting", Map.of("finite_RAW_potential", BUDGET, "actual_stored", frame.stored(), "actual_locations", frame.locations(), "actual_RAW_coal_smelted", frame.rawSmelted(), "actual_FUEL_coal_ignited", frame.fuelSpent(), "actual_coal_remaining", frame.coal(), "native_POST_checks", ledger.checks, "actual_native_jar_units", ledger.jarUnits, "voided_or_lost", 0));
        report.put("final_inventory_and_price_ledger", Map.of("actual_gold_RAW_snbt", snbt(player.getInventory().getItem(7), player), "actual_charge_then_price", TOTAL_PRICE, "saved_quicksilver_DROPs_remaining", 7, "actual_basic_tubes_remaining", count(player, tubeItem()), "actual_VisFilter_remaining", 1, "old_POTENTIA8_phial_EXACT", snbt(stack(player, value -> value.is(filledPhial())), player), "actual_returned_blank_RAW_snbt", snbt(stack(player, value -> value.is(labelItem())), player), "all_three_variants_in_world", true, "saved_total_XP", player.totalExperience, "acquisition_replayed", false));
        report.put("new_devices_at_finish_RAW_snbt", Map.of("furnace", beTag(oven(player.level())).toString(), "filter", beTag(tube(player.level(), 0)).toString(), "restrict", beTag(tube(player.level(), 1)).toString(), "oneway", beTag(tube(player.level(), 2)).toString(), "jar", beTag(jar(player.level())).toString(), "future_save_caveat", "New furnace1786,216,377 may still burn its already-paid ordinary fuel. A later continuation must allow native BurnTime/LIT decay, never freeze/reset it or require those transient clocks exact."));
        report.put("retained_prior_outputs", Map.of("actual_old_non_node_BEs", oldBEs.size(), "old15jars309units_original12jars173", true, "old72candles", true, "complete_accepted_valve_voidjar_patches", true, "all_old_wands_foci_armor_phial_resonator_apple", true, "only_two_old_native_furnace_clock_exceptions", true, "all_prior_knowledge_pools_scans_warp", true)); report.put("actual_saved_node_after_story_RAW_snbt", beTag(node(player)).toString()); finalVerified = true;
    }

    private void approachBlank(ServerPlayer player) {
        if (blankDropId == null || count(player, labelItem()) == 1) return; require(player.serverLevel().getEntity(blankDropId) instanceof ItemEntity item && ItemStack.matches(item.getItem(), blankDrop), "same actual blank physical item until pickup, never a reconstructed replacement"); ItemEntity item = (ItemEntity) player.serverLevel().getEntity(blankDropId);
        for (Direction direction : Direction.Plane.HORIZONTAL) { Vec3 candidate = new Vec3(item.getX() + direction.getStepX() * 1.05, workbench.getY(), item.getZ() + direction.getStepZ() * 1.05); if (player.level().noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) { stand(player, candidate.x, candidate.y, candidate.z); return; } } throw new IllegalStateException("No collision-free approach to actual blank drop at " + item.position());
    }
    private BlockPos patchMin() { return workbench.offset(-4, -2, -7); }
    private BlockPos patchMax() { return workbench.offset(17, 7, 8); }
    private AABB patchBounds() { return new AABB(Vec3.atLowerCornerOf(patchMin()), Vec3.atLowerCornerOf(patchMax().offset(1, 1, 1))); }
    private void patch(ServerLevel level) { for (int x = patchMin().getX() >> 4; x <= patchMax().getX() >> 4; x++) for (int z = patchMin().getZ() >> 4; z <= patchMax().getZ() >> 4; z++) level.getChunk(x, z); require(level.getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, patchBounds()).isEmpty() && pendingStacks(level).isEmpty(), "new patch refuses all old entities/physical/pending items/XP"); for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft") && !oldBlocks.containsKey(at), "never overwrite old BE/TC/scanned cells " + at); for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState()); }
    private int supply(ServerPlayer player, Item item, int amount) { require(item == Items.IRON_INGOT && amount == 2 || item == Items.GOLD_INGOT && amount == 2 || (item == Items.GLASS || item == Items.GOLD_NUGGET || item == silverwood() || item == Items.STONE || item == Items.LAPIS_LAZULI) && amount == 1 || item == Items.COAL && (amount == 4 || amount == 1), "ONLY declared finite RAW fixtures, never output/vis/essentia/XP"); int slot = emptyMain(player); player.getInventory().setItem(slot, new ItemStack(item, amount)); inputs.add(Map.of("raw_item", BuiltInRegistries.ITEM.getKey(item).toString(), "count", amount, "purpose", item == Items.COAL ? amount == 4 ? "RAW distillation" : "separate normal FUEL" : "native recipe RAW", "genuinely_empty_main_slot", slot)); return slot; }
    private List<ItemStack> physicalStacks(ServerPlayer player) {
        var result = new ArrayList<ItemStack>(); result.addAll(player.getInventory().items); result.add(player.containerMenu.getCarried());
        // The REAL matrix is storage while the saved8phial is being bound or
        // returned. Slot9 is a preview, NEVER physical output ownership.
        var table = table(player.level()); for (int cell = 0; cell < 9; cell++) result.add(table.getItem(cell)); result.add(table.getItem(TileArcaneWorkbench.WAND_SLOT));
        for (ItemEntity entity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds())) result.add(entity.getItem()); result.addAll(pendingStacks(player.serverLevel())); return result;
    }
    private static Map<String, Integer> itemAspects(List<ItemStack> stacks) { var result = new LinkedHashMap<String, Integer>(); for (ItemStack value : stacks) { if (ItemJar.isJar(value)) merge(result, ItemJar.getAspect(value), ItemJar.getAmount(value) * value.getCount()); if (value.is(filledPhial())) { var data = ItemEssence.data(value); require(data != null && Aspect.getAspect(data.tag()) != null, "actual filled-phial component"); merge(result, Aspect.getAspect(data.tag()), PhialData.AMOUNT * value.getCount()); } } return Map.copyOf(result); }
    @SuppressWarnings("unchecked") private List<ItemStack> pendingStacks(ServerLevel level) { try { var field = DeferredItemDrops.class.getDeclaredField("FACTORY"); field.setAccessible(true); var factory = (SavedData.Factory<DeferredItemDrops>) field.get(null); var queue = level.getDataStorage().get(factory, "thaumcraft_pending_inventory_drops"); if (queue == null) return List.of(); var result = new ArrayList<ItemStack>(); for (Tag raw : queue.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) { CompoundTag row = (CompoundTag) raw; if (!patchBounds().contains(Vec3.atCenterOf(BlockPos.of(row.getLong("Source"))))) continue; ItemStack value = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElseThrow(); value.setCount(row.getInt("Count")); result.add(value); } return result; } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only pending-output view needs root API reconciliation", error); } }
    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException { int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages"); ResourceLocation wanted = id("arcane/essentia_filter"); for (int index = page; index < Math.min(page + 2, pages.size()); index++) if (BookClientSmoke.field(pages.get(index), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(wanted)) { require(display.recipe() == client.level.getRecipeManager().byKey(wanted).orElseThrow(), "native book uses actual synced advanced recipe holder"); return true; } require(++turns <= 18, "bounded native TUBEFILTER recipe navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow(); client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false; }
    private void capture(int index) { require(captures == index, "exact contiguous capture order114-121"); ++captures; report.put("actual_captures_requested", captures); capture.accept(CAPTURES.get(index)); }
    @SuppressWarnings("unchecked") private static List<GuiMessage> chat(Minecraft client) throws ReflectiveOperationException { return List.copyOf((List<GuiMessage>) BookClientSmoke.field(client.gui.getChat(), "allMessages")); }
    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException { require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside menus"); if (!wanted.test(client.player.getInventory().getItem(HAND_SLOT))) { click(client, playerSlot(client, itemSlot(client.player, wanted)), HAND_SLOT, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; } if (client.player.tickCount - handChangedAt < 6) return false; select(client, HAND_SLOT); return true; }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException { return hand(client, ItemStack::isEmpty); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static int playerSlot(Minecraft client, int inventory) { return client.player.containerMenu.slots.stream().filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static void sendRotation(Minecraft client, Vec3 point) { lookAt(client, point); client.player.connection.send(new ServerboundMovePlayerPacket.Rot(client.player.getYRot(), client.player.getXRot(), client.player.onGround())); }
    private boolean use(Minecraft client, BlockPos pos, Direction face, Vec3 point) {
        require(client.player.canInteractWithBlock(pos, 0), "actual native Survival reach " + pos);
        sendRotation(client, point);
        if (!pos.equals(useAimPos) || face != useAimFace || !point.equals(useAimPoint)) {
            useAimPos = pos.immutable(); useAimFace = face; useAimPoint = point; useAimStarted = client.player.tickCount;
            return false;
        }
        // Sending body rotation and use in ONE packet batch does not give a
        // living actor's native HEAD/view pose a tick to settle. Aim first,
        // through ordinary movement packets, before asking strict server LOS.
        if (client.player.tickCount - useAimStarted < 6) return false;
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)).consumesAction(), "native useItemOn accepted " + pos + "/" + face);
        useAimPos = null; useAimFace = null; useAimPoint = null;
        return true;
    }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 delta = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void stand(ServerPlayer player, double x, double y, double z) { var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z); require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "collision-checked camera above existing declared floor"); player.teleportTo(x, y, z); }
    private void machineCamera(ServerPlayer player) { stand(player, furnace.getX() + 2.4, workbench.getY(), furnace.getZ() + 3.1); }
    private BlockPos filterPos() { return route.getFirst(); }
    private BlockPos inspectionPos() { return route.get(inspectIndex == 0 ? 1 : 0); }
    private Vec3 filterPoint() { return Vec3.atCenterOf(filterPos()).add(0, 0, .16); }
    private static void hover(Minecraft client, int inventory) throws ReflectiveOperationException { var slot = client.player.containerMenu.getSlot(playerSlot(client, inventory)); int x = (int) BookClientSmoke.field(client.screen, "leftPos") + slot.x + 8, y = (int) BookClientSmoke.field(client.screen, "topPos") + slot.y + 8; client.screen.mouseMoved(x, y); GLFW.glfwSetCursorPos(client.getWindow().getWindow(), x * (double) client.getWindow().getScreenWidth() / client.screen.width, y * (double) client.getWindow().getScreenHeight() / client.screen.height); }
    private static void releaseKeys(Minecraft client) { client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); }
    private void releaseInputs(Minecraft client) { releaseUse(client); aimCharge = false; ClientTickEvent.CLIENT_PRE.unregister(chargeGuard); releaseKeys(client); if (client.player != null && client.gameMode != null) client.gameMode.stopDestroyBlock(); if (previousCamera != null) client.options.setCameraType(previousCamera); }
    private static BlockPos serverSessionNode(ServerPlayer player, ItemStack wand) { try { var field = ItemWandCasting.class.getDeclaredField("SERVER_NODE_USES"); field.setAccessible(true); Object session = ((Map<?, ?>) field.get(null)).get(player); require(session != null && BookClientSmoke.field(session, "hand") == InteractionHand.MAIN_HAND && BookClientSmoke.field(session, "stack") == wand, "actual SERVER-owned node session exact hand/stack identity"); return ((BlockPos) BookClientSmoke.field(session, "pos")).immutable(); } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only accepted node-session view needs reconciliation", error); } }
    private static boolean sameExceptVis(ItemStack first, ItemStack second) { ItemStack a = first.copy(), b = second.copy(); a.remove(ThaumcraftContent.WAND_DATA.get()); b.remove(ThaumcraftContent.WAND_DATA.get()); return ItemStack.matches(a, b); }
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price) { require(sameExceptVis(before, after), "all non-vis/focus/craft-cost components exact"); WandData first = ItemWandCasting.data(before), last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same actual saved payer materials"); for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0), "exact actual finite centivis debit " + tag); }
    private static CompoundTag beTag(BlockEntity tile) { return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy(); }
    private static CompoundTag stableNodeTag(BlockEntity tile) { CompoundTag tag = beTag(tile); tag.remove("Aspects"); tag.remove("lastActive"); return tag; }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect type : list.getAspects()) merge(result, type, list.getAmount(type)); return Map.copyOf(result); }
    private static Map<String, Integer> singleton(Aspect type, int amount) { var result = new LinkedHashMap<String, Integer>(); merge(result, type, amount); return Map.copyOf(result); }
    private static int amount(Frame frame, String location, String tag) { return frame.locations().getOrDefault(location, Map.of()).getOrDefault(tag, 0); }
    private static void merge(Map<String, Integer> result, Aspect type, int amount) { require(amount >= 0 && (amount == 0 || type != null), "real native storage never contains marker/negative/untyped amount"); if (amount > 0) result.merge(type.getTag(), amount, Integer::sum); }
    private static boolean gold(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("greatwood") && ItemWandCasting.data(value).cap().equals("gold"); }
    private static boolean primary(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int slot = 0; slot < 36; slot++) if (wanted.test(player.getInventory().getItem(slot))) return slot; throw new IllegalStateException("Missing actual finite tube-filter input/output"); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().mapToInt(value -> countStack(value, item)).sum(); }
    private static int countStack(ItemStack value, Item item) { return value.is(item) ? value.getCount() : 0; }
    private static int emptyMain(Player player) { for (int slot = 9; slot < 36; slot++) if (player.getInventory().getItem(slot).isEmpty()) return slot; throw new IllegalStateException("No genuinely empty main inventory slot"); }
    private static Item tubeItem() { return ThaumcraftTubes.ESSENTIA_TUBE.get().asItem(); }
    private static Item filterItem() { return ThaumcraftTubes.ESSENTIA_FILTER.get().asItem(); }
    private static Item restrictItem() { return ThaumcraftTubes.ESSENTIA_RESTRICT.get().asItem(); }
    private static Item oneWayItem() { return ThaumcraftTubes.ESSENTIA_ONEWAY.get().asItem(); }
    private static Item visFilter() { return ThaumcraftContent.MATERIALS.get("vis_filter").get(); }
    private static Item drops() { return ThaumcraftContent.MATERIALS.get("quicksilver_drop").get(); }
    private static Item labelItem() { return ThaumcraftContent.MATERIALS.get("jar_label").get(); }
    private static Item filledPhial() { return ThaumcraftContent.FILLED_PHIAL.get(); }
    private static Item silverwood() { return ThaumcraftNaturals.SILVERWOOD_PLANKS.get().asItem(); }
    private TileTube tube(Level level, int index) { return (TileTube) level.getBlockEntity(route.get(index)); }
    private TileAlchemyFurnace oven(Level level) { return (TileAlchemyFurnace) level.getBlockEntity(furnace); }
    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(furnace.above(depth)); }
    private TileJar jar(Level level) { return (TileJar) level.getBlockEntity(jarPos); }
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
    private void next(Step next) { step = next; ticks = 0; thaumcraft.common.Thaumcraft.LOG.info("Tube-filter client step={} craft={} placement={} sourceDepth={} inspect={}", step, craftIndex, placement, potentiaDepth, inspectIndex); }
}
