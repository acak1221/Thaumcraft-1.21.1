package thaumcraft.common.lib.cult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Original TC4 cultist faction rules, transcribed from the 4.2.3.5 oracle.
 *
 * <p>Sources: {@code thaumcraft/common/entities/monster/EntityCultist.java},
 * {@code EntityCultistCleric.java}, {@code EntityCultistKnight.java},
 * {@code thaumcraft/common/entities/monster/boss/EntityCultistLeader.java},
 * {@code EntityCultistPortal.java} and the shared boss base
 * {@code boss/EntityThaumcraftBoss.java}. Every value here is pure so it can be
 * asserted without a world, a registry or a running game.
 */
public final class CultistRules {
    private CultistRules() {}

    // ---------------------------------------------------------------- EntityCultist
    /** Original EntityMob default: twenty health before the cultist's own attributes. */
    public static final double CULTIST_HEALTH = 20.0;
    public static final double CULTIST_DAMAGE = 4.0;
    public static final double CULTIST_SPEED = 0.3;
    public static final double CULTIST_FOLLOW_RANGE = 32.0;
    public static final int CULTIST_EXPERIENCE = 10;
    public static final float CULTIST_WIDTH = 0.6F;
    public static final float CULTIST_HEIGHT = 1.8F;

    /** Original EntityCultistCleric: thirty health, everything else inherited. */
    public static final double CLERIC_HEALTH = 30.0;
    /** Original EntityCultistKnight: thirty-six health, everything else inherited. */
    public static final double KNIGHT_HEALTH = 36.0;

    // ---------------------------------------------------------------- cultist drops
    /** Original ItemResource metadata of the three cultist drops. */
    public static final int RESOURCE_NONE = -1;
    public static final int RESOURCE_KNOWLEDGE_FRAGMENT = 9;
    public static final int RESOURCE_VOID_SEED = 17;
    public static final int RESOURCE_GOLD_COIN = 18;

    /**
     * Original EntityCultist.dropFewItems: one nextInt(10) roll picks the drop.
     *
     * @param roll    the original nextInt(10) result
     * @param looting the original looting level
     */
    public static int resourceDrop(int roll, int looting) {
        if (roll == 0) return RESOURCE_KNOWLEDGE_FRAGMENT;
        if (roll <= 1) return RESOURCE_VOID_SEED;
        if (roll <= 3 + looting) return RESOURCE_GOLD_COIN;
        return RESOURCE_NONE;
    }

    // ---------------------------------------------------------------- cultist gear
    /** Original EntityCultistCleric boots: hard difficulty only. */
    public static float clericBootsChance(boolean hard) { return hard ? 0.3F : 0.1F; }

    /** Original EntityCultistKnight rare gear: hard difficulty only. */
    public static float knightRareChance(boolean hard) { return hard ? 0.05F : 0.01F; }

    public static final int KNIGHT_WEAPON_IRON = 0;
    public static final int KNIGHT_WEAPON_VOID = 1;
    public static final int KNIGHT_WEAPON_THAUMIUM = 2;

    /** Original EntityCultistKnight: one nextInt(5) roll picks the rare weapon. */
    public static int knightRareWeapon(int roll) { return roll == 0 ? KNIGHT_WEAPON_VOID : KNIGHT_WEAPON_THAUMIUM; }

    /** Original EntityCultistKnight: the robe helm replaces the plate helm only on the void roll. */
    public static boolean knightUsesRobeHelm(int roll) { return roll == 0; }

    /** Original EntityCultistKnight: the thaumium roll drops the helm on a coin flip. */
    public static boolean knightKeepsHelm(boolean randomBoolean) { return !randomBoolean; }

    /** Original EntityCultistKnight enchantEquipment: 0.25 * tension. */
    public static float knightEnchantChance(float tension) { return 0.25F * tension; }

    /** Original EntityCultistKnight enchantEquipment power: (int)(5 + tension * nextInt(18)). */
    public static int knightEnchantPower(float tension, int roll) { return (int) (5.0F + tension * (float) roll); }

    /** Original EntityCultistLeader enchantEquipment: 0.5 * tension. */
    public static float leaderEnchantChance(float tension) { return 0.5F * tension; }

    /** Original EntityCultistLeader enchantEquipment power: (int)(7 + tension * nextInt(22)). */
    public static int leaderEnchantPower(float tension, int roll) { return (int) (7.0F + tension * (float) roll); }

