package thaumcraft.common.tiles;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.lib.AdvancedFurnaceFormation;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Durable identity adapter for original formed metadata. No extra stock or independent machine inventory. */
public sealed class TileAlchemyFurnaceAdvancedPart extends BlockEntity permits TileAlchemyFurnaceAdvanced,TileAlchemyFurnaceAdvancedNozzle {
    private BlockPos corePos;
    private UUID machine;
    private boolean badIdentity,standaloneCreative;
    private int identityTick;
    public TileAlchemyFurnaceAdvancedPart(BlockPos pos,BlockState state) { this(ThaumcraftAdvancedFurnace.PART_TILE.get(),pos,state); }
    protected TileAlchemyFurnaceAdvancedPart(BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state);
        var birth=AdvancedFurnaceFormation.birthIdentity(this);
        if(birth==null) birth=thaumcraft.common.items.ItemAdvancedAlchemyFurnace.birthIdentity(this);
        if(birth!=null) { corePos=birth.center(); machine=birth.machine(); standaloneCreative=birth.standaloneCreative(); }
    }
    public final boolean isStandaloneCreative() { return standaloneCreative; }
    public final UUID machineId() { return machine; }
    public final BlockPos ownerPosition() { return corePos; }
    public final boolean hasFormationIdentity() { return !badIdentity && machine!=null && corePos!=null; }
    public final boolean belongsTo(TileAlchemyFurnaceAdvanced core) { return hasFormationIdentity() && (this==core || !core.isStandaloneCreative()) && corePos.equals(core.getBlockPos()) && machine.equals(core.machineId()); }
    public final boolean identityInstalled() {
        if(!(level instanceof ServerLevel world) || isRemoved() || !hasFormationIdentity()) return false;
        var chunk=world.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4); BlockState state=getBlockState();
        return chunk!=null && chunk.getBlockEntity(worldPosition)==this && world.getBlockState(worldPosition)==state && state.is(ThaumcraftAdvancedFurnace.FURNACE.get()) && !isRemoved();
    }
    /** Pause for unknown owner chunk; exact owner absence/replacement retires only this current physical part. */
    public final boolean observeOwner() {
        if(!(level instanceof ServerLevel world) || !identityInstalled() || AdvancedFurnaceFormation.isMutationActive()) return false;
        var ownerChunk=world.getChunkSource().getChunkNow(corePos.getX()>>4,corePos.getZ()>>4);
        if(ownerChunk==null) return false;
        var owner=ownerChunk.getBlockEntity(corePos);
        if(owner instanceof TileAlchemyFurnaceAdvanced core && belongsTo(core) && !core.isRemoved() && core.isCurrent()) return true;
        BlockState before=getBlockState(); int part=before.getValue(BlockAdvancedAlchemyFurnace.PART);
        if(part!=0 && !EssentiaTransfers.isLocked(this) && identityInstalled()) world.setBlock(worldPosition,AdvancedFurnaceFormation.original(part),AdvancedFurnaceFormation.reversionFlags(world,worldPosition));
        return false;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,TileAlchemyFurnaceAdvancedPart part) {
        if(level.isClientSide || AdvancedFurnaceFormation.isMutationActive() || EssentiaTransfers.isLocked(part)) return;
        if(part.identityTick++%20==0) part.observeOwner();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(hasFormationIdentity()) { tag.putLong("FurnaceCore",corePos.asLong()); tag.putUUID("FurnaceMachine",machine); }
        if(standaloneCreative) tag.putBoolean("StandaloneCreative",true);
        if(badIdentity) tag.putBoolean("RejectedFurnaceIdentity",true);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        if(EssentiaTransfers.isLocked(this)) throw new IllegalStateException("Furnace part cannot reload an active native resource frame");
        super.loadAdditional(tag,registries); corePos=null; machine=null; identityTick=0; standaloneCreative=tag.getBoolean("StandaloneCreative"); badIdentity=tag.getBoolean("RejectedFurnaceIdentity");
        if(tag.contains("FurnaceCore",Tag.TAG_LONG) && tag.hasUUID("FurnaceMachine")) { corePos=BlockPos.of(tag.getLong("FurnaceCore")); machine=tag.getUUID("FurnaceMachine"); }
        else badIdentity=true;
        if(standaloneCreative && (getBlockState().getValue(BlockAdvancedAlchemyFurnace.PART)!=0 || corePos==null || !corePos.equals(worldPosition))) badIdentity=true;
    }
}
