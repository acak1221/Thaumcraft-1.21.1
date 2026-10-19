package thaumcraft.common.mixin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import thaumcraft.client.ThaumcraftFocusClient;

/** F predates offhand swap. Only the conflicting held-native-wand case wins. */
@Mixin(Minecraft.class)
public abstract class WandFocusKeyMixin {
    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"),
            slice = @Slice(from = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/Options;keySwapOffhand:Lnet/minecraft/client/KeyMapping;"),
                    to = @At(value = "FIELD",
                            target = "Lnet/minecraft/client/Options;keyDrop:Lnet/minecraft/client/KeyMapping;")),
            require = 1, allow = 1)
    private boolean thaumcraft$focusBeforeOffhandSwap(KeyMapping mapping) {
        Minecraft client = (Minecraft)(Object)this;
        return ThaumcraftFocusClient.consumeOffhandSwapClick(client, mapping);
    }
}
