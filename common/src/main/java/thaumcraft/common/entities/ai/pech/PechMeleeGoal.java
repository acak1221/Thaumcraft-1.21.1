package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.monster.EntityPech;

/** TC4 AIAttackOnCollide, specifically its ten-tick attack gate and legacy reach.
 * Current navigation supplies paths; native MeleeAttackGoal's twenty-tick and
 * changed path-recalculation policies are not substituted. */
public final class PechMeleeGoal extends Goal {
    private final EntityPech pech;private Path path;private int delay,attackTick,failedPenalty;
    private double oldX,oldY,oldZ;
    public PechMeleeGoal(EntityPech pech){this.pech=pech;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        var target=pech.getTarget();if(target==null||!target.isAlive())return false;
        if(--delay<=0){path=pech.getNavigation().createPath(target,0);delay=4+pech.getRandom().nextInt(7);return path!=null;}return true;
    }
    @Override public boolean canContinueToUse(){var target=pech.getTarget();return target!=null&&target.isAlive()&&!pech.getNavigation().isDone();}
    @Override public void start(){pech.getNavigation().moveTo(path,.6);delay=0;}
    @Override public void stop(){pech.getNavigation().stop();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        var target=pech.getTarget();if(target==null)return;
        pech.getLookControl().setLookAt(target,30,30);double distance=pech.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());
        double reach=pech.getBbWidth()*2*pech.getBbWidth()*2+target.getBbWidth();--delay;if(attackTick>0)--attackTick;
        if(pech.getSensing().hasLineOfSight(target)&&delay<=0&&(oldX==0&&oldY==0&&oldZ==0||target.distanceToSqr(oldX,oldY,oldZ)>=1||pech.getRandom().nextFloat()<.05F)){
            oldX=target.getX();oldY=target.getBoundingBox().minY;oldZ=target.getZ();delay=failedPenalty+4+pech.getRandom().nextInt(7);
            var current=pech.getNavigation().getPath();var end=current==null?null:current.getEndNode();failedPenalty=end!=null&&target.distanceToSqr(end.x,end.y,end.z)<1?0:failedPenalty+10;
            if(distance>1024)delay+=10;else if(distance>256)delay+=5;
            if(!pech.getNavigation().moveTo(target,.6))delay+=15;
        }
        if(distance<=reach&&attackTick<=0){attackTick=10;if(!pech.getMainHandItem().isEmpty())pech.swing(net.minecraft.world.InteractionHand.MAIN_HAND);pech.doHurtTarget(target);}
    }
}
