/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 3:56:13 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.kami;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerWarpGate;
import thaumic.tinkerer.common.content.KamiWorld;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.item.kami.KamiHooks;

/**
 * The block entity of the Celestial Gateway: ten slots for Celestial Pearls (one each), a flag that tells whether the gate
 * refuses teleports to it ({@code locked}), and the teleport itself.
 *
 * <p>The teleport rules are the original's: the destination block entity must be a gateway that is not locked, else the
 * player is told {@code ttmisc.noDest} or {@code ttmisc.noTeleport}; the player is taken off a mount and put 1.6 blocks
 * above the destination block, with the wand sound before (pitch 1) and after (pitch 0.1). A pearl attuned to another
 * dimension takes the player there (1.7.10 looked for a gateway at those coordinates in the player's own dimension, so the
 * destinations screen did not offer such pearls; see docs/deviations/KamiWorld.md).</p>
 */
public class TileWarpGate extends BlockEntity implements Container, ExtendedMenuProvider {
    private static final String TAG_LOCKED = "locked";
    public static final int SLOTS = 10;

    /** True when this gate refuses teleports to it (the button of its screen; the label reads "Allow incoming teleports"). */
    public boolean locked = false;
    private boolean teleportedThisTick = false;
    private final NonNullList<ItemStack> inventorySlots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    /** Client entry: opens the destinations screen for a gate (set by the client registration, a no-op on a server). */
    private static volatile Consumer<TileWarpGate> destinationsOpener = gate -> {};

    public TileWarpGate(BlockPos pos, BlockState state) {
        super(KamiWorld.warpGateType(), pos, state);
    }

    public static void setDestinationsOpener(Consumer<TileWarpGate> opener) {
        destinationsOpener = opener == null ? gate -> {} : opener;
    }

    // ---- teleporting ---------------------------------------------------------------------------------------------

    /**
     * 1.7.10 {@code teleportPlayer(player, coords)}: to the gateway at {@code coords} in the player's own dimension.
     * This is what the Focus of Celestial Recall asks for.
     */
    public static boolean teleportPlayer(ServerPlayer player, BlockPos coords) {
        return teleportPlayer(player, player.level().dimension(), coords);
    }

    /** The gateway at {@code coords} of {@code dimension}; false (and a message to the player) when there is none or it refuses. */
    public static boolean teleportPlayer(ServerPlayer player, ResourceKey<Level> dimension, BlockPos coords) {
        ServerLevel destination = player.server.getLevel(dimension);
        BlockEntity tile = destination == null ? null : destination.getBlockEntity(coords);
        if (tile instanceof TileWarpGate destGate) {
            if (!destGate.locked) {
                Level origin = player.level();
                playWand(origin, player.getX(), player.getY(), player.getZ(), 1F);
                player.stopRiding();
                Vec3 target = new Vec3(coords.getX() + 0.5, coords.getY() + 1.6, coords.getZ() + 0.5);
                if (destination == origin) {
                    player.connection.teleport(target.x, target.y, target.z, player.getYRot(), player.getXRot());
                } else {
                    player.changeDimension(new DimensionTransition(destination, target, Vec3.ZERO, player.getYRot(),
                            player.getXRot(), DimensionTransition.DO_NOTHING));
                }
                playWand(destination, player.getX(), player.getY(), player.getZ(), 0.1F);
                return true;
            } else {
                player.sendSystemMessage(Component.translatable("ttmisc.noTeleport"));
            }
        } else {
            player.sendSystemMessage(Component.translatable("ttmisc.noDest"));
        }
        return false;
    }

    /** {@code playSoundAtEntity(player, "thaumcraft:wand", 1F, pitch)}: heard by everyone near, the player included. */
    private static void playWand(Level level, double x, double y, double z, float pitch) {
        level.playSound(null, x, y, z, ThaumcraftSoundIds.event(ThaumcraftSoundIds.WAND), SoundSource.PLAYERS, 1F, pitch);
    }

    /**
     * 1.7.10 {@code teleportPlayer(player, index)}: to the destination of the pearl in a slot (at most once a tick).
     *
     * @return whether the player was teleported
     */
    public boolean teleportPlayer(ServerPlayer player, int index) {
        if (teleportedThisTick) return false;

        ItemStack stack = index >= 0 && index < getContainerSize() ? getItem(index) : ItemStack.EMPTY;
        if (!stack.isEmpty() && ItemSkyPearl.isAttuned(stack)
                && teleportPlayer(player, ItemSkyPearl.getDim(stack), ItemSkyPearl.getPos(stack))) {
            teleportedThisTick = true;
            return true;
        }
        return false;
    }

    // ---- the tick ------------------------------------------------------------------------------------------------

    /** The space just above the gate where a sneaking player opens the destinations screen. */
    public static AABB spaceAbove(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1);
    }

    /**
     * 1.7.10 {@code updateEntity} (both sides): on the client, the local player sneaking in the space above the gate opens
     * the destinations screen; the flag that limits a gate to one teleport per tick is cleared.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, TileWarpGate gate) {
        if (level.isClientSide) {
            Player local = KamiHooks.localPlayer();
            if (local != null && local.isShiftKeyDown()) {
                for (Player player : level.getEntitiesOfClass(Player.class, spaceAbove(pos))) {
                    if (player == local) {
                        destinationsOpener.accept(gate);
                        break;
                    }
                }
            }
        }
        gate.teleportedThisTick = false;
    }

    // ---- saving and syncing --------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCustomNBT(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCustomNBT(tag, registries);
    }

    public void readCustomNBT(CompoundTag tag, HolderLookup.Provider registries) {
        locked = tag.getBoolean(TAG_LOCKED);
        for (int i = 0; i < inventorySlots.size(); i++) inventorySlots.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventorySlots, registries);
    }

    public void writeCustomNBT(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(TAG_LOCKED, locked);
        ContainerHelper.saveAllItems(tag, inventorySlots, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCustomNBT(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Saves, and tells the clients that watch the gate (its screen and its destinations map read the client copy). */
    public void syncToClients() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** The button of the screen (the server's end of {@code PacketWarpGateButton}). */
    public void setLocked(boolean locked) {
        this.locked = locked;
        syncToClients();
    }

    // ---- the inventory -------------------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return inventorySlots.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventorySlots) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventorySlots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(inventorySlots, slot, count);
        if (!removed.isEmpty()) syncToClients();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventorySlots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventorySlots.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        syncToClients();
    }

    /** 1.7.10 {@code getInventoryStackLimit}: 1. */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** 1.7.10 {@code isItemValidForSlot}: only the Celestial Pearl. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemSkyPearl;
    }

    /** 1.7.10 {@code isUseableByPlayer}: the tile is still there and the player is within 8 blocks. */
    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOTS; i++) inventorySlots.set(i, ItemStack.EMPTY);
    }

    // ---- the menu ------------------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerWarpGate(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
