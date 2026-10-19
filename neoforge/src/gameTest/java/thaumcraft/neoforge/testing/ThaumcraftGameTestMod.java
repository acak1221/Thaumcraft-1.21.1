package thaumcraft.neoforge.testing;

import java.io.File;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/** Loaded only from the isolated gameTest mod roots, not from the production mod. */
@Mod(ThaumcraftGameTestMod.MODID)
public final class ThaumcraftGameTestMod {
    public static final String MODID = "thaumcraft_gametest";

    public ThaumcraftGameTestMod(IEventBus bus) {
        bus.addListener((RegisterGameTestsEvent event) -> event.register(ThaumcraftGameTests.class));
        if (GameTestHooks.isGametestServer() && System.getProperty("thaumcraft.gametest.report-file") != null) {
            try {
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(System.getProperty("thaumcraft.gametest.report-file"))));
            } catch (javax.xml.parsers.ParserConfigurationException exception) {
                throw new IllegalStateException("Cannot create GameTest XML reporter", exception);
            }
        }
    }
}
