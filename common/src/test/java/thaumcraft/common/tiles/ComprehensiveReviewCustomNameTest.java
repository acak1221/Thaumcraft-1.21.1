package thaumcraft.common.tiles;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** A malformed cosmetic field must not make an otherwise valid stock owner disappear. */
class ComprehensiveReviewCustomNameTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final List<Supplier<RetainedDataBlockEntity>> owners = List.of(
            () -> new TileThaumcraftInventory(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState()),
            () -> new TilePedestal(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState()));

    @Test void malformedNamesQuarantineTheWholeOwnerWithoutThrowingOrLosingItsStack() {
        List<Tag> names = List.of(StringTag.valueOf("{"), StringTag.valueOf("null"),
                StringTag.valueOf(""), IntTag.valueOf(7));
        for (var factory : owners) for (Tag name : names) {
            var source = factory.get();
            ((Container) source).setItem(0, new ItemStack(Items.DIAMOND));
            CompoundTag damaged = save(source);
            damaged.put("CustomName", name.copy());
            var restored = factory.get();
            assertDoesNotThrow(() -> restored.loadAdditional(damaged, registries), source.getClass().getSimpleName() + ": " + name);
            assertTrue(restored.isDataRejected(), "An unread name is part of the same recoverable stock owner");
            assertEquals(damaged, save(restored), "Quarantine retains the original name, item and count together");
            ((Container) restored).setItem(0, new ItemStack(Items.DIRT));
            assertEquals(damaged, save(restored), "A refused owner cannot silently overwrite the retained stock");
            restored.setRemoved();
            assertEquals(damaged, save(restored), "Retirement must not normalize malformed data into an empty inventory");
        }
    }

    @Test void validNamesAndUnnamedSnapshotsStillLoadAndRepairAQuarantinedOwner() {
        for (var factory : owners) {
            var source = factory.get();
            ((Container) source).setItem(0, new ItemStack(Items.DIAMOND));
            CompoundTag named = save(source);
            Component name = Component.literal("Named stock owner").withStyle(net.minecraft.ChatFormatting.GOLD);
            named.putString("CustomName", Component.Serializer.toJson(name, registries));
            var restored = factory.get();
            restored.loadAdditional(named, registries);
            assertFalse(restored.isDataRejected());
            assertEquals(name, ((net.minecraft.world.Nameable) restored).getCustomName());
            assertTrue(((Container) restored).getItem(0).is(Items.DIAMOND));

            CompoundTag damaged = named.copy(); damaged.putString("CustomName", "{");
            assertDoesNotThrow(() -> restored.loadAdditional(damaged, registries));
            assertTrue(restored.isDataRejected());
            CompoundTag unnamed = named.copy(); unnamed.remove("CustomName");
            restored.loadAdditional(unnamed, registries);
            assertFalse(restored.isDataRejected(), "A later complete valid snapshot repairs this owner");
            assertNull(((net.minecraft.world.Nameable) restored).getCustomName());
            assertTrue(((Container) restored).getItem(0).is(Items.DIAMOND));
        }
    }

    private CompoundTag save(RetainedDataBlockEntity tile) {
        CompoundTag tag = new CompoundTag();
        tile.saveAdditional(tag, registries);
        return tag;
    }
}
