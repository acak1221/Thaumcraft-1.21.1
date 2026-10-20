package thaumcraft.common.entities.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;

/** Original tiny tainted spider:5HP/2damage/XP2, closest vulnerable player12
 * without the vanilla spider brightness gate, world-RNG one-in-six custom loot. */
public final class EntityTaintSpider extends Spider implements ITaintedMob {
    public EntityTaintSpider(EntityType<? extends EntityTaintSpider> type,Level level){super(type,level);xpReward=2;}
    public EntityTaintSpider(Level level){this(ThaumcraftTaint.TAINT_SPIDER.get(),level);}
    public static AttributeSupplier.Builder createAttributes(){return Spider.createAttributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,12);}
    @Override protected void registerGoals(){
        super.registerGoals();targetSelector.removeAllGoals(goal->true);
        targetSelector.addGoal(1,new OriginalPlayerTarget(this));
    }
    /** mustSee=false only changes native continuation; native acquisition
     * separately defaults to LOS. Original World.closestVulnerable had no LOS. */
    private static final class OriginalPlayerTarget extends NearestAttackableTargetGoal<Player> {
        OriginalPlayerTarget(EntityTaintSpider spider){super(spider,Player.class,0,false,false,null);targetConditions.ignoreLineOfSight();}
    }
    @Override public float getVoicePitch(){return .7F;}
    @Override public float getPickRadius(){return .1F;}
    public float spiderScaleAmount(){return .4F;}
    @Override public void playSound(SoundEvent sound,float volume,float pitch){if(!isSilent())level().playSeededSound(null,getX(),getY(),getZ(),sound,getSoundSource(),volume,pitch,getUUID().getMostSignificantBits()^tickCount);}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height){return OriginalMobLoot.spawn(this,stack,height);}
    @Override protected void dropExperience(Entity killer){} // Original XP belongs to deathTime20, never die().
    @Override protected void tickDeath(){
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,2,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent){}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent){
        if(level.random.nextInt(6)==0)spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get(level.random.nextBoolean()?"tainted_goo":"taint_tendril").get()),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
}
