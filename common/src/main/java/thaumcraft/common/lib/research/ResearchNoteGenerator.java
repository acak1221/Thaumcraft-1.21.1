package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.common.lib.utils.HexUtils.Hex;

/**
 * Original TC4 ResearchManager.createNote geometry and RNG stream, without item,
 * inventory or world effects. Generates a puzzle; it does not grant research.
 */
public final class ResearchNoteGenerator {
    private ResearchNoteGenerator() {}

    public static ResearchNoteData generate(String key, Random random) {
        ResearchDefinition research = key == null ? null : ResearchCatalog.get(key);
        if (research == null) throw new IllegalArgumentException("Unknown research: " + key);
        return generate(research, random);
    }

    public static ResearchNoteData generate(ResearchDefinition research, Random random) {
        Objects.requireNonNull(research, "research");
        Objects.requireNonNull(random, "random");
        ResearchDefinition canonical = research.key() == null ? null : ResearchCatalog.get(research.key());
        if (canonical == null || canonical.aspectCost().isEmpty()) {
            throw new IllegalArgumentException("Unknown or aspectless research note key: " + research.key());
        }
        // Every aspectful canonical definition has complexity 1..3. Reject
        // unsupported addon input rather than preserve createNote's potential
        // impossible-grid/retry behavior outside that original constructor range.
        int complexity = research.complexity();
        if (complexity < 1 || complexity > 3 || research.aspectCost().isEmpty()) {
            throw new IllegalArgumentException("Research note needs aspect costs and complexity 1..3");
        }
        int radius = 1 + complexity;
        if (research.aspectCost().size() > radius * 6) {
            throw new IllegalArgumentException("Too many fixed aspects for the research ring");
        }
        List<String> aspects = new ArrayList<>();
        Aspect primary = null;
        int highest = 0;
        for (var cost : research.aspectCost().entrySet()) {
            Aspect aspect = Aspect.getAspect(cost.getKey());
            if (aspect == null || cost.getValue() == null || cost.getValue() <= 0) {
                throw new IllegalArgumentException("Invalid research aspect cost: " + cost.getKey());
            }
            aspects.add(cost.getKey());
            // ResearchItem.getResearchPrimaryTag: strict > retains the first tie.
            if (cost.getValue() > highest) {
                primary = aspect;
                highest = cost.getValue();
            }
        }
        checkCancellation();
        HashMap<String, Hex> locations = originalHexes(radius);
        // The helper preserves the discarded random start draw and float spacing.
        List<Hex> ring = HexUtils.distributeRingRandomly(radius, aspects.size(), random);
        HashMap<String, Hex> hexes = new HashMap<>();
        HashMap<String, Cell> entries = new HashMap<>();
        // Do not use new HashMap<>(locations): its pre-sized table differs from
        // the original default constructor plus sequential puts on collisions.
        for (Hex hex : locations.values()) {
            hexes.put(hex.toString(), hex);
            entries.put(hex.toString(), new Cell(hex.q, hex.r, 0, null));
        }
        for (int index = 0; index < ring.size(); index++) {
            Hex hex = ring.get(index);
            hexes.put(hex.toString(), hex);
            entries.put(hex.toString(), new Cell(hex.q, hex.r, 1, aspects.get(index)));
        }
        if (complexity > 1) {
            int blanks = complexity * 2;
            Hex[] candidates = hexes.values().toArray(Hex[]::new);
            while (blanks > 0) {
                // Cancellation is observable only when explicitly requested. No
                // arbitrary retry limit/shuffle changes canonical RNG vectors.
                checkCancellation();
                int selected = random.nextInt(candidates.length);
                if (selected < 0 || selected >= candidates.length) {
                    throw new IllegalArgumentException("Random returned an out-of-range research cell");
                }
                Hex candidate = candidates[selected];
                if (entries.get(candidate.toString()).type() != 0) continue;
                boolean allowed = true;
                for (int direction = 0; direction < 6; direction++) {
                    Hex neighbour = candidate.getNeighbour(direction);
                    Cell endpoint = entries.get(neighbour.toString());
                    if (endpoint == null || endpoint.type() != 1) continue;
                    int connections = 0;
                    for (int next = 0; next < 6; next++) {
                        if (hexes.containsKey(neighbour.getNeighbour(next).toString())) connections++;
                        if (connections >= 2) break;
                    }
                    // The candidate still counts here, as in the original.
                    if (connections < 2) { allowed = false; break; }
                }
                if (!allowed) continue;
                hexes.remove(candidate.toString());
                entries.remove(candidate.toString());
                candidates = hexes.values().toArray(Hex[]::new);
                blanks--;
            }
        }
        List<Cell> cells = new ArrayList<>();
        for (Hex hex : hexes.values()) cells.add(entries.get(hex.toString()));
        return new ResearchNoteData(research.key(), primary.getColor(), false, 0, cells);
    }

    /** Original HexUtils.generateHexes, including default HashMap insertion. */
    private static HashMap<String, Hex> originalHexes(int radius) {
        HashMap<String, Hex> locations = new HashMap<>();
        Hex current = new Hex(0, 0);
        locations.put(current.toString(), current);
        for (int ring = 0; ring < radius; ring++) {
            current = current.getNeighbour(4);
            Hex cursor = new Hex(current.q, current.r);
            for (int direction = 0; direction < 6; direction++) {
                for (int step = 0; step <= ring; step++) {
                    locations.put(cursor.toString(), cursor);
                    cursor = cursor.getNeighbour(direction);
                }
            }
        }
        return locations;
    }

    private static void checkCancellation() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Research note generation cancelled");
    }
}
