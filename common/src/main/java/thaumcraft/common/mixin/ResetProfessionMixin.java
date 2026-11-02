package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.lib.world.VillageProfessionBehaviors;

/** TC4's generated professions have no job site; keep native profession reset for every other villager. */
@Mixin(ResetProfession.class)
public abstract class ResetProfessionMixin {
    @WrapMethod(method = "create()Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;", require = 1, allow = 1)
    private static BehaviorControl<Villager> thaumcraft$originalProfessions(Operation<BehaviorControl<Villager>> original) {
        return VillageProfessionBehaviors.preserveOriginalProfessions(original.call());
    }
}
