package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Real admitted Silverwood rituals; finite stock, no prepared-session injection. */
public final class InfusionRitualAuditChecks {
    private InfusionRitualAuditChecks() {}

    public static void comparator(GameTestHelper test) {
        var state = ThaumcraftInfusion.MATRIX.get().defaultBlockState();
        // Original shared BlockStoneDevice:291-305 advertises comparators even for matrix (value zero).
        test.assertTrue(state.hasAnalogOutputSignal(), "original matrix advertises analog output capability");
        try (var f = new Fixture(test, true)) {
            test.assertTrue(f.matrix.getBlockState().getAnalogOutputSignal(f.lane.level, f.matrix.getBlockPos()) == 0,
                    "idle matrix analog value is zero");
            f.start();
            test.assertTrue(f.matrix.getBlockState().getAnalogOutputSignal(f.lane.level, f.matrix.getBlockPos()) == 0,
                    "crafting matrix analog value remains zero, not a progress meter");
        }
        test.succeed();
    }

    public static void progressivePaymentAndFinish(GameTestHelper test) {
        try (var f = new Fixture(test, true)) {
            var first = f.jars.getLast();
            var aspect = first.getAspect();
            first.takeFromContainer(aspect, 5); // Four of the LAST nine units remain; other aspects can be paid before backoff.
            f.start();
            test.assertTrue(f.stock() == 58 && f.matrix.getDemand().visSize() == 63 && f.occupied() == 7,
                    "start reserves snapshots, consumes no physical input or essence");
            f.ticks(580);
            test.assertTrue(f.stock() == 0 && f.matrix.getDemand().visSize() == 5 && f.occupied() == 7,
                    "58 individual units paid before shortage, ingredients untouched");
            f.ticks(100);
            test.assertTrue(f.matrix.isCrafting() && f.matrix.getDemand().visSize() == 5 && f.occupied() == 7
                    && ItemStack.matches(f.input, f.center.getItem(0)), "shortfall stalls without reward, refund or ingredient debit");
            first.addToContainer(aspect, 5);
            // Clear the original real-time 5-second backoff, not payment/session state. This test drives ticks synchronously.
            EssentiaHandler.clear(f.lane.level);
            f.ticks(50);
            test.assertTrue(f.matrix.getDemand().visSize() == 0 && f.stock() == 0 && f.occupied() == 7,
                    "refill pays precisely the remaining five units");
            int initialInstability = f.matrix.getInstability();
            f.ticks(10);
            var update = f.matrix.getUpdateTag(f.lane.level.registryAccess());
            test.assertTrue(update.getBoolean("crafting") && update.getBoolean("active")
                    && update.contains("IngredientTarget") && update.getInt("IngredientCountdown") == 5,
                    "real beam target/countdown appear in client update payload");
            int consumed = 0;
            for (int cycle = 1; cycle <= 42; cycle++) {
                if (cycle > 1) f.ticks(10);
                test.assertTrue(f.matrix.getInstability() == initialInstability,
                        "successful ingredient cycles do not raise instability (original M474-487)");
                if (cycle % 6 == 0) consumed++;
                test.assertTrue(f.matrix.getRemainingIngredientCount() == 7 - consumed && f.occupied() == 7 - consumed,
                        "six cycles remove exactly one physical ingredient, cycle=" + cycle);
            }
            f.ticks(10);
            var expected = f.recipe.assemble(new InfusionRecipeInput(f.input, f.components()), f.lane.level.registryAccess());
            test.assertTrue(!f.matrix.isCrafting() && f.matrix.isActive() && f.matrix.getInstability() == 0
                    && ItemStack.matches(expected, f.center.getItem(0)) && f.occupied() == 0 && f.stock() == 0,
                    "one exact output including all components, seven inputs and 63 essence paid once");
            test.assertTrue(f.signal(f.center) > 0 && f.pedestals.stream().allMatch(p -> f.signal(p) == 0),
                    "central comparator sees reward; peripheral comparators see empty pedestals");
            update = f.matrix.getUpdateTag(f.lane.level.registryAccess());
            test.assertTrue(!update.getBoolean("crafting") && !update.contains("IngredientTarget"), "finish clears render target and crafting state");
            f.ticks(100);
            test.assertTrue(ItemStack.matches(expected, f.center.getItem(0)) && f.stock() == 0 && f.occupied() == 0,
                    "idle ticks cannot repeat reward or debit");
        }
        test.succeed();
    }

