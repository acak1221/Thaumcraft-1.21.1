package thaumic.tinkerer.fabric.clientsmoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes {@code screenshots/INDEX.md}: every picture of the run with the one sentence that says what it shows, grouped by
 * content group, so that someone who did not run the harness can find the picture they need. Test-only code of the port (no
 * 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 */
final class ScreenshotIndex {
    private ScreenshotIndex() {}

    static void write(SmokeReport report) throws IOException {
        Map<String, StringBuilder> byGroup = new TreeMap<>();
        List<Map<String, Object>> shots = report.screenshots();
        for (Map<String, Object> shot : shots) {
            String group = String.valueOf(shot.get("group"));
            byGroup.computeIfAbsent(group, g -> new StringBuilder()).append("| `").append(shot.get("name")).append("` | ")
                    .append(String.valueOf(shot.get("shows")).replace("|", "/")).append(Boolean.TRUE.equals(shot.get("blank")) ? " **(the picture is one colour: nothing was drawn)**" : "")
                    .append(" |\n");
        }
        StringBuilder text = new StringBuilder();
        text.append("# Thaumic Tinkerer client smoke: screenshots\n\n");
        text.append(shots.size()).append(" pictures of one run, 1280x720, taken by the harness in `addons/thaumic-tinkerer/fabric/src/clientSmoke` ")
                .append("under Xvfb with Mesa software rendering (`docs/CLIENT_VERIFICATION.md`). Names say the kind: `blocks-` block rows (labels over the cells give ")
                .append("the block and the state that differs from the default; left to right = the order of the description), `items-` item grids (registry path, ")
                .append("then the translated name under each icon), `menu-` screens opened by a real interaction, `renderer-` close-ups of block entities with a renderer, ")
                .append("`hud-` overlays, `reference-` the empty stage that the visibility measure compares with; a `-ru_ru` suffix is the same picture after switching the ")
                .append("game to Russian. A picture is evidence of what this machine drew (software renderer, no GPU), not of how a GPU would draw it.\n\n");
        for (Map.Entry<String, StringBuilder> group : byGroup.entrySet()) {
            text.append("## ").append(group.getKey()).append("\n\n| file | what it shows |\n|---|---|\n").append(group.getValue()).append('\n');
        }
        Files.writeString(report.outDir().resolve("screenshots").resolve("INDEX.md"), text.toString(), StandardCharsets.UTF_8);
    }
}
