package thaumcraft.client.fx.particles;

import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The original {@code FXBreaking} of the thaumic slime jump (oracle
 * {@code thaumcraft/client/ClientProxy.java:1024-1035}, {@code slimeJumpFX(Entity, int)}): the slime ball
 * quarter icon in the thaumic purple {@code 0.7f, 0.0f, 1.0f}, the particle scale {@code 0.4f} and the
 * lifetime {@code (int)(66.0f / (rand.nextFloat() * 0.9f + 0.1f))}.
 */
public final class SlimeJumpParticle extends BreakingItemParticle {
    private SlimeJumpParticle(net.minecraft.client.multiplayer.ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, new ItemStack(Items.SLIME_BALL));
    }

    /**
     * The original jump burst: {@code count} fragments on the ring of the radius {@code size * 0.5f * radius}.
     */
    public static void emit(Entity target, int count, int size) {
        if (!(target.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) return;
        for (int i = 0; i < count; i++) {
            float angle = level.random.nextFloat() * (float) Math.PI * 2F;
            float radius = level.random.nextFloat() * .5F + .5F;
            float dx = Mth.sin(angle) * size * .5F * radius;
            float dz = Mth.cos(angle) * size * .5F * radius;
            var particle = new SlimeJumpParticle(level, target.getX() + dx,
                    (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2, target.getZ() + dz);
            particle.setColor(.7F, 0, 1F);
            particle.scale(.4F);
            particle.lifetime = (int) (66F / (level.random.nextFloat() * .9F + .1F));
            net.minecraft.client.Minecraft.getInstance().particleEngine.add(particle);
        }
    }
}
