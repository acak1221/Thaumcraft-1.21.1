package thaumcraft.common.lib.world;

import java.util.List;

/**
 * Original TC4 eldritch bestiary numbers, cooldowns and special-state transitions.
 *
 * <p>Every constant and method here is transcribed from the TC4 4.2.3.5 oracle classes
 * {@code EntityThaumcraftBoss}, {@code EntityEldritchWarden}, {@code EntityEldritchGolem},
 * {@code EntityEldritchCrab}, {@code EntityInhabitedZombie}, {@code EntityWatcher},
 * {@code EntityThaumicSlime} and {@code EntityTaintacleGiant}. The entities call these
 * static rules so every decision stays unit testable without a world.
 */
public final class EldritchMobRules {
    private EldritchMobRules() {}

    /**
     * Original attribute row of one eldritch mob.
     *
     * @param maxHealth     maximum health attribute
     * @param attackDamage  attack damage attribute
     * @param armor         intrinsic armour points added on top of the base attribute
     * @param movementSpeed movement speed attribute
     * @param followRange   follow range attribute
     * @param experience    experience value dropped on death
     * @param talkInterval  minimum ambient sound cadence in ticks
     */
    public record Stats(float maxHealth,float attackDamage,float armor,float movementSpeed,float followRange,int experience,int talkInterval) {}

    /** Original EntityEldritchWarden title table, index 0 to 14. */
    public static final String[] WARDEN_TITLES={"Aphoom-Zhah","Basatan","Chaugnar Faugn","Mnomquah","Nyogtha","Oorn","Shaikorth",
            "Rhan-Tegoth","Rhogog","Shudde M'ell","Vulthoom","Yag-Kosha","Yibb-Tstll","Zathog","Zushakon"};

    public static final Stats WARDEN=new Stats(200F,10F,4F,.33F,40F,50,500);
    public static final Stats GOLEM=new Stats(250F,10F,6F,.30F,40F,50,80);
    public static final Stats CRAB=new Stats(20F,4F,5F,.30F,16F,6,160);
    public static final Stats INHABITED_ZOMBIE=new Stats(30F,5F,0F,.23F,35F,5,80);
    public static final Stats WATCHER=new Stats(30F,6F,0F,.50F,16F,10,160);
    public static final Stats THAUMIC_SLIME=new Stats(20F,2F,0F,.70F,16F,5,80);
    public static final Stats TAINTACLE_GIANT=new Stats(125F,9F,0F,.70F,16F,20,80);

    /** Original EntityEldritchCrab speed written by the attribute initialiser while the helm flag is still false. */
    public static final float CRAB_BASE_SPEED=.3F;
    /** Original EntityEldritchCrab dormant helmeted speed, never applied because the helm flag is false during attribute setup. */
    public static final float CRAB_HELMETED_SPEED=.275F;

    // ------------------------------------------------------------------ shared boss base
    public static final float BOSS_HOME_RADIUS=24F;
    /** Original TC4 melee helper AI attack cooldown in ticks. */
    public static final int MELEE_AI_COOLDOWN=10;
    public static final float BOSS_KNOCKBACK_RESISTANCE=.95F;
    public static final int BOSS_HEAL_INTERVAL=30;
    public static final float BOSS_HEAL_AMOUNT=1F;
    public static final int BOSS_AGGRO_INTERVAL=20;
    public static final float BOSS_STALE_DISTANCE_SQUARED=16384F;
    public static final float BOSS_DAMAGE_CAP=35F;
    public static final int BOSS_ANGER_TICKS=200;
    public static final int BOSS_MAX_BONUS_PLAYERS=5;
    public static final float BOSS_HEALTH_BONUS=50F;
    public static final float BOSS_DAMAGE_BONUS=.5F;

