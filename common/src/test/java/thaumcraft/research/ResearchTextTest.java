package thaumcraft.research;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import thaumcraft.client.gui.ResearchText;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTextTest {
    @Test void preservesParagraphsFormattingRulesAndImageCoordinates() {
        var result = ResearchText.parse("A\r\nB<BR>§lC<LINE><IMG>thaumcraft:textures/misc/research2.png:0:88:255:158:.5</IMG>End");
        assertEquals(new ResearchText.Text("A B\n\n§lC"), result.get(0));
        assertInstanceOf(ResearchText.Rule.class, result.get(1));
        assertEquals(new ResearchText.Image("thaumcraft:textures/misc/research2.png", 0, 88, 255, 158, .5F), result.get(2));
        assertEquals(new ResearchText.Text("End"), result.get(3));
    }
    @Test void parsesEveryBundledEnglishAndRussianResearchIllustration() throws Exception {
        for (String locale : new String[]{"en_us", "ru_ru"}) {
            try (var stream = getClass().getClassLoader().getResourceAsStream("assets/thaumcraft/lang/" + locale + ".json")) {
                var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                int images = 0;
                for (var row : root.entrySet()) {
                    if (!row.getKey().startsWith("tc.research_page.")) continue;
                    for (var part : ResearchText.parse(row.getValue().getAsString())) {
                        if (part instanceof ResearchText.Image image) {
                            images++;
                            assertNotNull(getClass().getClassLoader().getResource("assets/" + image.texture().replace(':', '/')), image.texture());
                        }
                    }
                }
                assertEquals(29, images, locale);
            }
        }
    }
    @Test void rejectsMalformedIllustrationsRatherThanSilentlyDiscardingThem() {
        assertThrows(IllegalArgumentException.class, () -> ResearchText.parse("<IMG>missing</IMG>"));
        assertThrows(IllegalArgumentException.class, () -> ResearchText.parse("<IMG>thaumcraft:x:0:0:0:20:1</IMG>"));
        assertThrows(IllegalArgumentException.class, () -> ResearchText.parse("<IMG>thaumcraft:x:0:0:20:20:NaN</IMG>"));
    }
}
