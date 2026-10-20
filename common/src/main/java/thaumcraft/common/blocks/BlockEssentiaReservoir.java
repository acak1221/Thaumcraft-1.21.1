package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.ReservoirRules;
import thaumcraft.common.tiles.TileEssentiaReservoir;

/**
 * Original BlockEssentiaReservoir (hardness 2, resistance 17, metal, not opaque). It is not volatile only while it holds little: a
 * reservoir that held at least 16 units bursts when it is removed, however it is removed, with a small explosion and a spray of flux
 * (goo below its own level, gas at or above it, one more block than it held sixteenths of units, thrown into the air within four
 * blocks). The stock is not carried by the item. A wand turns the opening toward the clicked side (the opposite one while sneaking).
 */
public final class BlockEssentiaReservoir extends BaseEntityBlock {
    public static final MapCodec<BlockEssentiaReservoir> CODEC = simpleCodec(BlockEssentiaReservoir::new);

    public BlockEssentiaReservoir(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEssentiaReservoir(pos, state); }
    /** INTERIM: a plain translucent cube until the original tile renderer (the vessel and its coloured contents) is ported. */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEssentiaReservoir reservoir ? reservoir.comparatorSignal() : 0;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftTubes.RESERVOIR_BLOCK_ENTITY.get(), TileEssentiaReservoir::tick);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof TileEssentiaReservoir reservoir) {
            int sprays = ReservoirRules.sprays(reservoir.getAmount());
            if (sprays > 0) burst(server, pos, sprays);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Original breakBlock: the explosion first, then up to 50 random air blocks within +-4 take the flux until {@code sprays + 1} are placed. */
    static void burst(ServerLevel level, BlockPos pos, int sprays) {
        level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, ReservoirRules.BURST_POWER, Level.ExplosionInteraction.MOB);
        int placed = 0;
        for (int attempt = 0; attempt < ReservoirRules.BURST_TRIES; ++attempt) {
            int x = pos.getX() + ReservoirRules.burstOffset(level.random::nextInt);
            int y = pos.getY() + ReservoirRules.burstOffset(level.random::nextInt);
            int z = pos.getZ() + ReservoirRules.burstOffset(level.random::nextInt);
            BlockPos target = new BlockPos(x, y, z);
            if (!FluxFiniteBlock.loaded(level, target) || !ReservoirRules.burstTarget(target.equals(pos), level.getBlockState(target).isAir())) continue;
            FluxFiniteBlock flux = ReservoirRules.gooAt(y, pos.getY()) ? ThaumcraftFlux.FLUX_GOO.get() : ThaumcraftFlux.FLUX_GAS.get();
            level.setBlock(target, flux.withGeometry(flux.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, ReservoirRules.FLUX_LEVEL), level, target), Block.UPDATE_ALL);
            if (ReservoirRules.burstDone(placed++, sprays)) break;
        }
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.level() != level || player.getItemInHand(hand) != held || !pos.equals(hit.getBlockPos()) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(this)) return ItemInteractionResult.FAIL;
        return switch (tryWandInteraction(new UseOnContext(player, hand, hit))) {
            case SUCCESS -> ItemInteractionResult.SUCCESS; case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL; case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    /** Also used by the wand item when native secondary use skips the block's own use (a sneaking click turns the opening toward the side). */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || !(held.getItem() instanceof ItemWandCasting) || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof TileEssentiaReservoir reservoir)) return InteractionResult.PASS;
        if (!mayUse(context, held, reservoir)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (reservoir.setFacing(ReservoirRules.wandFacing(context.getClickedFace(), player.isShiftKeyDown()))) player.swing(context.getHand(), true);
        return InteractionResult.CONSUME;
    }

    private static boolean mayUse(UseOnContext context, ItemStack held, TileEssentiaReservoir reservoir) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return player != null && player.level() == level && player.getItemInHand(context.getHand()) == held && !player.isSpectator()
                && !reservoir.isRemoved() && !reservoir.isQuarantined() && reservoir.getLevel() == level && !EssentiaTransfers.isLocked(reservoir)
                && level.hasChunkAt(pos) && level.getBlockEntity(pos) == reservoir && level.getBlockState(pos) == reservoir.getBlockState()
                && level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && player.canInteractWithBlock(pos, 0)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, context.getClickedFace(), held);
    }
}
