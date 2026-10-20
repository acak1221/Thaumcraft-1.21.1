package thaumcraft.common.lib.taint;

import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;

/** Original TC4 tainted-animal and taint-seed ecology numbers.
 *
 * <p>Every value here comes from the original1.7.10 oracle
 * (thaumcraft/common/entities/monster/EntityTaintChicken|Cow|Pig|Sheep|Villager)
 * or, for the taint crawler and the taint seeds that only exist in the later
 * Thaumcraft6 build, from the original1.12.2 EntityTaintCrawler|EntityTaintSeed
 * classes. The class is deliberately pure: no world, no entity, no registry, so
 * the entities and the spawn/death hooks only have to call it. */
public final class TaintAnimalRules {
    private TaintAnimalRules() {}

    /** The original tainted animals, in the original ConfigEntities order. */
    public enum TaintedAnimal { CHICKEN, COW, PIG, SHEEP, VILLAGER }

    /** Original ItemResource metadata:11 tainted slime,12 taint tendril,18 gold coin. */
    public static final int TAINTED_GOO=11;
    public static final int TAINT_TENDRIL=12;
    public static final int GOLD_COIN=18;
    /** No drop at all. */
    public static final int NO_DROP=-1;

    /** Original EntityLiving follow range. */
    public static final double FOLLOW_RANGE=16.0D;
    /** Original EntityMob experience value. */
    public static final int EXPERIENCE=5;

    public static double maxHealth(TaintedAnimal animal) {
        return switch(animal) {
            case CHICKEN -> 8.0D;
            case COW -> 40.0D;
            case PIG -> 20.0D;
            case SHEEP -> 20.0D;
            case VILLAGER -> 30.0D;
        };
    }
    public static double attackDamage(TaintedAnimal animal) {
        return switch(animal) {
            case CHICKEN -> 3.0D;
            case COW -> 6.0D;
            case PIG -> 4.0D;
            case SHEEP -> 3.0D;
            case VILLAGER -> 4.0D;
        };
    }
    public static double movementSpeed(TaintedAnimal animal) {
        return switch(animal) {
            case CHICKEN -> 0.4D;
            case COW -> 0.27D;
            case PIG -> 0.275D;
            case SHEEP -> 0.25D;
            case VILLAGER -> 0.3D;
        };
    }
    public static float width(TaintedAnimal animal) {
        return switch(animal) {
            case CHICKEN -> 0.5F;
            case COW -> 0.9F;
            case PIG -> 0.9F;
            case SHEEP -> 0.9F;
            // The original tainted villager never calls setSize: it keeps the EntityMob default.
            case VILLAGER -> 0.6F;
        };
    }
    public static float height(TaintedAnimal animal) {
        return switch(animal) {
            case CHICKEN -> 0.8F;
            case COW -> 1.3F;
            case PIG -> 0.9F;
            case SHEEP -> 1.3F;
            case VILLAGER -> 1.8F;
        };
    }
    /** Original getMaxSpawnedInChunk: only the chicken and the pig raise it. */
    public static int maxSpawnedInChunk(TaintedAnimal animal) {
        return animal==TaintedAnimal.CHICKEN||animal==TaintedAnimal.PIG ? 2 : 1;
    }
    /** Original canDespawnInPeaceful: only the cow keeps the EntityMob default. */
    public static boolean despawnsInPeaceful(TaintedAnimal animal) { return animal==TaintedAnimal.COW; }
    /** Original getVoicePitch of every tainted animal. */
    public static float voicePitch() { return 0.7F; }
    /** Original getSoundVolume: only the cow lowers it. */
    public static float soundVolume(TaintedAnimal animal) { return animal==TaintedAnimal.COW ? 0.4F : 1.0F; }

