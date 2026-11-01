package thaumcraft.api.machines;

/**
 * The Arcane Ear (block {@code thaumcraft:arcane_ear}, the original {@code TileSensor}): the note it listens for.
 *
 * <p>The ear gives a ten-tick redstone pulse when a note block within 64 blocks plays its note on its instrument (the
 * instrument comes from the block under the ear and is not part of this interface). A right click raises the note by one
 * and wraps from 24 to 0. The original peripheral {@code tt_arcaneear} read and wrote the public field {@code note}.</p>
 */
public interface IArcaneEar {
    /** The lowest note, F#3 (the note block's 0). */
    int MIN_NOTE = 0;
    /** The highest note, F#5 (the note block's 24). */
    int MAX_NOTE = 24;

    /**
     * The note the ear listens for, {@value #MIN_NOTE} to {@value #MAX_NOTE}, in note block clicks. Saved with the block
     * (key {@code note}) and synced to clients. Either side.
     *
     * @return the note
     */
    int getNote();

    /**
     * Tunes the ear to a note, with the effects of tuning it by hand: the note is saved with the block and sent to the
     * clients that see it. Unlike the right click it does not play the note (neither did the original peripheral).
     * Setting the note the ear already has changes nothing.
     *
     * <p>Server side only, on the server thread: on a client copy, and on an ear that is not placed in a level, it does
     * nothing.</p>
     *
     * @param note the new note, {@value #MIN_NOTE} to {@value #MAX_NOTE}
     * @throws IllegalArgumentException for a note outside 0..24 (the original peripheral stored any byte; the ear never
     *                                  hears such a note, and a save of the port reads it back as the nearest of 0..24)
     * @throws IllegalStateException    when the ear is in a server level and this is not the server thread
     */
    void setNote(int note);
}
