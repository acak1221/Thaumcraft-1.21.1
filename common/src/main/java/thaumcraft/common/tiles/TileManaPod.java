package thaumcraft.common.tiles;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.config.ThaumcraftManaBeans;

/** Original non-ticking pod: block random ticks advance age; the third growth
 * crossbreeds in Forge NORTH,SOUTH,WEST,EAST order. A mature pod reports one
 * aspect but every original extraction/insertion API remains unavailable. */
public final class TileManaPod extends BlockEntity implements IAspectContainer {
    private Aspect aspect;
    public TileManaPod(BlockPos pos,BlockState state){super(ThaumcraftManaBeans.MANA_POD_BLOCK_ENTITY.get(),pos,state);}
    public Aspect getAspect(){return aspect;}
    public void setAspect(Aspect value){
        if(value!=null&&Aspect.getAspect(value.getTag())!=value)throw new IllegalArgumentException("Noncanonical pod aspect");
        if(level!=null&&level.isClientSide)return;
        if(level!=null&&!level.getServer().isSameThread())throw new IllegalStateException("Pod mutation requires the server thread");
        aspect=value;changed();
    }
    public void checkGrowth(){
        if(level==null||level.isClientSide||isRemoved()||level.getBlockEntity(worldPosition)!=this)return;
        if(!level.getServer().isSameThread())throw new IllegalStateException("Pod growth requires the server thread");
        BlockState state=getBlockState();if(!(state.getBlock() instanceof BlockManaPod))return;
        int age=state.getValue(BlockManaPod.AGE);
        if(age<7){age++;level.setBlock(worldPosition,state.setValue(BlockManaPod.AGE,age),3);}
        var growth=chooseAspect(level,worldPosition,age,aspect,level.random);
        if(growth.changed()){aspect=growth.aspect();changed();}
    }
    /**
     * WorldGenRegion-safe initialization after the real pod state/BE was admitted.
     * The generator owns a separate deterministic native worldgen growth stream;
     * runtime block growth still consumes the WORLD stream.
     * Region writes never query/mutate the ServerLevel's live chunk cache.
     *
     * <p>While a chunk is generated, {@code WorldGenRegion#setBlock} stores a
     * {@code DUMMY} block entity marker instead of a real tile, and
     * {@code LevelChunk#promotePendingBlockEntity} creates the real tile from that
     * marker WITHOUT its data. The aspect of the original lives in the tile itself
     * (oracle {@code TileManaPod.java:36-39,42-94}: the growth writes the aspect and
     * the tile saves it), so the port writes the aspect into the pending block entity
     * data of the chunk: the promoted tile then carries it, exactly as the original's
     * own tile does. The age of the original grows in the same call
     * (oracle {@code WorldGenManaPods.java:26-29}: the metadata two to six is placed
     * and {@code checkGrowth} raises it once, so the world carries the age three to
     * seven).</p>
     */
    public static boolean initializeGenerated(WorldGenLevel region,BlockPos pos,RandomSource growthRandom) {
        if(region instanceof ServerLevel server&&!server.getServer().isSameThread())throw new IllegalStateException("Live pod generation requires the server thread");
        if(!region.hasChunkAt(pos)||!thaumcraft.common.lib.world.WorldGenWriteArea.canWrite(region,pos))return false;
        BlockState state=region.getBlockState(pos);if(!(state.getBlock() instanceof BlockManaPod))return false;
        int age=state.getValue(BlockManaPod.AGE);if(age<7){age++;if(!region.setBlock(pos,state.setValue(BlockManaPod.AGE,age),2))return false;}
        Aspect existing=region.getBlockEntity(pos) instanceof TileManaPod live?live.getAspect():pendingAspect(region,pos);
        var growth=chooseAspect(region,pos,age,existing,growthRandom);if(!growth.changed())return true;
        if(region.getBlockEntity(pos) instanceof TileManaPod live){live.setAspect(growth.aspect());return true;}
        return writePendingAspect(region,pos,growth.aspect());
    }
    /** The chunk that owns the cell, without ever requesting another one. */
    private static net.minecraft.world.level.chunk.ChunkAccess chunkOf(WorldGenLevel region,BlockPos pos) {
        if(region instanceof ServerLevel server)return server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return region.getChunk(pos.getX()>>4,pos.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES,false);
    }
    /** The aspect the pending tile already carries, if any. */
    private static Aspect pendingAspect(WorldGenLevel region,BlockPos pos) {
        var chunk=chunkOf(region,pos);if(chunk==null)return null;
        var tag=chunk.getBlockEntityNbt(pos);if(tag==null)return null;
        return Aspect.getAspect(tag.getString("aspect"));
    }
    /**
     * The aspect of the original is written into the pending block entity data of the
     * chunk, so the real tile of the promoted chunk carries it. The marker of the
     * generation region is replaced by a real tile record of this port.
     */
    private static boolean writePendingAspect(WorldGenLevel region,BlockPos pos,Aspect aspect) {
        var chunk=chunkOf(region,pos);if(chunk==null)return false;
        var tag=chunk.getBlockEntityNbt(pos);
        if(tag==null){tag=new CompoundTag();tag.putInt("x",pos.getX());tag.putInt("y",pos.getY());tag.putInt("z",pos.getZ());}
        tag.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE
                .getKey(ThaumcraftManaBeans.MANA_POD_BLOCK_ENTITY.get()).toString());
        tag.putString("aspect",aspect.getTag());
        chunk.setBlockEntityNbt(tag);
        return true;
    }
    private record Growth(Aspect aspect,boolean changed) {}
    private static Growth chooseAspect(BlockGetter neighbors,BlockPos origin,int age,Aspect aspect,RandomSource random) {
        if(age<=2)return new Growth(aspect,false);
        boolean changed=false;
        if(age==3){
            AspectList parents=new AspectList();if(aspect!=null)parents.add(aspect,1);
            for(Direction direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST})
                if(neighbors.getBlockEntity(origin.relative(direction)) instanceof TileManaPod pod&&pod.aspect!=null)parents.add(pod.aspect,1);
            if(parents.size()>1){
                var choices=new ArrayList<Aspect>();Aspect[] types=parents.getAspects();
                for(int i=0;i<types.length;i++){
                    choices.add(types[i]);
                    for(int j=0;j<types.length;j++)if(i!=j){Aspect combined=Aspect.getCombinationResult(types[i],types[j]);if(combined!=null){choices.add(combined);choices.add(combined);}}
                }
                if(!choices.isEmpty()){aspect=choices.get(random.nextInt(choices.size()));changed=true;}
            }
            if(parents.size()>0&&aspect==null){aspect=parents.getAspectsSortedAmount()[0];changed=true;}
        }
        if(aspect==null){
            if(random.nextInt(8)==0)aspect=Aspect.PLANT;
            else{var primals=Aspect.getPrimalAspects();aspect=primals.get(random.nextInt(primals.size()));}
            changed=true;
        }
        return new Growth(aspect,changed);
    }
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);if(aspect!=null)tag.putString("aspect",aspect.getTag());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);aspect=Aspect.getAspect(tag.getString("aspect"));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AspectList getAspects(){return aspect!=null&&getBlockState().getValue(BlockManaPod.AGE)==7?new AspectList().add(aspect,1):null;}
    @Override public void setAspects(AspectList aspects){}
    @Override public boolean doesContainerAccept(Aspect aspect){return false;}
    @Override public int addToContainer(Aspect aspect,int amount){return 0;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){return false;}
    @Override public boolean takeFromContainer(AspectList aspects){return false;}
    @Override public boolean doesContainerContainAmount(Aspect aspect,int amount){return false;}
    @Override public boolean doesContainerContain(AspectList aspects){return false;}
    @Override public int containerContains(Aspect aspect){return 0;}
}
