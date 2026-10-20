package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.BlockCrystal;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.lib.crystals.CrystalClusterGeometry;
import thaumcraft.common.tiles.TileCrystal;

/** Native ModelPart port of original ModelCrystal/TileCrystalRenderer, including mixed color order and positional seed. */
public final class TileCrystalRenderer implements BlockEntityRenderer<TileCrystal> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/crystal.png");
    private static volatile ModelPart itemModel;
    private final ModelPart model;
    public TileCrystalRenderer(BlockEntityRendererProvider.Context context) { model = model(); itemModel = model; }
    private static ModelPart model() {
        var mesh = new MeshDefinition();
        // Original mirror=true is assigned AFTER addBox, so it does not retroactively reverse the cube's baked UVs.
        mesh.getRoot().addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(0, 0).addBox(-16, -16, 0, 16, 16, 16),
                PartPose.offsetAndRotation(0, 32, 0, .7071F, 0, .7071F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("crystal");
    }
    @Override public void render(TileCrystal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        render(model, ThaumcraftCrystalClusters.metadata(tile.getBlockState()), tile.orientation, tile.getBlockPos(), pose, buffers, overlay);
    }
    public static void renderItem(ItemStack item, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(item.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof BlockCrystal)) return;
        ModelPart model = itemModel; if (model == null) { model = model(); itemModel = model; }
        pose.pushPose();
        try { pose.translate(.5, .5, .5); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(-.5, -.5, -.5);
            render(model, ThaumcraftCrystalClusters.metadata(blockItem.getBlock().defaultBlockState()), (short) 1, BlockPos.ZERO, pose, buffers, overlay);
        } finally { pose.popPose(); }
    }
    private static void render(ModelPart model, int metadata, short orientation, BlockPos position, PoseStack pose, MultiBufferSource buffers, int overlay) {
        var player = Minecraft.getInstance().player;
        var attachment = CrystalClusterGeometry.attachment(orientation);
        for (var piece : CrystalClusterGeometry.pieces(metadata, position.getX(), position.getY(), position.getZ(), player == null ? 0 : player.tickCount)) {
            pose.pushPose();
            try {
                pose.translate(attachment.x(), attachment.y(), attachment.z());
                if (attachment.angle() != 0) pose.mulPose((attachment.axis() == 'x' ? Axis.XP : Axis.ZP).rotationDegrees(attachment.angle()));
                pose.mulPose(Axis.YP.rotationDegrees(piece.yaw())); pose.mulPose(Axis.XP.rotationDegrees(piece.pitch()));
                pose.scale(piece.scaleX(), piece.scaleY(), piece.scaleZ());
                int color = piece.color();
                int red = Math.min(255, (int) ((color >> 16 & 255) / 220F * 255));
                int green = Math.min(255, (int) ((color >> 8 & 255) / 220F * 255));
                int blue = Math.min(255, (int) ((color & 255) / 220F * 255));
                model.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), piece.light(), overlay,
                        0xff000000 | red << 16 | green << 8 | blue);
            } finally { pose.popPose(); }
        }
    }
}
