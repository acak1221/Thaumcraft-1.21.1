package thaumcraft.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import thaumcraft.common.lib.mods.ChampionModArmored;
import thaumcraft.common.lib.mods.ChampionModBold;
import thaumcraft.common.lib.mods.ChampionModDummy;
import thaumcraft.common.lib.mods.ChampionModFire;
import thaumcraft.common.lib.mods.ChampionModGrim;
import thaumcraft.common.lib.mods.ChampionModInfested;
import thaumcraft.common.lib.mods.ChampionModMighty;
import thaumcraft.common.lib.mods.ChampionModPoison;
import thaumcraft.common.lib.mods.ChampionModSickly;
import thaumcraft.common.lib.mods.ChampionModSpined;
import thaumcraft.common.lib.mods.ChampionModUndying;
import thaumcraft.common.lib.mods.ChampionModVampire;
import thaumcraft.common.lib.mods.ChampionModWarded;
import thaumcraft.common.lib.mods.ChampionModWarp;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.mods.IChampionModifierEffect;

/** The thirteen original champion modifiers: their identity, their bonuses and their effect. */
class ChampionModifierTest {
    /** Random that answers with fixed values so the original chances can be checked exactly. */
    private static final class ScriptedRandom extends Random {
        private final float floatValue;
        private final int intValue;
        private final boolean booleanValue;
        ScriptedRandom(float floatValue, int intValue) { this(floatValue, intValue, false); }
        ScriptedRandom(float floatValue, int intValue, boolean booleanValue) {
            this.floatValue = floatValue; this.intValue = intValue; this.booleanValue = booleanValue;
        }
        @Override public float nextFloat() { return floatValue; }
        @Override public int nextInt(int bound) { return intValue; }
        @Override public boolean nextBoolean() { return booleanValue; }
    }

    private static IChampionModifierEffect.Context attack(float amount, boolean player) {
        return IChampionModifierEffect.Context.attack(amount, player, 0, 10.0f, 20.0f, 0.0f, false);
    }
    private static IChampionModifierEffect.Context damaged(float amount, boolean unblockable, boolean thorns) {
        return IChampionModifierEffect.Context.damaged(amount, unblockable, thorns, true, false, 0, 10.0f, 20.0f, 0.0f, false);
    }
    private static IChampionModifierEffect.Context tick(int ticks, float health, float baseMaxHealth, boolean invulnerable) {
        return IChampionModifierEffect.Context.tick(ticks, health, baseMaxHealth, 0.0f, invulnerable);
    }
    private static IChampionModifierEffect.Context tick(int ticks, float health, float baseMaxHealth, float absorption, boolean invulnerable) {
        return IChampionModifierEffect.Context.tick(ticks, health, baseMaxHealth, absorption, invulnerable);
    }

