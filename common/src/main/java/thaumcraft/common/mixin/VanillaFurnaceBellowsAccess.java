package thaumcraft.common.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface VanillaFurnaceBellowsAccess {
    @Accessor("cookingProgress") int thaumcraft$getCookingProgress();
    @Accessor("cookingProgress") void thaumcraft$setCookingProgress(int value);
    @Accessor("cookingTotalTime") int thaumcraft$getCookingTotalTime();
}
