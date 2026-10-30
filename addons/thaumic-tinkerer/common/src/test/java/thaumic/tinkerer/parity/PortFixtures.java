package thaumic.tinkerer.parity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import thaumic.tinkerer.testing.parity.KnownDifference;
import thaumic.tinkerer.testing.parity.OracleData;
import thaumic.tinkerer.testing.parity.OracleData.Item;
import thaumic.tinkerer.testing.parity.OracleData.Page;
import thaumic.tinkerer.testing.parity.OracleData.Recipe;
import thaumic.tinkerer.testing.parity.OracleData.Research;
import thaumic.tinkerer.testing.parity.ParityComparator;
import thaumic.tinkerer.testing.parity.PortSnapshot;
import thaumic.tinkerer.testing.parity.PortSnapshot.PoolEntry;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortPage;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortRecipe;
import thaumic.tinkerer.testing.parity.PortSnapshot.PortResearch;

/**
 * A port that is exactly what the oracle says, for the tests of the comparison logic. Test-only code of the port
 * (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 */
final class PortFixtures {
    private PortFixtures() {}

    /** The environment of the running port: Kami, fire, the survival share tome and mirrors on, no other mod. */
    static final ParityComparator.Environment PORT = c -> c.kind().equals("config");

    /**
     * The one documented difference every comparison with the real oracle reports, because the port requires for every Kami
     * entry the union of the research (the entry of known_differences.json), so a port that equals the oracle still has it.
     */
    static final List<KnownDifference> KAMI = KnownDifference.fromClasspath().stream()
            .filter(k -> k.key().equals("<Kami entries>")).toList();

    /** The entry about the Kami requirements and some more. */
    static List<KnownDifference> withKami(KnownDifference... more) {
        List<KnownDifference> all = new ArrayList<>(KAMI);
        all.addAll(List.of(more));
        return all;
    }

    /** Some research of Thaumcraft's own tabs, which a Kami entry requires too. */
    static final List<PoolEntry> THAUMCRAFT_POOL = List.of(new PoolEntry("INFUSION", "ALCHEMY", false, false),
            new PoolEntry("NITOR", "ALCHEMY", false, false), new PoolEntry("GOGGLES", "BASICS", false, false),
            new PoolEntry("MINILITHX", "ARTIFICE", false, false), new PoolEntry("SOMEKAMI", "ARTIFICE", false, false),
            new PoolEntry("LOSTONE", "BASICS", true, false), new PoolEntry("VIRTUALONE", "BASICS", false, true),
            new PoolEntry("ELDRITCH_MINOR", "ELDRITCH", false, false));

    static boolean applies(boolean registered, List<OracleData.Condition> when) {
        return registered && ParityComparator.holdsForTest(when, PORT);
    }

    /** The recipes by name, the last registered one winning (what a research page shows), whatever their conditions. */
    static Map<String, Recipe> lastByName(OracleData oracle) {
        Map<String, Recipe> lastByName = new LinkedHashMap<>();
        for (Recipe r : oracle.recipes()) {
            if (r.name() == null || r.output() == null) continue;
            Recipe seen = lastByName.get(r.name());
            if (seen == null || seen.order() < r.order()) lastByName.put(r.name(), r);
        }
        return lastByName;
    }

    static List<PoolEntry> pool(OracleData oracle) {
        List<PoolEntry> pool = new ArrayList<>(THAUMCRAFT_POOL);
        for (Research r : oracle.research()) {
            if (applies(r.registered(), r.when()) && !r.kami()) pool.add(new PoolEntry(r.key(), r.category(), false, false));
        }
        return pool;
    }

    static PortSnapshot fromOracle(OracleData oracle) {
        Map<String, Recipe> lastByName = lastByName(oracle);
        List<PoolEntry> pool = pool(oracle);
        Map<String, PortResearch> research = new LinkedHashMap<>();
        for (Research r : oracle.research()) {
            if (applies(r.registered(), r.when())) research.put(r.key(), portResearch(r, pool, lastByName));
        }
        List<PortRecipe> recipes = new ArrayList<>();
        for (Recipe r : oracle.recipes()) {
            if (applies(r.registered(), r.when())) recipes.add(portRecipe(r));
        }
        return new PortSnapshot(research, pool, recipes, objectTags(oracle));
    }

