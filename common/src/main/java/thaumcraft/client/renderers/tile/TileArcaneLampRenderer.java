package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import thaumcraft.client.BoreClientEffects;
import thaumcraft.client.renderers.models.ModelArcaneBoreBase;
import thaumcraft.common.tiles.TileArcaneBoreBase;
import thaumcraft.common.tiles.TileArcaneLamp;

/** Original lamp renderer: its metal nozzle, plus the opposite nozzle on an attached Bore base. The body is the block model. */
public final class TileArcaneLampRenderer implements BlockEntityRenderer<TileArcaneLamp> {
    private final ModelArcaneBoreBase model = new ModelArcaneBoreBase();
    public TileArcaneLampRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TileArcaneLamp tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = tile.getFacing();
        if (tile.getLevel() != null && tile.getLevel().getBlockEntity(tile.getBlockPos().relative(facing)) instanceof TileArcaneBoreBase) {
            pose.pushPose();
            try {
                pose.translate(.5 + facing.getStepX(), facing.getStepY(), .5 + facing.getStepZ());
                turn(pose, facing.getOpposite());
                model.renderNozzle(pose, buffers.getBuffer(BoreClientEffects.SHELL), light, overlay);
            } finally { pose.popPose(); }
        }
        pose.pushPose();
        try {
            pose.translate(.5, 0, .5);
            turn(pose, facing);
            model.renderNozzle(pose, buffers.getBuffer(BoreClientEffects.SHELL), light, overlay);
        } finally { pose.popPose(); }
    }
    private static void turn(PoseStack pose, Direction facing) {
        switch (facing) {
            case DOWN -> { pose.translate(-.5, .5, 0); pose.mulPose(Axis.ZN.rotationDegrees(90)); }
            case UP -> { pose.translate(.5, .5, 0); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> {}
        }
    }
}
