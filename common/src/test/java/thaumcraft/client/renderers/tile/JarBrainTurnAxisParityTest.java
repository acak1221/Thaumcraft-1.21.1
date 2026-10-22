package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * The turn of the brain of the original brain jar.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:166-167}:
 * {@code glRotatef(f3 * 180 / pi, 0, 1, 0)} and {@code glRotatef(-90, 0, 1, 0)}. The signature of
 * {@code glRotatef} is {@code (angle, x, y, z)}: the axis of both rotations is the Y axis, so the
 * brain turns around the vertical axis and is then laid on its side. A rotation around the Z axis
 * would tilt the brain out of the horizontal plane.</p>
 */
class JarBrainTurnAxisParityTest {
    @Test void theBrainTurnsAroundTheVerticalAxis() {
        for (float turn = -3.0F; turn <= 3.0F; turn += 0.37F) {
            PoseStack pose = new PoseStack();
            TileJarBrainRenderer.applyTurn(pose, turn);
            Vector3f rotated = new Vector3f(1F, 0F, 0F);
            pose.last().pose().transformPosition(rotated);
            assertEquals(0.0F, rotated.y, 1.0E-5F,
                    "the brain of the original turns in the horizontal plane, not " + rotated);
            // The rotation is not the identity: the brain really turns.
            assertTrue(Math.abs(rotated.x) + Math.abs(rotated.z) > 0.5F,
                    "the brain of the original really turns, not " + rotated);
        }
    }

    @Test void theBrainIsLaidOnItsSideByTheSecondRotation() {
        // The original glRotatef(-90, 0, 1, 0) lays the head on its side: the vertical
        // axis of the model becomes the horizontal axis of the jar.
        PoseStack pose = new PoseStack();
        TileJarBrainRenderer.applyTurn(pose, 0F);
        Vector3f rotated = new Vector3f(0F, 1F, 0F);
        pose.last().pose().transformPosition(rotated);
        assertEquals(0.0F, rotated.y, 1.0E-5F,
                "the second rotation of the original lays the brain on its side, not " + rotated);
    }
}
