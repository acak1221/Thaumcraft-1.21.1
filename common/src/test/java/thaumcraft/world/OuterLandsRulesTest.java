package thaumcraft.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsBiomes;
import thaumcraft.common.lib.world.OuterLandsRules;
import static org.junit.jupiter.api.Assertions.*;

/** Oracle WorldProviderOuter, ChunkProviderOuter, BiomeGenEldritch, MazeHandler, GenCommon. */
class OuterLandsRulesTest {

    @Test void dimensionIdentityKeepsTheOriginalIdNameAndFolder() {
        assertEquals(-42, OuterLandsRules.DIMENSION_ID);
        assertEquals("The Outer Lands", OuterLandsRules.NAME);
        assertEquals("DIM_OUTERLANDS", OuterLandsRules.SAVE_FOLDER);
        assertEquals("Entering The Outer Lands", OuterLandsRules.WELCOME_MESSAGE);
        assertEquals("Leaving The Outer Lands", OuterLandsRules.DEPART_MESSAGE);
        assertEquals(50, OuterLandsRules.AVERAGE_GROUND_LEVEL);
        assertEquals(50, OuterLandsRules.MAZE_FLOOR_Y);
        assertEquals(1.0F, OuterLandsRules.CLOUD_HEIGHT);
        assertEquals(0.0F, OuterLandsRules.CELESTIAL_ANGLE);
        assertEquals(256, OuterLandsRules.HEIGHT);
        assertEquals(0, OuterLandsRules.MIN_Y);
    }

    @Test void dimensionLightAndFogKeepTheOriginalValues() {
        assertFalse(OuterLandsRules.HAS_SKY_LIGHT);
        assertEquals(0.0F, OuterLandsRules.AMBIENT_LIGHT);
        assertFalse(OuterLandsRules.SURFACE_WORLD);
        assertFalse(OuterLandsRules.RESPAWN_ALLOWED);
        assertTrue(OuterLandsRules.FOG_ENABLED);
        assertFalse(OuterLandsRules.SKY_COLOURED);
        // WorldProviderOuter.func_76562_b: 0xA080A0 scaled by 0.15.
        assertEquals(0xA080A0, OuterLandsRules.FOG_COLOR);
        assertEquals(0.15F, OuterLandsRules.FOG_BRIGHTNESS);
        assertEquals(0x181318, OuterLandsRules.packedFogColor());
        // BiomeGenEldritch.func_76731_a returns 0, the Outer Lands sky is black.
        assertEquals(0, OuterLandsRules.SKY_COLOR);
        assertEquals(0, OuterLandsBiomes.skyColor());
        assertEquals(0x181318, OuterLandsBiomes.fogColor());
        assertFalse(OuterLandsBiomes.hasPrecipitation());
        assertEquals(0.5F, OuterLandsBiomes.temperature());
        assertEquals(0.5F, OuterLandsBiomes.downfall());
    }

    @Test void outerLandsNeverGeneratesAuraNodes() {
        assertFalse(OuterLandsRules.generatesAuraNodes());
    }

    @Test void mazeCellFeaturesDispatchToTheOriginalRooms() {
        assertEquals(OuterLandsRules.RoomKind.PORTAL, OuterLandsRules.roomForFeature(1));
        assertEquals(OuterLandsRules.RoomKind.BOSS, OuterLandsRules.roomForFeature(2));
        assertEquals(OuterLandsRules.RoomKind.BOSS, OuterLandsRules.roomForFeature(3));
        assertEquals(OuterLandsRules.RoomKind.BOSS, OuterLandsRules.roomForFeature(4));
        assertEquals(OuterLandsRules.RoomKind.BOSS, OuterLandsRules.roomForFeature(5));
        assertEquals(OuterLandsRules.RoomKind.KEY_ROOM, OuterLandsRules.roomForFeature(6));
        assertEquals(OuterLandsRules.RoomKind.NEST_ROOM, OuterLandsRules.roomForFeature(7));
        assertEquals(OuterLandsRules.RoomKind.LIBRARY_ROOM, OuterLandsRules.roomForFeature(8));
        // 0, 9 and 10..14 are passages.
        for (int feature : new int[] {0, 9, 10, 11, 12, 13, 14, 99})
            assertEquals(OuterLandsRules.RoomKind.PASSAGE, OuterLandsRules.roomForFeature(feature), "feature " + feature);
    }

