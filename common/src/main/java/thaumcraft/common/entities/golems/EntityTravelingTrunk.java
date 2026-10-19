package thaumcraft.common.entities.golems;

import dev.architectury.registry.menu.MenuRegistry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerTravelingTrunk;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.ItemGolemUpgrade;
import thaumcraft.common.items.golems.TrunkRules;

/**
 * Original EntityTravelingTrunk: a living chest that hops after its owner, keeps 27 (36 with Earth) stacks, takes one golem upgrade
 * and is recalled by the golem bell. Movement is hand-made like the original (no AI goals): a hop is a jump plus a push in the facing
 * direction that lasts until the landing. The decisions are {@link TrunkRules}.
 */
public class EntityTravelingTrunk extends Mob implements OwnableEntity {
    private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STAY = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> UPGRADE = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROWS = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(EntityTravelingTrunk.class, EntityDataSerializers.INT);
    private static final SoundEvent UPGRADE_SOUND = sound("upgrade"), ZAP = sound("zap");
    /** Owner name to the trunks that follow it across dimensions (original EventHandlerEntity.linkedEntities). */
    private static final Map<UUID, List<WeakReference<EntityTravelingTrunk>>> LINKED = new HashMap<>();

    public final InventoryTrunk inventory = new InventoryTrunk(this);
    /** Client animation of the lid (original lidrot): 0 shut, up to 0.5 open. */
    public float lidrot;
    private int jumpDelay, attackTime;
    private float hopForward, hopStrafe;