    @Test void originalModifiersKeepTheirIdentityAndTrigger() {
        record Expected(int id, String name, String display, int trigger, String uuid, Class<?> effect) {}
        Expected[] expected = new Expected[] {
                new Expected(0, "bold", "Bold", IChampionModifierEffect.TRIGGER_ATTRIBUTE, "40289aa1-907f-4ac6-ad79-e6681efe2cbc", ChampionModBold.class),
                new Expected(1, "spine", "Spined", IChampionModifierEffect.TRIGGER_DAMAGED, "365eead5-3f15-42a8-9e68-36100faef945", ChampionModSpined.class),
                new Expected(2, "armor", "Armored", IChampionModifierEffect.TRIGGER_DAMAGED, "4e23758d-348e-42a8-8de6-08ae0a59033c", ChampionModArmored.class),
                new Expected(3, "mighty", "Mighty", IChampionModifierEffect.TRIGGER_ATTRIBUTE, "6d2ffe79-f034-4a06-b288-e1916c21e385", ChampionModMighty.class),
                new Expected(4, "grim", "Grim", IChampionModifierEffect.TRIGGER_ATTACK, "0f23321e-f921-4246-90b8-21ef202de224", ChampionModGrim.class),
                new Expected(5, "warded", "Warded", IChampionModifierEffect.TRIGGER_TICK, "b622c4d8-abc6-4db7-b3ee-5cf71b8e5286", ChampionModWarded.class),
                new Expected(6, "warp", "Warped", IChampionModifierEffect.TRIGGER_ATTACK, "107da049-af7a-4409-989a-6de23c8fe036", ChampionModWarp.class),
                new Expected(7, "undying", "Undying", IChampionModifierEffect.TRIGGER_TICK, "cb9484d3-6255-4893-a4f2-3ecc375692ee", ChampionModUndying.class),
                new Expected(8, "fiery", "Fiery", IChampionModifierEffect.TRIGGER_ATTACK, "6b567fdf-9245-48f5-8314-f93fe5db1427", ChampionModFire.class),
                new Expected(9, "sickly", "Sickly", IChampionModifierEffect.TRIGGER_ATTACK, "b5718868-9ab0-424c-af1f-8b35e836b46e", ChampionModSickly.class),
                new Expected(10, "venomous", "Venomous", IChampionModifierEffect.TRIGGER_ATTACK, "ab9a132e-c619-4c0a-a103-10cbbcfba1a2", ChampionModPoison.class),
                new Expected(11, "vampiric", "Vampiric", IChampionModifierEffect.TRIGGER_ATTACK, "3412251e-af81-4c3c-93ba-2e1c33b049ea", ChampionModVampire.class),
                new Expected(12, "infested", "Infested", IChampionModifierEffect.TRIGGER_DAMAGED, "9c577fbe-ddbc-4ea2-a661-770ea775f43b", ChampionModInfested.class)};
        assertEquals(expected.length, ChampionModifier.mods.length);
        for (Expected want : expected) {
            ChampionModifier modifier = ChampionModifier.mods[want.id()];
            assertEquals(want.id(), modifier.id);
            assertEquals(want.name(), modifier.name);
            assertEquals(want.display(), modifier.displayName);
            assertEquals(want.trigger(), modifier.trigger());
            assertEquals("champion.mod." + want.name(), modifier.localizationKey());
            assertEquals(UUID.fromString(want.uuid()), modifier.attributeModifierId);
            assertEquals(want.id() + 2, modifier.attributeAmount, 0.0);
            assertSame(want.effect(), modifier.effect.getClass());
        }
    }

    @Test void originalChampionBonusesAreSharedAndOnlyTwoModifiersAddMore() {
        for (ChampionModifier modifier : ChampionModifier.mods) {
            assertEquals(30.0, modifier.healthBonus(), 0.0);
            assertEquals(2.0, modifier.damageMultiplier(), 0.0);
        }
        assertEquals(1.3, ChampionModifier.mods[ChampionModifier.BOLD].speedMultiplier(), 0.0);
        assertEquals(1.0, ChampionModifier.mods[ChampionModifier.ARMOR].speedMultiplier(), 0.0);
        assertEquals(3.0, ChampionModifier.mods[ChampionModifier.MIGHTY].extraDamageMultiplier(), 0.0);
        assertEquals(1.0, ChampionModifier.mods[ChampionModifier.BOLD].extraDamageMultiplier(), 0.0);
        assertEquals(25.0f, ChampionModifier.CHAMPION_HEAL, 0.0f);
    }

    @Test void originalArmouredChampionTakesNineteenTwentyFifthsUnlessTheDamageBypassesArmour() {
        IChampionModifierEffect.Result reduced = new ChampionModArmored().performEffect(damaged(10.0f, false, false), new ScriptedRandom(0.5f, 0));
        assertEquals(7.6f, reduced.amount(), 1.0e-6f);
        assertEquals(0.0f, reduced.heal(), 0.0f);
        IChampionModifierEffect.Result unblockable = new ChampionModArmored().performEffect(damaged(10.0f, true, false), new ScriptedRandom(0.5f, 0));
        assertEquals(10.0f, unblockable.amount(), 1.0e-6f);
    }

