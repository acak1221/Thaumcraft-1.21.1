package thaumcraft.client.renderers.block;

/** BlockCustomOreRenderer:63,65: preserve the packed-integer max, not fullbright. */
public final class OreRenderRules {
    private static volatile int anisotropicFiltering=1;
    private OreRenderRules() {}
    public static int overlayLight(int brightness) {return Math.max(brightness,160);}
    public static boolean stonePass(int anisotropicFiltering) {return anisotropicFiltering>1;}
    public static boolean stonePass() {return stonePass(anisotropicFiltering);}
    /** Called on the render thread; terrain workers only read the cached setting. */
    public static boolean updateAnisotropicFiltering(int value) {
        if(anisotropicFiltering==value)return false;
        anisotropicFiltering=value;return true;
    }
    public static final float STONE_MIN=.005F, STONE_MAX=.995F;
}
