/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 19:01:20 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.transvector;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * The Transvector Interface: its sides mimic the sides of the block it is bound to, so the bound block can be reached
 * as if it stood where the interface stands (1.7.10 {@code TileTransvectorInterface}).
 *
 * <p>What is forwarded, to the block entity of the bound block, side for side:</p>
 * <ul>
 *   <li><b>items</b>: as a {@link WorldlyContainer}, so vanilla hoppers and every loader's item transfer, which treat a
 *       block entity that is a container as an inventory, see the bound block's inventory;</li>
 *   <li><b>essentia</b>: {@link IAspectContainer} and {@link IEssentiaTransport}, the API's own interfaces, which the tubes,
 *       jars and the infusion altar of Thaumcraft ask of any block entity.</li>
 * </ul>
 *
 * <p><b>Not forwarded: fluids and energy</b> (1.7.10 {@code IFluidHandler}, CoFH RF and IC2 EU). 1.21.1 has no fluid or
 * energy interface that common code can implement; they are loader capabilities (Fabric Transfer API, NeoForge
 * capabilities). {@link #getTile()} is the single place a loader module needs to forward them from.</p>
 */
public class TileTransvectorInterface extends TileTransvector implements WorldlyContainer, IAspectContainer, IEssentiaTransport {

    public TileTransvectorInterface(BlockPos pos, BlockState state) {
        super(TileBlocks.interfaceType(), pos, state);
    }

    public static int[] buildSlotsForLinearInventory(Container inv) {
        int[] slots = new int[inv.getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;

        return slots;
    }

    /** {@code updateEntity}: every five seconds the neighbours are told to look at the interface again (pipes). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileTransvectorInterface tile) {
        if (level.getGameTime() % 100 == 0) {
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    @Override
    public int getMaxDistance() {
        return TileBlocks.INTERFACE_DISTANCE;
    }

    /** {@code markDirty}: the bound block changed too. */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            BlockEntity tile = getTile();
            if (tile != null) tile.setChanged();
        }
    }

    // ---- items ----------------------------------------------------------------------------------------------------

    @Nullable
    private Container container() {
        return getTile() instanceof Container container ? container : null;
    }

    @Override
    public int getContainerSize() {
        Container container = container();
        return container != null ? container.getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        Container container = container();
        return container == null || container.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        Container container = container();
        return container != null ? container.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        Container container = container();
        return container != null ? container.removeItem(slot, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        Container container = container();
        return container != null ? container.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        Container container = container();
        if (container != null) container.setItem(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        Container container = container();
        return container != null ? container.getMaxStackSize() : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        Container container = container();
        return container != null && container.stillValid(player);
    }

    @Override
    public void startOpen(Player player) {
        Container container = container();
        if (container != null) container.startOpen(player);
    }

    @Override
    public void stopOpen(Player player) {
        Container container = container();
        if (container != null) container.stopOpen(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Container container = container();
        return container != null && container.canPlaceItem(slot, stack);
    }

    @Override
    public void clearContent() {
        // Nothing to clear here: the items are the bound block's.
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        BlockEntity tile = getTile();
        return tile instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(side)
                : tile instanceof Container container ? buildSlotsForLinearInventory(container) : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        BlockEntity tile = getTile();
        return tile instanceof WorldlyContainer worldly ? worldly.canPlaceItemThroughFace(slot, stack, side)
                : tile instanceof Container;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        BlockEntity tile = getTile();
        return tile instanceof WorldlyContainer worldly ? worldly.canTakeItemThroughFace(slot, stack, side)
                : tile instanceof Container;
    }

    // ---- aspects --------------------------------------------------------------------------------------------------

    @Nullable
    private IAspectContainer aspects() {
        return getTile() instanceof IAspectContainer container ? container : null;
    }

    @Override
    public AspectList getAspects() {
        IAspectContainer container = aspects();
        AspectList list = container != null ? container.getAspects() : null;
        return list != null ? list : new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
        IAspectContainer container = aspects();
        if (container != null) container.setAspects(aspects);
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        IAspectContainer container = aspects();
        return container != null && container.doesContainerAccept(aspect);
    }

    /** Returns what did not fit; an interface that is not bound to an aspect container accepts nothing. */
    @Override
    public int addToContainer(Aspect aspect, int amount) {
        IAspectContainer container = aspects();
        return container != null ? container.addToContainer(aspect, amount) : amount;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        IAspectContainer container = aspects();
        return container != null && container.takeFromContainer(aspect, amount);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public boolean takeFromContainer(AspectList aspects) {
        IAspectContainer container = aspects();
        return container != null && container.takeFromContainer(aspects);
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        IAspectContainer container = aspects();
        return container != null && container.doesContainerContainAmount(aspect, amount);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public boolean doesContainerContain(AspectList aspects) {
        IAspectContainer container = aspects();
        return container != null && container.doesContainerContain(aspects);
    }

    @Override
    public int containerContains(Aspect aspect) {
        IAspectContainer container = aspects();
        return container != null ? container.containerContains(aspect) : 0;
    }

    // ---- essentia transport ---------------------------------------------------------------------------------------

    @Nullable
    private IEssentiaTransport transport() {
        return getTile() instanceof IEssentiaTransport transport ? transport : null;
    }

    @Override
    public boolean isConnectable(Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null && transport.canInputFrom(face);
    }

    @Override
    public boolean canOutputTo(Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null && transport.canOutputTo(face);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        IEssentiaTransport transport = transport();
        if (transport != null) transport.setSuction(aspect, amount);
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.getSuctionType(face) : null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.getSuctionAmount(face) : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.takeEssentia(aspect, amount, face) : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.addEssentia(aspect, amount, face) : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.getEssentiaType(face) : null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.getEssentiaAmount(face) : 0;
    }

    @Override
    public int getMinimumSuction() {
        IEssentiaTransport transport = transport();
        return transport != null ? transport.getMinimumSuction() : 0;
    }

    @Override
    public boolean renderExtendedTube() {
        IEssentiaTransport transport = transport();
        return transport != null && transport.renderExtendedTube();
    }
}
