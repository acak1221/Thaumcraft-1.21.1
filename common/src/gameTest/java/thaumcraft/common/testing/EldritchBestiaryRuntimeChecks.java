package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWatcher;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * The original TC4 eldritch bestiary inside a real dedicated server: the registered numbers of
 * the warden, the golem, the crab, the watcher, the thaumic slime and the inhabited zombie, the
 * thirty five damage cap and the two hundred tick enrage of the shared boss base, the six
 * six percent ward of the warden, the beheading of the golem, the three quarters melee, the
 * thorns of the watcher, the split of the slime and the crab the inhabited zombie dies into.
 */
public final class EldritchBestiaryRuntimeChecks {
    private EldritchBestiaryRuntimeChecks() {}

    /** The original boss base caps a single hit at thirty five and enrages for two hundred ticks. */
    public static void wardenDamageCapAndEnrage(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            lane.floor(4);

            var heavy = lane.add(ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get().create(level));
            var plain = lane.add(ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get().create(level));
            heavy.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            plain.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(heavy);
            level.addFreshEntity(plain);

            test.assertTrue(heavy.getMaxHealth() == 200F && heavy.getAttributeValue(Attributes.ATTACK_DAMAGE) == 10F
                            && Math.abs(heavy.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.33D) < 1e-6D && heavy.getArmorValue() == 4
                            && heavy.getAttributeValue(Attributes.FOLLOW_RANGE) == 40F
                            && Math.abs(heavy.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.95D) < 1e-6D,
                    "the warden keeps the original 200 health, 10 damage, 0.33 speed, 4 armour, 40 follow range and 0.95 knockback resistance: "
                            + heavy.getMaxHealth() + " " + heavy.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + heavy.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + heavy.getArmorValue() + " "
                            + heavy.getAttributeValue(Attributes.FOLLOW_RANGE) + " " + heavy.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            test.assertTrue(heavy.getBbWidth() == 1.5F && heavy.getBbHeight() == 3.5F && Math.abs(heavy.getEyeHeight() - 3.1F) < 1e-4F,
                    "the warden keeps the original 1.5 by 3.5 body and the 3.1 eye height");
            test.assertTrue(heavy.getAmbientSoundInterval() == 500, "the warden talks every 500 ticks: " + heavy.getAmbientSoundInterval());

            // The original boss base caps every incoming hit at thirty five damage.
            heavy.hurt(level.damageSources().playerAttack(player), 100F);
            plain.hurt(level.damageSources().playerAttack(player), 35F);
            float heavyLoss = 200F - heavy.getHealth();
            float plainLoss = 200F - plain.getHealth();
            test.assertTrue(heavyLoss > 0F && Math.abs(heavyLoss - plainLoss) < 1e-4F,
                    "a hundred point hit costs exactly what a thirty five point hit costs: " + heavyLoss + " against " + plainLoss);
            test.assertTrue(heavy.getAnger() == 200, "the heavy hit enrages the boss for 200 ticks: " + heavy.getAnger());
            test.assertTrue(plain.getAnger() == 0, "a hit below the cap does not enrage the boss: " + plain.getAnger());
            test.assertTrue(heavy.hasEffect(MobEffects.REGENERATION) && heavy.getEffect(MobEffects.REGENERATION).getAmplifier() == 6
                            && heavy.hasEffect(MobEffects.DAMAGE_BOOST) && heavy.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 2
                            && heavy.hasEffect(MobEffects.MOVEMENT_SPEED) && heavy.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 2,
                    "the enrage applies the original regeneration six, strength two and speed two for 200 ticks: "
                            + (heavy.hasEffect(MobEffects.REGENERATION) ? heavy.getEffect(MobEffects.REGENERATION).getAmplifier() : -1));

            // The original spawn: a 150 tick spawn timer, a 66 percent ward and one of the fifteen titles.
            var spawned = ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get().create(level);
            spawned.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 4.5D, 0F, 0F);
            spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(lane.origin), MobSpawnType.EVENT, null);
            test.assertTrue(spawned.getSpawnTimer() == 150, "the warden claims a 150 tick spawn timer: " + spawned.getSpawnTimer());
            test.assertTrue(Math.abs(spawned.getAbsorptionAmount() - 132F) < 1e-3F,
                    "the ward of the warden is 66 percent of its 200 health: " + spawned.getAbsorptionAmount());
            test.assertTrue(EldritchMobRules.wardenTitles().contains(spawned.getTitleName()),
                    "the warden takes one of the original fifteen titles: " + spawned.getTitleName());
            test.assertTrue(spawned.isInvulnerableTo(level.damageSources().playerAttack(player)),
                    "the boss is invulnerable while its spawn timer runs");
            test.assertTrue(spawned.getRestrictRadius() == 24F, "the boss claims the original 24 block home: " + spawned.getRestrictRadius());
        } finally {
            lane.clean();
        }
        test.succeed();
    }

    /** The original golem loses its head above its health and hits for three quarters of its damage. */
    public static void golemBeheadingAndMelee(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            lane.floor(4);

            var golem = lane.add(ThaumcraftEldritchMobs.ELDRITCH_GOLEM.get().create(level));
            golem.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(golem);
            test.assertTrue(golem.getMaxHealth() == 250F && golem.getAttributeValue(Attributes.ATTACK_DAMAGE) == 10F
                            && Math.abs(golem.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.30D) < 1e-6D && golem.getArmorValue() == 6
                            && golem.getAttributeValue(Attributes.FOLLOW_RANGE) == 40F,
                    "the golem keeps the original 250 health, 10 damage, 0.30 speed, 6 armour and 40 follow range: "
                            + golem.getMaxHealth() + " " + golem.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + golem.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + golem.getArmorValue());
            test.assertTrue(golem.getBbWidth() == 1.75F && golem.getBbHeight() == 3.5F && Math.abs(golem.getEyeHeight() - 3F) < 1e-4F,
                    "the golem keeps the original 1.75 by 3.5 body and the 3 eye height");
            test.assertTrue(golem.getType().fireImmune(), "the golem is fire immune, as in the original");

            // The original golem loses its head on a hit that exceeds its remaining health.
            golem.setHeadless(true);
            test.assertTrue(Math.abs(golem.getEyeHeight() - 3.33F) < 1e-4F,
                    "the headless golem looks from 3.33: " + golem.getEyeHeight());
            golem.setHeadless(false);
            golem.setHealth(10F);
            boolean beheaded = golem.hurt(level.damageSources().playerAttack(player), 20F);
            test.assertTrue(!beheaded && golem.isHeadless() && Math.abs(golem.getHealth() - 10F) < 1e-4F,
                    "a hit above the remaining health takes the head and is refused: hurt=" + beheaded + " headless=" + golem.isHeadless()
                            + " health=" + golem.getHealth());
            test.assertTrue(golem.getSpawnTimer() == 100, "the headless golem claims a 100 tick spawn timer: " + golem.getSpawnTimer());

            // The original melee is three quarters of the attack damage and repeats every ten ticks.
            var pig = lane.add(EntityType.PIG.create(level));
            pig.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            float before = pig.getHealth();
            boolean hit = golem.doHurtTarget(pig);
            test.assertTrue(hit && Math.abs(before - pig.getHealth() - 7.5F) < 1e-3F,
                    "the golem melee deals three quarters of its ten attack damage: " + (before - pig.getHealth()));
            test.assertTrue(!golem.doHurtTarget(pig), "the second swing inside the ten tick cooldown is refused");
            test.assertTrue(golem.getAttackTimer() == 10, "the melee cooldown is the original ten ticks: " + golem.getAttackTimer());
        } finally {
            lane.clean();
        }
        test.succeed();
    }

    /** The original crab, watcher, slime and inhabited zombie keep their numbers in a real world. */
    public static void crabWatcherSlimeAndZombie(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            lane.floor(4);
            // The slime looks for a player within sixteen blocks, so the player waits far away.
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());

            var crab = lane.add(ThaumcraftEldritchMobs.ELDRITCH_CRAB.get().create(level));
            crab.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(crab);
            test.assertTrue(crab.getMaxHealth() == 20F && crab.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F
                            && Math.abs(crab.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.30D) < 1e-6D
                            && crab.getAttributeValue(Attributes.FOLLOW_RANGE) == 16F && crab.getArmorValue() == 0,
                    "the crab keeps the original 20 health, 4 damage, 0.30 speed and 16 follow range: "
                            + crab.getMaxHealth() + " " + crab.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + crab.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + crab.getAttributeValue(Attributes.FOLLOW_RANGE));
            crab.setHelm(true);
            test.assertTrue(crab.getArmorValue() == 5, "the helm of the crab is worth five armour points: " + crab.getArmorValue());
            crab.hurt(level.damageSources().generic(), 20F);
            test.assertTrue(!crab.hasHelm() && Math.abs(crab.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.30D) < 1e-6D,
                    "the crab loses its helm at half health and keeps the original speed: helm=" + crab.hasHelm() + " speed="
                            + crab.getAttributeValue(Attributes.MOVEMENT_SPEED));

            var watcher = lane.add(ThaumcraftEldritchMobs.WATCHER.get().create(level));
            watcher.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(watcher);
            test.assertTrue(watcher.getMaxHealth() == 30F && watcher.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6F
                            && Math.abs(watcher.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.50D) < 1e-6D
                            && watcher.getAttributeValue(Attributes.FOLLOW_RANGE) == 16F,
                    "the watcher keeps the original 30 health, 6 damage, 0.50 speed and 16 follow range: "
                            + watcher.getMaxHealth() + " " + watcher.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + watcher.getAttributeValue(Attributes.MOVEMENT_SPEED));
            test.assertTrue(watcher.getGazeDuration() == 80, "the gaze of the watcher lasts 80 ticks: " + watcher.getGazeDuration());
            var thorned = lane.add(EntityType.PIG.create(level));
            thorned.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            thorned.setNoAi(true);
            level.addFreshEntity(thorned);
            float thornBefore = thorned.getHealth();
            watcher.hurt(level.damageSources().mobAttack(thorned), 1F);
            test.assertTrue(Math.abs(thornBefore - thorned.getHealth() - 2F) < 1e-4F,
                    "the watcher answers a melee attacker with two points of thorns: " + (thornBefore - thorned.getHealth()));

            var slime = lane.add(ThaumcraftEldritchMobs.THAUMIC_SLIME.get().create(level));
            slime.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 4.5D, 0F, 0F);
            slime.setSlimeSize(4);
            level.addFreshEntity(slime);
            test.assertTrue(slime.getMaxHealth() == 4F && Math.abs(slime.getBbWidth() - 0.75F) < 1e-4F
                            && Math.abs(slime.getBbHeight() - 0.75F) < 1e-4F,
                    "the size four slime has four health and the original 0.75 body: " + slime.getMaxHealth() + " " + slime.getBbWidth());
            slime.hurt(level.damageSources().generic(), 10F);
            test.assertTrue(slime.getHealth() <= 0F, "the slime dies at its own size: " + slime.getHealth());

            int crabsBefore = level.getEntitiesOfClass(EntityEldritchCrab.class, lane.area(8)).size();
            var zombie = lane.add(ThaumcraftEldritchMobs.INHABITED_ZOMBIE.get().create(level));
            zombie.moveTo(lane.origin.getX() + 2.5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(zombie);
            test.assertTrue(zombie.getMaxHealth() == 30F && zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5F
                            && Math.abs(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.23D) < 1e-6D
                            && zombie.getAttributeValue(Attributes.FOLLOW_RANGE) == 35F
                            && zombie.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) == 0F,
                    "the inhabited zombie keeps the original 30 health, 5 damage, 0.23 speed, 35 follow range and no reinforcements: "
                            + zombie.getMaxHealth() + " " + zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + zombie.getAttributeValue(Attributes.FOLLOW_RANGE));
            zombie.hurt(level.damageSources().generic(), 100F);
            var crabs = level.getEntitiesOfClass(EntityEldritchCrab.class, lane.area(8));
            test.assertTrue(crabs.size() == crabsBefore + 1 && crabs.get(crabs.size() - 1).hasHelm()
                            && crabs.get(crabs.size() - 1).getArmorValue() == 5,
                    "the inhabited zombie dies into one helmeted eldritch crab: " + (crabs.size() - crabsBefore));
        } finally {
            lane.clean();
        }
        test.succeed();
    }
}