    /** Original EntityCultistLeader: easy difficulty arms the void sword, everything else the crimson sword. */
    public static boolean leaderUsesVoidSword(boolean easy) { return easy; }

    // ---------------------------------------------------------------- cleric combat
    /** Original EntityCultistCleric.attackEntityWithRangedAttack: the orb needs a roll above 0.66. */
    public static boolean clericFiresOrb(float roll) { return roll > 0.66F; }

    /** Original EntityCultistCleric: three small fireballs. */
    public static int clericFireballCount() { return 3; }

    /** Original EntityCultistCleric fireball spread: sqrt(distance) * 0.5. */
    public static float fireballSpread(float distance) { return (float) Math.sqrt(distance) * 0.5F; }

    /** Original EntityCultistCleric fires the fireballs from half a block above its own centre. */
    public static double fireballSpawnOffset() { return 0.5; }

    // ---------------------------------------------------------------- cleric ritualist
    /** Original AIAltarFocus compares a squared block distance against sixteen. */
    public static final double RITUALIST_MAX_HOME_DISTANCE_SQ = 16.0;

    /**
     * Original AIAltarFocus.updateTask: the ritualist breaks when it drifts from the
     * altar block or when the altar block is no longer an eldritch altar.
     */
    public static boolean ritualistShouldStop(double homeDistanceSq, boolean homeIsEldritchAltar) {
        return homeDistanceSq > RITUALIST_MAX_HOME_DISTANCE_SQ || !homeIsEldritchAltar;
    }

    // ---------------------------------------------------------------- cultist leader
    public static final double LEADER_HEALTH = 125.0;
    public static final double LEADER_DAMAGE = 5.0;
    public static final double LEADER_SPEED = 0.32;
    public static final int LEADER_EXPERIENCE = 40;
    public static final float LEADER_WIDTH = 0.75F;
    public static final float LEADER_HEIGHT = 2.25F;

    /** Original EntityCultistLeader title list. */
    public static final String[] LEADER_TITLES = {
            "Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse",
            "Dodorol", "Ebardo", "Ferrando", "Fertus", "Guillen", "Larpe", "Obano", "Zelipe"
    };

    /** Original EntityCultistLeader.getTitle: the byte index is clamped to the list. */
    public static String leaderTitle(int index) {
        return LEADER_TITLES[Math.max(0, Math.min(index, LEADER_TITLES.length - 1))];
    }

    /** Original EntityCultistLeader.updateAITasks aura: eight blocks, regeneration II for three seconds. */
    public static final double LEADER_AURA_RANGE = 8.0;
    public static final int LEADER_AURA_DURATION = 60;
    public static final int LEADER_AURA_AMPLIFIER = 1;

    // ---------------------------------------------------------------- shared boss base
    public static final double BOSS_KNOCKBACK_RESISTANCE = 0.95;
    public static final double BOSS_FOLLOW_RANGE = 40.0;
    public static final int BOSS_HOME_RADIUS = 24;
    public static final int BOSS_TARGET_SWITCH_INTERVAL = 20;
    /** Original EntityThaumcraftBoss: a candidate must beat the current target by this margin. */
    public static final int BOSS_AGGRO_MARGIN = 25;
    /** Original EntityThaumcraftBoss: a candidate must beat the current target by this ratio. */
    public static final double BOSS_AGGRO_RATIO = 1.1;
    /** Original EntityThaumcraftBoss: targets further away than this are dropped from the table. */
    public static final double BOSS_MAX_TARGET_DISTANCE_SQ = 16384.0;
    /** Original EntityThaumcraftBoss: a single hit above this enrages the boss. */
    public static final float BOSS_ENRAGE_THRESHOLD = 35.0F;
    public static final int BOSS_ENRAGE_ANGER = 200;
    public static final int BOSS_ENRAGE_DURATION = 200;
    /** Original EntityThaumcraftBoss health buff per additional player. */
    public static final double BOSS_HEALTH_BUFF = 50.0;
    /** Original EntityThaumcraftBoss damage buff per additional player. */
    public static final double BOSS_DAMAGE_BUFF = 0.5;
    /** Original EntityThaumcraftBoss: at most five buffs are applied. */
    public static final int BOSS_BUFF_STEPS = 5;

    /** Original EntityThaumcraftBoss: the enrage damage is capped at the threshold. */
    public static float bossDamageCap(float damage) { return Math.min(damage, BOSS_ENRAGE_THRESHOLD); }

    /** Original EntityThaumcraftBoss: regeneration amplifier is damage / 15. */
    public static int bossEnrageRegenerationAmplifier(float damage) { return (int) (damage / 15.0F); }

