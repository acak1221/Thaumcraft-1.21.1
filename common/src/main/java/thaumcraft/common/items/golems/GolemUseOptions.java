package thaumcraft.common.items.golems;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Original ordinary Use8 controls: block/empty space, right/left, sneak.
 * Configuration only; neither an entity-target mode nor item/click authority. */
public record GolemUseOptions(boolean emptySpace, boolean leftClick, boolean sneaking) {
    public static final GolemUseOptions DEFAULT = new GolemUseOptions(false, false, false);
    private static final Set<String> FIELD_NAMES = Set.of("empty_space", "left_click", "sneaking");

    /** Compare the actual representation, not DynamicOps' coercing boolean
     * getter: JSON booleans and native NBT byte0/1, never JSON numbers/strings,
     * NBT ints/floats or arbitrary nonzero bytes. RegistryOps delegates creation. */
    private static final Codec<Boolean> STRICT_BOOLEAN = new Codec<>() {
        @Override public <T> DataResult<Pair<Boolean, T>> decode(DynamicOps<T> ops, T input) {
            if (ops.createBoolean(false).equals(input)) return DataResult.success(Pair.of(false, ops.empty()));
            if (ops.createBoolean(true).equals(input)) return DataResult.success(Pair.of(true, ops.empty()));
            return DataResult.error(() -> "Expected a boolean Use option (native NBT byte0/1)");
        }

        @Override public <T> DataResult<T> encode(Boolean input, DynamicOps<T> ops, T prefix) {
            return Codec.BOOL.encode(input, ops, prefix);
        }
    };

    private static final Codec<GolemUseOptions> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
            STRICT_BOOLEAN.optionalFieldOf("empty_space", false).forGetter(GolemUseOptions::emptySpace),
            STRICT_BOOLEAN.optionalFieldOf("left_click", false).forGetter(GolemUseOptions::leftClick),
            STRICT_BOOLEAN.optionalFieldOf("sneaking", false).forGetter(GolemUseOptions::sneaking)
    ).apply(instance, GolemUseOptions::new));

    public static final Codec<GolemUseOptions> CODEC = new Codec<>() {
        @Override public <T> DataResult<Pair<GolemUseOptions, T>> decode(DynamicOps<T> ops, T input) {
            var decodedMap = ops.getMap(input);
            if (decodedMap.error().isPresent()) return DataResult.error(() -> "Expected golem Use options object");
            var map = decodedMap.result().orElseThrow();
            var keys = new HashSet<String>();
            try (var entries = map.entries()) {
                var iterator = entries.iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    var key = ops.getStringValue(entry.getFirst()).result().orElse(null);
                    if (key == null || !FIELD_NAMES.contains(key) || !keys.add(key)) {
                        return DataResult.error(() -> "Unsupported or duplicate golem Use option");
                    }
                    // A present malformed optional flag must not become false.
                    if (STRICT_BOOLEAN.parse(ops, entry.getSecond()).error().isPresent()) {
                        return DataResult.error(() -> "Invalid golem Use option: " + key);
                    }
                }
            }
            var result = FIELDS.decode(ops, input);
            if (result.error().isPresent()) return DataResult.error(() -> result.error().orElseThrow().message());
            return result;
        }

        @Override public <T> DataResult<T> encode(GolemUseOptions input, DynamicOps<T> ops, T prefix) {
            return FIELDS.encode(input, ops, prefix);
        }
    };

    /** Three independent native boolean fields, in record order. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GolemUseOptions> STREAM_CODEC = new StreamCodec<>() {
        @Override public GolemUseOptions decode(RegistryFriendlyByteBuf buffer) {
            return new GolemUseOptions(readBoolean(buffer), readBoolean(buffer), readBoolean(buffer));
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, GolemUseOptions options) {
            buffer.writeBoolean(options.emptySpace);
            buffer.writeBoolean(options.leftClick);
            buffer.writeBoolean(options.sneaking);
        }
    };

    private static boolean readBoolean(RegistryFriendlyByteBuf buffer) {
        int value = buffer.readUnsignedByte();
        if (value > 1) throw new DecoderException("Invalid golem Use option boolean");
        return value == 1;
    }

    public boolean isDefault() { return !emptySpace && !leftClick && !sneaking; }

    /** Optional future bounded-runtime gate, not an upgrade requirement. */
    public boolean supportsDefaultBaseline() { return isDefault(); }
}
