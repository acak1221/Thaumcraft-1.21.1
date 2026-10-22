package thaumcraft.client.renderers.item;

/** Registry/GL-independent original ItemBowBoneRenderer:97-112 texture rules. */
public final class BoneBowGlintRules {
    public static final float RED = .38F, GREEN = .19F, BLUE = .608F;
    private BoneBowGlintRules() {}
    public static float[] uv(float u, float v, long millis, int pass) {
        long period = pass == 0 ? 3000L : 4873L;
        float scroll = (millis % period) / (float) period * 8F * (pass == 0 ? 1F : -1F);
        double angle = Math.toRadians(pass == 0 ? -50 : 10);
        return new float[] { .125F * ((float) (u * Math.cos(angle) - v * Math.sin(angle)) + scroll),
                .125F * (float) (u * Math.sin(angle) + v * Math.cos(angle)) };
    }
}
