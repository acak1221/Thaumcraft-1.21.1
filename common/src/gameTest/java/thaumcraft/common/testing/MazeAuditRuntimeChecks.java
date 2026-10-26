package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsDimension;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.tiles.TileEldritchPortal;

/** Supplied seed/map inputs; actual dimension chunk generation and portal transfer, not templates. */
public final class MazeAuditRuntimeChecks {
    private MazeAuditRuntimeChecks() {}

    public static void generation(GameTestHelper test) {
        ServerLevel outer = OuterLandsDimension.level(test.getLevel().getServer());
        var generator = new MazeGenerator(15, 15, 20261021L);
        test.assertTrue(generator.generate(), "fixed audit seed completes");
        int ox = StructurePlacementRules.mazeOriginChunk(2048, 15);
        int oz = StructurePlacementRules.mazeOriginChunk(2048, 15);
        var maze = OuterLandsProvider.maze(outer);
        maze.storeLabyrinth(generator, 2048, 2048);
        test.assertTrue(!outer.hasChunk(2048, 2048), "storing map does not eagerly generate destination blocks");
        int cells = 0, boss = 0, portals = 0, minX = 99, maxX = -1, minZ = 99, maxZ = -1;
        int bossX = 0, bossZ = 0;
        for (int z = 0; z < 15; z++) for (int x = 0; x < 15; x++) {
            if (generator.grid[z][x] == 0) continue;
            cells++;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
            outer.getChunk(ox + x, oz + z);
            var cell = new MazeGenerator.Cell((short) generator.grid[z][x]);
            int bx = (ox + x) * 16, bz = (oz + z) * 16;
            int floorY = cell.feature >= 1 && cell.feature <= 8 ? 49 : 50;
            test.assertTrue(outer.getBlockState(new BlockPos(bx + 8, floorY, bz + 8)).is(Blocks.BEDROCK),
                    "real cell has oracle shell floor at Y" + floorY + ": " + x + "," + z);
            if (cell.feature >= 2 && cell.feature <= 5) {
                boss++;
                test.assertTrue(outer.getBlockState(new BlockPos(bx + 8, 63, bz + 8)).is(Blocks.BEDROCK),
                        "boss shell ceiling at Y63");
                if (cell.feature == 2) { bossX = x; bossZ = z; }
            }
            if (cell.feature == 1) {
                portals++;
                test.assertTrue(x == 8 && z == 8, "oracle portal offset is cell (8,8)");
                test.assertTrue(outer.getBlockState(new BlockPos(bx + 8, 53, bz + 8)).is(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()),
                        "generated gate at chunk centre Y53");
                test.assertTrue(outer.getBlockEntity(new BlockPos(bx + 8, 53, bz + 8)) instanceof TileEldritchPortal,
                        "generated gate has live block entity");
                int floor = 0;
                for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                    boolean bedrock = outer.getBlockState(new BlockPos(bx + dx, 49, bz + dz)).is(Blocks.BEDROCK);
                    test.assertTrue(bedrock == (dx >= 2 && dx <= 14 && dz >= 2 && dz <= 14), "portal floor oracle bounds");
                    if (bedrock) floor++;
                }
                test.assertTrue(floor == 169, "portal shell floor measures 13x13=169 bedrock blocks");
                for (int y = 54; y <= 58; y++)
                    test.assertTrue(outer.getBlockState(new BlockPos(bx + 8, y, bz + 8)).equals(
                                    thaumcraft.common.config.ThaumcraftOuterLands.state(
                                            thaumcraft.common.lib.world.DungeonRooms.Kind.ELDRITCH, y == 54 ? 1 : 2, -1)),
                            "portal obelisk has oracle five-block column at Y" + y);
            }
            if (cell.feature == 1 || cell.feature == 6 || cell.feature == 7 || cell.feature == 8)
                test.assertTrue(!outer.getBlockState(new BlockPos(bx + 7, 55, bz + 7))
                                .is(thaumcraft.common.config.ThaumcraftOuterLands.eldritchNothing()),
                        "real special room interior is not filled with void lining, feature " + cell.feature);
        }
        test.assertTrue(boss == 4 && portals == 1, "four boss quadrants and one portal generated");
        test.assertTrue(maxX - minX + 1 == 14 && maxZ - minZ + 1 == 14,
                "15x15 allocated grid has oracle 14x14 occupied chunk extent (224x224 blocks); actual="
                        + minX + ".." + maxX + "," + minZ + ".." + maxZ);
        test.assertTrue((generator.grid[bossZ][bossX + 1] >> 8) == 3
                        && (generator.grid[bossZ + 1][bossX] >> 8) == 4
                        && (generator.grid[bossZ + 1][bossX + 1] >> 8) == 5,
                "boss quadrants form oracle 2x2 chunks / 32x32 block cell extent");
        int bossFloor = 0;
        int bossBaseX = (ox + bossX) * 16, bossBaseZ = (oz + bossZ) * 16;
        for (int dz = 0; dz < 32; dz++) for (int dx = 0; dx < 32; dx++) {
            boolean bedrock = outer.getBlockState(new BlockPos(bossBaseX + dx, 49, bossBaseZ + dz)).is(Blocks.BEDROCK);
            test.assertTrue(bedrock == (dx >= 2 && dx <= 30 && dz >= 2 && dz <= 30), "boss floor oracle bounds");
            if (bedrock) bossFloor++;
        }
        test.assertTrue(bossFloor == 841, "boss shell floor measures 29x29=841 bedrock blocks");
        int seams = 0;
        for (int z = 0; z < 15; z++) for (int x = 0; x < 15; x++) {
            var cell = new MazeGenerator.Cell((short) generator.grid[z][x]);
            int bx = (ox + x) * 16, bz = (oz + z) * 16;
            if (cell.east) {
                for (int w = 7; w <= 9; w++) for (int d = 15; d <= 16; d++)
                    test.assertTrue(outer.getBlockState(new BlockPos(bx + d, 55, bz + w)).isAir(),
                            "east connection retains three-block clear core at " + x + "," + z + " w=" + w + " d=" + d);
                seams++;
            }
            if (cell.south) {
                for (int w = 7; w <= 9; w++) for (int d = 15; d <= 16; d++)
                    test.assertTrue(outer.getBlockState(new BlockPos(bx + w, 55, bz + d)).isAir(),
                            "south connection retains three-block clear core at " + x + "," + z + " w=" + w + " d=" + d);
                seams++;
            }
        }
        // Flood the real walkable blocks from a possible gate landing to the outside of the boss lock.
        BlockPos start = new BlockPos((ox + 8) * 16 + 7, 52, (oz + 8) * 16 + 7);
        var seen = new java.util.HashSet<BlockPos>();
        var queue = new java.util.ArrayDeque<BlockPos>();
        seen.add(start); queue.add(start);
        boolean reachedLock = false;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.remove();
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(direction);
                for (int dy = -1; dy <= 2; dy++) {
                    if (outer.getBlockEntity(side.offset(0, dy, 0)) instanceof thaumcraft.common.tiles.TileEldritchLock)
                        reachedLock = true;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = side.offset(0, dy, 0);
                    if (next.getX() < ox * 16 || next.getX() >= (ox + 15) * 16
                            || next.getZ() < oz * 16 || next.getZ() >= (oz + 15) * 16
                            || next.getY() < 51 || next.getY() > 59 || seen.contains(next)) continue;
                    if (!outer.getBlockState(next).getCollisionShape(outer, next).isEmpty()
                            || !outer.getBlockState(next.above()).getCollisionShape(outer, next.above()).isEmpty()
                            || outer.getBlockState(next.below()).getCollisionShape(outer, next.below()).isEmpty()) continue;
                    seen.add(next); queue.add(next);
                }
            }
        }
        test.assertTrue(reachedLock, "physical walk from gate landing reaches boss lock; explored=" + seen.size()
                + " start=" + outer.getBlockState(start) + " floor=" + outer.getBlockState(start.below())
                + " north=" + outer.getBlockState(start.north()) + " northFloor=" + outer.getBlockState(start.north().below())
                + " head=" + outer.getBlockState(start.above()));
        openedWardenArena(test, outer, ox + bossX, oz + bossZ);
        // A fresh disk cache, not an NBT-only round trip, must retain every real generated cell.
        SavedDataFlush.save(test.getLevel().getServer().overworld().getDataStorage());
        var directory = outer.getServer().getWorldPath(LevelResource.ROOT).resolve("data").toFile();
        var fresh = new DimensionDataStorage(directory, outer.getServer().getFixerUpper(), outer.registryAccess());
        var factory = new SavedData.Factory<>(OuterLandsProvider.MazeSavedData::new, OuterLandsProvider.MazeSavedData::load,
                net.minecraft.util.datafix.DataFixTypes.LEVEL);
        var loaded = fresh.get(factory, OuterLandsProvider.MazeSavedData.ID);
        test.assertTrue(loaded != null && loaded != maze, "maze reload is a distinct disk-backed instance");
        for (int z = 0; z < 15; z++) for (int x = 0; x < 15; x++)
            test.assertTrue(loaded.cell(ox + x, oz + z) == maze.cell(ox + x, oz + z), "disk retains cell " + x + "," + z);
        thaumcraft.common.Thaumcraft.LOG.info("MAZE_AUDIT cells={} extent={}..{},{}..{} bossUL={},{} portal=8,8 seams={} walkable={} roomFloor=49 bossCeiling=63",
                cells, minX, maxX, minZ, maxZ, bossX, bossZ, seams, seen.size());
        test.succeed();
    }

    private static void openedWardenArena(GameTestHelper test, ServerLevel outer, int bossChunkX, int bossChunkZ) {
        thaumcraft.common.tiles.TileEldritchLock entrance = null;
        for (int dz = 0; dz < 2; dz++) for (int dx = 0; dx < 2; dx++) {
            var chunk = outer.getChunk(bossChunkX + dx, bossChunkZ + dz);
            // The fixture opens immediately after synchronous generation, before normal chunk
            // promotion. Resolve packed tiles first, as a player reaching this room would do.
            for (BlockPos pos : chunk.getBlockEntitiesPos()) chunk.getBlockEntity(pos);
            for (var entity : chunk.getBlockEntities().values())
                if (entity instanceof thaumcraft.common.tiles.TileEldritchLock lock && lock.getFacing() >= 2 && lock.getFacing() <= 5)
                    entrance = lock;
        }
        test.assertTrue(entrance != null, "generated boss room has a directional entrance lock");
        var factory = new SavedData.Factory<>(thaumcraft.common.lib.world.OuterLandsBossRooms.BossSavedData::new,
                thaumcraft.common.lib.world.OuterLandsBossRooms.BossSavedData::load, net.minecraft.util.datafix.DataFixTypes.LEVEL);
        var bosses = outer.getDataStorage().computeIfAbsent(factory, thaumcraft.common.lib.world.OuterLandsBossRooms.BossSavedData.ID);
        bosses.bossCount = 0; // Supplied arena-selection input; first roll of Java seed zero exceeds .25.
        outer.getRandom().setSeed(0L);
        BlockPos lockPos = entrance.getBlockPos();
        int exit = OuterLandsProvider.maze(outer).cell(lockPos.getX() >> 4, lockPos.getZ() >> 4) >> 8;
        entrance.count = 100;
        thaumcraft.common.tiles.TileEldritchLock.tick(outer, lockPos, outer.getBlockState(lockPos), entrance);
        test.assertTrue(outer.getBlockState(lockPos).isAir(), "opening generated lock removes it");
        test.assertTrue(bosses.bossCount == 1, "supplied selection opens warden arena");
        int cx = bossChunkX * 16 + 16, cz = bossChunkZ * 16 + 16;
        int ax = cx + thaumcraft.common.lib.world.OuterLandsBossRooms.exitOffsetX(exit);
        int az = cz + thaumcraft.common.lib.world.OuterLandsBossRooms.exitOffsetZ(exit);
        for (BlockPos base : new BlockPos[]{new BlockPos(ax, 54, cz), new BlockPos(cx, 54, az)})
            for (int dy = 0; dy <= 4; dy++)
                test.assertTrue(outer.getBlockState(base.above(dy)).equals(thaumcraft.common.config.ThaumcraftOuterLands.state(
                                thaumcraft.common.lib.world.DungeonRooms.Kind.ELDRITCH, dy == 0 ? 1 : 2, -1)),
                        "warden obelisk at " + base.above(dy));
        test.assertTrue(thaumcraft.common.blocks.BlockEldritch.variant(outer.getBlockState(new BlockPos(ax, 52, az))) == 4,
                "warden pedestal at oracle exit offset; actual=" + outer.getBlockState(new BlockPos(ax, 52, az)));
        int wardens = 0;
        for (var entity : outer.getAllEntities())
            if (entity instanceof thaumcraft.common.entities.monster.EntityEldritchWarden
                    && entity.distanceToSqr(ax + .5, 53, az + .5) < .01) wardens++;
        test.assertTrue(wardens == 1, "one real warden spawns at oracle pedestal offset");
        thaumcraft.common.Thaumcraft.LOG.info("MAZE_AUDIT wardenCentre={},{} exitFeature={} pedestal={},52,{} obelisks={},54,{};{},54,{}",
                cx, cz, exit, ax, az, ax, cz, cx, az);
    }

    public static void nearestReturnGate(GameTestHelper test) {
        ServerLevel home = test.getLevel();
        ServerLevel outer = OuterLandsDimension.level(home.getServer());
        // The far gate shares the source chunk; the nearer gate is across its east boundary.
        BlockPos source = new BlockPos(48015, 80, 48008);
        BlockPos far = new BlockPos(48001, 80, 48008);
        BlockPos near = new BlockPos(48017, 80, 48008);
        gate(home, far); gate(home, near); gate(outer, source);
        outer.setChunkForced(source.getX() >> 4, source.getZ() >> 4, true);
        var player = test.makeMockServerPlayerInLevel();
        player.teleportTo(outer, source.getX() + .5, source.getY(), source.getZ() + .5, 0, 0);
        // A mock connection has no client respawn acknowledgement.
        player.hasChangedDimension();
        try {
            transfer(player);
            test.assertTrue(player.level() == home, "outer gate returns player to overworld");
            test.assertTrue(Math.abs(player.getX() - (near.getX() + .5)) <= 1.01
                            && Math.abs(player.getZ() - (near.getZ() + .5)) <= 1.01 && player.getY() == 80,
                    "nearest gate across chunk boundary wins; actual=" + player.position());
        } finally {
            player.discard();
            outer.setChunkForced(source.getX() >> 4, source.getZ() >> 4, false);
            for (BlockPos pos : new BlockPos[]{far, near}) for (int y = -1; y <= 1; y++) home.removeBlock(pos.offset(0, y, 0), false);
            for (int y = -1; y <= 1; y++) outer.removeBlock(source.offset(0, y, 0), false);
        }
        test.succeed();
    }

    public static void unexploredGate(GameTestHelper test) {
        ServerLevel home = test.getLevel();
        ServerLevel outer = OuterLandsDimension.level(home.getServer());
        var generator = new MazeGenerator(15, 15, 20261021L);
        test.assertTrue(generator.generate(), "fixed unexplored maze seed completes");
        OuterLandsProvider.maze(outer).storeLabyrinth(generator, 4096, 4096);
        BlockPos source = new BlockPos(4092 * 16 + 8, 53, 4096 * 16 + 8);
        BlockPos destination = new BlockPos(4096 * 16 + 8, 53, 4096 * 16 + 8);
        test.assertTrue(!outer.hasChunk(4096, 4096), "destination portal chunk starts unexplored");
        gate(home, source);
        home.setChunkForced(source.getX() >> 4, source.getZ() >> 4, true);
        var player = test.makeMockServerPlayerInLevel();
        player.teleportTo(home, source.getX() + .5, 53, source.getZ() + .5, 0, 0);
        try {
            transfer(player);
            test.assertTrue(player.level() == outer, "entry goes to outer dimension; actual=" + player.level().dimension().location() + " " + player.position());
            test.assertTrue(Math.abs(player.getX() - (destination.getX() + .5)) <= 1.01
                            && Math.abs(player.getZ() - (destination.getZ() + .5)) <= 1.01 && player.getY() == 53,
                    "oracle search finds unexplored gate 64 blocks away, not void fallback; actual=" + player.position());
        } finally {
            player.discard();
            home.setChunkForced(source.getX() >> 4, source.getZ() >> 4, false);
            for (int y = -1; y <= 1; y++) home.removeBlock(source.offset(0, y, 0), false);
        }
        test.succeed();
    }

    public static void returnGateBounds(GameTestHelper test) {
        ServerLevel home = test.getLevel();
        ServerLevel outer = OuterLandsDimension.level(home.getServer());
        BlockPos source = new BlockPos(60000, 80, 60008);
        BlockPos outside = source.offset(143, 0, 0);
        BlockPos inside = source.offset(128, 120, 0);
        gate(home, outside); gate(home, inside); gate(outer, source);
        home.setChunkForced(outside.getX() >> 4, outside.getZ() >> 4, true);
        outer.setChunkForced(source.getX() >> 4, source.getZ() >> 4, true);
        var player = test.makeMockServerPlayerInLevel();
        player.teleportTo(outer, source.getX() + .5, 80, source.getZ() + .5, 0, 0);
        player.hasChangedDimension();
        try {
            transfer(player);
            test.assertTrue(player.level() == home && Math.abs(player.getX() - (inside.getX() + .5)) <= 1.01
                            && player.getY() == 200,
                    "inclusive 128-block square rejects gate 143 blocks away; actual=" + player.position());
        } finally {
            player.discard();
            outer.setChunkForced(source.getX() >> 4, source.getZ() >> 4, false);
            home.setChunkForced(outside.getX() >> 4, outside.getZ() >> 4, false);
            for (BlockPos pos : new BlockPos[]{outside, inside}) for (int y = -1; y <= 1; y++) home.removeBlock(pos.offset(0, y, 0), false);
            for (int y = -1; y <= 1; y++) outer.removeBlock(source.offset(0, y, 0), false);
        }
        test.succeed();
    }

    /** Isolate the production transfer from mock-player entity-section promotion timing. */
    private static void transfer(net.minecraft.server.level.ServerPlayer player) {
        try {
            var method = TileEldritchPortal.class.getDeclaredMethod("transfer", net.minecraft.server.level.ServerPlayer.class);
            method.setAccessible(true);
            method.invoke(null, player);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("production portal transfer failed", failure);
        }
    }

    private static void gate(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.above(), ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get().defaultBlockState(), Block.UPDATE_ALL);
    }
}
