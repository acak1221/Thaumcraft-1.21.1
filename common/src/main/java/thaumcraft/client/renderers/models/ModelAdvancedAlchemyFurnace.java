package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bundled original OBJ vertices, UVs, normals and groups; triangles become degenerate quads for native entity buffers. */
public final class ModelAdvancedAlchemyFurnace {
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private static final Map<String,List<Vertex>> GROUPS=load();
    private static Map<String,List<Vertex>> load() {
        var positions=new ArrayList<float[]>(); var uv=new ArrayList<float[]>(); var normals=new ArrayList<float[]>();
        var groups=new LinkedHashMap<String,List<Vertex>>(); String group=null;
        var stream=ModelAdvancedAlchemyFurnace.class.getClassLoader().getResourceAsStream("assets/thaumcraft/textures/models/adv_alch_furnace.obj");
        if(stream==null) throw new IllegalStateException("Missing original advanced furnace OBJ");
        try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
            String row;
            while((row=reader.readLine())!=null) {
                String[] words=row.trim().split("\\s+"); if(words.length==0) continue;
                switch(words[0]) {
                    case "v" -> positions.add(new float[]{Float.parseFloat(words[1]),Float.parseFloat(words[2]),Float.parseFloat(words[3])});
                    case "vt" -> uv.add(new float[]{Float.parseFloat(words[1]),1F-Float.parseFloat(words[2])});
                    case "vn" -> normals.add(new float[]{Float.parseFloat(words[1]),Float.parseFloat(words[2]),Float.parseFloat(words[3])});
                    case "g" -> { group=words[1]; groups.putIfAbsent(group,new ArrayList<>()); }
                    case "f" -> {
                        if(group==null || words.length!=4) throw new IllegalStateException("Original furnace requires grouped triangles");
                        var triangle=new ArrayList<Vertex>(3);
                        for(int i=1;i<=3;i++) {
                            String[] index=words[i].split("/"); float[] p=positions.get(Integer.parseInt(index[0])-1),t=uv.get(Integer.parseInt(index[1])-1),n=normals.get(Integer.parseInt(index[2])-1);
                            triangle.add(new Vertex(p[0],p[1],p[2],t[0],t[1],n[0],n[1],n[2]));
                        }
                        groups.get(group).addAll(triangle); groups.get(group).add(triangle.get(2));
                    }
                    default -> {}
                }
            }
        } catch(Exception failure) { throw new IllegalStateException("Cannot read original advanced furnace OBJ",failure); }
        if(groups.size()!=2 || groups.get("Base").size()!=116*4 || groups.get("Tank").size()!=64*4)
            throw new IllegalStateException("Original furnace geometry/group counts changed");
        var frozen=new LinkedHashMap<String,List<Vertex>>(); groups.forEach((name,vertices)->frozen.put(name,List.copyOf(vertices))); return Map.copyOf(frozen);
    }
    public void render(String group,PoseStack pose,VertexConsumer output,int light,int overlay) {
        for(Vertex vertex:GROUPS.get(group)) output.addVertex(pose.last(),vertex.x,vertex.y,vertex.z).setColor(-1).setUv(vertex.u,vertex.v)
                .setOverlay(overlay).setLight(light).setNormal(pose.last(),vertex.nx,vertex.ny,vertex.nz);
    }
}
