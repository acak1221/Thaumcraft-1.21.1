package thaumcraft.common.lib.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/** Dynamic native enchantment identities; definitions live in the registry datapack rather than a numeric-id registry. */
public final class EquipmentEnchantments {
    public static final ResourceKey<Enchantment> REPAIR = key("repair"), HASTE = key("haste");
    private EquipmentEnchantments() {}
    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }
    public static int level(ItemStack stack, Level world, ResourceKey<Enchantment> enchantment) {
        // A missing stack, a detached world or an unknown enchantment is "no
        // enchantment", never a crash in the living tick of a player.
        if (stack == null || stack.isEmpty() || world == null || enchantment == null) return 0;
        return world.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(enchantment))
                .map(holder -> stack.getEnchantments().getLevel(holder))
                .orElse(0);
    }
    /** The original LivingUpdateEvent runs on both sides before movement. Repair alone is server-side. */
    public static void playerLivingTick(Player player) {
        thaumcraft.common.lib.baubles.PlayerAccessories.tick(player);
        thaumcraft.common.items.armor.TravelGearTick.tick(player);
        if (!player.level().isClientSide && player.tickCount % 40 == 0 && !player.getAbilities().instabuild)
            EnchantmentRepair.tick(player);
        EnchantmentHaste.tick(player);
        thaumcraft.common.lib.runic.RunicShield.tick(player);
    }
}
