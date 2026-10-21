package thaumcraft.common.blocks;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A block whose block entity is decided per state, the way the 1.7.10 original decided it per
 * metadata.
 *
 * <p>The original answered {@code hasTileEntity(int metadata)} (OpenTC4 BlockCosmeticSolid:259-265,
 * BlockEldritch:120-122, BlockEldritchNothing:61-63) and created the tile only for the metadata
 * that had one. Minecraft 1.21.1 decides per block: {@code BlockState#hasBlockEntity} is
 * {@code block instanceof EntityBlock} (Minecraft 1.21.1
 * {@code net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase}:803-805), and
 * {@code BaseEntityBlock} is an {@code EntityBlock}, so a block that carries a tile for one
 * variant claims a tile for all of them.
 *
 * <p>Two things follow from that and both are wrong for the original:
 * <ul>
 *   <li>the block entity of a state that never had one is created (or a {@code DUMMY} marker is
 *       written for it while the chunk is generated), and</li>
 *   <li>the piston refuses the block: {@code PistonBaseBlock#isPushable} answers
 *       {@code !blockState.hasBlockEntity()} (Minecraft 1.21.1
 *       {@code net/minecraft/world/level/block/piston/PistonBaseBlock}:277), so a block that
 *       claims a tile for every state is never moved by a piston, while the original moved it
 *       together with the tile that it carried.</li>
 * </ul>
 *
 * <p>A block that implements this interface answers the question of the original per state. The
 * common mixin {@code thaumcraft.common.mixin.PerStateBlockEntityMixin} reads the answer, so
 * the block entity of a state is created only for the states that have one.
 *
 * <p>The contract answers the question of the block entity and nothing else. The question of the
 * piston is a question of its own, because the 1.7.10 original refused a block by its class (the
 * vanilla 1.7.10 {@code net.minecraft.block.BlockPistonBase#canPushBlock} ended with {@code
 * return !(block instanceof ITileEntityProvider);}) and never by its metadata; see {@link
 * OriginalPistonPushable} and {@link OriginalPistonRule}.
 */
public interface PerStateBlockEntity {
    /**
     * True when the given state carries a block entity, the answer of the original
     * {@code hasTileEntity(int metadata)} for the metadata of that state.
     *
     * @param state the state of the block
     * @return true when the state has a block entity
     */
    boolean hasBlockEntityForState(BlockState state);
}
