package thaumcraft.api;

import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The 1.7.10 Forge ore dictionary names an add-on used, as 1.21.1 item tags.
 *
 * <p>1.21.1 has no ore dictionary; item tags took its place, and the shared {@code c:} tags of Fabric and NeoForge
 * hold the materials of every mod. The recipe and object tag API accepts the old names and resolves them here, so
 * {@code ThaumcraftApi.registerObjectTag("ingotCopper", aspects)} and a recipe ingredient {@code "ingotCopper"} keep
 * working. The first rule that applies wins:</p>
 * <ol>
 *   <li>the ore names Thaumcraft 4 itself registered or used ({@code nuggetThaumium}, {@code shardAir},
 *       {@code logWood}, {@code stickWood}, {@code ingotIron}...) are the {@code thaumcraft:original_ore/*} tags, which hold the
 *       Thaumcraft items and the vanilla ones the name held in 1.7.10;</li>
 *   <li>a namespaced id ({@code "c:ingots/copper"}, {@code "minecraft:logs"}) is that tag;</li>
 *   <li>a Forge name of a vanilla thing ({@code blockGlass}, {@code slimeball}, {@code dyeRed}...)
 *       is its conventional tag;</li>
 *   <li>a name of the Forge convention {@code <kind><Material>} is {@code c:<kinds>/<material>}: {@code ingotCopper}
 *       is {@code c:ingots/copper}, {@code oreLapis} {@code c:ores/lapis}, {@code blockSilver}
 *       {@code c:storage_blocks/silver}, {@code dustRedstone} {@code c:dusts/redstone}, {@code gemNetherQuartz}
 *       {@code c:gems/nether_quartz}.</li>
 * </ol>
 * <p>Any other name answers null. A tag only names its members; which items it holds is decided by the data packs
 * of the game, and a material no mod adds has an empty tag, exactly like an empty ore dictionary entry of 1.7.10.</p>
 */
public final class OreDictionaryNames {
    /** Forge names of vanilla things and their conventional tags (rule 3). */
    private static final Map<String, String> VANILLA = Map.ofEntries(
            Map.entry("stairWood", "minecraft:wooden_stairs"),
            Map.entry("treeLeaves", "minecraft:leaves"),
            Map.entry("blockGlass", "c:glass_blocks"),
            Map.entry("blockGlassColorless", "c:glass_blocks/colorless"),
            Map.entry("glass", "c:glass_blocks"),
            Map.entry("paneGlass", "c:glass_panes"),
            Map.entry("paneGlassColorless", "c:glass_panes/colorless"),
            Map.entry("stone", "c:stones"),
            Map.entry("cobblestone", "c:cobblestones"),
            Map.entry("sand", "c:sands"),
            Map.entry("sandstone", "c:sandstone/blocks"),
            Map.entry("gravel", "c:gravels"),
            Map.entry("obsidian", "c:obsidians"),
            Map.entry("netherrack", "c:netherracks"),
            Map.entry("chestWood", "c:chests/wooden"),
            Map.entry("chest", "c:chests"),
            Map.entry("workbench", "c:player_workstations/crafting_tables"),
            Map.entry("blockWool", "minecraft:wool"),
            Map.entry("wool", "minecraft:wool"),
            Map.entry("string", "c:strings"),
            Map.entry("feather", "c:feathers"),
            Map.entry("leather", "c:leathers"),
            Map.entry("bone", "c:bones"),
            Map.entry("gunpowder", "c:gunpowders"),
            Map.entry("egg", "c:eggs"),
            Map.entry("slimeball", "c:slime_balls"),
            Map.entry("enderpearl", "c:ender_pearls"),
            Map.entry("record", "c:music_discs"),
            Map.entry("dye", "c:dyes"),
            Map.entry("cropNetherWart", "c:crops/nether_wart"),
            Map.entry("gemQuartz", "c:gems/quartz"),
            Map.entry("blockQuartz", "c:storage_blocks/quartz"),
            Map.entry("dustGlowstone", "c:dusts/glowstone"));
    /** The kinds of the Forge convention and the root of their {@code c:} tags (rule 4). */
    private static final Map<String, String> KINDS = Map.ofEntries(
            Map.entry("ingot", "ingots"), Map.entry("nugget", "nuggets"), Map.entry("gem", "gems"),
            Map.entry("dust", "dusts"), Map.entry("ore", "ores"), Map.entry("block", "storage_blocks"),
            Map.entry("plate", "plates"), Map.entry("gear", "gears"), Map.entry("rod", "rods"),
            Map.entry("crop", "crops"), Map.entry("seed", "seeds"), Map.entry("dye", "dyes"),
            Map.entry("raw", "raw_materials"));

    private OreDictionaryNames() {}

    /** The item tag of an ore dictionary name or a namespaced tag id; null when no rule applies. */
    public static TagKey<Item> itemTag(String name) {
        ResourceLocation id = tagId(name);
        return id == null ? null : TagKey.create(Registries.ITEM, id);
    }

    /** The tag id of an ore dictionary name or a namespaced tag id; null when no rule applies. */
    public static ResourceLocation tagId(String name) {
        if (name == null || name.isEmpty()) return null;
        TagKey<Item> original = thaumcraft.common.lib.utils.OriginalOreTags.oreTag(name);
        if (original != null) return original.location();
        if (name.contains(":")) return ResourceLocation.tryParse(name);
        String vanilla = VANILLA.get(name);
        if (vanilla != null) return ResourceLocation.parse(vanilla);
        for (var kind : KINDS.entrySet()) {
            String prefix = kind.getKey();
            if (name.length() <= prefix.length() || !name.startsWith(prefix)) continue;
            String material = name.substring(prefix.length());
            if (!Character.isUpperCase(material.charAt(0))) continue;
            String path = kind.getValue() + "/" + snake(material);
            return ResourceLocation.tryBuild("c", path);
        }
        return null;
    }

    /** {@code NetherQuartz} is {@code nether_quartz}. */
    private static String snake(String camel) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < camel.length(); index++) {
            char c = camel.charAt(index);
            if (Character.isUpperCase(c) && index > 0) out.append('_');
            out.append(Character.toLowerCase(c));
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
