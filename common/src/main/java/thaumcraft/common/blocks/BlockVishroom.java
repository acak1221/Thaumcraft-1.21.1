package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Actual BlockCustomPlant metadata5: luminous Vishroom, confusion200 on
 * living contact, ordinary plains-soil/light survival and original wisps. */
public final class BlockVishroom extends BushBlock {
    public static final MapCodec<BlockVishroom> CODEC = simpleCodec(BlockVishroom::new);
    private static final VoxelShape SHAPE = Block.box(1.6,0,1.6,14.4,12.8,14.4);
    private static BiConsumer<Level,BlockPos> clientEffects;
    public BlockVishroom(Properties properties) { super(properties); }
    @Override protected MapCodec<BlockVishroom> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos) {
        return soil.is(Blocks.GRASS_BLOCK)||soil.is(Blocks.DIRT)||soil.is(Blocks.COARSE_DIRT)||soil.is(Blocks.PODZOL)||soil.is(Blocks.FARMLAND);
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()),level,pos.below())&&(level.getRawBrightness(pos,0)>=8||level.canSeeSky(pos));
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!state.canSurvive(level,pos))level.destroyBlock(pos,true);
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide&&entity instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.CONFUSION,200,0));
        super.entityInside(state,level,pos,entity);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(random.nextInt(3)==0&&clientEffects!=null)clientEffects.accept(level,pos);
    }
    public static void setClientEffects(BiConsumer<Level,BlockPos> effects) { clientEffects=effects; }
}