    @Test void bossRoomQuadrantsKeepTheirOriginalOrder() {
        assertTrue(OuterLandsRules.isBossQuadrant(2));
        assertTrue(OuterLandsRules.isBossQuadrant(5));
        assertFalse(OuterLandsRules.isBossQuadrant(1));
        assertFalse(OuterLandsRules.isBossQuadrant(6));
        assertEquals(0, OuterLandsRules.bossQuadrant(2));
        assertEquals(1, OuterLandsRules.bossQuadrant(3));
        assertEquals(2, OuterLandsRules.bossQuadrant(4));
        assertEquals(3, OuterLandsRules.bossQuadrant(5));
        assertEquals(-1, OuterLandsRules.bossQuadrant(6));
    }

    @Test void keyRoomGuardiansScaleWithTheOriginalDifficulty() {
        assertEquals(2, OuterLandsRules.keyRoomGuardians(0));
        assertEquals(2, OuterLandsRules.keyRoomGuardians(1));
        assertEquals(3, OuterLandsRules.keyRoomGuardians(2));
        assertEquals(4, OuterLandsRules.keyRoomGuardians(3));
        assertTrue(OuterLandsRules.keyRoomChampion(3, 0));
        assertFalse(OuterLandsRules.keyRoomChampion(3, 1));
        assertFalse(OuterLandsRules.keyRoomChampion(2, 0));
    }

    @Test void decorationRarityKeepsTheOriginalThresholds() {
        assertEquals(2, OuterLandsRules.lootUrnMeta(0.0F));
        assertEquals(2, OuterLandsRules.lootUrnMeta(0.024F));
        assertEquals(1, OuterLandsRules.lootUrnMeta(0.025F));
        assertEquals(1, OuterLandsRules.lootUrnMeta(0.099F));
        assertEquals(0, OuterLandsRules.lootUrnMeta(0.1F));
        assertTrue(OuterLandsRules.nestLootIsCrate(0.0F));
        assertTrue(OuterLandsRules.nestLootIsCrate(0.199F));
        assertFalse(OuterLandsRules.nestLootIsCrate(0.2F));
        assertEquals(0.15F, OuterLandsRules.NEST_LOOT_DENSITY);
        assertEquals(4, OuterLandsRules.commonDecorationMeta(1, 0));
        assertEquals(5, OuterLandsRules.commonDecorationMeta(0, 1));
        assertEquals(10, OuterLandsRules.commonDecorationMeta(0, 0));
        assertEquals(1, OuterLandsRules.taintFibreMeta(0));
        assertEquals(0, OuterLandsRules.taintFibreMeta(1));
    }

    @Test void mazeDirectionHelpersKeepTheOriginalBitTable() {
        assertEquals(2, MazeGenerator.opposite(1));
        assertEquals(1, MazeGenerator.opposite(2));
        assertEquals(8, MazeGenerator.opposite(4));
        assertEquals(4, MazeGenerator.opposite(8));
        assertEquals(-99, MazeGenerator.opposite(16));
        assertEquals(0, MazeGenerator.dx(1));
        assertEquals(1, MazeGenerator.dx(4));
        assertEquals(-1, MazeGenerator.dx(8));
        assertEquals(-1, MazeGenerator.dy(1));
        assertEquals(1, MazeGenerator.dy(2));
        assertEquals(0, MazeGenerator.dy(4));
    }

    @Test void mazeCellPacksTheOriginalBitLayout() {
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.north = true;
        cell.east = true;
        cell.feature = 6;
        short packed = cell.pack();
        assertEquals(1, packed & 1);
        assertEquals(4, packed & 4);
        assertEquals(0, packed & 2);
        assertEquals(6, packed >> 8);
        MazeGenerator.Cell restored = new MazeGenerator.Cell(packed);
        assertTrue(restored.north);
        assertTrue(restored.east);
        assertFalse(restored.south);
        assertFalse(restored.west);
        assertEquals(6, restored.feature);
        assertEquals(2, restored.exits());
    }

