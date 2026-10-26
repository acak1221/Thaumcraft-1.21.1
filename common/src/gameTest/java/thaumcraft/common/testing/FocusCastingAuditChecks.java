package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftHellbatFoci;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.config.ThaumcraftPrimalFoci;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.entities.projectile.EntityPechBlast;
import thaumcraft.common.items.wands.foci.WandFocusActions;

/** Real RMB/held-tick launches. No RNG or launch motion replacement in flight measurements. */
public final class FocusCastingAuditChecks {
    private FocusCastingAuditChecks() {}

    public static void launches(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 180, 4))) {
            a.clearLane();
            targetedRefusals(a);
            for (ItemFocusBasic item : List.of(ThaumcraftFoci.FIRE_FOCUS.get(), ThaumcraftFoci.FROST_FOCUS.get(),
                    ThaumcraftPech.FOCUS_PECH.get(), ThaumcraftPrimalFoci.FOCUS_PRIMAL.get())) {
                a.equip(new ItemStack(item), "gold", 1000); a.aim();
                a.begin();
                if (item == ThaumcraftFoci.FIRE_FOCUS.get()) a.tick(1);
                a.player.stopUsingItem();
                var shots = a.projectiles(Projectile.class);
                int expected = item == ThaumcraftFoci.FIRE_FOCUS.get() ? 2 : 1;
                a.require(shots.size() == expected, "exactly one native paid cast, canonical output count " + item);
                measure(a, item.toString(), shots);
                float health = a.player.getHealth();
                for (var shot : shots) {
                    Vec3 origin = shot.position();
                    double yaw = a.player.getYRot() / 180 * (float)Math.PI;
                    Vec3 expectedOrigin = a.player.getEyePosition().add(-Math.cos(yaw) * .16F, -(double).1F, -Math.sin(yaw) * .16F);
                    // Original Fire advances its muzzle by the FINAL scattered velocity before insertion.
                    if (shot instanceof EntityEmber) expectedOrigin = expectedOrigin.add(shot.getDeltaMovement());
                    a.require(origin.distanceTo(expectedOrigin) < .000001, "actual original eye/muzzle launch origin");
                    a.require(shot.getDeltaMovement().dot(a.player.getLookAngle()) > .3, "actual velocity points forwards");
                    double speed = shot.getDeltaMovement().length();
                    a.require(shot instanceof EntityEmber ? speed > .5 && speed < 1.6
                                    : item == ThaumcraftPrimalFoci.FOCUS_PRIMAL.get() ? speed > .45 && speed < .55
                                    : speed > 1.35 && speed < 1.65,
                            "actual native launch speed remains within original Gaussian envelope");
                    shot.tick();
                    a.require(!shot.isRemoved() && shot.position().distanceTo(origin) > .3,
                            "first actual flight tick leaves caster without immediate impact");
                }
                a.require(a.player.getHealth() == health, "actual first flight ticks never damage caster");
                a.purgeOutputs();
                a.equip(new ItemStack(item), "gold", 0); ItemStack empty = a.wand.copy();
                a.begin(); if (item == ThaumcraftFoci.FIRE_FOCUS.get()) a.tick(1);
                a.player.stopUsingItem();
                a.require(a.projectiles(Projectile.class).isEmpty() && ItemStack.matches(empty, a.wand),
                        "empty real wand emits nothing and preserves every component " + item);
            }
            // Measure actual sample spread rather than asserting an exposed scatter constant.
            for (boolean beam : new boolean[]{false, true}) {
                a.equip(beam ? a.focus(ThaumcraftFoci.FIRE_FOCUS.get(), FocusUpgradeType.firebeam, 3)
                        : new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()), "gold", 1000);
                a.begin(); a.tick(32); a.player.stopUsingItem();
                var shots = a.projectiles(EntityEmber.class);
                a.require(shots.size() == 64, "32 actual paid channel ticks produce exactly 64 embers");
                double rms = measure(a, beam ? "firebeam-sample" : "fire-sample", shots);
                a.require(beam ? rms < 1.5 : rms > 3 && rms < 25, "measured narrow beam / broad basic fire spread");
                a.purgeOutputs();
            }
            a.equip(a.focus(ThaumcraftFoci.FROST_FOCUS.get(), FocusUpgradeType.potency, 1,
                    FocusUpgradeType.potency, 2, FocusUpgradeType.scattershot, 3), "gold", 1000);
            a.begin(); var scatter = a.projectiles(EntityFrostShard.class);
            a.require(scatter.size() == 9, "one actual P2 scatter cast produces nine shards");
            a.require(measure(a, "frost-scatter-P2", scatter) > 1, "actual scatter directions diverge");
            var paid = a.wand.copy();
            a.player.gameMode.useItem(a.player, a.level, a.wand, InteractionHand.MAIN_HAND);
            a.require(a.projectiles(EntityFrostShard.class).size() == 9 && ItemStack.matches(paid, a.wand),
                    "immediate scatter RMB replay cannot cast or debit twice");
            try (var peer = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 160, 4))) {
                // The test's own chunk column: a fresh chunk 32 blocks away made the peer's shard depend on the
                // asynchronous entity section load (found nowhere in one environment, everywhere in another).
                peer.clearLane(); peer.equip(new ItemStack(ThaumcraftFoci.FROST_FOCUS.get()), "gold", 1000);
                // equip clears ONLY peer's cooldown, not the first actor's.
                a.require(WandFocusActions.cooldownMillis(a.player) > 0, "first player still cooling down");
                peer.begin();
                var peerShards = peer.projectiles(EntityFrostShard.class);
                peer.require(peerShards.size() == 1,
                        "second player in same real world casts during first player's cooldown: " + peerShards.size()
                                + " shards, peer cooldown " + WandFocusActions.cooldownMillis(peer.player) + " ms");
            }
        }
        test.succeed();
    }

    private static void targetedRefusals(FocusUpgradeRuntimeChecks.Actor a) {
        for (ItemFocusBasic item : List.of(ThaumcraftFoci.EXCAVATION_FOCUS.get(), ThaumcraftFoci.WARDING_FOCUS.get(),
                ThaumcraftFoci.PORTABLE_HOLE_FOCUS.get(), ThaumcraftFoci.TRADE_FOCUS.get(), ThaumcraftHellbatFoci.HELLBAT_FOCUS.get())) {
            a.equip(new ItemStack(item), "gold", 1000); a.aim(); ItemStack before = a.wand.copy();
            a.begin(); if (item == ThaumcraftFoci.EXCAVATION_FOCUS.get()) a.tick(1);
            a.player.stopUsingItem();
            a.require(ItemStack.matches(before, a.wand) && a.projectiles(Projectile.class).isEmpty(),
                    "actual targeted focus cast into empty sky spends nothing " + item);
            Thaumcraft.LOG.info("G31_CAST {} no-target debit=0 projectileCount=0", item);
        }
    }

    private static double measure(FocusUpgradeRuntimeChecks.Actor a, String name, List<? extends Projectile> shots) {
        double sum = 0, min = Double.POSITIVE_INFINITY, max = 0;
        for (var shot : shots) {
            var velocity = shot.getDeltaMovement();
            double angle = Math.toDegrees(Math.acos(Math.clamp(velocity.normalize().dot(a.player.getLookAngle()), -1, 1)));
            sum += angle * angle; min = Math.min(min, velocity.length()); max = Math.max(max, velocity.length());
        }
        double rms = Math.sqrt(sum / shots.size());
        Thaumcraft.LOG.info("G31_CAST {} count={} speed=[{},{}] angularRmsDegrees={}", name, shots.size(), min, max, rms);
        return rms;
    }

    public static void pechImpactReentry(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 180, 4))) {
            a.clearLane(); a.equip(new ItemStack(ThaumcraftPech.FOCUS_PECH.get()), "gold", 1000); a.begin();
            var shot = a.projectiles(EntityPechBlast.class).getFirst();
            var cow = EntityType.COW.create(a.level); a.require(cow != null, "native target exists");
            cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(Vec3.atCenterOf(a.root));
            a.require(a.level.addFreshEntity(cow), "native target enters world index");
            // Put target on the measured, unmodified real launch ray, close enough for splash.
            cow.setPos(shot.position().add(shot.getDeltaMovement().normalize().scale(1.5)).add(0, -.5, 0));
            int[] attempts = {0};
            EntityEvent.LivingHurt observe = (victim, source, amount) -> {
                if (victim == cow && source.getDirectEntity() == shot) {
                    attempts[0]++;
                    a.require(amount == 2, "base curse damage amount is exactly two");
                    if (attempts[0] == 1) shot.tick(); // Native extension callback reenters the same projectile once.
                }
                return EventResult.pass();
            };
            EntityEvent.LIVING_HURT.register(observe);
            try {
                shot.tick(); shot.tick();
                Thaumcraft.LOG.info("G31_CAST pech-impact-reentry damageAttempts={} healthLost={}", attempts[0], cow.getMaxHealth() - cow.getHealth());
                a.require(shot.isRemoved() && attempts[0] == 1 && cow.getHealth() == cow.getMaxHealth() - 2
                                && cow.getActiveEffects().size() == 1,
                        "one genuinely cast curse impact must attempt damage exactly once despite native callback reentry");
            } finally { EntityEvent.LIVING_HURT.unregister(observe); cow.discard(); }
        }
        test.succeed();
    }
}
