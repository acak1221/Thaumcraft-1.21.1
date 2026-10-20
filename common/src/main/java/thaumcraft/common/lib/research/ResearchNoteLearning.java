package thaumcraft.common.lib.research;

import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;

/** Original solved-note learning; solving a grid and learning its research are distinct. */
public final class ResearchNoteLearning {
    public enum Failure { NONE, UNFINISHED, UNAVAILABLE, ALREADY_COMPLETE, MISSING_PREREQUISITES }
    public record Result(Failure failure, Set<String> unlocked) {
        public Result {
            Objects.requireNonNull(failure, "failure");
            unlocked = Collections.unmodifiableSet(new LinkedHashSet<>(unlocked));
        }
        public boolean success() { return failure == Failure.NONE; }
    }
    private ResearchNoteLearning() {}

    /** The caller consumes one note only after a successful authoritative result. */
    public static Result learn(PlayerKnowledge knowledge, ResearchNoteData note) {
        Objects.requireNonNull(knowledge, "knowledge");
        Objects.requireNonNull(note, "note");
        if (!note.complete()) return rejected(Failure.UNFINISHED);
        var definition = ResearchCatalog.get(note.key());
        if (definition == null || definition.isVirtual() || definition.isAutoUnlock()
                || !definition.isEnabled()) return rejected(Failure.UNAVAILABLE);
        if (knowledge.isResearchComplete(note.key())) return rejected(Failure.ALREADY_COMPLETE);
        if (!definition.hasPrerequisites(knowledge.getResearchComplete())) return rejected(Failure.MISSING_PREREQUISITES);
        // ItemResearchNotes:87-104 does not charge RP, require its clue or check
        // the reader's discovered aspects/difficulty. Solved notes are transferable.
        var next = knowledge.copy();
        var unlocked = ResearchCompletion.apply(next, definition, ResearchCatalog::get);
        var result = new Result(Failure.NONE, unlocked);
        knowledge.copyFrom(next);
        return result;
    }

    private static Result rejected(Failure failure) { return new Result(failure, Set.of()); }
}
