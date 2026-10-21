package thaumcraft.eldritch;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchMobRules;
import static org.junit.jupiter.api.Assertions.*;

/** Original TC4 eldritch bestiary numbers, cooldowns and special state transitions. */
class EldritchMobRulesTest {
    @Test void originalAttributeTableIsPreserved() {
        assertEquals(200F,EldritchMobRules.WARDEN.maxHealth());
        assertEquals(10F,EldritchMobRules.WARDEN.attackDamage());
        assertEquals(4F,EldritchMobRules.WARDEN.armor());
        assertEquals(.33F,EldritchMobRules.WARDEN.movementSpeed());
        assertEquals(40F,EldritchMobRules.WARDEN.followRange());
        assertEquals(50,EldritchMobRules.WARDEN.experience());
        assertEquals(500,EldritchMobRules.WARDEN.talkInterval());

        assertEquals(250F,EldritchMobRules.GOLEM.maxHealth());
        assertEquals(10F,EldritchMobRules.GOLEM.attackDamage());
        assertEquals(6F,EldritchMobRules.GOLEM.armor());
        assertEquals(.30F,EldritchMobRules.GOLEM.movementSpeed());
        assertEquals(50,EldritchMobRules.GOLEM.experience());

        assertEquals(20F,EldritchMobRules.CRAB.maxHealth());
        assertEquals(4F,EldritchMobRules.CRAB.attackDamage());
        assertEquals(5F,EldritchMobRules.CRAB_HELMET_ARMOR);
        assertEquals(6,EldritchMobRules.CRAB.experience());
        assertEquals(160,EldritchMobRules.CRAB.talkInterval());

        assertEquals(30F,EldritchMobRules.INHABITED_ZOMBIE.maxHealth());
        assertEquals(5F,EldritchMobRules.INHABITED_ZOMBIE.attackDamage());
        assertEquals(.23F,EldritchMobRules.INHABITED_ZOMBIE.movementSpeed());
        assertEquals(35F,EldritchMobRules.INHABITED_ZOMBIE.followRange());
        assertEquals(5,EldritchMobRules.INHABITED_ZOMBIE.experience());

        assertEquals(30F,EldritchMobRules.WATCHER.maxHealth());
        assertEquals(6F,EldritchMobRules.WATCHER.attackDamage());
        assertEquals(.50F,EldritchMobRules.WATCHER.movementSpeed());
        assertEquals(16F,EldritchMobRules.WATCHER.followRange());
        assertEquals(10,EldritchMobRules.WATCHER.experience());
        assertEquals(160,EldritchMobRules.WATCHER.talkInterval());

        assertEquals(125F,EldritchMobRules.TAINTACLE_GIANT.maxHealth());
        assertEquals(9F,EldritchMobRules.TAINTACLE_GIANT.attackDamage());
        assertEquals(20,EldritchMobRules.TAINTACLE_GIANT.experience());
    }

    @Test void originalWardenTitlesArePreserved() {
        assertEquals(15,EldritchMobRules.WARDEN_TITLES.length);
        assertEquals("Aphoom-Zhah",EldritchMobRules.WARDEN_TITLES[0]);
        assertEquals("Zushakon",EldritchMobRules.WARDEN_TITLES[14]);
        assertEquals("Nyogtha",EldritchMobRules.wardenTitle(4));
        assertEquals("Aphoom-Zhah",EldritchMobRules.wardenTitle(15));
        assertEquals("Zushakon",EldritchMobRules.wardenTitle(-1));
    }

    @Test void originalWardenWardIsTwoThirdsOfTheBaseHealth() {
        assertEquals(132,EldritchMobRules.wardenWardCap(200F));
        assertEquals(12,EldritchMobRules.wardenWardCap(19F));
        assertEquals(132F,EldritchMobRules.wardenWardRegeneration(131F,132));
        assertEquals(132F,EldritchMobRules.wardenWardRegeneration(132F,132));
        assertTrue(EldritchMobRules.wardenWardsRegenerate(0,25));
        assertFalse(EldritchMobRules.wardenWardsRegenerate(1,25));
        assertFalse(EldritchMobRules.wardenWardsRegenerate(0,24));
    }

    @Test void originalWardenFrenzyGeometryIsPreserved() {
        assertEquals(150,EldritchMobRules.WARDEN_FREENZY_TICKS);
        assertTrue(EldritchMobRules.wardenFrenzyTeleports(150));
        assertFalse(EldritchMobRules.wardenFrenzyTeleports(149));
        assertTrue(EldritchMobRules.wardenFrenzyRingDue(120));
        assertTrue(EldritchMobRules.wardenFrenzyRingDue(0));
        assertFalse(EldritchMobRules.wardenFrenzyRingDue(111));
        assertFalse(EldritchMobRules.wardenFrenzyRingDue(121));
        assertEquals(18.75D,EldritchMobRules.wardenFrenzyRadius(0));
        assertEquals(3.75D,EldritchMobRules.wardenFrenzyRadius(120));        assertEquals(1,EldritchMobRules.wardenFrenzyStep(0));
        assertEquals(16,EldritchMobRules.wardenFrenzyStep(120));
        assertEquals(180,EldritchMobRules.wardenFrenzySamples(0));
        assertEquals(11,EldritchMobRules.wardenFrenzySamples(120));
        assertEquals(0D,EldritchMobRules.wardenFrenzyAngle(0,1));
        assertEquals(Math.PI,EldritchMobRules.wardenFrenzyAngle(90,1));
    }

