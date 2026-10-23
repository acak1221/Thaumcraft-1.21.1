package thaumcraft.common.testing;

import java.lang.reflect.Field;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.monster.EntityEldritchGolem;
import thaumcraft.common.entities.monster.EntityEldritchWarden;
import thaumcraft.common.entities.monster.EntityTaintacleGiant;
import thaumcraft.common.lib.events.BossPlacementRules;

/** Declared fixture: stationary lane, sword-sized attributed hits, no health/phase/counter writes.
 * Native entity ticker advances cooldowns, spawn protection, frenzy and death.
 * This is not a survival-player skill, client-visual or generated-arena acceptance. */
public final class BossFightRuntimeChecks {
    private BossFightRuntimeChecks() {}

    public static void fight(GameTestHelper test, String kind, boolean bars, boolean placement) {
        var lane = new WaveLane(test, 3, 2, 3);
        var level = lane.level;
        var player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        try {
            lane.floor(4);
            // Remember the air cells the warden's native frenzy may replace in this owned lane.
            if (kind.equals("warden")) for (int x=-5;x<=5;x++) for (int z=-5;z<=5;z++) {
                var pos=lane.origin.offset(x,0,z);
                lane.put(pos,level.getBlockState(pos));
            }
            Mob boss = lane.add(switch (kind) {
                case "golem" -> ThaumcraftEldritchMobs.ELDRITCH_GOLEM.get().create(level);
                case "warden" -> ThaumcraftEldritchMobs.ELDRITCH_WARDEN.get().create(level);
                case "praetor" -> ThaumcraftCultists.CULTIST_LEADER.get().create(level);
                case "giant" -> ThaumcraftEldritchMobs.TAINTACLE_GIANT.get().create(level);
                case "taintacle" -> ThaumcraftTaint.TAINTACLE.get().create(level);
                case "pech" -> ThaumcraftPech.PECH.get().create(level);
                default -> throw new IllegalArgumentException(kind);
            });
            boss.moveTo(lane.origin.getX()+.5, lane.origin.getY(), lane.origin.getZ()+.5, 0, 0);
            boss.setNoGravity(true);
            boss.finalizeSpawn(level, level.getCurrentDifficultyAt(lane.origin), net.minecraft.world.entity.MobSpawnType.EVENT, null);
            // Declare the join roll: original bosses are Bold, ordinary mobs stay ordinary.
            if (boss instanceof thaumcraft.api.entities.IThaumcraftBoss)
                ChampionJoinRoll.join(level,boss,thaumcraft.common.lib.mods.ChampionModifier.BOLD);
            else ChampionJoinRoll.join(level,boss);
            // Player stays outside retaliation range; only attributed incoming sword-sized hits are driven.
            player.setPos(lane.origin.getX()+100, lane.origin.getY(), lane.origin.getZ());
            boss.startSeenByPlayer(player);
            ServerBossEvent bar = bar(boss);
            boolean registeredViewer = bar!=null && bar.getPlayers().contains(player);
            if (bars && bar!=null) {
                boss.stopSeenByPlayer(player);
                test.assertTrue(!bar.getPlayers().contains(player), "leaving tracking range removes praetor bar viewer");
                boss.startSeenByPlayer(player);
            }
            if (placement) test.assertTrue(BossPlacementRules.bossNear(level, lane.origin.getX(), lane.origin.getY(), lane.origin.getZ()), "live praetor is found by the placement rule");
            if (boss instanceof EntityEldritchWarden w) test.assertTrue(w.getAbsorptionAmount()>=132, "finalized warden starts with its original ward");
            if (boss instanceof EntityTaintacleGiant giant) {
                float before=giant.getHealth();
                test.assertTrue(giant.hurt(level.damageSources().playerAttack(player),40) && before-giant.getHealth()<=35 && giant.getAnger()==200,
                        "giant caps a heavy hit and enters its original enrage");
            }
            boolean headless = false, frenzy = false, beam = false;
            int hits = 0;
            for (int tick=0; tick<2400 && boss.isAlive(); tick++) {
                if (tick % 20 == 0) {
                    boolean accepted = boss.hurt(level.damageSources().playerAttack(player), 7);
                    if (tick==0 && (kind.equals("golem") || kind.equals("warden"))) test.assertTrue(!accepted, "eldritch spawn protection rejects ordinary damage");
                    if (accepted) hits++;
                    if (boss instanceof EntityEldritchWarden w && tick>150 && w.getAbsorptionAmount()<=0 && !accepted) frenzy=true;
                }
                if (tick==160 && boss instanceof EntityEldritchWarden)
                    test.assertTrue(!boss.hurt(level.damageSources().drown(),7) && !boss.hurt(level.damageSources().wither(),7), "active warden rejects original drown/wither damage");
                if (tick==120 && boss instanceof EntityEldritchGolem)
                    test.assertTrue(!boss.hurt(level.damageSources().onFire(),7), "active golem retains original fire immunity");
                boss.setPos(lane.origin.getX()+.5, lane.origin.getY(), lane.origin.getZ()+.5);
                boss.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                level.tickNonPassenger(boss);
                if (bars && bar!=null) test.assertTrue(Math.abs(bar.getProgress()-boss.getHealth()/boss.getMaxHealth())<.0001F
                        && bar.getName().equals(boss.getDisplayName()), "native bar follows live boss health and name");
                if (boss instanceof EntityEldritchGolem g && g.isHeadless()) { headless=true; if(g.getBeamCharge()>0)beam=true; }
            }
            test.assertTrue(!boss.isAlive() && hits>0, kind+" can be killed by repeated uncapped sword-sized player damage; health="+boss.getHealth());
            if (kind.equals("golem")) test.assertTrue(headless && beam, "golem actually enters headless phase and charges its new beam before death");
            if (kind.equals("warden")) test.assertTrue(frenzy, "warden actually rejects hits during ward-break frenzy and later becomes killable");
            if (kind.equals("warden")) {
                int rings=0;
                net.minecraft.core.BlockPos field=null;
                var observed=new java.util.TreeSet<Integer>();
                for (int x=-4;x<=4;x++) for (int z=-4;z<=4;z++) {
                    if (x==0 && z==0) continue;
                    var state=level.getBlockState(lane.origin.offset(x,0,z));
                    if (state.getBlock() instanceof thaumcraft.common.blocks.BlockAiry) {
                        int metadata=thaumcraft.common.blocks.BlockAiry.variant(state);
                        observed.add(metadata);
                        if (metadata==11) { rings++; field=lane.origin.offset(x,0,z); }
                    }
                }
                test.assertTrue(rings>0, "warden frenzy creates original damaging eldritch-field rings (metadata 11), observed="+observed);
                var victim=lane.add(net.minecraft.world.entity.EntityType.PIG.create(level));
                victim.setPos(field.getX()+.5,field.getY(),field.getZ()+.5);
                victim.setNoAi(true);
                level.addFreshEntity(victim);
                victim.setDeltaMovement(1,.25,1);
                var state=level.getBlockState(field);
                state.entityInside(level,field,victim);
                boolean collision=victim.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                                && victim.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS).getAmplifier()==1
                                && Math.abs(victim.getDeltaMovement().x-.66)<.00001
                                && Math.abs(victim.getDeltaMovement().z-.66)<.00001
                                && victim.getDeltaMovement().y==.25;
                test.assertTrue(level.getBlockTicks().hasScheduledTick(field,state.getBlock()), "native frenzy scheduled field expiry");
                state.tick(level,field,level.random);
                boolean expired=level.getBlockState(field).isAir();
                test.assertTrue(collision && expired, "actual frenzy field applies original drag/Weakness II and scheduled tick clears it: collision="+collision+", expired="+expired);
            }
            if (kind.equals("golem") || kind.equals("warden") || kind.equals("giant"))
                test.assertTrue(lane.dropped(ThaumcraftContent.PRIMORDIAL_PEARL.get())==1, kind+" death drops exactly one pearl");
            if (kind.equals("golem") || kind.equals("warden") || kind.equals("praetor"))
                test.assertTrue(lane.dropped(ThaumcraftLoot.LOOT_BAG_RARE.get())>=1, kind+" death drops its rare bag");
            if (kind.equals("taintacle"))
                test.assertTrue(lane.dropped(ThaumcraftContent.MATERIALS.get("tainted_goo").get())+lane.dropped(ThaumcraftContent.MATERIALS.get("taint_tendril").get())==1, "root death drops goo or tendril");
            // Original EntityUtils.getEntitiesInRange includes a corpse until its removal.
            if (placement) test.assertTrue(BossPlacementRules.bossNear(level, lane.origin.getX(), lane.origin.getY(), lane.origin.getZ()), "original placement lock lasts through death animation");
            for (int tick=0; tick<21 && !boss.isRemoved(); tick++) level.tickNonPassenger(boss);
            test.assertTrue(boss.isRemoved(), kind+" completes native death cleanup");
            if (placement) test.assertTrue(!BossPlacementRules.bossNear(level, lane.origin.getX(), lane.origin.getY(), lane.origin.getZ()), "placement lock clears after native death removal");
            if (bars) {
                test.assertTrue(bar!=null && registeredViewer, kind+" registers a server boss bar");
                test.assertTrue(!bar.getPlayers().contains(player), kind+" removes viewer on death");
            }
        } finally {
            lane.clean();
        }
        test.succeed();
    }

    /** Read-only inspection of server registration; no new production testing API. */
    private static ServerBossEvent bar(Mob mob) {
        for (Class<?> type=mob.getClass(); type!=null; type=type.getSuperclass()) {
            for (Field field:type.getDeclaredFields()) if (field.getType()==ServerBossEvent.class) {
                try { field.setAccessible(true); return (ServerBossEvent)field.get(mob); }
                catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            }
        }
        return null;
    }
}