    /**
     * The port in the state the deviation texts describe: the registrations that exist without ComputerCraft (Aspectalyzer,
     * Golem Connector, Peripherals root) and the Remote Placer (dead in the original) at its moved cell, the two
     * quartz recipes that the port merged, and the pool of the Kami requirements that follows from it. Compared with the real
     * allow-list it has to pass, with every entry used.
     */
    static PortSnapshot documentedPort(OracleData oracle) {
        Map<String, Recipe> lastByName = lastByName(oracle);
        List<Research> expected = new ArrayList<>();
        for (Research r : oracle.research()) if (applies(r.registered(), r.when())) expected.add(r);
        Map<String, Research> added = new LinkedHashMap<>();
        for (Research r : oracle.research()) {
            if (!applies(r.registered(), r.when()) && !r.kami() && Set.of("ASPECT_ANALYZER", "GOLEM_CONNECTOR", "PERIPHERALS", "REMOTE_PLACER").contains(r.key())) {
                added.put(r.key(), r);
            }
        }
        List<PoolEntry> pool = new ArrayList<>(THAUMCRAFT_POOL);
        for (Research r : expected) if (!r.kami()) pool.add(new PoolEntry(r.key(), r.category(), false, false));
        for (Research r : added.values()) pool.add(new PoolEntry(r.key(), r.category(), false, false));
        Map<String, PortResearch> research = new LinkedHashMap<>();
        for (Research r : expected) research.put(r.key(), portResearch(r, pool, lastByName));
        for (Research r : added.values()) {
            PortResearch p = portResearch(r, pool, lastByName);
            if (r.key().equals("REMOTE_PLACER")) {
                p = new PortResearch(p.key(), p.category(), p.aspects(), -8, p.row(), p.complexity(), p.parents(), p.parentsHidden(),
                        p.siblings(), p.flags(), p.warp(), p.pages(), p.icon(), p.iconAspect());
            }
            research.put(r.key(), p);
        }
        List<PortRecipe> recipes = new ArrayList<>();
        PortRecipe quartz = null;
        PortRecipe stairs = null;
        for (Recipe r : oracle.recipes()) {
            boolean applicable = applies(r.registered(), r.when());
            if (!applicable && !Set.of("ASPECT_ANALYZER", "GOLEM_CONNECTOR", "REMOTE_PLACER").contains(r.name())) continue;
            if (r.output() == null) {
                if (applicable) recipes.add(portRecipe(r));
                continue;
            }
            PortRecipe p = portRecipe(r);
            String result = String.valueOf(r.output().resolved());
            if (result.equals("thaumictinkerer:dark_quartz_item")) {
                if (quartz != null) continue;                               // the charcoal recipe: merged into the first
                quartz = withSlot(withId(p, "thaumictinkerer:dark_quartz_0"), false, 4, List.of("minecraft:coal", "minecraft:charcoal"));
                recipes.add(quartz);
            } else if (result.equals("thaumictinkerer:dark_quartz_stairs")) {
                if (stairs != null) continue;                               // the left staircase: merged into the right one
                stairs = withId(p, "thaumictinkerer:dark_quartz_5");
                recipes.add(stairs);
            } else {
                recipes.add(p);
            }
        }
        return new PortSnapshot(research, pool, recipes, objectTags(oracle));
    }

    static PortRecipe withId(PortRecipe p, String id) {
        return new PortRecipe(id, p.name(), p.kind(), p.research(), p.output(), p.count(), p.aspects(), p.instability(), p.width(), p.height(),
                p.cells(), p.components(), p.outputAspect());
    }

    /** The object tags as the port would register them. */
    static Map<String, Map<String, Integer>> objectTags(OracleData oracle) {
        Map<String, Map<String, Integer>> tags = new LinkedHashMap<>();
        for (OracleData.ObjectTag t : oracle.objectTags()) tags.put(t.item().resolved(), new LinkedHashMap<>(t.aspects()));
        return tags;
    }

