package thaumcraft.client;

import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.common.config.ThaumcraftColoredCandles;

public final class ThaumcraftColoredCandlesClient {
    private static boolean initialized;
    private ThaumcraftColoredCandlesClient() {}
    public static void init() {
        if(initialized)return;initialized=true;
        for(int i=0;i<ThaumcraftColoredCandles.COLORED_ITEMS.size();i++) {
            int color=ThaumcraftColoredCandles.color(i+1);var block=ThaumcraftColoredCandles.COLORED_BLOCKS.get(i);var item=ThaumcraftColoredCandles.COLORED_ITEMS.get(i);
            RenderTypeRegistry.register(RenderType.cutout(),block.get());
            ColorHandlerRegistry.registerBlockColors((state,world,pos,tint)->tint==0?color:-1,block);
            ColorHandlerRegistry.registerItemColors((stack,tint)->tint==0?0xff000000|color:-1,item);
        }
    }
}
