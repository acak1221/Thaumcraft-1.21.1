package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsRules;
import thaumcraft.common.lib.world.OuterLandsWiringRules;

/**
 * The mapping of the original TC4 4.2.3.5 eldritch metadata onto the native blocks of the port.
 *
 * <p>Every expectation of this class is transcribed from the oracle
 * {@code thaumcraft/common/blocks/BlockEldritch.java}:
 * <ul>
 *   <li>{@code :78-89} - the icon of every metadata, and therefore the model and the texture
 *       that the native blockstate of the metadata has to name.</li>
 *   <li>{@code :92-94} - the icon of the metadata nine is the icon of the cosmetic solid with
 *       the metadata fourteen.</li>
 *   <li>{@code :120-135} - the light level of every metadata.</li>
 *   <li>{@code :231-246} - the hardness of every metadata.</li>
 *   <li>{@code :248-260} - the blast resistance of every metadata.</li>
 * </ul>
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block and not to the
 * block state, so the original family became six native blocks that carry the model of their
 * metadata in the "variant" block state property. The native blockstates of the port are the
 * authority of that grouping and are read by this test as well.
 */
class OuterLandsEldritchVariantTest {
    /** The floor of the labyrinth, the oracle generates the rooms at Y fifty. */
    private static final int BASE = OuterLandsRules.MAZE_FLOOR_Y;
    private static final String ASSETS = "assets/thaumcraft/";

    /**
     * One original metadata of the oracle family.
     *
     * @param metadata the original 1.7 metadata
     * @param blockId the native registry id of the block that hosts the metadata
     * @param models the models that the blockstate of the metadata names
     * @param textures the textures of those models
     * @param hardness the original hardness of the metadata
     * @param resistance the original blast resistance of the metadata
     * @param light the original light level of the metadata
     */
    private record Row(int metadata, String blockId, List<String> models, List<String> textures,
                       float hardness, float resistance, int light) {}

    /** The original metadata zero to ten and the values of the oracle. */
    private static final List<Row> ROWS = List.of(
            new Row(0, "thaumcraft:eldritch", List.of("thaumcraft:block/eldritch"),
                    List.of("thaumcraft:blocks/obsidiantile"), 50.0F, 20000.0F, 8),
            new Row(1, "thaumcraft:eldritch", List.of("thaumcraft:block/eldritch_1"),
                    List.of("thaumcraft:blocks/obsidiantile"), 50.0F, 20000.0F, 8),
            new Row(2, "thaumcraft:eldritch", List.of("thaumcraft:block/eldritch_2"),
                    List.of("thaumcraft:blocks/obsidiantile"), 50.0F, 20000.0F, 8),
            new Row(3, "thaumcraft:eldritch", List.of("thaumcraft:block/eldritch_3"),
                    List.of("thaumcraft:blocks/obsidiantile"), 50.0F, 20000.0F, 8),
            new Row(4, "thaumcraft:eldritch_crust", List.of("thaumcraft:block/eldritch_crust"),
                    List.of("thaumcraft:blocks/es_i_1"), 2.0F, 30.0F, 12),
            new Row(5, "thaumcraft:eldritch_crust", List.of("thaumcraft:block/eldritch_crust_5"),
                    List.of("thaumcraft:blocks/es_i_2"), 2.0F, 30.0F, 12),
            new Row(6, "thaumcraft:eldritch_deco", List.of("thaumcraft:block/eldritch_deco"),
                    List.of("thaumcraft:blocks/deco_1"), 4.0F, 100.0F, 5),
            new Row(7, "thaumcraft:eldritch_doorway", List.of("thaumcraft:block/eldritch_doorway"),
                    List.of("thaumcraft:blocks/deco_3"), -1.0F, Float.MAX_VALUE, 12),
            new Row(8, "thaumcraft:eldritch_doorway", List.of("thaumcraft:block/eldritch_doorway_8"),
                    List.of("thaumcraft:blocks/deco_2"), -1.0F, Float.MAX_VALUE, 5),
            new Row(9, "thaumcraft:eldritch_spawner", List.of("thaumcraft:block/eldritch_spawner"),
                    List.of("thaumcraft:blocks/crust"), 15.0F, 30.0F, 4),
            // The runed trap draws one of the four sheets of the oracle (BlockEldritch:106-111).
            new Row(10, "thaumcraft:eldritch_spawner",
                    List.of("thaumcraft:block/eldritch_spawner_10_0", "thaumcraft:block/eldritch_spawner_10_1",
                            "thaumcraft:block/eldritch_spawner_10_2", "thaumcraft:block/eldritch_spawner_10_3"),
                    List.of("thaumcraft:blocks/es_5", "thaumcraft:blocks/es_6", "thaumcraft:blocks/es_7",
                            "thaumcraft:blocks/es_8"), 15.0F, 30.0F, 0));