    @Test void originalWardenAttackSelectionIsPreserved() {
        assertTrue(EldritchMobRules.wardenUsesOrb(.21F));
        assertFalse(EldritchMobRules.wardenUsesOrb(.2F));
        assertFalse(EldritchMobRules.wardenUsesOrb(.19F));
        assertEquals(3,EldritchMobRules.wardenWarpAmount(0));
        assertEquals(5,EldritchMobRules.wardenWarpAmount(2));
        assertEquals(90,EldritchMobRules.wardenArmAngle(true));
        assertEquals(180,EldritchMobRules.wardenArmAngle(false));
    }

    @Test void originalGolemNumbersArePreserved() {
        assertEquals(3F,EldritchMobRules.golemEyeHeight(false));
        assertEquals(3.33F,EldritchMobRules.golemEyeHeight(true));
        // Oracle EntityEldritchGolem:216: the melee hit is the attack attribute times 0.75f, in float.
        assertEquals(10F*0.75F,EldritchMobRules.golemMeleeDamage(10F));
        assertEquals(10,EldritchMobRules.GOLEM_MELEE_COOLDOWN);
        assertEquals(.2D,EldritchMobRules.GOLEM_MELEE_KNOCKBACK_Y);
        assertEquals(135,EldritchMobRules.golemBeamDrain(150,0));
        assertEquals(131,EldritchMobRules.golemBeamDrain(150,4));
        assertEquals(8,EldritchMobRules.golemArcTicks(0));
        assertEquals(12,EldritchMobRules.golemArcTicks(4));
        assertTrue(EldritchMobRules.golemChargesBeam(true,false,0));
        assertFalse(EldritchMobRules.golemChargesBeam(false,false,0));
        assertFalse(EldritchMobRules.golemChargesBeam(true,true,0));
    }

    @Test void originalGolemBodyAndEyeHeightArePreserved() {
        // Oracle EntityEldritchGolem.func_70047_e:133-135 - the golem keeps its 1.75 by 3.5 body
        // and looks from three, or from 3.33 once its head is gone.
        assertEquals(1.75F,EldritchMobRules.golemDimensions(false).width());
        assertEquals(3.5F,EldritchMobRules.golemDimensions(false).height());
        assertEquals(3F,EldritchMobRules.golemDimensions(false).eyeHeight());
        assertEquals(1.75F,EldritchMobRules.golemDimensions(true).width());
        assertEquals(3.5F,EldritchMobRules.golemDimensions(true).height());
        assertEquals(3.33F,EldritchMobRules.golemDimensions(true).eyeHeight());
    }

    @Test void originalGolemOnlyLosesItsHeadOnALethalHit() {
        assertTrue(EldritchMobRules.golemBeheads(120F,100F,false));
        assertFalse(EldritchMobRules.golemBeheads(100F,100F,false));
        assertFalse(EldritchMobRules.golemBeheads(120F,100F,true));
    }

    @Test void originalCrabHelmetRulesArePreserved() {
        assertTrue(EldritchMobRules.crabHasHelm(3,.99F));
        assertTrue(EldritchMobRules.crabHasHelm(2,.32F));
        assertFalse(EldritchMobRules.crabHasHelm(2,.33F));
        assertTrue(EldritchMobRules.crabHelmetBreaks(10F,20F));
        assertFalse(EldritchMobRules.crabHelmetBreaks(11F,20F));
        assertEquals(10,EldritchMobRules.crabRideInterval(0));
        assertEquals(19,EldritchMobRules.crabRideInterval(9));
        assertTrue(EldritchMobRules.crabDismounts(.19F));
        assertFalse(EldritchMobRules.crabDismounts(.2F));
    }

    @Test void originalCrabEyeDropIsPreserved() {
        assertTrue(EldritchMobRules.crabDropsEye(0,0));
        assertFalse(EldritchMobRules.crabDropsEye(1,0));
        assertTrue(EldritchMobRules.crabDropsEye(2,1));
        assertFalse(EldritchMobRules.crabDropsEye(1,0));
        assertEquals(1F/3F,EldritchMobRules.crabEyeChance(0),1.0E-6F);
        assertEquals(2F/3F,EldritchMobRules.crabEyeChance(1),1.0E-6F);
        assertEquals(7F/9F,EldritchMobRules.crabEyeChance(2),1.0E-6F);
    }

