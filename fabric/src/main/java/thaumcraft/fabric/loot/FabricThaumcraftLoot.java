package thaumcraft.fabric.loot;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftLoot;

/** Native chest adapter: one existing weighted pool, retaining its vanilla rolls/functions. */
public final class FabricThaumcraftLoot {
    private static final Map<String,String> CATEGORIES = Map.ofEntries(
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
    private FabricThaumcraftLoot() {}

    public static void init() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!source.isBuiltin() || !key.location().getNamespace().equals("minecraft")) return;
            String category = CATEGORIES.get(key.location().getPath());
            if (category == null) return;
            var additions = ThaumcraftLoot.chestAdditions(category);
            var first = new AtomicBoolean(true);
            ((FabricLootTableBuilder)table).modifyPools(pool -> {
                // Original ChestGenHooks had one weighted list. Modern structures have
                // several specialized pools; append once to their primary pool so TC4
                // competes with its existing entries, rather than guaranteeing extra loot.
                if (!first.getAndSet(false)) return;
                for (var row : additions) {
                    var stack = row.baseStack();
                    var entry = LootItem.lootTableItem(stack.getItem()).setWeight(row.weight())
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(row.minCount(), row.maxCount())));
                    var charge = stack.get(ThaumcraftBaubles.LESSER_VIS.get());
                    if (charge != null) entry.apply(SetComponentsFunction.setComponent(ThaumcraftBaubles.LESSER_VIS.get(), charge));
                    pool.add(entry);
                }
            });
        });
    }
}
