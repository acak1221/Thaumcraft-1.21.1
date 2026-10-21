package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.items.relics.ItemHandMirror;
import thaumcraft.common.lib.world.BlockFamilyShapes;
import thaumcraft.common.lib.world.EldritchBlockRules;
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
        // The table holds the plate of the side it is indexed by: the bottom, the top, the
        // north (-Z), the south (+Z), the west (-X) and the east (+X) side of the block.
        for (Direction side : Direction.values()) SHAPES[side.ordinal()] = BlockFamilyShapes.mirrorPlate(side);
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

    /**
     * BlockMirror:168-175 - the original adds the clicked side to the metadata of the item,
     * so the side of the block the player clicked becomes the direction the mirror looks at
     * and the wall it hangs on is the opposite side.
     */
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    /**
     * BlockMirror:268-295 - the plate of the mirror is the thin square on the side opposite
     * to the direction it looks at, that is the side of the wall it hangs on.
     */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[EldritchBlockRules.mirrorWallSide(state.getValue(FACING)).ordinal()];
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

    /**
     * BlockMirror:92-104 - both mirror tiles update. The item mirror ejects its
     * items, decays its instability and repairs a broken link, the essentia
     * mirror repairs its own broken link. Without the ticker neither tile ever
     * ran.
     */
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return state.getValue(ESSENTIA)
                ? createTickerHelper(type, ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_TILE.get(), TileMirrorEssentia::tick)
                : createTickerHelper(type, ThaumcraftEldritchBlocks.MIRROR_TILE.get(), TileMirror::tick);
    }

    /**
     * BlockMirror:177-203 - the mirror breaks when the wall behind it, the side opposite to
     * the direction it looks at, is gone.
     */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(EldritchBlockRules.mirrorWallSide(facing));
        if (!level.getBlockState(support).isFaceSturdy(level, support, facing)) {
            level.destroyBlock(pos, false);
        }
    }

    /** BlockMirror:220-227 - an item that enters the mirror is sent to its twin. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) return;
        if (state.getValue(ESSENTIA)) return;
        // Oracle BlockMirror:163 - the 1.7.10 name `field_71088_bW` belongs to
        // Entity (it is `timeUntilPortal` in the 1.7.10 mapping), not to the
        // pickup delay of the item. The original therefore takes the item as
        // soon as it enters the mirror; it never waits for the pickup delay.
        if (!(entity instanceof ItemEntity item) || item.isRemoved()) return;
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

    /**
     * BlockMirror:227-235 with ItemHandMirror:100-121 and BlockMirrorItem:86-139 - the original ran the
     * item hook before the mirror consumed the click, so the hand mirror and the mirror item skip
     * the default block interaction of 1.21.1 and every other item is still refused by the mirror.
     */
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.CONSUME;
        if (stack.getItem() instanceof ItemHandMirror || stack.getItem() instanceof ItemMirrorBlock) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.CONSUME;
    }
}
