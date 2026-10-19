package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileInfusionMatrix;

/** Flattened BlockStoneDevice:2; native full-cube selection/collision, original BER. */
public final class BlockInfusionMatrix extends BaseEntityBlock {
    public static final MapCodec<BlockInfusionMatrix> CODEC = simpleCodec(BlockInfusionMatrix::new);

    public BlockInfusionMatrix(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileInfusionMatrix(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ThaumcraftInfusion.MATRIX_BLOCK_ENTITY.get(), TileInfusionMatrix::tick);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.getItem() instanceof ItemWandCasting && (player.isSpectator() || player.level() != level
                || !level.hasChunkAt(pos) || !player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), held))) return ItemInteractionResult.FAIL;
        // The wand item's InfusionStructure hook also handles secondary use.
        // Do not start here and then invoke that hook a second time in one click.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean blast = !replacement.is(this) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof TileInfusionMatrix matrix && matrix.isCrafting();
        // Remove the old BE before the explosion can synchronously remove blocks.
        // No manual matrix item drop: the native loot path alone owns that item.
        super.onRemove(state, level, pos, replacement, moving);
        // Original BlockStoneDevice:312-318, unlike instability's terrain-safe blasts.
        if (blast) level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2.0F, Level.ExplosionInteraction.BLOCK);
    }
}
