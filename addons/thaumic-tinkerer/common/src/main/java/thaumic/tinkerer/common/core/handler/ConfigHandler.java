/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The options of the 1.7.10 {@code ThaumicTinkerer.cfg}, in {@code config/thaumictinkerer.json}.
 *
 * <p>The field names are the 1.7.10 ones. Options that only existed because of numeric ids (potion ids, enchantment
 * ids, dimension ids) are gone: the potions, enchantments and the Bedrock dimension are registry entries now, and the
 * dimension is a data pack (see docs/DEVIATIONS.md). The file is written with the defaults the first time and is
 * rewritten with every known key on each load, so new options appear in it.</p>
 */
public final class ConfigHandler {
    private static final Logger LOG = LoggerFactory.getLogger("ThaumicTinkerer");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // general
    /** "Set to true to enable flight in this mod." */
    public static boolean enableFlight = true;
    /** Prefix [TT] in the Thaumonomicon text of the add-on's research. */
    public static boolean useTootlipIndicators = true;
    public static boolean enableSurvivalShareTome = true;
    public static boolean enableDebugCommands = false;
    /** Use the shared copper and tin tags in recipes when they have members. */
    public static boolean useOreDictMetal = true;
    public static boolean repairTConTools = false;
    /** Dimension ids ({@code namespace:path}) where the Bottomless Pouch inventory is refused. */
    public static String[] forbiddenDimensions = {};
    /** Block ids that the Focus of Dislocation refuses to move. */
    public static String[] blockBlacklist = {};
    public static boolean enableFire = true;
    public static boolean enableCake = true;
    public static boolean enableFireMechanics = true;
    public static boolean cropsAllowBonemeal = false;
    /** The Height of the Soul Heart bar. */
    public static int soulHeartHeight = 49;

    // kami
    public static boolean enableKami = true;
    public static boolean showPlacementMirrorBlocks = true;
    public static boolean shouldKamiLegsDoLightFeature = false;
    public static boolean shouldKamiBootsDoGrass = true;
    /** Ore ids ({@code namespace:path} or item tags) that are not spawned in the Bedrock dimension. */
    public static String[] bedrockOreBlacklist = {};
    public static int bedrockOreDensity = 1;

    // balance.shadowbeam (hundredths of vis)
    public static int visShadowbeamAir = 15;
    public static int visShadowbeamOrder = 25;
    public static int visShadowbeamEntropy = 25;
    public static int baseDamageShadowbeam = 8;

    private ConfigHandler() {}

    public static Path file() {
        return Platform.getConfigFolder().resolve("thaumictinkerer.json");
    }

    /** Reads the file (when there is one), applies it and writes it back with every key. Safe to call twice. */
    public static synchronized void loadConfig() {
        loadConfig(file());
    }

    /** Puts every option back to its 1.7.10 default. */
    public static synchronized void resetToDefaults() {
        enableFlight = true;
        useTootlipIndicators = true;
        enableSurvivalShareTome = true;
        enableDebugCommands = false;
        useOreDictMetal = true;
        repairTConTools = false;
        forbiddenDimensions = new String[0];
        blockBlacklist = new String[0];
        enableFire = true;
        enableCake = true;
        enableFireMechanics = true;
        cropsAllowBonemeal = false;
        soulHeartHeight = 49;
        enableKami = true;
        showPlacementMirrorBlocks = true;
        shouldKamiLegsDoLightFeature = false;
        shouldKamiBootsDoGrass = true;
        bedrockOreBlacklist = new String[0];
        bedrockOreDensity = 1;
        visShadowbeamAir = 15;
        visShadowbeamOrder = 25;
        visShadowbeamEntropy = 25;
        baseDamageShadowbeam = 8;
    }

