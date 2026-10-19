package thaumcraft.common.lib.essentia;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Closed Fabric-native output adapter. No virtual setItem, loader transfer API,
 * arbitrary addon callback commit, combined54-slot chest or blind refund. */
public final class CrystalInventoryTarget {
    private CrystalInventoryTarget() {}

    public static final class Snapshot {
        private final NonNullList<ItemStack> identity;
        private final List<ItemStack> contents;
        private final int cooldown;
        private Snapshot(NonNullList<ItemStack> items, int cooldown, HolderLookup.Provider registries) {
            if (items.getClass() != NonNullList.class) throw new IllegalArgumentException("Unproven inventory backing list");
            identity = items; this.cooldown = cooldown;
            // copy() alone can retain mutable CUSTOM_DATA/text payload aliases.
            // Codec round-trip is callback-bearing PREFLIGHT, never raw apply/
            // final matching; it also refuses an unrepresentable whole slot.
            contents = items.stream().map(stack -> stack.isEmpty() ? ItemStack.EMPTY
                    : ItemStack.parse(registries, stack.save(registries)).orElseThrow(() -> new IllegalArgumentException("Malformed inventory item components"))).toList();
            for (ItemStack stack : contents) if (!stack.isEmpty() && (stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize())) throw new IllegalArgumentException("Malformed native inventory slot; refusing to overwrite it");
        }
        public boolean matches(NonNullList<ItemStack> items, int currentCooldown) {
            return identity == items && cooldown == currentCooldown && ItemStack.listMatches(contents, items);
        }
    }
    /** Captures fields without leaking a mutable alias. Mixins call this only. */
    public static Snapshot snapshot(NonNullList<ItemStack> items, int cooldown, HolderLookup.Provider registries) { return new Snapshot(items, cooldown, registries); }

