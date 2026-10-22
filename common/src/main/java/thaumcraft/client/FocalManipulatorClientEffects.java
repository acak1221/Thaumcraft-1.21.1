package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.ref.WeakReference;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.tiles.TileFocalManipulator;

/** Tick-only, native ParticleEngine adaptation of the original tile's one
 * stationary FXGeneric per active CLIENT tick (atlas112, nine frames). No BER
 * emission, economic state, alternate particle manager or audio synthesis. */
public final class FocalManipulatorClientEffects implements TileFocalManipulator.ClientEffects {
    @Override public void tick(Level world, BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (!(world instanceof ClientLevel level) || client.level != level || !level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof TileFocalManipulator tile)
                || tile.isRemoved() || tile.isQuarantined() || tile.getSize() <= 0) return;
        var random = level.random;
        double x = pos.getX() + .5 + (random.nextFloat() - random.nextFloat()) * .3F;
        double y = pos.getY() + 1.25 + (random.nextFloat() - random.nextFloat()) * .3F;
        double z = pos.getZ() + .5 + (random.nextFloat() - random.nextFloat()) * .3F;
        float red = .5F + random.nextFloat() * .4F;
        float green = 1 - random.nextFloat() * .4F;
        float blue = 1 - random.nextFloat() * .4F;
        int life = 6 + random.nextInt(5);
        float scale = .7F + random.nextFloat() * .4F;
        client.particleEngine.add(new CraftSparkle(level, tile, x, y, z, red, green, blue, life, scale));
    }

    private static final class CraftSparkle extends Particle implements thaumcraft.client.fx.Tc4Particle {
        private final WeakReference<TileFocalManipulator> origin;
        private final BlockPos source;
        private final float halfSize;
        private final int color;

        CraftSparkle(ClientLevel level, TileFocalManipulator tile, double x, double y, double z,
                     float red, float green, float blue, int life, float scale) {
            super(level, x, y, z, 0, 0, 0);
            origin = new WeakReference<>(tile);
            source = tile.getBlockPos().immutable();
            lifetime = life;
            halfSize = .1F * scale;
            color = (int)(red * 255) << 16 | (int)(green * 255) << 8 | (int)(blue * 255);
            hasPhysics = false;
            xd = yd = zd = 0;
            setSize(.1F, .1F);
        }

        @Override public void tick() {
            xo = x; yo = y; zo = z;
            TileFocalManipulator tile = origin.get();
            if (Minecraft.getInstance().level != level || age++ >= lifetime || !level.hasChunkAt(source)
                    || tile == null || tile.isRemoved() || tile.getLevel() != level || level.getBlockEntity(source) != tile) remove();
        }

        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

        @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
            PoseStack pose = new PoseStack();
            pose.translate(Mth.lerp(partialTick, xo, x) - camera.getPosition().x,
                    Mth.lerp(partialTick, yo, y) - camera.getPosition().y,
                    Mth.lerp(partialTick, zo, z) - camera.getPosition().z);
            pose.mulPose(camera.rotation());
            int frame = 112 + Math.min(8, age * 9 / lifetime);
            float u = frame % 16 / 16F, v = frame / 16 / 16F;
            float opacity = age <= 1 || age >= lifetime - 1 ? .4F : .8F;
            // Existing native VisNetRender owns exact shader/blend/depth state
            // restoration and a private mesh, safe inside CUSTOM particle pass.
            VisNetRender.draw(VisNetRender.PARTICLES, GL11.GL_ONE, false, out ->
                    VisNetRender.quad(out, pose, halfSize, opacity, color, u, u + .0624375F, v, v + .0624375F));
        }
    }
}
