package thaumcraft.common.lib.world;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class NetherAuraParityTest {
    @Test void originalNetherDispatchGeneratesWildNodesAndSkipsEnd() {
        assertTrue(AuraDimensionRules.supportsWildNodes(Level.NETHER));
        assertTrue(AuraDimensionRules.supportsWildNodes(Level.OVERWORLD));
        assertFalse(AuraDimensionRules.supportsWildNodes(Level.END));
    }
    @Test void originalReplayNeedsBothGenAuraAndRegenAura() {
        assertTrue(AuraDimensionRules.netherReplayAura(true, true));
        assertFalse(AuraDimensionRules.netherReplayAura(true, false));
        assertFalse(AuraDimensionRules.netherReplayAura(false, true));
        assertFalse(AuraDimensionRules.netherReplayAura(false, false));
    }
}
