package thaumcraft.neoforge.testing;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native event-bus regression: permission checks publish intent without temporarily replacing the world. */
@PrefixGameTestTemplate(false)
public final class NeoForgeInteractionGameTests {
    @GameTest(template = "empty", templateNamespace = "thaumcraft", batch = "neoforge_interactions")
    public void neoEqualTradeProtectionSeesProposedBlock(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(8, 100, 8));
        var original = level.getBlockState(pos);
        var player = test.makeMockServerPlayerInLevel();
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        BlockState[] source = {Blocks.STONE.defaultBlockState()};
        BlockState[] replacement = {Blocks.GOLD_BLOCK.defaultBlockState()};
        int[] calls = {0};
        Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
            if (event.getLevel() != level || !event.getPos().equals(pos)) return;
            ++calls[0];
            test.assertTrue(event.getEntity() == player, "the native place event retains the actual player");
            test.assertTrue(event.getState() == replacement[0] && event.getPlacedBlock() == replacement[0],
                    "both native event state getters expose the proposed replacement");
            test.assertTrue(event.getBlockSnapshot().getState() == source[0]
                            && event.getBlockSnapshot().getCurrentState() == source[0]
                            && level.getBlockState(pos) == source[0],
                    "the real snapshot and world retain the source throughout the permission callback");
            if (event.getPlacedBlock().is(Blocks.GOLD_BLOCK)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, BlockEvent.EntityPlaceEvent.class, listener);
        try {
            level.setBlock(pos, source[0], Block.UPDATE_CLIENTS);
            test.assertTrue(!allowsSwap(level, player, pos, source[0], replacement[0]),
                    "a listener registered for EntityPlaceEvent can deny the proposed gold replacement");
            test.assertTrue(calls[0] == 1 && level.getBlockState(pos) == source[0],
                    "the cancelled preflight emits one event and never changes the source block");

            source[0] = Blocks.GOLD_BLOCK.defaultBlockState();
            replacement[0] = Blocks.STONE.defaultBlockState();
            level.setBlock(pos, source[0], Block.UPDATE_CLIENTS);
            test.assertTrue(allowsSwap(level, player, pos, source[0], replacement[0]),
                    "a permitted stone replacement is not rejected because its source is gold");
            test.assertTrue(calls[0] == 2 && level.getBlockState(pos) == source[0],
                    "the permitted preflight also emits one event without changing the world");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            level.setBlock(pos, original, Block.UPDATE_CLIENTS);
        }
        test.succeed();
    }

    /** Exercise the installed bridge itself without exposing a production permission API for tests. */
    private static boolean allowsSwap(ServerLevel level, ServerPlayer player, BlockPos pos,
                                      BlockState before, BlockState after) {
        try {
            var method = Class.forName("thaumcraft.neoforge.NeoForgeInteractions").getDeclaredMethod("allowsSwap",
                    ServerLevel.class, ServerPlayer.class, BlockPos.class, BlockState.class, BlockState.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, level, player, pos, before, after);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException("NeoForge swap permission callback failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot invoke the native NeoForge swap bridge", failure);
        }
    }
}
