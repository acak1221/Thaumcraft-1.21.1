package thaumic.tinkerer.testing.parity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * that subject, field, original value and port value; an allow-list entry that matches nothing is reported too, so the
 * list cannot go stale.</p>
 */
public final class ParityComparator {
    /** The research a Kami entry requires: every research of these tabs of Thaumcraft and the add-on (KamiResearchItem.setPages). */
    public static final Set<String> KAMI_TABS = Set.of("THAUMICTINKERER", "BASICS", "GOLEMANCY", "ARTIFICE", "ALCHEMY", "THAUMATURGY");
    /** KamiResearchItem.Blacklist and the "KAMI" suffix rule of the original. */
    public static final String KAMI_BLACKLIST_PREFIX = "MINILITH";
    public static final String KAMI_SUFFIX = "KAMI";

    private ParityComparator() {}

    /** The truth of a condition of the original in the running port. */
    public interface Environment {
        boolean value(Condition condition);
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
        public final List<KnownDifference> unusedKnown = new ArrayList<>();
        public final Map<String, Integer> counts = new LinkedHashMap<>();

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
            for (KnownDifference k : unusedKnown) sb.append("  STALE ").append(k).append(" (matched nothing)\n");
            return sb.toString();
        }
    }

    public static Result compare(OracleData oracle, PortSnapshot port, Environment env, List<KnownDifference> known) {
        List<Difference> all = new ArrayList<>();
        Result result = new Result();
        Recipes recipes = new Recipes(oracle, env);
        compareResearch(oracle, port, env, recipes, all, result.counts);
        compareRecipes(oracle, port, env, recipes, all, result.counts);
        boolean[] used = new boolean[known.size()];
        for (Difference d : all) {
            int hit = -1;
            for (int i = 0; i < known.size(); i++) {
                if (known.get(i).matches(d)) {
                    hit = i;
                    break;
                }
            }
            if (hit >= 0) {
                used[hit] = true;
                result.allowed.add(d);
            } else {
                result.failures.add(d);
            }
        }
        for (int i = 0; i < known.size(); i++) if (!used[i]) result.unusedKnown.add(known.get(i));
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

    private static String stack(String id, int count) {
        return id + " x" + count;
    }

    // ------------------------------------------------------------------------------------------ recipes by name

    /** The recipes of the original by name; a research page shows the last one registered under its name. */
    private static final class Recipes {
        private final Map<String, Recipe> lastByName = new HashMap<>();
        private final Map<String, Integer> countByName = new HashMap<>();

        Recipes(OracleData oracle, Environment env) {
            for (Recipe r : oracle.recipes()) {
                if (r.name() == null || !r.registered() || !holds(r.when(), env)) continue;
                countByName.merge(r.name(), 1, Integer::sum);
                Recipe seen = lastByName.get(r.name());
                if (seen == null || seen.order() < r.order()) lastByName.put(r.name(), r);
            }
        }

        String shown(String name) {
            Recipe r = lastByName.get(name);
            return r == null ? "?" + name : stack(String.valueOf(r.output().resolved()), r.output().count());
        }

        boolean shadowed(String name) {
            return name != null && countByName.getOrDefault(name, 0) > 1;
        }
    }

    // ------------------------------------------------------------------------------------------ research

    private static void compareResearch(OracleData oracle, PortSnapshot port, Environment env, Recipes recipes,
            List<Difference> out, Map<String, Integer> counts) {
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
                if (p != null) diff(out, "research", key, "presence", whyNot(e.getValue()), "registered");
            } else if (p == null) {
                diff(out, "research", key, "presence", "registered", "missing");
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
        if (o.iconResolved() != null) diff(out, "research", k, "icon", o.iconResolved(), p.icon());
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
        Set<String> expected = new LinkedHashSet<>(explicit);
        for (PoolEntry e : port.researchPool()) {
            if (e.lost() || e.virtual() || !KAMI_TABS.contains(e.category()) || e.key().startsWith(KAMI_BLACKLIST_PREFIX)
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
            String original = list(explicit) + " + every other research of " + KAMI_TABS.stream().sorted().toList() + " ("
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
        if (kind.startsWith("arcane")) return "arcane";
        if (kind.startsWith("crafting")) return "crafting";
        return kind;
    }

    private static String subject(Recipe r, Recipes recipes) {
        String out = String.valueOf(r.output().resolved());
        if (r.name() == null) return "<unnamed> -> " + out;
        return recipes.shadowed(r.name()) ? r.name() + " -> " + out : r.name();
    }

    private static String describe(Recipe r) {
        return r.kind() + " -> " + stack(String.valueOf(r.output().resolved()), r.output().count());
    }

    private static String describe(PortRecipe p) {
        return p.kind() + " -> " + stack(p.output(), p.count());
    }

    private static void compareRecipes(OracleData oracle, PortSnapshot port, Environment env, Recipes recipes,
            List<Difference> out, Map<String, Integer> counts) {
        List<PortRecipe> available = new ArrayList<>(port.recipes());
        int compared = 0;
        int ingredients = 0;
        int skipped = 0;
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
                if (family(p.kind()).equals(family(r.kind())) && r.name().equals(p.name())) {
                    matched.put(r, p);
                    available.remove(p);
                    break;
                }
            }
        }
        for (Recipe r : ordered) {
            String subject = subject(r, recipes);
            PortRecipe m = matched.get(r);
            if (m == null) {
                diff(out, "recipe", subject, "presence", describe(r), "missing");
                continue;
            }
            compared++;
            int[] ing = compareRecipe(r, m, subject, out);
            ingredients += ing[0];
            skipped += ing[1];
        }
        Set<String> oracleNames = new LinkedHashSet<>();
        for (Recipe r : oracle.recipes()) if (r.name() != null) oracleNames.add(r.name());
        for (PortRecipe p : available) {
            String why = p.name() != null && oracleNames.contains(p.name()) ? "registered by the original only when "
                    + oracle.recipes().stream().filter(r -> p.name().equals(r.name())).map(r -> when(r.when())).distinct().toList()
                    : "not in the original";
            boolean neverRegistered = oracle.recipes().stream().anyMatch(r -> !r.registered() && family(r.kind()).equals(family(p.kind()))
                    && p.output().equals(r.output().resolved()));
            diff(out, "recipe", p.name() != null ? p.name() + " -> " + p.output() : p.id(), "presence",
                    neverRegistered ? "never registered (its owner's shouldRegister() is false)" : why, describe(p));
        }
        counts.put("recipes expected", ordered.size());
        counts.put("recipes compared", compared);
        counts.put("recipes in the port", port.recipes().size());
        counts.put("ingredients compared", ingredients);
        counts.put("ingredients not comparable (ore names)", skipped);
    }

    private static PortRecipe matchByOutput(Recipe r, List<PortRecipe> available) {
        PortRecipe best = null;
        int bestScore = -1;
        for (PortRecipe p : available) {
            if (!family(p.kind()).equals(family(r.kind())) || r.output().resolved() == null
                    || !r.output().resolved().equals(p.output())) continue;
            int score = (r.name() != null && r.name().equals(p.name()) ? 8 : 0)
                    + (r.research() != null && r.research().equals(p.research()) ? 4 : 0)
                    + (r.output().count() == p.count() ? 2 : 0) + (r.width() == p.width() && r.height() == p.height() ? 1 : 0);
            if (score > bestScore) {
                best = p;
                bestScore = score;
            }
        }
        return best;
    }

    /** Returns {ingredients compared, ingredients not comparable}. */
    private static int[] compareRecipe(Recipe o, PortRecipe p, String s, List<Difference> out) {
        diff(out, "recipe", s, "kind", o.kind(), p.kind());
        if (!o.kind().equals("crafting_shaped") && !o.kind().equals("crafting_shapeless")) {
            diff(out, "recipe", s, "research", o.research(), p.research());
        }
        diff(out, "recipe", s, "output", o.output().resolved(), p.output());
        diff(out, "recipe", s, "count", o.output().count(), p.count());
        if (o.aspects() != null) diff(out, "recipe", s, "aspects", aspects(o.aspects()), aspects(p.aspects() == null ? Map.of() : p.aspects()));
        if (o.instability() != null) diff(out, "recipe", s, "instability", o.instability(), p.instability());
        int compared = 0;
        int skipped = 0;
        switch (o.kind()) {
            case "arcane_shaped", "crafting_shaped" -> {
                diff(out, "recipe", s, "shape", o.width() + "x" + o.height(), p.width() + "x" + p.height());
                diff(out, "recipe", s, "ingredients.count", o.cells().size(), p.ingredientCount());
                int[] r = compareCells(o.cells(), p.cells(), s, "ingredient", out);
                compared += r[0];
                skipped += r[1];
            }
            case "crafting_shapeless" -> {
                diff(out, "recipe", s, "ingredients.count", o.components().size(), p.ingredientCount());
                int[] r = compareMultiset(o.components(), p.components(), s, "ingredients", out);
                compared += r[0];
                skipped += r[1];
            }
            case "infusion" -> {
                diff(out, "recipe", s, "ingredients.count", o.components().size(), p.components().size());
                int[] c = compareCells(List.of(o.input()), p.cells(), s, "input", out);
                int[] r = compareMultiset(o.components(), p.components(), s, "components", out);
                compared += c[0] + r[0];
                skipped += c[1] + r[1];
            }
            case "crucible" -> {
                int[] c = compareCells(List.of(o.catalyst()), p.cells(), s, "catalyst", out);
                compared += c[0];
                skipped += c[1];
            }
            default -> out.add(new Difference("recipe", s, "kind", o.kind(), "unknown kind of the oracle"));
        }
        return new int[] { compared, skipped };
    }

    private static String ids(List<String> candidates) {
        if (candidates.size() > 4) return String.join("|", candidates.subList(0, 4)) + "|... (" + candidates.size() + ")";
        return String.join("|", candidates);
    }

    private static int[] compareCells(List<Item> original, List<List<String>> port, String subject, String what,
            List<Difference> out) {
        int compared = 0;
        int skipped = 0;
        int n = Math.min(original.size(), port.size());
        for (int i = 0; i < n; i++) {
            Item item = original.get(i);
            if (item.resolved() == null || item.isOreName()) {
                skipped++;
                continue;
            }
            compared++;
            if (!port.get(i).contains(item.resolved())) {
                out.add(new Difference("recipe", subject, what + "[" + i + "]", item.resolved(), ids(port.get(i))));
            }
        }
        return new int[] { compared, skipped };
    }

    /** The components of an infusion (or a shapeless recipe) are a multiset: each of the original must fit a distinct port slot. */
    private static int[] compareMultiset(List<Item> original, List<List<String>> port, String subject, String what,
            List<Difference> out) {
        List<List<String>> free = new ArrayList<>(port);
        List<String> unmatched = new ArrayList<>();
        int compared = 0;
        int skipped = 0;
        for (Item item : original) {
            if (item.resolved() == null || item.isOreName()) {
                skipped++;
                if (!free.isEmpty()) free.remove(free.size() - 1);      // an ore name fits any slot; keep the others honest
                continue;
            }
            compared++;
            int hit = -1;
            for (int i = 0; i < free.size(); i++) {
                if (free.get(i).contains(item.resolved())) {
                    hit = i;
                    break;
                }
            }
            if (hit < 0) unmatched.add(item.resolved());
            else free.remove(hit);
        }
        if (!unmatched.isEmpty()) {
            Map<String, Integer> expected = new TreeMap<>();
            for (Item item : original) expected.merge(String.valueOf(item.resolved()), 1, Integer::sum);
            List<String> got = new ArrayList<>();
            for (List<String> slot : port) got.add(ids(slot));
            out.add(new Difference("recipe", subject, what, expected.toString(), got.stream().sorted().toList().toString()
                    + " cannot supply " + unmatched));
        }
        return new int[] { compared, skipped };
    }
}
