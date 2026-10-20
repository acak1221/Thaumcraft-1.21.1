package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsProvider;

/**
 * Original TileEldritchAltar: the eldritch altar. It counts the eldritch eyes
 * that were placed on it and spawns cultists once it is complete.
 */
public final class TileEldritchAltar extends BlockEntity {
    /** The original entity of the four ritual clerics (oracle TileEldritchAltar:150). */
    private static final String CULTIST_CLERIC_ID = "cultist_cleric";
    /** The original entity of the cultist knights (oracle TileEldritchAltar:110). */
    private static final String CULTIST_KNIGHT_ID = "cultist_knight";
    /** The original entity of the eldritch guardian (oracle TileEldritchAltar:134). */
    private static final String ELDRITCH_GUARDIAN_ID = "eldritch_guardian";
    /** Original TileEldritchAltar:171 - a cleric is bound to the altar with a radius of eight. */
    private static final int CLERIC_HOME_RADIUS = 8;
    /** Original TileEldritchAltar:130,147 - a knight and a guardian are bound with a radius of sixteen. */
    private static final int GUARD_HOME_RADIUS = 16;

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
     *
     * <p>The 1.7 original answered whether a labyrinth already covered the chunk of the
     * altar and started a {@code MazeThread} when it did not. The native port reads the
     * same labyrinth cells from the saved data of the world and seeds a new labyrinth of
     * the same size when the altar stands outside of every labyrinth.
     */
    public boolean checkForMaze() {
        if (!(level instanceof ServerLevel server)) return true;
        int chunkX = getBlockPos().getX() >> 4;
        int chunkZ = getBlockPos().getZ() >> 4;
        net.minecraft.util.RandomSource random = server.getRandom();
        int width = EldritchBlockRules.altarMazeSize(random.nextInt(8));
        int height = EldritchBlockRules.altarMazeSize(random.nextInt(8));
        OuterLandsProvider.MazeSavedData maze = OuterLandsProvider.maze(server);
        if (maze.mazesInRange(chunkX, chunkZ, width, height)) return true;
        long seed = random.nextLong();
        MazeGenerator generator = new MazeGenerator(width, height, seed);
        while (!generator.generate()) generator = new MazeGenerator(width, height, ++seed);
        maze.storeLabyrinth(generator, chunkX, chunkZ);
        return false;
    }

    /** Original TileEldritchAltar:92-107 - the spawn timer of the altar. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchAltar altar) {
        // The original counter only ran while the altar was a spawner, so the first
        // spawn of a fresh altar comes eighty ticks after its activation.
        if (level.isClientSide || !altar.isSpawner()) return;
        altar.counter++;
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
        EntityType<?> type = entityType(CULTIST_CLERIC_ID);
        if (type == null) return;
        int success = 0;
        for (int a = 0; a < 4; a++) {
            int xx = EldritchBlockRules.altarClericOffsetX(a);
            int zz = EldritchBlockRules.altarClericOffsetZ(a);
            BlockPos spot = new BlockPos(pos.getX() + xx, pos.getY() - 1, pos.getZ() + zz);
            if (!level.getBlockState(spot).isAir()) continue;
            Entity entity = type.create(level);
            if (!(entity instanceof Mob mob)) continue;
            mob.moveTo(pos.getX() + .5D + xx, pos.getY(), pos.getZ() + .5D + zz, 0.0F, 0.0F);
            mob.restrictTo(pos, CLERIC_HOME_RADIUS);
            originalSpawnEquipment(mob, level, pos);
            if (!level.addFreshEntity(mob)) continue;
            success++;
            // Original TileEldritchAltar:190 - the cleric that reached the altar is a ritualist.
            if (mob instanceof EntityCultistCleric cleric) cleric.setIsRitualist(true);
        }
        if (success > EldritchBlockRules.ALTAR_CLERICS_REQUIRED) {
            spawnedClerics = true;
            setChanged();
        }
    }

    /** Original TileEldritchAltar:110-132 - the cultist knights. */
    private void spawnGuards(Level level, BlockPos pos) {
        EntityType<?> type = entityType(CULTIST_KNIGHT_ID);
        if (type == null) return;
        if (countNearby(level, pos, EntityCultistCleric.class) < 1) {
            setSpawner(false);
            return;
        }
        if (countNearby(level, pos, EntityCultist.class) >= EldritchBlockRules.ALTAR_MAX_CULTISTS) return;
        net.minecraft.util.RandomSource random = level.getRandom();
        int x = pos.getX() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        int y = pos.getY() + random.nextInt(4) * (random.nextInt(3) - 1) - 1;
        int z = pos.getZ() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        BlockPos spot = new BlockPos(x, y, z);
        if (!level.getBlockState(spot).isAir()) return;
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) return;
        mob.moveTo(x + .5D, y, z + .5D, 0.0F, 0.0F);
        mob.restrictTo(pos, GUARD_HOME_RADIUS);
        originalSpawnEquipment(mob, level, pos);
        level.addFreshEntity(mob);
    }

    /** Original TileEldritchAltar:134-148 - the eldritch guardian. */
    private void spawnGuardian(Level level, BlockPos pos) {
        EntityType<?> type = entityType(ELDRITCH_GUARDIAN_ID);
        if (type == null) return;
        net.minecraft.util.RandomSource random = level.getRandom();
        int x = pos.getX() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        int y = pos.getY() + random.nextInt(4) * (random.nextInt(3) - 1) - 1;
        int z = pos.getZ() + (4 + random.nextInt(7)) * (random.nextInt(3) - 1);
        BlockPos spot = new BlockPos(x, y, z);
        if (!level.getBlockState(spot).isAir()) return;
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) return;
        mob.moveTo(x + .5D, y, z + .5D, 0.0F, 0.0F);
        mob.restrictTo(pos, GUARD_HOME_RADIUS);
        level.addFreshEntity(mob);
    }

    /**
     * The original spawn equipment of a cultist. The 1.7 original called
     * {@code onSpawnWithEgg} on every cultist that the altar spawned, which gave it the
     * crimson robe of its rank.
     */
    private static void originalSpawnEquipment(Entity entity, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        if (!(entity instanceof EntityCultist cultist)) return;
        cultist.originalSpawnEquipment(server, server.getCurrentDifficultyAt(pos));
    }

    /**
     * Counts the entities of a kind that are close to the altar. The 1.7 original counted
     * the ritual clerics and the cultists, never the praetor, which is no cultist.
     */
    private int countNearby(Level level, BlockPos pos, Class<? extends Entity> type) {
        return level.getEntitiesOfClass(type, new AABB(pos).inflate(24.0D, 16.0D, 24.0D)).size();
    }

    /** Looks up an entity type by its original registry path. */
    static EntityType<?> entityType(String path) {
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path));
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
