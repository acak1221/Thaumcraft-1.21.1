package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.renderers.models.gear.ModelWand;

/**
 * Original ModelWand:173-206: the depth layer and pulsing translucent shell of a focus.
 *
 * <p>The original draws a one block cube of the {@code focus_primal_depth} icon inside the focus
 * of the wand, shrunk to {@code 0.16} (a staff {@code 0.165 / 0.1765 / 0.165}) and moved down by
 * {@code 0.09} (a staff {@code 0.15}), before the translucent focus cube is drawn over it. The
 * cube is drawn in the model frame of the wand, so the context pose, the model basis correction
 * and the use pose of {@link thaumcraft.client.ThaumcraftGearModelsClient#renderWand} carry it
 * exactly like the focus cube itself.</p>
 *
 * <p>Depth icons remain separate sheets so their six/eight pixel frames do not lower the global
 * block atlas mipmap level. PNG dimensions and native animation metadata select their frames,
 * including the primal focus's repeated frames. The API hook also supports addon depth icons.</p>
 */
public final class PrimalWandFocusRenderer {
    private static final ResourceLocation DEPTH = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/focus_primal_depth.png");
    private static final Map<ResourceLocation, Animation> ANIMATIONS = new HashMap<>();
    private static final Map<ResourceLocation, Sheet> ORNAMENTS = new HashMap<>();
    private PrimalWandFocusRenderer() {}

    /** Resource reloads must drop cached dimensions and animation metadata with the textures. */
    public static void reset() { ANIMATIONS.clear(); ORNAMENTS.clear(); }

    public static void renderFocus(ModelWand model, ItemFocusBasic focus, ItemStack stack, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay, int tick, boolean staff) {
        renderFocus(model, focus, stack, pose, buffers, light, overlay, tick, staff,
                texture -> ANIMATIONS.computeIfAbsent(texture, PrimalWandFocusRenderer::loadAnimation),
                texture -> ORNAMENTS.computeIfAbsent(texture, PrimalWandFocusRenderer::loadSheet));
    }

    /** The production drawing path; the resolver keeps resource IO separate from vertex recording tests. */
    static void renderFocus(ModelWand model, ItemFocusBasic focus, ItemStack stack, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay, int tick, boolean staff,
                            Function<ResourceLocation, Animation> animations) {
        renderFocus(model, focus, stack, pose, buffers, light, overlay, tick, staff, animations,
                texture -> ORNAMENTS.computeIfAbsent(texture, PrimalWandFocusRenderer::loadSheet));
    }

