package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsRules;
import thaumcraft.common.lib.world.OuterLandsWiringRules;

/**
 * The fidelity of the Outer Lands rooms against the original TC4 4.2.3.5.
 *
 * <p>Every expectation of this class is transcribed from the oracle
 * {@code thaumcraft/common/lib/world/dim} package:
 * <ul>
 *   <li>{@code GenPassage:54-55} - the floor and the ceiling of a trapped passage draw the
 *       buried stone separately.</li>
 *   <li>{@code GenNestRoom:115-119} - the nest room rolls its own loot rarity.</li>
 *   <li>{@code GenKeyRoom:97,105-107} - the permanent item keeps its metadata two and the
 *       guardians keep the original offset.</li>
 *   <li>{@code GenBossRoom:72-75} - the lock of a boss room takes the direction of its side.</li>
 *   <li>{@code GenLibraryRoom:78,86,95,98} - the desk slab of the library room keeps its half.</li>
 * </ul>
 */
class OuterLandsRoomFidelityTest {
    /** The floor of the labyrinth, the oracle generates the rooms at Y fifty. */
    private static final int BASE = OuterLandsRules.MAZE_FLOOR_Y;
    /**
     * The columns of a passage whose floor and ceiling are not covered by a wall of the
     * passage: the walls of the passage stand on the outer ring of the seven by seven room.
     */
    private static final int FIRST_COLUMN = 2;
    private static final int LAST_COLUMN = 6;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void roomVoidLiningNeverFillsTheWalkableInterior() {
        assertAll(java.util.stream.IntStream.of(1, 6, 7, 8).mapToObj(feature ->
                (org.junit.jupiter.api.function.Executable) () -> {
                    var cell = new MazeGenerator.Cell((short) (feature << 8));
                    cell.north = true;
                    var builder = new DungeonRooms.Builder();
                    var random = RandomSource.create(173L);
                    var decoration = RandomSource.create(19L);
                    switch (feature) {
                        case 1 -> DungeonRooms.generatePortal(builder, emptyView(), random, decoration, 0, 0, BASE, cell);
                        case 6 -> DungeonRooms.generateKeyRoom(builder, emptyView(), random, decoration, 0, 0, BASE, cell, 2);
                        case 7 -> DungeonRooms.generateNestRoom(builder, emptyView(), random, decoration, 0, 0, BASE, cell);
                        case 8 -> DungeonRooms.generateLibraryRoom(builder, emptyView(), random, decoration, 0, 0, BASE, cell);
                    }
                    assertFalse(builder.placements.stream().anyMatch(p -> p.kind() == DungeonRooms.Kind.ELDRITCH_NOTHING
                                    && p.x() == 7 && p.z() == 7 && p.y() == BASE + 5),
                            "oracle perimeter-only void lining, feature " + feature);
                }));
    }

    @Test void theTrappedPassageDrawsTheBuriedStoneOfTheFloorAndOfTheCeiling() {
        // Oracle GenPassage:54-55 - the floor and the ceiling each draw their own
        // "random.nextInt(3) == 0" roll, so the two halves of a column may differ.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_TRAPPED_PASSAGE;
        cell.north = true;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generatePassage(builder, emptyView(), RandomSource.create(20251021L),
                RandomSource.create(7L), 0, 0, BASE, cell);
        RandomSource control = RandomSource.create(20251021L);
        int different = 0;
        for (int w = 1; w < 8; ++w) {
            for (int h = 1; h < 8; ++h) {
                boolean floor = control.nextInt(3) == 0;
                boolean ceiling = control.nextInt(3) == 0;
                if (w < FIRST_COLUMN || w > LAST_COLUMN || h < FIRST_COLUMN || h > LAST_COLUMN) continue;
                if (floor != ceiling) ++different;
                assertEquals(floor, isTrappedStone(builder, 4 + w, BASE + 2, 4 + h),
                        "the floor of the column " + w + "/" + h + " of the trapped passage");
                assertEquals(ceiling, isTrappedStone(builder, 4 + w, BASE + 8, 4 + h),
                        "the ceiling of the column " + w + "/" + h + " of the trapped passage");
            }
        }
        assertTrue(different > 0, "the floor and the ceiling of a trapped passage are drawn separately");
    }

