package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.monster.EntityPech;

/** Vanilla1.7 AvoidEntity's PLAYER branch used by untamed Pech. Creative and
 * invisible players are deliberately included, with NO sight requirement.
 * Only modern spectators (absent in1.7) are excluded. Current native navigation
 * and retreat-position geometry supply paths, not modern combat targeting. */
public final class PechAvoidGoal extends Goal {
    private final EntityPech pech;private Player target;private Path path;
    public PechAvoidGoal(EntityPech pech){this.pech=pech;setFlags(EnumSet.of(Flag.MOVE));}
    public static Player closestPlayer(EntityPech pech,double range){
        Player result=null;double closest=-1;
        for(Player player:pech.level().players()){
            if(player.isSpectator())continue;
            double distance=player.distanceToSqr(pech);
            if((range<0||distance<range*range)&&(closest==-1||distance<closest)){closest=distance;result=player;}
        }
        return result;
    }
    @Override public boolean canUse(){
        target=closestPlayer(pech,8);if(target==null)return false;
        var candidate=DefaultRandomPos.getPosAway(pech,16,7,target.position());if(candidate==null)return false;
        // Old RandomPositionGenerator returns integer XYZ. Native returns
        // bottom-center X+.5/Z+.5; restore the old coordinate contract BEFORE
        // distance and exact path-end checks, including negative coordinates.
        var retreat=Vec3.atLowerCornerOf(BlockPos.containing(candidate));
        if(target.distanceToSqr(retreat)<target.distanceToSqr(pech))return false;
        path=pech.getNavigation().createPath(retreat.x,retreat.y,retreat.z,0);if(path==null)return false;
        var end=path.getEndNode();return end!=null&&end.x==(int)retreat.x&&end.z==(int)retreat.z;
    }
    @Override public boolean canContinueToUse(){return !pech.getNavigation().isDone();}
    @Override public void start(){pech.getNavigation().moveTo(path,.5);}
    @Override public void stop(){target=null;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){if(target!=null)pech.getNavigation().setSpeedModifier(pech.distanceToSqr(target)<49?.6:.5);}
}
