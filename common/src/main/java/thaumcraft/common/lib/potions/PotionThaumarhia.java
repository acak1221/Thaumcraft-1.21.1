package thaumcraft.common.lib.potions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.config.ThaumcraftFlux;

/** Original PotionThaumarhia: once per second, one chance in fifteen to leave ordinary flux goo in the empty feet cell. */
public final class PotionThaumarhia extends MobEffect {
    public PotionThaumarhia() { super(MobEffectCategory.HARMFUL, 0x664477); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }
    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (!target.level().isClientSide && target.level().random.nextInt(15) == 0) {
            BlockPos pos = target.blockPosition();
            if (target.level().hasChunkAt(pos) && target.level().getBlockState(pos).isAir()) target.level().setBlock(pos, ThaumcraftFlux.FLUX_GOO.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }
}
