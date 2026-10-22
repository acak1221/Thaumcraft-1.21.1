package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.nodes.NodeModifier;

/** Real biome storage/vanilla packet and actual disk unload/reload of a real embedded node; no inspected-client claim. */
public final class NodeBiomeStorageRuntimeChecks {
    private static final TicketType<BlockPos> TICKET = TicketType.create("thaumcraft-node-biome-lifecycle", Comparator.comparingLong(BlockPos::asLong));
    private record Quart(int x, int y, int z) {}
    private NodeBiomeStorageRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel(); var source = level.getChunkSource();
        BlockPos origin = test.absolutePos(new BlockPos(4, 10, 4));
        BlockPos position = new BlockPos((origin.getX() & ~15) + 1_048_596, origin.getY() + 100, (origin.getZ() & ~15) + 1_048_596);
        var chunkPos = new ChunkPos(position); source.addRegionTicket(TICKET, chunkPos, 0, position);
        LevelChunk oldChunk = level.getChunkAt(position); var oldState = level.getBlockState(position);
        var oldTile = level.getBlockEntity(position); var oldNbt = oldTile == null ? null : oldTile.saveWithFullMetadata(level.registryAccess());
        Map<Quart, Holder<Biome>> oldBiomes = snapshot(oldChunk); boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return; closed[0] = true;
            try {
                source.addRegionTicket(TICKET, chunkPos, 2, position);
                LevelChunk live = level.getChunkAt(position);
                level.removeBlockEntity(position); level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(position, oldState, Block.UPDATE_CLIENTS);
                if (oldNbt != null && level.getBlockEntity(position) != null) level.getBlockEntity(position).loadWithComponents(oldNbt, level.registryAccess());
                live.fillBiomesFromNoise((x, y, z, sampler) -> oldBiomes.get(new Quart(x, y, z)), source.randomState().sampler());
                live.setUnsaved(true); source.chunkMap.resendBiomesForChunks(java.util.List.of(live));
            } finally {
                source.removeRegionTicket(TICKET, chunkPos, 0, position); source.removeRegionTicket(TICKET, chunkPos, 2, position);
            }
        };
        try {
            test.assertTrue(oldTile == null, "private far-chunk fixture does not overwrite an existing block entity");
            // Explicit storage fixture, using the actual adapter both times; not a simulated biome predicate or rewritten registry tag.
            BiomeColumns.set(level, position.getX(), position.getZ(), ThaumcraftBiomes.EERIE);
            oldChunk.setUnsaved(false);
            test.assertTrue(BiomeColumns.set(level, position.getX(), position.getZ(), ThaumcraftBiomes.MAGICAL_FOREST) && oldChunk.isUnsaved(),
                    "actual biome-column update dirties native chunk save state");
            assertColumn(test, oldChunk, oldBiomes, position);
            test.assertTrue(!BiomeColumns.set(level, position.getX(), position.getZ(), ThaumcraftBiomes.MAGICAL_FOREST),
                    "an already-equal complete native column does not generate a second update");

            var wire = new FriendlyByteBuf(Unpooled.buffer());
            try {
                ClientboundChunksBiomesPacket.STREAM_CODEC.encode(wire, ClientboundChunksBiomesPacket.forChunks(java.util.List.of(oldChunk)));
                var decoded = ClientboundChunksBiomesPacket.STREAM_CODEC.decode(wire);
                test.assertTrue(decoded.chunkBiomeData().size() == 1 && decoded.chunkBiomeData().getFirst().pos().equals(chunkPos),
                        "vanilla biome packet codec retains the actual native chunk position");
                var payload = decoded.chunkBiomeData().getFirst().getReadBuffer();
                try {
                    // This uninstalled native container uses the same replaceBiomes decoder as ClientChunkCache; it is never inserted into the world.
                    LevelChunk decodedContainer = new LevelChunk(level, chunkPos); decodedContainer.replaceBiomes(payload);
                    assertColumn(test, decodedContainer, oldBiomes, position);
                } finally { payload.release(); }
            } finally { wire.release(); }

            var knotState = ThaumcraftContent.SILVERWOOD_KNOT.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            level.setBlock(position, knotState, Block.UPDATE_CLIENTS);
            var placed = level.getBlockEntity(position);
            test.assertTrue(placed instanceof TileNode && placed.getType() == ThaumcraftContent.NODE_BLOCK_ENTITY.get(), "actual embedded log owns the shared registered native TileNode");
            var node = (TileNode) placed;
            node.initializeGenerated(new AspectList().add(Aspect.AIR, 15).add(Aspect.ORDER, 6), NodeType.PURE, null);
            String scanId = node.getScanId();
            source.removeRegionTicket(TICKET, chunkPos, 0, position);
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    if (source.getChunkNow(chunkPos.x, chunkPos.z) != null || !node.isRemoved()) return;
                    source.addRegionTicket(TICKET, chunkPos, 2, position);
                    LevelChunk loadedChunk = level.getChunkAt(position); var restored = level.getBlockEntity(position);
                    test.assertTrue(loadedChunk != oldChunk && restored != node && restored instanceof TileNode,
                            "actual ServerLevel unload is followed by a fresh disk-loaded chunk and TileNode");
                    TileNode loaded = (TileNode) restored; var state = level.getBlockState(position);
                    test.assertTrue(state.is(ThaumcraftContent.SILVERWOOD_KNOT.get()) && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z
                                    && loaded.getNodeType() == NodeType.PURE && loaded.getNodeModifier() == null && loaded.getScanId().equals(scanId)
                                    && loaded.getAspects().getAmount(Aspect.AIR) == 15 && loaded.getAspects().getAmount(Aspect.ORDER) == 6
                                    && loaded.getAspectsBase().aspects.equals(loaded.getAspects().aspects),
                            "real native disk reload preserves knot axis, PURE type, UUID, current vis and base capacity");
                    assertColumn(test, loadedChunk, oldBiomes, position);
                    test.assertTrue(state.getLightEmission() == 7 && state.getCollisionShape(level, position).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                                    && state.getBlock().getCloneItemStack(level, position, state).is(ThaumcraftNaturals.SILVERWOOD_LOG.get().asItem()),
                            "embedded node remains a luminous full log and original pick-block identity is plain Silverwood");

                    // Declared zero-vis/FADING fixture isolates the original count1200 pruning boundary through the real ticker.
                    loaded.initializeGenerated(new AspectList().add(Aspect.AIR, 0), NodeType.NORMAL, NodeModifier.FADING);
                    var ticker = ThaumcraftContent.SILVERWOOD_KNOT.get().getTicker(level, state, ThaumcraftContent.NODE_BLOCK_ENTITY.get());
                    test.assertTrue(ticker != null, "real knot block provides the actual native node ticker");
                    for (int tick = 0; tick < 1199; tick++) ticker.tick(level, position, state, loaded);
                    test.assertTrue(level.getBlockState(position) == state && level.getBlockEntity(position) == loaded && loaded.getTickCount() == 1199,
                            "zero-vis embedded node survives until its original count1200 pruning boundary");
                    ticker.tick(level, position, state, loaded);
                    test.assertTrue(level.getBlockState(position).is(ThaumcraftNaturals.SILVERWOOD_LOG.get())
                                    && level.getBlockState(position).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z
                                    && level.getBlockEntity(position) == null && loaded.isRemoved(),
                            "actual original prune transition replaces exhausted knot with ordinary Silverwood on the same axis, without leaving a node tile");
                    cleanup.run(); test.succeed();
                } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
            });
            test.runAfterDelay(540, () -> { if (!closed[0]) { cleanup.run(); test.fail("isolated embedded node chunk did not actually unload before540 world ticks"); } });
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }

    private static Map<Quart, Holder<Biome>> snapshot(LevelChunk chunk) {
        var result = new HashMap<Quart, Holder<Biome>>();
        int baseX = chunk.getPos().x << 2, baseZ = chunk.getPos().z << 2;
        for (int x = baseX; x < baseX + 4; x++) for (int z = baseZ; z < baseZ + 4; z++)
            for (int y = QuartPos.fromBlock(chunk.getMinBuildHeight()); y <= QuartPos.fromBlock(chunk.getMaxBuildHeight() - 1); y++)
                result.put(new Quart(x, y, z), chunk.getNoiseBiome(x, y, z));
        return result;
    }

    private static void assertColumn(GameTestHelper test, LevelChunk chunk, Map<Quart, Holder<Biome>> before, BlockPos target) {
        int selectedX = QuartPos.fromBlock(target.getX()), selectedZ = QuartPos.fromBlock(target.getZ());
        for (var entry : before.entrySet()) {
            Quart key = entry.getKey(); var actual = chunk.getNoiseBiome(key.x, key.y, key.z);
            boolean selected = key.x == selectedX && key.z == selectedZ;
            test.assertTrue(selected ? actual.is(ThaumcraftBiomes.MAGICAL_FOREST) : actual.equals(entry.getValue()),
                    "one native4x4 column changes across all Y while every other stored quart holder is preserved: " + key);
        }
    }
}
