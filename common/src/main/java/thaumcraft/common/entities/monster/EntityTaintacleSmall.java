package thaumcraft.common.entities.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftTaint;

/** Original lifetime is deliberately transient and uses post-decrement, including its201st-tick death. */
public final class EntityTaintacleSmall extends EntityTaintacle {
    private int lifetime=200;
    public EntityTaintacleSmall(EntityType<? extends EntityTaintacleSmall> type,Level level) { super(type,level);xpReward=0; }
    public EntityTaintacleSmall(Level level) { this(ThaumcraftTaint.TAINTACLE_SMALL.get(),level); }
    public static AttributeSupplier.Builder createAttributes() { return EntityTaintacle.createAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.ATTACK_DAMAGE,2); }
    @Override public void tick() { super.tick();if(lifetime--<=0)actuallyHurt(damageSources().magic(),10); }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {dropNativeEquipment(level,source,recentlyHit);}
}