    @Test void originalSpinedChampionReflectsOneToThreeThornsDamage() {
        IChampionModifierEffect.Result reflected = new ChampionModSpined().performEffect(damaged(10.0f, false, false), new ScriptedRandom(0.5f, 2));
        assertEquals(3.0f, reflected.reflected(), 0.0f);
        assertEquals(10.0f, reflected.amount(), 0.0f);
        assertEquals(1.0f, new ChampionModSpined().performEffect(damaged(10.0f, false, false), new ScriptedRandom(0.5f, 0)).reflected(), 0.0f);
        assertEquals(0.0f, new ChampionModSpined().performEffect(damaged(10.0f, false, true), new ScriptedRandom(0.5f, 2)).reflected(), 0.0f);
        assertEquals(0.0f, new ChampionModSpined().performEffect(
                IChampionModifierEffect.Context.damaged(10.0f, false, false, false, false, 0, 10.0f, 20.0f, 0.0f, false), new ScriptedRandom(0.5f, 2)).reflected(), 0.0f);
    }

    @Test void originalGrimFierySicklyAndVenomousHitTheirTargetWithFortyPercentChance() {
        ScriptedRandom hits = new ScriptedRandom(0.39f, 0);
        ScriptedRandom misses = new ScriptedRandom(0.41f, 0);
        IChampionModifierEffect.Result grim = new ChampionModGrim().performEffect(attack(10.0f, false), hits);
        // Original ChampionModGrim:26-30 - Potion.field_82731_v is the WITHER, not weakness.
        assertEquals(IChampionModifierEffect.Status.WITHER, grim.status());
        assertEquals(200, grim.statusDuration());
        assertEquals(IChampionModifierEffect.Status.NONE, new ChampionModGrim().performEffect(attack(10.0f, false), misses).status());
        IChampionModifierEffect.Result fiery = new ChampionModFire().performEffect(attack(10.0f, false), hits);
        assertEquals(4, fiery.fireSeconds());
        assertEquals(0, new ChampionModFire().performEffect(attack(10.0f, false), misses).fireSeconds());
        IChampionModifierEffect.Result sickly = new ChampionModSickly().performEffect(attack(10.0f, false), hits);
        assertEquals(IChampionModifierEffect.Status.HUNGER, sickly.status());
        assertEquals(500, sickly.statusDuration());
        assertEquals(IChampionModifierEffect.Status.NONE, new ChampionModSickly().performEffect(attack(10.0f, false), misses).status());
        IChampionModifierEffect.Result venom = new ChampionModPoison().performEffect(attack(10.0f, false), hits);
        assertEquals(IChampionModifierEffect.Status.POISON, venom.status());
        assertEquals(100, venom.statusDuration());
        assertEquals(IChampionModifierEffect.Status.NONE, new ChampionModPoison().performEffect(attack(10.0f, false), misses).status());
    }

