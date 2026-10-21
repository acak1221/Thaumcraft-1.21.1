package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.monster.EntityEldritchGolem;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 {@code FXArc} (oracle {@code thaumcraft/client/fx/beams/FXArc.java:26-144}), the
 * gravity bent lightning arc of the eldritch golem.
 * <p>
 * The original builds the ribbon in its constructor: the launch velocity comes from
 * {@code Utils.calculateVelocity} with a gravity of {@code 0.115} and the requested height gain,
 * the arc is walked while the remaining distance to the target is longer than that velocity, at
 * most fifty times, and every emitted point is displaced by {@code 0.25} of noise while the walked
 * position itself stays clean. The ribbon is two crossed triangle strips of
 * {@code textures/misc/beamh.png} with a half width of {@code 0.25}, an alpha of {@code 0.8} and
 * the additive blend {@code 770,1}.
 * <p>
 * The original's UVs are the point index divided by the arc length, not by the point count; that
 * quirk is kept. A height gain that the original's projectile maths turns into a negative gain
 * produces NaN points and no visible arc; the original submits those NaN vertices, the native
 * skips the strip instead so no garbage reaches the driver.
 */
public final class FXArc extends Particle {
    /** Original FXArc:103. */
    private static final float SIZE = 0.25F;
    /** Original FXArc:111 and :126. */
    private static final float ALPHA = 0.8F;
    /** Original FXArc:34. */
    public static final net.minecraft.resources.ResourceLocation TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/beamh.png");
    private final List<Vec3> points = new ArrayList<>();
    private final float red, green, blue;
    private float length;

    private FXArc(ClientLevel level, double x, double y, double z, double tx, double ty, double tz,
                  float red, float green, float blue, double heightGain) {
        super(level, x, y, z);
        this.red = red;
        this.green = green;
        this.blue = blue;
        xd = yd = zd = 0;
        hasPhysics = false;
        setSize(.02F, .02F);
        // Original FXArc:50.
        lifetime = 1;
        Vec3 target = new Vec3(tx - x, ty - y, tz - z);
        // Original FXArc:57-71: the arc points, the noise moves the emitted point only.
        points.addAll(EldritchMobRules.golemArcPoints(target, heightGain, random));
        length = (float) target.length();
    }

    /**
     * Original {@code ClientProxy.arcLightning} (oracle {@code thaumcraft/client/ClientProxy.java:1152-1159}):
     * the target sparkle is created first, then the arc itself.
     */
    public static void arc(EntityEldritchGolem golem, double x, double y, double z,
                           double tx, double ty, double tz, float red, float green, float blue, float heightGain) {
        if (!(golem.level() instanceof ClientLevel level)) return;
        FXArcSparkle.emit(level, tx, ty, tz, red, green, blue);
        Minecraft.getInstance().particleEngine.add(new FXArc(level, x, y, z, tx, ty, tz, red, green, blue, heightGain));
    }

    /** Original FXArc:112-121 and :127-136, the vertical and the horizontal crossed strip. */
    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        if (!finite()) return;
        PoseStack pose = new PoseStack();
        var eye = camera.getPosition();
        pose.translate(Mth.lerp(partial, xo, x) - eye.x, Mth.lerp(partial, yo, y) - eye.y, Mth.lerp(partial, zo, z) - eye.z);
        int colour = (int) (red * 255) << 16 | (int) (green * 255) << 8 | (int) (blue * 255);
        VisNetRender.drawStrip(TEXTURE, GL11.GL_ONE, false, GameRenderer.getPositionTexColorShader(),
                out -> strip(out, pose, colour, true));
        VisNetRender.drawStrip(TEXTURE, GL11.GL_ONE, false, GameRenderer.getPositionTexColorShader(),
                out -> strip(out, pose, colour, false));
    }

    private void strip(VertexConsumer out, PoseStack pose, int colour, boolean vertical) {
        for (int c = 0; c < points.size(); ++c) {
            Vec3 point = points.get(c);
            // Original FXArc:116: the point index over the arc length, not over the point count.
            float u = c / length;
            if (vertical) {
                VisNetRender.vertex(out, pose, new Vec3(point.x, point.y - SIZE, point.z), u, 1, colour, ALPHA);
                VisNetRender.vertex(out, pose, new Vec3(point.x, point.y + SIZE, point.z), u, 0, colour, ALPHA);
            } else {
                VisNetRender.vertex(out, pose, new Vec3(point.x - SIZE, point.y, point.z - SIZE), u, 1, colour, ALPHA);
                VisNetRender.vertex(out, pose, new Vec3(point.x + SIZE, point.y, point.z + SIZE), u, 0, colour, ALPHA);
            }
        }
    }

    /** The original's projectile maths yields NaN for a negative height gain; such an arc is invisible. */
    private boolean finite() {
        if (!Float.isFinite(length)) return false;
        for (Vec3 point : points)
            if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) return false;
        return true;
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        // Original FXArc:74-76.
        if (age++ >= lifetime) remove();
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
}
