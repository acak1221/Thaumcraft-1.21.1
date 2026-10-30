package thaumic.tinkerer.fabric.clientsmoke;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The registered content of the add-on, read from the registries after the game started. Test-only code of the port (no
 * 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>Nothing here is a hand-written list of the add-on's blocks or items: a new registration shows up in the smoke on its
 * own. The only hand-written knowledge is in {@link GroupIndex}, which says what group a path belongs to.</p>
 */
final class TinkererContent {
    record Entry<T>(String path, String group, T value) {
        ResourceLocation id() {
            return LibMisc.id(path);
        }
    }

    private final GroupIndex groups;
    private final List<Entry<Block>> blocks = new ArrayList<>();
    private final List<Entry<Item>> items = new ArrayList<>();
    private final List<Entry<BlockEntityType<?>>> blockEntityTypes = new ArrayList<>();
    private final List<Entry<MenuType<?>>> menus = new ArrayList<>();

    TinkererContent(Path sourceRoot) {
        groups = GroupIndex.load(sourceRoot);
        // Registry order: a registry is iterated by raw id (registration order); its keySet() is a hash set and has no order.
        fill(BuiltInRegistries.BLOCK, blocks);
        fill(BuiltInRegistries.ITEM, items);
        fill(BuiltInRegistries.BLOCK_ENTITY_TYPE, blockEntityTypes);
        fill(BuiltInRegistries.MENU, menus);
        // a path the sources do not name takes the group of the path registered before it (the groups register in sequence)
        groups.inheritInOrder(blocks.stream().map(Entry::path).toList());
        groups.inheritInOrder(items.stream().map(Entry::path).toList());
        regroup(blocks);
        regroup(items);
        regroup(blockEntityTypes);
        regroup(menus);
        Comparator<Entry<?>> byGroup = Comparator.comparingInt(e -> GroupIndex.GROUPS.indexOf(e.group()));
        blocks.sort(byGroup);
        items.sort(byGroup);
        blockEntityTypes.sort(byGroup);
        menus.sort(byGroup);
    }

    private <T> void fill(net.minecraft.core.Registry<T> registry, List<Entry<T>> target) {
        for (T value : registry) {
            ResourceLocation id = registry.getKey(value);
            if (id != null && ours(id)) target.add(new Entry<>(id.getPath(), "", value));
        }
    }

    /** Entries are records; the group is known only after the whole registry was read, so they are rebuilt. */
    private <T> void regroup(List<Entry<T>> entries) {
        for (int i = 0; i < entries.size(); i++) {
            Entry<T> entry = entries.get(i);
            entries.set(i, new Entry<>(entry.path(), groups.groupOf(entry.path()), entry.value()));
        }
    }

    static boolean ours(ResourceLocation id) {
        return id.getNamespace().equals(LibMisc.MOD_ID);
    }

    GroupIndex groups() {
        return groups;
    }

    List<Entry<Block>> blocks() {
        return blocks;
    }

    List<Entry<Item>> items() {
        return items;
    }

    List<Entry<BlockEntityType<?>>> blockEntityTypes() {
        return blockEntityTypes;
    }

    List<Entry<MenuType<?>>> menus() {
        return menus;
    }

    /** The paths of the entries, for the inventory lists of the report. */
    static <T> List<String> paths(List<Entry<T>> entries) {
        return entries.stream().map(e -> e.group() + ":" + e.path()).collect(Collectors.toList());
    }
}
