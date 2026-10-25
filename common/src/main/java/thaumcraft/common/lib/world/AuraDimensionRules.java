package thaumcraft.common.lib.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Original dimension dispatch (:130-139) and Nether aura-only body (:548-555). */
public final class AuraDimensionRules {
    private AuraDimensionRules() {}
    public static boolean supportsWildNodes(ResourceKey<Level> dimension) {
        // Original generateNether:548-555 bypasses the surface blacklist; generateSurface:412
        // admits aura only at levels other than zero and two. Keep that asymmetry.
        return dimension.equals(Level.NETHER) || dimension.equals(Level.OVERWORLD)
                && NodeEcologyPolicy.permitsLevel(NodeEcologyPolicy.dimensionLevel(dimension));
    }
    public static boolean netherReplayAura(boolean generationEnabled, boolean regenerationEnabled) {
        return generationEnabled && regenerationEnabled;
    }
}