    @Test void originalVampiricChampionHealsHalfTheDamageButNeverLessThanTwo() {
        assertEquals(5.0f, new ChampionModVampire().performEffect(attack(10.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 1.0e-6f);
        assertEquals(2.0f, new ChampionModVampire().performEffect(attack(1.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 1.0e-6f);
        assertEquals(2.0f, new ChampionModVampire().performEffect(attack(4.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 1.0e-6f);
    }

    @Test void originalWarpedChampionWarpsItsPlayerTargetWithThirtyThreePercentChance() {
        ScriptedRandom hits = new ScriptedRandom(0.32f, 2);
        ScriptedRandom misses = new ScriptedRandom(0.34f, 2);
        IChampionModifierEffect.Result warped = new ChampionModWarp().performEffect(attack(10.0f, true), hits);
        assertEquals(3, warped.warp());
        assertEquals(0, new ChampionModWarp().performEffect(attack(10.0f, true), misses).warp());
        assertEquals(0, new ChampionModWarp().performEffect(attack(10.0f, false), hits).warp());
    }

    @Test void originalUndyingChampionHealsOneHealthEveryTwentyTicks() {
        assertEquals(1.0f, new ChampionModUndying().performEffect(tick(20, 5.0f, 20.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 0.0f);
        assertEquals(0.0f, new ChampionModUndying().performEffect(tick(21, 5.0f, 20.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 0.0f);
        assertEquals(1.0f, new ChampionModUndying().performEffect(tick(40, 5.0f, 20.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 0.0f);
    }

    @Test void originalWardedChampionRegainsOnePointOfItsWardEveryTwentyFifthTick() {
        // Oracle ChampionModWarded:25-30 - the ward of the warded champion is its absorption,
        // never its health, and it grows by one point every twenty fifth tick while the champion
        // is not hurt and its ward is below half of its base max health.
        assertEquals(1.0f, new ChampionModWarded().performEffect(tick(25, 5.0f, 20.0f, 0.0f, false), new ScriptedRandom(0.5f, 0)).absorption(), 0.0f);
        assertEquals(0.0f, new ChampionModWarded().performEffect(tick(25, 5.0f, 20.0f, 10.0f, false), new ScriptedRandom(0.5f, 0)).absorption(), 0.0f);
        assertEquals(0.0f, new ChampionModWarded().performEffect(tick(26, 5.0f, 20.0f, 0.0f, false), new ScriptedRandom(0.5f, 0)).absorption(), 0.0f);
        assertEquals(0.0f, new ChampionModWarded().performEffect(tick(25, 5.0f, 20.0f, 0.0f, true), new ScriptedRandom(0.5f, 0)).absorption(), 0.0f);
        // The ward is never a heal of the champion.
        assertEquals(0.0f, new ChampionModWarded().performEffect(tick(25, 5.0f, 20.0f, 0.0f, false), new ScriptedRandom(0.5f, 0)).heal(), 0.0f);
    }

    @Test void originalInfestedChampionSpawnsASpiderlingWithFortyPercentChance() {
        assertTrue(new ChampionModInfested().performEffect(damaged(10.0f, false, false), new ScriptedRandom(0.39f, 0)).spawnSpiderling());
        assertFalse(new ChampionModInfested().performEffect(damaged(10.0f, false, false), new ScriptedRandom(0.41f, 0)).spawnSpiderling());
    }

    @Test void originalBoldMightyAndDummyModifiersNeverChangeTheDamage() {
        assertEquals(0.0f, new ChampionModBold().performEffect(attack(10.0f, false), new ScriptedRandom(0.5f, 0)).amount(), 0.0f);
        assertEquals(0.0f, new ChampionModMighty().performEffect(attack(10.0f, false), new ScriptedRandom(0.5f, 0)).amount(), 0.0f);
        assertEquals(10.0f, new ChampionModDummy().performEffect(attack(10.0f, false), new ScriptedRandom(0.5f, 0)).amount(), 0.0f);
    }

    @Test void originalClientParticlesKeepTheirParticleAndTheirChance() {
        assertNotNull(new ChampionModArmored().showFx(new ScriptedRandom(0.5f, 0), 1.0f, 2.0f));
        assertNull(new ChampionModArmored().showFx(new ScriptedRandom(0.5f, 1), 1.0f, 2.0f));
        IChampionModifierEffect.Fx armoured = new ChampionModArmored().showFx(new ScriptedRandom(0.5f, 0), 1.0f, 2.0f);
        assertEquals(IChampionModifierEffect.Particle.GENERIC, armoured.kind());
        assertEquals(112, armoured.start());
        assertEquals(9, armoured.num());
        assertEquals(1, armoured.inc());
        IChampionModifierEffect.Fx grim = new ChampionModGrim().showFx(new ScriptedRandom(0.5f, 0), 1.0f, 2.0f);
        assertEquals(160, grim.start());
        assertEquals(10, grim.num());
        IChampionModifierEffect.Fx bold = new ChampionModBold().showFx(new ScriptedRandom(0.5f, 0), 1.0f, 2.0f);
        assertEquals(IChampionModifierEffect.Particle.SPARK, bold.kind());
        IChampionModifierEffect.Fx infested = new ChampionModInfested().showFx(new ScriptedRandom(0.5f, 0, true), 1.0f, 2.0f);
        assertEquals(IChampionModifierEffect.Particle.SLIME_JUMP, infested.kind());
        assertNull(new ChampionModInfested().showFx(new ScriptedRandom(0.5f, 0, false), 1.0f, 2.0f));
    }
}
