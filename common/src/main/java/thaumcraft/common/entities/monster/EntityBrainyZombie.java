package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.entities.OriginalLightRules;
import thaumcraft.common.lib.loot.OriginalMobLoot;

/** TC4's Angry Zombie (original class name BrainyZombie). Native ZombieVillager retains the old single zombie class's curable flag. */
public class EntityBrainyZombie extends ZombieVillager {
    private static final EntityDataAccessor<Boolean> VILLAGER = SynchedEntityData.defineId(EntityBrainyZombie.class, EntityDataSerializers.BOOLEAN);
    public EntityBrainyZombie(EntityType<? extends EntityBrainyZombie> type, Level level) {
        super(type, level);
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 25).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(VILLAGER, false); }
    public boolean isVillagerVariant() { return entityData.get(VILLAGER); }
    public void setVillagerVariant(boolean value) { entityData.set(VILLAGER, value); }
    @Override public int getArmorValue() { return Math.min(20, super.getArmorValue() + 3); }
    @Override protected void randomizeReinforcementsChance() { getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0); }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        var dimensions = EntityDimensions.scalable(.6F, 1.8F).withEyeHeight(1.53F);
        return isBaby() ? dimensions.scale(.5F) : dimensions;
    }
    /** 1.7 hostile light rule: the saved sky light against a nextInt(32) roll, then the block
     * light against a nextInt(8) roll. */
    public static boolean checkBrainySpawnRules(EntityType<? extends EntityBrainyZombie> type, ServerLevelAccessor level,
                                                MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (!OriginalLightRules.validMobLightLevel(level, pos, random)) return false;
        return Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }
    private record Pack(SpawnGroupData nativePack, boolean villager) implements SpawnGroupData {}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData pack) {
        Pack previous = pack instanceof Pack group ? group : null;
        SpawnGroupData nativePack = super.finalizeSpawn(level, difficulty, reason, previous == null ? pack : previous.nativePack());
        boolean villager = previous == null ? level.getRandom().nextFloat() < .05F : previous.villager();
        setVillagerVariant(villager);
        return new Pack(nativePack, villager);
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return isVillagerVariant() ? super.mobInteract(player, hand) : InteractionResult.PASS;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("IsVillager", isVillagerVariant()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setVillagerVariant(tag.getBoolean("IsVillager")); }
    @Override public SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override public SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
    @Override public SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
    @Override public SoundEvent getStepSound() { return SoundEvents.ZOMBIE_STEP; }
    @Override public float getVoicePitch() { return (isBaby() ? 1.5F : 1F) + (random.nextFloat() - random.nextFloat()) * .2F; }
    @Override protected ItemStack getSkull() { return ItemStack.EMPTY; } // Charged-creeper skull loot did not exist in 1.7.
    @Override public ItemEntity spawnAtLocation(ItemStack stack, float height) { return OriginalMobLoot.spawn(this, stack, height); }
    @Override protected void dropFromLootTable(DamageSource source, boolean recentlyHit) {}
    protected int fleshAttempts() { return 3; }
    protected int fleshPerDrop() { return 1; }
    protected float brainDropHeight() { return 1.5F; }
    protected void dropRareItem() {
        spawnAtLocation(switch (random.nextInt(3)) { case 0 -> Items.IRON_INGOT; case 1 -> Items.CARROT; default -> Items.POTATO; });
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int looting = OriginalMobLoot.looting(level, source);
        for (int attempt = 0; attempt < fleshAttempts(); attempt++) if (level.random.nextBoolean()) spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, fleshPerDrop()));
        if (OriginalMobLoot.brain(looting, level.random::nextInt)) spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get("zombie_brain").get()), brainDropHeight());
        // Native equipment first, as in original EntityLivingBase.onDeath; empty getSkull excludes the newer charged-creeper path.
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (recentlyHit && OriginalMobLoot.rare(looting, random::nextInt)) dropRareItem();
    }
}
