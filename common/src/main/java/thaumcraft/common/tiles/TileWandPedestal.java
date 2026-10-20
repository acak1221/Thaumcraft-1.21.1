package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
<<<<<<< HEAD
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * Original TileWandPedestal: the pedestal that fills a wand or a vis amulet from the nodes around it.
 * <p>
 * It holds one wand or amulet and looks for the nodes in the sixteen blocks around it. Every fifth tick it
 * moves one whole vis of an aspect from a node into the wand, and it keeps one vis of every aspect back
 * unless the wand is iron capped or has a wooden rod. A focus block on top of the pedestal lets it take the
 * primal parts of a compound aspect as well.
 * <p>
 * Oracle: {@code thaumcraft/common/tiles/TileWandPedestal.java}, lines 46-398.
 */
public class TileWandPedestal extends TileThaumcraftInventory implements IAspectContainer {
    /** One whole vis is a hundred centivis in the native port. */
    public static final int WHOLE_VIS = 100;
    private int counter;
    private boolean somethingChanged;
    /** Whether the pedestal is moving vis at the moment. */
    public boolean draining;
    /** The node the client draws the vis from. */
    public int drainX, drainY, drainZ, drainColor;
    private List<BlockPos> nodes;

    public TileWandPedestal(BlockPos pos, BlockState state) {
        // The wand slot is the only slot and it is the one the client sees.
        super(ThaumcraftLegacyTiles.WAND_PEDESTAL_BLOCK_ENTITY.get(), pos, state, 1, new int[]{0});
    }

    /** The nodes the pedestal knows, or null before the first search. */
    public List<BlockPos> nodes() { return nodes == null ? List.of() : List.copyOf(nodes); }
    /** Whether the pedestal is filling the wand at the moment. */
    public boolean isDraining() { return draining; }

    public static void tick(Level level, BlockPos pos, BlockState state, TileWandPedestal tile) {
        if (tile.level != level || !pos.equals(tile.worldPosition) || tile.isRemoved() || tile.getBlockState() != state) return;
        if (tile.nodes == null) tile.findNodes(level);
        ++tile.counter;
        ItemStack stack = tile.getItem(0);
        if (tile.counter % 20 == 0 && tile.somethingChanged && !tile.nodes.isEmpty() && !stack.isEmpty()) {
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            tile.somethingChanged = false;
        }
        if (tile.counter % LegacyTileRules.WAND_PEDESTAL_DRAW_PERIOD == 0 && !tile.nodes.isEmpty() && !stack.isEmpty()) {
            tile.drain(level, stack);
        }
        if (tile.counter % LegacyTileRules.WAND_PEDESTAL_RESCAN_PERIOD == 0) tile.findNodes(level);
    }

    /**
     * TileWandPedestal:182-296 - the pedestal moves one whole vis of an aspect from a node into the wand.
     */
    private void drain(Level level, ItemStack stack) {
        draining = false;
        if (stack.getItem() instanceof ItemWandCasting) {
            WandData data = ItemWandCasting.data(stack);
            int reserve = LegacyTileRules.wandPedestalReserve(data.cap(), data.rod()) * WHOLE_VIS;
            for (BlockPos nodePos : nodes) {
                if (!level.hasChunkAt(nodePos)) continue;
                if (!(level.getBlockEntity(nodePos) instanceof TileNode node)) continue;
                AspectList stored = node.getAspects();
                for (Aspect aspect : Aspect.aspects.values()) {
                    if (stored.getAmount(aspect) <= reserve) continue;
                    if (data.amount(aspect.getTag()) + WHOLE_VIS > data.capacity()) continue;
                    ItemWandCasting.addRealVis(stack, aspect.getTag(), WHOLE_VIS, true);
                    node.takeFromContainer(aspect, WHOLE_VIS);
                    somethingChanged = true;
                    draining = true;
                    drainX = nodePos.getX();
                    drainY = nodePos.getY();
                    drainZ = nodePos.getZ();
                    drainColor = aspect.getColor();
                    setChanged();
                    return;
                }
            }
            return;
        }
        if (stack.getItem() instanceof ItemAmuletVis amulet) {
            int reserve = WHOLE_VIS;
            for (BlockPos nodePos : nodes) {
                if (!level.hasChunkAt(nodePos)) continue;
                if (!(level.getBlockEntity(nodePos) instanceof TileNode node)) continue;
                AspectList stored = node.getAspects();
                for (Aspect aspect : Aspect.aspects.values()) {
                    if (!aspect.isPrimal()) continue;
                    if (stored.getAmount(aspect) <= reserve) continue;
                    if (amulet.getVis(stack, aspect) + WHOLE_VIS > amulet.getMaxVis(stack)) continue;
                    amulet.addRealVis(stack, aspect, WHOLE_VIS, true);
                    node.takeFromContainer(aspect, WHOLE_VIS);
                    somethingChanged = true;
                    draining = true;
                    drainX = nodePos.getX();
                    drainY = nodePos.getY();
                    drainZ = nodePos.getZ();
                    drainColor = aspect.getColor();
                    setChanged();
                    return;
                }
            }
        }
    }

