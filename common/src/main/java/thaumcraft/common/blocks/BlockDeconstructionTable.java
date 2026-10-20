package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftDeconstruction;
import thaumcraft.common.tiles.TileDeconstructionTable;

/** The original table keeps full-cube collision/support despite its visible legs. */
public final class BlockDeconstructionTable extends BaseEntityBlock {
    public static final MapCodec<BlockDeconstructionTable> CODEC = simpleCodec(BlockDeconstructionTable::new);
    public BlockDeconstructionTable(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileDeconstructionTable(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ThaumcraftDeconstruction.TILE.get(), TileDeconstructionTable::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, player); }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileDeconstructionTable table)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) MenuRegistry.openExtendedMenu(server, table, data -> data.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState after, boolean moving) {
        if (!after.is(this) && level.getBlockEntity(pos) instanceof TileDeconstructionTable table) table.evacuate();
        super.onRemove(state, level, pos, after, moving);
    }
}
