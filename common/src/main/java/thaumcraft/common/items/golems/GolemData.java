package thaumcraft.common.items.golems;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Configured placer state, never real cargo. Original ItemGolemPlacer:199-239 /
 * InventoryMob:236-276. Wanted stock is separate from six one-count exemplars;
 * material is the item ID and names use CUSTOM_NAME, not an embedded entity.
 * {@code upgrades} is the original placer {@code upgrades} array: slot order is kept,
 * -1 is an empty slot and trailing empties are dropped; the placer item's material
 * decides how many slots may actually be used. {@code guardTargets} is the Order Guard's target mask
 * ({@link GolemGuardTargets}); it is omitted from every serialised form while it has the default value.
 * {@code advanced} marks the advanced golem (original placer tag {@code advanced}): one more upgrade slot, a faster and
 * further-seeing worker. It too is omitted while false, so every earlier stack keeps its exact serialised form.
 * {@code decoration} is the golem's accessory letters in installation order ({@link GolemDecorations}); empty by default
 * and then omitted from every serialised form. */
public record GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                        boolean fillAnyAmount, List<GolemMarker> markers, GolemUseOptions useOptions,
                        List<Integer> upgrades, List<Integer> colors, int guardTargets, boolean advanced, String decoration) {
    public static final int NO_CORE = -1;
    public static final int FILL = 0;
    public static final int EMPTY_CORE = 1;
    public static final int GATHER = 2;
    public static final int HARVEST = 3;
    public static final int GUARD = 4;
    public static final int LIQUID = 5;
    public static final int ALCHEMY = 6;
    public static final int LUMBER = 7;
    public static final int USE = 8;
    public static final int BUTCHER = 9;
    public static final int SORTING = 10;
    public static final int FISHING = 11;
    /** One page of ghost slots; every Fire upgrade of an item-filter core adds another page. */
    public static final int FILTER_SLOTS = 6;
    public static final int MAX_FILTER_SLOTS = FILTER_SLOTS * (1 + GolemUpgrades.MAX_SAME_KIND);
    public static final int MAX_WANTED = 256;
    public static final int MAX_MARKERS = 64;
    public static final GolemData EMPTY = new GolemData(NO_CORE, Collections.nCopies(FILTER_SLOTS, ItemStack.EMPTY));
    private static final Set<String> FIELD_NAMES = Set.of("core", "filters", "wanted_counts", "fill_any_amount", "markers", "use_options", "upgrades", "colors", "guard_targets", "advanced", "decoration");
    public static final int NO_COLOR = GolemMarker.ANY;

    private static final Codec<GolemData> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("core").forGetter(GolemData::core),
            ItemStack.OPTIONAL_CODEC.listOf(FILTER_SLOTS, MAX_FILTER_SLOTS).fieldOf("filters").forGetter(GolemData::filters),
            Codec.intRange(0, MAX_WANTED).listOf(FILTER_SLOTS, MAX_FILTER_SLOTS).optionalFieldOf("wanted_counts")
                    .forGetter((GolemData data) -> Optional.of(data.wantedCounts)),
            Codec.BOOL.optionalFieldOf("fill_any_amount", false).forGetter(GolemData::fillAnyAmount),
            GolemMarker.CODEC.listOf(0, MAX_MARKERS).optionalFieldOf("markers", List.of()).forGetter(GolemData::markers),
            GolemUseOptions.CODEC.optionalFieldOf("use_options", GolemUseOptions.DEFAULT).forGetter(GolemData::useOptions),
            Codec.intRange(GolemUpgrade.EMPTY, GolemUpgrade.ENTROPY.id).listOf(0, GolemUpgrades.MAX_SLOTS)
                    .optionalFieldOf("upgrades", List.of()).forGetter(GolemData::upgrades),
            // Omitted when every slot colour is the default, so every pre-Order stack keeps its exact serialised form.
            Codec.intRange(NO_COLOR, GolemMarker.LAST_COLOR).listOf(GolemData.FILTER_SLOTS, GolemData.MAX_FILTER_SLOTS)
                    .optionalFieldOf("colors", List.of()).forGetter((GolemData data) -> data.allColorsDefault() ? List.of() : data.colors),
            // Omitted at the default (hostiles only), so a plain Guard and every earlier stack keep their serialised form.
            Codec.intRange(0, GolemGuardTargets.ALL).optionalFieldOf("guard_targets", GolemGuardTargets.DEFAULT).forGetter(GolemData::guardTargets),
            // Omitted while false, like the other optional configuration.
            Codec.BOOL.optionalFieldOf("advanced", false).forGetter(GolemData::advanced),
            // Omitted while empty; a non-empty value must be a sequence the installation rules could have produced.
            Codec.STRING.optionalFieldOf("decoration", "").forGetter(GolemData::decoration)
    ).apply(instance, (core, filters, wantedCounts, anyAmount, markers, useOptions, upgrades, colors, guardTargets, advanced, decoration) ->
            new GolemData(core, filters, wantedCounts.orElseGet(() -> defaultWantedCounts(core, filters)), anyAmount, markers, useOptions, upgrades, colors, guardTargets, advanced, decoration)));

    /** Native stack codecs carry every component, including explicit EMPTY slots.
     * Reject unknown future fields and partial lists instead of decoding a smaller,
     * apparently plain golem after dropping its unsupported configuration. */
    public static final Codec<GolemData> CODEC = new Codec<>() {
        @Override public <T> DataResult<Pair<GolemData, T>> decode(DynamicOps<T> ops, T input) {
            var decodedMap = ops.getMap(input);
            if (decodedMap.error().isPresent()) return DataResult.error(() -> "Expected golem configuration object");
            var map = decodedMap.result().orElseThrow();
            var keys = new HashSet<String>();
            try (var entries = map.entries()) {
                var iterator = entries.iterator();
                while (iterator.hasNext()) {
                    var key = ops.getStringValue(iterator.next().getFirst()).result().orElse(null);
                    if (key == null || !FIELD_NAMES.contains(key) || !keys.add(key)) {
                        return DataResult.error(() -> "Unsupported or duplicate golem configuration field");
                    }
                }
            }
            if (!keys.contains("core") || !keys.contains("filters")) {
                return DataResult.error(() -> "Golem configuration requires core and six filters");
            }
            try {
                // Validate supplied numbers before native INT can truncate them.
                if (!isSupportedCore(exactInt(ops, map.get("core")))) throw new IllegalArgumentException("Unsupported golem core");
                if (keys.contains("wanted_counts")) {
                    var stream = ops.getStream(map.get("wanted_counts")).result()
                            .orElseThrow(() -> new IllegalArgumentException("Expected six wanted counts"));
                    try (stream) {
                        var counts = stream.limit(MAX_FILTER_SLOTS + 1L).toList();
                        if (!pagedSize(counts.size())) throw new IllegalArgumentException("Expected six, twelve or eighteen wanted counts");
                        for (var count : counts) {
                            int value = exactInt(ops, count);
                            if (value < 0 || value > MAX_WANTED) throw new IllegalArgumentException("Invalid wanted count");
                        }
                    }
                }
                // A malformed OPTIONAL field is not permission to use its default.
                if (keys.contains("fill_any_amount") && Codec.BOOL.parse(ops, map.get("fill_any_amount")).error().isPresent()) {
                    throw new IllegalArgumentException("Invalid Fill mode");
                }
                if (keys.contains("markers") && GolemMarker.CODEC.listOf(0, MAX_MARKERS)
                        .parse(ops, map.get("markers")).error().isPresent()) {
                    throw new IllegalArgumentException("Invalid golem markers");
                }
                if (keys.contains("use_options") && GolemUseOptions.CODEC.parse(ops, map.get("use_options")).error().isPresent()) {
                    throw new IllegalArgumentException("Invalid golem Use options");
                }
                if (keys.contains("upgrades")) {
                    // Exact integers first (native INT would truncate), then only ids this port can honour.
                    var stream = ops.getStream(map.get("upgrades")).result()
                            .orElseThrow(() -> new IllegalArgumentException("Expected a golem upgrade list"));
                    try (stream) {
                        var slots = stream.limit(GolemUpgrades.MAX_SLOTS + 1L).toList();
                        if (slots.size() > GolemUpgrades.MAX_SLOTS) throw new IllegalArgumentException("Too many golem upgrade slots");
                        for (var slot : slots) {
                            if (!GolemUpgrades.validSlot(exactInt(ops, slot))) throw new IllegalArgumentException("Unsupported golem upgrade");
                        }
                    }
                }
                if (keys.contains("colors")) {
                    var stream = ops.getStream(map.get("colors")).result()
                            .orElseThrow(() -> new IllegalArgumentException("Expected a slot colour list"));
                    try (stream) {
                        var entries = stream.limit(MAX_FILTER_SLOTS + 1L).toList();
                        if (!pagedSize(entries.size())) throw new IllegalArgumentException("Expected six, twelve or eighteen slot colours");
                        for (var entry : entries) {
                            int color = exactInt(ops, entry);
                            if (color < NO_COLOR || color > GolemMarker.LAST_COLOR) throw new IllegalArgumentException("Invalid slot colour");
                        }
                    }
                }
                if (keys.contains("guard_targets")) {
                    int mask = exactInt(ops, map.get("guard_targets"));
                    if (!GolemGuardTargets.valid(mask)) throw new IllegalArgumentException("Invalid Guard target mask");
                }
                if (keys.contains("advanced") && Codec.BOOL.parse(ops, map.get("advanced")).error().isPresent()) {
                    throw new IllegalArgumentException("Invalid advanced golem flag");
                }
                if (keys.contains("decoration") && Codec.STRING.parse(ops, map.get("decoration")).error().isPresent()) {
                    throw new IllegalArgumentException("Invalid golem decoration");
                }
                var result = FIELDS.decode(ops, input);
                if (result.error().isPresent()) return DataResult.error(() -> result.error().orElseThrow().message());
                return result;
            } catch (IllegalArgumentException invalid) {
                return DataResult.error(invalid::getMessage);
            }
        }

        @Override public <T> DataResult<T> encode(GolemData input, DynamicOps<T> ops, T prefix) {
            return FIELDS.encode(input, ops, prefix);
        }
    };

    // Fixed six native stacks/counts; marker allocation is bounded before reading.
    public static final StreamCodec<RegistryFriendlyByteBuf, GolemData> STREAM_CODEC = new StreamCodec<>() {
        @Override public GolemData decode(RegistryFriendlyByteBuf buffer) {
            int core = ByteBufCodecs.VAR_INT.decode(buffer);
            if (!isSupportedCore(core)) throw new DecoderException("Unsupported golem core: " + core);
            int slots = ByteBufCodecs.VAR_INT.decode(buffer);
            if (!pagedSize(slots)) throw new DecoderException("Invalid golem ghost slot count");
            var filters = new ArrayList<ItemStack>(slots);
            for (int slot = 0; slot < slots; slot++) filters.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            var wantedCounts = new ArrayList<Integer>(slots);
            for (int slot = 0; slot < slots; slot++) wantedCounts.add(ByteBufCodecs.VAR_INT.decode(buffer));
            boolean anyAmount = buffer.readBoolean();
            int markerCount = ByteBufCodecs.VAR_INT.decode(buffer);
            if (markerCount < 0 || markerCount > MAX_MARKERS) throw new DecoderException("Invalid golem marker count");
            var markers = new ArrayList<GolemMarker>(markerCount);
            try {
                for (int marker = 0; marker < markerCount; marker++) {
                    var dimension = buffer.readResourceLocation();
                    // Three native integers preserve positions without packed-long truncation.
                    var pos = new BlockPos(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
                    int face = buffer.readUnsignedByte();
                    if (face > 5) throw new IllegalArgumentException("Invalid golem marker face");
                    markers.add(new GolemMarker(dimension, pos, Direction.from3DDataValue(face), buffer.readByte()));
                }
                GolemUseOptions useOptions = GolemUseOptions.STREAM_CODEC.decode(buffer);
                int upgradeCount = ByteBufCodecs.VAR_INT.decode(buffer);
                if (upgradeCount < 0 || upgradeCount > GolemUpgrades.MAX_SLOTS) throw new DecoderException("Invalid golem upgrade count");
                var upgrades = new ArrayList<Integer>(upgradeCount);
                for (int slot = 0; slot < upgradeCount; slot++) upgrades.add((int) buffer.readByte());
                int colorCount = ByteBufCodecs.VAR_INT.decode(buffer);
                if (!pagedSize(colorCount)) throw new DecoderException("Invalid golem slot colour count");
                var colors = new ArrayList<Integer>(colorCount);
                for (int slot = 0; slot < colorCount; slot++) colors.add((int) buffer.readByte());
                // One trailing byte: the Guard mask in the low bits, the advanced flag in bit 4 and, in bit 5, a following
                // decoration string. A plain golem therefore ends with exactly the byte it always did.
                int tail = buffer.readUnsignedByte();
                int guardTargets = tail & GolemGuardTargets.ALL;
                if ((tail & ~(GolemGuardTargets.ALL | ADVANCED_BIT | DECORATION_BIT)) != 0) throw new DecoderException("Invalid Guard target mask");
                String decoration = (tail & DECORATION_BIT) != 0 ? buffer.readUtf(MAX_DECORATION_LENGTH) : "";
                if ((tail & DECORATION_BIT) != 0 && decoration.isEmpty()) throw new DecoderException("Empty decoration marker");
                return new GolemData(core, filters, wantedCounts, anyAmount, markers, useOptions, upgrades, colors, guardTargets,
                        (tail & ADVANCED_BIT) != 0, decoration);
            } catch (IllegalArgumentException invalid) {
                throw new DecoderException("Invalid golem configuration", invalid);
            }
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, GolemData data) {
            ByteBufCodecs.VAR_INT.encode(buffer, data.core);
            ByteBufCodecs.VAR_INT.encode(buffer, data.filters.size());
            for (ItemStack filter : data.filters) ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, filter);
            for (int count : data.wantedCounts) ByteBufCodecs.VAR_INT.encode(buffer, count);
            buffer.writeBoolean(data.fillAnyAmount);
            ByteBufCodecs.VAR_INT.encode(buffer, data.markers.size());
            for (GolemMarker marker : data.markers) {
                buffer.writeResourceLocation(marker.dimension());
                buffer.writeVarInt(marker.pos().getX());
                buffer.writeVarInt(marker.pos().getY());
                buffer.writeVarInt(marker.pos().getZ());
                buffer.writeByte(marker.face().get3DDataValue());
                buffer.writeByte(marker.color());
            }
            GolemUseOptions.STREAM_CODEC.encode(buffer, data.useOptions);
            ByteBufCodecs.VAR_INT.encode(buffer, data.upgrades.size());
            for (int upgrade : data.upgrades) buffer.writeByte(upgrade);
            ByteBufCodecs.VAR_INT.encode(buffer, data.colors.size());
            for (int color : data.colors) buffer.writeByte(color);
            buffer.writeByte(data.guardTargets | (data.advanced ? ADVANCED_BIT : 0) | (data.decoration.isEmpty() ? 0 : DECORATION_BIT));
            if (!data.decoration.isEmpty()) buffer.writeUtf(data.decoration, MAX_DECORATION_LENGTH);
        }
    };

    /** The advanced flag shares the stream codec's trailing byte with the Guard mask (bits 0..3). */
    private static final int ADVANCED_BIT = 0x10;
    /** Bit 5 of that byte: a decoration string follows. */
    private static final int DECORATION_BIT = 0x20;
    private static final int MAX_DECORATION_LENGTH = GolemDecorations.SUPPORTED.length();

    /** Existing callers/default components retain six whitelist-only exemplars. */
    public GolemData(int core, List<ItemStack> filters) {
        this(core, filters, defaultWantedCounts(core, filters), false, List.of());
    }

    public GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                     boolean fillAnyAmount, List<GolemMarker> markers) {
        this(core, filters, wantedCounts, fillAnyAmount, markers, GolemUseOptions.DEFAULT);
    }

    public GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                     boolean fillAnyAmount, List<GolemMarker> markers, GolemUseOptions useOptions) {
        this(core, filters, wantedCounts, fillAnyAmount, markers, useOptions, List.of());
    }

    /** Every caller before the Order slot colours: all slots keep the default colour (any marker). */
    public GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                     boolean fillAnyAmount, List<GolemMarker> markers, GolemUseOptions useOptions,
                     List<Integer> upgrades) {
        this(core, filters, wantedCounts, fillAnyAmount, markers, useOptions, upgrades, List.of());
    }

    /** Every caller before the Guard target switches: a Guard attacks hostile monsters only. */
    public GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                     boolean fillAnyAmount, List<GolemMarker> markers, GolemUseOptions useOptions,
                     List<Integer> upgrades, List<Integer> colors) {
        this(core, filters, wantedCounts, fillAnyAmount, markers, useOptions, upgrades, colors, GolemGuardTargets.DEFAULT);
    }

    /** Every caller before the advanced golem: an ordinary golem. */
    public GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                     boolean fillAnyAmount, List<GolemMarker> markers, GolemUseOptions useOptions,
                     List<Integer> upgrades, List<Integer> colors, int guardTargets) {
        this(core, filters, wantedCounts, fillAnyAmount, markers, useOptions, upgrades, colors, guardTargets, false);
    }

    /** Every caller before the accessories: an undecorated golem. */
    public GolemData(int core, List<ItemStack> filters, List<Integer> wantedCounts,
                     boolean fillAnyAmount, List<GolemMarker> markers, GolemUseOptions useOptions,
                     List<Integer> upgrades, List<Integer> colors, int guardTargets, boolean advanced) {
        this(core, filters, wantedCounts, fillAnyAmount, markers, useOptions, upgrades, colors, guardTargets, advanced, "");
    }

    /** The same configuration with another accessory string; it must be one the installation rules could produce. */
    public GolemData withDecoration(String value) {
        return value.equals(decoration) ? this : new GolemData(core, filters, wantedCounts, fillAnyAmount, markers, useOptions,
                upgrades, colors, guardTargets, advanced, value);
    }

    /** The same configuration with the advanced flag set or cleared; everything else is carried over verbatim. */
    public GolemData withAdvanced(boolean value) {
        return value == advanced ? this : new GolemData(core, filters, wantedCounts, fillAnyAmount, markers, useOptions,
                upgrades, colors, guardTargets, value, decoration);
    }

    /** Slot colours exist only where the colour is a routing rule: Fill, Empty and Use, and only with an Order upgrade. */
    public static boolean hasSlotColors(int core) { return core == FILL || core == EMPTY_CORE || core == USE; }

    public static int orderCount(List<Integer> upgrades) {
        int order = 0;
        if (upgrades != null) for (Integer upgrade : upgrades) if (upgrade != null && upgrade == GolemUpgrade.ORDER.id) order++;
        return order;
    }

    public boolean allColorsDefault() {
        for (int color : colors) if (color != NO_COLOR) return false;
        return true;
    }

    /** Item-filter cores (Fill, Empty, Gather, Use): the only ones whose ghost slots Fire enlarges. */
    public static boolean hasItemGhosts(int core) { return core == FILL || core == EMPTY_CORE || core == GATHER || core == USE; }

    /**
     * Ghost slots: {@code 6 + 6n} for an item-filter core with n Fire upgrades (original EntityGolemBase:198-224). Decanting
     * keeps one meaningful slot (water is its only fluid, so the original {@code 1 + n} extra slots would be duplicates) and the
     * remaining cores have no ghosts; both stay at the one six-slot page.
     */
    public static int slotCount(int core, int fireUpgrades) {
        return hasItemGhosts(core) ? FILTER_SLOTS * (1 + Math.max(0, Math.min(GolemUpgrades.MAX_SAME_KIND, fireUpgrades))) : FILTER_SLOTS;
    }

    public static int fireCount(List<Integer> upgrades) {
        int fire = 0;
        if (upgrades != null) for (Integer upgrade : upgrades) if (upgrade != null && upgrade == GolemUpgrade.FIRE.id) fire++;
        return fire;
    }

    private static boolean pagedSize(int size) {
        return size >= FILTER_SLOTS && size <= MAX_FILTER_SLOTS && size % FILTER_SLOTS == 0;
    }

    public static boolean isSupportedCore(int core) {
        return core == NO_CORE || core == FILL || core == EMPTY_CORE || core == GATHER || core == HARVEST || core == LUMBER || core == GUARD || core == LIQUID || core == ALCHEMY || core == USE || core == BUTCHER || core == SORTING || core == FISHING;
    }

    /** Water-only Decanting uses a single pristine filled-container exemplar,
     * never the container's count/components as owned fluid or wanted stock. */
    public static boolean isSupportedWaterFilter(ItemStack exemplar) {
        return exemplar != null && (exemplar.isEmpty() || ItemStack.matches(exemplar, new ItemStack(Items.WATER_BUCKET)));
    }

    private static List<Integer> defaultWantedCounts(int core, List<ItemStack> filters) {
        if (filters == null || !pagedSize(filters.size())) throw new IllegalArgumentException("Expected six, twelve or eighteen golem ghost filters");
        var counts = new ArrayList<Integer>(filters.size());
        for (ItemStack filter : filters) {
            if (filter == null) throw new IllegalArgumentException("Null golem ghost filter");
            counts.add(filter.isEmpty() || core == LIQUID ? 0 : 1);
        }
        return counts;
    }

    private static <T> int exactInt(DynamicOps<T> ops, T input) {
        var number = ops.getNumberValue(input).result().orElse(null);
        if (number == null) throw new IllegalArgumentException("Expected integer golem configuration value");
        try { return new BigDecimal(number.toString()).intValueExact(); }
        catch (NumberFormatException | ArithmeticException invalid) { throw new IllegalArgumentException("Invalid integer golem configuration value", invalid); }
    }

    public GolemData {
        if (!isSupportedCore(core)) throw new IllegalArgumentException("Unsupported golem core: " + core);
        if (useOptions == null || (core != USE && !useOptions.isDefault())) {
            throw new IllegalArgumentException("Non-Use cores require default golem Use options");
        }
        if (upgrades == null || upgrades.size() > GolemUpgrades.MAX_SLOTS) {
            throw new IllegalArgumentException("Expected at most " + GolemUpgrades.MAX_SLOTS + " golem upgrade slots");
        }
        for (Integer upgrade : upgrades) {
            if (upgrade == null || !GolemUpgrades.validSlot(upgrade)) throw new IllegalArgumentException("Unsupported golem upgrade: " + upgrade);
        }
        // Ghost slots follow the Fire upgrades. A plain six-slot list (every pre-Fire placer and caller) is padded with
        // empty slots; any other size must match the core and its Fire count exactly, never silently truncate.
        int expectedSlots = slotCount(core, fireCount(upgrades));
        if (filters == null || wantedCounts == null) throw new IllegalArgumentException("Expected golem ghost filters and wanted counts");
        if (filters.size() == FILTER_SLOTS && wantedCounts.size() == FILTER_SLOTS && expectedSlots > FILTER_SLOTS) {
            var paddedFilters = new ArrayList<>(filters);
            var paddedCounts = new ArrayList<>(wantedCounts);
            while (paddedFilters.size() < expectedSlots) { paddedFilters.add(ItemStack.EMPTY); paddedCounts.add(0); }
            filters = paddedFilters;
            wantedCounts = paddedCounts;
        }
        if (filters.size() != expectedSlots) throw new IllegalArgumentException("Expected " + expectedSlots + " golem ghost filters");
        if (wantedCounts.size() != expectedSlots) throw new IllegalArgumentException("Expected " + expectedSlots + " wanted counts");
        // Slot colours: absent means all default; a short plain page is padded; any other length is refused.
        var normalizedColors = new ArrayList<Integer>(expectedSlots);
        if (colors == null || colors.isEmpty()) {
            for (int slot = 0; slot < expectedSlots; slot++) normalizedColors.add(NO_COLOR);
        } else {
            if (colors.size() != expectedSlots && !(colors.size() == FILTER_SLOTS && expectedSlots > FILTER_SLOTS)) {
                throw new IllegalArgumentException("Expected " + expectedSlots + " slot colours");
            }
            normalizedColors.addAll(colors);
            while (normalizedColors.size() < expectedSlots) normalizedColors.add(NO_COLOR);
        }
        boolean coloredSlot = false;
        for (Integer color : normalizedColors) {
            if (color == null || color < NO_COLOR || color > GolemMarker.LAST_COLOR) throw new IllegalArgumentException("Invalid slot colour: " + color);
            coloredSlot |= color != NO_COLOR;
        }
        if (coloredSlot && (!hasSlotColors(core) || orderCount(upgrades) == 0)) {
            throw new IllegalArgumentException("Slot colours need an Order upgrade on a Fill, Empty or Use core");
        }
        colors = List.copyOf(normalizedColors);
        // Target switches are Order's, on a Guard only: a non-default mask elsewhere is data this port cannot honour.
        if (!GolemGuardTargets.acceptable(guardTargets, core, orderCount(upgrades))) {
            throw new IllegalArgumentException("Guard target switches need an Order upgrade on a Guard core and a mask of 0..15");
        }
        if (!GolemDecorations.valid(decoration)) throw new IllegalArgumentException("Invalid golem decoration: " + decoration);
        var copy = new ArrayList<ItemStack>(expectedSlots);
        for (int slot = 0; slot < expectedSlots; slot++) {
            ItemStack filter = filters.get(slot);
            if (filter == null || !filter.isEmpty() && filter.getCount() != 1) {
                throw new IllegalArgumentException("A golem ghost filter must be EMPTY or one item");
            }
            Integer wanted = wantedCounts.get(slot);
            if (wanted == null || (filter.isEmpty() || core == LIQUID ? wanted != 0 : wanted < 1 || wanted > MAX_WANTED)) {
                throw new IllegalArgumentException("Wanted count must be 0 for fluid/EMPTY or 1..256 for a populated item filter");
            }
            if (core == LIQUID && (slot == 0 ? !isSupportedWaterFilter(filter) : !filter.isEmpty())) {
                throw new IllegalArgumentException("Water-only Decanting has only one pristine water-bucket ghost filter");
            }
            if (core == USE && wanted != (filter.isEmpty() ? 0 : 1)) {
                throw new IllegalArgumentException("Use item filters are whitelist-only, with wanted count 0/1");
            }
            copy.add(filter.copy());
        }
        if (core == LIQUID && fillAnyAmount) throw new IllegalArgumentException("Decanting has no Fill mode");
        if (core == USE && fillAnyAmount) throw new IllegalArgumentException("Use has no Fill mode");
        if ((core == SORTING || core == HARVEST || core == LUMBER || core == GUARD || core == FISHING || core == ALCHEMY || core == BUTCHER) && (fillAnyAmount || copy.stream().anyMatch(stack -> !stack.isEmpty())
                || wantedCounts.stream().anyMatch(count -> count != 0))) {
            throw new IllegalArgumentException("This ordinary core has no ghost inventory, wanted quantities or Fill mode");
        }
        if (markers == null || markers.size() > MAX_MARKERS) throw new IllegalArgumentException("Expected at most 64 golem markers");
        var unique = new HashSet<GolemMarker>();
        for (GolemMarker marker : markers) {
            if (marker == null || !unique.add(marker)) throw new IllegalArgumentException("Null or duplicate golem marker");
            if (marker.color() != NO_COLOR && orderCount(upgrades) == 0) throw new IllegalArgumentException("Coloured markers need an Order upgrade");
        }
        filters = List.copyOf(copy);
        wantedCounts = List.copyOf(wantedCounts);
        markers = List.copyOf(markers);
        upgrades = GolemUpgrades.canonical(upgrades);
    }

    /** Neither the supplied list nor its mutable ItemStacks escape into live state. */
    @Override public List<ItemStack> filters() {
        return filters.stream().map(ItemStack::copy).toList();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GolemData data) || core != data.core || fillAnyAmount != data.fillAnyAmount
                || !wantedCounts.equals(data.wantedCounts) || !markers.equals(data.markers)
                || !useOptions.equals(data.useOptions) || !upgrades.equals(data.upgrades) || !colors.equals(data.colors)
                || guardTargets != data.guardTargets || advanced != data.advanced || !decoration.equals(data.decoration)
                || filters.size() != data.filters.size()) return false;
        for (int slot = 0; slot < filters.size(); slot++) {
            if (!ItemStack.matches(filters.get(slot), data.filters.get(slot))) return false;
        }
        return true;
    }

    @Override public int hashCode() {
        int hash = core;
        for (ItemStack filter : filters) {
            int stackHash = filter.isEmpty() ? 0 : 31 * ItemStack.hashItemAndComponents(filter) + filter.getCount();
            hash = 31 * hash + stackHash;
        }
        hash = 31 * hash + wantedCounts.hashCode();
        hash = 31 * hash + Boolean.hashCode(fillAnyAmount);
        hash = 31 * hash + markers.hashCode();
        hash = 31 * hash + useOptions.hashCode();
        hash = 31 * hash + upgrades.hashCode();
        hash = 31 * hash + colors.hashCode();
        hash = 31 * hash + guardTargets;
        hash = 31 * hash + Boolean.hashCode(advanced);
        return 31 * hash + decoration.hashCode();
    }
}
