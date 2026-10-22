package thaumcraft.fabric.testing;

import net.fabricmc.api.ModInitializer;
import thaumcraft.common.testing.FluxOverfullRuntimeChecks;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks;
import thaumcraft.common.testing.ExcavationRemovalFailureRuntimeChecks;

/** Test-only blocks must enter the native registry during mod initialization,
 * never be constructed after registry freeze inside a running test. This entire
 * source set and its entrypoint are excluded from production artifacts. */
public final class ThaumcraftGameTestBootstrap implements ModInitializer {
    @Override public void onInitialize() {
        NodeTransductionRuntimeChecks.registerTestProbes();
        thaumcraft.common.testing.VisAmuletRuntimeChecks.registerTestProbes();
        FluxOverfullRuntimeChecks.registerTestProbes();
        ExcavationRemovalFailureRuntimeChecks.registerEarlyProbe();
        thaumcraft.common.testing.BiologicalLampRuntimeChecks.registerTestProbes();
        // The shared harness observes the loader's own block-break phases. Fabric
        // owns the three PlayerBlockBreakEvents; the shared side never names them.
        thaumcraft.common.testing.NativeBreakPhases.Holder.install(FabricBreakPhases::install);
    }
}
