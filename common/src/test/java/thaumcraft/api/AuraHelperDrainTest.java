package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.visnet.VisNetHandler;

/**
 * The drain of the retained 1.12 add-on surface.
 *
 * <p>The retained 1.12 reference drains the VIS NETWORK at the position and not the essence of one
 * node: {@code src/main/java/thaumcraft/common/world/aura/AuraHandler.java:27-33} delegates to
 * {@code VisNetHandler.drainVis(world, x, y, z, requested, simulate)}, and that method walks the
 * primal aspects of every root source that is in range of the position
 * ({@code src/main/java/thaumcraft/api/visnet/VisNetHandler.java:92-139}). The native projection keeps
 * that selection.</p>
 */
class AuraHelperDrainTest {
    @Test void theGuardsOfTheOriginalSurfaceAreKept() {
        BlockPos pos = new BlockPos(1, 2, 3);
        assertEquals(0, AuraHelper.drainVis(null, pos, 10, false), "a missing world drains nothing");
        assertEquals(0, AuraHelper.drainVis(null, pos, 0, false), "a request of nothing drains nothing");
        assertEquals(0, AuraHelper.drainVis(null, pos, -5, false), "a negative request drains nothing");
        assertEquals(0, VisNetHandler.drainVis(null, pos, 10, true), "the native twin has the same guards");
    }

    @Test void theAspectLessDrainWalksThePrimalAspectsInTheOriginalOrder() {
        List<Aspect> order = new ArrayList<>();
        for (Aspect aspect : Aspect.getPrimalAspects()) order.add(aspect);
        assertEquals(6, order.size(), "the original has six primal aspects");
        assertEquals(List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY),
                order, "the original order of the primal aspects is the registration order");
    }

    @Test void theDrainIsBoundedByTheRequestAndTakesThePrimalAspectsInOrder() {
        // Every primal aspect has five, so a request of twelve takes air five, earth five and fire two.
        assertEquals(12, VisNetHandler.planPrimalDrain(12, aspect -> 5),
                "the drain is bounded by the request and stops when the request is met");
        assertEquals(30, VisNetHandler.planPrimalDrain(1000, aspect -> 5),
                "the drain takes every primal aspect of the network");
        assertEquals(0, VisNetHandler.planPrimalDrain(0, aspect -> 5), "a request of nothing drains nothing");
        assertEquals(0, VisNetHandler.planPrimalDrain(-1, aspect -> 5), "a negative request drains nothing");
        assertTrue(VisNetHandler.planPrimalDrain(10, aspect -> -4) == 0,
                "a source without stock never gives a negative amount");
    }

    @Test void theProjectionReachesTheNetworkAndNotTheNodeAtThePosition() {
        // The retained add-on surface keeps its package and its entry point; the
        // selection itself is the network drain of VisNetHandler.
        assertEquals("thaumcraft.api.aura.AuraHelper", AuraHelper.class.getName(),
                "the retained add-on surface keeps its package");
        assertEquals("thaumcraft.api.visnet.VisNetHandler", VisNetHandler.class.getName(),
                "the drain of the retained surface is the native network drain");
    }
}
