package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.lib.SpaRules;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.warp.SanitySoapRules;

/**
 * The small items of TC4 4.2.3.5 against the 1.21.1 port.
 *
 * <p>Every number below is the value of the decompiled original; the file and the line of the
 * 1.7.10 oracle are named in the comment of the check. Neither a block nor an item is constructed
 * here: a constructed one leaves an unregistered intrusive holder behind and breaks the registry
 * freeze of the whole test suite, so every rule is a pure function or a registered id.</p>
 */
class BaseContentItemParityTest {

    // ------------------------------------------------------------- the bath salts

    /**
     * Oracle ItemBathSalts:44-46 - the salts live for two hundred ticks in the world.
     */
    @Test void theBathSaltsLiveForTwoHundredTicks() {
        assertEquals(200, SpaRules.BATH_SALT_LIFETIME, "ItemBathSalts:45 - the lifespan of the salts");
    }

    // ------------------------------------------------------------- the buckets

    /**
     * Oracle ItemBucketDeath:39-43 - the bucket of death holds one, and its cell is the cell of
     * the original metadata three.
     */
    @Test void theBucketOfDeathHoldsOneAndFillsTheOriginalMetadataThree() {
        assertEquals(1, EquipmentRules.BUCKET_DEATH_MAX_STACK_SIZE, "ItemBucketDeath:42 - setMaxStackSize(1)");
        assertEquals(3, thaumcraft.common.blocks.BlockFluidDeath.BUCKET_CELL_METADATA, "ItemBucketDeath:103");
    }

    /**
     * Oracle ItemBucketDeath:93-105 - the bucket pours into a spot that is empty or that is made
     * of a material that is not solid, and a spot that is neither solid nor a liquid is emptied
     * first.
     */
    @Test void theBucketOfDeathPoursIntoTheOriginalSpots() {
        // An empty spot takes the fluid, a solid one does not.
        assertTrue(EquipmentRules.bucketDeathCanPour(true, false, false));
        assertTrue(EquipmentRules.bucketDeathCanPour(false, false, false), "a spot that is not solid is emptied first");
        assertFalse(EquipmentRules.bucketDeathCanPour(false, true, false), "a solid spot refuses the fluid");
        // Only the spot that is neither solid nor a liquid is emptied.
        assertTrue(EquipmentRules.bucketDeathClearsSpot(false, false, false));
        assertFalse(EquipmentRules.bucketDeathClearsSpot(true, false, false), "an empty spot needs no clearing");
        assertFalse(EquipmentRules.bucketDeathClearsSpot(false, false, true), "a liquid is not emptied");
    }

    // ------------------------------------------------------------- the compass stone

    /**
     * Oracle ItemCompassStone:45-61 - the sinister node entries expire after ten seconds, a node
     * is looked for within a range of two hundred and fifty six and it has to be in the field of
     * view of two thirds of a radian.
     */
    @Test void theCompassStoneKeepsTheOriginalExpiryRangeAndFieldOfView() {
        assertEquals(10000L, EquipmentRules.COMPASS_STONE_EXPIRY_MILLIS, "ItemCompassStone:50 - the ten seconds");
        assertEquals(256.0F, EquipmentRules.COMPASS_STONE_RANGE, "ItemCompassStone:53 - the range");
        assertEquals(0.66F, EquipmentRules.COMPASS_STONE_FOV, "ItemCompassStone:53 - the field of view");
        assertFalse(EquipmentRules.compassStoneExpired(1000L, 1000L), "a fresh entry is alive");
        assertTrue(EquipmentRules.compassStoneExpired(1000L, 11001L), "an entry older than ten seconds is dropped");
        assertTrue(EquipmentRules.compassStoneVisible(100.0D, 256.0F, 1.0D), "a node in front of the holder");
        assertFalse(EquipmentRules.compassStoneVisible(300.0D, 256.0F, 1.0D), "a node out of the range");
    }

    // ------------------------------------------------------------- the keys

