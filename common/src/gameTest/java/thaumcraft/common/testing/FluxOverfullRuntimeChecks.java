package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.potions.FluxEffects;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** M09b explicit SERVER INPUTS: registered phase states/quantities, eligible
 * actors, controlled bounded RandomSource outcomes and isolated native callback
 * probes. No production/client seeds, cloned installed stock, clock/counter
 * setters, fluid/API stub, survival acquisition or full M11 ecology claim.
 * Root owns three Fabric wrappers/required names; use separate batches. */
public final class FluxOverfullRuntimeChecks {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final ResourceLocation CALLBACK_PROBE_ID = ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "flux_callback_neighbor_probe");
    private static NeighborProbe callbackProbe;
    private FluxOverfullRuntimeChecks() {}

    /** Call from the isolated GameTest mod's main initializer BEFORE registries
     * freeze. Repeated calls reuse the same registered test-only block. */
    public static void registerTestProbes() {
        if (callbackProbe != null) return;
        callbackProbe = Registry.register(BuiltInRegistries.BLOCK, CALLBACK_PROBE_ID, new NeighborProbe());
    }

    public static void overfullFlowStateAndPersistence(GameTestHelper test) {
        preflight(test);
        try (var f = new Fixture(test)) {
            f.open();
            schemaAndNativeSave(f);
            vertical(f);
            horizontal(f);
            crucibleCapacity(f);
        }
        test.succeed();
    }

    public static void originalExposureAndGooTail(GameTestHelper test) {
        preflight(test);
        try (var f = new Fixture(test)) {
            f.open();
            exposure(f);
            gooTail(f);
            effectCallbacks(f);
        }
        test.succeed();
    }

    /** Also waits for REAL scheduled server ticks: sealed initial tick, wall
     * removal -> neighbor wake -> actual transfer. Suggested timeout240. */
    public static void callbacksBoundariesAndNeighborWake(GameTestHelper test) {
        preflight(test);
        var f = new Fixture(test);
        try {
            f.open();
            callbackFences(f);
            boundariesAndUnavailable(f);
            seal(f);
            var gas = ThaumcraftFlux.FLUX_GAS.get();
            put(f, f.base, gas, 9);
            f.check(f.level.getBlockTicks().hasScheduledTick(f.base, gas), "actual onPlace scheduled the registered gas ticker");
            test.runAfterDelay(14, () -> {
                try {
                    f.check(amount(f, f.base) == 9 && mass(f) == 9, "sealed9 survives the actual initial scheduled tick without normalization");
                    f.check(!f.level.getBlockTicks().hasScheduledTick(f.base, gas), "sealed finite tick does not reschedule a processing flood");
                    f.level.setBlock(f.base.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    f.check(f.level.getBlockTicks().hasScheduledTick(f.base, gas), "real ceiling removal wakes native flow through neighborChanged");
                    test.runAfterDelay(14, () -> {
                        try {
                            f.check(amount(f, f.base) == 1 && f.level.getBlockState(f.base.above()).is(gas)
                                            && amount(f, f.base.above()) == 8 && mass(f) == 9,
                                    "actual scheduled tick after neighbor wake performs original AIR8+1, no manual completion/timing field writes");
                            test.succeed();
                        } finally { f.close(); }
                    });
                } catch (RuntimeException | Error failure) { f.close(); throw failure; }
            });
        } catch (RuntimeException | Error failure) { f.close(); throw failure; }
    }

    private static void schemaAndNativeSave(Fixture f) {
        seal(f);
        f.check(FluxFiniteBlock.QUANTA == 8, "ordinary merge capacity remains EIGHT");
        for (FluxFiniteBlock fluid : fluids()) {
            f.check(fluid.getStateDefinition().getPossibleStates().size() == 18 && fluid.defaultBlockState().getValue(FluxFiniteBlock.LEVEL) == 0,
                    "nine amounts times two GEOMETRY flags, old default1 retained");
            for (int metadata = 0; metadata <= 8; metadata++) for (boolean full : new boolean[] {false, true}) {
                var state = fluid.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, metadata).setValue(FluxFiniteBlock.FULL_HEIGHT, full);
                f.level.setBlock(f.base, state, TubeVariantsRuntimeChecks.QUIET);
                f.check(amount(f, f.base) == metadata + 1 && FluxFiniteBlock.quantaPercentage(state) == (metadata + 1) / 8F
                                && state.getLightEmission() == metadata * 7 / 8 && state.getFluidState().isEmpty()
                                && state.getShape(f.level, f.base).isEmpty() && state.getCollisionShape(f.level, f.base).isEmpty()
                                && state.canBeReplaced(new BlockPlaceContext(f.context(f.base))) == (metadata < 2),
                        "actual phase/metadata0..8 quantity/denominator/light/no native Fluid/empty shapes; full flag never mints stock");
                var encoded = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
                f.check(BlockState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow() == state,
                        "registered native blockstate codec retains exact phase, metadata and geometry, including9");
                if (metadata == 8) {
                    // Actual native chunk save, then native SECTION palette codec
                    // readback; decoded data is NEVER installed as a stock clone.
                    var chunk = f.level.getChunkSource().getChunkNow(f.base.getX() >> 4, f.base.getZ() >> 4);
                    f.check(chunk != null, "chunk save reads the ALREADY loaded owned chunk");
                    var saved = ChunkSerializer.write(f.level, chunk);
                    boolean found = false;
                    for (Tag raw : saved.getList("sections", Tag.TAG_COMPOUND)) {
                        var section = (net.minecraft.nbt.CompoundTag) raw;
                        if (section.getByte("Y") != f.base.getY() >> 4 || !section.contains("block_states")) continue;
                        var codec = PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC,
                                PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
                        var palette = codec.parse(NbtOps.INSTANCE, section.getCompound("block_states")).getOrThrow();
                        f.check(palette.get(f.base.getX() & 15, f.base.getY() & 15, f.base.getZ() & 15) == state
                                        && f.level.getBlockState(f.base) == state && amount(f, f.base) == 9,
                                "actual ChunkSerializer output/native palette readback preserves exact9 BEFORE a tick, not clamp/split/load-normalization");
                        found = true;
                    }
                    f.check(found, "actual saved native section contains the owned level8 cell; not a synthetic hand-built palette");
                }
            }
            put(f, f.base, fluid, 9);
            f.check(f.level.destroyBlock(f.base, true, f.player) && f.drops().isEmpty(), "native nine-unit flux destruction has NO item/BE/container loot");
        }
        // Registered geometry adaptation, not original corner-render parity.
        put(f, f.base, ThaumcraftFlux.FLUX_GAS.get(), 9);
        f.check(!f.level.getBlockState(f.base).getValue(FluxFiniteBlock.FULL_HEIGHT), "gas under solid ceiling uses inverted level8 model");
        f.put(f.base.above(), Blocks.AIR);
        var open = ThaumcraftFlux.FLUX_GAS.get().withGeometry(f.level.getBlockState(f.base), f.level, f.base);
        f.check(open.getValue(FluxFiniteBlock.FULL_HEIGHT) && FluxFiniteBlock.quanta(open) == 9,
                "open-ceiling gas full geometry does not change actual9; precise model bytes belong to root resource gate");
    }

    private static void vertical(Fixture f) {
        for (FluxFiniteBlock fluid : fluids()) {
            for (int destination : new int[] {0, 1, 7, 8, 9}) {
                seal(f);
                BlockPos target = f.base.relative(fluid.verticalDirection());
                put(f, f.base, fluid, 9);
                if (destination == 0) f.put(target, Blocks.AIR); else put(f, target, fluid, destination);
                try (var random = new Script(f, tail(fluid, i(30, 1)))) {
                    tick(f, f.base, random.random); random.done("vertical merge/full-target exact finite0 + original goo ceiling-tail30");
                }
                int source = destination < 8 ? destination + 1 : 9;
                int next = destination < 8 ? 8 : destination;
                f.check(amount(f, f.base) == source && amount(f, target) == next && mass(f) == 9 + destination,
                        "original SAME/AIR capacity8 overflow vs saturated8/overfull9 rejection: " + fluid + "/" + destination);
            }
            seal(f);
            BlockPos target = f.base.relative(fluid.verticalDirection());
            put(f, f.base, fluid, 9); f.put(target, Blocks.WATER);
            try (var random = new Script(f, tail(fluid, i(30, 1)))) {
                tick(f, f.base, random.random); random.done("actual water displacement finite0; lawful moved-away goo AIR tail30");
            }
            f.check(f.level.isEmptyBlock(f.base) && f.level.getBlockState(target).is(fluid) && amount(f, target) == 9 && mass(f) == 9,
                    "WATER is not AIR: direct displacement transfers ALL9 into one real target block");
            f.check(f.level.getBlockTicks().hasScheduledTick(target, fluid), "actual displaced9 target receives native phase tick scheduling");
        }
        for (boolean gasOrigin : new boolean[] {true, false}) {
            seal(f);
            var origin = gasOrigin ? ThaumcraftFlux.FLUX_GAS.get() : ThaumcraftFlux.FLUX_GOO.get();
            var other = gasOrigin ? ThaumcraftFlux.FLUX_GOO.get() : ThaumcraftFlux.FLUX_GAS.get();
            BlockPos target = f.base.relative(origin.verticalDirection());
            int otherAmount = gasOrigin ? 5 : 9;
            put(f, f.base, origin, 9); put(f, target, other, otherAmount);
            try (var random = new Script(f, tail(origin, i(30, 1)))) {
                tick(f, f.base, random.random); random.done("native density-swap exact finite0 + actual original-source goo tail");
            }
            f.check(f.level.getBlockState(f.base).is(other) && amount(f, f.base) == otherAmount
                            && f.level.getBlockState(target).is(origin) && amount(f, target) == 9 && mass(f) == 9 + otherAmount,
                    "density swap retains ALL9 AND exact other-phase quantity, never merges phases or clamps target");
        }
    }

    private static void horizontal(Fixture f) {
        for (FluxFiniteBlock fluid : fluids()) {
            for (int air = 0; air <= 4; air++) {
                seal(f); put(f, f.base, fluid, 9);
                for (int n = 0; n < air; n++) f.put(f.base.relative(SIDES[n]), Blocks.AIR);
                var draws = new ArrayList<Draw>();
                if (air == 4) draws.add(i(1, 0));
                if (fluid == ThaumcraftFlux.FLUX_GOO.get()) draws.add(i(30, 1));
                try (var random = new Script(f, draws.toArray(Draw[]::new))) {
                    tick(f, f.base, random.random); random.done("strict original N/S/W/E nine-unit remainder calls, including bound1");
                }
                int[] source = {9, 5, 3, 3, 2};
                f.check(amount(f, f.base) == source[air] && mass(f) == 9, "real native9 horizontal sharing conserves finite stock for eligible count" + air);
                for (int n = 0; n < air; n++) {
                    int expected = air == 4 ? (n == 0 ? 1 : 2) : 9 / (air + 1);
                    f.check(amount(f, f.base.relative(SIDES[n])) == expected, "exact native allocation in original cardinal order: " + SIDES[n]);
                }
            }
            for (int neighbor : new int[] {7, 8, 9}) {
                seal(f); put(f, f.base, fluid, 9); put(f, f.base.north(), fluid, neighbor);
                try (var random = new Script(f, tail(fluid, i(30, 1)))) { tick(f, f.base, random.random); random.done("strict <remaining-1 eligibility draws no finite RNG"); }
                f.check(amount(f, f.base) == (neighbor == 7 ? 8 : 9) && amount(f, f.base.north()) == (neighbor == 7 ? 8 : neighbor)
                                && mass(f) == 9 + neighbor, "q7 eligible, saturated8/overfull9 NOT eligible");
            }
            seal(f); put(f, f.base, fluid, 9);
            FluxFiniteBlock other = fluid == ThaumcraftFlux.FLUX_GAS.get() ? ThaumcraftFlux.FLUX_GOO.get() : ThaumcraftFlux.FLUX_GAS.get();
            put(f, f.base.north(), other, 9);
            try (var random = new Script(f, tail(fluid, i(30, 1)))) { tick(f, f.base, random.random); random.done("other flux phase9 is neither horizontally displaced nor same-fluid quanta"); }
            f.check(f.level.getBlockState(f.base).is(fluid) && f.level.getBlockState(f.base.north()).is(other)
                            && amount(f, f.base) == 9 && amount(f, f.base.north()) == 9 && mass(f) == 18,
                    "new ninth state retains the original horizontal cross-phase material veto, unlike vertical density swap");
        }
        seal(f); var gas = ThaumcraftFlux.FLUX_GAS.get(); put(f, f.base, gas, 8);
        for (Direction side : SIDES) f.put(f.base.relative(side), Blocks.AIR);
        try (var random = new Script(f, i(2, 0), i(1, 0))) { tick(f, f.base, random.random); random.done("unchanged ordinary8 deterministic branch requires bounds2,1"); }
        f.check(amount(f, f.base) == 2 && amount(f, f.base.north()) == 1 && amount(f, f.base.south()) == 1
                        && amount(f, f.base.west()) == 2 && amount(f, f.base.east()) == 2 && mass(f) == 8,
                "new overfull support does NOT alter accepted ordinary8 allocator/RNG or raise ordinary capacity");
    }

    private static void crucibleCapacity(Fixture f) {
        for (FluxFiniteBlock fluid : fluids()) {
            seal(f); put(f, f.base.above(), fluid, 9); f.put(f.base.east(), Blocks.AIR);
            try (var random = new Script(f, i(4, 0), i(3, 2), i(3, 1), i(3, 1), b(true))) {
                ThaumcraftFlux.spill(f.level, f.base); random.done("exceptional9 is full for original crucible scatter, not a new tenth-unit merge");
            }
            f.check(amount(f, f.base.above()) == 9 && amount(f, f.base.east()) == 1 && mass(f) == 10,
                    "old crucible max8/air-only scatter contract unchanged even beside a nine-unit birth");
        }
    }

    private static void exposure(Fixture f) {
        var gas = ThaumcraftFlux.FLUX_GAS.get();
        for (int metadata = 0; metadata <= 8; metadata++) for (boolean exhaust : new boolean[] {false, true}) {
            seal(f); f.player.removeAllEffects(); put(f, f.base, gas, metadata + 1);
            try (var random = new Script(f, i(10, 0), b(exhaust))) { collide(f, f.player); random.done("original chance10 then boolean, one native unit paid"); }
            var effect = f.player.getEffect(exhaust ? ThaumcraftFlux.visExhaust() : MobEffects.CONFUSION);
            f.check(effect != null && effect.getDuration() == (exhaust ? 1200 : 80 + metadata * 20)
                            && effect.getAmplifier() == (exhaust ? metadata / 3 : 0) && effect.isAmbient() == exhaust
                            && amount(f, f.base) == metadata && !f.player.hasEffect(MobEffects.WITHER)
                            && FluxEffects.isUncurable(effect) == exhaust,
                    "all original metadata0..8 typed effect costs/durations/amp/ambient and exact consume1; confusion is NOT Wither");
        }
        for (boolean exhaust : new boolean[] {false, true}) {
            seal(f); f.player.removeAllEffects(); put(f, f.base, gas, 9);
            f.player.addEffect(new MobEffectInstance(exhaust ? ThaumcraftFlux.visExhaust() : MobEffects.CONFUSION, 200));
            try (var random = new Script(f, i(10, 0))) { collide(f, f.player); random.done("already exhausted/NAUSEATED admission consumes only original chance roll"); }
            f.check(amount(f, f.base) == 9, "existing original guard effect prevents duplicate stock debit");
        }
        seal(f); f.player.removeAllEffects(); put(f, f.base, gas, 9);
        f.player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
        try (var random = new Script(f, i(10, 0), b(false))) { collide(f, f.player); random.done("Wither alone is NOT an original gas admission guard"); }
        f.check(amount(f, f.base) == 8 && f.player.hasEffect(MobEffects.WITHER)
                        && f.player.getEffect(MobEffects.CONFUSION).getDuration() == 240,
                "positive original nausea240 branch still runs beside unrelated Wither, without removing that unrelated effect");
        f.player.removeAllEffects(); put(f, f.base, gas, 9);
        try (var random = new Script(f, i(10, 1))) { collide(f, f.player); random.done("chance miss stops before all eligibility/boolean draws"); }
        f.check(amount(f, f.base) == 9 && f.player.getActiveEffects().isEmpty(), "miss leaves all native9 stock and effects unchanged");
        var zombie = EntityType.ZOMBIE.create(f.level);
        var tainted = new TaintedStand(f.level);
        var nonliving = new ItemEntity(f.level, f.base.getX(), f.base.getY(), f.base.getZ(), new ItemStack(Items.STONE));
        f.check(zombie != null, "actual registered native undead fixture");
        zombie.setPos(Vec3.atCenterOf(f.base)); tainted.setPos(Vec3.atCenterOf(f.base));
        try {
            for (Entity entity : List.of(zombie, tainted, nonliving)) {
                try (var random = new Script(f, i(10, 0))) { collide(f, entity); random.done("undead/tainted/nonliving still consume chance first, never boolean"); }
                f.check(amount(f, f.base) == 9, "all original immune actors preserve all9");
            }
        } finally { zombie.discard(); tainted.discard(); nonliving.discard(); }
        put(f, f.base, gas, 9);
        try (var random = new Script(f, i(10, 0), b(true))) { collide(f, f.player); random.done("nine-unit exhaustion fixture"); }
        f.player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        var milk = Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), f.level, f.player);
        var effect = f.player.getEffect(ThaumcraftFlux.visExhaust());
        f.check(milk.is(Items.BUCKET) && effect != null && FluxEffects.isUncurable(effect) && !f.player.hasEffect(MobEffects.POISON)
                        && Math.abs(ThaumcraftFlux.visPenalty(f.player) - .3F) < .00001F,
                "md8 retains original scoped no-milk exhaustion, ordinary cure and strongest-amp30% pricing");
        var loaded = MobEffectInstance.load((net.minecraft.nbt.CompoundTag) effect.save());
        f.player.removeAllEffects(); f.player.addEffect(loaded);
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), f.level, f.player);
        f.check(!f.player.hasEffect(ThaumcraftFlux.visExhaust()), "native save/load retains original curative reset quirk, not global permanent immunity");
        put(f, f.base, ThaumcraftFlux.FLUX_GOO.get(), 9); f.player.setDeltaMovement(.8, .2, -.4);
        try (var random = new Script(f)) { collide(f, f.player); random.done("ordinary goo collision uses NO RNG and no resource debit"); }
        effect = f.player.getEffect(ThaumcraftFlux.visExhaust());
        f.check(f.player.getDeltaMovement().equals(new Vec3(-.1, .2, .05)) && amount(f, f.base) == 9
                        && effect != null && effect.getDuration() == 600 && effect.getAmplifier() == 2 && FluxEffects.isUncurable(effect),
                "actual q9/8 makes multiplier-.125, reverses X/Z without clamping, Y/current9 unchanged;600/md8/3=2");
        f.player.removeAllEffects();
    }

    private static void gooTail(Fixture f) {
        var goo = ThaumcraftFlux.FLUX_GOO.get();
        for (int metadata : new int[] {2, 5}) {
            seal(f); put(f, f.base, goo, metadata + 1); f.put(f.base.above(), Blocks.AIR);
            try (var random = new Script(f, i(25, 1), i(30, 0), b(true))) { tick(f, f.base, random.random); random.done("original lower25 MISS falls through30 HIT then boolean"); }
            f.check(amount(f, f.base) == metadata && f.level.getBlockState(f.base.above()).is(ThaumcraftFlux.FLUX_GAS.get())
                            && amount(f, f.base.above()) == 1 && mass(f) == metadata + 1,
                    "actual production lower-density missed-spawn branch converts ONE goo unit into gas, not a nested early return");
        }
        for (Draw[] branch : new Draw[][] {{i(25, 0)}, {i(25, 1), i(50, 0)}, {i(25, 1), i(50, 1)}}) {
            seal(f); put(f, f.base, goo, 9); f.put(f.base.above(), Blocks.AIR);
            try (var random = new Script(f, branch)) { tick(f, f.base, random.random); random.done("high9 original25/conditional50 RNG, NOT30"); }
            f.check(amount(f, f.base) == 9 && f.level.getBlockState(f.base.above()).isAir() && mass(f) == 9,
                    "real M11 slime/taint still explicitly DEFERRED: no vanilla slime, fake entity debit or invented ninth-unit spill");
        }
        seal(f); put(f, f.base, goo, 9);
        try (var random = new Script(f, i(30, 0), b(true))) { tick(f, f.base, random.random); random.done("closed ceiling decay still draws boolean BEFORE failed air test"); }
        f.check(amount(f, f.base) == 8 && f.level.getBlockState(f.base.above()).is(Blocks.STONE) && mass(f) == 8,
                "lawful original closed-ceiling decay loses exactly1; no fake extra gas when above is not AIR");
        seal(f); put(f, f.base, goo, 9); f.put(f.base.below(), Blocks.WATER);
        try (var random = new Script(f, i(30, 0))) { tick(f, f.base, random.random); random.done("post-flow sourceAIR still rolls30 and NO boolean"); }
        f.check(f.level.isEmptyBlock(f.base) && amount(f, f.base.below()) == 9 && mass(f) == 9,
                "sourceAIR tail does not erase or reduce legitimately displaced destination9");
        seal(f); put(f, f.base, goo, 9); put(f, f.base.below(), ThaumcraftFlux.FLUX_GAS.get(), 9);
        try (var random = new Script(f, i(30, 0), b(false))) { tick(f, f.base, random.random); random.done("post-swap sourceGAS still gets exact original goo-tail30/boolean"); }
        f.check(f.level.getBlockState(f.base).is(ThaumcraftFlux.FLUX_GAS.get()) && amount(f, f.base) == 8
                        && f.level.getBlockState(f.base.below()).is(goo) && amount(f, f.base.below()) == 9 && mass(f) == 17,
                "lawful swapped-in gas tail preserved, unlike unrelated callback replacement; one intentional decay sink");
    }

    private static void effectCallbacks(Fixture f) {
        seal(f); var gas = ThaumcraftFlux.FLUX_GAS.get(); put(f, f.base, gas, 9);
        var actor = new EffectStand(f.level);
        actor.setPos(Vec3.atCenterOf(f.base));
        try {
            actor.callback = () -> {
                f.check(amount(f, f.base) == 8, "actual effect callback sees ONE already-paid native unit");
                collide(f, actor); // Same-cell reentry must stop BEFORE another RNG/debit.
                f.put(f.base, Blocks.STONE);
            };
            try (var random = new Script(f, i(10, 0), b(false))) { collide(f, actor); random.done("effect callback nested exposure cannot double-debit or draw again"); }
            f.check(actor.calls == 1 && actor.hasEffect(MobEffects.CONFUSION) && f.level.getBlockState(f.base).is(Blocks.STONE),
                    "real LivingEntity.addEffect callback replaces source safely; outer handler never rewrites the replacement");
        } finally { actor.discard(); }
        seal(f); put(f, f.base, gas, 9);
        var throwing = new EffectStand(f.level); throwing.setPos(Vec3.atCenterOf(f.base));
        try {
            throwing.callback = () -> { throw new ProbeFailure(); };
            boolean rejected = false;
            try (var random = new Script(f, i(10, 0), b(false))) {
                try { collide(f, throwing); } catch (ProbeFailure expected) { rejected = true; }
                random.done("throwing actual effect callback occurs only AFTER one paid unit, not a simulated debit");
            }
            f.check(rejected && amount(f, f.base) == 8, "post-payment effect exception never refunds/replays native gas unit");
            f.player.removeAllEffects();
            try (var random = new Script(f, i(10, 0), b(false))) { collide(f, f.player); random.done("exception finally releases actual flow/exposure frame"); }
            f.check(amount(f, f.base) == 7 && f.player.hasEffect(MobEffects.CONFUSION), "next ordinary exposure consumes a DIFFERENT actual unit after failed callback");
        } finally { throwing.discard(); }
    }

    private static void callbackFences(Fixture f) {
        seal(f); var gas = ThaumcraftFlux.FLUX_GAS.get(); put(f, f.base, gas, 9);
        for (Direction side : SIDES) f.put(f.base.relative(side), Blocks.AIR);
        try (var random = new Script(f, i(1, 0))) {
            random.onDraw = () -> {
                tick(f, f.base, random.random); // Actual native reentry, not a mock transfer.
                f.put(f.base, Blocks.STONE);
            };
            tick(f, f.base, random.random); random.done("one allocator draw; reentry has0 and real replacement abandons uncommitted redistribution");
        }
        f.check(f.level.getBlockState(f.base).is(Blocks.STONE) && mass(f) == 0
                        && java.util.Arrays.stream(SIDES).allMatch(side -> f.level.isEmptyBlock(f.base.relative(side))),
                "callback replacement itself removes old9; retired flow cannot credit four recipients or mutate its STONE successor");
        for (FluxFiniteBlock fluid : fluids()) {
            seal(f); put(f, f.base, fluid, 9); BlockPos target = f.base.relative(fluid.verticalDirection()); f.put(target, Blocks.WATER);
            var probe = callbackProbe;
            f.check(probe != null && BuiltInRegistries.BLOCK.get(CALLBACK_PROBE_ID) == probe,
                    "registerTestProbes must register the test-only callback observer BEFORE registry freeze");
            probe.reset();
            BlockPos probePos = f.base.east();
            try {
                // Registered test-only state uses ordinary native Level.setBlock;
                // arm only AFTER installation, and remove before scheduled ticks.
                f.put(probePos, probe);
                f.check(f.level.getBlockState(probePos).is(probe), "registered callback observer installed through normal native block placement");
                probe.callback = () -> {
                    f.check(f.level.isEmptyBlock(f.base) && f.level.getBlockState(target).is(fluid) && amount(f, target) == 9 && mass(f) == 9,
                            "actual native neighbor callback observes BOTH committed balances, never simultaneous source9+destination9");
                    f.level.getBlockState(target).tick(f.level, target, f.level.random);
                    f.put(f.base, Blocks.STONE);
                };
                try (var random = new Script(f)) { tick(f, f.base, random.random); random.done("post-commit callback reentry0; unrelated replacement prevents stale goo-tail30 entirely"); }
                f.check(probe.calls == 1 && f.level.getBlockState(f.base).is(Blocks.STONE) && amount(f, target) == 9 && mass(f) == 9,
                        "native notified replacement retained; no duplicate transfer, phantom refund or unrelated block erased by old goo tail");
            } finally {
                probe.reset();
                f.put(probePos, Blocks.STONE);
            }
        }
        seal(f); var goo = ThaumcraftFlux.FLUX_GOO.get(); put(f, f.base, goo, 3); f.put(f.base.above(), Blocks.AIR);
        try (var random = new Script(f, i(25, 1))) {
            random.onDraw = () -> f.put(f.base, Blocks.STONE);
            tick(f, f.base, random.random); random.done("replacement during failed25 cannot continue into30 or erase unrelated current owner");
        }
        f.check(f.level.getBlockState(f.base).is(Blocks.STONE), "goo tail distinguishes external replacement from lawful AIR/gas flow result");
        var stale = goo.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 8);
        try (var random = new Script(f)) { stale.tick(f.level, f.base, random.random); random.done("stale scheduled-state invocation rejected before flow AND tail RNG"); }
        f.check(f.level.getBlockState(f.base).is(Blocks.STONE), "stale Goo tick cannot erase STONE even with a normally successful decay fixture");
    }

    private static void boundariesAndUnavailable(Fixture f) {
        for (FluxFiniteBlock fluid : fluids()) {
            BlockPos edge = new BlockPos(f.base.getX(), fluid.verticalDirection() == Direction.UP
                    ? f.level.getMaxBuildHeight() - 1 : f.level.getMinBuildHeight(), f.base.getZ());
            try (var shadow = new Cells(f, edge)) {
                put(f, edge, fluid, 9);
                try (var random = new Script(f, tail(fluid, i(30, 1)))) { tick(f, edge, random.random); random.done("world-boundary whole9 sink, lawful sourceAIR goo-tail30 only"); }
                f.check(f.level.isEmptyBlock(edge), "all9 leave native vertical build boundary; no clamp or compensating spill");
            }
        }
        var remote = new BlockPos(30_000_000, f.level.getMinBuildHeight() + 2, 30_000_000);
        f.check(!FluxFiniteBlock.loaded(f.level, remote), "remote input really starts unavailable");
        try (var random = new Script(f)) {
            for (FluxFiniteBlock fluid : fluids()) fluid.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 8).tick(f.level, remote, random.random);
            random.done("unavailable source guard performs NO reads, forced chunk acquisition or RNG/tail");
        }
        f.check(!FluxFiniteBlock.loaded(f.level, remote), "actual failed stale/unavailable ticks leave remote chunk unavailable; NOT a full unload/restart claim");
    }

    private static List<FluxFiniteBlock> fluids() { return List.of(ThaumcraftFlux.FLUX_GAS.get(), ThaumcraftFlux.FLUX_GOO.get()); }
    private static Draw[] tail(FluxFiniteBlock fluid, Draw draw) { return fluid == ThaumcraftFlux.FLUX_GOO.get() ? new Draw[] {draw} : new Draw[0]; }
    private static void seal(Fixture f) { f.reset(); f.player.removeAllEffects(); for (BlockPos pos : BlockPos.betweenClosed(f.base.offset(-2, -2, -2), f.base.offset(2, 2, 2))) f.put(pos.immutable(), Blocks.STONE); }
    private static void put(Fixture f, BlockPos pos, FluxFiniteBlock fluid, int amount) { f.check(FluxFiniteBlock.loaded(f.level, pos), "direct quantity INPUT cell is already loaded/in-height"); var state = fluid.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, amount - 1); f.level.setBlock(pos, fluid.withGeometry(state, f.level, pos), TubeVariantsRuntimeChecks.QUIET); }
    private static int amount(Fixture f, BlockPos pos) { return FluxFiniteBlock.quanta(f.level.getBlockState(pos)); }
    private static int mass(Fixture f) { int total = 0; for (BlockPos pos : BlockPos.betweenClosed(f.base.offset(-2, -2, -2), f.base.offset(2, 2, 2))) total += amount(f, pos); return total; }
    private static void tick(Fixture f, BlockPos pos, RandomSource random) { f.level.getBlockState(pos).tick(f.level, pos, random); }
    private static void collide(Fixture f, Entity entity) { f.level.getBlockState(f.base).entityInside(f.level, f.base, entity); }
    private static void preflight(GameTestHelper test) { var base = test.absolutePos(new BlockPos(3, 3, 3)); for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) test.assertTrue(FluxFiniteBlock.loaded(test.getLevel(), base.offset(x, 0, z)), "preflight BEFORE shared shadow reads: native footprint already loaded, never force chunks"); }

    private record Draw(int bound, int result, boolean booleanCall) {}
    private static Draw i(int bound, int result) { return new Draw(bound, result, false); }
    private static Draw b(boolean result) { return new Draw(0, result ? 1 : 0, true); }

    /** Explicit deterministic INPUT, not production/client seeding. Every call
     * kind/bound/result is asserted, exact old level RNG object restored finally. */
    private static final class Script implements AutoCloseable {
        final Fixture f; final Draw[] draws; final Field field; final RandomSource original, random;
        final List<String> actual = new ArrayList<>(); int index; Runnable onDraw;
        Script(Fixture f, Draw... draws) {
            this.f = f; this.draws = draws; original = f.level.random;
            random = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(), new Class<?>[] {RandomSource.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "toString" -> "M09b declared finite-flux RandomSource INPUT";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError(method);
                };
                boolean bool = method.getName().equals("nextBoolean");
                int bound = args != null && args.length == 1 && args[0] instanceof Integer value ? value : 0;
                actual.add(method.getName() + "(" + bound + ")");
                f.check(index < draws.length, "unexpected extra RNG call: " + actual);
                Draw expected = draws[index++];
                f.check((bool || method.getName().equals("nextInt") && args != null && args.length == 1)
                                && bool == expected.booleanCall && bound == expected.bound
                                && (bool || expected.result >= 0 && expected.result < bound),
                        "exact original RNG kind/bound at" + index + ": " + actual);
                if (onDraw != null) { var callback = onDraw; onDraw = null; callback.run(); }
                if (bool) return expected.result != 0;
                return expected.result;
            });
            try { field = Level.class.getDeclaredField("random"); field.setAccessible(true); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot locate test-only actual level RNG", failure); }
            try { field.set(f.level, random); f.check(f.level.random == random, "declared INPUT installed, never a seeded world source"); }
            catch (IllegalAccessException | RuntimeException | Error failure) {
                try { field.set(f.level, original); } catch (IllegalAccessException restore) { failure.addSuppressed(restore); }
                throw new IllegalStateException("Cannot install test-only RNG INPUT", failure);
            }
        }
        void done(String why) { f.check(index == draws.length, why + "; exact observed calls=" + actual); }
        @Override public void close() { try { field.set(f.level, original); f.check(f.level.random == original, "finally restores EXACT original level RNG identity, no seed/advance/rewind"); } catch (IllegalAccessException failure) { throw new IllegalStateException("Cannot restore actual level RNG", failure); } }
    }

    /** Registered ONLY by the isolated GameTest mod. Synchronous observer is
     * reset/removed BEFORE real chunk serialization/timer fixtures; no BlockItem. */
    private static final class NeighborProbe extends Block {
        Runnable callback; int calls;
        NeighborProbe() {
            super(BlockBehaviour.Properties.of().strength(-1).noLootTable());
            getStateDefinition().getPossibleStates().forEach(BlockState::initCache);
        }
        void reset() { callback = null; calls = 0; }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
            if (callback == null) return;
            var action = callback; callback = null; calls++; action.run();
        }
    }
    private static final class EffectStand extends ArmorStand {
        Runnable callback; int calls;
        EffectStand(Level level) { super(level, 0, 0, 0); }
        @Override public boolean addEffect(MobEffectInstance effect, Entity source) {
            if (callback != null) { var action = callback; callback = null; calls++; action.run(); }
            return super.addEffect(effect, source);
        }
    }
    private static final class TaintedStand extends ArmorStand implements ITaintedMob { TaintedStand(Level level) { super(level, 0, 0, 0); } }
    private static final class ProbeFailure extends RuntimeException {}

    private static final class Cells implements AutoCloseable {
        final Fixture f; final Map<BlockPos, BlockState> states = new LinkedHashMap<>(); final Map<BlockPos, BlockEntity> tiles = new LinkedHashMap<>();
        Cells(Fixture f, BlockPos... positions) { this.f = f; for (BlockPos pos : positions) { f.check(FluxFiniteBlock.loaded(f.level, pos), "extra owned boundary cell already loaded"); states.put(pos, f.level.getBlockState(pos)); var tile = f.level.getBlockEntity(pos); if (tile != null) tiles.put(pos, tile); } }
        @Override public void close() { states.keySet().forEach(f.level::removeBlockEntity); states.forEach((pos, state) -> f.level.setBlock(pos, state, TubeVariantsRuntimeChecks.QUIET)); tiles.values().forEach(tile -> { tile.clearRemoved(); f.level.setBlockEntity(tile); }); }
    }
}
