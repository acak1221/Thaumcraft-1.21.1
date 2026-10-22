package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileMagicBox;

/**
 * Original BlockMagicBox: the linked storage box. It has no item form in the
 * original creative tab and opens its own inventory interface.
 */
public final class BlockMagicBox extends BaseEntityBlock {
    public static final MapCodec<BlockMagicBox> CODEC = simpleCodec(BlockMagicBox::new);

    public BlockMagicBox(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    /**
     * The original BlockMagicBox is an ordinary visible block: the 1.7.10 class extends
     * {@code net.minecraft.block.BlockContainer} (OpenTC4 BlockMagicBox:22) and it does not
     * override {@code getRenderType()}, so the vanilla render type zero draws its model.
     * {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE} for every block entity
     * block (Minecraft 1.21.1 BaseEntityBlock:22-25), so the port has to say it again.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** BlockMagicBox:87-91 - every magic box carries the inventory block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileMagicBox(pos, state);
    }

    /**
     * Original BlockMagicBox and TileMagicBox:165-170 - the box links itself to its neighbours.
     * The original ticked the tile on both sides, so the ticker runs on the client as well.
     */
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftEldritchBlocks.MAGIC_BOX_TILE.get(), TileMagicBox::tick);
    }

    /** BlockMagicBox:71-85 - the right click opens the box interface. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileMagicBox box)) return InteractionResult.PASS;
        if (!box.stillValid(player)) return InteractionResult.PASS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return InteractionResult.PASS;
        // The client rebuilds the menu from the position and the size of the linked box chain.
        dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(server,
                new net.minecraft.world.SimpleMenuProvider(
                        (menuId, inventory, viewer) -> new thaumcraft.common.container.ContainerMagicBox(menuId, inventory, box, pos, box.getContainerSize()),
                        Component.translatable("container.thaumcraft.magic_box")),
                buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeVarInt(box.getContainerSize());
                });
        return InteractionResult.CONSUME;
    }

    /** Oracle BlockMagicBox:72-75: breakBlock evacuates stock for every removal cause. */
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof TileMagicBox box) {
            box.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
