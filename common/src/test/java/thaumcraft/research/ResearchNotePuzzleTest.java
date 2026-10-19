package thaumcraft.research;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.research.ResearchNotePuzzle;
import thaumcraft.common.lib.utils.HexUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResearchNotePuzzleTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void linksRequireAnImmediateComponentInEitherDirection() {
        assertTrue(ResearchNotePuzzle.isDirectlyConnected("aer", "motus"));
        assertTrue(ResearchNotePuzzle.isDirectlyConnected("motus", "aer"));
        assertTrue(ResearchNotePuzzle.isDirectlyConnected("potentia", "praecantatio"));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("aer", "aer"));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("motus", "motus"));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("motus", "lux")); // Shared aer.
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("ordo", "volatus")); // Via motus only.
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("AER", "motus"));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("unknown", "motus"));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected(null, "motus"));
        assertFalse(ResearchNotePuzzle.isDirectlyConnected("motus", null));
    }

    @Test void originalFireFocusEndpointsSolveThroughEnergyAndMagic() {
        // Independent fixture from ConfigResearch:78 and the radius-two ring:
        // FIRE/MAGIC, complexity 1; endpoints (-2,2) and (2,-2).
        assertEquals(List.of("ignis", "praecantatio"),
                new ArrayList<>(ResearchCatalog.get("FOCUSFIRE").aspectCost().keySet()));
        assertEquals(Map.of("ignis", 3, "praecantatio", 3), ResearchCatalog.get("FOCUSFIRE").aspectCost());
        List<Cell> path = List.of(fixed(-2, 2, "ignis"), placed(-1, 1, "potentia"),
                placed(0, 0, "praecantatio"), placed(1, -1, "potentia"), fixed(2, -2, "praecantatio"));
        ResearchNoteData note = new ResearchNoteData("FOCUSFIRE", 0xff5a01, false, 7, grid(2, path));
        List<Cell> before = List.copyOf(note.cells());

        ResearchNoteData solved = ResearchNotePuzzle.solve(note, Set.of("ignis", "potentia", "praecantatio"));

        assertNotSame(note, solved);
        assertTrue(solved.complete());
        assertEquals(Set.copyOf(path), Set.copyOf(solved.cells()));
        assertEquals(5, solved.cells().size());
        assertEquals(note.key(), solved.key());
        assertEquals(note.color(), solved.color());
        assertEquals(7, solved.copies());
        assertFalse(note.complete());
        assertEquals(before, note.cells());
    }

    @Test void originalResearchExpertiseThreeEndpointFixtureConnectsBothBranches() {
        // ConfigResearch:384: MIND/SENSES/ORDER, complexity 1. The endpoint
        // coordinates are ring indexes 0,4,8, without calling the generator.
        assertEquals(List.of("cognitio", "sensus", "ordo"),
                new ArrayList<>(ResearchCatalog.get("RESEARCHER1").aspectCost().keySet()));
        List<Cell> path = List.of(fixed(-2, 2, "cognitio"), placed(-1, 1, "spiritus"),
                placed(0, 0, "sensus"), placed(1, 0, "spiritus"), fixed(2, 0, "sensus"),
                placed(0, -1, "aer"), placed(1, -2, "motus"), fixed(0, -2, "ordo"));
        ResearchNoteData note = new ResearchNoteData("RESEARCHER1", 0xffc2b3, false, 0, grid(2, path));

        ResearchNoteData solved = ResearchNotePuzzle.solve(note,
                Set.of("cognitio", "spiritus", "sensus", "aer", "motus", "ordo"));

        assertTrue(solved.complete());
        assertEquals(8, solved.cells().size());
        assertEquals(Set.copyOf(path), Set.copyOf(solved.cells()));
    }

    @Test void reachingSomeEndpointsDoesNotCompleteOrPruneAnUnreachableEndpoint() {
        ResearchNoteData note = note(fixed(0, 0, "aer"), placed(1, 0, "motus"),
                fixed(-1, 0, "terra"), blank(0, 1));
        List<Cell> before = List.copyOf(note.cells());

        assertSame(note, ResearchNotePuzzle.solve(note, Set.of("aer", "motus", "terra")));
        assertFalse(note.complete());
        assertEquals(before, note.cells());
    }

    @Test void bothEndsOfEveryEdgeMustBeDiscoveredIncludingPlacedAspects() {
        ResearchNoteData note = note(fixed(-1, 0, "aer"), placed(0, 0, "motus"), fixed(1, 0, "ordo"));
        assertSame(note, ResearchNotePuzzle.solve(note, Set.of("aer", "ordo")));
        assertSame(note, ResearchNotePuzzle.solve(note, Set.of("motus", "ordo")));
        assertSame(note, ResearchNotePuzzle.solve(note, Set.of("aer", "motus")));
        assertTrue(ResearchNotePuzzle.solve(note, Set.of("aer", "motus", "ordo")).complete());
    }

    @Test void equalSharedParentAndGrandparentAspectsCannotCompleteAdjacentEndpoints() {
        for (List<String> pair : List.of(List.of("aer", "aer"), List.of("motus", "motus"),
                List.of("motus", "lux"), List.of("ordo", "volatus"))) {
            ResearchNoteData note = note(fixed(0, 0, pair.getFirst()), fixed(1, 0, pair.getLast()));
            assertSame(note, ResearchNotePuzzle.solve(note, new LinkedHashSet<>(pair)), pair.toString());
        }
    }

    @Test void successRetainsConnectedBranchesAndInputOrderButRemovesBlanksAndDisconnectedPlacements() {
        Cell lastEndpoint = fixed(1, 0, "ordo");
        Cell branch = placed(0, 1, "volatus");
        Cell bridge = placed(0, 0, "motus");
        Cell firstEndpoint = fixed(-1, 0, "aer");
        ResearchNoteData note = note(blank(1, -1), placed(-1, 1, "aqua"),
                lastEndpoint, branch, bridge, firstEndpoint);

        ResearchNoteData solved = ResearchNotePuzzle.solve(note, Set.of("aer", "motus", "ordo", "volatus", "aqua"));

        assertTrue(solved.complete());
        assertEquals(List.of(lastEndpoint, branch, bridge, firstEndpoint), solved.cells());
        assertNull(solved.cellAt(1, -1));
        assertNull(solved.cellAt(-1, 1));
        assertEquals(6, note.cells().size());
    }

    @Test void absentCellsAreHolesAndBlankCellsDoNotBridgeAdjacentOccupiedCells() {
        ResearchNoteData hole = note(fixed(-1, 0, "aer"), fixed(1, 0, "motus"));
        ResearchNoteData blank = note(fixed(-1, 0, "aer"), blank(0, 0), fixed(1, 0, "motus"));
        assertSame(hole, ResearchNotePuzzle.solve(hole, Set.of("aer", "motus")));
        assertSame(blank, ResearchNotePuzzle.solve(blank, Set.of("aer", "motus")));
    }

    @Test void solvedNotesAreIdempotentEvenIfDiscoveryLaterChanges() {
        ResearchNoteData initial = note(fixed(0, 0, "aer"), fixed(1, 0, "motus"), blank(-1, 0));
        ResearchNoteData solved = ResearchNotePuzzle.solve(initial, Set.of("aer", "motus"));
        assertTrue(solved.complete());
        assertSame(solved, ResearchNotePuzzle.solve(solved, Set.of()));
        assertEquals(2, solved.cells().size());
    }

    @Test void singleEndpointPreservesOriginalVacuousCompletionButZeroEndpointsAreRejected() {
        ResearchNoteData single = note(fixed(0, 0, "aer"), placed(1, 0, "lux"), blank(-1, 0));
        ResearchNoteData solved = ResearchNotePuzzle.solve(single, Set.of());
        assertTrue(solved.complete());
        assertEquals(List.of(single.cellAt(0, 0)), solved.cells());
        assertThrows(IllegalArgumentException.class, () -> note(blank(0, 0), placed(1, 0, "motus")));
    }

    @Test void solvingDoesNotSpendAspectPoolsUnlockResearchOrChangeWarpAndScanState() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setInitializedAspects(true);
        knowledge.setAspectPool("aer", 17);
        knowledge.setAspectPool("motus", 0); // Discovery at zero still permits a connection.
        knowledge.setAspectPool("ordo", 4);
        knowledge.setWarpPerm(2);
        knowledge.setWarpSticky(4);
        knowledge.setWarpTemp(5);
        knowledge.setWarpCounter(7);
        knowledge.addResearch("@PRESERVED_CLUE");
        knowledge.scanItem("@minecraft:stone");
        knowledge.scanEntity("#minecraft:pig");
        knowledge.scanPhenomena("@NODE-preserved");
        CompoundTag before = knowledge.save();
        Set<String> discoveries = new LinkedHashSet<>(knowledge.getAspectsDiscovered().keySet());
        Set<String> discoveriesBefore = Set.copyOf(discoveries);
        ResearchNoteData note = note(fixed(-1, 0, "aer"), placed(0, 0, "motus"), fixed(1, 0, "ordo"));

        assertTrue(ResearchNotePuzzle.solve(note, discoveries).complete());

        assertEquals(before, knowledge.save());
        assertEquals(discoveriesBefore, discoveries);
        assertFalse(knowledge.isResearchComplete(note.key()));
    }

    private static ResearchNoteData note(Cell... cells) {
        return new ResearchNoteData("FOCUSFIRE", 0x9700c0, false, 0, List.of(cells));
    }

    private static Cell fixed(int q, int r, String aspect) { return new Cell(q, r, 1, aspect); }
    private static Cell placed(int q, int r, String aspect) { return new Cell(q, r, 2, aspect); }
    private static Cell blank(int q, int r) { return new Cell(q, r, 0, null); }

    private static List<Cell> grid(int radius, List<Cell> occupied) {
        Map<String, Cell> replacements = new HashMap<>();
        for (Cell cell : occupied) replacements.put(cell.coordinate(), cell);
        List<Cell> cells = new ArrayList<>();
        for (HexUtils.Hex hex : HexUtils.generateHexes(radius).values()) {
            cells.add(replacements.getOrDefault(hex.toString(), blank(hex.q, hex.r)));
        }
        return cells;
    }
}
