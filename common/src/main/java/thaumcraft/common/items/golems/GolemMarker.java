package thaumcraft.common.items.golems;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Original Marker:14-35, with a namespaced dimension. {@code color} is the Order upgrade's marker colour: {@link #ANY} (-1) is
 * the ordinary uncoloured marker, 0..15 are the dye colours. The colour takes part in equality (original {@code Marker.equals});
 * matching against a slot colour is {@link #accepts}. A marker without a colour keeps its pre-Order serialised form.
 */
public record GolemMarker(ResourceLocation dimension, BlockPos pos, Direction face, int color) {
    public static final int ANY = -1, LAST_COLOR = 15;
    private static final Set<String> REQUIRED = Set.of("dimension", "pos", "face");
    private static final Set<String> FIELDS = Set.of("dimension", "pos", "face", "color");
    private static final Codec<GolemMarker> VALUES = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(GolemMarker::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(GolemMarker::pos),
            Direction.CODEC.fieldOf("face").forGetter(GolemMarker::face),
            Codec.intRange(ANY, LAST_COLOR).optionalFieldOf("color", ANY).forGetter(GolemMarker::color)
    ).apply(instance, GolemMarker::new));

    /** Unknown future fields must not become an ordinary marker; an out-of-range colour is a decode error. */
    public static final Codec<GolemMarker> CODEC = new Codec<>() {
        @Override public <T> DataResult<Pair<GolemMarker, T>> decode(DynamicOps<T> ops, T input) {
            var decodedMap = ops.getMap(input);
            if (decodedMap.error().isPresent()) return DataResult.error(() -> "Expected golem marker object");
            var map = decodedMap.result().orElseThrow();
            var keys = new HashSet<String>();
            try (var entries = map.entries()) {
                var iterator = entries.iterator();
                while (iterator.hasNext()) {
                    var key = ops.getStringValue(iterator.next().getFirst()).result().orElse(null);
                    if (key == null || !FIELDS.contains(key) || !keys.add(key)) {
                        return DataResult.error(() -> "Unsupported or duplicate golem marker field");
                    }
                }
            }
            if (!keys.containsAll(REQUIRED)) return DataResult.error(() -> "Golem marker requires dimension, pos and face");
            try {
                // BlockPos's native integer stream must not truncate malformed coordinates.
                var coordinates = ops.getStream(map.get("pos")).result()
                        .orElseThrow(() -> new IllegalArgumentException("Expected three marker coordinates"));
                try (coordinates) {
                    var values = coordinates.limit(4).toList();
                    if (values.size() != 3) throw new IllegalArgumentException("Expected three marker coordinates");
                    for (var value : values) exactInt(ops, value, "Invalid marker coordinate");
                }
                if (keys.contains("color")) {
                    // Exact integer first: the native INT codec would truncate 3.7 or 2^32 + 1 into a valid colour.
                    int color = exactInt(ops, map.get("color"), "Invalid marker colour");
                    if (color < ANY || color > LAST_COLOR) throw new IllegalArgumentException("Invalid marker colour");
                }
                var result = VALUES.decode(ops, input);
                if (result.error().isPresent()) return DataResult.error(() -> result.error().orElseThrow().message());
                return result;
            } catch (IllegalArgumentException invalid) {
                return DataResult.error(invalid::getMessage);
            }
        }

        @Override public <T> DataResult<T> encode(GolemMarker input, DynamicOps<T> ops, T prefix) {
            return VALUES.encode(input, ops, prefix);
        }
    };

    private static <T> int exactInt(DynamicOps<T> ops, T value, String message) {
        var number = ops.getNumberValue(value).result().orElse(null);
        if (number == null) throw new IllegalArgumentException(message);
        try { return new BigDecimal(number.toString()).intValueExact(); }
        catch (NumberFormatException | ArithmeticException invalid) { throw new IllegalArgumentException(message, invalid); }
    }

    public GolemMarker {
        if (dimension == null || pos == null || face == null) throw new IllegalArgumentException("Incomplete golem marker");
        if (color < ANY || color > LAST_COLOR) throw new IllegalArgumentException("Invalid golem marker colour: " + color);
        pos = pos.immutable();
    }

    /** The ordinary uncoloured marker. */
    public GolemMarker(ResourceLocation dimension, BlockPos pos, Direction face) { this(dimension, pos, face, ANY); }

    public GolemMarker withColor(int color) { return new GolemMarker(dimension, pos, face, color); }

    /**
     * The cell the original clicks (oracle {@code AIUseItem:142-146}).
     *
     * <p>When the marked cell is an empty space the click lands on the cell
     * <b>behind</b> it, that is on the cell opposite the marked side.</p>
     */
    public BlockPos clickPos(boolean emptySpace) {
        return emptySpace ? pos.relative(face.getOpposite()) : pos;
    }

    /** Same dimension, block and face, whatever the colour (the bell edits one marker per spot). */
    public boolean sameSpot(GolemMarker other) {
        return other != null && dimension.equals(other.dimension) && pos.equals(other.pos) && face == other.face;
    }

    /**
     * Original {@code Marker.equalsFuzzy} as used by {@code GolemHelper.getMarkedSides} and the goals: a slot colour of -1
     * accepts a marker of any colour, any other slot colour only a marker of exactly that colour.
     */
    public static boolean accepts(int slotColor, int markerColor) { return slotColor == ANY || slotColor == markerColor; }

    public boolean acceptedBy(int slotColor) { return accepts(slotColor, color); }

    /**
     * ItemGolemBell.changeMarkers: the bell's click on one block face. Without Order the marker toggles (uncoloured). With Order
     * an existing marker cycles -1 -> 0 -> ... -> 15 -> removed, and a sneaking click removes it at once.
     * Returns the edited list, or {@code null} when adding would exceed {@code limit}.
     *
     * <p>The SELECTION is the original's, not a normalised one. Without Order the original searches for the marker with the
     * colour -1 <em>exactly</em> ({@code markers.indexOf(new Marker(x, y, z, dim, side, -1))}), so a coloured marker of an
     * earlier Order golem is left alone and an uncoloured marker is added next to it. With Order the original searches the
     * colours -1 .. 15 in ascending order and takes the LOWEST colour present at that spot
     * ({@code ItemGolemBell.changeMarkers:119-129} of the 4.2.3.5 original).</p>
     */
    public static List<GolemMarker> click(List<GolemMarker> markers, GolemMarker spot, boolean order, boolean sneaking, int limit) {
        var edited = new ArrayList<>(markers);
        int found = -1;
        int color = ANY;
        if (!order) {
            // Original: only the uncoloured marker of that spot is found, so a coloured one survives the click.
            for (int index = 0; index < edited.size(); index++) {
                GolemMarker marker = edited.get(index);
                if (marker.sameSpot(spot) && marker.color() == ANY) { found = index; break; }
            }
        } else {
            // Original: the colours -1 .. 15 are searched in ascending order; the lowest colour present wins.
            for (int candidate = ANY; candidate <= LAST_COLOR && found < 0; candidate++) {
                for (int index = 0; index < edited.size(); index++) {
                    GolemMarker marker = edited.get(index);
                    if (marker.sameSpot(spot) && marker.color() == candidate) { found = index; color = candidate; break; }
                }
            }
        }
        if (found < 0) {
            if (edited.size() >= limit) return null;
            edited.add(spot.withColor(ANY));
            return edited;
        }
        GolemMarker existing = edited.remove(found);
        // The original re-adds the recoloured marker at the END of the list, which is its order for first-match routing.
        if (order && !sneaking && color < LAST_COLOR) edited.add(existing.withColor(color + 1));
        return edited;
    }
}
