package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileEldritchCrabSpawner: the eldritch crab spawner. It counts down
 * and releases an eldritch crab when the countdown reaches zero.
 */
public final class TileEldritchCrabSpawner extends BlockEntity {
    /** Original TileEldritchCrabSpawner:23 - the countdown of the spawner. */
    public int count = 150;
    public int ticks = 0;
    int venting = 0;
    byte facing = 0;

    public TileEldritchCrabSpawner(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    public byte getFacing() { return facing; }

    public void setFacing(byte face) {
        this.facing = face;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original TileEldritchCrabSpawner:30-70 - the spawner countdown. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchCrabSpawner spawner) {
        if (spawner.ticks == 0) spawner.ticks = level.getRandom().nextInt(500);
        spawner.ticks++;
        if (level.isClientSide) return;
        spawner.count--;
        if (spawner.count < 0) {
            spawner.count = 50 + level.getRandom().nextInt(50);
            return;
        }
        if (spawner.count == 15 && spawner.isActivated(level) && !spawner.maxEntitiesReached(level)) {
            // TileEldritchCrabSpawner:50 - the warning fizz of the coming spawn.
            level.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                    net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1.0F);
        }
        if (spawner.count <= 0 && spawner.isActivated(level) && !spawner.maxEntitiesReached(level)) {
            spawner.count = 150 + level.getRandom().nextInt(100);
            spawner.spawnCrab(level, pos);
            level.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                    net.minecraft.sounds.SoundEvents.GENERIC_HURT, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1.0F);
        }
    }

    /** Original TileEldritchCrabSpawner:117-129 - the crab leaves through the facing side. */
    private void spawnCrab(Level level, BlockPos pos) {
        EntityType<?> type = TileEldritchAltar.entityType("eldritch_crab");
        if (type == null) return;
        Direction dir = Direction.from3DDataValue(facing);
        Entity crab = type.create(level);
        if (crab == null) return;
        crab.moveTo(pos.getX() + dir.getStepX() + .5D, pos.getY() + dir.getStepY() + .5D, pos.getZ() + dir.getStepZ() + .5D, 0.0F, 0.0F);
        crab.setDeltaMovement(dir.getStepX() * .2F, dir.getStepY() * .2F, dir.getStepZ() * .2F);
        level.addFreshEntity(crab);
    }

    /** Original TileEldritchCrabSpawner:117 - the spawner needs a player nearby. */
    private boolean isActivated(Level level) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                worldPosition.getX() + .5D - 16.0D, worldPosition.getY() + .5D - 16.0D, worldPosition.getZ() + .5D - 16.0D,
                worldPosition.getX() + .5D + 16.0D, worldPosition.getY() + .5D + 16.0D, worldPosition.getZ() + .5D + 16.0D);
        return !level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, box).isEmpty();
    }

    /** Original TileEldritchCrabSpawner:106-111 - at most five crabs may live nearby. */
    private boolean maxEntitiesReached(Level level) {
        int count = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(32.0D, 32.0D, 32.0D))) {
            if (entity.getType() == TileEldritchAltar.entityType("eldritch_crab")) count++;
        }
        return count > 5;
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = tag.getByte("facing");
        count = tag.contains("count") ? tag.getInt("count") : 150;
        ticks = tag.getInt("ticks");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", facing);
        tag.putInt("count", count);
        tag.putInt("ticks", ticks);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
