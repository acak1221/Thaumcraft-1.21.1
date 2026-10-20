package thaumcraft.common.lib.essentia;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Closed native output adapter; implementations are only the three vanilla inventory mixins. */
public interface NativeThaumatoriumInventory {
    ThaumatoriumOutput.Snapshot thaumcraft$thaumatoriumSnapshot();
    boolean thaumcraft$matchesThaumatoriumSnapshot(ThaumatoriumOutput.Snapshot snapshot);
    void thaumcraft$applyThaumatoriumOutput(ThaumatoriumOutput.Plan plan);
}
