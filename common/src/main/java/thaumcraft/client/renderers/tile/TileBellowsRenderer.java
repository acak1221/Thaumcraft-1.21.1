package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelBellows;
import thaumcraft.common.tiles.TileBellows;

/** Original TileBellowsRenderer:30-84 six-way transforms and asymmetric inflation. */
public final class TileBellowsRenderer implements BlockEntityRenderer<TileBellows> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/bellows.png");
    private final ModelBellows model = new ModelBellows();
    public TileBellowsRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TileBellows tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float scale = tile.getInflation(), displacement = (.125F + scale * .875F) / 2 - .5F;
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(.5, -.5, .5);
        Direction direction = tile.getFacing();
        if (direction == Direction.DOWN) { pose.translate(0, 1, -1); pose.mulPose(Axis.XP.rotationDegrees(90)); }
        else if (direction == Direction.UP) { pose.translate(0, 1, 1); pose.mulPose(Axis.XP.rotationDegrees(270)); }
        else if (direction == Direction.NORTH) pose.mulPose(Axis.YP.rotationDegrees(180));
        else if (direction == Direction.WEST) pose.mulPose(Axis.YP.rotationDegrees(270));
        else if (direction == Direction.EAST) pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.scale(.5F, (scale + .1F) / 2, .5F);
        model.bag.render(pose, buffer, light, overlay);
        pose.popPose();
        pose.pushPose(); pose.translate(0, -displacement, 0); model.top.render(pose, buffer, light, overlay); pose.popPose();
        pose.pushPose(); pose.translate(0, displacement, 0); model.bottom.render(pose, buffer, light, overlay); pose.popPose();
        model.middle.render(pose, buffer, light, overlay); model.nozzle.render(pose, buffer, light, overlay);
        pose.popPose();
    }
}
