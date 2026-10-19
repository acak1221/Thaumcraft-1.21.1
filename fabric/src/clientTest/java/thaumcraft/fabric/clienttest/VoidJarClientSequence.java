package thaumcraft.fabric.clienttest;

import dev.architectury.event.EventResult;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
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
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
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

/** Native, finite story in root's fresh copy of the accepted STOPPED Frost save. */
public final class VoidJarClientSequence {
    private enum Step {
        HANDOFF, SETUP, BOOK, BOOK_CAPTURE, BENCH_TRAVEL, BENCH_OPEN, FUND, FUNDED,
        GRID, PREVIEW, PREVIEW_ACK, TAKE, STORE, STORED, WAND_PICK, WAND_RETURN, CRAFT_DONE,
        EMPTY_HAND, EMPTY_SCREEN, EMPTY_CAPTURE, FURNACE_TRAVEL, FURNACE_OPEN, FUEL_PICK,
        FUEL_PUT, INPUT_PICK, INPUT_PUT, BURNING, FIRST_BATCH, SOURCE_LABEL_ONE,
        SOURCE_LABEL_ONE_ACK, SOURCE_LABEL_TWO, SOURCE_LABEL_TWO_ACK, DRAIN_WAIT,
        DRAIN_ACK, ALL_RAW_CONSUMED, FILLED_HAND, FILLED_SCREEN, FILLED_CAPTURE,
        ORDINARY_PLACE, ORDINARY_ACK, VOID_PLACE, VOID_ACK, VOID_LABEL, VOID_LABEL_ACK,
        RESERVE_PHIAL, RESERVE_PHIAL_ACK, TUBE_HAND, TUBE_PLACE, TUBE_ACK,
        OVERFLOW, DRAINED, REFUSAL_ARM, REFUSAL_READY, REFUSAL_USE, REFUSAL_ACK,
        EXTRACT, EXTRACT_ACK, INSERT_HAND, INSERT_SHIFT, INSERT_USE, INSERT_ACK,
        PHIAL_SCREEN, PHIAL_CAPTURE, BREAK_AIM, BREAK, PICKUP, REPLACE_TRAVEL,
        REPLACE, REPLACE_ACK, FINAL_CHECK, FINAL_ACK, FINAL_CAPTURE, DONE
    }

    private static final List<String> CAPTURES = List.of(
            "100-void-jar-original-O-J-P-native-book-recipe-en.png",
            "101-saved-empty-jar-blaze-native-void-upgrade-gold-450aqua-1350perditio-en.png",
            "102-earned-empty-void-jar-native-item-presentation-en.png",
            "103-two-real-32-unit-alembic-drains-filled-held-void-jar-en.png",
            "104-full-labelled-64-void-jar-native-top-tube-surplus-en.png",
            "105-native-whole-eight-phial-full-refusal-and-secondary-use-success-en.png",
            "106-native-labelled-void-jar-break-pickup-replace-retained-frost-world-en.png");
    private static final Map<String, Integer> GOLD_VIS = Map.of("aer", 500, "aqua", 2300, "ignis", 500, "ordo", 500, "perditio", 2300, "terra", 2300);
    private static final Map<String, Integer> FROST_VIS = Map.of("aer", 1951, "aqua", 2436, "ignis", 2846, "ordo", 933, "perditio", 431, "terra", 464);
    private static final Map<String, Integer> UPGRADE_PRICE = Map.of("aqua", 450, "perditio", 1350);
    private static final Map<String, Integer> ORDINARY_PRICE = Map.of("aqua", 90);
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final int HAND_SLOT = 3, FUEL_INPUTS = 4;

    public final Map<String, Object> report = new LinkedHashMap<>();
    private final Minecraft owningClient;
    private final Consumer<String> capture;
    private final BlockPos workbench, center, furnace, voidPos, ordinaryPos;
    private final long startedNanos = System.nanoTime();
    private final Map<BlockPos, BlockState> oldBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> oldBEs = new LinkedHashMap<>(), oldNodes = new LinkedHashMap<>();
    private final List<ItemStack> savedStacks = new ArrayList<>(), savedArmor = new ArrayList<>();
    private final List<Map<String, Object>> supplied = new ArrayList<>(), drains = new ArrayList<>(), transfers = new ArrayList<>(), balances = new ArrayList<>(), fuelEvents = new ArrayList<>();
    private Step step = Step.HANDOFF;
    private CompletableFuture<Void> serverAction;
    private CameraType previousCamera;
    private TickEvent.Server nativeTicks;
    private EntityEvent.Add nativeAdds;
    private LifecycleEvent.ServerState serverStopped;
    private volatile Throwable observationFailure;
    private volatile Ledger ledger;
    private volatile Frame observed;
    private volatile List<BlockPos> route = List.of();
    private volatile int potentiaDepth;
    private volatile boolean networkArmed, awaitingDrop;
    private volatile UUID nativeDropId;
    private volatile ItemStack nativeDrop;
    private ResourceLocation voidRecipe;
    private ItemStack savedGold, paidGold, savedPrimary, savedJar, savedPowder, savedOffhand, refusalPhial;
    private CompoundTag expectedKnowledge, refusalJar;
    private Map<String, Integer> coalTags = Map.of(), budget = Map.of();
    private List<int[]> gridClicks = List.of();
    private int ticks, totalTicks, turns, captures, craftIndex, gridIndex, destination, drainIndex, tubeIndex, coalBlocks, initialXp;
    private int handChangedAt = Integer.MIN_VALUE / 2;
    private long refusalTick;
    private boolean savedInvulnerability, benchEmpty, firstSourcesReady, finalVerified;

    public VoidJarClientSequence(Minecraft client, Consumer<String> capture, BlockPos bench, BlockPos oldCenter) {
        owningClient = client; this.capture = capture; workbench = bench.immutable(); center = oldCenter.immutable();
        furnace = workbench.east(8); voidPos = furnace.east(2); ordinaryPos = furnace.west(2);
        report.put("scope", "voidjar: ONLY a NEW voidjar-smoke-UUID private copy of the actual accepted STOPPED Frost save; acquisition is not replayed");
        report.put("accepted_input_record", "docs/porting/frost-client.json; accepted frost-smoke-35724910-4a45-4c82-b59b-b0042f5a3734 is NEVER opened or edited");
        report.put("boundary", "Saved empty jar1 + saved blaze powder1 + actual saved greatwood/gold slot7 are explicit upgrade inputs. Saved primary Frost wand, other wand/foci, worn armor, old12jars/173units/72candles, prior machines and XP11 are retained. NEW RAW inputs only: obsidian1, panes7, oak slab1, paper4, ink sac1, slime ball1, runtime-resolved bounded coal BLOCK inputs and4 normal coal fuel. Native crafts produce void jar1, ordinary jar1 and4 blank labels; no jar/label/phial/essentia output is supplied. Empty new table/furnace/two alembics, separate stone-floor/air patch, research completion, daylight/weather and collision-checked camera are declared fixtures.");
        report.put("native_action_boundary", "Actual native inventory/result clicks, held-jar useItemOn, BlockItem tube/jar placements, blank-label use, whole-eight phials, survival destruction and physical pickup only. No storage/vis/XP/output setters, backend crafting/transport calls, manual ticks, clock freeze, RNG changes, synthetic particles or fabricated outputs. ArcaneCrafting.find/ObjectAspectResolver are READ-ONLY assertions. SERVER_POST ledger reads native storage, inventory/cursor, actual physical drops and durable pending drops; never repairs an imbalance.");
        report.put("story", "Craft upgrade450aqua/1350perditio, ordinary sink90aqua and four ordinary labels. Let actual unseeded furnace select output ordering; attach real blank labels to both genuinely full32 sources before emptying them. Two real32 held-jar drains per output fill void64 and ordinary64. Place/label void64, extract a real8 phial, then build saved-tube UP route: eight refill units followed by explicitly counted overflow. Refuse that matching filled phial at full64, extract another8 and secondary-use insert one whole8 without removing label. Retain one actual8 phial; native break/pickup/replacement preserves void64/filter.");
        report.put("coordinates", Map.of("bench", workbench.toShortString(), "old_center", center.toShortString(), "new_furnace", furnace.toShortString(), "void_jar", voidPos.toShortString(), "ordinary_sink", ordinaryPos.toShortString()));
        report.put("capture_names", CAPTURES); report.put("capture_count", CAPTURES.size());
        report.put("supplied_raw_inputs_and_fixtures", supplied); report.put("actual_held_jar_drains", drains);
        report.put("passive_native_transfers", transfers); report.put("passive_exact_balances", balances); report.put("separate_native_raw_fuel_events", fuelEvents);
        report.put("limits", Map.of("wall_seconds", 600, "native_client_ticks", 12000, "ordinary_step_ticks", 400,
                "distillation_wait_ticks", 6000, "pipe_wait_ticks", 1200, "recipe_preview_fail_fast_ticks", 40, "world_game_time_or_RNG_overrides", false));
        report.put("visual_inspection", "Pending root inspection of exact100-106 PNGs.102/103 are real empty/filled jar inventory+held-item presentation,104 waits for observed full64 native overflow,105 hovers the actually retained matching8 phial. No afterRender re-render or substitute output frame.");
        report.put("not_covered", List.of("research/material/survival acquisition replay", "ordinary packaged install or remote server", "canceled callback/stale identity/creative/malformed/three-column recipe matrices: separate native server cases", "whole legacy FX/audio parity or independently heard sound", "NeoForge"));
    }

    public boolean tick(Minecraft client) throws ReflectiveOperationException {
        try { return advance(client); }
        catch (ReflectiveOperationException | RuntimeException | Error error) {
            releaseInputs(client);
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Void jar story failed at " + step + "; craft=" + craftIndex + "; drain=" + drainIndex + "; tube=" + tubeIndex, error);
            throw error;
        }
    }

