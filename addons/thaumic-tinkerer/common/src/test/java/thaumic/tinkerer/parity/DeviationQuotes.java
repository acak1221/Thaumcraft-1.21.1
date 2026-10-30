package thaumic.tinkerer.parity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import thaumic.tinkerer.testing.parity.KnownDifference;

/**
 * Checks that an entry of the allow-list is really documented by the deviation text it cites: the quoted sentence is in
 * the file, the passage around it (its paragraph or bullet, the two before it and the nearest heading above) contains
 * every name the entry lists under {@code mentions}, and at least one of those names is the entry's own (its research key,
 * its recipe name or the registry path of its result). A sentence of some other part of the deviation texts cannot be used
 * to allow-list a difference. Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 */
final class DeviationQuotes {
    private static final int ITEMS_BEFORE = 2;

    private DeviationQuotes() {}

    static String normalised(String text) {
        return text.replace("`", "").replace("*", "").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    static String letters(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** A paragraph, a bullet with its continuation lines, a table row or a heading. */
    record Item(String text, boolean heading) {}

    static List<Item> items(String markdown) {
        List<Item> items = new ArrayList<>();
        StringBuilder current = null;
        for (String line : markdown.split("\n", -1)) {
            boolean blank = line.isBlank();
            boolean heading = line.startsWith("#");
            boolean bullet = line.matches("^\\s{0,1}([-*]|\\d+\\.)\\s.*") || line.startsWith("|");
            if (blank || heading || bullet) {
                if (current != null) items.add(new Item(current.toString(), false));
                current = null;
                if (heading) items.add(new Item(line, true));
                else if (!blank) current = new StringBuilder(line);
            } else if (current == null) {
                current = new StringBuilder(line);
            } else {
                current.append(' ').append(line);
            }
        }
        if (current != null) items.add(new Item(current.toString(), false));
        return items;
    }

    /** The names of the entry that a mention may stand for: the key, or the name and the result path of a recipe key. */
    static List<String> ownNames(KnownDifference k) {
        List<String> names = new ArrayList<>();
        if (k.kind().equals("recipe") && k.key().contains("->")) {
            String left = k.key().substring(0, k.key().indexOf("->")).trim();
            String right = k.key().substring(k.key().indexOf("->") + 2).trim();
            if (!left.startsWith("<")) names.add(letters(left));
            names.add(letters(right.substring(right.lastIndexOf(':') + 1)));
        } else {
            names.add(letters(k.key()));
        }
        names.removeIf(n -> n.length() < 4);
        return names;
    }

    /** Everything that is wrong with the way the entry cites the deviation text; empty when it is documented. */
    static List<String> problems(KnownDifference k, Path deviations) throws IOException {
        List<String> problems = new ArrayList<>();
        Path file = deviations.resolve(k.deviation());
        if (k.deviation().isBlank() || !Files.isRegularFile(file)) {
            problems.add("the deviation file " + file + " does not exist");
            return problems;
        }
        if (k.quote().isBlank()) {
            problems.add("quotes nothing");
            return problems;
        }
        List<Item> items = items(Files.readString(file, StandardCharsets.UTF_8));
        String quote = normalised(k.quote());
        int at = -1;
        for (int i = 0; i < items.size() && at < 0; i++) if (normalised(items.get(i).text()).contains(quote)) at = i;
        if (at < 0) {
            problems.add("the sentence \"" + k.quote() + "\" is not in one passage of " + k.deviation());
            return problems;
        }
        StringBuilder window = new StringBuilder();
        for (int i = at; i >= 0 && i >= at - ITEMS_BEFORE; i--) window.append(' ').append(items.get(i).text());
        for (int i = at; i >= 0; i--) {
            if (items.get(i).heading()) {
                window.append(' ').append(items.get(i).text());
                break;
            }
        }
        String around = normalised(window.toString());
        if (k.mentions().isEmpty()) problems.add("lists no mentions: nothing ties the sentence to the entry");
        boolean tied = false;
        List<String> own = ownNames(k);
        for (String mention : k.mentions()) {
            if (!around.contains(normalised(mention))) {
                problems.add("\"" + mention + "\" is not in the passage around the sentence (" + k.deviation() + ")");
            }
            String m = letters(mention);
            for (String name : own) if (m.length() >= 4 && (m.contains(name) || name.contains(m))) tied = true;
        }
        if (!k.mentions().isEmpty() && !tied) problems.add("no mention names the entry " + k.key() + " (its own names: " + own + ")");
        return problems;
    }
}