    /**
     * Oracle ItemKey:99-128 - the access entry of a key is the kind of the key followed by the
     * name of the player, the gold key carries the access of the owner and only the iron key of
     * the owner opens a door that he does not own.
     */
    @Test void theKeysKeepTheOriginalAccessRule() {
        assertEquals(0, EquipmentRules.KEY_IRON, "ItemKey:99 - the iron key is the metadata zero");
        assertEquals(1, EquipmentRules.KEY_GOLD, "ItemKey:57 - the gold key is the metadata one");
        assertEquals(1, EquipmentRules.KEY_GRANT_ACCESS, "ItemKey:99 - the gold key grants the access of the owner");
        assertEquals("0Steve", EquipmentRules.keyAccessEntry(EquipmentRules.KEY_IRON, "Steve"));
        assertEquals("1Steve", EquipmentRules.keyAccessEntry(EquipmentRules.KEY_GOLD, "Steve"));
        assertEquals("10,64,-12", EquipmentRules.keyLocation(10, 64, -12), "ItemKey:95 - the location of the door");
        // The owner always may; a guest with the iron key of the owner may; the gold key may not.
        assertTrue(EquipmentRules.keyMayLink(true, false, EquipmentRules.KEY_GOLD));
        assertTrue(EquipmentRules.keyMayLink(false, true, EquipmentRules.KEY_IRON));
        assertFalse(EquipmentRules.keyMayLink(false, true, EquipmentRules.KEY_GOLD));
        assertFalse(EquipmentRules.keyMayLink(false, false, EquipmentRules.KEY_IRON));
        // The access list of the door.
        assertTrue(EquipmentRules.keyHasAccess(List.of("0Steve"), EquipmentRules.KEY_IRON, "Steve"));
        assertFalse(EquipmentRules.keyHasAccess(List.of("0Steve"), EquipmentRules.KEY_GOLD, "Steve"));
        assertTrue(EquipmentRules.keyHasGrantAccess(List.of("1Steve"), "Steve"));
        assertFalse(EquipmentRules.keyHasGrantAccess(List.of("0Steve"), "Steve"));
    }

    // ------------------------------------------------------------- the mana bean

    /**
     * Oracle ItemManaBean:68 - the random potion of a fresh bean lasts 160 ticks plus a random
     * of eighty, and ItemManaBean:71 - a quarter of the beans fill the aspect pool.
     */
    @Test void theManaBeanKeepsTheOriginalPotionDuration() {
        assertEquals(160, ItemManaBean.EFFECT_DURATION, "ItemManaBean:68 - the 160 ticks");
        assertEquals(80, ItemManaBean.EFFECT_DURATION_RANDOM, "ItemManaBean:68 - the random of eighty ticks");
        assertEquals(0.25F, ItemManaBean.ASPECT_POOL_CHANCE, "ItemManaBean:71 - the quarter of the beans");
    }

    // ------------------------------------------------------------- the nuggets

    /**
     * Oracle ItemNugget:39-52 - the nuggets of iron, copper, tin, silver, lead, quicksilver,
     * thaumium and void metal and the clusters of iron, copper, tin, silver, lead, cinnabar and
     * gold.
     */
    @Test void theNuggetsAndTheClustersKeepTheOriginalMetadata() {
        assertEquals(0, EquipmentRules.NUGGET_IRON);
        assertEquals(1, EquipmentRules.NUGGET_COPPER);
        assertEquals(2, EquipmentRules.NUGGET_TIN);
        assertEquals(3, EquipmentRules.NUGGET_SILVER);
        assertEquals(4, EquipmentRules.NUGGET_LEAD);
        assertEquals(5, EquipmentRules.NUGGET_QUICKSILVER);
        assertEquals(6, EquipmentRules.NUGGET_THAUMIUM);
        assertEquals(7, EquipmentRules.NUGGET_VOID);
        assertEquals(16, EquipmentRules.NUGGET_CLUSTER_IRON);
        assertEquals(17, EquipmentRules.NUGGET_CLUSTER_COPPER);
        assertEquals(18, EquipmentRules.NUGGET_CLUSTER_TIN);
        assertEquals(19, EquipmentRules.NUGGET_CLUSTER_SILVER);
        assertEquals(20, EquipmentRules.NUGGET_CLUSTER_LEAD);
        assertEquals(21, EquipmentRules.NUGGET_CLUSTER_CINNABAR);
        assertEquals(31, EquipmentRules.NUGGET_CLUSTER_GOLD);
        for (int cluster : new int[] {16, 17, 18, 19, 20, 21, 31}) {
            assertTrue(EquipmentRules.nuggetIsCluster(cluster), "the cluster " + cluster);
        }
        for (int nugget = 0; nugget <= 7; nugget++) {
            assertFalse(EquipmentRules.nuggetIsCluster(nugget), "the nugget " + nugget);
        }
    }

