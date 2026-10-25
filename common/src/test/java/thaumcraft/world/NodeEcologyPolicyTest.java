package thaumcraft.world;

import java.util.HashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.NodeEcologyPolicy;
import static org.junit.jupiter.api.Assertions.*;

class NodeEcologyPolicyTest {
    @Test void blacklistOneAllowsNodeEcologyWhileOnlyZeroAndTwoBlockIt() {
        for (int level : new int[] {Integer.MIN_VALUE, -1, 1, 3, 17, Integer.MAX_VALUE})
            assertTrue(NodeEcologyPolicy.permitsLevel(level), "original blacklist is not a generic nonnegative rejection: " + level);
        assertFalse(NodeEcologyPolicy.permitsLevel(0));
        assertFalse(NodeEcologyPolicy.permitsLevel(2));
    }

    @Test void canonicalNetherAndEndRemainExcludedEvenWhenTheirBlacklistAllowsNodes() {
        var old = new HashMap<>(NodeEcologyPolicy.dimensionBlacklist);
        var custom = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("test", "ecology_dimension"));
        try {
            NodeEcologyPolicy.dimensionBlacklist.clear();
            assertEquals(-1, NodeEcologyPolicy.dimensionLevel(custom));
            assertTrue(NodeEcologyPolicy.permitsDimension(custom));
            assertTrue(NodeEcologyPolicy.permitsDimension(Level.OVERWORLD));
            assertFalse(NodeEcologyPolicy.permitsDimension(Level.NETHER));
            assertFalse(NodeEcologyPolicy.permitsDimension(Level.END));
            NodeEcologyPolicy.dimensionBlacklist.put(Level.NETHER, 1);
            NodeEcologyPolicy.dimensionBlacklist.put(Level.END, 1);
            assertFalse(NodeEcologyPolicy.permitsDimension(Level.NETHER));
            assertFalse(NodeEcologyPolicy.permitsDimension(Level.END));
            NodeEcologyPolicy.dimensionBlacklist.put(custom, 0); assertFalse(NodeEcologyPolicy.permitsDimension(custom));
            NodeEcologyPolicy.dimensionBlacklist.put(custom, 2); assertFalse(NodeEcologyPolicy.permitsDimension(custom));
            NodeEcologyPolicy.dimensionBlacklist.put(custom, 1); assertTrue(NodeEcologyPolicy.permitsDimension(custom));
        } finally {
            NodeEcologyPolicy.dimensionBlacklist.clear(); NodeEcologyPolicy.dimensionBlacklist.putAll(old);
        }
    }
    @Test void wildNodeAdmissionReadsSurfaceBlacklistButPreservesIndependentNetherPass() {
        var old = new HashMap<>(NodeEcologyPolicy.dimensionBlacklist);
        try {
            for (int level : new int[]{-1, 0, 1, 2, 3}) {
                NodeEcologyPolicy.dimensionBlacklist.put(Level.OVERWORLD, level);
                assertEquals(level != 0 && level != 2,
                        thaumcraft.common.lib.world.AuraDimensionRules.supportsWildNodes(Level.OVERWORLD),
                        "oracle generateSurface:412 aura blacklist level " + level);
                NodeEcologyPolicy.dimensionBlacklist.put(Level.NETHER, level);
                assertTrue(thaumcraft.common.lib.world.AuraDimensionRules.supportsWildNodes(Level.NETHER),
                        "oracle generateNether:548-555 never consults surface blacklist");
            }
        } finally {
            NodeEcologyPolicy.dimensionBlacklist.clear(); NodeEcologyPolicy.dimensionBlacklist.putAll(old);
        }
    }

}
