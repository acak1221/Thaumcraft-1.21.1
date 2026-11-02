package thaumcraft.common.testing;

import com.mojang.serialization.JsonOps;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.loot.ModernStructureLoot;

/** Loaded Fabric loot events, finite seeded native rolls, and archaeological single-item invariants. */
public final class ModernExplorationLootRuntimeChecks {
    private ModernExplorationLootRuntimeChecks() {}

    public static void loadedStructureAndArchaeologyLoot(GameTestHelper test) {
        var level = test.getLevel();
        var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var fragment = ThaumcraftContent.MATERIALS.get("knowledge_fragment").get();
        for (String path : ModernStructureLoot.tablePaths()) {
            var id = ResourceLocation.withDefaultNamespace(path);
            var key = ResourceKey.create(Registries.LOOT_TABLE, id);
            var table = level.getServer().reloadableRegistries().getLootTable(key);
            var kind = ModernStructureLoot.kind(id);
            var encoded = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
            var pools = encoded.getAsJsonArray("pools");
            test.assertTrue(pools != null && !pools.isEmpty(), "actual loaded vanilla exploration table exists: " + path);
            String json = encoded.toString();
            test.assertTrue(json.contains("thaumcraft:knowledge_fragment") && json.contains("minecraft:"),
                    "actual Fabric adapter retains native entries and adds knowledge: " + path);
            if (kind == ModernStructureLoot.Kind.ARCHAEOLOGY)
                test.assertTrue(pools.size() == 1,
                        "archaeology keeps its native single roll and original finds: " + path);
            else test.assertTrue(pools.get(pools.size() - 1).toString().contains("minecraft:empty"),
                    "modern chest bonus remains optional, not a guaranteed extra reward: " + path);
            var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(test.absolutePos(net.minecraft.core.BlockPos.ZERO)))
                    .create(kind == ModernStructureLoot.Kind.ARCHAEOLOGY ? LootContextParamSets.ARCHAEOLOGY : LootContextParamSets.CHEST);
            boolean foundFragment = false, foundVanilla = false;
            for (int sample = 1; sample <= 512; sample++) {
                var stacks = table.getRandomItems(params, distributedSeed(sample));
                if (kind == ModernStructureLoot.Kind.ARCHAEOLOGY)
                    test.assertTrue(stacks.size() == 1 && stacks.getFirst().getCount() == 1,
                            "native brushing never receives an ignored second drop or oversized reward: " + path);
                for (ItemStack stack : stacks) {
                    foundFragment |= stack.is(fragment);
                    foundVanilla |= net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
                }
                if (foundFragment && foundVanilla && kind != ModernStructureLoot.Kind.ARCHAEOLOGY) break;
            }
            test.assertTrue(foundFragment && foundVanilla, "finite native rolls reach both knowledge and retained vanilla rewards: " + path);
        }
        for (String path : Set.of("chests/trial_chambers/reward_common", "chests/trial_chambers/reward_rare",
                "chests/trial_chambers/reward_ominous_rare", "chests/ancient_city_ice_box")) {
            var id = ResourceLocation.withDefaultNamespace(path);
            test.assertTrue(ModernStructureLoot.kind(id) == null, "nested reward tables are not separately multiplied: " + path);
            var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
            String json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().toString();
            test.assertTrue(!json.contains("thaumcraft:knowledge_fragment"), "non-target child/native tables stay unmodified: " + path);
        }
        test.succeed();
    }

    private static long distributedSeed(int sample) {
        // Adjacent small seeds have correlated first draws in Minecraft's legacy RNG.
        // In the weight-16 cold ocean table, seeds 1..512 never choose entry 15.
        // SplitMix64 spreads the same finite, reproducible sample across the seed space.
        long value = sample * 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
