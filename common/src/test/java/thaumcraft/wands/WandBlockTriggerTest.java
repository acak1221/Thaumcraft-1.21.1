package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.items.wands.ItemWandCasting;

/**
 * The public block trigger registry of the wand.
 *
 * <p>Original {@code thaumcraft/common/items/wands/ItemWandCasting.java:389-390} asks the public
 * {@code WandTriggerRegistry} right after the {@code IWandable} contract and returns its answer, so
 * an add-on can make a wand react to a block of another mod. The native wand does the same; the
 * registry itself is keyed by a block or by one exact block state, because 1.21.1 has no block
 * metadata.</p>
 */
class WandBlockTriggerTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void aRegisteredTriggerIsFoundForEveryStateOfItsBlock() {
        int[] seen = {-1};
        IWandTriggerManager manager = (level, wand, player, x, y, z, side, event) -> {
            seen[0] = event;
            return true;
        };
        // A block of its own, so that the static registry of the other tests cannot interfere.
        WandTriggerRegistry.registerWandBlockTrigger(manager, 4, Blocks.COBBLESTONE, "wanddispatchtest");
        assertTrue(ItemWandCasting.hasBlockTrigger(Blocks.COBBLESTONE.defaultBlockState()),
                "the wand asks the registry about the clicked block");
        assertTrue(ItemWandCasting.hasBlockTrigger(Blocks.COBBLESTONE.defaultBlockState()));
        assertFalse(ItemWandCasting.hasBlockTrigger(Blocks.CLAY.defaultBlockState()),
                "a block without a trigger is not one");
        assertTrue(WandTriggerRegistry.performTrigger(null, net.minecraft.world.item.ItemStack.EMPTY, null,
                1, 2, 3, 0, Blocks.COBBLESTONE.defaultBlockState()));
        assertTrue(seen[0] == 4, "the first successful trigger wins");
    }
}
