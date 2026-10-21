package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.entities.OriginalPlayerSearch;

/**
 * Original TileEldritchCrabSpawner: the eldritch crab spawner. It counts down
 * and releases an eldritch crab when the countdown reaches zero.
 *
 * <p>Oracle {@code TileEldritchCrabSpawner:24-57} - the countdown runs on the server only,
 * the vent of the spawner runs on the client only. The server announces the vent with a
 * block event of the type one, the client answers it with a vent of twenty ticks.
 */
public final class TileEldritchCrabSpawner extends BlockEntity {
    /** Original TileEldritchTrap:23 - the countdown of the spawner. */
    public int count = 150;
    public int ticks = 0;
    int venting = 0;
    byte facing = 0;
    /** The client tick of the last vent step, so that the vent runs once per tick. */
    private long lastClientTick = Long.MIN_VALUE;

    /** Original TileEldritchCrabSpawner:37 - the block event that starts the vent. */
    public static final int VENT_EVENT = 1;
    /** Original TileEldritchCrabSpawner:47-55 - the vent lasts twenty ticks. */
    public static final int VENT_TICKS = 20;
    /** Original TileEldritchCrabSpawner:85 - the spawner needs a player within sixteen blocks. */
    public static final double ACTIVATION_RADIUS = 16.0D;
    /** Original TileEldritchCrabSpawner:80 - at most five crabs may live around the spawner. */
    public static final int MAX_CRABS = 5;
    /** Original TileEldritchCrabSpawner:67 - the colour of the vent particles, 0x9988AA. */
    public static final int VENT_COLOUR = 0x9988AA;
    /** Original TileEldritchCrabSpawner:44 - the sound of the spawning crab. */
    public static final SoundEvent GORE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "gore"));

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
            // TileEldritchCrabSpawner:37 - the block event starts the vent of the spawner.
            level.blockEvent(pos, state.getBlock(), VENT_EVENT, 0);
            // TileEldritchCrabSpawner:38 - the warning fizz of the coming spawn.
            level.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                    net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1.0F);
        }
        if (spawner.count <= 0 && spawner.isActivated(level) && !spawner.maxEntitiesReached(level)) {
            spawner.count = 150 + level.getRandom().nextInt(100);
            spawner.spawnCrab(level, pos);
            // TileEldritchCrabSpawner:44 - the original plays the sound of the gore.
            level.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                    GORE, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1.0F);
        }
    }

    /**
     * Original TileEldritchCrabSpawner:70-77 - the client answers the block event of the
     * spawner with a vent of twenty ticks.
     */
    @Override public boolean triggerEvent(int id, int data) {
        if (id == VENT_EVENT) {
            venting = VENT_TICKS;
            return true;
        }
        return super.triggerEvent(id, data);
    }

    /** Original TileEldritchCrabSpawner:47 - the ticks the vent still runs. */
    public int getVenting() { return venting; }

    /** The client side countdown of the vent. */
    public void clientVentTick() { if (venting > 0) venting--; }

    /**
     * True once per client tick.
     *
     * <p>Oracle {@code TileEldritchCrabSpawner:47-55} runs the vent in the tick of the tile
     * entity, but the 1.21.1 eldritch block ticks its tile on the server only, so the client
     * side of the vent is driven by the renderer, which calls this once per client tick.
     */
    public boolean clientTick(Level level) {
        long time = level.getGameTime();
        if (time == lastClientTick) return false;
        lastClientTick = time;
        return true;
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
        // Original TileEldritchCrabSpawner:96 - the crab of the spawner never wears the helm.
        if (crab instanceof thaumcraft.common.entities.monster.EntityEldritchCrab eldritch) eldritch.setHelm(false);
        level.addFreshEntity(crab);
    }

    /** Original TileEldritchCrabSpawner:117 - the spawner needs a player nearby. */
    private boolean isActivated(Level level) {
        // Original TileEldritchCrabSpawner:85 - the original looks for the closest player
        // within sixteen blocks; the search is a sphere and a creative player is never found.
        return OriginalPlayerSearch.closestPlayer(level, worldPosition.getX() + .5D, worldPosition.getY() + .5D,
                worldPosition.getZ() + .5D, ACTIVATION_RADIUS) != null;
    }

    /** Original TileEldritchCrabSpawner:106-111 - at most five crabs may live nearby. */
    private boolean maxEntitiesReached(Level level) {
        int count = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(32.0D, 32.0D, 32.0D))) {
            if (entity.getType() == TileEldritchAltar.entityType("eldritch_crab")) count++;
        }
        return count > MAX_CRABS;
    }

    /**
     * Original TileEldritchCrabSpawner:118-124 - the spawner keeps its direction and nothing
     * else: the countdown of the original is not written into the saved data, so a reloaded
     * spawner starts again with its hundred and fifty ticks.
     */
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = tag.getByte("facing");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", facing);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
