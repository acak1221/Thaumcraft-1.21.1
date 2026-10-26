package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.gui.GuiGolem;
import thaumcraft.client.gui.GuiTravelingTrunk;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.ItemGolemBell;
import thaumcraft.common.items.golems.ItemGolemPlacer;

/** Client-only bindings; root owns common registration and the bootstrap call. */
public final class ThaumcraftGolemClient {
    public static final ResourceLocation HAS_CORE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "has_core");
    private static boolean initialized;
    private ThaumcraftGolemClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        MenuRegistry.registerScreenFactory(ThaumcraftGolems.GOLEM_MENU.get(), GuiGolem::new);
        MenuRegistry.registerScreenFactory(ThaumcraftGolems.TRUNK_MENU.get(), GuiTravelingTrunk::new);
        for (var placer : ThaumcraftGolems.placerItems()) ItemPropertiesRegistry.register(placer, HAS_CORE,
                (stack, level, entity, seed) -> stack.getOrDefault(ThaumcraftGolems.GOLEM_DATA.get(), GolemData.EMPTY).core() >= 0 ? 1F : 0F);
    }

    /**
     * The golem marks the original draws while the player holds a golemancer's bell or a golem placer
     * with markers (oracle RenderEventHandler.renderLast and renderMarkedBlocks:0-644).
     *
     * <p>With a bell, nothing is drawn unless its golem is loaded on this client; then the home of the
     * golem gets the rainbow {@code home.png} mark, every marker within sixty four blocks the
     * {@code mark.png} on the clicked face (rainbow without a colour, else the colour of the Order
     * upgrade), a marked air block a dashed outline on all six faces, and with a golem link quality above
     * three a wobbling {@code script.png} ribbon runs from the head of the golem to every marker. Root
     * calls this at Fabric LAST (NeoForge AFTER_LEVEL) with the world-view pose; every quad is drawn at
     * once with the original state: additive, no depth writes, no culling, depth tested, alpha above
     * 1/255. Not reproduced: the lightmap brightness 200 of the original tessellator (full brightness).</p>
     */
    public static void renderMarkers(PoseStack pose, MultiBufferSource buffers, float partialTick, Camera camera) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !player.isAlive() || pose == null || buffers == null || camera == null) return;
        // The main hand wins, as the original's current item; the off hand is the 1.21.1 fallback.
        ItemStack held = markerHolder(player.getMainHandItem());
        if (held == null) held = markerHolder(player.getOffhandItem());
        if (held == null) return;
        EntityGolemBase golem = null;
        BlockPos home = null;
        Direction homeFace = null;
        List<GolemMarker> markers;
        if (held.getItem() instanceof ItemGolemBell) {
            var selected = ItemGolemBell.selectedGolem(held);
            if (selected == null) return;
            golem = loadedGolem(level, selected.golem());
            if (golem == null) return;
            home = selected.home();
            homeFace = selected.face();
            markers = selected.markers();
        } else {
            markers = held.getOrDefault(ThaumcraftGolems.GOLEM_DATA.get(), GolemData.EMPTY).markers();
            if (markers.isEmpty()) return;
        }
        var eye = camera.getPosition();
        float time = (float) (System.nanoTime() / 30000000L);
        if (golem != null && home != null && eye.distanceToSqr(home.getX(), home.getY(), home.getZ()) < 4096) {
            float[] rainbow = rainbow(time, homeFace.get3DDataValue());
            drawMark(pose, eye, home.getX(), home.getY(), home.getZ(), homeFace, .01F, .65F, HOME, rainbow);
        }
        var dimension = level.dimension().location();
        for (GolemMarker marker : markers) {
            if (!dimension.equals(marker.dimension())) continue;
            BlockPos pos = marker.pos();
            Direction side = marker.face();
            double x = pos.getX() + side.getStepX(), y = pos.getY() + side.getStepY(), z = pos.getZ() + side.getStepZ();
            if (eye.distanceToSqr(x, y, z) >= 4096) continue;
            int color = marker.color();
            drawMark(pose, eye, x, y, z, side, .01F, .4F, MARK, color == GolemMarker.ANY ? rainbow(time, side.get3DDataValue()) : rgb(color));
            // hasChunkAt keeps a marker in an unloaded chunk from reading as air.
            if (level.hasChunkAt(pos) && level.isEmptyBlock(pos)) {
                for (Direction face : Direction.values()) {
                    drawMark(pose, eye, pos.getX() + face.getStepX(), pos.getY() + face.getStepY(), pos.getZ() + face.getStepZ(),
                            face, -.01F, .98F, EMPTY, color == GolemMarker.ANY ? rainbow(time, face.get3DDataValue()) : rgb(color));
                }
            }
            if (golem != null && Config.golemLinkQuality > 3) {
                drawLink(pose, eye, x - side.getStepX() * .5D, y - side.getStepY() * .5D, z - side.getStepZ() * .5D,
                        side, partialTick, color, golem, time);
            }
        }
    }

    private static final ResourceLocation HOME = texture("misc/home"), MARK = texture("misc/mark"), EMPTY = texture("blocks/empty"),
            SCRIPT = texture("misc/script");

    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path + ".png"); }

    private static ItemStack markerHolder(ItemStack stack) {
        return stack.getItem() instanceof ItemGolemBell || stack.getItem() instanceof ItemGolemPlacer ? stack : null;
    }

    /** The bell's golem, only when this client has it loaded (the original getEntityByID). */
    private static EntityGolemBase loadedGolem(net.minecraft.client.multiplayer.ClientLevel level, java.util.UUID id) {
        for (var entity : level.entitiesForRendering())
            if (entity instanceof EntityGolemBase golem && golem.getUUID().equals(id) && golem.isAlive()) return golem;
        return null;
    }

    /** The rainbow of the original: sin(t % 32767 / 12, 14, 16 + phase) * 0.2 + 0.8. */
    private static float[] rainbow(float time, float phase) {
        float cycle = time % 32767.0F;
        return new float[] {Mth.sin(cycle / 12.0F + phase) * .2F + .8F, Mth.sin(cycle / 14.0F + phase) * .2F + .8F,
                Mth.sin(cycle / 16.0F + phase) * .2F + .8F};
    }

    private static float[] rgb(int index) {
        int color = UtilsFX.color(index);
        return new float[] {(color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F};
    }

    /**
     * Original drawGolemHomeOverlay, drawMarkerOverlay and drawAirBlockoverlay: the centre of the cell
     * {@code x, y, z} nudged by {@code outward} along the face, turned onto the face plane and moved
     * half a block back to it, then UtilsFX.renderQuadCenteredFromTexture at the given scale.
     */
    private static void drawMark(PoseStack pose, Vec3 eye, double x, double y, double z, Direction side, float outward, float scale,
                                 ResourceLocation texture, float[] color) {
        pose.pushPose();
        try {
            pose.translate(x - eye.x + .5D + side.getStepX() * outward, y - eye.y + .5D + side.getStepY() * outward,
                    z - eye.z + .5D + side.getStepZ() * outward);
            pose.mulPose(new Quaternionf().rotationAxis(Mth.HALF_PI, -side.getStepY(), side.getStepX(), -side.getStepZ()));
            pose.translate(0, 0, side.getStepZ() < 0 ? .5D : -.5D);
            pose.scale(scale, scale, scale);
            var last = pose.last();
            VisNetRender.draw(texture, GL11.GL_ONE, false, VisNetRender.lowAlphaShader(), out -> {
                out.addVertex(last, -.5F, .5F, 0).setUv(0, 1).setColor(color[0], color[1], color[2], 1);
                out.addVertex(last, .5F, .5F, 0).setUv(1, 1).setColor(color[0], color[1], color[2], 1);
                out.addVertex(last, .5F, -.5F, 0).setUv(1, 0).setColor(color[0], color[1], color[2], 1);
                out.addVertex(last, -.5F, -.5F, 0).setUv(0, 0).setColor(color[0], color[1], color[2], 1);
            });
        } finally { pose.popPose(); }
    }

    /**
     * Original drawMarkerLine: a ribbon of {@code script.png} from the head of the golem, sampled
     * {@code round(distance) * quality} times, wobbling towards the cell in front of the marked face and
     * blending over the last {@code quality / 2} samples onto the centre of that face, where it fades out.
     */
    private static void drawLink(PoseStack pose, Vec3 eye, double x, double y, double z, Direction side, float partialTick, int color,
                                 EntityGolemBase golem, float time) {
        int quality = Config.golemLinkQuality;
        double ex = Mth.lerp(partialTick, golem.xOld, golem.getX());
        double ey = Mth.lerp(partialTick, golem.yOld, golem.getY());
        double ez = Mth.lerp(partialTick, golem.zOld, golem.getZ());
        double headY = ey + golem.getBbHeight();
        float[] fixed = color > GolemMarker.ANY ? rgb(color) : new float[] {1, 1, 1};
        double farX = (float) (x + .5D + side.getStepX() * .5D - ex), farY = (float) (y + .5D + side.getStepY() * .5D - headY),
                farZ = (float) (z + .5D + side.getStepZ() * .5D - ez);
        double nearX = (float) (x + .5D - ex), nearY = (float) (y + .5D - headY), nearZ = (float) (z + .5D - ez);
        double dX = x - ex, dY = y - ey, dZ = z - ez;
        float distance = (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        float segments = Math.round(distance) * (float) quality;
        // Original: zero samples divide zero by zero and draw nothing visible.
        if (segments <= 0) return;
        float cycle = time % 32767.0F;
        int phase = side.get3DDataValue();
        pose.pushPose();
        try {
            pose.translate(ex - eye.x, headY - eye.y, ez - eye.z);
            var last = pose.last();
            VisNetRender.drawStrip(SCRIPT, GL11.GL_ONE, false, VisNetRender.lowAlphaShader(), out -> {
                for (int i = 0; i <= segments; i++) {
                    float f = i / segments;
                    float alpha = Math.min(.75F, i * 1.5F / segments);
                    float amp = 1.0F - Math.abs(i - segments / 2.0F) / (segments / 2.0F);
                    float blend = 0.0F;
                    float[] rgb = color == GolemMarker.ANY ? new float[] {Mth.sin(cycle / 12.0F + phase + i) * .2F + .8F,
                            Mth.sin(cycle / 14.0F + phase + i) * .2F + .8F, Mth.sin(cycle / 16.0F + phase + i) * .2F + .8F} : fixed;
                    double wave = distance * (1.0F - f) * (float) quality;
                    double shift = (cycle / 5.0F);
                    float wx = Mth.sin((float) ((phase * 20 + z % 16.0D + wave - shift) / 4.0D)) * .5F;
                    float wy = Mth.sin((float) ((phase * 20 + x % 16.0D + wave - shift) / 3.0D)) * .5F;
                    float wz = Mth.sin((float) ((phase * 20 + y % 16.0D + wave - shift) / 2.0D)) * .5F;
                    double px = farX + wx * amp, py = farY + wy * amp, pz = farZ + wz * amp;
                    if (i > segments - (float) (quality / 2)) {
                        double qx = nearX + wx * amp, qy = nearY + wy * amp, qz = nearZ + wz * amp;
                        amp = (segments - i) / (quality / 2.0F);
                        blend = 1.0F - amp;
                        px = px * amp + qx * blend;
                        py = py * amp + qy * blend;
                        pz = pz * amp + qz * blend;
                    }
                    float u = (1.0F - f) * distance - time * .005F;
                    float a = alpha * (1.0F - blend);
                    out.addVertex(last, (float) (px * f), (float) (py * f - .05D), (float) (pz * f)).setUv(u, 1).setColor(rgb[0], rgb[1], rgb[2], a);
                    out.addVertex(last, (float) (px * f), (float) (py * f + .05D), (float) (pz * f)).setUv(u, 0).setColor(rgb[0], rgb[1], rgb[2], a);
                }
            });
        } finally { pose.popPose(); }
    }
}