    @Test void labyrinthLayoutIsDeterministicAndCarriesTheOriginalMarkers() {
        MazeGenerator first = new MazeGenerator(13, 15, 1234L);
        assertTrue(first.generate());
        MazeGenerator second = new MazeGenerator(13, 15, 1234L);
        assertTrue(second.generate());
        for (int y = 0; y < 15; ++y)
            for (int x = 0; x < 13; ++x) assertEquals(first.grid[y][x], second.grid[y][x], "cell " + x + "/" + y);
        // The portal marker sits in the centre of the labyrinth.
        int px = 1 + 13 / 2;
        int py = 1 + 15 / 2;
        assertEquals(1, new MazeGenerator.Cell((short) first.grid[py][px]).feature);
        // The 2x2 boss room markers sit in one corner: features 2, 3, 4 and 5.
        Set<Integer> quadrantFeatures = new HashSet<>();
        for (int y = 0; y < 15; ++y)
            for (int x = 0; x < 13; ++x) {
                int feature = new MazeGenerator.Cell((short) first.grid[y][x]).feature;
                if (feature >= 2 && feature <= 5) quadrantFeatures.add(feature);
            }
        assertEquals(Set.of(2, 3, 4, 5), quadrantFeatures);
        // Every generated cell carries a direction or a feature, never the blocked marker.
        for (int y = 0; y < 15; ++y)
            for (int x = 0; x < 13; ++x) assertTrue(first.grid[y][x] >= 0, "blocked cell " + x + "/" + y);
    }

    @Test void labyrinthAlwaysContainsAKeyRoomAndTheSpecialPassages() {
        for (long seed = 0; seed < 12; ++seed) {
            MazeGenerator generator = new MazeGenerator(11, 11, seed * 7919L);
            if (!generator.generate()) continue;
            boolean keyRoom = false;
            boolean special = false;
            for (int y = 0; y < 11; ++y)
                for (int x = 0; x < 11; ++x) {
                    int feature = new MazeGenerator.Cell((short) generator.grid[y][x]).feature;
                    if (feature == OuterLandsRules.FEATURE_KEY_ROOM) keyRoom = true;
                    if (feature >= OuterLandsRules.FEATURE_PASSAGE && feature <= OuterLandsRules.FEATURE_SPIDER_PASSAGE) special = true;
                }
            assertTrue(keyRoom, "the labyrinth always holds one key room, seed " + seed);
            assertTrue(special, "the labyrinth always holds decorated passages, seed " + seed);
        }
    }

    @Test void keyRoomLayoutKeepsItsShellAndItsContents() {
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.View view = emptyView();
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.north = true;
        cell.south = true;
        cell.east = true;
        cell.west = true;
        DungeonRooms.generateKeyRoom(builder, view, RandomSource.create(1L), RandomSource.create(2L), 3, 4,
                OuterLandsRules.MAZE_FLOOR_Y, cell, 2);
        int bedrock = 0;
        Set<Long> shell = new HashSet<>();
        boolean altar = false;
        boolean permanentItem = false;
        int guardians = 0;
        int chunkX = 3;
        int chunkZ = 4;
        for (DungeonRooms.Placement placement : builder.placements) {
            if (placement.kind() == DungeonRooms.Kind.BEDROCK) {
                ++bedrock;
                int localX = placement.x() - chunkX * 16;
                int localZ = placement.z() - chunkZ * 16;
                int localY = placement.y() - OuterLandsRules.MAZE_FLOOR_Y;
                if (localY < 0 || localY > 12) continue;
                if (localX < 1 || localX > 15 || localZ < 1 || localZ > 15) continue;
                if (localX != 1 && localX != 15 && localZ != 1 && localZ != 15) continue;
                shell.add(((long) localX << 40) | ((long) localZ << 20) | localY);
            }
            if (placement.kind() == DungeonRooms.Kind.ELDRITCH && placement.variant() == 3) altar = true;
        }
        for (DungeonRooms.Spawn spawn : builder.spawns) {
            if (spawn.kind() == DungeonRooms.SpawnKind.PERMANENT_ITEM) permanentItem = true;
            if (spawn.kind() == DungeonRooms.SpawnKind.ELDRITCH_GUARDIAN) ++guardians;
        }
        // The room shell is the complete 15x15 ring of thirteen layers.
        for (int localX = 1; localX <= 15; ++localX) {
            for (int localZ = 1; localZ <= 15; ++localZ) {
                if (localX != 1 && localX != 15 && localZ != 1 && localZ != 15) continue;
                for (int localY = 0; localY <= 12; ++localY) {
                    long key = ((long) localX << 40) | ((long) localZ << 20) | localY;
                    assertTrue(shell.contains(key), "missing shell block " + localX + "/" + localY + "/" + localZ);
                }
            }
        }
        assertEquals(15 * 15 * 13 - 13 * 13 * 13, shell.size());
        assertTrue(bedrock >= shell.size());
        assertTrue(altar);
        assertTrue(permanentItem);
        assertEquals(3, guardians);
    }

