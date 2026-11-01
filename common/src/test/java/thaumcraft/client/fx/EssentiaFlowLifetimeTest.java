package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class EssentiaFlowLifetimeTest {
    private static final BlockPos CONSUMER = new BlockPos(0, 64, 0);
    private static final BlockPos SOURCE = new BlockPos(16, 64, 0);

    @Test void unloadingEitherEndpointCannotPreserveAndReplayAnExpiredFlow() {
        for (BlockPos unloaded : List.of(CONSUMER, SOURCE)) {
            var flows = runningFlow();
            var rendered = new ArrayList<EssentiaFlowClientEffects.Flow>();
            for (int tick = 0; tick < 2; tick++) {
                EssentiaFlowClientEffects.tickFlows(flows, pos -> true, rendered::add);
            }
            for (int tick = 0; tick < 14; tick++) {
                EssentiaFlowClientEffects.tickFlows(flows, pos -> !pos.equals(unloaded), rendered::add);
            }
            assertTrue(flows.isEmpty(), "the flow expires while either endpoint is unloaded");
            EssentiaFlowClientEffects.tickFlows(flows, pos -> true, rendered::add);
            assertEquals(2, rendered.size(), "reloading the chunk cannot replay a stale flow");
        }
    }

    @Test void loadedFlowsStillRenderForAllFifteenOriginalTicks() {
        var flows = runningFlow();
        var rendered = new ArrayList<EssentiaFlowClientEffects.Flow>();
        for (int tick = 0; tick < 16; tick++) {
            EssentiaFlowClientEffects.tickFlows(flows, pos -> true, rendered::add);
        }
        assertEquals(15, rendered.size());
        assertTrue(flows.isEmpty());
    }

    private static Map<String, EssentiaFlowClientEffects.Flow> runningFlow() {
        var flows = new LinkedHashMap<String, EssentiaFlowClientEffects.Flow>();
        flows.put("flow", new EssentiaFlowClientEffects.Flow(CONSUMER, SOURCE, 0xFFFFFF,
                EssentiaFlowClientEffects.LIFETIME));
        return flows;
    }
}
