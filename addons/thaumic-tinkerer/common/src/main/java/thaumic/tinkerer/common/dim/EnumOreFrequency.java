/**
 * Created by pixlepix on 3/31/14.
 * <p/>
 * Numbers here used with permission of Vazkii
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.dim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.OreDictionaryNames;

/**
 * The ores of the Bedrock dimension and how often each is picked for a cluster (1.7.10: the Forge ore dictionary names
 * and the weights agreed with Vazkii). The ore dictionary is gone: a name is the item tag that
 * {@link OreDictionaryNames} resolves it to ({@code oreCoal} is {@code c:ores/coal}), and "the first block registered
 * under the name" ({@code OreDictionary.getOres(name).get(0)}) is the member of that tag with the lowest registry id.
 * A name is valid when it is not blacklisted, its tag has members and that first member is a block item, exactly the
 * three conditions of the original. Names no mod adds to a world are skipped, as an empty ore dictionary entry was.
 */
public enum EnumOreFrequency {

    ALUMINUM("oreAluminum", 617),
    AMBER("oreAmber", 161),
    APATITE("oreApatite", 269),
    TOPAZ("oreTopaz", 238),
    SILICON("oreSilicon", 234),
    ILMENITE("oreIlmenite", 270),
    CINNABAR("oreCinnabar", 172),
    COAL("oreCoal", 2648),
    COPPER("oreCopper", 603),
    DIAMOND("oreDiamond", 67),
    EMERALD("oreEmerald", 48),
    TANZANITE("oreTanzanite", 61),
    GOLD("oreGold", 164),
    AIR("oreInfusedAir", 94),
    EARTH("oreInfusedEarth", 35),
    ENTROPY("oreInfusedEntropy", 53),
    FIRE("oreInfusedFire", 42),
    ORDER("oreInfusedOrder", 31),
    WATER("oreInfusedWater", 27),
    IRON("oreIron", 1503),
    LAPIS("oreLapis", 57),
    LEAD("oreLead", 335),
    // DRACONIUM("oreDraconium", 72),
    PERIDOT("orePeridot", 79),
    REDSTONE("oreRedstone", 364),
    RUBY("oreRuby", 57),
    // METEORICIRON("oreMeteoricIron", 86),
    SAPPHIRE("oreSapphire", 70),
    MALACHITE("oreMalachite", 416),
    DESH("oreDesh", 105),
    TIN("oreTin", 507),
    URANIUM("oreUranium", 112),
    // ELECTROTINE("oreElectrotine", 392),
    NETHERQUARTZ("oreNetherQuartz", 809),
    // CERTUS_QUARTZ("oreCertusQuartz", 234),
    CHARGED_CERTUS_QUARTZ("oreChargedCertusQuartz", 117),
    CHEESE("oreCheese", 1024);
    // NICKEL("oreNickel", 144),
    // SULFUR("oreSulfur", 288),
    // COBALT("oreCobalt", 96),
    // ORIHARUKON("oreOriharukon", 44),
    // TUNGSTEN("oreTungsten", 22),
    // MAGNESIUM("oreMagnesium", 96),
    // ANDAMANTIUM("oreAdamantium", 11),
    // PLATINUM("orePlatinum", 33),
    // MITHRIL("oreMithril", 44);

    /**
     * Ore names whose 1.7.10 Forge convention differs from the {@code c:} tag the shared conventions of 1.21.1 use:
     * Nether quartz ore is {@code c:ores/quartz} there ({@code OreDictionaryNames} says {@code c:ores/nether_quartz}, which no
     * mod fills). The alias is tried when the tag of the plain name has no member.
     */
    private static final String[][] TAG_ALIASES = { { "oreNetherQuartz", "c:ores/quartz" } };

    public int freq;
    public String name;

    EnumOreFrequency(String name, int freq) {
        this.name = name;
        this.freq = freq;
    }

    // ---- the pure part: weights ----------------------------------------------------------------------------------

    /**
     * 1.7.10 {@code getRandomOre}: one draw {@code nextInt(sum)}, then the entries are walked in order, subtracting their
     * weights, until the draw goes negative. Returns the index of the entry picked, or -1 when there are no weights
     * (the original would throw on {@code nextInt(0)}).
     */
    public static int pickIndex(int[] weights, Random rand) {
        int sum = 0;
        for (int weight : weights) sum += weight;
        if (sum <= 0) return -1;
        int randInt = rand.nextInt(sum);
        for (int i = 0; i < weights.length; i++) {
            randInt -= weights[i];
            if (randInt < 0) return i;
        }
        // Unreachable for positive weights; the original fell back to iron ore here.
        return -1;
    }

