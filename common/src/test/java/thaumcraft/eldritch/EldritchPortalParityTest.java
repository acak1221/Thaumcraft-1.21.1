package thaumcraft.eldritch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.CameraBasis;
import thaumcraft.client.renderers.entity.RenderCultistPortal;
import thaumcraft.client.renderers.tile.TileEldritchCrystalRenderer;
import thaumcraft.client.renderers.tile.TileEldritchPortalRenderer;

/**
 * The camera basis and the two portal quads of the original TC4 4.2.3.5.
 *
 * <p>The oracle is the decompiled jar:
 * {@code thaumcraft/client/renderers/entity/RenderCultistPortal.java},
 * {@code thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java},
 * {@code thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java} and
 * the vanilla {@code net/minecraft/client/renderer/ActiveRenderInfo} of 1.7.10.
 */
class EldritchPortalParityTest {
    private static final float EPS = 1.0E-5F;
    /** The sine table of MathHelper has a resolution of one part in 65536. */
    private static final float TRIG = 1.0E-3F;

    // --------------------------------------------------------- the camera basis

    @Test void theLeftVectorOfTheOriginalIsTheLeftOfThePlayer() {
        // ActiveRenderInfo.updateRenderInfo: rotationX = cos(yaw), rotationZ = sin(yaw).
        // A player that looks to the south (yaw zero) has the east on its left.
        Vector3f left = CameraBasis.leftFor(0.0F, false);
        assertEquals(1.0F, left.x, TRIG, "the x of the left vector");
        assertEquals(0.0F, left.y, TRIG, "the y of the left vector");
        assertEquals(0.0F, left.z, TRIG, "the z of the left vector");
        Vector3f turned = CameraBasis.leftFor(90.0F, false);
        assertEquals(0.0F, turned.x, TRIG, "the x of the left vector at ninety degrees");
        assertEquals(1.0F, turned.z, TRIG, "the z of the left vector at ninety degrees");
    }

    @Test void theThirdPersonViewMirrorsTheBasisOfTheOriginal() {
        // ActiveRenderInfo.updateRenderInfo multiplies the basis with 1 - 2i, and
        // i is one in the third person view.
        Vector3f first = CameraBasis.leftFor(37.0F, false);
        Vector3f third = CameraBasis.leftFor(37.0F, true);
        assertEquals(-first.x, third.x, TRIG, "the x of the mirrored left vector");
        assertEquals(-first.z, third.z, TRIG, "the z of the mirrored left vector");
        Vector3f up = CameraBasis.upFor(37.0F, 21.0F, true);
        Vector3f upFirst = CameraBasis.upFor(37.0F, 21.0F, false);
        assertEquals(-upFirst.x, up.x, TRIG, "the x of the mirrored up vector");
        assertEquals(upFirst.y, up.y, TRIG, "the y of the up vector");
        assertEquals(-upFirst.z, up.z, TRIG, "the z of the mirrored up vector");
    }

    @Test void theUpVectorOfTheOriginalFollowsThePitch() {
        // ActiveRenderInfo.updateRenderInfo: rotationYZ = -rotationZ sin(pitch),
        // rotationXY = rotationX sin(pitch) and rotationXZ = cos(pitch).
        Vector3f level = CameraBasis.upFor(0.0F, 0.0F, false);
        assertEquals(0.0F, level.x, TRIG, "the x of the up vector");
        assertEquals(1.0F, level.y, TRIG, "the y of the up vector");
        assertEquals(0.0F, level.z, TRIG, "the z of the up vector");
        // A player that looks straight down sees the south as its up.
        Vector3f down = CameraBasis.upFor(0.0F, 90.0F, false);
        assertEquals(0.0F, down.x, TRIG, "the x of the up vector of a player that looks down");
        assertEquals(0.0F, down.y, TRIG, "the y of the up vector of a player that looks down");
        assertEquals(1.0F, down.z, TRIG, "the z of the up vector of a player that looks down");
    }

    // ------------------------------------------------------------ the eldritch gate

