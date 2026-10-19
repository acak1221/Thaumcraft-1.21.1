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
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileTube;

/**
 * TC4 BlockTubeRenderer:39-77,103-235 and the valve/oneway tile renderers,
 * expressed as native BER/builtin-item geometry using the retained assets.
 * No liquid overlay, invented pressure colours or connections through closed
 * ports. Targeting/collision deliberately remain separate from this geometry.
 */
public final class TileTubeRenderer implements BlockEntityRenderer<TileTube> {
    private static final ResourceLocation PIPE = sprite("pipe_1");
    private static final ResourceLocation CORE = sprite("pipe_2");
    private static final ResourceLocation JOINT = sprite("pipe_3");
    private static final ResourceLocation FILTER = sprite("pipe_filter");
    private static final ResourceLocation FILTER_CORE = sprite("pipe_filter_core");
    private static final ResourceLocation RESTRICT = sprite("pipe_restrict");
    private static final ResourceLocation ONEWAY = sprite("pipe_oneway");
    // Original BlockTube:105 registers iconValve as pipe_valve, not a new alias.
    private static final ResourceLocation WHEEL = sprite("pipe_valve");
    private static final ResourceLocation STEM = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/valve.png");
    private static final float W6 = 6 / 16F, W7 = 7 / 16F, W9 = 9 / 16F, W10 = 10 / 16F;

    public TileTubeRenderer(BlockEntityRendererProvider.Context context) {}

    private static final class ValveModel {
        private static final ModelPart STEM = bakeStem();
    }

    private static ModelPart bakeStem() {
        var mesh = new MeshDefinition();
        // ModelTubeValve:19-26. Legacy mirror=true was assigned AFTER addBox,
        // and therefore did not alter the already-constructed cube/UVs.
        mesh.getRoot().addOrReplaceChild("stem", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-1, 2, -1, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("stem");
    }

    @Override public void render(TileTube tube, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tube.getLevel();
        if (level == null) return;
        float[] lo = {W7, W7, W7}, hi = {W9, W9, W9};
        boolean[] axes = new boolean[3];
        boolean nonTube = false;
        for (Direction side : Direction.values()) {
            if (!tube.isConnectable(side)) continue;
            var neighbour = EssentiaTransfers.connectableNeighbor(tube, side);
            if (!(neighbour instanceof IEssentiaTransport transport)) continue;
            int axis = axis(side);
            axes[axis] = true;
            nonTube |= !(neighbour instanceof TileTube);
            float extension = transport.renderExtendedTube() ? W6 : 0;
            if (side.getAxisDirection() == Direction.AxisDirection.NEGATIVE) lo[axis] = -extension;
            else hi[axis] = 1 + extension;
        }
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        int drawn = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (!axes[axis]) continue;
            drawn++;
            float[] min = {W7, W7, W7}, max = {W9, W9, W9};
            min[axis] = lo[axis];
            max[axis] = hi[axis];
            box(tube, pose, vertices, min, max, null, light, overlay);
        }
        if (tube.isFilter()) {
            float min = W6 - .03125F, max = W10 + .03125F;
            box(tube, pose, vertices, new float[]{min, min, min}, new float[]{max, max, max}, FILTER, light, overlay);
            int tint = tube.getFilter() == null ? 0xFFFFFF : tube.getFilter().getColor();
            box(tube, pose, vertices, new float[]{min, min, min}, new float[]{max, max, max}, FILTER_CORE, light, overlay, tint);
        } else {
            boolean largeCore = drawn == 0 || nonTube || tube.isValve();
            float min = largeCore ? W6 : W7 - .03125F;
            float max = largeCore ? W10 : W9 + .03125F;
            box(tube, pose, vertices, new float[]{min, min, min}, new float[]{max, max, max},
                    largeCore ? CORE : tube.isRestrict() ? RESTRICT : JOINT, light, overlay);
        }
        if (tube.isValve()) renderValve(tube.getFacing(), tube.getRotation(), pose, buffers, light, overlay);
        if (tube.isOneWay() && EssentiaTransfers.connectableNeighbor(tube, tube.getFacing().getOpposite()) != null)
            renderOneWay(tube.getFacing(), pose, buffers, light, overlay);
    }

