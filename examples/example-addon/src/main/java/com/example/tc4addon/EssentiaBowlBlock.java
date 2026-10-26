package com.example.tc4addon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.items.ItemsTC;

/** The essentia bowl: a plain block with a block entity; crystallized essentia poured into it is read through the API. */
public class EssentiaBowlBlock extends BaseEntityBlock {
    public static final MapCodec<EssentiaBowlBlock> CODEC = simpleCodec(EssentiaBowlBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 5, 14);

    public EssentiaBowlBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EssentiaBowlBlockEntity(pos, state); }

    // [doc:essentia-item]
    /** Crystallized essentia is an IEssentiaContainerItem: its aspect is read through the API, then one crystal is used up. */
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                        InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ItemsTC.require("crystal_essence")) || !(stack.getItem() instanceof IEssentiaContainerItem container)
                || !(level.getBlockEntity(pos) instanceof EssentiaBowlBlockEntity bowl)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // a wand goes on to IWandable
        }
        AspectList contents = container.getAspects(stack);
        if (contents == null || contents.size() != 1) return ItemInteractionResult.FAIL;
        Aspect aspect = contents.getAspects()[0];
        if (!bowl.doesContainerAccept(aspect) || bowl.containerContains(aspect) + contents.getAmount(aspect) > EssentiaBowlBlockEntity.CAPACITY) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            bowl.addToContainer(aspect, contents.getAmount(aspect));
            stack.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    // [/doc]
}