    // ---- the world part: tags and blocks ---------------------------------------------------------------------------

    /** The tag of the ore name, or null when no rule knows the name. */
    public TagKey<Item> tag() {
        return OreDictionaryNames.itemTag(name);
    }

    /**
     * The first block registered under the ore name (1.7.10 {@code getOres(name).get(0)}): the member of the name's tag
     * with the lowest registry id, or of its alias tag ({@link #TAG_ALIASES}) when that tag is empty. Null when there is
     * none or when it is not a block item.
     */
    public Block firstBlock() {
        List<TagKey<Item>> candidates = new ArrayList<>();
        TagKey<Item> own = tag();
        if (own != null) candidates.add(own);
        for (String[] alias : TAG_ALIASES) {
            if (!alias[0].equals(name)) continue;
            ResourceLocation id = ResourceLocation.tryParse(alias[1]);
            if (id != null) candidates.add(TagKey.create(Registries.ITEM, id));
        }
        for (TagKey<Item> candidate : candidates) {
            Item first = firstOf(candidate);
            if (first != null) return first instanceof BlockItem blockItem ? blockItem.getBlock() : null;
        }
        return null;
    }

    private static Item firstOf(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag).map(HolderSet.Named::stream).orElseGet(java.util.stream.Stream::empty)
                .map(holder -> holder.value()).min(Comparator.comparingInt(BuiltInRegistries.ITEM::getId)).orElse(null);
    }

    /**
     * 1.7.10 {@code isValid}: not on the blacklist, the name has members, and the first is a block. The blacklist is the
     * config option {@code bedrockOreBlacklist}: entries are 1.7.10 ore names ({@code oreCoal}), tag ids
     * ({@code c:ores/coal}, with or without a leading {@code #}) or block ids ({@code minecraft:coal_ore}).
     */
    public boolean isValid(String[] blacklist) {
        if (blacklist != null && isBlacklisted(blacklist, null)) return false;
        Block block = firstBlock();
        return block != null && (blacklist == null || !isBlacklisted(blacklist, block));
    }

    /** True when an entry of the blacklist names this ore (by 1.7.10 name or tag id) or its block id. */
    public boolean isBlacklisted(String[] blacklist, Block block) {
        TagKey<Item> tag = tag();
        String tagId = tag == null ? null : tag.location().toString();
        String blockId = block == null ? null : BuiltInRegistries.BLOCK.getKey(block).toString();
        for (String entry : blacklist) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
            if (trimmed.equals(name) || (tagId != null && trimmed.equals(tagId))
                    || (blockId != null && trimmed.equals(blockId))) {
                return true;
            }
        }
        return false;
    }

    /** The valid ores in declaration order (1.7.10 {@code getValidOres}). */
    public static List<EnumOreFrequency> getValidOres(String[] blacklist) {
        List<EnumOreFrequency> result = new ArrayList<>();
        for (EnumOreFrequency e : EnumOreFrequency.values()) {
            if (e.isValid(blacklist)) result.add(e);
        }
        return result;
    }

    /** The sum of the weights of the valid ores (1.7.10 {@code getSum}). */
    public static int getSum(String[] blacklist) {
        int total = 0;
        for (EnumOreFrequency e : getValidOres(blacklist)) total += e.freq;
        return total;
    }

    /**
     * 1.7.10 {@code getRandomOre}: the state of the block of one valid ore, chosen by weight. The original answered iron
     * ore when the draw fell through; here that case and "no valid ore at all" answer null and the caller skips the cluster
     * (the original crashed on {@code nextInt(0)} without ores).
     */
    public static BlockState getRandomOre(Random rand, String[] blacklist) {
        List<EnumOreFrequency> valid = getValidOres(blacklist);
        int[] weights = new int[valid.size()];
        for (int i = 0; i < weights.length; i++) weights[i] = valid.get(i).freq;
        int index = pickIndex(weights, rand);
        if (index < 0) return null;
        Block block = valid.get(index).firstBlock();
        return block == null ? null : block.defaultBlockState();
    }
}
