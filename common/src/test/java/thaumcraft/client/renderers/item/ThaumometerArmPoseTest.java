package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.junit.jupiter.api.Test;

/**
 * The scanner arm is drawn from the player renderer's model, which every player shares. Its pose
 * must come from the original renderFirstPersonArm (swing reset, no motion), never from the last
 * player that model was set up for: a running player in multiplayer shook everyone's scanner arms.
 */
class ThaumometerArmPoseTest {
    private static final float BOB = 0.1F; // cos(0) * 0.05 + 0.05

    private static ModelPart arm(PartPose initial) {
        ModelPart part = new ModelPart(List.of(), Map.of());
        part.setInitialPose(initial);
        part.resetPose();
        return part;
    }

    private static void assertPose(ModelPart part, float x, float y, float z, float xRot, float yRot, float zRot) {
        assertEquals(x, part.x, 1e-6F, "x");
        assertEquals(y, part.y, 1e-6F, "y");
        assertEquals(z, part.z, 1e-6F, "z");
        assertEquals(xRot, part.xRot, 1e-6F, "xRot");
        assertEquals(yRot, part.yRot, 1e-6F, "yRot");
        assertEquals(zRot, part.zRot, 1e-6F, "zRot");
    }

    @Test void restingArmIsTheOriginalNoMotionPose() {
        ModelPart part = arm(PartPose.offset(-5.0F, 2.0F, 0.0F));
        ItemThaumometerRenderer.firstPersonArmPose(part);
        assertPose(part, -5.0F, 2.0F, 0.0F, 0.0F, 0.0F, BOB);
    }

    @Test void poseOfAnotherPlayerDoesNotLeakIn() {
        ModelPart still = arm(PartPose.offset(-5.0F, 2.0F, 0.0F));
        ItemThaumometerRenderer.firstPersonArmPose(still);
        // What HumanoidModel.setupAnim leaves on the shared arm for a running, swinging player.
        ModelPart running = arm(PartPose.offset(-5.0F, 2.0F, 0.0F));
        running.setRotation(-0.92F, 0.31F, 0.27F);
        running.setPos(-4.1F, 2.0F, 1.6F);
        ItemThaumometerRenderer.firstPersonArmPose(running);
        assertPose(running, still.x, still.y, still.z, still.xRot, still.yRot, still.zRot);
        // Drawn every frame: the pose must not accumulate.
        ItemThaumometerRenderer.firstPersonArmPose(running);
        assertPose(running, still.x, still.y, still.z, still.xRot, still.yRot, still.zRot);
    }

    @Test void slimArmKeepsItsOwnPivot() {
        ModelPart slim = arm(PartPose.offset(-5.0F, 2.5F, 0.0F));
        slim.setRotation(0.4F, 0.0F, -0.2F);
        ItemThaumometerRenderer.firstPersonArmPose(slim);
        assertPose(slim, -5.0F, 2.5F, 0.0F, 0.0F, 0.0F, BOB);
    }
}
