package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The native port of the original {@code FXBoreSparkle} (oracle
 * {@code thaumcraft/client/fx/particles/FXBoreSparkle.java}), the flying dig
 * sparkle of the arcane bore.
 *
 * <p>The original numbers, kept one for one:
 * <ul>
 * <li>{@code FXBoreSparkle.java:32-34} the particle starts grey and is given the
 * dig colour in {@code :51-53}: red {@code 0.2}, green {@code 0.6 + rand*0.3},
 * blue {@code 0.2}.</li>
 * <li>{@code FXBoreSparkle.java:35} the particle scale is {@code rand*0.5+0.5}.</li>
 * <li>{@code FXBoreSparkle.java:42-46} the life is {@code base/2 + rand(base)}
 * with {@code base = max(1, (int)(distance*3))}.</li>
 * <li>{@code FXBoreSparkle.java:47-50} the velocity is {@code gauss*0.01} on
 * every axis.</li>
 * <li>{@code FXBoreSparkle.java:56-63} a particle that is farther than the
 * render distance of the client (sixty four blocks, thirty two in the fast
 * graphics) dies at once.</li>
 * <li>{@code FXBoreSparkle.java:67} the size is {@code 0.1*scale*bob} with
 * {@code bob = sin(age/3)*0.5+1}.</li>
 * <li>{@code FXBoreSparkle.java:69-73} the sprite is the cell
 * {@code age%4} of the second row of the sixteen cell sheet.</li>
 * <li>{@code FXBoreSparkle.java:79} the particle is drawn with the full
 * bright light.</li>
 * <li>{@code FXBoreSparkle.java:91-116} the particle steers to its target with
 * {@code 0.3} (with {@code 0.6} and a shrinking scale below four blocks), the
 * velocity is damped with {@code 0.985} and clamped to {@code +-0.35}.</li>
 * </ul>
 *
 * <p>The original pushes the particle out of solid blocks with its own
 * {@code pushOutOfBlocks} routine; the native particle never collides with the
 * world, exactly like the native port of the sibling infusion particles
 * ({@code InfusionParticle}), and it never moves a game item.
 */
public final class BoreDigSparkleParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    /** The original sheet of the sparkle: sixteen cells, the second row is used. */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Tesselator BUFFER = new Tesselator(256);
    /** Original FXBoreSparkle:57 - the render distance of the client. */
    public static final int RENDER_DISTANCE = 64;
    /** Original FXBoreSparkle:58-60 - the render distance in the fast graphics. */
    public static final int FAST_RENDER_DISTANCE = 32;

    private final Vec3 target;
    private float scale;

    private BoreDigSparkleParticle(ClientLevel level, double x, double y, double z, Vec3 target) {
        super(level, x, y, z);
        this.target = target;
        xd = yd = zd = 0;
        gravity = 0;
        hasPhysics = false;
        setSize(.01F, .01F);
        scale = random.nextFloat() * .5F + .5F;
        int base = Math.max(1, (int) (target.subtract(x, y, z).length() * 3.0F));
        lifetime = base / 2 + random.nextInt(base);
        xd = random.nextGaussian() * .01F;
        yd = random.nextGaussian() * .01F;
        zd = random.nextGaussian() * .01F;
        // Original FXBoreSparkle:51-53.
        setColor(.2F, .6F + random.nextFloat() * .3F, .2F);
        // Original FXBoreSparkle:68 - the colour of the original is drawn with
        // an alpha of 0.75.
        alpha = .75F;
    }

    /**
     * The original {@code ClientProxy.boreDigFx} one in ten branch
     * (oracle {@code thaumcraft/client/ClientProxy.java:867-869}): the sparkle
     * starts inside the dug block and flies to the centre of the block the bore
     * digs into.
     *
     * @param level the client level
     * @param dig the block the bore digs
     * @param bore the block the bore digs into
     * @return the new particle, or null when the client cannot see it
     */
    public static BoreDigSparkleParticle dig(ClientLevel level, BlockPos dig, BlockPos bore) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        double x = dig.getX() + level.random.nextFloat();
        double y = dig.getY() + level.random.nextFloat();
        double z = dig.getZ() + level.random.nextFloat();
        Vec3 target = new Vec3(bore.getX() + .5D, bore.getY() + .5D, bore.getZ() + .5D);
        var particle = new BoreDigSparkleParticle(level, x, y, z, target);
        int distance = Minecraft.useFancyGraphics() ? RENDER_DISTANCE : FAST_RENDER_DISTANCE;
        if (player.distanceToSqr(x, y, z) > (double) distance * distance) particle.lifetime = 0;
        return particle;
    }

    /** Original FXBoreSparkle:42-46, the life of a sparkle of that distance. */
    public static int lifetimeFor(double distance, int randomBase) {
        int base = Math.max(1, (int) (distance * 3.0F));
        return base / 2 + randomBase % base;
    }

    /** Original FXBoreSparkle:67, the size of a sparkle of that age and scale. */
    public static float sizeFor(float scale, int age) {
        return .1F * scale * (Mth.sin(age / 3F) * .5F + 1F);
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        // Original FXBoreSparkle:91 - the particle dies in the target block.
        if (BlockPos.containing(x, y, z).equals(BlockPos.containing(target))) { remove(); return; }
        move(xd, yd, zd);
        xd *= .985F; yd *= .985F; zd *= .985F;
        Vec3 offset = target.subtract(x, y, z);
        double distance = offset.length();
        if (distance < 1.0E-4) { remove(); return; }
        double acceleration = distance < 4.0 ? .6 : .3;
        if (distance < 4.0) scale *= .9F;
        xd += offset.x / distance * acceleration;
        yd += offset.y / distance * acceleration;
        zd += offset.z / distance * acceleration;
        xd = Mth.clamp(xd, -.35F, .35F);
        yd = Mth.clamp(yd, -.35F, .35F);
        zd = Mth.clamp(zd, -.35F, .35F);
    }

    @Override public float getQuadSize(float partialTick) {
        return sizeFor(scale, age);
    }

    @Override protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    /** Original FXBoreSparkle:69-73 - the cell age%4 of the second row. */
    @Override protected float getU0() { return (age % 4) / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return .25F; }
    @Override protected float getV1() { return .25F + .0624375F; }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            super.render(buffer, camera, partialTick);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }
}
