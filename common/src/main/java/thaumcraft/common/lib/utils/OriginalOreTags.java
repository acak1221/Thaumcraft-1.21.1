package thaumcraft.common.lib.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The native replacement of the 1.7.10 Forge ore dictionary of TC4.
 *
 * <p>The original registered its ore names in {@code thaumcraft/common/config/ConfigItems.java:260-301} and used them
 * for the fuzzy infusion comparison ({@code InventoryUtils.areItemStacksEqual}, the {@code useOre} flag), for the
 * crucible catalysts ({@code CrucibleRecipe}) and for the smelting bonuses ({@code ConfigRecipes.java:493-495}). 1.21.1
 * has no ore dictionary, so every original ore name becomes one native item tag under {@code thaumcraft:original_ore/}.
 *
 * <p>The membership of a tag is the membership of the original ore name:
 * <ul>
 *   <li>{@code logWood} is the vanilla Forge log name plus the two magical logs of the original
 *       ({@code ConfigItems.java:294-295}), so it is the tag {@code thaumcraft:original_log_wood} that the recipe layer
 *       already uses.</li>
 *   <li>every other original name holds exactly the items the original registered, plus the vanilla members the
 *       Forge ore name already held before TC4 registered its own item in it.</li>
 * </ul>
 *
 * <p>Forced 1.21.1 change: the original compared the FIRST ore id of a stack and only the ores of that one id. The
 * native comparison asks every registered tag, so a stack that belongs to two of the original names matches a stack
 * of either of them. Every original name holds a disjoint set of materials, so the answer is the same for every
 * material the original recipes use.</p>
 */
public final class OriginalOreTags {
    /** The namespace of every native tag of an original ore name. */
    public static final String NAMESPACE = "thaumcraft";
    /** The path prefix of every native tag of an original ore name. */
    public static final String TAG_ROOT = "original_ore/";

    /** The original ore name and the native item tag that stands for it. */
    private static final Map<String, TagKey<Item>> ORE_TAGS = new LinkedHashMap<>();

    static {
        // Original ConfigItems.java:260-273: the custom ore blocks and the shards.
        ore("oreCinnabar", "ore_cinnabar");
        ore("oreInfusedAir", "ore_infused_air");
        ore("oreInfusedFire", "ore_infused_fire");
        ore("oreInfusedWater", "ore_infused_water");
        ore("oreInfusedEarth", "ore_infused_earth");
        ore("oreInfusedOrder", "ore_infused_order");
        ore("oreInfusedEntropy", "ore_infused_entropy");
        ore("shardAir", "shard_air");
        ore("shardFire", "shard_fire");
        ore("shardWater", "shard_water");
        ore("shardEarth", "shard_earth");
        ore("shardOrder", "shard_order");
        ore("shardEntropy", "shard_entropy");
        ore("oreAmber", "ore_amber");
        // Original ConfigItems.java:274-286: the resources and the nuggets.
        ore("quicksilver", "quicksilver");
        ore("gemAmber", "gem_amber");
        ore("nuggetIron", "nugget_iron");
        ore("nuggetCopper", "nugget_copper");
        ore("nuggetTin", "nugget_tin");
        ore("nuggetSilver", "nugget_silver");
        ore("nuggetLead", "nugget_lead");
        ore("nuggetThaumium", "nugget_thaumium");
        ore("nuggetVoid", "nugget_void");
        ore("nuggetQuicksilver", "nugget_quicksilver");
        ore("nuggetGold", "nugget_gold");
        ore("ingotThaumium", "ingot_thaumium");
        ore("ingotVoid", "ingot_void");
        // Original ConfigItems.java:287-293: the ore clusters.
        ore("clusterIron", "cluster_iron");
        ore("clusterCopper", "cluster_copper");
        ore("clusterTin", "cluster_tin");
        ore("clusterSilver", "cluster_silver");
        ore("clusterLead", "cluster_lead");
        ore("clusterCinnabar", "cluster_cinnabar");
        ore("clusterGold", "cluster_gold");
        // Original ConfigItems.java:294-301: the wood family and the saplings. The log name is the
        // tag the recipe layer already owns, because it holds the vanilla logs as well.
        ORE_TAGS.put("logWood", TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(NAMESPACE, "original_log_wood")));
        ore("plankWood", "plank_wood");
        ore("slabWood", "slab_wood");
        ore("treeSapling", "tree_sapling");
        // The vanilla Forge names the original recipes used (ConfigRecipes.java:131-132, 191, 193, 262, 282, 293,
        // 430, 439, 454, 459-467 and 493-495).
        ore("stickWood", "stick_wood");
        ore("ingotIron", "ingot_iron");
        ore("ingotGold", "ingot_gold");
        ore("ingotCopper", "ingot_copper");
        ore("ingotTin", "ingot_tin");
        ore("ingotSilver", "ingot_silver");
        ore("ingotLead", "ingot_lead");
        ore("dustIron", "dust_iron");
        ore("oreIron", "ore_iron");
        ore("oreGold", "ore_gold");
        ore("oreSilver", "ore_silver");
        ore("oreTin", "ore_tin");
        ore("oreLead", "ore_lead");
        ore("oreCopper", "ore_copper");
    }

    private OriginalOreTags() {}

    /** Registers one original ore name with its native item tag. */
    private static void ore(String oreName, String tagPath) {
        ORE_TAGS.put(oreName, TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(NAMESPACE, TAG_ROOT + tagPath)));
    }

    /**
     * Publishes every original ore name to {@link InventoryUtils}.
     *
     * <p>The root calls this once, right after the item registry is complete, because the tags themselves are read
     * from the item registry when the comparison runs.</p>
     */
    public static void register() {
        for (Map.Entry<String, TagKey<Item>> entry : ORE_TAGS.entrySet()) {
            InventoryUtils.registerOreTag(entry.getKey(), entry.getValue());
        }
    }

    /** The native item tag of an original ore name, or null when the name is unknown. */
    public static TagKey<Item> oreTag(String oreName) {
        return oreName == null ? null : ORE_TAGS.get(oreName);
    }

    /** Every original ore name and its native item tag. */
    public static Map<String, TagKey<Item>> oreTags() {
        return Collections.unmodifiableMap(ORE_TAGS);
    }
}
