package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.IGoggles;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.client.renderers.tile.TileNodeRenderer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.tiles.TileNode;

/**
 * World optics only: original RenderEventHandler:215-239,250-255,287-363.
 * This is not the inventory Shift aspect HUD, a scan, or a through-wall pick.
 * Architectury 13 has no world-render event; loader bootstrap supplies LAST.
 */
public final class ThaumcraftGogglesClient {
    public static final ResourceLocation UNKNOWN_ASPECT = ResourceLocation.fromNamespaceAndPath(
            "thaumcraft", "textures/aspects/_unknown.png");
    private static final Map<ResourceLocation, RenderType> ICON_TYPES = new HashMap<>();
    private static final Map<RenderType, RenderType> TEXT_TYPES = new HashMap<>();
    private static boolean initialized;
    private static float tagScale;

    private ThaumcraftGogglesClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> tagScale = 0);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> tagScale = 0);
    }

    /** Storage tag is never printed; unknown entries render only their tinted unknown image and amount. */
    public record AspectView(String tag, int amount, boolean known, ResourceLocation image, int color) {}

    /** Immutable snapshot of the SAME normal crosshair input consumed by render, not a fixture override. */
    public record TargetView(BlockPos position, Direction face, boolean spaceAbove, List<AspectView> entries, int note) {
        public TargetView {
            position = position.immutable();
            entries = List.copyOf(entries);
        }

        public String noteText() { return note >= 0 ? "Note: " + note : ""; }
    }

    /** Null means no permitted, normally targeted world popup. Reads live client-synchronized state. */
    public static TargetView targetView(Minecraft minecraft) {
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || minecraft.screen != null || minecraft.options.hideGui
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof IGoggles goggles) || !goggles.showIngamePopups(head, player)) return null;
        BlockPos pos = hit.getBlockPos();
        if (!level.hasChunkAt(pos)) return null;
        boolean spaceAbove = level.isEmptyBlock(pos.above());
        Direction face = spaceAbove ? Direction.UP : hit.getDirection();
        var state = level.getBlockState(pos);
        // Modern note pitch is already a synchronized blockstate, not the old
        // TileEntityNote field. Never revive original PacketNote's unbounded request.
        if (state.getBlock() instanceof NoteBlock) {
            return new TargetView(pos, face, spaceAbove, List.of(), state.getValue(NoteBlock.NOTE));
        }
        if (!(level.getBlockEntity(pos) instanceof IAspectContainer container)) return null;
        var aspects = container.getAspects();
        if (aspects == null || aspects.size() == 0) return null;
        var knowledge = KnowledgeAccess.get(player);
        List<AspectView> entries = new ArrayList<>(aspects.size());
        // No cull, primal reduction, scan requirement, or known-aspect filter.
        // In particular a node contributes CURRENT vis, never its base capacity.
        for (var aspect : aspects.getAspects()) {
            boolean known = knowledge.hasDiscoveredAspect(aspect.getTag());
            entries.add(new AspectView(aspect.getTag(), aspects.getAmount(aspect), known,
                    known ? aspect.getImage() : UNKNOWN_ASPECT, aspect.getColor()));
        }
        return new TargetView(pos, face, spaceAbove, entries, -1);
    }

    /**
     * Call once at Fabric WorldRenderEvents.LAST, after opaque AND translucent
     * world geometry. Pass context.matrixStack(), Minecraft.renderBuffers().bufferSource(),
     * context.tickCounter().getGameTimeDeltaPartialTick(false), context.camera().
     * The pose contains the world view rotation; this method subtracts camera
     * position exactly once. Do NOT translate it by -camera in the loader too.
     * LAST has no supported context.consumers(); the explicit native BufferSource
     * is flushed here, so neither full nodes nor their font batches remain queued
     * for an earlier world pass or get overdrawn by opaque block entities.
     */
    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick, Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || pose == null) return;
        buffers.endBatch();
        renderRevealedNodes(minecraft, pose, buffers, partialTick, camera);
        buffers.endBatch();
        TargetView target = targetView(minecraft);
        if (target != null) {
            if (!target.entries().isEmpty()) {
                // Original growth in blockHighlight, decay once in renderLast.
                if (tagScale < .3f) tagScale += .031f - tagScale / 10;
                drawTags(minecraft, target, pose, buffers, camera);
            }
            if (target.note() >= 0) drawNote(minecraft, target, pose, buffers, camera);
        }
        tagScale = Math.max(0, tagScale - .005f);
        buffers.endBatch();
    }

    private static void renderRevealedNodes(Minecraft minecraft, PoseStack pose, MultiBufferSource buffers,
                                             float partialTick, Camera camera) {
        var player = minecraft.player;
        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        // Without either source of full sight, the ordinary faint-core BER is
        // sufficient. Do not scan81 loaded chunk maps every unaided frame.
        if (!(head.getItem() instanceof IRevealer revealer && revealer.showNodes(head, player))
                && !(player.getMainHandItem().getItem() instanceof ItemThaumometer)) return;
        var eye = camera.getPosition();
        int minX = Mth.floor(player.getX() - 64) >> 4, maxX = Mth.floor(player.getX() + 64) >> 4;
        int minZ = Mth.floor(player.getZ() - 64) >> 4, maxZ = Mth.floor(player.getZ() + 64) >> 4;
        // Read only already-loaded chunk BE maps. Do not force chunks or rely on
        // visibleSections: native terrain occlusion can hide a revealed node's BER.
        // Ordinary unaided/empty cores still belong exclusively to that BER.
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var chunk = minecraft.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (var blockEntity : chunk.getBlockEntities().values()) {
                if (!(blockEntity instanceof TileNode node) || node.isRemoved() || node.getLevel() != minecraft.level) continue;
                BlockPos pos = node.getBlockPos();
                pose.pushPose();
                pose.translate(pos.getX() - eye.x, pos.getY() - eye.y, pos.getZ() - eye.z);
                TileNodeRenderer.renderRevealed(node, minecraft, partialTick, pose, buffers);
                pose.popPose();
            }
        }
    }

    private static void drawTags(Minecraft minecraft, TargetView target, PoseStack pose,
                                  MultiBufferSource buffers, Camera camera) {
        var pos = target.position();
        var face = target.face();
        var eye = camera.getPosition();
        pose.pushPose();
        pose.translate(pos.getX() + .5 + tagScale * 2 * face.getStepX() - eye.x,
                pos.getY() + .5 + (target.spaceAbove() ? .4 : 0) + tagScale * 2 * face.getStepY() - eye.y,
                pos.getZ() + .5 + tagScale * 2 * face.getStepZ() - eye.z);
        // Native1.21 nameplates use POSITIVE X/negative Y after the camera
        // rotation. Negative X mirrors icons and back-face-culls font glyphs.
        pose.mulPose(camera.rotation());
        pose.scale(1, -1, 1);
        List<AspectView> entries = target.entries();
        for (int i = 0; i < entries.size(); i++) {
            int row = i / 5, column = i % 5, rowSize = Math.min(5, entries.size() - row * 5);
            float shift = (column - rowSize / 2f + .5f) * tagScale * tagScale * 4;
            AspectView entry = entries.get(i);
            pose.pushPose();
            pose.translate(shift, -row * tagScale * 1.05f, 0);
            pose.scale(tagScale, tagScale, tagScale);
            icon(pose, buffers.getBuffer(ICON_TYPES.computeIfAbsent(entry.image(), PopupRenderType::new)), entry.color());
            // Original unknown aspects hide identity, NOT nonnegative quantity.
            if (entry.amount() >= 0) {
                String text = Integer.toString(entry.amount());
                pose.scale(.04f, .04f, .04f);
                drawText(minecraft.font, text, 13 - minecraft.font.width(text), 6, pose, buffers);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static void drawNote(Minecraft minecraft, TargetView target, PoseStack pose,
                                  MultiBufferSource buffers, Camera camera) {
        var pos = target.position();
        var eye = camera.getPosition();
        String text = target.noteText();
        pose.pushPose();
        pose.translate(pos.getX() + .5 - eye.x, pos.getY() + 1.5 - eye.y, pos.getZ() + .5 - eye.z);
        pose.mulPose(camera.rotation());
        pose.scale(.02f, -.02f, .02f);
        drawText(minecraft.font, text, -minecraft.font.width(text) / 2f, 0, pose, buffers);
        pose.popPose();
    }

    private static void drawText(Font font, String text, float x, float y, PoseStack pose, MultiBufferSource buffers) {
        MultiBufferSource textBuffers = type -> buffers.getBuffer(TEXT_TYPES.computeIfAbsent(type, PopupRenderType::new));
        font.drawInBatch(text, x + 1, y + 1, 0xff111111, false, pose.last().pose(), textBuffers,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        font.drawInBatch(text, x, y, 0xffffffff, false, pose.last().pose(), textBuffers,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
    }

    private static void icon(PoseStack pose, VertexConsumer buffer, int color) {
        iconVertex(pose, buffer, -.5f, -.5f, 0, 0, color);
        iconVertex(pose, buffer, .5f, -.5f, 1, 0, color);
        iconVertex(pose, buffer, .5f, .5f, 1, 1, color);
        iconVertex(pose, buffer, -.5f, .5f, 0, 1, color);
    }

    private static void iconVertex(PoseStack pose, VertexConsumer buffer, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose.last(), x, y, 0).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 191)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 0, 1);
    }

    /** Buffered, color-only native states; no global GL mutation in the world callback. */
    private static final class PopupRenderType extends RenderType {
        private PopupRenderType(ResourceLocation texture) { this(new TextureStateShard(texture, false, false)); }

        private PopupRenderType(TextureStateShard texture) {
            super("thaumcraft_goggles_aspect", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, false, false, () -> beginIcon(texture), () -> endIcon(texture));
        }

        private PopupRenderType(RenderType text) {
            super("thaumcraft_goggles_text", text.format(), text.mode(), text.bufferSize(), false, text.sortOnUpload(),
                    () -> { text.setupRenderState(); beginOverlay(); },
                    () -> { endOverlay(); text.clearRenderState(); LEQUAL_DEPTH_TEST.setupRenderState(); });
        }

        private static void beginOverlay() {
            // Native NO_DEPTH_TEST itself is a no-op for GL_ALWAYS in 1.21.1.
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void endOverlay() {
            COLOR_WRITE.clearRenderState();
            NO_DEPTH_TEST.clearRenderState();
        }

        private static void beginIcon(TextureStateShard texture) {
            RENDERTYPE_EYES_SHADER.setupRenderState();
            texture.setupRenderState();
            TRANSLUCENT_TRANSPARENCY.setupRenderState();
            NO_CULL.setupRenderState();
            beginOverlay();
        }

        private static void endIcon(TextureStateShard texture) {
            endOverlay();
            NO_CULL.clearRenderState();
            TRANSLUCENT_TRANSPARENCY.clearRenderState();
            texture.clearRenderState();
            RENDERTYPE_EYES_SHADER.clearRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
        }
    }
}
