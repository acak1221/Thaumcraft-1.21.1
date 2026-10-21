package thaumcraft.common.blocks;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A block whose pushability by a piston the 1.7.10 original decided by the class of the block and
 * not by its metadata.
 *
 * <p>The original asked one question about a block, and the metadata was never part of it: the
 * vanilla 1.7.10 {@code net.minecraft.block.BlockPistonBase#canPushBlock} ended with
 * {@code return !(block instanceof ITileEntityProvider);}. Every {@code BlockContainer} of the
 * original implements that interface, so a block of such a family was never moved by the piston,
 * for any of its metadata, and a block of a family whose class was a plain {@code Block} was moved
 * for every metadata. The move destroyed the tile of the metadata that carried one: the 1.7.10
 * {@code net.minecraft.tileentity.TileEntityPiston} carried the block and the metadata only, so
 * the block that arrived had no tile at all.
 *
 * <p>Minecraft 1.21.1 asks the question per state: {@code
 * net.minecraft.world.level.block.piston.PistonBaseBlock#isPushable} ends with
 * {@code return !blockState.hasBlockEntity();}, and {@code BlockState#hasBlockEntity} is
 * {@code block instanceof EntityBlock} (Minecraft 1.21.1
 * {@code net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase}:552-554, the decompilation of CFR 0.152). The two
 * rules agree for the vanilla blocks, and they do not agree for the blocks of Thaumcraft, because
 * the port carries the tile of a family in a block entity class where the original carried it in
 * a plain block.
 *
 * <p>A block that implements this interface answers the question of the original for every state
 * of itself; the answer of the original for a family of Thaumcraft is in {@link
 * OriginalPistonRule}. The common mixin {@code thaumcraft.common.mixin.PistonPushabilityMixin}
 * reads the answer, and every block that does not implement this interface keeps the answer of
 * the vanilla code.
 */
public interface OriginalPistonPushable {
    /**
     * True when the piston of the 1.7.10 original moved the given state.
     *
     * @param state the state of the block
     * @return true when the piston of the original moved the state
     */
    boolean isPushableInOriginal(BlockState state);
}
