package thaumcraft.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The original TC4 reflective item/block lookup.
 *
 * <p>Forced 1.21.1 change: the original reflected over {@code thaumcraft.common.config.ConfigItems} and
 * {@code ConfigBlocks}. The native port has no such classes and 1.21.1 has no metadata, so an original field
 * name and metadata ({@code getItem("itemResource", 2)}) are resolved through the table
 * {@code data/thaumcraft/tc4/legacy_ids.json} (written by {@code scripts/porting/extract_legacy_ids.py},
 * reference {@code docs/api/LEGACY_IDS.md}). Any other name is resolved through the live registries (a plain
 * name means {@code thaumcraft:<name>}) and only then through the original reflective field names for an
 * add-on that still publishes them.</p>
 *
 * <p>The table is read from the classpath on the first legacy lookup and never needs a running game; the
 * native objects are taken from the registries at the time of each call.</p>
 */
public class ItemApi {
    private static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");
    private static final String ITEM_CLASS = "thaumcraft.common.config.ConfigItems";
    private static final String BLOCK_CLASS = "thaumcraft.common.config.ConfigBlocks";
    /** {@code OreDictionary.WILDCARD_VALUE}: every metadata of an original field. */
    public static final int WILDCARD = 32767;

    private ItemApi() {}

    /** Returns a fresh named stack, or null for null/empty/malformed/missing names (failed lookups log).
     * An original {@code ConfigItems} field name selects its 1.21.1 item by the original metadata; a field
     * with a single item keeps the metadata as the wear of a damageable stack, as the original did.
     * Any other name is a namespaced item id, where the metadata only applies wear to a damageable stack. */
    public static ItemStack getItem(String itemString, int meta) {
        if (itemString == null || itemString.isEmpty()) return null;
        if (Legacy.isField(itemString, Legacy.ITEM)) return legacyStack(itemString, meta, Legacy.ITEM, "item");
        ResourceLocation id = lenientId(itemString);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return stack(BuiltInRegistries.ITEM.get(id), meta);
        Object reflected = reflect(ITEM_CLASS, itemString);
        if (reflected instanceof Item reflectedItem) return stack(reflectedItem, meta);
        if (reflected instanceof ItemStack stack) return stack.copy();
        LOG.warn("[Thaumcraft] Could not retrieve item identified by: {}", itemString);
        return null;
    }

