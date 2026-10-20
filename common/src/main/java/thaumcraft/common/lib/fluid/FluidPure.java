package thaumcraft.common.lib.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftSpa;

/** Native source/flow states; ONLY the exact Classic block consumer owns propagation. */
public abstract class FluidPure extends FlowingFluid {
    @Override public Fluid getSource(){return ThaumcraftSpa.PURE_FLUID_SOURCE.get();}
    @Override public Fluid getFlowing(){return ThaumcraftSpa.PURE_FLUID_FLOWING.get();}
    @Override public Item getBucket(){return ThaumcraftSpa.BUCKET_PURE.get();}
    @Override public boolean isSame(Fluid other){return other instanceof FluidPure;}
    @Override protected boolean canConvertToSource(Level level){return false;}
    @Override public int getTickDelay(LevelReader level){return PureFlowRules.TICK_DELAY;}
    @Override public int getSlopeFindDistance(LevelReader level){return 4;}
    @Override public int getDropOff(LevelReader level){return 1;}
    @Override protected float getExplosionResistance(){return 100F;}
    @Override public void tick(Level level,BlockPos pos,FluidState state){
        if(level.getBlockState(pos).getBlock() instanceof BlockFluidPure pure)pure.flowTick(level,pos,level.getBlockState(pos));
    }
    @Override protected void beforeDestroyingBlock(LevelAccessor level,BlockPos pos,BlockState state){Block.dropResources(state,level,pos,level.getBlockEntity(pos));}
    @Override public boolean canBeReplacedWith(FluidState state,BlockGetter level,BlockPos pos,Fluid other,Direction direction){
        // The old matching water material does not permit vanilla-water replacement.
        // Outgoing Classic canDisplace deliberately has its separate MAX-density rule.
        return direction==Direction.DOWN&&!other.is(FluidTags.WATER);
    }
    @Override public BlockState createLegacyBlock(FluidState state){
        return ThaumcraftSpa.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL,state.isSource()?0:8-state.getAmount());
    }
    @Override public float getOwnHeight(FluidState state){return PureFlowRules.filledHeight(state.isSource()?0:8-state.getAmount());}
    @Override public float getHeight(FluidState state,BlockGetter level,BlockPos pos){return getOwnHeight(state);}
    /** Classic SOURCE ray bounds stay a full cube; its renderer has a separate lower surface. */
    @Override public VoxelShape getShape(FluidState state,BlockGetter level,BlockPos pos){return state.isSource()?Shapes.block():Shapes.box(0,0,0,1,getHeight(state,level,pos),1);}
    @Override public Vec3 getFlow(BlockGetter level,BlockPos pos,FluidState state){return BlockFluidPure.flowVector(level,pos);}
    public static final class Source extends FluidPure {
        @Override public boolean isSource(FluidState state){return true;}
        @Override public int getAmount(FluidState state){return 8;}
    }
    public static final class Flowing extends FluidPure {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> builder){super.createFluidStateDefinition(builder);builder.add(LEVEL);}
        @Override public boolean isSource(FluidState state){return false;}
        @Override public int getAmount(FluidState state){return state.getValue(LEVEL);}
    }
}
