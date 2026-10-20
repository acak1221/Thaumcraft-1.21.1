package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelCube;
import thaumcraft.common.tiles.TileEtherealBloom;

/**
 * Original TileEtherealBloomRenderer: the ethereal bloom.
 *
 * <p>The original grows the bloom with the growth counter of the tile: four
 * leaf quads, four petal quads, four stalk quads, the crystal capacitor cube
 * in the middle and the light strip that shines out of the plant.
 *
 * <p>Deviation: the original light strip is a 1.7 fixed function helper that
 * scrolls a texture along the plant. The native port draws the same strip with
 * the same {@code nodes.png} texture as an additive quad, which keeps the
 * original texture and colour but not the original scrolling.
 */
public final class TileEtherealBloomRenderer implements BlockEntityRenderer<TileEtherealBloom> {
    /** The original texture of the crystal capacitor in the middle of the bloom. */
    public static final ResourceLocation CAPACITOR = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/crystalcapacitor.png");
    /** The original texture of the light strip. */
    public static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/nodes.png");

    private final ModelCube cube = new ModelCube(0);

    public TileEtherealBloomRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEtherealBloom tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float growth = tile.growthCounter + partialTick;
        float petals = Math.min(growth, 100.0F);
        float leaves = Math.min(growth, 50.0F);
        float stem = Math.max(0.0F, Math.min(growth - 33.0F, 33.0F));
        float core = Math.max(0.0F, Math.min(growth - 66.0F, 33.0F));
        float scalePetals = petals / 100.0F;
        float scaleLeaves = leaves / 60.0F + .1666666F;
        float scaleStem = stem / 33.0F;
        float scaleCore = core / 33.0F * .7F;
        // The light strip of the original.
        pose.pushPose();
        try {
            pose.translate(.5F, scalePetals, .5F);
            var strip = buffers.getBuffer(RenderType.entityTranslucent(NODES));
            quad(pose, strip, .5F, .5F, 0xAADDFF, light, overlay);
        } finally {
            pose.popPose();
        }
        // The crystal capacitor in the middle of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F - scaleCore / 8.0F, scalePetals - scaleCore / 6.0F, .5F - scaleCore / 8.0F);
            pose.scale(scaleCore / 4.0F, scaleCore / 3.0F, scaleCore / 4.0F);
            cube.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CAPACITOR)), light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite leavesSprite = atlas.apply(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/purifier_leaves"));
        TextureAtlasSprite stalkSprite = atlas.apply(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/purifier_stalk"));
        var leafBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        // The four leaves of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F, .25F, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.scale(scaleStem, scalePetals, scaleStem);
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    sprite(pose, leafBuffer, leavesSprite, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // The four petals of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F, .6F, .5F);
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.scale(scaleCore, scalePetals * .7F, scaleCore);
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    sprite(pose, leafBuffer, leavesSprite, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // The four stalks of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F, .5F, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.translate(0.0F, (1.0F - scalePetals) / 2.0F, 0.0F);
                    pose.scale(scaleLeaves, scalePetals, scaleLeaves);
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    sprite(pose, leafBuffer, stalkSprite, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
    }

    /** A centred quad of the size of one block. */
    private static void sprite(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite, int light, int overlay) {
        float[][] corners = {{-.5F, .5F}, {.5F, .5F}, {.5F, -.5F}, {-.5F, -.5F}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(uv[index][0]), sprite.getV(uv[index][1]))
                    .setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
        }
    }

    /** The light strip of the original, a quad of half a block. */
    private static void quad(PoseStack pose, VertexConsumer out, float half, float height, int color, int light, int overlay) {
        float[][] corners = {{-half, height}, {half, height}, {half, 0.0F}, {-half, 0.0F}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                    .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                    .setUv(uv[index][0], uv[index][1]).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
        }
    }
}