    /**
     * The native blocks of the eldritch family, registered under their production ids before the
     * vanilla bootstrap freezes the block registry.
     *
     * @see EldritchTestRegistry
     */
    private static final List<Block> FAMILY_BLOCKS = EldritchTestRegistry.family();

    /** The runed tablet, the key of the eldritch lock of the original. */
    private static final Item TABLET = EldritchTestRegistry.tablet();

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test void everyOriginalMetadataLandsOnItsOwnBlock() {
        for (Row row : ROWS) {
            OuterLandsWiringRules.EldritchVariant variant = OuterLandsWiringRules.eldritchVariant(row.metadata());
            assertNotNull(variant, "the metadata " + row.metadata() + " of the oracle");
            assertEquals(row.blockId(), variant.blockId(), "the block of the metadata " + row.metadata());
            assertEquals(row.metadata(), variant.variant(), "the variant of the metadata " + row.metadata());
            assertEquals(row.hardness(), variant.hardness(), 0.0F, "the hardness of the metadata " + row.metadata());
            assertEquals(row.resistance(), variant.resistance(), 0.0F,
                    "the blast resistance of the metadata " + row.metadata());
            assertEquals(row.light(), variant.light(), "the light of the metadata " + row.metadata());
            assertEquals(row.blockId(), OuterLandsWiringRules.eldritchBlock(row.metadata()),
                    "the block of the metadata " + row.metadata());
        }
    }

    @Test void theTableCoversEveryOriginalMetadata() {
        // Oracle BlockEldritch:151-153 - the original family has the metadata zero to ten.
        for (int metadata = 0; metadata <= 10; ++metadata) {
            assertNotNull(OuterLandsWiringRules.eldritchVariant(metadata), "the metadata " + metadata);
        }
        assertEquals(11, ROWS.size(), "the table has one row per original metadata");
    }

    @Test void theBlockStateOfEveryMetadataNamesItsModelAndItsTexture() throws Exception {
        for (Row row : ROWS) {
            String path = ASSETS + "blockstates/" + path(row.blockId()) + ".json";
            JsonObject variants = object(path).getAsJsonObject("variants");
            JsonElement entry = variants.get("variant=" + row.metadata());
            assertNotNull(entry, path + " misses the variant=" + row.metadata());
            List<String> models = new ArrayList<>();
            collect(entry, models);
            assertEquals(row.models(), models, "the models of the metadata " + row.metadata());
            for (int index = 0; index < models.size(); ++index) {
                JsonObject model = object(ASSETS + "models/" + models.get(index).substring("thaumcraft:".length()) + ".json");
                List<String> textures = new ArrayList<>();
                for (var texture : model.getAsJsonObject("textures").entrySet()) textures.add(texture.getValue().getAsString());
                assertTrue(textures.contains(row.textures().get(index)),
                        "the model " + models.get(index) + " does not use the texture " + row.textures().get(index));
                for (String texture : textures) {
                    assertEquals(row.textures().get(index), texture,
                            "the model " + models.get(index) + " uses a foreign texture");
                }
            }
        }
    }

