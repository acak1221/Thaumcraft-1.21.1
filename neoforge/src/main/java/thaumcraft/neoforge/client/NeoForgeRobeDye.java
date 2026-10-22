package thaumcraft.neoforge.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.common.items.armor.ItemRobeArmor;

/**
 * The NeoForge half of the original robe dye in the native armour layer.
 *
 * <p>NeoForge 21.1 replaces the {@code ItemStack.is(ItemTags.DYEABLE)} check of the native
 * armour layer with its own client item extension, so the shared Fabric redirect has no
 * target in the NeoForge patched class (it would fail the whole mixin application at the
 * start of a NeoForge client). The NeoForge half of the same behaviour is therefore the
 * native {@link IClientItemExtensions#getDefaultDyeColor(ItemStack)} of the robe items: the
 * colour is the shared {@link ItemRobeArmor#armorTint(ItemStack)}, which is the very value
 * the native armour layer of the Fabric loader computes for a dyeable layer.</p>
 */
public final class NeoForgeRobeDye {
    private NeoForgeRobeDye() {}

    /** Publishes the original robe colour for every robe of the port. */
    public static void register(RegisterClientExtensionsEvent event) {
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof ItemRobeArmor) {
                event.registerItem(new IClientItemExtensions() {
                    @Override public int getDefaultDyeColor(ItemStack stack) {
                        return ItemRobeArmor.armorTint(stack);
                    }
                }, item);
            }
        }
    }
}
