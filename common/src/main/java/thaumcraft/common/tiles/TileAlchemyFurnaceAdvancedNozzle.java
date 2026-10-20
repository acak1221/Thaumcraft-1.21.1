package thaumcraft.common.tiles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.lib.AdvancedFurnaceRules;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Original single-init outward port. All four native nozzles debit ONE current core pool. */
public final class TileAlchemyFurnaceAdvancedNozzle extends TileAlchemyFurnaceAdvancedPart implements IAspectContainer,EssentiaAtomicEndpoint {
    private static final Direction[] ORDER={Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private Direction facing;
    private boolean initialized,notifying;
    private TileAlchemyFurnaceAdvanced furnace;
    public TileAlchemyFurnaceAdvancedNozzle(BlockPos pos,BlockState state) { super(ThaumcraftAdvancedFurnace.NOZZLE.get(),pos,state); }
    public Direction getFacing() { return facing; }
    /** Pure identity accessor for executor alias locking, never lazily looks up a core. */
    public TileAlchemyFurnaceAdvanced atomicCore() { return furnace; }
    public static void tick(Level level,BlockPos pos,BlockState state,TileAlchemyFurnaceAdvancedNozzle tile) {
        if(!(level instanceof ServerLevel world) || tile.notifying || EssentiaTransfers.isLocked(tile) || !tile.current() || !tile.observeOwner() || tile.initialized) return;
        for(Direction direction:ORDER) {
            BlockPos target=pos.relative(direction);
            if(world.getChunkSource().getChunkNow(target.getX()>>4,target.getZ()>>4)==null) return; // Unknown does not permanently disable a valid later-loaded core.
        }
        tile.initialized=true;
        for(Direction direction:ORDER) {
            var candidate=world.getBlockEntity(pos.relative(direction));
            if(candidate instanceof TileAlchemyFurnaceAdvanced core && tile.belongsTo(core)) { tile.furnace=core; tile.facing=direction.getOpposite(); break; }
        }
    }
    private boolean current() {
        if(!(level instanceof ServerLevel world) || isRemoved() || !world.getServer().isSameThread()) return false;
        var chunk=world.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4); BlockState state=getBlockState();
        return chunk!=null && chunk.getBlockEntity(worldPosition)==this && world.getBlockState(worldPosition)==state
                && state.is(ThaumcraftAdvancedFurnace.FURNACE.get()) && state.getValue(BlockAdvancedAlchemyFurnace.PART)==1 && !isRemoved();
    }
    /** Callback-bearing endpoint preflight; raw snapshots/matches below never query world state. */
    public boolean nativeCoreCurrent() { return furnace!=null && facing!=null && belongsTo(furnace) && current() && furnace.nozzleCurrent(this)
            && furnace.getBlockPos().equals(worldPosition.relative(facing.getOpposite())) && !isRemoved(); }
    @Override public AspectList getAspects() { return nativeCoreCurrent()?furnace.getAspects():new AspectList(); }
    @Override public void setAspects(AspectList values) {}
    @Override public int addToContainer(Aspect aspect,int amount) { return amount; }
    @Override public boolean takeFromContainer(Aspect aspect,int amount) {
        if(!EssentiaAtomicEndpoint.validAspect(aspect) || amount<=0 || EssentiaTransfers.isLocked(this) || furnace!=null && EssentiaTransfers.isLocked(furnace) || !nativeCoreCurrent()) return false;
        var attempt=EssentiaTransfers.changeAdvancedFurnaceNozzle(this,aspect,amount);
        if(attempt.failure()!=null) thaumcraft.common.Thaumcraft.LOG.warn("Advanced nozzle extraction failed at {} (committed {})",worldPosition,attempt.committedUnits(),attempt.failure());
        return attempt.committedUnits()==amount;
    }
    @Override public boolean doesContainerContainAmount(Aspect aspect,int amount) { return amount>=0 && nativeCoreCurrent() && furnace.getAspects().getAmount(aspect)>=amount; }
    @Override public boolean doesContainerContain(AspectList values) { return false; }
    @Override public boolean takeFromContainer(AspectList values) { return false; }
    @Override public int containerContains(Aspect aspect) { return nativeCoreCurrent()?furnace.getAspects().getAmount(aspect):0; }
    @Override public boolean doesContainerAccept(Aspect aspect) { return false; }
    @Override public boolean isConnectable(Direction face) { return face!=null && face==facing && nativeCoreCurrent(); }
    @Override public boolean canInputFrom(Direction face) { return false; }
    @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
    @Override public void setSuction(Aspect aspect,int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getSuctionAmount(Direction face) { return 0; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public boolean renderExtendedTube() { return false; }
    @Override public Aspect getEssentiaType(Direction face) {
        if(!nativeCoreCurrent()) return null;
        var stock=furnace.stockIdentity(); return stock.isEmpty()?null:stock.keySet().iterator().next();
    }
    @Override public int getEssentiaAmount(Direction face) {
        Aspect aspect=getEssentiaType(face); return aspect==null?0:furnace.stockIdentity().getOrDefault(aspect,0);
    }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { return canOutputTo(face) && takeFromContainer(aspect,amount)?amount:0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    public int comparator() { return nativeCoreCurrent()?AdvancedFurnaceRules.comparator(furnace.getVis()):0; }
    private record NozzleState(TileAlchemyFurnaceAdvanced core,Map<Aspect,Integer> stock,Direction facing,boolean initialized) implements State {}
    @Override public State snapshotAtomicState() { return new NozzleState(furnace,furnace==null?Map.of():furnace.stockIdentity(),facing,initialized); }
    @Override public boolean matchesAtomicState(State before) {
        return before instanceof NozzleState saved && furnace==saved.core && facing==saved.facing && initialized==saved.initialized
                && furnace!=null && !furnace.isRemoved() && furnace.getLevel()==level && furnace.matchesNozzleStock(saved.stock) && !isRemoved();
    }
    @Override public boolean preflightTake(Aspect aspect,Direction outputFace) { return nativeCoreCurrent(); }
    @Override public Change prepareExtract(State before,Aspect aspect,int units) {
        if(!(before instanceof NozzleState saved) || saved.core==null || !EssentiaAtomicEndpoint.validAspect(aspect) || units<=0 || saved.stock.getOrDefault(aspect,0)<units) return null;
        var after=new LinkedHashMap<>(saved.stock); int left=after.get(aspect)-units;
        if(left==0) after.remove(aspect); else after.put(aspect,left);
        return new Change(this,before,new NozzleState(saved.core,Collections.unmodifiableMap(after),saved.facing,saved.initialized),units,true);
    }
    @Override public Change prepareInsert(State before,Aspect aspect,int units) { return null; }
    @Override public void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this,change);
        NozzleState before=(NozzleState)change.before(),after=(NozzleState)change.after();
        if(furnace!=before.core || before.core!=after.core) throw new IllegalStateException("Nozzle lost exact core identity");
        furnace.applyNozzleStock(this,change,before.stock,after.stock);
    }
    @Override public void notifyEssentiaStorageChanged() {
        if(notifying) return; notifying=true;
        try { if(nativeCoreCurrent()) furnace.notifyNozzleChanged(); }
        finally { notifying=false; }
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        if(EssentiaTransfers.isLocked(this) || furnace!=null && EssentiaTransfers.isLocked(furnace)) throw new IllegalStateException("Nozzle cannot reload through an active core debit");
        super.loadAdditional(tag,registries); initialized=false; facing=null; furnace=null;
    }
}