    /** Loader-neutral builtin item hook; caller applies the native item model's
     * display transform. Original BlockTubeRenderer:41-64 uses a vertical shaft,
     * large core and EAST-facing open wheel. No fake BE or world access. */
    public static void renderValveItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get())) return;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        box(null, pose, vertices, new float[]{W7, 0, W7}, new float[]{W9, 1, W9}, PIPE, light, overlay);
        box(null, pose, vertices, new float[]{W6, W6, W6}, new float[]{W10, W10, W10}, CORE, light, overlay);
        renderValve(Direction.EAST, 0, pose, buffers, light, overlay);
    }

    /** Original inventory art, without fake tiles/worlds or reading item flags.
     * Oneway's inventory overlay is deliberately NOT its world indicator. */
    public static void renderAdvancedItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item) || !(item.getBlock() instanceof BlockTube block)) return;
        BlockTube.Kind kind = block.kind();
        if (kind != BlockTube.Kind.FILTER && kind != BlockTube.Kind.RESTRICT && kind != BlockTube.Kind.ONEWAY) return;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        box(null, pose, vertices, new float[]{W7, 0, W7}, new float[]{W9, 1, W9}, kind == BlockTube.Kind.RESTRICT ? RESTRICT : PIPE, light, overlay);
        if (kind == BlockTube.Kind.ONEWAY)
            box(null, pose, vertices, new float[]{W7 - .001F, .1F, W7 - .001F}, new float[]{W9 + .001F, .9F, W9 + .001F}, ONEWAY, light, overlay);
        if (kind == BlockTube.Kind.FILTER) {
            float min = W6 - .03125F, max = W10 + .03125F;
            box(null, pose, vertices, new float[]{min, min, min}, new float[]{max, max, max}, FILTER, light, overlay);
            box(null, pose, vertices, new float[]{min, min, min}, new float[]{max, max, max}, FILTER_CORE, light, overlay);
        } else {
            float min = W7 - .03125F, max = W9 + .03125F;
            box(null, pose, vertices, new float[]{min, min, min}, new float[]{max, max, max}, kind == BlockTube.Kind.RESTRICT ? RESTRICT : JOINT, light, overlay);
        }
    }

    private static void renderOneWay(Direction facing, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, .5, .5);
        orientStem(facing, pose);
        // TileTubeOnewayRenderer:42-49: three ORIGINAL valve-model rods,
        // blue(.45,.5,1), scale(1.1,.5,1.1), successive localY -.25 travel.
        pose.scale(1.1F, .5F, 1.1F);
        pose.translate(0, -.5, 0);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(STEM));
        for (int piece = 0; piece < 3; piece++) {
            // Native model vertices store 8-bit colour: nearest ARGB to the
            // original floating (.45,.5,1), not a replacement piece of artwork.
            ValveModel.STEM.render(pose, vertices, light, overlay, 0xFF7380FF);
            pose.translate(0, -.25, 0);
        }
        pose.popPose();
    }

    private static void renderValve(Direction facing, float rotation, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, .5, .5);
        // Preserve the original matrix order, including the vertical special
        // case. Partial ticks are deliberately NOT interpolated: TC4 animates
        // the client field in native +/-20-degree tick steps, even while venting.
        orientStem(facing, pose);
        pose.mulPose(Axis.YP.rotationDegrees(-1.5F * rotation));
        pose.translate(0, -(rotation / 360F) * .12F, 0);
        ValveModel.STEM.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(STEM)), light, overlay);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.translate(-.25, -.25, -.25);
        pose.scale(.5F, .5F, .5F);
        extrudedWheel(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)), light, overlay);
        pose.popPose();
    }

    private static void orientStem(Direction facing, PoseStack pose) {
        if (facing.getStepY() == 0) pose.mulPose(Axis.YP.rotationDegrees(90));
        else {
            pose.mulPose(Axis.XN.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees(90 * facing.getStepY()));
        }
        pose.mulPose(switch (facing) {
            case DOWN -> Axis.YN.rotationDegrees(90);
            case UP -> Axis.YP.rotationDegrees(90);
            case NORTH -> Axis.ZN.rotationDegrees(90);
            case SOUTH -> Axis.ZP.rotationDegrees(90);
            case WEST -> Axis.XN.rotationDegrees(90);
            case EAST -> Axis.XP.rotationDegrees(90);
        });
    }

    /** Native equivalent of old ItemRenderer.renderItemIn2D: two sprite faces
     * and a half-texel-sampled strip per pixel on all four edges. Transparency
     * comes from the retained PNG, not invented solid spokes or donor artwork.
     * The original .1 depth is applied BEFORE the outer .5 model scale. */
    private static void extrudedWheel(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        TextureAtlasSprite texture = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHEEL);
        float depth = .1F;
        wheelQuad(pose, vertices, texture, Direction.SOUTH, light, overlay,
                new float[][]{{0,0,0,1,1},{1,0,0,0,1},{1,1,0,0,0},{0,1,0,1,0}});
        wheelQuad(pose, vertices, texture, Direction.NORTH, light, overlay,
                new float[][]{{0,1,-depth,1,0},{1,1,-depth,0,0},{1,0,-depth,0,1},{0,0,-depth,1,1}});
        int width = texture.contents().width(), height = texture.contents().height();
        for (int column = 0; column < width; column++) {
            float x0 = (float) column / width, x1 = (float) (column + 1) / width;
            float u = 1 - (column + .5F) / width;
            wheelQuad(pose, vertices, texture, Direction.WEST, light, overlay,
                    new float[][]{{x0,0,-depth,u,1},{x0,0,0,u,1},{x0,1,0,u,0},{x0,1,-depth,u,0}});
            wheelQuad(pose, vertices, texture, Direction.EAST, light, overlay,
                    new float[][]{{x1,1,-depth,u,0},{x1,1,0,u,0},{x1,0,0,u,1},{x1,0,-depth,u,1}});
        }
        for (int row = 0; row < height; row++) {
            float y0 = (float) row / height, y1 = (float) (row + 1) / height;
            float v = 1 - (row + .5F) / height;
            wheelQuad(pose, vertices, texture, Direction.UP, light, overlay,
                    new float[][]{{0,y1,0,1,v},{1,y1,0,0,v},{1,y1,-depth,0,v},{0,y1,-depth,1,v}});
            wheelQuad(pose, vertices, texture, Direction.DOWN, light, overlay,
                    new float[][]{{1,y0,0,0,v},{0,y0,0,1,v},{0,y0,-depth,1,v},{1,y0,-depth,0,v}});
        }
    }

    private static void wheelQuad(PoseStack pose, VertexConsumer vertices, TextureAtlasSprite texture, Direction normal,
                                  int light, int overlay, float[][] points) {
        for (float[] point : points) vertices.addVertex(pose.last(), point[0], point[1], point[2]).setColor(255, 255, 255, 255)
                .setUv(texture.getU(point[3]), texture.getV(point[4])).setOverlay(overlay).setLight(light)
                .setNormal(pose.last(), normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    private static int axis(Direction side) {
        return switch (side.getAxis()) { case X -> 0; case Y -> 1; case Z -> 2; };
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/" + name);
    }

    private static void box(TileTube tube, PoseStack pose, VertexConsumer vertices, float[] min, float[] max,
                             ResourceLocation override, int light, int overlay) {
        box(tube, pose, vertices, min, max, override, light, overlay, 0xFFFFFF);
    }

    private static void box(TileTube tube, PoseStack pose, VertexConsumer vertices, float[] min, float[] max,
                            ResourceLocation override, int light, int overlay, int tint) {
        for (Direction face : Direction.values()) {
            // Preserve the accepted basic tube's facing mark (a documented
            // discrepancy), but do NOT extend it to the original valve shaft.
            // Central pieces have the original explicit texture override.
            var texture = override != null ? override : tube.isRestrict() ? RESTRICT : tube.kind() == BlockTube.Kind.BASIC && face == tube.getFacing() ? JOINT : PIPE;
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
            float x0 = min[0], y0 = min[1], z0 = min[2], x1 = max[0], y1 = max[1], z1 = max[2];
            float shade = tube == null ? 1 : tube.getLevel().getShade(face, true);
            switch (face) {
                case DOWN -> quad(pose, vertices, sprite, face, shade, tint, light, overlay,
                        new float[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}}, 0, 2, false, false, min, max);
                case UP -> quad(pose, vertices, sprite, face, shade, tint, light, overlay,
                        new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}}, 0, 2, false, false, min, max);
                case NORTH -> quad(pose, vertices, sprite, face, shade, tint, light, overlay,
                        new float[][]{{x1,y1,z0},{x1,y0,z0},{x0,y0,z0},{x0,y1,z0}}, 0, 1, true, true, min, max);
                case SOUTH -> quad(pose, vertices, sprite, face, shade, tint, light, overlay,
                        new float[][]{{x0,y1,z1},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1}}, 0, 1, false, true, min, max);
                case WEST -> quad(pose, vertices, sprite, face, shade, tint, light, overlay,
                        new float[][]{{x0,y1,z0},{x0,y0,z0},{x0,y0,z1},{x0,y1,z1}}, 2, 1, false, true, min, max);
                case EAST -> quad(pose, vertices, sprite, face, shade, tint, light, overlay,
                        new float[][]{{x1,y1,z1},{x1,y0,z1},{x1,y0,z0},{x1,y1,z0}}, 2, 1, true, true, min, max);
            }
        }
    }

    private static void quad(PoseStack pose, VertexConsumer vertices, TextureAtlasSprite sprite, Direction face,
                             float shade, int tint, int light, int overlay, float[][] points, int uAxis, int vAxis,
                             boolean flipU, boolean flipV, float[] min, float[] max) {
        int red = Math.round(((tint >> 16) & 255) * shade), green = Math.round(((tint >> 8) & 255) * shade), blue = Math.round((tint & 255) * shade);
        for (float[] point : points) {
            // Legacy RenderBlocks maps block-relative bounds onto the sprite;
            // out-of-block extended arms use the full UV span on that axis.
            float u = coordinate(point[uAxis], min[uAxis], max[uAxis]);
            float v = coordinate(point[vAxis], min[vAxis], max[vAxis]);
            vertices.addVertex(pose.last(), point[0], point[1], point[2]).setColor(red, green, blue, 255)
                    .setUv(sprite.getU(flipU ? 1-u : u), sprite.getV(flipV ? 1-v : v))
                    .setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    private static float coordinate(float value, float min, float max) {
        return min < 0 || max > 1 ? (value - min) / (max - min) : value;
    }
}
