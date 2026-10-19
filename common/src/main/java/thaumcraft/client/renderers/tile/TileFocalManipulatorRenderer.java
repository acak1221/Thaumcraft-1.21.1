package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.renderers.models.ModelArcaneWorkbench;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Original shared six-box table and its actual loose focus. Native fixed-item
 * transforms replace old EntityItem/renderInFrame: bob/scale/thickness fidelity
 * is an explicit unverified adaptation, not a claim of legacy pixel parity. */
public final class TileFocalManipulatorRenderer implements BlockEntityRenderer<TileFocalManipulator> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wandtable.png");
    private final ModelArcaneWorkbench model = new ModelArcaneWorkbench();
    private final ItemRenderer items;

    public TileFocalManipulatorRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }

    @Override public void render(TileFocalManipulator tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5, 1, .5);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        } finally { pose.popPose(); }
        var focus = tile.getItem(0);
        if (tile.getLevel() == null || focus.isEmpty() || !(focus.getItem() instanceof ItemFocusBasic)) return;
        var camera = Minecraft.getInstance().getCameraEntity();
        float time = (camera == null ? tile.getTickCount() : camera.tickCount) + partialTick;
        // The original sine sets hoverStart (a PHASE), not a .4-block vertical
        // excursion. Keep the nested bob small; never spawn/tick an item entity.
        float hoverPhase = .2F + Mth.sin(time / 14F) * .2F;
        float bob = .1F + Mth.sin(hoverPhase) * .1F;
        pose.pushPose();
        try {
            pose.translate(.5, 1 + bob, .5);
            pose.mulPose(Axis.YP.rotationDegrees(time % 360));
            pose.scale(.5F, .5F, .5F);
            items.renderStatic(focus.copy(), ItemDisplayContext.FIXED, light, overlay, pose, buffers, tile.getLevel(), 0);
        } finally { pose.popPose(); }
    }
}
