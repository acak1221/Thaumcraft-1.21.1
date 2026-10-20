package thaumcraft.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Read-only view of the packaged mod resources for the data-invariant tests (no Minecraft classes, no registries). */
final class ResourceTree {
    private ResourceTree() { }

    static boolean exists(String path) { return ResourceTree.class.getClassLoader().getResource(path) != null; }

    static String text(String path) throws IOException {
        try (InputStream stream = ResourceTree.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("missing resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static byte[] bytes(String path) throws IOException {
        try (InputStream stream = ResourceTree.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("missing resource " + path);
            return stream.readAllBytes();
        }
    }

    static JsonElement json(String path) throws IOException { return JsonParser.parseString(text(path)); }

    static JsonObject object(String path) throws IOException { return json(path).getAsJsonObject(); }

    /** Every object key that appears more than once in one JSON object of the text (Gson would silently keep the last one). */
    static List<String> duplicateKeys(String text) throws IOException {
        List<String> duplicates = new ArrayList<>();
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            scan(reader, "$", duplicates);
        }
        return duplicates;
    }

    private static void scan(JsonReader reader, String path, List<String> duplicates) throws IOException {
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject();
                Set<String> seen = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!seen.add(name)) duplicates.add(path + "." + name);
                    scan(reader, path + "." + name, duplicates);
                }
                reader.endObject();
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                int index = 0;
                while (reader.hasNext()) scan(reader, path + "[" + index++ + "]", duplicates);
                reader.endArray();
            }
            default -> reader.skipValue();
        }
    }

    /**
     * Every regular file below {@code dir} (recursive) in the <em>mod's own</em> resources, as sorted paths relative to {@code dir}
     * with {@code /} separators. The game jars are on the test classpath too and carry their own {@code data/minecraft/...}; those
     * are not the mod's files and are left out here.
     */
    static List<String> files(String dir) throws IOException, URISyntaxException { return list(dir, true); }

    /** Like {@link #files} but over every classpath root, which is what compiled classes (a different root than resources) need. */
    static List<String> filesOnAnyRoot(String dir) throws IOException, URISyntaxException { return list(dir, false); }

    /** The classpath root that holds the mod's resources, found through a file only the mod has. */
    private static String modRoot() {
        String marker = "thaumcraft-common.mixins.json";
        URL url = ResourceTree.class.getClassLoader().getResource(marker);
        if (url == null) throw new AssertionError("missing resource " + marker);
        String text = url.toString();
        return text.substring(0, text.length() - marker.length());
    }

    private static List<String> list(String dir, boolean modOnly) throws IOException, URISyntaxException {
        String clean = dir.endsWith("/") ? dir.substring(0, dir.length() - 1) : dir;
        String root = modOnly ? modRoot() : "";
        Set<String> found = new TreeSet<>();
        var roots = ResourceTree.class.getClassLoader().getResources(clean);
        while (roots.hasMoreElements()) {
            URL url = roots.nextElement();
            if (modOnly && !url.toString().startsWith(root)) continue;
            if (url.getProtocol().equals("file")) {
                Path base = Path.of(url.toURI());
                try (Stream<Path> walk = Files.walk(base)) {
                    walk.filter(Files::isRegularFile).forEach(path -> found.add(base.relativize(path).toString().replace('\\', '/')));
                }
            } else if (url.getProtocol().equals("jar")) {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                connection.setUseCaches(false);
                String prefix = clean + "/";
                try (JarFile jar = connection.getJarFile()) {
                    jar.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith(prefix))
                            .forEach(entry -> found.add(entry.getName().substring(prefix.length())));
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(found));
    }

    /** File names directly inside {@code dir} that end with {@code suffix}, without the suffix, sorted. */
    static List<String> stems(String dir, String suffix) throws IOException, URISyntaxException {
        List<String> stems = new ArrayList<>();
        for (String file : files(dir)) if (file.indexOf('/') < 0 && file.endsWith(suffix)) stems.add(file.substring(0, file.length() - suffix.length()));
        return stems;
    }
}