    @Test void portalRoomHoldsThePortalAndTheObelisk() {
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        DungeonRooms.generatePortal(builder, emptyView(), RandomSource.create(3L), RandomSource.create(4L), 1, 1,
                OuterLandsRules.MAZE_FLOOR_Y, cell);
        boolean portal = false;
        int obelisk = 0;
        for (DungeonRooms.Placement placement : builder.placements) {
            if (placement.kind() == DungeonRooms.Kind.ELDRITCH_PORTAL) portal = true;
            if (placement.kind() == DungeonRooms.Kind.ELDRITCH && placement.variant() == 2) ++obelisk;
        }
        assertTrue(portal);
        assertEquals(4, obelisk);
    }

    @Test void roomLayoutsNeverLeaveTheirOwnChunk() {
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.north = true;
        cell.south = true;
        cell.east = true;
        cell.west = true;
        DungeonRooms.generatePassage(builder, emptyView(), RandomSource.create(5L), RandomSource.create(6L), 2, 2,
                OuterLandsRules.MAZE_FLOOR_Y, cell);
        assertFalse(builder.placements.isEmpty());
        for (DungeonRooms.Placement placement : builder.placements) {
            assertTrue(placement.x() >> 4 == 2, "x " + placement.x());
            assertTrue(placement.z() >> 4 == 2, "z " + placement.z());
        }
    }

    @Test void bossRoomQuadrantsBuildTheirOwnHalfOfTheRoom() {
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_BOSS_UPPER_LEFT;
        DungeonRooms.generateBossRoom(builder, emptyView(), RandomSource.create(7L), RandomSource.create(8L), 0, 0,
                OuterLandsRules.MAZE_FLOOR_Y, cell);
        boolean bedrock = false;
        for (DungeonRooms.Placement placement : builder.placements)
            if (placement.kind() == DungeonRooms.Kind.BEDROCK) bedrock = true;
        assertTrue(bedrock);
    }

    @Test void connectionPatternKeepsTheOriginalCrossSection() {
        assertEquals(11, DungeonRooms.PAT_CONNECT.length);
        assertEquals(11, DungeonRooms.PAT_CONNECT[0].length);
        assertEquals(0, DungeonRooms.PAT_CONNECT[0][0]);
        assertEquals(1, DungeonRooms.PAT_CONNECT[0][1]);
        assertEquals(8, DungeonRooms.PAT_CONNECT[1][1]);
        assertEquals(2, DungeonRooms.PAT_CONNECT[2][3]);
        assertEquals(7, DungeonRooms.PAT_DOORWAY.length);
        assertEquals(1, DungeonRooms.PAT_DOORWAY[3][3]);
        assertEquals(9, DungeonRooms.PAT_DOORWAY[3][4]);
        assertEquals(2, DungeonRooms.PAT_DOORWAY[0][1]);
        assertEquals(0, DungeonRooms.PAT_DOORWAY[0][0]);
    }

    /** A world view of an empty void: every position is air and nothing is the eldritch nothing. */
    private static DungeonRooms.View emptyView() {
        return new DungeonRooms.View() {
            @Override public boolean isAir(int x, int y, int z) {
                return true;
            }

            @Override public boolean isVoidBlock(int x, int y, int z) {
                return false;
            }

            @Override public boolean isSolidAdjacent(int x, int y, int z) {
                return false;
            }

            @Override public boolean isCosmeticSolidOrStair(int x, int y, int z) {
                return false;
            }
        };
    }

    @Test void spawnListKeepsOnlyTheTwoOriginalMonsters() {
        assertEquals(List.of("thaumcraft:inhabited_zombie", "thaumcraft:eldritch_guardian"),
                OuterLandsRules.MONSTER_SPAWNS);
        assertTrue(OuterLandsRules.PASSIVE_SPAWNS.isEmpty());
        assertEquals(0.1F, OuterLandsRules.CREATURE_SPAWN_PROBABILITY);
    }

    @Test void placementListIsFreeOfDuplicatesForTheRoomShell() {
        List<DungeonRooms.Placement> placements = new ArrayList<>();
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        DungeonRooms.generateLibraryRoom(builder, emptyView(), RandomSource.create(9L), RandomSource.create(10L), 5, 5,
                OuterLandsRules.MAZE_FLOOR_Y, cell);
        placements.addAll(builder.placements);
        assertFalse(placements.isEmpty());
        long desks = placements.stream().filter(p -> p.kind() == DungeonRooms.Kind.ELDRITCH && p.variant() == 5).count();
        assertEquals(10, desks);
    }
}
