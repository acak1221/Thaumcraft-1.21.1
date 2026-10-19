package thaumcraft.common.entities.golems;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import thaumcraft.common.tiles.TileChestHungry;
import thaumcraft.common.blocks.BlockFlesh;

/** Finite native golem inventories, NOT a registry of arbitrary Container providers. */
final class GolemHomeInventory {
    private GolemHomeInventory() {}

    static Destination find(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        BlockEntity first = level.getBlockEntity(pos);
        if (!supported(first) || first.isRemoved()) return null;
        BlockPos otherPos = null;
        BlockEntity other = null;
        BlockState otherState = null;
        if (first instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
            if (!level.hasChunkAt(otherPos)) return null;
            other = level.getBlockEntity(otherPos);
            otherState = level.getBlockState(otherPos);
            if (!(other instanceof ChestBlockEntity) || !supported(other) || other.isRemoved()
                    || otherState.getBlock() != state.getBlock()) return null;
        }
        // Native hopper lookup includes vanilla double chests and ignores a
        // blocked chest lid. Only the whitelisted BE(s) above may back the result.
        Container container = HopperBlockEntity.getContainerAt(level, pos);
        if (container != first && (other == null || container == null || container.getClass() != CompoundContainer.class)) return null;
        if (container == null) return null;
        return new Destination(level, pos.immutable(), state, first, otherPos, otherState, other, container);
    }

    /** Positive non-container classification, NEVER the negation of find/roomFor. */
    static boolean isWorldThrowTarget(ServerLevel level, BlockPos pos) {
        if (pos == null || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        // Native vanilla blocks, plus this port's plain base-Block solids. Do
        // not trust an addon's class just because it uses a familiar namespace.
        // Derive the native package from Block, not a development mapping name.
        boolean known = id != null && (id.getNamespace().equals("minecraft")
                && block.getClass().getPackageName().equals(Block.class.getPackageName())
                || id.getNamespace().equals("thaumcraft") && (block.getClass() == Block.class
                    || id.getPath().equals("flesh_block") && block.getClass() == BlockFlesh.class));
        return known && !state.hasBlockEntity() && !(block instanceof EntityBlock)
                && !(block instanceof Container) && !(block instanceof WorldlyContainerHolder)
                && level.getBlockEntity(pos) == null;
    }

    private static boolean supported(BlockEntity tile) {
        if (tile == null) return false;
        Class<?> type = tile.getClass();
        return type == ChestBlockEntity.class || type == TrappedChestBlockEntity.class
                || type == BarrelBlockEntity.class || type == HopperBlockEntity.class
                || type == FurnaceBlockEntity.class || type == BlastFurnaceBlockEntity.class
                || type == SmokerBlockEntity.class || type == TileChestHungry.class;
    }

    record Destination(ServerLevel level, BlockPos pos, BlockState state, BlockEntity first,
                       BlockPos otherPos, BlockState otherState, BlockEntity other, Container container) {
        boolean current() {
            return level.hasChunkAt(pos) && !first.isRemoved() && level.getBlockEntity(pos) == first
                    && level.getBlockState(pos) == state
                    && (other == null || level.hasChunkAt(otherPos) && !other.isRemoved()
                    && level.getBlockEntity(otherPos) == other && level.getBlockState(otherPos) == otherState);
        }

        int[] slots(Direction face) {
            return container instanceof WorldlyContainer sided ? sided.getSlotsForFace(face)
                    : IntStream.range(0, container.getContainerSize()).toArray();
        }

        boolean accepts(int slot, ItemStack stack, Direction face) {
            return slot >= 0 && slot < container.getContainerSize() && container.canPlaceItem(slot, stack)
                    && (!(container instanceof WorldlyContainer sided) || sided.canPlaceItemThroughFace(slot, stack, face));
        }

        boolean extracts(int slot, ItemStack stack, Direction face) {
            return slot >= 0 && slot < container.getContainerSize() && !stack.isEmpty()
                    && (!(container instanceof WorldlyContainer sided) || sided.canTakeItemThroughFace(slot, stack, face));
        }

        int firstExtractable(ItemStack exemplar, Direction face) {
            if (!current()) return -1;
            int[] exposed = slots(face);
            if (exposed == null) return -1;
            for (int slot : exposed) {
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                ItemStack actual = container.getItem(slot);
                if (extracts(slot, actual, face) && (exemplar.isEmpty()
                        || ItemStack.isSameItemSameComponents(actual, exemplar))) return slot;
            }
            return -1;
        }

        long countVisible(ItemStack exemplar, Direction face) {
            if (!current()) return 0;
            int[] exposed = slots(face);
            if (exposed == null) return 0;
            long count = 0;
            for (int slot : exposed) {
                if (slot < 0 || slot >= container.getContainerSize()) continue;
                ItemStack actual = container.getItem(slot);
                // Fill counts visible stock even when extraction from it is denied.
                if (!actual.isEmpty() && ItemStack.isSameItemSameComponents(actual, exemplar)) count += actual.getCount();
            }
            return count;
        }

        int roomFor(ItemStack stack, Direction face) {
            if (!current() || stack.isEmpty()) return 0;
            int[] exposed = slots(face);
            if (exposed == null) return 0;
            int maximum = Math.min(64, Math.min(stack.getMaxStackSize(), container.getMaxStackSize(stack)));
            int room = 0;
            for (int slot : exposed) {
                if (!accepts(slot, stack, face)) continue;
                ItemStack actual = container.getItem(slot);
                if (actual.isEmpty() || ItemStack.isSameItemSameComponents(actual, stack)) {
                    room += Math.max(0, maximum - actual.getCount());
                }
            }
            return room;
        }

        boolean hasSeedAndRoom(ItemStack exemplar, Direction face) {
            // Quantity is irrelevant; this is a read-only extraction probe of
            // actual destination stock, on the SAME face that accepts the cargo.
            return !exemplar.isEmpty() && current() && roomFor(exemplar, face) > 0
                    && firstExtractable(exemplar, face) >= 0;
        }

        boolean sharesStorage(Destination otherInventory) {
            return otherInventory != null && (first == otherInventory.first || first == otherInventory.other
                    || other != null && (other == otherInventory.first || other == otherInventory.other));
        }
    }
}
