package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/** Original filter:-1 marker only. Actual one-unit storage remains in TileTube. */
public final class TileTubeFilter extends TileTube implements IAspectContainer {
    public TileTubeFilter(BlockPos pos, BlockState state) {
        super(pos, state);
        if (!isFilter()) throw new IllegalArgumentException("A filter marker requires a filter tube block");
    }

    @Override public AspectList getAspects() { return getFilter() == null ? null : new AspectList().add(getFilter(), -1); }
    @Override public void setAspects(AspectList aspects) {}
    @Override public boolean doesContainerAccept(Aspect aspect) { return false; }
    @Override public int addToContainer(Aspect aspect, int amount) { return 0; }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) { return false; }
    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return false; }
    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) { return false; }
    @Override public int containerContains(Aspect aspect) { return 0; }
}
