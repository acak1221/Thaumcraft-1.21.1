package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.lib.world.WardedBehaviour;
import thaumcraft.common.tiles.TileWarded;

/** Original warded wrapper: unbreakable, no item/loot/explosion removal, with the remembered block's safe world behaviour. */
public final class BlockWarded extends BaseEntityBlock {
    public static final MapCodec<BlockWarded> CODEC = simpleCodec(BlockWarded::new);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public BlockWarded(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(LIGHT, 0)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIGHT); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileWarded(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return true; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getShape(state, level, pos, context) : original.getShape(level, pos, context);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getCollisionShape(state, level, pos, context) : original.getCollisionShape(level, pos, context);
    }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getOcclusionShape(state, level, pos) : original.getOcclusionShape(level, pos);
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getBlockSupportShape(state, level, pos) : original.getBlockSupportShape(level, pos);
    }
    @Override protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getVisualShape(state, level, pos, context) : original.getVisualShape(level, pos, context);
    }
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getLightBlock(state, level, pos) : original.getLightBlock(level, pos);
    }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? super.getShadeBrightness(state, level, pos) : original.getShadeBrightness(level, pos);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockState original = WardedBehaviour.original(level, pos);
        return original == null ? ItemStack.EMPTY : original.getBlock().getCloneItemStack(level, pos, original);
    }
    @Override protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        BlockState original = WardedBehaviour.original(level, pos);
        WardedBehaviour.invoke(level, pos, original, () -> original.attack(level, pos, player));
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        BlockState original = WardedBehaviour.original(level, pos);
        WardedBehaviour.invoke(level, pos, original, () -> original.entityInside(level, pos, entity));
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        BlockState original = WardedBehaviour.original(level, pos);
        WardedBehaviour.invoke(level, pos, original, () -> original.getBlock().stepOn(level, pos, original, entity));
    }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        BlockState original = WardedBehaviour.original(level, pos);
        if (original == null) super.fallOn(level, state, pos, entity, distance);
        else WardedBehaviour.invoke(level, pos, original, () -> original.getBlock().fallOn(level, original, pos, entity, distance));
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockState original = WardedBehaviour.original(level, pos);
        WardedBehaviour.invoke(level, pos, original, () -> original.getBlock().animateTick(original, level, pos, random));
    }
    @Override protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> drop) {
        // Original BlockWarded.onBlockExploded is deliberately empty, including when a huge explosion reaches the block.
    }
}
