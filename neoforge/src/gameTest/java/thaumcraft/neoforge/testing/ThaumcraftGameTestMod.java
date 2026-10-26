package thaumcraft.neoforge.testing;

import java.io.File;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.common.testing.BiologicalLampRuntimeChecks;
import thaumcraft.common.testing.ExcavationRemovalFailureRuntimeChecks;
import thaumcraft.common.testing.FluxOverfullRuntimeChecks;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks;
import thaumcraft.common.testing.VisAmuletRuntimeChecks;

/** Loaded only from the isolated gameTest mod roots, not from the production mod. */
@Mod(ThaumcraftGameTestMod.MODID)
public final class ThaumcraftGameTestMod {
    public static final String MODID = "thaumcraft_gametest";
    private static final Logger LOG = LoggerFactory.getLogger("ThaumcraftGameTest");

    public ThaumcraftGameTestMod(IEventBus bus) {
        bus.addListener((RegisterGameTestsEvent event) -> {
            event.register(ThaumcraftGameTests.class);
            // NeoForge-only scenarios, outside the catalogue generated from the Fabric suite.
            event.register(NeoForgeFluidCapabilityGameTests.class);
        });
        // The shared harness observes the loader's own block-break phases. NeoForge
        // owns the native BlockEvent.BreakEvent and the real removal; the shared
        // side never names a loader class.
        thaumcraft.common.testing.NativeBreakPhases.Holder.install(NeoForgeBreakPhases::install);
        // NeoForge writes saved data on its IO worker; disk-reload fixtures wait for it.
        thaumcraft.common.testing.SavedDataFlush.install(net.neoforged.neoforge.common.IOUtilities::waitUntilIOWorkerComplete);
        // The test-only blocks of the shared harness must enter the native registry
        // before it freezes. The Fabric harness registers them in its mod
        // initializer; NeoForge has no such phase, but its RegisterEvent fires
        // while the registry is still open, which is the exact equivalent.
        bus.addListener(ThaumcraftGameTestMod::onRegister);
        if (GameTestHooks.isGametestServer() && System.getProperty("thaumcraft.gametest.report-file") != null) {
            try {
                GlobalTestReporter.replaceWith(new ReportingJUnitReporter(
                        new File(System.getProperty("thaumcraft.gametest.report-file"))));
            } catch (javax.xml.parsers.ParserConfigurationException exception) {
                throw new IllegalStateException("Cannot create GameTest XML reporter", exception);
            }
        }
    }

    /**
     * The NeoForge equivalent of the Fabric game-test bootstrap.
     *
     * <p>Every probe of the shared harness is a test-only block (and one test-only block entity
     * type) that has to be in the native registry before the registry freezes: the probes observe
     * real neighbour notifications, real crop growth and real relay pulses, so they cannot be
     * manufactured inside a running test. The Fabric harness registers them in the mod
     * initializer; NeoForge offers the {@link RegisterEvent} of every registry instead, and that
     * event fires while the registry is still mutable. The registered objects are the very same
     * classes the shared harness owns, so both loaders observe the same native behaviour.</p>
     */
    private static void onRegister(RegisterEvent event) {
        var key = event.getRegistryKey();
        if (Registries.BLOCK.equals(key)) {
            // The Fabric harness registers this neighbour probe in its initializer too.
            thaumcraft.common.testing.ResourceSafetyRuntimeChecks.registerTestProbes();
            NodeTransductionRuntimeChecks.registerTestProbes();
            ExcavationRemovalFailureRuntimeChecks.registerEarlyProbe();
            BiologicalLampRuntimeChecks.registerTestProbes();
            FluxOverfullRuntimeChecks.registerTestProbes();
            VisAmuletRuntimeChecks.registerTestProbes();
        } else if (Registries.BLOCK_ENTITY_TYPE.equals(key)) {
            // The amulet pulse relay owns a block entity type; the block itself is
            // registered by the block pass above, so the type is registered here.
            VisAmuletRuntimeChecks.registerTestProbes();
        } else if (Registries.ITEM.equals(key)) {
            // The test add-on's wand rod, staff core, cap and focus items, as the Fabric initializer registers them.
            thaumcraft.common.testing.WandApiRuntimeChecks.registerTestItems();
        }
    }

    /**
     * The JUnit reporter of vanilla, plus the stack trace of the failure in the server log.
     *
     * <p>The XML report of vanilla keeps only the message of a failure, so an exception without a
     * message (a {@code NullPointerException}) is indistinguishable from an empty assertion. The
     * log line names the scenario and prints the whole trace, which is what the port analysis
     * needs.</p>
     */
    private static final class ReportingJUnitReporter implements TestReporter {
        private final JUnitLikeTestReporter delegate;

        ReportingJUnitReporter(File destination) throws javax.xml.parsers.ParserConfigurationException {
            this.delegate = new JUnitLikeTestReporter(destination);
        }

        @Override public void onTestFailed(GameTestInfo info) {
            var error = info.getError();
            LOG.error("GameTest scenario {} failed", info.getTestName(), error);
            delegate.onTestFailed(info);
        }

        @Override public void onTestSuccess(GameTestInfo info) {
            delegate.onTestSuccess(info);
        }

        @Override public void finish() {
            delegate.finish();
        }
    }
}
