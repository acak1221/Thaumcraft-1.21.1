package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.tiles.TileManaPod;

/** Hanging, physically collidable TC4 mana pod. Native BLOCK_ENTITY loot context
 * replaces the original static coordinate cache; Fortune/Silk never replace the
 * typed one-or-two-bean harvest. Only original six vanilla log/wood species and the
 * two TC4 magical logs support planting (no stripped/plank/new-species expansion). */
public final class BlockManaPod extends BaseEntityBlock {
    public static final MapCodec<BlockManaPod> CODEC=simpleCodec(BlockManaPod::new);
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,7);
    private static final VoxelShape[] SHAPES={box(4,12,4,12,16,12),box(4,10,4,12,16,12),box(4,8,4,12,16,12),box(4,6,4,12,16,12),box(4,5,4,12,16,12),box(4,4,4,12,16,12),box(4,3,4,12,16,12),box(4,2,4,12,16,12)};
    public BlockManaPod(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(AGE,0));}
    @Override protected MapCodec<BlockManaPod> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TileManaPod(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPES[state.getValue(AGE)];}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPES[state.getValue(AGE)];}
    @Override protected boolean isPathfindable(BlockState state,PathComputationType type){return true;}
    public static boolean isSupport(BlockState state){return state.is(Blocks.OAK_LOG)||state.is(Blocks.SPRUCE_LOG)||state.is(Blocks.BIRCH_LOG)||state.is(Blocks.JUNGLE_LOG)||state.is(Blocks.ACACIA_LOG)||state.is(Blocks.DARK_OAK_LOG)||state.is(Blocks.OAK_WOOD)||state.is(Blocks.SPRUCE_WOOD)||state.is(Blocks.BIRCH_WOOD)||state.is(Blocks.JUNGLE_WOOD)||state.is(Blocks.ACACIA_WOOD)||state.is(Blocks.DARK_OAK_WOOD)||state.is(ThaumcraftNaturals.GREATWOOD_LOG.get())||state.is(ThaumcraftNaturals.SILVERWOOD_LOG.get());}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBiome(pos).is(ThaumcraftManaBeans.MAGICAL_BIOMES)&&isSupport(level.getBlockState(pos.above()));}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos neighbor,boolean moved){if(!level.isClientSide&&!state.canSurvive(level,pos))level.destroyBlock(pos,true);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource ignored){
        if(!state.canSurvive(level,pos))level.destroyBlock(pos,true);
        else if(level.random.nextInt(30)==0&&level.getBlockEntity(pos) instanceof TileManaPod pod)pod.checkGrowth();
    }
    @Override protected float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos pos){return super.getDestroyProgress(state,player,level,pos)*(8-state.getValue(AGE));}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder context){
        var output=new ArrayList<ItemStack>();if(state.getValue(AGE)<2)return output;
        int count=state.getValue(AGE)==7&&context.getLevel().random.nextFloat()>.33F?2:1;
        Aspect aspect=context.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TileManaPod pod&&pod.getAspect()!=null?pod.getAspect():Aspect.PLANT;
        for(int i=0;i<count;i++)output.add(ItemManaBean.create(aspect));return output;
    }
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state){return new ItemStack(ThaumcraftManaBeans.MANA_BEAN.get());}
}
