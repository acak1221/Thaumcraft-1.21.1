/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlockEntity;

/**
 * Draws the camouflage of a device: while a camouflage is set the device's own model is not rendered (its block state has
 * {@code camo=true}), and this renderer draws the block it imitates instead (1.7.10 asked the camouflaged block for the
 * icon of each side).
 *
 * <p>The state is drawn the way the piston renderer draws a moving block: from its model, at the device's position, in the
 * world. That gives the lighting of the surroundings (the light of the six neighbours, ambient occlusion included, not the
 * light at the device's own cell, which is dark for an opaque device such as the Golem Connector), the tint of the position
 * and the culling of the faces that rest against an opaque neighbour. It does not get the chunk mesh's own batching or
 * the models of mods that hook the chunk renderer.</p>
 */
public class RenderTileCamoDevice<T extends CamoDeviceBlockEntity> implements BlockEntityRenderer<T> {

    private final BlockRenderDispatcher blockRenderer;
    /** The model renderer reseeds the source for every quad list; it is the render thread's own, not the level's. */
    private final RandomSource random = RandomSource.create();

    public RenderTileCamoDevice(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T device, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState camo = device.getCamo();
        Level level = device.getLevel();
        if (camo == null || level == null) return;
        VertexConsumer consumer = buffer.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(camo));
        blockRenderer.renderBatched(camo, device.getBlockPos(), level, poseStack, consumer, true, random);
    }
}
