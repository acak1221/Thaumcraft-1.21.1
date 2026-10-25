package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.tiles.TileThaumatorium;
import thaumcraft.common.tiles.TileThaumatoriumTop;

/** Two real blocks replace the alchemical constructs, sharing a lower inventory and front. */
public final class BlockThaumatorium extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final MapCodec<BlockThaumatorium> CODEC = simpleCodec(BlockThaumatorium::new);
    private final boolean top;
    public BlockThaumatorium(Properties properties) { this(properties, false); }
    public BlockThaumatorium(Properties properties, boolean top) { super(properties); this.top = top; registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return top ? new TileThaumatoriumTop(pos, state) : new TileThaumatorium(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // The upper half watches its lower half, exactly as the original did.
        if (top) {
            return level.isClientSide ? null
                    : createTickerHelper(type, ThaumcraftThaumatorium.TOP_TILE.get(), TileThaumatoriumTop::tick);
        }
        // Original TileThaumatorium:221-230 - the client of the machine draws the vent.
        return createTickerHelper(type, ThaumcraftThaumatorium.TILE.get(), TileThaumatorium::tick);
    }
    private TileThaumatorium machine(Level level, BlockPos pos) {
        BlockEntity tile = level.getBlockEntity(top ? pos.below() : pos);
        return tile instanceof TileThaumatorium machine ? machine : null;
    }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        TileThaumatorium tile = machine(level, pos);
        if (tile == null || player.isShiftKeyDown() || player.isSpectator()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sender && tile.stillValid(sender)) {
            var recipes = thaumcraft.common.crafting.ProgrammaticRecipes.crucibles(level.getRecipeManager());
            MenuRegistry.openExtendedMenu(sender, new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, viewer) -> new thaumcraft.common.container.ContainerThaumatorium(id, inventory, tile, recipes), tile.getDisplayName()), data -> {
                data.writeBlockPos(tile.getBlockPos());
                thaumcraft.common.crafting.CrucibleRecipeDisplay.write(data, level.registryAccess(), recipes);
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, player); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return !top; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { TileThaumatorium tile = machine(level, pos); return tile == null ? 0 : AbstractContainerMenu.getRedstoneSignalFromContainer(tile); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        TileThaumatorium tile = machine(level, pos);
        if (tile == null || !tile.structureValid()) level.setBlock(pos, ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(), Block.UPDATE_ALL);
        else tile.refreshUpgrades();
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.getBlock() != replacement.getBlock() && !top && level.getBlockEntity(pos) instanceof TileThaumatorium tile) tile.evacuate();
        super.onRemove(state, level, pos, replacement, moving);
    }
}
