package thaumcraft.baubles;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;

import static org.junit.jupiter.api.Assertions.*;

class AccessorySaveIntegrityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void wrongListElementTypeIsPreservedInsteadOfSavedAsEmpty() {
        CompoundTag malformed = new CompoundTag();
        ListTag items = new ListTag();
        items.add(StringTag.valueOf("unreadable accessory payload"));
        malformed.put("Items", items);
        PlayerAccessoryInventory inventory = new PlayerAccessoryInventory(null);
        inventory.load(malformed, null);
        assertTrue(inventory.isRejected());
        assertEquals(malformed, inventory.save(null));
        CompoundTag saved = inventory.save(null);
        saved.remove("Items");
        assertEquals(malformed, inventory.save(null), "quarantine must own its NBT");
        inventory.load(inventory.save(null), null);
        assertTrue(inventory.isRejected());
        assertEquals(malformed, inventory.save(null));
    }

    @Test void emptyAndAbsentInventoriesRemainDistinct() {
        PlayerAccessoryInventory inventory = new PlayerAccessoryInventory(null);
        CompoundTag empty = new CompoundTag();
        empty.put("Items", new ListTag());
        inventory.load(empty, null);
        assertFalse(inventory.isRejected());
        assertEquals(empty, inventory.save(null));
        CompoundTag absent = new CompoundTag();
        inventory.load(absent, null);
        assertTrue(inventory.isRejected());
        assertEquals(absent, inventory.save(null));
    }
}
