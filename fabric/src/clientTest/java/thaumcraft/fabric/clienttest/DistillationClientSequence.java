package thaumcraft.fabric.clienttest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
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
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.PhialData;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileBellows;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileNode;

/** Supplied inputs/machines/trees/camera; all distillation, phials and wand output use native actions. */
final class DistillationClientSequence {
    private enum Step {
        SETUP, FURNACE_OPEN, FUEL_INSERT, INPUT_INSERT, FURNACE_PROGRESS,
        DISTILL, DISTILL_VERIFY, DISTILL_WORLD, WITHDRAW_PREPARE, WITHDRAW, WITHDRAW_WAIT, WITHDRAW_VERIFY,
        JAR_PREPARE, JAR_INSERT, JAR_WAIT, JAR_VERIFY, JAR_WORLD, DISTILL_BOOK, DISTILL_BOOK_PAGE,
        TREE_SETUP, TREE_WORLD, WAND_SETUP, WAND_OPEN, WAND_FUND, WAND_CAP_PICKUP,
        WAND_CAP_TOP, WAND_CAP_BOTTOM, WAND_ROD, WAND_READY, WAND_PRICED, WAND_CRAFT, WAND_CRAFTED, WAND_STORED, WAND_VERIFY,
        WAND_CHARGE_POSITION, WAND_CHARGE_START, WAND_CHARGE, WAND_CHARGED, WAND_WORLD,
        BROWSER_OPEN, BROWSER_CATEGORY, BROWSER_PAN, BROWSER_SCENE, ROD_OPEN, ROD_PAGE, DONE
    }

    final Map<String, Object> evidence = new LinkedHashMap<>();
    private final BlockPos furnace, fireJar, energyJar, grove, greatwood, silverwood, workbench, node;
    private Step step = Step.SETUP;
    private int ticks, transfer, bookTurns;
    private float initialInflation = Float.NaN;
    private CompletableFuture<Void> serverAction;
    private Map<String, Object> serverSnapshot = Map.of();
    private WandData chargedOutput;

    DistillationClientSequence(BlockPos research) {
        furnace = research.offset(18, 0, 0);
        fireJar = furnace.offset(2, 0, 1);
        energyJar = furnace.offset(3, 0, 1);
        grove = research.offset(52, 0, 8);
        greatwood = grove.offset(-10, 0, -5);
        silverwood = grove.offset(10, 0, -5);
        workbench = grove.offset(0, 0, 17);
        node = workbench.offset(2, 2, 0);
        evidence.put("boundary", "Supplied empty machines, 1 Alumentum, 8 coal, 4 empty phials, book, research, two gold caps, greatwood rod, charged wood/iron crafting wand and a normal-node charging fixture. Server teleports/flat patches are camera fixtures, not survival progression. No furnace/alembic/jar essentia or crafted wand output is injected.");
        evidence.put("tree_boundary", "Fixed native sapling-growth seeds (greatwood 0, silverwood 7) on a supplied flat patch, not natural chunk frequency, PURE-knot ecology, shimmerleaf or spider-dungeon evidence.");
        evidence.put("not_covered", List.of("jar labels", "full survival material/research acquisition", "natural ore/tree/node distribution", "other wand materials/foci/staves"));
    }

    boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        ticks++;
        // Server distillation is deliberately allowed to run normally, even if
        // server/client tick rates diverge. The enclosing smoke retains its wall-clock bound.
        if (ticks > 1800) throw new IllegalStateException("Distillation client step timed out: " + step);
        switch (step) {
            case SETUP -> {
                client.setScreen(null);
                client.options.keyUse.setDown(false);
                client.options.keyShift.setDown(false);
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var level = player.serverLevel();
                    for (int x = -3; x <= 5; x++) for (int z = -2; z <= 6; z++) {
                        level.setBlockAndUpdate(furnace.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                        for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(furnace.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    level.setBlockAndUpdate(furnace, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
                    for (int depth = 1; depth <= 2; depth++) level.setBlockAndUpdate(furnace.above(depth),
                            ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.SOUTH));
                    level.setBlockAndUpdate(furnace.west(), ThaumcraftBellows.BELLOWS.get().defaultBlockState().setValue(BlockBellows.FACING, Direction.EAST));
                    level.setBlockAndUpdate(fireJar, ThaumcraftContent.JAR.get().defaultBlockState());
                    level.setBlockAndUpdate(energyJar, ThaumcraftContent.JAR.get().defaultBlockState());
                    player.setGameMode(GameType.SURVIVAL);
                    player.setShiftKeyDown(false);
                    player.setHealth(20);
                    player.getFoodData().setFoodLevel(20);
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(ThaumcraftContent.MATERIALS.get("alumentum").get()));
                    player.getInventory().setItem(1, new ItemStack(Items.COAL, 8));
                    player.getInventory().setItem(2, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), 4));
                    player.getInventory().setItem(6, new ItemStack(ThaumcraftContent.THAUMONOMICON.get()));
                    player.getInventory().selected = 8;
                    for (String key : List.of("DISTILESSENTIA", "BASICTHAUMATURGY", "CAP_gold", "ROD_greatwood")) KnowledgeAccess.get(player).addResearch(key);
                    KnowledgeEvents.sync(player);
                    player.inventoryMenu.broadcastChanges();
                    player.teleportTo(furnace.getX() + .5, furnace.getY(), furnace.getZ() + 2.6);
                    var tile = (TileAlchemyFurnace) level.getBlockEntity(furnace);
                    require(tile.getContainerSize() == 2 && tile.isEmpty() && tile.getVis() == 0, "supplied furnace starts with two empty real slots and no essentia");
                    for (int depth = 1; depth <= 2; depth++) require(alembic(level, depth).getAmount() == 0 && alembic(level, depth).getFilter() == null, "both supplied alembics start empty and unlabelled");
                    require(jar(level, fireJar).getAmount() == 0 && jar(level, energyJar).getAmount() == 0, "supplied jars are empty");
                });
                next(Step.FURNACE_OPEN);
            }
            case FURNACE_OPEN -> {
                if (!serverDone() || ticks < 15 || !client.player.getInventory().getItem(1).is(Items.COAL)
                        || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 4
                        || !(client.level.getBlockEntity(furnace.above(2)) instanceof TileAlembic)) return false;
                client.player.getInventory().selected = 8;
                useBlock(client, furnace);
                next(Step.FUEL_INSERT);
            }
            case FUEL_INSERT -> {
                if (!(client.screen instanceof GuiAlchemyFurnace) || ticks < 10) return false;
                var menu = furnaceMenu(client);
                require(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "native furnace menu has no fabricated result slot");
                click(client, 29, 0, ClickType.QUICK_MOVE); // Furnace hotbar0 -> actual fuel slot.
                next(Step.INPUT_INSERT);
            }
            case INPUT_INSERT -> {
                var menu = furnaceMenu(client);
                if (ticks < 8 || !menu.getSlot(1).getItem().is(ThaumcraftContent.MATERIALS.get("alumentum").get())
                        || !client.player.getInventory().getItem(0).isEmpty()) return false;
                // Coal is also fuel: its quick-move priority is NOT the input.
                // A real native hotbar swap explicitly selects input slot0.
                click(client, 0, 1, ClickType.SWAP);
                next(Step.FURNACE_PROGRESS);
            }
            case FURNACE_PROGRESS -> {
                var menu = furnaceMenu(client);
                if (!menu.isBurning() || !menu.isSpeedBoost() || menu.getCookTime() < 4 || !menu.getSlot(0).getItem().is(Items.COAL)) return false;
                require(menu.getCurrentItemBurnTime() == 6400 && menu.getBellowsCount() == 1 && menu.getSmeltTime() == 35,
                        "one native Alumentum fuel burns6400; one correctly oriented bellows prices coal at35 ticks");
                evidence.put("native_furnace_menu", Map.of("input_supplied", 8, "input_remaining_at_capture", menu.getSlot(0).getItem().getCount(),
                        "fuel_supplied", "thaumcraft:alumentum x1", "fuel_duration", menu.getCurrentItemBurnTime(), "cook_time_at_capture", menu.getCookTime(),
                        "smelt_time", menu.getSmeltTime(), "bellows", menu.getBellowsCount(), "real_slots", 2, "input_action", "C2S native SWAP", "fuel_action", "C2S native QUICK_MOVE"));
                next(Step.DISTILL);
                capture.accept("33-alchemy-furnace-native-fuel-and-coal-progress-en.png");
            }
            case DISTILL -> {
                var menu = furnaceMenu(client);
                var lower = alembic(client.level, 1);
                var upper = alembic(client.level, 2);
                if (!menu.getSlot(0).getItem().isEmpty() || menu.getVis() != 0 || lower.getAmount() != 16 || upper.getAmount() != 16) return false;
                require(lower.getAspect() != null && upper.getAspect() != null && Set.of(lower.getAspect(), upper.getAspect()).equals(Set.of(Aspect.FIRE, Aspect.ENERGY)),
                        "unfiltered native furnace output separates all16ignis and16potentia into two different real alembics");
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var level = player.serverLevel();
                    verifyEmptyFurnace(player);
                    var amounts = new LinkedHashMap<String, Integer>();
                    for (int depth = 1; depth <= 2; depth++) {
                        var tile = alembic(level, depth);
                        require(tile.getAmount() == 16 && tile.getMaxAmount() == 32 && tile.getFilter() == null, "server alembic has16/32 without a filter fixture");
                        amounts.merge(tile.getAspect().getTag(), tile.getAmount(), Integer::sum);
                    }
                    require(amounts.equals(Map.of("ignis", 16, "potentia", 16)), "server retains exactly the coal's original32 essentia");
                    require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0, "no phial output supplied before extraction");
                    serverSnapshot = Map.of("server_verified", true, "coal_consumed", 8, "alumentum_consumed", 1, "furnace_buffer_after", 0,
                            "alembics", amounts, "each_alembic_capacity", 32, "furnace_capacity", 50, "speed_boost", true, "furnace", furnace.toShortString());
                });
                next(Step.DISTILL_VERIFY);
            }
            case DISTILL_VERIFY -> {
                if (!serverDone()) return false;
                evidence.put("native_distillation", serverSnapshot);
                client.player.closeContainer();
                machineCamera(client);
                next(Step.DISTILL_WORLD);
            }
            case DISTILL_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                var bellows = (TileBellows) client.level.getBlockEntity(furnace.west());
                if (Float.isNaN(initialInflation)) { initialInflation = bellows.getInflation(); return false; }
                if (Math.abs(initialInflation - bellows.getInflation()) < .001F) return false;
                require(!bellows.isPowered() && bellows.getFacing() == Direction.EAST && client.level.getBlockState(furnace).getValue(BlockAlchemyFurnace.LIT),
                        "native animated unpowered bellows points into the still-burning furnace");
                evidence.put("bellows_animation", Map.of("inflation_before", initialInflation, "inflation_after", bellows.getInflation(), "facing", "east", "powered", false));
                lookAt(client, Vec3.atCenterOf(furnace).add(.9, .65, .2));
                next(Step.WITHDRAW_PREPARE);
                capture.accept("34-distilled-alembics-burning-furnace-and-bellows-world-en.png");
            }
            case WITHDRAW_PREPARE -> {
                onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() + .5, furnace.getY(), furnace.getZ() + 2.6));
                transfer = 0;
                next(Step.WITHDRAW);
            }
            case WITHDRAW -> {
                if (!serverDone() || ticks < 10) return false;
                client.player.getInventory().selected = 2;
                require(client.player.getMainHandItem().is(ThaumcraftContent.EMPTY_PHIAL.get()) && !client.player.isShiftKeyDown(), "withdrawal uses a real held empty phial without crouching");
                useBlock(client, furnace.above(1 + transfer / 2));
                next(Step.WITHDRAW_WAIT);
            }
            case WITHDRAW_WAIT -> {
                if (ticks < 8 || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != 3 - transfer
                        || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != transfer + 1
                        || alembic(client.level, 1).getAmount() != 16 - Math.min(transfer + 1, 2) * PhialData.AMOUNT
                        || alembic(client.level, 2).getAmount() != 16 - Math.max(0, transfer - 1) * PhialData.AMOUNT) return false;
                if (++transfer < 4) { next(Step.WITHDRAW); return false; }
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    verifyEmptyFurnace(player);
                    requireEmptyAlembics(player.serverLevel());
                    require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 0 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 4
                            && filledCount(player, "ignis") == 2 && filledCount(player, "potentia") == 2, "four native withdrawals preserve32 essentia as four eight-unit component phials");
                    serverSnapshot = Map.of("server_verified", true, "empty_phials_after", 0, "filled_phials", Map.of("ignis", 2, "potentia", 2),
                            "each_phial_amount", PhialData.AMOUNT, "total_essentia", 32, "alembics_after", 0, "action", "four native useItemOn packets");
                });
                next(Step.WITHDRAW_VERIFY);
            }
            case WITHDRAW_VERIFY -> {
                if (!serverDone()) return false;
                evidence.put("native_phial_withdrawal", serverSnapshot);
                onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() + 2.5, furnace.getY(), furnace.getZ() + 3.5));
                transfer = 0;
                next(Step.JAR_PREPARE);
            }
            case JAR_PREPARE -> {
                if (!serverDone() || ticks < 15) return false;
                next(Step.JAR_INSERT);
            }
            case JAR_INSERT -> {
                if (ticks < 6) return false;
                String tag = transfer < 2 ? "ignis" : "potentia";
                int slot = filledSlot(client.player, tag);
                require(slot >= 0, "the held filled phial must be one actually extracted from an alembic");
                client.player.getInventory().selected = slot;
                useBlock(client, transfer < 2 ? fireJar : energyJar);
                next(Step.JAR_WAIT);
            }
            case JAR_WAIT -> {
                BlockPos pos = transfer < 2 ? fireJar : energyJar;
                if (ticks < 8 || jar(client.level, pos).getAmount() != (transfer % 2 + 1) * PhialData.AMOUNT
                        || count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) != transfer + 1
                        || count(client.player, ThaumcraftContent.FILLED_PHIAL.get()) != 3 - transfer) return false;
                require(jar(client.level, pos).getAspect() == (transfer < 2 ? Aspect.FIRE : Aspect.ENERGY), "native jar insertion preserves the actual phial component's aspect");
                if (++transfer < 4) { next(Step.JAR_INSERT); return false; }
                onServer(client, () -> serverSnapshot = verifyJars(serverPlayer(client)));
                next(Step.JAR_VERIFY);
            }
            case JAR_VERIFY -> {
                if (!serverDone()) return false;
                evidence.put("native_phial_to_jar", serverSnapshot);
                client.player.getInventory().selected = itemSlot(client.player, ThaumcraftContent.EMPTY_PHIAL.get());
                onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() + 3.8, furnace.getY(), furnace.getZ() + 5));
                next(Step.JAR_WORLD);
            }
            case JAR_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                require(count(client.player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && jar(client.level, fireJar).getAmount() == 16
                        && jar(client.level, energyJar).getAmount() == 16, "client receives two genuinely filled jars and all four returned empty phials");
                lookAt(client, Vec3.atCenterOf(furnace).add(1.2, .35, .5));
                next(Step.DISTILL_BOOK);
                capture.accept("35-native-phial-transfers-fill-two-jars-world-en.png");
            }
            case DISTILL_BOOK -> {
                openBook(client, "DISTILESSENTIA");
                next(Step.DISTILL_BOOK_PAGE);
            }
            case DISTILL_BOOK_PAGE -> {
                if (ticks < 10 || !showRecipe(client, "arcane/alchemy_furnace")) return false;
                evidence.put("native_distillation_book", Map.of("research", "DISTILESSENTIA", "recipe", "thaumcraft:arcane/alchemy_furnace", "page_index", BookClientSmoke.field(client.screen, "page")));
                next(Step.TREE_SETUP);
                capture.accept("36-distillation-native-furnace-recipe-book-en.png");
            }
            case TREE_SETUP -> {
                client.setScreen(null);
                onServer(client, () -> prepareGrove(serverPlayer(client)));
                next(Step.TREE_WORLD);
            }
            case TREE_WORLD -> {
                if (!serverDone() || ticks < 30 || !client.level.getBlockState(greatwood).is(ThaumcraftNaturals.GREATWOOD_LOG.get())
                        || !client.level.getBlockState(silverwood).is(ThaumcraftNaturals.SILVERWOOD_LOG.get())) return false;
                for (Block block : palette()) require(client.getItemRenderer().getModel(new ItemStack(block), client.level, client.player, 0) != client.getModelManager().getMissingModel(), "native tree/masonry palette item model is present");
                evidence.put("native_growth_and_masonry_fixture", serverSnapshot);
                client.player.getInventory().selected = 8;
                lookAt(client, Vec3.atCenterOf(grove).add(0, 9, -3));
                next(Step.WAND_SETUP);
                capture.accept("37-native-greatwood-silverwood-and-masonry-world-en.png");
            }
            case WAND_SETUP -> {
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var level = player.serverLevel();
                    require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4, "returned phials are retained, not cleared for the next fixture");
                    for (int slot : new int[] {3, 4, 5, 7, 8}) require(player.getInventory().getItem(slot).isEmpty(), "wand fixture never overwrites native phial outputs");
                    level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
                    level.setBlockAndUpdate(node, ThaumcraftContent.AURA_NODE.get().defaultBlockState());
                    // Supply only the crafting wand and ingredients, never its result.
                    var vis = new LinkedHashMap<String, Integer>();
                    for (String tag : WandData.PRIMALS) vis.put(tag, 2500);
                    var wand = new ItemStack(ThaumcraftContent.WAND.get());
                    wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("wood", "iron", vis));
                    player.getInventory().setItem(3, wand);
                    player.getInventory().setItem(4, new ItemStack(ThaumcraftWands.GOLD_WAND_CAP.get(), 2));
                    player.getInventory().setItem(5, new ItemStack(ThaumcraftWands.GREATWOOD_WAND_ROD.get()));
                    player.getInventory().selected = 8;
                    player.inventoryMenu.broadcastChanges();
                    player.teleportTo(workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6);
                    require(((TileArcaneWorkbench) level.getBlockEntity(workbench)).isEmpty(), "supplied workbench starts empty; no matrix/result injection");
                });
                next(Step.WAND_OPEN);
            }
            case WAND_OPEN -> {
                if (!serverDone() || ticks < 15 || !client.player.getInventory().getItem(4).is(ThaumcraftWands.GOLD_WAND_CAP.get())
                        || !client.player.getInventory().getItem(5).is(ThaumcraftWands.GREATWOOD_WAND_ROD.get())) return false;
                useBlock(client, workbench);
                next(Step.WAND_FUND);
            }
            case WAND_FUND -> {
                if (!(client.screen instanceof GuiArcaneWorkbench) || ticks < 10) return false;
                click(client, 41, 0, ClickType.QUICK_MOVE); // Arcane hotbar3 -> wand slot1.
                next(Step.WAND_CAP_PICKUP);
            }
            case WAND_CAP_PICKUP -> {
                if (ticks < 8 || !arcane(client).getSlot(1).getItem().is(ThaumcraftContent.WAND.get()) || !client.player.getInventory().getItem(3).isEmpty()) return false;
                click(client, 42, 0, ClickType.PICKUP); // Two actual supplied caps, not a recipe ghost.
                next(Step.WAND_CAP_TOP);
            }
            case WAND_CAP_TOP -> {
                if (ticks < 8 || arcane(client).getCarried().getCount() != 2) return false;
                require(arcane(client).getCarried().is(ThaumcraftWands.GOLD_WAND_CAP.get()), "native cursor holds the two supplied gold caps");
                click(client, 4, 1, ClickType.PICKUP); // Menu4 = matrix2, top-right.
                next(Step.WAND_CAP_BOTTOM);
            }
            case WAND_CAP_BOTTOM -> {
                if (ticks < 8 || arcane(client).getCarried().getCount() != 1 || arcane(client).getSlot(4).getItem().getCount() != 1) return false;
                click(client, 8, 0, ClickType.PICKUP); // Menu8 = matrix6, bottom-left.
                next(Step.WAND_ROD);
            }
            case WAND_ROD -> {
                if (ticks < 8 || !arcane(client).getCarried().isEmpty() || arcane(client).getSlot(8).getItem().getCount() != 1) return false;
                click(client, 6, 5, ClickType.SWAP); // Menu6 = matrix4, centre; hotbar5 rod.
                next(Step.WAND_READY);
            }
            case WAND_READY -> {
                var menu = arcane(client);
                if (ticks < 15 || !menu.craftable() || !isGreatwoodGold(menu.getSlot(0).getItem())) return false;
                for (String tag : WandData.PRIMALS) require(menu.requiredVis(tag) == 990 && ItemWandCasting.getVis(menu.getSlot(1).getItem(), tag) == 2500, "native dynamic assembly prices9base vis at iron's1.1 modifier =990centivis per primal");
                for (int slot = 2; slot <= 10; slot++) require(menu.getSlot(slot).getItem().getCount() == (slot == 4 || slot == 6 || slot == 8 ? 1 : 0), "native clicks populated only original cap2/rod4/cap6 matrix cells");
                require(count(client.player, ThaumcraftWands.GOLD_WAND_CAP.get()) == 0 && count(client.player, ThaumcraftWands.GREATWOOD_WAND_ROD.get()) == 0
                        && count(client.player, ThaumcraftContent.WAND.get()) == 0, "all supplied components and the crafting wand are physically in the workbench, not an output fixture");
                evidence.put("native_wand_assembly_menu", Map.of("recipe", "thaumcraft:arcane/wand_assembly", "matrix_slots", List.of(2, 4, 6), "base_each_primal", 9,
                        "crafting_wand", "wood/iron", "each_primal_before", 2500, "each_primal_price", 990, "inputs", "2 gold caps + 1 greatwood rod", "result_is_preview_only", true));
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var tile = (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench);
                    var match = ArcaneCrafting.find(player, tile);
                    require(match != null && match.arcane() && match.recipe().id().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_assembly"))
                            && isGreatwoodGold(match.output()), "server recipe-manager match is the real dynamic wand assembly, not a substituted preview");
                    for (String tag : WandData.PRIMALS) require(match.prices().get(tag) == 990 && ItemWandCasting.getVis(tile.getItem(10), tag) == 2500,
                            "actual server recipe match prices each primal before any output extraction");
                });
                next(Step.WAND_PRICED);
            }
            case WAND_PRICED -> {
                if (!serverDone() || ticks < 10) return false;
                evidence.put("native_wand_recipe_id_server_verified", "thaumcraft:arcane/wand_assembly");
                next(Step.WAND_CRAFT);
                capture.accept("38-native-greatwood-gold-wand-assembly-menu-en.png");
            }
            case WAND_CRAFT -> {
                require(client.player.getInventory().getItem(7).isEmpty(), "native result destination starts empty");
                click(client, 0, 0, ClickType.PICKUP);
                require(arcane(client).getCarried().isEmpty(), "client result click does not optimistically grant a wand");
                next(Step.WAND_CRAFTED);
            }
            case WAND_CRAFTED -> {
                if (ticks < 10 || !isGreatwoodGold(arcane(client).getCarried())) return false;
                var menu = arcane(client);
                for (String tag : WandData.PRIMALS) require(ItemWandCasting.getVis(menu.getSlot(1).getItem(), tag) == 1510, "actual assembly debits exactly990centivis of every primal once");
                for (int slot = 2; slot <= 10; slot++) require(menu.getSlot(slot).getItem().isEmpty(), "all three native assembly inputs are consumed once");
                click(client, 45, 0, ClickType.PICKUP); // Store the server-created cursor in hotbar7.
                next(Step.WAND_STORED);
            }
            case WAND_STORED -> {
                if (ticks < 10 || !isGreatwoodGold(client.player.getInventory().getItem(7)) || !arcane(client).getCarried().isEmpty()) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var tile = (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench);
                    var output = player.getInventory().getItem(7);
                    require(isGreatwoodGold(output) && output.getCount() == 1 && ItemWandCasting.data(output).vis().isEmpty()
                            && ItemWandCasting.getMaxVis(output) == 5000 && output.getOrDefault(ThaumcraftWands.WAND_CRAFT_COST.get(), -1) == 9,
                            "server-created output has greatwood/gold, empty vis,5000capacity and original craft-cost9 component");
                    for (String tag : WandData.PRIMALS) require(ItemWandCasting.getVis(tile.getItem(10), tag) == 1510, "authoritative original crafting wand paid990 of every primal");
                    for (int slot = 0; slot < 10; slot++) require(tile.getItem(slot).isEmpty(), "server matrix and result storage contain no second output or ingredient");
                    require(count(player, ThaumcraftContent.WAND.get()) == 1 && count(player, ThaumcraftWands.GOLD_WAND_CAP.get()) == 0
                            && count(player, ThaumcraftWands.GREATWOOD_WAND_ROD.get()) == 0 && player.containerMenu.getCarried().isEmpty(), "native extraction grants exactly one wand and leaves no cursor duplicate");
                    require(WandMaterials.capModifier(ItemWandCasting.data(output).cap()) == 1F, "actual gold cap uses canonical1.0 modifier");
                    serverSnapshot = Map.of("server_verified", true, "recipe", "thaumcraft:arcane/wand_assembly", "output_count", 1, "rod", "greatwood", "cap", "gold",
                            "capacity_centivis", 5000, "gold_modifier", 1.0, "craft_cost_component", 9, "each_primal_debit", 990, "each_primal_remaining_in_original_wand", 1510);
                });
                next(Step.WAND_VERIFY);
            }
            case WAND_VERIFY -> {
                if (!serverDone()) return false;
                evidence.put("native_wand_assembly", serverSnapshot);
                client.player.closeContainer();
                client.player.getInventory().selected = 7;
                onServer(client, () -> serverPlayer(client).teleportTo(workbench.getX() + 1.5, workbench.getY(), workbench.getZ() + 2.6));
                next(Step.WAND_CHARGE_POSITION);
            }
            case WAND_CHARGE_POSITION -> {
                if (!serverDone() || ticks < 15 || !isGreatwoodGold(client.player.getMainHandItem()) || !(client.level.getBlockEntity(node) instanceof TileNode)) return false;
                lookAt(client, Vec3.atCenterOf(node));
                next(Step.WAND_CHARGE_START);
            }
            case WAND_CHARGE_START -> {
                lookAt(client, Vec3.atCenterOf(node));
                if (ticks < 10) return false;
                client.options.keyUse.setDown(true);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                next(Step.WAND_CHARGE);
            }
            case WAND_CHARGE -> {
                lookAt(client, Vec3.atCenterOf(node));
                if (WandData.PRIMALS.stream().anyMatch(tag -> ItemWandCasting.getVis(client.player.getMainHandItem(), tag) < 100)) return false;
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                next(Step.WAND_CHARGED);
            }
            case WAND_CHARGED -> {
                if (ticks < 15) return false;
                chargedOutput = ItemWandCasting.data(client.player.getMainHandItem());
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    require(isGreatwoodGold(player.getMainHandItem()) && ItemWandCasting.data(player.getMainHandItem()).equals(chargedOutput)
                            && chargedOutput.capacity() == 5000 && WandData.PRIMALS.stream().allMatch(tag -> chargedOutput.amount(tag) >= 100), "the actual crafted wand charges through native server item-use ticks, not an output component fixture");
                    var chargingNode = (TileNode) player.serverLevel().getBlockEntity(node);
                    var remaining = new LinkedHashMap<String, Integer>();
                    for (String tag : WandData.PRIMALS) remaining.put(tag, chargingNode.getAspects().getAmount(Aspect.getAspect(tag)));
                    require(remaining.values().stream().anyMatch(amount -> amount < 25), "native normal-node storage was actually drained");
                    require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4, "wand assembly/charging preserves all four returned phials");
                    serverSnapshot = Map.of("server_verified", true, "crafted_wand_vis_centivis", chargedOutput.vis(), "capacity_centivis", chargedOutput.capacity(),
                            "node_current_vis", remaining, "node_boundary", "supplied native NORMAL node, default25 per primal; not natural generation", "returned_empty_phials_retained", 4);
                });
                next(Step.WAND_WORLD);
            }
            case WAND_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                evidence.put("native_crafted_wand_charging", serverSnapshot);
                evidence.put("native_crafted_wand_presentation", wandPresentation(client));
                lookAt(client, Vec3.atCenterOf(node).add(-.4, -.4, 0));
                next(Step.BROWSER_OPEN);
                capture.accept("39-crafted-greatwood-gold-wand-native-charging-world-en.png");
            }
            case BROWSER_OPEN -> {
                client.player.getInventory().selected = 6;
                require(client.player.getMainHandItem().is(ThaumcraftContent.THAUMONOMICON.get()), "browser opens through the supplied real book item");
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                next(Step.BROWSER_CATEGORY);
            }
            case BROWSER_CATEGORY -> {
                if (!(client.screen instanceof GuiResearchBrowser) || ticks < 10) return false;
                BookClientSmoke.clickCategoryTab(client, "THAUMATURGY");
                next(Step.BROWSER_PAN);
            }
            case BROWSER_PAN -> {
                if (ticks < 10) return false;
                int x = (int) BookClientSmoke.field(client.screen, "graphLeft") + (int) BookClientSmoke.field(client.screen, "graphWidth") / 2;
                int y = (int) BookClientSmoke.field(client.screen, "graphTop") + (int) BookClientSmoke.field(client.screen, "graphHeight") / 2;
                require(client.screen.mouseDragged(x, y, 0, 24, -48), "native browser drag centres both newly ported material icons");
                next(Step.BROWSER_SCENE);
            }
            case BROWSER_SCENE -> {
                if (ticks < 10) return false;
                evidence.put("native_material_browser_icons", materialIcons(client));
                next(Step.ROD_OPEN);
                capture.accept("40-native-thaumaturgy-gold-cap-and-greatwood-rod-icons-en.png");
            }
            case ROD_OPEN -> {
                int[] point = researchPoint(client, "ROD_greatwood");
                var browser = client.screen;
                browser.mouseClicked(point[0] + 8, point[1] + 8, 0);
                browser.mouseReleased(point[0] + 8, point[1] + 8, 0);
                require(client.screen instanceof GuiResearchRecipe, "actual material icon click opens its native completed research entry");
                bookTurns = 0;
                next(Step.ROD_PAGE);
            }
            case ROD_PAGE -> {
                if (ticks < 10 || !showRecipe(client, "arcane/wand_rod_greatwood")) return false;
                evidence.put("native_material_book_navigation", Map.of("research", "ROD_greatwood", "recipe", "thaumcraft:arcane/wand_rod_greatwood", "opened_by_browser_click", true));
                next(Step.DONE);
            }
            case DONE -> { return true; }
        }
        return false;
    }

    private void verifyEmptyFurnace(ServerPlayer player) {
        var tile = (TileAlchemyFurnace) player.serverLevel().getBlockEntity(furnace);
        require(tile.isEmpty() && tile.getVis() == 0 && tile.getAspects().visSize() == 0 && tile.isBurning()
                && tile.isSpeedBoost() && tile.getCurrentItemBurnTime() == 6400 && tile.getBellowsCount() == 1, "server distillation spends all input/fuel and empties its buffer while the paid fuel remains burning");
        require(count(player, Items.COAL) == 0 && count(player, ThaumcraftContent.MATERIALS.get("alumentum").get()) == 0, "no supplied coal or Alumentum survives in player inventory");
    }

    private void requireEmptyAlembics(Level level) {
        for (int depth = 1; depth <= 2; depth++) require(alembic(level, depth).getAmount() == 0 && alembic(level, depth).getAspect() == null, "all native alembic essentia was extracted, not discarded or duplicated");
    }

    private Map<String, Object> verifyJars(ServerPlayer player) {
        var level = player.serverLevel();
        verifyEmptyFurnace(player);
        requireEmptyAlembics(level);
        require(jar(level, fireJar).getAspect() == Aspect.FIRE && jar(level, fireJar).getAmount() == 16
                && jar(level, energyJar).getAspect() == Aspect.ENERGY && jar(level, energyJar).getAmount() == 16
                && jar(level, fireJar).getMaxAmount() == 64 && jar(level, energyJar).getMaxAmount() == 64, "server native jars retain all16ignis +16potentia at64 capacity");
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftContent.FILLED_PHIAL.get()) == 0, "all four physical empty phials return after native jar insertions");
        return Map.of("server_verified", true, "jars", Map.of("ignis", 16, "potentia", 16), "each_jar_capacity", 64,
                "total_essentia_before", 32, "total_essentia_after", 32, "empty_phials_returned", 4, "filled_phials_remaining", 0,
                "furnace_buffer_remaining", 0, "alembic_contents_remaining", 0, "action", "four native filled-phial useItemOn packets");
    }

    private void prepareGrove(ServerPlayer player) {
        var level = player.serverLevel();
        // Owned fresh-world picture patch only. Preload fixture chunks here;
        // production generators themselves still refuse unavailable chunks.
        for (int x = (grove.getX() - 27) >> 4; x <= (grove.getX() + 27) >> 4; x++) {
            for (int z = (grove.getZ() - 21) >> 4; z <= (grove.getZ() + 29) >> 4; z++) level.getChunk(x, z);
        }
        require(grove.getY() + 39 < level.getMaxBuildHeight(), "tree fixture must fit native build height");
        for (int x = -24; x <= 24; x++) for (int z = -18; z <= 26; z++) {
            setFixtureBlock(level, grove.offset(x, -2, z), Blocks.DIRT.defaultBlockState());
            setFixtureBlock(level, grove.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y <= 38; y++) if (!level.isEmptyBlock(grove.offset(x, y, z))) setFixtureBlock(level, grove.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        setFixtureBlock(level, greatwood, ThaumcraftNaturals.GREATWOOD_SAPLING.get().defaultBlockState());
        setFixtureBlock(level, silverwood, ThaumcraftNaturals.SILVERWOOD_SAPLING.get().defaultBlockState());
        require(ThaumcraftNaturals.GREATWOOD_SAPLING.get().grow(level, greatwood, RandomSource.create(0)), "fixed greatwood fixture uses the actual native sapling growth algorithm");
        require(ThaumcraftNaturals.SILVERWOOD_SAPLING.get().grow(level, silverwood, RandomSource.create(7)), "fixed non-knot silverwood fixture uses actual native sapling growth without reroll");
        Map<String, Object> trees = Map.of("greatwood", treeGeometry(level, false), "silverwood", treeGeometry(level, true));
        var ids = new ArrayList<String>();
        var blocks = palette();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            BlockState state = block.defaultBlockState();
            if (block == ThaumcraftMasonry.STAIRS.get()) state = state.setValue(StairBlock.FACING, Direction.SOUTH);
            setFixtureBlock(level, grove.offset(-4 + (i % 4) * 2, 0, 9 + i / 4 * 2), state);
            ids.add(BuiltInRegistries.BLOCK.getKey(block).toString());
        }
        setFixtureBlock(level, grove.offset(-6, 0, 9), ThaumcraftNaturals.GREATWOOD_SAPLING.get().defaultBlockState());
        setFixtureBlock(level, grove.offset(4, 0, 9), ThaumcraftNaturals.SILVERWOOD_SAPLING.get().defaultBlockState());
        for (int y = 0; y < 5; y++) setFixtureBlock(level, grove.offset(0, y, 24), Blocks.STONE.defaultBlockState());
        player.teleportTo(grove.getX() + .5, grove.getY() + 5, grove.getZ() + 24.5);
        serverSnapshot = Map.of("server_verified", true, "growth_entry", "BlockMagicalSapling.grow(ServerLevel, BlockPos, RandomSource)",
                "greatwood_seed", 0, "silverwood_seed", 7, "native_tree_geometry", trees, "placed_palette_blocks", ids,
                "separate_sapling_samples", 2, "natural_frequency_claimed", false, "tree_origins", List.of(greatwood.toShortString(), silverwood.toShortString()));
    }

    private Map<String, Integer> treeGeometry(ServerLevel level, boolean silver) {
        Block log = silver ? ThaumcraftNaturals.SILVERWOOD_LOG.get() : ThaumcraftNaturals.GREATWOOD_LOG.get();
        Block leaves = silver ? ThaumcraftNaturals.SILVERWOOD_LEAVES.get() : ThaumcraftNaturals.GREATWOOD_LEAVES.get();
        int logs = 0, canopy = 0, horizontal = 0, natural = 0;
        for (BlockPos pos : BlockPos.betweenClosed(grove.offset(-24, 0, -18), grove.offset(24, 38, 7))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(log)) {
                logs++;
                if (state.getValue(RotatedPillarBlock.AXIS).isHorizontal()) horizontal++;
            } else if (state.is(leaves)) {
                canopy++;
                if (!state.getValue(LeavesBlock.PERSISTENT)) natural++;
            }
        }
        require(logs > 16 && canopy > 32 && horizontal > 0 && natural == canopy, "actual native tree has branched axis-aware logs and nonpersistent canopy, not manually arranged trunk/leaves");
        return Map.of("logs", logs, "leaves", canopy, "horizontal_logs", horizontal, "nonpersistent_leaves", natural);
    }

    private static List<Block> palette() {
        return List.of(ThaumcraftNaturals.GREATWOOD_LOG.get(), ThaumcraftNaturals.GREATWOOD_PLANKS.get(),
                ThaumcraftNaturals.SILVERWOOD_LOG.get(), ThaumcraftNaturals.SILVERWOOD_PLANKS.get(),
                ThaumcraftMasonry.ARCANE_STONE.get(), ThaumcraftMasonry.ARCANE_STONE_BRICKS.get(), ThaumcraftMasonry.STAIRS.get(), ThaumcraftMasonry.SLAB.get());
    }

    private static void setFixtureBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) != state) level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static Map<String, Object> wandPresentation(Minecraft client) throws ReflectiveOperationException {
        ItemStack stack = client.player.getMainHandItem();
        require(isGreatwoodGold(stack), "held presentation uses the native crafted result");
        // The wand is a builtin/entity item: its model is drawn by ThaumcraftGearModelsClient.renderWand
        // through BuiltinItemRendererRegistry, NOT by baked quads. Asking the BakedModel for quads can
        // only ever return the empty custom-renderer shell, so the previous assertion could not pass
        // for the right reason and failed even though the frame shows the correct 3D wand.
        //
        // The material selection that actually decides the pixels is ModelWand.rodTexture(baseTag)
        // and ModelWand.capTexture(tag). Assert THOSE, and assert that the files they name exist.
        var data = ItemWandCasting.data(stack);
        var rod = thaumcraft.common.items.wands.WandMaterials.rod(data.rod());
        var cap = thaumcraft.common.items.wands.WandMaterials.cap(data.cap());
        var rodTexture = thaumcraft.client.renderers.models.gear.ModelWand.rodTexture(rod.baseTag());
        var capTexture = thaumcraft.client.renderers.models.gear.ModelWand.capTexture(cap.tag());
        require(rodTexture.getPath().equals("textures/models/wand_rod_greatwood.png"),
                "the greatwood rod selects the original greatwood sheet, not the wood fallback: " + rodTexture);
        require(capTexture.getPath().equals("textures/models/wand_cap_gold.png"),
                "the gold cap selects the original gold sheet, not the iron fallback: " + capTexture);
        for (var sheet : List.of(rodTexture, capTexture)) {
            require(BookClientSmoke.class.getClassLoader().getResource("assets/" + sheet.getNamespace() + "/" + sheet.getPath()) != null,
                    "the selected material sheet exists in the shipped assets: " + sheet);
        }
        // The model still has to resolve to something (the builtin shell), so a missing model is caught.
        var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
        require(model != client.getModelManager().getMissingModel(), "the wand resolves an item model shell");
        var colors = (ItemColors) BookClientSmoke.field(client, "itemColors");
        require(colors.getColor(stack, 0) == -1, "wand's original material textures retain their native untinted colour");
        return Map.of("rod_texture", rodTexture.toString(), "cap_texture", capTexture.toString(),
                "tint_argb", colors.getColor(stack, 0), "capacity_centivis", ItemWandCasting.getMaxVis(stack),
                "tooltip", stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList());
    }

    private void openBook(Minecraft client, String key) {
        client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get(key)));
        bookTurns = 0;
    }

    private boolean showRecipe(Minecraft client, String path) throws ReflectiveOperationException {
        require(client.screen instanceof GuiResearchRecipe, "recipe evidence must use the native research book");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
        int page = (int) BookClientSmoke.field(client.screen, "page");
        var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) {
            Object recipe = BookClientSmoke.field(pages.get(i), "recipe");
            if (recipe instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id)) {
                require(display.recipe() == client.level.getRecipeManager().byKey(id).orElseThrow(), "book recipe image uses the current actual native RecipeHolder");
                return true;
            }
        }
        require(++bookTurns <= 24, "native recipe spread must be reachable with bounded page turns: " + id);
        clickButton(client, "→");
        ticks = 0;
        return false;
    }

    private static Map<String, Object> materialIcons(Minecraft client) throws ReflectiveOperationException {
        var resolve = Class.forName("thaumcraft.client.gui.ResearchIconDisplay").getDeclaredMethod("resolve", String.class);
        resolve.setAccessible(true);
        var result = new LinkedHashMap<String, Object>();
        int left = (int) BookClientSmoke.field(client.screen, "graphLeft"), top = (int) BookClientSmoke.field(client.screen, "graphTop");
        int width = (int) BookClientSmoke.field(client.screen, "graphWidth"), height = (int) BookClientSmoke.field(client.screen, "graphHeight");
        for (String key : List.of("ROD_greatwood", "CAP_gold")) {
            ItemStack icon = (ItemStack) resolve.invoke(null, ResearchCatalog.get(key).iconExpression());
            require(icon.is(key.equals("ROD_greatwood") ? ThaumcraftWands.GREATWOOD_WAND_ROD.get() : ThaumcraftWands.GOLD_WAND_CAP.get()), "native research icon resolves the actual material item, not book fallback");
            int[] point = researchPoint(client, key);
            require(point[0] - 2 >= left && point[1] - 2 >= top && point[0] + 24 < left + width && point[1] + 24 < top + height, "both material icons are fully inside the actual graph scissor");
            require(client.getItemRenderer().getModel(icon, client.level, client.player, 0) != client.getModelManager().getMissingModel(), "native browser material icon has a real item model");
            result.put(key, Map.of("item", BuiltInRegistries.ITEM.getKey(icon.getItem()).toString(), "x", point[0], "y", point[1], "fully_visible", true));
        }
        return result;
    }

    private static int[] researchPoint(Minecraft client, String key) throws ReflectiveOperationException {
        for (Object node : (List<?>) BookClientSmoke.field(client.screen, "nodes")) {
            if (((ResearchDefinition) BookClientSmoke.field(node, "research")).key().equals(key)) return new int[] {(int) BookClientSmoke.field(node, "x"), (int) BookClientSmoke.field(node, "y")};
        }
        throw new IllegalStateException("Native browser did not draw " + key);
    }

    private static void clickButton(Minecraft client, String label) {
        var screen = client.screen;
        var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(widget -> widget.active && widget.getMessage().getString().equals(label)).findFirst().orElseThrow();
        screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
        screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
    }

    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(furnace.above(depth)); }
    private static TileJar jar(Level level, BlockPos pos) { return (TileJar) level.getBlockEntity(pos); }
    private static ContainerAlchemyFurnace furnaceMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int filledCount(Player player, String tag) {
        return player.getInventory().items.stream().filter(stack -> stack.is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(stack) != null
                && ItemEssence.data(stack).tag().equals(tag)).mapToInt(ItemStack::getCount).sum();
    }
    private static int itemSlot(Player player, Item item) {
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot).is(item)) return slot;
        throw new IllegalStateException("Native hotbar lacks " + BuiltInRegistries.ITEM.getKey(item));
    }
    private static int filledSlot(Player player, String tag) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ThaumcraftContent.FILLED_PHIAL.get()) && ItemEssence.data(stack) != null && ItemEssence.data(stack).tag().equals(tag)) return slot;
        }
        return -1;
    }
    private static boolean isGreatwoodGold(ItemStack stack) {
        return stack.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(stack).rod().equals("greatwood") && ItemWandCasting.data(stack).cap().equals("gold");
    }
    private static void click(Minecraft client, int slot, int button, ClickType type) {
        client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player);
    }
    private static void useBlock(Minecraft client, BlockPos pos) {
        boolean jar = client.level.getBlockEntity(pos) instanceof TileJar;
        Vec3 target = new Vec3(pos.getX() + .5, pos.getY() + (jar ? .4 : .5), pos.getZ() + (jar ? .8125 : 1));
        require(client.player.canInteractWithBlock(pos, 0), "native horizontal block action stays within survival reach");
        lookAt(client, target);
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(target, Direction.SOUTH, pos, false)).consumesAction(), "native useItemOn must accept the real block action at " + pos);
    }
    private void machineCamera(Minecraft client) {
        onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() - 2.2, furnace.getY(), furnace.getZ() + 5.2));
    }
    private static void lookAt(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.yRotO = client.player.getYRot();
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        client.player.xRotO = client.player.getXRot();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private void onServer(Minecraft client, Runnable action) {
        require(serverAction == null || serverAction.isDone(), "fixture/server inspection actions must never overlap");
        serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer());
    }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
