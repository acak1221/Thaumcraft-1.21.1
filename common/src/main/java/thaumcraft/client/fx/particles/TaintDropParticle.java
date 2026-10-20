package thaumcraft.client.fx.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;

/** Native drip sprites replacing removed vanilla113/112/114 cells; original taint drip physics and colour. */
public final class TaintDropParticle extends TextureSheetParticle {
    private int bob=40;
    private TaintDropParticle(ClientLevel level,Vec3 point){
        super(level,point.x,point.y,point.z);xd=yd=zd=0;setColor(.3F,.1F,.8F);gravity=.06F;
        lifetime=(int)(64D/(Math.random()*.8D+.2D));frame("drip_hang");
    }
    public static void add(Level world,Vec3 point){if(world instanceof ClientLevel level)Minecraft.getInstance().particleEngine.add(new TaintDropParticle(level,point));}
    private static boolean jar(BlockState state){return state.is(ThaumcraftContent.JAR.get())||state.is(ThaumcraftContent.VOID_JAR.get());}
    private void frame(String sprite){setSprite(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_PARTICLES).apply(ResourceLocation.withDefaultNamespace(sprite)));}
    @Override public void tick(){
        xo=x;yo=y;zo=z;yd-=(double)gravity;
        if(bob-->0){xd*=.02D;yd*=.02D;zd*=.02D;frame("drip_hang");}else frame("drip_fall");
        move(xd,yd,zd);xd*=(double).98F;yd*=(double).98F;zd*=(double).98F;
        if(lifetime--<=0)remove();if(onGround){frame("drip_land");xd*=(double).7F;zd*=(double).7F;}
        var pos=BlockPos.containing(x,y,z);var state=level.getBlockState(pos);
        if(!jar(state)&&(state.isSolid()||state.liquid())){
            float surface=state.getFluidState().isEmpty()?1F:state.getFluidState().getHeight(level,pos);
            if(y<pos.getY()+surface)remove();
        }
    }
    @Override public void move(double dx,double dy,double dz){
        var pos=BlockPos.containing(x,y,z);
        if(jar(level.getBlockState(pos))){
            setBoundingBox(getBoundingBox().move(dx,dy,dz));setLocationFromBoundingbox();
            if(jar(level.getBlockState(BlockPos.containing(x,y+1,z)))){setPos(xo,yo,zo);yd=0;onGround=true;}
        }else super.move(dx,dy,dz);
    }
    @Override protected int getLightColor(float partial){return 257;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
}
