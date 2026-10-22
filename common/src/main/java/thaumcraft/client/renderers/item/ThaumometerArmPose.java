package thaumcraft.client.renderers.item;

/** Oracle ItemThaumometerRenderer:101-104, interpolated arm lag relative to the view. */
public final class ThaumometerArmPose {
    private ThaumometerArmPose() {}

    public static float lagDegrees(float view, float previousArm, float arm, float partial) {
        return (view - (previousArm + (arm - previousArm) * partial)) * 0.1F;
    }
}
