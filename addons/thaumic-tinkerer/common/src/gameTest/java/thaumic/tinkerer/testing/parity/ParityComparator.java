package thaumic.tinkerer.testing.parity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import thaumic.tinkerer.testing.parity.OracleData.Condition;
import thaumic.tinkerer.testing.parity.OracleData.Item;
import thaumic.tinkerer.testing.parity.OracleData.Page;
import thaumic.tinkerer.testing.parity.OracleData.Recipe;
import thaumic.tinkerer.testing.parity.OracleData.Research;
import thaumic.tinkerer.testing.parity.PortSnapshot.PoolEntry;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortPage;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortRecipe;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortResearch;

/**
 * Compares the content the port registered with Thaumcraft ({@link PortSnapshot}) with what Thaumic Tinkerer 1.7.10
 * registered ({@link OracleData}). Plain Java: no Minecraft types, so the logic is tested without a world and the
 * tests can mutate a value of the oracle and see the comparison fail. Test-only code of the port (no 1.7.10
 * counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>An entry of the original is expected when every condition it was registered under holds in the port (a mod that
 * does not exist on 1.21.1 never holds). Every difference is a failure unless a {@link KnownDifference} names exactly
 * that subject, field, original value and port value, and each entry allows as many differences as its
 * {@code occurrences} (one by default: it is consumed by the difference it matches); an allow-list entry that matches
 * less often is reported too, so the list cannot go stale.</p>
 *
 * <p>Shaped recipes are compared slot by slot over the full grid, so the position of a gap matters; a mirror image of
 * the pattern is the same recipe in 1.7.10 and in 1.21.1 (both match mirrored) and is accepted. A slot of the port has
 * to accept every item the original's ingredient stood for (all variants of a bare {@code Block}, the items of an ore
 * name) and, unless it belongs to a documented merge of two original recipes into one, nothing else.</p>
 */
public final class ParityComparator {
    /** The tabs of Thaumcraft a Kami entry looks at; the add-on's own tab (LibResearch.CATEGORY_THAUMICTINKERER) is the sixth. */
    public static final Set<String> KAMI_THAUMCRAFT_TABS = Set.of("BASICS", "GOLEMANCY", "ARTIFICE", "ALCHEMY", "THAUMATURGY");
    /** KamiResearchItem.Blacklist and the "KAMI" suffix rule of the original. */
    public static final String KAMI_BLACKLIST_PREFIX = "MINILITH";
    public static final String KAMI_SUFFIX = "KAMI";

    private ParityComparator() {}

    /** The research a Kami entry requires: every research of these tabs (KamiResearchItem.setPages). */
    public static Set<String> kamiTabs(String addOnTab) {
        Set<String> tabs = new LinkedHashSet<>(KAMI_THAUMCRAFT_TABS);
        tabs.add(addOnTab);
        return tabs;
    }

    /** The truth of a condition of the original in the running port. */
    public interface Environment {
        boolean value(Condition condition);
    }

    /**
     * The entries of the original are expected under the original's own configuration: every flag one of them depends on
     * has to be at the default of the original's {@code ConfigHandler} in the port, or the set of entries the comparison
     * expects would change silently with a changed default or a stale run directory. Empty when all flags are at their defaults.
     */
    public static List<String> configProblems(OracleData oracle, Environment env) {
        List<String> problems = new ArrayList<>();
        if (oracle.configDefaults().isEmpty()) problems.add("the oracle records no defaults of the flags it depends on");
        for (Map.Entry<String, Boolean> e : oracle.configDefaults().entrySet()) {
            boolean now = env.value(new Condition(e.getKey(), "config", true));
            if (now != e.getValue()) {
                problems.add("the flag " + e.getKey() + " is " + now + " in the port but " + e.getValue() + " by default in the original");
            }
        }
        return problems;
    }

    public record Difference(String kind, String subject, String field, String original, String port) {
        @Override
        public String toString() {
            return kind + " " + subject + ": " + field + ": original=" + original + " | port=" + port;
        }
    }

    public static final class Result {
        public final List<Difference> failures = new ArrayList<>();
        public final List<Difference> allowed = new ArrayList<>();
        /** The allow-list entries that matched fewer differences than they allow, and how often they matched. */
        public final List<KnownDifference> unusedKnown = new ArrayList<>();
        public final List<Integer> unusedMatched = new ArrayList<>();
        public final Map<String, Integer> counts = new LinkedHashMap<>();
        /** Things that were accepted without being a difference (mirror images, open ore tags), for the report. */
        public final List<String> notes = new ArrayList<>();

        public boolean passed() {
            return failures.isEmpty() && unusedKnown.isEmpty();
        }

        public List<Difference> failures(String kind) {
            return failures.stream().filter(d -> d.kind().equals(kind)).toList();
        }

