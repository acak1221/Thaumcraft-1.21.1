package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;

/** The public item and block registries of the native port. */
class ItemsBlocksApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void aNameIsResolvedInTheModNamespace() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumium_ingot"), ItemsTC.id("thaumium_ingot"));
        assertEquals(ResourceLocation.fromNamespaceAndPath("minecraft", "stone"), ItemsTC.id("minecraft:stone"));
        assertEquals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar"), BlocksTC.id("jar"));
    }

    @Test void theRegistriesAnswerRealRegisteredObjects() {
        assertSame(Items.STONE, ItemsTC.get("minecraft:stone"));
        assertSame(Blocks.STONE, BlocksTC.get("minecraft:stone"));
        assertTrue(ItemsTC.isRegistered("minecraft:stone"));
        assertFalse(ItemsTC.isRegistered("thaumcraft:not_a_real_item"));
        assertNull(ItemsTC.get("thaumcraft:not_a_real_item"));
        assertNull(BlocksTC.get("thaumcraft:not_a_real_block"));
        assertThrows(IllegalArgumentException.class, () -> ItemsTC.require("thaumcraft:not_a_real_item"));
    }

    @Test void theApiNamesMatchTheNativeRegistryIds() {
        // The API resolves the very identifiers the native deferred registers publish.
        for (var entry : thaumcraft.common.config.ThaumcraftContent.MATERIALS.entrySet()) {
            assertEquals(entry.getValue().getId(), ItemsTC.id(entry.getKey()),
                    "The public name of " + entry.getKey() + " must be its native registry id");
        }
        assertEquals(thaumcraft.common.config.ThaumcraftContent.JAR.getId(), BlocksTC.id("jar"));
        assertEquals(thaumcraft.common.config.ThaumcraftContent.AURA_NODE.getId(), BlocksTC.id("aura_node"));
    }

    @Test void theNativeCatalogueIsReachableThroughTheFacade() {
        assertEquals(thaumcraft.common.config.ThaumcraftContent.MATERIALS.get("thaumium_ingot").getId(),
                ItemsTC.id("thaumium_ingot"));
        // The native shard catalogue is keyed by the aspect tag, the registry id by the shard name.
        assertEquals(thaumcraft.common.config.ThaumcraftContent.SHARDS.get("aer").getId(), ItemsTC.id("air_shard"));
        assertEquals("thaumcraft", ItemsTC.id("thaumium_ingot").getNamespace());
    }

    @Test void theItemApiResolvesTheSameRegistry() {
        ItemStack stone = ItemApi.getItem("minecraft:stone", 0);
        assertNotNull(stone);
        assertSame(Items.STONE, stone.getItem());
        assertNull(ItemApi.getItem("thaumcraft:not_a_real_item", 0));
        ItemStack block = ItemApi.getBlock("minecraft:stone", 0);
        assertNotNull(block);
        assertSame(Items.STONE, block.getItem());
        assertNull(ItemApi.getBlock("thaumcraft:not_a_real_block", 0));
    }
}
