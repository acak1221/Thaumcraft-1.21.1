package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.BoreClientEffects;
import thaumcraft.client.renderers.models.ModelArcaneBoreBase;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.tiles.TileArcaneBoreBase;

/** Original base renderer26–54. Nozzle state is independent of top mining or
 * attachment. Original UP/DOWN display the same +X nozzle, not invented tilts. */
public final class TileArcaneBoreBaseRenderer implements BlockEntityRenderer<TileArcaneBoreBase> {
    private final ModelArcaneBoreBase model = new ModelArcaneBoreBase();
    private static final class ItemModel { private static final ModelArcaneBoreBase MODEL = new ModelArcaneBoreBase(); }
    public TileArcaneBoreBaseRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileArcaneBoreBase tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderModel(model, tile.nozzle(), pose, buffers, light, overlay);
    }
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (stack.is(ThaumcraftArcaneBore.ARCANE_BORE_BASE.get().asItem())) renderModel(ItemModel.MODEL, Direction.NORTH, pose, buffers, light, overlay);
    }
    private static void renderModel(ModelArcaneBoreBase model, Direction nozzle, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5, 0, .5);
            var vertices = buffers.getBuffer(BoreClientEffects.SHELL);
            model.renderBase(pose, vertices, light, overlay);
            float yaw = switch (nozzle) { case NORTH -> 90; case SOUTH -> 270; case WEST -> 180; case EAST, UP, DOWN -> 0; };
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            model.renderNozzle(pose, vertices, light, overlay);
        } finally { pose.popPose(); }
    }
}
