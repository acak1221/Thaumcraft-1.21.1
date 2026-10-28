/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [28 Sep 2013, 19:34:46 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.blocks.BlocksTC;
import thaumic.tinkerer.common.content.Machines;

/**
 * The Essentia Funnel: holds a jar of essentia and, every tick, moves one point of its essentia into the jar that the
 * hopper under the funnel points at.
 */
public class TileFunnel extends BlockEntity implements WorldlyContainer, IAspectContainer {

    private NonNullList<ItemStack> inventorySlots = NonNullList.withSize(1, ItemStack.EMPTY);

    public TileFunnel(BlockPos pos, BlockState state) {
        super(Machines.TILE_FUNNEL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileFunnel funnel) {
        funnel.update(level, pos);
    }

    private void update(Level level, BlockPos pos) {
        ItemStack jar = inventorySlots.get(0);
        if (!isFilledJar(jar)) return;
        IEssentiaContainerItem item = (IEssentiaContainerItem) jar.getItem();

        AspectList aspectList = item.getAspects(jar);
        if (aspectList == null || aspectList.size() != 1) return;
        Aspect aspect = aspectList.getAspects()[0];

        BlockPos hopperPos = pos.below();
        BlockState hopper = level.getBlockState(hopperPos);
        if (!hopper.is(Blocks.HOPPER)) return;

        BlockPos targetPos = hopperPos.relative(hopper.getValue(HopperBlock.FACING));
        BlockState targetState = level.getBlockState(targetPos);
        if (!(targetState.is(BlocksTC.jar()) || targetState.is(BlocksTC.voidJar()))) return;
        if (!(level.getBlockEntity(targetPos) instanceof IAspectContainer target)) return;

        AspectList contents = target.getAspects();
        // An empty jar takes the aspect when its label allows it; a jar with the same aspect takes more while it has
        // room (the void jar always has: addToContainer reports what did not fit, and a void jar swallows the surplus).
        // 1.7.10 read the first aspect of an empty list for a labelled empty jar of another aspect, which crashed.
        boolean accepts;
        if (contents == null || contents.size() == 0) accepts = target.doesContainerAccept(aspect);
        else accepts = contents.getAspects()[0] == aspect;

        if (accepts && target.addToContainer(aspect, 1) == 0) {
            item.setAspects(jar, aspectList.remove(aspect, 1));
            // An emptied jar without a label becomes an empty jar (the jar item does that itself); a labelled one stays.
            markDirtyAndSync();
        }
    }

    // ---- jars -----------------------------------------------------------------------------------------------

    /** The jar items of Thaumcraft: the ordinary and the void jar, filled or not. */
    public static boolean isJarItem(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IEssentiaContainerItem)) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "thaumcraft".equals(id.getNamespace()) && (id.getPath().equals("jar") || id.getPath().equals("jar_void"));
    }

    /**
     * {@code ItemJarFilled} of 1.7.10: a jar that holds essentia, or an empty one that carries a label. A jar that holds
     * nothing and has no label is the plain, stackable jar block item.
     */
    public static boolean isFilledJar(ItemStack stack) {
        if (!isJarItem(stack)) return false;
        AspectList contents = ((IEssentiaContainerItem) stack.getItem()).getAspects(stack);
        return contents != null && contents.size() > 0 || stack.getMaxStackSize() == 1;
    }

    // ---- saving and sync ------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventorySlots = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventorySlots, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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

    public void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- inventory ------------------------------------------------------------------------------------------

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

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[0] : new int[] { 0 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return isFilledJar(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != Direction.DOWN;
    }

    // ---- IAspectContainer: the aspects of the jar, read only --------------------------------------------------

    @Override
    public AspectList getAspects() {
        ItemStack stack = inventorySlots.get(0);
        if (!stack.isEmpty() && stack.getItem() instanceof IEssentiaContainerItem container) {
            AspectList aspects = container.getAspects(stack);
            if (aspects != null) return aspects;
        }
        return new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {}

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return false;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return false;
    }

    @Override
    @Deprecated
    public boolean takeFromContainer(AspectList aspects) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return false;
    }

    @Override
    @Deprecated
    public boolean doesContainerContain(AspectList aspects) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }
}
