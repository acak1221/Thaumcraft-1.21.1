package thaumcraft.common.entities.monster;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.cult.CultistEquipmentRules;
import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.loot.OriginalLootEnchantments;
import thaumcraft.common.lib.ward.WardRules;

/**
 * Original TC4 cultist praetor: a hundred and twenty-five health, a crimson praetor
 * armour, a homing energy orb and one of the thirteen champion modifiers.
 *
 * <p>The shared TC4 boss base (EntityThaumcraftBoss) is folded into this file because the
 * common base class is not part of the native tree yet: the aggro table, the enrage, the
 * player scaled health and damage buffs and the home anchor keep their original values.
 */
public class EntityCultistLeader extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> TITLE = SynchedEntityData.defineId(EntityCultistLeader.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHAMPION = SynchedEntityData.defineId(EntityCultistLeader.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(EntityCultistLeader.class, EntityDataSerializers.INT);
    private static final AttributeModifier[] HP_BUFFS = {
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_health_1"), CultistRules.BOSS_HEALTH_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_health_2"), CultistRules.BOSS_HEALTH_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_health_3"), CultistRules.BOSS_HEALTH_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_health_4"), CultistRules.BOSS_HEALTH_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_health_5"), CultistRules.BOSS_HEALTH_BUFF, AttributeModifier.Operation.ADD_VALUE)
    };
    private static final AttributeModifier[] DMG_BUFFS = {
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_damage_1"), CultistRules.BOSS_DAMAGE_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_damage_2"), CultistRules.BOSS_DAMAGE_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_damage_3"), CultistRules.BOSS_DAMAGE_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_damage_4"), CultistRules.BOSS_DAMAGE_BUFF, AttributeModifier.Operation.ADD_VALUE),
            new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "boss_damage_5"), CultistRules.BOSS_DAMAGE_BUFF, AttributeModifier.Operation.ADD_VALUE)
    };
    private static final AttributeModifier BOLD_SPEED = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "champion_bold_speed"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier MIGHTY_DAMAGE = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "champion_mighty_damage"), 3.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private final Map<Integer, Integer> aggro = new HashMap<>();
    private int spawnTimer;

    public EntityCultistLeader(EntityType<? extends EntityCultistLeader> type, Level level) {
        super(type, level);
        xpReward = CultistRules.LEADER_EXPERIENCE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CultistRules.LEADER_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CultistRules.LEADER_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, CultistRules.LEADER_SPEED)
                .add(Attributes.FOLLOW_RANGE, CultistRules.BOSS_FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, CultistRules.BOSS_KNOCKBACK_RESISTANCE);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new EntityCultist.CultistSwimGoal(this));
        goalSelector.addGoal(2, new EntityCultist.CultistLongRangeGoal(this, 16.0, 1.0, 30, 40, 24.0F));
        goalSelector.addGoal(3, new EntityCultist.CultistMeleeGoal(this, 1.1, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new EntityCultist.CultistHurtByTargetGoal(this, true));
        // Original EntityAINearestAttackableTarget:52-58 - a target chance of zero scans
        // every tick. The native goal scans every fifth tick unless the interval is one.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 1, true, false, null));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TITLE, 0);
        builder.define(CHAMPION, -1);
        builder.define(ANGER, 0);
    }

    /** Original EntityCultistLeader:120-130 - the title of the praetor is kept in its saved data. */
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("title", (byte) (int) entityData.get(TITLE));
        // Original EntityThaumcraftBoss:66-74 - the boss base keeps its home area.
        if (hasRestriction()) {
            tag.putInt("HomeD", (int) getRestrictRadius());
            tag.putInt("HomeX", getRestrictCenter().getX());
            tag.putInt("HomeY", getRestrictCenter().getY());
            tag.putInt("HomeZ", getRestrictCenter().getZ());
        }
    }

    /** Original EntityCultistLeader:127-130 - the praetor reads its title back. */
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTitle(tag.getByte("title"));
        // Original EntityThaumcraftBoss:59-64 - the boss base reads its home area back.
        if (tag.contains("HomeD")) {
            restrictTo(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), tag.getInt("HomeD"));
        }
    }

    // ---------------------------------------------------------------- identity
    public String getTitle() { return CultistRules.leaderTitle(entityData.get(TITLE)); }

    public void setTitle(int title) { entityData.set(TITLE, Math.max(0, Math.min(title, CultistRules.LEADER_TITLES.length - 1))); }

    public int getChampionModifier() { return entityData.get(CHAMPION); }

    public void setChampionModifier(int modifier) {
        entityData.set(CHAMPION, modifier < 0 || modifier >= CultistRules.CHAMPION_COUNT ? -1 : modifier);
    }

    public int getAnger() { return entityData.get(ANGER); }

    public void setAnger(int anger) { entityData.set(ANGER, anger); }

    public int getSpawnTimer() { return spawnTimer; }

    @Override public Component getName() {
        if (hasCustomName()) return getCustomName();
        return Component.translatable("entity.thaumcraft.cultistleader.name", getTitle(), championName());
    }

    /** Original ChampionModifier.getModNameLocalized with the original display name as the fallback. */
    public Component championName() {
        int modifier = getChampionModifier();
        if (modifier < 0) return Component.literal(CultistRules.CHAMPION_MOD_DISPLAY_NAMES[CultistRules.CHAMPION_BOLD]);
        return Component.translatable("champion.mod." + CultistRules.CHAMPION_MOD_NAMES[modifier]);
    }

    // ---------------------------------------------------------------- spawn
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        // Original EntityThaumcraftBoss.onSpawnWithEgg anchors the boss to its spawn point.
        restrictTo(blockPosition(), CultistRules.BOSS_HOME_RADIUS);
        applyChampionIdentity();
        return result;
    }

    /** Original EntityCultistLeader.onSpawnWithEgg: the title and the champion modifier of a fresh praetor. */
    public void originalSpawnEquipment(ServerLevelAccessor level, DifficultyInstance difficulty) {
        populateDefaultEquipmentSlots(random, difficulty);
        populateDefaultEquipmentEnchantments(level, random, difficulty);
        applyChampionIdentity();
    }

    /** Original EntityUtils.makeChampion gives every boss a random champion modifier. */
    private void applyChampionIdentity() {
        setTitle(random.nextInt(CultistRules.LEADER_TITLES.length));
        setChampionModifier(random.nextInt(CultistRules.CHAMPION_COUNT));
        applyChampionStats();
        setCustomName(Component.translatable("entity.thaumcraft.cultistleader.name", getTitle(), championName()));
    }

    /** Original EntityCultistLeader.addRandomArmor: the crimson praetor armour and the blade. */
    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        CultistEquipmentRules.Loadout loadout = CultistEquipmentRules.praetor(level().getDifficulty() == Difficulty.EASY);
        setItemSlot(EquipmentSlot.HEAD, EntityCultist.cultistPiece(loadout.piece(CultistEquipmentRules.Slot.HELM)));
        setItemSlot(EquipmentSlot.CHEST, EntityCultist.cultistPiece(loadout.piece(CultistEquipmentRules.Slot.CHEST)));
        setItemSlot(EquipmentSlot.LEGS, EntityCultist.cultistPiece(loadout.piece(CultistEquipmentRules.Slot.LEGS)));
        setItemSlot(EquipmentSlot.FEET, EntityCultist.cultistPiece(loadout.piece(CultistEquipmentRules.Slot.FEET)));
        ItemStack blade = EntityCultist.cultistWeapon(loadout.weapon());
        if (!blade.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, blade);
    }

    /** Original EntityCultistLeader.enchantEquipment: the held blade with the original chance and power. */
    @Override protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        enchantHeldBlade(level);
    }

    public void enchantHeldBlade(ServerLevelAccessor level) {
        ItemStack held = getMainHandItem();
        if (held.isEmpty()) return;
        float tension = level.getCurrentDifficultyAt(blockPosition()).getSpecialMultiplier();
        if (random.nextFloat() < CultistRules.leaderEnchantChance(tension)) {
            int power = CultistRules.leaderEnchantPower(tension, random.nextInt(22));
            OriginalLootEnchantments.enchant(level.getLevel(), held, random, power);
        }
    }

    /** Original EntityUtils.makeChampion: bold, mighty and warded change the praetor's own numbers. */
    public void applyChampionStats() {
        int modifier = getChampionModifier();
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (speed != null) speed.removeModifier(BOLD_SPEED);
        if (damage != null) damage.removeModifier(MIGHTY_DAMAGE);
        if (modifier == CultistRules.CHAMPION_BOLD && speed != null) speed.addPermanentModifier(BOLD_SPEED);
        if (modifier == CultistRules.CHAMPION_MIGHTY && damage != null) damage.addPermanentModifier(MIGHTY_DAMAGE);
        if (modifier == CultistRules.CHAMPION_WARDED) {
            // Oracle EntityUtils.makeChampion:271-274 - the warded champion takes half of its maximum
            // health as its ward, and the ward joins its absorption. 1.21.1 clamps the absorption by the
            // max absorption attribute, so the ward raises that attribute first.
            WardRules.setWard(this, CultistRules.championAbsorption((float) getAttributeValue(Attributes.MAX_HEALTH)));
        }
    }

    // ---------------------------------------------------------------- boss base behaviour
    /** Original EntityThaumcraftBoss.isInvulnerable while the spawn timer runs. */
    @Override public boolean isInvulnerable() { return super.isInvulnerable() || getSpawnTimer() > 0; }

    /** Original EntityThaumcraftBoss does not suffocate. */
    @Override public int decreaseAirSupply(int air) { return air; }

    /** Original EntityThaumcraftBoss.canBePushed is false while the boss is invulnerable. */
    @Override public boolean isPushable() { return super.isPushable() && !isInvulnerable(); }

    /** Original EntityThaumcraftBoss.canDespawn is false. */
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    /**
     * Original EntityCultistLeader.dropFewItems hands out a rare loot bag. The praetor
     * extends the boss base and not the cultist, so it never rolls the cultist resource
     * and its rare drop stays empty as in the original.
     */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ThaumcraftLoot.LOOT_BAG_RARE.get()), 1.5F);
    }

    /** Original EntityCultistLeader.getDropItem is air, so the praetor drops no plain mob item. */
    @Override protected void dropFromLootTable(DamageSource source, boolean recentlyHit) {}

    /** Original 1.7 item motion used Math.random, not the world or entity random. */
    @Override public net.minecraft.world.entity.item.ItemEntity spawnAtLocation(ItemStack stack, float height) {
        return thaumcraft.common.lib.loot.OriginalMobLoot.spawn(this, stack, height);
    }

    @Override public void tick() {
        super.tick();
        if (spawnTimer > 0) spawnTimer--;
        if (getAnger() > 0) setAnger(getAnger() - 1);
        if (level().isClientSide) {
            if (random.nextInt(15) == 0 && getAnger() > 0) {
                level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                        getX() + (double) (random.nextFloat() * getBbWidth() * 2.0F) - getBbWidth() / 2.0,
                        getY() + getBbHeight() + random.nextFloat() * 0.5,
                        getZ() + (double) (random.nextFloat() * getBbWidth() * 2.0F) - getBbWidth() / 2.0,
                        random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
            }
            return;
        }
        // Original EntityThaumcraftBoss heals one point every thirty ticks.
        if (tickCount % 30 == 0) heal(1.0F);
        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) setTarget(null);
        if (target != null && tickCount % CultistRules.BOSS_TARGET_SWITCH_INTERVAL == 0) rotateTarget(target);
        // Original EntityCultistLeader.updateAITasks feeds the nearby cultists.
        for (EntityCultist cultist : level().getEntitiesOfClass(EntityCultist.class,
                getBoundingBox().inflate(CultistRules.LEADER_AURA_RANGE))) {
            if (cultist.hasEffect(MobEffects.REGENERATION)) continue;
            cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION, CultistRules.LEADER_AURA_DURATION,
                    CultistRules.LEADER_AURA_AMPLIFIER));
        }
    }

    /** Original EntityThaumcraftBoss target rotation with the player scaled health and damage buffs. */
    private void rotateTarget(LivingEntity current) {
        int currentId = current.getId();
        CultistRules.AggroRotation rotation = CultistRules.rotateAggro(aggro, currentId, this::probeTarget);
        for (int removed : rotation.removed()) aggro.remove(removed);
        if (rotation.targetId() >= 0 && rotation.finalId() != currentId) {
            Entity entity = level().getEntity(rotation.targetId());
            if (entity instanceof LivingEntity living) setTarget(living);
        }
        applyPlayerBuffs(rotation.players());
    }

    /** Original EntityThaumcraftBoss probe: a missing, dead or distant entity is dropped. */
    private int probeTarget(int entityId) {
        Entity entity = level().getEntity(entityId);
        if (entity == null || !entity.isAlive()) return 0;
        if (entity.distanceToSqr(this) > CultistRules.BOSS_MAX_TARGET_DISTANCE_SQ) return 0;
        return entity instanceof Player ? 2 : 1;
    }

    /** Original EntityThaumcraftBoss: every extra player grants fifty health and half a point of damage. */
    private void applyPlayerBuffs(int players) {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (health == null || damage == null) return;
        double maxHealth = getAttributeValue(Attributes.MAX_HEALTH);
        for (int i = 0; i < CultistRules.BOSS_BUFF_STEPS; i++) {
            damage.removeModifier(DMG_BUFFS[i]);
            health.removeModifier(HP_BUFFS[i]);
        }
        int count = Math.min(CultistRules.BOSS_BUFF_STEPS, players - 1);
        for (int i = 0; i < count; i++) {
            health.addPermanentModifier(HP_BUFFS[i]);
            damage.addPermanentModifier(DMG_BUFFS[i]);
        }
        double ratio = getAttributeValue(Attributes.MAX_HEALTH) / maxHealth;
        setHealth((float) (getHealth() * ratio));
    }

    /** Original EntityThaumcraftBoss: a heavy hit enrages the boss and caps the damage of that hit. */
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source.getEntity() instanceof LivingEntity attacker) {
                int id = attacker.getId();
                aggro.put(id, CultistRules.bossAggroDamage((int) amount, aggro.getOrDefault(id, 0)));
            }
            if (amount > CultistRules.BOSS_ENRAGE_THRESHOLD) {
                if (getAnger() == 0) {
                    addEffect(new MobEffectInstance(MobEffects.REGENERATION, CultistRules.BOSS_ENRAGE_DURATION,
                            CultistRules.bossEnrageRegenerationAmplifier(amount)));
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, CultistRules.BOSS_ENRAGE_DURATION,
                            CultistRules.bossEnrageBuffAmplifier(amount)));
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, CultistRules.BOSS_ENRAGE_DURATION,
                            CultistRules.bossEnrageBuffAmplifier(amount)));
                    setAnger(CultistRules.BOSS_ENRAGE_ANGER);
                    if (source.getEntity() instanceof ServerPlayer player) {
                        player.sendSystemMessage(Component.literal(getName().getString() + " "
                                + Component.translatable("tc.boss.enrage")));
                    }
                }
                amount = CultistRules.bossDamageCap(amount);
            }
        }
        return super.hurt(source, amount);
    }

    // ---------------------------------------------------------------- combat
    /** Original EntityCultistLeader.attackEntityWithRangedAttack: the homing energy orb. */
    @Override public void performRangedAttack(LivingEntity target, float distance) {
        if (!hasLineOfSight(target)) return;
        swing(InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0, target.getZ(),
                30.0F, 30.0F);
        double dx = target.getX() - getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 2.0 - (getY() + getBbHeight() / 2.0);
        double dz = target.getZ() - getZ();
        EntityGolemOrb blast = new EntityGolemOrb(level(), this, target, true);
        EntityCultist.originalShoot(blast, dx, dy + 2.0, dz, 0.66F, 3.0F);
        playSound(EntityCultist.sound("egattack"), 1.0F, 1.0F + random.nextFloat() * 0.1F);
        level().addFreshEntity(blast);
    }

    @Override public boolean doHurtTarget(Entity victim) {
        boolean hit = super.doHurtTarget(victim);
        if (hit && victim instanceof LivingEntity living) applyChampionHit(living);
        return hit;
    }

    /** Original ChampionModWarp: the warped praetor warps a third of the players it strikes. */
    public void applyChampionHit(LivingEntity target) {
        if (getChampionModifier() != CultistRules.CHAMPION_WARP) return;
        if (!(target instanceof ServerPlayer player)) return;
        if (random.nextFloat() >= CultistRules.WARP_REWARD_CHANCE) return;
        WarpEvents.addTemporaryWarp(player, CultistRules.warpReward(random.nextInt(3)));
    }

    /** Original EntityCultistLeader.isOnSameTeam: the praetor never attacks its own faction. */
    @Override public boolean isAlliedTo(Entity other) {
        return other instanceof EntityCultist || other instanceof EntityCultistLeader || super.isAlliedTo(other);
    }

    /** Original EntityCultistLeader.canAttackClass excludes the three cultist kinds. */
    public boolean canAttackClass(Class<? extends Entity> type) {
        return type != EntityCultistCleric.class && type != EntityCultistLeader.class && type != EntityCultistKnight.class;
    }
}
