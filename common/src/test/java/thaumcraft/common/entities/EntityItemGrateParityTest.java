package thaumcraft.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The grate aware item of the original (TC4 4.2.3.5) against the 1.21.1 port.
 *
 * <p>Every number below is a value of the decompiled original; the file and the line
 * of the 1.7.10 oracle are named in the comment of the check. No block and no
 * entity is constructed here: a constructed block leaves an unregistered intrusive
 * holder behind and breaks the registry freeze of the whole test suite, so every
 * rule is a pure function.</p>
 */
class EntityItemGrateParityTest {

    /**
     * The 1.7.10 {@code EntityItem.onUpdate} calls the overridden push out with the
     * <b>middle</b> of the bounding box of the item, not with the position of its
     * feet: {@code this.j(this.posX, (this.boundingBox.minY + this.boundingBox.maxY) / 2.0,
     * this.posZ)}. The cell of the block the original asks is therefore the cell of
     * that middle.
     */
    @Test void theCellOfTheItemIsTheCellOfTheMiddleOfItsBoundingBox() {
        // The item of the filter is dropped 0.6 above the floor of the block
        // (oracle TileGrate:29) and its box is a quarter of a block high.
        assertEquals(0.725D, EntityItemGrate.cellHeight(0.6D, 0.85D), 1.0E-9D,
                "the 1.7.10 EntityItem.onUpdate passes (minY + maxY) / 2 to the push out");
        assertEquals(BlockPos.containing(0.5D, 0.725D, 0.5D),
                EntityItemGrate.grateCell(0.5D, 0.6D, 0.85D, 0.5D),
                "the cell of the item of the filter is the block of the filter itself");
    }

    /**
     * The original answers {@code true} from the overridden push out when the block
     * at the cell of the item is the metal device of the fifth or the sixth metadata
     * (oracle EntityItemGrate:26-36). The 1.7.10 {@code EntityItem.onUpdate} stores
     * that answer in {@code Entity.field_70145_X}, the field the MCP mapping names
     * {@code noClip} - "whether this entity won't clip with collision or not" - and
     * the 1.7.10 {@code Entity.moveEntity} skips the collision of the move when the
     * flag is set. The item of the filter therefore falls through the plate of the
     * filter; it is not pushed out of it.
     */
    @Test void theItemOfTheFilterDoesNotClipWhileItIsInsideTheGrate() {
        assertTrue(EntityItemGrate.noClipInsideGrate(true),
                "the original answers true inside the grate, the item does not clip");
        assertFalse(EntityItemGrate.noClipInsideGrate(false),
                "outside the grate the item clips as every other item");
    }

    /**
     * The original returns <b>before</b> the vanilla push out is called
     * (oracle EntityItemGrate:31-35), so the vanilla push out never runs for the
     * item of the filter and it never changes the motion of the item. The 1.7.10
     * vanilla push out is the method that picks the closest side of the cell and
     * sets the motion of the entity to {@code random.nextFloat() * 0.2f + 0.1f} in
     * the direction of that side.
     */
    @Test void theVanillaPushOutNeverRunsForTheItemOfTheFilter() {
        assertFalse(EntityItemGrate.appliesVanillaPushOut(true),
                "the original returns before the vanilla push out inside the grate");
        assertTrue(EntityItemGrate.appliesVanillaPushOut(false),
                "outside the grate the original delegates to the vanilla push out");
    }
}
