package thaumcraft.fabric.clienttest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.research.ResearchNotePuzzle;
import thaumcraft.common.lib.utils.HexUtils.Hex;

/** Detached, bounded ARCANEBORE route arithmetic for CLIENT TESTS ONLY.
 * No world/player/menu/item/network/RNG dependency or completion/write authority.
 * Never promote this into gameplay or treat a local certificate as acquisition. */
public final class BoreResearchNotePlan {
    public static final int HARD_STATE_LIMIT = 250_000;
    private static final int MAX_TAGS = 128, MAX_CELLS = 33, RADIUS = 3, INF = 1_000_000;
    private static final Set<String> FIXED_TAGS = Set.of("motus", "perfodio", "machina", "instrumentum");
    public static final Limits DEFAULT_LIMITS = new Limits(HARD_STATE_LIMIT, 4096, 32);

    private BoreResearchNotePlan() {}

    public enum Status {
        READY, INVALID_INPUT, MISSING_FIXED_DISCOVERY, ALREADY_COMPLETE,
        ALREADY_CONNECTED, PROVEN_UNREACHABLE, SEARCH_EXHAUSTED, VALIDATION_FAILED
    }

    /** Explicit finite work/alternative caps; smaller caps can cause exhaustion,
     * never a false proof of unreachable. There is no elapsed-time/clock budget. */
    public record Limits(int states, int pathStatesPerTarget, int pathsPerTarget) {
        public Limits {
            if (states < 1 || states > HARD_STATE_LIMIT || pathStatesPerTarget < 1
                    || pathStatesPerTarget > states || pathsPerTarget < 1 || pathsPerTarget > 128)
                throw new IllegalArgumentException("Finite planner limits exceed the declared bounds");
        }
    }

    /** One native PLACE on an existing blank cell; its conservative RP cost is1.
     * This is a description, not an Edit packet, callback or grant capability. */
    public record Placement(int q, int r, String aspect) {}

    public record Stats(int statesUsed, int stateLimit, int networkStates, int relaxationStates,
                        int pathStates, int distinctNetworks, boolean globalLimitReached,
                        boolean pathLimitReached, boolean alternativeLimitReached,
                        boolean minimumPlacementCountProven) {}

    public record Validation(boolean valid, String detail, int placementsRetainedByPureSolver,
                             int preexistingPlacedCellsPrunedByNativeCompletion) {}

    /** Private construction keeps snapshots/cost/certificate internally coherent.
     * Even a READY plan has NO authority to edit or complete its issued note. */
    public static final class Plan {
        private final ResearchNoteData issued;
        private final String fingerprint;
        private final Set<String> discovered;
        private final Map<String, Integer> budget;
        private final Limits limits;
        private final Status status;
        private final String reason;
        private final List<Placement> placements;
        private final Map<String, Integer> rpCosts;
        private final Stats stats;
        private final Validation validation;

        private Plan(ResearchNoteData issued, Set<String> discovered, Map<String, Integer> budget, Limits limits,
                     Status status, String reason, List<Placement> placements, Stats stats, Validation validation) {
            this.issued = issued; fingerprint = issued == null ? "" : fingerprint(issued);
            this.discovered = Collections.unmodifiableSet(new LinkedHashSet<>(discovered));
            this.budget = Collections.unmodifiableMap(new LinkedHashMap<>(budget));
            this.limits = limits; this.status = status; this.reason = reason;
            this.placements = List.copyOf(placements); rpCosts = costs(this.placements);
            this.stats = stats; this.validation = validation;
        }
        public ResearchNoteData issuedNote() { return issued; }
        public String noteFingerprint() { return fingerprint; }
        public Set<String> discoveredSnapshot() { return discovered; }
        public Map<String, Integer> actorBudgetSnapshot() { return budget; }
        public Limits limits() { return limits; }
        public Status status() { return status; }
        public String reason() { return reason; }
        public List<Placement> placements() { return placements; }
        public Map<String, Integer> rpCosts() { return rpCosts; }
        public int placementActions() { return placements.size(); }
        public Stats stats() { return stats; }
        public Validation validation() { return validation; }
        public boolean ready() { return status == Status.READY && validation.valid(); }

