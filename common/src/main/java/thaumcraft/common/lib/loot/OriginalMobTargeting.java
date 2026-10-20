package thaumcraft.common.lib.loot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Original1.7 World.getClosestVulnerablePlayer, including its strict range, sneaking and invisible armor cover gates. */
public final class OriginalMobTargeting {
    private OriginalMobTargeting() {}
    public static Player closestVulnerablePlayer(Entity source,double range) {
        double closest=-1;Player result=null;
        for(var player:source.level().players()) {
            if(player.getAbilities().invulnerable || !player.isAlive() || player.isSpectator())continue;
            double squared=player.distanceToSqr(source),effectiveRange=range;
            if(player.isShiftKeyDown())effectiveRange*=.8F;
            if(player.isInvisible())effectiveRange*=.7F*Math.max(.1F,player.getArmorCoverPercentage());
            if((range<0 || squared<effectiveRange*effectiveRange) && (closest==-1 || squared<closest)) { closest=squared;result=player; }
        }
        return result;
    }
}
