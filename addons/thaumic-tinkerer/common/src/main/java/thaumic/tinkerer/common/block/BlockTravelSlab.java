/**
 * Created by Katrina on 13/04/2015.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;

/**
 * Paving Slab of Travel: whoever walks on it is quickened and made to jump higher for two seconds.
 *
 * <p>The 1.7.10 double slab block ({@code travel_slab_full}) is the vanilla double state of this slab.</p>
 */
public class BlockTravelSlab extends SlabBlock implements ITTinkererRegisterable {
    /** The data recipe of the slab (1.7.10 name {@code PAVETRAVEL}). */
    public static final String RECIPE = "pave_travel_slab";

    public BlockTravelSlab(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Hardness 0.8, resistance 10 in the 1.7.10 setter (6 now). */
    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererCraftingBenchRecipe(RECIPE);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        walkEffects(level, pos, state, this, entity);
        super.stepOn(level, pos, state, entity);
    }

    /**
     * 1.7.10 {@code onEntityWalking} of the travel slab and stairs and of the warded slab: sparkles on the client, speed
     * II (level 1) and jump boost I (level 0) for forty ticks on a living entity.
     *
     * <p>1.7.10 called it once per step of the walker, 1.21.1 calls {@code stepOn} on every tick the entity is on the
     * ground, so the sparkles (five per call in 1.7.10) are thinned out to one call in eight.</p>
     */
    public static void walkEffects(Level level, BlockPos pos, BlockState state, net.minecraft.world.level.block.Block block,
            Entity entity) {
        if (!state.is(block)) return;
        if (entity instanceof LivingEntity living) {
            if (level.isClientSide) {
                if (level.random.nextInt(8) == 0) {
                    for (int i = 0; i < 5; i++) {
                        level.addParticle(ParticleTypes.END_ROD, pos.getX() + level.random.nextFloat(),
                                pos.getY() + 1.0, pos.getZ() + level.random.nextFloat(), 0.0, 0.02, 0.0);
                    }
                }
            } else {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
                living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0));
            }
        }
    }
}
