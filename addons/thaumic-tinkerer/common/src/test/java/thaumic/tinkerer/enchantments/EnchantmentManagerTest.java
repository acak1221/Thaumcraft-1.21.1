package thaumic.tinkerer.enchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentData;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.lib.LibEnchantNames;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The table of the Enchanter (what it offers, what a level costs, which research opens it) and the rules of which
 * enchantments stand together, against the 1.7.10 {@code EnchantmentManager}.</p>
 */
class EnchantmentManagerTest {
    private static final ResourceKey<Enchantment> REPAIR = ThaumcraftApi.ENCHANTMENT_REPAIR;

    @BeforeAll
    static void init() {
        EnchantmentManager.initEnchantmentData();
    }

    private static int amount(ResourceKey<Enchantment> enchantment, int level, Aspect aspect) {
        EnchantmentData data = EnchantmentManager.data(enchantment, level);
        assertNotNull(data, enchantment.location() + " level " + level);
        return data.aspects.getAmount(aspect);
    }

    // ---- the table ------------------------------------------------------------------------------------------------

    @Test
    void theTableHasTheTwentyFourVanillaTheTwoOfThaumcraftAndTheFourteenOfTheAddOn() {
        List<ResourceKey<Enchantment>> all = EnchantmentManager.enchantments();
        assertEquals(40, all.size());
        assertEquals(40, all.stream().distinct().count(), "no enchantment twice (the original registered Dispersed Strikes twice)");
        for (String name : LibEnchantNames.ALL) {
            assertTrue(EnchantmentManager.isKnown(thaumic.tinkerer.common.lib.LibEnchantIDs.key(name)), name);
        }
        assertTrue(EnchantmentManager.isKnown(ThaumcraftApi.ENCHANTMENT_HASTE));
        assertTrue(EnchantmentManager.isKnown(REPAIR));
        // 1.21.1 enchantments that 1.7.10 did not have are not offered
        assertFalse(EnchantmentManager.isKnown(Enchantments.MENDING));
        assertFalse(EnchantmentManager.isKnown(Enchantments.SWEEPING_EDGE));
    }

    @Test
    void theCostOfALevelIsTheCostOfLevelOneTimesLevelTimesOnePlusAFifthOfTheLevel() {
        // Protection: Terra 10, Perditio 7. Level n: n * (1 + .2 n): 1.2, 2.8 (the sum is 2 * 1.4), 4.8, 7.2 -- cut per aspect
        assertEquals(12, amount(Enchantments.PROTECTION, 1, Aspect.EARTH));
        assertEquals(8, amount(Enchantments.PROTECTION, 1, Aspect.ENTROPY));
        assertEquals(28, amount(Enchantments.PROTECTION, 2, Aspect.EARTH));
        assertEquals(19, amount(Enchantments.PROTECTION, 2, Aspect.ENTROPY));
        assertEquals(72, amount(Enchantments.PROTECTION, 4, Aspect.EARTH));
        assertEquals(50, amount(Enchantments.PROTECTION, 4, Aspect.ENTROPY));
        // Sharpness 5: Ordo 10 * 5 * 2 = 100
        assertEquals(100, amount(Enchantments.SHARPNESS, 5, Aspect.ORDER));
        // Efficiency: Perditio 12, Terra 4
        assertEquals(120, amount(Enchantments.EFFICIENCY, 5, Aspect.ENTROPY));
        assertEquals(40, amount(Enchantments.EFFICIENCY, 5, Aspect.EARTH));
        // the TT ones
        assertEquals(9, amount(ModEnchantments.ascentBoost, 1, Aspect.ENTROPY));
        assertEquals(12, amount(ModEnchantments.ascentBoost, 1, Aspect.AIR));
        assertEquals(36, amount(ModEnchantments.autoSmelt, 1, Aspect.FIRE));
        assertEquals(19, amount(ModEnchantments.pounce, 1, Aspect.EARTH));
        assertEquals(19, amount(ModEnchantments.pounce, 1, Aspect.AIR));
        assertEquals(24 + 24 + 24 + 24 + 24 + 6, totalOf(EnchantmentManager.data(REPAIR, 1).aspects));
    }

    private static int totalOf(AspectList list) {
        int total = 0;
        for (Aspect aspect : list.getAspects()) total += list.getAmount(aspect);
        return total;
    }

    @Test
    void lureAndLuckOfTheSeaAskForBeastWhichNoWandCanGive() {
        EnchantmentData lure = EnchantmentManager.data(Enchantments.LURE, 1);
        assertEquals(24, lure.aspects.getAmount(Aspect.BEAST));
        assertFalse(Aspect.BEAST.isPrimal());
    }

    @Test
    void aLevelBelowOneAndAnUnknownEnchantmentHaveNoData() {
        assertNull(EnchantmentManager.data(Enchantments.SHARPNESS, 0));
        assertNull(EnchantmentManager.data(Enchantments.MENDING, 1));
    }

