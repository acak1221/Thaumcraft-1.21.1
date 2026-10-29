/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Nov 30, 2013, 5:39:09 PM (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumic.tinkerer.common.block.BlockRepairer;
import thaumic.tinkerer.common.content.Machines;

/**
 * The Thaumic Restorer. Every ten ticks it draws one essentia from the tube on its front (the aspect the tube sucks:
 * instrumentum 8, fabrico 5 or ordo 3 points of durability) and mends the tool in its slot.
 */
public class TileRepairer extends BlockEntity implements WorldlyContainer, IAspectContainer, IEssentiaTransport {

    private static final String TAG_TOOK_LAST_TICK = "tookLastTick";
    /** Durability points mended per essentia, in the order the aspects are tried (1.7.10: an unordered map). */
    private static final Map<Aspect, Integer> repairValues = new LinkedHashMap<>();

    static {
        repairValues.put(Aspect.TOOL, 8);
        repairValues.put(Aspect.CRAFT, 5);
        repairValues.put(Aspect.ORDER, 3);
    }

    public int ticksExisted = 0;
    public boolean tookLastTick = true;
    /** Whether the block gave redstone power when the neighbours were last told. */
    private boolean powering = false;
    private NonNullList<ItemStack> inventorySlots = NonNullList.withSize(1, ItemStack.EMPTY);

    public TileRepairer(BlockPos pos, BlockState state) {
        super(Machines.TILE_REPAIRER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileRepairer repairer) {
        repairer.update(level, pos, state);
    }

    private void update(Level level, BlockPos pos, BlockState state) {
        if (++ticksExisted % 10 == 0) {
            if (level.isClientSide) {
                if (tookLastTick) {
                    ThaumcraftClientFX.sparkle(level, pos.getX() + 0.25 + level.random.nextDouble() / 2,
                            pos.getY() + 1 + level.random.nextDouble() / 2, pos.getZ() + 0.25 + level.random.nextDouble() / 2,
                            0);
                }
                return;
            }

            boolean oldTookLastTick = tookLastTick;
            tookLastTick = false;

            ItemStack stack = inventorySlots.get(0);
            if (!stack.isEmpty() && stack.getDamageValue() > 0) {
                int essentia = drawEssentia();
                int dmg = stack.getDamageValue();
                int newDmg = Math.max(0, dmg - essentia);
                stack.setDamageValue(newDmg);
                tookLastTick = newDmg != dmg;
                markDirtyAndSync();
            }
            if (tookLastTick != oldTookLastTick) level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // ---- saving and sync ------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tookLastTick = tag.getBoolean(TAG_TOOK_LAST_TICK);
        inventorySlots = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventorySlots, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_TOOK_LAST_TICK, tookLastTick);
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

    /** {@code markDirty} of the original: save and send to the clients. */
    public void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // The repairer is a redstone source while it holds a damaged tool. 1.7.10 never told the neighbours when
            // that changed, so the signal showed up only with the next block update; here they are told.
            boolean nowPowering = isPowering();
            if (nowPowering != powering) {
                powering = nowPowering;
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }
    }

    /** True while the slot holds a tool with damage: the repairer then gives a strong redstone signal of 15. */
    public boolean isPowering() {
        ItemStack stack = inventorySlots.get(0);
        return !stack.isEmpty() && stack.getDamageValue() != 0;
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
        return new int[] { 0 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return isRepairable(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return !isRepairable(stack);
    }

    /** True for a damaged tool (the repairer takes nothing else and gives back only a mended one). */
    public static boolean isRepairable(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0;
    }

    private boolean containsRepairableStack() {
        return isRepairable(inventorySlots.get(0));
    }

    // ---- essentia -------------------------------------------------------------------------------------------

    private int drawEssentia() {
        Direction orientation = getOrientation();
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ(), orientation);
        if (te != null) {
            return drawEssentia((IEssentiaTransport) te, orientation.getOpposite(), getSuctionAmount(orientation));
        }
        return 0;
    }

    /**
     * Takes one essentia from a neighbouring transport and returns the durability points it mends, or 0. The
     * neighbour must be willing to output on its face {@code side}, suck one of the three mending aspects (instrumentum
     * 8, fabrico 5, ordo 3) and suck less than the repairer does ({@code ownSuction}).
     */
    public static int drawEssentia(IEssentiaTransport source, Direction side, int ownSuction) {
        if (!source.canOutputTo(side)) return 0;

        for (Map.Entry<Aspect, Integer> repair : repairValues.entrySet()) {
            Aspect aspect = repair.getKey();
            if (source.getSuctionType(side) == aspect && source.getSuctionAmount(side) < ownSuction
                    && source.takeEssentia(aspect, 1, side) == 1) {
                return repair.getValue();
            }
        }
        return 0;
    }

    /** The side of the tube connection: the way the block faces. */
    public Direction getOrientation() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockRepairer.FACING) ? state.getValue(BlockRepairer.FACING) : Direction.NORTH;
    }

    @Override
    public AspectList getAspects() {
        ItemStack stack = inventorySlots.get(0);
        if (stack.isEmpty()) return new AspectList();
        return new AspectList().add(Aspect.ENTROPY, stack.getDamageValue());
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

    @Override
    public boolean isConnectable(Direction face) {
        return face == getOrientation();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return isConnectable(face);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {}

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return face == getOrientation() && containsRepairableStack() ? 128 : 0;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return face == getOrientation() ? Aspect.TOOL : null;
    }
}
