package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Original slime-ball quarter-icon FXBreaking, tinted purple, physical gravity
 * and lifetime66/(worldFloat*.9+.1); color/size/alpha fade are independent. */
public final class TaintSplooshParticle extends BreakingItemParticle {
    private TaintSplooshParticle(ClientLevel level,double x,double y,double z){super(level,x,y,z,new ItemStack(Items.SLIME_BALL));}
    public static void emit(Entity target,int count){
        if(!(target.level() instanceof ClientLevel level)||Minecraft.getInstance().level!=level)return;
        for(int i=0;i<count;i++){
            float angle=level.random.nextFloat()*(float)Math.PI*2F,radius=level.random.nextFloat()*.5F+.5F;
            float dx=Mth.sin(angle)*2F*.5F*radius,dz=Mth.cos(angle)*2F*.5F*radius;
            var fx=new TaintSplooshParticle(level,target.getX()+(double)dx,target.getY()+(double)(level.random.nextFloat()*target.getBbHeight()),target.getZ()+(double)dz);
            if(level.random.nextBoolean()){fx.setColor(.6F,0,.3F);fx.scale(.4F);}else{fx.setColor(.3F,0,.3F);fx.scale(.6F);}
            fx.lifetime=(int)(66F/(level.random.nextFloat()*.9F+.1F));Minecraft.getInstance().particleEngine.add(fx);
        }
    }
    @Override public float getQuadSize(float partial){return super.getQuadSize(partial)*(1F-age/(float)lifetime);}
    @Override public void render(VertexConsumer out,Camera camera,float partial){float before=alpha;alpha=before*(1F-age/(float)lifetime);try{super.render(out,camera,partial);}finally{alpha=before;}}
}
