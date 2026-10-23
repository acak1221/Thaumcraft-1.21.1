package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.tiles.TileNode;

/** Oracle TileNode606-694. Real registered nodes and production server ticks,
 * advanced synchronously in an isolated server GameTest, not elapsed client time.
 * Scoped world-RNG inputs are restored without advancing the original source. */
public final class NodeTimeAuditRuntimeChecks {
    private static final BlockPos AT = new BlockPos(2, 3, 2);
    private NodeTimeAuditRuntimeChecks() {}

    public static void curves(GameTestHelper test) {
        curve(test, null, 0, 600);
        curve(test, NodeModifier.BRIGHT, 0, 400);
        curve(test, NodeModifier.PALE, 0, 900);
        curve(test, NodeModifier.FADING, 0, 0);
        curve(test, null, 1, 1200);
        curve(test, null, 2, 12000);
        TileNode node = node(test, NodeModifier.FADING, 0,
                new AspectList().add(Aspect.AIR, 3).add(Aspect.PLANT, 7));
        node.takeFromContainer(Aspect.AIR, 3);
        List<Integer> bases = new ArrayList<>();
        try (var rng = new WorldRandom(test.getLevel(), bound -> bound == 20 ? 1 : 0)) {
            for (int i = 0; i < 3; i++) {
                ticks(node, 1200);
                bases.add(node.getNodeVisBase(Aspect.AIR));
                test.assertValueEqual(node.getNodeVisBase(Aspect.AIR), 2 - i, "depleted base loses exactly one per1200");
            }
        }
        System.out.println("NODE_TIME_AUDIT depleted base ticks1200,2400,3600=" + bases);
        test.assertTrue(!node.getAspects().aspects.containsKey(Aspect.AIR), "base0 prunes depleted current key");
        test.assertValueEqual(node.getAspects().getAmount(Aspect.PLANT), 7, "stocked compound does not passively decay");

        // No DIM exists in TC4: PALE is a modifier, not a node type. A forced
        // depleted-key removal can degrade NORMAL -> PALE -> FADING in ONE poll.
        node = node(test, null, 2, new AspectList().add(Aspect.AIR, 3).add(Aspect.PLANT, 7));
        node.takeFromContainer(Aspect.AIR, 3);
        try (var rng = new WorldRandom(test.getLevel(), bound -> 0)) { ticks(node, 1200); }
        test.assertTrue(node.getNodeModifier() == NodeModifier.FADING && node.getNodeType() == NodeType.NORMAL,
                "1/20 prune then1/5 degradation then1/5 fading; type stays NORMAL");
        System.out.println("NODE_TIME_AUDIT forced prune/degrade tick1200 base=" + node.getNodeVisBase(Aspect.AIR)
                + " currentKeys=" + node.getAspects().size() + " type=" + node.getNodeType() + " modifier=" + node.getNodeModifier());
        for (int lock : new int[]{1, 2}) {
            node = node(test, null, lock, new AspectList().add(Aspect.AIR, 100));
            node.setNodeType(NodeType.UNSTABLE);
            try (var rng = new WorldRandom(test.getLevel(), bound -> 42, true)) {
                ticks(node, 99);
                // Lock 2 still discharges (oracle TileNode:551 excludes only lock 1), and a
                // discharge draws six bound-5 offsets per second update. Those are not
                // stability draws, so the check filters them instead of demanding an empty log.
                boolean stabilityDrew = rng.bounds.stream().anyMatch(bound -> bound == 10000 / lock);
                test.assertTrue(node.getNodeType() == NodeType.UNSTABLE && !stabilityDrew,
                        "no stability roll before100");
                ticks(node, 1);
                // The repair is one draw at bound 10000/lock; a lock-2 node also logs its
                // discharge offsets (bound 5), so the stability draw is isolated by bound.
                long stabilityDraws = rng.bounds.stream().filter(bound -> bound == 10000 / lock).count();
                test.assertTrue(node.getNodeType() == NodeType.NORMAL && stabilityDraws == 1,
                        "UNSTABLE repair needs world coin then42 at10000/lock");
                System.out.println("NODE_TIME_AUDIT stability lock=" + lock + " tick100 UNSTABLE->NORMAL bound=" + rng.bounds);
            }
            node = node(test, NodeModifier.FADING, lock, new AspectList().add(Aspect.AIR, 100));
            try (var rng = new WorldRandom(test.getLevel(), bound -> 69)) {
                ticks(node, 100);
                long fadingDraws = rng.bounds.stream().filter(bound -> bound == 12500 / lock).count();
                test.assertTrue(node.getNodeModifier() == NodeModifier.PALE && fadingDraws == 1
                        && node.getRegenerationTicks() == 0, "FADING repair69 preserves original stale-zero recharge cache");
                System.out.println("NODE_TIME_AUDIT repair lock=" + lock + " tick100 FADING->PALE bound=" + rng.bounds + " cachedInterval=0");
            }
        }
        test.setBlock(AT, Blocks.AIR);
        test.setBlock(AT.below(), Blocks.AIR);
        test.succeed();
    }

