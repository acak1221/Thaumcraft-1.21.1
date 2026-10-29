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
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
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
import thaumic.tinkerer.common.core.helper.SimpleBlockFx;
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
    /** The duration of the speed and jump effects the paving gives (1.7.10: 40 ticks). */
    public static final int EFFECT_TICKS = 40;
    /** The effect is given again when no more than this many ticks are left. */
    private static final int RENEW_BELOW = 20;

    public BlockTravelSlab(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Hardness 0.8, resistance 10 in the 1.7.10 setter (6 now). */
    public static BlockBehaviour.Properties defaultProperties() {
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
     * ground, so the sparkles (five per call in 1.7.10, Thaumcraft's block sparkle) are thinned out to one call in eight,
     * and the effects are renewed only when they are missing or about to run out: adding the same effect on every tick would
     * re-apply its attribute modifiers and, for a player, send an update packet and fire an advancement trigger every tick.</p>
     */
    public static void walkEffects(Level level, BlockPos pos, BlockState state, net.minecraft.world.level.block.Block block,
            Entity entity) {
        if (!state.is(block)) return;
        if (entity instanceof LivingEntity living) {
            if (level.isClientSide) {
                if (level.random.nextInt(8) == 0) {
                    // blockSparkle(world, x, y, z, '?', 5): the dark blue of '?', five counts
                    SimpleBlockFx.blockSparkleClient(level, pos, SimpleBlockFx.SPARKLE_BLUE, 5);
                }
            } else {
                renew(living, MobEffects.MOVEMENT_SPEED, 1);
                renew(living, MobEffects.JUMP, 0);
            }
        }
    }

    /** The two seconds of an effect are given again once less than one second is left (or the effect is missing or weaker). */
    private static void renew(LivingEntity living, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = living.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier
                || !current.isInfiniteDuration() && current.getDuration() <= RENEW_BELOW) {
            living.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier));
        }
    }
}
