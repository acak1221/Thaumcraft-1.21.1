package thaumcraft.common.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A modern orb may represent several original orbs of the same value. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbCountAccessor {
    @Accessor("count") int thaumcraft$getOrbCount();
}
