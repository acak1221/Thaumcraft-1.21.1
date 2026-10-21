package thaumcraft.taint;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintAnimalRules.TaintedAnimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

/** Original TC4 tainted animal and taint seed ecology numbers. */
class TaintAnimalRulesTest {
    /** A deterministic draw sequence: every value is taken from the supplied list. */
    private static IntUnaryOperator draws(int... values) {
        AtomicInteger index=new AtomicInteger();
        return bound->values[Math.min(index.getAndIncrement(),values.length-1)];
    }
    private static BooleanSupplierSequence booleans(boolean... values) {
        return new BooleanSupplierSequence(values);
    }
    private static final class BooleanSupplierSequence implements java.util.function.BooleanSupplier {
        private final boolean[] values;private int index;
        BooleanSupplierSequence(boolean... values) { this.values=values; }
        @Override public boolean getAsBoolean() { return values[Math.min(index++,values.length-1)]; }
    }

    @Test void originalAttributesAreKept() {
        assertEquals(8.0D,TaintAnimalRules.maxHealth(TaintedAnimal.CHICKEN));
        assertEquals(3.0D,TaintAnimalRules.attackDamage(TaintedAnimal.CHICKEN));
        assertEquals(0.4D,TaintAnimalRules.movementSpeed(TaintedAnimal.CHICKEN));
        assertEquals(40.0D,TaintAnimalRules.maxHealth(TaintedAnimal.COW));
        assertEquals(6.0D,TaintAnimalRules.attackDamage(TaintedAnimal.COW));
        assertEquals(0.27D,TaintAnimalRules.movementSpeed(TaintedAnimal.COW));
        assertEquals(20.0D,TaintAnimalRules.maxHealth(TaintedAnimal.PIG));
        assertEquals(4.0D,TaintAnimalRules.attackDamage(TaintedAnimal.PIG));
        assertEquals(0.275D,TaintAnimalRules.movementSpeed(TaintedAnimal.PIG));
        assertEquals(20.0D,TaintAnimalRules.maxHealth(TaintedAnimal.SHEEP));
        assertEquals(3.0D,TaintAnimalRules.attackDamage(TaintedAnimal.SHEEP));
        assertEquals(0.25D,TaintAnimalRules.movementSpeed(TaintedAnimal.SHEEP));
        assertEquals(30.0D,TaintAnimalRules.maxHealth(TaintedAnimal.VILLAGER));
        assertEquals(4.0D,TaintAnimalRules.attackDamage(TaintedAnimal.VILLAGER));
        assertEquals(0.3D,TaintAnimalRules.movementSpeed(TaintedAnimal.VILLAGER));
    }
    @Test void originalHitboxesAreKept() {
        assertEquals(0.5F,TaintAnimalRules.width(TaintedAnimal.CHICKEN));
        assertEquals(0.8F,TaintAnimalRules.height(TaintedAnimal.CHICKEN));
        assertEquals(0.9F,TaintAnimalRules.width(TaintedAnimal.COW));
        assertEquals(1.3F,TaintAnimalRules.height(TaintedAnimal.COW));
        assertEquals(0.9F,TaintAnimalRules.width(TaintedAnimal.PIG));
        assertEquals(0.9F,TaintAnimalRules.height(TaintedAnimal.PIG));
        assertEquals(0.9F,TaintAnimalRules.width(TaintedAnimal.SHEEP));
        assertEquals(1.3F,TaintAnimalRules.height(TaintedAnimal.SHEEP));
        // The original tainted villager keeps the EntityMob default hitbox.
        assertEquals(0.6F,TaintAnimalRules.width(TaintedAnimal.VILLAGER));
        assertEquals(1.8F,TaintAnimalRules.height(TaintedAnimal.VILLAGER));
    }
    @Test void onlyTheCowDespawnsInPeaceful() {
        assertTrue(TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.COW));
        assertFalse(TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.CHICKEN));
        assertFalse(TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.PIG));
        assertFalse(TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.SHEEP));
        assertFalse(TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.VILLAGER));
    }
    @Test void originalChunkCapIsKept() {
        assertEquals(2,TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.CHICKEN));
        assertEquals(2,TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.PIG));
        assertEquals(1,TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.SHEEP));
        assertEquals(1,TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.COW));
        assertEquals(1,TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.VILLAGER));
    }
    @Test void chickenAlwaysDropsOneTaintResource() {
        assertEquals(TaintAnimalRules.TAINTED_GOO,
                TaintAnimalRules.taintedDrop(TaintedAnimal.CHICKEN,draws(0),booleans(false)));
        assertEquals(TaintAnimalRules.TAINT_TENDRIL,
                TaintAnimalRules.taintedDrop(TaintedAnimal.CHICKEN,draws(1),booleans(false)));
        assertEquals(TaintAnimalRules.TAINT_TENDRIL,
                TaintAnimalRules.taintedDrop(TaintedAnimal.CHICKEN,draws(3),booleans(false)));
    }
    @Test void cowAlwaysDropsOneTaintResource() {
        assertEquals(TaintAnimalRules.TAINTED_GOO,
                TaintAnimalRules.taintedDrop(TaintedAnimal.COW,draws(0),booleans(true)));
        assertEquals(TaintAnimalRules.TAINT_TENDRIL,
                TaintAnimalRules.taintedDrop(TaintedAnimal.COW,draws(0),booleans(false)));
    }
    @Test void pigAndSheepDropOneInThree() {
        assertEquals(TaintAnimalRules.NO_DROP,
                TaintAnimalRules.taintedDrop(TaintedAnimal.PIG,draws(1),booleans(true)));
        assertEquals(TaintAnimalRules.TAINTED_GOO,
                TaintAnimalRules.taintedDrop(TaintedAnimal.PIG,draws(0),booleans(true)));
        assertEquals(TaintAnimalRules.TAINT_TENDRIL,
                TaintAnimalRules.taintedDrop(TaintedAnimal.SHEEP,draws(0),booleans(false)));
        assertEquals(TaintAnimalRules.NO_DROP,
                TaintAnimalRules.taintedDrop(TaintedAnimal.SHEEP,draws(2),booleans(false)));
    }
    @Test void villagerDropsOneInTwoAndTheCoinWithLooting() {
        assertEquals(TaintAnimalRules.NO_DROP,
                TaintAnimalRules.taintedDrop(TaintedAnimal.VILLAGER,draws(1),booleans(true)));
        assertEquals(TaintAnimalRules.TAINTED_GOO,
                TaintAnimalRules.taintedDrop(TaintedAnimal.VILLAGER,draws(0),booleans(true)));
        // nextInt(13) below 1+looting.
        assertTrue(TaintAnimalRules.dropsGoldCoin(0,draws(0)));
        assertFalse(TaintAnimalRules.dropsGoldCoin(0,draws(1)));
        assertTrue(TaintAnimalRules.dropsGoldCoin(1,draws(1)));
        assertTrue(TaintAnimalRules.dropsGoldCoin(12,draws(12)));
        assertFalse(TaintAnimalRules.dropsGoldCoin(12,draws(13)));
    }
    @Test void originalFlapAnimationIsKept() {
        // Original: the flap speed grows by1.2 while airborne and shrinks by0.3 on the ground, clamped to zero and one.
        assertEquals(1.0F,TaintAnimalRules.flapSpeed(0.0F,false),1.0E-6F);
        assertEquals(0.0F,TaintAnimalRules.flapSpeed(0.0F,true),1.0E-6F);
        assertEquals(1.0F,TaintAnimalRules.flapSpeed(0.9F,false),1.0E-6F);
        assertEquals(0.7F,TaintAnimalRules.flapSpeed(1.0F,true),1.0E-6F);
        assertEquals(0.2F,TaintAnimalRules.flapSpeed(0.5F,true),1.0E-6F);
        // One while airborne, then nine tenths per tick.
        assertEquals(0.9F,TaintAnimalRules.flapAmplitude(1.0F,false),1.0E-6F);
        assertEquals(0.45F,TaintAnimalRules.flapAmplitude(0.5F,true),1.0E-6F);
        assertEquals(0.9F,TaintAnimalRules.flapAmplitude(0.5F,false),1.0E-6F);
        assertEquals(2.0F,TaintAnimalRules.flapPhase(0.0F,1.0F),1.0E-6F);
        // The original fall damping is nine tenths, not the vanilla0.6.
        assertEquals(-0.9F,TaintAnimalRules.fallDamping(-1.0F),1.0E-6F);
    }
    @Test void originalSheepEatingAnglesAreKept() {
        assertEquals(0.0F,TaintAnimalRules.sheepHeadAngle(0,0.0F));
        assertEquals(1.0F,TaintAnimalRules.sheepHeadAngle(4,0.0F));
        assertEquals(1.0F,TaintAnimalRules.sheepHeadAngle(36,0.0F));
        assertEquals(0.5F,TaintAnimalRules.sheepHeadAngle(2,0.0F),1.0E-6F);
        assertEquals(0.62831855F,TaintAnimalRules.sheepBodyAngle(40,0.0F,0.0F),1.0E-6F);
        assertEquals(0.0F,TaintAnimalRules.sheepBodyAngle(0,0.0F,0.0F),1.0E-6F);
        // The body yaw follows the render yaw when the sheep does not eat.
        assertEquals(1.0F,TaintAnimalRules.sheepBodyAngle(0,0.0F,57.295776F),1.0E-6F);
    }
    @Test void originalGrassConversionIsKept() {
        assertEquals(250,TaintAnimalRules.GRASS_TRIGGER_INTERVAL);
        assertTrue(TaintAnimalRules.convertsGrass(0));
        assertFalse(TaintAnimalRules.convertsGrass(1));
        assertEquals(40,TaintAnimalRules.GRASS_EAT_TICKS);
        assertEquals(39,TaintAnimalRules.grassTimer(40));
        assertEquals(0,TaintAnimalRules.grassTimer(0));
    }
    @Test void originalVillageCadenceIsKept() {
        assertEquals(70,TaintAnimalRules.villageSearchDelay(0));
        assertEquals(119,TaintAnimalRules.villageSearchDelay(49));
        assertEquals(32,TaintAnimalRules.VILLAGE_SEARCH_RADIUS);
        assertEquals(6.0F,TaintAnimalRules.villageRestrictionRadius(10),1.0E-6F);
    }
    @Test void originalCrawlerPoisonIsKept() {
        assertEquals(3,TaintAnimalRules.crawlerFluxSeconds(2));
        assertEquals(6,TaintAnimalRules.crawlerFluxSeconds(3));
        assertEquals(0,TaintAnimalRules.crawlerFluxSeconds(0));
        assertEquals(0,TaintAnimalRules.crawlerFluxSeconds(1));
        // nextInt(bound+1) above two.
        assertTrue(TaintAnimalRules.crawlerFluxPoison(3,draws(3)));
        assertFalse(TaintAnimalRules.crawlerFluxPoison(3,draws(2)));
        assertFalse(TaintAnimalRules.crawlerFluxPoison(0,draws(3)));
        assertEquals(40,TaintAnimalRules.CRAWLER_FIBRE_INTERVAL);
    }
    @Test void originalSeedValuesAreKept() {
        assertEquals(1,TaintAnimalRules.seedArea(false));
        assertEquals(2,TaintAnimalRules.seedArea(true));
        assertEquals(75,TaintAnimalRules.seedMaxHealth(false));
        assertEquals(150,TaintAnimalRules.seedMaxHealth(true));
        assertEquals(4,TaintAnimalRules.seedAttackDamage(false));
        assertEquals(7,TaintAnimalRules.seedAttackDamage(true));
        assertEquals(8,TaintAnimalRules.seedExperience(false));
        assertEquals(12,TaintAnimalRules.seedExperience(true));
        assertEquals(1200,TaintAnimalRules.SEED_REREGISTER_INTERVAL);
        assertEquals(4.0D,TaintAnimalRules.seedAuraRange(1),1.0E-9D);
        assertEquals(8.0D,TaintAnimalRules.seedAuraRange(2),1.0E-9D);
        assertEquals(256.0D,TaintAnimalRules.seedTentacleDistanceSquared(1),1.0E-9D);
        assertEquals(512.0D,TaintAnimalRules.seedTentacleDistanceSquared(2),1.0E-9D);
        assertEquals(100,TaintAnimalRules.seedFluxDuration());
        assertEquals(0,TaintAnimalRules.seedFluxAmplifier(1));
        assertEquals(1,TaintAnimalRules.seedFluxAmplifier(2));
    }
    @Test void primeSeedDropsOneToThreeCrystals() {
        assertEquals(1,TaintAnimalRules.seedPrimeCrystals(booleans(false,false)));
        assertEquals(2,TaintAnimalRules.seedPrimeCrystals(booleans(true,false)));
        assertEquals(3,TaintAnimalRules.seedPrimeCrystals(booleans(true,true)));
    }
    @Test void originalRandomIntIsInclusiveOnBothEnds() {
        assertEquals(-6,TaintAnimalRules.randomInt(draws(0),-6,6));
        assertEquals(6,TaintAnimalRules.randomInt(draws(12),-6,6));
        assertEquals(-3,TaintAnimalRules.seedSpreadOffset(1,draws(0)));
        assertEquals(3,TaintAnimalRules.seedSpreadOffset(1,draws(6)));
        assertEquals(-1,TaintAnimalRules.seedSpreadHeight(1,draws(0)));
        assertEquals(1,TaintAnimalRules.seedSpreadHeight(1,draws(2)));
    }
    @Test void originalDeathConversionTableIsKept() {
        assertTrue(TaintAnimalRules.convertsOnDeath(true,false));
        assertFalse(TaintAnimalRules.convertsOnDeath(false,false));
        assertFalse(TaintAnimalRules.convertsOnDeath(true,true));
        assertEquals(TaintedAnimal.CHICKEN,TaintAnimalRules.taintedVariant("minecraft:chicken"));
        assertEquals(TaintedAnimal.COW,TaintAnimalRules.taintedVariant("minecraft:cow"));
        assertEquals(TaintedAnimal.PIG,TaintAnimalRules.taintedVariant("minecraft:pig"));
        assertEquals(TaintedAnimal.SHEEP,TaintAnimalRules.taintedVariant("minecraft:sheep"));
        assertEquals(TaintedAnimal.VILLAGER,TaintAnimalRules.taintedVariant("minecraft:villager"));
        assertNull(TaintAnimalRules.taintedVariant("minecraft:creeper"));
        assertNull(TaintAnimalRules.taintedVariant("minecraft:zombie"));
        assertNull(TaintAnimalRules.taintedVariant(null));
    }
    @Test void originalSheepShearDropIsKept() {
        assertEquals(1,TaintAnimalRules.shearWoolCount(draws(0)));
        assertEquals(3,TaintAnimalRules.shearWoolCount(draws(2)));
    }
    @Test void originalArmourOfTheTaintedAnimalsIsKept() {
        // Oracle EntityTaintChicken:64-66 and EntityTaintPig:66-68 return a constant two,
        // EntityTaintSheep:63-65 a constant one; the cow and the villager keep the inherited
        // armour of their equipment.
        assertEquals(2,TaintAnimalRules.armor(TaintedAnimal.CHICKEN));
        assertEquals(2,TaintAnimalRules.armor(TaintedAnimal.PIG));
        assertEquals(1,TaintAnimalRules.armor(TaintedAnimal.SHEEP));
        assertEquals(0,TaintAnimalRules.armor(TaintedAnimal.COW));
        assertEquals(0,TaintAnimalRules.armor(TaintedAnimal.VILLAGER));
    }
}
