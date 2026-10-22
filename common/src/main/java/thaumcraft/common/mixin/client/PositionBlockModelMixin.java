package thaumcraft.common.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.renderers.block.PositionBlockModels;

/** Vanilla/NeoForge terrain path; Fabric's Indigo path has its own thin adapter. */
@Mixin(ModelBlockRenderer.class)
public abstract class PositionBlockModelMixin {
    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JI)V", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$positionGeometry(BlockAndTintGetter world, BakedModel model, BlockState state,
            BlockPos pos, PoseStack poses, VertexConsumer vertices, boolean checkSides, RandomSource random,
            long seed, int overlay, CallbackInfo ci) {
        if (!PositionBlockModels.handles(state)) return;
        BakedModel geometry = PositionBlockModels.worldModel(world,pos,state,model);
        if (thaumcraft.client.renderers.block.LegacyBlockQuads.handles(state)) {
            poses.pushPose();
            var offset = state.getOffset(world,pos);
            poses.translate(offset.x,offset.y,offset.z);
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            for (var quad : geometry.getQuads(state,null,random)) {
                int color = quad.isTinted() ? minecraft.getBlockColors().getColor(state,world,pos,quad.getTintIndex()) : 0xffffff;
                float shade = world.getShade(quad.getDirection(),quad.isShade());
                int light = quad instanceof thaumcraft.client.renderers.block.LegacyBlockQuads.LitQuad lit
                        ? lit.light() : net.minecraft.client.renderer.LevelRenderer.getLightColor(world,state,pos.relative(quad.getDirection()));
                vertices.putBulkData(poses.last(),quad,new float[]{shade,shade,shade,shade},(color>>16&255)/255F,
                        (color>>8&255)/255F,(color&255)/255F,1F,new int[]{light,light,light,light},overlay,true);
            }
            poses.popPose();
            ci.cancel();
            return;
        }
        ((ModelBlockRenderer)(Object)this).tesselateWithoutAO(world,geometry,state,pos,poses,vertices,
                checkSides,random,seed,overlay);
        ci.cancel();
    }
}
