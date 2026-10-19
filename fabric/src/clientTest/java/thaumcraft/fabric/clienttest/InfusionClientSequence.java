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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiAlchemyFurnace;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.gui.GuiResearchBrowser;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TilePedestal;

/** One playable native infusion story; fixtures supply inputs, never its paid intermediate/output state. */
public final class InfusionClientSequence {
    private enum Step {
        HANDOFF_HAND, HANDOFF_WAIT, SETUP, BOOK, BOOK_PAGE, MATRIX_CRAFT, PEDESTAL_CRAFT, CANDLE_CRAFT,
        CRAFT_TRAVEL, CRAFT_OPEN, CRAFT_FUND, CRAFT_GRID, CRAFT_READY,
        CRAFT_CAPTURE, CRAFT_TAKE, CRAFT_OUTPUT, CRAFT_STORE, CRAFT_STORED,
        CRAFT_WAND_PICK, CRAFT_WAND_STORE, CRAFT_VERIFY,
        BUILD_PREP, PLACE_POSITION, PLACE_HAND, PLACE_USE, PLACE_WAIT,
        FORMATION_PREP, FORMATION_AIM, FORMATION_USE, FORMATION_WAIT, FORMED_WORLD,
        FURNACE_TRAVEL, FURNACE_OPEN, FURNACE_FUEL, FURNACE_INPUT_PICK, FURNACE_INPUT_PUT,
        DISTILL_WAIT, DISTILL_VERIFY, PHIAL_POSITION, PHIAL_SELECT, PHIAL_USE, PHIAL_WITHDRAWN,
        JAR_POSITION, JAR_USE, JAR_FILLED, BATCH_VERIFY, ESSENTIA_WORLD,
        OFFER_PREP, OFFER_POSITION, OFFER_HAND, OFFER_USE, OFFER_WAIT,
        START_PREP, START_AIM, START_USE, START_WAIT, PAID_WORLD, CRAFT_WAIT, CORE_WORLD,
        CORE_POSITION, CORE_EJECT, CORE_PICKUP, CAPS_PREP, CAPS_CRAFT, ASSEMBLY_CRAFT,
        CHARGE_PREP, CHARGE_POSITION, CHARGE_AIM, CHARGE_START, CHARGE_WAIT,
        CHARGE_VERIFY, REWARD_WORLD, DONE
    }

    public final Map<String, Object> report = new LinkedHashMap<>();
    private static final List<String> PRIMALS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");
    private static final List<String> JAR_TAGS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio", "praecantatio", "vitreus");
    private static final int CANDLES = 72;
    private final BlockPos workbench, center, matrixPos, furnace, node;
    private final List<BlockPos> peripheral = new ArrayList<>(), candles = new ArrayList<>();
    private final Map<String, BlockPos> jars = new LinkedHashMap<>();
    private final List<Map<String, Object>> suppliedInputs = new ArrayList<>(), topUps = new ArrayList<>();
    private final List<Map<String, Object>> crafts = new ArrayList<>(), batches = new ArrayList<>(), placements = new ArrayList<>();
    private final List<ItemStack> retainedStacks = new ArrayList<>();
    private final List<Map<String, Object>> payments = new ArrayList<>(), ingredientPayments = new ArrayList<>();
    private Step step = Step.HANDOFF_HAND, afterCraft;
    private ItemStack handoffStack = ItemStack.EMPTY;
    private int handoffSlot = -1;
    private int ticks, totalTicks, bookTurns, clickIndex, crafted, destination, inventoryBefore;
    private int placeIndex, countBefore, batch, transfer, sourceDepth, sourceBefore, jarBefore, offerIndex;
    private int handChangedAt = Integer.MIN_VALUE / 2;
    private CompletableFuture<Void> serverAction;
    private Map<String, Object> snapshot = Map.of();
    private ItemStack expectedOldWand, offhandBefore, outputOnCursor;
    private CraftPlan craft;
    private Map<String, Integer> craftPrices = Map.of(), formationPrices = Map.of();
    private List<int[]> gridClicks = List.of();
    private final List<Placement> build = new ArrayList<>();
    private String transferTag;
    private long batchStarted, chargeStarted;
    private WandData chargedReward;
    private TickEvent.Server nativeTicks;
    private EntityEvent.Add nativeAdds;
    private LifecycleEvent.ServerState serverStopped;
    private volatile Throwable observationFailure;
    private volatile boolean finishedCraft;
    private volatile int paidUnits, paidIngredients, observedTicks;
    private boolean observedStart, ejectionArmed;
    private int lastDemand = 63, lastIngredients = 7, dropAttempts;
    private long observationStarted, lastPaymentTime = -1, firstIngredientTime = -1;
    private UUID coreDropId;
    private ItemStack coreDropStack;

    private record CraftPlan(String recipe, Item output, int amount, int repetitions, Item[] grid,
                             Map<String, Integer> baseVis, String capture) {}
    private record Placement(BlockPos target, BlockPos clicked, Item item) {}

