package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.lib.potions.FluxEffects;
import thaumcraft.common.lib.world.BiomeColumns;

/**
 * Original BlockFluxGoo: the tainted goo of the Outer Lands.
 * <p>
 * The goo is a finite fluid that grows every real thaumic slime that walks into it, that breeds
 * a new slime when it is thick enough and that turns into the taint when the world is old enough.
 * </p>
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockFluxGoo.java}, lines 27-147
 * ({@code onEntityCollidedWithBlock} lines 52-74, {@code updateTick} lines 88-122).
 * </p>
 */
public final class BlockFluxGoo extends FluxFiniteBlock {
    public static final MapCodec<BlockFluxGoo> CODEC = simpleCodec(BlockFluxGoo::new);
    /** Original BlockFluxGoo:97,120 - the gore sound of a born slime. */
    private static final SoundEvent GORE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "gore"));
    /** Original BlockFluxGoo:91 - a cell of the metadata two up to five breeds a size one slime. */
    public static final int SLIME_METADATA_MIN = 2;
    /** Original BlockFluxGoo:91 - the metadata five is the last one that breeds a size one slime. */
    public static final int SLIME_METADATA_MAX = 6;
    /** Original BlockFluxGoo:98 - the metadata six and above breed a size two slime or turn to taint. */
    public static final int TAINT_METADATA = 6;
    /** Original BlockFluxGoo:91,99 - the one in twenty five roll of a breeding cell. */
    public static final int BREED_ROLL = 25;
    /** Original BlockFluxGoo:107 - the one in fifty roll that turns a thick cell into the taint. */
    public static final int TAINT_ROLL = 50;
    /** Original BlockFluxGoo:111 - the one in thirty roll of the ordinary decay of a cell. */
    public static final int DECAY_ROLL = 30;
    /** Original BlockFluxGoo:96,103 - the size of a slime that is born in a breeding cell. */
    public static final int SLIME_SIZE_THIN = 1;
    /** Original BlockFluxGoo:103 - the size of a slime that is born in a thick cell. */
    public static final int SLIME_SIZE_THICK = 2;

    public BlockFluxGoo(Properties properties) { super(properties, 8, 30); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!state.is(this) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !sameStock(state, level.getBlockState(pos))) return;
        // Original BlockFluxGoo:54-63: a real thaumic slime grows by eating the goo and
        // never feels the slowdown nor the exhaustion of the other creatures.
        if (entity instanceof EntityThaumicSlime slime) {
            if (level.isClientSide) return;
            try (Step step = begin(level, pos, state)) {
                if (step == null) return;
                int metadata = step.observe(pos).getValue(LEVEL);
                if (slime.getSlimeSize() >= metadata || !level.random.nextBoolean()) return;
                if (!step.coherent()) return;
                slime.setSlimeSize(slime.getSlimeSize() + 1);
                BlockState next = metadata > 1 ? step.observe(pos).setValue(LEVEL, metadata - 1)
                        : Blocks.AIR.defaultBlockState();
                step.commit(step.change(pos, next, Block.UPDATE_ALL));
            }
            return;
        }
        // Prediction remains client-local; neither side clamps q9's -.125 factor.
        if (level.isClientSide) { slow(state, entity); return; }
        try (Step step = begin(level, pos, state)) {
            if (step == null) return;
            slow(state, entity);
            if (step.coherent() && entity instanceof LivingEntity living) {
                living.addEffect(FluxEffects.uncurable(new MobEffectInstance(ThaumcraftFlux.visExhaust(), 600, state.getValue(LEVEL) / 3, true, true)));
            }
        }
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
        // Original BlockFluxGoo:106-135 is ONE if/else-if chain. A failed
        // lower-density25 falls THROUGH to the30 decay, and a thick cell never
        // reaches the30 at all.
        if (metadata >= SLIME_METADATA_MIN && metadata < SLIME_METADATA_MAX && airAbove) {
            int spawn = random.nextInt(BREED_ROLL);
            if (spawn == 0) {
                // Original BlockFluxGoo:91-97: the cell is emptied and a size one slime is born.
                if (!step.commit(step.change(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))) return;
                spawnSlime(step, SLIME_SIZE_THIN);
                return;
            }
            // A failed roll leaves the first branch of the original chain and
            // reaches its 30 decay; the cell keeps every unit but one. An
            // external replacement of the cell ends the tail of the port, as
            // every other owned write of the flux does.
            if (!step.coherent()) return;
        } else if (metadata >= TAINT_METADATA && airAbove) {
            int spawn = random.nextInt(BREED_ROLL);
            if (spawn == 0) {
                // Original BlockFluxGoo:98-105: a size two slime is born instead.
                if (!step.commit(step.change(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))) return;
                spawnSlime(step, SLIME_SIZE_THICK);
                return;
            }
            // Original BlockFluxGoo:106-110: the taint takes the cell and its column.
            if (random.nextInt(TAINT_ROLL) == 0) {
                if (!step.coherent()) return;
                BiomeColumns.set(step.level, pos.getX(), pos.getZ(), ThaumcraftBiomes.TAINT);
                if (!step.coherent()) return;
                BlockState fibres = ThaumcraftTaint.FIBRES.get().defaultBlockState();
                if (!step.commit(step.change(pos, fibres, Block.UPDATE_ALL))) return;
                step.level.blockEvent(pos, ThaumcraftTaint.FIBRES.get(), 1, 0);
            }
            // A thick cell of the original never reaches its 30 decay.
            return;
        }
        int decay = random.nextInt(DECAY_ROLL);
        if (!step.coherent() || decay != 0) return;
        if (metadata == 0) {
            step.commit(step.change(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
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

    /**
     * Original BlockFluxGoo:94-97,102-105 and the 1.12 reference
     * {@code src/main/java/thaumcraft/common/blocks/BlockFluxGoo.java:118-124}: the slime is
     * born in the middle of the emptied cell with the given size and announces itself.
     */
    private static void spawnSlime(Step step, int size) {
        ServerLevel level = step.level;
        BlockPos pos = step.pos;
        EntityThaumicSlime slime = new EntityThaumicSlime(ThaumcraftEldritchMobs.THAUMIC_SLIME.get(), level);
        slime.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        slime.setSlimeSize(size);
        level.addFreshEntity(slime);
        level.playSound(null, slime, GORE, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
