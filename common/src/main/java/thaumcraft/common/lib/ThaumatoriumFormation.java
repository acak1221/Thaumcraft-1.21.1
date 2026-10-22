package thaumcraft.common.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.blocks.BlockThaumatorium;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftThaumatorium;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileCrucible;

/** Original WandManager.createThaumatorium accepts a click on either construct and spends 15 Fire, 30 Order and 30 Water. */
public final class ThaumatoriumFormation {
    private ThaumatoriumFormation() {}
    public static InteractionResult tryForm(UseOnContext context) {
        var level = context.getLevel(); BlockPos clicked = context.getClickedPos();
        if (!level.hasChunkAt(clicked) || !level.getBlockState(clicked).is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get())) return InteractionResult.PASS;
        BlockPos base = clicked;
        if (!(level.getBlockEntity(base.below()) instanceof TileCrucible)) base = base.below();
        if (!level.hasChunkAt(base.below()) || !level.hasChunkAt(base.above()) || !(level.getBlockEntity(base.below()) instanceof TileCrucible)
                || !level.getBlockState(base).is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get()) || !level.getBlockState(base.above()).is(ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return InteractionResult.FAIL;
        if (!(context.getPlayer() instanceof ServerPlayer player) || !KnowledgeAccess.get(player).isResearchComplete("THAUMATORIUM")) return InteractionResult.FAIL;
        ItemStack wand = context.getItemInHand();
        ItemStack originalWand = wand.copy();
        var oldBase = level.getBlockState(base); var oldTop = level.getBlockState(base.above());
        for (BlockPos pos : java.util.List.of(base.below(), base, base.above())) if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), wand)) return InteractionResult.FAIL;
        var costs = ArcaneVis.price(ThaumatoriumRules.FORM_VIS, wand, player);
        ItemStack paid = wand.copy();
        if (!ArcaneVis.debit(paid, costs)) return InteractionResult.FAIL;
        if (player.getItemInHand(context.getHand()) != wand || !ItemStack.matches(wand, originalWand)
                || level.getBlockState(base) != oldBase || level.getBlockState(base.above()) != oldTop || !(level.getBlockEntity(base.below()) instanceof TileCrucible)) return InteractionResult.FAIL;
        if (!level.setBlock(base, ThaumcraftThaumatorium.THAUMATORIUM.get().defaultBlockState().setValue(BlockThaumatorium.FACING, context.getClickedFace()), Block.UPDATE_CLIENTS)
                || !level.setBlock(base.above(), ThaumcraftThaumatorium.THAUMATORIUM_TOP.get().defaultBlockState().setValue(BlockThaumatorium.FACING, context.getClickedFace()), Block.UPDATE_CLIENTS)) {
            level.setBlock(base, oldBase, Block.UPDATE_CLIENTS); level.setBlock(base.above(), oldTop, Block.UPDATE_CLIENTS); return InteractionResult.FAIL;
        }
        player.setItemInHand(context.getHand(), paid);
        level.updateNeighborsAt(base, level.getBlockState(base).getBlock()); level.updateNeighborsAt(base.above(), level.getBlockState(base.above()).getBlock());
        level.playSound(null, base, SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.BLOCKS, 1, 1);
        // Original WandManager:244,245 - both halves of the new thaumatorium flash with the
        // original random grey of the block sparkle.
        thaumcraft.common.lib.network.BlockSparkleFxNetworking.sendBlockSparkle(server, base, -9999);
        thaumcraft.common.lib.network.BlockSparkleFxNetworking.sendBlockSparkle(server, base.above(), -9999);
        return InteractionResult.CONSUME;
    }
}
