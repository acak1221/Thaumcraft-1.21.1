package thaumcraft.common.testing;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.world.AuraDimensionRules;
import thaumcraft.common.lib.world.EldritchRingFeature;
import thaumcraft.common.lib.world.NodeEcologyPolicy;
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;
import thaumcraft.common.lib.world.ThaumcraftTotemFeature;
import thaumcraft.common.lib.world.WorldGenHilltopStones;
import thaumcraft.common.lib.world.WorldGenMound;

/** Native spawn/save/effect regressions for follow-up review findings 2, 3 and 14. */
public final class WorldBossFollowupRuntimeChecks {
    private WorldBossFollowupRuntimeChecks() {}

    public static void praetorHasOnePersistentChampion(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            for (int modifier = 0; modifier < ChampionModifier.mods.length; modifier++) {
                var boss = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(lane.level));
                boss.moveTo(lane.origin.getX() + .5, lane.origin.getY(), lane.origin.getZ() + .5, 0F, 0F);
                boss.finalizeSpawn(lane.level, lane.level.getCurrentDifficultyAt(lane.origin), MobSpawnType.EVENT, null);
                test.assertTrue(ChampionHooks.championId(boss) == ChampionHooks.NOT_ROLLED,
                        "finalizeSpawn initializes title/equipment without a second champion lottery");
                ChampionJoinRoll.join(lane.level, boss, modifier);
                test.assertTrue(boss.getChampionModifier() == modifier && ChampionHooks.clientChampionId(boss) == modifier,
                        "name, shared hooks and synchronized particles use the same modifier " + modifier);
                String name = boss.getName().getString();
                test.assertTrue(name.contains(ChampionModifier.mods[modifier].displayName), "name contains actual modifier: " + name);
                double expectedDamage = modifier == ChampionModifier.MIGHTY ? 20D : 5D;
                double expectedSpeed = modifier == ChampionModifier.BOLD ? .32D * 1.3D : .32D;
                test.assertTrue(Math.abs(boss.getAttributeValue(Attributes.ATTACK_DAMAGE) - expectedDamage) < 1e-6
                                && Math.abs(boss.getAttributeValue(Attributes.MOVEMENT_SPEED) - expectedSpeed) < 1e-6,
                        "only the selected modifier changes boss attributes: " + modifier);
                test.assertTrue(boss.getMaxHealth() == 125F, "boss does not receive ordinary champion health bonus");
                if (modifier == ChampionModifier.WARDED)
                    test.assertTrue(boss.getAbsorptionAmount() == 62F, "ward is granted exactly once from base health125");
                boss.originalSpawnEquipment(lane.level, lane.level.getCurrentDifficultyAt(lane.origin));
                test.assertTrue(boss.getChampionModifier() == modifier && boss.getName().getString().contains(ChampionModifier.mods[modifier].displayName),
                        "equipment/title initialization never rerolls an already admitted boss");
                CompoundTag saved = boss.saveWithoutId(new CompoundTag());
                test.assertTrue(saved.contains(ChampionHooks.SAVE_KEY) && saved.getInt(ChampionHooks.SAVE_KEY) == modifier,
                        "canonical champion identity is saved");
                boss.discard();
                var restored = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(lane.level));
                restored.load(saved);
                test.assertTrue(lane.level.addFreshEntity(restored), "saved boss is admitted after unload");
                test.assertTrue(restored.getChampionModifier() == modifier && ChampionHooks.clientChampionId(restored) == modifier,
                        "reload keeps name/effects/particles on the same persisted modifier");
                test.assertTrue(restored.getName().equals(boss.getName()) && restored.getTitle().equals(boss.getTitle()),
                        "reload preserves the complete boss identity");
                test.assertTrue(Math.abs(restored.getAttributeValue(Attributes.ATTACK_DAMAGE) - expectedDamage) < 1e-6,
                        "reload does not apply another attribute promotion");
                if (modifier == ChampionModifier.WARDED)
                    test.assertTrue(restored.getAbsorptionAmount() == 62F, "reload does not grant another ward");
                restored.discard();
            }
            var explicit = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(lane.level));
            EntityUtils.makeChampion(explicit, ChampionModifier.WARP, false);
            test.assertTrue(explicit.getChampionModifier() == ChampionModifier.WARP
                            && explicit.getName().getString().contains("Warped"),
                    "public makeChampion writes the canonical identity before the boss generates its name");

            var bold = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(lane.level));
            bold.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "other_base_speed"), .2,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            ChampionJoinRoll.join(lane.level, bold, ChampionModifier.BOLD);
            test.assertTrue(Math.abs(bold.getAttributeValue(Attributes.MOVEMENT_SPEED) - .32D * 1.5D) < 1e-6,
                    "original Bold operation one adds alongside another base multiplier");
        } finally { lane.clean(); }
        test.succeed();
    }

    public static void restoredPraetorWarpHitsOnce(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var boss = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(lane.level));
            boss.moveTo(lane.origin.getX(), lane.origin.getY(), lane.origin.getZ(), 0F, 0F);
            ChampionJoinRoll.join(lane.level, boss, ChampionModifier.WARP);
            CompoundTag saved = boss.saveWithoutId(new CompoundTag());
            boss.discard();
            var restored = lane.add(ThaumcraftCultists.CULTIST_LEADER.get().create(lane.level));
            restored.load(saved);
            lane.level.addFreshEntity(restored);
            var player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(lane.origin.getX(), lane.origin.getY(), lane.origin.getZ());
            for (int i = 0; i < 61; i++) player.tick();
            int before = KnowledgeAccess.get(player).getWarpTemp();
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredWarpEffect(lane.level, .1F, 1,
                    () -> test.assertTrue(restored.doHurtTarget(player), "restored boss lands a real melee hit"));
            test.assertTrue(KnowledgeAccess.get(player).getWarpTemp() == before + 2,
                    "one accepted melee hit applies exactly the shared Warp effect after reload");
        } finally { lane.clean(); }
        test.succeed();
    }

    public static void generationRespectsDimensionPolicies(GameTestHelper test) {
        var saved = new HashMap<>(NodeEcologyPolicy.dimensionBlacklist);
        var biomes = new HashMap<>(NodeEcologyPolicy.biomeBlacklist);
        try {
            ServerLevel overworld = test.getLevel().getServer().overworld();
            WorldGenLevel region = policyOnlyRegion(overworld);
            for (int level : new int[]{0, 1, 2}) {
                ThaumcraftApi.addDimBlacklist(Level.OVERWORLD, level);
                test.assertTrue(ThaumcraftApi.getDimBlacklist(Level.OVERWORLD) == level, "public dimension registration reaches live policy");
                assertStructuresRefused(test, region);
                int[] draws = {0};
                RandomSource random = new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
                    @Override public int nextInt(int bound) { draws[0]++; return 1; }
                };
                if (level != 1)
                    test.assertTrue(!ThaumcraftNormalNodeFeature.placeAuraPass(region, random, 0, 0), "blocked complete aura pass never reaches scattered temples");
                test.assertTrue(!ThaumcraftNormalNodeFeature.placeWildNode(region, random, 0, 0, false), "missed or blocked wild-node roll places nothing");
                test.assertTrue(draws[0] == (level == 1 ? 1 : 0), "surface aura blacklist is checked before RNG; level1 keeps the pass");
                test.assertTrue(AuraDimensionRules.supportsWildNodes(Level.NETHER), "original Nether aura bypasses surface blacklist");
            }
            NodeEcologyPolicy.dimensionBlacklist.remove(Level.OVERWORLD);
            var biome = overworld.getBiome(test.absolutePos(new BlockPos(3, 2, 3))).unwrapKey().orElseThrow();
            ThaumcraftApi.addBiomeBlacklist(biome, 0);
            test.assertTrue(ThaumcraftApi.getBiomeBlacklist(biome) == 0 && AuraDimensionRules.supportsWildNodes(Level.OVERWORLD),
                    "original biome blacklist is public and does not suppress aura nodes");
            test.assertTrue(ThaumcraftTotemFeature.permitsStructures(region), "unlisted overworld keeps the shared structure chain");
            treePassReadsPublicBiomePolicy(test);
            ThaumcraftApi.addDimBlacklist(Level.NETHER, 0);
            test.assertTrue(AuraDimensionRules.supportsWildNodes(Level.NETHER), "Nether blacklist preserves original independent aura behavior");
            for (var key : java.util.List.of(Level.NETHER, Level.END,
                    thaumcraft.common.lib.world.OuterLandsProvider.DIMENSION)) {
                ServerLevel other = test.getLevel().getServer().getLevel(key);
                test.assertTrue(other != null, "fixture dimension is loaded: " + key);
                assertStructuresRefused(test, policyOnlyRegion(other));
            }
        } finally {
            NodeEcologyPolicy.dimensionBlacklist.clear(); NodeEcologyPolicy.dimensionBlacklist.putAll(saved);
            NodeEcologyPolicy.biomeBlacklist.clear(); NodeEcologyPolicy.biomeBlacklist.putAll(biomes);
        }
        test.succeed();
    }

    /** Actual vegetation pass receives a real registry biome and loaded native non-flat dimension. */
    private static void treePassReadsPublicBiomePolicy(GameTestHelper test) {
        var naturalKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        ServerLevel natural = test.getLevel().getServer().getLevel(naturalKey);
        test.assertTrue(natural != null, "existing natural generation dimension is loaded");
        var holder = natural.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.FOREST);
        WorldGenLevel region = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> natural;
                    case "dimensionType" -> natural.dimensionType();
                    case "getMinBuildHeight" -> natural.getMinBuildHeight();
                    case "getMaxBuildHeight" -> natural.getMaxBuildHeight();
                    case "hasChunkAt" -> true;
                    case "getHeight" -> 64;
                    case "getBiome" -> holder;
                    default -> throw new AssertionError("Vegetation policy fixture must not access/write terrain: " + method.getName());
                });
        for (int blacklist : new int[]{-1, 0, 1, 2, 17}) {
            ThaumcraftApi.addBiomeBlacklist(net.minecraft.world.level.biome.Biomes.FOREST, blacklist);
            int[] draws = {0};
            RandomSource random = new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
                @Override public int nextInt(int bound) { draws[0]++; return 1; }
            };
            test.assertTrue(!thaumcraft.common.lib.world.ThaumcraftTreeFeature.placePass(region, random, 0, 0),
                    "declared non-growth rolls never create a tree");
            test.assertTrue(draws[0] == (blacklist == -1 ? 4 : 0),
                    "all registered original biome blacklist levels suppress the actual vegetation pass before RNG: " + blacklist);
        }
    }

    private static WorldGenLevel policyOnlyRegion(ServerLevel level) {
        return (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return level;
                    if (method.getName().equals("dimensionType")) return level.dimensionType();
                    throw new AssertionError("Rejected world generation must not read/write terrain: " + method.getName());
                });
    }

    private static void assertStructuresRefused(GameTestHelper test, WorldGenLevel region) {
        RandomSource random = new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
            @Override public int nextInt(int bound) { throw new AssertionError("Rejected structures must not consume RNG"); }
        };
        test.assertTrue(!ThaumcraftTotemFeature.permitsStructures(region), "complete exclusive structure chain is denied");
        test.assertTrue(!new WorldGenMound().placeSelected(region, random,
                new StructurePlacementRules.Draw(StructurePlacementRules.Kind.MOUND, 0, 0)), "mound selected entrypoint respects dimension");
        test.assertTrue(!new WorldGenHilltopStones().placeSelected(region, random,
                new StructurePlacementRules.Draw(StructurePlacementRules.Kind.HILLTOP_STONES, 0, 0), 0, 0), "hilltop selected entrypoint respects dimension");
        test.assertTrue(!new EldritchRingFeature().placeSelected(region, random,
                new StructurePlacementRules.Draw(StructurePlacementRules.Kind.ELDRITCH_RING, 0, 0), 0, 0), "ring selected entrypoint respects dimension");
        test.assertTrue(!WorldGenMound.preflight(region, 0, 0) && !WorldGenHilltopStones.preflight(region, 0, 0)
                && !EldritchRingFeature.preflight(region, 0, 0), "preflights agree with placement");
        test.assertTrue(!ThaumcraftTotemFeature.placePass(region, random, 0, 0), "totem cannot bypass the shared dimension gate");
    }
}