        /** Multi-line report of everything that is not clean. */
        public String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append(failures.size()).append(" differences, ").append(allowed.size()).append(" documented, ")
                    .append(unusedKnown.size()).append(" stale allow-list entries\n");
            for (Difference d : failures) sb.append("  FAIL ").append(d).append('\n');
            for (int i = 0; i < unusedKnown.size(); i++) {
                KnownDifference k = unusedKnown.get(i);
                sb.append("  STALE ").append(k.describe()).append(" (matched ").append(unusedMatched.get(i)).append(" of ")
                        .append(k.occurrences()).append(")\n");
            }
            return sb.toString();
        }
    }

    public static Result compare(OracleData oracle, PortSnapshot port, Environment env, List<KnownDifference> known) {
        List<Difference> all = new ArrayList<>();
        Result result = new Result();
        Recipes recipes = new Recipes(oracle, env);
        compareResearch(oracle, port, env, recipes, all, result);
        compareRecipes(oracle, port, env, recipes, all, result);
        compareObjectTags(oracle, port, all, result);
        int[] remaining = new int[known.size()];
        for (int i = 0; i < remaining.length; i++) remaining[i] = known.get(i).occurrences();
        for (Difference d : all) {
            int hit = -1;
            for (int i = 0; i < known.size(); i++) {
                if (remaining[i] > 0 && known.get(i).matches(d)) {
                    hit = i;
                    break;
                }
            }
            if (hit >= 0) {
                remaining[hit]--;
                result.allowed.add(d);
            } else {
                result.failures.add(d);
            }
        }
        for (int i = 0; i < known.size(); i++) {
            if (remaining[i] > 0) {
                result.unusedKnown.add(known.get(i));
                result.unusedMatched.add(known.get(i).occurrences() - remaining[i]);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------ helpers

    /** For the tests of this package's users: whether an entry of the original applies in an environment. */
    public static boolean holdsForTest(List<Condition> when, Environment env) {
        return holds(when, env);
    }

    static boolean holds(List<Condition> when, Environment env) {
        for (Condition c : when) if (env.value(c) != c.value()) return false;
        return true;
    }

    static String list(List<String> values) {
        return "[" + String.join(", ", values) + "]";
    }

    static String aspects(Map<String, Integer> values) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : values.entrySet()) parts.add(e.getKey() + ":" + e.getValue());
        return String.join(", ", parts);
    }

    static String when(List<Condition> conditions) {
        if (conditions.isEmpty()) return "always";
        List<String> parts = new ArrayList<>();
        for (Condition c : conditions) parts.add(c.toString());
        return String.join(" && ", parts);
    }

    private static void diff(List<Difference> out, String kind, String subject, String field, Object original, Object port) {
        String o = String.valueOf(original);
        String p = String.valueOf(port);
        if (!o.equals(p)) out.add(new Difference(kind, subject, field, o, p));
    }

    /** How a stack is printed in a difference and in the recipes a research page shows: the item, its aspect datum (the infused seeds), the count. */
    public static String stackText(String id, String aspect, int count) {
        return id + (aspect == null ? "" : "{aspect=" + aspect + "}") + " x" + count;
    }

    private static String stack(Item item) {
        return stackText(String.valueOf(item.resolved()), item.aspect(), item.count());
    }

    // ------------------------------------------------------------------------------------------ recipes by name

    /** The recipes of the original by name; a research page shows the last one registered under its name. */
    private static final class Recipes {
        private final Map<String, Recipe> lastByName = new HashMap<>();
        private final Map<String, Recipe> lastByNameAny = new HashMap<>();
        private final Map<String, Integer> countByName = new HashMap<>();

        Recipes(OracleData oracle, Environment env) {
            for (Recipe r : oracle.recipes()) {
                if (r.name() == null) continue;
                Recipe any = lastByNameAny.get(r.name());
                if (any == null || any.order() < r.order()) lastByNameAny.put(r.name(), r);
                if (!r.registered() || !holds(r.when(), env)) continue;
                countByName.merge(r.name(), 1, Integer::sum);
                Recipe seen = lastByName.get(r.name());
                if (seen == null || seen.order() < r.order()) lastByName.put(r.name(), r);
            }
        }

        /** What a research page that names a recipe shows; a recipe the original registers only under a condition that does not hold counts too. */
        String shown(String name) {
            Recipe r = lastByName.getOrDefault(name, lastByNameAny.get(name));
            return r == null || r.output() == null ? "?" + name : stack(r.output());
        }

        boolean shadowed(String name) {
            return name != null && countByName.getOrDefault(name, 0) > 1;
        }
    }

    // ------------------------------------------------------------------------------------------ research

    private static void compareResearch(OracleData oracle, PortSnapshot port, Environment env, Recipes recipes,
            List<Difference> out, Result result) {
        Map<String, Integer> counts = result.counts;
        Map<String, List<Research>> byKey = new LinkedHashMap<>();
        Set<String> kamiKeys = new LinkedHashSet<>();
        for (Research r : oracle.research()) {
            byKey.computeIfAbsent(r.key(), k -> new ArrayList<>()).add(r);
            if (r.kami()) kamiKeys.add(r.key());
        }
        int compared = 0;
        for (Map.Entry<String, List<Research>> e : byKey.entrySet()) {
            String key = e.getKey();
            List<Research> applicable = e.getValue().stream().filter(r -> r.registered() && holds(r.when(), env)).toList();
            PortResearch p = port.research().get(key);
            if (applicable.size() > 1) {
                diff(out, "research", key, "oracle", "one entry", applicable.size() + " entries apply at once");
            } else if (applicable.isEmpty()) {
                if (p != null) {
                    diff(out, "research", key, "presence", whyNot(e.getValue()), "registered");
                    // the port registers what the original does not (here): its fields are still checked against the original's definition
                    compareResearch(e.getValue().get(0), p, port, kamiKeys, recipes, out);
                }
            } else if (p == null) {
                diff(out, "research", key, "presence", "registered", "missing" + inAnotherTab(port, key, e.getValue().get(0).category()));
            } else {
                compareResearch(applicable.get(0), p, port, kamiKeys, recipes, out);
                compared++;
            }
        }
        for (PortResearch p : port.research().values()) {
            if (!byKey.containsKey(p.key())) diff(out, "research", p.key(), "presence", "not in the original", "registered");
        }
        counts.put("research compared", compared);
        counts.put("research in the oracle", byKey.size());
        counts.put("research in the port", port.research().size());
        kamiRequirements(oracle, port, env, out, result);
    }

    /** A research of the original that is missing from the add-on's tab but exists, under that key, in another tab. */
    private static String inAnotherTab(PortSnapshot port, String key, String category) {
        for (PoolEntry e : port.researchPool()) {
            if (e.key().equals(key) && !e.category().equals(category)) return " (registered in the tab " + e.category() + ")";
        }
        return "";
    }

    /**
     * The one documented difference of the Kami requirements. {@code KamiResearchItem.setPages} of 1.7.10 collected the
     * research registered at that moment, and {@code TTRegistry.postInit} registers the research of every item before that
     * of any block, so an item-owned Kami entry never required a block-owned research of the add-on and which item-owned
     * ones it required depended on the iteration order of a HashMap. The port requires the union for every entry
     * (see {@link #compareHiddenParents}). This is reported once, with the keys, so that it has to be allow-listed.
     */
    private static void kamiRequirements(OracleData oracle, PortSnapshot port, Environment env, List<Difference> out, Result result) {
        List<String> itemOwned = new ArrayList<>();
        List<String> blockOwnedKami = new ArrayList<>();
        Set<String> blockOwned = new TreeSet<>();
        for (Research r : oracle.research()) {
            if (r.kami()) {
                if (r.registered() && holds(r.when(), env) && r.parentsHiddenAuto()) {
                    (("block".equals(r.ownerKind())) ? blockOwnedKami : itemOwned).add(r.key());
                }
            } else if ("block".equals(r.ownerKind()) && !r.key().startsWith(KAMI_BLACKLIST_PREFIX) && !r.key().endsWith(KAMI_SUFFIX)
                    && port.researchPool().stream().anyMatch(e -> e.key().equals(r.key()) && !e.lost() && !e.virtual())) {
                blockOwned.add(r.key());          // block-owned research the port registers, whether or not the original did
            }
        }
        if (itemOwned.isEmpty() || blockOwned.isEmpty()) return;
        String original = "the " + itemOwned.size() + " item-owned Kami entries never required a block-owned research of the add-on "
                + "(TTRegistry.postInit registers the research of the items before that of the blocks) and only the item-owned research "
                + "registered before each (HashMap order); " + blockOwnedKami + " (block-owned) required the block-owned research "
                + "registered before it; Thaumcraft's five tabs and ResearchHelper's research were required by all";
        String got = "every Kami entry requires the union: all other non-Kami research of the six tabs, the block-owned research of the add-on included";
        diff(out, "research", "<Kami entries>", "parentsHidden (membership)", original, got);
        result.notes.add("Kami entries: the " + itemOwned.size() + " item-owned ones also require the " + blockOwned.size()
                + " block-owned research of the add-on " + blockOwned);
    }

    private static String whyNot(List<Research> entries) {
        if (entries.stream().noneMatch(Research::registered)) return "never registered (its owner's shouldRegister() is false)";
        List<String> conditions = new ArrayList<>();
        for (Research r : entries) if (r.registered()) conditions.add(when(r.when()));
        return "registered only when " + String.join(" or ", new LinkedHashSet<>(conditions));
    }

    private static void compareResearch(Research o, PortResearch p, PortSnapshot port, Set<String> kamiKeys, Recipes recipes,
            List<Difference> out) {
        String k = o.key();
        diff(out, "research", k, "category", o.category(), p.category());
        diff(out, "research", k, "aspects", aspects(o.aspects()), aspects(p.aspects()));
        diff(out, "research", k, "column", o.column(), p.column());
        diff(out, "research", k, "row", o.row(), p.row());
        diff(out, "research", k, "complexity", o.complexity(), p.complexity());
        diff(out, "research", k, "parents", list(o.parents()), list(p.parents()));
        compareHiddenParents(o, p, port, kamiKeys, out);
        diff(out, "research", k, "siblings", list(o.siblings()), list(p.siblings()));
        for (Map.Entry<String, Boolean> flag : o.flags().entrySet()) {
            diff(out, "research", k, "flag " + flag.getKey(), flag.getValue(), p.flags().get(flag.getKey()));
        }
        diff(out, "research", k, "warp", o.warp(), p.warp());
        if (o.iconResolved() != null) {
            diff(out, "research", k, "icon", o.iconResolved(), p.icon());
            diff(out, "research", k, "icon.aspect", o.iconAspect(), p.iconAspect());
        }
        comparePages(o, p, recipes, out);
    }

    private static void compareHiddenParents(Research o, PortResearch p, PortSnapshot port, Set<String> kamiKeys,
            List<Difference> out) {
        if (!o.parentsHiddenAuto()) {
            diff(out, "research", o.key(), "parentsHidden", list(o.parentsHidden()), list(p.parentsHidden()));
            return;
        }
        // KamiResearchItem.setPages: the explicit hidden parents, then every other research of the six tabs that is not
        // lost, virtual, a Kami entry itself, blacklisted (MINILITH*) or named *KAMI.
        List<String> explicit = o.parentsHidden();
        Set<String> tabs = kamiTabs(o.category());
        Set<String> expected = new LinkedHashSet<>(explicit);
        for (PoolEntry e : port.researchPool()) {
            if (e.lost() || e.virtual() || !tabs.contains(e.category()) || e.key().startsWith(KAMI_BLACKLIST_PREFIX)
                    || e.key().endsWith(KAMI_SUFFIX) || kamiKeys.contains(e.key())) continue;
            expected.add(e.key());
        }
        List<String> actual = p.parentsHidden();
        boolean prefix = actual.size() >= explicit.size() && actual.subList(0, explicit.size()).equals(explicit);
        Set<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);
        Set<String> extra = new LinkedHashSet<>(actual);
        extra.removeAll(expected);
        if (!prefix || !missing.isEmpty() || !extra.isEmpty() || actual.size() != new LinkedHashSet<>(actual).size()) {
            String original = list(explicit) + " + every other research of " + tabs.stream().sorted().toList() + " ("
                    + expected.size() + " keys)";
            String got = list(actual) + (missing.isEmpty() ? "" : " missing " + list(new ArrayList<>(missing)))
                    + (extra.isEmpty() ? "" : " unexpected " + list(new ArrayList<>(extra)))
                    + (prefix ? "" : " (the explicit hidden parents " + list(explicit) + " are not first)");
            out.add(new Difference("research", o.key(), "parentsHidden", original, got));
        }
    }

    private static void comparePages(Research o, PortResearch p, Recipes recipes, List<Difference> out) {
        String k = o.key();
        diff(out, "research", k, "pages.count", o.pages().size(), p.pages().size());
        int n = Math.min(o.pages().size(), p.pages().size());
        for (int i = 0; i < n; i++) {
            Page op = o.pages().get(i);
            PortPage pp = p.pages().get(i);
            String at = "pages[" + i + "]";
            diff(out, "research", k, at + ".kind", op.kind(), pp.kind());
            if (!op.kind().equals(pp.kind())) continue;
            if (op.kind().startsWith("text")) {
                diff(out, "research", k, at + ".text", op.key(), pp.text());
            } else if (op.recipes() != null && !op.recipes().isEmpty()) {
                List<String> expected = new ArrayList<>();
                for (String name : op.recipes()) expected.add(recipes.shown(name));
                diff(out, "research", k, at + ".recipes", list(expected), list(pp.recipes()));
            }
        }
    }

    // ------------------------------------------------------------------------------------------ recipes

    private static String family(String kind) {
        if (kind.startsWith("crafting_custom")) return kind;      // a custom recipe class is its own family
        if (kind.startsWith("arcane")) return "arcane";
        if (kind.startsWith("crafting")) return "crafting";
        return kind;
    }

    private static boolean grid(String kind) {
        return kind.equals("arcane_shaped") || kind.equals("crafting_shaped");
    }

    private static String subject(Recipe r, Recipes recipes) {
        String out = r.output() == null ? r.kindKey() : String.valueOf(r.output().resolved());
        if (r.name() == null) return "<unnamed> -> " + out;
        return recipes.shadowed(r.name()) ? r.name() + " -> " + out : r.name();
    }

    private static String describe(Recipe r) {
        return r.output() == null ? r.kindKey() : r.kind() + " -> " + stack(r.output());
    }

    private static String describe(PortRecipe p) {
        return p.kind() + " -> " + stackText(p.output(), p.outputAspect(), p.count());
    }

    private static List<Item> filled(List<Item> cells) {
        return cells.stream().filter(c -> c != null).toList();
    }

    /** What the comparison of the ingredients of one recipe found, for the counts and for the merged recipes. */
    private static final class Cmp {
        int compared;
        int notComparable;
        int openOre;
        int wildcards;
        int broader;
        boolean mirrored;

        void add(Cmp other) {
            compared += other.compared;
            notComparable += other.notComparable;
            openOre += other.openOre;
            wildcards += other.wildcards;
            broader += other.broader;
        }
    }

    private static void compareRecipes(OracleData oracle, PortSnapshot port, Environment env, Recipes recipes,
            List<Difference> out, Result result) {
        List<PortRecipe> available = new ArrayList<>(port.recipes());
        int compared = 0;
        Cmp total = new Cmp();
        int mirroredGrids = 0;
        List<Recipe> ordered = new ArrayList<>();
        for (Recipe r : oracle.recipes()) if (r.registered() && holds(r.when(), env)) ordered.add(r);
        ordered.sort((a, b) -> Integer.compare(a.order(), b.order()));
        // pass 1: the port recipe that makes the same item; pass 2: for what is left and has a unique name, the recipe of that name
        Map<Recipe, PortRecipe> matched = new LinkedHashMap<>();
        for (Recipe r : ordered) {
            PortRecipe m = matchByOutput(r, available);
            if (m != null) {
                matched.put(r, m);
                available.remove(m);
            }
        }
        for (Recipe r : ordered) {
            if (matched.containsKey(r) || r.name() == null || recipes.shadowed(r.name())) continue;
            for (PortRecipe p : available) {
                if (family(p.kind()).equals(family(r.kindKey())) && r.name().equals(p.name())) {
                    matched.put(r, p);
                    available.remove(p);
                    break;
                }
            }
        }
        // pass 3: an original recipe without a port recipe of its own whose twin (same kind, result and shape) has one is a
        // recipe the port merged into the twin's; that is a difference (it has to be documented), and the merged recipe has
        // to accept the ingredients of both
        Map<Recipe, PortRecipe> merged = new LinkedHashMap<>();
        Map<PortRecipe, List<Recipe>> groups = new LinkedHashMap<>();
        for (Recipe r : ordered) {
            if (matched.containsKey(r) || r.output() == null) continue;
            for (Map.Entry<Recipe, PortRecipe> twin : matched.entrySet()) {
                if (isTwin(r, twin.getKey())) {
                    merged.put(r, twin.getValue());
                    groups.computeIfAbsent(twin.getValue(), k -> new ArrayList<>(List.of(twin.getKey()))).add(r);
                    break;
                }
            }
        }
        Map<Recipe, Boolean> mirroredOf = new LinkedHashMap<>();
        for (Recipe r : ordered) {
            String subject = subject(r, recipes);
            PortRecipe m = matched.get(r);
            boolean isMerged = merged.containsKey(r);
            if (isMerged) {
                m = merged.get(r);
                diff(out, "recipe", subject, "presence", describe(r), "merged into " + m.id() + " with the other recipe of the same result");
            }
            if (m == null) {
                diff(out, "recipe", subject, "presence", describe(r), "missing");
                continue;
            }
            compared++;
            boolean inGroup = isMerged || groups.containsKey(m);
            Cmp cmp = compareRecipe(r, m, subject, out, inGroup);
            total.add(cmp);
            if (cmp.mirrored) {
                mirroredGrids++;
                result.notes.add("shaped recipe " + subject + ": the pattern of the port is the mirror image of the original's");
            }
            mirroredOf.put(r, cmp.mirrored);
        }
        // the merged recipes together: every slot accepts exactly what the original recipes put there
        int groupSlots = 0;
        for (Map.Entry<PortRecipe, List<Recipe>> g : groups.entrySet()) {
            PortRecipe p = g.getKey();
            Recipe primary = g.getValue().get(0);
            if (primary.cells().size() != p.cells().size()) continue;
            for (int i = 0; i < p.cells().size(); i++) {
                Set<String> union = new TreeSet<>();
                for (Recipe member : g.getValue()) {
                    List<Item> cells = member.cells();
                    int at = Boolean.TRUE.equals(mirroredOf.get(member)) ? mirrorIndex(i, member.width()) : i;
                    Item item = cells.get(at);
                    if (item != null) union.addAll(item.expected());
                }
                Set<String> accepted = new TreeSet<>(p.cells().get(i));
                groupSlots++;
                if (!union.equals(accepted)) {
                    out.add(new Difference("recipe", subject(primary, recipes), "ingredient[" + i + "] (merged)", union.toString(),
                            accepted.toString()));
                }
            }
        }
        Set<String> oracleNames = new LinkedHashSet<>();
        for (Recipe r : oracle.recipes()) if (r.name() != null) oracleNames.add(r.name());
        List<Recipe> inapplicable = new ArrayList<>();
        for (Recipe r : oracle.recipes()) if (!ordered.contains(r)) inapplicable.add(r);
        for (PortRecipe p : available) {
            Recipe counterpart = null;
            for (Recipe r : inapplicable) {
                if (r.output() != null && family(p.kind()).equals(family(r.kind())) && r.output().resolved() != null
                        && r.output().resolved().equals(p.output())) {
                    counterpart = r;
                    break;
                }
            }
            if (counterpart != null) {
                inapplicable.remove(counterpart);
                total.add(compareRecipe(counterpart, p, p.name() != null ? p.name() + " -> " + p.output() : p.id(), out, false));
            }
        }
        for (PortRecipe p : available) {
            String why = p.name() != null && oracleNames.contains(p.name()) ? "registered by the original only when "
                    + oracle.recipes().stream().filter(r -> p.name().equals(r.name())).map(r -> when(r.when())).distinct().toList()
                    : "not in the original";
            boolean neverRegistered = oracle.recipes().stream().anyMatch(r -> !r.registered() && r.output() != null
                    && family(r.kind()).equals(family(p.kind())) && p.output().equals(r.output().resolved()));
            diff(out, "recipe", p.name() != null ? p.name() + " -> " + p.output() : p.id(), "presence",
                    neverRegistered ? "never registered (its owner's shouldRegister() is false)" : why, describe(p));
        }
        Map<String, Integer> counts = result.counts;
        counts.put("recipes expected", ordered.size());
        counts.put("recipes compared", compared);
        counts.put("recipes in the port", port.recipes().size());
        counts.put("ingredients compared", total.compared);
        counts.put("ingredients not comparable (unresolved)", total.notComparable);
        counts.put("ingredients that are ore names (the tag of the port may hold more)", total.openOre);
        counts.put("wildcard ingredients checked against every variant", total.wildcards);
        counts.put("ingredient slots broader than the original (merged recipes)", total.broader);
        counts.put("slots of merged recipes checked against the union", groupSlots);
        counts.put("shaped patterns that are a mirror image of the original", mirroredGrids);
    }

    // ------------------------------------------------------------------------------------------ object tags

    /**
     * The aspects the add-on registers for its own items with Thaumcraft's object tags (the mob essences): every tag of the
     * original must exist with the same aspects, and the add-on must register no other tag for an item of its namespace.
     * Not compared when the snapshot carries no tags.
     */
    private static void compareObjectTags(OracleData oracle, PortSnapshot port, List<Difference> out, Result result) {
        if (port.objectTags() == null) return;
        Set<String> expected = new LinkedHashSet<>();
        int compared = 0;
        for (OracleData.ObjectTag tag : oracle.objectTags()) {
            String id = String.valueOf(tag.item().resolved());
            expected.add(id);
            Map<String, Integer> got = port.objectTags().get(id);
            if (got == null) {
                diff(out, "objectTag", id, "presence", aspects(tag.aspects()), "missing");
            } else {
                diff(out, "objectTag", id, "aspects", aspects(tag.aspects()), aspects(got));
            }
            compared++;
        }
        for (Map.Entry<String, Map<String, Integer>> e : port.objectTags().entrySet()) {
            if (!expected.contains(e.getKey())) diff(out, "objectTag", e.getKey(), "presence", "not in the original", aspects(e.getValue()));
        }
        result.counts.put("object tags compared", compared);
        result.counts.put("object tags in the port", port.objectTags().size());
    }

    /** Two original shaped recipes the port may have merged into one: same kind, result (item, aspect, count) and shape. */
    private static boolean isTwin(Recipe a, Recipe b) {
        return a != b && a.output() != null && b.output() != null && a.kind().equals(b.kind()) && grid(a.kind())
                && family(a.kindKey()).equals(family(b.kindKey())) && a.output().resolved() != null
                && a.output().resolved().equals(b.output().resolved()) && Objects.equals(a.output().aspect(), b.output().aspect())
                && a.output().count() == b.output().count() && a.width() == b.width() && a.height() == b.height();
    }

    private static PortRecipe matchByOutput(Recipe r, List<PortRecipe> available) {
        PortRecipe best = null;
        int bestScore = -1;
        for (PortRecipe p : available) {
            if (!family(p.kind()).equals(family(r.kindKey()))) continue;
            if (r.output() == null) return p;                    // a custom recipe class: the class is the match
            if (r.output().resolved() == null || !r.output().resolved().equals(p.output())) continue;
            int score = (Objects.equals(r.output().aspect(), p.outputAspect()) ? 16 : 0)
                    + (r.name() != null && r.name().equals(p.name()) ? 8 : 0)
                    + (r.research() != null && r.research().equals(p.research()) ? 4 : 0)
                    + (r.output().count() == p.count() ? 2 : 0) + (r.width() == p.width() && r.height() == p.height() ? 1 : 0);
            if (score > bestScore) {
                best = p;
                bestScore = score;
            }
        }
        return best;
    }

    /** The index of a slot of a row-major grid after mirroring it left to right. */
    private static int mirrorIndex(int index, int width) {
        return width <= 1 ? index : (index / width) * width + (width - 1 - index % width);
    }

    private static Cmp compareRecipe(Recipe o, PortRecipe p, String s, List<Difference> out, boolean merged) {
        Cmp cmp = new Cmp();
        diff(out, "recipe", s, "kind", o.kindKey(), p.kind());
        if (o.output() == null) return cmp;
        if (!o.kind().equals("crafting_shaped") && !o.kind().equals("crafting_shapeless")) {
            diff(out, "recipe", s, "research", o.research(), p.research());
        }
        diff(out, "recipe", s, "output", o.output().resolved(), p.output());
        diff(out, "recipe", s, "output.aspect", o.output().aspect(), p.outputAspect());
        diff(out, "recipe", s, "count", o.output().count(), p.count());
        if (o.aspects() != null) diff(out, "recipe", s, "aspects", aspects(o.aspects()), aspects(p.aspects() == null ? Map.of() : p.aspects()));
        if (o.instability() != null) diff(out, "recipe", s, "instability", o.instability(), p.instability());
        switch (o.kind()) {
            case "arcane_shaped", "crafting_shaped" -> {
                diff(out, "recipe", s, "shape", o.width() + "x" + o.height(), p.width() + "x" + p.height());
                diff(out, "recipe", s, "ingredients.count", filled(o.cells()).size(), p.ingredientCount());
                compareGrid(o, p, s, out, merged, cmp);
            }
            case "crafting_shapeless" -> {
                diff(out, "recipe", s, "ingredients.count", o.components().size(), p.ingredientCount());
                cmp.add(compareMultiset(o.components(), p.components(), s, "ingredients", out));
            }
            case "infusion" -> {
                diff(out, "recipe", s, "ingredients.count", o.components().size(), p.components().size());
                cmp.add(compareCells(List.of(o.input()), p.cells(), s, "input", out, merged));
                cmp.add(compareMultiset(o.components(), p.components(), s, "components", out));
            }
            case "crucible" -> cmp.add(compareCells(List.of(o.catalyst()), p.cells(), s, "catalyst", out, merged));
            default -> out.add(new Difference("recipe", s, "kind", o.kind(), "unknown kind of the oracle"));
        }
        return cmp;
    }

    /** The slots of a shaped recipe, cell by cell over the full grid; the mirror image of the pattern is the same recipe. */
    private static void compareGrid(Recipe o, PortRecipe p, String s, List<Difference> out, boolean merged, Cmp cmp) {
        if (o.cells().size() != p.cells().size()) {
            out.add(new Difference("recipe", s, "grid", o.cells().size() + " slots (" + o.width() + "x" + o.height() + ")",
                    p.cells().size() + " slots (" + p.width() + "x" + p.height() + ")"));
            return;
        }
        List<Difference> straight = new ArrayList<>();
        Cmp straightCmp = compareCells(o.cells(), p.cells(), s, "ingredient", straight, merged);
        if (straight.isEmpty() || o.width() <= 1) {
            out.addAll(straight);
            cmp.add(straightCmp);
            return;
        }
        List<Item> mirror = new ArrayList<>(o.cells());
        for (int i = 0; i < mirror.size(); i++) mirror.set(mirrorIndex(i, o.width()), o.cells().get(i));
        List<Difference> flipped = new ArrayList<>();
        Cmp flippedCmp = compareCells(mirror, p.cells(), s, "ingredient", flipped, merged);
        if (flipped.isEmpty()) {
            cmp.add(flippedCmp);
            cmp.mirrored = true;
        } else {
            out.addAll(straight);
            cmp.add(straightCmp);
        }
    }

    private static String ids(List<String> candidates) {
        if (candidates.size() > 4) return String.join("|", candidates.subList(0, 4)) + "|... (" + candidates.size() + ")";
        return String.join("|", candidates);
    }

    /** Whether a slot of the port accepts every id the original's ingredient stood for. */
    private static List<String> lacking(List<String> expected, List<String> accepted) {
        List<String> missing = new ArrayList<>();
        for (String id : expected) if (!accepted.contains(id)) missing.add(id);
        return missing;
    }

    /**
     * Slot by slot: the original's ingredient ({@code null} is an empty slot) against what the port's slot accepts. The
     * slot has to accept every item the ingredient stood for; unless the recipe is a merge of two original ones it
     * must not accept more either. An ore name is an open set (the tag of the port holds whatever the loaded mods add) and
     * is only required to contain the items the name stands for.
     */
    private static Cmp compareCells(List<Item> original, List<List<String>> port, String subject, String what,
            List<Difference> out, boolean merged) {
        Cmp cmp = new Cmp();
        int n = Math.min(original.size(), port.size());
        for (int i = 0; i < n; i++) {
            Item item = original.get(i);
            List<String> accepted = port.get(i);
            String at = what + "[" + i + "]";
            if (item == null) {
                if (!accepted.isEmpty()) out.add(new Difference("recipe", subject, at, "empty", ids(accepted)));
                continue;
            }
            List<String> expected = item.expected();
            if (expected.isEmpty()) {
                cmp.notComparable++;
                continue;
            }
            if (accepted.isEmpty()) {
                out.add(new Difference("recipe", subject, at, ids(expected), "empty"));
                continue;
            }
            cmp.compared++;
            if (item.wildcard() && item.variants().size() > 1) cmp.wildcards++;
            List<String> missing = lacking(expected, accepted);
            if (!missing.isEmpty()) {
                out.add(new Difference("recipe", subject, at, ids(expected), ids(accepted) + " lacks " + missing));
            } else if (accepted.size() > expected.size()) {
                if (item.isOreName()) {
                    cmp.openOre++;
                } else if (merged) {
                    cmp.broader++;
                } else {
                    out.add(new Difference("recipe", subject, at + " (broader)", ids(expected), "accepts " + ids(accepted)));
                }
            } else if (item.isOreName()) {
                cmp.openOre++;
            }
        }
        return cmp;
    }

    /** The components of an infusion (or a shapeless recipe) are a multiset: each of the original must fit a distinct port slot. */
    private static Cmp compareMultiset(List<Item> original, List<List<String>> port, String subject, String what,
            List<Difference> out) {
        Cmp cmp = new Cmp();
        List<List<String>> free = new ArrayList<>(port);
        List<String> unmatched = new ArrayList<>();
        List<String> broader = new ArrayList<>();
        // the narrowest slot that fits first, so a broad slot is not spent on an ingredient a narrow one could take
        for (Item item : original) {
            List<String> expected = item.expected();
            if (expected.isEmpty()) {
                cmp.notComparable++;
                if (!free.isEmpty()) free.remove(free.size() - 1);      // an unresolved ingredient fits any slot; keep the others honest
                continue;
            }
            cmp.compared++;
            if (item.wildcard() && item.variants().size() > 1) cmp.wildcards++;
            int hit = -1;
            for (int i = 0; i < free.size(); i++) {
                if (lacking(expected, free.get(i)).isEmpty() && (hit < 0 || free.get(i).size() < free.get(hit).size())) hit = i;
            }
            if (hit < 0) {
                unmatched.add(String.valueOf(expected));
            } else {
                if (free.get(hit).size() > expected.size()) {
                    if (item.isOreName()) cmp.openOre++;
                    else broader.add(ids(expected) + " in " + ids(free.get(hit)));
                } else if (item.isOreName()) {
                    cmp.openOre++;
                }
                free.remove(hit);
            }
        }
        if (!unmatched.isEmpty()) {
            Map<String, Integer> expected = new TreeMap<>();
            for (Item item : original) expected.merge(String.valueOf(item.expected().isEmpty() ? item.resolved() : item.expected()), 1, Integer::sum);
            List<String> got = new ArrayList<>();
            for (List<String> slot : port) got.add(ids(slot));
            out.add(new Difference("recipe", subject, what, expected.toString(), got.stream().sorted().toList().toString()
                    + " cannot supply " + unmatched));
        }
        if (!broader.isEmpty()) {
            out.add(new Difference("recipe", subject, what + " (broader)", "exactly the original's items", "accepts more: " + broader));
        }
        return cmp;
    }
}
