package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileWandPedestal: the wand pedestal. It refills the wand or the
 * vis amulet that stands on it from the nearby nodes.
 */
public final class TileWandPedestal extends BlockEntity implements WorldlyContainer {
    private static final int[] SLOTS = new int[] {0};
    private ItemStack[] inventory = new ItemStack[1];
    private Direction facing = Direction.NORTH;
    int counter = 0;
    boolean somethingChanged = false;
    public boolean draining = false;
    public int drainX = 0;
    public int drainY = 0;
    public int drainZ = 0;
    public int drainColor = 0;

    public TileWandPedestal(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.WAND_PEDESTAL_TILE.get(), pos, state);
    }

    public Direction getFacing() { return facing; }

    public void setFacing(Direction facing) {
        this.facing = facing == null ? Direction.NORTH : facing;
        setChanged();
    }

    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return inventory[0] == null || inventory[0].isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 && inventory[0] != null ? inventory[0] : ItemStack.EMPTY; }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || inventory[0] == null) return ItemStack.EMPTY;
        ItemStack removed = inventory[0].split(amount);
        if (inventory[0].isEmpty()) inventory[0] = null;
        setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0 || inventory[0] == null) return ItemStack.EMPTY;
        ItemStack removed = inventory[0];
        inventory[0] = null;
        setChanged();
        return removed;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        if (stack != null && stack.getCount() > 1) stack.setCount(1);
        inventory[0] = stack;
        setChanged();
    }

    @Override public void clearContent() { inventory[0] = null; setChanged(); }

    /** Original TileWandPedestal:277-284 - only a wand or a vis amulet is accepted. */
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack != null && (stack.getItem() instanceof ItemWandCasting || stack.getItem() instanceof ItemAmuletVis);
    }

    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValid(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }

    @Override public boolean stillValid(Player player) {
        if (level == null) return false;
        if (player.level() != level) return false;
        if (!level.hasChunkAt(worldPosition)) return false;
        if (level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) <= 64.0D;
    }

    /** Original TileWandPedestal:186-197 - the nodes around the pedestal. */
    private List<BlockPos> findNodes() {
        List<BlockPos> nodes = new ArrayList<>();
        if (level == null) return nodes;
        for (int xx = -8; xx <= 8; xx++) {
            for (int yy = -8; yy <= 8; yy++) {
                for (int zz = -8; zz <= 8; zz++) {
                    BlockPos pos = worldPosition.offset(xx, yy, zz);
                    if (level.getBlockEntity(pos) instanceof thaumcraft.common.tiles.TileNode) nodes.add(pos);
                }
            }
        }
        return nodes;
    }

    /** Original TileWandPedestal:198-268 - the pedestal refills the wand or the amulet. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileWandPedestal pedestal) {
        if (level.isClientSide) return;
        pedestal.counter++;
        if (pedestal.counter % 5 != 0) return;
        ItemStack held = pedestal.getItem(0);
        if (held.isEmpty()) return;
        boolean wand = held.getItem() instanceof ItemWandCasting;
        boolean amulet = held.getItem() instanceof ItemAmuletVis;
        if (!wand && !amulet) return;
        pedestal.draining = false;
        for (BlockPos nodePos : pedestal.findNodes()) {
            if (!(level.getBlockEntity(nodePos) instanceof thaumcraft.common.tiles.TileNode node)) continue;
            AspectList aspects = node.getAspects();
            for (Aspect aspect : aspects.getAspects()) {
                if (aspect == null || aspect.isPrimal()) continue;
                if (aspects.getAmount(aspect) <= 1) continue;
                if (wand) {
                    if (ItemWandCasting.getVis(held, aspect.getTag()) >= ItemWandCasting.getMaxVis(held)) continue;
                    ItemWandCasting.addRealVis(held, aspect.getTag(), 1, true);
                } else {
                    ItemAmuletVis amuletItem = (ItemAmuletVis) held.getItem();
                    if (amuletItem.getVis(held, aspect) >= amuletItem.getMaxVis(held)) continue;
                    amuletItem.addVis(held, aspect, 1, true);
                }
                node.takeFromContainer(aspect, 1);
                pedestal.somethingChanged = true;
                pedestal.draining = true;
                pedestal.drainX = nodePos.getX();
                pedestal.drainY = nodePos.getY();
                pedestal.drainZ = nodePos.getZ();
                pedestal.drainColor = aspect.getColor();
                pedestal.setChanged();
                return;
            }
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = Direction.from3DDataValue(tag.getByte("facing"));
        inventory = new ItemStack[1];
        if (tag.contains("Items")) {
            var items = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i);
                int slot = entry.getByte("Slot");
                if (slot < 0 || slot >= inventory.length) continue;
                inventory[slot] = ItemStack.parse(registries, entry.getCompound("stack")).orElse(ItemStack.EMPTY);
            }
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", (byte) facing.get3DDataValue());
        var items = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null || inventory[i].isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            entry.put("stack", inventory[i].save(registries));
            items.add(entry);
        }
        tag.put("Items", items);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
