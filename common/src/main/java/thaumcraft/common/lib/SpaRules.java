package thaumcraft.common.lib;

/** Original TileSpa/BlockFluidPure constants and integer arithmetic. */
public final class SpaRules {
    public static final int CAPACITY_MB=5000,PLACEMENT_MB=1000,PERIOD=40,BATH_SALT_LIFETIME=200;
    private SpaRules() {}
    public static int warpWardDuration(int permanentWarp) {
        int divisor=1;
        if(permanentWarp>0)divisor=Math.max(1,(int)Math.sqrt(permanentWarp));
        return Math.min(32000,200000/divisor);
    }
    /** Forge BlockFluidClassic subtracts one additional quantum from scaled light.
     * Its configured luminosity10 therefore gives source light8, not10. */
    public static int normalizedMetadata(int metadata){float percent=(8-metadata)/8F;if(percent<=0||percent>=1)return metadata;int result=(int)(1F/percent)-1;return result>=0&&result<16?result:metadata;}
    public static int pureLight(int metadata) { return Math.max(0,(7-metadata)*10/8); }
}
