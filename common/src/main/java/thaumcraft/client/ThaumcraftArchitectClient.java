package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.BlockCoordinates;
import thaumcraft.api.IArchitect;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.world.WardingOverlay;

/**
 * The world overlay of the Architect focus of the original.
 *
 * <p>Oracle {@code thaumcraft/client/lib/REHWandHandler.java:297-479}. The original draws the area
 * of the Architect focus in the world while the player looks at a block: every block of the area is
 * drawn as a translucent cube whose texture is the warded glass rune of the eight neighbours of the
 * face ({@code :311-313, :318-368}) and the axes of the preview are drawn as arrows at the anchor
 * ({@code :310, :429-479}).</p>
 *
 * <p>The numbers of the original are kept: the colour of a block is
 * {@code sin(ticks / 2 + x) * 0.2 + 0.3}, {@code sin(ticks / 3 + y) * 0.2 + 0.3} and
 * {@code sin(ticks / 4 + z) * 0.2 + 0.8} with the alpha {@code 0.2} ({@code :391-394}), the colour
 * of an arrow uses the divisors four, three and two and the alpha {@code 0.33}
 * ({@code :438-440, :449}), a face that touches another block of the area is not drawn
 * ({@code :370-372}) and the rune texture of a face is the original lookup table
 * {@code UtilsFX.connectedTextureRefByID} of the eight neighbour flags ({@code :356-366}), which the
 * port already carries in {@link WardingOverlay}.</p>
 *
 * <p>Forced 1.21.1 change: the original drew the silhouette of the jar block with the rune texture
 * of the warded glass ({@code :399-416}); the native draws the cube of the block, because the 1.7.10
 * block renderer cannot draw one model with the texture of another. The geometry of the faces, the
 * hidden inner faces, the colours and the arrows are the original's.</p>
 *
 * <p>The draw state is the original's too ({@code drawOverlayBlock} and {@code drawArchitectAxis}):
 * no depth test and no depth write, no face culling, the blend {@code SRC_ALPHA, ONE}, the cube
 * grown by {@code 0.001} on every side, and for the area the texture mode {@code GL_ADD}, which the
 * core shader {@code thaumcraft:architect_overlay} reproduces. With the depth test of an entity the
 * area lay inside the very blocks it marks and was never seen. Not reproduced: the lightmap value
 * {@code 200} the original tessellator carried; the area is drawn at full brightness.</p>
 */
public final class ThaumcraftArchitectClient {
    /** The arrow sheet of the original ({@code REHWandHandler:80}). */
    public static final ResourceLocation ARROWS =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/architect_arrows.png");
    /** The alpha of an overlay block of the original ({@code REHWandHandler:394}). */
    public static final float BLOCK_ALPHA = 0.2F;
    /** The alpha of an arrow of the original ({@code REHWandHandler:449}). */
    public static final float ARROW_ALPHA = 0.33F;
    /** The size of the arrow quad of the original ({@code REHWandHandler:453}). */
    public static final float ARROW_SIZE = 1.0F;
    /** The original render bounds of an overlay block are {@code -0.001} to {@code 1.001}. */
    public static final float GROWTH = 0.001F;

    private static ShaderInstance shader;
    private static final Map<ResourceLocation, RenderType> AREA_TYPES = new HashMap<>();

    /** The {@code thaumcraft:architect_overlay} core shader, registered by the loader client bootstrap. */
    public static void setShader(ShaderInstance instance) { shader = instance; }

    private ThaumcraftArchitectClient() {}

