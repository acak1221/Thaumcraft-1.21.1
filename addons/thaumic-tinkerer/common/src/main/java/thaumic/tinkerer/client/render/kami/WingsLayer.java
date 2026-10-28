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
package thaumic.tinkerer.client.render.kami;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import thaumic.tinkerer.client.model.kami.ModelWings;
import thaumic.tinkerer.common.item.kami.armor.ItemGemChest;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Draws the wings of the Robes of the Stratosphere on a player who wears them (1.7.10 {@code ItemGemChest.getArmorModel}).
 * They are drawn with the texture of the awakened armour, and flap while the player flies.
 *
 * <p>The loader's glue adds this layer to the player renderers (Fabric: {@code LivingEntityFeatureRendererRegistrationCallback}).</p>
 */
public class WingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The sheet of the awakened armour's outer layer: the wings are two regions of it (copied from the 1.7.10 texture {@code ichor_gem1}). */
    private static final ResourceLocation TEXTURE = LibMisc.id("textures/models/armor/ichor_gem_layer_1.png");

    private final ModelPart wing1;
    private final ModelPart wing2;

    public WingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(ModelWings.LAYER);
        this.wing1 = root.getChild(ModelWings.WING_1);
        this.wing2 = root.getChild(ModelWings.WING_2);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ItemGemChest)) return;

        if (player.getAbilities().flying) {
            wing1.yRot = (float) ((Math.sin(player.tickCount) + 1) * (Math.PI / 180F) * 15 + ModelWings.WING_1_REST);
            wing2.yRot = -wing1.yRot;
        } else {
            wing1.yRot = ModelWings.WING_1_REST;
            wing2.yRot = ModelWings.WING_2_REST;
        }

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(TEXTURE));
        wing1.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        wing2.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
