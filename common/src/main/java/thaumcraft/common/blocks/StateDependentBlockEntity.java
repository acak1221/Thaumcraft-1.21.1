package thaumcraft.common.blocks;

/**
 * Marks an {@link net.minecraft.world.level.block.EntityBlock} whose {@code newBlockEntity} answers
 * {@code null} for some of its states.
 *
 * <p>The 1.7.10 original decided per metadata whether a block has a tile entity ({@code hasTileEntity(int)}).
 * 1.21.1 decides per block: {@code BlockState#hasBlockEntity} is {@code block instanceof EntityBlock}. A
 * block that carries the tile on one variant and none on the others therefore reports a block entity for
 * all of them. While a chunk is generated, {@code WorldGenRegion#setBlock} stores a {@code DUMMY} block
 * entity marker for every such state, and when the chunk is promoted {@code LevelChunk} asks the block for
 * the real entity, gets {@code null} for a state without one and logs "Tried to load a block entity for
 * block ... but failed" for each. For these blocks that answer is expected, so the log line is dropped
 * (see {@code LevelChunkPendingBlockEntityMixin}); the entity of a state that has one is still created by
 * the unchanged vanilla code.
 */
public interface StateDependentBlockEntity {
}