    /** Original EntityThaumcraftBoss: strength and speed amplifiers are damage / 40. */
    public static int bossEnrageBuffAmplifier(float damage) { return (int) (damage / 40.0F); }

    /** Original EntityThaumcraftBoss: the damage table accumulates the hits of the same attacker. */
    public static int bossAggroDamage(int damage, int stored) { return damage + (stored > 0 ? stored : 0); }

    /**
     * Probe of a target id for the boss rotation: 0 means the entity is gone, 1 means it
     * is not a player and 2 means it is a player.
     */
    public interface TargetProbe { int probe(int entityId); }

    /**
     * Result of one original boss rotation pass.
     *
     * @param targetId the entity the boss would switch to, -1 when there is none
     * @param finalId  the id the original kept as its current candidate
     * @param players  the number of player candidates the original counted
     * @param removed  the aggro entries the original dropped
     */
    public record AggroRotation(int targetId, int finalId, int players, List<Integer> removed) {
        public AggroRotation {
            removed = List.copyOf(removed);
        }
    }

    /**
     * Original EntityThaumcraftBoss target rotation. The original resolves the entity of the
     * <em>previous</em> candidate id and stores that entity id, not its damage, in the
     * comparison slot. Both quirks are preserved.
     */
    public static AggroRotation rotateAggro(Map<Integer, Integer> aggro, int currentTargetId, TargetProbe probe) {
        List<Integer> removed = new ArrayList<>();
        int id = currentTargetId;
        int current = aggro.getOrDefault(id, 0);
        int last = current;
        int players = 0;
        int target = -1;
        for (Map.Entry<Integer, Integer> entry : aggro.entrySet()) {
            int candidate = entry.getKey();
            int damage = entry.getValue();
            if (damage <= current + BOSS_AGGRO_MARGIN || !(damage > current * BOSS_AGGRO_RATIO) || damage <= last) continue;
            int resolved = probe.probe(id);
            if (resolved == 0) {
                removed.add(candidate);
                continue;
            }
            target = id;
            if (resolved == 2) players++;
            id = candidate;
            last = candidate;
        }
        return new AggroRotation(target, id, players, removed);
    }

    // ---------------------------------------------------------------- champion modifiers
    /** Original ChampionModifier.mods order, thirteen entries. */
    public static final String[] CHAMPION_MOD_NAMES = {
            "bold", "spine", "armor", "mighty", "grim", "warded", "warp",
            "undying", "fiery", "sickly", "venomous", "vampiric", "infested"
    };

    public static final String[] CHAMPION_MOD_DISPLAY_NAMES = {
            "Bold", "Spined", "Armored", "Mighty", "Grim", "Warded", "Warped",
            "Undying", "Fiery", "Sickly", "Venomous", "Vampiric", "Infested"
    };

    public static final int CHAMPION_COUNT = 13;
    public static final int CHAMPION_BOLD = 0;
    public static final int CHAMPION_MIGHTY = 3;
    public static final int CHAMPION_WARDED = 5;
    public static final int CHAMPION_WARP = 6;

    /** Original EntityUtils.makeChampion: the warded champion absorbs half of its health. */
    public static float championAbsorption(float maxHealth) { return maxHealth / 2.0F; }

    /** Original ChampionModWarp: 33 percent of the hits warp the player by one to three. */
    public static final float WARP_REWARD_CHANCE = 0.33F;

    /** Original ChampionModWarp: one to three temporary warp. */
    public static int warpReward(int roll) { return 1 + roll; }

    // ---------------------------------------------------------------- cultist portal
    public static final double PORTAL_HEALTH = 500.0;
    public static final double PORTAL_KNOCKBACK_RESISTANCE = 1.0;
    public static final int PORTAL_EXPERIENCE = 30;
    public static final float PORTAL_WIDTH = 1.5F;
    public static final float PORTAL_HEIGHT = 3.0F;

