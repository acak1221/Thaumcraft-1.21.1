package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.config.*;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.entities.ai.pech.PechItemGoal;
import thaumcraft.common.entities.ai.pech.PechAvoidGoal;
import thaumcraft.common.entities.ai.pech.PechMeleeGoal;
import thaumcraft.common.entities.ai.pech.PechWatchGoal;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.projectile.EntityPechBlast;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusPech;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.pech.PechDrops;
import thaumcraft.common.lib.pech.PechTradeTables;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileManaPod;

/** Connected actors and supported native entities are declared fixtures. The
 * pod's positive test temporarily attaches MAGICAL to its real biome holder,
 * restores its tags in finally, and never claims natural biome acquisition.
 * Actual server use/menu/projectile/ADD and saved queue paths execute here. */
public final class PechRuntimeChecks {
    private PechRuntimeChecks(){}
    public static void entity(GameTestHelper test){run(test,0);}
    public static void trade(GameTestHelper test){run(test,1);}
    public static void curse(GameTestHelper test){run(test,2);}
    public static void beans(GameTestHelper test){run(test,3);}
    public static void goals(GameTestHelper test){run(test,4);}
    private static void run(GameTestHelper test,int group){try(var f=new Fixture(test)){switch(group){case 0->f.entity();case 1->f.trade();case 2->f.curse();case 3->f.beans();default->f.goals();}}test.succeed();}
    private static final class Fixture implements AutoCloseable {
        final FocusUpgradeRuntimeChecks.Actor a;final List<Entity> owned=new ArrayList<>();final CompoundTag knowledge;
        final Map<BlockPos,BlockState> extendedFloor=new LinkedHashMap<>();
        Fixture(GameTestHelper test){a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,90,4));knowledge=KnowledgeAccess.get(a.player).save();a.clearLane();for(int x=-3;x<=5;x++)for(int z=-3;z<=3;z++)a.put(a.root.offset(x,-1,z),Blocks.STONE.defaultBlockState());}
        <T extends Mob>T mob(EntityType<T> type,int x,int z){T mob=type.create(a.level);a.require(mob!=null,"actual registered entity exists "+type);mob.setPos(a.root.getX()+x+.5,a.root.getY(),a.root.getZ()+z+.5);mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);owned.add(mob);
            // The 1.7 original rolled nextInt100 of the world random for every EntityMob that
            // joined the world (oracle EventHandlerEntity.entitySpawns:391). The fixture declares
            // that draw, so the exact original attributes and death reward of the Pech stay
            // deterministic.
            a.require(ChampionJoinRoll.join(a.level,mob),"actual entity admitted");return mob;}
        EntityPech pech(int x,int z){return mob(ThaumcraftPech.PECH.get(),x,z);}
        void clear(){a.player.closeContainer();owned.forEach(Entity::discard);owned.clear();a.purgeOutputs();a.player.removeAllEffects();a.player.setHealth(a.player.getMaxHealth());}
        void entity(){
            var pech=pech(0,0);a.require(pech.getMaxHealth()==30&&pech.getAttributeValue(Attributes.ATTACK_DAMAGE)==6&&pech.getAttributeValue(Attributes.MOVEMENT_SPEED)==.5&&pech.getArmorValue()==2,"native original30/6/.5/armor2");
            a.require(Math.abs(pech.getBbWidth()-.6F)<1e-6&&Math.abs(pech.getBbHeight()-1.8F)<1e-6&&Math.abs(pech.getEyeHeight()-1.8F*.66F)<1e-6,"original Pech body and .66 eye");
            pech.setPechType(2);pech.setTamed(true);pech.setAnger(511);pech.loot[2]=ItemManaBean.create(Aspect.FIRE);pech.loot[8]=new ItemStack(Items.DIAMOND,6);
            CompoundTag saved=new CompoundTag();pech.addAdditionalSaveData(saved);var loaded=pech(2,0);loaded.readAdditionalSaveData(saved);
            a.require(loaded.getPechType()==2&&loaded.isTamed()&&loaded.getAnger()==511&&loaded.loot[8].getCount()==6&&ItemManaBean.data(loaded.loot[2]).firstTag().equals("ignis")&&loaded.updateAINextTick,"nine saved slots, components, type/anger/tame and deferred AI persist");
            a.require(pech.removeWhenFarAway(10000),"two pack slots allow original despawn even tamed");for(int i=0;i<5;i++)pech.loot[i]=new ItemStack(Items.DIRT);a.require(!pech.removeWhenFarAway(10000)&&pech.requiresCustomPersistence(),"five occupied pack slots suppress both native despawn gates");
            Arrays.fill(pech.loot,new ItemStack(Items.COBBLESTONE,64));pech.setTamed(false);
            a.require(pech.canPickup(new ItemStack(Items.EMERALD))&&!pech.canPickup(new ItemStack(Items.DIRT)),"untamed explicit valuables bypass a full pack; ordinary full-pack items do not");
            a.require(pech.getValue(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))==2&&pech.getValue(new ItemStack(Items.EMERALD))==5,"flattened enchanted apple retains old metadata-shared value2, emerald5");
            long tameSeed=seedForTame();pech.getRandom().setSeed(tameSeed);ItemStack offering=new ItemStack(Items.EMERALD,3);a.require(pech.pickupItem(offering).getCount()==2&&pech.isTamed(),"actual pickup tames with entity nextInt10<5 and consumes only ONE offering");
            Arrays.fill(pech.loot,ItemStack.EMPTY);pech.loot[0]=new ItemStack(Items.DIRT,63);ItemStack dirt=new ItemStack(Items.DIRT,4);
            a.require(pech.pickupItem(dirt).isEmpty()&&pech.loot[0].getCount()==64&&pech.loot[1].getCount()==3,"actual pack pickup merges partial stack then first empty slot");
            var neighbor=pech(1,0);neighbor.setTamed(true);pech.setTamed(true);a.player.setPos(a.root.getX()+.5,a.root.getY(),a.root.getZ()-2);
            pech.hurt(a.level.damageSources().playerAttack(a.player),1);
            a.require(pech.getAnger()>=400&&pech.getAnger()<800&&neighbor.getAnger()>=400&&!pech.isTamed()&&!neighbor.isTamed()&&neighbor.getTarget()==a.player,"real player damage angers nearby Pech before native damage and untames the group");
            pech.setTamed(true);neighbor.setTamed(true);pech.hurt(a.level.damageSources().playerAttack(a.player),1);
            a.require(!pech.isTamed()&&!neighbor.isTamed(),"even native invulnerability-window hurt refusal still triggers original group anger");
            pech.setAnger(2);pech.tick();a.require(pech.getAnger()==1,"ordinary native tick decrements saved anger");
            clear();pech=pech(0,0);ItemEntity marked=new ItemEntity(a.level,pech.getX()+1,pech.getY(),pech.getZ(),new ItemStack(Items.EMERALD),0,0,0);PechDrops.mark(marked);owned.add(marked);a.require(a.level.addFreshEntity(marked),"marked actual item admitted");
            var goal=new PechItemGoal(pech);pech.tickCount=5;a.require(!goal.canUse(),"real Pech pickup selector excludes persisted PechDrop marker");
            CompoundTag itemState=new CompoundTag();marked.addAdditionalSaveData(itemState);var copy=new ItemEntity(EntityType.ITEM,a.level);copy.readAdditionalSaveData(itemState);a.require(PechDrops.isMarked(copy),"PechDrop survives actual native ItemEntity NBT round trip without changing item components");
            marked.discard();var normal=new ItemEntity(a.level,pech.getX()+1,pech.getY(),pech.getZ(),new ItemStack(Items.EMERALD),0,0,0);owned.add(normal);a.level.addFreshEntity(normal);pech.tickCount=10;a.require(goal.canUse(),"same native nearby unmarked valuable is a valid five-tick pickup target");
            clear();boolean mage=false,stalker=false,forager=false;
            for(int i=0;i<64;i++){pech=pech(0,0);pech.getRandom().setSeed(i);pech.finalizeSpawn(a.level,a.level.getCurrentDifficultyAt(a.root),MobSpawnType.NATURAL,null);mage|=pech.getPechType()==1;stalker|=pech.getPechType()==2;forager|=pech.getPechType()==0;if(pech.getPechType()==1){var data=ItemWandCasting.data(pech.getMainHandItem());a.require(data.amount("terra")>=200&&data.amount("terra")<=700&&FocusData.get(pech.getMainHandItem()).is(ThaumcraftPech.FOCUS_PECH.get()),"real spawned Mage holds original charged Pech focus wand, whole-vis converted to centivis");}pech.discard();}
            a.require(mage&&stalker&&forager,"64 declared ENTITY RNG seeds reach all three actual spawn variants");
            combatAndDrops();
        }
        void combatAndDrops(){
            clear();var pech=pech(0,0);var victim=mob(EntityType.COW,1,0);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);victim.setHealth(200);pech.setTarget(victim);pech.setTamed(true);
            var melee=new PechMeleeGoal(pech);a.require(melee.canUse(),"real native path admits original melee goal");melee.start();melee.tick();float after=victim.getHealth();
            a.require(after==194,"actual TC4 melee goal invokes native attack damage6");
            for(int tick=0;tick<9;tick++){victim.setDeltaMovement(Vec3.ZERO);victim.tick();victim.setPos(a.root.getX()+1.5,a.root.getY(),a.root.getZ()+.5);melee.tick();}
            a.require(victim.getHealth()==after,"nine actual goal ticks cannot bypass ten-tick melee gate");
            victim.setDeltaMovement(Vec3.ZERO);victim.tick();victim.setPos(a.root.getX()+1.5,a.root.getY(),a.root.getZ()+.5);melee.tick();
            a.require(victim.getHealth()==188,"tenth actual goal tick attacks again rather than native20-tick substitution");
            pech.setPechType(1);pech.getRandom().setSeed(71);var expected=RandomSource.create(71);boolean shade=expected.nextFloat()<.1F;expected.nextFloat();
            pech.performRangedAttack(victim,.5F);final EntityPech attacker=pech;
            var curses=new ArrayList<EntityPechBlast>();a.level.getAllEntities().forEach(entity->{if(entity instanceof EntityPechBlast curse&&curse.getOwner()==attacker)curses.add(curse);});owned.addAll(curses);
            a.require(curses.size()==1&&curses.getFirst().getStrength()==1&&curses.getFirst().getDurationLevel()==0&&curses.getFirst().isNightshade()==shade&&pech.getRandom().nextLong()==expected.nextLong(),"actual NPC Mage launches strength1 curse using original ENTITY nightshade/pitch draws");
            pech.setPechType(2);pech.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));pech.performRangedAttack(victim,.5F);
            var arrows=new ArrayList<net.minecraft.world.entity.projectile.Arrow>();a.level.getAllEntities().forEach(entity->{if(entity instanceof net.minecraft.world.entity.projectile.Arrow arrow&&arrow.getOwner()==attacker)arrows.add(arrow);});owned.addAll(arrows);
            a.require(arrows.size()==1&&arrows.getFirst().getBaseDamage()>0&&arrows.getFirst().getDeltaMovement().lengthSqr()>0,"actual NPC Stalker emits a moving native Arrow with its real owner/damage");
            clear();pech=pech(0,0);pech.loot[0]=new ItemStack(Items.DIAMOND,3);
            long seed=goldSeedAfterOrbs();expected=RandomSource.create(seed);consumeOriginalOrbDraws(expected);boolean pack=expected.nextFloat()<.88F;boolean gold=expected.nextInt(10)<1;a.level.random.setSeed(seed);
            pech.hurt(a.level.damageSources().playerAttack(a.player),1000);
            int diamonds=a.items().stream().filter(item->item.getItem().is(Items.DIAMOND)).mapToInt(item->item.getItem().getCount()).sum();
            int coins=a.items().stream().filter(item->item.getItem().is(ThaumcraftContent.MATERIALS.get("gold_coin").get())).mapToInt(item->item.getItem().getCount()).sum();
            a.require(pech.isDeadOrDying()&&diamonds==(pack?3:0)&&coins==(gold?1:0)&&coins==1,"actual Pech death preserves pack .88/gold_coin rewards: diamonds="+diamonds+",coins="+coins);
            // Native1.21 drops XP immediately in dropAllDeathLoot. Five XP is
            // split3+1+1; each native tryMergeToExisting consumes nextInt40,
            // even when there are no pre-existing orbs. These are native XP
            // draws AFTER the original two loot draws, never item physics.
            int xp=0;for(var orb:a.level.getEntitiesOfClass(ExperienceOrb.class,a.bounds)){var tag=orb.saveWithoutId(new CompoundTag());xp+=tag.getShort("Value")*tag.getInt("Count");}
            a.require(xp==5,"actual Pech native death emits original five XP, got="+xp);
            for(int i=0;i<3;i++)expected.nextInt(40);long actualNext=a.level.random.nextLong(),expectedNext=expected.nextLong();
            a.require(actualNext==expectedNext,"actual death WORLD stream is loot .88/coin10 then native XP merge40x3, without item-physics draws: actual="+actualNext+",expected="+expectedNext);
            for(var dropped:a.items())if(dropped.getItem().is(ThaumcraftManaBeans.MANA_BEAN.get()))a.require(ItemManaBean.data(dropped.getItem())!=null&&Aspect.getAspect(ItemManaBean.data(dropped.getItem()).firstTag()).isPrimal(),"actual Pech death bean is typed with an original primal");
        }
        private static long goldSeed(){for(long seed=0;seed<10000;seed++){var rng=RandomSource.create(seed);if(rng.nextFloat()<.88F&&rng.nextInt(10)==0)return seed;}throw new AssertionError();}
        /**
         * The reduced primals of the original Pech, in the original order.
         *
         * <p>Oracle {@code thaumcraft/common/config/ConfigAspects.java:84} registers MAN 2,
         * MAGIC 2, EXCHANGE 2 and GREED 2 for the Pech of type zero; the reduction to the
         * primals of {@code ResearchManager.reduceToPrimals} (oracle
         * {@code thaumcraft/common/lib/research/ResearchManager.java:466-495}) leaves air 8,
         * order 8, water 14, earth 14, fire 6 and entropy 10.</p>
         */
        private static final int[] PECH_ORB_AMOUNTS={8,8,14,14,6,10};
        /**
         * The original death aspect orbs of the Pech.
         *
         * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerEntity.java:508-516}: the death
         * of a mob that was hit by a player draws one world boolean per primal aspect of the mob
         * and, for every true roll, the value of that aspect, BEFORE the loot of the same death.</p>
         */
        private static void consumeOriginalOrbDraws(RandomSource random){for(int amount:PECH_ORB_AMOUNTS)if(random.nextBoolean())random.nextInt(amount);}
        private static long goldSeedAfterOrbs(){for(long seed=0;seed<10000;seed++){var rng=RandomSource.create(seed);consumeOriginalOrbDraws(rng);if(rng.nextFloat()<.88F&&rng.nextInt(10)==0)return seed;}throw new AssertionError();}
        private static long seedForTame(){for(long seed=0;seed<1000;seed++)if(RandomSource.create(seed).nextInt(10)<5)return seed;throw new AssertionError();}
        void goals(){
            var pech=pech(0,0);var installed=new ArrayList<Goal>();
            // Public native selector traversal; a false predicate removes nothing.
            pech.removeAllGoals(goal->{installed.add(goal);return false;});
            var avoids=installed.stream().filter(PechAvoidGoal.class::isInstance).map(PechAvoidGoal.class::cast).toList();
            var watchers=installed.stream().filter(PechWatchGoal.class::isInstance).map(PechWatchGoal.class::cast).toList();
            a.require(avoids.size()==1&&watchers.size()==2,"actual registered untamed Pech installs original avoid and both watch goals");
            var close=watchers.stream().filter(goal->goal.getFlags().contains(Goal.Flag.MOVE)).findFirst().orElseThrow();
            var ordinary=watchers.stream().filter(goal->!goal.getFlags().contains(Goal.Flag.MOVE)).findFirst().orElseThrow();
            a.require(close.getFlags().equals(EnumSet.of(Goal.Flag.MOVE,Goal.Flag.LOOK))&&ordinary.getFlags().equals(EnumSet.of(Goal.Flag.LOOK)),"WatchClosest2 owns MOVE and LOOK; ordinary Mob watching only LOOK");
            a.player.setGameMode(GameType.CREATIVE);a.player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,400));
            a.player.setPos(pech.getX()+2,pech.getY(),pech.getZ());
            a.put(a.root.offset(1,0,0),Blocks.STONE.defaultBlockState());a.put(a.root.offset(1,1,0),Blocks.STONE.defaultBlockState());
            a.require(!pech.getSensing().hasLineOfSight(a.player)&&PechAvoidGoal.closestPlayer(pech,8)==a.player,"original plain nearest selector includes a creative invisible player behind a real opaque wall");
            pech.getRandom().setSeed(181);var expected=RandomSource.create(181);expected.nextFloat();int duration=40+expected.nextInt(40);
            a.require(close.canUse(),"actual close watcher admits invisible creative player without line of sight");close.start();
            a.require(pech.getRandom().nextLong()==expected.nextLong(),"actual close watch uses exactly chance float then original40..79 duration draw");
            for(int tick=0;tick<duration;tick++){a.require(close.canContinueToUse(),"original close watch remains active until exact duration");close.tick();}
            a.require(!close.canContinueToUse()&&pech.getLookControl().getWantedX()==a.player.getX()&&pech.getLookControl().getWantedY()==a.player.getEyeY(),"actual native LookControl targets player eyes and watching expires at original duration");close.stop();
            a.player.setPos(pech.getX()+8,pech.getY(),pech.getZ());a.require(PechAvoidGoal.closestPlayer(pech,8)==null,"plain nearest player range is strict: exactly eight blocks is outside");
            a.player.setPos(pech.getX()+2,pech.getY(),pech.getZ());a.player.setGameMode(GameType.SPECTATOR);a.require(PechAvoidGoal.closestPlayer(pech,8)==null,"modern spectators are excluded from the original player selector adapter");a.player.setGameMode(GameType.CREATIVE);
            a.put(a.root.offset(1,0,0),Blocks.AIR.defaultBlockState());a.put(a.root.offset(1,1,0),Blocks.AIR.defaultBlockState());
            // Native getPosAway searches a16-block horizontal radius. The
            // ordinary9x7 melee lane cannot support that navigation fixture.
            // Snapshot and restore a real37x37 floor; no target/path is forced.
            fleeFloor(a.root);
            var avoid=avoids.getFirst();boolean admitted=false;
            for(int seed=0;seed<128&&!admitted;seed++){pech.getRandom().setSeed(seed);admitted=avoid.canUse();}
            a.require(admitted,"bounded declared native flee-position seeds admit a real supported retreat path from the invisible creative player");avoid.start();avoid.tick();
            var path=pech.getNavigation().getPath();a.require(path!=null&&path.getEndNode()!=null&&!pech.getNavigation().isDone(),"original avoidance starts actual native navigation along its admitted path");
            var end=path.getEndNode();a.require(a.player.distanceToSqr(end.x,end.y,end.z)>=a.player.distanceToSqr(pech),"native path end retains original integer retreat coordinates farther from the player");pech.getNavigation().stop();avoid.stop();
            // Repeat the actual installed goal on an owned negative-coordinate
            // platform: truncating native bottom-center coordinates would
            // otherwise reject every valid path in this quadrant.
            Vec3 original=pech.position();var negative=new BlockPos(-97,a.root.getY(),-97);fleeFloor(negative);
            pech.setPos(negative.getX()+.5,negative.getY(),negative.getZ()+.5);pech.setOnGround(true);a.player.setPos(pech.getX()+2,pech.getY(),pech.getZ());admitted=false;
            for(int seed=0;seed<128&&!admitted;seed++){pech.getRandom().setSeed(seed);admitted=avoid.canUse();}
            a.require(admitted,"actual installed avoidance admits a native path from an invisible creative player at negative world coordinates");avoid.start();avoid.tick();path=pech.getNavigation().getPath();
            a.require(path!=null&&path.getEndNode()!=null&&path.getEndNode().x<0&&path.getEndNode().z<0,"negative-coordinate retreat starts real native navigation in the same quadrant");
            end=path.getEndNode();a.require(a.player.distanceToSqr(end.x,end.y,end.z)>=a.player.distanceToSqr(pech),"negative native path end preserves original integer distance and endpoint checks");pech.getNavigation().stop();avoid.stop();pech.setPos(original);pech.setOnGround(true);
            a.player.setPos(pech.getX()+12,pech.getY(),pech.getZ());var cow=mob(EntityType.COW,2,1);
            long seed=0;while(seed<100000&&RandomSource.create(seed).nextFloat()>=.02F)seed++;a.require(seed<100000,"declared bounded ordinary-watch chance seed exists");
            pech.getRandom().setSeed(seed);expected=RandomSource.create(seed);expected.nextFloat();duration=40+expected.nextInt(40);
            a.require(ordinary.canUse(),"actual ordinary watcher admits nearby native Mob on original .02 chance");ordinary.start();ordinary.tick();
            a.require(pech.getRandom().nextLong()==expected.nextLong()&&ordinary.canContinueToUse()&&pech.getLookControl().getWantedX()==cow.getX()&&pech.getLookControl().getWantedY()==cow.getEyeY(),"ordinary watcher uses exact duration RNG and native Mob eye target");
            cow.discard();a.require(!ordinary.canContinueToUse(),"removed native Mob ends watching immediately");ordinary.stop();
            // Ordinary Mob.tick drives the installed GoalSelector rather than calling this goal directly.
            pech.setTamed(true);pech.setCombatTask();pech.removeAllGoals(goal->!(goal instanceof PechWatchGoal));pech.setNoAi(false);
            a.player.setPos(pech.getX()+2,pech.getY(),pech.getZ());pech.getLookControl().setLookAt(pech.getX(),pech.getY()-10,pech.getZ());
            for(int tick=0;tick<4;tick++){pech.setDeltaMovement(Vec3.ZERO);pech.tick();}
            a.require(pech.getLookControl().getWantedX()==a.player.getX()&&pech.getLookControl().getWantedY()==a.player.getEyeY(),"ordinary native Pech ticks run the installed close watcher for an invisible creative player");
            a.player.setGameMode(GameType.SURVIVAL);
        }
        private void fleeFloor(BlockPos center){
            for(int x=(center.getX()-18)>>4;x<=(center.getX()+18)>>4;x++)for(int z=(center.getZ()-18)>>4;z<=(center.getZ()+18)>>4;z++)a.level.getChunk(x,z);
            for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++){
                var pos=center.offset(x,-1,z);a.require(a.level.getBlockEntity(pos)==null,"extended flee floor contains no block entity to overwrite");
                extendedFloor.putIfAbsent(pos,a.level.getBlockState(pos));a.put(pos,Blocks.STONE.defaultBlockState());
            }
        }
        void trade(){
            var pech=pech(0,0);pech.setTamed(true);pech.setPechType(0);a.player.setPos(pech.getX()+1,pech.getY(),pech.getZ());
            a.require(pech.mobInteract(a.player,InteractionHand.MAIN_HAND).consumesAction()&&a.player.containerMenu instanceof ContainerPech,"real native interaction opens admitted Pech menu");
            var menu=(ContainerPech)a.player.containerMenu;menu.getMerchantInventory().setItem(0,new ItemStack(Items.EMERALD,2));
            a.require(!pech.mobInteract(a.player,InteractionHand.MAIN_HAND).consumesAction()&&a.player.containerMenu==menu&&pech.isCustomer(a.player)&&menu.getMerchantInventory().getItem(0).getCount()==2,"replayed actual interaction cannot reopen/close the active customer menu or move its offering");
            a.require(!menu.clickMenuButton(a.player,1)&&menu.getMerchantInventory().getItem(0).getCount()==2,"wrong native menu button cannot spend or generate rewards");
            a.player.setPos(pech.getX()+9,pech.getY(),pech.getZ());a.require(!menu.clickMenuButton(a.player,0)&&menu.getMerchantInventory().getItem(0).getCount()==2,"actual stale-distance button refuses before RNG/payment");
            a.player.setPos(pech.getX()+1,pech.getY(),pech.getZ());a.level.random.setSeed(12);
            a.require(menu.clickMenuButton(a.player,0)&&menu.getMerchantInventory().getItem(0).getCount()==1,"actual original-table trade atomically pays exactly one offering");
            int outputs=0;for(int i=1;i<5;i++)outputs+=menu.getMerchantInventory().getItem(i).getCount();a.require(outputs>0,"actual paid trade publishes real reward stacks into four native outputs");
            var before=menu.getMerchantInventory().getItem(0).copy();a.require(!menu.clickMenuButton(a.player,0)&&ItemStack.matches(before,menu.getMerchantInventory().getItem(0)),"replayed button with uncleared output cannot pay again");
            a.require(!menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)),"actual output slots refuse insertion");
            List<ItemStack> returns=new ArrayList<>();for(int i=0;i<5;i++){var stack=menu.getMerchantInventory().getItem(i);if(!stack.isEmpty())returns.add(stack.copy());}
            int[] rejected={0};Set<UUID> candidates=new HashSet<>();
            EntityEvent.Add cancel=(entity,world)->{if(world==a.level&&entity instanceof ItemEntity item&&PechDrops.isMarked(item)){rejected[0]++;candidates.add(item.getUUID());return EventResult.interruptFalse();}return EventResult.pass();};
            EntityEvent.ADD.register(cancel);try{a.player.closeContainer();DeferredItemDrops.tick(a.level);}finally{EntityEvent.ADD.unregister(cancel);}
            a.require(rejected[0]==returns.size()&&!pech.isCustomer(a.player)&&!pech.trading,"real close gives every stack one deferred marked ADD attempt, then clears session");
            var queue=a.level.getDataStorage().get(new net.minecraft.world.level.saveddata.SavedData.Factory<>(DeferredItemDrops::new,DeferredItemDrops::load,null),"thaumcraft_pending_inventory_drops");
            a.require(queue!=null&&queue.pendingCount()==returns.size(),"cancelled native ADD retains every actual returned stack durably");
            CompoundTag saved=queue.save(new CompoundTag(),a.level.registryAccess());var pending=saved.getList("Pending",Tag.TAG_COMPOUND);
            a.require(pending.size()==returns.size()&&pending.stream().allMatch(raw->((CompoundTag)raw).getBoolean("PechDrop")&&((CompoundTag)raw).getInt("PickupDelay")==40),"saved return ownership carries marker and original40-tick pickup delay");
            var loaded=DeferredItemDrops.load(saved,a.level.registryAccess());a.require(loaded.pendingCount()==returns.size(),"actual queue NBT reload keeps Pech return ownership");
            // Explicitly loaded queue has no transient cooldown: replay one owned
            // snapshot, then settle the original against those same UUID owners.
            loaded.flush(a.level);queue.flush(a.level);
            var delivered=a.items().stream().filter(PechDrops::isMarked).toList();a.require(delivered.size()==returns.size(),"same paid UUIDs yield exactly one native item per returned stack after retry/reload");
            for(var item:delivered){a.require(candidates.contains(item.getUUID())&&item.hasPickUpDelay(),"retry keeps reserved UUID and delay");owned.add(item);}
            for(int type=0;type<3;type++){
                var entries=PechTradeTables.entries(a.level,type);for(int tier=1;tier<=5;tier++){final int value=tier;a.require(entries.stream().anyMatch(entry->entry.tier()==value),"actual table contains variant"+type+" tier"+tier);}
                if(type==0)a.require(entries.stream().filter(entry->entry.baseStack().is(ThaumcraftNaturals.SILVERWOOD_SAPLING.get().asItem())).count()==2,"original duplicate Silverwood-sapling weights remain two entries");
                if(type==1)a.require(entries.stream().anyMatch(entry->entry.baseStack().is(ThaumcraftCrystalClusters.MIXED.get().asItem())),"Mage table contains real mixed crystal");
                if(type==2)a.require(entries.stream().noneMatch(entry->entry.baseStack().is(ThaumcraftColoredCandles.byMeta(15))),"Stalker original0..14 candle loop excludes black15");
            }
        }
        void curse(){
            var item=ThaumcraftPech.FOCUS_PECH.get();ItemStack focus=a.focus(item,FocusUpgradeType.potency,1,FocusUpgradeType.extend,2,FocusUpgradeType.frugal,3,FocusUpgradeType.potency,4,FocusUpgradeType.nightshade,5);
            a.equip(focus,"gold",100);a.aim();a.require(item.getVisCost(focus).size()==6&&item.getVisCost(a.wand).size()==3&&ItemFocusPech.activationCost(a.wand,a.player).values().stream().allMatch(cost->cost==9),"real Nightshade tooltip versus outer-wand base-three payment quirk with Frugal1");
            a.begin();var blasts=a.projectiles(EntityPechBlast.class);a.require(blasts.size()==1,"actual native wand RMB creates one paid Pech curse");var blast=blasts.getFirst();owned.add(blast);
            a.require(blast.getStrength()==2&&blast.getDurationLevel()==1&&blast.isNightshade()&&blast.getOwner()==a.player&&a.vis("terra")==91&&a.vis("aqua")==91&&a.vis("perditio")==91&&a.vis("ignis")==100,"native curse carries real focus power/duration/Nightshade and exact debit");
            a.player.gameMode.useItem(a.player,a.level,a.wand,InteractionHand.MAIN_HAND);a.require(a.projectiles(EntityPechBlast.class).size()==1&&a.vis("terra")==91,"actual250ms cooldown blocks immediate native duplicate");
            blast.discard();a.player.setPos(a.root.getX()+4,a.root.getY(),a.root.getZ());var pig=mob(EntityType.PIG,0,0);var immune=pech(0,1);pig.setInvulnerable(true);
            a.put(a.root.east(),Blocks.STONE.defaultBlockState());blast=new EntityPechBlast(a.level,a.player,2,1,true);blast.setPos(a.root.getX()+.5,a.root.getY()+.5,a.root.getZ()+.5);blast.setDeltaMovement(1,0,0);a.level.addFreshEntity(blast);owned.add(blast);blast.tick();
            a.require(blast.isRemoved()&&pig.getHealth()==pig.getMaxHealth()&&pig.hasEffect(MobEffects.POISON)&&pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&pig.hasEffect(MobEffects.WEAKNESS),"actual native block collision splashes ALL three Nightshade effects despite refused damage");
            a.require(pig.getEffect(MobEffects.POISON).getDuration()==140&&pig.getEffect(MobEffects.POISON).getAmplifier()==2&&pig.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==3&&!immune.hasEffect(MobEffects.POISON),"original100+40E duration/P,P+1 amplifiers and Pech immunity");
            Set<Object> effects=new HashSet<>();
            for(int seed=0;seed<12;seed++){
                pig.removeAllEffects();blast=new EntityPechBlast(a.level,a.player,1,0,false);blast.setPos(a.root.getX()+.5,a.root.getY()+.5,a.root.getZ()+.5);blast.setDeltaMovement(1,0,0);blast.getRandom().setSeed(seed);a.level.addFreshEntity(blast);owned.add(blast);blast.tick();
                a.require(pig.getActiveEffects().size()==1,"ordinary actual splash chooses one status per living victim");effects.add(pig.getActiveEffects().iterator().next().getEffect());
            }
            a.require(effects.size()==3,"declared12 projectile RNG seeds cover Poison/Slowness/Weakness");
            blast=new EntityPechBlast(a.level,a.player,2,1,true);CompoundTag state=new CompoundTag();blast.saveWithoutId(state);state.putInt("PechBlastAge",500);var loaded=new EntityPechBlast(a.level);loaded.load(state);loaded.setPos(a.root.above(3).getCenter());owned.add(loaded);a.level.addFreshEntity(loaded);loaded.tick();
            a.require(loaded.isRemoved()&&loaded.getStrength()==0&&!loaded.isNightshade(),"reload preserves original nonpersisted power defaults while saved age cannot rejuvenate500tick lifetime");
            a.equip(focus,"gold",8);ItemStack before=a.wand.copy();a.begin();a.require(a.projectiles(EntityPechBlast.class).isEmpty()&&ItemStack.matches(before,a.wand),"one-short actual curse payment refuses all three debits and spawn atomically");
        }
        void beans(){
            var bean=ThaumcraftManaBeans.MANA_BEAN.get();ItemStack typed=ItemManaBean.create(Aspect.FIRE);a.require(bean.getUseDuration(typed,a.player)==10&&bean.getAspects(typed).getAmount(Aspect.FIRE)==1,"actual mana bean has original10tick food and typed aspect1");
            ItemStack named=new ItemStack(bean);named.set(DataComponents.CUSTOM_NAME,Component.literal("unrelated legacy NBT"));bean.inventoryTick(named,a.level,a.player,0,false);a.require(ItemManaBean.data(named)==null,"unrelated native component patch prevents original lazy reroll");
            ItemStack fresh=new ItemStack(bean);long worldSeed=154;var expected=RandomSource.create(worldSeed);a.level.random.setSeed(worldSeed);bean.inventoryTick(fresh,a.level,a.player,0,false);a.require(ItemManaBean.data(fresh)!=null&&a.level.random.nextLong()==expected.nextLong(),"fresh ANY-aspect initialization consumes item-local RNG only");
            bean.setAspects(fresh,new AspectList());bean.inventoryTick(fresh,a.level,a.player,0,false);a.require(ItemManaBean.data(fresh).isEmpty(),"explicit initialized-empty contents never reroll");
            var support=a.root.above(2);a.put(support,Blocks.OAK_LOG.defaultBlockState());a.player.setPos(a.root.getX()+2,a.root.getY(),a.root.getZ());a.player.setItemInHand(InteractionHand.MAIN_HAND,typed);
            var context=new UseOnContext(a.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(support),Direction.DOWN,support,false));
            a.require(!bean.useOn(context).consumesAction()&&typed.getCount()==1&&a.level.isEmptyBlock(support.below()),"ordinary untagged biome cannot plant or spend a bean");
            try(var magical=new MagicalBiomeFixture(a.level.getBiome(support))){
                a.require(bean.useOn(context).consumesAction()&&typed.isEmpty()&&a.level.getBlockEntity(support.below()) instanceof TileManaPod,"actual server use places a native pod and consumes exactly one bean under declared MAGICAL biome fixture");
                var pod=(TileManaPod)a.level.getBlockEntity(support.below());a.require(pod.getAspect()==Aspect.FIRE&&pod.getBlockState().getValue(BlockManaPod.AGE)==0,"planted pod copies first aspect without inventing growth");
                long growthSeed=growthSeed();var growthRng=RandomSource.create(growthSeed);growthRng.nextInt(30);a.level.random.setSeed(growthSeed);
                pod.getBlockState().randomTick(a.level,support.below(),RandomSource.create(999));
                a.require(pod.getBlockState().getValue(BlockManaPod.AGE)==1&&a.level.random.nextLong()==growthRng.nextLong(),"actual block randomTick uses original WORLD1/30 gate, not caller RNG");
                for(int i=0;i<6;i++)pod.checkGrowth();a.require(pod.getBlockState().getValue(BlockManaPod.AGE)==7&&pod.getAspects().getAmount(Aspect.FIRE)==1&&pod.getBlockState().getLightEmission()==7,"seven real growth calls mature typed luminous pod");
                a.require(!pod.takeFromContainer(Aspect.FIRE,1)&&!pod.doesContainerAccept(Aspect.FIRE)&&pod.addToContainer(Aspect.FIRE,1)==0,"mature original aspect report is not an invented essentia extraction/insertion endpoint");
                CompoundTag saved=pod.saveWithFullMetadata(a.level.registryAccess());pod.setAspect(null);pod.loadWithComponents(saved,a.level.registryAccess());a.require(pod.getAspect()==Aspect.FIRE,"actual pod BE aspect NBT round trip");
                a.level.random.setSeed(1);a.level.destroyBlock(support.below(),true);var drops=a.items().stream().filter(item->item.getItem().is(bean)).toList();a.require(!drops.isEmpty()&&drops.size()<=2&&drops.stream().allMatch(item->ItemManaBean.data(item.getItem()).firstTag().equals("ignis")),"actual mature native harvest uses BLOCK_ENTITY snapshot and yields one/two typed beans");owned.addAll(drops);
                // Crossbreed order: self Fire, NORTH Air, SOUTH Earth. Air+Fire
                // and Fire+Air each enter twice; all input/combination weights
                // are read from the original ordered list, not sorted tags.
                a.put(support.below(),ThaumcraftManaBeans.MANA_POD.get().defaultBlockState().setValue(BlockManaPod.AGE,2));pod=(TileManaPod)a.level.getBlockEntity(support.below());pod.setAspect(Aspect.FIRE);
                a.put(support.north(),Blocks.OAK_LOG.defaultBlockState());a.put(support.north().below(),ThaumcraftManaBeans.MANA_POD.get().defaultBlockState());((TileManaPod)a.level.getBlockEntity(support.north().below())).setAspect(Aspect.AIR);
                a.put(support.south(),Blocks.OAK_LOG.defaultBlockState());a.put(support.south().below(),ThaumcraftManaBeans.MANA_POD.get().defaultBlockState());((TileManaPod)a.level.getBlockEntity(support.south().below())).setAspect(Aspect.EARTH);
                var parents=new Aspect[]{Aspect.FIRE,Aspect.AIR,Aspect.EARTH};var choices=new ArrayList<Aspect>();for(int i=0;i<parents.length;i++){choices.add(parents[i]);for(int j=0;j<parents.length;j++)if(i!=j){var combined=Aspect.getCombinationResult(parents[i],parents[j]);if(combined!=null){choices.add(combined);choices.add(combined);}}}
                var rng=RandomSource.create(17);Aspect chosen=choices.get(rng.nextInt(choices.size()));a.level.random.setSeed(17);pod.checkGrowth();a.require(pod.getAspect()==chosen&&a.level.random.nextLong()==rng.nextLong(),"third growth uses original N/S/W/E ordered duplicate combination weights and exactly one WORLD draw");
            }
            ItemStack restoredBiomeBean=ItemManaBean.create(Aspect.FIRE);a.player.setItemInHand(InteractionHand.MAIN_HAND,restoredBiomeBean);
            var restoredContext=new UseOnContext(a.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(support),Direction.DOWN,support,false));
            a.require(!bean.useOn(restoredContext).consumesAction()&&restoredBiomeBean.getCount()==1,"restored original biome tags again refuse planting without spending");
            a.require(BlockManaPod.isSupport(Blocks.OAK_WOOD.defaultBlockState())&&!BlockManaPod.isSupport(Blocks.STRIPPED_OAK_LOG.defaultBlockState())&&!BlockManaPod.isSupport(Blocks.MANGROVE_LOG.defaultBlockState()),"old LOG axis12 maps to native bark WOOD; stripped/new species are not invented supports");
            clear();for(int i=0;i<4;i++)pech(i,0);
            try(var magical=new MagicalBiomeFixture(a.level.getBiome(a.root))){
                var rng=RandomSource.create(7183);var crowdExpected=RandomSource.create(7183);
                a.require(!SpawnPlacements.checkSpawnRules(ThaumcraftPech.PECH.get(),a.level,MobSpawnType.NATURAL,a.root,rng)&&rng.nextLong()==crowdExpected.nextLong(),"actual registered Pech spawn predicate rejects four nearby Pech BEFORE light RNG");
            }
            clear();
            // Real native10tick eat completion: pick null slot0 so the fixture
            // cannot gain a random harmful effect before checking one food debit.
            ItemStack meal=ItemManaBean.create(Aspect.FIRE);meal.setCount(2);a.player.setItemInHand(InteractionHand.MAIN_HAND,meal);a.player.getFoodData().setFoodLevel(10);a.player.getFoodData().setSaturation(0);
            long seed=foodSeed(0,false);a.level.random.setSeed(seed);a.require(a.player.gameMode.useItem(a.player,a.level,meal,InteractionHand.MAIN_HAND).consumesAction(),"actual native food RMB begins ten-tick use");a.tick(10);
            a.require(meal.getCount()==1&&a.player.getFoodData().getFoodLevel()==11&&!a.player.isUsingItem(),"actual completed mana bean pays ONE food and grants original nutrition1");
            ItemStack saturation=ItemManaBean.create(Aspect.FIRE);saturation.setCount(2);a.player.setItemInHand(InteractionHand.MAIN_HAND,saturation);a.player.getFoodData().setFoodLevel(10);a.player.getFoodData().setSaturation(0);a.level.random.setSeed(foodSeed(23,false));
            a.require(a.player.gameMode.useItem(a.player,a.level,saturation,InteractionHand.MAIN_HAND).consumesAction(),"actual Saturation-slot meal begins");a.tick(10);
            a.require(saturation.getCount()==1&&a.player.getFoodData().getFoodLevel()==11,"original instant Saturation23 is a no-op, not native extra nutrition");
            int pool=KnowledgeAccess.get(a.player).getAspectPoolFor("ignis");ItemStack rewardMeal=ItemManaBean.create(Aspect.FIRE);rewardMeal.setCount(2);a.player.setItemInHand(InteractionHand.MAIN_HAND,rewardMeal);a.level.random.setSeed(foodSeed(0,true));
            a.require(a.player.gameMode.useItem(a.player,a.level,rewardMeal,InteractionHand.MAIN_HAND).consumesAction(),"actual RP-award meal begins");a.tick(10);
            a.require(rewardMeal.getCount()==1&&KnowledgeAccess.get(a.player).getAspectPoolFor("ignis")==pool+1,"actual25% typed meal awards exactly ONE first-aspect pool point after food payment");
            int filled=0;for(int i=0;i<40;i++)if(ItemManaBean.effectAt(i)!=null)filled++;a.require(filled==32&&ItemManaBean.effectAt(24)==null&&ItemManaBean.effectAt(31).value()==ThaumcraftInfusion.FLUX_TAINT.get()&&ItemManaBean.effectAt(39).value()==ThaumcraftWarp.THAUMARHIA.get(),"canonical40-slot array retains eight nulls and TC4 custom31..39");
        }
        private static long growthSeed(){for(long seed=0;seed<10000;seed++)if(RandomSource.create(seed).nextInt(30)==0)return seed;throw new AssertionError();}
        private static long foodSeed(int slot,boolean reward){for(long seed=0;seed<100000;seed++){var rng=RandomSource.create(seed);if(rng.nextInt(40)==slot&&(rng.nextFloat()<.25F)==reward)return seed;}throw new AssertionError();}
        @Override public void close(){try{clear();KnowledgeAccess.get(a.player).load(knowledge);}finally{try{extendedFloor.forEach(a::put);}finally{a.close();}}}
    }
    private static final class MagicalBiomeFixture implements AutoCloseable {
        final Holder.Reference<Biome> biome;final List<TagKey<Biome>> original;final Method bind;
        @SuppressWarnings("unchecked")MagicalBiomeFixture(Holder<Biome> holder){
            if(!(holder instanceof Holder.Reference<?> reference))throw new IllegalStateException("Test biome must be a real registry holder");biome=(Holder.Reference<Biome>)reference;original=biome.tags().toList();
            try{bind=Holder.Reference.class.getDeclaredMethod("bindTags",Collection.class);bind.setAccessible(true);var tags=new ArrayList<>(original);tags.add(ThaumcraftManaBeans.MAGICAL_BIOMES);bind.invoke(biome,tags);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        }
        @Override public void close(){try{bind.invoke(biome,original);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    }
}
