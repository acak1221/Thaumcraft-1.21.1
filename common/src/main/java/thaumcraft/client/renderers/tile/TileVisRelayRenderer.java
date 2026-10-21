package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.client.renderers.models.ModelVisRelay;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.tiles.TileVisRelay;
import thaumcraft.common.tiles.TileMagicWorkbenchCharger;

/** Original relay geometry/orientations and native parent beam. Java weak parent
 * is dereferenced ONCE per draw; no attachment/debit mutation in this renderer. */
public final class TileVisRelayRenderer implements BlockEntityRenderer<TileVisRelay> {
    private static final RenderType BASE=VisNetRender.modelType("relay_base","vis_relay",false),CRYSTAL=VisNetRender.modelType("relay_crystal","vis_relay",true);
    private static volatile ModelVisRelay itemModel;private final ModelVisRelay model;private final Map<TileVisRelay,BeamView> beams=new WeakHashMap<>();
    private static final class BeamView {int lastTick;float opacity=.3F;BeamView(int tick){lastTick=tick;}float observe(TileVisRelay relay){int now=VisNetRender.cameraTicks(),elapsed=Math.max(0,now-lastTick);opacity=Math.max(.3F,opacity-elapsed*.025F);lastTick=now;if(relay.getPulse()>0)opacity=.8F;return opacity;}}
    public TileVisRelayRenderer(BlockEntityRendererProvider.Context context){model=new ModelVisRelay(Minecraft.getInstance().getResourceManager());itemModel=model;}
    @Override public void render(TileVisRelay relay,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var reference=relay.getParent();TileVisNode parent=reference==null?null:reference.get();boolean linked=liveParent(relay,parent);renderModel(model,relay.getOrientation(),relay.getAttunement(),linked,VisNetRender.cameraTicks()+partialTick,pose,buffers,light,overlay);
        if(!linked){beams.remove(relay);return;}if(!VisNetRender.flush(buffers))return;
        Vec3 own=new Vec3(.5,.5,.5).subtract(Vec3.atLowerCornerOf(relay.getOrientation().getNormal()).scale(.05));Vec3 start=Vec3.atCenterOf(parent.getBlockPos()).subtract(Vec3.atLowerCornerOf(relay.getBlockPos()));if(parent instanceof TileVisRelay other)start=start.subtract(Vec3.atLowerCornerOf(other.getOrientation().getNormal()).scale(.05));else if(parent instanceof TileMagicWorkbenchCharger charger)start=start.subtract(Vec3.atLowerCornerOf(charger.getOrientation().getNormal()).scale(.05));
        float opacity=beams.computeIfAbsent(relay,ignored->new BeamView(VisNetRender.cameraTicks())).observe(relay);var viewer=Minecraft.getInstance().player;if(viewer==null)return;ItemStack head=viewer.getItemBySlot(EquipmentSlot.HEAD);boolean revealer=head.getItem() instanceof IRevealer;
        // Original FXBeamPower:154-158 and :187-191 - the beam is ten times fainter than the flare
        // and both of them are ten times brighter for a viewer that wears a revealer.
        float beamOpacity=revealer?opacity:opacity*.1F;float flareOpacity=revealer?opacity:opacity*.2F;
        int colour=Mth.clamp((int)(relay.getPulseRed()*255),0,255)<<16|Mth.clamp((int)(relay.getPulseGreen()*255),0,255)<<8|Mth.clamp((int)(relay.getPulseBlue()*255),0,255);float ticks=VisNetRender.cameraTicks()+partialTick;VisNetRender.beam(pose,start,own,ticks,beamOpacity,colour);VisNetRender.flare(pose,own,ticks,.66F*opacity,flareOpacity,colour,light);
    }
    private static boolean liveParent(TileVisRelay relay,TileVisNode parent){var level=relay.getLevel();return level!=null && parent!=null && parent!=relay && !parent.isRemoved() && parent.getLevel()==level && level.hasChunkAt(parent.getBlockPos()) && level.getBlockEntity(parent.getBlockPos())==parent && relay.getBlockPos().distSqr(parent.getBlockPos())>0 && relay.getBlockPos().distSqr(parent.getBlockPos())<=64;}
    public static void renderItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!stack.is(ThaumcraftNodeDevices.VIS_RELAY_ITEM.get()))return;ModelVisRelay mesh=itemModel;if(mesh==null){mesh=new ModelVisRelay(Minecraft.getInstance().getResourceManager());itemModel=mesh;}renderModel(mesh,Direction.UP,-1,false,VisNetRender.cameraTicks(),pose,buffers,light,overlay);}
    private static void renderModel(ModelVisRelay mesh,Direction facing,int attunement,boolean linked,float ticks,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();try{pose.translate(.5,.5,.5);switch(facing){case DOWN->pose.mulPose(Axis.XN.rotationDegrees(90));case UP->pose.mulPose(Axis.XP.rotationDegrees(90));case NORTH->{}case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(Axis.YP.rotationDegrees(90));case EAST->pose.mulPose(Axis.YN.rotationDegrees(90));}pose.mulPose(Axis.XP.rotationDegrees(180));pose.mulPose(Axis.ZP.rotationDegrees(45));
            pose.pushPose();try{pose.scale(.75F,.75F,.75F);pose.translate(0,0,-.16);mesh.render("RingBase",pose,buffers.getBuffer(BASE),light,overlay,0xffffff);}finally{pose.popPose();}mesh.render("RingFloat",pose,buffers.getBuffer(BASE),light,overlay,0xffffff);
            int colour=0xffffff;if(attunement>=0 && attunement<6){int source=TileVisRelay.colorFor(attunement);colour=Math.min(255,(source>>16&255)*255/200)<<16|Math.min(255,(source>>8&255)*255/200)<<8|Math.min(255,(source&255)*255/200);}int glow=(linked?50:0)+(int)(150F*(Mth.sin(ticks/2F)*.05F+.95F));mesh.render("Crystal",pose,buffers.getBuffer(CRYSTAL),glow,overlay,colour);
        }finally{pose.popPose();}
    }
    /** Include the real beam segment in culling, not just the half-cell model. */
    @Override public boolean shouldRenderOffScreen(TileVisRelay relay){return true;}
    @Override public boolean shouldRender(TileVisRelay relay,Vec3 camera){AABB bounds=new AABB(relay.getBlockPos());var reference=relay.getParent();var parent=reference==null?null:reference.get();if(liveParent(relay,parent))bounds=bounds.minmax(new AABB(parent.getBlockPos()));return bounds.distanceToSqr(camera)<=64*64;}
    @Override public int getViewDistance(){return 64;}
}
