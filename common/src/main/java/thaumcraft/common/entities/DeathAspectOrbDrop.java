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
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.entities.DeathAspectOrbRules;
import thaumcraft.common.lib.research.EntityAspectBindings;

/**
 * The original death aspect orbs.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerEntity.java:508-516} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}). The original handles the death of a
 * living entity in two branches: a mob that carries the taint poison and that is not a tainted
 * mob is replaced by its tainted twin, and every other mob that was recently hit drops one aspect
 * orb per primal aspect of its scanned aspects.</p>
 *
 * <p>Forced 1.21.1 changes:</p>
 * <ul>
 *   <li>The original reads the private twenty tick recently-hit flag of the 1.7
 *   {@code EntityLivingBase} ({@code EntityUtils.getRecentlyHit}). Vanilla 1.7.10 only sets that
 *   flag when a player hurts the mob (the Forge 1.7.10 patch of {@code EntityLivingBase.attackEntityFrom}),
 *   and the native equivalent of that flag is the 1.21.1 {@code lastHurtByPlayerTime}, which the
 *   native taint swarm, the native fire bat and the native golem already use for the original
 *   {@code EntityUtils.setRecentlyHit} calls.</li>
 *   <li>The original scans the aspects of the dead mob with
 *   {@code ScanManager.generateEntityAspects}; the native port uses the same aspect bindings
 *   ({@code EntityAspectBindings.describe}).</li>
 *   <li>The original spawns the orb with {@code world.spawnEntityInWorld}; the native port uses
 *   the same guarded orb factory as the node emission, so an invalid orb can never be published.</li>
 * </ul>
 *
 * <p>The order of the world random is the original one, and it is proven:</p>
 * <ul>
 *   <li>The Forge 1.7.10 patch of {@code EntityLivingBase.onDeath} starts with
 *   {@code ForgeHooks.onLivingDeath(this, source)}, which fires the {@code LivingDeathEvent} of
 *   the original, and only afterwards captures the drops and fires
 *   {@code ForgeHooks.onLivingDrops} (the Forge 1.7.10 patch of
 *   {@code net/minecraft/entity/EntityLivingBase.java}, the hunks at {@code @@ -883,6 +888,7 @@},
 *   {@code @@ -908,6 +914,10 @@} and {@code @@ -923,6 +933,16 @@}). The two rolls of an aspect
 *   therefore happen BEFORE the loot of the same death, exactly like the original.</li>
 *   <li>The native {@code EntityEvent.LIVING_DEATH} of Architectury is injected at the head of the
 *   1.21.1 {@code LivingEntity.die} (the {@code dev.architectury.mixin.fabric.LivingDeathInvoker}
 *   of the Architectury fabric module), and the native {@code dropAllDeathLoot} is called inside
 *   that same method, so the native death loot follows the native orbs.</li>
 * </ul>
 * <p>The consequence is the original one: the world random of a death that a player caused is
 * shifted by the aspect rolls of the dead mob, and the loot of that death therefore draws the
 * values the original would have drawn after the orbs. The GameTests of the lanes that lock the
 * world random stream of a death declare the orb rolls of their fixture explicitly.</p>
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

    /** Original {@code EventHandlerEntity:508-516}: the aspect orbs of a dead mob. */
    private static EventResult onLivingDeath(Entity entity, DamageSource source) {
        if (!(entity instanceof LivingEntity living)) return EventResult.pass();
        if (!(living.level() instanceof ServerLevel server)) return EventResult.pass();
        // The original branches: a mob that carries the taint poison and that is not a tainted mob
        // is converted into its tainted twin and never reaches the aspect orb branch.
        boolean fluxTainted = living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()));
        if (fluxTainted && !(living instanceof ITaintedMob)) return EventResult.pass();
        // The original recently-hit flag of the 1.7 EntityLivingBase.
        if (living.lastHurtByPlayerTime <= 0) return EventResult.pass();
        var target = EntityAspectBindings.describe(living);
        if (target == null || target.aspects() == null || target.aspects().size() == 0) return EventResult.pass();
        // The original draws the two rolls of every aspect from the WORLD random.
        var drops = DeathAspectOrbRules.drops(target.aspects(), () -> server.getRandom().nextBoolean(),
                bound -> server.getRandom().nextInt(bound));
        for (var drop : drops) {
            // Original EntityAspectOrb:57-68 - the scatter of the orb uses the global random.
            float yaw = (float) (Math.random() * 360D);
            var motion = new Vec3((float) (Math.random() * .2D - .1D) * 2F, (float) (Math.random() * .2D) * 2F,
                    (float) (Math.random() * .2D - .1D) * 2F);
            try {
                var orb = EntityAspectOrb.unspawned(server, living.position(), motion, yaw, drop.aspect(), drop.value());
                if (orb != null) server.addFreshEntity(orb);
            } catch (RuntimeException failure) {
                // A death must never take the server down with it.
                Thaumcraft.LOG.warn("Death aspect orb could not be published for {}", drop.aspect().getTag(), failure);
            }
        }
        return EventResult.pass();
    }
}
