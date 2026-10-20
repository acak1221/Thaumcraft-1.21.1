package thaumcraft.common.items.baubles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Immutable native component; both capacities have independently bounded disk/network codecs. */
public record VisAmuletData(Map<String, Integer> vis) {
    public static final List<String> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");
    public static final VisAmuletData EMPTY = new VisAmuletData(Map.of());
    private static final Codec<String> ASPECT = Codec.STRING.comapFlatMap(tag -> PRIMALS.contains(tag)
            ? DataResult.success(tag) : DataResult.error(() -> "Non-primal amulet aspect: " + tag), tag -> tag);
    public VisAmuletData {
        LinkedHashMap<String,Integer> copy = new LinkedHashMap<>();
        vis.forEach((tag, amount) -> {
            if (!PRIMALS.contains(tag) || amount == null || amount < 0 || amount > 25000) throw new IllegalArgumentException("Invalid amulet vis");
            copy.put(tag, amount);
        });
        vis = Map.copyOf(copy);
    }
    public static Codec<VisAmuletData> codec(int capacity) {
        return Codec.unboundedMap(ASPECT, Codec.intRange(0, capacity)).xmap(VisAmuletData::new, VisAmuletData::vis);
    }
    public static StreamCodec<ByteBuf,VisAmuletData> streamCodec(int capacity) { return ByteBufCodecs.fromCodec(codec(capacity)); }
    public int amount(String tag) { return vis.getOrDefault(tag, 0); }
    public VisAmuletData withAmount(String tag, int amount, int capacity) {
        if (!PRIMALS.contains(tag) || amount < 0 || amount > capacity) throw new IllegalArgumentException("Amulet vis exceeds capacity");
        var copy = new LinkedHashMap<>(vis); copy.put(tag, amount); return new VisAmuletData(copy);
    }
}