    private boolean advance(Minecraft client) throws ReflectiveOperationException {
        require(client == owningClient, "same root-owned client");
        ++ticks; ++totalTicks;
        int stepBudget = switch (step) {
            case FIRST_BATCH, DRAIN_WAIT, ALL_RAW_CONSUMED -> 6000;
            case OVERFLOW, DRAINED -> 1200;
            default -> 400;
        };
        require(ticks <= stepBudget && totalTicks <= 12000 && System.nanoTime() - startedNanos <= 600_000_000_000L,
                "bounded native void story: " + step + "; stepTicks=" + ticks + "/" + stepBudget);
        if (observationFailure != null) throw new IllegalStateException("Read-only native accounting failed", observationFailure);
        switch (step) {
            case HANDOFF -> {
                var world = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                String name = world.getFileName().toString(); require(name.startsWith("voidjar-smoke-"), "only root's NEW private voidjar world, never accepted Frost source"); UUID.fromString(name.substring("voidjar-smoke-".length()));
                require(world.getParent().getFileName().toString().equals("saves") && world.getParent().getParent().getFileName().toString().equals("run-clienttest"), "isolated client-test save only");
                report.put("actual_owned_world", world.toString()); previousCamera = client.options.getCameraType(); client.options.setCameraType(CameraType.FIRST_PERSON);
                client.setScreen(null); releaseKeys(client);
                require(client.player.containerMenu == client.player.inventoryMenu && client.player.containerMenu.getCarried().isEmpty(), "empty actual native cursor, no inherited menu");
                onServer(client, () -> prepare(serverPlayer(client))); next(Step.SETUP);
            }
            case SETUP -> {
                if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("JARVOID")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("JARVOID"))); next(Step.BOOK);
            }
            case BOOK -> { if (ticks < 8 || !showRecipe(client)) return false; next(Step.BOOK_CAPTURE); }
            case BOOK_CAPTURE -> { report.put("native_book", Map.of("research", "JARVOID", "actual_synced_recipe", voidRecipe.toString(), "vertical", List.of("obsidian", "ACTUAL saved empty ordinary jar", "ACTUAL saved blaze powder"), "base_whole_vis", Map.of("aqua", 5, "perditio", 15))); next(Step.BENCH_TRAVEL); capture(0); }
            case BENCH_TRAVEL -> { client.setScreen(null); onServer(client, () -> stand(serverPlayer(client), workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6)); next(Step.BENCH_OPEN); }
            case BENCH_OPEN -> {
                if (!serverDone() || ticks < 12) return false; select(client, 7);
                use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5), true); next(Step.FUND);
            }
            case FUND -> {
                if (ticks < 8 || !(client.screen instanceof GuiArcaneWorkbench)) return false;
                for (int i = 0; i <= 10; i++) require(arcane(client).getSlot(i).getItem().isEmpty(), "genuinely empty new native workbench");
                click(client, playerSlot(client, 7), 0, ClickType.QUICK_MOVE); next(Step.FUNDED);
            }
            case FUNDED -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getSlot(1).getItem(), savedGold)) return false;
                require(client.player.getInventory().getItem(7).isEmpty(), "actual saved gold payer, not primary Frost payer, funds table"); beginGrid(client);
            }
            case GRID -> {
                if (ticks < 2) return false;
                if (gridIndex == gridClicks.size()) { next(Step.PREVIEW); return false; }
                int[] at = gridClicks.get(gridIndex++); click(client, at[0], at[1], ClickType.PICKUP); ticks = 0;
            }
            case PREVIEW -> {
                if (ticks < 8) return false;
                if (!arcane(client).craftable() || !arcane(client).getSlot(0).getItem().is(craftOutput())) {
                    require(ticks < 40, "native recipe preview did not appear within40ticks; recipe=" + craftRecipe()
                            + "; output=" + arcane(client).getSlot(0).getItem() + "; grid="
                            + java.util.stream.IntStream.range(2, 11).mapToObj(slot -> arcane(client).getSlot(slot).getItem().toString()).toList());
                    return false;
                }
                require(arcane(client).getCarried().isEmpty() && arcane(client).getSlot(0).getItem().getCount() == (craftIndex == 2 ? 4 : 1), "finite real native result preview"); verifyGrid(arcane(client), false);
                onServer(client, () -> inspectCraft(serverPlayer(client))); next(Step.PREVIEW_ACK);
            }
            case PREVIEW_ACK -> {
                if (!serverDone()) return false;
                for (String tag : WandData.PRIMALS) require(arcane(client).requiredVis(tag) == craftPrice().getOrDefault(tag, 0), "native actor-priced FLOAT centivis: " + tag);
                next(Step.TAKE); if (craftIndex == 0) capture(1);
            }
            case TAKE -> { click(client, 0, 0, ClickType.PICKUP); next(Step.STORE); }
            case STORE -> {
                if (ticks < 6 || !arcane(client).getCarried().is(craftOutput())) return false;
                require(arcane(client).getCarried().getCount() == (craftIndex == 2 ? 4 : 1), "one native result extraction, no shift-repeat"); verifyGrid(arcane(client), true);
                destination = emptyMain(client.player); click(client, playerSlot(client, destination), 0, ClickType.PICKUP); next(Step.STORED);
            }
            case STORED -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || !client.player.getInventory().getItem(destination).is(craftOutput())) return false;
                verifyDebit(paidGold, arcane(client).getSlot(1).getItem(), craftPrice()); paidGold = arcane(client).getSlot(1).getItem().copy();
                if (++craftIndex < 3) beginGrid(client); else { click(client, 1, 0, ClickType.PICKUP); next(Step.WAND_PICK); }
            }
            case WAND_PICK -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), paidGold)) return false;
                require(client.player.getInventory().getItem(7).isEmpty(), "return payer to its actual saved slot7"); click(client, playerSlot(client, 7), 0, ClickType.PICKUP); next(Step.WAND_RETURN);
            }
            case WAND_RETURN -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || !ItemStack.matches(client.player.getInventory().getItem(7), paidGold)) return false;
                client.player.closeContainer(); onServer(client, () -> {
                    var player = serverPlayer(client); require(((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "paid result retrieval leaves table/cursor genuinely empty");
                    require(count(player, voidItem()) == 1 && count(player, ordinaryItem()) == 1 && count(player, labelItem()) == 4 && count(player, Items.BLAZE_POWDER) == 0 && count(player, Items.OBSIDIAN) == 0 && count(player, Items.GLASS_PANE) == 0 && count(player, Items.OAK_SLAB) == 0 && count(player, Items.PAPER) == 0 && count(player, Items.INK_SAC) == 0 && count(player, Items.SLIME_BALL) == 0, "ONLY saved jar/powder and declared raw recipe inputs consumed; all three outputs genuinely crafted");
                    verifyDebit(savedGold, player.getInventory().getItem(7), Map.of("aqua", 540, "perditio", 1350)); benchEmpty = true;
                    report.put("native_craft_outputs", Map.of("void_jar", snbt(stack(player, value -> value.is(voidItem())), player), "ordinary_jar", snbt(stack(player, value -> value.is(ordinaryItem())), player), "blank_labels", 4, "gold_payer_after", snbt(player.getInventory().getItem(7), player), "centivis_total", Map.of("aqua", 540, "perditio", 1350), "primary_Frost_payer_unchanged", ItemStack.matches(stack(player, VoidJarClientSequence::primary), savedPrimary), "output_grants", 0));
                }); next(Step.CRAFT_DONE);
            }
            case CRAFT_DONE -> { if (!serverDone() || ticks < 8) return false; next(Step.EMPTY_HAND); }
            case EMPTY_HAND -> {
                if (!hand(client, value -> value.is(voidItem()) && ItemJar.getAmount(value) == 0)) return false;
                require(ItemJar.getFilter(client.player.getMainHandItem()) == null, "actual earned empty/unlabelled void jar"); client.setScreen(new InventoryScreen(client.player)); next(Step.EMPTY_SCREEN);
            }
            case EMPTY_SCREEN -> { if (ticks < 8 || !client.isWindowActive()) return false; hover(client, HAND_SLOT); next(Step.EMPTY_CAPTURE); }
            case EMPTY_CAPTURE -> { if (ticks < 3) return false; requireHover(client, HAND_SLOT); next(Step.FURNACE_TRAVEL); capture(2); }
            case FURNACE_TRAVEL -> { client.setScreen(null); onServer(client, () -> machineCamera(serverPlayer(client))); next(Step.FURNACE_OPEN); }
            case FURNACE_OPEN -> {
                if (!serverDone() || ticks < 12 || !emptyHand(client)) return false;
                use(client, furnace, Direction.SOUTH, Vec3.atCenterOf(furnace).add(0, 0, .5), true); next(Step.FUEL_PICK);
            }
            case FUEL_PICK -> {
                if (ticks < 8 || !(client.screen instanceof GuiAlchemyFurnace)) return false;
                require(furnaceMenu(client).getSlot(0).getItem().isEmpty() && furnaceMenu(client).getSlot(1).getItem().isEmpty(), "both NEW furnace slots start empty");
                click(client, playerSlot(client, itemSlot(client.player, value -> value.is(Items.COAL))), 0, ClickType.PICKUP); next(Step.FUEL_PUT);
            }
            case FUEL_PUT -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL)) return false; require(furnaceMenu(client).getCarried().getCount() == FUEL_INPUTS, "finite normal coal fuel, distinct from raw coal BLOCK input"); click(client, 1, 0, ClickType.PICKUP); next(Step.INPUT_PICK); }
            case INPUT_PICK -> { if (ticks < 5 || !furnaceMenu(client).getCarried().isEmpty()) return false; click(client, playerSlot(client, itemSlot(client.player, value -> value.is(Items.COAL_BLOCK))), 0, ClickType.PICKUP); next(Step.INPUT_PUT); }
            case INPUT_PUT -> { if (ticks < 5 || !furnaceMenu(client).getCarried().is(Items.COAL_BLOCK)) return false; require(furnaceMenu(client).getCarried().getCount() == coalBlocks, "all and only runtime-resolved finite raw input blocks"); click(client, 0, 0, ClickType.PICKUP); next(Step.BURNING); }
            case BURNING -> {
                if (ticks < 8 || !furnaceMenu(client).isBurning()) return false;
                require(!furnaceMenu(client).isSpeedBoost() && furnaceMenu(client).getCurrentItemBurnTime() == 1600 && furnaceMenu(client).getBellowsCount() == 0 && furnaceMenu(client).getSmeltTime() == coalTags.values().stream().mapToInt(Integer::intValue).sum() * 10, "actual native input duration, ordinary1600 fuel, no bellows/boost/clock shortcut");
                client.player.closeContainer(); next(Step.FIRST_BATCH);
            }
            case FIRST_BATCH -> {
                if (!serverDone()) return false;
                if (!firstSourcesReady) {
                    if (ticks % 10 == 0) onServer(client, () -> identifySources(serverPlayer(client)));
                    return false;
                }
                next(Step.SOURCE_LABEL_ONE);
            }
            case SOURCE_LABEL_ONE -> { if (!hand(client, value -> value.is(labelItem()))) return false; require(ItemJarLabel.data(client.player.getMainHandItem()) == null, "actually crafted BLANK source label"); use(client, furnace.above(), Direction.SOUTH, sourcePoint(1), true); next(Step.SOURCE_LABEL_ONE_ACK); }
            case SOURCE_LABEL_ONE_ACK -> { if (ticks < 6 || alembic(client.level, 1).getFilter() != alembic(client.level, 1).getAspect()) return false; next(Step.SOURCE_LABEL_TWO); }
            case SOURCE_LABEL_TWO -> { if (!hand(client, value -> value.is(labelItem()))) return false; require(ItemJarLabel.data(client.player.getMainHandItem()) == null, "second actually crafted blank source label"); use(client, furnace.above(2), Direction.SOUTH, sourcePoint(2), true); next(Step.SOURCE_LABEL_TWO_ACK); }
            case SOURCE_LABEL_TWO_ACK -> {
                if (ticks < 6 || alembic(client.level, 2).getFilter() != alembic(client.level, 2).getAspect()) return false;
                require(count(client.player, labelItem()) == 2, "two real labels consumed, not aspect/filter fixtures"); next(Step.DRAIN_WAIT);
            }
            case DRAIN_WAIT -> {
                int depth = drainIndex % 2 == 0 ? potentiaDepth : 3 - potentiaDepth;
                Item item = drainIndex % 2 == 0 ? voidItem() : ordinaryItem(); int before = drainIndex < 2 ? 0 : 32;
                if (alembic(client.level, depth).getAmount() != 32 || !hand(client, value -> value.is(item) && ItemJar.getAmount(value) == before)) return false;
                require(alembic(client.level, depth).getFilter() == (drainIndex % 2 == 0 ? Aspect.ENERGY : Aspect.FIRE), "real native source label prevents retagging after emptying");
                use(client, furnace.above(depth), Direction.SOUTH, sourcePoint(depth), true); next(Step.DRAIN_ACK);
            }
            case DRAIN_ACK -> {
                Item item = drainIndex % 2 == 0 ? voidItem() : ordinaryItem(); int after = drainIndex < 2 ? 32 : 64;
                if (ticks < 6 || count(client.player, item) != 1 || ItemJar.getAmount(stack(client.player, value -> value.is(item))) != after) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); ItemStack actual = stack(player, value -> value.is(item)); Aspect type = drainIndex % 2 == 0 ? Aspect.ENERGY : Aspect.FIRE;
                    require(ItemJar.getAmount(actual) == after && ItemJar.getAspect(actual) == type && actual.getCount() == 1 && actual.getMaxStackSize() == 1, "genuine held-jar transaction stores EXACTLY32 more, with correct variant/components/stackability");
                    drains.add(Map.of("native_drain", drainIndex + 1, "source", furnace.above(drainIndex % 2 == 0 ? potentiaDepth : 3 - potentiaDepth).toShortString(), "aspect", type.getTag(), "actual_added", 32, "stored_after", after, "actual_item_snbt", snbt(actual, player)));
                }); next(Step.ALL_RAW_CONSUMED);
            }
            case ALL_RAW_CONSUMED -> {
                if (!serverDone()) return false;
                if (drainIndex < 3) { ++drainIndex; next(Step.DRAIN_WAIT); return false; }
                if (observed == null || observed.rawBlocks() != 0) return false;
                require(drains.size() == 4 && observed.stored().equals(budget), "all finite raw inputs REALLY distilled; no essentia lost before overflow"); next(Step.FILLED_HAND);
            }
            case FILLED_HAND -> { if (!hand(client, value -> value.is(voidItem()) && ItemJar.getAmount(value) == 64)) return false; client.setScreen(new InventoryScreen(client.player)); next(Step.FILLED_SCREEN); }
            case FILLED_SCREEN -> { if (ticks < 8 || !client.isWindowActive()) return false; hover(client, HAND_SLOT); next(Step.FILLED_CAPTURE); }
            case FILLED_CAPTURE -> { if (ticks < 3) return false; requireHover(client, HAND_SLOT); next(Step.ORDINARY_PLACE); capture(3); }
            case ORDINARY_PLACE -> { client.setScreen(null); if (!hand(client, value -> value.is(ordinaryItem()) && ItemJar.getAmount(value) == 64)) return false; use(client, ordinaryPos.below(), Direction.UP, Vec3.atCenterOf(ordinaryPos.below()).add(0, .5, 0), true); next(Step.ORDINARY_ACK); }
            case ORDINARY_ACK -> { if (ticks < 8 || !(client.level.getBlockEntity(ordinaryPos) instanceof TileJar jar) || jar.getAmount() != 64 || jar.getAspect() != Aspect.FIRE) return false; next(Step.VOID_PLACE); }
            case VOID_PLACE -> { if (!hand(client, value -> value.is(voidItem()) && ItemJar.getAmount(value) == 64)) return false; use(client, voidPos.below(), Direction.UP, Vec3.atCenterOf(voidPos.below()).add(0, .5, 0), true); next(Step.VOID_ACK); }
            case VOID_ACK -> { if (ticks < 8 || !(client.level.getBlockEntity(voidPos) instanceof TileJar jar) || !jar.isVoid() || jar.getAmount() != 64) return false; next(Step.VOID_LABEL); }
            case VOID_LABEL -> { if (!hand(client, value -> value.is(labelItem()))) return false; require(ItemJarLabel.data(client.player.getMainHandItem()) == null, "real third crafted blank label, never a supplied bound label"); use(client, voidPos, Direction.SOUTH, jarPoint(), true); next(Step.VOID_LABEL_ACK); }
            case VOID_LABEL_ACK -> {
                if (ticks < 6 || jar(client.level).getFilter() != Aspect.ENERGY || count(client.player, labelItem()) != 1) return false;
                require(jar(client.level).getFacing() == Direction.SOUTH && jar(client.level).getAmount() == 64, "label binds ACTUAL contents without creating/removing essentia"); next(Step.RESERVE_PHIAL);
            }
            case RESERVE_PHIAL -> { if (!hand(client, value -> value.is(emptyPhial()))) return false; use(client, voidPos, Direction.SOUTH, jarPoint(), true); next(Step.RESERVE_PHIAL_ACK); }
            case RESERVE_PHIAL_ACK -> {
                if (ticks < 6 || jar(client.level).getAmount() != 56 || count(client.player, emptyPhial()) != 3 || count(client.player, filledPhial()) != 1) return false;
                require(phialUnits(client.player) == 8 && jar(client.level).getFilter() == Aspect.ENERGY, "one whole8 extracted from actual full void jar before any pipe exists"); next(Step.TUBE_HAND);
            }
            case TUBE_HAND -> { if (!hand(client, value -> value.is(tubeItem()))) return false; next(Step.TUBE_PLACE); }
            case TUBE_PLACE -> {
                if (ticks < 5) return false;
                if (tubeIndex == route.size() - 1) networkArmed = true;
                BlockPos anchor = tubeIndex == 0 ? furnace.above(potentiaDepth) : route.get(tubeIndex - 1); Direction face = tubeIndex == 2 ? Direction.DOWN : Direction.EAST;
                use(client, anchor, face, Vec3.atCenterOf(anchor).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5), true); next(Step.TUBE_ACK);
            }
            case TUBE_ACK -> {
                if (ticks < 6 || !(client.level.getBlockEntity(route.get(tubeIndex)) instanceof TileTube)) return false;
                require(count(client.player, tubeItem()) == 6 - tubeIndex - 1, "one actual saved tube consumed per native placement");
                if (++tubeIndex < route.size()) next(Step.TUBE_PLACE); else next(Step.OVERFLOW);
            }
            case OVERFLOW -> {
                Frame frame = observed;
                if (frame == null || frame.voided().getOrDefault("potentia", 0) < 1 || jar(client.level).getAmount() != 64) return false;
                lookAt(client, Vec3.atCenterOf(furnace).add(.8, .65, 0));
                report.put("native_full_overflow_capture", Map.of("native_tick", frame.tick(), "visible_and_server_amount", 64, "actually_voided_so_far", frame.voided(), "stored_now", frame.stored(), "source_and_pipe_buffers", frame.locations(), "actual_filter", "potentia", "synthetic_FX_calls", 0)); next(Step.DRAINED); capture(4);
            }
            case DRAINED -> {
                if (observed == null || !observed.potentiaDrained()) return false;
                require(observed.rawBlocks() == 0 && observed.voided().getOrDefault("potentia", 0) == budget.get("potentia") - 72 && jar(client.level).getAmount() == 64, "finite native source completely drains; stored64+real8phial+explicitoverflow exactly equals supplied potentia"); next(Step.REFUSAL_ARM);
            }
            case REFUSAL_ARM -> {
                if (!hand(client, value -> value.is(filledPhial()))) return false;
                onServer(client, () -> { var player = serverPlayer(client); refusalPhial = player.getMainHandItem().copy(); refusalJar = beTag(jar(player.level())); refusalTick = player.level().getGameTime(); require(phialAmount(refusalPhial) == 8 && jar(player.level()).getAmount() == 64, "actual matching whole8 and actual full64 before refused native dispatch"); }); next(Step.REFUSAL_READY);
            }
            case REFUSAL_READY -> { if (!serverDone()) return false; next(Step.REFUSAL_USE); }
            case REFUSAL_USE -> { report.put("full_phial_client_result", use(client, voidPos, Direction.SOUTH, jarPoint(), false)); next(Step.REFUSAL_ACK); }
            case REFUSAL_ACK -> {
                if (ticks < 8 || !serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); require(ItemStack.matches(player.getMainHandItem(), refusalPhial) && beTag(jar(player.level())).equals(refusalJar) && count(player, emptyPhial()) == 3 && count(player, filledPhial()) == 1, "full void refuses WHOLE matching phial: no debit, overflow, label removal or output duplication");
                    report.put("native_full_phial_refusal", Map.of("native_before_tick", refusalTick, "native_after_tick", player.level().getGameTime(), "actual_phial_unchanged", snbt(refusalPhial, player), "actual_full_jar_unchanged", refusalJar.toString(), "filled_phials", 1, "empty_phials", 3));
                }); next(Step.EXTRACT);
            }
            case EXTRACT -> { if (!serverDone() || !hand(client, value -> value.is(emptyPhial()))) return false; use(client, voidPos, Direction.SOUTH, jarPoint(), true); next(Step.EXTRACT_ACK); }
            case EXTRACT_ACK -> { if (ticks < 6 || jar(client.level).getAmount() != 56 || count(client.player, emptyPhial()) != 2 || count(client.player, filledPhial()) != 2) return false; require(phialUnits(client.player) == 16, "second genuine whole8 extraction, no residual source or tube refill"); next(Step.INSERT_HAND); }
            case INSERT_HAND -> { if (!hand(client, value -> value.is(filledPhial()))) return false; client.options.keyShift.setDown(true); next(Step.INSERT_SHIFT); }
            case INSERT_SHIFT -> { if (ticks < 6 || !client.player.isShiftKeyDown()) return false; next(Step.INSERT_USE); }
            case INSERT_USE -> { use(client, voidPos, Direction.SOUTH, jarPoint(), true); client.options.keyShift.setDown(false); next(Step.INSERT_ACK); }
            case INSERT_ACK -> {
                if (ticks < 8 || client.player.isShiftKeyDown() || jar(client.level).getAmount() != 64 || count(client.player, emptyPhial()) != 3 || count(client.player, filledPhial()) != 1) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); require(jar(player.level()).getAmount() == 64 && jar(player.level()).getFilter() == Aspect.ENERGY && phialUnits(player) == 8 && count(player, labelItem()) == 1 && !player.isShiftKeyDown(), "real secondary-use phial succeeds by WHOLE8 ahead of label removal; released Shift and actual label remain");
                    report.put("native_whole_eight_success", Map.of("second_extraction", 8, "secondary_use_insertion", 8, "amount_before_insertion", 56, "amount_after_insertion", 64, "label_retained", true, "remaining_actual_filled_phial_units", 8, "remaining_empty_phials", 3));
                }); next(Step.PHIAL_SCREEN);
            }
            case PHIAL_SCREEN -> { if (!serverDone() || ticks < 6 || !hand(client, value -> value.is(filledPhial()))) return false; client.setScreen(new InventoryScreen(client.player)); hover(client, HAND_SLOT); next(Step.PHIAL_CAPTURE); }
            case PHIAL_CAPTURE -> { if (ticks < 8 || !client.isWindowActive()) return false; hover(client, HAND_SLOT); requireHover(client, HAND_SLOT); next(Step.BREAK_AIM); capture(5); }
            case BREAK_AIM -> {
                client.setScreen(null); if (!emptyHand(client) || ticks < 8) return false;
                lookAt(client, jarPoint()); require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(voidPos, 0), "actual survival jar destruction in native reach");
                awaitingDrop = true; require(client.gameMode.startDestroyBlock(voidPos, Direction.SOUTH), "native survival starts breaking earned full labelled void jar"); next(Step.BREAK);
            }
            case BREAK -> {
                lookAt(client, jarPoint()); if (!client.level.isEmptyBlock(voidPos)) { client.gameMode.continueDestroyBlock(voidPos, Direction.SOUTH); return false; }
                client.gameMode.stopDestroyBlock(); next(Step.PICKUP);
            }
            case PICKUP -> {
                if (!serverDone()) return false;
                if (nativeDrop == null || count(client.player, voidItem()) != 1) { if (ticks % 8 == 0) onServer(client, () -> approachDrop(serverPlayer(client))); return false; }
                require(ItemStack.matches(stack(client.player, value -> value.is(voidItem())), nativeDrop), "client physically picked up SAME native filled/labelled variant/components");
                onServer(client, () -> { var player = serverPlayer(client); require(ItemStack.matches(stack(player, value -> value.is(voidItem())), nativeDrop) && player.serverLevel().getEntity(nativeDropId) == null, "native physical pickup owns exact original drop; entity genuinely gone"); report.put("native_survival_drop_pickup", Map.of("UUID", nativeDropId.toString(), "actual_drop_snbt", snbt(nativeDrop, player), "amount", 64, "filter", "potentia", "max_stack", nativeDrop.getMaxStackSize(), "item_entity_relocation_or_output_setters", 0)); machineCamera(player); }); next(Step.REPLACE_TRAVEL);
            }
            case REPLACE_TRAVEL -> { if (!serverDone() || ticks < 12) return false; next(Step.REPLACE); }
            case REPLACE -> { if (!hand(client, value -> value.is(voidItem()))) return false; require(ItemStack.matches(client.player.getMainHandItem(), nativeDrop), "place actual physically picked-up output, never a reconstructed item"); use(client, voidPos.below(), Direction.UP, Vec3.atCenterOf(voidPos.below()).add(0, .5, 0), true); next(Step.REPLACE_ACK); }
            case REPLACE_ACK -> { if (ticks < 10 || !(client.level.getBlockEntity(voidPos) instanceof TileJar jar) || !jar.isVoid() || jar.getAmount() != 64 || jar.getFilter() != Aspect.ENERGY || count(client.player, voidItem()) != 0) return false; next(Step.FINAL_CHECK); }
            case FINAL_CHECK -> { onServer(client, () -> { finalState(serverPlayer(client)); stopObservation(); }); next(Step.FINAL_ACK); }
            case FINAL_ACK -> {
                if (!serverDone() || ticks < 8) return false;
                require(finalVerified && nativeTicks == null && nativeAdds == null && serverStopped == null && captures == 6, "all passive hooks removed, exactly six preceding captures");
                if (!emptyHand(client)) return false; lookAt(client, Vec3.atCenterOf(furnace).add(.75, .65, 0)); next(Step.FINAL_CAPTURE);
            }
            case FINAL_CAPTURE -> { if (ticks < 8) return false; report.put("native_client_ticks", totalTicks); report.put("elapsed_wall_seconds", (System.nanoTime() - startedNanos) / 1_000_000_000D); next(Step.DONE); capture(6); }
            case DONE -> { require(captures == CAPTURES.size(), "exact seven captures100-106"); client.setScreen(null); releaseInputs(client); return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        require(center.equals(new BlockPos(1474, 216, 377)) && workbench.equals(new BlockPos(1714, 216, 377)), "root passes ONLY new bench1714,216,377 and old center1474,216,377");
        var level = player.serverLevel(); require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && player.isAlive(), "actual accepted Survival mode, not a creative setup conversion");
        initialXp = player.totalExperience; savedInvulnerability = player.isInvulnerable(); require(initialXp == 11 && savedInvulnerability, "actual saved XP11 and Entity Invulnerable1 unchanged");
        savedGold = player.getInventory().getItem(7).copy(); paidGold = savedGold.copy(); savedPrimary = stack(player, VoidJarClientSequence::primary).copy();
        savedJar = stack(player, value -> value.is(ordinaryItem())).copy(); savedPowder = stack(player, value -> value.is(Items.BLAZE_POWDER)).copy(); savedOffhand = player.getOffhandItem().copy();
        require(gold(savedGold) && ItemWandCasting.data(savedGold).vis().equals(GOLD_VIS) && FocusData.get(savedGold).isEmpty(), "EXACT actual accepted greatwood/gold slot7, no charge grant");
        require(primary(savedPrimary) && ItemWandCasting.data(savedPrimary).vis().equals(FROST_VIS) && FocusData.get(savedPrimary).is(ThaumcraftFoci.FROST_FOCUS.get()), "actual accepted main Frost payer is EXACT, never used or topped up");
        require(savedJar.getCount() == 1 && ItemJar.isEmptyIngredient(savedJar) && savedPowder.getCount() == 1 && count(player, ordinaryItem()) == 1 && count(player, voidItem()) == 0, "consume ONLY actual saved genuinely empty jar1/powder1; no saved void output");
        require(count(player, emptyPhial()) == 4 && count(player, filledPhial()) == 0 && count(player, tubeItem()) == 6 && count(player, ThaumcraftContent.WAND.get()) == 3 && savedOffhand.is(Items.APPLE) && savedOffhand.getCount() == 1, "actual four phials/six tubes/three wands/offhand apple");
        require(count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.SHOCK_FOCUS.get()) == 1 && count(player, ThaumcraftFoci.EXCAVATION_FOCUS.get()) == 1 && count(player, Items.DIAMOND) == 0, "accepted Frost installed, exact old Fire/Shock/returned Excavation retained; old earned diamond already consumed, not replayed");
        List<Item> armor = List.of(ThaumcraftWearables.GOGGLES.get(), ThaumcraftWearables.ROBE_CHESTPLATE.get(), ThaumcraftWearables.ROBE_LEGGINGS.get(), ThaumcraftWearables.ROBE_BOOTS.get());
        for (int i = 0; i < ARMOR.size(); i++) { ItemStack actual = player.getItemBySlot(ARMOR.get(i)); require(actual.is(armor.get(i)) && actual.getDamageValue() == (i >= 2 ? 1 : 0), "actual worn saved armor0/0/1/1"); savedArmor.add(actual.copy()); }
        require(savedArmor.get(1).getOrDefault(DataComponents.REPAIR_COST, 0) == 1 && WandVisDiscount.percentagePoints(player, Aspect.WATER) == 10 && WandVisDiscount.modifier(savedGold, player, Aspect.WATER) == .9F && (int) (500 * WandVisDiscount.modifier(savedGold, player, Aspect.WATER)) == 450 && (int) (1500 * WandVisDiscount.modifier(savedGold, player, Aspect.ENTROPY)) == 1350, "native actual FLOAT gold/full-gear upgrade450/1350, without charge or gear changes");
        for (ItemStack value : player.getInventory().items) if (!value.isEmpty()) savedStacks.add(value.copy());
        scanOld(level); patch(level);
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState()); level.setBlockAndUpdate(furnace, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
        for (int depth = 1; depth <= 2; depth++) level.setBlockAndUpdate(furnace.above(depth), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.SOUTH));
        require(oven(level).isEmpty() && oven(level).getVis() == 0 && alembic(level, 1).getAmount() == 0 && alembic(level, 2).getAmount() == 0 && alembic(level, 1).getFilter() == null && alembic(level, 2).getFilter() == null, "only empty new devices, NEVER seeded outputs or filters");
        var recipes = level.getRecipeManager().getRecipes().stream().filter(holder -> holder.value().getResultItem(level.registryAccess()).is(voidItem())).toList(); require(recipes.size() == 1, "one actual registered native void recipe; do not guess root's resource alias"); voidRecipe = recipes.getFirst().id();
        var knowledge = KnowledgeAccess.get(player); CompoundTag original = knowledge.save().copy(); var keys = new LinkedHashSet<String>(); keys.addAll(ResearchCatalog.get("JARVOID").parents()); keys.addAll(ResearchCatalog.get("JARVOID").parentsHidden()); keys.add("JARVOID");
        var already = new LinkedHashMap<String, Boolean>(); for (String key : keys) { already.put(key, knowledge.isResearchComplete(key)); knowledge.addResearch(key); } expectedKnowledge = knowledge.save().copy(); KnowledgeEvents.sync(player);
        report.put("research_fixture", Map.of("keys", List.copyOf(keys), "already_complete", already, "earned_here", false, "before_snbt", original.toString(), "after_snbt", expectedKnowledge.toString()));
        coalTags = aspects(ObjectAspectResolver.get(new ItemStack(Items.COAL_BLOCK), level));
        require(coalTags.size() == 2 && coalTags.getOrDefault("potentia", 0) > 0 && coalTags.getOrDefault("ignis", 0).equals(coalTags.get("potentia")) && coalTags.values().stream().mapToInt(Integer::intValue).sum() <= 50, "actual native coal-block tags must be exactly two equal positive distillable types, no guessed high-aspect fuel");
        coalBlocks = (96 + coalTags.get("potentia") - 1) / coalTags.get("potentia"); require(coalBlocks >= 1 && coalBlocks <= 12, "bounded actual native input count");
        var resolvedBudget = new LinkedHashMap<String, Integer>(); coalTags.forEach((tag, amount) -> resolvedBudget.put(tag, amount * coalBlocks)); budget = Map.copyOf(resolvedBudget);
        require(budget.get("potentia") >= 96 && budget.get("potentia") <= 112, "bounded native budget permits64 jar +8 real phial + explicitly observed surplus; other type fits64+32+furnace");
        for (Item raw : List.of(Items.OBSIDIAN, Items.GLASS_PANE, Items.OAK_SLAB, Items.PAPER, Items.INK_SAC, Items.SLIME_BALL, Items.COAL_BLOCK, Items.COAL, labelItem())) require(count(player, raw) == 0, "new input never confused with existing accepted stacks: " + raw);
        supply(player, Items.OBSIDIAN, 1); supply(player, Items.GLASS_PANE, 7); supply(player, Items.OAK_SLAB, 1); supply(player, Items.PAPER, 4); supply(player, Items.INK_SAC, 1); supply(player, Items.SLIME_BALL, 1); supply(player, Items.COAL_BLOCK, coalBlocks); supply(player, Items.COAL, FUEL_INPUTS);
        verifyPlannedRecipes(player);
        long oldDay = level.getDayTime(); level.setDayTime(6000); level.setWeatherParameters(0, 12000, false, false); machineCamera(player); player.inventoryMenu.broadcastChanges();
        supplied.add(Map.of("fixture", "empty table/furnace/two alembics, bounded separate stone floor/air patch, research, camera and daylight/clear weather", "previous_day_time", oldDay, "daylight", 6000, "world_game_time_writes", 0, "new_patch_min", patchMin().toShortString(), "new_patch_max", patchMax().toShortString()));
        report.put("native_raw_aspect_resolution", Map.of("actual_coal_BLOCK_tags_per_one", coalTags, "actual_input_blocks", coalBlocks, "finite_supplied_essentia_potential", budget, "fuel_is_separate_normal_coal", FUEL_INPUTS, "unprocessed_input_potential_is_accounted_until_native_smelt", true));
        var saved = new LinkedHashMap<String, Object>(); saved.put("main_stacks_snbt", savedStacks.stream().map(value -> snbt(value, player)).toList()); saved.put("armor_snbt", savedArmor.stream().map(value -> snbt(value, player)).toList()); saved.put("offhand_snbt", snbt(savedOffhand, player)); saved.put("actual_saved_gold_input", snbt(savedGold, player)); saved.put("primary_Frost_untouched", snbt(savedPrimary, player)); saved.put("old_non_node_BE_count", oldBEs.size()); saved.put("old_block_count", oldBlocks.size()); saved.put("total_XP", initialXp); report.put("actual_saved_inputs", saved);
        ledger = new Ledger(player); startObservation(player);
    }

    private void beginGrid(Minecraft client) { gridClicks = recipeClicks(client); gridIndex = 0; next(Step.GRID); }
    private Item craftOutput() { return craftIndex == 0 ? voidItem() : craftIndex == 1 ? ordinaryItem() : labelItem(); }
    private ResourceLocation craftRecipe() { return craftIndex == 0 ? voidRecipe : id(craftIndex == 1 ? "arcane/warded_jar" : "jar_label"); }
    private Map<String, Integer> craftPrice() { return craftIndex == 0 ? UPGRADE_PRICE : craftIndex == 1 ? ORDINARY_PRICE : Map.of(); }
    private Item ingredient(int cell) { return ingredient(cell, craftIndex); }
    private Item ingredient(int cell, int craft) {
        if (craft == 0) return switch (cell) { case 1 -> Items.OBSIDIAN; case 4 -> ordinaryItem(); case 7 -> Items.BLAZE_POWDER; default -> Items.AIR; };
        if (craft == 1) return cell == 4 ? Items.AIR : cell == 1 ? Items.OAK_SLAB : Items.GLASS_PANE;
        return cell == 0 ? Items.INK_SAC : cell == 1 ? Items.SLIME_BALL : cell <= 5 ? Items.PAPER : Items.AIR;
    }
    /** Read-only matching of detached copies of the ACTUAL raw stacks. No menu,
     * resource payment, output delivery, or world/storage mutation is performed. */
    private void verifyPlannedRecipes(ServerPlayer player) {
        var checks = new ArrayList<Map<String, Object>>();
        for (int craft = 0; craft < 3; craft++) {
            var projection = new SimpleContainer(11);
            for (int cell = 0; cell < 9; cell++) {
                Item wanted = ingredient(cell, craft);
                if (wanted != Items.AIR) projection.setItem(cell, stack(player, value -> value.is(wanted)).copyWithCount(1));
            }
            projection.setItem(TileArcaneWorkbench.WAND_SLOT, savedGold.copy());
            var match = ArcaneCrafting.find(player, projection);
            ResourceLocation expected = craft == 0 ? voidRecipe : id(craft == 1 ? "arcane/warded_jar" : "jar_label");
            Item output = craft == 0 ? voidItem() : craft == 1 ? ordinaryItem() : labelItem();
            require(match != null && match.recipe().id().equals(expected) && match.output().is(output)
                    && match.output().getCount() == (craft == 2 ? 4 : 1),
                    "read-only recipe preflight refuses missing/incompatible actual inputs BEFORE native clicks: " + expected);
            checks.add(Map.of("actual_recipe", expected.toString(), "actual_input_copies",
                    match.input().items().stream().map(ItemStack::toString).toList(), "native_output_count", match.output().getCount(), "read_only", true));
        }
        report.put("read_only_actual_recipe_preflight", checks);
    }
    private List<int[]> recipeClicks(Minecraft client) {
        var clicks = new ArrayList<int[]>(); var ingredients = new LinkedHashSet<Item>(); for (int cell = 0; cell < 9; cell++) if (ingredient(cell) != Items.AIR) ingredients.add(ingredient(cell));
        for (Item item : ingredients) {
            int slot = playerSlot(client, itemSlot(client.player, value -> value.is(item))), needed = 0; clicks.add(new int[] {slot, 0});
            for (int cell = 0; cell < 9; cell++) if (ingredient(cell) == item) { clicks.add(new int[] {cell + 2, 1}); ++needed; }
            require(client.player.containerMenu.getSlot(slot).getItem().getCount() == needed, "exact finite recipe stack, no unexplained remainder: " + item);
        }
        return clicks;
    }
    private void verifyGrid(ContainerArcaneWorkbench menu, boolean empty) { for (int cell = 0; cell < 9; cell++) { ItemStack actual = menu.getSlot(cell + 2).getItem(); Item wanted = ingredient(cell); require(empty || wanted == Items.AIR ? actual.isEmpty() : actual.is(wanted) && actual.getCount() == 1, "native recipe grid/actual consumption: " + cell); } }
    private void inspectCraft(ServerPlayer player) {
        var table = (TileArcaneWorkbench) player.level().getBlockEntity(workbench); var match = ArcaneCrafting.find(player, table);
        require(match != null && match.recipe().id().equals(craftRecipe()) && match.arcane() == (craftIndex < 2) && match.prices().equals(craftPrice()) && match.output().is(craftOutput()), "actual native recipe/result and actor-priced vis");
        if (craftIndex == 0) require(ItemStack.matches(table.getItem(4), savedJar) && ItemStack.matches(table.getItem(7), savedPowder) && ItemStack.matches(table.getItem(10), savedGold), "ONLY exact saved jar/powder/gold payer are used in original O/J/P upgrade");
        report.put("native_preview_" + craftIndex, Map.of("actual_recipe", match.recipe().id().toString(), "centivis", match.prices(), "actual_output", snbt(match.output(), player), "payer_before", snbt(table.getItem(10), player), "direct_craft_calls", 0));
    }

    private void identifySources(ServerPlayer player) {
        if (alembic(player.level(), 1).getAmount() != 32 || alembic(player.level(), 2).getAmount() != 32) return;
        var low = alembic(player.level(), 1); var high = alembic(player.level(), 2);
        require(low.getAspect() != high.getAspect() && (low.getAspect() == Aspect.ENERGY || high.getAspect() == Aspect.ENERGY) && (low.getAspect() == Aspect.FIRE || high.getAspect() == Aspect.FIRE), "actual unseeded native RNG selected two different input aspects");
        potentiaDepth = low.getAspect() == Aspect.ENERGY ? 1 : 2;
        var path = new ArrayList<BlockPos>(); path.add(furnace.above(potentiaDepth).east()); path.add(furnace.above(potentiaDepth).east(2)); if (potentiaDepth == 2) path.add(voidPos.above()); route = List.copyOf(path);
        report.put("actual_native_source_order", Map.of("lower", low.getAspect().getTag(), "upper", high.getAspect().getTag(), "potentia_depth", potentiaDepth, "native_route", route.stream().map(BlockPos::toShortString).toList(), "saved_tubes_to_use", route.size(), "RNG_or_type_seed_writes", 0, "labels", "two genuinely full32 sources receive actually crafted BLANK labels before any held drain")); firstSourcesReady = true;
    }

    /** A detached read-only snapshot. Binding/filter is never counted as stored essentia. */
    private record Frame(long tick, int rawBlocks, int fuelLeft, Map<String, Integer> stored, Map<String, Integer> voided,
                         Map<String, Map<String, Integer>> locations, boolean potentiaDrained) {}

    /**
     * Passive native accounting, including simultaneous transfers in a server tick.
     * Furnace out = prior buffer + actually consumed raw tags - current buffer.
     * Source out = prior source + that furnace out - current source. Each tube's
     * out follows identically along the single unbranched source->jar route, so a
     * same-tick pass-through cannot hide a unit in a net buffer delta. Global loss
     * is accepted ONLY for that last native tube->already-full void transfer.
     */
    private final class Ledger {
        final ServerPlayer player;
        final long startedTick;
        Frame previous;
        final Map<String, Integer> voided = new LinkedHashMap<>();
        int checks, heldDrains, pipeStored, pipeVoided, fuelSpent;
        long lastJarPull = Long.MIN_VALUE / 2;
        Ledger(ServerPlayer player) { this.player = player; startedTick = player.level().getGameTime(); previous = snapshot(); verifyBalance(previous); observed = previous; }
        void observe() {
            require(player.isAlive() && player.serverLevel() == oven(player.level()).getLevel() && player.level().getGameTime() - startedTick <= 12000 && System.nanoTime() - startedNanos <= 600_000_000_000L, "bounded live owned server/player/new machines, including while capture I/O suspends the client story");
            Frame now = snapshot(); int smelted = previous.rawBlocks() - now.rawBlocks(); require(smelted == 0 || smelted == 1, "one actual raw input consumption per native tick");
            int spent = previous.fuelLeft() - now.fuelLeft(); require(spent == 0 || spent == 1, "native fuel debits at most one real coal per tick");
            if (spent == 1) { ++fuelSpent; require(oven(player.level()).getCurrentItemBurnTime() == 1600 && !oven(player.level()).isSpeedBoost(), "real normal-coal fuel event, no essentia production"); fuelEvents.add(Map.of("native_tick", now.tick(), "raw_normal_coal_consumed", 1, "actual_native_duration", 1600, "essentia_produced_from_fuel", 0)); }
            var furnaceOut = new LinkedHashMap<String, Integer>();
            int lostPotentia = 0;
            for (String tag : budget.keySet()) {
                int lost = previous.stored().getOrDefault(tag, 0) + smelted * coalTags.get(tag) - now.stored().getOrDefault(tag, 0);
                require(lost >= 0 && lost <= 1 && (lost == 0 || tag.equals("potentia") && networkArmed && amount(previous, "void:world", tag) == 64 && amount(now, "void:world", tag) == 64), "no invented/lost essentia; ONLY one already-full native void pull may destroy one unit: " + tag + "/" + lost);
                if (lost > 0) { voided.merge(tag, lost, Integer::sum); lostPotentia += lost; }
                int out = amount(previous, "furnace", tag) + smelted * coalTags.get(tag) - amount(now, "furnace", tag);
                require(out >= 0 && out <= 1, "actual furnace emits at most one of each type per native transfer tick"); furnaceOut.put(tag, out);
                if (smelted == 1) transfer(now, "actual_raw_coal_block", "furnace", tag, coalTags.get(tag), coalTags.get(tag), 0);
            }
            int sourceToPipe = 0;
            for (int depth = 1; depth <= 2; depth++) {
                String location = "alembic:" + depth;
                for (String tag : budget.keySet()) {
                    TileAlembic source = alembic(player.level(), depth);
                    String sourceTag = source.getAspect() != null ? source.getAspect().getTag() : source.getFilter() != null ? source.getFilter().getTag() : previous.locations().getOrDefault(location, Map.of()).keySet().stream().findFirst().orElse("");
                    int incoming = tag.equals(sourceTag) ? furnaceOut.getOrDefault(tag, 0) : 0;
                    int outgoing = amount(previous, location, tag) + incoming - amount(now, location, tag);
                    require(outgoing == 0 || outgoing == 1 && networkArmed && depth == potentiaDepth && tag.equals("potentia") || outgoing == 32 && !networkArmed, "native alembic only emits a real held32 drain or one route unit, never an unexplained delta");
                    if (incoming > 0) transfer(now, "furnace", location, tag, incoming, incoming, 0);
                    if (outgoing == 32) {
                        String itemLocation = tag.equals("potentia") ? "void:items" : "ordinary:items";
                        require(amount(now, itemLocation, tag) - amount(previous, itemLocation, tag) == 32, "actual source32 exactly equals held-item32 increase"); ++heldDrains; transfer(now, location, itemLocation, tag, 32, 32, 0);
                    } else if (outgoing == 1) sourceToPipe += outgoing;
                }
            }
            int receivedByJar = 0;
            if (networkArmed) {
                int incoming = sourceToPipe;
                for (int index = 0; index < route.size(); index++) {
                    String location = "tube:" + index; int outgoing = amount(previous, location, "potentia") + incoming - amount(now, location, "potentia");
                    require(outgoing >= 0 && outgoing <= 1, "native one-unit tube flow along actual unbranched route");
                    if (incoming > 0) transfer(now, index == 0 ? "alembic:" + potentiaDepth : "tube:" + (index - 1), location, "potentia", incoming, incoming, 0);
                    incoming = outgoing;
                }
                require(lostPotentia <= incoming, "every actually voided unit has a real last-tube debit, never just unexplained global loss");
                if (incoming > 0) { int stored = incoming - lostPotentia; require(now.tick() - lastJarPull >= 5 && amount(now, "void:world", "potentia") - amount(previous, "void:world", "potentia") == stored, "one native local5tick last-tube debit = jar increment + explicit overflow"); lastJarPull = now.tick(); receivedByJar = incoming; pipeStored += stored; pipeVoided += lostPotentia; transfer(now, "tube:" + (route.size() - 1), "void:world", "potentia", incoming, stored, lostPotentia); }
            } else require(lostPotentia == 0, "no overflow before actual native route exists");
            int phialDelta = amount(now, "phials", "potentia") - amount(previous, "phials", "potentia");
            if (phialDelta != 0) { require(Math.abs(phialDelta) == 8 && receivedByJar == 0 && amount(now, "void:world", "potentia") - amount(previous, "void:world", "potentia") == -phialDelta, "every actual phial change is WHOLE8, exactly paired to the real jar with no concurrent pipe refill"); transfer(now, phialDelta > 0 ? "void:world" : "phials", phialDelta > 0 ? "phials" : "void:world", "potentia", 8, 8, 0); }
            for (String kind : List.of("void", "ordinary")) {
                String tag = kind.equals("void") ? "potentia" : "ignis";
                int worldDelta = amount(now, kind + ":world", tag) - amount(previous, kind + ":world", tag);
                if (Math.abs(worldDelta) == 64) {
                    require(amount(now, kind + ":items", tag) - amount(previous, kind + ":items", tag) == -worldDelta, "native whole filled-jar item/world ownership transition preserves all64 units");
                    transfer(now, kind + (worldDelta > 0 ? ":items" : ":world"), kind + (worldDelta > 0 ? ":world" : ":items"), tag, 64, 64, 0);
                }
            }
            now = new Frame(now.tick(), now.rawBlocks(), now.fuelLeft(), now.stored(), Map.copyOf(voided), now.locations(), now.potentiaDrained()); verifyBalance(now);
            if (smelted > 0 || !now.locations().equals(previous.locations()) || !now.voided().equals(previous.voided()) || spent > 0) {
                var row = new LinkedHashMap<String, Object>(); row.put("native_tick", now.tick()); row.put("unprocessed_raw_blocks", now.rawBlocks()); row.put("supplied_input_potential", budget); row.put("stored", now.stored()); row.put("explicitly_voided", now.voided()); row.put("locations", now.locations()); row.put("real_fuel_left", now.fuelLeft()); row.put("native_raw_blocks_consumed_this_tick", smelted); balances.add(row);
            }
            ++checks; previous = now; observed = now;
        }
        Frame snapshot() {
            var level = player.serverLevel(); var locations = new LinkedHashMap<String, Map<String, Integer>>(); locations.put("furnace", aspects(oven(level).getAspects()));
            for (int depth = 1; depth <= 2; depth++) locations.put("alembic:" + depth, singleton(alembic(level, depth).getAspect(), alembic(level, depth).getAmount()));
            for (int index = 0; index < route.size(); index++) {
                BlockEntity tile = level.getBlockEntity(route.get(index));
                if (tile instanceof TileTube tube) { require(tube.getStoredEssentiaAmount() >= 0 && tube.getStoredEssentiaAmount() <= 1 && tube.getVenting() == 0 && (tube.getStoredEssentiaAmount() == 0 || tube.getStoredEssentiaType() == Aspect.ENERGY), "real capacity1 unvented route never carries the other output"); locations.put("tube:" + index, singleton(tube.getStoredEssentiaType(), tube.getStoredEssentiaAmount())); }
            }
            if (level.getBlockEntity(voidPos) instanceof TileJar jar) { require(jar.isVoid(), "actual registered void variant, not a saved arbitrary flag"); locations.put("void:world", singleton(jar.getAspect(), jar.getAmount())); }
            if (level.getBlockEntity(ordinaryPos) instanceof TileJar jar) { require(!jar.isVoid(), "actual native ordinary sink remains ordinary"); locations.put("ordinary:world", singleton(jar.getAspect(), jar.getAmount())); }
            var voidItems = new LinkedHashMap<String, Integer>(); var ordinaryItems = new LinkedHashMap<String, Integer>(); var phials = new LinkedHashMap<String, Integer>();
            var physical = new ArrayList<ItemStack>(); physical.addAll(player.getInventory().items); physical.add(player.containerMenu.getCarried());
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, patchBounds())) physical.add(entity.getItem());
            // Read the real durable queue without enqueue/flush/tick or any live
            // setter. A native deferred drop is storage too, not a lost unit.
            for (ItemStack pending : pendingStacks(level)) physical.add(pending);
            for (ItemStack value : physical) {
                if (ItemJar.isJar(value)) merge(ItemJar.isVoid(value) ? voidItems : ordinaryItems, ItemJar.getAspect(value), ItemJar.getAmount(value) * value.getCount());
                if (value.is(filledPhial())) { var data = ItemEssence.data(value); require(data != null && data.tag().equals("potentia"), "only genuine matching whole8 phials from this new story"); merge(phials, Aspect.ENERGY, PhialData.AMOUNT * value.getCount()); }
            }
            locations.put("void:items", Map.copyOf(voidItems)); locations.put("ordinary:items", Map.copyOf(ordinaryItems)); locations.put("phials", Map.copyOf(phials));
            var total = new LinkedHashMap<String, Integer>(); for (var contents : locations.values()) contents.forEach((tag, amount) -> { if (amount > 0) total.merge(tag, amount, Integer::sum); });
            int raw = count(player, Items.COAL_BLOCK) + countStack(player.containerMenu.getCarried(), Items.COAL_BLOCK) + countStack(oven(level).getItem(0), Items.COAL_BLOCK);
            int fuel = count(player, Items.COAL) + countStack(player.containerMenu.getCarried(), Items.COAL) + countStack(oven(level).getItem(1), Items.COAL);
            boolean dry = potentiaDepth > 0 && total.getOrDefault("potentia", 0) == 72 && oven(level).getAspects().getAmount(Aspect.ENERGY) == 0 && alembic(level, potentiaDepth).getAmount() == 0;
            for (int index = 0; index < route.size(); index++) dry &= locations.getOrDefault("tube:" + index, Map.of()).isEmpty();
            return new Frame(level.getGameTime(), raw, fuel, Map.copyOf(total), Map.copyOf(voided), Map.copyOf(locations), dry);
        }
        void verifyBalance(Frame frame) {
            require(frame.rawBlocks() >= 0 && frame.rawBlocks() <= coalBlocks && frame.fuelLeft() >= 0 && frame.fuelLeft() <= FUEL_INPUTS, "finite actual raw/fuel inputs");
            for (String tag : budget.keySet()) require(budget.get(tag) == frame.rawBlocks() * coalTags.get(tag) + frame.stored().getOrDefault(tag, 0) + frame.voided().getOrDefault(tag, 0), "EXACT supplied = unprocessed raw potential + stored + explicitly voided: " + tag);
            for (String tag : frame.stored().keySet()) require(budget.containsKey(tag), "no unexpected supplied/seeded aspect");
        }
        private void transfer(Frame frame, String from, String to, String tag, int supplied, int stored, int destroyed) {
            require(supplied == stored + destroyed, "EACH native essentia transfer supplied = stored + explicitly voided");
            transfers.add(Map.of("native_tick", frame.tick(), "from", from, "to", to, "aspect", tag, "supplied", supplied, "stored", stored, "voided", destroyed, "read_only_observation", true));
        }
    }

    private void startObservation(ServerPlayer player) {
        nativeTicks = server -> { if (server != player.server || observationFailure != null) return; try { ledger.observe(); } catch (RuntimeException | Error error) { observationFailure = error; stopObservation(); } };
        nativeAdds = (entity, level) -> {
            if (level == player.level() && awaitingDrop && entity instanceof ItemEntity item && item.getItem().is(voidItem()) && patchBounds().contains(item.position())) {
                try {
                    require(nativeDropId == null, "exactly ONE native void-jar drop, no duplicate output entity"); ItemStack actual = item.getItem();
                    require(actual.getCount() == 1 && actual.getMaxStackSize() == 1 && ItemJar.isVoid(actual) && ItemJar.getAmount(actual) == 64 && ItemJar.getAspect(actual) == Aspect.ENERGY && ItemJar.getFilter(actual) == Aspect.ENERGY, "real native drop preserves void variant, actual64 contents and actual label");
                    nativeDrop = actual.copy(); nativeDropId = item.getUUID();
                } catch (RuntimeException | Error error) { observationFailure = error; }
            }
            return EventResult.pass();
        };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        TickEvent.SERVER_POST.register(nativeTicks); EntityEvent.ADD.register(nativeAdds); LifecycleEvent.SERVER_STOPPED.register(serverStopped);
    }
    private void stopObservation() {
        if (nativeTicks != null) { TickEvent.SERVER_POST.unregister(nativeTicks); nativeTicks = null; }
        if (nativeAdds != null) { EntityEvent.ADD.unregister(nativeAdds); nativeAdds = null; }
        if (serverStopped != null) { LifecycleEvent.SERVER_STOPPED.unregister(serverStopped); serverStopped = null; }
    }

    /** Read-only durable-output view; never calls a gameplay enqueue/flush/tick. */
    @SuppressWarnings("unchecked")
    private List<ItemStack> pendingStacks(ServerLevel level) {
        try {
            var field = DeferredItemDrops.class.getDeclaredField("FACTORY"); field.setAccessible(true);
            var factory = (SavedData.Factory<DeferredItemDrops>) field.get(null);
            var pending = level.getDataStorage().get(factory, "thaumcraft_pending_inventory_drops"); if (pending == null) return List.of();
            CompoundTag detached = pending.save(new CompoundTag(), level.registryAccess()); var result = new ArrayList<ItemStack>();
            for (Tag raw : detached.getList("Pending", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) raw; if (!patchBounds().contains(Vec3.atCenterOf(BlockPos.of(row.getLong("Source"))))) continue;
                ItemStack value = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElseThrow(); value.setCount(row.getInt("Count")); result.add(value);
            }
            return result;
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Read-only pending-drop accounting needs root API reconciliation", error); }
    }

    private void scanOld(ServerLevel level) {
        for (int x = (center.getX() - 12) >> 4; x <= (center.getX() + 230) >> 4; x++) for (int z = (center.getZ() - 18) >> 4; z <= (center.getZ() + 14) >> 4; z++) level.getChunk(x, z);
        int jars = 0, units = 0, candles = 0;
        // Includes all earlier fixtures AND the complete accepted Frost patch
        // (through x1700/y224/z389); NEW patch starts x1710, never excluded old BE.
        for (BlockPos at : BlockPos.betweenClosed(center.offset(-12, -2, -18), center.offset(230, 10, 14))) {
            oldBlocks.put(at.immutable(), level.getBlockState(at)); BlockEntity tile = level.getBlockEntity(at);
            if (tile instanceof TileNode) oldNodes.put(at.immutable(), stableNodeTag(tile)); else if (tile != null) oldBEs.put(at.immutable(), beTag(tile));
            if (tile instanceof TileJar jar) { ++jars; units += jar.getAmount(); }
            if (level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get())) ++candles;
        }
        require(jars == 12 && units == 173 && candles == 72 && oldBEs.size() >= 37, "whole prior-world scan retains actual12jars/173units/72candles and prior Frost/Excavation benches/machines");
        require(level.getBlockEntity(center.offset(208, 0, 0)) instanceof TileArcaneWorkbench table && table.isEmpty(), "accepted Frost bench included, empty and never repurposed");
        report.put("old_world_scan", Map.of("min", center.offset(-12, -2, -18).toShortString(), "max", center.offset(230, 10, 14).toShortString(), "old_jar_count", jars, "old_jar_units", units, "old_candles", candles, "all_non_node_BEs_exact_at_end", "pending final assertion", "native_node_current_regeneration", "allowed; identities/base/type retained, no setters"));
    }
    private void retained(ServerPlayer player) {
        for (ItemStack saved : savedStacks) {
            if (gold(saved)) continue;
            int before = savedStacks.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(value -> ItemStack.isSameItemSameComponents(value, saved)).mapToInt(ItemStack::getCount).sum();
            int consumed = saved.is(ordinaryItem()) || saved.is(Items.BLAZE_POWDER) || saved.is(emptyPhial()) ? 1 : saved.is(tubeItem()) ? route.size() : 0;
            require(after == before - consumed, "every old main count/component retained EXCEPT declared saved jar/powder/one converted phial/placed tubes: " + saved);
        }
        for (int index = 0; index < ARMOR.size(); index++) require(ItemStack.matches(player.getItemBySlot(ARMOR.get(index)), savedArmor.get(index)), "same exact saved worn armor");
        require(ItemStack.matches(stack(player, VoidJarClientSequence::primary), savedPrimary) && ItemStack.matches(player.getInventory().getItem(7), paidGold) && ItemStack.matches(player.getOffhandItem(), savedOffhand) && count(player, ThaumcraftContent.WAND.get()) == 3, "primary Frost/other wand/foci/offhand exact; native paid gold returned to7");
        require(player.totalExperience == 11 && player.totalExperience == initialXp && player.isInvulnerable() == savedInvulnerability && player.isAlive() && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "saved XP11/Entity flag/Survival preserved, no damage/XP fixture");
        require(expectedKnowledge.equals(KnowledgeAccess.get(player).save()), "knowledge/pools/scans/warp unchanged beyond declared JARVOID research fixture");
        for (var entry : oldBlocks.entrySet()) {
            BlockPos at = entry.getKey(); BlockState before = entry.getValue(), after = player.level().getBlockState(at); BlockPos cactus = center.offset(149, 0, 5);
            boolean column = at.getX() == cactus.getX() && at.getZ() == cactus.getZ() && at.getY() >= cactus.getY() && at.getY() <= cactus.getY() + 2;
            boolean naturalGrowth = column && (before.is(Blocks.CACTUS) && after.is(Blocks.CACTUS) || before.isAir() && after.is(Blocks.CACTUS) && player.level().getBlockState(at.below()).is(Blocks.CACTUS));
            require(before == after || naturalGrowth, "all old blocks through complete Frost patch retained; ONLY native saved cactus AGE/bounded growth allowed: " + at);
        }
        for (var entry : oldBEs.entrySet()) require(player.level().getBlockEntity(entry.getKey()) != null && beTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "old BE/jar/container data exact: " + entry.getKey());
        for (var entry : oldNodes.entrySet()) require(player.level().getBlockEntity(entry.getKey()) instanceof TileNode && stableNodeTag(player.level().getBlockEntity(entry.getKey())).equals(entry.getValue()), "old node identity/base/type unchanged; native current/lastActive may evolve without intervention");
    }
    private void finalState(ServerPlayer player) {
        ledger.observe(); Frame frame = observed; retained(player); report.put("old_world_scan_final_verified", true);
        require(benchEmpty && ((TileArcaneWorkbench) player.level().getBlockEntity(workbench)).isEmpty() && player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty() && !player.isUsingItem() && !player.isShiftKeyDown(), "native menus/cursor/inputs released; genuinely empty new table");
        require(frame.rawBlocks() == 0 && frame.potentiaDrained() && ledger.heldDrains == 4 && drains.size() == 4 && ledger.pipeStored == 8 && ledger.pipeVoided == budget.get("potentia") - 72 && ledger.checks >= 1000, "all four32 held drains, eight real refill units and EACH overflow unit observed on real native ticks");
        require(jar(player.level()).isVoid() && jar(player.level()).getAmount() == 64 && jar(player.level()).getAspect() == Aspect.ENERGY && jar(player.level()).getFilter() == Aspect.ENERGY && jar(player.level()).getFacing() == Direction.SOUTH && jar(player.level()).getSuctionAmount(Direction.UP) == 32 && jar(player.level()).getMinimumSuction() == 48, "actual replaced full labelled void64: original32full suction/48minimum, native fresh facing");
        TileJar normal = (TileJar) player.level().getBlockEntity(ordinaryPos); require(!normal.isVoid() && normal.getAmount() == 64 && normal.getAspect() == Aspect.FIRE && normal.getFilter() == null, "native crafted ordinary jar retains actual64 other output, never mistaken for an old saved jar");
        require(alembic(player.level(), potentiaDepth).getFilter() == Aspect.ENERGY && alembic(player.level(), 3 - potentiaDepth).getFilter() == Aspect.FIRE, "both actual source labels persist without seeding any contents");
        require(count(player, emptyPhial()) == 3 && count(player, filledPhial()) == 1 && phialUnits(player) == 8 && count(player, tubeItem()) == 6 - route.size() && count(player, labelItem()) == 1 && ItemJarLabel.data(stack(player, value -> value.is(labelItem()))) == null, "original four phials conserved as3empty+1real8, saved tubes and fourth crafted BLANK label retained");
        require(count(player, ordinaryItem()) == 0 && count(player, voidItem()) == 0 && count(player, Items.BLAZE_POWDER) == 0 && nativeDropId != null && nativeDrop != null, "saved jar/powder consumed; actual crafted jar outputs placed, real drop retained/replaced exactly once");
        require(player.serverLevel().getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && pendingStacks(player.serverLevel()).isEmpty(), "no lost/duplicated physical jars/labels/XP or pending output debt");
        require(ledger.fuelSpent + frame.fuelLeft() == FUEL_INPUTS, "all supplied raw fuel is either natively burnt or actually retained, never discarded");
        var accounting = new LinkedHashMap<String, Object>(); accounting.put("supplied", budget); accounting.put("stored", frame.stored()); accounting.put("explicitly_voided", frame.voided()); accounting.put("unprocessed_raw_blocks", 0); accounting.put("actual_final_locations", frame.locations()); accounting.put("native_checks", ledger.checks); accounting.put("native_held_drains_32", ledger.heldDrains); accounting.put("native_pipe_refill_units", ledger.pipeStored); accounting.put("native_full_overflow_units", ledger.pipeVoided); accounting.put("normal_fuel_coal_spent", ledger.fuelSpent); accounting.put("normal_fuel_coal_retained", frame.fuelLeft()); accounting.put("asserted_each_transition", "supplied = stored + voided; before native smelt also includes explicitly unprocessed raw potential"); report.put("final_exact_unit_accounting", accounting);
        report.put("new_native_outputs_retained", Map.of("placed_void_jar", "potentia64 + actual label", "placed_ordinary_jar", "ignis64", "actual_filled_phial", snbt(stack(player, value -> value.is(filledPhial())), player), "new_blank_labels_remaining", 1, "new_source_labels_remaining", 2, "other_distillation_output_in_real_furnace_alembic", budget.get("ignis") - 64, "saved_tubes_remaining", count(player, tubeItem()), "empty_original_phials", 3, "final_gold_payer", snbt(player.getInventory().getItem(7), player)));
        report.put("retained_prior_outputs", Map.of("all_old_main_components_counts_except_declared_inputs", true, "primary_Frost_payer_EXACT", true, "other_old_wand_and_all_foci_EXACT", true, "all_armor_worn_EXACT", true, "old_twelve_jars_173_units_72_candles", true, "old_non_node_BEs_EXACT", oldBEs.size(), "complete_accepted_Frost_patch_included", true, "saved_total_XP", player.totalExperience, "prior_acquisition_replayed", false));
        report.put("native_replaced_void_jar", Map.of("actual_BE_snbt", beTag(jar(player.level())).toString(), "actual_variant", "thaumcraft:jar_void", "native_new_facing", jar(player.level()).getFacing().getName(), "actual_drop_UUID", nativeDropId.toString(), "outputs_or_essentia_injected", 0)); finalVerified = true;
    }

    private void approachDrop(ServerPlayer player) {
        if (nativeDropId == null) return;
        if (count(player, voidItem()) == 1) { require(ItemStack.matches(stack(player, value -> value.is(voidItem())), nativeDrop), "physical pickup retains exact observed native drop"); return; }
        require(player.serverLevel().getEntity(nativeDropId) instanceof ItemEntity, "same actual native item entity remains until pickup, never a replacement output");
        ItemEntity entity = (ItemEntity) player.serverLevel().getEntity(nativeDropId); require(ItemStack.matches(entity.getItem(), nativeDrop), "same native drop components while approaching its random real position");
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 candidate = new Vec3(entity.getX() + direction.getStepX() * 1.05, workbench.getY(), entity.getZ() + direction.getStepZ() * 1.05);
            if (player.level().noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) { stand(player, candidate.x, candidate.y, candidate.z); return; }
        }
        throw new IllegalStateException("No collision-free camera approach to real native drop at " + entity.position());
    }

    private BlockPos patchMin() { return workbench.offset(-4, -2, -7); }
    private BlockPos patchMax() { return workbench.offset(15, 7, 8); }
    private AABB patchBounds() { return new AABB(Vec3.atLowerCornerOf(patchMin()), Vec3.atLowerCornerOf(patchMax().offset(1, 1, 1))); }
    private void patch(ServerLevel level) {
        for (int x = patchMin().getX() >> 4; x <= patchMax().getX() >> 4; x++) for (int z = patchMin().getZ() >> 4; z <= patchMax().getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(ExperienceOrb.class, patchBounds()).isEmpty() && level.getEntitiesOfClass(LivingEntity.class, patchBounds()).isEmpty() && pendingStacks(level).isEmpty(), "NEW patch refuses old living entities/items/XP/pending outputs");
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) require(level.getBlockEntity(at) == null && !BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).getNamespace().equals("thaumcraft") && !oldBlocks.containsKey(at), "NEW patch may not overwrite ANY old BE/TC block/scanned cell: " + at);
        for (BlockPos at : BlockPos.betweenClosed(patchMin(), patchMax())) level.setBlockAndUpdate(at, at.getY() < workbench.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }
    private void supply(ServerPlayer player, Item item, int amount) {
        require(item == Items.OBSIDIAN && amount == 1 || item == Items.GLASS_PANE && amount == 7 || item == Items.OAK_SLAB && amount == 1 || item == Items.PAPER && amount == 4 || item == Items.INK_SAC && amount == 1 || item == Items.SLIME_BALL && amount == 1 || item == Items.COAL_BLOCK && amount == coalBlocks || item == Items.COAL && amount == FUEL_INPUTS, "ONLY declared finite RAW materials; never a jar/label/essentia/phial/vis/XP output grant");
        int slot = emptyMain(player); player.getInventory().setItem(slot, new ItemStack(item, amount)); supplied.add(Map.of("raw_item", BuiltInRegistries.ITEM.getKey(item).toString(), "count", amount, "genuinely_empty_main_slot", slot));
    }
    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page"); var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int index = page; index < Math.min(page + 2, pages.size()); index++) if (BookClientSmoke.field(pages.get(index), "recipe") instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(voidRecipe)) { require(display.recipe() == client.level.getRecipeManager().byKey(voidRecipe).orElseThrow(), "native book uses actual synced manager RecipeHolder"); return true; }
        require(++turns <= 16, "bounded native JARVOID recipe navigation"); var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(value -> value.active && value.getMessage().getString().equals("→")).findFirst().orElseThrow();
        var screen = client.screen; screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0); ticks = 0; return false;
    }
    private void capture(int index) { require(captures == index, "exact capture order100-106, no unrelated screenshots"); ++captures; report.put("actual_captures_requested", captures); capture.accept(CAPTURES.get(index)); }
    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand SWAP only outside machine menus");
        if (!wanted.test(client.player.getInventory().getItem(HAND_SLOT))) { click(client, playerSlot(client, itemSlot(client.player, wanted)), HAND_SLOT, ClickType.SWAP); handChangedAt = client.player.tickCount; return false; }
        if (client.player.tickCount - handChangedAt < 6) return false; select(client, HAND_SLOT); return true;
    }
    private boolean emptyHand(Minecraft client) throws ReflectiveOperationException { return hand(client, ItemStack::isEmpty); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException { client.player.getInventory().selected = slot; var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem"); ensure.setAccessible(true); ensure.invoke(client.gameMode); }
    private static int playerSlot(Minecraft client, int inventory) { return client.player.containerMenu.slots.stream().filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventory).findFirst().orElseThrow().index; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static String use(Minecraft client, BlockPos pos, Direction face, Vec3 point, boolean accepted) { require(client.player.canInteractWithBlock(pos, 0), "native block action in actual Survival reach: " + pos); lookAt(client, point); var result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)); require(!accepted || result.consumesAction(), "native useItemOn must accept " + pos + "/" + face); return result.name(); }
    private static void lookAt(Minecraft client, Vec3 point) { Vec3 delta = point.subtract(client.player.getEyePosition()); client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot(); client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot(); }
    private static void stand(ServerPlayer player, double x, double y, double z) { var body = new AABB(x - .3, y + .001, z - .3, x + .3, y + 1.8, z + .3); BlockPos floor = BlockPos.containing(x, y - .01, z); require(player.level().noCollision(player, body) && !player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty(), "collision-checked camera above existing declared solid floor"); player.teleportTo(x, y, z); }
    private void machineCamera(ServerPlayer player) { stand(player, furnace.getX() + .5, workbench.getY(), furnace.getZ() + 3.2); }
    private Vec3 sourcePoint(int depth) { return Vec3.atCenterOf(furnace.above(depth)).add(0, -.05, .5); }
    private Vec3 jarPoint() { return new Vec3(voidPos.getX() + .5, voidPos.getY() + .4, voidPos.getZ() + .8125); }
    private static void hover(Minecraft client, int inventory) throws ReflectiveOperationException { int[] point = inventoryPoint(client, inventory); client.screen.mouseMoved(point[0], point[1]); GLFW.glfwSetCursorPos(client.getWindow().getWindow(), point[0] * (double) client.getWindow().getScreenWidth() / client.screen.width, point[1] * (double) client.getWindow().getScreenHeight() / client.screen.height); }
    private static void requireHover(Minecraft client, int inventory) throws ReflectiveOperationException { int[] point = inventoryPoint(client, inventory); double x = client.mouseHandler.xpos() * client.screen.width / client.getWindow().getScreenWidth(), y = client.mouseHandler.ypos() * client.screen.height / client.getWindow().getScreenHeight(); require(Math.abs(x - point[0]) <= 1 && Math.abs(y - point[1]) <= 1, "real native cursor hovers actual jar/phial inventory slot"); }
    private static int[] inventoryPoint(Minecraft client, int inventory) throws ReflectiveOperationException { var slot = client.player.containerMenu.getSlot(playerSlot(client, inventory)); return new int[] {(int) BookClientSmoke.field(client.screen, "leftPos") + slot.x + 8, (int) BookClientSmoke.field(client.screen, "topPos") + slot.y + 8}; }
    private static void releaseKeys(Minecraft client) { client.options.keyUse.setDown(false); client.options.keyAttack.setDown(false); client.options.keyShift.setDown(false); }
    private void releaseInputs(Minecraft client) { releaseKeys(client); if (client.player != null && client.gameMode != null) { client.gameMode.stopDestroyBlock(); if (client.player.isUsingItem()) client.gameMode.releaseUsingItem(client.player); } if (previousCamera != null) client.options.setCameraType(previousCamera); }
    /** Detached comparison copies only; never installed in player/world storage. */
    private static void verifyDebit(ItemStack before, ItemStack after, Map<String, Integer> price) { ItemStack a = before.copy(), b = after.copy(); a.remove(ThaumcraftContent.WAND_DATA.get()); b.remove(ThaumcraftContent.WAND_DATA.get()); require(ItemStack.matches(a, b), "all non-vis wand/focus/craft-cost components EXACT"); WandData first = ItemWandCasting.data(before), last = ItemWandCasting.data(after); require(first.rod().equals(last.rod()) && first.cap().equals(last.cap()), "same actual saved payer materials"); for (String tag : WandData.PRIMALS) require(first.amount(tag) - last.amount(tag) == price.getOrDefault(tag, 0), "exact finite native centivis debit: " + tag); }
    private static int amount(Frame frame, String location, String tag) { return frame.locations().getOrDefault(location, Map.of()).getOrDefault(tag, 0); }
    private static Map<String, Integer> singleton(Aspect aspect, int amount) { var result = new LinkedHashMap<String, Integer>(); merge(result, aspect, amount); return Map.copyOf(result); }
    private static void merge(Map<String, Integer> result, Aspect type, int amount) { require(amount >= 0 && (amount == 0 || type != null), "native storage never hides untyped/negative essentia"); if (amount > 0) result.merge(type.getTag(), amount, Integer::sum); }
    private static Map<String, Integer> aspects(AspectList list) { var result = new LinkedHashMap<String, Integer>(); for (Aspect aspect : list.getAspects()) merge(result, aspect, list.getAmount(aspect)); return Map.copyOf(result); }
    private static int phialAmount(ItemStack value) { var data = ItemEssence.data(value); require(value.is(filledPhial()) && data != null && data.tag().equals("potentia"), "native matching filled phial has actual component"); return PhialData.AMOUNT * value.getCount(); }
    private static int phialUnits(Player player) { return player.getInventory().items.stream().filter(value -> value.is(filledPhial())).mapToInt(VoidJarClientSequence::phialAmount).sum(); }
    private static CompoundTag beTag(BlockEntity tile) { return tile.saveWithFullMetadata(tile.getLevel().registryAccess()).copy(); }
    private static CompoundTag stableNodeTag(BlockEntity tile) { CompoundTag result = beTag(tile); result.remove("Aspects"); result.remove("lastActive"); return result; }
    private static boolean gold(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("greatwood") && ItemWandCasting.data(value).cap().equals("gold"); }
    private static boolean primary(ItemStack value) { return value.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(value).rod().equals("silverwood") && ItemWandCasting.data(value).cap().equals("thaumium"); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int index = 0; index < 36; index++) if (wanted.test(player.getInventory().getItem(index))) return index; throw new IllegalStateException("Missing actual finite void-story input/output"); }
    private static ItemStack stack(Player player, Predicate<ItemStack> wanted) { return player.getInventory().getItem(itemSlot(player, wanted)); }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().mapToInt(value -> countStack(value, item)).sum(); }
    private static int countStack(ItemStack value, Item item) { return value.is(item) ? value.getCount() : 0; }
    private static int emptyMain(Player player) { for (int index = 9; index < 36; index++) if (player.getInventory().getItem(index).isEmpty()) return index; throw new IllegalStateException("No genuinely empty main inventory slot"); }
    private static Item voidItem() { return ThaumcraftContent.VOID_JAR.get().asItem(); }
    private static Item ordinaryItem() { return ThaumcraftContent.JAR.get().asItem(); }
    private static Item labelItem() { return ThaumcraftContent.MATERIALS.get("jar_label").get(); }
    private static Item emptyPhial() { return ThaumcraftContent.EMPTY_PHIAL.get(); }
    private static Item filledPhial() { return ThaumcraftContent.FILLED_PHIAL.get(); }
    private static Item tubeItem() { return ThaumcraftTubes.ESSENTIA_TUBE.get().asItem(); }
    private TileAlchemyFurnace oven(Level level) { return (TileAlchemyFurnace) level.getBlockEntity(furnace); }
    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(furnace.above(depth)); }
    private TileJar jar(Level level) { return (TileJar) level.getBlockEntity(voidPos); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ContainerAlchemyFurnace furnaceMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String snbt(ItemStack value, ServerPlayer player) { return value.save(player.registryAccess()).toString(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "no overlapping fixture/read-only server actions"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) {
        step = next; ticks = 0;
        thaumcraft.common.Thaumcraft.LOG.info("Void jar client step={} craft={} drain={} tube={}", step, craftIndex, drainIndex, tubeIndex);
    }
}
