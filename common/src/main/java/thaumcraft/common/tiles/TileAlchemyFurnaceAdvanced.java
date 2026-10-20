package thaumcraft.common.tiles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.lib.AdvancedFurnaceFormation;
import thaumcraft.common.lib.AdvancedFurnaceRules;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Original500 mixed slurry and three independent raw-CV pools; dropped items are the only input. */
public final class TileAlchemyFurnaceAdvanced extends TileAlchemyFurnaceAdvancedPart {
    private static final SoundEvent BUBBLE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","bubble"));
    private Map<Aspect,Integer> stock = Map.of();
    private int heat, power1, power2, count, processed, clientVis;
    private boolean destroy, ticking, processing, notifying;
    private CompoundTag rejected;
    private PowerDraw draw;
    private static final class PowerDraw {
        final Aspect aspect; final int before; int paid;
        PowerDraw(Aspect aspect,int before) { this.aspect=aspect; this.before=before; }
    }
    public TileAlchemyFurnaceAdvanced(BlockPos pos, BlockState state) { super(ThaumcraftAdvancedFurnace.TILE.get(),pos,state); }
    public AspectList getAspects() { var copy=new AspectList(); stock.forEach(copy::add); return copy; }
    public int getVis() { return level!=null && level.isClientSide?clientVis:total(stock); }
    public int getMaxVis() { return AdvancedFurnaceRules.MAX_VIS; }
    public int getHeat() { return heat; }
    public int getPower1() { return power1; }
    public int getPower2() { return power2; }
    public int getTickCount() { return count; }
    public int getProcessed() { return processed; }
    public boolean isQuarantined() { return rejected!=null; }
    public boolean isDestroyPending() { return destroy; }
    public boolean isBusy() { return ticking || processing || notifying || EssentiaTransfers.isLocked(this); }
    public boolean isCurrent() {
        if (!(level instanceof ServerLevel world) || isRemoved() || !world.getServer().isSameThread() || world.getServer().getLevel(world.dimension())!=world || !hasFormationIdentity() || !worldPosition.equals(ownerPosition())) return false;
        var chunk=world.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        BlockState state=getBlockState();
        return chunk!=null && chunk.getBlockEntity(worldPosition)==this && world.getBlockState(worldPosition)==state
                && state.is(ThaumcraftAdvancedFurnace.FURNACE.get()) && state.getValue(BlockAdvancedAlchemyFurnace.PART)==0
                && !isRemoved() && level==world && getBlockState()==state;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,TileAlchemyFurnaceAdvanced tile) {
        if (!(level instanceof ServerLevel world) || tile.level!=world || !tile.worldPosition.equals(pos)
                || AdvancedFurnaceFormation.isMutationActive() || tile.ticking || tile.processing || tile.notifying || tile.isQuarantined() || EssentiaTransfers.isLocked(tile)) return;
        tile.ticking=true;
        try {
            if (!tile.isCurrent()) return;
            var status=AdvancedFurnaceFormation.structure(tile);
            if (status==AdvancedFurnaceFormation.Status.UNLOADED) return;
            if (tile.destroy || status==AdvancedFurnaceFormation.Status.INCOMPLETE) { tile.destroy=true; AdvancedFurnaceFormation.disassemble(tile,false); return; }
            tile.count++;
            if(tile.processed>0) tile.processed--;
            if(tile.count%5==0) {
                int before=tile.heat; tile.heat--;
                if(tile.heat<=500) tile.drawPower(Aspect.FIRE);
                if(!tile.isCurrent() || tile.destroy) return;
                if(tile.power1<=500) tile.drawPower(Aspect.ENTROPY);
                if(!tile.isCurrent() || tile.destroy) return;
                if(tile.power2<=500) tile.drawPower(Aspect.WATER);
                if(tile.isCurrent()) { world.blockEntityChanged(pos); if(before/50!=tile.heat/50) tile.notifyDisplay(); else tile.updateLight(); }
            }
        } finally { tile.draw=null; tile.ticking=false; }
    }
    private void drawPower(Aspect aspect) {
        if (!isCurrent() || destroy || draw!=null || AdvancedFurnaceFormation.structure(this)!=AdvancedFurnaceFormation.Status.COMPLETE) return;
        draw=new PowerDraw(aspect,power(aspect));
        try { VisNetHandler.drainVis(level,worldPosition,aspect,50); }
        finally { draw=null; }
    }
    public boolean hasPreparedPowerGrant(Aspect aspect) { return ticking && !processing && !notifying && draw!=null && draw.aspect==aspect && !destroy && rejected==null; }
    /** Callback-bearing pre-debit observation; executor rechecks before every source debit. */
    public boolean readyForPowerGrant(Aspect aspect,int remaining) {
        PowerDraw prepared=draw;
        return hasPreparedPowerGrant(aspect) && prepared!=null && remaining>0 && remaining<=50-prepared.paid
                && !EssentiaTransfers.isLocked(this) && isCurrent()
                && AdvancedFurnaceFormation.structure(this)==AdvancedFurnaceFormation.Status.COMPLETE
                && draw==prepared && power(aspect)==prepared.before+prepared.paid && !destroy && rejected==null;
    }
    /** Native executor's exact committed receipt is consumed before pulse/FX callbacks. No world reads. */
    public void acceptPreparedPowerGrant(Aspect aspect,int units) {
        VisNetHandler.requireAdvancedFurnaceGrant(this,aspect,units);
        PowerDraw prepared=draw;
        if(!hasPreparedPowerGrant(aspect) || units<=0 || units>50-prepared.paid || power(aspect)!=prepared.before+prepared.paid)
            throw new IllegalStateException("Advanced furnace lost its exact prepared power state");
        if(aspect==Aspect.FIRE) heat+=units; else if(aspect==Aspect.ENTROPY) power1+=units; else if(aspect==Aspect.WATER) power2+=units;
        else throw new IllegalStateException("Unsupported advanced furnace power");
        prepared.paid+=units;
    }
    private int power(Aspect aspect) { return aspect==Aspect.FIRE?heat:aspect==Aspect.ENTROPY?power1:power2; }
    public void requestDestroy() { if(level instanceof ServerLevel && !isRemoved()) { destroy=true; setChanged(); } }

