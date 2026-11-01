package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import thaumcraft.client.renderers.models.ModelBrain;
import thaumcraft.client.renderers.models.ModelEldritchCap;
import thaumcraft.client.renderers.models.ModelJar;
import thaumcraft.client.renderers.models.ModelAlembic;
import thaumcraft.client.renderers.models.ModelEssentiaReservoir;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Original default inventory tiles must contain their real animated meshes, not placeholder block boxes. */
class SpecialBlockInventoryGeometryTest {
    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void allThreeBrainPartsStayInsideTheOriginalBrineAndGlass() {
        Bounds brine = new Bounds();
        var content = new PoseStack();
        TileJarBrainRenderer.contentPose(content);
        new ModelJar().renderBrine(content, brine, 0, 0);
        assertEquals(24, brine.count);
        assertEquals(.25F, brine.min[0], 1E-6F);
        assertEquals(.75F, brine.max[0], 1E-6F);
        assertEquals(.0725F, brine.min[1], 1E-6F);
        assertEquals(.6975F, brine.max[1], 1E-6F);
        for (int ticks = 0; ticks < 180; ticks++) {
            var pose = new PoseStack();
            TileJarBrainRenderer.contentPose(pose);
            TileJarBrainRenderer.brainPose(pose, ticks, 0);
            Bounds brain = new Bounds();
            new ModelBrain().render(pose, brain, 0, 0);
            assertEquals(72, brain.count, "the original three brain shapes must all be drawn");
            for (int axis = 0; axis < 3; axis++) {
                assertTrue(brain.min[axis] >= brine.min[axis], "brain below brine at tick " + ticks);
                assertTrue(brain.max[axis] <= brine.max[axis], "brain outside brine at tick " + ticks);
            }
        }
    }

    @Test void scrubberObjRetainsAllCapAndTipTrianglesInTheOriginalDefaultFacing() {
        assertEquals(VertexFormat.Mode.TRIANGLES, TileFluxScrubberRenderer.geometryType().mode(),
                "QUADS regroup the original triangle vertices into unrelated surfaces");
        var pose = new PoseStack();
        TileFluxScrubberRenderer.basePose(pose, Direction.DOWN);
        var cap = new Bounds();
        var model = new ModelEldritchCap();
        model.renderCapGroup(pose, cap, 0, 0, -1);
        assertEquals(20 * 3, cap.count);
        assertEquals(0, cap.min[0], 1E-6F);
        assertEquals(1, cap.max[0], 1E-6F);
        assertEquals(0, cap.min[1], 1E-6F);
        assertEquals(.8F, cap.max[1], 1E-6F);
        var tip = new Bounds();
        pose.translate(0, 0, -TileFluxScrubberRenderer.bobFor(0));
        model.renderTipGroup(pose, tip, 0, 0, -1);
        assertEquals(12 * 3, tip.count);
        assertEquals(.725F, tip.min[1], 1E-6F);
        assertEquals(.925F, tip.max[1], 1E-6F);
        assertEquals(.375F, tip.min[0], 1E-6F);
        assertEquals(.625F, tip.max[0], 1E-6F);
    }

    @Test void reservoirUsesEveryOriginalObjTriangleAroundTheGlassVessel() {
        var pose = new PoseStack();
        TileEssentiaReservoirRenderer.framePose(pose, Direction.DOWN);
        var frame = new Bounds();
        new ModelEssentiaReservoir(resources()).render(pose, frame, 0, 0);
        assertEquals(236 * 3, frame.count);
        assertEquals(-.0044F, frame.min[0], 1E-5F);
        assertEquals(1.0044F, frame.max[0], 1E-5F);
        assertEquals(0, frame.min[1], 1E-5F);
        assertEquals(.9375F, frame.max[1], 1E-5F);
    }

    @Test void inventoryAlembicUsesTheOriginalUnconnectedGroupsAndNotTheSupportTubes() {
        var vertices = new Bounds();
        TileAlembicRenderer.renderInventoryModel(new ModelAlembic(resources()), new PoseStack(), vertices, 0, 0);
        // Original no-world branch renders Legs60 + Pot64 + Panel12; it omits the two tube groups.
        assertEquals((60 + 64 + 12) * 3, vertices.count);
        assertEquals(.06F, vertices.min[0], 1E-5F);
        assertEquals(.94F, vertices.max[0], 1E-5F);
        assertTrue(vertices.min[1] < 0, "the original inventory branch lowers the Z-up mesh by0.4");
    }

    private static ResourceManager resources() {
        return new ResourceManager() {
            @Override public Optional<Resource> getResource(ResourceLocation location) {
                String path = "assets/" + location.getNamespace() + "/" + location.getPath();
                if (getClass().getClassLoader().getResource(path) == null) return Optional.empty();
                return Optional.of(new Resource(null, () -> getClass().getClassLoader().getResourceAsStream(path)));
            }
            @Override public Set<String> getNamespaces() { return Set.of("thaumcraft"); }
            @Override public List<Resource> getResourceStack(ResourceLocation location) { return getResource(location).stream().toList(); }
            @Override public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) { return Map.of(); }
            @Override public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) { return Map.of(); }
            @Override public Stream<net.minecraft.server.packs.PackResources> listPacks() { return Stream.empty(); }
        };
    }

    private static final class Bounds implements VertexConsumer {
        final float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        final float[] max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        int count;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            float[] point = {x, y, z};
            for (int axis = 0; axis < 3; axis++) { min[axis] = Math.min(min[axis], point[axis]); max[axis] = Math.max(max[axis], point[axis]); }
            count++; return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
