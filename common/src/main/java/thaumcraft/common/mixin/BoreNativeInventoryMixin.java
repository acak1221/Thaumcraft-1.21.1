package thaumcraft.common.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.tiles.TileArcaneBore;

/** Custom, NON-remapped field names bind the frozen top's actual two private
 * slot objects. No slot getter/setter or mutable alias is returned to callers. */
@Mixin(value = TileArcaneBore.class, remap = false)
public abstract class BoreNativeInventoryMixin implements BoreNativeContext.InventoryAccess {
    @Shadow(remap = false) private ItemStack focus;
    @Shadow(remap = false) private ItemStack pick;

    @Override public void thaumcraft$boreObserveInventory(BoreNativeContext.InventoryObservation observation) {
        observation.accept((TileArcaneBore) (Object) this, focus, pick);
    }
}
