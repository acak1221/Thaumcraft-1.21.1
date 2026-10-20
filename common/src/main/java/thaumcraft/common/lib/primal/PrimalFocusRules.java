package thaumcraft.common.lib.primal;

import java.util.Random;

/** Exact ItemFocusPrimal cost clock and EntityPrimalOrb deterministic motion arithmetic. */
public final class PrimalFocusRules {
    public record Costs(int aqua,int aer,int terra,int ignis,int ordo,int perditio) {}
    public record Jitter(double x,double y,double z) {}
    private PrimalFocusRules(){}
    public static Costs costAtMillis(long millis){
        var random=new Random(millis/200L);
        return new Costs(50+random.nextInt(5)*50,50+random.nextInt(5)*50,50+random.nextInt(5)*50,
                50+random.nextInt(5)*50,50+random.nextInt(5)*50,50+random.nextInt(5)*50);
    }
    public static Jitter jitter(int entityId,int count){
        var random=new Random(entityId+count); // Original signed INT addition occurs before widening to the seed.
        return new Jitter((double)((random.nextFloat()-random.nextFloat())*.01F),
                (double)((random.nextFloat()-random.nextFloat())*.01F),
                (double)((random.nextFloat()-random.nextFloat())*.01F));
    }
    public static double seekerAxis(double velocity,double delta,double distanceSquared){
        // Original divides by SQUARED distance, then casts to float before clamping.
        float value=(float)(velocity+(delta/distanceSquared)*.2D);
        return Math.max(-.2F,Math.min(.2F,value));
    }
    public static boolean special(int roll,boolean portalBlock){return roll<=(portalBlock?10:1);}
    public static int taintOffset(float first,float second){return (int)(first-second*6F);}
}
