package thaumcraft.common.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.client.renderers.entity.LegacyVillagerSkins;

/** The original villager model has a head and nose, without the modern hat cube and brim. */
@Mixin(LivingEntityRenderer.class)
public abstract class VillagerHeadLayersMixin {
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 1, allow = 1)
    private void thaumcraft$originalVillagerHead(EntityModel<?> model, PoseStack poses, VertexConsumer vertices,
            int light, int overlay, int color, Operation<Void> original, @Local(argsOnly = true) LivingEntity entity) {
        if (!(entity instanceof Villager villager) || !(model instanceof VillagerModel<?> villagerModel)
                || LegacyVillagerSkins.texture(villager.getVillagerData().getProfession()) == null) {
            original.call(model, poses, vertices, light, overlay, color);
            return;
        }
        var hat = villagerModel.root().getChild("head").getChild("hat");
        var rim = hat.getChild("hat_rim");
        boolean hatVisible = hat.visible, rimVisible = rim.visible;
        hat.visible = false;
        rim.visible = false;
        try {
            original.call(model, poses, vertices, light, overlay, color);
        } finally {
            hat.visible = hatVisible;
            rim.visible = rimVisible;
        }
    }
}
