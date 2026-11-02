package thaumcraft.common.lib.loot;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLoot;

/** Owner-approved modern exploration extension, separate from the retained TC4 chest weights. */
public final class ModernStructureLoot {
    public enum Kind { ANCIENT_CITY, TRIAL_CHEST, VAULT, OMINOUS_VAULT, ARCHAEOLOGY }
    private static final Map<String, Kind> TABLES = Map.ofEntries(
            Map.entry("chests/ancient_city", Kind.ANCIENT_CITY),
            Map.entry("chests/trial_chambers/entrance", Kind.TRIAL_CHEST),
            Map.entry("chests/trial_chambers/corridor", Kind.TRIAL_CHEST),
            Map.entry("chests/trial_chambers/intersection", Kind.TRIAL_CHEST),
            Map.entry("chests/trial_chambers/intersection_barrel", Kind.TRIAL_CHEST),
            Map.entry("chests/trial_chambers/supply", Kind.TRIAL_CHEST),
            // Only top-level vault tables: modifying nested common/rare/unique tables would multiply the bonus.
            Map.entry("chests/trial_chambers/reward", Kind.VAULT),
            Map.entry("chests/trial_chambers/reward_ominous", Kind.OMINOUS_VAULT),
            Map.entry("archaeology/desert_pyramid", Kind.ARCHAEOLOGY),
            Map.entry("archaeology/desert_well", Kind.ARCHAEOLOGY),
            Map.entry("archaeology/ocean_ruin_cold", Kind.ARCHAEOLOGY),
            Map.entry("archaeology/ocean_ruin_warm", Kind.ARCHAEOLOGY),
            Map.entry("archaeology/trail_ruins_common", Kind.ARCHAEOLOGY),
            Map.entry("archaeology/trail_ruins_rare", Kind.ARCHAEOLOGY));
    private ModernStructureLoot() {}

    public static Kind kind(ResourceLocation table) {
        return table.getNamespace().equals("minecraft") ? TABLES.get(table.getPath()) : null;
    }
    public static Set<String> tablePaths() { return TABLES.keySet(); }

    /** One optional extra item roll: all existing vanilla pools and their guaranteed rewards remain intact. */
    public static LootPool.Builder bonusPool(Kind kind) {
        var pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
        Item fragment = ThaumcraftContent.MATERIALS.get("knowledge_fragment").get();
        Item salis = ThaumcraftContent.MATERIALS.get("salis_mundus").get();
        return switch (kind) {
            case ANCIENT_CITY -> pool.add(EmptyLootItem.emptyItem().setWeight(42))
                    .add(row(fragment, 5, 1, 2)).add(row(salis, 2, 1, 1))
                    .add(row(ThaumcraftLoot.LOOT_BAG_UNCOMMON.get(), 1, 1, 1));
            case TRIAL_CHEST -> pool.add(EmptyLootItem.emptyItem().setWeight(47))
                    .add(row(fragment, 2, 1, 1)).add(row(salis, 1, 1, 1));
            case VAULT -> pool.add(EmptyLootItem.emptyItem().setWeight(16))
                    .add(row(fragment, 3, 1, 2)).add(row(ThaumcraftLoot.LOOT_BAG_UNCOMMON.get(), 1, 1, 1));
            case OMINOUS_VAULT -> pool.add(EmptyLootItem.emptyItem().setWeight(15))
                    .add(row(fragment, 3, 1, 2)).add(row(ThaumcraftLoot.LOOT_BAG_UNCOMMON.get(), 1, 1, 1))
                    .add(row(ThaumcraftLoot.LOOT_BAG_RARE.get(), 1, 1, 1));
            case ARCHAEOLOGY -> throw new IllegalArgumentException("Archaeology must keep a single native item roll");
        };
    }

    public static LootPoolSingletonContainer.Builder<?> archaeologyEntry() {
        // Brushable blocks expect one selected item. Add a weight-one fragment to their existing
        // first pool instead of appending another roll whose second item would be ignored.
        return row(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get(), 1, 1, 1);
    }

    private static LootPoolSingletonContainer.Builder<?> row(Item item, int weight, int minimum, int maximum) {
        return LootItem.lootTableItem(item).setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(minimum, maximum)));
    }
}
