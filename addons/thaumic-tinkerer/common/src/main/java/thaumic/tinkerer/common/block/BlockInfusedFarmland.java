/**
 * Created by pixlepix on 8/4/14.
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

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumic.tinkerer.common.block.tile.TileInfusedFarmland;

/**
 * The soil of the infused crops: farmland that never dries, is never trampled, always looks wet, is a full block
 * (1.7.10 reset the bounds to the full cube after the constructor of farmland had shrunk them) and keeps the aspects
 * the crops left in it. Only infused grain can be planted on it: the seeds of other plants want vanilla farmland.
 */
public class BlockInfusedFarmland extends FarmBlock implements EntityBlock {

    public BlockInfusedFarmland(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Hardness 0.6, gravel sound (1.7.10 {@code setHardness(0.6F); setStepSound(soundTypeGravel)}). */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6F).sound(SoundType.GRAVEL)
                .isViewBlocking((state, level, pos) -> true).isSuffocating((state, level, pos) -> true);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInfusedFarmland(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    // Prevent farmland degrading

    /** 1.7.10 {@code onFallenUpon}: nothing, the soil is not trampled (the fall is still a fall). */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    /** 1.7.10 {@code updateTick}: nothing; the soil neither dries nor turns back into dirt on its own. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {}
}
