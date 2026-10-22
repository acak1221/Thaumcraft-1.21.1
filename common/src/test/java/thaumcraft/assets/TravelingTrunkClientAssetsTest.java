package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.entity.RenderTravelingTrunk;
import thaumcraft.common.items.golems.GolemUpgrade;

/** The Traveling Trunk's client inputs (original ModelTrunk 64x64 skin, guitrunkbase, the upgrade icons) and the renderer's pure numbers. */
class TravelingTrunkClientAssetsTest {
    private int[] png(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            ByteBuffer header = ByteBuffer.wrap(stream.readNBytes(24));
            return new int[] {header.getInt(16), header.getInt(20)};
        }
    }

    @Test void theSkinsAndTheScreenKeepTheOriginalSizes() throws Exception {
        assertEquals(64, png("assets/thaumcraft/textures/models/trunk.png")[0]);
        assertEquals(64, png("assets/thaumcraft/textures/models/trunk.png")[1]);
        assertEquals(64, png("assets/thaumcraft/textures/models/trunkangry.png")[0]);
        assertEquals(64, png("assets/thaumcraft/textures/models/trunkangry.png")[1]);
        // The screen reads the bar at (176, 16), the stay mark at (176, 0) and the Earth band at (0, 206) of a 256 sheet.
        assertEquals(256, png("assets/thaumcraft/textures/gui/guitrunkbase.png")[0]);
        assertEquals(256, png("assets/thaumcraft/textures/gui/guitrunkbase.png")[1]);
    }

    @Test void everyUpgradeHasTheIconTheBadgeDraws() throws Exception {
        for (GolemUpgrade upgrade : GolemUpgrade.values()) {
            assertEquals(16, png("assets/thaumcraft/textures/items/golem_upgrade_" + upgrade.key + ".png")[0], upgrade.key);
        }
    }

    @Test void theScreenCaptionsAndMessagesAreTranslated() throws Exception {
        for (String lang : new String[] {"en_us", "ru_ru"}) {
            try (var stream = getClass().getClassLoader().getResourceAsStream("assets/thaumcraft/lang/" + lang + ".json");
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject names = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : new String[] {"entity.trunk.guiname", "entity.trunk.move", "entity.trunk.stay"}) {
                    assertTrue(names.has(key), lang + " lacks " + key);
                }
            }
        }
    }

    @Test void theRendererUsesTheOriginalScaleAndLidFormulas() {
        // Oracle RenderTravelingTrunk:38-48 - adjustTrunk with a zero squash: the sides are 1/1.4,
        // the overall scale is 2/1.5 (2/1.33 with the Earth upgrade) and the height is 0.5 / sides.
        // The divisions are done in double and narrowed to a float.
        float[] plain = RenderTravelingTrunk.trunkScale(0);
        assertEquals(.952381F, plain[0]);
        assertEquals(.93333334F, plain[1]);
        assertEquals(.952381F, plain[2]);
        float[] earth = RenderTravelingTrunk.trunkScale(GolemUpgrade.EARTH.id);
        assertEquals(1.0741138F, earth[0]);
        assertEquals(1.0526315F, earth[1]);
        assertEquals(1.0741138F, earth[2]);
        assertEquals(.952381F, RenderTravelingTrunk.trunkScale(GolemUpgrade.ENTROPY.id)[0]);
        // Shut lid (lidrot 0) is flat; the fully open value 0.5 raises it by 1 - 0.5^3 of a right angle.
        // The original multiplies with the float literal 3.141593f and negates, so a flat lid is a negative zero.
        assertEquals(-0.0F, RenderTravelingTrunk.lidAngle(0F));
        assertEquals(-1.3744469F, RenderTravelingTrunk.lidAngle(0.5F));
        assertTrue(RenderTravelingTrunk.lidAngle(0.2F) > RenderTravelingTrunk.lidAngle(0.5F), "the lid keeps rising with lidrot");
    }
}
