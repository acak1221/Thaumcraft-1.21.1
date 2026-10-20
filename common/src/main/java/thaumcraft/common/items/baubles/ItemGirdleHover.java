package thaumcraft.common.items.baubles;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;

/** Original ItemGirdleHover: both-sided .33F subtraction, deliberately allowing a negative crossing. */
public final class ItemGirdleHover extends Item implements IBauble,IRunicArmor {
    public ItemGirdleHover(Properties properties){super(properties);}
    @Override public BaubleType getBaubleType(ItemStack stack){return BaubleType.BELT;}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
    @Override public void onWornTick(ItemStack stack,LivingEntity wearer){if(wearer.fallDistance>0)wearer.fallDistance-=.33F;}
}
