package thaumcraft.common.entities;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.entities.DeathAspectOrbRules;
import thaumcraft.common.lib.research.EntityAspectBindings;

/**
 * The original TC4 death aspect orbs.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java:534-546}. The original
 * handles the death of a living entity in two branches: a mob that carries the taint poison and
 * that is not a tainted mob is replaced by its tainted twin, and every other mob that was
 * recently hit drops one aspect orb per primal aspect of its scanned aspects.</p>
 *
 * <p>Forced 1.21.1 changes:</p>
 * <ul>
 *   <li>The original reads the private twenty tick recently-hit flag of the 1.7
 *   {@code EntityLivingBase} ({@code EntityUtils.getRecentlyHit}). The 1.21.1 damage cooldown of
 *   the mob carries the same window: it is set to twenty ticks on every damage and counted down
 *   every tick, so the killing blow itself is inside the window, exactly like the original.</li>
 *   <li>The original scans the aspects of the dead mob with
 *   {@code ScanManager.generateEntityAspects}; the native port uses the same aspect bindings
 *   ({@code EntityAspectBindings.describe}).</li>
 *   <li>The original spawns the orb with {@code world.spawnEntityInWorld}; the native port uses
 *   the same guarded orb factory as the node emission, so an invalid orb can never be published.</li>
 * </ul>
 */
public final class DeathAspectOrbDrop {
    private static boolean registered;

    private DeathAspectOrbDrop() {}

    /** Registers the original living death event of the aspect orbs. */
    public static void register() {
        if (registered) return;
        registered = true;
        EntityEvent.LIVING_DEATH.register(DeathAspectOrbDrop::onLivingDeath);
    }

    /** Original {@code EventHandlerEntity:534-546}: the aspect orbs of a dead mob. */
    private static EventResult onLivingDeath(Entity entity, DamageSource source) {
        if (!(entity instanceof LivingEntity living)) return EventResult.pass();
        if (!(living.level() instanceof ServerLevel server)) return EventResult.pass();
        // The original branches: a mob that carries the taint poison and that is not a tainted mob
        // is converted into its tainted twin and never reaches the aspect orb branch.
        boolean fluxTainted = living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()));
        if (fluxTainted && !(living instanceof ITaintedMob)) return EventResult.pass();
        // The original recently-hit flag of the 1.7 EntityLivingBase.
        if (living.invulnerableTime <= 0) return EventResult.pass();
        var target = EntityAspectBindings.describe(living);
        if (target == null || target.aspects() == null || target.aspects().size() == 0) return EventResult.pass();
        var drops = DeathAspectOrbRules.drops(target.aspects(), () -> server.getRandom().nextBoolean(),
                bound -> server.getRandom().nextInt(bound));
        for (var drop : drops) {
            // Original EntityAspectOrb:36-47 - the scatter of the orb uses the global random.
            float yaw = (float) (Math.random() * 360D);
            var motion = new Vec3((float) (Math.random() * .2D - .1D) * 2F, (float) (Math.random() * .2D) * 2F,
                    (float) (Math.random() * .2D - .1D) * 2F);
            var orb = EntityAspectOrb.unspawned(server, living.position(), motion, yaw, drop.aspect(), drop.value());
            if (orb != null) server.addFreshEntity(orb);
        }
        return EventResult.pass();
    }
}
