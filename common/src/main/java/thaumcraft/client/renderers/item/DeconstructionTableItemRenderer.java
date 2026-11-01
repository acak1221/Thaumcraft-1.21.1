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
            // GROUND is the existing world tile's stationary entity-item view. It never draws
            // first-person arms/readout, and preserves the native item display's lifting offset.
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
    }
}
