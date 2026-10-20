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
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /** BlockMagicBox:87-91 - every magic box carries the inventory block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileMagicBox(pos, state);
    }

    /** BlockMagicBox:71-85 - the right click opens the box interface. */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileMagicBox box)) return InteractionResult.PASS;
        if (!box.stillValid(player)) return InteractionResult.PASS;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (menuId, inventory, viewer) -> new thaumcraft.common.container.ContainerMagicBox(menuId, inventory, box, pos, box.getContainerSize()),
                Component.translatable("container.thaumcraft.magic_box")));
        return InteractionResult.CONSUME;
    }

    /** BlockMagicBox:64-70 - the box drops its whole content. */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileMagicBox box) {
            box.dropContents(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
