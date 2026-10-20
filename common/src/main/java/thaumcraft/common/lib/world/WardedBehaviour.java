package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockWarded;
import thaumcraft.common.tiles.TileWarded;

/** Read-only remembered-state queries and scoped original entity callbacks; the actual ward stays authoritative. */
public final class WardedBehaviour {
    private record Callback(Level level, BlockPos pos, BlockState wrapper, Callback previous) {}
    private static final ThreadLocal<Callback> CALLBACK = new ThreadLocal<>();
    private WardedBehaviour() {}

    /** Null for ordinary, missing or quarantined wards. Never invent a restorable stone state. */
    public static BlockState original(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof BlockWarded)) return null;
        if (!(level.getBlockEntity(pos) instanceof TileWarded tile) || !tile.validWard()) return null;
        BlockState original = tile.originalState();
        return original.getBlock() instanceof BlockWarded ? null : original;
    }

    /** For the native callers that replaced Forge's canSustainPlant/isBeaconBase/getEnchantPowerBonus hooks with state/tag reads. */
    public static BlockState query(BlockGetter level, BlockPos pos) {
        BlockState original = original(level, pos);
        return original == null ? level.getBlockState(pos) : original;
    }

    public static void invoke(Level level, BlockPos pos, BlockState original, Runnable action) {
        if (original == null || original.getBlock() instanceof BlockWarded || original(level, pos) != original) return;
        Callback previous = CALLBACK.get();
        for (Callback row = previous; row != null; row = row.previous) {
            if (row.level == level && row.pos.equals(pos)) return;
        }
        CALLBACK.set(new Callback(level, pos.immutable(), level.getBlockState(pos), previous));
        try { action.run(); }
        finally { if (previous == null) CALLBACK.remove(); else CALLBACK.set(previous); }
    }

    /** Redstone ore's original attack/step callback writes its lit state: that must not strip the player's protection. */
    public static boolean deniesCallbackReplacement(Level level, BlockPos pos, BlockState replacement) {
        for (Callback row = CALLBACK.get(); row != null; row = row.previous) {
            if (row.level == level && row.pos.equals(pos) && level.getBlockState(pos) == row.wrapper && replacement != row.wrapper) return true;
        }
        return false;
    }
}
