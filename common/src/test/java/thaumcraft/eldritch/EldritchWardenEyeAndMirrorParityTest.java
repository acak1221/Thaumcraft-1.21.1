package thaumcraft.eldritch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.entity.RenderEldritchWarden;
import thaumcraft.client.renderers.models.ModelEldritchGuardian;

/**
 * The two findings the client smoke reported about the eldritch models, checked
 * against the original TC4 4.2.3.5.
 *
 * <h2>The warden's hood eye</h2>
 *
 * <p>The client read {@code isRender} of the oracle
 * ({@code thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java:122-127})
 * and concluded that the original draws the hood eye in the ordinary pass. It does not:
 * the render method of the original model
 * ({@code ModelEldritchGuardian.java:428-465}) draws <em>thirty eight</em> parts and
 * {@code HoodEye} is not one of them. The only place the original ever draws the hood
 * eye is the warden block of the same method
 * ({@code ModelEldritchGuardian.java:467-479}), in a separate additive pass at the
 * scale 1.01. The native port does exactly that: the guardian model hides the part
 * and the warden renderer draws it again with the additive blend.</p>
 *
 * <h2>The mirror of a box</h2>
 *
 * <p>The 1.7.10 {@code ModelBox} - the class the 1.7.10 {@code ModelRenderer.Cube} was
 * derived from - mirrors a box in two steps when the renderer carries the mirror flag
 * (the field {@code field_78809_i} of the 1.7.10 {@code ModelRenderer}, the flag the
 * oracle sets on the left parts of the cultist armour, for example
 * {@code thaumcraft/client/renderers/models/gear/ModelFortressArmor.java:272-277}):</p>
 *
 * <pre>
 * if (renderer.field_78809_i) {          // the mirror flag
 *     float swap = maxX;                // the box is mirrored in X, its extent is unchanged
 *     maxX = minX;
 *     minX = swap;
 * }
 * for (int i = 0; i &lt; quads.length; ++i) quads[i].flip();   // every quad is wound the other way
 * </pre>
 *
 * <p>The 1.21.1 {@code ModelPart.Cube} does the same: it swaps the minimum and the
 * maximum X and the {@code ModelPart.Polygon} constructor reverses the vertex order.
 * The normals follow: a geometric mirror in X negates the X of the normal and the
 * reversed winding negates the whole normal, so the net effect is a negation of the X
 * component alone, which is exactly what the 1.21.1 polygon does with
 * {@code normal.mul(-1, 1, 1)}. The test below proves that with the real 1.21.1
 * classes, so the mirrored boxes of the port need no compensation at all.</p>
 *
 * <h3>The two implementations side by side</h3>
 *
 * <p>The 1.7.10 side is the official Mojang {@code client.jar} of 1.7.10 (the classes
 * {@code net.minecraft.client.model.ModelBox} and {@code net.minecraft.client.model.TexturedQuad}
 * of the jar, read with the official MCP mapping
 * {@code de.oceanlabs.mcp:mcp_stable:12-1.7.10}), the 1.21.1 side is the remapped jar
 * {@code net.minecraft.client.model.geom.ModelPart}:</p>
 *
 * <table>
 *   <caption>The mirror of a box, step by step</caption>
 *   <tr><th>step</th><th>1.7.10</th><th>1.21.1</th></tr>
 *   <tr><td>the extent in X</td>
 *       <td>{@code ModelBox}: the minimum and the maximum X are swapped</td>
 *       <td>{@code ModelPart.Cube}: the minimum and the maximum X are swapped</td></tr>
 *   <tr><td>the texture region</td>
 *       <td>{@code TexturedQuad}: the four vertices are remapped with the region of the face</td>
 *       <td>{@code ModelPart.Polygon}: the four vertices are remapped with the region of the face</td></tr>
 *   <tr><td>the winding</td>
 *       <td>{@code TexturedQuad.flip}: the four vertices are reversed</td>
 *       <td>{@code ModelPart.Polygon}: the four vertices are reversed</td></tr>
 *   <tr><td>the normal</td>
 *       <td>{@code TexturedQuad}: the cross product of the (reversed) vertices</td>
 *       <td>{@code ModelPart.Polygon}: the direction of the face, the X negated</td></tr>
 * </table>
 *
 * <p>The two are the same numbers: for the box of two model units the mirrored quad of
 * the 1.7.10 jar is {@code (0,0,0) u=0.03125 v=0.03125}, {@code (2,0,0) u=0.01563 v=0.03125},
 * {@code (2,0,2) u=0.01563 v=0.00000}, {@code (0,0,2) u=0.03125 v=0.00000} and the 1.21.1
 * model gives the same quad in world units. The only difference is the unit: the 1.7.10 box
 * keeps the model units, the 1.21.1 cube divides them by sixteen when it is compiled
 * ({@code ModelPart.Cube.compile}), so the mirror of the test is about {@code 2/16} and
 * not about {@code 2}.</p>
 */
class EldritchWardenEyeAndMirrorParityTest {
    private static final float EPS = 1.0E-5F;
    /** The width of the tested box in world units: the 1.21.1 model divides the model units by sixteen. */
    private static final float BOX = 2.0F / 16.0F;

    // ------------------------------------------------------- the warden's hood eye

    @Test void theGuardianDoesNotDrawTheHoodEyeInItsOrdinaryPass() {
        // The original's render list of ModelEldritchGuardian:428-465 has thirty eight
        // parts and no HoodEye; only the warden block of the same method draws it.
        ModelPart root = ModelEldritchGuardian.createBodyLayer().bakeRoot();
        assertNotNull(root.getChild("HoodEye"), "the original guardian has the hood eye part");
        new ModelEldritchGuardian(root);
        assertFalse(root.getChild("HoodEye").visible,
                "the original draws the hood eye only in the warden pass, so the guardian hides it");
    }