    /** Root passes research+(160,0,8) and research+(176,0,8), NOT the accepted M06 origins. */
    public InfusionClientSequence(BlockPos workbench, BlockPos center) {
        this.workbench = workbench.immutable();
        this.center = center.immutable();
        matrixPos = center.above(2);
        furnace = center.west(9);
        node = workbench.offset(2, 2, 0);
        for (int x : new int[] {-3, 0, 3}) for (int z : new int[] {-3, 0, 3}) {
            if (x != 0 || z != 0) peripheral.add(center.offset(x, 0, z));
        }
        for (int radius : new int[] {4, 5}) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) == radius) candles.add(center.offset(x, 0, z));
        }
        for (int i = 0; i < JAR_TAGS.size(); i++) jars.put(JAR_TAGS.get(i), center.offset(-7 + 2 * i, 0, -8));
        report.put("boundary", "Retains the actual M06 greatwood/gold wand, book, four empty phials, six tubes, eight quicksilver drops, returned Fire Focus, blaze powder, offhand apple and every other prior inventory stack. Supplies only declared raw/intermediate INPUTS, research, old-wand INPUT vis top-ups, empty machines, stone floor, one temporary plain-stone matrix support and a finite NORMAL charging node. No new wand/core, essentia, filled phial/jar, bound label, pedestal contents, matrix session, activation or formed pillar is injected. Not full survival acquisition.");
        report.put("native_action_boundary", "C2S inventory/menu clicks, ordinary and arcane result extraction, crafted BlockItem placements on real clicked UP faces, real matrix wand useItemOn for formation/start, normal furnace ticks, real empty/filled-phial useItemOn, seven real peripheral inserts, native pedestal ejection/physical pickup, native wand assembly and held-use node charging. Server futures only prepare declared inputs/camera or inspect; no manual tile ticking or direct formation/start/drain/output calls.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "center", center.toShortString(), "matrix", matrixPos.toShortString(),
                "furnace", furnace.toShortString(), "node", node.toShortString(), "jars", jars.values().stream().map(BlockPos::toShortString).toList()));
        report.put("preparation_bounds", Map.of("workbench_min", workbench.offset(-2, -1, -2).toShortString(), "workbench_max", workbench.offset(2, 4, 3).toShortString(),
                "altar_min", center.offset(-11, -1, -11).toShortString(), "altar_max", center.offset(11, 5, 11).toShortString(), "old_fixtures_reused", false));
        report.put("supplied_inputs", suppliedInputs);
        report.put("old_wand_input_top_ups", topUps);
        report.put("native_crafts", crafts);
        report.put("native_placements", placements);
        report.put("distillation_batches", batches);
        report.put("capture_count", 8);
        report.put("not_covered", List.of("full survival/research acquisition", "natural worldgen/node distribution", "all instability outcomes in this client story",
                "other infusion rewards/enchantments", "focus transfer or Fire use with the new wand", "NeoForge", "visual/audio parity before actual capture inspection"));
    }

    public boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        try {
            return tickSequence(client, capture);
        } catch (ReflectiveOperationException | RuntimeException | Error error) {
            client.options.keyUse.setDown(false);
            client.options.keyShift.setDown(false);
            // Also runs for local-stage timeout/server-future failure. Normal
            // completion and the server-side watchdog remove these same observers.
            if (client.getSingleplayerServer() != null) client.getSingleplayerServer().execute(this::stopObservation);
            if (error instanceof RuntimeException) throw new IllegalStateException("Infusion sequence failed at step=" + step
                    + "; batch=" + batch + "; transfer=" + transfer + "; placement=" + placeIndex
                    + "; recipe=" + (craft == null ? "none" : craft.recipe()), error);
            throw error;
        }
    }

    private boolean tickSequence(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ++ticks;
        ++totalTicks;
        if (ticks > 2400 || totalTicks > 24000) throw new IllegalStateException("Infusion client timeout: step=" + step
                + "; recipe=" + (craft == null ? "none" : craft.recipe()) + "; batch=" + batch + "; transfer=" + transfer
                + "; placement=" + placeIndex + "; paid=" + paidUnits + "/63; ingredients=" + paidIngredients + "/7; hand=" + client.player.getMainHandItem());
        if (observationFailure != null) throw new IllegalStateException("Infusion native observation failed at " + step, observationFailure);
        switch (step) {
            case HANDOFF_HAND -> {
                client.setScreen(null);
                require(client.player.containerMenu == client.player.inventoryMenu, "previous native story returned its machine menu");
                ItemStack actual = client.player.getInventory().getItem(8);
                if (actual.isEmpty()) { next(Step.SETUP); return false; }
                // M06 leaves its six REAL crafted tubes in hotbar8. Move that
                // stack with a native SWAP; never erase it to make an action slot.
                handoffStack = actual.copy();
                for (int slot = 9; slot < 36; slot++) if (client.player.getInventory().getItem(slot).isEmpty()) { handoffSlot = slot; break; }
                require(handoffSlot >= 9, "one genuinely empty native slot for the retained M06 hand stack");
                click(client, inventorySlot(handoffSlot), 8, ClickType.SWAP);
                next(Step.HANDOFF_WAIT);
            }
            case HANDOFF_WAIT -> {
                if (ticks < 8 || !client.player.getInventory().getItem(8).isEmpty()
                        || !ItemStack.matches(client.player.getInventory().getItem(handoffSlot), handoffStack)) return false;
                report.put("native_prior_hand_preserved", Map.of("item", itemId(handoffStack.getItem()), "count", handoffStack.getCount(),
                        "from_hotbar", 8, "to_empty_main_inventory_slot", handoffSlot, "action", "native inventory SWAP packet"));
                next(Step.SETUP);
            }
            case SETUP -> {
                client.setScreen(null);
                client.options.keyUse.setDown(false);
                client.options.keyShift.setDown(false);
                select(client, 8);
                onServer(client, () -> prepare(serverPlayer(client)));
                next(Step.BOOK);
            }
            case BOOK -> {
                if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("ROD_silverwood")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("ROD_silverwood")));
                next(Step.BOOK_PAGE);
            }
            case BOOK_PAGE -> {
                if (ticks < 10 || !showRecipe(client)) return false;
                report.put("native_book", Map.of("research", "ROD_silverwood", "recipe", "thaumcraft:infusion/wand_rod_silverwood",
                        "central", "silverwood log", "components", "balanced shard + six distinct primal shards", "essentia", cost(), "recipe_instability", 5,
                        "opened_by", "native completed-research screen fixture, not survival research acquisition"));
                next(Step.MATRIX_CRAFT);
                capture.accept("53-native-silverwood-infusion-recipe-book-en.png");
            }
            case MATRIX_CRAFT -> beginCraft(client, new CraftPlan("arcane/infusion_matrix", matrixItem(), 1, 1,
                    new Item[] {stone(), shard("aer"), stone(), shard("aer"), Items.ENDER_PEARL, shard("aer"), stone(), shard("aer"), stone()},
                    Map.of("ordo", 40), "54-native-infusion-matrix40ordo-arcane-menu-en.png"), Step.PEDESTAL_CRAFT);
            case PEDESTAL_CRAFT -> beginCraft(client, new CraftPlan("arcane/arcane_pedestal", pedestalItem(), 2, 5,
                    new Item[] {stone(), stone(), stone(), null, stone(), null, stone(), stone(), stone()}, Map.of("aer", 5), null), Step.CANDLE_CRAFT);
            case CANDLE_CRAFT -> beginCraft(client, new CraftPlan("tallow_candle", candleItem(), 3, 24,
                    new Item[] {Items.STRING, null, null, tallow(), null, null, tallow(), null, null}, Map.of(), null), Step.BUILD_PREP);
            case CRAFT_TRAVEL -> {
                if (!serverDone() || ticks < 12) return false;
                select(client, 8);
                use(client, workbench, Direction.SOUTH, southPoint(workbench));
                next(Step.CRAFT_OPEN);
            }
            case CRAFT_OPEN -> {
                if (!(client.screen instanceof GuiArcaneWorkbench) || ticks < 8) return false;
                require(arcane(client).getCarried().isEmpty(), "craft starts with a genuinely empty cursor: " + craft.recipe());
                for (int slot = 1; slot <= 10; slot++) require(arcane(client).getSlot(slot).getItem().isEmpty(), "fresh native workbench inputs, not a supplied recipe: " + craft.recipe());
                click(client, arcaneSlot(7), 0, ClickType.QUICK_MOVE);
                next(Step.CRAFT_FUND);
            }
            case CRAFT_FUND -> {
                if (ticks < 8 || !ItemStack.matches(arcane(client).getSlot(1).getItem(), expectedOldWand)) return false;
                require(client.player.getInventory().getItem(7).isEmpty(), "the real retained wand moved into the actual wand slot");
                gridClicks = recipeClicks(client);
                clickIndex = 0;
                next(Step.CRAFT_GRID);
            }
            case CRAFT_GRID -> {
                if (ticks < 2) return false;
                if (clickIndex == gridClicks.size()) { next(Step.CRAFT_READY); return false; }
                int[] at = gridClicks.get(clickIndex++);
                click(client, at[0], at[1], ClickType.PICKUP);
                ticks = 0;
            }
            case CRAFT_READY -> {
                var menu = arcane(client);
                if (ticks < 10 || !menu.craftable() || !menu.getSlot(0).getItem().is(craft.output())) return false;
                require(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().getCount() == craft.amount(), "actual native result count/cursor for " + craft.recipe());
                verifyGrid(menu, craft.repetitions());
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var table = (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench);
                    var match = ArcaneCrafting.find(player, table);
                    require(match != null && match.recipe().id().equals(id(craft.recipe())) && match.output().is(craft.output()) && match.output().getCount() == craft.amount(),
                            "authoritative recipe-manager match for " + craft.recipe());
                    craftPrices = craft.baseVis().isEmpty() ? Map.of() : ArcaneVis.price(craft.baseVis(), table.getItem(10), player);
                    require(match.arcane() == !craft.baseVis().isEmpty() && match.prices().equals(craftPrices)
                            && ItemStack.matches(table.getItem(10), expectedOldWand), "native original price and actual input wand for " + craft.recipe());
                    if (craft.recipe().equals("arcane/wand_assembly")) require(isReward(match.output()) && ItemWandCasting.data(match.output()).vis().isEmpty()
                            && ItemWandCasting.getMaxVis(match.output()) == 10000 && match.output().getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), -1) == 27,
                            "actual Silverwood/gold preview is EMPTY with capacity10000 and assembly cost component27");
                    snapshot = Map.of("recipe", match.recipe().id().toString(), "prices_per_craft_centivis", craftPrices, "base_vis", craft.baseVis(),
                            "repetitions", craft.repetitions(), "each_result_count", craft.amount(), "physical_output_before_extraction", count(player, craft.output()));
                });
                next(Step.CRAFT_CAPTURE);
            }
            case CRAFT_CAPTURE -> {
                if (!serverDone()) return false;
                for (String primal : PRIMALS) require(arcane(client).requiredVis(primal) == craftPrices.getOrDefault(primal, 0), "native menu price agrees with server for " + primal);
                report.put("preview_" + craft.recipe(), snapshot);
                next(Step.CRAFT_TAKE);
                if (craft.capture() != null) capture.accept(craft.capture());
            }
            case CRAFT_TAKE -> {
                require(arcane(client).getCarried().isEmpty(), "no hidden cursor output before result extraction");
                inventoryBefore = count(client.player, craft.output());
                click(client, 0, 0, ClickType.PICKUP);
                next(Step.CRAFT_OUTPUT);
            }
            case CRAFT_OUTPUT -> {
                var menu = arcane(client);
                if (ticks < 6 || !menu.getCarried().is(craft.output())) return false;
                require(menu.getCarried().getCount() == craft.amount(), "one native recipe extraction, not a batch-created output");
                var data = ItemWandCasting.data(expectedOldWand);
                for (var price : craftPrices.entrySet()) data = data.withAmount(price.getKey(), data.amount(price.getKey()) - price.getValue());
                ItemStack expected = expectedOldWand.copy();
                expected.set(ThaumcraftContent.WAND_DATA.get(), data); // Assertion copy ONLY, never installed as an output.
                require(ItemStack.matches(menu.getSlot(1).getItem(), expected), "native craft paid the observed modifier-priced vis exactly once: " + craft.recipe());
                expectedOldWand = expected;
                verifyGrid(menu, craft.repetitions() - crafted - 1);
                outputOnCursor = menu.getCarried().copy();
                if (craft.recipe().equals("arcane/wand_assembly")) require(isReward(outputOnCursor) && ItemWandCasting.data(outputOnCursor).vis().isEmpty(), "new wand is genuinely empty, not precharged");
                destination = outputDestination(client.player, outputOnCursor);
                next(Step.CRAFT_STORE);
            }
            case CRAFT_STORE -> {
                click(client, arcaneSlot(destination), 0, ClickType.PICKUP);
                next(Step.CRAFT_STORED);
            }
            case CRAFT_STORED -> {
                if (ticks < 6 || !arcane(client).getCarried().isEmpty() || count(client.player, craft.output()) != inventoryBefore + craft.amount()) return false;
                if (++crafted < craft.repetitions()) { next(Step.CRAFT_TAKE); return false; }
                click(client, 1, 0, ClickType.PICKUP); // Wand slot normalizes SWAP buttons; use its real cursor path.
                next(Step.CRAFT_WAND_PICK);
            }
            case CRAFT_WAND_PICK -> {
                if (ticks < 6 || !ItemStack.matches(arcane(client).getCarried(), expectedOldWand)) return false;
                require(client.player.getInventory().getItem(7).isEmpty(), "original wand's return destination was not overwritten");
                click(client, arcaneSlot(7), 0, ClickType.PICKUP);
                next(Step.CRAFT_WAND_STORE);
            }
            case CRAFT_WAND_STORE -> {
                if (ticks < 6 || !ItemStack.matches(client.player.getInventory().getItem(7), expectedOldWand) || !arcane(client).getCarried().isEmpty()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(((TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench)).isEmpty() && player.containerMenu.getCarried().isEmpty(), "all native recipe inputs and wand leave the empty workbench");
                    retained(player);
                    var entry = new LinkedHashMap<String, Object>(snapshot);
                    entry.put("native_result_extractions", crafted);
                    entry.put("actual_total_output", crafted * craft.amount());
                    entry.put("old_wand_after", ItemWandCasting.data(player.getInventory().getItem(7)).vis());
                    entry.put("server_verified", true);
                    crafts.add(entry);
                });
                client.player.closeContainer();
                next(Step.CRAFT_VERIFY);
            }
            case CRAFT_VERIFY -> {
                if (!serverDone() || ticks < 8) return false;
                next(afterCraft);
            }
            case BUILD_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(count(player, matrixItem()) == 1 && count(player, pedestalItem()) == 10 && count(player, candleItem()) == CANDLES
                            && count(player, stone()) == 4 && count(player, bricks()) == 4 && count(player, tallow()) == 0 && count(player, Items.STRING) == 0,
                            "one native matrix, five pedestal crafts,24 ordinary candle crafts and exactly the raw blueprint remainder");
                    require(player.serverLevel().isEmptyBlock(center.above()), "temporary matrix support has a fresh empty cell");
                    player.serverLevel().setBlockAndUpdate(center.above(), Blocks.STONE.defaultBlockState());
                    report.put("temporary_support_fixture", Map.of("position", center.above().toShortString(), "block", "minecraft:stone", "removed_after_native_matrix_placement", true));
                });
                build.add(new Placement(center, center.below(), pedestalItem()));
                for (BlockPos at : peripheral) build.add(new Placement(at, at.below(), pedestalItem()));
                for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) {
                    BlockPos at = center.offset(x, 0, z);
                    build.add(new Placement(at, at.below(), bricks()));
                    build.add(new Placement(at.above(), at, stone()));
                }
                for (BlockPos at : candles) build.add(new Placement(at, at.below(), candleItem()));
                build.add(new Placement(matrixPos, center.above(), matrixItem()));
                next(Step.PLACE_POSITION);
            }
            case PLACE_POSITION -> {
                if (!serverDone()) return false;
                var job = build.get(placeIndex);
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(player.serverLevel().isEmptyBlock(job.target()), "native placement cannot replace an existing fixture/output: " + job.target());
                    if (job.target().equals(matrixPos)) {
                        // Stand on the already-crafted south pedestal, so the
                        // support's UP face really is visible from above.
                        // No floating player or invented placement face.
                        require(player.serverLevel().getBlockState(center.south(3)).is(ThaumcraftInfusion.PEDESTAL.get()), "native pedestal provides the matrix-placement step");
                        player.teleportTo(center.getX() + .5, center.getY() + 1, center.getZ() + 3.5);
                    } else player.teleportTo(job.target().getX() + .5, center.getY(), job.target().getZ() + 2.6);
                });
                next(Step.PLACE_HAND);
            }
            case PLACE_HAND -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> stack.is(build.get(placeIndex).item()))) return false;
                countBefore = count(client.player, build.get(placeIndex).item());
                next(Step.PLACE_USE);
            }
            case PLACE_USE -> {
                var job = build.get(placeIndex);
                use(client, job.clicked(), Direction.UP, Vec3.atCenterOf(job.clicked()).add(0, .5, 0));
                next(Step.PLACE_WAIT);
            }
            case PLACE_WAIT -> {
                var job = build.get(placeIndex);
                if (ticks < 6 || client.level.getBlockState(job.target()).getBlock().asItem() != job.item()
                        || count(client.player, job.item()) != countBefore - 1) return false;
                placements.add(Map.of("target", job.target().toShortString(), "clicked", job.clicked().toShortString(), "face", "up", "crafted_or_raw_input_item", itemId(job.item())));
                if (++placeIndex < build.size()) { next(Step.PLACE_POSITION); return false; }
                next(Step.FORMATION_PREP);
            }
            case FORMATION_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(level.getBlockState(center.above()).is(Blocks.STONE) && level.getBlockEntity(center.above()) == null
                            && !matrix(level).isActive() && !matrix(level).isCrafting(), "only the declared plain support is removed, never matrix activation/session data");
                    level.setBlockAndUpdate(center.above(), Blocks.AIR.defaultBlockState());
                    require(count(player, pedestalItem()) == 1 && count(player, matrixItem()) == 0 && count(player, candleItem()) == 0
                            && count(player, stone()) == 0 && count(player, bricks()) == 0, "nine of ten crafted pedestals and all72crafted candles were natively placed; one pedestal is retained");
                    for (BlockPos at : candles) require(level.getBlockState(at).is(ThaumcraftInfusion.TALLOW_CANDLE.get()), "actual ordinary-crafted stabilizer remains at " + at);
                    topUp(player, "before native25/primal formation");
                    var base = new LinkedHashMap<String, Integer>(); for (String tag : PRIMALS) base.put(tag, 25);
                    formationPrices = ArcaneVis.price(base, player.getInventory().getItem(7), player);
                    player.teleportTo(center.getX() + .5, center.getY(), center.getZ() + 2.6);
                });
                next(Step.FORMATION_AIM);
            }
            case FORMATION_AIM -> {
                if (!serverDone() || ticks < 10) return false;
                select(client, 7);
                lookAt(client, southPoint(matrixPos));
                next(Step.FORMATION_USE);
            }
            case FORMATION_USE -> {
                use(client, matrixPos, Direction.SOUTH, southPoint(matrixPos));
                next(Step.FORMATION_WAIT);
            }
            case FORMATION_WAIT -> {
                if (ticks < 10 || !(client.level.getBlockEntity(matrixPos) instanceof TileInfusionMatrix matrix) || !matrix.isActive()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    var data = ItemWandCasting.data(expectedOldWand);
                    for (var price : formationPrices.entrySet()) data = data.withAmount(price.getKey(), data.amount(price.getKey()) - price.getValue());
                    ItemStack expected = expectedOldWand.copy(); expected.set(ThaumcraftContent.WAND_DATA.get(), data);
                    require(ItemStack.matches(player.getInventory().getItem(7), expected) && matrix(level).isActive() && !matrix(level).isCrafting(), "native wand formation alone paid the actual modifier-priced25/primal and activated the empty matrix");
                    expectedOldWand = expected;
                    int lower = 0, upper = 0;
                    for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) for (int dy = 0; dy <= 1; dy++) {
                        var state = level.getBlockState(center.offset(x, dy, z));
                        require(state.is(ThaumcraftInfusion.PILLAR.get()) && state.getValue(BlockInfusionPillar.HALF) == (dy == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER), "native formation produced each lower/upper pillar half");
                        if (dy == 0) lower++; else upper++;
                    }
                    retained(player);
                    snapshot = Map.of("server_verified", true, "entry", "native wand useItemOn on crafted matrix", "base_each_primal_vis", 25,
                            "actual_prices_centivis", formationPrices, "lower_pillars", lower, "upper_pillars", upper, "native_candles", CANDLES,
                            "pedestals_placed", 9, "pedestals_retained", 1);
                    player.teleportTo(center.getX() + 7.4, center.getY(), center.getZ() + 8.4);
                });
                next(Step.FORMED_WORLD);
            }
            case FORMED_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                report.put("native_formation", snapshot);
                lookAt(client, Vec3.atCenterOf(center).add(0, 1.1, 0));
                next(Step.FURNACE_TRAVEL);
                capture.accept("55-native-crafted-altar-wand-formation-and72candles-world-en.png");
            }
            case FURNACE_TRAVEL -> {
                onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() + .5, furnace.getY(), furnace.getZ() + 2.6));
                next(Step.FURNACE_OPEN);
            }
            case FURNACE_OPEN -> {
                if (!serverDone() || ticks < 10) return false;
                select(client, 8);
                use(client, furnace, Direction.SOUTH, southPoint(furnace));
                next(Step.FURNACE_FUEL);
            }
            case FURNACE_FUEL -> {
                if (!(client.screen instanceof GuiAlchemyFurnace) || ticks < 8) return false;
                if (batch == 0) {
                    int slot = itemSlot(client.player, stack -> stack.is(Items.COAL));
                    require(client.player.getInventory().getItem(slot).getCount() == 8 && furnaceMenu(client).getSlot(1).getItem().isEmpty(), "one finite supplied8-coal fuel stack, never supplied essentia");
                    click(client, furnaceSlot(slot), 0, ClickType.QUICK_MOVE);
                }
                next(Step.FURNACE_INPUT_PICK);
            }
            case FURNACE_INPUT_PICK -> {
                if (ticks < 8) return false;
                require(furnaceMenu(client).getSlot(0).getItem().isEmpty() && furnaceMenu(client).getCarried().isEmpty(), "one finished batch cannot be overwritten by another raw input");
                int slot = itemSlot(client.player, stack -> stack.is(shard(PRIMALS.get(batch))));
                require(client.player.getInventory().getItem(slot).getCount() == 8, "each native batch takes exactly eight unprocessed raw primal shards");
                click(client, furnaceSlot(slot), 0, ClickType.PICKUP);
                next(Step.FURNACE_INPUT_PUT);
            }
            case FURNACE_INPUT_PUT -> {
                if (ticks < 6 || !furnaceMenu(client).getCarried().is(shard(PRIMALS.get(batch)))) return false;
                require(furnaceMenu(client).getCarried().getCount() == 8, "real cursor, not backend input insertion");
                click(client, 0, 0, ClickType.PICKUP);
                onServer(client, () -> batchStarted = serverPlayer(client).serverLevel().getGameTime());
                next(Step.DISTILL_WAIT);
            }
            case DISTILL_WAIT -> {
                if (!serverDone() || ticks < 8 || !furnaceMenu(client).getSlot(0).getItem().isEmpty() || furnaceMenu(client).getVis() != 0) return false;
                int amount = 0; for (int depth = 1; depth <= 3; depth++) amount += alembic(client.level, depth).getAmount();
                if (amount != 32) return false;
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
                require(sourceDepth != 0 && transfer < 4, "a real typed alembic must fund each whole eight-unit withdrawal");
                sourceBefore = alembic(client.level, sourceDepth).getAmount();
                transferTag = alembic(client.level, sourceDepth).getAspect().getTag();
                require(transferTag.equals(PRIMALS.get(batch)) || transferTag.equals("praecantatio") || transferTag.equals("vitreus"), "native batch has only its original three aspect types");
                onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() + .5, furnace.getY(), furnace.getZ() + 2.6));
                next(Step.PHIAL_SELECT);
            }
            case PHIAL_SELECT -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> stack.is(ThaumcraftContent.EMPTY_PHIAL.get()))) return false;
                require(count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) == 0, "the same four returned phials are reused, no filled phials supplied");
                next(Step.PHIAL_USE);
            }
            case PHIAL_USE -> {
                BlockPos at = furnace.above(sourceDepth);
                use(client, at, Direction.SOUTH, southPoint(at));
                next(Step.PHIAL_WITHDRAWN);
            }
            case PHIAL_WITHDRAWN -> {
                if (ticks < 6 || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 3 || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 1
                        || alembic(client.level, sourceDepth).getAmount() != sourceBefore - PhialData.AMOUNT) return false;
                int slot = itemSlot(client.player, stack -> filledWith(stack, transferTag));
                require(client.player.getInventory().getItem(slot).getCount() == 1, "native extracted phial preserves the source aspect component");
                BlockPos at = jars.get(transferTag);
                jarBefore = jar(client.level, at).getAmount();
                require(jarBefore == 0 || jar(client.level, at).getAspect() == Aspect.getAspect(transferTag), "magic/vitreus/primal jars are never mixed");
                onServer(client, () -> serverPlayer(client).teleportTo(at.getX() + .5, at.getY(), at.getZ() + 2.6));
                next(Step.JAR_POSITION);
            }
            case JAR_POSITION -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> filledWith(stack, transferTag))) return false;
                next(Step.JAR_USE);
            }
            case JAR_USE -> {
                BlockPos at = jars.get(transferTag);
                use(client, at, Direction.SOUTH, new Vec3(at.getX() + .5, at.getY() + .4, at.getZ() + .8125));
                next(Step.JAR_FILLED);
            }
            case JAR_FILLED -> {
                var target = jar(client.level, jars.get(transferTag));
                if (ticks < 6 || target.getAmount() != jarBefore + PhialData.AMOUNT || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 4
                        || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 0) return false;
                require(target.getAspect() == Aspect.getAspect(transferTag) && target.getFilter() == null, "native whole-phial insertion preserves type without a supplied label");
                if (++transfer < 4) { next(Step.PHIAL_POSITION); return false; }
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    for (int depth = 1; depth <= 3; depth++) require(alembic(level, depth).getAmount() == 0, "all32batch units were moved by actual phials, not cleared");
                    require(oven(level).getVis() == 0 && oven(level).getItem(0).isEmpty() && storedTotal(player).equals(produced(batch + 1)), "exact cumulative native shard budget after batch " + batch);
                    retained(player);
                });
                next(Step.BATCH_VERIFY);
            }
            case BATCH_VERIFY -> {
                if (!serverDone()) return false;
                if (++batch < 6) { next(Step.FURNACE_TRAVEL); return false; }
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(storedTotal(player).equals(produced(6)) && storedTotal(player).values().stream().mapToInt(Integer::intValue).sum() == 192, "48raw shards produce exactly96primal+48magic+48vitreus, all retained");
                    for (String tag : JAR_TAGS) require(jar(level, jars.get(tag)).getAmount() == produced(6).get(tag)
                            && jar(level, jars.get(tag)).getAspect() == Aspect.getAspect(tag) && jar(level, jars.get(tag)).getFilter() == null, "native phials fill each distinct unlabelled jar exactly");
                    snapshot = Map.of("server_verified", true, "raw_distilled_shards", 48, "real_phial_withdrawals", 24, "real_phial_jar_insertions", 24,
                            "total_produced", 192, "jar_contents", produced(6), "empty_phials_retained", 4, "fuel_coal_supplied", 8,
                            "fuel_coal_unconsumed", oven(level).getItem(1).getCount() + count(player, Items.COAL), "fuel_coal_ignited", 8 - oven(level).getItem(1).getCount() - count(player, Items.COAL));
                    player.teleportTo(center.getX() + 9.2, center.getY(), center.getZ() - 10.2);
                });
                next(Step.ESSENTIA_WORLD);
            }
            case ESSENTIA_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                report.put("native_finite_essentia_supply", snapshot);
                select(client, 7);
                lookAt(client, Vec3.atCenterOf(center).add(0, 1, -3));
                next(Step.OFFER_PREP);
                capture.accept("56-native48shards-phials-eight-typed-jars192essentia-world-en.png");
            }
            case OFFER_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    for (String tag : PRIMALS) require(count(player, shard(tag)) == 0, "all six finite distillation inputs were actually consumed");
                    supply(player, ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem(), 1, "central infusion INPUT");
                    supply(player, shard("balanced"), 1, "separate peripheral infusion INPUT");
                    for (String tag : PRIMALS) supply(player, shard(tag), 1, "separate peripheral infusion INPUT, not distilled");
                    player.inventoryMenu.broadcastChanges();
                });
                next(Step.OFFER_POSITION);
            }
            case OFFER_POSITION -> {
                if (!serverDone()) return false;
                BlockPos at = offerPosition();
                onServer(client, () -> {
                    require(pedestal(serverPlayer(client).level(), at).isEmpty(), "native ingredient insertion cannot overwrite a pedestal output");
                    serverPlayer(client).teleportTo(at.getX() + .5, at.getY(), at.getZ() + 2.6);
                });
                next(Step.OFFER_HAND);
            }
            case OFFER_HAND -> {
                if (!serverDone() || ticks < 8 || !hand(client, stack -> stack.is(offerItem()))) return false;
                countBefore = count(client.player, offerItem());
                next(Step.OFFER_USE);
            }
            case OFFER_USE -> {
                use(client, offerPosition(), Direction.SOUTH, pedestalPoint(offerPosition()));
                next(Step.OFFER_WAIT);
            }
            case OFFER_WAIT -> {
                var actual = pedestal(client.level, offerPosition()).getItem(0);
                if (ticks < 6 || !actual.is(offerItem()) || actual.getCount() != 1 || count(client.player, offerItem()) != countBefore - 1) return false;
                if (++offerIndex < 8) { next(Step.OFFER_POSITION); return false; }
                next(Step.START_PREP);
            }
            case START_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    var holder = level.getRecipeManager().byKey(id("infusion/wand_rod_silverwood")).orElseThrow();
                    require(holder.value() instanceof InfusionStackRecipe, "actual native STACK infusion recipe is loaded");
                    var recipe = (InfusionStackRecipe) holder.value();
                    List<ItemStack> offered = peripheral.stream().map(at -> pedestal(level, at).getItem(0)).filter(stack -> !stack.isEmpty()).toList();
                    require(offered.size() == 7 && pedestal(level, peripheral.get(7)).isEmpty()
                            && recipe.matches(new InfusionRecipeInput(pedestal(level, center).getItem(0), offered), level)
                            && recipe.aspectCost().visSize() == 63 && recipe.instability() == 5 && matrix(level).isActive() && !matrix(level).isCrafting(),
                            "real central log/seven unordered components, one balancing pedestal empty; no injected session");
                    require(storedTotal(player).equals(produced(6)), "all192genuine units exist before the unpaid native start");
                    startObservation(player);
                    player.teleportTo(center.getX() + .5, center.getY(), center.getZ() + 2.6);
                });
                next(Step.START_AIM);
            }
            case START_AIM -> {
                if (!serverDone() || ticks < 10) return false;
                select(client, 7);
                lookAt(client, southPoint(matrixPos));
                next(Step.START_USE);
            }
            case START_USE -> {
                use(client, matrixPos, Direction.SOUTH, southPoint(matrixPos));
                next(Step.START_WAIT);
            }
            case START_WAIT -> {
                // Do not teleport out of reach until the actual start packet
                // has been accepted; a queued camera task can beat that packet.
                if (ticks < 8 || !matrix(client.level).isCrafting() || paidUnits < 1) return false;
                onServer(client, () -> serverPlayer(client).teleportTo(center.getX() + 9.2, center.getY(), center.getZ() - 10.2));
                next(Step.PAID_WORLD);
            }
            case PAID_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                lookAt(client, Vec3.atCenterOf(center).add(0, 1, -3));
                var matrix = matrix(client.level);
                int demand = matrix.getDemand().visSize();
                if (!matrix.isCrafting() || demand > 48 || demand <= 0 || paidUnits < 15) return false;
                report.put("native_paid_phase_capture", Map.of("client_remaining_demand", demand, "server_observed_paid_units_at_request", paidUnits,
                        "recipe_instability", matrix.getRecipeInstability(), "actual_symmetry", matrix.getSymmetry(), "actual_instability", matrix.getInstability(),
                        "stabilization", "72ordinary-crafted native candle placements in opposite pairs; eight peripheral pedestals, seven occupied; no I/session/RNG writes"));
                next(Step.CRAFT_WAIT);
                capture.accept("57-native-infusion-paid-wireless-essentia-beams-world-en.png");
            }
            case CRAFT_WAIT -> {
                if (!finishedCraft || !serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(paidUnits == 63 && paidIngredients == 7 && !matrix(level).isCrafting() && matrix(level).getDemand().visSize() == 0
                            && pedestal(level, center).getItem(0).is(rod()) && pedestal(level, center).getItem(0).getCount() == 1,
                            "actual native63one-unit payments and seven physical component consumptions yield exactly one Silverwood core");
                    require(storedTotal(player).equals(leftovers()) && storedTotal(player).values().stream().mapToInt(Integer::intValue).sum() == 129, "all129unused essentia, including48vitreus waste, remain physical");
                    retained(player);
                    snapshot = Map.of("server_verified", true, "recipe", "thaumcraft:infusion/wand_rod_silverwood", "real_one_unit_drains", paidUnits,
                            "real_component_consumptions", paidIngredients, "native_server_ticks_observed", observedTicks, "remaining_essentia", leftovers(),
                            "core_outputs", 1, "actual_drain_payments", List.copyOf(payments), "actual_component_payments", List.copyOf(ingredientPayments));
                    player.teleportTo(center.getX() + .5, center.getY(), center.getZ() + 2.6);
                });
                next(Step.CORE_WORLD);
            }
            case CORE_WORLD -> {
                if (!serverDone() || ticks < 15 || !pedestal(client.level, center).getItem(0).is(rod()) || matrix(client.level).isCrafting()) return false;
                report.put("native_infusion_result", snapshot);
                lookAt(client, Vec3.atCenterOf(center).add(0, .8, 0));
                next(Step.CORE_POSITION);
                capture.accept("58-native63essentia-seven-components-silverwood-core-pedestal-world-en.png");
            }
            case CORE_POSITION -> {
                onServer(client, () -> {
                    ejectionArmed = true;
                    require(count(serverPlayer(client), rod()) == 0, "core still belongs only to its genuine output pedestal before ejection");
                });
                next(Step.CORE_EJECT);
            }
            case CORE_EJECT -> {
                if (!serverDone() || ticks < 8) return false;
                require(client.player.getInventory().getItem(8).isEmpty(), "native core ejection uses an actually empty main hand");
                select(client, 8);
                use(client, center, Direction.SOUTH, pedestalPoint(center));
                next(Step.CORE_PICKUP);
            }
            case CORE_PICKUP -> {
                if (ticks < 12 || count(client.player, rod()) != 1 || !pedestal(client.level, center).isEmpty()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(dropAttempts == 1 && coreDropId != null && coreDropStack != null && level.getEntity(coreDropId) == null
                            && ItemStack.matches(player.getInventory().getItem(itemSlot(player, stack -> stack.is(rod()))), coreDropStack)
                            && pedestal(level, center).isEmpty() && count(player, rod()) == 1, "one real pedestal-ejected ItemEntity was physically picked up, not granted by a fixture");
                    report.put("native_core_pickup", Map.of("native_drop_uuid", coreDropId.toString(), "observed_native_add_attempts", dropAttempts,
                            "actual_item_snbt", coreDropStack.save(player.registryAccess()).toString(), "physical_pickup", true));
                    stopObservation();
                });
                next(Step.CAPS_PREP);
            }
            case CAPS_PREP -> {
                if (!serverDone()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    supply(player, Items.GOLD_NUGGET, 10, "two native gold-cap crafts INPUT, not supplied caps");
                    topUp(player, "before two gold caps and native27/primal Silverwood assembly");
                    player.inventoryMenu.broadcastChanges();
                });
                next(Step.CAPS_CRAFT);
            }
            case CAPS_CRAFT -> {
                if (!serverDone()) return false;
                beginCraft(client, new CraftPlan("arcane/wand_cap_gold", ThaumcraftWands.GOLD_WAND_CAP.get(), 1, 2,
                        new Item[] {Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.GOLD_NUGGET, Items.GOLD_NUGGET, null, Items.GOLD_NUGGET, null, null, null},
                        Map.of("ordo", 3, "ignis", 3, "aer", 3), null), Step.ASSEMBLY_CRAFT);
            }
            case ASSEMBLY_CRAFT -> beginCraft(client, new CraftPlan("arcane/wand_assembly", ThaumcraftContent.WAND.get(), 1, 1,
                    new Item[] {null, null, ThaumcraftWands.GOLD_WAND_CAP.get(), null, rod(), null, ThaumcraftWands.GOLD_WAND_CAP.get(), null, null},
                    costVis(27), "59-native-silverwood100vis-gold-wand-arcane-assembly-menu-en.png"), Step.CHARGE_PREP);
            case CHARGE_PREP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    int slot = itemSlot(player, InfusionClientSequence::isReward);
                    ItemStack reward = player.getInventory().getItem(slot);
                    require(isReward(reward) && ItemWandCasting.data(reward).vis().isEmpty() && ItemWandCasting.getMaxVis(reward) == 10000
                            && reward.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), -1) == 27 && count(player, ThaumcraftContent.WAND.get()) == 2
                            && count(player, rod()) == 0 && count(player, ThaumcraftWands.GOLD_WAND_CAP.get()) == 0 && count(player, Items.GOLD_NUGGET) == 0,
                            "native assembly consumes the actually infused core and two actually crafted caps, yields one EMPTY100vis reward and preserves the old wand");
                    require(level.isEmptyBlock(node) && level.getBlockEntity(node) == null, "fresh finite node fixture does not replace another unit's node");
                    level.setBlockAndUpdate(node, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
                    ((TileNode) level.getBlockEntity(node)).setAspects(new AspectList().add(Aspect.AIR, 80));
                    chargeStarted = level.getGameTime();
                    retained(player);
                    report.put("native_empty_wand_reward", Map.of("actual_item_snbt", reward.save(player.registryAccess()).toString(), "rod", "silverwood", "cap", "gold",
                            "capacity_centivis", 10000, "craft_cost_component", 27, "vis_before_real_charging", ItemWandCasting.data(reward).vis(), "original_wand_retained", true));
                    player.teleportTo(workbench.getX() + 1.5, workbench.getY(), workbench.getZ() + 2.6);
                });
                next(Step.CHARGE_POSITION);
            }
            case CHARGE_POSITION -> {
                if (!serverDone() || ticks < 12 || !(client.level.getBlockEntity(node) instanceof TileNode) || !hand(client, InfusionClientSequence::isReward)) return false;
                next(Step.CHARGE_AIM);
            }
            case CHARGE_AIM -> {
                lookAt(client, Vec3.atCenterOf(node));
                if (ticks < 10) return false;
                next(Step.CHARGE_START);
            }
            case CHARGE_START -> {
                lookAt(client, Vec3.atCenterOf(node));
                client.options.keyUse.setDown(true);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                next(Step.CHARGE_WAIT);
            }
            case CHARGE_WAIT -> {
                lookAt(client, Vec3.atCenterOf(node));
                if (ItemWandCasting.getVis(client.player.getMainHandItem(), "aer") < 6000) return false;
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                next(Step.CHARGE_VERIFY);
            }
            case CHARGE_VERIFY -> {
                if (ticks < 15) return false;
                chargedReward = ItemWandCasting.data(client.player.getMainHandItem());
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    ItemStack reward = player.getMainHandItem();
                    var chargingNode = (TileNode) level.getBlockEntity(node);
                    int remaining = chargingNode.getAspects().getAmount(Aspect.AIR);
                    require(!player.isUsingItem() && isReward(reward) && ItemWandCasting.data(reward).equals(chargedReward) && chargedReward.capacity() == 10000
                            && chargedReward.amount("aer") >= 6000 && chargedReward.amount("aer") > ItemWandCasting.getMaxVis(player.getInventory().getItem(7)),
                            "real held-use charges the native reward beyond the retained greatwood's50vis capacity");
                    require(chargingNode.getNodeVisBase(Aspect.AIR) == 80 && remaining >= 1 && remaining < 80 && chargedReward.amount("aer") % 100 == 0,
                            "supplied finite NORMAL80aer node was really drained and NODEPRESERVE leaves it alive");
                    require(PRIMALS.stream().filter(tag -> !tag.equals("aer")).allMatch(tag -> chargedReward.amount(tag) == 0), "charging cannot invent primals absent from its source");
                    retained(player);
                    require(storedTotal(player).equals(leftovers()) && count(player, pedestalItem()) == 1 && count(player, ThaumcraftContent.WAND.get()) == 2,
                            "all129essentia, spare crafted pedestal, original wand and prior outputs survive the reward story");
                    require(nativeTicks == null && nativeAdds == null && serverStopped == null, "all temporary native observations were removed before assembly/charging");
                    report.put("native_net_outputs", Map.of("crafted_matrix_placed", 1, "crafted_pedestals_placed", 9, "crafted_pedestals_retained", 1,
                            "ordinary_candles_crafted_and_placed", CANDLES, "infused_cores_created", 1, "infused_cores_consumed_by_assembly", 1,
                            "new_native_silverwood_gold_wands", 1, "original_greatwood_gold_wands_retained", 1, "remaining_real_essentia", 129));
                    snapshot = Map.of("server_verified", true, "actual_wand_snbt", reward.save(player.registryAccess()).toString(), "capacity_centivis", 10000,
                            "vis_centivis", chargedReward.vis(), "old_wand_capacity_centivis", 5000, "node_fixture_initial_current_and_base", Map.of("aer", 80),
                            "node_actual_remaining_aer", remaining, "native_elapsed_node_fixture_ticks", level.getGameTime() - chargeStarted,
                            "remaining_infusion_essentia", leftovers(), "input_only_node_fixture", true);
                });
                next(Step.REWARD_WORLD);
            }
            case REWARD_WORLD -> {
                if (!serverDone() || ticks < 10) return false;
                report.put("native100vis_wand_charging", snapshot);
                report.put("native_reward_presentation", presentation(client));
                report.put("native_client_ticks", totalTicks);
                lookAt(client, Vec3.atCenterOf(node).add(-.3, -.3, 0));
                next(Step.DONE);
                capture.accept("60-native-crafted-silverwood100vis-wand-charged-above50-world-en.png");
            }
            case DONE -> { return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        var level = player.serverLevel();
        require(workbench.getY() == center.getY() && workbench.getX() + 2 < center.getX() - 11,
                "root must supply fresh DISJOINT research+160 workbench and+176altar patches; do not reuse accepted M06 fixtures");
        require(candles.size() == CANDLES && peripheral.size() == 8, "literal paired72candle/eight-pedestal layout");
        patch(level, workbench.offset(-2, -1, -2), workbench.offset(2, 4, 3));
        patch(level, center.offset(-11, -1, -11), center.offset(11, 5, 11));
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        level.setBlockAndUpdate(furnace, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
        for (int depth = 1; depth <= 3; depth++) level.setBlockAndUpdate(furnace.above(depth), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.SOUTH));
        for (BlockPos at : jars.values()) level.setBlockAndUpdate(at, ThaumcraftContent.JAR.get().defaultBlockState());
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0
                        && count(player, ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get()) == 6 && count(player, ThaumcraftContent.MATERIALS.get("quicksilver_drop").get()) == 8
                        && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1 && count(player, Items.BLAZE_POWDER) == 1,
                "accepted M06 retained native outputs must be handed off intact");
        require(player.getInventory().getItem(6).is(ThaumcraftContent.THAUMONOMICON.get()) && player.getOffhandItem().is(Items.APPLE)
                && player.getOffhandItem().getCount() == 1 && player.getInventory().getItem(8).isEmpty(), "book/apple/empty action slot are retained, never replaced");
        ItemStack wand = player.getInventory().getItem(7);
        require(wand.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(wand).rod().equals("greatwood") && ItemWandCasting.data(wand).cap().equals("gold")
                && FocusData.get(wand).isEmpty() && count(player, ThaumcraftContent.WAND.get()) == 1, "reuse the actual unfocused greatwood/gold wand, keep the returned Fire Focus separate");
        for (Item item : List.of(stone(), bricks(), tallow(), Items.STRING, Items.ENDER_PEARL, Items.COAL, Items.GOLD_NUGGET,
                ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem(), shard("balanced"), matrixItem(), pedestalItem(), candleItem(), rod(), ThaumcraftWands.GOLD_WAND_CAP.get())) {
            require(count(player, item) == 0, "refuse to consume a retained stack as a new fixture ingredient/output: " + itemId(item));
        }
        for (String tag : PRIMALS) require(count(player, shard(tag)) == 0, "new shard inputs must not consume a prior retained shard stack");
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty() && stack != wand) retainedStacks.add(stack.copy());
        offhandBefore = player.getOffhandItem().copy();
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        topUp(player, "before native matrix/pedestals");
        supply(player, stone(), 43, "4matrix +35for five pedestal crafts +4raw pillar tops");
        supply(player, bricks(), 4, "raw altar blueprint bases, native placement");
        supply(player, shard("aer"), 4, "native matrix INPUT shards, separate from48distillation shards");
        supply(player, Items.ENDER_PEARL, 1, "native matrix INPUT");
        supply(player, tallow(), 48, "24ordinary candle crafts INPUT");
        supply(player, Items.STRING, 24, "24ordinary candle crafts INPUT");
        for (String tag : PRIMALS) {
            supply(player, shard(tag), 8, "finite distillation INPUT, not one of seven infusion components");
            var aspects = ObjectAspectResolver.get(new ItemStack(shard(tag)), level);
            require(aspects.visSize() == 4 && aspects.getAmount(Aspect.getAspect(tag)) == 2 && aspects.getAmount(Aspect.MAGIC) == 1
                    && aspects.getAmount(Aspect.CRYSTAL) == 1, "actual native primal shard aspect resolver establishes2primal+1magic+1vitreus budget");
        }
        supply(player, Items.COAL, 8, "finite normal furnace fuel INPUT supplied once; unburned remainder reported");
        for (String key : List.of("INFUSION", "ROD_silverwood", "NODEPRESERVE")) KnowledgeAccess.get(player).addResearch(key);
        KnowledgeEvents.sync(player);
        player.getInventory().selected = 8;
        player.inventoryMenu.broadcastChanges();
        player.teleportTo(workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6);
        report.put("empty_machine_fixtures", Map.of("arcane_workbench", 1, "alchemy_furnace", 1, "unlabelled_alembics", 3, "unlabelled_jars", 8,
                "infusion_output_blocks_supplied", 0, "research_inputs", List.of("INFUSION", "ROD_silverwood", "NODEPRESERVE")));
    }

    private static void patch(ServerLevel level, BlockPos min, BlockPos max) {
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        require(level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max.offset(1, 1, 1)))).isEmpty(), "fresh bounded patch cannot disturb another unit's physical outputs");
        for (BlockPos at : BlockPos.betweenClosed(min, max)) {
            var tile = level.getBlockEntity(at);
            require(tile == null || tile instanceof TileArcaneWorkbench table && table.isEmpty()
                    || tile instanceof TileAlchemyFurnace oven && oven.isEmpty() && oven.getVis() == 0
                    || tile instanceof TileAlembic alembic && alembic.getAmount() == 0 && alembic.getFilter() == null
                    || tile instanceof TileJar jar && jar.getAmount() == 0 && jar.getFilter() == null,
                    "fixture preparation must not erase retained contents, formed altars or prior nodes: " + at);
        }
        for (BlockPos at : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(at, at.getY() == min.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
    }

    private void supply(ServerPlayer player, Item item, int amount, String purpose) {
        int slot = -1;
        for (int i = 9; i < 36; i++) if (player.getInventory().getItem(i).isEmpty()) { slot = i; break; }
        require(slot >= 0 && amount > 0 && amount <= item.getDefaultInstance().getMaxStackSize(), "declared input requires a genuinely empty inventory slot: " + item);
        player.getInventory().setItem(slot, new ItemStack(item, amount));
        suppliedInputs.add(Map.of("item", itemId(item), "count", amount, "purpose", purpose, "empty_inventory_destination", slot));
    }

    private void topUp(ServerPlayer player, String purpose) {
        ItemStack actual = player.getInventory().getItem(7);
        require(actual.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(actual).rod().equals("greatwood") && ItemWandCasting.data(actual).cap().equals("gold")
                && FocusData.get(actual).isEmpty(), "only the actual retained input wand may receive a declared vis top-up");
        var before = ItemWandCasting.data(actual);
        var after = before;
        var added = new LinkedHashMap<String, Integer>();
        for (String tag : PRIMALS) { added.put(tag, 5000 - before.amount(tag)); after = after.withAmount(tag, 5000); }
        actual.set(ThaumcraftContent.WAND_DATA.get(), after);
        expectedOldWand = actual.copy();
        topUps.add(Map.of("purpose", purpose, "before_centivis", before.vis(), "after_centivis", after.vis(), "added_centivis", added));
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
    }

    private void beginCraft(Minecraft client, CraftPlan plan, Step following) {
        require(serverDone(), "recipe camera cannot overlap a server observation");
        client.setScreen(null);
        craft = plan; afterCraft = following; crafted = 0;
        onServer(client, () -> serverPlayer(client).teleportTo(workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6));
        next(Step.CRAFT_TRAVEL);
    }

    /** Literal menu clicks for this story, not an alternate crafting implementation. */
    private List<int[]> recipeClicks(Minecraft client) {
        var clicks = new ArrayList<int[]>();
        var cells = new LinkedHashMap<Item, List<Integer>>();
        for (int i = 0; i < 9; i++) if (craft.grid()[i] != null) cells.computeIfAbsent(craft.grid()[i], ignored -> new ArrayList<>()).add(i + 2);
        for (var entry : cells.entrySet()) {
            int needed = entry.getValue().size() * craft.repetitions();
            int inventory = itemSlot(client.player, stack -> stack.is(entry.getKey()) && stack.getCount() >= needed);
            int source = arcaneSlot(inventory);
            clicks.add(new int[] {source, 0});
            for (int cell : entry.getValue()) for (int n = 0; n < craft.repetitions(); n++) clicks.add(new int[] {cell, 1});
            if (client.player.getInventory().getItem(inventory).getCount() > needed) clicks.add(new int[] {source, 0});
        }
        return clicks;
    }

    private void verifyGrid(ContainerArcaneWorkbench menu, int amount) {
        for (int i = 0; i < 9; i++) {
            Item expected = craft.grid()[i]; ItemStack actual = menu.getSlot(i + 2).getItem();
            require(expected == null || amount == 0 ? actual.isEmpty() : actual.is(expected) && actual.getCount() == amount,
                    "native recipe grid cell " + i + " after " + crafted + " crafts of " + craft.recipe());
        }
    }

    private void inspectBatch(ServerPlayer player) {
        var level = player.serverLevel(); var typed = new LinkedHashMap<String, Integer>();
        require(oven(level).getItem(0).isEmpty() && oven(level).getVis() == 0 && !oven(level).isSpeedBoost(), "normal native furnace fully distilled the finite batch with no injected buffer/boost");
        for (int depth = 1; depth <= 3; depth++) {
            var tile = alembic(level, depth);
            require(tile.getFilter() == null && tile.getAspect() != null && tile.getAmount() > 0 && tile.getAmount() <= 32, "three genuine unlabelled typed alembics");
            typed.merge(tile.getAspect().getTag(), tile.getAmount(), Integer::sum);
        }
        require(typed.equals(Map.of(PRIMALS.get(batch), 16, "praecantatio", 8, "vitreus", 8)) && storedTotal(player).equals(produced(batch + 1)),
                "native original eight-shard batch yields16primal+8magic+8vitreus without loss or type substitution");
        snapshot = Map.of("server_verified", true, "batch", batch + 1, "input_item", itemId(shard(PRIMALS.get(batch))), "raw_inputs_consumed", 8,
                "native_elapsed_server_ticks", level.getGameTime() - batchStarted, "actual_typed_alembics", typed, "cumulative_exact_essentia", produced(batch + 1));
    }

    /** Passive actual END_SERVER_TICK and ADD observations; never invoke or replace a gameplay tick. */
    private void startObservation(ServerPlayer player) {
        require(nativeTicks == null && nativeAdds == null && serverStopped == null, "one scoped infusion observation only");
        observationStarted = player.serverLevel().getGameTime();
        nativeAdds = (entity, level) -> {
            if (level == player.level() && ejectionArmed && entity instanceof ItemEntity item && item.getItem().is(rod())
                    && item.distanceToSqr(player) < 16) {
                dropAttempts++;
                coreDropId = item.getUUID();
                coreDropStack = item.getItem().copy();
            }
            return EventResult.pass();
        };
        nativeTicks = server -> {
            if (server != player.server) return;
            try {
                require(!player.isRemoved() && player.level() instanceof ServerLevel && player.serverLevel().getGameTime() - observationStarted <= 2400,
                        "scoped infusion observer expired or its player/world left");
                observeNativeTick(player);
            } catch (RuntimeException | Error error) {
                report.put("native_observer_failure", Map.of("message", String.valueOf(error.getMessage()),
                        "observed_ticks", observedTicks, "core_drop_attempts", dropAttempts,
                        "actual_core_inventory_count", count(player, rod()), "actual_center_empty", player.level().hasChunkAt(center)
                                && player.level().getBlockEntity(center) instanceof TilePedestal pedestal && pedestal.isEmpty(),
                        "player_removed", player.isRemoved()));
                observationFailure = error;
                stopObservation();
            }
        };
        serverStopped = server -> { if (server == player.server) stopObservation(); };
        EntityEvent.ADD.register(nativeAdds);
        TickEvent.SERVER_POST.register(nativeTicks);
        LifecycleEvent.SERVER_STOPPED.register(serverStopped);
    }

    private void observeNativeTick(ServerPlayer player) {
        var level = player.serverLevel(); var matrix = matrix(level);
        if (!observedStart) {
            if (!matrix.isCrafting()) return;
            observedStart = true;
            require(matrix.getRecipeInstability() == 5 && matrix.getSymmetry() == -6 && matrix.getInstability() == -1
                    && matrix.getPedestalPositions().size() == 8, "real72paired candles and seven-of-eight occupancy yield symmetry-6 and original I5-6=-1, with no forced state/seed");
            report.put("native_stabilization", Map.of("recipe_instability", 5, "ordinary_candles_crafted_and_placed", CANDLES,
                    "actual_symmetry", matrix.getSymmetry(), "actual_start_instability", matrix.getInstability(), "peripheral_pedestals", 8,
                    "occupied_components", 7, "extra_empty_balancing_pedestal", peripheral.get(7).toShortString(), "instability_or_rng_writes", 0));
        }
        ++observedTicks;
        int demand = matrix.getDemand().visSize();
        int delta = lastDemand - demand;
        require(delta == 0 || delta == 1, "each actual native server tick pays zero or ONE essentia, never a bulk simulated drain");
        if (delta == 1) {
            require(matrix.getTickCount() % 10 == 0 && (lastPaymentTime < 0 || level.getGameTime() - lastPaymentTime == 10), "actual one-unit drains follow original ten-tick cadence");
            lastPaymentTime = level.getGameTime();
            paidUnits++;
            payments.add(Map.of("unit", paidUnits, "server_game_time", lastPaymentTime, "matrix_native_tick", matrix.getTickCount(), "remaining_demand", demand));
        }
        lastDemand = demand;
        var actual = storedTotal(player);
        var expected = new LinkedHashMap<String, Integer>();
        for (String tag : JAR_TAGS) {
            int paid = cost().containsKey(tag) ? 9 - matrix.getDemand().getAmount(Aspect.getAspect(tag)) : 0;
            expected.put(tag, produced(6).get(tag) - paid);
        }
        require(actual.equals(expected) && actual.values().stream().mapToInt(Integer::intValue).sum() == 192 - paidUnits,
                "every real server tick conserves each of eight types across furnace/alembics/jars/real phials plus paid demand");
        require(ItemStack.matches(player.getInventory().getItem(7), expectedOldWand), "native infusion start/cycles cost no additional wand vis");
        int remaining = matrix.getRemainingIngredientCount();
        int ingredientsDelta = lastIngredients - remaining;
        require(ingredientsDelta == 0 || ingredientsDelta == 1, "native ingredient consumption is one real pedestal at a time");
        if (ingredientsDelta == 1) {
            require(paidUnits == 63 && demand == 0 && matrix.getTickCount() % 10 == 0, "all63real units are paid before any of seven native component consumptions");
            if (firstIngredientTime < 0) {
                firstIngredientTime = level.getGameTime();
                require(firstIngredientTime - lastPaymentTime == 60, "first ingredient uses six real ten-tick cycles after the last essentia payment");
            } else require(level.getGameTime() - ((Number) ingredientPayments.getLast().get("server_game_time")).longValue() == 60,
                    "each later ingredient also requires six actual ten-tick cycles");
            paidIngredients++;
            ingredientPayments.add(Map.of("component", paidIngredients, "server_game_time", level.getGameTime(), "matrix_native_tick", matrix.getTickCount(), "remaining_components", remaining));
        }
        lastIngredients = remaining;
        long occupied = peripheral.stream().filter(at -> !pedestal(level, at).isEmpty()).count();
        require(occupied == remaining, "native session and physical seven-component pedestal multiset decrease together");
        if (matrix.isCrafting()) {
            require(matrix.getInstability() == -1 && pedestal(level, center).getItem(0).is(ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem()),
                    "genuine fully funded stabilized craft keeps its original central log until completion");
        } else {
            require(paidUnits == 63 && paidIngredients == 7 && demand == 0 && (ejectionArmed || pedestal(level, center).getItem(0).is(rod())),
                    "native session finishes only after all payments, with the actual core on its pedestal");
            finishedCraft = true;
        }
    }

    private void stopObservation() {
        if (nativeTicks != null) {
            TickEvent.SERVER_POST.unregister(nativeTicks);
            require(!TickEvent.SERVER_POST.isRegistered(nativeTicks), "scoped native infusion tick observer removed");
            nativeTicks = null;
        }
        if (nativeAdds != null) {
            EntityEvent.ADD.unregister(nativeAdds);
            require(!EntityEvent.ADD.isRegistered(nativeAdds), "scoped native core-drop observer removed");
            nativeAdds = null;
        }
        if (serverStopped != null) {
            LifecycleEvent.SERVER_STOPPED.unregister(serverStopped);
            require(!LifecycleEvent.SERVER_STOPPED.isRegistered(serverStopped), "scoped infusion shutdown cleanup removed");
            serverStopped = null;
        }
    }

    private Map<String, Integer> storedTotal(Player player) {
        Level level = player.level(); var total = new LinkedHashMap<String, Integer>();
        var furnaceAspects = oven(level).getAspects();
        for (Aspect type : furnaceAspects.getAspects()) merge(total, type, furnaceAspects.getAmount(type));
        for (int depth = 1; depth <= 3; depth++) merge(total, alembic(level, depth).getAspect(), alembic(level, depth).getAmount());
        for (BlockPos at : jars.values()) merge(total, jar(level, at).getAspect(), jar(level, at).getAmount());
        for (ItemStack stack : player.getInventory().items) if (stack.is(ThaumcraftContent.FILLED_PHIAL.get())) {
            require(ItemEssence.data(stack) != null, "real filled phial must preserve its typed native component");
            merge(total, Aspect.getAspect(ItemEssence.data(stack).tag()), stack.getCount() * PhialData.AMOUNT);
        }
        return total;
    }

    private static void merge(Map<String, Integer> total, Aspect aspect, int amount) {
        require(amount >= 0 && (amount == 0 || aspect != null), "physical essentia storage has a nonnegative typed amount");
        if (amount > 0) total.merge(aspect.getTag(), amount, Integer::sum);
    }

    private static Map<String, Integer> produced(int batches) {
        var result = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < batches; i++) result.put(PRIMALS.get(i), 16);
        if (batches > 0) { result.put("praecantatio", batches * 8); result.put("vitreus", batches * 8); }
        return result;
    }
    private static Map<String, Integer> cost() {
        var result = new LinkedHashMap<String, Integer>(); for (String tag : PRIMALS) result.put(tag, 9); result.put("praecantatio", 9); return result;
    }
    private static Map<String, Integer> costVis(int amount) {
        var result = new LinkedHashMap<String, Integer>(); for (String tag : PRIMALS) result.put(tag, amount); return result;
    }
    private static Map<String, Integer> leftovers() {
        var result = new LinkedHashMap<>(produced(6)); cost().forEach((tag, amount) -> result.put(tag, result.get(tag) - amount)); return result;
    }

    private void retained(Player player) {
        require(ItemStack.matches(player.getInventory().getItem(7), expectedOldWand) && FocusData.get(player.getInventory().getItem(7)).isEmpty()
                && ItemStack.matches(player.getOffhandItem(), offhandBefore), "actual paid old wand/unattached focus state and exact original offhand survive");
        for (ItemStack original : retainedStacks) {
            int before = retainedStacks.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, original)).mapToInt(ItemStack::getCount).sum();
            int after = player.getInventory().items.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, original)).mapToInt(ItemStack::getCount).sum();
            require(after >= before, "retained actual item/count/components were not overwritten: " + original);
        }
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0, "all four reused physical phials return empty at fixture boundaries");
    }

    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page");
        var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) {
            Object value = BookClientSmoke.field(pages.get(i), "recipe");
            if (value instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id("infusion/wand_rod_silverwood"))) {
                require(display.recipe() == client.level.getRecipeManager().byKey(id("infusion/wand_rod_silverwood")).orElseThrow(), "native book uses the real synced infusion RecipeHolder");
                return true;
            }
        }
        require(++bookTurns <= 24, "bounded native Silverwood recipe page navigation");
        var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(widget -> widget.active && widget.getMessage().getString().equals("→")).findFirst().orElseThrow();
        client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
        client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
        ticks = 0;
        return false;
    }

    private Map<String, Object> presentation(Minecraft client) {
        ItemStack actual = client.player.getMainHandItem();
        require(isReward(actual), "presentation only inspects the actual native reward");
        var model = client.getItemRenderer().getModel(actual, client.level, client.player, 0);
        require(model != client.getModelManager().getMissingModel(), "actual Silverwood/gold component-driven model exists");
        var textures = new LinkedHashSet<String>();
        for (var quad : model.getQuads(null, null, RandomSource.create(0))) textures.add(quad.getSprite().contents().name().toString());
        for (Direction face : Direction.values()) for (var quad : model.getQuads(null, face, RandomSource.create(0))) textures.add(quad.getSprite().contents().name().toString());
        report.put("native_reward_model_observation", Map.of("actual_sprites", List.copyOf(textures), "rod", ItemWandCasting.data(actual).rod(), "cap", ItemWandCasting.data(actual).cap()));
        require(textures.contains("thaumcraft:models/wand_rod_silverwood") && textures.contains("thaumcraft:models/wand_cap_gold"), "held reward selects original Silverwood rod and gold cap sprites, not a fallback");
        return Map.of("actual_model_sprites", List.copyOf(textures), "tooltip", actual.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
                .stream().map(Component::getString).toList(), "capacity_centivis", ItemWandCasting.getMaxVis(actual));
    }

    private boolean hand(Minecraft client, Predicate<ItemStack> wanted) throws ReflectiveOperationException {
        require(client.player.containerMenu == client.player.inventoryMenu, "native hand swaps occur only outside machine menus");
        if (!wanted.test(client.player.getInventory().getItem(8))) {
            int source = itemSlot(client.player, wanted);
            require(source != 7 && source != 6, "scripted action hand cannot displace the retained old wand/book");
            click(client, inventorySlot(source), 8, ClickType.SWAP);
            handChangedAt = client.player.tickCount;
            return false;
        }
        if (client.player.tickCount - handChangedAt < 6) return false;
        select(client, 8);
        return true;
    }

    private BlockPos offerPosition() { return offerIndex == 0 ? center : peripheral.get(offerIndex - 1); }
    private Item offerItem() { return offerIndex == 0 ? ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem() : shard(offerIndex == 1 ? "balanced" : PRIMALS.get(offerIndex - 2)); }
    private static boolean filledWith(ItemStack stack, String tag) { return stack.is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(stack) != null && ItemEssence.data(stack).tag().equals(tag); }
    private static boolean isReward(ItemStack stack) { return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals("silverwood") && ItemWandCasting.data(stack).cap().equals("gold") && FocusData.get(stack).isEmpty(); }
    private static int outputDestination(Player player, ItemStack output) {
        for (int slot = 9; slot < 36; slot++) {
            ItemStack at = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(at, output) && at.getCount() + output.getCount() <= at.getMaxStackSize()) return slot;
        }
        for (int slot = 9; slot < 36; slot++) if (player.getInventory().getItem(slot).isEmpty()) return slot;
        throw new IllegalStateException("No genuinely empty/compatible native output destination");
    }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException {
        client.player.getInventory().selected = slot;
        var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem");
        ensure.setAccessible(true); ensure.invoke(client.gameMode);
    }
    private static void use(Minecraft client, BlockPos pos, Direction face, Vec3 point) {
        require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(pos, 0), "native survival reach for " + pos + "/" + face);
        lookAt(client, point);
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)).consumesAction(), "native useItemOn accepted at " + pos + "/" + face);
    }
    private static void lookAt(Minecraft client, Vec3 point) {
        Vec3 delta = point.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot();
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot();
    }
    private static Vec3 southPoint(BlockPos at) { return Vec3.atCenterOf(at).add(0, 0, .5); }
    private static Vec3 pedestalPoint(BlockPos at) { return Vec3.atCenterOf(at).add(0, 0, .25); }
    private static int arcaneSlot(int inventory) { return inventory < 9 ? inventory + 38 : inventory + 2; }
    private static int furnaceSlot(int inventory) { return inventory < 9 ? inventory + 29 : inventory - 7; }
    private static int inventorySlot(int inventory) { return inventory < 9 ? inventory + 36 : inventory; }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int itemSlot(Player player, Predicate<ItemStack> wanted) { for (int slot = 0; slot < 36; slot++) if (wanted.test(player.getInventory().getItem(slot))) return slot; throw new IllegalStateException("Native infusion inventory lacks required item at this local stage"); }
    private static Item stone() { return ThaumcraftMasonry.ARCANE_STONE.get().asItem(); }
    private static Item bricks() { return ThaumcraftMasonry.ARCANE_STONE_BRICKS.get().asItem(); }
    private static Item shard(String tag) { return ThaumcraftContent.SHARDS.get(tag).get(); }
    private static Item tallow() { return ThaumcraftContent.MATERIALS.get("magic_tallow").get(); }
    private static Item matrixItem() { return ThaumcraftInfusion.MATRIX_ITEM.get(); }
    private static Item pedestalItem() { return ThaumcraftInfusion.PEDESTAL_ITEM.get(); }
    private static Item candleItem() { return ThaumcraftInfusion.TALLOW_CANDLE_ITEM.get(); }
    private static Item rod() { return ThaumcraftWands.SILVERWOOD_WAND_ROD.get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private TileInfusionMatrix matrix(Level level) { return (TileInfusionMatrix) level.getBlockEntity(matrixPos); }
    private TileAlchemyFurnace oven(Level level) { return (TileAlchemyFurnace) level.getBlockEntity(furnace); }
    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(furnace.above(depth)); }
    private static TileJar jar(Level level, BlockPos at) { return (TileJar) level.getBlockEntity(at); }
    private static TilePedestal pedestal(Level level, BlockPos at) { return (TilePedestal) level.getBlockEntity(at); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ContainerAlchemyFurnace furnaceMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "infusion server observations/fixtures never overlap"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
