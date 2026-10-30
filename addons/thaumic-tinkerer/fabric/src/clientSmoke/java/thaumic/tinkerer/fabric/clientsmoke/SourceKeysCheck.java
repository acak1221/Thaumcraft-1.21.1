package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import thaumic.tinkerer.fabric.clientsmoke.Translations.Needed;

/**
 * The translation keys the add-on's code names as string literals (tooltips, chat messages, screen labels). Test-only
 * code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The registries cannot say which labels a screen draws, the sources can: every literal that looks like one of the
 * add-on's key families ({@code ttmisc.*}, {@code ttwaila.*}, {@code container.thaumictinkerer.*}...) is looked up. A literal
 * that ends in a dot is the front half of a computed key ({@code "ttmisc.mode." + n}); for those the check only asks that
 * some key with the prefix exists. A key that is only used inside a comment can show up here; the target names the
 * file so that it can be told.</p>
 */
final class SourceKeysCheck implements SmokeCheck {
    private static final Pattern LITERAL = Pattern.compile("\"((?:ttmisc|ttwaila|ttcomputer|ttresearch|tt\\.nei|tooltip\\.thaumictinkerer|"
            + "container\\.thaumictinkerer|effect\\.thaumictinkerer|item\\.thaumictinkerer|block\\.thaumictinkerer|"
            + "enchantment\\.thaumictinkerer|itemGroup)\\.[A-Za-z0-9_.:]*)\"(\\s*\\+)?");

    @Override
    public String name() {
        return "source_translation_keys";
    }

    @Override
    public void run(SmokeRun run) throws IOException {
        Path root = run.sourceRoot();
        List<Path> files = new ArrayList<>();
        for (String dir : List.of("common/src/main/java", "fabric/src/main/java")) {
            try (Stream<Path> stream = Files.walk(root.resolve(dir))) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Needed> exact = new ArrayList<>();
        List<String> prefixes = new ArrayList<>();
        Map<String, String> english = run.lang("en_us");
        for (Path file : files) {
            String className = file.getFileName().toString().replace(".java", "");
            String group = run.content().groups().groupOfClass(className);
            Matcher matcher = LITERAL.matcher(Files.readString(file));
            while (matcher.find()) {
                String key = matcher.group(1);
                if (!seen.add(className + "|" + key)) continue;
                // "ttmisc.mode." + n and "ttmisc.awakenedArmor" + n are the front half of a computed key
                if (key.endsWith(".") || matcher.group(2) != null) {
                    boolean any = english.keySet().stream().anyMatch(k -> k.startsWith(key));
                    prefixes.add(key);
                    if (!any) {
                        run.report().fail(PHASE, "missing_translation", group, "source:" + className, "no en_us key starts with computed prefix '" + key + "'");
                    }
                } else {
                    exact.add(new Needed(key, group, "source:" + className));
                }
            }
        }
        Translations.verify(run, "source_literals", exact);
        run.report().info(PHASE, name(), exact.size() + " literal keys and " + prefixes.size() + " computed prefixes in " + files.size() + " source files");
    }
}
