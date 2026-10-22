package thaumcraft.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.client.fx.particles.TaintDropParticle;
import thaumcraft.client.fx.particles.TaintLandParticle;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.EntityFallingTaint;

public final class ThaumcraftTaintBlocksClient {
    private ThaumcraftTaintBlocksClient(){}
    public static void init(){
        RenderTypeRegistry.register(RenderType.translucent(),ThaumcraftTaint.FIBRES.get());
        ColorHandlerRegistry.registerBlockColors((state,world,pos,tint)->world!=null&&pos!=null?BiomeColors.getAverageGrassColor(world,pos):7160201,ThaumcraftTaint.SOIL.get(),ThaumcraftTaint.FIBRES.get());
        ColorHandlerRegistry.registerItemColors((stack,tint)->0xff6d4189,ThaumcraftTaint.SOIL.get().asItem(),ThaumcraftTaint.fibreItem(0),ThaumcraftTaint.fibreItem(1),ThaumcraftTaint.fibreItem(2),ThaumcraftTaint.fibreItem(3),ThaumcraftTaint.fibreItem(4));
        BlockTaint.setClientEffects(TaintDropParticle::add);EntityFallingTaint.setClientEffects(TaintLandParticle::add);
    }
}
