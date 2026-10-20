package thaumcraft.warp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.potions.PotionBlurredVision;
import thaumcraft.common.lib.potions.PotionDeathGaze;
import thaumcraft.common.lib.potions.PotionSunScorned;
import thaumcraft.common.lib.potions.PotionThaumarhia;
import thaumcraft.common.lib.potions.PotionUnnaturalHunger;
import thaumcraft.common.lib.potions.PotionWarpWard;

/** The six warp potions: their category, colour and how often each one acts, which is part of the original behaviour. */
class PotionEffectRulesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void everyPotionHasItsOriginalCategoryAndColour() {
        record Expected(MobEffect effect, MobEffectCategory category, int color) { }
        for (Expected expected : new Expected[] {
                new Expected(new PotionSunScorned(), MobEffectCategory.HARMFUL, 16308330),
                new Expected(new PotionThaumarhia(), MobEffectCategory.HARMFUL, 0x664477),
                new Expected(new PotionUnnaturalHunger(), MobEffectCategory.HARMFUL, 0x446633),
                new Expected(new PotionBlurredVision(), MobEffectCategory.HARMFUL, 0x808080),
                new Expected(new PotionDeathGaze(), MobEffectCategory.HARMFUL, 0x664433),
                new Expected(new PotionWarpWard(), MobEffectCategory.BENEFICIAL, 14742263)}) {
            String name = expected.effect().getClass().getSimpleName();
            assertEquals(expected.category(), expected.effect().getCategory(), name);
            assertEquals(expected.color(), expected.effect().getColor(), name);
        }
    }

    @Test void sunScorchActsEveryFortyTicksWhateverTheAmplifier() {
        MobEffect effect = new PotionSunScorned();
        for (int amplifier = 0; amplifier < 4; amplifier++) {
            for (int duration = 0; duration <= 200; duration++) assertEquals(duration % 40 == 0, effect.shouldApplyEffectTickThisTick(duration, amplifier), duration + "/" + amplifier);
        }
    }

    @Test void thaumarhiaActsEveryTwentyTicks() {
        MobEffect effect = new PotionThaumarhia();
        for (int duration = 0; duration <= 200; duration++) assertEquals(duration % 20 == 0, effect.shouldApplyEffectTickThisTick(duration, 0), String.valueOf(duration));
    }

    @Test void unnaturalHungerActsOnEveryTickUnlikeTheVanillaHungerEffect() {
        MobEffect effect = new PotionUnnaturalHunger();
        for (int duration = 0; duration <= 100; duration++) assertTrue(effect.shouldApplyEffectTickThisTick(duration, 0), String.valueOf(duration));
    }

    @Test void thePurelyPassiveMarkersNeverTick() {
        for (MobEffect marker : new MobEffect[] {new PotionBlurredVision(), new PotionDeathGaze(), new PotionWarpWard()}) {
            for (int duration = 0; duration <= 100; duration++) assertFalse(marker.shouldApplyEffectTickThisTick(duration, 0), marker.getClass().getSimpleName() + " " + duration);
        }
    }
}
