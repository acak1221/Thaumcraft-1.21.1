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

/** ONLY the retained original node_stabilizer OBJ: lock28 + piston12 triangles.
 * No MTL, general loader, accepted-model changes or donor implementation. */
public final class ModelNodeStabilizer {
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private final Map<String,List<Vertex>> groups;
    public ModelNodeStabilizer(ResourceManager resources){
        var positions=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var normals=new ArrayList<float[]>();var parsed=new LinkedHashMap<String,List<Vertex>>();List<Vertex> current=null;
        try(BufferedReader reader=resources.getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/node_stabilizer.obj")).openAsReader()){
            String line;int lines=0,faces=0;
            while((line=reader.readLine())!=null){if(++lines>1024)throw new IOException("Original stabilizer OBJ line bound");line=line.trim();if(line.isEmpty() || line.startsWith("#"))continue;String[] fields=line.split("\\s+");switch(fields[0]){
                case "v" -> positions.add(vector(fields));
                case "vt" -> uv.add(vector(fields)); // Original u,v,w; w validated, not used.
                case "vn" -> normals.add(vector(fields));
                case "g" -> {if(fields.length!=2 || !Set.of("lock","piston").contains(fields[1]))throw new IOException("Unknown original stabilizer group");current=parsed.computeIfAbsent(fields[1],ignored->new ArrayList<>());}
                case "s" -> {if(fields.length!=2)throw new IOException("Invalid original smoothing directive");}
                case "f" -> {if(current==null || fields.length!=4 || ++faces>128)throw new IOException("Original stabilizer requires bounded triangles");for(int i=1;i<4;i++){String[] index=fields[i].split("/");if(index.length!=3)throw new IOException("Original position/UV/normal required");float[] p=at(positions,index[0]),t=at(uv,index[1]),n=at(normals,index[2]);current.add(new Vertex(p[0],p[1],p[2],t[0],1-t[1],n[0],n[1],n[2]));}}
                default -> throw new IOException("Unsupported original stabilizer directive: "+fields[0]);
            }if(positions.size()>128 || uv.size()>128 || normals.size()>128)throw new IOException("Original stabilizer OBJ table bound");}
            if(!parsed.keySet().equals(Set.of("lock","piston")) || parsed.get("lock").size()!=84 || parsed.get("piston").size()!=36)throw new IOException("Original stabilizer group/triangle counts changed");parsed.replaceAll((key,value)->List.copyOf(value));groups=Map.copyOf(parsed);
        }catch(IOException|IllegalArgumentException|IndexOutOfBoundsException error){throw new IllegalStateException("Unable to read retained original node_stabilizer OBJ",error);}
    }
    private static float[] vector(String[] fields)throws IOException{if(fields.length!=4)throw new IOException("Original stabilizer needs three-field vectors");float[] out=new float[3];for(int i=0;i<3;i++){out[i]=Float.parseFloat(fields[i+1]);if(!Float.isFinite(out[i]))throw new IOException("Nonfinite original coordinate");}return out;}
    private static float[] at(List<float[]> values,String raw)throws IOException{int index=Integer.parseInt(raw);if(index<=0 || index>values.size())throw new IOException("Invalid/nonpositive original OBJ index");return values.get(index-1);}
    public void render(String part,PoseStack pose,VertexConsumer vertices,int light,int overlay){List<Vertex> mesh=groups.get(part);if(mesh==null)throw new IllegalArgumentException("Unknown stabilizer part: "+part);for(Vertex v:mesh)vertices.addVertex(pose.last(),v.x,v.y,v.z).setColor(255,255,255,255).setUv(v.u,v.v).setOverlay(overlay).setLight(light).setNormal(pose.last(),v.nx,v.ny,v.nz);}
}
