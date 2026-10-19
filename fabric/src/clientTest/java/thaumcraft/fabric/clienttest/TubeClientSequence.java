package thaumcraft.fabric.clienttest;

import dev.architectury.event.events.common.TickEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
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
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerAlchemyFurnace;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.items.ItemJarLabel;
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
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/** Native recipe/menu/block actions; only empty machines, raw inputs, research and camera are fixtures. */
final class TubeClientSequence {
    private enum Step {
        SETUP, BOOK, BOOK_PAGE, OPEN, SILVER_PICK, SILVER_GRID, DROPS_READY, DROPS_TAKE, DROPS_STORE,
        FUND, MATRIX, TUBES_READY, TUBES_CAPTURE, TUBES_TAKE, TUBES_STORE, WAND_PICK, WAND_STORE,
        CRAFT_VERIFY, NETWORK_POSITION, PLACE_FIRST, PLACE_SECOND, DRY_VERIFY, DRY_WORLD,
        CLOSE_AIM, CLOSE_WAIT, FURNACE_POSITION, FURNACE_OPEN, FUEL, COAL_PICK, COAL_GRID, BURNING,
        DISTILL, CLOSED_STABLE, CLOSED_WORLD, REOPEN_AIM, REOPEN_WAIT, LABEL_MOVE, LABEL_AIM, LABEL_WAIT,
        FILL, FILLED_WORLD, BREAK_AIM, BREAK, PICKUP, JAR_MOVE, JAR_PLACE, JAR_VERIFY, RESTORED_WORLD, DONE
    }

    final Map<String, Object> report = new LinkedHashMap<>();
    private static final Map<String, Integer> BUDGET = Map.of("ignis", 8, "potentia", 8);
    private final BlockPos workbench, furnace, first, second, jarPos;
    private Step step = Step.SETUP;
    private int ticks, turns, action, silverSlot, labelSlot, jarSlot;
    private int[][] matrixClicks;
    private CompletableFuture<Void> serverAction;
    private Map<String, Object> snapshot = Map.of();
    private ItemStack wandBefore, focusBefore, bookBefore, appleBefore, nativeJarDrop;
    private WandData paidWand;
    private Aspect sourceType, otherType;
    private long closedAt;
    private int closedUpstream, closedDownstream;
    private boolean sourceReady, stable, filled, jarPicked;
    private UUID jarDropId;
    private TickEvent.Server conservation;
    private volatile Throwable observationFailure;
    private int checkedTicks, jarIncrements, lastJarAmount, initialJarAmount;

    /** Literal origins supplied by root; use empty, non-overlapping fixture patches. */
    TubeClientSequence(BlockPos workbench, BlockPos furnace) {
        this.workbench = workbench.immutable();
        this.furnace = furnace.immutable();
        first = furnace.offset(1, 1, 0);
        second = furnace.offset(2, 1, 0);
        jarPos = furnace.east(2);
        report.put("boundary", "Retains the actual greatwood/gold wand, book, four returned empty phials, returned Fire Focus, blaze powder and offhand apple. The one actual Focus-sequence quicksilver is CONSUMED by native ordinary crafting into9drops; one drop buys8native arcane tubes, leaving8drops. Supplied raw inputs:2iron ingots,1glass block,1gold nugget,4input coal,1fuel coal and1BLANK label. Supplied empty workbench/furnace/two alembics/jar, bounded stone patches, TUBES research and an INPUT-only aqua/ordo top-up to1000centivis each. No tube output, essentia, filled jar, bound label or jar item output is injected.");
        report.put("native_action_boundary", "No OS input or direct wand/transport/label backend calls. Native C2S inventory clicks, BlockItem placement on clicked EAST faces, wand useItemOn with server indexed-arm retrace, finite normal-coal distillation, jar UP pull, blank-label use, survival destroy progress, physical item pickup and component-bearing BlockItem replacement. Teleports and floor/machine placement are declared fixtures, not survival acquisition or world generation.");
        report.put("coordinates", Map.of("workbench", workbench.toShortString(), "furnace", furnace.toShortString(), "alembics", List.of(furnace.above().toShortString(), furnace.above(2).toShortString()),
                "tubes", List.of(first.toShortString(), second.toShortString()), "jar", jarPos.toShortString()));
        report.put("not_covered", List.of("full64 jar boundary", "vent conflicts", "tube reload", "advanced tubes", "natural world generation", "full survival/research acquisition", "visual parity before PNG inspection"));
    }