    /** Original EntityCultistPortal: the stage that arms the praetor. */
    public static final int PORTAL_BOSS_STAGE = 12;
    /** Original EntityCultistPortal: the stage counter value that places the four banners. */
    public static final int PORTAL_BANNER_STAGE_COUNTER = 160;
    /** Original EntityCultistPortal: the banner ring radius in blocks. */
    public static final int PORTAL_BANNER_RADIUS = 6;
    /** Original EntityCultistPortal: the loot crates appear between these stage counter values. */
    public static final int PORTAL_CRATE_MIN_STAGE_COUNTER = 20;
    public static final int PORTAL_CRATE_MAX_STAGE_COUNTER = 150;
    /** Original EntityCultistPortal: one crate every thirteen stage counter ticks. */
    public static final int PORTAL_CRATE_INTERVAL = 13;
    /** Original EntityCultistPortal: a player inside this range advances the fight. */
    public static final double PORTAL_ACTIVATION_RANGE = 48.0;
    /** Original EntityCultistPortal: cultists inside this range set the fight tempo. */
    public static final double PORTAL_MINION_RANGE = 32.0;
    /** Original EntityCultistPortal: the spawned minions anchor their home here. */
    public static final int PORTAL_MINION_HOME_RADIUS = 32;

    /** Original EntityCultistPortal.getTiming: twenty ticks per living cultist. */
    public static int portalTiming(int cultistCount) { return cultistCount * 20; }

    /** Original EntityCultistPortal stage machine. */
    public static int portalStageCounter(int stage, int cultistCount, IntUnaryOperator random) {
        if (stage >= 0 && stage <= 4) return 15 + random.applyAsInt(10 - stage) - stage;
        if (stage == PORTAL_BOSS_STAGE) return 50 + portalTiming(cultistCount) * 2 + random.applyAsInt(50);
        int timing = portalTiming(cultistCount);
        return timing + random.applyAsInt(5 + timing / 3);
    }

    /** Original EntityCultistPortal: the praetor is summoned at stage twelve. */
    public static boolean portalSpawnsBoss(int stage) { return stage == PORTAL_BOSS_STAGE; }

    /** Original EntityCultistPortal: every stage but the praetor stage sends minions. */
    public static boolean portalSpawnsMinions(int stage) { return stage != PORTAL_BOSS_STAGE; }

    /** Original EntityCultistPortal.spawnMinions: a roll above 0.33 makes a knight, otherwise a cleric. */
    public static boolean minionIsKnight(float roll) { return roll > 0.33F; }

    /** Original EntityCultistPortal loot crates: five percent rare, fifteen percent uncommon. */
    public static int lootCrateVariant(float roll) { return roll < 0.05F ? 2 : (roll < 0.2F ? 1 : 0); }

    /** Original EntityCultistPortal.spawnMinions: past the boss stage the portal burns itself. */
    public static int portalSelfDamage(int stage, int roll) { return stage > PORTAL_BOSS_STAGE ? 5 + roll : 0; }

    /** Original EntityCultistPortal: the portal heals one point per tick until the boss stage. */
    public static float portalHeal(int stage) { return stage < PORTAL_BOSS_STAGE ? 1.0F : 0.0F; }

    /** Original EntityCultistPortal: the four banners stand on the four cardinal directions. */
    public static int bannerFacing(int directionIndex) {
        return switch (directionIndex) {
            case 2 -> 8;
            case 3 -> 0;
            case 4 -> 12;
            case 5 -> 4;
            default -> 0;
        };
    }

    // ---------------------------------------------------------------- native ids
    /** Native ids of the cultist gear, reserved by the native language file. */
    public static final String ITEM_ROBE_HOOD = "cultist_robe_hood";
    public static final String ITEM_ROBE_CHESTPLATE = "cultist_robe";
    public static final String ITEM_ROBE_LEGGINGS = "cultist_robe_legs";
    public static final String ITEM_PLATE_HELM = "cultist_plate_helm";
    public static final String ITEM_PLATE_CHESTPLATE = "cultist_plate";
    public static final String ITEM_PLATE_LEGGINGS = "cultist_plate_legs";
    public static final String ITEM_LEADER_HELM = "cultist_leader_helm";
    public static final String ITEM_LEADER_CHESTPLATE = "cultist_leader";
    public static final String ITEM_LEADER_LEGGINGS = "cultist_leader_legs";
    public static final String ITEM_BOOTS = "cultist_boots";
    public static final String ITEM_CRIMSON_SWORD = "crimson_sword";
    /** Native id candidates of the eldritch altar the cleric ritualist focuses on. */
    public static final String[] BLOCK_ELDRITCH_ALTAR_IDS = {"eldritch_altar", "eldritch"};
    /** Native id of the wooden device that carries the crimson cult banner. */
    public static final String[] BLOCK_WOODEN_DEVICE_IDS = {"wooden_device"};
    /** Native id of the abandoned loot crate the portal seeds around itself. */
    public static final String[] BLOCK_LOOT_CRATE_IDS = {"loot_crate"};
}
