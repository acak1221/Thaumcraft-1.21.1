package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.common.lib.network.ExcavationFxNetworking;
import thaumcraft.common.lib.network.ExcavationFxPayload;

/**
 * Server-eligible continuous green0x00FF66 beam, impact5/end-size2. The beam of the original
 * FXBeamWand:37-297 is a fan of three planes that turns around its own axis, with a width that
 * pulses over four ticks and an opacity of four tenths that fades to a tenth in the last four
 * ticks of its life. Independently written native camera-facing additive mesh: three tapered
 * ribbons/five glints, at most32 casters. This is an explicit bounded approximation, NOT legacy
 * particle/audio parity. No beam entity or client mining.
 * Original FXBeamWand:84,95-101 gives initial8/refresh-at-least4 tick lifetime.
 */
public final class ThaumcraftExcavationClient {
    private static final int MAX_BEAMS = 32;
    /** The number of planes of the fan of the original ({@code FXBeamWand:240}). */
    public static final int PLANES = 3;
    /** The angle between two planes of the fan of the original ({@code FXBeamWand:246}). */
    public static final float PLANE_STEP = 60.0F;
    /** The speed of the fan of the original ({@code FXBeamWand:54,171}). */
    public static final int ROTATION_SPEED = 5;
    /** The opacity of the beam of the original ({@code FXBeamWand:177}). */
    public static final float BEAM_OPACITY = 0.4F;
    /** The half width of a plane of the original ({@code FXBeamWand:235-238}). */
    public static final float PLANE_HALF_WIDTH = 0.15F;
    /** The four beam textures of the original beam (type 0 to 3: beam, beam1, beam2, beam3); the excavation focus draws type 2. */
    private static final RenderType[] BEAM_TYPES = {new BeamRenderType("beam"), new BeamRenderType("beam1"),
            new BeamRenderType("beam2"), new BeamRenderType("beam3")};
    /** The texture type of the excavation beam. */
    private static final int EXCAVATION_TYPE = 2;
    /** The particle atlas of the original, the sheet of the impact of the beam. */
    private static final RenderType GLINT = new BeamRenderType("particles");
    /** The alpha of the impact of the original ({@code FXBeamWand:275,288}). */
    public static final float IMPACT_ALPHA = 0.66F;
    private static final Map<Integer, Beam> BEAMS = new LinkedHashMap<>();
    /**
     * The beams add-ons ask for through the client effect facade, by the id of the entity that owns them. They are drawn by the
     * same pass as the excavation beams, are never listed by {@link #beams()} (that is the state the server sent) and are dropped
     * by the same rules.
     */
    private static final Map<Integer, Beam> FACADE_BEAMS = new LinkedHashMap<>();
    private static boolean initialized;
    private ThaumcraftExcavationClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ExcavationFxNetworking.initClient(ThaumcraftExcavationClient::accept);
        ClientTickEvent.CLIENT_POST.register(ThaumcraftExcavationClient::tick);
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> clear());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clear());
    }
    /** The ticks a beam lasts from its first appearance (original FXBeamWand:84, the age of the particle). */
    public static final int BEAM_LIFETIME = 8;
    /** The fewest ticks a beam has left after it was refreshed (original FXBeamWand:95-101). */
    public static final int BEAM_REFRESH = 4;
    /** The ticks a beam has left after a refresh: the larger of what is left and {@link #BEAM_REFRESH}. */
    public static int refreshedLifetime(int remaining) { return Math.max(remaining, BEAM_REFRESH); }
    public static void clear() { BEAMS.clear(); FACADE_BEAMS.clear(); }
    /** The number of beams drawn for the client effect facade, for the diagnostics of the client. */
    public static int facadeBeamCount() { return FACADE_BEAMS.size(); }

    /**
     * The continuous beam of the client effect facade ({@code ThaumcraftClientFX.beam}), the counterpart of the original
     * {@code Thaumcraft.proxy.beamCont}: a beam that lives eight ticks and is kept alive, for at least four more, by every call
     * for the same owner, the way the original updated the beam it had returned. The caller has validated the arguments.
     *
     * @param owner    the entity the beam belongs to; the beam ends when it is removed or farther than 64 blocks from the player
     * @param texture  0 to 3, the original beam type
     * @param reverse  whether the texture slides the other way
     * @param endSize  the scale of the far end of the beam, as the original {@code endmod}
     * @param impact   the ticks the glint at the far end still shows, as the original {@code impact}
     */
    public static void addFacadeBeam(net.minecraft.world.entity.Entity owner, Vec3 from, Vec3 to, int rgb, int texture,
                                     boolean reverse, float endSize, int impact) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || owner.level() != client.level) return;
        Beam beam = FACADE_BEAMS.get(owner.getId());
        if (beam == null) {
            if (FACADE_BEAMS.size() == MAX_BEAMS) FACADE_BEAMS.remove(FACADE_BEAMS.keySet().iterator().next());
            FACADE_BEAMS.put(owner.getId(), new Beam(client.level.dimension().location(), from, to, rgb, texture, reverse, endSize, impact));
        } else beam.refresh(from, to, rgb, texture, reverse, endSize, impact);
    }
    public static int activeBeamCount() { return BEAMS.size(); }
    /** Read-only actual S2C visual state, not an injection/test-control API. */
    public static List<BeamView> beams() {
        return BEAMS.entrySet().stream().map(entry -> {
            Beam beam = entry.getValue();
            return new BeamView(beam.dimension, entry.getKey(), beam.origin, beam.target, beam.impact,
                    beam.endSize, beam.crackStage, beam.completed, beam.success, beam.remaining);
        }).toList();
    }
    public record BeamView(ResourceLocation dimension, int casterId, Vec3 origin, Vec3 target, int impact,
            float endSize, int crackStage, boolean completed, boolean success, int remainingTicks) {}

    private static void accept(ExcavationFxPayload payload) {
        var client = Minecraft.getInstance(); var level = client.level;
        if (level == null || client.player == null || !level.dimension().location().equals(payload.dimension())) return;
        var caster = level.getEntity(payload.casterId());
        if (caster == null || caster.isRemoved() || caster.distanceToSqr(payload.origin()) > 3 * 3) return;
        Beam beam = BEAMS.get(payload.casterId());
        if (beam == null) {
            if (BEAMS.size() == MAX_BEAMS) BEAMS.remove(BEAMS.keySet().iterator().next());
            BEAMS.put(payload.casterId(), new Beam(payload));
        } else beam.refresh(payload);
        // Oracle ClientProxy:806-809 only forwards positive same-coordinate
        // progress. Miss/release/new-target/cancel do NOT explicitly clear it.
        // Keep native stale-crack expiry, and progress/hardness*9 (not*10).
        if (payload.crackStage() >= 0) client.levelRenderer.destroyBlockProgress(payload.casterId(), payload.blockPos(), payload.crackStage());
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) { clear(); return; }
        BEAMS.entrySet().removeIf(entry -> step(client, entry));
        FACADE_BEAMS.entrySet().removeIf(entry -> step(client, entry));
    }

    /** One client tick of a beam; true when the beam is over. */
    private static boolean step(Minecraft client, Map.Entry<Integer, Beam> entry) {
        Beam beam = entry.getValue();
        beam.previousSize = Math.min(beam.age / 4.0F, 1.0F);
        ++beam.age;
        if (beam.impact > 0) --beam.impact;
        var caster = client.level.getEntity(entry.getKey());
        return --beam.remaining <= 0 || !client.level.dimension().location().equals(beam.dimension)
                || caster == null || caster.isRemoved() || client.player.distanceToSqr(beam.origin) > 64 * 64;
    }

    private static final class Beam {
        private final ResourceLocation dimension;
        private Vec3 previousOrigin, origin, previousTarget, target;
        private int age, remaining = BEAM_LIFETIME, impact, crackStage;
        private float endSize;
        private float previousSize;
        private boolean completed, success;
        /** The colour of the beam (the excavation green 0x00FF66 unless the facade asked for another), its texture and direction. */
        private float red = 0.0F, green = 1.0F, blue = 0.4F;
        private int texture = EXCAVATION_TYPE;
        private boolean reverse;
        private Beam(ExcavationFxPayload payload) {
            dimension = payload.dimension(); origin = payload.origin(); target = payload.target();
            refresh(payload); remaining = BEAM_LIFETIME;
        }
        private Beam(ResourceLocation dimension, Vec3 from, Vec3 to, int rgb, int texture, boolean reverse, float endSize, int impact) {
            this.dimension = dimension; origin = from; target = to;
            refresh(from, to, rgb, texture, reverse, endSize, impact); remaining = BEAM_LIFETIME;
        }
        private void refresh(Vec3 from, Vec3 to, int rgb, int texture, boolean reverse, float endSize, int impact) {
            previousOrigin = origin; previousTarget = target; origin = from; target = to;
            remaining = refreshedLifetime(remaining); this.impact = impact; this.endSize = endSize;
            red = (rgb >> 16 & 255) / 255.0F; green = (rgb >> 8 & 255) / 255.0F; blue = (rgb & 255) / 255.0F;
            this.texture = texture; this.reverse = reverse;
        }
        private void refresh(ExcavationFxPayload payload) {
            previousOrigin = origin; previousTarget = target; origin = payload.origin(); target = payload.target();
            remaining = refreshedLifetime(remaining); impact = payload.impact() ? 5 : 0; endSize = payload.impact() ? 2 : 0;
            crackStage = payload.crackStage(); completed = payload.completed(); success = payload.success();
        }
        /**
         * The width of the beam of the original ({@code FXBeamWand:173-176}).
         *
         * <p>The original pulses the width of the beam over four ticks and interpolates it with the
         * width of the previous frame.</p>
         *
         * @param partial the partial tick of the frame
         * @return the width of the beam
         */
        private float size(float partial) {
            float target = Math.min(age / 4.0F, 1.0F);
            return previousSize + (target - previousSize) * partial;
        }
        /**
         * The opacity of the beam of the original ({@code FXBeamWand:177-180}).
         *
         * <p>The original keeps the opacity of the beam at four tenths and fades it to a tenth in
         * the last four ticks of its life.</p>
         *
         * @return the opacity of the beam
         */
        private float opacity() {
            if (remaining > 4) return BEAM_OPACITY;
            return BEAM_OPACITY - (4 - remaining) * 0.1F;
        }
    }

    /** Fabric LAST: SAME untranslated world pose/buffers/partial/camera as Shock.
     * Camera subtraction happens ONCE here, never in the loader hook. */
    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick, Camera camera) {
        var client = Minecraft.getInstance();
        if (pose == null || client.level == null || client.player == null || BEAMS.isEmpty() && FACADE_BEAMS.isEmpty()) return;
        float partial = Mth.clamp(partialTick, 0, 1);
        long gameTime = client.level.getGameTime();
        int slide = client.player.tickCount;
        boolean[] used = new boolean[BEAM_TYPES.length];
        for (Beam beam : BEAMS.values()) used[beam.texture] = true;
        for (Beam beam : FACADE_BEAMS.values()) used[beam.texture] = true;
        for (int pass = 0; pass < 2; pass++) {
            // Pass 0 draws the beams, one render type per texture in use; pass 1 draws the glints at their far ends.
            for (int texture = 0; texture < (pass == 0 ? BEAM_TYPES.length : 1); texture++) {
                if (pass == 0 && !used[texture]) continue;
                RenderType type = pass == 0 ? BEAM_TYPES[texture] : GLINT;
                VertexConsumer vertices = buffers.getBuffer(type);
                for (Map<Integer, Beam> beams : List.of(BEAMS, FACADE_BEAMS)) {
                    for (Beam beam : beams.values()) {
                        if (pass == 0 && beam.texture != texture) continue;
                        if (!client.level.dimension().location().equals(beam.dimension)) continue;
                        Vec3 origin = beam.previousOrigin.lerp(beam.origin, partial), target = beam.previousTarget.lerp(beam.target, partial);
                        Vec3 end = target.subtract(origin), eye = camera.getPosition().subtract(origin);
                        // Original FXBeamWand:173-176 - the width of the beam pulses over four ticks.
                        float size = beam.size(partial);
                        // Original FXBeamWand:177-180 - the opacity of the beam and its fade in the last
                        // four ticks of its life.
                        float opacity = beam.opacity();
                        pose.pushPose();
                        try {
                            pose.translate(-eye.x, -eye.y, -eye.z);
                            if (pass == 0) {
                                // Original FXBeamWand:229-255 - the beam is a fan of three planes that
                                // turns around its own axis.
                                drawBeam(pose, vertices, end, length(end), size, opacity, beam.endSize,
                                        gameTime, partial, slide, beam);
                            } else if (beam.impact > 0) {
                                // Original FXBeamWand:261-263 and :269-297 - the impact of the beam.
                                impact(pose, vertices, camera, end, beam);
                            }
                        } finally { pose.popPose(); }
                    }
                }
                buffers.endBatch(type);
            }
        }
    }

    /**
     * The three planes of the beam of the original.
     *
     * <p>Original {@code FXBeamWand:229-255}: the fan turns around the axis of the beam with the
     * rotation of the world time and every plane is turned by another sixty degrees. The ribbon of
     * a plane is a quarter of a block wide, it is as long as the beam and its far end is scaled by
     * the end size of the beam.</p>
     */
    private static void drawBeam(PoseStack pose, VertexConsumer vertices, Vec3 end, float length,
                                 float size, float opacity, float endSize, long gameTime, float partial, int slide, Beam beam) {
        if (length < 1.0E-4F) return;
        Vec3 direction = end.normalize();
        pose.pushPose();
        try {
            // Original FXBeamWand:230-234 - the frame of the beam: the local Y is the direction
            // of the beam, the local X and Z are the two sides of the fan.
            pose.mulPose(Axis.YP.rotationDegrees(yaw(direction)));
            // Local +Y is the beam length, not +Z. Elevation is measured
            // above the XZ plane, so tilt +Y by its complementary angle.
            pose.mulPose(Axis.XP.rotationDegrees(90.0F - elevation(direction)));
            // Original FXBeamWand:239 - the fan turns around the axis of the beam.
            pose.mulPose(Axis.YP.rotationDegrees(spin(gameTime, partial)));
            for (int plane = 0; plane < PLANES; plane++) {
                // Original FXBeamWand:246 - every plane is turned by another sixty degrees.
                pose.mulPose(Axis.YP.rotationDegrees(PLANE_STEP));
                // Original FXBeamWand:201-206 and :235-238 - the texture of the beam slides.
                float base = -1.0F + scroll(slide, partial, beam.reverse) + plane / 3.0F;
                float top = length * size + base;
                plane(pose, vertices, length, size, endSize, base, top, opacity, beam);
            }
        } finally { pose.popPose(); }
    }

    /** One plane of the fan of the original ({@code FXBeamWand:247-254}). */
    private static void plane(PoseStack pose, VertexConsumer vertices, float length, float size, float endSize,
                              float v0, float v1, float alpha, Beam beam) {
        float half = PLANE_HALF_WIDTH * size;
        float halfEnd = PLANE_HALF_WIDTH * size * endSize;
        float far = length * size;
        vertex(pose, vertices, -halfEnd, far, 0.0F, 1.0F, v1, alpha, beam);
        vertex(pose, vertices, -half, 0.0F, 0.0F, 1.0F, v0, alpha, beam);
        vertex(pose, vertices, half, 0.0F, 0.0F, 0.0F, v0, alpha, beam);
        vertex(pose, vertices, halfEnd, far, 0.0F, 0.0F, v1, alpha, beam);
    }

    /** The yaw of the direction of the beam, in degrees. */
    public static float yaw(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
    }

    /** The elevation of the direction of the beam, in degrees. */
    public static float elevation(Vec3 direction) {
        return (float) Math.toDegrees(Math.asin(Mth.clamp(direction.y, -1.0F, 1.0F)));
    }

    /**
     * The rotation of the fan of the original ({@code FXBeamWand:171}).
     *
     * @param gameTime the time of the world
     * @param partial the partial tick of the frame
     * @return the rotation of the fan, in degrees
     */
    public static float spin(long gameTime, float partial) {
        return (gameTime % (360L / ROTATION_SPEED)) * ROTATION_SPEED + ROTATION_SPEED * partial;
    }

    /**
     * The slide of the texture of the original ({@code FXBeamWand:201-206}).
     *
     * @param slide the age of the player
     * @param partial the partial tick of the frame
     * @return the slide of the texture
     */
    public static float scroll(int slide, float partial) {
        return scroll(slide, partial, false);
    }

    /**
     * The slide of the texture, optionally running the other way (the original {@code reverse} flag of the beam: the age the
     * texture is slid by changes its sign).
     */
    public static float scroll(int slide, float partial, boolean reverse) {
        float value = reverse ? -(slide + partial) : slide + partial;
        return -value * 0.2F - Mth.floor(-value * 0.1F);
    }

    private static float length(Vec3 end) { return (float) end.length(); }

    /**
     * The impact of the beam of the original ({@code FXBeamWand:269-297}).
     *
     * <p>The original draws one camera facing quad of the particle atlas of the original
     * ({@code thaumcraft:textures/misc/particles.png}), the frame of the atlas is the age of the
     * beam over sixteen and the size of the quad is {@code endMod / 2 / (6 - impact)}.</p>
     */
    private static void impact(PoseStack pose, VertexConsumer vertices, Camera camera, Vec3 end, Beam beam) {
        var rightVector = new Vector3f(1, 0, 0).rotate(camera.rotation());
        var upVector = new Vector3f(0, 1, 0).rotate(camera.rotation());
        Vec3 right = new Vec3(rightVector.x, rightVector.y, rightVector.z), up = new Vec3(upVector.x, upVector.y, upVector.z);
        // Original FXBeamWand:281 - the size of the impact of the original.
        float size = beam.endSize / 2.0F / (6 - beam.impact);
        // Original FXBeamWand:276-279 - the frame of the particle atlas.
        int part = beam.age % 16;
        float u = part / 16.0F, u1 = u + 0.0624375F;
        float v = 0.3125F, v1 = v + 0.0624375F;
        // Original FXBeamWand:275 and :288 - the colour of the beam with the alpha of two thirds.
        vertex(pose, vertices, end.subtract(right.scale(size)).subtract(up.scale(size)), u1, v1, IMPACT_ALPHA, beam);
        vertex(pose, vertices, end.add(right.scale(-size)).add(up.scale(size)), u1, v, IMPACT_ALPHA, beam);
        vertex(pose, vertices, end.add(right.scale(size)).add(up.scale(size)), u, v, IMPACT_ALPHA, beam);
        vertex(pose, vertices, end.add(right.scale(size)).subtract(up.scale(size)), u, v1, IMPACT_ALPHA, beam);
    }
    private static void vertex(PoseStack pose, VertexConsumer vertices, Vec3 point, float u, float v, float alpha, Beam beam) {
        vertices.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z).setColor(beam.red, beam.green, beam.blue, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 1, 0);
    }
    private static void vertex(PoseStack pose, VertexConsumer vertices, float x, float y, float z,
                               float u, float v, float alpha, Beam beam) {
        vertices.addVertex(pose.last(), x, y, z).setColor(beam.red, beam.green, beam.blue, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 1, 0);
    }
    private static final class BeamRenderType extends RenderType {
        private BeamRenderType(String texture) {
            this(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/" + texture + ".png"), false, false), texture);
        }
        private BeamRenderType(TextureStateShard texture, String name) {
            super("thaumcraft_excavation_" + name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, false,
                    () -> begin(texture), () -> end(texture));
        }
        private static void begin(TextureStateShard texture) {
            RENDERTYPE_EYES_SHADER.setupRenderState(); texture.setupRenderState(); LIGHTNING_TRANSPARENCY.setupRenderState();
            NO_CULL.setupRenderState(); LEQUAL_DEPTH_TEST.setupRenderState(); COLOR_WRITE.setupRenderState();
        }
        private static void end(TextureStateShard texture) {
            COLOR_WRITE.clearRenderState(); LEQUAL_DEPTH_TEST.clearRenderState(); NO_CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState(); texture.clearRenderState(); RENDERTYPE_EYES_SHADER.clearRenderState();
        }
    }
}
