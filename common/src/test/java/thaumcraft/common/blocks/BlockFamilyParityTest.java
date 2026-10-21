package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.BlockFamilyShapes;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * The parity of the TC4 4.2.3.5 block families with the 1.21.1 port.
 *
 * <p>Every number and every box below is taken from the decompiled oracle
 * ({@code /home/Acak/work/thaumcraft-4.2.3.5-oracle}) and the file and the line
 * of the oracle are named in the comment of the check. The boxes are read in the
 * 0..1 block units of Minecraft 1.21.1.</p>
 */
class BlockFamilyParityTest {

    // ------------------------------------------------------------- eldritch

    /**
     * BlockEldritchNothing:51 - the original calls {@code setLightLevel(0.2)} and the 1.7.10
     * setter stores {@code (int)(15 * 0.2)} = 3, so the blank filler emits the light level three.
     */
    @Test void theEldritchNothingEmitsTheOriginalLightLevel() {
        assertEquals(3, EldritchBlockRules.ELDRITCH_NOTHING_LIGHT);
    }

    /**
     * BlockEldritchNothing:49 - the setter value 6000000 is stored three times and the 1.7.10
     * getter divides it by five, so the effective blast resistance is 3600000.
     */
    @Test void theEldritchNothingKeepsItsBlastResistance() {
        assertEquals(3600000.0F, EldritchBlockRules.ELDRITCH_NOTHING_RESISTANCE);
    }