    @Test void aPlainPassageNeverDrawsTheBuriedStone() {
        // Oracle GenPassage:54-55 - the roll of three is only drawn by a trapped passage.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_PASSAGE;
        cell.north = true;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generatePassage(builder, emptyView(), RandomSource.create(5L), RandomSource.create(6L),
                0, 0, BASE, cell);
        for (int w = FIRST_COLUMN; w <= LAST_COLUMN; ++w) {
            for (int h = FIRST_COLUMN; h <= LAST_COLUMN; ++h) {
                assertFalse(isTrappedStone(builder, 4 + w, BASE + 2, 4 + h), "the floor of a plain passage");
                assertFalse(isTrappedStone(builder, 4 + w, BASE + 8, 4 + h), "the ceiling of a plain passage");
            }
        }
    }

    @Test void theNestRoomRollsItsOwnLootRarity() {
        // Oracle GenNestRoom:116-118 - the nest room rolls fifteen, twenty five and sixty
        // percent and not the rarity of the common decoration of GenCommon:284-285.
        assertEquals(2, OuterLandsRules.nestLootUrnMeta(0.0F));
        assertEquals(2, OuterLandsRules.nestLootUrnMeta(0.1499F));
        assertEquals(1, OuterLandsRules.nestLootUrnMeta(0.15F));
        assertEquals(1, OuterLandsRules.nestLootUrnMeta(0.3999F));
        assertEquals(0, OuterLandsRules.nestLootUrnMeta(0.4F));
        // The decoration rarity of the rest of the labyrinth stays the original one.
        assertEquals(2, OuterLandsRules.lootUrnMeta(0.0F));
        assertEquals(1, OuterLandsRules.lootUrnMeta(0.05F));
        assertEquals(0, OuterLandsRules.lootUrnMeta(0.5F));
    }

    @Test void theNestRoomIsRicherThanTheRestOfTheLabyrinth() {
        int[] counts = new int[3];
        int rooms = 200;
        for (long seed = 0; seed < rooms; ++seed) {
            MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
            cell.feature = OuterLandsRules.FEATURE_NEST_ROOM;
            DungeonRooms.Builder builder = new DungeonRooms.Builder();
            DungeonRooms.generateNestRoom(builder, emptyView(), RandomSource.create(seed),
                    RandomSource.create(seed + 1000L), 0, 0, BASE, cell);
            for (DungeonRooms.Placement placement : builder.placements) {
                if (placement.kind() != DungeonRooms.Kind.LOOT_URN
                        && placement.kind() != DungeonRooms.Kind.LOOT_CRATE) continue;
                ++counts[placement.variant()];
            }
        }
        int total = counts[0] + counts[1] + counts[2];
        assertTrue(total > 1000, "the sample of the nest rooms is large enough: " + total);
        double rich = counts[2] / (double) total;
        double common = counts[1] / (double) total;
        double plain = counts[0] / (double) total;
        assertTrue(rich > 0.11 && rich < 0.19, "the rich containers of the nest are the original fifteen percent: " + rich);
        assertTrue(common > 0.20 && common < 0.30, "the common containers of the nest are the original twenty five percent: " + common);
        assertTrue(plain > 0.55 && plain < 0.65, "the plain containers of the nest are the original sixty percent: " + plain);
    }

