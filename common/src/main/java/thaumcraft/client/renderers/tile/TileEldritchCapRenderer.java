package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.client.renderers.models.ModelEldritchCap;
import thaumcraft.common.config.ThaumcraftWarp;

/**
 * Original TileEldritchCapRenderer: the capstone of the eldritch obelisk and
 * the cap of the eldritch altar.
 *
 * <p>The original loads {@code textures/models/obelisk_cap.obj} and draws its
 * {@code Cap} group, rotated by ninety degrees, in the middle of the block.
 * The native tree already carries the converted wavefront model in
 * {@link ModelEldritchCap}, so the geometry is reused as it is.
 *
 * <p>The original altar variant also draws the eldritch eyes that were placed
 * on the altar as little items around its rim.
 */
public final class TileEldritchCapRenderer {
    /** The original texture of the obelisk capstone. */
    public static final ResourceLocation CAP = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/obelisk_cap.png");
    /** The original texture of the altar cap. */
    public static final ResourceLocation CAP_ALTAR = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/obelisk_cap_altar.png");
    /** The original texture of the Outer Lands variant. */
    public static final ResourceLocation CAP_OUTER = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/obelisk_cap_2.png");
    /** The original radius of the eye ring of the altar. */
    private static final float EYE_RADIUS = .46F;
    /** The original height of the eye ring of the altar. */
    private static final float EYE_HEIGHT = .2F;

    private final ModelEldritchCap model = new ModelEldritchCap();

    /**
     * Draws the cap of a block.
     *
     * @param texture the original texture of the cap
     * @param outer   true in the Outer Lands, where the original uses the second texture
     */
    public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                       ResourceLocation texture, boolean outer) {
        ResourceLocation used = outer ? CAP_OUTER : texture;
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(used));
        pose.pushPose();
        try {
            // The original translates to the middle of the block and turns the cap on its side.
            pose.translate(.5F, 0.0F, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            model.renderCapGroup(pose, out, light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
    }

    /**
     * The original eye ring of the eldritch altar: one eldritch eye item per
     * eye, standing on a circle around the middle of the altar.
     */
    public void renderEyes(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int eyes, Level tileLevel) {
        if (eyes <= 0) return;
        ItemStack eye = new ItemStack(ThaumcraftWarp.ELDRITCH_EYE.get());
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        try {
            pose.translate(.5F, 0.0F, .5F);
            for (int index = 0; index < eyes; index++) {
                pose.pushPose();
                try {
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    pose.translate(EYE_RADIUS, EYE_HEIGHT, 0.0F);
                    pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                    pose.mulPose(Axis.XP.rotationDegrees(-18.0F));
                    itemRenderer.renderStatic(eye, ItemDisplayContext.FIXED, light, overlay, pose, buffers, tileLevel, 0);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
    }
}