    /** Returns a fresh named block-item stack, or null for null/empty/malformed/missing names (failed lookups log).
     * An original {@code ConfigBlocks} field name selects the item form of the original metadata; when the
     * metadata selects a state that the block's own item places, the stack carries it as
     * {@code minecraft:block_state}. Any other name is a namespaced block id. */
    public static ItemStack getBlock(String itemString, int meta) {
        if (itemString == null || itemString.isEmpty()) return null;
        if (Legacy.isField(itemString, Legacy.BLOCK)) return legacyStack(itemString, meta, Legacy.BLOCK, "block");
        ResourceLocation id = lenientId(itemString);
        if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) return stack(BuiltInRegistries.BLOCK.get(id), meta);
        Object reflected = reflect(BLOCK_CLASS, itemString);
        if (reflected instanceof Block reflectedBlock) return stack(reflectedBlock, meta);
        if (reflected instanceof ItemStack stack) return stack.copy();
        LOG.warn("[Thaumcraft] Could not retrieve block identified by: {}", itemString);
        return null;
    }

    /**
     * The 1.21.1 item id of an original field and metadata: the item of a {@code ConfigItems} field, the item
     * form of a {@code ConfigBlocks} field. Null for an unknown name or metadata, for a variant with no 1.21.1
     * counterpart and for the wildcard of a field with several items ({@link #legacyItemIds}).
     */
    public static ResourceLocation legacyItemId(String field, int meta) {
        Legacy.Row row = Legacy.row(field, meta);
        return row == null ? null : row.itemForm();
    }

    /** The 1.21.1 block id of an original {@code ConfigBlocks} field and metadata, or null. */
    public static ResourceLocation legacyBlockId(String field, int meta) {
        Legacy.Row row = Legacy.row(field, meta);
        return row == null || !Legacy.BLOCK.equals(row.kind()) ? null : row.id();
    }

    /**
     * Every 1.21.1 item id an original field and metadata stands for: one id, or for {@link #WILDCARD} every
     * distinct item of the field, in metadata order. Empty for an unknown name.
     */
    public static List<ResourceLocation> legacyItemIds(String field, int meta) {
        if (meta != WILDCARD) {
            ResourceLocation id = legacyItemId(field, meta);
            return id == null ? List.of() : List.of(id);
        }
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (Legacy.Row row : Legacy.rows(field)) {
            if (row.meta() != WILDCARD && row.itemForm() != null) ids.add(row.itemForm());
        }
        return List.copyOf(ids);
    }

    /**
     * The block state an original {@code ConfigBlocks} field and metadata selected, from the live block
     * registry, or null for an unknown name or metadata, a variant with no 1.21.1 block, a block that is not
     * registered (yet) and the wildcard of a field with several blocks.
     */
    public static BlockState legacyBlockState(String field, int meta) {
        Legacy.Row row = Legacy.row(field, meta);
        if (row == null || !Legacy.BLOCK.equals(row.kind()) || row.id() == null) return null;
        if (!BuiltInRegistries.BLOCK.containsKey(row.id())) return null;
        BlockState state = BuiltInRegistries.BLOCK.get(row.id()).defaultBlockState();
        for (Map.Entry<String, String> entry : row.state().entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            state = property == null ? null : with(state, property, entry.getValue());
            if (state == null) {
                LOG.warn("[Thaumcraft] {}:{} names the state {}={} that {} does not have", field, meta,
                        entry.getKey(), entry.getValue(), row.id());
                return null;
            }
        }
        return state;
    }

    /** True when the name is an original {@code ConfigItems} or {@code ConfigBlocks} field. */
    public static boolean isLegacyName(String name) {
        return Legacy.isField(name, Legacy.ITEM) || Legacy.isField(name, Legacy.BLOCK);
    }

    /** The original fields of the table, {@code ConfigItems} first, in declaration order. */
    public static Set<String> legacyNames() {
        return Legacy.table().fields.keySet();
    }

    /** The metadata values the table lists for an original field, ascending, the wildcard last. */
    public static List<Integer> legacyMetadata(String field) {
        List<Integer> metas = new ArrayList<>();
        for (Legacy.Row row : Legacy.rows(field)) metas.add(row.meta());
        return metas;
    }

    private static ItemStack legacyStack(String field, int meta, String kind, String what) {
        Legacy.Row row = Legacy.row(field, meta);
        ResourceLocation itemId = row == null ? null : row.itemForm();
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            String reason = row == null ? "no such original metadata"
                    : itemId == null ? Optional.ofNullable(row.note()).orElse("no 1.21.1 item")
                    : itemId + " is not registered";
            LOG.warn("[Thaumcraft] Could not retrieve {} identified by: {} (metadata {}: {})", what, field, meta, reason);
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        // A field with a single item keeps the original damage semantics of new ItemStack(item, 1, meta).
        ItemStack stack = row.meta() == meta ? new ItemStack(item) : stack(item, meta);
        // The block's own item places its default state; another state of the original metadata (a top slab,
        // a cosmetic variant, an active fetter) travels as minecraft:block_state, which BlockItem applies on
        // placement. A variant item of its own (the obsidian tile, the research table) already places it.
        if (Legacy.BLOCK.equals(kind) && !row.state().isEmpty() && item instanceof BlockItem blockItem
                && blockItem.getBlock().asItem() == item
                && row.id() != null && row.id().equals(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()))) {
            BlockState state = legacyBlockState(field, row.meta());
            if (state != null && state != blockItem.getBlock().defaultBlockState()) {
                stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(row.state()));
            }
        }
        return stack;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(null);
    }

    /**
     * The original metadata is the native damage value.
     *
     * <p>Forced 1.21.1 change: 1.21.1 has no item metadata; the original metadata only survives on a
     * damageable item, and it is the damage value there.</p>
     */
    private static ItemStack stack(ItemLike item, int meta) {
        ItemStack stack = new ItemStack(item, 1);
        if (meta != 0 && stack.isDamageableItem()) stack.setDamageValue(Math.min(meta, stack.getMaxDamage() - 1));
        return stack;
    }

    /** Null/empty or malformed namespaced IDs return null; malformed plain paths throw ResourceLocationException. */
    public static ItemStack getBlockItem(String itemString) {
        if (itemString == null || itemString.isEmpty()) return null;
        ResourceLocation id = id(itemString);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
        if (block == null) return null;
        Item item = block.asItem();
        return item instanceof BlockItem ? new ItemStack(item) : null;
    }

    private static ResourceLocation id(String name) {
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
    }

    /** {@link #id} that answers null for a malformed plain path, as the original lookup never threw. */
    private static ResourceLocation lenientId(String name) {
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.tryBuild("thaumcraft", name);
    }

    private static Object reflect(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException | RuntimeException absent) {
            return null;
        }
    }

    /** The original name table, read once on first use (initialisation-on-demand holder). */
    static final class Legacy {
        static final String ITEM = "item";
        static final String BLOCK = "block";
        static final String RESOURCE = "data/thaumcraft/tc4/legacy_ids.json";

        /** One original field and metadata. {@code item} is the item form of a block row. */
        record Row(String kind, String field, int meta, ResourceLocation id, Map<String, String> state,
                   ResourceLocation item, String note) {
            ResourceLocation itemForm() { return ITEM.equals(kind) ? id : item; }
        }

        static final class Table {
            final Map<String, String> kinds = new LinkedHashMap<>();
            final Map<String, Map<Integer, Row>> fields = new LinkedHashMap<>();
        }

        private static final class Holder {
            static final Table TABLE = load();
        }

        private Legacy() {}

        static Table table() { return Holder.TABLE; }

        static boolean isField(String name, String kind) {
            return name != null && kind.equals(table().kinds.get(name));
        }

        static List<Row> rows(String field) {
            Map<Integer, Row> rows = field == null ? null : table().fields.get(field);
            return rows == null ? List.of() : List.copyOf(rows.values());
        }

        /**
         * The row of a field and metadata. A field with exactly one row besides the wildcard answers every
         * metadata with it: the original item had no subtypes, the metadata was only its damage.
         */
        static Row row(String field, int meta) {
            Map<Integer, Row> rows = field == null ? null : table().fields.get(field);
            if (rows == null) return null;
            Row row = rows.get(meta);
            if (row != null) return row;
            Row only = null;
            for (Row candidate : rows.values()) {
                if (candidate.meta() == WILDCARD) continue;
                if (only != null) return null;
                only = candidate;
            }
            return only != null && only.meta() == 0 ? only : null;
        }

        private static Table load() {
            Table table = new Table();
            try (InputStream stream = ItemApi.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                if (stream == null) {
                    LOG.error("[Thaumcraft] Missing original name table {}", RESOURCE);
                    return table;
                }
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported schema");
                for (JsonElement element : root.getAsJsonArray("rows")) {
                    JsonObject json = element.getAsJsonObject();
                    String kind = json.get("kind").getAsString();
                    String field = json.get("field").getAsString();
                    Map<String, String> state = new LinkedHashMap<>();
                    if (json.has("state")) {
                        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("state").entrySet()) {
                            state.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                    Row row = new Row(kind, field, json.get("meta").getAsInt(), location(json, "id"),
                            Collections.unmodifiableMap(state), location(json, "item"),
                            json.has("note") ? json.get("note").getAsString() : null);
                    table.kinds.put(field, kind);
                    table.fields.computeIfAbsent(field, key -> new LinkedHashMap<>()).put(row.meta(), row);
                }
            } catch (Exception failure) {
                // A broken table must not take the add-on lookup or the game down: legacy names then miss.
                LOG.error("[Thaumcraft] Could not read the original name table {}", RESOURCE, failure);
                table.kinds.clear();
                table.fields.clear();
            }
            return table;
        }

        private static ResourceLocation location(JsonObject json, String key) {
            JsonElement value = json.get(key);
            return value == null || value.isJsonNull() ? null : ResourceLocation.parse(value.getAsString());
        }
    }
}