    /** Original1.7 LivingDeathEvent: a flux tainted animal dies and is replaced by its tainted twin. */
    public static boolean convertsOnDeath(boolean fluxTainted,boolean alreadyTainted) {
        return fluxTainted&&!alreadyTainted;
    }
    /** Original1.7 LivingDeathEvent conversion table, keyed by the vanilla1.21 entity id. */
    public static TaintedAnimal taintedVariant(String vanillaEntityId) {
        if(vanillaEntityId==null)return null;
        return switch(vanillaEntityId) {
            case "minecraft:chicken" -> TaintedAnimal.CHICKEN;
            case "minecraft:cow" -> TaintedAnimal.COW;
            case "minecraft:pig" -> TaintedAnimal.PIG;
            case "minecraft:sheep" -> TaintedAnimal.SHEEP;
            case "minecraft:villager" -> TaintedAnimal.VILLAGER;
            default -> null;
        };
    }

    /** Original dropFewItems of the tainted animals. The draw order is part of the original. */
    public static int taintedDrop(TaintedAnimal animal,IntUnaryOperator nextInt,BooleanSupplier nextBoolean) {
        switch(animal) {
            case CHICKEN -> { return nextInt.applyAsInt(4)==0 ? TAINTED_GOO : TAINT_TENDRIL; }
            case COW -> { return nextBoolean.getAsBoolean() ? TAINTED_GOO : TAINT_TENDRIL; }
            case PIG, SHEEP -> {
                if(nextInt.applyAsInt(3)!=0)return NO_DROP;
                return nextBoolean.getAsBoolean() ? TAINTED_GOO : TAINT_TENDRIL;
            }
            case VILLAGER -> {
                if(nextInt.applyAsInt(2)!=0)return NO_DROP;
                return nextBoolean.getAsBoolean() ? TAINTED_GOO : TAINT_TENDRIL;
            }
        }
        return NO_DROP;
    }
    /** Original tainted villager coin drop: nextInt(13) is below 1 + looting. */
    public static boolean dropsGoldCoin(int looting,IntUnaryOperator nextInt) { return nextInt.applyAsInt(13)<1+looting; }
    /** Original tainted sheep onSheared: one to three magenta wool blocks. */
    public static int shearWoolCount(IntUnaryOperator nextInt) { return 1+nextInt.applyAsInt(3); }

    /** Original1.7 EntityTaintChicken flap speed: three tenths up while airborne, one tenth down. */
    public static float flapSpeed(float speed,boolean onGround) {
        float next=speed+(onGround ? -1.0F : 4.0F)*0.3F;
        return next<0.0F ? 0.0F : (next>1.0F ? 1.0F : next);
    }
    /** Original1.7 EntityTaintChicken flap amplitude: one while airborne, then nine tenths per tick. */
    public static float flapAmplitude(float amplitude,boolean onGround) {
        float next=(!onGround&&amplitude<1.0F) ? 1.0F : amplitude;
        return next*0.9F;
    }
    /** Original1.7 EntityTaintChicken flap phase. */
    public static float flapPhase(float phase,float amplitude) { return phase+amplitude*2.0F; }
    /** Original1.7 EntityTaintChicken fall damping: nine tenths, not the vanilla0.6. */
    public static float fallDamping(float motionY) { return motionY*0.9F; }

    /** Original AIConvertGrass trigger: one tick in250. */
    public static final int GRASS_TRIGGER_INTERVAL=250;
    /** Original AIConvertGrass: the draw has to be zero. */
    public static boolean convertsGrass(int draw) { return draw==0; }
    /** Original AIConvertGrass eat animation length. */
    public static final int GRASS_EAT_TICKS=40;
    public static int grassTimer(int timer) { return timer>0 ? timer-1 : 0; }
    /** Original1.7 func_44003_c: the head pitch while the sheep eats. */
    public static float sheepHeadAngle(int timer,float partial) {
        if(timer<=0)return 0.0F;
        if(timer>=4&&timer<=36)return 1.0F;
        if(timer<4)return (timer-partial)/4.0F;
        return -((timer-40)-partial)/4.0F;
    }
    /** Original1.7 func_44002_d: the body yaw while the sheep eats. */
    public static float sheepBodyAngle(int timer,float partial,float renderYawOffset) {
        if(timer>4&&timer<=36) {
            float value=((timer-4)-partial)/32.0F;
            return 0.62831855F+0.2199115F*(float)Math.sin(value*28.7F);
        }
        return timer>0 ? 0.62831855F : renderYawOffset/57.295776F;
    }

