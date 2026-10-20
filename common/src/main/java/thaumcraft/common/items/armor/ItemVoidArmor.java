package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.items.VoidMetal;

/**
 * Original ItemVoidArmor: repairs itself (once from the inventory tick and, while worn, once more from the armor tick, so a worn piece
 * mends two points every second), adds one warp, a runic charge of zero.
 */
public class ItemVoidArmor extends ArmorItem implements IRepairable, IRunicArmor, IWarpingGear {
    public ItemVoidArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(type.getDurability(10)).rarity(Rarity.UNCOMMON));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; } // Original constant, not an implemented shield.

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetal.repair(stack, entity);
        // Original onArmorTick: the worn piece, on the same tick, once more.
        if (entity instanceof Player player && player.getItemBySlot(getEquipmentSlot()) == stack) VoidMetal.repair(stack, entity);
    }

    @Override public int getWarp(ItemStack stack, Player player) { return 1; }
}
