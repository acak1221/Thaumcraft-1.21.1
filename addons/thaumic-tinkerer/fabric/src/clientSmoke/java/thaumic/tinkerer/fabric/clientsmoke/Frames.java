package thaumic.tinkerer.fabric.clientsmoke;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Counts rendered frames. The tick runs more often than the software renderer draws, so "wait N ticks" does not mean "N
 * frames were drawn"; a picture is taken only after the frames it needs. Test-only code of the port (no 1.7.10
 * counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>With a level loaded every frame renders the world, so the world frames are counted; without one (the title screen) the
 * frames of the screens are.</p>
 */
final class Frames {
    private final Set<Screen> seen = Collections.newSetFromMap(new WeakHashMap<>());
    private volatile long world;
    private volatile long screen;

    void install() {
        WorldRenderEvents.END.register(context -> world++);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!seen.add(screen)) return;
            ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, delta) -> this.screen++);
        });
    }

    /** The frame counter that matches what is on the window now. */
    long count() {
        return Minecraft.getInstance().level != null ? world : screen;
    }

    long worldFrames() {
        return world;
    }

    long screenFrames() {
        return screen;
    }
}
