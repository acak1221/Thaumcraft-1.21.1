package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.TilePedestal;

/** Flattened StoneDevice1. Original narrow selection but full-cube physical collision. */
public final class BlockPedestal extends BaseEntityBlock {
    public static final MapCodec<BlockPedestal> CODEC = simpleCodec(BlockPedestal::new);
    private static final VoxelShape SELECTION = box(4, 0, 4, 12, 15.84, 12);

    public BlockPedestal(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TilePedestal(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SELECTION; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TilePedestal pedestal ? AbstractContainerMenu.getRedstoneSignalFromContainer(pedestal) : 0;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level.getBlockEntity(pos) instanceof TilePedestal pedestal) pedestal.evacuate();
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        // StoneDevice:329-332: ANY non-air block above ejects the current item.
        // This is not an owner/crafting lock and does not destroy the pedestal.
        if (level instanceof ServerLevel && level.hasChunkAt(pos.above()) && !level.getBlockState(pos.above()).isAir()
                && level.getBlockEntity(pos) instanceof TilePedestal pedestal) pedestal.evacuate();
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = interact(level, pos, player, hand, held, hit);
        if (result == InteractionResult.FAIL) return ItemInteractionResult.FAIL;
        return result.consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
    }

    private InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (player.isSpectator() || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TilePedestal pedestal)) return InteractionResult.PASS;
        if (!player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Pedestal interaction requires the server thread");
        if (player.level() != level || player.getItemInHand(hand) != held || !level.getBlockState(pos).is(this)
                || level.getBlockEntity(pos) != pedestal || pedestal.isRemoved()) return InteractionResult.FAIL;
        boolean eject = !pedestal.isEmpty();
        if (eject) {
            pedestal.ejectTo(player);
        } else {
            if (held.isEmpty()) return InteractionResult.PASS;
            ItemStack inserted = held.copyWithCount(1);
            // StoneDevice:367-376 consumes one even in creative. Both writes
            // precede the pedestal's callback-bearing changed notification.
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            pedestal.setItem(0, inserted);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2F,
                ((level.random.nextFloat() - level.random.nextFloat()) * .7F + 1) * (eject ? 1.5F : 1.6F));
        return InteractionResult.CONSUME;
    }
}