    public void collide(ItemEntity entity) {
        if(!(level instanceof ServerLevel world) || processing || ticking || notifying || EssentiaTransfers.isLocked(this)
                || rejected!=null || destroy || processed!=0 || entity==null || entity.isRemoved() || entity.level()!=world) return;
        processing=true;
        try {
            if(!isCurrent() || world.getEntity(entity.getUUID())!=entity
                    || AdvancedFurnaceFormation.structure(this)!=AdvancedFurnaceFormation.Status.COMPLETE) return;
            ItemStack input=entity.getItem(); ItemStack before=input.copy();
            if(before.isEmpty()) return;
            AspectList tags=ObjectAspectResolver.get(before,world);
            long amount=0;
            for(Aspect aspect:tags.getAspects()) { int units=tags.getAmount(aspect); if(!EssentiaAtomicEndpoint.validAspect(aspect) || units<0) return; amount+=units; }
            if(amount<=0 || amount>500-getVis() || heat<amount*2 || power1<amount || power2<amount) return;
            Map<Aspect,Integer> oldStock=stock;
            var added=new LinkedHashMap<>(oldStock); for(Aspect aspect:tags.getAspects()) added.merge(aspect,tags.getAmount(aspect),Integer::sum);
            Map<Aspect,Integer> after=Collections.unmodifiableMap(added);
            ItemStack remainder=before.copy(); remainder.shrink(1);
            int oldHeat=heat, oldPower1=power1, oldPower2=power2;
            if(!isCurrent() || AdvancedFurnaceFormation.structure(this)!=AdvancedFurnaceFormation.Status.COMPLETE
                    || entity.isRemoved() || world.getEntity(entity.getUUID())!=entity || entity.getItem()!=input
                    || !ItemStack.matches(input,before) || stock!=oldStock || heat!=oldHeat || power1!=oldPower1 || power2!=oldPower2 || processed!=0 || destroy) return;
            // Callback-free physical input debit and preallocated stock/energy/cooldown commit.
            input.shrink(1); stock=after; heat-=((int)amount)*2; power1-=(int)amount; power2-=(int)amount; processed=AdvancedFurnaceRules.cooldown(heat);
            if(remainder.isEmpty()) entity.discard(); else entity.setItem(remainder);
            setChanged(); notifyDisplay();
            world.playSound(null,entity.getX(),entity.getY(),entity.getZ(),BUBBLE,SoundSource.BLOCKS,.2F,1F+world.random.nextFloat()*.4F);
        } finally { processing=false; }
    }
    Map<Aspect,Integer> stockIdentity() { return stock; }
    boolean matchesNozzleStock(Map<Aspect,Integer> before) { return stock==before && rejected==null && !destroy && !processing; }
    void applyNozzleStock(TileAlchemyFurnaceAdvancedNozzle nozzle,EssentiaAtomicEndpoint.Change change,Map<Aspect,Integer> before,Map<Aspect,Integer> after) {
        EssentiaTransfers.requireAtomicChange(nozzle,change);
        if(!matchesNozzleStock(before) || !EssentiaTransfers.isLocked(this)) throw new IllegalStateException("Nozzle requires the locked exact core pool");
        stock=after;
    }
    public boolean nozzleCurrent(TileAlchemyFurnaceAdvancedNozzle nozzle) {
        return !destroy && rejected==null && !processing && !ticking && isCurrent()
                && AdvancedFurnaceFormation.structure(this)==AdvancedFurnaceFormation.Status.COMPLETE
                && nozzle.getLevel()==level;
    }
    public void notifyNozzleChanged() { if(isCurrent()) { setChanged(); notifyDisplay(); } }
    private void notifyDisplay() {
        if(notifying) return;
        notifying=true;
        try {
            if(!isCurrent()) return;
            updateLight();
            if(isCurrent()) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
            if(isCurrent()) for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos pos=worldPosition.relative(direction);
                level.updateNeighbourForOutputSignal(pos,level.getBlockState(pos).getBlock());
            }
        } finally { notifying=false; }
    }
    private void updateLight() {
        if(!isCurrent()) return;
        BlockState state=getBlockState(); int light=AdvancedFurnaceRules.light(heat);
        if(state.getValue(BlockAdvancedAlchemyFurnace.LIGHT)!=light) level.setBlock(worldPosition,state.setValue(BlockAdvancedAlchemyFurnace.LIGHT,light),Block.UPDATE_CLIENTS);
    }
    private static int total(Map<Aspect,Integer> values) { int total=0; for(int units:values.values()) total+=units; return total; }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(rejected!=null) { tag.put("RejectedAdvancedFurnace",rejected.copy()); return; }
        getAspects().writeToNBT(tag); tag.putShort("vis",(short)getVis()); tag.putShort("heat",(short)heat);
        tag.putShort("power1",(short)power1); tag.putShort("power2",(short)power2);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        if(isBusy() || VisNetHandler.isConsumeActive(level)) throw new IllegalStateException("Advanced furnace cannot reload during active resource mutation");
        if(level!=null && level.isClientSide) { clientVis=tag.getShort("vis"); heat=tag.getShort("heat"); return; }
        super.loadAdditional(tag,registries);
        rejected=null; stock=Map.of(); heat=power1=power2=count=processed=0; destroy=false; draw=null;
        if(tag.contains("RejectedAdvancedFurnace")) { rejected=tag.getCompound("RejectedAdvancedFurnace").copy(); return; }
        try {
            for(String key:new String[]{"vis","heat","power1","power2"}) if(tag.contains(key) && !tag.contains(key,Tag.TAG_SHORT)) throw new IllegalArgumentException(key);
            var values=new LinkedHashMap<Aspect,Integer>(); long total=0;
            if(tag.contains("Aspects") && !tag.contains("Aspects",Tag.TAG_LIST)) throw new IllegalArgumentException("Aspects");
            for(Tag value:tag.getList("Aspects",Tag.TAG_COMPOUND)) {
                var row=(CompoundTag)value; Aspect aspect=Aspect.getAspect(row.getString("key")); int units=row.getInt("amount");
                if(!EssentiaAtomicEndpoint.validAspect(aspect) || !row.contains("amount",Tag.TAG_INT) || units<0 || values.put(aspect,units)!=null) throw new IllegalArgumentException("stock"); total+=units;
            }
            if(total>500 || tag.contains("vis") && tag.getShort("vis")!=total) throw new IllegalArgumentException("total");
            heat=tag.getShort("heat"); power1=tag.getShort("power1"); power2=tag.getShort("power2");
            if(power1<0 || power1>550 || power2<0 || power2>550) throw new IllegalArgumentException("power");
            stock=Collections.unmodifiableMap(values);
        } catch(RuntimeException malformed) { rejected=tag.copy(); stock=Map.of(); heat=power1=power2=0; }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag=new CompoundTag(); tag.putShort("vis",(short)getVis()); tag.putShort("heat",(short)heat); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
