package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelTable;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.tiles.TileTable;

public final class TileTableRenderer implements BlockEntityRenderer<TileTable> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/table.png");
    private final ModelTable model = new ModelTable();

    public TileTableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileTable tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int variant = tile.getBlockState().getValue(BlockTable.VARIANT);
        if (variant >= 6) return;
        pose.pushPose();
        try {
            pose.translate(.5, 1, .5);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            if (variant == 1) pose.mulPose(Axis.YP.rotationDegrees(90));
            model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        } finally {
            pose.popPose();
        }
    }
}