    private static void curve(GameTestHelper test, NodeModifier modifier, int lock, int interval) {
        TileNode node = node(test, modifier, lock, new AspectList().add(Aspect.AIR, 100));
        node.takeFromContainer(Aspect.AIR, 80);
        int[] checkpoints = lock == 2 ? new int[]{0, 11999, 12000, 23999, 24000}
                : new int[]{0, 399, 400, 599, 600, 899, 900, 1199, 1200, 1800, 2400, 3600};
        List<Integer> measured = new ArrayList<>();
        try (var rng = new WorldRandom(test.getLevel(), bound -> bound == 20 ? 1 : 0)) {
            int previous = 0;
            for (int tick : checkpoints) {
                ticks(node, tick - previous); previous = tick;
                int current = node.getAspects().getAmount(Aspect.AIR);
                measured.add(current);
                test.assertValueEqual(current, 20 + (interval == 0 ? 0 : tick / interval), "oracle recharge curve tick" + tick);
                test.assertValueEqual(node.getNodeVisBase(Aspect.AIR), 100, "modifier/elapsed time never scales ordinary base");
            }
            // Oracle TileNode.handleDischarge:551 excludes ONLY lock 1 ("getLock() == 1"),
            // not lock 2 - an advanced stabilizer does NOT stop its source node from
            // discharging. So the lock-2 curve also draws the six discharge offsets every
            // second update (24000 / 2 * 6 = 72000). The recharge selection adds one draw per
            // successful refill. The assertion therefore counts both, per lock, instead of
            // assuming locks block discharge.
            int dischargeDraws = lock == 2 ? (checkpoints[checkpoints.length - 1] / 2) * 6 : 0;
            int refillDraws = interval == 0 ? 0 : checkpoints[checkpoints.length - 1] / interval;
            int expectedDraws = dischargeDraws + refillDraws;
            test.assertValueEqual(rng.bounds.size(), expectedDraws,
                    "world draws are the oracle's discharge offsets plus one per successful refill");
            // Recharge has no chance roll (bound 1 is the depleted-key choice); a lock-2 node
            // additionally draws six bound-5 discharge offsets per second update.
            test.assertTrue(rng.bounds.stream().allMatch(bound -> bound == 1 || (lock == 2 && bound == 5)),
                    "recharge draws only the depleted-key choice; lock 2 adds the oracle's discharge offsets");
            System.out.println("NODE_TIME_AUDIT modifier=" + modifier + " lock=" + lock + " ticks="
                    + java.util.Arrays.toString(checkpoints) + " vis=" + measured + " worldDraws=" + rng.bounds.size());
        }
    }

    /** Regression: original setNodeVisBase(short -1) calls AspectList.reduce,
     * which refuses the overdraw at base0 instead of writing negative capacity. */
    public static void zeroCapacityDecay(GameTestHelper test) {
        TileNode node = node(test, NodeModifier.FADING, 0,
                new AspectList().add(Aspect.AIR, 1).add(Aspect.PLANT, 7));
        node.setNodeVisBase(Aspect.AIR, (short) 0);
        node.takeFromContainer(Aspect.AIR, 1);
        test.assertValueEqual(node.getNodeVisBase(Aspect.AIR), 0, "explicit zero capacity with retained zero current");
        try (var rng = new WorldRandom(test.getLevel(), bound -> bound == 20 ? 1 : 0)) {
            ticks(node, 1199);
            test.assertValueEqual(node.getNodeVisBase(Aspect.AIR), 0, "zero base unchanged through1199");
            ticks(node, 1);
        }
        int actual = node.getNodeVisBase(Aspect.AIR);
        System.out.println("NODE_TIME_AUDIT zero-base tick1200 actual=" + actual + " oracle=0");
        test.assertValueEqual(actual, 0, "oracle refuses base0 minus1 overdraw; never persist negative capacity");
        test.assertTrue(!node.getAspects().aspects.containsKey(Aspect.AIR), "zero-base aspect pruned");
        test.setBlock(AT, Blocks.AIR);
        test.succeed();
    }

