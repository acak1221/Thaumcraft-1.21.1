package thaumcraft.neoforge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.block.PositionBlockModels;

/** NeoForge's chunk compiler calls the ModelData overload, not vanilla's entry. */
@Mixin(ModelBlockRenderer.class)
public abstract class NeoForgePositionBlockModelMixin {
    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$positionGeometry(BlockAndTintGetter world, BakedModel model, BlockState state,
            BlockPos pos, PoseStack poses, VertexConsumer vertices, boolean checkSides, RandomSource random,
            long seed, int overlay, ModelData data, RenderType layer, CallbackInfo ci) {
        if (!PositionBlockModels.handles(state)) return;
        var geometry = PositionBlockModels.worldModel(world,pos,state,model);
        ((ModelBlockRenderer)(Object)this).tesselateWithoutAO(world,geometry,state,pos,poses,vertices,
                checkSides,random,seed,overlay);
        ci.cancel();
    }
}