    @Test void theKeyRoomGuardiansKeepTheOriginalOffset() {
        // Oracle GenKeyRoom:105,107 - the offset of a guardian is the product of
        // MathHelper.getRandomIntegerInRange(world.rand, 1, 3) and
        // MathHelper.getRandomIntegerInRange(world.rand, -1, 1). The 1.7 helper is
        // "min + random.nextInt(max - min + 1)", so the sign factor is minus one to one and a
        // guardian may stand right of the centre.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_KEY_ROOM;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        RecordingRandom worldRandom = new RecordingRandom(31337L);
        DungeonRooms.generateKeyRoom(builder, emptyView(), RandomSource.create(1L), worldRandom, 0, 0, BASE, cell, 2);
        List<DungeonRooms.Spawn> guardians = builder.spawns.stream()
                .filter(spawn -> spawn.kind() == DungeonRooms.SpawnKind.ELDRITCH_GUARDIAN).toList();
        assertEquals(3, guardians.size(), "the normal difficulty holds three guardians");
        // The guardians are the last consumers of the world random of the room, so their four
        // rolls of three are the last four rolls of every guardian.
        List<String> draws = worldRandom.draws();
        int rolls = 4 * guardians.size();
        assertTrue(draws.size() >= rolls, "the room draws at least the rolls of the guardians");
        int[] values = new int[rolls];
        for (int i = 0; i < rolls; ++i) {
            String draw = draws.get(draws.size() - rolls + i);
            assertTrue(draw.startsWith("int(3)="), "a guardian draws the roll of three of the original: " + draw);
            values[i] = Integer.parseInt(draw.substring("int(3)=".length()));
        }
        for (int guard = 0; guard < guardians.size(); ++guard) {
            int expectedX = (1 + values[4 * guard]) * (-1 + values[4 * guard + 1]);
            int expectedZ = (1 + values[4 * guard + 2]) * (-1 + values[4 * guard + 3]);
            assertEquals(8 + expectedX, guardians.get(guard).x(), "the guardian stands on the original offset");
            assertEquals(8 + expectedZ, guardians.get(guard).z(), "the guardian stands on the original offset");
        }
    }

    @Test void aGuardianMayStandRightOfTheCentre() {
        // The offset of the original is the product of one to three and minus one to one, so
        // the offsets of minus three to three all occur.
        Map<Integer, Integer> offsets = new HashMap<>();
        for (long seed = 0; seed < 200; ++seed) {
            MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
            cell.feature = OuterLandsRules.FEATURE_KEY_ROOM;
            DungeonRooms.Builder builder = new DungeonRooms.Builder();
            DungeonRooms.generateKeyRoom(builder, emptyView(), RandomSource.create(seed),
                    RandomSource.create(seed * 31L + 7L), 0, 0, BASE, cell, 2);
            for (DungeonRooms.Spawn spawn : builder.spawns) {
                if (spawn.kind() != DungeonRooms.SpawnKind.ELDRITCH_GUARDIAN) continue;
                offsets.merge(spawn.x() - 8, 1, Integer::sum);
                offsets.merge(spawn.z() - 8, 1, Integer::sum);
            }
        }
        for (int offset : new int[] {-3, -2, -1, 0, 1, 2, 3}) {
            assertTrue(offsets.containsKey(offset), "the offset " + offset + " occurs: " + offsets);
        }
    }

    @Test void theBossRoomLockTakesTheDirectionOfItsSide() {
        // Oracle GenBossRoom:44-75 - the lock of the doorway takes the direction of the side
        // of the room the cell has an exit on.
        for (int[] sides : new int[][] {{0b0001, 2}, {0b0010, 3}, {0b0100, 5}, {0b1000, 4}}) {
            MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
            cell.feature = OuterLandsRules.FEATURE_BOSS_UPPER_LEFT;
            if ((sides[0] & 1) != 0) cell.north = true;
            if ((sides[0] & 2) != 0) cell.south = true;
            if ((sides[0] & 4) != 0) cell.east = true;
            if ((sides[0] & 8) != 0) cell.west = true;
            DungeonRooms.Builder builder = new DungeonRooms.Builder();
            DungeonRooms.generateBossRoom(builder, emptyView(), RandomSource.create(1L), RandomSource.create(2L),
                    0, 0, BASE, cell);
            DungeonRooms.Placement lock = lock(builder);
            assertEquals(sides[1], lock.facing(), "the lock of the side " + sides[1]);
        }
    }

