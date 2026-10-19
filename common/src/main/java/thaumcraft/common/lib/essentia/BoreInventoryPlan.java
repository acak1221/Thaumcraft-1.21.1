package thaumcraft.common.lib.essentia;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Detached data arithmetic for the original InventoryUtils:49-159 output walk.
 * This is NOT permission to debit, write an inventory, or deliver an output.
 * The future closed executor owns native identity/lifetime/stock revalidation,
 * callback preflight, paid ownership, raw writes, cooldowns and notifications.
 * The shadow walk never queries a container, invokes a supplied callback, or
 * uses native getMaxStackSize (whose default99 is not the original64).
 * Codec capture/copy is separate, callback-bearing preflight.
 */
public final class BoreInventoryPlan {
    public static final int INVENTORY_LIMIT = 64;
    public static final long MAX_BUNDLE_ITEMS = Integer.MAX_VALUE;

    public enum Reason {
        INVALID_STACK, INVALID_ITEM_MAXIMUM, UNREPRESENTABLE_COMPONENTS,
        INVALID_INVENTORY, INVALID_SLOT_COUNT, INVALID_EXPOSURE, INVALID_PERMISSIONS,
        INVALID_PAIR, INVALID_BUNDLE, BUNDLE_TOTAL_OVERFLOW, INCONSISTENT_ITEM_MAXIMUM
    }

    /** A refusal has no usable partial plan. It never changes the supplied data;
     * the caller must retain its whole owned bundle, not infer a delivery. */
    public static final class Refused extends IllegalArgumentException {
        private final Reason reason;
        private Refused(Reason reason, String message) { super(message); this.reason = reason; }
        private Refused(Reason reason, String message, RuntimeException cause) { super(message, cause); this.reason = reason; }
        public Reason reason() { return reason; }
    }

    /** Private native count1 identity and canonical component data never escape.
     * Count is separate, so a source/residual is not limited by the codec's99.
     * Capture/copyStack are codec-bearing PREFLIGHT, never a raw-write phase.
     * Only audited native items/components belong in this bounded adapter. */
    public static final class Stack {
        private static final Stack EMPTY = new Stack(0, 0, null, null);
        private final int count, itemMaximum;
        private final Tag encodedUnit;
        private final ItemStack unit;

        private Stack(int count, int itemMaximum, Tag encodedUnit, ItemStack unit) {
            this.count = count; this.itemMaximum = itemMaximum;
            this.encodedUnit = encodedUnit; this.unit = unit;
        }

        public static Stack empty() { return EMPTY; }

        /** itemMaximum must already have been evaluated by the caller's bounded
         * item preflight. This method never asks the item for it. */
        public static Stack of(HolderLookup.Provider registries, ItemStack source, int itemMaximum) {
            if (registries == null || source == null || source.isEmpty() || source.getCount() <= 0) {
                throw refuse(Reason.INVALID_STACK, "A positive nonempty stack and registry lookup are required");
            }
            if (itemMaximum <= 0) throw refuse(Reason.INVALID_ITEM_MAXIMUM, "Invalid precomputed item maximum");
            int count = source.getCount();
            try {
                var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                // copy() alone can alias mutable custom data or text components.
                Tag encoded = ItemStack.CODEC.encodeStart(ops, source.copyWithCount(1)).getOrThrow().copy();
                ItemStack unit = ItemStack.CODEC.parse(ops, encoded.copy()).getOrThrow();
                if (unit.isEmpty() || unit.getCount() != 1 || source.getCount() != count
                        || !ItemStack.isSameItemSameComponents(source, unit)
                        || !encoded.equals(ItemStack.CODEC.encodeStart(ops, unit).getOrThrow())) {
                    throw refuse(Reason.UNREPRESENTABLE_COMPONENTS, "Stack components did not round-trip exactly");
                }
                return new Stack(count, itemMaximum, encoded, unit);
            } catch (Refused failure) { throw failure; }
            catch (RuntimeException failure) {
                throw new Refused(Reason.UNREPRESENTABLE_COMPONENTS, "Cannot freeze complete stack components", failure);
            }
        }

        public boolean isEmpty() { return count == 0; }
        public int count() { return count; }
        public int itemMaximum() { return itemMaximum; }

