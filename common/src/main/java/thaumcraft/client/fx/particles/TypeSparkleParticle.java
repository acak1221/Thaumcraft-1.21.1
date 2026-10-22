package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;

/**
 * The configurable sparkle of the original ({@code thaumcraft/client/fx/particles/FXSparkle.java}).
 *
 * <p>Oracle {@code FXSparkle.java:22-110}: the sparkle lives {@code 3 * multiplier} ticks, it
 * draws the frames {@code 16 + age / multiplier} of the sheet {@code textures/misc/particles.png}
 * (the row four, the frames of the four by four block at the column sixteen), it shrinks over its
 * life, it has no gravity and no movement and the alpha of the draw is {@code 0.75f}.</p>
 *
 * <p>The colour is the one of the type of the original
 * ({@code FXSparkle.java:50-105}): the type zero is the green of the shimmerleaf, the type one the
 * orange, the type two the blue of the portable hole, the type three and the type four the two
 * greens, the type five the dark random grey, the type six the white of the frost focus and the
 * type seven the cyan.</p>
 *
 * <p>The quad of the original is {@code 0.1f * field_70544_f} and the field is the size of the
 * particle, which the 1.7.10 {@code EntityFX} sets to a random one to two
 * ({@code EntityFX.java:41}) and the constructor of the original multiplies with the size of the
 * call.</p>
 */
public final class TypeSparkleParticle extends Particle {
    /** The first frame of the sheet of the original. */
    public static final int FIRST_FRAME = 16;
    /** The quarter of the particle of the original. */
    public static final float QUAD = .1F;
    /** The alpha of the original. */
    public static final float ALPHA = .75F;

    private final float size;
    private final int multiplier;
    private final float gravity;

    public TypeSparkleParticle(ClientLevel level, double x, double y, double z, float size, int type, int multiplier) {
        this(level, x, y, z, size, type, multiplier, 0F);
    }

    /**
     * The sparkle of the original with its gravity.
     *
     * <p>Oracle {@code FXSparkle.java:168-172} ({@code setGravity}) and the tick of the original
     * ({@code FXSparkle.java:172-176}): the motion of the particle falls with
     * {@code 0.04 * gravity} every tick.</p>
     */
    public TypeSparkleParticle(ClientLevel level, double x, double y, double z, float size, int type, int multiplier, float gravity) {
        super(level, x, y, z);
        this.multiplier = multiplier;
        this.gravity = gravity;
        lifetime = 3 * multiplier;
        hasPhysics = false;
        xd = yd = zd = 0;
        xo = x; yo = y; zo = z;
        // The 1.7.10 EntityFX draws the size of the particle as a random one to two.
        this.size = QUAD * (level.random.nextFloat() * .5F + .5F) * 2F * size;
        float[] colour = colour(type, level.random);
        setColor(colour[0], colour[1], colour[2]);
        setSize(.01F, .01F);
    }

    /**
     * The colour of the type of the original.
     *
     * <p>Oracle {@code FXSparkle.java:50-105}. The type two is the blue of the portable hole:
     * {@code 0.2f, 0.2f, 0.7f + rand * 0.3f}.</p>
     */
    public static float[] colour(int type, RandomSource random) {
        return switch (type) {
            case 0 -> new float[]{.75F + random.nextFloat() * .25F, .25F + random.nextFloat() * .25F, .75F + random.nextFloat() * .25F};
            case 1 -> new float[]{.5F + random.nextFloat() * .3F, .5F + random.nextFloat() * .3F, .2F};
            case 2 -> new float[]{.2F, .2F, .7F + random.nextFloat() * .3F};
            case 3 -> new float[]{.2F, .7F + random.nextFloat() * .3F, .2F};
            case 4 -> new float[]{.7F + random.nextFloat() * .3F, .2F, .2F};
            case 5 -> new float[]{random.nextFloat() * .1F, random.nextFloat() * .1F, random.nextFloat() * .1F};
            case 6 -> new float[]{.8F + random.nextFloat() * .2F, .8F + random.nextFloat() * .2F, .8F + random.nextFloat() * .2F};
            case 7 -> new float[]{.2F, .5F + random.nextFloat() * .3F, .6F + random.nextFloat() * .3F};
            default -> new float[]{1F, 1F, 1F};
        };
    }

    /**
     * The number of the particles the original draws for one call of its proxy.
     *
     * <p>Oracle {@code ClientProxy.java:899-905} ({@code particleCount}): the base of the proxy
     * sparkle is two, so the count is zero at the minimal particle setting, two at the decreased
     * one and four at the full one, and the proxy draws the particle when
     * {@code rand.nextInt(6) < count}.</p>
     */
    public static int particleCount() {
        var setting = Minecraft.getInstance().options.particles().get();
        return setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? 2 : 4;
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        // The original falls with 0.04 * gravity every tick.
        yd -= .04F * gravity;
        move(xd, yd, zd);
        xd *= .9080000019073486F; yd *= .9080000019073486F; zd *= .9080000019073486F;
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        var pose = new PoseStack();
        var eye = camera.getPosition();
        pose.translate(Mth.lerp(partial, xo, x) - eye.x, Mth.lerp(partial, yo, y) - eye.y, Mth.lerp(partial, zo, z) - eye.z);
        pose.mulPose(camera.rotation());
        // The frame of the original: the particle field plus the age over the multiplier.
        int part = FIRST_FRAME + age / multiplier;
        float u = (part % 4) / 16F;
        int rgb = (int) (rCol * 255) << 16 | (int) (gCol * 255) << 8 | (int) (bCol * 255);
        // The original shrinks the quad over the life of the particle.
        float quad = size * (lifetime - age + 1) / lifetime;
        VisNetRender.draw(VisNetRender.PARTICLES, GL11.GL_ONE, false, out ->
                VisNetRender.quad(out, pose, quad, ALPHA, rgb, u, u + .0624375F, .25F, .3124375F));
    }
}
