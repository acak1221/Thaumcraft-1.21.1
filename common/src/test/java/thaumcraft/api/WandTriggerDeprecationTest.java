package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import thaumcraft.api.wands.WandTriggerRegistry;

/** Compatibility overloads remain linkable but must warn callers about discarded metadata. */
class WandTriggerDeprecationTest {
    @Test void allSixMetadataOverloadsAreDeprecated() {
        int metadataOverloads = 0;
        for (var method : WandTriggerRegistry.class.getDeclaredMethods()) {
            var parameters = Arrays.asList(method.getParameterTypes());
            int blockIndex = parameters.indexOf(Block.class);
            if (blockIndex < 0 || blockIndex + 1 >= parameters.size()
                    || parameters.get(blockIndex + 1) != int.class) continue;
            assertTrue(method.isAnnotationPresent(Deprecated.class), method.toString());
            metadataOverloads++;
        }
        assertEquals(6, metadataOverloads);
    }

    @Test void stateAwareQueriesRemainTheSupportedReplacement() throws NoSuchMethodException {
        assertFalse(WandTriggerRegistry.class.getMethod("hasTrigger", BlockState.class)
                .isAnnotationPresent(Deprecated.class));
        assertFalse(WandTriggerRegistry.class.getMethod("hasTrigger", BlockState.class, String.class)
                .isAnnotationPresent(Deprecated.class));
    }
}
