package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.lib.AdvancedFurnaceFormation;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedNozzle;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedPart;

/** Original BlockAlchemyFurnace, distinct from the retained normal-furnace class name. */
public final class BlockAdvancedAlchemyFurnace extends BaseEntityBlock {
    public static final MapCodec<BlockAdvancedAlchemyFurnace> CODEC=simpleCodec(BlockAdvancedAlchemyFurnace::new);
    public static final IntegerProperty PART=IntegerProperty.create("part",0,4),LIGHT=IntegerProperty.create("light",0,15);
    private static final VoxelShape INTAKE=box(0,0,0,16,11.2,16);
    @FunctionalInterface public interface ClientEffects { void animate(Level level,BlockPos pos,RandomSource random); }
    private static ClientEffects effects=(level,pos,random)->{};
    public static void setClientEffects(ClientEffects value) { effects=java.util.Objects.requireNonNull(value); }
    public BlockAdvancedAlchemyFurnace(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(PART,0).setValue(LIGHT,0)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(PART,LIGHT); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(PART)==0?new TileAlchemyFurnaceAdvanced(pos,state):state.getValue(PART)==1?new TileAlchemyFurnaceAdvancedNozzle(pos,state):new TileAlchemyFurnaceAdvancedPart(pos,state);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide) return null;
        if(state.getValue(PART)==0) return createTickerHelper(type,ThaumcraftAdvancedFurnace.TILE.get(),TileAlchemyFurnaceAdvanced::tick);
        if(state.getValue(PART)==1) return createTickerHelper(type,ThaumcraftAdvancedFurnace.NOZZLE.get(),TileAlchemyFurnaceAdvancedNozzle::tick);
        return createTickerHelper(type,ThaumcraftAdvancedFurnace.PART_TILE.get(),TileAlchemyFurnaceAdvancedPart::tick);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(PART)==0 && context instanceof EntityCollisionContext entity && !(entity.getEntity() instanceof LivingEntity)?INTAKE:Shapes.block();
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide && state.getValue(PART)==0 && entity instanceof ItemEntity item && level.getBlockEntity(pos) instanceof TileAlchemyFurnaceAdvanced core) core.collide(item);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        return state.getValue(PART)==1 && level.getBlockEntity(pos) instanceof TileAlchemyFurnaceAdvancedNozzle nozzle?nozzle.comparator():0;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!replacement.is(this) || replacement.getValue(PART)!=state.getValue(PART)) AdvancedFurnaceFormation.broken(level,pos,state.getValue(PART));
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(state.getValue(PART)==0 && level.getBlockEntity(pos) instanceof TileAlchemyFurnaceAdvanced core && core.getVis()>0) effects.animate(level,pos,random);
    }
}
