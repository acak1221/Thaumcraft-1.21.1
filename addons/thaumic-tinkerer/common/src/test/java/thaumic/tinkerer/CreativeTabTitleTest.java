/*
 * This file is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (no 1.7.10
 * counterpart); it is distributed under the same licence as the add-on (Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The title of the add-on's creative tab. The code asks the game for {@link LibMisc#CREATIVE_TAB_KEY}
 * ({@code itemGroup.thaumictinkerer}); the converted 1.7.10 language files only carry the old spelling
 * {@code itemGroup.ThaumicTinkerer}, so the key has to come from {@code lang-extra/core.<lang>.json}. Without it the
 * client shows the raw key as the tab title (seen in the client smoke run of 2026-10-30).
 *
 * <p>Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 */
class CreativeTabTitleTest {
    private static final String[] LANGUAGES = {"en_us", "ru_ru", "de_de", "fr_fr", "ko_kr", "zh_cn"};

    private static JsonObject lang(String language) throws IOException {
        String resource = "assets/" + LibMisc.MOD_ID + "/lang/" + language + ".json";
        try (InputStream in = CreativeTabTitleTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return parsed.getAsJsonObject();
        }
    }

    @Test
    void theKeyTheCodeAsksForIsTheLowerCaseModIdOne() {
        assertEquals("itemGroup.thaumictinkerer", LibMisc.CREATIVE_TAB_KEY);
    }

    @Test
    void everyLanguageFileTranslatesTheCreativeTabTitle() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String language : LANGUAGES) {
            JsonObject texts = lang(language);
            if (!texts.has(LibMisc.CREATIVE_TAB_KEY) || texts.get(LibMisc.CREATIVE_TAB_KEY).getAsString().isBlank()) {
                missing.add(language);
            }
        }
        assertEquals(List.of(), missing, "no text for " + LibMisc.CREATIVE_TAB_KEY + " in");
    }

    @Test
    void theTitleOfTheEnglishTabIsTheNameOfTheMod() throws IOException {
        JsonObject en = lang("en_us");
        assertTrue(en.has(LibMisc.CREATIVE_TAB_KEY));
        String title = en.get(LibMisc.CREATIVE_TAB_KEY).getAsString();
        assertEquals("Thaumic Tinkerer", title);
        assertFalse(title.startsWith("itemGroup."), "a raw key is not a title");
    }
}
