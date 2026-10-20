package thaumcraft.common.lib.research;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.math.BigDecimal;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.ItemResearchNotes;

/** Original research-note copying grows a max-stack-one item in place. Native inventory codecs cap a saved count at 99. */
public final class ResearchNoteOverstackCodec {
    private ResearchNoteOverstackCodec() {}

    /** Wrap the original codec BEFORE OPTIONAL_CODEC and all inventory/item-entity users capture it. Never widen other items. */
    public static Codec<ItemStack> wrap(Codec<ItemStack> vanilla) {
        return new Codec<>() {
            @Override public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
                int count = largeCount(ops, input);
                if (count <= 99) return vanilla.decode(ops, input);
                return vanilla.decode(ops, ops.set(input, "count", ops.createInt(1))).flatMap(decoded -> {
                    ItemStack stack = decoded.getFirst();
                    if (!(stack.getItem() instanceof ItemResearchNotes)) return DataResult.error(() -> "Oversized saved count is supported only for research notes");
                    stack.setCount(count);
                    return DataResult.success(Pair.of(stack, decoded.getSecond()));
                });
            }
            @Override public <T> DataResult<T> encode(ItemStack stack, DynamicOps<T> ops, T prefix) {
                if (stack.getCount() <= 99 || !(stack.getItem() instanceof ItemResearchNotes)) return vanilla.encode(stack, ops, prefix);
                ItemStack one = stack.copyWithCount(1);
                return vanilla.encode(one, ops, prefix).map(encoded -> ops.set(encoded, "count", ops.createInt(stack.getCount())));
            }
        };
    }
    private static <T> int largeCount(DynamicOps<T> ops, T input) {
        var map = ops.getMap(input).result();
        if (map.isEmpty()) return 0;
        T value = map.get().get("count");
        if (value == null) return 0;
        var number = ops.getNumberValue(value).result();
        if (number.isEmpty()) return 0;
        try { return new BigDecimal(number.get().toString()).intValueExact(); }
        catch (NumberFormatException | ArithmeticException malformed) { return 0; }
    }
}
