package thaumcraft.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import thaumcraft.client.gui.GuiAccessories;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.network.AccessoryNetworking;

public final class ThaumcraftBaublesClient {
    private static boolean initialized;
    private ThaumcraftBaublesClient(){}
    public static void init(){
        if(initialized)return;initialized=true;
        AccessoryNetworking.initClient();
        dev.architectury.event.events.client.ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
                access.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        net.minecraft.network.chat.Component.translatable("gui.thaumcraft.accessories.button"),
                        button -> AccessoryNetworking.requestOpen())
                        .bounds(screen.width - 28, 8, 20, 20)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                                net.minecraft.network.chat.Component.translatable("container.thaumcraft.accessories")))
                        .build());
        });
        MenuRegistry.registerScreenFactory(ThaumcraftBaubles.MENU.get(),GuiAccessories::new);
        for(var entry:ThaumcraftBaubles.DISCOUNT_RINGS.entrySet()) {
            int color=thaumcraft.api.aspects.Aspect.getAspect(entry.getKey()).getColor();
            ColorHandlerRegistry.registerItemColors((stack,tint)->tint==0?0xff000000|color:-1,entry.getValue());
        }
    }
}
