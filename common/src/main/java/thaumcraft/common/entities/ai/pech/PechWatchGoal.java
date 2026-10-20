package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityPech;

/** Vanilla1.7 WatchClosest and WatchClosest2. The close PLAYER watcher owns
 * MOVE+LOOK and probability1; the Mob watcher owns LOOK and probability.02.
 * Original plain nearest queries have no new invisibility/creative/LOS gates. */
public final class PechWatchGoal extends Goal {
    private final EntityPech pech;private final boolean players;private final float range,chance;private Entity target;private int time;
    public PechWatchGoal(EntityPech pech,boolean players){this.pech=pech;this.players=players;range=players?3:8;chance=players?1:.02F;setFlags(players?EnumSet.of(Flag.MOVE,Flag.LOOK):EnumSet.of(Flag.LOOK));}
    @Override public boolean canUse(){
        if(pech.getRandom().nextFloat()>=chance)return false;
        if(players)target=PechAvoidGoal.closestPlayer(pech,range);
        else{
            target=null;double distance=Double.MAX_VALUE;
            for(Mob mob:pech.level().getEntitiesOfClass(Mob.class,pech.getBoundingBox().inflate(range,3,range))){
                if(mob==pech)continue;double next=mob.distanceToSqr(pech);if(next<=distance){distance=next;target=mob;}
            }
        }
        return target!=null;
    }
    @Override public boolean canContinueToUse(){return target!=null&&target.isAlive()&&pech.distanceToSqr(target)<=range*range&&time>0;}
    @Override public void start(){time=40+pech.getRandom().nextInt(40);}
    @Override public void stop(){target=null;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){if(target!=null)pech.getLookControl().setLookAt(target.getX(),target.getY()+target.getEyeHeight(),target.getZ(),10,pech.getMaxHeadXRot());--time;}
}
