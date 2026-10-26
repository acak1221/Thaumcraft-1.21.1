package thaumcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import thaumcraft.client.gui.GuiAccessories;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.network.AccessoryNetworking;
import thaumcraft.common.mixin.client.AbstractContainerScreenAccess;

/**
 * The way to the bauble slots. TC4 used the Baubles mod and its menu, opened with the key B. The
 * port has its own four slots (an amulet, two rings and a belt), so it carries the key itself, and a
 * small button by the player model of the survival and the creative inventory, which follows the
 * panel when the recipe book moves it.
 */
public final class ThaumcraftBaublesClient {
    public static final KeyMapping ACCESSORIES = new KeyMapping("key.thaumcraft.accessories", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, KeyMapping.CATEGORY_INVENTORY);
    private static boolean initialized;
    private ThaumcraftBaublesClient(){}
    public static void init(){
        if(initialized)return;initialized=true;
        AccessoryNetworking.initClient();
        KeyMappingRegistry.register(ACCESSORIES);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (ACCESSORIES.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) AccessoryNetworking.requestOpen();
            }
        });
        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            AccessoryButton button = screen instanceof InventoryScreen inventory ? new AccessoryButton(inventory, 64, 66)
                    : screen instanceof CreativeModeInventoryScreen creative ? new AccessoryButton(creative, 128, 37) : null;
            if (button == null) return;
            button.follow();
            access.addRenderableWidget(button);
        });
        // The recipe book moves the panel and the creative tabs change without a new init.
        ClientGuiEvent.RENDER_PRE.register((screen, graphics, mouseX, mouseY, delta) -> {
            for (var child : screen.children()) if (child instanceof AccessoryButton button) button.follow();
            return dev.architectury.event.EventResult.pass();
        });
        MenuRegistry.registerScreenFactory(ThaumcraftBaubles.MENU.get(),GuiAccessories::new);
        for(var entry:ThaumcraftBaubles.DISCOUNT_RINGS.entrySet()) {
            int color=thaumcraft.api.aspects.Aspect.getAspect(entry.getKey()).getColor();
            ColorHandlerRegistry.registerItemColors((stack,tint)->tint==0?0xff000000|color:-1,entry.getValue());
        }
    }

    /** A small button at a fixed place on the screen's panel; in the creative screen only on its inventory tab. */
    private static final class AccessoryButton extends Button {
        private final AbstractContainerScreen<?> screen;
        private final int offsetX, offsetY;

        AccessoryButton(AbstractContainerScreen<?> screen, int offsetX, int offsetY) {
            super(0, 0, 11, 11, Component.translatable("gui.thaumcraft.accessories.button"), button -> AccessoryNetworking.requestOpen(),
                    DEFAULT_NARRATION);
            this.screen = screen;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            setTooltip(Tooltip.create(Component.translatable("gui.thaumcraft.accessories.tooltip",
                    Component.translatable("container.thaumcraft.accessories"), ACCESSORIES.getTranslatedKeyMessage())));
        }

        void follow() {
            visible = active = !(screen instanceof CreativeModeInventoryScreen creative) || creative.isInventoryOpen();
            var access = (AbstractContainerScreenAccess) screen;
            setPosition(access.thaumcraft$leftPos() + offsetX, access.thaumcraft$topPos() + offsetY);
        }
    }
}