    /**
     * The native ids of the nuggets and the clusters that exist in 1.21.1. The tin, the silver
     * and the lead nuggets and their clusters need the ores of the other mods and are the
     * documented catalogue gap of the port.
     */
    @Test void theNativeNuggetAndClusterItemsAreRegistered() {
        assertEquals("thaumcraft:quicksilver_drop", ThaumcraftContent.MATERIALS.get("quicksilver_drop").getId().toString());
        assertEquals("thaumcraft:copper_nugget", ThaumcraftContent.MATERIALS.get("copper_nugget").getId().toString());
        assertEquals("thaumcraft:cluster_iron", ThaumcraftContent.MATERIALS.get("cluster_iron").getId().toString());
        assertEquals("thaumcraft:cluster_copper", ThaumcraftContent.MATERIALS.get("cluster_copper").getId().toString());
        assertEquals("thaumcraft:cluster_cinnabar", ThaumcraftContent.MATERIALS.get("cluster_cinnabar").getId().toString());
        assertEquals("thaumcraft:cluster_gold", ThaumcraftContent.MATERIALS.get("cluster_gold").getId().toString());
    }

    // ------------------------------------------------------------- the edible nuggets

    /**
     * Oracle ItemNuggetEdible:28-37 - the edible nuggets restore one point of hunger and a third
     * of a point of saturation and are eaten in ten ticks.
     */
    @Test void theEdibleNuggetsKeepTheOriginalFoodValues() {
        assertEquals(1, EquipmentRules.NUGGET_EDIBLE_HUNGER, "ItemNuggetEdible:29 - the one point of hunger");
        assertEquals(0.3F, EquipmentRules.NUGGET_EDIBLE_SATURATION, "ItemNuggetEdible:29 - the third of a point of saturation");
        assertEquals(10, EquipmentRules.NUGGET_EDIBLE_USE_DURATION, "ItemNuggetEdible:30 - the ten ticks of the use");
        assertEquals(0, EquipmentRules.NUGGET_EDIBLE_CHICKEN);
        assertEquals(1, EquipmentRules.NUGGET_EDIBLE_BEEF);
        assertEquals(2, EquipmentRules.NUGGET_EDIBLE_PORK);
        assertEquals(3, EquipmentRules.NUGGET_EDIBLE_FISH);
    }

    // ------------------------------------------------------------- the resources

    /**
     * Oracle ItemResource:54-86 - the nineteen resources from the alumentum to the gold coin.
     */
    @Test void theNineteenResourcesKeepTheOriginalMetadata() {
        assertEquals(0, EquipmentRules.RESOURCE_ALUMENTUM);
        assertEquals(1, EquipmentRules.RESOURCE_NITOR);
        assertEquals(2, EquipmentRules.RESOURCE_THAUMIUM_INGOT);
        assertEquals(3, EquipmentRules.RESOURCE_QUICKSILVER);
        assertEquals(4, EquipmentRules.RESOURCE_TALLOW);
        assertEquals(5, EquipmentRules.RESOURCE_BRAIN);
        assertEquals(6, EquipmentRules.RESOURCE_AMBER);
        assertEquals(7, EquipmentRules.RESOURCE_CLOTH);
        assertEquals(8, EquipmentRules.RESOURCE_FILTER);
        assertEquals(9, EquipmentRules.RESOURCE_KNOWLEDGE_FRAGMENT);
        assertEquals(10, EquipmentRules.RESOURCE_MIRRORED_GLASS);
        assertEquals(11, EquipmentRules.RESOURCE_TAINTED_GOO);
        assertEquals(12, EquipmentRules.RESOURCE_TAINT_TENDRIL);
        assertEquals(13, EquipmentRules.RESOURCE_LABEL);
        assertEquals(14, EquipmentRules.RESOURCE_SALIS_MUNDUS);
        assertEquals(15, EquipmentRules.RESOURCE_PRIMAL_CHARM);
        assertEquals(16, EquipmentRules.RESOURCE_VOID_INGOT);
        assertEquals(17, EquipmentRules.RESOURCE_VOID_SEED);
        assertEquals(18, EquipmentRules.RESOURCE_GOLD_COIN);
        // The two tainted resources are the metadata eleven and twelve.
        assertTrue(EquipmentRules.resourceIsTainted(EquipmentRules.RESOURCE_TAINTED_GOO));
        assertTrue(EquipmentRules.resourceIsTainted(EquipmentRules.RESOURCE_TAINT_TENDRIL));
        for (int type = 0; type <= 18; type++) {
            if (type == 11 || type == 12) continue;
            assertFalse(EquipmentRules.resourceIsTainted(type), "the resource " + type);
        }
        // The taint of a carried resource comes on one tick in 4321 and lasts 120 ticks.
        assertEquals(4321, EquipmentRules.TAINT_ITEM_CHANCE, "ItemResource.onUpdate - the roll of the taint");
        assertEquals(120, EquipmentRules.TAINT_ITEM_POISON_TICKS, "ItemResource.onUpdate - the taint poison");
    }