    /**
     * BlockCrystal:149-151 - the eldritch crystal drops one balanced shard, the original
     * itemShard metadata six. BlockEldritch:190 drops nothing for every other metadata.
     */
    @Test void onlyTheEldritchCrystalDropsTheBalancedShard() {
        assertTrue(EldritchBlockRules.eldritchDropsBalancedShard(EldritchBlockRules.ELDRITCH_CRYSTAL));
        assertFalse(EldritchBlockRules.eldritchDropsBalancedShard(EldritchBlockRules.ELDRITCH_ALTAR));
        assertFalse(EldritchBlockRules.eldritchDropsBalancedShard(EldritchBlockRules.ELDRITCH_GLYPHED));
        assertFalse(EldritchBlockRules.eldritchDropsBalancedShard(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        // The glyphed stone drops the knowledge fragment and the glowing crust itself.
        assertTrue(EldritchBlockRules.eldritchDropsResource(EldritchBlockRules.ELDRITCH_GLYPHED));
        assertTrue(EldritchBlockRules.eldritchDropsSelf(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
    }

    // --------------------------------------------------------------- devices

    /**
     * TileGrate:65-67 - the metal device metadata five is the OPEN filter: it takes an item
     * only while it is five. BlockMetalDevice:711-720 - a powered filter becomes six and an
     * unpowered one five, so six is the closed one.
     */
    @Test void theMetalDeviceFilterIsOpenOnFiveAndClosedOnSix() {
        assertEquals(5, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN);
        assertEquals(6, EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED);
        assertEquals(EldritchBlockRules.DEVICE_KIND_GRATE, EldritchBlockRules.metalDeviceKind(5));
        assertEquals(EldritchBlockRules.DEVICE_KIND_GRATE, EldritchBlockRules.metalDeviceKind(6));
    }

    /**
     * BlockMetalDevice:264-268 - both filter metadata values keep the same selection box, the
     * metal plate across the top three sixteenths of the block.
     */
    @Test void bothFilterVariantsKeepTheirSelectionBox() {
        var plate = BlockFamilyShapes.itemFilterPlate().bounds();
        assertFalse(BlockFamilyShapes.itemFilterPlate().isEmpty(), "the filter keeps its plate");
        assertEquals(0.8125D, plate.minY, 0.0D, "the plate is the top three sixteenths");
        assertEquals(1.0D, plate.maxY, 0.0D);
        assertEquals(0.0D, plate.minX, 0.0D);
        assertEquals(1.0D, plate.maxX, 0.0D);
    }

    // --------------------------------------------------------- arcane plate

    /**
     * BlockWoodenDevice:249-256 - the box of the plate is inset by a sixteenth on every side,
     * the unpressed plate is a sixteenth high and the pressed one half of that.
     */
    @Test void theArcanePressurePlateKeepsTheOriginalBox() {
        assertEquals(0.0625D, EldritchBlockRules.PLATE_BOX_INSET, 0.0D);
        assertEquals(0.0625D, EldritchBlockRules.PLATE_BOX_HEIGHT, 0.0D);
        assertEquals(0.03125D, EldritchBlockRules.PLATE_BOX_HEIGHT_PRESSED, 0.0D);

        var open = BlockFamilyShapes.arcanePressurePlate(EldritchBlockRules.PLATE_BOX_HEIGHT).bounds();
        assertEquals(0.0625D, open.minX, 0.0D, "the plate is inset by a sixteenth");
        assertEquals(0.0D, open.minY, 0.0D);
        assertEquals(0.0625D, open.minZ, 0.0D);
        assertEquals(0.9375D, open.maxX, 0.0D);
        assertEquals(0.0625D, open.maxY, 0.0D, "the unpressed plate is a sixteenth high");
        assertEquals(0.9375D, open.maxZ, 0.0D);

        var pressed = BlockFamilyShapes.arcanePressurePlate(EldritchBlockRules.PLATE_BOX_HEIGHT_PRESSED).bounds();
        assertEquals(0.03125D, pressed.maxY, 0.0D, "the pressed plate is half as high");
        assertEquals(0.0625D, pressed.minX, 0.0D);
        assertEquals(0.9375D, pressed.maxX, 0.0D);
    }

    // ---------------------------------------------------------------- mirror

    /**
     * BlockMirror:168-175 - the original adds the clicked side to the metadata of the item, so
     * the mirror looks at the side the player clicked and hangs on the wall behind it.
     * BlockMirror:268-295 - its plate is the thin square on that wall side.
     */
    @Test void theMirrorHangsOnTheWallBehindIt() {
        for (Direction facing : Direction.values()) {
            Direction wall = EldritchBlockRules.mirrorWallSide(facing);
            assertNotNull(wall);
            assertEquals(facing.getOpposite(), wall, "the wall of a mirror that looks " + facing);
        }
        assertEquals(0.0625D, EldritchBlockRules.MIRROR_PLATE_WIDTH, 0.0D);

        for (Direction wall : Direction.values()) {
            var box = BlockFamilyShapes.mirrorPlate(wall).bounds();
            if (wall == Direction.DOWN) {
                assertEquals(0.0D, box.minY, 0.0D, "the plate at the bottom of the block");
                assertEquals(0.0625D, box.maxY, 0.0D);
            } else if (wall == Direction.UP) {
                assertEquals(0.9375D, box.minY, 0.0D, "the plate at the top of the block");
                assertEquals(1.0D, box.maxY, 0.0D);
            } else if (wall == Direction.NORTH) {
                assertEquals(0.0D, box.minZ, 0.0D, "the plate at the north side of the block");
                assertEquals(0.0625D, box.maxZ, 0.0D);
                assertEquals(1.0D, box.maxX, 0.0D);
            } else if (wall == Direction.SOUTH) {
                assertEquals(0.9375D, box.minZ, 0.0D, "the plate at the south side of the block");
                assertEquals(1.0D, box.maxZ, 0.0D);
            } else if (wall == Direction.WEST) {
                assertEquals(0.0D, box.minX, 0.0D, "the plate at the west side of the block");
                assertEquals(0.0625D, box.maxX, 0.0D);
            } else {
                assertEquals(0.9375D, box.minX, 0.0D, "the plate at the east side of the block");
                assertEquals(1.0D, box.maxX, 0.0D);
            }
        }
    }
}