    @Test void theBossRoomLockOfACellWithoutAnExitKeepsTheUnknownDirection() {
        // Oracle GenBossRoom:48 - the doorway starts with the unknown direction, which is the
        // sixth ForgeDirection, and a cell without an exit never replaces it.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_BOSS_UPPER_LEFT;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generateBossRoom(builder, emptyView(), RandomSource.create(1L), RandomSource.create(2L),
                0, 0, BASE, cell);
        DungeonRooms.Placement lock = lock(builder);
        assertEquals(OuterLandsRules.DIRECTION_UNKNOWN, lock.facing(), "the lock of a cell without an exit");
    }

    @Test void theLibraryDeskSlabKeepsItsHalf() {
        // Oracle GenLibraryRoom:78,86,95,98 - the desk slab is the metadata one below the
        // eldritch block and the metadata nine above it, which is the upper half. The half of
        // a 1.21.1 slab lives in its "type" property and not in the integer "variant"
        // property of the other blocks, so the original metadata has to be decoded by hand.
        BlockState lower = ThaumcraftOuterLands.slabState(Blocks.STONE_SLAB, 1);
        BlockState upper = ThaumcraftOuterLands.slabState(Blocks.STONE_SLAB, 9);
        assertEquals(SlabType.BOTTOM, lower.getValue(SlabBlock.TYPE), "the metadata one is the lower half");
        assertEquals(SlabType.TOP, upper.getValue(SlabBlock.TYPE), "the metadata nine is the upper half");
        assertEquals("thaumcraft:cosmetic_stone_slab_eldritch", OuterLandsWiringRules.stoneSlabBlock(1));
        assertEquals("thaumcraft:cosmetic_stone_slab_eldritch", OuterLandsWiringRules.stoneSlabBlock(9));
    }

    @Test void theLibraryRoomPlacesTheDesksWithTheOriginalMetadata() {
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generateLibraryRoom(builder, emptyView(), RandomSource.create(9L), RandomSource.create(10L),
                0, 0, BASE, cell);
        List<DungeonRooms.Placement> slabs = builder.placements.stream()
                .filter(placement -> placement.kind() == DungeonRooms.Kind.COSMETIC_SLAB_STONE).toList();
        assertEquals(10, slabs.size(), "the library room holds ten desks");
        assertEquals(5, slabs.stream().filter(placement -> placement.variant() == 1).count(),
                "the five lower desks carry the metadata one");
        assertEquals(5, slabs.stream().filter(placement -> placement.variant() == 9).count(),
                "the five upper desks carry the metadata nine");
    }

    @Test void theKeyRoomKeepsThePermanentItemOfTheOriginalMetadata() {
        // Oracle GenKeyRoom:97 - the key room holds the eldritch object with the metadata two,
        // the runed tablet.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_KEY_ROOM;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generateKeyRoom(builder, emptyView(), RandomSource.create(1L), RandomSource.create(2L),
                0, 0, BASE, cell, 2);
        DungeonRooms.Spawn item = builder.spawns.stream()
                .filter(spawn -> spawn.kind() == DungeonRooms.SpawnKind.PERMANENT_ITEM).findFirst().orElseThrow();
        assertEquals(2, item.variant(), "the permanent item of the key room keeps the metadata two");
        assertEquals("thaumcraft:eldritch_tablet", OuterLandsWiringRules.eldritchObjectItem(item.variant()));
    }

    @Test void theOuterLandsColoursAgreeWithTheNativeBiomeDocument() throws Exception {
        String path = "data/thaumcraft/worldgen/biome/eldritch_lands.json";
        JsonObject document;
        try (InputStream stream = OuterLandsRoomFidelityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing native biome document " + path);
            document = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject effects = document.getAsJsonObject("effects");
        assertEquals(effects.get("fog_color").getAsInt(), OuterLandsRules.BIOME_FOG_COLOR, "the fog colour");
        assertEquals(effects.get("grass_color").getAsInt(), OuterLandsRules.GRASS_COLOR, "the grass colour");
        assertEquals(effects.get("foliage_color").getAsInt(), OuterLandsRules.FOLIAGE_COLOR, "the foliage colour");
        assertEquals(effects.get("water_color").getAsInt(), OuterLandsRules.WATER_COLOR, "the water colour");
        assertEquals(effects.get("water_fog_color").getAsInt(), OuterLandsRules.WATER_FOG_COLOR, "the water fog colour");
        assertEquals(effects.get("sky_color").getAsInt(), OuterLandsRules.SKY_COLOR, "the sky colour");
    }

    @Test void theDimensionEffectsDocumentCarriesTheOriginalColours() throws Exception {
        // Oracle WorldProviderOuter:74-115 - the Outer Lands has a black sky, no sky at all,
        // a cloud height of one and the fog colour 0xA080A0 scaled by 0.15.
        String path = "data/thaumcraft/dimension_effects/outer_lands.json";
        JsonObject effects;
        try (InputStream stream = OuterLandsRoomFidelityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing native dimension effects document " + path);
            effects = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        assertEquals(OuterLandsRules.packedFogColor(), effects.get("fog_color").getAsInt(), "the fog colour");
        assertEquals(OuterLandsRules.packedFogColor(), effects.get("cloud_color").getAsInt(), "the cloud colour");
        assertEquals(OuterLandsRules.CLOUD_HEIGHT, effects.get("cloud_height").getAsFloat(), "the cloud height");
        assertEquals(OuterLandsRules.AMBIENT_LIGHT, effects.get("ambient_light").getAsFloat(), "the ambient light");
        assertEquals(OuterLandsRules.SKY_COLOR, effects.get("sky_color").getAsInt(), "the sky colour");
        assertFalse(effects.get("has_sky").getAsBoolean(), "the Outer Lands has no sky");
    }

    /** The lock of a boss room, the eldritch block with the metadata eight. */
    private static DungeonRooms.Placement lock(DungeonRooms.Builder builder) {
        return builder.placements.stream()
                .filter(placement -> placement.kind() == DungeonRooms.Kind.ELDRITCH
                        && placement.variant() == EldritchBlockRules.ELDRITCH_LOCK)
                .findFirst().orElseThrow(() -> new AssertionError("the boss room holds no lock"));
    }

    /** True when the last placement of a position is the trapped stone of a passage. */
    private static boolean isTrappedStone(DungeonRooms.Builder builder, int x, int y, int z) {
        boolean trapped = false;
        for (DungeonRooms.Placement placement : builder.placements) {
            if (placement.x() != x || placement.y() != y || placement.z() != z) continue;
            trapped = placement.kind() == DungeonRooms.Kind.ELDRITCH
                    && placement.variant() == EldritchBlockRules.ELDRITCH_RUNED;
        }
        return trapped;
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

    /**
     * A random source that records every draw of the room code, so that a test can prove
     * which roll the original code draws and in which order.
     */
    private static final class RecordingRandom implements RandomSource {
        private final RandomSource delegate;
        private final List<String> draws = new ArrayList<>();

        RecordingRandom(long seed) {
            this.delegate = RandomSource.create(seed);
        }

        List<String> draws() {
            return this.draws;
        }

        @Override public RandomSource fork() {
            throw new UnsupportedOperationException("the room code never forks its random");
        }

        @Override public PositionalRandomFactory forkPositional() {
            throw new UnsupportedOperationException("the room code never forks its random");
        }

        @Override public void setSeed(long seed) {
            this.delegate.setSeed(seed);
        }

        @Override public int nextInt() {
            int value = this.delegate.nextInt();
            this.draws.add("int=" + value);
            return value;
        }

        @Override public int nextInt(int bound) {
            int value = this.delegate.nextInt(bound);
            this.draws.add("int(" + bound + ")=" + value);
            return value;
        }

        @Override public long nextLong() {
            long value = this.delegate.nextLong();
            this.draws.add("long=" + value);
            return value;
        }

        @Override public boolean nextBoolean() {
            boolean value = this.delegate.nextBoolean();
            this.draws.add("boolean=" + value);
            return value;
        }

        @Override public float nextFloat() {
            float value = this.delegate.nextFloat();
            this.draws.add("float=" + value);
            return value;
        }

        @Override public double nextDouble() {
            double value = this.delegate.nextDouble();
            this.draws.add("double=" + value);
            return value;
        }

        @Override public double nextGaussian() {
            double value = this.delegate.nextGaussian();
            this.draws.add("gaussian=" + value);
            return value;
        }
    }
}
