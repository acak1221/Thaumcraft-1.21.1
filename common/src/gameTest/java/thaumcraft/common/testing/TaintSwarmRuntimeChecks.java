package thaumcraft.common.testing;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** Actual native factory, AI/bites, flags/attributes, death loot and no-floor spawn predicates.
 * Physical actors/terrain, vector and RNG branch inputs are declared fixtures, never progression acquisition/client evidence. */
public final class TaintSwarmRuntimeChecks {
    private TaintSwarmRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try(var check=new Check(test)) { check.run(); }
        test.succeed();
    }
    private static final class Check implements AutoCloseable {
        final Fixture f;final BlockPos origin;final AABB bounds;final List<Entity> entities=new ArrayList<>();
        final List<ChunkPos> forced=new ArrayList<>();final Set<UUID> originalDrops=new HashSet<>(),ownedDrops=new HashSet<>();final boolean oldLoot;
        Check(GameTestHelper test) {
            f=new Fixture(test);origin=f.base.above(140);bounds=new AABB(origin).inflate(5);
            oldLoot=f.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
            try {
                f.open();forced.addAll(GolemChunks.hold(f.level,List.of(origin)));
                for(BlockPos point:BlockPos.betweenClosed(origin.offset(-5,-2,-5),origin.offset(5,7,5))) {
                    BlockPos pos=point.immutable();f.states.putIfAbsent(pos,f.level.getBlockState(pos));
                    if(f.level.getBlockEntity(pos)!=null)f.originals.putIfAbsent(pos,f.level.getBlockEntity(pos));
                    f.level.setBlock(pos,pos.getY()==origin.getY()-2?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2|16);
                }
                for(var drop:f.level.getEntitiesOfClass(ItemEntity.class,bounds))originalDrops.add(drop.getUUID());
                f.level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,f.level.getServer());farPlayer();
            } catch(RuntimeException|Error failure) {close();throw failure;}
        }
        void farPlayer() {f.player.setPos(origin.getX()+32,origin.getY()+1,origin.getZ()+32);f.player.setDeltaMovement(Vec3.ZERO);}
        <T extends Mob> T spawn(EntityType<T> type,double x,double y,double z) {
            T mob=type.create(f.level);f.check(mob!=null,"real registered native entity factory "+type);
            mob.setPos(origin.getX()+x,origin.getY()+y,origin.getZ()+z);mob.setNoAi(true);mob.setNoGravity(true);mob.setSilent(true);
            f.level.addFreshEntity(mob);entities.add(mob);return mob;
        }
        EntityTaintSwarm swarm() {return spawn(ThaumcraftTaint.TAINT_SWARM.get(),.25,0,.25);}
        void clear() {entities.forEach(Entity::discard);entities.clear();for(UUID id:ownedDrops)if(f.level.getEntity(id) instanceof ItemEntity item)item.discard();ownedDrops.clear();farPlayer();}
        List<ItemEntity> drops() {var values=f.level.getEntitiesOfClass(ItemEntity.class,bounds).stream().filter(item->!originalDrops.contains(item.getUUID())).toList();values.forEach(item->ownedDrops.add(item.getUUID()));return values;}
        void run() {
            EntityTaintSwarm swarm=swarm();
            f.check(swarm instanceof ITaintedMob&&swarm.getBbWidth()==2&&swarm.getBbHeight()==2&&swarm.getMaxHealth()==30
                            &&swarm.getAttributeValue(Attributes.ATTACK_DAMAGE)==2&&!swarm.isPushable()&&swarm.getLightLevelDependentMagicValue()==1,
                    "real original swarm is2x2, health30/damage2, tainted, fullbright and unpushable");
            for(BlockPos water:BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(1,2,1)))f.level.setBlock(water,Blocks.WATER.defaultBlockState(),2|16);
            swarm.setAirSupply(30);for(int tick=0;tick<25;tick++)swarm.tick();
            f.check(swarm.getHealth()==30&&swarm.getAirSupply()>=30&&!swarm.causeFallDamage(100,1,f.level.damageSources().fall()),"actual underwater swarm breathes without air loss and has no invented fall injury");
            for(BlockPos water:BlockPos.betweenClosed(origin.offset(-1,0,-1),origin.offset(1,2,1)))f.level.setBlock(water,Blocks.AIR.defaultBlockState(),2|16);
            var saved=new CompoundTag();swarm.damBonus=130;swarm.addAdditionalSaveData(saved);saved.putByte("Flags",(byte)0xA7);
            EntityTaintSwarm loaded=swarm();loaded.readAdditionalSaveData(saved);loaded.setIsSummoned(false);var after=new CompoundTag();loaded.addAdditionalSaveData(after);
            f.check(loaded.damBonus==-126&&after.getByte("Flags")==((byte)0xA4)&&loaded.getAttributeValue(Attributes.ATTACK_DAMAGE)==2,
                    "original Flags false clears lowTWO bits only; damBonus signedBYTE130→−126 does not recalculate the original saved attack attribute");
            loaded.setIsSummoned(true);f.check(loaded.getIsSummoned(),"real synchronized bit1 can re-enable summoning");clear();

            swarm=swarm();var control=spawn(EntityType.PIG,4,0,4);Vec3 motion=new Vec3(.3,.4,.5);swarm.setDeltaMovement(motion);control.setDeltaMovement(motion);
            swarm.tick();control.tick();f.check(Math.abs(swarm.getDeltaMovement().y-control.getDeltaMovement().y*(double).6F)<1E-12,
                    "actual whole native server tick damps vanilla post-movement Y by original FLOAT.6; inherited X/Z remain native physics");clear();
            swarm=swarm();var victim=spawn(EntityType.PIG,4,1,-4);swarm.setTarget(victim);swarm.setDeltaMovement(.2,-.3,.4);
            action(swarm);Vec3 chased=swarm.getDeltaMovement();
            f.check(Math.abs(chased.x-(.2+(.5-.2)*.025000000149011613D))<1E-14
                            &&Math.abs(chased.y-(-.3+((double).7F+.3)*(double).1F))<1E-14
                            &&Math.abs(chased.z-(.4+(-.5-.4)*.02500000001490116D))<1E-14,
                    "real legacy action retains asymmetric X/Z chase double constants and promoted-float vertical coefficient");clear();
            swarm=swarm();action(swarm);Vec3 idle=swarm.getDeltaMovement();
            f.check(Math.abs(Math.abs(idle.x)-.5*.015000000014901161D)<1E-14&&Math.abs(Math.abs(idle.z)-.5*.015000000014901161D)<1E-14
                            &&Math.abs(Math.abs(idle.y)-(double).7F*(double).1F)<1E-14,
                    "actual targetless old Creature+flight action produces original idle.015000000014901161 doubles, not a generic flying goal");clear();

            swarm=swarm();swarm.setIsSummoned(true);swarm.setNoAi(false);swarm.tick();
            f.check(swarm.getHealth()==25&&swarm.getTarget()==null,"actual summoned targetless AI attempts original generic self-damage5 and never acquires ordinary nearby players");clear();
            swarm=swarm();f.player.setPos(swarm.getX()+8,swarm.getY(),swarm.getZ());swarm.setNoAi(false);swarm.tick();
            f.check(swarm.getTarget()==f.player,"actual natural old Creature action acquires vulnerable player inside strict12block radius");clear();
            swarm=swarm();f.player.setPos(swarm.getX()+8,swarm.getY(),swarm.getZ());f.mode(GameType.CREATIVE);swarm.setNoAi(false);swarm.tick();
            f.check(swarm.getTarget()==null,"native creative/invulnerable player is not an invented swarm target");f.mode(GameType.SURVIVAL);clear();

            swarm=swarm();victim=spawn(EntityType.PIG,2,0,.25);swarm.setIsSummoned(true);swarm.setTarget(victim);swarm.setNoAi(false);
            Vec3 preserved=new Vec3(.12,.04,-.07);victim.setDeltaMovement(preserved);swarm.tick();
            f.check(victim.getHealth()==8&&victim.hasEffect(MobEffects.WEAKNESS)&&victim.getEffect(MobEffects.WEAKNESS).getDuration()==100
                            &&victim.getEffect(MobEffects.WEAKNESS).getAmplifier()==0&&victim.lastHurtByPlayerTime==100&&victim.getDeltaMovement().equals(preserved)&&!victim.hurtMarked,
                    "actual summoned native bite deals2, Weakness100/0, recently-hit100 and restores original victim velocity/mark after real damage");
            for(int tick=1;tick<=9;tick++){resetBite(swarm,victim);swarm.tick();}
            f.check(victim.getHealth()==8,"nine actual intervening AI ticks cannot bypass original minimum10 bite cooldown");
            for(int tick=10;tick<=14;tick++){resetBite(swarm,victim);swarm.tick();}
            f.check(victim.getHealth()==6,"bounded original10..14 cooldown admits exactly one second real bite by tick14");clear();
            swarm=swarm();victim=spawn(EntityType.PIG,.25,2,.25);swarm.setTarget(victim);swarm.setNoAi(false);swarm.tick();
            f.check(victim.getHealth()==10&&!victim.hasEffect(MobEffects.WEAKNESS),"strict nonoverlapping vertical boundary rejects a nearby bite before damage/effect");clear();
            swarm=swarm();victim=spawn(EntityType.PIG,2,0,.25);victim.setInvulnerable(true);victim.setDeltaMovement(preserved);swarm.setTarget(victim);swarm.setIsSummoned(true);swarm.setNoAi(false);swarm.tick();
            f.check(victim.getHealth()==10&&!victim.hasEffect(MobEffects.WEAKNESS)&&victim.lastHurtByPlayerTime==100&&victim.getDeltaMovement().equals(preserved)&&!victim.hurtMarked,
                    "refused native damage grants no Weakness but retains original preattack recent-hit mark and no-knockback cleanup");clear();
            swarm=swarm();victim=spawn(EntityType.PIG,2,0,.25);swarm.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,0));swarm.setTarget(victim);swarm.setNoAi(false);swarm.tick();
            f.check(Math.abs(victim.getHealth()-5.4F)<1E-5,"actual bite retains original Strength130-percent attribute multiplier: damage2×2.3=4.6, not native modern+3");clear();
            loot();spawnRules();
        }
        void resetBite(EntityTaintSwarm swarm,LivingEntity target) {target.tick();swarm.setPos(origin.getX()+.25,origin.getY(),origin.getZ()+.25);swarm.setDeltaMovement(Vec3.ZERO);target.setPos(origin.getX()+2,origin.getY(),origin.getZ()+.25);target.setDeltaMovement(Vec3.ZERO);}
        void loot() {
            for(boolean expected:new boolean[]{false,true}) {
                long seed=0;while(seed<100000&&RandomSource.create(seed).nextBoolean()!=expected)seed++;f.check(seed<100000,"declared bounded original worldBoolean death vector exists");
                EntityTaintSwarm swarm=swarm();var draw=RandomSource.create(seed);draw.nextBoolean();f.level.random.setSeed(seed);swarm.hurt(f.level.damageSources().generic(),1000);
                var actual=drops();f.check(actual.size()==(expected?1:0)&&actual.stream().allMatch(item->item.getItem().is(ThaumcraftContent.MATERIALS.get("tainted_goo").get())&&item.getItem().getCount()==1)
                                &&f.level.random.nextLong()==draw.nextLong(),"actual environmental swarm death keeps original worldBoolean50-percent ONEgoo, no tendril/fake roll or ItemEntity sound RNG drift: "+expected);clear();
            }
            EntityTaintSwarm swarm=swarm();f.level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,f.level.getServer());swarm.hurt(f.level.damageSources().generic(),1000);
            f.check(drops().isEmpty(),"native doMobLoot=false refuses the actual original swarm death output");f.level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,f.level.getServer());clear();
        }
        void spawnRules() {
            long time=f.level.getDayTime();
            try {
                f.level.setDayTime(6000);f.level.updateSkyBrightness();var actual=RandomSource.create(1710);var expected=RandomSource.create(1710);expected.nextInt(7);
                f.check(!SpawnPlacements.checkSpawnRules(ThaumcraftTaint.TAINT_SWARM.get(),f.level,MobSpawnType.NATURAL,origin,actual)&&actual.nextLong()==expected.nextLong(),
                        "registered actual bright-day swarm spawn rejects after FIRST original raw-light nextInt7 without inherited32/8 draws");
                f.level.setDayTime(18000);f.level.updateSkyBrightness();boolean admitted=false;
                for(long seed=0;seed<1000;seed++)admitted|=SpawnPlacements.checkSpawnRules(ThaumcraftTaint.TAINT_SWARM.get(),f.level,MobSpawnType.NATURAL,origin,RandomSource.create(seed));
                EntityTaintSwarm swarm=swarm();f.check(admitted&&f.level.isEmptyBlock(origin.below())&&swarm.checkSpawnObstruction(f.level),
                        "real registered predicate/collision admits dark unsupported air; modern monster floor requirement is not original swarm gameplay");
            } finally {f.level.setDayTime(time);f.level.updateSkyBrightness();}
        }
        @Override public void close() {
            try {clear();f.level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(oldLoot,f.level.getServer());f.close();}
            finally {for(var chunk:forced)f.level.setChunkForced(chunk.x,chunk.z,false);}
        }
    }
    private static void action(EntityTaintSwarm swarm) {
        try {var action=EntityTaintSwarm.class.getDeclaredMethod("customServerAiStep");action.setAccessible(true);action.invoke(swarm);}
        catch(InvocationTargetException failure) {if(failure.getCause() instanceof RuntimeException runtime)throw runtime;if(failure.getCause() instanceof Error error)throw error;throw new IllegalStateException(failure.getCause());}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot invoke actual protected native swarm action",failure);}
    }
}
