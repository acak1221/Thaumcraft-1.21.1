package thaumcraft.baubles;

import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.Test;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.items.baubles.ItemRunicBauble;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandRodPassive;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.AnyPrimalRefill;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Cap;
import thaumcraft.common.lib.baubles.RunicBaubleRules.CultistPiece;
import thaumcraft.common.lib.baubles.RunicBaubleRules.DyeShare;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Family;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Rod;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original accessory numbers: the runic amulet, ring and girdle, the nine blank baubles, the ten cultist armour pieces, the
 * wand rod and cap metadata and the rod refill. Original sources: ItemAmuletRunic, ItemRingRunic, ItemGirdleRunic,
 * ItemBaubleBlanks, ItemCultistBoots, ItemCultistRobeArmor, ItemCultistPlateArmor, ItemCultistLeaderArmor, ItemWandRod,
 * ItemWandCap, ConfigItems:301-334 and WandRodPrimalOnUpdate.
 */
class RunicBaubleRulesTest {
    /** A fixed aspect rod never draws a random number: asking fails the test. */
    private static final IntUnaryOperator NO_PICK = bound -> { throw new AssertionError("nothing may be drawn"); };

    @Test void theRunicAccessoriesKeepTheirOriginalChargeSlotAndRarity() {
        assertEquals(8, RunicBaubleRules.runicCharge(Family.AMULET, 0));
        assertEquals(7, RunicBaubleRules.runicCharge(Family.AMULET, 1));
        assertEquals(1, RunicBaubleRules.runicCharge(Family.RING, 0));
        assertEquals(5, RunicBaubleRules.runicCharge(Family.RING, 1));
        assertEquals(4, RunicBaubleRules.runicCharge(Family.RING, 2));
        assertEquals(4, RunicBaubleRules.runicCharge(Family.RING, 3));
        assertEquals(10, RunicBaubleRules.runicCharge(Family.GIRDLE, 0));
        assertEquals(9, RunicBaubleRules.runicCharge(Family.GIRDLE, 1));
        assertEquals(BaubleType.AMULET, RunicBaubleRules.slotType(Family.AMULET, 0));
        assertEquals(BaubleType.AMULET, RunicBaubleRules.slotType(Family.AMULET, 1));
        assertEquals(BaubleType.RING, RunicBaubleRules.slotType(Family.RING, 0));
        assertEquals(BaubleType.BELT, RunicBaubleRules.slotType(Family.GIRDLE, 1));
        assertEquals(Rarity.UNCOMMON, RunicBaubleRules.runicRarity(Family.RING, 0), "only the lesser ring is uncommon");
        assertEquals(Rarity.RARE, RunicBaubleRules.runicRarity(Family.RING, 1));
        assertEquals(Rarity.RARE, RunicBaubleRules.runicRarity(Family.AMULET, 0));
        assertEquals(Rarity.RARE, RunicBaubleRules.runicRarity(Family.GIRDLE, 0));
        assertEquals(ItemRunicBauble.Upgrade.NONE, RunicBaubleRules.runicUpgrade(Family.AMULET, 0));
        assertEquals(ItemRunicBauble.Upgrade.EMERGENCY, RunicBaubleRules.runicUpgrade(Family.AMULET, 1));
        assertEquals(ItemRunicBauble.Upgrade.CHARGED, RunicBaubleRules.runicUpgrade(Family.RING, 2));
        assertEquals(ItemRunicBauble.Upgrade.HEALING, RunicBaubleRules.runicUpgrade(Family.RING, 3));
        assertEquals(ItemRunicBauble.Upgrade.KINETIC, RunicBaubleRules.runicUpgrade(Family.GIRDLE, 1));
        assertEquals(8, RunicBaubleRules.runicAccessories().size());
        assertThrows(IllegalArgumentException.class, () -> RunicBaubleRules.runic(Family.RING, 4));
    }

