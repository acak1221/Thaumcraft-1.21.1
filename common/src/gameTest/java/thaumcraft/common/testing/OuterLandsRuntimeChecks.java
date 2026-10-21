package thaumcraft.common.testing;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.OuterLandsRules;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageWizardManager;
import thaumcraft.common.lib.world.VillageWorldGenRules;
import thaumcraft.common.lib.world.EldritchRingFeature;
import thaumcraft.common.lib.world.WorldGenHilltopStones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.OuterLandsDimension;
import thaumcraft.common.lib.world.OuterLandsDimensionRules;
import thaumcraft.common.lib.world.StructurePlacementRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import thaumcraft.common.lib.world.OuterLandsBiomes;

/**
 * The original TC4 Outer Lands and village components inside a real dedicated server: the
 * registered dimension, dimension type and Eldritch biome with their original values, the
 * labyrinth cell map the running server keeps in its saved data, the original labyrinth layout
 * of the maze generator and the original village weights, house layouts, trade tables and the
 * two surface structures of the hilltop stones and the eldritch ring.
 */
public final class OuterLandsRuntimeChecks {
    private OuterLandsRuntimeChecks() {}

    /** The registered Outer Lands dimension, dimension type and Eldritch biome of the server. */
    public static void dimensionAndBiomeUseTheOriginalValues(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            var access = level.getServer().registryAccess();
            var types = access.registryOrThrow(Registries.DIMENSION_TYPE);
            var biomes = access.registryOrThrow(Registries.BIOME);
            test.assertTrue(types.containsKey(OuterLandsProvider.DIMENSION_TYPE),
                    "the server registered the original Outer Lands dimension type");
            var type = types.get(OuterLandsProvider.DIMENSION_TYPE);
            test.assertTrue(!type.hasSkyLight() && type.fixedTime().isPresent() && type.fixedTime().getAsLong() == 0L
                            && type.ambientLight() == 0.0F
                            && type.minY() == OuterLandsRules.MIN_Y && type.height() == OuterLandsRules.HEIGHT
                            && type.logicalHeight() == OuterLandsRules.LOGICAL_HEIGHT,
                    "the Outer Lands keep the original no sky light, fixed time zero, no ambient light and 256 block height: "
                            + type.hasSkyLight() + " " + type.fixedTime() + " " + type.ambientLight() + " " + type.height());
            var biome = biomes.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "eldritch_lands"));
            test.assertTrue(biome != null, "the server registered the original Eldritch biome");
            test.assertTrue(!biome.hasPrecipitation()
                            && Math.abs(biome.getBaseTemperature() - OuterLandsRules.BIOME_TEMPERATURE) < 1e-6F
                            && biome.getMobSettings().getCreatureProbability() == OuterLandsRules.CREATURE_SPAWN_PROBABILITY,
                    "the Eldritch biome keeps the original no precipitation, half temperature and tenth creature chance: "
                            + biome.hasPrecipitation() + " " + biome.getBaseTemperature() + " "
                            + biome.getMobSettings().getCreatureProbability());
            var monsters = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            test.assertTrue(monsters.size() == 2 && monsters.stream().allMatch(entry -> entry.getWeight().asInt() == 1
                            && entry.minCount == 1 && entry.maxCount == 1),
                    "the Eldritch biome spawns the original two monsters with weight one: " + monsters.size());
            test.assertTrue(biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().isEmpty(),
                    "the Eldritch biome has no passive spawns");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original labyrinth layout of the Outer Lands maze. */
    public static void mazeUsesTheOriginalCellLayout(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var generator = new MazeGenerator(21, 21, 20251020L);
            test.assertTrue(generator.generate(), "the labyrinth generator completes the original layout");
            int portal = 0;
            int boss = 0;
            int negative = 0;
            int keyRoom = 0;
            for (int y = 0; y < generator.height(); y++)
                for (int x = 0; x < generator.width(); x++) {
                    int cell = generator.grid[y][x];
                    if (cell < 0) negative++;
                    int feature = (cell >> 8) & 0xFF;
                    if (feature == OuterLandsRules.FEATURE_PORTAL) portal++;
                    if (OuterLandsRules.isBossQuadrant(feature)) boss++;
                    if (feature == OuterLandsRules.FEATURE_KEY_ROOM) keyRoom++;
                }
            test.assertTrue(negative == 0, "the labyrinth never keeps a negative cell: " + negative);
            test.assertTrue(portal == 1, "the labyrinth has the original single portal cell: " + portal);
            test.assertTrue(boss == 4, "the labyrinth has the original four boss room quadrants: " + boss);
            test.assertTrue(keyRoom == 1, "the labyrinth has the original single key room: " + keyRoom);
            test.assertTrue(OuterLandsRules.roomForFeature(OuterLandsRules.FEATURE_PORTAL) == OuterLandsRules.RoomKind.PORTAL
                            && OuterLandsRules.roomForFeature(OuterLandsRules.FEATURE_BOSS_UPPER_LEFT) == OuterLandsRules.RoomKind.BOSS
                            && OuterLandsRules.roomForFeature(OuterLandsRules.FEATURE_KEY_ROOM) == OuterLandsRules.RoomKind.KEY_ROOM
                            && OuterLandsRules.roomForFeature(OuterLandsRules.FEATURE_NEST_ROOM) == OuterLandsRules.RoomKind.NEST_ROOM
                            && OuterLandsRules.roomForFeature(OuterLandsRules.FEATURE_LIBRARY_ROOM) == OuterLandsRules.RoomKind.LIBRARY_ROOM
                            && OuterLandsRules.roomForFeature(OuterLandsRules.FEATURE_PASSAGE) == OuterLandsRules.RoomKind.PASSAGE,
                    "the original feature to room dispatch of the oracle is kept");
            test.assertTrue(OuterLandsRules.keyRoomGuardians(0) == 2 && OuterLandsRules.keyRoomGuardians(2) == 3
                            && OuterLandsRules.keyRoomGuardians(3) == 4,
                    "the key room keeps the original guardian count per difficulty");
            test.assertTrue(OuterLandsRules.keyRoomChampion(3, 0) && !OuterLandsRules.keyRoomChampion(3, 1)
                            && !OuterLandsRules.keyRoomChampion(2, 0),
                    "only the first guardian of a hard key room is a champion");
            // The same seed always produces the same labyrinth.
            var twin = new MazeGenerator(21, 21, 20251020L);
            twin.generate();
            boolean same = true;
            for (int y = 0; y < generator.height() && same; y++)
                for (int x = 0; x < generator.width(); x++)
                    if (generator.grid[y][x] != twin.grid[y][x]) { same = false; break; }
            test.assertTrue(same, "the same seed always produces the same original labyrinth");
            // The running server keeps the labyrinth cells in its own saved data.
            var maze = OuterLandsProvider.maze(lane.level);
            maze.storeLabyrinth(generator, 0, 0);
            boolean stored = false;
            for (int chunkX = -64; chunkX <= 64 && !stored; chunkX++)
                for (int chunkZ = -64; chunkZ <= 64 && !stored; chunkZ++)
                    if (maze.cell(chunkX, chunkZ) != 0) stored = true;
            test.assertTrue(stored, "the server stores the labyrinth cells in its saved data");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original village components, trade tables and surface structures of the server. */
    public static void villageComponentsUseTheOriginalLayout(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            var features = level.getServer().registryAccess().registryOrThrow(Registries.FEATURE);
            // The eldritch ring is registered as thaumcraft:tc4_eldritch_ring (ThaumcraftOuterLands:76).
            for (String id : new String[]{"village_banker_homes", "village_wizard_towers", "hilltop_stones", "tc4_eldritch_ring"}) {
                test.assertTrue(features.containsKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", id)),
                        "the server registered the original village feature " + id);
            }
            test.assertTrue(VillageBankerManager.PIECE_WEIGHT == VillageWorldGenRules.BANKER_HOME_WEIGHT
                            && VillageWizardManager.PIECE_WEIGHT == VillageWorldGenRules.WIZARD_TOWER_WEIGHT
                            && VillageWorldGenRules.TOTAL_HOUSE_WEIGHT == 100,
                    "the village keeps the original piece weights of 25 and 15: " + VillageBankerManager.PIECE_WEIGHT + " "
                            + VillageWizardManager.PIECE_WEIGHT + " " + VillageWorldGenRules.TOTAL_HOUSE_WEIGHT);
            test.assertTrue(VillageBankerManager.PROFESSION_ID == 191 && VillageWizardManager.PROFESSION_ID == 190,
                    "the village keeps the original profession ids of the banker and the wizard");
            var banker = VillageWorldGenRules.bankerHome(true, 1);
            var wizard = VillageWorldGenRules.wizardTower();
            test.assertTrue(banker.size() > 100 && wizard.size() > 200,
                    "the original banker home and wizard tower keep their block lists: " + banker.size() + " " + wizard.size());
            test.assertTrue(banker.stream().anyMatch(place -> place.material() == VillageWorldGenRules.Material.DOOR_BOTTOM)
                            && banker.stream().anyMatch(place -> place.material() == VillageWorldGenRules.Material.IRON_BARS)
                            && wizard.stream().anyMatch(place -> place.material() == VillageWorldGenRules.Material.CHEST)
                            && wizard.stream().anyMatch(place -> place.material() == VillageWorldGenRules.Material.GLOWSTONE),
                    "the banker home keeps its door and its bars and the wizard tower its chest and its glowstone");
            test.assertTrue(VillageBankerManager.trades().size() == 12 && VillageWizardManager.trades().size() == 15,
                    "the village keeps the original twelve banker trades and fifteen wizard trades: "
                            + VillageBankerManager.trades().size() + " " + VillageWizardManager.trades().size());
            test.assertTrue(VillageWorldGenRules.towerChestTotalWeight() == 81
                            && VillageWorldGenRules.towerChestItemCount(0) == 4 && VillageWorldGenRules.towerChestItemCount(5) == 9,
                    "the tower chest keeps the original weight of 81 and four to nine items: "
                            + VillageWorldGenRules.towerChestTotalWeight());
            // The original surface structures: one chunk in forty carries the hilltop stones and
            // one chunk in sixty six carries the eldritch ring.
            test.assertTrue(VillageWorldGenRules.HILLTOP_STONE_CHANCE == 40
                            && VillageWorldGenRules.ELDRITCH_RING_CHANCE == 66
                            && VillageWorldGenRules.eldritchRingSize(0) == 11 && VillageWorldGenRules.eldritchRingSize(5) == 21,
                    "the surface structures keep the original chances and the original ring size: "
                            + VillageWorldGenRules.eldritchRingSize(5));
            test.assertTrue(VillageWorldGenRules.isHilltopCorner(3, 3) && !VillageWorldGenRules.isHilltopCorner(3, 1)
                            && VillageWorldGenRules.isHilltopPillar(3, 1) && !VillageWorldGenRules.isHilltopPillar(2, 2),
                    "the hilltop stones keep the original footprint with its skipped corners");
            test.assertTrue(VillageWorldGenRules.isEldritchCorner(3, 3) && VillageWorldGenRules.isEldritchRingPosition(3, 1)
                            && !VillageWorldGenRules.isEldritchRingPosition(3, 3) && !VillageWorldGenRules.isEldritchRingPosition(2, 2),
                    "the eldritch ring keeps the original footprint with its skipped corners and diagonal");
            test.assertTrue(VillageWorldGenRules.eldritchArmsAltar(3) && !VillageWorldGenRules.eldritchArmsAltar(0)
                            && VillageWorldGenRules.eldritchAltarSpawnerTypeOne(6),
                    "the eldritch ring keeps the original altar rolls");
            // The village plan of a chunk is the same for every chunk of the same cell.
            var plan = VillageWorldGenRules.villagePlan(level.getServer().overworld().getSeed(), 5, 7);
            var twin = VillageWorldGenRules.villagePlan(level.getServer().overworld().getSeed(), 5, 7);
            test.assertTrue(plan.size() >= 1 && plan.size() <= VillageWorldGenRules.VILLAGE_HOUSE_SLOTS
                            && plan.houses().equals(twin.houses()),
                    "the village plan of a chunk is deterministic: " + plan.size());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The chunk generation of the Outer Lands builds the labyrinth of the eldritch ring. */
    public static void labyrinth(GameTestHelper test) {
        ServerLevel outer = OuterLandsDimension.level(test.getLevel().getServer());
        test.assertTrue(outer != null, "the Outer Lands level exists in the running server");
        int[] room = seedLabyrinth(outer, 512, 512);
        // The shell floor of a room sits one block below the room base.
        test.assertTrue(outer.getBlockState(new BlockPos(room[0], OuterLandsRules.MAZE_FLOOR_Y - 1, room[1])).is(Blocks.BEDROCK),
                "the labyrinth room floor is generated in the Outer Lands");
        test.assertTrue(outer.getBlockState(new BlockPos(room[0], OuterLandsRules.MAZE_FLOOR_Y + 3, room[1]))
                .is(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()), "the eldritch gate of the labyrinth portal room is generated");
        test.assertTrue(outer.getBlockState(new BlockPos(room[0], OuterLandsRules.MAZE_FLOOR_Y + 2, room[1]))
                .is(ThaumcraftEldritchBlocks.ELDRITCH.get()), "the obelisk base of the labyrinth portal room is generated");
        test.succeed();
    }

    /**
     * Generating a labyrinth room of the Outer Lands places thousands of cosmetic stones, eldritch blocks and
     * eldritch nothing, most of them variants without a block entity, and used to write one "Tried to load a
     * block entity ... but failed" warning for each of them.
     */
    public static void generationLogsNoBlockEntityWarnings(GameTestHelper test) {
        ServerLevel outer = OuterLandsDimension.level(test.getLevel().getServer());
        test.assertTrue(outer != null, "the Outer Lands level exists in the running server");
        VanillaLogCapture log = new VanillaLogCapture();
        try {
            int[] room = seedLabyrinth(outer, 768, 768);
            test.assertTrue(outer.getBlockState(new BlockPos(room[0], OuterLandsRules.MAZE_FLOOR_Y + 3, room[1]))
                    .is(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()), "the labyrinth portal room was really generated by this check");
        } catch (RuntimeException failure) {
            log.close();
            throw failure;
        }
        // Block entities of a freshly generated chunk are promoted over the next ticks.
        test.runAfterDelay(40, () -> {
            try {
                test.assertTrue(log.containing("Tried to load a block entity").isEmpty(),
                        "generating the labyrinth room writes no block entity warning, " + log.sample("Tried to load a block entity"));
            } finally {
                log.close();
            }
            test.succeed();
        });
    }

    public static void portal(GameTestHelper test) {
        ServerLevel overworld = test.getLevel();
        ServerLevel outer = OuterLandsDimension.level(overworld.getServer());
        test.assertTrue(outer != null, "the Outer Lands level exists in the running server");
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        BlockPos overGate = player.blockPosition();
        gate(overworld, overGate);
        // The gate of the Outer Lands stands on the floor of a labyrinth room.
        int gateY = OuterLandsRules.MAZE_FLOOR_Y + 3;
        BlockPos outerGate = new BlockPos(overGate.getX(), gateY, overGate.getZ());
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                outer.setBlock(new BlockPos(outerGate.getX() + dx, OuterLandsRules.MAZE_FLOOR_Y - 1, outerGate.getZ() + dz),
                        Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        gate(outer, outerGate);
        // The gate scans every fifth tick (oracle TileEldritchPortal:63).
        test.runAfterDelay(20, () -> {
            test.assertTrue(player.level() == outer, "the eldritch gate moved the player into the Outer Lands");
            test.assertTrue(player.isAlive() && !player.isDeadOrDying(), "the player survived the transfer into the Outer Lands");
            test.assertTrue(Math.abs(player.getX() - (outerGate.getX() + .5D)) <= 1.5D
                    && Math.abs(player.getZ() - (outerGate.getZ() + .5D)) <= 1.5D, "the player lands one block beside the gate");
            test.assertTrue(player.getY() == OuterLandsDimensionRules.gateLandingHeight(gateY),
                    "the player lands at the height of the gate");
            // The gate of the Outer Lands stands on the floor of a labyrinth room, so the player
            // never falls into the void of the Outer Lands.
            test.assertTrue(player.getY() >= OuterLandsRules.MAZE_FLOOR_Y, "the player does not fall out of the world");
            test.assertTrue(!outer.getBlockState(new BlockPos(outerGate.getX(), OuterLandsRules.MAZE_FLOOR_Y - 1, outerGate.getZ())).isAir(),
                    "the gate of the Outer Lands stands on solid ground");
            test.succeed();
        });
    }

    private static int[] seedLabyrinth(ServerLevel outer, int ringChunkX, int ringChunkZ) {
        int width = 15;
        int height = 15;
        long seed = 20261021L;
        MazeGenerator generator = new MazeGenerator(width, height, seed);
        while (!generator.generate()) generator = new MazeGenerator(width, height, ++seed);
        // The portal cell is the centre of the labyrinth (oracle MazeGenerator:159-161).
        int[] portalChunk = StructurePlacementRules.mazeCellChunk(1 + width / 2, 1 + height / 2,
                ringChunkX, ringChunkZ, width, height);
        OuterLandsProvider.maze(outer).storeLabyrinth(generator, ringChunkX, ringChunkZ);
        outer.getChunk(portalChunk[0], portalChunk[1]);
        return new int[] {portalChunk[0] * 16 + 8, portalChunk[1] * 16 + 8};
    }

    /** The eldritch gate with the two eldritch blocks that keep it alive. */
    private static void gate(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.above(), ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    /** The Outer Lands exists in the running server and keeps the original values. */
    public static void levelStemKeepsTheOriginalValues(GameTestHelper test) {
        ServerLevel overworld = test.getLevel();
        ServerLevel outer = OuterLandsDimension.level(overworld.getServer());
        test.assertTrue(outer != null, "the Outer Lands level exists in the running server");
        test.assertTrue(outer.dimension().equals(OuterLandsProvider.DIMENSION), "the Outer Lands carries the registered dimension key");
        var expected = OuterLandsDimensionRules.dimensionTypeValues();
        var type = outer.dimensionType();
        test.assertTrue(type.hasSkyLight() == expected.hasSkyLight(), "the Outer Lands has no sky light");
        test.assertTrue(type.ambientLight() == expected.ambientLight(), "the Outer Lands keeps the original ambient light of zero");
        test.assertTrue(type.height() == expected.height() && type.minY() == expected.minY()
                && type.logicalHeight() == expected.logicalHeight(), "the Outer Lands keeps the original 256 block tall world");
        test.assertTrue(type.fixedTime().orElse(-1L) == expected.fixedTime(), "the sun never moves in the Outer Lands");
        test.assertTrue(type.coordinateScale() == expected.coordinateScale(), "the Outer Lands keeps the original coordinate scale");
        test.assertTrue(type.bedWorks() == expected.bedWorks() && type.respawnAnchorWorks() == expected.respawnAnchorWorks(),
                "the Outer Lands refuses beds and respawn anchors");
        test.assertTrue(type.monsterSpawnBlockLightLimit() == expected.monsterSpawnBlockLightLimit(),
                "the Outer Lands keeps the original monster spawn light limit");
        var generator = outer.getChunkSource().getGenerator();
        test.assertTrue(generator instanceof FlatLevelSource, "the Outer Lands uses the flat generator of the void chunks");
        var settings = ((FlatLevelSource) generator).settings();
        test.assertTrue(settings.getLayersInfo().size() == 1
                && settings.getLayersInfo().get(0).getHeight() == OuterLandsDimensionRules.LAYER_HEIGHT,
                "the Outer Lands is a single air layer over the void");
        test.assertTrue(settings.getBiome().is(OuterLandsBiomes.ELDRITCH), "the Outer Lands uses the single eldritch biome");
        test.assertTrue(outer.getChunk(0, 0).getBlockState(new BlockPos(8, 0, 8)).isAir(),
                "the flat air layer of the Outer Lands is empty above the void");
        test.succeed();
    }
}
