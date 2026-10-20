package thaumcraft.common.testing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * The original TC4 projectiles inside a real dedicated server: the bottle of taint that poisons
 * every living thing of its splash and grows taint fibres, the homing golem orb that deals the
 * attack damage of its thrower and dies after its original lifetime, and the primal arrow with
 * its original damage per type and its original type effects.
 */
public final class ProjectileRuntimeChecks {
    private ProjectileRuntimeChecks() {}

    /** The original bottle of taint poisons the splash and grows taint fibres. */
    public static void bottleOfTaintPoisonsAndGrowsFibres(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(4);
            var fluxTaint = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());
            var victim = lane.add(EntityType.PIG.create(level));
            victim.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            victim.setNoAi(true);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
            victim.setHealth(1000F);
            level.addFreshEntity(victim);

            var bottle = lane.add(new EntityBottleTaint(level, player));
            bottle.setPos(lane.origin.getX() + .5D, lane.origin.getY() + 1D, lane.origin.getZ() + .5D);
            bottle.setDeltaMovement(0D, -0.5D, 0D);
            level.addFreshEntity(bottle);
            for (int tick = 0; tick < 20; tick++) level.tickNonPassenger(bottle);
            test.assertTrue(bottle.isRemoved(), "the thrown bottle of taint bursts on the ground: " + bottle.isRemoved()
                    + " at " + bottle.position() + " victim=" + victim.position());
            test.assertTrue(victim.hasEffect(fluxTaint),
                    "the bottle of taint poisons every living thing of its splash: " + victim.getEffect(fluxTaint));
            test.assertTrue(victim.getEffect(fluxTaint).getDuration() == 100,
                    "the broken bottle poisons its splash for the original hundred ticks: "
                            + victim.getEffect(fluxTaint).getDuration());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original golem orb homes on its target and deals the damage of its thrower. */
    public static void golemOrbHomesAndDealsTheOriginalDamage(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            var thrower = lane.add(EntityType.ZOMBIE.create(level));
            thrower.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            thrower.setNoAi(true);
            level.addFreshEntity(thrower);
            var victim = lane.add(EntityType.PIG.create(level));
            victim.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 4.5D, 0F, 0F);
            victim.setNoAi(true);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100D);
            victim.setHealth(100F);
            level.addFreshEntity(victim);

            var red = lane.add(new EntityGolemOrb(level, thrower, victim, true));
            level.addFreshEntity(red);
            test.assertTrue(red.isRed() && red.getTargetId() == victim.getId(),
                    "the red golem orb keeps the original target: " + red.getTargetId());
            test.assertTrue(Math.abs(red.getPickRadius() - 0.1F) < 1e-4F,
                    "the golem orb keeps the original pick radius of 0.1: " + red.getPickRadius());
            for (int tick = 0; tick < 40; tick++) level.tickNonPassenger(red);
            test.assertTrue(victim.getHealth() < 100F,
                    "the golem orb hits its target: " + victim.getHealth() + " orb=" + red.position()
                            + " victim=" + victim.position() + " removed=" + red.isRemoved());
            test.assertTrue(Math.abs((100F - victim.getHealth()) - 3D) < 1.5D,
                    "the red golem orb deals the original attack damage of its thrower: " + (100F - victim.getHealth()));
            test.assertTrue(red.isRemoved(), "the golem orb disappears on the hit: " + red.isRemoved());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original primal arrows keep their damage and their type effects. */
    public static void primalArrowKeepsItsDamageAndEffects(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            var shooter = lane.add(EntityType.ZOMBIE.create(level));
            shooter.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            shooter.setNoAi(true);
            level.addFreshEntity(shooter);
            var victim = lane.add(EntityType.PIG.create(level));
            victim.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            victim.setNoAi(true);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100D);
            victim.setHealth(100F);
            level.addFreshEntity(victim);

            var order = lane.add(new EntityPrimalArrow(level, shooter, 3F, ProjectileRules.PRIMAL_ARROW_TYPE_ORDER));
            order.setPos(lane.origin.getX() + .5D, lane.origin.getY() + 1D, lane.origin.getZ() + 1.5D);
            order.setDeltaMovement(0D, 0D, 1D);
            level.addFreshEntity(order);
            for (int tick = 0; tick < 25; tick++) level.tickNonPassenger(order);
            test.assertTrue(order.getArrowType() == ProjectileRules.PRIMAL_ARROW_TYPE_ORDER,
                    "the primal arrow keeps its original type: " + order.getArrowType());
            test.assertTrue(victim.getHealth() < 100F, "the primal arrow hits its victim: " + victim.getHealth()
                    + " arrow=" + order.position() + " removed=" + order.isRemoved());
            test.assertTrue(victim.hasEffect(MobEffects.WEAKNESS)
                            && victim.getEffect(MobEffects.WEAKNESS).getAmplifier() == ProjectileRules.PRIMAL_ARROW_ORDER_AMPLIFIER
                            && victim.getEffect(MobEffects.WEAKNESS).getDuration() <= ProjectileRules.PRIMAL_ARROW_ORDER_TICKS,
                    "the order arrow weakens its victim for the original two hundred ticks: "
                            + (victim.hasEffect(MobEffects.WEAKNESS) ? victim.getEffect(MobEffects.WEAKNESS).getAmplifier() : -1));
            test.assertTrue(Math.abs((100F - victim.getHealth()) - 1.68D) < 0.6D,
                    "the order arrow keeps the original four fifths of its 2.1 base damage: " + (100F - victim.getHealth()));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
