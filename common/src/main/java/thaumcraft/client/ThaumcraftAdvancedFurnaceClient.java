package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.particles.SlurryBubbleParticle;
import thaumcraft.client.renderers.tile.TileAlchemyFurnaceAdvancedRenderer;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;

/** No gameplay dependency on client class loading. */
public final class ThaumcraftAdvancedFurnaceClient {
    private ThaumcraftAdvancedFurnaceClient() {}
    public static void init() {
        BlockEntityRendererRegistry.register(ThaumcraftAdvancedFurnace.TILE.get(),TileAlchemyFurnaceAdvancedRenderer::new);
        BlockAdvancedAlchemyFurnace.setClientEffects((world,pos,random)-> {
            if(!(world instanceof ClientLevel level) || Minecraft.getInstance().level!=level) return;
            var options=Minecraft.getInstance().options.particles().get();
            // Original always births two bubbles; native minimal particle setting suppresses ambient effects.
            if(options==ParticleStatus.MINIMAL) return;
            bubble(level,pos.getX()+random.nextFloat(),pos.getY()+1,pos.getZ()+random.nextFloat(),random);
            if(random.nextInt(50)==0) level.playLocalSound(pos.getX()+random.nextFloat(),pos.getY()+1,pos.getZ()+random.nextFloat(),net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1F+random.nextFloat()*.1F,.9F+random.nextFloat()*.15F,false);
            int x=random.nextInt(2),z=random.nextInt(2);
            bubble(level,pos.getX()-.6+random.nextFloat()*.2+x*2,pos.getY()+2,pos.getZ()-.6+random.nextFloat()*.2+z*2,random);
        });
    }
    private static void bubble(ClientLevel level,double x,double y,double z,RandomSource random) {
        float size=.06F+random.nextFloat()*.06F;
        var bubble=new SlurryBubbleParticle(level,x,y,z,size);
        bubble.setColor(.6F-random.nextFloat()*.2F,0,.6F+random.nextFloat()*.2F);
        Minecraft.getInstance().particleEngine.add(bubble);
    }
}