    @Test
    void theDataCarriesTheIconAndTheResearchOfTheEntry() {
        assertTrue(EnchantmentManager.data(Enchantments.SHARPNESS, 1).vanilla);
        assertEquals("", EnchantmentManager.data(Enchantments.SHARPNESS, 1).research);
        assertEquals("thaumictinkerer:textures/enchant/sharpness.png", EnchantmentManager.data(Enchantments.SHARPNESS, 1).texture.toString());
        assertFalse(EnchantmentManager.data(ModEnchantments.vampirism, 1).vanilla);
        assertEquals("thaumictinkerer:textures/enchant/vamprisim.png",
                EnchantmentManager.data(ModEnchantments.vampirism, 1).texture.toString(), "the 1.7.10 spelling of the file");
    }

    @Test
    void theResearchThatOpensAnEnchantmentIsTheOneOfTheOriginalIncludingTheCopiedVampirismEntries() {
        assertEquals(LibResearch.KEY_ENCHANT_ASCENT_BOOST, researchOf(ModEnchantments.ascentBoost));
        assertEquals(LibResearch.KEY_ENCHANT_SLOW_FALL, researchOf(ModEnchantments.slowFall));
        assertEquals(LibResearch.KEY_ENCHANT_AUTO_SMELT, researchOf(ModEnchantments.autoSmelt));
        assertEquals(LibResearch.KEY_ENCHANT_DESINTEGRATE, researchOf(ModEnchantments.desintegrate));
        assertEquals(LibResearch.KEY_ENCHANT_QUICK_DRAW, researchOf(ModEnchantments.quickDraw));
        assertEquals(LibResearch.KEY_ENCHANT_VAMPIRISM, researchOf(ModEnchantments.vampirism));
        // 1.7.10 registered these eight with the research of Vampirism
        for (ResourceKey<Enchantment> copied : List.of(ModEnchantments.focusedStrike, ModEnchantments.dispersedStrikes,
                ModEnchantments.valiance, ModEnchantments.finalStrike, ModEnchantments.tunnel, ModEnchantments.shatter,
                ModEnchantments.shockwave, ModEnchantments.pounce)) {
            assertEquals(LibResearch.KEY_ENCHANT_VAMPIRISM, researchOf(copied), copied.location().toString());
        }
        for (ResourceKey<Enchantment> open : List.of(Enchantments.PROTECTION, Enchantments.SILK_TOUCH, REPAIR,
                ThaumcraftApi.ENCHANTMENT_HASTE)) {
            assertEquals("", researchOf(open), open.location().toString());
        }
    }

    private static String researchOf(ResourceKey<Enchantment> enchantment) {
        return EnchantmentManager.data(enchantment, 1).research;
    }

    // ---- the rules ------------------------------------------------------------------------------------------------

    private static boolean forbids(ResourceKey<Enchantment> candidate, ResourceKey<Enchantment> chosen) {
        return EnchantmentManager.rulesForbid(candidate, List.of(chosen));
    }

    @Test
    void theRulesAreSymmetricForEveryPairOfTheTable() {
        List<ResourceKey<Enchantment>> all = EnchantmentManager.enchantments();
        for (ResourceKey<Enchantment> a : all) {
            for (ResourceKey<Enchantment> b : all) {
                assertEquals(forbids(a, b), forbids(b, a), a.location() + " / " + b.location());
            }
        }
    }

    @Test
    void unbreakingAndRepairGoWithEveryEnchantmentOfTheAddOn() {
        for (String name : LibEnchantNames.ALL) {
            ResourceKey<Enchantment> enchantment = thaumic.tinkerer.common.lib.LibEnchantIDs.key(name);
            assertFalse(forbids(enchantment, Enchantments.UNBREAKING), name + " with Unbreaking");
            assertFalse(forbids(enchantment, REPAIR), name + " with Repair");
        }
    }

    @Test
    void theToolEnchantmentsAcceptOnlyUnbreakingAndRepair() {
        for (ResourceKey<Enchantment> only : List.of(ModEnchantments.autoSmelt, ModEnchantments.desintegrate)) {
            for (ResourceKey<Enchantment> other : EnchantmentManager.enchantments()) {
                if (other.equals(only)) continue;
                boolean allowed = other.equals(Enchantments.UNBREAKING) || other.equals(REPAIR);
                assertEquals(!allowed, forbids(only, other), only.location() + " with " + other.location());
            }
        }
    }

    @Test
    void theStrikeEnchantmentsOfTheSwordFollowTheirOwnWhitelists() {
        // Dispersed and Focused: Unbreaking, Repair, Knockback, Looting
        for (ResourceKey<Enchantment> strike : List.of(ModEnchantments.dispersedStrikes, ModEnchantments.focusedStrike)) {
            assertFalse(forbids(strike, Enchantments.KNOCKBACK));
            assertFalse(forbids(strike, Enchantments.LOOTING));
            assertTrue(forbids(strike, Enchantments.SHARPNESS));
            assertTrue(forbids(strike, Enchantments.SMITE));
            assertTrue(forbids(strike, Enchantments.FIRE_ASPECT));
            assertTrue(forbids(strike, ModEnchantments.vampirism));
        }
        // Final Strike and Valiance: Unbreaking, Repair, Sharpness, Smite
        for (ResourceKey<Enchantment> strike : List.of(ModEnchantments.finalStrike, ModEnchantments.valiance)) {
            assertFalse(forbids(strike, Enchantments.SHARPNESS));
            assertFalse(forbids(strike, Enchantments.SMITE));
            assertTrue(forbids(strike, Enchantments.BANE_OF_ARTHROPODS), "the original listed Smite twice and never Bane");
            assertTrue(forbids(strike, Enchantments.KNOCKBACK));
            assertTrue(forbids(strike, Enchantments.LOOTING));
        }
        assertTrue(forbids(ModEnchantments.finalStrike, ModEnchantments.valiance));
        assertTrue(forbids(ModEnchantments.dispersedStrikes, ModEnchantments.focusedStrike));
    }