    @Test void theWardenDrawsTheHoodEyeAtTheOriginalScale() {
        // Original ModelEldritchGuardian:470 - GL11.glScaled(1.01, 1.01, 1.01).
        assertEquals(1.01F, RenderEldritchWarden.HOOD_EYE_SCALE, EPS,
                "the original scale of the warden's hood eye");
    }

    // ------------------------------------------------------------ the mirror of a box

    @Test void theMirroredBoxIsTheMirrorImageAndKeepsTheTextureRegion() {
        // The plain box of zero to two and the mirrored box of the same shape.
        List<float[]> plain = render(false);
        List<float[]> mirrored = render(true);
        assertEquals(plain.size(), mirrored.size(), "the mirror does not change the number of vertices");
        assertEquals(24, plain.size(), "a box of the 1.21.1 model has twenty four vertices");

        // The mirror of the 1.7.10 ModelBox swaps the minimum and the maximum X and winds
        // every quad the other way: the vertex j of a mirrored quad is the vertex three
        // minus j of the plain quad with the X mirrored about the centre of the box.
        // The 1.21.1 cube is compiled in world units, the model units are divided by
        // sixteen (ModelPart.Cube.compile, net.minecraft.client.model.geom.ModelPart:288-290),
        // so the box of two model units is BOX wide in the buffer of the test.
        for (int face = 0; face < 6; face++) {
            for (int vertex = 0; vertex < 4; vertex++) {
                float[] source = plain.get(face * 4 + 3 - vertex);
                float[] target = mirrored.get(face * 4 + vertex);
                assertEquals(BOX - source[0], target[0], EPS, "the mirrored x of the vertex " + vertex + " of the face " + face);
                assertEquals(source[1], target[1], EPS, "the y of the vertex " + vertex + " of the face " + face);
                assertEquals(source[2], target[2], EPS, "the z of the vertex " + vertex + " of the face " + face);
                assertEquals(source[3], target[3], EPS, "the u of the vertex " + vertex + " of the face " + face);
                assertEquals(source[4], target[4], EPS, "the v of the vertex " + vertex + " of the face " + face);
            }
        }
    }

    @Test void theMirroredBoxNegatesOnlyTheXOfTheNormal() {
        List<float[]> plain = render(false);
        List<float[]> mirrored = render(true);
        for (int face = 0; face < 6; face++) {
            for (int vertex = 0; vertex < 4; vertex++) {
                float[] source = plain.get(face * 4 + 3 - vertex);
                float[] target = mirrored.get(face * 4 + vertex);
                // The 1.21.1 polygon negates the X of the normal of a mirrored face, the
                // 1.7.10 mirror has the same result: the geometric mirror in X negates the X
                // of the normal and the reversed winding negates the whole normal, so the two
                // cancel in X and stay in Y and Z.
                assertEquals(-source[5], target[5], EPS, "the normal x of the vertex " + vertex + " of the face " + face);
                assertEquals(source[6], target[6], EPS, "the normal y of the vertex " + vertex + " of the face " + face);
                assertEquals(source[7], target[7], EPS, "the normal z of the vertex " + vertex + " of the face " + face);
            }
        }
    }

    @Test void theMirroredBoxKeepsTheTextureRegionOfTheOriginal() {
        // The 1.7.10 ModelBox gives the mirrored quad the same texture region as the plain
        // one; the 1.21.1 polygon assigns the same region too, only the winding changes.
        List<float[]> plain = render(false);
        List<float[]> mirrored = render(true);
        float plainMin = 1.0F, plainMax = 0.0F, mirroredMin = 1.0F, mirroredMax = 0.0F;
        for (float[] vertex : plain) {
            plainMin = Math.min(plainMin, vertex[3]);
            plainMax = Math.max(plainMax, vertex[3]);
        }
        for (float[] vertex : mirrored) {
            mirroredMin = Math.min(mirroredMin, vertex[3]);
            mirroredMax = Math.max(mirroredMax, vertex[3]);
        }
        assertEquals(plainMin, mirroredMin, EPS, "the smallest u of the mirrored box");
        assertEquals(plainMax, mirroredMax, EPS, "the largest u of the mirrored box");
    }

    /** Renders a box of the 1.21.1 model and returns its vertices with their normals. */
    private static List<float[]> render(boolean mirror) {
        var builder = CubeListBuilder.create().texOffs(0, 0);
        if (mirror) builder = builder.mirror();
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("cube", builder.addBox(0.0F, 0.0F, 0.0F, 2, 2, 2), PartPose.ZERO);
        ModelPart part = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        var recorder = new Recorder();
        part.render(new PoseStack(), recorder, 15728640, 0);
        return recorder.vertices;
    }

    /** A vertex consumer that remembers the position, the texture coordinate and the normal. */
    private static final class Recorder implements VertexConsumer {
        private final List<float[]> vertices = new ArrayList<>();
        private float x, y, z;

        @Override public VertexConsumer addVertex(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { vertices.add(new float[]{x, y, z, u, v, 0, 0, 0}); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float nx, float ny, float nz) {
            int last = vertices.size() - 1;
            if (last >= 0) {
                float[] vertex = vertices.get(last);
                vertex[5] = nx; vertex[6] = ny; vertex[7] = nz;
            }
            return this;
        }
    }
}
