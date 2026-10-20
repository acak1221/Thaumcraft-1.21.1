package thaumcraft.client;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.particles.HungryNodeFragment;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.research.HungryNodeRules;
import thaumcraft.common.lib.world.HungryNodeTrace;
import thaumcraft.common.tiles.TileNode;

/** Original Hungry First client branch and player/entity motion prediction; never changes aura. */
public final class ThaumcraftHungryNodeClient {
    private static boolean initialized;
    private ThaumcraftHungryNodeClient() {}
    public static void init() { if (!initialized) { initialized = true; TileNode.setHungryClientTick(ThaumcraftHungryNodeClient::tick); } }
    private static void tick(TileNode node) {
        Minecraft client = Minecraft.getInstance();
        if (!(node.getLevel() instanceof ClientLevel level) || client.level != level || !level.hasChunkAt(node.getBlockPos())
                || node.isRemoved() || level.getBlockEntity(node.getBlockPos()) != node) return;
        Vec3 centre = Vec3.atCenterOf(node.getBlockPos());
        var setting = client.options.particles().get();
        int count = setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? 1 : 2;
        int visible = client.options.graphicsMode().get() == GraphicsStatus.FAST ? 32 : 64;
        for (int particle = 0; particle < count; particle++) {
            var hit = HungryNodeTrace.randomHit(level,node.getBlockPos(),level.random);
            if (hit == null || node.getBlockPos().distSqr(hit.getBlockPos()) >= 256D) continue;
            var source = hit.getBlockPos(); var state = level.getBlockState(source);
            if (state.isAir() || client.getCameraEntity() == null || client.getCameraEntity().distanceToSqr(Vec3.atCenterOf(source)) > visible * visible) continue;
            client.particleEngine.add(new HungryNodeFragment(level,node,source,state));
        }
        if (ThaumcraftSettings.current().hardNodes()) {
            for (Entity entity : level.getEntitiesOfClass(Entity.class,new AABB(node.getBlockPos()).inflate(15))) {
                if (entity.isRemoved() || entity instanceof Player player && player.getAbilities().invulnerable) continue;
                Vec3 motion = entity.getDeltaMovement().add(HungryNodeRules.pull(centre.subtract(entity.position())));
                if (Double.isFinite(motion.x) && Double.isFinite(motion.y) && Double.isFinite(motion.z)) entity.setDeltaMovement(motion);
            }
        }
    }
}