    @Test void theResolverPutsEveryMetadataOnItsBlock() {
        assumeTrue(!FAMILY_BLOCKS.isEmpty(), "the block registry was frozen before this class was loaded");
        for (Row row : ROWS) {
            BlockState state = ThaumcraftOuterLands.state(DungeonRooms.Kind.ELDRITCH, row.metadata(), -1);
            assertNotNull(state, "the state of the metadata " + row.metadata());
            assertEquals(row.blockId(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                    "the block of the metadata " + row.metadata());
            assertEquals(row.metadata(), state.getValue(BlockEldritch.VARIANT),
                    "the variant of the metadata " + row.metadata());
            assertEquals(row.light(), state.getLightEmission(), "the light of the metadata " + row.metadata());
            assertEquals(row.hardness(), state.getBlock().defaultDestroyTime(),
                    "the hardness of the metadata " + row.metadata());
            assertEquals(row.resistance(), state.getBlock().getExplosionResistance(),
                    "the blast resistance of the metadata " + row.metadata());
        }
    }

    @Test void theResolverKnowsEveryNativeBlockOfTheFamily() {
        assumeTrue(!FAMILY_BLOCKS.isEmpty(), "the block registry was frozen before this class was loaded");
        for (Row row : ROWS) {
            Block block = ThaumcraftOuterLands.eldritchBlock(row.metadata());
            assertNotNull(block, "the block of the metadata " + row.metadata());
            assertTrue(block instanceof BlockEldritch, "the block of the metadata " + row.metadata());
            assertTrue(((BlockEldritch) block).allows(row.metadata()),
                    "the block of the metadata " + row.metadata() + " does not host it");
        }
    }

    @Test void theEldritchLockAcceptsTheRunedTablet() {
        // Oracle BlockEldritch:278 - the lock of a boss room opens with the eldritch object
        // with the metadata two, the runed tablet of the key room.
        assumeTrue(TABLET != null, "the item registry was frozen before this class was loaded");
        assertTrue(BlockEldritch.isEldritchLockKey(new ItemStack(TABLET)), "the runed tablet opens the lock");
        assertFalse(BlockEldritch.isEldritchLockKey(new ItemStack(Items.STONE)), "a stone does not open the lock");
        assertEquals("thaumcraft:eldritch_tablet", OuterLandsWiringRules.eldritchObjectItem(
                EldritchBlockRules.ELDRITCH_OBJECT_TABLET));
    }

    @Test void theBossRoomLockOfACellWithoutAnExitStaysInsideItsChunk() {
        // Oracle GenBossRoom:48 - the doorway of a quadrant without an exit keeps the unknown
        // direction and the coordinates zero, which is the world origin of the original. The
        // 1.21.1 generation writes into the chunk that is generated, so the native keeps the
        // corner of its own chunk.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_BOSS_UPPER_LEFT;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generateBossRoom(builder, emptyView(), RandomSource.create(1L), RandomSource.create(2L),
                5, 7, BASE, cell);
        boolean lock = false;
        for (DungeonRooms.Placement placement : builder.placements) {
            assertTrue(placement.x() >= 80 && placement.x() < 96,
                    "the placement " + placement + " leaves the chunk in x");
            assertTrue(placement.z() >= 112 && placement.z() < 128,
                    "the placement " + placement + " leaves the chunk in z");
            if (placement.kind() == DungeonRooms.Kind.ELDRITCH
                    && placement.variant() == EldritchBlockRules.ELDRITCH_LOCK) lock = true;
        }
        assertTrue(lock, "the boss room holds its lock");
    }

    @Test void thePortalRoomClearsAllFourCorners() {
        // Oracle GenPortal:84 - the portal room is cleared in all four corners, the lower
        // right one included.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_PORTAL;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generatePortal(builder, emptyView(), RandomSource.create(1L), RandomSource.create(2L),
                0, 0, BASE, cell);
        for (int[] corner : new int[][] {{3, 3}, {3, 13}, {13, 3}, {13, 13}}) {
            for (int c = 1; c < 12; ++c) {
                DungeonRooms.Placement placement = last(builder, corner[0], BASE + c, corner[1]);
                assertNotNull(placement, "the corner " + corner[0] + "/" + corner[1] + " holds no block");
                assertEquals(DungeonRooms.Kind.AIR, placement.kind(),
                        "the corner " + corner[0] + "/" + corner[1] + " is not cleared");
            }
        }
    }

    @Test void thePortalRoomKeepsItsWalls() {
        // The clearing of the portal room is limited to the four corners.
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_PORTAL;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generatePortal(builder, emptyView(), RandomSource.create(1L), RandomSource.create(2L),
                0, 0, BASE, cell);
        DungeonRooms.Placement placement = last(builder, 8, BASE + 5, 8);
        assertNotNull(placement, "the middle of the portal room holds no block");
        assertTrue(placement.kind() != DungeonRooms.Kind.AIR, "the middle of the portal room is cleared");
    }

    // ------------------------------------------------------------------ helpers

    /** The path of a registry id of the mod namespace. */
    private static String path(String registryId) {
        return registryId.substring("thaumcraft:".length());
    }

    /** The models of a blockstate entry, which may be a list of the models of a random variant. */
    private static void collect(JsonElement apply, List<String> models) {
        if (apply.isJsonArray()) {
            for (JsonElement element : apply.getAsJsonArray()) collect(element, models);
        } else {
            models.add(apply.getAsJsonObject().get("model").getAsString());
        }
    }

    /** The last placement at a position, which is the one that wins in the world. */
    private static DungeonRooms.Placement last(DungeonRooms.Builder builder, int x, int y, int z) {
        DungeonRooms.Placement found = null;
        for (DungeonRooms.Placement placement : builder.placements) {
            if (placement.x() != x || placement.y() != y || placement.z() != z) continue;
            found = placement;
        }
        return found;
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

    /** One document of the packaged resources of the mod. */
    private static JsonObject object(String path) throws Exception {
        try (InputStream stream = OuterLandsEldritchVariantTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
