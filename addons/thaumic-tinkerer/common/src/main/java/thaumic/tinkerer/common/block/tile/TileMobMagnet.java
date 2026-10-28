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

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.item.ItemSoulMould;

/**
 * The Corporeal Attractor: a magnet for living beings, filtered by the entity type of the soul mould in its slot and
 * by adult / child. The adult flag and the slot are shown and changed in a screen (menu data and menu buttons replace
 * the 1.7.10 packet, see docs/deviations/Machines.md).
 */
public class TileMobMagnet extends TileMagnet implements Container {

    private static final String TAG_ADULT = "adultCheck";

    /** Menu data slot of the adult flag: 1 adult, 0 child. */
    public static final int DATA_ADULT = 0;

    public boolean adult = true;
    private NonNullList<ItemStack> inventorySlots = NonNullList.withSize(1, ItemStack.EMPTY);

    /** The data the menu synchronises: the adult flag. */
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_ADULT && adult ? 1 : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_ADULT) setAdult(value != 0);
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public TileMobMagnet(BlockPos pos, BlockState state) {
        super(Machines.TILE_MOB_MAGNET.get(), pos, state);
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    /** Sets the adult flag and sends it to the clients ({@code setAdultSearch} of the peripheral, the menu buttons). */
    public void setAdult(boolean adult) {
        if (this.adult == adult) return;
        this.adult = adult;
        markDirtyAndSync();
    }

    /** The entity types the magnet moves: living beings that are no players, matching the adult flag and the mould. */
    @Override
    protected boolean isSelected(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) return false;

        boolean can;
        if (entity instanceof AgeableMob ageable) can = adult != ageable.isBaby();
        else can = true;

        ItemStack mould = inventorySlots.get(0);
        if (can && !mould.isEmpty()) {
            String pattern = ItemSoulMould.getPatternName(mould);
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            return name.equals(pattern);
        }

        return can;
    }

    // ---- saving and sync ------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ADULT)) adult = tag.getBoolean(TAG_ADULT);
        inventorySlots = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventorySlots, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_ADULT, adult);
        ContainerHelper.saveAllItems(tag, inventorySlots, true, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- Container ------------------------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return inventorySlots.size();
    }

    @Override
    public boolean isEmpty() {
        return inventorySlots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventorySlots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(inventorySlots, slot, amount);
        if (!removed.isEmpty()) markDirtyAndSync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventorySlots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventorySlots.set(slot, stack);
        markDirtyAndSync();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D) <= 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        inventorySlots.clear();
        markDirtyAndSync();
    }
}
