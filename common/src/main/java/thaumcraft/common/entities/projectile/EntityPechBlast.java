package thaumcraft.common.entities.projectile;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.entities.monster.EntityPech;

/** TC4 curse projectile: splash affects every living non-Pech except its owner,
 * including targets whose damage attempt refuses. Current voxel collision and
 * native damage/protection rules replace the old vanilla engine. Server impact
 * events alone emit the client burst. Reload retains the original NON-persisted
 * strength/duration/nightshade defaults, but age is saved to prevent lifetime
 * rejuvenation. A terminal projectile returns instead of moving after removal. */
public final class EntityPechBlast extends Projectile {
    public static final float LAUNCH_SPEED=1.5F;
    public static final byte IMPACT_EVENT=63;
    private static Consumer<EntityPechBlast> clientTrail=ignored->{};
    private static Consumer<EntityPechBlast> clientImpact=ignored->{};
    private int strength,duration,age;
    private boolean nightshade;
    private Vec3 previousTrailPosition;

    public EntityPechBlast(EntityType<? extends EntityPechBlast> type,Level level) {super(type,level);}
    public EntityPechBlast(Level level) {this(ThaumcraftPech.PECH_BLAST.get(),level);}
    public EntityPechBlast(Level level,LivingEntity owner,int strength,int duration,boolean nightshade) {
        this(level);configure(strength,duration,nightshade);setOwner(owner);
        float yaw=owner.getYRot()/180*(float)Math.PI,pitch=owner.getXRot()/180*(float)Math.PI;
        setPos(owner.getX()-(double)(Mth.cos(yaw)*.16F),owner.getEyeY()-(double).1F,owner.getZ()-(double)(Mth.sin(yaw)*.16F));
        // EntityThrowable performs exactly ONE scatter1 launch (three Gaussians).
        shoot(-Mth.sin(yaw)*Mth.cos(pitch)*.4F,-Mth.sin(pitch)*.4F,Mth.cos(yaw)*Mth.cos(pitch)*.4F,LAUNCH_SPEED,1);
    }
    public EntityPechBlast(Level level,double x,double y,double z,int strength,int duration,boolean nightshade) {
        this(level);setPos(x,y,z);configure(strength,duration,nightshade);
    }
    private void configure(int strength,int duration,boolean nightshade) {
        if(strength<0||strength>5||duration<0||duration>5)throw new IllegalArgumentException("Invalid curse power");
        this.strength=strength;this.duration=duration;this.nightshade=nightshade;
    }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    public int getStrength(){return strength;}
    public int getDurationLevel(){return duration;}
    public boolean isNightshade(){return nightshade;}
    public int getAge(){return age;}
    public Vec3 previousTrailPosition(){return previousTrailPosition==null?position():previousTrailPosition;}
    @Override protected double getDefaultGravity(){return .025F;}
    @Override public boolean canUsePortal(boolean allowPassengers){return true;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){double range=getBoundingBox().getSize()*4*64;return distance<range*range;}
    @Override public Vec3 getMovementToShoot(double x,double y,double z,float speed,float scatter){
        float length=(float)Math.sqrt(x*x+y*y+z*z);
        if(!Float.isFinite(length)||length<=1e-12F||!Float.isFinite(speed)||!Float.isFinite(scatter))return Vec3.ZERO;
        return new Vec3((x/length+random.nextGaussian()*.007499999832361937*scatter)*speed,
                (y/length+random.nextGaussian()*.007499999832361937*scatter)*speed,
                (z/length+random.nextGaussian()*.007499999832361937*scatter)*speed);
    }
    @Override public void tick(){
        if(isRemoved())return;
        if(level().isClientSide)clientTrail.accept(this); // Original BEFORE EntityThrowable.tick.
        previousTrailPosition=position();setOldPosAndRot();setDeltaMovement(finite(getDeltaMovement()));
        if(age<Integer.MAX_VALUE)++age;
        super.tick();if(isRemoved())return;
        HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        if(!level().isClientSide&&hit.getType()!=HitResult.Type.MISS)onHit(hit);
        if(isRemoved())return;
        checkInsideBlocks();if(isRemoved())return;
        Vec3 motion=finite(getDeltaMovement()),next=position().add(motion);float drag=.99F;
        if(isInWater()){
            drag=.8F;
            for(int i=0;i<4;i++)level().addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE,next.x-motion.x*.25,next.y-motion.y*.25,next.z-motion.z*.25,motion.x,motion.y,motion.z);
        }
        setDeltaMovement(motion.scale(drag));applyGravity();setPos(next);updateRotation();
        if(age>500)discard();
    }
    @Override protected void updateRotation(){
        Vec3 motion=getDeltaMovement();float horizontal=(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z);
        float yaw=(float)(Math.atan2(motion.x,motion.z)*180/Math.PI),pitch=(float)(Math.atan2(motion.y,horizontal)*180/Math.PI);
        while(pitch-xRotO< -180)xRotO-=360;while(pitch-xRotO>=180)xRotO+=360;
        while(yaw-yRotO< -180)yRotO-=360;while(yaw-yRotO>=180)yRotO+=360;
        setXRot(xRotO+(pitch-xRotO)*.2F);setYRot(yRotO+(yaw-yRotO)*.2F);
    }
    @Override protected boolean canHitEntity(Entity entity){return !level().isClientSide&&!entity.isSpectator()&&entity.isPickable()&&(entity!=getOwner()||age>=5);}
    @Override protected void onHit(HitResult hit){
        if(level().isClientSide||isRemoved()||hit.getType()==HitResult.Type.MISS)return;
        // Current projectile position, not ray contact: exactly original splash box.
        for(Entity target:level().getEntities(getOwner(),getBoundingBox().inflate(2))){
            if(target instanceof EntityPech||!(target instanceof LivingEntity living))continue;
            living.hurt(damageSources().thrown(this,getOwner()),strength+2);
            int selected=nightshade?-1:random.nextInt(3);
            if(selected==-1||selected==0)living.addEffect(new MobEffectInstance(MobEffects.POISON,100+duration*40,strength));
            if(selected==-1||selected==1)living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100+duration*40,strength+1));
            if(selected==-1||selected==2)living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100+duration*40,strength));
        }
        level().broadcastEntityEvent(this,IMPACT_EVENT);discard();
    }
    private static Vec3 finite(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)&&Float.isFinite((float)Math.sqrt(v.lengthSqr()))?v:Vec3.ZERO;}
    public static void setClientTrail(Consumer<EntityPechBlast> handler){clientTrail=Objects.requireNonNull(handler);}
    public static void setClientImpact(Consumer<EntityPechBlast> handler){clientImpact=Objects.requireNonNull(handler);}
    @Override public void handleEntityEvent(byte event){if(event==IMPACT_EVENT){if(level().isClientSide)clientImpact.accept(this);}else super.handleEntityEvent(event);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("PechBlastAge",age);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);age=Math.max(0,tag.getInt("PechBlastAge"));strength=duration=0;nightshade=false;}
}
