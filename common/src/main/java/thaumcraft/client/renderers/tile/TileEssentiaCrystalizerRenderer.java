package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.models.ModelEssentiaCrystalizer;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;

/** Original renderer:41-88, actively checked against pinned OpenTC4. Full shell,
 * four relay Crystal parts, literal orientation/quarterturn/translation order. */
public final class TileEssentiaCrystalizerRenderer implements BlockEntityRenderer<TileEssentiaCrystalizer> {
    private static final RenderType SHELL=new ShellType(),CRYSTALS=new CrystalType();
    private static volatile ModelEssentiaCrystalizer itemModel;
    private final ModelEssentiaCrystalizer model;
    public TileEssentiaCrystalizerRenderer(BlockEntityRendererProvider.Context context){model=new ModelEssentiaCrystalizer(Minecraft.getInstance().getResourceManager());itemModel=model;}
    @Override public void render(TileEssentiaCrystalizer tile,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){render(model,tile.getFacing(),tile.getSpin()+tile.getSpinIncrement()*partialTick,tile.getRed(),tile.getGreen(),tile.getBlue(),partialTick,pose,buffers,light,overlay);}
    /** Six-argument builtin item hook: original fresh DOWN/white/spin0 mesh,
     * SAME retained sheets and parts, no fake BE/world or saved contents gauge. */
    public static void renderItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!stack.is(ThaumcraftTubes.ESSENTIA_CRYSTALLIZER_ITEM.get()))return;ModelEssentiaCrystalizer model=itemModel;if(model==null){model=new ModelEssentiaCrystalizer(Minecraft.getInstance().getResourceManager());itemModel=model;}render(model,Direction.DOWN,0,1,1,1,0,pose,buffers,light,overlay);}
    private static void render(ModelEssentiaCrystalizer model,Direction facing,float spin,float red,float green,float blue,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        switch(facing){case DOWN->pose.mulPose(Axis.XP.rotationDegrees(-90));case UP->pose.mulPose(Axis.XP.rotationDegrees(90));case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(Axis.YP.rotationDegrees(90));case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));case NORTH->{}}
        pose.translate(0,0,-.5);model.renderShell(pose,buffers.getBuffer(SHELL),light,overlay);
        var camera=Minecraft.getInstance().getCameraEntity();int ticks=camera==null?0:camera.tickCount;
        for(int q=0;q<4;q++){pose.pushPose();pose.scale(.75F,.75F,.75F);float glow=Mth.sin((ticks+partialTick+q*10)/2F)*.05F+.95F;
            // Original gl lightmap coordinates (j,0), not a new fullbright RGB
            // pulse. Native packed light's LOW16 coordinate is that same j.
            int glowLight=50+(int)(150*glow);pose.mulPose(Axis.ZP.rotationDegrees(90*q));pose.translate(.34,0,1.2125);pose.mulPose(Axis.ZP.rotationDegrees(spin));model.renderCrystal(pose,buffers.getBuffer(CRYSTALS),glowLight,overlay,red,green,blue);pose.popPose();}
        pose.popPose();
    }
    // Native TRIANGLES, not Minecraft's quad entity buffers. No triangle-as-quad
    // translucency sorting: preserve the original immediate four-part order.
    private static final class ShellType extends RenderType {
        private static final TextureStateShard TEXTURE=new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/crystalizer.png"),false,false);
        private ShellType(){super("thaumcraft_crystalizer_shell",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,32768,false,false,ShellType::begin,ShellType::end);}
        private static void begin(){RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState();TEXTURE.setupRenderState();NO_TRANSPARENCY.setupRenderState();NO_CULL.setupRenderState();LIGHTMAP.setupRenderState();OVERLAY.setupRenderState();LEQUAL_DEPTH_TEST.setupRenderState();COLOR_DEPTH_WRITE.setupRenderState();}
        private static void end(){COLOR_DEPTH_WRITE.clearRenderState();LEQUAL_DEPTH_TEST.clearRenderState();OVERLAY.clearRenderState();LIGHTMAP.clearRenderState();NO_CULL.clearRenderState();NO_TRANSPARENCY.clearRenderState();TEXTURE.clearRenderState();RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();}
    }
    private static final class CrystalType extends RenderType {
        private static final TextureStateShard TEXTURE=new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/vis_relay.png"),false,false);
        private CrystalType(){super("thaumcraft_crystalizer_crystals",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,16384,false,false,CrystalType::begin,CrystalType::end);}
        private static void begin(){RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();TEXTURE.setupRenderState();TRANSLUCENT_TRANSPARENCY.setupRenderState();NO_CULL.setupRenderState();LIGHTMAP.setupRenderState();OVERLAY.setupRenderState();LEQUAL_DEPTH_TEST.setupRenderState();COLOR_DEPTH_WRITE.setupRenderState();}
        private static void end(){COLOR_DEPTH_WRITE.clearRenderState();LEQUAL_DEPTH_TEST.clearRenderState();OVERLAY.clearRenderState();LIGHTMAP.clearRenderState();NO_CULL.clearRenderState();TRANSLUCENT_TRANSPARENCY.clearRenderState();TEXTURE.clearRenderState();RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();}
    }
}