    /** Original EntityThaumcraftBoss caps every incoming hit at 35 damage. */
    public static float bossDamageCap(float damage) { return Math.min(damage,BOSS_DAMAGE_CAP); }
    /** Original boss scaling grants one bonus tier per extra player, five at most. */
    public static int bossBonusPlayers(int players) { return Math.min(BOSS_MAX_BONUS_PLAYERS,Math.max(0,players-1)); }
    /**
     * Original aggro gate of the shared boss base: a candidate has to beat the current target aggro
     * by 25 and by ten percent. The third marker is the original loop state, which the oracle
     * overwrites with the candidate entity id instead of its aggro value; that quirk is preserved.
     */
    public static boolean bossAggroQualifies(int candidateAggro,int currentAggro,int highestMarker) {
        return candidateAggro>currentAggro+25 && (double)candidateAggro>(double)currentAggro*1.1D && candidateAggro>highestMarker;
    }
    /** Original boss enrage window: regeneration, damage boost and speed for 200 ticks. */
    public static int bossRegenerationAmplifier(float damage) { return (int)(damage/15F); }
    public static int bossStrengthAmplifier(float damage) { return (int)(damage/40F); }
    public static int bossSpeedAmplifier(float damage) { return (int)(damage/40F); }
    /** Original eldritch object item id, dropped by every eldritch boss. */
    public static final String ELDRITCH_OBJECT_ID="eldritch_object";
    /** Original cultist plate item ids worn and dropped by the eldritch crab and the inhabited zombie. */
    public static final String CULTIST_PLATE_HELMET_ID="cultist_plate_helmet";
    public static final String CULTIST_PLATE_CHEST_ID="cultist_plate_chest";
    public static final String CULTIST_PLATE_LEGS_ID="cultist_plate_legs";
    /** Original airy block id used by the warden eldritch field. */
    public static final String AIRY_BLOCK_ID="airy";

    // ------------------------------------------------------------------ warden
    public static final int WARDEN_SPAWN_TIMER=150;
    public static final int WARDEN_FREENZY_TICKS=150;
    public static final int WARDEN_FREENZY_LAST_RING=121;
    public static final int WARDEN_FREENZY_RING_INTERVAL=10;
    public static final int WARDEN_FREENZY_SAMPLES=180;
    public static final float WARDEN_WARD_RATIO=.66F;
    public static final float WARDEN_SCREECH_CHANCE=.2F;
    public static final int WARDEN_WARP_MIN=3;
    public static final int WARDEN_WARP_SPAN=3;
    public static final int WARDEN_SCREECH_TICKS=400;
    public static final int WARDEN_WARD_INTERVAL=25;

    /** Original warden ward ceiling is 66 percent of the effective maximum health. */
    public static int wardenWardCap(float maxHealth) { return (int)(maxHealth*WARDEN_WARD_RATIO); }
    /** Original warden regenerates one ward point every 25 ticks while below the ceiling. */
    public static float wardenWardRegeneration(float absorption,int cap) { return absorption<cap?absorption+1F:absorption; }
    /** Original warden keeps its ward topped up only while it is not freshly hurt. */
    public static boolean wardenWardsRegenerate(int hurtTime,int tickCount) { return hurtTime<=0&&tickCount%WARDEN_WARD_INTERVAL==0; }
    /** Original field frenzy lays a ring every ten ticks until the counter drops below 121. */
    public static boolean wardenFrenzyRingDue(int counter) { return counter<WARDEN_FREENZY_LAST_RING&&counter%WARDEN_FREENZY_RING_INTERVAL==0; }
    public static double wardenFrenzyRadius(int counter) { return (WARDEN_FREENZY_TICKS-counter)/8D; }
    public static int wardenFrenzyStep(int counter) { return 1+counter/8; }
    public static int wardenFrenzySamples(int counter) { return WARDEN_FREENZY_SAMPLES/wardenFrenzyStep(counter); }
    public static double wardenFrenzyAngle(int sample,int step) { return Math.toRadians(sample*2*step); }
    /** Original warden teleports home exactly when the frenzy counter is still at its start value. */
    public static boolean wardenFrenzyTeleports(int counter) { return counter==WARDEN_FREENZY_TICKS; }
    /** Original warden fires an eldritch orb unless the roll falls into the screech window. */
    public static boolean wardenUsesOrb(float roll) { return roll>WARDEN_SCREECH_CHANCE; }
    /** Original warden warp on screech is three to five. */
    public static int wardenWarpAmount(int nextInt3) { return WARDEN_WARP_MIN+nextInt3; }
    /** Original warden alternates the firing arm by ninety or one hundred eighty degrees. */
    public static int wardenArmAngle(boolean rightArm) { return rightArm?90:180; }

    // ------------------------------------------------------------------ eldritch golem
    public static final float GOLEM_EYE_HEIGHT=3F;
    public static final float GOLEM_HEADLESS_EYE_HEIGHT=3.33F;
    public static final int GOLEM_MELEE_COOLDOWN=10;
    public static final float GOLEM_MELEE_DAMAGE_FACTOR=.75F;
    public static final double GOLEM_MELEE_KNOCKBACK_Y=.2D;
    public static final int GOLEM_BEAM_MAX_CHARGE=150;
    public static final int GOLEM_BEAM_DRAIN_MIN=15;
    public static final int GOLEM_BEAM_DRAIN_SPAN=5;
    public static final float GOLEM_BEAM_SPEED=.66F;
    public static final float GOLEM_BEAM_INACCURACY=5F;
    public static final int GOLEM_SPAWN_TIMER=100;
    public static final int GOLEM_ARC_MIN=8;
    public static final int GOLEM_ARC_SPAN=5;
    public static final float GOLEM_ARC_VOLUME=.65F;
    public static final float GOLEM_SMASH_STRENGTH=2F;
    public static final double GOLEM_HEADLESS_KNOCKBACK=1.5D;

