/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.core.handler.kami;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumic.tinkerer.common.item.kami.ItemPlacementMirror;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The phantom blocks of the Placement Mirror: while a player holds the mirror, the blocks it would place are drawn
 * translucent where they would go, if the player has the blocks (1.7.10: a {@code RenderWorldLastEvent} handler, alpha
 * 0.6). Architectury has no event for the end of the world render, so the loader's glue calls {@link #render} from the
 * render event of its platform (Fabric: {@code WorldRenderEvents.AFTER_TRANSLUCENT}), when
 * {@code ConfigHandler.showPlacementMirrorBlocks} is on.
 */
public final class PlacementMirrorPredictionRenderer {
    private static final float ALPHA = 0.6F;

    private PlacementMirrorPredictionRenderer() {}

    /**
     * @param poseStack the world render's pose stack (camera at the origin of the rotation, not of the translation)
     * @param buffers   the world's buffer source, which this method ends for the translucent block layer
     * @param camera    the camera of the frame
     */
    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || !TTRegistry.hasItem(LibItemNames.PLACEMENT_MIRROR)) return;
        var mirror = TTRegistry.item(LibItemNames.PLACEMENT_MIRROR);
        boolean drew = false;
        for (Player player : level.players()) {
            ItemStack current = player.getMainHandItem();
            if (current.is(mirror) && renderPlayerLook(poseStack, buffers, camera.getPosition(), player, current)) {
                drew = true;
            }
        }
        if (drew) buffers.endBatch(RenderType.translucent());
    }

    private static boolean renderPlayerLook(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera, Player player,
            ItemStack stack) {
        BlockState required = ItemPlacementMirror.getBlockState(stack);
        if (required == null) return false;
        List<BlockPos> coords = ItemPlacementMirror.getBlocksToPlace(stack, player);
        if (coords.isEmpty() || !ItemPlacementMirror.hasBlocks(player, required.getBlock(), coords.size())) return false;

        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blocks.getBlockModel(required);
        VertexConsumer consumer = new AlphaVertexConsumer(buffers.getBuffer(RenderType.translucent()), ALPHA);
        for (BlockPos pos : coords) {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            blocks.getModelRenderer().renderModel(poseStack.last(), consumer, required, model, 1F, 1F, 1F,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        return true;
    }

    /** A vertex consumer that multiplies the alpha of everything that passes through it. */
    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alphaValue) {
            delegate.setColor(red, green, blue, (int) (alphaValue * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
