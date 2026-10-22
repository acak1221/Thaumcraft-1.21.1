package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.mods.ChampionRules;

/**
 * The original TC4 champion system inside a real dedicated server: the spawn roll of the original
 * whitelist, the original thirty health and double damage bonus of a promoted champion, the
 * original name, the persistent champion id and the three original triggers of the modifiers:
 * the tick of the warded champion, the hit of the venomous champion and the wound of the
 * infested champion.
 */
public final class ChampionRuntimeChecks {
    private ChampionRuntimeChecks() {}

    /** Oracle EntityUtils:272 and ChampionModWarded:26 read getBaseValue, not getAttributeValue. */
    public static void wardUsesBaseHealth(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var spider = lane.add(EntityType.SPIDER.create(lane.level));
            spider.setPos(lane.origin.getX(), lane.origin.getY(), lane.origin.getZ());
            ChampionJoinRoll.join(lane.level, spider, ChampionModifier.WARDED);
            test.assertTrue(spider.getMaxHealth() == 46F && spider.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == 16D,
                    "spider fixture carries original base16 and champion bonus30");
            test.assertTrue(spider.getAbsorptionAmount() == 8F,
                    "original ward is half BASE health, expected8 actual=" + spider.getAbsorptionAmount());
            spider.tickCount = 24;
            lane.level.tickNonPassenger(spider);
            test.assertTrue(spider.getAbsorptionAmount() == 8F, "ward never regenerates above base-health ceiling8");
            spider.setAbsorptionAmount(6F);
            spider.invulnerableTime = 0;
            spider.tickCount = 24;
            lane.level.tickNonPassenger(spider);
            test.assertTrue(spider.getAbsorptionAmount() == 7F, "one native tick25 regenerates exactly one ward");
            lane.level.tickNonPassenger(spider);
            test.assertTrue(spider.getAbsorptionAmount() == 7F, "ordinary non-cadence tick gives no ward");
        } finally { lane.clean(); }
        test.succeed();
    }

    public static void warpedHitReachesPlayer(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(lane.origin.getX(), lane.origin.getY(), lane.origin.getZ());
            var champion = lane.add(EntityType.ZOMBIE.create(lane.level));
            champion.setPos(player.position());
            ChampionJoinRoll.join(lane.level, champion, ChampionModifier.WARP);
            var knowledge = thaumcraft.common.lib.capabilities.KnowledgeAccess.get(player);
            int before = knowledge.getWarpTemp();
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredWarpEffect(lane.level, .1F, 1,
                    () -> player.hurt(lane.level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2,
                    "native player hit awards original two temporary warp exactly once; actual delta=" + (knowledge.getWarpTemp() - before));
            ChampionJoinRoll.declaredWarpEffect(lane.level, .1F, 1,
                    () -> player.hurt(lane.level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2, "rejected hit awards no further warp");
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredWarpEffect(lane.level, .9F, 1,
                    () -> player.hurt(lane.level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2, "unsuccessful chance roll awards no warp");
        } finally { lane.clean(); }
        test.succeed();
    }

    /** Native player damage must dispatch the same champion effects as native mob damage. */
    public static void effectsReachPlayerVictimsOnce(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            var level = lane.level;
            var player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(lane.origin.getX(), lane.origin.getY(), lane.origin.getZ());
            var champion = lane.add(EntityType.ZOMBIE.create(level));
            champion.setPos(player.position());
            ChampionJoinRoll.join(level, champion, ChampionModifier.VAMPIRIC);
            var pig = lane.add(EntityType.PIG.create(level));
            pig.setPos(player.position());
            level.addFreshEntity(pig);
            champion.setHealth(10F);
            pig.hurt(level.damageSources().mobAttack(champion), 4F);
            test.assertTrue(champion.getHealth() == 12F, "non-player hit still heals exactly once through living-entity hook");
            champion.setHealth(10F);
            player.invulnerableTime = 0;
            test.assertTrue(player.hurt(level.damageSources().mobAttack(champion), 4F), "native player accepts champion hit");
            test.assertTrue(champion.getHealth() == 12F, "one player hit heals vampiric attacker exactly two, actual=" + champion.getHealth());
            // A rejected second hit during hurt immunity is not another champion action.
            player.hurt(level.damageSources().mobAttack(champion), 4F);
            test.assertTrue(champion.getHealth() == 12F, "rejected player hit must not heal attacker again");
            ChampionHooks.setChampionModifier(champion, ChampionModifier.VENOMOUS);
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredEffect(level, .1F, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
            var poison = player.getEffect(net.minecraft.world.effect.MobEffects.POISON);
            test.assertTrue(poison != null && poison.getDuration() == 100 && poison.getAmplifier() == 0,
                    "venomous player victim receives original 100-tick poison, not attacker");
            test.assertTrue(!champion.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "poison belongs only to victim");
            player.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredEffect(level, .9F, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "failed poison roll has no effect");
            for (int modifier : new int[]{ChampionModifier.GRIM, ChampionModifier.SICKLY, ChampionModifier.FIERY}) {
                ChampionHooks.setChampionModifier(champion, modifier);
                player.invulnerableTime = 0;
                player.setHealth(20F);
                ChampionJoinRoll.declaredEffect(level, .1F, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
                if (modifier == ChampionModifier.FIERY) {
                    test.assertTrue(player.getRemainingFireTicks() == 80, "fiery champion sets player alight for exactly four seconds");
                    player.clearFire();
                } else {
                    var effect = modifier == ChampionModifier.GRIM ? net.minecraft.world.effect.MobEffects.WITHER : net.minecraft.world.effect.MobEffects.HUNGER;
                    var applied = player.getEffect(effect);
                    test.assertTrue(applied != null && applied.getDuration() == (modifier == ChampionModifier.GRIM ? 200 : 500)
                            && applied.getAmplifier() == 0, "original champion status reaches player victim");
                    player.removeEffect(effect);
                }
            }
            ChampionHooks.setChampionModifier(champion, ChampionModifier.WARP);
            var knowledge = thaumcraft.common.lib.capabilities.KnowledgeAccess.get(player);
            int before = knowledge.getWarpTemp();
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredWarpEffect(level, .1F, 1, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2, "one warped champion hit awards exactly the declared original two temporary warp");
            ChampionJoinRoll.declaredWarpEffect(level, .1F, 1, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2, "hurt-immunity rejection does not award warp twice");
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredWarpEffect(level, .9F, 1, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2, "failed warped roll awards no warp");
            player.setGameMode(GameType.CREATIVE);
            player.invulnerableTime = 0;
            ChampionJoinRoll.declaredWarpEffect(level, .1F, 1, () -> player.hurt(level.damageSources().mobAttack(champion), 1F));
            test.assertTrue(knowledge.getWarpTemp() == before + 2, "creative immunity awards no warp");
        } finally { lane.clean(); }
        test.succeed();
    }

    /** The original spawn roll and the original promotion bonuses of a champion. */
    public static void promotionUsesTheOriginalBonuses(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            // The original roll only reaches the mobs of the original bestiary.
            var pig = lane.add(EntityType.PIG.create(level));
            pig.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(pig);
            ChampionHooks.onEntityAdded(pig, level);
            test.assertTrue(ChampionHooks.championId(pig) == ChampionHooks.NOT_ROLLED,
                    "the original roll never reaches a passive animal: " + ChampionHooks.championId(pig));
            var watcher = lane.add(ThaumcraftEldritchMobs.WATCHER.get().create(level));
            watcher.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 1.5D, 0F, 0F);
            level.addFreshEntity(watcher);
            ChampionHooks.onEntityAdded(watcher, level);
            int rolled = ChampionHooks.championId(watcher);
            test.assertTrue(rolled != ChampionHooks.NOT_ROLLED,
                    "the original roll reaches the watcher of the original whitelist: " + rolled);
            test.assertTrue(rolled == ChampionRules.NO_CHAMPION || (rolled >= 0 && rolled < ChampionRules.modifierCount()),
                    "the watcher carries one of the original thirteen modifiers or none: " + rolled);

            // The original promotion: thirty health, double damage and the original name.
            var champion = lane.add(EntityType.ZOMBIE.create(level));
            champion.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            level.addFreshEntity(champion);
            double baseHealth = champion.getAttributeValue(Attributes.MAX_HEALTH);
            double baseDamage = champion.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double baseSpeed = champion.getAttributeValue(Attributes.MOVEMENT_SPEED);
            ChampionRules.Adapter.promote(champion, ChampionModifier.MIGHTY);
            test.assertTrue(Math.abs(champion.getAttributeValue(Attributes.MAX_HEALTH) - (baseHealth + 30D)) < 1e-6D,
                    "a champion keeps the original thirty health bonus: " + champion.getAttributeValue(Attributes.MAX_HEALTH));
            test.assertTrue(Math.abs(champion.getAttributeValue(Attributes.ATTACK_DAMAGE) - baseDamage * 6D) < 1e-6D,
                    "the mighty champion keeps the original double and triple damage: " + champion.getAttributeValue(Attributes.ATTACK_DAMAGE));
            test.assertTrue(champion.getCustomName() != null && champion.getCustomName().getString().startsWith("Mighty"),
                    "a champion is named after its original modifier: " + champion.getCustomName());
            test.assertTrue(champion.getHealth() > baseHealth,
                    "the promotion heals the champion for the original twenty five health: " + champion.getHealth());

            // The original removes the modifier of the champion before it applies it again
            // (oracle EntityUtils.makeChampion:241-249), so a second promotion of the same
            // modifier replaces the bonus instead of adding it twice.
            double promotedHealth = champion.getAttributeValue(Attributes.MAX_HEALTH);
            double promotedDamage = champion.getAttributeValue(Attributes.ATTACK_DAMAGE);
            ChampionRules.Adapter.promote(champion, ChampionModifier.MIGHTY);
            test.assertTrue(Math.abs(champion.getAttributeValue(Attributes.MAX_HEALTH) - promotedHealth) < 1e-6D
                            && Math.abs(champion.getAttributeValue(Attributes.ATTACK_DAMAGE) - promotedDamage) < 1e-6D,
                    "a second promotion of the same modifier replaces the bonus instead of adding it twice: "
                            + champion.getAttributeValue(Attributes.MAX_HEALTH) + " / " + champion.getAttributeValue(Attributes.ATTACK_DAMAGE));

            // The bold champion keeps the original thirty percent movement speed.
            var bold = lane.add(EntityType.ZOMBIE.create(level));
            bold.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 3.5D, 0F, 0F);
            level.addFreshEntity(bold);
            double boldBase = bold.getAttributeValue(Attributes.MOVEMENT_SPEED);
            ChampionRules.Adapter.promote(bold, ChampionModifier.BOLD);
            test.assertTrue(Math.abs(bold.getAttributeValue(Attributes.MOVEMENT_SPEED) - boldBase * 1.3D) < 1e-6D,
                    "the bold champion keeps the original thirty percent speed bonus: " + bold.getAttributeValue(Attributes.MOVEMENT_SPEED));

            // The original champion id travels with the entity in its nbt.
            ChampionHooks.setChampionModifier(bold, ChampionModifier.VENOMOUS);
            CompoundTag saved = bold.saveWithoutId(new CompoundTag());
            test.assertTrue(saved.getInt(ChampionHooks.SAVE_KEY) == ChampionModifier.VENOMOUS,
                    "the champion id is persisted in the original nbt key: " + saved.getInt(ChampionHooks.SAVE_KEY));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original champion modifiers answer the tick, the hit and the wound of their champion. */
    public static void modifiersAnswerTickHitAndWound(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            // The original warded champion: the original promotion gave it thirty health more,
            // double damage and the name of its modifier (oracle EntityUtils.makeChampion:243-254),
            // the original makeChampion case five gave it half of its maximum health as its ward
            // (oracle EntityUtils.makeChampion:271-274) and the original modifier regained one
            // point of that ward on every twenty fifth tick (oracle ChampionModWarded:25-30).
            var warded = lane.add(EntityType.SPIDER.create(level));
            warded.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            ChampionJoinRoll.join(level, warded, ChampionModifier.WARDED);
            test.assertTrue(warded.getMaxHealth() == 46F && warded.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4F
                            && warded.getCustomName() != null && warded.getCustomName().getString().startsWith("Warded"),
                    "the warded champion keeps the original thirty health, double damage and name: "
                            + warded.getMaxHealth() + " " + warded.getAttributeValue(Attributes.ATTACK_DAMAGE)
                            + " " + warded.getCustomName());
            test.assertTrue(Math.abs(warded.getAbsorptionAmount() - 23F) < 1e-4F,
                    "the warded champion carries the original ward of half of its maximum health: "
                            + warded.getAbsorptionAmount());
            // The spent ward: the original modifier regains one point of it on the twenty fifth tick.
            warded.setAbsorptionAmount(0F);
            StringBuilder trace = new StringBuilder();
            float peak = warded.getAbsorptionAmount();
            for (int tick = 0; tick < 25; tick++) {
                level.tickNonPassenger(warded);
                peak = Math.max(peak, warded.getAbsorptionAmount());
                trace.append(' ').append(warded.getAbsorptionAmount());
            }
            test.assertTrue(peak > 0F,
                    "the warded champion regains one point of its ward on its original twenty five tick cycle: peak=" + peak
                            + " ward=" + warded.getAbsorptionAmount() + " ticks=" + warded.tickCount
                            + " id=" + ChampionHooks.championId(warded) + " fire=" + warded.isOnFire() + " trace=" + trace);
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /**
     * The original whitelist matched the class of the entity, so the cave spider inherited the
     * level of the spider and was a champion candidate of the original.
     */
    public static void caveSpiderIsAChampionCandidate(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            // Oracle ConfigEntities:196 - the original whitelisted the class of the spider, so
            // the cave spider inherited its level and the lowest roll of the original promoted
            // it (oracle EventHandlerEntity.entitySpawns:391-419).
            var spider = lane.add(EntityType.CAVE_SPIDER.create(level));
            spider.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            ChampionJoinRoll.join(level, spider, ChampionModifier.BOLD);
            test.assertTrue(ChampionHooks.championId(spider) == ChampionModifier.BOLD,
                    "the cave spider of the original spider class is a champion candidate: "
                            + ChampionHooks.championId(spider));
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }

    /** The original venomous champion poisons its victim and the infested one answers a wound. */
    public static void modifiersAnswerHitAndWound(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        try {
            ServerLevel level = lane.level;
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.setPos(lane.origin.getX() + 40D, lane.origin.getY(), lane.origin.getZ());
            lane.floor(4);

            // The original venomous champion poisons its victim on four of every ten hits.
            var venomous = lane.add(EntityType.ZOMBIE.create(level));
            venomous.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            level.addFreshEntity(venomous);
            ChampionHooks.setChampionModifier(venomous, ChampionModifier.VENOMOUS);
            var victim = lane.add(EntityType.PIG.create(level));
            victim.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + .5D, 0F, 0F);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
            victim.setHealth(1000F);
            level.addFreshEntity(victim);
            // The original venomous champion poisons its victim on four of every ten hits; the
            // scenario declares that original roll of the world random, so the fixture never
            // depends on the position of the world stream.
            ChampionJoinRoll.declaredEffect(level, 0.1F, () -> {
                for (int hit = 0; hit < 20; hit++) {
                    victim.hurt(level.damageSources().mobAttack(venomous), 1F);
                    for (int tick = 0; tick < 12; tick++) level.tickNonPassenger(victim);
                }
            });
            test.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                    "the venomous champion poisons its victim: " + victim.getEffect(net.minecraft.world.effect.MobEffects.POISON));

            // The original infested champion answers a wound with a tainted spiderling.
            var infested = lane.add(EntityType.ZOMBIE.create(level));
            infested.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            infested.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
            infested.setHealth(1000F);
            level.addFreshEntity(infested);
            ChampionHooks.setChampionModifier(infested, ChampionModifier.INFESTED);
            var attacker = lane.add(EntityType.PIG.create(level));
            attacker.moveTo(lane.origin.getX() + .5D, lane.origin.getY(), lane.origin.getZ() + 2.5D, 0F, 0F);
            attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000D);
            attacker.setHealth(1000F);
            level.addFreshEntity(attacker);
            // The original infested champion answers every wound with a tainted spiderling on four
            // of every ten wounds; the scenario declares that original roll of the world random.
            ChampionJoinRoll.declaredEffect(level, 0.1F, () -> {
                for (int hit = 0; hit < 20; hit++) {
                    infested.hurt(level.damageSources().mobAttack(attacker), 1F);
                    for (int tick = 0; tick < 12; tick++) level.tickNonPassenger(infested);
                }
            });
            var spiderlings = level.getEntitiesOfClass(EntityTaintSpider.class, lane.area(4));
            test.assertTrue(!spiderlings.isEmpty(),
                    "the infested champion answers a wound with a tainted spiderling: " + spiderlings.size());
        } catch (RuntimeException | Error failure) {
            lane.clean();
            throw failure;
        }
        lane.clean();
        test.succeed();
    }
}
