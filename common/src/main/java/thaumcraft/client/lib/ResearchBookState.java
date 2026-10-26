package thaumcraft.client.lib;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import thaumcraft.common.Thaumcraft;

/**
 * The port's own Thaumonomicon conveniences (ThaumcraftTweaks): the research page the book reopens on
 * and the research bookmarks. Neither exists in TC4 4.2.3.5; both are client state only.
 */
public final class ResearchBookState {
    /** At most this many bookmarks, one column beside the map. */
    public static final int MAX_BOOKMARKS = 9;
    private static String pageResearch;
    private static int page;
    private static Bookmarks bookmarks;

    private ResearchBookState() {}

    /** The book was closed on this research page: the next opening shows it again. */
    public static void rememberPage(String research, int spread) { pageResearch = research; page = Math.max(0, spread); }
    /** The player went back to the map: the next opening shows the map. */
    public static void forgetPage() { pageResearch = null; page = 0; }
    public static String rememberedResearch() { return pageResearch; }
    public static int rememberedPage() { return page; }

    /** The bookmarks of this computer, read once from the config folder. */
    public static Bookmarks bookmarks() {
        if (bookmarks == null) {
            bookmarks = Bookmarks.load(dev.architectury.platform.Platform.getConfigFolder().resolve("thaumcraft-bookmarks.txt"));
        }
        return bookmarks;
    }

    /** Research keys, one per line, in the order they were bookmarked. */
    public static final class Bookmarks {
        private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_.:@-]{1,64}");
        private final Path file;
        private final LinkedHashSet<String> keys = new LinkedHashSet<>();

        private Bookmarks(Path file) { this.file = file; }

        /** Reads the file; a missing or unreadable file is no bookmarks, invalid lines are skipped. */
        static Bookmarks load(Path file) {
            var loaded = new Bookmarks(file);
            if (file == null || !Files.isRegularFile(file)) return loaded;
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String key = line.strip();
                    if (KEY.matcher(key).matches() && loaded.keys.size() < MAX_BOOKMARKS) loaded.keys.add(key);
                }
            } catch (IOException | RuntimeException unreadable) {
                Thaumcraft.LOG.warn("Cannot read the Thaumonomicon bookmarks at {}", file, unreadable);
            }
            return loaded;
        }

        public boolean contains(String key) { return keys.contains(key); }
        public List<String> keys() { return List.copyOf(keys); }
        public boolean full() { return keys.size() >= MAX_BOOKMARKS; }

        /** Bookmarks the research, or removes its bookmark; answers whether it is bookmarked now. */
        public boolean toggle(String key) {
            if (!keys.remove(key)) {
                if (!KEY.matcher(key).matches() || full()) return false;
                keys.add(key);
            }
            save();
            return keys.contains(key);
        }

        public void remove(String key) { if (keys.remove(key)) save(); }

        private void save() {
            if (file == null) return;
            try {
                Files.createDirectories(file.toAbsolutePath().getParent());
                Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
                Files.write(temporary, new ArrayList<>(keys), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException plainFileSystem) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException | RuntimeException failure) {
                // The bookmark still works for this session.
                Thaumcraft.LOG.warn("Cannot save the Thaumonomicon bookmarks at {}", file, failure);
            }
        }
    }
}
