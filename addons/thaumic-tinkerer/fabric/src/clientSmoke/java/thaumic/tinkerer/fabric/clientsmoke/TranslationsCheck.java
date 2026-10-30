package thaumic.tinkerer.fabric.clientsmoke;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.fabric.clientsmoke.Translations.Needed;

/**
 * The names the registries give: item and block names (the key the game actually asks for, not the one the registry path
 * suggests), the creative tab, mob effects, entity types and the enchantment descriptions of the data files. Test-only code
 * of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Enchantments are data, not registry entries on the title screen, so their description keys are read from the JSON files
 * under {@code data/thaumictinkerer/enchantment}; the world phase compares them with the dynamic registry.</p>
 */
final class TranslationsCheck implements SmokeCheck {
    @Override
    public String name() {
        return "translations";
    }

    @Override
    public void run(SmokeRun run) throws IOException {
        List<Needed> items = new ArrayList<>();
        for (var entry : run.content().items()) {
            items.add(new Needed(nameKey(new ItemStack(entry.value())), entry.group(), "item:" + entry.path()));
        }
        Translations.verify(run, "items", items);

        List<Needed> blocks = new ArrayList<>();
        for (var entry : run.content().blocks()) {
            blocks.add(new Needed(entry.value().getDescriptionId(), entry.group(), "block:" + entry.path()));
        }
        Translations.verify(run, "blocks", blocks);

        List<Needed> misc = new ArrayList<>();
        Component tab = TTRegistry.CREATIVE_TAB.get().getDisplayName();
        misc.add(new Needed(keyOf(tab), "Core", "creative_tab"));
        for (var id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (!TinkererContent.ours(id)) continue;
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
            misc.add(new Needed(effect.getDescriptionId(), run.content().groups().groupOf(id.getPath()), "effect:" + id.getPath()));
        }
        for (var id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!TinkererContent.ours(id)) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            misc.add(new Needed(type.getDescriptionId(), run.content().groups().groupOf(id.getPath()), "entity:" + id.getPath()));
        }
        Translations.verify(run, "registries_misc", misc);

        Translations.verify(run, "enchantments", enchantmentKeys(run));
    }

    /** The key of the name the game would show for a default stack: the hover name when it is translatable. */
    private static String nameKey(ItemStack stack) {
        Component name = stack.getHoverName();
        String key = keyOf(name);
        return key.isEmpty() ? stack.getItem().getDescriptionId() : key;
    }

    private static String keyOf(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    private List<Needed> enchantmentKeys(SmokeRun run) throws IOException {
        List<Needed> needed = new ArrayList<>();
        var container = FabricLoader.getInstance().getModContainer(LibMisc.MOD_ID);
        if (container.isEmpty()) return needed;
        Path dir = container.get().findPath("data/" + LibMisc.MOD_ID + "/enchantment").orElse(null);
        if (dir == null) return needed;
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        for (Path file : files) {
            String path = file.getFileName().toString().replace(".json", "");
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String key = json.has("description") && json.get("description").isJsonObject()
                    && json.getAsJsonObject("description").has("translate")
                    ? json.getAsJsonObject("description").get("translate").getAsString() : "(no translate key in description)";
            needed.add(new Needed(key, "Enchantments", "enchantment:" + path));
            if (!key.equals("enchantment." + LibMisc.MOD_ID + "." + path)) {
                run.report().fail(PHASE, "enchantment_key_convention", "Enchantments", "enchantment:" + path,
                        "description key is '" + key + "', the convention is enchantment." + LibMisc.MOD_ID + "." + path);
            }
        }
        run.report().inventory("enchantments_from_data", needed.stream().map(Needed::target).toList());
        return needed;
    }
}