    @Test
    void vampirismRefusesFireAspectAndKnockbackOnly() {
        assertTrue(forbids(ModEnchantments.vampirism, Enchantments.FIRE_ASPECT));
        assertTrue(forbids(ModEnchantments.vampirism, Enchantments.KNOCKBACK));
        assertFalse(forbids(ModEnchantments.vampirism, Enchantments.SHARPNESS));
        assertFalse(forbids(ModEnchantments.vampirism, Enchantments.LOOTING));
    }

    @Test
    void theMovementEnchantmentsOfTheBootsExcludeFeatherFallingAndEachOther() {
        assertTrue(forbids(ModEnchantments.slowFall, Enchantments.FEATHER_FALLING));
        assertTrue(forbids(ModEnchantments.shockwave, Enchantments.FEATHER_FALLING));
        assertTrue(forbids(ModEnchantments.shockwave, ModEnchantments.slowFall));
        assertTrue(forbids(ModEnchantments.slowFall, ModEnchantments.shockwave), "the rule of Shockwave forbids it from the other side");
        assertFalse(forbids(ModEnchantments.slowFall, ThaumcraftApi.ENCHANTMENT_HASTE));
        assertFalse(forbids(ModEnchantments.shockwave, Enchantments.PROTECTION));
    }

    @Test
    void theDiggingEnchantmentsExcludeEfficiencyAndTunnelAndShatterExcludeEachOther() {
        assertTrue(forbids(ModEnchantments.shatter, Enchantments.EFFICIENCY));
        assertTrue(forbids(ModEnchantments.tunnel, Enchantments.EFFICIENCY));
        assertTrue(forbids(ModEnchantments.shatter, ModEnchantments.tunnel));
        assertTrue(forbids(ModEnchantments.shatter, ModEnchantments.desintegrate));
        assertTrue(forbids(ModEnchantments.tunnel, ModEnchantments.autoSmelt), "Flaming Touch takes only Unbreaking and Repair");
        assertFalse(forbids(ModEnchantments.shatter, Enchantments.FORTUNE));
        assertFalse(forbids(ModEnchantments.tunnel, Enchantments.SILK_TOUCH));
    }

    @Test
    void quickDrawRefusesPunchAndTheOthersAreFree() {
        assertTrue(forbids(ModEnchantments.quickDraw, Enchantments.PUNCH));
        assertFalse(forbids(ModEnchantments.quickDraw, Enchantments.POWER));
        assertFalse(forbids(ModEnchantments.quickDraw, Enchantments.FLAME));
        assertFalse(forbids(ModEnchantments.quickDraw, Enchantments.INFINITY));
    }

    @Test
    void ascentBoostAndPounceHaveNoRuleOfTheirOwn() {
        // only the whitelists of six other enchantments refuse them (an enchantment on the legs never meets those on a tool)
        List<ResourceKey<Enchantment>> whitelisted = List.of(ModEnchantments.autoSmelt, ModEnchantments.desintegrate,
                ModEnchantments.dispersedStrikes, ModEnchantments.focusedStrike, ModEnchantments.finalStrike, ModEnchantments.valiance);
        for (ResourceKey<Enchantment> other : EnchantmentManager.enchantments()) {
            if (whitelisted.contains(other)) continue;
            if (!other.equals(ModEnchantments.ascentBoost)) assertFalse(forbids(ModEnchantments.ascentBoost, other), other.location().toString());
            if (!other.equals(ModEnchantments.pounce)) assertFalse(forbids(ModEnchantments.pounce, other), other.location().toString());
        }
        for (ResourceKey<Enchantment> other : whitelisted) {
            assertTrue(forbids(ModEnchantments.pounce, other), other.location() + " takes nothing but its list");
        }
    }

    @Test
    void severalChosenEnchantmentsAreAllChecked() {
        List<ResourceKey<Enchantment>> chosen = List.of(Enchantments.SHARPNESS, Enchantments.KNOCKBACK);
        assertTrue(EnchantmentManager.rulesForbid(ModEnchantments.finalStrike, chosen), "Knockback is not on its list");
        assertTrue(EnchantmentManager.rulesForbid(ModEnchantments.dispersedStrikes, chosen), "Sharpness is not on its list");
        assertFalse(EnchantmentManager.rulesForbid(ModEnchantments.finalStrike, List.of(Enchantments.SHARPNESS, Enchantments.SMITE)));
    }
}
