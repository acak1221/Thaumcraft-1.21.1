package thaumcraft.common.testing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemFishingLoot;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * Order, Entropy and Air on a real Fishing golem's catch: the golem's own upgrades, read from real golems, move the junk and treasure
 * windows (original AIFish:183-184), double the catch (:118-120) and, with Fire, cook it. The random source is injected, so every
 * window edge is exact: real golems, the real level and registries, and the scripted random numbers are the INPUTS; which pool the
 * result comes from, its item and the number of results are never supplied. The last part draws thousands of results from the real
 * loot generation with a seeded random and checks the shares of each pool. This is the single call the Fishing goal makes per catch
 * ({@code GolemFishingLoot.nextCatch}, {@code catchCount}); a live cast is a separate test.
 */
public final class GolemFishingUpgradesRuntimeChecks {
    private static final int TALLOW = 2;
    private static final int AIR = GolemUpgrade.AIR.id, FIRE = GolemUpgrade.FIRE.id, ORDER = GolemUpgrade.ORDER.id, ENTROPY = GolemUpgrade.ENTROPY.id;
    private static final GolemFishingLoot.Pond OPEN = new GolemFishingLoot.Pond(0, 0, 0);
    private static final GolemFishingLoot.Pond LAKE = new GolemFishingLoot.Pond(4, 4, 12);

    private GolemFishingUpgradesRuntimeChecks() {}

    /** Random numbers in order, then a fixed fallback: floats come from a seeded source, bounded ints are 0 (the first entry of a pool). */
    private static final class Scripted implements RandomSource {
        private final RandomSource base = RandomSource.create(7);
        final ArrayDeque<Float> floats = new ArrayDeque<>();
        final ArrayDeque<Integer> ints = new ArrayDeque<>();
        int lastBound = -1;

        Scripted roll(float... values) { for (float value : values) floats.add(value); return this; }
        Scripted draw(int... values) { for (int value : values) ints.add(value); return this; }
        @Override public RandomSource fork() { return base.fork(); }
        @Override public PositionalRandomFactory forkPositional() { return base.forkPositional(); }
        @Override public void setSeed(long seed) { base.setSeed(seed); }
        @Override public int nextInt() { return ints.isEmpty() ? 0 : ints.pop(); }
        @Override public int nextInt(int bound) { lastBound = bound; return ints.isEmpty() ? 0 : ints.pop(); }
        @Override public long nextLong() { return base.nextLong(); }
        @Override public boolean nextBoolean() { return base.nextBoolean(); }
        @Override public float nextFloat() { return floats.isEmpty() ? base.nextFloat() : floats.pop(); }
        @Override public double nextDouble() { return base.nextDouble(); }
        @Override public double nextGaussian() { return base.nextGaussian(); }
    }

    private enum Pool { JUNK, TREASURE, FISH, OTHER }

    /** The first entry of each pool is unique to it: leather boots, lily pad, cod. */
    private static Pool pool(ItemStack stack) {
        if (stack.is(Items.LEATHER_BOOTS)) return Pool.JUNK;
        if (stack.is(Items.LILY_PAD)) return Pool.TREASURE;
        if (stack.is(Items.COD)) return Pool.FISH;
        return Pool.OTHER;
    }

