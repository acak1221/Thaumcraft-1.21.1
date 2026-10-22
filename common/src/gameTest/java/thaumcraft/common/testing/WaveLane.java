package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * The shared lane of the GameTest scenarios of the new content wave.
 *
 * <p>The lane owns a real floor inside the test structure, the entities and the blocks a
 * scenario places, and a clean up that only touches what the scenario itself placed. Nothing
 * here invents a world: every block is a real server block and every entity is a real server
 * entity that the real tick machinery drives.
 */
final class WaveLane {
    /**
     * The radius of the clean up sweep of the lane, in blocks.
     *
     * <p>The vanilla GameTest grid packs the scenarios of a batch eight blocks apart and five
     * blocks of margin, so the next scenario's structure is THIRTEEN blocks away and its own
     * fixture starts nine blocks away (its base is three blocks into its structure and its floor
     * reaches four blocks further). A wider sweep reached into the neighbouring scenario and
     * discarded ITS mobs - and a golem of a neighbouring scenario is a Mob - which stopped that
     * scenario's golem for good. The lane's own footprint is a floor and a roof of four blocks
     * around its origin, so five blocks catch everything the content spawns inside the lane and
     * stop eight blocks short of the neighbour's fixture.</p>
     */
    static final int CLEAN_RADIUS = 5;

    final GameTestHelper test;
    final ServerLevel level;
    final BlockPos origin;
    final List<Entity> spawned = new ArrayList<>();
    private final List<BlockPos> placed = new ArrayList<>();
    private final List<ChunkPos> forced = new ArrayList<>();

    WaveLane(GameTestHelper test, int x, int y, int z) {
        this.test = test;
        this.level = test.getLevel();
        this.origin = test.absolutePos(new BlockPos(x, y, z));
        // A GameTest is built at a random place on every run, so the chunk of the lane is not
        // necessarily a ticking chunk. The entities and the block entities of the lane only
        // tick when their chunk is loaded and ticking, so the lane holds its own chunks.
        forced.addAll(GolemChunks.hold(level, List.of(origin)));
        boolean opened = false;
        try {
            roof(4);
            opened = true;
        } finally {
            if (!opened) clean();
        }
    }

    /** A real stone floor under the lane, so the mobs actually stand on something. */
    void floor(int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) put(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
    }

    /** A real stone roof over the lane, so the daylight never burns the mobs of the lane. */
    void roof(int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) put(origin.offset(dx, 4, dz), Blocks.STONE.defaultBlockState());
    }

    /**
     * Waits for the real server world until the condition holds and then runs the assertions.
     *
     * <p>The GameTest clock and the world clock are not the same speed, so a scenario that
     * waits for a world behaviour polls the world instead of guessing a delay.</p>
     */
    static void poll(GameTestHelper test, int attempts, java.util.function.BooleanSupplier ready, Runnable assertions) {
        if (attempts <= 0) {
            test.fail("the real server world did not reach the expected state of the scenario in time");
            return;
        }
        test.runAfterDelay(3, () -> {
            if (!ready.getAsBoolean()) {
                poll(test, attempts - 1, ready, assertions);
                return;
            }
            assertions.run();
            test.succeed();
        });
    }

    /** Places a real block in the lane and remembers it for the clean up. */
    void put(BlockPos pos, BlockState state) {
        placed.add(pos.immutable());
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    /** Registers an entity with the lane so the clean up removes it again. */
    <T extends Entity> T add(T entity) {
        spawned.add(entity);
        return entity;
    }

    /** The area of the lane, used to collect the real drops and the real entities. */
    AABB area(double radius) { return new AABB(origin).inflate(radius); }

    /** The number of real dropped items of one item inside the lane. */
    int dropped(Item item) {
        int total = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area(16), found -> found.getItem().is(item)))
            total += entity.getItem().getCount();
        return total;
    }

    /** Removes everything the lane owns and leaves the rest of the world alone. */
    void clean() {
        spawned.forEach(Entity::discard);
        spawned.clear();
        for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        forced.clear();
        // The lane also owns whatever the content itself spawned inside it: the champion spiderlings,
        // the cultist minions of the portal and the real drops of the dead mobs.
        //
        // The sweep stays inside the lane (see CLEAN_RADIUS): a wider sweep reached into the
        // neighbouring scenario and discarded its mobs, and a golem of a neighbouring scenario is
        // a Mob - that stopped the golem of the neighbour for good.
        level.getEntities((Entity)null, area(CLEAN_RADIUS), entity -> entity instanceof Mob || entity instanceof ItemEntity
                        || entity instanceof Projectile).forEach(Entity::discard);
        for (BlockPos pos : placed) if (level.isLoaded(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        placed.clear();
    }
}