        /** Initial binding only. After a real acknowledged placement the note/RP
         * snapshots naturally change: caller tracks that ack or replans READ-ONLY. */
        public boolean matchesInputs(ResearchNoteData actual, Set<String> actualDiscovered, Map<String, Integer> actualBudget) {
            return issued != null && issued.equals(actual) && fingerprint.equals(fingerprint(actual))
                    && discovered.equals(actualDiscovered) && budget.equals(actualBudget);
        }
        public Validation validateAgainst(ResearchNoteData actual, Set<String> actualDiscovered, Map<String, Integer> actualBudget) {
            if (!ready()) return invalid("PLAN_NOT_READY: " + status);
            if (!matchesInputs(actual, actualDiscovered, actualBudget)) return invalid("INPUT_SNAPSHOT_CHANGED");
            return verify(actual, discovered, budget, placements);
        }
    }

    public static Plan plan(ResearchNoteData issued, Set<String> actuallyDiscovered, Map<String, Integer> actualActorRpBudget) {
        return plan(issued, actuallyDiscovered, actualActorRpBudget, DEFAULT_LIMITS);
    }

    public static Plan plan(ResearchNoteData issued, Set<String> actuallyDiscovered,
                            Map<String, Integer> actualActorRpBudget, Limits limits) {
        Set<String> discovered = Set.of(); Map<String, Integer> budget = Map.of();
        Limits bounded = limits == null ? DEFAULT_LIMITS : limits;
        try {
            if (issued == null || actuallyDiscovered == null || actualActorRpBudget == null || limits == null)
                throw new IllegalArgumentException("Missing issued note/discovery/actor budget/limits");
            if (actuallyDiscovered.size() > MAX_TAGS || actualActorRpBudget.size() > MAX_TAGS)
                throw new IllegalArgumentException("Aspect input exceeds bounded128-tag domain");
            var known = new TreeSet<String>();
            for (String tag : actuallyDiscovered) {
                if (!canonical(tag)) throw new IllegalArgumentException("Noncanonical discovered tag: " + tag);
                known.add(tag);
            }
            discovered = Collections.unmodifiableSet(new LinkedHashSet<>(known));
            var funds = new TreeMap<String, Integer>();
            for (var entry : actualActorRpBudget.entrySet()) {
                String tag = entry.getKey(); Integer value = entry.getValue();
                if (!canonical(tag) || !known.contains(tag)) throw new IllegalArgumentException("Budget tag is not actually discovered: " + tag);
                // Do not reinterpret signed-short aliases/overflow or an unfunded
                // negative raw pool as spendable RP. No bonuses/free-roll credit.
                if (value == null || value < 0 || value > Short.MAX_VALUE)
                    throw new IllegalArgumentException("Actor RP budget must be normal0..32767: " + tag);
                funds.put(tag, value);
            }
            budget = Collections.unmodifiableMap(new LinkedHashMap<>(funds));
            requireBoreBoard(issued);
            if (issued.complete()) return failure(issued, discovered, budget, bounded, Status.ALREADY_COMPLETE,
                    "Observed note already complete; no placements or completion authority returned", null);
            var missing = new TreeSet<String>();
            for (Cell cell : issued.cells()) if (cell.type() == 1 && !known.contains(cell.aspect())) missing.add(cell.aspect());
            if (!missing.isEmpty()) return failure(issued, discovered, budget, bounded, Status.MISSING_FIXED_DISCOVERY,
                    "Actual fixed tags must be discovered before any completion claim: " + missing, null);
            var model = new Model(issued, discovered, budget);
            if (model.allFixed(model.connected(model.initial))) return failure(issued, discovered, budget, bounded, Status.ALREADY_CONNECTED,
                    "Unfinished input graph already connects its anchors; zero actions are not a native completion acknowledgement", null);
            var search = new Search(model, bounded);
            search.explore(model.initial);
            if (search.best != null) {
                List<Placement> placements = model.orderedPrefix(search.best);
                Validation checked = verify(issued, discovered, budget, placements);
                if (!checked.valid()) return failure(issued, discovered, budget, bounded, Status.VALIDATION_FAILED, checked.detail(), search);
                return new Plan(issued, discovered, budget, bounded, Status.READY,
                        search.truncated() ? "Locally verified funded route; bounded search does not prove minimum RP"
                                : "Locally verified minimum-placement funded route within the supplied immutable inputs",
                        placements, search.stats(), checked);
            }
            return failure(issued, discovered, budget, bounded,
                    search.truncated() ? Status.SEARCH_EXHAUSTED : Status.PROVEN_UNREACHABLE,
                    search.truncated() ? "No route found before a declared work/alternative cap; NOT an unsatisfiability proof"
                            : "No funded route: sound relaxed reachability/budget bounds or exhaustive connection search",
                    search);
        } catch (IllegalArgumentException rejected) {
            return failure(issued, discovered, budget, bounded, Status.INVALID_INPUT, rejected.getMessage(), null);
        }
    }

