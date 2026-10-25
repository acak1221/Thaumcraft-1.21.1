package thaumcraft.fabric.testing;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import thaumcraft.common.config.ThaumcraftLoot;

/** Exercises the actual registered Fabric event with every native table-source category. */
public final class LootSourceRegressionChecks {
    private LootSourceRegressionChecks() {}

    public static void run(GameTestHelper test) {
        var lookup = test.getLevel().registryAccess();
        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
        JsonObject expected = null;
        int additions = ThaumcraftLoot.chestAdditions("dungeonChest").size();
        test.assertTrue(additions > 0, "actual registered TC4 chest rows exist");
        for (LootTableSource source : LootTableSource.values()) {
            var builder = table();
            LootTableEvents.MODIFY.invoker().modifyLootTable(key, builder, source, lookup);
            var json = LootTable.DIRECT_CODEC.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE), builder.build())
                    .getOrThrow().getAsJsonObject();
            var pools = json.getAsJsonArray("pools");
            test.assertTrue(pools.size() == 2 && pools.get(0).getAsJsonObject().getAsJsonArray("entries").size() == additions + 1
                            && pools.get(1).getAsJsonObject().getAsJsonArray("entries").size() == 1,
                    "TC4 entries append exactly once to the existing primary weighted pool: " + source);
            if (expected == null) expected = json;
            else test.assertTrue(expected.equals(json), "vanilla, bundled, datapack and replaced tables retain identical rows/counts/weights/functions: " + source);
        }
        var unrelated = table();
        var untouched = LootTable.DIRECT_CODEC.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE), unrelated.build()).getOrThrow();
        LootTableEvents.MODIFY.invoker().modifyLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("other", "chests/simple_dungeon")), unrelated, LootTableSource.DATA_PACK, lookup);
        test.assertTrue(untouched.equals(LootTable.DIRECT_CODEC.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE), unrelated.build()).getOrThrow()),
                "an unrelated namespace is not modified merely because its path matches");
        var empty = LootTable.lootTable();
        LootTableEvents.MODIFY.invoker().modifyLootTable(key, empty, LootTableSource.DATA_PACK, lookup);
        var emptyJson = LootTable.DIRECT_CODEC.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE), empty.build()).getOrThrow().getAsJsonObject();
        test.assertTrue(!emptyJson.has("pools") || emptyJson.getAsJsonArray("pools").isEmpty(),
                "an explicitly empty replacement table stays empty, matching NeoForge's no-primary-pool path");
        test.succeed();
    }

    private static LootTable.Builder table() {
        return LootTable.lootTable().setParamSet(LootContextParamSets.CHEST)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(7)).add(LootItem.lootTableItem(Items.STONE)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(2)).add(LootItem.lootTableItem(Items.COBBLESTONE)));
    }
}
