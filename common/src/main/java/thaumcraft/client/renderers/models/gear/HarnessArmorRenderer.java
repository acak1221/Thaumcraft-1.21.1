package thaumcraft.client.renderers.models.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.ThaumcraftTravelGearClient;
import thaumcraft.common.items.armor.HarnessData;
import thaumcraft.common.lib.geometry.HarnessObjMesh;

/** Original chest cube and retained124-triangle backpack, explicit sneak
 * transforms and fullbright back. No default armor geometry is substituted. */
public final class HarnessArmorRenderer {
    private static final ResourceLocation CHEST=texture("textures/models/hoverharness.png"),BACK=texture("textures/models/hoverharness2.png"),MESH=texture("textures/models/hoverharness.obj");
    private static final ModelPart BODY=body();
    private static HarnessObjMesh mesh;
    private HarnessArmorRenderer() {}
    private static ResourceLocation texture(String path) {return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static ModelPart body() {var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4,new CubeDeformation(.6F)),PartPose.ZERO);
        // Original ModelBiped defaults remain64x32 even though the retained
        // custom chest image is128x64. Preserve those normalized UVs.
        return LayerDefinition.create(mesh,64,32).bakeRoot().getChild("body");}
    public static void reset() {mesh=null;}
    private static HarnessObjMesh mesh() {
        if(mesh==null)try(var input=Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).open()) {mesh=HarnessObjMesh.read(new InputStreamReader(input,StandardCharsets.UTF_8));}
        catch(IOException invalid) {throw new IllegalStateException("Cannot load retained hover harness mesh",invalid);}return mesh;
    }
    public static void render(PoseStack pose,MultiBufferSource buffers,ItemStack armor,LivingEntity entity,EquipmentSlot slot,int light,HumanoidModel<LivingEntity> context) {
        if(slot!=EquipmentSlot.CHEST||!HarnessData.isHarness(armor))return;
        pose.pushPose();try {
            if(entity.isShiftKeyDown())pose.mulPose(Axis.XP.rotationDegrees(28.64789F));
            BODY.copyFrom(context.body);BODY.render(pose,ItemRenderer.getArmorFoilBuffer(buffers,RenderType.armorCutoutNoCull(CHEST),armor.hasFoil()),light,OverlayTexture.NO_OVERLAY);
        }finally {pose.popPose();}
        pose.pushPose();try {
            pose.scale(.1F,.1F,.1F);pose.mulPose(Axis.XN.rotationDegrees(90));
            if(entity.isShiftKeyDown())pose.mulPose(Axis.XP.rotationDegrees(28.64789F));
            pose.translate(0,.33,-3.7);VertexConsumer vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(BACK));
            for(var triangle:mesh().triangles()) {
                float averageU=(triangle.a().u()+triangle.b().u()+triangle.c().u())/3F,averageV=(triangle.a().v()+triangle.b().v()+triangle.c().v())/3F;
                vertex(pose,vertices,triangle.a(),averageU,averageV);vertex(pose,vertices,triangle.b(),averageU,averageV);vertex(pose,vertices,triangle.c(),averageU,averageV);
                // Native entity consumers are quad batches: a repeated final
                // vertex preserves exactly one triangle and one degenerate.
                vertex(pose,vertices,triangle.c(),averageU,averageV);
            }
        }finally {pose.popPose();}
        ThaumcraftTravelGearClient.renderHarnessEffects(pose,buffers,armor,entity);
    }
    private static void vertex(PoseStack pose,VertexConsumer out,HarnessObjMesh.Vertex vertex,float averageU,float averageV) {
        float u=vertex.u()+(vertex.u()>averageU?-.0005F:.0005F),v=vertex.v()+(vertex.v()>averageV?-.0005F:.0005F);
        out.addVertex(pose.last(),vertex.x(),vertex.y(),vertex.z()).setColor(0xffffffff).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),vertex.nx(),vertex.ny(),vertex.nz());
    }
}
