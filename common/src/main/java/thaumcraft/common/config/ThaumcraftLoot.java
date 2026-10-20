package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.ItemLootBag;
import thaumcraft.common.lib.loot.OriginalLootEnchantments;

/** Original Config.initLoot chest entries and three flattened ItemLootBag subtypes. */
public final class ThaumcraftLoot {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    public static final RegistrySupplier<ItemLootBag> LOOT_BAG_COMMON = ITEMS.register("loot_bag_common",
            () -> new ItemLootBag(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON), 0));
    public static final RegistrySupplier<ItemLootBag> LOOT_BAG_UNCOMMON = ITEMS.register("loot_bag_uncommon",
            () -> new ItemLootBag(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), 1));
    public static final RegistrySupplier<ItemLootBag> LOOT_BAG_RARE = ITEMS.register("loot_bag_rare",
            () -> new ItemLootBag(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2));
    private static ItemStack chargedAmulet;
    private static boolean registered;
    private ThaumcraftLoot() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, LOOT_BAG_COMMON, LOOT_BAG_UNCOMMON, LOOT_BAG_RARE);
    }

    /** Original initLoot uses one time-seeded charge template, shared by all chest/bag copies. */
    public static synchronized ItemStack lootAmulet() {
        if (chargedAmulet == null) {
            ItemStack amulet = new ItemStack(ThaumcraftBaubles.VIS_AMULET_LESSER.get());
            Random random = new Random(System.currentTimeMillis());
            for (Aspect primal : Aspect.getPrimalAspects())
                ThaumcraftBaubles.VIS_AMULET_LESSER.get().addVis(amulet, primal, random.nextInt(5), true);
            chargedAmulet = amulet;
        }
        return chargedAmulet.copy();
    }

    public record DungeonEntry(ItemStack baseStack, int minCount, int maxCount, int weight) {
        public DungeonEntry { baseStack = baseStack.copy(); }
        @Override public ItemStack baseStack() { return baseStack.copy(); }
    }
    private static void entry(List<DungeonEntry> entries, Item item, int minimum, int maximum, int weight) {
        entries.add(new DungeonEntry(new ItemStack(item), minimum, maximum, weight));
    }

    /**
     * Forge1.7 ChestGenHooks.getItems("dungeonChest", world.rand), consumed by original Pech trades.
     * This is an ENTRY list, never a modern weighted dungeon-table roll. The enchanted-book
     * entry is regenerated on EVERY invocation before the Pech's uniform entry selection.
     */
    public static List<DungeonEntry> dungeonEntries(ServerLevel level) {
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level)
            throw new IllegalStateException("Dungeon loot descriptors require the canonical server level/thread");
        return dungeonEntries(level,level.random);
    }

    /** Original Greatwood uses a private tree RNG. Region generation must not touch
     * ServerLevel.random or require its owner thread just to create loot descriptors. */
    public static List<DungeonEntry> dungeonEntries(net.minecraft.world.level.WorldGenLevel level,net.minecraft.util.RandomSource random) {
        java.util.Objects.requireNonNull(level);java.util.Objects.requireNonNull(random);
        List<DungeonEntry> entries = new ArrayList<>();
        entry(entries, Items.SADDLE, 1, 1, 10);
        entry(entries, Items.IRON_INGOT, 1, 4, 10);
        entry(entries, Items.BREAD, 1, 1, 10);
        entry(entries, Items.WHEAT, 1, 4, 10);
        entry(entries, Items.GUNPOWDER, 1, 4, 10);
        entry(entries, Items.STRING, 1, 4, 10);
        entry(entries, Items.BUCKET, 1, 1, 10);
        entry(entries, Items.GOLDEN_APPLE, 1, 1, 1);
        entry(entries, Items.REDSTONE, 1, 4, 10);
        entry(entries, Items.MUSIC_DISC_13, 1, 1, 10);
        entry(entries, Items.MUSIC_DISC_CAT, 1, 1, 10);
        entry(entries, Items.NAME_TAG, 1, 1, 10);
        entry(entries, Items.GOLDEN_HORSE_ARMOR, 1, 1, 2);
        entry(entries, Items.IRON_HORSE_ARMOR, 1, 1, 5);
        entry(entries, Items.DIAMOND_HORSE_ARMOR, 1, 1, 1);
        entries.add(new DungeonEntry(OriginalLootEnchantments.enchant(level.registryAccess(), new ItemStack(Items.BOOK), random, 30), 1, 1, 1));
        entries.addAll(chestAdditions("dungeonChest"));
        return entries;
    }

    /** Original TC4 additions only; the loader appends these to the native chest's existing weighted pool. */
    public static List<DungeonEntry> chestAdditions(String category) {
        List<DungeonEntry> entries = new ArrayList<>();
        if (category.equals("villageBlacksmith")) {
            entry(entries, ThaumcraftContent.MATERIALS.get("thaumium_ingot").get(), 1, 3, 10);
            return entries;
        }
        int common, uncommon;
        switch (category) {
            case "dungeonChest", "pyramidJungleChest", "pyramidDesertyChest" -> { common = 5; uncommon = 4; }
            case "mineshaftCorridor", "strongholdCorridor", "strongholdCrossing", "strongholdLibrary" -> { common = 4; uncommon = 3; }
            default -> { return entries; }
        }
        for (Item item : List.of(LOOT_BAG_COMMON.get(), ThaumcraftContent.MATERIALS.get("thaumium_ingot").get(), ThaumcraftContent.MATERIALS.get("amber_gem").get()))
            entry(entries, item, 1, 3, common);
        for (Item item : List.of(LOOT_BAG_UNCOMMON.get(), ThaumcraftBaubles.MUNDANE_AMULET.get(), ThaumcraftBaubles.MUNDANE_RING.get(), ThaumcraftBaubles.MUNDANE_BELT.get(), ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()))
            entry(entries, item, 1, 2, uncommon);
        if (category.equals("strongholdLibrary"))
            entry(entries, ThaumcraftContent.MATERIALS.get("knowledge_fragment").get(), 3, 6, 20);
        for (Item item : List.of(LOOT_BAG_RARE.get(), ThaumcraftContent.THAUMONOMICON.get(), ThaumcraftEquipment.THAUMIUM_SWORD.get(), ThaumcraftEquipment.THAUMIUM_PICKAXE.get(), ThaumcraftEquipment.THAUMIUM_AXE.get(), ThaumcraftEquipment.THAUMIUM_HOE.get(), ThaumcraftBaubles.RUNIC_RING_LESSER.get()))
            entry(entries, item, 1, 1, 1);
        for (String primal : List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"))
            entry(entries, ThaumcraftBaubles.DISCOUNT_RINGS.get(primal).get(), 1, 1, 1);
        entries.add(new DungeonEntry(lootAmulet(), 1, 1, 1));
        return entries;
    }
}
