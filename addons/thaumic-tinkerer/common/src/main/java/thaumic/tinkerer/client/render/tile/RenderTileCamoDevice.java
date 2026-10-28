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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlockEntity;

/**
 * Draws the camouflage of a device: while a camouflage is set the device's own model is not rendered (its block state has
 * {@code camo=true}), and this renderer draws the block it imitates instead (1.7.10 asked the camouflaged block for the
 * icon of each side). The state is drawn like a block in a block entity renderer: from its model, lit by the light at the
 * device, without ambient occlusion and with the default tint.
 */
public class RenderTileCamoDevice<T extends CamoDeviceBlockEntity> implements BlockEntityRenderer<T> {

    private final BlockRenderDispatcher blockRenderer;

    public RenderTileCamoDevice(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T device, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState camo = device.getCamo();
        if (camo == null) return;
        blockRenderer.renderSingleBlock(camo, poseStack, buffer, packedLight, packedOverlay);
    }
}
