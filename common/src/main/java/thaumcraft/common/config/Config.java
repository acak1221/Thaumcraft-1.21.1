package thaumcraft.common.config;

import java.util.ArrayList;
import thaumcraft.api.aspects.Aspect;

/**
 * The public configuration surface of the original.
 *
 * <p>Oracle {@code thaumcraft/common/config/Config.java:102-200} declares the public static
 * options of the original and the static block at {@code :830-895} gives their original
 * defaults. The original wrote them into a Forge {@code Configuration} file; the native port
 * keeps a properties file ({@link ThaumcraftSettings}) and this class publishes the original
 * options with their original defaults, so that an add-on that reads
 * {@code Config.hardNode} or {@code Config.dimensionOuterId} still finds them.</p>
 *
 * <p>Forced 1.21.1 changes:</p>
 * <ul>
 *   <li>The Forge {@code Configuration} and its {@code Property} do not exist, so the
 *   {@code config} field of the original is not carried over; the options are read from the
 *   native properties file and copied into this class by {@link #apply(ResearchSettings)} and
 *   {@link #apply(ThaumcraftTaintSettings.Values)}.</li>
 *   <li>The 1.7 {@code Material} of the air, the flux goo and the taint do not exist in 1.21.1,
 *   so the three material fields of the original are not carried over.</li>
 *   <li>The 1.7 {@code Enchantment} does not exist in 1.21.1, so the two enchantment fields keep
 *   their original names with the type {@code Object} and stay {@code null}.</li>
 *   <li>The potion ids of the original are the 1.7.10 ids of the {@code Potion} array; 1.21.1
 *   registers the effects in a registry, so the ids are kept as the original constants.</li>
 * </ul>
 *
 * <p>The options that a configuration replaces are the runtime values of the original, exactly
 * like the original's own fields. Their <em>declared</em> defaults - the values of the original's
 * static block - are published by {@link Defaults}, because the runtime values overwrite them
 * as soon as a configuration is read.</p>
 */
public class Config {
    /**
     * The declared defaults of the public options.
     *
     * <p>The original published the eighty-six public options in its static block (oracle
     * {@code thaumcraft/common/config/Config.java:830-895}) and replaced only a part of them
     * with the values of its configuration file ({@code :388-392} and
     * {@code :457-459}) or with the snapshot the server sent to the client (oracle
     * {@code thaumcraft/common/lib/network/misc/PacketConfig.java:33-53}). The public options
     * of the native port carry the same runtime values, so the declared defaults of the
     * original's static block are published here: they are the values the original published
     * before it read its configuration, and they survive every configuration.</p>
     *
     * <p>One option of the original has a configuration default that differs from its
     * declared default: the cheat sheet is declared {@code true} by the static block
     * ({@code :869}) while the original's configuration file writes {@code false} for it
     * ({@code :456}), so a configured server disables the cheat sheet.</p>
     */
    public static final class Defaults {
        /** Original Config.java:869 - the static block declares the cheat sheet allowed. */
        public static final boolean ALLOW_CHEAT_SHEET = true;
        /** Original Config.java:882 - the cheat sheet copy the client reads. */
        public static final boolean CALLOW_CHEAT_SHEET = true;
        /** Original Config.java:871 - the warded stone. */
        public static final boolean WARDED_STONE = true;
        /** Original Config.java:880 - the warded stone copy the client reads. */
        public static final boolean CWARDED_STONE = true;
        /** Original Config.java:851 - the arcane mirrors. */
        public static final boolean ALLOW_MIRRORS = true;
        /** Original Config.java:879 - the mirrors copy the client reads. */
        public static final boolean CALLOW_MIRRORS = true;
        /** Original Config.java:841 - the hard mode of the nodes. */
        public static final boolean HARD_NODE = true;
        /** Original Config.java:878 - the hard node copy the client reads. */
        public static final boolean CHARD_NODE = true;
        /** Original Config.java:842 - the wuss mode. */
        public static final boolean WUSS = false;
        /** Original Config.java:877 - the wuss mode copy the client reads. */
        public static final boolean CWUSS = false;
        /** Original Config.java:887 - the research difficulty. */
        public static final int RESEARCH_DIFFICULTY = 0;
        /** Original Config.java:888 - the research difficulty copy the client reads. */
        public static final int CRESEARCH_DIFFICULTY = 0;
        /** Original Config.java:888 - the aspect cap of the research pool. */
        public static final int ASPECT_TOTAL_CAP = 100;
        /** Original Config.java:889 - the aspect cap copy the client reads. */
        public static final int CASPECT_TOTAL_CAP = 100;
        /** Original Config.java:836 - the spread rate of the taint. */
        public static final int TAINT_SPREAD_RATE = 200;
        /** Original Config.java:876 - the glowing taint. */
        public static final boolean GLOWY_TAINT = true;
        /** Original Config.java:894 - the taint spores. */
        public static final boolean SPAWN_TAINT_SPORE = true;

        private Defaults() {}
    }

    /** The category names of the original configuration file. */
    public static final String CATEGORY_ENCH = "Enchantments";
    public static final String CATEGORY_ENTITIES = "Entities";
    public static final String CATEGORY_BIOMES = "Biomes";
    public static final String CATEGORY_RESEARCH = "Research";
    public static final String CATEGORY_GEN = "World_Generation";
    public static final String CATEGORY_REGEN = "World_Regeneration";
    public static final String CATEGORY_SPAWN = "Monster_Spawning";
    public static final String CATEGORY_RUNIC = "Runic_Shielding";
    /** The original size of the aura of a node. */
    public static final float auraSize = 4.0f;

