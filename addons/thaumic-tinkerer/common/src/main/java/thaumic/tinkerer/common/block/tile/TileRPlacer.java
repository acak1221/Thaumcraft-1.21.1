/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlockEntity;
import thaumic.tinkerer.common.block.camo.PulseDeviceBlock;
import thaumic.tinkerer.common.block.camo.RedstonePulseReceiver;
import thaumic.tinkerer.common.block.tile.container.ContainerRemotePlacer;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.core.fakeplayer.TinkererFakePlayer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/**
 * The Remote Placer: on a redstone pulse it uses the item in its slot on the empty block one to four blocks away in the
 * direction it faces (1.7.10 {@code TileRPlacer}). A fake player does the placing, first as a click on the block
 * ({@code onItemUse}) and, when that does nothing, as a click into the air ({@code onItemRightClick}).
 */
public class TileRPlacer extends CamoDeviceBlockEntity implements Container, ExtendedMenuProvider, RedstonePulseReceiver {

    private static final String TAG_BLOCKS = "blocks";

    /** The profile of Forge's {@code FakePlayerFactory.getMinecraft} player, which the original placer used. */
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");

    /**
     * The original aimed an east-facing placer to the west: orientation 5 used {@code x - blocks}, a copy of the case
     * for orientation 4. Kept (see docs/deviations/TileBlocks.md); set to false to aim east.
     */
    public static final boolean EAST_AIMS_WEST_AS_IN_1_7_10 = true;

    /** The distance of the target in blocks, 1 to 4 (set in the screen). */
    public int blocks = 1;
    private final NonNullList<ItemStack> inventorySlots = NonNullList.withSize(1, ItemStack.EMPTY);
    @Nullable
    private TinkererFakePlayer player;

    public TileRPlacer(BlockPos pos, BlockState state) {
        super(TileBlocks.placerType(), pos, state);
    }

    /** The direction the placer faces: the block state's facing ({@code orientation} in 1.7.10). */
    public Direction getOrientation() {
        BlockState state = getBlockState();
        return state.hasProperty(PulseDeviceBlock.FACING) ? state.getValue(PulseDeviceBlock.FACING) : Direction.NORTH;
    }

    /** The block the placer works on. */
    public BlockPos getTargetPos() {
        Direction orientation = getOrientation();
        if (orientation == Direction.EAST && EAST_AIMS_WEST_AS_IN_1_7_10) orientation = Direction.WEST;
        return worldPosition.relative(orientation, blocks);
    }

    public void setBlocks(int blocks) {
        this.blocks = Mth.clamp(blocks, 1, 4);
        syncToClients();
    }

    private void syncToClients() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // ---- the pulse ------------------------------------------------------------------------------------------------

    @Override
    public void receiveRedstonePulse() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ItemStack stack = inventorySlots.get(0);
        if (stack.isEmpty()) return;

        BlockPos target = getTargetPos();
        if (!serverLevel.hasChunkAt(target) || !serverLevel.getBlockState(target).isAir()) return;

        if (player == null) player = new TinkererFakePlayer(serverLevel, PROFILE);
        Direction orientation = getOrientation();
        aimPlayer(player, orientation);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);

        Direction side = orientation.getOpposite();
        // 1.7.10 passed the hit as (0, 0, 0): the lower corner of the block.
        BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(target), side, target, false);
        boolean done = player.useItemOnBlock(hit).consumesAction();
        if (!done) {
            player.useItemInAir();
        }

        // What the item became (a bucket that was emptied, an item that was used up) stays in the slot.
        ItemStack after = player.getInventory().getItem(0);
        player.getInventory().setItem(0, ItemStack.EMPTY);
        inventorySlots.set(0, after.isEmpty() ? ItemStack.EMPTY : after);
        player.dropAllExcept();
        syncToClients();
    }

    /** The fake player stands in the placer and looks the way it faces. */
    private void aimPlayer(TinkererFakePlayer fake, Direction orientation) {
        float yaw = orientation.getAxis().isHorizontal() ? orientation.toYRot() : 0F;
        float pitch = orientation == Direction.DOWN ? 90F : orientation == Direction.UP ? -90F : 0F;
        fake.placeAt(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, yaw, pitch);
    }

    @Override
    public void setRemoved() {
        player = null;
        super.setRemoved();
    }

    // ---- saving and syncing ---------------------------------------------------------------------------------------

    @Override
    protected void writeCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeCustomData(tag, registries);
        tag.putInt(TAG_BLOCKS, blocks);
        ContainerHelper.saveAllItems(tag, inventorySlots, registries);
    }

    @Override
    protected void readCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readCustomData(tag, registries);
        blocks = Mth.clamp(tag.getInt(TAG_BLOCKS), 1, 4);
        inventorySlots.clear();
        for (int i = 0; i < 1; i++) inventorySlots.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventorySlots, registries);
    }

    // ---- the one-slot inventory ----------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return inventorySlots.size();
    }

    @Override
    public boolean isEmpty() {
        return inventorySlots.get(0).isEmpty();
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

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    /** {@code isItemValidForSlot}: block items only. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64;
    }

    @Override
    public void clearContent() {
        inventorySlots.clear();
        for (int i = 0; i < 1; i++) inventorySlots.set(i, ItemStack.EMPTY);
    }

    // ---- the menu -------------------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerRemotePlacer(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
