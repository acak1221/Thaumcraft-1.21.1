package thaumcraft.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteData.Cell;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.lib.research.ResearchNoteEditing.Edit;
import thaumcraft.common.lib.research.ResearchNoteEditing.FloatSource;
import thaumcraft.common.lib.research.ResearchNoteEditing.PoolChange;
import thaumcraft.common.lib.research.ResearchNoteEditing.Result;
import thaumcraft.common.lib.research.ResearchNoteEditing.State;
import thaumcraft.common.lib.research.ScribingInk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static thaumcraft.common.lib.research.ResearchNoteEditing.Action.*;
import static thaumcraft.common.lib.research.ResearchNoteEditing.Failure.*;
import static thaumcraft.common.lib.research.ResearchNoteEditing.Funding.*;

class ResearchNoteEditingTest {
    private static final ScribingInk.State INK = new ScribingInk.State(true, 17, 100, true);
    private static final Edit PLACE_LUX = new Edit(PLACE, 1, 0, "lux");
    private static final Edit ERASE_LUX = new Edit(ERASE, 1, 0, null);

    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void ordinaryPlacementUsesActorBeforeBonusWithoutResearchOrRandomness() {
        ResearchNoteData note = openNote();
        State before = state(note, pools(2), Set.of(), Map.of("lux", 1, "aqua", 3));
        Trace random = new Trace(0.0F);

        Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

        assertTrue(result.success());
        assertEquals(ACTOR_POOL, result.funding());
        assertEquals(new PoolChange("lux", (short) -1, 2, 1, true, true), result.poolChange());
        assertEquals(1, result.poolChange().queriedTotal());
        assertEquals(new ScribingInk.Plan(true, 17, 18), result.ink());
        assertEquals(new State(replace(note, placed(1, 0, "lux")), false, inkAt(18),
                pools(1), Set.of(), before.bonusPools()), result.after());
        assertFalse(result.completesNote());
        assertFalse(result.freePlacement());
        assertFalse(result.refunded());
        assertNull(result.orbPitch());
        random.assertDraws();
        assertEquals(state(note, pools(2), Set.of(), Map.of("lux", 1, "aqua", 3)), before);
    }

    @Test void spendingLastActorPointRetainsDiscoveryAtZeroInsteadOfRemovingTheTag() {
        State before = state(openNote(), pools(1), Set.of("RESEARCHER1"), Map.of("lux", 1));
        Trace random = new Trace();

        Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

        assertTrue(result.success());
        assertEquals(pools(0), result.after().actorPools());
        assertTrue(result.after().actorPools().containsKey("lux"));
        assertEquals(before.bonusPools(), result.after().bonusPools());
        assertEquals(0, result.poolChange().queriedTotal());
        assertFalse(result.poolChange().discoversAspect());
        random.assertDraws(); // Researcher1 alone never adds a placement roll.
    }

    @Test void zeroActorPoolFallsBackToBonusAndOnlyTheLastBonusUnitDeletesItsKey() {
        for (int amount : List.of(1, 2)) {
            State before = state(openNote(), pools(0), Set.of(), Map.of("lux", amount, "terra", 4));
            Trace random = new Trace();

            Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

            assertTrue(result.success());
            assertEquals(TABLE_BONUS, result.funding());
            assertNull(result.poolChange()); // No actor save/correction branch.
            assertEquals(before.actorPools(), result.after().actorPools());
            assertEquals(amount == 1 ? Map.of("terra", 4) : Map.of("lux", 1, "terra", 4),
                    result.after().bonusPools());
            assertEquals(replace(before.note(), placed(1, 0, "lux")), result.after().note());
            assertEquals(18, result.after().ink().damage());
            random.assertDraws();
        }
    }

    @Test void unfundedPlacementIsRejectedBeforeEvenAWinningResearcher2Roll() {
        for (int raw : List.of(0, -1, 32768, 65535, 65536, Integer.MAX_VALUE, Integer.MIN_VALUE)) {
            for (Map<String, Integer> bonus : List.of(Map.<String, Integer>of(), Map.of("lux", 0), Map.of("lux", -1))) {
                State before = state(openNote(), pools(raw), Set.of("RESEARCHER2"), bonus);
                assertRefused(before, PLACE_LUX, INSUFFICIENT_ASPECTS);
            }
        }
    }

    @Test void undiscoveredPlacementIsRejectedEvenWithBonusAndResearcher2() {
        State before = state(openNote(), primals(), Set.of("RESEARCHER2"), Map.of("lux", 1));
        assertRefused(before, PLACE_LUX, UNDISCOVERED_ASPECT);
    }

