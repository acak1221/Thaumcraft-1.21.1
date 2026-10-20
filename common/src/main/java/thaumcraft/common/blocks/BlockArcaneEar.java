package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.config.ThaumcraftArcaneEar;
import thaumcraft.common.lib.ArcaneEarNotes;
import thaumcraft.common.tiles.TileSensor;

/** Original blockWoodenDevice:1: right click tunes pitch; both power kinds reach every face. */
public final class BlockArcaneEar extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneEar> CODEC = simpleCodec(BlockArcaneEar::new);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public BlockArcaneEar(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(POWERED, false)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWERED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileSensor(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ThaumcraftArcaneEar.ARCANE_EAR_TILE.get(), TileSensor::tick);
    }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof TileSensor tile && tile.getRedstoneTicks() > 0 ? 15 : 0;
    }
    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) { return getSignal(state, level, pos, side); }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!oldState.is(this) && level.getBlockEntity(pos) instanceof TileSensor tile) tile.updateTone();
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (level.getBlockEntity(pos) instanceof TileSensor tile) tile.updateTone();
        super.neighborChanged(state, level, pos, block, from, moved);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileSensor tile) { tile.changePitch(); tile.triggerNote(true); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int instrument, int note) {
        // Native packets decode the action as unsigned: the original silent -1 action arrives at the client as 255.
        if (instrument == 255) instrument = -1;
        if (instrument < -1 || instrument > 4 || note < 0 || note > 24) return false;
        if (level.isClientSide) {
            if (instrument >= 0) level.playLocalSound(pos, ArcaneEarNotes.instrument(instrument).getSoundEvent().value(), SoundSource.BLOCKS,
                    3.0F, (float) Math.pow(2.0, (note - 12) / 12.0), false);
            level.addParticle(ParticleTypes.NOTE, pos.getX() + .5, pos.getY() + 1.2, pos.getZ() + .5, note / 24.0, 0, 0);
        }
        return true;
    }
}
