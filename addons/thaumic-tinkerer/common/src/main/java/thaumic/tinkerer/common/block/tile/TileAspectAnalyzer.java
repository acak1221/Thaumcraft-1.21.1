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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.content.Machines;

/**
 * The inventory of the Aspectalyzer: one slot of up to 64 items. 1.7.10 made it a ComputerCraft / OpenComputers
 * peripheral; the four peripheral calls ({@code hasItem}, {@code itemHasAspects}, {@code getAspects},
 * {@code getAspectCount}) are kept as plain methods, the two computer mods are not ported (see
 * docs/deviations/Machines.md).
 */
public class TileAspectAnalyzer extends BlockEntity implements Container {

    private NonNullList<ItemStack> inventorySlots = NonNullList.withSize(1, ItemStack.EMPTY);

    public TileAspectAnalyzer(BlockPos pos, BlockState state) {
        super(Machines.TILE_ASPECT_ANALYZER.get(), pos, state);
    }

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

    // ---- Container ----------------------------------------------------------------------------------------

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
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventorySlots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventorySlots.set(slot, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
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
        setChanged();
    }

    // ---- the peripheral calls of 1.7.10 ---------------------------------------------------------------------

    /** {@code hasItem}: whether the slot holds an item. */
    public boolean hasItemMethod() {
        return !getItem(0).isEmpty();
    }

    /**
     * The aspects of the item in the slot, as a scan of it gives them (the object tags plus the contents of an
     * essentia container); null when the slot is empty.
     */
    public AspectList getAspectList() {
        ItemStack stack = getItem(0);
        if (stack.isEmpty() || level == null) return null;
        return ThaumcraftApiHelper.getObjectAspects(stack, level);
    }

    /** {@code itemHasAspects}: whether the item in the slot has any aspect. */
    public boolean itemHasAspectsMethod() {
        AspectList aspects = getAspectList();
        return aspects != null && aspects.size() > 0;
    }

    /** {@code getAspects}: the tags of the aspects of the item, sorted as the game sorts aspects (1-based in Lua). */
    public List<String> getAspectsMethod() {
        AspectList aspects = getAspectList();
        if (aspects == null) return List.of();
        return java.util.Arrays.stream(aspects.getAspectsSorted()).map(Aspect::getTag).toList();
    }

    /** {@code getAspectCount}: the amount of every aspect of the item, by tag. */
    public Map<String, Integer> getAspectsAmountsMethod() {
        AspectList aspects = getAspectList();
        Map<String, Integer> amounts = new LinkedHashMap<>();
        if (aspects == null) return amounts;
        for (Aspect aspect : aspects.getAspectsSorted()) amounts.put(aspect.getTag(), aspects.getAmount(aspect));
        return amounts;
    }
}
