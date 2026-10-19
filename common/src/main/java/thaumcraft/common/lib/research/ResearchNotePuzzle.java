package thaumcraft.common.lib.research;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.utils.HexUtils.Hex;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** TC4 research-note connectivity, without inventory or player-state effects. */
public final class ResearchNotePuzzle {
    private ResearchNotePuzzle() {}

    /**
     * Returns the same note when already solved or when any fixed endpoint is
     * unreachable. Success returns a solved copy retaining the connected occupied
     * component and every fixed endpoint, in the input cell order.
     */
    public static ResearchNoteData solve(ResearchNoteData note, Set<String> discoveredAspects) {
        Objects.requireNonNull(note, "note");
        Objects.requireNonNull(discoveredAspects, "discoveredAspects");
        if (note.complete()) return note;

        Cell start = null;
        for (Cell cell : note.cells()) {
            if (cell.type() == 1) {
                start = cell;
                break;
            }
        }
        // The native data constructor rejects zero endpoints. Keep this guard so
        // a future relaxed reader cannot reproduce the original free completion.
        if (start == null) return note;

        Set<String> connected = new HashSet<>();
        visit(note, start, discoveredAspects, connected);
        for (Cell cell : note.cells()) {
            if (cell.type() == 1 && !connected.contains(cell.coordinate())) return note;
        }

        // ResearchManager.checkResearchCompletion (TC4 4.2.3.5:221-248) keeps
        // branches in the reachable component too, rather than a minimal path.
        List<Cell> remaining = note.cells().stream()
                .filter(cell -> cell.type() == 1 || connected.contains(cell.coordinate()))
                .toList();
        return note.withCells(remaining, true);
    }

    /** Immediate component in either direction; equality and shared ancestors do not link. */
    public static boolean isDirectlyConnected(String firstTag, String secondTag) {
        if (firstTag == null || secondTag == null) return false;
        Aspect first = Aspect.getAspect(firstTag);
        Aspect second = Aspect.getAspect(secondTag);
        return first != null && second != null && first != second
                && (hasComponent(first, second) || hasComponent(second, first));
    }

    private static boolean hasComponent(Aspect compound, Aspect component) {
        Aspect[] components = compound.getComponents();
        return components != null && (components[0] == component || components[1] == component);
    }

    private static void visit(ResearchNoteData note, Cell source, Set<String> discovered, Set<String> connected) {
        connected.add(source.coordinate());
        Hex position = new Hex(source.q(), source.r());
        // ResearchManager.checkConnections (TC4 4.2.3.5:251-266): directions
        // 0..5, occupied neighbours, both aspects discovered, direct components.
        for (int direction = 0; direction < 6; direction++) {
            Hex neighbour = position.getNeighbour(direction);
            Cell target = note.cellAt(neighbour.q, neighbour.r);
            if (target == null || target.type() < 1 || connected.contains(target.coordinate())) continue;
            if (!discovered.contains(source.aspect()) || !discovered.contains(target.aspect())) continue;
            if (!isDirectlyConnected(source.aspect(), target.aspect())) continue;
            visit(note, target, discovered, connected);
        }
    }
}