    @Test void knownCompoundCanBePlacedWithoutDiscoveringItsComponents() {
        Map<String, Integer> actor = primals();
        actor.put("praecantatio", 2);
        State before = state(openNote(), actor, Set.of(), Map.of());
        assertFalse(before.actorPools().containsKey("vacuos"));
        assertFalse(before.actorPools().containsKey("potentia"));
        Trace random = new Trace();

        Result result = ResearchNoteEditing.evaluate(before, new Edit(PLACE, 1, 0, "praecantatio"), random);

        assertTrue(result.success());
        actor.put("praecantatio", 1);
        assertEquals(new State(replace(before.note(), placed(1, 0, "praecantatio")), false, inkAt(18),
                actor, Set.of(), Map.of()), result.after());
        assertEquals(new PoolChange("praecantatio", (short) -1, 2, 1, true, true), result.poolChange());
        random.assertDraws();
    }

    @Test void invalidPlacementTagsDoNotBecomeEraseAndConsumeNothing() {
        State before = state(openNote(), pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
        for (String tag : new String[]{null, "", "null", "LUX", "unknown", "x".repeat(129)}) {
            assertRefused(before, new Edit(PLACE, 1, 0, tag), UNKNOWN_ASPECT);
        }
    }

    @Test void fixedHolesOutsideCoordinatesAndWrongCellTypesAreRejectedWithoutDraws() {
        ResearchNoteData note = note(fixed(0, 0, "aer"), blank(1, 0), placed(-1, 0, "lux"), fixed(3, 0, "ignis"));
        State before = state(note, pools(2), Set.of("RESEARCHER1", "RESEARCHER2"), Map.of("lux", 1));
        assertRefused(before, new Edit(PLACE, 0, 0, "lux"), FIXED_CELL);
        assertRefused(before, new Edit(ERASE, 0, 0, null), FIXED_CELL);
        assertRefused(before, new Edit(PLACE, 0, 1, "lux"), MISSING_CELL);
        assertRefused(before, new Edit(ERASE, 0, 1, null), MISSING_CELL);
        assertRefused(before, new Edit(ERASE, 1, 0, null), NOT_PLACED);
        assertRefused(before, new Edit(PLACE, -1, 0, "lux"), NOT_BLANK);
        for (int[] coordinate : List.of(new int[]{5, 0}, new int[]{0, -5}, new int[]{4, 1},
                new int[]{-4, -1}, new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE})) {
            assertRefused(before, new Edit(PLACE, coordinate[0], coordinate[1], "lux"), INVALID_COORDINATE);
            assertRefused(before, new Edit(ERASE, coordinate[0], coordinate[1], null), INVALID_COORDINATE);
        }
    }

