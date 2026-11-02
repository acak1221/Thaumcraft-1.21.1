package thaumcraft.common.lib.utils;

import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Persistent output kinds are explicit; an old untyped row must stay an ordinary item. */
class ComprehensiveReviewDropQueueCodecTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final BlockPos SOURCE = new BlockPos(2, 64, 3);

    @Test void allPreviousQueueVersionsKeepOrdinaryOutputWithoutOptingIntoGratePhysics() {
        for (int version = 1; version <= 4; version++) {
            CompoundTag row = row();
            row.putInt("Count", 7);
            var restored = DeferredItemDrops.load(envelope(version, row), registries);
            assertEquals(1, restored.pendingCount()); assertEquals(0, restored.quarantinedCount());
            CompoundTag saved = restored.save(new CompoundTag(), registries).getList("Pending", Tag.TAG_COMPOUND).getCompound(0);
            assertEquals(7, saved.getInt("Count")); assertFalse(saved.contains("GrateDrop"));
            assertEquals(row, saved, "An old row retains its stock and original motion without a new entity kind");
        }
    }

    @Test void grateKindAndBothOriginalMirrorPickupDelaysSurviveTheActualCodec() {
        CompoundTag grate = row(); grate.putBoolean("GrateDrop", true);
        for (CompoundTag row : List.of(grate, mirror(0), mirror(10))) {
            var restored = DeferredItemDrops.load(envelope(5, row), registries);
            assertEquals(1, restored.pendingCount()); assertEquals(0, restored.quarantinedCount());
            assertEquals(row, restored.save(new CompoundTag(), registries).getList("Pending", Tag.TAG_COMPOUND).getCompound(0));
        }
    }

    @Test void malformedNewKindsRetainTheirOriginalRowsInsteadOfSilentlyChangingEntityType() {
        CompoundTag oldVersion = row(); oldVersion.putBoolean("GrateDrop", true);
        assertQuarantined(envelope(4, oldVersion), oldVersion);
        CompoundTag wrongKindType = row(); wrongKindType.putInt("GrateDrop", 1);
        CompoundTag wrongKindValue = row(); wrongKindValue.putBoolean("GrateDrop", false);
        CompoundTag changedPhysics = row(); changedPhysics.putBoolean("GrateDrop", true); changedPhysics.putDouble("DY", .1);
        CompoundTag conflictingKinds = mirror(0); conflictingKinds.putBoolean("GrateDrop", true);
        CompoundTag unrepresentable = row(); unrepresentable.putBoolean("GrateDrop", true); unrepresentable.putInt("Count", 100);
        for (CompoundTag row : List.of(wrongKindType, wrongKindValue, changedPhysics, conflictingKinds, unrepresentable))
            assertQuarantined(envelope(5, row), row);
    }

    private CompoundTag mirror(int pickupDelay) {
        CompoundTag row = row(); row.putInt("PortalCooldown", 20);
        if (pickupDelay > 0) row.putInt("PickupDelay", pickupDelay);
        return row;
    }
    private CompoundTag row() {
        var row = new CompoundTag(); row.putUUID("Id", UUID.randomUUID()); row.putLong("Source", SOURCE.asLong());
        row.put("Stack", new ItemStack(Items.DIAMOND).save(registries)); row.putInt("Count", 1);
        row.putDouble("X", SOURCE.getX() + .5); row.putDouble("Y", SOURCE.getY() + .6); row.putDouble("Z", SOURCE.getZ() + .5);
        row.putDouble("DX", 0); row.putDouble("DY", -.1); row.putDouble("DZ", 0);
        return row;
    }
    private static CompoundTag envelope(int version, CompoundTag row) {
        var input = new CompoundTag(); input.putInt("Version", version);
        var pending = new ListTag(); pending.add(row.copy()); input.put("Pending", pending);
        return input;
    }
    private void assertQuarantined(CompoundTag input, CompoundTag row) {
        var restored = DeferredItemDrops.load(input, registries);
        assertEquals(0, restored.pendingCount()); assertEquals(1, restored.quarantinedCount());
        assertEquals(row, restored.save(new CompoundTag(), registries).getList("Quarantined", Tag.TAG_COMPOUND).getCompound(0));
    }
}
