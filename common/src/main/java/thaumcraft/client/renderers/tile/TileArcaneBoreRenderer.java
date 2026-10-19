package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.BoreClientEffects;
import thaumcraft.client.renderers.models.ModelArcaneBore;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.tiles.TileArcaneBore;

/** Original top renderer30–90, with read-only native beam presentation. Pose
 * transforms never reorient/advance the tile or alter credit/selected targets. */
public final class TileArcaneBoreRenderer implements BlockEntityRenderer<TileArcaneBore> {
    private final ModelArcaneBore model = new ModelArcaneBore();
    private static final class ItemModel { private static final ModelArcaneBore MODEL = new ModelArcaneBore(); }
    public TileArcaneBoreRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileArcaneBore tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var turns = tile.rotation();
        float yaw = turns.x() - tile.getAimX() + partialTick * turns.velocityX();
        float tilt = turns.z() - tile.getAimZ() + partialTick * turns.velocityZ();
        renderModel(model, tile.attachment(), yaw, tilt, tile.getTopRotation(), tile.hasFocus(), partialTick, pose, buffers, light, overlay);
        BoreClientEffects.beam(tile, partialTick, pose, buffers);
    }
    /** Actual fresh empty top geometry, no detached BE, stock or gameplay update. */
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (stack.is(ThaumcraftArcaneBore.ARCANE_BORE.get().asItem())) {
            renderModel(ItemModel.MODEL, Direction.UP, 0, 0, 0, false, 0, pose, buffers, light, overlay);
        }
    }
    private static void renderModel(ModelArcaneBore model, Direction attachment, float yaw, float tilt, float spin, boolean focus,
                                    float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5, .5, .5);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            var vertices = buffers.getBuffer(BoreClientEffects.SHELL);
            pose.pushPose();
            try {
                if (attachment == Direction.DOWN) pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(0, -.5, 0);
                model.renderBase(pose, vertices, light, overlay);
            } finally { pose.popPose(); }
            pose.mulPose(Axis.ZP.rotationDegrees(tilt));
            pose.pushPose();
            try {
                pose.mulPose(Axis.ZP.rotationDegrees(90)); pose.translate(0, -.5, 0);
                model.renderNozzle(pose, vertices, light, overlay);
            } finally { pose.popPose(); }
            pose.pushPose();
            try {
                pose.mulPose(Axis.YP.rotationDegrees(spin)); pose.translate(0, .5, 0);
                model.renderEmitter(focus, pose, vertices, light, overlay);
            } finally { pose.popPose(); }
            BoreClientEffects.finish(buffers, BoreClientEffects.SHELL);
            var camera = Minecraft.getInstance().getCameraEntity();
            BoreClientEffects.vortices(pose, buffers, camera == null ? 0 : camera.tickCount, partialTick);
            pose.pushPose();
            try {
                pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.translate(0, .3, 0); pose.scale(.6F, .6F, .6F);
                model.renderJar(pose, buffers.getBuffer(BoreClientEffects.GLASS), light, overlay);
            } finally { pose.popPose(); }
            BoreClientEffects.finish(buffers, BoreClientEffects.GLASS);
        } finally { pose.popPose(); }
    }
    /** Fabric's native offscreen set avoids culling the original extended head
     * and a visible ray when the block origin alone is outside the frustum. */
    public static AABB renderBounds(TileArcaneBore tile) { return new AABB(tile.getBlockPos()).inflate(1); }
    @Override public boolean shouldRenderOffScreen(TileArcaneBore tile) { return true; }
    @Override public int getViewDistance() { return 64; }
    @Override public boolean shouldRender(TileArcaneBore tile, Vec3 camera) { return renderBounds(tile).distanceToSqr(camera) <= 64 * 64; }
}
