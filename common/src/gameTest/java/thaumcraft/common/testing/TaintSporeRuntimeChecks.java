package thaumcraft.common.testing;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.entities.monster.EntityTaintSpore;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.lib.world.BiomeColumns;

/** Declared owned native entity/biome inputs, actual server tick machinery,
 * physical damage/death/drop/burst. No natural taint acquisition/client claim. */
public final class TaintSporeRuntimeChecks {
    private TaintSporeRuntimeChecks(){}
    private static FocusUpgradeRuntimeChecks.Actor actor(GameTestHelper test){return new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,200,4));}
    private static void cleanup(FocusUpgradeRuntimeChecks.Actor a){a.level.getEntitiesOfClass(Mob.class,a.bounds,e->e instanceof EntityTaintSpider||e instanceof EntityTaintSpore||e instanceof EntityTaintSwarm).forEach(Mob::discard);a.purgeOutputs();}
    private static void anchor(FocusUpgradeRuntimeChecks.Actor a){a.put(a.root.below(),ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,4));}
    private static EntityTaintSpore spore(FocusUpgradeRuntimeChecks.Actor a,int size){var spore=ThaumcraftTaint.TAINT_SPORE.get().create(a.level);spore.setSporeSize(size);spore.moveTo(a.root.getX()+.5,a.root.getY(),a.root.getZ()+.5,0,0);a.require(a.level.addFreshEntity(spore),"actual registered native spore enters its owned loaded lane");return spore;}
    private static void steps(FocusUpgradeRuntimeChecks.Actor a,Mob mob,int count){for(int i=0;i<count;i++)a.level.tickNonPassenger(mob);}
    public static void growthAndBurst(GameTestHelper test){
        try(var a=actor(test);var column=new Column(a.level,a.root)){
            try {
                a.clearLane();anchor(a);a.require(BlockTaintFibres.isTaintBiome(a.level,a.root),"actual stored native Taint biome column supports the spore; no false biome provider");
                var spore=spore(a,2);a.require(spore.getHealth()==1&&spore.getAttributeValue(Attributes.ATTACK_DAMAGE)==1&&spore.getBbWidth()==.5F&&spore.getEyeHeight()==.5F*.85F&&spore.getPickRadius()==0&&!spore.isPushable()&&spore.isPickable(),"actual default size2 uses half-block body/inherited.85eye and original health/damage/pick/push contract");
                Vec3 before=spore.position();spore.move(MoverType.SELF,new Vec3(2,2,2));a.require(spore.position().equals(before),"actual fibres4 anchors ALL native movement");
                steps(a,spore,1200);a.require(spore.getSporeSize()==2&&spore.isAlive(),"1200 actual native server entity updates retain original size2 in actual Taint biome");
                CompoundTag saved=spore.saveWithoutId(new CompoundTag());a.require(saved.getInt("Size")==1&&!saved.contains("growth"),"native persistence writes original size-minus-one and omits runtime growth");
                steps(a,spore,1);a.require(spore.getSporeSize()==3,"the original postincrement grows on update1201");spore.discard();
                saved.remove("UUID");var reloaded=ThaumcraftTaint.TAINT_SPORE.get().create(a.level);reloaded.load(saved);a.require(a.level.addFreshEntity(reloaded),"native size snapshot reload owns a fresh entity identity");
                steps(a,reloaded,1);a.require(reloaded.getSporeSize()==2,"actual reload restarts growth instead of persisting the previous1200 counter");steps(a,reloaded,1199);a.require(reloaded.getSporeSize()==2,"reloaded counter1200 still has not grown");steps(a,reloaded,1);a.require(reloaded.getSporeSize()==3,"reloaded update1201 grows again");
                reloaded.setSporeSize(6);var draws=new ArrayList<Object>();draws.add(.5F);draws.add(new Roll(4,3));for(int i=0;i<30;i++)draws.add(.5F);
                try(var random=new WorldInput(a.level,draws)){reloaded.playerTouch(a.player);random.complete();}
                var spiders=a.level.getEntitiesOfClass(EntityTaintSpider.class,a.bounds);a.require(reloaded.isRemoved()&&spiders.size()==5&&a.level.getBlockState(a.root.below()).getValue(BlockTaintFibres.TYPE)==3&&a.items().isEmpty(),"actual contact rupture creates witnessed5spiders, restores only its support4to3 and has no death loot");
                for(var spider:spiders)a.require(spider.getX()==a.root.getX()+.5&&spider.getY()==a.root.getY()+.5&&spider.getZ()==a.root.getZ()+.5&&spider.getYRot()==180&&spider.getHealth()==5&&spider.getAttributeValue(Attributes.ATTACK_DAMAGE)==2,"actual burst child retains original six world draws/position/yaw and real tiny-spider attributes");
                reloaded.playerTouch(a.player);a.require(a.level.getEntitiesOfClass(EntityTaintSpider.class,a.bounds).size()==5,"a removed original spore cannot replay its already claimed physical burst");
                var unregistered=ThaumcraftTaint.TAINT_SPORE.get().create(a.level);unregistered.moveTo(a.root.getX(),a.root.getY(),a.root.getZ(),0,0);unregistered.playerTouch(a.player);a.require(!unregistered.isRemoved()&&a.level.getEntitiesOfClass(EntityTaintSpider.class,a.bounds).size()==5,"a detached never-installed entity cannot authorize world child/material writes");
            }finally{cleanup(a);}
        }test.succeed();
    }
    public static void swarmerBoundaryAndLoot(GameTestHelper test){
        try(var a=actor(test);var column=new Column(a.level,a.root)){
            try {
                a.clearLane();anchor(a);var swarmer=ThaumcraftTaint.TAINT_SWARMER.get().create(a.level);swarmer.moveTo(a.root.getX()+.5,a.root.getY(),a.root.getZ()+.5,0,0);a.require(a.level.addFreshEntity(swarmer),"actual registered swarmer enters the owned lane");
                a.require(swarmer.getSporeSize()==10&&swarmer.getBbWidth()==1&&swarmer.getBbHeight()==1&&swarmer.getEyeHeight()==.85F&&swarmer.getMaxHealth()==75,"actual size10 Swarmer has fixed1body/.85inherited eye and75health");
                a.player.setPos(swarmer.getX()+15,swarmer.getY(),swarmer.getZ());a.player.setGameMode(GameType.CREATIVE);steps(a,swarmer,500);
                a.require(swarmer.getSpawnCounter()==0&&a.level.getEntitiesOfClass(EntityTaintSwarm.class,a.bounds).isEmpty(),"native500counter waits at0 rather than spawning for an actual invulnerable creative player");
                a.player.setGameMode(GameType.SURVIVAL);try(var random=new WorldInput(a.level,List.of(.5F,.25F))){steps(a,swarmer,1);random.complete();}
                var swarms=a.level.getEntitiesOfClass(EntityTaintSwarm.class,a.bounds);a.require(swarmer.getSpawnCounter()==500&&swarms.size()==1&&swarms.getFirst().getY()==swarmer.getY()+.5&&swarms.getFirst().getYRot()==90,"actual vulnerable player16 reopens one genuine Swarm burst with original pitch/yaw world draws");
                var copy=ThaumcraftTaint.TAINT_SWARMER.get().create(a.level);copy.load(swarmer.saveWithoutId(new CompoundTag()));a.require(copy.getSpawnCounter()==500&&copy.getSporeSize()==10,"native swarmer reload retains size but resets the absent-NBT counter500");
                steps(a,swarmer,1);swarmer.playerTouch(a.player);a.require(a.level.getEntitiesOfClass(EntityTaintSwarm.class,a.bounds).size()==1,"native player collision neither ruptures the Swarmer nor adds an extra immediate Swarm");
                try(var random=new WorldInput(a.level,List.of(true,false))){a.require(swarmer.hurt(a.player.damageSources().playerAttack(a.player),100),"actual lethal native damage reaches the Swarmer death path");random.complete();}
                a.require(a.items().size()==2&&a.items().stream().mapToInt(e->e.getItem().getCount()).sum()==2&&a.items().stream().anyMatch(e->e.getItem().is(ThaumcraftContent.MATERIALS.get("tainted_goo").get()))&&a.items().stream().anyMatch(e->e.getItem().is(ThaumcraftContent.MATERIALS.get("taint_tendril").get())),"actual Swarmer death executes both original world Boolean drops once without native table/equipment duplication");
                try(var random=new WorldInput(a.level,List.of(.5F,.75F))){steps(a,swarmer,1);random.complete();}
                a.require(swarmer.deathTime==1&&a.level.getEntitiesOfClass(EntityTaintSwarm.class,a.bounds).size()==2,"actual deathTime1 releases one more genuine Swarm");steps(a,swarmer,1);a.require(a.level.getEntitiesOfClass(EntityTaintSwarm.class,a.bounds).size()==2,"deathTime2 cannot repeat its death Swarm");
            }finally{cleanup(a);}
        }test.succeed();
    }
    public static void spiderTargetsAndLoot(GameTestHelper test){
        try(var a=actor(test)){
            try {
                a.clearLane();for(int x=-4;x<=5;x++)for(int z=-4;z<=4;z++)a.put(a.root.offset(x,-1,z),Blocks.STONE.defaultBlockState());
                for(int y=0;y<=3;y++)for(int z=-4;z<=4;z++)a.put(a.root.offset(1,y,z),Blocks.STONE.defaultBlockState());
                var spider=ThaumcraftTaint.TAINT_SPIDER.get().create(a.level);spider.moveTo(a.root.getX()+.5,a.root.getY(),a.root.getZ()+.5,0,0);a.require(a.level.addFreshEntity(spider),"actual registered tiny Taint Spider enters its owned lane");
                a.player.setPos(a.root.getX()+3.5,a.root.getY(),a.root.getZ()+.5);steps(a,spider,8);
                a.require(spider.getTarget()==a.player&&!spider.getSensing().hasLineOfSight(a.player)&&spider.getBbWidth()==.4F&&spider.getBbHeight()==.3F&&spider.getEyeHeight()==.3F*.85F&&spider.getPickRadius()==.1F&&spider.getVoicePitch()==.7F,"actual tiny spider targets its real vulnerable player12 through an actual wall and retains physical/eye/pick/PITCH values");
                a.player.setPos(spider.getX()+.5,spider.getY(),spider.getZ());float health=a.player.getHealth();a.require(spider.doHurtTarget(a.player)&&a.player.getHealth()<health&&spider.getAttributeValue(Attributes.ATTACK_DAMAGE)==2,"actual native spider attack damages its real player using original attack2 attribute");spider.discard();
                for(int branch=0;branch<3;branch++){
                    a.purgeOutputs();var victim=ThaumcraftTaint.TAINT_SPIDER.get().create(a.level);victim.moveTo(a.root.getX()+.5,a.root.getY(),a.root.getZ()+.5,0,0);a.require(a.level.addFreshEntity(victim),"actual next native death owns a separate spider identity");
                    List<Object> draws=branch==0?List.of(new Roll(6,5)):List.of(new Roll(6,0),branch==1);
                    try(var random=new WorldInput(a.level,draws)){a.require(victim.hurt(a.player.damageSources().playerAttack(a.player),100),"actual player damage enters tiny spider death loot");random.complete();}
                    if(branch==0)a.require(a.items().isEmpty(),"original five-of-six rejection emits no native string/eye/custom loot");
                    else a.require(a.items().size()==1&&a.items().getFirst().getItem().getCount()==1&&a.items().getFirst().getItem().is(ThaumcraftContent.MATERIALS.get(branch==1?"tainted_goo":"taint_tendril").get()),"actual one-in-six death emits the witnessed Boolean resource once and preserves next world RNG draw");
                    steps(a,victim,20);a.require(victim.isRemoved()&&a.level.getEntitiesOfClass(ExperienceOrb.class,a.bounds).stream().mapToInt(ExperienceOrb::getValue).sum()==2,"actual native player-killed tiny spider death releases originalXP2 once");
                }
            }finally{cleanup(a);}
        }test.succeed();
    }
    private record Roll(int bound,int result){}
    /** Scoped synchronous world-RNG INPUT; the exact previous source is restored
     * before another native world operation or tick can run. */
    private static final class WorldInput implements AutoCloseable {
        private final Level level;private final Field field;private final RandomSource previous;private final List<Object> draws;private int at;
        WorldInput(Level level,List<Object> draws){this.level=level;this.draws=List.copyOf(draws);try{field=Level.class.getDeclaredField("random");field.setAccessible(true);previous=(RandomSource)field.get(level);field.set(level,new LegacyRandomSource(0){
            private Object take(){if(at==WorldInput.this.draws.size())throw new AssertionError("Unexpected extra world RNG draw");return WorldInput.this.draws.get(at++);}
            @Override public int nextInt(int bound){Object value=take();if(!(value instanceof Roll roll)||roll.bound()!=bound)throw new AssertionError("Unexpected world nextInt("+bound+") at"+(at-1)+": "+value);return roll.result();}
            @Override public boolean nextBoolean(){Object value=take();if(!(value instanceof Boolean b))throw new AssertionError("Unexpected world Boolean: "+value);return b;}
            @Override public float nextFloat(){Object value=take();if(!(value instanceof Float f))throw new AssertionError("Unexpected world float: "+value);return f;}
            @Override public long nextLong(){throw new AssertionError("Native audio/item transport must not add a world long draw");}
        });}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
        void complete(){if(at!=draws.size())throw new AssertionError("Unconsumed original world draws: "+at+"/"+draws.size());}
        @Override public void close(){try{field.set(level,previous);}catch(IllegalAccessException failure){throw new AssertionError(failure);}}
    }
    /** Save and restore the ENTIRE actual quart column, preserving preexisting
     * vertical biome variation rather than restoring one guessed surface key. */
    private static final class Column implements AutoCloseable {
        private final ServerLevel level;private final LevelChunk chunk;private final int qx,qz;private final Map<Integer,Holder<Biome>> old=new LinkedHashMap<>();
        Column(ServerLevel level,BlockPos pos){this.level=level;chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)throw new AssertionError("Owned actor chunk must already exist");qx=QuartPos.fromBlock(pos.getX());qz=QuartPos.fromBlock(pos.getZ());for(int y=QuartPos.fromBlock(chunk.getMinBuildHeight());y<=QuartPos.fromBlock(chunk.getMaxBuildHeight()-1);y++)old.put(y,chunk.getNoiseBiome(qx,y,qz));BiomeColumns.set(level,pos.getX(),pos.getZ(),ThaumcraftBiomes.TAINT);}
        @Override public void close(){chunk.fillBiomesFromNoise((x,y,z,sampler)->x==qx&&z==qz?old.get(y):chunk.getNoiseBiome(x,y,z),level.getChunkSource().randomState().sampler());chunk.setUnsaved(true);level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));}
    }
}
