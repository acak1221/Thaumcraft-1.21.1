package thaumcraft.common.items;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original ItemCrystalEssence's WHOLE ordered list, not a singleton item store.
 * Empty is explicit initialized-empty legacy NBT, distinct from NO component.
 * Bounds exclude only invalid registry/transport data and native int overflow:
 * distinct canonical tags cannot outnumber the LIVE registry, including addons.
 * Never trim a valid list to one type, sort it, or multiply it by stack count. */
public final class CrystalEssenceData {
    private static final int MAX_TAG_LENGTH = 32_767;
    public static final int MAX_TOTAL = Integer.MAX_VALUE; // Crystal base is EMPTY.

    // Codec.INT's Number.intValue can truncate fractional/oversized JSON numbers.
    // Validate BEFORE conversion, not after a damaged value becomes a valid int.
    private static final Codec<Integer> POSITIVE_INT = new Codec<>() {
        @Override public <T> DataResult<Pair<Integer, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input).flatMap(number -> {
                try {
                    int value = new BigDecimal(number.toString()).intValueExact();
                    if (value < 1) return DataResult.error(() -> "Invalid crystal aspect amount: " + number);
                    return DataResult.success(Pair.of(value, ops.empty()));
                } catch (NumberFormatException | ArithmeticException invalid) {
                    return DataResult.error(() -> "Invalid crystal aspect amount: " + number);
                }
            });
        }
        @Override public <T> DataResult<T> encode(Integer input, DynamicOps<T> ops, T prefix) {
            if (input == null || input < 1) return DataResult.error(() -> "Invalid crystal aspect amount");
            return ops.mergeToPrimitive(prefix, ops.createInt(input));
        }
    };

    private record Entry(String tag, int amount) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.string(1, MAX_TAG_LENGTH).fieldOf("tag").forGetter(Entry::tag),
                POSITIVE_INT.fieldOf("amount").forGetter(Entry::amount)
        ).apply(instance, Entry::new));
    }

    public static final CrystalEssenceData EMPTY = new CrystalEssenceData(List.of());
    private static final Codec<List<Entry>> ENTRIES_CODEC = Entry.CODEC.listOf();
    public static final Codec<CrystalEssenceData> CODEC = new Codec<>() {
        @Override public <T> DataResult<Pair<CrystalEssenceData, T>> decode(DynamicOps<T> ops, T input) {
            var stream = ops.getStream(input);
            if (stream.error().isPresent()) return DataResult.error(() -> stream.error().orElseThrow().message());
            long size;
            try (var values = stream.result().orElseThrow()) { size = values.limit((long) Aspect.aspects.size() + 1).count(); }
            if (size > Aspect.aspects.size()) return DataResult.error(() -> "Invalid crystal aspect count: " + size);
            var decoded = ENTRIES_CODEC.decode(ops, input);
            // ListCodec may offer the valid SUBSET as a partial result. Strip
            // that partial completely: recovery may not mint a different item.
            if (decoded.error().isPresent()) return DataResult.error(() -> decoded.error().orElseThrow().message());
            var pair = decoded.result().orElseThrow();
            try { return DataResult.success(Pair.of(new CrystalEssenceData(pair.getFirst()), pair.getSecond())); }
            catch (IllegalArgumentException invalid) { return DataResult.error(invalid::getMessage); }
        }
        @Override public <T> DataResult<T> encode(CrystalEssenceData input, DynamicOps<T> ops, T prefix) {
            return ENTRIES_CODEC.encode(input.entries, ops, prefix);
        }
    };

    public static final StreamCodec<ByteBuf, CrystalEssenceData> STREAM_CODEC = new StreamCodec<>() {
        @Override public CrystalEssenceData decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            if (size < 0 || size > Aspect.aspects.size()) throw new DecoderException("Invalid crystal aspect count: " + size);
            var entries = new ArrayList<Entry>(size);
            for (int index = 0; index < size; index++) entries.add(new Entry(
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)));
            try { return new CrystalEssenceData(entries); }
            catch (IllegalArgumentException exception) { throw new DecoderException("Invalid crystal essence data", exception); }
        }
        @Override public void encode(ByteBuf buffer, CrystalEssenceData data) {
            ByteBufCodecs.VAR_INT.encode(buffer, data.entries.size());
            for (Entry entry : data.entries) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.tag());
                ByteBufCodecs.VAR_INT.encode(buffer, entry.amount());
            }
        }
    };

    private final List<Entry> entries;
    private final Map<String, Integer> amounts;

    public CrystalEssenceData(Map<String, Integer> amounts) { this(entries(amounts)); }

    private static List<Entry> entries(Map<String, Integer> amounts) {
        if (amounts == null) throw new IllegalArgumentException("Null crystal aspect list");
        var entries = new ArrayList<Entry>(amounts.size());
        amounts.forEach((tag, amount) -> {
            if (amount == null) throw new IllegalArgumentException("Null crystal aspect amount");
            entries.add(new Entry(tag, amount));
        });
        return entries;
    }

    private CrystalEssenceData(List<Entry> entries) {
        if (entries.size() > Aspect.aspects.size()) throw new IllegalArgumentException("Invalid crystal aspect count: " + entries.size());
        var copy = new LinkedHashMap<String, Integer>();
        long total = 0;
        for (Entry entry : entries) {
            String tag = entry.tag();
            Aspect aspect = tag == null ? null : Aspect.getAspect(tag);
            if (tag == null || tag.isEmpty() || tag.length() > MAX_TAG_LENGTH || aspect == null || !tag.equals(aspect.getTag())) {
                throw new IllegalArgumentException("Unknown or noncanonical crystal aspect: " + tag);
            }
            if (entry.amount() < 1 || (total += entry.amount()) > MAX_TOTAL) throw new IllegalArgumentException("Invalid crystal aspect amount/total");
            if (copy.putIfAbsent(tag, entry.amount()) != null) throw new IllegalArgumentException("Duplicate crystal aspect: " + tag);
        }
        this.entries = List.copyOf(entries);
        this.amounts = Collections.unmodifiableMap(copy);
    }

    /** Validate the complete mutable legacy list before changing any ItemStack. */
    public static CrystalEssenceData fromAspects(AspectList aspects) {
        if (aspects == null) throw new IllegalArgumentException("Null crystal aspect list");
        var entries = new ArrayList<Entry>(aspects.aspects.size());
        aspects.aspects.forEach((aspect, amount) -> {
            if (aspect == null || Aspect.getAspect(aspect.getTag()) != aspect || amount == null) {
                throw new IllegalArgumentException("Null or noncanonical crystal aspect/amount");
            }
            entries.add(new Entry(aspect.getTag(), amount));
        });
        return new CrystalEssenceData(entries);
    }

    public boolean isEmpty() { return entries.isEmpty(); }
    /** Immutable insertion-ordered view. Empty has no first tag. */
    public Map<String, Integer> amounts() { return amounts; }
    public String firstTag() { return isEmpty() ? null : entries.getFirst().tag(); }
    public int amount(String tag) { return amounts.getOrDefault(tag, 0); }
    public AspectList toAspects() {
        var result = new AspectList();
        for (Entry entry : entries) result.add(Aspect.getAspect(entry.tag()), entry.amount());
        return result;
    }

    // Native stack equality MUST preserve first-entry tint and the full order.
    @Override public boolean equals(Object other) { return other instanceof CrystalEssenceData data && entries.equals(data.entries); }
    @Override public int hashCode() { return entries.hashCode(); }
    @Override public String toString() { return amounts.toString(); }
}