    boolean tick(Minecraft client, Consumer<String> capture) throws ReflectiveOperationException {
        if (++ticks > 1800) throw new IllegalStateException("Tube client step timed out: " + step + "; selected=" + client.player.getInventory().selected + "; hand=" + client.player.getMainHandItem());
        if (observationFailure != null) throw new IllegalStateException("Actual tube/jar conservation failed", observationFailure);
        switch (step) {
            case SETUP -> {
                client.setScreen(null);
                client.options.keyUse.setDown(false);
                client.options.keyShift.setDown(false);
                select(client, 8); // Keep native last-sent cache AND server fixture at8.
                onServer(client, () -> prepare(serverPlayer(client)));
                next(Step.BOOK);
            }
            case BOOK -> {
                if (!serverDone() || ticks < 15 || !KnowledgeAccess.get(client.player).isResearchComplete("TUBES")) return false;
                client.setScreen(new GuiResearchRecipe(new GuiResearchBrowser(), ResearchCatalog.get("TUBES")));
                next(Step.BOOK_PAGE);
            }
            case BOOK_PAGE -> {
                if (ticks < 10 || !showRecipe(client)) return false;
                report.put("native_book", Map.of("research", "TUBES", "recipe", "thaumcraft:arcane/essentia_tube", "entry_opened_by", "native research screen fixture",
                        "pattern", List.of(" Q ", "IGI", " B "), "Q", "one quicksilver DROP", "output", 8, "base_vis", Map.of("aqua", 5, "ordo", 5)));
                next(Step.OPEN);
                capture.accept("47-native-basic-essentia-tube-recipe-book-en.png");
            }
            case OPEN -> {
                client.setScreen(null);
                select(client, 8);
                use(client, workbench, Direction.SOUTH, Vec3.atCenterOf(workbench).add(0, 0, .5));
                next(Step.SILVER_PICK);
            }
            case SILVER_PICK -> {
                if (!(client.screen instanceof GuiArcaneWorkbench) || ticks < 8) return false;
                silverSlot = itemSlot(client.player, quicksilver());
                require(count(client.player, quicksilver()) == 1 && arcane(client).getSlot(1).getItem().isEmpty(), "ordinary prerequisite consumes the one actual flower-crafted quicksilver, without a wand");
                click(client, arcaneSlot(silverSlot), 0, ClickType.PICKUP);
                next(Step.SILVER_GRID);
            }
            case SILVER_GRID -> {
                if (ticks < 6 || !arcane(client).getCarried().is(quicksilver())) return false;
                click(client, 6, 0, ClickType.PICKUP);
                next(Step.DROPS_READY);
            }
            case DROPS_READY -> {
                if (ticks < 8 || !arcane(client).getSlot(0).getItem().is(drops())) return false;
                require(arcane(client).craftable() && arcane(client).getSlot(0).getItem().getCount() == 9, "real ordinary single-cell recipe previews exactly9quicksilver drops");
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var match = ArcaneCrafting.find(player, (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench));
                    require(match != null && !match.arcane() && match.output().is(drops()) && match.output().getCount() == 9 && match.prices().isEmpty(), "actual native ordinary RecipeHolder, not a supplied prerequisite output");
                    snapshot = Map.of("recipe", match.recipe().id().toString(), "input", "retained thaumcraft:quick_silver x1", "output", "thaumcraft:quicksilver_drop x9", "vis_cost", 0);
                });
                next(Step.DROPS_TAKE);
            }
            case DROPS_TAKE -> {
                if (!serverDone()) return false;
                report.put("native_quicksilver_prerequisite", snapshot);
                click(client, 0, 0, ClickType.PICKUP);
                next(Step.DROPS_STORE);
            }
            case DROPS_STORE -> {
                if (ticks < 8 || !arcane(client).getCarried().is(drops())) return false;
                require(arcane(client).getCarried().getCount() == 9 && arcane(client).getSlot(6).getItem().isEmpty(), "native extraction consumed the actual quicksilver once");
                click(client, arcaneSlot(silverSlot), 0, ClickType.PICKUP);
                next(Step.FUND);
            }
            case FUND -> {
                if (ticks < 8 || count(client.player, drops()) != 9 || !arcane(client).getCarried().isEmpty()) return false;
                click(client, 45, 0, ClickType.QUICK_MOVE); // Actual retained hotbar7 wand -> wand slot.
                matrixClicks = new int[][] {{arcaneSlot(silverSlot), 0}, {3, 1}, {arcaneSlot(silverSlot), 0},
                        {11, 0}, {5, 1}, {7, 0}, {12, 0}, {6, 0}, {13, 0}, {9, 0}};
                next(Step.MATRIX);
            }
            case MATRIX -> {
                if (ticks < 6 || !arcane(client).getSlot(1).getItem().is(ThaumcraftContent.WAND.get())) return false;
                int[] at = matrixClicks[action++];
                click(client, at[0], at[1], ClickType.PICKUP);
                next(action == matrixClicks.length ? Step.TUBES_READY : Step.MATRIX);
            }
            case TUBES_READY -> {
                var menu = arcane(client);
                if (ticks < 10 || !menu.craftable() || !menu.getSlot(0).getItem().is(tubeItem())) return false;
                require(menu.getSlot(0).getItem().getCount() == 8 && menu.requiredVis("aqua") == 500 && menu.requiredVis("ordo") == 500 && menu.getCarried().isEmpty(), "actual gold-capped arcane tube preview is8for500aqua/500ordo centivis");
                for (int cell = 0; cell < 9; cell++) {
                    Item expected = switch (cell) { case 1 -> drops(); case 3, 5 -> Items.IRON_INGOT; case 4 -> Items.GLASS; case 7 -> Items.GOLD_NUGGET; default -> Items.AIR; };
                    ItemStack stack = menu.getSlot(cell + 2).getItem();
                    require(expected == Items.AIR ? stack.isEmpty() : stack.is(expected) && stack.getCount() == 1, "native clicks populated only the original Q/IGI/B cells");
                }
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    var match = ArcaneCrafting.find(player, (TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench));
                    require(match != null && match.arcane() && match.recipe().id().equals(id("arcane/essentia_tube")) && match.output().is(tubeItem()) && match.output().getCount() == 8
                            && match.prices().equals(Map.of("aqua", 500, "ordo", 500)), "authoritative original tube recipe/prices are backed by the native manager");
                    require(count(player, tubeItem()) == 0 && count(player, drops()) == 8 && count(player, quicksilver()) == 0, "no physical tube output exists before result extraction");
                    snapshot = Map.of("server_verified", true, "recipe", match.recipe().id().toString(), "output", 8, "prices_centivis", match.prices(), "quicksilver_drops_remaining", 8);
                });
                next(Step.TUBES_CAPTURE);
            }
            case TUBES_CAPTURE -> {
                if (!serverDone()) return false;
                report.put("native_arcane_preview", snapshot);
                next(Step.TUBES_TAKE);
                capture.accept("48-native-basic-essentia-tube-arcane-assembly-menu-en.png");
            }
            case TUBES_TAKE -> { click(client, 0, 0, ClickType.PICKUP); next(Step.TUBES_STORE); }
            case TUBES_STORE -> {
                if (ticks < 8 || !arcane(client).getCarried().is(tubeItem())) return false;
                require(arcane(client).getCarried().getCount() == 8 && client.player.getInventory().getItem(15).isEmpty(), "one real8-tube cursor output has a reserved empty inventory destination");
                click(client, 17, 0, ClickType.PICKUP); // Arcane inventory15.
                next(Step.WAND_PICK);
            }
            case WAND_PICK -> {
                if (ticks < 8 || count(client.player, tubeItem()) != 8 || !arcane(client).getCarried().isEmpty()) return false;
                click(client, 1, 0, ClickType.PICKUP); // Avoid original wand-slot SWAP-button normalisation.
                next(Step.WAND_STORE);
            }
            case WAND_STORE -> {
                if (ticks < 8 || !arcane(client).getCarried().is(ThaumcraftContent.WAND.get())) return false;
                click(client, 45, 0, ClickType.PICKUP);
                next(Step.CRAFT_VERIFY);
            }
            case CRAFT_VERIFY -> {
                if (ticks < 8 || !client.player.getInventory().getItem(7).is(ThaumcraftContent.WAND.get())) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client);
                    paidWand = ItemWandCasting.data(wandBefore).withAmount("aqua", 500).withAmount("ordo", 500);
                    ItemStack expected = wandBefore.copy(); expected.set(ThaumcraftContent.WAND_DATA.get(), paidWand);
                    require(ItemStack.matches(player.getInventory().getItem(7), expected) && ((TileArcaneWorkbench) player.serverLevel().getBlockEntity(workbench)).isEmpty()
                            && player.containerMenu.getCarried().isEmpty(), "native craft pays exactly500of each requested primal and returns the unchanged rest of the actual wand");
                    require(count(player, tubeItem()) == 8 && count(player, drops()) == 8 && count(player, quicksilver()) == 0
                            && count(player, Items.IRON_INGOT) == 0 && count(player, Items.GLASS) == 0 && count(player, Items.GOLD_NUGGET) == 0, "actual ordinary+arcane crafts conserve all physical ingredients and output exactly8tubes");
                    retained(player);
                    snapshot = Map.of("server_verified", true, "native_tube_outputs", 8, "remaining_drops", 8, "original_quicksilver_consumed", 1, "each_requested_primal_debit", 500, "wand_after", paidWand.vis());
                    player.teleportTo(furnace.getX() + 2.4, furnace.getY(), furnace.getZ() + 2.6);
                });
                client.player.closeContainer();
                next(Step.NETWORK_POSITION);
            }
            case NETWORK_POSITION -> {
                if (!serverDone() || ticks < 15 || !(client.level.getBlockEntity(jarPos) instanceof TileJar)) return false;
                report.put("native_tube_craft", snapshot);
                require(client.player.getInventory().getItem(8).isEmpty(), "native placement hotbar destination is empty");
                click(client, 15, 8, ClickType.SWAP); // Real inventory15 -> hotbar8.
                next(Step.PLACE_FIRST);
            }
            case PLACE_FIRST -> {
                if (ticks < 8 || !client.player.getInventory().getItem(8).is(tubeItem())) return false;
                select(client, 8);
                use(client, furnace.above(), Direction.EAST, Vec3.atCenterOf(furnace.above()).add(.5, 0, 0));
                next(Step.PLACE_SECOND);
            }
            case PLACE_SECOND -> {
                if (ticks < 8 || !(client.level.getBlockEntity(first) instanceof TileTube tube)) return false;
                require(tube.getFacing() == Direction.EAST && count(client.player, tubeItem()) == 7, "first real floating BlockItem placement uses clicked EAST, not opposite");
                use(client, first, Direction.EAST, Vec3.atCenterOf(first).add(.125, 0, 0));
                next(Step.DRY_VERIFY);
            }
            case DRY_VERIFY -> {
                if (ticks < 8 || !(client.level.getBlockEntity(second) instanceof TileTube tube)) return false;
                require(tube.getFacing() == Direction.EAST && count(client.player, tubeItem()) == 6, "second real clicked-face placement consumes exactly one more crafted tube");
                onServer(client, () -> {
                    var level = serverPlayer(client).serverLevel();
                    require(tube(level, first).getFacing() == Direction.EAST && tube(level, second).getFacing() == Direction.EAST
                            && tube(level, first).isConnectable(Direction.EAST) && tube(level, second).isConnectable(Direction.WEST), "native two-tube arm connection and placement orientation are authoritative");
                    require(total(level).isEmpty() && jar(level).getFilter() == null, "real unlabelled jar, pipes, alembics and furnace all start genuinely dry");
                    snapshot = Map.of("server_verified", true, "placement", "two native BlockItem useItemOn/EAST packets", "facing", "east", "floating_first_tube", level.isEmptyBlock(first.below()), "crafted_tubes_in_world", 2, "crafted_tubes_retained", 6);
                });
                next(Step.DRY_WORLD);
            }
            case DRY_WORLD -> {
                if (!serverDone() || ticks < 10) return false;
                report.put("native_placement_and_dry_network", snapshot);
                select(client, 7);
                lookAt(client, Vec3.atCenterOf(first).add(.35, 0, 0));
                next(Step.CLOSE_AIM);
                capture.accept("49-native-two-crafted-tubes-floating-dry-network-world-en.png");
            }
            case CLOSE_AIM -> {
                aimArm(client);
                if (ticks < 10) return false;
                use(client, first, Direction.SOUTH, Vec3.atCenterOf(first).add(.35, 0, 0));
                next(Step.CLOSE_WAIT);
            }
            case CLOSE_WAIT -> {
                if (ticks < 8 || tube(client.level, first).isConnectable(Direction.EAST) || tube(client.level, second).isConnectable(Direction.WEST)) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(!tube(level, first).isConnectable(Direction.EAST) && !tube(level, second).isConnectable(Direction.WEST)
                            && tube(level, first).isConnectable(Direction.WEST) && tube(level, second).isConnectable(Direction.DOWN), "actual wand server retrace toggles indexed EAST arm and matching neighbor WEST, not the clicked SOUTH face or core");
                    require(ItemWandCasting.data(player.getInventory().getItem(7)).equals(paidWand), "native arm toggle costs no vis");
                    player.teleportTo(furnace.getX() + .5, furnace.getY(), furnace.getZ() + 2.6);
                });
                next(Step.FURNACE_POSITION);
            }
            case FURNACE_POSITION -> {
                if (!serverDone() || ticks < 15) return false;
                select(client, emptyHotbar(client));
                use(client, furnace, Direction.SOUTH, Vec3.atCenterOf(furnace).add(0, 0, .5));
                next(Step.FURNACE_OPEN);
            }
            case FURNACE_OPEN -> {
                if (!(client.screen instanceof GuiAlchemyFurnace) || ticks < 8) return false;
                click(client, 5, 0, ClickType.QUICK_MOVE); // Furnace inventory12: one normal fuel coal.
                next(Step.FUEL);
            }
            case FUEL -> {
                if (ticks < 8 || !furnaceMenu(client).getSlot(1).getItem().is(Items.COAL)) return false;
                click(client, 6, 0, ClickType.PICKUP); // Furnace inventory13: four INPUT coal, not quick-moved into fuel.
                next(Step.COAL_PICK);
            }
            case COAL_PICK -> {
                if (ticks < 6 || furnaceMenu(client).getCarried().getCount() != 4) return false;
                require(furnaceMenu(client).getCarried().is(Items.COAL), "real cursor holds exactly the four supplied distillation inputs");
                click(client, 0, 0, ClickType.PICKUP);
                next(Step.COAL_GRID);
            }
            case COAL_GRID -> {
                if (ticks < 8 || !furnaceMenu(client).isBurning()) return false;
                require(!furnaceMenu(client).isSpeedBoost() && furnaceMenu(client).getCurrentItemBurnTime() == 1600 && furnaceMenu(client).getSmeltTime() == 40,
                        "one real normal coal fuel burns1600; each original four-aspect coal smelts in40ticks without bellows");
                client.player.closeContainer();
                onServer(client, () -> serverPlayer(client).teleportTo(furnace.getX() + 2.4, furnace.getY(), furnace.getZ() + 2.6));
                next(Step.BURNING);
            }
            case BURNING -> {
                if (!serverDone() || ticks < 15) return false;
                select(client, 7);
                next(Step.DISTILL);
            }
            case DISTILL -> {
                if (!serverDone()) return false;
                if (!sourceReady) {
                    if (ticks % 10 == 0) onServer(client, () -> inspectFinishedSource(serverPlayer(client)));
                    return false;
                }
                report.put("native_distillation_source", snapshot);
                next(Step.CLOSED_STABLE);
            }
            case CLOSED_STABLE -> {
                if (!serverDone()) return false;
                if (!stable) {
                    if (ticks % 10 == 0) onServer(client, () -> {
                        var level = serverPlayer(client).serverLevel();
                        if (level.getGameTime() - closedAt < 50) return;
                        require(!tube(level, first).isConnectable(Direction.EAST) && !tube(level, second).isConnectable(Direction.WEST), "closed native arms remain closed for at least50server ticks");
                        require(alembic(level, 1).getAmount() + tube(level, first).getStoredEssentiaAmount() == closedUpstream
                                && jar(level).getAmount() + tube(level, second).getStoredEssentiaAmount() == closedDownstream, "closed cut preserves EXACT upstream/downstream sums, allowing an already-buffered downstream unit to reach the jar");
                        require(total(level).equals(BUDGET), "closed network preserves exactly8ignis+8potentia across every actual buffer");
                        snapshot = Map.of("server_verified", true, "native_closed_server_ticks", level.getGameTime() - closedAt, "upstream_sum", closedUpstream,
                                "downstream_sum", closedDownstream, "exact_total", total(level), "closed_sides", List.of("first.EAST", "second.WEST"));
                        stable = true;
                    });
                    return false;
                }
                next(Step.CLOSED_WORLD);
            }
            case CLOSED_WORLD -> {
                report.put("native_indexed_arm_close_and_stability", snapshot);
                lookAt(client, Vec3.atCenterOf(furnace).add(1, .8, 0));
                next(Step.REOPEN_AIM);
                capture.accept("50-native-wand-indexed-arm-closed-typed-alembics-world-en.png");
            }
            case REOPEN_AIM -> {
                aimArm(client);
                if (ticks < 10) return false;
                use(client, first, Direction.SOUTH, Vec3.atCenterOf(first).add(.35, 0, 0));
                next(Step.REOPEN_WAIT);
            }
            case REOPEN_WAIT -> {
                if (ticks < 8 || !tube(client.level, first).isConnectable(Direction.EAST) || !tube(client.level, second).isConnectable(Direction.WEST) || jar(client.level).getAmount() < 1) return false;
                labelSlot = emptyHotbar(client);
                click(client, 14, labelSlot, ClickType.SWAP); // Actual blank label -> free hotbar slot.
                next(Step.LABEL_MOVE);
            }
            case LABEL_MOVE -> {
                if (ticks < 8 || !(client.player.getInventory().getItem(labelSlot).getItem() instanceof ItemJarLabel)) return false;
                require(ItemJarLabel.data(client.player.getInventory().getItem(labelSlot)) == null, "supplied label is really blank; its binding must come from the genuinely filled jar");
                select(client, labelSlot);
                next(Step.LABEL_AIM);
            }
            case LABEL_AIM -> {
                lookAt(client, jarPoint());
                if (ticks < 8) return false;
                use(client, jarPos, Direction.SOUTH, jarPoint());
                next(Step.LABEL_WAIT);
            }
            case LABEL_WAIT -> {
                if (ticks < 8 || jar(client.level).getFilter() != sourceType || !client.player.getInventory().getItem(labelSlot).isEmpty()) return false;
                report.put("native_label", Map.of("input", "one supplied BLANK label", "action", "native useItemOn after the first real jar pull", "actual_filter", sourceType.getTag(), "label_consumed", 1));
                next(Step.FILL);
            }
            case FILL -> {
                if (!serverDone()) return false;
                if (!filled) {
                    if (ticks % 10 == 0) onServer(client, () -> {
                        var player = serverPlayer(client); var level = player.serverLevel();
                        if (jar(level).getAmount() != 8 || alembic(level, 1).getAmount() != 0 || tube(level, first).getStoredEssentiaAmount() != 0 || tube(level, second).getStoredEssentiaAmount() != 0) return;
                        verifyTransferred(player);
                        snapshot = Map.of("server_verified", true, "jar", Map.of("aspect", sourceType.getTag(), "amount", 8, "capacity", 64, "filter", jar(level).getFilter().getTag()),
                                "untouched_other_alembic", Map.of("aspect", otherType.getTag(), "amount", 8), "exact_total", total(level), "native_tick_conservation_checks", checkedTicks,
                                "observed_jar_increments", jarIncrements, "each_observed_increment", 1, "input_coal_consumed", 4, "fuel_coal_consumed", 1);
                        stopConservation();
                        filled = true;
                    });
                    return false;
                }
                next(Step.FILLED_WORLD);
            }
            case FILLED_WORLD -> {
                if (ticks < 10 || jar(client.level).getAmount() != 8 || jar(client.level).getFilter() != sourceType) return false;
                report.put("native_transport_and_labelled_jar", snapshot);
                select(client, 7);
                lookAt(client, Vec3.atCenterOf(furnace).add(1.05, .7, 0));
                next(Step.BREAK_AIM);
                capture.accept("51-native-typed-tube-pull-labelled-jar-and-other-alembic-world-en.png");
            }
            case BREAK_AIM -> {
                if (ticks == 1) select(client, emptyHotbar(client));
                lookAt(client, jarPoint());
                if (ticks < 10) return false;
                require(!client.gameMode.hasInfiniteItems() && client.player.canInteractWithBlock(jarPos, 0), "filled jar break is genuinely within native survival reach");
                require(client.gameMode.startDestroyBlock(jarPos, Direction.SOUTH), "native survival starts breaking the actual filled labelled jar");
                next(Step.BREAK);
            }
            case BREAK -> {
                lookAt(client, jarPoint());
                if (!client.level.isEmptyBlock(jarPos)) { client.gameMode.continueDestroyBlock(jarPos, Direction.SOUTH); return false; }
                client.gameMode.stopDestroyBlock();
                next(Step.PICKUP);
            }
            case PICKUP -> {
                if (!serverDone()) return false;
                if (!jarPicked || count(client.player, ThaumcraftContent.JAR.get().asItem()) != 1) {
                    if (ticks % 8 == 0) onServer(client, () -> inspectJarDrop(serverPlayer(client)));
                    return false;
                }
                jarSlot = itemSlot(client.player, ThaumcraftContent.JAR.get().asItem());
                require(ItemStack.matches(client.player.getInventory().getItem(jarSlot), nativeJarDrop), "native physical pickup retains every actual jar-item component");
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    require(ItemStack.matches(player.getInventory().getItem(jarSlot), nativeJarDrop) && level.getEntity(jarDropId) == null,
                            "server inventory owns exactly the native dropped jar and its physical entity was picked up");
                    var carriedTotal = total(level); merge(carriedTotal, sourceType, jarItemAmount(nativeJarDrop));
                    require(carriedTotal.equals(BUDGET), "world buffers PLUS the actual carried jar conserve the entire original coal budget");
                    snapshot = Map.of("native_drop_uuid", jarDropId.toString(), "actual_item_snbt", nativeJarDrop.save(player.registryAccess()).toString(), "carried_jar_amount", 8, "exact_world_plus_carried_total", carriedTotal);
                    player.teleportTo(furnace.getX() + 2.4, furnace.getY(), furnace.getZ() + 2.6);
                });
                next(Step.JAR_MOVE);
            }
            case JAR_MOVE -> {
                if (!serverDone() || ticks < 15) return false;
                report.put("native_jar_break_and_pickup", snapshot);
                if (jarSlot >= 9) { int destination = emptyHotbar(client); click(client, jarSlot, destination, ClickType.SWAP); jarSlot = destination; }
                next(Step.JAR_PLACE);
            }
            case JAR_PLACE -> {
                if (ticks < 8 || !ItemStack.matches(client.player.getInventory().getItem(jarSlot), nativeJarDrop)) return false;
                select(client, jarSlot);
                use(client, jarPos.below(), Direction.UP, Vec3.atCenterOf(jarPos.below()).add(0, .5, 0));
                next(Step.JAR_VERIFY);
            }
            case JAR_VERIFY -> {
                if (ticks < 10 || !(client.level.getBlockEntity(jarPos) instanceof TileJar jar) || jar.getAmount() != 8 || jar.getFilter() != sourceType) return false;
                onServer(client, () -> {
                    var player = serverPlayer(client); var level = player.serverLevel();
                    verifyTransferred(player);
                    require(count(player, ThaumcraftContent.JAR.get().asItem()) == 0 && jar(level).getAspect() == sourceType && jar(level).getFilter() == sourceType,
                            "native BlockItem placement consumes the physically picked-up jar and restores actual contents/filter, not an injected BE");
                    require(count(player, tubeItem()) == 6 && count(player, drops()) == 8 && count(player, quicksilver()) == 0, "all retained crafted tubes and prerequisite drops survive the complete native story");
                    retained(player);
                    snapshot = Map.of("server_verified", true, "aspect", sourceType.getTag(), "amount", 8, "filter", jar(level).getFilter().getTag(), "facing", jar(level).getFacing().getName(),
                            "jar_items_remaining", 0, "exact_total", total(level), "retained_tubes", 6, "retained_drops", 8, "empty_phials_retained", 4);
                    player.teleportTo(furnace.getX() + 2.4, furnace.getY(), furnace.getZ() + 3.2);
                });
                select(client, 7);
                next(Step.RESTORED_WORLD);
            }
            case RESTORED_WORLD -> {
                if (!serverDone() || ticks < 15) return false;
                report.put("native_filled_labelled_jar_replacement", snapshot);
                lookAt(client, Vec3.atCenterOf(furnace).add(1.05, .7, 0));
                next(Step.DONE);
                capture.accept("52-native-transport-jar-survival-break-pickup-replace-world-en.png");
            }
            case DONE -> { return true; }
        }
        return false;
    }

    private void prepare(ServerPlayer player) {
        var level = player.serverLevel();
        require(workbench.distSqr(furnace) > 64, "root supplies separate workbench and furnace patches");
        patch(level, workbench, 2, 3);
        patch(level, furnace, 4, 5);
        level.setBlockAndUpdate(workbench, ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState());
        level.setBlockAndUpdate(furnace, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
        for (int depth = 1; depth <= 2; depth++) level.setBlockAndUpdate(furnace.above(depth), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState().setValue(BlockAlembic.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(jarPos, ThaumcraftContent.JAR.get().defaultBlockState());
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.removeAllEffects();
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, quicksilver()) == 1 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1
                && count(player, Items.BLAZE_POWDER) == 1 && count(player, tubeItem()) == 0 && count(player, drops()) == 0 && count(player, Items.COAL) == 0, "accepted Focus handoff supplies real retained outputs, no tube/coal/drop output fixtures");
        require(player.getInventory().getItem(8).isEmpty(), "placement hotbar8 is not overwritten");
        for (int slot = 9; slot <= 15; slot++) require(player.getInventory().getItem(slot).isEmpty(), "new raw inputs never overwrite retained outputs");
        ItemStack wand = player.getInventory().getItem(7);
        require(wand.is(ThaumcraftContent.WAND.get()) && ItemWandCasting.data(wand).rod().equals("greatwood") && ItemWandCasting.data(wand).cap().equals("gold") && FocusData.get(wand).isEmpty(), "retain the actual crafted gold/greatwood wand with its focus removed");
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("aqua", 1000).withAmount("ordo", 1000));
        wandBefore = wand.copy(); focusBefore = player.getInventory().getItem(itemSlot(player, ThaumcraftFoci.FIRE_FOCUS.get())).copy();
        bookBefore = player.getInventory().getItem(6).copy(); appleBefore = player.getOffhandItem().copy();
        require(bookBefore.is(ThaumcraftContent.THAUMONOMICON.get()) && appleBefore.is(Items.APPLE) && appleBefore.getCount() == 1, "retain the actual accepted book and original offhand apple");
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 2));
        player.getInventory().setItem(10, new ItemStack(Items.GLASS));
        player.getInventory().setItem(11, new ItemStack(Items.GOLD_NUGGET));
        player.getInventory().setItem(12, new ItemStack(Items.COAL));
        player.getInventory().setItem(13, new ItemStack(Items.COAL, 4));
        player.getInventory().setItem(14, ItemJarLabel.blank());
        var coal = ObjectAspectResolver.get(new ItemStack(Items.COAL), level);
        require(coal.visSize() == 4 && coal.getAmount(Aspect.FIRE) == 2 && coal.getAmount(Aspect.ENERGY) == 2, "actual native coal aspect resolver establishes the finite exact8+8 output budget");
        KnowledgeAccess.get(player).addResearch("TUBES"); KnowledgeEvents.sync(player);
        player.getInventory().selected = 8; player.inventoryMenu.broadcastChanges();
        player.teleportTo(workbench.getX() + .5, workbench.getY(), workbench.getZ() + 2.6);
    }

    private static void patch(ServerLevel level, BlockPos origin, int radius, int height) {
        // Refuse to discard prior real BE contents. Root must choose fresh patches;
        // an empty old workbench/furnace may be reset as an explicit machine fixture.
        for (BlockPos at : BlockPos.betweenClosed(origin.offset(-radius, 0, -radius), origin.offset(radius, height, radius))) {
            var tile = level.getBlockEntity(at);
            require(tile == null || tile instanceof TileArcaneWorkbench table && table.isEmpty()
                    || tile instanceof TileAlchemyFurnace oven && oven.isEmpty() && oven.getVis() == 0
                    || tile instanceof TileAlembic alembic && alembic.getAmount() == 0 && alembic.getFilter() == null
                    || tile instanceof TileJar jar && jar.getAmount() == 0 && jar.getFilter() == null, "fixture patch must not delete prior contents or another unit's BE: " + at);
        }
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y <= height; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }

    private void inspectFinishedSource(ServerPlayer player) {
        var level = player.serverLevel(); var oven = oven(level);
        if (!oven.getItem(0).isEmpty() || oven.getVis() != 0) return;
        require(oven.getItem(1).isEmpty() && count(player, Items.COAL) == 0 && total(level).equals(BUDGET), "four actual source coal and one actual fuel coal are spent, retaining exactly8ignis+8potentia in the real storage chain");
        sourceType = alembic(level, 1).getAspect(); otherType = alembic(level, 2).getAspect();
        require(sourceType != null && otherType != null && sourceType != otherType && Set.of(sourceType, otherType).equals(Set.of(Aspect.FIRE, Aspect.ENERGY))
                && alembic(level, 2).getAmount() == 8, "unfiltered native furnace selected two distinct real alembic types; the second source remains untouched");
        closedAt = level.getGameTime();
        closedUpstream = alembic(level, 1).getAmount() + tube(level, first).getStoredEssentiaAmount();
        closedDownstream = tube(level, second).getStoredEssentiaAmount() + jar(level).getAmount();
        require(closedUpstream + closedDownstream == 8 && closedDownstream <= 1, "closed native cut cannot move more than its one already-buffered downstream unit");
        snapshot = Map.of("server_verified", true, "source_input_coal", 4, "fuel_coal", 1, "exact_produced_budget", BUDGET,
                "lower_aspect", sourceType.getTag(), "upper_aspect", otherType.getTag(), "untouched_upper_amount", 8, "furnace_buffer", 0, "injected_essentia", 0);
        sourceReady = true;
        startConservation(player);
    }

    private void startConservation(ServerPlayer player) {
        initialJarAmount = lastJarAmount = jar(player.level()).getAmount();
        conservation = server -> {
            if (server != player.server || observationFailure != null) return;
            try {
                var level = player.serverLevel();
                require(total(level).equals(BUDGET) && oven(level).getItem(0).isEmpty() && oven(level).getVis() == 0, "every observed native server tick preserves the exact finished-source budget across furnace/alembics/tubes/jar");
                require(alembic(level, 2).getAspect() == otherType && alembic(level, 2).getAmount() == 8, "the disconnected other aspect never moves");
                for (BlockPos at : List.of(first, second)) {
                    var tube = tube(level, at);
                    require(tube.getStoredEssentiaAmount() >= 0 && tube.getStoredEssentiaAmount() <= 1
                            && (tube.getStoredEssentiaAmount() == 0 || tube.getStoredEssentiaType() == sourceType), "actual tubes keep capacity1 and never transport the other alembic's type");
                }
                int now = jar(level).getAmount(), delta = now - lastJarAmount;
                require(delta == 0 || delta == 1, "each actual observed jar pull adds exactly one unit, never a bulk push");
                if (delta == 1) jarIncrements++;
                lastJarAmount = now; checkedTicks++;
            } catch (RuntimeException | Error error) { observationFailure = error; }
        };
        TickEvent.SERVER_POST.register(conservation);
    }

    private void stopConservation() {
        TickEvent.SERVER_POST.unregister(conservation);
        require(!TickEvent.SERVER_POST.isRegistered(conservation), "scoped passive native transport observer is removed");
    }

    private void verifyTransferred(ServerPlayer player) {
        var level = player.serverLevel();
        require(total(level).equals(BUDGET) && oven(level).getVis() == 0 && oven(level).isEmpty(), "exact original budget survives with no source input or furnace buffer");
        require(alembic(level, 1).getAmount() == 0 && alembic(level, 2).getAmount() == 8 && alembic(level, 2).getAspect() == otherType
                && tube(level, first).getStoredEssentiaAmount() == 0 && tube(level, second).getStoredEssentiaAmount() == 0
                && tube(level, first).getStoredEssentiaType() == null && tube(level, second).getStoredEssentiaType() == null, "native pull drains only the connected typed alembic and empties both real one-unit tubes");
        require(checkedTicks >= 50 && jarIncrements == 8 - initialJarAmount, "all actual jar units are accounted for by observed single-unit pulls with at least50native conservation ticks");
        require(jar(level).getAmount() == 8 && jar(level).getAspect() == sourceType && jar(level).getFilter() == sourceType
                && jar(level).isConnectable(Direction.UP) && !jar(level).isConnectable(Direction.DOWN) && jar(level).getSuctionAmount(Direction.UP) == 64,
                "actual filled labelled jar is UP-only and uses original64label suction, not a side input or supplied contents");
        for (Direction face : Direction.values()) require(jar(level).isConnectable(face) == (face == Direction.UP)
                && jar(level).canInputFrom(face) == (face == Direction.UP) && jar(level).canOutputTo(face) == (face == Direction.UP), "jar exposes only its original UP transport face");
        require(ItemWandCasting.data(player.getInventory().getItem(7)).equals(paidWand), "wand toggling and native transport do not consume extra vis");
    }

    private void inspectJarDrop(ServerPlayer player) {
        var level = player.serverLevel();
        if (!level.isEmptyBlock(jarPos)) return; // Native destroy packet, not fixture removal, must win first.
        if (jarPicked && count(player, ThaumcraftContent.JAR.get().asItem()) == 1) return; // Client inventory packet may still be in flight.
        var box = new AABB(Vec3.atLowerCornerOf(jarPos.offset(-1, -1, -1)), Vec3.atLowerCornerOf(jarPos.offset(2, 2, 2)));
        var items = level.getEntitiesOfClass(ItemEntity.class, box, entity -> entity.getItem().is(ThaumcraftContent.JAR.get().asItem()));
        require(items.size() == 1 && items.getFirst().getItem().getCount() == 1, "real survival break emits exactly one physical jar entity");
        var item = items.getFirst();
        if (jarPicked) require(item.getUUID().equals(jarDropId) && ItemStack.matches(item.getItem(), nativeJarDrop), "approach the SAME native dropped jar, never a replacement output");
        else { nativeJarDrop = item.getItem().copy(); jarDropId = item.getUUID(); }
        require(nativeJarDrop.getMaxStackSize() == 1 && ItemJarLabel.aspect(nativeJarDrop) == sourceType && jarItemAmount(nativeJarDrop) == 8,
                "actual native dropped item carries all8units and its real label component without item-output injection");
        require(nativeJarDrop.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getString("Aspect").equals(sourceType.getTag())
                && level.getEntitiesOfClass(ItemEntity.class, box, entity -> entity.getItem().getItem() instanceof ItemJarLabel).isEmpty(), "native break preserves the actual stored type without duplicating a loose label");
        jarPicked = true;
        // The native drop has a random offset/velocity. A fixed camera can miss
        // its pickup box forever. Approach its REAL position from a collision-
        // free side, retaining the overhead pipe and vanilla pickup delay/ticks.
        // This moves only the declared player/camera fixture, never the item.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 candidate = new Vec3(item.getX() + direction.getStepX() * 1.05, jarPos.getY(), item.getZ() + direction.getStepZ() * 1.05);
            if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) {
                player.teleportTo(candidate.x, candidate.y, candidate.z);
                return;
            }
        }
        throw new IllegalStateException("No collision-free native pickup approach for jar at " + item.position());
    }

    private static int jarItemAmount(ItemStack item) {
        var data = item.get(DataComponents.BLOCK_ENTITY_DATA);
        require(data != null, "native filled jar has actual block-entity data");
        return data.copyTag().getInt("Amount");
    }

    private Map<String, Integer> total(Level level) {
        var result = new LinkedHashMap<String, Integer>();
        var aspects = oven(level).getAspects();
        for (Aspect type : aspects.getAspects()) merge(result, type, aspects.getAmount(type));
        for (int depth = 1; depth <= 2; depth++) merge(result, alembic(level, depth).getAspect(), alembic(level, depth).getAmount());
        for (BlockPos at : List.of(first, second)) if (level.getBlockEntity(at) instanceof TileTube tube) merge(result, tube.getStoredEssentiaType(), tube.getStoredEssentiaAmount());
        if (level.getBlockEntity(jarPos) instanceof TileJar jar) merge(result, jar.getAspect(), jar.getAmount());
        return result;
    }

    private static void merge(Map<String, Integer> result, Aspect type, int amount) {
        require(amount >= 0 && (amount == 0 || type != null), "real storage cannot hide a negative or untyped amount");
        if (amount > 0) result.merge(type.getTag(), amount, Integer::sum);
    }

    private void retained(Player player) {
        require(count(player, ThaumcraftContent.EMPTY_PHIAL.get()) == 4 && count(player, ThaumcraftFoci.FIRE_FOCUS.get()) == 1
                && ItemStack.matches(player.getInventory().getItem(itemSlot(player, ThaumcraftFoci.FIRE_FOCUS.get())), focusBefore)
                && ItemStack.matches(player.getInventory().getItem(6), bookBefore) && ItemStack.matches(player.getOffhandItem(), appleBefore)
                && count(player, Items.BLAZE_POWDER) == 1 && FocusData.get(player.getInventory().getItem(7)).isEmpty(), "tube story preserves all retained accepted outputs except its explicitly consumed quicksilver input");
    }

    private boolean showRecipe(Minecraft client) throws ReflectiveOperationException {
        int page = (int) BookClientSmoke.field(client.screen, "page");
        var pages = (List<?>) BookClientSmoke.field(client.screen, "pages");
        for (int i = page; i < Math.min(page + 2, pages.size()); i++) {
            Object recipe = BookClientSmoke.field(pages.get(i), "recipe");
            if (recipe instanceof ThaumcraftRecipeDisplay display && display.recipe().id().equals(id("arcane/essentia_tube"))) {
                require(display.recipe() == client.level.getRecipeManager().byKey(id("arcane/essentia_tube")).orElseThrow(), "native book diagram uses the current real tube RecipeHolder");
                return true;
            }
        }
        require(++turns <= 24, "TUBES native recipe spread must be reachable");
        var button = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(widget -> widget.active && widget.getMessage().getString().equals("→")).findFirst().orElseThrow();
        client.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0); client.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
        ticks = 0; return false;
    }

    private void aimArm(Minecraft client) { lookAt(client, Vec3.atCenterOf(first).add(.35, 0, 0)); }
    private Vec3 jarPoint() { return new Vec3(jarPos.getX() + .5, jarPos.getY() + .4, jarPos.getZ() + .8125); }
    private static void select(Minecraft client, int slot) throws ReflectiveOperationException {
        client.player.getInventory().selected = slot;
        var ensure = client.gameMode.getClass().getDeclaredMethod("ensureHasSentCarriedItem");
        ensure.setAccessible(true); ensure.invoke(client.gameMode);
    }
    private static void use(Minecraft client, BlockPos pos, Direction face, Vec3 point) {
        require(client.player.canInteractWithBlock(pos, 0), "native block action remains within actual survival reach");
        lookAt(client, point);
        require(client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false)).consumesAction(), "native useItemOn must accept " + pos + "/" + face);
    }
    private static void lookAt(Minecraft client, Vec3 point) {
        Vec3 delta = point.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z))); client.player.yRotO = client.player.getYRot();
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))); client.player.xRotO = client.player.getXRot();
    }
    private static int emptyHotbar(Minecraft client) { for (int slot = 0; slot < 9; slot++) if (client.player.getInventory().getItem(slot).isEmpty()) return slot; throw new IllegalStateException("No actual empty native hotbar slot"); }
    private static int arcaneSlot(int inventory) { return inventory < 9 ? 38 + inventory : inventory + 2; }
    private static Item drops() { return ThaumcraftContent.MATERIALS.get("quicksilver_drop").get(); }
    private static Item quicksilver() { return ThaumcraftContent.MATERIALS.get("quick_silver").get(); }
    private static Item tubeItem() { return ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private TileJar jar(Level level) { return (TileJar) level.getBlockEntity(jarPos); }
    private TileAlchemyFurnace oven(Level level) { return (TileAlchemyFurnace) level.getBlockEntity(furnace); }
    private TileAlembic alembic(Level level, int depth) { return (TileAlembic) level.getBlockEntity(furnace.above(depth)); }
    private static TileTube tube(Level level, BlockPos at) { return (TileTube) level.getBlockEntity(at); }
    private static ContainerArcaneWorkbench arcane(Minecraft client) { return (ContainerArcaneWorkbench) client.player.containerMenu; }
    private static ContainerAlchemyFurnace furnaceMenu(Minecraft client) { return (ContainerAlchemyFurnace) client.player.containerMenu; }
    private static int count(Player player, Item item) { return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static int itemSlot(Player player, Item item) { for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(item)) return slot; throw new IllegalStateException("Native inventory lacks " + item); }
    private static void click(Minecraft client, int slot, int button, ClickType type) { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); }
    private static ServerPlayer serverPlayer(Minecraft client) { return client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void onServer(Minecraft client, Runnable action) { require(serverAction == null || serverAction.isDone(), "fixture/server observations never overlap"); serverAction = CompletableFuture.runAsync(action, client.getSingleplayerServer()); }
    private boolean serverDone() { if (serverAction == null) return true; if (!serverAction.isDone()) return false; serverAction.join(); return true; }
    private void next(Step next) { step = next; ticks = 0; }
}
