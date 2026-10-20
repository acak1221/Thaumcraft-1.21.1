package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/**
 * A scannable block: the original default target of the thaumometer.
 *
 * <p>Forced 1.21.1 change: the block registry name is a {@link net.minecraft.resources.ResourceLocation},
 * so the automatic research key of a single block is that native identifier.</p>
 */
public class ScanBlock implements IScanThing {
    private final String research;
    private final Block[] blocks;

    public ScanBlock(Block block) {
        this("!" + BuiltInRegistries.BLOCK.getKey(block), block);
    }

    public ScanBlock(String research, Block... blocks) {
        this.research = research;
        this.blocks = blocks == null ? new Block[0] : blocks;
    }

    @Override public boolean checkThing(Player player, Object thing) {
        if (!(thing instanceof BlockPos pos) || player == null) return false;
        var level = player.level();
        if (level == null || !level.isInWorldBounds(pos)) return false;
        Block found = level.getBlockState(pos).getBlock();
        for (Block block : blocks) {
            if (found == block) return true;
        }
        return false;
    }

    @Override public String getResearchKey(Player player, Object thing) { return research; }

    /** The blocks this handler accepts. */
    public Block[] blocks() { return blocks.clone(); }
}
