package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.enchantment.WandEnchantmentRules;

/**
 * The SCEPTRE page and the JARLABEL page of the research book of the original.
 *
 * <p>Original {@code thaumcraft/common/config/ConfigResearch.java:110-144}: the SCEPTRE page holds
 * the three sceptre recipes as an array of arcane recipes. The first is the iron cap with the wood
 * rod, the second the gold cap with the greatwood rod and the third the thaumium cap with the
 * silverwood rod. Every one of them is the grid {@code " TF"}, {@code " RT"}, {@code "T  "} of two
 * wand caps, one wand rod and the primal charm, and every one of them costs
 * {@code (int)(capCraftCost * rodCraftCost * 1.5f)} of every primal aspect.</p>
 *
 * <p>Original {@code thaumcraft/common/config/ConfigRecipes.java:417-425} and
 * {@code ConfigResearch.java:302-305}: the JARLABEL page holds one recipe for every registered
 * aspect, each of them binding a blank jar label to a phial of eight units of that aspect. The
 * number of the entries is the number of the aspects of the 4.2.3.5 tree, which is
 * <b>forty eight</b> and not sixteen.</p>
 *
 * <p>No test constructs a {@code Block}: every rule under test is a pure static function
 * ({@link ThaumcraftRecipeDisplay#SCEPTRE_VARIANTS} and
 * {@link ThaumcraftRecipeDisplay#jarLabelAspects()}).</p>
 */
class ResearchRecipeArrayPageParityTest {
    @Test void theSceptrePageHasTheThreeVariantsOfTheOriginal() {
        // Original ConfigResearch:110-144, in the order of the original.
        assertEquals(List.of(
                new ThaumcraftRecipeDisplay.SceptreVariant("wood", "iron"),
                new ThaumcraftRecipeDisplay.SceptreVariant("greatwood", "gold"),
                new ThaumcraftRecipeDisplay.SceptreVariant("silverwood", "thaumium")),
                ThaumcraftRecipeDisplay.SCEPTRE_VARIANTS);
    }

    @Test void theSceptreVisCostIsTheOriginalFormula() {
        // Original ConfigResearch:112, :125 and :138:
        // cost = (int)((float)(WAND_CAP_X.getCraftCost() * WAND_ROD_Y.getCraftCost()) * 1.5f)
        // The iron cap costs one, the gold cap three and the thaumium cap six; the wood rod
        // costs one, the greatwood rod three and the silverwood rod nine.
        int[] expected = {1, 13, 81};
        for (int index = 0; index < ThaumcraftRecipeDisplay.SCEPTRE_VARIANTS.size(); index++) {
            var variant = ThaumcraftRecipeDisplay.SCEPTRE_VARIANTS.get(index);
            int cap = WandMaterials.cap(variant.cap()).craftCost();
            int rod = WandMaterials.rod(variant.rod()).craftCost();
            assertEquals(expected[index], WandEnchantmentRules.sceptreCost(cap, rod),
                    "the vis cost of the sceptre " + index + " of the original");
        }
    }

    @Test void theJarLabelPageHasOneEntryPerAspectOfTheOriginal() {
        // Original ConfigRecipes:417-425: the loop runs over Aspect.aspects.values().
        List<String> tags = ThaumcraftRecipeDisplay.jarLabelAspects();
        assertEquals(48, tags.size(), "the 4.2.3.5 tree has forty eight aspects, not sixteen");
        assertEquals(Aspect.aspects.size(), tags.size());
        // The order of the original is the registration order of the aspects.
        assertEquals("aer", tags.getFirst());
        assertEquals("vinculum", tags.getLast());
    }

    @Test void everyAspectOfThePageIsAKnownAspect() {
        for (String tag : ThaumcraftRecipeDisplay.jarLabelAspects()) {
            assertTrue(Aspect.getAspect(tag) != null, "the aspect of the jar label page is known: " + tag);
        }
    }
}
