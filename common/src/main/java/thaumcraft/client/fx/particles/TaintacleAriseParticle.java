package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.blocks.BlockShockTrap;

/** Original slime-item quarter sprite, shrinking/alpha fade and independent support-block debris. */
public final class TaintacleAriseParticle extends BreakingItemParticle {
    private TaintacleAriseParticle(ClientLevel level,double x,double y,double z){super(level,x,y,z,new ItemStack(Items.SLIME_BALL));}
    @Override public float getQuadSize(float partial){return super.getQuadSize(partial)*(1F-age/(float)lifetime);}
    @Override public void render(VertexConsumer out,Camera camera,float partial){float original=alpha;alpha*=1F-age/(float)lifetime;try{super.render(out,camera,partial);}finally{alpha=original;}}
    public static void arise(EntityTaintacle entity){
        if(!(entity.level() instanceof ClientLevel level))return;
        var engine=Minecraft.getInstance().particleEngine;
        var support=new BlockPos(Mth.floor(entity.getX()),Mth.floor(entity.getY())-1,Mth.floor(entity.getZ()));
        for(int j=0;j<2F*entity.getBbHeight();j++){
            float angle=level.random.nextFloat()*(float)Math.PI*entity.getBbHeight(),radial=level.random.nextFloat()*.5F+.5F;
            float x=Mth.sin(angle)*entity.getBbHeight()*.25F*radial,z=Mth.cos(angle)*entity.getBbHeight()*.25F*radial;
            var particle=new TaintacleAriseParticle(level,entity.getX()+x,entity.getY(),entity.getZ()+z);
            particle.setColor(.4F,0,.4F);particle.scale(.5F);particle.setLifetime((int)(66F/(level.random.nextFloat()*.9F+.1F)));engine.add(particle);
            if(!BlockShockTrap.semanticAir(level.getBlockState(support))){
                angle=level.random.nextFloat()*(float)Math.PI*entity.getBbHeight();radial=level.random.nextFloat()*.5F+.5F;
                x=Mth.sin(angle)*entity.getBbHeight()*.25F*radial;z=Mth.cos(angle)*entity.getBbHeight()*.25F*radial;
                engine.add(new TerrainParticle(level,entity.getX()+x,entity.getY(),entity.getZ()+z,0,0,0,level.getBlockState(support),support));
            }
        }
    }
}
