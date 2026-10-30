package thaumic.tinkerer.testing.parity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * One documented difference between the original and the port ({@code tt_oracle/known_differences.json}): the
 * subject (a research key or a recipe name), the field, the original value, the port value and the reason, which
 * names the deviation text it comes from ({@code deviation}: the file below docs/deviations, {@code quote}: the sentence of
 * that file that documents it; {@code mentions} are names that the passage around the sentence has to contain and that bind
 * it to this entry; a unit test checks both). It allows exactly that difference, {@code occurrences} times (default once:
 * an entry is consumed by the difference it matches), and nothing else: a different port value for the same field is a
 * failure again, and so is a second identical difference. Test-only code of the port, CC BY-NC-SA 3.0 like the add-on.
 */
public record KnownDifference(String kind, String key, String field, String original, String port, String reason,
        String deviation, String quote, List<String> mentions, int occurrences) {
    public static final String RESOURCE = "/tt_oracle/known_differences.json";

    public static List<KnownDifference> fromClasspath() {
        try (InputStream in = KnownDifference.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException(RESOURCE + " is not on the classpath");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException error) {
            throw new IllegalStateException("cannot read " + RESOURCE, error);
        }
    }

    public static List<KnownDifference> parse(JsonObject document) {
        List<KnownDifference> out = new ArrayList<>();
        for (JsonElement e : document.getAsJsonArray("differences")) {
            JsonObject o = e.getAsJsonObject();
            String reason = o.get("reason").getAsString();
            String deviation = o.has("deviation") ? o.get("deviation").getAsString() : "";
            String quote = o.has("quote") ? o.get("quote").getAsString() : "";
            if (reason.isBlank()) throw new IllegalStateException("a known difference without a reason: " + o);
            List<String> mentions = new ArrayList<>();
            if (o.has("mentions")) for (JsonElement m : o.getAsJsonArray("mentions")) mentions.add(m.getAsString());
            int occurrences = o.has("occurrences") ? o.get("occurrences").getAsInt() : 1;
            if (occurrences < 1) throw new IllegalStateException("a known difference that allows nothing: " + o);
            out.add(new KnownDifference(o.get("kind").getAsString(), o.get("key").getAsString(), o.get("field").getAsString(),
                    o.get("original").getAsString(), o.get("port").getAsString(), reason, deviation, quote, mentions, occurrences));
        }
        return out;
    }

    public boolean matches(ParityComparator.Difference d) {
        return kind.equals(d.kind()) && key.equals(d.subject()) && field.equals(d.field()) && original.equals(d.original())
                && port.equals(d.port());
    }

    @Override
    public String toString() {
        return kind + " " + key + " " + field + ": original=" + original + " port=" + port;
    }

    /** The entry as the comparison prints a difference, for the messages about a stale entry. */
    public String describe() {
        return occurrences == 1 ? toString() : toString() + " (x" + occurrences + ")";
    }
}
