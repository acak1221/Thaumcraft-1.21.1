package thaumcraft.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.entities.ai.combat.TaintCreeperSwell;
import thaumcraft.common.lib.golems.GolemAiRules;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintDeathRules;
import thaumcraft.common.lib.taint.TaintDeathRules.TaintedTwin;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.lib.utils.OriginalOreTags;

/**
 * The cross subsystem glue of the port: the taint death conversion, the ore dictionary replacement, the golem
 * cargo, the champion state and the public research surface.
 *
 * <p>Every assertion here is about the ORIGINAL values of TC4 4.2.3.5, taken from the oracle:
 * {@code EventHandlerEntity.java:480-507} (the taint death conversion),
 * {@code ConfigItems.java:260-301} and {@code ConfigRecipes.java:131-495} (the ore names) and
 * {@code EntityUtils.java:59-65} (the champion modifier table).</p>
 */
class CrossSubsystemRulesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ------------------------------------------------------------------ the taint death conversion

    @Test void theOriginalConversionTableIsKept() {
        assertEquals(TaintedTwin.CHICKEN, TaintDeathRules.twin("minecraft:chicken"));
        assertEquals(TaintedTwin.COW, TaintDeathRules.twin("minecraft:cow"));
        assertEquals(TaintedTwin.PIG, TaintDeathRules.twin("minecraft:pig"));
        assertEquals(TaintedTwin.SHEEP, TaintDeathRules.twin("minecraft:sheep"));
        assertEquals(TaintedTwin.VILLAGER, TaintDeathRules.twin("minecraft:villager"));
        assertEquals(TaintedTwin.CREEPER, TaintDeathRules.twin("minecraft:creeper"));
        // Original EventHandlerEntity:497: every other mob became a thaumic slime.
        assertEquals(TaintedTwin.SLIME, TaintDeathRules.twin("minecraft:zombie"));
        assertEquals(TaintedTwin.SLIME, TaintDeathRules.twin("minecraft:skeleton"));
        assertEquals(TaintedTwin.SLIME, TaintDeathRules.twin("minecraft:player"));
        assertEquals(TaintedTwin.SLIME, TaintDeathRules.twin(null));
    }

    @Test void theTaintedAnimalLayerOwnsTheFiveFarmAnimals() {
        assertTrue(TaintDeathRules.animalTwin("minecraft:chicken"));
        assertTrue(TaintDeathRules.animalTwin("minecraft:cow"));
        assertTrue(TaintDeathRules.animalTwin("minecraft:pig"));
        assertTrue(TaintDeathRules.animalTwin("minecraft:sheep"));
        assertTrue(TaintDeathRules.animalTwin("minecraft:villager"));
        assertFalse(TaintDeathRules.animalTwin("minecraft:creeper"));
        assertFalse(TaintDeathRules.animalTwin("minecraft:zombie"));
    }

    @Test void onlyATaintedMobOnTheServerConverts() {
        assertTrue(TaintDeathRules.converts(true, true, false));
        assertFalse(TaintDeathRules.converts(false, true, false));
        assertFalse(TaintDeathRules.converts(true, false, false));
        assertFalse(TaintDeathRules.converts(true, true, true));
    }

    @Test void theSlimeSizeComesFromTheMaxHealth() {
        // Original EventHandlerEntity:500: (int)(1.0f + Math.min(maxHealth / 10.0f, 6.0f)).
        assertEquals(3, TaintDeathRules.slimeSize(20F));
        assertEquals(1, TaintDeathRules.slimeSize(0F));
        assertEquals(3, TaintDeathRules.slimeSize(25F));
        assertEquals(7, TaintDeathRules.slimeSize(60F));
        assertEquals(7, TaintDeathRules.slimeSize(600F));
    }

    @Test void theTaintSeedGrowsOnTheTaintBlocksAroundIt() {
        // The TC6 flux saturation has no TC4 counterpart: the native growth supply is the taint block.
        assertTrue(TaintAnimalRules.seedGrowthSupply(true, 0));
        assertTrue(TaintAnimalRules.seedGrowthSupply(false, 1));
        assertFalse(TaintAnimalRules.seedGrowthSupply(false, 0));
    }

    @Test void theTaintCreeperSwellKeepsTheOriginalRanges() {
        // Original AICreeperSwell: the swell starts below nine blocks and is given up beyond forty nine.
        assertEquals(9.0D, TaintCreeperSwell.SWELL_RANGE, 1E-9D);
        assertEquals(49.0D, TaintCreeperSwell.SWELL_CANCEL_RANGE, 1E-9D);
    }

    // ------------------------------------------------------------------ the ore dictionary replacement

    @Test void everyOriginalOreNameHasItsOwnNativeTag() {
        Map<String, TagKey<Item>> tags = OriginalOreTags.oreTags();
        // Original ConfigItems.java:260-301.
        for (String name : List.of("oreCinnabar", "oreInfusedAir", "oreInfusedFire", "oreInfusedWater", "oreInfusedEarth",
                "oreInfusedOrder", "oreInfusedEntropy", "shardAir", "shardFire", "shardWater", "shardEarth", "shardOrder",
                "shardEntropy", "oreAmber", "quicksilver", "gemAmber", "nuggetIron", "nuggetCopper", "nuggetTin",
                "nuggetSilver", "nuggetLead", "nuggetThaumium", "nuggetVoid", "nuggetQuicksilver", "nuggetGold",
                "ingotThaumium", "ingotVoid", "clusterIron", "clusterCopper", "clusterTin", "clusterSilver",
                "clusterLead", "clusterCinnabar", "clusterGold", "logWood", "plankWood", "slabWood", "treeSapling")) {
            assertNotNull(tags.get(name), "The original ore name " + name + " needs a native tag");
            assertEquals("thaumcraft", tags.get(name).location().getNamespace());
        }
        // The vanilla Forge names the original recipes used (ConfigRecipes.java:131-495).
        for (String name : List.of("stickWood", "ingotIron", "ingotGold", "ingotCopper", "ingotTin", "ingotSilver",
                "ingotLead", "dustIron", "oreIron", "oreGold")) {
            assertNotNull(tags.get(name), "The original ore name " + name + " needs a native tag");
        }
        // The log name is the tag the recipe layer already owns.
        assertEquals("thaumcraft:original_log_wood", tags.get("logWood").location().toString());
        assertEquals("thaumcraft:original_ore/ingot_thaumium", tags.get("ingotThaumium").location().toString());
        assertNull(OriginalOreTags.oreTag("oreDictionaryNameThatNeverExisted"));
    }

    @Test void theOriginalOreTagsCarryTheOriginalMembers() throws Exception {
        JsonObject logWood = tag("data/thaumcraft/tags/item/original_log_wood.json");
        assertTrue(logWood.getAsJsonArray("values").toString().contains("minecraft:oak_log"));
        assertTrue(logWood.getAsJsonArray("values").toString().contains("thaumcraft:greatwood_log"));
        assertTrue(logWood.getAsJsonArray("values").toString().contains("thaumcraft:silverwood_log"));

        JsonObject ingotThaumium = tag("data/thaumcraft/tags/item/original_ore/ingot_thaumium.json");
        assertEquals("thaumcraft:thaumium_ingot", ingotThaumium.getAsJsonArray("values").get(0).getAsString());

        JsonObject shardAir = tag("data/thaumcraft/tags/item/original_ore/shard_air.json");
        assertEquals("thaumcraft:air_shard", shardAir.getAsJsonArray("values").get(0).getAsString());

        JsonObject oreCinnabar = tag("data/thaumcraft/tags/item/original_ore/ore_cinnabar.json");
        assertEquals("thaumcraft:cinnabar_ore", oreCinnabar.getAsJsonArray("values").get(0).getAsString());

        JsonObject plankWood = tag("data/thaumcraft/tags/item/original_ore/plank_wood.json");
        assertTrue(plankWood.getAsJsonArray("values").toString().contains("thaumcraft:greatwood_planks"));
        assertTrue(plankWood.getAsJsonArray("values").toString().contains("thaumcraft:silverwood_planks"));
    }

    @Test void theOreNamesArePublishedToTheInventoryHelper() {
        OriginalOreTags.register();
        Map<String, TagKey<Item>> published = InventoryUtils.registeredOreTags();
        assertEquals(OriginalOreTags.oreTags().size(), published.size());
        for (Map.Entry<String, TagKey<Item>> entry : OriginalOreTags.oreTags().entrySet()) {
            assertSame(entry.getValue(), published.get(entry.getKey()), "The original ore name " + entry.getKey());
        }
    }

    // ------------------------------------------------------------------------ the golem cargo

    @Test void theCargoRoomIsTheOriginalRoom() {
        // Original EntityGolemBase.getCarrySpace.
        assertEquals(10, GolemAiRules.carrySpace(10, null));
        assertEquals(10, GolemAiRules.carrySpace(10, net.minecraft.world.item.ItemStack.EMPTY));
        assertEquals(4, GolemAiRules.carrySpace(10, stack(net.minecraft.world.item.Items.IRON_INGOT, 6)));
        assertEquals(0, GolemAiRules.carrySpace(10, stack(net.minecraft.world.item.Items.IRON_INGOT, 10)));
    }

    // --------------------------------------------------------------------- the champion modifier table

    @Test void theChampionModifierTableIsTheOriginalTable() {
        var mods = EntityUtils.championMods();
        assertEquals(13, mods.size());
        assertEquals("Bold", mods.get(0).name());
        assertEquals("Mighty", mods.get(3).name());
        assertEquals("Warded", mods.get(5).name());
        assertEquals("Infested", mods.get(12).name());
    }

    // -------------------------------------------------------------------- the public research surface

    @Test void thePublicRegistryExposesTheTwoHundredAndOneOriginalEntries() {
        ResearchCategories.loadNative();
        assertEquals(6, ResearchCategories.researchCategories.size());
        int entries = 0;
        for (var list : ResearchCategories.researchCategories.values()) entries += list.research.size();
        assertEquals(201, entries);
        assertEquals(19, ResearchCategories.getResearchList("BASICS").research.size());
        assertEquals(35, ResearchCategories.getResearchList("ALCHEMY").research.size());
        assertEquals(39, ResearchCategories.getResearchList("GOLEMANCY").research.size());
        assertEquals(50, ResearchCategories.getResearchList("ARTIFICE").research.size());
        assertEquals(42, ResearchCategories.getResearchList("THAUMATURGY").research.size());
        assertEquals(16, ResearchCategories.getResearchList("ELDRITCH").research.size());
    }

    @Test void thePublicRegistryCarriesTheOriginalKeysAndPages() {
        ResearchItem focus = ResearchCategories.getResearch("FOCUSFIRE");
        assertNotNull(focus);
        assertEquals("THAUMATURGY", focus.category);
        assertEquals(3, focus.definition().aspectCost().get("ignis"));
        // The public view carries the aspect cost of the original research entry.
        assertEquals(3, focus.tags.getAmount(thaumcraft.api.aspects.Aspect.getAspect("ignis")));
        assertEquals(3, focus.definition().pages().size());
        assertEquals(3, focus.getPages().length);
        assertEquals("BASICS", ResearchCategories.getResearch("ASPECTS").category);
        assertNull(ResearchCategories.getResearch("THERE_IS_NO_SUCH_RESEARCH"));
    }

    @Test void theOriginalCategoryNamesAreTheSixOriginalOnes() {
        ResearchCategories.loadNative();
        assertEquals(Set.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"),
                ResearchCategories.researchCategories.keySet());
    }

    // ------------------------------------------------------------------------------- the helpers

    private static JsonObject tag(String resource) throws Exception {
        try (InputStream stream = CrossSubsystemRulesTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "The native item tag " + resource + " must exist");
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                assertFalse(object.get("replace").getAsBoolean());
                JsonArray values = object.getAsJsonArray("values");
                assertTrue(values.size() > 0, "The native item tag " + resource + " must not be empty");
                return object;
            }
        }
    }

    private static net.minecraft.world.item.ItemStack stack(net.minecraft.world.item.Item item, int count) {
        var stack = new net.minecraft.world.item.ItemStack(item, count);
        return stack;
    }
}
