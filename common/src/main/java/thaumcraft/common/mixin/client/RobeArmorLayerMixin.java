package thaumcraft.common.mixin.client;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.items.armor.ItemRobeArmor;

/** Native armor color alone requires DYEABLE even when the material layer is
 * dyeable. Opt robes into THIS rendering check, never the vanilla dye recipe or
 * cauldron tag: those would change TC4's palette/default/creative wash costs. */
@Mixin(HumanoidArmorLayer.class)
public abstract class RobeArmorLayerMixin {
    @Redirect(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean thaumcraft$renderOriginalRobeColor(ItemStack stack, TagKey<Item> tag) {
        return tag.equals(ItemTags.DYEABLE) && stack.getItem() instanceof ItemRobeArmor || stack.is(tag);
    }
}