    /** Original tainted villager village search radius. */
    public static final int VILLAGE_SEARCH_RADIUS=32;
    /** Original tainted villager village search cadence: 70 + a draw below50. */
    public static int villageSearchDelay(int draw) { return 70+draw; }
    /** Original tainted villager home radius: the village size times0.6. */
    public static float villageRestrictionRadius(int villageSize) { return villageSize*0.6F; }

    /** Original1.12 taint crawler fibre placement cadence. */
    public static final int CRAWLER_FIBRE_INTERVAL=40;
    /** Original1.12 taint crawler flux taint seconds: normal3, hard6, everything else none. */
    public static int crawlerFluxSeconds(int difficulty) {
        return switch(difficulty) {
            case 2 -> 3;
            case 3 -> 6;
            default -> 0;
        };
    }
    /** Original1.12 taint crawler poison roll: nextInt(bound+1) above2. */
    public static boolean crawlerFluxPoison(int bound,IntUnaryOperator nextInt) {
        return bound>0&&nextInt.applyAsInt(bound+1)>2;
    }

    /** Original1.12 taint seed spread area: the prime seed spreads over a doubled area. */
    public static int seedArea(boolean prime) { return prime ? 2 : 1; }

    /**
     * The growth supply of the taint seed.
     *
     * <p>Forced 1.21.1 change: the later Thaumcraft6 seed grows on the flux saturation of its chunk, and the TC4
     * aura has no flux and no per chunk aura at all, so the native seed grows on the taint blocks around it: the
     * block it stands on or one of its six neighbours.</p>
     */
    public static boolean seedGrowthSupply(boolean selfIsTaint, int taintedNeighbours) {
        return selfIsTaint || taintedNeighbours > 0;
    }
    public static int seedMaxHealth(boolean prime) { return prime ? 150 : 75; }
    public static int seedAttackDamage(boolean prime) { return prime ? 7 : 4; }
    public static int seedExperience(boolean prime) { return prime ? 12 : 8; }
    /** Original1.12 taint seed re-registers itself with the taint helper every1200 ticks. */
    public static final int SEED_REREGISTER_INTERVAL=1200;
    /** Original TC6 taint spread area default32: a seed keeps25.6 blocks clear of other seeds. */
    public static final double SEED_SPAWN_EXCLUSION_RADIUS=32.0D*0.8D;
    /** Original1.12 taint seed aura range: the area times4 blocks. */
    public static double seedAuraRange(int area) { return area*4.0D; }
    /** Original1.12 taint seed tentacle trigger distance: the area times256, squared. */
    public static double seedTentacleDistanceSquared(int area) { return area*256.0D; }
    /** Original1.12 taint seed flux taint potion duration. */
    public static int seedFluxDuration() { return 100; }
    /** Original1.12 taint seed flux taint potion amplifier: the area minus one. */
    public static int seedFluxAmplifier(int area) { return area-1; }
    /** Original1.12 taint seed fibre spread offset: the area times3 sideways and the area up. */
    public static int seedSpreadOffset(int area,IntUnaryOperator nextInt) { return randomInt(nextInt,-area*3,area*3); }
    /** Original1.12 taint seed vertical spread offset: the area up and down. */
    public static int seedSpreadHeight(int area,IntUnaryOperator nextInt) { return randomInt(nextInt,-area,area); }
    /** Original MathHelper.getRandomInt is inclusive on both ends. */
    public static int randomInt(IntUnaryOperator nextInt,int min,int max) { return min+nextInt.applyAsInt(max-min+1); }
    /** Original1.12 taint seed prime drop: one flux crystal plus up to two more. */
    public static int seedPrimeCrystals(BooleanSupplier nextBoolean) {
        int crystals=1;
        if(nextBoolean.getAsBoolean())crystals++;
        if(nextBoolean.getAsBoolean())crystals++;
        return crystals;
    }
}
