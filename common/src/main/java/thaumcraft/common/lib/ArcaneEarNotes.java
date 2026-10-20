package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/** Original NoteBlockEvent.Play queue: level-local, current tick only, consumed independently by every ear. */
public final class ArcaneEarNotes {
    private record Note(BlockPos pos, int instrument, int pitch) {}
    private static final Map<ServerLevel, List<Note>> NOTES = new WeakHashMap<>();
    private ArcaneEarNotes() {}

    /** The five original instrument ids; newer Minecraft instruments are distinct sounds, not aliases. */
    public static int legacyInstrument(NoteBlockInstrument instrument) {
        return switch (instrument) {
            case HARP -> 0;
            case BASEDRUM -> 1;
            case SNARE -> 2;
            case HAT -> 3;
            case BASS -> 4;
            default -> -1;
        };
    }
    public static NoteBlockInstrument instrument(int legacy) {
        return switch (legacy) {
            case 1 -> NoteBlockInstrument.BASEDRUM;
            case 2 -> NoteBlockInstrument.SNARE;
            case 3 -> NoteBlockInstrument.HAT;
            case 4 -> NoteBlockInstrument.BASS;
            default -> NoteBlockInstrument.HARP;
        };
    }
    public static void record(ServerLevel level, BlockPos pos, NoteBlockInstrument instrument, int pitch) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Note playback requires the server thread");
        int legacy = legacyInstrument(instrument);
        if (legacy < 0 || pitch < 0 || pitch > 24) return;
        // No gameTime equality gate: input can arrive just before ServerLevel.tick increments that clock.
        // The original lifetime ends at the world POST tick, not at the increment at its beginning.
        NOTES.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Note(pos.immutable(), legacy, pitch));
    }
    public static boolean matches(ServerLevel level, BlockPos ear, int instrument, int pitch) {
        List<Note> notes = NOTES.get(level);
        if (notes == null) return false;
        for (Note note : notes) {
            // The original compares block centres. Their .5 offsets cancel on all three axes.
            if (note.instrument() == instrument && note.pitch() == pitch && ear.distSqr(note.pos()) <= 4096.0) return true;
        }
        return false;
    }
    public static void clear(ServerLevel level) { NOTES.remove(level); }
}
