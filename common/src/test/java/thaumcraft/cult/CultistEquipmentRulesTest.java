package thaumcraft.cult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.cult.CultistEquipmentRules;
import thaumcraft.common.lib.cult.CultistRules;

/**
 * The original TC4 cultist equipment table, asserted without a world or a registry.
 * The oracle sources are {@code EntityCultistKnight.java:75-96},
 * {@code EntityCultistCleric.java:92-102} and
 * {@code thaumcraft/common/entities/monster/boss/EntityCultistLeader.java:132-143}.
 */
class CultistEquipmentRulesTest {

    // ---------------------------------------------------------------- the crimson knight
    @Test void theKnightWearsTheCrimsonPlateTheBootsAndTheIronBlade() {
        CultistEquipmentRules.Loadout loadout = CultistEquipmentRules.knight(false, new Rolls(0.5F, 0, false));
        assertEquals(CultistEquipmentRules.Piece.PLATE_HELM, loadout.helm());
        assertEquals(CultistEquipmentRules.Piece.PLATE_CHEST, loadout.chest());
        assertEquals(CultistEquipmentRules.Piece.PLATE_LEGS, loadout.legs());
        assertEquals(CultistEquipmentRules.Piece.BOOTS, loadout.feet());
        assertEquals(CultistEquipmentRules.Weapon.IRON_SWORD, loadout.weapon());
    }

    @Test void theKnightDrawsItsRollsOnlyInsideTheOriginalBranches() {
        Rolls plain = new Rolls(0.5F, 0, false);
        CultistEquipmentRules.knight(false, plain);
        assertEquals(List.of("float"), plain.draws());
        Rolls voidBlade = new Rolls(0.01F, 0, false);
        CultistEquipmentRules.knight(true, voidBlade);
        assertEquals(List.of("float", "int"), voidBlade.draws());
        Rolls thaumium = new Rolls(0.01F, 3, false);
        CultistEquipmentRules.knight(true, thaumium);
        assertEquals(List.of("float", "int", "boolean"), thaumium.draws());
    }

    @Test void theRareKnightWearsTheVoidBladeAndTheRobeHood() {
        CultistEquipmentRules.Loadout loadout = CultistEquipmentRules.knight(true, new Rolls(0.01F, 0, false));
        assertEquals(CultistEquipmentRules.Piece.ROBE_HOOD, loadout.helm());
        assertEquals(CultistEquipmentRules.Piece.PLATE_CHEST, loadout.chest());
        assertEquals(CultistEquipmentRules.Piece.PLATE_LEGS, loadout.legs());
        assertEquals(CultistEquipmentRules.Piece.BOOTS, loadout.feet());
        assertEquals(CultistEquipmentRules.Weapon.VOID_SWORD, loadout.weapon());
    }

    @Test void theRareKnightWearsTheThaumiumBladeAndKeepsTheHelmOnTheCoinFlip() {
        CultistEquipmentRules.Loadout kept = CultistEquipmentRules.knight(true, new Rolls(0.01F, 4, false));
        assertEquals(CultistEquipmentRules.Piece.PLATE_HELM, kept.helm());
        assertEquals(CultistEquipmentRules.Weapon.THAUMIUM_SWORD, kept.weapon());
        CultistEquipmentRules.Loadout dropped = CultistEquipmentRules.knight(true, new Rolls(0.01F, 4, true));
        assertNull(dropped.helm());
        assertEquals(CultistEquipmentRules.Weapon.THAUMIUM_SWORD, dropped.weapon());
    }

    @Test void theRareKnightIsRareOnNormalAndCommonOnHard() {
        // The original chance is one percent off hard and five percent on hard.
        assertEquals(CultistEquipmentRules.Weapon.IRON_SWORD,
                CultistEquipmentRules.knight(false, new Rolls(0.02F, 0, false)).weapon());
        assertEquals(CultistEquipmentRules.Weapon.VOID_SWORD,
                CultistEquipmentRules.knight(true, new Rolls(0.02F, 0, false)).weapon());
    }

    // ---------------------------------------------------------------- the crimson cleric
    @Test void theClericWearsTheCrimsonRobeAndNoBlade() {
        CultistEquipmentRules.Loadout loadout = CultistEquipmentRules.cleric(false, new Rolls(0.5F, 0, false));
        assertEquals(CultistEquipmentRules.Piece.ROBE_HOOD, loadout.helm());
        assertEquals(CultistEquipmentRules.Piece.ROBE_CHEST, loadout.chest());
        assertEquals(CultistEquipmentRules.Piece.ROBE_LEGS, loadout.legs());
        assertEquals(CultistEquipmentRules.Weapon.NONE, loadout.weapon());
    }

    @Test void theClericWearsTheBootsOnlyOnTheOriginalChance() {
        assertNull(CultistEquipmentRules.cleric(false, new Rolls(0.5F, 0, false)).feet());
        assertEquals(CultistEquipmentRules.Piece.BOOTS,
                CultistEquipmentRules.cleric(false, new Rolls(0.05F, 0, false)).feet());
        assertNull(CultistEquipmentRules.cleric(true, new Rolls(0.4F, 0, false)).feet());
        assertEquals(CultistEquipmentRules.Piece.BOOTS,
                CultistEquipmentRules.cleric(true, new Rolls(0.2F, 0, false)).feet());
    }

