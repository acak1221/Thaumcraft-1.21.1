package thaumcraft.common.tiles;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileEldritchAltar: the eldritch altar. It counts the eldritch eyes
 * that were placed on it and spawns cultists once it is complete.
 */
public final class TileEldritchAltar extends BlockEntity {
    private boolean spawner = false;
    private boolean open = false;
    private boolean spawnedClerics = false;
    private byte spawnType = 0;
    private byte eyes = 0;
    private int counter = 0;

    public TileEldritchAltar(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    public boolean isSpawner() { return spawner; }
    public void setSpawner(boolean spawner) { this.spawner = spawner; }
    public byte getSpawnType() { return spawnType; }
    public void setSpawnType(byte spawnType) { this.spawnType = spawnType; }
    public byte getEyes() { return eyes; }
    public void setEyes(byte eyes) { this.eyes = eyes; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    /**
     * Original TileEldritchAltar:217-226 - the altar starts an eldritch maze.
     * The maze generator belongs to the Outer Lands port; until it exists the
     * altar reports a ready maze so the altar itself stays usable.
     */
    public boolean checkForMaze() { return true; }

    /** Original TileEldritchAltar:92-107 - the spawn timer of the altar. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchAltar altar) {
        altar.counter++;
        if (level.isClientSide || !altar.isSpawner()) return;
        if (altar.counter < EldritchBlockRules.ALTAR_FIRST_SPAWN_DELAY) return;
        if (altar.counter % EldritchBlockRules.ALTAR_SPAWN_PERIOD != 0) return;
        if (altar.spawnType == EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST) {
            if (!altar.spawnedClerics) altar.spawnClerics(level, pos);
            else altar.spawnGuards(level, pos);
        } else {
            altar.spawnGuardian(level, pos);
        }
    }

    /** Original TileEldritchAltar:150-191 - the four ritual clerics. */
    private void spawnClerics(Level level, BlockPos pos) {
        EntityType<?> type = entityType("cultist_cleric");
        if (type == null) return;
        int success = 0;
        for (int a = 0; a < 4; a++) {
            int xx = EldritchBlockRules.altarClericOffsetX(a);
            int zz = EldritchBlockRules.altarClericOffsetZ(a);
            BlockPos spot = new BlockPos(pos.getX() + xx, pos.getY() - 1, pos.getZ() + zz);
            if (!level.getBlockState(spot).isAir()) continue;
            Entity entity = type.create(level);
            if (entity == null) continue;
            entity.moveTo(pos.getX() + .5D + xx, pos.getY(), pos.getZ() + .5D + zz, 0.0F, 0.0F);
            // Original TileEldritchAltar:180 - the cleric anchors its home at the altar.
            prepare(entity, level, pos, CultistRules.ALTAR_CLERIC_HOME_RADIUS);
            if (!level.addFreshEntity(entity)) continue;
            success++;
            // Original TileEldritchAltar:185 - the cleric kneels at the altar as a ritualist.
            if (entity instanceof EntityCultistCleric cleric) cleric.setIsRitualist(true);
        }
        if (success > EldritchBlockRules.ALTAR_CLERICS_REQUIRED) {
            spawnedClerics = true;
            setChanged();
        }
    }

    /** Original TileEldritchAltar:110-132 - the cultist knights. */
    private void spawnGuards(Level level, BlockPos pos) {
        EntityType<?> type = entityType("cultist_knight");
        if (type == null) return;
        if (countNearby(level, pos, "cultist_cleric") < 1) {
            setSpawner(false);
            return;
        }
        if (countNearby(level, pos, "cultist") >= EldritchBlockRules.ALTAR_MAX_CULTISTS) return;
        net.minecraft.util.RandomSource random = level.getRandom();
        int x = pos.getX() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        int y = pos.getY() + random.nextInt(4) * (random.nextInt(3) - 1) - 1;
        int z = pos.getZ() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        BlockPos spot = new BlockPos(x, y, z);
        if (!level.getBlockState(spot).isAir()) return;
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.moveTo(x + .5D, y, z + .5D, 0.0F, 0.0F);
        // Original TileEldritchAltar:125-127 - the knight rolls its gear and anchors at the altar.
        prepare(entity, level, pos, CultistRules.ALTAR_GUARD_HOME_RADIUS);
        level.addFreshEntity(entity);
    }

    /** Original TileEldritchAltar:134-148 - the eldritch guardian. */
    private void spawnGuardian(Level level, BlockPos pos) {
        EntityType<?> type = entityType("eldritch_guardian");
        if (type == null) return;
        net.minecraft.util.RandomSource random = level.getRandom();
        int x = pos.getX() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        int y = pos.getY() + random.nextInt(4) * (random.nextInt(3) - 1) - 1;
        int z = pos.getZ() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        BlockPos spot = new BlockPos(x, y, z);
        if (!level.getBlockState(spot).isAir()) return;
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.moveTo(x + .5D, y, z + .5D, 0.0F, 0.0F);
        // Original TileEldritchAltar:141-144 - the guardian anchors at the altar.
        prepare(entity, level, pos, CultistRules.ALTAR_GUARD_HOME_RADIUS);
        level.addFreshEntity(entity);
    }

    /**
     * Original TileEldritchAltar:125-127 and 180 - the spawned cultist rolls its own
     * equipment, keeps its home at the altar and never despawns.
     */
    private void prepare(Entity entity, Level level, BlockPos pos, int homeRadius) {
        if (entity instanceof Mob mob) {
            mob.restrictTo(pos, homeRadius);
            mob.setPersistenceRequired();
        }
        if (entity instanceof EntityCultist cultist && level instanceof ServerLevel server)
            cultist.originalSpawnEquipment(server, server.getCurrentDifficultyAt(pos));
    }

    /** Counts the cultists that are close to the altar. */
    private int countNearby(Level level, BlockPos pos, String fragment) {
        int count = 0;
        for (Entity entity : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(24.0D, 16.0D, 24.0D))) {
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            if (path.contains(fragment)) count++;
        }
        return count;
    }

    /** Looks up an entity type by its registry path fragment. */
    static EntityType<?> entityType(String fragment) {
        for (var entry : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entry);
            if (key.getPath().contains(fragment)) return entry;
        }
        return null;
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // TileEldritchAltar:43-69 - the altar keeps the eyes, the open state and the spawner state.
        eyes = tag.getByte("eyes");
        open = tag.getBoolean("open");
        spawnedClerics = tag.getBoolean("spawnedClerics");
        spawner = tag.getBoolean("spawner");
        spawnType = tag.getByte("spawntype");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("eyes", eyes);
        tag.putBoolean("open", open);
        tag.putBoolean("spawnedClerics", spawnedClerics);
        tag.putBoolean("spawner", spawner);
        tag.putByte("spawntype", spawnType);
    }
}