    @Test void originalWatcherGazeRulesArePreserved() {
        assertEquals(80,EldritchMobRules.watcherGazeDuration(false));
        assertEquals(60,EldritchMobRules.watcherGazeDuration(true));
        assertEquals(1F,EldritchMobRules.watcherGazeDamage(2,false));
        assertEquals(3F,EldritchMobRules.watcherGazeDamage(3,false));
        assertEquals(3F,EldritchMobRules.watcherGazeDamage(2,true));
        assertEquals(5F,EldritchMobRules.watcherGazeDamage(3,true));
        assertEquals(2F,EldritchMobRules.WATCHER_THORNS_DAMAGE);
        assertEquals(40/(float)80,EldritchMobRules.watcherGazeProgress(40,80));
    }

    @Test void originalSlimeSizeRulesArePreserved() {
        assertEquals(1,EldritchMobRules.slimeInitialSize(0));
        assertEquals(2,EldritchMobRules.slimeInitialSize(1));
        assertEquals(4,EldritchMobRules.slimeInitialSize(2));
        assertEquals(10,EldritchMobRules.slimeInitialJumpDelay(0));
        assertEquals(29,EldritchMobRules.slimeInitialJumpDelay(19));
        assertEquals(8,EldritchMobRules.slimeJumpDelay(0));
        assertEquals(23,EldritchMobRules.slimeJumpDelay(15));
        assertEquals(4,EldritchMobRules.slimeAttackStrength(4));
        // Oracle EntityThaumicSlime:299: the sound volume is 0.1f times the root of the size, in float.
        assertEquals(.1F*(float)Math.sqrt(4),EldritchMobRules.slimeSoundVolume(4));
        assertEquals(100,EldritchMobRules.slimeMergedSize(99,50));
        assertEquals(60,EldritchMobRules.slimeMergedSize(50,10));
        assertEquals(2,EldritchMobRules.slimeSplitCount(4));
        assertEquals(10,EldritchMobRules.slimeSplitCount(100));
        assertTrue(EldritchMobRules.slimeCanSpit(4));
        assertFalse(EldritchMobRules.slimeCanSpit(3));
        // Oracle EntityThaumicSlime:102-103: the body is 0.25f times the root of the size plus 0.25f.
        assertEquals(.25F*(float)Math.sqrt(4)+.25F,EldritchMobRules.slimeDimension(4));
        assertEquals(.25F*(float)Math.sqrt(1)+.25F,EldritchMobRules.slimeDimension(1));
        assertEquals(4F,EldritchMobRules.slimeHealth(4));
    }

    @Test void originalGiantTaintacleEnrageIsPreserved() {
        assertTrue(EldritchMobRules.giantEnrages(36F,0));
        assertFalse(EldritchMobRules.giantEnrages(36F,10));
        assertFalse(EldritchMobRules.giantEnrages(35F,0));
        var effects=EldritchMobRules.giantEnrageEffects(60F);
        assertEquals(4,effects.regeneration());
        assertEquals(1,effects.strength());
        assertEquals(1,effects.speed());
        assertEquals(35F,EldritchMobRules.giantDamageCap(60F));
        assertEquals(20F,EldritchMobRules.giantDamageCap(20F));
        assertTrue(EldritchMobRules.giantDropsSpecialObject(0));
        assertFalse(EldritchMobRules.giantDropsSpecialObject(1));
    }

    @Test void originalSharedBossRulesArePreserved() {
        assertEquals(35F,EldritchMobRules.bossDamageCap(90F));
        assertEquals(12F,EldritchMobRules.bossDamageCap(12F));
        assertEquals(0,EldritchMobRules.bossBonusPlayers(1));
        assertEquals(1,EldritchMobRules.bossBonusPlayers(2));
        assertEquals(5,EldritchMobRules.bossBonusPlayers(9));
        assertEquals(50F,EldritchMobRules.BOSS_HEALTH_BONUS);
        assertEquals(.5F,EldritchMobRules.BOSS_DAMAGE_BONUS);
        assertEquals(24F,EldritchMobRules.BOSS_HOME_RADIUS);
        assertEquals(.95F,EldritchMobRules.BOSS_KNOCKBACK_RESISTANCE);
        assertEquals(16384F,EldritchMobRules.BOSS_STALE_DISTANCE_SQUARED);
        assertEquals(4,EldritchMobRules.bossRegenerationAmplifier(60F));
        assertEquals(1,EldritchMobRules.bossStrengthAmplifier(60F));
        assertEquals(1,EldritchMobRules.bossSpeedAmplifier(60F));
    }

    @Test void originalBossAggroGateKeepsItsQuirk() {
        // A candidate needs more than twenty five aggro and more than ten percent of the current aggro.
        assertTrue(EldritchMobRules.bossAggroQualifies(100,50,50));
        assertFalse(EldritchMobRules.bossAggroQualifies(75,50,50));
        assertFalse(EldritchMobRules.bossAggroQualifies(51,50,50));
        // The original overwrites its highest marker with the candidate entity id, which is usually
        // small, so the third gate stops blocking the candidate.
        assertTrue(EldritchMobRules.bossAggroQualifies(100,50,3));
        assertFalse(EldritchMobRules.bossAggroQualifies(100,50,150));
    }
}
