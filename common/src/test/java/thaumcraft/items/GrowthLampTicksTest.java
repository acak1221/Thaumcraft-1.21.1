package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.GrowthLampTicks;

/** The saved one-tick plant updates a growth lamp leaves behind: valid rows survive, anything unclear is set aside, never grown. */
class GrowthLampTicksTest {
    private static final long POS = BlockPos.asLong(12, 64, -7);

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static CompoundTag row(long pos, String block, long due) {
        CompoundTag row = new CompoundTag();
        row.putLong("Pos", pos);
        row.putString("Block", block);
        row.putLong("Due", due);
        return row;
    }

    private static CompoundTag tag(CompoundTag... pending) {
        CompoundTag tag = new CompoundTag();
        ListTag rows = new ListTag();
        for (CompoundTag row : pending) rows.add(row);
        tag.put("Pending", rows);
        return tag;
    }

    private static CompoundTag roundTrip(CompoundTag tag) { return GrowthLampTicks.load(tag, null).save(new CompoundTag(), null); }

    private static ListTag list(CompoundTag tag, String name) { return tag.getList(name, Tag.TAG_COMPOUND); }

    @Test void aValidRowSurvivesALoadAndSaveAndLeavesNoRejectedList() {
        CompoundTag out = roundTrip(tag(row(POS, "minecraft:wheat", 1234L)));
        assertEquals(1, list(out, "Pending").size());
        assertEquals(row(POS, "minecraft:wheat", 1234L), list(out, "Pending").getCompound(0));
        assertFalse(out.contains("Rejected"), "an empty rejected list is not written");
    }

    @Test void anEmptyQueueSavesAnEmptyPendingList() {
        CompoundTag out = roundTrip(new CompoundTag());
        assertTrue(out.contains("Pending"));
        assertTrue(list(out, "Pending").isEmpty());
        assertFalse(out.contains("Rejected"));
    }

    @Test void malformedCollectionsRemainRecoverableAcrossRepeatedSaves() {
        for (String field : new String[]{"Pending", "Rejected"}) {
            CompoundTag malformed = tag(row(POS, "minecraft:wheat", 5L));
            ListTag strings = new ListTag();
            strings.add(net.minecraft.nbt.StringTag.valueOf("unreadable scheduled update"));
            malformed.put(field, strings);
            CompoundTag out = roundTrip(malformed);
            assertTrue(list(out, "Pending").isEmpty());
            assertEquals(malformed, list(out, "Rejected").getCompound(0));
            assertEquals(out, roundTrip(out));
            malformed = tag(row(POS, "minecraft:wheat", 5L));
            malformed.putInt(field, 99);
            out = roundTrip(malformed);
            assertEquals(malformed, list(out, "Rejected").getCompound(0));
            assertEquals(out, roundTrip(out));
        }
    }

    @Test void rowsNamingNoKnownBlockAreSetAside() {
        CompoundTag out = roundTrip(tag(
                row(POS, "thaumcraft:no_such_crop", 5L),   // unknown block
                row(POS, "minecraft:air", 5L),             // air is never a plant
                row(POS, "not a resource location!", 5L)));
        assertTrue(list(out, "Pending").isEmpty());
        assertEquals(3, list(out, "Rejected").size());
    }

    @Test void rowsMissingAFieldOrWithTheWrongTagTypeAreSetAside() {
        CompoundTag noPos = row(POS, "minecraft:wheat", 5L);
        noPos.remove("Pos");
        CompoundTag noDue = row(POS, "minecraft:wheat", 5L);
        noDue.remove("Due");
        CompoundTag noBlock = row(POS, "minecraft:wheat", 5L);
        noBlock.remove("Block");
        CompoundTag intPos = row(POS, "minecraft:wheat", 5L);
        intPos.putInt("Pos", 7);
        CompoundTag out = roundTrip(tag(noPos, noDue, noBlock, intPos));
        assertTrue(list(out, "Pending").isEmpty());
        assertEquals(4, list(out, "Rejected").size());
    }

    @Test void theSamePositionAndBlockTwiceKeepsTheFirstAndSetsTheSecondAside() {
        CompoundTag out = roundTrip(tag(row(POS, "minecraft:wheat", 10L), row(POS, "minecraft:wheat", 99L)));
        assertEquals(1, list(out, "Pending").size());
        assertEquals(10L, list(out, "Pending").getCompound(0).getLong("Due"), "the earlier due time wins, as the native scheduler deduplicates");
        assertEquals(1, list(out, "Rejected").size());
        assertEquals(99L, list(out, "Rejected").getCompound(0).getLong("Due"));
    }

    @Test void theSamePositionWithADifferentBlockIsAnotherUpdate() {
        CompoundTag out = roundTrip(tag(row(POS, "minecraft:wheat", 10L), row(POS, "minecraft:carrots", 10L), row(POS + 1, "minecraft:wheat", 10L)));
        assertEquals(3, list(out, "Pending").size());
        assertFalse(out.contains("Rejected"));
    }

    @Test void rowsRejectedByAnEarlierLoadAreKeptAndNewOnesAreAppended() {
        CompoundTag earlier = row(POS, "thaumcraft:gone", 3L);
        CompoundTag tag = tag(row(POS, "minecraft:air", 4L));
        ListTag kept = new ListTag();
        kept.add(earlier);
        tag.put("Rejected", kept);
        CompoundTag out = roundTrip(tag);
        assertEquals(2, list(out, "Rejected").size());
        assertEquals(earlier, list(out, "Rejected").getCompound(0));
        assertEquals("minecraft:air", list(out, "Rejected").getCompound(1).getString("Block"));
    }

    @Test void aDueTimeInThePastOrNegativeIsStillKeptForTheNextTick() {
        CompoundTag out = roundTrip(tag(row(POS, "minecraft:wheat", -5L), row(POS + 2, "minecraft:wheat", 0L)));
        assertEquals(2, list(out, "Pending").size());
        assertEquals(-5L, list(out, "Pending").getCompound(0).getLong("Due"));
    }

    @Test void thePositionIsPackedTheWayBlockPosPacksIt() {
        CompoundTag out = roundTrip(tag(row(POS, "minecraft:wheat", 1L)));
        assertEquals(new BlockPos(12, 64, -7), BlockPos.of(list(out, "Pending").getCompound(0).getLong("Pos")));
    }
}
