package thaumcraft.tiles;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrucibleActionSaveIntegrityTest {
    // Exercise the actual private SavedData reader, not a source-text facsimile.
    private static CompoundTag roundTrip(CompoundTag tag) throws Exception {
        var type = Class.forName("thaumcraft.common.tiles.TileCrucible$CrucibleActions");
        var load = type.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        return ((SavedData) load.invoke(null, tag, null)).save(new CompoundTag(), null);
    }

    @Test void malformedCollectionsRetainWholeOwnerAcrossRepeatedSaves() throws Exception {
        for (String field : new String[]{"Outputs", "Spills", "Quarantined"}) {
            CompoundTag malformed = new CompoundTag();
            malformed.putInt("Version", 1);
            ListTag strings = new ListTag();
            strings.add(StringTag.valueOf("unreadable paid result"));
            malformed.put(field, strings);
            assertRetained(malformed);
            malformed.putInt(field, 99);
            assertRetained(malformed);
        }
    }

    private static void assertRetained(CompoundTag malformed) throws Exception {
        CompoundTag saved = roundTrip(malformed);
        assertEquals(malformed, saved.getList("Quarantined", Tag.TAG_COMPOUND).getCompound(0));
        assertTrue(saved.getList("Outputs", Tag.TAG_COMPOUND).isEmpty());
        assertTrue(saved.getList("Spills", Tag.TAG_COMPOUND).isEmpty());
        assertEquals(saved, roundTrip(saved));
    }

    @Test void emptyOwnerSurvivesRoundTrip() throws Exception {
        CompoundTag empty = new CompoundTag();
        empty.putInt("Version", 1);
        CompoundTag saved = roundTrip(empty);
        assertEquals(saved, roundTrip(saved));
        assertTrue(saved.getList("Quarantined", Tag.TAG_COMPOUND).isEmpty());
    }
}
