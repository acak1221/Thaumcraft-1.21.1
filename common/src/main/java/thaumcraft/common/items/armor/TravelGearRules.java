package thaumcraft.common.items.armor;

/** Original ItemBootsTraveller and Hover float/counter contracts, without a world. */
public final class TravelGearRules {
    public static final int EFFICIENCY=360;
    public record Expenditure(int fuel,int charge,boolean sustained,boolean consumed) {}
    private TravelGearRules() {}
    public static float groundAcceleration(boolean water) { return water?.055F/4F:.055F; }
    public static float airAcceleration(boolean hovering) { return hovering?.03F:.05F; }
    public static float afterBootFall(float distance) { return distance>0?distance-.25F:distance; }
    public static double jumpBonus() { return (double).275F; }
    public static float damping(int haste,boolean girdle) { return Math.min(1F,.7F+.075F*haste+(girdle?.21F:0)); }
    public static Expenditure expend(int fuel,int charge,boolean girdle) {
        if(fuel<0||fuel>64||charge<0||charge>360)throw new IllegalArgumentException("Invalid physical harness fuel/counter");
        if(fuel==0)return new Expenditure(0,charge,false,false);
        if(charge<360F*(girdle?.8F:1F))return new Expenditure(fuel,charge+1,true,false);
        return new Expenditure(fuel-1,0,fuel>1,true);
    }
}
