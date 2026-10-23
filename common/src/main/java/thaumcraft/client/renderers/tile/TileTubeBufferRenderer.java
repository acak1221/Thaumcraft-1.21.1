package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.tiles.TileBellows;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Original BlockTubeRenderer:79-85,131-222 and TileTubeBufferRenderer:25-49.
 * No liquid gauge, invented pressure colours or fake item/world tile. */
public final class TileTubeBufferRenderer implements BlockEntityRenderer<TileTubeBuffer> {
    private static final ResourceLocation PIPE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/pipe_buffer");
    private static final ResourceLocation STEM = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/valve.png");
    private static final float W4 = 4 / 16F, W7 = 7 / 16F, W9 = 9 / 16F, W12 = 12 / 16F;
    private static final class Model {
        private static final ModelPart STEM = bakeStem();
    }
    public TileTubeBufferRenderer(BlockEntityRendererProvider.Context context) {}

    private static ModelPart bakeStem() {
        var mesh = new MeshDefinition();
        // Original mirror=true was set after addBox, not on the baked cube.
        mesh.getRoot().addOrReplaceChild("stem", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-1, 2, -1, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("stem");
    }

    @Override public void render(TileTubeBuffer buffer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (buffer.getLevel() == null) return;
        float[] lo = {W7, W7, W7}, hi = {W9, W9, W9}; boolean[] axes = new boolean[3];
        for (Direction side : Direction.values()) {
            if (!buffer.isSideOpen(side)) continue;
            var level = buffer.getLevel(); var at = buffer.getBlockPos().relative(side);
            if (!level.hasChunkAt(at)) continue;
            var tile = level.getBlockEntity(at);
            IEssentiaTransport transport = tile instanceof IEssentiaTransport value && value.isConnectable(side.getOpposite()) ? value : null;
            // The original BLOCK renderer alone also accepts inward bellows
            // for buffer arms; the choke renderer below accepts transports only.
            boolean bellows = tile instanceof TileBellows value && value.getFacing() == side.getOpposite();
            if (transport == null && !bellows) continue;
            int axis = axis(side); axes[axis] = true;
            float extension = transport != null && transport.renderExtendedTube() ? 6 / 16F : 0;
            if (side.getAxisDirection() == Direction.AxisDirection.NEGATIVE) lo[axis] = -extension;
            else hi[axis] = 1 + extension;
        }
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (int axis = 0; axis < 3; axis++) if (axes[axis]) {
            float[] min = {W7, W7, W7}, max = {W9, W9, W9}; min[axis] = lo[axis]; max[axis] = hi[axis];
            box(buffer, pose, vertices, min, max, light, overlay);
        }
        box(buffer, pose, vertices, new float[]{W4, W4, W4}, new float[]{W12, W12, W12}, light, overlay);
        for (Direction side : Direction.values()) {
            int choke = buffer.getChoke(side);
            if (choke == 0 || !buffer.isSideOpen(side) || !transportNeighbor(buffer, side)) continue;
            pose.pushPose();
            try {
                pose.translate(.5, .5, .5); orient(side.getOpposite(), pose);
                pose.scale(1.2F, 1, 1.2F); pose.translate(0, -.5, 0);
                // Nearest native 8-bit vertex colour to legacy (1,.3,.3) or
                // (.3,.3,1). The mesh and retained valve texture are unchanged.
                Model.STEM.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(STEM)), light, overlay,
                        choke == 2 ? 0xFFFF4D4D : 0xFF4D4DFF);
            } finally {
                pose.popPose();
            }
        }
    }

    /** Six-argument Fabric builtin-item-compatible hook; plain original buffer
     * item is its large core plus vertical shaft, with NO saved choke overlay. */
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get())) return;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        box(null, pose, vertices, new float[]{W4, W4, W4}, new float[]{W12, W12, W12}, light, overlay);
        box(null, pose, vertices, new float[]{W7, 0, W7}, new float[]{W9, 1, W9}, light, overlay);
    }

    private static boolean transportNeighbor(TileTubeBuffer buffer, Direction side) {
        var level = buffer.getLevel(); var at = buffer.getBlockPos().relative(side);
        return level != null && level.hasChunkAt(at) && level.getBlockEntity(at) instanceof IEssentiaTransport transport
                && transport.isConnectable(side.getOpposite());
    }
    private static int axis(Direction side) { return switch (side.getAxis()) { case X -> 0; case Y -> 1; case Z -> 2; }; }
    private static void orient(Direction facing, PoseStack pose) {
        if (facing.getStepY() == 0) pose.mulPose(Axis.YP.rotationDegrees(90));
        else { pose.mulPose(Axis.XN.rotationDegrees(90)); pose.mulPose(Axis.XP.rotationDegrees(90 * facing.getStepY())); }
        pose.mulPose(switch (facing) { case DOWN -> Axis.YN.rotationDegrees(90); case UP -> Axis.YP.rotationDegrees(90); case NORTH -> Axis.ZN.rotationDegrees(90); case SOUTH -> Axis.ZP.rotationDegrees(90); case WEST -> Axis.XN.rotationDegrees(90); case EAST -> Axis.XP.rotationDegrees(90); });
    }
    private static void box(TileTubeBuffer buffer, PoseStack pose, VertexConsumer vertices, float[] min, float[] max, int light, int overlay) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(PIPE);
        float x0 = min[0], y0 = min[1], z0 = min[2], x1 = max[0], y1 = max[1], z1 = max[2];
        for (Direction face : Direction.values()) {
            float shade = buffer == null ? 1 : buffer.getLevel().getShade(face, true);
            switch (face) {
                case DOWN -> quad(pose, vertices, sprite, face, shade, light, overlay, new float[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}}, 0, 2, false, false, min, max);
                case UP -> quad(pose, vertices, sprite, face, shade, light, overlay, new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}}, 0, 2, false, false, min, max);
                case NORTH -> quad(pose, vertices, sprite, face, shade, light, overlay, new float[][]{{x1,y1,z0},{x1,y0,z0},{x0,y0,z0},{x0,y1,z0}}, 0, 1, true, true, min, max);
                case SOUTH -> quad(pose, vertices, sprite, face, shade, light, overlay, new float[][]{{x0,y1,z1},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1}}, 0, 1, false, true, min, max);
                case WEST -> quad(pose, vertices, sprite, face, shade, light, overlay, new float[][]{{x0,y1,z0},{x0,y0,z0},{x0,y0,z1},{x0,y1,z1}}, 2, 1, false, true, min, max);
                case EAST -> quad(pose, vertices, sprite, face, shade, light, overlay, new float[][]{{x1,y1,z1},{x1,y0,z1},{x1,y0,z0},{x1,y1,z0}}, 2, 1, true, true, min, max);
            }
        }
    }
    private static void quad(PoseStack pose, VertexConsumer vertices, TextureAtlasSprite sprite, Direction face, float shade,
                             int light, int overlay, float[][] points, int uAxis, int vAxis, boolean flipU, boolean flipV, float[] min, float[] max) {
        int colour = Math.round(255 * shade);
        for (float[] point : points) {
            float u = coordinate(point[uAxis], min[uAxis], max[uAxis]), v = coordinate(point[vAxis], min[vAxis], max[vAxis]);
            vertices.addVertex(pose.last(), point[0], point[1], point[2]).setColor(colour, colour, colour, 255)
                    .setUv(sprite.getU(flipU ? 1-u : u), sprite.getV(flipV ? 1-v : v)).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }
    private static float coordinate(float value, float min, float max) { return min < 0 || max > 1 ? (value - min) / (max - min) : value; }
}
