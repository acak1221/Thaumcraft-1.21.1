package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
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
 * <p>Deviation: the original overwrites the global light texture with a value
 * of {@code 210 * shade} to make the crystal pulse. Minecraft 1.21.1 has no
 * global light texture override in the block entity pass, so the crystal keeps
 * the light of the block.
 */
public final class TileEldritchCrystalRenderer implements BlockEntityRenderer<TileEldritchCrystal> {
    /** The original texture of the base of the crystal. */
    public static final ResourceLocation CRUST = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/crust.png");
    /** The original texture of the crystal itself. */
    public static final ResourceLocation CRYSTAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/vcrystal.png");
    /** The original alpha of the crystal. */
    private static final int CRYSTAL_COLOR = 0xb2ffffff;

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
            model.renderCrystal(pose, buffers.getBuffer(RenderType.entityTranslucent(CRYSTAL)), light, overlay, CRYSTAL_COLOR);
        } finally {
            pose.popPose();
        }
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
