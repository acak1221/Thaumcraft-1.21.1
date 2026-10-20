package thaumcraft.common.entities.monster;

/** Original minimumY1 expressed relative to the actual native world bottom, preserving belowzero1.21 terrain. */
public final class TaintSwarmFlightRules {
    private TaintSwarmFlightRules() {}
    public static boolean aboveWorldBottom(int waypointY,int minimumBuildHeight) { return waypointY>minimumBuildHeight; }
}
