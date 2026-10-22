package thaumcraft.client.fx.particles;

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
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** Native ParticleEngine adaptation of TC4 FXVent; no second particle manager. */
public final class FXVent extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return 1; }
    private static final Tesselator BUFFER = new Tesselator(256);
    private float scale;

    private FXVent(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, int colour) {
        super(level, x, y, z);
        setSize(.02F, .02F);
        scale = random.nextFloat() * .1F + .05F;
        hasPhysics = false;
        setColor((colour >> 16 & 255) / 255F, (colour >> 8 & 255) / 255F, (colour & 255) / 255F);
        // FXVent:59-69. This caller's direction is nonzero, including the
        // source TileTube quirk that repeats fx for the Z component.
        float length = Mth.sqrt((float)(dx*dx + dy*dy + dz*dz));
        xd = heading(dx / length);
        yd = heading(dy / length);
        zd = heading(dz / length);
    }

    private double heading(double unit) {
        return (unit + random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * .0075F * 5) * .125F;
    }

    public static void emit(Level world, BlockPos pos, int colour) {
        if (!(world instanceof ClientLevel level)) return;
        var camera = Minecraft.getInstance().getCameraEntity();
        int range = Minecraft.useFancyGraphics() ? 50 : 25;
        if (camera == null || camera.distanceToSqr(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5) > range*range) return;
        var tile = level.getBlockEntity(pos);
        var direction = new java.util.Random((tile == null ? pos.hashCode() : tile.hashCode()) * 4);
        float pitch = direction.nextFloat() * 360, yaw = direction.nextFloat() * 360;
        double fx = -Mth.sin(yaw / 180F * Mth.PI) * Mth.cos(pitch / 180F * Mth.PI);
        double fy = -Mth.sin(pitch / 180F * Mth.PI);
        // TileTube:129 passes fx,fy,fx, not fx,fy,fz. Preserve that quirk.
        Minecraft.getInstance().particleEngine.add(new FXVent(level, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5,
                fx/5, fy/5, fx/5, colour));
    }

    /**
     * Original TileThaumatorium:221-230 - the vent of the machine, with the position and the
     * direction of the original call. FXVent:33-46 keeps the original distance cull.
     */
    public static void emit(Level world, double x, double y, double z, double dx, double dy, double dz, int colour) {
        if (!(world instanceof ClientLevel level)) return;
        var camera = Minecraft.getInstance().getCameraEntity();
        int range = Minecraft.useFancyGraphics() ? 50 : 25;
        if (camera == null || camera.distanceToSqr(x, y, z) > range * range) return;
        Minecraft.getInstance().particleEngine.add(new FXVent(level, x, y, z, dx, dy, dz, colour));
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        age++;
        if (scale > 1) remove();
        yd += .0025;
        move(xd, yd, zd);
        xd *= .8500000190734863; yd *= .8500000190734863; zd *= .8500000190734863;
        if (scale < 1) scale *= 1.15F;
        if (onGround) { xd *= .7F; zd *= .7F; }
    }

    @Override public float getQuadSize(float partialTick) { return .3F * scale; }
    @Override protected float getU0() { return (int)(1 + scale * 4) % 16 / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return (int)(1 + scale * 4) / 16 / 16F; }
    @Override protected float getV1() { return getV0() + .0624375F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, ResourceParticle.TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            alpha = .4F * Math.max(0, 1-scale);
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
