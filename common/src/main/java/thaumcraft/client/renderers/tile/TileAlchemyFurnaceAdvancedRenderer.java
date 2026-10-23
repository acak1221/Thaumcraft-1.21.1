package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.ModelAdvancedAlchemyFurnace;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;

/** Original OBJ Base+four Tank groups and actual fill/heat masked quads. */
public final class TileAlchemyFurnaceAdvancedRenderer implements BlockEntityRenderer<TileAlchemyFurnaceAdvanced> {
    private static final ResourceLocation BASE=texture("alch_furnace"),BASE_ON=texture("alch_furnace_on"),TANK=texture("alch_furnace_tank"),TANK_ON=texture("alch_furnace_tank_on");
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/"+path+".png"); }
    private final ModelAdvancedAlchemyFurnace model=new ModelAdvancedAlchemyFurnace();
    private static final class ItemModel { private static final ModelAdvancedAlchemyFurnace MODEL=new ModelAdvancedAlchemyFurnace(); }
    public TileAlchemyFurnaceAdvancedRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TileAlchemyFurnaceAdvanced tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        draw(model,tile.getHeat(),tile.getVis(),pose,buffers,light,overlay);
    }
    public static void renderItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(stack.is(thaumcraft.common.config.ThaumcraftAdvancedFurnace.FURNACE_ITEM.get())) draw(ItemModel.MODEL,0,0,pose,buffers,light,overlay);
    }
    private static void draw(ModelAdvancedAlchemyFurnace model,int heat,int vis,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5,0,.5); pose.mulPose(Axis.XN.rotationDegrees(90));
            model.render("Base",pose,buffers.getBuffer(RenderType.entityCutoutNoCull(heat>100?BASE_ON:BASE)),light,overlay);
            var tank=buffers.getBuffer(RenderType.entityCutoutNoCull(vis>0?TANK_ON:TANK));
            for(int side=0;side<4;side++) { pose.pushPose(); try { pose.mulPose(Axis.ZP.rotationDegrees(side*90)); model.render("Tank",pose,tank,light,overlay); } finally { pose.popPose(); } }
            if(vis>0) {
                pose.pushPose(); try { pose.translate(.5,-.5,1.1); pose.mulPose(Axis.YP.rotationDegrees(180)); quad("thaumcraft:blocks/fluxgoo",190,0,pose,buffers,overlay); } finally { pose.popPose(); }
                float empty=1F-vis/500F;
                for(int side=0;side<4;side++) {
                    pose.pushPose(); try { pose.mulPose(Axis.ZP.rotationDegrees(side*90)); pose.mulPose(Axis.XN.rotationDegrees(90)); pose.translate(.85,-1.8,-1.4); pose.scale(.3F,.6F,1);
                    quad("thaumcraft:blocks/metalbase",150,0,pose,buffers,overlay); pose.translate(0,0,-.01); quad("thaumcraft:blocks/fluxgoo",190,empty,pose,buffers,overlay); } finally { pose.popPose(); }
                    pose.pushPose(); try { pose.mulPose(Axis.ZN.rotationDegrees(side*90)); pose.mulPose(Axis.XP.rotationDegrees(90)); pose.translate(1.15,1.8,-1.4); pose.scale(-.3F,-.6F,-1);
                    quad("thaumcraft:blocks/metalbase",150,0,pose,buffers,overlay); pose.translate(0,0,.01); quad("thaumcraft:blocks/fluxgoo",190,empty,pose,buffers,overlay); } finally { pose.popPose(); }
                }
            }
            if(heat>100) {
                pose.pushPose(); try { pose.translate(0,0,1);
                    for(int side=0;side<4;side++) {
                        pose.pushPose(); try { pose.mulPose(Axis.ZP.rotationDegrees(side*90)); pose.mulPose(Axis.XP.rotationDegrees(135)); pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.translate(-.5,0,-1);
                            quad("minecraft:block/lava_still",220,1F-Math.min(1F,heat/500F),pose,buffers,overlay);
                        pose.translate(0,0,.05); quad("thaumcraft:blocks/metalbase",150,0,pose,buffers,overlay); } finally { pose.popPose(); }
                    }
                } finally {
                    pose.popPose();
                }
            }
        } finally { pose.popPose(); }
    }
    private static void quad(String id,int brightness,float empty,PoseStack pose,MultiBufferSource buffers,int overlay) {
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse(id));
        var output=buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        float[] xy={0,1,1,1,1,empty,0,empty};
        int light=brightness; // Original Tessellator.setBrightness literal packed lightmap value, sky component zero.
        for(int vertex=0;vertex<4;vertex++) output.addVertex(pose.last(),xy[vertex*2],xy[vertex*2+1],0).setColor(-1)
                .setUv(vertex==0 || vertex==3?sprite.getU0():sprite.getU1(),vertex<2?sprite.getV1():sprite.getV0())
                .setOverlay(overlay).setLight(light).setNormal(pose.last(),0,0,-1);
    }
    public static AABB renderBounds(TileAlchemyFurnaceAdvanced tile) { var pos=tile.getBlockPos(); return new AABB(pos.getX()-1,pos.getY(),pos.getZ()-1,pos.getX()+2,pos.getY()+2,pos.getZ()+2); }
    @Override public boolean shouldRenderOffScreen(TileAlchemyFurnaceAdvanced tile) { return true; }
    @Override public int getViewDistance() { return 64; }
    @Override public boolean shouldRender(TileAlchemyFurnaceAdvanced tile,Vec3 camera) { return renderBounds(tile).distanceToSqr(camera)<=64*64; }
}
