package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.lib.loot.OriginalMobTargeting;

/** Actual native AI ticks, attacks, admitted death loot and loaded spawn data.
 * Connected player, supported high floor, bare mobs and declared RNG seeds are fixtures, not survival acquisition. */
public final class FireBatWispRuntimeChecks {
    private FireBatWispRuntimeChecks() {}
    public static void fireBat(GameTestHelper test) { run(test,0); }
    public static void wisp(GameTestHelper test) { run(test,1); }
    public static void population(GameTestHelper test) { run(test,2); }
    private static void run(GameTestHelper test,int group) {
        var fixture=new Fixture(test);
        try { fixture.start();if(group==0)fixture.fireBat();else if(group==1)fixture.wisp();else fixture.population(); }
        finally {fixture.close();}
        test.succeed();
    }
    private static final class Fixture {
        final GameTestHelper test;final ServerLevel level;final BlockPos origin;final AABB bounds;
        final List<Entity> entities=new ArrayList<>();final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();final List<ChunkPos> forced=new ArrayList<>();
        final Connection connection=new Connection(PacketFlow.SERVERBOUND);final EmbeddedChannel channel=new EmbeddedChannel(connection);final ServerPlayer player;
        final boolean oldLoot;
        Fixture(GameTestHelper test) {
            this.test=test;level=test.getLevel();origin=test.absolutePos(new BlockPos(3,90,3));bounds=new AABB(origin).inflate(20);
            oldLoot=level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
            player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"monsters-"+UUID.randomUUID().toString().substring(0,5)),ClientInformation.createDefault());
        }
        void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false));
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-10,-1,-10),origin.offset(10,12,10)))saved.put(pos.immutable(),level.getBlockState(pos));
            forced.addAll(GolemChunks.hold(level,saved.keySet()));
            saved.forEach((pos,state)->level.setBlock(pos,pos.getY()==origin.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2|16));
            player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.teleportTo(origin.getX()+.5,origin.getY(),origin.getZ()-8);
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,level.getServer());
        }
        <T extends Mob> T spawn(EntityType<T> type,int x,int y,int z) {
            T mob=type.create(level);test.assertTrue(mob!=null,"registered native entity "+type);
            mob.setPos(origin.getX()+x+.5,origin.getY()+y,origin.getZ()+z+.5);mob.setNoGravity(true);mob.setNoAi(true);
            // The 1.7 original rolled nextInt100 of the world random for every EntityMob that
            // joined the world (oracle EventHandlerEntity.entitySpawns:391). The fixture declares
            // that draw, so the exact original attributes of the Wisp stay deterministic.
            test.assertTrue(ChampionJoinRoll.join(level,mob),"actual registered hostile enters the native world index");
            entities.add(mob);return mob;
        }
        void clear() {entities.forEach(Entity::discard);entities.clear();drops().forEach(Entity::discard);player.removeAllEffects();}
        List<ItemEntity> drops() {return level.getEntitiesOfClass(ItemEntity.class,bounds);}
        int amount(net.minecraft.world.item.Item item) {return drops().stream().map(ItemEntity::getItem).filter(stack->stack.is(item)).mapToInt(ItemStack::getCount).sum();}
        EntityFireBat bat() {return spawn(ThaumcraftMonsters.FIREBAT.get(),0,1,0);}
        EntityWisp wispEntity() {return spawn(ThaumcraftMonsters.WISP.get(),0,1,0);}
        void fireBat() {
            var bat=bat();test.assertTrue(bat.getMaxHealth()==5 && bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==1 && bat.getIsBatHanging() && bat.fireImmune(),"natural firebat starts hanging, health5, damage1, fire immunity");
            test.assertTrue(Math.abs(bat.getBbWidth()-.5F)<.0001 && Math.abs(bat.getBbHeight()-.9F)<.0001 && !bat.isPushable(),"original firebat box and no pushing");
            bat.damBonus=3;bat.setIsSummoned(true);test.assertTrue(bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==5,"summoned normal damage2+bonus");
            bat.setIsDevil(true);test.assertTrue(bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==6 && bat.getMaxHealth()==5,"Devil changes summoned damage but retains original constructor's health5 quirk");
            bat.setIsDevil(false);test.assertTrue(bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==6,"clearing Devil alone does not reset original attack attribute");
            bat.setIsSummoned(false);test.assertTrue(bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"clearing Summoned resets damage1");
            bat.damBonus=130;bat.setIsSummoned(true);bat.setIsDevil(true);bat.setIsExplosive(true);bat.setIsVampire(true);bat.owner=player;
            CompoundTag state=new CompoundTag();bat.addAdditionalSaveData(state);var loaded=bat();loaded.readAdditionalSaveData(state);
            test.assertTrue(loaded.getIsBatHanging() && loaded.getIsSummoned() && loaded.getIsDevil() && loaded.getIsExplosive() && loaded.getIsVampire()
                            && loaded.damBonus==-126 && loaded.owner==null && loaded.getAttributeValue(Attributes.ATTACK_DAMAGE)==133,
                    "all BatFlags, signed-byte damBonus and native saved attributes persist; original owner does not");
            clear();bat=bat();test.assertTrue(!bat.hurt(level.damageSources().onFire(),99) && !bat.hurt(level.damageSources().explosion(player,player),99) && bat.getIsBatHanging(),"fire/explosion refusal does not wake a hanging bat");
            test.assertTrue(bat.hurt(level.damageSources().playerAttack(player),1) && !bat.getIsBatHanging() && bat.getTarget()==player,"accepted damage wakes and retaliates through native hurt");
            bat.causeFallDamage(100,1,level.damageSources().fall());test.assertTrue(bat.getHealth()==4,"firebat ignores falling damage");
            clear();bat=bat();bat.setIsBatHanging(true);bat.setPos(bat.getX(),origin.getY()+1.35,bat.getZ());bat.setDeltaMovement(.3,.4,.5);bat.tick();
            test.assertTrue(bat.getDeltaMovement().equals(Vec3.ZERO) && Math.abs(bat.getY()-(origin.getY()+2-.9F))<.0001,"hanging tick anchors ceiling and clears all motion");
            clear();bat=bat();bat.setNoAi(false);bat.tick();test.assertTrue(!bat.getIsBatHanging(),"native hanging AI wakes when its ceiling block is absent");
            clear();bat=bat();level.setBlock(origin.above(),Blocks.WATER.defaultBlockState(),2|16);level.setBlock(origin.above(2),Blocks.WATER.defaultBlockState(),2|16);bat.tick();
            test.assertTrue(bat.getHealth()==4 && !bat.getIsBatHanging(),"real water state applies drown1 and wakes the hanging firebat");
            level.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),2|16);level.setBlock(origin.above(2),Blocks.AIR.defaultBlockState(),2|16);
            clear();bat=bat();bat.setIsBatHanging(false);bat.setIsSummoned(true);bat.setNoAi(false);bat.tick();test.assertTrue(bat.getHealth()==3,"native summoned AI without a target pays original generic self damage2");
            clear();var pig=spawn(EntityType.PIG,1,1,0);bat=bat();bat.damBonus=2;bat.setIsSummoned(true);bat.setIsDevil(true);bat.setIsVampire(true);bat.owner=player;bat.setHealth(2);bat.setIsBatHanging(false);bat.setTarget(pig);bat.setNoAi(false);
            Vec3 motion=new Vec3(.12,.04,-.07);pig.setDeltaMovement(motion);float health=pig.getHealth();bat.tick();
            test.assertTrue(pig.getHealth()==health-5 && pig.getDeltaMovement().equals(motion) && !pig.hurtMarked,"native Vampire melee uses damage5 while restoring original target velocity/mark");
            test.assertTrue(pig.lastHurtByPlayerTime==100 && player.hasEffect(MobEffects.REGENERATION) && player.getEffect(MobEffects.REGENERATION).getAmplifier()==1
                            && player.getEffect(MobEffects.REGENERATION).getDuration()==26 && bat.getHealth()==3,"summoned recent-hit mark and Vampire owner regeneration26/1 plus self heal1");
            for(int i=0;i<19;i++){pig.setDeltaMovement(Vec3.ZERO);pig.tick();pig.setPos(origin.getX()+1.5,origin.getY()+1,origin.getZ()+.5);bat.setPos(origin.getX()+.5,origin.getY()+1,origin.getZ()+.5);bat.setDeltaMovement(Vec3.ZERO);bat.tick();}
            test.assertTrue(pig.getHealth()==health-5,"actual intervening19 native AI ticks cannot bypass attack cooldown20");
            bat.setPos(origin.getX()+.5,origin.getY()+1,origin.getZ()+.5);bat.setDeltaMovement(Vec3.ZERO);bat.tick();test.assertTrue(pig.isDeadOrDying(),"twentieth native AI tick permits the next full damage5 melee");
            clear();pig=spawn(EntityType.PIG,1,1,0);bat=bat();bat.setIsBatHanging(false);bat.setIsSummoned(true);bat.setIsDevil(true);bat.setTarget(pig);bat.setNoAi(false);level.random.setSeed(fireSeed(false,false));bat.tick();
            test.assertTrue(pig.getRemainingFireTicks()>=80 && pig.getHealth()==10 && !bat.isRemoved(),"actual AI ignite branch is four seconds for summoned non-Vampire, without melee damage or Devil explosion");
            clear();pig=spawn(EntityType.PIG,1,1,0);bat=bat();bat.setIsBatHanging(false);bat.setTarget(pig);bat.setNoAi(false);level.random.setSeed(fireSeed(false,false));bat.tick();
            test.assertTrue(pig.getRemainingFireTicks()>=40 && pig.getHealth()==10 && !bat.isRemoved(),"natural non-exploding ignite branch retains original two-second fire");
            clear();pig=spawn(EntityType.PIG,1,1,0);bat=bat();bat.setIsBatHanging(false);bat.setTarget(pig);bat.setNoAi(false);level.random.setSeed(fireSeed(false,true));bat.tick();
            test.assertTrue(pig.getHealth()==9 && !bat.isRemoved(),"natural non-exploding melee branch uses actual damage1");
            clear();pig=spawn(EntityType.PIG,1,1,0);pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);pig.setHealth(200);bat=bat();bat.setIsBatHanging(false);bat.setTarget(pig);bat.setNoAi(false);level.random.setSeed(fireSeed(true,false));bat.tick();
            test.assertTrue(bat.isRemoved() && pig.getHealth()<200,"natural one-in10 melee explosion branch consumes its bat");
            clear();pig=spawn(EntityType.PIG,1,1,0);pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);pig.setHealth(200);bat=bat();bat.damBonus=3;bat.setIsSummoned(true);bat.setIsExplosive(true);bat.setIsBatHanging(false);bat.setTarget(pig);bat.setNoAi(false);bat.tick();
            test.assertTrue(bat.isRemoved() && pig.getHealth()<200 && level.getBlockState(origin.below()).is(Blocks.STONE),"explosive actual AI damages target, consumes bat and preserves terrain");
            clear();int powder=0;
            for(int i=0;i<6;i++) {bat=bat();bat.getRandom().setSeed(i);bat.hurt(level.damageSources().generic(),1000);powder+=amount(Items.GUNPOWDER);drops().forEach(Entity::discard);}
            test.assertTrue(powder>0 && powder<=12,"actual natural firebat deaths emit original gunpowder0..2");
            clear();bat=bat();bat.setIsSummoned(true);bat.hurt(level.damageSources().generic(),1000);test.assertTrue(drops().isEmpty(),"summoned firebat death emits no gunpowder or replacement loot");
        }
        static long fireSeed(boolean explosion,boolean melee) {
            for(long seed=0;seed<10000;seed++) {var random=RandomSource.create(seed);if((random.nextInt(10)==0)==explosion && random.nextBoolean()==melee)return seed;}
            throw new AssertionError("No firebat fixture seed");
        }
        static long zapSeed(boolean moving,boolean hit) {
            for(long seed=0;seed<10000;seed++) {var random=RandomSource.create(seed);random.nextInt(1000);if((random.nextFloat()<(moving?.4F:.66F))==hit)return seed;}
            throw new AssertionError("No wisp fixture seed");
        }
        void wisp() {
            var wisp=wispEntity();test.assertTrue(wisp.getMaxHealth()==22 && wisp.getAttributeValue(Attributes.ATTACK_DAMAGE)==3 && Math.abs(wisp.getBbWidth()-.9F)<.0001,"native original Wisp22 health/3 damage/.9 box");
            wisp.setAspectTag(Aspect.FIRE.getTag());CompoundTag tag=new CompoundTag();wisp.addAdditionalSaveData(tag);var loaded=wispEntity();loaded.readAdditionalSaveData(tag);
            test.assertTrue(tag.getString("Type").equals(Aspect.FIRE.getTag()) && loaded.getAspectTag().equals(Aspect.FIRE.getTag()),"original Type String persists native synchronized aspect");
            wisp.setInvulnerable(true);test.assertTrue(!wisp.hurt(level.damageSources().playerAttack(player),1) && wisp.getAttackTarget()==player,"even refused native damage sets original retaliation target before parent hurt");
            clear();wisp=wispEntity();wisp.setAspectTag("invalid");wisp.waypointX=wisp.getX();wisp.waypointY=wisp.getY();wisp.waypointZ=wisp.getZ();wisp.setNoAi(false);wisp.tick();
            test.assertTrue(Aspect.getAspect(wisp.getAspectTag())!=null && Double.isFinite(wisp.getDeltaMovement().x) && Double.isFinite(wisp.getDeltaMovement().y) && Double.isFinite(wisp.getDeltaMovement().z),"actual native AI initializes aspect and safely handles original zero-vector waypoint NaN defect");
            for(boolean moving:new boolean[]{false,true})for(boolean hit:new boolean[]{false,true}) {
                clear();var pig=spawn(EntityType.PIG,2,1,0);wisp=wispEntity();wisp.setAspectTag(Aspect.AIR.getTag());wisp.hurt(level.damageSources().mobAttack(pig),0);
                wisp.attackCounter=19;wisp.courseChangeCooldown=10;wisp.setNoAi(false);pig.setDeltaMovement(moving?new Vec3(.101,0,0):Vec3.ZERO);
                long seed=zapSeed(moving,hit);var expected=RandomSource.create(seed);expected.nextInt(1000);expected.nextFloat();int counter=-20+expected.nextInt(20);level.random.setSeed(seed);
                float before=pig.getHealth();wisp.tick();
                test.assertTrue(pig.getHealth()==before-(hit?(moving?3:4):0) && wisp.attackCounter==counter && wisp.prevAttackCounter==19,
                        "actual twentieth-tick zap movement="+moving+" hit="+hit+" retains exact damage/chance/cooldown and RNG draw order");
            }
            clear();var pig=spawn(EntityType.PIG,2,1,0);wisp=wispEntity();wisp.setAspectTag(Aspect.AIR.getTag());wisp.hurt(level.damageSources().mobAttack(pig),0);wisp.attackCounter=7;wisp.courseChangeCooldown=10;wisp.setNoAi(false);
            for(int y=0;y<4;y++)level.setBlock(origin.offset(1,y,0),Blocks.STONE.defaultBlockState(),2|16);wisp.tick();
            test.assertTrue(wisp.attackCounter==6 && pig.getHealth()==10,"blocked native line of sight decays a positive charge without damage");
            for(int y=0;y<4;y++)level.setBlock(origin.offset(1,y,0),Blocks.AIR.defaultBlockState(),2|16);
            wisp.attackCounter=7;pig.setPos(origin.getX()+20,origin.getY()+1,origin.getZ());wisp.tick();test.assertTrue(wisp.attackCounter==6 && pig.getHealth()==10,"target beyond sixteen blocks decays charge without a zap");
            clear();wisp=wispEntity();wisp.setAspectTag(Aspect.ELDRITCH.getTag());wisp.hurt(level.damageSources().generic(),1000);
            test.assertTrue(amount(ThaumcraftEssences.WISP_ESSENCE.get())==1,"actual environmental Wisp death emits one essence without recent-player gate");
            var essence=drops().stream().filter(item->item.getItem().is(ThaumcraftEssences.WISP_ESSENCE.get())).findFirst().orElseThrow();
            var aspects=ThaumcraftEssences.WISP_ESSENCE.get().getAspects(essence.getItem());test.assertTrue(aspects.size()==1 && aspects.getAmount(Aspect.ELDRITCH)==2,"emitted Wisp essence carries its own exact aspect2");
            clear();wisp=wispEntity();wisp.setAspectTag(Aspect.AIR.getTag());level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,level.getServer());wisp.hurt(level.damageSources().generic(),1000);test.assertTrue(drops().isEmpty(),"native doMobLoot=false gates Wisp custom output");
        }
        void population() {
            var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
            for(var key:List.of(Biomes.NETHER_WASTES,Biomes.CRIMSON_FOREST,Biomes.WARPED_FOREST,Biomes.SOUL_SAND_VALLEY,Biomes.BASALT_DELTAS)) {
                var mobs=registry.get(key).getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
                test.assertTrue(mobs.stream().anyMatch(e->e.type==ThaumcraftMonsters.FIREBAT.get() && e.minCount==1 && e.maxCount==2 && e.getWeight().asInt()==10),"loaded Nether Firebat10/1..2 "+key);
                test.assertTrue(mobs.stream().anyMatch(e->e.type==ThaumcraftMonsters.WISP.get() && e.minCount==1 && e.maxCount==1 && e.getWeight().asInt()==5),"loaded Nether Wisp5/1 "+key);
            }
            long time=level.getDayTime();
            try {
                // The original light rule is a ROLL: the saved sky light of the cell against
                // nextInt(32) and the block light against nextInt(8). A bright cell is refused
                // by some seeds and admitted by the others, exactly as the original does.
                level.setDayTime(6000);level.updateSkyBrightness();
                int refused=0;for(long seed=0;seed<64;++seed){
                    if(!SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.FIREBAT.get(),level,MobSpawnType.NATURAL,origin,RandomSource.create(seed))
                            ||!SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.WISP.get(),level,MobSpawnType.NATURAL,origin,RandomSource.create(seed)))refused++;}
                test.assertTrue(refused>0&&refused<64,"both registered placement predicates follow the original32 sky roll, not a daylight switch: "+refused+" of 64 refused");
                level.setDayTime(18000);level.updateSkyBrightness();boolean batAllowed=false,wispAllowed=false;
                for(int seed=0;seed<1000;seed++) {batAllowed|=SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.FIREBAT.get(),level,MobSpawnType.NATURAL,origin,RandomSource.create(seed));wispAllowed|=SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.WISP.get(),level,MobSpawnType.NATURAL,origin,RandomSource.create(seed));}
                test.assertTrue(batAllowed && wispAllowed,"both native placement predicates admit dark air without invented ground requirement");
            } finally {level.setDayTime(time);level.updateSkyBrightness();}
            var wisp=wispEntity();test.assertTrue(wisp.getMaxSpawnClusterSize()==2 && wisp.checkSpawnObstruction(level),"one Wisp permits native unobstructed spawn, original cluster cap2");
            for(int i=0;i<7;i++)spawn(ThaumcraftMonsters.WISP.get(),i-4,2,3);
            test.assertTrue(!wisp.checkSpawnObstruction(level),"original eight-Wisp neighborhood cap includes all nearby variants");
            var crowdedRandom=RandomSource.create(7183);
            test.assertTrue(!SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.WISP.get(),level,MobSpawnType.NATURAL,origin,crowdedRandom)
                            && crowdedRandom.nextLong()==RandomSource.create(7183).nextLong(),
                    "actual registered placement rejects eight existing Wisps BEFORE consuming either original light random draw");
            clear();var bat=bat();bat.setPos(player.getX(),player.getY(),player.getZ()+8.01);
            player.setShiftKeyDown(false);test.assertTrue(OriginalMobTargeting.closestVulnerablePlayer(bat,10)==player,"vulnerable ordinary player inside original range is found");
            player.setShiftKeyDown(true);test.assertTrue(OriginalMobTargeting.closestVulnerablePlayer(bat,10)==null,"sneaking range .8 excludes8.01 blocks of a ten-block base range");player.setShiftKeyDown(false);
            player.setInvisible(true);test.assertTrue(OriginalMobTargeting.closestVulnerablePlayer(bat,10)==null,"invisible unarmored player uses original .7*max(.1,armor-cover) range");player.setInvisible(false);
            bat.setIsBatHanging(false);bat.setNoAi(false);bat.tick();test.assertTrue(bat.getTarget()==player,"ordinary native Firebat AI acquires a vulnerable player inside its original twelve-block range");
            clear();wisp=wispEntity();wisp.setAspectTag(Aspect.AIR.getTag());wisp.courseChangeCooldown=10;wisp.setNoAi(false);
            long acquireSeed=-1;for(long seed=0;seed<100000;seed++)if(RandomSource.create(seed).nextInt(1000)==0){acquireSeed=seed;break;}
            test.assertTrue(acquireSeed>=0,"bounded declared Wisp acquisition seed found");level.random.setSeed(acquireSeed);wisp.tick();
            test.assertTrue(wisp.getAttackTarget()==player,"native Wisp AI's one-in1000 acquisition branch chooses a vulnerable player within16");
        }
        void close() {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(oldLoot,level.getServer());clear();
            saved.forEach((pos,state)->level.setBlock(pos,state,2|16));for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
            try {if(level.getServer().getPlayerList().getPlayer(player.getUUID())==player)level.getServer().getPlayerList().remove(player);}
            finally {player.discard();channel.finishAndReleaseAll();}
        }
    }
}
