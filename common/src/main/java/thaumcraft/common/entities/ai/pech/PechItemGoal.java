package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.pech.PechDrops;

/** Original AIPechItemEntityGoto, including the default golemDelay FIVE-tick
 * search gate, whole-stack canPickup admission and failed-path retry penalty. */
public final class PechItemGoal extends Goal {
    /** The original 1.7 selector priority of {@code AIPechItemEntityGoto}. */
    public static final int PRIORITY = 3;
    private final EntityPech pech;private ItemEntity target;private int count,failedPathFindingPenalty;
    public PechItemGoal(EntityPech pech){this.pech=pech;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        if(pech.tickCount%5>0||--count>0)return false;
        double range=Double.MAX_VALUE;
        for(Entity entity:pech.level().getEntities(pech,pech.getBoundingBox().inflate(16))){
            if(!(entity instanceof ItemEntity item)||!pech.canPickup(item.getItem())||PechDrops.isMarked(item))continue;
            double distance=item.distanceToSqr(pech);
            if(distance<range&&distance<=256){range=distance;target=item;}
        }
        return target!=null;
    }
    @Override public boolean canContinueToUse(){return target!=null&&target.isAlive()&&!pech.getNavigation().isDone()&&target.distanceToSqr(pech)<256;}
    @Override public void start(){pech.getNavigation().moveTo(pech.getNavigation().createPath(target,0),speed());count=0;}
    @Override public void stop(){target=null;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    private double speed(){return pech.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.5;}
    @Override public void tick(){
        if(target==null||!target.isAlive())return;
        pech.getLookControl().setLookAt(target,30,30);
        if(pech.getSensing().hasLineOfSight(target)&&--count<=0){
            count=failedPathFindingPenalty+4+pech.getRandom().nextInt(4);
            pech.getNavigation().moveTo(target,speed());var path=pech.getNavigation().getPath();var end=path==null?null:path.getEndNode();
            failedPathFindingPenalty=end!=null&&target.distanceToSqr(end.x,end.y,end.z)<1?0:failedPathFindingPenalty+10;
        }
        if(pech.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())<=1.5){
            count=0;int before=target.getItem().getCount();ItemStack remaining=pech.pickupItem(target.getItem());
            if(remaining.isEmpty())target.discard();else target.setItem(remaining);
            if(remaining.isEmpty()||remaining.getCount()!=before)pech.level().playSound(null,target.getX(),target.getY(),target.getZ(),SoundEvents.ITEM_PICKUP,target.getSoundSource(),.2F,((target.level().random.nextFloat()-target.level().random.nextFloat())*.7F+1)*2);
        }
    }
}
