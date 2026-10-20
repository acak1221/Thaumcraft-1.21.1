package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.blocks.BlockArcaneLamp;
import thaumcraft.common.config.ThaumcraftLamps;
import thaumcraft.common.lib.ArcaneLampRules;

/** Original light lamp: one triangular random candidate per server tick, without fuel, vis or a redstone pause. */
public final class TileArcaneLamp extends BlockEntity {
    public TileArcaneLamp(BlockPos pos, BlockState state) { super(ThaumcraftLamps.ARCANE_LAMP_TILE.get(), pos, state); }
    public Direction getFacing() { return getBlockState().getValue(BlockArcaneLamp.FACING); }

    public static void tick(Level level, BlockPos pos, BlockState state, TileArcaneLamp tile) {
        if (level instanceof ServerLevel && tile.level == level && !tile.isRemoved() && level.getBlockEntity(pos) == tile
                && tile.worldPosition.equals(pos) && level.getBlockState(pos) == state) tile.spreadLight(level.random);
    }

    /** Native one-tick action with an injectable RNG for exact offset and refusal checks. */
    public boolean spreadLight(RandomSource random) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || isRemoved()
                || server.getBlockEntity(worldPosition) != this || !getBlockState().is(ThaumcraftLamps.ARCANE_LAMP.get())) return false;
        int x = worldPosition.getX() + random.nextInt(16) - random.nextInt(16);
        int y = worldPosition.getY() + random.nextInt(16) - random.nextInt(16);
        int z = worldPosition.getZ() + random.nextInt(16) - random.nextInt(16);
        BlockPos column = new BlockPos(x, worldPosition.getY(), z);
        if (!server.hasChunkAt(column) || !server.getWorldBorder().isWithinBounds(column)) return false;
        y = ArcaneLampRules.candidateY(y, server.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), server.getMinBuildHeight());
        BlockPos candidate = new BlockPos(x, y, z);
        BlockState existing = server.getBlockState(candidate);
        if (server.isOutsideBuildHeight(candidate) || !existing.isAir() || existing.is(ThaumcraftLamps.ARCANE_LIGHT.get())
                || !ArcaneLampRules.darkEnough(server.getRawBrightness(candidate, 0))) return false;
        return server.setBlock(candidate, ThaumcraftLamps.ARCANE_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    /** The original removes every lamp light in this cube, including another lamp's lights, with no source ownership field. */
    public void removeLights() {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return;
        int radius = ArcaneLampRules.REMOVE_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-radius, -radius, -radius), worldPosition.offset(radius, radius, radius))) {
            if (!server.isOutsideBuildHeight(pos) && server.getBlockState(pos).is(ThaumcraftLamps.ARCANE_LIGHT.get())) server.removeBlock(pos, false);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("orientation", getFacing().get3DDataValue());
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
