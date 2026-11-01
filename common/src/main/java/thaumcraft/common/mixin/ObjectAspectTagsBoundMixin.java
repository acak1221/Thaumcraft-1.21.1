package thaumcraft.common.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tag membership changes on reload without replacing the item registry or the loaded level. */
@Mixin(MappedRegistry.class)
public abstract class ObjectAspectTagsBoundMixin {
    @Inject(method = "bindTags", at = @At("RETURN"))
    private void thaumcraft$refreshObjectAspectTags(CallbackInfo ci) {
        if ((Object) this == BuiltInRegistries.ITEM) thaumcraft.api.ThaumcraftApi.onItemTagsBound();
    }
}
