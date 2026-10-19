package thaumcraft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;
import java.util.List;

public final class ItemEssence extends Item {
    public ItemEssence(Properties properties) { super(properties); }
    public static ItemStack filled(String tag) {
        ItemStack result = new ItemStack(ThaumcraftContent.FILLED_PHIAL.get());
        result.set(ThaumcraftContent.PHIAL_DATA.get(), new PhialData(tag));
        return result;
    }
    public static PhialData data(ItemStack stack) { return stack.get(ThaumcraftContent.PHIAL_DATA.get()); }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        // Secondary-use skips the block hook in 1.21. Use exactly the same
        // item-first path, including its whole-phial price and creative debit.
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
        return interact(context.getItemInHand(), context.getLevel(), context.getClickedPos(), player, context.getHand(), hit).result();
    }

    public static ItemInteractionResult interact(ItemStack held, Level level, BlockPos pos, Player player,
                                                InteractionHand hand, BlockHitResult hit) {
        boolean empty = held.is(ThaumcraftContent.EMPTY_PHIAL.get());
        if (!empty && !held.is(ThaumcraftContent.FILLED_PHIAL.get()) || !level.hasChunkAt(pos))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        BlockEntity tile = level.getBlockEntity(pos);
        TileJar jar = tile instanceof TileJar value && level.getBlockState(pos).getBlock() instanceof BlockJar ? value : null;
        TileAlembic alembic = tile instanceof TileAlembic value && level.getBlockState(pos).getBlock() instanceof BlockAlembic ? value : null;
        if (jar == null && alembic == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.isEmpty() || player.level() != level || player.isSpectator() || player.getItemInHand(hand) != held
                || !hit.getBlockPos().equals(pos) || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), held) || tile.isRemoved() || EssentiaTransfers.isLocked(tile)
                || level.getBlockEntity(pos) != tile || !level.getBlockState(pos).is(tile.getBlockState().getBlock())) return ItemInteractionResult.FAIL;
        EssentiaStorageEndpoint storage = (EssentiaStorageEndpoint) tile;
        Aspect beforeType = storage.getStoredEssentiaType();
        int before = storage.getStoredEssentiaAmount();
        Aspect afterType;
        int after;
        ItemStack output;
        if (empty) {
            if (beforeType == null || Aspect.getAspect(beforeType.getTag()) != beforeType || before < PhialData.AMOUNT) return ItemInteractionResult.FAIL;
            afterType = beforeType;
            after = before - PhialData.AMOUNT;
            output = filled(beforeType.getTag());
        } else {
            PhialData data = data(held);
            afterType = data == null ? null : Aspect.getAspect(data.tag());
            // ItemEssence:171-178: void jars do NOT waive eight free spaces.
            // Acceptance is label-only, so test actual stored type separately.
            if (jar == null || afterType == null || !jar.doesContainerAccept(afterType)
                    || before > 0 && beforeType != afterType || before > jar.getMaxAmount() - PhialData.AMOUNT)
                return ItemInteractionResult.FAIL;
            after = before + PhialData.AMOUNT;
            output = new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get());
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!EssentiaTransfers.exchangeItem(tile, player, hand, held, beforeType, before, afterType, after, output, false))
            return ItemInteractionResult.FAIL;
        level.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .25F, 1);
        return ItemInteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        PhialData data = data(stack);
        if (data != null) tooltip.add(Component.literal(Aspect.getAspect(data.tag()).getName() + " × " + PhialData.AMOUNT));
    }
}