    private static Plan failure(ResearchNoteData note, Set<String> discovered, Map<String, Integer> budget,
                                Limits limits, Status status, String reason, Search search) {
        Stats stats = search == null ? new Stats(0, limits.states(), 0, 0, 0, 0, false, false, false, false) : search.stats();
        if (stats.minimumPlacementCountProven()) stats = new Stats(stats.statesUsed(), stats.stateLimit(), stats.networkStates(),
                stats.relaxationStates(), stats.pathStates(), stats.distinctNetworks(), stats.globalLimitReached(),
                stats.pathLimitReached(), stats.alternativeLimitReached(), false);
        return new Plan(note, discovered, budget, limits, status, reason, List.of(), stats, invalid(reason));
    }
    private static Validation invalid(String detail) { return new Validation(false, detail, 0, 0); }
    private static boolean canonical(String tag) {
        Aspect aspect = tag == null ? null : Aspect.getAspect(tag);
        return aspect != null && tag.equals(aspect.getTag());
    }
    private static void requireBoreBoard(ResearchNoteData note) {
        if (!"ARCANEBORE".equals(note.key()) || note.cells().size() > MAX_CELLS)
            throw new IllegalArgumentException("Only the bounded ARCANEBORE board (at most33 cells) is supported");
        int fixed = 0; var tags = new TreeSet<String>();
        for (Cell cell : note.cells()) {
            if (Math.abs(cell.q()) > RADIUS || Math.abs(cell.r()) > RADIUS || Math.abs(cell.q() + cell.r()) > RADIUS)
                throw new IllegalArgumentException("Bore cells exceed original radius3");
            if (cell.type() == 1) { fixed++; tags.add(cell.aspect()); }
        }
        if (fixed != 4 || !tags.equals(FIXED_TAGS)) throw new IllegalArgumentException("Observed Bore fixed endpoints do not match the four canonical tags");
    }