    public static void growth(GameTestHelper test) {
        for (NodeModifier modifier : new NodeModifier[]{null, NodeModifier.BRIGHT, NodeModifier.PALE}) {
            test.setBlock(AT.east(), Blocks.AIR);
            test.setBlock(AT, Blocks.AIR);
            test.setBlock(AT.below(), Blocks.AIR);
            test.setBlock(AT.east().below(), Blocks.AIR);
            test.setBlock(AT, ThaumcraftContent.AURA_NODE.get());
            test.setBlock(AT.east(), ThaumcraftContent.AURA_NODE.get());
            TileNode strong = test.getBlockEntity(AT), weak = test.getBlockEntity(AT.east());
            strong.initializeNatural(new AspectList().add(Aspect.AIR, 100), NodeType.NORMAL, modifier);
            weak.initializeNatural(new AspectList().add(Aspect.AIR, 5), NodeType.NORMAL, null);
            ticks(weak, 1); // Cache victim regeneration600 before it is bullied.
            int[] offsetDraw = {0};
            try (var rng = new WorldRandom(test.getLevel(), bound -> bound == 5 && offsetDraw[0]++ == 0 ? 1 : 0)) {
                int n = modifier == NodeModifier.BRIGHT ? 1 : modifier == NodeModifier.PALE ? 3 : 2;
                ticks(strong, n);
                int growthBound = modifier == NodeModifier.BRIGHT ? 67 : 101;
                List<Integer> expected = new ArrayList<>(List.of(5, 5, 5, 5, 5, 5, 1, growthBound));
                if (modifier == NodeModifier.PALE) expected.add(100);
                expected.add(3);
                test.assertTrue(rng.bounds.equals(expected), "oracle world RNG discharge/growth bounds " + rng.bounds);
                test.assertValueEqual(strong.getNodeVisBase(Aspect.AIR), 101, "successful growth adds one base, not current");
                test.assertValueEqual(strong.getAspects().getAmount(Aspect.AIR), 100, "full strong node does not get free current from growth");
                test.assertValueEqual(weak.getAspects().getAmount(Aspect.AIR), 4, "one discharge consumes exactly one victim vis");
                test.assertValueEqual(weak.getNodeVisBase(Aspect.AIR), 4, "1/3 growth branch removes one victim base");
                test.assertValueEqual(weak.getRechargeWait(), 300, "victim recharge delay is cached interval/2");
                test.assertTrue(strong.getNodeType() == NodeType.NORMAL
                        && strong.getNodeModifier() == (modifier == NodeModifier.PALE ? null : modifier),
                        "growth changes only PALE modifier on1/100 roll, never node type");
                System.out.println("NODE_TIME_AUDIT growth modifier=" + modifier + " tick=" + n
                        + " strongCurrent/base=100/101 weakCurrent/base=4/4 wait=300 worldBounds=" + rng.bounds);
            }
        }
        test.setBlock(AT, Blocks.AIR);
        test.setBlock(AT.east(), Blocks.AIR);
        test.succeed();
    }

    private static TileNode node(GameTestHelper test, NodeModifier modifier, int lock, AspectList base) {
        test.setBlock(AT, Blocks.AIR);
        test.setBlock(AT.below(), lock == 1 ? ThaumcraftNodeDevices.NODE_STABILIZER.get()
                : lock == 2 ? ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER.get() : Blocks.AIR);
        // A real silverwood-knot BE isolates unlocked recharge from discharge RNG.
        test.setBlock(AT, lock == 0 ? ThaumcraftContent.SILVERWOOD_KNOT.get() : ThaumcraftContent.AURA_NODE.get());
        TileNode node = test.getBlockEntity(AT);
        node.initializeNatural(base, NodeType.NORMAL, modifier);
        return node;
    }

    private static void ticks(TileNode node, int ticks) {
        for (int i = 0; i < ticks; i++) TileNode.serverTick(node.getLevel(), node.getBlockPos(), node.getBlockState(), node);
    }

    private static final class WorldRandom implements AutoCloseable {
        final ServerLevel level;
        final Field field;
        final RandomSource original;
        final List<Integer> bounds = new ArrayList<>();
        WorldRandom(ServerLevel level, IntUnaryOperator values) {
            this(level, values, false);
        }
        WorldRandom(ServerLevel level, IntUnaryOperator values, boolean coin) {
            this.level = level; original = level.random;
            RandomSource input = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                    new Class<?>[]{RandomSource.class}, (proxy, method, args) -> {
                        if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                            int bound = (Integer) args[0]; bounds.add(bound); return values.applyAsInt(bound);
                        }
                        if (method.getName().equals("nextBoolean")) return coin;
                        if (method.getName().equals("toString")) return "node-time-audit world input";
                        throw new AssertionError("Unexpected node RNG call " + method);
                    });
            try { field = Level.class.getDeclaredField("random"); field.setAccessible(true); field.set(level, input); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        @Override public void close() {
            try { field.set(level, original); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }
    }
}
