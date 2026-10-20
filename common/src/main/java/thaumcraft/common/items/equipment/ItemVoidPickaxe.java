package thaumcraft.common.items.equipment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.items.VoidMetal;

/** Original ItemVoidPickaxe: repairs itself, withers what it hits for four seconds, adds one warp. */
public class ItemVoidPickaxe extends PickaxeItem implements IRepairable, IWarpingGear {
    public ItemVoidPickaxe(Tier tier, Properties properties) {
        // Legacy pickaxe 2 + material 3 = additive 5; the attack speed is a 1.21 combat adapter.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(tier, 2, -2.8F)));
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetal.repair(stack, entity);
    }

    // The original hook is the attacking player's left click; a landed hit is the native equivalent.
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        VoidMetal.wither(target, attacker, VoidMetal.TOOL_WITHER, false);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override public int getWarp(ItemStack stack, Player player) { return 1; }
}