    /** Replays descriptions on a fresh cell list, preserving ALL observed cells,
     * holes/order/occupied labels. The solved/pruned copy is used ONLY to verify. */
    private static Validation verify(ResearchNoteData note, Set<String> known, Map<String, Integer> budget, List<Placement> placements) {
        requireBoreBoard(note);
        if (note.complete()) return invalid("Observed complete flag changed");
        for (Cell cell : note.cells()) if (cell.type() == 1 && !known.contains(cell.aspect())) return invalid("Missing fixed discovery");
        var model = new Model(note, known, budget);
        int[] values = model.initial.clone(); long connected = model.connected(values);
        var cells = new ArrayList<>(note.cells()); var spent = new TreeMap<String, Integer>();
        for (Placement placement : placements) {
            if (placement == null || !known.contains(placement.aspect()) || !canonical(placement.aspect())) return invalid("Undiscovered/noncanonical placement");
            int index = model.index(placement.q(), placement.r());
            if (index < 0 || note.cells().get(index).type() != 0 || values[index] >= 0) return invalid("Placement overwrites a label, repeats a cell or fills a hole");
            if (model.allFixed(connected)) return invalid("Unused placement follows an earlier native completion prefix");
            int tag = model.tags.indexOf(placement.aspect());
            if (tag < 0 || !model.touches(index, tag, connected, values)) return invalid("Placement is not connected to the main component");
            int cost = spent.merge(placement.aspect(), 1, Integer::sum);
            if (cost > budget.getOrDefault(placement.aspect(), 0)) return invalid("Supplied actor RP budget exceeded");
            values[index] = tag; connected = model.connected(values);
            Cell original = cells.get(index);
            cells.set(index, new Cell(original.q(), original.r(), 2, placement.aspect()));
        }
        if (!model.allFixed(connected)) return invalid("Not all four fixed endpoints are connected");
        ResearchNoteData candidate = note.withCells(cells, false); // Detached; NEVER a server/item write.
        ResearchNoteData solved = ResearchNotePuzzle.solve(candidate, known);
        if (!solved.complete()) return invalid("Native pure puzzle rejected the hypothetical candidate");
        int retained = 0, existingPruned = 0;
        for (Placement placement : placements) {
            Cell result = solved.cellAt(placement.q(), placement.r());
            if (result == null || result.type() != 2 || !placement.aspect().equals(result.aspect())) return invalid("Native completion would discard a planned placement");
            retained++;
        }
        for (Cell old : note.cells()) if (old.type() == 2 && solved.cellAt(old.q(), old.r()) == null) existingPruned++;
        return new Validation(true, existingPruned == 0 ? "Detached candidate accepted; all planned cells retained"
                : "Detached candidate accepted; native completion prunes " + existingPruned + " PREEXISTING placed cells (no refund assumed)", retained, existingPruned);
    }

