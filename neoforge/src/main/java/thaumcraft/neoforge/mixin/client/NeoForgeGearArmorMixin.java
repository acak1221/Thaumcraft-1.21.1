package thaumcraft.neoforge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.ThaumcraftGearModelsClient;

/**
 * The NeoForge half of the original TC4 gear models.
 *
 * <p>NeoForge 21.1 does own a native hook for the armour model: the NeoForge patch of
 * {@code net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer.java} adds
 * {@code protected Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot,
 * A model)}, which {@code net.neoforged.neoforge.client.ClientHooks.getArmorModel} forwards to
 * {@code IClientItemExtensions.getGenericArmorModel}. The hook can only return a
 * {@code Model}, so it can carry the void robe and the fortress armour, but not the hover
 * harness: the original harness renderer draws the chest cube AND the retained 124 triangle OBJ
 * pack on the back with its own unlit render type, and an OBJ pack has no model expression.</p>
 *
 * <p>This mixin therefore draws all three families of the NeoForge loader, and it draws them
 * with the very same shared renderers the Fabric loader draws them with
 * ({@code thaumcraft.client.ThaumcraftGearModelsClient#renderGearItem}), so the two loaders
 * cannot draw the gear differently. It intercepts the same method the Fabric API intercepts:
 * the Fabric mixin {@code net.fabricmc.fabric.mixin.client.rendering.ArmorFeatureRendererMixin}
 * injects into {@code HumanoidArmorLayer#renderArmorPiece}, which the refmap of the Fabric API
 * resolves to the six argument overload of that method.</p>
 *
 * <p>The target and its descriptor were read from the NeoForge-patched class
 * {@code net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer.class} of the patched
 * Minecraft archive of the NeoForge 21.1.259 userdev jar:
 * {@code private void renderArmorPiece(PoseStack, MultiBufferSource, LivingEntity, EquipmentSlot,
 * int, HumanoidModel)}. The six argument overload of the NeoForge patch only delegates to the
 * twelve argument one, so cancelling it skips the whole native draw of the piece.</p>
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class NeoForgeGearArmorMixin {
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void thaumcraft$originalGear(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                         EquipmentSlot slot, int light, HumanoidModel<?> model, CallbackInfo ci) {
        ItemStack armor = entity.getItemBySlot(slot);
        if (armor.isEmpty()) return;
        if (!ThaumcraftGearModelsClient.renderGearItem(pose, buffers, armor, entity, slot, light, parentModel())) return;
        ci.cancel();
    }

    /** The wearer's own humanoid model, exactly the context the Fabric renderer receives. */
    @SuppressWarnings("unchecked")
    private HumanoidModel<LivingEntity> parentModel() {
        return (HumanoidModel<LivingEntity>) ((RenderLayer) (Object) this).getParentModel();
    }
}