    @Test void missingSolvedAndDiscoveryNoteStatesRemainUntouched() {
        ResearchNoteData note = openNote();
        State before = state(note, pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
        assertRefused(new State(null, false, INK, before.actorPools(), before.completedResearch(), before.bonusPools()),
                PLACE_LUX, MISSING_NOTE);
        assertRefused(new State(note, true, INK, before.actorPools(), before.completedResearch(), before.bonusPools()),
                PLACE_LUX, DISCOVERY_PRESENTATION);
        ResearchNoteData complete = note.withCells(note.cells(), true);
        assertRefused(new State(complete, false, INK, before.actorPools(), before.completedResearch(), before.bonusPools()),
                PLACE_LUX, SOLVED);
        assertRefused(new State(complete, true, INK, before.actorPools(), before.completedResearch(), before.bonusPools()),
                ERASE_LUX, DISCOVERY_PRESENTATION);
    }

    @Test void unusableOrMissingScribeMarkerRefusesBothPlaceAndEraseBeforeRandomness() {
        for (ScribingInk.State ink : List.of(new ScribingInk.State(false, 0, 100, true),
                new ScribingInk.State(false, 0, 0, false), new ScribingInk.State(true, 100, 100, true),
                new ScribingInk.State(true, 101, 100, false), new ScribingInk.State(true, 0, 0, false))) {
            State place = new State(openNote(), false, ink, pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
            State erase = new State(placedNote(), false, ink, pools(2), Set.of("RESEARCHER1"), Map.of("lux", 1));
            assertRefused(place, PLACE_LUX, MISSING_INK);
            assertRefused(erase, ERASE_LUX, MISSING_INK);
        }
    }

    @Test void malformedActionsNeverDrawOrTrustAnEraseAspectFromTheCaller() {
        State before = state(placedNote(), pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
        assertRefused(before, null, INVALID_ACTION);
        assertRefused(before, new Edit(null, 1, 0, null), INVALID_ACTION);
        assertRefused(before, new Edit(ERASE, 1, 0, "lux"), INVALID_ACTION);
        assertRefused(before, new Edit(ERASE, 1, 0, "ignis"), INVALID_ACTION);
    }

    @Test void researcher2FreePlacementUsesStrictThresholdThenPitchWithoutSpendingEitherFundingSource() {
        float decision = Math.nextDown(0.1F);
        for (int raw : List.of(0, 2)) {
            // RESEARCHER2 is checked independently; RESEARCHER1 is deliberately absent.
            State before = state(openNote(), pools(raw), Set.of("RESEARCHER2"), Map.of("lux", 1));
            Trace random = new Trace(decision, 0.875F, 0.375F);

            Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

            assertTrue(result.success());
            assertTrue(result.freePlacement());
            assertEquals(RESEARCHER_FREE, result.funding());
            assertNull(result.poolChange());
            // The original float operations produce one ULP below literal 1.075F.
            assertEquals(0x3f899999, Float.floatToIntBits(result.orbPitch()));
            assertEquals(before.actorPools(), result.after().actorPools());
            assertEquals(before.bonusPools(), result.after().bonusPools());
            assertEquals(new ScribingInk.Plan(true, 17, 18), result.ink());
            random.assertDraws(decision, 0.875F);
            assertEquals(0.375F, random.nextFloat()); // The next gameplay draw was not consumed.
        }
    }

    @Test void researcher2ExactTenthPaysNormallyAndDoesNotDrawPitch() {
        for (int raw : List.of(0, 2)) {
            State before = state(openNote(), pools(raw), Set.of("RESEARCHER1", "RESEARCHER2"), Map.of("lux", 1));
            Trace random = new Trace(0.1F, 0.875F);

            Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

            assertTrue(result.success());
            assertFalse(result.freePlacement());
            assertNull(result.orbPitch());
            assertEquals(raw == 0 ? TABLE_BONUS : ACTOR_POOL, result.funding());
            assertEquals(pools(raw == 0 ? 0 : 1), result.after().actorPools());
            assertEquals(raw == 0 ? Map.of() : Map.of("lux", 1), result.after().bonusPools());
            random.assertDraws(0.1F);
            assertEquals(0.875F, random.nextFloat());
        }
    }

    @Test void lastTableInkPointIsLiteralEvenForNonDamageableToolAndFreePlacement() {
        for (boolean damageable : List.of(false, true)) {
            State before = new State(openNote(), false, new ScribingInk.State(true, 99, 100, damageable),
                    pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
            Trace random = new Trace(0.0F, 0.5F);

            Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

            assertTrue(result.success());
            assertTrue(result.freePlacement());
            assertEquals(new ScribingInk.Plan(true, 99, 100), result.ink());
            assertEquals(new ScribingInk.State(true, 100, 100, damageable), result.after().ink());
            random.assertDraws(0.0F, 0.5F); // No player durability/enchantment draw.
            assertRefused(result.after(), ERASE_LUX, MISSING_INK);
        }
    }

    @Test void eraseWithoutResearchersStillDrawsOnceAndCanEraseUnknownAspect() {
        State before = state(placedNote(), primals(), Set.of(), Map.of("lux", 1));
        Trace random = new Trace(0.0F, 0.75F);

        Result result = ResearchNoteEditing.evaluate(before, ERASE_LUX, random);

        assertTrue(result.success());
        assertEquals(ResearchNoteEditing.Failure.NONE, result.failure());
        assertEquals(ResearchNoteEditing.Funding.NONE, result.funding());
        assertNull(result.poolChange());
        assertNull(result.orbPitch());
        assertFalse(result.refunded());
        assertEquals(new State(replace(before.note(), blank(1, 0)), false, inkAt(18),
                before.actorPools(), before.completedResearch(), before.bonusPools()), result.after());
        assertFalse(result.after().actorPools().containsKey("lux"));
        random.assertDraws(0.0F);
        assertEquals(0.75F, random.nextFloat());
    }

    @Test void eraseResearcherThresholdsShareExactlyOneDecisionRollAndDoNotAddPercentages() {
        record Vector(Set<String> research, float decision, boolean refund) {}
        List<Vector> vectors = List.of(
                new Vector(Set.of("RESEARCHER1"), Math.nextDown(0.25F), true),
                new Vector(Set.of("RESEARCHER1"), 0.25F, false),
                new Vector(Set.of("RESEARCHER2"), Math.nextDown(0.5F), true),
                new Vector(Set.of("RESEARCHER2"), 0.5F, false),
                new Vector(Set.of("RESEARCHER1", "RESEARCHER2"), 0.1F, true),
                new Vector(Set.of("RESEARCHER1", "RESEARCHER2"), 0.25F, true),
                new Vector(Set.of("RESEARCHER1", "RESEARCHER2"), Math.nextDown(0.5F), true),
                new Vector(Set.of("RESEARCHER1", "RESEARCHER2"), 0.5F, false),
                new Vector(Set.of("RESEARCHER1", "RESEARCHER2"), 0.6F, false));
        for (Vector vector : vectors) {
            State before = state(placedNote(), pools(7), vector.research(), Map.of("lux", 2));
            Trace random = new Trace(vector.decision(), 0.25F, 0.875F);

            Result result = ResearchNoteEditing.evaluate(before, ERASE_LUX, random);

            assertTrue(result.success(), vector.toString());
            assertEquals(vector.refund(), result.refunded(), vector.toString());
            assertEquals(replace(before.note(), blank(1, 0)), result.after().note());
            assertEquals(pools(vector.refund() ? 8 : 7), result.after().actorPools());
            assertEquals(before.bonusPools(), result.after().bonusPools());
            assertEquals(18, result.after().ink().damage());
            if (vector.refund()) {
                assertEquals(new PoolChange("lux", (short) 1, 7, 8, true, true), result.poolChange());
                assertEquals(0.95F, result.orbPitch().floatValue());
                random.assertDraws(vector.decision(), 0.25F);
                assertEquals(0.875F, random.nextFloat());
            } else {
                assertNull(result.poolChange());
                assertNull(result.orbPitch());
                random.assertDraws(vector.decision());
                assertEquals(0.25F, random.nextFloat());
            }
        }
    }

    @Test void bonusFundedPlacementByOneActorCanRefundAndDiscoverForAnotherActor() {
        State actorA = state(openNote(), pools(0), Set.of(), Map.of("lux", 1));
        Result placement = ResearchNoteEditing.evaluate(actorA, PLACE_LUX, new Trace());
        assertTrue(placement.success());
        assertEquals(TABLE_BONUS, placement.funding());
        assertTrue(placement.after().bonusPools().isEmpty());
        State actorB = new State(placement.after().note(), false, placement.after().ink(),
                primals(), Set.of("RESEARCHER1"), placement.after().bonusPools());
        Trace random = new Trace(0.125F, 0.5F);

        Result erased = ResearchNoteEditing.evaluate(actorB, ERASE_LUX, random);

        assertTrue(erased.success());
        assertTrue(erased.refunded());
        assertTrue(erased.poolChange().discoversAspect());
        assertEquals(new PoolChange("lux", (short) 1, 0, 1, false, true), erased.poolChange());
        assertEquals(pools(1), erased.after().actorPools());
        assertEquals(Map.of(), erased.after().bonusPools()); // No funding history/refund to bonus.
        assertEquals(pools(0), actorA.actorPools());
        assertEquals(pools(0), placement.after().actorPools());
        assertEquals(19, erased.after().ink().damage());
        random.assertDraws(0.125F, 0.5F);
    }

    @Test void researcherFreeCellAlsoRefundsActorPointsWithoutRestoringBonus() {
        State before = state(openNote(), pools(0), Set.of("RESEARCHER2"), Map.of("lux", 1));
        Trace random = new Trace(0.0F, 0.25F, 0.4F, 0.75F, 0.625F);
        Result placement = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);
        assertTrue(placement.freePlacement());

        Result erase = ResearchNoteEditing.evaluate(placement.after(), ERASE_LUX, random);

        assertTrue(erase.refunded());
        assertEquals(pools(1), erase.after().actorPools());
        assertEquals(Map.of("lux", 1), erase.after().bonusPools());
        assertEquals(19, erase.after().ink().damage());
        assertEquals(1.05F, erase.orbPitch().floatValue());
        random.assertDraws(0.0F, 0.25F, 0.4F, 0.75F);
        assertEquals(0.625F, random.nextFloat());
    }

    @Test void placementUsesSignedShortQueryButDebitUsesRawPositiveStorage() {
        // PlayerKnowledge bytecode: getAspectPoolFor i2s@15;
        // addAspectPool raw getAmount/ifle@53-56; TileResearchTable ignores return@248.
        record Vector(int raw, short query, boolean actorBranch, int after, boolean applied, short total) {}
        List<Vector> vectors = List.of(
                new Vector(32767, (short) 32767, true, 32766, true, (short) 32766),
                new Vector(32768, (short) -32768, false, 32768, false, (short) 0),
                new Vector(65535, (short) -1, false, 65535, false, (short) 0),
                new Vector(65536, (short) 0, false, 65536, false, (short) 0),
                new Vector(65537, (short) 1, true, 65536, true, (short) 0),
                new Vector(-65535, (short) 1, true, -65535, false, (short) 1),
                new Vector(-32769, (short) 32767, true, -32769, false, (short) 32767),
                new Vector(Integer.MAX_VALUE, (short) -1, false, Integer.MAX_VALUE, false, (short) 0),
                new Vector(Integer.MIN_VALUE, (short) 0, false, Integer.MIN_VALUE, false, (short) 0));
        for (Vector vector : vectors) {
            State before = state(openNote(), pools(vector.raw()), Set.of(), Map.of("lux", 1));
            Trace random = new Trace();

            Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

            assertEquals(vector.query(), ResearchNoteEditing.queryPool(vector.raw()));
            assertTrue(result.success(), vector.toString());
            assertEquals(vector.actorBranch() ? ACTOR_POOL : TABLE_BONUS, result.funding());
            assertEquals(new State(replace(before.note(), placed(1, 0, "lux")), false, inkAt(18),
                    pools(vector.after()), Set.of(), vector.actorBranch() ? Map.of("lux", 1) : Map.of()), result.after());
            if (vector.actorBranch()) {
                assertEquals(new PoolChange("lux", (short) -1, vector.raw(), vector.after(), true, vector.applied()),
                        result.poolChange());
                // A no-op debit still has the source's save/correction effect, not free-placement sound.
                assertEquals(vector.total(), result.poolChange().queriedTotal());
            } else assertNull(result.poolChange());
            assertFalse(result.freePlacement());
            assertNull(result.orbPitch());
            random.assertDraws();
        }
    }

    @Test void refundsUseRawIntAdditionWithoutScanCapShortNarrowingOrOverflowRefusal() {
        // AspectList.add's iadd@28 preserves live int wrapping; the correction alone is short.
        record Vector(int before, int after, short total) {}
        for (Vector vector : List.of(new Vector(100, 101, (short) 101),
                new Vector(32767, 32768, (short) -32768), new Vector(32768, 32769, (short) -32767),
                new Vector(65535, 65536, (short) 0), new Vector(65536, 65537, (short) 1),
                new Vector(-65535, -65534, (short) 2), new Vector(-32769, -32768, (short) -32768),
                new Vector(-1, 0, (short) 0), new Vector(Integer.MAX_VALUE, Integer.MIN_VALUE, (short) 0),
                new Vector(Integer.MIN_VALUE, -2147483647, (short) 1))) {
            State before = state(placedNote(), pools(vector.before()), Set.of("RESEARCHER1"), Map.of("lux", 1));
            Trace random = new Trace(0.125F, 0.25F);

            Result result = ResearchNoteEditing.evaluate(before, ERASE_LUX, random);

            assertTrue(result.success(), vector.toString());
            assertTrue(result.refunded());
            assertEquals(pools(vector.after()), result.after().actorPools());
            assertEquals(new PoolChange("lux", (short) 1, vector.before(), vector.after(), true, true), result.poolChange());
            assertEquals(vector.total(), result.poolChange().queriedTotal());
            assertEquals(before.bonusPools(), result.after().bonusPools());
            assertEquals(18, result.after().ink().damage());
            random.assertDraws(0.125F, 0.25F);
        }
    }

    @Test void placementSolvesCandidateRetainsConnectedBranchesAndOrderAndPlansDiscovery() {
        Cell last = fixed(2, 0, "ignis");
        Cell branch = placed(3, 0, "potentia");
        Cell first = fixed(0, 0, "aer");
        // Synthetic NITOR geometry, not a claim about NITOR generator endpoints.
        ResearchNoteData note = note(blank(-1, 1), placed(-2, 0, "perditio"), last, branch, blank(1, 0), first);
        Map<String, Integer> actor = pools(1);
        actor.put("potentia", 0);
        Set<String> completed = Set.of("@PRESERVED_CLUE"); // No CRUCIBLE prerequisite is demanded by editing.
        State before = state(note, actor, completed, Map.of("lux", 1));
        Trace random = new Trace();

        Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, random);

        ResearchNoteData expected = note.withCells(List.of(last, branch, placed(1, 0, "lux"), first), true);
        assertTrue(result.success());
        assertTrue(result.completesNote());
        assertTrue(result.after().discoveryPresentation());
        actor.put("lux", 0);
        assertEquals(new State(expected, true, inkAt(18), actor, completed, Map.of("lux", 1)), result.after());
        assertEquals("NITOR", result.after().note().key());
        assertEquals(0x123456, result.after().note().color());
        assertEquals(7, result.after().note().copies());
        assertFalse(before.note().complete());
        assertEquals(6, before.note().cells().size());
        random.assertDraws();
        assertRefused(result.after(), new Edit(ERASE, 1, 0, null), DISCOVERY_PRESENTATION);
    }

    @Test void unknownFixedEndpointBlocksSolveButDoesNotBlockLegalKnownPlacementOrBecomeDiscovered() {
        ResearchNoteData note = note(fixed(0, 0, "lux"), blank(1, 0), fixed(2, 0, "potentia"), blank(-1, 0));
        Map<String, Integer> actor = primals();
        actor.put("ignis", 1);
        actor.put("potentia", 0);
        State before = state(note, actor, Set.of("NITOR"), Map.of()); // Already knowing the research is not an edit gate.

        Result result = ResearchNoteEditing.evaluate(before, new Edit(PLACE, 1, 0, "ignis"), new Trace());

        assertTrue(result.success());
        assertFalse(result.completesNote());
        assertFalse(result.after().discoveryPresentation());
        assertFalse(result.after().actorPools().containsKey("lux"));
        assertEquals(replace(note, placed(1, 0, "ignis")), result.after().note());
        assertEquals(before.completedResearch(), result.after().completedResearch());
        assertEquals(0, result.after().actorPools().get("ignis").intValue());
    }

    @Test void successfulUnknownEraseRefundIsDiscoveredBeforeSolvingTheRemainingBridge() {
        ResearchNoteData note = refundSolveNote();
        State before = state(note, primals(), Set.of("RESEARCHER1"), Map.of("lux", 1));
        Trace random = new Trace(0.125F, 0.75F, 0.625F);

        Result result = ResearchNoteEditing.evaluate(before, new Edit(ERASE, -1, 0, null), random);

        ResearchNoteData solved = note.withCells(List.of(fixed(0, 0, "aer"), placed(1, 0, "lux"), fixed(2, 0, "ignis")), true);
        assertTrue(result.success());
        assertTrue(result.refunded());
        assertTrue(result.poolChange().discoversAspect());
        assertEquals(new PoolChange("lux", (short) 1, 0, 1, false, true), result.poolChange());
        assertEquals(new State(solved, true, inkAt(18), pools(1), Set.of("RESEARCHER1"), Map.of("lux", 1)), result.after());
        assertTrue(result.completesNote());
        assertEquals(1.05F, result.orbPitch().floatValue());
        assertFalse(before.actorPools().containsKey("lux"));
        assertFalse(before.note().complete());
        random.assertDraws(0.125F, 0.75F);
        assertEquals(0.625F, random.nextFloat());
    }

    @Test void failedUnknownEraseRefundDoesNotDiscoverOrPruneAndStillSpendsInk() {
        ResearchNoteData note = refundSolveNote();
        State before = state(note, primals(), Set.of("RESEARCHER1"), Map.of("lux", 1));
        Trace random = new Trace(0.25F, 0.75F);

        Result result = ResearchNoteEditing.evaluate(before, new Edit(ERASE, -1, 0, null), random);

        assertTrue(result.success());
        assertFalse(result.refunded());
        assertFalse(result.completesNote());
        assertNull(result.poolChange());
        assertNull(result.orbPitch());
        assertEquals(new State(replace(note, blank(-1, 0)), false, inkAt(18),
                primals(), Set.of("RESEARCHER1"), Map.of("lux", 1)), result.after());
        assertFalse(result.after().actorPools().containsKey("lux"));
        assertEquals(blank(-1, 0), result.after().note().cellAt(-1, 0)); // Blank, not hole.
        random.assertDraws(0.25F);
        assertEquals(0.75F, random.nextFloat());
    }

    @Test void erasingABridgeSolvesOnlyAfterMutationAndNeverPrunesOnFailure() {
        ResearchNoteData note = note(fixed(0, 0, "aer"), placed(1, 0, "lux"), fixed(2, 0, "ignis"),
                placed(-2, 0, "perditio"), blank(-1, 1));
        State before = state(note, pools(0), Set.of(), Map.of());
        Trace random = new Trace(0.0F);

        Result result = ResearchNoteEditing.evaluate(before, ERASE_LUX, random);

        assertTrue(result.success());
        assertFalse(result.completesNote());
        assertEquals(replace(note, blank(1, 0)), result.after().note());
        assertEquals(5, result.after().note().cells().size());
        assertEquals(18, result.after().ink().damage());
        random.assertDraws(0.0F);
    }

    @Test void eraseInvokesSolverAndPreservesOriginalSingleFixedVacuousCompletion() {
        ResearchNoteData note = note(fixed(0, 0, "aer"), placed(1, 0, "lux"), blank(-1, 0));
        State before = state(note, primals(), Set.of(), Map.of());
        Trace random = new Trace(0.0F);

        Result result = ResearchNoteEditing.evaluate(before, ERASE_LUX, random);

        assertTrue(result.completesNote());
        assertEquals(note.withCells(List.of(fixed(0, 0, "aer")), true), result.after().note());
        assertEquals(before.actorPools(), result.after().actorPools());
        assertEquals(new ScribingInk.Plan(true, 17, 18), result.ink());
        random.assertDraws(0.0F);
    }

    @Test void planningNeverMutatesFullNativeKnowledgeOrGrantsResearchScansOrWarp() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.setInitializedAspects(true);
        knowledge.addResearch("RESEARCHER1");
        knowledge.addResearch("@PRESERVED_CLUE");
        knowledge.setWarpPerm(3);
        knowledge.setWarpSticky(5);
        knowledge.setWarpTemp(7);
        knowledge.setWarpCounter(17);
        knowledge.scanItem("@minecraft:stone");
        knowledge.scanEntity("#minecraft:pig");
        knowledge.scanPhenomena("@NODE-preserved");
        var saved = knowledge.save();
        State before = state(refundSolveNote(), knowledge.getAspectsDiscovered(), knowledge.getResearchComplete(), Map.of());

        Result result = ResearchNoteEditing.evaluate(before, new Edit(ERASE, -1, 0, null), new Trace(0.0F, 0.5F));

        assertTrue(result.completesNote());
        assertTrue(result.poolChange().discoversAspect());
        assertEquals(before.completedResearch(), result.after().completedResearch());
        assertEquals(saved, knowledge.save()); // Even the proposed refund is not externally committed here.
        assertFalse(knowledge.hasDiscoveredAspect("lux"));
        assertFalse(knowledge.isResearchComplete("NITOR"));
    }

    @Test void snapshotsAndProposedOutputsAreImmutableAndIndependentOfCallerCollections() {
        Map<String, Integer> actor = pools(2);
        Map<String, Integer> bonus = new LinkedHashMap<>(Map.of("lux", 1));
        Set<String> research = new LinkedHashSet<>(Set.of("RESEARCHER1"));
        State before = state(openNote(), actor, research, bonus);
        actor.put("lux", 100);
        bonus.clear();
        research.add("RESEARCHER2");

        Result result = ResearchNoteEditing.evaluate(before, PLACE_LUX, new Trace());

        assertEquals(pools(2), before.actorPools());
        assertEquals(Map.of("lux", 1), before.bonusPools());
        assertEquals(Set.of("RESEARCHER1"), before.completedResearch());
        assertEquals(pools(1), result.after().actorPools());
        assertThrows(UnsupportedOperationException.class, () -> before.actorPools().put("lux", 5));
        assertThrows(UnsupportedOperationException.class, () -> result.after().actorPools().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.after().bonusPools().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.after().completedResearch().add("NITOR"));
        assertThrows(UnsupportedOperationException.class, () -> result.after().note().cells().clear());
    }

    @Test void badRandomAdapterThrowsWithoutPublishingPartialPoolInkOrNoteState() {
        for (float bad : new float[]{-0.01F, 1.0F, Float.NaN, Float.POSITIVE_INFINITY}) {
            State before = state(openNote(), pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
            State snapshot = state(openNote(), pools(2), Set.of("RESEARCHER2"), Map.of("lux", 1));
            Trace decision = new Trace(bad);
            assertThrows(IllegalArgumentException.class, () -> ResearchNoteEditing.evaluate(before, PLACE_LUX, decision));
            assertEquals(snapshot, before);
            decision.assertDraws(bad);
            Trace pitch = new Trace(0.0F, bad);
            assertThrows(IllegalArgumentException.class, () -> ResearchNoteEditing.evaluate(before, PLACE_LUX, pitch));
            assertEquals(snapshot, before);
            pitch.assertDraws(0.0F, bad); // No fictional RNG rollback.
        }
    }

    @Test void exactSourceRandomStreamContinuesAfterZeroOneAndTwoDrawBranches() {
        // Java Random is the source 1.7.10 world generator. Test the policy's use
        // of the supplied stream, not merely an output success flag.
        boolean sawFree = false;
        boolean sawPaid = false;
        for (long seed : List.of(0L, 1L, 12L, 4096L, 123456789L)) {
            Random actual = new Random(seed);
            Random expected = new Random(seed);
            State noResearch = state(openNote(), pools(2), Set.of(), Map.of("lux", 1));
            Result paid = ResearchNoteEditing.evaluate(noResearch, PLACE_LUX, actual::nextFloat);
            assertTrue(paid.success()); // Zero draws.
            Result erased = ResearchNoteEditing.evaluate(paid.after(), ERASE_LUX, actual::nextFloat);
            assertTrue(erased.success());
            expected.nextFloat(); // Erase without researcher still draws once.
            State r2 = new State(erased.after().note(), false, erased.after().ink(),
                    erased.after().actorPools(), Set.of("RESEARCHER2"), erased.after().bonusPools());
            float roll = expected.nextFloat();
            Float pitch = roll < 0.1F ? 0.9F + expected.nextFloat() * 0.2F : null;

            Result placement = ResearchNoteEditing.evaluate(r2, PLACE_LUX, actual::nextFloat);

            assertTrue(placement.success());
            assertEquals(roll < 0.1F, placement.freePlacement());
            assertEquals(pitch, placement.orbPitch());
            assertEquals(expected.nextLong(), actual.nextLong(), "next RNG state, seed=" + seed);
            sawFree |= placement.freePlacement();
            sawPaid |= !placement.freePlacement();
        }
        assertTrue(sawFree, "Seeded vectors must exercise the two-draw free branch");
        assertTrue(sawPaid, "Seeded vectors must exercise the one-draw paid branch");
    }

    private static State state(ResearchNoteData note, Map<String, Integer> actor, Set<String> research,
                               Map<String, Integer> bonus) {
        return new State(note, false, INK, actor, research, bonus);
    }

    private static ScribingInk.State inkAt(int damage) { return new ScribingInk.State(true, damage, 100, true); }

    private static Map<String, Integer> primals() {
        Map<String, Integer> pools = new LinkedHashMap<>();
        for (String tag : PlayerKnowledge.PRIMAL_TAGS) pools.put(tag, 0);
        return pools;
    }

    private static Map<String, Integer> pools(int lux) {
        Map<String, Integer> pools = primals();
        pools.put("lux", lux);
        return pools;
    }

    private static ResearchNoteData openNote() { return note(fixed(0, 0, "aer"), blank(1, 0), fixed(3, 0, "ignis")); }
    private static ResearchNoteData placedNote() { return replace(openNote(), placed(1, 0, "lux")); }
    private static ResearchNoteData refundSolveNote() {
        return note(fixed(0, 0, "aer"), placed(1, 0, "lux"), fixed(2, 0, "ignis"), placed(-1, 0, "lux"));
    }
    private static ResearchNoteData note(Cell... cells) {
        // Hand-authored geometry with real catalog/components; not generation evidence.
        return new ResearchNoteData("NITOR", 0x123456, false, 7, List.of(cells));
    }
    private static ResearchNoteData replace(ResearchNoteData note, Cell replacement) {
        return note.withCells(note.cells().stream().map(cell ->
                cell.q() == replacement.q() && cell.r() == replacement.r() ? replacement : cell).toList(), false);
    }
    private static Cell fixed(int q, int r, String tag) { return new Cell(q, r, 1, tag); }
    private static Cell placed(int q, int r, String tag) { return new Cell(q, r, 2, tag); }
    private static Cell blank(int q, int r) { return new Cell(q, r, 0, null); }

    private static void assertRefused(State before, Edit edit, ResearchNoteEditing.Failure expected) {
        Trace random = new Trace(0.0F, 0.75F);
        Result result = ResearchNoteEditing.evaluate(before, edit, random);
        assertEquals(expected, result.failure());
        assertFalse(result.success());
        assertSame(before, result.after());
        assertSame(before.note(), result.after().note());
        assertEquals(ResearchNoteEditing.Funding.NONE, result.funding());
        assertNull(result.poolChange());
        assertNull(result.orbPitch());
        assertFalse(result.freePlacement());
        assertFalse(result.refunded());
        assertFalse(result.completesNote());
        assertEquals(new ScribingInk.Plan(false, before.ink().damage(), before.ink().damage()), result.ink());
        random.assertDraws();
    }

    private static final class Trace implements FloatSource {
        private final float[] values;
        private final List<Float> draws = new ArrayList<>();
        private int cursor;

        private Trace(float... values) { this.values = values.clone(); }

        @Override public float nextFloat() {
            assertTrue(cursor < values.length, "Unexpected extra gameplay RNG draw");
            float value = values[cursor++];
            draws.add(value);
            return value;
        }

        private void assertDraws(float... expected) {
            List<Float> ordered = new ArrayList<>();
            for (float value : expected) ordered.add(value);
            assertEquals(ordered, draws, "Ordered world.nextFloat trace");
        }
    }
}
