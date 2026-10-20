package thaumcraft.common.testing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.entities.monster.EntityTaintChicken;
import thaumcraft.common.entities.monster.EntityTaintCrawler;
import thaumcraft.common.entities.monster.EntityTaintPig;
import thaumcraft.common.entities.monster.EntityTaintSeed;
import thaumcraft.common.entities.monster.EntityTaintSeedPrime;

/**
 * The original TC4 tainted animals inside a real dedicated server: the registered numbers of the
 * five tainted farm animals, the tainted crawler and the taint seeds, the original drops, the
 * conversion of a flux tainted animal into its tainted twin and the flux taint aura of a seed.
 */
public final class TaintedAnimalRuntimeChecks {
    private TaintedAnimalRuntimeChecks() {}

    /** The original tainted animals keep their numbers, their quirks and their drops. */
    public static void animalNumbersAndDrops(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(4);

            var chicken = lane.add(ThaumcraftTaintAnimals.TAINT_CHICKEN.get().create(level));
            chicken.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(chicken);
            test.assertTrue(chicken.getMaxHealth() == 8F && chicken.getAttributeValue(Attributes.ATTACK_DAMAGE) == 3F
                            && Math.abs(chicken.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.4D) < 1e-6D
                            && chicken.getAttributeValue(Attributes.FOLLOW_RANGE) == 16F,
                    "the tainted chicken keeps the original 8 health, 3 damage, 0.4 speed and 16 follow range: "
                            + chicken.getMaxHealth() + " " + chicken.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + chicken.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + chicken.getAttributeValue(Attributes.FOLLOW_RANGE));
            test.assertTrue(!chicken.causeFallDamage(10F, 1F, level.damageSources().fall()),
                    "the tainted chicken never takes fall damage");
            test.assertTrue(Math.abs(chicken.getVoicePitch() - 0.7F) < 1e-4F,
                    "the tainted chicken keeps the original voice pitch of 0.7: " + chicken.getVoicePitch());

            var cow = lane.add(ThaumcraftTaintAnimals.TAINT_COW.get().create(level));
            cow.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 1.5D, 0F, 0F);
            level.addFreshEntity(cow);
            test.assertTrue(cow.getMaxHealth() == 40F && cow.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6F
                            && Math.abs(cow.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.27D) < 1e-6D,
                    "the tainted cow keeps the original 40 health, 6 damage and 0.27 speed: " + cow.getMaxHealth() + " "
                            + cow.getAttributeValue(Attributes.ATTACK_DAMAGE) + " " + cow.getAttributeValue(Attributes.MOVEMENT_SPEED));
            test.assertTrue(Math.abs(cow.getVoicePitch() - 0.7F) < 1e-4F,
                    "the tainted cow keeps the original voice pitch of 0.7: " + cow.getVoicePitch());

            var pig = lane.add(ThaumcraftTaintAnimals.TAINT_PIG.get().create(level));
            pig.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(pig);
            test.assertTrue(pig.getMaxHealth() == 20F && pig.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F
                            && Math.abs(pig.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.275D) < 1e-6D,
                    "the tainted pig keeps the original 20 health, 4 damage and 0.275 speed: " + pig.getMaxHealth() + " "
                            + pig.getAttributeValue(Attributes.ATTACK_DAMAGE) + " " + pig.getAttributeValue(Attributes.MOVEMENT_SPEED));

            var sheep = lane.add(ThaumcraftTaintAnimals.TAINT_SHEEP.get().create(level));
            sheep.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            level.addFreshEntity(sheep);
            test.assertTrue(sheep.getMaxHealth() == 20F && sheep.getAttributeValue(Attributes.ATTACK_DAMAGE) == 3F
                            && Math.abs(sheep.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.25D) < 1e-6D,
                    "the tainted sheep keeps the original 20 health, 3 damage and 0.25 speed: " + sheep.getMaxHealth() + " "
                            + sheep.getAttributeValue(Attributes.ATTACK_DAMAGE) + " " + sheep.getAttributeValue(Attributes.MOVEMENT_SPEED));

