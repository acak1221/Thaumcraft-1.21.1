package thaumcraft.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;
import thaumcraft.common.lib.utils.CropUtils;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * The registry lookups of the shared helper layer.
 *
 * <p>Every one of them answers "no value" instead of throwing when the world, the stack or the
 * registry entry is missing: the original 1.7.10 code never had a registry lookup that could fail,
 * so 1.21.1 has to answer the same question without a NullPointerException.</p>
 */
class NullSafeRegistryLookupTest {

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theCropKeyOfAMissingBlockDoesNotThrow() {
        assertEquals("#3", CropUtils.key((net.minecraft.world.level.block.Block) null, 3));
    }

    @Test void theCropKeyOfARegisteredBlockIsTheRegistryForm() {
        assertEquals("minecraft:wheat#3", CropUtils.key(Blocks.WHEAT, 3));
    }

    @Test void aMissingStackHasNoEnchantmentLevel() {
        assertEquals(0, EquipmentEnchantments.level(null, null, EquipmentEnchantments.HASTE));
        assertEquals(0, EquipmentEnchantments.level(ItemStack.EMPTY, null, EquipmentEnchantments.HASTE));
    }

    @Test void aDetachedWorldHasNoEnchantmentLevel() {
        assertEquals(0, EquipmentEnchantments.level(new ItemStack(Items.IRON_PICKAXE), null,
                EquipmentEnchantments.HASTE));
    }

    @Test void aMissingStackHasNoUnbreakingLevel() {
        assertEquals(0, InventoryUtils.unbreakingLevel(null, null));
    }

    @Test void aDetachedWorldHasNoUnbreakingLevel() {
        assertEquals(0, InventoryUtils.unbreakingLevel(null, new ItemStack(Items.IRON_PICKAXE)));
    }

    @Test void theRepairEnchantmentIdentityIsStable() {
        assertNotNull(EquipmentEnchantments.REPAIR.location());
        assertEquals("thaumcraft", EquipmentEnchantments.REPAIR.location().getNamespace());
        assertEquals("repair", EquipmentEnchantments.REPAIR.location().getPath());
    }
}
