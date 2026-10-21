package thaumcraft.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockEldritchNothing;
import thaumcraft.common.blocks.BlockEldritchPortal;
import thaumcraft.common.blocks.BlockMagicBox;
import thaumcraft.common.blocks.BlockMetalDevice;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.blocks.PerStateBlockEntity;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.FamilyVariant;

/**
 * The render shape and the block entity contract of the eldritch, cosmetic and device families.
 *
 * <p>Two defects of the port are fixed here and both of them are read from the 1.7.10 original:
 * <ul>
 *   <li><b>The invisible blocks.</b> {@code BaseEntityBlock} answers {@code RenderShape.INVISIBLE}
 *       for every block entity block (Minecraft 1.21.1
 *       {@code net/minecraft/world/level/block/BaseEntityBlock}:22-25), so the blocks that carry a
 *       tile were drawn as nothing. The original draws them: the cosmetic solid is an ordinary
 *       block (OpenTC4 BlockCosmeticSolid:52), the eldritch stone, the cosmetic opaque and the
 *       three device families are drawn by the renderer of their family (OpenTC4
 *       BlockEldritch:140-141, BlockCosmeticOpaque:68-69, BlockMetalDevice:145-146,
 *       BlockStoneDevice:69-70, BlockWoodenDevice:200-201), and the magic box is an ordinary
 *       block (OpenTC4 BlockMagicBox:22). The blocks that the original did not draw stay
 *       invisible: the eldritch nothing, the eldritch gate, the airy family and the mirror
 *       (OpenTC4 BlockEldritchNothing:57-58, BlockEldritchPortal:78-79, BlockAiry:196-197,
 *       BlockMirror:79-80).</li>
 *   <li><b>The piston.</b> {@code PistonBaseBlock#isPushable} answers
 *       {@code !blockState.hasBlockEntity()} (Minecraft 1.21.1
 *       {@code net/minecraft/world/level/block/piston/PistonBaseBlock}:277) and
 *       {@code BlockState#hasBlockEntity} is {@code block instanceof EntityBlock} (Minecraft
 *       1.21.1 {@code net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase}:803-805),
 *       so a block that claims a tile for all of its states is never moved by a piston. The
 *       original decided per metadata ({@code hasTileEntity(int)}), so the port answers the
 *       question per state ({@link PerStateBlockEntity}) and the common mixin
 *       {@code thaumcraft.common.mixin.PerStateBlockEntityMixin} gives that answer to the
 *       vanilla code.</li>
 * </ul>
 *
 * <p>The tests read the port and the oracle only; they need neither a level nor a registry.
 */
class BlockVisibilityAndPistonTest {
    /** The three groups of the cosmetic solid family. */
    private static final int[] COSMETIC_GROUPS = {0, 1, 2};
    /** The six groups of the eldritch family. */
    private static final int[] ELDRITCH_GROUPS = {0, 1, 2, 3, 4, 5};
    /** The four groups of the airy family. */
    private static final int[] AIRY_GROUPS = {0, 1, 2, 3};

    // ------------------------------------------------------------------ render shape

    @Test void theCosmeticStonesAreDrawnAsTheirModel() {
        for (int group : COSMETIC_GROUPS) {
            BlockCosmeticSolid block = BlockCosmeticSolid.group(group, properties());
            assertEquals(RenderShape.MODEL, block.defaultBlockState().getRenderShape(),
                    "the cosmetic solid of the group " + group + " is drawn as its model");
        }
    }

    @Test void theEldritchStonesAreDrawnAsTheirModel() {
        for (int group : ELDRITCH_GROUPS) {
            BlockEldritch block = BlockEldritch.group(group, properties());
            assertEquals(RenderShape.MODEL, block.defaultBlockState().getRenderShape(),
                    "the eldritch stone of the group " + group + " is drawn as its model");
        }
    }

