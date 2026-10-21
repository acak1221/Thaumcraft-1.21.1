package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockLoot;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.cult.CultistRoomRules;
import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileBanner;

/**
 * Original TC4 crimson portal: the stationary gate that arms itself with four cult
 * banners, seeds loot crates around itself and sends wave after wave of cultists after
 * the player until the praetor answers at stage twelve.
 */
public class EntityCultistPortal extends Monster {
    private int stage = 0;
    private int stageCounter = 200;
    public int pulse = 0;

    public EntityCultistPortal(EntityType<? extends EntityCultistPortal> type, Level level) {
        super(type, level);
        xpReward = CultistRules.PORTAL_EXPERIENCE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CultistRules.PORTAL_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, CultistRules.PORTAL_KNOCKBACK_RESISTANCE);
    }

    public int getStage() { return stage; }

    public int getStageCounter() { return stageCounter; }

    @Override public Component getName() {
        return hasCustomName() ? getCustomName() : Component.translatable("entity.thaumcraft.cultistportal.name");
    }

    /** Original EntityCultistPortal: the portal never moves. */
    @Override public void move(MoverType type, Vec3 movement) {}

    /** Original EntityCultistPortal.canBePushed is false. */
    @Override public boolean isPushable() { return false; }

    /** Original EntityCultistPortal.canBeCollidedWith is true. */
    @Override public boolean isPickable() { return true; }

    /** Original EntityCultistPortal.fall does nothing. */
    @Override public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /** Original EntityCultistPortal.addPotionEffect ignores every effect. */
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) { return false; }

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("stage", stage);
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stage = tag.getInt("stage");
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (stageCounter > 0) {
                --stageCounter;
                if (stageCounter == CultistRules.PORTAL_BANNER_STAGE_COUNTER && stage == 0) placeBanners();
                if (stageCounter > CultistRules.PORTAL_CRATE_MIN_STAGE_COUNTER
                        && stageCounter < CultistRules.PORTAL_CRATE_MAX_STAGE_COUNTER
                        && stage == 0 && stageCounter % CultistRules.PORTAL_CRATE_INTERVAL == 0) placeLootCrate();
            } else {
                Player player = level().getNearestPlayer(this, CultistRules.PORTAL_ACTIVATION_RANGE);
                if (player != null) {
                    level().broadcastEntityEvent(this, (byte) 16);
                    int cultists = countCultists();
                    stageCounter = CultistRules.portalStageCounter(stage, cultists, random::nextInt);
                    if (CultistRules.portalSpawnsBoss(stage)) spawnBoss();
                    else spawnMinions();
                    ++stage;
                } else {
                    stageCounter = 30 + random.nextInt(30);
                }
            }
            if (stage < CultistRules.PORTAL_BOSS_STAGE) heal(CultistRules.portalHeal(stage));
            damageNearbyPlayers();
        }
        if (pulse > 0) --pulse;
    }

    /** Original EntityCultistPortal.getTiming: twenty ticks per living cultist around the portal. */
    private int countCultists() {
        return level().getEntitiesOfClass(EntityCultist.class,
                new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(CultistRules.PORTAL_MINION_RANGE)).size();
    }

    /** Original EntityCultistPortal: the four crimson cult banners around the portal. */
    private void placeBanners() {
        level().broadcastEntityEvent(this, (byte) 16);
        BlockState state = bannerState();
        // Original ForgeDirection ordinals two to five: north, south, west, east.
        for (int direction = 2; direction < 6; direction++) {
            CultistRoomRules.Banner banner = CultistRoomRules.banner(direction);
            BlockPos at = new BlockPos((int) getX() + banner.offsetX() * CultistRules.PORTAL_BANNER_RADIUS,
                    (int) getY(), (int) getZ() + banner.offsetZ() * CultistRules.PORTAL_BANNER_RADIUS);
            level().setBlock(at, state, 3);
            // Original EntityCultistPortal:157 - the banner shows its face to the portal.
            if (level().getBlockEntity(at) instanceof TileBanner tile) tile.setFacing(banner.facing());
            // Original EntityCultistPortal:157 - the banner draws the arc lightning of the portal.
            if (level() instanceof ServerLevel server)
                thaumcraft.common.lib.network.BlockFxNetworking.sendArc(server, this, at);
            playSound(EntityCultist.sound("wandfail"), 1.0F, 1.0F);
        }
    }

    /** The wooden device that carries the banner: the original metadata eight. */
    private BlockState bannerState() {
        return ThaumcraftEldritchBlocks.WOODEN_DEVICE.get().defaultBlockState()
                .setValue(BlockWoodenDevice.VARIANT, EldritchBlockRules.DEVICE_WOOD_BANNER);
    }

    /** Original EntityCultistPortal: the abandoned crates that seed the arena. */
    private void placeLootCrate() {
        int x = (int) getX() + random.nextInt(5) - random.nextInt(5);
        int z = (int) getZ() + random.nextInt(5) - random.nextInt(5);
        if (!CultistRoomRules.crateSpot((int) getX(), (int) getZ(), x, z)) return;
        BlockPos at = new BlockPos(x, (int) getY(), z);
        if (!level().getBlockState(at).isAir()) return;
        // Original EntityCultistPortal draws the crate variant from the world random.
        int variant = CultistRoomRules.crateRarity(level().getRandom().nextFloat());
        level().broadcastEntityEvent(this, (byte) 16);
        level().setBlock(at, lootCrateState(variant), 3);
        // Original EntityCultistPortal:169 - the crate draws the arc lightning of the portal.
        if (level() instanceof ServerLevel server)
            thaumcraft.common.lib.network.BlockFxNetworking.sendArc(server, this, at);
        playSound(EntityCultist.sound("wandfail"), 1.0F, 1.0F);
    }

    /** The abandoned crate: the original metadata is the rarity of the generated loot. */
    private BlockState lootCrateState(int rarity) {
        return ThaumcraftEldritchBlocks.LOOT_CRATE.get().defaultBlockState().setValue(BlockLoot.RARITY, rarity);
    }

    /** Original EntityCultistPortal.spawnMinions. */
    private void spawnMinions() {
        EntityCultist cultist = CultistRules.minionIsKnight(random.nextFloat())
                ? new EntityCultistKnight(ThaumcraftCultists.CULTIST_KNIGHT.get(), level())
                : new EntityCultistCleric(ThaumcraftCultists.CULTIST_CLERIC.get(), level());
        cultist.moveTo(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25,
                getZ() + random.nextFloat() - random.nextFloat(), 0.0F, 0.0F);
        cultist.originalSpawnEquipment(serverLevel(), difficulty());
        cultist.restrictTo(blockPosition(), CultistRules.PORTAL_MINION_HOME_RADIUS);
        level().broadcastEntityEvent(cultist, (byte) 3);
        level().addFreshEntity(cultist);
        playSound(EntityCultist.sound("wandfail"), 1.0F, 1.0F);
        int selfDamage = CultistRules.portalSelfDamage(stage, random.nextInt(5));
        // Original EntityCultistPortal:224 - the portal burns itself with DamageSource.outOfWorld.
        if (selfDamage > 0) hurt(damageSources().fellOutOfWorld(), selfDamage);
    }

    /** Original EntityCultistPortal.spawnBoss. */
    private void spawnBoss() {
        EntityCultistLeader boss = new EntityCultistLeader(ThaumcraftCultists.CULTIST_LEADER.get(), level());
        boss.moveTo(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25,
                getZ() + random.nextFloat() - random.nextFloat(), 0.0F, 0.0F);
        boss.originalSpawnEquipment(serverLevel(), difficulty());
        boss.restrictTo(blockPosition(), CultistRules.PORTAL_MINION_HOME_RADIUS);
        level().broadcastEntityEvent(boss, (byte) 3);
        level().addFreshEntity(boss);
        playSound(EntityCultist.sound("wandfail"), 1.0F, 1.0F);
    }

    /** Original EntityCultistPortal.onCollideWithPlayer: eight points of magic damage. */
    private void damageNearbyPlayers() {
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(3.0))) {
            if (distanceToSqr(player) >= 3.0) continue;
            if (player.hurt(damageSources().indirectMagic(this, this), 8.0F)) {
                playSound(EntityCultist.sound("zap"), 1.0F, (random.nextFloat() - random.nextFloat()) * 0.1F + 1.0F);
            }
        }
    }

    private ServerLevel serverLevel() { return (ServerLevel) level(); }

    private DifficultyInstance difficulty() {
        return level() instanceof ServerLevel server ? server.getCurrentDifficultyAt(blockPosition())
                : new DifficultyInstance(Difficulty.NORMAL, 0L, 0L, 1.0F);
    }

    /** Original EntityCultistPortal.handleStatusUpdate: the pulse of the portal. */
    @Override public void handleEntityEvent(byte event) {
        if (event == 16) {
            pulse = 10;
            level().broadcastEntityEvent(this, (byte) 3);
        } else super.handleEntityEvent(event);
    }

    /** Original EntityCultistPortal: the primordial pearl falls as a floating special item. */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemEntity special = ThaumcraftContent.SPECIAL_ITEM.get().create(level);
        if (special == null) return;
        special.setItem(new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get()));
        special.setPos(getX(), getY() + getBbHeight() / 2.0, getZ());
        special.setDeltaMovement(0.0, 0.1, 0.0);
        special.setPickUpDelay(10);
        level.addFreshEntity(special);
    }

    /** Original EntityCultistPortal.getDropItem is air. */
    @Override protected void dropFromLootTable(DamageSource source, boolean recentlyHit) {}

    /** Original EntityCultistPortal.onDeath: the portal bursts. */
    @Override public void die(DamageSource source) {
        if (!level().isClientSide) level().explode(this, getX(), getY(), getZ(), 2.0F, ExplosionInteraction.NONE);
        super.die(source);
    }

    @Override protected SoundEvent getAmbientSound() { return EntityCultist.sound("monolith"); }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return EntityCultist.sound("zap"); }

    @Override protected SoundEvent getDeathSound() { return EntityCultist.sound("shock"); }

    @Override protected float getSoundVolume() { return 0.75F; }

    @Override public int getAmbientSoundInterval() { return 540; }
}
