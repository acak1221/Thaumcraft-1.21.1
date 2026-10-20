package thaumcraft.common.lib.enchantment;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Original EventHandlerEntity:322-339; this is added forward acceleration, not an attribute/potion multiplier. Original isAirBorne maps to native hasImpulse. */
public final class EnchantmentHaste {
    private EnchantmentHaste() {}
    public static void tick(Player player) {
        if (player.getAbilities().flying || player.zza <= 0) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int level = EquipmentEnchantments.level(boots, player.level(), EquipmentEnchantments.HASTE);
        if (level <= 0) return;
        float bonus = EquipmentEnchantmentRules.hasteAcceleration(level, player.hasImpulse, player.isInWater());
        // Original Entity.moveFlying(0,1,bonus): retain FLOAT yaw conversion/products before adding to double velocity.
        // Native moveRelative instead performs the vector products in double and pre-multiplies the yaw conversion constant.
        float angle = player.getYRot() * (float)Math.PI / 180F;
        float sin = Mth.sin(angle), cos = Mth.cos(angle);
        player.setDeltaMovement(player.getDeltaMovement().add(0F * cos - bonus * sin, 0, bonus * cos + 0F * sin));
    }
}
