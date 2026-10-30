package thaumic.tinkerer.fabric.clientsmoke;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Takes the pictures. The window's frame buffer is read with {@link Screenshot#grab} from the client tick, which runs
 * between two frames: the buffer then still holds the last complete frame, so a picture is requested only after the frames
 * the scene needs (see {@link Frames}). Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the
 * add-on, not distributed.
 *
 * <p>Each picture is also measured (the share of the most frequent colour, the mean colour): an all-black or one-colour
 * frame means the scene was not drawn, which a viewer of the PNG would see at once but a script would not.</p>
 */
final class Shots {
    private final SmokeReport report;
    private final Path dir;

    Shots(SmokeReport report) {
        this.report = report;
        this.dir = report.outDir().resolve("screenshots");
    }

    /**
     * A step that grabs the frame buffer once and finishes when the PNG is written. {@code details} are added to the entry of
     * the report (group, targets, what the picture should show).
     */
    Script.Step shot(String name, String group, String shows) {
        boolean[] issued = { false };
        return run -> {
            Minecraft client = run.client();
            String file = name + ".png";
            Path path = dir.resolve(file);
            if (!issued[0]) {
                issued[0] = true;
                Files.deleteIfExists(path);
                Screenshot.grab(report.outDir().toFile(), file, client.getMainRenderTarget(), message -> { });
                return false;
            }
            if (!Files.isRegularFile(path) || Files.size(path) < 100) return false;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", file);
            entry.put("path", path.toString());
            entry.put("group", group);
            entry.put("shows", shows);
            entry.put("screen", client.screen == null ? "none (world)" : client.screen.getClass().getName());
            entry.put("window", client.getWindow().getWidth() + "x" + client.getWindow().getHeight());
            entry.putAll(measure(path));
            report.screenshot(entry);
            report.write();
            return true;
        };
    }

    /** Colour statistics of the PNG; a failed read is recorded, not thrown. */
    static Map<String, Object> measure(Path png) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (InputStream in = Files.newInputStream(png); NativeImage image = NativeImage.read(in)) {
            Map<Integer, Integer> counts = new HashMap<>();
            long red = 0, green = 0, blue = 0, samples = 0;
            for (int y = 0; y < image.getHeight(); y += 3) {
                for (int x = 0; x < image.getWidth(); x += 3) {
                    int abgr = image.getPixelRGBA(x, y);
                    counts.merge(abgr & 0xFFFFFF, 1, Integer::sum);
                    red += abgr & 0xFF;
                    green += (abgr >> 8) & 0xFF;
                    blue += (abgr >> 16) & 0xFF;
                    samples++;
                }
            }
            int top = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            double share = samples == 0 ? 1.0 : (double) top / samples;
            result.put("pixels", image.getWidth() + "x" + image.getHeight());
            result.put("most_frequent_colour_share", Math.round(share * 1000) / 1000.0);
            result.put("distinct_colours_sampled", counts.size());
            result.put("mean_rgb", new int[] { (int) (red / Math.max(1, samples)), (int) (green / Math.max(1, samples)),
                    (int) (blue / Math.max(1, samples)) });
            result.put("blank", share > 0.995);
        } catch (IOException | RuntimeException e) {
            result.put("measure_error", e.toString());
        }
        return result;
    }
}
