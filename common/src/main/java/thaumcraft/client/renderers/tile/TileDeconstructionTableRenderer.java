package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.models.ModelArcaneWorkbench;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.tiles.TileDeconstructionTable;

/** Original workbench geometry/decontable skin, persistent thaumometer, real input and unclaimed primal. */
public final class TileDeconstructionTableRenderer implements BlockEntityRenderer<TileDeconstructionTable> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/decontable.png");
    private final ModelArcaneWorkbench model = new ModelArcaneWorkbench();
    private final ItemRenderer items;
    public TileDeconstructionTableRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }
    @Override public void render(TileDeconstructionTable table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); pose.translate(.5, 1, .5); pose.mulPose(Axis.XP.rotationDegrees(180));
        model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay); pose.popPose();
        if (table.getLevel() == null) return;
        pose.pushPose(); pose.translate(.5, .92, .5); pose.scale(.8F, .8F, .8F);
        items.renderStatic(new ItemStack(ThaumcraftContent.THAUMOMETER.get()), ItemDisplayContext.GROUND, light, overlay, pose, buffers, table.getLevel(), 0); pose.popPose();
        var viewer = Minecraft.getInstance().getCameraEntity(); float ticks = (viewer == null ? table.getLevel().getGameTime() : viewer.tickCount) + partial;
        if (!table.getItem(0).isEmpty()) {
            pose.pushPose(); pose.translate(.5, 1.15 + (Mth.sin(ticks / 14F) * .2F + .2F) * .1F, .5); pose.mulPose(Axis.YP.rotationDegrees(ticks % 360));
            items.renderStatic(table.getItem(0).copyWithCount(1), ItemDisplayContext.GROUND, light, overlay, pose, buffers, table.getLevel(), 0); pose.popPose();
        }
        if (table.reward() != null) {
            pose.pushPose(); pose.translate(.5, 1.081, .5); pose.mulPose(Axis.XP.rotationDegrees(90)); pose.mulPose(Axis.ZP.rotationDegrees(ticks % 360)); pose.scale(.024F, .024F, .024F);
            var aspect = table.reward(); int color = aspect.getColor(); var out = buffers.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
            float[] xy = {-8,8,8,8,8,-8,-8,-8};
            for (int vertex = 0; vertex < 4; vertex++) out.addVertex(pose.last(), xy[vertex * 2], xy[vertex * 2 + 1], 0)
                    .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 204).setUv(vertex == 0 || vertex == 3 ? 0 : 1, vertex < 2 ? 0 : 1)
                    .setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
            pose.popPose();
        }
    }
}
