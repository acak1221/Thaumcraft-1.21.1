package thaumcraft.neoforge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.lib.utils.ChestLootPoolRules;

/**
 * NeoForge half of the original TC4 chest rows.
 *
 * <p>Fabric appends the original rows to the primary pool of the vanilla chest tables through the
 * mutable builder of the Fabric loot API, so the TC4 rows compete with the vanilla rows of the very
 * same weighted list. The 1.21.1 loot structures are immutable and their constructors are package
 * private, so the NeoForge parity rewrites the encoded table of the loader instead: the original
 * rows are appended to the entries of the first pool and the table is decoded again. The result is
 * the same single weighted list as on Fabric.</p>
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

    @SuppressWarnings("unchecked")
    private static void onLootTableLoad(LootTableLoadEvent event) {
        var name = event.getName();
        if (!name.getNamespace().equals("minecraft")) return;
        String category = CATEGORIES.get(name.getPath());
        if (category == null) return;
        var additions = ThaumcraftLoot.chestAdditions(category);
        if (additions.isEmpty()) return;
        var ops = RegistryOps.create(JsonOps.INSTANCE, event.getRegistries());
        JsonElement encoded = LootTable.DIRECT_CODEC.encodeStart(ops, event.getTable()).result().orElse(null);
        if (encoded == null || !encoded.isJsonObject()) return;
        JsonObject root = encoded.getAsJsonObject();
        // The rows are appended to the FIRST pool, exactly like the Fabric
        // modifyPools adapter. A table without pools has nothing to extend.
        JsonObject pool = ChestLootPoolRules.firstPool(root);
        if (pool == null) return;
        if (!pool.has("entries")) return;
        JsonArray entries = pool.getAsJsonArray("entries");
        for (var row : additions) {
            var stack = row.baseStack();
            var entry = LootItem.lootTableItem(stack.getItem()).setWeight(row.weight())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(row.minCount(), row.maxCount())));
            var charge = stack.get(ThaumcraftBaubles.LESSER_VIS.get());
            if (charge != null) entry.apply(SetComponentsFunction.setComponent(ThaumcraftBaubles.LESSER_VIS.get(), charge));
            JsonElement rowJson = LootItem.CODEC.codec().encodeStart(ops, (LootItem) entry.build()).result().orElse(null);
            if (rowJson == null) return;
            entries.add(rowJson);
        }
        var rebuilt = LootTable.DIRECT_CODEC.parse(ops, encoded).result().orElse(null);
        if (rebuilt != null) event.setTable(rebuilt);
    }
}