        /** Fresh mutable stack AND fresh decoded component payload, not a shallow
         * copy of a retained native template. No registry/world reference is kept. */
        public ItemStack copyStack(HolderLookup.Provider registries) {
            if (isEmpty()) return new ItemStack(Items.AIR, 0);
            if (registries == null) throw refuse(Reason.UNREPRESENTABLE_COMPONENTS, "Missing registry lookup");
            try {
                var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                ItemStack copy = ItemStack.CODEC.parse(ops, encodedUnit.copy()).getOrThrow();
                if (copy.getCount() != 1 || !ItemStack.isSameItemSameComponents(unit, copy)
                        || !encodedUnit.equals(ItemStack.CODEC.encodeStart(ops, copy).getOrThrow())) {
                    throw refuse(Reason.UNREPRESENTABLE_COMPONENTS, "Stack copy changed the frozen components");
                }
                copy.setCount(count);
                return copy;
            } catch (Refused failure) { throw failure; }
            catch (RuntimeException failure) {
                throw new Refused(Reason.UNREPRESENTABLE_COMPONENTS, "Cannot copy complete stack components", failure);
            }
        }

        private Stack withCount(int after) {
            return after == 0 ? EMPTY : new Stack(after, itemMaximum, encodedUnit, unit);
        }
        private boolean sameComponents(Stack other) {
            return !isEmpty() && !other.isEmpty() && ItemStack.isSameItemSameComponents(unit, other.unit);
        }
    }

    public enum Profile {
        CHEST(27), HOPPER(5), FURNACE(3);
        private final int slots;
        Profile(int slots) { this.slots = slots; }
        public int slots() { return slots; }
    }

    /** Frozen physical slots, copied exposure, and per-original-bundle-member
     * permissions. A true permission means the caller already combined all
     * canPlace/canPlaceThroughFace policy. It is not permission to commit. */
    public static final class Inventory {
        private final Profile profile;
        private final List<Stack> slots;
        private final int[] order;
        private final boolean[][] permitted;

        private Inventory(Profile profile, List<Stack> slots, int[] exposure, boolean[][] permitted) {
            if (slots == null || slots.size() != profile.slots()) {
                throw refuse(Reason.INVALID_INVENTORY, "Wrong physical slot count for " + profile);
            }
            var copied = new ArrayList<Stack>(slots.size());
            for (Stack stack : slots) {
                if (stack == null) throw refuse(Reason.INVALID_INVENTORY, "Null physical slot");
                if (stack.count() > INVENTORY_LIMIT) throw refuse(Reason.INVALID_SLOT_COUNT, "Physical slot exceeds original64");
                copied.add(stack); // Stack is immutable, including its private component data.
            }
            this.profile = profile; this.slots = List.copyOf(copied);
            order = exposure == null ? new int[0] : exposure.clone();
            boolean[] seen = new boolean[slots.size()];
            for (int slot : order) {
                if (slot < 0 || slot >= slots.size() || seen[slot]) {
                    throw refuse(Reason.INVALID_EXPOSURE, "Invalid or duplicate physical slot exposure");
                }
                seen[slot] = true;
            }
            if (permitted == null) throw refuse(Reason.INVALID_PERMISSIONS, "Missing frozen slot permissions");
            this.permitted = new boolean[permitted.length][];
            for (int index = 0; index < permitted.length; index++) {
                if (permitted[index] == null || permitted[index].length != slots.size()) {
                    throw refuse(Reason.INVALID_PERMISSIONS, "Wrong frozen permission row size");
                }
                this.permitted[index] = permitted[index].clone();
            }
        }

        public static Inventory chest(List<Stack> slots, boolean[][] permitted) {
            return new Inventory(Profile.CHEST, slots, ordinarySlots(Profile.CHEST), permitted);
        }
        public static Inventory hopper(List<Stack> slots, boolean[][] permitted) {
            return new Inventory(Profile.HOPPER, slots, ordinarySlots(Profile.HOPPER), permitted);
        }
        /** Null exposure preserves the original sided no-slots case. */
        public static Inventory furnace(List<Stack> slots, int[] exposure, boolean[][] permitted) {
            return new Inventory(Profile.FURNACE, slots, exposure, permitted);
        }
        public Profile profile() { return profile; }
        public List<Stack> slots() { return slots; }
        public int[] slotOrder() { return order.clone(); }

