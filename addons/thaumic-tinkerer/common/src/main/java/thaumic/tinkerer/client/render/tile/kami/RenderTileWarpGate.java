/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 4:44:15 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile.kami;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Quaternionf;
import thaumic.tinkerer.client.model.kami.ModelSpinningCubes;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;

/**
 * What the Celestial Gateway draws besides its model: the glowing frame of its top face and the twelve small cubes that
 * circle two and a half blocks above it, with four older rings trailing behind them.
 *
 * <p>1.7.10's block renderer drew a second layer on the top face at full brightness ({@code setBrightness(255)}, the third
 * texture of the block). The model of the block has the holes of that frame (the transparent pixels of the top texture are
 * exactly the white pixels of the layer), and the layer is drawn here at full brightness over them. Plain JSON block models
 * of 1.21.1 have no emissive faces (NeoForge adds them with its own model data, Fabric does not).</p>
 */
public class RenderTileWarpGate implements BlockEntityRenderer<TileWarpGate> {
    /** The glow layer ({@code warp_gate_2}); in the block atlas. */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "block/warp_gate_2");
    /** A hair above the top face, so the edges of the layer meet the frame without a gap. */
    private static final float TOP = 1.0005F;
    /** 1.7.10: {@code repeat = 5}. */
    private static final int REPEAT = 5;
    private static final int CUBES = 12;
    private static final float HALF_SQRT_2 = (float) Math.sqrt(0.5D);

    public RenderTileWarpGate(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileWarpGate gate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        LocalPlayer player = Minecraft.getInstance().player;
        // 1.7.10 drove the motion with the client player's ticksExisted.
        double ticks = (player == null ? 0 : player.tickCount) + partialTick;
        glow(pose, buffers);
        pose.pushPose();
        pose.translate(0.5, 2.5, 0.5);
        // glRotatef(180, 1, 0, 1): half a turn about the diagonal of the block's footprint.
        pose.mulPose(new Quaternionf().rotateAxis((float) Math.PI, HALF_SQRT_2, 0F, HALF_SQRT_2));
        ModelSpinningCubes.renderSpinningCubes(pose, buffers, ticks, CUBES, REPEAT, REPEAT);
        pose.popPose();
    }

    /** The glow layer on the top face, at full brightness. */
    private static void glow(PoseStack pose, MultiBufferSource buffers) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(GLOW);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        vertex(consumer, pose, 0F, 1F, sprite.getU0(), sprite.getV1());
        vertex(consumer, pose, 1F, 1F, sprite.getU1(), sprite.getV1());
        vertex(consumer, pose, 1F, 0F, sprite.getU1(), sprite.getV0());
        vertex(consumer, pose, 0F, 0F, sprite.getU0(), sprite.getV0());
    }

    private static void vertex(VertexConsumer consumer, PoseStack pose, float x, float z, float u, float v) {
        consumer.addVertex(pose.last(), x, TOP, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0F, 1F, 0F);
    }
}
