package thaumcraft.common.lib.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original TileResearchTable.duplicate: every base aspect cost plus the OLD copies counter; no scribing ink or table bonus. */
public final class ResearchNoteCopying {
    private ResearchNoteCopying() {}
    public record Plan(PlayerKnowledge knowledge, ResearchNoteData note, int count, Map<String, Integer> costs) {
        public Plan { costs = Collections.unmodifiableMap(new LinkedHashMap<>(costs)); }
    }
    /** Read-only GUI/admission query; never clone the player's complete history every frame. */
    public static boolean canPay(PlayerKnowledge actor, ResearchNoteData note, int count) {
        if (actor == null || note == null || !note.complete() || !actor.isResearchComplete("RESEARCHDUPE")
                || count <= 0 || count == Integer.MAX_VALUE || note.copies() == Integer.MAX_VALUE) return false;
        var research = ResearchCatalog.get(note.key());
        if (research == null || !research.isEnabled()) return false;
        for (var entry : research.aspectCost().entrySet()) {
            long price = (long) entry.getValue() + note.copies();
            int raw = actor.getAspectPoolFor(entry.getKey());
            if (price <= 0 || price > Integer.MAX_VALUE || raw <= 0 || ResearchNoteEditing.queryPool(raw) < price) return false;
        }
        return true;
    }
    /** Stages private next-state only. The caller separately admits and pays paper+black dye from main inventory. */
    public static Plan plan(PlayerKnowledge actor, ResearchNoteData note, int count) {
        if (!canPay(actor, note, count)) return null;
        var staged = actor.copy();
        Map<String, Integer> costs = new LinkedHashMap<>();
        for (var entry : ResearchCatalog.get(note.key()).aspectCost().entrySet()) {
            int cost = Math.toIntExact((long) entry.getValue() + note.copies());
            if (!staged.addAspectPool(entry.getKey(), -cost)) return null;
            costs.put(entry.getKey(), cost);
        }
        var after = new ResearchNoteData(note.key(), note.color(), true, note.copies() + 1, note.cells());
        return new Plan(staged, after, count + 1, costs);
    }
}
