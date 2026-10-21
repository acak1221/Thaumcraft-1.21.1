package thaumcraft.blocks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import thaumcraft.common.blocks.OriginalPistonPushable;
import thaumcraft.common.blocks.OriginalPistonRule;
import thaumcraft.common.blocks.PerStateBlockEntity;

/**
 * The pushability of the blocks of Thaumcraft in the piston of the 1.7.10 original.
 *
 * <p>The original asked the question once per class of block and never per metadata: the vanilla
 * 1.7.10 {@code net.minecraft.block.BlockPistonBase#canPushBlock} ended with
 * {@code return !(block instanceof ITileEntityProvider);}. Every {@code BlockContainer} of the
 * original implements that interface, so a block of such a family was never moved by the piston,
 * for any of its metadata, and a block of a family whose class was a plain {@code Block} was moved
 * for every metadata. The move destroyed the tile of the metadata that carried one: the 1.7.10
 * {@code net.minecraft.tileentity.TileEntityPiston} carried the block and the metadata only, so
 * the block that arrived had no tile at all.
 *
 * <p>Minecraft 1.21.1 asks the question per state: {@code
 * net.minecraft.world.level.block.piston.PistonBaseBlock#isPushable} ends with
 * {@code return !blockState.hasBlockEntity();}, and {@code BlockState#hasBlockEntity} is
 * {@code block instanceof EntityBlock} (Minecraft 1.21.1
 * {@code net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase}:552-554). The port
 * therefore answers the question of the original with the contract {@link
 * OriginalPistonPushable}, and the common mixin {@code
 * thaumcraft.common.mixin.PistonPushabilityMixin} asks it.
 *
 * <p>The test reads the rule through the pure function that the production code calls, so it
 * builds no block: a constructed block leaves an unregistered intrusive holder in the registry
 * and breaks the registry freeze of the whole test suite.
 */
class PistonPushabilityTest {

    // ------------------------------------------------------------------ the families of the original

    @Test void theCosmeticStonesWereMovedByThePistonOfTheOriginal() {
        // Oracle BlockCosmeticSolid:60-61 - the 1.7.10 class extended net.minecraft.block.Block
        // and did not implement ITileEntityProvider, so the piston of the original moved the
        // sixteen stones of the family. The warding stone (the metadata three, the tile at
        // BlockCosmeticSolid:278-296) and the node (the metadata eight) were moved as well, and
        // the move destroyed the tile of the two.
        for (int metadata = 0; metadata < 16; metadata++) {
            assertTrue(pushable(OriginalPistonRule.Family.COSMETIC_SOLID, metadata),
                    "the cosmetic stone of the metadata " + metadata + " was moved by the piston of the original");
        }
    }

    @Test void theEldritchFillerWasMovedByThePistonOfTheOriginal() {
        // Oracle BlockEldritchNothing:42-43 - the 1.7.10 class extended net.minecraft.block.Block
        // and did not implement ITileEntityProvider, so the piston of the original moved the
        // filler for every metadata, the exposed filler (the metadata one, the tile at
        // BlockEldritchNothing:81-87) included, and the move destroyed its tile.
        for (int metadata = 0; metadata < 2; metadata++) {
            assertTrue(pushable(OriginalPistonRule.Family.ELDRITCH_NOTHING, metadata),
                    "the eldritch filler of the metadata " + metadata + " was moved by the piston of the original");
        }
    }

    @Test void theEldritchGateWasMovedByThePistonOfTheOriginal() {
        // Oracle BlockEldritchPortal:38-39 - the 1.7.10 class extended net.minecraft.block.Block
        // and did not implement ITileEntityProvider, so the piston of the original moved the gate,
        // whose tile existed for every metadata (BlockEldritchPortal:115-121).
        for (int metadata = 0; metadata < 2; metadata++) {
            assertTrue(pushable(OriginalPistonRule.Family.ELDRITCH_PORTAL, metadata),
                    "the eldritch gate of the metadata " + metadata + " was moved by the piston of the original");
        }
    }

    @Test void theSilverwoodNodeWasMovedByThePistonOfTheOriginal() {
        // Oracle BlockMagicalLog:50-51 - the 1.7.10 class extended
        // net.minecraft.block.BlockRotatedPillar and did not implement ITileEntityProvider, so the
        // piston of the original moved the log, the silverwood node (the metadata two, the tile
        // at BlockMagicalLog:160-170) included, and the move destroyed the tile of the node.
        assertTrue(pushable(OriginalPistonRule.Family.SILVERWOOD_NODE, 2),
                "the silverwood node was moved by the piston of the original");
    }

    @Test void theManaPodWasMovedByThePistonOfTheOriginal() {
        // Oracle BlockManaPod:51-52 - the 1.7.10 class extended net.minecraft.block.Block and did
        // not implement ITileEntityProvider, so the piston of the original moved the pod, whose
        // tile existed for every metadata (BlockManaPod:232-238).
        for (int metadata = 0; metadata < 8; metadata++) {
            assertTrue(pushable(OriginalPistonRule.Family.MANA_POD, metadata),
                    "the mana pod of the age " + metadata + " was moved by the piston of the original");
        }
    }

