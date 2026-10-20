package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftVisCharging;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileMagicWorkbenchCharger;

/** Original metalDevice:2: fixed hanging relay geometry, no support-break rule, charges a workbench directly below. */
public final class BlockMagicWorkbenchCharger extends BaseEntityBlock {
    public static final MapCodec<BlockMagicWorkbenchCharger> CODEC = simpleCodec(BlockMagicWorkbenchCharger::new);
    private static final VoxelShape SHAPE = box(5, 8, 5, 11, 16, 11);
    public BlockMagicWorkbenchCharger(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileMagicWorkbenchCharger(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftVisCharging.VIS_CHARGE_RELAY_TILE.get(), TileMagicWorkbenchCharger::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && !VisNetHandler.isNativeServerLevel(level)) return ItemInteractionResult.FAIL;
        if (!pos.equals(hit.getBlockPos()) || !level.hasChunkAt(pos) || level.getBlockState(pos) != state
                || player.getItemInHand(hand) != held || !(level.getBlockEntity(pos) instanceof TileMagicWorkbenchCharger tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return switch (tile.interact(new UseOnContext(player, hand, hit))) {
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        if (!context.getLevel().isClientSide && !VisNetHandler.isNativeServerLevel(context.getLevel())) return InteractionResult.FAIL;
        if (!(context.getItemInHand().getItem() instanceof ItemWandCasting) || context.getPlayer() == null
                || !context.getLevel().hasChunkAt(context.getClickedPos())
                || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileMagicWorkbenchCharger tile)) return InteractionResult.PASS;
        return tile.interact(context);
    }
}
