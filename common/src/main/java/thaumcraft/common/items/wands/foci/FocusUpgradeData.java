package thaumcraft.common.items.wands.foci;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftContent;

/** Five ordered TC4 rank slots. Missing data is empty; holes are not compacted.
 * No list/array alias, aggregate-level cache, legacy NBT migration or enchantment. */
public record FocusUpgradeData(List<Short> ranks) {
    public static final int SLOTS = 5;
    public static final FocusUpgradeData EMPTY = new FocusUpgradeData(List.of((short)-1, (short)-1, (short)-1, (short)-1, (short)-1));
    private static final Codec<Short> ID_CODEC = Codec.intRange(-1, Short.MAX_VALUE).xmap(value -> (short)(int)value, Short::intValue);
    public static final Codec<FocusUpgradeData> CODEC = ID_CODEC.listOf(SLOTS, SLOTS).flatXmap(
            values -> valid(values) ? DataResult.success(new FocusUpgradeData(values))
                    : DataResult.error(() -> "Expected five supported focus upgrade IDs or -1"),
            data -> DataResult.success(data.ranks));
    // Fixed ten bytes: no attacker-supplied count, oversized allocation or ignored tail.
    public static final StreamCodec<ByteBuf, FocusUpgradeData> STREAM_CODEC = new StreamCodec<>() {
        @Override public FocusUpgradeData decode(ByteBuf buffer) {
            List<Short> values = new ArrayList<>(SLOTS);
            for (int slot = 0; slot < SLOTS; slot++) values.add(buffer.readShort());
            if (!valid(values)) throw new DecoderException("Unsupported focus upgrade ID");
            return new FocusUpgradeData(values);
        }
        @Override public void encode(ByteBuf buffer, FocusUpgradeData data) {
            for (short id : data.ranks) buffer.writeShort(id);
        }
    };

    public FocusUpgradeData {
        if (!valid(ranks)) throw new IllegalArgumentException("Expected five supported focus upgrade IDs or -1");
        ranks = List.copyOf(ranks);
    }

    private static boolean valid(List<Short> values) {
        return values != null && values.size() == SLOTS
                && values.stream().allMatch(id -> id != null && (id == -1 || FocusUpgradeType.get(id) != null));
    }

    public short[] applied() {
        short[] result = new short[SLOTS];
        for (int slot = 0; slot < SLOTS; slot++) result[slot] = ranks.get(slot);
        return result;
    }
    public int level(FocusUpgradeType type) {
        if (type == null || FocusUpgradeType.get(type.id) != type) return 0;
        int count = 0;
        for (short id : ranks) if (id == type.id) count++;
        return count;
    }
    public FocusUpgradeData withUpgrade(FocusUpgradeType type, int rank) {
        if (rank < 1 || rank > SLOTS || type == null || FocusUpgradeType.get(type.id) != type || ranks.get(rank - 1) != -1)
            throw new IllegalArgumentException("Invalid or occupied focus rank");
        var values = new ArrayList<>(ranks);
        values.set(rank - 1, type.id);
        return new FocusUpgradeData(values);
    }

    public static FocusUpgradeData read(ItemStack focus) {
        return focus == null || focus.isEmpty() ? EMPTY : focus.getOrDefault(ThaumcraftFoci.FOCUS_UPGRADES.get(), EMPTY);
    }

    /** Item-specific checks also cover data attached directly without a decoder. */
    public static boolean supportedForFocus(ItemStack focus) {
        if (focus == null || focus.isEmpty() || focus.getCount() != 1
                || focus.has(ThaumcraftFoci.WAND_FOCUS.get()) || focus.has(ThaumcraftContent.WAND_DATA.get())
                || !(focus.getItem() instanceof ItemFocusFire || focus.getItem() instanceof ItemFocusFrost
                    || focus.getItem() instanceof ItemFocusShock || focus.getItem() instanceof ItemFocusExcavation
                    || focus.getItem() instanceof ItemFocusPortableHole || focus.getItem() instanceof ItemFocusWarding
                    || focus.getItem() instanceof ItemFocusTrade)) return false;
        if (focus.getItem() instanceof ItemFocusWarding && read(focus).level(FocusUpgradeType.enlarge) > 0
                && read(focus).level(FocusUpgradeType.architect) == 0) return false;
        ItemFocusBasic item = (ItemFocusBasic) focus.getItem();
        List<Short> ranks = read(focus).ranks;
        for (int slot = 0; slot < SLOTS; slot++) {
            short id = ranks.get(slot);
            if (id == -1) continue;
            boolean found = false;
            FocusUpgradeType[] choices = item.getPossibleUpgradesByRank(focus, slot + 1);
            if (choices != null) for (FocusUpgradeType choice : choices) if (choice != null && choice.id == id) found = true;
            if (!found) return false;
        }
        return true;
    }

    public static int level(ItemStack focus, FocusUpgradeType type) {
        if (focus == null || focus.isEmpty()) return 0;
        if (!supportedForFocus(focus)) throw new IllegalArgumentException("Unsupported focus upgrade data");
        return read(focus).level(type);
    }
}
