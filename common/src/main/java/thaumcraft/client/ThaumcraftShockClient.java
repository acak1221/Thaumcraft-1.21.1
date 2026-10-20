package thaumcraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.common.lib.network.ShockFxNetworking;
import thaumcraft.common.lib.network.ShockFxPayload;

/**
 * Paid-server-pulse visuals only. TC4 ItemFocusShock:99-117 / FXLightningBoltCommon:76-87,138-145:
 * seed, duration6, multiplier.5, speed8, type2 blue, width.125, large/small additive passes.
 * Independently written midpoint ribbons: bounded128 main segments +8 two-segment forks,
 * five impact glints, at most32 live bolts. NOT full legacy fractal/branch/joint parity.
 * No lightning entity, world writes, local damage, payment or speculative client firing.
 */
public final class ThaumcraftShockClient {
    private static final int MAX_BOLTS = 32;
    private static final RenderType LARGE = new BoltRenderType("large"), SMALL = new BoltRenderType("small");
    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static boolean initialized;

    private ThaumcraftShockClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ShockFxNetworking.initClient(ThaumcraftShockClient::accept);
        ClientTickEvent.CLIENT_POST.register(ThaumcraftShockClient::tick);
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> BOLTS.clear());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> BOLTS.clear());
    }

    /** Read-only visual state; never an effect-injection/test-control entry point. */
    public static int activeBoltCount() { return BOLTS.size(); }

    private static void accept(ShockFxPayload payload) {
        Minecraft client = Minecraft.getInstance();
        var level = client.level;
        if (level == null || client.player == null || !level.dimension().location().equals(payload.dimension())
                || client.player.distanceToSqr(payload.origin()) > 64 * 64
                || !level.hasChunkAt(BlockPos.containing(payload.origin())) || !level.hasChunkAt(BlockPos.containing(payload.target()))) return;
        var caster = level.getEntity(payload.casterId());
        if (caster == null || caster.isRemoved() || !caster.getBoundingBox().inflate(3).contains(payload.origin())) return;
        if (BOLTS.size() == MAX_BOLTS) BOLTS.removeFirst();
        BOLTS.add(new Bolt(payload));
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) { BOLTS.clear(); return; }
        BOLTS.removeIf(bolt -> ++bolt.age >= bolt.lifetime || !client.level.dimension().location().equals(bolt.dimension)
                || client.player.distanceToSqr(bolt.origin) > 64 * 64);
    }

    private record Segment(Vec3 from, Vec3 to, float light, float progress) {}
    private record Glint(Vec3 motion, float size) {}

    private static final class Bolt {
        private final ResourceLocation dimension;
        private final Vec3 origin, end;
        private final List<Segment> segments;
        private final List<Glint> glints;
        private final int travel, fade, lifetime;
        private int age;

        private Bolt(ShockFxPayload pulse) {
            dimension = pulse.dimension(); origin = pulse.origin(); end = pulse.target().subtract(origin);
            Random random = new Random(pulse.seed());
            double length = end.length();
            travel = Math.max(1, (int)(length * 3));
            fade = 6 + random.nextInt(6) - 3;
            lifetime = (travel + fade + 7) / 8;
            List<Vec3> points = List.of(Vec3.ZERO, end);
            int[] divisors = {8, 12, 17, 23, 30, 34, 40};
            for (int divisor : divisors) {
                List<Vec3> next = new ArrayList<>();
                for (int i = 0; i < points.size() - 1; i++) {
                    Vec3 a = points.get(i), b = points.get(i + 1), axis = b.subtract(a).normalize();
                    Vec3 side = perpendicular(axis), up = axis.cross(side).normalize();
                    double angle = random.nextDouble() * Math.PI * 2, amount = (random.nextDouble() - .5) * length * .5 / divisor;
                    next.add(a);
                    next.add(a.add(b).scale(.5).add(side.scale(Math.cos(angle) * amount)).add(up.scale(Math.sin(angle) * amount)));
                }
                next.add(end); points = next;
            }
            List<Segment> result = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i), b = points.get(i + 1);
                float progress = (i + 1) / (float)(points.size() - 1);
                result.add(new Segment(a, b, 1, progress));
                if (i % 16 == 8) {
                    Vec3 offset = new Vec3(random.nextDouble() - .5, random.nextDouble() - .5, random.nextDouble() - .5)
                            .scale(Math.min(.6, length * .04));
                    Vec3 middle = b.add(offset.scale(.55)), tip = b.add(offset);
                    result.add(new Segment(a, middle, .5F, progress)); result.add(new Segment(middle, tip, .25F, progress));
                }
            }
            segments = List.copyOf(result);
            List<Glint> impact = new ArrayList<>();
            if (pulse.impact()) for (int i = 0; i < 5; i++) impact.add(new Glint(
                    new Vec3(random.nextDouble() - .5, random.nextDouble() - .5, random.nextDouble() - .5).scale(.15),
                    .06F + random.nextFloat() * .04F));
            glints = List.copyOf(impact);
        }
    }

    /**
     * Fabric LAST hook: pass the SAME un-translated world pose/BufferSource/partial/camera as goggles.
     * This method subtracts the camera ONCE, per bolt. No loader-side -camera translation.
     * Native shader/buffer batches own GL state; no legacy immediate-mode calls are copied.
     */
    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, float partialTick, Camera camera) {
        var client = Minecraft.getInstance();
        if (pose == null || client.level == null || client.player == null || BOLTS.isEmpty()) return;
        float partial = Mth.clamp(partialTick, 0, 1);
        for (int pass = 0; pass < 2; pass++) {
            RenderType type = pass == 0 ? LARGE : SMALL;
            VertexConsumer vertices = buffers.getBuffer(type);
            for (Bolt bolt : BOLTS) {
                if (!client.level.dimension().location().equals(bolt.dimension)) continue;
                Vec3 eye = camera.getPosition().subtract(bolt.origin);
                float time = (bolt.age + partial) * 8;
                float reach = Mth.clamp(time / bolt.travel, 0, 1), fade = Mth.clamp((time - bolt.travel) / bolt.fade, 0, 1);
                pose.pushPose();
                pose.translate(-eye.x, -eye.y, -eye.z);
                for (Segment segment : bolt.segments) if (segment.progress <= reach) ribbon(pose, vertices, eye, segment, pass, fade);
                if (pass == 1 && reach >= 1) impact(pose, vertices, camera, bolt, fade);
                pose.popPose();
            }
            buffers.endBatch(type);
        }
    }

    private static Vec3 perpendicular(Vec3 axis) {
        Vec3 side = axis.cross(new Vec3(0, 1, 0));
        return side.lengthSqr() < 1.0E-8 ? new Vec3(1, 0, 0) : side.normalize();
    }

    private static void ribbon(PoseStack pose, VertexConsumer vertices, Vec3 eye, Segment segment, int pass, float fade) {
        Vec3 middle = segment.from.add(segment.to).scale(.5), direction = segment.to.subtract(segment.from);
        Vec3 side = direction.cross(eye.subtract(middle));
        if (side.lengthSqr() < 1.0E-8) side = perpendicular(direction.normalize());
        double width = .125 * (eye.distanceTo(middle) / 5 + 1) * (1 + segment.light) * .5;
        side = side.normalize().scale(width);
        float alpha = (pass == 0 ? (1 - fade) * .4F : 1 - fade * .5F) * segment.light;
        vertex(pose, vertices, segment.to.subtract(side), .5F, 0, pass, alpha);
        vertex(pose, vertices, segment.from.subtract(side), .5F, 0, pass, alpha);
        vertex(pose, vertices, segment.from.add(side), .5F, 1, pass, alpha);
        vertex(pose, vertices, segment.to.add(side), .5F, 1, pass, alpha);
    }

    private static void impact(PoseStack pose, VertexConsumer vertices, Camera camera, Bolt bolt, float fade) {
        Vector3f rightVector = new Vector3f(1, 0, 0).rotate(camera.rotation()), upVector = new Vector3f(0, 1, 0).rotate(camera.rotation());
        Vec3 right = new Vec3(rightVector.x, rightVector.y, rightVector.z), up = new Vec3(upVector.x, upVector.y, upVector.z);
        for (Glint glint : bolt.glints) {
            Vec3 center = bolt.end.add(glint.motion.scale(bolt.age + .5));
            Vec3 r = right.scale(glint.size), u = up.scale(glint.size);
            vertex(pose, vertices, center.subtract(r).subtract(u), 0, 1, 1, 1 - fade);
            vertex(pose, vertices, center.add(r).subtract(u), 1, 1, 1, 1 - fade);
            vertex(pose, vertices, center.add(r).add(u), 1, 0, 1, 1 - fade);
            vertex(pose, vertices, center.subtract(r).add(u), 0, 0, 1, 1 - fade);
        }
    }

    private static void vertex(PoseStack pose, VertexConsumer vertices, Vec3 point, float u, float v, int pass, float alpha) {
        vertices.addVertex(pose.last(), (float)point.x, (float)point.y, (float)point.z).setColor(.1F, .1F, pass == 0 ? .6F : 1F, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 1, 0);
    }

    private static final class BoltRenderType extends RenderType {
        private BoltRenderType(String size) {
            this(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/p_" + size + ".png"), false, false), size);
        }
        private BoltRenderType(TextureStateShard texture, String size) {
            super("thaumcraft_shock_" + size, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 16384, false, false,
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
