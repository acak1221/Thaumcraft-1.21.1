package thaumcraft.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.AspectOrbQueue;

/**
 * The saved form of the paid aspect-orb queue: what a load keeps pending, what it settles and what it sets aside as rejected.
 * Rows are only ever decoded, never delivered, so no world is needed.
 */
class AspectOrbQueueTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private static CompoundTag row(UUID id) {
        CompoundTag row = new CompoundTag();
        row.putUUID("Id", id);
        row.putString("Aspect", "aer");
        row.putInt("Value", 1);
        row.putDouble("X", 10.5);
        row.putDouble("Y", 64.5);
        row.putDouble("Z", -3.5);
        row.putDouble("MotionX", 0.1);
        row.putDouble("MotionY", 0.2);
        row.putDouble("MotionZ", -0.1);
        row.putFloat("Yaw", 90F);
        row.putLong("RetryAt", 7L);
        return row;
    }

    private static CompoundTag row(UUID id, Consumer<CompoundTag> change) {
        CompoundTag row = row(id);
        change.accept(row);
        return row;
    }

    private static CompoundTag queue(CompoundTag... pending) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        ListTag rows = new ListTag();
        rows.addAll(List.of(pending));
        tag.put("Pending", rows);
        return tag;
    }

    private static CompoundTag saved(CompoundTag loaded) { return AspectOrbQueue.load(loaded, null).save(new CompoundTag(), null); }

    private static ListTag list(CompoundTag tag, String name) { return tag.getList(name, Tag.TAG_COMPOUND); }

    @Test void aValidRowSurvivesALoadAndSaveUnchanged() {
        CompoundTag out = saved(queue(row(ID)));
        assertEquals(1, out.getInt("Version"));
        assertEquals(1, list(out, "Pending").size());
        assertEquals(row(ID), list(out, "Pending").getCompound(0));
        assertTrue(list(out, "Rejected").isEmpty());
        assertTrue(list(out, "Settled").isEmpty());
    }

    @Test void eachOfTheSixPrimalAspectsIsAccepted() {
        for (String aspect : List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio")) {
            CompoundTag out = saved(queue(row(ID, r -> r.putString("Aspect", aspect))));
            assertEquals(1, list(out, "Pending").size(), aspect);
            assertTrue(list(out, "Rejected").isEmpty(), aspect);
        }
    }

    @Test void aSavedWorldFromAnotherVersionIsKeptWholeAsRejectedAndDeliversNothing() {
        CompoundTag foreign = queue(row(ID));
        foreign.putInt("Version", 2);
        CompoundTag out = saved(foreign);
        assertTrue(list(out, "Pending").isEmpty());
        assertEquals(1, list(out, "Rejected").size());
        assertEquals(foreign, list(out, "Rejected").getCompound(0));
        CompoundTag unversioned = queue(row(ID));
        unversioned.remove("Version");
        assertEquals(1, list(saved(unversioned), "Rejected").size());
    }

    @Test void rowsThatBreakTheOriginalOrbRulesAreRejectedNotDelivered() {
        List<Consumer<CompoundTag>> breakers = List.of(
                r -> r.putString("Aspect", "lux"),               // not a primal aspect
                r -> r.putString("Aspect", "nonsense"),          // not an aspect at all
                r -> r.putInt("Value", 2),                       // the original emits single-vis orbs only
                r -> r.putDouble("X", 30000000D),                // outside the world limit
                r -> r.putDouble("Z", -30000000D),
                r -> r.putDouble("Y", 3.0E7),
                r -> r.putDouble("MotionX", 0.21),               // faster than the original scatter
                r -> r.putDouble("MotionZ", -0.21),
                r -> r.putDouble("MotionY", -0.01),              // orbs never start downward
                r -> r.putDouble("MotionY", 0.41),
                r -> r.putDouble("X", Double.NaN),
                r -> r.putDouble("MotionX", Double.POSITIVE_INFINITY),
                r -> r.putFloat("Yaw", Float.NaN));
        for (int index = 0; index < breakers.size(); index++) {
            CompoundTag out = saved(queue(row(ID, breakers.get(index))));
            assertTrue(list(out, "Pending").isEmpty(), "breaker " + index + " stayed pending");
            assertEquals(1, list(out, "Rejected").size(), "breaker " + index + " was not set aside");
        }
    }

    @Test void theLimitsOfTheOriginalScatterAreStillAccepted() {
        List<Consumer<CompoundTag>> edges = List.of(
                r -> r.putDouble("MotionY", 0D),
                r -> r.putDouble("MotionY", 0.4),
                r -> r.putDouble("MotionX", 0.2),
                r -> r.putDouble("MotionZ", -0.2),
                r -> r.putDouble("X", 29999999.5),
                r -> r.putDouble("Y", -64D));
        for (int index = 0; index < edges.size(); index++) {
            CompoundTag out = saved(queue(row(ID, edges.get(index))));
            assertEquals(1, list(out, "Pending").size(), "edge " + index + " was refused");
            assertTrue(list(out, "Rejected").isEmpty(), "edge " + index + " was set aside");
        }
    }

    @Test void aRowMissingAnyFieldIsRejected() {
        for (String field : List.of("Id", "Aspect", "Value", "X", "Y", "Z", "MotionX", "MotionY", "MotionZ", "Yaw", "RetryAt")) {
            CompoundTag broken = row(ID);
            broken.remove(field);
            if (field.equals("Id")) { broken.remove("IdMost"); broken.remove("IdLeast"); }
            CompoundTag out = saved(queue(broken));
            assertTrue(list(out, "Pending").isEmpty(), field + " missing but the row stayed pending");
            assertEquals(1, list(out, "Rejected").size(), field);
        }
    }

    @Test void aRowWithTheWrongTagTypeForAFieldIsRejected() {
        CompoundTag out = saved(queue(row(ID, r -> r.putInt("X", 10)), row(OTHER, r -> r.putString("Yaw", "90"))));
        assertTrue(list(out, "Pending").isEmpty());
        assertEquals(2, list(out, "Rejected").size());
    }

    @Test void twoRowsWithTheSameIdAreBothRejectedBecauseTheOwnerIsAmbiguous() {
        CompoundTag out = saved(queue(row(ID), row(ID, r -> r.putDouble("X", 99D)), row(OTHER)));
        assertEquals(1, list(out, "Pending").size());
        assertEquals(OTHER, list(out, "Pending").getCompound(0).getUUID("Id"));
        assertEquals(2, list(out, "Rejected").size());
    }

    @Test void aRowAlreadySettledIsNeitherDeliveredAgainNorRejected() {
        CompoundTag tag = queue(row(ID), row(OTHER));
        ListTag settled = new ListTag();
        CompoundTag receipt = new CompoundTag();
        receipt.putUUID("Id", ID);
        settled.add(receipt);
        tag.put("Settled", settled);
        CompoundTag out = saved(tag);
        assertEquals(1, list(out, "Pending").size());
        assertEquals(OTHER, list(out, "Pending").getCompound(0).getUUID("Id"));
        assertTrue(list(out, "Rejected").isEmpty());
        assertEquals(1, list(out, "Settled").size());
        assertEquals(ID, list(out, "Settled").getCompound(0).getUUID("Id"));
    }

    @Test void aSettledReceiptWithoutAnIdIsSetAsideNotSilentlyDropped() {
        CompoundTag tag = queue();
        ListTag settled = new ListTag();
        settled.add(new CompoundTag());
        tag.put("Settled", settled);
        CompoundTag out = saved(tag);
        assertTrue(list(out, "Settled").isEmpty());
        assertEquals(1, list(out, "Rejected").size());
    }

    @Test void aSectionOfTheWrongKindRejectsTheWholeTag() {
        CompoundTag notAList = queue(row(ID));
        notAList.put("Settled", new CompoundTag());
        CompoundTag out = saved(notAList);
        assertTrue(list(out, "Pending").isEmpty(), "nothing may be delivered from a tag that is not understood");
        assertEquals(1, list(out, "Rejected").size());
        CompoundTag wrongElements = queue(row(ID));
        ListTag numbers = new ListTag();
        numbers.add(IntTag.valueOf(1));
        wrongElements.put("Pending", numbers);
        out = saved(wrongElements);
        assertTrue(list(out, "Pending").isEmpty());
        assertEquals(1, list(out, "Rejected").size());
    }

    @Test void rowsRejectedBeforeAreKeptAndNewRejectionsAreAppended() {
        CompoundTag tag = queue(row(ID, r -> r.putInt("Value", 5)));
        ListTag earlier = new ListTag();
        CompoundTag old = row(OTHER);
        old.putString("Rejection", "Outside native build height");
        earlier.add(old);
        tag.put("Rejected", earlier);
        CompoundTag out = saved(tag);
        assertEquals(2, list(out, "Rejected").size());
        assertEquals("Outside native build height", list(out, "Rejected").getCompound(0).getString("Rejection"));
        assertFalse(list(out, "Rejected").getCompound(1).contains("Rejection"));
    }

    @Test void pendingRowsKeepTheirOrderThroughASaveAndLoad() {
        UUID third = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
        CompoundTag out = saved(queue(row(OTHER), row(third), row(ID)));
        ListTag rows = list(out, "Pending");
        assertEquals(List.of(OTHER, third, ID), List.of(rows.getCompound(0).getUUID("Id"), rows.getCompound(1).getUUID("Id"), rows.getCompound(2).getUUID("Id")));
        assertEquals(out, saved(out), "a second round trip must change nothing");
    }
}
