package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Visual-only native particle arc: original nodeBolt type-0 colours, .03 width,
 * p_large/p_small passes, duration10/multiplier4/speed5. The bounded 32-segment
 * displacement/forks are a native approximation, NOT legacy fractal/branch parity.
 * No lightning entity, collision damage, terrain operation or game RNG on server.
 */
public final class InfusionArcParticle extends Particle {
    private static final ResourceLocation LARGE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/p_large.png");
    private static final ResourceLocation SMALL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/p_small.png");
    private static final Tesselator BUFFER = new Tesselator(16384);
    private final InfusionParticle.Scope scope;
    private final BlockPos target;
    private final List<Segment> segments;
    private final int travel, fade;

    private record Segment(Vec3 from, Vec3 to, float light, float progress) {}

    public InfusionArcParticle(ClientLevel level, InfusionParticle.Scope scope, Vec3 from, Vec3 to) {
        super(level, from.x, from.y, from.z);
        this.scope = scope;
        target = BlockPos.containing(to);
        hasPhysics = false;
        xd = yd = zd = 0;
        double length = from.distanceTo(to);
        travel = Math.max(1, (int)(length * 3));
        fade = 5 + random.nextInt(10);
        lifetime = Math.min(40, (travel + fade + 4) / 5);
        List<Vec3> points = List.of(from, to);
        // Independently written midpoint bridge, deliberately bounded rather
        // than importing the old WRVector3/bolt manager or donor implementations.
        for (int depth = 0; depth < 5; depth++) {
            List<Vec3> next = new ArrayList<>();
            int divisor = switch (depth) { case 0 -> 8; case 1 -> 12; case 2 -> 17; case 3 -> 23; default -> 30; };
            double amount = length * 4 / divisor;
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i), b = points.get(i + 1);
                Vec3 axis = b.subtract(a).normalize();
                Vec3 side = axis.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1.0E-6) side = axis.cross(new Vec3(1, 0, 0));
                side = side.normalize();
                Vec3 up = axis.cross(side).normalize();
                double angle = random.nextDouble() * Math.PI * 2;
                double displacement = (random.nextDouble() - .5) * amount;
                Vec3 middle = a.add(b).scale(.5).add(side.scale(Math.cos(angle) * displacement))
                        .add(up.scale(Math.sin(angle) * displacement));
                next.add(a);
                next.add(middle);
            }
            next.add(to);
            points = next;
        }
        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            float progress = (i + 1) / (float)(points.size() - 1);
            result.add(new Segment(points.get(i), points.get(i + 1), 1, progress));
            if (i % 8 == 4) {
                Vec3 branch = points.get(i + 1).add((random.nextDouble() - .5) * .8,
                        (random.nextDouble() - .5) * .8, (random.nextDouble() - .5) * .8);
                result.add(new Segment(points.get(i), branch, .5F, progress));
            }
        }
        segments = List.copyOf(result);
        setBoundingBox(new AABB(from, to).inflate(1));
    }

    private boolean valid() {
        if (!scope.valid(level) || !InfusionParticle.loaded(level, target)) return false;
        // No implicit client chunk loads, even for a fork crossing a boundary.
        for (Segment segment : segments) {
            if (!InfusionParticle.loaded(level, BlockPos.containing(segment.from))
                    || !InfusionParticle.loaded(level, BlockPos.containing(segment.to))) return false;
        }
        return true;
    }

    @Override public void tick() {
        if (age++ >= lifetime || !valid()) remove();
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        if (!isAlive() || !valid()) return;
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        float time = (age + partialTick) * 5;
        float reach = Mth.clamp(time / travel, 0, 1);
        float fading = Mth.clamp((time - travel) / fade, 0, 1);
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            for (int pass = 0; pass < 2; pass++) {
                RenderSystem.setShaderTexture(0, pass == 0 ? LARGE : SMALL);
                var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                for (Segment segment : segments) {
                    if (segment.progress > reach) continue;
                    ribbon(buffer, camera.getPosition(), segment, pass, fading);
                }
                var mesh = buffer.build();
                if (mesh != null) BufferUploader.drawWithShader(mesh);
            }
        } finally {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }

    private static void ribbon(VertexConsumer buffer, Vec3 eye, Segment segment, int pass, float fading) {
        Vec3 middle = segment.from.add(segment.to).scale(.5);
        Vec3 direction = segment.to.subtract(segment.from);
        Vec3 side = direction.cross(eye.subtract(middle));
        if (side.lengthSqr() < 1.0E-8) side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(1, 0, 0);
        float width = .03F * ((float)eye.distanceTo(middle) / 5 + 1) * (1 + segment.light) * .5F;
        side = side.normalize().scale(width);
        float alpha = (pass == 0 ? (1 - fading) * .4F : 1 - fading * .5F) * segment.light;
        vertex(buffer, segment.to.subtract(side).subtract(eye), 0, pass, alpha);
        vertex(buffer, segment.from.subtract(side).subtract(eye), 0, pass, alpha);
        vertex(buffer, segment.from.add(side).subtract(eye), 1, pass, alpha);
        vertex(buffer, segment.to.add(side).subtract(eye), 1, pass, alpha);
    }

    private static void vertex(VertexConsumer buffer, Vec3 point, float v, int pass, float alpha) {
        buffer.addVertex((float)point.x, (float)point.y, (float)point.z).setUv(.5F, v)
                .setColor(pass == 0 ? .6F : 1, pass == 0 ? .3F : .6F, pass == 0 ? .6F : 1, alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
