package thaumcraft.common.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumcraft.common.lib.essentia.CrystalInventoryTarget;
import thaumcraft.common.lib.essentia.NativeCrystalInventoryAccess;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class CrystalFurnaceInventoryMixin implements NativeCrystalInventoryAccess {
    @Shadow protected NonNullList<ItemStack> items;
    @Override public CrystalInventoryTarget.Snapshot thaumcraft$crystalSnapshot() { return CrystalInventoryTarget.snapshot(items, 0, ((BlockEntity) (Object) this).getLevel().registryAccess()); }
    @Override public boolean thaumcraft$matchesCrystalSnapshot(CrystalInventoryTarget.Snapshot snapshot) { return snapshot.matches(items, 0); }
    @Override public void thaumcraft$applyCrystalInventory(CrystalInventoryTarget.Plan plan) { items = plan.replacementFor((BlockEntity) (Object) this); }
}
