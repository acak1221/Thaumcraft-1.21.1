package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.lib.potions.FluxEffects;

/** Original BlockFluxGas:45-49,78-93. Density -4; viscosity 2500 / 200 = 12 ticks. */
public final class BlockFluxGas extends FluxFiniteBlock {
    public static final MapCodec<BlockFluxGas> CODEC = simpleCodec(BlockFluxGas::new);

    public BlockFluxGas(Properties properties) { super(properties, -4, 12); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel) || !state.is(this)) return;
        try (Step step = begin(level, pos, state)) {
            if (step == null) return;
            // O Gas80/86 field_76431_k = confusion (NOT field_82731_v/wither).
            // Chance remains BEFORE all original eligibility tests.
            if (level.random.nextInt(10) != 0 || !step.coherent() || !(entity instanceof LivingEntity living)
                    || entity instanceof ITaintedMob || living.getType().is(EntityTypeTags.UNDEAD)
                    || living.hasEffect(ThaumcraftFlux.visExhaust()) || living.hasEffect(MobEffects.CONFUSION)) return;
            int metadata = step.observe(pos).getValue(LEVEL);
            boolean exhaustion = level.random.nextBoolean();
            if (!step.coherent()) return;
            BlockState next = metadata == 0 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                    : step.observe(pos).setValue(LEVEL, metadata - 1);
            // Commit the ONE shared native unit before callback-bearing effects.
            // Reentry cannot expose/debit the same unit twice; later replacement
            // never authorizes a read/modify/write on that new block.
            if (!step.commit(step.change(pos, next, Block.UPDATE_ALL))) return;
            if (exhaustion) living.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 1200, metadata / 3, true, true)));
            else living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80 + metadata * 20, 0, false, true));
        }
    }
}
