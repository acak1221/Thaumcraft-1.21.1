package thaumcraft.common.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumcraft.common.lib.essentia.NativeThaumatoriumInventory;
import thaumcraft.common.lib.essentia.ThaumatoriumOutput;

@Mixin(ChestBlockEntity.class)
public abstract class ThaumatoriumChestInventoryMixin implements NativeThaumatoriumInventory {
    @Shadow private NonNullList<ItemStack> items;
    @Override public ThaumatoriumOutput.Snapshot thaumcraft$thaumatoriumSnapshot() { return ThaumatoriumOutput.snapshot(items, 0, ((BlockEntity) (Object) this).getLevel().registryAccess()); }
    @Override public boolean thaumcraft$matchesThaumatoriumSnapshot(ThaumatoriumOutput.Snapshot snapshot) { return snapshot.matches(items, 0); }
    @Override public void thaumcraft$applyThaumatoriumOutput(ThaumatoriumOutput.Plan plan) { items = plan.replacementFor((BlockEntity) (Object) this); }
}