        private static int[] ordinarySlots(Profile profile) {
            int[] result = new int[profile.slots()];
            for (int index = 0; index < result.length; index++) result[index] = index;
            return result;
        }
    }

    /** At most one positive leftover per original bundle index. */
    public static final class Residual {
        private final int bundleIndex;
        private final Stack stack;
        private Residual(int bundleIndex, Stack stack) { this.bundleIndex = bundleIndex; this.stack = stack; }
        public int bundleIndex() { return bundleIndex; }
        public Stack stack() { return stack; }
    }

    /** Primitive audit data, not callbacks or a mutation capability. -1 means a
     * pass was not reached; 0 means it ran with no exposed slots. The second far
     * MERGE is intentional even though frozen permissions often make it redundant. */
    public static final class Walk {
        private final int bundleIndex, nearMerge, farMerge, nearSlots, farMergeAgain;
        private Walk(int bundleIndex, int nearMerge, int farMerge, int nearSlots, int farMergeAgain) {
            this.bundleIndex = bundleIndex; this.nearMerge = nearMerge; this.farMerge = farMerge;
            this.nearSlots = nearSlots; this.farMergeAgain = farMergeAgain;
        }
        public int bundleIndex() { return bundleIndex; }
        public int nearMergeVisited() { return nearMerge; }
        public int farMergeVisited() { return farMerge; }
        public int nearSlotsVisited() { return nearSlots; }
        public int farMergeAgainVisited() { return farMergeAgain; }
    }

    private final List<Stack> bundle, nearBefore, farBefore, nearAfter, farAfter;
    private final List<Residual> residuals;
    private final List<Walk> walks;
    private final int[] nearChanged, farChanged;
    private final long bundleItems, insertedItems, residualItems;

    private BoreInventoryPlan(List<Stack> bundle, List<Stack> nearBefore, List<Stack> farBefore,
                              List<Stack> nearAfter, List<Stack> farAfter, List<Residual> residuals,
                              List<Walk> walks, long bundleItems) {
        this.bundle = List.copyOf(bundle); this.nearBefore = nearBefore; this.farBefore = farBefore;
        this.nearAfter = List.copyOf(nearAfter); this.farAfter = List.copyOf(farAfter);
        this.residuals = List.copyOf(residuals); this.walks = List.copyOf(walks);
        nearChanged = changedSlots(nearBefore, this.nearAfter); farChanged = changedSlots(farBefore, this.farAfter);
        this.bundleItems = bundleItems;
        long remaining = 0;
        for (Residual residual : residuals) remaining = Math.addExact(remaining, residual.stack().count());
        residualItems = remaining; insertedItems = Math.subtractExact(bundleItems, remaining);
        long before = Math.addExact(total(nearBefore), total(farBefore));
        long after = Math.addExact(total(this.nearAfter), total(this.farAfter));
        if (Math.addExact(before, bundleItems) != Math.addExact(after, remaining)) {
            throw new IllegalStateException("Bore shadow plan did not conserve item counts");
        }
    }

    public static BoreInventoryPlan plan(List<Stack> bundle, Inventory near) { return plan(bundle, near, null); }

