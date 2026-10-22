package thaumcraft.client.renderers.block;

/** BlockTaintRenderer:41,48-64: metadata zero/one; opposite-face icon argument. */
public final class TaintOverlayRules {
    private TaintOverlayRules() {}
    public static boolean applies(int metadata) {return metadata==0||metadata==1;}
    public static int textureSide(int worldSide) {return worldSide^1;}
    public static final int BRIGHTNESS=200;
}
