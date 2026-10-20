package thaumcraft.common.entities.projectile;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftShockOrbs;
import thaumcraft.common.blocks.BlockShockTrap;

/** Original Earth Shock orb: point-AABB blast (including caster),20 triangular field attempts and four owner-grace air ticks. */
public final class EntityShockOrb extends Projectile {
    private static final EntityDataAccessor<Integer> AREA=SynchedEntityData.defineId(EntityShockOrb.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DAMAGE=SynchedEntityData.defineId(EntityShockOrb.class,EntityDataSerializers.INT);
    private static Consumer<EntityShockOrb> clientImpact;
    private int age;
    public EntityShockOrb(EntityType<? extends EntityShockOrb> type,Level level) { super(type,level); }
    public EntityShockOrb(Level level,LivingEntity owner) {
        this(ThaumcraftShockOrbs.SHOCK_ORB.get(),level); setOwner(owner);
        float yaw=owner.getYRot()/180F*(float)Math.PI,pitch=owner.getXRot()/180F*(float)Math.PI;
        setPos(owner.getX()-(double)(Mth.cos(yaw)*.16F),owner.getEyeY()-(double).1F,owner.getZ()-(double)(Mth.sin(yaw)*.16F));
        shoot(-Mth.sin(yaw)*Mth.cos(pitch)*.4F,-Mth.sin(pitch)*.4F,Mth.cos(yaw)*Mth.cos(pitch)*.4F,1.5F,1F);
    }
    public static void setClientImpact(Consumer<EntityShockOrb> effect) { clientImpact=java.util.Objects.requireNonNull(effect); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(AREA,4);builder.define(DAMAGE,5); }
    public void configure(int area,int damage) {
        if(area<=0||damage<0) throw new IllegalArgumentException("Invalid shock orb parameters");
        entityData.set(AREA,area);entityData.set(DAMAGE,damage);
    }
    public int getArea() { return entityData.get(AREA); }
    public int getDamage() { return entityData.get(DAMAGE); }
    public int getAge() { return age; }
    @Override public float getPickRadius() { return .1F; }
    @Override protected double getDefaultGravity() { return .05F; }
    @Override public Vec3 getMovementToShoot(double x,double y,double z,float speed,float scatter) {
        float length=(float)Math.sqrt(x*x+y*y+z*z);
        if(!Float.isFinite(length)||length<=1e-12F) return Vec3.ZERO;
        return new Vec3((x/length+random.nextGaussian()*.007499999832361937*scatter)*speed,
                (y/length+random.nextGaussian()*.007499999832361937*scatter)*speed,
                (z/length+random.nextGaussian()*.007499999832361937*scatter)*speed);
    }
    @Override protected boolean canHitEntity(Entity entity) {
        return !level().isClientSide && entity.isPickable() && !entity.isSpectator() && (entity!=getOwner()||age>=5);
    }
    @Override public void tick() {
        if(isRemoved()) return;
        setOldPosAndRot(); ++age; super.tick();
        Vec3 motion=getDeltaMovement();
        if(!Double.isFinite(motion.lengthSqr())) { discard();return; }
        HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        if(!level().isClientSide && hit.getType()!=HitResult.Type.MISS) onHit(hit);
        if(isRemoved()) return;
        checkInsideBlocks();
        if(isRemoved()) return;
        Vec3 next=position().add(getDeltaMovement()); float drag=.99F;
        if(isInWater()) {
            drag=.8F;
            for(int i=0;i<4;i++) level().addParticle(ParticleTypes.BUBBLE,next.x-motion.x*.25,next.y-motion.y*.25,next.z-motion.z*.25,motion.x,motion.y,motion.z);
        }
        setDeltaMovement(getDeltaMovement().scale(drag));applyGravity();setPos(next);updateRotation();
        if(age>500) discard();
    }
    @Override protected void onHit(HitResult hit) {
        if(!(level() instanceof ServerLevel server)||isRemoved()) return;
        double x=getX(),y=getY(),z=getZ(); int area=getArea();
        // Original EntityUtils range is an inflated POINT AABB, not a sphere or the orb's own inflated box.
        for(Entity victim:server.getEntities(this,new AABB(x,y,z,x,y,z).inflate(area))) {
            if(!visible(server,position(),victim.position())) continue;
            victim.hurt(damageSources().indirectMagic(this,getOwner()),getDamage());
            if(isRemoved()) return;
        }
        int baseY=Mth.floor(y);
        for(int attempt=0;attempt<20;attempt++) {
            long xx=(long)Mth.floor(x)+random.nextInt(area)-random.nextInt(area);
            long zz=(long)Mth.floor(z)+random.nextInt(area)-random.nextInt(area);
            if(xx<Integer.MIN_VALUE||xx>Integer.MAX_VALUE||zz<Integer.MIN_VALUE||zz>Integer.MAX_VALUE) continue;
            int yy=(int)Math.min(server.getMaxBuildHeight()-1L,(long)baseY+area);
            long bottom=(long)baseY-area; boolean readable=true;
            BlockPos pos=new BlockPos((int)xx,yy,(int)zz);
            while(yy>bottom) {
                pos=new BlockPos((int)xx,yy,(int)zz);
                if(!loaded(server,pos)) { readable=false;break; }
                if(!BlockShockTrap.semanticAir(server.getBlockState(pos))) break;
                --yy;
            }
            pos=new BlockPos((int)xx,yy,(int)zz); BlockPos above=pos.above();
            if(!readable||!loaded(server,pos)||!loaded(server,above)) continue;
            BlockState ground=server.getBlockState(pos),target=server.getBlockState(above);
            if(BlockShockTrap.semanticAir(ground)||!BlockShockTrap.semanticAir(target)||BlockShockTrap.originalAiry(target)) continue;
            // Original coordinate overload traces from candidate centreY+1.5 TOWARDS the orb base.
            if(!visible(server,new Vec3(xx+.5,yy+1.5,zz+.5),position())) continue;
            server.setBlock(above,ThaumcraftShockOrbs.SHOCK_TRAP.get().defaultBlockState(),Block.UPDATE_ALL);
        }
        server.broadcastEntityEvent(this,(byte)61);
        server.playSound(null,x,y,z,SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","shock")),SoundSource.PLAYERS,1,1+(random.nextFloat()-random.nextFloat())*.2F);
        discard();
    }
    public static boolean loaded(ServerLevel level,BlockPos pos) {
        return level.isInWorldBounds(pos)&&level.getWorldBorder().isWithinBounds(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private static boolean visible(ServerLevel level,Vec3 from,Vec3 to) {
        BlockGetter view=new BlockGetter() {
            @Override public BlockState getBlockState(BlockPos pos) { if(!loaded(level,pos)) throw Unloaded.INSTANCE;return level.getBlockState(pos); }
            @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
            @Override public BlockEntity getBlockEntity(BlockPos pos) { return loaded(level,pos)?level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4).getBlockEntities().get(pos):null; }
            @Override public int getHeight() { return level.getHeight(); }
            @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
        };
        try { return view.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()==HitResult.Type.MISS; }
        catch(Unloaded unavailable) { return false; }
    }
    private static final class Unloaded extends RuntimeException {
        static final Unloaded INSTANCE=new Unloaded();private Unloaded() { super(null,null,false,false); }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isInvulnerableTo(source)) return false;
        markHurt(); Entity attacker=source.getEntity();
        if(attacker==null) return false;
        setDeltaMovement(attacker.getLookAngle().scale(.9));
        playSound(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","zap")),1,1+(random.nextFloat()-random.nextFloat())*.2F);
        return true; // Original caster ownership remains unchanged after deflection.
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==61) { if(level().isClientSide&&clientImpact!=null)clientImpact.accept(this); }
        else super.handleEntityEvent(event);
    }
}
