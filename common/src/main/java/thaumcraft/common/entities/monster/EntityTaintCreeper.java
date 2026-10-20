package thaumcraft.common.entities.monster;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintCreeper;
import thaumcraft.common.entities.ai.combat.TaintCreeperSwell;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.world.BiomeColumns;

/**
 * Original TC4 {@code thaumcraft/common/entities/monster/EntityTaintCreeper}.
 *
 * <p>The tainted creeper of the original: thirty health, two damage, a quarter of the movement speed and its own
 * swell state. It never destroys blocks: its explosion only spreads the taint, poisons the living entities around
 * it with the taint poison and turns the ground into the taint biome.</p>
 *
 * <p>Oracle: {@code EntityTaintCreeper.java}. The values are copied from the original:
 * <ul>
 *   <li>{@code func_110147_ax}: max health 30, attack damage 2, movement speed 0.25.</li>
 *   <li>{@code func_70069_a(float)}: the fall distance grows the fuse by 1.5 per block, never past
 *       {@code fuseTime - 5}.</li>
 *   <li>{@code func_70071_h_}: the swell state adds one tick to the fuse every tick, the fuse ends at 30.</li>
 *   <li>{@code func_70628_a(boolean, int)}: a coin flip between the tainted goo and the taint tendril.</li>
 * </ul>
 *
 * <p>Forced 1.21.1 change: 1.7.10 called {@code Entity.fall(float, float)} when the mob landed; 1.21.1 has no such
 * hook any more, so the landing is watched in {@link #move(MoverType, Vec3)} and the original growth is kept.</p>
 */
public class EntityTaintCreeper extends Monster implements ITaintedMob {
    /** The client side particles of the original taint explosion and of the spawn splash. */
    @FunctionalInterface public interface ClientEffects { void effect(EntityTaintCreeper entity, boolean explosion); }
    private static ClientEffects clientEffects;

