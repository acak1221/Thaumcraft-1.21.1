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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.tiles.TileSpa;

/** Original StoneDevice12 full collision cube, filled-container payment and empty-container menu opening. */
public final class BlockSpa extends BaseEntityBlock {
    public static final MapCodec<BlockSpa> CODEC=simpleCodec(BlockSpa::new);
    public BlockSpa(Properties properties){super(properties.strength(3,15).sound(SoundType.STONE).noOcclusion());}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TileSpa(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ThaumcraftSpa.SPA_TILE.get(),TileSpa::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown()||!(level.getBlockEntity(pos) instanceof TileSpa tile))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide&&!tile.canInteract())return ItemInteractionResult.CONSUME;
        if(player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return ItemInteractionResult.FAIL;
        if(TileSpa.filledContainer(held)!=null){
            if(player instanceof ServerPlayer actor&&actor.canInteractWithBlock(pos,0))tile.fillFromHeldContainer(actor,hand);
            // Original full/wrong-fluid tanks swallow the click without opening a menu or spending the container.
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return open(level,pos,player).consumesAction()?ItemInteractionResult.sidedSuccess(level.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return open(level,pos,player);}
    private InteractionResult open(Level level,BlockPos pos,Player player){
        if(player.isShiftKeyDown()||player.isSpectator()||!(level.getBlockEntity(pos) instanceof TileSpa tile))return InteractionResult.PASS;
        if(!level.isClientSide&&!tile.canInteract())return InteractionResult.CONSUME;
        if(!tile.stillValid(player))return InteractionResult.PASS;
        if(player instanceof ServerPlayer actor&&tile.isLiveOwner()&&actor.canInteractWithBlock(pos,0)&&level.mayInteract(actor,pos))MenuRegistry.openExtendedMenu(actor,tile,data->data.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!replacement.is(this)&&level.getBlockEntity(pos) instanceof TileSpa tile)tile.evacuate();super.onRemove(state,level,pos,replacement,moving);
    }
}
