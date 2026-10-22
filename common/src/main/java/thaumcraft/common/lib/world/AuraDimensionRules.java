package thaumcraft.common.lib.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Original dimension dispatch (:130-139) and Nether aura-only body (:548-555). */
public final class AuraDimensionRules {
    private AuraDimensionRules() {}
    public static boolean supportsWildNodes(ResourceKey<Level> dimension) {
        return dimension.equals(Level.OVERWORLD) || dimension.equals(Level.NETHER);
    }
    public static boolean netherReplayAura(boolean generationEnabled, boolean regenerationEnabled) {
        return generationEnabled && regenerationEnabled;
    }
}
