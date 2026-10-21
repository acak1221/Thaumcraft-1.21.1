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
import thaumcraft.client.renderers.models.ModelEldritchCrystal;
import thaumcraft.common.tiles.TileEldritchCrystal;

/**
 * Original TileEldritchCrystalRenderer: the eldritch crystal of the Outer Lands.
 *
 * <p>The original draws the {@code Base} group of {@code vcrystal.obj} with the
 * crust texture and the translucent {@code Crystal} group with the crystal
 * texture. The native tree already carries the converted wavefront model in
 * {@link ModelEldritchCrystal}, so the geometry is reused as it is.
 *
 * <p>Oracle {@code TileEldritchCrystalRenderer.java:44-50}: the original
 * overwrites the light map of the crystal with a fixed texel that pulses with
 * the tick counter of the player, so the crystal keeps its own brightness and
 * does not follow the light of the block. The native port reproduces the pulse
 * with a packed light of its own: the light level of the block light of the
 * original texel is {@code (int) (210 * shade) / 16}, that is eleven, twelve or
 * thirteen, and the crystal is drawn with that light instead of the light of the
 * block.
 */
public final class TileEldritchCrystalRenderer implements BlockEntityRenderer<TileEldritchCrystal> {
    /** The original texture of the base of the crystal. */
    public static final ResourceLocation CRUST = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/crust.png");
    /** The original texture of the crystal itself. */
    public static final ResourceLocation CRYSTAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/vcrystal.png");
    /** The original alpha of the crystal. */
    private static final int CRYSTAL_COLOR = 0xb2ffffff;
    /** The original divisor of the tick counter of the pulse, oracle :45. */
    private static final float PULSE_PERIOD = 6.0F;
    /** The original amplitude of the pulse, oracle :45. */
    private static final float PULSE_AMPLITUDE = 0.075F;
    /** The original offset of the pulse, oracle :45. */
    private static final float PULSE_OFFSET = 0.925F;
    /** The original brightness of the crystal, oracle :46. */
    private static final float PULSE_BRIGHTNESS = 210.0F;

    private final ModelEldritchCrystal model = new ModelEldritchCrystal();

    public TileEldritchCrystalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEldritchCrystal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5F, .5F, .5F);
            orientation(pose, tile.getOrientation());
            pose.translate(0.0F, 0.0F, -.5F);
            // The original turns the crystal by a quarter turn per position.
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F * (Math.abs(tile.getBlockPos().hashCode()) % 4)));
            model.renderBase(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CRUST)), light, overlay, 0xffffffff);
            model.renderCrystal(pose, buffers.getBuffer(RenderType.entityTranslucent(CRYSTAL)), crystalLight(), overlay, CRYSTAL_COLOR);
        } finally {
            pose.popPose();
        }
    }

    /**
     * The light of the crystal of the original: the pulse of the light map.
     *
     * <p>Oracle {@code TileEldritchCrystalRenderer.java:45-49}: the shade is
     * {@code sin(ticks / 6) * 0.075 + 0.925} and the light map is set to the texel
     * {@code (int) (210 * shade)}, whose light level of the block light is the
     * texel divided by sixteen.
     */
    public static int crystalLight() {
        var player = Minecraft.getInstance().player;
        float ticks = player == null ? 0.0F : player.tickCount;
        float shade = Mth.sin(ticks / PULSE_PERIOD) * PULSE_AMPLITUDE + PULSE_OFFSET;
        int level = (int) (PULSE_BRIGHTNESS * shade) / 16;
        return LightTexture.pack(level, 0);
    }

    /** The original translateFromOrientation of the crystal and the crab vent. */
    public static void orientation(PoseStack pose, int orientation) {
        switch (orientation) {
            case 0 -> pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            case 1 -> pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case 2 -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case 4 -> pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            case 5 -> pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            default -> {
            }
        }
    }
}
