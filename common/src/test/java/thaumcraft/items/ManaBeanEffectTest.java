package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.ItemManaBean;

/** The mana bean lottery: slots 1..23 are the 1.7 potion ids in order, 0 and 24..30 are the eight empty slots. */
class ManaBeanEffectTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void slotsOneToTwentyThreeAreTheOriginalPotionIdsInOrder() {
        List<Holder<MobEffect>> original1_7 = List.of(
                MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SPEED, MobEffects.DIG_SLOWDOWN, MobEffects.DAMAGE_BOOST,
                MobEffects.HEAL, MobEffects.HARM, MobEffects.JUMP, MobEffects.CONFUSION, MobEffects.REGENERATION, MobEffects.DAMAGE_RESISTANCE,
                MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING, MobEffects.INVISIBILITY, MobEffects.BLINDNESS, MobEffects.NIGHT_VISION,
                MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER, MobEffects.HEALTH_BOOST, MobEffects.ABSORPTION,
                MobEffects.SATURATION);
        for (int slot = 1; slot <= 23; slot++) assertSame(original1_7.get(slot - 1), ItemManaBean.effectAt(slot), "slot " + slot);
    }

    @Test void theGapBetweenTheVanillaAndTheThaumcraftEffectsIsEmpty() {
        assertNull(ItemManaBean.effectAt(0));
        for (int slot = 24; slot <= 30; slot++) assertNull(ItemManaBean.effectAt(slot), "slot " + slot);
    }

    @Test void slotsOutsideTheLotteryHaveNoEffect() {
        for (int slot : new int[] {-1, -100, 40, 41, 1000, Integer.MAX_VALUE, Integer.MIN_VALUE}) assertNull(ItemManaBean.effectAt(slot), "slot " + slot);
    }

    @Test void theInstantAndPassiveEffectsAreTheOnesTheEatingCodeSpecialCases() {
        assertNotNull(ItemManaBean.effectAt(6));
        assertSame(true, ItemManaBean.effectAt(6).value().isInstantenous());
        assertSame(true, ItemManaBean.effectAt(7).value().isInstantenous());
        assertSame(true, ItemManaBean.effectAt(23).value().isInstantenous(), "saturation is the one instant slot the eater leaves a no-op (slot 23)");
        assertSame(false, ItemManaBean.effectAt(10).value().isInstantenous());
    }
}
