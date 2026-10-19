package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.common.tiles.TileChestHungry;

/** Original TC4 renderer30–67: vanilla single-chest geometry, retained texture,
 * six metadata facings and cubic lid easing. Presentation never ticks the tile. */
public final class TileChestHungryRenderer implements BlockEntityRenderer<TileChestHungry> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/chesthungry.png");
    private static final RenderType SHELL = RenderType.entityCutout(TEXTURE);
    private final ModelPart bottom, lid, lock;

    public TileChestHungryRenderer(BlockEntityRendererProvider.Context context) {
        var chest = context.bakeLayer(ModelLayers.CHEST);
        // Native CHEST retains the 64x64 cube UVs, but uses Y-up/Z-forward
        // coordinates. Translate its cubes back to ModelChest's original local
        // coordinates before the original mirrored transform; do not flip UVs
        // or reinterpret this PNG as a modern vanilla chest texture.
        bottom = chest.getChild("bottom");
        bottom.setPos(0, 6, 0);
        lid = hinge(chest.getChild("lid"), 1, -5, -14);
        lock = hinge(chest.getChild("lock"), 8, 0, -29);
    }

    /** Keep the baked cubes/UVs, restoring the old ModelChest hinge at Y7/Z15. */
    private static ModelPart hinge(ModelPart part, float pivotX, float offsetY, float offsetZ) {
        part.setPos(-pivotX, offsetY, offsetZ);
        var hinge = new ModelPart(List.of(), Map.of("native", part));
        hinge.setPos(pivotX, 7, 15);
        return hinge;
    }

    @Override public void render(TileChestHungry tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        // Original detached/item tiles use metadata0, not the block's default
        // facing. UP/DOWN likewise deliberately retain rotation0.
        Direction facing = tile.hasLevel() ? tile.getBlockState().getValue(BlockStateProperties.FACING) : Direction.DOWN;
        renderStatic(facing, tile.getOpenNess(partialTick), pose, buffers, light, overlay);
    }

    /** Shared world/item draw. Openness is raw linear interpolation, not eased;
     * callers have already applied any native item-model display transform. */
    public void renderStatic(Direction facing, float openness, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float closed = 1.0F - openness;
        float eased = 1.0F - closed * closed * closed;
        lid.xRot = -(eased * (float) Math.PI / 2.0F);
        lock.xRot = lid.xRot;
        float yaw = switch (facing) {
            case NORTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            case SOUTH, DOWN, UP -> 0;
        };
        pose.pushPose();
        try {
            pose.translate(0, 1, 1);
            pose.scale(1, -1, -1);
            pose.translate(.5, .5, .5);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.translate(-.5, -.5, -.5);
            var vertices = buffers.getBuffer(SHELL);
            lid.render(pose, vertices, light, overlay);
            lock.render(pose, vertices, light, overlay);
            bottom.render(pose, vertices, light, overlay);
        } finally { pose.popPose(); }
    }
}
