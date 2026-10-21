package thaumcraft.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftLoot;

/**
 * NeoForge half of the original TC4 chest rows.
 *
 * <p>Fabric appends the original rows to the primary pool of the vanilla chest tables through the
 * mutable builder of the Fabric loot API, so the TC4 rows compete with the vanilla rows of the very
 * same weighted list. The 1.21.1 loot structures are immutable, so the NeoForge parity appends the
 * original rows to the rows of the first pool of the loaded table itself. The rows of the pool keep
 * their identity, their order and their weight, and the appended rows are the very objects the
 * Fabric builder produces, so the weighted list and the draw order of the chest are the same on
 * both loaders.</p>
 *
 * <p>The earlier implementation of this adapter rebuilt the table from its encoded form. That
 * cannot work for the vanilla chest tables: the {@code minecraft:enchant_randomly} function of the
 * dungeon tables writes the list of its enchantments, and the enchantment registry is a dynamic
 * registry that the loot table loader does not have, so the encoding of the table failed. Even for
 * the tables without that function the rebuild failed, because the rows of a pool are decoded with
 * the dispatch codec of the entry type and the plain map codec of the loot item does not write the
 * type field of the row.</p>
 */
final class NeoForgeLootTables {
    private static final Map<String, String> CATEGORIES = Map.ofEntries(
            Map.entry("chests/simple_dungeon", "dungeonChest"),
            Map.entry("chests/desert_pyramid", "pyramidDesertyChest"),
            Map.entry("chests/jungle_temple", "pyramidJungleChest"),
            Map.entry("chests/abandoned_mineshaft", "mineshaftCorridor"),
            Map.entry("chests/stronghold_corridor", "strongholdCorridor"),
            Map.entry("chests/stronghold_crossing", "strongholdCrossing"),
            Map.entry("chests/stronghold_library", "strongholdLibrary"),
            Map.entry("chests/village/village_weaponsmith", "villageBlacksmith"),
            Map.entry("chests/village/village_toolsmith", "villageBlacksmith"),
            Map.entry("chests/village/village_armorer", "villageBlacksmith"));

    private NeoForgeLootTables() {}

    static void register(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(NeoForgeLootTables::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        var name = event.getName();
        if (!name.getNamespace().equals("minecraft")) return;
        String category = CATEGORIES.get(name.getPath());
        if (category == null) return;
        var additions = ThaumcraftLoot.chestAdditions(category);
        if (additions.isEmpty()) return;
        var pools = ((NeoForgeLootTableAccess) event.getTable()).thaumcraft$pools();
        if (pools == null || pools.isEmpty()) return;
        List<LootPoolEntryContainer> rows = new ArrayList<>();
        for (var row : additions) {
            var stack = row.baseStack();
            var entry = LootItem.lootTableItem(stack.getItem()).setWeight(row.weight())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(row.minCount(), row.maxCount())));
            var charge = stack.get(ThaumcraftBaubles.LESSER_VIS.get());
            if (charge != null) entry.apply(SetComponentsFunction.setComponent(ThaumcraftBaubles.LESSER_VIS.get(), charge));
            rows.add((LootPoolEntryContainer) entry.build());
        }
        // The rows are appended to the FIRST pool, exactly like the Fabric
        // modifyPools adapter: the TC4 rows compete with the vanilla rows of
        // the same weighted list instead of guaranteeing extra loot.
        ((NeoForgeLootPoolAccess) pools.getFirst()).thaumcraft$appendEntries(rows);
    }
}
