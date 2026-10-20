package thaumcraft.common.entities.monster;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftTaint;

/** Original fixed-size75HP swarmer: counter500 waits for a vulnerable player16,
 * one actual swarm per boundary and one more on deathTime1. Counter resets on
 * reload, like the original absent-NBT field; status6 controls client reset. */
public final class EntityTaintSporeSwarmer extends EntityTaintSpore {
    private int spawnCounter=500;
    private boolean releasing;
    public EntityTaintSporeSwarmer(EntityType<? extends EntityTaintSporeSwarmer> type,Level level){super(type,level);setSporeSize(10);}
    public EntityTaintSporeSwarmer(Level level){this(ThaumcraftTaint.TAINT_SWARMER.get(),level);}
    public static AttributeSupplier.Builder createAttributes(){return EntityTaintSpore.createAttributes().add(Attributes.MAX_HEALTH,75).add(Attributes.ATTACK_DAMAGE,1);}
    @Override protected float physicalSize(){return 1;}
    public int getSpawnCounter(){return spawnCounter;}
    @Override public boolean hurt(DamageSource source,float amount){if(level().isClientSide)TaintSwarmVisuals.sploosh(this,10);return super.hurt(source,amount);}
    @Override protected void sporeOnUpdate(){
        moveTowardsClosestSpace(getX(),getY(),getZ());if(spawnCounter>0)--spawnCounter;
        // Original closest vulnerable-player query is independent of sight.
        if(spawnCounter<=0&&level().getNearestPlayer(getX(),getY(),getZ(),16,entity->entity instanceof Player player&&!player.isSpectator()&&!player.getAbilities().invulnerable)!=null){spawnCounter=500;swarmBurst(1);}
        if(level().isClientSide)TaintSwarmVisuals.tick(this,(500-spawnCounter)/25,.1F,10F,0);
        if(deathTime==1)swarmBurst(1);
    }
    @Override public void playerTouch(Player player){}
    protected void swarmBurst(int count){
        if(!currentServerEntity()||releasing)return;releasing=true;
        try {
            burstSound();
            for(int i=0;i<count;i++){
                var swarm=new EntityTaintSwarm(ThaumcraftTaint.TAINT_SWARM.get(),level());swarm.moveTo(getX(),getY()+.5,getZ(),level().random.nextFloat()*360F,0);level().addFreshEntity(swarm);
            }
            level().broadcastEntityEvent(this,(byte)6);
        }finally{releasing=false;}
    }
    @Override public void handleEntityEvent(byte event){if(event==6){spawnCounter=500;TaintSwarmVisuals.sploosh(this,25);TaintSwarmVisuals.clear(this);}else super.handleEntityEvent(event);}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return sound("roots");}
    @Override protected int taintedDrops(){return 2;}
}
