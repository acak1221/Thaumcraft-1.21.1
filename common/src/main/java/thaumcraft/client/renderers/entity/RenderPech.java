package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import thaumcraft.client.renderers.models.ModelPech;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.items.wands.ItemWandCasting;

/** Original three skins and right-arm item transform; current native item
 * models render their own held geometry/components instead of removed Forge
 * IItemRenderer passes. Invalid saved variant bytes use the Forager skin rather
 * than reproducing the original array-index renderer crash. */
public final class RenderPech extends MobRenderer<EntityPech,ModelPech> {
    private static final ResourceLocation[] SKINS={texture("pech_forage"),texture("pech_thaum"),texture("pech_stalker")};
    public RenderPech(EntityRendererProvider.Context context){
        super(context,new ModelPech(),.5F);
        addLayer(new RenderLayer<EntityPech,ModelPech>(this){
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,EntityPech pech,float limb,float amount,float partial,float age,float yaw,float pitch){
                var held=pech.getMainHandItem();if(held.isEmpty())return;pose.pushPose();
                try{
                    getParentModel().rightArm().translateAndRotate(pose);pose.translate(-.0625,.3375,.0625);
                    if(held.getItem() instanceof BlockItem&&context.getItemRenderer().getModel(held,pech.level(),pech,0).isGui3d()){pose.translate(0,.1875,-.3125);pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(45));pose.scale(-.375F,-.375F,.375F);}
                    else if(held.is(Items.BOW)){pose.translate(0,.125,.3125);pose.mulPose(Axis.YP.rotationDegrees(-20));pose.scale(.625F,-.625F,.625F);pose.mulPose(Axis.XP.rotationDegrees(-100));pose.mulPose(Axis.YP.rotationDegrees(45));}
                    else if(held.getItem() instanceof ItemWandCasting||held.getItem() instanceof net.minecraft.world.item.TieredItem||held.is(Items.FISHING_ROD)){
                        if(held.getItem() instanceof ItemWandCasting)pose.translate(0,-.125,0);
                        if(held.is(Items.FISHING_ROD)){pose.mulPose(Axis.ZP.rotationDegrees(180));pose.translate(0,-.125,0);}
                        pose.translate(0,.1875,0);pose.scale(.625F,-.625F,.625F);pose.mulPose(Axis.XP.rotationDegrees(-100));pose.mulPose(Axis.YP.rotationDegrees(45));
                    }else{pose.translate(.25,.1875,-.1875);pose.scale(.375F,.375F,.375F);pose.mulPose(Axis.ZP.rotationDegrees(60));pose.mulPose(Axis.XP.rotationDegrees(-90));pose.mulPose(Axis.ZP.rotationDegrees(20));}
                    context.getItemInHandRenderer().renderItem(pech,held,ItemDisplayContext.NONE,false,pose,buffers,light);
                }finally{pose.popPose();}
            }
        });
    }
    private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/"+name+".png");}
    @Override public ResourceLocation getTextureLocation(EntityPech pech){int type=pech.getPechType();return SKINS[type>=0&&type<3?type:0];}
    @Override public void render(EntityPech pech,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();try{if(pech.isShiftKeyDown())pose.translate(0,-.125,0);super.render(pech,yaw,partial,pose,buffers,light);}finally{pose.popPose();}}
}
