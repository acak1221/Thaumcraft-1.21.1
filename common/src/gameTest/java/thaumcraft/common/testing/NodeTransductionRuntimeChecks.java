package thaumcraft.common.testing;

import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.visnet.VisNodeFailures;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileNodeEnergized;

/** Original converter33-157 / BlockAiry350-381. Explicit SERVER INPUTS: native
 * empty devices, named raw ordinary-node bases and scoped branch RNG. Calling
 * production tickers synchronously is NOT elapsed-client-time evidence. All
 * reloads replace the CURRENT registered BE, never mutate a detached substitute. */
public final class NodeTransductionRuntimeChecks {
    private static NeighborProbe conversionProbe, failureProbe;
    private NodeTransductionRuntimeChecks() {}

    /** Called ONLY by the isolated game-test mod's main initializer, before the
     * native block registry freezes. Separate probes keep the two batches apart. */
    public static synchronized void registerTestProbes() {
        if (conversionProbe == null) conversionProbe = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "node_conversion_neighbor_probe"), new NeighborProbe());
        if (failureProbe == null) failureProbe = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "node_failure_neighbor_probe"), new NeighborProbe());
    }

    private static NeighborProbe probe(NeighborProbe registered) {
        if (registered == null) throw new IllegalStateException("Game-test mod must register node probes before registry freeze");
        registered.clear();
        return registered;
    }

    /** Suggested timeout240; its1001/951 calls are actual production updates. */
    public static void conversionPersistenceAndCooldown(GameTestHelper test) {
        try (var f = fixture(test, false)) {
            f.open();
            var tower = tower(f, f.base, primals(80), TileNode.NodeModifier.PALE, TileNode.NodeType.NORMAL);
            f.check(tower.converter.getCount() == 0 && tower.converter.getStatus() == 1 && tower.converter.hasStabilizer(),
                    "fresh actual neighbor action initializes0/status1 with unpowered lower BASIC BE");
            try (var rng = new InputRandom(f, bound -> 0)) {
                converterTicks(f, tower.converter, 127);
                rng.onlyInts(127, 6, "ONE choice of a CURRENT key on EACH warm update, not a guaranteed successful debit");
            }
            f.check(tower.node.getAspects().getAmount(Aspect.AIR) == 0
                            && tower.node.getAspects().getAmount(Aspect.FIRE) == 80
                            && tower.node.getAspects().getAspects().length == 6 && tower.node.getAspectsBase().visSize() == 480,
                    "80 successful aer debits then47 failed retained-ZERO-key attempts; no invented1000-vis bill or base debit");
            var saved = tower.converter.saveWithFullMetadata(f.level.registryAccess());
            var old = tower.converter;
            var converter = reload(f, old, saved, TileNodeConverter.class);
            f.check(converter != old && converter.getCount() == 127 && converter.getStatus() == 1
                            && converter.getUpdateTag(f.level.registryAccess()).getInt("count") == 127,
                    "actual loadStatic/current replacement persists warm count/status, not a restarted timer");
            int retiredCount = old.getCount();
            TileNodeConverter.serverTick(f.level, old.getBlockPos(), old.getBlockState(), old);
            f.check(old.getCount() == retiredCount && converter.getCount() == 127, "retired old converter cannot warm its replacement");
            try (var rng = new InputRandom(f, bound -> 0)) {
                converterTicks(f, converter, 873);
                rng.onlyInts(873, 6, "remaining actual warm updates still choose among SIX retained current keys");
            }
            f.check(converter.getCount() == 1000 && existing(f, f.base) == tower.node
                            && f.level.getBlockState(f.base).is(ThaumcraftContent.AURA_NODE.get()),
                    "1000 actual updates from0 remain ORDINARY: conversion check precedes increment");
            try (var rng = new InputRandom(f, bound -> { throw new AssertionError("Unexpected conversion RNG bound" + bound); })) {
                converterTick(f, converter);
                rng.onlyInts(0, 6, "NORMAL update1001 calculates capacity without another warm-up debit/RNG choice");
            }
            var source = (TileNodeEnergized) existing(f, f.base);
            f.check(source != null && source.getAuraBase().aspects.equals(primals(80).aspects)
                            && source.getNodeType() == TileNode.NodeType.NORMAL && source.getNodeModifier() == TileNode.NodeModifier.PALE
                            && source.getNodeId().equals("blank") && source.getVisBase().aspects.values().stream().allMatch(value -> value == 8)
                            && source.getAvailableVis().visSize() == 0 && source.getRefillEpoch() == 0
                            && existing(f, f.base.below()) == tower.support && tower.node.isRemoved() && f.drops().isEmpty(),
                    "ONE native replacement captures BASE/type/mod, retains SAME lower device, original blank energized id, capacity8x6 but NO first pulse/ordinary harvest");
            var state = source.getBlockState();
            AABB outline = state.getShape(f.level, f.base, CollisionContext.empty()).bounds();
            f.check(close(outline.minX, .3) && close(outline.maxX, .7) && close(outline.minY, .3) && close(outline.maxY, .7)
                            && state.getCollisionShape(f.level, f.base, CollisionContext.empty()).isEmpty()
                            && state.getLightEmission() == 8 && state.getDestroySpeed(f.level, f.base) == 2
                            && state.getBlock().getExplosionResistance() == 200 && state.getBlock().asItem() == Items.AIR,
                    "actual energized airy target .3..7, no entity collision, light8/hardness2/resistance200 and NO portable energized item");
            sourceTick(f, source);
            f.check(source.getAvailableVis().visSize() == 48 && source.getRefillEpoch() == 1, "only FIRST actual source ticker supplies its renewable pulse");

            powerUpper(f, converter, false, false);
            converterTicks(f, converter, 400);
            f.check(converter.getCount() == 600 && converter.getStatus() == 2 && existing(f, f.base) == source,
                    "upper power OFF does not instantly remove energized supply");
            // checkStatus is an explicit ORIGINAL neighbor action, never an observer.
            converter.checkStatus();
            f.check(converter.getCount() == 1000 && converter.getStatus() == 2,
                    "original checkStatus quirk resets an energized cooldown to1000 EVEN while unpowered");
            converterTicks(f, converter, 900);
            f.check(converter.getCount() == 100, "900 real unpowered updates reach100 after the neighbor reset");
            powerUpper(f, converter, true, false);
            converterTicks(f, converter, 3);
            f.check(converter.getCount() == 103 && existing(f, f.base) == source,
                    "repower before cooldown raises actual count, preserves same source and never zeroes ordinary current");
            converter.checkStatus();
            powerUpper(f, converter, false, false);
            converterTicks(f, converter, 949);
            f.check(converter.getCount() == 51 && existing(f, f.base) == source, "949 actual cooldown updates still energized/count51");
            saved = converter.saveWithFullMetadata(f.level.registryAccess());
            converter = reload(f, converter, saved, TileNodeConverter.class);
            f.check(converter.getCount() == 51 && converter.getStatus() == 2, "native cooldown reload retains51/status2");
            converterTick(f, converter);
            f.check(converter.getCount() == 50 && existing(f, f.base) == source, "950th actual update reaches50 but is STILL energized");
            converterTick(f, converter);
            var reverted = (TileNode) existing(f, f.base);
            f.check(reverted != null && reverted != tower.node && reverted.getAspectsBase().aspects.equals(primals(80).aspects)
                            && reverted.getNodeModifier() == TileNode.NodeModifier.PALE && reverted.getNodeType() == TileNode.NodeType.NORMAL
                            && reverted.getAspects().visSize() == 0 && reverted.getAspects().getAspects().length == 6
                            && converter.getCount() == 49 && converter.getStatus() == 0 && f.drops().isEmpty(),
                    "951st actual update replaces ONE source with same BASE/type/mod CURRENT0; original same-tick decrement50->49, no free full node/drop");
            var ordinarySave = reverted.saveWithFullMetadata(f.level.registryAccess());
            reverted = reload(f, reverted, ordinarySave, TileNode.class);
            f.check(reverted.getAspects().visSize() == 0 && reverted.getAspectsBase().visSize() == 480,
                    "actual reverted-current0 persists across CURRENT native replacement, not default creative25x6");
            interruptionsAndQuarantine(f);
            conversionCallbackReplacement(f);
            energizedHasNoOrdinaryHarvest(f);
        }
        test.succeed();
    }

    private static void interruptionsAndQuarantine(Fixture f) {
        f.reset();
        var t = tower(f, f.base, new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        try (var rng = new InputRandom(f, bound -> 0)) { converterTicks(f, t.converter, 17); rng.onlyInts(17, 1, "short warm branch"); }
        powerUpper(f, t.converter, false, true);
        converterTicks(f, t.converter, 4);
        f.check(t.converter.getCount() == 13 && t.converter.getStatus() == 0 && existing(f, f.base) == t.node,
                "ordinary interruption really decays17->13/status0, not a reset or conversion");
        powerUpper(f, t.converter, true, true);
        try (var rng = new InputRandom(f, bound -> 0)) { converterTicks(f, t.converter, 2); rng.onlyInts(2, 1, "repowered ordinary attempts"); }
        f.check(t.converter.getCount() == 15 && t.converter.getStatus() == 1, "ordinary repower resumes retained13, not fresh0");
        f.level.removeBlockEntity(f.base.below());
        t.converter.checkStatus();
        try (var rng = new InputRandom(f, bound -> { throw new AssertionError("Missing lower BE should not warm/debit"); })) {
            converterTicks(f, t.converter, 3);
            rng.onlyInts(0, 1, "converter original stabilizer predicate needs its actual installed BE, unlike ordinary-node BLOCK-only lock");
        }
        f.check(t.converter.getStatus() == 0 && t.converter.getCount() == 12 && !t.converter.hasStabilizer()
                        && f.level.getBlockState(f.base.below()).is(ThaumcraftNodeDevices.NODE_STABILIZER.get())
                        && existing(f, f.base.below()) == null,
                "lower BLOCK-without-BE input cannot warm and is observed WITHOUT lazy manufacture");
        var valid = t.converter.saveWithFullMetadata(f.level.registryAccess());
        var malformed = new LinkedHashMap<String, CompoundTag>();
        for (String key : List.of("count", "status")) {
            var bad = valid.copy(); bad.put(key, new ListTag()); malformed.put("wrong field type " + key, bad);
            bad = valid.copy(); bad.remove(key); malformed.put("missing field " + key, bad);
        }
        for (int count : new int[] {-2, 1001, Integer.MAX_VALUE}) { var bad = valid.copy(); bad.putInt("count", count); malformed.put("count" + count, bad); }
        for (int status : new int[] {-1, 3}) { var bad = valid.copy(); bad.putInt("status", status); malformed.put("status" + status, bad); }
        var badSentinel = valid.copy(); badSentinel.putInt("count", -1); badSentinel.putInt("status", 1); malformed.put("status1 with sentinel-1", badSentinel);
        var current = t.converter;
        for (var entry : malformed.entrySet()) {
            current = reload(f, current, entry.getValue(), TileNodeConverter.class);
            int before = current.getCount(); converterTicks(f, current, 3); current.checkStatus();
            f.check(current.isQuarantined() && current.getCount() == before && existing(f, f.base) == t.node
                            && current.saveWithoutMetadata(f.level.registryAccess()).getCompound("RejectedConverter").equals(entry.getValue()),
                    "whole malformed converter disabled/evidence preserved, never auto-default-warms: " + entry.getKey());
        }
    }

    private static void conversionCallbackReplacement(Fixture f) {
        f.reset();
        var tower = tower(f, f.base, new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        try (var rng = new InputRandom(f, bound -> 0)) { converterTicks(f, tower.converter, 1000); rng.onlyInts(1000, 1, "actual warm-up before conversion callback"); }
        var notice = probe(conversionProbe);
        try {
            f.level.setBlock(f.base.north(), notice.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            var replacement = new TileNode[1];
            notice.callback = () -> {
                f.check(existing(f, f.base) instanceof TileNodeEnergized prepared && prepared.getAuraBase().getAmount(Aspect.EARTH) == 64
                                && prepared.getVisBase().getAmount(Aspect.EARTH) == 8 && prepared.getAvailableVis().visSize() == 0,
                        "FIRST conversion neighbor callback sees fully prepared captured BASE/capacity and ZERO pulse, never default free20x6");
                f.put(f.base, Blocks.STONE); f.put(f.base, ThaumcraftContent.AURA_NODE.get());
                replacement[0] = (TileNode) existing(f, f.base);
                replacement[0].initializeNatural(new AspectList().add(Aspect.FIRE, 7), TileNode.NodeModifier.BRIGHT);
            };
            converterTick(f, tower.converter);
            if (notice.failure != null) throw new AssertionError("Conversion notification assertion failed", notice.failure);
            f.check(notice.calls == 1 && replacement[0] != null && existing(f, f.base) == replacement[0]
                            && replacement[0].getAspects().getAmount(Aspect.FIRE) == 7
                            && replacement[0].getAspectsBase().getAmount(Aspect.FIRE) == 7
                            && replacement[0].getAspectsBase().getAmount(Aspect.EARTH) == 0
                            && replacement[0].getNodeModifier() == TileNode.NodeModifier.BRIGHT && f.pendingRows().isEmpty() && f.drops().isEmpty(),
                    "actual synchronous conversion replacement is NEVER overwritten/initialized from stale captured node; no essence harvest/second owner");
        } finally {
            try { f.put(f.base.north(), Blocks.AIR); }
            finally { notice.clear(); }
        }
    }

    private static void energizedHasNoOrdinaryHarvest(Fixture f) {
        f.reset(); var source = energize(f, f.base, new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
        f.aim(net.minecraft.world.phys.Vec3.atCenterOf(f.base));
        f.check(f.player.gameMode.destroyBlock(f.base) && source.isRemoved()
                        && !f.level.getBlockState(f.base).is(ThaumcraftNodeDevices.ENERGIZED_NODE.get())
                        && f.pendingRows().isEmpty() && f.drops().isEmpty(),
                "actual SURVIVAL native final-break action yields NO ordinary-node essence or portable energized item; not elapsed mining/client evidence");
    }

    /** Separate batch/timeout240. Isolated native blast, NOT the accepted workshop.
     * No finite-fluid flow/decay cases here; D owns those independently. */
    public static void isolatedFailureLifecycle(GameTestHelper test) {
        try (var f = fixture(test, true)) {
            f.open();
            // The retained fixture base is final. Raise ONLY this blast's source
            // one block: its full saved +/-8 margin then starts at y-64, not-65.
            BlockPos origin = f.base.above();
            f.check(f.bounds.contains(net.minecraft.world.phys.Vec3.atCenterOf(origin.offset(-7, -7, -7)))
                            && f.bounds.contains(net.minecraft.world.phys.Vec3.atCenterOf(origin.offset(7, 7, 7))),
                    "FULL native trial offset cube remains inside retained fixture item/escrow cleanup bounds");
            for (int cause = 0; cause < 3; cause++) {
                f.reset();
                var source = energize(f, origin, new AspectList().add(Aspect.EARTH, 64), null, TileNode.NodeType.NORMAL);
                sourceTick(f, source);
                var upper = (TileNodeConverter) existing(f, origin.above());
                f.put(origin.below(2), Blocks.AIR);
                f.put(origin.above(2), Blocks.AIR);
                var callback = probe(failureProbe);
                try {
                    f.level.setBlock(origin.north(), callback.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                    callback.callback = () -> {
                        f.check(f.level.getBlockState(origin).isAir() && existing(f, origin) != source
                                        && source.isRemoved() && source.getAvailableVis().visSize() == 0,
                                "FIRST source-removal neighbor callback already sees AIR/current absence/retired pulse, BEFORE native explosion/trials");
                        f.check(explosions(f) == 0 && InputRandom.activeIntCount() == 0,
                                "native removal notification precedes BOTH actual explosion packet and all fifty-trial RNG draws");
                        VisNodeFailures.checkTower(source);
                        // Actual replacement while failure is notifying: old owner
                        // may neither remove it after blast nor birth flux over it.
                        f.level.setBlock(origin, Blocks.DIAMOND_BLOCK.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                    };
                    f.player.setPos(origin.getX() + 20.5, origin.getY() + .5, origin.getZ() + .5);
                    f.check(f.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(origin).inflate(7)).isEmpty(),
                            "isolated blast footprint contains NO prior players/entities/items to damage; accepted workshop is never the test world");
                    f.chat(); // discard prior actor packets before observing ONLY this native explosion
                    final int reason = cause;
                    try (var rng = new InputRandom(f, bound -> {
                        if (bound != 8) throw new AssertionError("Unexpected burst integer bound " + bound);
                        // Alternate two targets, then repeat: y-2 goo and y+2 gas.
                        // All fifty trials still draw SIX ints even after occupancy.
                        int call = InputRandom.activeIntCount();
                        int trial = (call - 1) / 6, component = (call - 1) % 6;
                        return component == (trial % 2 == 0 ? 3 : 2) ? 2 : 0;
                    }, true)) {
                        if (reason == 0) {
                            f.level.setBlock(origin.below(), Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                            upper.checkStatus();
                        } else if (reason == 1) {
                            f.level.setBlock(origin.below().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                            upper.checkStatus();
                        } else {
                            f.level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                        }
                        if (callback.failure != null) throw new AssertionError("Failure notification assertion failed", callback.failure);
                        rng.onlyInts(300, 8, "ONE native original fifty-trial burst, six bound8 draws EACH, no duplicate failure/reentrant retry");
                        f.check(explosions(f) == 1, "actual native server emits EXACTLY ONE radius3 explosion packet, not a textual/explodify mock");
                        f.check(callback.calls == 1 && f.level.getBlockState(origin).is(Blocks.DIAMOND_BLOCK)
                                        && source.isQuarantined() && source.getAvailableVis().visSize() == 0,
                                "known loaded remove/power-lower/remove-upper fails exact old identity ONCE and preserves synchronous replacement; cause" + reason);
                        var goo = f.level.getBlockState(origin.below(2));
                        var gas = f.level.getBlockState(origin.above(2));
                        f.check(goo.is(ThaumcraftFlux.FLUX_GOO.get()) && gas.is(ThaumcraftFlux.FLUX_GAS.get())
                                        && goo.getValue(FluxFiniteBlock.LEVEL) == 8 && gas.getValue(FluxFiniteBlock.LEVEL) == 8
                                        && FluxFiniteBlock.quanta(goo) == 9 && FluxFiniteBlock.quanta(gas) == 9,
                                "only TWO unique AIR births from fifty duplicate trials: below goo9/above gas9, NOT clamped capacity8");
                        VisNodeFailures.checkTower(source); sourceTick(f, source); upper.checkStatus();
                        rng.onlyInts(300, 8, "retired source/repeated callbacks never replay original burst or generation");
                        f.check(explosions(f) == 0, "retired owner cannot issue a SECOND native explosion packet");
                    }
                    f.check(f.level.getBlockState(origin.north()).getBlock() == callback,
                            "actual native NONE-interaction blast preserves the neighbor terrain (no terrain-destruction permission)");
                } finally {
                    try { f.put(origin.north(), Blocks.AIR); }
                    finally { callback.clear(); }
                }
            }
        }
        test.succeed();
    }

    record Tower(TileNode node, TileNodeConverter converter, BlockEntity support) {}

    static Tower tower(Fixture f, BlockPos pos, AspectList base, TileNode.NodeModifier modifier, TileNode.NodeType type) {
        f.put(pos.below(), ThaumcraftNodeDevices.NODE_STABILIZER.get());
        f.put(pos, ThaumcraftContent.AURA_NODE.get());
        var node = (TileNode) existing(f, pos);
        f.check(node != null, "actual native ordinary-node factory creates this owned SERVER INPUT");
        node.initializeNatural(base, modifier);
        if (type != TileNode.NodeType.NORMAL) {
            var tag = node.saveWithFullMetadata(f.level.registryAccess()); tag.putByte("type", (byte) type.ordinal());
            node = reload(f, node, tag, TileNode.class); // declared RAW special-TYPE storage input, not its ecology
        }
        f.put(pos.above(), ThaumcraftNodeDevices.NODE_TRANSDUCER.get());
        var converter = (TileNodeConverter) existing(f, pos.above());
        powerUpper(f, converter, true, true);
        return new Tower(node, converter, existing(f, pos.below()));
    }

    static TileNodeEnergized energize(Fixture f, BlockPos pos, AspectList base, TileNode.NodeModifier modifier, TileNode.NodeType type) {
        var tower = tower(f, pos, base, modifier, type);
        try (var rng = new InputRandom(f, bound -> type == TileNode.NodeType.UNSTABLE && bound == 5 ? 2 : 0)) {
            converterTicks(f, tower.converter, 1001);
            f.check(rng.bounds.stream().filter(bound -> bound == base.size()).count() >= 1000,
                    "fixture source is EARNED by1001 actual converter updates from its declared RAW node, never given energized stock");
        }
        var current = existing(f, pos);
        f.check(current instanceof TileNodeEnergized source && source.getAvailableVis().visSize() == 0 && source.getRefillEpoch() == 0,
                "actual converted source starts ZERO pulse before any source tick");
        return (TileNodeEnergized) current;
    }

    static void powerUpper(Fixture f, TileNodeConverter converter, boolean on, boolean neighborAction) {
        f.put(converter.getBlockPos().east(), on ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        f.check(converter.isPowered() == on, "actual upper redstone power, not status/count injection");
        if (neighborAction) converter.checkStatus();
    }
    static void converterTick(Fixture f, TileNodeConverter tile) {
        var pos = tile.getBlockPos(); var state = f.level.getBlockState(pos);
        var ticker = state.getTicker(f.level, ThaumcraftNodeDevices.NODE_TRANSDUCER_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing production transducer ticker");
        ticker.tick(f.level, pos, state, tile);
    }
    static void converterTicks(Fixture f, TileNodeConverter tile, int count) { for (int i = 0; i < count; i++) converterTick(f, tile); }
    static void sourceTick(Fixture f, TileNodeEnergized tile) {
        // Stale-owner probes deliberately retain the original state argument.
        var ticker = tile.getBlockState().getTicker(f.level, ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing production energized ticker");
        ticker.tick(f.level, tile.getBlockPos(), tile.getBlockState(), tile);
    }
    static BlockEntity existing(Fixture f, BlockPos pos) {
        var chunk = f.level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntities().get(pos); // NEVER lazy create an absent BE
    }
    static <T extends BlockEntity> T reload(Fixture f, T old, CompoundTag tag, Class<T> type) {
        BlockPos pos = old.getBlockPos();
        f.check(existing(f, pos) == old, "reload detaches ONLY the CURRENT installed native owner");
        f.level.removeBlockEntity(pos);
        BlockEntity loaded = BlockEntity.loadStatic(pos, f.level.getBlockState(pos), tag, f.level.registryAccess());
        f.check(type.isInstance(loaded), "actual registered native loadStatic factory " + type.getSimpleName());
        f.level.setBlockEntity(loaded);
        f.check(existing(f, pos) == loaded && loaded.getLevel() == f.level && loaded != old, "loaded owner is really installed/current, not a detached mutation");
        return type.cast(loaded);
    }
    static AspectList primals(int amount) {
        var out = new AspectList();
        for (Aspect aspect : List.of(Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY)) out.add(aspect, amount);
        return out;
    }

    /** Extends ONLY the retained fixture's explicitly saved/restored cells. Every
     * column must ALREADY be loaded; bounds are NOT a terrain-loading mechanism.
     * Sources at-7, relay0 and consumers+7 are fourteen blocks apart. All output
     * cells stay inside retained bounds8 so its ownership/escrow cleanup applies.
     * Blast source alone rises1: candidate offsets +/-7 remain within those
     * retained bounds, while its FULL +/-8 saved/read margin stays in height. */
    static Fixture fixture(GameTestHelper test, boolean blast) {
        BlockPos base = test.absolutePos(new BlockPos(3, 3, 3)).above(blast ? 1 : 0);
        int radius = blast ? 11 : 12, vertical = blast ? 8 : 3;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            test.assertTrue(test.getLevel().getChunkSource().getChunkNow((base.getX() + x) >> 4, (base.getZ() + z) >> 4) != null,
                    "owned VisNet/power/ray/blast footprint must ALREADY be loaded before any native fixture read");
        }
        test.assertTrue(!test.getLevel().isOutsideBuildHeight(base.below(vertical)) && !test.getLevel().isOutsideBuildHeight(base.above(vertical)),
                "explicitly bounded owned fixture is inside native build height");
        var f = new Fixture(test);
        for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-radius, -vertical, -radius), base.offset(radius, vertical, radius))) {
            BlockPos pos = mutable.immutable();
            if (f.states.containsKey(pos)) continue;
            f.states.put(pos, f.level.getBlockState(pos));
            BlockEntity tile = existing(f, pos);
            if (tile != null) f.originals.put(pos, tile);
        }
        return f;
    }

    /** Synchronous SERVER branch INPUT only. Never seed/advance/rewind production
     * RNG; finally restores the EXACT original instance. The optional explosion
     * prefix accepts native noninteger effects separately, never counts those as
     * fifty-trial placement draws. No asynchronous code runs while installed. */
    static final class InputRandom implements AutoCloseable {
        private static final ThreadLocal<InputRandom> ACTIVE = new ThreadLocal<>();
        final Fixture f; final Field field; final RandomSource original, input;
        final List<Integer> bounds = new ArrayList<>(); final List<String> effects = new ArrayList<>();
        InputRandom(Fixture f, IntUnaryOperator values) { this(f, values, false); }
        InputRandom(Fixture f, IntUnaryOperator values, boolean nativeEffects) {
            this.f = f; original = f.level.random;
            if (ACTIVE.get() != null) throw new IllegalStateException("Nested branch INPUT");
            input = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(), new Class<?>[] {RandomSource.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "toString" -> "M09b SERVER-only branch INPUT";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError(method);
                };
                if (method.getName().equals("nextInt") && args != null && args.length == 1) {
                    int bound = (Integer) args[0]; bounds.add(bound);
                    int result = values.applyAsInt(bound);
                    f.check(bound > 0 && result >= 0 && result < bound, "explicit branch input respects ACTUAL original RNG bound" + bound);
                    return result;
                }
                effects.add(method.getName());
                if (nativeEffects) return switch (method.getName()) {
                    case "nextFloat" -> .5F; case "nextDouble", "nextGaussian" -> .5D; case "nextBoolean" -> false;
                    // Keep the real default triangle implementation: two declared .5 uniform samples yield its mean.
                    case "triangle" -> java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
                    default -> throw new AssertionError("Unexpected native explosion effect RNG " + method);
                };
                throw new AssertionError("Unexpected branch RNG " + method);
            });
            try { field = Level.class.getDeclaredField("random"); field.setAccessible(true); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot locate test-only Level RNG", failure); }
            try { field.set(f.level, input); ACTIVE.set(this); f.check(f.level.random == input, "scoped branch INPUT installed without a production seed"); }
            catch (IllegalAccessException | RuntimeException | Error failure) {
                try { field.set(f.level, original); } catch (IllegalAccessException restore) { failure.addSuppressed(restore); }
                ACTIVE.remove(); throw new IllegalStateException("Cannot install scoped branch INPUT", failure);
            }
        }
        static int activeIntCount() { return ACTIVE.get().bounds.size(); }
        void onlyInts(int count, int bound, String why) {
            f.check(bounds.size() == count && bounds.stream().allMatch(value -> value == bound), why + "; actual draws=" + bounds.size());
        }
        @Override public void close() {
            try {
                boolean owned = f.level.random == input;
                field.set(f.level, original);
                f.check(owned && f.level.random == original, "finally restores EXACT original RNG identity without seed/advance/rewind");
            } catch (IllegalAccessException failure) { throw new IllegalStateException("Cannot restore native RNG", failure); }
            finally { ACTIVE.remove(); }
        }
    }

    static final class NeighborProbe extends Block {
        Runnable callback; int calls; Throwable failure;
        NeighborProbe() {
            super(BlockBehaviour.Properties.of().strength(-1).noLootTable());
            getStateDefinition().getPossibleStates().forEach(BlockState::initCache);
        }
        void clear() { callback = null; calls = 0; failure = null; }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
            if (callback == null) return;
            var once = callback; callback = null; ++calls;
            try { once.run(); }
            catch (RuntimeException | Error problem) { failure = problem; throw problem; }
        }
    }
    private static boolean close(double a, double b) { return Math.abs(a - b) < 0.00001; }

    private static int explosions(Fixture f) {
        f.connection.flushChannel(); f.channel.runPendingTasks(); Object packet; int count = 0;
        while ((packet = f.channel.readOutbound()) != null) {
            try {
                if (packet instanceof ClientboundExplodePacket explosion) {
                    ++count;
                    f.check(explosion.getPower() == 3.0F && explosion.getToBlow().isEmpty(),
                            "actual native explosion packet has literal power3 and NO terrain-destruction list, not guessed visual/audio radius");
                }
            } finally { ReferenceCountUtil.release(packet); }
        }
        return count;
    }
}
