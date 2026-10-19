package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.client.renderers.models.ModelInfusionPillar;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.tiles.TileInfusionPillar;

/** TC4 TileInfusionPillarRenderer:33-46, with one complete OBJ per LOWER half. */
public final class TileInfusionPillarRenderer implements BlockEntityRenderer<TileInfusionPillar> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/pillar.png");
    private static final RenderType TRIANGLES = new PillarRenderType();
    private final ModelInfusionPillar model;

    public TileInfusionPillarRenderer(BlockEntityRendererProvider.Context context) {
        // BlockEntityRenderDispatcher reconstructs its renderers on resource
        // reload. Parse the CURRENT resource pack, never a static filesystem copy.
        model = new ModelInfusionPillar(Minecraft.getInstance().getResourceManager());
    }

    @Override public void render(TileInfusionPillar tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = tile.getBlockState();
        if (!state.hasProperty(BlockInfusionPillar.HALF) || !state.hasProperty(BlockInfusionPillar.FACING)
                || state.getValue(BlockInfusionPillar.HALF) != DoubleBlockHalf.LOWER) return;
        int angle = switch (state.getValue(BlockInfusionPillar.FACING)) {
            case SOUTH -> 90; // Original orientation 3.
            case WEST -> 270; // 4.
            case EAST -> 180; // 5.
            default -> 0; // NORTH / 2.
        };
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        model.render(pose, buffers.getBuffer(TRIANGLES), light, overlay);
        pose.popPose();
    }

    // The original OBJ reaches 2.038 blocks above its lower BE. Do not let the
    // dispatcher's one-block frustum box clip its tip or the upper half.
    @Override public boolean shouldRenderOffScreen(TileInfusionPillar tile) { return true; }

    private static final class PillarRenderType extends RenderType {
        private static final TextureStateShard PILLAR_TEXTURE = new TextureStateShard(TEXTURE, false, false);

        private PillarRenderType() {
            super("thaumcraft_infusion_pillar", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
                    16384, false, false, PillarRenderType::begin, PillarRenderType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState();
            PILLAR_TEXTURE.setupRenderState();
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
            PILLAR_TEXTURE.clearRenderState();
            RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();
        }
    }
}
