package thaumcraft.common.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumcraft.common.lib.essentia.CrystalInventoryTarget;
import thaumcraft.common.lib.essentia.NativeCrystalInventoryAccess;

@Mixin(HopperBlockEntity.class)
public abstract class CrystalHopperInventoryMixin implements NativeCrystalInventoryAccess {
    @Shadow private NonNullList<ItemStack> items;
    @Shadow private int cooldownTime;
    @Override public CrystalInventoryTarget.Snapshot thaumcraft$crystalSnapshot() { return CrystalInventoryTarget.snapshot(items, cooldownTime, ((BlockEntity) (Object) this).getLevel().registryAccess()); }
    @Override public boolean thaumcraft$matchesCrystalSnapshot(CrystalInventoryTarget.Snapshot snapshot) { return snapshot.matches(items, cooldownTime); }
    @Override public void thaumcraft$applyCrystalInventory(CrystalInventoryTarget.Plan plan) { items = plan.replacementFor((BlockEntity) (Object) this); cooldownTime = 8; }
}
