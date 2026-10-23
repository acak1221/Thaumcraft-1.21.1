package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.client.renderers.models.ModelArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.tiles.TileArcaneWorkbench;

public final class TileArcaneWorkbenchRenderer implements BlockEntityRenderer<TileArcaneWorkbench> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/worktable.png");
    private final ModelArcaneWorkbench model = new ModelArcaneWorkbench();
    private final ItemRenderer items;

    public TileArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }

    @Override public void render(TileArcaneWorkbench tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5, 1, .5);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        } finally {
            pose.popPose();
        }
        var wand = tile.getItem(TileArcaneWorkbench.WAND_SLOT);
        if (!ArcaneVis.isCraftingWand(wand) || tile.getLevel() == null) return;
        // Original TileArcaneWorkbenchRenderer:46-58 lays the real stored wand
        // on the top, without spawning a world entity or changing its components.
        pose.pushPose();
        try {
            pose.translate(.65, 1.0625, .25);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(20));
            items.renderStatic(wand, ItemDisplayContext.GROUND, light, overlay, pose, buffers, tile.getLevel(), 0);
        } finally {
            pose.popPose();
        }
    }
}
