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
import thaumcraft.common.config.ThaumcraftVisCharging;
import thaumcraft.common.tiles.TileMagicWorkbenchCharger;
import thaumcraft.common.tiles.TileVisRelay;

/** Original charger mesh: floating ring, four supports and the attuned crystal, with a native parent beam. */
public final class TileMagicWorkbenchChargerRenderer implements BlockEntityRenderer<TileMagicWorkbenchCharger> {
    private static final RenderType BASE = VisNetRender.modelType("charger_base", "vis_relay", false);
    private static final RenderType CRYSTAL = VisNetRender.modelType("charger_crystal", "vis_relay", true);
    private static volatile ModelVisRelay itemModel;
    private final ModelVisRelay model;
    private final Map<TileMagicWorkbenchCharger, BeamView> beams = new WeakHashMap<>();
    private static final class BeamView {
        int lastTick; float opacity = .3F;
        BeamView(int tick) { lastTick = tick; }
        float observe(TileMagicWorkbenchCharger charger) {
            int now = VisNetRender.cameraTicks(), elapsed = Math.max(0, now - lastTick);
            opacity = Math.max(.3F, opacity - elapsed * .025F); lastTick = now;
            if (charger.getPulse() > 0) opacity = .8F;
            return opacity;
        }
    }
    public TileMagicWorkbenchChargerRenderer(BlockEntityRendererProvider.Context context) {
        model = new ModelVisRelay(Minecraft.getInstance().getResourceManager()); itemModel = model;
    }
    @Override public void render(TileMagicWorkbenchCharger tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var parentRef = tile.getParent(); TileVisNode parent = parentRef == null ? null : parentRef.get();
        boolean linked = liveParent(tile, parent);
        renderModel(model, tile.getAttunement(), linked, VisNetRender.cameraTicks() + partialTick, pose, buffers, light, overlay);
        if (!linked) { beams.remove(tile); return; }
        if (!VisNetRender.flush(buffers)) return;
        float alpha = beams.computeIfAbsent(tile, ignored -> new BeamView(VisNetRender.cameraTicks())).observe(tile);
        var viewer = Minecraft.getInstance().player;
        if (viewer == null) return;
        if (!(viewer.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof IRevealer)) alpha *= .1F;
        Vec3 start = Vec3.atCenterOf(parent.getBlockPos()).subtract(Vec3.atLowerCornerOf(tile.getBlockPos()));
        if (parent instanceof TileVisRelay relay) start = start.subtract(Vec3.atLowerCornerOf(relay.getOrientation().getNormal()).scale(.05));
        else if (parent instanceof TileMagicWorkbenchCharger charger) start = start.subtract(Vec3.atLowerCornerOf(charger.getOrientation().getNormal()).scale(.05));
        int rgb = Mth.clamp((int) (tile.getPulseRed() * 255), 0, 255) << 16
                | Mth.clamp((int) (tile.getPulseGreen() * 255), 0, 255) << 8 | Mth.clamp((int) (tile.getPulseBlue() * 255), 0, 255);
        VisNetRender.beam(pose, start, new Vec3(.5, .55, .5), VisNetRender.cameraTicks() + partialTick, alpha, rgb);
    }
    private static boolean liveParent(TileMagicWorkbenchCharger tile, TileVisNode parent) {
        var level = tile.getLevel();
        return level != null && parent != null && parent != tile && !parent.isRemoved() && parent.getLevel() == level
                && level.hasChunkAt(parent.getBlockPos()) && level.getBlockEntity(parent.getBlockPos()) == parent
                && tile.getBlockPos().distSqr(parent.getBlockPos()) > 0 && tile.getBlockPos().distSqr(parent.getBlockPos()) <= 64;
    }
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftVisCharging.VIS_CHARGE_RELAY_ITEM.get())) return;
        ModelVisRelay mesh = itemModel;
        if (mesh == null) { mesh = new ModelVisRelay(Minecraft.getInstance().getResourceManager()); itemModel = mesh; }
        renderModel(mesh, -1, false, VisNetRender.cameraTicks(), pose, buffers, light, overlay);
    }
    private static void renderModel(ModelVisRelay mesh, int color, boolean linked, float ticks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5, .5, .5); pose.mulPose(Axis.XP.rotationDegrees(90)); pose.mulPose(Axis.ZP.rotationDegrees(45));
            mesh.render("RingFloat", pose, buffers.getBuffer(BASE), light, overlay, 0xffffff);
            pose.pushPose();
            try {
                pose.mulPose(Axis.XP.rotationDegrees(180)); pose.translate(0, 0, .5);
                for (int side = 0; side < 4; side++) { mesh.render("Support", pose, buffers.getBuffer(BASE), light, overlay, 0xffffff); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            } finally { pose.popPose(); }
            int rgb = 0xffffff;
            if (color >= 0 && color < 6) {
                int source = TileVisRelay.colorFor(color);
                rgb = Math.min(255, (source >> 16 & 255) * 255 / 200) << 16 | Math.min(255, (source >> 8 & 255) * 255 / 200) << 8 | Math.min(255, (source & 255) * 255 / 200);
            }
            int glow = (linked ? 50 : 0) + (int) (150 * (Mth.sin(ticks / 2) * .05F + .95F));
            mesh.render("Crystal", pose, buffers.getBuffer(CRYSTAL), glow, overlay, rgb);
        } finally { pose.popPose(); }
    }
    @Override public boolean shouldRenderOffScreen(TileMagicWorkbenchCharger tile) { return true; }
    @Override public boolean shouldRender(TileMagicWorkbenchCharger tile, Vec3 camera) {
        AABB bounds = new AABB(tile.getBlockPos()); var ref = tile.getParent(); var parent = ref == null ? null : ref.get();
        if (liveParent(tile, parent)) bounds = bounds.minmax(new AABB(parent.getBlockPos()));
        return bounds.distanceToSqr(camera) <= 64 * 64;
    }
    @Override public int getViewDistance() { return 64; }
}
