package thaumcraft.api.machines;

import thaumcraft.api.aspects.Aspect;

/**
 * The Deconstruction Table (block {@code thaumcraft:deconstruction_table}, the original {@code TileDeconstructionTable}):
 * its input and the primal it found.
 *
 * <p>The table breaks one item of its input every forty ticks; with a chance it yields one primal aspect, which waits on
 * the table until a player takes it as a research point. While a primal waits the table does not work. The original
 * peripheral {@code tt_deconstructor} read the public field {@code aspect} and the stack in slot 0.</p>
 */
public interface IDeconstructor {
    /**
     * The primal aspect waiting to be taken (original {@code aspect}), or {@code null} when there is none. Saved with
     * the block (key {@code Aspect}) and synced to clients. Either side.
     *
     * @return one of the six primal aspects, or {@code null}
     */
    Aspect getReward();

    /**
     * Whether an item lies in the input slot (original {@code getStackInSlot(0) != null}). A table whose saved data
     * could not be read keeps that data untouched and answers false. Either side.
     *
     * @return true when the input slot holds an item
     */
    boolean hasItem();
}