    public static synchronized void loadConfig(Path path) {
        resetToDefaults();
        JsonObject json = new JsonObject();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (parsed != null && parsed.isJsonObject()) json = parsed.getAsJsonObject();
            } catch (IOException | RuntimeException e) {
                LOG.warn("Cannot read {}, using the defaults: {}", path, e.toString());
            }
        }
        JsonObject general = section(json, "general");
        enableFlight = bool(general, "modFlight.enabled", enableFlight);
        useTootlipIndicators = bool(general, "tooltipIndicators.enabled", useTootlipIndicators);
        enableSurvivalShareTome = bool(general, "shareTome.survival.enabled", enableSurvivalShareTome);
        enableDebugCommands = bool(general, "debugCommands.enabled", enableDebugCommands);
        useOreDictMetal = bool(general, "oreDictMetal.enabled", useOreDictMetal);
        repairTConTools = bool(general, "repairTconTools.enabled", repairTConTools);
        forbiddenDimensions = strings(general, "forbiddenDimensions", forbiddenDimensions);
        blockBlacklist = strings(general, "focusOfDislocationBlacklist", blockBlacklist);
        enableFire = bool(general, "imbuedFire.enabled", enableFire);
        enableFireMechanics = bool(general, "imbuedFireSpread.enabled", enableFireMechanics);
        enableCake = bool(general, "imbuedFire.cake.enabled", enableCake);
        cropsAllowBonemeal = bool(general, "cropsAllowBonemeal.enabled", cropsAllowBonemeal);
        soulHeartHeight = clamp(integer(general, "soulHeart.height", soulHeartHeight), 0, 256);

        JsonObject kami = section(json, "kami");
        enableKami = bool(kami, "kami.forceenabled", enableKami);
        showPlacementMirrorBlocks = bool(kami, "placementMirror.blocks.show", showPlacementMirrorBlocks);
        shouldKamiLegsDoLightFeature = bool(kami, "kamiLeggingsLightPlacing.enabled", shouldKamiLegsDoLightFeature);
        shouldKamiBootsDoGrass = bool(kami, "kamiBootsDirtIntoGrass.enabled", shouldKamiBootsDoGrass);
        bedrockOreBlacklist = strings(kami, "bedrockDimension.oreBlacklist", bedrockOreBlacklist);
        bedrockOreDensity = clamp(integer(kami, "bedrockDimension.oreDensity", bedrockOreDensity), 0, 1023);

        JsonObject shadowbeam = section(json, "balance.shadowbeam");
        visShadowbeamAir = integer(shadowbeam, "aerVisCost", visShadowbeamAir);
        visShadowbeamOrder = integer(shadowbeam, "ordoVisCost", visShadowbeamOrder);
        visShadowbeamEntropy = integer(shadowbeam, "perditioVisCost", visShadowbeamEntropy);
        baseDamageShadowbeam = integer(shadowbeam, "baseDamage", baseDamageShadowbeam);

        write(path);
    }

    private static void write(Path path) {
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("modFlight.enabled", enableFlight);
        general.put("tooltipIndicators.enabled", useTootlipIndicators);
        general.put("shareTome.survival.enabled", enableSurvivalShareTome);
        general.put("debugCommands.enabled", enableDebugCommands);
        general.put("oreDictMetal.enabled", useOreDictMetal);
        general.put("repairTconTools.enabled", repairTConTools);
        general.put("forbiddenDimensions", forbiddenDimensions);
        general.put("focusOfDislocationBlacklist", blockBlacklist);
        general.put("imbuedFire.enabled", enableFire);
        general.put("imbuedFireSpread.enabled", enableFireMechanics);
        general.put("imbuedFire.cake.enabled", enableCake);
        general.put("cropsAllowBonemeal.enabled", cropsAllowBonemeal);
        general.put("soulHeart.height", soulHeartHeight);
        Map<String, Object> kami = new LinkedHashMap<>();
        kami.put("kami.forceenabled", enableKami);
        kami.put("placementMirror.blocks.show", showPlacementMirrorBlocks);
        kami.put("kamiLeggingsLightPlacing.enabled", shouldKamiLegsDoLightFeature);
        kami.put("kamiBootsDirtIntoGrass.enabled", shouldKamiBootsDoGrass);
        kami.put("bedrockDimension.oreBlacklist", bedrockOreBlacklist);
        kami.put("bedrockDimension.oreDensity", bedrockOreDensity);
        Map<String, Object> shadowbeam = new LinkedHashMap<>();
        shadowbeam.put("aerVisCost", visShadowbeamAir);
        shadowbeam.put("ordoVisCost", visShadowbeamOrder);
        shadowbeam.put("perditioVisCost", visShadowbeamEntropy);
        shadowbeam.put("baseDamage", baseDamageShadowbeam);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("general", general);
        root.put("kami", kami);
        root.put("balance.shadowbeam", shadowbeam);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            LOG.warn("Cannot write {}: {}", path, e.toString());
        }
    }

    private static JsonObject section(JsonObject root, String name) {
        JsonElement element = root.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        try {
            return o.has(key) ? o.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int integer(JsonObject o, String key, int fallback) {
        try {
            return o.has(key) ? o.get(key).getAsInt() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String[] strings(JsonObject o, String key, String[] fallback) {
        try {
            if (!o.has(key)) return fallback;
            var array = o.getAsJsonArray(key);
            String[] out = new String[array.size()];
            for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsString();
            return out;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
