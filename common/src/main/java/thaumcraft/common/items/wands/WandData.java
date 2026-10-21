package thaumcraft.common.items.wands;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, persistent/synchronized item component. Amounts are TC4 centivis; the largest capacity is a staff's 250 whole vis. */
public record WandData(String rod, String cap, Map<String, Integer> vis) {
    /** The most one aspect can hold: the silverwood and primal staves as a sceptre, 375 whole vis (original rod capacity x 150). */
    public static final int MAX_CENTIVIS = WandMaterials.MAX_CAPACITY;
    public static final List<String> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");
    public static final WandData EMPTY = new WandData("wood", "iron", Map.of());
    private static final Codec<String> ROD_CODEC = Codec.STRING.comapFlatMap(
            value -> WandMaterials.supportsRod(value) ? DataResult.success(value)
                    : DataResult.error(() -> "Unsupported wand rod: " + value), value -> value);
    private static final Codec<String> CAP_CODEC = Codec.STRING.comapFlatMap(
            value -> WandMaterials.supportsCap(value) ? DataResult.success(value)
                    : DataResult.error(() -> "Unsupported wand cap: " + value), value -> value);
    private static final Codec<String> ASPECT_CODEC = Codec.STRING.comapFlatMap(
            value -> PRIMALS.contains(value) ? DataResult.success(value)
                    : DataResult.error(() -> "Non-primal wand aspect: " + value), value -> value);
    // Decode cross-field capacity errors as DataResults, not constructor exceptions
    // escaping an ItemStack/NBT/network decoder. Old wood/iron saves keep their defaults.
    private record Encoded(String rod, String cap, Map<String, Integer> vis) {}
    private static final Codec<Encoded> ENCODED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ROD_CODEC.optionalFieldOf("rod", "wood").forGetter(Encoded::rod),
            CAP_CODEC.optionalFieldOf("cap", "iron").forGetter(Encoded::cap),
            Codec.unboundedMap(ASPECT_CODEC, Codec.intRange(0, MAX_CENTIVIS)).optionalFieldOf("vis", Map.of()).forGetter(Encoded::vis)
    ).apply(instance, Encoded::new));
    public static final Codec<WandData> CODEC = ENCODED_CODEC.flatXmap(encoded -> {
        int capacity = maxCapacity(encoded.rod());
        if (encoded.vis().values().stream().anyMatch(amount -> amount > capacity)) {
            return DataResult.error(() -> "Wand vis exceeds " + encoded.rod() + " capacity: " + capacity);
        }
        return DataResult.success(new WandData(encoded.rod(), encoded.cap(), encoded.vis()));
    }, data -> DataResult.success(new Encoded(data.rod(), data.cap(), data.vis())));
    public static final StreamCodec<ByteBuf, WandData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public WandData {
        int capacity = maxCapacity(rod);
        WandMaterials.cap(cap);
        Objects.requireNonNull(vis, "vis");
        LinkedHashMap<String, Integer> copy = new LinkedHashMap<>();
        vis.forEach((tag, amount) -> {
            if (tag == null || amount == null || !PRIMALS.contains(tag) || amount < 0 || amount > capacity) {
                throw new IllegalArgumentException("Invalid wand vis: " + tag);
            }
            copy.put(tag, amount);
        });
        vis = Map.copyOf(copy);
    }

    public int amount(String tag) { return vis.getOrDefault(tag, 0); }
    /** The capacity of the rod alone: the amount a plain wand of this rod holds. */
    public int capacity() { return WandMaterials.capacity(rod); }
    /**
     * The largest amount any wand of this rod can hold: the SCEPTRE capacity of the same rod.
     *
     * <p>The record cannot see the sceptre marker, which is a separate data component of the stack, so it
     * accepts the largest capacity of its rod. The exact capacity of a stack is
     * {@link ItemWandCasting#getMaxVis(ItemStack)}.</p>
     */
    public int maxCapacity() { return maxCapacity(rod); }

    private static int maxCapacity(String rod) { return WandMaterials.capacity(rod, true); }

    public WandData withAmount(String tag, int amount) {
        if (!PRIMALS.contains(tag)) throw new IllegalArgumentException("Non-primal aspect " + tag);
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>(vis);
        values.put(tag, Math.max(0, Math.min(amount, maxCapacity())));
        return new WandData(rod, cap, values);
    }
}
