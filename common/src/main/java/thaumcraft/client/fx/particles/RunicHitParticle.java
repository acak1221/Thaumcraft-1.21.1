package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;

/** Original FXShieldRunes: entity-midpoint following hemisphere,8..11ticks and fifteen animated retained frames. */
public final class RunicHitParticle extends Particle {
    private static final ResourceLocation MODEL=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/hemis.obj");
    private static List<Vertex[]> mesh;
    private record Vertex(float x,float y,float z,float u,float v) {}
    private final Entity target;
    private final float yaw,pitch;
    public RunicHitParticle(ClientLevel level,Entity target,float yaw,float pitch) {
        super(level,target.getX(),target.getBoundingBox().getCenter().y,target.getZ());
        this.target=target; this.yaw=yaw; this.pitch=pitch;
        hasPhysics=false; lifetime=8+random.nextInt(4); setSize(.01F,.01F);
        xo=x; yo=y; zo=z;
    }
    public static void resetMesh() { mesh=null; }
    private static List<Vertex[]> mesh() {
        if(mesh!=null) return mesh;
        var points=new ArrayList<float[]>(); var uvs=new ArrayList<float[]>(); var faces=new ArrayList<Vertex[]>();
        try(BufferedReader reader=Minecraft.getInstance().getResourceManager().openAsReader(MODEL)) {
            String line;
            while((line=reader.readLine())!=null) {
                String[] tokens=line.trim().split("\\s+");
                if(tokens[0].equals("v")) points.add(new float[]{Float.parseFloat(tokens[1]),Float.parseFloat(tokens[2]),Float.parseFloat(tokens[3])});
                else if(tokens[0].equals("vt")) uvs.add(new float[]{Float.parseFloat(tokens[1]),1-Float.parseFloat(tokens[2])});
                else if(tokens[0].equals("f")) {
                    Vertex[] polygon=new Vertex[tokens.length-1];
                    for(int i=1;i<tokens.length;i++) {
                        String[] index=tokens[i].split("/"); var p=points.get(Integer.parseInt(index[0])-1); var uv=uvs.get(Integer.parseInt(index[1])-1);
                        polygon[i-1]=new Vertex(p[0],p[1],p[2],uv[0],uv[1]);
                    }
                    for(int i=1;i<polygon.length-1;i++) faces.add(new Vertex[]{polygon[0],polygon[i],polygon[i+1],polygon[i+1]});
                }
            }
        } catch(IOException|RuntimeException failure) { throw new IllegalStateException("Cannot read retained runic hemisphere",failure); }
        if(faces.isEmpty()) throw new IllegalStateException("Empty retained runic hemisphere");
        mesh=List.copyOf(faces); return mesh;
    }
    @Override public void tick() {
        xo=x; yo=y; zo=z;
        if(age++>=lifetime||target.isRemoved()) { remove(); return; }
        x=target.getX(); y=target.getBoundingBox().getCenter().y; z=target.getZ();
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer ignored,Camera camera,float partial) {
        float fade=(age+partial)/lifetime;
        int frame=Math.min(15,(int)(14F*fade)+1);
        var pose=new PoseStack(); var eye=camera.getPosition();
        pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z);
        pose.mulPose(Axis.YP.rotationDegrees(180-yaw)); pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        float scale=.4F*target.getBbHeight(); pose.scale(scale,scale,scale);
        float alpha=Math.min(1F,(1F-fade)*3F);
        VisNetRender.draw(VisNetRender.texture("models/hemis"+frame),GL11.GL_ONE,false,out->{
            for(Vertex[] face:mesh()) for(Vertex v:face) out.addVertex(pose.last(),v.x,v.y,v.z).setUv(v.u,v.v).setColor(1F,1F,1F,alpha);
        });
    }
}
