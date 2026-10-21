package thaumcraft.common.lib.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.Thaumcraft;

/**
 * The original TC4 4.2.3.5 smelting bonus catalogue.
 *
 * <p>The original registers eighteen bonus entries in {@code ConfigRecipes.initializeSmelting}
 * (oracle {@code thaumcraft/common/config/ConfigRecipes.java:493-510}) and pays them from the ARCANE
 * FURNACE: every item the furnace ejects also pays the bonus of its input (oracle
 * {@code thaumcraft/common/tiles/TileArcaneFurnace.java:237-259}). One bonus unit is rolled without a
 * bellows (one chance in four) and one roll per bellows (44 % each) after that, so smelting a purified
 * ore cluster in the arcane furnace yields two ingots plus a bonus nugget.</p>
 *
 * <p>The corpus lives in {@code data/thaumcraft/tc4/smelting_bonus.json} and keeps the original
 * registration order, the original line of every entry and the reason for every entry the port cannot
 * carry. The item identities are flattened: the original 1.7.10 metadata lives in the item id, so an
 * exact entry names one native item, and the four raw fish of the original {@code Items.fish} wildcard
 * entry become the {@code thaumcraft:original_raw_fish} tag.</p>
 *
 * <p>Nothing here resolves an item at load time: the catalogue is parsed during the mod bootstrap, long
 * before the item registry is frozen. The ids are resolved on the first lookup instead.</p>
 */
public final class SmeltingBonusCatalog {
    /** The bundled corpus of the original registrations. */
    public static final String RESOURCE = "data/thaumcraft/tc4/smelting_bonus.json";

    /** One original registration: either an original ore name, one exact item or one item tag. */
    public record Entry(int sourceLine, String ore, String item, String itemTag, String output) {
        public boolean isOre() { return ore != null; }
    }

    /** One original registration the port cannot carry, with the reason why. */
    public record Skipped(int sourceLine, String ore, String item, String reason) {}

    private static volatile List<Entry> entries = List.of();
    private static volatile List<Skipped> skipped = List.of();
    private static boolean loaded;

    private SmeltingBonusCatalog() {}

    /** Parses the bundled corpus. Safe to call during the mod bootstrap: no item is resolved yet. */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        var stream = SmeltingBonusCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing smelting bonus corpus: " + RESOURCE);
        List<Entry> entries = new ArrayList<>();
        List<Skipped> skipped = new ArrayList<>();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) throw new IllegalStateException("Unsupported smelting bonus schema");
            for (JsonElement element : root.getAsJsonArray("bonus")) {
                JsonObject row = element.getAsJsonObject();
                String ore = row.has("ore") ? row.get("ore").getAsString() : null;
                String item = row.has("item") ? row.get("item").getAsString() : null;
                String tag = row.has("item_tag") ? row.get("item_tag").getAsString() : null;
                String output = row.get("output").getAsString();
                int inputs = (ore == null ? 0 : 1) + (item == null ? 0 : 1) + (tag == null ? 0 : 1);
                if (inputs != 1) throw new IllegalStateException("Invalid smelting bonus entry at line " + row.get("source_line"));
                entries.add(new Entry(row.get("source_line").getAsInt(), ore, item, tag, output));
            }
            if (root.has("skipped")) {
                for (JsonElement element : root.getAsJsonArray("skipped")) {
                    JsonObject row = element.getAsJsonObject();
                    skipped.add(new Skipped(row.get("source_line").getAsInt(),
                            row.has("ore") ? row.get("ore").getAsString() : null,
                            row.has("item") ? row.get("item").getAsString() : null,
                            row.get("reason").getAsString()));
                }
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read the smelting bonus corpus", failure);
        }
        SmeltingBonusCatalog.entries = List.copyOf(entries);
        SmeltingBonusCatalog.skipped = List.copyOf(skipped);
        Thaumcraft.LOG.info("Loaded {} original smelting bonus entries ({} skipped)", entries.size(), skipped.size());
    }

    /** Every original registration the port carries, in the original order. */
    public static List<Entry> entries() {
        if (!loaded) load();
        return entries;
    }

    /** Every original registration the port cannot carry, with the reason why. */
    public static List<Skipped> skipped() {
        if (!loaded) load();
        return skipped;
    }

    /** The native item of an id, or null when the registry does not know it. */
    public static Item resolve(String id) {
        if (id == null) return null;
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    /** The native items an exact entry accepts. */
    public static List<ItemStack> inputs(Entry entry) {
        List<ItemStack> inputs = new ArrayList<>();
        if (entry.ore() != null) return inputs; // An ore name is resolved by the caller's tag lookup.
        if (entry.item() != null) {
            Item item = resolve(entry.item());
            if (item != null) inputs.add(new ItemStack(item));
            return inputs;
        }
        if (entry.itemTag() != null) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.itemTag()));
            var holder = BuiltInRegistries.ITEM.getTag(tag).orElse(null);
            if (holder != null) holder.forEach(item -> inputs.add(new ItemStack(item.value())));
        }
        return inputs;
    }
}
