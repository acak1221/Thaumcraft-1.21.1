package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Original TC4 text tokens. Layout is separate so translations remain untouched. */
public final class ResearchText {
    private static final Pattern TAG = Pattern.compile("<IMG>(.*?)</IMG>|<BR>|<LINE>", Pattern.DOTALL);
    private ResearchText() {}
    public sealed interface Part permits Text, Image, Rule {}
    public record Text(String value) implements Part {}
    public record Image(String texture, int u, int v, int width, int height, float scale) implements Part {}
    public record Rule() implements Part {}

    public static List<Part> parse(String original) {
        List<Part> parts = new ArrayList<>();
        var matcher = TAG.matcher(original);
        int previous = 0;
        while (matcher.find()) {
            text(parts, original.substring(previous, matcher.start()));
            if (matcher.group(1) != null) {
                String[] fields = matcher.group(1).split(":");
                if (fields.length != 7) throw new IllegalArgumentException("Invalid TC4 research illustration");
                Image image = new Image(fields[0] + ":" + fields[1], Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]), Integer.parseInt(fields[5]), Float.parseFloat(fields[6]));
                if (image.u < 0 || image.v < 0 || image.width <= 0 || image.height <= 0 || image.scale <= 0 || !Float.isFinite(image.scale)) {
                    throw new IllegalArgumentException("Invalid TC4 illustration dimensions");
                }
                parts.add(image);
            } else if (matcher.group().equals("<LINE>")) parts.add(new Rule());
            else appendText(parts, "\n\n");
            previous = matcher.end();
        }
        text(parts, original.substring(previous));
        return List.copyOf(parts);
    }

    private static void text(List<Part> parts, String value) {
        // Source .lang physical wrapping is not an authored paragraph break.
        String text = value.replaceAll("[\\r\\n]+", " ").replaceAll("\uFFFD(?=[0-9a-fk-or])", "§");
        if (!text.isEmpty()) appendText(parts, text);
    }

    private static void appendText(List<Part> parts, String text) {
        if (!parts.isEmpty() && parts.getLast() instanceof Text previous) {
            parts.set(parts.size() - 1, new Text(previous.value + text));
        } else parts.add(new Text(text));
    }
}
