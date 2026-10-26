package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The 1.7.10 ore dictionary names an add-on passes to the recipe and object tag API. */
class OreDictionaryNamesTest {
    private static String id(String name) {
        var id = OreDictionaryNames.tagId(name);
        return id == null ? null : id.toString();
    }

    @Test void namesThaumcraftItselfUsedKeepTheirOriginalTags() {
        assertEquals("thaumcraft:original_ore/nugget_thaumium", id("nuggetThaumium"));
        assertEquals("thaumcraft:original_ore/ingot_iron", id("ingotIron"));
        assertEquals("thaumcraft:original_log_wood", id("logWood"));
        assertEquals("thaumcraft:original_ore/shard_air", id("shardAir"));
        // A name Thaumcraft 4 itself used wins over the convention: its tag holds the Thaumcraft members too.
        assertEquals("thaumcraft:original_ore/ingot_silver", id("ingotSilver"));
    }

    @Test void theForgeConventionBecomesTheConventionalTag() {
        assertEquals("c:ingots/steel", id("ingotSteel"));
        assertEquals("c:ores/lapis", id("oreLapis"));
        assertEquals("c:storage_blocks/copper", id("blockCopper"));
        assertEquals("c:gems/nether_quartz", id("gemNetherQuartz"));
        assertEquals("c:dusts/redstone", id("dustRedstone"));
        assertEquals("c:dyes/light_blue", id("dyeLightBlue"));
        assertEquals("c:nuggets/brass", id("nuggetBrass"));
    }

    @Test void vanillaNamesAndNamespacedIds() {
        assertEquals("c:glass_panes", id("paneGlass"));
        assertEquals("c:slime_balls", id("slimeball"));
        assertEquals("minecraft:wool", id("blockWool"));
        assertEquals("c:ingots/copper", id("c:ingots/copper"));
        assertEquals("minecraft:logs", id("minecraft:logs"));
    }

    @Test void anythingElseIsNoTag() {
        assertNull(id("listAllmeatraw"));
        assertNull(id("ingot"));
        assertNull(id("ingotlower"));
        assertNull(id(""));
        assertNull(id(null));
    }
}
