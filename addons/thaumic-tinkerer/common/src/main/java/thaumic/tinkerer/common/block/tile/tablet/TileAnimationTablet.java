/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 15:51:34 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.tablet;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.BlockAnimationTablet;
import thaumic.tinkerer.common.block.tile.container.ContainerAnimationTablet;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * The block entity of the Animation Tablet (Tool Dynamism Tablet): it holds one item, swings it on the block or entity
 * in front of it, and a fake player does what the item does (1.7.10: {@code TileAnimationTablet}).
 *
 * <p>The behaviour is that of the original: the left-click mode attacks entities and breaks blocks (as fast as the item
 * would for a player, with the item's wear); the right-click mode uses the item on the entity or block in front, and
 * places blocks, seeds, redstone and till soil in the block in front of it by clicking the top of the block below it;
 * in redstone mode a swing starts only while the tablet or the block above it is powered. The client animates the swing
 * (the 1.7.10 tile did, from the same counters).</p>
 */
public class TileAnimationTablet extends BlockEntity implements Container, ExtendedMenuProvider {
    private static final String TAG_LEFT_CLICK = "leftClick";
    private static final String TAG_REDSTONE = "redstone";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_MOD = "mod";
    private static final String TAG_ITEM = "Item";

    private static final byte SWING_SPEED = 3;
    private static final byte MAX_DEGREE = 45;
    private static final byte MAX_ITERATIONS = MAX_DEGREE / SWING_SPEED;

    /** Ticks between two failure messages to the players around (the original sent one every tick). */
    private static final int FAILURE_MESSAGE_INTERVAL = 100;

    public boolean leftClick = true;
    public boolean redstone = false;
    private byte prevSwingProgress;
    private byte swingProgress;
    private byte swingMod;

    /** The item in the tablet; {@link ItemStack#EMPTY} when there is none (1.7.10: null). */
    public ItemStack heldItem = ItemStack.EMPTY;

    @Nullable
    private TabletFakePlayer player;
    private boolean isBreaking = false;
    @Nullable
    private BlockPos breakingPos;
    private int breakTicks;
    private int lastBreakStage = -1;
    private int ticksElapsed;
    private int prevTicksElapsed;
    private long lastFailureMessage = Long.MIN_VALUE / 2;

    public TileAnimationTablet(BlockPos pos, BlockState state) {
        super(TileBlocks.animationTabletType(), pos, state);
    }

    // ---- client animation -----------------------------------------------------------------------------------------

    public final float getRenderSwingProgress(float partialTicks) {
        return prevSwingProgress + (swingProgress - prevSwingProgress) * partialTicks;
    }

    public final float getTicksExisted(float partialTicks) {
        return prevTicksElapsed + (ticksElapsed - prevTicksElapsed) * partialTicks;
    }

    /** The way the tablet faces (1.7.10: the metadata, 2 north, 3 south, 4 west, 5 east). */
    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockAnimationTablet.FACING) ? state.getValue(BlockAnimationTablet.FACING) : Direction.NORTH;
    }

    /** The block the tablet works on. */
    public BlockPos getTargetPos() {
        return worldPosition.relative(getFacing());
    }

    // ---- ticking --------------------------------------------------------------------------------------------------

    /** {@code updateEntity}, client branch: the swing animation. */
    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TileAnimationTablet tablet) {
        tablet.prevSwingProgress = tablet.swingProgress;
        tablet.prevTicksElapsed = tablet.ticksElapsed;
        tablet.ticksElapsed += 1;
        if (!tablet.heldItem.isEmpty()) {
            int iterations = Math.min(1, MAX_ITERATIONS);
            for (int i = 0; i < iterations; i++) {
                tablet.swingProgress += tablet.swingMod;
                if (tablet.swingProgress >= MAX_DEGREE) {
                    tablet.swingMod = -SWING_SPEED;
                } else if (tablet.swingProgress <= 0) {
                    tablet.stopSwinging();
                }
            }
        } else {
            tablet.stopSwinging();
        }
    }

    /** {@code updateEntity}, server branch. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TileAnimationTablet tablet) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        try {
            tablet.tickServer(serverLevel);
        } catch (Throwable e) {
            tablet.reportFailure(serverLevel, e);
        }
    }

    private void tickServer(ServerLevel serverLevel) {
        if (heldItem.isEmpty()) {
            if (isBreaking) stopBreaking();
            stopSwinging();
            return;
        }
        TabletFakePlayer player = player(serverLevel);
        player.onUpdate();

        ItemStack stack = heldItem;
        HitResult hit = calculateHit(player);
        swingProgress += swingMod;
        if (swingProgress >= MAX_DEGREE) {
            if (hit != null) swingHit(player, stack, hit);
            swingMod = -SWING_SPEED;
        } else if (swingProgress <= 0) {
            stopSwinging();
        }

        if (isBreaking) {
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                stopBreaking();
                return;
            }
            updateBlockRemoving(player, ((BlockHitResult) hit).getBlockPos());
        }
        if (isIdle() && hit != null) {
            if (redstone && !isBreaking) {
                boolean power = level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above());
                if (!power) return;
            }
            if (hit.getType() == HitResult.Type.MISS) {
                BlockHitResult below = (BlockHitResult) hit;
                if (leftClick || !canUseItem(heldItem, level.getBlockState(below.getBlockPos()))) return;
            }
            initiateSwing();
        }
    }

    /** The fake player of this tablet, created the first time it is needed. */
    private TabletFakePlayer player(ServerLevel serverLevel) {
        if (player == null) player = new TabletFakePlayer(serverLevel, this);
        return player;
    }

    /** What a right click with this item does in the block in front of the tablet (1.7.10 {@code canUseItem}). */
    private static boolean canUseItem(ItemStack stack, BlockState below) {
        Item item = stack.getItem();
        // ItemBlock, ItemReed, ItemSeeds and ItemRedstone are all block items in 1.21.1.
        if (item instanceof BlockItem) return true;
        return item instanceof HoeItem && (below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK));
    }

    private void reportFailure(ServerLevel serverLevel, Throwable e) {
        ThaumicTinkerer.LOG.error("The Animation Tablet at {} failed", worldPosition, e);
        long now = serverLevel.getGameTime();
        if (now - lastFailureMessage < FAILURE_MESSAGE_INTERVAL) return;
        lastFailureMessage = now;
        List<Player> list = serverLevel.getEntitiesOfClass(Player.class, new AABB(worldPosition.getX() - 8, worldPosition.getY() - 8,
                worldPosition.getZ() - 8, worldPosition.getX() + 8, worldPosition.getY() + 8, worldPosition.getZ() + 8));
        for (Player nearby : list) {
            nearby.sendSystemMessage(Component.literal("Something went wrong with a Tool Dynamism Tablet! Check your game log.")
                    .withStyle(ChatFormatting.RED));
            nearby.sendSystemMessage(Component.literal(String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }
    }

    // ---- the swing ------------------------------------------------------------------------------------------------

    /** What the tablet does when the swing reaches its end: {@code swingHit}. */
    private void swingHit(TabletFakePlayer player, ItemStack stack, HitResult hit) {
        if (leftClick) {
            if (hit.getType() == HitResult.Type.ENTITY) {
                player.attackEntity(((EntityHitResult) hit).getEntity());
                updateState(player);
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                if (!isBreaking) {
                    BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                        isBreaking = true;
                        startBreaking(player, pos);
                        updateState(player);
                    }
                }
            }
        } else {
            if (hit.getType() == HitResult.Type.ENTITY) {
                if (player.interactWithEntity(((EntityHitResult) hit).getEntity())) updateState(player);
                return;
            }
            // The block hit, or the top of the block below (a MISS in the original): the right click of a player.
            InteractionResult used = player.useItemOnBlock((BlockHitResult) hit);
            if (used.consumesAction()) {
                updateState(player);
                return;
            }
            if (player.useItemInAir().consumesAction()) updateState(player);
        }
    }

    public final boolean isIdle() {
        return swingMod == 0;
    }

    public final boolean getIsBreaking() {
        return isBreaking;
    }

    private void stopSwinging() {
        swingProgress = 0;
        swingMod = 0;
    }

    public void initiateSwing() {
        swingMod = SWING_SPEED;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
    }

    /** The client side of {@link #initiateSwing()}: the block event restarts the swing animation. */
    @Override
    public boolean triggerEvent(int id, int type) {
        if (level == null || !level.isClientSide) return true;
        if (id == 0) {
            swingMod = SWING_SPEED;
            swingProgress = 0;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    // ---- breaking -------------------------------------------------------------------------------------------------

    /** {@code onBlockClicked}: the first hit; an instantly breakable block goes at once. */
    private void startBreaking(TabletFakePlayer player, BlockPos pos) {
        breakingPos = pos;
        breakTicks = 0;
        lastBreakStage = -1;
        BlockState state = level.getBlockState(pos);
        state.attack(level, pos, player);
        if (state.getDestroyProgress(player, level, pos) >= 1.0F) {
            player.gameMode.destroyBlock(pos);
        }
    }

    /**
     * {@code updateBlockRemoving}: the progress after n ticks is the per-tick progress times n + 1, the crack stage is
     * shown to the players around, and the block breaks (with the drops of the held tool) at 100%.
     */
    private void updateBlockRemoving(TabletFakePlayer player, BlockPos pos) {
        if (breakingPos == null || !breakingPos.equals(pos)) {
            startBreaking(player, pos);
            return;
        }
        breakTicks++;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        float progress = state.getDestroyProgress(player, level, pos) * (breakTicks + 1);
        int stage = (int) (progress * 10.0F);
        if (stage != lastBreakStage) {
            level.destroyBlockProgress(player.getId(), pos, stage);
            lastBreakStage = stage;
        }
        if (progress >= 1.0F) {
            player.gameMode.destroyBlock(pos);
            updateState(player);
        }
    }

    /** {@code stopBreaking}: the crack disappears and the tablet is free to swing again. */
    private void stopBreaking() {
        isBreaking = false;
        if (player != null && level != null) {
            level.destroyBlockProgress(player.getId(), breakingPos != null ? breakingPos : getTargetPos(), -1);
        }
        breakingPos = null;
        breakTicks = 0;
        lastBreakStage = -1;
    }

    /** What the player holds after an action is what the tablet holds; then the clients learn about it. */
    private void updateState(TabletFakePlayer player) {
        ItemStack stack = player.getHeldItem();
        if (stack.isEmpty()) {
            this.heldItem = ItemStack.EMPTY;
            player.syncSlots();
        } else {
            this.heldItem = stack;
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // ---- what is in front -----------------------------------------------------------------------------------------

    /**
     * Casts a ray between the tablet's centre and the middle of the box of the block in front to find the side and the
     * hit point; works for both entities and blocks, whichever is closer wins. When nothing is there the result is a
     * MISS on the top of the block below (so that placing blocks works), as in the original.
     */
    @Nullable
    private HitResult calculateHit(TabletFakePlayer player) {
        BlockPos target = getTargetPos();
        Vec3 position = Vec3.atCenterOf(worldPosition);
        HitResult hit = null;

        BlockState state = level.getBlockState(target);
        if (!state.isAir()) {
            VoxelShape shape = state.getShape(level, target);
            if (!shape.isEmpty()) {
                AABB aabb = shape.bounds().move(target);
                BlockHitResult clipped = shape.clip(position, aabb.getCenter(), target);
                if (clipped != null) hit = clipped;
            }
        }

        List<Entity> entities = level.getEntities(player, new AABB(target));
        if (!entities.isEmpty()) {
            double dist = hit == null ? Double.MAX_VALUE : position.distanceToSqr(hit.getLocation());
            Entity targetedEntity = null;
            Vec3 hitVec = null;
            for (Entity e : entities) {
                if (!e.isPickable()) continue;
                float borderSize = e.getPickRadius();
                AABB aabb = e.getBoundingBox().inflate(borderSize);
                java.util.Optional<Vec3> intercept = aabb.clip(position, aabb.getCenter());
                if (aabb.contains(position)) {
                    targetedEntity = e;
                    hitVec = intercept.orElse(position);
                    break;
                }
                if (intercept.isPresent()) {
                    double entityDist = position.distanceToSqr(intercept.get());
                    if (dist > entityDist) {
                        hitVec = intercept.get();
                        targetedEntity = e;
                        dist = entityDist;
                    }
                }
            }
            if (targetedEntity != null) hit = new EntityHitResult(targetedEntity, hitVec);
        }

        if (hit == null) {
            // Use the block beneath.
            BlockPos below = target.below();
            BlockState belowState = level.getBlockState(below);
            if (!belowState.isAir()) {
                VoxelShape shape = belowState.getShape(level, below);
                if (!shape.isEmpty()) {
                    AABB aabb = shape.bounds().move(below);
                    Vec3 center = aabb.getCenter();
                    hit = BlockHitResult.miss(new Vec3(center.x, aabb.maxY, center.z), Direction.UP, below);
                }
            }
        }
        return hit;
    }

    // ---- CC/OC peripheral surface of the original, as plain methods ------------------------------------------------

    /** {@code trigger}: false while the tablet is busy. */
    public boolean trigger() {
        if (!isIdle()) return false;
        initiateSwing();
        return true;
    }

    /** {@code getRotation}: 0 north, 1 south, 2 west, 3 east (the metadata minus two). */
    public int getRotation() {
        return switch (getFacing()) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    /** {@code setRotation}; a value above 3 is refused as in the original (a negative one too: there is no such side). */
    public void setRotation(int rotation) {
        if (rotation < 0 || rotation > 3) throw new IllegalArgumentException("Invalid value: " + rotation + ".");
        Direction facing = switch (rotation) {
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            case 3 -> Direction.EAST;
            default -> Direction.NORTH;
        };
        level.setBlock(worldPosition, getBlockState().setValue(BlockAnimationTablet.FACING, facing), Block.UPDATE_ALL);
    }

    public void setLeftClick(boolean value) {
        this.leftClick = value;
        syncToClients();
    }

    public void setRedstone(boolean value) {
        this.redstone = value;
        syncToClients();
    }

    private void syncToClients() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // ---- saving and syncing ---------------------------------------------------------------------------------------

    private void writeCustom(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(TAG_LEFT_CLICK, leftClick);
        tag.putBoolean(TAG_REDSTONE, redstone);
        tag.put(TAG_ITEM, heldItem.saveOptional(registries));
    }

    private void readCustom(CompoundTag tag, HolderLookup.Provider registries) {
        leftClick = tag.getBoolean(TAG_LEFT_CLICK);
        redstone = tag.getBoolean(TAG_REDSTONE);
        heldItem = ItemStack.parseOptional(registries, tag.getCompound(TAG_ITEM));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte(TAG_PROGRESS, swingProgress);
        tag.putByte(TAG_MOD, swingMod);
        writeCustom(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // An update packet carries the custom data only: it must not stop a swing that is running on the client.
        if (tag.contains(TAG_PROGRESS)) swingProgress = tag.getByte(TAG_PROGRESS);
        if (tag.contains(TAG_MOD)) swingMod = tag.getByte(TAG_MOD);
        readCustom(tag, registries);
    }

    /** A client ignores an update whose tag is empty; the custom data is never empty, as it carries the item. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCustom(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        if (isBreaking && level != null && !level.isClientSide) stopBreaking();
        player = null;
        super.setRemoved();
    }

    // ---- the one-slot inventory ----------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return heldItem.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? heldItem : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot == 0 && !heldItem.isEmpty()) {
            ItemStack removed = heldItem.split(count);
            if (heldItem.isEmpty()) heldItem = ItemStack.EMPTY;
            syncToClients();
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack removed = heldItem;
        heldItem = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        heldItem = stack;
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        syncToClients();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64;
    }

    @Override
    public void clearContent() {
        heldItem = ItemStack.EMPTY;
    }

    // ---- the menu -------------------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerAnimationTablet(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
