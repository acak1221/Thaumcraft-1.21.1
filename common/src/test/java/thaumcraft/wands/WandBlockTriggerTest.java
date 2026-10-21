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
        // The registry is a global static and both the answer and the dispatch of the wand are not
        // mod scoped, so this test needs a block that no other test of the suite registers. COBBLESTONE
        // is registered by WandTriggerRegistryApiTest and CLAY as well, and the dispatch of the first
        // successful trigger would then answer with the event of the other test.
        WandTriggerRegistry.registerWandBlockTrigger(manager, 4, Blocks.SAND, "wanddispatchtest");
        assertTrue(ItemWandCasting.hasBlockTrigger(Blocks.SAND.defaultBlockState()),
                "the wand asks the registry about the clicked block");
        assertTrue(ItemWandCasting.hasBlockTrigger(Blocks.SAND.defaultBlockState()));
        assertFalse(ItemWandCasting.hasBlockTrigger(Blocks.GRAVEL.defaultBlockState()),
                "a block without a trigger is not one");
        assertTrue(WandTriggerRegistry.performTrigger(null, net.minecraft.world.item.ItemStack.EMPTY, null,
                1, 2, 3, 0, Blocks.SAND.defaultBlockState()));
        assertTrue(seen[0] == 4, "the first successful trigger wins");
    }
}
