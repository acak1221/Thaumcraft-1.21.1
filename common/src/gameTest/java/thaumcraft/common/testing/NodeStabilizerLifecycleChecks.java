package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileNode;

/** Native NORMAL-node M09a checks. Lists/modifiers, research, funded wands and the
 * explicitly scoped deterministic RandomSource are SERVER INPUT fixtures, not
 * wild rolls or production/client RNG seeds. Only actual production tick calls
 * advance phase; no counter/lock/cache/current-clock setter completes an action.
 * Real same-location loadStatic replacement is NOT chunk unload or disk restart. */
public final class NodeStabilizerLifecycleChecks {
    private NodeStabilizerLifecycleChecks() {}

    /** Suggested Fabric wrapper timeout: 240 ticks; all production calls are synchronous. */
    public static void lockPhaseDischargeAndWand(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            lockPhase(f);
            discharge(f);
            replacementDuringSampling(f);
            wandDrain(f);
            minimumHeight(f);
        }
        test.succeed();
    }

    /** Suggested Fabric wrapper timeout: 240 ticks; not 1800 elapsed client ticks. */
    public static void regenerationRepairAndPersistence(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            regenerationPair(f, null, 600);
            regenerationPair(f, TileNode.NodeModifier.BRIGHT, 400);
            regenerationPair(f, TileNode.NodeModifier.PALE, 900);
            lockChangeBeforeRecharge(f);
            fadingRepair(f);
            historicalCatchUp(f);
            persistenceAndStaleOwners(f);
        }
        test.succeed();
    }

    private static void lockPhase(Fixture f) {
        f.reset();
        var node = node(f, f.base, new AspectList().add(Aspect.AIR, 10), null);
        basic(f, f.base.below());
        try (var random = InputRandom.zeroes(f)) {
            pureReads(f, node, 0, 0, -1);
            tick(f, node);
            f.check(node.getTickCount() == 1 && node.getLock() == 1 && node.getRegenerationTicks() == 1200,
                    "++count BEFORE first lock poll; basic support affects actual first recharge cache");
            ticks(f, node, 48);
            f.check(node.getTickCount() == 49 && node.getLock() == 1, "real native phase49, never a counter setter");
            // Original checkLock matches the BLOCK/metadata, NOT a required TE.
            f.level.removeBlockEntity(f.base.below());
            // Level.getBlockEntity would lazily CREATE the missing native BE.
            // Observe the already-loaded chunk map without changing the fixture.
            var supportEntities = f.level.getChunkAt(f.base.below()).getBlockEntities();
            f.check(f.level.getBlockState(f.base.below()).is(ThaumcraftNodeDevices.NODE_STABILIZER.get())
                            && !supportEntities.containsKey(f.base.below()),
                    "explicit original-BLOCK-without-BE input, not a different support block");
            pureReads(f, node, 49, 1, 1200);
            tick(f, node);
            f.check(node.getLock() == 1 && node.getRegenerationTicks() == 1200 && !supportEntities.containsKey(f.base.below()),
                    "phase50 still locks from registered BASIC BLOCK with no supporting BE");
            random.expectOnlyInts(0, 5, "locked origin emits NO world-RNG draws through50");
        }

        power(f, node, true);
        try (var random = InputRandom.zeroes(f)) {
            pureReads(f, node, 50, 1, 1200);
            ticks(f, node, 49);
            pureReads(f, node, 99, 1, 1200);
            random.expectOnlyInts(0, 5, "power change does not eagerly refresh cached lock before100");
            tick(f, node);
            f.check(node.getLock() == 0 && node.getRegenerationTicks() == 600,
                    "phase100 powered BASIC BLOCK unlocks and invalidates/recomputes old doubled cache");
            random.expectOnlyInts(6, 5, "newly unlocked normal origin samples six offsets on the SAME phase100 tick");
            pureReads(f, node, 100, 0, 600);
            random.expectOnlyInts(6, 5, "cached getter never repolls power or consumes another RNG draw");
        }
        power(f, node, false);
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 49);
            pureReads(f, node, 149, 0, 600);
            random.expectOnlyInts(24 * 6, 5, "unpowered support remains cached unlocked through149");
            tick(f, node);
            f.check(node.getLock() == 1 && node.getRegenerationTicks() == 1200, "phase150 restores real doubled interval");
            random.expectOnlyInts(24 * 6, 5, "phase150 basic lock returns BEFORE offset RNG");
        }
        f.put(f.base.below(), Blocks.AIR);
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 49);
            pureReads(f, node, 199, 1, 1200);
            random.expectOnlyInts(0, 5, "removed support retains the original full cached phase150..199 policy");
            tick(f, node);
            f.check(node.getLock() == 0 && node.getRegenerationTicks() == 600, "phase200 actually sees removed support");
            random.expectOnlyInts(6, 5, "support-removal phase200 resumes ordinary offset sampling");
        }
        f.put(f.base.below(), ThaumcraftMasonry.ARCANE_STONE_BRICKS.get());
        basic(f, f.base.east());
        basic(f, f.base.above());
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 50);
            f.check(node.getTickCount() == 250 && node.getLock() == 0 && node.getRegenerationTicks() == 600,
                    "wrong stone device below and BASIC blocks sideways/above are not immediate-below basic support");
            random.expectOnlyInts(25 * 6, 5, "ordinary wrong-support policy still performs real sampling");
        }
        basic(f, f.base.below());
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 49);
            pureReads(f, node, 299, 0, 600);
            tick(f, node);
            f.check(node.getLock() == 1 && node.getRegenerationTicks() == 1200,
                    "native support swap activates only at300, never on getter/block callback");
            random.expectOnlyInts(24 * 6, 5, "phase300 stops RNG before sampling while old phases still sample");
        }
    }

    private static void discharge(Fixture f) {
        // Both strong and weak locked origins are protected. These are actual
        // installed nodes, not two non-RNG branches standing in for successful bullying.
        for (int lockedSize : new int[] {10, 100}) {
            f.reset();
            var locked = node(f, f.base, new AspectList().add(Aspect.AIR, lockedSize), null);
            var other = node(f, f.base.east(), new AspectList().add(Aspect.AIR, lockedSize == 10 ? 100 : 10), null);
            basic(f, locked.getBlockPos().below());
            var before = snapshot(f, locked);
            var otherBefore = snapshot(f, other);
            try (var random = InputRandom.zeroes(f)) {
                ticks(f, locked, 2);
                random.expectOnlyInts(0, 5, "basic locked origin skips ALL discharge RNG for size" + lockedSize);
                f.check(locked.getLock() == 1 && sameStock(f, locked, before) && sameStock(f, other, otherBefore),
                        "both strong and weak locked origins preserve all actual current/base/identity metadata");
            }
        }

        f.reset();
        var strong = node(f, f.base, new AspectList().add(Aspect.AIR, 100), null);
        strong.takeVis("aer", 10);
        var weak = node(f, f.base.east(), new AspectList().add(Aspect.AIR, 10), null);
        basic(f, weak.getBlockPos().below());
        try (var random = InputRandom.zeroes(f)) {
            tick(f, strong);
            tick(f, weak);
            random.expectOnlyInts(0, 5, "fresh first polls establish unlocked bully/locked victim without offset sampling");
        }
        var strongBefore = snapshot(f, strong);
        var weakBefore = snapshot(f, weak);
        try (var random = InputRandom.script(f, 5, 1, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0)) {
            tick(f, strong);
            random.finished("unlocked bully samples the exact locked neighbor but stops BEFORE nextInt(aspect-count)");
            f.check(sameStock(f, strong, strongBefore) && sameStock(f, weak, weakBefore)
                            && weak.getRechargeWait() == 0 && weak.getLock() == 1,
                    "selected locked victim loses neither vis/base nor recharge wait; bully gains nothing");
        }
        f.put(weak.getBlockPos().below(), Blocks.AIR);
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, weak, 48); // 1 ->49, still locked after removal.
            pureReads(f, weak, 49, 1, 1200);
            random.expectOnlyInts(0, 5, "locked victim retains cached protection through49 after removal");
            tick(f, weak);
            random.expectOnlyInts(6, 5, "victim's actual50 poll unlocks and samples, not an eager getter");
        }
        try (var random = InputRandom.script(f, 5, 1, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 1, 0)) {
            tick(f, strong); // count3, ordinary no-discharge phase.
            tick(f, strong); // count4: meaningful SUCCESS, no growth roll because strong has room.
            random.finished("actual unlocked-success branch draws six bounds5 plus exactly ONE aspect bound1");
            f.check(strong.getAspects().getAmount(Aspect.AIR) == 91 && weak.getAspects().getAmount(Aspect.AIR) == 9
                            && strong.getNodeVisBase(Aspect.AIR) == 100 && weak.getNodeVisBase(Aspect.AIR) == 10
                            && weak.getRechargeWait() == 300,
                    "real production bullying transfers exactly1, preserves total100/current and both bases, sets victim wait600/2");
        }
    }

    private static void replacementDuringSampling(Fixture f) {
        for (boolean replaceOrigin : new boolean[] {false, true}) {
            f.reset();
            var strong = node(f, f.base, new AspectList().add(Aspect.AIR, 100), null);
            strong.takeVis("aer", 10);
            var weak = node(f, f.base.east(), new AspectList().add(Aspect.AIR, 10), null);
            try (var random = InputRandom.zeroes(f)) {
                tick(f, strong); tick(f, weak);
                random.expectOnlyInts(0, 5, "replacement probe initializes two real first-poll phases with no RNG");
            }
            var oldStrong = snapshot(f, strong);
            var oldWeak = snapshot(f, weak);
            var installed = new TileNode[1];
            int[] calls = {0};
            // Explicit REENTRANT RNG INPUT: replace actual world ownership while
            // production is sampling, not by a mocked debit or copied stock API.
            try (var random = new InputRandom(f, bound -> {
                int call = ++calls[0];
                int replacementCall = replaceOrigin ? 6 : 7;
                if (call == replacementCall) {
                    BlockPos pos = replaceOrigin ? strong.getBlockPos() : weak.getBlockPos();
                    installed[0] = node(f, pos, new AspectList().add(Aspect.FIRE, 7), null);
                }
                if (call <= 6) {
                    f.check(bound == 5, "replacement probe offset RNG retains original bound5");
                    return call == 1 ? 1 : 0;
                }
                f.check(!replaceOrigin && call == 7 && bound == 1, "selected victim aspect RNG has exact original bound1");
                return 0;
            })) {
                tick(f, strong);
                f.check(calls[0] == (replaceOrigin ? 6 : 7) && installed[0] != null,
                        "exact observable RNG count reaches actual synchronous same-coordinate replacement");
                if (replaceOrigin) random.expectOnlyInts(6, 5, "retired origin exits after the original SIX sampled offsets");
                else random.expectCounts(Map.of(5, 6, 1, 1), 0, "retired selected victim exits after ONE actual aspect draw");
                f.check(sameStock(f, strong, oldStrong) && sameStock(f, weak, oldWeak)
                                && installed[0].getAspects().getAmount(Aspect.FIRE) == 7
                                && installed[0].getNodeVisBase(Aspect.FIRE) == 7
                                && installed[0].getAspects().getAmount(Aspect.AIR) == 0,
                        "replacement cannot transfer vis into/from the detached old owner or debit/mutate its newly installed replacement");
            }
        }
    }

    private static void wandDrain(Fixture f) {
        f.reset();
        var node = node(f, f.base, new AspectList().add(Aspect.AIR, 3).add(Aspect.PLANT, 7), null);
        basic(f, f.base.below());
        try (var random = InputRandom.zeroes(f)) { tick(f, node); random.expectOnlyInts(0, 5, "wand fixture establishes lock without RNG"); }
        f.aim(Vec3.atCenterOf(f.base));
        KnowledgeAccess.get(f.player).removeResearch("NODETAPPER1");
        KnowledgeAccess.get(f.player).removeResearch("NODETAPPER2");
        KnowledgeAccess.get(f.player).addResearch("NODEPRESERVE");
        ItemStack wand = NodeStabilizerRuntimeChecks.wand("greatwood", "gold", 5000);
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(wand).withAmount("aer", 4800));
        // Independent declared wand INPUTS, prepared BEFORE the actions. Never
        // refill/debit an already-charged wand to manufacture a later branch.
        ItemStack partial = NodeStabilizerRuntimeChecks.wand("greatwood", "gold", 5000);
        partial.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(partial).withAmount("aer", 4950));
        ItemStack preserved = NodeStabilizerRuntimeChecks.wand("greatwood", "gold", 5000);
        preserved.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(preserved).withAmount("aer", 4900));
        f.player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        try (var random = InputRandom.script(f, 1, 0)) {
            node.drainIntoWand(f.player, wand);
            random.finished("basic lock does NOT prohibit existing ordinary player wand charging; one eligible-primal draw");
        }
        f.check(node.getLock() == 1 && node.getAspects().getAmount(Aspect.AIR) == 2 && ItemWandCasting.getVis(wand, "aer") == 4900,
                "actual locked node action conserves100 centivis for ONE node unit, not arbitrary lock-denied charging");
        f.player.getInventory().selected = 1;
        f.player.setItemInHand(InteractionHand.MAIN_HAND, partial);
        try (var random = InputRandom.script(f, 1, 0)) { node.drainIntoWand(f.player, partial); random.finished("real final partial-capacity draw"); }
        f.check(node.getAspects().getAmount(Aspect.AIR) == 1 && ItemWandCasting.getVis(partial, "aer") == 5000,
                "original final50-centivis room still spends one whole node unit, leaving preserved last1");
        f.player.getInventory().selected = 2;
        f.player.setItemInHand(InteractionHand.MAIN_HAND, preserved);
        try (var random = InputRandom.zeroes(f)) {
            node.drainIntoWand(f.player, preserved);
            random.expectOnlyInts(0, 1, "NODEPRESERVE rejects last1 before eligible-aspect RNG");
        }
        f.player.setShiftKeyDown(true);
        try (var random = InputRandom.script(f, 1, 0)) { node.drainIntoWand(f.player, preserved); random.finished("sneaking retains original preserve override"); }
        f.player.setShiftKeyDown(false);
        f.check(node.getAspects().getAmount(Aspect.AIR) == 0 && node.getAspects().aspects.containsKey(Aspect.AIR)
                        && node.getNodeVisBase(Aspect.AIR) == 3 && node.getAspects().getAmount(Aspect.PLANT) == 7
                        && ItemWandCasting.getVis(preserved, "aer") == 5000
                        && ItemWandCasting.getVis(wand, "aer") == 4900 && ItemWandCasting.getVis(partial, "aer") == 5000,
                "lock leaves canonical zero-key/preserve/cap-rounding/compound behavior intact; no accidental prune during this short action");
        for (ItemStack actual : List.of(wand, partial, preserved)) for (String tag : WandData.PRIMALS) if (!tag.equals("aer")) {
            f.check(ItemWandCasting.getVis(actual, tag) == 5000, "each native owned wand conserves unrelated primal: " + tag);
        }
    }

    private static void minimumHeight(Fixture f) {
        f.reset();
        BlockPos bottom = new BlockPos(f.base.getX(), f.level.getMinBuildHeight(), f.base.getZ());
        f.check(f.level.hasChunkAt(bottom), "height adaptation uses an ALREADY loaded owned x/z column, never force-loads a chunk");
        try (var shadow = new ExtraCells(f, bottom, bottom.above())) {
            var unsupported = node(f, bottom, new AspectList().add(Aspect.AIR, 10), null);
            try (var random = InputRandom.zeroes(f)) {
                tick(f, unsupported);
                f.check(unsupported.getLock() == 0 && unsupported.getRegenerationTicks() == 600,
                        "node AT modern min-build-height has no below-world support lookup/lock");
                random.expectOnlyInts(0, 5, "bottom first tick is still ordinary non-discharge phase");
            }
            f.put(bottom, Blocks.AIR);
            basic(f, bottom);
            var supported = node(f, bottom.above(), new AspectList().add(Aspect.AIR, 10), null);
            try (var random = InputRandom.zeroes(f)) {
                tick(f, supported);
                f.check(supported.getBlockPos().getY() < 0 && supported.getLock() == 1 && supported.getRegenerationTicks() == 1200,
                        "real basic support at minimum height locks an ordinary node at negative min+1, not legacy y>0");
                random.expectOnlyInts(0, 5, "negative-height basic lock also returns before RNG");
            }
        }
    }

    private static void regenerationPair(Fixture f, TileNode.NodeModifier modifier, int interval) {
        f.reset();
        var free = depleted(f, f.base, modifier);
        var locked = depleted(f, f.base.east(2), modifier);
        basic(f, locked.getBlockPos().below());
        String freeId = free.getScanId(), lockedId = locked.getScanId();
        var base = free.getAspectsBase().aspects;
        int start = deficit(free);
        f.check(start == 9 && deficit(locked) == start && free.getAspects().getAspects().length == 3,
                "explicit equal-positive-deficit fixture; no zero entries can accidentally prune at1200");
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, free, interval - 1);
            ticks(f, locked, interval - 1);
            f.check(free.getRegenerationTicks() == interval && locked.getRegenerationTicks() == interval * 2
                            && free.getLock() == 0 && locked.getLock() == 1 && deficit(free) == start && deficit(locked) == start,
                    "actual " + modifier + " baseline" + interval + " vs basic-lock" + interval * 2 + ", no premature recharge");
            tick(f, free);
            tick(f, locked);
            f.check(deficit(free) == start - 1 && deficit(locked) == start,
                    "first UNLOCKED recharge at" + interval + " restores exactly1; locked deficit still9");
            ticks(f, free, interval - 1);
            ticks(f, locked, interval - 1);
            f.check(deficit(free) == start - 1 && deficit(locked) == start,
                    "no first locked recharge before full doubled interval" + interval * 2);
            tick(f, free);
            tick(f, locked);
            f.check(deficit(free) == start - 2 && deficit(locked) == start - 1,
                    "real doubled-boundary restores free SECOND1 vs locked FIRST1; no RNG drain loss");
            int dischargeInterval = modifier == TileNode.NodeModifier.BRIGHT ? 1 : modifier == TileNode.NodeModifier.PALE ? 3 : 2;
            int samplingTicks = interval * 2 / dischargeInterval;
            random.expectCounts(Map.of(5, samplingTicks * 6, 3, 3), modifier == TileNode.NodeModifier.PALE ? samplingTicks : 0,
                    "six actual unlocked offset draws/eligible tick, PALE boolean only there, exactly THREE recharge choices among three deficits; locked outgoing0");
        }
        f.check(free.getScanId().equals(freeId) && locked.getScanId().equals(lockedId), "actual regeneration never replaces either scan UUID");
        for (var node : List.of(free, locked)) {
            f.check(node.getNodeModifier() == modifier && node.getNodeType() == TileNode.NodeType.NORMAL && node.isNatural()
                            && node.getAspects().size() == 3 && node.getAspectsBase().aspects.equals(base)
                            && node.getAspects().aspects.values().stream().allMatch(value -> value > 0),
                    "bounded production ticks preserve every positive base/current key, NORMAL type and declared modifier");
        }
    }

    private static void lockChangeBeforeRecharge(Fixture f) {
        f.reset();
        var node = node(f, f.base, new AspectList().add(Aspect.AIR, 10), null);
        node.takeVis("aer", 1);
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 599);
            f.check(node.getLock() == 0 && node.getRegenerationTicks() == 600 && deficit(node) == 1, "real unlocked599 fixture before original recharge boundary");
            basic(f, f.base.below());
            pureReads(f, node, 599, 0, 600);
            tick(f, node);
            f.check(node.getTickCount() == 600 && node.getLock() == 1 && node.getRegenerationTicks() == 1200 && deficit(node) == 1,
                    "lock change invalidates old600 cache BEFORE actual phase600 recharge, preventing a free old-interval unit");
            ticks(f, node, 599);
            power(f, node, true);
            pureReads(f, node, 1199, 1, 1200);
            tick(f, node);
            f.check(node.getTickCount() == 1200 && node.getLock() == 0 && node.getRegenerationTicks() == 600 && deficit(node) == 0,
                    "phase1200 support power invalidates doubled cache BEFORE one actual unlocked recharge");
            random.expectCounts(Map.of(5, (299 + 1) * 6, 1, 1), 0, "599 unlocked phases plus1200 unlocked sample, exactly ONE actual recharge, no zero pruning");
        }
    }

    private static void fadingRepair(Fixture f) {
        f.reset();
        var node = node(f, f.base, new AspectList().add(Aspect.AIR, 10), TileNode.NodeModifier.FADING);
        node.takeVis("aer", 1);
        basic(f, f.base.below());
        try (var random = InputRandom.script(f, 12500, 49)) {
            ticks(f, node, 99);
            f.check(node.getLock() == 1 && node.getRegenerationTicks() == 0 && deficit(node) == 1 && random.bounds.isEmpty(),
                    "locked FADING interval0 and no repair RNG before original100 boundary");
            tick(f, node);
            random.finished("negative INPUT outcome49 at EXACT nextInt(12500), not success69 or an invented guaranteed repair");
            f.check(node.getNodeModifier() == TileNode.NodeModifier.FADING && node.getRegenerationTicks() == 0 && deficit(node) == 1,
                    "count100 negative outcome leaves FADING/current/base/cache unchanged");
        }
        var chunk = f.level.getChunkAt(f.base);
        boolean alreadyDirty = chunk.isUnsaved();
        try (var random = InputRandom.script(f, 12500, 69)) {
            ticks(f, node, 99);
            f.check(node.getTickCount() == 199 && random.bounds.isEmpty(), "only actual next hundredth tick may roll repair again");
            // TEST INPUT only: clear/observe this ALREADY loaded chunk's dirty
            // flag synchronously; retain old OR new dirty state in finally.
            chunk.setUnsaved(false);
            tick(f, node);
            random.finished("success INPUT outcome69 at exact bound12500, one draw at real200");
            f.check(node.getNodeModifier() == TileNode.NodeModifier.PALE && node.getRegenerationTicks() == 0
                            && deficit(node) == 1 && chunk.isUnsaved(),
                    "original FADING->PALE assigns/syncs PALE but DOES NOT invalidate cached FADING0; original setter250-252, not nodeChange");
            var update = node.getUpdatePacket();
            f.check(update != null && update.getTag() != null
                            && update.getTag().getByte("modifier") == (byte) TileNode.NodeModifier.PALE.ordinal()
                            && node.getUpdateTag(f.level.registryAccess()).getByte("modifier") == (byte) TileNode.NodeModifier.PALE.ordinal(),
                    "actual native custom-update packet/tag exposes PALE and real stock; not remote-client receive evidence");
        } finally {
            chunk.setUnsaved(alreadyDirty || chunk.isUnsaved());
        }
        try (var random = InputRandom.zeroes(f)) {
            tick(f, node);
            f.check(node.getRegenerationTicks() == 0, "original next201 still has cached FADING0 despite new PALE modifier; no unauthorized automatic repair");
            ticks(f, node, 1598);
            f.check(node.getTickCount() == 1799 && deficit(node) == 1, "new PALE keeps the original stale-zero interval until a real cache invalidation");
            tick(f, node);
            f.check(deficit(node) == 1 && node.getRegenerationTicks() == 0, "even actual1800 does NOT restore vis while original cached FADING0 remains");
            random.expectOnlyInts(0, 1, "locked new PALE has no discharge/repair/recharge RNG while the original interval remains0");
        }
        power(f, node, true);
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 49);
            pureReads(f, node, 1849, 1, 0);
            tick(f, node);
            f.check(node.getLock() == 0 && node.getRegenerationTicks() == 900 && deficit(node) == 1,
                    "only actual powered-support poll1850 invalidates stale0 and computes unlocked PALE900");
            random.expectOnlyInts(0, 5, "1850 is not a PALE discharge phase or recharge boundary");
        }
        power(f, node, false);
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, node, 49);
            pureReads(f, node, 1899, 0, 900);
            tick(f, node);
            f.check(node.getLock() == 1 && node.getRegenerationTicks() == 1800 && deficit(node) == 1,
                    "actual unpowered-support poll1900 restores basic lock and computes PALE900*2");
            ticks(f, node, 1699);
            f.check(node.getTickCount() == 3599 && deficit(node) == 1, "no synthetic phase reset or premature restored-interval refill");
            tick(f, node);
            f.check(deficit(node) == 0 && node.getRegenerationTicks() == 1800, "real3600 boundary finally restores exactlyONE after genuine power-cycle cache invalidation");
            random.expectCounts(Map.of(5, 17 * 6, 1, 1), 17,
                    "unlocked PALE1851..1899 performs17 exact offset/boolean branches, re-locked node draws only ONE recharge choice at3600");
        }
        f.reset();
        var free = node(f, f.base, new AspectList().add(Aspect.AIR, 10), TileNode.NodeModifier.FADING);
        free.takeVis("aer", 1);
        basic(f, f.base.east());
        try (var random = InputRandom.zeroes(f)) {
            ticks(f, free, 1200);
            f.check(free.getLock() == 0 && free.getRegenerationTicks() == 0 && free.getNodeModifier() == TileNode.NodeModifier.FADING && deficit(free) == 1,
                    "unlocked FADING remains0/no repair even beside a non-supporting basic device, including prune boundary with positive keys");
            random.expectOnlyInts(0, 12500, "unlocked FADING NEVER rolls repair or discharge/recharge RNG");
        }
    }

    private static void historicalCatchUp(Fixture f) {
        f.reset();
        var free = depleted(f, f.base, null);
        var locked = depleted(f, f.base.east(2), null);
        basic(f, locked.getBlockPos().below());
        // EXPLICIT OLD-SAVE TIMESTAMP INPUT, not a current production/client
        // clock write: 3.5 original600*75ms periods, midway between boundaries.
        long historical = System.currentTimeMillis() - 157500L;
        var freeTag = free.saveWithFullMetadata(f.level.registryAccess());
        var lockedTag = locked.saveWithFullMetadata(f.level.registryAccess());
        freeTag.putLong("lastActive", historical);
        lockedTag.putLong("lastActive", historical);
        free = reload(f, free, freeTag);
        locked = reload(f, locked, lockedTag);
        f.check(free.getLock() == 0 && locked.getLock() == 0 && free.getTickCount() == 0 && locked.getTickCount() == 0,
                "both actual old-save loads start transient lock/count0 BEFORE first real world poll");
        try (var random = InputRandom.script(f, 3, 0, 3, 0, 3, 0, 3, 0)) {
            long elapsed = System.currentTimeMillis() - historical;
            f.check(elapsed >= 135000L && elapsed < 180000L,
                    "declared old timestamp remains within3-vs1 catch-up window; no current clock is forced to hide a slow fixture");
            tick(f, free);
            tick(f, locked);
            random.finished("old timestamp restores three unlocked units vs ONE locked unit with actual doubled1200*75ms interval");
            f.check(deficit(free) == 6 && deficit(locked) == 8 && free.getRegenerationTicks() == 600 && locked.getRegenerationTicks() == 1200,
                    "native wall-clock catch-up uses new FIRST-POLL basic lock, not historical/base600 for both");
        }
        try (var random = InputRandom.zeroes(f)) {
            tick(f, free);
            tick(f, locked);
            f.check(deficit(free) == 6 && deficit(locked) == 8, "old timestamp catch-up is consumed once, not repeated on every real tick");
            random.expectOnlyInts(6, 5, "second unlocked tick samples normally; no replayed catch-up choices");
        }
    }

    private static void persistenceAndStaleOwners(Fixture f) {
        f.reset();
        var node = node(f, f.base, new AspectList().add(Aspect.AIR, 10).add(Aspect.PLANT, 7), TileNode.NodeModifier.PALE);
        node.takeVis("aer", 3);
        basic(f, f.base.below());
        try (var random = InputRandom.zeroes(f)) { ticks(f, node, 49); random.expectOnlyInts(0, 5, "saved locked phase49 is reached through real production ticks"); }
        var saved = node.saveWithFullMetadata(f.level.registryAccess());
        var update = node.getUpdateTag(f.level.registryAccess());
        for (String key : List.of("lock", "nodeLock", "count", "regeneration", "wait", "rechargeWait")) {
            f.check(!saved.contains(key) && !update.contains(key), "lock/phase/cache/wait remain original transient fields, absent disk/custom update: " + key);
        }
        var original = snapshot(f, node);
        TileNode old = node;
        // Unknown transient-looking fields are declared malformed INPUT, not a
        // claim that the accepted normal-node loader quarantines arbitrary NBT.
        saved.putByte("nodeLock", (byte) 1);
        saved.putInt("lock", 1);
        saved.putInt("count", 1199);
        saved.putInt("regeneration", 1);
        saved.putInt("wait", 0);
        node = reload(f, node, saved);
        f.check(node != old && node.getLock() == 0 && node.getTickCount() == 0 && node.getRegenerationTicks() == -1
                        && node.getRechargeWait() == 0 && sameStock(f, node, original),
                "actual native registered reload retains EXACT current/base/scan UUID/NORMAL/modifier/natural, not injected lock/count/cache");
        try (var random = InputRandom.zeroes(f)) {
            pureReads(f, node, 0, 0, -1);
            tick(f, node);
            f.check(node.getLock() == 1 && node.getTickCount() == 1 && node.getRegenerationTicks() == 1800 && deficit(node) == 3,
                    "first actual reload tick polls REAL support and recomputes PALE900*2, not saved lock or immediate free recharge");
            random.expectOnlyInts(0, 5, "native reload plus pure getter does not draw production RNG or fabricate refill");
        }
        var installedBefore = snapshot(f, node);
        var detachedBefore = snapshot(f, old);
        int detachedCount = old.getTickCount();
        try (var random = InputRandom.zeroes(f)) {
            TileNode.serverTick(f.level, f.base, f.level.getBlockState(f.base), old);
            f.check(old.getTickCount() == detachedCount && sameStock(f, old, detachedBefore) && sameStock(f, node, installedBefore),
                    "removed old BE ticker cannot mutate phase/stock or transfer anything into its replacement");
            old.clearRemoved(); // explicit detached-not-marked-removed identity INPUT.
            TileNode.serverTick(f.level, f.base, f.level.getBlockState(f.base), old);
            f.check(old.getTickCount() == detachedCount && sameStock(f, old, detachedBefore) && sameStock(f, node, installedBefore),
                    "even an unmarked detached BE is refused by CURRENT installed identity, not merely removed flag");
            old.setRemoved();
            random.expectOnlyInts(0, 5, "stale identity attempts return before ANY world RNG");
        }
        var payload = node.getUpdatePacket();
        f.check(payload != null && payload.getTag() != null
                        && payload.getTag().getString("scanId").equals(node.getScanId())
                        && payload.getTag().getByte("type") == (byte) TileNode.NodeType.NORMAL.ordinal()
                        && payload.getTag().getByte("modifier") == (byte) TileNode.NodeModifier.PALE.ordinal(),
                "native update snapshot retains UUID/type/modifier without authoritative cached lock or phase");
        var staleState = node.getBlockState();
        var last = node;
        var beforeRemoval = snapshot(f, last);
        int lastCount = last.getTickCount();
        f.put(f.base, Blocks.STONE);
        last.clearRemoved(); // explicit stale block + old state argument INPUT.
        try (var random = InputRandom.zeroes(f)) {
            TileNode.serverTick(f.level, f.base, staleState, last);
            f.check(f.level.getBlockState(f.base).is(Blocks.STONE) && last.getTickCount() == lastCount && sameStock(f, last, beforeRemoval),
                    "stale aura state argument cannot authorize a detached node over a real STONE replacement");
            random.expectOnlyInts(0, 5, "stale actual block and current identity checked before RNG");
        } finally { last.setRemoved(); }
    }

    private static TileNode node(Fixture f, BlockPos pos, AspectList aspects, TileNode.NodeModifier modifier) {
        f.check(!f.level.isOutsideBuildHeight(pos) && f.level.hasChunkAt(pos), "node fixture uses only already-loaded in-height cells");
        // setBlock on an unchanged state need not recreate a removed BE.
        f.put(pos, Blocks.AIR);
        f.put(pos, ThaumcraftContent.AURA_NODE.get());
        var node = (TileNode) f.level.getBlockEntity(pos);
        f.check(node != null && node.getLevel() == f.level && f.level.getBlockEntity(pos) == node, "actual installed native node factory/current identity");
        node.initializeNatural(aspects, modifier);
        return node;
    }

    private static TileNode depleted(Fixture f, BlockPos pos, TileNode.NodeModifier modifier) {
        var node = node(f, pos, new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10).add(Aspect.PLANT, 10), modifier);
        for (Aspect aspect : node.getAspects().getAspects()) f.check(node.takeFromContainer(aspect, 3), "explicit positive direct deficit INPUT: " + aspect.getTag());
        return node;
    }

    private static int deficit(TileNode node) {
        int total = 0;
        for (Aspect aspect : node.getAspectsBase().getAspects()) total += node.getNodeVisBase(aspect) - node.getAspects().getAmount(aspect);
        return total;
    }

    private static void basic(Fixture f, BlockPos pos) {
        f.put(pos, Blocks.AIR);
        f.put(pos, ThaumcraftNodeDevices.NODE_STABILIZER.get());
    }

    private static void power(Fixture f, TileNode node, boolean value) {
        BlockPos support = node.getBlockPos().below();
        f.put(support.east(), value ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        f.check(f.level.hasNeighborSignal(support) == value, "actual world power at BASIC support, not node lock/power field injection");
    }

    private static void pureReads(Fixture f, TileNode node, int phase, int lock, int regeneration) {
        var before = node.saveWithFullMetadata(f.level.registryAccess());
        int wait = node.getRechargeWait();
        var chunk = f.level.getChunkAt(node.getBlockPos());
        boolean wasDirty = chunk.isUnsaved();
        // Synchronous dirty-observation INPUT, retaining old OR new flag below.
        chunk.setUnsaved(false);
        try {
            for (int call = 0; call < 32; call++) {
                f.check(node.getLock() == lock && node.getTickCount() == phase && node.getRegenerationTicks() == regeneration
                                && node.getRechargeWait() == wait,
                        "read-only cached getters neither repoll support nor initialize sentinel/recharge/phase");
            }
            f.check(before.equals(node.saveWithFullMetadata(f.level.registryAccess())) && !chunk.isUnsaved(),
                    "all getter reads leave native persisted payload AND chunk dirty flag unchanged");
        } finally {
            chunk.setUnsaved(wasDirty || chunk.isUnsaved());
        }
    }

    private static void tick(Fixture f, TileNode node) {
        var pos = node.getBlockPos();
        var state = f.level.getBlockState(pos);
        var ticker = state.getTicker(f.level, ThaumcraftContent.NODE_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing actual ordinary-node production ticker");
        ticker.tick(f.level, pos, state, node);
    }

    private static void ticks(Fixture f, TileNode node, int count) { for (int call = 0; call < count; call++) tick(f, node); }

    private static CompoundTag snapshot(Fixture f, TileNode node) {
        var tag = node.saveWithoutMetadata(f.level.registryAccess());
        tag.remove("lastActive"); // natural lifecycle timestamp is checked separately by historical catch-up.
        return tag;
    }

    private static boolean sameStock(Fixture f, TileNode node, CompoundTag expected) { return snapshot(f, node).equals(expected); }

    private static TileNode reload(Fixture f, TileNode old, CompoundTag tag) {
        BlockPos pos = old.getBlockPos();
        f.check(f.level.getBlockEntity(pos) == old, "native reload detaches only its own current owner");
        f.level.removeBlockEntity(pos);
        var loaded = BlockEntity.loadStatic(pos, f.level.getBlockState(pos), tag, f.level.registryAccess());
        f.check(loaded instanceof TileNode, "actual registered ordinary-node loadStatic factory");
        f.level.setBlockEntity(loaded);
        return (TileNode) loaded;
    }

    /** Bounded synchronous TEST INPUT replacement; never seeded, delegated to,
     * advanced or rewound the actual world's RandomSource. All calls observable.
     * Reflection only adapts Level's existing final instance field in gameTest;
     * no production accessor, mixin or gameplay API is added. */
    private static final class InputRandom implements AutoCloseable {
        final Fixture f;
        final Field field;
        final RandomSource original, replacement;
        final List<Integer> bounds = new ArrayList<>();
        final List<String> nonIntegerCalls = new ArrayList<>();
        final int[] script;
        int scriptIndex;

        private InputRandom(Fixture f, IntUnaryOperator values) { this(f, values, null); }

        private InputRandom(Fixture f, IntUnaryOperator values, int[] script) {
            this.f = f;
            this.script = script;
            original = f.level.random;
            replacement = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(), new Class<?>[] {RandomSource.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "toString" -> "M09a explicit deterministic RandomSource INPUT";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("Unexpected Object method " + method);
                };
                if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                    int bound = (Integer) args[0];
                    bounds.add(bound);
                    f.check(bound > 0, "production RNG requires a positive original bound");
                    int value;
                    if (this.script != null) {
                        f.check(scriptIndex + 1 < this.script.length, "unexpected extra RNG draw, bound" + bound + "; observed=" + bounds);
                        f.check(bound == this.script[scriptIndex], "exact original RNG bound at draw" + scriptIndex / 2 + "; observed=" + bounds);
                        value = this.script[scriptIndex + 1];
                        scriptIndex += 2;
                    } else value = values.applyAsInt(bound);
                    f.check(value >= 0 && value < bound, "deterministic INPUT result lies within ACTUAL requested bound");
                    return value;
                }
                nonIntegerCalls.add(method.getName());
                if (method.getName().equals("nextBoolean") && this.script == null) return false;
                throw new AssertionError("Unexpected RNG method in bounded stabilizer INPUT: " + method.getName());
            });
            try {
                field = Level.class.getDeclaredField("random");
                field.setAccessible(true);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot install test-only Level RandomSource INPUT", failure);
            }
            try {
                field.set(f.level, replacement);
                f.check(f.level.random == replacement, "scoped deterministic INPUT is installed, not a seeded production RNG");
            } catch (IllegalAccessException | RuntimeException | Error failure) {
                try { field.set(f.level, original); }
                catch (IllegalAccessException restore) { failure.addSuppressed(restore); }
                throw new IllegalStateException("Cannot install/verify test-only Level RandomSource INPUT", failure);
            }
        }

        static InputRandom zeroes(Fixture f) { return new InputRandom(f, bound -> 0); }
        static InputRandom script(Fixture f, int... boundValuePairs) {
            if (boundValuePairs.length % 2 != 0) throw new IllegalArgumentException("Expected bound/value pairs");
            return new InputRandom(f, bound -> { throw new AssertionError("Script unexpectedly delegated"); }, boundValuePairs);
        }

        void finished(String why) {
            f.check(script != null && scriptIndex == script.length && nonIntegerCalls.isEmpty(), why + "; actual bounds=" + bounds);
        }

        void expectOnlyInts(int count, int bound, String why) {
            f.check(bounds.size() == count && bounds.stream().allMatch(value -> value == bound) && nonIntegerCalls.isEmpty(),
                    why + "; actual integer calls=" + bounds.size() + ", other=" + nonIntegerCalls);
        }

        void expectCounts(Map<Integer, Integer> expected, int booleans, String why) {
            var actual = new LinkedHashMap<Integer, Integer>();
            for (int bound : bounds) actual.merge(bound, 1, Integer::sum);
            f.check(actual.equals(expected) && nonIntegerCalls.size() == booleans && nonIntegerCalls.stream().allMatch("nextBoolean"::equals),
                    why + "; actual bounds=" + actual + ", boolean calls=" + nonIntegerCalls.size());
        }

        @Override public void close() {
            try {
                boolean stillOwned = f.level.random == replacement;
                field.set(f.level, original);
                f.check(stillOwned && f.level.random == original, "finally restores EXACT original level RNG identity without seeding/advancing it");
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot restore actual level RNG", failure);
            }
        }
    }

    /** Save/restore ONLY the two explicitly touched height-boundary cells, in an
     * already-loaded chunk; never a broad chunk/world snapshot or forced load. */
    private static final class ExtraCells implements AutoCloseable {
        final Fixture f;
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        final Map<BlockPos, BlockEntity> tiles = new LinkedHashMap<>();

        ExtraCells(Fixture f, BlockPos... positions) {
            this.f = f;
            for (BlockPos pos : positions) {
                f.check(f.level.hasChunkAt(pos) && !f.level.isOutsideBuildHeight(pos), "owned shadow never force-loads/out-of-height cells");
                states.put(pos, f.level.getBlockState(pos));
                var tile = f.level.getBlockEntity(pos);
                if (tile != null) tiles.put(pos, tile);
            }
        }

        @Override public void close() {
            for (BlockPos pos : states.keySet()) f.level.removeBlockEntity(pos);
            states.forEach((pos, state) -> f.level.setBlock(pos, state, TubeVariantsRuntimeChecks.QUIET));
            tiles.values().forEach(tile -> { tile.clearRemoved(); f.level.setBlockEntity(tile); });
        }
    }
}
