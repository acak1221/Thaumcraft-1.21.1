package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.Utils;

/**
 * The part of {@link ThaumcraftApiHelper#isWoodLog(net.minecraft.world.level.Level, BlockPos)} that needs no data pack, and
 * the guards of the tree helpers. The log rule is membership of the block tag {@code #minecraft:logs}, which a bare
 * Minecraft bootstrap does not bind, so here only what is NOT a log can be checked (a leaf block, which an earlier version
 * of the rule counted, planks, air); the rule for the logs themselves, the plain oak log included, is checked with the real
 * tags in a running world (GameTest {@code knowledgeApiWoodLogRule}), and so is the walk of {@code breakFurthestBlock}
 * (GameTests {@code knowledgeApiFurthestBlock*}).
 */
class TreeFellingApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void aLeafBlockIsNotALogWhateverTheWood() {
        for (Block block : new Block[] {Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES, Blocks.AZALEA_LEAVES}) {
            assertFalse(Utils.isWoodLogState(block.defaultBlockState()), block + " is a leaf block: it sustains no leaves and is no log");
        }
    }

    @Test void blocksThatAreNeitherLeavesNorLogsAreNotLogsAndNullIsNotAState() {
        for (Block block : new Block[] {Blocks.AIR, Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.BEDROCK, Blocks.OAK_SAPLING, Blocks.CRAFTING_TABLE, Blocks.GOLD_BLOCK, Blocks.CHEST}) {
            assertFalse(Utils.isWoodLogState(block.defaultBlockState()), block + " is no log");
        }
        assertFalse(Utils.isWoodLogState(null));
    }

    @Test void theHelpersAnswerFalseOrNullForMissingArguments() {
        assertFalse(ThaumcraftApiHelper.isWoodLog(null, BlockPos.ZERO));
        assertFalse(ThaumcraftApiHelper.breakFurthestBlock(null, BlockPos.ZERO, Blocks.BIRCH_LOG, null));
        assertNull(ThaumcraftApiHelper.findFurthestBlock(null, BlockPos.ZERO, Blocks.BIRCH_LOG));
        assertNull(ThaumcraftApiHelper.findFurthestBlock(null, null, null));
    }
}
