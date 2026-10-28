package thaumcraft.api.aspects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** TC4's ordered aspect amounts. add sums; merge takes the maximum. */
public class AspectList implements Serializable {
    public final LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

    /** One written entry of the codecs: the tag of an aspect and its amount. */
    private record Row(Aspect aspect, int amount) {
        static final Codec<Row> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Aspect.CODEC.fieldOf("tag").forGetter(Row::aspect),
                ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(Row::amount)
        ).apply(instance, Row::new));
    }

    /**
     * Codec of an aspect list, so that a data component, a JSON file or saved data can hold aspects. Not a 1.7.10
     * member: an add-on of the original wrote {@link #writeToNBT(CompoundTag)} into an NBT tag itself.
     *
     * <p><b>Written form.</b> A list of entries in the order of the list: {@code [{"tag": "ignis", "amount": 4},
     * {"tag": "tempus", "amount": 2}]} (the tag of {@link Aspect#CODEC}, a positive int). The empty list is
     * {@code []} and is a valid, empty {@code AspectList}. Decoding keeps the written order, which is the insertion
     * order of the result.</p>
     *
     * <p><b>Strict decoding.</b> An unknown tag (an add-on aspect that is not registered, see {@link Aspect#CODEC}), an
     * amount below one and a tag that appears twice are errors of the whole list: the data is never trimmed or summed
     * into a different list. The result is a new mutable list on every decode.</p>
     *
     * <p><b>Encoding.</b> An entry with an amount of zero or less is not written: such an entry holds no aspect, and
     * {@link #reduce} leaves exactly one behind when it empties an aspect, so a list that was reduced still saves.
     * {@link #getAmount} of a decoded list therefore equals that of the encoded one for every aspect with a positive
     * amount, and is zero for the others. Encoding an aspect that is not registered under its own tag is an error
     * ({@link Aspect#CODEC}).</p>
     *
     * <p><b>Equality.</b> {@code AspectList} compares by identity, as in the original. Two stacks whose data component
     * holds equal but distinct lists are therefore different stacks (they do not merge). A component that must compare
     * by value holds a small immutable value type of its own that wraps the list, copies it on the way in and out and
     * compares {@link #getAspects()} and {@link #getAmount} (the guide shows one).</p>
     *
     * <p><b>Side and thread.</b> Pure; either side, any thread that may read the aspect registry.</p>
     */
    public static final Codec<AspectList> CODEC = Row.CODEC.listOf().flatXmap(AspectList::fromRows, AspectList::toRows);

    /**
     * Network codec of an aspect list, usable with any {@link ByteBuf} (so as the {@code networkSynchronized} codec
     * of a data component type): a var-int count, then per entry the tag ({@link Aspect#STREAM_CODEC}) and the amount
     * as a var-int. Same rules as {@link #CODEC}: entries of zero or less are not written, order is kept.
     *
     * <p>Decoding throws {@link DecoderException} for a count above the number of registered aspects (distinct aspects
     * cannot outnumber the registry, which bounds the allocation), an unknown tag, an amount below one and a repeated
     * tag. Client and server must register the same aspects, see {@link Aspect}.</p>
     */
    public static final StreamCodec<ByteBuf, AspectList> STREAM_CODEC = new StreamCodec<>() {
        @Override public AspectList decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            if (size < 0 || size > Aspect.aspects.size()) {
                throw new DecoderException("Invalid aspect list size " + size + " (" + Aspect.aspects.size() + " aspects are registered)");
            }
            AspectList list = new AspectList();
            for (int index = 0; index < size; index++) {
                Aspect aspect = Aspect.STREAM_CODEC.decode(buffer);
                int amount = ByteBufCodecs.VAR_INT.decode(buffer);
                if (amount < 1) throw new DecoderException("Invalid amount " + amount + " of aspect '" + aspect.getTag() + "'");
                if (list.aspects.put(aspect, amount) != null) throw new DecoderException("Duplicate aspect '" + aspect.getTag() + "' in an aspect list");
            }
            return list;
        }

        @Override public void encode(ByteBuf buffer, AspectList list) {
            List<Row> rows = rows(list);
            ByteBufCodecs.VAR_INT.encode(buffer, rows.size());
            for (Row row : rows) {
                Aspect.STREAM_CODEC.encode(buffer, row.aspect());
                ByteBufCodecs.VAR_INT.encode(buffer, row.amount());
            }
        }
    };

    private static DataResult<AspectList> fromRows(List<Row> rows) {
        AspectList list = new AspectList();
        for (Row row : rows) {
            if (list.aspects.put(row.aspect(), row.amount()) != null) {
                return DataResult.error(() -> "Duplicate aspect '" + row.aspect().getTag() + "' in an aspect list");
            }
        }
        return DataResult.success(list);
    }

    private static DataResult<List<Row>> toRows(AspectList list) {
        return list == null ? DataResult.error(() -> "Cannot write a null aspect list") : DataResult.success(rows(list));
    }

    /** The entries that carry an aspect: positive amounts only, in the order of the list. */
    private static List<Row> rows(AspectList list) {
        List<Row> rows = new ArrayList<>(list.aspects.size());
        for (Map.Entry<Aspect, Integer> entry : list.aspects.entrySet()) {
            Integer amount = entry.getValue();
            if (entry.getKey() != null && amount != null && amount > 0) rows.add(new Row(entry.getKey(), amount));
        }
        return rows;
    }

    /**
     * Original AspectList:25-36 - the object aspects of an item stack.
     * Null/empty stacks are passed to the handler; Exceptions are swallowed (retaining any amounts already copied), but Errors escape.
     *
     * <p>The original wrapped the call in a try/catch and kept an empty list when the add-on
     * surface threw; the native keeps the same behaviour.</p>
     */
    public AspectList(ItemStack stack) {
        try {
            AspectList temp = ThaumcraftApiHelper.getObjectAspects(stack);
            if (temp != null) {
                for (Aspect tag : temp.getAspects()) {
                    this.add(tag, temp.getAmount(tag));
                }
            }
        } catch (Exception exception) {
            // The original swallowed the failure of the add-on surface and kept an empty list.
        }
    }

    /** Creates an empty mutable list; no registry or world state is changed. */
    public AspectList() {
    }

    /** Returns an independent amount map sharing the same mutable Aspect identities. */
    public AspectList copy() {
        AspectList result = new AspectList();
        result.aspects.putAll(this.aspects);
        return result;
    }

    /** Returns the number of distinct keys, including keys with zero or negative amounts. */
    public int size() { return this.aspects.size(); }
    /** Returns the signed sum in this list's consumer-defined units; integer overflow is unchecked. */
    public int visSize() { return this.aspects.values().stream().mapToInt(Integer::intValue).sum(); }
    /** Returns the stored amount, or zero for an absent key (including null); does not mutate this list. */
    public int getAmount(Aspect aspect) { return this.aspects.getOrDefault(aspect, 0); }
    /** Returns a new insertion-ordered key array; Aspect objects themselves remain shared. */
    public Aspect[] getAspects() { return this.aspects.keySet().toArray(Aspect[]::new); }
    /** Returns a new array of primal keys; a null key inserted directly into the public map throws NPE. */
    public Aspect[] getPrimalAspects() { return this.aspects.keySet().stream().filter(Aspect::isPrimal).toArray(Aspect[]::new); }

    /** Returns a new key array sorted by tag; null keys/tags inserted through mutable state can throw NPE. */
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

    /** Returns a new key array using {@link #getAspectsSortedAmount()}; never changes the stored order. */
    public Aspect[] getAspectsSortedByAmount() { return getAspectsSortedAmount(); }

    /**
     * The original addition: the amount of an aspect grows by the added one.
     * Null aspects are ignored; signed amounts are summed without overflow checks.
     *
     * <p>Forced 1.21.1 change (documented deviation from the original): the original stored the aspect
     * under its own key, so a {@code null} aspect - the aspect of an unknown tag that
     * {@link Aspect#getAspect(String)} answered for - became a key of the map (oracle
     * {@code thaumcraft/api/aspects/AspectList.java}, the {@code add} method has no null check). The
     * original then crashed with a {@link NullPointerException} in {@code getPrimalAspects}, which calls
     * {@code isPrimal()} on every key without a null check. The native port drops a {@code null} aspect
     * instead, so an item of another mod never takes the whole aspect list down; the amount of an
     * unknown tag is then not counted by {@link #size()} and not answered by {@link #getAmount(Aspect)}.</p>
     */
    public AspectList add(Aspect aspect, int amount) {
        if (aspect != null) this.aspects.put(aspect, getAmount(aspect) + amount);
        return this;
    }

    /** Adds other's amounts into this receiver and returns this; null other throws NPE, and units are not converted. */
    public AspectList add(AspectList other) {
        if (other == null) throw new IllegalArgumentException("other must not be null");
        for (Aspect aspect : other.getAspects()) add(aspect, other.getAmount(aspect));
        return this;
    }

    /** Stores the maximum existing/supplied signed amount and returns this; null aspect is ignored, with no unit conversion. */
    public AspectList merge(Aspect aspect, int amount) {
        if (aspect != null) this.aspects.put(aspect, this.aspects.containsKey(aspect) ? Math.max(getAmount(aspect), amount) : amount);
        return this;
    }

    /** Merges each amount by maximum into this receiver and returns this; null other throws NPE. */
    public AspectList merge(AspectList other) {
        if (other == null) throw new IllegalArgumentException("other must not be null");
        for (Aspect aspect : other.getAspects()) merge(aspect, other.getAmount(aspect));
        return this;
    }

    /** Subtracts amount in the list's units, retaining a zero key; null aspect, negative amount or insufficient stock returns false unchanged. */
    public boolean reduce(Aspect aspect, int amount) {
        if (aspect == null || amount < 0 || getAmount(aspect) < amount) return false;
        this.aspects.put(aspect, getAmount(aspect) - amount);
        return true;
    }

    /** Subtracts amount in the list's units and removes nonpositive stock; null aspect/negative amount is ignored, returning this. */
    public AspectList remove(Aspect aspect, int amount) {
        if (aspect == null || amount < 0) return this;
        int left = getAmount(aspect) - amount;
        if (left <= 0) this.aspects.remove(aspect); else this.aspects.put(aspect, left);
        return this;
    }

    /** Removes the key (null is allowed by the map) and returns this; no other state changes. */
    public AspectList remove(Aspect aspect) {
        this.aspects.remove(aspect);
        return this;
    }

    /** Returns a new recursive primal decomposition, preserving amount units; merge uses maxima within each level, not global deduplication. */
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

    /** Writes amounts without unit conversion to tag's Aspects list; null tag throws NPE. */
    public void writeToNBT(CompoundTag tag) { writeToNBT(tag, "Aspects"); }

    /** Replaces tag's named list with aspect tags and signed amounts; tag/label must be non-null, and this list is unchanged. */
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

    /** Clears this receiver and reads tag's Aspects list; null tag throws NPE after clearing. */
    public void readFromNBT(CompoundTag tag) { readFromNBT(tag, "Aspects"); }

    /** Clears this receiver and adds named-list amounts unchanged; unknown aspect names are dropped, missing lists read empty, and tag/label must be non-null. */
    public void readFromNBT(CompoundTag tag, String label) {
        if (tag == null) throw new IllegalArgumentException("tag must not be null");
        this.aspects.clear();
        ListTag list = tag.getList(label, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            add(Aspect.getAspect(entry.getString("key")), entry.getInt("amount"));
        }
    }
}
