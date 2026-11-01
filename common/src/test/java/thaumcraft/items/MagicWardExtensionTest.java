package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.armor.MagicWardGear;
import thaumcraft.common.lib.MagicWard;

class MagicWardExtensionTest {
    @Test void defaultPrioritySharesKeepTheOriginalSourceDivisorsAndSetFactor() {
        MagicWardGear fortress = new MagicWardGear() {
            @Override public int wardArmor(ItemStack stack) { return 7; }
            @Override public boolean wardsFire() { return true; }
            @Override public double wardFactor(ItemStack stack, LivingEntity wearer) { return 1.2; }
        };
        assertEquals(.24,fortress.wardShare(null,null,MagicWard.Kind.MAGIC),1E-12);
        assertEquals(.42,fortress.wardShare(null,null,MagicWard.Kind.FIRE),1E-12);
        assertEquals(0,fortress.wardShare(null,null,MagicWard.Kind.NONE));
        MagicWardGear robe = stack -> 6;
        assertEquals(6D/35D,robe.wardShare(null,null,MagicWard.Kind.MAGIC),1E-12);
        assertEquals(0,robe.wardShare(null,null,MagicWard.Kind.FIRE));
        MagicWardGear broken = stack -> 0;
        assertEquals(0,broken.wardShare(null,null,MagicWard.Kind.MAGIC));
    }
}
