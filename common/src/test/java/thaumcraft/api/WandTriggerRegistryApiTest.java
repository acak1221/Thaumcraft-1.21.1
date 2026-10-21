package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.api.wands.WandTriggerRegistry;

/**
 * The public block trigger registry of the original.
 *
 * <p>Original {@code thaumcraft/api/wands/WandTriggerRegistry.java:25-86} (CFR 0.152 decompilation of
 * {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original keys a trigger by the pair of a block and a
 * metadata, answers a query with the exact registration OR the wildcard registration of the whole
 * block ({@code -1}), and dispatches the same two forms in the same order. The native registry keys a
 * trigger either by one exact block state or by the whole block, because 1.21.1 has no block metadata;
 * the wildcard registration of the whole block is the block keyed form.</p>
 *
 * <p>Forced 1.21.1 changes: the metadata argument of the original survives in the signature and is
 * ignored, and the state keyed registration replaces the metadata keyed one.</p>
 */
class WandTriggerRegistryApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theAnswerAndTheDispatchAgreeOnTheBlockKeyedRegistration() {
        int[] seen = {-1};
        IWandTriggerManager manager = (level, wand, player, x, y, z, side, event) -> {
            seen[0] = event;
            return true;
        };
        // A mod id of its own, so that the static registry of the other tests cannot interfere.
        WandTriggerRegistry.registerWandBlockTrigger(manager, 7, Blocks.COBBLESTONE, "w2api-blockkeyed");
        BlockState state = Blocks.COBBLESTONE.defaultBlockState();
        assertTrue(WandTriggerRegistry.hasTrigger(state),
                "the original answers a wildcard registration of the whole block");
        assertTrue(WandTriggerRegistry.hasTrigger(state, "w2api-blockkeyed"),
                "the original per-mod answer finds the wildcard registration too");
        assertTrue(WandTriggerRegistry.hasTrigger(Blocks.COBBLESTONE, 3, "w2api-blockkeyed"),
                "the original per-mod block and metadata answer finds the wildcard registration");
        assertTrue(WandTriggerRegistry.performTrigger(null, ItemStack.EMPTY, null, 1, 2, 3, 0, state));
        assertTrue(seen[0] == 7, "the first successful trigger wins");
    }

    @Test void aBlockWithoutARegistrationHasNoTrigger() {
        assertFalse(WandTriggerRegistry.hasTrigger(Blocks.CLAY.defaultBlockState(), "w2api-blockkeyed"),
                "a block without a registration is not one");
        assertFalse(WandTriggerRegistry.hasTrigger(Blocks.CLAY, 0, "w2api-blockkeyed"),
                "the original block and metadata answer of an unregistered block is false");
    }

    @Test void theOriginalBlockAndMetadataDispatchIsRestored() {
        int[] seen = {-1};
        IWandTriggerManager manager = (level, wand, player, x, y, z, side, event) -> {
            seen[0] = event;
            return true;
        };
        WandTriggerRegistry.registerWandBlockTrigger(manager, 9, Blocks.CLAY, 2, "w2api-metadata");
        assertTrue(WandTriggerRegistry.performTrigger(null, ItemStack.EMPTY, null, 1, 2, 3, 0, Blocks.CLAY, 2),
                "the original block and metadata dispatch reaches the registration");
        assertTrue(seen[0] == 9, "the first successful trigger wins");
    }
}
