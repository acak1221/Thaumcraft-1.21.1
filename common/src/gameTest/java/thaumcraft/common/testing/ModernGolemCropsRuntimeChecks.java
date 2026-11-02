package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/** One real AI worker reuses a compact farm; no private-goal injection or synthetic harvesting. */
public final class ModernGolemCropsRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int DEADLINE = 3600;
    private ModernGolemCropsRuntimeChecks() {}

    public static void run(GameTestHelper test, Supplier<GolemFireOrderRuntimeChecks.PlacementProbe> probes) {
        var fixture = new Fixture(test, probes.get());
        try { fixture.start(); }
        catch (RuntimeException failure) { fixture.finish(); throw failure; }
        test.onEachTick(() -> {
            if (fixture.finished) return;
            try { if (fixture.poll()) { fixture.finish(); test.succeed(); } }
            catch (RuntimeException failure) { fixture.finish(); throw failure; }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (fixture.finished) return;
            String state = fixture.describe(); fixture.finish();
            test.fail("Modern crop worker did not complete within " + DEADLINE + " ticks: " + state);
        });
    }

    private static final class Fixture {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base, second, berry, head, body;
        final GolemFireOrderRuntimeChecks.PlacementProbe probe;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> ownedTickets = new ArrayList<>();
        final Map<BlockPos, Integer> breaks = new HashMap<>();
        final BlockEvent.Break breakListener;
        final int oldRandomTicks;
        final boolean oldGriefing, oldDrops;
        EntityGolemBase worker;
        ItemEntity supplied;
        boolean finished, registered, touched, denied, deniedChecked, regrown;
        int phase;
        long phaseStart;

        Fixture(GameTestHelper test, GolemFireOrderRuntimeChecks.PlacementProbe probe) {
            this.test = test; this.level = test.getLevel(); this.probe = probe;
            // The room reaches two blocks below base; keep it clear of the GameTest
            // structure's own block entities and the other native workbench fixtures.
            base = test.absolutePos(new BlockPos(3, 2, 3)).atY(180);
            second = base.south(2); berry = base; head = base.east(2).above(); body = base.south(2).above();
            oldRandomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            oldGriefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            oldDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
            breakListener = (world, pos, state, actor, xp) -> {
                if (world == level && actor != null && GolemHarvestPlatform.MACHINE_NAME.equals(actor.getGameProfile().getName())
                        && new AABB(Vec3.atLowerCornerOf(base.offset(-3, -2, -3)), Vec3.atLowerCornerOf(base.offset(8, 6, 6))).contains(Vec3.atCenterOf(pos)))
                    breaks.merge(pos.immutable(), 1, Integer::sum);
                return EventResult.pass();
            };
        }

        void start() {
            // Native navigation reads a48-block halo. Force exactly its bounded rectangle,
            // shared by every phase, rather than allocating a new large lane per crop.
            int minX = Math.floorDiv(base.getX() - 51, 16), maxX = Math.floorDiv(base.getX() + 55, 16);
            int minZ = Math.floorDiv(base.getZ() - 51, 16), maxZ = Math.floorDiv(base.getZ() + 53, 16);
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (!level.getForcedChunks().contains(chunk.toLong())) {
                    level.setChunkForced(x, z, true); ownedTickets.add(chunk);
                }
                GameTestChunkAwait.ticketed(test, chunk);
            }
            GameTestChunkAwait.entityTicking(test, base);
            GameTestChunkAwait.entityTicking(test, base.offset(1, 0, 2));
            for (int x : new int[]{-3, 7}) for (int z : new int[]{-3, 5})
                GameTestChunkAwait.entityTicking(test, base.offset(x, 0, z));
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-3, -2, -3), base.offset(7, 5, 5))) {
                test.assertTrue(level.getBlockEntity(p) == null, "compact crop fixture owns no foreign block entity at " + p);
                saved.put(p.immutable(), level.getBlockState(p));
            }
            touched = true;
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true, level.getServer());
            for (BlockPos p : saved.keySet()) {
                boolean wall = p.getX() == base.getX() - 3 || p.getX() == base.getX() + 7
                        || p.getZ() == base.getZ() - 3 || p.getZ() == base.getZ() + 5;
                level.setBlock(p, p.getY() == base.getY() - 1 || wall && p.getY() >= base.getY() && p.getY() < base.getY() + 3
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            level.setBlock(base.offset(1, 4, 1), Blocks.GLOWSTONE.defaultBlockState(), QUIET);
            level.setBlock(base.offset(4, 4, 0), Blocks.GLOWSTONE.defaultBlockState(), QUIET);
            plant(base.offset(4, 0, -2), Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, 2));
            plant(base.offset(4, 0, 0), Blocks.TORCHFLOWER_CROP.defaultBlockState().setValue(TorchflowerCropBlock.AGE, 1));
            pitcher(base.offset(4, 0, 2), 3);
            BlockEvent.BREAK.register(breakListener); registered = true;
            probe.deny(pos -> {
                if (phase == 4 && pos.equals(berry) && !denied) { denied = true; return true; }
                return false;
            });
            worker = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            worker.setGolemMaterial(6);
            worker.applyConfiguration(new GolemData(GolemData.HARVEST,
                    Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY),
                    Collections.nCopies(GolemData.FILTER_SLOTS, 0), false, List.of(), GolemUseOptions.DEFAULT,
                    List.of(GolemUpgrade.ORDER.id)));
            worker.setPos(Vec3.atBottomCenterOf(base.offset(1, 0, 2)));
            worker.setHome(base.offset(1, 0, 2), Direction.UP); worker.setPersistenceRequired();
            test.assertTrue(level.addFreshEntity(worker), "the real native Harvest/Order worker is admitted");
            setupPhase();
        }

        void plant(BlockPos at, BlockState crop) {
            level.setBlock(at.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), QUIET);
            level.setBlock(at, crop, QUIET);
        }
        void pitcher(BlockPos at, int age) {
            plant(at, Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE, age));
            level.setBlock(at.above(), Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE, age)
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), QUIET);
        }
        void seed(Item item) {
            supplied = new ItemEntity(level, base.getX() + .5, base.getY() + .15, base.getZ() + .5, new ItemStack(item));
            supplied.setDeltaMovement(Vec3.ZERO); supplied.setPickUpDelay(32767); supplied.setUnlimitedLifetime(); supplied.tickCount = 100;
            test.assertTrue(level.addFreshEntity(supplied), "the declared one real seed/pod is admitted");
        }
        List<ItemEntity> items() {
            return level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(Vec3.atLowerCornerOf(base.offset(-3, -2, -3)), Vec3.atLowerCornerOf(base.offset(8, 6, 6))), Entity::isAlive);
        }
        int count(Item item) { return items().stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum(); }

        void setupPhase() {
            items().forEach(Entity::discard); supplied = null;
            breaks.clear(); // The same cells held ordinary break-harvest crops in the preceding phases.
            for (BlockPos p : BlockPos.betweenClosed(base.offset(0, 0, 0), base.offset(2, 2, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), QUIET);
            for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), QUIET);
            switch (phase) {
                case 0 -> { plant(base, Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, 3)); seed(Items.BEETROOT_SEEDS); }
                case 1 -> { plant(base, Blocks.TORCHFLOWER.defaultBlockState()); seed(Items.TORCHFLOWER_SEEDS); }
                case 2 -> { pitcher(base, 4); seed(Items.PITCHER_POD); }
                case 3 -> { plant(base, Blocks.TORCHFLOWER.defaultBlockState()); pitcher(second, 4); }
                case 4 -> {
                    level.setBlock(berry.below(), Blocks.DIRT.defaultBlockState(), QUIET);
                    level.setBlock(berry, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3), QUIET);
                    level.setBlock(head.above(), Blocks.STONE.defaultBlockState(), QUIET);
                    level.setBlock(head, Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true), QUIET);
                    level.setBlock(body.above(), Blocks.STONE.defaultBlockState(), QUIET);
                    level.setBlock(body, Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, true), QUIET);
                    level.setBlock(body.below(), Blocks.CAVE_VINES.defaultBlockState(), QUIET);
                    level.setBlock(base.east().below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), QUIET);
                    seed(Items.TORCHFLOWER_SEEDS); // A retained berry must not trigger Order's neighbouring seed planting.
                }
                default -> throw new IllegalStateException("Unknown crop phase " + phase);
            }
            phaseStart = test.getTick();
        }

        boolean poll() {
            test.assertTrue(worker.isAlive() && worker.canWork(), "the actual worker stays live and admitted: " + worker.describeWorkState());
            test.assertTrue(level.getBlockState(base.offset(4, 0, -2)).is(Blocks.BEETROOTS)
                            && level.getBlockState(base.offset(4, 0, -2)).getValue(BeetrootBlock.AGE) == 2
                            && level.getBlockState(base.offset(4, 0, 0)).is(Blocks.TORCHFLOWER_CROP)
                            && level.getBlockState(base.offset(4, 0, 0)).getValue(TorchflowerCropBlock.AGE) == 1
                            && level.getBlockState(base.offset(4, 0, 2)).is(Blocks.PITCHER_CROP)
                            && level.getBlockState(base.offset(4, 0, 2)).getValue(PitcherCropBlock.AGE) == 3,
                    "native AI leaves immature beetroot, torchflower and double pitcher untouched");
            boolean done = switch (phase) {
                case 0 -> level.getBlockState(base).is(Blocks.BEETROOTS) && level.getBlockState(base).getValue(BeetrootBlock.AGE) == 0 && count(Items.BEETROOT) >= 1;
                case 1 -> level.getBlockState(base).is(Blocks.TORCHFLOWER_CROP) && count(Items.TORCHFLOWER) == 1;
                case 2 -> level.getBlockState(base).is(Blocks.PITCHER_CROP) && level.getBlockState(base).getValue(PitcherCropBlock.AGE) == 0
                        && level.getBlockState(base.above()).isAir() && count(Items.PITCHER_PLANT) == 1;
                case 3 -> level.getBlockState(base).isAir() && level.getBlockState(second).isAir()
                        && level.getBlockState(second.above()).isAir() && count(Items.TORCHFLOWER) == 1 && count(Items.PITCHER_PLANT) == 1;
                case 4 -> berriesDone();
                default -> false;
            };
            if (!done) {
                if (test.getTick() - phaseStart > 1000) test.fail("Modern crop phase stalled: " + describe());
                return false;
            }
            if (phase == 1 || phase == 2) test.assertTrue(supplied != null && !supplied.isAlive()
                            && count(phase == 1 ? Items.TORCHFLOWER_SEEDS : Items.PITCHER_POD) == 0,
                    "Order planted only by spending the declared real one seed/pod");
            Thaumcraft.LOG.info("Modern crop AI completed phase{} after{}ticks", phase, test.getTick() - phaseStart);
            if (++phase == 5) return true;
            setupPhase(); return false;
        }

        boolean berriesDone() {
            if (denied && !deniedChecked) {
                test.assertTrue(level.getBlockState(berry).getValue(SweetBerryBushBlock.AGE) == 3 && count(Items.SWEET_BERRIES) == 0,
                        "a real native USE protection veto leaves mature berries and yield untouched");
                deniedChecked = true;
            }
            if (level.getBlockState(berry).getValue(SweetBerryBushBlock.AGE) != 1
                    || level.getBlockState(head).getValue(CaveVines.BERRIES) || level.getBlockState(body).getValue(CaveVines.BERRIES)) return false;
            int sweet = count(Items.SWEET_BERRIES), glow = count(Items.GLOW_BERRIES);
            if (sweet < (regrown ? 4 : 2) || glow < (regrown ? 4 : 2)) return false;
            test.assertTrue(sweet <= (regrown ? 6 : 3) && glow == (regrown ? 4 : 2)
                            && breaks.getOrDefault(berry, 0) == 0 && breaks.getOrDefault(head, 0) == 0 && breaks.getOrDefault(body, 0) == 0,
                    "berry harvest retains plants and emits exactly native2..3sweet/oneglow per node without break hooks: sweet="
                            + sweet + ", glow=" + glow + ", regrown=" + regrown + ", breaks=" + breaks);
            test.assertTrue(deniedChecked && supplied != null && supplied.isAlive() && supplied.getItem().getCount() == 1
                            && level.getBlockState(base.east()).isAir(),
                    "berry picking was USE-protected and never spent a seed on neighbouring Order replanting");
            if (regrown) return true;
            // Declared second growth input, using the blocks' actual public native growth methods.
            var bush = (SweetBerryBushBlock) Blocks.SWEET_BERRY_BUSH;
            bush.performBonemeal(level, level.random, berry, level.getBlockState(berry));
            bush.performBonemeal(level, level.random, berry, level.getBlockState(berry));
            ((net.minecraft.world.level.block.CaveVinesBlock) Blocks.CAVE_VINES).performBonemeal(level, level.random, head, level.getBlockState(head));
            ((net.minecraft.world.level.block.CaveVinesPlantBlock) Blocks.CAVE_VINES_PLANT).performBonemeal(level, level.random, body, level.getBlockState(body));
            regrown = true; return false;
        }

        String describe() { return "phase=" + phase + " at=" + level.getBlockState(base) + " worker=" + (worker == null ? "none" : worker.describeWorkState()); }
        void finish() {
            if (finished) return; finished = true;
            try {
                if (registered) BlockEvent.BREAK.unregister(breakListener);
                probe.close(); if (worker != null) worker.discard();
                if (touched) { items().forEach(Entity::discard); saved.forEach((pos, state) -> level.setBlock(pos, state, QUIET)); }
                level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldRandomTicks, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(oldGriefing, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(oldDrops, level.getServer());
            } finally { ownedTickets.forEach(c -> level.setChunkForced(c.x, c.z, false)); }
        }
    }
}