    private static final EntityDataAccessor<Integer> CREEPER_STATE = SynchedEntityData.defineId(EntityTaintCreeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> POWERED = SynchedEntityData.defineId(EntityTaintCreeper.class, EntityDataSerializers.BOOLEAN);

    /** Original {@code lastActiveTime}: the fuse value the swell started at. */
    private int lastActiveTime;
    /** Original {@code timeSinceIgnited}: the fuse of the original. */
    private int timeSinceIgnited;
    /** Original {@code fuseTime}: thirty ticks. */
    private int fuseTime = 30;
    /** Original {@code explosionRadius}: three blocks. */
    private int explosionRadius = 3;

    public EntityTaintCreeper(EntityType<? extends EntityTaintCreeper> type, Level level) {
        super(type, level);
    }

    public EntityTaintCreeper(Level level) {
        this(ThaumcraftTaintCreeper.TAINT_CREEPER.get(), level);
    }

    /** The client bootstrap installs the renderer of the original particles. */
    public static void setClientEffects(ClientEffects effects) {
        clientEffects = Objects.requireNonNull(effects);
    }

    /** Original {@code func_110147_ax}: thirty health, two damage, a quarter of the movement speed. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CREEPER_STATE, -1);
        builder.define(POWERED, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(2, new TaintCreeperSwell(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0D, 1.2D));
        goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    /** Original {@code func_70692_ba}: the taint creeper never despawns. */
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    /** Original {@code func_70652_k}: the taint creeper always answers a melee attack. */
    @Override public boolean doHurtTarget(Entity target) { return true; }

    /**
     * Original {@code func_82143_as}: three blocks without a target, three plus the max health minus one with one.
     *
     * <p>The original overrode the inherited method with the very expression of {@code EntityLiving}, so the native
     * port keeps the same numbers on the native hook.</p>
     */
    @Override public int getMaxFallDistance() {
        return getTarget() == null ? 3 : 3 + (int) (getMaxHealth() - 1.0F);
    }

    /**
     * Original {@code func_70069_a(float)}: the fall distance grows the fuse.
     *
     * <p>1.7.10 called {@code Entity.fall(float, float)} when the mob landed; 1.21.1 has no such hook, so the
     * landing is watched here and the original growth of 1.5 per block, capped at {@code fuseTime - 5}, is kept.</p>
     */
    @Override public void move(MoverType type, Vec3 movement) {
        float distance = fallDistance;
        boolean grounded = onGround();
        super.move(type, movement);
        if (grounded || !onGround() || distance <= 0.0F) return;
        timeSinceIgnited = (int) (timeSinceIgnited + distance * 1.5F);
        if (timeSinceIgnited > fuseTime - 5) timeSinceIgnited = fuseTime - 5;
    }

    /** Original {@code getCreeperState}: minus one while the creeper does not swell, one while it does. */
    public int getCreeperState() { return entityData.get(CREEPER_STATE); }

    /** Original {@code setCreeperState}. */
    public void setCreeperState(int state) { entityData.set(CREEPER_STATE, state); }

    /** Original {@code isPowered}: the charged creeper state of the vanilla creeper. */
    public boolean isPowered() { return entityData.get(POWERED); }

    /** Original {@code getCreeperFlashIntensity}: the white flash of the swelling creeper. */
    public float getCreeperFlashIntensity(float partialTicks) {
        return ((float) lastActiveTime + (float) (timeSinceIgnited - lastActiveTime) * partialTicks) / 28.0F;
    }

    /** Original {@code func_70071_h_}: the swell state, the fuse and the taint explosion. */
    @Override public void tick() {
        if (isAlive()) {
            int state = getCreeperState();
            if (state > 0) {
                lastActiveTime = timeSinceIgnited;
                if (timeSinceIgnited == 0) playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
                timeSinceIgnited += state;
                if (timeSinceIgnited < 0) timeSinceIgnited = 0;
                if (timeSinceIgnited >= fuseTime) {
                    timeSinceIgnited = fuseTime;
                    if (!level().isClientSide) {
                        taintExplosion();
                        discard();
                    } else if (clientEffects != null) {
                        clientEffects.effect(this, true);
                    }
                }
            }
        }
        super.tick();
        // Original func_70636_d: the spawn splash of the first five ticks.
        if (level().isClientSide && tickCount < 5 && clientEffects != null) clientEffects.effect(this, false);
    }

    /**
     * Original {@code func_70071_h_} of the server branch: the explosion, the taint poison and the taint spread.
     *
     * <p>The original created an explosion of the strength 1.5 that never destroys blocks, poisoned every living
     * entity within six blocks that was not a tainted mob and not immune to fire, and turned up to ten random
     * spots of the neighbourhood into the taint biome with taint fibres on top.</p>
     */
    private void taintExplosion() {
        if (!(level() instanceof ServerLevel server)) return;
        server.explode(this, getX(), getY() + getBbHeight() / 2.0F, getZ(), 1.5F, false, ExplosionInteraction.NONE);
        AABB box = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(6.0D, 6.0D, 6.0D);
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, box)) {
            if (living instanceof ITaintedMob || living.fireImmune()) continue;
            living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()), 100, 0, false, false));
        }
        spreadTaint(server);
    }

    /** Original taint spread of the taint creeper: ten spots of the taint biome with fibres on top. */
    private void spreadTaint(ServerLevel server) {
        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());
        for (int attempt = 0; attempt < 10; attempt++) {
            int xx = x + (int) ((random.nextFloat() - random.nextFloat()) * 5.0F);
            int zz = z + (int) ((random.nextFloat() - random.nextFloat()) * 5.0F);
            // The original drew the coin from the world random and the offsets from the own random.
            if (!server.getRandom().nextBoolean()) continue;
            var biome = BiomeColumns.surfaceHolder(server, xx, zz);
            if (biome != null && biome.is(ThaumcraftBiomes.TAINT)) continue;
            BiomeColumns.set(server, xx, zz, ThaumcraftBiomes.TAINT);
            BlockPos below = new BlockPos(xx, y - 1, zz);
            BlockPos at = new BlockPos(xx, y, zz);
            if (!server.getBlockState(below).isFaceSturdy(server, below, Direction.UP)) continue;
            if (!server.getBlockState(at).canBeReplaced()) continue;
            server.setBlock(at, ThaumcraftTaint.FIBRES.get().defaultBlockState(), 3);
        }
    }

    /** Original {@code func_70014_b} and {@code func_70037_a}: the fuse, the explosion radius and the charged state. */
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("powered", isPowered());
        tag.putShort("Fuse", (short) fuseTime);
        tag.putByte("ExplosionRadius", (byte) explosionRadius);
        tag.putBoolean("Powered", isPowered());
        tag.putByte("CreeperState", (byte) getCreeperState());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fuse")) fuseTime = tag.getShort("Fuse");
        if (tag.contains("ExplosionRadius")) explosionRadius = tag.getByte("ExplosionRadius");
        entityData.set(POWERED, tag.getBoolean("powered") || tag.getBoolean("Powered"));
        timeSinceIgnited = 0;
        lastActiveTime = 0;
        setCreeperState(tag.getByte("CreeperState"));
    }

    /** Original {@code func_70628_a(boolean, int)}: a coin flip between the tainted goo and the taint tendril. */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(level.random.nextBoolean()
                ? ThaumcraftContent.MATERIALS.get("tainted_goo").get()
                : ThaumcraftContent.MATERIALS.get("taint_tendril").get()), getBbHeight() / 2.0F);
    }

    @Override public ItemEntity spawnAtLocation(ItemStack stack, float height) { return OriginalMobLoot.spawn(this, stack, height); }

    /** Original {@code func_70621_aR} and {@code func_70673_aS}: the vanilla creeper sounds. */
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CREEPER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CREEPER_DEATH; }
    /** Original {@code func_70647_i}: the sound pitch of the creeper. */
    @Override public float getVoicePitch() { return 0.7F; }
}
