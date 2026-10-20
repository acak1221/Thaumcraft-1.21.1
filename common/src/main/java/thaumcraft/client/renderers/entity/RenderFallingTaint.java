package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.EntityFallingTaint;

/** Native moving crust model, centred at the original entity's half-height yOffset. */
public final class RenderFallingTaint extends EntityRenderer<EntityFallingTaint> {
    private final BlockRenderDispatcher blocks;
    public RenderFallingTaint(EntityRendererProvider.Context context){super(context);blocks=context.getBlockRenderDispatcher();shadowRadius=.5F;}
    @Override public ResourceLocation getTextureLocation(EntityFallingTaint entity){return TextureAtlas.LOCATION_BLOCKS;}
    @Override public void render(EntityFallingTaint entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(entity.isQuarantined()||entity.getBlockState().isAir()||entity.level().getBlockState(entity.blockPosition()).getBlock()==entity.getBlockState().getBlock())return;
        pose.pushPose();try{pose.translate(-.5D,-.5D,-.5D);blocks.renderSingleBlock(entity.getBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);}finally{pose.popPose();}
        super.render(entity,yaw,partial,pose,buffers,light);
    }
}
