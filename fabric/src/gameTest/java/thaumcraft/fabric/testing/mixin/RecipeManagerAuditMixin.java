package thaumcraft.fabric.testing.mixin;

import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only audit of all global recipe replacements, including exceptional restores. */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerAuditMixin {
    private int thaumcraft$before;

    @Inject(method = "replaceRecipes", at = @At("HEAD"))
    private void thaumcraft$beforeReplace(CallbackInfo ci) {
        thaumcraft$before = ((RecipeManager) (Object) this).getRecipes().size();
    }

    @Inject(method = "replaceRecipes", at = @At("RETURN"))
    private void thaumcraft$afterReplace(CallbackInfo ci) {
        String caller = StackWalker.getInstance().walk(frames -> frames
                .filter(frame -> frame.getClassName().startsWith("thaumcraft.common.testing."))
                .findFirst().map(Object::toString).orElse("outside scenario"));
        LoggerFactory.getLogger("RecipePollutionAudit").info("RECIPE_REPLACE caller={} before={} after={}",
                caller, thaumcraft$before, ((RecipeManager) (Object) this).getRecipes().size());
    }
}
