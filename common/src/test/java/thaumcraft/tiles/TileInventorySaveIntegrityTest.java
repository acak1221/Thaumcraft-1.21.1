package thaumcraft.tiles;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.tiles.TileThaumcraftInventory;

import static org.junit.jupiter.api.Assertions.*;

class TileInventorySaveIntegrityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final class Inventory extends TileThaumcraftInventory {
        Inventory() { super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), 2, new int[]{1}); }
        CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag, registries);
            return tag;
        }
        void load(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }
    }

    @Test void replacingWithAnEmptySnapshotClearsBothSlotGroups() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Inventory tile = new Inventory();
        CompoundTag empty = tile.save(registries);
        tile.setItem(0, new ItemStack(Items.DIRT, 7));
        tile.setItem(1, new ItemStack(Items.STONE, 1));
        tile.load(empty, registries);
        assertTrue(tile.isEmpty());
        assertEquals(empty, tile.save(registries));
    }

    @Test void removingASyncedStackClearsTheReceivingInstance() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Inventory server = new Inventory(), client = new Inventory();
        server.setItem(1, new ItemStack(Items.STONE));
        client.load(server.getUpdateTag(registries), registries);
        assertEquals(Items.STONE, client.getItem(1).getItem());
        server.removeItem(1, 1);
        client.load(server.getUpdateTag(registries), registries);
        assertTrue(client.getItem(1).isEmpty());
    }

    @Test void populatedDiskRoundTripPreservesBothSlotGroups() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Inventory source = new Inventory(), restored = new Inventory();
        source.setItem(0, new ItemStack(Items.DIRT, 7));
        source.setItem(1, new ItemStack(Items.STONE, 1));
        CompoundTag saved = source.save(registries);
        restored.load(saved, registries);
        assertEquals(saved, restored.save(registries));
    }
}
