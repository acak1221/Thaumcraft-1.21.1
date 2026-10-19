package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.potions.FluxEffects;

/** Original BlockFluxGoo:69-89,103-135. Density 8; viscosity 6000 / 200 = 30 ticks. */
public final class BlockFluxGoo extends FluxFiniteBlock {
    public static final MapCodec<BlockFluxGoo> CODEC = simpleCodec(BlockFluxGoo::new);

    public BlockFluxGoo(Properties properties) { super(properties, 8, 30); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!state.is(this) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !sameStock(state, level.getBlockState(pos))) return;
        // Prediction remains client-local; neither side clamps q9's -.125 factor.
        if (level.isClientSide) { slow(state, entity); return; }
        try (Step step = begin(level, pos, state)) {
            if (step == null) return;
            slow(state, entity);
            if (step.coherent() && entity instanceof LivingEntity living) {
                living.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 600, state.getValue(LEVEL) / 3, true, true)));
            }
        }
        // M11 pending: only the real EntityThaumicSlime may grow by consuming goo.
        // No vanilla Slime substitution and no pretend tainted-mob exemptions here.
    }

    private static void slow(BlockState state, Entity entity) {
        float multiplier = 1 - quantaPercentage(state);
        Vec3 velocity = entity.getDeltaMovement();
        entity.setDeltaMovement(velocity.x * multiplier, velocity.y, velocity.z * multiplier);
    }

    @Override protected void afterFlow(Step step, RandomSource random) {
        BlockPos pos = step.pos;
        BlockState current = step.observe(pos);
        if (!step.coherent() || current == null || !(current.isAir() || current.getBlock() instanceof FluxFiniteBlock)) return;
        // Original:105 re-reads metadata AFTER finite flow, even at a now-air
        // source or at gas left by a density swap. Preserve that tail/RNG quirk.
        int metadata = current.getBlock() instanceof FluxFiniteBlock ? current.getValue(LEVEL) : 0;
        BlockPos above = pos.above();
        BlockState overhead = step.observe(above);
        boolean airAbove = overhead != null && overhead.isAir();
        // Original outer condition includes the roll: a failed lower-density25
        // falls THROUGH to30, unlike the formerly nested native condition.
        if (metadata >= 2 && metadata < 6 && airAbove) {
            int spawn = random.nextInt(25);
            if (!step.coherent() || spawn == 0) return; // M11 size1/gore deferred; no fake debit.
        } else if (metadata >= 6 && airAbove) {
            int spawn = random.nextInt(25);
            if (!step.coherent() || spawn == 0) return; // M11 real size2 deferred.
            random.nextInt(50); // Default taintFromFlux=true; real biome/fibres still M11.
            return; // Both high-density outcomes retain stock until those systems exist.
        }
        int decay = random.nextInt(30);
        if (!step.coherent() || decay != 0) return;
        if (metadata == 0) {
            step.commit(step.change(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        } else {
            if (!step.commit(step.change(pos, current.setValue(LEVEL, metadata - 1), Block.UPDATE_ALL))) return;
            // Preserve short-circuit RNG order: boolean before air test.
            boolean makeGas = random.nextBoolean();
            if (!step.coherent()) return;
            if (makeGas && airAbove) {
                BlockFluxGas gas = ThaumcraftFlux.FLUX_GAS.get();
                if (step.observe(above).isAir()) step.commit(step.change(above, gas.defaultBlockState(), Block.UPDATE_ALL));
            }
        }
    }
}