    public static float golemEyeHeight(boolean headless) { return headless?GOLEM_HEADLESS_EYE_HEIGHT:GOLEM_EYE_HEIGHT; }
    /** Original golem melee deals three quarters of its attack damage. */
    public static float golemMeleeDamage(float attackDamage) { return attackDamage*GOLEM_MELEE_DAMAGE_FACTOR; }
    /** Original golem beam drains fifteen to nineteen charge per shot. */
    public static int golemBeamDrain(int charge,int nextInt5) { return charge-(GOLEM_BEAM_DRAIN_MIN+nextInt5%GOLEM_BEAM_DRAIN_SPAN); }
    /** Original golem only charges its beam while it is headless and not already charging. */
    public static boolean golemChargesBeam(boolean headless,boolean charging,int charge) {
        return headless&&!charging&&charge<=0;
    }
    /** Original golem loses its head on any hit that exceeds its current health. */
    public static boolean golemBeheads(float damage,float health,boolean headless) { return !headless&&damage>health; }
    /** Original golem arcing lightning lasts eight to twelve ticks. */
    public static int golemArcTicks(int nextInt5) { return GOLEM_ARC_MIN+nextInt5%GOLEM_ARC_SPAN; }

    // ------------------------------------------------------------------ eldritch crab
    public static final float CRAB_HELMET_ARMOR=5F;
    public static final float CRAB_HELMET_BREAK_RATIO=.5F;
    public static final float CRAB_HELMET_CHANCE=.33F;
    public static final int CRAB_RIDE_INTERVAL_MIN=10;
    public static final int CRAB_RIDE_INTERVAL_SPAN=10;
    public static final float CRAB_DISMOUNT_CHANCE=.2F;
    public static final double CRAB_ATTACH_DISTANCE_SQUARED=4D;
    public static final int CRAB_EYE_ROLL=3;

    /** Original crab always wears its helm on hard and one third of the time otherwise. */
    public static boolean crabHasHelm(int difficultyId,float roll) { return difficultyId>=3||roll<CRAB_HELMET_CHANCE; }
    /** Original crab loses its helm once it drops to half health. */
    public static boolean crabHelmetBreaks(float health,float maxHealth) { return maxHealth>0&&health/maxHealth<=CRAB_HELMET_BREAK_RATIO; }
    /** Original crab ride attacks land every ten to nineteen ticks. */
    public static int crabRideInterval(int nextInt10) { return CRAB_RIDE_INTERVAL_MIN+nextInt10%CRAB_RIDE_INTERVAL_SPAN; }
    /** Original crab dismounts from its victim one time in five. */
    public static boolean crabDismounts(float roll) { return roll<CRAB_DISMOUNT_CHANCE; }
    /** Original crab eye drop: one in three, or one in (one plus looting). */
    public static boolean crabDropsEye(int eyeRoll,int lootingRoll) { return eyeRoll==0||lootingRoll>0; }
    /** Probability of the original crab eye drop for a given looting level. */
    public static float crabEyeChance(int looting) { return 1F/3F+(2F/3F)*(looting/(looting+1F)); }

    // ------------------------------------------------------------------ inhabited zombie
    public static final float INHABITED_PLATE_CHANCE_NORMAL=.6F;
    public static final float INHABITED_PLATE_CHANCE_HARD=.9F;
    public static final double INHABITED_ISOLATION_RADIUS=32D;
    public static final double INHABITED_ISOLATION_HEIGHT=16D;

    /** Original inhabited zombie wears its chest and leg plates with a difficulty dependent chance. */
    public static float inhabitedPlateChance(int difficultyId) { return difficultyId>=3?INHABITED_PLATE_CHANCE_HARD:INHABITED_PLATE_CHANCE_NORMAL; }

    // ------------------------------------------------------------------ watcher
    public static final int WATCHER_GAZE_TICKS=80;
    public static final int WATCHER_ELDER_GAZE_TICKS=60;
    public static final float WATCHER_THORNS_DAMAGE=2F;
    public static final float WATCHER_ELDER_SIZE=1.9975F;
    public static final float WATCHER_BASE_SIZE=.85F;
    public static final int WATCHER_EYE_HEIGHT_DIVISOR=2;

