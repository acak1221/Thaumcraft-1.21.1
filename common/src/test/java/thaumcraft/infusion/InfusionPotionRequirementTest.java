package thaumcraft.infusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.crafting.InfusionPotionRequirement;
import thaumcraft.common.lib.loot.LootPotionVariants;

/** The four potions the runic accessory upgrades demand, named by their 1.7 metadata values. */
class InfusionPotionRequirementTest {
    @Test void onlyTheFourOriginalPotionsAreKnown() {
        for (String id : new String[] {"minecraft:strong_strength", "minecraft:strong_swiftness", "minecraft:long_regeneration", "minecraft:strong_harming"}) assertTrue(InfusionPotionRequirement.known(id), id);
        for (String id : new String[] {"minecraft:strength", "minecraft:long_strength", "minecraft:healing", "minecraft:strong_healing", "thaumcraft:strong_strength", "", "strong_strength"}) assertFalse(InfusionPotionRequirement.known(id), id);
    }

    @Test void eachPotionResolvesToItsOriginalMetadataWeightAndStrength() {
        LootPotionVariants.Variant strength = InfusionPotionRequirement.legacyVariant("minecraft:strong_strength");
        assertEquals(8233, strength.metadata());
        assertEquals(5, strength.effectId());
        assertEquals(1, strength.amplifier());
        assertEquals(1800, strength.duration());
        LootPotionVariants.Variant swiftness = InfusionPotionRequirement.legacyVariant("minecraft:strong_swiftness");
        assertEquals(8226, swiftness.metadata());
        assertEquals(1, swiftness.effectId());
        assertEquals(1, swiftness.amplifier());
        assertEquals(1800, swiftness.duration());
        LootPotionVariants.Variant regeneration = InfusionPotionRequirement.legacyVariant("minecraft:long_regeneration");
        assertEquals(8257, regeneration.metadata());
        assertEquals(10, regeneration.effectId());
        assertEquals(0, regeneration.amplifier());
        assertEquals(2400, regeneration.duration());
        assertEquals(3, regeneration.weight());
    }

    @Test void theHarmingPotionIsTheSplashVariantWithTheInstantEffectDuration() {
        LootPotionVariants.Variant harming = InfusionPotionRequirement.legacyVariant("minecraft:strong_harming");
        assertEquals(16428, harming.metadata());
        assertTrue(harming.splash());
        assertEquals(7, harming.effectId());
        assertEquals(1, harming.amplifier());
        assertEquals(1, harming.duration());
        assertFalse(InfusionPotionRequirement.legacyVariant("minecraft:strong_strength").splash());
    }

    @Test void anyOtherPotionHasNoLegacyVariant() {
        for (String id : new String[] {"minecraft:strength", "minecraft:water", "", "strong_strength"}) assertThrows(IllegalArgumentException.class, () -> InfusionPotionRequirement.legacyVariant(id), id);
    }

    @Test void everyKnownPotionIsOneOfTheSeventyTwoLootVariants() {
        for (String id : new String[] {"minecraft:strong_strength", "minecraft:strong_swiftness", "minecraft:long_regeneration", "minecraft:strong_harming"}) {
            assertTrue(LootPotionVariants.all().contains(InfusionPotionRequirement.legacyVariant(id)), id);
        }
    }
}