    /** TileWandPedestal:298-309 - the pedestal looks for the nodes in the sixteen blocks around it. */
    private void findNodes(Level level) {
        List<BlockPos> found = new ArrayList<>();
        int radius = LegacyTileRules.WAND_PEDESTAL_SCAN_RADIUS;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos target = worldPosition.offset(dx, dy, dz);
                    if (!level.hasChunkAt(target)) continue;
                    if (!(level.getBlockEntity(target) instanceof TileNode)) continue;
                    found.add(target.immutable());
                }
            }
        }
        nodes = found;
    }

    /** Whether the focus block sits on the pedestal; it lets the pedestal take the primal parts as well. */
    public boolean hasFocus(Level level) {
        BlockPos above = worldPosition.above();
        if (!level.hasChunkAt(above)) return false;
        return level.getBlockState(above).getBlock() instanceof BlockWandPedestal pedestal && pedestal.isFocus();
    }

    /** TileWandPedestal:337-358 - the aspect readout of the pedestal is the whole vis of the wand. */
    @Override public AspectList getAspects() {
        ItemStack stack = getItem(0);
        if (stack.isEmpty()) return null;
        AspectList out = new AspectList();
        if (stack.getItem() instanceof ItemWandCasting) {
            WandData data = ItemWandCasting.data(stack);
            for (String tag : data.vis().keySet()) {
                Aspect aspect = Aspect.getAspect(tag);
                if (aspect == null) continue;
                out.add(aspect, LegacyTileRules.wandPedestalReadout(data.amount(tag)));
            }
            return out;
        }
        if (stack.getItem() instanceof ItemAmuletVis amulet) {
            for (Aspect aspect : Aspect.aspects.values()) {
                int amount = amulet.getVis(stack, aspect);
                if (amount <= 0) continue;
                out.add(aspect, LegacyTileRules.wandPedestalReadout(amount));
            }
            return out;
        }
        return null;
    }

    @Override public void setAspects(AspectList aspects) { }
    @Override public int addToContainer(Aspect tag, int amount) { return 0; }
    @Override public boolean takeFromContainer(Aspect tag, int amount) { return false; }
    @Override public boolean takeFromContainer(AspectList ot) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect tag, int amount) { return false; }
    @Override public boolean doesContainerContain(AspectList ot) { return false; }
    @Override public int containerContains(Aspect tag) { return 0; }
    @Override public boolean doesContainerAccept(Aspect tag) { return true; }

    /** TileWandPedestal:321-323 - the pedestal only accepts a wand or a vis amulet. */
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemWandCasting || stack.getItem() instanceof ItemAmuletVis;
    }

    /** TileWandPedestal:113-121 - the gui name of the pedestal. */
    @Override public Component getDisplayName() {
        return hasCustomName() ? getCustomName() : Component.translatable("container.wandpedestal");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("draining", draining);
=======
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
>>>>>>> port/eldritchblocks
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
<<<<<<< HEAD
        draining = tag.getBoolean("draining");
        nodes = null;
        somethingChanged = false;
        counter = 0;
    }
=======
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
>>>>>>> port/eldritchblocks
}
