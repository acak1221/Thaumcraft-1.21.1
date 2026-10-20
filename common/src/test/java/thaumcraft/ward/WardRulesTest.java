package thaumcraft.ward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.mods.ChampionWiringRules;
import thaumcraft.common.lib.ward.WardRules;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * The original TC4 wards and the absorption capacity of 1.21.1 that carries them.
 *
 * <p>1.21.1 clamps {@code LivingEntity#setAbsorptionAmount} by the
 * {@code generic.max_absorption} attribute, whose default is zero, so every ward of the original
 * has to be carried by a capacity of its own size.
 */
class WardRulesTest {
    @Test void everyOriginalWardFitsIntoTheCapacityOfItsMob() {
        // Oracle EntityEldritchWarden:318 - the warden takes two thirds of its maximum health.
        assertEquals(132, EldritchMobRules.wardenWardCap(200F));
        assertEquals(132.0, WardRules.capacity(EldritchMobRules.wardenWardCap(200F)), 0.0);
        // Oracle EntityUtils.makeChampion:271-274 - the warded champion takes half of its maximum
        // health.
        assertEquals(10, ChampionWiringRules.wardedChampionWard(20.0));
        assertEquals(10.0, WardRules.capacity(ChampionWiringRules.wardedChampionWard(20.0)), 0.0);
        // The warded praetor takes half of its maximum health as well.
        assertEquals(62.5F, CultistRules.championAbsorption(125.0F));
        assertEquals(62.5, WardRules.capacity(CultistRules.championAbsorption(125.0F)), 0.0);
    }

    @Test void aCapacityIsNeverNegative() {
        assertEquals(0.0, WardRules.capacity(-1F), 0.0);
        assertEquals(0.0, WardRules.capacity(0F), 0.0);
        assertEquals(2048.0, WardRules.capacity(2048F), 0.0);
    }
}