    /** The research as the port would register it, whatever the conditions of the original say. */
    static PortResearch portResearch(Research r, List<PoolEntry> pool, Map<String, Recipe> lastByName) {
        List<String> hidden = new ArrayList<>(r.parentsHidden());
        if (r.parentsHiddenAuto()) {
            for (PoolEntry e : pool) {
                boolean skipped = e.lost() || e.virtual() || !ParityComparator.kamiTabs(r.category()).contains(e.category())
                        || e.key().startsWith(ParityComparator.KAMI_BLACKLIST_PREFIX)
                        || e.key().endsWith(ParityComparator.KAMI_SUFFIX);
                if (!skipped && !hidden.contains(e.key())) hidden.add(e.key());
            }
        }
        List<PortPage> pages = new ArrayList<>();
        for (Page p : r.pages()) {
            List<String> shown = new ArrayList<>();
            for (String name : p.recipes() == null ? List.<String>of() : p.recipes()) {
                Recipe target = lastByName.get(name);
                shown.add(ParityComparator.stackText(target.output().resolved(), target.output().aspect(), target.output().count()));
            }
            pages.add(new PortPage(p.kind(), p.key(), shown));
        }
        return new PortResearch(r.key(), r.category(), new LinkedHashMap<>(r.aspects()), r.column(), r.row(), r.complexity(),
                new ArrayList<>(r.parents()), hidden, new ArrayList<>(r.siblings()), new LinkedHashMap<>(r.flags()), r.warp(), pages,
                r.iconResolved(), r.iconAspect());
    }

    /** The recipe as the port would register it: every slot accepts exactly what the original's ingredient stood for. */
    static PortRecipe portRecipe(Recipe r) {
        List<List<String>> cells = new ArrayList<>();
        List<List<String>> components = new ArrayList<>();
        if (r.output() == null) {
            return new PortRecipe("tt:" + r.order(), null, r.kindKey(), null, "minecraft:air", 0, null, null, 0, 0, List.of(), List.of());
        }
        switch (r.kind()) {
            case "arcane_shaped", "crafting_shaped" -> r.cells().forEach(c -> cells.add(c == null ? List.of() : candidates(c)));
            case "infusion" -> {
                cells.add(candidates(r.input()));
                r.components().forEach(c -> components.add(candidates(c)));
            }
            case "crucible" -> cells.add(candidates(r.catalyst()));
            default -> r.components().forEach(c -> components.add(candidates(c)));
        }
        return new PortRecipe("tt:" + r.order(), r.kind().startsWith("crafting") ? null : r.name(), r.kind(), r.research(),
                r.output().resolved(), r.output().count(), r.aspects() == null ? null : new LinkedHashMap<>(r.aspects()),
                r.instability(), r.width(), r.height(), cells, components, r.output().aspect());
    }

    private static List<String> candidates(Item item) {
        return item.expected().isEmpty() ? List.of("any") : new ArrayList<>(item.expected());
    }

    /** The same recipe with another list of ids in one slot (a grid slot, or a component when {@code component}). */
    static PortRecipe withSlot(PortRecipe p, boolean component, int index, List<String> ids) {
        List<List<String>> cells = new ArrayList<>(p.cells());
        List<List<String>> components = new ArrayList<>(p.components());
        (component ? components : cells).set(index, ids);
        return new PortRecipe(p.id(), p.name(), p.kind(), p.research(), p.output(), p.count(), p.aspects(), p.instability(), p.width(),
                p.height(), cells, components, p.outputAspect());
    }

    /** The same snapshot with the recipe at {@code index} replaced. */
    static PortSnapshot withRecipe(PortSnapshot port, PortRecipe before, PortRecipe after) {
        List<PortRecipe> recipes = new ArrayList<>(port.recipes());
        recipes.set(recipes.indexOf(before), after);
        return port.withRecipes(recipes);
    }
}
