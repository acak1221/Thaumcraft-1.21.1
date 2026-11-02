package thaumcraft.common.crafting.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.crafting.transfer.RecipeTransferPlan;
import thaumcraft.api.crafting.transfer.RecipeTransferResult;
import thaumcraft.api.crafting.transfer.RecipeTransferTarget;

/** Server-only, copy-planned ingredient arrangement. It never creates output or consumes vis. */
public final class RecipeTransfers {
    private static final Logger LOG = LoggerFactory.getLogger(RecipeTransfers.class);
    private static final ThreadLocal<Set<RecipeTransferTarget>> ACTIVE = ThreadLocal.withInitial(
            () -> Collections.newSetFromMap(new IdentityHashMap<>()));
    private RecipeTransfers() {}

    public static RecipeTransferResult transfer(ServerPlayer actor, RecipeTransferTarget target, ResourceLocation id, boolean maximum) {
        if (actor == null || target == null || !actor.getServer().isSameThread()) return RecipeTransferResult.NO_TARGET;
        if (actor.containerMenu != target || !target.canTransferRecipe(actor) || !ACTIVE.get().add(target)) return RecipeTransferResult.STALE_MENU;
        try {
            ResourceLocation canonical = RecipeTransferRecipes.normalize(id);
            if (RecipeTransferRecipes.holder(actor.level(), canonical) == null) return RecipeTransferResult.UNKNOWN_RECIPE;
            var recipe = RecipeTransferRecipes.resolve(actor.level(), canonical);
            if (recipe == null) return RecipeTransferResult.UNSUPPORTED;
            Container contents = target.recipeTransferContents();
            if (contents == null || contents.getContainerSize() < 9) return RecipeTransferResult.NO_TARGET;
            List<ItemStack> matrix = java.util.stream.IntStream.range(0, 9).mapToObj(i -> contents.getItem(i).copy()).toList();
            List<ItemStack> sources = target.recipeTransferSourceSnapshot(actor).stream().map(ItemStack::copy).toList();
            if (sources.size() > 4096) return RecipeTransferResult.UNSUPPORTED;
            if (!recipe.initialResearch(actor, matrix)) return RecipeTransferResult.LOCKED_RESEARCH;
            // Asking for one recipe does not empty an already correctly arranged stack of ingredients.
            if (!maximum && recipe.matches(actor.level(), matrix)) {
                if (!recipe.researched(actor, matrix)) return RecipeTransferResult.LOCKED_RESEARCH;
                return commit(actor, target, new RecipeTransferPlan(canonical, matrix, sources, matrix, sources, 1));
            }
            List<Group> pool = groups(sources, matrix);
            boolean noRoom = false;
            for (int count = maximum ? 64 : 1; count >= 1; count--) {
                Search search = new Search(actor, recipe, pool, sources, count);
                if (search.solve()) {
                    if (!recipe.researched(actor, search.matrix)) return RecipeTransferResult.LOCKED_RESEARCH;
                    return commit(actor, target, new RecipeTransferPlan(canonical, matrix, sources, search.matrix, search.sources, count));
                }
                noRoom |= search.hadMaterials;
                if (!maximum) break;
            }
            return noRoom ? RecipeTransferResult.INVENTORY_FULL : RecipeTransferResult.MISSING_INGREDIENTS;
        } catch (RuntimeException failure) {
            LOG.warn("Recipe transfer refused before an acknowledged commit", failure);
            return RecipeTransferResult.FAILED;
        } finally { ACTIVE.get().remove(target); }
    }

    private static RecipeTransferResult commit(ServerPlayer actor, RecipeTransferTarget target, RecipeTransferPlan plan) {
        if (actor.containerMenu != target || !target.canTransferRecipe(actor)) return RecipeTransferResult.STALE_MENU;
        return target.applyRecipeTransfer(actor, plan) ? RecipeTransferResult.SUCCESS : RecipeTransferResult.STALE_MENU;
    }

