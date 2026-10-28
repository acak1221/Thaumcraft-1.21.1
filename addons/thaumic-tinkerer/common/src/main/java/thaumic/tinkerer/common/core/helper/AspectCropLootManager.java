/**
 * Created by pixlepix on 8/7/14, reworked by Spaghetti on 2026-08-21 These very lines of code will cause great trouble
 * in the future. However, they have proven to be less chaotic than anticipated in the past, so they live on ... even if
 * that means nerfing them to the ground.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.items.ItemsTC;
import thaumic.tinkerer.common.item.ItemInfusedGrain;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * What a ripe infused crop yields: for every aspect a weighted list of stacks. The table is the 1.7.10 one entry for
 * entry; the entries that 1.7.10 found by scanning the ore dictionary for a name ({@code addAspectLoot(aspect, "logWood")})
 * are item tags here, resolved when the loot is rolled (tags are not bound while the table is filled), one entry per
 * member of the tag, each worth one.
 *
 * <p>Ore names without a counterpart in Minecraft 1.21.1 or its common tags ("bucketEnder", "rodBlizz", "cloud")
 * add nothing, as they added nothing in 1.7.10 without the mods that registered them. The table is
 * {@link #getAllLoot()} for the recipe viewers (the NEI handler of 1.7.10 listed it).</p>
 */
public class AspectCropLootManager {

    /** A source of stacks: one stack, or every member of an item tag in the given count. */
    private record Source(@Nullable ItemStack stack, @Nullable TagKey<Item> tag, int count, int weight) {
        List<ItemStack> resolve() {
            List<ItemStack> stacks = new ArrayList<>();
            if (stack != null) {
                stacks.add(stack.copy());
            } else if (tag != null) {
                var named = BuiltInRegistries.ITEM.getTag(tag);
                if (named.isPresent()) {
                    for (Holder<Item> holder : named.get()) {
                        stacks.add(new ItemStack(holder.value(), count));
                    }
                }
            }
            return stacks;
        }
    }

    public static class LootEntry {

        public final ItemStack stack;
        public final int weight;

        public LootEntry(ItemStack stack, int weight) {
            this.stack = stack;
            this.weight = weight;
        }
    }

    private static final Random RAND = new Random();

    private static final Map<Aspect, List<Source>> lootMap = new HashMap<>();

    /** One weighted stack for the aspect, a copy; null when the aspect yields nothing. */
    @Nullable
    public static ItemStack getLootForAspect(@Nullable Aspect aspect) {
        List<LootEntry> aspectLootList = entriesOf(aspect);

        if (aspectLootList.isEmpty()) {
            return null;
        }

        int sum = 0;
        for (LootEntry entry : aspectLootList) {
            sum += entry.weight;
        }

        if (sum > 0) {
            int randInt = RAND.nextInt(sum);
            for (LootEntry entry : aspectLootList) {
                if (randInt < entry.weight) {
                    return entry.stack.copy();
                }
                randInt -= entry.weight;
            }
        }
        return null;
    }

    private static List<LootEntry> entriesOf(@Nullable Aspect aspect) {
        List<LootEntry> entries = new ArrayList<>();
        List<Source> sources = aspect == null ? null : lootMap.get(aspect);
        if (sources == null) return entries;
        for (Source source : sources) {
            for (ItemStack stack : source.resolve()) {
                if (!stack.isEmpty()) entries.add(new LootEntry(stack, source.weight()));
            }
        }
        return entries;
    }

    public static void addAspectLoot(Aspect aspect, ItemStack stack) {
        addAspectLoot(aspect, stack, 1);
    }

    /** Every member of the tag, one stack of {@code count} each, each worth one (the ore dictionary scan of 1.7.10). */
    public static void addAspectLoot(Aspect aspect, TagKey<Item> target, int count) {
        lootMap.computeIfAbsent(aspect, a -> new ArrayList<>()).add(new Source(null, target, count, 1));
    }

