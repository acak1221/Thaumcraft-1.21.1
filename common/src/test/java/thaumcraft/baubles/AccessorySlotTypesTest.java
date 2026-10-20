package thaumcraft.baubles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.world.item.ItemStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;

/** The four accessory slots and what each accepts: one amulet, two rings, one belt, in that order. */
class AccessorySlotTypesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theSlotsAreAmuletRingRingBelt() {
        assertEquals(BaubleType.AMULET, PlayerAccessoryInventory.slotType(0));
        assertEquals(BaubleType.RING, PlayerAccessoryInventory.slotType(1));
        assertEquals(BaubleType.RING, PlayerAccessoryInventory.slotType(2));
        assertEquals(BaubleType.BELT, PlayerAccessoryInventory.slotType(3));
    }

    @Test void thereAreExactlyFourSlots() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> PlayerAccessoryInventory.slotType(4));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> PlayerAccessoryInventory.slotType(-1));
    }

    @Test void nothingMatchesASlotOutsideTheFourOrWhenTheStackIsNotAnAccessory() {
        for (int slot = -2; slot <= 5; slot++) assertFalse(PlayerAccessoryInventory.matchesType(slot, ItemStack.EMPTY), "slot " + slot);
    }
}
