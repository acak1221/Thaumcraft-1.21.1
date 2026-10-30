package thaumic.tinkerer.fabric.clientsmoke;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Tells whether a block that was put on the stage can be seen in a picture: the cell's cube is projected through the camera
 * of the picture, and the pixels of the picture inside it are compared with the picture of the same empty stage. Test-only code
 * of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>A block that renders nothing (a broken or missing model, a camouflage block with nothing to copy, a gas) leaves the
 * pixels as they were. The result is a number, not a verdict: whether a block ought to be invisible is for the reader to
 * judge.</p>
 */
final class Visibility {
    /** Share of pixels of a cell's box that differ from the empty stage for the cell to count as seen. */
    static final double SEEN = 0.02;

    private Visibility() {}

    /** The camera of a picture, read from the client player at the moment the picture was requested. */
    record Camera(Vec3 eye, float pitch, float yaw, double fovDegrees, int width, int height) {
        static Camera of(Minecraft client) {
            return new Camera(client.player.getEyePosition(), client.player.getXRot(), client.player.getYRot(),
                    client.options.fov().get(), client.getWindow().getWidth(), client.getWindow().getHeight());
        }
    }

    /** The share of the pixels inside the projected unit cube at {@code cell} that differ between the two pictures; -1 when unreadable. */
    static double changed(Path picture, Path reference, Camera camera, BlockPos cell) {
        try (InputStream a = Files.newInputStream(picture); InputStream b = Files.newInputStream(reference);
                NativeImage image = NativeImage.read(a); NativeImage empty = NativeImage.read(b)) {
            int[] box = box(camera, cell, image.getWidth(), image.getHeight());
            if (box == null) return -1;
            long different = 0;
            long total = 0;
            for (int y = box[1]; y <= box[3]; y++) {
                for (int x = box[0]; x <= box[2]; x++) {
                    total++;
                    int p = image.getPixelRGBA(x, y);
                    int q = empty.getPixelRGBA(x, y);
                    int delta = Math.abs((p & 0xFF) - (q & 0xFF)) + Math.abs(((p >> 8) & 0xFF) - ((q >> 8) & 0xFF))
                            + Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF));
                    if (delta > 36) different++;
                }
            }
            return total == 0 ? -1 : (double) different / total;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    /** Pixel bounding box {minX, minY, maxX, maxY} of the unit cube whose lower corner is {@code cell}, clipped to the picture. */
    static int[] box(Camera camera, BlockPos cell, int width, int height) {
        double yaw = Math.toRadians(camera.yaw());
        double pitch = Math.toRadians(camera.pitch());
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(forward);
        double focal = (height / 2.0) / Math.tan(Math.toRadians(camera.fovDegrees()) / 2.0);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3 corner = new Vec3(cell.getX() + (i & 1), cell.getY() + ((i >> 1) & 1), cell.getZ() + ((i >> 2) & 1)).subtract(camera.eye());
            double depth = corner.dot(forward);
            if (depth < 0.1) return null;
            double x = width / 2.0 + focal * corner.dot(right) / depth;
            double y = height / 2.0 - focal * corner.dot(up) / depth;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        int x0 = (int) Math.max(0, Math.floor(minX));
        int y0 = (int) Math.max(0, Math.floor(minY));
        int x1 = (int) Math.min(width - 1, Math.ceil(maxX));
        int y1 = (int) Math.min(height - 1, Math.ceil(maxY));
        return x1 < x0 || y1 < y0 ? null : new int[] { x0, y0, x1, y1 };
    }
}
