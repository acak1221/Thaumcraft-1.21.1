package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
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
 * Server-eligible continuous green0x00FF66 beam, impact5/end-size2. Independently
 * written native camera-facing additive mesh: two tapered ribbons/five glints,
 * at most32 casters. This is an explicit bounded approximation, NOT legacy
 * rotating three-plane/particle/audio parity. No beam entity or client mining.
 * Original FXBeamWand:84,95-101 gives initial8/refresh-at-least4 tick lifetime.
 */
public final class ThaumcraftExcavationClient {
    private static final int MAX_BEAMS = 32;
    private static final RenderType BEAM = new BeamRenderType("beam2"), GLINT = new BeamRenderType("p_small");
    private static final Map<Integer, Beam> BEAMS = new LinkedHashMap<>();
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
    public static void clear() { BEAMS.clear(); }
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
        BEAMS.entrySet().removeIf(entry -> {
            Beam beam = entry.getValue(); ++beam.age; if (beam.impact > 0) --beam.impact;
            var caster = client.level.getEntity(entry.getKey());
            return --beam.remaining <= 0 || !client.level.dimension().location().equals(beam.dimension)
                    || caster == null || caster.isRemoved() || client.player.distanceToSqr(beam.origin) > 64 * 64;
        });
    }

    private static final class Beam {
        private final ResourceLocation dimension;
        private Vec3 previousOrigin, origin, previousTarget, target;
        private int age, remaining = 8, impact, crackStage;
        private float endSize;
        private boolean completed, success;
        private Beam(ExcavationFxPayload payload) {
            dimension = payload.dimension(); origin = payload.origin(); target = payload.target();
            refresh(payload); remaining = 8;
        }
        private void refresh(ExcavationFxPayload payload) {
            previousOrigin = origin; previousTarget = target; origin = payload.origin(); target = payload.target();
            remaining = Math.max(remaining, 4); impact = payload.impact() ? 5 : 0; endSize = payload.impact() ? 2 : 0;
            crackStage = payload.crackStage(); completed = payload.completed(); success = payload.success();
        }
    }

    /** Fabric LAST: SAME untranslated world pose/buffers/partial/camera as Shock.
     * Camera subtraction happens ONCE here, never in the loader hook. */
    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick, Camera camera) {
        var client = Minecraft.getInstance();
        if (pose == null || client.level == null || client.player == null || BEAMS.isEmpty()) return;
        float partial = Mth.clamp(partialTick, 0, 1);
        for (int pass = 0; pass < 2; pass++) {
            RenderType type = pass == 0 ? BEAM : GLINT;
            VertexConsumer vertices = buffers.getBuffer(type);
            for (Beam beam : BEAMS.values()) {
                if (!client.level.dimension().location().equals(beam.dimension)) continue;
                Vec3 origin = beam.previousOrigin.lerp(beam.origin, partial), target = beam.previousTarget.lerp(beam.target, partial);
                Vec3 end = target.subtract(origin), eye = camera.getPosition().subtract(origin);
                float opacity = .4F * Math.min((beam.age + partial) / 4F, 1) * Math.min(beam.remaining / 4F, 1);
                pose.pushPose(); pose.translate(-eye.x, -eye.y, -eye.z);
                ribbon(pose, vertices, eye, end, (pass == 0 ? .15 : .05), beam.endSize, opacity);
                if (pass == 1 && beam.impact > 0) impact(pose, vertices, camera, end, beam, opacity);
                pose.popPose();
            }
            buffers.endBatch(type);
        }
    }

    private static void ribbon(PoseStack pose, VertexConsumer vertices, Vec3 eye, Vec3 end, double width, float endSize, float alpha) {
        if (end.lengthSqr() < 1.0E-8) return;
        Vec3 side = end.cross(eye.subtract(end.scale(.5)));
        if (side.lengthSqr() < 1.0E-8) side = end.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(1, 0, 0);
        Vec3 startWidth = side.normalize().scale(width), endWidth = startWidth.scale(endSize);
        vertex(pose, vertices, end.subtract(endWidth), 0, 0, alpha);
        vertex(pose, vertices, startWidth.scale(-1), 0, 1, alpha);
        vertex(pose, vertices, startWidth, 1, 1, alpha);
        vertex(pose, vertices, end.add(endWidth), 1, 0, alpha);
    }
    private static void impact(PoseStack pose, VertexConsumer vertices, Camera camera, Vec3 end, Beam beam, float alpha) {
        var rightVector = new Vector3f(1, 0, 0).rotate(camera.rotation());
        var upVector = new Vector3f(0, 1, 0).rotate(camera.rotation());
        Vec3 right = new Vec3(rightVector.x, rightVector.y, rightVector.z), up = new Vec3(upVector.x, upVector.y, upVector.z);
        for (int i = 0; i < 5; i++) {
            double angle = (beam.age * .2 + i * Math.PI * .5);
            Vec3 center = end.add(right.scale(i == 0 ? 0 : Math.cos(angle) * .12)).add(up.scale(i == 0 ? 0 : Math.sin(angle) * .12));
            double size = (i == 0 ? .18 : .065) * beam.endSize / (6 - beam.impact);
            Vec3 r = right.scale(size), u = up.scale(size);
            vertex(pose, vertices, center.subtract(r).subtract(u), 0, 1, alpha);
            vertex(pose, vertices, center.add(r).subtract(u), 1, 1, alpha);
            vertex(pose, vertices, center.add(r).add(u), 1, 0, alpha);
            vertex(pose, vertices, center.subtract(r).add(u), 0, 0, alpha);
        }
    }
    private static void vertex(PoseStack pose, VertexConsumer vertices, Vec3 point, float u, float v, float alpha) {
        vertices.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z).setColor(0F, 1F, .4F, alpha)
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
