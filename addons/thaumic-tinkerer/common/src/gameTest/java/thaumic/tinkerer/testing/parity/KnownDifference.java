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
 * names the deviation text it comes from. It allows exactly that difference and nothing else: a different port value
 * for the same field is a failure again. Test-only code of the port, CC BY-NC-SA 3.0 like the add-on.
 */
public record KnownDifference(String kind, String key, String field, String original, String port, String reason,
        String deviation) {
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
            if (reason.isBlank()) throw new IllegalStateException("a known difference without a reason: " + o);
            out.add(new KnownDifference(o.get("kind").getAsString(), o.get("key").getAsString(), o.get("field").getAsString(),
                    o.get("original").getAsString(), o.get("port").getAsString(), reason, deviation));
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
}
