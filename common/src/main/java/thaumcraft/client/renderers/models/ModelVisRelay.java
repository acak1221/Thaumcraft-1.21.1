package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Bounded adapter for the retained original relay OBJ, not a general loader.
 * Relay draws RingBase32, RingFloat32 and Crystal16; the workbench charger also
 * draws Support36. Pinned OpenTC4 compared, not copied. */
public final class ModelVisRelay {
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private final Map<String,List<Vertex>> groups;
    public ModelVisRelay(ResourceManager resources){
        var positions=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var normals=new ArrayList<float[]>();var parsed=new LinkedHashMap<String,List<Vertex>>();String group="";
        try(BufferedReader reader=resources.openAsReader(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/vis_relay.obj"))){
            String line;while((line=reader.readLine())!=null){line=line.trim();if(line.isEmpty() || line.startsWith("#"))continue;String[] part=line.split("\\s+");switch(part[0]){
                case "v"->positions.add(new float[]{number(part[1]),number(part[2]),number(part[3])});
                case "vt"->uv.add(new float[]{number(part[1]),number(part[2])});
                case "vn"->normals.add(new float[]{number(part[1]),number(part[2]),number(part[3])});
                case "g"->{group=part[1];if(!Set.of("RingBase","RingFloat","Crystal","Support").contains(group))throw new IOException("Unexpected relay group "+group);parsed.computeIfAbsent(group,ignored->new ArrayList<>());}
                case "f"->{if(part.length!=4 || group.isEmpty())throw new IOException("Original relay must contain grouped triangles");for(int i=1;i<4;i++){String[] at=part[i].split("/");if(at.length!=3)throw new IOException("Missing original OBJ attributes");float[] p=positions.get(Integer.parseInt(at[0])-1),t=uv.get(Integer.parseInt(at[1])-1),n=normals.get(Integer.parseInt(at[2])-1);parsed.get(group).add(new Vertex(p[0],p[1],p[2],t[0],1-t[1],n[0],n[1],n[2]));}}
                case "s","o","mtllib","usemtl"->{} // No MTL/file loading or material substitution.
                default->throw new IOException("Unexpected retained relay record "+part[0]);
            }}
        }catch(IOException|RuntimeException failure){throw new IllegalStateException("Cannot read retained original vis_relay.obj",failure);}
        if(!parsed.keySet().equals(Set.of("RingBase","RingFloat","Crystal","Support")) || parsed.get("RingBase").size()!=96 || parsed.get("RingFloat").size()!=96 || parsed.get("Crystal").size()!=48 || parsed.get("Support").size()!=108)throw new IllegalStateException("Retained relay geometry changed");
        var immutable=new LinkedHashMap<String,List<Vertex>>();parsed.forEach((name,vertices)->immutable.put(name,List.copyOf(vertices)));groups=Map.copyOf(immutable);
    }
    private static float number(String text){float value=Float.parseFloat(text);if(!Float.isFinite(value))throw new IllegalArgumentException("Nonfinite original OBJ coordinate");return value;}
    public void render(String group,PoseStack pose,VertexConsumer out,int light,int overlay,int colour){
        if(!Set.of("RingBase","RingFloat","Crystal","Support").contains(group))throw new IllegalArgumentException("Not an original rendered relay part "+group);
        for(Vertex v:groups.get(group))out.addVertex(pose.last(),v.x,v.y,v.z).setColor(colour>>16&255,colour>>8&255,colour&255,255).setUv(v.u,v.v).setOverlay(overlay).setLight(light).setNormal(pose.last(),v.nx,v.ny,v.nz);
    }
}
