package thaumic.tinkerer.common.block;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.block.camo.DeviceSounds;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Animation Tablet (Tool Dynamism Tablet), a two-pixel plate that works on the block in front of it.
 *
 * <p>1.7.10 kept the facing in the metadata (2 north, 3 south, 4 west, 5 east) and the "powered" mark in bit 8; they are
 * the {@code facing} and {@code powered} block state properties now. The block faces the player who places it.</p>
 */
public class BlockAnimationTablet extends BaseEntityBlock implements ITTinkererRegisterable {
    public static final MapCodec<BlockAnimationTablet> CODEC = simpleCodec(BlockAnimationTablet::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 1.7.10 metadata bit 8: the redstone mode saw power; the tablet cannot be turned with a wand while it is set. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** {@code setBlockBounds(0, 0, 0, 1, 1/16*2, 1)}. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public BlockAnimationTablet() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F, 50F).sound(SoundType.METAL).noOcclusion());
    }

    protected BlockAnimationTablet(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** {@code onBlockPlacedBy}: the tablet faces the player (the direction opposite to where the player looks). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAnimationTablet(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, TileBlocks.animationTabletType(), TileAnimationTablet::clientTick)
                : createTickerHelper(type, TileBlocks.animationTabletType(), TileAnimationTablet::serverTick);
    }

    /** {@code breakBlock}: the held item spills out (in stacks of ten to thirty, as the original did). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * {@code onNeighborBlockChange}: in redstone mode the tablet remembers whether it or the block above it is powered
     * (the original set this only while the redstone mode was on and left a stale mark after switching it off; here the
     * mark follows the mode).
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof TileAnimationTablet tablet)) return;
        boolean power = tablet.redstone && (level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above()));
        if (power != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, power), Block.UPDATE_INVISIBLE);
        }
    }

    /** The wand turns the tablet (north, south, west, east, north), unless it is powered, swinging or breaking. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !WandHelper.isWand(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileAnimationTablet tablet) {
            boolean activated = state.getValue(POWERED);
            if (!activated && !tablet.getIsBreaking() && tablet.isIdle()) {
                level.setBlock(pos, state.setValue(FACING, nextFacing(state.getValue(FACING))), Block.UPDATE_ALL);
                level.playSound(null, pos, DeviceSounds.WAND_TOOL, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1F);
            } else {
                // Rare chance this might happen, but better to cope for it.
                player.sendSystemMessage(Component.translatable("ttmisc.animationTablet.notRotatable"));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The turn order of the original metadata: 2 north, 3 south, 4 west, 5 east, then 2 again. */
    public static Direction nextFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.EAST;
            default -> Direction.NORTH;
        };
    }

    /** Any other right click opens the tablet's interface. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileAnimationTablet tablet) {
            MenuRegistry.openExtendedMenu(serverPlayer, tablet);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_ANIMATION_TABLET,
                new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.METAL, 1).add(Aspect.MOTION, 1)
                        .add(Aspect.ENERGY, 1),
                -8,
                2,
                4,
                new ItemStack(this)).setWarp(1).setParents(LibResearch.KEY_MAGNETS).setPages(
                        new ResearchPage("0"),
                        ResearchHelper.arcaneRecipePage(LibResearch.KEY_ANIMATION_TABLET));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_ANIMATION_TABLET,
                LibResearch.KEY_ANIMATION_TABLET,
                new ItemStack(this),
                new AspectList().add(Aspect.AIR, 25).add(Aspect.ORDER, 15).add(Aspect.FIRE, 10),
                "GIG",
                "ICI",
                'G',
                new ItemStack(Items.GOLD_INGOT),
                'I',
                new ItemStack(Items.IRON_INGOT),
                'C',
                ItemsTC.stack("golem_core_blank"));
    }
}
