package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityPech;

/** Vanilla1.7 EntityAIArrowAttack used twice by Pech, with20..50 arrow or20..30
 * curse timing. Every simulation tick updates sight and its range-scaled timer. */
public final class PechRangedGoal extends Goal {
    // Vanilla 1.7 EntityAIArrowAttack used by EntityPech: preserve its range,
    // visible-update gate, minimum interval, navigation speed and aiming limits.
    private static final float ATTACK_RANGE = 15;
    private static final double ATTACK_RANGE_SQUARED = 225;
    private static final int VISIBLE_UPDATES_TO_STOP = 20;
    private static final int MIN_ATTACK_INTERVAL = 20;
    private static final double CHASE_SPEED = .6;
    private static final float AIM_TURN_LIMIT = 30;
    private static final float MIN_ATTACK_POWER = .1F;
    private final EntityPech pech;private final int maximum;private LivingEntity target;private int sight,attackTime=-1;
    public PechRangedGoal(EntityPech pech,int maximum){this.pech=pech;this.maximum=maximum;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){if(pech.getTarget()==null)return false;target=pech.getTarget();return true;}
    @Override public boolean canContinueToUse(){return canUse()||!pech.getNavigation().isDone();}
    @Override public void stop(){target=null;sight=0;attackTime=-1;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        if(target==null)return;double distance=pech.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());boolean visible=pech.getSensing().hasLineOfSight(target);
        if(visible)++sight;else sight=0;
        if(distance<=ATTACK_RANGE_SQUARED&&sight>=VISIBLE_UPDATES_TO_STOP)pech.getNavigation().stop();else pech.getNavigation().moveTo(target,CHASE_SPEED);
        pech.getLookControl().setLookAt(target,AIM_TURN_LIMIT,AIM_TURN_LIMIT);
        if(--attackTime==0){
            if(distance>ATTACK_RANGE_SQUARED||!visible)return;
            float ratio=(float)Math.sqrt(distance)/ATTACK_RANGE;pech.performRangedAttack(target,Mth.clamp(ratio,MIN_ATTACK_POWER,1));attackTime=Mth.floor(ratio*(maximum-MIN_ATTACK_INTERVAL)+MIN_ATTACK_INTERVAL);
        }else if(attackTime<0)attackTime=Mth.floor((float)Math.sqrt(distance)/ATTACK_RANGE*(maximum-MIN_ATTACK_INTERVAL)+MIN_ATTACK_INTERVAL);
    }
}
