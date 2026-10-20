package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockGolemFetter;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * The Golem Fetter on real content: the block's own redstone behaviour (toggling with the signal, the original's "looks only when a
 * neighbour changes" quirk, hardness, tool, drops, the loaded arcane recipe) and two real Fill golems on stacked platforms, one
 * standing on an active fetter and one on an inactive one. The held golem must not move, take or carry anything while the free one
 * delivers; removing the signal must set the held golem to work. A third golem is stopped on its way: an active fetter appears under it
 * while it walks, it must stand still, and it finishes once the signal is gone. NOT a client/GUI check: the inactive pose is unseen.
 */
public final class GolemFetterRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int TALLOW = 2, DEADLINE = 1500, HOLD = 100, LAYER = 6, BENCH = 2;
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemFetterRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.theBlockFollowsTheSignal();
            check.theBlockIsOrdinaryStone();
            check.theRecipeIsLoaded();
            check.armLanes();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Fetter lanes failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the fetter lanes did not finish within " + DEADLINE + " ticks: " + state);
        });
    }

    private static final class Lane {
        final String name;
        final boolean fettered;
        EntityGolemBase golem;
        ChestBlockEntity home, source;
        BlockPos standing;
        Vec3 start;
        boolean done;

        Lane(String name, boolean fettered) { this.name = name; this.fettered = fettered; }
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Lane> lanes = new ArrayList<>();
        private BlockState active, idle;
        private boolean touched, finished, released;
        private long releasedAt, midFreezeAt;
        private int midPhase;
        private BlockPos midFloor;
        private Vec3 midFrozen;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-2, -3, -2), base.offset(3, LAYER * (BENCH + 1) + 4, 5))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
        }

        private void start() {
            touched = true;
            forced.addAll(GolemChunks.hold(level, original.keySet()));
            for (BlockPos mutable : original.keySet()) {
                level.setBlock(mutable, (mutable.getY() - (base.getY() - 1)) % LAYER == 0 && mutable.getY() >= base.getY() - 1
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            idle = ThaumcraftMasonry.GOLEM_FETTER.get().defaultBlockState();
            active = idle.setValue(BlockGolemFetter.ACTIVE, true);
        }

        private BlockState at(BlockPos pos) { return level.getBlockState(pos); }

        // ---- the block ------------------------------------------------------------------------------------------

        private void theBlockFollowsTheSignal() {
            var bench = base.offset(0, LAYER * BENCH + 1, 0);
            var fetter = bench;
            level.setBlock(fetter, idle, Block.UPDATE_ALL);
            test.assertTrue(at(fetter).equals(idle), "a placed fetter is inactive, with no signal about");
            level.setBlock(fetter.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(at(fetter).equals(active), "a neighbouring redstone block makes the fetter active: " + at(fetter));
            level.setBlock(fetter.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(at(fetter).equals(idle), "taking the signal away makes it inactive again: " + at(fetter));
            // Any side powers it, not just one.
            for (Direction side : new Direction[] {Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP}) {
                level.setBlock(fetter.relative(side), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                test.assertTrue(at(fetter).equals(active), "a redstone block on the " + side + " side activates it");
                level.setBlock(fetter.relative(side), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                test.assertTrue(at(fetter).equals(idle), "and releasing it from the " + side + " side deactivates it");
            }
            level.setBlock(fetter, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            // The original looks only when a NEIGHBOUR changes (onNeighborBlockChange): a fetter placed beside a block that is already
            // powered stays inactive until the next neighbour change.
            var second = bench.offset(0, 0, 3);
            level.setBlock(second.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(second, idle, Block.UPDATE_ALL);
            test.assertTrue(at(second).equals(idle), "placed beside an already powered block it stays inactive, as in the original: " + at(second));
            level.setBlock(second.north(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(at(second).equals(active), "the next neighbour change makes it notice the signal: " + at(second));
            test.assertTrue(BlockGolemFetter.holds(level, second.above()) && !BlockGolemFetter.holds(level, second.above(2)) && !BlockGolemFetter.holds(level, second),
                    "only a golem whose feet are directly above an ACTIVE fetter is held");
            level.setBlock(second.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(at(second).equals(idle) && !BlockGolemFetter.holds(level, second.above()), "and releases it, so nothing is held any more");
            level.setBlock(second.north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(second, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        private void theBlockIsOrdinaryStone() {
            var fetter = ThaumcraftMasonry.GOLEM_FETTER.get();
            var pos = base.offset(0, LAYER * BENCH + 1, 0);
            for (BlockState state : List.of(idle, active)) {
                level.setBlock(pos, state, QUIET);
                test.assertTrue(state.getDestroySpeed(level, pos) == 2.0F && fetter.getExplosionResistance() == 6.0F,
                        "hardness 2 and explosion resistance 6, the cosmetic-solid base values");
                test.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.requiresCorrectToolForDrops(), "a pickaxe block that needs the right tool");
                var drops = state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, new ItemStack(Items.IRON_PICKAXE)));
                test.assertTrue(drops.size() == 1 && drops.get(0).is(fetter.asItem()) && drops.get(0).getCount() == 1,
                        "it drops one Golem Fetter (the inactive one) in either state: " + drops);
                test.assertTrue(fetter.getCloneItemStack(level, pos, state).is(fetter.asItem()), "pick block gives the item");
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem_fetter")), "the item exists");
        }

        private void theRecipeIsLoaded() {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/golem_fetter"));
            test.assertTrue(holder.isPresent() && holder.get().value() instanceof ArcaneShapedRecipe, "the arcane recipe is loaded");
            var recipe = (ArcaneShapedRecipe) holder.get().value();
            test.assertTrue(recipe.research().equals("GOLEMFETTER") && recipe.pattern().equals(List.of("SSS", "IRI", "BBB"))
                            && recipe.vis().equals(Map.of("terra", 5, "ordo", 5)) && recipe.result().is(ThaumcraftMasonry.GOLEM_FETTER.get().asItem())
                            && recipe.result().getCount() == 1,
                    "GOLEMFETTER, the original SSS/IRI/BBB with five earth and five order");
            Map<String, ItemStack> key = Map.of("S", new ItemStack(ThaumcraftMasonry.ARCANE_STONE.get()), "I", new ItemStack(Items.IRON_INGOT),
                    "B", new ItemStack(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get()), "R", new ItemStack(Items.REDSTONE_BLOCK));
            key.forEach((symbol, stack) -> test.assertTrue(recipe.key().get(symbol).test(stack), symbol + " accepts " + stack.getItem()));
            test.assertTrue(!recipe.key().get("S").test(new ItemStack(ThaumcraftMasonry.ARCANE_STONE_BRICKS.get())) && !recipe.key().get("R").test(new ItemStack(Items.REDSTONE)),
                    "arcane stone is not its bricks and a redstone block is not redstone dust");
        }

        // ---- two real golems ------------------------------------------------------------------------------------

        private ChestBlockEntity chest(BlockPos pos, ItemStack contents) {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), QUIET);
            var chest = (ChestBlockEntity) level.getBlockEntity(pos);
            test.assertTrue(chest != null, "a chest block entity at " + pos);
            if (!contents.isEmpty()) chest.setItem(0, contents.copy());
            return chest;
        }

        private static int count(Container container) {
            int total = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.is(Items.COBBLESTONE)) total += stack.getCount();
            }
            return total;
        }

        private void lane(String name, boolean fettered) {
            var lane = new Lane(name, fettered);
            lane.standing = base.offset(1, LAYER * lanes.size(), 0);
            lane.home = chest(lane.standing.west(), ItemStack.EMPTY);
            lane.source = chest(lane.standing.south(3), new ItemStack(Items.COBBLESTONE, 64));
            level.setBlock(lane.standing.below(), idle, Block.UPDATE_ALL); // the fetter replaces the platform block under the golem
            if (fettered) level.setBlock(lane.standing.below(2), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            test.assertTrue(at(lane.standing.below()).equals(fettered ? active : idle), name + ": the fetter under the golem is " + (fettered ? "active" : "inactive"));
            var filters = new ArrayList<>(NO_FILTERS);
            var counts = new ArrayList<>(NO_COUNTS);
            filters.set(0, new ItemStack(Items.COBBLESTONE));
            counts.set(0, 8);
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(TALLOW);
            golem.applyConfiguration(new GolemData(GolemData.FILL, filters, counts, false,
                    List.of(new GolemMarker(OVERWORLD, lane.source.getBlockPos(), Direction.NORTH, GolemMarker.ANY)), GolemUseOptions.DEFAULT, List.of()));
            golem.setPos(Vec3.atBottomCenterOf(lane.standing));
            golem.setHome(lane.standing, Direction.EAST);
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            lane.golem = golem;
            lane.start = golem.position();
            lanes.add(lane);
        }

        private void armLanes() {
            lane("held golem (active fetter)", true);
            lane("free golem (inactive fetter)", false);
            lane("walking golem (fetter switched on under it)", false);
        }

        private Lane held() { return lanes.get(0); }
        private Lane free() { return lanes.get(1); }
        private Lane walking() { return lanes.get(2); }

        /** Switches an active fetter on under the walking golem, checks it stands still, then switches it off. */
        private void walkingGolem(long tick) {
            var lane = walking();
            switch (midPhase) {
                case 0 -> {
                    if (lane.golem.position().distanceToSqr(lane.start) > .36) { // it is on its way to the source
                        midFloor = lane.golem.blockPosition().below();
                        level.setBlock(midFloor, idle, Block.UPDATE_ALL);
                        level.setBlock(midFloor.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                        test.assertTrue(at(midFloor).equals(active), "the fetter under the walking golem switches on: " + at(midFloor));
                        midFreezeAt = tick + 6; // a few ticks for the walking momentum to die away
                        midPhase = 1;
                    }
                }
                case 1 -> {
                    if (tick == midFreezeAt) midFrozen = lane.golem.position();
                    else if (tick > midFreezeAt && tick < midFreezeAt + 40) {
                        test.assertTrue(lane.golem.isFettered() && lane.golem.position().distanceToSqr(midFrozen) < .0001 && count(lane.home) == 0,
                                "the walking golem stands still on the active fetter: moved from " + midFrozen + " to " + lane.golem.position());
                    } else if (tick >= midFreezeAt + 40) {
                        level.setBlock(midFloor.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        test.assertTrue(at(midFloor).equals(idle), "the fetter under the walking golem switches off");
                        midPhase = 2;
                    }
                }
                default -> { }
            }
        }

        private boolean poll() {
            long tick = test.getTick();
            walkingGolem(tick);
            for (Lane lane : lanes) {
                if (lane != walking() && tick >= 2 && (!released || tick > releasedAt + 2)) {
                    test.assertTrue(lane.golem.isFettered() == (lane.fettered && !released), lane.name + ": isFettered follows the block under it");
                }
                if (!lane.done && count(lane.home) > 0) lane.done = true;
            }
            if (!released) {
                // The held golem is frozen: nothing taken, nothing carried, not a step.
                var lane = held();
                test.assertTrue(count(lane.home) == 0 && count(lane.source) == 64 && lane.golem.getCarriedItem().isEmpty()
                                && lane.golem.position().distanceToSqr(lane.start) < .0025,
                        lane.name + " must not work or move: home " + count(lane.home) + ", source " + count(lane.source)
                                + ", carrying " + lane.golem.getCarriedItem() + ", at " + lane.golem.position());
                if (tick >= HOLD && free().done) {
                    level.setBlock(lane.standing.below(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    test.assertTrue(at(lane.standing.below()).equals(idle), "removing the signal releases the fetter");
                    released = true;
                    releasedAt = tick;
                }
                return false;
            }
            return held().done && free().done && walking().done && midPhase == 2;
        }

        private String describe() {
            var text = new StringBuilder("released=" + released + "; ");
            for (Lane lane : lanes) {
                text.append("walking phase ").append(midPhase).append(' ').append(lane.name).append(" fettered=").append(lane.golem.isFettered()).append(" home ").append(count(lane.home))
                        .append(" source ").append(count(lane.source)).append(" carrying ").append(lane.golem.getCarriedItem())
                        .append(" at ").append(lane.golem.position()).append(" ticks ").append(lane.golem.tickCount).append("; ");
            }
            return text.toString();
        }

        private void finish() {
            if (finished) return;
            finished = true;
            for (Entity entity : spawned) entity.discard();
            if (touched) original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
