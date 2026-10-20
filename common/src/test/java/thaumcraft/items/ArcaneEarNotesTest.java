package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.ArcaneEarNotes;

/** The Arcane Ear only knew the five instruments of the original note block; every newer instrument is a different sound. */
class ArcaneEarNotesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final Map<NoteBlockInstrument, Integer> ORIGINAL = new EnumMap<>(Map.of(
            NoteBlockInstrument.HARP, 0, NoteBlockInstrument.BASEDRUM, 1, NoteBlockInstrument.SNARE, 2,
            NoteBlockInstrument.HAT, 3, NoteBlockInstrument.BASS, 4));

    @Test void theFiveOriginalInstrumentsKeepTheirOriginalNumbers() {
        for (var entry : ORIGINAL.entrySet()) assertEquals(entry.getValue(), ArcaneEarNotes.legacyInstrument(entry.getKey()), entry.getKey().name());
    }

    @Test void everyOtherInstrumentIsNotAnOriginalOneAndIsSkipped() {
        int others = 0;
        for (NoteBlockInstrument instrument : NoteBlockInstrument.values()) {
            if (ORIGINAL.containsKey(instrument)) continue;
            others++;
            assertEquals(-1, ArcaneEarNotes.legacyInstrument(instrument), instrument.name() + " must not alias an original instrument");
        }
        assertEquals(true, others > 10, "the game has far more instruments than the original five");
    }

    @Test void theNumbersMapBackToTheInstrumentsTheyCameFrom() {
        for (var entry : ORIGINAL.entrySet()) assertSame(entry.getKey(), ArcaneEarNotes.instrument(entry.getValue()), entry.getKey().name());
    }

    @Test void anUnknownNumberFallsBackToTheHarp() {
        for (int unknown : new int[] {-1, 5, 6, 100, Integer.MIN_VALUE, Integer.MAX_VALUE}) assertSame(NoteBlockInstrument.HARP, ArcaneEarNotes.instrument(unknown), "number " + unknown);
    }
}
