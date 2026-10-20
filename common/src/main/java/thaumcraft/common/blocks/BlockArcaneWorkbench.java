package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileTable;

/** Flattened TC4 blockTable:15, obtained through its original free base-table wand trigger. */
public final class BlockArcaneWorkbench extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneWorkbench> CODEC = simpleCodec(BlockArcaneWorkbench::new);

    public BlockArcaneWorkbench(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileArcaneWorkbench(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    // The original BlockTable collision/ray bounds are the full cube, not its visible legs.
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(new ItemStack(ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get())); }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) { return new ItemStack(ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get()); }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, player); }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileArcaneWorkbench table)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) MenuRegistry.openExtendedMenu(serverPlayer, table, data -> data.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && level.getBlockEntity(pos) instanceof TileArcaneWorkbench table) table.evacuate();
        super.onRemove(state, level, pos, replacement, moving);
    }

    public static InteractionResult tryCreate(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || player.isSpectator() || !ArcaneVis.isWorkbenchWand(held) || !level.hasChunkAt(pos)) return InteractionResult.PASS;
        BlockState before = level.getBlockState(pos);
        // Source BlockTable:224-238 permits exactly metadata 0/1. Do not convert
        // a research main, a secondary/double-table half, or any foreign BE.
        if (!before.is(ThaumcraftContent.TABLE.get()) || before.getValue(BlockTable.VARIANT) > 1
                || level.getBlockEntity(pos) == null || level.getBlockEntity(pos).getClass() != TileTable.class) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Arcane conversion requires the server thread");

        BlockState after = ThaumcraftArcane.ARCANE_WORKBENCH.get().defaultBlockState();
        if (!level.setBlock(pos, after, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof TileArcaneWorkbench table)) {
            level.setBlock(pos, before, Block.UPDATE_ALL);
            return InteractionResult.FAIL;
        }
        // Transfer the actual held wand, including centivis and every component.
        // Source clears the selected stack even for creative players; no vis debit.
        table.setItem(TileArcaneWorkbench.WAND_SLOT, held.copy());
        player.setItemInHand(context.getHand(), ItemStack.EMPTY);
        player.getInventory().setChanged();
        level.playSound(null, pos.getX() + .5, pos.getY() + .1, pos.getZ() + .5,
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, .15F, .5F);
        return InteractionResult.CONSUME;
    }
}
