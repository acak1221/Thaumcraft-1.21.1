package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.entities.ModelGolemAccessories;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemItemMatch;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.utils.Utils;

/**
 * The golem menu of the original 4.2.3.5, pinned against the oracle.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/ContainerGolem.java:82-112} — the menu buttons are the page
 * arrows 66 and 67, the eight option switches 50 .. 57 and the Order colour buttons, which are the golem's own slot
 * ids: a button below the slot count lowers the colour of that slot, a button from the slot count up to twice the
 * slot count raises it. Oracle {@code thaumcraft/client/gui/GuiGolem.java:102-145,328-342} — the colour chip of a
 * ghost is the 6x6 tinted square of the texture at (0,176) above the slot, and the colour name of the hovered bar is
 * drawn above the panel when the golem carries Entropy.</p>
 *
 * <p>These assertions are authored but NOT executed: the leader has to run {@code :common:test}.</p>
 */
class GolemMenuOrderTest {
    @Test void theOrderColourCycleIsTheOriginalOne() {
        // Original ContainerGolem:97-101: the left button lowers the colour, -1 wraps to 15.
        assertEquals(15, ContainerGolem.nextColour(GolemMarker.ANY, -1), "any lowered is fifteen");
        assertEquals(0, ContainerGolem.nextColour(1, -1), "one lowered is zero");
        assertEquals(GolemMarker.ANY, ContainerGolem.nextColour(0, -1), "zero lowered is any");
        assertEquals(14, ContainerGolem.nextColour(15, -1), "fifteen lowered is fourteen");
        // Original ContainerGolem:103-108: the right button raises the colour, 15 wraps to -1.
        assertEquals(0, ContainerGolem.nextColour(GolemMarker.ANY, 1), "any raised is zero");
        assertEquals(15, ContainerGolem.nextColour(14, 1), "fourteen raised is fifteen");
        assertEquals(GolemMarker.ANY, ContainerGolem.nextColour(15, 1), "fifteen raised is any");
    }

    @Test void theMenuButtonsKeepTheOriginalIds() {
        // Original ContainerGolem:84-90: the page arrows.
        assertEquals(66, ContainerGolem.PREVIOUS_PAGE, "the previous page button");
        assertEquals(67, ContainerGolem.NEXT_PAGE, "the next page button");
        // Original ContainerGolem:92-94: the option switches 50 .. 57; 50 is the Fill amount mode, 51 .. 54 the
        // Guard target switches and 55 .. 57 the Entropy item comparison switches (GuiGolem:362-409).
        assertEquals(51, ContainerGolem.GUARD_BUTTON, "the first Guard target switch");
        assertEquals(4, GolemGuardTargets.BUTTON_COUNT, "four Guard target switches");
        assertEquals(55, ContainerGolem.ITEM_MATCH_BUTTON, "the first item comparison switch");
        assertEquals(3, GolemItemMatch.BUTTON_COUNT, "three item comparison switches");
        assertEquals(51 + 4, ContainerGolem.ITEM_MATCH_BUTTON, "the Guard switches and the item switches do not overlap");
    }

    @Test void theSixteenMarkerColoursAreTheOriginalOnes() {
        // Original thaumcraft/common/lib/utils/Utils.java:79-80: the sixteen dye colours and their values.
        assertEquals(0xF0F0F0, Utils.colors[0], "white");
        assertEquals(15435844, Utils.colors[1], "orange");
        assertEquals(12801229, Utils.colors[2], "magenta");
        assertEquals(6719955, Utils.colors[3], "light blue");
        assertEquals(14602026, Utils.colors[4], "yellow");
        assertEquals(4312372, Utils.colors[5], "lime");
        assertEquals(14188952, Utils.colors[6], "pink");
        assertEquals(0x434343, Utils.colors[7], "gray");
        assertEquals(0xA0A0A0, Utils.colors[8], "light gray");
        assertEquals(2651799, Utils.colors[9], "cyan");
        assertEquals(8073150, Utils.colors[10], "purple");
        assertEquals(2437522, Utils.colors[11], "blue");
        assertEquals(5320730, Utils.colors[12], "brown");
        assertEquals(3887386, Utils.colors[13], "green");
        assertEquals(11743532, Utils.colors[14], "red");
        assertEquals(0x1E1B1B, Utils.colors[15], "black");
        // The order of the original names is the order of the sixteen dye colours.
        assertEquals("White", Utils.colorNames[0]);
        assertEquals("Light Blue", Utils.colorNames[3]);
        assertEquals("Black", Utils.colorNames[15]);
        for (int color = 0; color < 16; color++) assertNotNull(DyeColor.byId(color), "the dye colour " + color);
    }

    @Test void everyMarkerColourHasItsName() throws IOException {
        JsonObject lang = readLang();
        assertEquals("Any color", lang.get("tc.port.golem.color.any").getAsString());
        for (int color = 0; color < 16; color++) {
            String key = "tc.port.golem.color." + color;
            assertTrue(lang.has(key), "the language file has " + key);
            assertEquals(Utils.colorNames[color], lang.get(key).getAsString(), key);
        }
    }

    @Test void theAccessoryModelKeepsTheOriginalPartsAndHeadOffset() {
        // Original ModelGolemAccessories:45-97: the fourteen worn pieces on the golem's own 128x128 sheet, the head
        // pieces at the golem's head height of thirty units.
        var root = ModelGolemAccessories.createBodyLayer(30F).bakeRoot();
        assertEquals(15, root.getAllParts().count(), "the original accessories own fourteen pieces");
        assertEquals(30F, root.getChild("head_fez").y, 1.0E-5F, "the fez sits at the original head height");
        assertEquals(30F, root.getChild("head_hat").y, 1.0E-5F, "the hat sits at the original head height");
        assertEquals(30F, root.getChild("bowtie").y, 1.0E-5F, "the bow tie sits at the original body height");
        assertEquals(30F, root.getChild("plate").y, 1.0E-5F, "the plate sits at the original body height");
    }

    private static JsonObject readLang() throws IOException {
        Path path = Path.of("src/main/resources/assets/thaumcraft/lang/en_us.json");
        if (!Files.exists(path)) path = Path.of("common/src/main/resources/assets/thaumcraft/lang/en_us.json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
