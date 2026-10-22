package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.renderers.models.ModelBrain;
import thaumcraft.client.renderers.models.ModelJar;
import thaumcraft.common.tiles.TileJarBrain;

/**
 * The original brain jar: the brain floating in the brine of the jar, turning to look at the
 * closest experience orb or at the closest player.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:155-175} ({@code renderBrain}):
 * the brain bobs with {@code sin(ticksSinceLevel / 14) * 0.03 + 0.03}, it is turned by the
 * interpolated {@code rota}/{@code rotb} of the jar around the Z axis, then by minus ninety degrees
 * around the Y axis, and it is drawn with the scale {@code 0.4} on the {@code brain2.png} sheet. The
 * brine of the jar is the {@code jarbrine.png} volume of the original {@code ModelJar}.</p>
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:60-64}: the whole jar
 * content is drawn at the block centre, one hundredth of a block above the block floor, turned by
 * one hundred and eighty degrees around the X axis. The native block entity renderer pose is already
 * at the block centre, so the original offset is the minus forty nine hundredths of a block in Y.</p>
 */
public final class TileJarBrainRenderer implements BlockEntityRenderer<TileJarBrain> {
    /** The original brain sheet. */
    private static final ResourceLocation BRAIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/brain2.png");
    /** The original brine sheet. */
    private static final ResourceLocation BRINE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/jarbrine.png");
    /** The original bob: the sine of the player tick divided by fourteen, times 0.03, plus 0.03. */
    public static final float BOB_AMPLITUDE = .03F;
    public static final float BOB_PERIOD = 14.0F;
    /** The original scale of the brain model. */
    public static final float BRAIN_SCALE = .4F;
    /** The original height of the brain above the block floor. */
    public static final float BRAIN_HEIGHT = -.8F;
    /** The original offset of the whole jar content above the block floor. */
    public static final float CONTENT_OFFSET = .01F;
    private final ModelBrain brain = new ModelBrain();
    private final ModelJar jar = new ModelJar();

    public TileJarBrainRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileJarBrain tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // Original TileJarRenderer:62-63 - the content frame of the jar: the block centre and
        // one hundredth of a block above the block floor.
        pose.translate(.5F, CONTENT_OFFSET, .5F);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        renderBrain(tile, partialTick, pose, buffers, overlay);
        // Original TileJarRenderer:173-174 - the brine of the jar, in the content frame.
        jar.renderBrine(pose, buffers.getBuffer(RenderType.entityTranslucent(BRINE)), LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
    }

    /** The original {@code renderBrain}: the bob, the turn and the scale of the brain. */
    private void renderBrain(TileJarBrain tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int overlay) {
        float bob = bob(Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount);
        pose.pushPose();
        pose.translate(0, BRAIN_HEIGHT + bob, 0);
        float turn = turn(tile.rota, tile.rotb, partialTick);
        pose.mulPose(Axis.ZP.rotationDegrees(turn * 180.0F / 3.141593F));
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale(BRAIN_SCALE, BRAIN_SCALE, BRAIN_SCALE);
        brain.render(pose, buffers.getBuffer(RenderType.entityTranslucent(BRAIN)), LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
    }

    /** The original bob of the brain: {@code sin(ticksSinceLevel / 14) * 0.03 + 0.03}. */
    public static float bob(int ticksSinceLevel) {
        return Mth.sin(ticksSinceLevel / BOB_PERIOD) * BOB_AMPLITUDE + BOB_AMPLITUDE;
    }

    /**
     * The original turn of the brain: the difference of the two angles, wrapped into
     * {@code [-pi, pi]}, interpolated with the frame.
     */
    public static float turn(float rota, float rotb, float partialTick) {
        float difference = rota - rotb;
        while (difference >= 3.141593F) difference -= 6.283185F;
        while (difference < -3.141593F) difference += 6.283185F;
        return rotb + difference * partialTick;
    }
}
