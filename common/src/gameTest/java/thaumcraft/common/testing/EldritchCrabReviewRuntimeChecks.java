package thaumcraft.common.testing;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.lib.mods.ChampionHooks;

/** A supplied already-mounted crab, actual native combat and its own sixty ticks. */
public final class EldritchCrabReviewRuntimeChecks {
    private EldritchCrabReviewRuntimeChecks() {}

    public static void mountedCrabAttacksAgainAfterItsCooldown(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var chunk = new ChunkPos(f.base);
            boolean forced = !f.level.getForcedChunks().contains(chunk.toLong());
            if (forced) f.level.setChunkForced(chunk.x, chunk.z, true);
            int[] attempts = {0};
            var crab = new EntityEldritchCrab(ThaumcraftEldritchMobs.ELDRITCH_CRAB.get(), f.level) {
                @Override public boolean doHurtTarget(Entity target) { attempts[0]++; return super.doHurtTarget(target); }
            };
            var victim = EntityType.PIG.create(f.level);
            try {
                GameTestChunkAwait.entityTicking(test, f.base);
                test.assertTrue(victim != null, "native mounted-combat victim is available");
                crab.setNoAi(true); crab.setNoGravity(true); victim.setNoAi(true); victim.setNoGravity(true);
                crab.setPos(Vec3.atBottomCenterOf(f.base)); victim.setPos(Vec3.atBottomCenterOf(f.base));
                victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); victim.setHealth(100);
                ChampionHooks.setChampionModifier(crab, -1);
                test.assertTrue(f.level.addFreshEntity(victim) && f.level.addFreshEntity(crab),
                        "actual ordinary crab and native victim are admitted");
                test.assertTrue(crab.startRiding(victim, true), "fixture begins with the native crab already attached");
                crab.tick();
                float afterFirst = victim.getHealth();
                test.assertTrue(afterFirst < 100 && afterFirst > 0, "the first native mounted hit actually damages its victim");
                for (int tick = 0; tick < 60 && victim.isAlive(); tick++) {
                    victim.tick();
                    // Reattach after the original random dismount so the finite
                    // fixture isolates the attack timer, without replacing RNG or damage.
                    if (crab.getVehicle() != victim) crab.startRiding(victim, true);
                    crab.tick();
                }
                test.assertTrue(victim.getHealth() < afterFirst,
                        "mounted native crab must inflict another hit after its 10..19-tick cooldown: first="
                                + afterFirst + ", after60=" + victim.getHealth());
                test.assertTrue(attempts[0] >= 4 && attempts[0] <= 7,
                        "sixty further ticks retain the original 10..19-tick cadence, rather than attacking every tick: " + attempts[0]);
            } finally {
                crab.discard(); if (victim != null) victim.discard();
                if (forced) f.level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
        test.succeed();
    }
}
