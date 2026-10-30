package thaumic.tinkerer.fabric.clientsmoke;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import thaumic.tinkerer.fabric.clientsmoke.TinkererContent.Entry;

/**
 * Every menu type the add-on registers has a screen factory on the client. Test-only code of the port (no 1.7.10
 * counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The factory table of the game is private; it is read by reflection. That only says a factory is registered: that the
 * screen opens and draws is shown by the menus phase of the world part.</p>
 */
final class ScreensCheck implements SmokeCheck {
    @Override
    public String name() {
        return "menu_screens";
    }

    @Override
    public void run(SmokeRun run) throws ReflectiveOperationException {
        Field field = MenuScreens.class.getDeclaredField("SCREENS");
        field.setAccessible(true);
        Map<?, ?> screens = (Map<?, ?>) field.get(null);
        for (Entry<MenuType<?>> entry : run.content().menus()) {
            String target = "menu:" + entry.path();
            if (screens.containsKey(entry.value())) run.report().pass(PHASE, "menu_screen_factory", entry.group(), target, "a screen factory is registered");
            else run.report().fail(PHASE, "screen_missing", entry.group(), target, "the menu type has no screen factory (MenuScreens)");
        }
        run.report().inventory("menus", TinkererContent.paths(run.content().menus()));
    }
}
