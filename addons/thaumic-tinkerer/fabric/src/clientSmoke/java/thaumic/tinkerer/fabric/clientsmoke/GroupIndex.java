package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;

/**
 * Which content group registered a registry path. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0
 * like the add-on, not distributed.
 *
 * <p>The groups of {@code thaumic.tinkerer.common.content} register their objects one after the other, and the paths
 * they register are the constants of {@code LibBlockNames}/{@code LibItemNames}. This class reads the source of each group
 * and resolves the constants that are the first argument of a {@code TTRegistry} registration call. A path that a source
 * does not name (a computed one such as {@code infused_grain_aer}) takes the group of the path registered just before it,
 * because the groups register in sequence; the report says which of the two rules gave the group.</p>
 */
final class GroupIndex {
    /** The groups in the order {@code ContentGroups.ALL} registers them. */
    static final List<String> GROUPS = List.of("SimpleBlocks", "TileBlocks", "Machines", "BasicItems", "Foci", "Enchantments",
            "KamiGear", "KamiWorld");

    private static final Pattern REGISTRATION = Pattern.compile(
            "(?:TTRegistry\\.(?:item|block|blockWithoutItem|hiddenItem)|\\bfire|BLOCK_ENTITIES\\.register|MENUS\\.register)\\(\\s*"
                    + "(?:(Lib(?:Block|Item)Names)\\.([A-Z0-9_]+)(\\[[^\\]]*\\])?|\"([a-z0-9_]+)\")");

    private static final Pattern IDENTIFIER = Pattern.compile("\\b[A-Z][A-Za-z0-9]{3,}\\b");

    private final Map<String, String> classGroup = new HashMap<>();
    private final Map<String, String> bySource = new HashMap<>();
    private final Map<String, String> byOrder = new HashMap<>();
    private final List<String> problems = new ArrayList<>();

    private GroupIndex() {}

    /** Reads the group sources under {@code sourceRoot} (the directory addons/thaumic-tinkerer). */
    static GroupIndex load(Path sourceRoot) {
        GroupIndex index = new GroupIndex();
        for (String group : GROUPS) {
            Path file = sourceRoot.resolve("common/src/main/java/thaumic/tinkerer/common/content/" + group + ".java");
            try {
                String text = Files.readString(file);
                Matcher matcher = REGISTRATION.matcher(text);
                while (matcher.find()) {
                    for (String path : resolve(matcher)) index.bySource.putIfAbsent(path, group);
                }
                index.collectClasses(text, group);
                Path client = sourceRoot.resolve("common/src/main/java/thaumic/tinkerer/client/" + group + "Client.java");
                if (Files.isRegularFile(client)) index.collectClasses(Files.readString(client), group);
            } catch (IOException | ReflectiveOperationException e) {
                index.problems.add(group + ": " + e);
            }
        }
        return index;
    }

    /** Every capitalised identifier of a group's registrar belongs to that group (the first group that names it wins). */
    private void collectClasses(String text, String group) {
        Matcher matcher = IDENTIFIER.matcher(text);
        while (matcher.find()) classGroup.putIfAbsent(matcher.group(), group);
    }

    /** The group whose registrar names a class (a block, item, screen or renderer), or "Unassigned". */
    String groupOfClass(String simpleName) {
        return classGroup.getOrDefault(simpleName, "Unassigned");
    }

    private static List<String> resolve(Matcher matcher) throws ReflectiveOperationException {
        if (matcher.group(4) != null) return List.of(matcher.group(4));
        Class<?> holder = matcher.group(1).equals("LibBlockNames") ? LibBlockNames.class : LibItemNames.class;
        Field field = holder.getField(matcher.group(2));
        Object value = field.get(null);
        if (value instanceof String path) return List.of(path);
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < Array.getLength(value); i++) paths.add((String) Array.get(value, i));
        return paths;
    }

    /**
     * Fills the groups of the paths the sources do not name, from the registration order of {@code orderedPaths} (all the
     * registry paths of the add-on, blocks and items, each registry in its own order).
     */
    void inheritInOrder(List<String> orderedPaths) {
        String previous = null;
        List<String> pending = new ArrayList<>();
        for (String path : orderedPaths) {
            String group = bySource.get(path);
            if (group == null) {
                if (previous == null) pending.add(path);
                else byOrder.put(path, previous);
            } else {
                for (String early : pending) byOrder.put(early, group);
                pending.clear();
                previous = group;
            }
        }
    }

    String groupOf(String path) {
        String group = bySource.get(path);
        if (group != null) return group;
        group = byOrder.get(path);
        return group == null ? "Unassigned" : group;
    }

    /** "source" when a group source names the path, "registration order" when it was inherited, "none" otherwise. */
    String basis(String path) {
        return bySource.containsKey(path) ? "source" : byOrder.containsKey(path) ? "registration order" : "none";
    }

    List<String> problems() {
        return problems;
    }

    Map<String, String> asMap(List<String> paths) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String path : paths) map.put(path, groupOf(path) + " (" + basis(path) + ")");
        return map;
    }
}
