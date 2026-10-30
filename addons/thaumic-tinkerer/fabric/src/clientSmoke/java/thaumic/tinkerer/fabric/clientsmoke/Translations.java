package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.locale.Language;

/**
 * Checks translation keys in English (the game's own lookup) and in Russian (the language file). Test-only code of the port
 * (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>A missing English key shows the raw key to the player. A missing Russian key shows the English text (the game fills
 * a language from en_us), so it is a gap in the translation and not a broken screen; the two are separate results.</p>
 */
final class Translations {
    /** A key some object of the add-on needs. {@code group} and {@code target} say whose it is. */
    record Needed(String key, String group, String target) {}

    private Translations() {}

    /** Records one result line per missing key and one summary per family and language. */
    static void verify(SmokeRun run, String family, List<Needed> needed) {
        Map<String, String> russian = run.lang("ru_ru");
        Language english = Language.getInstance();
        List<String> noEnglish = new ArrayList<>();
        List<String> noRussian = new ArrayList<>();
        for (Needed need : needed) {
            if (!english.has(need.key())) {
                noEnglish.add(need.key());
                run.report().fail(SmokeCheck.PHASE, "missing_translation", need.group(), need.target(),
                        family + ": en_us has no '" + need.key() + "' (the game shows the raw key)");
            }
            if (!russian.containsKey(need.key())) {
                noRussian.add(need.key());
                run.report().fail(SmokeCheck.PHASE, "missing_translation_ru_ru", need.group(), need.target(),
                        family + ": ru_ru has no '" + need.key() + "' (the game shows the English text"
                                + (english.has(need.key()) ? ")" : ", and en_us has none either)"));
            }
        }
        run.report().add(SmokeCheck.PHASE, "translations_" + family, "", "", noEnglish.isEmpty() && noRussian.isEmpty()
                ? SmokeReport.Result.PASS : SmokeReport.Result.INFO,
                needed.size() + " keys; missing in en_us: " + noEnglish.size() + ", missing in ru_ru: " + noRussian.size());
    }
}
