package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;

/** Native grindstone input/output verifies the alleged survival route to a VIS bar overflow. */
public final class WandSceptreGrindstoneRuntimeChecks {
    private WandSceptreGrindstoneRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        try (var fixture = new TubeVariantsRuntimeChecks.Fixture(test)) {
            fixture.open();
            var enchantment = test.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
            for (String rod : new String[]{"wood", "silverwood_staff"}) {
                ItemStack sceptre = new ItemStack(ThaumcraftContent.WAND.get());
                sceptre.set(ThaumcraftWandEnchantments.WAND_SCEPTRE.get(), Boolean.TRUE);
                int capacity = WandMaterials.capacity(rod, true);
                var data = new WandData(rod, "gold", Map.of("aer", capacity - 1));
                sceptre.set(ThaumcraftContent.WAND_DATA.get(), data);
                sceptre.enchant(enchantment, 1);
                var menu = new GrindstoneMenu(73, fixture.player.getInventory(), ContainerLevelAccess.NULL);
                menu.getSlot(0).set(sceptre);
                ItemStack output = menu.getSlot(2).getItem().copy();
                test.assertTrue(!output.isEmpty(), "native grindstone emits a disenchantable sceptre: " + rod);
                test.assertTrue(!EnchantmentHelper.hasAnyEnchantments(output), "native grindstone removed the real enchantment");
                test.assertTrue(EnchantmentHelper.hasAnyEnchantments(sceptre), "preview did not mutate the input");
                test.assertTrue(ThaumcraftWandEnchantments.isSceptre(output), "sceptre identity is a preserved custom component, not an enchantment");
                test.assertTrue(data.equals(ItemWandCasting.data(output)), "all stored vis and rod/cap survive native disenchantment");
                test.assertTrue(ItemWandCasting.getMaxVis(output) == capacity, "the sceptre capacity does not shrink after disenchantment");
                test.assertTrue(ItemWandCasting.data(output).amount("aer") <= ItemWandCasting.getMaxVis(output), "near-full contents still fit the preserved capacity");
                menu.getSlot(0).set(ItemStack.EMPTY);
            }
        }
        test.succeed();
    }
}
