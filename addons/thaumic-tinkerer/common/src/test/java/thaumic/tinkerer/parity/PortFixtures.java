package thaumic.tinkerer.parity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import thaumic.tinkerer.testing.parity.OracleData;
import thaumic.tinkerer.testing.parity.OracleData.Condition;
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

    /** Some research of Thaumcraft's own tabs, which a Kami entry requires too. */
    static final List<PoolEntry> THAUMCRAFT_POOL = List.of(new PoolEntry("INFUSION", "ALCHEMY", false, false),
            new PoolEntry("NITOR", "ALCHEMY", false, false), new PoolEntry("GOGGLES", "BASICS", false, false),
            new PoolEntry("MINILITHX", "ARTIFICE", false, false), new PoolEntry("SOMEKAMI", "ARTIFICE", false, false),
            new PoolEntry("LOSTONE", "BASICS", true, false), new PoolEntry("VIRTUALONE", "BASICS", false, true),
            new PoolEntry("ELDRITCH_MINOR", "ELDRITCH", false, false));

    static PortSnapshot fromOracle(OracleData oracle) {
        Map<String, Recipe> lastByName = new LinkedHashMap<>();
        for (Recipe r : oracle.recipes()) {
            if (r.name() != null && r.registered() && ParityComparator.holdsForTest(r.when(), PORT)) {
                Recipe seen = lastByName.get(r.name());
                if (seen == null || seen.order() < r.order()) lastByName.put(r.name(), r);
            }
        }
        Map<String, PortResearch> research = new LinkedHashMap<>();
        List<PoolEntry> pool = new ArrayList<>(THAUMCRAFT_POOL);
        for (Research r : oracle.research()) {
            if (r.registered() && ParityComparator.holdsForTest(r.when(), PORT) && !r.kami()) {
                pool.add(new PoolEntry(r.key(), r.category(), false, false));
            }
        }
        for (Research r : oracle.research()) {
            if (!r.registered() || !ParityComparator.holdsForTest(r.when(), PORT)) continue;
            List<String> hidden = new ArrayList<>(r.parentsHidden());
            if (r.parentsHiddenAuto()) {
                for (PoolEntry e : pool) {
                    boolean skipped = e.lost() || e.virtual() || !ParityComparator.KAMI_TABS.contains(e.category())
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
                    shown.add(target.output().resolved() + " x" + target.output().count());
                }
                pages.add(new PortPage(p.kind(), p.key(), shown));
            }
            research.put(r.key(), new PortResearch(r.key(), r.category(), new LinkedHashMap<>(r.aspects()), r.column(), r.row(),
                    r.complexity(), new ArrayList<>(r.parents()), hidden, new ArrayList<>(r.siblings()),
                    new LinkedHashMap<>(r.flags()), r.warp(), pages, r.iconResolved()));
        }
        List<PortRecipe> recipes = new ArrayList<>();
        for (Recipe r : oracle.recipes()) {
            if (!r.registered() || !ParityComparator.holdsForTest(r.when(), PORT)) continue;
            List<List<String>> cells = new ArrayList<>();
            List<List<String>> components = new ArrayList<>();
            switch (r.kind()) {
                case "arcane_shaped", "crafting_shaped" -> r.cells().forEach(c -> cells.add(candidates(c)));
                case "infusion" -> {
                    cells.add(candidates(r.input()));
                    r.components().forEach(c -> components.add(candidates(c)));
                }
                case "crucible" -> cells.add(candidates(r.catalyst()));
                default -> r.components().forEach(c -> components.add(candidates(c)));
            }
            recipes.add(new PortRecipe("tt:" + r.order(), r.kind().startsWith("crafting") ? null : r.name(), r.kind(),
                    r.research(), r.output().resolved(), r.output().count(),
                    r.aspects() == null ? null : new LinkedHashMap<>(r.aspects()), r.instability(), r.width(), r.height(), cells,
                    components));
        }
        return new PortSnapshot(research, pool, recipes);
    }

    private static List<String> candidates(Item item) {
        return item.resolved() == null ? List.of("any") : List.of(item.resolved());
    }
}