    private record Actor(BlockEntity tile, BlockState state, Snapshot stock) {}
    static final class Group {
        private final ServerLevel level;
        private final List<BlockEntity> actors;
        private final List<BlockState> states;
        private List<Actor> snapshots;
        Group(ServerLevel level, List<BlockEntity> actors) {
            this.level = level; this.actors = List.copyOf(actors); states = actors.stream().map(BlockEntity::getBlockState).toList();
        }
        List<BlockEntity> actors() { return actors; }
        private boolean current() {
            for (int i = 0; i < actors.size(); i++) if (!installed(level, actors.get(i), states.get(i))) return false;
            return true;
        }
        Plan prepare(ItemStack output, Direction insertionFace) {
            // Native loot generation is a callback preflight, while paid output
            // is already durable. Capture BOTH halves after all loot unpacking.
            for (BlockEntity actor : actors) ((Container) actor).getItem(0);
            if (!current()) return null;
            var captured = new ArrayList<Actor>();
            for (int i = 0; i < actors.size(); i++) {
                BlockEntity actor = actors.get(i);
                Snapshot stock = ((NativeCrystalInventoryAccess) actor).thaumcraft$crystalSnapshot();
                int expected = actor.getClass() == ChestBlockEntity.class ? 27 : actor.getClass() == HopperBlockEntity.class ? 5 : 3;
                if (stock.contents.size() != expected || ((Container) actor).getContainerSize() != expected) throw new IllegalArgumentException("Malformed native inventory size");
                captured.add(new Actor(actor, states.get(i), stock));
            }
            snapshots = List.copyOf(captured);
            Plan plan = candidate(0, true, output, insertionFace);
            if (plan == null && actors.size() == 2) plan = candidate(1, true, output, insertionFace);
            if (plan == null) plan = candidate(0, false, output, insertionFace);
            if (plan == null && actors.size() == 2) plan = candidate(1, true, output, insertionFace); // Original repeats MERGE, never empty far-half.
            return plan != null && matches() ? plan : null;
        }
        private Plan candidate(int actorIndex, boolean mergeOnly, ItemStack output, Direction face) {
            Actor actor = snapshots.get(actorIndex); Container container = (Container) actor.tile();
            int[] slots;
            if (container instanceof WorldlyContainer sided) {
                int[] exposed = sided.getSlotsForFace(face); if (exposed == null) return null;
                slots = exposed.clone();
            } else { slots = new int[actor.stock().contents.size()]; for (int i = 0; i < slots.length; i++) slots[i] = i; }
            for (int slot : slots) {
                if (slot < 0 || slot >= actor.stock().contents.size()) throw new IllegalArgumentException("Malformed sided inventory exposure");
                ItemStack old = actor.stock().contents.get(slot);
                if (mergeOnly && (old.isEmpty() || old.getItem() != output.getItem())) continue;
                ItemStack observed = container.getItem(slot); // Callback, not a raw accounting getter.
                if (!ItemStack.matches(old, observed)) return null;
                if (!container.canPlaceItem(slot, output.copy()) || container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, output.copy(), face)) continue;
                int capacity = Math.min(container.getMaxStackSize(output.copy()), output.getMaxStackSize());
                if (capacity <= 0 || !old.isEmpty() && (!ItemStack.isSameItemSameComponents(old, output) || old.getCount() >= Math.min(capacity, old.getMaxStackSize()))) continue;
                NonNullList<ItemStack> after = NonNullList.withSize(actor.stock().contents.size(), ItemStack.EMPTY);
                for (int i = 0; i < after.size(); i++) after.set(i, actor.stock().contents.get(i).copy());
                after.set(slot, old.isEmpty() ? output.copy() : old.copyWithCount(old.getCount() + 1));
                return new Plan(this, actor, after);
            }
            return null;
        }
        boolean matches() {
            if (!current() || snapshots == null) return false;
            for (Actor actor : snapshots) if (!((NativeCrystalInventoryAccess) actor.tile()).thaumcraft$matchesCrystalSnapshot(actor.stock())) return false;
            return true;
        }
    }
    public static final class Plan {
        private final Group group;
        private final Actor receiver;
        private final NonNullList<ItemStack> after;
        private Plan(Group group, Actor receiver, NonNullList<ItemStack> after) { this.group = group; this.receiver = receiver; this.after = after; }
        boolean matches() { return group.matches(); }
        boolean owns(BlockEntity tile) { return receiver.tile() == tile; }
        void applyRaw() { ((NativeCrystalInventoryAccess) receiver.tile()).thaumcraft$applyCrystalInventory(this); }
        public NonNullList<ItemStack> replacementFor(BlockEntity tile) { EssentiaTransfers.requireCrystalInventoryMutation(tile, this); return after; }
        void notifyChanged() {
            receiver.tile().setChanged();
            if (installed(group.level, receiver.tile(), receiver.state())) group.level.sendBlockUpdated(receiver.tile().getBlockPos(), receiver.state(), receiver.state(), Block.UPDATE_CLIENTS);
        }
    }
    static Group discover(ServerLevel level, BlockPos target) {
        if (!level.hasChunkAt(target)) return null;
        BlockEntity selected = level.getBlockEntity(target);
        if (!supported(selected)) return null; // In particular NEVER call an addon setter/getter to "try" insertion.
        if (!installed(level, selected, selected.getBlockState())) return null;
        var actors = new ArrayList<BlockEntity>(); actors.add(selected);
        if (selected.getClass() == ChestBlockEntity.class && selected.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockState state = selected.getBlockState(); BlockPos partnerPos = target.relative(ChestBlock.getConnectedDirection(state));
            if (!level.hasChunkAt(partnerPos)) return null;
            BlockEntity partner = level.getBlockEntity(partnerPos);
            if (!supported(partner) || partner.getClass() != ChestBlockEntity.class || !installed(level, partner, partner.getBlockState())) return null;
            BlockState paired = partner.getBlockState();
            if (paired.getValue(ChestBlock.TYPE) != state.getValue(ChestBlock.TYPE).getOpposite() || paired.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)
                    || !partnerPos.relative(ChestBlock.getConnectedDirection(paired)).equals(target)) return null;
            actors.add(partner);
        }
        return new Group(level, actors);
    }
    private static boolean supported(BlockEntity tile) {
        return tile instanceof NativeCrystalInventoryAccess && (tile.getClass() == ChestBlockEntity.class && tile.getBlockState().is(Blocks.CHEST)
                || tile.getClass() == HopperBlockEntity.class && tile.getBlockState().is(Blocks.HOPPER)
                || tile.getClass() == FurnaceBlockEntity.class && tile.getBlockState().is(Blocks.FURNACE));
    }
    private static boolean installed(ServerLevel level, BlockEntity tile, BlockState state) {
        return !tile.isRemoved() && tile.getLevel() == level && level.hasChunkAt(tile.getBlockPos()) && level.getBlockEntity(tile.getBlockPos()) == tile && level.getBlockState(tile.getBlockPos()) == state;
    }
}
