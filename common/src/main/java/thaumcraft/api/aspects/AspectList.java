package thaumcraft.api.aspects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/** TC4's ordered aspect amounts. add sums; merge takes the maximum. */
public class AspectList {
    public final LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

    public AspectList copy() {
        AspectList result = new AspectList();
        result.aspects.putAll(this.aspects);
        return result;
    }

    public int size() { return this.aspects.size(); }
    public int visSize() { return this.aspects.values().stream().mapToInt(Integer::intValue).sum(); }
    public int getAmount(Aspect aspect) { return this.aspects.getOrDefault(aspect, 0); }
    public Aspect[] getAspects() { return this.aspects.keySet().toArray(Aspect[]::new); }
    public Aspect[] getPrimalAspects() { return this.aspects.keySet().stream().filter(Aspect::isPrimal).toArray(Aspect[]::new); }

    public Aspect[] getAspectsSorted() {
        Aspect[] result = getAspects();
        Arrays.sort(result, Comparator.comparing(Aspect::getTag));
        return result;
    }

    /**
     * Original AspectList.getAspectsSortedAmount:100-123: the aspects are sorted by
     * decreasing amount, but an aspect without an amount never moves and equal
     * amounts keep their declared order.
     */
    public Aspect[] getAspectsSortedAmount() {
        Aspect[] result = getAspects();
        int start = 0;
        for (int index = 0; index <= result.length; index++) {
            if (index < result.length && getAmount(result[index]) > 0) continue;
            sortByAmount(result, start, index);
            start = index + 1;
        }
        return result;
    }

    /** A stable descending sort of one run of aspects that carry an amount. */
    private void sortByAmount(Aspect[] aspects, int from, int to) {
        for (int index = from + 1; index < to; index++) {
            Aspect aspect = aspects[index];
            int amount = getAmount(aspect);
            int position = index - 1;
            while (position >= from && getAmount(aspects[position]) < amount) {
                aspects[position + 1] = aspects[position];
                position--;
            }
            aspects[position + 1] = aspect;
        }
    }

    public Aspect[] getAspectsSortedByAmount() { return getAspectsSortedAmount(); }

    public AspectList add(Aspect aspect, int amount) {
        if (aspect != null) this.aspects.put(aspect, getAmount(aspect) + amount);
        return this;
    }

    public AspectList add(AspectList other) {
        for (Aspect aspect : other.getAspects()) add(aspect, other.getAmount(aspect));
        return this;
    }

    public AspectList merge(Aspect aspect, int amount) {
        if (aspect != null) this.aspects.put(aspect, this.aspects.containsKey(aspect) ? Math.max(getAmount(aspect), amount) : amount);
        return this;
    }

    public AspectList merge(AspectList other) {
        for (Aspect aspect : other.getAspects()) merge(aspect, other.getAmount(aspect));
        return this;
    }

    public boolean reduce(Aspect aspect, int amount) {
        if (aspect == null || amount < 0 || getAmount(aspect) < amount) return false;
        this.aspects.put(aspect, getAmount(aspect) - amount);
        return true;
    }

    public AspectList remove(Aspect aspect, int amount) {
        if (aspect == null || amount < 0) return this;
        int left = getAmount(aspect) - amount;
        if (left <= 0) this.aspects.remove(aspect); else this.aspects.put(aspect, left);
        return this;
    }

    public AspectList remove(Aspect aspect) {
        this.aspects.remove(aspect);
        return this;
    }

    public AspectList reduceToPrimals(boolean merge) {
        AspectList result = new AspectList();
        for (Aspect aspect : getAspects()) {
            if (aspect.isPrimal()) {
                if (merge) result.merge(aspect, getAmount(aspect)); else result.add(aspect, getAmount(aspect));
            } else {
                AspectList parents = new AspectList();
                for (Aspect parent : aspect.getComponents()) parents.add(parent, getAmount(aspect));
                result.add(parents.reduceToPrimals(merge));
            }
        }
        return result;
    }

    public void writeToNBT(CompoundTag tag) { writeToNBT(tag, "Aspects"); }

    public void writeToNBT(CompoundTag tag, String label) {
        ListTag list = new ListTag();
        for (Aspect aspect : getAspects()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", getAmount(aspect));
            list.add(entry);
        }
        tag.put(label, list);
    }

    public void readFromNBT(CompoundTag tag) { readFromNBT(tag, "Aspects"); }

    public void readFromNBT(CompoundTag tag, String label) {
        this.aspects.clear();
        ListTag list = tag.getList(label, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            add(Aspect.getAspect(entry.getString("key")), entry.getInt("amount"));
        }
    }
}