    @Test void theEtherealBloomWasMovedByThePistonOfTheOriginal() {
        // Oracle BlockCustomPlant:55-56 - the 1.7.10 class extended net.minecraft.block.BlockBush
        // and did not implement ITileEntityProvider, so the piston of the original moved the
        // plant, the ethereal bloom (the metadata four, the tile at BlockCustomPlant:97-108)
        // included, and the move destroyed the tile of the bloom.
        assertTrue(pushable(OriginalPistonRule.Family.CUSTOM_PLANT, 4),
                "the ethereal bloom was moved by the piston of the original");
    }

    @Test void theAmberBlocksWereNeverMovedByThePistonOfTheOriginal() {
        // Oracle BlockCosmeticOpaque:58-59 - the 1.7.10 class extended
        // net.minecraft.block.BlockContainer, which implements ITileEntityProvider, so the piston
        // of the original never moved the family. The amber block and the amber brick carry no
        // tile (BlockCosmeticOpaque:214-218), and they were refused as well.
        for (int metadata = 0; metadata < 3; metadata++) {
            assertFalse(pushable(OriginalPistonRule.Family.COSMETIC_OPAQUE, metadata),
                    "the cosmetic opaque of the metadata " + metadata + " was never moved by the piston of the original");
        }
    }

    @Test void theEldritchStonesWereNeverMovedByThePistonOfTheOriginal() {
        // Oracle BlockEldritch:60-61 - the 1.7.10 class extended
        // net.minecraft.block.BlockContainer, which implements ITileEntityProvider, so the piston
        // of the original never moved the family. The stones without a tile
        // (BlockEldritch:151-153) were refused as well.
        for (int metadata = 0; metadata < 12; metadata++) {
            assertFalse(pushable(OriginalPistonRule.Family.ELDRITCH, metadata),
                    "the eldritch stone of the metadata " + metadata + " was never moved by the piston of the original");
        }
    }

    @Test void theAiryFamilyWasNeverMovedByThePistonOfTheOriginal() {
        // Oracle BlockAiry:77-78 - the 1.7.10 class extended net.minecraft.block.BlockContainer,
        // which implements ITileEntityProvider, so the piston of the original never moved the
        // family. The variants without a tile (BlockAiry:313-326) were refused as well.
        for (int metadata = 0; metadata < 13; metadata++) {
            assertFalse(pushable(OriginalPistonRule.Family.AIRY, metadata),
                    "the airy block of the metadata " + metadata + " was never moved by the piston of the original");
        }
    }

    // ------------------------------------------------------------------ the contract of the port

    @Test void theBlocksThatDecideTheirBlockEntityPerStateAlsoAnswerThePistonOfTheOriginal() {
        // The block entity of a state and the pushability of a state are two questions of the
        // vanilla code, and the port answers them apart: the contract of the block entity decides
        // whether a block entity is created, and the contract of the piston decides whether the
        // piston moves the block. Every block that answers the first one has to answer the second
        // one as well, otherwise the answer of the vanilla code would move a family that the
        // original never moved.
        List<String> perState = List.of(
                "thaumcraft.common.blocks.BlockAiry",
                "thaumcraft.common.blocks.BlockCosmeticOpaque",
                "thaumcraft.common.blocks.BlockCosmeticSolid",
                "thaumcraft.common.blocks.BlockEldritch",
                "thaumcraft.common.blocks.BlockEldritchNothing");
        for (String name : perState) {
            Class<?> type = load(name);
            assertTrue(PerStateBlockEntity.class.isAssignableFrom(type),
                    name + " decides its block entity per state");
            assertTrue(OriginalPistonPushable.class.isAssignableFrom(type),
                    name + " answers the question of the piston of the original as well");
        }
    }

    @Test void theMixinThatAsksTheContractIsRegistered() {
        // The contract only reaches the vanilla code through the common mixin, so the mixin has
        // to be part of the common mixin configuration of both loaders.
        JsonObject config = mixinConfig();
        assertTrue(config.getAsJsonArray("mixins").toString().contains("PistonPushabilityMixin"),
                "the common mixin configuration names the mixin of the piston");
    }

    // ------------------------------------------------------------------ the helpers

    /** The answer of the piston of the original, read through the rule of the production code. */
    private static boolean pushable(OriginalPistonRule.Family family, int metadata) {
        return OriginalPistonRule.isPushableInOriginal(family, metadata);
    }

    /** The class of a block of the port, read without building the block. */
    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, PistonPushabilityTest.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("the class " + name + " is missing", failure);
        }
    }

    private static JsonObject mixinConfig() {
        try (InputStream stream = PistonPushabilityTest.class.getClassLoader()
                .getResourceAsStream("thaumcraft-common.mixins.json")) {
            assertNotNull(stream, "the common mixin configuration is missing");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception failure) {
            throw new AssertionError("the common mixin configuration cannot be read", failure);
        }
    }
}
