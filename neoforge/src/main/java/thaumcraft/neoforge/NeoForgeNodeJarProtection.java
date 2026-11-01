package thaumcraft.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.common.lib.NodeJarFormation;

/** Native claim preflight for the bounded node-jar transfer, before any world mutation. */
public final class NeoForgeNodeJarProtection implements NodeJarFormation.Protection {
    private static final NeoForgeNodeJarProtection INSTANCE = new NeoForgeNodeJarProtection();
    private NeoForgeNodeJarProtection() {}

    public static void init() { NodeJarFormation.installProtection(INSTANCE); }

    @Override public boolean breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before) {
        var event = new BlockEvent.BreakEvent(level, pos, before, player);
        // Architectury receives this native event; emitting the shared event here as well
        // would ask every shared protection twice for the same cell.
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override public boolean placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before,
                                        BlockState after, InteractionHand hand, Direction face) {
        var event = new ProposedPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos), before, after, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    /** Keep the real source snapshot and level while both state getters report the intended owner. */
    private static final class ProposedPlaceEvent extends BlockEvent.EntityPlaceEvent {
        private final BlockState replacement;

        private ProposedPlaceEvent(BlockSnapshot snapshot, BlockState against, BlockState replacement, ServerPlayer player) {
            super(snapshot, against, player);
            this.replacement = replacement;
        }

        @Override public BlockState getState() { return replacement; }
        @Override public BlockState getPlacedBlock() { return replacement; }
    }
}
