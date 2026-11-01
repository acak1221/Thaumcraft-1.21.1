package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.client.renderers.models.ModelArcaneWorkbench;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDeconstruction;

/** Original BlockTableRenderer:48 uses the same table and persistent scanner as the world tile. */
public final class DeconstructionTableItemRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/decontable.png");
    private static final ModelArcaneWorkbench MODEL = new ModelArcaneWorkbench();

    private DeconstructionTableItemRenderer() {}

    /** The item's authored native display transform is already applied by the caller. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftDeconstruction.ITEM.get())) return;
        Minecraft client = Minecraft.getInstance();
        renderTableParts(client.getItemRenderer(), client.level, pose, buffers, light, overlay);
    }

    /** Static original parts shared by item and world rendering, without a fabricated tile or world. */
    public static void renderTableParts(ItemRenderer items, Level level, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            tablePose(pose);
            MODEL.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        } finally {
            pose.popPose();
        }
        pose.pushPose();
        try {
            meterPose(pose);
            // Restore the original stationary RenderItem + Forge ENTITY helper before the
            // native GROUND display. A bare renderStatic omits its frame scale and height.
            items.renderStatic(new ItemStack(ThaumcraftContent.THAUMOMETER.get()), ItemDisplayContext.GROUND,
                    light, overlay, pose, buffers, level, 0);
        } finally {
            pose.popPose();
        }
    }

    static void tablePose(PoseStack pose) {
        pose.translate(.5, 1, .5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
    }

    static void meterPose(PoseStack pose) {
        pose.translate(.5, .92, .5);
        pose.scale(.8F, .8F, .8F);
        // Original EntityItem age/hoverStart/partial=0: bob=.1, rotation=0.
        pose.translate(0, .1F, 0);
        // ForgeHooksClient.renderEntityItem: renderInFrame, BLOCK_3D helper, non-block item.
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.translate(0, .05F, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale(.5F, .5F, .5F);
        // Cancel the authored modern ground lift; the original helper above supplies it.
        // ItemRenderer still applies the display and builtin corner; the scanner centres itself.
        pose.translate(0, -3F / 16F, 0);
    }
}
