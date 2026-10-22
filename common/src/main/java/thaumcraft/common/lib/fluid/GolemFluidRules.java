package thaumcraft.common.lib.fluid;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * The pure rules of the fluid half of the original golem helper.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:331-369}: the original asks
 * the fluid registry for every registered liquid, narrows the list to the fluid of the cargo and,
 * when the golem holds any filter item, to the fluid of a filled container of its inventory.</p>
 *
 * <p>No level, no tile and no registry is touched here, so the rules can be pinned by a test.</p>
 */
public final class GolemFluidRules {
    private GolemFluidRules() {}

    /**
     * Whether a registry entry is a liquid of the original's registry.
     *
     * <p>Oracle {@code GolemHelper.getReggedLiquids}:331-339 holds the <b>still</b> fluids of the
     * game. The 1.21.1 registry also holds the empty fluid and the flowing variants of every
     * liquid; a flowing fluid answers {@code isSource()} with false and the empty fluid answers
     * {@code isEmpty()} with true, so both are rejected here.</p>
     */
    public static boolean registrable(Fluid fluid) {
        if (fluid == null) return false;
        FluidState state = fluid.defaultFluidState();
        return !state.isEmpty() && state.isSource();
    }

    /**
     * Original {@code GolemHelper.getMissingLiquids}:352 — a loaded cargo accepts only its own fluid.
     *
     * <p>{@code golem.fluidCarried != null && golem.fluidCarried.amount > 0 && fluidID != id} skips
     * every other fluid. An empty cargo accepts everything.</p>
     */
    public static boolean carriedAccepts(ResourceLocation fluid, ResourceLocation carried, int carriedAmountMb) {
        return carriedAmountMb <= 0 || Objects.equals(fluid, carried);
    }

    /**
     * Original {@code getMissingLiquids}:354-363 — a filter narrows the set to its own fluid.
     *
     * <p>When the inventory of the golem holds any item, the original scans every slot for a
     * filled container and requires {@code fis.isFluidEqual(fs)}, so the fluid of the filter
     * container is the only fluid left. A non-empty filter that is no filled container therefore
     * leaves no missing liquid at all ({@code filterFluid == null}).</p>
     */
    public static boolean filterAccepts(ResourceLocation fluid, boolean filtered, ResourceLocation filterFluid) {
        return !filtered || Objects.equals(fluid, filterFluid);
    }
}
