package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.BannerPose;
import thaumcraft.client.renderers.models.ModelBanner;

class BannerPoseParityTest {
    private static final float EPS = 1E-6F;

    @Test void equippedTranslationSurvivesTheSharedSlotFrame() {
        var ground = item(ItemDisplayContext.GROUND);
        for (var context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND}) {
            var hand = item(context);
            // Original ItemBannerRenderer bytecode32-35: +1 on each axis, scaled by native framing.
            assertEquals(ground.last().pose().m30() + .5F, hand.last().pose().m30(), EPS);
            assertEquals(ground.last().pose().m31() + .5F, hand.last().pose().m31(), EPS);
            assertEquals(ground.last().pose().m32() + .5F, hand.last().pose().m32(), EPS);
        }
    }

    @Test void theOriginalItemTurnsAroundYAndNativeFramingCentersItsActualMesh() {
        PoseStack pose = item(ItemDisplayContext.GUI);
        BannerPose.model(pose, 0, false);
        // Literal matrix fixture: original item Y180 followed by tile X180 at half native scale.
        assertVector(.5F, .7578125F, .5234375F, pose.last().pose().transformPosition(new Vector3f()));
        assertVector(0, .7578125F, .5234375F, pose.last().pose().transformPosition(new Vector3f(1, 0, 0)));
        assertVector(.5F, .2578125F, .5234375F, pose.last().pose().transformPosition(new Vector3f(0, 1, 0)));

        var model = new ModelBanner();
        var vertices = new Recorder();
        model.renderPole(pose, vertices, 0, 0);
        model.renderBeam(pose, vertices, 0, 0);
        model.renderTabs(pose, vertices, 0, 0);
        model.renderBanner(pose, vertices, 0, 0);
        for (int axis = 0; axis < 3; axis++) {
            final int component = axis;
            double min = vertices.points.stream().mapToDouble(point -> point[component]).min().orElseThrow();
            double max = vertices.points.stream().mapToDouble(point -> point[component]).max().orElseThrow();
            assertEquals(.5, (min + max) / 2, EPS, "resting mesh centered on axis " + axis);
        }
    }

    @Test void everyWorldFacingKeepsThePoleUpright() {
        for (int facing = 0; facing < 16; facing++) {
            PoseStack pose = new PoseStack();
            BannerPose.model(pose, facing, true);
            // A Y-facing turn must not tilt the pole. The previous Z turn tilted or inverted it.
            assertVector(.5F, .5F, .5F, pose.last().pose().transformPosition(new Vector3f(0, 1, 0)));
        }
        PoseStack east = new PoseStack();
        BannerPose.model(east, 4, true);
        assertVector(.5F, 1.5F, -.5F, east.last().pose().transformPosition(new Vector3f(1, 0, 0)));
    }

    @Test void aspectGlyphTurnsOverWithoutFlippingItsVerticalCoordinates() {
        PoseStack pose = new PoseStack();
        BannerPose.aspect(pose, 0);
        // Original TileBannerRenderer bytecode321-345 and UtilsFX.drawTag(-8,4).
        assertVector(.3F, .75F, .05001F, pose.last().pose().transformPosition(new Vector3f(-8, 20, 0)));
        assertVector(-.3F, .15F, .05001F, pose.last().pose().transformPosition(new Vector3f(8, 4, 0)));
    }

    private static PoseStack item(ItemDisplayContext context) {
        PoseStack pose = new PoseStack();
        BannerPose.item(pose, context);
        return pose;
    }

    private static void assertVector(float x, float y, float z, Vector3f actual) {
        assertEquals(x, actual.x, EPS);
        assertEquals(y, actual.y, EPS);
        assertEquals(z, actual.z, EPS);
    }

    private static final class Recorder implements VertexConsumer {
        final List<float[]> points = new ArrayList<>();
        @Override public VertexConsumer addVertex(float x, float y, float z) { points.add(new float[]{x, y, z}); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