    /** The production drawing path with an ornament sheet resolver as well. */
    static void renderFocus(ModelWand model, ItemFocusBasic focus, ItemStack stack, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay, int tick, boolean staff,
                            Function<ResourceLocation, Animation> animations, Function<ResourceLocation, Sheet> ornaments) {
        // Original ModelWand.render:414-635 draws the ornament first, then the depth layer, then the shell.
        ResourceLocation ornament = focus.getOrnament(stack);
        if (ornament != null) {
            Sheet sheet = ornaments.apply(ornament);
            renderOrnament(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ornament)), light, overlay,
                    sheet.animation().at(tick), sheet.width(), sheet.height());
        }
        ResourceLocation texture = focus.getFocusDepthLayerIcon(stack);
        if (texture != null) renderDepthLayer(pose, buffers, light, overlay, staff, texture, animations.apply(texture).at(tick));
        int tint = (int) ((texture == null ? .95F : .6F) * 255) << 24 | focus.getFocusColor(stack) & 0xFFFFFF;
        pose.pushPose();
        try {
            if (staff) {
                pose.translate(0, ModelWand.Geometry.STAFF_FOCUS_SHIFT, 0);
                pose.scale(ModelWand.Geometry.STAFF_FOCUS_XZ, ModelWand.Geometry.STAFF_FOCUS_Y, ModelWand.Geometry.STAFF_FOCUS_XZ);
            } else pose.scale(ModelWand.Geometry.WAND_FOCUS, ModelWand.Geometry.WAND_FOCUS, ModelWand.Geometry.WAND_FOCUS);
            // The original passes a raw lightmap coordinate (195..214), not FULL_BRIGHT. An emissive
            // shader ignores that lightmap entirely, so use the ordinary translucent entity shader.
            model.renderFocus(pose, buffers.getBuffer(RenderType.entityTranslucent(ModelWand.FOCUS_TEXTURE)),
                    Geometry.pulse(tick), overlay, tint);
        } finally { pose.popPose(); }
    }

    private static Animation loadAnimation(ResourceLocation texture) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return Animation.STATIC;
        try (var input = resource.get().open(); var image = NativeImage.read(input)) {
            return Animation.from(image.getWidth(), image.getHeight(), resource.get().metadata()
                    .getSection(AnimationMetadataSection.SERIALIZER).orElse(null));
        } catch (IOException | RuntimeException invalidTexture) {
            // The native texture manager supplies its missing texture; never break the entire wand
            // render because a resource pack supplied a missing or malformed depth sheet/metadata.
            return Animation.STATIC;
        }
    }

    /** An ornament sheet: its animation and the pixel size of one frame (the original icon width/height). */
    record Sheet(Animation animation, int width, int height) {
        static final Sheet MISSING = new Sheet(Animation.STATIC, 16, 16);
    }

    private static Sheet loadSheet(ResourceLocation texture) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return Sheet.MISSING;
        try (var input = resource.get().open(); var image = NativeImage.read(input)) {
            var metadata = resource.get().metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
            Animation animation = Animation.from(image.getWidth(), image.getHeight(), metadata);
            int width = image.getWidth(), height = image.getHeight();
            if (animation != Animation.STATIC) { width /= animation.columns; height /= animation.rows; }
            return new Sheet(animation, Math.max(1, width), Math.max(1, height));
        } catch (IOException | RuntimeException invalidTexture) {
            return Sheet.MISSING;
        }
    }

    /**
     * Original ModelWand.render:422-632: the ornament of the focus ({@code ItemFocusBasic.getOrnament}), an extruded item
     * icon drawn twice, the second turned ninety degrees about Y, inside the turn of a hundred and eighty degrees about X.
     */
    static void renderOrnament(PoseStack pose, VertexConsumer out, int light, int overlay, UV uv, int width, int height) {
        pose.pushPose();
        try {
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            for (int pass = 0; pass < 2; pass++) {
                pose.pushPose();
                try {
                    if (pass == 1) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                    pose.translate(ModelWand.Geometry.ORNAMENT_SHIFT_X, ModelWand.Geometry.ORNAMENT_SHIFT_Y, ModelWand.Geometry.ORNAMENT_SHIFT_Z);
                    pose.scale(ModelWand.Geometry.ORNAMENT_SCALE, ModelWand.Geometry.ORNAMENT_SCALE, ModelWand.Geometry.ORNAMENT_SCALE);
                    renderItemIn2D(pose, out, light, overlay, uv.u1(), uv.v0(), uv.u0(), uv.v1(), width, height, ModelWand.Geometry.ORNAMENT_THICKNESS);
                } finally { pose.popPose(); }
            }
        } finally { pose.popPose(); }
    }

    /**
     * 1.7.10 {@code ItemRenderer.renderItemIn2D(tess, maxU, minV, minU, maxV, width, height, thickness)}: the front and back
     * faces of a unit square and one edge strip per pixel column and row, so an icon has depth.
     */
    private static void renderItemIn2D(PoseStack pose, VertexConsumer out, int light, int overlay,
                                       float u1, float v1, float u0, float v0, int width, int height, float thickness) {
        float back = -thickness;
        quad(pose, out, light, overlay, 0, 0, 1, new float[][]{{0, 0, 0, u1, v0}, {1, 0, 0, u0, v0}, {1, 1, 0, u0, v1}, {0, 1, 0, u1, v1}});
        quad(pose, out, light, overlay, 0, 0, -1, new float[][]{{0, 1, back, u1, v1}, {1, 1, back, u0, v1}, {1, 0, back, u0, v0}, {0, 0, back, u1, v0}});
        float halfU = .5F * (u1 - u0) / width, halfV = .5F * (v0 - v1) / height;
        for (int k = 0; k < width; k++) {
            float x = (float) k / width, u = u1 + (u0 - u1) * x - halfU;
            quad(pose, out, light, overlay, -1, 0, 0, new float[][]{{x, 0, back, u, v0}, {x, 0, 0, u, v0}, {x, 1, 0, u, v1}, {x, 1, back, u, v1}});
        }
        for (int k = 0; k < width; k++) {
            float x = (float) k / width, u = u1 + (u0 - u1) * x - halfU, next = x + 1.0F / width;
            quad(pose, out, light, overlay, 1, 0, 0, new float[][]{{next, 1, back, u, v1}, {next, 1, 0, u, v1}, {next, 0, 0, u, v0}, {next, 0, back, u, v0}});
        }
        for (int k = 0; k < height; k++) {
            float y = (float) k / height, v = v0 + (v1 - v0) * y - halfV, next = y + 1.0F / height;
            quad(pose, out, light, overlay, 0, 1, 0, new float[][]{{0, next, 0, u1, v}, {1, next, 0, u0, v}, {1, next, back, u0, v}, {0, next, back, u1, v}});
        }
        for (int k = 0; k < height; k++) {
            float y = (float) k / height, v = v0 + (v1 - v0) * y - halfV;
            quad(pose, out, light, overlay, 0, -1, 0, new float[][]{{1, y, 0, u0, v}, {0, y, 0, u1, v}, {0, y, back, u1, v}, {1, y, back, u0, v}});
        }
    }

    private static void quad(PoseStack pose, VertexConsumer out, int light, int overlay, float nx, float ny, float nz, float[][] vertices) {
        for (float[] vertex : vertices) {
            out.addVertex(pose.last(), vertex[0], vertex[1], vertex[2]).setColor(0xffffffff).setUv(vertex[3], vertex[4])
                    .setOverlay(overlay).setLight(light).setNormal(pose.last(), nx, ny, nz);
        }
    }

    /**
     * Original ModelWand:173-181: the depth layer, in the model frame of the wand.
     *
     * @param staff whether the wand is a staff, which uses the larger and lower layer
     * @param tick  the tick of the world, which selects the frame of the strip and the pulse
     */
    public static void renderDepthLayer(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int tick, boolean staff) {
        renderDepthLayer(pose, buffers, light, overlay, staff, DEPTH,
                ANIMATIONS.computeIfAbsent(DEPTH, PrimalWandFocusRenderer::loadAnimation).at(tick));
    }

    private static void renderDepthLayer(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                         boolean staff, ResourceLocation texture, UV uv) {
        Geometry.Depth depth = Geometry.depth(staff);
        pose.pushPose();
        try {
            pose.translate(0, depth.translationY(), 0);
            pose.scale((float) depth.x(), (float) depth.y(), (float) depth.z());
            renderDepth(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, uv);
        } finally { pose.popPose(); }
    }

    private static void renderDepth(PoseStack pose, VertexConsumer out, int light, int overlay, UV uv) {
        float h = .5F;
        face(pose,out,light,overlay,uv,0,-1,0, new float[][]{{-h,-h,-h},{h,-h,-h},{h,-h,h},{-h,-h,h}});
        face(pose,out,light,overlay,uv,0,1,0, new float[][]{{-h,h,h},{h,h,h},{h,h,-h},{-h,h,-h}});
        face(pose,out,light,overlay,uv,0,0,-1, new float[][]{{h,-h,-h},{-h,-h,-h},{-h,h,-h},{h,h,-h}});
        face(pose,out,light,overlay,uv,0,0,1, new float[][]{{-h,-h,h},{h,-h,h},{h,h,h},{-h,h,h}});
        face(pose,out,light,overlay,uv,-1,0,0, new float[][]{{-h,-h,-h},{-h,-h,h},{-h,h,h},{-h,h,-h}});
        face(pose,out,light,overlay,uv,1,0,0, new float[][]{{h,-h,h},{h,-h,-h},{h,h,-h},{h,h,h}});
    }
    private static void face(PoseStack pose, VertexConsumer out, int light, int overlay, UV uv,
                             float nx, float ny, float nz, float[][] points) {
        for (int index = 0; index < 4; index++) {
            float[] point = points[index];
            out.addVertex(pose.last(),point[0],point[1],point[2]).setColor(0xffffffff)
                    .setUv(index == 1 || index == 2 ? uv.u1() : uv.u0(), index < 2 ? uv.v1() : uv.v0())
                    .setOverlay(overlay).setLight(light).setNormal(pose.last(),nx,ny,nz);
        }
    }

    record UV(float u0, float v0, float u1, float v1) {}
    static final class Animation {
        static final Animation STATIC = new Animation(1, 1, List.of(new TimedFrame(0, 1)));
        private final int columns, rows;
        private final List<TimedFrame> frames;
        private final long duration;
        private record TimedFrame(int index, int time) {}
        private Animation(int columns, int rows, List<TimedFrame> frames) {
            this.columns = columns;
            this.rows = rows;
            this.frames = List.copyOf(frames);
            this.duration = frames.stream().mapToLong(TimedFrame::time).sum();
        }
        static Animation from(int width, int height, AnimationMetadataSection metadata) {
            if (metadata == null) return STATIC;
            var size = metadata.calculateFrameSize(width, height);
            if (size.width() <= 0 || size.height() <= 0 || width % size.width() != 0 || height % size.height() != 0) return STATIC;
            int columns = width / size.width(), rows = height / size.height();
            var frames = new ArrayList<TimedFrame>();
            metadata.forEachFrame((index, time) -> {
                if (index >= 0 && index < columns * rows && time > 0) frames.add(new TimedFrame(index, time));
            });
            if (frames.isEmpty()) for (int index = 0; index < columns * rows; index++) {
                frames.add(new TimedFrame(index, Math.max(1, metadata.getDefaultFrameTime())));
            }
            return new Animation(columns, rows, frames);
        }
        UV at(int tick) {
            long elapsed = Math.floorMod((long) tick, duration);
            for (var frame : frames) {
                if (elapsed < frame.time()) {
                    int x = frame.index() % columns, y = frame.index() / columns;
                    return new UV((float) x / columns, (float) y / rows, (float) (x + 1) / columns, (float) (y + 1) / rows);
                }
                elapsed -= frame.time();
            }
            throw new AssertionError("animation cycle has no frame");
        }
    }

    /** Independent data/math nested class can be tested without initializing any client renderer or GL state. */
    public static final class Geometry {
        private static final int[] FRAMES = {0,1,1,2,3,4,5,6,6,7,8,9,10,11,11,12,13,14,15,16,16,17,18,19,20,21,21,22,23,24,25,26,26,27,28,29};
        public record Depth(double x, double y, double z, double translationY) {}
        private Geometry() {}
        /** Original ModelWand:177-179 and :183-185: the scale and the shift of the depth layer. */
        public static Depth depth(boolean staff) { return staff ? new Depth(.165D,.1765D,.165D,-.15D) : new Depth(.16D,.16D,.16D,-.09D); }
        public static int frame(int tick) { return FRAMES[Math.floorMod(tick,FRAMES.length)]; }
        public static int pulse(int tick) { return (int)(195F + Mth.sin(tick / 3F) * 10F + 10F); }
        public static int[] frames() { return FRAMES.clone(); }
    }
}
