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
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Actual BlockCustomPlant metadata5: luminous Vishroom, confusion200 on
 * living contact, original BlockBush stay rule and original wisps. */
public final class BlockVishroom extends BushBlock {
    public static final MapCodec<BlockVishroom> CODEC = simpleCodec(BlockVishroom::new);
    private static final VoxelShape SHAPE = Block.box(1.6,0,1.6,14.4,12.8,14.4);
    private static BiConsumer<Level,BlockPos> clientEffects;
    public BlockVishroom(Properties properties) { super(properties); }
    @Override protected MapCodec<BlockVishroom> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos) {
        // Oracle BlockCustomPlant:130-132 + the inherited 1.7.10 BlockBush.func_149718_j
        // (canBlockStay, the official client jar, the class ajr): the plant itself only stays on
        // the grass, the dirt and the farmland. The Cave soil of the manashroom (the metadata
        // five) is the rule of its ITEM (oracle BlockCustomPlantItem:85-101 asks the soil for a
        // Cave plant, and the Cave case of the Forge 1.7.10 table of Block.canSustainPlant is
        // isSideSolid(world, x, y, z, UP)); the item of the manashroom carries that plant type.
        return BlockCustomPlant.bushSoil(soil);
    }
    /**
     * Oracle BlockCustomPlant:130-132 - the original plant answers {@code canPlaceBlockAt} with
     * a plain {@code true}, so it is never refused a position; the rule that removes it again is
     * the inherited {@code BlockBush} stay rule, which 1.21.1 asks in {@code canSurvive}.
     */
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()),level,pos.below());
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
