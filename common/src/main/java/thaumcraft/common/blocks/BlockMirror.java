package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileMirror;
import thaumcraft.common.tiles.TileMirrorEssentia;

/**
 * Original BlockMirror: the item mirror and the essentia mirror. The original
 * metadata zero to five selected the facing, six to eleven the essentia
 * mirror; the facing became a block state property.
 */
public final class BlockMirror extends BaseEntityBlock {
    public static final MapCodec<BlockMirror> CODEC = simpleCodec(BlockMirror::new);
    /** The side the mirror is attached to. */
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
    /** True for the essentia mirror. */
    public static final BooleanProperty ESSENTIA = BooleanProperty.create("essentia");
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        double w = 0.0625D;
        SHAPES[Direction.DOWN.ordinal()] = Block.box(0.0D, 0.0D, 0.0D, 16.0D, w * 16.0D, 16.0D);
        SHAPES[Direction.UP.ordinal()] = Block.box(0.0D, 16.0D - w * 16.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        SHAPES[Direction.NORTH.ordinal()] = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, w * 16.0D);
        SHAPES[Direction.SOUTH.ordinal()] = Block.box(0.0D, 0.0D, 16.0D - w * 16.0D, 16.0D, 16.0D, 16.0D);
        SHAPES[Direction.WEST.ordinal()] = Block.box(16.0D - w * 16.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        SHAPES[Direction.EAST.ordinal()] = Block.box(0.0D, 0.0D, 0.0D, w * 16.0D, 16.0D, 16.0D);
    }

    public BlockMirror(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ESSENTIA, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ESSENTIA);
    }

    public static Direction facingOf(BlockState state) { return state.getValue(FACING); }
    public static boolean isEssentia(BlockState state) { return state.getValue(ESSENTIA); }

    /** BlockMirror:87-93 - the mirror is a thin plate on its facing side. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** BlockMirror:95-99 - the two mirror kinds use different block entities. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(ESSENTIA)) {
            return new TileMirrorEssentia(pos, state);
        }
        return new TileMirror(pos, state);
    }

    /** BlockMirror:150-176 - the mirror breaks when it loses its support. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing);
        if (!level.getBlockState(support).isFaceSturdy(level, support, facing.getOpposite())) {
            level.destroyBlock(pos, false);
        }
    }

    /** BlockMirror:220-227 - an item that enters the mirror is sent to its twin. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) return;
        if (state.getValue(ESSENTIA)) return;
        if (!(entity instanceof ItemEntity item) || item.isRemoved() || item.hasPickUpDelay()) return;
        if (!(level.getBlockEntity(pos) instanceof TileMirror mirror)) return;
        mirror.transport(item);
    }

    /** BlockMirror:186-217 - the drop keeps the link of a linked mirror. */
    @Override protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        ItemStack drop = new ItemStack(this);
        drop.set(DataComponents.ITEM_NAME, Component.translatable(state.getValue(ESSENTIA)
                ? "block.thaumcraft.mirror_essentia" : "block.thaumcraft.mirror"));
        if (params.getLevel() == null) return List.of(drop);
        BlockEntity tile = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (tile instanceof TileMirror mirror && mirror.isLinked()) {
            drop.set(DataComponents.CUSTOM_DATA, CustomData.of(mirror.linkData()));
        } else if (tile instanceof TileMirrorEssentia mirror && mirror.isLinked()) {
            drop.set(DataComponents.CUSTOM_DATA, CustomData.of(mirror.linkData()));
        }
        return List.of(drop);
    }

    /** BlockMirror:227-235 - the original right click is consumed without opening a container. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.CONSUME;
    }
}