    private static final class Group {
        final ItemStack sample;
        long amount;
        Group(ItemStack stack) { sample = stack.copyWithCount(1); amount = stack.getCount(); }
    }
    private static List<Group> groups(List<ItemStack> first, List<ItemStack> second) {
        List<Group> groups = new ArrayList<>();
        for (List<ItemStack> inventory : List.of(first, second)) for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            int index = groupOf(groups, stack);
            if (index < 0) groups.add(new Group(stack)); else groups.get(index).amount += stack.getCount();
        }
        return groups;
    }
    private static int groupOf(List<Group> groups, ItemStack item) {
        for (int i = 0; i < groups.size(); i++) if (ItemStack.isSameItemSameComponents(groups.get(i).sample, item)) return i;
        return -1;
    }

    private static final class Search {
        private static final int MAX_ATTEMPTS = 200_000;
        final ServerPlayer actor;
        final RecipeTransferRecipes.Template recipe;
        final List<Group> pool;
        final List<ItemStack> originalSources;
        final int count;
        final long[] remaining;
        final int[] assignment = new int[9];
        final List<Integer> order = new ArrayList<>();
        final List<List<Integer>> candidates = new ArrayList<>(9);
        int attempts;
        boolean hadMaterials;
        List<ItemStack> matrix, sources;

        Search(ServerPlayer actor, RecipeTransferRecipes.Template recipe, List<Group> pool, List<ItemStack> originalSources, int count) {
            this.actor = actor; this.recipe = recipe; this.pool = pool; this.originalSources = originalSources; this.count = count;
            remaining = pool.stream().mapToLong(group -> group.amount).toArray();
            java.util.Arrays.fill(assignment, -1);
            for (int cell = 0; cell < 9; cell++) {
                List<Integer> eligible = new ArrayList<>();
                if (!recipe.matrix().get(cell).isEmpty()) {
                    order.add(cell);
                    for (int i = 0; i < pool.size(); i++) if (pool.get(i).amount >= count
                            && pool.get(i).sample.getMaxStackSize() >= count && recipe.accepts(cell, pool.get(i).sample)) eligible.add(i);
                }
                candidates.add(eligible);
            }
            order.sort(Comparator.comparingInt(cell -> candidates.get(cell).size()));
        }
        boolean solve() {
            if (order.stream().anyMatch(cell -> candidates.get(cell).isEmpty())) return false;
            if (!hasCapacity()) return false;
            if (!couldPack()) { hadMaterials = true; return false; }
            return assign(0);
        }
        /** Reject impossible tag overlaps without enumerating permutations of the same scarce stacks. */
        private boolean hasCapacity() {
            int[] allocated = new int[pool.size()];
            int[] cells = new int[9];
            java.util.Arrays.fill(cells, -1);
            for (int cell : order) if (!allocate(cell, cells, allocated, new boolean[pool.size()])) return false;
            return true;
        }
        private boolean allocate(int cell, int[] cells, int[] allocated, boolean[] seen) {
            for (int group : candidates.get(cell)) {
                if (seen[group]) continue;
                seen[group] = true;
                if (allocated[group] < remaining[group] / count) {
                    cells[cell] = group;
                    allocated[group]++;
                    return true;
                }
                for (int occupied : order) if (cells[occupied] == group && allocate(occupied, cells, allocated, seen)) {
                    cells[cell] = group;
                    return true;
                }
            }
            return false;
        }
        /**
         * Optimistic storage bound: ignore which ingredient gets each variant, but account for
         * every leftover component variant and occupied source cell. If even this relaxed
         * allocation needs too many cells, no permutation can return the old matrix safely.
         */
        private boolean couldPack() {
            int impossible = originalSources.size() + 1;
            int[] best = new int[order.size() + 1];
            java.util.Arrays.fill(best, impossible);
            best[0] = 0;
            for (int group = 0; group < pool.size(); group++) {
                int eligibleCells = 0;
                for (int cell : order) if (candidates.get(cell).contains(group)) eligibleCells++;
                int maxAllocated = (int) Math.min(eligibleCells, pool.get(group).amount / count);
                int slotCapacity = Math.max(1, Math.min(64, pool.get(group).sample.getMaxStackSize()));
                // Preserve pre-existing oversized source stacks just as pack() does. Taking
                // the largest such capacity for ALL cells only makes this bound optimistic.
                for (ItemStack original : originalSources) if (!original.isEmpty()
                        && ItemStack.isSameItemSameComponents(original, pool.get(group).sample))
                    slotCapacity = Math.max(slotCapacity, original.getCount());
                int[] next = new int[best.length];
                java.util.Arrays.fill(next, impossible);
                for (int used = 0; used < best.length; used++) if (best[used] < impossible) {
                    for (int taken = 0; taken <= maxAllocated && used + taken < best.length; taken++) {
                        long left = pool.get(group).amount - (long) taken * count;
                        int cells = (int) Math.min(impossible, (left + slotCapacity - 1) / slotCapacity);
                        next[used + taken] = Math.min(next[used + taken], Math.min(impossible, best[used] + cells));
                    }
                }
                best = next;
            }
            return best[order.size()] <= originalSources.size();
        }
        private boolean assign(int position) {
            if (++attempts > MAX_ATTEMPTS) return false;
            if (position == order.size()) {
                List<ItemStack> offered = new ArrayList<>(9);
                for (int cell = 0; cell < 9; cell++) offered.add(assignment[cell] < 0 ? ItemStack.EMPTY : pool.get(assignment[cell]).sample.copyWithCount(count));
                // In particular this preserves TC's original greedy shapeless overlap rule.
                if (!recipe.matches(actor.level(), offered)) return false;
                hadMaterials = true;
                List<ItemStack> packed = pack(false);
                if (packed == null) packed = pack(true);
                if (packed == null) return false;
                matrix = offered; sources = packed; return true;
            }
            int cell = order.get(position);
            for (int group : candidates.get(cell)) {
                if (remaining[group] < count) continue;
                assignment[cell] = group; remaining[group] -= count;
                if (assign(position + 1)) return true;
                remaining[group] += count; assignment[cell] = -1;
            }
            return false;
        }

        private List<ItemStack> pack(boolean compact) {
            long[] left = remaining.clone();
            List<ItemStack> result = new ArrayList<>(Collections.nCopies(originalSources.size(), ItemStack.EMPTY));
            // First retain original cells. Compact only if returning old matrix contents needs room.
            for (int slot = 0; slot < originalSources.size(); slot++) {
                ItemStack original = originalSources.get(slot);
                if (original.isEmpty()) continue;
                int group = groupOf(pool, original);
                int limit = compact ? Math.max(original.getCount(), Math.min(64, original.getMaxStackSize())) : original.getCount();
                int keep = (int) Math.min(left[group], limit);
                if (keep > 0) { result.set(slot, original.copyWithCount(keep)); left[group] -= keep; }
            }
            for (int group = 0; group < pool.size(); group++) {
                ItemStack sample = pool.get(group).sample;
                int limit = Math.min(64, sample.getMaxStackSize());
                for (int slot = 0; slot < result.size() && left[group] > 0; slot++) {
                    ItemStack existing = result.get(slot);
                    if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, sample)) continue;
                    int add = (int) Math.min(left[group], Math.max(0, limit - existing.getCount()));
                    if (add > 0) { existing.grow(add); left[group] -= add; }
                }
                for (int slot = 0; slot < result.size() && left[group] > 0; slot++) if (result.get(slot).isEmpty()) {
                    int add = (int) Math.min(left[group], limit); result.set(slot, sample.copyWithCount(add)); left[group] -= add;
                }
                if (left[group] != 0) return null;
            }
            return result;
        }
    }
}
