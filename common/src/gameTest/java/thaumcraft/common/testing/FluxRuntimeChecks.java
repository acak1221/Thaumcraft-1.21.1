package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.potions.FluxEffects;

/** One native, loader-neutral fixture. No stand-in fluids, injected effects, or mock block states. */
public final class FluxRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private FluxRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> blockEntities = new LinkedHashMap<>();
        for (BlockPos mutable : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            BlockPos saved = mutable.immutable();
            states.put(saved, level.getBlockState(saved));
            if (level.getBlockEntity(saved) != null) blockEntities.put(saved, level.getBlockEntity(saved).saveWithFullMetadata(level.registryAccess()));
        }
        var effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) inventory.add(player.getInventory().getItem(slot).copy());
        var position = player.position();
        var motion = player.getDeltaMovement();
        var mode = player.gameMode.getGameModeForPlayer();
        int selected = player.getInventory().selected;
        // Reseeding is confined to this disposable GameTest world, as in OreRuntimeChecks.
        long resumeSeed = level.random.nextLong();
        try {
            player.removeAllEffects();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(pos));
            spillChecks(test, level, pos);
            finiteChecks(test, level, pos);
            decayChecks(test, level, pos);
            exposureChecks(test, level, pos, player);
        } finally {
            for (var entry : states.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), QUIET);
            for (var entry : blockEntities.entrySet()) if (level.getBlockEntity(entry.getKey()) != null) level.getBlockEntity(entry.getKey()).loadWithComponents(entry.getValue(), level.registryAccess());
            player.removeAllEffects();
            effects.forEach(player::addEffect);
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); ++slot) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.setGameMode(mode);
            player.setPos(position);
            player.setDeltaMovement(motion);
            level.random.setSeed(resumeSeed);
        }
        test.succeed();
    }

    private static void spillChecks(GameTestHelper test, ServerLevel level, BlockPos pos) {
        seal(level, pos);
        air(level, pos.above());
        long miss = seed(random -> random.nextInt(4) != 0);
        RandomSource expected = RandomSource.create(miss);
        expected.nextInt(4);
        level.random.setSeed(miss);
        ThaumcraftFlux.spill(level, pos);
        test.assertTrue(level.getBlockState(pos.above()).isAir() && level.random.nextLong() == expected.nextLong(), "spill miss consumes only its 1-in-4 roll");
        for (boolean gas : new boolean[] {true, false}) {
            long hit = seed(random -> random.nextInt(4) == 0 && random.nextBoolean() == gas);
            expected = RandomSource.create(hit);
            expected.nextInt(4);
            expected.nextBoolean();
            air(level, pos.above());
            level.random.setSeed(hit);
            ThaumcraftFlux.spill(level, pos);
            FluxFiniteBlock fluid = gas ? ThaumcraftFlux.FLUX_GAS.get() : ThaumcraftFlux.FLUX_GOO.get();
            test.assertTrue(level.getBlockState(pos.above()).is(fluid) && amount(level, pos.above()) == 1
                    && level.random.nextLong() == expected.nextLong(), "air spill places the real chosen fluid, level 0, in original RNG order");
            put(level, pos.above(), fluid, 7);
            expected = RandomSource.create(hit);
            expected.nextInt(4);
            level.random.setSeed(hit);
            ThaumcraftFlux.spill(level, pos);
            test.assertTrue(amount(level, pos.above()) == 8 && level.random.nextLong() == expected.nextLong(), "density merge reaches level 7 without a boolean or scatter roll");
        }
        long scatter = seed(random -> {
            if (random.nextInt(4) != 0) return false;
            int x = random.nextInt(3) - 1, y = random.nextInt(3) - 1, z = random.nextInt(3) - 1;
            return x != 0 || z != 0 || y == -1;
        });
        seal(level, pos);
        put(level, pos.above(), ThaumcraftFlux.FLUX_GOO.get(), 8);
        expected = RandomSource.create(scatter);
        expected.nextInt(4);
        BlockPos target = pos.offset(expected.nextInt(3) - 1, expected.nextInt(3) - 1, expected.nextInt(3) - 1);
        air(level, target);
        boolean gas = expected.nextBoolean();
        level.random.setSeed(scatter);
        ThaumcraftFlux.spill(level, pos);
        test.assertTrue(amount(level, pos.above()) == 8 && level.getBlockState(target).is(gas ? ThaumcraftFlux.FLUX_GAS.get() : ThaumcraftFlux.FLUX_GOO.get())
                && amount(level, target) == 1 && level.random.nextLong() == expected.nextLong(), "full fluid scatters x/y/z before the air-only fluid choice, without overflowing level 7");
        BlockPos unloaded = new BlockPos(30_000_000, level.getMinBuildHeight() + 2, 30_000_000);
        test.assertTrue(!FluxFiniteBlock.loaded(level, unloaded), "remote chunk starts unloaded");
        level.random.setSeed(seed(random -> random.nextInt(4) == 0));
        ThaumcraftFlux.spill(level, unloaded);
        test.assertTrue(!FluxFiniteBlock.loaded(level, unloaded), "spill never acquires or generates an unloaded chunk");
    }

    private static void finiteChecks(GameTestHelper test, ServerLevel level, BlockPos pos) {
        var gas = ThaumcraftFlux.FLUX_GAS.get();
        var goo = ThaumcraftFlux.FLUX_GOO.get();
        test.assertTrue(gas.density() == -4 && gas.tickRate() == 12 && goo.density() == 8 && goo.tickRate() == 30, "original density, viscosity-derived tick rates and eight quanta");
        for (int metadata = 0; metadata < 8; ++metadata) {
            BlockState state = gas.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, metadata);
            test.assertTrue(FluxFiniteBlock.quanta(state) == metadata + 1 && state.getLightEmission() == metadata * 7 / 8, "level encodes amount minus one and original scaled light");
        }
        for (FluxFiniteBlock fluid : new FluxFiniteBlock[] {gas, goo}) {
            seal(level, pos);
            BlockPos destination = pos.relative(fluid.verticalDirection());
            put(level, pos, fluid, 4);
            put(level, destination, fluid, 6);
            tick(level, pos, RandomSource.create(seed(random -> random.nextInt(30) != 0)));
            test.assertTrue(amount(level, destination) == 8 && amount(level, pos) == 2 && mass(level, pos) == 10, "vertical-first finite merge preserves all ten units and leaves capacity overflow at the origin");
            seal(level, pos);
            put(level, pos, fluid, 3);
            air(level, destination);
            RandomSource actual = RandomSource.create(1), expected = RandomSource.create(1);
            tick(level, pos, actual);
            if (fluid == goo) expected.nextInt(30);
            test.assertTrue(level.getBlockState(pos).isAir() && level.getBlockState(destination).is(fluid) && amount(level, destination) == 3, "gas rises and goo falls without an infinite source");
            test.assertTrue(actual.nextLong() == expected.nextLong(), "goo preserves the original post-flow tail RNG even after its origin becomes air");
        }
        seal(level, pos);
        put(level, pos, gas, 3);
        put(level, pos.above(), goo, 5);
        tick(level, pos, RandomSource.create(1));
        test.assertTrue(level.getBlockState(pos).is(goo) && amount(level, pos) == 5 && level.getBlockState(pos.above()).is(gas)
                && amount(level, pos.above()) == 3 && mass(level, pos) == 8, "density swap preserves both fluids rather than destroying the other phase");
        seal(level, pos);
        put(level, pos, gas, 8);
        for (Direction direction : Direction.Plane.HORIZONTAL) air(level, pos.relative(direction));
        put(level, pos.west(), gas, 2);
        tick(level, pos, RandomSource.create(1));
        test.assertTrue(mass(level, pos) == 10 && amount(level, pos) == 2, "horizontal sharing conserves a finite pool of ten units");
        for (Direction direction : Direction.Plane.HORIZONTAL) test.assertTrue(amount(level, pos.relative(direction)) == 2, "each eligible cardinal neighbor receives its share");
        Direction[] order = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (long seed = 0; seed < 8; ++seed) {
            seal(level, pos);
            put(level, pos, gas, 8);
            for (Direction direction : order) air(level, pos.relative(direction));
            RandomSource expected = RandomSource.create(seed), actual = RandomSource.create(seed);
            int count = 5, remainder = 3;
            int[] amounts = new int[4];
            for (int i = 0; i < 4; ++i) {
                amounts[i] = 1;
                if (remainder == count || remainder > 1 && expected.nextInt(count - remainder) != 0) { ++amounts[i]; --remainder; }
                --count;
            }
            tick(level, pos, actual);
            test.assertTrue(mass(level, pos) == 8 && amount(level, pos) == 1 + (remainder > 0 ? 1 : 0)
                    && actual.nextLong() == expected.nextLong(), "finite remainder preserves mass and original conditional RNG consumption");
            for (int i = 0; i < 4; ++i) test.assertTrue(amount(level, pos.relative(order[i])) == amounts[i], "remainder allocation follows original N/S/W/E priority");
        }
        seal(level, pos);
        put(level, pos, goo, 8);
        put(level, pos.north(), gas, 3);
        tick(level, pos, RandomSource.create(seed(random -> random.nextInt(30) != 0)));
        test.assertTrue(amount(level, pos) == 8 && level.getBlockState(pos.north()).is(gas) && amount(level, pos.north()) == 3,
                "MaterialTaint blocksMovement=true prevents horizontal displacement of the other flux phase");
        seal(level, pos);
        put(level, pos, gas, 2);
        put(level, pos.north(), gas, 1);
        tick(level, pos, RandomSource.create(1));
        test.assertTrue(amount(level, pos) == 2 && amount(level, pos.north()) == 1, "Forge's strict less-than amount-1 threshold does not flatten a one-unit difference");
        test.assertTrue(Blocks.SUGAR_CANE.defaultBlockState().is(FluxFiniteBlock.DISPLACEMENT_BLOCKED)
                && Blocks.OAK_DOOR.defaultBlockState().is(FluxFiniteBlock.DISPLACEMENT_BLOCKED)
                && Blocks.OAK_SIGN.defaultBlockState().is(FluxFiniteBlock.DISPLACEMENT_BLOCKED), "native loaded tags explicitly preserve reeds, doors and signs");
    }

    private static void decayChecks(GameTestHelper test, ServerLevel level, BlockPos pos) {
        var goo = ThaumcraftFlux.FLUX_GOO.get();
        seal(level, pos);
        put(level, pos, goo, 2);
        air(level, pos.above());
        tick(level, pos, RandomSource.create(seed(random -> random.nextInt(30) == 0 && random.nextBoolean())));
        test.assertTrue(level.getBlockState(pos).is(goo) && amount(level, pos) == 1 && level.getBlockState(pos.above()).is(ThaumcraftFlux.FLUX_GAS.get())
                && amount(level, pos.above()) == 1 && mass(level, pos) == 2, "low goo density decays into one real gas quantum, preserving coupled conversion mass");
        tick(level, pos, RandomSource.create(seed(random -> random.nextInt(30) == 0)));
        test.assertTrue(level.getBlockState(pos).isAir() && amount(level, pos.above()) == 1, "last goo quantum dissipates on the original one-in-thirty branch");
        for (int amount : new int[] {4, 8}) {
            seal(level, pos);
            put(level, pos, goo, amount);
            air(level, pos.above());
            tick(level, pos, RandomSource.create(seed(random -> random.nextInt(25) == 0)));
            test.assertTrue(level.getBlockState(pos).is(goo) && amount(level, pos) == amount, "pending real Thaumic Slime branch does not silently consume goo (M11 not complete)");
        }
        tick(level, pos, RandomSource.create(seed(random -> random.nextInt(25) != 0 && random.nextInt(50) == 0)));
        test.assertTrue(level.getBlockState(pos).is(goo) && amount(level, pos) == 8, "pending taint biome/fibres conversion leaves its resource intact");
    }

    private static void exposureChecks(GameTestHelper test, ServerLevel level, BlockPos pos, ServerPlayer player) {
        var gas = ThaumcraftFlux.FLUX_GAS.get();
        var goo = ThaumcraftFlux.FLUX_GOO.get();
        seal(level, pos);
        put(level, pos, gas, 8);
        long exhaust = seed(random -> random.nextInt(10) == 0 && random.nextBoolean());
        level.random.setSeed(seed(random -> random.nextInt(10) != 0));
        collide(level, pos, player);
        test.assertTrue(!player.hasEffect(ThaumcraftFlux.visExhaust()) && amount(level, pos) == 8, "failed gas exposure neither applies a generic poison nor spends density");
        level.random.setSeed(exhaust);
        collide(level, pos, player);
        var effect = player.getEffect(ThaumcraftFlux.visExhaust());
        test.assertTrue(effect != null && effect.getDuration() == 1200 && effect.getAmplifier() == 2 && effect.isAmbient()
                && FluxEffects.isUncurable(effect) && amount(level, pos) == 7 && close(ThaumcraftFlux.visPenalty(player), .3F), "gas exposes for 1200 ticks, md/3 amplifier, ambient/uncurable and spends exactly one quantum");
        level.random.setSeed(exhaust);
        collide(level, pos, player);
        test.assertTrue(amount(level, pos) == 7, "already-exhausted targets do not consume another gas quantum");
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        ItemStack bucket = Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), level, player);
        test.assertTrue(bucket.is(Items.BUCKET) && player.hasEffect(ThaumcraftFlux.visExhaust()) && !player.hasEffect(MobEffects.POISON), "actual native milk leaves the protected instance but cures ordinary effects and returns its bucket");
        player.removeAllEffects();
        test.assertTrue(!player.hasEffect(ThaumcraftFlux.visExhaust()) && ThaumcraftFlux.visPenalty(player) == 0, "admin removeAllEffects still removes protected exhaustion");
        var loaded = MobEffectInstance.load((CompoundTag) effect.save());
        test.assertTrue(loaded != null && loaded.getEffect() == ThaumcraftFlux.visExhaust() && !FluxEffects.isUncurable(loaded), "effect NBT round-trips with the actual registered holder and original custom-curative reset");
        player.addEffect(loaded);
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), level, player);
        test.assertTrue(!player.hasEffect(ThaumcraftFlux.visExhaust()), "both loaders preserve the original milk-curable-after-reload quirk");
        put(level, pos, gas, 5);
        level.random.setSeed(seed(random -> random.nextInt(10) == 0 && !random.nextBoolean()));
        collide(level, pos, player);
        // Original Gas80/86 field_76431_k maps to confusion, not Wither.
        effect = player.getEffect(MobEffects.CONFUSION);
        test.assertTrue(effect != null && effect.getDuration() == 160 && effect.getAmplifier() == 0 && !effect.isAmbient() && amount(level, pos) == 4, "gas's other boolean branch applies original 80+md*20 nausea and spends one quantum");
        level.random.setSeed(exhaust);
        collide(level, pos, player);
        test.assertTrue(amount(level, pos) == 4, "already-nauseated targets do not consume gas");
        player.removeAllEffects();
        put(level, pos, gas, 1);
        level.random.setSeed(exhaust);
        collide(level, pos, player);
        test.assertTrue(level.getBlockState(pos).isAir(), "successful exposure consumes and removes the final gas quantum");
        player.removeAllEffects();
        put(level, pos, gas, 8);
        var undead = EntityType.ZOMBIE.create(level);
        var tainted = new TaintedArmorStand(level);
        if (undead == null) throw new IllegalStateException("Native zombie fixture could not be created");
        level.random.setSeed(exhaust);
        collide(level, pos, undead);
        level.random.setSeed(exhaust);
        collide(level, pos, tainted);
        test.assertTrue(amount(level, pos) == 8 && !undead.hasEffect(ThaumcraftFlux.visExhaust()) && !tainted.hasEffect(ThaumcraftFlux.visExhaust()), "gas excludes actual undead and the TC4 tainted marker independently");
        put(level, pos, goo, 4);
        player.setDeltaMovement(.8, .2, -.4);
        collide(level, pos, player);
        collide(level, pos, undead);
        effect = player.getEffect(ThaumcraftFlux.visExhaust());
        test.assertTrue(effect != null && effect.getDuration() == 600 && effect.getAmplifier() == 1 && FluxEffects.isUncurable(effect)
                && player.getDeltaMovement().equals(new Vec3(.4, .2, -.2)) && amount(level, pos) == 4
                && undead.hasEffect(ThaumcraftFlux.visExhaust()), "goo halves X/Z at four quanta, leaves Y/mass alone, and exhausts even undead for 600 ticks");
        player.addEffect(new MobEffectInstance(ThaumcraftFlux.infectiousVisExhaust(), 200, 2));
        test.assertTrue(close(ThaumcraftFlux.visPenalty(player), .3F) && !ThaumcraftFlux.visExhaust().value().shouldApplyEffectTickThisTick(40, 2), "pricing uses max amplifier, not stacked penalties; normal exhaustion is passive");
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 100, 0));
        collide(level, pos, player);
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), level, player);
        test.assertTrue(!player.hasEffect(ThaumcraftFlux.visExhaust()), "original combine retains pre-existing curatives; the whole effect type is not made uncurable");
        undead.discard();
        tainted.discard();
    }

    private static boolean close(float actual, float expected) { return Math.abs(actual - expected) < .00001F; }
    private static long seed(Predicate<RandomSource> condition) {
        for (long seed = 0; seed < 100_000; ++seed) if (condition.test(RandomSource.create(seed))) return seed;
        throw new IllegalStateException("No native random seed satisfies the flux fixture");
    }
    private static void seal(ServerLevel level, BlockPos pos) { for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) level.setBlock(cell, Blocks.STONE.defaultBlockState(), QUIET); }
    private static void air(ServerLevel level, BlockPos pos) { level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET); }
    private static void put(ServerLevel level, BlockPos pos, FluxFiniteBlock fluid, int amount) { level.setBlock(pos, fluid.withGeometry(fluid.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, amount - 1), level, pos), QUIET); }
    private static int amount(ServerLevel level, BlockPos pos) { return FluxFiniteBlock.quanta(level.getBlockState(pos)); }
    private static int mass(ServerLevel level, BlockPos pos) { int total = 0; for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) total += amount(level, cell); return total; }
    private static void tick(ServerLevel level, BlockPos pos, RandomSource random) { level.getBlockState(pos).tick(level, pos, random); }
    private static void collide(ServerLevel level, BlockPos pos, net.minecraft.world.entity.Entity entity) { level.getBlockState(pos).entityInside(level, pos, entity); }
    /** Test-only native entity subclass, never registered or spawned as a new gameplay mob. */
    private static final class TaintedArmorStand extends ArmorStand implements ITaintedMob { private TaintedArmorStand(ServerLevel level) { super(level, 0, 0, 0); } }
}
