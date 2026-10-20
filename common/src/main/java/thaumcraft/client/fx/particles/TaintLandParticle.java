package thaumcraft.client.fx.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.common.entities.EntityFallingTaint;

/** Original slime-ball fragments: dark-purple, scale .4, 66/(worldFloat*.9+.1) life and fade. */
public final class TaintLandParticle extends BreakingItemParticle {
    private TaintLandParticle(ClientLevel level,double x,double y,double z){
        super(level,x,y,z,new ItemStack(Items.SLIME_BALL));setColor(.1F,0,.1F);scale(.4F);gravity=1F;
        lifetime=(int)(66F/(level.random.nextFloat()*.9F+.1F));
    }
    public static void add(EntityFallingTaint entity){
        if(!(entity.level() instanceof ClientLevel level))return;
        float angle=level.random.nextFloat()*(float)Math.PI*2F,radius=level.random.nextFloat()*.5F+.5F;
        double x=entity.getX()+Mth.sin(angle)*radius,z=entity.getZ()+Mth.cos(angle)*radius;
        Minecraft.getInstance().particleEngine.add(new TaintLandParticle(level,x,(entity.getBoundingBox().minY+entity.getBoundingBox().maxY)/2D,z));
    }
    @Override public void tick(){super.tick();alpha=1F-age/(float)lifetime;}
    @Override public float getQuadSize(float partial){return quadSize*(1F-age/(float)lifetime);}
}
