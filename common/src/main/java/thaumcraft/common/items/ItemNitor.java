package thaumcraft.common.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.config.ThaumcraftResources;

/** TC4 ItemResource:196-249. A plain Item keeps its constructor registry-order safe. */
public final class ItemNitor extends Item {
    public ItemNitor(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (player == null || player.isSpectator() || stack.isEmpty() || !level.isLoaded(clicked)) return InteractionResult.FAIL;
        var clickedState = level.getBlockState(clicked);
        Direction face = context.getClickedFace();
        BlockPos target = clicked;
        if (clickedState.is(Blocks.SNOW) && clickedState.getValue(SnowLayerBlock.LAYERS) == 1) {
            // Original thin snow forces side=UP but replaces the snow in place,
            // even when the clicked face was horizontal.
            face = Direction.UP;
        } else if (!clickedState.is(Blocks.VINE) && !clickedState.is(Blocks.SHORT_GRASS)
                && !clickedState.is(Blocks.FERN) && !clickedState.is(Blocks.DEAD_BUSH)
                && !clickedState.canBeReplaced(new BlockPlaceContext(context))) {
            target = clicked.relative(face);
        }
        if (!level.isLoaded(target) || !level.isInWorldBounds(target) || !level.getWorldBorder().isWithinBounds(target)
                || !level.mayInteract(player, target) || !player.mayUseItemAt(target, face, stack)) return InteractionResult.FAIL;

        var hit = new BlockHitResult(context.getClickLocation(), face, target, context.isInside());
        var placement = new BlockPlaceContext(new UseOnContext(player, context.getHand(), hit));
        // Do not let a second native click-offset silently skip an occupied
        // target. Replacement, survival and collision checks are still native.
        if (!placement.getClickedPos().equals(target) || !placement.canPlace()) return InteractionResult.FAIL;
        var block = ThaumcraftResources.NITOR.get();
        var state = block.getStateForPlacement(placement);
        if (state == null || !state.canSurvive(level, target)
                || !level.isUnobstructed(state, target, CollisionContext.of(player))) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(target, state, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        var placed = level.getBlockState(target);
        if (placed.is(block)) {
            block.setPlacedBy(level, target, placed, player, stack);
            if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, target, stack);
        }
        var sound = state.getSoundType();
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(player, state));
        stack.shrink(1); // Deliberately NOT consume(1, player): TC4 also decrements in creative.
        if (player.getAbilities().instabuild) {
            // ServerPlayerGameMode restores the argument stack's old creative
            // count after useOn. Replace the hand reference so that native
            // wrapper cannot undo this source-level resource-item quirk.
            player.setItemInHand(context.getHand(), stack.copy());
        }
        return InteractionResult.CONSUME;
    }
}
