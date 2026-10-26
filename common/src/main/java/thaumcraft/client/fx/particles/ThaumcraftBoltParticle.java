package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.bolt.WispLightningShape;
import thaumcraft.client.renderers.VisNetRender;

/**
 * The original node bolt of the eldritch trap and the node (oracle
 * {@code thaumcraft/client/ClientProxy.java:798-803}, {@code nodeBolt(world, x, y, z, x2, y2, z2)}):
 * the {@code FXLightningBolt} with the type zero, the duration ten, the multiplier four, the age step five and
 * the fractal of the original {@code FXLightningBoltCommon}.
 *
 * <p>The type zero colour of the original is the purple triple {@code 0.6, 0.3, 0.6} on the large pass and
 * {@code 1.0, 0.6, 1.0} on the small pass, with the additive blend {@code (SRC_ALPHA, ONE)} of the original.</p>
 */
public final class ThaumcraftBoltParticle extends Particle {
    /** The original nodeBolt duration. */
    private static final int DURATION = 10;
    /** The original nodeBolt multiplier. */
    private static final float MULTIPLIER = 4.0F;
    /** The original nodeBolt age step. */
    private static final int SPEED = 5;
    /** The original FXLightningBolt type zero large pass colour. */
    private static final int TYPE_ZERO_LARGE = 0x994c99;
    /** The original FXLightningBolt type zero small pass colour. */
    private static final int TYPE_ZERO_SMALL = 0xff99ff;

    private final WispLightningShape bolt;
    private int boltAge;

    private ThaumcraftBoltParticle(ClientLevel level, Vec3 from, Vec3 to, long seed) {
        super(level, from.x, from.y, from.z);
        // The original ClientProxy.nodeBolt(world, x, y, z, x2, y2, z2):
        // FXLightningBolt(world, x, y, z, x2, y2, z2, seed, 10, 4.0f, 5) - the duration ten, the multiplier four
        // and the age step five of the original FXLightningBoltCommon.
        bolt = new WispLightningShape(from, to, seed, DURATION, MULTIPLIER, SPEED);
        boltAge = -(int) (bolt.length * 3);
        xd = yd = zd = 0;
        hasPhysics = false;
        setSize(.01F, .01F);
    }

    /** The original nodeBolt between two points. */
    public static void between(ClientLevel level, Vec3 from, Vec3 to) {
        if (level == null || from == null || to == null) return;
        // The original float(0,0,0) normalisation produced NaN vertices; an empty bolt stays empty.
        if (from.distanceToSqr(to) < 1.0E-10) return;
        Minecraft.getInstance().particleEngine.add(new ThaumcraftBoltParticle(level, from, to, level.random.nextLong()));
    }

    @Override public void tick() {
        boltAge = Math.min(bolt.maxAge, boltAge + bolt.increment);
        if (boltAge >= bolt.maxAge) remove();
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) return;
        int range = Minecraft.useFancyGraphics() ? 100 : 50;
        if (player.distanceToSqr(x, y, z) > range * range) return;
        float fraction = boltAge >= 0 ? boltAge / (float) bolt.maxAge : 0;
        int travel = (int) (bolt.length * 3);
        int reach = travel == 0 ? bolt.mainSegments : (int) ((boltAge + partial + travel) / (float) travel * bolt.mainSegments);
        PoseStack pose = new PoseStack();
        var eye = camera.getPosition();
        pose.translate(-eye.x, -eye.y, -eye.z);
        var look = camera.getLookVector();
        Vec3 view = new Vec3(look.x, look.y, look.z);
        for (int pass = 0; pass < 2; pass++) {
            final float alpha = pass == 0 ? (1 - fraction) * .4F : 1 - fraction * .5F;
            final int rgb = pass == 0 ? TYPE_ZERO_LARGE : TYPE_ZERO_SMALL;
            VisNetRender.draw(pass == 0 ? VisNetRender.BOLT_LARGE : VisNetRender.BOLT_SMALL, GL11.GL_ONE, false,
                    VisNetRender.lowAlphaShader(), out -> {
                        for (var segment : bolt.segments) {
                            if (segment.number > reach) continue;
                            // The original divides the ribbon width by the joint sine; a sine the normalised dot
                            // product pushed out of the unit interval made the original vertex coordinates not a
                            // number and the original rasteriser dropped the quad. The port drops the same segment.
                            if (!segment.renderable()) continue;
                            Vec3 start = segment.start(), end = segment.end();
                            float dx = (float) player.getX() - (float) start.x;
                            float dy = (float) player.getY() - (float) start.y;
                            float dz = (float) player.getZ() - (float) start.z;
                            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                            float width = .03F * (distance / 5 + 1) * (1 + segment.light) * .5F;
                            Vec3 before = view.cross(segment.previousDirection()).scale(width / segment.sinPrevious());
                            Vec3 after = view.cross(segment.nextDirection()).scale(width / segment.sinNext());
                            float opacity = alpha * segment.light;
                            vertex(out, pose, end.subtract(after), .5F, 0, rgb, opacity);
                            vertex(out, pose, start.subtract(before), .5F, 0, rgb, opacity);
                            vertex(out, pose, start.add(before), .5F, 1, rgb, opacity);
                            vertex(out, pose, end.add(after), .5F, 1, rgb, opacity);
                            if (segment.last()) {
                                Vec3 rounded = end.add(segment.direction().normalize().scale(width));
                                vertex(out, pose, rounded.subtract(after), 0, 0, rgb, opacity);
                                vertex(out, pose, end.subtract(after), .5F, 0, rgb, opacity);
                                vertex(out, pose, end.add(after), .5F, 1, rgb, opacity);
                                vertex(out, pose, rounded.add(after), 0, 1, rgb, opacity);
                            }
                            if (segment.first()) {
                                Vec3 rounded = start.subtract(segment.direction().normalize().scale(width));
                                vertex(out, pose, start.subtract(before), .5F, 0, rgb, opacity);
                                vertex(out, pose, rounded.subtract(before), 0, 0, rgb, opacity);
                                vertex(out, pose, rounded.add(before), 0, 1, rgb, opacity);
                                vertex(out, pose, start.add(before), .5F, 1, rgb, opacity);
                            }
                        }
                    });
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, Vec3 point, float u, float v, int rgb, float alpha) {
        VisNetRender.vertex(out, pose, point, u, v, rgb, alpha);
    }
}