    /**
     * The overlay of the Architect focus.
     *
     * @param pose the pose of the world
     * @param buffers the buffers of the client
     * @param partialTick the partial tick of the frame
     * @param camera the camera of the frame
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, float partialTick, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || pose == null || buffers == null || camera == null || shader == null) return;
        ItemStack wand = player.getMainHandItem();
        ItemStack focus = FocusData.get(wand);
        if (!(focus.getItem() instanceof IArchitect architect)) return;
        // Original REHWandHandler:305 - the area is only drawn when the focus carries the
        // architect upgrade.
        if (!WandArchitect.enabled(wand)) return;
        // Original REHWandHandler:298-306 - the anchor is the block under the crosshair.
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || minecraft.hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockPos anchor = hit.getBlockPos();
        int side = hit.getDirection().ordinal();
        List<BlockCoordinates> area = architect.getArchitectBlocks(wand, minecraft.level,
                anchor.getX(), anchor.getY(), anchor.getZ(), side, player);
        if (area == null || area.isEmpty()) return;
        try {
            draw(pose, buffers, minecraft, player, wand, architect, anchor, side, area, camera, partialTick);
        } finally {
            // Draw the last batch here, in the world matrices, and not at whatever flush of the frame
            // comes next.
            if (buffers instanceof MultiBufferSource.BufferSource source) source.endLastBatch();
        }
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, Minecraft minecraft, Player player,
                             ItemStack wand, IArchitect architect, BlockPos anchor, int side,
                             List<BlockCoordinates> area, Camera camera, float partialTick) {
        Set<BlockPos> cells = new HashSet<>();
        for (BlockCoordinates cell : area) cells.add(new BlockPos(cell.x, cell.y, cell.z));
        var eye = camera.getPosition();
        float ticks = player.tickCount + partialTick;
        for (BlockCoordinates cell : area) {
            BlockPos pos = new BlockPos(cell.x, cell.y, cell.z);
            pose.pushPose();
            try {
                pose.translate(pos.getX() - eye.x, pos.getY() - eye.y, pos.getZ() - eye.z);
                drawOverlayBlock(pose, buffers, cells, pos, ticks);
            } finally { pose.popPose(); }
        }
        // Original REHWandHandler:310 - the axes of the preview are drawn at the anchor.
        boolean dx = architect.showAxis(wand, minecraft.level, player, side, IArchitect.EnumAxis.X);
        boolean dy = architect.showAxis(wand, minecraft.level, player, side, IArchitect.EnumAxis.Y);
        boolean dz = architect.showAxis(wand, minecraft.level, player, side, IArchitect.EnumAxis.Z);
        if (!(dx || dy || dz)) return;
        pose.pushPose();
        try {
            pose.translate(anchor.getX() - eye.x + 0.5D, anchor.getY() - eye.y + 0.5D, anchor.getZ() - eye.z + 0.5D);
            drawArchitectAxis(pose, buffers, anchor, ticks, dx, dy, dz);
        } finally { pose.popPose(); }
    }

    /**
     * One translucent block of the area.
     *
     * @param pose the pose of the world
     * @param buffers the buffers of the client
     * @param cells the blocks of the area
     * @param pos the block that is drawn
     * @param ticks the age of the player
     */
    private static void drawOverlayBlock(PoseStack pose, MultiBufferSource buffers, Set<BlockPos> cells,
                                         BlockPos pos, float ticks) {
        // Original REHWandHandler:391-394 - the colour of the block.
        float r = (float) Math.sin(ticks / 2.0F + pos.getX()) * 0.2F + 0.3F;
        float g = (float) Math.sin(ticks / 3.0F + pos.getY()) * 0.2F + 0.3F;
        float b = (float) Math.sin(ticks / 4.0F + pos.getZ()) * 0.2F + 0.8F;
        for (Direction face : Direction.values()) {
            // Original REHWandHandler:370-372 - a face that touches another block of the area is
            // not drawn.
            if (faceHidden(cells, pos, face)) continue;
            int texture = WardingOverlay.texture(mask(cells, pos, face));
            VertexConsumer out = buffers.getBuffer(areaType(
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/warded_glass_" + texture + ".png")));
            quad(pose, out, face, r, g, b, BLOCK_ALPHA);
        }
    }

    /**
     * Whether the face of a block of the area is hidden by another block of the area.
     *
     * <p>Original {@code REHWandHandler:370-372}: a face whose neighbour in that direction belongs
     * to the area is not drawn, so the area shows its shell only.</p>
     *
     * @param cells the blocks of the area
     * @param pos the block that is drawn
     * @param face the face that is drawn
     * @return true when the face is hidden
     */
    public static boolean faceHidden(Set<BlockPos> cells, BlockPos pos, Direction face) {
        return cells.contains(pos.relative(face));
    }

    /**
     * The eight neighbour flags of a face of the area, in the order of the original.
     *
     * <p>Original {@code REHWandHandler:323-355}: the flags are the blocks of the area that touch
     * the face, in the order the original lists them for the face.</p>
     *
     * @param cells the blocks of the area
     * @param pos the block that is drawn
     * @param face the face that is drawn
     * @return the bit mask of the eight neighbours
     */
    public static int mask(Set<BlockPos> cells, BlockPos pos, Direction face) {
        int mask = 0;
        for (int index = 0; index < 8; index++) {
            if (cells.contains(WardingOverlay.neighbour(pos, face, index))) mask |= 1 << index;
        }
        return mask;
    }

    /**
     * The axes of the preview at the anchor.
     *
     * <p>Original {@code REHWandHandler:429-479}: the arrow quad is drawn twice for every axis, the
     * second one turned by ninety degrees, so that the axis is seen from every side.</p>
     */
    private static void drawArchitectAxis(PoseStack pose, MultiBufferSource buffers, BlockPos anchor,
                                          float ticks, boolean dx, boolean dy, boolean dz) {
        // Original REHWandHandler:438-440 - the colour of the arrows.
        float r = (float) Math.sin(ticks / 4.0F + anchor.getX()) * 0.2F + 0.3F;
        float g = (float) Math.sin(ticks / 3.0F + anchor.getY()) * 0.2F + 0.3F;
        float b = (float) Math.sin(ticks / 2.0F + anchor.getZ()) * 0.2F + 0.8F;
        VertexConsumer out = buffers.getBuffer(ArchitectRenderType.ARROW_TYPE);
        pose.pushPose();
        try {
            // Original REHWandHandler:450 - the arrows lie in the plane of the screen.
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (dx) {
                pose.pushPose();
                try {
                    arrow(pose, out, r, g, b);
                    pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                    arrow(pose, out, r, g, b);
                } finally { pose.popPose(); }
            }
            if (dz) {
                pose.pushPose();
                try {
                    pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    arrow(pose, out, r, g, b);
                    pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                    arrow(pose, out, r, g, b);
                } finally { pose.popPose(); }
            }
            if (dy) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                arrow(pose, out, r, g, b);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                arrow(pose, out, r, g, b);
            }
        } finally { pose.popPose(); }
    }

    /** The arrow quad of the original ({@code UtilsFX.renderQuadCenteredFromTexture:359-376}). */
    private static void arrow(PoseStack pose, VertexConsumer out, float r, float g, float b) {
        pose.pushPose();
        try {
            pose.scale(ARROW_SIZE, ARROW_SIZE, ARROW_SIZE);
            vertex(pose, out, -0.5F, 0.5F, 0.0F, 0.0F, 1.0F, r, g, b, ARROW_ALPHA);
            vertex(pose, out, 0.5F, 0.5F, 0.0F, 1.0F, 1.0F, r, g, b, ARROW_ALPHA);
            vertex(pose, out, 0.5F, -0.5F, 0.0F, 1.0F, 0.0F, r, g, b, ARROW_ALPHA);
            vertex(pose, out, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F, r, g, b, ARROW_ALPHA);
        } finally { pose.popPose(); }
    }

    /** One face of the cube of an overlay block. */
    private static void quad(PoseStack pose, VertexConsumer out, Direction face, float r, float g, float b, float alpha) {
        float[][] corners = switch (face) {
            case DOWN -> new float[][]{{0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {0, 0, 0}};
            case UP -> new float[][]{{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}};
            case NORTH -> new float[][]{{1, 1, 0}, {0, 1, 0}, {0, 0, 0}, {1, 0, 0}};
            case SOUTH -> new float[][]{{0, 1, 1}, {1, 1, 1}, {1, 0, 1}, {0, 0, 1}};
            case WEST -> new float[][]{{0, 1, 0}, {0, 1, 1}, {0, 0, 1}, {0, 0, 0}};
            case EAST -> new float[][]{{1, 1, 1}, {1, 1, 0}, {1, 0, 0}, {1, 0, 1}};
        };
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int index = 0; index < 4; index++) {
            vertex(pose, out, grown(corners[index][0]), grown(corners[index][1]), grown(corners[index][2]),
                    uv[index][0], uv[index][1], r, g, b, alpha);
        }
    }

    /** A corner of the unit cube on the original render bounds {@code -0.001} to {@code 1.001}. */
    public static float grown(float corner) {
        return corner <= 0.0F ? -GROWTH : 1.0F + GROWTH;
    }

    private static void vertex(PoseStack pose, VertexConsumer out, float x, float y, float z,
                               float u, float v, float r, float g, float b, float alpha) {
        out.addVertex(pose.last(), x, y, z).setUv(u, v).setColor(r, g, b, alpha);
    }

    private static RenderType areaType(ResourceLocation texture) {
        return AREA_TYPES.computeIfAbsent(texture, ArchitectRenderType::area);
    }

    /** The original draw state of the area and of the arrows. */
    private static final class ArchitectRenderType extends RenderType {
        static final RenderType ARROW_TYPE = new ArchitectRenderType("thaumcraft_architect_arrows",
                new TextureStateShard(ARROWS, false, false),
                new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader));

        private ArchitectRenderType(String name, TextureStateShard texture, ShaderStateShard program) {
            super(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
                    () -> begin(texture, program), () -> end(texture, program));
        }

        static RenderType area(ResourceLocation texture) {
            return new ArchitectRenderType("thaumcraft_architect_area", new TextureStateShard(texture, false, false),
                    new ShaderStateShard(() -> shader));
        }

        private static void begin(TextureStateShard texture, ShaderStateShard program) {
            program.setupRenderState();
            texture.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState();
            NO_CULL.setupRenderState();
            // Native NO_DEPTH_TEST is a no-op for GL_ALWAYS in 1.21.1; clearing the LEQUAL state
            // switches the depth test off, as the original glDisable(GL_DEPTH_TEST).
            LEQUAL_DEPTH_TEST.clearRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void end(TextureStateShard texture, ShaderStateShard program) {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            NO_CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            texture.clearRenderState();
            program.clearRenderState();
        }
    }
}
