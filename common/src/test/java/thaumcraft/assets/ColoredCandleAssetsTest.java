package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The sixteen tallow candle colours (the original's {@code blockCandle} metadata 0..15, which follows the vanilla dye order). The
 * resource files are checked directly; the registering class is never loaded.
 */
class ColoredCandleAssetsTest {
    private static final String ASSETS = "assets/thaumcraft/";
    /** Vanilla dye order, which is the order of the original candle metadata. */
    private static final List<String> DYES = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black");

    private static String candle(int meta) { return meta == 0 ? "tallow_candle" : DYES.get(meta) + "_tallow_candle"; }

    @Test void everyColourHasItsBlockstateModelsAndItemModel() throws Exception {
        List<String> missing = new ArrayList<>();
        for (int meta = 0; meta < 16; meta++) {
            String id = candle(meta);
            for (String path : new String[] {ASSETS + "blockstates/" + id + ".json", ASSETS + "models/block/" + id + ".json", ASSETS + "models/item/" + id + ".json"}) {
                if (!ResourceTree.exists(path)) missing.add(path);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void eachColourHasAShapelessDyeRecipeFromThePlainCandleExceptWhite() throws Exception {
        List<String> problems = new ArrayList<>();
        for (int meta = 1; meta < 16; meta++) {
            String id = candle(meta);
            JsonObject recipe = ResourceTree.object("data/thaumcraft/recipe/" + id + ".json");
            if (!recipe.get("type").getAsString().equals("minecraft:crafting_shapeless")) problems.add(id + " is not shapeless");
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            List<String> items = new ArrayList<>();
            for (JsonElement ingredient : ingredients) {
                JsonObject object = ingredient.getAsJsonObject();
                items.add(object.has("item") ? object.get("item").getAsString() : "#" + object.get("tag").getAsString());
            }
            // Black, blue and brown dyes accept the other dye items of that colour (ink sac, lapis, cocoa) through a mod tag.
            String dye = List.of("black", "blue", "brown").contains(DYES.get(meta)) ? "#thaumcraft:" + DYES.get(meta) + "_dyes" : "minecraft:" + DYES.get(meta) + "_dye";
            if (!items.equals(List.of("thaumcraft:tallow_candle", dye))) problems.add(id + " uses " + items);
            if (!recipe.getAsJsonObject("result").get("id").getAsString().equals("thaumcraft:" + id)) problems.add(id + " makes " + recipe.getAsJsonObject("result"));
        }
        assertEquals(List.of(), problems);
    }

    @Test void namesFollowTheOriginalMetadataOrderInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(ASSETS + "lang/en_us.json");
        JsonObject ru = ResourceTree.object(ASSETS + "lang/ru_ru.json");
        List<String> problems = new ArrayList<>();
        for (int meta = 0; meta < 16; meta++) {
            String legacy = "tile.blockCandle." + meta + ".name";
            String modern = "block.thaumcraft." + candle(meta);
            for (JsonObject lang : new JsonObject[] {en, ru}) {
                if (!lang.has(legacy) || !lang.has(modern)) problems.add("missing " + legacy + " or " + modern);
            }
            if (en.has(legacy) && en.has(modern) && !en.get(legacy).getAsString().equals(en.get(modern).getAsString())) problems.add("en_us differs for " + meta);
            // The Russian block names say "сальная свеча" (tallow candle) where the legacy ones say "свеча"; the colour wording may differ
            // (vanilla calls magenta "пурпурный" and light blue "голубой").
            if (ru.has(legacy) && !ru.get(legacy).getAsString().endsWith("свеча")) problems.add("ru_ru legacy name of " + meta);
            if (ru.has(modern) && !ru.get(modern).getAsString().endsWith("сальная свеча")) problems.add("ru_ru block name of " + meta);
        }
        assertEquals(List.of(), problems);
    }

    @Test void theEnglishNameIsTheColourPlusTallowCandle() throws Exception {
        JsonObject en = ResourceTree.object(ASSETS + "lang/en_us.json");
        List<String> wrong = new ArrayList<>();
        for (int meta = 0; meta < 16; meta++) {
            StringBuilder expected = new StringBuilder();
            for (String word : DYES.get(meta).split("_")) expected.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            expected.append("Tallow Candle");
            if (!en.get("block.thaumcraft." + candle(meta)).getAsString().equals(expected.toString())) wrong.add(candle(meta));
        }
        assertEquals(List.of(), wrong);
    }

    @Test void theCandleTagListsExactlyTheSixteenColoursInMetadataOrder() throws Exception {
        JsonArray values = ResourceTree.object("data/thaumcraft/tags/item/tallow_candles.json").getAsJsonArray("values");
        List<String> expected = new ArrayList<>();
        for (int meta = 0; meta < 16; meta++) expected.add("thaumcraft:" + candle(meta));
        List<String> actual = new ArrayList<>();
        for (JsonElement value : values) actual.add(value.getAsString());
        assertEquals(expected, actual);
    }

    @Test void onlyThePlainCandleHasALootTableTheColouredOnesDropThroughCode() throws Exception {
        assertTrue(ResourceTree.exists("data/thaumcraft/loot_table/blocks/tallow_candle.json"));
        List<String> unexpected = new ArrayList<>();
        for (int meta = 1; meta < 16; meta++) if (ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + candle(meta) + ".json")) unexpected.add(candle(meta));
        assertEquals(List.of(), unexpected, "a coloured candle loot table would drop in addition to the code path: decide on one");
    }
}
