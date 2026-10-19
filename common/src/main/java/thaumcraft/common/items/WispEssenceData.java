package thaumcraft.common.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Only the original contained aspects, NEVER the item's separate auram2 base.
 * Ordered entries matter: ItemWispEssence:83 tints with the FIRST aspect.
 * Invalid/unknown/duplicate/nonpositive or overflowing data fails the native
 * component decode as a whole; it is not silently trimmed into a different item.
 * Every registered addon aspect is accepted, not just the built-in 48 tags.
 */
public final class WispEssenceData {
    private static final int MAX_TAG_LENGTH = 32_767; // Native UTF transport limit.
    // Original getBonusTags adds the separately registered two-unit item base.
    // Reserve that headroom rather than allow its native int sum to overflow.
    public static final int MAX_TOTAL = Integer.MAX_VALUE - 2;

    private record Entry(String tag, int amount) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.string(1, MAX_TAG_LENGTH).fieldOf("tag").forGetter(Entry::tag),
                Codec.intRange(1, MAX_TOTAL).fieldOf("amount").forGetter(Entry::amount)
        ).apply(instance, Entry::new));
    }

    public static final Codec<WispEssenceData> CODEC = Entry.CODEC.listOf().comapFlatMap(entries -> {
        try { return DataResult.success(new WispEssenceData(entries)); }
        catch (IllegalArgumentException exception) { return DataResult.error(exception::getMessage); }
    }, data -> data.entries);

    public static final StreamCodec<ByteBuf, WispEssenceData> STREAM_CODEC = new StreamCodec<>() {
        @Override public WispEssenceData decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            // Unique known tags can never outnumber the LIVE registry. This is
            // a bounded allocation without excluding legitimately added aspects.
            if (size < 1 || size > Aspect.aspects.size()) throw new DecoderException("Invalid wisp aspect count: " + size);
            List<Entry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) entries.add(new Entry(
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)));
            try { return new WispEssenceData(entries); }
            catch (IllegalArgumentException exception) { throw new DecoderException("Invalid wisp essence data", exception); }
        }

        @Override public void encode(ByteBuf buffer, WispEssenceData data) {
            ByteBufCodecs.VAR_INT.encode(buffer, data.entries.size());
            for (Entry entry : data.entries) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.tag());
                ByteBufCodecs.VAR_INT.encode(buffer, entry.amount());
            }
        }
    };

    private final List<Entry> entries;
    private final Map<String, Integer> amounts;

    public WispEssenceData(Map<String, Integer> amounts) {
        this(Objects.requireNonNull(amounts, "amounts").entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), Objects.requireNonNull(entry.getValue(), "amount"))).toList());
    }

    private WispEssenceData(List<Entry> entries) {
        if (entries.isEmpty() || entries.size() > Aspect.aspects.size()) throw new IllegalArgumentException("Invalid wisp aspect count: " + entries.size());
        var copy = new LinkedHashMap<String, Integer>();
        long total = 0;
        for (Entry entry : entries) {
            String tag = entry.tag();
            if (tag == null || tag.isEmpty() || tag.length() > MAX_TAG_LENGTH || Aspect.getAspect(tag) == null) {
                throw new IllegalArgumentException("Unknown or invalid wisp aspect: " + tag);
            }
            if (entry.amount() < 1 || entry.amount() > MAX_TOTAL || (total += entry.amount()) > MAX_TOTAL) {
                throw new IllegalArgumentException("Invalid wisp aspect amount/total");
            }
            if (copy.putIfAbsent(tag, entry.amount()) != null) throw new IllegalArgumentException("Duplicate wisp aspect: " + tag);
        }
        this.entries = List.copyOf(entries);
        this.amounts = Collections.unmodifiableMap(copy);
    }

    public static WispEssenceData fromAspects(AspectList aspects) {
        var amounts = new LinkedHashMap<String, Integer>();
        for (Aspect aspect : aspects.getAspects()) amounts.put(aspect.getTag(), aspects.getAmount(aspect));
        return new WispEssenceData(amounts);
    }

    /** Immutable insertion-ordered view; callers cannot mutate component state. */
    public Map<String, Integer> amounts() { return amounts; }
    public String firstTag() { return entries.getFirst().tag(); }
    public int amount(String tag) { return amounts.getOrDefault(tag, 0); }
    public AspectList toAspects() {
        AspectList result = new AspectList();
        for (Entry entry : entries) result.add(Aspect.getAspect(entry.tag()), entry.amount());
        return result;
    }

    // Map.equals ignores order, but equal native components must have equal
    // first-aspect tints and must not merge differently ordered containers.
    @Override public boolean equals(Object other) { return other instanceof WispEssenceData data && entries.equals(data.entries); }
    @Override public int hashCode() { return entries.hashCode(); }
    @Override public String toString() { return amounts.toString(); }
}
