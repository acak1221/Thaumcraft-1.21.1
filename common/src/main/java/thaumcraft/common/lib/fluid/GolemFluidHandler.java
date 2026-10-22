package thaumcraft.common.lib.fluid;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * A loader independent view of a block that holds a fluid.
 *
 * <p>TC4 4.2.3.5 compiled against the Forge {@code IFluidHandler} of 1.7.10
 * (oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:341-446}): the home of the golem
 * is <b>any</b> {@code IFluidHandler}, the source is <b>any</b> marked {@code IFluidHandler} of the
 * wanted fluid and the cargo is a {@code FluidStack}, that is a fluid <b>and</b> an amount.</p>
 *
 * <p>The 1.21.1 common module has no Forge and no Fabric transfer API, so this interface is the
 * port's own. It must not name a class of either loader: the NeoForge half answers with the
 * {@code IFluidHandler} capability and the Fabric half with {@code FluidStorage.SIDED}.</p>
 *
 * <p>The unit is the <b>millibucket</b> of the original: a {@code FluidStack} of 4.2.3.5 holds
 * thousandths of a bucket and the loader that speaks droplets converts at its own border.</p>
 */
public interface GolemFluidHandler {
    /** The fluid the container holds on the given face, or {@code null} when it holds none. */
    ResourceLocation fluid(Direction face);

    /** The amount in millibuckets the container holds on the given face. */
    int amount(Direction face);

    /**
     * Whether the container would take the given fluid on the given face.
     *
     * <p>Oracle {@code GolemHelper.getMissingLiquids}:341-369 asks the home's handler with
     * {@code canFill(facing, fluid)} for every registered fluid.</p>
     */
    boolean canFill(Direction face, ResourceLocation fluid);

    /**
     * Takes at most {@code amountMb} millibuckets of {@code fluid} from the given face.
     *
     * <p>A {@code commit} of {@code false} only asks what the container would give, exactly like the
     * original's probe {@code drain(side, new FluidStack(fluid, 1), false)}.</p>
     *
     * @return the amount in millibuckets that was taken (or would be taken)
     */
    int drain(Direction face, ResourceLocation fluid, int amountMb, boolean commit);

    /**
     * Puts at most {@code amountMb} millibuckets of {@code fluid} into the given face.
     *
     * <p>A {@code commit} of {@code false} only asks what the container would take.</p>
     *
     * @return the amount in millibuckets that was accepted (or would be accepted)
     */
    int fill(Direction face, ResourceLocation fluid, int amountMb, boolean commit);
}