    @Test void theBlankBaublesKeepTheirSlotAndTheirOnePercentPrimalDiscount() {
        assertEquals(BaubleType.AMULET, RunicBaubleRules.blank(0).type());
        assertEquals(BaubleType.RING, RunicBaubleRules.blank(1).type());
        assertEquals(BaubleType.BELT, RunicBaubleRules.blank(2).type());
        for (int metadata = 3; metadata <= 8; metadata++) assertEquals(BaubleType.RING, RunicBaubleRules.blank(metadata).type(), "metadata " + metadata);
        assertEquals(0, RunicBaubleRules.blankVisDiscount(0, "aer"), "the mundane amulet discounts nothing");
        assertEquals(0, RunicBaubleRules.blankVisDiscount(1, "aer"));
        assertEquals(0, RunicBaubleRules.blankVisDiscount(2, "aer"));
        assertEquals(1, RunicBaubleRules.blankVisDiscount(3, "aer"));
        assertEquals(0, RunicBaubleRules.blankVisDiscount(3, "terra"), "only its own primal is discounted");
        assertEquals(1, RunicBaubleRules.blankVisDiscount(8, "perditio"));
        assertEquals(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"), RunicBaubleRules.PRIMALS, "the original primal order");
        for (int index = 0; index < RunicBaubleRules.PRIMALS.size(); index++)
            assertEquals(RunicBaubleRules.PRIMALS.get(index), RunicBaubleRules.blank(3 + index).discountAspect(), "metadata " + (3 + index));
        assertEquals(9, RunicBaubleRules.blankBaubles().size());
        assertThrows(IllegalArgumentException.class, () -> RunicBaubleRules.blank(9));
    }

    @Test void theCultistArmourKeepsItsOriginalProtectionDurabilityRarityDiscountAndWarp() {
        assertEquals(10, RunicBaubleRules.cultistArmor().size());
        CultistPiece hood = RunicBaubleRules.cultist("cultist_robe_hood");
        assertEquals(ArmorItem.Type.HELMET, hood.type());
        assertEquals(2, hood.protection(), "the vanilla iron protection of the hood");
        assertEquals(165, hood.durability(), "the iron durability factor 15");
        assertEquals(0, hood.toughness(), "the original 1.7.10 has no armour toughness");
        assertEquals(Rarity.UNCOMMON, hood.rarity());
        assertEquals(1, hood.visDiscount());
        assertEquals(1, hood.warp());
        CultistPiece robe = RunicBaubleRules.cultist("cultist_robe");
        assertEquals(ArmorItem.Type.CHESTPLATE, robe.type());
        assertEquals(6, robe.protection());
        assertEquals(240, robe.durability());
        assertEquals(1, robe.visDiscount());
        assertEquals(1, robe.warp());
        CultistPiece legs = RunicBaubleRules.cultist("cultist_robe_legs");
        assertEquals(ArmorItem.Type.LEGGINGS, legs.type());
        assertEquals(5, legs.protection());
        assertEquals(225, legs.durability());
        CultistPiece helm = RunicBaubleRules.cultist("cultist_plate_helm");
        assertEquals(2, helm.protection());
        assertEquals(165, helm.durability());
        assertEquals(Rarity.UNCOMMON, helm.rarity());
        assertEquals(0, helm.visDiscount(), "the plate has no vis discount");
        assertEquals(0, helm.warp(), "the plate has no warp");
        assertEquals(6, RunicBaubleRules.cultist("cultist_plate").protection());
        assertEquals(5, RunicBaubleRules.cultist("cultist_plate_legs").protection());
        CultistPiece praetor = RunicBaubleRules.cultist("cultist_leader_helm");
        assertEquals(3, praetor.protection(), "the fortress protection of the Praetor");
        assertEquals(440, praetor.durability(), "the durability factor 40");
        assertEquals(Rarity.RARE, praetor.rarity(), "the Praetor armour is rare");
        assertEquals(0, praetor.visDiscount());
        assertEquals(0, praetor.warp());
        assertEquals(7, RunicBaubleRules.cultist("cultist_leader").protection());
        assertEquals(640, RunicBaubleRules.cultist("cultist_leader").durability());
        assertEquals(6, RunicBaubleRules.cultist("cultist_leader_legs").protection());
        assertEquals(600, RunicBaubleRules.cultist("cultist_leader_legs").durability());
        CultistPiece boots = RunicBaubleRules.cultist("cultist_boots");
        assertEquals(ArmorItem.Type.BOOTS, boots.type());
        assertEquals(2, boots.protection());
        assertEquals(165, boots.durability());
        assertEquals(Rarity.UNCOMMON, boots.rarity());
        assertEquals(1, boots.visDiscount());
        assertEquals(1, boots.warp());
        assertThrows(IllegalArgumentException.class, () -> RunicBaubleRules.cultist("cultist_robes"));
    }

    @Test void theWandRodMetadataKeepsItsOriginalCapability() {
        assertEquals("greatwood", RunicBaubleRules.rod(0).tag());
        assertEquals("obsidian", RunicBaubleRules.rod(1).tag());
        assertEquals("silverwood", RunicBaubleRules.rod(2).tag());
        assertEquals("ice", RunicBaubleRules.rod(3).tag());
        assertEquals("quartz", RunicBaubleRules.rod(4).tag());
        assertEquals("reed", RunicBaubleRules.rod(5).tag());
        assertEquals("blaze", RunicBaubleRules.rod(6).tag());
        assertEquals("bone", RunicBaubleRules.rod(7).tag());
        assertEquals("greatwood_staff", RunicBaubleRules.rod(50).tag());
        assertEquals("obsidian_staff", RunicBaubleRules.rod(51).tag());
        assertEquals("silverwood_staff", RunicBaubleRules.rod(52).tag());
        assertEquals("ice_staff", RunicBaubleRules.rod(53).tag());
        assertEquals("quartz_staff", RunicBaubleRules.rod(54).tag());
        assertEquals("reed_staff", RunicBaubleRules.rod(55).tag());
        assertEquals("blaze_staff", RunicBaubleRules.rod(56).tag());
        assertEquals("bone_staff", RunicBaubleRules.rod(57).tag());
        assertEquals("primal_staff", RunicBaubleRules.rod(100).tag());
        assertEquals(25, RunicBaubleRules.rodCapacity(-1), "the mundane wood rod is a stick");
        assertEquals(1, RunicBaubleRules.rodCraftCost(-1));
        assertEquals(50, RunicBaubleRules.rodCapacity(0));
        assertEquals(3, RunicBaubleRules.rodCraftCost(0));
        assertEquals(100, RunicBaubleRules.rodCapacity(2));
        assertEquals(9, RunicBaubleRules.rodCraftCost(2));
        assertEquals(75, RunicBaubleRules.rodCapacity(1));
        assertEquals(6, RunicBaubleRules.rodCraftCost(1));
        assertEquals(125, RunicBaubleRules.rodCapacity(50));
        assertEquals(8, RunicBaubleRules.rodCraftCost(50));
        assertEquals(175, RunicBaubleRules.rodCapacity(51));
        assertEquals(14, RunicBaubleRules.rodCraftCost(51));
        assertEquals(250, RunicBaubleRules.rodCapacity(52));
        assertEquals(24, RunicBaubleRules.rodCraftCost(52));
        assertEquals(250, RunicBaubleRules.rodCapacity(100));
        assertEquals(32, RunicBaubleRules.rodCraftCost(100), "the dearest rod in the game");
        assertEquals("terra", RunicBaubleRules.rodPassive(1));
        assertEquals("aqua", RunicBaubleRules.rodPassive(3));
        assertEquals("ordo", RunicBaubleRules.rodPassive(4));
        assertEquals("aer", RunicBaubleRules.rodPassive(5));
        assertEquals("ignis", RunicBaubleRules.rodPassive(6));
        assertEquals("perditio", RunicBaubleRules.rodPassive(7));
        assertNull(RunicBaubleRules.rodPassive(0));
        assertEquals(RunicBaubleRules.ANY_PRIMAL, RunicBaubleRules.rodPassive(100), "the primal staff refills any primal");
        assertTrue(RunicBaubleRules.rod(6).glowing(), "the blaze rod glows");
        assertTrue(RunicBaubleRules.rod(56).glowing(), "the blaze staff core glows");
        assertTrue(RunicBaubleRules.rod(100).runes(), "the primal staff core is runed");
        assertFalse(RunicBaubleRules.rod(0).runes());
        // The native material catalogue holds the same numbers in centivis.
        for (Rod rod : RunicBaubleRules.rods()) {
            var catalogue = WandMaterials.rod(rod.tag());
            assertEquals(rod.capacity() * 100, catalogue.capacity(), rod.tag());
            assertEquals(rod.craftCost(), catalogue.craftCost(), rod.tag());
            assertEquals(rod.passive(), catalogue.passive(), rod.tag());
        }
        assertEquals(RunicBaubleRules.ANY_PRIMAL, WandMaterials.ANY_PRIMAL);
    }

    @Test void theWandCapMetadataKeepsItsOriginalCostModifier() {
        assertEquals("iron", RunicBaubleRules.cap(0).tag());
        assertEquals("gold", RunicBaubleRules.cap(1).tag());
        assertEquals("thaumium", RunicBaubleRules.cap(2).tag());
        assertEquals("copper", RunicBaubleRules.cap(3).tag());
        assertEquals("silver", RunicBaubleRules.cap(4).tag());
        assertEquals("silver_inert", RunicBaubleRules.cap(5).tag());
        assertEquals("thaumium_inert", RunicBaubleRules.cap(6).tag());
        assertEquals("void", RunicBaubleRules.cap(7).tag());
        assertEquals("void_inert", RunicBaubleRules.cap(8).tag());
        assertEquals(1.1F, RunicBaubleRules.capModifier(0), 0F);
        assertEquals(1.0F, RunicBaubleRules.capModifier(1), 0F);
        assertEquals(0.9F, RunicBaubleRules.capModifier(2), 0F);
        assertEquals(0.8F, RunicBaubleRules.capModifier(7), 0F, "the void cap is the cheapest to use");
        assertEquals(1, RunicBaubleRules.capCraftCost(0));
        assertEquals(3, RunicBaubleRules.capCraftCost(1));
        assertEquals(6, RunicBaubleRules.capCraftCost(2));
        assertEquals(9, RunicBaubleRules.capCraftCost(7));
        assertEquals(2, RunicBaubleRules.capCraftCost(3), "the copper cap of another mod");
        assertEquals(4, RunicBaubleRules.capCraftCost(4), "the silver cap of another mod");
        assertTrue(RunicBaubleRules.cap(0).charged());
        assertFalse(RunicBaubleRules.cap(5).charged(), "the inert caps are uncharged");
        assertFalse(RunicBaubleRules.cap(6).charged());
        assertFalse(RunicBaubleRules.cap(8).charged());
        assertEquals(List.of("ordo", "perditio"), RunicBaubleRules.cap(3).specialAspects());
        assertEquals(1.0F, RunicBaubleRules.cap(3).specialModifier(), 0F);
        assertEquals(List.of("aer", "terra", "ignis", "aqua"), RunicBaubleRules.cap(4).specialAspects());
        assertEquals(0.95F, RunicBaubleRules.cap(4).specialModifier(), 0F);
        // The native material catalogue holds the same numbers for the four ordinary caps.
        for (Cap cap : RunicBaubleRules.caps()) {
            if (!WandMaterials.supportsCap(cap.tag())) continue;
            assertEquals(cap.modifier(), WandMaterials.capModifier(cap.tag()), 0F, cap.tag());
            assertEquals(cap.craftCost(), WandMaterials.capCraftCost(cap.tag()), cap.tag());
        }
    }

    @Test void theWandPartsArePlainStackableItemsThatNeverWearOut() {
        assertEquals(64, RunicBaubleRules.WAND_PART_STACK_SIZE);
    }

    @Test void aFixedAspectRodRefillsItsAspectOnEveryTwoHundredthTickBelowATenth() {
        assertEquals(200, RunicBaubleRules.FIXED_ASPECT_PERIOD);
        assertEquals(50, RunicBaubleRules.ANY_PRIMAL_PERIOD);
        assertEquals(100, RunicBaubleRules.VIS_STEP, "one whole vis in centivis");
        assertEquals(750, RunicBaubleRules.refillThreshold(7500));
        assertEquals(100, RunicBaubleRules.fixedAspectRefill(200, 7500, 0, "terra"));
        assertEquals(100, RunicBaubleRules.fixedAspectRefill(0, 7500, 0, "terra"), "tick zero counts too");
        assertEquals(100, RunicBaubleRules.fixedAspectRefill(400, 7500, 0, "terra"));
        assertEquals(0, RunicBaubleRules.fixedAspectRefill(50, 7500, 0, "terra"));
        assertEquals(0, RunicBaubleRules.fixedAspectRefill(199, 7500, 0, "terra"));
        assertEquals(750, RunicBaubleRules.fixedAspectRefill(200, 7500, 750, "terra"), "at a tenth it stops");
        assertEquals(849, RunicBaubleRules.fixedAspectRefill(200, 7500, 749, "terra"), "just below the tenth it still refills");
        assertEquals(0, RunicBaubleRules.fixedAspectRefill(200, 7500, 0, null), "a rod without a passive never refills");
        // The native wand passive agrees with the original rule.
        assertEquals(RunicBaubleRules.FIXED_ASPECT_PERIOD, WandRodPassive.PERIOD);
        assertEquals(RunicBaubleRules.ANY_PRIMAL_PERIOD, WandRodPassive.PRIMAL_PERIOD);
        assertEquals(RunicBaubleRules.VIS_STEP, WandRodPassive.STEP);
        WandData wand = new WandData("obsidian", "iron", Map.of());
        assertEquals(RunicBaubleRules.fixedAspectRefill(200, wand.capacity(), wand.amount("terra"), "terra"),
                WandRodPassive.tick(wand, 200, NO_PICK).amount("terra"));
    }

    @Test void thePrimalStaffRefillsOneLowPrimalOnEveryFiftiethTick() {
        Map<String, Integer> empty = Map.of();
        AnyPrimalRefill refill = RunicBaubleRules.anyPrimalRefill(50, 25000, empty, bound -> 0);
        assertTrue(refill.due());
        assertEquals("aer", refill.aspect(), "the first low primal in the original order");
        assertEquals(100, refill.amount());
        assertFalse(RunicBaubleRules.anyPrimalRefill(49, 25000, empty, NO_PICK).due());
        assertTrue(RunicBaubleRules.anyPrimalRefill(0, 25000, empty, bound -> 0).due(), "tick zero counts too, like the original modulo");
        // Only the primals below a tenth of the capacity are offered.
        int threshold = RunicBaubleRules.refillThreshold(25000);
        Map<String, Integer> mixed = Map.of("aer", threshold, "terra", threshold + 1, "ignis", 25000, "aqua", threshold);
        AnyPrimalRefill low = RunicBaubleRules.anyPrimalRefill(50, 25000, mixed, bound -> 0);
        assertTrue(low.due());
        assertEquals("ordo", low.aspect());
        assertEquals(100, low.amount());
        // Nothing is low: the staff leaves the wand alone and draws no number.
        Map<String, Integer> satisfied = Map.of("aer", threshold, "terra", threshold, "ignis", threshold, "aqua", threshold,
                "ordo", threshold, "perditio", threshold);
        assertFalse(RunicBaubleRules.anyPrimalRefill(50, 25000, satisfied, NO_PICK).due());
    }

    @Test void theRobeDyeGivesEveryContributorOneShareOfTheColour() {
        // A black robe and a white dye: both take a share, the shares are averaged and scaled to the average brightness.
        assertEquals(0x7F7F7F, RunicBaubleRules.mixRobeColor(List.of(DyeShare.robe(0x000000), DyeShare.dye(0))));
        // The robe alone keeps its own colour.
        assertEquals(0x6A3880, RunicBaubleRules.mixRobeColor(List.of(DyeShare.robe(0x6A3880))));
        // The default robe and a red dye: the original arithmetic, share by share.
        assertEquals(0x8C3960, RunicBaubleRules.mixRobeColor(List.of(DyeShare.robe(0x6A3880), DyeShare.dye(14))));
        // The order of the shares does not change the result: every channel divides exactly.
        assertEquals(RunicBaubleRules.mixRobeColor(List.of(DyeShare.robe(0x6A3880), DyeShare.dye(14))),
                RunicBaubleRules.mixRobeColor(List.of(DyeShare.dye(14), DyeShare.robe(0x6A3880))));
        // A grey dye shares its own colour with the robe: the shares are averaged and scaled to the average brightness.
        assertEquals(0x5B4266, RunicBaubleRules.mixRobeColor(List.of(DyeShare.robe(0x6A3880), DyeShare.dye(7))));
    }
}
