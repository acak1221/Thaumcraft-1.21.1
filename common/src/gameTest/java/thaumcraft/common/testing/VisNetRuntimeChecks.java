package thaumcraft.common.testing;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks.InputRandom;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.common.tiles.TileVisRelay;

/** Original energized35-175, ResearchManager470-494 MAX, VisNet29-181 and graph
 * first/40. Raw network int1 is1 centivis-compatible quantum, NOT100 or essentia.
 * Native SERVER bases/policies are declared fixture inputs. Every built-in source
 * below is actually converted, never supplied as a finished energized device. */
public final class VisNetRuntimeChecks {
    private VisNetRuntimeChecks() {}

    /** Suggested timeout240, separate batch. No elapsed client-tick claim. */
    public static void sourceBudgets(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open();
            maximumAndEpochs(f);
            modifiers(f);
            unstable(f);
            strictPersistence(f);
        }
        test.succeed();
    }

    private static void maximumAndEpochs(Fixture f) {
        f.reset();
        var base = new AspectList().add(Aspect.EARTH, 9).add(Aspect.METAL, 16);
        var source = NodeTransductionRuntimeChecks.energize(f, f.base, base, null, TileNode.NodeType.NORMAL);
        f.check(source.getAuraBase().aspects.equals(base.aspects) && source.getVisBase().getAmount(Aspect.EARTH) == 4
                        && source.getVisBase().getAmount(Aspect.ORDER) == 4 && source.getVisBase().size() == 2
                        && source.getAvailableVis().visSize() == 0 && source.consumeVis(Aspect.EARTH, 1) == 0,
                "original MAX overlap terra9+metallum16 => terra16/ordo16 =>4/4, NEVER accepted helper's SUM terra41=>6; no pulse before source tick");
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        long epoch = source.getRefillEpoch();
        f.check(epoch == 1 && source.getAvailableVis().getAmount(Aspect.EARTH) == 4, "first actual source update replaces pulse with capacity");
        var persisted = source.saveWithFullMetadata(f.level.registryAccess());
        var chunk = f.level.getChunkSource().getChunkNow(f.base.getX() >> 4, f.base.getZ() >> 4);
        boolean dirty = chunk.isUnsaved(); chunk.setUnsaved(false);
        try (var rng = new InputRandom(f, bound -> { throw new AssertionError("Getter recomputed/drew RNG " + bound); })) {
            for (int i = 0; i < 32; i++) {
                source.getAuraBase().aspects.clear(); source.getAspects().aspects.clear(); source.getVisBase().aspects.clear(); source.getAvailableVis().aspects.clear();
                f.check(source.getRefillEpoch() == epoch && source.getAvailableVis().getAmount(Aspect.EARTH) == 4
                                && source.getAuraBase().getAmount(Aspect.METAL) == 16 && source.getAspects().getAmount(Aspect.ORDER) == 4,
                        "ALL list getters are independent defensive snapshots; observing never consumes/recalculates/refills");
            }
            rng.onlyInts(0, 5, "pure source observer uses NO production RNG");
            f.check(persisted.equals(source.saveWithFullMetadata(f.level.registryAccess())) && !chunk.isUnsaved(),
                    "pure getters leave native persisted fields AND actual chunk dirty flag unchanged");
        } finally { chunk.setUnsaved(dirty || chunk.isUnsaved()); }
        int a = VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, 3);
        int b = VisNetHandler.drainVis(f.level, f.base.west(), Aspect.EARTH, 3);
        f.check(a == 3 && b == 1 && a + b + source.getAvailableVis().getAmount(Aspect.EARTH) == 4
                        && source.getAvailableVis().getAmount(Aspect.ORDER) == 4 && source.getRefillEpoch() == epoch,
                "TWO actual consumers share ONE source epoch4; partial3+1+residual0, independent ORDO4, no clock/refill in drain");
        f.check(VisNetHandler.drainVis(f.level, f.base.east(), Aspect.ORDER, 1) == 1
                        && source.getAvailableVis().getAmount(Aspect.ORDER) == 3
                        && VisNetHandler.drainVis(f.level, f.base.east(), Aspect.METAL, 99) == 0,
                "raw amount1 debits1 not100; compound aura is NOT spendable compound pulse");
        for (int amount : new int[] {-1, 0}) f.check(VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, amount) == 0,
                "nonpositive raw requests never create credit");
        f.check(VisNetHandler.drainVis(f.level, f.base.east(), null, 1) == 0 && source.getAvailableVis().getAmount(Aspect.ORDER) == 3,
                "null cannot spend ANY independent primal");
        String unregisteredTag = "m09b_unregistered_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var noncanonical = new Aspect(unregisteredTag, 0x123456, new Aspect[] {Aspect.AIR, Aspect.FIRE});
        Aspect.aspects.remove(unregisteredTag, noncanonical);
        try {
            f.check(VisNetHandler.drainVis(f.level, f.base.east(), noncanonical, 1) == 0
                            && source.getAvailableVis().getAmount(Aspect.ORDER) == 3,
                    "actual public boundary refuses unregistered/noncanonical aspect object without partial independent-primal loss");
        } finally { Aspect.aspects.remove(unregisteredTag, noncanonical); }
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.check(source.getRefillEpoch() == epoch + 1 && source.getAvailableVis().getAmount(Aspect.EARTH) == 4
                        && source.getAvailableVis().getAmount(Aspect.ORDER) == 4, "NEXT actual tick FULLY replaces remaining3 with4, never accumulates to7");
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.check(source.getAvailableVis().getAmount(Aspect.EARTH) == 4 && source.getAvailableVis().getAmount(Aspect.ORDER) == 4,
                "unused next epoch still4/4, no accumulation or ordinary-node/wand consumer debit");
    }

    private static void modifiers(Fixture f) {
        // Explicit square-boundary oracles, not expectations computed by the
        // same production helper under test. Truncate BEFORE floor(sqrt).
        int[][] amountsAndRates = {{7, 2, 2, 1}, {8, 3, 2, 2}, {15, 4, 3, 2}, {17, 4, 3, 2}};
        var mods = new TileNode.NodeModifier[] {TileNode.NodeModifier.BRIGHT, TileNode.NodeModifier.PALE, TileNode.NodeModifier.FADING};
        for (var row : amountsAndRates) for (int m = 0; m < mods.length; m++) {
            f.reset();
            var source = NodeTransductionRuntimeChecks.energize(f, f.base, new AspectList().add(Aspect.EARTH, row[0]), mods[m], TileNode.NodeType.NORMAL);
            f.check(source.getAuraBase().getAmount(Aspect.EARTH) == row[0] && source.getVisBase().getAmount(Aspect.EARTH) == row[m + 1],
                    "original float/int/sqrt boundary base" + row[0] + "/" + mods[m] + " has literal rate" + row[m + 1]);
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            int actual = VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, Integer.MAX_VALUE);
            f.check(actual == row[m + 1] && source.getAvailableVis().getAmount(Aspect.EARTH) == 0,
                    "literal modifier rate is genuinely spendable bounded raw stock, no integer-overflow scaling");
        }
    }

    private static void unstable(Fixture f) {
        f.reset();
        var source = NodeTransductionRuntimeChecks.energize(f, f.base, new AspectList().add(Aspect.AIR, 1).add(Aspect.EARTH, 16), null, TileNode.NodeType.UNSTABLE);
        int[] call = {0};
        try (var rng = new InputRandom(f, bound -> {
            int i = ++call[0];
            f.check(bound == (i == 1 ? 500 : 5) && i <= 3, "EXACT original unstable reset500 then one offset5 PER reduced primal");
            return i == 1 ? 1 : i == 2 ? 0 : 4;
        })) {
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            f.check(call[0] == 3 && source.getVisBase().getAmount(Aspect.AIR) == 0 && source.getVisBase().getAmount(Aspect.EARTH) == 6
                            && source.getVisBase().size() == 1, "scripted SERVER reset1: rate1-2 omitted (<1), rate4+2=6; allowed original outcomes, not production seed/distribution");
            int got = VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, 4);
            f.check(got == 4 && got + source.getAvailableVis().getAmount(Aspect.EARTH) == 6,
                    "unstable chosen capacity obeys exact epoch conservation, no unstable reroll during drain");
        }
        try (var rng = new InputRandom(f, bound -> { f.check(bound == 500, "no-reset unstable tick asks ONLY nextInt500"); return 0; })) {
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            rng.onlyInts(1, 500, "reset miss retains SAME actual capacity, replaces residual with6");
            f.check(source.getAvailableVis().getAmount(Aspect.EARTH) == 6 && source.getVisBase().getAmount(Aspect.AIR) == 0,
                    "no-reset next epoch neither accumulates old residual2 nor resurrects omitted aer");
        }
    }

    private static void strictPersistence(Fixture f) {
        f.reset();
        var source = NodeTransductionRuntimeChecks.energize(f, f.base,
                new AspectList().add(Aspect.EARTH, 9).add(Aspect.METAL, 16).add(Aspect.AIR, 25), TileNode.NodeModifier.BRIGHT, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.check(VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, 2) == 2, "real epoch partially spent BEFORE native save");
        var saved = source.saveWithFullMetadata(f.level.registryAccess());
        f.check(saved.getList("Aspects", Tag.TAG_COMPOUND).size() == 3 && saved.getList("AEB", Tag.TAG_COMPOUND).size() == 3,
                "original auraBASE three ordered aspects and reduced AEB three capacities are both persisted, distinct from pulse");
        for (String field : List.of("vis", "Vis", "AvailableVis", "refillEpoch", "nodeCounter", "parent", "children"))
            f.check(!saved.contains(field), "ephemeral stock/epoch/Java graph not serialized: " + field);
        var old = source;
        source = NodeTransductionRuntimeChecks.reload(f, source, saved, TileNodeEnergized.class);
        f.check(source.getAvailableVis().visSize() == 0 && source.getRefillEpoch() == 0 && source.getNodeCounter() == 0
                        && source.getAuraBase().aspects.equals(old.getAuraBase().aspects)
                        && source.getVisBase().aspects.equals(old.getVisBase().aspects) && source.getNodeModifier() == TileNode.NodeModifier.BRIGHT,
                "ACTUAL current factory reload keeps BASE/AEB/type/mod/id but ZERO unused pulse/transient graph");
        f.check(VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, 1) == 0, "saved partially spent pulse cannot resurrect BEFORE new actual source tick");
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.check(VisNetHandler.drainVis(f.level, f.base.east(), Aspect.EARTH, 99) == 4,
                "first actual tick after real reload earns fresh capacity4, not saved residual2 or doubled8");

        var badTags = new LinkedHashMap<String, CompoundTag>();
        for (String field : List.of("Aspects", "AEB", "type", "modifier", "nodeId")) {
            var bad = saved.copy(); bad.remove(field); badTags.put("missing " + field, bad);
            bad = saved.copy(); bad.put(field, field.equals("Aspects") || field.equals("AEB") ? net.minecraft.nbt.StringTag.valueOf("wrong-native-type") : new ListTag()); badTags.put("wrong type " + field, bad);
        }
        for (String listName : List.of("Aspects", "AEB")) {
            var bad = saved.copy(); var list = bad.getList(listName, Tag.TAG_COMPOUND);
            list.add(list.getCompound(0).copy()); badTags.put("duplicate " + listName, bad);
            for (int amount : new int[] {-1, 0}) {
                bad = saved.copy(); bad.getList(listName, Tag.TAG_COMPOUND).getCompound(0).putInt("amount", amount);
                badTags.put("nonpositive " + listName + "/" + amount, bad);
            }
            bad = saved.copy(); bad.getList(listName, Tag.TAG_COMPOUND).getCompound(0).putString("key", "unknown_m09b_aspect"); badTags.put("unknown " + listName, bad);
            bad = saved.copy(); bad.getList(listName, Tag.TAG_COMPOUND).getCompound(0).putDouble("amount", 4); badTags.put("nonINT count " + listName, bad);
        }
        var bad = saved.copy(); bad.getList("AEB", Tag.TAG_COMPOUND).getCompound(0).putInt("amount", 999); badTags.put("stronger inconsistent AEB", bad);
        for (String field : List.of("type", "modifier")) {
            bad = saved.copy(); bad.putByte(field, (byte) 127); badTags.put("invalid enum " + field, bad);
            bad = saved.copy(); bad.putInt(field, 0); badTags.put("INT not BYTE " + field, bad);
        }
        for (var entry : badTags.entrySet()) {
            source = NodeTransductionRuntimeChecks.reload(f, source, entry.getValue(), TileNodeEnergized.class);
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            f.check(source.isQuarantined() && source.getAuraBase().size() == 0 && source.getVisBase().size() == 0
                            && source.getAvailableVis().size() == 0 && source.getRefillEpoch() == 0 && source.consumeVis(Aspect.EARTH, 99) == 0
                            && source.saveWithoutMetadata(f.level.registryAccess()).getCompound("RejectedEnergized").equals(entry.getValue()),
                    "WHOLE bad source disabled/evidence retained, never a default-free20x6/partial good subset: " + entry.getKey());
            var rejected = source.saveWithFullMetadata(f.level.registryAccess());
            source = NodeTransductionRuntimeChecks.reload(f, source, rejected, TileNodeEnergized.class);
            f.check(source.isQuarantined() && source.getUpdateTag(f.level.registryAccess()).getCompound("RejectedEnergized").equals(entry.getValue()),
                    "quarantine survives SECOND actual reload without nesting or restoring usable stock");
        }
        f.reset(); f.put(f.base, ThaumcraftNodeDevices.ENERGIZED_NODE.get());
        var blank = (TileNodeEnergized) NodeTransductionRuntimeChecks.existing(f, f.base);
        NodeTransductionRuntimeChecks.sourceTick(f, blank);
        f.check(blank.getAuraBase().size() == 0 && blank.getAspects().size() == 0 && blank.getAvailableVis().size() == 0
                        && blank.getRefillEpoch() == 0 && blank.consumeVis(Aspect.EARTH, 1) == 0,
                "raw uninitialized native factory is NOT a default-free20x6 source; no given finished energized acceptance fixture");
        boolean refused = false;
        try { blank.setAspects(NodeTransductionRuntimeChecks.primals(20)); } catch (IllegalStateException expected) { refused = true; }
        f.check(refused && blank.getAuraBase().size() == 0, "live installed source cannot be initialized/refunded by a public put/grant facade");
    }

    /** Suggested timeout240, separate batch. */
    public static void graphSelection(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open(); rangesAndNegativeCaches(f); selectionAndLos(f); colorsAndMultipleRoots(f); relayPersistence(f); coordinates(f);
        }
        test.succeed();
    }

    private static void rangesAndNegativeCaches(Fixture f) {
        f.reset(); BlockPos sourcePos = f.base.west(7), consumer = f.base;
        f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 1) == 0, "actual NEGATIVE drainer cache before any owned source");
        var source = NodeTransductionRuntimeChecks.energize(f, sourcePos, new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 1) == 1, "source registration invalidates actual cached NEGATIVE lookup");
        f.check(VisNetHandler.drainVis(f.level, sourcePos, Aspect.EARTH, 1) == 0
                        && VisNetHandler.drainVis(f.level, sourcePos.east(8), Aspect.EARTH, 1) == 1
                        && VisNetHandler.drainVis(f.level, sourcePos.east(8).above(), Aspect.EARTH, 1) == 0,
                "actual drain excludes d2=0, includes EXACT8/d2=64, excludes8-plus-height/d2=65");
        f.check(VisNetHandler.drainVis(f.level, new BlockPos(f.base.getX(), f.level.getMinBuildHeight() - 1, f.base.getZ()), Aspect.EARTH, 1) == 0
                        && VisNetHandler.drainVis(f.level, new BlockPos(f.base.getX(), f.level.getMaxBuildHeight(), f.base.getZ()), Aspect.EARTH, 1) == 0,
                "actual drain refuses both native build-height bounds without a below/above terrain read");
        var relay = relay(f, f.base, (byte) -1); relayTick(f, relay);
        f.check(parent(relay) == source && VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 2) == 2,
                "genuine native relay makes fourteen-block path source(-7)->relay0->consumer(+7), outside direct source range8");
        f.put(f.base.east(3), Blocks.STONE);
        f.check(VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 1) == 1,
                "opaque consumer-to-relay obstacle is NOT an invented consumer LOS gate");
        f.level.removeBlockEntity(f.base);
        f.check(NodeTransductionRuntimeChecks.existing(f, f.base) == null
                        && VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 1) == 0,
                "retired intermediate invalidates actual positive route; observe chunk map, NEVER lazily recreate missing relay");
        relay = relay(f, f.base, (byte) -1); relayTick(f, relay);
        f.check(parent(relay) == source && VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 1) == 1,
                "same-coordinate actual replacement/add invalidates the new NEGATIVE route too");
    }

    private static void selectionAndLos(Fixture f) {
        f.reset();
        var source = NodeTransductionRuntimeChecks.energize(f, f.base.west(4), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        var near = relay(f, f.base.west(2).south(2), (byte) -1); relayTick(f, near);
        var child = relay(f, f.base, (byte) -1); relayTick(f, child);
        f.check(parent(child) == source && parent(near) == source,
                "root candidate d2=16 MINUS childRange*2=16 beats closer relay d2=8: original root bias, not plain Euclidean nearest");
        var tie = relay(f, f.base.west(2).south(4), (byte) -1); relayTick(f, tie);
        f.check(parent(tie) == source, "same-root strict-closer tie: source20-16 == relay4, source candidate wins without <= replacement");
        // A fresh SINGLE-path graph prevents the closer relay from providing a
        // legitimate alternate path during the following LOS timing case.
        f.reset();
        source = NodeTransductionRuntimeChecks.energize(f, f.base.west(4), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        child = relay(f, f.base, Direction.EAST, (byte) -1);
        f.put(f.base.west(), Blocks.WATER); // nonAIR support, deliberately no collision in the native fluid-NONE ray
        relayTick(f, child);
        var targetHit = f.level.clip(new ClipContext(Vec3.atCenterOf(source.getBlockPos()), Vec3.atCenterOf(child.getBlockPos()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        f.check(targetHit.getType() == HitResult.Type.BLOCK && targetHit.getBlockPos().equals(child.getBlockPos())
                        && VisNetHandler.canNodeBeSeen(source, child), "ACTUAL native ray hits the target collision shape; that target-block hit is accepted, not treated as obstruction");
        BlockPos obstacle = f.base.west(1);
        f.put(obstacle, Blocks.STONE);
        f.check(!VisNetHandler.canNodeBeSeen(source, child), "actual opaque full collision blocks native attachment ray");
        f.put(obstacle, Blocks.GLASS);
        f.check(!VisNetHandler.canNodeBeSeen(source, child), "transparent GLASS still has actual collision; no opacity-only LOS approximation");
        f.put(obstacle, Blocks.WHITE_CARPET);
        f.check(VisNetHandler.canNodeBeSeen(source, child), "same nonAIR cell's carpet collision lies below center ray and does NOT occlude");
        f.level.setBlock(obstacle, Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, Direction.EAST), TubeVariantsRuntimeChecks.QUIET);
        f.check(!VisNetHandler.canNodeBeSeen(source, child), "OPEN vertical native trapdoor SHAPE crosses ray despite non-fullcube geometry");
        f.put(obstacle, Blocks.STONE);
        f.check(child.consumeVis(Aspect.EARTH, 1) == 1, "changed parent ray is NOT rechecked on each consume BEFORE original refresh phase");
        for (int i = source.getNodeCounter(); i < 40; i++) NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.check(parent(child) == source && child.consumeVis(Aspect.EARTH, 1) == 1 && source.getNodeCounter() == 40,
                "source counter1..40 preserves attachment; no eager every-request LOS gate");
        NodeTransductionRuntimeChecks.sourceTick(f, source); // old counter40 -> first/every40 graph refresh
        relayTick(f, child);
        f.check(source.getNodeCounter() == 41 && parent(child) == null && child.consumeVis(Aspect.EARTH, 1) == 0,
                "actual41st tick checks child LOS and detaches occluded child; no counter setter or fake elapsed client interval");
        f.put(obstacle, Blocks.WATER); child.nodeRefresh = true; relayTick(f, child);
        f.check(parent(child) == source, "production refresh-request flag rebuilds actual native parent after obstacle removed");
        var a = child.getChildren();
        boolean immutable = false; try { a.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        f.check(immutable, "public child snapshot cannot mutate graph ownership");
        var refs = source.getChildren(); refs.forEach(WeakReference::clear);
        f.check(parent(child) == source, "clearing returned weak snapshots cannot expire INTERNAL live graph edges");
    }

    private static void colorsAndMultipleRoots(Fixture f) {
        f.reset();
        var source = NodeTransductionRuntimeChecks.energize(f, f.base.west(7), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        var fire = relay(f, f.base, (byte) 1); relayTick(f, fire);
        var earth = relay(f, f.base.east(7), (byte) 3); relayTick(f, earth);
        f.check(parent(fire) == source && parent(earth) == null,
                "uncolored SOURCE accepts fire child, but earth child14 from source cannot attach to incompatible fire parent");
        attune(f, earth, "ignis"); relayTick(f, earth);
        f.check(parent(earth) == fire && earth.getRootSource().get() == source,
                "native NONCONSUMING shard interaction changes real link compatibility and rebuilds relay chain");
        int a = fire.consumeVis(Aspect.EARTH, 5), b = earth.consumeVis(Aspect.EARTH, 5);
        f.check(a == 5 && b == 3 && a + b + source.getAvailableVis().getAmount(Aspect.EARTH) == 8,
                "FIRE-colored relay really forwards TERRA: color is NOT an aspect filter; TWO paths to SAME ROOT share8 with no independent budget");

        f.reset();
        source = NodeTransductionRuntimeChecks.energize(f, f.base.west(7), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        var nearest = relay(f, f.base, (byte) -1); relayTick(f, nearest);
        var farther = relay(f, f.base.south(3), (byte) -1); relayTick(f, farther);
        f.check(parent(farther) == nearest && VisNetHandler.drainVis(f.level, f.base.east(6), Aspect.EARTH, 1) == 1
                        && nearest.getPulse() == 5 && farther.getPulse() == 0,
                "per-ROOT consumer chooses ONE nearest descendant (d2=36, not45); native pulse proves chosen branch, no duplicate farther-path budget");

        f.reset();
        var left = NodeTransductionRuntimeChecks.energize(f, f.base.west(7), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        var right = NodeTransductionRuntimeChecks.energize(f, f.base.east(7), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, left); NodeTransductionRuntimeChecks.sourceTick(f, right);
        f.check(VisNetHandler.drainVis(f.level, f.base, Aspect.EARTH, 12) == 12
                        && left.getAvailableVis().getAmount(Aspect.EARTH) + right.getAvailableVis().getAmount(Aspect.EARTH) == 4,
                "TWO DISTINCT roots really fulfill partial12 of total16; no unspecified HashMap root/tie-order assertion");
    }

    private static void relayPersistence(Fixture f) {
        f.reset();
        var source = NodeTransductionRuntimeChecks.energize(f, f.base.west(7), new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        var relay = relay(f, f.base, (byte) -1);
        var baseline = relay.saveWithFullMetadata(f.level.registryAccess());
        f.check(baseline.contains("color", Tag.TAG_BYTE) && baseline.getByte("color") == -1
                        && baseline.contains("orientation", Tag.TAG_BYTE) && baseline.getByte("orientation") == Direction.UP.get3DDataValue(),
                "genuine native serialization ALWAYS contains exact BYTE color AND orientation before any malformed input is made");
        relay = NodeTransductionRuntimeChecks.reload(f, relay, baseline, TileVisRelay.class);
        relayTick(f, relay);
        f.check(!relay.isQuarantined() && parent(relay) == source && relay.consumeVis(Aspect.EARTH, 1) == 1,
                "unchanged genuinely serialized baseline reloads CURRENT and routes normally; invalid-case assertions cannot start from an already broken tag");
        int available = source.getAvailableVis().getAmount(Aspect.EARTH);
        long epoch = source.getRefillEpoch();
        var malformed = new LinkedHashMap<String, CompoundTag>();
        for (String field : List.of("color", "orientation")) {
            var missing = baseline.copy(); missing.remove(field); malformed.put("missing " + field, missing);
            var wrongType = baseline.copy(); wrongType.putInt(field, baseline.getByte(field)); malformed.put("INT instead of BYTE " + field, wrongType);
        }
        for (byte color : new byte[] {-2, 6}) {
            var invalid = baseline.copy(); invalid.putByte("color", color); malformed.put("out-of-range color " + color, invalid);
        }
        for (byte orientation : new byte[] {-1, 6}) {
            var invalid = baseline.copy(); invalid.putByte("orientation", orientation); malformed.put("out-of-range orientation " + orientation, invalid);
        }
        var mismatch = baseline.copy(); mismatch.putByte("orientation", (byte) Direction.DOWN.get3DDataValue());
        malformed.put("valid orientation BYTE mismatches authoritative native UP FACING", mismatch);
        for (var entry : malformed.entrySet()) {
            relay = NodeTransductionRuntimeChecks.reload(f, relay, entry.getValue(), TileVisRelay.class);
            relayTick(f, relay);
            f.check(relay.isQuarantined() && parent(relay) == null && relay.getRootSource() == null && relay.getPulse() == 0
                            && relay.getOrientation() == Direction.UP && relay.consumeVis(Aspect.EARTH, 1) == 0
                            && VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 1) == 0
                            && source.getAvailableVis().getAmount(Aspect.EARTH) == available && source.getRefillEpoch() == epoch,
                    "WHOLE malformed relay quarantines with no stronger untyped fallback, native facing change, route, pulse, debit or refill: " + entry.getKey());
            var persisted = relay.saveWithFullMetadata(f.level.registryAccess());
            f.check(persisted.getCompound("RejectedVisRelay").equals(entry.getValue()), "exact entire rejected native payload is retained: " + entry.getKey());
            relay = NodeTransductionRuntimeChecks.reload(f, relay, persisted, TileVisRelay.class);
            relayTick(f, relay);
            f.check(relay.isQuarantined() && relay.consumeVis(Aspect.EARTH, 1) == 0 && parent(relay) == null
                            && source.getAvailableVis().getAmount(Aspect.EARTH) == available
                            && relay.saveWithFullMetadata(f.level.registryAccess()).getCompound("RejectedVisRelay").equals(entry.getValue()),
                    "SECOND actual current-owner reload preserves exact quarantine/evidence and cannot manufacture routing authority: " + entry.getKey());
        }
        relay = NodeTransductionRuntimeChecks.reload(f, relay, baseline, TileVisRelay.class);
        relayTick(f, relay);
        f.check(!relay.isQuarantined() && parent(relay) == source && relay.consumeVis(Aspect.EARTH, 1) == 1
                        && source.getAvailableVis().getAmount(Aspect.EARTH) == available - 1 && source.getRefillEpoch() == epoch,
                "unchanged native baseline recovers normal route only by explicit valid current-owner reload; no manufactured source pulse");
    }

    private static void coordinates(Fixture f) {
        var mutable = new BlockPos.MutableBlockPos(20_000_000, -63, -20_000_000);
        var point = new WorldCoordinates(f.level.dimension(), mutable); mutable.set(0, 0, 0);
        var far = new WorldCoordinates(-20_000_000, 319, 20_000_000, f.level.dimension());
        double distance = point.getDistanceSquaredToWorldCoordinates(far);
        f.check(point.x == 20_000_000 && point.y == -63 && point.z == -20_000_000 && distance > Integer.MAX_VALUE
                        && distance == far.getDistanceSquaredToWorldCoordinates(point),
                "immutable full-int native coordinates and wide symmetric distance, no short wrapping/int squared overflow");
        var tag = new CompoundTag(); point.writeNBT(tag);
        f.check(WorldCoordinates.readNBT(tag).orElseThrow().equals(point), "native full-coordinate/dimension codec round-trip");
        var other = new WorldCoordinates(f.level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER, point.pos());
        f.check(!point.equals(other) && Double.isInfinite(point.getDistanceSquaredToWorldCoordinates(other))
                        && VisNetHandler.inRange(f.level, point, other, 8) < 0,
                "same coordinates in DIFFERENT dimension are not equal or routeable");
        tag.putDouble("w_x", point.x);
        f.check(WorldCoordinates.readNBT(tag).isEmpty(), "whole location rejects wrong numeric type instead of defaulting/truncating");
    }

    static TileVisRelay relay(Fixture f, BlockPos pos, byte color) {
        return relay(f, pos, Direction.UP, color);
    }
    private static TileVisRelay relay(Fixture f, BlockPos pos, Direction orientation, byte color) {
        f.put(pos.relative(orientation.getOpposite()), Blocks.STONE); f.put(pos, Blocks.AIR);
        f.level.setBlock(pos, ThaumcraftNodeDevices.VIS_RELAY.get().defaultBlockState().setValue(BlockVisRelay.FACING, orientation), TubeVariantsRuntimeChecks.QUIET);
        var relay = (TileVisRelay) NodeTransductionRuntimeChecks.existing(f, pos);
        f.check(relay != null, "actual native relay factory/current BE");
        if (color != -1) {
            // Declared server policy INPUT (not claimed shard acquisition). Live
            // changes below use actual nonconsuming item actions instead.
            var tag = relay.saveWithFullMetadata(f.level.registryAccess()); tag.putByte("color", color);
            relay = NodeTransductionRuntimeChecks.reload(f, relay, tag, TileVisRelay.class);
        }
        return relay;
    }
    static void relayTick(Fixture f, TileVisRelay relay) {
        var state = relay.getBlockState(); var ticker = state.getTicker(f.level, ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing native relay ticker");
        ticker.tick(f.level, relay.getBlockPos(), state, relay);
    }
    static TileVisNode parent(TileVisNode node) { var ref = node.getParent(); return ref == null ? null : ref.get(); }
    static void attune(Fixture f, TileVisRelay relay, String shard) {
        var held = new ItemStack(ThaumcraftContent.SHARDS.get(shard).get(), 3);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, held); f.player.setShiftKeyDown(false); f.aim(Vec3.atCenterOf(relay.getBlockPos()));
        var before = held.copy();
        f.check(f.use(relay.getBlockPos()).consumesAction() && f.player.getMainHandItem() == held && ItemStack.matches(held, before),
                "actual native relay shard attunement changes policy WITHOUT consuming/tagging/moving even one shard");
    }
}
