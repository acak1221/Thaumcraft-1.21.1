package thaumcraft.assets;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.SourceFilter;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Validate against native Minecraft codecs, beyond file-existence checks. */
class NativeResourceCodecTest {
    @Test void legacySpriteAtlasUsesNativeSourceCodecs() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/minecraft/atlases/blocks.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var sources = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("sources");
            assertFalse(sources.isEmpty());
            for (var element : sources) {
                var source = element.getAsJsonObject();
                String type = source.get("type").getAsString();
                var result = switch (type) {
                    case "minecraft:directory" -> DirectoryLister.CODEC.codec().parse(JsonOps.INSTANCE, source);
                    case "minecraft:filter" -> SourceFilter.CODEC.codec().parse(JsonOps.INSTANCE, source);
                    case "minecraft:single" -> SingleFile.CODEC.codec().parse(JsonOps.INSTANCE, source);
                    default -> throw new AssertionError("Unexpected atlas source type " + type);
                };
                assertTrue(result.result().isPresent(), () -> "Minecraft rejected " + source + ": " + result.error());
            }
        }
    }

    @Test void staticModelSheetsUseNativeFullImageFrames() {
        for (int[] dimensions : new int[][]{{64, 32}, {128, 64}}) {
            var frame = AnimationMetadataSection.EMPTY.calculateFrameSize(dimensions[0], dimensions[1]);
            assertEquals(dimensions[0], frame.width(), "no metadata must retain the full UV sheet width");
            assertEquals(dimensions[1], frame.height(), "no metadata must retain the full UV sheet height");
        }
    }

    @Test void singleSpriteStillRequiresANativeResourceIdentifier() {
        var missing = JsonParser.parseString("{\"type\":\"minecraft:single\"}");
        assertTrue(SingleFile.CODEC.codec().parse(JsonOps.INSTANCE, missing).error().isPresent());
        var invalid = JsonParser.parseString("{\"type\":\"minecraft:single\",\"resource\":\"thaumcraft:models/BadName\"}");
        assertTrue(SingleFile.CODEC.codec().parse(JsonOps.INSTANCE, invalid).error().isPresent());
    }

    @Test void soundEventIdentifiersAreLegalNativeResourceLocations() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/thaumcraft/sounds.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var sounds = JsonParser.parseReader(reader).getAsJsonObject();
            assertFalse(sounds.isEmpty());
            for (String name : sounds.keySet()) {
                assertNotNull(ResourceLocation.tryParse("thaumcraft:" + name), "Illegal sound event identifier " + name);
            }
        }
    }
}
