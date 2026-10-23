package thaumcraft.common.testing;

import java.util.Optional;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.EldritchRingFeature;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.lib.world.ThaumcraftBiomeDecorationFeature;

/** Native FEATURES regions with declared missing-registry / malformed-save inputs. */
public final class WorldgenCrashRuntimeChecks {
    private record Quart(int x, int y, int z) {}
    private WorldgenCrashRuntimeChecks() {}

    public static void missingBiomeColumn(GameTestHelper test) { missingBiome(test, true); }
    public static void missingBiomeFeature(GameTestHelper test) { missingBiome(test, false); }

    private static void missingBiome(GameTestHelper test, boolean conversion) {
        ServerLevel level = test.getLevel();
        var source = level.registryAccess().registryOrThrow(Registries.BIOME);
        var filtered = new MappedRegistry<Biome>(Registries.BIOME, Lifecycle.stable());
        source.entrySet().forEach(entry -> {
            if (!entry.getKey().equals(ThaumcraftBiomes.TAINT))
                filtered.register(entry.getKey(), entry.getValue(), RegistrationInfo.BUILT_IN);
        });
        filtered.freeze();
        var registries = new java.util.ArrayList<Registry<?>>();
        level.registryAccess().registries().forEach(entry -> registries.add(
                entry.key().equals(Registries.BIOME) ? filtered : entry.value()));
        RegistryAccess access = new RegistryAccess.ImmutableRegistryAccess(registries);
        ChunkPos centre = new ChunkPos(520, 520);
        var chunk = level.getChunk(centre.x, centre.z);
        var saved = new java.util.HashMap<Quart, Holder<Biome>>();
        for (int x = centre.x * 4; x < centre.x * 4 + 4; x++)
            for (int z = centre.z * 4; z < centre.z * 4 + 4; z++)
                for (int y = QuartPos.fromBlock(chunk.getMinBuildHeight()); y < QuartPos.fromBlock(chunk.getMaxBuildHeight()); y++)
                    saved.put(new Quart(x, y, z), chunk.getNoiseBiome(x, y, z));
        var before = chunk.getNoiseBiome(centre.x * 4, 15, centre.z * 4);
        int[] writes = {0};
        var region = region(level, centre, access, writes);
        if (conversion) {
            test.assertTrue(!BiomeColumns.set(region, centre.getMinBlockX(), centre.getMinBlockZ(), ThaumcraftBiomes.TAINT),
                    "absent target biome refuses native region conversion instead of throwing");
            test.assertTrue(chunk.getNoiseBiome(centre.x * 4, 15, centre.z * 4).equals(before), "failed lookup preserves biome storage");
            var missing = net.minecraft.resources.ResourceKey.create(Registries.BIOME,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "absent_worldgen_crash_test"));
            test.assertTrue(!BiomeColumns.set(level, centre.getMinBlockX(), centre.getMinBlockZ(), missing),
                    "live ServerLevel equivalent also refuses an absent biome rather than throwing");
            test.succeed();
            return;
        }
        // The centre selects taint decoration, while the registry deliberately lacks its conversion target.
        var taint = source.getHolderOrThrow(ThaumcraftBiomes.TAINT);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> x == centre.x * 4 + 2 && z == centre.z * 4 + 2 ? taint : before,
                level.getChunkSource().randomState().sampler());
        try (var log = new VanillaLogCapture()) {
            test.assertTrue(!new ThaumcraftBiomeDecorationFeature().place(context(region, centre)),
                    "taint feature refuses missing conversion resource before any decoration writes");
            test.assertTrue(writes[0] == 0, "missing biome feature must not leave partial block writes");
            test.assertTrue(log.containing("Missing native biome thaumcraft:taint").size() == 1,
                    "missing biome refusal logs the resource once per feature attempt");
            test.assertTrue(log.containing("Missing native biome thaumcraft:taint").getFirst().contains("8320"),
                    "missing biome diagnostic includes generation position");
        } finally {
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> saved.get(new Quart(x, y, z)), level.getChunkSource().randomState().sampler());
            chunk.setUnsaved(true);
        }
        test.succeed();
    }

    public static void malformedMazeRooms(GameTestHelper test) { malformedMaze(test, false); }
    public static void malformedMazeRing(GameTestHelper test) { malformedMaze(test, true); }

    private static void malformedMaze(GameTestHelper test, boolean ring) {
        ServerLevel level = test.getLevel();
        var original = OuterLandsProvider.maze(level);
        var input = new CompoundTag();
        input.putString("cells", "unread labyrinth");
        input.putString("keep", "for offline repair");
        var quarantined = readMalformedMap(level, input, ring);
        var storage = level.getServer().overworld().getDataStorage();
        ChunkPos centre = new ChunkPos(ring ? 524 : 522, 520);
        int[] writes = {0};
        var region = region(level, centre, level.registryAccess(), writes);
        storage.set(OuterLandsProvider.MazeSavedData.ID, quarantined);
        try (var log = new VanillaLogCapture()) {
            boolean placed;
            if (ring) {
                // Supplied valid terrain: stone to Y59 and air above, below sea-level gate.
                placed = new EldritchRingFeature().placeSelected(region, RandomSource.create(42),
                        new StructurePlacementRules.Draw(StructurePlacementRules.Kind.ELDRITCH_RING,
                                centre.getMinBlockX() + 8, centre.getMinBlockZ() + 8), centre.x, centre.z);
                test.assertTrue(!new EldritchRingFeature().placeSelected(region, RandomSource.create(42),
                        new StructurePlacementRules.Draw(StructurePlacementRules.Kind.ELDRITCH_RING,
                                centre.getMinBlockX() + 8, centre.getMinBlockZ() + 8), centre.x, centre.z),
                        "repeated quarantined ring placement also refuses");
            } else {
                placed = new OuterLandsProvider.OuterLandsMazeFeature().place(context(region, centre));
                test.assertTrue(!new OuterLandsProvider.OuterLandsMazeFeature().place(context(region, centre)),
                        "repeated quarantined room placement also refuses");
            }
            test.assertTrue(!placed, "quarantined maze makes " + (ring ? "ring" : "room") + " feature skip cleanly");
            test.assertTrue(writes[0] == 0, "quarantined map must prevent all chunk writes");
            test.assertTrue(log.containing("Skipping maze generation at").size() == 1,
                    "quarantined map logs once even across repeated feature attempts");
            String diagnostic = log.containing("Skipping maze generation at").getFirst();
            test.assertTrue(diagnostic.contains("cells must be a compound list") && diagnostic.contains(ring ? "z=8328" : "z=8320"),
                    "quarantine diagnostic retains original rejection reason and generation position");
        } finally {
            storage.set(OuterLandsProvider.MazeSavedData.ID, original);
            test.assertTrue(input.equals(quarantined.save(new CompoundTag(), level.registryAccess())),
                    "generation preserves rejected maze NBT exactly, including unknown fields");
            test.assertTrue(!quarantined.isDirty(), "generation does not dirty or replace quarantined payload");
        }
        test.succeed();
    }

    private static OuterLandsProvider.MazeSavedData readMalformedMap(ServerLevel level, CompoundTag input, boolean ring) {
        try {
            var directory = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("c5fix-malformed-" + (ring ? "ring" : "rooms"));
            java.nio.file.Files.createDirectories(directory);
            var root = new CompoundTag(); root.put("data", input.copy());
            root.putInt("DataVersion", net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            net.minecraft.nbt.NbtIo.writeCompressed(root, directory.resolve(OuterLandsProvider.MazeSavedData.ID + ".dat"));
            var disk = new net.minecraft.world.level.storage.DimensionDataStorage(directory.toFile(),
                    level.getServer().getFixerUpper(), level.registryAccess());
            var factory = new net.minecraft.world.level.saveddata.SavedData.Factory<>(OuterLandsProvider.MazeSavedData::new,
                    OuterLandsProvider.MazeSavedData::load, net.minecraft.util.datafix.DataFixTypes.LEVEL);
            return java.util.Objects.requireNonNull(disk.get(factory, OuterLandsProvider.MazeSavedData.ID));
        } catch (java.io.IOException failure) {
            throw new AssertionError("could not write malformed SavedData fixture", failure);
        }
    }

    private static FeaturePlaceContext<NoneFeatureConfiguration> context(WorldGenRegion region, ChunkPos centre) {
        return new FeaturePlaceContext<>(Optional.empty(), region, region.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(42), new BlockPos(centre.getMinBlockX(), 60, centre.getMinBlockZ()), NoneFeatureConfiguration.INSTANCE);
    }

    private static WorldGenRegion region(ServerLevel level, ChunkPos centre, RegistryAccess access, int[] writes) {
        var chunk = level.getChunk(centre.x, centre.z);
        return new WorldGenRegion(level, null, ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), chunk) {
            @Override public RegistryAccess registryAccess() { return access; }
            @Override public boolean hasChunk(int x, int z) { return x == centre.x && z == centre.z; }
            @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean load) {
                if (!hasChunk(x, z)) throw new AssertionError("unexpected external chunk lookup");
                return chunk;
            }
            @Override public int getHeight(net.minecraft.world.level.levelgen.Heightmap.Types type, int x, int z) { return 60; }
            @Override public BlockState getBlockState(BlockPos pos) { return pos.getY() < 60 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(); }
            @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int depth) { writes[0]++; return true; }
        };
    }
}
