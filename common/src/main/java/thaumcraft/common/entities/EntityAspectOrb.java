package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftAspectOrbs;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.utils.AspectOrbQueue;
import thaumcraft.common.lib.research.AspectOrbRules;

/** Original EntityAspectOrb:40-244. It stores wand vis, never research points or XP. */
public final class EntityAspectOrb extends Entity {
    public static final int MAX_AGE = AspectOrbRules.MAX_AGE;
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(EntityAspectOrb.class, EntityDataSerializers.STRING);
    private int age, cooldown, health = 5, value, issuedValue;
    private Player closestPlayer;
    private boolean ticking, claiming, consumed;
    private CompoundTag rejected;
    public EntityAspectOrb(EntityType<? extends EntityAspectOrb> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data) { data.define(ASPECT, ""); }
    public Aspect getAspect() { return Aspect.getAspect(entityData.get(ASPECT)); }
    public int getAspectValue() { return value; }
    public int getOrbAge() { return age; }
    public int getOrbCooldown() { return cooldown; }
    public boolean isQuarantined() { return rejected != null; }
    public boolean wasIssuedAs(Aspect aspect, int amount) { return getAspect() == aspect && issuedValue == amount; }
    public boolean isConsumed() { return consumed; }

    public static EntityAspectOrb unspawned(ServerLevel level, Vec3 position, Vec3 motion, float yaw, Aspect aspect, int amount) {
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level || aspect == null || !aspect.isPrimal()
                || Aspect.getAspect(aspect.getTag()) != aspect || amount <= 0 || amount > Short.MAX_VALUE
                || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || !Double.isFinite(motion.x) || !Double.isFinite(motion.y) || !Double.isFinite(motion.z) || !Float.isFinite(yaw)) throw new IllegalArgumentException("Invalid native aspect orb");
        var orb = ThaumcraftAspectOrbs.ORB.get().create(level);
        if (orb == null) throw new IllegalStateException("Missing aspect orb entity");
        orb.entityData.set(ASPECT, aspect.getTag()); orb.value = orb.issuedValue = amount;
        orb.setPos(position); orb.setDeltaMovement(motion); orb.setYRot(yaw); return orb;
    }
    private boolean current() {
        return level() instanceof ServerLevel server && server.getServer().isSameThread() && server.getServer().getLevel(server.dimension()) == server
                && !isRemoved() && server.hasChunkAt(blockPosition()) && server.getEntity(getUUID()) == this;
    }
    private boolean validValue() { var aspect = getAspect(); return rejected == null && !consumed && value > 0 && value <= Short.MAX_VALUE && aspect != null && aspect.isPrimal(); }
    @Override public void setLevelCallback(EntityInLevelCallback callback) {
        super.setLevelCallback(callback);
        if (callback != EntityInLevelCallback.NULL && !level().isClientSide) AspectOrbQueue.admitted(this);
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 1024D * getViewScale() * getViewScale(); }
    @Override public boolean isPushable() { return false; }

    @Override public void tick() {
        if (ticking || claiming || !level().isClientSide && !current()) return;
        ticking = true;
        try {
            super.tick();
            if (consumed) { if (!level().isClientSide) discard(); return; }
            if (rejected != null || isRemoved()) return;
            if (cooldown > 0) --cooldown;
            setDeltaMovement(getDeltaMovement().add(0, -(double)AspectOrbRules.GRAVITY, 0));
            if (level().getBlockState(blockPosition()).getFluidState().is(FluidTags.LAVA)) {
                setDeltaMovement((random.nextFloat() - random.nextFloat()) * .2F, .2F, (random.nextFloat() - random.nextFloat()) * .2F);
                playSound(SoundEvents.LAVA_EXTINGUISH, .4F, 2F + random.nextFloat() * .4F);
            }
            moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2D, getZ());
            if (closestPlayer != null && (closestPlayer.isRemoved() || closestPlayer.level() != level())) closestPlayer = null;
            if (tickCount % 5 == 0 && closestPlayer == null && validValue()) {
                double nearest = Double.MAX_VALUE;
                for (Player player : level().getEntitiesOfClass(Player.class, new AABB(position(), position()).inflate(8))) {
                    double distance = player.distanceToSqr(this);
                    if (distance < nearest && !player.isSpectator() && firstWand(player) >= 0) { nearest = distance; closestPlayer = player; }
                }
            }
            if (closestPlayer != null) {
                Vec3 difference = new Vec3(closestPlayer.getX() - getX(), closestPlayer.getEyeY() - getY(), closestPlayer.getZ() - getZ());
                setDeltaMovement(getDeltaMovement().add(AspectOrbRules.attraction(difference)));
            }
            move(MoverType.SELF, getDeltaMovement());
            float friction = .98F;
            if (onGround()) {
                friction = .58800006F;
                var below = level().getBlockState(BlockPos.containing(getX(), getBoundingBox().minY - 1, getZ()));
                if (!below.isAir()) friction = below.getBlock().getFriction() * .98F;
            }
            Vec3 motion = getDeltaMovement(); setDeltaMovement(motion.x * friction, motion.y * .98F * (onGround() ? -.9F : 1F), motion.z * friction);
            if (++age >= MAX_AGE) discard();
        } finally { ticking = false; }
    }
    /** Source InventoryUtils398-409 chooses the FIRST eligible hotbar slot, including staves/sceptres. */
    private int firstWand(Player player) {
        var aspect = getAspect(); if (aspect == null || !WandData.PRIMALS.contains(aspect.getTag())) return -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack wand = player.getInventory().getItem(slot);
            if (!wand.isEmpty() && wand.getItem() instanceof ItemWandCasting && ItemWandCasting.getVis(wand, aspect.getTag()) < ItemWandCasting.getMaxVis(wand)) return slot;
        }
        return -1;
    }
    @Override public void playerTouch(Player actor) {
        if (!(actor instanceof ServerPlayer player) || !current() || !validValue() || claiming || cooldown != 0 || player.takeXpDelay != 0
                || player.level() != level() || !player.isAlive() || player.isSpectator()
                || ((ServerLevel)level()).getServer().getPlayerList().getPlayer(player.getUUID()) != player
                || !player.getBoundingBox().inflate(1, .5, 1).intersects(getBoundingBox())) return;
        claiming = true;
        try {
            int slot = firstWand(player); if (slot < 0) return;
            ItemStack actual = player.getInventory().getItem(slot);
            ItemStack before = ItemStack.parse(level().registryAccess(), actual.copyWithCount(1).save(level().registryAccess())).orElseThrow(); before.setCount(actual.getCount());
            ItemStack after = before.copy(); int amount = AspectOrbRules.centivis(value);
            int leftover = ItemWandCasting.addRealVis(after, getAspect().getTag(), amount, true);
            if (leftover >= amount || !current() || consumed || !validValue() || player.level() != level() || !player.isAlive() || player.isSpectator()
                    || ((ServerLevel)level()).getServer().getPlayerList().getPlayer(player.getUUID()) != player
                    || !player.getBoundingBox().inflate(1, .5, 1).intersects(getBoundingBox())
                    || player.getInventory().getItem(slot) != actual || !ItemStack.matches(actual, before) || player.takeXpDelay != 0) return;
            // Raw owner transfer precedes inventory notices, sounds and removal callbacks.
            actual.set(ThaumcraftContent.WAND_DATA.get(), after.get(ThaumcraftContent.WAND_DATA.get())); value = 0; consumed = true; player.takeXpDelay = 2;
            try {
                player.getInventory().setChanged();
                playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, .1F, .5F * ((random.nextFloat() - random.nextFloat()) * .7F + 1.8F));
                discard();
            } catch (RuntimeException failure) { Thaumcraft.LOG.warn("Aspect orb was paid into its actual wand; feedback failed", failure); }
        } finally { claiming = false; }
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && current() && !consumed && rejected == null && !isInvulnerableTo(source) && Float.isFinite(amount) && amount > 0) {
            markHurt(); health = (int)(health - amount); if (health <= 0) discard();
        }
        return false; // Original returns false even when that damage destroys the orb.
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (rejected != null) { tag.put("RejectedAspectOrb", rejected.copy()); return; }
        tag.putShort("Health", (short)(byte)health); tag.putShort("Age", (short)age); tag.putShort("Value", (short)value); tag.putString("Aspect", entityData.get(ASPECT));
        tag.putInt("IssuedValue", issuedValue); tag.putBoolean("Consumed", consumed);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        age = cooldown = value = issuedValue = 0; health = 5; consumed = false; closestPlayer = null; rejected = null;
        if (tag.contains("RejectedAspectOrb", Tag.TAG_COMPOUND)) { rejected = tag.getCompound("RejectedAspectOrb").copy(); return; }
        try {
            if (!tag.contains("Health",Tag.TAG_SHORT) || !tag.contains("Age",Tag.TAG_SHORT) || !tag.contains("Value",Tag.TAG_SHORT)
                    || !tag.contains("Aspect",Tag.TAG_STRING) || tag.contains("IssuedValue") && !tag.contains("IssuedValue",Tag.TAG_INT)
                    || tag.contains("Consumed") && !tag.contains("Consumed",Tag.TAG_BYTE)) throw new IllegalArgumentException("Missing native orb fields");
            Aspect aspect = Aspect.getAspect(tag.getString("Aspect"));
            health = tag.getShort("Health") & 255; age = tag.getShort("Age"); value = tag.getShort("Value"); consumed = tag.getBoolean("Consumed");
            issuedValue = tag.contains("IssuedValue", Tag.TAG_INT) ? tag.getInt("IssuedValue") : value;
            if (aspect == null || !aspect.isPrimal() || age < 0 || health <= 0 || health > 5 || issuedValue <= 0 || issuedValue > Short.MAX_VALUE
                    || consumed && value != 0 || !consumed && (value <= 0 || value > issuedValue)) throw new IllegalArgumentException("Invalid saved aspect orb");
            entityData.set(ASPECT, aspect.getTag());
        } catch (RuntimeException failure) { rejected = tag.copy(); value = issuedValue = 0; entityData.set(ASPECT, ""); }
    }
}
