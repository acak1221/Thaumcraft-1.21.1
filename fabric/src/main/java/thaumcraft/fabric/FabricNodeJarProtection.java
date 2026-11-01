package thaumcraft.fabric;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.NodeJarFormation;

/** Native Fabric claims see every shell/source cell before the node changes ownership. */
public final class FabricNodeJarProtection implements NodeJarFormation.Protection {
    private static final FabricNodeJarProtection INSTANCE = new FabricNodeJarProtection();
    private FabricNodeJarProtection() {}

    public static void init() { NodeJarFormation.installProtection(INSTANCE); }

    @Override public boolean breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before) {
        var tile = level.getBlockEntity(pos);
        return NodeJarFormation.commonBreak(level, player, pos, before)
                && PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, before, tile);
    }

    @Override public boolean placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before,
                                        BlockState after, InteractionHand hand, Direction face) {
        // Fabric has no native placement veto event. The original click has already consulted
        // UseBlockCallback; replaying it here would also replay item-use handlers during preflight.
        return NodeJarFormation.commonPlace(level, player, pos, after);
    }
}
