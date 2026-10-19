package thaumcraft.common.entities.golems;

import com.mojang.serialization.Codec;
import dev.architectury.hooks.item.ItemStackHooks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemDecorations;
import thaumcraft.common.items.golems.GolemAspectCargo;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemItemMatch;
import thaumcraft.common.items.golems.GolemLiquidQueue;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.items.golems.GolemWaterCargo;
import thaumcraft.common.items.golems.ItemGolemPlacer;
import thaumcraft.common.items.golems.ItemGolemBell;
import thaumcraft.common.items.golems.ItemGolemCore;
import thaumcraft.common.items.golems.ItemGolemDecoration;
import thaumcraft.common.items.golems.ItemGolemUpgrade;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileJar;

/** Supported ordinary materials and Fill/Empty/Gather/Harvest/Guard/Decanting/Alchemy/Use/Butcher/Sorting/Fishing. */
public class EntityGolemBase extends AbstractGolem {
    private static final EntityDataAccessor<Integer> MATERIAL = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CORE = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<BlockPos> HOME = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> HOME_FACE = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> WATER_MB = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ESSENTIA_TAG = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ESSENTIA_AMOUNT = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BOOTUP = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HEALING = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RIGHT_ARM = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FISHING_BOBBER_ID = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> FISHING_BOBBER_UUID = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.OPTIONAL_UUID);
    // Eighteen ghost accessors cover six slots plus two Fire pages; only getSlotCount() of them are meaningful.
    private static final List<EntityDataAccessor<ItemStack>> FILTERS = defineGhosts(EntityDataSerializers.ITEM_STACK);
    private static final List<EntityDataAccessor<Integer>> WANTED = defineGhosts(EntityDataSerializers.INT);
    // Order slot colours (-1 any, 0..15 dye colour), one per ghost slot like WANTED.
    private static final List<EntityDataAccessor<Integer>> COLORS = defineGhosts(EntityDataSerializers.INT);
    private static <T> List<EntityDataAccessor<T>> defineGhosts(EntityDataSerializer<T> serializer) {
        List<EntityDataAccessor<T>> accessors = new ArrayList<>(GolemData.MAX_FILTER_SLOTS);
        for (int slot = 0; slot < GolemData.MAX_FILTER_SLOTS; slot++) accessors.add(SynchedEntityData.defineId(EntityGolemBase.class, serializer));
        return List.copyOf(accessors);
    }
    private static final EntityDataAccessor<Boolean> FILL_ANY = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.BOOLEAN);
    // Packed upgrade slots (GolemUpgrades.pack); the server owns the real int[] below.
    private static final EntityDataAccessor<Integer> UPGRADES = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.INT);
    // Advanced golem (original placer/entity tag "advanced"): the client needs it only for the number of upgrade slots.
    private static final EntityDataAccessor<Boolean> ADVANCED = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.BOOLEAN);
    // Accessory letters in installation order (GolemDecorations); the client renders them, the server derives every effect.
    private static final EntityDataAccessor<String> DECORATION = SynchedEntityData.defineId(EntityGolemBase.class, EntityDataSerializers.STRING);
    private static final Codec<List<GolemMarker>> MARKER_CODEC = GolemMarker.CODEC.listOf();
    private static final SoundEvent UPGRADE = sound("upgrade"), ZAP = sound("zap"), CLACK = sound("cameraclack");
    private static final List<String> CONFIG_KEYS = List.of("GolemType", "GolemCore", "Core", "HomeFacing",
            "advanced", "Decoration", "deco", "upgrades", "colors", "toggles", "GuardTargets", "ItemMatch", "Markers", "Inventory", "WantedCounts", "FillAny", "UseOptions");
    private static final Set<String> GHOST_STACK_KEYS = Set.of("Slot", "id", "count", "components");
    private static final Set<String> USE_STACK_KEYS = Set.of("id", "count", "components");
    private static final int MAX_USE_REMAINDERS = 41; // 40 non-selected native inventory slots + at most one carried/menu stack.

    // This is the ONE real ITEM cargo owner. Synched data and public getters are
    // copied displays, never equipment, filters, another inventory or output.
    private ItemStack carried = ItemStack.EMPTY;
    private int waterMb; // Separate actual water stock, never an item/container/filter.
    private Tag unreadWater;
    private GolemAspectCargo aspectCargo = GolemAspectCargo.EMPTY; // One actual immutable owner, NOT a display jar.
    private Tag unreadAspect;
    private final Set<Player> configurationOpeners = new HashSet<>();
    private final GatherGolemGoals work = new GatherGolemGoals(this);
    private final InventoryGolemGoals inventories = new InventoryGolemGoals(this);
    private final HarvestGolemGoals harvest = new HarvestGolemGoals(this);
    private final LumberGolemGoals lumber = new LumberGolemGoals(this);
    private final GuardGolemGoals guard = new GuardGolemGoals(this);
    private final FishingGolemGoals fishing = new FishingGolemGoals(this);
    private final LiquidGolemGoals liquid = new LiquidGolemGoals(this);
    private final AlchemyGolemGoals alchemy = new AlchemyGolemGoals(this);
    private final UseGolemGoals use = new UseGolemGoals(this);
    private GolemUseOptions useOptions = GolemUseOptions.DEFAULT;
    private List<ItemStack> pendingUseExtras = List.of(); // Actual returned owners, NOT GolemData/ghosts.
    private Tag unreadUseRemainders;
    private boolean useWorkScope, useInvoking, useTransferScope, publishingUseRemainders;
    private boolean useCargoDetached, useCargoReturned, usePermissionReady, useSucceeded, detachingUseCargo;
    private ServerLevel useWorkLevel;
    private UseGolemGoals.Target useIntent;
    private GolemHomeInventory.Destination useHomeIntent;
    private BlockState useHomeState;
    private ItemStack useBeforeCarried, useItemIdentity; // The latter is authorization ONLY; never a refund/supply.
    private Entity.RemovalReason deferredUseRemoval;
    private List<GolemMarker> markers = List.of();
    // One entry per material slot, -1 empty. Server-owned; the client reads the packed copy.
    private int[] upgradeSlots = new int[0];
    private boolean reflectingThorns;
    // Order Guard target switches (GolemGuardTargets); server-only, the menu mirrors them. Default: hostile monsters only.
    private int guardTargets = GolemGuardTargets.DEFAULT;
    private boolean advanced;
    private String decoration = "";
    // Entropy's item-comparison switches (GolemItemMatch); server-only, the menu mirrors them.
    private int itemMatch;
    private int core = GolemData.NO_CORE;
    private int material;
    private int regenTimer;
    private boolean hasHome;
    private boolean busy;
    private boolean checkingButcherPosition, checkingButcherPopulation;
    private BlockPos throwTarget; // Non-null only during the private worker's queue publication.
    private ServerLevel waterWorkLevel;
    private BlockPos waterSourceIntent, waterHomePosition, waterAnchorIntent;
    private BlockState waterSourceState;
    private Direction waterHomeFace;
    private TileCrucible waterHomeIntent;
    private int waterStockBefore;
    private boolean waterPermissionReady;
    private Entity.RemovalReason deferredWaterRemoval;
    private boolean aspectWorkScope, aspectPermissionReady;
    private ServerLevel aspectWorkLevel;
    private BlockPos aspectHomePosition;
    private Direction aspectHomeFace;
    private BlockEntity aspectBaseIntent;
    private TileAlembic aspectSourceIntent;
    private TileJar aspectSinkIntent;
    private Aspect aspectIntent;
    private int aspectLimit;
    private GolemAspectCargo aspectBeforeIntent;
    private Entity.RemovalReason deferredAspectRemoval;
    private boolean retiring;
    private boolean droppingCargo;
    private boolean unsupported;
    private CompoundTag retainedConfiguration;
    private Tag unreadCarried;

    public EntityGolemBase(EntityType<? extends EntityGolemBase> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false); // Gather must never also equip the same dropped item.
        setPathfindingMalus(PathType.WATER, 8);
        getNavigation().setCanFloat(true);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanPassDoors(true);
            ground.setCanOpenDoors(false);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        // A new entity starts as Straw; fresh placement chooses its actual material.
        // Native navigation multiplies material speed by modifier 1, NOT speed again.
        EnumGolemType type = EnumGolemType.STRAW;
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, type.health).add(Attributes.ARMOR, type.armor)
                .add(Attributes.ATTACK_DAMAGE, 2 + type.strength).add(Attributes.MOVEMENT_SPEED, type.speed)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 1);
    }
    private static SoundEvent sound(String path) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", path)); }
    @Override protected void registerGoals() { /* Coreless is genuinely dormant, not a disabled global mob. */ }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MATERIAL, EnumGolemType.STRAW.ordinal());
        builder.define(CORE, GolemData.NO_CORE);
        builder.define(OWNER, "");
        builder.define(HOME, BlockPos.ZERO);
        builder.define(HOME_FACE, Direction.DOWN.get3DDataValue());
        builder.define(CARRIED, ItemStack.EMPTY);
        builder.define(WATER_MB, 0);
        builder.define(ESSENTIA_TAG, "");
        builder.define(ESSENTIA_AMOUNT, 0);
        builder.define(BOOTUP, 0F);
        builder.define(HEALING, 0);
        builder.define(ACTION, 0);
        builder.define(RIGHT_ARM, 0);
        builder.define(FISHING_BOBBER_ID, -1);
        builder.define(FISHING_BOBBER_UUID, Optional.empty());
        for (EntityDataAccessor<ItemStack> filter : FILTERS) builder.define(filter, ItemStack.EMPTY);
        for (EntityDataAccessor<Integer> wanted : WANTED) builder.define(wanted, 0);
        for (EntityDataAccessor<Integer> color : COLORS) builder.define(color, GolemData.NO_COLOR);
        builder.define(FILL_ANY, false);
        builder.define(UPGRADES, 0);
        builder.define(ADVANCED, false);
        builder.define(DECORATION, "");
    }
    @Override public void checkDespawn() { /* Original placed golems never naturally despawn. */ }
    @Override protected SoundEvent getAmbientSound() { return CLACK; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return CLACK; }
    @Override protected SoundEvent getDeathSound() { return CLACK; }
    @Override protected float getSoundVolume() { return .1F; }
    @Override protected Component getTypeName() {
        Item placer = ThaumcraftGolems.placerForMaterial(getGolemMaterial());
        // Native Entity.getName still gives an actual custom name precedence.
        return placer == null ? super.getTypeName() : placer.getDescription();
    }

    public int getGolemMaterial() { return level().isClientSide ? entityData.get(MATERIAL) : material; }
    public void setGolemMaterial(int value) {
        requireServerThread();
        if (!EnumGolemType.isSupported(value)) throw new IllegalArgumentException("Unsupported golem material: " + value);
        if (!(level() instanceof ServerLevel server) || tickCount != 0 || current() || isRemoved()
                || server.getEntity(getId()) == this || server.getEntity(getUUID()) == this
                || !carried.isEmpty() || unreadCarried != null || waterMb != 0 || unreadWater != null
                || aspectCargo.amount() != 0 || unreadAspect != null || essentiaMutationActive()
                || fishing.hasPending() || hasUseRemainders() || useCustodyActive() || unsupported || retiring || busy) {
            throw new IllegalStateException("Material can only be selected on a fresh, unadmitted golem");
        }
        material = value;
        advanced = false; // A fresh golem is ordinary; applyConfiguration may make it advanced.
        entityData.set(ADVANCED, false);
        decoration = "";
        entityData.set(DECORATION, "");
        upgradeSlots = GolemUpgrades.empty(EnumGolemType.getType(value).upgrades);
        entityData.set(UPGRADES, 0);
        applyMaterialAttributes(EnumGolemType.getType(value));
        setHealth(getMaxHealth()); // Only this legitimate fresh-creation path fills health.
        entityData.set(MATERIAL, value);
    }
    private void applyMaterialAttributes(EnumGolemType type) {
        // Keep native modifiers and every unrelated saved attribute. Only the
        // four canonical material BASE values are selected here, with the Air
        // speed factor and the Earth damage (original E:255, :376) on top.
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.health + GolemDecorations.maxHealthBonus(decoration));
        getAttribute(Attributes.ARMOR).setBaseValue(GolemDecorations.armor(type.armor, decoration));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(GolemUpgrades.speed(type.speed, GolemUpgrades.count(upgradeSlots, GolemUpgrade.AIR), advanced, decoration));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(GolemUpgrades.meleeDamage(type.strength, GolemUpgrades.count(upgradeSlots, GolemUpgrade.EARTH), decoration));
    }
    @Override public boolean fireImmune() {
        EnumGolemType type = EnumGolemType.getType(getGolemMaterial());
        return type != null && type.fireResist;
    }
    public int getGolemCore() { return level().isClientSide ? entityData.get(CORE) : core; }
    private static boolean inventoryCore(int value) {
        return value == GolemData.FILL || value == GolemData.EMPTY_CORE || value == GolemData.GATHER || value == GolemData.SORTING;
    }
    private static boolean activeCore(int value) {
        return inventoryCore(value) || value == GolemData.HARVEST || value == GolemData.LUMBER || value == GolemData.GUARD
                || value == GolemData.LIQUID || value == GolemData.ALCHEMY || value == GolemData.USE
                || value == GolemData.BUTCHER || value == GolemData.FISHING;
    }
    private static boolean requiresEmptyControls(int value) {
        return value == GolemData.SORTING || value == GolemData.HARVEST || value == GolemData.LUMBER || value == GolemData.GUARD
                || value == GolemData.ALCHEMY || value == GolemData.BUTCHER || value == GolemData.FISHING;
    }
    /** Supported active worker; this does NOT imply a configuration menu. */
    public boolean hasSupportedCore() { return activeCore(getGolemCore()); }
    public boolean hasConfigurationMenu() {
        return (inventoryCore(getGolemCore()) || getGolemCore() == GolemData.GUARD || getGolemCore() == GolemData.LIQUID || getGolemCore() == GolemData.USE)
                && !unsupported && !retiring && unreadCarried == null && unreadWater == null && unreadAspect == null
                && !hasUseRemainders() && !useCustodyActive()
                && EnumGolemType.isSupported(getGolemMaterial()) && isAlive() && current();
    }
    /** Material strength plus one per Earth upgrade (original E:850-852). */
    public int getGolemStrength() {
        EnumGolemType type = EnumGolemType.getType(getGolemMaterial());
        return type == null ? 0 : GolemUpgrades.strength(type.strength, getUpgradeAmount(GolemUpgrade.EARTH));
    }
    /** Original E:255: Earth is counted twice, once inside the strength and once more here. */
    public float getMeleeDamage() {
        EnumGolemType type = EnumGolemType.getType(getGolemMaterial());
        return type == null ? 0 : GolemUpgrades.meleeDamage(type.strength, getUpgradeAmount(GolemUpgrade.EARTH), getDecoration());
    }
    public boolean isAdvanced() { return level().isClientSide ? entityData.get(ADVANCED) : advanced; }
    /** The accessory letters in installation order (original E:112, 819-825); empty when undecorated. */
    public String getDecoration() { return level().isClientSide ? entityData.get(DECORATION) : decoration; }
    /** The material's upgrade slots plus the advanced golem's extra one (original E:149, 625). */
    public int getUpgradeSlotCount() {
        EnumGolemType type = EnumGolemType.getType(getGolemMaterial());
        return type == null ? 0 : GolemUpgrades.slotCount(type.upgrades, isAdvanced());
    }
    private int[] upgradeView() {
        return level().isClientSide ? GolemUpgrades.unpack(entityData.get(UPGRADES), getUpgradeSlotCount()) : upgradeSlots;
    }
    /** The fitted type in a slot, or -1 for an empty or out-of-range slot. */
    public int getUpgrade(int slot) {
        int[] slots = upgradeView();
        return slot >= 0 && slot < slots.length ? slots[slot] : GolemUpgrade.EMPTY;
    }
    public int getUpgradeAmount(GolemUpgrade kind) { return GolemUpgrades.count(upgradeView(), kind); }
    private void syncUpgrades() { entityData.set(UPGRADES, GolemUpgrades.pack(upgradeSlots)); }
    /** Original E:1148: 20 ticks, 18 for an advanced golem. */
    public int getAttackSpeed() { return GolemUpgrades.attackSpeed(isAdvanced()); }
    /** Ticks between two repairs: the material's delay, two thirds of it with a fez (original E:386-389). */
    public int getRegenDelay() {
        EnumGolemType type = EnumGolemType.getType(getGolemMaterial());
        return type == null ? 0 : GolemDecorations.regenDelay(type.regenDelay, getDecoration());
    }
    /** Material capacity plus min(16, max(4, capacity)) per Earth upgrade (original E:355-362). */
    public int getCarryLimit() {
        EnumGolemType type = EnumGolemType.getType(getGolemMaterial());
        return type == null ? 0 : GolemUpgrades.carry(type.carry, getUpgradeAmount(GolemUpgrade.EARTH));
    }
    /** Water in millibuckets: floor(sqrt(carry limit)) buckets (original E:364-366). */
    public int getFluidCarryLimit() { return GolemUpgrades.fluidLimitMb(getCarryLimit()); }
    /** Work radius {@code 16 + 4n} for n Water upgrades, +10% (at least 1) with spectacles, +20% (at least 2) when advanced (original E:417-427). */
    public float getRange() { return GolemUpgrades.range(getUpgradeAmount(GolemUpgrade.WATER), isAdvanced(), getDecoration()); }
    /** The original's {@code dmod * dmod}: float arithmetic, shared by every squared home/marker/target bound. */
    double rangeSqr() {
        float range = getRange();
        return range * range;
    }
    public BlockPos getHomePosition() { return entityData.get(HOME); }
    public Direction getHomeFacing() { return Direction.from3DDataValue(entityData.get(HOME_FACE)); }
    public String getOwnerName() { return entityData.get(OWNER); }
    public void setOwnerName(String name) {
        requireServerThread();
        if (!level().isClientSide && !busy && !useCustodyActive() && !hasUseRemainders() && !essentiaMutationActive()) entityData.set(OWNER, name == null ? "" : name);
    }
    public void setHome(BlockPos pos, Direction facing) {
        requireServerThread();
        if (level().isClientSide || pos == null || facing == null || retiring || unsupported || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive()) return;
        use.reset();
        alchemy.reset();
        liquid.reset();
        if (core == GolemData.HARVEST) harvest.reset();
        if (core == GolemData.LUMBER) lumber.reset();
        if (core == GolemData.GUARD || core == GolemData.BUTCHER) guard.reset();
        if (core == GolemData.FISHING) cancelFishingCast();
        hasHome = true;
        restrictTo(pos, 32);
        entityData.set(HOME, pos.immutable());
        entityData.set(HOME_FACE, facing.get3DDataValue());
        getNavigation().stop();
    }
    public ItemStack getCarriedItem() { return (level().isClientSide ? entityData.get(CARRIED) : carried).copy(); }
    public float getBootupTimer() { return entityData.get(BOOTUP); }
    public int getHealingTimer() { return entityData.get(HEALING); }
    // Original pose value, NOT the raw six-tick action countdown.
    public int getActionTimer() { return 3 - Math.abs(entityData.get(ACTION) - 3); }
    void startActionTimer() {
        requireServerThread();
        if (level() instanceof ServerLevel server && isAlive() && current() && entityData.get(ACTION) == 0) {
            entityData.set(ACTION, 6);
            server.broadcastEntityEvent(this, (byte) 4);
        }
    }
    // Fishing uses the independent raw right arm, not Guard/Harvest's shaped action.
    public int getRightArmTimer() { return entityData.get(RIGHT_ARM); }
    public int getLeftArmTimer() { return 0; }
    void startFishingArm() {
        requireServerThread();
        if (level() instanceof ServerLevel server && core == GolemData.FISHING && isAlive() && current() && entityData.get(RIGHT_ARM) == 0) {
            entityData.set(RIGHT_ARM, 5);
            server.broadcastEntityEvent(this, (byte) 8);
        }
    }
    private void startRightArmAction() {
        if (level() instanceof ServerLevel server && core == GolemData.USE && isAlive() && current() && entityData.get(RIGHT_ARM) == 0) {
            entityData.set(RIGHT_ARM, 5);
            server.broadcastEntityEvent(this, (byte) 8);
        }
    }
    void syncFishingBobber(EntityGolemBobber bobber) {
        requireServerThread();
        entityData.set(FISHING_BOBBER_UUID, bobber == null ? Optional.empty() : Optional.of(bobber.getUUID()));
        entityData.set(FISHING_BOBBER_ID, bobber == null ? -1 : bobber.getId());
    }
    public EntityGolemBobber getFishingBobber() {
        int id = entityData.get(FISHING_BOBBER_ID);
        Optional<UUID> uuid = entityData.get(FISHING_BOBBER_UUID);
        if (getGolemCore() != GolemData.FISHING || id < 0 || uuid.isEmpty() || !isAlive() || !current()) return null;
        Entity candidate = level().getEntity(id);
        if (!(candidate instanceof EntityGolemBobber bobber) || !uuid.get().equals(bobber.getUUID()) || bobber.level() != level()
                || !bobber.isAlive() || bobber.isRemoved() || !level().hasChunkAt(bobber.blockPosition()) || bobber.getFisher() != this) return null;
        if (level() instanceof ServerLevel server && (server.getEntity(getUUID()) != this || server.getEntity(uuid.get()) != bobber
                || !fishing.ownsBobber(bobber))) return null;
        return bobber;
    }
    public boolean hasFishingCast() { return getFishingBobber() != null; }
    /** Ghost slots in use: 6 + 6n for an item-filter core with n Fire upgrades, otherwise one six-slot page (see GolemData.slotCount). */
    public int getSlotCount() { return GolemData.slotCount(getGolemCore(), getUpgradeAmount(GolemUpgrade.FIRE)); }
    /** Guard target switches exist on a Guard core that carries an Order upgrade (original GuiGolem:167). */
    public boolean canSwitchGuardTargets() { return GolemGuardTargets.configurable(getGolemCore(), getUpgradeAmount(GolemUpgrade.ORDER)); }
    /** The server's authoritative mask; without Order it is always the default (hostile monsters only). */
    public int getGuardTargets() { return guardTargets; }
    /** Entropy's switches exist on Fill, Empty, Gather, Use and Sorting golems that carry an Entropy upgrade (original GuiGolem:214). */
    public boolean canSwitchItemMatch() { return GolemItemMatch.configurable(getGolemCore(), getUpgradeAmount(GolemUpgrade.ENTROPY)); }
    /** The three switches as the owner set them. */
    public int getItemMatch() { return itemMatch; }
    /** What the golem compares with: the switches, widened to tag-equivalents for Empty and Use whenever Entropy is fitted. */
    int itemMatchFlags() { return GolemItemMatch.effective(itemMatch, core, GolemUpgrades.count(upgradeSlots, GolemUpgrade.ENTROPY)); }
    public boolean setItemMatch(int flags) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive()
                || !canSwitchItemMatch() || !GolemItemMatch.valid(flags)) return false;
        if (itemMatch == flags) return true;
        itemMatch = flags;
        use.reset();
        inventories.configurationChanged();
        return true;
    }
    /** Flips one switch (GolemItemMatch.TAGS ... IGNORE_COMPONENTS); false when the switch does not exist. */
    public boolean toggleItemMatch(int flag) {
        int next = GolemItemMatch.toggle(itemMatch, flag);
        return next != itemMatch && setItemMatch(next);
    }
    public boolean setGuardTargets(int mask) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive()
                || !canSwitchGuardTargets() || !GolemGuardTargets.valid(mask)) return false;
        if (guardTargets == mask) return true;
        guardTargets = mask;
        guard.reset(); // A target chosen under the old switches may be forbidden now.
        return true;
    }
    /** Flips one category (GolemGuardTargets.MONSTERS ... CREEPERS); false when the switch does not exist. */
    public boolean toggleGuardTarget(int category) {
        int next = GolemGuardTargets.toggle(guardTargets, category);
        return next != guardTargets && setGuardTargets(next);
    }
    /** Order slot colours exist only on Fill, Empty and Use golems that carry an Order upgrade. */
    public boolean canColorSlots() { return GolemData.hasSlotColors(getGolemCore()) && getUpgradeAmount(GolemUpgrade.ORDER) > 0; }
    public int getSlotColor(int slot) { return slot >= 0 && slot < getSlotCount() ? entityData.get(COLORS.get(slot)) : GolemData.NO_COLOR; }
    public boolean setSlotColor(int slot, int color) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive()
                || !canColorSlots() || slot < 0 || slot >= getSlotCount() || color < GolemData.NO_COLOR || color > GolemMarker.LAST_COLOR) return false;
        if (entityData.get(COLORS.get(slot)) == color) return true;
        entityData.set(COLORS.get(slot), color);
        use.reset();
        inventories.configurationChanged();
        return true;
    }
    /**
     * Original getColorsMatching: the colours of the ghost slots that match {@code stack} (an empty hand matches the empty
     * slots); when no slot holds an exemplar at all, every slot's colour. Cores without colourable slots, and every golem
     * without Order, answer a single "any" so ordinary routing is unchanged. Matching follows Entropy's switches (exact without them).
     */
    public List<Integer> colorsMatching(ItemStack stack) {
        if (!canColorSlots()) return List.of(GolemData.NO_COLOR);
        int slots = getSlotCount();
        boolean anyFilter = false;
        List<Integer> matching = new ArrayList<>();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack exemplar = entityData.get(FILTERS.get(slot));
            if (!exemplar.isEmpty()) anyFilter = true;
            boolean same = stack == null || stack.isEmpty() ? exemplar.isEmpty() : !exemplar.isEmpty() && GolemItemMatch.matches(stack, exemplar, itemMatch);
            if (same) matching.add(entityData.get(COLORS.get(slot)));
        }
        if (!anyFilter) {
            matching.clear();
            for (int slot = 0; slot < slots; slot++) matching.add(entityData.get(COLORS.get(slot)));
        }
        return matching;
    }
    /** Whether a marker may serve {@code stack}: some matching slot colour is "any" or the marker's own colour. */
    public boolean markerAccepts(GolemMarker marker, ItemStack stack) {
        if (marker == null) return false;
        for (int color : colorsMatching(stack)) if (GolemMarker.accepts(color, marker.color())) return true;
        return false;
    }
    public ItemStack getFilter(int slot) { return slot >= 0 && slot < getSlotCount() ? entityData.get(FILTERS.get(slot)).copy() : ItemStack.EMPTY; }
    public void setFilter(int slot, ItemStack exemplar) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive() || requiresEmptyControls(getGolemCore())
                || slot < 0 || slot >= getSlotCount() || exemplar == null) return;
        if (core == GolemData.LIQUID) {
            if (slot != 0 || !GolemData.isSupportedWaterFilter(exemplar)) return;
            busy = true;
            try {
                liquid.reset();
                entityData.set(WANTED.get(0), 0);
                entityData.set(FILTERS.get(0), exemplar.copy());
            } finally { busy = false; }
            return;
        }
        ItemStack old = entityData.get(FILTERS.get(slot));
        int wanted = exemplar.isEmpty() ? 0 : core != GolemData.USE && !old.isEmpty() && ItemStack.isSameItemSameComponents(old, exemplar)
                ? Math.max(1, getWantedCount(slot)) : 1;
        entityData.set(FILTERS.get(slot), exemplar.isEmpty() ? ItemStack.EMPTY : exemplar.copyWithCount(1));
        entityData.set(WANTED.get(slot), wanted);
        if (core == GolemData.USE) use.reset();
        inventories.configurationChanged();
    }
    public int getWantedCount(int slot) { return slot >= 0 && slot < getSlotCount() ? entityData.get(WANTED.get(slot)) : 0; }
    public void setWantedCount(int slot, int wanted) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive() || core == GolemData.LIQUID || core == GolemData.USE
                || requiresEmptyControls(getGolemCore()) || slot < 0 || slot >= getSlotCount()) return;
        if (wanted <= 0) {
            entityData.set(FILTERS.get(slot), ItemStack.EMPTY);
            entityData.set(WANTED.get(slot), 0);
        } else if (!entityData.get(FILTERS.get(slot)).isEmpty()) entityData.set(WANTED.get(slot), Math.min(256, wanted));
        inventories.configurationChanged();
    }
    public boolean isFillAnyAmount() { return entityData.get(FILL_ANY); }
    public void setFillAnyAmount(boolean any) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive()
                || core == GolemData.LIQUID || core == GolemData.USE || requiresEmptyControls(getGolemCore())) return;
        entityData.set(FILL_ANY, any);
        inventories.configurationChanged();
    }
    private static List<GolemMarker> copyMarkers(List<GolemMarker> source) {
        if (source == null || source.size() > 64) throw new IllegalArgumentException("At most 64 golem markers are supported");
        List<GolemMarker> result = new ArrayList<>(source.size());
        for (GolemMarker marker : source) {
            if (marker == null || marker.dimension() == null || marker.pos() == null || marker.face() == null) {
                throw new IllegalArgumentException("A marker requires a dimension, position and inventory face");
            }
            result.add(new GolemMarker(marker.dimension(), marker.pos().immutable(), marker.face(), marker.color()));
        }
        return List.copyOf(result);
    }
    public List<GolemMarker> getMarkers() { return copyMarkers(markers); }
    public void setMarkers(List<GolemMarker> value) {
        requireServerThread();
        if (level().isClientSide || unsupported || retiring || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive()) return;
        List<GolemMarker> copied = copyMarkers(value);
        // A colour is an Order rule: a golem without the upgrade keeps only uncoloured markers.
        if (getUpgradeAmount(GolemUpgrade.ORDER) == 0 && copied.stream().anyMatch(marker -> marker.color() != GolemMarker.ANY)) return;
        use.reset();
        alchemy.reset();
        liquid.reset();
        markers = copied; // Other dimensions persist, but never authorize work here.
        inventories.configurationChanged();
    }
    private boolean baselineEmptyControls() {
        if (isFillAnyAmount()) return false;
        for (int slot = 0; slot < FILTERS.size(); slot++) {
            if (!entityData.get(FILTERS.get(slot)).isEmpty() || entityData.get(WANTED.get(slot)) != 0) return false;
        }
        return true;
    }
    private boolean liquidControls() {
        if (isFillAnyAmount() || !GolemData.isSupportedWaterFilter(entityData.get(FILTERS.get(0)))) return false;
        for (int slot = 0; slot < FILTERS.size(); slot++) {
            if (entityData.get(WANTED.get(slot)) != 0 || slot > 0 && !entityData.get(FILTERS.get(slot)).isEmpty()) return false;
        }
        return true;
    }
    private boolean useWhitelistControls() {
        if (isFillAnyAmount()) return false;
        for (int slot = 0; slot < FILTERS.size(); slot++) {
            ItemStack filter = entityData.get(FILTERS.get(slot));
            int wanted = entityData.get(WANTED.get(slot));
            if (filter.isEmpty() ? wanted != 0 : filter.getCount() != 1 || wanted != 1) return false;
        }
        return true;
    }
    private boolean useControls() { return useWhitelistControls() && useOptions.supportsDefaultBaseline(); }
    public GolemUseOptions getUseOptions() { return useOptions; }
    public boolean setUseOptions(GolemUseOptions options) {
        requireServerThread();
        if (!(level() instanceof ServerLevel) || core != GolemData.USE || options == null || !options.supportsDefaultBaseline()
                || busy || useCustodyActive() || hasUseRemainders() || essentiaMutationActive() || !hasConfigurationMenu()) return false;
        use.reset();
        useOptions = options;
        return true;
    }
    public GolemData configuredData() {
        if (!EnumGolemType.isSupported(getGolemMaterial()) || getGolemCore() != GolemData.NO_CORE && !hasSupportedCore()
                || unsupported || unreadCarried != null || unreadWater != null || unreadAspect != null || fishing.hasUnreadPending()
                || hasUseRemainders() || useCustodyActive() || getGolemCore() == GolemData.USE && !useControls()
                || getGolemCore() != GolemData.USE && !useOptions.isDefault()
                || requiresEmptyControls(getGolemCore()) && !baselineEmptyControls()
                || getGolemCore() == GolemData.LIQUID && !liquidControls() || waterMb > 0 && getGolemCore() != GolemData.LIQUID
                || aspectCargo.amount() > 0 && getGolemCore() != GolemData.ALCHEMY) {
            throw new IllegalStateException("Unsupported golem configuration cannot be flattened into a material placer");
        }
        int slots = getSlotCount();
        return new GolemData(getGolemCore(), FILTERS.subList(0, slots).stream().map(filter -> entityData.get(filter).copy()).toList(),
                WANTED.subList(0, slots).stream().map(entityData::get).toList(), isFillAnyAmount(), getMarkers(), getUseOptions(),
                GolemUpgrades.toList(upgradeSlots), COLORS.subList(0, slots).stream().map(entityData::get).toList(), guardTargets, advanced, decoration, itemMatch);
    }
    public void applyConfiguration(GolemData data) {
        requireServerThread();
        if (level().isClientSide) return;
        if (!EnumGolemType.isSupported(material) || data == null
                || data.core() != GolemData.NO_CORE && !activeCore(data.core())) {
            throw new IllegalArgumentException("Unsupported ordinary golem material/core");
        }
        if (tickCount != 0 || current() || isRemoved() || busy || essentiaMutationActive() || !carried.isEmpty() || waterMb != 0 || unreadWater != null
                || aspectCargo.amount() != 0 || unreadAspect != null
                || fishing.hasPending() || hasUseRemainders() || useCustodyActive() || retiring || unsupported) {
            throw new IllegalStateException("Configuration can only be applied to a fresh golem");
        }
        List<ItemStack> filters = data.filters();
        List<Integer> wanted = data.wantedCounts();
        List<GolemMarker> configuredMarkers = copyMarkers(data.markers());
        int slots = filters.size();
        if (wanted.size() != slots || slots != GolemData.slotCount(data.core(), GolemData.fireCount(data.upgrades()))) {
            throw new IllegalArgumentException("The ghost filters and wanted counts must match the core and its Fire upgrades");
        }
        for (int slot = 0; slot < slots; slot++) {
            if (wanted.get(slot) == null || (filters.get(slot).isEmpty() || data.core() == GolemData.LIQUID ? wanted.get(slot) != 0
                    : wanted.get(slot) < 1 || wanted.get(slot) > 256)) throw new IllegalArgumentException("Invalid ghost wanted quantity");
            if (data.core() == GolemData.LIQUID && (slot == 0 ? !GolemData.isSupportedWaterFilter(filters.get(slot)) : !filters.get(slot).isEmpty())) {
                throw new IllegalArgumentException("Decanting permits only one pristine water-bucket exemplar");
            }
            if (requiresEmptyControls(data.core()) && (!filters.get(slot).isEmpty() || wanted.get(slot) != 0)) {
                throw new IllegalArgumentException("Ordinary Harvest/Guard/Alchemy/Butcher/Sorting/Fishing have no ghost filters or wanted quantities");
            }
            if (data.core() == GolemData.USE && (filters.get(slot).isEmpty() ? wanted.get(slot) != 0
                    : filters.get(slot).getCount() != 1 || wanted.get(slot) != 1)) throw new IllegalArgumentException("Use has six one-count whitelist ghosts only");
        }
        if (requiresEmptyControls(data.core()) && data.fillAnyAmount()) throw new IllegalArgumentException("Ordinary Harvest/Guard/Alchemy/Butcher/Sorting/Fishing have no Fill mode");
        if (data.core() == GolemData.LIQUID && data.fillAnyAmount()) throw new IllegalArgumentException("Decanting has no Fill mode");
        if (data.core() == GolemData.USE ? data.fillAnyAmount() || !data.useOptions().supportsDefaultBaseline()
                : !data.useOptions().isDefault()) throw new IllegalArgumentException("Unsupported Use options/baseline");
        // Never truncated like the original placer path: more upgrades than slots is refused outright.
        int[] fittedUpgrades = GolemUpgrades.fit(data.upgrades(), GolemUpgrades.slotCount(EnumGolemType.getType(material).upgrades, data.advanced()));
        if (fittedUpgrades == null) throw new IllegalArgumentException("More golem upgrades than this material has slots, or an unsupported upgrade");
        core = data.core();
        useOptions = data.useOptions();
        advanced = data.advanced();
        entityData.set(ADVANCED, advanced);
        decoration = data.decoration();
        entityData.set(DECORATION, decoration);
        itemMatch = data.itemMatch(); // GolemData already refused non-zero switches without an Entropy upgrade on a core that has them.
        upgradeSlots = fittedUpgrades;
        syncUpgrades();
        applyMaterialAttributes(EnumGolemType.getType(material));
        setHealth(getMaxHealth()); // A fresh placement starts at full health, including a top hat's five.
        for (int slot = 0; slot < slots; slot++) {
            if (core == GolemData.LIQUID || core == GolemData.USE) entityData.set(FILTERS.get(slot), filters.get(slot).copy());
            else if (requiresEmptyControls(core)) entityData.set(FILTERS.get(slot), ItemStack.EMPTY); // Validated empty, not payload normalization.
            else setFilter(slot, filters.get(slot));
            entityData.set(WANTED.get(slot), wanted.get(slot));
        }
        for (int slot = 0; slot < COLORS.size(); slot++) {
            entityData.set(COLORS.get(slot), slot < slots ? data.colors().get(slot) : GolemData.NO_COLOR);
        }
        entityData.set(FILL_ANY, data.fillAnyAmount());
        markers = configuredMarkers;
        guardTargets = data.guardTargets(); // GolemData already refused a non-default mask without a Guard core and Order.
        entityData.set(CORE, core);
        rebuildGoals();
    }

    private void requireServerThread() {
        if (level() instanceof ServerLevel server && !server.getServer().isSameThread()) {
            throw new IllegalStateException("Golem mutations require the server thread");
        }
    }
    private boolean current() {
        return !isRemoved() && level().hasChunkAt(blockPosition()) && level().getEntity(getId()) == this;
    }
    public boolean canConfigure(Player player) {
        return canEditLinks(player) && distanceToSqr(player) <= 64;
    }
    /** A previously selected loaded worker can receive bell links remotely;
     * invalid/retiring state is still refused, without imposing a second entity ray. */
    public boolean canEditLinks(Player player) {
        return player != null && !ItemGolemPlacer.isPlacementInProgress()
                && !unsupported && !retiring && unreadCarried == null && unreadWater == null && unreadAspect == null && !fishing.hasUnreadPending()
                && unreadUseRemainders == null && deferredUseRemoval == null && !useCustodyActive()
                && (!(player instanceof ServerPlayer actor) || !GolemUsePlatform.isActorBorrowed(actor))
                && EnumGolemType.isSupported(getGolemMaterial()) && isAlive() && current()
                && player.isAlive() && player.level() == level();
    }
    private boolean canInteract(Player player, ItemStack held) {
        return canConfigure(player) && !player.isSpectator() && player.canInteractWithEntity(this, 0)
                && level().mayInteract(player, blockPosition()) && player.mayUseItemAt(blockPosition(), Direction.UP, held);
    }
    public void startConfiguration(Player player) {
        requireServerThread();
        if (level().isClientSide || busy || essentiaMutationActive() || !canConfigure(player) || !hasConfigurationMenu()) return;
        if (core == GolemData.LIQUID) liquid.reset();
        if (core == GolemData.USE) use.reset();
        configurationOpeners.add(player);
        if (core == GolemData.GUARD || core == GolemData.BUTCHER) guard.reset();
        if (core == GolemData.FISHING) cancelFishingCast();
        getNavigation().stop();
    }
    public void stopConfiguration(Player player) {
        requireServerThread();
        configurationOpeners.remove(player);
    }
    boolean canWork() {
        return !level().isClientSide && !ItemGolemPlacer.isPlacementInProgress()
                && isAlive() && current() && hasHome && EnumGolemType.isSupported(material) && !unsupported && !retiring
                && unreadCarried == null && unreadWater == null && unreadAspect == null
                && !hasUseRemainders()
                && (!useWorkScope || useInvoking)
                && !fishing.hasPending() && hasSupportedCore() && configurationOpeners.isEmpty()
                && (core != GolemData.LIQUID || liquidControls()) && (core != GolemData.USE || useControls());
    }
    void addWorkGoal(int priority, Goal goal) { goalSelector.addGoal(priority, goal); }
    void addTargetGoal(int priority, Goal goal) { targetSelector.addGoal(priority, goal); }
    private void rebuildGoals() {
        use.reset();
        alchemy.reset();
        liquid.reset();
        cancelFishingCast();
        fishing.resetRuntime();
        guard.reset();
        harvest.reset();
        lumber.reset();
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        work.reset();
        inventories.reset();
        getNavigation().stop();
        boolean enabled = EnumGolemType.isSupported(material) && !unsupported && hasSupportedCore();
        if (getNavigation() instanceof GroundPathNavigation ground) ground.setCanOpenDoors(enabled);
        if (enabled) {
            work.install();
            if (core == GolemData.HARVEST) harvest.install();
            else if (core == GolemData.LUMBER) lumber.install();
            else if (core == GolemData.GUARD || core == GolemData.BUTCHER) guard.install();
            else if (core == GolemData.FISHING) fishing.install();
            else if (core == GolemData.LIQUID) liquid.install();
            else if (core == GolemData.ALCHEMY) alchemy.install();
            else if (core == GolemData.USE) use.install();
            else if (inventoryCore(core) && core != GolemData.GATHER) inventories.install();
        }
    }
    boolean hasInventoryRoute() {
        // Shared ReturnHome also yields to combat, farming, a live cast and liquid intent.
        if (core == GolemData.GUARD || core == GolemData.BUTCHER) return guard.hasCombatRoute();
        if (core == GolemData.FISHING) return fishing.hasCast();
        if (core == GolemData.LIQUID) return liquid.hasRoute();
        if (core == GolemData.ALCHEMY) return alchemy.hasRoute();
        if (core == GolemData.USE) return use.hasRoute();
        if (core == GolemData.LUMBER) return lumber.hasTarget();
        return core == GolemData.HARVEST ? harvest.hasTarget() : inventoryCore(core) && core != GolemData.GATHER && inventories.hasRoute();
    }
    boolean hasCargo() { return !carried.isEmpty(); }
    int carrySpace() {
        int limit = getCarryLimit();
        return carried.isEmpty() ? limit : Math.max(0, Math.min(limit, carried.getMaxStackSize()) - carried.getCount());
    }
    double distanceToHomeSqr() {
        BlockPos home = getHomePosition();
        return distanceToSqr(home.getX() + .5, home.getY() + .5, home.getZ() + .5);
    }
    /** Guard the native pathfinder's local region as well as its target against chunk loads. */
    boolean loadedNavigationArea(BlockPos target) {
        if (!level().hasChunkAt(target)) return false;
        int minX = Math.min(blockPosition().getX(), target.getX()) - 48;
        int maxX = Math.max(blockPosition().getX(), target.getX()) + 48;
        int minZ = Math.min(blockPosition().getZ(), target.getZ()) - 48;
        int maxZ = Math.max(blockPosition().getZ(), target.getZ()) + 48;
        for (int x = Math.floorDiv(minX, 16); x <= Math.floorDiv(maxX, 16); x++) {
            for (int z = Math.floorDiv(minZ, 16); z <= Math.floorDiv(maxZ, 16); z++) {
                if (!level().hasChunk(x, z)) return false;
            }
        }
        return true;
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        settleUseCustody();
        if (busy) return;
        configurationOpeners.removeIf(player -> !canConfigure(player));
        if (level() instanceof ServerLevel server) {
            if (core == GolemData.FISHING) {
                busy = true;
                try { fishing.serverTick(server); }
                finally { busy = false; }
            } else if (fishing.hasPending()) evacuateFishingCatch();
        }
        if (retiring || !isAlive()) { use.reset(); alchemy.reset(); liquid.reset(); cancelFishingCast(); guard.reset(); harvest.reset(); lumber.reset(); return; }
        if (canWork()) {
            work.serverTick();
            if (core == GolemData.HARVEST) harvest.serverTick();
            else if (core == GolemData.LUMBER) lumber.serverTick();
            else if (core == GolemData.GUARD || core == GolemData.BUTCHER) guard.serverTick();
            else if (core == GolemData.LIQUID) liquid.serverTick();
            else if (core == GolemData.ALCHEMY) alchemy.serverTick();
            else if (inventoryCore(core) && core != GolemData.GATHER) inventories.serverTick();
        } else {
            use.reset();
            alchemy.reset();
            liquid.reset();
            cancelFishingCast();
            guard.reset();
            harvest.reset();
            lumber.reset();
            getNavigation().stop();
            getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
            setZza(0);
            setXxa(0);
        }
    }
    @Override public void aiStep() {
        super.aiStep();
        settleUseCustody();
        if (isNoAi() && core == GolemData.USE) use.reset();
        if (isNoAi() && (core == GolemData.GUARD || core == GolemData.BUTCHER)) guard.reset();
        if (!isAlive() || retiring) { use.reset(); alchemy.reset(); liquid.reset(); cancelFishingCast(); guard.reset(); harvest.reset(); lumber.reset(); }
        if (!(level() instanceof ServerLevel server) || !isAlive() || !current() || retiring) return;
        requireServerThread();
        int action = entityData.get(ACTION);
        if (action > 0) entityData.set(ACTION, action - 1);
        int arm = entityData.get(RIGHT_ARM);
        if (arm > 0) entityData.set(RIGHT_ARM, arm - 1);
        int healing = entityData.get(HEALING);
        if (healing > 0) entityData.set(HEALING, healing - 1);
        float boot = entityData.get(BOOTUP);
        if (boot > 0) entityData.set(BOOTUP, boot < .001F ? 0 : boot * boot / 33.1F);
        if (EnumGolemType.isSupported(material) && !unsupported) {
            if (regenTimer > 0) --regenTimer;
            else {
                regenTimer = getRegenDelay();
                if (getHealth() < getMaxHealth()) {
                    heal(1);
                    entityData.set(HEALING, 5);
                    server.broadcastEntityEvent(this, (byte) 5);
                }
            }
        }
        BlockPos home = getHomePosition();
        if (isAlive() && current() && hasHome && !busy && !unsupported
                && (distanceToSqr(home.getX(), home.getY(), home.getZ()) >= 2304 || isInWall())) trySafeHomeReturn(server);
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 4) entityData.set(ACTION, 6);
        else if (event == 5) entityData.set(HEALING, 5);
        else if (event == 7) entityData.set(BOOTUP, 33F);
        else if (event == 8) entityData.set(RIGHT_ARM, 5);
        else super.handleEntityEvent(event);
    }
    @Override public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && (useCustodyActive() || core == GolemData.USE && droppingCargo)) {
            rememberUseRemoval(reason);
            return; // Actual actor hand/returns must reconcile BEFORE one native terminal request.
        }
        if (!level().isClientSide && (aspectWorkScope || essentiaMutationActive())) {
            deferredAspectRemoval = reason;
            return; // Exactly one native removal is finished after paired changes AND notifications.
        }
        if (!level().isClientSide && (waterSourceIntent != null || waterHomeIntent != null)) {
            // Finish this exact raw handoff, then honor native terminal/unload
            // handling once. Refusing it forever would strand the actual owner.
            deferredWaterRemoval = reason;
            return;
        }
        boolean saving = reason.shouldSave() || reason == Entity.RemovalReason.CHANGED_DIMENSION
                || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER;
        if (!level().isClientSide && !saving && unreadUseRemainders != null) return;
        if (!level().isClientSide && !saving && (core == GolemData.USE || !pendingUseExtras.isEmpty())
                && level() instanceof ServerLevel server && !queueDeathCargo(server)) {
            rememberUseRemoval(reason);
            return;
        }
        if (!level().isClientSide && !saving && (unreadWater != null || unsupported && waterMb > 0)) return;
        if (!level().isClientSide && !saving && (unreadAspect != null || unsupported && aspectCargo.amount() > 0)) return;
        if (alchemy != null) alchemy.reset();
        if (use != null) use.reset();
        if (liquid != null) liquid.reset();
        if (fishing != null) {
            cancelFishingCast();
            // Native unload/transfer may already hold a serialized entity image.
            // Publication belongs BEFORE that save, never after a pending row was copied.
            boolean serialized = reason.shouldSave() || reason == Entity.RemovalReason.CHANGED_DIMENSION
                    || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER;
            if (!serialized && !evacuateFishingCatch()) return;
        }
        try {
            if (guard != null) guard.reset();
            if (harvest != null) harvest.reset();
            if (lumber != null) lumber.reset();
        } finally { deferredUseRemoval = null; super.remove(reason); }
    }
    void tickFishingCast() {
        if (busy || !(level() instanceof ServerLevel server) || !server.getServer().isSameThread()) return;
        busy = true;
        try { fishing.activeTick(server); }
        finally { busy = false; }
    }
    void cancelFishingCast() {
        if (fishing == null) return;
        boolean previous = busy;
        busy = true;
        try { fishing.cancelCast(); }
        finally { busy = previous; }
    }
    private boolean evacuateFishingCatch() {
        if (fishing == null || !fishing.hasPending()) return true;
        if (!(level() instanceof ServerLevel server) || !server.getServer().isSameThread()) return false;
        boolean previous = busy;
        busy = true;
        try {
            if (!fishing.publishPending(server)) return false;
            fishing.cancelCast();
            return true;
        } finally { busy = previous; }
    }
    private void trySafeHomeReturn(ServerLevel server) {
        BlockPos home = getHomePosition();
        for (int dy = 1; dy >= -1; dy--) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos feet = home.offset(dx, dy, dz);
            double x = feet.getX() + .5, y = feet.getY(), z = feet.getZ() + .5;
            AABB bounds = getBoundingBox().move(x - getX(), y - getY(), z - getZ());
            if (!server.hasChunkAt(feet.below()) || server.isOutsideBuildHeight(feet)
                    || !server.getWorldBorder().isWithinBounds(bounds)) continue;
            boolean loaded = true;
            for (BlockPos cell : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
                if (!server.hasChunkAt(cell) || server.isOutsideBuildHeight(cell)) { loaded = false; break; }
            }
            if (!loaded || !server.getBlockState(feet.below()).isFaceSturdy(server, feet.below(), Direction.UP)
                    || server.containsAnyLiquid(bounds) || !server.noCollision(this, bounds)
                    || !server.getEntities(this, bounds, Entity::isAlive).isEmpty()) continue;
            moveTo(x, y, z, getYRot(), getXRot());
            fallDistance = 0;
            getNavigation().stop();
            return;
        }
    }

    /**
     * Policy shared by nearest, self-retaliation, chase and every strike (original isValidTarget, E:1150-1180). Hostile
     * monsters other than creepers are always eligible; creepers, animals and players need an Order upgrade AND their
     * switch ({@link GolemGuardTargets}), and every switch but Monsters starts off.
     */
    public boolean isGuardTarget(LivingEntity target) {
        if (core != GolemData.GUARD || !(level() instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !canWork() || server.getEntity(getUUID()) != this || target == null || target == this
                || target instanceof AbstractGolem || target instanceof AbstractVillager || target instanceof Bat
                || !guardCategoryAllowed(target, server)
                || !target.isAlive() || target.isRemoved() || target.level() != server
                || !server.hasChunkAt(target.blockPosition()) || server.getEntity(target.getId()) != target
                || server.getEntity(target.getUUID()) != target || isAlliedTo(target) || target.isAlliedTo(this)
                || target instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        BlockPos pos = target.blockPosition(); // Floored target FEET, not an actor-range sphere or home center.
        BlockPos home = getHomePosition();
        double dx = (double) pos.getX() - home.getX();
        double dy = (double) pos.getY() - home.getY();
        double dz = (double) pos.getZ() - home.getZ();
        return dx * dx + dy * dy + dz * dz < rangeSqr(); // Original func_110176_b: strictly inside getRange() of the home.
    }
    /**
     * The category gate of {@link #isGuardTarget}. Animals are the native analogue of the original EntityAnimal/IAnimals
     * (land {@link Animal} and {@link WaterAnimal}); tamed and owned pets, golems, villagers and bats are excluded by
     * {@link #isGuardTarget} itself. A player is never the
     * owner, never creative or spectating (such a target could not be hurt, and the golem would chase it forever), and is
     * only attackable while the server allows PvP.
     */
    private boolean guardCategoryAllowed(LivingEntity target, ServerLevel server) {
        boolean order = getUpgradeAmount(GolemUpgrade.ORDER) > 0;
        if (target instanceof Creeper) return order && GolemGuardTargets.allows(guardTargets, GolemGuardTargets.CREEPERS);
        if (target instanceof Enemy) return GolemGuardTargets.allows(guardTargets, GolemGuardTargets.MONSTERS);
        if (target instanceof Player player) {
            return order && GolemGuardTargets.allows(guardTargets, GolemGuardTargets.PLAYERS) && server.getServer().isPvpAllowed()
                    && !player.isCreative() && !player.isSpectator() && !player.getGameProfile().getName().equals(getOwnerName());
        }
        if (target instanceof Animal || target instanceof WaterAnimal) return order && GolemGuardTargets.allows(guardTargets, GolemGuardTargets.ANIMALS);
        return false;
    }
    /** Explicit core dispatch; Guard's hostile policy above is deliberately unchanged. */
    boolean isCombatTarget(LivingEntity target) {
        return switch (core) {
            case GolemData.GUARD -> isGuardTarget(target);
            case GolemData.BUTCHER -> hasButcherSurplus(target);
            default -> false;
        };
    }
    private boolean butcherWorker(ServerLevel server) {
        return level() == server && server.getServer().isSameThread() && core == GolemData.BUTCHER && !isNoAi()
                && canWork() && baselineEmptyControls() && server.getEntity(getUUID()) == this;
    }
    private static boolean nativeButcherAnimal(LivingEntity target) {
        Class<?> type = target.getClass();
        return type == Cow.class && target.getType() == EntityType.COW
                || type == Pig.class && target.getType() == EntityType.PIG
                || type == Sheep.class && target.getType() == EntityType.SHEEP
                || type == Chicken.class && target.getType() == EntityType.CHICKEN
                || type == MushroomCow.class && target.getType() == EntityType.MOOSHROOM;
    }
    /** Read-only eligibility, also repeated AFTER every permission query and just before hurt. */
    private boolean butcherState(LivingEntity target, ServerLevel server) {
        if (!butcherWorker(server) || target == null || target == this || !nativeButcherAnimal(target)
                || !(target instanceof Animal animal) || animal.isBaby() || animal.hasCustomName()
                || animal.getLeashData() != null || animal.isNoAi() || animal.isInvulnerable()
                || !animal.isAlive() || animal.isRemoved() || animal.level() != server
                || animal.getTeam() != null || isAlliedTo(animal) || animal.isAlliedTo(this)
                || animal instanceof TamableAnimal tame && tame.isTame()
                || animal instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        BlockPos pos = animal.blockPosition();
        if (!server.hasChunkAt(pos) || server.isOutsideBuildHeight(pos) || !server.getWorldBorder().isWithinBounds(pos)
                || server.getEntity(animal.getId()) != animal || server.getEntity(animal.getUUID()) != animal
                || !getBoundingBox().inflate(getRange(), 4, getRange()).intersects(animal.getBoundingBox())) return false;
        BlockPos home = getHomePosition(); // Floored feet to INTEGER home, strictly inside the live getRange() sphere.
        double dx = (double) pos.getX() - home.getX(), dy = (double) pos.getY() - home.getY(), dz = (double) pos.getZ() - home.getZ();
        return dx * dx + dy * dy + dz * dz < rangeSqr();
    }
    /** Nonbreaking native position permission only; preserve busy when called INSIDE a strike. */
    boolean canUseButcherPosition(BlockPos pos) {
        if (checkingButcherPosition || !(level() instanceof ServerLevel server) || !butcherWorker(server) || pos == null
                || server.isOutsideBuildHeight(pos) || !server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos)) return false;
        boolean previous = busy;
        busy = true;
        checkingButcherPosition = true;
        try {
            return GolemHarvestPlatform.mayInteract(this, pos) && butcherWorker(server) && server.hasChunkAt(pos)
                    && !server.isOutsideBuildHeight(pos) && server.getWorldBorder().isWithinBounds(pos);
        } catch (RuntimeException denied) {
            Thaumcraft.LOG.warn("Golem Butcher position query failed at {}; refusing eligibility", pos, denied);
            return false;
        } finally { checkingButcherPosition = false; busy = previous; }
    }
    // Ephemeral members of ONE query, never a saved population, hunt entitlement or kill reservation.
    private record ButcherMember(LivingEntity animal, BlockPos position) {}
    private ButcherMember queryButcherMember(LivingEntity target, ServerLevel server) {
        if (!butcherState(target, server)) return null;
        BlockPos position = target.blockPosition().immutable();
        return canUseButcherPosition(position) && butcherState(target, server) && position.equals(target.blockPosition())
                ? new ButcherMember(target, position) : null;
    }
    public boolean isButcherCandidate(LivingEntity target) {
        if (checkingButcherPosition || checkingButcherPopulation || !(level() instanceof ServerLevel server)
                || !server.getServer().isSameThread()) return false;
        boolean previous = busy;
        busy = true;
        try { return queryButcherMember(target, server) != null; }
        catch (RuntimeException invalid) {
            Thaumcraft.LOG.warn("Golem Butcher candidate query failed; refusing eligibility", invalid);
            return false;
        } finally { busy = previous; }
    }
    private boolean butcherMemberCurrent(ServerLevel server, LivingEntity target, ButcherMember member) {
        return member.animal().getClass() == target.getClass() && member.position().equals(member.animal().blockPosition())
                && butcherState(member.animal(), server);
    }
    private boolean butcherSurplusCurrent(ServerLevel server, LivingEntity target, List<ButcherMember> members) {
        if (!butcherState(target, server)) return false;
        int count = 0;
        boolean includesTarget = false;
        for (ButcherMember member : members) if (butcherMemberCurrent(server, target, member)) {
            ++count;
            if (member.animal() == target) includesTarget = true;
        }
        return includesTarget && count > 2;
    }
    private List<ButcherMember> checkedButcherSurplus(ServerLevel server, LivingEntity target) {
        if (checkingButcherPosition || checkingButcherPopulation || !server.getServer().isSameThread()) return List.of();
        boolean previous = busy;
        busy = true;
        checkingButcherPopulation = true;
        try {
            ButcherMember victim = queryButcherMember(target, server);
            if (victim == null) return List.of();
            List<ButcherMember> permitted = new ArrayList<>();
            permitted.add(victim);
            Set<LivingEntity> seen = new HashSet<>();
            seen.add(target);
            // No effectful permission predicates inside the live native entity-section traversal.
            List<LivingEntity> nearby = server.getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(getRange(), 4, getRange()), animal -> animal.getClass() == target.getClass());
            for (LivingEntity animal : nearby) {
                if (!seen.add(animal)) continue;
                ButcherMember member = queryButcherMember(animal, server);
                if (member != null) permitted.add(member);
                if (!butcherMemberCurrent(server, target, victim)) return List.of();
                // Three still-eligible permitted exact-class members suffice, including the victim.
                // Recheck earlier members after the newest query's native callbacks.
                if (butcherSurplusCurrent(server, target, permitted)) return permitted;
            }
            return List.of();
        } catch (RuntimeException invalid) {
            Thaumcraft.LOG.warn("Golem Butcher surplus query failed; refusing hunt", invalid);
            return List.of();
        } finally { checkingButcherPopulation = false; busy = previous; }
    }
    public boolean hasButcherSurplus(LivingEntity target) {
        if (!(level() instanceof ServerLevel server) || !server.getServer().isSameThread()) return false;
        return butcherSurplusCurrent(server, target, checkedButcherSurplus(server, target));
    }
    boolean isWithinGuardReach(LivingEntity target) {
        float width = target.getBbWidth();
        double reach = (double) (width * 2.0F * width * 2.0F) + 1.0;
        return distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) <= reach;
    }
    private boolean hasGuardEquipment() {
        for (ItemStack equipped : getAllSlots()) if (!equipped.isEmpty()) return true;
        return false;
    }
    @Override public boolean doHurtTarget(Entity other) {
        if (busy || !(other instanceof LivingEntity victim) || !(level() instanceof ServerLevel server)
                || !server.getServer().isSameThread() || !guard.ownsAttempt(victim)) return false;
        if (!isCombatTarget(victim)) {
            if (core == GolemData.BUTCHER) guard.reset();
            return false;
        }
        busy = true;
        try {
            // The private goal has already paid its 20-active-update cooldown.
            // Swing before native hurt even when cancellation/invulnerability rejects it.
            startActionTimer();
            if (core == GolemData.GUARD) {
                if (!guard.ownsAttempt(victim) || !isGuardTarget(victim) || !isWithinGuardReach(victim)
                        || !getMainHandItem().isEmpty() || !getOffhandItem().isEmpty()
                        || !loadedNavigationArea(victim.blockPosition()) || !hasLineOfSight(victim)) return false;
            } else if (core == GolemData.BUTCHER) {
                List<ButcherMember> permitted = checkedButcherSurplus(server, victim);
                boolean visible = loadedNavigationArea(victim.blockPosition()) && hasLineOfSight(victim);
                if (!butcherSurplusCurrent(server, victim, permitted)) {
                    guard.reset(); // Never finish an injured third animal after the herd/protection state changed.
                    return false;
                }
                if (!guard.ownsAttempt(victim) || !visible || !isWithinGuardReach(victim)
                        || !getMainHandItem().isEmpty() || !getOffhandItem().isEmpty()) return false;
            } else return false;
            // Ordinary unarmed hit only: no IronGolem randomness/launch, equipment
            // effects, owner/fake-player credit, extra permission event or direct HP edit.
            boolean hurt = victim.hurt(damageSources().mobAttack(this), getMeleeDamage());
            // Fire (original E:1120-1122): only a hit that actually landed sets the victim alight, 4 s per upgrade.
            int fire = getUpgradeAmount(GolemUpgrade.FIRE);
            if (hurt && fire > 0) victim.igniteForSeconds(GolemUpgrades.igniteSeconds(fire));
            // Visor (original E:1068-1071): the victim counts as recently struck by a player, so it drops experience.
            if (hurt && GolemDecorations.marksRecentHit(decoration)) victim.lastHurtByPlayerTime = GolemDecorations.RECENT_HIT_TICKS;
            return hurt;
        } finally { busy = false; }
    }

    boolean harvestCrop(BlockPos pos, BlockState expected) {
        if (busy || core != GolemData.HARVEST || !(level() instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !canWork() || server.getEntity(getUUID()) != this || !harvest.ownsAttempt(pos, expected)
                || !GolemCrops.ripe(server, pos, expected) || distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 4) return false;
        busy = true;
        try {
            boolean harvested = GolemHarvestPlatform.harvest(this, pos, expected);
            // Order (original AIHarvestCrops:194): only after the crop was actually removed; never undoes the harvest.
            if (harvested && getUpgradeAmount(GolemUpgrade.ORDER) > 0) GolemHarvestPlatform.replant(this, pos, expected);
            return harvested;
        } finally { busy = false; }
    }

    /** One endpoint of the anchor's tree, only for the goal's own delay-0 attempt. */
    boolean chopLog(BlockPos anchor, BlockState expectedAnchor) {
        if (busy || core != GolemData.LUMBER || !(level() instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !canWork() || server.getEntity(getUUID()) != this || !lumber.ownsAttempt(anchor, expectedAnchor)
                || server.getBlockState(anchor) != expectedAnchor
                || distanceToSqr(anchor.getX() + .5, anchor.getY() + .5, anchor.getZ() + .5) > 4) return false;
        busy = true;
        try { return GolemHarvestPlatform.chop(this, anchor, expectedAnchor); }
        finally { busy = false; }
    }

    boolean eligibleItem(ItemEntity item) {
        if (core != GolemData.GATHER || !canWork() || item == null || !item.isAlive() || item.level() != level() || carrySpace() <= 0
                || distanceFromHomeSqr(item) > rangeSqr() || !level().hasChunkAt(item.blockPosition())) return false;
        ItemStack source = item.getItem();
        // Root-owned access widener exposes the actual native counter: TC4 <5,
        // not vanilla hopper's fully-zero hasPickUpDelay approximation.
        if (source.isEmpty() || item.pickupDelay >= 5) return false;
        if (!carried.isEmpty() && (!ItemStack.isSameItemSameComponents(carried, source) || source.getCount() > carrySpace())) return false;
        boolean empty = true;
        for (EntityDataAccessor<ItemStack> filter : FILTERS) {
            ItemStack exemplar = entityData.get(filter);
            if (exemplar.isEmpty()) continue;
            empty = false;
            if (ItemStack.isSameItemSameComponents(exemplar, source)) return true;
        }
        return empty;
    }
    private double distanceFromHomeSqr(ItemEntity item) {
        BlockPos home = getHomePosition();
        return item.distanceToSqr(home.getX() + .5, home.getY() + .5, home.getZ() + .5);
    }
    boolean gatherItem(ItemEntity item) {
        if (busy || !(level() instanceof ServerLevel server) || !eligibleItem(item) || distanceToSqr(item) > 2
                || server.getEntity(item.getUUID()) != item) return false;
        requireServerThread();
        busy = true;
        try {
            if (!DeferredItemDrops.allowPlayerTouch(item) || !eligibleItem(item) || !current()) return false;
            ItemStack source = item.getItem();
            int amount = Math.min(source.getCount(), Math.min(carrySpace(), source.getMaxStackSize()));
            if (amount <= 0 || source == carried || !item.isAlive() || server.getEntity(item.getUUID()) != item) return false;
            // Both real owners change BEFORE synced data, entity setters/removal or audio.
            ItemStack paid = source.split(amount);
            if (carried.isEmpty()) carried = paid;
            else { carried.grow(amount); paid.shrink(amount); }
            ItemStack remainder = source.split(source.getCount());
            if (remainder.isEmpty()) item.discard();
            else item.setItem(remainder); // The same actual entity retains age, UUID, motion, delay and owner.
            syncCarried();
            server.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.NEUTRAL, .2F, ((random.nextFloat() - random.nextFloat()) * .7F + 1) * 2);
            return true;
        } finally { busy = false; }
    }
    private void syncCarried() { entityData.set(CARRIED, carried.copy()); }

    private boolean hasUseRemainders() { return unreadUseRemainders != null || !pendingUseExtras.isEmpty(); }
    private boolean useCustodyActive() {
        return useWorkScope || useCargoDetached || useTransferScope || publishingUseRemainders
                || !level().isClientSide && GolemUsePlatform.isBorrowing(this);
    }
    boolean useReady() {
        return level() instanceof ServerLevel server && server.getServer().isSameThread() && core == GolemData.USE
                && !isNoAi() && deferredUseRemoval == null && canWork() && useControls() && server.getEntity(getUUID()) == this;
    }
    boolean useItemAllowed(ItemStack actual) {
        if (actual == null || !useControls()) return false;
        if (actual.isEmpty()) return baselineEmptyControls();
        if (!UseGolemGoals.seed(actual)) return false;
        boolean filtered = false;
        for (int slot = 0; slot < FILTERS.size(); slot++) {
            ItemStack ghost = entityData.get(FILTERS.get(slot));
            if (ghost.isEmpty()) continue;
            filtered = true;
            if (GolemItemMatch.matches(actual, ghost, itemMatchFlags())) return true;
        }
        return !filtered;
    }
    private boolean useHomeBindingCurrent() {
        if (useIntent == null || useWorkLevel == null || !useIntent.home().equals(getHomePosition()) || useIntent.facing() != getHomeFacing()
                || !UseGolemGoals.loaded(useWorkLevel, getHomePosition()) || !UseGolemGoals.loaded(useWorkLevel, homeInventoryPosition())) return false;
        if (useItemIdentity.isEmpty()) {
            return useHomeIntent == null && useWorkLevel.getBlockState(homeInventoryPosition()) == useHomeState
                    && GolemHomeInventory.isWorldThrowTarget(useWorkLevel, homeInventoryPosition());
        }
        return useHomeIntent != null && useHomeIntent.level() == useWorkLevel && useHomeIntent.pos().equals(homeInventoryPosition())
                && useHomeIntent.current();
    }
    private boolean useFrameCurrent() {
        return busy && useWorkScope && useWorkLevel != null && level() == useWorkLevel && useWorkLevel.getServer().isSameThread()
                && !useCargoReturned && useReady() && use.ownsAttempt(useIntent) && useItemIdentity != null
                && use.current(useIntent, useItemIdentity) && useHomeBindingCurrent();
    }
    private boolean useCargoPhaseCurrent() {
        if (useCargoDetached) return carried.isEmpty() && GolemUsePlatform.isBorrowing(this);
        return useBeforeCarried != null && carried == useBeforeCarried && ItemStack.matches(carried, useItemIdentity);
    }
    private boolean useEyesOn(GolemMarker marker) {
        if (!(level() instanceof ServerLevel server) || !loadedNavigationArea(marker.pos())) return false;
        BlockHitResult hit = server.clip(new ClipContext(getEyePosition(), Vec3.atCenterOf(marker.pos()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        // Thin lever/button shapes need not intersect their cell center; any earlier solid still denies sight.
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(marker.pos());
    }
    /** Root's native BEFORE hook may ask again while the real hand is DETACHED, never outside this exact goal. */
    public boolean canUseMarkedBlock(GolemMarker marker, BlockState expectedTarget, BlockState expectedAdjacent) {
        return useFrameCurrent() && usePermissionReady && useIntent.marker().equals(marker)
                && useIntent.target() == expectedTarget && useIntent.adjacent() == expectedAdjacent && useCargoPhaseCurrent()
                && distanceToMarkerSqr(marker) <= 4 && useEyesOn(marker)
                && useFrameCurrent() && useCargoPhaseCurrent() && distanceToMarkerSqr(marker) <= 4;
    }
    public ItemStack detachUseCargo() {
        if (!(level() instanceof ServerLevel server) || !server.getServer().isSameThread() || detachingUseCargo
                || !GolemUsePlatform.isDetaching(this) || useCargoDetached || useCargoReturned || useIntent == null) {
            throw new IllegalStateException("No exact native Use detaching scope");
        }
        detachingUseCargo = true;
        try {
            if (!canUseMarkedBlock(useIntent.marker(), useIntent.target(), useIntent.adjacent()) || useCargoDetached || useCargoReturned
                    || carried != useBeforeCarried || !ItemStack.matches(carried, useItemIdentity)) {
                throw new IllegalStateException("Use hand/job changed during detaching preflight");
            }
        } finally { detachingUseCargo = false; }
        ItemStack actual = carried;
        carried = ItemStack.EMPTY;
        useCargoDetached = true;
        return actual; // NO sync, callback or lookup after the raw move; EMPTY also enters the borrowed phase.
    }
    private static void validateActualUseStack(ItemStack actual) {
        if (actual == null || !actual.isEmpty() && (actual.getCount() < 1 || actual.getCount() > actual.getMaxStackSize())) {
            throw new IllegalArgumentException("Invalid native Use return stack");
        }
    }
    public void restoreUseCargo(ItemStack actualHand, List<ItemStack> actualExtras) {
        if (!(level() instanceof ServerLevel server) || !server.getServer().isSameThread() || !busy || !useWorkScope
                || useWorkLevel != server || !useCargoDetached || useCargoReturned || !GolemUsePlatform.isReturning(this)
                || !carried.isEmpty() || hasUseRemainders() || actualExtras == null) {
            throw new IllegalStateException("No exact native Use returning scope");
        }
        // Do NOT require alive/current/supported or old target state: the real action may have changed all of them.
        validateActualUseStack(actualHand);
        List<ItemStack> supplied = List.copyOf(actualExtras); // Nulls/structure validated BEFORE touching any owner.
        if (supplied.size() > MAX_USE_REMAINDERS) throw new IllegalArgumentException("Too many native Use remainder owners");
        IdentityHashMap<ItemStack, Boolean> seen = new IdentityHashMap<>();
        if (!actualHand.isEmpty()) seen.put(actualHand, Boolean.TRUE);
        List<ItemStack> nonempty = new ArrayList<>();
        for (ItemStack actual : supplied) {
            validateActualUseStack(actual);
            if (actual.isEmpty()) continue;
            if (seen.put(actual, Boolean.TRUE) != null) throw new IllegalArgumentException("Aliased native Use return owners");
            nonempty.add(actual); // The actual references, NOT copies or repaired pre-call snapshots.
        }
        List<ItemStack> prepared = List.copyOf(nonempty);
        carried = actualHand;
        pendingUseExtras = prepared;
        useCargoReturned = true;
        useCargoDetached = false;
        return; // Root raw-clears its corresponding slots NEXT; absolutely no callbacks/sync after these assignments.
    }
    boolean useMarkedBlock(UseGolemGoals.Target job) {
        if (busy || useCustodyActive() || !useReady() || !use.ownsAttempt(job) || !use.current(job, carried)
                || !(level() instanceof ServerLevel server)) return false;
        ItemStack identity = carried.copy(); // Authorization only; NEVER handed to an actor, source, refund or queue.
        busy = true;
        useWorkScope = useInvoking = true;
        useWorkLevel = server;
        useIntent = job;
        useBeforeCarried = carried;
        useItemIdentity = identity;
        useCargoDetached = useCargoReturned = usePermissionReady = useSucceeded = false;
        try {
            useHomeState = server.getBlockState(homeInventoryPosition());
            useHomeIntent = identity.isEmpty() ? null : homeInventory();
            if (!useFrameCurrent() || !GolemHarvestPlatform.mayInteract(this, homeInventoryPosition()) || !useFrameCurrent()) return false;
            if (!GolemHarvestPlatform.mayInteract(this, job.marker().pos()) || !useFrameCurrent()) return false;
            if (!GolemHarvestPlatform.mayInteract(this, job.marker().pos().relative(job.marker().face())) || !useFrameCurrent()) return false;
            usePermissionReady = true;
            if (!canUseMarkedBlock(job.marker(), job.target(), job.adjacent())) return false;
            useSucceeded = GolemUsePlatform.useBlock(this, job.marker(), job.target(), job.adjacent()); // Exactly ONE native call.
            return useSucceeded;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem Use attempt failed; keeping actual post-call ownership, never refunding/replaying", failure);
            return false;
        } finally {
            useInvoking = false;
            if (useCargoDetached || GolemUsePlatform.isBorrowing(this)) {
                Thaumcraft.LOG.error("Golem Use actor did not finish returning its actual hand; freezing custody and refusing save/removal/new clicks");
            }
            finishUseWork();
        }
    }
    private boolean publishUseRemainders() {
        if (unreadUseRemainders != null) return false;
        if (pendingUseExtras.isEmpty()) return true;
        if (publishingUseRemainders || useCargoDetached || GolemUsePlatform.isBorrowing(this)
                || !(level() instanceof ServerLevel server) || !server.getServer().isSameThread()) return false;
        boolean previous = busy;
        busy = publishingUseRemainders = true;
        try {
            DeferredItemDrops.enqueue(server, blockPosition(), pendingUseExtras);
            pendingUseExtras = List.of(); // Existing queue owns every actual extra BEFORE notifications/terminal handling.
            return true;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem Use remainder publication failed; retaining the same actual extras, not replaying use", failure);
            return false;
        } finally { publishingUseRemainders = false; busy = previous; }
    }
    private void finishUseWork() {
        if (!useWorkScope || useInvoking || useCargoDetached || GolemUsePlatform.isBorrowing(this)) return;
        try {
            publishUseRemainders();
            syncCarried();
            if (useSucceeded) startRightArmAction();
        } catch (RuntimeException feedback) {
            Thaumcraft.LOG.warn("Golem Use feedback failed after actual hand return; no refund or repeat", feedback);
        } finally {
            usePermissionReady = useCargoReturned = useSucceeded = false;
            useIntent = null;
            useHomeIntent = null;
            useHomeState = null;
            useWorkLevel = null;
            useBeforeCarried = useItemIdentity = null;
            useWorkScope = false;
            busy = false;
            completeDeferredUseRemoval();
        }
    }
    private void rememberUseRemoval(Entity.RemovalReason reason) { if (deferredUseRemoval == null) deferredUseRemoval = reason; }
    private void completeDeferredUseRemoval() {
        if (deferredUseRemoval == null || busy || useCustodyActive() || hasUseRemainders()) return;
        Entity.RemovalReason reason = deferredUseRemoval;
        deferredUseRemoval = null;
        remove(reason); // A failed evacuation remembers this one reason again; next tick retries publication only.
    }
    private void settleUseCustody() {
        if (!(level() instanceof ServerLevel server) || !server.getServer().isSameThread()) return;
        if (useWorkScope) { finishUseWork(); return; }
        if (busy || useCustodyActive()) return;
        if (!pendingUseExtras.isEmpty()) publishUseRemainders();
        completeDeferredUseRemoval();
    }
    private void readUseRemainders(CompoundTag tag) {
        pendingUseExtras = List.of();
        unreadUseRemainders = null;
        if (!tag.contains("UseRemainders")) return;
        Tag raw = tag.get("UseRemainders");
        try {
            if (!(raw instanceof ListTag list) || list.size() > MAX_USE_REMAINDERS
                    || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND
                    || !list.isEmpty() && core != GolemData.USE) throw new IllegalArgumentException("Unsupported Use remainder list");
            List<ItemStack> parsed = new ArrayList<>();
            for (Tag row : list) {
                if (!(row instanceof CompoundTag entry) || !USE_STACK_KEYS.containsAll(entry.getAllKeys())) throw new IllegalArgumentException("Unknown Use remainder fields");
                ItemStack actual = ItemStack.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), entry).getOrThrow();
                validateActualUseStack(actual);
                if (actual.isEmpty() || !entry.equals(actual.save(registryAccess()))) throw new IllegalArgumentException("Noncanonical/unsupported actual Use remainder");
                parsed.add(actual);
            }
            pendingUseExtras = List.copyOf(parsed);
        } catch (RuntimeException invalid) {
            unreadUseRemainders = raw.copy(); // Retain the WHOLE raw list; never partly pay/strip a future item.
            unsupported = true;
        }
    }
    private void saveUseRemainders(CompoundTag tag) {
        if (unreadUseRemainders != null) { tag.put("UseRemainders", unreadUseRemainders.copy()); return; }
        ListTag list = new ListTag();
        for (ItemStack actual : pendingUseExtras) list.add(actual.save(registryAccess()));
        tag.put("UseRemainders", list);
    }

    public int getWaterAmountMb() { return level().isClientSide ? entityData.get(WATER_MB) : waterMb; }
    public GolemWaterCargo getWaterCargo() {
        if (unreadWater != null) throw new IllegalStateException("Unread water cargo cannot be flattened");
        return new GolemWaterCargo(getWaterAmountMb());
    }
    public void applyWaterCargo(GolemWaterCargo water) {
        requireServerThread();
        if (water == null || !(level() instanceof ServerLevel server) || tickCount != 0 || current() || isRemoved()
                || server.getEntity(getId()) != null || server.getEntity(getUUID()) != null || busy || retiring || unsupported
                || unreadWater != null || waterMb != 0 || unreadCarried != null || !carried.isEmpty() || fishing.hasPending()
                || aspectCargo.amount() != 0 || unreadAspect != null || hasUseRemainders() || useCustodyActive() || essentiaMutationActive()) {
            throw new IllegalStateException("Water cargo can only be applied to a fresh unadmitted golem");
        }
        water.validate(material, getUpgradeAmount(GolemUpgrade.EARTH));
        if (water.amountMb() == 0) return; // Legitimate empty non-liquid placement is a no-op.
        if (core != GolemData.LIQUID || !liquidControls()) throw new IllegalArgumentException("Positive water requires valid Decanting controls");
        waterMb = water.amountMb();
        syncWater();
    }
    private void syncWater() { entityData.set(WATER_MB, waterMb); }
    int waterSpace() { return GolemWaterCargo.capacity(material, getUpgradeAmount(GolemUpgrade.EARTH)) - waterMb; }
    boolean liquidReady() {
        return level() instanceof ServerLevel server && server.getServer().isSameThread() && core == GolemData.LIQUID
                && canWork() && server.getEntity(getUUID()) == this && waterMb >= 0
                && waterMb <= GolemWaterCargo.capacity(material, getUpgradeAmount(GolemUpgrade.EARTH));
    }
    boolean currentWaterHome(TileCrucible home) {
        if (!liquidReady() || home == null || !(level() instanceof ServerLevel server)) return false;
        BlockPos pos = homeInventoryPosition();
        return LiquidGolemGoals.loaded(server, pos) && home.getLevel() == server && !home.isRemoved()
                && pos.equals(home.getBlockPos()) && server.getBlockEntity(pos) == home
                && home.getType().isValid(server.getBlockState(pos))
                && home.getWaterAmount() >= 0 && home.getWaterAmount() <= TileCrucible.CAPACITY;
    }
    private TileCrucible findWaterHome() {
        if (!liquidReady() || !(level() instanceof ServerLevel server) || !LiquidGolemGoals.loaded(server, homeInventoryPosition())) return null;
        return server.getBlockEntity(homeInventoryPosition()) instanceof TileCrucible home && currentWaterHome(home) ? home : null;
    }
    /** Finite non-breaking home preflight; a full known crucible is still supported. */
    TileCrucible permittedWaterHome() {
        if (busy || !liquidReady()) return null;
        busy = true;
        try {
            TileCrucible home = findWaterHome();
            return home != null && GolemHarvestPlatform.mayInteract(this, home.getBlockPos()) && currentWaterHome(home) ? home : null;
        } finally { busy = false; }
    }
    private void bindWaterWork(ServerLevel server, TileCrucible home, BlockPos source, BlockState state) {
        waterWorkLevel = server;
        waterHomeIntent = home;
        waterHomePosition = getHomePosition();
        waterHomeFace = getHomeFacing();
        waterSourceIntent = source == null ? null : source.immutable();
        waterSourceState = state;
        waterStockBefore = waterMb;
        waterPermissionReady = false;
    }
    private boolean rawWaterOwner() {
        return busy && waterWorkLevel != null && level() == waterWorkLevel && waterWorkLevel.getServer().isSameThread()
                && !isRemoved() && !retiring && !unsupported && unreadWater == null && core == GolemData.LIQUID
                && EnumGolemType.isSupported(material) && waterWorkLevel.getEntity(getId()) == this
                && waterWorkLevel.getEntity(getUUID()) == this;
    }
    private boolean waterBindingCurrent() {
        return rawWaterOwner() && liquidReady() && waterPermissionReady && waterMb == waterStockBefore
                && waterHomePosition.equals(getHomePosition()) && waterHomeFace == getHomeFacing()
                && currentWaterHome(waterHomeIntent);
    }
    /**
     * The source is either a marked block itself or, with Entropy, ordinary source water within the golem's range of a marked block
     * that is itself still ordinary source water (the anchor the queue grows from, original AILiquidGather:225-262).
     */
    private boolean markedSourceState(BlockPos source, BlockState expected) {
        if (waterWorkLevel == null || source == null || expected == null || waterAnchorIntent == null
                || !LiquidGolemGoals.loaded(waterWorkLevel, source) || !LiquidGolemGoals.loaded(waterWorkLevel, waterAnchorIntent)) return false;
        for (GolemMarker marker : markers) {
            if (!marker.dimension().equals(waterWorkLevel.dimension().location()) || !marker.pos().equals(waterAnchorIntent)) continue;
            if (!waterAnchorIntent.equals(source)) {
                BlockState anchorState = waterWorkLevel.getBlockState(waterAnchorIntent);
                if (getUpgradeAmount(GolemUpgrade.ENTROPY) <= 0 || !GolemLiquidQueue.inRange(waterAnchorIntent, source, getRange())
                        || !LiquidGolemGoals.sourceWater(waterWorkLevel, waterAnchorIntent, anchorState)) return false;
            }
            return waterWorkLevel.getBlockState(source) == expected && LiquidGolemGoals.sourceWater(waterWorkLevel, source, expected);
        }
        return false;
    }
    /** Root's BEFORE-removal query: only the current private gather attempt grants authority. */
    public boolean canCollectSourceWater(BlockPos source, BlockState expected) {
        return waterSourceIntent != null && waterSourceIntent.equals(source) && waterSourceState == expected
                && waterBindingCurrent() && liquid.ownsCollectionAttempt(source, expected)
                && getNavigation().isDone() && waterAnchorIntent != null
                && distanceToSqr(waterAnchorIntent.getX() + .5, waterAnchorIntent.getY() + .5, waterAnchorIntent.getZ() + .5) < 4
                && waterSpace() >= GolemWaterCargo.BUCKET && markedSourceState(source, expected);
    }
    /** AFTER root's confirmed removal: do NOT require the old source/home to remain present. */
    public boolean creditCollectedSourceWater(BlockPos source, int expectedAmountMb) {
        if (!rawWaterOwner() || !waterPermissionReady || waterSourceIntent == null || !waterSourceIntent.equals(source)
                || expectedAmountMb != waterStockBefore || waterMb != expectedAmountMb || waterMb < 0
                || waterSpace() < GolemWaterCargo.BUCKET) return false;
        waterMb = expectedAmountMb + GolemWaterCargo.BUCKET;
        return true; // No sync, notification, setter, lookup or callback after the raw credit.
    }
    public boolean canTransferWaterTo(TileCrucible actualHome) {
        return waterSourceIntent == null && waterHomeIntent != null && waterHomeIntent == actualHome
                && waterBindingCurrent() && liquid.ownsDeliveryAttempt(actualHome) && waterMb > 0
                && getNavigation().isDone() && distanceToHomeSqr() <= 5;
    }
    public boolean detachWaterToCrucible(TileCrucible actualHome, int accepted, int expectedAmountMb) {
        if (!canTransferWaterTo(actualHome) || expectedAmountMb != waterStockBefore || waterMb != expectedAmountMb
                || accepted <= 0 || accepted > waterMb || accepted > TileCrucible.CAPACITY - actualHome.getWaterAmount()) return false;
        waterMb = expectedAmountMb - accepted;
        return true; // Root raw-adds to this exact crucible NEXT, before any feedback callback.
    }
    boolean collectMarkedSourceWater(BlockPos source, BlockState state) { return collectMarkedSourceWater(source, source, state); }
    /** {@code anchor} is the marked block next to the golem; {@code source} is the block drained, the anchor itself unless Entropy queued another. */
    boolean collectMarkedSourceWater(BlockPos anchor, BlockPos source, BlockState state) {
        if (busy || !liquidReady() || !liquid.ownsCollectionAttempt(anchor, source, state) || !(level() instanceof ServerLevel server)) return false;
        int before = waterMb;
        busy = true;
        try {
            TileCrucible home = findWaterHome();
            if (home == null) { liquid.reset(); return false; }
            bindWaterWork(server, home, source, state);
            waterAnchorIntent = anchor.immutable();
            if (!GolemHarvestPlatform.mayInteract(this, home.getBlockPos()) || !currentWaterHome(home)) { liquid.reset(); return false; }
            if (!GolemHarvestPlatform.mayInteract(this, source)) return false;
            waterPermissionReady = true;
            if (!canCollectSourceWater(source, state)) return false;
            GolemHarvestPlatform.collectSourceWater(this, source, state);
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem source-water operation failed at {}; keeping actual paid stock", source, failure);
        } finally { finishWaterWork(server, before); }
        return waterMb == before + GolemWaterCargo.BUCKET;
    }
    int deliverWaterToHome(TileCrucible home) {
        if (busy || !liquidReady() || !liquid.ownsDeliveryAttempt(home) || !(level() instanceof ServerLevel server)) return 0;
        int before = waterMb;
        busy = true;
        try {
            if (!currentWaterHome(home)) { liquid.reset(); return 0; }
            bindWaterWork(server, home, null, null);
            if (!GolemHarvestPlatform.mayInteract(this, home.getBlockPos())) { liquid.reset(); return 0; }
            waterPermissionReady = true;
            if (!canTransferWaterTo(home)) return 0;
            int accepted = home.fillWaterFromGolem(this, before);
            if (accepted != before - waterMb) Thaumcraft.LOG.warn("Crucible water handoff return disagreed with actual golem debit at {}", home.getBlockPos());
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem crucible delivery failed; never refunding already accepted water", failure);
        } finally { finishWaterWork(server, before); }
        return before - waterMb;
    }
    private void finishWaterWork(ServerLevel server, int before) {
        waterWorkLevel = null;
        waterSourceIntent = waterHomePosition = waterAnchorIntent = null;
        waterSourceState = null;
        waterHomeFace = null;
        waterHomeIntent = null;
        waterPermissionReady = false;
        try {
            if (waterMb != before) {
                syncWater();
                if (level() == server && isAlive() && current()) {
                    startActionTimer();
                    server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL,
                            .2F, 1 + (server.random.nextFloat() - server.random.nextFloat()) * .3F);
                }
            }
        } catch (RuntimeException feedback) {
            Thaumcraft.LOG.warn("Golem water feedback failed after payment; stock is unchanged", feedback);
        } finally {
            busy = false;
            Entity.RemovalReason deferred = deferredWaterRemoval;
            deferredWaterRemoval = null;
            if (deferred != null) remove(deferred);
        }
    }
    private void readWaterCargo(CompoundTag tag) {
        waterMb = 0;
        unreadWater = null;
        if (!tag.contains("WaterCargo")) return; // Older supported entities owned no water.
        Tag raw = tag.get("WaterCargo");
        try {
            if (!(raw instanceof CompoundTag compound)) throw new IllegalArgumentException("WaterCargo must be a compound");
            GolemWaterCargo water = GolemWaterCargo.parse(compound);
            water.validate(material, getUpgradeAmount(GolemUpgrade.EARTH));
            if (water.amountMb() > 0 && core != GolemData.LIQUID) throw new IllegalArgumentException("Non-Decanting water stock");
            waterMb = water.amountMb();
        } catch (RuntimeException invalid) {
            unreadWater = raw.copy();
            unsupported = true;
        }
    }

    private boolean essentiaMutationActive() {
        return !level().isClientSide && EssentiaTransfers.isGolemMutationActive(this);
    }
    public Aspect getEssentiaAspect() {
        if (!level().isClientSide) return aspectCargo.aspect();
        String tag = entityData.get(ESSENTIA_TAG);
        return tag.isEmpty() ? null : Aspect.getAspect(tag);
    }
    public int getEssentiaAmount() { return level().isClientSide ? entityData.get(ESSENTIA_AMOUNT) : aspectCargo.amount(); }
    public GolemAspectCargo getAspectCargo() {
        if (unreadAspect != null) throw new IllegalStateException("Unread AspectCargo cannot be flattened");
        return level().isClientSide ? new GolemAspectCargo(getEssentiaAspect(), getEssentiaAmount()) : aspectCargo;
    }
    public void applyAspectCargo(GolemAspectCargo stock) {
        requireServerThread();
        if (stock == null || !(level() instanceof ServerLevel server) || tickCount != 0 || current() || isRemoved()
                || server.getEntity(getId()) != null || server.getEntity(getUUID()) != null || busy || essentiaMutationActive()
                || retiring || unsupported || unreadAspect != null || aspectCargo.amount() != 0 || waterMb != 0 || unreadWater != null
                || !carried.isEmpty() || unreadCarried != null || fishing.hasPending() || hasUseRemainders() || useCustodyActive()) {
            throw new IllegalStateException("Aspect cargo requires a fresh, unadmitted, stock-free golem");
        }
        stock.validate(material, getUpgradeAmount(GolemUpgrade.EARTH));
        if (stock.amount() == 0) return;
        if (core != GolemData.ALCHEMY || !baselineEmptyControls()) throw new IllegalArgumentException("Positive essentia requires baseline Alchemy");
        aspectCargo = stock;
        syncAspectCargo();
    }
    private void syncAspectCargo() {
        entityData.set(ESSENTIA_TAG, aspectCargo.aspect() == null ? "" : aspectCargo.aspect().getTag());
        entityData.set(ESSENTIA_AMOUNT, aspectCargo.amount());
    }
    boolean alchemyReady() {
        return level() instanceof ServerLevel server && server.getServer().isSameThread() && core == GolemData.ALCHEMY
                && canWork() && baselineEmptyControls() && server.getEntity(getUUID()) == this
                && aspectCargo.amount() <= GolemAspectCargo.capacity(material, getUpgradeAmount(GolemUpgrade.EARTH));
    }
    /** Narrow selection query; never an effectful break/use callback or a stock grant. */
    boolean mayUseAlchemyPosition(BlockPos pos) {
        if (busy || essentiaMutationActive() || !alchemyReady() || !(level() instanceof ServerLevel server)
                || !AlchemyGolemGoals.loaded(server, pos)) return false;
        busy = true;
        try {
            return GolemHarvestPlatform.mayInteract(this, pos) && level() == server && alchemyReady()
                    && AlchemyGolemGoals.loaded(server, pos);
        } finally { busy = false; }
    }
    private void bindAspectWork(ServerLevel server, BlockEntity base, TileAlembic source, TileJar sink, Aspect aspect, int limit) {
        aspectWorkLevel = server;
        aspectHomePosition = getHomePosition();
        aspectHomeFace = getHomeFacing();
        aspectBaseIntent = base;
        aspectSourceIntent = source;
        aspectSinkIntent = sink;
        aspectIntent = aspect;
        aspectLimit = limit;
        aspectBeforeIntent = aspectCargo;
        aspectPermissionReady = false;
        aspectWorkScope = true;
    }
    private boolean aspectNativeOwner() {
        return busy && aspectWorkScope && aspectWorkLevel != null && level() == aspectWorkLevel
                && aspectWorkLevel.getServer().isSameThread() && !isRemoved() && !retiring && !unsupported && unreadAspect == null
                && core == GolemData.ALCHEMY && EnumGolemType.isSupported(material)
                && aspectWorkLevel.getEntity(getId()) == this && aspectWorkLevel.getEntity(getUUID()) == this;
    }
    private boolean aspectBeforeCurrent() {
        return aspectNativeOwner() && deferredAspectRemoval == null && alchemyReady() && aspectBeforeIntent != null
                && aspectCargo.equals(aspectBeforeIntent) && aspectHomePosition.equals(getHomePosition()) && aspectHomeFace == getHomeFacing();
    }
    public boolean canCollectEssentia(TileAlembic source, Aspect aspect, int units) {
        return aspectBeforeCurrent() && aspectPermissionReady && aspectSourceIntent == source && source != null && aspectSinkIntent == null
                && aspectIntent == aspect && units > 0 && units <= aspectLimit && alchemy.ownsCollectionAttempt(source, aspect)
                && alchemy.currentSource(aspectBaseIntent, source, aspect, units);
    }
    public boolean canDeliverEssentia(TileJar sink, Aspect aspect, int units) {
        if (!aspectBeforeCurrent() || !aspectPermissionReady || aspectSinkIntent != sink || sink == null || aspectSourceIntent != null
                || aspectIntent != aspect || units <= 0 || units > aspectLimit || aspectCargo.aspect() != aspect || units > aspectCargo.amount()
                || !alchemy.ownsDeliveryAttempt(sink, aspect) || !getNavigation().isDone()) return false;
        BlockPos pos = sink.getBlockPos();
        return distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 4 && alchemy.currentJar(sink, aspect, units);
    }
    /** Executor-only, exact prepared objects; all checks precede the ONE raw owner replacement. */
    public void applyAspectCargoRaw(GolemAspectCargo before, GolemAspectCargo after) {
        EssentiaTransfers.requireGolemCargoChange(this, before, after);
        if (!aspectNativeOwner() || !aspectPermissionReady || !essentiaMutationActive() || before == null || after == null
                || !aspectCargo.equals(before) || !before.equals(aspectBeforeIntent)
                || !aspectHomePosition.equals(getHomePosition()) || aspectHomeFace != getHomeFacing()) {
            throw new IllegalStateException("No matching Alchemy payment scope");
        }
        int earth = getUpgradeAmount(GolemUpgrade.EARTH);
        before.validate(material, earth);
        after.validate(material, earth);
        if (aspectSourceIntent != null && aspectSinkIntent == null) {
            int credit = after.amount() - before.amount();
            if (!alchemy.ownsCollectionAttempt(aspectSourceIntent, aspectIntent) || credit <= 0 || credit > aspectLimit || after.aspect() != aspectIntent
                    || before.amount() > 0 && before.aspect() != aspectIntent) throw new IllegalArgumentException("Unexpected essentia credit");
        } else if (aspectSinkIntent != null && aspectSourceIntent == null) {
            int debit = before.amount() - after.amount();
            if (!alchemy.ownsDeliveryAttempt(aspectSinkIntent, aspectIntent) || debit <= 0 || debit > aspectLimit || before.aspect() != aspectIntent
                    || after.amount() > 0 && after.aspect() != aspectIntent) throw new IllegalArgumentException("Unexpected essentia debit");
        } else throw new IllegalStateException("No exact essentia endpoint intent");
        aspectCargo = after;
        return; // Nothing fallible, no sync, callback or world query follows raw replacement.
    }
    public void notifyGolemEssentiaChanged() {
        if (!aspectNativeOwner() || !essentiaMutationActive()) throw new IllegalStateException("Essentia notification outside executor scope");
        syncAspectCargo(); // Both raw owners are already paid; executor holds the mutation guard through notification.
    }
    boolean collectAlchemyEssentia(TileAlembic source, Aspect aspect, int requested) {
        if (busy || essentiaMutationActive() || !alchemyReady() || !alchemy.ownsCollectionAttempt(source, aspect)
                || requested <= 0 || !(level() instanceof ServerLevel server)) return false;
        AlchemyGolemGoals.Source binding = alchemy.findSource(false);
        if (binding == null || binding.alembic() != source || binding.aspect() != aspect) return false;
        GolemAspectCargo before = aspectCargo;
        boolean qualifying = false;
        busy = true;
        try {
            bindAspectWork(server, binding.base(), source, null, aspect, Math.min(requested, binding.limit()));
            BlockPos home = homeInventoryPosition(), pos = source.getBlockPos();
            if (!AlchemyGolemGoals.loaded(server, home) || !GolemHarvestPlatform.mayInteract(this, home) || !aspectBeforeCurrent()) return false;
            if (!AlchemyGolemGoals.current(server, binding.base()) || !AlchemyGolemGoals.current(server, source)) return false;
            if (!pos.equals(home) && (!AlchemyGolemGoals.loaded(server, pos) || !GolemHarvestPlatform.mayInteract(this, pos) || !aspectBeforeCurrent())) return false;
            if (!AlchemyGolemGoals.current(server, binding.base()) || !AlchemyGolemGoals.current(server, source)) return false;
            aspectPermissionReady = true;
            int limit = Math.min(aspectLimit, Math.min(source.getAmount(), getCarryLimit() - aspectCargo.amount()));
            if (!canCollectEssentia(source, aspect, limit)) return false;
            qualifying = true;
            EssentiaTransfers.Attempt attempt = EssentiaTransfers.collectAlembic(this, source, Direction.UP, aspect, limit);
            if (attempt.failure() != null) Thaumcraft.LOG.warn("Golem alembic transfer reported {} committed units; never refunding", attempt.committedUnits(), attempt.failure());
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem alembic transfer failed; retaining actual cargo, never replaying a debit", failure);
        } finally { finishAspectWork(server, before, .05F); }
        return qualifying; // Even a qualifying zero-return attempt uses the original short delay mapping.
    }
    int deliverAlchemyEssentia(TileJar sink, Aspect aspect, int requested) {
        if (busy || essentiaMutationActive() || !alchemyReady() || !alchemy.ownsDeliveryAttempt(sink, aspect)
                || requested <= 0 || !(level() instanceof ServerLevel server)) return 0;
        GolemAspectCargo before = aspectCargo;
        busy = true;
        try {
            bindAspectWork(server, null, null, sink, aspect, requested);
            BlockPos home = homeInventoryPosition(), pos = sink.getBlockPos();
            // Delivery needs the home ANCHOR/permission, not a surviving furnace or source supply.
            if (!AlchemyGolemGoals.loaded(server, home) || !GolemHarvestPlatform.mayInteract(this, home) || !aspectBeforeCurrent()) return 0;
            if (!AlchemyGolemGoals.current(server, sink)) return 0;
            if (!AlchemyGolemGoals.loaded(server, pos) || !GolemHarvestPlatform.mayInteract(this, pos) || !aspectBeforeCurrent()) return 0;
            aspectPermissionReady = true;
            int limit = Math.min(aspectLimit, Math.min(aspectCargo.amount(), sink.getMaxAmount() - sink.getAmount()));
            if (!canDeliverEssentia(sink, aspect, limit)) return 0;
            EssentiaTransfers.Attempt attempt = EssentiaTransfers.deliverNormalJar(this, sink, aspect, limit);
            if (attempt.failure() != null) Thaumcraft.LOG.warn("Golem jar transfer reported {} committed units; never refunding", attempt.committedUnits(), attempt.failure());
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem jar transfer failed; preserving exact actual remainder", failure);
        } finally { finishAspectWork(server, before, .2F); }
        return before.amount() - aspectCargo.amount();
    }
    private void finishAspectWork(ServerLevel server, GolemAspectCargo before, float volume) {
        aspectPermissionReady = false;
        aspectSourceIntent = null;
        aspectSinkIntent = null;
        aspectBaseIntent = null;
        aspectIntent = null;
        aspectHomePosition = null;
        aspectHomeFace = null;
        aspectWorkLevel = null;
        aspectBeforeIntent = null;
        try {
            if (!aspectCargo.equals(before)) {
                syncAspectCargo(); // Read-only display repair also survives an executor notification failure.
                if (level() == server && isAlive() && current()) server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_SWIM,
                        SoundSource.NEUTRAL, volume, 1 + (server.random.nextFloat() - server.random.nextFloat()) * .3F);
            }
        } catch (RuntimeException feedback) {
            Thaumcraft.LOG.warn("Golem essentia feedback failed after payment; no refund/repeat", feedback);
        } finally {
            aspectWorkScope = false;
            busy = false;
            Entity.RemovalReason deferred = deferredAspectRemoval;
            deferredAspectRemoval = null;
            if (deferred != null) remove(deferred); // ONE deferred native removal, after all paired notifications/feedback.
        }
    }
    private void readAspectCargo(CompoundTag tag) {
        aspectCargo = GolemAspectCargo.EMPTY;
        unreadAspect = null;
        if (!tag.contains("AspectCargo")) return;
        Tag raw = tag.get("AspectCargo");
        try {
            if (!(raw instanceof CompoundTag compound)) throw new IllegalArgumentException("AspectCargo must be a compound");
            GolemAspectCargo parsed = GolemAspectCargo.parse(compound);
            parsed.validate(material, getUpgradeAmount(GolemUpgrade.EARTH));
            if (parsed.amount() > 0 && core != GolemData.ALCHEMY) throw new IllegalArgumentException("Non-Alchemy essentia stock");
            aspectCargo = parsed;
        } catch (RuntimeException invalid) {
            unreadAspect = raw.copy();
            unsupported = true;
        }
    }

    BlockPos homeInventoryPosition() { return getHomePosition().relative(getHomeFacing().getOpposite()); }
    GolemHomeInventory.Destination homeInventory() {
        return hasHome && level() instanceof ServerLevel server ? GolemHomeInventory.find(server, homeInventoryPosition()) : null;
    }
    boolean hasHomeWorldThrowTarget() {
        return hasHome && level() instanceof ServerLevel server
                && GolemHomeInventory.isWorldThrowTarget(server, homeInventoryPosition());
    }
    boolean markedWorldThrowTarget(GolemMarker marker) {
        // Unlike inventory routes, original Empty world markers have NO range16
        // or home-position exclusion. The face does not offset their aim.
        return marker != null && markers.contains(marker) && level() instanceof ServerLevel server
                && (carried.isEmpty() || markerAccepts(marker, carried))
                && marker.dimension().equals(server.dimension().location())
                && GolemHomeInventory.isWorldThrowTarget(server, marker.pos());
    }
    GolemHomeInventory.Destination markedInventory(GolemMarker marker) {
        if (!(level() instanceof ServerLevel server) || marker == null || !markers.contains(marker)
                || !marker.dimension().equals(level().dimension().location()) || marker.pos().equals(homeInventoryPosition())) return null;
        BlockPos home = getHomePosition();
        double dx = (double) marker.pos().getX() - home.getX();
        double dy = (double) marker.pos().getY() - home.getY();
        double dz = (double) marker.pos().getZ() - home.getZ();
        double range = rangeSqr();
        if (dx * dx + dy * dy + dz * dz >= range || distanceToSqr(marker.pos().getX() + .5,
                marker.pos().getY() + .5, marker.pos().getZ() + .5) > range) return null;
        GolemHomeInventory.Destination result = GolemHomeInventory.find(server, marker.pos());
        // A native double chest is one storage owner, not a remote source just
        // because the marker happens to name its other physical half.
        return result == null || result.sharesStorage(homeInventory()) ? null : result;
    }
    GolemHomeInventory.Destination markedSortingInventory(GolemMarker marker) {
        if (core != GolemData.SORTING || !(level() instanceof ServerLevel server) || marker == null || !markers.contains(marker)
                || !marker.dimension().equals(server.dimension().location()) || marker.pos().equals(homeInventoryPosition())
                || distanceToMarkerSqr(marker) > rangeSqr()) return null;
        // Sorting's original bound is relative to the GOLEM, not its home. Do
        // not widen Fill/Empty's existing stricter lookup to implement this.
        GolemHomeInventory.Destination result = GolemHomeInventory.find(server, marker.pos());
        return result == null || result.sharesStorage(homeInventory()) ? null : result;
    }
    double distanceToMarkerSqr(GolemMarker marker) {
        return distanceToSqr(marker.pos().getX() + .5, marker.pos().getY() + .5, marker.pos().getZ() + .5);
    }

    private GolemMarker firstAdjacentWorldThrowMarker() {
        for (GolemMarker marker : markers) {
            if (distanceToMarkerSqr(marker) < 4 && markedWorldThrowTarget(marker)) return marker;
        }
        return null;
    }
    private boolean eligibleDirectedThrow(BlockPos target) {
        if (target == null || !canWork() || !hasCargo() || !getNavigation().isDone()) return false;
        if (core == GolemData.FILL) {
            return distanceToHomeSqr() <= 5 && target.equals(homeInventoryPosition()) && hasHomeWorldThrowTarget();
        }
        if (core == GolemData.EMPTY_CORE) {
            GolemMarker first = firstAdjacentWorldThrowMarker();
            return first != null && target.equals(first.pos());
        }
        return false; // Only Fill/Empty acquire ordinary directed-throw authorization.
    }

    /** Queue bridge, authorized only inside the real worker's private throw intent. */
    public boolean canPublishDirectedThrow(BlockPos target) {
        return level() instanceof ServerLevel server && server.getServer().isSameThread()
                && busy && throwTarget != null && throwTarget.equals(target)
                && server.getEntity(getUUID()) == this && eligibleDirectedThrow(target);
    }
    /** Raw payment: no display/event/AI/world callback may follow the assignment here. */
    public boolean detachDirectedThrowCargo(BlockPos target, ItemStack expected) {
        if (expected == null || !canPublishDirectedThrow(target) || !ItemStack.matches(carried, expected)) return false;
        carried = ItemStack.EMPTY;
        return true;
    }
    boolean throwCarriedAtHome() {
        return core == GolemData.FILL && throwCarried(homeInventoryPosition());
    }
    boolean throwCarriedAtMarker() {
        if (core != GolemData.EMPTY_CORE) return false;
        GolemMarker first = firstAdjacentWorldThrowMarker();
        return first != null && throwCarried(first.pos());
    }
    private boolean throwCarried(BlockPos target) {
        if (busy || !(level() instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !eligibleDirectedThrow(target)) return false;
        BlockPos intendedTarget = target.immutable();
        busy = true;
        try {
            throwTarget = intendedTarget;
            if (!DeferredItemDrops.enqueueGolemThrow(server, this, throwTarget)) return false;
            // True means the existing queue owns the WHOLE paid stack. Feedback
            // failure must never refund it, re-extract it or publish it again.
            syncCarried();
            startActionTimer();
            return true;
        } finally {
            throwTarget = null;
            busy = false;
        }
    }

    /** First matching exposed slot only; never an assembled or copied output batch. */
    int takeFromInventory(GolemHomeInventory.Destination source, Direction face, ItemStack exemplar, int requested) {
        if (busy || !canWork() || hasCargo() || source == null || source.level() != level() || !source.current() || exemplar.isEmpty()
                || requested <= 0 || !getNavigation().isDone()) return 0;
        if (core == GolemData.USE && (!useReady() || !(level() instanceof ServerLevel server)
                || !UseGolemGoals.loaded(server, getHomePosition()) || !UseGolemGoals.loaded(server, homeInventoryPosition()))) return 0;
        GolemHomeInventory.Destination home = homeInventory();
        if (core == GolemData.EMPTY_CORE || core == GolemData.SORTING || core == GolemData.USE) {
            if (home == null || !source.sharesStorage(home) || face != getHomeFacing()
                    || tickCount % 5 != 0 || distanceToHomeSqr() > 5) return 0;
            if (core == GolemData.USE && (!use.ownsTake(source, exemplar) || !source.pos().equals(homeInventoryPosition()))) return 0;
        } else if (core == GolemData.FILL) {
            boolean adjacent = false;
            for (GolemMarker marker : markers) {
                if (marker.face() == face && marker.pos().equals(source.pos()) && distanceToMarkerSqr(marker) < 4
                        && markerAccepts(marker, exemplar) && markedInventory(marker) != null) { adjacent = true; break; }
            }
            if (!adjacent || source.sharesStorage(home)) return 0;
        } else return 0;
        requireServerThread();
        boolean useTransfer = core == GolemData.USE;
        busy = true;
        if (useTransfer) useTransferScope = true;
        try {
            UseGolemGoals.Target useTarget = useTransfer ? use.findTarget(exemplar) : null;
            if (useTransfer && (useTarget == null || !permitUseHomeTransfer(source, face, useTarget, exemplar))) return 0;
            int match = itemMatchFlags();
            int slot = source.firstExtractable(exemplar, face, match);
            if (slot < 0) return 0;
            Container inventory = source.container();
            ItemStack actual = inventory.getItem(slot);
            int before = actual.getCount();
            int wanted = core == GolemData.FILL ? Math.min(requested, inventories.fillRequest(exemplar)) : requested;
            int amount = Math.min(wanted, Math.min(before, Math.min(carrySpace(), actual.getMaxStackSize())));
            if (amount <= 0 || !canWork() || hasCargo() || !source.current() || inventory.getItem(slot) != actual
                    || actual.getCount() != before || !source.extracts(slot, actual, face)
                    || !GolemItemMatch.matches(actual, exemplar, match)
                    || (core == GolemData.EMPTY_CORE || core == GolemData.SORTING) && !inventories.canDeliver(actual)) return 0;
            if (useTransfer && (!useReady() || !use.ownsTake(source, exemplar) || !UseGolemGoals.seed(actual)
                    || !use.current(useTarget, actual) || !source.pos().equals(homeInventoryPosition()) || face != getHomeFacing())) return 0;
            // Actual slot debit and actual cargo assignment BOTH precede native
            // setters/dirty notices. A callback can never spend the old slot too.
            carried = actual.split(amount);
            if (actual.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
            else inventory.setChanged();
            return amount;
        } finally {
            try { syncCarried(); }
            finally {
                if (useTransfer) useTransferScope = false;
                busy = false;
                if (useTransfer) completeDeferredUseRemoval();
            }
        }
    }

    private boolean permitUseHomeTransfer(GolemHomeInventory.Destination home, Direction face, UseGolemGoals.Target actionTarget, ItemStack actual) {
        if (!busy || !useTransferScope || !useReady() || home == null || home.level() != level() || !home.current()
                || !home.pos().equals(homeInventoryPosition()) || face != getHomeFacing()
                || !(level() instanceof ServerLevel server) || !UseGolemGoals.loaded(server, getHomePosition())
                || !UseGolemGoals.loaded(server, home.pos())) return false;
        BlockPos anchor = getHomePosition().immutable();
        if (!GolemHarvestPlatform.mayInteract(this, home.pos()) || !useReady() || !home.current()) return false;
        if (actionTarget != null) {
            if (!use.current(actionTarget, actual) || !GolemHarvestPlatform.mayInteract(this, actionTarget.marker().pos())
                    || !use.current(actionTarget, actual)) return false;
            if (!GolemHarvestPlatform.mayInteract(this, actionTarget.marker().pos().relative(actionTarget.marker().face()))
                    || !use.current(actionTarget, actual)) return false;
        } else if (!use.ownsReturn() || !use.shouldReturn(actual)) return false;
        return useReady() && home.current() && anchor.equals(getHomePosition()) && face == getHomeFacing()
                && home.pos().equals(homeInventoryPosition());
    }

    int placeCarriedAtHome() {
        if (core == GolemData.HARVEST || core == GolemData.LUMBER || core == GolemData.GUARD || core == GolemData.FISHING
                || core == GolemData.LIQUID || core == GolemData.ALCHEMY || core == GolemData.BUTCHER
                || !getNavigation().isDone() || distanceToHomeSqr() > 5) return 0;
        // HomeReplace10 is NOT unconditional HomePlace: valid just-taken stock
        // must be allowed to leave home for its seeded destination.
        if (core == GolemData.SORTING && (!canWork() || tickCount % 5 != 0 || !inventories.shouldReturnSortingCargo(carried))) return 0;
        if (core == GolemData.USE && (!useReady() || !use.ownsReturn() || tickCount % 5 != 0 || !use.shouldReturn(carried))) return 0;
        if (core == GolemData.USE && (!(level() instanceof ServerLevel server) || !UseGolemGoals.loaded(server, getHomePosition())
                || !UseGolemGoals.loaded(server, homeInventoryPosition()))) return 0;
        return placeIntoInventory(homeInventory(), getHomeFacing(), null);
    }
    int placeCarriedAtMarker(GolemMarker marker) {
        if (core != GolemData.EMPTY_CORE && core != GolemData.SORTING
                || marker == null || !getNavigation().isDone() || distanceToMarkerSqr(marker) >= 4
                || !markerAccepts(marker, carried)) return 0;
        return placeIntoInventory(core == GolemData.SORTING ? markedSortingInventory(marker) : markedInventory(marker), marker.face(), marker);
    }
    private int placeIntoInventory(GolemHomeInventory.Destination target, Direction face, GolemMarker marker) {
        if (busy || !canWork() || !hasCargo() || target == null || target.level() != level() || !target.current()) return 0;
        requireServerThread();
        boolean useTransfer = core == GolemData.USE;
        busy = true;
        if (useTransfer) useTransferScope = true;
        int moved = 0;
        try {
            if (useTransfer && (marker != null || !permitUseHomeTransfer(target, face, null, carried))) return 0;
            Container inventory = target.container();
            int[] slots = target.slots(face);
            if (slots == null) return 0;
            // Strict occupied merges precede empties, including a native double chest.
            for (int pass = 0; pass < 2 && !carried.isEmpty(); pass++) for (int slot : slots) {
                if (!canWork() || !target.current() || carried.isEmpty() || marker != null && !markers.contains(marker)) return moved;
                ItemStack source = carried;
                if (!target.accepts(slot, source, face)) continue;
                ItemStack old = inventory.getItem(slot);
                if ((pass == 0) == old.isEmpty() || old == source) continue;
                if (!old.isEmpty() && !ItemStack.isSameItemSameComponents(old, source)) continue;
                int oldCount = old.getCount();
                int maximum = Math.min(64, Math.min(source.getMaxStackSize(), inventory.getMaxStackSize(source)));
                int amount = Math.min(source.getCount(), Math.max(0, maximum - oldCount));
                if (amount <= 0) continue;
                // Re-probe the SAME face's real extractable seed at EVERY paid
                // commit, not just when the route or preview was selected.
                if (core == GolemData.SORTING && marker != null && !target.hasSeedAndRoom(source, face, itemMatchFlags())) return moved;
                if (!target.current() || carried != source || inventory.getItem(slot) != old
                        || old.getCount() != oldCount || !canWork()) continue;
                if (useTransfer && (!useReady() || !use.ownsReturn() || !target.pos().equals(homeInventoryPosition()) || face != getHomeFacing())) return moved;
                ItemStack paid = source.split(amount);
                if (source.isEmpty()) carried = ItemStack.EMPTY;
                moved += amount;
                if (old.isEmpty()) inventory.setItem(slot, paid);
                else {
                    old.grow(amount);
                    paid.shrink(amount);
                    inventory.setChanged();
                }
            }
            return moved;
        } finally {
            try { syncCarried(); }
            finally {
                if (useTransfer) useTransferScope = false;
                busy = false;
                if (useTransfer) completeDeferredUseRemoval();
            }
        }
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || held.getItem() instanceof ItemWandCasting) return InteractionResult.PASS;
        if (busy || retiring || !EnumGolemType.isSupported(getGolemMaterial()) || unsupported || !canInteract(player, held)) return InteractionResult.PASS;
        if (held.is(ThaumcraftGolems.GOLEM_BELL.get())) return ItemGolemBell.select(held, player, this);
        if (held.getItem() instanceof ItemGolemCore coreItem && activeCore(coreItem.getCore()) && getGolemCore() == GolemData.NO_CORE) {
            if ((requiresEmptyControls(coreItem.getCore()) || coreItem.getCore() == GolemData.LIQUID) && !baselineEmptyControls()) return InteractionResult.PASS;
            if (coreItem.getCore() == GolemData.USE && (!useWhitelistControls() || !useOptions.supportsDefaultBaseline())) return InteractionResult.PASS;
            if (level().isClientSide) return InteractionResult.SUCCESS;
            installCore(player, hand, held, coreItem.getCore());
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof ItemGolemDecoration decorationItem) {
            // Original customInteraction:896-899: any golem, even a coreless one; refused repeats and partners pay nothing.
            if (!GolemDecorations.canAdd(getDecoration(), decorationItem.letter())) return InteractionResult.PASS;
            if (level().isClientSide) return InteractionResult.SUCCESS;
            installDecoration(player, hand, held, decorationItem.letter());
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof ItemGolemUpgrade upgradeItem) {
            // Original E:991-1006 follows core installation. No free slot (or a third of one type)
            // means no payment AND no menu while an upgrade is held. Works on a coreless golem too.
            int slot = GolemUpgrades.installSlot(upgradeView(), upgradeItem.kind().id);
            if (slot < 0) return InteractionResult.PASS;
            if (level().isClientSide) return InteractionResult.SUCCESS;
            installUpgrade(player, hand, held, upgradeItem.kind(), slot);
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.COOKIE)) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            feedCookie(player, hand, held);
            return InteractionResult.CONSUME;
        }
        if (hasConfigurationMenu()) {
            if (player instanceof ServerPlayer sender) ContainerGolem.open(sender, this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS; // Harvest/Fishing/coreless must not open an empty dummy menu.
    }
    private void installCore(Player player, InteractionHand hand, ItemStack held, int installedCore) {
        requireServerThread();
        busy = true;
        try {
            if (core != GolemData.NO_CORE || !activeCore(installedCore) || player.getItemInHand(hand) != held
                    || waterMb != 0 || unreadWater != null || aspectCargo.amount() != 0 || unreadAspect != null || essentiaMutationActive()
                    || !(held.getItem() instanceof ItemGolemCore item) || item.getCore() != installedCore || !canInteract(player, held)
                    || (requiresEmptyControls(installedCore) || installedCore == GolemData.LIQUID) && !baselineEmptyControls()
                    || installedCore == GolemData.USE && (!useWhitelistControls() || !useOptions.supportsDefaultBaseline())) return;
            core = installedCore; // Actual installation precedes its one-item payment.
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            }
            entityData.set(CORE, core);
            rebuildGoals();
            entityData.set(BOOTUP, 33F);
            level().broadcastEntityEvent(this, (byte) 7);
            playSound(UPGRADE, .5F, 1);
            player.getInventory().setChanged();
        } finally { busy = false; }
    }
    private void installUpgrade(Player player, InteractionHand hand, ItemStack held, GolemUpgrade kind, int slot) {
        requireServerThread();
        busy = true;
        try {
            if (player.getItemInHand(hand) != held || !(held.getItem() instanceof ItemGolemUpgrade item) || item.kind() != kind
                    || essentiaMutationActive() || !canInteract(player, held) || !kind.isSupported()
                    || GolemUpgrades.installSlot(upgradeSlots, kind.id) != slot) return;
            upgradeSlots[slot] = kind.id; // Actual installation precedes its one-item payment, like the core.
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            }
            syncUpgrades();
            applyMaterialAttributes(EnumGolemType.getType(material));
            playSound(UPGRADE, .5F, 1);
            player.getInventory().setChanged();
        } finally { busy = false; }
    }
    private void installDecoration(Player player, InteractionHand hand, ItemStack held, char letter) {
        requireServerThread();
        busy = true;
        try {
            if (player.getItemInHand(hand) != held || !(held.getItem() instanceof ItemGolemDecoration item) || item.letter() != letter
                    || essentiaMutationActive() || !canInteract(player, held) || !GolemDecorations.canAdd(decoration, letter)) return;
            decoration = decoration + letter; // The installation precedes its one-item payment, like the core and upgrades.
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            }
            entityData.set(DECORATION, decoration);
            applyMaterialAttributes(EnumGolemType.getType(material));
            playSound(CLACK, 1, 1);
            player.getInventory().setChanged();
        } finally { busy = false; }
    }
    private void feedCookie(Player player, InteractionHand hand, ItemStack held) {
        requireServerThread();
        busy = true;
        try {
            ItemStack remainder = ItemStackHooks.getCraftingRemainingItem(held.copyWithCount(1));
            if (!canInteract(player, held) || player.getItemInHand(hand) != held || !held.is(Items.COOKIE)) return;
            held.shrink(1); // Original consumeInventoryItem consumes a cookie even in creative.
            if (held.isEmpty()) player.setItemInHand(hand, remainder);
            else if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
                DeferredItemDrops.enqueue((ServerLevel) level(), player.blockPosition(), List.of(remainder));
            }
            heal(5);
            ServerLevel server = (ServerLevel) level();
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + .5, getZ(), 3, .2, .4, .2, .02);
            for (int i = 0; i < 3; i++) playSound(SoundEvents.GENERIC_EAT, .3F, (random.nextFloat() - random.nextFloat()) * .2F + 1);
            player.getInventory().setChanged();
        } finally { busy = false; }
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof Player player && source.getEntity() == player
                && player.getMainHandItem().is(ThaumcraftGolems.GOLEM_BELL.get())) {
            if (!level().isClientSide) recall(player, player.isShiftKeyDown());
            return false; // Bell failure is NOT permission to apply ordinary damage.
        }
        reflectThorns(source);
        return super.hurt(source, amount);
    }
    /**
     * Entropy thorns (original E:1139-1142): before the ordinary damage, whatever DIRECTLY damaged the golem
     * (the original reads getSourceOfDamage, so an arrow takes the thorns, not its shooter) is hurt for
     * 2n + nextInt(2n) and plays the thorns sound. The original recurses without bound when two Entropy
     * golems hurt each other; a reflected hit never reflects again here (documented deviation).
     */
    private void reflectThorns(DamageSource source) {
        int entropy = getUpgradeAmount(GolemUpgrade.ENTROPY);
        Entity attacker = source.getDirectEntity();
        if (entropy <= 0 || reflectingThorns || level().isClientSide || attacker == null || attacker == this) return;
        reflectingThorns = true;
        try {
            attacker.hurt(damageSources().thorns(this), GolemUpgrades.thornsDamage(entropy, random::nextInt));
            attacker.playSound(SoundEvents.THORNS_HIT, .5F, 1);
        } finally { reflectingThorns = false; }
    }
    public boolean recall(Player player, boolean dismantle) {
        if (player == null || !(level() instanceof ServerLevel server) || busy || essentiaMutationActive() || retiring || !EnumGolemType.isSupported(material) || unsupported
                || unreadCarried != null || unreadWater != null || unreadAspect != null || unreadUseRemainders != null || useCustodyActive()
                || dismantle && (waterMb > 0 || aspectCargo.amount() > 0)
                || !canInteract(player, player.getMainHandItem()) || !player.getMainHandItem().is(ThaumcraftGolems.GOLEM_BELL.get())) return false;
        requireServerThread();
        // Configured placer data has no equipment storage. These workers must not
        // discard nonbaseline real gear merely to recall it.
        if ((core == GolemData.GUARD || core == GolemData.FISHING || core == GolemData.LIQUID
                || core == GolemData.ALCHEMY || core == GolemData.USE || core == GolemData.BUTCHER) && hasGuardEquipment()) return false;
        busy = true;
        try {
            use.reset();
            alchemy.reset();
            liquid.reset();
            if (!publishUseRemainders()) return false;
            if (!fishing.publishPending(server)) return false;
            fishing.cancelCast();
            if (!canInteract(player, player.getMainHandItem()) || !player.getMainHandItem().is(ThaumcraftGolems.GOLEM_BELL.get())
                    || level() != server || server.getEntity(getId()) != this || server.getEntity(getUUID()) != this
                    || !isAlive() || retiring || unsupported || unreadCarried != null || unreadWater != null || unreadAspect != null) return false;
            if ((core == GolemData.GUARD || core == GolemData.FISHING || core == GolemData.LIQUID
                    || core == GolemData.ALCHEMY || core == GolemData.USE || core == GolemData.BUTCHER) && hasGuardEquipment()) return false;
            Item placerItem = ThaumcraftGolems.placerForMaterial(material);
            if (placerItem == null) return false;
            ItemStack placer = new ItemStack(placerItem);
            List<ItemStack> outputs = new ArrayList<>();
            outputs.add(placer);
            if (dismantle) {
                if (core != GolemData.NO_CORE) {
                    Item fittedCore = ThaumcraftGolems.coreForType(core);
                    if (fittedCore == null) return false;
                    outputs.add(new ItemStack(fittedCore));
                }
                // ItemGolemBell:302-305: a coin only for an occupied slot, in slot order; a lost upgrade is gone.
                for (GolemUpgrade returned : GolemUpgrades.dismantleReturns(upgradeSlots, random::nextBoolean)) {
                    Item upgradeItem = ThaumcraftGolems.upgradeItem(returned);
                    if (upgradeItem == null) return false;
                    outputs.add(new ItemStack(upgradeItem));
                }
                // ItemGolemBell:272: the advanced tag survives even the bare placer.
                if (advanced) placer.set(ThaumcraftGolems.GOLEM_DATA.get(), GolemData.EMPTY.withAdvanced(true));
            } else {
                placer.set(ThaumcraftGolems.GOLEM_DATA.get(), configuredData());
                if (hasCustomName()) placer.set(DataComponents.CUSTOM_NAME, getCustomName().copy());
                getWaterCargo().writeToPlacer(placer);
                if (aspectCargo.amount() > 0) getAspectCargo().writeToPlacer(placer);
            }
            if (!carried.isEmpty()) outputs.add(carried); // Separate real stock, never in placer data.
            // Existing durable queue owns ALL outputs before cargo is cleared or
            // the entity is discarded; no addFreshEntity(false)-and-delete path.
            DeferredItemDrops.enqueue(server, blockPosition(), outputs);
            carried = ItemStack.EMPTY;
            waterMb = 0; // Raw stock transfer beside item cargo, BEFORE any removal/display callbacks.
            aspectCargo = GolemAspectCargo.EMPTY;
            retiring = true;
            configurationOpeners.clear();
            getNavigation().stop();
            discard();
            server.playSound(null, getX(), getY(), getZ(), ZAP, SoundSource.NEUTRAL, .5F, 1);
            return true;
        } catch (RuntimeException failure) {
            if (!retiring) Thaumcraft.LOG.warn("Golem recall failed at {}; retaining the living entity and cargo", blockPosition(), failure);
            else throw failure;
            return false;
        } finally { busy = false; }
    }
    @Override protected void dropCustomDeathLoot(ServerLevel server, DamageSource source, boolean recentlyHit) {
        queueDeathCargo(server); // No replacement placer/core, ghost filters or equipment cargo.
    }
    private boolean queueDeathCargo(ServerLevel server) {
        if (useCustodyActive()) return false; // In-flight death waits for the actor's ACTUAL post-call hand.
        use.reset();
        guard.reset(); // Clear a dead Guard/Butcher hunt without changing its cooldown or any stock.
        alchemy.reset();
        liquid.reset();
        cancelFishingCast();
        if (!publishUseRemainders()) return false;
        if (!evacuateFishingCatch()) return false;
        if (unreadCarried != null) return false;
        if (carried.isEmpty()) return true;
        if (droppingCargo) return false;
        droppingCargo = true;
        try {
            DeferredItemDrops.enqueue(server, blockPosition(), List.of(carried));
            carried = ItemStack.EMPTY;
            syncCarried();
            return true;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem cargo evacuation failed at {}; retaining cargo on the entity", blockPosition(), failure);
            return false;
        } finally { droppingCargo = false; }
    }
    @Override protected void tickDeath() {
        // A failed enqueue is retried while the dead entity still owns its cargo,
        // rather than allowing vanilla's 20-tick removal to silently delete it.
        if (level() instanceof ServerLevel server && !queueDeathCargo(server)) return;
        if (unreadWater != null || unsupported && waterMb > 0) return;
        if (unreadAspect != null || unsupported && aspectCargo.amount() > 0) return;
        // Known water/essentia follow original death loss; no buckets/jars/placers.
        super.tickDeath();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        if (useCustodyActive()) throw new IllegalStateException("Cannot serialize a transient Use call/borrowed paid hand");
        // Evacuate before capturing NBT; on failure the same private paid row is saved.
        evacuateFishingCatch();
        publishUseRemainders();
        super.addAdditionalSaveData(tag);
        fishing.savePending(tag);
        saveUseRemainders(tag);
        tag.put("WaterCargo", unreadWater == null ? new GolemWaterCargo(waterMb).save() : unreadWater.copy());
        tag.put("AspectCargo", unreadAspect == null ? aspectCargo.save() : unreadAspect.copy());
        BlockPos home = getHomePosition();
        tag.putBoolean("HomeSet", hasHome);
        tag.putInt("HomeX", home.getX()); tag.putInt("HomeY", home.getY()); tag.putInt("HomeZ", home.getZ());
        tag.putByte("HomeFacing", (byte) getHomeFacing().get3DDataValue());
        tag.putInt("GolemType", material);
        tag.putInt("Core", getGolemCore());
        tag.putInt("GolemCore", getGolemCore());
        tag.putString("Owner", getOwnerName());
        tag.putFloat("Bootup", getBootupTimer());
        tag.putInt("Healing", getHealingTimer());
        if (unreadCarried != null) tag.put("ItemCarried", unreadCarried.copy());
        else if (!carried.isEmpty()) tag.put("ItemCarried", carried.save(registryAccess()));
        int slots = getSlotCount();
        NonNullList<ItemStack> ghosts = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int slot = 0; slot < slots; slot++) ghosts.set(slot, getFilter(slot));
        CompoundTag ghostTag = new CompoundTag();
        ContainerHelper.saveAllItems(ghostTag, ghosts, registryAccess());
        tag.put("Inventory", ghostTag.getList("Items", Tag.TAG_COMPOUND));
        int[] wanted = new int[slots];
        for (int slot = 0; slot < slots; slot++) wanted[slot] = getWantedCount(slot);
        tag.putIntArray("WantedCounts", wanted);
        byte[] colors = new byte[slots];
        boolean coloured = false;
        for (int slot = 0; slot < slots; slot++) {
            colors[slot] = (byte) getSlotColor(slot);
            coloured |= colors[slot] != GolemData.NO_COLOR;
        }
        if (coloured) tag.putByteArray("colors", colors); // Omitted when all default: pre-Order saves keep their exact form.
        // Omitted at the default so every earlier save keeps its exact form. Not the original "toggles" byte: its four bits
        // mean the opposite (set = forbidden) and a fresh original Guard allows everything.
        if (guardTargets != GolemGuardTargets.DEFAULT) tag.putByte("GuardTargets", (byte) guardTargets);
        tag.putBoolean("FillAny", isFillAnyAmount());
        tag.put("UseOptions", GolemUseOptions.CODEC.encodeStart(NbtOps.INSTANCE, useOptions).getOrThrow());
        tag.put("Markers", MARKER_CODEC.encodeStart(NbtOps.INSTANCE, markers).getOrThrow());
        tag.putByteArray("upgrades", GolemUpgrades.toBytes(upgradeSlots)); // Original key; -1 is an empty slot.
        if (advanced) tag.putBoolean("advanced", true); // Original key; omitted while false so earlier saves keep their form.
        if (!decoration.isEmpty()) tag.putString("Decoration", decoration); // Original key; omitted while empty.
        // Not the original "toggles" byte: its bits 5..7 mean the same, but its bits 1..4 are the Guard's inverted switches.
        if (itemMatch != 0) tag.putByte("ItemMatch", (byte) itemMatch);
        if (retainedConfiguration != null) for (String key : retainedConfiguration.getAllKeys()) {
            tag.put(key, retainedConfiguration.get(key).copy());
        }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        requireServerThread();
        // A native load must not overwrite an unpublished paid row, or publish it
        // and then import a second copy of that same serialized row.
        if (busy || essentiaMutationActive() || fishing.hasPending() || waterMb != 0 || unreadWater != null
                || aspectCargo.amount() != 0 || unreadAspect != null || !carried.isEmpty() || unreadCarried != null
                || hasUseRemainders() || useCustodyActive() || deferredUseRemoval != null) {
            throw new IllegalStateException("Cannot reload a busy golem or overwrite owned cargo/pending stock");
        }
        busy = true;
        try { readGolemData(tag); }
        finally { busy = false; }
    }
    private void readGolemData(CompoundTag tag) {
        use.reset();
        alchemy.reset();
        liquid.reset();
        cancelFishingCast();
        super.readAdditionalSaveData(tag);
        // Native super may have read Health against the default Straw maximum
        // when Attributes were absent. Recover the saved value, never refill.
        float savedHealth = tag.contains("Health", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("Health") : getHealth();
        material = tag.getInt("GolemType");
        core = tag.contains("GolemCore", Tag.TAG_ANY_NUMERIC) ? tag.getInt("GolemCore")
                : tag.contains("Core", Tag.TAG_ANY_NUMERIC) ? tag.getInt("Core") : GolemData.NO_CORE;
        int facing = tag.getInt("HomeFacing");
        unsupported = !EnumGolemType.isSupported(material) || core != GolemData.NO_CORE && !activeCore(core)
                || facing < 0 || facing > 5 || unsupportedExtras(tag);
        // Padding like the original; an unsupported type or more upgrades than slots keeps the raw array
        // (retained below) instead of being truncated, and the golem stays inert.
        upgradeSlots = new int[0];
        advanced = !unsupported && tag.getBoolean("advanced");
        entityData.set(ADVANCED, advanced);
        decoration = !unsupported ? tag.getString("Decoration") : "";
        entityData.set(DECORATION, decoration);
        if (!unsupported) {
            int[] stored = GolemUpgrades.fromBytes(tag.getByteArray("upgrades"), GolemUpgrades.slotCount(EnumGolemType.getType(material).upgrades, advanced));
            if (stored == null) unsupported = true;
            else upgradeSlots = stored;
        }
        syncUpgrades();
        retainedConfiguration = null;
        useOptions = GolemUseOptions.DEFAULT;
        if (tag.contains("UseOptions")) {
            try { useOptions = GolemUseOptions.CODEC.parse(NbtOps.INSTANCE, tag.get("UseOptions")).getOrThrow(); }
            catch (RuntimeException invalid) { unsupported = true; } // Raw dedicated options retained with configuration below.
        }
        if (core == GolemData.USE ? !useOptions.supportsDefaultBaseline() : !useOptions.isDefault()) unsupported = true;
        if (!fishing.loadPending(tag)) unsupported = true;
        readUseRemainders(tag);
        readWaterCargo(tag);
        readAspectCargo(tag);
        unreadCarried = null;
        carried = ItemStack.EMPTY;
        if (tag.contains("ItemCarried") && !tag.contains("ItemCarried", Tag.TAG_COMPOUND)) {
            unreadCarried = tag.get("ItemCarried").copy();
            unsupported = true;
        } else if (tag.contains("ItemCarried", Tag.TAG_COMPOUND) && !tag.getCompound("ItemCarried").isEmpty()) {
            CompoundTag encoded = tag.getCompound("ItemCarried");
            carried = ItemStack.parse(registryAccess(), encoded).orElse(ItemStack.EMPTY);
            if (carried.isEmpty()) { unreadCarried = encoded.copy(); unsupported = true; }
            else if (core == GolemData.USE) {
                try {
                    validateActualUseStack(carried);
                    if (!USE_STACK_KEYS.containsAll(encoded.getAllKeys()) || !encoded.equals(carried.save(registryAccess()))) {
                        throw new IllegalArgumentException("Unsupported actual Use hand encoding");
                    }
                } catch (RuntimeException invalid) { unreadCarried = encoded.copy(); unsupported = true; }
            }
        }
        hasHome = !tag.contains("HomeSet") || tag.getBoolean("HomeSet");
        BlockPos home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        restrictTo(home, 32);
        entityData.set(HOME, home);
        entityData.set(HOME_FACE, facing >= 0 && facing < 6 ? facing : Direction.DOWN.get3DDataValue());
        entityData.set(OWNER, tag.getString("Owner"));
        entityData.set(CORE, core);
        regenTimer = 0; // Original repair cadence is transient, not placer/entity fuel.
        entityData.set(BOOTUP, tag.getFloat("Bootup"));
        entityData.set(HEALING, tag.getInt("Healing"));
        int slots = getSlotCount(); // Core and upgrade slots were read above.
        NonNullList<ItemStack> ghosts = NonNullList.withSize(slots, ItemStack.EMPTY);
        CompoundTag ghostTag = new CompoundTag();
        ListTag inventory = tag.getList("Inventory", Tag.TAG_COMPOUND);
        if (tag.get("Inventory") instanceof ListTag rawInventory && !rawInventory.isEmpty()
                && rawInventory.getElementType() != Tag.TAG_COMPOUND) unsupported = true;
        ghostTag.put("Items", inventory);
        ContainerHelper.loadAllItems(ghostTag, ghosts, registryAccess());
        for (int slot = 0; slot < FILTERS.size(); slot++) {
            ItemStack ghost = slot < slots ? ghosts.get(slot) : ItemStack.EMPTY;
            entityData.set(FILTERS.get(slot), ghost.isEmpty() ? ItemStack.EMPTY : ghost.copyWithCount(1));
        }
        boolean[] occupiedGhostRows = new boolean[slots];
        for (Tag row : inventory) {
            CompoundTag entry = (CompoundTag) row;
            int slot = entry.getInt("Slot");
            ItemStack decoded = ItemStack.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), entry)
                    .result().orElse(ItemStack.EMPTY);
            if (!entry.contains("Slot", Tag.TAG_BYTE) || !GHOST_STACK_KEYS.containsAll(entry.getAllKeys()) || slot < 0 || slot >= slots
                    || decoded.isEmpty() || decoded.getCount() != 1) unsupported = true;
            else if (occupiedGhostRows[slot]) unsupported = true;
            else occupiedGhostRows[slot] = true;
        }
        int[] wanted = tag.getIntArray("WantedCounts");
        boolean hasWanted = tag.contains("WantedCounts");
        // A six-entry array on a golem whose Fire pages came later is padded; any other length is unsupported data.
        boolean wantedFits = wanted.length == slots || wanted.length == GolemData.FILTER_SLOTS && slots > GolemData.FILTER_SLOTS;
        if (hasWanted && !wantedFits) unsupported = true;
        for (int slot = 0; slot < FILTERS.size(); slot++) {
            if (slot >= slots) { entityData.set(WANTED.get(slot), 0); continue; }
            boolean empty = ghosts.get(slot).isEmpty();
            int value = hasWanted && wantedFits ? (slot < wanted.length ? wanted[slot] : 0) : empty || core == GolemData.LIQUID ? 0 : 1;
            if (empty || core == GolemData.LIQUID ? value != 0 : value < 1 || value > 256) {
                unsupported = true;
                value = empty || core == GolemData.LIQUID ? 0 : 1; // Display only; malformed original retained below.
            }
            entityData.set(WANTED.get(slot), value);
        }
        // Slot colours (original key "colors"): a six-entry array on a golem whose Fire pages came later is padded.
        byte[] storedColors = tag.getByteArray("colors");
        boolean hasColors = tag.contains("colors");
        boolean colorsFit = !hasColors || storedColors.length == slots || storedColors.length == GolemData.FILTER_SLOTS && slots > GolemData.FILTER_SLOTS;
        if (hasColors && !colorsFit) unsupported = true;
        boolean anyColor = false;
        for (int slot = 0; slot < COLORS.size(); slot++) {
            int color = hasColors && colorsFit && slot < slots && slot < storedColors.length ? storedColors[slot] : GolemData.NO_COLOR;
            if (color < GolemData.NO_COLOR || color > GolemMarker.LAST_COLOR) { unsupported = true; color = GolemData.NO_COLOR; }
            anyColor |= color != GolemData.NO_COLOR;
            entityData.set(COLORS.get(slot), color);
        }
        if (anyColor && !canColorSlots()) unsupported = true; // A colour without its Order rule is data this port cannot honour.
        guardTargets = GolemGuardTargets.DEFAULT;
        if (tag.contains("GuardTargets")) {
            int stored = tag.getByte("GuardTargets") & 0xFF; // The tag type was checked in unsupportedExtras.
            if (!GolemGuardTargets.acceptable(stored, core, getUpgradeAmount(GolemUpgrade.ORDER))) unsupported = true; // Kept raw below.
            else guardTargets = stored;
        }
        itemMatch = 0;
        if (tag.contains("ItemMatch")) {
            int stored = tag.getByte("ItemMatch") & 0xFF; // The tag type was checked in unsupportedExtras.
            if (!GolemItemMatch.acceptable(stored, core, getUpgradeAmount(GolemUpgrade.ENTROPY))) unsupported = true; // Kept raw below.
            else itemMatch = stored;
        }
        entityData.set(FILL_ANY, tag.getBoolean("FillAny"));
        if (requiresEmptyControls(core) && !baselineEmptyControls()) unsupported = true;
        if (core == GolemData.LIQUID && !liquidControls()) unsupported = true;
        if (core == GolemData.USE && !useControls()) unsupported = true;
        markers = List.of();
        if (tag.contains("Markers")) {
            Tag raw = tag.get("Markers");
            if (!(raw instanceof ListTag list) || list.size() > 64) unsupported = true;
            else {
                try {
                    var decoded = MARKER_CODEC.parse(NbtOps.INSTANCE, raw).result();
                    if (decoded.isEmpty()) unsupported = true;
                    else {
                        markers = copyMarkers(decoded.get());
                        var encoded = MARKER_CODEC.encodeStart(NbtOps.INSTANCE, markers).result();
                        if (encoded.isEmpty() || !raw.equals(encoded.get())) unsupported = true;
                        if (getUpgradeAmount(GolemUpgrade.ORDER) == 0 && markers.stream().anyMatch(marker -> marker.color() != GolemMarker.ANY)) {
                            unsupported = true; // Coloured markers without Order are retained, not honoured.
                            markers = List.of();
                        }
                    }
                } catch (RuntimeException invalidMarkers) {
                    unsupported = true; // No partial marker normalization may authorize work/recall.
                    markers = List.of();
                }
            }
        }
        if (unsupported) {
            retainedConfiguration = new CompoundTag();
            for (String key : CONFIG_KEYS) if (tag.contains(key)) retainedConfiguration.put(key, tag.get(key).copy());
        } else {
            applyMaterialAttributes(EnumGolemType.getType(material));
            setHealth(savedHealth);
        }
        entityData.set(MATERIAL, material); // Raw identity also survives unsupported/future NBT.
        configurationOpeners.clear();
        retiring = false;
        syncCarried();
        syncWater();
        syncAspectCargo();
        rebuildGoals();
    }
    private static boolean unsupportedExtras(CompoundTag tag) {
        if (tag.contains("Inventory") && !tag.contains("Inventory", Tag.TAG_LIST)
                || tag.contains("Markers") && !tag.contains("Markers", Tag.TAG_LIST)
                || tag.contains("WantedCounts") && !tag.contains("WantedCounts", Tag.TAG_INT_ARRAY)
                || tag.contains("FillAny") && (!tag.contains("FillAny", Tag.TAG_BYTE) || tag.getByte("FillAny") < 0 || tag.getByte("FillAny") > 1)
                || tag.contains("upgrades") && !tag.contains("upgrades", Tag.TAG_BYTE_ARRAY)
                || tag.contains("colors") && !tag.contains("colors", Tag.TAG_BYTE_ARRAY)
                || tag.contains("GuardTargets") && !tag.contains("GuardTargets", Tag.TAG_BYTE)
                || tag.contains("ItemMatch") && !tag.contains("ItemMatch", Tag.TAG_BYTE)) return true;
        if (tag.contains("advanced") && (!tag.contains("advanced", Tag.TAG_BYTE) || tag.getByte("advanced") < 0 || tag.getByte("advanced") > 1)
                || tag.contains("Decoration") && (!tag.contains("Decoration", Tag.TAG_STRING) || !GolemDecorations.valid(tag.getString("Decoration")))
                || !tag.getString("deco").isEmpty()
                || tag.getByte("toggles") != 0) return true;
        if (tag.contains("Core") && tag.contains("GolemCore") && tag.getInt("Core") != tag.getInt("GolemCore")) return true;
        return false;
    }
}
