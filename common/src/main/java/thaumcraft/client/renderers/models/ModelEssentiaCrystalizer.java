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
import net.minecraft.util.Mth;

/** Bounded adapter for the two retained original, triangulated, NO-MTL OBJs.
 * All crystalizer shell groups are used; ONLY vis_relay's Crystal is emitted.
 * This is not a replacement OBJ framework and never overwrites an accepted model. */
public final class ModelEssentiaCrystalizer {
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private final List<Vertex> shell, crystal;

    public ModelEssentiaCrystalizer(ResourceManager resources) {
        Map<String,List<Vertex>> body=load(resources,"crystalizer",Set.of("top","base"));
        var all=new ArrayList<Vertex>();all.addAll(body.get("top"));all.addAll(body.get("base"));shell=List.copyOf(all);
        crystal=load(resources,"vis_relay",Set.of("Crystal","RingFloat","RingBase","Support")).get("Crystal");
    }
    private static Map<String,List<Vertex>> load(ResourceManager resources,String name,Set<String> allowed) {
        var groups=new LinkedHashMap<String,List<Vertex>>();var positions=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var normals=new ArrayList<float[]>();List<Vertex> group=null;
        try(BufferedReader reader=resources.getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/"+name+".obj")).openAsReader()) {
            String line;int lines=0,faces=0;
            while((line=reader.readLine())!=null){if(++lines>4096)throw new IOException("Crystallizer OBJ exceeds bounded line count");line=line.trim();if(line.isEmpty() || line.startsWith("#"))continue;String[] fields=line.split("\\s+");
                switch(fields[0]) {
                    case "v" -> positions.add(vector(fields,3));
                    // Both original files use THREE-field vt (u,v,w); their
                    // renderer uses u,v only. Validate w too, never fake an MTL.
                    case "vt" -> uv.add(vector(fields,3));
                    case "vn" -> normals.add(vector(fields,3));
                    case "g" -> {if(fields.length!=2 || !allowed.contains(fields[1]))throw new IOException("Unknown crystallizer OBJ group");group=groups.computeIfAbsent(fields[1],ignored->new ArrayList<>());}
                    case "s" -> {if(fields.length!=2)throw new IOException("Invalid original smoothing directive");}
                    case "f" -> {if(group==null || fields.length!=4 || ++faces>1024)throw new IOException("Crystallizer OBJ needs bounded original triangles");for(int i=1;i<=3;i++){String[] indices=fields[i].split("/");if(indices.length!=3)throw new IOException("Position/UV/normal required");float[] p=at(positions,indices[0]),t=at(uv,indices[1]),n=at(normals,indices[2]);group.add(new Vertex(p[0],p[1],p[2],t[0],1-t[1],n[0],n[1],n[2]));}}
                    default -> throw new IOException("Unsupported crystallizer OBJ directive: "+fields[0]);
                }
                if(positions.size()>1024 || uv.size()>1024 || normals.size()>1024)throw new IOException("Crystallizer OBJ tables exceed bounded format");
            }
            if(!groups.keySet().equals(allowed) || groups.values().stream().anyMatch(List::isEmpty))throw new IOException("Missing original crystallizer OBJ group");groups.replaceAll((key,value)->List.copyOf(value));return Map.copyOf(groups);
        } catch(IOException|IllegalArgumentException|IndexOutOfBoundsException error){throw new IllegalStateException("Unable to read retained original "+name+" OBJ",error);}
    }
    private static float[] at(List<float[]> table,String raw)throws IOException{int index=Integer.parseInt(raw);if(index<=0 || index>table.size())throw new IOException("Invalid/nonpositive original OBJ index");return table.get(index-1);}
    private static float[] vector(String[] fields,int size)throws IOException{if(fields.length!=size+1)throw new IOException("Unexpected original OBJ vector width");float[] values=new float[size];for(int i=0;i<size;i++){values[i]=Float.parseFloat(fields[i+1]);if(!Float.isFinite(values[i]))throw new IOException("Nonfinite original OBJ coordinate");}return values;}
    public void renderShell(PoseStack pose,VertexConsumer vertices,int light,int overlay){render(shell,pose,vertices,light,overlay,1,1,1);}
    public void renderCrystal(PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue){render(crystal,pose,vertices,light,overlay,red,green,blue);}
    private static void render(List<Vertex> mesh,PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue){int r=Math.round(255*Mth.clamp(red,0,1)),g=Math.round(255*Mth.clamp(green,0,1)),b=Math.round(255*Mth.clamp(blue,0,1));for(Vertex v:mesh)vertices.addVertex(pose.last(),v.x,v.y,v.z).setColor(r,g,b,255).setUv(v.u,v.v).setOverlay(overlay).setLight(light).setNormal(pose.last(),v.nx,v.ny,v.nz);}
}
