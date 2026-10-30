package thaumic.tinkerer.fabric.clientsmoke;

import java.util.List;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The creative inventory with the add-on's tab selected: the title of the tab and its icon and contents as the player sees them.
 * Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The picture is the visual side of the creative-tab translation check: a missing key shows up as the raw key in the tab's
 * title. The tab is selected through Fabric's {@code FabricCreativeInventoryScreen}, which also switches to the page that holds it.</p>
 */
final class CreativeTabPhase extends ScriptedPhase {
    @Override
    public String name() {
        return "creative_tab";
    }

    @Override
    protected List<Script> scripts(SmokeRun run) {
        Script script = new Script("creative-tab");
        CreativeModeInventoryScreen[] screen = { null };
        script.run("open the creative inventory", r -> {
            Minecraft client = r.client();
            screen[0] = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), true);
            client.setScreen(screen[0]);
        });
        script.until("the screen", () -> run.client().screen == screen[0], 10);
        script.run("select the add-on's tab", r -> {
            boolean selected = ((FabricCreativeInventoryScreen) screen[0]).setSelectedItemGroup(TTRegistry.CREATIVE_TAB.get());
            if (!selected) r.report().add("creative_tab", "creative_tab_selected", "Core", "creative_tab", SmokeReport.Result.HARNESS_ERROR, "the tab could not be selected through FabricCreativeInventoryScreen");
        });
        script.frames(8);
        script.then("picture", run.shots().shot("menu-creative-tab", "Core", "the creative inventory with the Thaumic Tinkerer tab selected; the tab's title is drawn at the top of the panel"), 60);
        script.run("close", r -> r.client().setScreen(null));
        return List.of(script);
    }
}
