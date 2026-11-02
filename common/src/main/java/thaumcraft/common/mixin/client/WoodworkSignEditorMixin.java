package thaumcraft.common.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.ThaumcraftWoodworkClient;
import thaumcraft.client.renderers.block.WoodworkSignModels;
import thaumcraft.common.config.ThaumcraftWoodwork;

/** Keeps the native editor/text protocol and transforms, drawing the same UV-correct board as the world. */
@Mixin(SignEditScreen.class)
public abstract class WoodworkSignEditorMixin extends AbstractSignEditScreen {
    protected WoodworkSignEditorMixin(SignBlockEntity sign, boolean front, boolean filtered) { super(sign, front, filtered); }

    @Inject(method = "renderSignBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void thaumcraft$magicalWoodBoard(GuiGraphics graphics, BlockState state, CallbackInfo ci) {
        if (ThaumcraftWoodwork.family(woodType) == null) return;
        // These are SignEditScreen's native background transforms. Text rendering,
        // filtering, editing both sides and completion packets remain native.
        graphics.pose().translate(0, 31, 0);
        graphics.pose().scale(62.500004F, 62.500004F, -62.500004F);
        WoodworkSignModels.renderStanding(graphics.pose(), graphics.bufferSource(), ThaumcraftWoodworkClient.material(woodType),
                15728880, OverlayTexture.NO_OVERLAY, state.getBlock() instanceof StandingSignBlock);
        ci.cancel();
    }
}
