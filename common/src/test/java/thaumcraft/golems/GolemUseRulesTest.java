package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * The rules of the Use core of the original.
 *
 * <p>Oracle {@code AIUseItem:135,155,190} and {@code GuiGolem:206-236}: the three
 * switches of the Use core are the toggles 0, 1 and 2 of the golem — "Block or
 * Empty space", "Right click or Left click" and "Not sneaking or Sneaking" — and
 * the goal uses whatever item the golem carries on whatever marked cell the
 * switch accepts.</p>
 *
 * <p>No block, item or entity is constructed here: the rules are pure.</p>
 */
class GolemUseRulesTest {
    @Test void theBlockSwitchAcceptsOnlyTheCellItNames() {
        // The default: a block.
        GolemUseOptions block = new GolemUseOptions(false, false, false);
        assertFalse(block.acceptsTarget(false), "the default rejects an empty space");
        assertTrue(block.acceptsTarget(true), "the default accepts a block");
        // The switch: an empty space.
        GolemUseOptions space = new GolemUseOptions(true, false, false);
        assertTrue(space.acceptsTarget(true), "the empty space mode accepts an empty space");
        assertFalse(space.acceptsTarget(false), "the empty space mode rejects a block");
    }

    @Test void theEmptySpaceModeClicksTheCellBehindTheMarkedOne() {
        // Oracle AIUseItem:142-146 — with an empty space the click is offset by the
        // opposite of the marked side.
        GolemMarker marker = new GolemMarker(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                new BlockPos(10, 20, 30), Direction.NORTH);
        assertEquals(new BlockPos(10, 20, 30), marker.clickPos(false), "the block mode clicks the marked cell");
        assertEquals(new BlockPos(10, 20, 31), marker.clickPos(true), "the empty space mode clicks the cell behind");
    }

    @Test void theThreeSwitchesAreIndependent() {
        for (int bits = 0; bits < 8; bits++) {
            GolemUseOptions options = new GolemUseOptions((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0);
            assertEquals((bits & 1) != 0, options.emptySpace(), "the block switch");
            assertEquals((bits & 2) != 0, options.leftClick(), "the click switch");
            assertEquals((bits & 4) != 0, options.sneaking(), "the sneaking switch");
            assertTrue(options.isSupported(), "every combination is an ordinary Use mode");
        }
    }
}
