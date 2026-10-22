package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/**
 * Native billboards using TC4's ORIGINAL 32x32 node-strip atlas, not a block
 * model. Original TileNodeRenderer:50-199 supplies the frames, aspect colours,
 * pulse, modifier brightness, faint unaided core and normal-node core strip.
 * Full node sight is head IRevealer, otherwise main-hand thaumometer with the
 * original UtilsFX:629-654 visibility equation. The late world pass draws full
 * nodes even when opaque terrain culls their ordinary block-entity section.
 * Drain beams and special-node gameplay remain outside this renderer's scope.
 */
public final class TileNodeRenderer implements BlockEntityRenderer<TileNode> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/nodes.png");
    private static final RenderType FAINT = new NodeRenderType("faint", false, false);
    private static final RenderType ADDITIVE = new NodeRenderType("revealed_additive", true, false);
    private static final RenderType TRANSLUCENT = new NodeRenderType("revealed_translucent", true, true);
    /** The original node of a node jar keeps the depth test: only the revealed node ignores it. */
    private static final RenderType ADDITIVE_DEPTH = new NodeRenderType("jar_additive", false, false);
    private static final RenderType TRANSLUCENT_DEPTH = new NodeRenderType("jar_translucent", false, true);
    /** The original view distance of a node jar: the same sixty four blocks as a node. */
    public static final double JAR_VIEW_DISTANCE = 64.0D;
    /** The original size of the node inside a node jar. */
    public static final float JAR_NODE_SIZE = .7F;

    public TileNodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileNode node, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // Original renderNode:160-173 has NO explicit faint-branch range check.
        // Normal native BER distance/frustum culling still applies to this core.
        if (view(node, minecraft).revealed() && node.getAspects().size() > 0) return;
        int frame = (int)Math.floorMod(System.nanoTime() / 40000000L + node.getBlockPos().getX(), 32L);
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        quad(pose, buffers.getBuffer(FAINT), .5f, .1f, 0xffffff, frame, 1);
        pose.popPose();
    }

    /** Actual renderer input; revealed is the original condition, not a scan/research gate. */
    public record NodeView(boolean revealed, double range, double distance) {
        public boolean inRange() { return distance <= range; }
    }

    public static NodeView view(TileNode node, Minecraft minecraft) {
        var viewer = minecraft.player;
        if (viewer == null) return new NodeView(false, 64, Double.POSITIVE_INFINITY);
        ItemStack head = viewer.getItemBySlot(EquipmentSlot.HEAD);
        boolean headSight = head.getItem() instanceof IRevealer revealer && revealer.showNodes(head, viewer);
        boolean meterSight = !headSight && viewer.getMainHandItem().getItem() instanceof ItemThaumometer
                && visibleToThaumometer(node, minecraft);
        double distance = Math.sqrt(viewer.distanceToSqr(Vec3.atCenterOf(node.getBlockPos())));
        return new NodeView(headSight || meterSight, meterSight ? 48 : 64, distance);
    }

    /** Sight condition; actual full drawing also requires view.inRange() and nonempty current aspects. */
    public static boolean revealedTo(TileNode node, Minecraft minecraft) { return view(node, minecraft).revealed(); }
    public static double revealRange(TileNode node, Minecraft minecraft) { return view(node, minecraft).range(); }

    private static boolean visibleToThaumometer(TileNode node, Minecraft minecraft) {
        var viewer = minecraft.player;
        var pos = node.getBlockPos();
        double distance = Math.sqrt(viewer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
        if (distance < 2) return true;
        Vec3 direction = Vec3.atCenterOf(pos).subtract(viewer.getEyePosition()).normalize();
        double angle = Math.acos(direction.dot(viewer.getLookAngle()));
        // Intentional TC4 quirk, NOT degrees -> radians: joined.srg:4043 maps
        // field_74334_X to vanilla 1.7.10 bbj.aF, initialized to 70.0F (both
        // constructors, bytecode 975-977); options load uses saved * 40 + 70.
        // Modern Options.fov().get() is likewise raw degrees/default 70. Thus
        // UtilsFX:636 compares acos radians against 35.44 at default FOV, not
        // an invented .44-radian cone. Third person bypasses the angle as before.
        double threshold = .44f + minecraft.options.fov().get() / 2.0f;
        return angle < threshold && minecraft.options.getCameraType().isFirstPerson() && distance < 400
                || !minecraft.options.getCameraType().isFirstPerson() && distance < 400;
    }

    /** Called once per loaded node by the late, camera-relative world pass. */
    public static void renderRevealed(TileNode node, Minecraft minecraft, float partialTick,
                                      PoseStack pose, MultiBufferSource buffers) {
        NodeView view = view(node, minecraft);
        var aspects = node.getAspects();
        if (!view.revealed() || !view.inRange() || aspects.size() == 0) return;
        var viewer = minecraft.player;
        if (viewer == null) return;
        float age = viewer.tickCount + partialTick;
        float alpha = (float)((view.range() - view.distance()) / view.range());
        drawNode(minecraft, partialTick, pose, buffers, node.getBlockPos().getX(), aspects,
                node.getNodeType(), node.getNodeModifier(), alpha, 1F, true, age);
    }

    /**
     * The original node inside a node jar.
     *
     * <p>Oracle {@code thaumcraft/client/renderers/tile/TileNodeRenderer.java:176-199}: a node jar is
     * always visible, its node is drawn with the size {@code 0.7f}, the depth test is <em>kept</em>
     * (only a revealed node ignores it) and the view distance is the same sixty four blocks.</p>
     *
     * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:54-59}: the node of a
     * node jar is drawn one tenth of a block below the centre of the block.</p>
     */
    public static void renderJarNode(TileJarNode jar, Minecraft minecraft, float partialTick,
                                     PoseStack pose, MultiBufferSource buffers) {
        var aspects = jar.getAspects();
        if (aspects.size() == 0) return;
        var viewer = minecraft.player;
        if (viewer == null) return;
        // 1.21.1 has no Entity#distanceTo(Vec3) any more, only the squared one.
        double distance = Math.sqrt(viewer.distanceToSqr(Vec3.atCenterOf(jar.getBlockPos())));
        if (distance > JAR_VIEW_DISTANCE) return;
        float age = viewer.tickCount + partialTick;
        float alpha = (float)((JAR_VIEW_DISTANCE - distance) / JAR_VIEW_DISTANCE);
        pose.pushPose();
        // Original TileJarRenderer:56 - the node of the jar is one tenth of a block lower.
        pose.translate(0, -.1F, 0);
        drawNode(minecraft, partialTick, pose, buffers, jar.getBlockPos().getX(), aspects,
                jar.getNodeType(), jar.getNodeModifier(), alpha, JAR_NODE_SIZE, false, age);
        pose.popPose();
    }

    /** The shared body of the original {@code renderNode}. */
    private static void drawNode(Minecraft minecraft, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                 int blockX, thaumcraft.api.aspects.AspectList aspects, NodeType type,
                                 NodeModifier modifier, float alpha, float size, boolean depthIgnore, float age) {
        long now = System.nanoTime();
        int frame = (int) Math.floorMod(now / 40000000L + blockX, 32L);
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        drawNodeBody(pose, buffers, frame, aspects, type, modifier, alpha, size, depthIgnore, age, now);
        pose.popPose();
    }

    /**
     * The original {@code ItemNodeRenderer.renderItemNode}: the node of the item form.
     *
     * <p>Oracle {@code thaumcraft/client/renderers/tile/ItemNodeRenderer.java:72-170}: the node of the
     * item is drawn in the plane of the item and not facing the camera, it ignores the depth test and it
     * uses the frame {@code (nanoTime / 40000000 + 1) % 32}. The caller draws it three times, so the
     * item shows the three crossed planes of the original.</p>
     */
    public static void drawItemNode(PoseStack pose, MultiBufferSource buffers,
                                    thaumcraft.api.aspects.AspectList aspects, NodeType type,
                                    NodeModifier modifier, float alpha, float size, float age) {
        long now = System.nanoTime();
        int frame = (int) Math.floorMod(now / 40000000L + 1L, 32L);
        drawNodeBody(pose, buffers, frame, aspects, type, modifier, alpha, size, true, age, now, true);
    }

    /** The body of the node, without the camera rotation of the world. */
    private static void drawNodeBody(PoseStack pose, MultiBufferSource buffers, int frame,
                                     thaumcraft.api.aspects.AspectList aspects, NodeType type,
                                     NodeModifier modifier, float alpha, float size, boolean depthIgnore, float age,
                                     long now) {
        drawNodeBody(pose, buffers, frame, aspects, type, modifier, alpha, size, depthIgnore, age, now, false);
    }

    /**
     * The body of the node.
     *
     * <p>The item form of the original divides the alpha of the aspects by the whole
     * count of the aspects (oracle {@code ItemNodeRenderer.java:117}), the node of the
     * world divides it by the half count (oracle {@code TileNodeRenderer.java:103}).</p>
     */
    private static void drawNodeBody(PoseStack pose, MultiBufferSource buffers, int frame,
                                     thaumcraft.api.aspects.AspectList aspects, NodeType type,
                                     NodeModifier modifier, float alpha, float size, boolean depthIgnore, float age,
                                     long now, boolean itemForm) {
        pose.pushPose();
        if (modifier == NodeModifier.BRIGHT) alpha *= 1.5f;
        else if (modifier == NodeModifier.PALE) alpha *= .66f;
        else if (modifier == NodeModifier.FADING) alpha *= Mth.sin(age / 3) * .25f + .33f;
        float total = 0;
        int count = 0;
        float angle = 0;
        RenderType additive = depthIgnore ? ADDITIVE : ADDITIVE_DEPTH;
        RenderType translucent = depthIgnore ? TRANSLUCENT : TRANSLUCENT_DEPTH;
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            total += amount;
            float pulse = Mth.sin(age / (14 - count)) * .25f + .5f;
            float scale = .2f + pulse * (amount / 50f);
            angle = (float)((now / 5000000L) % (5000 + 500L * count)) / (5000 + 500f * count) * Mth.TWO_PI;
            float opacity = alpha / (itemForm ? Math.max(1, aspects.size()) : Math.max(1, aspects.size() / 2f));
            if (aspect.getBlend() == 771) opacity *= 1.5f;
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotation(angle));
            quad(pose, buffers.getBuffer(aspect.getBlend() == 771 ? translucent : additive),
                    scale * size, opacity, aspect.getColor(), frame, 0);
            pose.popPose();
            ++count;
        }
        // Reserved type rows remain distinguishable in saved/admin nodes;
        // drawing them is NOT an implementation of their gameplay/ecology.
        int strip = switch (type) {
            case NORMAL -> 1;
            case UNSTABLE -> 6;
            case DARK -> 2;
            case TAINTED -> 5;
            case PURE -> 4;
            case HUNGRY -> 3;
        };
        float coreSize = .1f + total / aspects.size() / 150;
        if (type == NodeType.HUNGRY) coreSize *= .75f;
        if (type != NodeType.UNSTABLE) pose.mulPose(Axis.ZP.rotation(angle));
        boolean dark = type == NodeType.DARK || type == NodeType.TAINTED;
        quad(pose, buffers.getBuffer(dark ? translucent : additive), coreSize * size, alpha, 0xffffff, frame, strip);
        pose.popPose();
    }

    private static void quad(PoseStack pose, VertexConsumer buffer, float size, float alpha, int color, int frame, int strip) {
        float u0 = frame / 32f, u1 = (frame + 1) / 32f;
        float v0 = strip / 32f, v1 = (strip + 1) / 32f;
        int opacity = Mth.clamp((int)(alpha * 255), 0, 255);
        // Counter-clockwise facing the camera; UVs retain the original strip orientation.
        vertex(pose, buffer, -size, -size, u1, v1, color, opacity);
        vertex(pose, buffer, size, -size, u0, v1, color, opacity);
        vertex(pose, buffer, size, size, u0, v0, color, opacity);
        vertex(pose, buffer, -size, size, u1, v0, color, opacity);
    }

    private static void vertex(PoseStack pose, VertexConsumer buffer, float x, float y, float u, float v, int color, int alpha) {
        buffer.addVertex(pose.last(), x, y, 0)
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 0, 1);
    }

    @Override public int getViewDistance() { return 64; }

    /**
     * Native state shards, with TC4's SRC_ALPHA/ONE and no depth write/cull.
     * Plain eyes() uses ONE/ONE: it would ignore the faint-core/modifier alpha
     * and expose every unaided node at full brightness.
     */
    private static final class NodeRenderType extends RenderType {
        private static final TextureStateShard NODE_TEXTURE = new TextureStateShard(TEXTURE, true, false);

        private NodeRenderType(String name, boolean ignoreDepth, boolean translucent) {
            super("thaumcraft_node_" + name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, false, true, () -> begin(ignoreDepth, translucent), () -> end(ignoreDepth, translucent));
        }

        private static void begin(boolean ignoreDepth, boolean translucent) {
            RENDERTYPE_EYES_SHADER.setupRenderState();
            NODE_TEXTURE.setupRenderState();
            (translucent ? TRANSLUCENT_TRANSPARENCY : LIGHTNING_TRANSPARENCY).setupRenderState();
            NO_CULL.setupRenderState();
            // Native 1.21.1 NO_DEPTH_TEST (GL_ALWAYS) is a no-op shard. Clear
            // LEQUAL first so an enabled terrain depth test cannot survive it.
            // Both blend paths own and restore this state only while drawing.
            if (ignoreDepth) LEQUAL_DEPTH_TEST.clearRenderState();
            (ignoreDepth ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST).setupRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void end(boolean ignoreDepth, boolean translucent) {
            COLOR_WRITE.clearRenderState();
            (ignoreDepth ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST).clearRenderState();
            if (ignoreDepth) LEQUAL_DEPTH_TEST.setupRenderState();
            NO_CULL.clearRenderState();
            (translucent ? TRANSLUCENT_TRANSPARENCY : LIGHTNING_TRANSPARENCY).clearRenderState();
            NODE_TEXTURE.clearRenderState();
            RENDERTYPE_EYES_SHADER.clearRenderState();
        }
    }
}