    @Test void theGateOfTheOriginalIsTwoBlocksWideWhenItIsOpen() {
        // TileEldritchPortalRenderer:41,44 - the scale is the opening counter
        // clamped to five and divided by five, and it is the half width of the quad.
        assertEquals(0.0F, TileEldritchPortalRenderer.halfWidth(0, 0.0F), EPS, "a closed gate");
        assertEquals(1.0F, TileEldritchPortalRenderer.halfWidth(5, 0.0F), EPS, "an open gate");
        assertEquals(1.0F, TileEldritchPortalRenderer.halfWidth(30, 0.0F), EPS, "a gate that counts above five");
        // TileEldritchPortalRenderer:40,45 - the scaley is the opening counter
        // clamped to thirty and divided by thirty, and it is the half height.
        assertEquals(0.0F, TileEldritchPortalRenderer.halfHeight(0, 0.0F), EPS, "a closed gate");
        assertEquals(1.0F, TileEldritchPortalRenderer.halfHeight(30, 0.0F), EPS, "an open gate");
        assertEquals(5.0F / 30.0F, TileEldritchPortalRenderer.halfHeight(5, 0.0F), EPS, "a half open gate");
    }

    @Test void theFrameOfTheGateIsTurnedByNinetyDegrees() {
        // TileEldritchPortalRenderer:74-81 - the u of the frame runs from the
        // bottom to the top of the gate and its v from the left to the right.
        float[] uv = TileEldritchPortalRenderer.frameUv(0);
        assertEquals(8, uv.length, "the four corners of the gate carry two coordinates");
        assertEquals(0.0F, uv[0], EPS, "the u of the bottom right corner");
        assertEquals(1.0F, uv[1], EPS, "the v of the bottom right corner");
        assertEquals(0.0625F, uv[2], EPS, "the u of the top right corner");
        assertEquals(1.0F, uv[3], EPS, "the v of the top right corner");
        assertEquals(0.0625F, uv[4], EPS, "the u of the top left corner");
        assertEquals(0.0F, uv[5], EPS, "the v of the top left corner");
        assertEquals(0.0F, uv[6], EPS, "the u of the bottom left corner");
        assertEquals(0.0F, uv[7], EPS, "the v of the bottom left corner");
    }

    // --------------------------------------------------------- the cultist portal

    @Test void theFrameOfTheCultistPortalIsUpright() {
        // RenderCultistPortal:95-108 - the frame is counted backwards and the u
        // of the frame runs from the left to the right and its v from the bottom
        // to the top.
        float[] uv = RenderCultistPortal.frameUv(0);
        assertEquals(8, uv.length, "the four corners of the portal carry two coordinates");
        assertEquals(0.0625F, uv[0], EPS, "the u of the bottom right corner");
        assertEquals(0.0F, uv[1], EPS, "the v of the bottom right corner");
        assertEquals(0.0625F, uv[2], EPS, "the u of the top right corner");
        assertEquals(1.0F, uv[3], EPS, "the v of the top right corner");
        assertEquals(0.0F, uv[4], EPS, "the u of the top left corner");
        assertEquals(1.0F, uv[5], EPS, "the v of the top left corner");
        assertEquals(0.0F, uv[6], EPS, "the u of the bottom left corner");
        assertEquals(0.0F, uv[7], EPS, "the v of the bottom left corner");
        // The original counts the frames backwards, the first frame of the
        // animation is the last one of the sheet.
        assertEquals(1.0F, RenderCultistPortal.frameUv(15)[0], EPS, "the u of the last frame");
    }

    // ------------------------------------------------------------- the crystal

    @Test void theCrystalKeepsTheLightOfTheOriginalAndNotTheLightOfItsBlock() {
        // TileEldritchCrystalRenderer:45-49 - the original overwrites the
        // coordinates of the light map with (5898, 178) up to (5898, 210) and the
        // light map clamps them, so the crystal is always drawn with the
        // brightest texel of the light map and the pulse of the original never
        // reaches the screen.
        assertEquals(LightTexture.FULL_BRIGHT, TileEldritchCrystalRenderer.crystalLight(),
                "the crystal is drawn with the full light");
    }
}
