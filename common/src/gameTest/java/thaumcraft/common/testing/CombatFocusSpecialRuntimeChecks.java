package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.entities.projectile.EntityShockOrb;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.WandFocusActions;

/** Real connected native RMB/held ticks, ADD/hurt refusals and public projectile
 * flight. High owned fixtures isolate chain's8-block neighbourhood. Probability
 * input is declared only for the ember's original ignition chance. */
public final class CombatFocusSpecialRuntimeChecks {
    private CombatFocusSpecialRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try (var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,170,4))) {
            a.clearLane(); upgrades(a); fire(a); frost(a); earthshock(a); chain(a);
        }
        test.succeed();
    }
    private static void price(FocusUpgradeRuntimeChecks.Actor a,String tag,int amount) {
        for(String primal:WandData.PRIMALS)a.require(a.vis(primal)==100-(primal.equals(tag)?amount:0),"exact committed special focus debit "+primal);
    }
    private static Cow cow(FocusUpgradeRuntimeChecks.Actor a,Vec3 pos) {
        var cow=EntityType.COW.create(a.level);a.require(cow!=null,"actual native cow type exists");
        cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(pos);
        a.require(a.level.addFreshEntity(cow),"owned native target enters the real index");return cow;
    }
    private static ItemStack fireball(FocusUpgradeRuntimeChecks.Actor a) {
        return a.focus(ThaumcraftFoci.FIRE_FOCUS.get(),FocusUpgradeType.potency,1,FocusUpgradeType.alchemistsfire,2,
                FocusUpgradeType.fireball,3,FocusUpgradeType.potency,4,FocusUpgradeType.potency,5);
    }
    private static void upgrades(FocusUpgradeRuntimeChecks.Actor a) {
        var fire=ThaumcraftFoci.FIRE_FOCUS.get();ItemStack ball=fireball(a);
        var shown=fire.getVisCost(ball);
        a.require(shown.getAmount(Aspect.FIRE)==66&&shown.getAmount(Aspect.ENTROPY)==33&&shown.size()==2
                &&fire.getActivationCooldown(ball)==1000&&fire.getAnimation(ball)==ItemFocusBasic.WandFocusAnimation.WAVE&&fire.isVisCostPerTick(ball),
                "original Fireball tooltip66/33,1000ms,WAVE and per-tick-label quirk survive");
        var gate=a.focus(fire,FocusUpgradeType.alchemistsfire,2,FocusUpgradeType.fireball,3);
        a.require(!fire.applyUpgrade(gate,FocusUpgradeType.alchemistsfire,4),"original Fireball refuses a second alchemist rank after an existing one");
        var beam=a.focus(fire,FocusUpgradeType.alchemistsfire,2,FocusUpgradeType.firebeam,3,FocusUpgradeType.alchemistsfire,4);
        a.require(fire.getUpgradeLevel(beam,FocusUpgradeType.alchemistsfire)==2,"beam retains both original alchemist slots");
        var shock=ThaumcraftFoci.SHOCK_FOCUS.get();
        a.require(!shock.applyUpgrade(new ItemStack(shock),FocusUpgradeType.enlarge,4),"Enlarge requires Chain or Earthshock");
        ItemStack raw=new ItemStack(shock);raw.set(ThaumcraftFoci.FOCUS_UPGRADES.get(),new FocusUpgradeData(List.of((short)-1,(short)-1,(short)-1,(short)3,(short)-1)));
        a.require(!FocusData.isSupportedFocus(raw),"direct component data cannot bypass the Enlarge prerequisite");
    }
    private static void fire(FocusUpgradeRuntimeChecks.Actor a) {
        ItemStack focus=fireball(a);a.equip(focus,"gold",100);a.begin();
        var orbs=a.projectiles(EntityExplosiveOrb.class);
        a.require(orbs.size()==1&&!a.player.isUsingItem()&&orbs.getFirst().getStrength()==2.2F&&orbs.getFirst().isFiery(),
                "actual Fireball activation spawns one Potency3/alchemist orb without starting a channel");
        price(a,"ignis",10);
        a.require(WandFocusActions.cooldownMillis(a.player)>0,"actual Fireball activation installs its real1000ms dispatcher deadline");
        a.player.gameMode.useItem(a.player,a.level,a.wand,InteractionHand.MAIN_HAND);
        a.require(a.projectiles(EntityExplosiveOrb.class).size()==1&&a.vis("ignis")==90,"immediate real cooldown replay creates no orb or second debit");
        EntityExplosiveOrb orb=orbs.getFirst();var saved=new CompoundTag();orb.saveWithoutId(saved);var loaded=new EntityExplosiveOrb(a.level);loaded.load(saved);
        a.require(loaded.getOwner()==a.player&&loaded.getStrength()==2.2F&&loaded.isFiery()&&loaded.getAge()==orb.getAge(),"actual orb NBT retains paid specialization and native ownership");loaded.discard();
        a.purgeOutputs();
        for(GameType mode:List.of(GameType.SURVIVAL,GameType.CREATIVE)) {
            a.player.setGameMode(mode);a.equip(focus,"gold",9);var before=a.wand.copy();a.begin();
            a.require(a.projectiles(EntityExplosiveOrb.class).isEmpty()&&ItemStack.matches(a.wand,before)&&!a.player.isUsingItem(),"unfunded "+mode+" Fireball is atomic and creates no channel/projectile");
        }
        a.player.setGameMode(GameType.CREATIVE);a.equip(focus,"gold",100);a.begin();price(a,"ignis",10);a.purgeOutputs();a.player.setGameMode(GameType.SURVIVAL);
        a.equip(focus,"gold",100);int[] attempts={0};
        EntityEvent.Add cancel=(entity,world)->{
            if(world!=a.level||!(entity instanceof EntityExplosiveOrb candidate)||candidate.getOwner()!=a.player)return EventResult.pass();
            attempts[0]++;a.require(a.vis("ignis")==90,"native Fireball ADD callback sees already committed payment");return EventResult.interruptFalse();
        };
        EntityEvent.ADD.register(cancel);try{a.begin();}finally{EntityEvent.ADD.unregister(cancel);}
        a.require(attempts[0]==1&&a.projectiles(EntityExplosiveOrb.class).isEmpty(),"one cancelled native Fireball spawn stays paid with no retry/refund");price(a,"ignis",10);

        a.clearLane();ItemStack beam=a.focus(ThaumcraftFoci.FIRE_FOCUS.get(),FocusUpgradeType.potency,1,FocusUpgradeType.alchemistsfire,2,
                FocusUpgradeType.firebeam,3,FocusUpgradeType.alchemistsfire,4,FocusUpgradeType.potency,5);
        a.equip(beam,"gold",100);a.begin();a.tick(1);a.player.stopUsingItem();var embers=a.projectiles(EntityEmber.class);
        a.require(embers.size()==4&&embers.stream().allMatch(e->e.getDamage()==6.75F&&e.getScatter()==.25F&&e.getDuration()==30&&e.getFirey()==2&&e.getFireSeconds()==5),
                "actual beam tick creates2+P tightly scattered6.75 damage/30tick/alchemist2 embers");price(a,"ignis",10);
        EntityEmber ignition=embers.getFirst();embers.stream().skip(1).forEach(Entity::discard);
        a.put(a.root,Blocks.STONE.defaultBlockState());a.put(a.root.west().below(),Blocks.STONE.defaultBlockState());
        ignition.setPos(Vec3.atCenterOf(a.root).add(-1.1,0,0));ignition.setDeltaMovement(1.5,0,0);ignition.getRandom().setSeed(4640);ignition.tick();
        a.require(ignition.isRemoved()&&a.level.getBlockState(a.root).is(Blocks.STONE)&&a.level.getBlockState(a.root.west()).is(Blocks.FIRE),
                "declared low-roll real ember collision places supported fire in adjacent AIR without replacing the struck stone");
        a.clearLane();a.equip(focus,"gold",100);a.begin();orb=a.projectiles(EntityExplosiveOrb.class).getFirst();
        a.put(a.root,Blocks.STONE.defaultBlockState());a.put(a.root.below(),Blocks.STONE.defaultBlockState());
        orb.setPos(Vec3.atCenterOf(a.root).add(-1.1,0,0));orb.setDeltaMovement(1.3,0,0);orb.tick();
        a.require(orb.isRemoved()&&a.level.getBlockState(a.root).is(Blocks.STONE)&&a.level.getBlockState(a.root.below()).is(Blocks.STONE)&&a.items().isEmpty(),
                "actual Fireball impact/explosion preserves native terrain and creates no block loot");
        a.clearLane();a.equip(focus,"gold",100);a.begin();orb=a.projectiles(EntityExplosiveOrb.class).getFirst();
        Cow direct=cow(a,Vec3.atBottomCenterOf(a.root));direct.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);direct.setHealth(100);
        EntityExplosiveOrb projectile=orb;int[] directHits={0},explosions={0};
        EntityEvent.LivingHurt observe=(victim,source,amount)->{
            if(victim!=direct)return EventResult.pass();
            if(source.getDirectEntity()==projectile){directHits[0]++;a.require(source.is(EntityExplosiveOrb.DAMAGE_TYPE)&&source.getEntity()==a.player
                            &&source.is(DamageTypeTags.IS_FIRE)&&source.is(DamageTypeTags.IS_PROJECTILE)&&source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                            &&!source.is(DamageTypeTags.BYPASSES_ARMOR)&&amount==projectile.getStrength()*1.5F,
                    "actual direct orb hurt retains original fire+magic/projectile flags, owner and strength*1.5 without armour bypass");}
            if(source.is(DamageTypes.EXPLOSION)){explosions[0]++;a.require(source.getEntity()==null,"original explosion itself has no caster/source owner");}
            return EventResult.pass();
        };
        EntityEvent.LIVING_HURT.register(observe);
        try {orb.setPos(direct.position().add(-1.8,.7,0));orb.setDeltaMovement(3,0,0);orb.tick();
            a.require(orb.isRemoved()&&directHits[0]==1&&explosions[0]==1&&direct.getHealth()<100,"actual native orb flight performs one direct hit and one separate ownerless explosion");
        }finally{EntityEvent.LIVING_HURT.unregister(observe);direct.discard();}
    }
    private static void frost(FocusUpgradeRuntimeChecks.Actor a) {
        a.clearLane();var item=ThaumcraftFoci.FROST_FOCUS.get();
        ItemStack scatter=a.focus(item,FocusUpgradeType.alchemistsfrost,1,FocusUpgradeType.potency,2,FocusUpgradeType.scattershot,3,
                FocusUpgradeType.potency,4,FocusUpgradeType.alchemistsfrost,5);
        a.equip(scatter,"gold",100);a.begin();var shots=a.projectiles(EntityFrostShard.class);
        a.require(shots.size()==9&&shots.stream().allMatch(s->s.getDamage()==1&&s.isFragile()&&s.getFrosty()==2&&s.getBounceStrength()==.5),"real Scattershot creates5+2P distinct fragile/slowing shards");
        a.require(a.vis("aqua")==95&&a.vis("ignis")==98&&a.vis("perditio")==98&&a.vis("aer")==100&&a.vis("terra")==100&&a.vis("ordo")==100,
                "actual Scattershot pays original WAND5/2/2, without tooltip20aqua5aer surcharge");
        a.require(item.getVisCost(scatter).getAmount(Aspect.WATER)==20&&item.getVisCost(scatter).getAmount(Aspect.AIR)==5&&item.getActivationCooldown(scatter)==500,
                "the actual scatter focus advertises its separate20aqua5aer tooltip and500ms cooldown");
        Cow target=cow(a,Vec3.atBottomCenterOf(a.root));
        try {
            for(int i=0;i<2;i++){var shot=shots.get(i);shot.setPos(target.position().add(-1.8,.7,0));shot.setDeltaMovement(3,0,0);shot.tick();a.require(shot.isRemoved(),"a fragile native entity collision consumes the shot");}
            var slow=target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            a.require(target.getHealth()==target.getMaxHealth()-2&&target.invulnerableTime==0&&slow!=null&&slow.getDuration()==200&&slow.getAmplifier()==1,
                    "two real fragile collisions hit without waiting for immunity; frost2 applies200tick SlownessII");
            target.removeAllEffects();float health=target.getHealth();
            EntityEvent.LivingHurt protect=(victim,source,amount)->victim==target?EventResult.interruptFalse():EventResult.pass();
            EntityEvent.LIVING_HURT.register(protect);
            try {var shot=shots.get(2);shot.setPos(target.position().add(-1.8,.7,0));shot.setDeltaMovement(3,0,0);shot.tick();
                a.require(shot.isRemoved()&&target.getHealth()==health&&target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"original frost/fragile effects still follow a real native damage refusal");
            } finally {EntityEvent.LIVING_HURT.unregister(protect);}
        } finally {target.discard();}a.purgeOutputs();
        ItemStack boulder=a.focus(item,FocusUpgradeType.alchemistsfrost,1,FocusUpgradeType.potency,2,FocusUpgradeType.iceboulder,3,FocusUpgradeType.potency,4,FocusUpgradeType.alchemistsfrost,5);
        a.equip(boulder,"gold",100);a.begin();var shard=a.projectiles(EntityFrostShard.class).getFirst();
        a.require(shard.getDamage()==8&&shard.getBounceStrength()==.8&&shard.getBounceLimit()==6&&!shard.isFragile()&&shard.getFrosty()==2,"real Ice Boulder has8 damage,.8 bounce,six remaining bounces and frost2");
        a.put(a.root,Blocks.STONE.defaultBlockState());
        for(int impact=1;impact<=7;impact++) {
            shard.setPos(Vec3.atCenterOf(a.root).add(-1.1,0,0));shard.setDeltaMovement(1.3,0,0);shard.tick();
            a.require(shard.getImpactCount()==impact&&shard.getBounceLimit()==6-impact&&shard.isRemoved()==(impact==7),"original Boulder post-decrement survives six actual impacts; seventh terminates "+impact);
            if(impact==3){var tag=new CompoundTag();shard.saveWithoutId(tag);var loaded=new EntityFrostShard(a.level);loaded.load(tag);
                a.require(loaded.isBoulder()&&loaded.getFrosty()==2&&loaded.getDamage()==8&&loaded.getBounceLimit()==3&&loaded.getAge()==3,"native advanced Frost NBT retains specialization and earned impacts");loaded.discard();}
        }
        a.purgeOutputs();
    }
    private static void chain(FocusUpgradeRuntimeChecks.Actor a) {
        a.clearLane();a.aim();
        AABB neighbourhood=new AABB(Vec3.atCenterOf(a.root),Vec3.atCenterOf(a.root)).inflate(24,5,24);
        a.require(a.level.getEntities((Entity)null,neighbourhood,e->e instanceof net.minecraft.world.entity.LivingEntity&&e!=a.player).isEmpty(),"owned chain neighbourhood has no unrelated living entity to damage");
        ItemStack focus=a.focus(ThaumcraftFoci.SHOCK_FOCUS.get(),FocusUpgradeType.potency,1,FocusUpgradeType.potency,2,
                FocusUpgradeType.chainlightning,3,FocusUpgradeType.enlarge,4,FocusUpgradeType.enlarge,5);
        var shown=ThaumcraftFoci.SHOCK_FOCUS.get().getVisCost(focus);
        a.require(shown.getAmount(Aspect.AIR)==40&&shown.getAmount(Aspect.WATER)==10&&ThaumcraftFoci.SHOCK_FOCUS.get().getActivationCooldown(focus)==500,"original Chain tooltip40aer10aqua and500ms");
        List<Cow> targets=new ArrayList<>();
        try {
            for(Vec3 offset:List.of(new Vec3(.5,0,.5),new Vec3(.5,0,3.5),new Vec3(-2.5,0,3.5),new Vec3(-2.5,0,-2.5),new Vec3(.5,0,-2.5),new Vec3(3.5,0,-2.5),new Vec3(3.5,0,3.5)))
                targets.add(cow(a,Vec3.atLowerCornerOf(a.root).add(offset)));
            a.equip(focus,"gold",100);a.begin();a.tick(1);a.player.stopUsingItem();price(a,"aer",25);
            a.require(targets.getFirst().getHealth()==targets.getFirst().getMaxHealth()-8&&targets.stream().skip(1).allMatch(c->c.getHealth()==c.getMaxHealth()-6),
                    "actual Chain primary6+P and exactly2+2Enlarge distinct secondary4+P jumps run with one base25aer payment");
        } finally {targets.forEach(Entity::discard);}
        a.clearLane();
        Cow primary=cow(a,Vec3.atBottomCenterOf(a.root));Cow secondary=cow(a,Vec3.atBottomCenterOf(a.root).add(0,0,3));
        a.equip(focus,"gold",100);ItemStack original=a.wand;ItemStack replacement=original.copy();
        EntityEvent.LivingHurt replace=(victim,source,amount)->{
            if(victim==primary&&source.getEntity()==a.player)a.player.setItemInHand(InteractionHand.MAIN_HAND,replacement);
            return EventResult.pass();
        };
        EntityEvent.LIVING_HURT.register(replace);
        try {a.begin();a.tick(1);a.player.stopUsingItem();
            a.require(a.player.getMainHandItem()==replacement&&ItemStack.matches(replacement,FocusUpgradeRuntimeChecks.Actor.charged(focus,"gold",100))
                            &&thaumcraft.common.items.wands.ItemWandCasting.getVis(original,"aer")==75&&secondary.getHealth()==secondary.getMaxHealth(),
                    "real hurt callback hand replacement cannot inherit a paid chain or debit the replacement wand");
        } finally {EntityEvent.LIVING_HURT.unregister(replace);primary.discard();secondary.discard();}
    }
    private static void earthshock(FocusUpgradeRuntimeChecks.Actor a) {
        a.clearLane();var item=ThaumcraftFoci.SHOCK_FOCUS.get();
        ItemStack focus=a.focus(item,FocusUpgradeType.potency,1,FocusUpgradeType.potency,2,FocusUpgradeType.earthshock,3,
                FocusUpgradeType.enlarge,4,FocusUpgradeType.enlarge,5);
        var shown=item.getVisCost(focus);
        a.require(shown.size()==2&&shown.getAmount(Aspect.AIR)==75&&shown.getAmount(Aspect.EARTH)==25&&item.getActivationCooldown(focus)==1000
                        &&item.getAnimation(focus)==ItemFocusBasic.WandFocusAnimation.WAVE,"original Earthshock advertises75aer25terra,WAVE and1000ms");
        a.equip(focus,"gold",100);a.begin();var orbs=a.projectiles(EntityShockOrb.class);
        a.require(orbs.size()==1&&orbs.getFirst().getArea()==8&&orbs.getFirst().getDamage()==7&&!a.player.isUsingItem(),
                "actual Earthshock RMB creates one area4+2Enlarge/damage-truncated5+1.33P orb without a channel");
        price(a,"aer",25);a.require(WandFocusActions.cooldownMillis(a.player)>0,"native Earthshock dispatcher owns its actual1000ms deadline");
        a.purgeOutputs();
    }
}