    /** Classification of any result, for the sampled shares. A fishing rod is junk when plain and treasure when enchanted. */
    private static Pool poolOfAny(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.COD || item == Items.SALMON || item == Items.TROPICAL_FISH || item == Items.PUFFERFISH) return Pool.FISH;
        if (item == Items.LILY_PAD || item == Items.NAME_TAG || item == Items.SADDLE || item == Items.BOW || item == Items.ENCHANTED_BOOK) return Pool.TREASURE;
        if (item == Items.FISHING_ROD) return stack.isEnchanted() ? Pool.TREASURE : Pool.JUNK;
        return Pool.JUNK;
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var base = test.absolutePos(new BlockPos(3, 3, 3));
        var spawned = new ArrayList<Entity>();
        try {
            var plain = fishing(test, level, List.of(), base, spawned);
            var order1 = fishing(test, level, List.of(ORDER), base.east(2), spawned);
            var order2 = fishing(test, level, List.of(ORDER, ORDER), base.east(4), spawned);
            var entropy1 = fishing(test, level, List.of(ENTROPY), base.east(6), spawned);
            var entropy2 = fishing(test, level, List.of(ENTROPY, ENTROPY), base.east(8), spawned);
            var both = fishing(test, level, List.of(ENTROPY, ORDER), base.east(10), spawned);
            var air1 = fishing(test, level, List.of(AIR), base.east(12), spawned);
            var air2 = fishing(test, level, List.of(AIR, AIR), base.east(14), spawned);
            var fire = fishing(test, level, List.of(FIRE), base.east(16), spawned);
            var airFire = fishing(test, level, List.of(AIR, FIRE), base.east(18), spawned);
            test.assertTrue(order2.getUpgradeAmount(GolemUpgrade.ORDER) == 2 && entropy2.getUpgradeAmount(GolemUpgrade.ENTROPY) == 2
                            && air2.getUpgradeAmount(GolemUpgrade.AIR) == 2 && plain.getUpgradeAmount(GolemUpgrade.ORDER) == 0,
                    "ten real Fishing golems carry the upgrades they were configured with");

            // ---- Order raises the treasure window [junk, junk + treasure): plain 0.10-0.15, one Order 0.10-0.1625, two 0.10-0.175 ----
            expect(test, level, OPEN, plain, 0.0F, Pool.JUNK, "plain, bottom of the junk window");
            expect(test, level, OPEN, plain, 0.099F, Pool.JUNK, "plain, top of the junk window");
            expect(test, level, OPEN, plain, 0.101F, Pool.TREASURE, "plain, bottom of the treasure window");
            expect(test, level, OPEN, plain, 0.149F, Pool.TREASURE, "plain, top of the treasure window");
            expect(test, level, OPEN, plain, 0.16F, Pool.FISH, "plain, a roll just above the window is a fish");
            expect(test, level, OPEN, plain, 0.99F, Pool.FISH, "plain, top of the range");
            expect(test, level, OPEN, order1, 0.16F, Pool.TREASURE, "one Order turns that same roll into treasure");
            expect(test, level, OPEN, order1, 0.17F, Pool.FISH, "but its window ends at 0.1625");
            expect(test, level, OPEN, order2, 0.17F, Pool.TREASURE, "two Order reach 0.175");
            expect(test, level, OPEN, order2, 0.18F, Pool.FISH, "and no further");
            expect(test, level, OPEN, order2, 0.099F, Pool.JUNK, "Order never moves the junk window");
            expect(test, level, OPEN, order2, 0.0F, Pool.JUNK, "nor its start");

            // ---- Entropy shrinks junk to 0.075 / 0.05 and slides the (unchanged width) treasure window down with it ----
            expect(test, level, OPEN, entropy1, 0.07F, Pool.JUNK, "one Entropy: 0.07 is still junk");
            expect(test, level, OPEN, entropy1, 0.08F, Pool.TREASURE, "one Entropy: 0.08 is already treasure (plain: junk)");
            expect(test, level, OPEN, plain, 0.08F, Pool.JUNK, "the plain golem rolls that junk");
            expect(test, level, OPEN, entropy2, 0.04F, Pool.JUNK, "two Entropy: junk ends at 0.05");
            expect(test, level, OPEN, entropy2, 0.06F, Pool.TREASURE, "two Entropy: 0.06 is treasure");
            expect(test, level, OPEN, entropy2, 0.11F, Pool.FISH, "two Entropy: the treasure window moved to 0.05-0.10, so 0.11 is a fish (plain: treasure)");
            expect(test, level, OPEN, plain, 0.11F, Pool.TREASURE, "the plain golem rolls that treasure");
            expect(test, level, OPEN, entropy2, 0.16F, Pool.FISH, "Entropy does not raise the treasure chance");

            // ---- both on one golem, and a pond that adds its own bonus ----
            expect(test, level, OPEN, both, 0.08F, Pool.TREASURE, "Entropy + Order, 0.08");
            expect(test, level, OPEN, both, 0.14F, Pool.FISH, "Entropy + Order: junk 0.075 + treasure 0.0625 = 0.1375, so 0.14 is a fish");
            expect(test, level, LAKE, plain, 0.07F, Pool.TREASURE, "a lake (junk 0.06, treasure 0.072) with no upgrades");
            expect(test, level, LAKE, plain, 0.14F, Pool.FISH, "plain lake, 0.14 is a fish (junk 0.06 + treasure 0.072 = 0.132)");
            expect(test, level, LAKE, order2, 0.14F, Pool.TREASURE, "the same lake with two Order: treasure 0.097 reaches 0.157");
            expect(test, level, LAKE, plain, 0.02F, Pool.JUNK, "plain lake, 0.02 is junk (junk 0.06)");
            expect(test, level, LAKE, entropy2, 0.02F, Pool.TREASURE, "the same lake with two Entropy: junk 0.01, so 0.02 is treasure");

            // ---- Air: a second catch with probability Air / 10, and no random number is drawn without Air ----
            for (var golem : List.of(plain, order1, order2, entropy2, both, fire)) {
                catches(test, golem, 0, 1, false, "no Air");
            }
            catches(test, air1, 0, 2, true, "one Air, draw 0");
            catches(test, air1, 1, 1, true, "one Air, draw 1");
            catches(test, air1, 9, 1, true, "one Air, draw 9");
            catches(test, air2, 0, 2, true, "two Air, draw 0");
            catches(test, air2, 1, 2, true, "two Air, draw 1");
            catches(test, air2, 2, 1, true, "two Air, draw 2");
            catches(test, air2, 9, 1, true, "two Air, draw 9");
            catches(test, airFire, 0, 2, true, "Air + Fire, draw 0");

            // ---- Fire cooks every result of the catch, through the same call; junk and treasure are not cookable ----
            ItemStack cooked = GolemFishingLoot.nextCatch(level, new Scripted().roll(0.5F), fire, OPEN);
            test.assertTrue(cooked.is(Items.COOKED_COD) && cooked.getCount() == 1, "Fire: a rolled cod is delivered cooked: " + cooked);
            test.assertTrue(GolemFishingLoot.nextCatch(level, new Scripted().roll(0.5F), plain, OPEN).is(Items.COD), "no Fire: the cod stays raw");
            test.assertTrue(GolemFishingLoot.nextCatch(level, new Scripted().roll(0.5F), airFire, OPEN).is(Items.COOKED_COD), "Air + Fire: cooked too");
            expect(test, level, OPEN, fire, 0.0F, Pool.JUNK, "Fire leaves junk alone");
            expect(test, level, OPEN, fire, 0.12F, Pool.TREASURE, "Fire leaves treasure alone");

            sampledShares(test, level, plain, order2, entropy2);
        } finally {
            for (Entity entity : spawned) entity.discard();
        }
        test.succeed();
    }

    private static void catches(GameTestHelper test, EntityGolemBase golem, int draw, int expected, boolean drawn, String why) {
        var random = new Scripted().draw(draw);
        int count = GolemFishingLoot.catchCount(random, golem);
        test.assertTrue(count == expected && random.ints.isEmpty() == drawn && (!drawn || random.lastBound == 10),
                why + ": expected " + expected + " catch(es) with the draw " + (drawn ? "taken from bound 10" : "untouched") + ", got " + count
                        + ", draw left " + random.ints.size() + ", bound " + random.lastBound);
    }

    /** Thousands of results from the real loot generation: each upgrade moves only its own pool's share. */
    private static void sampledShares(GameTestHelper test, ServerLevel level, EntityGolemBase plain, EntityGolemBase order2, EntityGolemBase entropy2) {
        int samples = 12000;
        double[] junk = new double[3], treasure = new double[3];
        var golems = List.of(plain, order2, entropy2);
        for (int index = 0; index < golems.size(); index++) {
            var random = RandomSource.create(20261019L);
            int junkCount = 0, treasureCount = 0;
            for (int sample = 0; sample < samples; sample++) {
                switch (poolOfAny(GolemFishingLoot.nextCatch(level, random, golems.get(index), OPEN))) {
                    case JUNK -> junkCount++;
                    case TREASURE -> treasureCount++;
                    default -> { }
                }
            }
            junk[index] = junkCount / (double) samples;
            treasure[index] = treasureCount / (double) samples;
        }
        String shares = "junk " + java.util.Arrays.toString(junk) + ", treasure " + java.util.Arrays.toString(treasure);
        test.assertTrue(Math.abs(junk[0] - 0.10) < 0.012 && Math.abs(treasure[0] - 0.05) < 0.010, "plain golem: 10 % junk, 5 % treasure: " + shares);
        test.assertTrue(Math.abs(treasure[1] - 0.075) < 0.010 && Math.abs(junk[1] - 0.10) < 0.012, "two Order: treasure 7.5 %, junk unchanged: " + shares);
        test.assertTrue(Math.abs(junk[2] - 0.05) < 0.010 && Math.abs(treasure[2] - 0.05) < 0.010, "two Entropy: junk 5 %, treasure unchanged: " + shares);
        test.assertTrue(treasure[1] > treasure[0] + 0.015 && junk[2] < junk[0] - 0.03, "and the differences themselves are well outside the noise: " + shares);
    }

    private static void expect(GameTestHelper test, ServerLevel level, GolemFishingLoot.Pond pond, EntityGolemBase golem, float roll, Pool expected, String why) {
        var random = new Scripted().roll(roll);
        ItemStack result = GolemFishingLoot.nextCatch(level, random, golem, pond);
        Pool actual = pool(result);
        test.assertTrue(actual == expected && random.floats.isEmpty(), why + ": roll " + roll + " gave " + result + " (" + actual + "), expected " + expected);
    }

    private static EntityGolemBase fishing(GameTestHelper test, ServerLevel level, List<Integer> upgrades, BlockPos at, List<Entity> spawned) {
        var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
        golem.setGolemMaterial(TALLOW);
        golem.applyConfiguration(new GolemData(GolemData.FISHING, Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY),
                Collections.nCopies(GolemData.FILTER_SLOTS, 0), false, List.of(), GolemUseOptions.DEFAULT, upgrades));
        golem.setPos(Vec3.atBottomCenterOf(at));
        golem.setHome(at, Direction.UP);
        test.assertTrue(level.addFreshEntity(golem), "the Fishing golem is admitted");
        spawned.add(golem);
        return golem;
    }
}