            var villager = lane.add(ThaumcraftTaintAnimals.TAINT_VILLAGER.get().create(level));
            villager.moveTo(lane.origin.getX() + 2.5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(villager);
            test.assertTrue(villager.getMaxHealth() == 30F && villager.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F
                            && Math.abs(villager.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.3D) < 1e-6D,
                    "the tainted villager keeps the original 30 health, 4 damage and 0.3 speed: " + villager.getMaxHealth() + " "
                            + villager.getAttributeValue(Attributes.ATTACK_DAMAGE) + " " + villager.getAttributeValue(Attributes.MOVEMENT_SPEED));

            // The original drop of a tainted animal is tainted goo or a taint tendril, never a vanilla item.
            Item goo = ThaumcraftTaintAnimals.taintMaterial(11).getItem();
            Item tendril = ThaumcraftTaintAnimals.taintMaterial(12).getItem();
            chicken.hurt(level.damageSources().playerAttack(player), 1000F);
            int material = 0;
            int foreign = 0;
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, lane.area(4))) {
                if (drop.getItem().is(goo) || drop.getItem().is(tendril)) material++;
                else foreign++;
            }
            test.assertTrue(material == 1, "the tainted chicken drops exactly one taint material: " + material);
            test.assertTrue(foreign == 0, "the tainted chicken never drops a vanilla item: " + foreign);
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original flux tainted animal dies into its tainted twin. */
    public static void conversionIntoTheTaintedTwin(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            lane.floor(4);

            var fluxTaint = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());
            var tainted = lane.add(EntityType.CHICKEN.create(level));
            tainted.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(tainted);
            tainted.addEffect(new MobEffectInstance(fluxTaint, 200, 0, false, false));
            tainted.hurt(level.damageSources().playerAttack(player), 1000F);
            var twins = level.getEntitiesOfClass(EntityTaintChicken.class, lane.area(4));
            test.assertTrue(twins.size() == 1,
                    "a flux tainted chicken dies into one tainted chicken: " + twins.size());
            test.assertTrue(twins.isEmpty() || twins.get(0).getMaxHealth() == 8F,
                    "the tainted twin is the original eight health chicken");

            var plain = lane.add(EntityType.PIG.create(level));
            plain.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(plain);
            plain.hurt(level.damageSources().playerAttack(player), 1000F);
            test.assertTrue(level.getEntitiesOfClass(EntityTaintPig.class, lane.area(4)).isEmpty(),
                    "a pig without the flux taint dies into no tainted pig");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original tainted crawler and the taint seeds keep their numbers and their aura. */
    public static void crawlerAndSeedNumbers(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            var crawler = lane.add(ThaumcraftTaintAnimals.TAINT_CRAWLER.get().create(level));
            crawler.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(crawler);
            test.assertTrue(crawler.getMaxHealth() == 8F && crawler.getAttributeValue(Attributes.ATTACK_DAMAGE) == 2F
                            && Math.abs(crawler.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.275D) < 1e-6D
                            && crawler.getAttributeValue(Attributes.FOLLOW_RANGE) == 16F
                            && Math.abs(crawler.getPickRadius() - 0.1F) < 1e-4F,
                    "the tainted crawler keeps the original 8 health, 2 damage, 0.275 speed, 16 follow range and 0.1 pick radius: "
                            + crawler.getMaxHealth() + " " + crawler.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + crawler.getAttributeValue(Attributes.MOVEMENT_SPEED) + " " + crawler.getPickRadius());

            var seed = lane.add(ThaumcraftTaintAnimals.TAINT_SEED.get().create(level));
            seed.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(seed);
            var prime = lane.add(ThaumcraftTaintAnimals.TAINT_SEED_PRIME.get().create(level));
            prime.moveTo(lane.origin.getX() + 2.5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(prime);
            test.assertTrue(seed.getMaxHealth() == 75F && seed.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F
                            && prime.getMaxHealth() == 150F && prime.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7F,
                    "the taint seed keeps the original 75 health and 4 damage and the prime seed 150 health and 7 damage: "
                            + seed.getMaxHealth() + " " + seed.getAttributeValue(Attributes.ATTACK_DAMAGE) + " "
                            + prime.getMaxHealth() + " " + prime.getAttributeValue(Attributes.ATTACK_DAMAGE));

            // The original seed is rooted: it never walks away.
            double x = seed.getX();
            double z = seed.getZ();
            seed.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(3D, 0D, 3D));
            test.assertTrue(Math.abs(seed.getX() - x) < 1e-9 && Math.abs(seed.getZ() - z) < 1e-9,
                    "the taint seed is rooted and never walks: " + seed.position());

            // The original aura of a seed covers four blocks and lasts one hundred ticks.
            var victim = lane.add(EntityType.PIG.create(level));
            victim.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            victim.setNoAi(true);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
            victim.setHealth(1000F);
            level.addFreshEntity(victim);
            for (int tick = 0; tick < 25; tick++) level.tickNonPassenger(seed);
            var fluxTaint = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());
            test.assertTrue(victim.hasEffect(fluxTaint),
                    "a living entity next to the taint seed gets the original flux taint: " + victim.getEffect(fluxTaint)
                            + " seedTicks=" + seed.tickCount + " victim=" + victim.position() + " seed=" + seed.position());
            test.assertTrue(victim.hasEffect(fluxTaint) && victim.getEffect(fluxTaint).getAmplifier() == 0,
                    "the flux taint of the ordinary seed has the original amplifier zero");
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
