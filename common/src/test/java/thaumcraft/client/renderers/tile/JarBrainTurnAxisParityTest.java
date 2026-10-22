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
 * brain turns around the vertical axis and is then turned by ninety degrees around the very same
 * vertical axis. The second rotation does <strong>not</strong> lay the brain on its side: the
 * vertical axis of the model stays the vertical axis and only the heading of the brain turns.</p>
 *
 * <p>The bytecode of the original jar agrees: the two calls are
 * {@code GL11.glRotatef:(FFFF)V} with the arguments {@code (angle, 0.0, 1.0, 0.0)} and
 * {@code (-90.0, 0.0, 1.0, 0.0)}.</p>
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

    @Test void theSecondRotationTurnsTheHeadingAroundTheVerticalAxis() {
        // The original glRotatef(-90, 0, 1, 0) is a turn of ninety degrees around the
        // vertical axis. The vertical axis of the model is the axis of the turn, so it
        // stays the vertical axis of the jar, and the heading of the brain turns.
        PoseStack pose = new PoseStack();
        TileJarBrainRenderer.applyTurn(pose, 0F);
        Vector3f vertical = new Vector3f(0F, 1F, 0F);
        pose.last().pose().transformPosition(vertical);
        assertEquals(1.0F, vertical.y, 1.0E-5F,
                "the vertical axis of the original brain stays vertical, not " + vertical);
        assertEquals(0.0F, vertical.x, 1.0E-5F,
                "the vertical axis of the original brain stays vertical, not " + vertical);
        assertEquals(0.0F, vertical.z, 1.0E-5F,
                "the vertical axis of the original brain stays vertical, not " + vertical);
        Vector3f heading = new Vector3f(1F, 0F, 0F);
        pose.last().pose().transformPosition(heading);
        assertEquals(0.0F, heading.x, 1.0E-5F,
                "the heading of the brain turns by ninety degrees around the vertical axis, not " + heading);
        assertEquals(0.0F, heading.y, 1.0E-5F,
                "the heading of the brain stays in the horizontal plane, not " + heading);
        assertEquals(1.0F, heading.z, 1.0E-5F,
                "the heading of the brain turns by ninety degrees around the vertical axis, not " + heading);
    }
}
