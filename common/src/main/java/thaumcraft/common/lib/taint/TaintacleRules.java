package thaumcraft.common.lib.taint;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

/** Original rooted movement, agitation and ModelTaintacle FLOAT animation. */
public final class TaintacleRules {
    public record Segment(float pitch,float roll){}
    private TaintacleRules(){}
    public static boolean agitated(double squaredDistance,float height){return squaredDistance<(double)(height*7F*(height*7F));}
    public static float flail(float current,boolean active){return active?(current<3?current+.2F:current):(current>1?current-.2F:current);}
    public static float ariseHeight(int ticks,float height){float duration=height*10F;return ticks<duration?(duration-ticks)/duration*height:0;}
    public static List<Segment> segments(int length,float age,float flail,boolean agitated,boolean hurting){
        float mod=.0625F*.2F,frequency=agitated?3F:1F-mod,intensity=flail+(hurting?mod:-mod);
        var result=new ArrayList<Segment>(length-1);
        for(int k=0;k<length-1;k++)result.add(new Segment(.15F*intensity*Mth.sin(age*.1F*frequency-k/2F),.1F/intensity*Mth.sin(age*.15F-k/2F)));
        return List.copyOf(result);
    }
}
