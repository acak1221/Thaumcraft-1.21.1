package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityPech;

/** Vanilla1.7 EntityAIArrowAttack used twice by Pech, with20..50 arrow or20..30
 * curse timing. Every simulation tick updates sight and its range-scaled timer. */
public final class PechRangedGoal extends Goal {
    private final EntityPech pech;private final int maximum;private LivingEntity target;private int sight,attackTime=-1;
    public PechRangedGoal(EntityPech pech,int maximum){this.pech=pech;this.maximum=maximum;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){if(pech.getTarget()==null)return false;target=pech.getTarget();return true;}
    @Override public boolean canContinueToUse(){return canUse()||!pech.getNavigation().isDone();}
    @Override public void stop(){target=null;sight=0;attackTime=-1;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        if(target==null)return;double distance=pech.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());boolean visible=pech.getSensing().hasLineOfSight(target);
        if(visible)++sight;else sight=0;
        if(distance<=225&&sight>=20)pech.getNavigation().stop();else pech.getNavigation().moveTo(target,.6);
        pech.getLookControl().setLookAt(target,30,30);
        if(--attackTime==0){
            if(distance>225||!visible)return;
            float ratio=(float)Math.sqrt(distance)/15;pech.performRangedAttack(target,Mth.clamp(ratio,.1F,1));attackTime=Mth.floor(ratio*(maximum-20)+20);
        }else if(attackTime<0)attackTime=Mth.floor((float)Math.sqrt(distance)/15*(maximum-20)+20);
    }
}
