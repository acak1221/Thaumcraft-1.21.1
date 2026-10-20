package thaumcraft.common.lib.essentia;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import thaumcraft.common.tiles.TileThaumatorium;

/** Full-output capacity preflight and callback-free native field replacement. Unknown addon containers are left untouched. */
public final class ThaumatoriumOutput {
    private static final ThreadLocal<Plan> RAW = new ThreadLocal<>();
    private ThaumatoriumOutput() {}
    public static final class Snapshot {
        private final NonNullList<ItemStack> identity;
        private final List<ItemStack> items;
        private final int cooldown;
        private Snapshot(NonNullList<ItemStack> items, int cooldown, HolderLookup.Provider registries) {
            identity = items; this.items = items.stream().map(stack -> stack.isEmpty() ? ItemStack.EMPTY : ItemStack.parse(registries, stack.save(registries)).orElseThrow()).toList(); this.cooldown = cooldown;
            for (ItemStack stack : this.items) if (!stack.isEmpty() && (stack.getCount() < 1 || stack.getCount() > stack.getMaxStackSize())) throw new IllegalArgumentException("Malformed native output inventory");
        }
        public boolean matches(NonNullList<ItemStack> observed, int cooldown) { return identity == observed && this.cooldown == cooldown && ItemStack.listMatches(items, observed); }
    }
    public static Snapshot snapshot(NonNullList<ItemStack> items, int cooldown, HolderLookup.Provider registries) { return new Snapshot(items, cooldown, registries); }
    private record Owner(BlockEntity tile, BlockState state, Snapshot before, NonNullList<ItemStack> after) {}
    public static final class Plan {
        private final ServerLevel level;
        private final List<Owner> owners;
        private final ItemStack output;
        private TileThaumatorium machine;
        private Plan(ServerLevel level, List<Owner> owners, ItemStack output) { this.level = level; this.owners = List.copyOf(owners); this.output = output.copy(); }
        public boolean physical() { return owners.isEmpty(); }
        public ItemStack output() { return output.copy(); }
        public boolean matches() {
            for (Owner owner : owners) if (!installed(level, owner.tile(), owner.state())
                    || !((NativeThaumatoriumInventory) owner.tile()).thaumcraft$matchesThaumatoriumSnapshot(owner.before())) return false;
            return true;
        }
        public boolean commit(TileThaumatorium machine) {
            if (RAW.get() != null || !matches() || !machine.canCommitOutput(this)) return false;
            this.machine = machine;
            RAW.set(this);
            try {
                machine.applyCraftRaw(this);
                for (Owner owner : owners) ((NativeThaumatoriumInventory) owner.tile()).thaumcraft$applyThaumatoriumOutput(this);
            } finally { RAW.remove(); this.machine = null; }
            // Both owners have been replaced before any callback-bearing world notification.
            machine.notifyCraftChanged();
            for (Owner owner : owners) if (installed(level, owner.tile(), owner.state())) owner.tile().setChanged();
            return true;
        }
        public NonNullList<ItemStack> replacementFor(BlockEntity tile) {
            if (RAW.get() != this || machine == null) throw new IllegalStateException("Output writes require the exact Thaumatorium craft scope");
            return owners.stream().filter(owner -> owner.tile() == tile).findFirst().orElseThrow().after();
        }
        public boolean ownsRaw(TileThaumatorium tile) { return RAW.get() == this && machine == tile; }
    }
    private static boolean supported(BlockEntity tile) {
        return tile instanceof NativeThaumatoriumInventory && (tile.getClass() == ChestBlockEntity.class && tile.getBlockState().is(Blocks.CHEST)
                || tile.getClass() == HopperBlockEntity.class && tile.getBlockState().is(Blocks.HOPPER)
                || tile.getClass() == FurnaceBlockEntity.class && tile.getBlockState().is(Blocks.FURNACE));
    }
    private static boolean installed(ServerLevel level, BlockEntity tile, BlockState state) {
        return tile.getLevel() == level && !tile.isRemoved() && level.hasChunkAt(tile.getBlockPos()) && level.getBlockEntity(tile.getBlockPos()) == tile && level.getBlockState(tile.getBlockPos()) == state;
    }
    /** Null means a real container exists but cannot accept the WHOLE output; an empty-owner plan means physical output. */
    public static Plan prepare(ServerLevel level, BlockPos target, Direction face, ItemStack output) {
        if (!level.hasChunkAt(target) || output.isEmpty() || output.getCount() > output.getMaxStackSize()) return null;
        BlockEntity tile = level.getBlockEntity(target);
        if (!(tile instanceof Container)) return new Plan(level, List.of(), output);
        if (!supported(tile)) return null;
        var tiles = new ArrayList<BlockEntity>(); tiles.add(tile);
        if (tile instanceof ChestBlockEntity && tile.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos pairedPos = target.relative(ChestBlock.getConnectedDirection(tile.getBlockState()));
            if (!level.hasChunkAt(pairedPos)) return null;
            BlockEntity paired = level.getBlockEntity(pairedPos);
            if (!(paired instanceof ChestBlockEntity) || !supported(paired)) return null;
            BlockState first = tile.getBlockState(), second = paired.getBlockState();
            if (second.getValue(ChestBlock.TYPE) != first.getValue(ChestBlock.TYPE).getOpposite()
                    || second.getValue(ChestBlock.FACING) != first.getValue(ChestBlock.FACING)
                    || !pairedPos.relative(ChestBlock.getConnectedDirection(second)).equals(target)) return null;
            tiles.add(paired);
        }
        var owners = new ArrayList<Owner>();
        for (BlockEntity actor : tiles) {
            Container container = (Container) actor;
            container.getItem(0); // Resolve vanilla loot tables before snapshot/preflight.
            Snapshot snapshot = ((NativeThaumatoriumInventory) actor).thaumcraft$thaumatoriumSnapshot();
            NonNullList<ItemStack> after = NonNullList.withSize(snapshot.items.size(), ItemStack.EMPTY);
            for (int slot = 0; slot < after.size(); slot++) after.set(slot, snapshot.items.get(slot).copy());
            owners.add(new Owner(actor, actor.getBlockState(), snapshot, after));
        }
        int remaining = output.getCount();
        // Original InventoryUtils: merge near, merge far, try every near slot, then merge far again; never empty far slots.
        int[] passes = owners.size() == 2 ? new int[] {0, 1, 0, 1} : new int[] {0, 0};
        for (int pass = 0; pass < passes.length; pass++) {
            Owner owner = owners.get(passes[pass]);
            boolean merge = owners.size() == 2 ? pass != 2 : pass == 0;
            Container container = (Container) owner.tile();
            int[] slots;
            if (container instanceof WorldlyContainer sided) slots = sided.getSlotsForFace(face).clone();
            else { slots = new int[container.getContainerSize()]; for (int slot = 0; slot < slots.length; slot++) slots[slot] = slot; }
            for (int slot : slots) {
                if (slot < 0 || slot >= owner.after().size()) return null;
                ItemStack old = owner.after().get(slot);
                if (merge && old.isEmpty() || !old.isEmpty() && !ItemStack.isSameItemSameComponents(old, output)) continue;
                if (!container.canPlaceItem(slot, output.copy()) || container instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, output.copy(), face)) continue;
                int room = Math.min(container.getMaxStackSize(output), output.getMaxStackSize()) - old.getCount();
                int moved = Math.min(remaining, Math.max(0, room));
                if (moved > 0) { owner.after().set(slot, output.copyWithCount(old.getCount() + moved)); remaining -= moved; }
                if (remaining == 0) { Plan plan = new Plan(level, owners, output); return plan.matches() ? plan : null; }
            }
        }
        return null;
    }
}
