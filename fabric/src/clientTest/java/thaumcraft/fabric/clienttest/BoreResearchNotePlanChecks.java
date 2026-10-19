package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteGenerator;

/** Detached algorithm checks, NOT live research acquisition. Synthetic seeds and
 * positive arithmetic budgets exist only in this process; no world/player,
 * issued item, packet, note edit, ink, RP or production completion is created. */
public final class BoreResearchNotePlanChecks {
    private BoreResearchNotePlanChecks() {}
    private static final List<String> TAGS = List.of("aer", "ordo", "terra", "humanus", "motus", "perfodio", "machina", "instrumentum");
    private static final long[] SEEDS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            42, 127, 256, 512, 65535, 0x5eed5eedL, 0x123456789abcdefL, 123456789, 987654321,
            -1, -2, -42, -65536, Long.MIN_VALUE, Long.MAX_VALUE, 0x4000000000000000L};
    public static void main(String[] args) throws Exception {
        require(args.length == 1, "One explicit owned result path is required");
        Path output = Path.of(args[0]);
        require(!Files.exists(output), "Never overwrite an earlier detached result");
        ResearchCatalog.loadBundled();
        var known = new LinkedHashSet<>(TAGS);
        var funds = new LinkedHashMap<String, Integer>(); TAGS.forEach(tag -> funds.put(tag, 29));
        var result = new LinkedHashMap<String, Object>();
        var rows = new ArrayList<Map<String, Object>>();
        var finiteRows = new ArrayList<Map<String, Object>>();
        result.put("status", "RUNNING_DETACHED_ONLY");
        result.put("scope", "Synthetic algorithm inputs only; no actual Player653/world/issued note, native GUI, acquisition, RP or ink evidence");
        result.put("cases", rows); write(output, result);
        result.put("finite_envelope_cases", finiteRows);
        long started = System.nanoTime();
        try {
            for (long seed : SEEDS) {
                ResearchNoteData note = ResearchNoteGenerator.generate("ARCANEBORE", new Random(seed));
                String before = BoreResearchNotePlan.fingerprint(note);
                var plan = BoreResearchNotePlan.plan(note, known, funds);
                var row = new LinkedHashMap<String, Object>();
                row.put("synthetic_seed", seed); row.put("status", plan.status().name());
                row.put("reason", plan.reason()); row.put("placements", plan.placementActions());
                row.put("states", plan.stats().statesUsed()); row.put("rp_costs", plan.rpCosts()); rows.add(row);
                write(output, result); // Keep truthful diagnostics even if a bound is too tight.
                require(plan.ready(), "Detached seed " + seed + " did not yield a bounded route: " + plan.status() + " / " + plan.reason());
                require(plan.validateAgainst(note, known, funds).valid(), "Independent replay did not validate the route");
                require(plan.placementActions() > 0 && plan.placementActions() <= 29
                        && plan.validation().placementsRetainedByPureSolver() == plan.placementActions(), "All planned paid cells must remain connected");
                require(plan.rpCosts().values().stream().mapToInt(Integer::intValue).sum() == plan.placementActions(), "Whole per-tag unit cost is conserved");
                require(plan.rpCosts().entrySet().stream().allMatch(entry -> entry.getValue() > 0 && entry.getValue() <= funds.get(entry.getKey())), "No unprovided synthetic funding");
                require(plan.stats().statesUsed() <= BoreResearchNotePlan.HARD_STATE_LIMIT, "Declared CPU-state cap exceeded");
                require(!note.complete() && before.equals(BoreResearchNotePlan.fingerprint(note)) && plan.issuedNote().equals(note), "Detached planning changed its observed input");
                // Predicted finite acquisition envelope, still ONLY a detached
                // arithmetic fixture: the real player's scans/combinations and
                // resulting RP are not supplied or claimed by this process.
                var finite = Map.of("aer", 0, "terra", 20, "ordo", 14, "humanus", 1,
                        "instrumentum", 7, "motus", 7, "perfodio", 3, "machina", 3);
                var limited = BoreResearchNotePlan.plan(note, known, finite);
                var finiteRow = new LinkedHashMap<String, Object>(); finiteRow.put("synthetic_seed", seed);
                finiteRow.put("status", limited.status().name()); finiteRow.put("reason", limited.reason());
                finiteRow.put("placements", limited.placementActions()); finiteRow.put("states", limited.stats().statesUsed());
                finiteRow.put("rp_costs", limited.rpCosts()); finiteRows.add(finiteRow); write(output, result);
                require(limited.ready() && limited.validateAgainst(note, known, finite).valid(),
                        "Detached finite envelope seed " + seed + " has no admitted bounded route: " + limited.status());
                if (seed == 0) boundaryCases(note, plan, known, funds, result);
            }
            result.put("status", "PASSED_DETACHED_ALGORITHM_ONLY");
            result.put("synthetic_boards_passed", SEEDS.length);
            result.put("finite_envelope_boards_passed", SEEDS.length);
            result.put("boundary_assertions_passed", 8);
            result.put("seconds", (System.nanoTime() - started) / 1_000_000_000.0);
            write(output, result);
            System.out.println("Detached Bore planner:" + SEEDS.length + " synthetic boards in2 budget envelopes +8 boundary assertions PASS; no live/client/research acquisition claim");
        } catch (RuntimeException | Error failure) {
            result.put("status", "FAILED_DETACHED_CHECK"); result.put("failure", failure.toString()); write(output, result); throw failure;
        }
    }

    private static void boundaryCases(ResearchNoteData note, BoreResearchNotePlan.Plan plan,
                                      Set<String> known, Map<String, Integer> funds, Map<String, Object> result) {
        var reversedTags = new ArrayList<>(TAGS); Collections.reverse(reversedTags);
        var reversedSet = new LinkedHashSet<>(reversedTags); var reversedFunds = new LinkedHashMap<String, Integer>();
        reversedTags.forEach(tag -> reversedFunds.put(tag, funds.get(tag)));
        var again = BoreResearchNotePlan.plan(note, reversedSet, reversedFunds);
        require(again.ready() && plan.placements().equals(again.placements()) && plan.rpCosts().equals(again.rpCosts())
                && plan.stats().equals(again.stats()) && plan.noteFingerprint().equals(again.noteFingerprint()), "Input iteration order changed the detached route");
        var missing = new LinkedHashSet<>(known); missing.remove("motus");
        var missingFunds = new LinkedHashMap<>(funds); missingFunds.remove("motus");
        refused(BoreResearchNotePlan.plan(note, missing, missingFunds), BoreResearchNotePlan.Status.MISSING_FIXED_DISCOVERY);
        refused(BoreResearchNotePlan.plan(note, known, Map.of()), BoreResearchNotePlan.Status.PROVEN_UNREACHABLE);
        refused(BoreResearchNotePlan.plan(note, known, funds, new BoreResearchNotePlan.Limits(1, 1, 1)), BoreResearchNotePlan.Status.SEARCH_EXHAUSTED);
        var negative = new LinkedHashMap<>(funds); negative.put("aer", -1);
        refused(BoreResearchNotePlan.plan(note, known, negative), BoreResearchNotePlan.Status.INVALID_INPUT);
        var aliased = new LinkedHashMap<>(funds); aliased.put("aer", 32768);
        refused(BoreResearchNotePlan.plan(note, known, aliased), BoreResearchNotePlan.Status.INVALID_INPUT);
        var changedFunds = new LinkedHashMap<>(funds); changedFunds.put("aer", 28);
        require(!plan.matchesInputs(note, known, changedFunds) && !plan.validateAgainst(note, known, changedFunds).valid(), "Changed RP snapshot reused the old plan binding");
        var changedNote = new ResearchNoteData(note.key(), note.color() ^ 1, note.complete(), note.copies(), note.cells());
        require(!plan.matchesInputs(changedNote, known, funds) && !plan.validateAgainst(changedNote, known, funds).valid(), "Changed note metadata reused the old plan binding");
        result.put("boundary_assertions", List.of("deterministic insertion order", "missing fixed discovery", "zero supplied funding",
                "explicit exhaustion", "negative funding", "short overflow alias", "changed funding", "changed note metadata"));
    }
    private static void refused(BoreResearchNotePlan.Plan plan, BoreResearchNotePlan.Status status) {
        require(plan.status() == status && !plan.ready() && plan.placements().isEmpty() && plan.rpCosts().isEmpty(), "Refusal/exhaustion leaked a plan: " + plan.status());
    }
    private static void write(Path output, Map<String, Object> result) throws java.io.IOException {
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n");
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
