package thaumcraft.common.testing;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** Malformed SAVED INPUT fixtures only. No real machine success/payment/loot is
 * manufactured; the whole purpose is to prove these rows NEVER become output. */
public final class BoreJournalDowngradeRuntimeChecks {
    private BoreJournalDowngradeRuntimeChecks() {}

    public static void reservedUuidCannotBecomeLegacyOutput(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            for (String phase : List.of("ARMED", "UNKNOWN", "WITNESSED", "MALFORMED_JOURNAL")) {
                CompoundTag saved = input(f, phase);
                DeferredItemDrops queue = DeferredItemDrops.load(saved, f.level.registryAccess());
                f.check(queue.pendingCount() == 0 && queue.quarantinedCount() >= 1,
                        "a UUID reserved even by an inert/malformed Bore journal cannot downgrade by losing metadata: " + phase);
                f.check(queue.boreJournalCount() == (phase.equals("MALFORMED_JOURNAL") ? 0 : 1),
                        "known journal retained or explicitly quarantined, not silently discarded: " + phase);
                if (phase.equals("ARMED") || phase.equals("UNKNOWN"))
                    f.check(queue.boreUnknownCount() == 1, "unwitnessed saved owner is UNKNOWN, never success inferred from air");
                queue.flush(f.level); // Deliberately detached: generic downgrade used to bypass the Bore guard.
                f.check(f.drops().isEmpty() && queue.pendingCount() == 0,
                        "malformed reserved row emits no physical stock from a detached candidate queue: " + phase);
            }
            // Ordinary unrelated V1/V2/V3 inputs keep their historical reader.
            // Do not flush them or mislabel fixture rows as paid machine outputs.
            for (int version : new int[]{1, 2, 3}) {
                var saved = input(f, "UNKNOWN"); saved.putInt("Version", version); saved.remove("BoreJournals");
                var legacy = DeferredItemDrops.load(saved, f.level.registryAccess());
                f.check(legacy.pendingCount() == 1 && legacy.quarantinedCount() == 0,
                        "unrelated ordinary legacy reader is unchanged for version " + version);
            }
        }
        test.succeed();
    }

    private static CompoundTag input(Fixture f, String phase) {
        UUID transaction = UUID.randomUUID(), output = UUID.randomUUID();
        var source = f.base; var target = f.base.above(3); var stack = new ItemStack(Items.COBBLESTONE);
        CompoundTag row = new CompoundTag(); row.putUUID("Id", output); row.putLong("Source", source.asLong());
        row.putInt("Count", 1); row.put("Stack", stack.save(f.level.registryAccess()));
        row.putDouble("X", source.getX() + .5 + .66); row.putDouble("Y", source.getY() + .4 - 1); row.putDouble("Z", source.getZ() + .5);
        row.putDouble("DX", .075F); row.putDouble("DY", .025F); row.putDouble("DZ", 0);
        CompoundTag metadata = new CompoundTag(); metadata.putUUID("Transaction", transaction); metadata.putLong("Target", target.asLong());
        metadata.putInt("Nozzle", Direction.EAST.get3DDataValue()); metadata.putInt("Attachment", Direction.UP.get3DDataValue());
        metadata.putBoolean("Evacuation", false); metadata.putInt("Member", 0); metadata.putInt("Segment", 0); row.put("Bore", metadata);
        CompoundTag journal = new CompoundTag(); journal.putInt("BoreJournalVersion", phase.equals("MALFORMED_JOURNAL") ? 99 : 1);
        journal.putUUID("Id", transaction); journal.putUUID("Machine", UUID.randomUUID()); journal.putUUID("Lifetime", UUID.randomUUID());
        journal.putLong("Source", source.asLong()); journal.putLong("Revision", 0); journal.putLong("Sequence", 1); journal.putInt("CreditBits", 0);
        String actualPhase = phase.equals("MALFORMED_JOURNAL") ? "UNKNOWN" : phase;
        journal.putString("Phase", actualPhase); journal.putBoolean("Fenced", !actualPhase.equals("WITNESSED")); journal.putString("Mode", "HARVEST");
        journal.putLong("Target", target.asLong()); journal.put("OldState", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        journal.putInt("Nozzle", Direction.EAST.get3DDataValue()); journal.putInt("Attachment", Direction.UP.get3DDataValue());
        var pick = new ItemStack(Items.WOODEN_PICKAXE); journal.put("PickBefore", pick.save(f.level.registryAccess()));
        pick.setDamageValue(1); journal.put("PickAfter", pick.save(f.level.registryAccess()));
        ListTag candidates = new ListTag(); candidates.add(row.copy()); journal.put("Rows", candidates);
        CompoundTag member = new CompoundTag(); member.putInt("Member", 0); member.putInt("Count", 1); member.put("Stack", stack.save(f.level.registryAccess()));
        ListTag originals = new ListTag(); originals.add(member); journal.put("OriginalBundle", originals);
        CompoundTag saved = new CompoundTag(); saved.putInt("Version", 3);
        ListTag journals = new ListTag(); journals.add(journal); saved.put("BoreJournals", journals);
        row.remove("Bore"); ListTag pending = new ListTag(); pending.add(row); saved.put("Pending", pending);
        return saved;
    }
}
