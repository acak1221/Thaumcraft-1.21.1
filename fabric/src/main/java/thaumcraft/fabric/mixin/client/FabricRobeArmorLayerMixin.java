package thaumcraft.fabric.mixin.client;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.items.armor.ItemRobeArmor;

/**
 * The Fabric half of the original robe dye in the native armour layer.
 *
 * <p>Native armor color alone requires DYEABLE even when the material layer is
 * dyeable. Opt robes into THIS rendering check, never the vanilla dye recipe or
 * cauldron tag: those would change TC4's palette/default/creative wash costs.</p>
 *
 * <p>This mixin is loader specific on purpose. NeoForge 21.1 replaces the
 * {@code ItemStack.is(TagKey)} call of {@code HumanoidArmorLayer#renderArmorPiece} with its own
 * client item extension, so the redirect has no target there and would fail the whole mixin
 * application at the start of a NeoForge client. The NeoForge half of the same behaviour is
 * the native {@code IClientItemExtensions#getDefaultDyeColor} of the robe items.</p>
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class FabricRobeArmorLayerMixin {
    @Redirect(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean thaumcraft$renderOriginalRobeColor(ItemStack stack, TagKey<Item> tag) {
        return tag.equals(ItemTags.DYEABLE) && stack.getItem() instanceof ItemRobeArmor || stack.is(tag);
    }
}