    // ---------------------------------------------------------------- the praetor
    @Test void thePraetorWearsTheCrimsonPraetorArmourAndTheCrimsonBlade() {
        CultistEquipmentRules.Loadout loadout = CultistEquipmentRules.praetor(false);
        assertEquals(CultistEquipmentRules.Piece.LEADER_HELM, loadout.helm());
        assertEquals(CultistEquipmentRules.Piece.LEADER_CHEST, loadout.chest());
        assertEquals(CultistEquipmentRules.Piece.LEADER_LEGS, loadout.legs());
        assertEquals(CultistEquipmentRules.Piece.BOOTS, loadout.feet());
        assertEquals(CultistEquipmentRules.Weapon.CRIMSON_SWORD, loadout.weapon());
    }

    @Test void thePraetorTakesTheVoidBladeOnlyOnEasy() {
        assertEquals(CultistEquipmentRules.Weapon.VOID_SWORD, CultistEquipmentRules.praetor(true).weapon());
        assertEquals(CultistEquipmentRules.Weapon.CRIMSON_SWORD, CultistEquipmentRules.praetor(false).weapon());
    }

    // ---------------------------------------------------------------- the piece table
    @Test void theSlotsAnswerTheOriginalArmourOfTheKind() {
        CultistEquipmentRules.Loadout knight = CultistEquipmentRules.knight(false, new Rolls(0.5F, 0, false));
        assertEquals(CultistEquipmentRules.Piece.PLATE_HELM, knight.piece(CultistEquipmentRules.Slot.HELM));
        assertEquals(CultistEquipmentRules.Piece.PLATE_CHEST, knight.piece(CultistEquipmentRules.Slot.CHEST));
        assertEquals(CultistEquipmentRules.Piece.PLATE_LEGS, knight.piece(CultistEquipmentRules.Slot.LEGS));
        assertEquals(CultistEquipmentRules.Piece.BOOTS, knight.piece(CultistEquipmentRules.Slot.FEET));
        CultistEquipmentRules.Loadout cleric = CultistEquipmentRules.cleric(false, new Rolls(0.5F, 0, false));
        assertEquals(CultistEquipmentRules.Piece.ROBE_HOOD, cleric.piece(CultistEquipmentRules.Slot.HELM));
        assertEquals(CultistEquipmentRules.Piece.ROBE_CHEST, cleric.piece(CultistEquipmentRules.Slot.CHEST));
        assertEquals(CultistEquipmentRules.Piece.ROBE_LEGS, cleric.piece(CultistEquipmentRules.Slot.LEGS));
        CultistEquipmentRules.Loadout praetor = CultistEquipmentRules.praetor(false);
        assertEquals(CultistEquipmentRules.Piece.LEADER_HELM, praetor.piece(CultistEquipmentRules.Slot.HELM));
        assertEquals(CultistEquipmentRules.Piece.LEADER_CHEST, praetor.piece(CultistEquipmentRules.Slot.CHEST));
        assertEquals(CultistEquipmentRules.Piece.LEADER_LEGS, praetor.piece(CultistEquipmentRules.Slot.LEGS));
    }

    @Test void thePieceIdsAreTheRegisteredCultistArmourIds() {
        assertEquals("cultist_robe_hood", CultistEquipmentRules.Piece.ROBE_HOOD.id());
        assertEquals("cultist_robe", CultistEquipmentRules.Piece.ROBE_CHEST.id());
        assertEquals("cultist_robe_legs", CultistEquipmentRules.Piece.ROBE_LEGS.id());
        assertEquals("cultist_plate_helm", CultistEquipmentRules.Piece.PLATE_HELM.id());
        assertEquals("cultist_plate", CultistEquipmentRules.Piece.PLATE_CHEST.id());
        assertEquals("cultist_plate_legs", CultistEquipmentRules.Piece.PLATE_LEGS.id());
        assertEquals("cultist_leader_helm", CultistEquipmentRules.Piece.LEADER_HELM.id());
        assertEquals("cultist_leader", CultistEquipmentRules.Piece.LEADER_CHEST.id());
        assertEquals("cultist_leader_legs", CultistEquipmentRules.Piece.LEADER_LEGS.id());
        assertEquals("cultist_boots", CultistEquipmentRules.Piece.BOOTS.id());
    }

    @Test void theTenPiecesShareTheIdsOfTheCultistRulesTable() {
        for (CultistEquipmentRules.Piece piece : CultistEquipmentRules.Piece.values()) {
            assertTrue(List.of(CultistRules.ITEM_ROBE_HOOD, CultistRules.ITEM_ROBE_CHESTPLATE, CultistRules.ITEM_ROBE_LEGGINGS,
                    CultistRules.ITEM_PLATE_HELM, CultistRules.ITEM_PLATE_CHESTPLATE, CultistRules.ITEM_PLATE_LEGGINGS,
                    CultistRules.ITEM_LEADER_HELM, CultistRules.ITEM_LEADER_CHESTPLATE, CultistRules.ITEM_LEADER_LEGGINGS,
                    CultistRules.ITEM_BOOTS).contains(piece.id()), piece.id());
        }
        assertEquals(10, CultistEquipmentRules.Piece.values().length);
    }

    // ---------------------------------------------------------------- the helper
    /** A deterministic stand in for the original random source of the equipment rolls. */
    private static final class Rolls implements CultistEquipmentRules.Rolls {
        private final float floatRoll;
        private final int intRoll;
        private final boolean booleanRoll;
        private final List<String> draws = new ArrayList<>();

        Rolls(float floatRoll, int intRoll, boolean booleanRoll) {
            this.floatRoll = floatRoll;
            this.intRoll = intRoll;
            this.booleanRoll = booleanRoll;
        }

        @Override public float nextFloat() { draws.add("float"); return floatRoll; }

        @Override public int nextInt(int bound) { draws.add("int"); return intRoll; }

        @Override public boolean nextBoolean() { draws.add("boolean"); return booleanRoll; }

        List<String> draws() { return draws; }
    }
}
