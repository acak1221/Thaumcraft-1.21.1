package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelAlembic;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;

/** Original OBJ and original texture, including facing and furnace/stack support groups. */
public final class TileAlembicRenderer implements BlockEntityRenderer<TileAlembic> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/alembic.png");
    private static final ResourceLocation LABEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/label.png");
    private static final RenderType TRIANGLES = new AlembicRenderType();
    private final ModelAlembic model;

    public TileAlembicRenderer(BlockEntityRendererProvider.Context context) {
        // The dispatcher recreates this renderer on resource reload, so the
        // model respects resource packs without a global parser/reload cache.
        model = new ModelAlembic(Minecraft.getInstance().getResourceManager());
    }

    @Override public void render(TileAlembic tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tile.getLevel();
        var below = level == null ? null : level.getBlockEntity(tile.getBlockPos().below());
        int angle = originalAngle(tile.getFacing());
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.pushPose();
        // The source OBJ is Z-up. Source renderer:44,49-60 applies -90 X,
        // then a Z rotation (WEST0/EAST180/SOUTH90/NORTH270).
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        var buffer = buffers.getBuffer(TRIANGLES);
        if (below instanceof TileAlchemyFurnace) {
            model.render("TubeMain", pose, buffer, light, overlay);
            model.render("Legs", pose, buffer, light, overlay);
        } else if (below instanceof TileAlembic) {
            model.render("TubeMain", pose, buffer, light, overlay);
            model.render("TubeSmall", pose, buffer, light, overlay);
        } else {
            model.render("Legs", pose, buffer, light, overlay);
        }
        model.render("Pot", pose, buffer, light, overlay);
        model.render("Panel", pose, buffer, light, overlay);
        pose.popPose();
        if (tile.getFilter() != null) {
            // Source renderer:78-103's bound label. This visual reflects BE
            // state only; it does not pretend a label-item attachment API exists.
            pose.mulPose(Axis.YP.rotationDegrees(90 + angle));
            quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(LABEL)), .135F, .468F, -.409F, 0xffffff, light, overlay);
            quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tile.getFilter().getImage())), .104F, .468F, -.41F,
                    tile.getFilter().getColor(), light, overlay);
        }
        pose.popPose();
    }

    private static int originalAngle(Direction facing) {
        return switch (facing) {
            case EAST -> 180;
            case SOUTH -> 90;
            case NORTH -> 270;
            default -> 0;
        };
    }

    private static void quad(PoseStack pose, VertexConsumer buffer, float half, float y, float z, int color, int light, int overlay) {
        vertex(pose, buffer, -half, y + half, z, 0, 0, color, light, overlay);
        vertex(pose, buffer, half, y + half, z, 1, 0, color, light, overlay);
        vertex(pose, buffer, half, y - half, z, 1, 1, color, light, overlay);
        vertex(pose, buffer, -half, y - half, z, 0, 1, color, light, overlay);
    }

    private static void vertex(PoseStack pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int color, int light, int overlay) {
        buffer.addVertex(pose.last(), x, y, z).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
    }

    /** Do not feed triangles into Minecraft's quad entity render types. */
    private static final class AlembicRenderType extends RenderType {
        private static final TextureStateShard TEXTURE_STATE = new TextureStateShard(TEXTURE, false, false);

        private AlembicRenderType() {
            super("thaumcraft_alembic", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
                    16384, false, false, AlembicRenderType::begin, AlembicRenderType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState();
            TEXTURE_STATE.setupRenderState();
            NO_TRANSPARENCY.setupRenderState();
            NO_CULL.setupRenderState();
            LIGHTMAP.setupRenderState();
            OVERLAY.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_DEPTH_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_DEPTH_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            OVERLAY.clearRenderState();
            LIGHTMAP.clearRenderState();
            NO_CULL.clearRenderState();
            NO_TRANSPARENCY.clearRenderState();
            TEXTURE_STATE.clearRenderState();
            RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();
        }
    }
}
