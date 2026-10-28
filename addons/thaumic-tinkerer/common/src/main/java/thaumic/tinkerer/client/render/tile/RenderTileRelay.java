/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 17:55:04 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.client.model.ModelRelay;
import thaumic.tinkerer.common.block.tile.TileEntityRelay;

/**
 * The relay's special renderer of the original. <b>1.7.10 never registered it</b> (the relay is drawn as an ordinary
 * textured cube) and its texture ({@code textures/models/relay.png}) does not exist in the original resources, so this
 * class is ported but {@code TileBlocksClient} does not register it either; it is here so that nothing of the original
 * is silently dropped, and works once somebody draws the texture and registers it.
 */
public class RenderTileRelay implements BlockEntityRenderer<TileEntityRelay> {

    private static final ResourceLocation texture = ResourceLocation.parse(LibResources.MODEL_RELAY_TEXTURE);
    private final ModelRelay model = new ModelRelay(ModelRelay.createBodyLayer().bakeRoot());

    @Override
    public void render(TileEntityRelay relay, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.scale(1F, -1F, -1F);
        model.render(poseStack, buffer.getBuffer(RenderType.entityCutout(texture)), packedLight, packedOverlay);
        poseStack.popPose();
    }
}
