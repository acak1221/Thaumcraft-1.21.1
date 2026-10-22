package thaumcraft.common.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.client.renderers.block.LegacyBlockQuads;
import thaumcraft.client.renderers.block.PositionBlockModels;
import thaumcraft.client.renderers.block.TaintOverlayRules;
import thaumcraft.common.blocks.BlockTaint;

/** Keep pass0 solid and route only the fibre overlay through the translucent chunk buffer. */
@Mixin(SectionCompiler.class)
public abstract class TaintOverlayChunkLayerMixin {
    @Shadow private BufferBuilder getOrBeginLayer(Map<RenderType,BufferBuilder> layers,
            SectionBufferBuilderPack pack,RenderType type) {throw new AssertionError();}

    // Both vanilla and patched NeoForge compile methods pop the block's local pose
    // immediately after renderBatched; unlike its signature, this call is unchanged.
    @Inject(method="compile",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void thaumcraft$transparentTaint(CallbackInfoReturnable<SectionCompiler.Results> ci,
            @Local(argsOnly=true) RenderChunkRegion world,@Local(argsOnly=true) SectionBufferBuilderPack pack,
            @Local(ordinal=2) BlockPos pos,@Local BlockState state,@Local PoseStack poses,
            @Local Map<RenderType,BufferBuilder> layers) {
        if(!(state.getBlock() instanceof BlockTaint taint)||!TaintOverlayRules.applies(taint.metadata()))return;
        var model=Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if(model instanceof PositionBlockModels.LayeredModel)return; // Fabric already emitted both materials.
        var quads=LegacyBlockQuads.taintOverlays(world,pos,state);
        if(quads.isEmpty())return;
        LegacyBlockQuads.render(world,pos,state,poses,getOrBeginLayer(layers,pack,RenderType.translucent()),quads,OverlayTexture.NO_OVERLAY);
    }
}
