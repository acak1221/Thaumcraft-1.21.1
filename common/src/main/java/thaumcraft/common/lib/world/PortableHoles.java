package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.tiles.TileHole;

/** Original ItemFocusPortableHole.createHole and the blacklist, as the world side of the Portable Hole. */
public final class PortableHoles {
    /**
     * Original portableHoleBlackList (beds, pistons and their heads, the wooden door and, from the default configuration, the iron
     * door, the flux goo and gas; the eldritch blocks and the arcane furnace are not ported yet), as a data tag so that it can be
     * extended as the configuration could. Every door of 1.21.1 is in it, not only the two of 1.7.
     */
    public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "portable_hole_blacklist"));

    private PortableHoles() {}

    /** Original loop condition of the focus: what may stand in the way when the depth is measured (a block with a tile may). */
    public static boolean passable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.is(BLACKLIST) && !state.is(Blocks.BEDROCK) && !state.is(ThaumcraftHoles.HOLE_BLOCK.get())
                && !state.isAir() && state.getDestroySpeed(level, pos) != -1.0F;
    }

    /**
     * Original createHole: a loaded, ordinary block with no tile, not blacklisted, not bedrock, not already a hole, not air or anything a
     * block could be placed over (plants, fluids, snow) and not unbreakable becomes a hole that remembers it and lasts {@code duration}
     * ticks; {@code count} blocks of a chain are left from here along {@code side} (-1: none). The neighbours are not updated.
     */
    public static boolean create(ServerLevel level, BlockPos pos, int side, int count, int duration) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null || !passable(level, pos, state) || state.canBeReplaced()) return false;
        level.setBlock(pos, ThaumcraftHoles.HOLE_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (!(level.getBlockEntity(pos) instanceof TileHole hole)) return false;
        hole.configure(state, duration, count, side);
        return true;
    }
}