    public static void addAspectLoot(Aspect aspect, TagKey<Item> target) {
        addAspectLoot(aspect, target, 1);
    }

    /** The given items, one stack of {@code count} each, each worth one. */
    public static void addAspectLoot(Aspect aspect, Item[] items, int count) {
        for (Item item : items) {
            addAspectLoot(aspect, new ItemStack(item, count), 1);
        }
    }

    public static void addAspectLoot(Aspect aspect, ItemStack stack, int power) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        lootMap.computeIfAbsent(aspect, a -> new ArrayList<>()).add(new Source(stack.copy(), null, 0, power));
    }

    /** The table with the tags resolved (for the recipe viewers): aspect to weighted stacks. */
    public static Map<Aspect, List<LootEntry>> getAllLoot() {
        Map<Aspect, List<LootEntry>> all = new LinkedHashMap<>();
        for (Aspect aspect : Aspect.aspects.values()) {
            all.put(aspect, entriesOf(aspect));
        }
        return all;
    }

    private static TagKey<Item> tag(String id) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(id));
    }

    @Nullable
    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    /** Fills the table; runs when Thaumcraft is ready (the items of Thaumcraft and the add-on exist then). */
    public static synchronized void populateLootMap() {
        lootMap.clear();
        for (Aspect a : Aspect.aspects.values()) {
            lootMap.put(a, new ArrayList<>());
        }
        for (Aspect primal : ItemInfusedGrain.ASPECTS) {
            addAspectLoot(primal, ItemInfusedGrain.stackFor(primal));
        }

        addAspectLoot(Aspect.ORDER, new ItemStack(Items.GLASS, 16));
        addAspectLoot(Aspect.ENTROPY, new ItemStack(Items.SAND, 16));

        addAspectLoot(Aspect.ELDRITCH, new ItemStack(Items.ENDER_PEARL, 4), 10);
        addAspectLoot(Aspect.ELDRITCH, new ItemStack(Items.ENDER_EYE, 4), 5);
        // "bucketEnder": a bucket of ender from EnderIO, no counterpart
        addAspectLoot(Aspect.ELDRITCH, ItemApi.getItem("itemResource", 17)); // void seed

        addAspectLoot(Aspect.TREE, ItemTags.LOGS, 1); // "logWood"

        for (Aspect tag : Aspect.aspects.values()) {
            ItemStack i = ItemsTC.stack("wisp_essence");
            if (i.getItem() instanceof IEssentiaContainerItem container) {
                container.setAspects(i, new AspectList().add(tag, 2));
            }
            addAspectLoot(Aspect.AURA, i);
        }

        addAspectLoot(Aspect.MIND, new ItemStack(Items.PAPER, 64), 15);
        addAspectLoot(Aspect.MIND, new ItemStack(Items.BOOK, 32), 10);
        addAspectLoot(Aspect.MIND, new ItemStack(Items.BOOKSHELF, 16), 5);
        addAspectLoot(Aspect.MIND, stackOf(ItemApi.getItem("itemResource", 9), 4), 5); // knowledge fragment

        addAspectLoot(Aspect.FLESH, stackOf(ItemApi.getItem("itemResource", 5), 16), 1); // zombie brain (the defunct metadata 5)

        addAspectLoot(Aspect.UNDEAD, new ItemStack(Items.ROTTEN_FLESH, 32));
        addAspectLoot(Aspect.UNDEAD, new ItemStack(Items.BONE, 24));

        Item darkQuartz = item(LibMisc.MOD_ID + ":" + LibBlockNames.DARK_QUARTZ);
        if (darkQuartz != null) addAspectLoot(Aspect.CRAFT, new ItemStack(darkQuartz, 32));
        addAspectLoot(Aspect.CRAFT, stackOf(ItemApi.getBlock("blockStoneDevice", 0), 16)); // alchemical furnace
        addAspectLoot(Aspect.CRAFT, new ItemStack(Items.CRAFTING_TABLE, 4));

        addAspectLoot(Aspect.HUNGER, new ItemStack(Items.NETHER_WART, 16));

        addAspectLoot(Aspect.COLD, new ItemStack(Items.ICE, 2));
        addAspectLoot(Aspect.COLD, new ItemStack(Items.SNOWBALL, 64));
        addAspectLoot(Aspect.COLD, new ItemStack(Items.SNOW_BLOCK, 32));
        addAspectLoot(Aspect.COLD, new ItemStack(Items.SNOW, 8));
        // "rodBlizz": the blizz rod of Thermal Foundation, no counterpart

        // "grass": nothing in Forge's ore dictionary of 1.7.10, plainly meant for the grasses; the saplings are "sapling"
        addAspectLoot(Aspect.PLANT, new Item[] { Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN }, 1);
        addAspectLoot(Aspect.PLANT, ItemTags.SAPLINGS, 1);

        for (int i = 0; i < 12; i++) {
            addAspectLoot(Aspect.MAN, ItemApi.getItem("itemGolemCore", i));
        }

        addAspectLoot(Aspect.ARMOR, new ItemStack(Items.DIAMOND_BOOTS));
        addAspectLoot(Aspect.ARMOR, new ItemStack(Items.DIAMOND_LEGGINGS));
        addAspectLoot(Aspect.ARMOR, new ItemStack(Items.DIAMOND_CHESTPLATE));
        addAspectLoot(Aspect.ARMOR, new ItemStack(Items.DIAMOND_HELMET));

        addAspectLoot(Aspect.MINE, ItemApi.getItem("itemPickThaumium", 0));
        addAspectLoot(Aspect.HARVEST, ItemApi.getItem("itemHoeThaumium", 0));
        addAspectLoot(Aspect.WEAPON, ItemApi.getItem("itemSwordThaumium", 0));
        addAspectLoot(Aspect.TOOL, ItemApi.getItem("itemShovelThaumium", 0));
        addAspectLoot(Aspect.TOOL, ItemApi.getItem("itemAxeThaumium", 0));

        addAspectLoot(Aspect.SLIME, tag("c:slime_balls"), 4); // "slime"

        addAspectLoot(Aspect.GREED, new ItemStack(Items.GOLD_INGOT, 4));
        addAspectLoot(Aspect.GREED, new ItemStack(Items.EMERALD, 2));

        addAspectLoot(Aspect.LIGHT, new ItemStack(Items.GLOWSTONE_DUST, 16), 5);
        addAspectLoot(Aspect.LIGHT, stackOf(ItemApi.getItem("itemResource", 1), 4)); // nitor
        Item brightNitor = item(LibMisc.MOD_ID + ":" + LibItemNames.BRIGHT_NTIOR);
        if (brightNitor != null) addAspectLoot(Aspect.LIGHT, new ItemStack(brightNitor));

        addAspectLoot(Aspect.MECHANISM, new ItemStack(Items.PISTON, 8));
        addAspectLoot(Aspect.MECHANISM, new ItemStack(Items.HOPPER, 4));
        addAspectLoot(Aspect.MECHANISM, new ItemStack(Items.DROPPER, 8));
        addAspectLoot(Aspect.MECHANISM, new ItemStack(Items.DISPENSER, 8));

        addAspectLoot(Aspect.CROP, new ItemStack(Items.WHEAT, 32));
        addAspectLoot(Aspect.CROP, tag("c:seeds"), 1); // "seed"

        addAspectLoot(Aspect.METAL, new ItemStack(Items.IRON_INGOT, 4), 100);

        addAspectLoot(Aspect.DEATH, new ItemStack(Items.BONE, 32));

        addAspectLoot(Aspect.MOTION, new ItemStack(Items.RAIL), 10);

        addAspectLoot(Aspect.MOTION, new ItemStack(Items.ACTIVATOR_RAIL));

        addAspectLoot(Aspect.CLOTH, new ItemStack(Items.WHITE_WOOL, 16), 30);
        addAspectLoot(Aspect.CLOTH, new ItemStack(Items.STRING, 15), 10);
        for (DyeColor color : DyeColor.values()) {
            addAspectLoot(Aspect.CLOTH, new ItemStack(coloured(color, "wool"), 4));
        }

        addAspectLoot(Aspect.EXCHANGE, tag("c:ingots/copper"), 4); // "ingotCopper"
        addAspectLoot(Aspect.EXCHANGE, stackOf(ItemApi.getBlock("blockCustomOre", 0), 4)); // cinnabar ore

        addAspectLoot(Aspect.ENERGY, stackOf(ItemApi.getItem("itemResource", 0), 12)); // alumentum

        for (int i = 0; i <= 6; i++) {
            addAspectLoot(Aspect.MAGIC, ItemApi.getItem("itemShard", i));
        }

        addAspectLoot(Aspect.HEAL, new ItemStack(Items.GOLDEN_APPLE));
        addAspectLoot(Aspect.HEAL, new ItemStack(Items.CAKE));
        addAspectLoot(Aspect.HEAL, PotionContents.createItemStack(Items.POTION, Potions.HEALING)); // potionitem 8261

        for (DyeColor color : DyeColor.values()) {
            addAspectLoot(Aspect.SENSES, new ItemStack(DyeItem.byColor(color)));
            addAspectLoot(Aspect.SENSES, new ItemStack(coloured(color, "stained_glass")));
            addAspectLoot(Aspect.SENSES, new ItemStack(coloured(color, "stained_glass_pane"), 2));
        }

        addAspectLoot(Aspect.SOUL, new ItemStack(Items.SOUL_SAND, 16), 2);

        // "cloud": a block of Botania/ExtraUtilities, no counterpart

        addAspectLoot(Aspect.DARKNESS, new ItemStack(Items.OBSIDIAN, 10));

        addAspectLoot(Aspect.VOID, new ItemStack(Items.BUCKET));
        addAspectLoot(Aspect.VOID, new Item[] { Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.MILK_BUCKET }, 1); // "bucket"
        addAspectLoot(Aspect.VOID, new ItemStack(Items.BOWL, 4)); // "bowl" (count 4)
        addAspectLoot(Aspect.VOID, stackOf(ItemsTC.stack("void_ingot"), 2)); // "ingotVoid" (count 2)

        addAspectLoot(Aspect.POISON, stackOf(ItemApi.getItem("itemResource", 3), 16)); // quicksilver
        addAspectLoot(Aspect.POISON, new ItemStack(Items.SPIDER_EYE, 8));

        addAspectLoot(Aspect.LIFE, new ItemStack(Items.EGG, 8));

        addAspectLoot(Aspect.TRAP, new ItemStack(Items.COBWEB, 4));
        addAspectLoot(Aspect.TRAP, ItemTags.WOODEN_TRAPDOORS, 1); // "trapdoorWood"
        addAspectLoot(Aspect.TRAP, stackOf(ItemApi.getItem("itemResource", 6), 16)); // amber

        addAspectLoot(Aspect.TRAVEL, stackOf(ItemApi.getBlock("blockCosmeticSolid", 2), 4)); // paving stone of travel

        addAspectLoot(Aspect.TAINT, stackOf(ItemApi.getItem("itemResource", 11), 4));

        addAspectLoot(Aspect.CRYSTAL, new ItemStack(Items.DIAMOND, 1));
        addAspectLoot(Aspect.CRYSTAL, new ItemStack(Items.QUARTZ, 8));
        addAspectLoot(Aspect.CRYSTAL, new ItemStack(Items.GLASS, 32), 4);
    }

    private static Item coloured(DyeColor color, String suffix) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_" + suffix));
    }

    /** The stack with another count; an absent stack stays absent. */
    @Nullable
    private static ItemStack stackOf(@Nullable ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) return null;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
