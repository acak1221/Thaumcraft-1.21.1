package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusPrimal;

/** Original ModelWand:173-210. Direct textures keep its tiny animated depth strip outside the global block atlas. */
public final class PrimalWandFocusRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand.png");
    private static final ResourceLocation DEPTH = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/focus_primal_depth.png");
    private static ModelPart shell;
    private PrimalWandFocusRenderer() {}

    private static ModelPart shell() {
        if (shell == null) {
            var mesh = new MeshDefinition();
            // The original addBox happens while the renderer's texture size is32x32.
            // Its later setTextureSize64x32/mirror assignment does not rebuild already-created cube UVs.
            mesh.getRoot().addOrReplaceChild("focus", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6), PartPose.ZERO);
            shell = LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("focus");
        }
        return shell;
    }

    /** ItemRenderer has already applied the selected model's display transform and its -.5 centering. */
    public static void render(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof ItemWandCasting)) return;
        ItemStack focus = FocusData.get(stack);
        if (!(focus.getItem() instanceof ItemFocusPrimal primal)) return;
        boolean staff = WandMaterials.rod(ItemWandCasting.data(stack).rod()).staff();
        var player = Minecraft.getInstance().player;
        int ticks = player == null ? 0 : player.tickCount;
        int pulse = Geometry.pulse(ticks);
        // Modern emissive rendering keeps ambient world light from masking the original pulse.
        // Original lightmap coordinate195..215 is represented by continuous per-vertex brightness at fullbright.
        float brightness = pulse / 240F;
        int color = primal.getFocusColor(focus);
        int red = (int) ((color >> 16 & 255) * brightness), green = (int) ((color >> 8 & 255) * brightness), blue = (int) ((color & 255) * brightness);
        int tint = 0x99000000 | red << 16 | green << 8 | blue; // Original alpha0.6, not a second opaque focus cube.
        pose.pushPose();
        try {
            // Centers of the existing native wand/staff top-cap shapes; old item renderer's X180 reverses model Y/Z.
            pose.translate(.5D, Geometry.anchor(staff), .5D);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.pushPose();
            try {
                Geometry.Depth depth = Geometry.depth(staff);
                pose.translate(0, depth.translationY(), 0); pose.scale((float) depth.x(), (float) depth.y(), (float) depth.z());
                renderDepth(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DEPTH)), light, overlay, Geometry.frame(ticks));
            } finally { pose.popPose(); }
            if (staff) { pose.translate(0, -.0475D, 0); pose.scale(.525F, .5525F, .525F); }
            else pose.scale(.5F, .5F, .5F);
            shell().render(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)), LightTexture.FULL_BRIGHT, overlay, tint);
        } finally { pose.popPose(); }
    }

    private static void renderDepth(PoseStack pose, VertexConsumer out, int light, int overlay, int frame) {
        float h = .5F, v0 = frame / 30F, v1 = (frame + 1) / 30F;
        face(pose,out,light,overlay,v0,v1,0,-1,0, new float[][]{{-h,-h,-h},{h,-h,-h},{h,-h,h},{-h,-h,h}});
        face(pose,out,light,overlay,v0,v1,0,1,0, new float[][]{{-h,h,h},{h,h,h},{h,h,-h},{-h,h,-h}});
        face(pose,out,light,overlay,v0,v1,0,0,-1, new float[][]{{h,-h,-h},{-h,-h,-h},{-h,h,-h},{h,h,-h}});
        face(pose,out,light,overlay,v0,v1,0,0,1, new float[][]{{-h,-h,h},{h,-h,h},{h,h,h},{-h,h,h}});
        face(pose,out,light,overlay,v0,v1,-1,0,0, new float[][]{{-h,-h,-h},{-h,-h,h},{-h,h,h},{-h,h,-h}});
        face(pose,out,light,overlay,v0,v1,1,0,0, new float[][]{{h,-h,h},{h,-h,-h},{h,h,-h},{h,h,h}});
    }
    private static void face(PoseStack pose, VertexConsumer out, int light, int overlay, float v0, float v1,
                             float nx, float ny, float nz, float[][] points) {
        for (int index = 0; index < 4; index++) {
            float[] point = points[index];
            out.addVertex(pose.last(),point[0],point[1],point[2]).setColor(0xffffffff)
                    .setUv(index == 1 || index == 2 ? 1F : 0F, index < 2 ? v1 : v0)
                    .setOverlay(overlay).setLight(light).setNormal(pose.last(),nx,ny,nz);
        }
    }

    /** Independent data/math nested class can be tested without initializing any client renderer or GL state. */
    public static final class Geometry {
        private static final int[] FRAMES = {0,1,1,2,3,4,5,6,6,7,8,9,10,11,11,12,13,14,15,16,16,17,18,19,20,21,21,22,23,24,25,26,26,27,28,29};
        public record Depth(double x, double y, double z, double translationY) {}
        private Geometry() {}
        public static double anchor(boolean staff) { return (staff ? 28.15D : 15.25D) / 16D; }
        public static Depth depth(boolean staff) { return staff ? new Depth(.165D,.1765D,.165D,-.15D) : new Depth(.16D,.16D,.16D,-.09D); }
        public static int frame(int tick) { return FRAMES[Math.floorMod(tick,FRAMES.length)]; }
        public static int pulse(int tick) { return (int)(195F + Mth.sin(tick / 3F) * 10F + 10F); }
        public static int[] frames() { return FRAMES.clone(); }
        public static double[][] depthBounds(boolean staff) {
            Depth depth = depth(staff); double center = anchor(staff) - depth.translationY();
            return new double[][]{{(0.5D-depth.x()/2)*16,(center-depth.y()/2)*16,(0.5D-depth.z()/2)*16},
                    {(0.5D+depth.x()/2)*16,(center+depth.y()/2)*16,(0.5D+depth.z()/2)*16}};
        }
    }
}