    /**
     * The nineteen resources are nineteen own item ids of the port.
     */
    @Test void theNineteenResourcesAreRegisteredAsOwnItems() {
        for (String id : new String[] {"alumentum", "nitor", "thaumium_ingot", "quick_silver", "magic_tallow",
                "zombie_brain", "amber_gem", "enchanted_fabric", "vis_filter", "knowledge_fragment",
                "mirrored_glass", "tainted_goo", "taint_tendril", "jar_label", "salis_mundus",
                "primal_charm", "void_ingot", "void_seed", "gold_coin"}) {
            assertNotNull(ThaumcraftContent.MATERIALS.get(id), "the resource " + id + " is missing");
            assertEquals("thaumcraft:" + id, ThaumcraftContent.MATERIALS.get(id).getId().toString());
        }
    }

    // ------------------------------------------------------------- the sanity soap

    /**
     * Oracle ItemSanitySoap:35-39 and :88-94 - the soap is used for two hundred ticks and the
     * sticky warp comes with a chance of 0.33, plus 0.25 with the Ward and plus 0.25 in the
     * pure fluid.
     *
     * <p>The original computes the chance in {@code float}: {@code float chance = 0.33f;} and then
     * {@code chance += 0.25f;} once for the Ward and once for the pure fluid. The sum is therefore
     * the single precision {@code 0.33f + 0.25f} (bits {@code 0x3f147ae2}) and not the decimal
     * literal {@code 0.58f} (bits {@code 0x3f147ae1}); the expectations below are written as the
     * original's own expressions so that the test documents that arithmetic.</p>
     */
    @Test void theSanitySoapKeepsTheOriginalDurationAndChances() {
        assertEquals(200, SanitySoapRules.DURATION, "ItemSanitySoap:53 - the two hundred ticks");
        assertEquals(0.33F, SanitySoapRules.chance(false, false), "ItemSanitySoap:88 - the base chance");
        assertEquals(0.33F + 0.25F, SanitySoapRules.chance(true, false), "ItemSanitySoap:88-90 - the Ward adds a quarter");
        assertEquals(0.33F + 0.25F, SanitySoapRules.chance(false, true), "ItemSanitySoap:88-93 - the pure fluid adds a quarter");
        assertEquals(0.33F + 0.25F + 0.25F, SanitySoapRules.chance(true, true), "ItemSanitySoap:88-93 - both add a quarter");
    }

    // ------------------------------------------------------------- the triple meat treat

    /**
     * Oracle ItemTripleMeatTreat:26-31 - the treat restores six points of hunger, four fifths of
     * a point of saturation and gives the regeneration of five seconds in two thirds of the cases.
     */
    @Test void theTripleMeatTreatKeepsTheOriginalFoodValues() {
        assertEquals(6, EquipmentRules.TREAT_HUNGER, "ItemTripleMeatTreat:27 - the six points of hunger");
        assertEquals(0.8F, EquipmentRules.TREAT_SATURATION, "ItemTripleMeatTreat:27 - the four fifths");
        assertEquals(0.66F, EquipmentRules.TREAT_EFFECT_CHANCE, "ItemTripleMeatTreat:29 - the two thirds");
        assertEquals(100, EquipmentRules.TREAT_REGENERATION_TICKS, "ItemTripleMeatTreat:29 - the five seconds in ticks");
    }

    /**
     * The treat carries a name in every language of the port.
     */
    @Test void theTripleMeatTreatIsNamedInEveryLanguage() throws Exception {
        for (String lang : new String[] {"en_us", "ru_ru"}) {
            JsonObject language = language(lang);
            assertNotNull(language.get("item.thaumcraft.triple_meat_treat"), "the treat has no name in " + lang);
            assertFalse(language.get("item.thaumcraft.triple_meat_treat").getAsString().isBlank(), "the name in " + lang);
        }
    }

    /** The language file of the port. */
    private static JsonObject language(String lang) throws Exception {
        var stream = BaseContentItemParityTest.class.getClassLoader()
                .getResourceAsStream("assets/thaumcraft/lang/" + lang + ".json");
        assertNotNull(stream, "the language file " + lang + " is missing");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
