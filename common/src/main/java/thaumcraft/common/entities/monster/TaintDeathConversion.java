package thaumcraft.common.entities.monster;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.lib.taint.TaintDeathRules;
import thaumcraft.common.lib.taint.TaintDeathRules.TaintedTwin;

/**
 * The original TC4 taint death conversion of the mob that is not one of the tainted farm animals.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java:480-507}. The original converted every
 * living entity that died while the taint poison was active: a creeper became a taint creeper, the five farm
 * animals became their tainted counterparts and every other mob became a thaumic slime whose size came from the
 * max health of the dead mob.</p>
 *
 * <p>The native port splits the original handler in two: the tainted animal layer already converts the five farm
 * animals ({@code ThaumcraftTaintAnimals.onLivingDeath}), so this layer owns the taint creeper and the thaumic
 * slime. The two layers never convert the same mob, because this one skips the farm animals.</p>
 *
 * <p>Forced 1.21.1 change: the original called {@code setDead()} on the dying mob, because it replaced the mob
 * with its twin. The native port lets the mob die as it would and only adds the twin, exactly like the tainted
 * animal layer does, so the original loot of the dead mob is still dropped.</p>
 */
public final class TaintDeathConversion {
    private static boolean registered;

    private TaintDeathConversion() {}

    /** Registers the original living death event of the taint conversion. */
    public static void register() {
        if (registered) return;
        registered = true;
        EntityEvent.LIVING_DEATH.register(TaintDeathConversion::onLivingDeath);
    }

    /** Original {@code EventHandlerEntity:481-507}: the conversion of a dying mob. */
    private static EventResult onLivingDeath(Entity entity, DamageSource source) {
        if (!(entity instanceof LivingEntity living)) return EventResult.pass();
        if (!(living.level() instanceof ServerLevel server)) return EventResult.pass();
        boolean fluxTainted = living.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()));
        if (!TaintDeathRules.converts(true, fluxTainted, living instanceof ITaintedMob)) return EventResult.pass();
        String entityId = EntityType.getKey(living.getType()).toString();
        // The tainted animal layer owns the five farm animals of the original table.
        if (TaintDeathRules.animalTwin(entityId)) return EventResult.pass();
        TaintedTwin twin = TaintDeathRules.twin(entityId);
        if (twin == TaintedTwin.CREEPER) {
            EntityTaintCreeper creeper = new EntityTaintCreeper(server);
            creeper.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), 0.0F);
            server.addFreshEntity(creeper);
            return EventResult.pass();
        }
        if (twin == TaintedTwin.SLIME) {
            EntityThaumicSlime slime = new EntityThaumicSlime(ThaumcraftEldritchMobs.THAUMIC_SLIME.get(), server);
            slime.setSlimeSize(TaintDeathRules.slimeSize(living.getMaxHealth()));
            slime.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), 0.0F);
            server.addFreshEntity(slime);
        }
        return EventResult.pass();
    }
}