    /** Complete shadow walk only. No source stack, captured slot or input array
     * is mutated, even if a later malformed member refuses the whole plan. */
    public static BoreInventoryPlan plan(List<Stack> bundle, Inventory near, Inventory far) {
        if (bundle == null) throw refuse(Reason.INVALID_BUNDLE, "Missing output bundle");
        if (near == null) throw refuse(Reason.INVALID_INVENTORY, "Missing near inventory snapshot");
        if (far != null && (near == far || near.profile != Profile.CHEST || far.profile != Profile.CHEST)) {
            throw refuse(Reason.INVALID_PAIR, "Only distinct ordinary chest snapshots can pair");
        }
        var sources = new ArrayList<Stack>(bundle.size());
        long items = 0;
        for (Stack stack : bundle) {
            if (stack == null || stack.isEmpty()) throw refuse(Reason.INVALID_BUNDLE, "Bundle contains an empty member");
            items = Math.addExact(items, stack.count());
            if (items > MAX_BUNDLE_ITEMS) throw refuse(Reason.BUNDLE_TOTAL_OVERFLOW, "Bundle total exceeds representable item count");
            sources.add(stack);
        }
        if (near.permitted.length != sources.size() || far != null && far.permitted.length != sources.size()) {
            throw refuse(Reason.INVALID_PERMISSIONS, "Permission rows must match the whole original bundle");
        }
        var nearShadow = new ArrayList<>(near.slots);
        List<Stack> farBefore = far == null ? List.of() : far.slots;
        var farShadow = new ArrayList<>(farBefore);
        var residuals = new ArrayList<Residual>();
        var walks = new ArrayList<Walk>(sources.size());
        for (int index = 0; index < sources.size(); index++) {
            Stack source = sources.get(index);
            Scan scanned = scan(source, source.count(), index, near, nearShadow, true);
            int remaining = scanned.remaining(), nearMerge = scanned.visited();
            int farMerge = -1, nearSlots = -1, farAgain = -1;
            if (remaining > 0 && far != null) {
                scanned = scan(source, remaining, index, far, farShadow, true);
                remaining = scanned.remaining(); farMerge = scanned.visited();
            }
            if (remaining > 0) {
                scanned = scan(source, remaining, index, near, nearShadow, false);
                remaining = scanned.remaining(); nearSlots = scanned.visited();
            }
            if (remaining > 0 && far != null) {
                scanned = scan(source, remaining, index, far, farShadow, true);
                remaining = scanned.remaining(); farAgain = scanned.visited();
            }
            if (remaining > 0) residuals.add(new Residual(index, source.withCount(remaining)));
            walks.add(new Walk(index, nearMerge, farMerge, nearSlots, farAgain));
        }
        return new BoreInventoryPlan(sources, near.slots, farBefore, nearShadow, farShadow, residuals, walks, items);
    }

    private record Scan(int remaining, int visited) {}

    private static Scan scan(Stack source, int remaining, int bundleIndex, Inventory inventory,
                             List<Stack> shadow, boolean mergeOnly) {
        int visited = 0;
        for (int slot : inventory.order) {
            if (remaining == 0) break;
            visited++;
            Stack old = shadow.get(slot);
            if ((mergeOnly && old.isEmpty()) || !inventory.permitted[bundleIndex][slot]) continue;
            int moved;
            if (old.isEmpty()) {
                // Original empty-slot branch uses inventory64, NOT itemMaximum.
                moved = Math.min(INVENTORY_LIMIT, remaining);
                shadow.set(slot, source.withCount(moved));
            } else {
                if (!old.sameComponents(source)) continue;
                if (old.itemMaximum != source.itemMaximum) {
                    throw refuse(Reason.INCONSISTENT_ITEM_MAXIMUM, "Strictly matching stacks have contradictory item maxima");
                }
                long room = Math.min((long) INVENTORY_LIMIT - old.count(), (long) source.itemMaximum - old.count());
                // Root-approved safety adaptation: original negative room must
                // not take stock back out of an over-item-max physical slot.
                if (room <= 0) continue;
                moved = (int) Math.min(room, remaining);
                shadow.set(slot, old.withCount(Math.addExact(old.count(), moved)));
            }
            remaining = Math.subtractExact(remaining, moved);
        }
        return new Scan(remaining, visited);
    }

    private static Refused refuse(Reason reason, String message) { return new Refused(reason, message); }
    private static long total(List<Stack> stacks) {
        long total = 0;
        for (Stack stack : stacks) total = Math.addExact(total, stack.count());
        return total;
    }
    private static int[] changedSlots(List<Stack> before, List<Stack> after) {
        int size = 0;
        for (int slot = 0; slot < before.size(); slot++) if (before.get(slot) != after.get(slot)) size++;
        int[] result = new int[size];
        for (int slot = 0, index = 0; slot < before.size(); slot++) if (before.get(slot) != after.get(slot)) result[index++] = slot;
        return result;
    }

    public List<Stack> bundle() { return bundle; }
    public List<Stack> nearBefore() { return nearBefore; }
    public List<Stack> farBefore() { return farBefore; }
    public List<Stack> nearAfter() { return nearAfter; }
    public List<Stack> farAfter() { return farAfter; }
    public List<Residual> residuals() { return residuals; }
    public List<Walk> walks() { return walks; }
    public int[] nearChangedSlots() { return nearChanged.clone(); }
    public int[] farChangedSlots() { return farChanged.clone(); }
    public long bundleItems() { return bundleItems; }
    public long insertedItems() { return insertedItems; }
    public long residualItems() { return residualItems; }
}