    public EntityTravelingTrunk(EntityType<? extends EntityTravelingTrunk> type, Level level) {
        super(type, level);
        jumpDelay = random.nextInt(20) + 10;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, TrunkRules.MAX_HEALTH).add(Attributes.ATTACK_DAMAGE, TrunkRules.ATTACK_DAMAGE);
    }

    private static SoundEvent sound(String path) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", path)); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OPEN, false).define(STAY, false).define(OWNER, Optional.empty()).define(OWNER_NAME, "")
                .define(UPGRADE, TrunkRules.NO_UPGRADE).define(ROWS, 3).define(ANGER, 0);
    }

    // ---- state ---------------------------------------------------------------------------------------------------

    public boolean isOpen() { return entityData.get(OPEN); }
    public void setOpen(boolean open) { entityData.set(OPEN, open); }
    public boolean getStay() { return entityData.get(STAY); }
    public void setStay(boolean stay) { entityData.set(STAY, stay); }
    public int getUpgrade() { return entityData.get(UPGRADE); }
    public int getRows() { return entityData.get(ROWS); }
    public int getAnger() { return entityData.get(ANGER); }
    public void setAnger(int anger) { entityData.set(ANGER, Math.max(0, anger)); }
    public int slotCount() { return getRows() * 9; }
    public String getOwnerName() { return entityData.get(OWNER_NAME); }
    @Override public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }

    public void setOwner(Player player) {
        entityData.set(OWNER, Optional.of(player.getUUID()));
        entityData.set(OWNER_NAME, player.getGameProfile().getName());
    }

    public boolean isOwner(Player player) { return player != null && player.getUUID().equals(getOwnerUUID()); }

    /** Original setUpgrade + setInvSize. */
    public void setUpgrade(int upgrade) {
        entityData.set(UPGRADE, TrunkRules.validUpgrade(upgrade) ? upgrade : TrunkRules.NO_UPGRADE);
        setInvSize();
    }

    public void setInvSize() { entityData.set(ROWS, TrunkRules.rows(getUpgrade())); }

    // ---- save ----------------------------------------------------------------------------------------------------

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Stay", getStay());
        tag.putByte("upgrade", (byte) getUpgrade());
        tag.putString("Owner", getOwnerName());
        if (getOwnerUUID() != null) tag.putUUID("OwnerUUID", getOwnerUUID());
        tag.put("Inventory", inventory.save(registryAccess()));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStay(tag.getBoolean("Stay"));
        int upgrade = tag.getByte("upgrade");
        if (!TrunkRules.validUpgrade(upgrade)) Thaumcraft.LOG.warn("Traveling trunk with unknown upgrade {} read as having none", upgrade);
        setUpgrade(upgrade);
        entityData.set(OWNER_NAME, tag.getString("Owner"));
        entityData.set(OWNER, tag.hasUUID("OwnerUUID") ? Optional.of(tag.getUUID("OwnerUUID")) : Optional.empty());
        inventory.load(tag.getList("Inventory", Tag.TAG_COMPOUND), registryAccess());
    }

    // ---- damage, death, bell --------------------------------------------------------------------------------------

    @Override public boolean fireImmune() { return true; }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player && source.getEntity() == player
                && player.getMainHandItem().is(ThaumcraftGolems.GOLEM_BELL.get())) {
            if (!level().isClientSide) recall(player, player.isShiftKeyDown());
            return false; // The bell is a recall tool, never ordinary damage.
        }
        if (source.is(DamageTypes.CACTUS) || TrunkRules.invulnerable(getUpgrade())) return false;
        return super.hurt(source, amount);
    }

    /**
     * Original ItemGolemBell.onLeftClickEntity: the trunk becomes a spawner item. Plain recall keeps the upgrade and the name, and keeps the
     * contents only with the Order upgrade; a sneaking recall (dismantling) gives a blank spawner, maybe the upgrade item (one chance in
     * two) and drops every stack. A Water trunk answers to its owner alone.
     */
    public boolean recall(Player player, boolean dismantle) {
        int upgrade = getUpgrade();
        if (!isAlive() || !TrunkRules.mayOpen(upgrade, isOwner(player))) return false;
        ItemStack dropped = new ItemStack(ThaumcraftGolems.TRUNK_SPAWNER.get());
        if (dismantle) {
            if (upgrade > TrunkRules.NO_UPGRADE && random.nextBoolean()) spawnAtLocation(new ItemStack(ThaumcraftGolems.upgradeItem(upgradeKind(upgrade))), 0.5F);
        } else {
            if (hasCustomName()) dropped.set(DataComponents.CUSTOM_NAME, getCustomName());
            dropped.set(ThaumcraftGolems.TRUNK_UPGRADE.get(), upgrade);
            if (upgrade == TrunkRules.ORDER) dropped.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory.contents()));
        }
        spawnAtLocation(dropped, 0.5F);
        if (upgrade != TrunkRules.ORDER || dismantle) inventory.dropAll();
        playSound(ZAP, 0.5F, 1.0F);
        discard();
        return true;
    }

    private static GolemUpgrade upgradeKind(int upgrade) {
        for (GolemUpgrade kind : GolemUpgrade.values()) if (kind.id == upgrade) return kind;
        throw new IllegalArgumentException("No golem upgrade " + upgrade);
    }

    @Override public void die(DamageSource source) {
        if (!level().isClientSide) inventory.dropAll();
        super.die(source);
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return net.minecraft.world.level.block.SoundType.WOOD.getBreakSound(); }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ITEM_BREAK; }
    @Override protected float getSoundVolume() { return 0.5F; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canPickUpLoot() { return false; }

    // ---- interaction ---------------------------------------------------------------------------------------------

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ThaumcraftGolems.GOLEM_BELL.get())) {
            return TrunkRules.mayOpen(getUpgrade(), isOwner(player)) ? InteractionResult.PASS : InteractionResult.CONSUME;
        }
        if (getUpgrade() == TrunkRules.NO_UPGRADE && stack.getItem() instanceof ItemGolemUpgrade upgrade) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            setUpgrade(upgrade.kind().id);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            playSound(UPGRADE_SOUND, 0.5F, 1.0F);
            return InteractionResult.CONSUME;
        }
        var food = stack.get(DataComponents.FOOD);
        if (food != null && getHealth() < getMaxHealth()) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            heal(food.nutrition());
            playSound(getHealth() == getMaxHealth() ? SoundEvents.PLAYER_BURP : SoundEvents.GENERIC_EAT, 0.5F, random.nextFloat() * 0.5F + (getHealth() == getMaxHealth() ? 0.5F : 0.5F));
            level().broadcastEntityEvent(this, (byte) 18);
            return InteractionResult.CONSUME;
        }
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!TrunkRules.mayOpen(getUpgrade(), isOwner(player))) return InteractionResult.CONSUME;
        if (player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, new SimpleMenuProvider((id, playerInventory, who) -> new ContainerTravelingTrunk(id, playerInventory, this),
                    hasCustomName() ? getCustomName() : getTypeName()), buffer -> buffer.writeVarInt(getId()).writeVarInt(getRows()));
        }
        return InteractionResult.CONSUME;
    }

    @Override public void handleEntityEvent(byte event) {
        if (event == 17) lidrot = 0.15F;
        else if (event == 18) { lidrot = 0.15F; showHearts(); }
        else super.handleEntityEvent(event);
    }

    private void showHearts() {
        for (int index = 0; index < 1; index++) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART, getX() + random.nextFloat() * getBbWidth() * 2 - getBbWidth(),
                    getY() + 0.5 + random.nextFloat() * getBbHeight(), getZ() + random.nextFloat() * getBbWidth() * 2 - getBbWidth(),
                    random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
        }
    }

    // ---- ticking -------------------------------------------------------------------------------------------------

    @Override public void aiStep() {
        super.aiStep();
        if (TrunkRules.pullsItems(getUpgrade())) pullItems();
    }

    @Override public void tick() {
        super.tick();
        if (isInWater()) setDeltaMovement(getDeltaMovement().add(0, 0.033, 0));
        if (level().isClientSide) {
            if (!onGround() && getDeltaMovement().y < 0 && !isInWater()) lidrot += 0.015F;
            if ((onGround() || isInWater()) && !isOpen()) lidrot = Math.max(0F, lidrot - 0.1F);
            if (isOpen()) lidrot += 0.035F;
            lidrot = Math.min(lidrot, isOpen() ? 0.5F : 0.2F);
        } else if (getHealth() < getMaxHealth() && TrunkRules.healsNow(getUpgrade(), tickCount)) {
            heal(1.0F);
        }
    }

    /** The hop's push: the facing direction until the landing, with the air acceleration of the original jumpMovementFactor. */
    @Override protected float getFlyingSpeed() { return TrunkRules.airAcceleration(getUpgrade()); }

    @Override public void travel(Vec3 travelVector) {
        super.travel(new Vec3(hopStrafe * 0.98F, travelVector.y, hopForward * 0.98F));
    }

    @Override protected void customServerAiStep() {
        if (getAnger() > 0) setAnger(getAnger() - 1);
        if (attackTime > 0) --attackTime;
        fallDistance = 0;
        LivingEntity owner = getOwner();
        if (owner == null) return;
        link(owner);
        if (!getStay() && distanceTo(owner) > TrunkRules.TELEPORT_DISTANCE && teleportNextTo(owner)) {
            setTarget(null);
            return;
        }
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
            setAnger(5);
        }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (!getStay() && TrunkRules.defendsOwner(getUpgrade()) && getAnger() == 0 && getTarget() == null
                && attacker != null && attacker.isAlive() && hasLineOfSight(attacker)) {
            setAnger(TrunkRules.ANGER_TICKS);
            setTarget(attacker);
        }
        boolean move = false;
        LivingEntity target = getTarget();
        if (getAnger() > 0 && target != null && target.isAlive() && target != owner) {
            face(target);
            move = true;
            if (attackTime <= 0 && distanceTo(target) < 1.5 && target.getBoundingBox().maxY > getBoundingBox().minY && target.getBoundingBox().minY < getBoundingBox().maxY) {
                attackTime = TrunkRules.attackCooldown(random.nextInt(5));
                target.hurt(damageSources().mobAttack(this), TrunkRules.ATTACK_DAMAGE);
                level().broadcastEntityEvent(this, (byte) 17);
                playSound(SoundEvents.BLAZE_HURT, 0.5F, random.nextFloat() * 0.1F + 0.9F);
            }
        }
        if (distanceTo(owner) > TrunkRules.FOLLOW_DISTANCE && getAnger() == 0 && !getStay()) {
            face(owner);
            move = true;
        }
        if ((onGround() || isInWater()) && jumpDelay-- <= 0 && move) {
            jumpDelay = TrunkRules.hopDelay(random.nextInt(10));
            getJumpControl().jump();
            hopStrafe = 1.0F - random.nextFloat() * 2.0F;
            hopForward = TrunkRules.fast(getUpgrade()) ? 8.0F : 6.0F;
            if (isInWater()) hopForward *= 0.75F;
            playSound(SoundEvents.CHEST_CLOSE, 0.1F, random.nextFloat() * 0.1F + 0.9F);
        } else if (onGround()) {
            hopForward = 0;
            hopStrafe = 0;
        }
    }

    /** Original faceEntity(target, 10, 20): at most ten degrees of yaw and twenty of pitch a tick. */
    private void face(Entity target) {
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dy = (target instanceof LivingEntity living ? living.getEyeY() : (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2) - getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * 180.0 / Math.PI);
        setXRot(Mth.approachDegrees(getXRot(), pitch, 20.0F));
        float turned = Mth.approachDegrees(getYRot(), yaw, 10.0F);
        setYRot(turned);
        setYHeadRot(turned);
        setYBodyRot(turned);
    }

    /** Original: the first free spot on the ring of 16 cells round the owner (a 5 x 5 square without its 3 x 3 core). */
    private boolean teleportNextTo(LivingEntity owner) {
        int baseX = Mth.floor(owner.getX()) - 2, baseZ = Mth.floor(owner.getZ()) - 2, y = Mth.floor(owner.getBoundingBox().minY);
        for (int dx = 0; dx <= 4; dx++) for (int dz = 0; dz <= 4; dz++) {
            if (dx >= 1 && dz >= 1 && dx <= 3 && dz <= 3) continue;
            BlockPos floor = new BlockPos(baseX + dx, y - 1, baseZ + dz), feet = floor.above(), head = feet.above();
            BlockState under = level().getBlockState(floor);
            boolean support = under.isCollisionShapeFullBlock(level(), floor) || under.getFluidState().is(FluidTags.WATER);
            if (!support || blocks(feet) || blocks(head)) continue;
            level().playSound(null, baseX + dx + 0.5, y, baseZ + dz + 0.5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5F, 1.0F);
            moveTo(baseX + dx + 0.5, y, baseZ + dz + 0.5, getYRot(), getXRot());
            return true;
        }
        return false;
    }

    private boolean blocks(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return !state.getCollisionShape(level(), pos).isEmpty();
    }

    // ---- items ---------------------------------------------------------------------------------------------------

    private void pullItems() {
        if (isRemoved() || getHealth() <= 0) return;
        if (!level().isClientSide) {
            AABB swallow = new AABB(getX() - TrunkRules.SWALLOW_RADIUS, getY() - TrunkRules.SWALLOW_RADIUS, getZ() - TrunkRules.SWALLOW_RADIUS,
                    getX() + TrunkRules.SWALLOW_RADIUS, getY() + TrunkRules.SWALLOW_RADIUS, getZ() + TrunkRules.SWALLOW_RADIUS);
            for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, swallow)) {
                ItemStack stack = item.getItem();
                ItemStack rest = inventory.insert(stack);
                if (rest.getCount() == stack.getCount()) continue;
                playSound(SoundEvents.GENERIC_EAT, 0.5F, random.nextFloat() * 0.5F + 0.5F);
                level().broadcastEntityEvent(this, (byte) 17);
                if (rest.isEmpty()) item.discard();
                else item.setItem(rest);
            }
        }
        AABB pull = new AABB(getX() - TrunkRules.PULL_RADIUS, getY() - TrunkRules.PULL_RADIUS, getZ() - TrunkRules.PULL_RADIUS,
                getX() + TrunkRules.PULL_RADIUS, getY() + TrunkRules.PULL_RADIUS, getZ() + TrunkRules.PULL_RADIUS);
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, pull)) {
            double dx = item.getX() - getX(), dy = item.getY() - getY() + getBbHeight() * 0.8F, dz = item.getZ() - getZ();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-6) continue;
            item.setDeltaMovement(item.getDeltaMovement().subtract(dx / length * TrunkRules.PULL_STRENGTH, dy / length * TrunkRules.PULL_STRENGTH, dz / length * TrunkRules.PULL_STRENGTH));
        }
    }

    // ---- following the owner across dimensions -------------------------------------------------------------------

    private void link(LivingEntity owner) {
        List<WeakReference<EntityTravelingTrunk>> trunks = LINKED.computeIfAbsent(owner.getUUID(), key -> new ArrayList<>());
        trunks.removeIf(reference -> reference.get() == null || reference.get().isRemoved());
        for (WeakReference<EntityTravelingTrunk> reference : trunks) if (reference.get() == this) return;
        trunks.add(new WeakReference<>(this));
    }

    /** Original EventHandlerEntity:322-330: when the owner changes dimension, every linked trunk that is not told to stay follows. */
    public static void ownerChangedDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        List<WeakReference<EntityTravelingTrunk>> trunks = LINKED.get(player.getUUID());
        if (trunks == null) return;
        ServerLevel destination = player.serverLevel();
        for (WeakReference<EntityTravelingTrunk> reference : new ArrayList<>(trunks)) {
            EntityTravelingTrunk trunk = reference.get();
            if (trunk == null || trunk.isRemoved() || trunk.getStay() || trunk.isOnPortalCooldown() || trunk.level() == destination) continue;
            trunk.setPortalCooldown(trunk.getDimensionChangingDelay());
            Entity moved = trunk.changeDimension(new DimensionTransition(destination, player.position().add(0, 0.25, 0), Vec3.ZERO, trunk.getYRot(), trunk.getXRot(), DimensionTransition.DO_NOTHING));
            if (moved == null) Thaumcraft.LOG.warn("Traveling trunk of {} could not follow into {}", player.getName().getString(), to.location());
        }
    }

    public Component ownerLabel() { return Component.literal(getOwnerName()); }
}
