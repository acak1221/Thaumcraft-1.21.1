package thaumcraft.common.mixin;

import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumcraft.common.lib.bore.BoreNativeContext;

/** Read-only identity link from the actual registered level ticker to its bound
 * ticker. Exposing this value cannot manufacture the private native context. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$RebindableTickingBlockEntityWrapper")
public abstract class BoreNativeTickerWrapperMixin implements BoreNativeContext.WrapperAccess {
    @Shadow private TickingBlockEntity ticker;
    @Override public TickingBlockEntity thaumcraft$boreWrappedTicker() { return ticker; }
}
