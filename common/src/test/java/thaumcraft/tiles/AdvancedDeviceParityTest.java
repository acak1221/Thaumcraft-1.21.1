package thaumcraft.tiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.blocks.BlockFluxGas;
import thaumcraft.common.blocks.BlockFluxGoo;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.tiles.TileParityRules;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileMirror;

/**
 * The numbers and the state transitions of the advanced devices of the M09 lane, checked
 * against the decompiled TC4 4.2.3.5 original (the 1.7.10 oracle). Every assertion names the
 * original behaviour it protects.
 *
 * <p>These tests were AUTHORED but NOT EXECUTED: the wave forbids a Gradle run, so the
 * leader has to run {@code :common:test} before the verdicts are trusted.</p>
 */
class AdvancedDeviceParityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ----------------------------------------------------------------------------------------------
    // Flux scrubber (oracle thaumcraft/common/tiles/TileFluxScrubber.java)
    // ----------------------------------------------------------------------------------------------

    /**
     * Oracle TileFluxScrubber:59-62 - the scrubber matches the flux MATERIAL. The flux goo
     * (oracle BlockFluxGoo:32) and the flux gas (oracle BlockFluxGas:28) are the two blocks
     * that use {@code Config.fluxGoomaterial} (oracle Config:876), so the original removed
     * both. The native tree knew the flux goo only.
     */
    @Test void theFluxScrubberRemovesTheFluxGooAndTheFluxGas() {
        BlockState goo = new BlockFluxGoo(BlockBehaviour.Properties.of()).defaultBlockState();
        BlockState gas = new BlockFluxGas(BlockBehaviour.Properties.of()).defaultBlockState();
        assertTrue(TileParityRules.fluxScrubberRemoves(goo), "the flux goo is flux");
        assertTrue(TileParityRules.fluxScrubberRemoves(gas), "the flux gas shares the flux material of the original");
        assertFalse(TileParityRules.fluxScrubberRemoves(Blocks.AIR.defaultBlockState()));
        assertFalse(TileParityRules.fluxScrubberRemoves(Blocks.STONE.defaultBlockState()));
    }

    /** Oracle TileFluxScrubber:36-53,58-60 - four charges, at most four essentia, ten AIR vis. */
    @Test void theFluxScrubberKeepsItsOriginalNumbers() {
        assertEquals(4, EldritchBlockRules.FLUX_CHARGE_COST);
        assertEquals(4, EldritchBlockRules.FLUX_ESSENTIA_MAX);
        assertEquals(4, EldritchBlockRules.FLUX_ESSENTIA_CHANCE);
        assertEquals(5, EldritchBlockRules.FLUX_POWER_MIN);
        assertEquals(10, EldritchBlockRules.FLUX_POWER_DRAIN);
        assertEquals(16, EldritchBlockRules.FLUX_SCAN_DISTANCE);
        assertEquals(16, EldritchBlockRules.FLUX_SCAN_BATCH);
        assertEquals(5, EldritchBlockRules.FLUX_POWER_PER_BLOCK);
    }

    /**
     * Oracle TileFluxScrubber:89 - only the flux whose squared distance to the scrubber is
     * below the squared radius of sixteen is cleaned, so the corner of the cube is out.
     */
    @Test void theFluxScrubberOnlyReachesTheRadiusOfSixteen() {
        assertTrue(EldritchBlockRules.fluxScrubberInRange(0, 0, 0));
        assertTrue(EldritchBlockRules.fluxScrubberInRange(15, 0, 0), "fifteen blocks away is inside");
        assertFalse(EldritchBlockRules.fluxScrubberInRange(16, 0, 0), "sixteen blocks away is outside");
        assertFalse(EldritchBlockRules.fluxScrubberInRange(15, 15, 0), "the corner of the cube is outside");
    }

    /**
     * Oracle TileFluxScrubber:92 - a flux block loses one quantum, the block at the lowest
     * quantum vanishes.
     */
    @Test void aScrubbedFluxBlockLosesOneQuantum() {
        assertEquals(6, EldritchBlockRules.fluxScrubberReducedLevel(7));
        assertEquals(0, EldritchBlockRules.fluxScrubberReducedLevel(1));
        assertEquals(-1, EldritchBlockRules.fluxScrubberReducedLevel(0), "the lowest quantum vanishes");
    }

    // ----------------------------------------------------------------------------------------------
    // Node stabilisers (oracle thaumcraft/common/tiles/TileNodeStabilizer.java and TileNode.java)
    // ----------------------------------------------------------------------------------------------

    /**
     * Oracle TileNodeStabilizer:14 - the lock is one for the stone device of the ninth
     * metadata and two for the device of the tenth metadata. Oracle TileNode:885-889 reads
     * that lock from the block below the node, so the advanced stabiliser locks its node
     * with two.
     */
    @Test void theAdvancedNodeStabilizerLocksItsNodeWithTwo() {
        assertEquals(1, TileParityRules.nodeStabilizerLock(true, false), "the basic stabiliser locks with one");
        assertEquals(2, TileParityRules.nodeStabilizerLock(false, true), "the advanced stabiliser locks with two");
        assertEquals(0, TileParityRules.nodeStabilizerLock(false, false), "no stabiliser, no lock");
    }

    /**
     * Oracle TileNode:644-650 - a node that is locked with one regenerates twice as fast, a
     * node that is locked with two regenerates twenty times as fast.
     */
    @Test void aLockedNodeRegeneratesFasterTheStrongerTheLockIs() {
        assertEquals(400, TileParityRules.nodeRegenerationInterval(400, 0));
        assertEquals(800, TileParityRules.nodeRegenerationInterval(400, 1));
        assertEquals(8000, TileParityRules.nodeRegenerationInterval(400, 2));
    }

    /**
     * Oracle TileNode:781 - the roll that repairs a fading node of a locked node is 12500
     * divided by the lock.
     */
    @Test void theFadingRepairOfALockedNodeUsesTheDivisorOfItsLock() {
        assertEquals(12500, TileParityRules.nodeFadingRepairBound(1));
        assertEquals(6250, TileParityRules.nodeFadingRepairBound(2));
        assertEquals(0, TileParityRules.nodeFadingRepairBound(0), "an unlocked node never repairs itself");
    }

    // ----------------------------------------------------------------------------------------------
    // Mirrors (oracle thaumcraft/common/tiles/TileMirror.java and ItemHandMirror.java)
    // ----------------------------------------------------------------------------------------------

    /**
     * Oracle TileMirror:207 and ItemHandMirror:147 - the item that leaves a mirror gets a
     * portal cooldown of twenty ticks. The 1.7.10 field is {@code Entity.timeUntilPortal}
     * (the 1.7.10 name of {@code field_71088_bW}), never the pick up delay.
     */
    @Test void theItemOfAMirrorOnlyWaitsForThePortalCooldown() {
        assertEquals(20, TileParityRules.MIRROR_PORTAL_COOLDOWN);
        assertEquals(20, EquipmentRules.MIRROR_ITEM_LIFETIME);
    }

    /**
     * Oracle TileMirror:330-344 - the slot byte and the stack share one compound, so the
     * fields of the stack sit at the top of the entry and the reader parses the entry
     * itself. The native tree wrote a nested "stack" compound and read the entry, so every
     * buffered item was lost on the next save.
     */
    @Test void theBufferedItemsOfTheMirrorSurviveASaveAndLoad() {
        RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ItemStack stack = new ItemStack(Items.DIRT, 7);
        CompoundTag entry = TileMirror.saveOutputStack(stack, 0, registries);
        assertEquals(0, entry.getByte("Slot"), "the original keeps the slot byte in the entry");
        assertTrue(entry.contains("id"), "the stack fields sit at the top of the entry");
        ItemStack read = TileMirror.loadOutputStack(entry, registries);
        assertEquals(Items.DIRT, read.getItem());
        assertEquals(7, read.getCount());
    }

    /**
     * Oracle TileMirror:290,297-300,310 - the ejection rate is the square of the instability
     * over fifty, nothing leaves the mirror in its first twenty ticks and the instability
     * falls by one every twentieth tick.
     */
    @Test void theMirrorEjectsAndDecaysLikeTheOriginal() {
        assertEquals(0, EldritchBlockRules.mirrorEjectTickRate(0));
        assertEquals(0, EldritchBlockRules.mirrorEjectTickRate(49));
        assertEquals(1, EldritchBlockRules.mirrorEjectTickRate(50));
        assertEquals(4, EldritchBlockRules.mirrorEjectTickRate(100));
        assertEquals(20, EldritchBlockRules.MIRROR_EJECT_DELAY);
        assertEquals(20, EldritchBlockRules.MIRROR_INSTABILITY_DECAY_PERIOD);
        assertEquals(1, EldritchBlockRules.mirrorInstabilityVisPayment(5), "at most one ORDER vis per call");
        assertEquals(0, EldritchBlockRules.mirrorInstabilityVisPayment(0));
    }

    // ----------------------------------------------------------------------------------------------
    // Magic box (oracle thaumcraft/common/tiles/TileMagicBox.java)
    // ----------------------------------------------------------------------------------------------

    /**
     * Oracle TileMagicBox:24-26 - the box owns twenty seven slots, every box of the chain
     * adds another twenty seven.
     */
    @Test void theMagicBoxGrowsByTwentySevenSlotsPerBox() {
        assertEquals(27, EldritchBlockRules.magicBoxSize(0));
        assertEquals(54, EldritchBlockRules.magicBoxSize(1));
        assertEquals(27 * 27, EldritchBlockRules.magicBoxSize(26));
        assertEquals(64, EldritchBlockRules.MAGIC_BOX_MAX_STACK);
    }

    /**
     * Oracle TileMagicBox:291-305 - the search of the neighbouring boxes stops after a
     * thousand and twenty four boxes.
     */
    @Test void theMagicBoxSearchStopsAfterAThousandAndTwentyFourBoxes() {
        assertEquals(1024, EldritchBlockRules.MAGIC_BOX_MAX_LINKED);
        assertNotEquals(1023, EldritchBlockRules.MAGIC_BOX_MAX_LINKED);
    }
}