    @Test void theDeviceFamiliesAndTheMagicBoxAreDrawnAsTheirModel() {
        assertEquals(RenderShape.MODEL, new BlockMetalDevice(properties()).defaultBlockState().getRenderShape(),
                "the metal device is drawn as its model");
        assertEquals(RenderShape.MODEL, new BlockStoneDevice(properties()).defaultBlockState().getRenderShape(),
                "the stone device is drawn as its model");
        assertEquals(RenderShape.MODEL, new BlockWoodenDevice(properties()).defaultBlockState().getRenderShape(),
                "the wooden device is drawn as its model");
        assertEquals(RenderShape.MODEL, new BlockMagicBox(properties()).defaultBlockState().getRenderShape(),
                "the magic box is drawn as its model");
        assertEquals(RenderShape.MODEL, new BlockArcaneDoor(properties()).defaultBlockState().getRenderShape(),
                "the arcane door is drawn as its model");
        assertEquals(RenderShape.MODEL, new BlockArcanePressurePlate(properties()).defaultBlockState().getRenderShape(),
                "the arcane pressure plate is drawn as its model");
        assertEquals(RenderShape.MODEL, new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_AMBER, properties())
                        .defaultBlockState().getRenderShape(),
                "the cosmetic opaque is drawn as its model");
    }

    @Test void theBlocksTheOriginalDidNotDrawStayInvisible() {
        // The render type minus one of the original is "not drawn at all".
        assertEquals(RenderShape.INVISIBLE, new BlockEldritchNothing(properties()).defaultBlockState().getRenderShape(),
                "the eldritch nothing is not drawn, the tile renderer draws the void");
        assertEquals(RenderShape.INVISIBLE, new BlockEldritchPortal(properties()).defaultBlockState().getRenderShape(),
                "the eldritch gate is not drawn, the tile renderer draws the gate");
        assertEquals(RenderShape.INVISIBLE, new BlockMirror(properties()).defaultBlockState().getRenderShape(),
                "the mirror is not drawn, the tile renderer draws the pane");
        for (int group : AIRY_GROUPS) {
            assertEquals(RenderShape.INVISIBLE, BlockAiry.group(group, properties()).defaultBlockState().getRenderShape(),
                    "the airy block of the group " + group + " is not drawn");
        }
    }

    // ------------------------------------------------------- the block entity contract

    @Test void theCosmeticSolidCarriesTheTileOfTheWardingStoneAndOfTheNodeOnly() {
        // OpenTC4 BlockCosmeticSolid:259-265 - the original created the tile for the metadata
        // three and for the metadata eight only.
        for (int group : COSMETIC_GROUPS) {
            BlockCosmeticSolid block = BlockCosmeticSolid.group(group, properties());
            for (int variant : EldritchBlockRules.cosmeticGroupVariants(group)) {
                boolean expected = variant == EldritchBlockRules.COSMETIC_WARDING_STONE
                        || variant == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED;
                assertEquals(expected, hasTile(block, variant),
                        "the cosmetic solid of the metadata " + variant + " and its block entity");
            }
        }
    }

    @Test void theCosmeticOpaqueCarriesTheTileOfTheWardedGlassOnly() {
        // OpenTC4 BlockCosmeticOpaque:190-192 and :221-223 - the original created the owned
        // tile for the metadata two only.
        BlockCosmeticOpaque amber = new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_AMBER, properties());
        assertFalse(hasTile(amber, 0), "the amber block has no tile");
        assertFalse(hasTile(amber, 1), "the amber brick has no tile");
        BlockCosmeticOpaque warded = new BlockCosmeticOpaque(BlockCosmeticOpaque.GROUP_WARDED, properties());
        assertTrue(hasTile(warded, EldritchBlockRules.COSMETIC_OPAQUE_OWNED), "the warded glass has its tile");
    }

    @Test void theEldritchStoneCarriesTheTileOfTheOriginalMetadataOnly() {
        // OpenTC4 BlockEldritch:120-122 - the original created the tile for the metadata zero,
        // one, three, eight, nine and ten only. The metadata eleven is the eldritch crystal of
        // the port, a block of its own in the original, and it carries its tile.
        for (int group : ELDRITCH_GROUPS) {
            BlockEldritch block = BlockEldritch.group(group, properties());
            for (int variant : EldritchBlockRules.eldritchGroupVariants(group)) {
                boolean expected = variant == EldritchBlockRules.ELDRITCH_ALTAR
                        || variant == EldritchBlockRules.ELDRITCH_OBELISK
                        || variant == EldritchBlockRules.ELDRITCH_CAPSTONE
                        || variant == EldritchBlockRules.ELDRITCH_LOCK
                        || variant == EldritchBlockRules.ELDRITCH_CRAB_SPAWNER
                        || variant == EldritchBlockRules.ELDRITCH_RUNED
                        || variant == EldritchBlockRules.ELDRITCH_CRYSTAL;
                assertEquals(expected, hasTile(block, variant),
                        "the eldritch stone of the metadata " + variant + " and its block entity");
            }
        }
    }

    @Test void theEldritchNothingCarriesTheTileOfTheExposedFillerOnly() {
        // OpenTC4 BlockEldritchNothing:61-63 - the original created the tile for the metadata
        // one only, the exposed filler.
        BlockEldritchNothing block = new BlockEldritchNothing(properties());
        assertFalse(hasTileState(block, exposed(block, 0)), "the covered filler has no tile");
        assertTrue(hasTileState(block, exposed(block, 1)), "the exposed filler has its tile");
    }

    @Test void theAiryFamilyCarriesTheTileOfTheWardingFenceOnly() {
        // OpenTC4 BlockAiry:275-289 - the original created a tile for the metadata zero, one
        // and five; the port owns the warding fence only, the node, the nitor and the energised
        // node are native blocks of their own.
        for (int group : AIRY_GROUPS) {
            BlockAiry block = BlockAiry.group(group, properties());
            for (int variant : EldritchBlockRules.airyGroupVariants(group)) {
                assertEquals(variant == EldritchBlockRules.AIRY_WARDING_FENCE, hasTile(block, variant),
                        "the airy block of the metadata " + variant + " and its block entity");
            }
        }
    }

    @Test void theStatesWithoutATileAreTheOnesThePistonMoves() {
        // The piston asks the state, not the block: PistonBaseBlock#isPushable answers
        // !blockState.hasBlockEntity(). The answer of the port is the answer of the original
        // hasTileEntity(int), so the states that never had a tile are moved by a piston.
        // The original family of the cosmetic stones has sixteen metadata values and two of them
        // carry a tile, so fourteen of them are moved.
        int cosmeticMoved = 0;
        for (int group : COSMETIC_GROUPS) {
            BlockCosmeticSolid block = BlockCosmeticSolid.group(group, properties());
            for (int variant : EldritchBlockRules.cosmeticGroupVariants(group)) {
                if (!hasTile(block, variant)) ++cosmeticMoved;
            }
        }
        assertEquals(14, cosmeticMoved, "the cosmetic stones the piston moves");

        // The original family of the eldritch stones has the metadata zero to ten, six of them
        // carry a tile, so five of them are moved. The metadata eleven is the crystal of the
        // port and it carries its tile.
        int eldritchMoved = 0;
        for (int group : ELDRITCH_GROUPS) {
            BlockEldritch block = BlockEldritch.group(group, properties());
            for (int variant : EldritchBlockRules.eldritchGroupVariants(group)) {
                if (!hasTile(block, variant)) ++eldritchMoved;
            }
        }
        assertEquals(5, eldritchMoved, "the eldritch stones the piston moves");

        // The eldritch nothing is moved while it is covered and it stands still while it is
        // exposed, because the exposed filler is the one that holds the tile of the void.
        BlockEldritchNothing nothing = new BlockEldritchNothing(properties());
        assertTrue(hasTileState(nothing, exposed(nothing, 1)), "the exposed filler holds the tile of the void");
        assertFalse(hasTileState(nothing, exposed(nothing, 0)), "the covered filler holds no tile");
    }

    @Test void theMixinThatReadsTheContractIsRegistered() {
        // The contract only reaches the vanilla code through the common mixin, so the mixin has
        // to be part of the common mixin configuration of both loaders.
        JsonObject config = mixinConfig();
        assertTrue(config.getAsJsonArray("mixins").toString().contains("PerStateBlockEntityMixin"),
                "the common mixin configuration names the mixin of the block entity contract");
    }

    // ------------------------------------------------------------------ selection shape

    @Test void theStonesAreSelectedAsTheFullCubeOfTheOriginal() {
        // OpenTC4 BlockCosmeticSolid:165-167 - the original sets the bounds of the stone to the
        // full block, and so does the eldritch stone (OpenTC4 BlockEldritch:110-113).
        for (int group : COSMETIC_GROUPS) {
            BlockCosmeticSolid block = BlockCosmeticSolid.group(group, properties());
            for (int variant : EldritchBlockRules.cosmeticGroupVariants(group)) {
                assertEquals(Shapes.block(), stateOf(block, variant).getShape(null, BlockPos.ZERO),
                        "the selection box of the cosmetic solid of the metadata " + variant);
            }
        }
        for (int group : ELDRITCH_GROUPS) {
            BlockEldritch block = BlockEldritch.group(group, properties());
            for (int variant : EldritchBlockRules.eldritchGroupVariants(group)) {
                assertEquals(Shapes.block(), stateOf(block, variant).getShape(null, BlockPos.ZERO),
                        "the selection box of the eldritch stone of the metadata " + variant);
            }
        }
    }

    @Test void theFillerWithoutASelectionBoxHasNone() {
        // OpenTC4 BlockEldritchNothing:69-71 and BlockEldritchPortal:80-89 - the original gives
        // the eldritch nothing and the eldritch gate no selection box at all.
        assertEquals(Shapes.empty(), new BlockEldritchNothing(properties()).defaultBlockState().getShape(null, BlockPos.ZERO),
                "the eldritch nothing has no selection box");
        assertEquals(Shapes.empty(), new BlockEldritchPortal(properties()).defaultBlockState().getShape(null, BlockPos.ZERO),
                "the eldritch gate has no selection box");
    }

    // ------------------------------------------------------------------ helpers

    /** The properties of a block of the port. */
    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of();
    }

    /** True when the state of the given original metadata carries a block entity. */
    private static boolean hasTile(Block block, int variant) {
        return hasTileState(block, stateOf(block, variant));
    }

    /** True when the given state carries a block entity. */
    private static boolean hasTileState(Block block, BlockState state) {
        assertTrue(block instanceof PerStateBlockEntity,
                "the block " + block + " does not answer the question of the block entity per state");
        return ((PerStateBlockEntity) block).hasBlockEntityForState(state);
    }

    /** The state of the eldritch nothing with the given exposure. */
    private static BlockState exposed(BlockEldritchNothing block, int exposed) {
        return block.defaultBlockState().setValue(BlockEldritchNothing.EXPOSED, exposed);
    }

    /** The state of the given original metadata of the given block. */
    private static BlockState stateOf(Block block, int variant) {
        BlockState state = block.defaultBlockState();
        Property<?> property = FamilyVariant.of(state);
        // A group with a single metadata value carries no property, the default state is it.
        if (property == null) return state;
        return FamilyVariant.with(state, variant);
    }

    /** The common mixin configuration of the port. */
    private static JsonObject mixinConfig() {
        try (InputStream stream = BlockVisibilityAndPistonTest.class.getClassLoader()
                .getResourceAsStream("thaumcraft-common.mixins.json")) {
            assertNotNull(stream, "the common mixin configuration is missing");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError("the common mixin configuration cannot be read", failure);
        }
    }
}
