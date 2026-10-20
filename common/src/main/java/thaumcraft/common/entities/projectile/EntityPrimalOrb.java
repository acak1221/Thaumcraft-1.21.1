package thaumcraft.common.entities.projectile;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftPrimalFoci;
import thaumcraft.common.lib.primal.PrimalFocusRules;
import thaumcraft.common.lib.primal.PrimalOrbWorldEffects;

/** Distinct original chaotic orb, including squared-distance Seeker and the zero-coordinate entity-impact node quirk. */
public final class EntityPrimalOrb extends Projectile {
    private static final EntityDataAccessor<Boolean> SEEKER=SynchedEntityData.defineId(EntityPrimalOrb.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ORIGINAL_CASTER=SynchedEntityData.defineId(EntityPrimalOrb.class,EntityDataSerializers.INT);
    private static Consumer<EntityPrimalOrb> clientTrail=ignored->{},clientImpact=ignored->{};
    public static final byte IMPACT_EVENT=65;
    private int count;
    private boolean impacting;
    public EntityPrimalOrb(EntityType<? extends EntityPrimalOrb> type,Level level){super(type,level);}
    public EntityPrimalOrb(Level level){this(ThaumcraftPrimalFoci.PRIMAL_ORB.get(),level);}
    public EntityPrimalOrb(Level level,LivingEntity owner,boolean seeker){
        this(level);setOwner(owner);entityData.set(SEEKER,seeker);entityData.set(ORIGINAL_CASTER,owner.getId());
        float yaw=owner.getYRot()/180F*(float)Math.PI,pitch=owner.getXRot()/180F*(float)Math.PI;
        setPos(owner.getX()-(double)(Mth.cos(yaw)*.16F),owner.getEyeY()-(double).1F,owner.getZ()-(double)(Mth.sin(yaw)*.16F));
        shoot(-Mth.sin(yaw)*Mth.cos(pitch)*.4F,-Mth.sin(pitch)*.4F,Mth.cos(yaw)*Mth.cos(pitch)*.4F,.5F,1F);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(SEEKER,false);builder.define(ORIGINAL_CASTER,0);}
    public boolean isSeeker(){return entityData.get(SEEKER);}
    public int originalCasterId(){return entityData.get(ORIGINAL_CASTER);}
    public int getAge(){return count;}
    @Override public float getPickRadius(){return .1F;}
    @Override protected double getDefaultGravity(){return .001F;}
    @Override public Vec3 getMovementToShoot(double x,double y,double z,float speed,float scatter){
        float length=(float)Math.sqrt(x*x+y*y+z*z);
        if(!Float.isFinite(length)||length<=1e-12F)return Vec3.ZERO;
        return new Vec3((x/length+random.nextGaussian()*.007499999832361937*scatter)*speed,
                (y/length+random.nextGaussian()*.007499999832361937*scatter)*speed,
                (z/length+random.nextGaussian()*.007499999832361937*scatter)*speed);
    }
    @Override protected boolean canHitEntity(Entity target){return !level().isClientSide&&target.isPickable()&&!target.isSpectator()&&(target!=getOwner()||count>=5);}
    @Override public void tick(){
        if(isRemoved())return;
        ++count;
        if(!level().isClientSide&&eyesInOriginalPortal()){onHit(new EntityHitResult(this));if(isRemoved())return;}
        if(level().isClientSide)clientTrail.accept(this);
        if(count>20){
            if(!isSeeker()){
                var jitter=PrimalFocusRules.jitter(getId(),count);setDeltaMovement(getDeltaMovement().add(jitter.x(),jitter.y(),jitter.z()));
            }else{
                LivingEntity nearest=null;double distance=Double.MAX_VALUE;
                for(var living:level().getEntitiesOfClass(LivingEntity.class,new AABB(getX(),getY(),getZ(),getX(),getY(),getZ()).inflate(16))){
                    if(living.getId()==originalCasterId()||living.isRemoved())continue;
                    double candidate=distanceToSqr(living);if(candidate<distance){distance=candidate;nearest=living;}
                }
                if(nearest!=null){
                    Vec3 motion=getDeltaMovement();setDeltaMovement(
                            PrimalFocusRules.seekerAxis(motion.x,nearest.getX()-getX(),distance),
                            PrimalFocusRules.seekerAxis(motion.y,nearest.getBoundingBox().minY+nearest.getBbHeight()*.9D-getY(),distance),
                            PrimalFocusRules.seekerAxis(motion.z,nearest.getZ()-getZ(),distance));
                }
            }
        }
        // Exact squared-distance arithmetic is retained. A zero-distance original NaN terminates instead of corrupting native entity/chunk coordinates.
        if(!Double.isFinite(getDeltaMovement().lengthSqr())){discard();return;}
        setOldPosAndRot();super.tick();if(isRemoved())return;
        HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        if(!level().isClientSide&&hit.getType()!=HitResult.Type.MISS)onHit(hit);
        if(isRemoved())return;
        checkInsideBlocks();if(isRemoved())return;
        Vec3 motion=getDeltaMovement(),next=position().add(motion);float drag=.99F;
        if(isInWater()){
            drag=.8F;
            for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,next.x-motion.x*.25,next.y-motion.y*.25,next.z-motion.z*.25,motion.x,motion.y,motion.z);
        }
        setDeltaMovement(motion.scale(drag));applyGravity();setPos(next);updateRotation();if(count>5000)discard();
    }
    /** Original Entity.isInsideOfMaterial(portal), including the old metadata/liquid-height calculation and Entity eyeheight0. */
    public boolean eyesInOriginalPortal(){
        BlockPos pos=BlockPos.containing(getX(),getEyeY(),getZ());
        if(level() instanceof ServerLevel server&&!EntityShockOrb.loaded(server,pos))return false;
        var state=level().getBlockState(pos);int metadata;
        if(state.is(Blocks.NETHER_PORTAL))metadata=state.getValue(NetherPortalBlock.AXIS)==net.minecraft.core.Direction.Axis.X?1:2;
        else if(state.is(Blocks.END_PORTAL))metadata=0;else return false;
        float percent=(metadata+1)/9F-.11111111F;
        return getEyeY()<(double)((float)(pos.getY()+1)-percent);
    }
    @Override protected void onHit(HitResult hit){
        if(!(level() instanceof ServerLevel server)||isRemoved()||impacting||hit.getType()==HitResult.Type.MISS)return;
        impacting=true;
        try{
            boolean portalBlock=hit.getType()==HitResult.Type.BLOCK&&eyesInOriginalPortal();
            server.broadcastEntityEvent(this,IMPACT_EVENT);
            server.explode(null,getX(),getY(),getZ(),portalBlock?4F:2F,false,Level.ExplosionInteraction.BLOCK);
            if(!isSeeker()&&PrimalFocusRules.special(random.nextInt(100),portalBlock)){
                if(random.nextBoolean())PrimalOrbWorldEffects.taint(this,server,random);
                else PrimalOrbWorldEffects.smallNode(server,hit instanceof BlockHitResult block?block.getBlockPos():BlockPos.ZERO,random);
            }
        }finally{discard();impacting=false;}
    }
    public static void setClientTrail(Consumer<EntityPrimalOrb> handler){clientTrail=Objects.requireNonNull(handler);}
    public static void setClientImpact(Consumer<EntityPrimalOrb> handler){clientImpact=Objects.requireNonNull(handler);}
    @Override public void handleEntityEvent(byte event){if(event==IMPACT_EVENT){if(level().isClientSide)clientImpact.accept(this);}else super.handleEntityEvent(event);}
    // Original count/seeker/oi are spawn-only, NOT custom NBT. Native Projectile persists the owner's UUID separately.
}
