package thaumcraft.common.lib.research;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable TC4 table-edit planning, not an inventory/world transaction.
 *
 * Original anchors: TileResearchTable:141-183, PlayerKnowledge:95-122 and
 * AspectList:125-159 (TC4 4.2.3.5). Admission follows the normal original GUI:
 * existing blank/placed cells, discovered/funded placement and unfinished notes.
 * This intentionally rejects the original packet's overwrite/hole/free-funding
 * exploits before drawing randomness. Ownership or research prerequisites are
 * not added. Native menu/session/revision checks remain the caller's job.
 */
public final class ResearchNoteEditing {
    private ResearchNoteEditing() {}

    public enum Action { PLACE, ERASE }

    public enum Failure {
        NONE, MISSING_NOTE, DISCOVERY_PRESENTATION, SOLVED, MISSING_INK,
        INVALID_ACTION, INVALID_COORDINATE, MISSING_CELL, FIXED_CELL,
        NOT_BLANK, NOT_PLACED, UNKNOWN_ASPECT, UNDISCOVERED_ASPECT, INSUFFICIENT_ASPECTS
    }

    /** ACTOR_POOL identifies the original debit branch, even when its debit is a no-op. */
    public enum Funding { NONE, ACTOR_POOL, TABLE_BONUS, RESEARCHER_FREE }

    /** Bind directly to the admitted actor/table's shared world RNG, not a separately seeded RNG. */
    @FunctionalInterface public interface FloatSource { float nextFloat(); }

    /**
     * Pool presence is discovery, including zero and negative values. Actor values
     * are live raw ints, NOT short query/wire/persistence totals. The native caller
     * supplies initialized knowledge; this planner does not initialize primal RP.
     * Null note represents a missing component. Ink marker/usability is independent
     * of damageability, creative mode and enchantments on the physical stack.
     */
    public record State(ResearchNoteData note, boolean discoveryPresentation, ScribingInk.State ink,
                        Map<String, Integer> actorPools, Set<String> completedResearch,
                        Map<String, Integer> bonusPools) {
        public State {
            Objects.requireNonNull(ink, "ink");
            actorPools = snapshot(actorPools);
            completedResearch = Collections.unmodifiableSet(new LinkedHashSet<>(
                    Objects.requireNonNull(completedResearch, "completedResearch")));
            bonusPools = snapshot(bonusPools);
        }
    }

    /** Only PLACE carries an aspect. ERASE always takes the aspect from the current cell. */
    public record Edit(Action action, int q, int r, String aspect) {}

    /**
     * One original addAspectPool invocation. Non-null means the knowledge-save and
     * amount-zero pool-correction branch, even if applied=false for a negative raw
     * debit. queriedTotal models its signed-short total, NOT the new raw storage.
     * A refund is not a scan reward/toast and has no scanning soft cap.
     */
    public record PoolChange(String aspect, short requestedAmount, int before, int after,
                             boolean previouslyDiscovered, boolean applied) {
        public short queriedTotal() { return queryPool(after); }
        public boolean discoversAspect() { return applied && requestedAmount > 0 && !previouslyDiscovered; }
    }

    /**
     * Proposed final snapshots and effects only. No material commit, saving,
     * synchronization or sound is performed. On refusal after is the same State
     * instance, ink is unchanged and every effect is absent.
     *
     * The caller must commit note/presentation, literal ink, actor pool and bonus
     * together after native revalidation, preserving stack count/components and
     * all unrelated knowledge. Emit orb (volume .2) and completion effects only
     * after that commit. Do not use queriedTotal to replace a live raw pool.
     */
    public record Result(Failure failure, State after, Funding funding, PoolChange poolChange,
                         ScribingInk.Plan ink, Float orbPitch, boolean completesNote) {
        public boolean success() { return failure == Failure.NONE; }
        public boolean freePlacement() { return funding == Funding.RESEARCHER_FREE; }
        public boolean refunded() { return poolChange != null && poolChange.requestedAmount() > 0; }
    }

    /** Original PlayerKnowledge.getAspectPoolFor: the cast affects the query, not live storage. */
    public static short queryPool(int raw) { return (short) raw; }

