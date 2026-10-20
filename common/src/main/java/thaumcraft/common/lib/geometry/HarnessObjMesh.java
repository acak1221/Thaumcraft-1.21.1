package thaumcraft.common.lib.geometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Retained harness OBJ is a triangle-only v/vt/vn mesh. Parsing is independent
 * of rendering and never changes the retained model or gameplay random state. */
public record HarnessObjMesh(List<Triangle> triangles) {
    public record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    public record Triangle(Vertex a,Vertex b,Vertex c) {}
    public HarnessObjMesh {triangles=List.copyOf(triangles);}
    public static HarnessObjMesh read(Reader input) throws IOException {
        var positions=new ArrayList<float[]>();var texture=new ArrayList<float[]>();var normals=new ArrayList<float[]>();var faces=new ArrayList<Triangle>();
        try(var reader=new BufferedReader(input)) {
            String line;while((line=reader.readLine())!=null) {
                line=line.strip();if(line.isEmpty()||line.startsWith("#"))continue;String[] words=line.split("\\s+");
                switch(words[0]) {
                    case "v"->positions.add(vector(words,3));
                    case "vt"->texture.add(vector(words,2));
                    case "vn"->normals.add(vector(words,3));
                    case "f"->{if(words.length!=4)throw new IOException("Harness OBJ requires triangle faces");
                        faces.add(new Triangle(vertex(words[1],positions,texture,normals),vertex(words[2],positions,texture,normals),vertex(words[3],positions,texture,normals)));}
                    case "g","s","o","mtllib","usemtl"->{ }
                    default->throw new IOException("Unknown harness mesh command: "+words[0]);
                }
            }
        }catch(IndexOutOfBoundsException|NumberFormatException invalid) {throw new IOException("Invalid retained harness OBJ",invalid);}
        if(faces.isEmpty())throw new IOException("Empty retained harness mesh");return new HarnessObjMesh(faces);
    }
    private static float[] vector(String[] words,int count) throws IOException {
        if(words.length<count+1)throw new IOException("Short OBJ vector");float[] result=new float[count];
        for(int i=0;i<count;i++){result[i]=Float.parseFloat(words[i+1]);if(!Float.isFinite(result[i]))throw new IOException("Nonfinite OBJ vector");}return result;
    }
    private static Vertex vertex(String word,List<float[]> positions,List<float[]> texture,List<float[]> normals) throws IOException {
        String[] indices=word.split("/",-1);if(indices.length!=3)throw new IOException("Missing OBJ vertex/UV/normal index");
        float[] p=positions.get(Integer.parseInt(indices[0])-1),uv=texture.get(Integer.parseInt(indices[1])-1),n=normals.get(Integer.parseInt(indices[2])-1);
        return new Vertex(p[0],p[1],p[2],uv[0],1-uv[1],n[0],n[1],n[2]);
    }
}
