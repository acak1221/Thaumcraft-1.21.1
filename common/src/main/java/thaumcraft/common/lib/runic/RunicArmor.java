package thaumcraft.common.lib.runic;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IRunicArmor;
import thaumcraft.common.config.ThaumcraftRunic;

/** Original signed NBT byte RS.HARDEN, represented by a bounded native component. */
public final class RunicArmor {
    private RunicArmor() {}
    public static int hardening(ItemStack stack) {
        return stack.isEmpty() || !(stack.getItem() instanceof IRunicArmor) ? 0 : stack.getOrDefault(ThaumcraftRunic.HARDENING.get(), 0);
    }
    public static int finalCharge(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IRunicArmor armor ? armor.getRunicCharge(stack) + hardening(stack) : 0;
    }
    public static ItemStack augment(ItemStack central) {
        if (central.isEmpty() || !(central.getItem() instanceof IRunicArmor)) return ItemStack.EMPTY;
        ItemStack result = central.copy();
        result.set(ThaumcraftRunic.HARDENING.get(), RunicRules.nextHardening(hardening(central)));
        return result;
    }
}