    public static int biomeTaintID = 192;
    public static int biomeMagicalForestID = 193;
    public static int biomeEerieID = 194;
    public static int biomeEldritchID = 195;
    public static int biomeTaintWeight = 2;
    public static int biomeMagicalForestWeight = 5;
    public static int taintSpreadRate = Defaults.TAINT_SPREAD_RATE;
    public static boolean taintFromFlux = true;
    public static boolean hardNode = Defaults.HARD_NODE;
    public static boolean wuss = Defaults.WUSS;
    public static int dimensionOuterId = -42;
    public static boolean championMobs = true;
    public static int shieldRecharge = 2000;
    public static int shieldWait = 80;
    public static int shieldCost = 50;
    public static boolean colorBlind = false;
    public static boolean shaders = true;
    public static boolean crooked = true;
    public static boolean showTags = false;
    public static boolean blueBiome = false;
    public static boolean allowMirrors = Defaults.ALLOW_MIRRORS;
    public static boolean dialBottom = false;
    public static int nodeRefresh = 10;
    public static boolean genAura = true;
    public static boolean genStructure = true;
    public static boolean genCinnibar = true;
    public static boolean genAmber = true;
    public static boolean genInfusedStone = true;
    public static boolean genTrees = true;
    public static boolean genTaint = true;
    public static boolean regenAura = false;
    public static boolean regenStructure = false;
    public static boolean regenCinnibar = false;
    public static boolean regenAmber = false;
    public static boolean regenInfusedStone = false;
    public static boolean regenTrees = false;
    public static boolean regenTaint = false;
    public static String regenKey = "DEFAULT";
    public static boolean wardedStone = Defaults.WARDED_STONE;
    public static boolean allowCheatSheet = Defaults.ALLOW_CHEAT_SHEET;
    public static boolean golemChestInteract = true;
    public static int nodeRarity = 36;
    public static int specialNodeRarity = 18;
    public static int notificationDelay = 5000;
    public static int notificationMax = 15;
    public static boolean glowyTaint = Defaults.GLOWY_TAINT;
    public static int researchDifficulty = Defaults.RESEARCH_DIFFICULTY;
    public static int aspectTotalCap = Defaults.ASPECT_TOTAL_CAP;
    public static int golemDelay = 5;
    public static int golemIgnoreDelay = 10000;
    public static int golemLinkQuality = 16;
    public static boolean CwardedStone = Defaults.CWARDED_STONE;
    public static boolean CallowCheatSheet = Defaults.CALLOW_CHEAT_SHEET;
    public static boolean CallowMirrors = Defaults.CALLOW_MIRRORS;
    public static boolean ChardNode = Defaults.CHARD_NODE;
    public static boolean Cwuss = Defaults.CWUSS;
    public static int CresearchDifficulty = Defaults.CRESEARCH_DIFFICULTY;
    public static int CaspectTotalCap = Defaults.CASPECT_TOTAL_CAP;
    public static boolean spawnAngryZombie = true;
    public static boolean spawnFireBat = true;
    public static boolean spawnTaintacle = true;
    public static boolean spawnWisp = true;
    public static boolean spawnTaintSpore = Defaults.SPAWN_TAINT_SPORE;
    public static boolean spawnPech = true;
    public static boolean spawnElder = true;
    public static int potionVisExhaustID = 18;
    public static int potionInfVisExhaustID = 18;
    public static int potionBlurredID = 18;
    public static int potionThaumarhiaID = 18;
    public static int potionTaintPoisonID = 19;
    public static int potionUnHungerID = 17;
    public static int potionSunScornedID = 17;
    public static int potionWarpWardID = 23;
    public static int potionDeathGazeID = 17;
    /** The original 1.7 enchantment fields; 1.21.1 has no {@code Enchantment} class. */
    public static Object enchHaste = null;
    public static Object enchRepair = null;
    public static ArrayList<Aspect> aspectOrder = new ArrayList<>();
    public static boolean foundCopperIngot = false;
    public static boolean foundTinIngot = false;
    public static boolean foundSilverIngot = false;
    public static boolean foundLeadIngot = false;
    public static boolean foundCopperOre = false;
    public static boolean foundTinOre = false;
    public static boolean foundSilverOre = false;
    public static boolean foundLeadOre = false;

    private Config() {}

    /**
     * Copies the seven research facts of the native settings into the original options.
     *
     * <p>The original writes them twice: once in the plain option and once in the prefixed copy
     * that the cheat sheet reads (oracle {@code Config.syncConfigurable:388-392} and the
     * assignments at {@code :457-459}).</p>
     */
    public static void apply(ResearchSettings settings) {
        if (settings == null) return;
        hardNode = settings.hardNodes();
        ChardNode = settings.hardNodes();
        wuss = settings.wuss();
        Cwuss = settings.wuss();
        researchDifficulty = settings.difficulty();
        CresearchDifficulty = settings.difficulty();
        aspectTotalCap = settings.aspectCap();
        CaspectTotalCap = settings.aspectCap();
        allowCheatSheet = settings.cheatSheet();
        CallowCheatSheet = settings.cheatSheet();
        allowMirrors = settings.mirrors();
        CallowMirrors = settings.mirrors();
        wardedStone = settings.wardedStone();
        CwardedStone = settings.wardedStone();
    }

    /** Copies the native taint settings into the original options. */
    public static void apply(ThaumcraftTaintSettings.Values taint) {
        if (taint == null) return;
        taintSpreadRate = taint.spreadRate();
        glowyTaint = taint.glowing();
        spawnTaintSpore = taint.spawnSpores();
    }
}
