package thaumcraft.common.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.config.ThaumcraftWoodwork;

/** Native hanging editing/text/networking, replacing only its otherwise missing magical-wood background. */
@Mixin(HangingSignEditScreen.class)
public abstract class WoodworkHangingSignEditorMixin extends AbstractSignEditScreen {
    protected WoodworkHangingSignEditorMixin(SignBlockEntity sign, boolean front, boolean filtered) { super(sign, front, filtered); }

    @Inject(method = "renderSignBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumcraft$magicalWoodBoard(GuiGraphics graphics, BlockState state, CallbackInfo ci) {
        var family = ThaumcraftWoodwork.family(woodType);
        if (family == null) return;
        graphics.pose().translate(0, -13, 0);
        graphics.pose().scale(4.5F, 4.5F, 1);
        var planks = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/planks_" + family.name() + ".png");
        var frame = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/" + family.name() + "side.png");
        graphics.blit(planks, -8, -8, 0, 0, 16, 16, 16, 16);
        graphics.blit(frame, -8, -8, 0, 0, 16, 1, 16, 16);
        graphics.blit(frame, -8, 7, 0, 0, 16, 1, 16, 16);
        graphics.blit(frame, -8, -7, 0, 0, 1, 14, 16, 16);
        graphics.blit(frame, 7, -7, 0, 0, 1, 14, 16, 16);
        ci.cancel();
    }
}