    public static void interruptions(GameTestHelper test) {
        try (var f = new Fixture(test, true)) {
            f.start();
            f.ticks(630);
            f.ticks(10);
            var target = f.matrix.getIngredientTarget();
            var pedestal = (TilePedestal) f.lane.level.getBlockEntity(target);
            var removed = pedestal.getItem(0).copy();
            test.assertTrue(f.lane.level.destroyBlock(target, true), "native peripheral pedestal break during beam");
            DeferredItemDrops.tick(f.lane.level);
            var drops = f.lane.level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(2));
            var ingredients = drops.stream().filter(e -> ItemStack.isSameItemSameComponents(e.getItem(), removed)).toList();
            test.assertTrue(ingredients.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1,
                    "broken pedestal evacuates exactly its one unconsumed ingredient");
            f.ticks(10);
            test.assertTrue(f.matrix.isCrafting() && ItemStack.matches(f.input, f.center.getItem(0))
                    && f.matrix.getRemainingIngredientCount() == 7, "missing pedestal does not complete or erase unpaid ingredient");
            test.assertTrue(f.matrix.getDemand().visSize() == 1, "missing first ingredient adds exactly one debt before trying later ingredients");
            // Stop the structure rather than ask hazards to leave everything intact.
            var corner = f.lane.origin.offset(-1, 0, -1);
            var lower = f.lane.level.getBlockState(corner);
            f.lane.put(corner, Blocks.AIR.defaultBlockState());
            f.ticks(20);
            var debt = f.matrix.getDemand().visSize();
            var remaining = f.matrix.getRemainingIngredientCount();
            var held = f.pedestals.stream().filter(p -> !p.isRemoved()).map(p -> p.getItem(0).copy()).toList();
            f.ticks(100);
            test.assertTrue(!f.matrix.isActive() && f.matrix.isCrafting() && f.matrix.getDemand().visSize() == debt
                    && f.matrix.getRemainingIngredientCount() == remaining, "broken structure suspends paid session, not restart/refund");
            int index = 0;
            for (var p : f.pedestals) if (!p.isRemoved()) test.assertTrue(ItemStack.matches(held.get(index++), p.getItem(0)),
                    "suspended ritual preserves each surviving pedestal");
            f.lane.put(corner, lower);
            f.matrix.setActive(true);
            test.assertTrue(f.matrix.isActive() && f.matrix.getDemand().visSize() == debt, "repair/reactivation retains debt");
            var stale = f.matrix;
            test.assertTrue(f.lane.level.destroyBlock(stale.getBlockPos(), true), "native crafting matrix destruction and original removal explosion");
            f.ticks(100);
            test.assertTrue(stale.isRemoved() && f.lane.level.getBlockEntity(stale.getBlockPos()) == null
                    && f.stock() == 0, "destroyed/stale matrix cannot drain or produce output");
            test.assertTrue(f.lane.level.getEntitiesOfClass(ItemEntity.class, f.lane.area(12)).stream()
                    .noneMatch(e -> e.getItem().is(f.recipe.result().getItem())), "matrix removal never emits prepared reward");
        }
        test.succeed();
    }

    public static void measuredInstability(GameTestHelper test) {
        int hazards = 0, expectedHazards = 0, increases = 0, expectedIncreases = 0;
        try (var f = new Fixture(test, false)) {
            // 1,000 independently admitted real rituals, one cycle each, seeds 0..999.
            // A stocked first aspect makes a skipped payment an unambiguous hazard indicator.
            for (int run = 0; run < 1000; run++) {
                f.reset(); f.start();
                int instability = f.matrix.getInstability();
                var oracle = RandomSource.create(run);
                boolean hazard = oracle.nextInt(500) <= instability;
                if (hazard) expectedHazards++;
                f.lane.level.random.setSeed(run);
                f.ticks(10);
                int paid = 63 - f.matrix.getDemand().visSize();
                if (paid == 0) hazards++;
                test.assertTrue(paid == (hazard ? 0 : 1), "original <=I /500 hazard roll versus actual payment, run=" + run);
                test.assertTrue(f.matrix.getInstability() == instability, "paid ingredient/essentia does not increase instability");
            }
            // 1,000 independent shortage cycles at the same naturally admitted I; all seven aspects absent.
            for (int run = 0; run < 1000; run++) {
                f.reset(); f.start();
                for (var jar : f.jars) jar.takeFromContainer(jar.getAspect(), jar.getAmount());
                int before = f.matrix.getInstability();
                var oracle = RandomSource.create(run);
                int expected = 0;
                if (oracle.nextInt(500) > before) for (int aspect = 0; aspect < 7; aspect++)
                    if (oracle.nextInt(85) == 0) expected++;
                expectedIncreases += expected;
                f.lane.level.random.setSeed(run);
                f.ticks(10);
                int actual = f.matrix.getInstability() - before;
                increases += actual;
                test.assertTrue(actual == expected && f.matrix.getDemand().visSize() == 63,
                        "seven failed aspects roll 1/(100-3*5), no completion, run=" + run);
            }
        }
        System.out.println("G24_INFUSION_MEASURE rituals=1000 hazards=" + hazards + " oracle=" + expectedHazards
                + " shortageRituals=1000 increases=" + increases + " oracleIncreases=" + expectedIncreases);
        test.succeed();
    }

    public static void measuredMissingIngredients(GameTestHelper test) {
        int debts = 0, increases = 0, expectedDebts = 0, expectedIncreases = 0;
        try (var f = new Fixture(test, true)) {
            for (int run = 0; run < 100; run++) {
                f.reset(); f.start(); f.ticks(630);
                test.assertTrue(f.matrix.getDemand().visSize() == 0 && f.matrix.getInstability() < 0,
                        "real stable ritual has paid all63 essence before ingredient-shortage measurement");
                // Move unpaid physical inputs into fixture escrow; no ingredient/session field injection.
                var escrow = f.pedestals.stream().map(p -> p.removeItemNoUpdate(0)).filter(s -> !s.isEmpty()).toList();
                test.assertTrue(escrow.size() == 7, "seven physical inputs retained in fixture escrow");
                int before = f.matrix.getInstability(), debt = 0, increase = 0;
                var oracle = RandomSource.create(run);
                for (int index = 0; index < 7; index++) if (oracle.nextInt(1 + index) == 0) {
                    debt++;
                    oracle.nextInt(7);
                    if (oracle.nextInt(40) == 0) increase++;
                }
                expectedDebts += debt; expectedIncreases += increase;
                f.lane.level.random.setSeed(run); f.ticks(10);
                debts += f.matrix.getDemand().visSize();
                increases += f.matrix.getInstability() - before;
                test.assertTrue(f.matrix.getDemand().visSize() == debt && f.matrix.getInstability() - before == increase
                        && f.matrix.isCrafting() && f.matrix.getRemainingIngredientCount() == 7
                        && ItemStack.matches(f.input, f.center.getItem(0)),
                        "missing index rolls 1/(1+index) debt and 1/(50-2*5) instability, run=" + run);
            }
        }
        System.out.println("G24_INFUSION_MISSING rituals=100 debts=" + debts + " oracleDebts=" + expectedDebts
                + " increases=" + increases + " oracleIncreases=" + expectedIncreases);
        test.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        final WaveLane lane;
        final GameTestHelper test;
        final net.minecraft.server.level.ServerPlayer player;
        final InfusionStackRecipe recipe;
        final ItemStack input = new ItemStack(ThaumcraftNaturals.SILVERWOOD_LOG.get());
        final List<TilePedestal> pedestals = new ArrayList<>();
        final List<TileJar> jars = new ArrayList<>();
        TileInfusionMatrix matrix;
        final TilePedestal center;

        Fixture(GameTestHelper test, boolean stabilize) {
            this.test = test;
            lane = new WaveLane(test, 3, 100, 3);
            player = test.makeMockServerPlayerInLevel();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.setPos(lane.origin.getX() + .5, lane.origin.getY() + 2, lane.origin.getZ() + 2);
            KnowledgeAccess.get(player).addResearch("ROD_silverwood");
            recipe = lane.level.getRecipeManager().getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get())
                    .stream().map(h -> h.value()).filter(r -> r.research().equals("ROD_silverwood")).findFirst().orElseThrow();
            lane.floor(7);
            if (stabilize) for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++)
                if (Math.abs(x) > 1 || Math.abs(z) > 1) lane.put(lane.origin.offset(x, 0, z), Blocks.SKELETON_SKULL.defaultBlockState());
            lane.put(lane.origin, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            center = (TilePedestal) lane.level.getBlockEntity(lane.origin);
            for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
                lane.put(lane.origin.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                lane.put(lane.origin.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            for (int x : new int[]{-3, 0, 3}) for (int z : new int[]{-3, 0, 3}) if (x != 0 || z != 0) {
                var pos = lane.origin.offset(x, 0, z);
                lane.put(pos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
                pedestals.add((TilePedestal) lane.level.getBlockEntity(pos));
            }
            int index = 0;
            for (var entry : recipe.aspects().entrySet()) {
                var pos = lane.origin.offset(-3 + index++, 0, 6);
                lane.put(pos, ThaumcraftContent.JAR.get().defaultBlockState());
                jars.add((TileJar) lane.level.getBlockEntity(pos));
            }
            reset();
        }

        List<ItemStack> components() { return recipe.components().stream().map(i -> i.getItems()[0].copyWithCount(1)).toList(); }
        void reset() {
            stop();
            EssentiaHandler.clear(lane.level);
            // Own only fixture-generated flux and drops; each trial has new raw stock.
            for (var p : pedestals) lane.put(p.getBlockPos().above(), Blocks.AIR.defaultBlockState());
            lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(12)).forEach(ItemEntity::discard);
            lane.put(lane.origin.above(2), Blocks.AIR.defaultBlockState());
            lane.put(lane.origin.above(2), ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            matrix = (TileInfusionMatrix) lane.level.getBlockEntity(lane.origin.above(2));
            center.setItem(0, input.copy());
            var items = components();
            for (int i = 0; i < pedestals.size(); i++) pedestals.get(i).setItem(0, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
            int i = 0;
            for (var entry : recipe.aspects().entrySet()) {
                var jar = jars.get(i++);
                if (jar.getAmount() > 0) jar.takeFromContainer(jar.getAspect(), jar.getAmount());
                jar.addToContainer(Aspect.getAspect(entry.getKey()), entry.getValue());
            }
            matrix.setActive(true);
        }
        void start() { test.assertTrue(matrix.startCrafting(player), "real registered Silverwood recipe admits"); }
        void ticks(int amount) { for (int i = 0; i < amount; i++) TileInfusionMatrix.tick(lane.level, matrix.getBlockPos(), matrix.getBlockState(), matrix); }
        int stock() { return jars.stream().mapToInt(TileJar::getAmount).sum(); }
        int occupied() { return (int) pedestals.stream().filter(p -> !p.isEmpty()).count(); }
        int signal(TilePedestal p) { return p.getBlockState().getAnalogOutputSignal(lane.level, p.getBlockPos()); }
        void stop() {
            if (matrix == null || matrix.isRemoved()) return;
            var tag = matrix.saveWithFullMetadata(lane.level.registryAccess());
            tag.putBoolean("active", false); tag.putBoolean("crafting", false);
            matrix.loadWithComponents(tag, lane.level.registryAccess());
        }
        @Override public void close() {
            stop();
            center.clearContent();
            for (var pedestal : pedestals) if (!pedestal.isRemoved()) pedestal.clearContent();
            DeferredItemDrops.tick(lane.level);
            lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(12)).forEach(ItemEntity::discard);
            EssentiaHandler.clear(lane.level);
            lane.clean();
            player.discard();
        }
    }
}
