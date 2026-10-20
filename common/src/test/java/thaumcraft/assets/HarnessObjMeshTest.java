package thaumcraft.assets;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.common.lib.geometry.HarnessObjMesh;

class HarnessObjMeshTest {
    @Test void retainedMeshHasItsActualCompleteTriangleAndUvData() throws Exception {
        Path path=Path.of("src/main/resources/assets/thaumcraft/textures/models/hoverharness.obj");
        if(!Files.exists(path))path=Path.of("common").resolve(path);
        var mesh=HarnessObjMesh.read(Files.newBufferedReader(path));assertEquals(124,mesh.triangles().size());
        var vertices=mesh.triangles().stream().flatMap(t->Stream.of(t.a(),t.b(),t.c())).toList();assertEquals(372,vertices.size());
        assertTrue(vertices.stream().allMatch(v->v.u()>=0&&v.u()<=1&&v.v()>=0&&v.v()<=1));
        assertEquals(2.25F,mesh.triangles().getFirst().a().x());assertEquals(-2F,mesh.triangles().getFirst().a().y());assertEquals(.375F,mesh.triangles().getFirst().a().v());
        assertTrue(vertices.stream().anyMatch(v->v.z()>10));assertTrue(vertices.stream().anyMatch(v->v.y()<-7));
    }
    @Test void brokenVerticesCannotProduceAnIncompleteSubstituteModel() {
        assertThrows(java.io.IOException.class,()->HarnessObjMesh.read(new StringReader("v NaN 0 0")));
        assertThrows(java.io.IOException.class,()->HarnessObjMesh.read(new StringReader("v 0 0 0\nf 1/1/1 1/1/1 1/1/1")));
        assertThrows(java.io.IOException.class,()->HarnessObjMesh.read(new StringReader("# no triangles")));
    }
}
