package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.entities.monster.EntityTaintacleSmall;
import thaumcraft.common.entities.monster.OriginalTaintCombat;
import thaumcraft.common.lib.world.BiomeColumns;

/** Real registered mobs, native attributes/effects/damage and ServerLevel entity ticker.
 * Declared high-altitude lane, palette, equipment and potion inputs; no private counters/RNG edits,
 * no acquisition replay, elapsed-world-time or inspected-client claim. Root wrapper timeout140. */
public final class TaintacleRuntimeChecks {
    private TaintacleRuntimeChecks(){}
    public static void combatAndRootedAi(GameTestHelper test){
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(8,180,8));var terrain=new Terrain(a)){
            terrain.open();a.player.setPos(a.root.getX()+100,a.root.getY(),a.root.getZ());
            var tentacle=terrain.tentacle(a.root);
            a.require(tentacle.getMaxHealth()==50&&tentacle.getBbHeight()==3F&&tentacle.getBbWidth()==.66F&&tentacle.getPickRadius()==.25F,
                    "real registered main tentacle retains50 health, .66x3 dimensions and .25 collision pick radius");
            var effects=tentacle.getAttribute(Attributes.ATTACK_DAMAGE);
            a.require(effects!=null&&OriginalTaintCombat.mobDamage(tentacle)==7&&OriginalTaintCombat.tentacleDamage(tentacle)==7,"clean old/native attack attribute starts7");
            tentacle.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,1000,0));
            assertDamage(a,tentacle,16.1F,19.1F,"StrengthI old multiplicative attribute plus manual+3");
            tentacle.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,1000,0));
            assertDamage(a,tentacle,14.95F,15.95F,"StrengthI and WeaknessI original operations before separate+3-2");
            tentacle.removeEffect(MobEffects.DAMAGE_BOOST);
            assertDamage(a,tentacle,6.5F,4.5F,"WeaknessI subtracts.5 attribute then manual2");
            tentacle.removeAllEffects();
            var unrelated=ResourceLocation.fromNamespaceAndPath("thaumcraft","taintacle_runtime_unrelated");
            effects.addTransientModifier(new AttributeModifier(unrelated,1,AttributeModifier.Operation.ADD_VALUE));
            tentacle.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,1000,0));
            assertDamage(a,tentacle,18.4F,21.4F,"unrelated native attribute modifier survives old potion replacement");
            a.require(effects.hasModifier(unrelated),"detached calculation preserves unrelated live native modifier");
            tentacle.removeAllEffects();effects.removeModifier(unrelated);
            var victim=terrain.cow(a.root.east(2));var ignored=terrain.small(a.root.east(1));
            for(int y=0;y<4;y++)a.put(a.root.east().above(y),Blocks.STONE.defaultBlockState());
            tick(a,tentacle,2);
            a.require(tentacle.getTarget()==victim&&victim.getHealth()==10&&tentacle.originalAttackTime()==0,
                    "real nearest non-tainted living acquisition excludes closer Small; native eye LOS wall prevents melee");
            for(int y=0;y<4;y++)a.put(a.root.east().above(y),Blocks.AIR.defaultBlockState());
            tick(a,tentacle,1);
            a.require(victim.getHealth()==3&&tentacle.originalAttackTime()==20&&victim.getLastDamageSource()!=null
                            &&victim.getLastDamageSource().getEntity()==tentacle&&victim.getLastDamageSource().is(DamageSourceThaumcraft.TENTACLE),
                    "actual native AI melee deals7 normal attributed tentacle damage and starts original20 cooldown");
            tick(a,tentacle,1);a.require(victim.getHealth()==3&&tentacle.originalAttackTime()==19,"cooldown prevents an extra native attack on next update");
            ignored.discard();victim.discard();tentacle.setTarget(null);
            Vec3 original=tentacle.position();tentacle.move(MoverType.SELF,new Vec3(1,.25,1));
            a.require(tentacle.position().equals(original),"actual rooted move removes horizontal and upward movement");
            tentacle.move(MoverType.SELF,new Vec3(1,-.25,1));
            a.require(tentacle.getX()==original.x&&tentacle.getZ()==original.z&&tentacle.getY()==original.y-.25,"original root still falls rather than inventing no-gravity immunity");
            tentacle.setPos(original);tentacle.invulnerableTime=100;
            BiomeColumns.set(a.level,a.root.getX(),a.root.getZ(),Biomes.PLAINS);
            float health=tentacle.getHealth();while(tentacle.tickCount<20)tick(a,tentacle,1);
            a.require(tentacle.getHealth()==health-1,"actual20th entity update applies original protected outside-taint damage despite generic hurt grace");
        }
        test.succeed();
    }
    public static void retaliationLifetimeAndLoot(GameTestHelper test){
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(8,220,8));var terrain=new Terrain(a)){
            terrain.open();var parent=terrain.tentacle(a.root);a.player.setPos(a.root.getX()+17.5,a.root.getY(),a.root.getZ()+.5);
            terrain.column(BlockPos.containing(a.player.position()));
            terrain.put(BlockPos.containing(a.player.position()).below(),ThaumcraftTaint.SOIL.get().defaultBlockState());
            a.require(parent.hurt(a.level.damageSources().playerAttack(a.player),1),"real remote player attack reaches root hostile");
            var children=a.level.getEntitiesOfClass(EntityTaintacleSmall.class,parent.getBoundingBox().inflate(24));
            a.require(children.size()==1&&parent.originalAttackTime()>=40&&parent.originalAttackTime()<=59,
                    "actual >16-block damage retaliation creates one real Small and original40..59 cooldown without natural spawn/floor gate");
            var child=children.getFirst();terrain.owned.add(child);
            a.require(Math.abs(child.getX()-a.player.getX())<1&&Math.abs(child.getZ()-a.player.getZ())<1&&child.getY()==a.player.getY()
                            &&child.getMaxHealth()==8&&OriginalTaintCombat.mobDamage(child)==2,
                    "real child uses triangular float offsets at victim feet, health8/damage2");
            a.player.setPos(a.root.getX()+100,a.root.getY(),a.root.getZ());
            child.setNoGravity(true);child.setNoAi(true);terrain.column(child.blockPosition());
            a.require(!child.checkSpawnRules(a.level,MobSpawnType.NATURAL),"Small never passes natural spawn rules");
            tick(a,child,200);a.require(child.isAlive()&&child.getHealth()==8,"Small survives first200 real native entity ticker updates in actual taint column");
            var saved=new CompoundTag();child.saveWithoutId(saved);var reloaded=new EntityTaintacleSmall(a.level);reloaded.load(saved);reloaded.setUUID(java.util.UUID.randomUUID());reloaded.setNoGravity(true);reloaded.setNoAi(true);terrain.owned.add(reloaded);
            a.require(a.level.addFreshEntity(reloaded),"actual native save/reload produces an independent lifetime owner");
            tick(a,reloaded,1);a.require(reloaded.isAlive()&&reloaded.getHealth()==8,"original transient lifetime reloads fresh rather than serializing exhausted200 ticks");
            tick(a,child,1);a.require(child.isDeadOrDying()&&child.getHealth()==0&&child.getCombatTracker().getDeathMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents message&&message.getKey().equals("death.attack.drown"),
                    "post-decrement lifetime applies protected drown10 on real update201");
            var drops=a.level.getEntitiesOfClass(ItemEntity.class,child.getBoundingBox().inflate(2));
            a.require(drops.isEmpty(),"ordinary Small death produces no custom tendril/goo loot");
            reloaded.discard();
            var dying=terrain.tentacle(a.root.west(2));dying.hurt(a.level.damageSources().playerAttack(a.player),1000);
            var ownDrops=a.level.getEntitiesOfClass(ItemEntity.class,dying.getBoundingBox().inflate(2));
            a.require(ownDrops.size()==1&&ownDrops.getFirst().getItem().getCount()==1
                            &&(ownDrops.getFirst().getItem().is(ThaumcraftContent.MATERIALS.get("tainted_goo").get())||ownDrops.getFirst().getItem().is(ThaumcraftContent.MATERIALS.get("taint_tendril").get())),
                    "actual main death emits exactly one original goo/tendril, without fabricated Small drops");
            ownDrops.forEach(Entity::discard);
        }
        test.succeed();
    }
    public static void noInventedFloorOrPotionMutation(GameTestHelper test){
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(8,260,8));var terrain=new Terrain(a)){
            terrain.open();a.player.setPos(a.root.getX()+100,a.root.getY(),a.root.getZ());
            var candidate=new EntityTaintacle(a.level);terrain.owned.add(candidate);candidate.setPos(Vec3.atBottomCenterOf(a.root));
            a.require(a.level.isEmptyBlock(a.root.below())&&candidate.checkSpawnObstruction(a.level),"native original living obstruction permits actual empty space below a root: no new support-floor gate");
            a.put(a.root,Blocks.WATER.defaultBlockState());a.require(!candidate.checkSpawnObstruction(a.level),"actual native liquid box prevents spawn");
            a.put(a.root,Blocks.STONE.defaultBlockState());a.require(!candidate.checkSpawnObstruction(a.level),"actual native block collision prevents spawn");
            a.put(a.root,Blocks.AIR.defaultBlockState());
            a.put(a.root,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,1));
            a.require(!EntityTaintacle.checkSpawnRules(ThaumcraftTaint.TAINTACLE.get(),a.level,MobSpawnType.NATURAL,a.root,RandomSource.create(1)),"nonzero fibres metadata does not count as original rooted spawn material");
            a.put(a.root,ThaumcraftTaint.FIBRES.get().defaultBlockState());BiomeColumns.set(a.level,a.root.getX(),a.root.getZ(),Biomes.PLAINS);
            a.require(!candidate.checkSpawnRules(a.level,MobSpawnType.NATURAL),"same fibres in actual wrong surface biome refuses natural root");
            candidate.discard();
            var attacker=terrain.tentacle(a.root.west(2));
            for(int variant=0;variant<4;variant++){
                attacker.removeAllEffects();if((variant&1)!=0)attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,100,0));
                if((variant&2)!=0)attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0));
                var target=EntityType.PIG.create(a.level);a.require(target!=null,"native living target factory");terrain.owned.add(target);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.setPos(Vec3.atBottomCenterOf(a.root.east(2)));target.setNoGravity(true);target.setNoAi(true);a.level.addFreshEntity(target);
                double live=attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);float expected=switch(variant){case 0->7;case 1->19.1F;case 2->4.5F;default->15.95F;};
                a.require(attacker.doHurtTarget(target)&&Math.abs(target.getHealth()-(100-expected))<.00001F&&attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)==live,
                        "real native damage observes original strength/weakness vector without modifying live attributes, variant"+variant);
                target.discard();
            }
        }
        test.succeed();
    }
    private static void assertDamage(FocusUpgradeRuntimeChecks.Actor a,EntityTaintacle attacker,float mob,float tentacle,String label){
        double before=attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);var effects=attacker.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers();
        a.require(Math.abs(OriginalTaintCombat.mobDamage(attacker)-mob)<.00001F&&Math.abs(OriginalTaintCombat.tentacleDamage(attacker)-tentacle)<.00001F
                        &&attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)==before&&attacker.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().equals(effects),label+"; native attribute/effects unchanged");
    }
    private static void tick(FocusUpgradeRuntimeChecks.Actor a,Entity entity,int count){for(int i=0;i<count;i++)a.level.tickNonPassenger(entity);}
    private static final class Terrain implements AutoCloseable{
        final FocusUpgradeRuntimeChecks.Actor a;final List<Entity> owned=new ArrayList<>();
        final Map<LevelChunk,List<byte[]>> biomes=new LinkedHashMap<>();final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> extraBlocks=new LinkedHashMap<>();
        Terrain(FocusUpgradeRuntimeChecks.Actor a){this.a=a;}
        void open(){a.clearLane();column(a.root);}
        void column(BlockPos pos){
            var chunk=a.level.getChunkAt(pos);if(!biomes.containsKey(chunk)){
                var sections=new ArrayList<byte[]>();for(var section:chunk.getSections()){
                    var buffer=new FriendlyByteBuf(Unpooled.buffer());try{section.getBiomes().write(buffer);byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);sections.add(bytes);}finally{buffer.release();}
                }
                biomes.put(chunk,sections);
            }
            BiomeColumns.set(a.level,pos.getX(),pos.getZ(),ThaumcraftBiomes.TAINT);
            a.require(BiomeColumns.surfaceHolder(a.level,pos.getX(),pos.getZ()).is(ThaumcraftBiomes.TAINT),"declared native surface column installed or already identical");
        }
        void put(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){extraBlocks.putIfAbsent(pos,a.level.getBlockState(pos));a.require(a.level.getBlockEntity(pos)==null,"extra retaliation soil input has no previous BE");a.put(pos,state);}
        EntityTaintacle tentacle(BlockPos pos){var entity=new EntityTaintacle(a.level);prepare(entity,pos);return entity;}
        EntityTaintacleSmall small(BlockPos pos){var entity=new EntityTaintacleSmall(a.level);prepare(entity,pos);return entity;}
        void prepare(EntityTaintacle entity,BlockPos pos){column(pos);entity.setPos(Vec3.atBottomCenterOf(pos));entity.setNoGravity(true);entity.setNoAi(true);owned.add(entity);a.require(a.level.addFreshEntity(entity),"actual registered tainted hostile enters native world index");}
        net.minecraft.world.entity.animal.Cow cow(BlockPos pos){var entity=EntityType.COW.create(a.level);a.require(entity!=null&&entity.getMaxHealth()==10,"native cow input has10 health");entity.setPos(Vec3.atBottomCenterOf(pos));entity.setNoGravity(true);entity.setNoAi(true);owned.add(entity);a.require(a.level.addFreshEntity(entity),"native non-tainted target enters world");return entity;}
        @Override public void close(){
            owned.forEach(Entity::discard);extraBlocks.forEach(a::put);
            for(var entry:biomes.entrySet()){
                var chunk=entry.getKey();for(int i=0;i<chunk.getSections().length;i++){
                    var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(entry.getValue().get(i)));try{chunk.getSections()[i].readBiomes(buffer);}finally{buffer.release();}
                }
                chunk.setUnsaved(true);
            }
            a.level.getChunkSource().chunkMap.resendBiomesForChunks(new ArrayList<>(biomes.keySet()));
        }
    }
}
