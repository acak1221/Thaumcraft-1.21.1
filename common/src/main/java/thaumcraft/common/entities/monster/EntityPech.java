package thaumcraft.common.entities.monster;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.entities.ai.pech.PechItemGoal;
import thaumcraft.common.entities.ai.pech.PechAvoidGoal;
import thaumcraft.common.entities.ai.pech.PechMeleeGoal;
import thaumcraft.common.entities.ai.pech.PechRangedGoal;
import thaumcraft.common.entities.ai.pech.PechWatchGoal;
import thaumcraft.common.entities.projectile.EntityPechBlast;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalMobTargeting;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Original neutral Pech: three equipment-defined variants, nine saved pack
 * slots, greedy taming, group anger and one-customer native trade sessions.
 * Current navigation/combat/equipment-enchantment geometry is the platform
 * adapter; TC4 gates, own RNG decisions and pack semantics remain explicit. */
public final class EntityPech extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> TYPE=SynchedEntityData.defineId(EntityPech.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(EntityPech.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> TAMED=SynchedEntityData.defineId(EntityPech.class,EntityDataSerializers.BYTE);
    public final ItemStack[] loot=new ItemStack[9];
    public boolean trading,updateAINextTick;
    public float mumble;
    private int chargeCount;
    private LivingEntity legacyTarget;
    private UUID customer;
    private PechMeleeGoal melee;private PechRangedGoal arrows,blasts;private PechAvoidGoal avoid;
    public EntityPech(EntityType<? extends EntityPech> type,Level level){
        super(type,level);xpReward=5;Arrays.fill(loot,ItemStack.EMPTY);
        if(getNavigation() instanceof GroundPathNavigation navigation){navigation.setCanOpenDoors(true);navigation.setCanFloat(false);}
        setDropChance(EquipmentSlot.MAINHAND,.2F);if(!level.isClientSide)setCombatTask();
    }
    public static AttributeSupplier.Builder createAttributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.5);}
    @Override protected void registerGoals(){
        melee=new PechMeleeGoal(this);arrows=new PechRangedGoal(this,50);blasts=new PechRangedGoal(this,30);avoid=new PechAvoidGoal(this);
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
            @Override public boolean canUse(){return isAlive()&&!isInWater()&&isTamed()&&onGround()&&!hurtMarked&&trading;}
            @Override public void start(){getNavigation().stop();}
            @Override public void stop(){trading=false;}
        });
        goalSelector.addGoal(3,new PechItemGoal(this));goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.5));goalSelector.addGoal(6,new MoveThroughVillageGoal(this,1,false,0,()->true));
        goalSelector.addGoal(9,new RandomStrollGoal(this,.6));
        goalSelector.addGoal(9,new PechWatchGoal(this,true));goalSelector.addGoal(10,new PechWatchGoal(this,false));goalSelector.addGoal(11,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(TYPE,(byte)0);builder.define(ANGER,0);builder.define(TAMED,(byte)0);}
    public int getPechType(){return entityData.get(TYPE);}
    public void setPechType(int value){entityData.set(TYPE,(byte)value);}
    public int getAnger(){return entityData.get(ANGER);}
    public void setAnger(int value){entityData.set(ANGER,(int)(short)value);}
    public boolean isTamed(){return entityData.get(TAMED)==1;}
    public void setTamed(boolean value){entityData.set(TAMED,value?(byte)1:(byte)0);}
    @Override public Component getName(){return hasCustomName()?getCustomName():Component.translatable(switch(getPechType()){case 1->"entity.Thaumcraft.Pech.1.name";case 2->"entity.Thaumcraft.Pech.2.name";default->"entity.Thaumcraft.Pech.name";});}
    @Override public int getArmorValue(){return Math.min(20,super.getArmorValue()+2);}
    @Override public boolean canBeLeashed(){return false;}
    public int occupiedPackSlots(){int count=0;for(ItemStack stack:loot)if(!stack.isEmpty())count++;return count;}
    @Override public boolean removeWhenFarAway(double distance){return occupiedPackSlots()<5;}
    @Override public boolean requiresCustomPersistence(){return occupiedPackSlots()>=5||super.requiresCustomPersistence();}
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack){super.setItemSlot(slot,stack);if(!level().isClientSide&&slot==EquipmentSlot.MAINHAND)updateAINextTick=true;}
    public void setCombatTask(){
        if(melee==null)return;goalSelector.removeGoal(melee);goalSelector.removeGoal(arrows);goalSelector.removeGoal(blasts);
        ItemStack item=getMainHandItem();goalSelector.addGoal(2,item.is(Items.BOW)?arrows:item.getItem() instanceof ItemWandCasting?blasts:melee);
        goalSelector.removeGoal(avoid);if(!isTamed())goalSelector.addGoal(4,avoid);
    }
    @Override protected void customServerAiStep(){if(updateAINextTick){updateAINextTick=false;setCombatTask();}super.customServerAiStep();if(tickCount%40==0)heal(1);}
    @Override protected void populateDefaultEquipmentSlots(RandomSource random,DifficultyInstance difficulty){
        Item selected=switch(random.nextInt(20)){case 0,12->ThaumcraftContent.WAND.get();case 1->Items.STONE_SWORD;case 3->Items.STONE_AXE;case 5->Items.IRON_SWORD;case 6->Items.IRON_AXE;case 7->Items.FISHING_ROD;case 8->Items.STONE_PICKAXE;case 9->Items.IRON_PICKAXE;case 2,4,10,11,13->Items.BOW;default->null;};
        if(selected==null)return;ItemStack stack=new ItemStack(selected);
        if(selected==ThaumcraftContent.WAND.get()){
            stack.set(ThaumcraftFoci.WAND_FOCUS.get(),new FocusData(new ItemStack(ThaumcraftPech.FOCUS_PECH.get())));
            WandData data=WandData.EMPTY;
            for(Aspect aspect:new Aspect[]{Aspect.EARTH,Aspect.ENTROPY,Aspect.WATER})data=data.withAmount(aspect.getTag(),(2+random.nextInt(6))*100);
            for(Aspect aspect:new Aspect[]{Aspect.AIR,Aspect.FIRE,Aspect.ORDER})data=data.withAmount(aspect.getTag(),random.nextInt(4)*100);
            ItemWandCasting.withData(stack,data); // Original addVis multiplies its whole-vis amount by100.
        }
        setItemSlot(EquipmentSlot.MAINHAND,stack);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){
        // Original calls EntityLiving.finalizeSpawn TWICE. Each adds an independent
        // Gaussian FOLLOW_RANGE modifier. Modern Mob would use triangular WORLD
        // draws + a new left-handed roll, so port the two actual old effects here.
        spawnBonus("pech_spawn_bonus_first");populateDefaultEquipmentSlots(random,difficulty);
        ItemStack held=getMainHandItem();
        if(held.getItem() instanceof ItemWandCasting){setPechType(1);setDropChance(EquipmentSlot.MAINHAND,.1F);}
        else if(!held.isEmpty()){
            if(held.is(Items.BOW))setPechType(2);
            populateDefaultEquipmentEnchantments(level,random,difficulty);
        }
        setCanPickUpLoot(random.nextFloat()<.75F*difficulty.getSpecialMultiplier());spawnBonus("pech_spawn_bonus_second");return data;
    }
    private void spawnBonus(String id){getAttribute(Attributes.FOLLOW_RANGE).addOrReplacePermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("thaumcraft",id),random.nextGaussian()*.05,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));}
    public static boolean checkPechSpawnRules(EntityType<? extends EntityPech> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        if(!populationAllowed(type,level,pos))return false;
        if(level.getDifficulty()==Difficulty.PEACEFUL||level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
        var server=level.getLevel();int light=server.getMaxLocalRawBrightness(pos,server.isThundering()?10:server.getSkyDarken());
        return light<=random.nextInt(8)&&Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
    private static boolean populationAllowed(EntityType<? extends EntityPech> type,ServerLevelAccessor level,BlockPos pos){
        var biome=level.getBiome(pos);var key=biome.unwrapKey().orElse(null);
        if(!biome.is(ThaumcraftManaBeans.MAGICAL_BIOMES)||key!=null&&key.location().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft","taint")))return false;
        if(level.getLevel().dimension()!=Level.OVERWORLD&&(key==null||!key.location().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft","magical_forest"))&&!key.location().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft","eerie"))))return false;
        var box=type.getDimensions().makeBoundingBox(pos.getX()+.5,pos.getY(),pos.getZ()+.5).inflate(16);
        return level.getLevel().getEntitiesOfClass(EntityPech.class,box).size()<4;
    }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return level instanceof ServerLevelAccessor accessor&&populationAllowed(getTypeAsPech(),accessor,blockPosition())&&super.checkSpawnRules(level,reason);}
    @SuppressWarnings("unchecked")private EntityType<? extends EntityPech> getTypeAsPech(){return (EntityType<? extends EntityPech>)getType();}
    public boolean canPickup(ItemStack incoming){
        if(incoming==null||incoming.isEmpty())return false;
        if(!isTamed()&&explicitValue(incoming)>0)return true;
        for(ItemStack stored:loot)if(stored.isEmpty()||ItemStack.isSameItemSameComponents(stored,incoming)&&stored.getCount()+incoming.getCount()<=stored.getMaxStackSize())return true;
        return false;
    }
    public ItemStack pickupItem(ItemStack incoming){
        if(incoming==null||incoming.isEmpty())return ItemStack.EMPTY;
        if(!isTamed()&&isValued(incoming)){
            if(random.nextInt(10)<getValue(incoming)){setTamed(true);updateAINextTick=true;level().broadcastEntityEvent(this,(byte)18);}
            incoming.shrink(1);return incoming.isEmpty()?ItemStack.EMPTY:incoming;
        }
        for(ItemStack stored:loot)if(!stored.isEmpty()&&stored.getCount()<stored.getMaxStackSize()&&ItemStack.isSameItemSameComponents(stored,incoming)){
            int amount=Math.min(incoming.getCount(),stored.getMaxStackSize()-stored.getCount());stored.grow(amount);incoming.shrink(amount);if(incoming.isEmpty())return ItemStack.EMPTY;
        }
        for(int i=0;i<loot.length;i++)if(loot[i].isEmpty()){loot[i]=incoming.copy();return ItemStack.EMPTY;}
        return incoming.isEmpty()?ItemStack.EMPTY:incoming;
    }
    private static int explicitValue(ItemStack stack){
        if(stack.getItem() instanceof ItemManaBean)return 1;
        if(stack.is(Items.GOLD_INGOT)||stack.is(Items.GOLDEN_APPLE)||stack.is(Items.ENCHANTED_GOLDEN_APPLE))return 2;
        if(stack.is(Items.ENDER_PEARL))return 3;if(stack.is(Items.DIAMOND))return 4;if(stack.is(Items.EMERALD))return 5;return 0;
    }
    public boolean isValued(ItemStack stack){return stack!=null&&!stack.isEmpty()&&(explicitValue(stack)>0||ObjectAspectResolver.get(stack,level()).getAmount(Aspect.GREED)>0);}
    public int getValue(ItemStack stack){if(stack==null||stack.isEmpty())return 0;int explicit=explicitValue(stack);return explicit>0?explicit:Math.min(32,ObjectAspectResolver.get(stack,level()).getAmount(Aspect.GREED));}
    public boolean beginTrading(ServerPlayer player){if(!isAlive()||!isTamed()||player.level()!=level()||player.distanceToSqr(this)>64)return false;if(customer!=null)return false;customer=player.getUUID();trading=true;return true;}
    public boolean isCustomer(Player player){return customer!=null&&customer.equals(player.getUUID());}
    public void endTrading(Player player){if(isCustomer(player)){customer=null;trading=false;}}
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.isShiftKeyDown()||player.getItemInHand(hand).is(Items.NAME_TAG))return InteractionResult.PASS;
        if(!level().isClientSide&&player instanceof ServerPlayer server&&beginTrading(server)){
            MenuRegistry.openExtendedMenu(server,new SimpleMenuProvider((id,inventory,actor)->new ContainerPech(id,inventory,this),getName()),buffer->buffer.writeVarInt(getId()));return InteractionResult.CONSUME;
        }
        return super.mobInteract(player,hand);
    }
    private void becomeAngryAt(LivingEntity attacker){
        legacyTarget=attacker;if(getAnger()<=0){level().broadcastEntityEvent(this,(byte)19);playSound(sound("pech_charge"),getSoundVolume(),getVoicePitch());}
        setTarget(attacker);setAnger(400+random.nextInt(400));setTamed(false);updateAINextTick=true;
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerable())return false;
        if(!level().isClientSide&&source.getEntity() instanceof Player attacker){for(EntityPech pech:level().getEntitiesOfClass(EntityPech.class,getBoundingBox().inflate(32,16,32),pech->pech!=this))pech.becomeAngryAt(attacker);becomeAngryAt(attacker);}
        return super.hurt(source,amount);
    }
    @Override public void tick(){
        if(mumble>0)mumble*=.75F;if(getAnger()>0)setAnger(getAnger()-1);
        if(!level().isClientSide&&getAnger()>0&&(legacyTarget==null||getTarget()==null)){
            // The original discards findPlayerToAttack's return. Preserve its
            // query only; reload anger alone must NOT invent a target.
            OriginalMobTargeting.closestVulnerablePlayer(this,16);setTarget(legacyTarget);
            if(legacyTarget!=null){if(chargeCount>0)--chargeCount;if(chargeCount==0){chargeCount=100;playSound(sound("pech_charge"),getSoundVolume(),getVoicePitch());}level().broadcastEntityEvent(this,(byte)17);}
        }
        if(level().isClientSide){if(random.nextInt(15)==0&&getAnger()>0)particle(false);if(random.nextInt(25)==0&&isTamed())particle(true);}
        super.tick();
    }
    @Override public void playAmbientSound(){
        if(!level().isClientSide){
            if(random.nextInt(3)==0&&!level().getEntitiesOfClass(EntityPech.class,getBoundingBox().inflate(4,2,4),pech->pech!=this).isEmpty()){
                level().broadcastEntityEvent(this,(byte)17);playSound(sound("pech_trade"),getSoundVolume(),getVoicePitch());return;
            }
            level().broadcastEntityEvent(this,(byte)16);
        }
        super.playAmbientSound();
    }
    @Override public int getAmbientSoundInterval(){return 120;}
    @Override protected float getSoundVolume(){return .4F;}
    @Override protected SoundEvent getAmbientSound(){return sound("pech_idle");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return sound("pech_hit");}
    @Override protected SoundEvent getDeathSound(){return sound("pech_death");}
    public static SoundEvent sound(String path){return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",path));}
    @Override public void handleEntityEvent(byte event){
        if(event==16)mumble=(float)Math.PI;else if(event==17)mumble=(float)Math.PI*2;else if(event==18)for(int i=0;i<5;i++)particle(true);
        if(event==19){for(int i=0;i<5;i++)particle(false);mumble=(float)Math.PI*2;}else super.handleEntityEvent(event);
    }
    private void particle(boolean happy){
        double vx=random.nextGaussian()*.02,vy=random.nextGaussian()*.02,vz=random.nextGaussian()*.02;
        level().addParticle(happy?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.ANGRY_VILLAGER,getX()+(double)(random.nextFloat()*getBbWidth()*2)-getBbWidth(),getY()+.5+(double)(random.nextFloat()*getBbHeight()),getZ()+(double)(random.nextFloat()*getBbWidth()*2)-getBbWidth(),vx,vy,vz);
    }
    @Override public void performRangedAttack(LivingEntity target,float ratio){
        if(level().isClientSide)return;
        if(getPechType()==2){
            var arrow=new PechArrow(level(),this,target,14-level().getDifficulty().getId()*4);
            var enchantments=level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int power=EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.POWER),getMainHandItem());
            int punch=EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.PUNCH),getMainHandItem());
            double damage=ratio*2+random.nextGaussian()*.25+(double)(level().getDifficulty().getId()*.11F);if(power>0)damage+=power*.5+.5;
            arrow.setBaseDamage(damage);arrow.punch=punch;playSound(SoundEvents.ARROW_SHOOT,1,1/(random.nextFloat()*.4F+.8F));level().addFreshEntity(arrow);
        }else if(getPechType()==1){
            var blast=new EntityPechBlast(level(),this,1,0,random.nextFloat()<.1F);
            double x=target.getX()+target.getDeltaMovement().x-getX(),y=target.getY()+target.getEyeHeight()-1.500000023841858-getY(),z=target.getZ()+target.getDeltaMovement().z-getZ();
            float horizontal=(float)Math.sqrt(x*x+z*z);blast.shoot(x,y+horizontal*.1F,z,1.5F,4);
            playSound(sound("ice"),.4F,1+random.nextFloat()*.1F);level().addFreshEntity(blast);
        }
        swing(InteractionHand.MAIN_HAND);
    }
    /** Ordinary native Arrow packets/terrain/combat, with original 1.7 launch
     * constructor and Gaussian+sign noise. No modern bow weapon attachment:
     * original Pech explicitly reads POWER/PUNCH and ignores FLAME. */
    private static final class PechArrow extends Arrow {
        private int punch;
        PechArrow(Level level,EntityPech owner,LivingEntity target,float scatter){
            super(level,owner,new ItemStack(Items.ARROW),null);setPos(owner.getX(),owner.getEyeY()-(double).1F,owner.getZ());
            double x=target.getX()-owner.getX(),y=target.getBoundingBox().minY+(double)(target.getBbHeight()/3)-getY(),z=target.getZ()-owner.getZ();float horizontal=(float)Math.sqrt(x*x+z*z);
            if(horizontal<1e-7)return;setPos(owner.getX()+x/horizontal,getY(),owner.getZ()+z/horizontal);shoot(x,y+(double)(horizontal*.2F),z,1.6F,scatter);
        }
        @Override public Vec3 getMovementToShoot(double x,double y,double z,float speed,float scatter){
            float length=(float)Math.sqrt(x*x+y*y+z*z);if(!Float.isFinite(length)||length<=1e-12F)return Vec3.ZERO;
            return new Vec3((x/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075F*scatter)*speed,
                    (y/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075F*scatter)*speed,
                    (z/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075F*scatter)*speed);
        }
        @Override protected void doKnockback(LivingEntity target,DamageSource source){
            if(punch<=0)return;Vec3 v=getDeltaMovement();float horizontal=(float)Math.sqrt(v.x*v.x+v.z*v.z);
            if(horizontal>0)target.push(v.x*punch*(double).6F/horizontal,.1,v.z*punch*(double).6F/horizontal);
        }
    }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height){return OriginalMobLoot.spawn(this,stack,height);}
    @Override protected void dropFromLootTable(DamageSource source,boolean recentlyHit){}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){
        int looting=OriginalMobLoot.looting(level,source);
        for(ItemStack stack:loot)if(!stack.isEmpty()&&level.random.nextFloat()<.88F)spawnAtLocation(stack.copy(),1.5F);
        var primals=Aspect.getPrimalAspects();for(int i=0;i<1+looting;i++)if(random.nextBoolean())spawnAtLocation(ItemManaBean.create(primals.get(random.nextInt(primals.size()))),1.5F);
        if(level.random.nextInt(10)<1+looting)spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get("gold_coin").get()),1.5F);
        super.dropCustomDeathLoot(level,source,recentlyHit);
        if(recentlyHit&&OriginalMobLoot.rare(looting,random::nextInt))spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()),1.5F);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putByte("PechType",(byte)getPechType());tag.putShort("Anger",(short)getAnger());tag.putBoolean("Tamed",isTamed());
        var pack=new ListTag();for(ItemStack stack:loot)pack.add(stack.isEmpty()?new CompoundTag():stack.save(registryAccess()));tag.put("Loot",pack);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);if(tag.contains("PechType"))setPechType(tag.getByte("PechType"));setAnger(tag.getShort("Anger"));setTamed(tag.getBoolean("Tamed"));
        var pack=tag.getList("Loot",10);for(int i=0;i<loot.length;i++)loot[i]=i<pack.size()?ItemStack.parseOptional(registryAccess(),pack.getCompound(i)):ItemStack.EMPTY;
        legacyTarget=null;customer=null;trading=false;updateAINextTick=true;
    }
}
