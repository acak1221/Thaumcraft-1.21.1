package thaumcraft.fabric.testing;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;
import thaumcraft.common.lib.research.ResearchDefinition;

/** Actual resource-pack reload and public IMAGE-page rendering; isolated review client only. */
public final class TailImageCacheReviewChecks {
    private TailImageCacheReviewChecks() {}

    public record Result(List<Map<String, Object>> checks, List<String> failures, String fixtureDirectory) {
        public Result {
            checks = List.copyOf(checks);
            failures = List.copyOf(failures);
        }
    }

    /**
     * Invoke once on the client thread in the renderer review world, with no reload in progress.
     * Poll the returned future from subsequent client ticks; do not join it on the client thread.
     * The fixture pack is deselected and the original selection reloaded before completion.
     * Its local files are retained in the isolated run directory as evidence.
     */
    public static CompletableFuture<Result> run(Minecraft client) {
        try {
            require(client.isSameThread(), "IMAGE reload review must start on the client thread");
            require(client.gameDirectory.toPath().toAbsolutePath().normalize().getFileName().toString()
                    .equals("run-review-renderers"), "IMAGE reload review requires run-review-renderers");
            require(client.level != null && client.player != null && client.getOverlay() == null,
                    "IMAGE reload review requires a loaded world and no active resource reload");
            return new Scenario(client).run();
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private static final class Scenario {
        private final Minecraft client;
        private final List<String> previousSelection;
        private final List<Map<String, Object>> checks = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();
        private final Path packDirectory;
        private final String packId;
        private final ResourceLocation absent, resized;
        private final byte[] square, rectangle;

        Scenario(Minecraft client) throws IOException {
            this.client = client;
            previousSelection = List.copyOf(client.getResourcePackRepository().getSelectedIds());
            Path packs = client.getResourcePackDirectory().toAbsolutePath().normalize();
            require(packs.startsWith(client.gameDirectory.toPath().toAbsolutePath().normalize()),
                    "Fixture resource packs must remain inside the isolated client directory");
            Files.createDirectories(packs);
            packDirectory = Files.createTempDirectory(packs, "tail-image-cache-review-");
            packId = "file/" + packDirectory.getFileName();
            String token = UUID.randomUUID().toString();
            absent = ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "textures/" + token + "/appeared.png");
            resized = ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "textures/" + token + "/resized.png");
            square = existingPng("textures/aspects/_unknown.png", 32, 32);
            rectangle = existingPng("textures/models/bootstraveler.png", 64, 32);
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            Files.writeString(packDirectory.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":" + format + ",\"description\":\"Isolated IMAGE cache review fixture\"}}\n");
            write(resized, square);
        }

        CompletableFuture<Result> run() {
            var repository = client.getResourcePackRepository();
            repository.reload();
            require(repository.isAvailable(packId), "Native resource-pack discovery did not find " + packId);
            var selected = new ArrayList<>(previousSelection);
            selected.add(packId);
            repository.setSelected(selected);
            CompletableFuture<Void> work = client.reloadResourcePacks()
                    .thenRunAsync(this::primePages, client)
                    .thenComposeAsync(ignored -> {
                        try {
                            write(absent, square);
                            write(resized, rectangle);
                            return client.reloadResourcePacks();
                        } catch (IOException failure) {
                            return CompletableFuture.failedFuture(failure);
                        }
                    }, client)
                    .thenRunAsync(this::checkReloadedPages, client);
            return work.handleAsync((ignored, failure) -> {
                if (failure != null) record("IMAGE resource-reload fixture completes", false, failure.toString());
                repository.setSelected(previousSelection);
                return client.reloadResourcePacks().handleAsync((restored, restoreFailure) -> {
                    if (restoreFailure != null)
                        record("IMAGE resource-reload fixture restores prior resource packs", false, restoreFailure.toString());
                    else record("IMAGE resource-reload fixture restores prior resource packs",
                            List.copyOf(repository.getSelectedIds()).equals(previousSelection),
                            "before=" + previousSelection + ", after=" + repository.getSelectedIds());
                    return new Result(checks, failures, packDirectory.toString());
                }, client);
            }, client).thenCompose(future -> future);
        }

        private void primePages() {
            require(client.getResourceManager().getResource(absent).isEmpty(), "Missing-image fixture unexpectedly exists");
            checkDimensions("Initial square resource is available through the actual ResourceManager", resized, 32, 32);
            Draw missing = render(absent), present = render(resized);
            record("Initial missing IMAGE page draws no image", missing.count() == 0, missing.toString());
            record("Initial square IMAGE page renders native 120x120 geometry",
                    present.equals(new Draw(1, 120, 120, 32, 32)), present.toString());
        }

        private void checkReloadedPages() {
            // These controls distinguish a cache failure from pack-selection or reload failure.
            checkDimensions("Reloaded actual ResourceManager supplies newly present IMAGE", absent, 32, 32);
            checkDimensions("Reloaded actual ResourceManager supplies changed IMAGE dimensions", resized, 64, 32);
            Draw appeared = render(absent), changed = render(resized);
            record("Previously missing IMAGE page renders after actual resource reload",
                    appeared.equals(new Draw(1, 120, 120, 32, 32)), appeared.toString());
            record("Existing IMAGE page uses new dimensions after actual resource reload",
                    changed.equals(new Draw(1, 120, 60, 64, 32)), changed.toString());
        }

        private Draw render(ResourceLocation texture) {
            var page = new ResearchDefinition.Page("api", null, null, null, null, new ResearchPage(texture, null));
            ThaumcraftRecipeDisplay display = ThaumcraftRecipeDisplay.resolve(page, client.level);
            require(display != null, "Public IMAGE page resolver rejected " + texture);
            var graphics = new ObservedGraphics(client, texture);
            try {
                display.render(graphics, client.font, client.level, client.player, 8, 8, 120, 120, -1000, -1000, 0);
                graphics.flush();
                return graphics.draw;
            } finally {
                graphics.setColor(1, 1, 1, 1);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }
        }

        private void checkDimensions(String name, ResourceLocation texture, int width, int height) {
            try (var stream = client.getResourceManager().open(texture); var image = NativeImage.read(stream)) {
                record(name, image.getWidth() == width && image.getHeight() == height,
                        "expected=" + width + 'x' + height + ", actual=" + image.getWidth() + 'x' + image.getHeight());
            } catch (IOException failure) {
                throw new IllegalStateException("Actual ResourceManager cannot read fixture " + texture, failure);
            }
        }

        private byte[] existingPng(String path, int width, int height) throws IOException {
            byte[] bytes;
            try (var stream = client.getResourceManager().open(ResourceLocation.fromNamespaceAndPath("thaumcraft", path))) {
                bytes = stream.readAllBytes();
            }
            try (var image = NativeImage.read(new java.io.ByteArrayInputStream(bytes))) {
                require(image.getWidth() == width && image.getHeight() == height, "Unexpected original fixture size: " + path);
            }
            return bytes; // Existing texture bytes are copied unchanged; no generated/edited image.
        }

        private void write(ResourceLocation texture, byte[] png) throws IOException {
            Path target = packDirectory.resolve("assets").resolve(texture.getNamespace()).resolve(texture.getPath()).normalize();
            require(target.startsWith(packDirectory), "Fixture resource escaped its unique pack");
            Files.createDirectories(target.getParent());
            Files.write(target, png);
        }

        private void record(String name, boolean passed, String detail) {
            checks.add(Map.of("check", name, "passed", passed, "detail", detail));
            if (!passed) failures.add(name + ": " + detail);
        }
    }

    private record Draw(int count, int width, int height, int textureWidth, int textureHeight) {}

    /** Observe the public IMAGE draw call while delegating every draw to the actual native GUI. */
    private static final class ObservedGraphics extends GuiGraphics {
        private final ResourceLocation observed;
        private Draw draw = new Draw(0, 0, 0, 0, 0);

        ObservedGraphics(Minecraft client, ResourceLocation observed) {
            super(client, client.renderBuffers().bufferSource());
            this.observed = observed;
        }

        @Override public void blit(ResourceLocation texture, int x, int y, int width, int height,
                                   float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
            if (texture.equals(observed)) draw = new Draw(draw.count() + 1, width, height, textureWidth, textureHeight);
            super.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
