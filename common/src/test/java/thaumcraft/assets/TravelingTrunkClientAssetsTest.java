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
        // adjustTrunk with a zero squash: 1/1.4 on the sides, 2/1.5 overall (2/1.33 with Earth), height 0.5 / sides.
        float[] plain = RenderTravelingTrunk.trunkScale(0);
        assertEquals(0.95238F, plain[0], 1e-4F);
        assertEquals(0.93333F, plain[1], 1e-4F);
        float[] earth = RenderTravelingTrunk.trunkScale(GolemUpgrade.EARTH.id);
        assertEquals(1.07411F, earth[0], 1e-4F);
        assertEquals(1.05263F, earth[1], 1e-4F);
        assertEquals(plain[0], RenderTravelingTrunk.trunkScale(GolemUpgrade.ENTROPY.id)[0], 0F);
        // Shut lid (lidrot 0) is flat; the fully open value 0.5 raises it by 1 - 0.5^3 of a right angle.
        assertEquals(0F, RenderTravelingTrunk.lidAngle(0F), 1e-6F);
        assertEquals(-(1F - 0.125F) * (float) Math.PI / 2F, RenderTravelingTrunk.lidAngle(0.5F), 1e-6F);
        assertTrue(RenderTravelingTrunk.lidAngle(0.2F) > RenderTravelingTrunk.lidAngle(0.5F), "the lid keeps rising with lidrot");
    }
}
