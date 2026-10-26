package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.enchantment.WandEnchantmentRules;

/**
 * The craft cost of an assembled wand.
 *
 * <p>Original {@code thaumcraft/common/lib/crafting/ArcaneWandRecipe.java:59} writes {@code cc * cr}
 * to the item metadata of the assembled wand, and original
 * {@code thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java:64} writes
 * {@code (int) ((float) (cc * cr) * 1.5F)} for the sceptre. The dearest wand is the primal staff
 * (32) with a void cap (9), the dearest sceptre half as much again.</p>
 */
class WandCraftCostTest {
    @Test void theDearestSceptreCostsHalfAsMuchAgainAsTheDearestWand() {
        assertEquals(288, WandMaterials.assemblyCost("primal_staff", "void"), "the dearest plain wand");
        assertEquals(432, WandEnchantmentRules.sceptreCost(9, 32), "the dearest sceptre");
        assertEquals(432, WandEnchantmentRules.MAX_CRAFT_COST);
    }

    @Test void theCraftCostComponentHoldsEveryOriginalCraftCost() {
        // The component is the native form of the original item metadata; a sceptre
        // carries it too, so the range has to reach the dearest sceptre.
        assertTrue(ThaumcraftWands.craftCostCodec().encodeStart(JsonOps.INSTANCE, 288).result().isPresent(),
                "the dearest plain wand");
        assertTrue(ThaumcraftWands.craftCostCodec().encodeStart(JsonOps.INSTANCE, 432).result().isPresent(),
                "the dearest sceptre");
        // Add-on parts (WandRod/WandCap registered by another mod) may cost more than the native catalogue,
        // so the range is the original one: item metadata, written as a short (ArcaneWandRecipe:59-61).
        assertTrue(ThaumcraftWands.craftCostCodec().encodeStart(JsonOps.INSTANCE, 433).result().isPresent(),
                "an add-on wand dearer than the dearest native sceptre");
        assertTrue(ThaumcraftWands.craftCostCodec().encodeStart(JsonOps.INSTANCE, (int) Short.MAX_VALUE).result().isPresent(),
                "the largest original metadata");
        assertTrue(ThaumcraftWands.craftCostCodec().encodeStart(JsonOps.INSTANCE, Short.MAX_VALUE + 1).error().isPresent(),
                "nothing above the original short metadata");
        assertTrue(ThaumcraftWands.craftCostCodec().encodeStart(JsonOps.INSTANCE, -1).error().isPresent(), "never negative");
    }
}
