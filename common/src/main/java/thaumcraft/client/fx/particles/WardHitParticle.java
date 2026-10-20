package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;

/** Original FXBlockWard: fixed-face, randomly rotated hemis0..15 animation,12–16ticks and the original rise/fade profile. */
public final class WardHitParticle extends Particle {
    private static final ResourceLocation[] FRAMES = new ResourceLocation[16];
    static { for (int frame = 0; frame < FRAMES.length; frame++) FRAMES[frame] = VisNetRender.texture("models/hemis" + frame); }
    private final Direction side;
    private final int rotation;
    private final float scale, sx, sy, sz;
    public WardHitParticle(ClientLevel level, BlockPos pos, Direction side, float fx, float fy, float fz) {
        super(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        this.side = side;
        lifetime = 12 + random.nextInt(5);
        scale = (float) (1.4 + random.nextGaussian() * .3);
        rotation = random.nextInt(360);
        sx = side.getStepX() == 0 ? Mth.clamp(fx - .6F + random.nextFloat() * .2F, -.4F, .4F) : 0;
        sy = side.getStepY() == 0 ? Mth.clamp(fy - .6F + random.nextFloat() * .2F, -.4F, .4F) : 0;
        sz = side.getStepZ() == 0 ? Mth.clamp(fz - .6F + random.nextFloat() * .2F, -.4F, .4F) : 0;
        hasPhysics = false; gravity = 0; xd = yd = zd = 0; alpha = 0;
        setSize(.01F, .01F);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        float rise = lifetime / 5F;
        alpha = age <= rise ? age / rise : (lifetime - age) / (float) lifetime;
        if (age++ >= lifetime) remove();
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        int frame = Math.min(15, (int) (15F * (age + partialTick) / lifetime));
        var pose = new PoseStack();
        var eye = camera.getPosition();
        pose.translate(x - eye.x + sx, y - eye.y + sy, z - eye.z + sz);
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.PI / 2F, side.getStepY(), -side.getStepX(), side.getStepZ()));
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        if (side.getStepZ() > 0) { pose.translate(0, 0, .505); pose.mulPose(Axis.YN.rotationDegrees(180)); }
        else pose.translate(0, 0, -.505);
        VisNetRender.draw(FRAMES[frame], GL11.GL_ONE, false, out -> {
            float half = scale * .5F;
            point(out, pose, -half, half, 0, 1);
            point(out, pose, half, half, 1, 1);
            point(out, pose, half, -half, 1, 0);
            point(out, pose, -half, -half, 0, 0);
        });
    }
    private void point(VertexConsumer out, PoseStack pose, float x, float y, float u, float v) {
        out.addVertex(pose.last(), x, y, 0).setUv(u, v).setColor(rCol, gCol, bCol, alpha / 2F);
    }
}