    /** Original watcher gazes for eighty ticks, or sixty while it is the elder size. */
    public static int watcherGazeDuration(boolean elder) { return elder?WATCHER_ELDER_GAZE_TICKS:WATCHER_GAZE_TICKS; }
    /** Original watcher gaze damage is one, plus two on hard, plus two while elder. */
    public static float watcherGazeDamage(int difficultyId,boolean elder) {
        float damage=1F;
        if(difficultyId>=3)damage+=2F;
        if(elder)damage+=2F;
        return damage;
    }
    /** Original watcher gaze progress runs from zero to one over the gaze window. */
    public static float watcherGazeProgress(int ticks,int duration) { return duration<=0?0F:(ticks/(float)duration); }

    // ------------------------------------------------------------------ thaumic slime
    public static final int SLIME_MAX_SIZE=100;
    public static final int SLIME_SPIT_COUNTER=101;
    public static final int SLIME_SPIT_INTERVAL_TICKS=100;
    public static final int SLIME_INITIAL_JUMP_MIN=10;
    public static final int SLIME_INITIAL_JUMP_SPAN=20;
    public static final int SLIME_JUMP_DELAY_MIN=8;
    public static final int SLIME_JUMP_DELAY_SPAN=16;
    public static final double SLIME_SPIT_RANGE=4D;
    public static final int SLIME_LAUNCHED=10;
    public static final int SLIME_SPLIT_MIN_SIZE=2;
    public static final int SLIME_SIZE_VARIANTS=3;

    /** Original thaumic slime starts at size one, two or four. */
    public static int slimeInitialSize(int nextInt3) { return 1<<(nextInt3%SLIME_SIZE_VARIANTS); }
    /** Original slime jump delay is eight to twenty three ticks on spawn. */
    public static int slimeInitialJumpDelay(int nextInt20) { return SLIME_INITIAL_JUMP_MIN+nextInt20%SLIME_INITIAL_JUMP_SPAN; }
    /** Original slime jump delay between hops is eight to twenty three ticks. */
    public static int slimeJumpDelay(int nextInt16) { return SLIME_JUMP_DELAY_MIN+nextInt16%SLIME_JUMP_DELAY_SPAN; }
    /** Original slime attack strength equals its size. */
    public static int slimeAttackStrength(int size) { return size; }
    /** Original slime sound volume scales with the square root of its size. */
    public static float slimeSoundVolume(int size) { return .1F*(float)Math.sqrt(size); }
    /** Original slime merges into a neighbour up to size one hundred. */
    public static int slimeMergedSize(int target,int source) { return Math.min(SLIME_MAX_SIZE,target+source); }
    /** Original slime splits into the square root of its size. */
    public static int slimeSplitCount(int size) { return (int)Math.sqrt(size); }
    /** Original slime spits at players only when it is larger than three. */
    public static boolean slimeCanSpit(int size) { return size>3; }
    /** Original slime only splits when it is larger than one. */
    public static boolean slimeSplits(int size) { return slimeSplitCount(size)>1; }
    /** Original slime half width and height follow 0.25 times the square root of the size plus 0.25. */
    public static float slimeDimension(int size) { return .25F*(float)Math.sqrt(size)+.25F; }
    /** Original slime health equals its size. */
    public static float slimeHealth(int size) { return size; }

    // ------------------------------------------------------------------ giant taintacle
    public static final int GIANT_ANGER_TICKS=200;
    public static final float GIANT_DAMAGE_CAP=35F;
    public static final float GIANT_WIDTH=1.1F;
    public static final float GIANT_HEIGHT=6F;

    /** Original giant taintacle enrage effects for a hit above the cap. */
    public record Enrage(int regeneration,int strength,int speed) {}
    public static Enrage giantEnrageEffects(float damage) {
        return new Enrage((int)(damage/15F),(int)(damage/40F),(int)(damage/40F));
    }
    /** Original giant taintacle caps every hit at 35 damage. */
    public static float giantDamageCap(float damage) { return Math.min(damage,GIANT_DAMAGE_CAP); }
    /** Original giant taintacle only enrages once per anger window. */
    public static boolean giantEnrages(float damage,int anger) { return damage>GIANT_DAMAGE_CAP&&anger==0; }
    /** Original giant taintacle drops its special object only when no other giant is close by. */
    public static boolean giantDropsSpecialObject(int nearbyGiants) { return nearbyGiants<=0; }

    /** Original titles are picked uniformly from the fifteen entries. */
    public static String wardenTitle(int index) { return WARDEN_TITLES[Math.floorMod(index,WARDEN_TITLES.length)]; }
    /** Original warden title list as an immutable view. */
    public static List<String> wardenTitles() { return List.of(WARDEN_TITLES); }
}
