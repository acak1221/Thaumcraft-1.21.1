package thaumcraft.common.lib.warp;

import java.util.function.DoubleSupplier;
import thaumcraft.common.lib.research.PlayerKnowledge;

/** Original ItemSanitySoap: duration200, strict >195 release and one unconditional float roll. */
public final class SanitySoapRules {
    public static final int DURATION=200;
    public record Cleansing(int stickyDelta,int temporaryDelta) {public boolean changed(){return stickyDelta!=0||temporaryDelta!=0;}}
    private SanitySoapRules(){}
    public static boolean finished(int remaining){return DURATION-remaining>195;}
    public static float chance(boolean ward,boolean pureFluid){float chance=.33F;if(ward)chance+=.25F;if(pureFluid)chance+=.25F;return chance;}
    public static Cleansing cleanse(PlayerKnowledge knowledge,boolean ward,boolean pureFluid,DoubleSupplier floatRoll){
        // The float is consumed BEFORE sticky>0, even with no warp in any channel.
        boolean reduce=floatRoll.getAsDouble()<chance(ward,pureFluid);int sticky=0,temporary=0;
        if(reduce&&knowledge.getWarpSticky()>0){knowledge.addWarpSticky(-1);sticky=-1;knowledge.setWarpCounter(knowledge.getTotalWarp());}
        if(knowledge.getWarpTemp()>0){temporary=-knowledge.getWarpTemp();knowledge.addWarpTemp(temporary);knowledge.setWarpCounter(knowledge.getTotalWarp());}
        return new Cleansing(sticky,temporary);
    }
}
