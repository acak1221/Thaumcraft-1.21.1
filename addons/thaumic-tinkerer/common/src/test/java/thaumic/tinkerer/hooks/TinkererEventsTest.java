package thaumic.tinkerer.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.architectury.event.EventResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.client.hooks.ItemRenderers;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.common.hooks.TinkererEvents.FallData;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The rules of the loader-neutral events (order of the chain, what zero means, what a single listener or no listener
 * does), with listeners that ignore their game arguments, so no world is needed. That the loader posts them at the right
 * places is checked by the GameTests of {@code HooksRuntimeChecks}, not here.</p>
 */
class TinkererEventsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final float EPSILON = 1.0E-4F;

    // ---- LIVING_DAMAGE ---------------------------------------------------------------------------------------------

    @Test
    void withoutListenersTheDamageIsLeftAlone() {
        assertEquals(10F, TinkererEvents.postLivingDamage(null, null, 10F), EPSILON);
    }

    @Test
    void listenersChainInTheOrderOfRegistration() {
        TinkererEvents.LivingDamage doubled = (victim, source, amount) -> amount * 2F;
        TinkererEvents.LivingDamage minusThree = (victim, source, amount) -> amount - 3F;
        TinkererEvents.LIVING_DAMAGE.register(doubled);
        TinkererEvents.LIVING_DAMAGE.register(minusThree);
        try {
            assertEquals(17F, TinkererEvents.postLivingDamage(null, null, 10F), EPSILON, "(10 * 2) - 3");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(doubled);
            TinkererEvents.LIVING_DAMAGE.unregister(minusThree);
        }
        TinkererEvents.LIVING_DAMAGE.register(minusThree);
        TinkererEvents.LIVING_DAMAGE.register(doubled);
        try {
            assertEquals(14F, TinkererEvents.postLivingDamage(null, null, 10F), EPSILON, "(10 - 3) * 2");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(doubled);
            TinkererEvents.LIVING_DAMAGE.unregister(minusThree);
        }
        assertEquals(10F, TinkererEvents.postLivingDamage(null, null, 10F), EPSILON, "unregistered: back to the plain amount");
    }

    @Test
    void zeroCancelsAndStopsTheChain() {
        int[] later = { 0 };
        TinkererEvents.LivingDamage cancel = (victim, source, amount) -> 0F;
        TinkererEvents.LivingDamage counter = (victim, source, amount) -> {
            later[0]++;
            return amount;
        };
        TinkererEvents.LIVING_DAMAGE.register(cancel);
        TinkererEvents.LIVING_DAMAGE.register(counter);
        try {
            assertEquals(0F, TinkererEvents.postLivingDamage(null, null, 10F), 0F);
            assertEquals(0, later[0], "the listener after the cancel is not called");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(cancel);
            TinkererEvents.LIVING_DAMAGE.unregister(counter);
        }
    }

    @Test
    void aSingleListenerIsNormalisedLikeAChain() {
        // Architectury hands a lone listener out as the invoker itself, so the poster must do the normalising.
        for (float bad : new float[] { 0F, -4F, Float.NaN }) {
            TinkererEvents.LivingDamage listener = (victim, source, amount) -> bad;
            TinkererEvents.LIVING_DAMAGE.register(listener);
            try {
                assertEquals(0F, TinkererEvents.postLivingDamage(null, null, 10F), 0F, "result " + bad + " cancels");
            } finally {
                TinkererEvents.LIVING_DAMAGE.unregister(listener);
            }
        }
    }

    @Test
    void anAmountOfNoDamageIsNotPosted() {
        int[] calls = { 0 };
        TinkererEvents.LivingDamage listener = (victim, source, amount) -> {
            calls[0]++;
            return amount + 5F;
        };
        TinkererEvents.LIVING_DAMAGE.register(listener);
        try {
            assertEquals(0F, TinkererEvents.postLivingDamage(null, null, 0F), 0F);
            assertEquals(0, calls[0], "a snowball's zero damage reaches nobody");
        } finally {
            TinkererEvents.LIVING_DAMAGE.unregister(listener);
        }
    }

    // ---- LIVING_ARMOR_ABSORB ---------------------------------------------------------------------------------------

    /** A damage source that answers the one tag test the poster makes (the registries of a data pack are not loaded in a unit test). */
    private static DamageSource source(boolean bypassesArmor) {
        return new DamageSource(Holder.direct(new DamageType("test", 0.0F))) {
            @Override
            public boolean is(TagKey<DamageType> tag) {
                return bypassesArmor && tag == DamageTypeTags.BYPASSES_ARMOR;
            }
        };
    }

    @Test
    void armorAbsorbWithoutListenersIsTheSameValue() {
        float absorbed = 5.8F;
        float result = TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, absorbed);
        assertEquals(Float.floatToRawIntBits(absorbed), Float.floatToRawIntBits(result), "nothing listens: the game's own value, bit for bit");
    }

    @Test
    void armorAbsorbChainsAndEachListenerSeesWhatTheLastReturned() {
        List<Float> seen = new ArrayList<>();
        TinkererEvents.LivingArmorAbsorb replace = (victim, source, damage, absorbed) -> {
            seen.add(damage);
            seen.add(absorbed);
            return damage * 0.85F;
        };
        TinkererEvents.LivingArmorAbsorb addOne = (victim, source, damage, absorbed) -> {
            seen.add(absorbed);
            return absorbed + 1F;
        };
        TinkererEvents.LIVING_ARMOR_ABSORB.register(replace);
        TinkererEvents.LIVING_ARMOR_ABSORB.register(addOne);
        try {
            assertEquals(9.5F, TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, 4F), EPSILON, "8.5 replaced the 4, then +1");
            assertEquals(List.of(10F, 4F, 8.5F), seen, "the damage, the vanilla amount, then what the first returned");
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(replace);
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(addOne);
        }
        seen.clear();
        TinkererEvents.LIVING_ARMOR_ABSORB.register(addOne);
        TinkererEvents.LIVING_ARMOR_ABSORB.register(replace);
        try {
            assertEquals(8.5F, TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, 4F), EPSILON, "the last to replace wins");
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(addOne);
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(replace);
        }
        assertEquals(4F, TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, 4F), EPSILON, "unregistered: back to the game's value");
    }

    @Test
    void armorAbsorbIsHeldBetweenNothingAndTheWholeBlowAndNaNIsIgnored() {
        // a lone listener is its own invoker, so the poster has to do the clamping too
        float[] answers = { -3F, 0F, 10F, 12F, Float.POSITIVE_INFINITY, Float.NaN };
        float[] expected = { 0F, 0F, 10F, 10F, 10F, 4F };
        for (int i = 0; i < answers.length; i++) {
            float answer = answers[i];
            TinkererEvents.LivingArmorAbsorb listener = (victim, source, damage, absorbed) -> answer;
            TinkererEvents.LIVING_ARMOR_ABSORB.register(listener);
            try {
                assertEquals(expected[i], TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, 4F), 0F, "answer " + answer);
            } finally {
                TinkererEvents.LIVING_ARMOR_ABSORB.unregister(listener);
            }
        }
        // in a chain the NaN of one listener is skipped and the next one still sees a number in range
        List<Float> seen = new ArrayList<>();
        TinkererEvents.LivingArmorAbsorb nan = (victim, source, damage, absorbed) -> Float.NaN;
        TinkererEvents.LivingArmorAbsorb after = (victim, source, damage, absorbed) -> {
            seen.add(absorbed);
            return absorbed;
        };
        TinkererEvents.LivingArmorAbsorb big = (victim, source, damage, absorbed) -> 99F;
        TinkererEvents.LIVING_ARMOR_ABSORB.register(big);
        TinkererEvents.LIVING_ARMOR_ABSORB.register(nan);
        TinkererEvents.LIVING_ARMOR_ABSORB.register(after);
        try {
            assertEquals(10F, TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, 4F), 0F);
            assertEquals(List.of(10F), seen, "the listener after the 99 saw 10 (held to the blow), not NaN");
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(big);
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(nan);
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(after);
        }
    }

    @Test
    void armorAbsorbIsNotPostedForArmourBypassingSourcesOrBlowsOfNoDamage() {
        int[] calls = { 0 };
        TinkererEvents.LivingArmorAbsorb everything = (victim, source, damage, absorbed) -> {
            calls[0]++;
            return damage;
        };
        TinkererEvents.LIVING_ARMOR_ABSORB.register(everything);
        try {
            assertEquals(0F, TinkererEvents.postLivingArmorAbsorb(null, source(true), 10F, 0F), 0F, "starvation, magic, the void: armour does not reduce them");
            assertEquals(0F, TinkererEvents.postLivingArmorAbsorb(null, source(false), 0F, 0F), 0F, "no damage");
            assertEquals(0F, TinkererEvents.postLivingArmorAbsorb(null, source(false), -2F, 0F), 0F, "negative damage");
            assertEquals(0F, TinkererEvents.postLivingArmorAbsorb(null, source(false), Float.NaN, 0F), 0F, "NaN damage");
            assertEquals(0F, TinkererEvents.postLivingArmorAbsorb(null, source(false), Float.POSITIVE_INFINITY, 0F), 0F, "infinite damage");
            assertEquals(0, calls[0], "none of them reached the listener");
            assertEquals(10F, TinkererEvents.postLivingArmorAbsorb(null, source(false), 10F, 0F), 0F, "a plain blow does");
            assertEquals(1, calls[0]);
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(everything);
        }
    }

    @Test
    void armorStepGivesBackTheGamesOwnFloatUnlessAListenerChangedTheAmount() {
        // a blow for which damage - (damage - vanilla) is not vanilla's float: handing the round trip back would change the damage
        // in the last place when nobody has anything to say
        float damage = Float.NaN;
        float vanilla = 0F;
        for (int i = 1; i < 5000 && Float.isNaN(damage); i++) {
            float candidate = 2F + i * 0.0137F;
            float left = candidate * 0.2F; // the armour takes most of it (below half the blow, where damage - left is not exact)
            if (candidate - (candidate - left) != left) {
                damage = candidate;
                vanilla = left;
            }
        }
        assertFalse(Float.isNaN(damage), "there are blows whose round trip is not the same float");
        assertTrue(damage - (damage - vanilla) != vanilla, "control: the round trip of " + damage + " is not " + vanilla);
        int expected = Float.floatToRawIntBits(vanilla);
        assertEquals(expected, Float.floatToRawIntBits(TinkererEvents.damageAfterArmorAbsorb(null, source(false), damage, vanilla)),
                "nobody listens: the game's own float");

        TinkererEvents.LivingArmorAbsorb leaveAlone = (victim, source, blow, absorbed) -> absorbed;
        TinkererEvents.LIVING_ARMOR_ABSORB.register(leaveAlone);
        try {
            assertEquals(expected, Float.floatToRawIntBits(TinkererEvents.damageAfterArmorAbsorb(null, source(false), damage, vanilla)),
                    "a listener that returns what it was given changes nothing");
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(leaveAlone);
        }

        TinkererEvents.LivingArmorAbsorb replace = (victim, source, blow, absorbed) -> blow * 0.85F;
        TinkererEvents.LIVING_ARMOR_ABSORB.register(replace);
        try {
            assertEquals(Float.floatToRawIntBits(damage - damage * 0.85F),
                    Float.floatToRawIntBits(TinkererEvents.damageAfterArmorAbsorb(null, source(false), damage, vanilla)),
                    "a listener that replaced the amount: damage - result");
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(replace);
        }
    }

    @Test
    void armorStepOfABlowThatIsNotFiniteIsTheGamesOwnValueNotNaN() {
        // vanilla's formula returns infinity for an infinite blow, so "what the armour took" is Infinity - Infinity = NaN, which is
        // never == to itself: the step must still hand the game its own infinity (a NaN would be NaN health)
        int[] calls = { 0 };
        TinkererEvents.LivingArmorAbsorb everything = (victim, source, blow, absorbed) -> {
            calls[0]++;
            return blow;
        };
        TinkererEvents.LIVING_ARMOR_ABSORB.register(everything);
        try {
            assertEquals(Float.POSITIVE_INFINITY,
                    TinkererEvents.damageAfterArmorAbsorb(null, source(false), Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), 0F,
                    "an infinite blow stays infinite");
            assertTrue(Float.isNaN(TinkererEvents.damageAfterArmorAbsorb(null, source(false), Float.NaN, Float.NaN)),
                    "a NaN blow is the game's own NaN");
            assertEquals(10F, TinkererEvents.damageAfterArmorAbsorb(null, source(true), 10F, 10F), 0F,
                    "a source that bypasses armour: the game's value");
            assertEquals(0F, TinkererEvents.damageAfterArmorAbsorb(null, source(false), 0F, 0F), 0F, "no damage");
            assertEquals(0, calls[0], "none of them was posted");
            assertEquals(0F, TinkererEvents.damageAfterArmorAbsorb(null, source(false), 10F, 4F), 0F, "a plain blow is (the listener takes it all)");
            assertEquals(1, calls[0]);
        } finally {
            TinkererEvents.LIVING_ARMOR_ABSORB.unregister(everything);
        }
    }

    // ---- BREAK_SPEED -----------------------------------------------------------------------------------------------

    @Test
    void breakSpeedChainsAndZeroMeansCannotDig() {
        TinkererEvents.BreakSpeed fiveTimes = (player, state, pos, speed) -> speed * 5F;
        TinkererEvents.BreakSpeed halved = (player, state, pos, speed) -> speed / 2F;
        TinkererEvents.BREAK_SPEED.register(fiveTimes);
        TinkererEvents.BREAK_SPEED.register(halved);
        try {
            assertEquals(5F, TinkererEvents.postBreakSpeed(null, null, null, 2F), EPSILON);
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(fiveTimes);
            TinkererEvents.BREAK_SPEED.unregister(halved);
        }
        TinkererEvents.BreakSpeed never = (player, state, pos, speed) -> -1F;
        TinkererEvents.BREAK_SPEED.register(never);
        try {
            assertEquals(0F, TinkererEvents.postBreakSpeed(null, null, null, 2F), 0F, "negative is cannot dig, as zero");
        } finally {
            TinkererEvents.BREAK_SPEED.unregister(never);
        }
        assertEquals(2F, TinkererEvents.postBreakSpeed(null, null, null, 2F), EPSILON);
        assertEquals(0F, TinkererEvents.postBreakSpeed(null, null, null, 0F), 0F, "a speed of zero is not posted");
    }

    // ---- LIVING_JUMP -----------------------------------------------------------------------------------------------

    @Test
    void jumpListenersAllRunInOrder() {
        List<String> seen = new ArrayList<>();
        TinkererEvents.LivingJump first = entity -> seen.add("first");
        TinkererEvents.LivingJump second = entity -> seen.add("second");
        TinkererEvents.postLivingJump(null); // nobody listens: fine
        TinkererEvents.LIVING_JUMP.register(first);
        TinkererEvents.LIVING_JUMP.register(second);
        try {
            TinkererEvents.postLivingJump(null);
        } finally {
            TinkererEvents.LIVING_JUMP.unregister(first);
            TinkererEvents.LIVING_JUMP.unregister(second);
        }
        assertEquals(List.of("first", "second"), seen);
    }

    // ---- LIVING_FALL -----------------------------------------------------------------------------------------------

    @Test
    void fallListenersShareTheFallDataAndAnInterruptCancels() {
        TinkererEvents.LivingFall halve = (entity, fall) -> {
            fall.setDistance(fall.getDistance() / 2F);
            return EventResult.pass();
        };
        TinkererEvents.LivingFall soften = (entity, fall) -> {
            fall.setDamageMultiplier(fall.getDamageMultiplier() * 0.5F);
            return EventResult.pass();
        };
        FallData fall = new FallData(10F, 1F);
        TinkererEvents.LIVING_FALL.register(halve);
        TinkererEvents.LIVING_FALL.register(soften);
        try {
            assertFalse(TinkererEvents.postLivingFall(null, fall));
            assertEquals(5F, fall.getDistance(), EPSILON);
            assertEquals(0.5F, fall.getDamageMultiplier(), EPSILON);
        } finally {
            TinkererEvents.LIVING_FALL.unregister(halve);
            TinkererEvents.LIVING_FALL.unregister(soften);
        }
        int[] later = { 0 };
        TinkererEvents.LivingFall cancel = (entity, f) -> EventResult.interruptFalse();
        TinkererEvents.LivingFall counter = (entity, f) -> {
            later[0]++;
            return EventResult.pass();
        };
        TinkererEvents.LIVING_FALL.register(cancel);
        TinkererEvents.LIVING_FALL.register(counter);
        try {
            assertTrue(TinkererEvents.postLivingFall(null, new FallData(10F, 1F)));
            assertEquals(0, later[0]);
        } finally {
            TinkererEvents.LIVING_FALL.unregister(cancel);
            TinkererEvents.LIVING_FALL.unregister(counter);
        }
        assertFalse(TinkererEvents.postLivingFall(null, new FallData(10F, 1F)), "no listener: no cancel");
    }

    // ---- USE_ITEM_TICK ---------------------------------------------------------------------------------------------

    @Test
    void useItemTickChainsAndNeverGoesBelowOne() {
        assertEquals(40, TinkererEvents.postUseItemTick(null, null, 40), "no listener");
        // an item with no use time counts 0, -1, -2 in vanilla and never completes: nobody listening must not change that
        assertEquals(0, TinkererEvents.postUseItemTick(null, null, 0), "no listener, a use that is not counting down: untouched");
        assertEquals(-3, TinkererEvents.postUseItemTick(null, null, -3), "and a negative count too");
        TinkererEvents.UseItemTick leaveAlone = (entity, stack, remaining) -> remaining;
        TinkererEvents.USE_ITEM_TICK.register(leaveAlone);
        try {
            assertEquals(0, TinkererEvents.postUseItemTick(null, null, 0), "a listener that changes nothing changes nothing");
            assertEquals(7, TinkererEvents.postUseItemTick(null, null, 7));
        } finally {
            TinkererEvents.USE_ITEM_TICK.unregister(leaveAlone);
        }
        TinkererEvents.UseItemTick takeThree = (entity, stack, remaining) -> remaining - 3;
        TinkererEvents.UseItemTick takeTwo = (entity, stack, remaining) -> remaining - 2;
        TinkererEvents.USE_ITEM_TICK.register(takeThree);
        TinkererEvents.USE_ITEM_TICK.register(takeTwo);
        try {
            assertEquals(35, TinkererEvents.postUseItemTick(null, null, 40));
            assertEquals(1, TinkererEvents.postUseItemTick(null, null, 4), "the use ends on this tick, not before it begins");
            assertEquals(1, TinkererEvents.postUseItemTick(null, null, 2), "and never below one");
            assertEquals(-5, TinkererEvents.postUseItemTick(null, null, 0),
                    "a use that was not counting down is the listener's to change, not raised to one");
        } finally {
            TinkererEvents.USE_ITEM_TICK.unregister(takeThree);
            TinkererEvents.USE_ITEM_TICK.unregister(takeTwo);
        }
    }

    // ---- LIVING_DROPS ----------------------------------------------------------------------------------------------

    @Test
    void dropListenersEditTheListAndCanCancel() {
        assertFalse(TinkererEvents.hasLivingDropsListeners(), "nobody listens at the start");
        int[] looting = { -1 };
        boolean[] recently = { false };
        TinkererEvents.LivingDrops seeContext = (victim, source, drops, level, recentlyHit) -> {
            looting[0] = level;
            recently[0] = recentlyHit;
            return EventResult.pass();
        };
        TinkererEvents.LivingDrops clear = (victim, source, drops, level, recentlyHit) -> {
            drops.clear();
            return EventResult.pass();
        };
        TinkererEvents.LIVING_DROPS.register(seeContext);
        TinkererEvents.LIVING_DROPS.register(clear);
        try {
            assertTrue(TinkererEvents.hasLivingDropsListeners());
            List<net.minecraft.world.entity.item.ItemEntity> drops = new ArrayList<>();
            drops.add(null);
            assertFalse(TinkererEvents.postLivingDrops(null, null, drops, 3, true));
            assertTrue(drops.isEmpty(), "the second listener cleared the list the first one saw");
            assertEquals(3, looting[0]);
            assertTrue(recently[0]);
        } finally {
            TinkererEvents.LIVING_DROPS.unregister(seeContext);
            TinkererEvents.LIVING_DROPS.unregister(clear);
        }
        assertFalse(TinkererEvents.hasLivingDropsListeners(), "unregistered: nobody listens again");

        TinkererEvents.LivingDrops cancel = (victim, source, drops, level, recentlyHit) -> EventResult.interruptFalse();
        TinkererEvents.LIVING_DROPS.register(cancel);
        try {
            assertTrue(TinkererEvents.hasLivingDropsListeners(), "a lone listener counts too (it is its own invoker)");
            assertTrue(TinkererEvents.postLivingDrops(null, null, new ArrayList<>(), 0, false));
        } finally {
            TinkererEvents.LIVING_DROPS.unregister(cancel);
        }
        assertFalse(TinkererEvents.hasLivingDropsListeners());
    }

    // ---- ItemRenderers ---------------------------------------------------------------------------------------------

    @Test
    void itemRenderersWaitForTheBackendAndRefuseADuplicate() {
        Item waiting = Items.STICK;
        Item direct = Items.BOWL;
        ItemRenderers.Renderer renderer = (stack, context, pose, buffers, light, overlay) -> { };
        assertThrows(NullPointerException.class, () -> ItemRenderers.register(waiting, null));

        ItemRenderers.register(waiting, renderer);
        assertSame(renderer, ItemRenderers.get(waiting));
        assertThrows(IllegalStateException.class, () -> ItemRenderers.register(waiting, renderer), "one renderer per item");

        List<Item> handed = new ArrayList<>();
        ItemRenderers.setBackend((item, r) -> handed.add(item));
        assertTrue(ItemRenderers.hasBackend());
        assertTrue(handed.contains(waiting), "what was registered before the backend came is handed over by setBackend");
        handed.clear();

        ItemRenderers.register(direct, renderer);
        assertEquals(List.of(direct), handed, "with a backend a registration goes straight through");

        // installing a backend again hands nothing over twice
        handed.clear();
        ItemRenderers.setBackend((item, r) -> handed.add(item));
        assertTrue(handed.isEmpty());
    }
}
