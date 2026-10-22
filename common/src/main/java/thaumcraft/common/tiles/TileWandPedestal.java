package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileWandPedestal: the pedestal that fills a wand or a vis amulet from the nodes around it.
 * <p>
 * It holds one wand or amulet and looks for the nodes in the sixteen blocks around it. Every fifth tick it
 * moves one whole vis of an aspect from a node into the wand, and it keeps one vis of every aspect back
 * unless the wand is iron capped or has a wooden rod. A focus block on top of the pedestal lets it take the
 * primal parts of a compound aspect as well.
 * <p>
 * The original wand pedestal is the metadata five of the stone device family. The native port has that
 * metadata in the stone device block and the pedestal with its focus block as the wand pedestal block, so
 * the tile takes the block entity type of the block it belongs to.
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
    /** The direction the pedestal was placed in. */
    private Direction facing = Direction.NORTH;

    public TileWandPedestal(BlockPos pos, BlockState state) {
        // The wand slot is the only slot and it is the one the client sees.
        super(typeFor(state), pos, state, 1, new int[]{0});
    }

    /**
     * The registered block entity type of the wand pedestal that the state belongs to. The native wand
     * pedestal block and the wand pedestal metadata of the stone device are two blocks of the original
     * family and every block has exactly one block entity type.
     */
    private static BlockEntityType<?> typeFor(BlockState state) {
        if (state.getBlock() instanceof BlockStoneDevice
                && BlockStoneDevice.variant(state) == EldritchBlockRules.DEVICE_STONE_WAND_PEDESTAL) {
            return ThaumcraftEldritchBlocks.WAND_PEDESTAL_TILE.get();
        }
        return ThaumcraftLegacyTiles.WAND_PEDESTAL_BLOCK_ENTITY.get();
    }

    /** The nodes the pedestal knows, or null before the first search. */
    public List<BlockPos> nodes() { return nodes == null ? List.of() : List.copyOf(nodes); }
    /** Whether the pedestal is filling the wand at the moment. */
    public boolean isDraining() { return draining; }
    /** The direction the pedestal was placed in. */
    public Direction getFacing() { return facing; }

    /** The direction the pedestal was placed in. */
    public void setFacing(Direction facing) {
        this.facing = facing == null ? Direction.NORTH : facing;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileWandPedestal tile) {
        if (tile.isDataRejected()) return;
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
            // The pedestal fills the wand up to its own capacity; a sceptre holds one and a half times
            // the capacity of its rod (original ItemWandCasting:119-121).
            int capacity = ItemWandCasting.getMaxVis(stack);
            int reserve = LegacyTileRules.wandPedestalReserve(data.cap(), data.rod()) * WHOLE_VIS;
            for (BlockPos nodePos : nodes) {
                if (!level.hasChunkAt(nodePos)) continue;
                if (!(level.getBlockEntity(nodePos) instanceof TileNode node)) continue;
                AspectList stored = node.getAspects();
                for (Aspect aspect : Aspect.aspects.values()) {
                    if (stored.getAmount(aspect) <= reserve) continue;
                    if (data.amount(aspect.getTag()) + WHOLE_VIS > capacity) continue;
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
        if (isDataRejected()) return false;
        if (slot != 0 || stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemWandCasting || stack.getItem() instanceof ItemAmuletVis;
    }

    /** TileWandPedestal:113-121 - the gui name of the pedestal. */
    @Override public Component getDisplayName() {
        return hasCustomName() ? getCustomName() : Component.translatable("container.wandpedestal");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (saveRejected(tag)) return;
        super.saveAdditional(tag, registries);
        tag.putBoolean("draining", draining);
        tag.putByte("facing", (byte) facing.get3DDataValue());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (isDataRejected()) return;
        draining = tag.getBoolean("draining");
        nodes = null;
        somethingChanged = false;
        counter = 0;
        facing = Direction.from3DDataValue(tag.getByte("facing"));
    }
}
