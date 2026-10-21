package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileWardingStone: the warding stone. It pushes the entities away
 * from it and seals the two blocks above it with the warding fence.
 */
public final class TileWardingStone extends BlockEntity {
    int count = 0;

    public TileWardingStone(BlockPos pos, BlockState state) {
        this(ThaumcraftEldritchBlocks.WARDING_STONE_TILE.get(), pos, state);
    }

    /**
     * The warding stone of a block that owns its own block entity type, like the
     * paving stone of warding of the legacy registration.
     */
    public TileWardingStone(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Original TileWardingStone:24 - the ward only works with a redstone signal. */
    public boolean gettingPower(Level level) {
        return level.hasNeighborSignal(worldPosition);
    }

    /** Original TileWardingStone:28-44 - the ward pushes the entities away. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileWardingStone stone) {
        if (level.isClientSide) return;
        stone.count++;
        if (!stone.gettingPower(level) && stone.count % EldritchBlockRules.WARDING_STONE_PUSH_PERIOD == 0) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0D, pos.getY() + EldritchBlockRules.WARDING_STONE_HEIGHT, pos.getZ() + 1.0D).inflate(.1D, .1D, .1D);
            for (net.minecraft.world.entity.LivingEntity entity : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
                if (!EldritchBlockRules.wardingStonePushes(entity instanceof net.minecraft.world.entity.player.Player, !entity.isAlive())) continue;
                double angle = (entity.getYRot() + 180.0F) * Math.PI / 180.0D;
                entity.push(-Math.sin(angle) * EldritchBlockRules.WARDING_STONE_PUSH, EldritchBlockRules.WARDING_STONE_PUSH_Y,
                        Math.cos(angle) * EldritchBlockRules.WARDING_STONE_PUSH);
            }
        }
        // Original TileWardingStone:47-54 - the ward seals the two blocks above the stone.
        if (stone.count % EldritchBlockRules.WARDING_STONE_SEAL_PERIOD == 0) {
            seal(level, pos, 1);
            seal(level, pos, 2);
        }
    }

    /** Original TileWardingStone:34-42 - the airy blocks above the stone are sealed. */
    private static void seal(Level level, BlockPos pos, int height) {
        BlockPos target = pos.above(height);
        BlockState state = level.getBlockState(target);
        // The original keeps an airy block of the third metadata and only replaces a replaceable block.
        if (state.getBlock() == ThaumcraftEldritchBlocks.AIRY.get()
                && thaumcraft.common.blocks.BlockAiry.variant(state) == EldritchBlockRules.AIRY_LEAVES_ALT) {
            return;
        }
        if (!state.canBeReplaced()) return;
        level.setBlock(target, thaumcraft.common.lib.world.FamilyVariant.with(ThaumcraftEldritchBlocks.AIRY.get().defaultBlockState(), EldritchBlockRules.AIRY_WARDING_FENCE), Block.UPDATE_ALL);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        count = tag.getInt("count");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("count", count);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