    private static Map<String, Integer> costs(List<Placement> placements) {
        var counts = new TreeMap<String, Integer>();
        for (Placement placement : placements) counts.merge(placement.aspect(), 1, Integer::sum);
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    /** Length-framed SHA256 includes native cell order AND every note field.
     * This is an equality binding, NOT an issuance/session/revision signature. */
    public static String fingerprint(ResearchNoteData note) {
        if (note == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            text(digest, "bore-note-plan-v1"); text(digest, note.key()); integer(digest, note.color());
            integer(digest, note.complete() ? 1 : 0); integer(digest, note.copies()); integer(digest, note.cells().size());
            for (Cell cell : note.cells()) {
                integer(digest, cell.q()); integer(digest, cell.r()); integer(digest, cell.type());
                if (cell.aspect() == null) integer(digest, -1); else text(digest, cell.aspect());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("Required SHA-256 missing", impossible); }
    }
    private static void text(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8); integer(digest, bytes.length); digest.update(bytes);
    }
    private static void integer(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24)); digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8)); digest.update((byte) value);
    }

    private static final class Model {
        final ResearchNoteData note;
        final List<String> tags;
        final boolean[] known;
        final boolean[][] links;
        final int[][] linkedTags, neighbours;
        final int[] initial, funds;
        final int root, blanks;
        final long fixed;
        Model(ResearchNoteData note, Set<String> discovered, Map<String, Integer> budget) {
            this.note = note;
            var names = new TreeSet<>(discovered);
            for (Cell cell : note.cells()) if (cell.type() > 0) names.add(cell.aspect());
            if (names.size() > MAX_TAGS) throw new IllegalArgumentException("Combined aspect domain exceeds128 tags");
            tags = List.copyOf(names); known = new boolean[tags.size()]; funds = new int[tags.size()];
            int first = -1, empty = 0; long endpoints = 0;
            initial = new int[note.cells().size()]; neighbours = new int[initial.length][6];
            for (int cell = 0; cell < initial.length; cell++) {
                Cell value = note.cells().get(cell);
                initial[cell] = value.type() == 0 ? -1 : tags.indexOf(value.aspect());
                if (value.type() == 0) empty++;
                if (value.type() == 1) { endpoints |= 1L << cell; if (first < 0) first = cell; }
                Hex hex = new Hex(value.q(), value.r());
                for (int direction = 0; direction < 6; direction++) {
                    Hex adjacent = hex.getNeighbour(direction); neighbours[cell][direction] = index(adjacent.q, adjacent.r);
                }
            }
            root = first; blanks = empty; fixed = endpoints;
            links = new boolean[tags.size()][tags.size()]; linkedTags = new int[tags.size()][];
            for (int tag = 0; tag < tags.size(); tag++) {
                known[tag] = discovered.contains(tags.get(tag));
                funds[tag] = Math.min(blanks, budget.getOrDefault(tags.get(tag), 0)); // Search-only safe cap, never invents funds.
                var linked = new ArrayList<Integer>();
                for (int other = 0; other < tags.size(); other++) if (ResearchNotePuzzle.isDirectlyConnected(tags.get(tag), tags.get(other))) {
                    links[tag][other] = true; linked.add(other);
                }
                linkedTags[tag] = linked.stream().mapToInt(Integer::intValue).toArray();
            }
        }
        int index(int q, int r) {
            for (int index = 0; index < note.cells().size(); index++) {
                Cell cell = note.cells().get(index);
                if (cell.q() == q && cell.r() == r) return index;
            }
            return -1;
        }
        boolean allFixed(long component) { return (component & fixed) == fixed; }
        long connected(int[] values) {
            if (root < 0 || values[root] < 0 || !known[values[root]]) return 0;
            long reached = 1L << root;
            var queue = new ArrayDeque<Integer>(); queue.add(root);
            while (!queue.isEmpty()) {
                int from = queue.removeFirst();
                for (int to : neighbours[from]) if (to >= 0 && (reached & 1L << to) == 0 && values[to] >= 0
                        && known[values[to]] && links[values[from]][values[to]]) {
                    reached |= 1L << to; queue.addLast(to);
                }
            }
            return reached;
        }
        boolean touches(int cell, int tag, long component, int[] values) {
            for (int neighbour : neighbours[cell]) if (neighbour >= 0 && (component & 1L << neighbour) != 0
                    && links[tag][values[neighbour]]) return true;
            return false;
        }
        int count(int[] values) {
            int count = 0;
            for (int cell = 0; cell < values.length; cell++) if (initial[cell] < 0 && values[cell] >= 0) count++;
            return count;
        }
        int[] remaining(int[] values) {
            int[] remaining = funds.clone();
            for (int cell = 0; cell < values.length; cell++) if (initial[cell] < 0 && values[cell] >= 0) --remaining[values[cell]];
            return remaining;
        }
        boolean allowed(int cell, int tag, int[] values, int[] remaining) {
            return known[tag] && (values[cell] >= 0 ? values[cell] == tag : remaining[tag] > 0);
        }
        /** Never erases observed cells. Choose only a connected prefix of NEW
         * hypothetical cells, stopping at the first completion (not after it). */
        List<Placement> orderedPrefix(int[] solution) {
            int[] values = initial.clone(); var result = new ArrayList<Placement>();
            long connected = connected(values);
            while (!allFixed(connected)) {
                boolean advanced = false;
                for (int cell = 0; cell < values.length; cell++) if (initial[cell] < 0 && values[cell] < 0 && solution[cell] >= 0
                        && touches(cell, solution[cell], connected, values)) {
                    Cell original = note.cells().get(cell); values[cell] = solution[cell];
                    result.add(new Placement(original.q(), original.r(), tags.get(solution[cell])));
                    connected = connected(values); advanced = true;
                    if (allFixed(connected)) break;
                }
                if (!advanced) throw new IllegalArgumentException("Hypothetical solution contains no connected placement order");
            }
            return List.copyOf(result);
        }
    }

    private enum Work { NETWORK, RELAXATION, PATH }
    private static final class Counter {
        final int limit;
        int used, networks, relaxations, paths;
        boolean exhausted;
        Counter(int limit) { this.limit = limit; }
        boolean take(Work kind) {
            if (used >= limit) { exhausted = true; return false; }
            used++;
            switch (kind) { case NETWORK -> networks++; case RELAXATION -> relaxations++; case PATH -> paths++; }
            return true;
        }
    }
    private record Target(int cell, int distance, int[] distances) {}

    private static final class Search {
        final Model model;
        final Limits limits;
        final Counter counter;
        final Set<String> networks = new TreeSet<>();
        boolean pathCut, alternativeCut;
        int[] best;
        int bestCost = Integer.MAX_VALUE;
        Search(Model model, Limits limits) { this.model = model; this.limits = limits; counter = new Counter(limits.states()); }
        boolean truncated() { return counter.exhausted || pathCut || alternativeCut; }
        Stats stats() {
            return new Stats(counter.used, counter.limit, counter.networks, counter.relaxations, counter.paths,
                    networks.size(), counter.exhausted, pathCut, alternativeCut, best != null && !truncated());
        }
        void explore(int[] values) {
            if (!counter.take(Work.NETWORK) || !networks.add(key(values))) return;
            long component = model.connected(values);
            if (model.allFixed(component)) {
                List<Placement> prefix = model.orderedPrefix(values);
                if (prefix.size() < bestCost) {
                    best = model.initial.clone();
                    for (Placement placement : prefix) best[model.index(placement.q(), placement.r())] = model.tags.indexOf(placement.aspect());
                    bestCost = prefix.size();
                }
                return;
            }
            int used = model.count(values);
            int[] remaining = model.remaining(values);
            int available = Arrays.stream(remaining).sum();
            int room = Math.min(available, best == null ? model.blanks - used : bestCost - used - 1);
            if (room <= 0) return;
            var targets = new ArrayList<Target>();
            for (int cell = 0; cell < values.length; cell++) if ((model.fixed & 1L << cell) != 0 && (component & 1L << cell) == 0) {
                int[] distances = relaxed(values, remaining, cell);
                if (distances == null) return; // Global budget, NOT an unreachable proof.
                int nearest = INF;
                for (int start = 0; start < values.length; start++) if ((component & 1L << start) != 0)
                    nearest = Math.min(nearest, distances[start * model.tags.size() + values[start]]);
                // This relaxed product graph IGNORES quotas/repeated-hex label
                // conflicts. No path here is a sound impossibility for this
                // branch; max root-to-anchor distance is a safe shared-network LB.
                if (nearest == INF || nearest > room) return;
                targets.add(new Target(cell, nearest, distances));
            }
            targets.sort(Comparator.comparingInt(Target::distance).thenComparingInt(Target::cell));
            for (Target target : targets) {
                if (counter.exhausted) return;
                var paths = new Paths(this, values, remaining, component, target, room);
                int alternatives = 0;
                while (!counter.exhausted && alternatives < limits.pathsPerTarget()) {
                    int[] connection = paths.next();
                    if (connection == null) break;
                    alternatives++;
                    explore(connection); // Alternative paths AND next-anchor order are backtracked.
                }
                if (alternatives == limits.pathsPerTarget() && !paths.queue.isEmpty()) alternativeCut = true;
            }
        }
        /** Reverse0/1 BFS over (cell,canonical-tag), permitting repeated labels
         * and ignoring per-tag quantities beyond positivity. Overapproximation
         * may miss a proof, never manufacture an unreachable proof. */
        int[] relaxed(int[] values, int[] remaining, int target) {
            int width = model.tags.size(); int[] distance = new int[values.length * width]; Arrays.fill(distance, INF);
            var queue = new ArrayDeque<Integer>(); int goal = target * width + values[target];
            if (!counter.take(Work.RELAXATION)) return null;
            distance[goal] = 0; queue.add(goal);
            while (!queue.isEmpty()) {
                int node = queue.removeFirst(), cell = node / width, tag = node % width;
                int weight = values[cell] >= 0 ? 0 : 1;
                for (int otherCell : model.neighbours[cell]) if (otherCell >= 0) for (int otherTag : model.linkedTags[tag]) {
                    if (!model.allowed(otherCell, otherTag, values, remaining)) continue;
                    int previous = otherCell * width + otherTag, cost = distance[node] + weight;
                    if (cost >= distance[previous]) continue;
                    if (!counter.take(Work.RELAXATION)) return null;
                    distance[previous] = cost;
                    if (weight == 0) queue.addFirst(previous); else queue.addLast(previous);
                }
            }
            return distance;
        }
    }

    private record Path(int end, long visited, int[] values, int[] used, int cost, int estimate, long serial) {}

    /** Best-first SIMPLE paths, not a blind assignment of all29 blanks. Each
     * yielded prefix reaches at least one new fixed endpoint; recursion depth
     * is at most3. Existing occupied labels are never overwritten. */
    private static final class Paths {
        final Search search;
        final int[] remaining;
        final long main, oldFixed;
        final Target target;
        final int room;
        final PriorityQueue<Path> queue = new PriorityQueue<>(Comparator.comparingInt(Path::estimate)
                .thenComparingInt(path -> path.estimate() - path.cost()).thenComparingLong(Path::serial));
        final Set<String> seen = new TreeSet<>();
        int generated;
        long serial;
        boolean stopped;
        Paths(Search search, int[] values, int[] remaining, long main, Target target, int room) {
            this.search = search; this.remaining = remaining; this.main = main; this.target = target; this.room = room;
            oldFixed = main & search.model.fixed;
            for (int cell = 0; cell < values.length && !stopped; cell++) if ((main & 1L << cell) != 0) {
                int estimate = target.distances()[cell * search.model.tags.size() + values[cell]];
                if (estimate <= room) offer(new Path(cell, 1L << cell, values.clone(), new int[remaining.length], 0, estimate, serial++));
            }
        }
        void offer(Path path) {
            String identity = key(path.values()) + ":" + path.end() + ":" + Long.toUnsignedString(path.visited(), 16);
            if (!seen.add(identity)) return;
            if (generated >= search.limits.pathStatesPerTarget()) {
                search.pathCut = stopped = true; queue.clear(); return;
            }
            if (!search.counter.take(Work.PATH)) { stopped = true; queue.clear(); return; }
            generated++; queue.add(path);
        }
        int[] next() {
            Model model = search.model;
            while (!stopped && !queue.isEmpty()) {
                Path path = queue.remove();
                long connected = model.connected(path.values());
                if ((connected & model.fixed) != oldFixed) return path.values();
                for (int cell : model.neighbours[path.end()]) {
                    if (cell < 0 || (main & 1L << cell) != 0 || (path.visited() & 1L << cell) != 0) continue;
                    for (int tag : model.linkedTags[path.values()[path.end()]]) {
                        if (!model.known[tag]) continue;
                        boolean blank = path.values()[cell] < 0;
                        if (blank ? path.used()[tag] >= remaining[tag] : path.values()[cell] != tag) continue;
                        int cost = path.cost() + (blank ? 1 : 0);
                        int distance = target.distances()[cell * model.tags.size() + tag];
                        if (distance == INF || cost + distance > room) continue;
                        int[] values = path.values().clone(), used = path.used().clone(); values[cell] = tag;
                        if (blank) used[tag]++;
                        offer(new Path(cell, path.visited() | 1L << cell, values, used, cost, cost + distance, serial++));
                        if (stopped) return null;
                    }
                }
            }
            return null;
        }
    }

    private static String key(int[] values) {
        var key = new StringBuilder(values.length);
        for (int value : values) key.append((char) (value + 1));
        return key.toString();
    }
}