    /**
     * Plans one admitted edit without changing input state. Rejected edits draw
     * nothing. Accepted erase always draws once, even without either Researcher.
     * FloatSource must return nextFloat values in [0,1); an invalid adapter throws
     * without publishing state, but already consumed RNG is never rolled back.
     */
    public static Result evaluate(State state, Edit edit, FloatSource random) {
        Objects.requireNonNull(state, "state");
        Failure failure = validate(state, edit);
        if (failure != Failure.NONE) return refused(state, failure);

        Cell target = state.note().cellAt(edit.q(), edit.r());
        String aspect = edit.action() == Action.PLACE ? edit.aspect() : target.aspect();
        boolean r1 = state.completedResearch().contains("RESEARCHER1");
        boolean r2 = state.completedResearch().contains("RESEARCHER2");
        Map<String, Integer> actor = new LinkedHashMap<>(state.actorPools());
        Map<String, Integer> bonus = new LinkedHashMap<>(state.bonusPools());
        Funding funding = Funding.NONE;
        PoolChange poolChange = null;
        Float pitch = null;
        Cell replacement;

        // TileResearchTable:153-174. Keep decision and optional pitch draws in
        // this order; the player world and table world are the same after admission.
        if (edit.action() == Action.PLACE) {
            if (r2 && nextFloat(random) < 0.1F) {
                funding = Funding.RESEARCHER_FREE;
                pitch = orbPitch(random);
            } else if (queryPool(actor.get(aspect)) > 0) {
                funding = Funding.ACTOR_POOL;
                poolChange = changePool(actor, aspect, (short) -1);
            } else {
                funding = Funding.TABLE_BONUS;
                int remaining = bonus.get(aspect) - 1;
                // Original AspectList.remove deletes a last bonus key; actor
                // AspectList.reduce instead keeps discovery at zero.
                if (remaining <= 0) bonus.remove(aspect);
                else bonus.put(aspect, remaining);
            }
            replacement = new Cell(target.q(), target.r(), 2, aspect);
        } else {
            float roll = nextFloat(random);
            if ((r1 && roll < 0.25F) || (r2 && roll < 0.5F)) {
                pitch = orbPitch(random);
                poolChange = changePool(actor, aspect, (short) 1);
            }
            replacement = new Cell(target.q(), target.r(), 0, null);
        }

        ArrayList<Cell> cells = new ArrayList<>(state.note().cells());
        cells.set(cells.indexOf(target), replacement);
        ResearchNoteData candidate = state.note().withCells(cells, false);
        ScribingInk.Plan ink = ScribingInk.planTable(state.ink());
        // TileResearchTable:170 precedes :180: an unknown erase-refund aspect
        // must already be discovered when testing the remaining connections.
        ResearchNoteData note = ResearchNotePuzzle.solve(candidate, actor.keySet());
        ScribingInk.State nextInk = new ScribingInk.State(state.ink().scribeTools(), ink.afterDamage(),
                state.ink().maximum(), state.ink().damageable());
        State after = new State(note, note.complete(), nextInk, actor, state.completedResearch(), bonus);
        return new Result(Failure.NONE, after, funding, poolChange, ink, pitch, note.complete());
    }

    private static Failure validate(State state, Edit edit) {
        if (state.note() == null) return Failure.MISSING_NOTE;
        if (state.discoveryPresentation()) return Failure.DISCOVERY_PRESENTATION;
        if (state.note().complete()) return Failure.SOLVED;
        if (!state.ink().usable()) return Failure.MISSING_INK;
        if (edit == null || edit.action() == null
                || (edit.action() == Action.ERASE && edit.aspect() != null)) return Failure.INVALID_ACTION;
        int radius = ResearchNoteData.MAX_RADIUS;
        if (edit.q() < -radius || edit.q() > radius || edit.r() < -radius || edit.r() > radius
                || Math.abs((long) edit.q() + edit.r()) > radius) return Failure.INVALID_COORDINATE;
        Cell target = state.note().cellAt(edit.q(), edit.r());
        if (target == null) return Failure.MISSING_CELL;
        if (target.type() == 1) return Failure.FIXED_CELL;
        if (edit.action() == Action.ERASE) return target.type() == 2 ? Failure.NONE : Failure.NOT_PLACED;
        if (target.type() != 0) return Failure.NOT_BLANK;
        String tag = edit.aspect();
        if (tag == null || tag.isEmpty() || tag.length() > ResearchNoteData.MAX_ASPECT_LENGTH) return Failure.UNKNOWN_ASPECT;
        Aspect aspect = Aspect.getAspect(tag);
        if (aspect == null || !tag.equals(aspect.getTag())) return Failure.UNKNOWN_ASPECT;
        if (!state.actorPools().containsKey(tag)) return Failure.UNDISCOVERED_ASPECT;
        // Original GUI sees short-synchronized actor pools. Funding is checked
        // BEFORE the free-placement roll, closing the old server's unfunded path.
        if (queryPool(state.actorPools().get(tag)) <= 0 && state.bonusPools().getOrDefault(tag, 0) <= 0) {
            return Failure.INSUFFICIENT_ASPECTS;
        }
        return Failure.NONE;
    }

    private static PoolChange changePool(Map<String, Integer> actor, String tag, short amount) {
        boolean known = actor.containsKey(tag);
        int before = actor.getOrDefault(tag, 0);
        // Original addAspectPool tests raw >0 for a debit, independently of its
        // signed-short query. TileResearchTable ignores the false debit return.
        boolean applied = amount > 0 || before > 0;
        // Deliberate Java int arithmetic: AspectList.add uses iadd, including
        // MAX_VALUE -> MIN_VALUE on refund. No scan cap, clamp or overflow refusal.
        int after = applied ? before + amount : before;
        if (applied) actor.put(tag, after);
        return new PoolChange(tag, amount, before, after, known, applied);
    }

    private static Result refused(State state, Failure failure) {
        int damage = state.ink().damage();
        return new Result(failure, state, Funding.NONE, null,
                new ScribingInk.Plan(false, damage, damage), null, false);
    }

    private static float orbPitch(FloatSource random) { return 0.9F + nextFloat(random) * 0.2F; }

    private static float nextFloat(FloatSource source) {
        float value = Objects.requireNonNull(source, "random").nextFloat();
        if (!(value >= 0.0F && value < 1.0F)) throw new IllegalArgumentException("RNG result outside [0,1)");
        return value;
    }

    private static <K, V> Map<K, V> snapshot(Map<K, V> source) {
        LinkedHashMap<K, V> copy = new LinkedHashMap<>();
        Objects.requireNonNull(source, "map").forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, "map key"), Objects.requireNonNull(value, "map value")));
        return Collections.unmodifiableMap(copy);
    }
}
